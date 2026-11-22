package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.WidgetLiteApplication
import com.example.data.model.WidgetConfig
import com.example.data.model.WidgetSizePreset
import com.example.data.model.WidgetType

enum class PinResult {
    REQUEST_SENT,
    LAUNCHER_UNSUPPORTED,
    REQUIRES_MANUAL_ADD
}

object WidgetUpdateHelper {

    const val EXTRA_CONFIG_ID = "extra_config_id"
    const val EXTRA_APP_WIDGET_ID = "extra_app_widget_id"
    const val EXTRA_ACTION_SLOT = "extra_action_slot"
    const val ACTION_PINNED_CALLBACK = "com.example.widget.ACTION_PINNED_CALLBACK"
    const val ACTION_WIDGET_INTERACT = "com.example.widget.ACTION_WIDGET_INTERACT"

    suspend fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val app = context.applicationContext as WidgetLiteApplication
        val repo = app.repository
        val config = repo.getWidgetByAppWidgetId(appWidgetId) ?: WidgetConfig.createDefault(WidgetType.DIGITAL_CLOCK)
        val telemetry = repo.collectSystemTelemetry()

        val options: Bundle = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        val (fallbackW, fallbackH) = defaultDpForSize(config.size)
        val widthDp = if (minW > 40) minW else fallbackW
        val heightDp = if (minH > 40) minH else fallbackH

        val bitmap = WidgetCanvasRenderer.render(context, config, telemetry, widthDp, heightDp)
        val views = RemoteViews(context.packageName, R.layout.widget_host_layout)
        views.setImageViewBitmap(R.id.widget_canvas_image, bitmap)

        // Root tap behavior
        val rootIntent = buildRootPendingIntent(context, config, appWidgetId)
        views.setOnClickPendingIntent(R.id.widget_root, rootIntent)

        // Interactive bottom action bar hotspots
        configureActionBar(context, views, config, appWidgetId)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    suspend fun updateAllWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val providers = listOf(
            WidgetSmallProvider::class.java,
            WidgetMediumProvider::class.java,
            WidgetLargeProvider::class.java,
            WidgetStripProvider::class.java
        )
        for (cls in providers) {
            val ids = manager.getAppWidgetIds(ComponentName(context, cls))
            for (id in ids) {
                updateAppWidget(context, manager, id)
            }
        }
    }

    suspend fun requestPinWidget(context: Context, config: WidgetConfig): PinResult {
        val app = context.applicationContext as WidgetLiteApplication
        val savedId = app.repository.saveWidget(config)
        app.repository.setPendingPinConfigId(savedId)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return PinResult.REQUIRES_MANUAL_ADD
        }
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) {
            return PinResult.LAUNCHER_UNSUPPORTED
        }

        val providerClass = when (config.size) {
            WidgetSizePreset.SMALL_2X2 -> WidgetSmallProvider::class.java
            WidgetSizePreset.MEDIUM_4X2 -> WidgetMediumProvider::class.java
            WidgetSizePreset.LARGE_4X4 -> WidgetLargeProvider::class.java
            WidgetSizePreset.STRIP_4X1 -> WidgetStripProvider::class.java
        }

        val callbackIntent = Intent(context, WidgetPinReceiver::class.java).apply {
            action = ACTION_PINNED_CALLBACK
            putExtra(EXTRA_CONFIG_ID, savedId)
        }
        val successCallback = PendingIntent.getBroadcast(
            context,
            (savedId and 0x7FFFFFFF).toInt(),
            callbackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val launched = manager.requestPinAppWidget(ComponentName(context, providerClass), null, successCallback)
        return if (launched) PinResult.REQUEST_SENT else PinResult.LAUNCHER_UNSUPPORTED
    }

    private fun buildRootPendingIntent(
        context: Context,
        config: WidgetConfig,
        appWidgetId: Int
    ): PendingIntent {
        val payload = config.decodedPayload()
        val reqCode = appWidgetId * 10 + 9
        return when (config.widgetType) {
            WidgetType.FLASHLIGHT,
            WidgetType.RANDOM_NUMBER,
            WidgetType.QUOTES,
            WidgetType.DAILY_REMINDER,
            WidgetType.WEATHER,
            WidgetType.TEMPERATURE,
            WidgetType.WEATHER_FORECAST -> {
                val intent = Intent(context, WidgetActionReceiver::class.java).apply {
                    action = ACTION_WIDGET_INTERACT
                    putExtra(EXTRA_APP_WIDGET_ID, appWidgetId)
                    putExtra(EXTRA_CONFIG_ID, config.id)
                    putExtra(EXTRA_ACTION_SLOT, 0)
                }
                PendingIntent.getBroadcast(
                    context,
                    reqCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
            WidgetType.WEBSITE -> {
                val url = payload.targetUrl.let {
                    if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it"
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                PendingIntent.getActivity(
                    context,
                    reqCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
            WidgetType.CALCULATOR -> {
                val calcIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_CALCULATOR)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val resolved = calcIntent.resolveActivity(context.packageManager)
                val target = if (resolved != null) calcIntent else Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                PendingIntent.getActivity(
                    context,
                    reqCode,
                    target,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
            WidgetType.APP_SHORTCUT, WidgetType.CUSTOM_ICON_SHORTCUT -> {
                val launch = if (payload.targetPackage.isNotBlank()) {
                    context.packageManager.getLaunchIntentForPackage(payload.targetPackage)
                } else null
                val target = launch?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    ?: Intent(context, MainActivity::class.java).apply {
                        putExtra(EXTRA_CONFIG_ID, config.id)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                PendingIntent.getActivity(
                    context,
                    reqCode,
                    target,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
            else -> {
                val openApp = Intent(context, MainActivity::class.java).apply {
                    putExtra(EXTRA_CONFIG_ID, config.id)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                PendingIntent.getActivity(
                    context,
                    reqCode,
                    openApp,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
        }
    }

    private fun configureActionBar(
        context: Context,
        views: RemoteViews,
        config: WidgetConfig,
        appWidgetId: Int
    ) {
        val type = config.widgetType
        when (type) {
            WidgetType.QUICK_SETTINGS -> {
                views.setViewVisibility(R.id.widget_action_bar, View.VISIBLE)
                views.setViewVisibility(R.id.widget_action_3, View.VISIBLE)
                views.setOnClickPendingIntent(
                    R.id.widget_action_1,
                    settingsActivityPendingIntent(context, Settings.ACTION_WIFI_SETTINGS, appWidgetId * 10 + 1)
                )
                views.setOnClickPendingIntent(
                    R.id.widget_action_2,
                    settingsActivityPendingIntent(context, Settings.ACTION_BLUETOOTH_SETTINGS, appWidgetId * 10 + 2)
                )
                views.setOnClickPendingIntent(
                    R.id.widget_action_3,
                    settingsActivityPendingIntent(context, Settings.ACTION_DISPLAY_SETTINGS, appWidgetId * 10 + 3)
                )
            }
            WidgetType.STOPWATCH -> {
                views.setViewVisibility(R.id.widget_action_bar, View.VISIBLE)
                views.setViewVisibility(R.id.widget_action_3, View.GONE)
                views.setOnClickPendingIntent(R.id.widget_action_1, broadcastAction(context, config.id, appWidgetId, 1))
                views.setOnClickPendingIntent(R.id.widget_action_2, broadcastAction(context, config.id, appWidgetId, 2))
            }
            WidgetType.POMODORO_TIMER,
            WidgetType.DAILY_GOALS,
            WidgetType.TODO_LIST,
            WidgetType.HABIT_TRACKER -> {
                views.setViewVisibility(R.id.widget_action_bar, View.VISIBLE)
                views.setViewVisibility(R.id.widget_action_3, View.VISIBLE)
                views.setOnClickPendingIntent(R.id.widget_action_1, broadcastAction(context, config.id, appWidgetId, 1))
                views.setOnClickPendingIntent(R.id.widget_action_2, broadcastAction(context, config.id, appWidgetId, 2))
                views.setOnClickPendingIntent(R.id.widget_action_3, broadcastAction(context, config.id, appWidgetId, 3))
            }
            else -> {
                views.setViewVisibility(R.id.widget_action_bar, View.GONE)
            }
        }
    }

    private fun settingsActivityPendingIntent(context: Context, action: String, reqCode: Int): PendingIntent {
        val intent = Intent(action).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        return PendingIntent.getActivity(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun broadcastAction(
        context: Context,
        configId: Long,
        appWidgetId: Int,
        slot: Int
    ): PendingIntent {
        val intent = Intent(context, WidgetActionReceiver::class.java).apply {
            action = ACTION_WIDGET_INTERACT
            putExtra(EXTRA_CONFIG_ID, configId)
            putExtra(EXTRA_APP_WIDGET_ID, appWidgetId)
            putExtra(EXTRA_ACTION_SLOT, slot)
        }
        return PendingIntent.getBroadcast(
            context,
            appWidgetId * 10 + slot,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun defaultDpForSize(size: WidgetSizePreset): Pair<Int, Int> = when (size) {
        WidgetSizePreset.SMALL_2X2 -> 170 to 170
        WidgetSizePreset.MEDIUM_4X2 -> 320 to 160
        WidgetSizePreset.LARGE_4X4 -> 320 to 320
        WidgetSizePreset.STRIP_4X1 -> 320 to 84
    }
}
