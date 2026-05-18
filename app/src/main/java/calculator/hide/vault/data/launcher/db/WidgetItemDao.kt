package calculator.hide.vault.data.launcher.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WidgetItemDao {
    @Query("SELECT * FROM widget_items")
    suspend fun getAll(): List<WidgetItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WidgetItemEntity): Long

    @Query("DELETE FROM widget_items WHERE appWidgetId = :appWidgetId")
    suspend fun deleteByWidgetId(appWidgetId: Int)
}
