package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.WidgetConfig

@Database(entities = [WidgetConfig::class], version = 1, exportSchema = false)
abstract class WidgetDatabase : RoomDatabase() {
    abstract fun widgetDao(): WidgetDao

    companion object {
        @Volatile
        private var INSTANCE: WidgetDatabase? = null

        fun getInstance(context: Context): WidgetDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WidgetDatabase::class.java,
                    "widgetlite.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
