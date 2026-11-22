package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.monitor.GameMonitorService
import com.example.monitor.LiveMonitorBus
import com.example.ui.ForgeViewModel
import com.example.ui.components.InAppSideEdgeTab
import com.example.ui.screens.GearChangesScreen
import com.example.ui.screens.GuidesScreen
import com.example.ui.screens.OptimizerScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.ForgeObsidian
import com.example.ui.theme.ForgeSlateDark
import com.example.ui.theme.ForgeSurfaceElevated
import com.example.ui.theme.ForgeTextSecondary
import com.example.ui.theme.HunterForgeTheme
import com.example.ui.theme.MoltenAmber

private enum class ForgeTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    SCAN("Scan", Icons.Filled.Radar, Icons.Outlined.Radar, "nav_scan"),
    VAULT("Vault", Icons.Filled.Inventory2, Icons.Outlined.Inventory2, "nav_vault"),
    OPTIMIZE("Optimize", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_optimize"),
    CHANGES("Changes", Icons.Filled.SwapHoriz, Icons.Outlined.SwapHoriz, "nav_changes"),
    GUIDES("Guides", Icons.Filled.MenuBook, Icons.Outlined.MenuBook, "nav_guides")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HunterForgeTheme {
                HunterForgeApp()
            }
        }
    }
}

@Composable
fun HunterForgeApp(
    viewModel: ForgeViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var isSideTabExpanded by rememberSaveable { mutableStateOf(false) }

    val vaultItems by viewModel.vaultItems.collectAsStateWithLifecycle()
    val heroes by viewModel.heroes.collectAsStateWithLifecycle()
    val customBuilds by viewModel.customBuilds.collectAsStateWithLifecycle()
    val selectedVaultBuild by viewModel.selectedVaultBuild.collectAsStateWithLifecycle()
    val selectedHero by viewModel.selectedHero.collectAsStateWithLifecycle()
    val optimizedLoadout by viewModel.optimizedLoadout.collectAsStateWithLifecycle()
    val rosterSummary by viewModel.rosterSummary.collectAsStateWithLifecycle()
    val excludeOtherEquipped by viewModel.excludeOtherEquipped.collectAsStateWithLifecycle()
    val liveState by viewModel.liveMonitorState.collectAsStateWithLifecycle()
    val mediaScanState by viewModel.mediaScanState.collectAsStateWithLifecycle()
    val compareAId by viewModel.compareItemAId.collectAsStateWithLifecycle()
    val compareBId by viewModel.compareItemBId.collectAsStateWithLifecycle()
    val comparisonResult by viewModel.comparisonResult.collectAsStateWithLifecycle()
    val rosterChangePlan by viewModel.rosterChangePlan.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusBanner) {
        statusBanner?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearStatusBanner()
        }
    }

    LaunchedEffect(liveState.requestedAppTab) {
        liveState.requestedAppTab?.let { target ->
            selectedTabIndex = target.coerceIn(0, ForgeTab.entries.lastIndex)
            LiveMonitorBus.consumeRequestedTab()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ForgeObsidian,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = ForgeSlateDark,
                tonalElevation = 0.dp
            ) {
                ForgeTab.entries.forEachIndexed { index, tab ->
                    val selected = selectedTabIndex == index
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            if (tab == ForgeTab.CHANGES && rosterChangePlan.totalChangesCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = MoltenAmber,
                                            contentColor = ForgeObsidian
                                        ) {
                                            Text("${rosterChangePlan.totalChangesCount}")
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            }
                        },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MoltenAmber,
                            selectedTextColor = MoltenAmber,
                            indicatorColor = ForgeSurfaceElevated,
                            unselectedIconColor = ForgeTextSecondary,
                            unselectedTextColor = ForgeTextSecondary
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (ForgeTab.entries[selectedTabIndex]) {
                ForgeTab.SCAN -> ScannerScreen(
                    liveState = liveState,
                    mediaScanState = mediaScanState,
                    vaultCount = vaultItems.size,
                    heroCount = heroes.size,
                    onScanVideoUri = viewModel::scanVideoUri,
                    onScanImageUri = viewModel::scanImageUri,
                    onCancelScan = viewModel::cancelActiveScan,
                    onRemoveStagedItem = viewModel::removeStagedItem,
                    onCommitStagedItems = viewModel::commitStagedItems,
                    onDismissStaging = viewModel::dismissStaging,
                    onSaveManualItem = viewModel::saveVaultItem,
                    onSetInterval = viewModel::setMonitorInterval,
                    onSetAutoSave = viewModel::setMonitorAutoSave,
                    onSetAutoScan = viewModel::setMonitorAutoScan,
                    onSaveLiveDetections = viewModel::saveLiveDetectionsToVault,
                    onClearLiveHistory = viewModel::clearLiveMonitorHistory,
                    onUpdateHunters = {
                        viewModel.updateAllHuntersRoster()
                        selectedTabIndex = ForgeTab.CHANGES.ordinal
                    }
                )

                ForgeTab.VAULT -> VaultScreen(
                    items = vaultItems,
                    heroes = heroes,
                    customBuilds = customBuilds,
                    selectedBuildFilter = selectedVaultBuild,
                    compareItemAId = compareAId,
                    compareItemBId = compareBId,
                    comparisonResult = comparisonResult,
                    onSelectBuildFilter = viewModel::selectVaultBuildFilter,
                    onToggleCompare = viewModel::toggleCompareItem,
                    onClearCompare = viewModel::clearComparison,
                    onSaveItem = viewModel::saveVaultItem,
                    onDeleteItem = viewModel::deleteVaultItem
                )

                ForgeTab.OPTIMIZE -> OptimizerScreen(
                    heroes = heroes,
                    selectedHero = selectedHero,
                    customBuilds = customBuilds,
                    optimizedLoadout = optimizedLoadout,
                    rosterSummary = rosterSummary,
                    excludeOtherEquipped = excludeOtherEquipped,
                    vaultCount = vaultItems.size,
                    onSelectHero = viewModel::selectHero,
                    onSaveHero = viewModel::saveHero,
                    onDeleteHero = viewModel::deleteHero,
                    onAssignRoleAndBuild = viewModel::assignRoleAndBuildToHero,
                    onSaveBuild = viewModel::saveCustomBuild,
                    onDeleteBuild = viewModel::deleteCustomBuild,
                    onExportBuildCode = viewModel::exportBuildCode,
                    onDecodeBuildCode = viewModel::decodeBuildCode,
                    onSetExcludeOtherEquipped = viewModel::setExcludeOtherEquipped,
                    onEquipLoadout = viewModel::equipRecommendedLoadout,
                    onUpdateAllHunters = viewModel::updateAllHuntersRoster
                )

                ForgeTab.CHANGES -> GearChangesScreen(
                    plan = rosterChangePlan,
                    vaultCount = vaultItems.size,
                    onApplySingleChange = viewModel::applySingleGearChange,
                    onApplyHeroChanges = viewModel::applyAllChangesForHero,
                    onApplyAllChanges = viewModel::applyAllRosterGearChanges,
                    onNavigateToOptimize = { selectedTabIndex = ForgeTab.OPTIMIZE.ordinal },
                    onNavigateToScan = { selectedTabIndex = ForgeTab.SCAN.ordinal }
                )

                ForgeTab.GUIDES -> GuidesScreen()
            }

            // Small collapsible right-edge quick tab inside the app as well
            InAppSideEdgeTab(
                isExpanded = isSideTabExpanded,
                isMonitorRunning = liveState.isRunning,
                statusLine = if (liveState.isRunning) {
                    liveState.statusMessage
                } else {
                    "Tap 'Look at New Gear' to scan or 'Update Hunters' to re-optimize."
                },
                onToggleExpand = { isSideTabExpanded = !isSideTabExpanded },
                onScanNewGear = {
                    isSideTabExpanded = false
                    if (liveState.isRunning) {
                        context.startService(
                            GameMonitorService.actionIntent(
                                context,
                                GameMonitorService.ACTION_SCAN_NOW
                            )
                        )
                    } else {
                        selectedTabIndex = ForgeTab.SCAN.ordinal
                    }
                },
                onUpdateHunters = {
                    isSideTabExpanded = false
                    viewModel.updateAllHuntersRoster()
                    selectedTabIndex = ForgeTab.CHANGES.ordinal
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(bottom = 24.dp)
            )
        }
    }
}
