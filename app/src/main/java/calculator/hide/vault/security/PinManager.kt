package calculator.hide.vault.security

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Manages PIN using PBKDF2 with salt
 * Stores hash and salt in EncryptedSharedPreferences
 */
class PinManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "vault_pin_prefs"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val PBKDF2_ITERATIONS = 10000
        private const val SALT_LENGTH = 16 // bytes
        private const val HASH_LENGTH = 32 // bytes (256 bits)
    }
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    /**
     * Check if PIN is already set
     */
    fun isPinSet(): Boolean {
        return sharedPreferences.contains(KEY_PIN_HASH)
    }
    
    /**
     * Set a new PIN
     * @param pin The PIN to set (4-12 digits)
     * @return true if successful
     */
    fun setPin(pin: String): Boolean {
        if (pin.length < 4 || pin.length > 12 || !pin.all { it.isDigit() }) {
            return false
        }
        
        val salt = generateSalt()
        val hash = hashPin(pin, salt)
        
        sharedPreferences.edit()
            .putString(KEY_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .apply()
        
        return true
    }
    
    /**
     * Verify PIN
     * @param pin The PIN to verify
     * @return true if PIN is correct
     */
    fun verifyPin(pin: String): Boolean {
        if (!isPinSet()) {
            return false
        }
        
        val storedHash = Base64.decode(
            sharedPreferences.getString(KEY_PIN_HASH, null),
            Base64.NO_WRAP
        ) ?: return false
        
        val storedSalt = Base64.decode(
            sharedPreferences.getString(KEY_PIN_SALT, null),
            Base64.NO_WRAP
        ) ?: return false
        
        val computedHash = hashPin(pin, storedSalt)
        
        // Constant-time comparison to prevent timing attacks
        if (storedHash.size != computedHash.size) {
            return false
        }
        
        var result = 0
        for (i in storedHash.indices) {
            result = result or (storedHash[i].toInt() xor computedHash[i].toInt())
        }
        
        return result == 0
    }
    
    /**
     * Generate random salt
     */
    private fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return salt
    }
    
    /**
     * Hash PIN using PBKDF2
     */
    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(
            pin.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            HASH_LENGTH * 8 // bits
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }
    
    /**
     * Change PIN (requires old PIN verification)
     */
    fun changePin(oldPin: String, newPin: String): Boolean {
        if (!verifyPin(oldPin)) {
            return false
        }
        return setPin(newPin)
    }
}

