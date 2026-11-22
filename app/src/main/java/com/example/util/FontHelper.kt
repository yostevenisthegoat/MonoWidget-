package com.example.util

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.model.WidgetFontFamily
import com.example.data.model.WidgetFontWeight

object FontHelper {

    fun composeFontFamily(family: WidgetFontFamily): FontFamily = when (family) {
        WidgetFontFamily.PLUS_JAKARTA -> FontFamily(Font(R.font.plus_jakarta_sans, FontWeight.Normal))
        WidgetFontFamily.INTER -> FontFamily(Font(R.font.inter, FontWeight.Normal))
        WidgetFontFamily.SPACE_GROTESK -> FontFamily(Font(R.font.space_grotesk, FontWeight.Normal))
        WidgetFontFamily.JETBRAINS_MONO -> FontFamily(Font(R.font.jetbrains_mono, FontWeight.Normal))
        WidgetFontFamily.PLAYFAIR_DISPLAY -> FontFamily(Font(R.font.playfair_display, FontWeight.Normal))
        WidgetFontFamily.OUTFIT -> FontFamily(Font(R.font.outfit, FontWeight.Normal))
        WidgetFontFamily.SYSTEM_SANS -> FontFamily.SansSerif
    }

    fun composeFontWeight(weight: WidgetFontWeight): FontWeight = when (weight) {
        WidgetFontWeight.REGULAR -> FontWeight.Normal
        WidgetFontWeight.MEDIUM -> FontWeight.Medium
        WidgetFontWeight.SEMIBOLD -> FontWeight.SemiBold
        WidgetFontWeight.BOLD -> FontWeight.Bold
    }

    fun androidTypeface(context: Context, family: WidgetFontFamily, weight: WidgetFontWeight): Typeface {
        val resId = when (family) {
            WidgetFontFamily.PLUS_JAKARTA -> R.font.plus_jakarta_sans
            WidgetFontFamily.INTER -> R.font.inter
            WidgetFontFamily.SPACE_GROTESK -> R.font.space_grotesk
            WidgetFontFamily.JETBRAINS_MONO -> R.font.jetbrains_mono
            WidgetFontFamily.PLAYFAIR_DISPLAY -> R.font.playfair_display
            WidgetFontFamily.OUTFIT -> R.font.outfit
            WidgetFontFamily.SYSTEM_SANS -> 0
        }
        val base = if (resId != 0) {
            try {
                ResourcesCompat.getFont(context, resId) ?: Typeface.SANS_SERIF
            } catch (_: Exception) {
                Typeface.SANS_SERIF
            }
        } else {
            Typeface.SANS_SERIF
        }
        val isBold = weight == WidgetFontWeight.BOLD || weight == WidgetFontWeight.SEMIBOLD
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            Typeface.create(base, weight.weightValue, false)
        } else {
            Typeface.create(base, if (isBold) Typeface.BOLD else Typeface.NORMAL)
        }
    }
}
