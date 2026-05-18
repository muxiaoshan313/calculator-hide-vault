package calculator.hide.vault.ui.launcher

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import calculator.hide.vault.R
import calculator.hide.vault.activities.CalculatorActivity
import calculator.hide.vault.ui.auth.PinVerifyActivity
import calculator.hide.vault.ui.auth.PrivateSpaceAuthManager

class PrivateAppManagerActivity : AppCompatActivity() {

    private val viewModel: HomeLauncherViewModel by viewModels()
    private lateinit var adapter: ManagerAppAdapter
    private var contentShown = false
    private var managerSearch = ""

    private val pinLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) showContent()
        else finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        if (savedInstanceState != null) {
            contentShown = savedInstanceState.getBoolean(KEY_CONTENT, false)
        }

        val auth = PrivateSpaceAuthManager.getInstance(this)
        if (auth.isSessionValid() || contentShown) showContent()
        else pinLauncher.launch(PinVerifyActivity.createIntent(this))
    }

    private fun showContent() {
        if (contentShown && ::adapter.isInitialized) return
        contentShown = true
        setContentView(R.layout.activity_app_manager)

        val rv = findViewById<RecyclerView>(R.id.rvManagerApps)
        val etSearch = findViewById<EditText>(R.id.etManagerSearch)

        adapter = ManagerAppAdapter(
            loadIcon = { app, callback -> viewModel.loadIconAsync(app, callback) },
            onItemClick = { item -> viewModel.toggleHidden(item.app) }
        )
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                managerSearch = s?.toString().orEmpty().trim()
                filterManagerList()
            }
        })

        viewModel.managerApps.observe(this) { filterManagerList() }

        findViewById<Button>(R.id.btnClearAll).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.clear_all_hidden_title)
                .setMessage(R.string.clear_all_hidden_message)
                .setPositiveButton(R.string.remove_all) { _, _ -> viewModel.clearAllHidden() }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        findViewById<Button>(R.id.btnBackCalculator).setOnClickListener {
            startActivity(
                Intent(this, CalculatorActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            )
            finish()
        }

        viewModel.refresh()
    }

    private fun filterManagerList() {
        val list = viewModel.managerApps.value.orEmpty()
        val filtered = if (managerSearch.isBlank()) list else {
            val q = managerSearch.lowercase()
            list.filter {
                it.app.label.lowercase().contains(q) ||
                    it.app.packageName.lowercase().contains(q)
            }
        }
        adapter.submitList(filtered)
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
