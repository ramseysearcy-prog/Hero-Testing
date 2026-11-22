package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.CapProgress
import com.example.domain.GearTier
import com.example.domain.RollEvaluation
import com.example.domain.StatMap
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
import com.example.ui.theme.TierBoss
import com.example.ui.theme.TierChaos
import com.example.ui.theme.TierHeroic
import com.example.ui.theme.TierLegendary
import com.example.ui.theme.TierOriginal
import com.example.ui.theme.TierPrimal
import com.example.ui.theme.TierUltimate
import java.util.Locale

fun GearTier.color(): Color = when (this) {
    GearTier.CHAOS -> TierChaos
    GearTier.ORIGINAL -> TierOriginal
    GearTier.PRIMAL -> TierPrimal
    GearTier.BOSS -> TierBoss
    GearTier.ULTIMATE -> TierUltimate
    GearTier.LEGENDARY -> TierLegendary
    GearTier.HEROIC -> TierHeroic
}

@Composable
fun TierBadge(tier: GearTier, modifier: Modifier = Modifier) {
    val tierColor = tier.color()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = tierColor.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, tierColor.copy(alpha = 0.65f))
    ) {
        Text(
            text = tier.displayName.uppercase(Locale.US),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = tierColor,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun RollGradeBadge(eval: RollEvaluation, modifier: Modifier = Modifier) {
    val color = when {
        eval.efficiencyPct >= 90 -> MoltenAmber
        eval.efficiencyPct >= 80 -> TierChaos
        eval.efficiencyPct >= 68 -> ForgeEmerald
        eval.efficiencyPct >= 52 -> ArcaneCyan
        else -> ForgeTextSecondary
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.6f))
    ) {
        Text(
            text = "${eval.gradeLabel} • ${eval.efficiencyPct}%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatPillsRow(stats: StatMap, modifier: Modifier = Modifier, maxItems: Int = 8) {
    val entries = stats.nonZeroEntries().take(maxItems)
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        entries.forEach { (type, value) ->
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = ForgeSurfaceElevated,
                border = BorderStroke(1.dp, ForgeBorder.copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = type.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = ForgeTextSecondary
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = type.formatValue(value),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MoltenAmber
                    )
                }
            }
        }
    }
}

@Composable
fun CapGaugeBar(cap: CapProgress, modifier: Modifier = Modifier) {
    val fraction = (cap.effectiveValue / cap.capValue).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = fraction,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 350f),
        label = "capProgress"
    )

    val barColor = when {
        cap.isOvercapped -> ForgeCrimson
        fraction >= 0.96f -> ForgeEmerald
        fraction >= 0.65f -> ArcaneCyan
        else -> MoltenAmber
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = cap.label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = ForgeTextPrimary
            )
            val rawStr = if (cap.isFlatDecimal) {
                String.format(Locale.US, "%.2f", cap.rawValue)
            } else {
                "${cap.rawValue.toInt()}%"
            }
            val capStr = if (cap.isFlatDecimal) {
                String.format(Locale.US, "%.2f", cap.capValue)
            } else {
                "${cap.capValue.toInt()}%"
            }
            val minStr = cap.minTarget?.let { m ->
                val formatted = if (cap.isFlatDecimal) String.format(Locale.US, "%.2f", m) else "${m.toInt()}%"
                "$formatted-"
            } ?: ""
            val statusSuffix = when {
                cap.isOvercapped -> " (OVERCAP)"
                cap.minTarget != null && cap.rawValue < cap.minTarget -> " (BELOW MIN)"
                cap.minTarget != null -> " (IN RANGE)"
                fraction >= 0.98f -> " (CAPPED)"
                else -> ""
            }
            Text(
                text = "$rawStr / $minStr$capStr$statusSuffix",
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    cap.isOvercapped -> ForgeCrimson
                    cap.minTarget != null && cap.rawValue < cap.minTarget -> MoltenAmber
                    cap.minTarget != null || fraction >= 0.98f -> ForgeEmerald
                    else -> ForgeTextSecondary
                }
            )
        }
        Spacer(Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ForgeObsidian)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
    }
}

/**
 * In-app tiny collapsible right-edge tab that stays unobtrusive on the screen edge and expands
 * when clicked so the user can tell Hunter Forge to scan new gear or update all hunters anytime.
 */
@Composable
fun InAppSideEdgeTab(
    isExpanded: Boolean,
    isMonitorRunning: Boolean,
    statusLine: String,
    onToggleExpand: () -> Unit,
    onScanNewGear: () -> Unit,
    onUpdateHunters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End)
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(min = 210.dp, max = 250.dp)
                    .padding(end = 4.dp),
                shape = RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp),
                color = ForgeSlateDark.copy(alpha = 0.96f),
                border = BorderStroke(1.dp, ArcaneCyan.copy(alpha = 0.7f)),
                shadowElevation = 12.dp
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isMonitorRunning) ForgeEmerald else MoltenAmber)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "QUICK FORGE",
                                style = MaterialTheme.typography.labelMedium,
                                color = MoltenAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = onToggleExpand,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("side_tab_collapse_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Collapse side tab",
                                tint = ForgeTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = statusLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = ForgeTextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Button(
                        onClick = onScanNewGear,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("side_tab_look_gear_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MoltenAmber,
                            contentColor = ForgeObsidian
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Look at New Gear", style = MaterialTheme.typography.labelLarge)
                    }

                    Button(
                        onClick = onUpdateHunters,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("side_tab_update_hunters_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ArcaneCyan,
                            contentColor = ForgeObsidian
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Update Hunters", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // Real small edge tab pill
        Surface(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                .clickable(onClick = onToggleExpand)
                .border(
                    width = 1.dp,
                    color = if (isMonitorRunning) ForgeEmerald else MoltenAmber,
                    shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                )
                .testTag("side_edge_tab"),
            shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
            color = ForgeSurface.copy(alpha = 0.94f),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = if (isExpanded) "Collapse quick tab" else "Expand quick tab",
                    tint = MoltenAmber,
                    modifier = Modifier.size(16.dp)
                )
                Icon(
                    imageVector = Icons.Default.Radar,
                    contentDescription = null,
                    tint = if (isMonitorRunning) ForgeEmerald else ArcaneCyan,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}
