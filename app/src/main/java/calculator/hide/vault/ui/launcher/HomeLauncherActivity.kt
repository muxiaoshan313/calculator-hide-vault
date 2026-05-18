package calculator.hide.vault.ui.launcher

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import calculator.hide.vault.data.launcher.LauncherItemType
import calculator.hide.vault.launcher.repository.WallpaperRepository
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import calculator.hide.vault.R
import calculator.hide.vault.ui.auth.PinVerifyActivity
import calculator.hide.vault.ui.auth.PrivateSpaceAuthManager

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
        val btnSetDefaultHome = findViewById<Button>(R.id.btnSetDefaultHome)

        updateDefaultHomeButton(btnSetDefaultHome)
        btnSetDefaultHome.setOnClickListener {
            LauncherHomeSettingsHelper.requestDefaultHome(this)
        }
        if (!LauncherHomeSettingsHelper.isDefaultHome(this)) {
            maybePromptSetDefaultHome()
        }

        pageAdapter = DesktopPageAdapter {
            DesktopGridAdapter(
                loadIcon = { app, callback -> viewModel.loadIconAsync(app, callback) },
                onAppClick = { cell -> cell.app?.let { viewModel.launchApp(it) } },
                onAppLongClick = { cell -> cell.app?.let { showDesktopMenu(cell) } }
            )
        }
        vpDesktop.adapter = pageAdapter
        vpDesktop.offscreenPageLimit = 1
        vpDesktop.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.setCurrentScreen(position)
                updatePageIndicator(pageIndicator, position)
            }
        })

        dockAdapter = DockAdapter(
            loadIcon = { app, callback -> viewModel.loadIconAsync(app, callback) },
            onSlotClick = { slot ->
                when (slot.itemType) {
                    LauncherItemType.ALL_APPS_ENTRY ->
                        startActivity(Intent(this, AppDrawerActivity::class.java))
                    LauncherItemType.PRIVATE_SPACE_ENTRY -> openPrivateSpace()
                    else -> slot.app?.let { viewModel.launchApp(it) }
                }
            },
            onAppLongClick = { slot -> slot.app?.let { showDockMenu(slot) } }
        )
        rvDock.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvDock.adapter = dockAdapter

        etSearch.setOnClickListener {
            startActivity(Intent(this, AppDrawerActivity::class.java))
        }

        findViewById<TextView>(R.id.tvCompliance).apply {
            text = getString(R.string.launcher_compliance_notice)
            setOnLongClickListener {
                startActivity(Intent(this@HomeLauncherActivity, LauncherSettingsActivity::class.java))
                true
            }
        }

        val emptyHint = findViewById<TextView>(R.id.tvEmptyDesktopHint)
        viewModel.desktopPages.observe(this) { pages ->
            pageAdapter.submitPages(pages)
            setupPageIndicator(pageIndicator, pages.size)
            val hasDesktopApps = pages.any { page -> page.any { it.app != null } }
            emptyHint.visibility = if (hasDesktopApps) View.GONE else View.VISIBLE
        }
        viewModel.dockSlots.observe(this) { dockAdapter.submitList(it) }
        viewModel.currentScreen.observe(this) { screen ->
            if (vpDesktop.currentItem != screen && screen < pageAdapter.itemCount) {
                vpDesktop.setCurrentItem(screen, false)
            }
        }
        viewModel.toastMessage.observe(this) { msg ->
            msg?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearToast()
            }
        }
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
        findViewById<Button>(R.id.btnSetDefaultHome)?.let { updateDefaultHomeButton(it) }
        loadWallpaper()
        viewModel.refresh()
    }

    private fun loadWallpaper() {
        val iv = findViewById<ImageView>(R.id.ivWallpaper) ?: return
        val wallpaper = WallpaperRepository(this).loadHomeWallpaper()
        if (wallpaper != null) {
            iv.setImageDrawable(wallpaper)
        } else {
            iv.setImageResource(android.R.color.black)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LauncherHomeSettingsHelper.REQUEST_DEFAULT_HOME) {
            LauncherHomeSettingsHelper.onDefaultHomeResult(this, resultCode)
            findViewById<Button>(R.id.btnSetDefaultHome)?.let { updateDefaultHomeButton(it) }
        }
    }

    private fun updateDefaultHomeButton(button: Button) {
        val isDefault = LauncherHomeSettingsHelper.isDefaultHome(this)
        button.visibility = if (isDefault) View.GONE else View.VISIBLE
    }

    private fun maybePromptSetDefaultHome() {
        val prefs = getSharedPreferences(PREFS_LAUNCHER, MODE_PRIVATE)
        if (prefs.getBoolean(KEY_PROMPTED_DEFAULT_HOME, false)) return
        prefs.edit().putBoolean(KEY_PROMPTED_DEFAULT_HOME, true).apply()
        AlertDialog.Builder(this)
            .setTitle(R.string.set_default_home)
            .setMessage(R.string.set_default_home_prompt)
            .setPositiveButton(R.string.set_default_home) { _, _ ->
                LauncherHomeSettingsHelper.requestDefaultHome(this)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
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
            onMoveToPrivate = { viewModel.moveToPrivateSpace(app) },
            onAddToDock = { viewModel.addToDock(app) }
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

    companion object {
        private const val PREFS_LAUNCHER = "launcher_prefs"
        private const val KEY_PROMPTED_DEFAULT_HOME = "prompted_default_home"
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
