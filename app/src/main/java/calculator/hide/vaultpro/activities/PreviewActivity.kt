package calculator.hide.vaultpro.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import calculator.hide.vaultpro.R
import calculator.hide.vaultpro.data.ItemType
import calculator.hide.vaultpro.data.VaultItem
import calculator.hide.vaultpro.security.CryptoManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Preview Activity - Shows decrypted content
 * For photos: ImageView preview
 * For videos/files: Decrypt to temp file and open with system Intent
 */
class PreviewActivity : AppCompatActivity() {
    
    private lateinit var imageView: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var textView: TextView
    private lateinit var cryptoManager: CryptoManager
    private var vaultItem: VaultItem? = null
    private var tempFile: File? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Hide system action bar
        supportActionBar?.hide()
        
        // Prevent screenshots
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        
        setContentView(R.layout.activity_preview)
        
        imageView = findViewById(R.id.imageView)
        progressBar = findViewById(R.id.progressBar)
        textView = findViewById(R.id.tvFileName)
        
        cryptoManager = CryptoManager(this)
        
        val itemId = intent.getLongExtra("item_id", -1)
        if (itemId == -1L) {
            finish()
            return
        }
        
        // Load item from database (simplified - in real app, use ViewModel)
        CoroutineScope(Dispatchers.Main).launch {
            loadAndPreview(itemId)
        }
    }
    
    private suspend fun loadAndPreview(itemId: Long) = withContext(Dispatchers.IO) {
        // TODO: Load from database via ViewModel
        // For now, get from intent extras
        val type = ItemType.valueOf(intent.getStringExtra("item_type") ?: "FILE")
        val displayName = intent.getStringExtra("display_name") ?: "Unknown"
        val encryptedPath = intent.getStringExtra("encrypted_path") ?: ""
        val mimeType = intent.getStringExtra("mime_type") ?: ""
        
        vaultItem = VaultItem(
            id = itemId,
            type = type,
            displayName = displayName,
            mimeType = mimeType,
            encryptedPath = encryptedPath,
            size = 0,
            createdAt = 0
        )
        
        withContext(Dispatchers.Main) {
            textView.text = displayName
            previewItem(vaultItem!!)
        }
    }
    
    private fun previewItem(item: VaultItem) {
        progressBar.visibility = View.VISIBLE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val encryptedFile = File(item.encryptedPath)
                if (!encryptedFile.exists()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PreviewActivity, "File not found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    return@launch
                }
                
                when (item.type) {
                    ItemType.PHOTO -> {
                        // Decrypt and show in ImageView (for photos, still use memory-based for preview)
                        val decryptedData = cryptoManager.decrypt(encryptedFile)
                        withContext(Dispatchers.Main) {
                            val bitmap = android.graphics.BitmapFactory.decodeByteArray(decryptedData, 0, decryptedData.size)
                            imageView.setImageBitmap(bitmap)
                            imageView.visibility = View.VISIBLE
                            progressBar.visibility = View.GONE
                        }
                    }
                    ItemType.VIDEO, ItemType.FILE -> {
                        // Decrypt to temp file using stream decryption and open with system Intent
                        val temp = File.createTempFile("vault_preview_", null, this@PreviewActivity.cacheDir)
                        tempFile = temp
                        FileOutputStream(temp).use { outputStream ->
                            cryptoManager.decryptFileToOutputStream(encryptedFile, outputStream)
                        }
                        
                        withContext(Dispatchers.Main) {
                            // Show warning dialog
                            AlertDialog.Builder(this@PreviewActivity)
                                .setTitle("Security Warning")
                                .setMessage("Opening this file will create a temporary decrypted copy. It will be deleted when you close the app.")
                                .setPositiveButton("Open") { _, _ ->
                                    openFileWithSystemIntent(temp)
                                }
                                .setNegativeButton("Cancel") { _, _ ->
                                    temp.delete()
                                    finish()
                                }
                                .setOnDismissListener {
                                    progressBar.visibility = View.GONE
                                }
                                .show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PreviewActivity, "Failed to decrypt: ${e.message}", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                }
            }
        }
    }
    
    private fun openFileWithSystemIntent(file: File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, contentResolver.getType(uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No app to open this file", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up temp file
        tempFile?.delete()
    }
}

