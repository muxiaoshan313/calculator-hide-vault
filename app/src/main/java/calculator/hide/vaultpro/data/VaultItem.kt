package calculator.hide.vaultpro.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a vault item (photo/video/file)
 */
@Entity(tableName = "vault_items")
data class VaultItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val type: ItemType, // PHOTO, VIDEO, FILE
    
    val displayName: String, // Original filename
    
    val mimeType: String, // MIME type (e.g., "image/jpeg", "video/mp4")
    
    val encryptedPath: String, // Path to encrypted .vault file
    
    val size: Long, // File size in bytes
    
    val createdAt: Long // Timestamp in milliseconds
)

enum class ItemType {
    PHOTO,
    VIDEO,
    FILE
}

