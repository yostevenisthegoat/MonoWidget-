package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.WidgetConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface WidgetDao {
    @Query("SELECT * FROM widget_configs ORDER BY lastUsedAt DESC")
    fun getAllWidgets(): Flow<List<WidgetConfig>>

    @Query("SELECT * FROM widget_configs ORDER BY lastUsedAt DESC")
    suspend fun getAllWidgetsSnapshot(): List<WidgetConfig>

    @Query("SELECT * FROM widget_configs WHERE isPinned = 1 ORDER BY lastUsedAt DESC")
    fun getPinnedWidgets(): Flow<List<WidgetConfig>>

    @Query("SELECT * FROM widget_configs ORDER BY lastUsedAt DESC LIMIT :limit")
    fun getRecentWidgets(limit: Int = 6): Flow<List<WidgetConfig>>

    @Query("SELECT * FROM widget_configs WHERE id = :id LIMIT 1")
    suspend fun getWidgetById(id: Long): WidgetConfig?

    @Query("SELECT * FROM widget_configs WHERE appWidgetId = :appWidgetId LIMIT 1")
    suspend fun getWidgetByAppWidgetId(appWidgetId: Int): WidgetConfig?

    @Query("SELECT * FROM widget_configs WHERE appWidgetId != -1")
    suspend fun getBoundAppWidgets(): List<WidgetConfig>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWidget(config: WidgetConfig): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWidgets(configs: List<WidgetConfig>)

    @Update
    suspend fun updateWidget(config: WidgetConfig)

    @Query("DELETE FROM widget_configs WHERE id = :id")
    suspend fun deleteWidgetById(id: Long)

    @Query("UPDATE widget_configs SET appWidgetId = -1, isPinned = 0 WHERE appWidgetId = :appWidgetId")
    suspend fun clearAppWidgetBinding(appWidgetId: Int)

    @Query("SELECT COUNT(*) FROM widget_configs")
    suspend fun count(): Int
}
