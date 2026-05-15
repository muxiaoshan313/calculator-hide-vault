package calculator.hide.vaultpro.ui.launcher

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import calculator.hide.vaultpro.R
import calculator.hide.vaultpro.ui.auth.PinVerifyActivity
import calculator.hide.vaultpro.ui.auth.PrivateSpaceAuthManager

class HomeLauncherActivity : AppCompatActivity() {

    private val viewModel: HomeLauncherViewModel by viewModels()
    private lateinit var pageAdapter: DesktopPageAdapter
    private lateinit var dockAdapter: DockAdapter
    private var packageMonitor: LauncherPackageMonitor? = null

    private val privateSpaceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startActivity(Intent(this, PrivateDesktopActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_home_launcher)

        val vpDesktop = findViewById<ViewPager2>(R.id.vpDesktop)
        val pageIndicator = findViewById<LinearLayout>(R.id.pageIndicator)
        val rvDock = findViewById<RecyclerView>(R.id.rvDock)
        val etSearch = findViewById<EditText>(R.id.etHomeSearch)

        pageAdapter = DesktopPageAdapter {
            DesktopGridAdapter(
                onAppClick = { cell -> cell.app?.let { viewModel.launchApp(it) } },
                onAppLongClick = { cell -> cell.app?.let { showDesktopMenu(cell) } }
            )
        }
        vpDesktop.adapter = pageAdapter
        vpDesktop.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.setCurrentScreen(position)
                updatePageIndicator(pageIndicator, position)
            }
        })

        dockAdapter = DockAdapter(
            onAppClick = { slot -> slot.app?.let { viewModel.launchApp(it) } },
            onAppLongClick = { slot -> slot.app?.let { showDockMenu(slot) } }
        )
        rvDock.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvDock.adapter = dockAdapter

        etSearch.setOnClickListener {
            startActivity(Intent(this, AppDrawerActivity::class.java))
        }

        findViewById<ImageButton>(R.id.btnAllApps).setOnClickListener {
            startActivity(Intent(this, AppDrawerActivity::class.java))
        }

        findViewById<ImageButton>(R.id.btnPrivateSpace).setOnClickListener {
            openPrivateSpace()
        }

        findViewById<TextView>(R.id.tvCompliance).apply {
            text = getString(R.string.launcher_compliance_notice)
            setOnLongClickListener {
                startActivity(Intent(this@HomeLauncherActivity, LauncherSettingsActivity::class.java))
                true
            }
        }

        viewModel.desktopPages.observe(this) { pages ->
            pageAdapter.submitPages(pages)
            setupPageIndicator(pageIndicator, pages.size)
        }
        viewModel.dockSlots.observe(this) { dockAdapter.submitList(it) }
        viewModel.currentScreen.observe(this) { screen ->
            if (vpDesktop.currentItem != screen) vpDesktop.setCurrentItem(screen, false)
        }
        viewModel.toastMessage.observe(this) { msg ->
            msg?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearToast()
            }
        }

        viewModel.refresh()
    }

    override fun onStart() {
        super.onStart()
        packageMonitor = LauncherPackageMonitor(this) { viewModel.refresh() }.also { it.register() }
    }

    override fun onStop() {
        packageMonitor?.unregister()
        packageMonitor = null
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun openPrivateSpace() {
        val auth = PrivateSpaceAuthManager.getInstance(this)
        if (auth.isSessionValid()) {
            startActivity(Intent(this, PrivateDesktopActivity::class.java))
        } else {
            privateSpaceLauncher.launch(PinVerifyActivity.createIntent(this))
        }
    }

    private fun showDesktopMenu(cell: DesktopCellUi) {
        val app = cell.app ?: return
        AppActionHelper.showDesktopAppMenu(
            activity = this,
            app = app,
            onRemoveFromDesktop = { viewModel.removeFromDesktop(cell.desktopId) },
            onMoveToPrivate = { viewModel.moveToPrivateSpace(app) }
        )
    }

    private fun showDockMenu(slot: DockSlotUi) {
        val app = slot.app ?: return
        AppActionHelper.showDockAppMenu(
            activity = this,
            app = app,
            onRemoveFromDock = { viewModel.removeFromDock(app) },
            onMoveToPrivate = { viewModel.moveToPrivateSpace(app) }
        )
    }

    private fun setupPageIndicator(container: LinearLayout, count: Int) {
        container.removeAllViews()
        repeat(count) { index ->
            val dot = TextView(this).apply {
                text = "●"
                setTextColor(
                    ContextCompat.getColor(
                        this@HomeLauncherActivity,
                        if (index == 0) android.R.color.white else android.R.color.darker_gray
                    )
                )
                setPadding(8, 0, 8, 0)
            }
            container.addView(dot)
        }
    }

    private fun updatePageIndicator(container: LinearLayout, selected: Int) {
        for (i in 0 until container.childCount) {
            (container.getChildAt(i) as? TextView)?.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (i == selected) android.R.color.white else android.R.color.darker_gray
                )
            )
        }
    }
}
