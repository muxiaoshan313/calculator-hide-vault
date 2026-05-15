package calculator.hide.vaultpro.data.launcher.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        HiddenAppEntity::class,
        DesktopItemEntity::class,
        DockItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun hiddenAppDao(): HiddenAppDao
    abstract fun desktopItemDao(): DesktopItemDao
    abstract fun dockItemDao(): DockItemDao

    companion object {
        @Volatile
        private var INSTANCE: LauncherDatabase? = null

        fun getInstance(context: Context): LauncherDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LauncherDatabase::class.java,
                    "launcher_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
