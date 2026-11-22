package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.EhtOcrParser
import com.example.domain.GearSlot
import com.example.domain.GearTier
import com.example.domain.ItemKind
import com.example.domain.ScanSource
import com.example.domain.StatMap
import com.example.domain.StatType
import com.example.domain.VaultItem
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.ForgeBorder
import com.example.ui.theme.ForgeObsidian
import com.example.ui.theme.ForgeSlateDark
import com.example.ui.theme.ForgeTextSecondary
import com.example.ui.theme.MoltenAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemEditorDialog(
    initialItem: VaultItem?,
    onDismiss: () -> Unit,
    onSave: (VaultItem) -> Unit
) {
    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var kind by remember { mutableStateOf(initialItem?.kind ?: ItemKind.GEAR) }
    var slot by remember { mutableStateOf(initialItem?.slot ?: GearSlot.WEAPON) }
    var tier by remember { mutableStateOf(initialItem?.tier ?: GearTier.PRIMAL) }
    var enhancementText by remember { mutableStateOf((initialItem?.enhancementLevel ?: 0).toString()) }
    var statValues by remember {
        mutableStateOf(
            StatType.entries.associateWith { type ->
                val v = initialItem?.stats?.valueOf(type) ?: 0f
                if (kotlin.math.abs(v) > 0.001f) {
                    if (type.isFlatDecimal) String.format(java.util.Locale.US, "%.2f", v)
                    else v.toString().removeSuffix(".0")
                } else ""
            }
        )
    }
    var ocrPasteText by remember { mutableStateOf(initialItem?.rawOcrText ?: "") }
    var showOcrBox by remember { mutableStateOf(false) }

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialItem == null) "Log Gear / Rune" else "Edit Item Stats",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MoltenAmber
                    )
                    TextButton(onClick = { showOcrBox = !showOcrBox }) {
                        Text(if (showOcrBox) "Hide OCR Paste" else "Parse OCR Text", color = ArcaneCyan)
                    }
                }

                if (showOcrBox) {
                    OutlinedTextField(
                        value = ocrPasteText,
                        onValueChange = { ocrPasteText = it },
                        label = { Text("Paste EHT tooltip text (e.g. Primal Axe, ATK +22%, Crit DMG +38%)") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_ocr_paste_input")
                    )
                    OutlinedButton(
                        onClick = {
                            val parsed = EhtOcrParser.parse(ocrPasteText)
                            if (parsed != null) {
                                name = parsed.name
                                kind = parsed.kind
                                slot = parsed.slot
                                tier = parsed.tier
                                enhancementText = parsed.enhancementLevel.toString()
                                statValues = StatType.entries.associateWith { type ->
                                    val v = parsed.stats.valueOf(type)
                                    if (kotlin.math.abs(v) > 0.001f) {
                                        if (type.isFlatDecimal) String.format(java.util.Locale.US, "%.2f", v)
                                        else v.toString().removeSuffix(".0")
                                    } else ""
                                }
                            }
                        },
                        modifier = Modifier.testTag("apply_ocr_parse_btn")
                    ) {
                        Text("Extract Stats via OCR Parser")
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_name_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ItemKind.entries.forEach { k ->
                        FilterChip(
                            selected = kind == k,
                            onClick = {
                                kind = k
                                slot = if (k == ItemKind.RUNE) GearSlot.RUNE else GearSlot.WEAPON
                            },
                            label = { Text(k.displayName) }
                        )
                    }
                }

                if (kind == ItemKind.GEAR) {
                    Text("Slot", style = MaterialTheme.typography.titleSmall, color = ForgeTextSecondary)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GearSlot.equipmentSlots.forEach { s ->
                            FilterChip(
                                selected = slot == s,
                                onClick = { slot = s },
                                label = { Text(s.displayName) }
                            )
                        }
                    }
                }

                Text("Tier / Rarity", style = MaterialTheme.typography.titleSmall, color = ForgeTextSecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GearTier.entries.forEach { t ->
                        FilterChip(
                            selected = tier == t,
                            onClick = { tier = t },
                            label = { Text(t.displayName) }
                        )
                    }
                }

                OutlinedTextField(
                    value = enhancementText,
                    onValueChange = { enhancementText = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Enhancement (+0 to +25)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Item Stats", style = MaterialTheme.typography.titleSmall, color = MoltenAmber)

                // Key combat stats grid (2 per row)
                val pairs = StatType.entries.chunked(2)
                pairs.forEach { rowTypes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowTypes.forEach { statType ->
                            OutlinedTextField(
                                value = statValues[statType].orEmpty(),
                                onValueChange = { input ->
                                    val filtered = input.replace('−', '-').filterIndexed { index, char ->
                                        char.isDigit() || char == '.' || (index == 0 && char == '-')
                                    }
                                    statValues = statValues + (statType to filtered)
                                },
                                label = { Text(statType.shortLabel) },
                                placeholder = { Text(if (statType.isFlatDecimal) "0.12" else "18") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowTypes.size == 1) Spacer(Modifier.weight(1f))
                    }
                }

                Spacer(Modifier.height(4.dp))
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
                            var map = StatMap()
                            StatType.entries.forEach { t ->
                                val parsedVal = statValues[t]?.toFloatOrNull() ?: 0f
                                if (parsedVal.isFinite() && parsedVal != 0f) {
                                    map = map.withStat(t, parsedVal)
                                }
                            }
                            val finalName = name.trim().ifBlank {
                                if (kind == ItemKind.RUNE) "${tier.displayName} Rune"
                                else "${tier.displayName} ${slot.displayName}"
                            }
                            onSave(
                                VaultItem(
                                    id = initialItem?.id ?: 0L,
                                    name = finalName,
                                    kind = kind,
                                    slot = if (kind == ItemKind.RUNE) GearSlot.RUNE else slot,
                                    tier = tier,
                                    enhancementLevel = (enhancementText.toIntOrNull() ?: 0).coerceIn(0, 30),
                                    stats = map,
                                    rawOcrText = ocrPasteText,
                                    source = initialItem?.source ?: ScanSource.MANUAL,
                                    equippedHeroId = initialItem?.equippedHeroId,
                                    createdAt = initialItem?.createdAt ?: System.currentTimeMillis()
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MoltenAmber,
                            contentColor = ForgeObsidian
                        ),
                        modifier = Modifier.testTag("save_item_dialog_btn")
                    ) {
                        Text("Save to Vault")
                    }
                }
            }
        }
    }
}
