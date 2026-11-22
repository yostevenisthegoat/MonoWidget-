package com.example.util

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt

object ColorPalette {
    val widgetBackgroundSwatches = listOf(
        "#FFFFFF", "#F8FAFC", "#F4F4F5", "#FAF5EE",
        "#14171C", "#1E2229", "#0F172A", "#1C1917",
        "#EFF6FF", "#ECFDF5", "#FEF3C7", "#FDF2F8",
        "#2563EB", "#059669", "#D97706", "#7C3AED"
    )

    val widgetTextSwatches = listOf(
        "#14171C", "#27272A", "#52525B", "#FFFFFF",
        "#F4F4F5", "#E2E8F0", "#2563EB", "#059669",
        "#D97706", "#DC2626", "#7C3AED", "#0F766E"
    )

    fun parseColorInt(hex: String, fallback: Int = android.graphics.Color.WHITE): Int {
        return try {
            val clean = if (hex.startsWith("#")) hex else "#$hex"
            clean.toColorInt()
        } catch (_: Exception) {
            fallback
        }
    }

    fun parseComposeColor(hex: String, fallback: Color = Color.White): Color {
        return Color(parseColorInt(hex, android.graphics.Color.WHITE))
    }

    fun toHex(colorInt: Int): String {
        return String.format("#%06X", 0xFFFFFF and colorInt)
    }

    fun isDarkColor(colorInt: Int): Boolean {
        val r = android.graphics.Color.red(colorInt) / 255.0
        val g = android.graphics.Color.green(colorInt) / 255.0
        val b = android.graphics.Color.blue(colorInt) / 255.0
        val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
        return luminance < 0.5
    }
}
