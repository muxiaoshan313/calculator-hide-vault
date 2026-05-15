package calculator.hide.vaultpro.import

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import calculator.hide.vaultpro.data.ItemType
import calculator.hide.vaultpro.data.VaultItem
import calculator.hide.vaultpro.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Handles importing files via Storage Access Framework (SAF)
 * Supports both "Import" (keep original) and "Move" (delete original after import)
 */
class SafImporter(
    private val context: Context,
    private val cryptoManager: CryptoManager
) {
    
    /**
     * Import mode
     */
    enum class ImportMode {
        IMPORT, // Keep original file
        MOVE    // Delete original file after import
    }
    
    /**
     * Import result
     */
    data class ImportResult(
        val item: VaultItem?,
        val deleteResult: DeleteResult
    )
    
    /**
     * Delete result
     */
    enum class DeleteResult {
        SUCCESS,           // Successfully deleted
        FAILED,           // Failed to delete
        NEED_USER_CONFIRM, // Need user confirmation (Android 11+)
        CANCELLED         // User cancelled deletion
    }
    
    /**
     * Import a file from SAF URI
     * @param uri The content:// URI from ACTION_OPEN_DOCUMENT
     * @param mode Import mode (IMPORT or MOVE)
     * @return ImportResult containing VaultItem and delete status
     */
    suspend fun importFile(uri: Uri, mode: ImportMode = ImportMode.MOVE): ImportResult = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            
            // Take persistable URI permission if possible
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Not all URIs support persistable permissions, continue anyway
            }
            
            // Get file info
            val fileName = getFileName(contentResolver, uri) ?: "unknown"
            val fileSize = getFileSize(contentResolver, uri)
            val mimeType = contentResolver.getType(uri) ?: ""
            
            // Determine item type
            val itemType = when {
                mimeType.startsWith("image/") -> ItemType.PHOTO
                mimeType.startsWith("video/") -> ItemType.VIDEO
                else -> ItemType.FILE
            }
            
            // Read file content using stream encryption
            val inputStream = contentResolver.openInputStream(uri)
                ?: return@withContext ImportResult(null, DeleteResult.FAILED)
            
            // Create encrypted file using stream encryption
            val vaultDir = File(context.filesDir, "vault")
            if (!vaultDir.exists()) {
                vaultDir.mkdirs()
            }
            
            val encryptedFile = File(vaultDir, "${System.currentTimeMillis()}_${fileName}.vault")
            
            try {
                cryptoManager.encryptStreamToFile(inputStream, encryptedFile)
                inputStream.close()
            } catch (e: Exception) {
                inputStream.close()
                throw e
            }
            
            // Create VaultItem
            val item = VaultItem(
                type = itemType,
                displayName = fileName,
                mimeType = mimeType,
                encryptedPath = encryptedFile.absolutePath,
                size = fileSize,
                createdAt = System.currentTimeMillis()
            )
            
            // Delete source file if mode is MOVE
            val deleteResult = if (mode == ImportMode.MOVE) {
                deleteSourceFile(uri)
            } else {
                DeleteResult.SUCCESS // No deletion needed for IMPORT mode
            }
            
            ImportResult(item, deleteResult)
        } catch (e: Exception) {
            e.printStackTrace()
            ImportResult(null, DeleteResult.FAILED)
        }
    }
    
    /**
     * Delete source file with fallback strategies
     * @param sourceUri The URI of the source file to delete
     * @return DeleteResult
     */
    private fun deleteSourceFile(sourceUri: Uri): DeleteResult {
        val contentResolver = context.contentResolver
        
        // Strategy 1: Try contentResolver.delete
        try {
            val deleted = contentResolver.delete(sourceUri, null, null)
            if (deleted > 0) {
                return DeleteResult.SUCCESS
            }
        } catch (e: Exception) {
            // Continue to next strategy
        }
        
        // Strategy 2: Try DocumentsContract.deleteDocument
        try {
            if (DocumentsContract.isDocumentUri(context, sourceUri)) {
                if (DocumentsContract.deleteDocument(contentResolver, sourceUri)) {
                    return DeleteResult.SUCCESS
                }
            }
        } catch (e: Exception) {
            // Continue to next strategy
        }
        
        // Strategy 3: For Android 11+ MediaStore, need user confirmation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                // Check if it's a MediaStore URI
                if (sourceUri.scheme == "content" && 
                    (sourceUri.authority == MediaStore.AUTHORITY || 
                     sourceUri.toString().contains("media"))) {
                    return DeleteResult.NEED_USER_CONFIRM
                }
            } catch (e: Exception) {
                // Fall through
            }
        }
        
        return DeleteResult.FAILED
    }
    
    /**
     * Create delete request for Android 11+ MediaStore files
     * @param sourceUri The URI to delete
     * @return IntentSender for user confirmation, or null if not needed
     */
    fun createDeleteRequest(sourceUri: Uri): android.content.IntentSender? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val pendingIntent = MediaStore.createDeleteRequest(
                    context.contentResolver,
                    listOf(sourceUri)
                )
                return pendingIntent.intentSender
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }
    
    /**
     * Get file name from URI
     */
    private fun getFileName(contentResolver: ContentResolver, uri: Uri): String? {
        var name: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }
    
    /**
     * Get file size from URI
     */
    private fun getFileSize(contentResolver: ContentResolver, uri: Uri): Long {
        var size: Long = 0
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0) {
                    size = it.getLong(sizeIndex)
                }
            }
        }
        return size
    }
}
