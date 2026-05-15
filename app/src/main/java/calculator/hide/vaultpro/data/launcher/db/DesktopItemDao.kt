package calculator.hide.vaultpro.data.launcher.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DesktopItemDao {
    @Query("SELECT * FROM desktop_items")
    suspend fun getAll(): List<DesktopItemEntity>

    @Query("SELECT * FROM desktop_items WHERE screenIndex = :screenIndex")
    suspend fun getByScreen(screenIndex: Int): List<DesktopItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DesktopItemEntity): Long

    @Query("DELETE FROM desktop_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM desktop_items WHERE packageName = :packageName AND className = :className")
    suspend fun deleteByKey(packageName: String, className: String)

    @Query(
        "UPDATE desktop_items SET screenIndex = :screenIndex, cellX = :cellX, cellY = :cellY WHERE id = :id"
    )
    suspend fun updatePosition(id: Long, screenIndex: Int, cellX: Int, cellY: Int)

    @Query("DELETE FROM desktop_items WHERE packageName || '/' || className NOT IN (:validKeys)")
    suspend fun clearInvalidItems(validKeys: List<String>)
}
