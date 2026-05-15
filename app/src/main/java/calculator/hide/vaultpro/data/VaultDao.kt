package calculator.hide.vaultpro.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for VaultItem
 */
@Dao
interface VaultDao {
    
    @Query("SELECT * FROM vault_items WHERE type = :type ORDER BY createdAt DESC")
    fun getItemsByType(type: ItemType): Flow<List<VaultItem>>
    
    @Query("SELECT * FROM vault_items WHERE id = :id")
    suspend fun getItemById(id: Long): VaultItem?
    
    @Insert
    suspend fun insert(item: VaultItem): Long
    
    @Delete
    suspend fun delete(item: VaultItem)
    
    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("SELECT COUNT(*) FROM vault_items WHERE type = :type")
    suspend fun getCountByType(type: ItemType): Int
}

