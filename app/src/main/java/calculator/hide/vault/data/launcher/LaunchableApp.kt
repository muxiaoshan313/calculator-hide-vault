package calculator.hide.vault.data.launcher

import android.content.ComponentName
import android.os.UserHandle

data class LaunchableApp(
    val label: String,
    val packageName: String,
    val className: String,
    val componentName: ComponentName,
    val user: UserHandle
) {
    val key: String
        get() = "$packageName/$className"
}
