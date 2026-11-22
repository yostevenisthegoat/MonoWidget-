package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.WidgetLiteApplication
import com.example.data.local.AppAccentColor
import com.example.data.local.AppThemeMode
import com.example.data.local.UserSettings
import com.example.data.model.WidgetCategory
import com.example.data.model.WidgetConfig
import com.example.data.model.WidgetPayload
import com.example.data.model.WidgetPayloadCodec
import com.example.data.model.WidgetType
import com.example.data.remote.GeocodingResultDto
import com.example.util.InstalledAppEntry
import com.example.util.QuotesBank
import com.example.util.SystemTelemetry
import com.example.widget.PinResult
import com.example.widget.WidgetUpdateHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class AppTab(val route: String, val label: String) {
    HOME("home", "Home"),
    EXPLORE("explore", "Explore"),
    MY_WIDGETS("my_widgets", "My Widgets"),
    STUDIO("studio", "Studio"),
    SETTINGS("settings", "Settings")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as WidgetLiteApplication).repository

    val savedWidgets: StateFlow<List<WidgetConfig>> = repo.allWidgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSettings: StateFlow<UserSettings> = repo.userSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    private val _telemetry = MutableStateFlow(repo.collectSystemTelemetry())
    val telemetry: StateFlow<SystemTelemetry> = _telemetry.asStateFlow()

    private val _studioConfig = MutableStateFlow(WidgetConfig.createDefault(WidgetType.DIGITAL_CLOCK))
    val studioConfig: StateFlow<WidgetConfig> = _studioConfig.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<WidgetCategory?>(null)
    val selectedCategory: StateFlow<WidgetCategory?> = _selectedCategory.asStateFlow()

    private val _cityResults = MutableStateFlow<List<GeocodingResultDto>>(emptyList())
    val cityResults: StateFlow<List<GeocodingResultDto>> = _cityResults.asStateFlow()

    private val _isWeatherLoading = MutableStateFlow(false)
    val isWeatherLoading: StateFlow<Boolean> = _isWeatherLoading.asStateFlow()

    private val _launchableApps = MutableStateFlow<List<InstalledAppEntry>>(emptyList())
    val launchableApps: StateFlow<List<InstalledAppEntry>> = _launchableApps.asStateFlow()

    private val _nowMillis = MutableStateFlow(System.currentTimeMillis())
    val nowMillis: StateFlow<Long> = _nowMillis.asStateFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        viewModelScope.launch {
            repo.ensureSeededDefaults()
            _launchableApps.value = repo.getLaunchableApps()
        }
        // Live ticker for clock, stopwatch, pomodoro & telemetry inside app
        viewModelScope.launch {
            var tick = 0
            while (isActive) {
                _nowMillis.value = System.currentTimeMillis()
                if (tick % 5 == 0) {
                    _telemetry.value = repo.collectSystemTelemetry()
                }
                tick++
                delay(1000L)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: WidgetCategory?) {
        _selectedCategory.value = category
    }

    fun openTypeInStudio(type: WidgetType, isDarkTheme: Boolean = false) {
        viewModelScope.launch {
            repo.recordRecentType(type.id)
        }
        _studioConfig.value = WidgetConfig.createDefault(type, isDarkTheme)
    }

    fun openConfigInStudio(config: WidgetConfig) {
        viewModelScope.launch {
            repo.recordRecentType(config.typeId)
        }
        _studioConfig.value = config
    }

    fun openConfigById(configId: Long) {
        viewModelScope.launch {
            val found = repo.getWidgetById(configId)
            if (found != null) {
                _studioConfig.value = found
            }
        }
    }

    fun updateStudioConfig(transform: (WidgetConfig) -> WidgetConfig) {
        _studioConfig.value = transform(_studioConfig.value)
    }

    fun updateStudioPayload(transform: (WidgetPayload) -> WidgetPayload) {
        val current = _studioConfig.value
        val nextPayload = transform(current.decodedPayload())
        _studioConfig.value = current.copy(payloadJson = WidgetPayloadCodec.encode(nextPayload))
    }

    fun resetStudioToDefault(isDarkTheme: Boolean) {
        val current = _studioConfig.value
        val fresh = WidgetConfig.createDefault(current.widgetType, isDarkTheme).copy(
            id = current.id,
            appWidgetId = current.appWidgetId,
            isPinned = current.isPinned
        )
        _studioConfig.value = fresh
        emitMessage("Reset ${current.widgetType.title} to default style.")
    }

    fun saveStudioWidget() {
        viewModelScope.launch {
            val current = _studioConfig.value
            val savedId = repo.saveWidget(current)
            _studioConfig.value = current.copy(id = savedId, lastUsedAt = System.currentTimeMillis())
            WidgetUpdateHelper.updateAllWidgets(getApplication())
            emitMessage("Saved \"${current.title}\" to My Widgets.")
        }
    }

    fun duplicateWidget(config: WidgetConfig) {
        viewModelScope.launch {
            val copy = config.copy(
                id = 0L,
                title = "${config.title} Copy",
                appWidgetId = -1,
                isPinned = false,
                createdAt = System.currentTimeMillis(),
                lastUsedAt = System.currentTimeMillis()
            )
            repo.saveWidget(copy)
            emitMessage("Duplicated widget.")
        }
    }

    fun deleteWidget(config: WidgetConfig) {
        viewModelScope.launch {
            repo.deleteWidget(config.id)
            emitMessage("Deleted \"${config.title}\".")
        }
    }

    fun pinStudioWidgetToHome() {
        pinConfigToHome(_studioConfig.value)
    }

    fun pinConfigToHome(config: WidgetConfig) {
        viewModelScope.launch {
            val savedId = repo.saveWidget(config)
            val saved = config.copy(id = savedId)
            if (saved.id == _studioConfig.value.id || _studioConfig.value.id == 0L) {
                _studioConfig.value = saved
            }
            when (WidgetUpdateHelper.requestPinWidget(getApplication(), saved)) {
                PinResult.REQUEST_SENT -> emitMessage("Confirm widget placement on your home screen.")
                PinResult.LAUNCHER_UNSUPPORTED -> emitMessage("Saved! Long-press your home screen -> Widgets -> WidgetLite to add.")
                PinResult.REQUIRES_MANUAL_ADD -> emitMessage("Saved! Add WidgetLite from your home screen widget drawer.")
            }
        }
    }

    fun handlePreviewInteraction(slot: Int = 0) {
        val config = _studioConfig.value
        val p = config.decodedPayload()
        val now = System.currentTimeMillis()
        when (config.widgetType) {
            WidgetType.RANDOM_NUMBER -> {
                val lo = minOf(p.rngMin, p.rngMax)
                val hi = maxOf(p.rngMin, p.rngMax)
                val rolled = if (lo == hi) lo else Random.nextInt(lo, hi + 1)
                updateStudioPayload { it.copy(rngLastValue = rolled) }
            }
            WidgetType.QUOTES -> {
                updateStudioPayload { it.copy(quoteIndex = (it.quoteIndex + 1) % QuotesBank.quotes.size) }
            }
            WidgetType.DAILY_REMINDER -> {
                updateStudioPayload { it.copy(reminderDoneToday = !it.reminderDoneToday) }
            }
            WidgetType.FLASHLIGHT -> {
                val toggled = toggleDeviceFlashlight(!p.flashlightOn)
                updateStudioPayload { it.copy(flashlightOn = toggled) }
            }
            WidgetType.STOPWATCH -> {
                if (slot == 2) {
                    updateStudioPayload { it.copy(stopwatchRunning = false, stopwatchAccumulatedMs = 0L, stopwatchStartedAtMs = 0L) }
                } else {
                    if (p.stopwatchRunning) {
                        val acc = p.stopwatchAccumulatedMs + (now - p.stopwatchStartedAtMs).coerceAtLeast(0L)
                        updateStudioPayload { it.copy(stopwatchRunning = false, stopwatchAccumulatedMs = acc, stopwatchStartedAtMs = 0L) }
                    } else {
                        updateStudioPayload { it.copy(stopwatchRunning = true, stopwatchStartedAtMs = now) }
                    }
                }
            }
            WidgetType.POMODORO_TIMER -> {
                when (slot) {
                    2 -> updateStudioPayload {
                        val nextBreak = !it.pomodoroIsBreak
                        val mins = if (nextBreak) it.pomodoroBreakMinutes else it.pomodoroMinutes
                        it.copy(pomodoroIsBreak = nextBreak, pomodoroRunning = false, pomodoroRemainingMs = mins * 60_000L, pomodoroLastTickMs = 0L)
                    }
                    3 -> updateStudioPayload {
                        val mins = if (it.pomodoroIsBreak) it.pomodoroBreakMinutes else it.pomodoroMinutes
                        it.copy(pomodoroRunning = false, pomodoroRemainingMs = mins * 60_000L, pomodoroLastTickMs = 0L)
                    }
                    else -> updateStudioPayload {
                        if (it.pomodoroRunning) {
                            val elapsed = if (it.pomodoroLastTickMs > 0) now - it.pomodoroLastTickMs else 0L
                            val rem = (it.pomodoroRemainingMs - elapsed).coerceAtLeast(0L)
                            it.copy(pomodoroRunning = false, pomodoroRemainingMs = rem, pomodoroLastTickMs = 0L)
                        } else {
                            val base = if (it.pomodoroRemainingMs <= 0L) it.pomodoroMinutes * 60_000L else it.pomodoroRemainingMs
                            it.copy(pomodoroRunning = true, pomodoroRemainingMs = base, pomodoroLastTickMs = now)
                        }
                    }
                }
            }
            WidgetType.DAILY_GOALS -> {
                when (slot) {
                    1 -> updateStudioPayload { it.copy(goalCurrent = (it.goalCurrent - 1).coerceAtLeast(0)) }
                    2 -> updateStudioPayload { it.copy(goalCurrent = it.goalCurrent + 1) }
                    3 -> updateStudioPayload { it.copy(goalCurrent = 0) }
                    else -> updateStudioPayload { it.copy(goalCurrent = it.goalCurrent + 1) }
                }
            }
            WidgetType.TODO_LIST -> {
                val idx = (slot - 1).coerceIn(0, p.todos.lastIndex.coerceAtLeast(0))
                updateStudioPayload {
                    it.copy(todos = it.todos.mapIndexed { i, item -> if (i == idx) item.copy(done = !item.done) else item })
                }
            }
            WidgetType.HABIT_TRACKER -> {
                val idx = (slot - 1).coerceIn(0, p.habits.lastIndex.coerceAtLeast(0))
                updateStudioPayload {
                    it.copy(habits = it.habits.mapIndexed { i, h ->
                        if (i == idx) {
                            val nextDone = !h.completedToday
                            val nextStreak = if (nextDone) h.streak + 1 else (h.streak - 1).coerceAtLeast(0)
                            h.copy(completedToday = nextDone, streak = nextStreak)
                        } else h
                    })
                }
            }
            WidgetType.WEATHER, WidgetType.TEMPERATURE, WidgetType.WEATHER_FORECAST -> refreshWeatherForStudio()
            else -> {}
        }
    }

    private fun toggleDeviceFlashlight(targetOn: Boolean): Boolean {
        return try {
            val cm = getApplication<Application>().getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return false
            val camId = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return false
            cm.setTorchMode(camId, targetOn)
            targetOn
        } catch (_: Exception) {
            emitMessage("Flashlight hardware unavailable on this device.")
            false
        }
    }

    fun searchCities(query: String) {
        if (!userSettings.value.allowWeatherNetwork) {
            emitMessage("Weather network access is disabled in Privacy Settings.")
            return
        }
        viewModelScope.launch {
            repo.searchCities(query)
                .onSuccess { _cityResults.value = it }
                .onFailure { emitMessage("Unable to search cities offline.") }
        }
    }

    fun selectCityAndFetchWeather(city: GeocodingResultDto) {
        val label = listOfNotNull(city.name, city.country).joinToString(", ")
        updateStudioPayload {
            it.copy(cityName = label, latitude = city.latitude, longitude = city.longitude)
        }
        _cityResults.value = emptyList()
        refreshWeatherForStudio()
    }

    fun refreshWeatherForStudio() {
        if (!userSettings.value.allowWeatherNetwork) {
            emitMessage("Weather network access is disabled in Privacy Settings.")
            return
        }
        viewModelScope.launch {
            _isWeatherLoading.value = true
            val payload = _studioConfig.value.decodedPayload()
            repo.fetchLiveWeather(payload)
                .onSuccess { updated ->
                    updateStudioPayload { updated }
                    emitMessage("Updated weather for ${updated.cityName}.")
                }
                .onFailure {
                    emitMessage("Could not fetch weather. Check internet connection.")
                }
            _isWeatherLoading.value = false
        }
    }

    fun importPhotoFromPicker(uri: Uri) {
        viewModelScope.launch {
            val savedPath = repo.copyPickedPhotoToInternal(uri)
            if (savedPath != null) {
                updateStudioConfig { it.copy(bgImageKey = savedPath) }
                emitMessage("Background photo updated.")
            } else {
                emitMessage("Could not read selected image.")
            }
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { repo.setThemeMode(mode) }
    }

    fun setAccentColor(accent: AppAccentColor) {
        viewModelScope.launch { repo.setAccentColor(accent) }
    }

    fun setAllowWeatherNetwork(allowed: Boolean) {
        viewModelScope.launch { repo.setAllowWeatherNetwork(allowed) }
    }

    suspend fun buildExportJson(): String = repo.exportAllConfigsJson()

    fun importFromJson(json: String) {
        viewModelScope.launch {
            repo.importConfigsJson(json)
                .onSuccess { count -> emitMessage("Imported $count widget configuration(s).") }
                .onFailure { emitMessage("Invalid configuration file.") }
        }
    }

    fun clearLocalPhotos() {
        viewModelScope.launch {
            repo.clearAllUserPhotosAndCache()
            emitMessage("Cleared local cached widget photos.")
        }
    }

    fun refreshTelemetryAndWidgets() {
        viewModelScope.launch {
            _telemetry.value = repo.collectSystemTelemetry()
            WidgetUpdateHelper.updateAllWidgets(getApplication())
            emitMessage("Refreshed telemetry & active home screen widgets.")
        }
    }

    fun emitMessage(msg: String) {
        viewModelScope.launch { _messages.emit(msg) }
    }
}
