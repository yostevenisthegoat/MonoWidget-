package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.local.AppPreferences
import com.example.data.local.WidgetDatabase
import com.example.data.repository.WidgetRepository

class WidgetLiteApplication : Application() {

    val database: WidgetDatabase by lazy { WidgetDatabase.getInstance(this) }
    val preferences: AppPreferences by lazy { AppPreferences(this) }
    val repository: WidgetRepository by lazy {
        WidgetRepository(this, database.widgetDao(), preferences)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "WidgetLite Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Optional local daily reminder alerts configured by you."
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val REMINDER_CHANNEL_ID = "widgetlite_reminders"
    }
}
