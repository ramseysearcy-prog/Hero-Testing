package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ForgeRepository
import com.example.domain.BuildCodeCodec
import com.example.domain.CommunityMinMaxEngine
import com.example.domain.CustomBuild
import com.example.domain.EhtOcrParser
import com.example.domain.GearChangeAction
import com.example.domain.HeroChangeGroup
import com.example.domain.HeroProfile
import com.example.domain.HeroRole
import com.example.domain.ItemComparisonResult
import com.example.domain.OptimizedLoadoutResult
import com.example.domain.ParsedOcrItem
import com.example.domain.RosterChangePlan
import com.example.domain.ScanSource
import com.example.domain.VaultItem
import com.example.monitor.LiveMonitorBus
import com.example.monitor.LiveMonitorState
import com.example.ocr.MediaScanUpdate
import com.example.ocr.VideoOcrProcessor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MediaScanUiState {
    data object Idle : MediaScanUiState
    data class ScanningVideo(
        val currentFrame: Int,
        val totalFrames: Int,
        val foundItems: List<ParsedOcrItem>
    ) : MediaScanUiState
    data object ScanningImage : MediaScanUiState
    data class StagingResults(
        val items: List<ParsedOcrItem>,
        val source: ScanSource,
        val rawTextPreview: String
    ) : MediaScanUiState
    data class Error(val message: String) : MediaScanUiState
}

class ForgeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ForgeRepository(AppDatabase.getInstance(application))

    val vaultItems: StateFlow<List<VaultItem>> = repository.vaultItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val heroes: StateFlow<List<HeroProfile>> = repository.heroes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val customBuilds: StateFlow<List<CustomBuild>> = repository.customBuilds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val liveMonitorState: StateFlow<LiveMonitorState> = LiveMonitorBus.state

    private val _mediaScanState = MutableStateFlow<MediaScanUiState>(MediaScanUiState.Idle)
    val mediaScanState: StateFlow<MediaScanUiState> = _mediaScanState.asStateFlow()

    private val _selectedHeroId = MutableStateFlow<Long?>(null)
    val selectedHeroId: StateFlow<Long?> = _selectedHeroId.asStateFlow()

    private val _selectedVaultBuildId = MutableStateFlow<Long?>(null)
    val selectedVaultBuildId: StateFlow<Long?> = _selectedVaultBuildId.asStateFlow()

    private val _compareItemAId = MutableStateFlow<Long?>(null)
    val compareItemAId: StateFlow<Long?> = _compareItemAId.asStateFlow()

    private val _compareItemBId = MutableStateFlow<Long?>(null)
    val compareItemBId: StateFlow<Long?> = _compareItemBId.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _rosterSummary = MutableStateFlow<List<OptimizedLoadoutResult>>(emptyList())
    val rosterSummary: StateFlow<List<OptimizedLoadoutResult>> = _rosterSummary.asStateFlow()

    private var scanJob: Job? = null

    val selectedHero: StateFlow<HeroProfile?> = combine(heroes, _selectedHeroId) { list, id ->
        list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val selectedVaultBuild: StateFlow<CustomBuild?> = combine(customBuilds, _selectedVaultBuildId) { builds, id ->
        builds.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val optimizedLoadout: StateFlow<OptimizedLoadoutResult?> = combine(
        selectedHero,
        customBuilds,
        vaultItems
    ) { hero, builds, items ->
        hero?.let {
            val assigned = it.assignedBuildId?.let { bid -> builds.firstOrNull { b -> b.id == bid } }
            CommunityMinMaxEngine.optimizeLoadout(
                hero = it,
                allItems = items,
                assignedBuild = assigned
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val comparisonResult: StateFlow<ItemComparisonResult?> = combine(
        vaultItems,
        _compareItemAId,
        _compareItemBId,
        selectedHero,
        customBuilds
    ) { items, idA, idB, hero, builds ->
        val a = items.firstOrNull { it.id == idA }
        val b = items.firstOrNull { it.id == idB }
        if (a != null && b != null) {
            val assigned = hero?.assignedBuildId?.let { bid -> builds.firstOrNull { it.id == bid } }
            CommunityMinMaxEngine.compareItems(a, b, hero, assigned)
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val rosterChangePlan: StateFlow<RosterChangePlan> = combine(
        heroes,
        vaultItems,
        customBuilds
    ) { heroList, items, builds ->
        CommunityMinMaxEngine.planAllHeroGearChanges(
            heroes = heroList,
            allItems = items,
            buildsById = builds.associateBy { it.id }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), RosterChangePlan(emptyList(), 0, 0))

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun scanVideoUri(uri: Uri) {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            VideoOcrProcessor.scanVideo(getApplication(), uri).collect { update ->
                when (update) {
                    is MediaScanUpdate.Progress -> {
                        _mediaScanState.value = MediaScanUiState.ScanningVideo(
                            currentFrame = update.currentFrame,
                            totalFrames = update.totalFrames,
                            foundItems = update.detectedItems
                        )
                    }
                    is MediaScanUpdate.Completed -> {
                        if (update.detectedItems.isEmpty()) {
                            _mediaScanState.value = MediaScanUiState.Error(
                                "Scanned ${update.framesScanned} frames, but no Evil Hunter Tycoon gear or rune stats were recognized. Make sure item tooltips are clearly visible in the video."
                            )
                        } else {
                            _mediaScanState.value = MediaScanUiState.StagingResults(
                                items = update.detectedItems,
                                source = ScanSource.VIDEO_OCR,
                                rawTextPreview = update.lastRawText
                            )
                        }
                    }
                    is MediaScanUpdate.Failed -> {
                        _mediaScanState.value = MediaScanUiState.Error(update.message)
                    }
                }
            }
        }
    }

    fun scanImageUri(uri: Uri) {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _mediaScanState.value = MediaScanUiState.ScanningImage
            val result = VideoOcrProcessor.scanImage(getApplication(), uri)
            result.onSuccess { (parsed, rawText) ->
                if (parsed == null) {
                    _mediaScanState.value = MediaScanUiState.Error(
                        "OCR completed, but no gear or rune stats were matched. You can paste/edit text manually in Add Item."
                    )
                } else {
                    _mediaScanState.value = MediaScanUiState.StagingResults(
                        items = listOf(parsed),
                        source = ScanSource.SCREENSHOT_OCR,
                        rawTextPreview = rawText
                    )
                }
            }.onFailure { err ->
                _mediaScanState.value = MediaScanUiState.Error(err.localizedMessage ?: "Screenshot OCR failed.")
            }
        }
    }

    fun cancelActiveScan() {
        scanJob?.cancel()
        _mediaScanState.value = MediaScanUiState.Idle
    }

    fun dismissStaging() {
        _mediaScanState.value = MediaScanUiState.Idle
    }

    fun removeStagedItem(signature: String) {
        val current = _mediaScanState.value
        if (current is MediaScanUiState.StagingResults) {
            val remaining = current.items.filterNot { it.dedupSignature == signature }
            _mediaScanState.value = if (remaining.isEmpty()) {
                MediaScanUiState.Idle
            } else {
                current.copy(items = remaining)
            }
        }
    }

    fun commitStagedItems() {
        val current = _mediaScanState.value
        if (current !is MediaScanUiState.StagingResults) return
        viewModelScope.launch {
            val domainItems = current.items.map { it.toVaultItem(current.source) }
            val addedCount = repository.saveItemsDeduplicated(domainItems)
            _mediaScanState.value = MediaScanUiState.Idle
            _statusBanner.value = if (addedCount > 0) {
                "Logged $addedCount new item(s) to Vault."
            } else {
                "All detected items were already in your Vault."
            }
        }
    }

    fun saveLiveDetectionsToVault() {
        val detections = liveMonitorState.value.recentDetections
        if (detections.isEmpty()) return
        viewModelScope.launch {
            val added = repository.saveItemsDeduplicated(
                detections.map { it.toVaultItem(ScanSource.LIVE_MONITOR) }
            )
            _statusBanner.value = "Synced $added new item(s) from Live Monitor to Vault."
        }
    }

    fun clearLiveMonitorHistory() {
        LiveMonitorBus.clearDetections()
    }

    fun setMonitorInterval(intervalMs: Long) {
        LiveMonitorBus.update { it.copy(scanIntervalMs = intervalMs) }
    }

    fun setMonitorAutoSave(enabled: Boolean) {
        LiveMonitorBus.update { it.copy(autoSaveToVault = enabled) }
    }

    fun setMonitorAutoScan(enabled: Boolean) {
        LiveMonitorBus.update { it.copy(autoScanEnabled = enabled) }
    }

    fun parseManualOcrText(rawText: String): ParsedOcrItem? = EhtOcrParser.parse(rawText)

    fun saveVaultItem(item: VaultItem) {
        viewModelScope.launch {
            repository.saveItem(item)
            _statusBanner.value = "Saved ${item.name} to Vault."
        }
    }

    fun deleteVaultItem(id: Long) {
        viewModelScope.launch {
            if (_compareItemAId.value == id) _compareItemAId.value = null
            if (_compareItemBId.value == id) _compareItemBId.value = null
            repository.deleteItem(id)
            _statusBanner.value = "Removed item from Vault."
        }
    }

    fun selectVaultBuildFilter(buildId: Long?) {
        _selectedVaultBuildId.value = if (_selectedVaultBuildId.value == buildId) null else buildId
    }

    fun toggleCompareItem(item: VaultItem) {
        val a = _compareItemAId.value
        val b = _compareItemBId.value
        when {
            a == item.id -> _compareItemAId.value = null
            b == item.id -> _compareItemBId.value = null
            a == null -> _compareItemAId.value = item.id
            b == null -> _compareItemBId.value = item.id
            else -> {
                _compareItemAId.value = b
                _compareItemBId.value = item.id
            }
        }
    }

    fun clearComparison() {
        _compareItemAId.value = null
        _compareItemBId.value = null
    }

    fun selectHero(heroId: Long) {
        _selectedHeroId.value = heroId
    }

    fun saveHero(hero: HeroProfile) {
        viewModelScope.launch {
            val id = repository.saveHero(hero)
            _selectedHeroId.value = if (hero.id != 0L) hero.id else id
            _statusBanner.value = "Saved ${hero.name} (${hero.role.displayName})."
        }
    }

    fun assignRoleAndBuildToHero(hero: HeroProfile, role: HeroRole, buildId: Long?) {
        saveHero(hero.copy(role = role, assignedBuildId = buildId))
    }

    fun deleteHero(heroId: Long) {
        viewModelScope.launch {
            repository.deleteHero(heroId)
            if (_selectedHeroId.value == heroId) _selectedHeroId.value = null
            _statusBanner.value = "Hero deleted and gear unequipped."
        }
    }

    fun saveCustomBuild(build: CustomBuild) {
        viewModelScope.launch {
            repository.saveBuild(build)
            _statusBanner.value = "Saved build '${build.name}'."
        }
    }

    fun deleteCustomBuild(buildId: Long) {
        viewModelScope.launch {
            if (_selectedVaultBuildId.value == buildId) _selectedVaultBuildId.value = null
            repository.deleteBuild(buildId)
            _statusBanner.value = "Build deleted."
        }
    }

    fun exportBuildCode(build: CustomBuild): String = BuildCodeCodec.encode(build)

    fun decodeBuildCode(code: String): Result<CustomBuild> = BuildCodeCodec.decode(code)

    fun equipRecommendedLoadout(result: OptimizedLoadoutResult) {
        markGearChangesApplied(CommunityMinMaxEngine.gearChangesFor(result))
    }

    fun applySingleGearChange(change: GearChangeAction) {
        markGearChangesApplied(listOf(change))
    }

    fun applyAllChangesForHero(group: HeroChangeGroup) {
        markGearChangesApplied(group.changes)
    }

    fun applyAllRosterGearChanges(plan: RosterChangePlan) {
        markGearChangesApplied(plan.heroGroups.flatMap { it.changes })
    }

    private fun markGearChangesApplied(changes: List<GearChangeAction>) {
        if (changes.isEmpty()) return
        viewModelScope.launch {
            _statusBanner.value = if (repository.applyGearChanges(changes)) {
                "Marked ${changes.size} confirmed gear change(s)."
            } else {
                "The Vault changed. Review the refreshed swaps before confirming again."
            }
        }
    }

    fun updateAllHuntersRoster() {
        viewModelScope.launch {
            val results = repository.runRosterOptimizationSummary()
            _rosterSummary.value = results
            if (results.isEmpty()) {
                _statusBanner.value = "Add at least one hero first to update hunter loadouts."
            } else {
                val summaryText = results.joinToString("\n") { res ->
                    val aspd = String.format(java.util.Locale.US, "%.2f", res.effectiveTotals.atkSpd)
                    val crit = res.effectiveTotals.critChance.toInt()
                    val bestWeapon = res.slotRecommendations.firstOrNull { it.bestItem != null }?.bestItem?.name ?: "No Gear"
                    "${res.hero.name}: ASPD $aspd/0.25 • Crit $crit% • Top: $bestWeapon"
                }
                LiveMonitorBus.update { it.copy(lastHunterUpdateSummary = summaryText) }
                _statusBanner.value = "Re-evaluated ${results.size} hunter(s) against your Vault & Builds."
            }
        }
    }
}
