package calculator.hide.vaultpro.ui.auth

import android.content.Context
import calculator.hide.vaultpro.security.PinManager

/**
 * Session-based auth for private launcher features.
 * Reuses vault PIN via [PinManager].
 */
class PrivateSpaceAuthManager private constructor(context: Context) {

    private val pinManager = PinManager(context.applicationContext)

    private var lastAuthenticatedAt: Long = 0L
    private var wentToBackgroundAt: Long = 0L

    fun isPinSet(): Boolean = pinManager.isPinSet()

    fun verifyPin(pin: String): Boolean = pinManager.verifyPin(pin)

    fun setPin(pin: String): Boolean = pinManager.setPin(pin)

    fun isSessionValid(): Boolean {
        if (lastAuthenticatedAt == 0L) return false
        val now = System.currentTimeMillis()
        if (wentToBackgroundAt > lastAuthenticatedAt &&
            now - wentToBackgroundAt > SESSION_TIMEOUT_MS
        ) {
            return false
        }
        return now - lastAuthenticatedAt < SESSION_TIMEOUT_MS
    }

    fun markAuthenticated() {
        lastAuthenticatedAt = System.currentTimeMillis()
        wentToBackgroundAt = 0L
    }

    fun invalidateSession() {
        lastAuthenticatedAt = 0L
        wentToBackgroundAt = 0L
    }

    fun onAppBackgrounded() {
        if (lastAuthenticatedAt > 0L) {
            wentToBackgroundAt = System.currentTimeMillis()
        }
    }

    companion object {
        private const val SESSION_TIMEOUT_MS = 3 * 60 * 1000L

        @Volatile
        private var instance: PrivateSpaceAuthManager? = null

        fun getInstance(context: Context): PrivateSpaceAuthManager {
            return instance ?: synchronized(this) {
                instance ?: PrivateSpaceAuthManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
