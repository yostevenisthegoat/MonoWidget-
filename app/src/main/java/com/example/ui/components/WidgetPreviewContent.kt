package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WidgetConfig
import com.example.data.model.WidgetSizePreset
import com.example.data.model.WidgetType
import com.example.util.QrCodeGenerator
import com.example.util.QuotesBank
import com.example.util.SunCalculator
import com.example.util.SystemTelemetry
import com.example.widget.WidgetCanvasContent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WidgetPreviewContent(
    config: WidgetConfig,
    style: ComposeWidgetStyle,
    telemetry: SystemTelemetry,
    nowMillis: Long,
    onInteract: ((Int) -> Unit)?
) {
    val payload = config.decodedPayload()
    val now = Date(nowMillis)

    fun textStyle(sizeSp: Float, color: Color = style.textColor, weight: FontWeight = style.fontWeight) =
        TextStyle(
            color = color,
            fontFamily = style.fontFamily,
            fontWeight = weight,
            fontSize = (sizeSp * style.fontScale).sp,
            textAlign = style.textAlign
        )

    @Composable
    fun Kicker(text: String) {
        Text(
            text = text.uppercase(),
            style = textStyle(10f, style.accentColor, FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }

    when (config.widgetType) {
        WidgetType.DIGITAL_CLOCK, WidgetType.MINIMALIST_CLOCK -> {
            val pattern = when {
                payload.use24Hour && payload.showSeconds -> "HH:mm:ss"
                payload.use24Hour -> "HH:mm"
                payload.showSeconds -> "hh:mm:ss a"
                else -> "h:mm a"
            }
            val timeStr = SimpleDateFormat(pattern, Locale.getDefault()).format(now)
            val dateStr = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(now)
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = style.horizontalAlign
            ) {
                Text(
                    text = timeStr,
                    style = textStyle(if (config.widgetType == WidgetType.MINIMALIST_CLOCK) 26f else 30f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                if (payload.showDate) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = dateStr,
                        style = textStyle(12f, style.secondaryTextColor),
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        WidgetType.ANALOG_CLOCK -> {
            val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
            val hr = cal.get(Calendar.HOUR)
            val min = cal.get(Calendar.MINUTE)
            val sec = cal.get(Calendar.SECOND)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize(0.86f).aspectRatio(1f)) {
                    val r = size.minDimension / 2f
                    val c = center
                    drawCircle(color = style.trackColor, radius = r, style = Stroke(width = 3.dp.toPx()))
                    for (i in 0 until 12) {
                        val a = Math.toRadians((i * 30 - 90).toDouble())
                        drawLine(
                            color = style.secondaryTextColor,
                            start = Offset(c.x + (cos(a) * r * 0.84f).toFloat(), c.y + (sin(a) * r * 0.84f).toFloat()),
                            end = Offset(c.x + (cos(a) * r * 0.94f).toFloat(), c.y + (sin(a) * r * 0.94f).toFloat()),
                            strokeWidth = 1.6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    val hrA = Math.toRadians(((hr + min / 60f) * 30f - 90f).toDouble())
                    val minA = Math.toRadians((min * 6f - 90f).toDouble())
                    val secA = Math.toRadians((sec * 6f - 90f).toDouble())
                    drawLine(
                        color = style.textColor,
                        start = c,
                        end = Offset(c.x + (cos(hrA) * r * 0.50f).toFloat(), c.y + (sin(hrA) * r * 0.50f).toFloat()),
                        strokeWidth = 3.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = style.textColor,
                        start = c,
                        end = Offset(c.x + (cos(minA) * r * 0.74f).toFloat(), c.y + (sin(minA) * r * 0.74f).toFloat()),
                        strokeWidth = 2.4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = style.accentColor,
                        start = c,
                        end = Offset(c.x + (cos(secA) * r * 0.82f).toFloat(), c.y + (sin(secA) * r * 0.82f).toFloat()),
                        strokeWidth = 1.4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = style.accentColor, radius = 4.dp.toPx())
                }
            }
        }

        WidgetType.CALENDAR, WidgetType.THEMED_CALENDAR -> {
            val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
            val monthStr = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(now)
            if (config.widgetType == WidgetType.THEMED_CALENDAR && config.size != WidgetSizePreset.LARGE_4X4) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = style.horizontalAlign
                ) {
                    Kicker(monthStr)
                    Text(
                        text = SimpleDateFormat("dd", Locale.getDefault()).format(now),
                        style = textStyle(30f),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = SimpleDateFormat("EEEE", Locale.getDefault()).format(now),
                        style = textStyle(12f, style.secondaryTextColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                val today = cal.get(Calendar.DAY_OF_MONTH)
                val temp = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
                val firstDow = temp.get(Calendar.DAY_OF_WEEK) - 1
                val daysInMonth = temp.getActualMaximum(Calendar.DAY_OF_MONTH)
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Kicker(monthStr)
                    Row(Modifier.fillMaxWidth()) {
                        listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
                            Text(
                                text = d,
                                style = textStyle(10f, style.secondaryTextColor),
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    val totalSlots = firstDow + daysInMonth
                    val rows = ((totalSlots + 6) / 7).coerceAtMost(6)
                    for (r in 0 until rows) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            for (c in 0 until 7) {
                                val day = r * 7 + c - firstDow + 1
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    if (day in 1..daysInMonth) {
                                        val isToday = day == today
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(if (isToday) style.accentColor else Color.Transparent),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = day.toString(),
                                                style = textStyle(11f, if (isToday) Color.White else style.textColor),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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

        WidgetType.DAILY_AGENDA -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Kicker("Today's Agenda")
                payload.agenda.take(3).forEach { item ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(item.time, style = textStyle(11f, style.secondaryTextColor), maxLines = 1)
                        Text(item.title, style = textStyle(12f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        WidgetType.TODO_LIST -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Kicker("To-Do List")
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    payload.todos.take(4).forEachIndexed { idx, item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(enabled = onInteract != null) { onInteract?.invoke(idx + 1) }
                                .padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (item.done) style.accentColor else style.trackColor)
                            )
                            Text(
                                text = item.text,
                                style = textStyle(12.5f, if (item.done) style.secondaryTextColor else style.textColor)
                                    .copy(textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                ActionPillRow(listOf("Toggle #1", "Toggle #2", "Toggle #3"), style, onInteract)
            }
        }

        WidgetType.NOTES, WidgetType.CUSTOM_TEXT, WidgetType.CUSTOM_COLOR -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = style.horizontalAlign
            ) {
                Kicker(payload.heading.ifBlank { config.title })
                Text(
                    text = payload.bodyText.ifBlank {
                        if (config.widgetType == WidgetType.CUSTOM_COLOR) config.bgColorHex else "Tap to customize text"
                    },
                    style = textStyle(13.5f),
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        WidgetType.COUNTDOWN -> {
            val diffMs = (payload.countdownTargetEpochMs - nowMillis).coerceAtLeast(0L)
            val days = diffMs / (1000L * 60 * 60 * 24)
            val hrs = (diffMs / (1000L * 60 * 60)) % 24
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = style.horizontalAlign
            ) {
                Kicker(payload.countdownTitle)
                Text("${days}d", style = textStyle(28f), modifier = Modifier.fillMaxWidth())
                Text("$hrs hrs left", style = textStyle(11f, style.secondaryTextColor), modifier = Modifier.fillMaxWidth())
            }
        }

        WidgetType.STOPWATCH -> {
            val elapsed = if (payload.stopwatchRunning && payload.stopwatchStartedAtMs > 0L) {
                payload.stopwatchAccumulatedMs + (nowMillis - payload.stopwatchStartedAtMs)
            } else payload.stopwatchAccumulatedMs
            val secTotal = (elapsed / 1000L).coerceAtLeast(0L)
            val formatted = String.format(Locale.US, "%02d:%02d", (secTotal / 60), secTotal % 60)
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = style.horizontalAlign
            ) {
                Kicker(if (payload.stopwatchRunning) "Stopwatch • Running" else "Stopwatch")
                Text(formatted, style = textStyle(26f), modifier = Modifier.fillMaxWidth())
                ActionPillRow(listOf(if (payload.stopwatchRunning) "Pause" else "Start", "Reset"), style, onInteract)
            }
        }

        WidgetType.POMODORO_TIMER -> {
            val remMs = WidgetCanvasContent.computePomodoroRemaining(payload)
            val secTotal = (remMs / 1000L).coerceAtLeast(0L)
            val formatted = String.format(Locale.US, "%02d:%02d", secTotal / 60, secTotal % 60)
            val phase = if (payload.pomodoroIsBreak) "Break" else "Focus"
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = style.horizontalAlign
            ) {
                Kicker("$phase • ${if (payload.pomodoroRunning) "Active" else "Paused"}")
                Text(formatted, style = textStyle(26f), modifier = Modifier.fillMaxWidth())
                ActionPillRow(listOf(if (payload.pomodoroRunning) "Pause" else "Start", "Switch", "Reset"), style, onInteract)
            }
        }

        WidgetType.DAILY_GOALS -> {
            val pct = (payload.goalCurrent.toFloat() / payload.goalTarget.coerceAtLeast(1)).coerceIn(0f, 1f)
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Kicker(payload.goalTitle)
                Text("${payload.goalCurrent} / ${payload.goalTarget} ${payload.goalUnit}", style = textStyle(17f), modifier = Modifier.fillMaxWidth())
                PreviewProgressBar(pct, style)
                ActionPillRow(listOf("-1", "+1", "Reset"), style, onInteract)
            }
        }

        WidgetType.HABIT_TRACKER -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Kicker("Habit Tracker")
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    payload.habits.take(3).forEachIndexed { idx, h ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(enabled = onInteract != null) { onInteract?.invoke(idx + 1) },
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = (if (h.completedToday) "✓ " else "○ ") + h.name,
                                style = textStyle(12f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text("${h.streak}d", style = textStyle(11f, style.accentColor))
                        }
                    }
                }
                ActionPillRow(listOf("Habit 1", "Habit 2", "Habit 3"), style, onInteract)
            }
        }

        WidgetType.BATTERY -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = style.horizontalAlign
            ) {
                Kicker(if (telemetry.isCharging) "Charging" else "Battery")
                Text("${telemetry.batteryPercent}%", style = textStyle(26f), modifier = Modifier.fillMaxWidth())
                PreviewProgressBar(telemetry.batteryPercent / 100f, style)
            }
        }

        WidgetType.STORAGE -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Kicker("Storage")
                Text(
                    String.format(Locale.US, "%.1f / %.0f GB", telemetry.storageUsedGb, telemetry.storageTotalGb),
                    style = textStyle(16f),
                    modifier = Modifier.fillMaxWidth()
                )
                PreviewProgressBar(telemetry.storageUsedPercent / 100f, style)
            }
        }

        WidgetType.RAM -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Kicker("System RAM")
                Text("${telemetry.ramUsedMb} / ${telemetry.ramTotalMb} MB", style = textStyle(15f), modifier = Modifier.fillMaxWidth())
                PreviewProgressBar(telemetry.ramUsedPercent / 100f, style)
            }
        }

        WidgetType.DEVICE_INFO -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Kicker("Device Info")
                Text(telemetry.deviceModel, style = textStyle(14.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Android ${telemetry.androidVersion} • API ${telemetry.apiLevel}", style = textStyle(11.5f, style.secondaryTextColor))
                Text("Uptime: ${telemetry.uptimeLabel}", style = textStyle(11f, style.secondaryTextColor))
            }
        }

        WidgetType.INTERNET_STATUS -> {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (telemetry.isOnline) "Online • ${telemetry.transportLabel}" else "Offline",
                    style = textStyle(13.5f)
                )
                Text(
                    text = if (telemetry.isMetered) "Metered" else "Unmetered",
                    style = textStyle(11f, style.accentColor)
                )
            }
        }

        WidgetType.WIFI_INFO -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Kicker("Wi-Fi Info")
                Text(
                    if (telemetry.wifiConnected) "${telemetry.wifiLinkMbps} Mbps Link" else "Disconnected",
                    style = textStyle(16f)
                )
                Text(
                    if (telemetry.wifiConnected) "Signal ${telemetry.wifiSignalPercent}% • ${telemetry.wifiFrequencyMhz} MHz" else "Connect to Wi-Fi",
                    style = textStyle(11f, style.secondaryTextColor)
                )
            }
        }

        WidgetType.NETWORK_SPEED -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Kicker("Network Speed")
                Text("↓ ${telemetry.downBandwidthMbps} Mbps   ↑ ${telemetry.upBandwidthMbps} Mbps", style = textStyle(15f))
                Text(
                    String.format(Locale.US, "Session: %.1f MB RX • %.1f MB TX", telemetry.sessionRxMb, telemetry.sessionTxMb),
                    style = textStyle(11f, style.secondaryTextColor)
                )
            }
        }

        WidgetType.WEATHER, WidgetType.TEMPERATURE -> {
            val tempVal = if (payload.useFahrenheit) payload.currentTempC * 9 / 5 + 32 else payload.currentTempC
            val unit = if (payload.useFahrenheit) "°F" else "°C"
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = style.horizontalAlign
            ) {
                Kicker(payload.cityName)
                Text("${tempVal.toInt()}$unit", style = textStyle(if (config.widgetType == WidgetType.TEMPERATURE) 30f else 26f), modifier = Modifier.fillMaxWidth())
                Text(
                    if (config.widgetType == WidgetType.WEATHER) "${payload.weatherCondition} • ${payload.windKmh.toInt()} km/h" else payload.weatherCondition,
                    style = textStyle(11f, style.secondaryTextColor),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        WidgetType.WEATHER_FORECAST -> {
            val unit = if (payload.useFahrenheit) "°F" else "°C"
            fun fmt(c: Double) = "${(if (payload.useFahrenheit) c * 9 / 5 + 32 else c).toInt()}$unit"
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Kicker("${payload.cityName} • Forecast")
                Text("${fmt(payload.currentTempC)}  ${payload.weatherCondition}", style = textStyle(17f))
                payload.forecast.take(4).forEach { day ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${day.dayLabel} • ${day.condition}", style = textStyle(12f))
                        Text("${fmt(day.highC)} / ${fmt(day.lowC)}", style = textStyle(12f, style.secondaryTextColor))
                    }
                }
            }
        }

        WidgetType.MULTI_TIMEZONE, WidgetType.WORLD_CLOCK -> {
            val fmt = SimpleDateFormat(if (payload.use24Hour) "HH:mm" else "h:mm a", Locale.getDefault())
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Kicker("World Clock")
                payload.timeZones.take(3).forEach { tzId ->
                    fmt.timeZone = TimeZone.getTimeZone(tzId)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tzId.substringAfterLast("/").replace("_", " "), style = textStyle(12.5f))
                        Text(fmt.format(now), style = textStyle(12.5f, style.accentColor))
                    }
                }
            }
        }

        WidgetType.SUNRISE_SUNSET -> {
            val sun = SunCalculator.calculate(payload.latitude, payload.longitude)
            val rise = payload.sunriseIso.ifBlank { sun.sunriseText }
            val set = payload.sunsetIso.ifBlank { sun.sunsetText }
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Kicker("Daylight • ${sun.daylightDuration}")
                Text("☀ $rise   ☾ $set", style = textStyle(16f))
                PreviewProgressBar(sun.daylightProgress, style)
            }
        }

        WidgetType.QUOTES -> {
            val q = QuotesBank.get(payload.quoteIndex)
            val body = payload.customQuote.ifBlank { "“${q.text}”" }
            val auth = if (payload.customQuote.isNotBlank()) "Personal" else "— ${q.author}"
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = style.horizontalAlign
            ) {
                Text(body, style = textStyle(12.5f), maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                Text(auth, style = textStyle(10.5f, style.accentColor), modifier = Modifier.fillMaxWidth())
            }
        }

        WidgetType.PHOTO -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
                if (payload.heading.isNotBlank()) {
                    Text(payload.heading, style = textStyle(12f), modifier = Modifier.fillMaxWidth())
                }
            }
        }

        WidgetType.QUICK_SETTINGS -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ActionPillRow(listOf("Wi-Fi", "Bluetooth", "Display"), style, onInteract)
            }
        }

        WidgetType.FLASHLIGHT -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = style.horizontalAlign
            ) {
                Kicker("Flashlight")
                Text(if (payload.flashlightOn) "ON" else "OFF", style = textStyle(24f), modifier = Modifier.fillMaxWidth())
                Text("Tap to toggle", style = textStyle(10.5f, style.secondaryTextColor), modifier = Modifier.fillMaxWidth())
            }
        }

        WidgetType.CALCULATOR, WidgetType.WEBSITE, WidgetType.APP_SHORTCUT, WidgetType.CUSTOM_ICON_SHORTCUT -> {
            val label = when (config.widgetType) {
                WidgetType.CALCULATOR -> "Calculator"
                WidgetType.WEBSITE -> payload.heading.ifBlank { payload.targetUrl.removePrefix("https://") }
                WidgetType.APP_SHORTCUT, WidgetType.CUSTOM_ICON_SHORTCUT -> payload.targetAppLabel
                else -> config.title
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = style.horizontalAlign
            ) {
                Kicker("Shortcut")
                Text(label, style = textStyle(15f), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                Text("Tap to launch", style = textStyle(10.5f, style.secondaryTextColor), modifier = Modifier.fillMaxWidth())
            }
        }

        WidgetType.QR_CODE -> {
            val qr = remember(payload.qrContent) { QrCodeGenerator.encode(payload.qrContent) }
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(5.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cell = size.width / qr.size.toFloat()
                        for (y in 0 until qr.size) {
                            for (x in 0 until qr.size) {
                                if (qr.get(x, y)) {
                                    drawRect(
                                        color = Color.Black,
                                        topLeft = Offset(x * cell, y * cell),
                                        size = androidx.compose.ui.geometry.Size(cell + 0.5f, cell + 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(payload.heading.ifBlank { "Scan QR" }, style = textStyle(10.5f, style.secondaryTextColor), maxLines = 1)
            }
        }

        WidgetType.RANDOM_NUMBER -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = style.horizontalAlign
            ) {
                Kicker("RNG (${payload.rngMin}–${payload.rngMax})")
                Text("${payload.rngLastValue}", style = textStyle(28f), modifier = Modifier.fillMaxWidth())
                Text("Tap to roll", style = textStyle(10.5f, style.secondaryTextColor), modifier = Modifier.fillMaxWidth())
            }
        }

        WidgetType.DAILY_REMINDER -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = style.horizontalAlign
            ) {
                Kicker(if (payload.reminderDoneToday) "Done Today ✓" else "Reminder • ${payload.reminderTime}")
                Text(payload.reminderTitle, style = textStyle(15f), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                Text("Tap to check off", style = textStyle(10.5f, style.secondaryTextColor), modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun PreviewProgressBar(progress: Float, style: ComposeWidgetStyle) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(CircleShape)
            .background(style.trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0.04f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(style.accentColor)
        )
    }
}

@Composable
private fun ActionPillRow(
    labels: List<String>,
    style: ComposeWidgetStyle,
    onInteract: ((Int) -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        labels.forEachIndexed { idx, label ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(style.trackColor)
                    .clickable(enabled = onInteract != null) { onInteract?.invoke(idx + 1) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = style.textColor,
                    fontFamily = style.fontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}
