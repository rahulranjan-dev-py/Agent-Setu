package app.agentsetu.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.agentsetu.R
import app.agentsetu.core.calc.Calculators
import app.agentsetu.core.input.AmountInput
import app.agentsetu.core.model.Money
import app.agentsetu.data.db.InterestRateEntity
import app.agentsetu.data.repo.InterestRateRepository
import app.agentsetu.share.ShareCard
import app.agentsetu.ui.common.AppScaffold
import app.agentsetu.ui.common.FormField
import app.agentsetu.ui.common.display
import app.agentsetu.ui.common.rupees
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.launch

enum class CalcKind { TD, RD, MIS }

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val rates: InterestRateRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val kind: CalcKind = CalcKind.valueOf(checkNotNull(savedStateHandle.get<String>("kind")))
    var amount by mutableStateOf("")
    var rate by mutableStateOf("")
    var years by mutableStateOf(5)
        private set
    var tableRate by mutableStateOf<InterestRateEntity?>(null)
        private set

    init {
        loadRate()
    }

    fun selectYears(value: Int) {
        years = value
        loadRate()
    }

    private fun scheme() = when (kind) {
        CalcKind.TD -> "TD_${years}Y"
        CalcKind.RD -> "RD_5Y"
        CalcKind.MIS -> "MIS"
    }

    /** Pre-fills the rate from the user's own rate table; they can still type another one. */
    private fun loadRate() {
        viewModelScope.launch {
            val found = rates.inForce(scheme())
            tableRate = found
            rate = found?.rate?.let { BigDecimal(it).stripTrailingZeros().toPlainString() }.orEmpty()
        }
    }
}

/** One result line: label and amount in paise. */
private data class Line(val label: Int, val paise: Long)

@Composable
fun CalculatorScreen(onBack: () -> Unit, viewModel: CalculatorViewModel = hiltViewModel()) {
    val vm = viewModel
    val context = LocalContext.current
    val title = stringResource(
        when (vm.kind) {
            CalcKind.TD -> R.string.tools_calc_td
            CalcKind.RD -> R.string.tools_calc_rd
            CalcKind.MIS -> R.string.tools_calc_mis
        },
    )
    val amount = AmountInput.parse(vm.amount)?.takeIf { it.signum() > 0 }
    val rate = AmountInput.parsePercent(vm.rate)?.takeIf { it.signum() > 0 }
    val lines: List<Line>? = if (amount == null || rate == null) null else when (vm.kind) {
        CalcKind.TD -> Calculators.timeDeposit(amount, rate, vm.years).let {
            listOf(
                Line(R.string.calc_td_yearly, Money.toPaise(it.yearlyInterest)),
                Line(R.string.calc_total_interest, Money.toPaise(it.totalInterest)),
                Line(R.string.calc_principal_back, Money.toPaise(it.principal)),
            )
        }
        CalcKind.RD -> Calculators.recurringDeposit(amount, rate).let {
            listOf(
                Line(R.string.calc_rd_deposited, Money.toPaise(it.deposited)),
                Line(R.string.calc_total_interest, Money.toPaise(it.interest)),
                Line(R.string.calc_rd_maturity, Money.toPaise(it.maturity)),
            )
        }
        CalcKind.MIS -> Calculators.monthlyIncome(amount, rate).let {
            listOf(
                Line(R.string.calc_mis_monthly, Money.toPaise(it.monthlyInterest)),
                Line(R.string.calc_total_interest, Money.toPaise(it.totalInterest)),
                Line(R.string.calc_principal_back, Money.toPaise(it.principal)),
            )
        }
    }

    // Resolved here because share runs outside composition.
    val labels = mapOf(
        R.string.calc_td_yearly to stringResource(R.string.calc_td_yearly),
        R.string.calc_total_interest to stringResource(R.string.calc_total_interest),
        R.string.calc_principal_back to stringResource(R.string.calc_principal_back),
        R.string.calc_rd_deposited to stringResource(R.string.calc_rd_deposited),
        R.string.calc_rd_maturity to stringResource(R.string.calc_rd_maturity),
        R.string.calc_mis_monthly to stringResource(R.string.calc_mis_monthly),
    )
    val amountLine = amount?.let {
        if (vm.kind == CalcKind.RD) stringResource(R.string.calc_card_monthly, rupees(Money.toPaise(it)))
        else stringResource(R.string.calc_card_amount, rupees(Money.toPaise(it)))
    }
    val rateLine = rate?.let { stringResource(R.string.calc_card_rate, it.stripTrailingZeros().toPlainString()) }
    val termLine = if (vm.kind == CalcKind.TD) stringResource(R.string.calc_years_value, vm.years) else null
    val footer = listOf(
        stringResource(R.string.calc_estimate_note),
        stringResource(R.string.share_footer_verify),
        stringResource(R.string.share_footer_unofficial),
    )

    AppScaffold(title = title, onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FormField(
                vm.amount, { vm.amount = it },
                stringResource(if (vm.kind == CalcKind.RD) R.string.business_instalment else R.string.business_deposit),
                keyboardType = KeyboardType.Decimal,
            )
            if (vm.kind == CalcKind.TD) {
                Text(stringResource(R.string.calc_years), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 3, 5).forEach { y ->
                        FilterChip(
                            selected = vm.years == y,
                            onClick = { vm.selectYears(y) },
                            label = { Text(stringResource(R.string.calc_years_value, y)) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }
            }
            val table = vm.tableRate
            FormField(
                vm.rate, { vm.rate = it }, stringResource(R.string.calc_rate),
                supporting = when {
                    table == null -> stringResource(R.string.calc_rate_none)
                    table.isSample || !table.verified -> stringResource(R.string.calc_rate_sample)
                    else -> stringResource(R.string.calc_rate_from_table, table.effectiveFrom.display())
                },
                keyboardType = KeyboardType.Decimal,
            )
            if (lines != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        lines.forEach { line ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(line.label), style = MaterialTheme.typography.bodyLarge)
                                Text(rupees(line.paise), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
                Text(stringResource(R.string.calc_estimate_note), style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = {
                        val body = listOfNotNull(amountLine, termLine, rateLine) +
                            lines.map { "${labels.getValue(it.label)}: ${rupees(it.paise)}" }
                        ShareCard.share(context, if (termLine != null) "$title · $termLine" else title, body, footer)
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) { Text(stringResource(R.string.action_share_image)) }
            }
        }
    }
}
