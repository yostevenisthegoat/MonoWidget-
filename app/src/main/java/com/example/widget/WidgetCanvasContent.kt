package com.example.widget

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.text.TextUtils
import com.example.data.model.WidgetAlignment
import com.example.data.model.WidgetConfig
import com.example.data.model.WidgetPayload
import com.example.data.model.WidgetType
import com.example.util.QrCodeGenerator
import com.example.util.QuotesBank
import com.example.util.SunCalculator
import com.example.util.SystemTelemetry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.sin

object WidgetCanvasContent {

    fun drawContent(
        canvas: Canvas,
        bounds: RectF,
        config: WidgetConfig,
        style: ResolvedWidgetStyle,
        telemetry: SystemTelemetry,
        scale: Float
    ) {
        val payload = config.decodedPayload()
        val align = when (style.alignment) {
            WidgetAlignment.START -> Paint.Align.LEFT
            WidgetAlignment.CENTER -> Paint.Align.CENTER
            WidgetAlignment.END -> Paint.Align.RIGHT
        }
        val anchorX = when (style.alignment) {
            WidgetAlignment.START -> bounds.left
            WidgetAlignment.CENTER -> bounds.centerX()
            WidgetAlignment.END -> bounds.right
        }

        fun paint(sizeSp: Float, color: Int = style.textColor, alpha: Int = 255): TextPaint {
            return TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                this.alpha = alpha
                this.typeface = style.typeface
                this.textSize = sizeSp * style.fontScale * scale
                this.textAlign = align
            }
        }

        fun drawEllipsized(text: String, x: Float, y: Float, maxW: Float, p: TextPaint) {
            val clipped = TextUtils.ellipsize(text, p, maxW.coerceAtLeast(24f), TextUtils.TruncateAt.END).toString()
            canvas.drawText(clipped, x, y, p)
        }

        val w = bounds.width()
        val h = bounds.height()
        val now = Date()

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
                val mainSize = if (config.widgetType == WidgetType.MINIMALIST_CLOCK) 30f else 34f
                val mainP = paint(mainSize)
                val subP = paint(12f, style.secondaryTextColor)
                val centerY = bounds.centerY() + (if (payload.showDate) -4f * scale else 10f * scale)
                drawEllipsized(timeStr, anchorX, centerY, w, mainP)
                if (payload.showDate) {
                    drawEllipsized(dateStr, anchorX, centerY + 20f * scale, w, subP)
                }
            }

            WidgetType.ANALOG_CLOCK -> {
                val radius = (minOf(w, h) * 0.44f).coerceAtLeast(24f)
                val cx = bounds.centerX()
                val cy = bounds.centerY()
                val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = style.surfaceTrackColor
                    this.style = Paint.Style.STROKE
                    strokeWidth = 2.5f * scale
                }
                canvas.drawCircle(cx, cy, radius, ringPaint)

                val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = style.secondaryTextColor
                    strokeWidth = 1.6f * scale
                    strokeCap = Paint.Cap.ROUND
                }
                for (i in 0 until 12) {
                    val angle = Math.toRadians((i * 30 - 90).toDouble())
                    val r1 = radius * 0.84f
                    val r2 = radius * 0.94f
                    canvas.drawLine(
                        cx + (cos(angle) * r1).toFloat(),
                        cy + (sin(angle) * r1).toFloat(),
                        cx + (cos(angle) * r2).toFloat(),
                        cy + (sin(angle) * r2).toFloat(),
                        tickPaint
                    )
                }

                val cal = Calendar.getInstance()
                val hr = cal.get(Calendar.HOUR)
                val min = cal.get(Calendar.MINUTE)
                val hrAngle = Math.toRadians(((hr + min / 60f) * 30f - 90f).toDouble())
                val minAngle = Math.toRadians((min * 6f - 90f).toDouble())

                val handPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = style.textColor
                    strokeCap = Paint.Cap.ROUND
                    strokeWidth = 3.6f * scale
                }
                canvas.drawLine(
                    cx, cy,
                    cx + (cos(hrAngle) * radius * 0.50f).toFloat(),
                    cy + (sin(hrAngle) * radius * 0.50f).toFloat(),
                    handPaint
                )
                handPaint.strokeWidth = 2.4f * scale
                canvas.drawLine(
                    cx, cy,
                    cx + (cos(minAngle) * radius * 0.74f).toFloat(),
                    cy + (sin(minAngle) * radius * 0.74f).toFloat(),
                    handPaint
                )
                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = style.accentColor }
                canvas.drawCircle(cx, cy, 4f * scale, dotPaint)
            }

            WidgetType.CALENDAR, WidgetType.THEMED_CALENDAR -> {
                val cal = Calendar.getInstance()
                val monthTitle = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(now)
                val headerP = paint(14f, style.accentColor).apply { textAlign = Paint.Align.LEFT }
                canvas.drawText(monthTitle.uppercase(Locale.getDefault()), bounds.left, bounds.top + 16f * scale, headerP)

                if (config.widgetType == WidgetType.THEMED_CALENDAR && h < w * 0.7f) {
                    val dayNum = SimpleDateFormat("dd", Locale.getDefault()).format(now)
                    val dow = SimpleDateFormat("EEEE", Locale.getDefault()).format(now)
                    drawEllipsized(dayNum, anchorX, bounds.centerY() + 14f * scale, w, paint(34f))
                    drawEllipsized(dow, anchorX, bounds.bottom - 4f * scale, w, paint(12f, style.secondaryTextColor))
                } else {
                    val today = cal.get(Calendar.DAY_OF_MONTH)
                    val tempCal = cal.clone() as Calendar
                    tempCal.set(Calendar.DAY_OF_MONTH, 1)
                    val firstDow = tempCal.get(Calendar.DAY_OF_WEEK) - 1
                    val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val dayHeaders = listOf("S", "M", "T", "W", "T", "F", "S")
                    val cellW = w / 7f
                    val startY = bounds.top + 34f * scale
                    val rowH = ((bounds.bottom - startY) / 7f).coerceAtLeast(14f * scale)

                    val lblP = paint(10f, style.secondaryTextColor).apply { textAlign = Paint.Align.CENTER }
                    for (c in 0 until 7) {
                        canvas.drawText(dayHeaders[c], bounds.left + c * cellW + cellW / 2f, startY, lblP)
                    }
                    val numP = paint(11f, style.textColor).apply { textAlign = Paint.Align.CENTER }
                    val accentCircle = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = style.accentColor }
                    for (day in 1..daysInMonth) {
                        val slot = firstDow + day - 1
                        val row = slot / 7 + 1
                        val col = slot % 7
                        val cx = bounds.left + col * cellW + cellW / 2f
                        val cy = startY + row * rowH
                        if (day == today) {
                            canvas.drawCircle(cx, cy - 3.5f * scale, 9f * scale, accentCircle)
                            numP.color = Color.WHITE
                        } else {
                            numP.color = style.textColor
                        }
                        canvas.drawText(day.toString(), cx, cy, numP)
                    }
                }
            }

            WidgetType.DAILY_AGENDA -> {
                drawEllipsized("TODAY'S AGENDA", bounds.left, bounds.top + 13f * scale, w, paint(10f, style.accentColor).apply { textAlign = Paint.Align.LEFT })
                var y = bounds.top + 34f * scale
                val rowP = paint(12f).apply { textAlign = Paint.Align.LEFT }
                val timeP = paint(11f, style.secondaryTextColor).apply { textAlign = Paint.Align.LEFT }
                payload.agenda.take(3).forEach { item ->
                    if (y < bounds.bottom) {
                        canvas.drawText(item.time, bounds.left, y, timeP)
                        drawEllipsized(item.title, bounds.left + 44f * scale, y, w - 44f * scale, rowP)
                        y += 22f * scale
                    }
                }
            }

            WidgetType.TODO_LIST -> {
                drawEllipsized("TO-DO LIST", bounds.left, bounds.top + 13f * scale, w, paint(10f, style.accentColor).apply { textAlign = Paint.Align.LEFT })
                var y = bounds.top + 34f * scale
                val itemP = paint(12.5f).apply { textAlign = Paint.Align.LEFT }
                val barReserve = 34f * scale
                payload.todos.take(4).forEach { todo ->
                    if (y < bounds.bottom - barReserve) {
                        val boxRect = RectF(bounds.left, y - 10f * scale, bounds.left + 12f * scale, y + 2f * scale)
                        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = if (todo.done) style.accentColor else style.secondaryTextColor
                            this.style = if (todo.done) Paint.Style.FILL else Paint.Style.STROKE
                            strokeWidth = 1.5f * scale
                        }
                        canvas.drawRoundRect(boxRect, 3f * scale, 3f * scale, boxPaint)
                        itemP.alpha = if (todo.done) 135 else 255
                        itemP.isStrikeThruText = todo.done
                        drawEllipsized(todo.text, bounds.left + 20f * scale, y, w - 22f * scale, itemP)
                        y += 22f * scale
                    }
                }
                drawActionPills(canvas, bounds, style, scale, listOf("Toggle #1", "Toggle #2", "Toggle #3"))
            }

            WidgetType.NOTES, WidgetType.CUSTOM_TEXT, WidgetType.CUSTOM_COLOR -> {
                val heading = payload.heading.ifBlank { config.title }
                val body = payload.bodyText.ifBlank {
                    if (config.widgetType == WidgetType.CUSTOM_COLOR) config.bgColorHex else "Tap to edit text in WidgetLite"
                }
                drawEllipsized(heading, anchorX, bounds.top + 16f * scale, w, paint(13f, style.accentColor))
                val lines = body.split("\n").take(5)
                var y = bounds.top + 38f * scale
                val bodyP = paint(13f)
                lines.forEach { line ->
                    if (y < bounds.bottom) {
                        drawEllipsized(line, anchorX, y, w, bodyP)
                        y += 19f * scale
                    }
                }
            }

            WidgetType.COUNTDOWN -> {
                val diffMs = (payload.countdownTargetEpochMs - System.currentTimeMillis()).coerceAtLeast(0L)
                val days = diffMs / (1000L * 60 * 60 * 24)
                val hours = (diffMs / (1000L * 60 * 60)) % 24
                drawEllipsized(payload.countdownTitle.uppercase(), anchorX, bounds.top + 14f * scale, w, paint(10f, style.accentColor))
                drawEllipsized("${days}d", anchorX, bounds.centerY() + 10f * scale, w, paint(30f))
                drawEllipsized("$hours hrs left", anchorX, bounds.bottom - 4f * scale, w, paint(11f, style.secondaryTextColor))
            }

            WidgetType.STOPWATCH -> {
                val elapsed = if (payload.stopwatchRunning && payload.stopwatchStartedAtMs > 0L) {
                    payload.stopwatchAccumulatedMs + (System.currentTimeMillis() - payload.stopwatchStartedAtMs)
                } else payload.stopwatchAccumulatedMs
                val totalSec = (elapsed / 1000L).coerceAtLeast(0L)
                val hrs = totalSec / 3600
                val mins = (totalSec % 3600) / 60
                val secs = totalSec % 60
                val formatted = if (hrs > 0) String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
                else String.format(Locale.US, "%02d:%02d", mins, secs)
                drawEllipsized(if (payload.stopwatchRunning) "STOPWATCH • RUNNING" else "STOPWATCH", anchorX, bounds.top + 13f * scale, w, paint(10f, style.accentColor))
                drawEllipsized(formatted, anchorX, bounds.centerY() + 2f * scale, w, paint(28f))
                drawActionPills(canvas, bounds, style, scale, listOf(if (payload.stopwatchRunning) "Pause" else "Start", "Reset"))
            }

            WidgetType.POMODORO_TIMER -> {
                val remMs = computePomodoroRemaining(payload)
                val totalSec = (remMs / 1000L).coerceAtLeast(0L)
                val mins = totalSec / 60
                val secs = totalSec % 60
                val label = if (payload.pomodoroIsBreak) "BREAK" else "FOCUS"
                val status = if (payload.pomodoroRunning) "$label • ACTIVE" else "$label • PAUSED"
                drawEllipsized(status, anchorX, bounds.top + 13f * scale, w, paint(10f, style.accentColor))
                drawEllipsized(String.format(Locale.US, "%02d:%02d", mins, secs), anchorX, bounds.centerY() + 2f * scale, w, paint(28f))
                drawActionPills(canvas, bounds, style, scale, listOf(if (payload.pomodoroRunning) "Pause" else "Start", "Switch", "Reset"))
            }

            WidgetType.DAILY_GOALS -> {
                val pct = (payload.goalCurrent.toFloat() / payload.goalTarget.coerceAtLeast(1)).coerceIn(0f, 1f)
                drawEllipsized(payload.goalTitle.uppercase(), bounds.left, bounds.top + 13f * scale, w, paint(10f, style.accentColor).apply { textAlign = Paint.Align.LEFT })
                drawEllipsized("${payload.goalCurrent} / ${payload.goalTarget} ${payload.goalUnit}", bounds.left, bounds.top + 36f * scale, w, paint(18f).apply { textAlign = Paint.Align.LEFT })
                drawProgressBar(canvas, bounds.left, bounds.top + 48f * scale, w, 7f * scale, pct, style)
                drawActionPills(canvas, bounds, style, scale, listOf("-1", "+1", "Reset"))
            }

            WidgetType.HABIT_TRACKER -> {
                drawEllipsized("HABIT TRACKER", bounds.left, bounds.top + 13f * scale, w, paint(10f, style.accentColor).apply { textAlign = Paint.Align.LEFT })
                var y = bounds.top + 35f * scale
                val p = paint(12f).apply { textAlign = Paint.Align.LEFT }
                val sP = paint(11f, style.accentColor).apply { textAlign = Paint.Align.RIGHT }
                payload.habits.take(3).forEach { habit ->
                    if (y < bounds.bottom - 34f * scale) {
                        val mark = if (habit.completedToday) "✓ " else "○ "
                        drawEllipsized(mark + habit.name, bounds.left, y, w - 50f * scale, p)
                        canvas.drawText("${habit.streak}d", bounds.right, y, sP)
                        y += 22f * scale
                    }
                }
                drawActionPills(canvas, bounds, style, scale, listOf("Habit 1", "Habit 2", "Habit 3"))
            }

            WidgetType.BATTERY -> {
                drawEllipsized(if (telemetry.isCharging) "CHARGING" else "BATTERY", anchorX, bounds.top + 14f * scale, w, paint(10f, style.accentColor))
                drawEllipsized("${telemetry.batteryPercent}%", anchorX, bounds.centerY() + 10f * scale, w, paint(28f))
                drawProgressBar(canvas, bounds.left, bounds.bottom - 14f * scale, w, 6f * scale, telemetry.batteryPercent / 100f, style)
            }

            WidgetType.STORAGE -> {
                drawEllipsized("STORAGE", bounds.left, bounds.top + 13f * scale, w, paint(10f, style.accentColor).apply { textAlign = Paint.Align.LEFT })
                drawEllipsized(
                    String.format(Locale.US, "%.1f / %.0f GB", telemetry.storageUsedGb, telemetry.storageTotalGb),
                    bounds.left, bounds.top + 38f * scale, w, paint(18f).apply { textAlign = Paint.Align.LEFT }
                )
                drawEllipsized("${telemetry.storageUsedPercent}% used", bounds.left, bounds.top + 56f * scale, w, paint(11f, style.secondaryTextColor).apply { textAlign = Paint.Align.LEFT })
                drawProgressBar(canvas, bounds.left, bounds.bottom - 12f * scale, w, 7f * scale, telemetry.storageUsedPercent / 100f, style)
            }

            WidgetType.RAM -> {
                drawEllipsized("SYSTEM RAM", bounds.left, bounds.top + 13f * scale, w, paint(10f, style.accentColor).apply { textAlign = Paint.Align.LEFT })
                drawEllipsized(
                    "${telemetry.ramUsedMb} / ${telemetry.ramTotalMb} MB",
                    bounds.left, bounds.top + 38f * scale, w, paint(17f).apply { textAlign = Paint.Align.LEFT }
                )
                drawEllipsized("${telemetry.ramUsedPercent}% active", bounds.left, bounds.top + 56f * scale, w, paint(11f, style.secondaryTextColor).apply { textAlign = Paint.Align.LEFT })
                drawProgressBar(canvas, bounds.left, bounds.bottom - 12f * scale, w, 7f * scale, telemetry.ramUsedPercent / 100f, style)
            }

            WidgetType.DEVICE_INFO -> {
                drawEllipsized("DEVICE", bounds.left, bounds.top + 13f * scale, w, paint(10f, style.accentColor).apply { textAlign = Paint.Align.LEFT })
                drawEllipsized(telemetry.deviceModel, bounds.left, bounds.top + 34f * scale, w, paint(15f).apply { textAlign = Paint.Align.LEFT })
                drawEllipsized("Android ${telemetry.androidVersion} (API ${telemetry.apiLevel})", bounds.left, bounds.top + 54f * scale, w, paint(11.5f, style.secondaryTextColor).apply { textAlign = Paint.Align.LEFT })
                drawEllipsized("Uptime: ${telemetry.uptimeLabel}", bounds.left, bounds.bottom - 4f * scale, w, paint(11f, style.secondaryTextColor).apply { textAlign = Paint.Align.LEFT })
            }

            WidgetType.INTERNET_STATUS -> {
                val statusText = if (telemetry.isOnline) "Online • ${telemetry.transportLabel}" else "Offline"
                val sub = if (telemetry.isMetered) "Metered" else "Unmetered"
                drawEllipsized(statusText, bounds.left, bounds.centerY() + 4f * scale, w * 0.7f, paint(14f).apply { textAlign = Paint.Align.LEFT })
                drawEllipsized(sub, bounds.right, bounds.centerY() + 4f * scale, w * 0.35f, paint(11f, style.accentColor).apply { textAlign = Paint.Align.RIGHT })
            }

            WidgetType.WIFI_INFO -> {
                drawEllipsized("WI-FI", bounds.left, bounds.top + 13f * scale, w, paint(10f, style.accentColor).apply { textAlign = Paint.Align.LEFT })
                val headline = if (telemetry.wifiConnected) "${telemetry.wifiLinkMbps} Mbps Link" else "Wi-Fi Disconnected"
                drawEllipsized(headline, bounds.left, bounds.top + 38f * scale, w, paint(17f).apply { textAlign = Paint.Align.LEFT })
                val sub = if (telemetry.wifiConnected) "Signal ${telemetry.wifiSignalPercent}% • ${telemetry.wifiFrequencyMhz} MHz" else "Connect to view link details"
                drawEllipsized(sub, bounds.left, bounds.bottom - 6f * scale, w, paint(11f, style.secondaryTextColor).apply { textAlign = Paint.Align.LEFT })
            }

            WidgetType.NETWORK_SPEED -> {
                drawEllipsized("NETWORK BANDWIDTH", bounds.left, bounds.top + 13f * scale, w, paint(10f, style.accentColor).apply { textAlign = Paint.Align.LEFT })
                drawEllipsized("↓ ${telemetry.downBandwidthMbps} Mbps  ↑ ${telemetry.upBandwidthMbps} Mbps", bounds.left, bounds.top + 38f * scale, w, paint(16f).apply { textAlign = Paint.Align.LEFT })
                drawEllipsized(
                    String.format(Locale.US, "Session: %.1f MB RX • %.1f MB TX", telemetry.sessionRxMb, telemetry.sessionTxMb),
                    bounds.left, bounds.bottom - 6f * scale, w, paint(11f, style.secondaryTextColor).apply { textAlign = Paint.Align.LEFT }
                )
            }

            WidgetType.WEATHER, WidgetType.TEMPERATURE -> {
                val temp = formatTemp(payload.currentTempC, payload.useFahrenheit)
                drawEllipsized(payload.cityName.uppercase(), anchorX, bounds.top + 13f * scale, w, paint(10f, style.accentColor))
                drawEllipsized(temp, anchorX, bounds.centerY() + 8f * scale, w, paint(if (config.widgetType == WidgetType.TEMPERATURE) 32f else 28f))
                if (config.widgetType == WidgetType.WEATHER) {
                    drawEllipsized("${payload.weatherCondition} • Wind ${payload.windKmh.toInt()} km/h", anchorX, bounds.bottom - 4f * scale, w, paint(11f, style.secondaryTextColor))
                } else {
                    drawEllipsized(payload.weatherCondition, anchorX, bounds.bottom - 4f * scale, w, paint(11f, style.secondaryTextColor))
                }
            }

            WidgetType.WEATHER_FORECAST -> {
                drawEllipsized("${payload.cityName} • FORECAST", bounds.left, bounds.top + 13f * scale, w, paint(10f, style.accentColor).apply { textAlign = Paint.Align.LEFT })
                drawEllipsized(
                    "${formatTemp(payload.currentTempC, payload.useFahrenheit)}  ${payload.weatherCondition}",
                    bounds.left, bounds.top + 38f * scale, w, paint(18f).apply { textAlign = Paint.Align.LEFT }
                )
                var y = bounds.top + 64f * scale
                val rowP = paint(12f).apply { textAlign = Paint.Align.LEFT }
                val hiLoP = paint(12f, style.secondaryTextColor).apply { textAlign = Paint.Align.RIGHT }
                payload.forecast.take(4).forEach { day ->
                    if (y < bounds.bottom) {
                        canvas.drawText("${day.dayLabel} • ${day.condition}", bounds.left, y, rowP)
                        canvas.drawText(
                            "${formatTemp(day.highC, payload.useFahrenheit)} / ${formatTemp(day.lowC, payload.useFahrenheit)}",
                            bounds.right, y, hiLoP
                        )
                        y += 22f * scale
                    }
                }
            }

            WidgetType.MULTI_TIMEZONE, WidgetType.WORLD_CLOCK -> {
                drawEllipsized("WORLD CLOCK", bounds.left, bounds.top + 13f * scale, w, paint(10f, style.accentColor).apply { textAlign = Paint.Align.LEFT })
                var y = bounds.top + 36f * scale
                val cityP = paint(12.5f).apply { textAlign = Paint.Align.LEFT }
                val timeP = paint(13f, style.accentColor).apply { textAlign = Paint.Align.RIGHT }
                val sdf = SimpleDateFormat(if (payload.use24Hour) "HH:mm" else "h:mm a", Locale.getDefault())
                payload.timeZones.take(3).forEach { tzId ->
                    if (y < bounds.bottom) {
                        val tz = TimeZone.getTimeZone(tzId)
                        sdf.timeZone = tz
                        val city = tzId.substringAfterLast("/").replace("_", " ")
                        drawEllipsized(city, bounds.left, y, w * 0.55f, cityP)
                        canvas.drawText(sdf.format(now), bounds.right, y, timeP)
                        y += 24f * scale
                    }
                }
            }

            WidgetType.SUNRISE_SUNSET -> {
                val sun = SunCalculator.calculate(payload.latitude, payload.longitude)
                val rise = payload.sunriseIso.ifBlank { sun.sunriseText }
                val set = payload.sunsetIso.ifBlank { sun.sunsetText }
                drawEllipsized("DAYLIGHT • ${sun.daylightDuration}", bounds.left, bounds.top + 13f * scale, w, paint(10f, style.accentColor).apply { textAlign = Paint.Align.LEFT })
                drawEllipsized("☀ $rise   ☾ $set", bounds.left, bounds.centerY() + 4f * scale, w, paint(16f).apply { textAlign = Paint.Align.LEFT })
                drawProgressBar(canvas, bounds.left, bounds.bottom - 12f * scale, w, 6f * scale, sun.daylightProgress, style)
            }

            WidgetType.QUOTES -> {
                val q = QuotesBank.get(payload.quoteIndex)
                val text = payload.customQuote.ifBlank { "“${q.text}”" }
                val author = if (payload.customQuote.isNotBlank()) "Personal Quote" else "— ${q.author}"
                drawEllipsized(text, anchorX, bounds.centerY() - 2f * scale, w, paint(13f))
                drawEllipsized(author, anchorX, bounds.bottom - 6f * scale, w, paint(10.5f, style.accentColor))
            }

            WidgetType.PHOTO -> {
                if (payload.heading.isNotBlank()) {
                    drawEllipsized(payload.heading, anchorX, bounds.bottom - 6f * scale, w, paint(12f))
                }
            }

            WidgetType.QUICK_SETTINGS -> {
                drawActionPills(canvas, bounds, style, scale, listOf("Wi-Fi", "Bluetooth", "Display"))
            }

            WidgetType.FLASHLIGHT -> {
                val stateLabel = if (payload.flashlightOn) "ON" else "OFF"
                drawEllipsized("FLASHLIGHT", anchorX, bounds.top + 16f * scale, w, paint(10f, style.accentColor))
                drawEllipsized(stateLabel, anchorX, bounds.centerY() + 12f * scale, w, paint(26f))
                drawEllipsized("Tap to toggle", anchorX, bounds.bottom - 4f * scale, w, paint(10.5f, style.secondaryTextColor))
            }

            WidgetType.CALCULATOR, WidgetType.WEBSITE, WidgetType.APP_SHORTCUT, WidgetType.CUSTOM_ICON_SHORTCUT -> {
                val label = when (config.widgetType) {
                    WidgetType.CALCULATOR -> "Calculator"
                    WidgetType.WEBSITE -> payload.heading.ifBlank { payload.targetUrl.removePrefix("https://") }
                    WidgetType.APP_SHORTCUT, WidgetType.CUSTOM_ICON_SHORTCUT -> payload.targetAppLabel
                    else -> config.title
                }
                drawEllipsized("SHORTCUT", anchorX, bounds.top + 16f * scale, w, paint(10f, style.accentColor))
                drawEllipsized(label, anchorX, bounds.centerY() + 8f * scale, w, paint(16f))
                drawEllipsized("Tap to open", anchorX, bounds.bottom - 4f * scale, w, paint(10.5f, style.secondaryTextColor))
            }

            WidgetType.QR_CODE -> {
                val qr = QrCodeGenerator.encode(payload.qrContent)
                val qrSize = (minOf(w, h - 18f * scale) * 0.85f).coerceAtLeast(36f)
                val left = bounds.centerX() - qrSize / 2f
                val top = bounds.top + 2f * scale
                val bgRect = RectF(left - 4f * scale, top - 4f * scale, left + qrSize + 4f * scale, top + qrSize + 4f * scale)
                val whiteP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
                canvas.drawRoundRect(bgRect, 6f * scale, 6f * scale, whiteP)
                val darkP = Paint().apply { color = Color.BLACK }
                val cell = qrSize / qr.size.toFloat()
                for (y in 0 until qr.size) {
                    for (x in 0 until qr.size) {
                        if (qr.get(x, y)) {
                            canvas.drawRect(
                                left + x * cell,
                                top + y * cell,
                                left + (x + 1) * cell + 0.4f,
                                top + (y + 1) * cell + 0.4f,
                                darkP
                            )
                        }
                    }
                }
                drawEllipsized(payload.heading.ifBlank { "Scan QR" }, anchorX, bounds.bottom - 2f * scale, w, paint(10.5f, style.secondaryTextColor))
            }

            WidgetType.RANDOM_NUMBER -> {
                drawEllipsized("RNG (${payload.rngMin}–${payload.rngMax})", anchorX, bounds.top + 14f * scale, w, paint(10f, style.accentColor))
                drawEllipsized(payload.rngLastValue.toString(), anchorX, bounds.centerY() + 12f * scale, w, paint(30f))
                drawEllipsized("Tap to roll", anchorX, bounds.bottom - 4f * scale, w, paint(10.5f, style.secondaryTextColor))
            }

            WidgetType.DAILY_REMINDER -> {
                val badge = if (payload.reminderDoneToday) "DONE TODAY ✓" else "REMINDER • ${payload.reminderTime}"
                drawEllipsized(badge, anchorX, bounds.top + 14f * scale, w, paint(10f, style.accentColor))
                drawEllipsized(payload.reminderTitle, anchorX, bounds.centerY() + 6f * scale, w, paint(16f))
                drawEllipsized("Tap to toggle check-off", anchorX, bounds.bottom - 4f * scale, w, paint(10.5f, style.secondaryTextColor))
            }
        }
    }

    fun computePomodoroRemaining(payload: WidgetPayload): Long {
        if (!payload.pomodoroRunning || payload.pomodoroLastTickMs <= 0L) {
            return payload.pomodoroRemainingMs.coerceAtLeast(0L)
        }
        val elapsed = (System.currentTimeMillis() - payload.pomodoroLastTickMs).coerceAtLeast(0L)
        return (payload.pomodoroRemainingMs - elapsed).coerceAtLeast(0L)
    }

    private fun formatTemp(tempC: Double, useF: Boolean): String {
        val v = if (useF) tempC * 9.0 / 5.0 + 32.0 else tempC
        val unit = if (useF) "°F" else "°C"
        return "${v.toInt()}$unit"
    }

    private fun drawProgressBar(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        progress: Float,
        style: ResolvedWidgetStyle
    ) {
        val r = height / 2f
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = style.surfaceTrackColor }
        canvas.drawRoundRect(RectF(left, top, left + width, top + height), r, r, trackPaint)
        val fillW = (width * progress.coerceIn(0f, 1f)).coerceAtLeast(height)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = style.accentColor }
        canvas.drawRoundRect(RectF(left, top, left + fillW, top + height), r, r, fillPaint)
    }

    private fun drawActionPills(
        canvas: Canvas,
        bounds: RectF,
        style: ResolvedWidgetStyle,
        scale: Float,
        labels: List<String>
    ) {
        if (labels.isEmpty()) return
        val pillH = 24f * scale
        val top = bounds.bottom - pillH
        val gap = 6f * scale
        val pillW = (bounds.width() - gap * (labels.size - 1)) / labels.size
        val bgP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = style.surfaceTrackColor }
        val txtP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.textColor
            typeface = style.typeface
            textSize = 10.5f * scale
            textAlign = Paint.Align.CENTER
        }
        labels.forEachIndexed { idx, label ->
            val l = bounds.left + idx * (pillW + gap)
            val rect = RectF(l, top, l + pillW, bounds.bottom)
            canvas.drawRoundRect(rect, 8f * scale, 8f * scale, bgP)
            canvas.drawText(label, rect.centerX(), rect.centerY() + 3.5f * scale, txtP)
        }
    }
}
