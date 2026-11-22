package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.HabitItem
import com.example.data.model.TodoItem
import com.example.data.model.WidgetConfig
import com.example.data.model.WidgetPayload
import com.example.data.model.WidgetType
import com.example.data.remote.GeocodingResultDto
import com.example.util.InstalledAppEntry
import com.example.util.QuotesBank

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioContentTab(
    config: WidgetConfig,
    cityResults: List<GeocodingResultDto>,
    isWeatherLoading: Boolean,
    launchableApps: List<InstalledAppEntry>,
    onUpdatePayload: ((WidgetPayload) -> WidgetPayload) -> Unit,
    onSearchCities: (String) -> Unit,
    onSelectCity: (GeocodingResultDto) -> Unit,
    onRefreshWeather: () -> Unit,
    onRequestPermissionMessage: (String) -> Unit
) {
    val payload = config.decodedPayload()

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                onUpdatePayload { it.copy(reminderNotifyEnabled = true) }
                onRequestPermissionMessage("Reminder notifications enabled.")
            } else {
                onUpdatePayload { it.copy(reminderNotifyEnabled = false) }
                onRequestPermissionMessage("Notification permission denied; widget check-off still works locally.")
            }
        }
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        config.widgetType.platformNote?.let { note ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        when (config.widgetType) {
            WidgetType.DIGITAL_CLOCK, WidgetType.MINIMALIST_CLOCK -> {
                ToggleRow("24-Hour Time", payload.use24Hour) { checked ->
                    onUpdatePayload { it.copy(use24Hour = checked) }
                }
                ToggleRow("Show Seconds (in-app preview)", payload.showSeconds) { checked ->
                    onUpdatePayload { it.copy(showSeconds = checked) }
                }
                ToggleRow("Show Date Line", payload.showDate) { checked ->
                    onUpdatePayload { it.copy(showDate = checked) }
                }
            }

            WidgetType.NOTES, WidgetType.CUSTOM_TEXT, WidgetType.CUSTOM_COLOR, WidgetType.PHOTO -> {
                OutlinedTextField(
                    value = payload.heading,
                    onValueChange = { v -> onUpdatePayload { it.copy(heading = v) } },
                    label = { Text("Heading / Caption") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (config.widgetType != WidgetType.PHOTO) {
                    OutlinedTextField(
                        value = payload.bodyText,
                        onValueChange = { v -> onUpdatePayload { it.copy(bodyText = v) } },
                        label = { Text("Body Text") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            WidgetType.TODO_LIST -> {
                var newTask by remember { mutableStateOf("") }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newTask,
                        onValueChange = { newTask = it },
                        label = { Text("Add task") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        if (newTask.isNotBlank()) {
                            val item = TodoItem("t_${System.currentTimeMillis()}", newTask.trim(), false)
                            onUpdatePayload { it.copy(todos = it.todos + item) }
                            newTask = ""
                        }
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add")
                    }
                }
                payload.todos.forEach { todo ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = todo.done,
                            onCheckedChange = { checked ->
                                onUpdatePayload {
                                    it.copy(todos = it.todos.map { t -> if (t.id == todo.id) t.copy(done = checked) else t })
                                }
                            }
                        )
                        Text(todo.text, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            onUpdatePayload { it.copy(todos = it.todos.filterNot { t -> t.id == todo.id }) }
                        }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove task")
                        }
                    }
                }
            }

            WidgetType.HABIT_TRACKER -> {
                var newHabit by remember { mutableStateOf("") }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newHabit,
                        onValueChange = { newHabit = it },
                        label = { Text("New habit") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        if (newHabit.isNotBlank()) {
                            val h = HabitItem("h_${System.currentTimeMillis()}", newHabit.trim(), false, 0)
                            onUpdatePayload { it.copy(habits = it.habits + h) }
                            newHabit = ""
                        }
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add habit")
                    }
                }
                payload.habits.forEach { habit ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = habit.completedToday,
                            onCheckedChange = { checked ->
                                onUpdatePayload {
                                    it.copy(habits = it.habits.map { h ->
                                        if (h.id == habit.id) h.copy(
                                            completedToday = checked,
                                            streak = if (checked) h.streak + 1 else (h.streak - 1).coerceAtLeast(0)
                                        ) else h
                                    })
                                }
                            }
                        )
                        Text("${habit.name} (${habit.streak}d)", modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            onUpdatePayload { it.copy(habits = it.habits.filterNot { h -> h.id == habit.id }) }
                        }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove habit")
                        }
                    }
                }
            }

            WidgetType.DAILY_GOALS -> {
                OutlinedTextField(
                    value = payload.goalTitle,
                    onValueChange = { v -> onUpdatePayload { it.copy(goalTitle = v) } },
                    label = { Text("Goal Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = payload.goalTarget.toString(),
                        onValueChange = { v ->
                            v.toIntOrNull()?.let { target ->
                                onUpdatePayload { it.copy(goalTarget = target.coerceIn(1, 9999)) }
                            }
                        },
                        label = { Text("Target") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = payload.goalUnit,
                        onValueChange = { v -> onUpdatePayload { it.copy(goalUnit = v) } },
                        label = { Text("Unit") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            WidgetType.COUNTDOWN -> {
                OutlinedTextField(
                    value = payload.countdownTitle,
                    onValueChange = { v -> onUpdatePayload { it.copy(countdownTitle = v) } },
                    label = { Text("Event Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ControlSection("Target Offset") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 7, 14, 30, 90, 365).forEach { days ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val target = System.currentTimeMillis() + days * 24L * 60 * 60 * 1000
                                    onUpdatePayload { it.copy(countdownTargetEpochMs = target) }
                                },
                                label = { Text("+${days}d") }
                            )
                        }
                    }
                }
            }

            WidgetType.POMODORO_TIMER -> {
                ControlSection("Focus Duration") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 25, 45, 50).forEach { mins ->
                            FilterChip(
                                selected = payload.pomodoroMinutes == mins,
                                onClick = {
                                    onUpdatePayload {
                                        it.copy(
                                            pomodoroMinutes = mins,
                                            pomodoroRunning = false,
                                            pomodoroIsBreak = false,
                                            pomodoroRemainingMs = mins * 60_000L
                                        )
                                    }
                                },
                                label = { Text("${mins}m") }
                            )
                        }
                    }
                }
            }

            WidgetType.WEATHER, WidgetType.TEMPERATURE, WidgetType.WEATHER_FORECAST, WidgetType.SUNRISE_SUNSET -> {
                var cityQuery by remember { mutableStateOf("") }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = cityQuery,
                        onValueChange = { cityQuery = it },
                        label = { Text("Search City (Open-Meteo)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onSearchCities(cityQuery) }) {
                        Icon(Icons.Rounded.Search, contentDescription = "Search city")
                    }
                }
                cityResults.forEach { city ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCity(city) }
                    ) {
                        Text(
                            text = listOfNotNull(city.name, city.admin1, city.country).joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                ToggleRow("Use Fahrenheit (°F)", payload.useFahrenheit) { f ->
                    onUpdatePayload { it.copy(useFahrenheit = f) }
                }
                OutlinedButton(
                    onClick = onRefreshWeather,
                    enabled = !isWeatherLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isWeatherLoading) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.height(16.dp).width(16.dp))
                    } else {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Refresh Live Weather (${payload.cityName})")
                }
            }

            WidgetType.MULTI_TIMEZONE, WidgetType.WORLD_CLOCK -> {
                ToggleRow("24-Hour Time", payload.use24Hour) { checked ->
                    onUpdatePayload { it.copy(use24Hour = checked) }
                }
                val presets = listOf(
                    "America/New_York", "America/Los_Angeles", "Europe/London",
                    "Europe/Paris", "Asia/Tokyo", "Asia/Singapore", "Australia/Sydney"
                )
                ControlSection("Active Time Zones (Pick up to 3)") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { tz ->
                            val selected = tz in payload.timeZones
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    val next = if (selected) {
                                        (payload.timeZones - tz).ifEmpty { listOf(tz) }
                                    } else {
                                        (payload.timeZones + tz).takeLast(3)
                                    }
                                    onUpdatePayload { it.copy(timeZones = next) }
                                },
                                label = { Text(tz.substringAfterLast("/").replace("_", " ")) }
                            )
                        }
                    }
                }
            }

            WidgetType.QUOTES -> {
                OutlinedTextField(
                    value = payload.customQuote,
                    onValueChange = { v -> onUpdatePayload { it.copy(customQuote = v) } },
                    label = { Text("Custom Quote (leave blank for curated deck)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = {
                    onUpdatePayload { it.copy(customQuote = "", quoteIndex = (it.quoteIndex + 1) % QuotesBank.quotes.size) }
                }) {
                    Text("Next Curated Quote")
                }
            }

            WidgetType.QR_CODE -> {
                OutlinedTextField(
                    value = payload.heading,
                    onValueChange = { v -> onUpdatePayload { it.copy(heading = v) } },
                    label = { Text("Label Below QR") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = payload.qrContent,
                    onValueChange = { v -> onUpdatePayload { it.copy(qrContent = v) } },
                    label = { Text("QR URL, Wi-Fi String, or Text") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            WidgetType.WEBSITE -> {
                OutlinedTextField(
                    value = payload.heading,
                    onValueChange = { v -> onUpdatePayload { it.copy(heading = v) } },
                    label = { Text("Shortcut Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = payload.targetUrl,
                    onValueChange = { v -> onUpdatePayload { it.copy(targetUrl = v) } },
                    label = { Text("Website URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            WidgetType.APP_SHORTCUT, WidgetType.CUSTOM_ICON_SHORTCUT -> {
                ControlSection("Target Installed App (${payload.targetAppLabel})") {
                    if (launchableApps.isEmpty()) {
                        Text("No launchable apps reported by system.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        launchableApps.take(12).forEach { app ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (payload.targetPackage == app.packageName) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onUpdatePayload {
                                            it.copy(targetPackage = app.packageName, targetAppLabel = app.label)
                                        }
                                    }
                            ) {
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            WidgetType.RANDOM_NUMBER -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = payload.rngMin.toString(),
                        onValueChange = { v -> v.toIntOrNull()?.let { m -> onUpdatePayload { it.copy(rngMin = m) } } },
                        label = { Text("Minimum") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = payload.rngMax.toString(),
                        onValueChange = { v -> v.toIntOrNull()?.let { m -> onUpdatePayload { it.copy(rngMax = m) } } },
                        label = { Text("Maximum") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            WidgetType.DAILY_REMINDER -> {
                OutlinedTextField(
                    value = payload.reminderTitle,
                    onValueChange = { v -> onUpdatePayload { it.copy(reminderTitle = v) } },
                    label = { Text("Reminder Text") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = payload.reminderTime,
                    onValueChange = { v -> onUpdatePayload { it.copy(reminderTime = v) } },
                    label = { Text("Reminder Time (e.g. 14:00)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ToggleRow("Enable Local Alert Permission", payload.reminderNotifyEnabled) { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onUpdatePayload { it.copy(reminderNotifyEnabled = enabled) }
                    }
                }
            }

            WidgetType.DAILY_AGENDA -> {
                var newTime by remember { mutableStateOf("10:00") }
                var newTitle by remember { mutableStateOf("") }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newTime,
                        onValueChange = { newTime = it },
                        label = { Text("Time") },
                        singleLine = true,
                        modifier = Modifier.width(96.dp)
                    )
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Agenda Item") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = {
                        if (newTitle.isNotBlank()) {
                            onUpdatePayload {
                                it.copy(agenda = it.agenda + com.example.data.model.AgendaItem(newTime.trim().ifBlank { "12:00" }, newTitle.trim()))
                            }
                            newTitle = ""
                        }
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add agenda item")
                    }
                }
                payload.agenda.forEachIndexed { idx, item ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${item.time}  •  ${item.title}", modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            onUpdatePayload {
                                it.copy(agenda = it.agenda.filterIndexed { i, _ -> i != idx })
                            }
                        }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove agenda item")
                        }
                    }
                }
            }

            else -> {
                Text(
                    text = "This widget updates automatically from live device telemetry or system time. Tap Style, Type, or Layout tabs to customize its appearance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
