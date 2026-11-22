package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.local.AppAccentColor
import com.example.data.local.AppThemeMode

@Composable
fun WidgetLiteTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    accentColor: AppAccentColor = AppAccentColor.COBALT,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val primary = if (isDark && accentColor == AppAccentColor.GRAPHITE) {
        Color(0xFFF4F4F5)
    } else {
        Color(accentColor.colorLong)
    }

    val onPrimary = if (isDark && accentColor == AppAccentColor.GRAPHITE) {
        Color(0xFF121417)
    } else {
        Color.White
    }

    val primaryContainer = primary.copy(alpha = if (isDark) 0.20f else 0.12f)
    val onPrimaryContainer = if (isDark) Color(0xFFF3F4F6) else primary

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = primary,
            background = DarkBackground,
            onBackground = DarkOnSurface,
            surface = DarkSurface,
            onSurface = DarkOnSurface,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = DarkOnSurfaceVariant,
            outline = DarkOutline,
            outlineVariant = DarkOutline
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = primary,
            background = LightBackground,
            onBackground = LightOnSurface,
            surface = LightSurface,
            onSurface = LightOnSurface,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = LightOnSurfaceVariant,
            outline = LightOutline,
            outlineVariant = LightOutline
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WidgetLiteTypography,
        content = content
    )
}
