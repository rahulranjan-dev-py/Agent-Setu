package app.agentsetu.data.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Small app-level flags that are not customer data. */
@Singleton
class AppSettings @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _disclaimerAccepted = MutableStateFlow(
        prefs.getInt(KEY_DISCLAIMER_VERSION, 0) >= CURRENT_DISCLAIMER_VERSION,
    )

    /** False until the user acknowledges the current disclaimer; bumping the version asks again. */
    val disclaimerAccepted: StateFlow<Boolean> = _disclaimerAccepted.asStateFlow()

    fun acceptDisclaimer() {
        prefs.edit().putInt(KEY_DISCLAIMER_VERSION, CURRENT_DISCLAIMER_VERSION).apply()
        _disclaimerAccepted.value = true
    }

    /** Identifies the bundled seed last loaded, so seeding runs once per app update, not every launch. */
    var seedMarker: String?
        get() = prefs.getString(KEY_SEED_MARKER, null)
        set(value) {
            prefs.edit().putString(KEY_SEED_MARKER, value).apply()
        }

    private val _themeMode = MutableStateFlow(
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, null) ?: "") }.getOrDefault(ThemeMode.SYSTEM),
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _themeMode.value = mode
        applyNightMode(mode)
    }

    /** Also tells AppCompat, so the fingerprint prompt and system dialogs follow the choice. */
    fun applyNightMode(mode: ThemeMode = _themeMode.value) {
        AppCompatDelegate.setDefaultNightMode(
            when (mode) {
                ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            },
        )
    }

    /** Minute of the day for the daily reminder summary (default 07:00). */
    private val _reminderMinuteOfDay = MutableStateFlow(prefs.getInt(KEY_REMINDER_MINUTE, DEFAULT_REMINDER_MINUTE))
    val reminderMinuteOfDay: StateFlow<Int> = _reminderMinuteOfDay.asStateFlow()

    fun setReminderTime(hour: Int, minute: Int) {
        val value = hour * 60 + minute
        prefs.edit().putInt(KEY_REMINDER_MINUTE, value).apply()
        _reminderMinuteOfDay.value = value
    }

    /** When the user last saved or shared a backup file (epoch millis, 0 = never). */
    var lastBackupAt: Long
        get() = prefs.getLong(KEY_LAST_BACKUP, 0)
        set(value) {
            prefs.edit().putLong(KEY_LAST_BACKUP, value).apply()
        }

    companion object {
        const val CURRENT_DISCLAIMER_VERSION = 1
        const val DEFAULT_REMINDER_MINUTE = 7 * 60
        private const val KEY_THEME = "theme_mode"
        private const val KEY_REMINDER_MINUTE = "reminder_minute_of_day"
        private const val KEY_LAST_BACKUP = "last_backup_at"
        private const val KEY_SEED_MARKER = "seed_marker"
        private const val PREFS_NAME = "app_settings"
        private const val KEY_DISCLAIMER_VERSION = "disclaimer_accepted_version"
    }
}
