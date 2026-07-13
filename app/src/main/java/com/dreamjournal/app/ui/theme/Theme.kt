package com.dreamjournal.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.dreamjournal.app.domain.settings.ThemeMode

private val LightColors = lightColorScheme(
    primary = AccentPrimary,
    onPrimary = Color.White,
    primaryContainer = AccentPrimaryContainer,
    onPrimaryContainer = TextPrimary,
    secondary = AccentSecondary,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = TextPrimary,
    surface = LightSurface,
    onSurface = TextPrimary,
    surfaceVariant = LightSurfaceRaised,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceRaised,
    error = Danger,
    onError = Color.White,
    outline = Color(0xFFCBC6BC),
    outlineVariant = Color(0xFFE1DDD4)
)

private val DarkColors = darkColorScheme(
    primary = AccentPrimaryLight,
    onPrimary = DarkBackground,
    primaryContainer = AccentPrimary.copy(alpha = 0.25f),
    onPrimaryContainer = TextOnDark,
    secondary = AccentSecondary,
    onSecondary = DarkBackground,
    background = DarkBackground,
    onBackground = TextOnDark,
    surface = DarkSurface,
    onSurface = TextOnDark,
    surfaceVariant = DarkSurfaceRaised,
    onSurfaceVariant = TextOnDarkSecondary,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceRaised,
    error = Danger,
    onError = DarkBackground,
    outline = Color(0xFF52605C),
    outlineVariant = Color(0xFF34413E)
)

@Composable
fun QingJiTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val colorScheme = if (themeMode == ThemeMode.DARK) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
