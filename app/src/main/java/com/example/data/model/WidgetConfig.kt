package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "widget_configs")
data class WidgetConfig(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val typeId: String,
    val title: String,
    val sizePreset: String = WidgetSizePreset.MEDIUM_4X2.name,
    val bgColorHex: String = "#FFFFFF",
    val textColorHex: String = "#14171C",
    val accentColorHex: String = "#2563EB",
    val fontFamilyKey: String = WidgetFontFamily.PLUS_JAKARTA.key,
    val fontScale: Float = 1.0f,
    val fontWeightName: String = WidgetFontWeight.MEDIUM.name,
    val alpha: Float = 0.96f,
    val borderRadiusDp: Float = 22f,
    val paddingDp: Float = 16f,
    val alignmentName: String = WidgetAlignment.START.name,
    val themeOverrideName: String = WidgetThemeOverride.CUSTOM.name,
    val bgImageKey: String = "", // "", "preset_horizon", "preset_botanical", or "file://<path>"
    val bgImageDim: Float = 0.35f,
    val payloadJson: String = "",
    val appWidgetId: Int = -1,
    val isPinned: Boolean = false,
    val lastUsedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) {
    val widgetType: WidgetType get() = WidgetType.fromId(typeId)
    val size: WidgetSizePreset
        get() = WidgetSizePreset.entries.firstOrNull { it.name == sizePreset } ?: widgetType.defaultSize
    val fontFamily: WidgetFontFamily
        get() = WidgetFontFamily.entries.firstOrNull { it.key == fontFamilyKey } ?: WidgetFontFamily.PLUS_JAKARTA
    val fontWeight: WidgetFontWeight
        get() = WidgetFontWeight.entries.firstOrNull { it.name == fontWeightName } ?: WidgetFontWeight.MEDIUM
    val alignment: WidgetAlignment
        get() = WidgetAlignment.entries.firstOrNull { it.name == alignmentName } ?: WidgetAlignment.START
    val themeOverride: WidgetThemeOverride
        get() = WidgetThemeOverride.entries.firstOrNull { it.name == themeOverrideName } ?: WidgetThemeOverride.CUSTOM

    fun decodedPayload(): WidgetPayload = WidgetPayloadCodec.decode(payloadJson)

    companion object {
        fun createDefault(type: WidgetType, isDarkSurface: Boolean = false): WidgetConfig {
            val defaultBg = when {
                type == WidgetType.CUSTOM_COLOR -> "#2563EB"
                isDarkSurface -> "#181B20"
                else -> "#FFFFFF"
            }
            val defaultText = when {
                type == WidgetType.CUSTOM_COLOR -> "#FFFFFF"
                isDarkSurface -> "#F4F5F7"
                else -> "#14171C"
            }
            val defaultFont = when (type) {
                WidgetType.DIGITAL_CLOCK, WidgetType.STOPWATCH, WidgetType.POMODORO_TIMER,
                WidgetType.RAM, WidgetType.NETWORK_SPEED -> WidgetFontFamily.JETBRAINS_MONO
                WidgetType.QUOTES, WidgetType.MINIMALIST_CLOCK -> WidgetFontFamily.PLAYFAIR_DISPLAY
                WidgetType.WEATHER, WidgetType.TEMPERATURE -> WidgetFontFamily.SPACE_GROTESK
                else -> WidgetFontFamily.PLUS_JAKARTA
            }
            val defaultAlign = when (type) {
                WidgetType.ANALOG_CLOCK, WidgetType.MINIMALIST_CLOCK, WidgetType.QUOTES,
                WidgetType.QR_CODE, WidgetType.RANDOM_NUMBER, WidgetType.FLASHLIGHT,
                WidgetType.CALCULATOR, WidgetType.WEBSITE, WidgetType.APP_SHORTCUT,
                WidgetType.CUSTOM_ICON_SHORTCUT -> WidgetAlignment.CENTER
                else -> WidgetAlignment.START
            }
            val defaultBgImage = if (type == WidgetType.PHOTO) "preset_horizon" else ""
            val defaultTextOnPhoto = if (type == WidgetType.PHOTO) "#FFFFFF" else defaultText

            return WidgetConfig(
                typeId = type.id,
                title = type.title,
                sizePreset = type.defaultSize.name,
                bgColorHex = defaultBg,
                textColorHex = defaultTextOnPhoto,
                accentColorHex = "#2563EB",
                fontFamilyKey = defaultFont.key,
                fontScale = 1.0f,
                fontWeightName = WidgetFontWeight.MEDIUM.name,
                alpha = 0.96f,
                borderRadiusDp = 22f,
                paddingDp = 16f,
                alignmentName = defaultAlign.name,
                themeOverrideName = WidgetThemeOverride.CUSTOM.name,
                bgImageKey = defaultBgImage,
                bgImageDim = 0.30f,
                payloadJson = WidgetPayloadCodec.encode(WidgetPayloadCodec.defaultFor(type))
            )
        }
    }
}
