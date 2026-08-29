package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AuraColorScheme = darkColorScheme(
    primary = AuraPrimary,
    onPrimary = Color(0xFF042111),
    primaryContainer = AuraDeepEmerald,
    onPrimaryContainer = AuraTertiary,
    secondary = AuraSecondary,
    onSecondary = Color(0xFF042115),
    secondaryContainer = AuraSurfaceElevated,
    onSecondaryContainer = AuraTextPrimary,
    tertiary = AuraTertiary,
    background = AuraBackground,
    onBackground = AuraTextPrimary,
    surface = AuraSurface,
    onSurface = AuraTextPrimary,
    surfaceVariant = AuraSurfaceElevated,
    onSurfaceVariant = AuraTextSecondary,
    outline = AuraSurfaceBorder,
    error = AuraError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AuraColorScheme,
        typography = Typography,
        content = content
    )
}
