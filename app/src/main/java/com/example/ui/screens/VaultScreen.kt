package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.CommunityMinMaxEngine
import com.example.domain.CustomBuild
import com.example.domain.GearSlot
import com.example.domain.HeroProfile
import com.example.domain.ItemComparisonResult
import com.example.domain.ItemKind
import com.example.domain.VaultItem
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
fun VaultScreen(
    items: List<VaultItem>,
    heroes: List<HeroProfile>,
    customBuilds: List<CustomBuild>,
    selectedBuildFilter: CustomBuild?,
    compareItemAId: Long?,
    compareItemBId: Long?,
    comparisonResult: ItemComparisonResult?,
    onSelectBuildFilter: (Long?) -> Unit,
    onToggleCompare: (VaultItem) -> Unit,
    onClearCompare: () -> Unit,
    onSaveItem: (VaultItem) -> Unit,
    onDeleteItem: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedKind by remember { mutableStateOf<ItemKind?>(null) }
    var selectedSlot by remember { mutableStateOf<GearSlot?>(null) }
    var editingItem by remember { mutableStateOf<VaultItem?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showComparePanel by remember { mutableStateOf(false) }

    val heroNamesById = remember(heroes) { heroes.associate { it.id to it.name } }

    val filteredItems = remember(items, searchQuery, selectedKind, selectedSlot, selectedBuildFilter) {
        val base = items.filter { item ->
            val matchesKind = selectedKind == null || item.kind == selectedKind
            val matchesSlot = selectedSlot == null || item.slot == selectedSlot
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                item.name.lowercase().contains(q) ||
                item.tier.displayName.lowercase().contains(q) ||
                item.slot.displayName.lowercase().contains(q) ||
                item.stats.nonZeroEntries().any { it.first.label.lowercase().contains(q) }
            matchesKind && matchesSlot && matchesQuery
        }
        if (selectedBuildFilter != null) {
            base.sortedByDescending { CommunityMinMaxEngine.evaluateItemFitForBuild(it, selectedBuildFilter).fitPercent }
        } else {
            base
        }
    }

    if (showComparePanel && comparisonResult != null) {
        BackHandler { showComparePanel = false }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
        ) {
            if (compareItemAId != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = ForgeSurfaceElevated,
                    border = BorderStroke(1.dp, ArcaneCyan)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val count = listOfNotNull(compareItemAId, compareItemBId).size
                            Text(
                                text = if (count == 1) "Compare: 1 / 2 selected — tap another item" else "2 items ready to compare!",
                                style = MaterialTheme.typography.titleSmall,
                                color = ArcaneCyan
                            )
                        }
                        if (comparisonResult != null) {
                            Button(
                                onClick = { showComparePanel = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MoltenAmber,
                                    contentColor = ForgeObsidian
                                ),
                                modifier = Modifier.testTag("open_compare_view_btn")
                            ) {
                                Text("Compare Now")
                            }
                        }
                        IconButton(onClick = onClearCompare) {
                            Icon(Icons.Default.Close, contentDescription = "Clear comparison", tint = ForgeTextSecondary)
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("Search name, tier, or stat (e.g. Crit DMG, Demon)...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vault_search_input")
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedKind == null,
                        onClick = { selectedKind = null; selectedSlot = null },
                        label = { Text("All (${items.size})") }
                    )
                    ItemKind.entries.forEach { kind ->
                        val count = items.count { it.kind == kind }
                        FilterChip(
                            selected = selectedKind == kind,
                            onClick = {
                                selectedKind = if (selectedKind == kind) null else kind
                                if (selectedKind == ItemKind.RUNE) selectedSlot = GearSlot.RUNE
                                else if (selectedSlot == GearSlot.RUNE) selectedSlot = null
                            },
                            label = { Text("${kind.displayName} ($count)") }
                        )
                    }
                }

                if (customBuilds.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Build Fit:", style = MaterialTheme.typography.labelSmall, color = ArcaneCyan)
                        customBuilds.forEach { b ->
                            FilterChip(
                                selected = selectedBuildFilter?.id == b.id,
                                onClick = { onSelectBuildFilter(b.id) },
                                label = { Text(b.name) }
                            )
                        }
                    }
                }

                if (selectedKind != ItemKind.RUNE) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GearSlot.equipmentSlots.forEach { slot ->
                            FilterChip(
                                selected = selectedSlot == slot,
                                onClick = { selectedSlot = if (selectedSlot == slot) null else slot },
                                label = { Text(slot.displayName) }
                            )
                        }
                    }
                }
            }

            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = ForgeTextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (items.isEmpty()) "Your Vault is Empty" else "No Matching Items",
                            style = MaterialTheme.typography.titleMedium,
                            color = ForgeTextPrimary
                        )
                        Text(
                            text = if (items.isEmpty()) {
                                "Use Live Monitor, Video OCR, or tap + below to log your Evil Hunter Tycoon gear and runes."
                            } else {
                                "Try clearing your search or slot filter."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = ForgeTextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        val eval = remember(item) { CommunityMinMaxEngine.evaluateItemRoll(item) }
                        val buildFit = remember(item, selectedBuildFilter) {
                            selectedBuildFilter?.let { CommunityMinMaxEngine.evaluateItemFitForBuild(item, it) }
                        }
                        val isComparing = item.id == compareItemAId || item.id == compareItemBId
                        val equippedName = item.equippedHeroId?.let { heroNamesById[it] }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = ForgeSurface),
                            border = BorderStroke(
                                width = if (isComparing) 2.dp else 1.dp,
                                color = if (isComparing) ArcaneCyan else ForgeBorder
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (item.enhancementLevel > 0) "+${item.enhancementLevel} ${item.name}" else item.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = ForgeTextPrimary
                                        )
                                        Text(
                                            text = buildString {
                                                append("${item.slot.displayName} • ${item.source.displayName}")
                                                if (equippedName != null) append(" • Equipped: $equippedName")
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (equippedName != null) ForgeEmerald else ForgeTextSecondary
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TierBadge(item.tier)
                                        RollGradeBadge(eval)
                                    }
                                }

                                if (buildFit != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = ArcaneCyan.copy(alpha = 0.14f),
                                        border = BorderStroke(1.dp, ArcaneCyan.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = "Fits '${buildFit.buildName}': ${buildFit.fitPercent}%" +
                                                if (buildFit.matchedStats.isNotEmpty()) " (${buildFit.matchedStats.joinToString(", ")})" else "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ArcaneCyan,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                StatPillsRow(item.stats)

                                Text(
                                    text = eval.verdict,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ForgeTextSecondary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { onToggleCompare(item) },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("compare_item_${item.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Balance,
                                            contentDescription = null,
                                            tint = if (isComparing) ArcaneCyan else MoltenAmber,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = when (item.id) {
                                                compareItemAId -> "Slot A Selected"
                                                compareItemBId -> "Slot B Selected"
                                                else -> "Compare"
                                            },
                                            color = if (isComparing) ArcaneCyan else ForgeTextPrimary
                                        )
                                    }

                                    Row {
                                        IconButton(
                                            onClick = { editingItem = item },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit item", tint = ForgeTextSecondary)
                                        }
                                        IconButton(
                                            onClick = { onDeleteItem(item.id) },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete item", tint = ForgeCrimson)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MoltenAmber,
            contentColor = ForgeObsidian,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
                .testTag("vault_add_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add gear or rune")
        }

        if (showComparePanel && comparisonResult != null) {
            ComparatorOverlay(
                result = comparisonResult,
                onClose = { showComparePanel = false }
            )
        }
    }

    if (showAddDialog) {
        ItemEditorDialog(
            initialItem = null,
            onDismiss = { showAddDialog = false },
            onSave = { item ->
                onSaveItem(item)
                showAddDialog = false
            }
        )
    }

    editingItem?.let { current ->
        ItemEditorDialog(
            initialItem = current,
            onDismiss = { editingItem = null },
            onSave = { updated ->
                onSaveItem(updated)
                editingItem = null
            }
        )
    }
}

@Composable
private fun ComparatorOverlay(
    result: ItemComparisonResult,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ForgeObsidian.copy(alpha = 0.97f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stat Comparator",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MoltenAmber
                )
                TextButton(onClick = onClose, modifier = Modifier.testTag("close_compare_btn")) {
                    Text("Close", color = ArcaneCyan)
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ForgeSurfaceElevated,
                border = BorderStroke(1.dp, MoltenAmber.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(result.winnerLabel, style = MaterialTheme.typography.titleMedium, color = MoltenAmber)
                    Text(result.recommendedForHero, style = MaterialTheme.typography.bodySmall, color = ForgeTextSecondary)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CompareHeaderCard("ITEM A", result.itemA, result.evalA.gradeLabel, result.evalA.efficiencyPct, Modifier.weight(1f))
                CompareHeaderCard("ITEM B", result.itemB, result.evalB.gradeLabel, result.evalB.efficiencyPct, Modifier.weight(1f))
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ForgeSurface),
                border = BorderStroke(1.dp, ForgeBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Stat", style = MaterialTheme.typography.labelMedium, color = ForgeTextSecondary, modifier = Modifier.weight(1.2f))
                        Text("Item A", style = MaterialTheme.typography.labelMedium, color = ForgeTextSecondary, modifier = Modifier.weight(0.9f))
                        Text("Item B", style = MaterialTheme.typography.labelMedium, color = ForgeTextSecondary, modifier = Modifier.weight(0.9f))
                        Text("Delta", style = MaterialTheme.typography.labelMedium, color = ForgeTextSecondary, modifier = Modifier.weight(0.9f))
                    }
                    HorizontalDivider(color = ForgeBorder)
                    result.statRows.forEach { row ->
                        val deltaColor = when {
                            row.deltaBMinusA > 0.005f -> ForgeEmerald
                            row.deltaBMinusA < -0.005f -> ForgeCrimson
                            else -> ForgeTextSecondary
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ForgeSlateDark.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(row.statType.shortLabel, style = MaterialTheme.typography.bodySmall, color = ForgeTextPrimary, modifier = Modifier.weight(1.2f))
                            Text(
                                text = if (row.valueA > 0.001f) row.statType.formatValue(row.valueA) else "—",
                                style = MaterialTheme.typography.labelMedium,
                                color = ForgeTextPrimary,
                                modifier = Modifier.weight(0.9f)
                            )
                            Text(
                                text = if (row.valueB > 0.001f) row.statType.formatValue(row.valueB) else "—",
                                style = MaterialTheme.typography.labelMedium,
                                color = ForgeTextPrimary,
                                modifier = Modifier.weight(0.9f)
                            )
                            Text(
                                text = row.statType.formatDelta(row.deltaBMinusA),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = deltaColor,
                                modifier = Modifier.weight(0.9f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareHeaderCard(
    tag: String,
    item: VaultItem,
    grade: String,
    efficiency: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ForgeSlateDark),
        border = BorderStroke(1.dp, ForgeBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(tag, style = MaterialTheme.typography.labelSmall, color = ArcaneCyan)
            Text(item.name, style = MaterialTheme.typography.titleSmall, color = ForgeTextPrimary)
            TierBadge(item.tier)
            Text("$grade Roll • $efficiency%", style = MaterialTheme.typography.labelMedium, color = MoltenAmber)
        }
    }
}
