package app.agentsetu.ui.customers

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.agentsetu.R
import app.agentsetu.core.input.MobileNumber
import app.agentsetu.data.db.AgentSetuDatabase
import app.agentsetu.data.db.CustomerEntity
import app.agentsetu.ui.common.AppScaffold
import app.agentsetu.ui.common.CheckRow
import app.agentsetu.ui.common.FormField
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class CustomerEditViewModel @Inject constructor(
    private val db: AgentSetuDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val customerId: String? = savedStateHandle["id"]
    private var existing: CustomerEntity? = null

    val isNew get() = customerId == null
    var name by mutableStateOf("")
    var mobile by mutableStateOf("")
    var village by mutableStateOf("")
    var notes by mutableStateOf("")
    var consent by mutableStateOf(false)
    var showErrors by mutableStateOf(false)
        private set

    val nameMissing get() = name.isBlank()
    val mobileInvalid get() = mobile.isNotBlank() && MobileNumber.normalize(mobile) == null
    val consentMissing get() = !consent

    init {
        customerId?.let { id ->
            viewModelScope.launch {
                db.customerDao().get(id)?.let {
                    existing = it
                    name = it.name
                    mobile = it.mobile.orEmpty()
                    village = it.village.orEmpty()
                    notes = it.notes
                    consent = it.consentGiven
                }
            }
        }
    }

    fun save(onSaved: (String) -> Unit) {
        showErrors = true
        if (nameMissing || mobileInvalid || consentMissing) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val cleanMobile = mobile.takeIf { it.isNotBlank() }?.let(MobileNumber::normalize)
            val customer = existing?.copy(
                name = name.trim(),
                mobile = cleanMobile,
                village = village.trim().ifBlank { null },
                notes = notes.trim(),
                consentGiven = true,
                updatedAt = now,
            ) ?: CustomerEntity(
                name = name.trim(),
                mobile = cleanMobile,
                village = village.trim().ifBlank { null },
                notes = notes.trim(),
                consentGiven = true,
                createdAt = now,
                updatedAt = now,
            )
            db.customerDao().upsert(customer)
            onSaved(customer.id)
        }
    }
}

@Composable
fun CustomerEditScreen(onBack: () -> Unit, onSaved: (String) -> Unit, viewModel: CustomerEditViewModel = hiltViewModel()) {
    val vm = viewModel
    val required = stringResource(R.string.error_required)
    AppScaffold(
        title = stringResource(if (vm.isNew) R.string.customer_add else R.string.customer_edit),
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FormField(
                vm.name, { vm.name = it }, stringResource(R.string.customer_name),
                error = if (vm.showErrors && vm.nameMissing) required else null,
            )
            FormField(
                vm.mobile, { vm.mobile = it }, stringResource(R.string.customer_mobile),
                error = if (vm.showErrors && vm.mobileInvalid) stringResource(R.string.error_mobile) else null,
                keyboardType = KeyboardType.Phone,
            )
            FormField(vm.village, { vm.village = it }, stringResource(R.string.customer_village))
            FormField(vm.notes, { vm.notes = it }, stringResource(R.string.customer_notes), singleLine = false)
            Text(
                stringResource(R.string.customer_no_id_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            CheckRow(vm.consent, { vm.consent = it }, stringResource(R.string.customer_consent))
            if (vm.showErrors && vm.consentMissing) {
                Text(
                    stringResource(R.string.customer_consent_required),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(
                onClick = { vm.save(onSaved) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}
