package calculator.hide.vaultpro.ui.launcher

import android.app.Application
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import calculator.hide.vaultpro.R
import calculator.hide.vaultpro.data.launcher.AppRepository
import calculator.hide.vaultpro.data.launcher.LaunchableApp
import calculator.hide.vaultpro.data.launcher.LauncherConstants
import calculator.hide.vaultpro.data.launcher.LauncherHomeData
import calculator.hide.vaultpro.data.launcher.LauncherIconLoader
import calculator.hide.vaultpro.data.launcher.LauncherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DesktopCellUi(
    val cellX: Int,
    val cellY: Int,
    val app: LaunchableApp?,
    val icon: Drawable? = null,
    val desktopId: Long? = null
)

data class DockSlotUi(
    val position: Int,
    val app: LaunchableApp?,
    val icon: Drawable?,
    val dockId: Long?
)

data class AppListItemUi(
    val app: LaunchableApp,
    val icon: Drawable?,
    val isHidden: Boolean = false
)

class HomeLauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LauncherRepository(application)
    private val iconLoader = LauncherIconLoader(AppRepository(application))

    private var homeData: LauncherHomeData? = null
    private var searchQuery: String = ""
    private var refreshJob: Job? = null

    private val _currentScreen = MutableLiveData(0)
    val currentScreen: LiveData<Int> = _currentScreen

    private val _desktopPages = MutableLiveData<List<List<DesktopCellUi>>>(emptyList())
    val desktopPages: LiveData<List<List<DesktopCellUi>>> = _desktopPages

    private val _dockSlots = MutableLiveData<List<DockSlotUi>>(emptyList())
    val dockSlots: LiveData<List<DockSlotUi>> = _dockSlots

    private val _drawerApps = MutableLiveData<List<AppListItemUi>>(emptyList())
    val drawerApps: LiveData<List<AppListItemUi>> = _drawerApps

    private val _privateApps = MutableLiveData<List<AppListItemUi>>(emptyList())
    val privateApps: LiveData<List<AppListItemUi>> = _privateApps

    private val _managerApps = MutableLiveData<List<AppListItemUi>>(emptyList())
    val managerApps: LiveData<List<AppListItemUi>> = _managerApps

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) { repository.loadHomeData() }
                homeData = data
                val pages = withContext(Dispatchers.IO) { buildDesktopPages(data) }
                val dock = withContext(Dispatchers.IO) { buildDockSlots(data) }
                val drawer = buildDrawerApps(data)
                val privateList = buildPrivateApps(data)
                val manager = buildManagerApps(data)

                _desktopPages.value = pages
                _dockSlots.value = dock
                _drawerApps.value = drawer
                _privateApps.value = privateList
                _managerApps.value = manager

                val maxSlots = LauncherConstants.MAX_SCREENS_CAP * LauncherConstants.CELLS_PER_SCREEN
                if (data.publicApps.size > maxSlots) {
                    _toastMessage.value = getApplication<Application>().getString(
                        R.string.desktop_pages_overflow,
                        LauncherConstants.MAX_SCREENS_CAP,
                        maxSlots
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "refresh failed", e)
                _toastMessage.value = getApplication<Application>().getString(R.string.launcher_load_failed)
            }
        }
    }

    fun loadIconAsync(app: LaunchableApp, onResult: (Drawable?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val icon = try {
                iconLoader.loadIcon(app)
            } catch (_: Exception) {
                null
            }
            withContext(Dispatchers.Main) { onResult(icon) }
        }
    }

    fun setCurrentScreen(index: Int) {
        val pageCount = _desktopPages.value?.size ?: 1
        if (index in 0 until pageCount) {
            _currentScreen.value = index
        }
    }

    fun setSearchQuery(query: String) {
        searchQuery = query.trim()
        homeData?.let { _drawerApps.value = buildDrawerApps(it) }
    }

    fun launchApp(app: LaunchableApp) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.launchApp(app)
        }
    }

    fun addToDesktop(app: LaunchableApp) {
        _toastMessage.value =
            getApplication<Application>().getString(R.string.already_on_desktop)
    }

    fun addToDock(app: LaunchableApp) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { repository.addToDock(app) }
            _toastMessage.value =
                if (ok) null else getApplication<Application>().getString(R.string.dock_full)
            refresh()
        }
    }

    fun moveToPrivateSpace(app: LaunchableApp) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.addToHidden(app) }
            refresh()
        }
    }

    fun removeFromPrivateSpace(app: LaunchableApp) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.removeFromHidden(app) }
            refresh()
        }
    }

    fun toggleHidden(app: LaunchableApp) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.toggleHidden(app) }
            refresh()
        }
    }

    fun clearAllHidden() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.clearAllHidden() }
            refresh()
        }
    }

    fun removeFromDock(app: LaunchableApp) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.removeFromDock(app.packageName, app.className)
            }
            refresh()
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun findApp(key: String): LaunchableApp? = homeData?.appByKey?.get(key)

    private fun buildDesktopPages(data: LauncherHomeData): List<List<DesktopCellUi>> {
        val apps = data.publicApps
        val pageCount = LauncherConstants.computePageCount(apps.size)
        return (0 until pageCount).map { screen ->
            val startIndex = screen * LauncherConstants.CELLS_PER_SCREEN
            buildList {
                for (y in 0 until LauncherConstants.GRID_ROWS) {
                    for (x in 0 until LauncherConstants.GRID_COLUMNS) {
                        val cellIndex = y * LauncherConstants.GRID_COLUMNS + x
                        val app = apps.getOrNull(startIndex + cellIndex)
                        add(
                            DesktopCellUi(
                                cellX = x,
                                cellY = y,
                                app = app,
                                icon = null,
                                desktopId = null
                            )
                        )
                    }
                }
            }
        }
    }

    private fun buildDockSlots(data: LauncherHomeData): List<DockSlotUi> {
        return (0 until LauncherConstants.DOCK_SIZE).map { position ->
            val entity = data.dockItems.find { it.position == position }
            val app = entity?.let { data.appByKey[it.key] }
            DockSlotUi(
                position = position,
                app = app,
                icon = null,
                dockId = entity?.id
            )
        }
    }

    private fun buildDrawerApps(data: LauncherHomeData): List<AppListItemUi> {
        var apps = data.publicApps.map { AppListItemUi(it, icon = null) }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            apps = apps.filter {
                it.app.label.lowercase().contains(q) ||
                    it.app.packageName.lowercase().contains(q)
            }
        }
        return apps
    }

    private fun buildPrivateApps(data: LauncherHomeData): List<AppListItemUi> {
        return data.privateApps.map { AppListItemUi(it, icon = null, isHidden = true) }
    }

    private fun buildManagerApps(data: LauncherHomeData): List<AppListItemUi> {
        return data.allApps.map { app ->
            AppListItemUi(
                app = app,
                icon = null,
                isHidden = app.key in data.hiddenKeys
            )
        }
    }

    companion object {
        private const val TAG = "HomeLauncherViewModel"
    }
}
