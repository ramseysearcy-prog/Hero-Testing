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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.CapProgress
import com.example.domain.CommunityMinMaxEngine
import com.example.domain.HeroTrait
import com.example.ui.components.CapGaugeBar
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.ForgeBorder
import com.example.ui.theme.ForgeEmerald
import com.example.ui.theme.ForgeSlateDark
import com.example.ui.theme.ForgeSurface
import com.example.ui.theme.ForgeTextPrimary
import com.example.ui.theme.ForgeTextSecondary
import com.example.ui.theme.MoltenAmber
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GuidesScreen(
    modifier: Modifier = Modifier
) {
    var calcTrait by remember { mutableStateOf(HeroTrait.HEROIC) }
    var calcQuickStrike by remember { mutableStateOf(true) }
    var calcGearAspd by remember { mutableFloatStateOf(0.14f) }
    var calcGearCrit by remember { mutableFloatStateOf(32f) }
    var calcGearEva by remember { mutableFloatStateOf(26f) }

    val totalAspd = calcTrait.bonusStats.atkSpd + (if (calcQuickStrike) 0.06f else 0f) + calcGearAspd
    val totalCrit = calcTrait.bonusStats.critChance + calcGearCrit
    val totalEva = calcTrait.bonusStats.evasion + calcGearEva

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Community Min-Max Field Manual",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MoltenAmber
                )
                Text(
                    text = "Hard caps, 3rd class priorities, and roll-quality rules built directly into Hunter Forge's optimizer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ForgeTextSecondary
                )
            }

            // Interactive Breakpoint Sandbox Calculator
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = ForgeSlateDark),
                    border = BorderStroke(1.dp, ArcaneCyan.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Interactive Cap & Breakpoint Calculator",
                            style = MaterialTheme.typography.titleMedium,
                            color = ArcaneCyan
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(HeroTrait.HEROIC, HeroTrait.SWIFT, HeroTrait.SHARP, HeroTrait.NIMBLE, HeroTrait.NEUTRAL).forEach { t ->
                                FilterChip(
                                    selected = calcTrait == t,
                                    onClick = { calcTrait = t },
                                    label = { Text(t.displayName) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Quick Strike / Fury (+0.06 ASPD)", style = MaterialTheme.typography.bodySmall, color = ForgeTextPrimary)
                            Switch(checked = calcQuickStrike, onCheckedChange = { calcQuickStrike = it })
                        }

                        Text("Gear + Rune ASPD: ${String.format(java.util.Locale.US, "+%.2f", calcGearAspd)}", style = MaterialTheme.typography.bodySmall, color = ForgeTextSecondary)
                        Slider(value = calcGearAspd, onValueChange = { calcGearAspd = it }, valueRange = 0f..0.30f)

                        Text("Gear + Rune Crit Chance: +${calcGearCrit.roundToInt()}%", style = MaterialTheme.typography.bodySmall, color = ForgeTextSecondary)
                        Slider(value = calcGearCrit, onValueChange = { calcGearCrit = it }, valueRange = 0f..60f)

                        Text("Gear + Rune Evasion: +${calcGearEva.roundToInt()}%", style = MaterialTheme.typography.bodySmall, color = ForgeTextSecondary)
                        Slider(value = calcGearEva, onValueChange = { calcGearEva = it }, valueRange = 0f..50f)

                        CapGaugeBar(
                            CapProgress(
                                label = "Attack Speed",
                                rawValue = totalAspd,
                                effectiveValue = min(totalAspd, CommunityMinMaxEngine.CAP_ASPD_BONUS),
                                capValue = CommunityMinMaxEngine.CAP_ASPD_BONUS,
                                isFlatDecimal = true,
                                isOvercapped = totalAspd > CommunityMinMaxEngine.CAP_ASPD_BONUS + 0.01f
                            )
                        )
                        CapGaugeBar(
                            CapProgress(
                                label = "Crit Chance",
                                rawValue = totalCrit,
                                effectiveValue = min(totalCrit, CommunityMinMaxEngine.CAP_CRIT_CHANCE),
                                capValue = CommunityMinMaxEngine.CAP_CRIT_CHANCE,
                                isFlatDecimal = false,
                                isOvercapped = totalCrit > CommunityMinMaxEngine.CAP_CRIT_CHANCE + 1f
                            )
                        )
                        CapGaugeBar(
                            CapProgress(
                                label = "Evasion",
                                rawValue = totalEva,
                                effectiveValue = min(totalEva, CommunityMinMaxEngine.CAP_EVASION),
                                capValue = CommunityMinMaxEngine.CAP_EVASION,
                                isFlatDecimal = false,
                                isOvercapped = totalEva > CommunityMinMaxEngine.CAP_EVASION + 1f
                            )
                        )
                    }
                }
            }

            items(CommunityMinMaxEngine.communityGuides, key = { it.id }) { guide ->
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
                            Text(
                                text = guide.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = ForgeTextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MoltenAmber.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, MoltenAmber.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = guide.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MoltenAmber,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = guide.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = ArcaneCyan
                        )

                        Spacer(Modifier.height(2.dp))
                        guide.keyRules.forEach { rule ->
                            Text(
                                text = "• $rule",
                                style = MaterialTheme.typography.bodySmall,
                                color = ForgeTextSecondary
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ForgeSlateDark,
                    border = BorderStroke(1.dp, ForgeEmerald.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "Tip: All Vault items, Hero rosters, and equipped loadouts are saved locally on your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ForgeEmerald,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                }
            }
        }
    }
}
