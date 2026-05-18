package calculator.hide.vault.data.launcher.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folder_items")
data class FolderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long,
    val packageName: String,
    val className: String,
    val sortIndex: Int
)
