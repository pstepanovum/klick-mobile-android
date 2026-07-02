package com.klic.mobile.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary          = BrandPrimary,
    onPrimary        = BrandOnPrimary,
    background       = DarkBackground,
    onBackground     = DarkTextPrimary,
    surface          = DarkSurface,
    onSurface        = DarkTextPrimary,
    surfaceVariant   = DarkSurfaceRaised,
    onSurfaceVariant = DarkTextMuted,
    error            = BrandDanger,
    onError          = BrandOnPrimary,
)

private val LightColors = lightColorScheme(
    primary          = BrandPrimary,
    onPrimary        = BrandOnPrimary,
    background       = LightBackground,
    onBackground     = LightTextPrimary,
    surface          = LightSurface,
    onSurface        = LightTextPrimary,
    surfaceVariant   = LightSurfaceRaised,
    onSurfaceVariant = LightTextMuted,
    error            = BrandDanger,
    onError          = BrandOnPrimary,
)

@Composable
fun KlicTheme(isDark: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isDark) DarkColors else LightColors,
        typography  = KlicTypography,
        content     = content,
    )
}
