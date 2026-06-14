package com.mdp.badmintonadmin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    onPrimary = Color.Black,
    secondary = PurpleGrey80,
    onSecondary = Color.Black,
    tertiary = FinishGreenDark,
    onTertiary = OnFinishGreenDark,
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkBackground,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = Color(0xFF938F99),
    primaryContainer = Color(0xFF381E72),
    onPrimaryContainer = Color(0xFFEADDFF),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    onPrimary = Color.White,
    secondary = PurpleGrey40,
    onSecondary = Color.White,
    tertiary = FinishGreenLight,
    onTertiary = OnFinishGreenLight,
    background = LightBackground,
    onBackground = Color(0xFF1C1B1F),
    surface = LightBackground,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = Color(0xFF79747E),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    error = Color(0xFFB3261E),
    onError = Color.White
)

@Composable
fun BadmintonAdminTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Note: Ensuring accessible brand colors are used by bypassing dynamicColor for now
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
