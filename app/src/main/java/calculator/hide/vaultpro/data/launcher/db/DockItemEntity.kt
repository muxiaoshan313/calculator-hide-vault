package calculator.hide.vaultpro.data.launcher.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dock_items")
data class DockItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val className: String,
    val position: Int
) {
    val key: String
        get() = "$packageName/$className"
}
