package calculator.hide.vault.fragments

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import calculator.hide.vault.R
import calculator.hide.vault.data.ItemType
import calculator.hide.vault.data.VaultItem
import calculator.hide.vault.security.CryptoManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * RecyclerView adapter for VaultItem
 * Supports both list view (for files) and grid view with thumbnails (for photos/videos)
 */
class VaultItemAdapter(
    private val itemType: ItemType,
    private val showThumbnail: Boolean,
    private val onItemClick: (VaultItem) -> Unit,
    private val onExportClick: ((VaultItem) -> Unit)? = null
) : ListAdapter<VaultItem, RecyclerView.ViewHolder>(VaultItemDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (showThumbnail) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_vault_media, parent, false)
            MediaViewHolder(view, parent.context)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_vault_file, parent, false)
            FileViewHolder(view)
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is MediaViewHolder -> holder.bind(getItem(position))
            is FileViewHolder -> holder.bind(getItem(position))
        }
    }
    
    inner class MediaViewHolder(itemView: View, private val context: android.content.Context) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageViewThumbnail)
        private val videoIndicator: ImageView = itemView.findViewById(R.id.ivVideoIndicator)
        private val exportButton: android.widget.ImageButton = itemView.findViewById(R.id.btnExport)
        private val cryptoManager = CryptoManager(context)
        private var currentItemId: Long = -1
        private var loadJob: kotlinx.coroutines.Job? = null
        
        fun bind(item: VaultItem) {
            // Cancel previous load job if any
            loadJob?.cancel()
            
            // Store current item ID to check if this ViewHolder is still bound to the same item
            currentItemId = item.id
            
            // Show video indicator for videos
            videoIndicator.visibility = if (item.type == ItemType.VIDEO) View.VISIBLE else View.GONE
            
            // Show export button
            exportButton.visibility = View.VISIBLE
            exportButton.setOnClickListener {
                onExportClick?.invoke(item)
            }
            
            // Clear previous image
            imageView.setImageBitmap(null)
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
            
            // Load thumbnail asynchronously
            loadJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                try {
                    android.util.Log.d("VaultItemAdapter", "Loading thumbnail for item: ${item.id}, path: ${item.encryptedPath}")
                    
                    val encryptedFile = File(item.encryptedPath)
                    if (!encryptedFile.exists()) {
                        android.util.Log.e("VaultItemAdapter", "Encrypted file does not exist: ${item.encryptedPath}")
                        withContext(Dispatchers.Main) {
                            // Check if this ViewHolder is still bound to the same item
                            if (currentItemId == item.id && adapterPosition != RecyclerView.NO_POSITION) {
                                imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                            }
                        }
                        return@launch
                    }
                    
                    android.util.Log.d("VaultItemAdapter", "Decrypting file...")
                    val decryptedData = cryptoManager.decrypt(encryptedFile)
                    if (decryptedData.isEmpty()) {
                        android.util.Log.e("VaultItemAdapter", "Decrypted data is empty")
                        withContext(Dispatchers.Main) {
                            if (currentItemId == item.id && adapterPosition != RecyclerView.NO_POSITION) {
                                imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                            }
                        }
                        return@launch
                    }
                    
                    android.util.Log.d("VaultItemAdapter", "Decoded ${decryptedData.size} bytes, decoding bitmap...")
                    val bitmap = BitmapFactory.decodeByteArray(decryptedData, 0, decryptedData.size)
                    if (bitmap == null) {
                        android.util.Log.e("VaultItemAdapter", "Failed to decode bitmap")
                        withContext(Dispatchers.Main) {
                            if (currentItemId == item.id && adapterPosition != RecyclerView.NO_POSITION) {
                                imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                            }
                        }
                        return@launch
                    }
                    
                    android.util.Log.d("VaultItemAdapter", "Bitmap decoded: ${bitmap.width}x${bitmap.height}")
                    
                    // Create thumbnail (square, maintain aspect ratio)
                    val size = minOf(bitmap.width, bitmap.height)
                    val x = (bitmap.width - size) / 2
                    val y = (bitmap.height - size) / 2
                    val cropped = android.graphics.Bitmap.createBitmap(bitmap, x, y, size, size)
                    val thumbnail = android.graphics.Bitmap.createScaledBitmap(
                        cropped,
                        400,
                        400,
                        true
                    )
                    if (cropped != bitmap) {
                        cropped.recycle()
                    }
                    bitmap.recycle()
                    
                    android.util.Log.d("VaultItemAdapter", "Thumbnail created, updating UI...")
                    withContext(Dispatchers.Main) {
                        // Check if this ViewHolder is still bound to the same item before updating UI
                        if (currentItemId == item.id && adapterPosition != RecyclerView.NO_POSITION) {
                            imageView.setImageBitmap(thumbnail)
                            android.util.Log.d("VaultItemAdapter", "Thumbnail set successfully")
                        } else {
                            android.util.Log.d("VaultItemAdapter", "ViewHolder recycled, discarding thumbnail")
                            // ViewHolder was recycled, recycle the bitmap
                            thumbnail.recycle()
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VaultItemAdapter", "Error loading thumbnail", e)
                    e.printStackTrace()
                    // Show placeholder on error
                    withContext(Dispatchers.Main) {
                        if (currentItemId == item.id && adapterPosition != RecyclerView.NO_POSITION) {
                            imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                        }
                    }
                }
            }
            
            itemView.setOnClickListener {
                onItemClick(item)
            }
            
            // Long press for export menu
            itemView.setOnLongClickListener {
                showExportMenu(itemView, item)
                true
            }
        }
        
        private fun showExportMenu(view: View, item: VaultItem) {
            val popup = android.widget.PopupMenu(view.context, view)
            popup.menu.add("Export")
            popup.setOnMenuItemClickListener {
                if (it.title == "Export") {
                    onExportClick?.invoke(item)
                }
                true
            }
            popup.show()
        }
    }
    
    inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.tvFileName)
        private val sizeTextView: TextView = itemView.findViewById(R.id.tvFileSize)
        private val dateTextView: TextView = itemView.findViewById(R.id.tvFileDate)
        
        fun bind(item: VaultItem) {
            nameTextView.text = item.displayName
            sizeTextView.text = formatFileSize(item.size)
            dateTextView.text = formatDate(item.createdAt)
            
            itemView.setOnClickListener {
                onItemClick(item)
            }
            
            // Long press for export menu
            itemView.setOnLongClickListener {
                showExportMenu(itemView, item)
                true
            }
        }
        
        private fun showExportMenu(view: View, item: VaultItem) {
            val popup = android.widget.PopupMenu(view.context, view)
            popup.menu.add("Export")
            popup.setOnMenuItemClickListener {
                if (it.title == "Export") {
                    onExportClick?.invoke(item)
                }
                true
            }
            popup.show()
        }
        
        private fun formatFileSize(bytes: Long): String {
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            
            return when {
                gb >= 1 -> String.format("%.2f GB", gb)
                mb >= 1 -> String.format("%.2f MB", mb)
                kb >= 1 -> String.format("%.2f KB", kb)
                else -> "$bytes B"
            }
        }
        
        private fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }
    
    private class VaultItemDiffCallback : DiffUtil.ItemCallback<VaultItem>() {
        override fun areItemsTheSame(oldItem: VaultItem, newItem: VaultItem): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: VaultItem, newItem: VaultItem): Boolean {
            return oldItem == newItem
        }
    }
}

