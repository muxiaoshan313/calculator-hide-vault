package calculator.hide.vaultpro.activities

import android.app.Activity
import android.app.Application
import android.os.Bundle
import calculator.hide.vaultpro.ui.auth.PrivateSpaceAuthManager

class App : Application() {

    private var startedActivityCount = 0

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) {
                if (startedActivityCount == 0) {
                    // Returned to foreground — session validity checked on demand
                }
                startedActivityCount++
            }

            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit

            override fun onActivityStopped(activity: Activity) {
                startedActivityCount--
                if (startedActivityCount == 0) {
                    PrivateSpaceAuthManager.getInstance(this@App).onAppBackgrounded()
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
