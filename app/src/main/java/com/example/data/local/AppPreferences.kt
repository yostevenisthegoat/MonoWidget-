package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "widgetlite_prefs")

enum class AppThemeMode(val displayName: String) {
    SYSTEM("Follow System"),
    LIGHT("Light Mode"),
    DARK("Dark Mode")
}

enum class AppAccentColor(val displayName: String, val hex: String, val colorLong: Long) {
    COBALT("Cobalt", "#2563EB", 0xFF2563EB),
    EMERALD("Emerald", "#059669", 0xFF059669),
    AMBER("Amber", "#D97706", 0xFFD97706),
    TERRACOTTA("Terracotta", "#DC2626", 0xFFDC2626),
    VIOLET("Violet", "#7C3AED", 0xFF7C3AED),
    GRAPHITE("Monochrome", "#18181B", 0xFF18181B)
}

data class UserSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val accentColor: AppAccentColor = AppAccentColor.COBALT,
    val allowWeatherNetwork: Boolean = true,
    val recentTypesCsv: String = "digital_clock,weather,todo_list,pomodoro_timer",
    val pendingPinConfigId: Long = -1L
) {
    val recentTypeIds: List<String>
        get() = recentTypesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

class AppPreferences(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val ALLOW_WEATHER_NETWORK = booleanPreferencesKey("allow_weather_network")
        val RECENT_TYPES = stringPreferencesKey("recent_types")
        val PENDING_PIN_CONFIG_ID = longPreferencesKey("pending_pin_config_id")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val theme = AppThemeMode.entries.firstOrNull {
            it.name == prefs[Keys.THEME_MODE]
        } ?: AppThemeMode.SYSTEM

        val accent = AppAccentColor.entries.firstOrNull {
            it.name == prefs[Keys.ACCENT_COLOR]
        } ?: AppAccentColor.COBALT

        UserSettings(
            themeMode = theme,
            accentColor = accent,
            allowWeatherNetwork = prefs[Keys.ALLOW_WEATHER_NETWORK] ?: true,
            recentTypesCsv = prefs[Keys.RECENT_TYPES] ?: "digital_clock,weather,todo_list,pomodoro_timer",
            pendingPinConfigId = prefs[Keys.PENDING_PIN_CONFIG_ID] ?: -1L
        )
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAccentColor(accent: AppAccentColor) {
        context.dataStore.edit { it[Keys.ACCENT_COLOR] = accent.name }
    }

    suspend fun setAllowWeatherNetwork(allowed: Boolean) {
        context.dataStore.edit { it[Keys.ALLOW_WEATHER_NETWORK] = allowed }
    }

    suspend fun recordRecentType(typeId: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[Keys.RECENT_TYPES] ?: "")
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() && it != typeId }
            val updated = (listOf(typeId) + current).take(8)
            prefs[Keys.RECENT_TYPES] = updated.joinToString(",")
        }
    }

    suspend fun setPendingPinConfigId(configId: Long) {
        context.dataStore.edit { it[Keys.PENDING_PIN_CONFIG_ID] = configId }
    }
}
