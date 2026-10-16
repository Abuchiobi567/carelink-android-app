package com.example.carelink.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = BluePrimary,
    onPrimary = SurfaceLight,
    secondary = OrangeAccent,
    onSecondary = SurfaceLight,
    background = BackgroundLight,
    onBackground = BluePrimary,
    surface = SurfaceLight,
    onSurface = BluePrimary,
    error = RedAlert,
    onError = SurfaceLight
)

private val DarkColors = darkColorScheme(
    primary = BluePrimaryDark,
    onPrimary = BackgroundDark,
    secondary = OrangeAccentDark,
    onSecondary = BackgroundDark,
    background = BackgroundDark,
    onBackground = BlueLight,
    surface = SurfaceDark,
    onSurface = BlueLight,
    error = RedAlert,
    onError = BackgroundDark
)

// wraps the whole app in Material3. light/dark by default follows the system,
// but the settings screen can force one or the other via forceDarkMode.
@Composable
fun CareLinkTheme(
    forceDarkMode: Boolean? = null,
    content: @Composable () -> Unit
) {
    val useDark = forceDarkMode ?: isSystemInDarkTheme()
    val colors = if (useDark) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}