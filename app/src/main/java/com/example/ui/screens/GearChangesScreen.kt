package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.GearChangeAction
import com.example.domain.HeroChangeGroup
import com.example.domain.RosterChangePlan
import com.example.domain.VaultItem
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

@Composable
fun GearChangesScreen(
    plan: RosterChangePlan,
    vaultCount: Int,
    onApplySingleChange: (GearChangeAction) -> Unit,
    onApplyHeroChanges: (HeroChangeGroup) -> Unit,
    onApplyAllChanges: (RosterChangePlan) -> Unit,
    onNavigateToOptimize: () -> Unit,
    onNavigateToScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPendingOnly by rememberSaveable { mutableStateOf(true) }

    val displayedGroups = if (showPendingOnly) {
        plan.heroGroups.filter { it.changes.isNotEmpty() }
    } else {
        plan.heroGroups
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        when {
            plan.heroGroups.isEmpty() -> {
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
                                text = "No Hunters in Roster Yet",
                                style = MaterialTheme.typography.headlineSmall,
                                color = ForgeTextPrimary
                            )
                            Text(
                                text = "Add your Evil Hunter Tycoon heroes in Optimize first. Once added, this tab shows every suggested gear & rune swap across all heroes in one place.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ForgeTextSecondary
                            )
                            Button(
                                onClick = onNavigateToOptimize,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MoltenAmber,
                                    contentColor = ForgeObsidian
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("go_to_optimize_from_changes_btn")
                            ) {
                                Text("Go to Optimize Roster", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            vaultCount == 0 -> {
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
                                imageVector = Icons.Default.Radar,
                                contentDescription = null,
                                tint = ArcaneCyan,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Vault Is Empty",
                                style = MaterialTheme.typography.headlineSmall,
                                color = ForgeTextPrimary
                            )
                            Text(
                                text = "Scan or add gear and runes first so Hunter Forge can generate swap instructions for your ${plan.heroGroups.size} hero(es).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ForgeTextSecondary
                            )
                            Button(
                                onClick = onNavigateToScan,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ArcaneCyan,
                                    contentColor = ForgeObsidian
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("go_to_scan_from_changes_btn")
                            ) {
                                Text("Scan Gear Now", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 720.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top Summary & Batch Apply Card
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = ForgeSlateDark),
                            border = BorderStroke(
                                1.dp,
                                if (plan.totalChangesCount > 0) MoltenAmber.copy(alpha = 0.65f) else ForgeEmerald.copy(alpha = 0.65f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (plan.totalChangesCount > 0) Icons.Default.SwapHoriz else Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (plan.totalChangesCount > 0) MoltenAmber else ForgeEmerald,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Town Gear Swap Checklist",
                                                style = MaterialTheme.typography.titleLarge,
                                                color = ForgeTextPrimary
                                            )
                                            Text(
                                                text = if (plan.totalChangesCount > 0) {
                                                    "${plan.totalChangesCount} suggested change(s) across ${plan.heroesNeedingChangesCount} hero(es)"
                                                } else {
                                                    "All ${plan.heroGroups.size} hunter(s) are already wearing their optimal gear!"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (plan.totalChangesCount > 0) ArcaneCyan else ForgeEmerald
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilterChip(
                                        selected = showPendingOnly,
                                        onClick = { showPendingOnly = true },
                                        label = { Text("Pending (${plan.heroesNeedingChangesCount})") },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("changes_filter_pending")
                                    )
                                    FilterChip(
                                        selected = !showPendingOnly,
                                        onClick = { showPendingOnly = false },
                                        label = { Text("All Heroes (${plan.heroGroups.size})") },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("changes_filter_all")
                                    )
                                }

                                if (plan.totalChangesCount > 0) {
                                    Button(
                                        onClick = { onApplyAllChanges(plan) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ForgeEmerald,
                                            contentColor = ForgeObsidian
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .minimumInteractiveComponentSize()
                                            .testTag("apply_all_changes_btn")
                                    ) {
                                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Mark All ${plan.totalChangesCount} Swaps Done", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    if (displayedGroups.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = ForgeSurface),
                                border = BorderStroke(1.dp, ForgeEmerald.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = ForgeEmerald,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Text(
                                        text = "Zero Pending Gear Swaps",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ForgeEmerald
                                    )
                                    Text(
                                        text = "Every hunter's equipped gear matches the latest min-max recommendation. Tap 'All Heroes' above to verify current loadouts, or scan new items in game.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ForgeTextSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        items(displayedGroups, key = { it.hero.id }) { group ->
                            HeroSwapGroupCard(
                                group = group,
                                onApplySingleChange = onApplySingleChange,
                                onApplyHeroChanges = { onApplyHeroChanges(group) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroSwapGroupCard(
    group: HeroChangeGroup,
    onApplySingleChange: (GearChangeAction) -> Unit,
    onApplyHeroChanges: () -> Unit
) {
    val hasChanges = group.changes.isNotEmpty()
    val res = group.recommendedLoadout

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ForgeSurface),
        border = BorderStroke(
            1.dp,
            if (hasChanges) MoltenAmber.copy(alpha = 0.5f) else ForgeEmerald.copy(alpha = 0.45f)
        )
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
                        text = "${group.hero.name} • ${group.hero.advancement.displayName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = ForgeTextPrimary
                    )
                    val buildTag = group.assignedBuild?.let { " • Build: ${it.name}" } ?: ""
                    Text(
                        text = "${group.hero.role.displayName}$buildTag",
                        style = MaterialTheme.typography.bodySmall,
                        color = ArcaneCyan
                    )
                    Text(
                        text = "Target Caps: ASPD ${String.format(Locale.US, "%.2f", res.effectiveTotals.atkSpd)}/0.25 • Crit ${res.effectiveTotals.critChance.roundToInt()}% • Eva ${res.effectiveTotals.evasion.roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = ForgeTextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (hasChanges) MoltenAmber.copy(alpha = 0.16f) else ForgeEmerald.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, if (hasChanges) MoltenAmber else ForgeEmerald)
                ) {
                    Text(
                        text = if (hasChanges) "${group.changes.size} SWAP${if (group.changes.size > 1) "S" else ""}" else "READY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (hasChanges) MoltenAmber else ForgeEmerald,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (hasChanges) {
                OutlinedButton(
                    onClick = onApplyHeroChanges,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("apply_hero_changes_btn_${group.hero.id}")
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp), tint = ForgeEmerald)
                    Spacer(Modifier.width(6.dp))
                    Text("Apply All ${group.changes.size} Swap(s) for ${group.hero.name}", color = ForgeEmerald)
                }

                HorizontalDivider(color = ForgeBorder.copy(alpha = 0.5f))

                group.changes.forEach { change ->
                    SwapActionRow(
                        change = change,
                        onDone = { onApplySingleChange(change) }
                    )
                }
            } else {
                Text(
                    text = "All equipped gear and runes already match the best build (${res.combatScore.roundToInt()} pts).",
                    style = MaterialTheme.typography.bodySmall,
                    color = ForgeEmerald
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SwapActionRow(
    change: GearChangeAction,
    onDone: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ForgeSlateDark,
        border = BorderStroke(1.dp, ForgeBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ForgeSurfaceElevated,
                        border = BorderStroke(1.dp, ArcaneCyan.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = change.slotLabel.uppercase(Locale.US),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ArcaneCyan,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    val sourceLabel = change.previousOwnerHeroName?.let { "Take from $it" } ?: "From Bag / Vault"
                    val sourceColor = if (change.previousOwnerHeroName != null) MoltenAmber else ForgeEmerald
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = sourceColor.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, sourceColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = sourceLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = sourceColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                Button(
                    onClick = onDone,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForgeEmerald,
                        contentColor = ForgeObsidian
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("apply_change_btn_${change.id}")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Done", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }

            // Old Item -> New Item Swap Instruction
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "UNEQUIP",
                        style = MaterialTheme.typography.labelSmall,
                        color = ForgeTextSecondary
                    )
                    Text(
                        text = formatCompactItemName(change.currentItem) ?: "Empty Slot",
                        style = MaterialTheme.typography.bodySmall,
                        color = ForgeTextSecondary,
                        textDecoration = if (change.currentItem != null) TextDecoration.LineThrough else null,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Replace with",
                    tint = MoltenAmber,
                    modifier = Modifier.size(18.dp)
                )

                Column(modifier = Modifier.weight(1.25f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "EQUIP",
                            style = MaterialTheme.typography.labelSmall,
                            color = ForgeEmerald,
                            fontWeight = FontWeight.Bold
                        )
                        TierBadge(change.suggestedItem.tier)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = formatCompactItemName(change.suggestedItem) ?: "",
                        style = MaterialTheme.typography.titleSmall,
                        color = ForgeTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (change.statGainHighlights.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    change.statGainHighlights.forEach { deltaText ->
                        val isGain = deltaText.contains("+")
                        val chipColor = if (isGain) ForgeEmerald else ForgeCrimson
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = chipColor.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, chipColor.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = deltaText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = chipColor,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatCompactItemName(item: VaultItem?): String? {
    if (item == null) return null
    val prefix = if (item.enhancementLevel > 0) "+${item.enhancementLevel} " else ""
    return "$prefix${item.name}"
}
