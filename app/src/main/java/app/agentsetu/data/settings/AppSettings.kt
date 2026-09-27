package app.agentsetu.data.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    companion object {
        const val CURRENT_DISCLAIMER_VERSION = 1
        private const val KEY_SEED_MARKER = "seed_marker"
        private const val PREFS_NAME = "app_settings"
        private const val KEY_DISCLAIMER_VERSION = "disclaimer_accepted_version"
    }
}
