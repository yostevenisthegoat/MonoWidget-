package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import com.example.WidgetLiteApplication
import com.example.data.model.WidgetPayloadCodec
import com.example.data.model.WidgetType
import com.example.util.QuotesBank
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

open class BaseWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (id in appWidgetIds) {
                    WidgetUpdateHelper.updateAppWidget(context, appWidgetManager, id)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WidgetUpdateHelper.updateAppWidget(context, appWidgetManager, appWidgetId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        val app = context.applicationContext as WidgetLiteApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.repository.unbindAppWidgetIds(appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class WidgetSmallProvider : BaseWidgetProvider()
class WidgetMediumProvider : BaseWidgetProvider()
class WidgetLargeProvider : BaseWidgetProvider()
class WidgetStripProvider : BaseWidgetProvider()

class WidgetPinReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val pendingResult = goAsync()
        val app = context.applicationContext as WidgetLiteApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var configId = intent.getLongExtra(WidgetUpdateHelper.EXTRA_CONFIG_ID, -1L)
                if (configId <= 0L) {
                    configId = app.repository.userSettings.first().pendingPinConfigId
                }
                if (configId > 0L) {
                    app.repository.bindAppWidget(configId, appWidgetId)
                    app.repository.setPendingPinConfigId(-1L)
                }
                WidgetUpdateHelper.updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WidgetUpdateHelper.ACTION_WIDGET_INTERACT) return
        val appWidgetId = intent.getIntExtra(
            WidgetUpdateHelper.EXTRA_APP_WIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        val slot = intent.getIntExtra(WidgetUpdateHelper.EXTRA_ACTION_SLOT, 0)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val pendingResult = goAsync()
        val app = context.applicationContext as WidgetLiteApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = app.repository
                val config = repo.getWidgetByAppWidgetId(appWidgetId) ?: return@launch
                val p = config.decodedPayload()
                val now = System.currentTimeMillis()

                val updatedPayload = when (config.widgetType) {
                    WidgetType.TODO_LIST -> {
                        val idx = (slot - 1).coerceIn(0, p.todos.lastIndex.coerceAtLeast(0))
                        val newTodos = p.todos.mapIndexed { i, item ->
                            if (i == idx) item.copy(done = !item.done) else item
                        }
                        p.copy(todos = newTodos)
                    }
                    WidgetType.HABIT_TRACKER -> {
                        val idx = (slot - 1).coerceIn(0, p.habits.lastIndex.coerceAtLeast(0))
                        val newHabits = p.habits.mapIndexed { i, h ->
                            if (i == idx) {
                                val nextDone = !h.completedToday
                                val nextStreak = if (nextDone) h.streak + 1 else (h.streak - 1).coerceAtLeast(0)
                                h.copy(completedToday = nextDone, streak = nextStreak)
                            } else h
                        }
                        p.copy(habits = newHabits)
                    }
                    WidgetType.DAILY_GOALS -> when (slot) {
                        1 -> p.copy(goalCurrent = (p.goalCurrent - 1).coerceAtLeast(0))
                        2 -> p.copy(goalCurrent = p.goalCurrent + 1)
                        else -> p.copy(goalCurrent = 0)
                    }
                    WidgetType.STOPWATCH -> when (slot) {
                        1 -> {
                            if (p.stopwatchRunning) {
                                val acc = p.stopwatchAccumulatedMs + (now - p.stopwatchStartedAtMs).coerceAtLeast(0L)
                                p.copy(stopwatchRunning = false, stopwatchAccumulatedMs = acc, stopwatchStartedAtMs = 0L)
                            } else {
                                p.copy(stopwatchRunning = true, stopwatchStartedAtMs = now)
                            }
                        }
                        else -> p.copy(stopwatchRunning = false, stopwatchAccumulatedMs = 0L, stopwatchStartedAtMs = 0L)
                    }
                    WidgetType.POMODORO_TIMER -> when (slot) {
                        1 -> {
                            if (p.pomodoroRunning) {
                                val rem = WidgetCanvasContent.computePomodoroRemaining(p)
                                p.copy(pomodoroRunning = false, pomodoroRemainingMs = rem, pomodoroLastTickMs = 0L)
                            } else {
                                val base = if (p.pomodoroRemainingMs <= 0L) p.pomodoroMinutes * 60_000L else p.pomodoroRemainingMs
                                p.copy(pomodoroRunning = true, pomodoroRemainingMs = base, pomodoroLastTickMs = now)
                            }
                        }
                        2 -> {
                            val nextBreak = !p.pomodoroIsBreak
                            val mins = if (nextBreak) p.pomodoroBreakMinutes else p.pomodoroMinutes
                            p.copy(
                                pomodoroIsBreak = nextBreak,
                                pomodoroRunning = false,
                                pomodoroRemainingMs = mins * 60_000L,
                                pomodoroLastTickMs = 0L
                            )
                        }
                        else -> {
                            val mins = if (p.pomodoroIsBreak) p.pomodoroBreakMinutes else p.pomodoroMinutes
                            p.copy(pomodoroRunning = false, pomodoroRemainingMs = mins * 60_000L, pomodoroLastTickMs = 0L)
                        }
                    }
                    WidgetType.RANDOM_NUMBER -> {
                        val low = minOf(p.rngMin, p.rngMax)
                        val high = maxOf(p.rngMin, p.rngMax)
                        val nextVal = if (low == high) low else Random.nextInt(low, high + 1)
                        p.copy(rngLastValue = nextVal)
                    }
                    WidgetType.QUOTES -> {
                        p.copy(quoteIndex = (p.quoteIndex + 1) % QuotesBank.quotes.size)
                    }
                    WidgetType.DAILY_REMINDER -> {
                        p.copy(reminderDoneToday = !p.reminderDoneToday)
                    }
                    WidgetType.FLASHLIGHT -> {
                        val nextState = toggleFlashlight(context, !p.flashlightOn)
                        p.copy(flashlightOn = nextState)
                    }
                    WidgetType.WEATHER, WidgetType.TEMPERATURE, WidgetType.WEATHER_FORECAST -> {
                        val allowNet = repo.userSettings.first().allowWeatherNetwork
                        if (allowNet) {
                            repo.fetchLiveWeather(p).getOrElse { p }
                        } else p
                    }
                    else -> p
                }

                val updatedConfig = config.copy(payloadJson = WidgetPayloadCodec.encode(updatedPayload))
                repo.saveWidget(updatedConfig)
                WidgetUpdateHelper.updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun toggleFlashlight(context: Context, targetOn: Boolean): Boolean {
        return try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return false
            val cameraId = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return false
            cm.setTorchMode(cameraId, targetOn)
            targetOn
        } catch (_: Exception) {
            false
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WidgetUpdateHelper.updateAllWidgets(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
