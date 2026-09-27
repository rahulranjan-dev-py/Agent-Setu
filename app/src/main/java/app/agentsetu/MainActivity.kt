package app.agentsetu

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import app.agentsetu.ui.AgentSetuRoot
import app.agentsetu.ui.theme.AgentSetuTheme
import dagger.hilt.android.AndroidEntryPoint

// AppCompatActivity (not plain ComponentActivity) so per-app language switching works on Android 8-12.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AgentSetuTheme {
                AgentSetuRoot()
            }
        }
    }
}
