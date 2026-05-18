package calculator.hide.vault.data.launcher.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val screenIndex: Int? = null,
    val cellX: Int? = null,
    val cellY: Int? = null,
    val inDock: Boolean = false,
    val dockPosition: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)
