package calculator.hide.vaultpro.data.launcher.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DockItemDao {
    @Query("SELECT * FROM dock_items ORDER BY position ASC")
    suspend fun getAll(): List<DockItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DockItemEntity): Long

    @Query("DELETE FROM dock_items WHERE packageName = :packageName AND className = :className")
    suspend fun deleteByKey(packageName: String, className: String)

    @Query("UPDATE dock_items SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Int)

    @Query("DELETE FROM dock_items WHERE packageName || '/' || className NOT IN (:validKeys)")
    suspend fun clearInvalidItems(validKeys: List<String>)
}
