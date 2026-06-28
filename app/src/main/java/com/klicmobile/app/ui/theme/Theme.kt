package com.klicmobile.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val KlicColors = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceRaised,
    error = Danger,
)

@Composable
fun KlicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KlicColors, // dark-first regardless of system setting
        typography = KlicTypography,
        content = content,
    )
}
