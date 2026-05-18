package calculator.hide.vault.data.launcher.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HiddenAppDao {
    @Query("SELECT * FROM hidden_apps ORDER BY sortIndex ASC")
    suspend fun getAll(): List<HiddenAppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HiddenAppEntity)

    @Query("DELETE FROM hidden_apps WHERE `key` = :key")
    suspend fun deleteByKey(key: String)

    @Query("SELECT COUNT(*) > 0 FROM hidden_apps WHERE `key` = :key")
    suspend fun isHidden(key: String): Boolean

    @Query("DELETE FROM hidden_apps")
    suspend fun clearAll()

    @Query("DELETE FROM hidden_apps WHERE `key` NOT IN (:validKeys)")
    suspend fun clearInvalid(validKeys: List<String>)
}
