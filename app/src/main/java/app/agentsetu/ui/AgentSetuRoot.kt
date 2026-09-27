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
import app.agentsetu.security.AppLock
import app.agentsetu.ui.lock.LockScreen
import app.agentsetu.ui.lock.PinSetupScreen
import app.agentsetu.ui.nav.MainNavigation
import app.agentsetu.ui.profile.ProfileScreen
import app.agentsetu.ui.welcome.WelcomeScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class RootState { LOADING, NEEDS_DISCLAIMER, NEEDS_PROFILE, NEEDS_PIN_CHOICE, LOCKED, READY }

@HiltViewModel
class RootViewModel @Inject constructor(
    private val settings: AppSettings,
    val appLock: AppLock,
    db: AgentSetuDatabase,
    seedLoader: SeedLoader,
) : ViewModel() {

    val state = combine(
        settings.disclaimerAccepted,
        db.userProfileDao().observe(),
        appLock.choiceMade,
        appLock.locked,
    ) { accepted, profile, choiceMade, locked ->
        when {
            // The lock comes first: nothing, not even onboarding answers, shows without the PIN.
            locked -> RootState.LOCKED
            !accepted -> RootState.NEEDS_DISCLAIMER
            profile == null -> RootState.NEEDS_PROFILE
            !choiceMade -> RootState.NEEDS_PIN_CHOICE
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

/** Gives screens inside navigation access to the app lock. */
@HiltViewModel
class AgentSetuRootLock @Inject constructor(val appLock: AppLock) : ViewModel()

/** Lock (if a PIN is set), disclaimer, profile, optional PIN, then the app. */
@Composable
fun AgentSetuRoot(viewModel: RootViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (state) {
        RootState.LOADING -> Unit
        RootState.LOCKED -> LockScreen(viewModel.appLock)
        RootState.NEEDS_DISCLAIMER -> WelcomeScreen(onAccept = viewModel::acceptDisclaimer)
        // Saving the profile flips the state by itself.
        RootState.NEEDS_PROFILE -> ProfileScreen(onBack = null, onSaved = {})
        RootState.NEEDS_PIN_CHOICE -> PinSetupScreen(
            appLock = viewModel.appLock,
            onDone = {},
            onSkip = viewModel.appLock::skipForNow,
            onBack = null,
        )
        RootState.READY -> MainNavigation()
    }
}
