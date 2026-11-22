package com.example.data.model

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class TodoItem(
    val id: String,
    val text: String,
    val done: Boolean = false
)

@JsonClass(generateAdapter = true)
data class AgendaItem(
    val time: String,
    val title: String
)

@JsonClass(generateAdapter = true)
data class HabitItem(
    val id: String,
    val name: String,
    val completedToday: Boolean = false,
    val streak: Int = 0
)

@JsonClass(generateAdapter = true)
data class ForecastDay(
    val dayLabel: String,
    val condition: String,
    val code: Int,
    val highC: Double,
    val lowC: Double
)

@JsonClass(generateAdapter = true)
data class WidgetPayload(
    // Clock / Minimalist
    val use24Hour: Boolean = false,
    val showSeconds: Boolean = false,
    val showDate: Boolean = true,
    // Notes / Custom Text / Photo / Color
    val heading: String = "",
    val bodyText: String = "",
    // Todo
    val todos: List<TodoItem> = listOf(
        TodoItem("t1", "Morning focus block", true),
        TodoItem("t2", "Review open PRs", false),
        TodoItem("t3", "Evening walk", false)
    ),
    // Agenda
    val agenda: List<AgendaItem> = listOf(
        AgendaItem("09:00", "Deep Work"),
        AgendaItem("13:00", "Design Review"),
        AgendaItem("17:30", "Workout")
    ),
    // Countdown
    val countdownTitle: String = "Launch Day",
    val countdownTargetEpochMs: Long = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000,
    // Stopwatch
    val stopwatchRunning: Boolean = false,
    val stopwatchAccumulatedMs: Long = 0L,
    val stopwatchStartedAtMs: Long = 0L,
    // Pomodoro
    val pomodoroMinutes: Int = 25,
    val pomodoroBreakMinutes: Int = 5,
    val pomodoroRunning: Boolean = false,
    val pomodoroRemainingMs: Long = 25L * 60 * 1000,
    val pomodoroLastTickMs: Long = 0L,
    val pomodoroIsBreak: Boolean = false,
    // Daily Goals
    val goalTitle: String = "Water Intake",
    val goalCurrent: Int = 5,
    val goalTarget: Int = 8,
    val goalUnit: String = "cups",
    // Habit Tracker
    val habits: List<HabitItem> = listOf(
        HabitItem("h1", "Read 20 pages", true, 6),
        HabitItem("h2", "Meditate 10m", false, 3),
        HabitItem("h3", "Zero sugar", false, 12)
    ),
    // Weather & Sun
    val cityName: String = "San Francisco",
    val latitude: Double = 37.7749,
    val longitude: Double = -122.4194,
    val useFahrenheit: Boolean = false,
    val currentTempC: Double = 18.4,
    val feelsLikeC: Double = 17.8,
    val weatherCondition: String = "Partly Cloudy",
    val weatherCode: Int = 2,
    val windKmh: Double = 14.0,
    val humidityPercent: Int = 62,
    val sunriseIso: String = "06:54",
    val sunsetIso: String = "17:02",
    val weatherUpdatedAtMs: Long = 0L,
    val forecast: List<ForecastDay> = listOf(
        ForecastDay("Today", "Clear", 1, 20.0, 12.0),
        ForecastDay("Tom", "Cloudy", 2, 18.0, 11.0),
        ForecastDay("Day 3", "Rain", 61, 15.0, 10.0),
        ForecastDay("Day 4", "Sunny", 0, 21.0, 13.0)
    ),
    // World Clock / Time Zones
    val timeZones: List<String> = listOf("America/New_York", "Europe/London", "Asia/Tokyo"),
    // Quotes
    val quoteIndex: Int = 0,
    val customQuote: String = "",
    // Shortcuts / QR
    val targetUrl: String = "https://example.com",
    val targetPackage: String = "",
    val targetAppLabel: String = "Choose App",
    val shortcutIconKey: String = "bolt",
    val qrContent: String = "https://github.com",
    // Random Number
    val rngMin: Int = 1,
    val rngMax: Int = 100,
    val rngLastValue: Int = 42,
    // Daily Reminder
    val reminderTitle: String = "Stretch & Hydrate",
    val reminderTime: String = "14:00",
    val reminderDoneToday: Boolean = false,
    val reminderNotifyEnabled: Boolean = false,
    // Flashlight state cache
    val flashlightOn: Boolean = false
)

object WidgetPayloadCodec {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(WidgetPayload::class.java)

    fun encode(payload: WidgetPayload): String = adapter.toJson(payload)

    fun decode(json: String?): WidgetPayload {
        if (json.isNullOrBlank()) return WidgetPayload()
        return try {
            adapter.fromJson(json) ?: WidgetPayload()
        } catch (_: Exception) {
            WidgetPayload()
        }
    }

    fun defaultFor(type: WidgetType): WidgetPayload {
        return when (type) {
            WidgetType.CUSTOM_TEXT -> WidgetPayload(
                heading = "FOCUS",
                bodyText = "Less noise, more clarity."
            )
            WidgetType.NOTES -> WidgetPayload(
                heading = "Quick Note",
                bodyText = "• Finish design specs\n• Call Alex at 4 PM\n• Pick up coffee beans"
            )
            WidgetType.CUSTOM_COLOR -> WidgetPayload(
                heading = "Calm Slate",
                bodyText = "Minimalist Swatch"
            )
            WidgetType.QR_CODE -> WidgetPayload(
                heading = "Scan Link",
                qrContent = "https://widgetlite.org"
            )
            WidgetType.WEBSITE -> WidgetPayload(
                heading = "Open Docs",
                targetUrl = "https://developer.android.com"
            )
            else -> WidgetPayload()
        }
    }
}
