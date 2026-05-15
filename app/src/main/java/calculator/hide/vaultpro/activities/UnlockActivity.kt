package calculator.hide.vaultpro.activities

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
import calculator.hide.vaultpro.security.PinManager

/**
 * PIN Unlock Activity
 * First time: Set PIN and confirm
 * Subsequent: Verify PIN
 */
class UnlockActivity : AppCompatActivity() {
    
    private lateinit var pinManager: PinManager
    private lateinit var titleTextView: TextView
    private lateinit var pinEditText: EditText
    private lateinit var confirmPinEditText: EditText
    private lateinit var confirmLabelTextView: TextView
    private lateinit var submitButton: Button
    
    private var isSettingPin = false
    private var firstPin: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Hide system action bar
        supportActionBar?.hide()
        setContentView(R.layout.activity_unlock)
        
        pinManager = PinManager(this)
        
        titleTextView = findViewById(R.id.tvTitle)
        pinEditText = findViewById(R.id.etPin)
        confirmPinEditText = findViewById(R.id.etConfirmPin)
        confirmLabelTextView = findViewById(R.id.tvConfirmLabel)
        submitButton = findViewById(R.id.btnSubmit)
        
        pinEditText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        
        if (pinManager.isPinSet()) {
            // Verify mode
            setupVerifyMode()
        } else {
            // Setup mode
            setupSetupMode()
        }
        
        submitButton.setOnClickListener {
            if (isSettingPin) {
                handlePinSetup()
            } else {
                handlePinVerify()
            }
        }
    }
    
    private fun setupVerifyMode() {
        isSettingPin = false
        titleTextView.text = "Enter PIN"
        confirmPinEditText.visibility = View.GONE
        confirmLabelTextView.visibility = View.GONE
        submitButton.text = "Unlock"
    }
    
    private fun setupSetupMode() {
        isSettingPin = true
        titleTextView.text = "Set PIN (4-12 digits)"
        confirmPinEditText.visibility = View.VISIBLE
        confirmLabelTextView.visibility = View.VISIBLE
        confirmPinEditText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        submitButton.text = "Set PIN"
    }
    
    private fun handlePinSetup() {
        val pin = pinEditText.text.toString()
        val confirmPin = confirmPinEditText.text.toString()
        
        if (pin.length < 4 || pin.length > 12) {
            Toast.makeText(this, "PIN must be 4-12 digits", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (firstPin == null) {
            // First entry
            if (!pin.all { it.isDigit() }) {
                Toast.makeText(this, "PIN must contain only digits", Toast.LENGTH_SHORT).show()
                return
            }
            firstPin = pin
            pinEditText.text.clear()
            confirmPinEditText.text.clear()
            titleTextView.text = "Confirm PIN"
            Toast.makeText(this, "Please confirm your PIN", Toast.LENGTH_SHORT).show()
        } else {
            // Confirmation
            if (pin != firstPin) {
                Toast.makeText(this, "PINs do not match", Toast.LENGTH_SHORT).show()
                firstPin = null
                pinEditText.text.clear()
                confirmPinEditText.text.clear()
                titleTextView.text = "Set PIN (4-12 digits)"
                return
            }
            
            if (pinManager.setPin(pin)) {
                Toast.makeText(this, "PIN set successfully", Toast.LENGTH_SHORT).show()
                navigateToVault()
            } else {
                Toast.makeText(this, "Failed to set PIN", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun handlePinVerify() {
        val pin = pinEditText.text.toString()
        
        if (pinManager.verifyPin(pin)) {
            navigateToVault()
        } else {
            Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
            pinEditText.text.clear()
        }
    }
    
    private fun navigateToVault() {
        startActivity(Intent(this, VaultActivity::class.java))
        finish()
    }
}

