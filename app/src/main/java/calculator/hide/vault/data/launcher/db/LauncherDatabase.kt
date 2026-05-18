package calculator.hide.vault.data.launcher.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        HiddenAppEntity::class,
        DesktopItemEntity::class,
        DockItemEntity::class,
        FolderEntity::class,
        FolderItemEntity::class,
        WidgetItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun hiddenAppDao(): HiddenAppDao
    abstract fun desktopItemDao(): DesktopItemDao
    abstract fun dockItemDao(): DockItemDao
    abstract fun folderDao(): FolderDao
    abstract fun folderItemDao(): FolderItemDao
    abstract fun widgetItemDao(): WidgetItemDao

    companion object {
        @Volatile
        private var INSTANCE: LauncherDatabase? = null

        fun getInstance(context: Context): LauncherDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LauncherDatabase::class.java,
                    "launcher_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
