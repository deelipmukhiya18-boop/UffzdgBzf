package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AssistantDarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = CardDark,
    onPrimaryContainer = CyanAccent,
    secondary = CyanAccent,
    onSecondary = Color.Black,
    secondaryContainer = SurfaceBorder,
    onSecondaryContainer = TextPrimary,
    tertiary = SuccessGreen,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    error = DangerRed,
    onError = Color.White
)

private val AssistantLightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3EAFF),
    onPrimaryContainer = PrimaryBlue,
    secondary = Color(0xFF007A99),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD4F3FA),
    onSecondaryContainer = Color(0xFF003847),
    tertiary = Color(0xFF00897B),
    background = Color(0xFFF6F8FD),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE8EEF9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = DangerRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek AI dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AssistantDarkColorScheme else AssistantLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
