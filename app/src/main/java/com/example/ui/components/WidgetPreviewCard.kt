package com.example.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.WidgetAlignment
import com.example.data.model.WidgetConfig
import com.example.data.model.WidgetSizePreset
import com.example.data.model.WidgetThemeOverride
import com.example.util.ColorPalette
import com.example.util.FontHelper
import com.example.util.SystemTelemetry
import java.io.File

data class ComposeWidgetStyle(
    val bgColor: Color,
    val textColor: Color,
    val secondaryTextColor: Color,
    val accentColor: Color,
    val trackColor: Color,
    val fontFamily: FontFamily,
    val fontWeight: FontWeight,
    val fontScale: Float,
    val textAlign: TextAlign,
    val horizontalAlign: Alignment.Horizontal
)

@Composable
fun WidgetPreviewCard(
    config: WidgetConfig,
    telemetry: SystemTelemetry,
    nowMillis: Long,
    modifier: Modifier = Modifier,
    compactMode: Boolean = false,
    onInteract: ((Int) -> Unit)? = null,
    onCardClick: (() -> Unit)? = null
) {
    val sysDark = (LocalConfiguration.current.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES

    val useLight = config.themeOverride == WidgetThemeOverride.LIGHT ||
        (config.themeOverride == WidgetThemeOverride.FOLLOW_SYSTEM && !sysDark)
    val useDark = config.themeOverride == WidgetThemeOverride.DARK ||
        (config.themeOverride == WidgetThemeOverride.FOLLOW_SYSTEM && sysDark)

    val baseBg = when {
        useLight -> Color.White
        useDark -> Color(0xFF14171C)
        else -> ColorPalette.parseComposeColor(config.bgColorHex, Color.White)
    }
    val textColor = when {
        useLight -> Color(0xFF14171C)
        useDark -> Color(0xFFF4F5F7)
        else -> ColorPalette.parseComposeColor(config.textColorHex, Color(0xFF14171C))
    }
    val accentColor = ColorPalette.parseComposeColor(config.accentColorHex, Color(0xFF2563EB))
    val isDarkBg = baseBg.luminance() < 0.45f || config.bgImageKey.isNotBlank()
    val borderColor = if (isDarkBg) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.09f)

    val align = config.alignment
    val textAlign = when (align) {
        WidgetAlignment.START -> TextAlign.Start
        WidgetAlignment.CENTER -> TextAlign.Center
        WidgetAlignment.END -> TextAlign.End
    }
    val horizontalAlign = when (align) {
        WidgetAlignment.START -> Alignment.Start
        WidgetAlignment.CENTER -> Alignment.CenterHorizontally
        WidgetAlignment.END -> Alignment.End
    }

    val style = ComposeWidgetStyle(
        bgColor = baseBg,
        textColor = textColor,
        secondaryTextColor = textColor.copy(alpha = 0.70f),
        accentColor = accentColor,
        trackColor = textColor.copy(alpha = 0.14f),
        fontFamily = FontHelper.composeFontFamily(config.fontFamily),
        fontWeight = FontHelper.composeFontWeight(config.fontWeight),
        fontScale = (if (compactMode) config.fontScale * 0.85f else config.fontScale).coerceIn(0.65f, 1.55f),
        textAlign = textAlign,
        horizontalAlign = horizontalAlign
    )

    val shape = RoundedCornerShape(config.borderRadiusDp.dp)
    val maxWidthDp = when (config.size) {
        WidgetSizePreset.SMALL_2X2 -> if (compactMode) 170.dp else 200.dp
        WidgetSizePreset.MEDIUM_4X2 -> if (compactMode) 280.dp else 340.dp
        WidgetSizePreset.LARGE_4X4 -> if (compactMode) 260.dp else 310.dp
        WidgetSizePreset.STRIP_4X1 -> if (compactMode) 290.dp else 350.dp
    }

    Box(
        modifier = modifier
            .widthIn(max = maxWidthDp)
            .fillMaxWidth()
            .aspectRatio(config.size.aspectRatio)
            .clip(shape)
            .background(baseBg.copy(alpha = config.alpha.coerceIn(0.05f, 1f)))
            .border(1.dp, borderColor, shape)
            .then(
                if (onCardClick != null) Modifier.clickable(onClick = onCardClick)
                else if (onInteract != null && !config.widgetType.hasInteractiveActions) {
                    Modifier.clickable { onInteract(0) }
                } else Modifier
            )
    ) {
        if (config.bgImageKey.isNotBlank()) {
            when {
                config.bgImageKey == "preset_horizon" -> {
                    Image(
                        painter = painterResource(R.drawable.img_preset_horizon_1795326903253),
                        contentDescription = "Widget background",
                        contentScale = ContentScale.Crop,
                        alpha = config.alpha,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                config.bgImageKey == "preset_botanical" -> {
                    Image(
                        painter = painterResource(R.drawable.img_preset_botanical_1795326903794),
                        contentDescription = "Widget background",
                        contentScale = ContentScale.Crop,
                        alpha = config.alpha,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                config.bgImageKey.startsWith("file://") -> {
                    val file = File(config.bgImageKey.removePrefix("file://"))
                    if (file.exists()) {
                        AsyncImage(
                            model = file,
                            contentDescription = "Custom widget background",
                            contentScale = ContentScale.Crop,
                            alpha = config.alpha,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            if (config.bgImageDim > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = config.bgImageDim.coerceIn(0f, 0.85f)))
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    if (compactMode) (config.paddingDp * 0.8f).coerceAtLeast(8f).dp
                    else config.paddingDp.dp
                )
        ) {
            WidgetPreviewContent(
                config = config,
                style = style,
                telemetry = telemetry,
                nowMillis = nowMillis,
                onInteract = onInteract
            )
        }
    }
}
