package calculator.hide.vault.data.launcher

import android.content.Context
import calculator.hide.vault.data.launcher.db.DesktopItemEntity
import calculator.hide.vault.data.launcher.db.DockItemEntity
import calculator.hide.vault.data.launcher.db.HiddenAppEntity
import calculator.hide.vault.data.launcher.db.LauncherDatabase

class LauncherRepository(context: Context) {

    private val appContext = context.applicationContext
    private val appRepository = AppRepository(appContext)
    private val db = LauncherDatabase.getInstance(appContext)

    suspend fun loadAllApps(): List<LaunchableApp> = appRepository.loadLaunchableApps()

    suspend fun loadHomeData(): LauncherHomeData {
        val allApps = appRepository.loadLaunchableApps()
        val appByKey = allApps.associateBy { it.key }
        val validKeys = allApps.map { it.key }

        cleanupInvalidRecords(validKeys)

        val hiddenEntities = db.hiddenAppDao().getAll()
        val hiddenKeys = hiddenEntities.map { it.key }.toSet()
        val publicApps = allApps.filter { it.key !in hiddenKeys }
        val publicKeys = publicApps.map { it.key }.toSet()
        val privateApps = hiddenEntities.mapNotNull { appByKey[it.key] }
            .sortedBy { entity ->
                hiddenEntities.find { it.key == entity.key }?.sortIndex ?: Int.MAX_VALUE
            }

        val desktopEntities = db.desktopItemDao().getAll()
            .filter { entity ->
                val key = entity.key
                key != null && key in publicKeys && key !in hiddenKeys
            }
        val dockEntities = db.dockItemDao().getAll()
            .filter { entity ->
                val key = entity.key
                key != null && key in publicKeys && key !in hiddenKeys &&
                    entity.position < LauncherConstants.DOCK_USER_SLOTS
            }

        return LauncherHomeData(
            allApps = allApps,
            publicApps = publicApps,
            privateApps = privateApps,
            hiddenKeys = hiddenKeys,
            hiddenEntities = hiddenEntities,
            desktopItems = desktopEntities,
            dockItems = dockEntities,
            appByKey = appByKey
        )
    }

    suspend fun addToHidden(app: LaunchableApp) {
        val existing = db.hiddenAppDao().getAll()
        val nextIndex = (existing.maxOfOrNull { it.sortIndex } ?: -1) + 1
        db.hiddenAppDao().insert(
            HiddenAppEntity(
                key = app.key,
                packageName = app.packageName,
                className = app.className,
                sortIndex = nextIndex,
                addedAt = System.currentTimeMillis(),
                mode = calculator.hide.vault.data.launcher.HiddenAppMode.LOCAL
            )
        )
        db.desktopItemDao().deleteByKey(app.packageName, app.className)
        db.dockItemDao().deleteByKey(app.packageName, app.className)
        db.folderItemDao().deleteByKey(app.packageName, app.className)
    }

    suspend fun removeFromHidden(app: LaunchableApp) {
        db.hiddenAppDao().deleteByKey(app.key)
    }

    suspend fun toggleHidden(app: LaunchableApp): Boolean {
        return if (db.hiddenAppDao().isHidden(app.key)) {
            removeFromHidden(app)
            false
        } else {
            addToHidden(app)
            true
        }
    }

    suspend fun clearAllHidden() {
        db.hiddenAppDao().clearAll()
    }

    suspend fun addToDesktop(app: LaunchableApp): Boolean {
        val data = loadHomeData()
        if (app.key in data.hiddenKeys) return false
        if (data.desktopItems.any { it.key == app.key }) return true
        val slot = findFirstEmptyDesktopSlot(data.desktopItems) ?: return false
        db.desktopItemDao().insert(
            DesktopItemEntity(
                packageName = app.packageName,
                className = app.className,
                screenIndex = slot.screenIndex,
                cellX = slot.cellX,
                cellY = slot.cellY
            )
        )
        return true
    }

    suspend fun addToDock(app: LaunchableApp): Boolean {
        val data = loadHomeData()
        if (app.key in data.hiddenKeys) return false
        if (data.dockItems.any { it.key == app.key }) return true
        val usedPositions = data.dockItems.map { it.position }.toSet()
        val position = (0 until LauncherConstants.DOCK_USER_SLOTS).firstOrNull { it !in usedPositions }
            ?: return false
        db.dockItemDao().insert(
            DockItemEntity(
                itemType = calculator.hide.vault.data.launcher.LauncherItemType.APP,
                packageName = app.packageName,
                className = app.className,
                position = position
            )
        )
        return true
    }

    suspend fun removeFromDesktop(id: Long) {
        db.desktopItemDao().deleteById(id)
    }

    suspend fun removeFromDesktopByKey(packageName: String, className: String) {
        db.desktopItemDao().deleteByKey(packageName, className)
    }

    suspend fun removeFromDock(packageName: String, className: String) {
        db.dockItemDao().deleteByKey(packageName, className)
    }

    fun launchApp(app: LaunchableApp) {
        appRepository.launchApp(app)
    }

    fun getIcon(app: LaunchableApp) = appRepository.getIcon(app)

    private suspend fun cleanupInvalidRecords(validKeys: List<String>) {
        if (validKeys.isEmpty()) {
            db.hiddenAppDao().clearAll()
            return
        }
        if (validKeys.isNotEmpty()) {
            db.hiddenAppDao().clearInvalid(validKeys)
            db.desktopItemDao().clearInvalidItems(validKeys)
            db.dockItemDao().clearInvalidItems(validKeys)
        }
    }

    private fun findFirstEmptyDesktopSlot(
        items: List<DesktopItemEntity>
    ): DesktopSlot? {
        val occupied = items.associateBy { Triple(it.screenIndex, it.cellX, it.cellY) }
        for (screen in 0 until LauncherConstants.MAX_SCREENS) {
            for (y in 0 until LauncherConstants.GRID_ROWS) {
                for (x in 0 until LauncherConstants.GRID_COLUMNS) {
                    if (Triple(screen, x, y) !in occupied) {
                        return DesktopSlot(screen, x, y)
                    }
                }
            }
        }
        return null
    }
}

data class LauncherHomeData(
    val allApps: List<LaunchableApp>,
    val publicApps: List<LaunchableApp>,
    val privateApps: List<LaunchableApp>,
    val hiddenKeys: Set<String>,
    val hiddenEntities: List<HiddenAppEntity>,
    val desktopItems: List<DesktopItemEntity>,
    val dockItems: List<DockItemEntity>,
    val appByKey: Map<String, LaunchableApp>
)

data class DesktopSlot(val screenIndex: Int, val cellX: Int, val cellY: Int)
