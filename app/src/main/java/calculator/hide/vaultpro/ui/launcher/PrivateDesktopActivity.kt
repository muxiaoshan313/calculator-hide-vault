package calculator.hide.vaultpro.ui.launcher

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import calculator.hide.vaultpro.R
import calculator.hide.vaultpro.activities.CalculatorActivity
import calculator.hide.vaultpro.data.launcher.LauncherConstants
import calculator.hide.vaultpro.ui.auth.PinVerifyActivity
import calculator.hide.vaultpro.ui.auth.PrivateSpaceAuthManager

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

    companion object {
        private const val KEY_CONTENT = "content_shown"
    }
}
