package app.agentsetu

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import app.agentsetu.security.AppLock
import app.agentsetu.ui.AgentSetuRoot
import app.agentsetu.ui.theme.AgentSetuTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// AppCompatActivity (a FragmentActivity) so per-app language switching works on Android 8-12 and
// the fingerprint prompt can attach to it.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var appLock: AppLock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AgentSetuTheme {
                AgentSetuRoot()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appLock.onForeground(System.currentTimeMillis())
    }

    override fun onStop() {
        super.onStop()
        appLock.onBackground(System.currentTimeMillis())
    }
}
