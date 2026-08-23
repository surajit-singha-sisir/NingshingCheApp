package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightEditorialColorScheme = lightColorScheme(
    primary = Amber900,
    onPrimary = Color.White,
    primaryContainer = Amber100,
    onPrimaryContainer = Amber950,
    secondary = Amber800,
    onSecondary = Color.White,
    secondaryContainer = Amber50,
    onSecondaryContainer = Amber900,
    tertiary = Amber700,
    onTertiary = Color.White,
    background = PaperCanvasLight,
    onBackground = TextPrimaryLight,
    surface = PaperSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = PaperSurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = PaperCardBorderLight,
    outlineVariant = Color(0xFFE2D6CE)
)

private val DarkEditorialColorScheme = darkColorScheme(
    primary = Amber400,
    onPrimary = Amber950,
    primaryContainer = Color(0xFF381E10),
    onPrimaryContainer = Amber200,
    secondary = Amber300,
    onSecondary = Amber950,
    secondaryContainer = Color(0xFF2C1B12),
    onSecondaryContainer = Amber100,
    tertiary = Color(0xFF93C5FD),
    onTertiary = Color(0xFF0F172A),
    background = NightCanvas,
    onBackground = NightTextPrimary,
    surface = NightSurface,
    onSurface = NightTextPrimary,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightTextSecondary,
    outline = NightCardBorder,
    outlineVariant = Color(0xFF4A3B33)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkEditorialColorScheme else LightEditorialColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EditorialTypography,
        content = content
    )
}
