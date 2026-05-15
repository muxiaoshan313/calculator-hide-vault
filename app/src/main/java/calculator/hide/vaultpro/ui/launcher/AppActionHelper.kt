package calculator.hide.vaultpro.ui.launcher

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import calculator.hide.vaultpro.R
import calculator.hide.vaultpro.data.launcher.LaunchableApp

object AppActionHelper {

    fun showDesktopAppMenu(
        activity: Activity,
        app: LaunchableApp,
        onRemoveFromDesktop: () -> Unit,
        onMoveToPrivate: () -> Unit
    ) {
        val options = arrayOf(
            activity.getString(R.string.remove_from_desktop),
            activity.getString(R.string.move_to_private_space),
            activity.getString(R.string.app_info),
            activity.getString(R.string.uninstall)
        )
        AlertDialog.Builder(activity)
            .setTitle(app.label)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> onRemoveFromDesktop()
                    1 -> onMoveToPrivate()
                    2 -> openAppInfo(activity, app)
                    3 -> uninstallApp(activity, app)
                }
            }
            .show()
    }

    fun showDrawerAppMenu(
        activity: Activity,
        app: LaunchableApp,
        onAddDesktop: () -> Unit,
        onAddDock: () -> Unit,
        onMoveToPrivate: () -> Unit
    ) {
        val options = arrayOf(
            activity.getString(R.string.add_to_desktop),
            activity.getString(R.string.add_to_dock),
            activity.getString(R.string.move_to_private_space),
            activity.getString(R.string.app_info)
        )
        AlertDialog.Builder(activity)
            .setTitle(app.label)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> onAddDesktop()
                    1 -> onAddDock()
                    2 -> onMoveToPrivate()
                    3 -> openAppInfo(activity, app)
                }
            }
            .show()
    }

    fun showDockAppMenu(
        activity: Activity,
        app: LaunchableApp,
        onRemoveFromDock: () -> Unit,
        onMoveToPrivate: () -> Unit
    ) {
        val options = arrayOf(
            activity.getString(R.string.remove_from_dock),
            activity.getString(R.string.move_to_private_space),
            activity.getString(R.string.app_info),
            activity.getString(R.string.uninstall)
        )
        AlertDialog.Builder(activity)
            .setTitle(app.label)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> onRemoveFromDock()
                    1 -> onMoveToPrivate()
                    2 -> openAppInfo(activity, app)
                    3 -> uninstallApp(activity, app)
                }
            }
            .show()
    }

    fun showPrivateAppMenu(
        activity: Activity,
        app: LaunchableApp,
        onRemoveFromPrivate: () -> Unit
    ) {
        val options = arrayOf(
            activity.getString(R.string.remove_from_private_space),
            activity.getString(R.string.app_info)
        )
        AlertDialog.Builder(activity)
            .setTitle(app.label)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> onRemoveFromPrivate()
                    1 -> openAppInfo(activity, app)
                }
            }
            .show()
    }

    fun openAppInfo(activity: Activity, app: LaunchableApp) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", app.packageName, null)
            }
            activity.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(activity, R.string.action_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    fun uninstallApp(activity: Activity, app: LaunchableApp) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:${app.packageName}")
            }
            activity.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(activity, R.string.action_unavailable, Toast.LENGTH_SHORT).show()
        }
    }
}
