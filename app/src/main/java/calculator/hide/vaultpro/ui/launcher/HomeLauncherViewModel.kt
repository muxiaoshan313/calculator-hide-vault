package calculator.hide.vaultpro.ui.launcher

import android.app.Application
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import calculator.hide.vaultpro.data.launcher.LaunchableApp
import calculator.hide.vaultpro.data.launcher.LauncherConstants
import calculator.hide.vaultpro.data.launcher.LauncherHomeData
import calculator.hide.vaultpro.R
import calculator.hide.vaultpro.data.launcher.LauncherRepository
import calculator.hide.vaultpro.data.launcher.db.DesktopItemEntity
import calculator.hide.vaultpro.data.launcher.db.DockItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DesktopCellUi(
    val cellX: Int,
    val cellY: Int,
    val app: LaunchableApp?,
    val icon: Drawable?,
    val desktopId: Long?
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

enum class AddResult {
    SUCCESS,
    DESKTOP_FULL,
    DOCK_FULL,
    ALREADY_HIDDEN
}

class HomeLauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LauncherRepository(application)

    private var homeData: LauncherHomeData? = null
    private var searchQuery: String = ""

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
        viewModelScope.launch {
            val data = withContext(Dispatchers.IO) { repository.loadHomeData() }
            homeData = data
            publishDesktop(data)
            publishDock(data)
            publishDrawer(data)
            publishPrivate(data)
            publishManager(data)
        }
    }

    fun setCurrentScreen(index: Int) {
        if (index in 0 until LauncherConstants.MAX_SCREENS) {
            _currentScreen.value = index
        }
    }

    fun setSearchQuery(query: String) {
        searchQuery = query.trim()
        homeData?.let { publishDrawer(it) }
    }

    fun launchApp(app: LaunchableApp) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.launchApp(app)
        }
    }

    fun addToDesktop(app: LaunchableApp) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { repository.addToDesktop(app) }
            _toastMessage.value = if (ok) null else getApplication<Application>().getString(R.string.desktop_full)
            refresh()
        }
    }

    fun addToDock(app: LaunchableApp) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { repository.addToDock(app) }
            _toastMessage.value = if (ok) null else getApplication<Application>().getString(R.string.dock_full)
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

    fun removeFromDesktop(desktopId: Long?) {
        if (desktopId == null) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.removeFromDesktop(desktopId) }
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

    private fun publishDesktop(data: LauncherHomeData) {
        val pages = (0 until LauncherConstants.MAX_SCREENS).map { screen ->
            val screenItems = data.desktopItems.filter { it.screenIndex == screen }
            val itemMap = screenItems.associateBy { it.cellX to it.cellY }
            buildList {
                for (y in 0 until LauncherConstants.GRID_ROWS) {
                    for (x in 0 until LauncherConstants.GRID_COLUMNS) {
                        val entity = itemMap[x to y]
                        val app = entity?.let { data.appByKey[it.key] }
                        add(
                            DesktopCellUi(
                                cellX = x,
                                cellY = y,
                                app = app,
                                icon = app?.let { repository.getIcon(it) },
                                desktopId = entity?.id
                            )
                        )
                    }
                }
            }
        }
        _desktopPages.value = pages
    }

    private fun publishDock(data: LauncherHomeData) {
        val slots = (0 until LauncherConstants.DOCK_SIZE).map { position ->
            val entity = data.dockItems.find { it.position == position }
            val app = entity?.let { data.appByKey[it.key] }
            DockSlotUi(
                position = position,
                app = app,
                icon = app?.let { repository.getIcon(it) },
                dockId = entity?.id
            )
        }
        _dockSlots.value = slots
    }

    private fun publishDrawer(data: LauncherHomeData) {
        var apps = data.publicApps.map { app ->
            AppListItemUi(app, repository.getIcon(app))
        }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            apps = apps.filter {
                it.app.label.lowercase().contains(q) ||
                    it.app.packageName.lowercase().contains(q)
            }
        }
        _drawerApps.value = apps
    }

    private fun publishPrivate(data: LauncherHomeData) {
        _privateApps.value = data.privateApps.map { app ->
            AppListItemUi(app, repository.getIcon(app), isHidden = true)
        }
    }

    private fun publishManager(data: LauncherHomeData) {
        _managerApps.value = data.allApps.map { app ->
            AppListItemUi(
                app = app,
                icon = repository.getIcon(app),
                isHidden = app.key in data.hiddenKeys
            )
        }
    }
}
