package calculator.hide.vault.ui.launcher

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.UserHandle

class LauncherPackageMonitor(
    context: Context,
    private val onPackagesChanged: () -> Unit
) : LauncherApps.Callback() {

    private val launcherApps: LauncherApps =
        context.getSystemService(LauncherApps::class.java)
    private val handler = Handler(Looper.getMainLooper())

    fun register() {
        launcherApps.registerCallback(this, handler)
    }

    fun unregister() {
        launcherApps.unregisterCallback(this)
    }

    override fun onPackageAdded(packageName: String, user: UserHandle) {
        onPackagesChanged()
    }

    override fun onPackageRemoved(packageName: String, user: UserHandle) {
        onPackagesChanged()
    }

    override fun onPackageChanged(packageName: String, user: UserHandle) {
        onPackagesChanged()
    }

    override fun onPackagesAvailable(
        packageNames: Array<out String>,
        user: UserHandle,
        replacing: Boolean
    ) {
        onPackagesChanged()
    }

    override fun onPackagesUnavailable(
        packageNames: Array<out String>,
        user: UserHandle,
        replacing: Boolean
    ) {
        onPackagesChanged()
    }
}
