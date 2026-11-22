package com.example.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import com.example.R
import com.example.data.model.WidgetAlignment
import com.example.data.model.WidgetConfig
import com.example.data.model.WidgetThemeOverride
import com.example.util.ColorPalette
import com.example.util.FontHelper
import com.example.util.SystemTelemetry
import java.io.File

data class ResolvedWidgetStyle(
    val bgColor: Int,
    val textColor: Int,
    val secondaryTextColor: Int,
    val accentColor: Int,
    val surfaceTrackColor: Int,
    val borderStrokeColor: Int,
    val typeface: Typeface,
    val fontScale: Float,
    val paddingPx: Float,
    val radiusPx: Float,
    val alignment: WidgetAlignment,
    val isDark: Boolean
)

object WidgetCanvasRenderer {

    fun render(
        context: Context,
        config: WidgetConfig,
        telemetry: SystemTelemetry,
        widthDp: Int,
        heightDp: Int
    ): Bitmap {
        val density = context.resources.displayMetrics.density.coerceIn(1.5f, 3.0f)
        // Clamp bitmap dimensions to stay comfortably below Android RemoteViews Binder limits
        val maxPx = 680f
        val rawW = (widthDp.coerceAtLeast(100) * density)
        val rawH = (heightDp.coerceAtLeast(60) * density)
        val downscale = minOf(1f, maxPx / maxOf(rawW, rawH))
        val scale = density * downscale
        val w = (rawW * downscale).toInt().coerceIn(180, 680)
        val h = (rawH * downscale).toInt().coerceIn(96, 680)

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val style = resolveStyle(context, config, scale)
        val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())
        val clipPath = Path().apply {
            addRoundRect(rect, style.radiusPx, style.radiusPx, Path.Direction.CW)
        }

        canvas.save()
        canvas.clipPath(clipPath)

        // Background color with transparency
        val bgAlpha = (config.alpha.coerceIn(0f, 1f) * 255).toInt()
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(
                bgAlpha,
                Color.red(style.bgColor),
                Color.green(style.bgColor),
                Color.blue(style.bgColor)
            )
        }
        canvas.drawRect(rect, bgPaint)

        // Optional Background Image
        if (config.bgImageKey.isNotBlank()) {
            loadBgBitmap(context, config.bgImageKey)?.let { bgBmp ->
                drawCenterCrop(canvas, bgBmp, w, h, bgAlpha)
                bgBmp.recycle()
                if (config.bgImageDim > 0.01f) {
                    val dimPaint = Paint().apply {
                        color = Color.argb((config.bgImageDim.coerceIn(0f, 0.85f) * 255).toInt(), 0, 0, 0)
                    }
                    canvas.drawRect(rect, dimPaint)
                }
            }
        }

        // Subtle crisp border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = 1.2f * scale
            color = style.borderStrokeColor
        }
        canvas.drawRoundRect(
            RectF(0.6f * scale, 0.6f * scale, w - 0.6f * scale, h - 0.6f * scale),
            style.radiusPx,
            style.radiusPx,
            borderPaint
        )

        // Delegate widget content drawing
        val contentRect = RectF(
            style.paddingPx,
            style.paddingPx,
            (w - style.paddingPx).coerceAtLeast(style.paddingPx + 20f),
            (h - style.paddingPx).coerceAtLeast(style.paddingPx + 20f)
        )
        WidgetCanvasContent.drawContent(canvas, contentRect, config, style, telemetry, scale)

        canvas.restore()
        return bitmap
    }

    private fun resolveStyle(context: Context, config: WidgetConfig, scale: Float): ResolvedWidgetStyle {
        val sysNight = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

        val useLight = config.themeOverride == WidgetThemeOverride.LIGHT ||
            (config.themeOverride == WidgetThemeOverride.FOLLOW_SYSTEM && !sysNight)
        val useDark = config.themeOverride == WidgetThemeOverride.DARK ||
            (config.themeOverride == WidgetThemeOverride.FOLLOW_SYSTEM && sysNight)

        val bgColor = when {
            useLight -> Color.parseColor("#FFFFFF")
            useDark -> Color.parseColor("#14171C")
            else -> ColorPalette.parseColorInt(config.bgColorHex, Color.WHITE)
        }
        val textColor = when {
            useLight -> Color.parseColor("#14171C")
            useDark -> Color.parseColor("#F4F5F7")
            else -> ColorPalette.parseColorInt(config.textColorHex, Color.parseColor("#14171C"))
        }
        val accentColor = ColorPalette.parseColorInt(config.accentColorHex, Color.parseColor("#2563EB"))
        val isDark = ColorPalette.isDarkColor(bgColor) || config.bgImageKey.isNotBlank()

        val secondaryText = Color.argb(
            180,
            Color.red(textColor),
            Color.green(textColor),
            Color.blue(textColor)
        )
        val track = Color.argb(
            38,
            Color.red(textColor),
            Color.green(textColor),
            Color.blue(textColor)
        )
        val border = if (isDark) Color.argb(42, 255, 255, 255) else Color.argb(24, 0, 0, 0)

        return ResolvedWidgetStyle(
            bgColor = bgColor,
            textColor = textColor,
            secondaryTextColor = secondaryText,
            accentColor = accentColor,
            surfaceTrackColor = track,
            borderStrokeColor = border,
            typeface = FontHelper.androidTypeface(context, config.fontFamily, config.fontWeight),
            fontScale = config.fontScale.coerceIn(0.7f, 1.6f),
            paddingPx = config.paddingDp.coerceIn(4f, 32f) * scale,
            radiusPx = config.borderRadiusDp.coerceIn(0f, 40f) * scale,
            alignment = config.alignment,
            isDark = isDark
        )
    }

    private fun loadBgBitmap(context: Context, key: String): Bitmap? {
        return try {
            when {
                key == "preset_horizon" -> BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.img_preset_horizon_1795326903253
                )
                key == "preset_botanical" -> BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.img_preset_botanical_1795326903794
                )
                key.startsWith("file://") -> {
                    val file = File(key.removePrefix("file://"))
                    if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun drawCenterCrop(canvas: Canvas, bmp: Bitmap, targetW: Int, targetH: Int, alpha: Int) {
        val srcRatio = bmp.width.toFloat() / bmp.height.coerceAtLeast(1)
        val dstRatio = targetW.toFloat() / targetH.coerceAtLeast(1)
        val srcRect = if (srcRatio > dstRatio) {
            val cropW = (bmp.height * dstRatio).toInt()
            val left = (bmp.width - cropW) / 2
            Rect(left, 0, left + cropW, bmp.height)
        } else {
            val cropH = (bmp.width / dstRatio).toInt()
            val top = (bmp.height - cropH) / 2
            Rect(0, top, bmp.width, top + cropH)
        }
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { this.alpha = alpha }
        canvas.drawBitmap(bmp, srcRect, Rect(0, 0, targetW, targetH), paint)
    }
}
