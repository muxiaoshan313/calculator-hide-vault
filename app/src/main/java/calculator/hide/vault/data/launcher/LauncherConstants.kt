package calculator.hide.vault.data.launcher

object LauncherConstants {
    const val GRID_COLUMNS = 5
    const val GRID_ROWS = 6
    const val CELLS_PER_SCREEN = GRID_COLUMNS * GRID_ROWS
    const val MAX_SCREENS = 5
    const val DOCK_SIZE = 5
    /** User-assignable dock slots (last two are fixed launcher entries). */
    const val DOCK_USER_SLOTS = 3
    const val DOCK_ALL_APPS_POSITION = 3
    const val DOCK_PRIVATE_SPACE_POSITION = 4
    const val ICON_SIZE_DP = 56
    const val FOLDER_COLUMNS = 4

    fun computePageCount(appCount: Int): Int {
        if (appCount <= 0) return 1
        val needed = (appCount + CELLS_PER_SCREEN - 1) / CELLS_PER_SCREEN
        return needed.coerceIn(1, MAX_SCREENS)
    }
}

object LauncherItemType {
    const val APP = "APP"
    const val FOLDER = "FOLDER"
    const val WIDGET = "WIDGET"
    const val SHORTCUT = "SHORTCUT"
    const val PRIVATE_SPACE_ENTRY = "PRIVATE_SPACE_ENTRY"
    const val ALL_APPS_ENTRY = "ALL_APPS_ENTRY"
}

object HiddenAppMode {
    const val LOCAL = "LOCAL"
    const val SYSTEM_PRIVATE_PROFILE = "SYSTEM_PRIVATE_PROFILE"
}
