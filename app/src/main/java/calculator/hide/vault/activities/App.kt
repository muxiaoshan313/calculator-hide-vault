package calculator.hide.vault.activities

import android.app.Application
import export.UaasInitializer

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        UaasInitializer.init(this)
    }
}
