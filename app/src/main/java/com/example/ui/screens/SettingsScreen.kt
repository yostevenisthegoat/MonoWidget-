package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.AppAccentColor
import com.example.data.local.AppThemeMode
import com.example.data.local.UserSettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: UserSettings,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onSetAccentColor: (AppAccentColor) -> Unit,
    onSetAllowWeatherNetwork: (Boolean) -> Unit,
    onBuildExportJson: suspend () -> String,
    onImportJson: (String) -> Unit,
    onClearLocalPhotos: () -> Unit,
    onRefreshWidgets: () -> Unit,
    onNotify: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showPlatformGuideDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            if (uri != null) {
                scope.launch {
                    try {
                        val json = onBuildExportJson()
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(json.toByteArray(Charsets.UTF_8))
                        }
                        onNotify("Exported widget configurations.")
                    } catch (_: Exception) {
                        onNotify("Failed to export file.")
                    }
                }
            }
        }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (!json.isNullOrBlank()) {
                        onImportJson(json)
                    }
                } catch (_: Exception) {
                    onNotify("Could not read selected file.")
                }
            }
        }
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column {
                Text("Settings", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Theme, privacy controls & open-source transparency",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Theme & Accent
        item {
            SettingsCard("Appearance") {
                Text("Theme Mode", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { onSetThemeMode(mode) },
                            label = { Text(mode.displayName) },
                            modifier = Modifier.testTag("theme_chip_${mode.name}")
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text("Accent Color", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppAccentColor.entries.forEach { accent ->
                        val selected = settings.accentColor == accent
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(accent.colorLong))
                                .border(
                                    width = if (selected) 2.5.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable { onSetAccentColor(accent) }
                                .testTag("accent_${accent.name}"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = accent.displayName,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Privacy Settings
        item {
            SettingsCard("Privacy & Local-First Storage") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "Zero analytics, zero ad SDKs, zero tracking, and no account required.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Allow Weather Network Fetch", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Connects only to Open-Meteo when you refresh weather widgets.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.allowWeatherNetwork,
                        onCheckedChange = onSetAllowWeatherNetwork,
                        modifier = Modifier.testTag("privacy_weather_switch")
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onClearLocalPhotos, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Clear Cached Widget Photos")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onRefreshWidgets, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Refresh Active Home Screen Widgets")
                }
            }
        }

        // Export & Import
        item {
            SettingsCard("Backup & Portability") {
                Text(
                    "Export or import your widget configurations as portable JSON.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch("widgetlite-backup.json") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_configs_button")
                    ) {
                        Icon(Icons.Rounded.Upload, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Export")
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_configs_button")
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Import")
                    }
                }
            }
        }

        // Open Source & Platform Transparency
        item {
            SettingsCard("About & Open Source") {
                SettingsLinkRow("Android Platform Limitations Guide", Icons.Rounded.Info) {
                    showPlatformGuideDialog = true
                }
                SettingsLinkRow("Open-Source Licenses", Icons.Rounded.Code) {
                    showLicensesDialog = true
                }
                SettingsLinkRow("About WidgetLite", Icons.Rounded.Info) {
                    showAboutDialog = true
                }
                SettingsLinkRow("GitHub Repository (MIT License)", Icons.AutoMirrored.Rounded.OpenInNew) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/widgetlite/widgetlite-android"))
                    context.startActivity(intent)
                }
            }
        }
    }

    if (showPlatformGuideDialog) {
        AlertDialog(
            onDismissRequest = { showPlatformGuideDialog = false },
            title = { Text("Android Platform Notes") },
            text = {
                Text(
                    "• Background Refresh: Android limits standard AppWidget periodic updates to >= 30 minutes to preserve battery life.\n\n" +
                        "• Wi-Fi SSID: Android 10+ hides network SSIDs unless background/fine location is granted. WidgetLite skips invasive location prompts and shows private signal & link speed instead.\n\n" +
                        "• Quick Settings: Android prevents third-party apps from toggling system radios silently, so Quick Settings shortcuts jump straight to the exact system panel.\n\n" +
                        "• Launcher Pinning: Most modern launchers support in-app pinning; if yours doesn't, add WidgetLite via your launcher's widget drawer."
                )
            },
            confirmButton = {
                TextButton(onClick = { showPlatformGuideDialog = false }) { Text("Got it") }
            }
        )
    }

    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            title = { Text("Open-Source Licenses") },
            text = {
                Text(
                    "WidgetLite is licensed under the MIT License.\n\n" +
                        "Open-source libraries used:\n" +
                        "• Kotlin & Coroutines (Apache 2.0)\n" +
                        "• Jetpack Compose, Material 3, Room, DataStore, Navigation (Apache 2.0)\n" +
                        "• Retrofit, OkHttp, Moshi (Apache 2.0)\n" +
                        "• Coil Image Loader (Apache 2.0)\n" +
                        "• Google Fonts: Plus Jakarta Sans, Inter, Space Grotesk, JetBrains Mono, Playfair Display, Outfit (SIL OFL 1.1)\n" +
                        "• Open-Meteo Weather API (CC BY 4.0)"
                )
            },
            confirmButton = {
                TextButton(onClick = { showLicensesDialog = false }) { Text("Close") }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About WidgetLite") },
            text = {
                Text(
                    "WidgetLite v1.0\n\n" +
                        "Your Home Screen. Your Freedom. Completely Free.\n\n" +
                        "No ads, no subscriptions, no paywalls, and no locked widgets. Every customization option and all 39 native Android home screen widgets are unlocked forever."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingsLinkRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}
