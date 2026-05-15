package calculator.hide.vaultpro.data.launcher.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hidden_apps")
data class HiddenAppEntity(
    @PrimaryKey val key: String,
    val packageName: String,
    val className: String,
    val sortIndex: Int,
    val addedAt: Long
)
