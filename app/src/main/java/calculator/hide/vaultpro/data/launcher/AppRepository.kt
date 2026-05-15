package calculator.hide.vaultpro.data.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle

class AppRepository(private val context: Context) {

    private val launcherApps: LauncherApps =
        context.getSystemService(LauncherApps::class.java)

    fun loadLaunchableApps(): List<LaunchableApp> {
        val result = mutableListOf<LaunchableApp>()

        launcherApps.profiles.forEach { user ->
            val apps = launcherApps.getActivityList(null, user)
            apps.forEach { info ->
                result.add(
                    LaunchableApp(
                        label = info.label?.toString().orEmpty(),
                        packageName = info.componentName.packageName,
                        className = info.componentName.className,
                        componentName = info.componentName,
                        user = user
                    )
                )
            }
        }

        return result
            .distinctBy { it.key }
            .sortedBy { it.label.lowercase() }
    }

    fun launchApp(app: LaunchableApp) {
        launcherApps.startMainActivity(
            app.componentName,
            app.user,
            null,
            null
        )
    }

    fun getIcon(packageName: String, className: String, user: UserHandle = Process.myUserHandle()): Drawable? {
        return try {
            val intent = Intent().setComponent(ComponentName(packageName, className))
            val info = launcherApps.resolveActivity(intent, user) ?: return null
            info.getIcon(0)
        } catch (_: Exception) {
            null
        }
    }

    fun getIcon(app: LaunchableApp): Drawable? {
        return try {
            val intent = Intent().setComponent(app.componentName)
            val info = launcherApps.resolveActivity(intent, app.user) ?: return null
            info.getIcon(0)
        } catch (_: Exception) {
            null
        }
    }
}
