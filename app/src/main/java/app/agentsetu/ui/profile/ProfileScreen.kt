package app.agentsetu.ui.profile

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.agentsetu.R
import app.agentsetu.core.model.StaffType
import app.agentsetu.data.db.AgentSetuDatabase
import app.agentsetu.data.db.UserProfileEntity
import app.agentsetu.ui.common.AppScaffold
import app.agentsetu.ui.common.FormField
import app.agentsetu.ui.common.RadioGroup
import app.agentsetu.ui.common.SectionTitle
import app.agentsetu.ui.common.labelRes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val db: AgentSetuDatabase,
) : ViewModel() {
    var staffType by mutableStateOf<StaffType?>(null)
    var designation by mutableStateOf("")
    var office by mutableStateOf("")
    var division by mutableStateOf("")
    private var existing: UserProfileEntity? = null

    init {
        viewModelScope.launch {
            db.userProfileDao().observe().first()?.let {
                existing = it
                staffType = it.staffType
                designation = it.designation
                office = it.officeName
                division = it.division
            }
        }
    }

    fun save(onSaved: () -> Unit) {
        val type = staffType ?: return
        val language = AppCompatDelegate.getApplicationLocales().toLanguageTags().ifBlank { "system" }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val profile = existing?.copy(
                staffType = type,
                designation = designation.trim(),
                officeName = office.trim(),
                division = division.trim(),
                language = language,
                updatedAt = now,
            ) ?: UserProfileEntity(
                staffType = type,
                designation = designation.trim(),
                officeName = office.trim(),
                division = division.trim(),
                language = language,
                createdAt = now,
                updatedAt = now,
            )
            db.userProfileDao().upsert(profile)
            existing = profile
            onSaved()
        }
    }
}

/** Used during onboarding (no back button) and from Settings. */
@Composable
fun ProfileScreen(onBack: (() -> Unit)?, onSaved: () -> Unit, viewModel: ProfileViewModel = hiltViewModel()) {
    AppScaffold(title = stringResource(R.string.profile_title), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.profile_intro), style = MaterialTheme.typography.bodyLarge)
            SectionTitle(stringResource(R.string.profile_staff_type))
            RadioGroup(
                options = StaffType.entries,
                selected = viewModel.staffType,
                label = { stringResource(it.labelRes()) },
                onSelect = { viewModel.staffType = it },
            )
            FormField(viewModel.designation, { viewModel.designation = it }, stringResource(R.string.profile_designation))
            FormField(viewModel.office, { viewModel.office = it }, stringResource(R.string.profile_office))
            FormField(viewModel.division, { viewModel.division = it }, stringResource(R.string.profile_division))
            Button(
                onClick = { viewModel.save(onSaved) },
                enabled = viewModel.staffType != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}
