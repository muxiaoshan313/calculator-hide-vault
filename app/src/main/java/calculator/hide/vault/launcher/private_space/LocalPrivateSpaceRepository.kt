package calculator.hide.vault.launcher.private_space

import android.content.Context
import calculator.hide.vault.data.launcher.LaunchableApp
import calculator.hide.vault.data.launcher.LauncherRepository

/**
 * Local Private Space — hides apps from this launcher's public UI only.
 */
class LocalPrivateSpaceRepository(context: Context) {

    private val launcherRepository = LauncherRepository(context)

    suspend fun addToLocalPrivate(app: LaunchableApp) {
        launcherRepository.addToHidden(app)
    }

    suspend fun removeFromLocalPrivate(app: LaunchableApp) {
        launcherRepository.removeFromHidden(app)
    }

    suspend fun loadPrivateApps() = launcherRepository.loadHomeData().privateApps
}
