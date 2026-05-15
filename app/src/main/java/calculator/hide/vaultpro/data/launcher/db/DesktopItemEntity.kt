package calculator.hide.vaultpro.data.launcher.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "desktop_items")
data class DesktopItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val className: String,
    val screenIndex: Int,
    val cellX: Int,
    val cellY: Int,
    val spanX: Int = 1,
    val spanY: Int = 1,
    val itemType: String = "APP"
) {
    val key: String
        get() = "$packageName/$className"
}
