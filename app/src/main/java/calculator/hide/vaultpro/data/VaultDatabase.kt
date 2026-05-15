package calculator.hide.vaultpro.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for vault items
 */
@Database(entities = [VaultItem::class], version = 2, exportSchema = false)
abstract class VaultDatabase : RoomDatabase() {
    
    abstract fun vaultDao(): VaultDao
    
    companion object {
        @Volatile
        private var INSTANCE: VaultDatabase? = null
        
        fun getDatabase(context: Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "vault_database"
                )
                .fallbackToDestructiveMigration() // For development - will recreate DB on version change
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

