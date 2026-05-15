package calculator.hide.vaultpro.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import calculator.hide.vaultpro.R
import calculator.hide.vaultpro.data.ItemType
import calculator.hide.vaultpro.data.VaultDatabase
import calculator.hide.vaultpro.data.VaultItem
import calculator.hide.vaultpro.import.SafImporter
import calculator.hide.vaultpro.security.CryptoManager
import kotlinx.coroutines.launch

/**
 * Fragment for displaying videos
 */
class VideosFragment : Fragment() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VaultItemAdapter
    private lateinit var database: VaultDatabase
    private lateinit var safImporter: SafImporter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_videos, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        database = VaultDatabase.getDatabase(requireContext())
        safImporter = SafImporter(requireContext(), CryptoManager(requireContext()))
        
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        
        adapter = VaultItemAdapter(ItemType.VIDEO, true,
            onItemClick = { item ->
                openPreview(item)
            },
            onExportClick = { item ->
                exportItem(item)
            }
        )
        recyclerView.adapter = adapter
        
        lifecycleScope.launch {
            database.vaultDao().getItemsByType(ItemType.VIDEO).collect { items ->
                adapter.submitList(items)
                view.findViewById<android.widget.TextView>(R.id.tvEmpty)?.visibility = 
                    if (items.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
    
    fun importFile(uri: Uri, mode: calculator.hide.vaultpro.import.SafImporter.ImportMode) {
        lifecycleScope.launch {
            try {
                val result = safImporter.importFile(uri, mode)
                if (result.item != null) {
                    val item = result.item
                    if (item.type == ItemType.VIDEO) {
                        database.vaultDao().insert(item)
                        
                        when (result.deleteResult) {
                            calculator.hide.vaultpro.import.SafImporter.DeleteResult.SUCCESS -> {
                                android.widget.Toast.makeText(
                                    requireContext(),
                                    "Moved to vault (original file deleted)",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                            calculator.hide.vaultpro.import.SafImporter.DeleteResult.NEED_USER_CONFIRM -> {
                                (requireActivity() as? calculator.hide.vaultpro.activities.VaultActivity)?.requestDeleteConfirmation(uri)
                            }
                            else -> {
                                android.widget.Toast.makeText(
                                    requireContext(),
                                    "Imported to vault, but original file was not deleted",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } else {
                        android.widget.Toast.makeText(
                            requireContext(),
                            "Please import a video",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Failed to import video",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(
                    requireContext(),
                    "Error: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    fun handleDeleteConfirmed(uri: Uri) {
        android.widget.Toast.makeText(
            requireContext(),
            "Moved to vault (original file deleted)",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    
    fun exportItem(item: VaultItem) {
        lifecycleScope.launch {
            try {
                val exportManager = calculator.hide.vaultpro.export.ExportManager(requireContext(), calculator.hide.vaultpro.security.CryptoManager(requireContext()))
                val result = exportManager.exportItem(item)
                
                if (result.success) {
                    androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Export Successful")
                        .setMessage("File exported successfully. Delete from vault?")
                        .setPositiveButton("Delete") { _, _ ->
                            lifecycleScope.launch {
                                val encryptedFile = java.io.File(item.encryptedPath)
                                encryptedFile.delete()
                                database.vaultDao().delete(item)
                                android.widget.Toast.makeText(
                                    requireContext(),
                                    "Exported and removed from vault",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        .setNegativeButton("Keep") { _, _ ->
                            android.widget.Toast.makeText(
                                requireContext(),
                                "Exported (kept in vault)",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                        .show()
                } else {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Export failed: ${result.errorMessage}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(
                    requireContext(),
                    "Export error: ${e.message}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun openPreview(item: VaultItem) {
        val intent = android.content.Intent(requireContext(), calculator.hide.vaultpro.activities.PreviewActivity::class.java).apply {
            putExtra("item_id", item.id)
            putExtra("item_type", item.type.name)
            putExtra("display_name", item.displayName)
            putExtra("mime_type", item.mimeType)
            putExtra("encrypted_path", item.encryptedPath)
        }
        startActivity(intent)
    }
}

