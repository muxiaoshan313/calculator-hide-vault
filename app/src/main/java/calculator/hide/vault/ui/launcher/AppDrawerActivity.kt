package calculator.hide.vault.ui.launcher

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import calculator.hide.vault.R

class AppDrawerActivity : AppCompatActivity() {

    private val viewModel: HomeLauncherViewModel by viewModels()
    private lateinit var adapter: AppListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_app_drawer)

        val rv = findViewById<RecyclerView>(R.id.rvDrawerApps)
        val etSearch = findViewById<EditText>(R.id.etDrawerSearch)

        adapter = AppListAdapter(
            loadIcon = { app, callback -> viewModel.loadIconAsync(app, callback) },
            onItemClick = { item -> viewModel.launchApp(item.app) },
            onItemLongClick = { item ->
                showDrawerMenu(item)
                true
            }
        )
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s?.toString().orEmpty())
            }
        })

        viewModel.drawerApps.observe(this) { adapter.submitList(it) }
        viewModel.toastMessage.observe(this) { msg ->
            msg?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearToast()
            }
        }

        viewModel.refresh()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun showDrawerMenu(item: AppListItemUi) {
        AppActionHelper.showDrawerAppMenu(
            activity = this,
            app = item.app,
            onAddDock = { viewModel.addToDock(item.app) },
            onMoveToPrivate = { viewModel.moveToPrivateSpace(item.app) }
        )
    }
}
