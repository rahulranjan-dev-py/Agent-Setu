package app.agentsetu.ui.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.agentsetu.R
import app.agentsetu.core.catalogue.DefaultProducts
import app.agentsetu.core.format.IndianFormat
import app.agentsetu.core.input.AmountInput
import app.agentsetu.data.db.AgentSetuDatabase
import app.agentsetu.data.db.InterestRateEntity
import app.agentsetu.data.repo.InterestRateRepository
import app.agentsetu.ui.common.AppScaffold
import app.agentsetu.ui.common.CheckRow
import app.agentsetu.ui.common.DateField
import app.agentsetu.ui.common.FormField
import app.agentsetu.ui.common.display
import app.agentsetu.ui.common.localized
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class InterestRatesViewModel @Inject constructor(db: AgentSetuDatabase) : ViewModel() {
    private val order = DefaultProducts.all.map { it.code }

    val rates = db.interestRateDao().observeAllVersions()
        .map { rows ->
            val today = LocalDate.now()
            rows.filter { !it.deleted && (it.effectiveTo == null || !it.effectiveTo.isBefore(today)) }
                .sortedBy { order.indexOf(it.scheme) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun InterestRatesScreen(onBack: () -> Unit, onEdit: (String) -> Unit, viewModel: InterestRatesViewModel = hiltViewModel()) {
    val rates by viewModel.rates.collectAsStateWithLifecycle()
    // Rates change quarterly: warn when the newest one is more than about a quarter old.
    val newest = rates.maxOfOrNull { it.effectiveFrom }
    val stale = newest != null && newest.isBefore(LocalDate.now().minusMonths(3))

    AppScaffold(title = stringResource(R.string.rates_interest_title), onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Text(stringResource(R.string.rates_interest_banner), Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (stale) {
                item {
                    Text(
                        stringResource(R.string.rates_interest_stale, newest!!.display()),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            items(rates, key = { it.id }) { rate -> RateCard(rate) { onEdit(rate.id) } }
        }
    }
}

@Composable
private fun RateCard(rate: InterestRateEntity, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val name = DefaultProducts.byCode(rate.scheme)?.let { localized(it.nameEn, it.nameHi) } ?: rate.scheme
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.rates_rate, BigDecimal(rate.rate).stripTrailingZeros().toPlainString()),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(stringResource(R.string.rates_from, rate.effectiveFrom.display()), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(
                    when {
                        rate.isSample -> R.string.rates_badge_sample
                        rate.verified -> R.string.rates_badge_verified
                        else -> R.string.rates_badge_unverified
                    },
                ),
                style = MaterialTheme.typography.labelLarge,
                color = if (rate.verified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
    }
}

@HiltViewModel
class InterestRateEditViewModel @Inject constructor(
    private val db: AgentSetuDatabase,
    private val repo: InterestRateRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val id: String = checkNotNull(savedStateHandle["id"])
    var current by mutableStateOf<InterestRateEntity?>(null)
        private set
    var rate by mutableStateOf("")
    var from by mutableStateOf(IndianFormat.date(LocalDate.now()))
    var source by mutableStateOf("")
    var verified by mutableStateOf(false)
    var tried by mutableStateOf(false)
        private set
    var ended by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            db.interestRateDao().get(id)?.let {
                current = it
                rate = BigDecimal(it.rate).stripTrailingZeros().toPlainString()
                source = if (it.isSample) "" else it.source
                verified = it.verified
            }
        }
    }

    val rateInvalid get() = AmountInput.parsePercent(rate) == null
    val fromInvalid get() = IndianFormat.parseDate(from) == null
    val sourceMissing get() = source.isBlank()

    fun save(onSaved: () -> Unit) {
        tried = true
        ended = false
        val newRate = AmountInput.parsePercent(rate)
        val start = IndianFormat.parseDate(from)
        if (newRate == null || start == null || sourceMissing) return
        viewModelScope.launch {
            try {
                repo.changeRate(id, newRate, start, source.trim(), verified)
                onSaved()
            } catch (e: IllegalArgumentException) {
                ended = true
            }
        }
    }
}

@Composable
fun InterestRateEditScreen(onBack: () -> Unit, onSaved: () -> Unit, viewModel: InterestRateEditViewModel = hiltViewModel()) {
    val vm = viewModel
    val current = vm.current ?: return
    val name = DefaultProducts.byCode(current.scheme)?.let { localized(it.nameEn, it.nameHi) } ?: current.scheme
    AppScaffold(title = stringResource(R.string.rate_edit_title), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(name, style = MaterialTheme.typography.titleLarge)
            FormField(
                vm.rate, { vm.rate = it }, stringResource(R.string.calc_rate),
                error = if (vm.tried && vm.rateInvalid) stringResource(R.string.error_percent) else null,
                keyboardType = KeyboardType.Decimal,
            )
            DateField(
                vm.from, { vm.from = it }, stringResource(R.string.rule_from),
                error = if (vm.tried && vm.fromInvalid) stringResource(R.string.error_date) else null,
            )
            FormField(
                vm.source, { vm.source = it }, stringResource(R.string.rate_source),
                error = if (vm.tried && vm.sourceMissing) stringResource(R.string.error_required) else null,
            )
            CheckRow(vm.verified, { vm.verified = it }, stringResource(R.string.rate_verified))
            Text(stringResource(R.string.rule_history_note), style = MaterialTheme.typography.bodyMedium)
            if (vm.ended) Text(stringResource(R.string.rate_error_ended), color = MaterialTheme.colorScheme.error)
            Button(
                onClick = { vm.save(onSaved) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) { Text(stringResource(R.string.action_save)) }
        }
    }
}
