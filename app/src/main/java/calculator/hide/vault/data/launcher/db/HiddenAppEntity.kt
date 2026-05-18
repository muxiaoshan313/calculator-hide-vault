package calculator.hide.vault.data.launcher.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import calculator.hide.vault.data.launcher.HiddenAppMode

@Entity(tableName = "hidden_apps")
data class HiddenAppEntity(
    @PrimaryKey val key: String,
    val packageName: String,
    val className: String,
    val sortIndex: Int,
    val addedAt: Long,
    val mode: String = HiddenAppMode.LOCAL
)
