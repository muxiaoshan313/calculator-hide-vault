package calculator.hide.vaultpro.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import calculator.hide.vaultpro.R

/**
 * PIN verification / first-time setup gate for private launcher features.
 */
class PinVerifyActivity : AppCompatActivity() {

    private lateinit var authManager: PrivateSpaceAuthManager
    private lateinit var titleTextView: TextView
    private lateinit var pinEditText: EditText
    private lateinit var confirmPinEditText: EditText
    private lateinit var confirmLabelTextView: TextView
    private lateinit var submitButton: Button

    private var isSettingPin = false
    private var firstPin: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_unlock)

        authManager = PrivateSpaceAuthManager.getInstance(this)

        titleTextView = findViewById(R.id.tvTitle)
        pinEditText = findViewById(R.id.etPin)
        confirmPinEditText = findViewById(R.id.etConfirmPin)
        confirmLabelTextView = findViewById(R.id.tvConfirmLabel)
        submitButton = findViewById(R.id.btnSubmit)

        pinEditText.inputType =
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD

        if (authManager.isPinSet()) {
            setupVerifyMode()
        } else {
            setupSetupMode()
        }

        submitButton.setOnClickListener {
            if (isSettingPin) handlePinSetup() else handlePinVerify()
        }
    }

    private fun setupVerifyMode() {
        isSettingPin = false
        titleTextView.text = getString(R.string.enter_pin)
        confirmPinEditText.visibility = View.GONE
        confirmLabelTextView.visibility = View.GONE
        submitButton.text = getString(R.string.unlock)
    }

    private fun setupSetupMode() {
        isSettingPin = true
        titleTextView.text = getString(R.string.set_pin_title)
        confirmPinEditText.visibility = View.VISIBLE
        confirmLabelTextView.visibility = View.VISIBLE
        confirmPinEditText.inputType =
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        submitButton.text = getString(R.string.set_pin_button)
    }

    private fun handlePinSetup() {
        val pin = pinEditText.text.toString()
        if (pin.length < 4 || pin.length > 12) {
            Toast.makeText(this, R.string.pin_length_error, Toast.LENGTH_SHORT).show()
            return
        }
        if (!pin.all { it.isDigit() }) {
            Toast.makeText(this, R.string.pin_digits_only, Toast.LENGTH_SHORT).show()
            return
        }

        if (firstPin == null) {
            firstPin = pin
            pinEditText.text.clear()
            confirmPinEditText.text.clear()
            titleTextView.text = getString(R.string.confirm_pin_title)
            Toast.makeText(this, R.string.confirm_pin_hint, Toast.LENGTH_SHORT).show()
            return
        }

        if (pin != firstPin) {
            Toast.makeText(this, R.string.pin_mismatch, Toast.LENGTH_SHORT).show()
            firstPin = null
            pinEditText.text.clear()
            confirmPinEditText.text.clear()
            titleTextView.text = getString(R.string.set_pin_title)
            return
        }

        if (authManager.setPin(pin)) {
            authManager.markAuthenticated()
            finishSuccess()
        } else {
            Toast.makeText(this, R.string.pin_set_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handlePinVerify() {
        val pin = pinEditText.text.toString()
        if (authManager.verifyPin(pin)) {
            authManager.markAuthenticated()
            finishSuccess()
        } else {
            Toast.makeText(this, R.string.incorrect_pin, Toast.LENGTH_SHORT).show()
            pinEditText.text.clear()
        }
    }

    private fun finishSuccess() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    companion object {
        const val REQUEST_CODE = 2001

        fun createIntent(context: android.content.Context): Intent {
            return Intent(context, PinVerifyActivity::class.java)
        }
    }
}
