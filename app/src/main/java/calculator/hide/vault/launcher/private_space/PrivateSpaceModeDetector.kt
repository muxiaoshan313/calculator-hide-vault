package calculator.hide.vault.launcher.private_space

import android.content.Context
import android.os.Build
import calculator.hide.vault.launcher.system.HomeRoleManager

/**
 * Phase 5 will implement System Private Space detection.
 * Phase 1 always uses Local Private Space fallback.
 */
object PrivateSpaceModeDetector {

    fun isSystemPrivateSpaceSupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 35) return false
        if (!HomeRoleManager.isDefaultHome(context)) return false
        return false
    }

    fun shouldUseLocalFallback(context: Context): Boolean {
        return !isSystemPrivateSpaceSupported(context)
    }
}
