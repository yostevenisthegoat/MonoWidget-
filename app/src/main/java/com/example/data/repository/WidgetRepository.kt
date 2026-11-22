package com.example.data.repository

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.data.local.AppAccentColor
import com.example.data.local.AppPreferences
import com.example.data.local.AppThemeMode
import com.example.data.local.UserSettings
import com.example.data.local.WidgetDao
import com.example.data.model.ForecastDay
import com.example.data.model.WidgetConfig
import com.example.data.model.WidgetPayload
import com.example.data.model.WidgetPayloadCodec
import com.example.data.model.WidgetType
import com.example.data.remote.GeocodingResultDto
import com.example.data.remote.WeatherNetworkClient
import com.example.util.InstalledAppEntry
import com.example.util.SunCalculator
import com.example.util.SystemInfoCollector
import com.example.util.SystemTelemetry
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@JsonClass(generateAdapter = true)
data class WidgetExportBundle(
    val app: String = "WidgetLite",
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val widgets: List<WidgetConfig>
)

class WidgetRepository(
    private val context: Context,
    private val widgetDao: WidgetDao,
    private val preferences: AppPreferences
) {
    val allWidgets: Flow<List<WidgetConfig>> = widgetDao.getAllWidgets()
    val pinnedWidgets: Flow<List<WidgetConfig>> = widgetDao.getPinnedWidgets()
    val userSettings: Flow<UserSettings> = preferences.settings

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val exportAdapter = moshi.adapter(WidgetExportBundle::class.java).indent("  ")

    suspend fun ensureSeededDefaults() = withContext(Dispatchers.IO) {
        if (widgetDao.count() == 0) {
            val isSystemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            val starters = listOf(
                WidgetConfig.createDefault(WidgetType.DIGITAL_CLOCK, isSystemDark),
                WidgetConfig.createDefault(WidgetType.WEATHER, isSystemDark),
                WidgetConfig.createDefault(WidgetType.TODO_LIST, isSystemDark),
                WidgetConfig.createDefault(WidgetType.POMODORO_TIMER, isSystemDark),
                WidgetConfig.createDefault(WidgetType.PHOTO, isSystemDark),
                WidgetConfig.createDefault(WidgetType.QR_CODE, isSystemDark)
            )
            widgetDao.insertWidgets(starters)
        }
    }

    suspend fun getWidgetById(id: Long): WidgetConfig? = withContext(Dispatchers.IO) {
        widgetDao.getWidgetById(id)
    }

    suspend fun getWidgetByAppWidgetId(appWidgetId: Int): WidgetConfig? = withContext(Dispatchers.IO) {
        widgetDao.getWidgetByAppWidgetId(appWidgetId)
    }

    suspend fun getBoundAppWidgets(): List<WidgetConfig> = withContext(Dispatchers.IO) {
        widgetDao.getBoundAppWidgets()
    }

    suspend fun saveWidget(config: WidgetConfig): Long = withContext(Dispatchers.IO) {
        val updated = config.copy(lastUsedAt = System.currentTimeMillis())
        val savedId = if (updated.id == 0L) {
            widgetDao.insertWidget(updated)
        } else {
            widgetDao.updateWidget(updated)
            updated.id
        }
        preferences.recordRecentType(updated.typeId)
        savedId
    }

    suspend fun deleteWidget(id: Long) = withContext(Dispatchers.IO) {
        widgetDao.deleteWidgetById(id)
    }

    suspend fun bindAppWidget(configId: Long, appWidgetId: Int) = withContext(Dispatchers.IO) {
        widgetDao.clearAppWidgetBinding(appWidgetId)
        val existing = widgetDao.getWidgetById(configId)
        if (existing != null) {
            if (existing.appWidgetId == -1 || existing.appWidgetId == appWidgetId) {
                widgetDao.updateWidget(
                    existing.copy(
                        appWidgetId = appWidgetId,
                        isPinned = true,
                        lastUsedAt = System.currentTimeMillis()
                    )
                )
            } else {
                // Clone so multiple home screen instances of a config work independently
                val copy = existing.copy(
                    id = 0L,
                    appWidgetId = appWidgetId,
                    isPinned = true,
                    lastUsedAt = System.currentTimeMillis()
                )
                widgetDao.insertWidget(copy)
            }
        }
    }

    suspend fun unbindAppWidgetIds(appWidgetIds: IntArray) = withContext(Dispatchers.IO) {
        for (id in appWidgetIds) {
            widgetDao.clearAppWidgetBinding(id)
        }
    }

    suspend fun recordRecentType(typeId: String) {
        preferences.recordRecentType(typeId)
    }

    suspend fun setPendingPinConfigId(configId: Long) {
        preferences.setPendingPinConfigId(configId)
    }

    suspend fun setThemeMode(mode: AppThemeMode) = preferences.setThemeMode(mode)
    suspend fun setAccentColor(accent: AppAccentColor) = preferences.setAccentColor(accent)
    suspend fun setAllowWeatherNetwork(allowed: Boolean) = preferences.setAllowWeatherNetwork(allowed)

    fun collectSystemTelemetry(): SystemTelemetry = SystemInfoCollector.collect(context)

    fun getLaunchableApps(): List<InstalledAppEntry> = SystemInfoCollector.getLaunchableApps(context)

    suspend fun searchCities(query: String): Result<List<GeocodingResultDto>> = withContext(Dispatchers.IO) {
        runCatching {
            if (query.trim().length < 2) emptyList()
            else WeatherNetworkClient.geocodingApi.searchCity(query.trim()).results.orEmpty()
        }
    }

    suspend fun fetchLiveWeather(payload: WidgetPayload): Result<WidgetPayload> = withContext(Dispatchers.IO) {
        runCatching {
            val response = WeatherNetworkClient.forecastApi.getForecast(
                latitude = payload.latitude,
                longitude = payload.longitude
            )
            val current = response.current
            val daily = response.daily
            val forecastList = buildList {
                if (daily != null) {
                    val count = minOf(daily.time.size, daily.tempMax.size, daily.tempMin.size, daily.weatherCode.size, 4)
                    val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val outFmt = SimpleDateFormat("EEE", Locale.getDefault())
                    for (i in 0 until count) {
                        val dayLabel = if (i == 0) "Today" else {
                            try {
                                val parsed = inFmt.parse(daily.time[i])
                                if (parsed != null) outFmt.format(parsed) else daily.time[i]
                            } catch (_: Exception) {
                                daily.time[i]
                            }
                        }
                        val code = daily.weatherCode[i]
                        add(
                            ForecastDay(
                                dayLabel = dayLabel,
                                condition = WeatherNetworkClient.describeWmoCode(code),
                                code = code,
                                highC = daily.tempMax[i],
                                lowC = daily.tempMin[i]
                            )
                        )
                    }
                }
            }
            val sunTimes = SunCalculator.calculate(payload.latitude, payload.longitude)
            val sunrise = daily?.sunrise?.firstOrNull()?.substringAfter("T")?.take(5) ?: sunTimes.sunriseText
            val sunset = daily?.sunset?.firstOrNull()?.substringAfter("T")?.take(5) ?: sunTimes.sunsetText

            payload.copy(
                currentTempC = current?.temperature2m ?: payload.currentTempC,
                feelsLikeC = current?.apparentTemperature ?: payload.feelsLikeC,
                weatherCode = current?.weatherCode ?: payload.weatherCode,
                weatherCondition = WeatherNetworkClient.describeWmoCode(current?.weatherCode ?: payload.weatherCode),
                windKmh = current?.windSpeed10m ?: payload.windKmh,
                humidityPercent = current?.relativeHumidity2m ?: payload.humidityPercent,
                sunriseIso = sunrise,
                sunsetIso = sunset,
                weatherUpdatedAtMs = System.currentTimeMillis(),
                forecast = if (forecastList.isNotEmpty()) forecastList else payload.forecast
            )
        }
    }

    suspend fun copyPickedPhotoToInternal(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "widget_photos").apply { mkdirs() }
            val outFile = File(dir, "photo_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                val raw = BitmapFactory.decodeStream(input) ?: return@withContext null
                val maxDim = 960
                val scale = minOf(1f, maxDim.toFloat() / maxOf(raw.width, raw.height).toFloat())
                val resized = if (scale < 1f) {
                    Bitmap.createScaledBitmap(
                        raw,
                        (raw.width * scale).toInt().coerceAtLeast(1),
                        (raw.height * scale).toInt().coerceAtLeast(1),
                        true
                    )
                } else raw
                FileOutputStream(outFile).use { fos ->
                    resized.compress(Bitmap.CompressFormat.JPEG, 88, fos)
                }
                if (resized !== raw) raw.recycle()
                resized.recycle()
            }
            "file://${outFile.absolutePath}"
        } catch (_: Exception) {
            null
        }
    }

    suspend fun exportAllConfigsJson(): String = withContext(Dispatchers.IO) {
        val list = widgetDao.getAllWidgetsSnapshot().map {
            it.copy(appWidgetId = -1, isPinned = false)
        }
        exportAdapter.toJson(WidgetExportBundle(widgets = list))
    }

    suspend fun importConfigsJson(json: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val bundle = exportAdapter.fromJson(json)
                ?: throw IllegalArgumentException("Invalid WidgetLite configuration file.")
            val sanitized = bundle.widgets.map {
                it.copy(id = 0L, appWidgetId = -1, isPinned = false, lastUsedAt = System.currentTimeMillis())
            }
            widgetDao.insertWidgets(sanitized)
            sanitized.size
        }
    }

    suspend fun clearAllUserPhotosAndCache() = withContext(Dispatchers.IO) {
        File(context.filesDir, "widget_photos").deleteRecursively()
        val now = Date()
        now.time
    }
}
