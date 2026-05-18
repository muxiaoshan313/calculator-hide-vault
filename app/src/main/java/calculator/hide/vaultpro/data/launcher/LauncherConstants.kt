package calculator.hide.vaultpro.data.launcher

object LauncherConstants {
    const val GRID_COLUMNS = 4
    const val GRID_ROWS = 5
    const val CELLS_PER_SCREEN = GRID_COLUMNS * GRID_ROWS
    /** Max desktop pages when many apps are installed (4×5×50 = 1000 app slots). */
    const val MAX_SCREENS_CAP = 50
    const val DOCK_SIZE = 4

    fun computePageCount(appCount: Int): Int {
        if (appCount <= 0) return 1
        val needed = (appCount + CELLS_PER_SCREEN - 1) / CELLS_PER_SCREEN
        return needed.coerceAtMost(MAX_SCREENS_CAP)
    }
}
