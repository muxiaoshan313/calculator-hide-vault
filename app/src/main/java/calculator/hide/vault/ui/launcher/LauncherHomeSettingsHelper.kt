package calculator.hide.vault.ui.launcher

import android.app.Activity
import android.app.role.RoleManager as AndroidRoleManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import calculator.hide.vault.R

object LauncherHomeSettingsHelper {

    const val REQUEST_DEFAULT_HOME = 1001

    fun requestDefaultHome(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = activity.getSystemService(AndroidRoleManager::class.java)
            if (roleManager.isRoleAvailable(AndroidRoleManager.ROLE_HOME) &&
                !roleManager.isRoleHeld(AndroidRoleManager.ROLE_HOME)
            ) {
                val intent = roleManager.createRequestRoleIntent(AndroidRoleManager.ROLE_HOME)
                activity.startActivityForResult(intent, REQUEST_DEFAULT_HOME)
                return
            }
            if (roleManager.isRoleHeld(AndroidRoleManager.ROLE_HOME)) {
                Toast.makeText(activity, R.string.already_default_home, Toast.LENGTH_SHORT).show()
                return
            }
        }
        try {
            activity.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        } catch (_: Exception) {
            Toast.makeText(activity, R.string.home_settings_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    fun registerForDefaultHome(
        activity: Activity,
        launcher: ActivityResultLauncher<Intent>
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = activity.getSystemService(AndroidRoleManager::class.java)
            if (roleManager.isRoleAvailable(AndroidRoleManager.ROLE_HOME) &&
                !roleManager.isRoleHeld(AndroidRoleManager.ROLE_HOME)
            ) {
                launcher.launch(roleManager.createRequestRoleIntent(AndroidRoleManager.ROLE_HOME))
                return
            }
        }
        try {
            activity.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        } catch (_: Exception) {
            Toast.makeText(activity, R.string.home_settings_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    fun onDefaultHomeResult(activity: Activity, resultCode: Int) {
        if (resultCode == Activity.RESULT_OK) {
            Toast.makeText(activity, R.string.default_home_set, Toast.LENGTH_SHORT).show()
        }
    }

    fun isDefaultHome(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = activity.getSystemService(AndroidRoleManager::class.java)
            return roleManager.isRoleHeld(AndroidRoleManager.ROLE_HOME)
        }
        return false
    }
}
