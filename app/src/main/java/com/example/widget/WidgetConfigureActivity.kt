package com.example.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.WidgetLiteApplication
import com.example.data.local.UserSettings
import com.example.data.model.WidgetConfig
import com.example.data.model.WidgetType
import com.example.ui.theme.WidgetLiteTheme
import kotlinx.coroutines.launch

class WidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val app = application as WidgetLiteApplication
        val repo = app.repository

        setContent {
            val settings by repo.userSettings.collectAsStateWithLifecycle(initialValue = UserSettings())
            val savedWidgets by repo.allWidgets.collectAsStateWithLifecycle(initialValue = emptyList())
            val scope = rememberCoroutineScope()
            var selectedTab by remember { mutableIntStateOf(0) }

            WidgetLiteTheme(themeMode = settings.themeMode, accentColor = settings.accentColor) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Choose Widget", fontWeight = FontWeight.SemiBold) },
                            navigationIcon = {
                                IconButton(
                                    onClick = { finish() },
                                    modifier = Modifier.testTag("configure_close_button")
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Cancel")
                                }
                            }
                        )
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        TabRow(selectedTabIndex = selectedTab) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("Saved (${savedWidgets.size})") }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("All Templates (${WidgetType.entries.size})") }
                            )
                        }

                        if (selectedTab == 0) {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(savedWidgets, key = { it.id }) { config ->
                                    ConfigPickerCard(
                                        title = config.title,
                                        subtitle = "${config.widgetType.category.displayName} • ${config.size.label}",
                                        isSaved = true,
                                        onClick = {
                                            scope.launch {
                                                repo.bindAppWidget(config.id, appWidgetId)
                                                WidgetUpdateHelper.updateAppWidget(
                                                    this@WidgetConfigureActivity,
                                                    AppWidgetManager.getInstance(this@WidgetConfigureActivity),
                                                    appWidgetId
                                                )
                                                val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                                setResult(Activity.RESULT_OK, resultValue)
                                                finish()
                                            }
                                        }
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(WidgetType.entries, key = { it.id }) { type ->
                                    ConfigPickerCard(
                                        title = type.title,
                                        subtitle = "${type.category.displayName} • ${type.subtitle}",
                                        isSaved = false,
                                        onClick = {
                                            scope.launch {
                                                val newConfig = WidgetConfig.createDefault(type)
                                                val id = repo.saveWidget(newConfig)
                                                repo.bindAppWidget(id, appWidgetId)
                                                WidgetUpdateHelper.updateAppWidget(
                                                    this@WidgetConfigureActivity,
                                                    AppWidgetManager.getInstance(this@WidgetConfigureActivity),
                                                    appWidgetId
                                                )
                                                val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                                setResult(Activity.RESULT_OK, resultValue)
                                                finish()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigPickerCard(
    title: String,
    subtitle: String,
    isSaved: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                imageVector = if (isSaved) Icons.Rounded.CheckCircle else Icons.Rounded.Add,
                contentDescription = "Select widget",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
