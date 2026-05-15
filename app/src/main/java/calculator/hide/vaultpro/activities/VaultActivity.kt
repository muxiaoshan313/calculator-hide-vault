package calculator.hide.vaultpro.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import calculator.hide.vaultpro.R
import calculator.hide.vaultpro.fragments.FilesFragment
import calculator.hide.vaultpro.fragments.PhotosFragment
import calculator.hide.vaultpro.fragments.VideosFragment
import calculator.hide.vaultpro.security.PinManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

/**
 * Vault Activity - Private space with tabs for Photos/Videos/Files
 * Features:
 * - FLAG_SECURE to prevent screenshots
 * - Auto-lock after 10 seconds in background
 * - FAB to import files
 */
class VaultActivity : AppCompatActivity() {
    
    companion object {
        private const val REQUEST_CODE_IMPORT = 1001
        private const val REQUEST_CODE_DELETE = 1002
        private const val AUTO_LOCK_DELAY = 10000L // 10 seconds
    }
    
    private var pendingDeleteUri: Uri? = null
    private var pendingImportMode: calculator.hide.vaultpro.import.SafImporter.ImportMode = calculator.hide.vaultpro.import.SafImporter.ImportMode.MOVE
    
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var fab: FloatingActionButton
    private lateinit var lockHandler: Handler
    private var lockRunnable: Runnable? = null
    private var isLocked = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Hide system action bar
        supportActionBar?.hide()
        
        // Prevent screenshots and screen recording
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        
        setContentView(R.layout.activity_vault)
        
        // Setup custom title bar buttons
        findViewById<android.widget.ImageButton>(R.id.btnSettings).setOnClickListener {
            showSettingsDialog()
        }
        findViewById<android.widget.ImageButton>(R.id.btnLock).setOnClickListener {
            lock()
        }
        
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        fab = findViewById(R.id.fabImport)
        
        // Setup ViewPager with fragments
        viewPager.adapter = VaultPagerAdapter(this)
        
        // Connect TabLayout with ViewPager
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Photos"
                1 -> "Videos"
                2 -> "Files"
                else -> ""
            }
        }.attach()
        
        // FAB click - show import options
        fab.setOnClickListener {
            showImportDialog()
        }
        
        lockHandler = Handler(Looper.getMainLooper())
    }
    
    // Menu removed - using custom title bar buttons instead
    
    private fun showImportDialog() {
        val options = arrayOf("Import Photo", "Import Video", "Import File")
        AlertDialog.Builder(this)
            .setTitle("Import")
            .setItems(options) { _, which ->
                // Show import mode selection
                showImportModeDialog(which)
            }
            .show()
    }
    
    private fun showImportModeDialog(fileType: Int) {
        val modes = arrayOf("Move to Vault (delete original)", "Import (keep original)")
        var selectedModeIndex = 0 // Default: Move (index 0) - this should show filled circle
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Import Mode")
            .setSingleChoiceItems(modes, selectedModeIndex) { _, which ->
                selectedModeIndex = which
            }
            .setPositiveButton("OK") { _, _ ->
                val mode = if (selectedModeIndex == 0) {
                    calculator.hide.vaultpro.import.SafImporter.ImportMode.MOVE
                } else {
                    calculator.hide.vaultpro.import.SafImporter.ImportMode.IMPORT
                }
                pendingImportMode = mode
                
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    when (fileType) {
                        0 -> type = "image/*"
                        1 -> type = "video/*"
                        2 -> type = "*/*"
                    }
                    addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivityForResult(intent, REQUEST_CODE_IMPORT)
            }
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.show()
        
        // Explicitly set the checked state to ensure first item shows as selected (filled circle)
        val listView = dialog.listView
        if (listView != null) {
            listView.post {
                // Clear all selections first
                for (i in 0 until listView.adapter.count) {
                    listView.setItemChecked(i, false)
                }
                // Then set the first item as checked (this should show filled circle)
                listView.setItemChecked(selectedModeIndex, true)
                listView.setSelection(selectedModeIndex)
            }
        }
    }
    
    private fun showSettingsDialog() {
        val options = arrayOf("Change PIN", "Cancel")
        AlertDialog.Builder(this)
            .setTitle("Settings")
            .setItems(options) { _, which ->
                if (which == 0) {
                    showChangePinDialog()
                }
            }
            .show()
    }
    
    private fun showChangePinDialog() {
        val pinManager = PinManager(this)
        val view = layoutInflater.inflate(R.layout.dialog_change_pin, null)
        val etOldPin = view.findViewById<android.widget.EditText>(R.id.etOldPin)
        val etNewPin = view.findViewById<android.widget.EditText>(R.id.etNewPin)
        val etConfirmNewPin = view.findViewById<android.widget.EditText>(R.id.etConfirmNewPin)
        
        AlertDialog.Builder(this)
            .setTitle("Change PIN")
            .setView(view)
            .setPositiveButton("Change") { _, _ ->
                val oldPin = etOldPin.text.toString()
                val newPin = etNewPin.text.toString()
                val confirmPin = etConfirmNewPin.text.toString()
                
                if (newPin != confirmPin) {
                    android.widget.Toast.makeText(this, "New PINs do not match", android.widget.Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                if (newPin.length < 4 || newPin.length > 12) {
                    android.widget.Toast.makeText(this, "PIN must be 4-12 digits", android.widget.Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                if (pinManager.changePin(oldPin, newPin)) {
                    android.widget.Toast.makeText(this, "PIN changed successfully", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(this, "Failed to change PIN", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_CODE_IMPORT && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                // Import based on current tab position
                when (viewPager.currentItem) {
                    0 -> {
                        // Photos tab
                        val fragment = supportFragmentManager.fragments.find { it is PhotosFragment } as? PhotosFragment
                        fragment?.importFile(uri, pendingImportMode)
                    }
                    1 -> {
                        // Videos tab
                        val fragment = supportFragmentManager.fragments.find { it is VideosFragment } as? VideosFragment
                        fragment?.importFile(uri, pendingImportMode)
                    }
                    2 -> {
                        // Files tab
                        val fragment = supportFragmentManager.fragments.find { it is FilesFragment } as? FilesFragment
                        fragment?.importFile(uri, pendingImportMode)
                    }
                }
            }
        } else if (requestCode == REQUEST_CODE_DELETE && resultCode == RESULT_OK) {
            // User confirmed deletion
            pendingDeleteUri?.let { uri: Uri ->
                when (viewPager.currentItem) {
                    0 -> {
                        val fragment = supportFragmentManager.fragments.find { it is PhotosFragment } as? PhotosFragment
                        fragment?.handleDeleteConfirmed(uri)
                    }
                    1 -> {
                        val fragment = supportFragmentManager.fragments.find { it is VideosFragment } as? VideosFragment
                        fragment?.handleDeleteConfirmed(uri)
                    }
                    2 -> {
                        val fragment = supportFragmentManager.fragments.find { it is FilesFragment } as? FilesFragment
                        fragment?.handleDeleteConfirmed(uri)
                    }
                }
            }
            pendingDeleteUri = null
        } else if (requestCode == REQUEST_CODE_DELETE && resultCode != RESULT_OK) {
            // User cancelled deletion
            android.widget.Toast.makeText(
                this,
                "File imported but original file was not deleted",
                android.widget.Toast.LENGTH_LONG
            ).show()
            pendingDeleteUri = null
        }
    }
    
    fun requestDeleteConfirmation(uri: Uri) {
        pendingDeleteUri = uri
        val safImporter = calculator.hide.vaultpro.import.SafImporter(this, calculator.hide.vaultpro.security.CryptoManager(this))
        val intentSender = safImporter.createDeleteRequest(uri)
        
        if (intentSender != null) {
            try {
                startIntentSenderForResult(intentSender, REQUEST_CODE_DELETE, null, 0, 0, 0)
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(
                    this,
                    "File imported but original file was not deleted",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                pendingDeleteUri = null
            }
        } else {
            android.widget.Toast.makeText(
                this,
                "File imported but original file was not deleted",
                android.widget.Toast.LENGTH_LONG
            ).show()
            pendingDeleteUri = null
        }
    }
    
    override fun onStop() {
        super.onStop()
        // Start auto-lock timer
        lockRunnable = Runnable {
            if (!isFinishing && !isLocked) {
                lock()
            }
        }
        lockHandler.postDelayed(lockRunnable!!, AUTO_LOCK_DELAY)
    }
    
    override fun onStart() {
        super.onStart()
        // Cancel auto-lock timer
        lockRunnable?.let { lockHandler.removeCallbacks(it) }
        isLocked = false
    }
    
    private fun lock() {
        isLocked = true
        startActivity(Intent(this, UnlockActivity::class.java))
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        lockRunnable?.let { lockHandler.removeCallbacks(it) }
    }
    
    /**
     * ViewPager adapter for vault fragments
     */
    private class VaultPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 3
        
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> PhotosFragment()
                1 -> VideosFragment()
                2 -> FilesFragment()
                else -> PhotosFragment()
            }
        }
    }
}

