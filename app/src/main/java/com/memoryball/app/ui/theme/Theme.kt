package com.memoryball.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Minimal, practical palette. Brand indigo #5B6CFF is preserved; supporting tones,
// a warm accent (echoing the app icon) and layered surfaces make it feel refined.
private val LightColors = lightColorScheme(
    primary = Color(0xFF5B6CFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1E6FF),
    onPrimaryContainer = Color(0xFF16205E),
    inversePrimary = Color(0xFFB7C4FF),

    secondary = Color(0xFF5A6078),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1E5F5),
    onSecondaryContainer = Color(0xFF171D33),

    tertiary = Color(0xFFB0723A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0C4),
    onTertiaryContainer = Color(0xFF3A2200),

    background = Color(0xFFF5F6FB),
    onBackground = Color(0xFF191B21),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191B21),
    surfaceVariant = Color(0xFFE6E9F3),
    onSurfaceVariant = Color(0xFF44474F),

    surfaceBright = Color(0xFFFBFAFF),
    surfaceDim = Color(0xFFD8DAE2),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9F9FF),
    surfaceContainer = Color(0xFFF3F4FC),
    surfaceContainerHigh = Color(0xFFEDEEF7),
    surfaceContainerHighest = Color(0xFFE8E9F3),

    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB7C4FF),
    onPrimary = Color(0xFF23317A),
    primaryContainer = Color(0xFF3A488F),
    onPrimaryContainer = Color(0xFFE0E5FF),
    inversePrimary = Color(0xFF5B6CFF),

    secondary = Color(0xFFC3C7DD),
    onSecondary = Color(0xFF2D3143),
    secondaryContainer = Color(0xFF444861),
    onSecondaryContainer = Color(0xFFE1E5F5),

    tertiary = Color(0xFFF2B98A),
    onTertiary = Color(0xFF422900),
    tertiaryContainer = Color(0xFF5F3E16),
    onTertiaryContainer = Color(0xFFFFE0C4),

    background = Color(0xFF111318),
    onBackground = Color(0xFFE3E6ED),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE3E6ED),
    surfaceVariant = Color(0xFF45474F),
    onSurfaceVariant = Color(0xFFC5C7D0),

    surfaceBright = Color(0xFF37393F),
    surfaceDim = Color(0xFF111318),
    surfaceContainerLowest = Color(0xFF0C0E13),
    surfaceContainerLow = Color(0xFF191B21),
    surfaceContainer = Color(0xFF1D1F25),
    surfaceContainerHigh = Color(0xFF272930),
    surfaceContainerHighest = Color(0xFF32343B),

    outline = Color(0xFF8F909A),
    outlineVariant = Color(0xFF45474F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun MemoryBallTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
