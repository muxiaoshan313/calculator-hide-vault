package calculator.hide.vault.ui.launcher

import android.app.role.RoleManager as AndroidRoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import calculator.hide.vault.R

class LauncherSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_launcher_settings)

        val tvStatus = findViewById<TextView>(R.id.tvDefaultHomeStatus)
        val btnSet = findViewById<Button>(R.id.btnSetDefaultHome)

        updateStatus(tvStatus)

        btnSet.setOnClickListener {
            LauncherHomeSettingsHelper.requestDefaultHome(this)
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus(findViewById(R.id.tvDefaultHomeStatus))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LauncherHomeSettingsHelper.REQUEST_DEFAULT_HOME) {
            LauncherHomeSettingsHelper.onDefaultHomeResult(this, resultCode)
            updateStatus(findViewById(R.id.tvDefaultHomeStatus))
        }
    }

    private fun updateStatus(tv: TextView) {
        val isDefault = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(AndroidRoleManager::class.java)
            roleManager.isRoleHeld(AndroidRoleManager.ROLE_HOME)
        } else {
            false
        }
        tv.text = if (isDefault) {
            getString(R.string.default_home_status_set)
        } else {
            getString(R.string.default_home_status_not_set)
        }
    }
}
