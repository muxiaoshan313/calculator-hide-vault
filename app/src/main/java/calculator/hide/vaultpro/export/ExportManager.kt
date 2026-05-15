package calculator.hide.vaultpro.export

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import calculator.hide.vaultpro.data.ItemType
import calculator.hide.vaultpro.data.VaultItem
import calculator.hide.vaultpro.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException

/**
 * Handles exporting files from vault to system storage (MediaStore)
 * Supports Photos, Videos, and Files
 */
class ExportManager(
    private val context: Context,
    private val cryptoManager: CryptoManager
) {
    
    /**
     * Export result
     */
    data class ExportResult(
        val success: Boolean,
        val uri: Uri? = null,
        val errorMessage: String? = null
    )
    
    /**
     * Export a vault item to system storage
     * @param item The vault item to export
     * @return ExportResult
     */
    suspend fun exportItem(item: VaultItem): ExportResult = withContext(Dispatchers.IO) {
        try {
            val encryptedFile = File(item.encryptedPath)
            if (!encryptedFile.exists()) {
                return@withContext ExportResult(false, null, "Encrypted file not found")
            }
            
            when (item.type) {
                ItemType.PHOTO -> exportPhoto(item)
                ItemType.VIDEO -> exportVideo(item)
                ItemType.FILE -> exportFile(item)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ExportResult(false, null, e.message ?: "Export failed")
        }
    }
    
    /**
     * Export photo to MediaStore Images
     */
    private suspend fun exportPhoto(item: VaultItem): ExportResult = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            
            // Create content values
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, item.displayName)
                put(MediaStore.Images.Media.MIME_TYPE, item.mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/VaultCalculator")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            
            // Insert URI
            val uri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return@withContext ExportResult(false, null, "Failed to create image URI")
            
            // Write decrypted content using stream decryption
            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val encryptedFile = File(item.encryptedPath)
                    cryptoManager.decryptFileToOutputStream(encryptedFile, outputStream)
                } ?: throw FileNotFoundException("Failed to open output stream")
                
                // Update IS_PENDING to 0 (Android Q+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                }
                
                ExportResult(true, uri)
            } catch (e: Exception) {
                // Clean up on failure
                try {
                    contentResolver.delete(uri, null, null)
                } catch (ignored: Exception) {}
                throw e
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ExportResult(false, null, e.message ?: "Failed to export photo")
        }
    }
    
    /**
     * Export video to MediaStore Video
     */
    private suspend fun exportVideo(item: VaultItem): ExportResult = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            
            // Create content values
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, item.displayName)
                put(MediaStore.Video.Media.MIME_TYPE, item.mimeType)
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/VaultCalculator")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }
            
            // Insert URI
            val uri = contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return@withContext ExportResult(false, null, "Failed to create video URI")
            
            // Write decrypted content using stream decryption
            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val encryptedFile = File(item.encryptedPath)
                    cryptoManager.decryptFileToOutputStream(encryptedFile, outputStream)
                } ?: throw FileNotFoundException("Failed to open output stream")
                
                // Update IS_PENDING to 0 (Android Q+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                }
                
                ExportResult(true, uri)
            } catch (e: Exception) {
                // Clean up on failure
                try {
                    contentResolver.delete(uri, null, null)
                } catch (ignored: Exception) {}
                throw e
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ExportResult(false, null, e.message ?: "Failed to export video")
        }
    }
    
    /**
     * Export file to MediaStore Downloads
     */
    private suspend fun exportFile(item: VaultItem): ExportResult = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            
            // Create content values
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, item.displayName)
                put(MediaStore.Downloads.MIME_TYPE, item.mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/VaultCalculator")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
            }
            
            // Insert URI
            val uri = contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return@withContext ExportResult(false, null, "Failed to create download URI")
            
            // Write decrypted content using stream decryption
            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val encryptedFile = File(item.encryptedPath)
                    cryptoManager.decryptFileToOutputStream(encryptedFile, outputStream)
                } ?: throw FileNotFoundException("Failed to open output stream")
                
                // Update IS_PENDING to 0 (Android Q+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                }
                
                ExportResult(true, uri)
            } catch (e: Exception) {
                // Clean up on failure
                try {
                    contentResolver.delete(uri, null, null)
                } catch (ignored: Exception) {}
                throw e
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ExportResult(false, null, e.message ?: "Failed to export file")
        }
    }
}

