package calculator.hide.vault.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import calculator.hide.vault.R
import calculator.hide.vault.config.SecretFormulaConfig
import calculator.hide.vault.ui.launcher.HomeLauncherActivity
import calculator.hide.vault.ui.launcher.PrivateAppManagerActivity
import calculator.hide.vault.ui.launcher.PrivateDesktopActivity

/**
 * Calculator Activity - Main entry point
 * Hidden trigger: Enter "123456" then long press "=" for 2 seconds to unlock vault
 */
class CalculatorActivity : AppCompatActivity() {
    
    companion object {
        private const val SECRET_CODE = "123456" // Configurable constant
        private const val LONG_PRESS_DURATION = 2000L // 2 seconds
    }
    
    private lateinit var displayTextView: TextView
    private var currentInput = StringBuilder()
    private var shouldCalculate = true
    private var longPressHandler: Handler? = null
    private var longPressRunnable: Runnable? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Hide action bar
        supportActionBar?.hide()
        setContentView(R.layout.activity_calculator)
        
        displayTextView = findViewById(R.id.tvDisplay)
        
        // Number buttons
        findViewById<Button>(R.id.btn0).setOnClickListener { appendNumber("0") }
        findViewById<Button>(R.id.btn1).setOnClickListener { appendNumber("1") }
        findViewById<Button>(R.id.btn2).setOnClickListener { appendNumber("2") }
        findViewById<Button>(R.id.btn3).setOnClickListener { appendNumber("3") }
        findViewById<Button>(R.id.btn4).setOnClickListener { appendNumber("4") }
        findViewById<Button>(R.id.btn5).setOnClickListener { appendNumber("5") }
        findViewById<Button>(R.id.btn6).setOnClickListener { appendNumber("6") }
        findViewById<Button>(R.id.btn7).setOnClickListener { appendNumber("7") }
        findViewById<Button>(R.id.btn8).setOnClickListener { appendNumber("8") }
        findViewById<Button>(R.id.btn9).setOnClickListener { appendNumber("9") }
        
        // Operator buttons
        findViewById<Button>(R.id.btnAdd).setOnClickListener { appendOperator("+") }
        findViewById<Button>(R.id.btnSubtract).setOnClickListener { appendOperator("-") }
        findViewById<Button>(R.id.btnMultiply).setOnClickListener { appendOperator("*") }
        findViewById<Button>(R.id.btnDivide).setOnClickListener { appendOperator("/") }
        findViewById<Button>(R.id.btnDot).setOnClickListener { appendNumber(".") }
        
        // Function buttons
        findViewById<Button>(R.id.btnClear).setOnClickListener { clear() }
        findViewById<Button>(R.id.btnBackspace).setOnClickListener { backspace() }
        
        // Equals button with long press detection
        val btnEquals = findViewById<Button>(R.id.btnEquals)
        btnEquals.setOnClickListener {
            if (shouldCalculate) {
                calculate()
            }
        }
        
        btnEquals.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    // Start long press timer when pressed
                    checkSecretCode()
                }
                android.view.MotionEvent.ACTION_UP, 
                android.view.MotionEvent.ACTION_CANCEL -> {
                    // Cancel timer if released before duration
                    longPressRunnable?.let { longPressHandler?.removeCallbacks(it) }
                }
            }
            false // Don't consume event, let click listener handle it
        }
        
        longPressHandler = Handler(Looper.getMainLooper())
    }
    
    private fun appendNumber(number: String) {
        currentInput.append(number)
        updateDisplay()
        shouldCalculate = true
    }
    
    private fun appendOperator(operator: String) {
        if (currentInput.isEmpty() || currentInput.last().toString() in listOf("+", "-", "*", "/")) {
            return
        }
        currentInput.append(operator)
        updateDisplay()
        shouldCalculate = true
    }
    
    private fun clear() {
        currentInput.clear()
        updateDisplay()
        shouldCalculate = true
    }
    
    private fun backspace() {
        if (currentInput.isNotEmpty()) {
            currentInput.deleteCharAt(currentInput.length - 1)
            updateDisplay()
        }
        shouldCalculate = true
    }
    
    private fun calculate() {
        if (currentInput.isEmpty()) return
        if (trySecretFormulaNavigation()) return

        try {
            val expression = currentInput.toString()
            val result = evaluateExpression(expression)
            val formattedResult = formatResult(result, expression)
            currentInput.clear()
            currentInput.append(formattedResult)
            updateDisplay()
        } catch (e: Exception) {
            displayTextView.text = "Error"
            currentInput.clear()
        }
    }
    
    private fun evaluateExpression(expression: String): Double {
        // Parse expression into tokens (numbers and operators)
        val tokens = mutableListOf<String>()
        var current = StringBuilder()
        
        for (char in expression.replace(" ", "")) {
            if (char in "+-*/") {
                if (current.isNotEmpty()) {
                    tokens.add(current.toString())
                    current.clear()
                }
                tokens.add(char.toString())
            } else {
                current.append(char)
            }
        }
        if (current.isNotEmpty()) {
            tokens.add(current.toString())
        }
        
        if (tokens.isEmpty()) return 0.0
        
        // First pass: handle multiplication and division
        val processed = mutableListOf<String>()
        var i = 0
        while (i < tokens.size) {
            if (i < tokens.size - 2 && tokens[i + 1] in "*/") {
                val left = tokens[i].toDouble()
                val op = tokens[i + 1]
                val right = tokens[i + 2].toDouble()
                
                val result = when (op) {
                    "*" -> left * right
                    "/" -> {
                        if (right == 0.0) throw ArithmeticException("Division by zero")
                        left / right
                    }
                    else -> throw IllegalArgumentException("Invalid operator")
                }
                
                processed.add(result.toString())
                i += 3
            } else {
                processed.add(tokens[i])
                i++
            }
        }
        
        // Second pass: handle addition and subtraction
        var result = processed[0].toDouble()
        i = 1
        while (i < processed.size) {
            val op = processed[i]
            val operand = processed[i + 1].toDouble()
            
            result = when (op) {
                "+" -> result + operand
                "-" -> result - operand
                else -> throw IllegalArgumentException("Invalid operator: $op")
            }
            
            i += 2
        }
        
        return result
    }
    
    /**
     * Format calculation result:
     * - If integer, display without decimal point
     * - If decimal, preserve correct decimal places from input
     * - Round if necessary (for division that doesn't divide evenly)
     */
    private fun formatResult(result: Double, originalExpression: String): String {
        // Check if result is effectively an integer (with small epsilon to handle floating point errors)
        val roundedResult = Math.round(result * 1e10) / 1e10
        if (Math.abs(roundedResult % 1.0) < 1e-10) {
            return roundedResult.toLong().toString()
        }
        
        // Calculate maximum decimal places from original expression
        val maxDecimalPlaces = getMaxDecimalPlaces(originalExpression)
        val hasDivision = originalExpression.contains("/")
        
        if (hasDivision) {
            // For division: use up to 10 decimal places, then remove trailing zeros
            // This handles cases where division doesn't divide evenly
            val formatted = String.format("%.10f", result)
            return formatted.trimEnd('0').trimEnd('.')
        } else {
            // For addition, subtraction, multiplication: preserve input precision
            // Use the maximum decimal places from input numbers
            if (maxDecimalPlaces > 0) {
                val formatted = String.format("%.${maxDecimalPlaces}f", result)
                return formatted.trimEnd('0').trimEnd('.')
            } else {
                // No decimals in input, but result has decimals (shouldn't happen for +, -, *)
                // But handle it anyway
                return String.format("%.10f", result).trimEnd('0').trimEnd('.')
            }
        }
    }
    
    /**
     * Get maximum decimal places from numbers in the expression
     */
    private fun getMaxDecimalPlaces(expression: String): Int {
        // Parse expression to extract all numbers
        val numbers = mutableListOf<String>()
        var current = StringBuilder()
        
        for (char in expression.replace(" ", "")) {
            if (char in "+-*/") {
                if (current.isNotEmpty()) {
                    numbers.add(current.toString())
                    current.clear()
                }
            } else {
                current.append(char)
            }
        }
        if (current.isNotEmpty()) {
            numbers.add(current.toString())
        }
        
        var maxPlaces = 0
        for (numStr in numbers) {
            if (numStr.contains(".")) {
                val decimalPart = numStr.substring(numStr.indexOf(".") + 1)
                maxPlaces = maxOf(maxPlaces, decimalPart.length)
            }
        }
        
        return maxPlaces
    }
    
    private fun updateDisplay() {
        displayTextView.text = if (currentInput.isEmpty()) "0" else currentInput.toString()
    }
    
    private fun trySecretFormulaNavigation(): Boolean {
        val destination = SecretFormulaConfig.matchDestination(currentInput.toString())
            ?: return false

        shouldCalculate = false
        currentInput.clear()
        updateDisplay()

        val target = when (destination) {
            SecretFormulaConfig.Destination.PRIVATE_APP_MANAGER ->
                Intent(this, PrivateAppManagerActivity::class.java)
            SecretFormulaConfig.Destination.PUBLIC_DESKTOP ->
                Intent(this, HomeLauncherActivity::class.java)
            SecretFormulaConfig.Destination.PRIVATE_DESKTOP ->
                Intent(this, PrivateDesktopActivity::class.java)
        }
        startActivity(target)
        return true
    }

    private fun checkSecretCode() {
        val input = currentInput.toString()
        if (input == SecretFormulaConfig.FORMULA_VAULT) {
            // Start long press timer
            longPressRunnable = Runnable {
                // Trigger unlock
                shouldCalculate = false
                startActivity(Intent(this, UnlockActivity::class.java))
            }
            longPressHandler?.postDelayed(longPressRunnable!!, LONG_PRESS_DURATION)
        } else {
            // Cancel if not matching
            longPressRunnable?.let { longPressHandler?.removeCallbacks(it) }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        longPressRunnable?.let { longPressHandler?.removeCallbacks(it) }
    }
}

