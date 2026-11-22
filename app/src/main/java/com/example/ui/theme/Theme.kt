package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ForgeDarkColorScheme = darkColorScheme(
    primary = MoltenAmber,
    onPrimary = ForgeObsidian,
    primaryContainer = MoltenAmberDark,
    onPrimaryContainer = ForgeTextPrimary,
    secondary = ArcaneCyan,
    onSecondary = ForgeObsidian,
    secondaryContainer = ArcaneCyanDark,
    onSecondaryContainer = ForgeTextPrimary,
    tertiary = TierChaos,
    onTertiary = ForgeTextPrimary,
    background = ForgeObsidian,
    onBackground = ForgeTextPrimary,
    surface = ForgeSlateDark,
    onSurface = ForgeTextPrimary,
    surfaceVariant = ForgeSurface,
    onSurfaceVariant = ForgeTextSecondary,
    surfaceContainerLow = ForgeSlateDark,
    surfaceContainer = ForgeSurface,
    surfaceContainerHigh = ForgeSurfaceElevated,
    outline = ForgeBorder,
    outlineVariant = ForgeBorder.copy(alpha = 0.5f),
    error = ForgeCrimson,
    onError = ForgeTextPrimary
)

val ForgeShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun HunterForgeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ForgeDarkColorScheme,
        typography = Typography,
        shapes = ForgeShapes,
        content = content
    )
}
