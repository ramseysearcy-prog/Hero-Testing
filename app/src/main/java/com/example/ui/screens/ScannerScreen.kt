package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.domain.CommunityMinMaxEngine
import com.example.domain.ScanSource
import com.example.domain.VaultItem
import com.example.monitor.GameMonitorService
import com.example.monitor.LiveMonitorState
import com.example.ui.MediaScanUiState
import com.example.ui.components.ItemEditorDialog
import com.example.ui.components.RollGradeBadge
import com.example.ui.components.StatPillsRow
import com.example.ui.components.TierBadge
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.ForgeBorder
import com.example.ui.theme.ForgeCrimson
import com.example.ui.theme.ForgeEmerald
import com.example.ui.theme.ForgeObsidian
import com.example.ui.theme.ForgeSlateDark
import com.example.ui.theme.ForgeSurface
import com.example.ui.theme.ForgeSurfaceElevated
import com.example.ui.theme.ForgeTextPrimary
import com.example.ui.theme.ForgeTextSecondary
import com.example.ui.theme.MoltenAmber

@Composable
fun ScannerScreen(
    liveState: LiveMonitorState,
    mediaScanState: MediaScanUiState,
    vaultCount: Int,
    heroCount: Int,
    onScanVideoUri: (Uri) -> Unit,
    onScanImageUri: (Uri) -> Unit,
    onCancelScan: () -> Unit,
    onRemoveStagedItem: (String) -> Unit,
    onCommitStagedItems: () -> Unit,
    onDismissStaging: () -> Unit,
    onSaveManualItem: (VaultItem) -> Unit,
    onSetInterval: (Long) -> Unit,
    onSetAutoSave: (Boolean) -> Unit,
    onSetAutoScan: (Boolean) -> Unit,
    onSaveLiveDetections: () -> Unit,
    onClearLiveHistory: () -> Unit,
    onUpdateHunters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var canDrawOverlay by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var showManualDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canDrawOverlay = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Zero-permission Photo Picker for Videos and Screenshots
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(onScanVideoUri) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(onScanImageUri) }

    // MediaProjection Consent Launcher
    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = GameMonitorService.startServiceIntent(
                context = context,
                resultCode = result.resultCode,
                data = result.data!!
            )
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }

    val startProjectionFlow = {
        val manager = context.getSystemService(MediaProjectionManager::class.java)
        projectionLauncher.launch(manager.createScreenCaptureIntent())
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        // Proceed with MediaProjection regardless; notifications simply enhance FGS controls
        startProjectionFlow()
    }

    val onStartMonitorClicked = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startProjectionFlow()
        }
    }

    if (mediaScanState is MediaScanUiState.StagingResults) {
        BackHandler { onDismissStaging() }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Atmospheric Forge Hero Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_forge_banner_1795332678903),
                        contentDescription = "Hunter Forge anvil and arcane runes",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        ForgeObsidian.copy(alpha = 0.25f),
                                        ForgeObsidian.copy(alpha = 0.72f),
                                        ForgeObsidian
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "HUNTER FORGE",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MoltenAmber
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Live Game OCR • Video Scanner • Min-Max Optimizer",
                            style = MaterialTheme.typography.bodySmall,
                            color = ForgeTextPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ForgeSlateDark.copy(alpha = 0.9f),
                                border = BorderStroke(1.dp, ForgeBorder)
                            ) {
                                Text(
                                    text = "$vaultCount Items Logged",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ArcaneCyan,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ForgeSlateDark.copy(alpha = 0.9f),
                                border = BorderStroke(1.dp, ForgeBorder)
                            ) {
                                Text(
                                    text = "$heroCount Heroes Saved",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MoltenAmber,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Live Game Monitor + Floating Side Tab Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = ForgeSurface),
                    border = BorderStroke(
                        1.dp,
                        if (liveState.isRunning) ForgeEmerald.copy(alpha = 0.8f) else ForgeBorder
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Radar,
                                    contentDescription = null,
                                    tint = if (liveState.isRunning) ForgeEmerald else MoltenAmber,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Live Game Monitor",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = ForgeTextPrimary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (liveState.isRunning) {
                                    if (liveState.isPaused) MoltenAmber.copy(alpha = 0.18f)
                                    else ForgeEmerald.copy(alpha = 0.18f)
                                } else ForgeSurfaceElevated
                            ) {
                                Text(
                                    text = when {
                                        !liveState.isRunning -> "IDLE"
                                        liveState.isPaused -> "PAUSED"
                                        else -> "ACTIVE"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        !liveState.isRunning -> ForgeTextSecondary
                                        liveState.isPaused -> MoltenAmber
                                        else -> ForgeEmerald
                                    },
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = liveState.statusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ForgeTextSecondary
                        )

                        // Floating Side-Edge Tab Permission Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ForgeSlateDark,
                            border = BorderStroke(
                                1.dp,
                                if (canDrawOverlay) ArcaneCyan.copy(alpha = 0.45f) else MoltenAmber.copy(alpha = 0.45f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = if (canDrawOverlay) ArcaneCyan else MoltenAmber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Floating Side Tab Over Game",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = ForgeTextPrimary
                                        )
                                        Text(
                                            text = if (canDrawOverlay) {
                                                "Enabled • Small edge tab floats over Evil Hunter Tycoon when monitoring."
                                            } else {
                                                "Optional • Grant 'Display over other apps' for the small collapsible edge tab."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ForgeTextSecondary
                                        )
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        openOverlaySettings(context)
                                    },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("grant_overlay_tab_btn")
                                ) {
                                    Text(if (canDrawOverlay) "Manage" else "Enable")
                                }
                            }
                        }

                        // Primary Start / Control Buttons
                        if (!liveState.isRunning) {
                            Button(
                                onClick = onStartMonitorClicked,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("start_live_monitor_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MoltenAmber,
                                    contentColor = ForgeObsidian
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Start Live Game Monitor", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        context.startService(
                                            GameMonitorService.actionIntent(
                                                context,
                                                GameMonitorService.ACTION_SCAN_NOW
                                            )
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("live_scan_now_btn"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MoltenAmber,
                                        contentColor = ForgeObsidian
                                    )
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Scan Now")
                                }

                                OutlinedButton(
                                    onClick = {
                                        context.startService(
                                            GameMonitorService.actionIntent(
                                                context,
                                                GameMonitorService.ACTION_TOGGLE_PAUSE
                                            )
                                        )
                                    },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("live_pause_btn")
                                ) {
                                    Icon(
                                        imageVector = if (liveState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = if (liveState.isPaused) "Resume monitor" else "Pause monitor"
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        context.startService(
                                            GameMonitorService.actionIntent(
                                                context,
                                                GameMonitorService.ACTION_STOP
                                            )
                                        )
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ForgeCrimson),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("live_stop_btn")
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = "Stop monitor")
                                }
                            }
                        }

                        // Monitor Settings
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-Log Scanned Gear", style = MaterialTheme.typography.titleSmall, color = ForgeTextPrimary)
                                Text("Saves unique tooltips straight to local Vault", style = MaterialTheme.typography.bodySmall, color = ForgeTextSecondary)
                            }
                            Switch(
                                checked = liveState.autoSaveToVault,
                                onCheckedChange = onSetAutoSave,
                                modifier = Modifier.testTag("monitor_autosave_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Continuous Auto-Scan", style = MaterialTheme.typography.titleSmall, color = ForgeTextPrimary)
                                Text("Turn off to scan only when you tap side tab", style = MaterialTheme.typography.bodySmall, color = ForgeTextSecondary)
                            }
                            Switch(
                                checked = liveState.autoScanEnabled,
                                onCheckedChange = onSetAutoScan,
                                modifier = Modifier.testTag("monitor_autoscan_switch")
                            )
                        }

                        if (liveState.autoScanEnabled) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Interval:", style = MaterialTheme.typography.bodySmall, color = ForgeTextSecondary)
                                listOf(1000L to "1.0s", 1500L to "1.5s", 2500L to "2.5s").forEach { (ms, label) ->
                                    FilterChip(
                                        selected = liveState.scanIntervalMs == ms,
                                        onClick = { onSetInterval(ms) },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }

                        // Live Stats Telemetry
                        if (liveState.framesAnalyzed > 0 || liveState.lastHunterUpdateSummary != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ForgeObsidian,
                                border = BorderStroke(1.dp, ForgeBorder)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Frames: ${liveState.framesAnalyzed} • Logged: ${liveState.itemsLoggedCount}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ArcaneCyan
                                    )
                                    liveState.lastDetectedVerdict?.let {
                                        Text(
                                            text = "Last Scan: $it",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MoltenAmber
                                        )
                                    }
                                    liveState.lastHunterUpdateSummary?.let { summary ->
                                        Text(
                                            text = "Hunter Roster Update:\n$summary",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ForgeTextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Video & Screenshot OCR Upload Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = ForgeSurface),
                    border = BorderStroke(1.dp, ForgeBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Upload Video or Screenshot",
                            style = MaterialTheme.typography.titleLarge,
                            color = ForgeTextPrimary
                        )
                        Text(
                            text = "Select a screen recording of you scrolling through Evil Hunter Tycoon gear & runes. ML Kit OCR extracts, deduplicates, and grades every item.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ForgeTextSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    videoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("upload_video_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ArcaneCyan,
                                    contentColor = ForgeObsidian
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.VideoFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Scan Video", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    imagePickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("upload_image_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ImageSearch, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Screenshot")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showManualDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("manual_add_item_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Manual / Paste OCR")
                            }

                            OutlinedButton(
                                onClick = onUpdateHunters,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("quick_update_hunters_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp), tint = MoltenAmber)
                                Spacer(Modifier.width(6.dp))
                                Text("Update Hunters", color = MoltenAmber)
                            }
                        }

                        // Active Video/Image OCR Progress
                        AnimatedVisibility(visible = mediaScanState is MediaScanUiState.ScanningVideo || mediaScanState is MediaScanUiState.ScanningImage || mediaScanState is MediaScanUiState.Error) {
                            when (mediaScanState) {
                                is MediaScanUiState.ScanningVideo -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val progress = if (mediaScanState.totalFrames > 0) {
                                            mediaScanState.currentFrame.toFloat() / mediaScanState.totalFrames.toFloat()
                                        } else 0f
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Analyzing frame ${mediaScanState.currentFrame}/${mediaScanState.totalFrames} • Found ${mediaScanState.foundItems.size}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = ArcaneCyan
                                            )
                                            TextButton(onClick = onCancelScan) {
                                                Text("Cancel", color = ForgeCrimson)
                                            }
                                        }
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = MoltenAmber,
                                            trackColor = ForgeObsidian
                                        )
                                    }
                                }
                                is MediaScanUiState.ScanningImage -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Running ML Kit OCR on screenshot...", style = MaterialTheme.typography.labelMedium, color = ArcaneCyan)
                                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MoltenAmber)
                                    }
                                }
                                is MediaScanUiState.Error -> {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = ForgeCrimson.copy(alpha = 0.14f),
                                        border = BorderStroke(1.dp, ForgeCrimson.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = mediaScanState.message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ForgeTextPrimary,
                                                modifier = Modifier.weight(1f)
                                            )
                                            TextButton(onClick = onDismissStaging) {
                                                Text("OK", color = MoltenAmber)
                                            }
                                        }
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }

            // 4. Recent Live Detections Feed OR Atmospheric Empty State
            if (liveState.recentDetections.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Live Monitor Captures (${liveState.recentDetections.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MoltenAmber
                        )
                        Row {
                            if (!liveState.autoSaveToVault) {
                                TextButton(onClick = onSaveLiveDetections) {
                                    Text("Sync to Vault", color = ArcaneCyan)
                                }
                            }
                            TextButton(onClick = onClearLiveHistory) {
                                Text("Clear", color = ForgeTextSecondary)
                            }
                        }
                    }
                }
                items(liveState.recentDetections, key = { it.dedupSignature }) { parsed ->
                    val eval = remember(parsed) {
                        CommunityMinMaxEngine.evaluateItemRoll(parsed.toVaultItem(ScanSource.LIVE_MONITOR))
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = ForgeSlateDark),
                        border = BorderStroke(1.dp, ForgeBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (parsed.enhancementLevel > 0) "+${parsed.enhancementLevel} ${parsed.name}" else parsed.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = ForgeTextPrimary
                                    )
                                    Text(
                                        text = "${parsed.slot.displayName} • ${parsed.kind.displayName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ForgeTextSecondary
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TierBadge(parsed.tier)
                                    RollGradeBadge(eval)
                                }
                            }
                            StatPillsRow(parsed.stats)
                        }
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = ForgeSlateDark),
                        border = BorderStroke(1.dp, ForgeBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_scanner_empty_1795332679135),
                                contentDescription = "Arcane gear scanner illustration",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Text(
                                text = "Ready to Inspect Gear & Runes",
                                style = MaterialTheme.typography.titleMedium,
                                color = ForgeTextPrimary
                            )
                            Text(
                                text = "Start Live Game Monitor to place the small quick tab on the side of your screen while playing, or upload a video / screenshot above.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ForgeTextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Staging Modal Overlay for Video / Screenshot OCR Results
        if (mediaScanState is MediaScanUiState.StagingResults) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = ForgeObsidian.copy(alpha = 0.96f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 720.dp)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "OCR Scan Results (${mediaScanState.items.size})",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MoltenAmber
                            )
                            Text(
                                text = "Review detected gear & runes before logging to Vault",
                                style = MaterialTheme.typography.bodySmall,
                                color = ForgeTextSecondary
                            )
                        }
                        TextButton(onClick = onDismissStaging) {
                            Text("Discard", color = ForgeCrimson)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(mediaScanState.items, key = { it.dedupSignature }) { parsed ->
                            val eval = remember(parsed) {
                                CommunityMinMaxEngine.evaluateItemRoll(parsed.toVaultItem(mediaScanState.source))
                            }
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = ForgeSurface),
                                border = BorderStroke(1.dp, ForgeBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (parsed.enhancementLevel > 0) "+${parsed.enhancementLevel} ${parsed.name}" else parsed.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = ForgeTextPrimary
                                            )
                                            Text(
                                                text = "${parsed.slot.displayName} • Confidence ${(parsed.confidence * 100).toInt()}%",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ForgeTextSecondary
                                            )
                                        }
                                        IconButton(onClick = { onRemoveStagedItem(parsed.dedupSignature) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Remove item", tint = ForgeCrimson)
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        TierBadge(parsed.tier)
                                        RollGradeBadge(eval)
                                    }
                                    StatPillsRow(parsed.stats)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onCommitStagedItems,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("commit_staged_items_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MoltenAmber,
                            contentColor = ForgeObsidian
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Log ${mediaScanState.items.size} Item(s) to Vault", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showManualDialog) {
        ItemEditorDialog(
            initialItem = null,
            onDismiss = { showManualDialog = false },
            onSave = { item ->
                onSaveManualItem(item)
                showManualDialog = false
            }
        )
    }
}

private fun openOverlaySettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
