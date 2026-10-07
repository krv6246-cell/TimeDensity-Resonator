package com.timedensity.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = HunterCyan,
    secondary = HunterViolet,
    tertiary = HunterMetal,
    background = HunterBackground,
    surface = HunterSurface,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun REZONATORTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}