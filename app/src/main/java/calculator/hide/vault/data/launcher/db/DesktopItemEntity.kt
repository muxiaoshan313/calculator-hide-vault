package calculator.hide.vault.data.launcher.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import calculator.hide.vault.data.launcher.LauncherItemType

@Entity(tableName = "desktop_items")
data class DesktopItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemType: String = LauncherItemType.APP,
    val packageName: String? = null,
    val className: String? = null,
    val screenIndex: Int,
    val cellX: Int,
    val cellY: Int,
    val spanX: Int = 1,
    val spanY: Int = 1,
    val folderId: Long? = null,
    val widgetId: Int? = null,
    val title: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val key: String?
        get() = if (packageName != null && className != null) "$packageName/$className" else null
}
