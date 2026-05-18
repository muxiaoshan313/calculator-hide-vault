package calculator.hide.vault.data.launcher.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders")
    suspend fun getAll(): List<FolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FolderEntity): Long

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteById(id: Long)
}
