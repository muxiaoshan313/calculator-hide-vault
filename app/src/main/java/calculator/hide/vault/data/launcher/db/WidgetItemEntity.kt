package calculator.hide.vault.data.launcher.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "widget_items")
data class WidgetItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appWidgetId: Int,
    val providerPackage: String,
    val providerClass: String,
    val screenIndex: Int,
    val cellX: Int,
    val cellY: Int,
    val spanX: Int,
    val spanY: Int
)
