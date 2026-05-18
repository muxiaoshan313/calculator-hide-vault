package calculator.hide.vault.data.launcher.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FolderItemDao {
    @Query("SELECT * FROM folder_items WHERE folderId = :folderId ORDER BY sortIndex ASC")
    suspend fun getByFolder(folderId: Long): List<FolderItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FolderItemEntity): Long

    @Query("DELETE FROM folder_items WHERE packageName = :packageName AND className = :className")
    suspend fun deleteByKey(packageName: String, className: String)

    @Query("DELETE FROM folder_items WHERE folderId = :folderId")
    suspend fun deleteByFolder(folderId: Long)
}
