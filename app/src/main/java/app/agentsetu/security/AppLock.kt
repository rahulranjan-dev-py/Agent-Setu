package app.agentsetu.security

import android.content.Context
import app.agentsetu.core.security.LockPolicy
import app.agentsetu.core.security.PinHasher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

sealed interface PinAttempt {
    data object Ok : PinAttempt
    data object Wrong : PinAttempt
    data class Wait(val untilMs: Long) : PinAttempt
}

/**
 * Optional PIN (and fingerprint) lock. Asked on every cold start and after 2 minutes in the
 * background. Only a salted hash of the PIN is kept.
 */
@Singleton
class AppLock @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val isPinSet: Boolean get() = prefs.getString(KEY_HASH, null) != null

    private val _locked = MutableStateFlow(isPinSet)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    /** True once the user set a PIN or chose "skip" during onboarding. */
    private val _choiceMade = MutableStateFlow(isPinSet || prefs.getBoolean(KEY_CHOICE_MADE, false))
    val choiceMade: StateFlow<Boolean> = _choiceMade.asStateFlow()

    var biometricEnabled: Boolean
        get() = isPinSet && prefs.getBoolean(KEY_BIOMETRIC, false)
        set(value) {
            prefs.edit().putBoolean(KEY_BIOMETRIC, value).apply()
        }

    private var backgroundedAt: Long? = null

    fun onBackground(now: Long) {
        if (isPinSet && !_locked.value) backgroundedAt = now
    }

    fun onForeground(now: Long) {
        if (isPinSet && LockPolicy.shouldLockOnReturn(backgroundedAt, now)) _locked.value = true
        backgroundedAt = null
    }

    fun waitUntil(): Long = prefs.getLong(KEY_LOCKED_UNTIL, 0)

    suspend fun tryPin(pin: String, now: Long = System.currentTimeMillis()): PinAttempt {
        val until = waitUntil()
        if (now < until) return PinAttempt.Wait(until)
        val hash = prefs.getString(KEY_HASH, null) ?: return PinAttempt.Ok.also { _locked.value = false }
        val ok = withContext(Dispatchers.Default) { PinHasher.verify(pin, hash) }
        if (ok) {
            prefs.edit().putInt(KEY_FAILURES, 0).putLong(KEY_LOCKED_UNTIL, 0).apply()
            _locked.value = false
            return PinAttempt.Ok
        }
        val failures = prefs.getInt(KEY_FAILURES, 0) + 1
        val wait = LockPolicy.lockoutMs(failures)
        prefs.edit().putInt(KEY_FAILURES, failures).putLong(KEY_LOCKED_UNTIL, if (wait > 0) now + wait else 0).apply()
        return if (wait > 0) PinAttempt.Wait(now + wait) else PinAttempt.Wrong
    }

    fun unlockWithBiometric() {
        prefs.edit().putInt(KEY_FAILURES, 0).putLong(KEY_LOCKED_UNTIL, 0).apply()
        _locked.value = false
    }

    suspend fun setPin(pin: String) {
        val hash = withContext(Dispatchers.Default) { PinHasher.hash(pin) }
        prefs.edit().putString(KEY_HASH, hash).putBoolean(KEY_CHOICE_MADE, true).putInt(KEY_FAILURES, 0).apply()
        _choiceMade.value = true
    }

    fun removePin() {
        prefs.edit().remove(KEY_HASH).putBoolean(KEY_BIOMETRIC, false).apply()
        _locked.value = false
    }

    fun skipForNow() {
        prefs.edit().putBoolean(KEY_CHOICE_MADE, true).apply()
        _choiceMade.value = true
    }

    private companion object {
        const val PREFS_NAME = "app_lock"
        const val KEY_HASH = "pin_hash"
        const val KEY_BIOMETRIC = "biometric"
        const val KEY_FAILURES = "failures"
        const val KEY_LOCKED_UNTIL = "locked_until"
        const val KEY_CHOICE_MADE = "choice_made"
    }
}
