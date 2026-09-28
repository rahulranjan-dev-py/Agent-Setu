package app.agentsetu

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.os.Build
import app.agentsetu.data.db.DatabaseKeyProvider
import app.agentsetu.data.settings.AppSettings
import app.agentsetu.ui.DataUnavailableScreen
import app.agentsetu.data.settings.ThemeMode
import app.agentsetu.security.AppLock
import app.agentsetu.update.UpdateChecker
import app.agentsetu.ui.AgentSetuRoot
import app.agentsetu.ui.theme.AgentSetuTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// AppCompatActivity (a FragmentActivity) so per-app language switching works on Android 8-12 and
// the fingerprint prompt can attach to it.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var appLock: AppLock
    @Inject lateinit var settings: AppSettings
    @Inject lateinit var updates: UpdateChecker
    @Inject lateinit var keyProvider: DatabaseKeyProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Customer names must not sit in the Recents thumbnail (Android 13+ honours this flag).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) setRecentsScreenshotEnabled(false)
        // Checked before anything asks Hilt for the database: an unopenable key must not crash-loop.
        val dataUsable = keyProvider.isUsable()
        setContent {
            val mode by settings.themeMode.collectAsStateWithLifecycle()
            val dark = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            AgentSetuTheme(darkTheme = dark) {
                if (dataUsable) AgentSetuRoot() else DataUnavailableScreen()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appLock.onForeground(System.currentTimeMillis())
        updates.onForeground()
    }

    override fun onStop() {
        super.onStop()
        appLock.onBackground(System.currentTimeMillis())
    }
}
