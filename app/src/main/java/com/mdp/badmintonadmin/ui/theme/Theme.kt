package com.mdp.badmintonadmin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// Dark only, like ScoreCounter and Pickles. No dynamic colour: the court palette is the brand.
private val CourtColorScheme = darkColorScheme(
    primary = Coral,
    onPrimary = OnCoral,
    primaryContainer = CourtGreen,
    onPrimaryContainer = OnCourtGreen,
    inversePrimary = Coral,
    secondary = CourtLine,
    onSecondary = CourtNight,
    secondaryContainer = CourtSurfaceTop,
    onSecondaryContainer = Chalk,
    tertiary = WinGreen,
    onTertiary = OnWinGreen,
    tertiaryContainer = CourtGreen,
    onTertiaryContainer = OnCourtGreen,
    background = CourtNight,
    onBackground = Chalk,
    surface = CourtSurface,
    onSurface = Chalk,
    surfaceVariant = CourtSurfaceHigh,
    onSurfaceVariant = Muted,
    surfaceTint = CourtSurface,
    inverseSurface = Chalk,
    inverseOnSurface = CourtNight,
    error = Danger,
    onError = OnDanger,
    errorContainer = DangerContainer,
    onErrorContainer = Chalk,
    outline = CourtOutlineStrong,
    outlineVariant = CourtOutline,
    scrim = CourtNight,
    surfaceBright = CourtSurfaceTop,
    surfaceDim = CourtNight,
    surfaceContainerLowest = CourtNight,
    surfaceContainerLow = CourtSurface,
    surfaceContainer = CourtSurface,
    surfaceContainerHigh = CourtSurfaceHigh,
    surfaceContainerHighest = CourtSurfaceTop
)

private val CourtShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BadmintonAdminTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CourtColorScheme,
        typography = Typography,
        shapes = CourtShapes,
        content = content
    )
}
