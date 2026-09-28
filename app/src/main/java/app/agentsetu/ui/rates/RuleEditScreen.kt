package app.agentsetu.ui.rates

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
import app.agentsetu.core.format.IndianFormat
import app.agentsetu.core.input.AmountInput
import app.agentsetu.core.model.CommissionBasis
import app.agentsetu.data.db.AgentSetuDatabase
import app.agentsetu.data.db.CommissionRuleEntity
import app.agentsetu.data.repo.CommissionRepository
import app.agentsetu.ui.common.AppScaffold
import app.agentsetu.ui.common.CheckRow
import app.agentsetu.ui.common.DateField
import app.agentsetu.ui.common.FormField
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class RuleEditViewModel @Inject constructor(
    private val db: AgentSetuDatabase,
    private val commission: CommissionRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val ruleId: String = checkNotNull(savedStateHandle["id"])

    var rule by mutableStateOf<CommissionRuleEntity?>(null)
        private set
    var rate by mutableStateOf("")
    var from by mutableStateOf(IndianFormat.date(LocalDate.now()))
    var orderRef by mutableStateOf("")
    var verified by mutableStateOf(false)
    var showErrors by mutableStateOf(false)
        private set
    var ruleEnded by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            db.commissionRuleDao().get(ruleId)?.let {
                rule = it
                rate = BigDecimal(it.rate).stripTrailingZeros().toPlainString()
                // Sample order references say "VERIFY"; make the user type the real one.
                orderRef = if (it.isSample) "" else it.orderRef
                verified = it.verified
            }
        }
    }

    val isFlat get() = rule?.basis == CommissionBasis.FLAT_PER_CASE
    private fun parsedRate(): BigDecimal? = if (isFlat) AmountInput.parse(rate) else AmountInput.parsePercent(rate)
    val rateInvalid get() = parsedRate() == null
    val fromInvalid get() = IndianFormat.parseDate(from) == null
    val orderMissing get() = orderRef.isBlank()

    fun save(onSaved: () -> Unit) {
        showErrors = true
        ruleEnded = false
        val newRate = parsedRate() ?: return
        val start = IndianFormat.parseDate(from) ?: return
        if (orderMissing) return
        viewModelScope.launch {
            try {
                commission.changeRate(ruleId, newRate, start, orderRef.trim(), verified)
                onSaved()
            } catch (e: IllegalArgumentException) {
                ruleEnded = true
            }
        }
    }
}

@Composable
fun RuleEditScreen(onBack: () -> Unit, onSaved: () -> Unit, viewModel: RuleEditViewModel = hiltViewModel()) {
    val vm = viewModel
    val rule = vm.rule ?: return
    AppScaffold(title = stringResource(R.string.rule_edit_title), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.rule_current, rateText(rule)), style = MaterialTheme.typography.titleMedium)
            if (rule.notes.isNotBlank()) Text(rule.notes, style = MaterialTheme.typography.bodyMedium)
            FormField(
                vm.rate, { vm.rate = it },
                stringResource(if (vm.isFlat) R.string.rule_new_amount else R.string.rule_new_rate),
                error = if (vm.showErrors && vm.rateInvalid) {
                    stringResource(if (vm.isFlat) R.string.error_amount else R.string.error_percent)
                } else {
                    null
                },
                keyboardType = KeyboardType.Decimal,
            )
            DateField(
                vm.from, { vm.from = it }, stringResource(R.string.rule_from),
                error = if (vm.showErrors && vm.fromInvalid) stringResource(R.string.error_date) else null,
            )
            FormField(
                vm.orderRef, { vm.orderRef = it }, stringResource(R.string.rule_order),
                error = if (vm.showErrors && vm.orderMissing) stringResource(R.string.error_required) else null,
            )
            CheckRow(vm.verified, { vm.verified = it }, stringResource(R.string.rule_verified))
            Text(stringResource(R.string.rule_history_note), style = MaterialTheme.typography.bodyMedium)
            if (vm.ruleEnded) {
                Text(stringResource(R.string.rule_error_ended), color = MaterialTheme.colorScheme.error)
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
