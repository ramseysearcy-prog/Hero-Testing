package com.example.monitor

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.ForgeRepository
import com.example.domain.CommunityMinMaxEngine
import com.example.domain.EhtOcrParser
import com.example.domain.ScanSource
import com.example.ocr.MlKitOcrEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

class GameMonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var repository: ForgeRepository
    private lateinit var notificationManager: NotificationManager

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var scanLoopJob: Job? = null

    private val sessionSignatures = mutableSetOf<String>()

    // Floating side-edge tab overlay fields (when SYSTEM_ALERT_WINDOW is granted)
    private var windowManager: WindowManager? = null
    private var overlayRoot: View? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var isOverlayExpanded = false
    private var statusOverlayLabel: TextView? = null
    private var gearActionOverlayBtn: TextView? = null
    private var autoToggleOverlayBtn: TextView? = null
    private var collapsedTabView: TextView? = null
    private var expandedCardView: LinearLayout? = null

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            Log.i(TAG, "MediaProjection stopped by system or user")
            stopMonitoringAndSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = ForgeRepository(AppDatabase.getInstance(applicationContext))
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_DATA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_DATA_INTENT)
                }

                if (resultCode != Activity.RESULT_OK || data == null) {
                    LiveMonitorBus.update {
                        it.copy(
                            isRunning = false,
                            statusMessage = "Screen capture consent was cancelled."
                        )
                    }
                    stopSelf()
                    return START_NOT_STICKY
                }

                // Android 14+ requires startForeground() with FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                // BEFORE calling MediaProjectionManager.getMediaProjection().
                val notification = buildNotification("Monitoring ready • Tap side tab in game")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }

                startCapturePipeline(resultCode, data)
            }

            ACTION_SCAN_NOW -> {
                serviceScope.launch { captureAndAnalyzeSingleFrame(force = true) }
            }

            ACTION_UPDATE_HUNTERS -> {
                serviceScope.launch { runHunterRosterUpdate() }
            }

            ACTION_TOGGLE_PAUSE -> {
                val currentlyPaused = LiveMonitorBus.state.value.isPaused
                val nextPaused = !currentlyPaused
                LiveMonitorBus.update {
                    it.copy(
                        isPaused = nextPaused,
                        statusMessage = if (nextPaused) "Monitor paused" else "Monitor active"
                    )
                }
                refreshOverlayAndNotification()
            }

            ACTION_STOP -> {
                stopMonitoringAndSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startCapturePipeline(resultCode: Int, data: Intent) {
        try {
            val projectionManager = getSystemService(MediaProjectionManager::class.java)
            val projection = projectionManager.getMediaProjection(resultCode, data)
            if (projection == null) {
                LiveMonitorBus.update {
                    it.copy(isRunning = false, statusMessage = "Unable to start MediaProjection.")
                }
                stopSelf()
                return
            }

            mediaProjection = projection
            projection.registerCallback(projectionCallback, Handler(Looper.getMainLooper()))

            val metrics = resources.displayMetrics
            // Downscale capture slightly for ultra-fast OCR without excessive memory pressure
            val scale = if (metrics.widthPixels > 1080) 0.7f else 0.85f
            val width = (metrics.widthPixels * scale).toInt().coerceAtLeast(360)
            val height = (metrics.heightPixels * scale).toInt().coerceAtLeast(640)
            val density = metrics.densityDpi

            val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            imageReader = reader

            virtualDisplay = projection.createVirtualDisplay(
                "HunterForgeMonitor",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                null
            )

            val hasOverlay = Settings.canDrawOverlays(this)
            if (hasOverlay) {
                Handler(Looper.getMainLooper()).post { attachSideEdgeOverlay() }
            }

            LiveMonitorBus.update {
                it.copy(
                    isRunning = true,
                    isPaused = false,
                    hasOverlayBubble = hasOverlay,
                    statusMessage = if (hasOverlay) {
                        "Live Monitor active • Small side tab is ready over Evil Hunter Tycoon."
                    } else {
                        "Live Monitor active • Use notification actions or grant overlay tab."
                    }
                )
            }

            startAutoScanLoop()
            refreshOverlayAndNotification()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start capture pipeline", e)
            LiveMonitorBus.update {
                it.copy(
                    isRunning = false,
                    statusMessage = "Monitor error: ${e.localizedMessage ?: "Permission denied"}"
                )
            }
            stopMonitoringAndSelf()
        }
    }

    private fun startAutoScanLoop() {
        scanLoopJob?.cancel()
        scanLoopJob = serviceScope.launch {
            while (isActive) {
                val state = LiveMonitorBus.state.value
                if (state.isRunning && !state.isPaused && state.autoScanEnabled) {
                    captureAndAnalyzeSingleFrame(force = false)
                }
                delay(LiveMonitorBus.state.value.scanIntervalMs.coerceIn(900L, 4000L))
            }
        }
    }

    private suspend fun captureAndAnalyzeSingleFrame(force: Boolean) {
        val reader = imageReader ?: return
        val image = try {
            reader.acquireLatestImage()
        } catch (_: Exception) {
            null
        } ?: run {
            if (force) {
                LiveMonitorBus.update { it.copy(statusMessage = "No fresh frame yet — tap item tooltip in game and retry.") }
                refreshOverlayAndNotification()
            }
            return
        }

        var bitmap: Bitmap? = null
        try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * image.width

            val raw = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            raw.copyPixelsFromBuffer(buffer)
            bitmap = if (rowPadding > 0) {
                Bitmap.createBitmap(raw, 0, 0, image.width, image.height).also { raw.recycle() }
            } else {
                raw
            }
        } catch (e: Exception) {
            Log.w(TAG, "Frame conversion error", e)
        } finally {
            image.close()
        }

        val frameBitmap = bitmap ?: return
        try {
            val rawText = MlKitOcrEngine.recognizeText(frameBitmap)
            val parsed = EhtOcrParser.parse(rawText)

            LiveMonitorBus.update { it.copy(framesAnalyzed = it.framesAnalyzed + 1) }

            if (parsed == null) {
                if (force) {
                    LiveMonitorBus.update {
                        it.copy(statusMessage = "Scanned screen: no item/rune stats detected.")
                    }
                    refreshOverlayAndNotification()
                }
                return
            }

            val isNewInSession = sessionSignatures.add(parsed.dedupSignature)
            val vaultItem = parsed.toVaultItem(ScanSource.LIVE_MONITOR)
            val rollEval = CommunityMinMaxEngine.evaluateItemRoll(vaultItem)
            val verdict = "${rollEval.gradeLabel} (${rollEval.efficiencyPct}%) • ${rollEval.verdict}"

            var savedIncrement = 0
            if (LiveMonitorBus.state.value.autoSaveToVault && (isNewInSession || force)) {
                savedIncrement = repository.saveItemsDeduplicated(listOf(vaultItem))
            }

            LiveMonitorBus.update { state ->
                val updatedList = if (isNewInSession) {
                    (listOf(parsed) + state.recentDetections).take(20)
                } else {
                    state.recentDetections
                }
                state.copy(
                    itemsLoggedCount = state.itemsLoggedCount + savedIncrement,
                    lastDetectedItem = parsed,
                    lastDetectedVerdict = verdict,
                    recentDetections = updatedList,
                    statusMessage = "Detected ${parsed.name} [${rollEval.gradeLabel} ${rollEval.efficiencyPct}%]"
                )
            }
            refreshOverlayAndNotification()
        } catch (e: Exception) {
            Log.e(TAG, "OCR failure on live frame", e)
        } finally {
            frameBitmap.recycle()
        }
    }

    private suspend fun runHunterRosterUpdate() {
        val results = repository.runRosterOptimizationSummary()
        val summary = if (results.isEmpty()) {
            "No heroes in Roster yet. Add a hero in Hunter Forge -> Optimize."
        } else {
            results.joinToString("\n") { res ->
                val aspd = res.effectiveTotals.atkSpd
                val crit = res.effectiveTotals.critChance.roundToInt()
                val bestWep = res.slotRecommendations.firstOrNull { it.bestItem != null }?.bestItem?.name ?: "No Gear"
                "${res.hero.name} (${res.hero.advancement.displayName}): ASPD ${String.format(java.util.Locale.US, "%.2f", aspd)}/0.25 • Crit $crit% • Top: $bestWep"
            }
        }

        LiveMonitorBus.update {
            it.copy(
                lastHunterUpdateSummary = summary,
                statusMessage = if (results.isEmpty()) {
                    "Add a hero first to update hunter loadouts."
                } else {
                    "Updated ${results.size} hunter loadout(s) from Vault!"
                }
            )
        }
        refreshOverlayAndNotification()
    }

    private fun refreshOverlayAndNotification() {
        val state = LiveMonitorBus.state.value
        notificationManager.notify(NOTIFICATION_ID, buildNotification(state.statusMessage))
        Handler(Looper.getMainLooper()).post {
            updateOverlayLabels(state)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachSideEdgeOverlay() {
        if (overlayRoot != null || !Settings.canDrawOverlays(this)) return
        windowManager = getSystemService(WindowManager::class.java)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 0
            y = dp(180)
        }
        overlayParams = params

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // 1. Ultra-compact collapsed side-edge tab (real small pill hugging right edge)
        val tab = TextView(this).apply {
            text = "◀\nF\nO\nR\nG\nE"
            setTextColor(Color.parseColor("#F5A623"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            gravity = Gravity.CENTER
            setPadding(dp(5), dp(10), dp(5), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E6141821"))
                setStroke(dp(1), Color.parseColor("#F5A623"))
                cornerRadii = floatArrayOf(
                    dp(12).toFloat(), dp(12).toFloat(),
                    0f, 0f,
                    0f, 0f,
                    dp(12).toFloat(), dp(12).toFloat()
                )
            }
        }
        collapsedTabView = tab

        // 2. Expanded floating mini-console
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F2141821"))
                setStroke(dp(1), Color.parseColor("#38BDF8"))
                cornerRadius = dp(14).toFloat()
            }
        }
        expandedCardView = card

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titleTv = TextView(this).apply {
            text = "HUNTER FORGE"
            setTextColor(Color.parseColor("#F5A623"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val collapseBtn = makeMiniChip("Hide", "#2E384B", "#F3F4F6") {
            isOverlayExpanded = false
            collapsedTabView?.visibility = View.VISIBLE
            expandedCardView?.visibility = View.GONE
        }
        headerRow.addView(titleTv)
        headerRow.addView(collapseBtn)

        val statusTv = TextView(this).apply {
            text = LiveMonitorBus.state.value.statusMessage
            setTextColor(Color.parseColor("#E2E8F0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding(0, dp(6), 0, dp(8))
            maxWidth = dp(220)
        }
        statusOverlayLabel = statusTv

        val lookGearBtn = makeMiniButton("Look at New Gear (Scan Now)", "#F5A623", "#0D0F14") {
            serviceScope.launch { captureAndAnalyzeSingleFrame(force = true) }
        }
        gearActionOverlayBtn = lookGearBtn

        val updateHuntersBtn = makeMiniButton("Update Hunters (Re-Optimize)", "#38BDF8", "#0D0F14") {
            serviceScope.launch { runHunterRosterUpdate() }
        }

        val bottomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, 0)
        }
        val autoBtn = makeMiniChip("Auto: ON", "#1E2432", "#10B981") {
            val next = !LiveMonitorBus.state.value.autoScanEnabled
            LiveMonitorBus.update {
                it.copy(
                    autoScanEnabled = next,
                    statusMessage = if (next) "Auto-scan ON" else "Manual scan mode (tap Look at New Gear)"
                )
            }
            refreshOverlayAndNotification()
        }
        autoToggleOverlayBtn = autoBtn

        val openAppBtn = makeMiniChip("Open App", "#1E2432", "#F5A623") {
            LiveMonitorBus.update { it.copy(requestedAppTab = 2) }
            val launch = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(launch)
        }

        val stopBtn = makeMiniChip("Stop", "#451A1A", "#FCA5A5") {
            stopMonitoringAndSelf()
        }

        bottomRow.addView(autoBtn)
        bottomRow.addView(spaceView(dp(6)))
        bottomRow.addView(openAppBtn)
        bottomRow.addView(spaceView(dp(6)))
        bottomRow.addView(stopBtn)

        card.addView(headerRow)
        card.addView(statusTv)
        card.addView(lookGearBtn)
        card.addView(spaceView(dp(6)))
        card.addView(updateHuntersBtn)
        card.addView(bottomRow)

        container.addView(tab)
        container.addView(card)

        // Allow dragging the tiny side tab vertically along the right edge, or tapping to expand
        var initialY = 0
        var initialTouchY = 0f
        var moved = false
        tab.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialY = params.y
                    initialTouchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dy) > dp(6)) moved = true
                    params.y = (initialY + dy).coerceIn(dp(48), dp(620))
                    windowManager?.updateViewLayout(container, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        isOverlayExpanded = true
                        tab.visibility = View.GONE
                        card.visibility = View.VISIBLE
                        updateOverlayLabels(LiveMonitorBus.state.value)
                    }
                    true
                }
                else -> false
            }
        }

        overlayRoot = container
        try {
            windowManager?.addView(container, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach overlay window", e)
            overlayRoot = null
        }
    }

    private fun updateOverlayLabels(state: LiveMonitorState) {
        val detail = buildString {
            append(state.statusMessage)
            state.lastDetectedVerdict?.let {
                append("\n")
                append(it)
            }
            state.lastHunterUpdateSummary?.let {
                append("\n")
                append(it.lineSequence().take(2).joinToString("\n"))
            }
        }
        statusOverlayLabel?.text = detail
        autoToggleOverlayBtn?.text = if (state.autoScanEnabled) "Auto: ON" else "Auto: OFF"
        autoToggleOverlayBtn?.setTextColor(
            Color.parseColor(if (state.autoScanEnabled) "#10B981" else "#A0AEC0")
        )
    }

    private fun makeMiniButton(label: String, bgHex: String, fgHex: String, onClick: () -> Unit): TextView =
        TextView(this).apply {
            text = label
            setTextColor(Color.parseColor(fgHex))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = GradientDrawable().apply {
                setColor(Color.parseColor(bgHex))
                cornerRadius = dp(8).toFloat()
            }
            setOnClickListener { onClick() }
        }

    private fun makeMiniChip(label: String, bgHex: String, fgHex: String, onClick: () -> Unit): TextView =
        TextView(this).apply {
            text = label
            setTextColor(Color.parseColor(fgHex))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(5), dp(8), dp(5))
            background = GradientDrawable().apply {
                setColor(Color.parseColor(bgHex))
                cornerRadius = dp(6).toFloat()
            }
            setOnClickListener { onClick() }
        }

    private fun spaceView(heightPx: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(heightPx, heightPx)
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            DisplayMetrics().let { resources.displayMetrics }
        ).roundToInt()

    private fun stopMonitoringAndSelf() {
        scanLoopJob?.cancel()
        scanLoopJob = null
        try {
            virtualDisplay?.release()
        } catch (_: Exception) {
        }
        virtualDisplay = null
        try {
            imageReader?.close()
        } catch (_: Exception) {
        }
        imageReader = null
        try {
            mediaProjection?.unregisterCallback(projectionCallback)
            mediaProjection?.stop()
        } catch (_: Exception) {
        }
        mediaProjection = null

        Handler(Looper.getMainLooper()).post {
            overlayRoot?.let { root ->
                try {
                    windowManager?.removeView(root)
                } catch (_: Exception) {
                }
            }
            overlayRoot = null
        }

        LiveMonitorBus.update {
            it.copy(
                isRunning = false,
                isPaused = false,
                hasOverlayBubble = false,
                statusMessage = "Monitor stopped."
            )
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.monitor_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.monitor_channel_desc)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            100,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val scanNowPending = PendingIntent.getService(
            this,
            101,
            Intent(this, GameMonitorService::class.java).setAction(ACTION_SCAN_NOW),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val updateHuntersPending = PendingIntent.getService(
            this,
            102,
            Intent(this, GameMonitorService::class.java).setAction(ACTION_UPDATE_HUNTERS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPending = PendingIntent.getService(
            this,
            103,
            Intent(this, GameMonitorService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.monitor_notification_title))
            .setContentText(statusText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(statusText))
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .addAction(0, "Scan Gear", scanNowPending)
            .addAction(0, "Update Hunters", updateHuntersPending)
            .addAction(0, getString(R.string.action_stop), stopPending)
            .build()
    }

    override fun onDestroy() {
        stopMonitoringAndSelf()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "GameMonitorService"
        private const val CHANNEL_ID = "hunter_forge_monitor_channel"
        private const val NOTIFICATION_ID = 4041

        const val ACTION_START = "com.example.monitor.ACTION_START"
        const val ACTION_STOP = "com.example.monitor.ACTION_STOP"
        const val ACTION_SCAN_NOW = "com.example.monitor.ACTION_SCAN_NOW"
        const val ACTION_UPDATE_HUNTERS = "com.example.monitor.ACTION_UPDATE_HUNTERS"
        const val ACTION_TOGGLE_PAUSE = "com.example.monitor.ACTION_TOGGLE_PAUSE"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_DATA_INTENT = "extra_data_intent"

        fun startServiceIntent(context: Context, resultCode: Int, data: Intent): Intent =
            Intent(context, GameMonitorService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_DATA_INTENT, data)
            }

        fun actionIntent(context: Context, action: String): Intent =
            Intent(context, GameMonitorService::class.java).apply {
                this.action = action
            }
    }
}
