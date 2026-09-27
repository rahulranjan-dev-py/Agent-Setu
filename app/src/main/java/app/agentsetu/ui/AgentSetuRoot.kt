package app.agentsetu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.agentsetu.data.seed.SeedLoader
import app.agentsetu.data.settings.AppSettings
import app.agentsetu.ui.home.HomeScreen
import app.agentsetu.ui.welcome.WelcomeScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class RootViewModel @Inject constructor(
    private val settings: AppSettings,
    seedLoader: SeedLoader,
) : ViewModel() {
    val disclaimerAccepted = settings.disclaimerAccepted

    init {
        viewModelScope.launch {
            // Local-only log; nothing leaves the phone. Screens show "No rule" until rules load.
            runCatching { seedLoader.ensureSeeded() }.onFailure { Log.e("AgentSetu", "Seeding failed", it) }
        }
    }

    fun acceptDisclaimer() = settings.acceptDisclaimer()
}

/** Nothing else is reachable until the non-affiliation disclaimer has been acknowledged. */
@Composable
fun AgentSetuRoot(viewModel: RootViewModel = hiltViewModel()) {
    val accepted by viewModel.disclaimerAccepted.collectAsStateWithLifecycle()
    if (accepted) {
        HomeScreen()
    } else {
        WelcomeScreen(onAccept = viewModel::acceptDisclaimer)
    }
}
