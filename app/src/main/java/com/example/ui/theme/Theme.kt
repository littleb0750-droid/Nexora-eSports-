package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val NaxoraDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = ObsidianDeep,
    primaryContainer = CyanButtonBg,
    onPrimaryContainer = NeonCyan,
    secondary = NeonPurple,
    onSecondary = ObsidianDeep,
    secondaryContainer = PurpleButtonBg,
    onSecondaryContainer = PurpleSoftText,
    tertiary = AmberGold,
    onTertiary = ObsidianDeep,
    background = ObsidianDeep,
    onBackground = TextPrimary,
    surface = ObsidianBase,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCardSolid,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    outlineVariant = InsetBorder,
    error = CrimsonDanger,
    onError = TextPrimary
)

val NaxoraShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun NaxoraTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NaxoraDarkColorScheme,
        typography = Typography,
        shapes = NaxoraShapes,
        content = content
    )
}
