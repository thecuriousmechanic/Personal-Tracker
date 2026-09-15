package com.krishna.personalos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = SageWash,
    onPrimaryContainer = Ice,
    secondary = Cyan,
    secondaryContainer = SageWash,
    onSecondaryContainer = Ice,
    tertiary = Mint,
    background = Ink,
    onBackground = Ice,
    surface = DeepNavy,
    surfaceContainerLowest = Paper,
    surfaceContainerLow = Ink,
    surfaceContainer = SurfaceNavy,
    surfaceContainerHigh = RaisedNavy,
    surfaceContainerHighest = RaisedNavy,
    surfaceTint = Color.Transparent,
    onSurface = Ice,
    surfaceVariant = RaisedNavy,
    onSurfaceVariant = Muted,
    outline = Divider,
    outlineVariant = Divider,
    error = Color(0xFFAF3838),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9BCBAD),
    onPrimary = Color(0xFF123823),
    primaryContainer = Color(0xFF294837),
    onPrimaryContainer = Color(0xFFD7F2DE),
    secondary = Color(0xFF9BCBAD),
    onSecondary = Color(0xFF123823),
    secondaryContainer = Color(0xFF294837),
    onSecondaryContainer = Color(0xFFD7F2DE),
    tertiary = Color(0xFFD1B4E0),
    background = Color(0xFF191919),
    onBackground = Color(0xFFE6E6E3),
    surface = Color(0xFF191919),
    surfaceContainerLowest = Color(0xFF151515),
    surfaceContainerLow = Color(0xFF1D1D1D),
    surfaceContainer = Color(0xFF232323),
    surfaceContainerHigh = Color(0xFF2B2B2B),
    surfaceContainerHighest = Color(0xFF333333),
    surfaceTint = Color.Transparent,
    onSurface = Color(0xFFE6E6E3),
    surfaceVariant = Color(0xFF2B2B2B),
    onSurfaceVariant = Color(0xFFA6A6A1),
    outline = Color(0xFF4B4B49),
    outlineVariant = Color(0xFF343432),
    error = Color(0xFFFFB4AB),
)

private val PersonalShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

@Composable
fun PersonalOSTheme(darkMode: Boolean = false, content: @Composable () -> Unit) {
    applyLegacyPalette(darkMode)
    MaterialTheme(
        colorScheme = if (darkMode) DarkColors else LightColors,
        typography = PersonalTypography,
        shapes = PersonalShapes,
        content = content,
    )
}
