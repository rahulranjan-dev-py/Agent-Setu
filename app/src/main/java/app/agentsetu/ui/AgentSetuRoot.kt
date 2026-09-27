package app.agentsetu.ui

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.agentsetu.data.db.AgentSetuDatabase
import app.agentsetu.data.seed.SeedLoader
import app.agentsetu.data.settings.AppSettings
import app.agentsetu.ui.nav.MainNavigation
import app.agentsetu.ui.profile.ProfileScreen
import app.agentsetu.ui.welcome.WelcomeScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class RootState { LOADING, NEEDS_DISCLAIMER, NEEDS_PROFILE, READY }

@HiltViewModel
class RootViewModel @Inject constructor(
    private val settings: AppSettings,
    db: AgentSetuDatabase,
    seedLoader: SeedLoader,
) : ViewModel() {

    val state = combine(settings.disclaimerAccepted, db.userProfileDao().observe()) { accepted, profile ->
        when {
            !accepted -> RootState.NEEDS_DISCLAIMER
            profile == null -> RootState.NEEDS_PROFILE
            else -> RootState.READY
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RootState.LOADING)

    init {
        viewModelScope.launch {
            // Local-only log; nothing leaves the phone. Screens show "No rule" until rules load.
            runCatching { seedLoader.ensureSeeded() }.onFailure { Log.e("AgentSetu", "Seeding failed", it) }
        }
    }

    fun acceptDisclaimer() = settings.acceptDisclaimer()
}

/** Disclaimer first, then the profile (staff type decides which rules apply), then the app. */
@Composable
fun AgentSetuRoot(viewModel: RootViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (state) {
        RootState.LOADING -> Unit
        RootState.NEEDS_DISCLAIMER -> WelcomeScreen(onAccept = viewModel::acceptDisclaimer)
        // Saving the profile flips the state to READY by itself.
        RootState.NEEDS_PROFILE -> ProfileScreen(onBack = null, onSaved = {})
        RootState.READY -> MainNavigation()
    }
}
