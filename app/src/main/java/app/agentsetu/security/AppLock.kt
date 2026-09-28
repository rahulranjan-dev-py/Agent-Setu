package app.agentsetu.security

import android.content.Context
import app.agentsetu.core.security.LockPolicy
import app.agentsetu.core.security.PinHasher
import app.agentsetu.core.security.RecoveryCode
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
 * Optional PIN lock, with fingerprint / face / phone-lock unlock. Asked on every cold start and after
 * 2 minutes in the background. Only salted hashes of the PIN and of the recovery code are kept.
 *
 * Forgot PIN: the recovery code shown at PIN setup, or the phone's own lock, verifies the user; the
 * PIN is then removed and set-up is asked again (resetForNewPin). There is no other way in.
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

    /** PINs set before 1.1.1 have no recovery code until the PIN is changed. */
    val hasRecoveryCode: Boolean get() = prefs.getString(KEY_RECOVERY_HASH, null) != null

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
            clearFailures()
            _locked.value = false
            return PinAttempt.Ok
        }
        return recordFailure(now)
    }

    /**
     * Forgot PIN, way 1: the recovery code. Shares the PIN's lock-out counter, so it cannot be
     * guessed any faster than the PIN. On success the PIN is removed and set-up is asked again.
     */
    suspend fun tryRecoveryCode(input: String, now: Long = System.currentTimeMillis()): PinAttempt {
        val until = waitUntil()
        if (now < until) return PinAttempt.Wait(until)
        val hash = prefs.getString(KEY_RECOVERY_HASH, null) ?: return PinAttempt.Wrong
        val code = RecoveryCode.normalize(input)
        val ok = RecoveryCode.isValid(code) && withContext(Dispatchers.Default) { PinHasher.verify(code, hash) }
        if (!ok) return recordFailure(now)
        resetForNewPin()
        return PinAttempt.Ok
    }

    /** Forgot PIN, way 2: the phone's own lock verified the user (Biometrics.prompt succeeded). */
    fun resetAfterDeviceUnlock() = resetForNewPin()

    private fun resetForNewPin() {
        prefs.edit()
            .remove(KEY_HASH)
            .remove(KEY_RECOVERY_HASH)
            .putBoolean(KEY_BIOMETRIC, false)
            .putBoolean(KEY_CHOICE_MADE, false)
            .putInt(KEY_FAILURES, 0)
            .putLong(KEY_LOCKED_UNTIL, 0)
            .apply()
        _locked.value = false
        _choiceMade.value = false
    }

    private fun clearFailures() {
        prefs.edit().putInt(KEY_FAILURES, 0).putLong(KEY_LOCKED_UNTIL, 0).apply()
    }

    private fun recordFailure(now: Long): PinAttempt {
        val failures = prefs.getInt(KEY_FAILURES, 0) + 1
        val wait = LockPolicy.lockoutMs(failures)
        prefs.edit().putInt(KEY_FAILURES, failures).putLong(KEY_LOCKED_UNTIL, if (wait > 0) now + wait else 0).apply()
        return if (wait > 0) PinAttempt.Wait(now + wait) else PinAttempt.Wrong
    }

    fun unlockWithBiometric() {
        clearFailures()
        _locked.value = false
    }

    /**
     * Sets (or changes) the PIN and returns a fresh recovery code, formatted for writing down.
     * The code is shown once and never stored, only its hash.
     */
    suspend fun setPin(pin: String): String {
        val code = RecoveryCode.generate()
        val (hash, codeHash) = withContext(Dispatchers.Default) { PinHasher.hash(pin) to PinHasher.hashSecret(code) }
        prefs.edit()
            .putString(KEY_HASH, hash)
            .putString(KEY_RECOVERY_HASH, codeHash)
            .putBoolean(KEY_CHOICE_MADE, true)
            .putInt(KEY_FAILURES, 0)
            .apply()
        _choiceMade.value = true
        return RecoveryCode.format(code)
    }

    fun removePin() {
        prefs.edit().remove(KEY_HASH).remove(KEY_RECOVERY_HASH).putBoolean(KEY_BIOMETRIC, false).apply()
        _locked.value = false
    }

    fun skipForNow() {
        prefs.edit().putBoolean(KEY_CHOICE_MADE, true).apply()
        _choiceMade.value = true
    }

    private companion object {
        const val PREFS_NAME = "app_lock"
        const val KEY_HASH = "pin_hash"
        const val KEY_RECOVERY_HASH = "recovery_hash"
        const val KEY_BIOMETRIC = "biometric"
        const val KEY_FAILURES = "failures"
        const val KEY_LOCKED_UNTIL = "locked_until"
        const val KEY_CHOICE_MADE = "choice_made"
    }
}
