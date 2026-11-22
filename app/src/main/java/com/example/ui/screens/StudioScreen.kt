package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.WidgetAlignment
import com.example.data.model.WidgetConfig
import com.example.data.model.WidgetFontFamily
import com.example.data.model.WidgetFontWeight
import com.example.data.model.WidgetPayload
import com.example.data.model.WidgetSizePreset
import com.example.data.model.WidgetThemeOverride
import com.example.data.remote.GeocodingResultDto
import com.example.ui.components.WidgetPreviewCard
import com.example.util.ColorPalette
import com.example.util.InstalledAppEntry
import com.example.util.SystemTelemetry
import kotlin.math.roundToInt

private enum class PreviewBackdrop(val label: String) {
    NEUTRAL("Neutral"),
    WALLPAPER("Wallpaper"),
    DARK("Dark")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioScreen(
    config: WidgetConfig,
    telemetry: SystemTelemetry,
    nowMillis: Long,
    isDarkTheme: Boolean,
    cityResults: List<GeocodingResultDto>,
    isWeatherLoading: Boolean,
    launchableApps: List<InstalledAppEntry>,
    onUpdateConfig: ((WidgetConfig) -> WidgetConfig) -> Unit,
    onUpdatePayload: ((WidgetPayload) -> WidgetPayload) -> Unit,
    onInteractPreview: (Int) -> Unit,
    onPickPhoto: (Uri) -> Unit,
    onSearchCities: (String) -> Unit,
    onSelectCity: (GeocodingResultDto) -> Unit,
    onRefreshWeather: () -> Unit,
    onResetDefault: () -> Unit,
    onSave: () -> Unit,
    onPinToHome: () -> Unit,
    onRequestPermissionMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var backdrop by remember { mutableStateOf(PreviewBackdrop.WALLPAPER) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let(onPickPhoto) }
    )

    Column(modifier = modifier.fillMaxSize()) {
        // Header + Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Customization Studio", style = MaterialTheme.typography.titleLarge)
                Text(
                    "${config.widgetType.title} • ${config.size.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onResetDefault,
                    modifier = Modifier.testTag("studio_reset_button")
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset to default")
                }
                IconButton(
                    onClick = onSave,
                    modifier = Modifier.testTag("studio_save_button")
                ) {
                    Icon(Icons.Rounded.BookmarkAdd, contentDescription = "Save to My Widgets")
                }
            }
        }

        // Live Interactive Preview Stage
        Surface(
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(216.dp)
            ) {
                when (backdrop) {
                    PreviewBackdrop.NEUTRAL -> Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    PreviewBackdrop.DARK -> Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0E1014))
                    )
                    PreviewBackdrop.WALLPAPER -> Image(
                        painter = painterResource(R.drawable.img_preset_horizon_1795326903253),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    WidgetPreviewCard(
                        config = config,
                        telemetry = telemetry,
                        nowMillis = nowMillis,
                        onInteract = onInteractPreview
                    )
                }

                // Backdrop switch pills
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PreviewBackdrop.entries.forEach { mode ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (backdrop == mode) MaterialTheme.colorScheme.primary
                            else Color.Black.copy(alpha = 0.45f),
                            modifier = Modifier.clickable { backdrop = mode }
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Primary Save + Pin CTA Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onSave,
                modifier = Modifier
                    .weight(1f)
                    .testTag("studio_save_cta")
            ) {
                Icon(Icons.Rounded.BookmarkAdd, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Save")
            }
            Button(
                onClick = onPinToHome,
                modifier = Modifier
                    .weight(1.4f)
                    .testTag("studio_pin_cta")
            ) {
                Icon(Icons.Rounded.AddToHomeScreen, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Add to Home")
            }
        }

        // Editor Tabs
        val tabs = listOf("Style", "Type", "Layout", "Content")
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title, fontWeight = FontWeight.Medium) },
                    modifier = Modifier.testTag("studio_tab_${title.lowercase()}")
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    item {
                        ControlSection("Theme Mode Override") {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                WidgetThemeOverride.entries.forEach { mode ->
                                    FilterChip(
                                        selected = config.themeOverride == mode,
                                        onClick = { onUpdateConfig { it.copy(themeOverrideName = mode.name) } },
                                        label = { Text(mode.displayName) }
                                    )
                                }
                            }
                        }
                    }
                    item {
                        ColorSwatchPicker(
                            label = "Background Color",
                            selectedHex = config.bgColorHex,
                            swatches = ColorPalette.widgetBackgroundSwatches,
                            onSelect = { hex ->
                                onUpdateConfig {
                                    it.copy(
                                        bgColorHex = hex,
                                        themeOverrideName = WidgetThemeOverride.CUSTOM.name
                                    )
                                }
                            }
                        )
                    }
                    item {
                        ColorSwatchPicker(
                            label = "Text Color",
                            selectedHex = config.textColorHex,
                            swatches = ColorPalette.widgetTextSwatches,
                            onSelect = { hex ->
                                onUpdateConfig {
                                    it.copy(
                                        textColorHex = hex,
                                        themeOverrideName = WidgetThemeOverride.CUSTOM.name
                                    )
                                }
                            }
                        )
                    }
                    item {
                        ColorSwatchPicker(
                            label = "Accent Color",
                            selectedHex = config.accentColorHex,
                            swatches = ColorPalette.widgetTextSwatches,
                            onSelect = { hex -> onUpdateConfig { it.copy(accentColorHex = hex) } }
                        )
                    }
                    item {
                        SliderControl(
                            label = "Transparency (Opacity)",
                            valueText = "${(config.alpha * 100).roundToInt()}%",
                            value = config.alpha,
                            valueRange = 0.1f..1.0f,
                            onValueChange = { a -> onUpdateConfig { it.copy(alpha = a) } }
                        )
                    }
                    if (config.widgetType.supportsBgImage) {
                        item {
                            ControlSection("Background Image") {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = config.bgImageKey.isBlank(),
                                        onClick = { onUpdateConfig { it.copy(bgImageKey = "") } },
                                        label = { Text("None") }
                                    )
                                    FilterChip(
                                        selected = config.bgImageKey == "preset_horizon",
                                        onClick = { onUpdateConfig { it.copy(bgImageKey = "preset_horizon") } },
                                        label = { Text("Horizon") }
                                    )
                                    FilterChip(
                                        selected = config.bgImageKey == "preset_botanical",
                                        onClick = { onUpdateConfig { it.copy(bgImageKey = "preset_botanical") } },
                                        label = { Text("Botanical") }
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            photoPicker.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                    ) {
                                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Pick Photo")
                                    }
                                }
                                if (config.bgImageKey.isNotBlank()) {
                                    Spacer(Modifier.height(10.dp))
                                    SliderControl(
                                        label = "Photo Dimming Overlay",
                                        valueText = "${(config.bgImageDim * 100).roundToInt()}%",
                                        value = config.bgImageDim,
                                        valueRange = 0f..0.8f,
                                        onValueChange = { d -> onUpdateConfig { it.copy(bgImageDim = d) } }
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    item {
                        ControlSection("Font Family") {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                WidgetFontFamily.entries.forEach { family ->
                                    FilterChip(
                                        selected = config.fontFamily == family,
                                        onClick = { onUpdateConfig { it.copy(fontFamilyKey = family.key) } },
                                        label = { Text(family.displayName) }
                                    )
                                }
                            }
                        }
                    }
                    item {
                        ControlSection("Font Weight") {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                WidgetFontWeight.entries.forEach { weight ->
                                    FilterChip(
                                        selected = config.fontWeight == weight,
                                        onClick = { onUpdateConfig { it.copy(fontWeightName = weight.name) } },
                                        label = { Text(weight.displayName) }
                                    )
                                }
                            }
                        }
                    }
                    item {
                        SliderControl(
                            label = "Font Size Scale",
                            valueText = String.format("%.2f×", config.fontScale),
                            value = config.fontScale,
                            valueRange = 0.75f..1.50f,
                            onValueChange = { s -> onUpdateConfig { it.copy(fontScale = s) } }
                        )
                    }
                }
                2 -> {
                    item {
                        OutlinedTextField(
                            value = config.title,
                            onValueChange = { t -> onUpdateConfig { it.copy(title = t) } },
                            label = { Text("Widget Label") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        ControlSection("Widget Dimensions") {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                WidgetSizePreset.entries.forEach { size ->
                                    FilterChip(
                                        selected = config.size == size,
                                        onClick = { onUpdateConfig { it.copy(sizePreset = size.name) } },
                                        label = { Text(size.label) }
                                    )
                                }
                            }
                        }
                    }
                    item {
                        ControlSection("Text Alignment") {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                WidgetAlignment.entries.forEach { align ->
                                    FilterChip(
                                        selected = config.alignment == align,
                                        onClick = { onUpdateConfig { it.copy(alignmentName = align.name) } },
                                        label = { Text(align.displayName) }
                                    )
                                }
                            }
                        }
                    }
                    item {
                        SliderControl(
                            label = "Border Radius",
                            valueText = "${config.borderRadiusDp.roundToInt()} dp",
                            value = config.borderRadiusDp,
                            valueRange = 0f..36f,
                            onValueChange = { r -> onUpdateConfig { it.copy(borderRadiusDp = r) } }
                        )
                    }
                    item {
                        SliderControl(
                            label = "Inner Padding",
                            valueText = "${config.paddingDp.roundToInt()} dp",
                            value = config.paddingDp,
                            valueRange = 4f..28f,
                            onValueChange = { p -> onUpdateConfig { it.copy(paddingDp = p) } }
                        )
                    }
                }
                3 -> {
                    item {
                        StudioContentTab(
                            config = config,
                            cityResults = cityResults,
                            isWeatherLoading = isWeatherLoading,
                            launchableApps = launchableApps,
                            onUpdatePayload = onUpdatePayload,
                            onSearchCities = onSearchCities,
                            onSelectCity = onSelectCity,
                            onRefreshWeather = onRefreshWeather,
                            onRequestPermissionMessage = onRequestPermissionMessage
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ControlSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable
private fun SliderControl(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valueText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSwatchPicker(
    label: String,
    selectedHex: String,
    swatches: List<String>,
    onSelect: (String) -> Unit
) {
    var customHex by remember(selectedHex) { mutableStateOf(selectedHex) }
    ControlSection(label) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            swatches.forEach { hex ->
                val color = ColorPalette.parseComposeColor(hex)
                val selected = selectedHex.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (selected) 2.5.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                        .clickable {
                            customHex = hex
                            onSelect(hex)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = if (ColorPalette.isDarkColor(ColorPalette.parseColorInt(hex))) Color.White else Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = customHex,
            onValueChange = { input ->
                customHex = input
                if (input.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                    onSelect(input.uppercase())
                }
            },
            label = { Text("Custom Hex (#RRGGBB)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
