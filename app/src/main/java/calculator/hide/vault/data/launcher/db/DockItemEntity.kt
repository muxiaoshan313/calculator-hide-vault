package calculator.hide.vault.data.launcher.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import calculator.hide.vault.data.launcher.LauncherItemType

@Entity(tableName = "dock_items")
data class DockItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemType: String = LauncherItemType.APP,
    val packageName: String? = null,
    val className: String? = null,
    val position: Int,
    val folderId: Long? = null,
    val title: String? = null
) {
    val key: String?
        get() = if (packageName != null && className != null) "$packageName/$className" else null
}
