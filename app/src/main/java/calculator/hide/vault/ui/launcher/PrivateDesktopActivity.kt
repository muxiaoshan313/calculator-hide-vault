package calculator.hide.vault.ui.launcher

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import calculator.hide.vault.R
import calculator.hide.vault.activities.CalculatorActivity
import calculator.hide.vault.data.launcher.LauncherConstants
import calculator.hide.vault.ui.auth.PinVerifyActivity
import calculator.hide.vault.ui.auth.PrivateSpaceAuthManager

class PrivateDesktopActivity : AppCompatActivity() {

    private val viewModel: HomeLauncherViewModel by viewModels()
    private lateinit var adapter: AppListAdapter
    private var contentShown = false

    private val pinLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            showContent()
        } else {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        if (savedInstanceState != null) {
            contentShown = savedInstanceState.getBoolean(KEY_CONTENT, false)
        }

        val auth = PrivateSpaceAuthManager.getInstance(this)
        if (auth.isSessionValid() || contentShown) {
            showContent()
        } else {
            pinLauncher.launch(PinVerifyActivity.createIntent(this))
        }
    }

    private fun showContent() {
        if (contentShown && ::adapter.isInitialized) return
        contentShown = true
        maybeShowPrivateSpaceDisclaimer()
        setContentView(R.layout.activity_private_desktop)

        val rv = findViewById<RecyclerView>(R.id.rvPrivateGrid)
        adapter = AppListAdapter(
            loadIcon = { app, callback -> viewModel.loadIconAsync(app, callback) },
            onItemClick = { item -> viewModel.launchApp(item.app) },
            onItemLongClick = { item ->
                AppActionHelper.showPrivateAppMenu(
                    activity = this,
                    app = item.app,
                    onRemoveFromPrivate = { viewModel.removeFromPrivateSpace(item.app) }
                )
                true
            }
        )
        rv.layoutManager = GridLayoutManager(this, LauncherConstants.GRID_COLUMNS)
        rv.adapter = adapter

        viewModel.privateApps.observe(this) { adapter.submitList(it) }

        findViewById<Button>(R.id.btnExitPrivate).setOnClickListener {
            PrivateSpaceAuthManager.getInstance(this).invalidateSession()
            startActivity(
                Intent(this, CalculatorActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            )
            finish()
        }

        viewModel.refresh()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_CONTENT, contentShown)
    }

    override fun onResume() {
        super.onResume()
        if (contentShown) viewModel.refresh()
    }

    private fun maybeShowPrivateSpaceDisclaimer() {
        val prefs = getSharedPreferences(PREFS_LAUNCHER, MODE_PRIVATE)
        if (prefs.getBoolean(KEY_DISCLAIMER_SHOWN, false)) return
        prefs.edit().putBoolean(KEY_DISCLAIMER_SHOWN, true).apply()
        AlertDialog.Builder(this)
            .setTitle(R.string.private_space_disclaimer_title)
            .setMessage(R.string.private_space_disclaimer_message)
            .setPositiveButton(R.string.private_space_disclaimer_understand, null)
            .setNegativeButton(android.R.string.cancel) { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    companion object {
        private const val KEY_CONTENT = "content_shown"
        private const val PREFS_LAUNCHER = "launcher_prefs"
        private const val KEY_DISCLAIMER_SHOWN = "private_space_disclaimer_shown"
    }
}
