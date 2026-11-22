package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.BuildArchetype
import com.example.domain.CommunityMinMaxEngine
import com.example.domain.CustomBuild
import com.example.domain.HeroAdvancement
import com.example.domain.HeroBaseClass
import com.example.domain.HeroProfile
import com.example.domain.HeroRole
import com.example.domain.HeroTrait
import com.example.domain.OptimizedLoadoutResult
import com.example.domain.StatTargetRange
import com.example.domain.StatType
import com.example.domain.TargetRace
import com.example.ui.components.CapGaugeBar
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
import java.util.Locale
import kotlin.math.roundToInt

private enum class OptimizerSection { HEROES, BUILDS }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OptimizerScreen(
    heroes: List<HeroProfile>,
    selectedHero: HeroProfile?,
    customBuilds: List<CustomBuild>,
    optimizedLoadout: OptimizedLoadoutResult?,
    rosterSummary: List<OptimizedLoadoutResult>,
    excludeOtherEquipped: Boolean,
    vaultCount: Int,
    onSelectHero: (Long) -> Unit,
    onSaveHero: (HeroProfile) -> Unit,
    onDeleteHero: (Long) -> Unit,
    onAssignRoleAndBuild: (HeroProfile, HeroRole, Long?) -> Unit,
    onSaveBuild: (CustomBuild) -> Unit,
    onDeleteBuild: (Long) -> Unit,
    onExportBuildCode: (CustomBuild) -> String,
    onDecodeBuildCode: (String) -> Result<CustomBuild>,
    onSetExcludeOtherEquipped: (Boolean) -> Unit,
    onEquipLoadout: (OptimizedLoadoutResult) -> Unit,
    onUpdateAllHunters: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeSection by rememberSaveable { mutableStateOf(OptimizerSection.HEROES) }
    var showHeroDialog by remember { mutableStateOf(false) }
    var editingHero by remember { mutableStateOf<HeroProfile?>(null) }

    var showBuildDialog by remember { mutableStateOf(false) }
    var editingBuild by remember { mutableStateOf<CustomBuild?>(null) }
    var shareCodeBuild by remember { mutableStateOf<Pair<CustomBuild, String>?>(null) }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
        ) {
            // Top Mode Switcher: Hunters vs Builds & Share Codes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = activeSection == OptimizerSection.HEROES,
                    onClick = { activeSection = OptimizerSection.HEROES },
                    label = { Text("Hunters (${heroes.size})") },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("optimizer_tab_heroes")
                )
                FilterChip(
                    selected = activeSection == OptimizerSection.BUILDS,
                    onClick = { activeSection = OptimizerSection.BUILDS },
                    label = { Text("Builds & Codes (${customBuilds.size})") },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("optimizer_tab_builds")
                )
            }

            if (activeSection == OptimizerSection.BUILDS) {
                BuildsAndCodesSection(
                    customBuilds = customBuilds,
                    onCreateNewBuild = {
                        editingBuild = null
                        showBuildDialog = true
                    },
                    onEditBuild = { build ->
                        editingBuild = build
                        showBuildDialog = true
                    },
                    onUseTemplate = { template ->
                        editingBuild = template.copy(id = 0L)
                        showBuildDialog = true
                    },
                    onDeleteBuild = onDeleteBuild,
                    onShareBuild = { build ->
                        shareCodeBuild = build to onExportBuildCode(build)
                    },
                    onDecodeBuildCode = onDecodeBuildCode,
                    onSaveImportedBuild = onSaveBuild
                )
            } else if (heroes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.widthIn(max = 520.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = ForgeSurface),
                        border = BorderStroke(1.dp, ForgeBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = MoltenAmber,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Build Your Hunter Roster",
                                style = MaterialTheme.typography.headlineSmall,
                                color = ForgeTextPrimary
                            )
                            Text(
                                text = "Add your Evil Hunter Tycoon heroes with their 3rd Class, Role, and Assigned Build. The min-max engine calculates exact Attack Speed (0.25), Crit (50%), Evasion (40%), and custom stat range combinations from your Vault.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ForgeTextSecondary
                            )
                            Button(
                                onClick = { showHeroDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MoltenAmber,
                                    contentColor = ForgeObsidian
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("create_first_hero_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Add First Hero", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { activeSection = OptimizerSection.BUILDS },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("open_builds_from_empty_btn")
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = ArcaneCyan)
                                Spacer(Modifier.width(8.dp))
                                Text("Create or Import Builds First", color = ArcaneCyan)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Hero Selector Strip
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Hunter Roster (${heroes.size})",
                                style = MaterialTheme.typography.titleLarge,
                                color = MoltenAmber
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = onUpdateAllHunters,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("update_all_hunters_btn")
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = ArcaneCyan)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Update All", color = ArcaneCyan)
                                }
                                Button(
                                    onClick = { showHeroDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MoltenAmber,
                                        contentColor = ForgeObsidian
                                    ),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("add_hero_btn")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Hero")
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            heroes.forEach { hero ->
                                FilterChip(
                                    selected = hero.id == selectedHero?.id,
                                    onClick = { onSelectHero(hero.id) },
                                    label = { Text("${hero.name} (${hero.advancement.displayName})") }
                                )
                            }
                        }
                    }

                    // Batch Roster Update Summary (if triggered)
                    if (rosterSummary.isNotEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = ForgeSurfaceElevated),
                                border = BorderStroke(1.dp, ArcaneCyan.copy(alpha = 0.6f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Town Roster Min-Max Snapshot",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = ArcaneCyan
                                    )
                                    rosterSummary.forEach { res ->
                                        val buildTag = res.assignedBuild?.let { " [${it.name}]" } ?: ""
                                        Text(
                                            text = "${res.hero.name}$buildTag: Score ${res.combatScore.roundToInt()} • ASPD ${String.format(Locale.US, "%.2f", res.effectiveTotals.atkSpd)}/0.25 • Crit ${res.effectiveTotals.critChance.roundToInt()}% • Eva ${res.effectiveTotals.evasion.roundToInt()}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ForgeTextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Selected Hero Profile Card + Quick Role & Build Assignment + Exclude Toggle
                    if (selectedHero != null && optimizedLoadout != null) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = ForgeSurface),
                                border = BorderStroke(1.dp, ForgeBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${selectedHero.name} • ${selectedHero.advancement.displayName}",
                                                style = MaterialTheme.typography.titleLarge,
                                                color = ForgeTextPrimary
                                            )
                                            Text(
                                                text = "${selectedHero.baseClass.displayName} • ${selectedHero.trait.displayName} Trait • ${selectedHero.archetype.displayName}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ArcaneCyan
                                            )
                                            Text(
                                                text = "Role: ${selectedHero.role.displayName} • Target: ${selectedHero.targetRace.displayName} • ${selectedHero.runeSlots} Runes",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ForgeTextSecondary
                                            )
                                        }
                                        Row {
                                            IconButton(
                                                onClick = { editingHero = selectedHero },
                                                modifier = Modifier.testTag("edit_hero_btn")
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit hero", tint = ForgeTextSecondary)
                                            }
                                            IconButton(
                                                onClick = { onDeleteHero(selectedHero.id) },
                                                modifier = Modifier.testTag("delete_hero_btn")
                                            ) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete hero", tint = ForgeCrimson)
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = ForgeBorder.copy(alpha = 0.5f))

                                    // Inline Hero Role Assignment
                                    Text(
                                        text = "Assigned Role",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MoltenAmber
                                    )
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        HeroRole.entries.forEach { role ->
                                            FilterChip(
                                                selected = selectedHero.role == role,
                                                onClick = {
                                                    onAssignRoleAndBuild(selectedHero, role, selectedHero.assignedBuildId)
                                                },
                                                label = { Text(role.displayName) }
                                            )
                                        }
                                    }

                                    // Inline Assigned Custom Build Assignment
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Assigned Build Profile",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ArcaneCyan
                                        )
                                        TextButton(
                                            onClick = { activeSection = OptimizerSection.BUILDS },
                                            modifier = Modifier.testTag("manage_builds_shortcut_btn")
                                        ) {
                                            Text("+ Manage Builds", color = ArcaneCyan)
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        FilterChip(
                                            selected = selectedHero.assignedBuildId == null,
                                            onClick = {
                                                onAssignRoleAndBuild(selectedHero, selectedHero.role, null)
                                            },
                                            label = { Text("Auto (Class Meta)") }
                                        )
                                        customBuilds.forEach { build ->
                                            FilterChip(
                                                selected = selectedHero.assignedBuildId == build.id,
                                                onClick = {
                                                    onAssignRoleAndBuild(selectedHero, selectedHero.role, build.id)
                                                },
                                                label = { Text(build.name) }
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Respect Equipped Gear Lock", style = MaterialTheme.typography.titleSmall, color = ForgeTextPrimary)
                                            Text("Skip items equipped by other hunters", style = MaterialTheme.typography.bodySmall, color = ForgeTextSecondary)
                                        }
                                        Switch(
                                            checked = excludeOtherEquipped,
                                            onCheckedChange = onSetExcludeOtherEquipped
                                        )
                                    }

                                    Button(
                                        onClick = { onEquipLoadout(optimizedLoadout) },
                                        enabled = vaultCount > 0,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ForgeEmerald,
                                            contentColor = ForgeObsidian
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .minimumInteractiveComponentSize()
                                            .testTag("equip_recommended_btn")
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Equip Recommended Loadout (${optimizedLoadout.combatScore.roundToInt()} pts)", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Custom Build Target Ranges Panel (if a build with custom ranges is assigned)
                        if (optimizedLoadout.buildRangeProgressList.isNotEmpty()) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = ForgeSlateDark),
                                    border = BorderStroke(1.dp, ArcaneCyan.copy(alpha = 0.6f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "Build Targets: ${optimizedLoadout.assignedBuild?.name ?: "Custom"}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = ArcaneCyan
                                        )
                                        optimizedLoadout.buildRangeProgressList.forEach { rangeProgress ->
                                            CapGaugeBar(rangeProgress)
                                        }
                                    }
                                }
                            }
                        }

                        // Stat Caps & Min-Max Distribution Panel
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = ForgeSlateDark),
                                border = BorderStroke(1.dp, MoltenAmber.copy(alpha = 0.45f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Community Stat Caps & Effective Build",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MoltenAmber
                                    )
                                    optimizedLoadout.capProgressList.forEach { cap ->
                                        CapGaugeBar(cap)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text("Effective Total Stats (After Caps):", style = MaterialTheme.typography.labelMedium, color = ArcaneCyan)
                                    StatPillsRow(optimizedLoadout.effectiveTotals, maxItems = 12)
                                }
                            }
                        }

                        // Overcap Warnings & Community Notes
                        if (optimizedLoadout.overcapWarnings.isNotEmpty() || optimizedLoadout.communityNotes.isNotEmpty()) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = ForgeSurface),
                                    border = BorderStroke(1.dp, ForgeBorder)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        optimizedLoadout.overcapWarnings.forEach { warning ->
                                            Row(verticalAlignment = Alignment.Top) {
                                                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = ForgeCrimson, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text(warning, style = MaterialTheme.typography.bodySmall, color = ForgeCrimson)
                                            }
                                        }
                                        optimizedLoadout.communityNotes.forEach { note ->
                                            Text("• $note", style = MaterialTheme.typography.bodySmall, color = ForgeTextSecondary)
                                        }
                                    }
                                }
                            }
                        }

                        // Recommended 8 Equipment Slots
                        item {
                            Text(
                                text = "Optimal 8-Slot Gear Combination",
                                style = MaterialTheme.typography.titleMedium,
                                color = ForgeTextPrimary
                            )
                        }

                        items(optimizedLoadout.slotRecommendations, key = { it.slot.name }) { rec ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = ForgeSurface),
                                border = BorderStroke(1.dp, ForgeBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = rec.slot.displayName.uppercase(Locale.US),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ArcaneCyan,
                                            fontWeight = FontWeight.Bold
                                        )
                                        rec.bestItem?.let { TierBadge(it.tier) }
                                    }
                                    if (rec.bestItem != null) {
                                        Text(
                                            text = if (rec.bestItem.enhancementLevel > 0) "+${rec.bestItem.enhancementLevel} ${rec.bestItem.name}" else rec.bestItem.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = ForgeTextPrimary
                                        )
                                        StatPillsRow(rec.bestItem.stats)
                                        Text(
                                            text = rec.rationale,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MoltenAmber
                                        )
                                        rec.runnerUpItem?.let { alt ->
                                            Text(
                                                text = "Alternate: ${alt.name} (${alt.tier.displayName})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ForgeTextSecondary
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = rec.rationale,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ForgeTextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        // Recommended Runes
                        item {
                            Text(
                                text = "Optimal Rune Combination (${optimizedLoadout.runeRecommendations.size}/${selectedHero.runeSlots})",
                                style = MaterialTheme.typography.titleMedium,
                                color = ForgeTextPrimary
                            )
                        }

                        if (optimizedLoadout.runeRecommendations.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = ForgeSlateDark,
                                    border = BorderStroke(1.dp, ForgeBorder)
                                ) {
                                    Text(
                                        text = "No Runes available in Vault. Scan or log Runes so the optimizer can fill missing ASPD/Crit caps and stack Crit DMG!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ForgeTextSecondary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                    )
                                }
                            }
                        } else {
                            items(optimizedLoadout.runeRecommendations, key = { "rune_${it.slotIndex}_${it.rune.id}" }) { runeRec ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = ForgeSlateDark),
                                    border = BorderStroke(1.dp, ArcaneCyan.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "RUNE SLOT #${runeRec.slotIndex} • ${runeRec.rune.name}",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = ForgeTextPrimary
                                            )
                                            TierBadge(runeRec.rune.tier)
                                        }
                                        StatPillsRow(runeRec.rune.stats)
                                        Text(
                                            text = runeRec.rationale,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ArcaneCyan
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showHeroDialog) {
        HeroEditorDialog(
            initialHero = null,
            customBuilds = customBuilds,
            onDismiss = { showHeroDialog = false },
            onSave = { hero ->
                onSaveHero(hero)
                showHeroDialog = false
            }
        )
    }

    editingHero?.let { current ->
        HeroEditorDialog(
            initialHero = current,
            customBuilds = customBuilds,
            onDismiss = { editingHero = null },
            onSave = { updated ->
                onSaveHero(updated)
                editingHero = null
            }
        )
    }

    if (showBuildDialog) {
        BuildEditorDialog(
            initialBuild = editingBuild,
            onDismiss = {
                showBuildDialog = false
                editingBuild = null
            },
            onSave = { saved ->
                onSaveBuild(saved)
                showBuildDialog = false
                editingBuild = null
            }
        )
    }

    shareCodeBuild?.let { (build, code) ->
        ShareBuildCodeDialog(
            build = build,
            shareCode = code,
            onDismiss = { shareCodeBuild = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BuildsAndCodesSection(
    customBuilds: List<CustomBuild>,
    onCreateNewBuild: () -> Unit,
    onEditBuild: (CustomBuild) -> Unit,
    onUseTemplate: (CustomBuild) -> Unit,
    onDeleteBuild: (Long) -> Unit,
    onShareBuild: (CustomBuild) -> Unit,
    onDecodeBuildCode: (String) -> Result<CustomBuild>,
    onSaveImportedBuild: (CustomBuild) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var codeInput by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }
    var decodedPreview by remember { mutableStateOf<CustomBuild?>(null) }
    var importCustomName by remember { mutableStateOf("") }

    val templates = remember { CommunityMinMaxEngine.starterBuildTemplates() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section 1: Upload / Import Share Code from Other Players
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ForgeSlateDark),
                border = BorderStroke(1.dp, ArcaneCyan.copy(alpha = 0.65f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = ArcaneCyan)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Upload / View Build Code",
                            style = MaterialTheme.typography.titleMedium,
                            color = ArcaneCyan
                        )
                    }
                    Text(
                        text = "Paste a Hunter Forge build code (HF1:...) shared by another player to preview its desired stat ranges, then name and save it locally.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ForgeTextSecondary
                    )

                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = {
                            codeInput = it
                            importError = null
                        },
                        label = { Text("Paste Build Code (HF1:...)") },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_build_code_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val pasted = clipboard.getText()?.text.orEmpty()
                                if (pasted.isNotBlank()) {
                                    codeInput = pasted
                                    importError = null
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("paste_build_code_btn")
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp), tint = ForgeTextPrimary)
                            Spacer(Modifier.width(6.dp))
                            Text("Paste", color = ForgeTextPrimary)
                        }
                        Button(
                            onClick = {
                                val res = onDecodeBuildCode(codeInput)
                                res.onSuccess { build ->
                                    decodedPreview = build
                                    importCustomName = build.name
                                    importError = null
                                }.onFailure { err ->
                                    decodedPreview = null
                                    importError = err.localizedMessage ?: "Invalid build code."
                                }
                            },
                            enabled = codeInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ArcaneCyan,
                                contentColor = ForgeObsidian
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("preview_build_code_btn")
                        ) {
                            Text("Preview Build", fontWeight = FontWeight.Bold)
                        }
                    }

                    importError?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = ForgeCrimson
                        )
                    }

                    decodedPreview?.let { preview ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ForgeSurface,
                            border = BorderStroke(1.dp, ForgeEmerald.copy(alpha = 0.7f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Shared Build Preview: ${preview.name}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ForgeEmerald
                                )
                                Text(
                                    text = "Role: ${preview.role.displayName} • Target: ${preview.targetRace.displayName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ArcaneCyan
                                )
                                if (preview.notes.isNotBlank()) {
                                    Text(
                                        text = preview.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ForgeTextSecondary
                                    )
                                }
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    preview.statRanges.values.forEach { range ->
                                        StatRangeChip(range)
                                    }
                                }

                                OutlinedTextField(
                                    value = importCustomName,
                                    onValueChange = { importCustomName = it },
                                    label = { Text("Name This Build Before Saving") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("import_build_name_input")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TextButton(
                                        onClick = {
                                            decodedPreview = null
                                            codeInput = ""
                                            importCustomName = ""
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Discard", color = ForgeTextSecondary)
                                    }
                                    Button(
                                        onClick = {
                                            val finalName = importCustomName.trim().ifBlank { preview.name }
                                            onSaveImportedBuild(
                                                preview.copy(
                                                    id = 0L,
                                                    name = finalName,
                                                    createdAt = System.currentTimeMillis()
                                                )
                                            )
                                            decodedPreview = null
                                            codeInput = ""
                                            importCustomName = ""
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ForgeEmerald,
                                            contentColor = ForgeObsidian
                                        ),
                                        modifier = Modifier
                                            .weight(2f)
                                            .minimumInteractiveComponentSize()
                                            .testTag("save_imported_build_btn")
                                    ) {
                                        Text("Save to My Builds", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Create Custom Build + Starter Templates
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "My Builds (${customBuilds.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = MoltenAmber
                    )
                    Text(
                        text = "Set desired stat ranges so Vault & Heroes know what fits.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ForgeTextSecondary
                    )
                }
                Button(
                    onClick = onCreateNewBuild,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MoltenAmber,
                        contentColor = ForgeObsidian
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("new_build_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Build")
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Quick Start Community Templates",
                style = MaterialTheme.typography.labelMedium,
                color = ForgeTextSecondary
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                templates.forEach { template ->
                    OutlinedButton(
                        onClick = { onUseTemplate(template) },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp), tint = MoltenAmber)
                        Spacer(Modifier.width(6.dp))
                        Text(template.name, color = ForgeTextPrimary)
                    }
                }
            }
        }

        if (customBuilds.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ForgeSurface),
                    border = BorderStroke(1.dp, ForgeBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No Custom Builds Saved Yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = ForgeTextPrimary
                        )
                        Text(
                            text = "Tap '+ New Build' or pick a Community Template above to customize desired stat ranges, assign them to Heroes, or share build codes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ForgeTextSecondary
                        )
                    }
                }
            }
        } else {
            items(customBuilds, key = { it.id }) { build ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ForgeSurface),
                    border = BorderStroke(1.dp, ForgeBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = build.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MoltenAmber
                                )
                                Text(
                                    text = "${build.role.displayName} • Target: ${build.targetRace.displayName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ArcaneCyan
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = { onEditBuild(build) },
                                    modifier = Modifier.testTag("edit_build_btn_${build.id}")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit build", tint = ForgeTextSecondary)
                                }
                                IconButton(
                                    onClick = { onDeleteBuild(build.id) },
                                    modifier = Modifier.testTag("delete_build_btn_${build.id}")
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete build", tint = ForgeCrimson)
                                }
                            }
                        }

                        if (build.notes.isNotBlank()) {
                            Text(
                                text = build.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = ForgeTextSecondary
                            )
                        }

                        if (build.statRanges.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                build.statRanges.values.forEach { range ->
                                    StatRangeChip(range)
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { onShareBuild(build) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("share_build_btn_${build.id}")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = ArcaneCyan)
                            Spacer(Modifier.width(8.dp))
                            Text("Get Shareable Build Code", color = ArcaneCyan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRangeChip(range: StatTargetRange) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = ForgeSurfaceElevated,
        border = BorderStroke(1.dp, ForgeBorder)
    ) {
        Text(
            text = "${range.statType.shortLabel}: ${range.statType.formatPlainValue(range.minDesired)}–${range.statType.formatPlainValue(range.maxDesired)} (${range.priorityWeight.toInt()}x)",
            style = MaterialTheme.typography.labelSmall,
            color = ForgeTextPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ShareBuildCodeDialog(
    build: CustomBuild,
    shareCode: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    BackHandler(onBack = onDismiss)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = ForgeSlateDark,
            border = BorderStroke(1.dp, ArcaneCyan.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Share Build: ${build.name}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MoltenAmber
                )
                Text(
                    text = "Anyone with Hunter Forge can paste this code in Builds & Codes -> Upload / View Build Code to inspect and save your stat ranges.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ForgeTextSecondary
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ForgeObsidian,
                    border = BorderStroke(1.dp, ForgeBorder)
                ) {
                    Text(
                        text = shareCode,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = ArcaneCyan,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .testTag("generated_build_code_text")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            clipboard.setText(AnnotatedString(shareCode))
                            copied = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (copied) ForgeEmerald else MoltenAmber,
                            contentColor = ForgeObsidian
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("copy_build_code_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (copied) "Copied!" else "Copy Code", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Hunter Forge Build: ${build.name}")
                                putExtra(Intent.EXTRA_TEXT, shareCode)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Build Code"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("system_share_build_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = ArcaneCyan)
                        Spacer(Modifier.width(6.dp))
                        Text("Share", color = ArcaneCyan)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Done", color = ForgeTextSecondary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BuildEditorDialog(
    initialBuild: CustomBuild?,
    onDismiss: () -> Unit,
    onSave: (CustomBuild) -> Unit
) {
    var name by remember { mutableStateOf(initialBuild?.name ?: "") }
    var role by remember { mutableStateOf(initialBuild?.role ?: HeroRole.MAIN_CARRY) }
    var targetRace by remember { mutableStateOf(initialBuild?.targetRace ?: TargetRace.DEMON) }
    var notes by remember { mutableStateOf(initialBuild?.notes ?: "") }

    val enabledStats = remember {
        mutableStateMapOf<StatType, Boolean>().apply {
            StatType.entries.forEach { type ->
                put(type, initialBuild?.statRanges?.containsKey(type) == true)
            }
        }
    }
    val minInputs = remember {
        mutableStateMapOf<StatType, String>().apply {
            StatType.entries.forEach { type ->
                val existing = initialBuild?.statRanges?.get(type)?.minDesired
                val defaultMin = if (type.isFlatDecimal) "0.20" else (type.suggestedLoadoutMax * 0.6f).roundToInt().toString()
                put(
                    type,
                    existing?.let {
                        if (type.isFlatDecimal) String.format(Locale.US, "%.2f", it) else it.roundToInt().toString()
                    } ?: defaultMin
                )
            }
        }
    }
    val maxInputs = remember {
        mutableStateMapOf<StatType, String>().apply {
            StatType.entries.forEach { type ->
                val existing = initialBuild?.statRanges?.get(type)?.maxDesired
                val defaultMax = if (type.isFlatDecimal) "0.25" else type.suggestedLoadoutMax.roundToInt().toString()
                put(
                    type,
                    existing?.let {
                        if (type.isFlatDecimal) String.format(Locale.US, "%.2f", it) else it.roundToInt().toString()
                    } ?: defaultMax
                )
            }
        }
    }
    val priorityWeights = remember {
        mutableStateMapOf<StatType, Float>().apply {
            StatType.entries.forEach { type ->
                put(type, initialBuild?.statRanges?.get(type)?.priorityWeight ?: 2.0f)
            }
        }
    }

    BackHandler(onBack = onDismiss)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = ForgeSlateDark,
            border = BorderStroke(1.dp, ForgeBorder)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (initialBuild == null || initialBuild.id == 0L) "Create Build Profile" else "Edit Build Profile",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MoltenAmber
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Build Name (e.g. Chaos Executioner)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("build_name_input")
                )

                Text("Target Hero Role", style = MaterialTheme.typography.titleSmall, color = ForgeTextSecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HeroRole.entries.forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r.displayName) }
                        )
                    }
                }

                Text("Target Monster Race", style = MaterialTheme.typography.titleSmall, color = ForgeTextSecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TargetRace.entries.forEach { tr ->
                        FilterChip(
                            selected = targetRace == tr,
                            onClick = { targetRace = tr },
                            label = { Text(tr.displayName) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Strategy (optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = ForgeBorder.copy(alpha = 0.6f))
                Text(
                    text = "Desired Stat Ranges & Weights",
                    style = MaterialTheme.typography.titleSmall,
                    color = ArcaneCyan
                )
                Text(
                    text = "Check the stats this build cares about and set Min–Max desired totals across your full loadout.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ForgeTextSecondary
                )

                StatType.entries.forEach { statType ->
                    val checked = enabledStats[statType] == true
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (checked) ForgeSurface else ForgeObsidian.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (checked) ArcaneCyan.copy(alpha = 0.55f) else ForgeBorder.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { enabledStats[statType] = it },
                                        colors = CheckboxDefaults.colors(checkedColor = MoltenAmber)
                                    )
                                    Text(
                                        text = statType.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (checked) ForgeTextPrimary else ForgeTextSecondary
                                    )
                                }
                                if (checked) {
                                    val w = priorityWeights[statType] ?: 2.0f
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(1f, 2f, 3f).forEach { weightOption ->
                                            FilterChip(
                                                selected = kotlin.math.abs(w - weightOption) < 0.1f,
                                                onClick = { priorityWeights[statType] = weightOption },
                                                label = { Text("${weightOption.toInt()}x") }
                                            )
                                        }
                                    }
                                }
                            }

                            if (checked) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = minInputs[statType].orEmpty(),
                                        onValueChange = { minInputs[statType] = it },
                                        label = { Text("Min Desired") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = maxInputs[statType].orEmpty(),
                                        onValueChange = { maxInputs[statType] = it },
                                        label = { Text("Max Desired") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = ForgeTextSecondary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val builtRanges = mutableMapOf<StatType, StatTargetRange>()
                            StatType.entries.forEach { statType ->
                                if (enabledStats[statType] == true) {
                                    val parsedMin = minInputs[statType]?.toFloatOrNull()?.coerceAtLeast(0f) ?: 0f
                                    val parsedMaxRaw = maxInputs[statType]?.toFloatOrNull()?.coerceAtLeast(0f)
                                        ?: statType.suggestedLoadoutMax
                                    val safeMax = maxOf(parsedMin, parsedMaxRaw)
                                    builtRanges[statType] = StatTargetRange(
                                        statType = statType,
                                        minDesired = parsedMin,
                                        maxDesired = safeMax,
                                        priorityWeight = priorityWeights[statType] ?: 2.0f
                                    )
                                }
                            }
                            onSave(
                                CustomBuild(
                                    id = initialBuild?.id ?: 0L,
                                    name = name.trim().ifBlank { "${role.displayName} Build" },
                                    role = role,
                                    targetRace = targetRace,
                                    notes = notes.trim(),
                                    statRanges = builtRanges,
                                    createdAt = initialBuild?.createdAt ?: System.currentTimeMillis()
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MoltenAmber,
                            contentColor = ForgeObsidian
                        ),
                        modifier = Modifier.testTag("save_build_dialog_btn")
                    ) {
                        Text("Save Build")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroEditorDialog(
    initialHero: HeroProfile?,
    customBuilds: List<CustomBuild>,
    onDismiss: () -> Unit,
    onSave: (HeroProfile) -> Unit
) {
    var name by remember { mutableStateOf(initialHero?.name ?: "") }
    var baseClass by remember { mutableStateOf(initialHero?.baseClass ?: HeroBaseClass.BERSERKER) }
    var advancement by remember { mutableStateOf(initialHero?.advancement ?: HeroAdvancement.EXECUTIONER) }
    var role by remember { mutableStateOf(initialHero?.role ?: HeroRole.MAIN_CARRY) }
    var assignedBuildId by remember { mutableStateOf(initialHero?.assignedBuildId) }
    var trait by remember { mutableStateOf(initialHero?.trait ?: HeroTrait.HEROIC) }
    var archetype by remember { mutableStateOf(initialHero?.archetype ?: BuildArchetype.FIELD_CHAOS) }
    var targetRace by remember { mutableStateOf(initialHero?.targetRace ?: TargetRace.DEMON) }
    var runeSlots by remember { mutableIntStateOf(initialHero?.runeSlots ?: 5) }
    var quickStrike by remember { mutableStateOf(initialHero?.hasQuickStrikeBuff ?: true) }
    var guildAspd by remember { mutableFloatStateOf(initialHero?.guildAspdBuffPct ?: 5f) }
    var guildCrit by remember { mutableFloatStateOf(initialHero?.guildCritBuffPct ?: 5f) }

    BackHandler(onBack = onDismiss)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = ForgeSlateDark,
            border = BorderStroke(1.dp, ForgeBorder)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (initialHero == null) "Add Hero Profile" else "Edit Hero Profile",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MoltenAmber
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Hunter Name (e.g. Kaelen)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hero_name_input")
                )

                Text("Base Class", style = MaterialTheme.typography.titleSmall, color = ForgeTextSecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HeroBaseClass.entries.forEach { c ->
                        FilterChip(
                            selected = baseClass == c,
                            onClick = {
                                baseClass = c
                                advancement = HeroAdvancement.forClass(c).last()
                            },
                            label = { Text(c.displayName) }
                        )
                    }
                }

                Text("3rd Class Advancement", style = MaterialTheme.typography.titleSmall, color = ForgeTextSecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HeroAdvancement.forClass(baseClass).forEach { adv ->
                        FilterChip(
                            selected = advancement == adv,
                            onClick = { advancement = adv },
                            label = { Text(adv.displayName) }
                        )
                    }
                }

                Text("Hero Role", style = MaterialTheme.typography.titleSmall, color = MoltenAmber)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HeroRole.entries.forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r.displayName) }
                        )
                    }
                }

                Text("Assigned Custom Build", style = MaterialTheme.typography.titleSmall, color = ArcaneCyan)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = assignedBuildId == null,
                        onClick = { assignedBuildId = null },
                        label = { Text("Auto (Class Meta)") }
                    )
                    customBuilds.forEach { build ->
                        FilterChip(
                            selected = assignedBuildId == build.id,
                            onClick = { assignedBuildId = build.id },
                            label = { Text(build.name) }
                        )
                    }
                }

                Text("Characteristic / Trait", style = MaterialTheme.typography.titleSmall, color = ForgeTextSecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HeroTrait.entries.forEach { t ->
                        FilterChip(
                            selected = trait == t,
                            onClick = { trait = t },
                            label = { Text(t.displayName) }
                        )
                    }
                }

                Text("Build Archetype", style = MaterialTheme.typography.titleSmall, color = ForgeTextSecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BuildArchetype.entries.forEach { b ->
                        FilterChip(
                            selected = archetype == b,
                            onClick = { archetype = b },
                            label = { Text(b.displayName) }
                        )
                    }
                }

                Text("Target Monster Race", style = MaterialTheme.typography.titleSmall, color = ForgeTextSecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TargetRace.entries.forEach { r ->
                        FilterChip(
                            selected = targetRace == r,
                            onClick = { targetRace = r },
                            label = { Text(r.displayName) }
                        )
                    }
                }

                Text("Unlocked Rune Slots: $runeSlots", style = MaterialTheme.typography.titleSmall, color = ArcaneCyan)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..5).forEach { count ->
                        FilterChip(
                            selected = runeSlots == count,
                            onClick = { runeSlots = count },
                            label = { Text("$count") }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Quick Strike / Fury Buff", style = MaterialTheme.typography.titleSmall, color = ForgeTextPrimary)
                        Text("Adds +0.06 ASPD equivalent in cap math", style = MaterialTheme.typography.bodySmall, color = ForgeTextSecondary)
                    }
                    Switch(checked = quickStrike, onCheckedChange = { quickStrike = it })
                }

                Text("Guild Attack Speed Buff: ${guildAspd.roundToInt()}%", style = MaterialTheme.typography.bodySmall, color = ForgeTextSecondary)
                Slider(
                    value = guildAspd,
                    onValueChange = { guildAspd = it },
                    valueRange = 0f..10f,
                    steps = 9
                )

                Text("Guild Crit Buff: ${guildCrit.roundToInt()}%", style = MaterialTheme.typography.bodySmall, color = ForgeTextSecondary)
                Slider(
                    value = guildCrit,
                    onValueChange = { guildCrit = it },
                    valueRange = 0f..10f,
                    steps = 9
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = ForgeTextSecondary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                HeroProfile(
                                    id = initialHero?.id ?: 0L,
                                    name = name.trim().ifBlank { advancement.displayName },
                                    baseClass = baseClass,
                                    advancement = advancement,
                                    role = role,
                                    assignedBuildId = assignedBuildId,
                                    trait = trait,
                                    archetype = archetype,
                                    targetRace = targetRace,
                                    runeSlots = runeSlots,
                                    hasQuickStrikeBuff = quickStrike,
                                    guildAspdBuffPct = guildAspd,
                                    guildCritBuffPct = guildCrit,
                                    createdAt = initialHero?.createdAt ?: System.currentTimeMillis()
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MoltenAmber,
                            contentColor = ForgeObsidian
                        ),
                        modifier = Modifier.testTag("save_hero_dialog_btn")
                    ) {
                        Text("Save Hero")
                    }
                }
            }
        }
    }
}
