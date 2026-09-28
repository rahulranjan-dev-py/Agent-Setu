package app.agentsetu.ui.rates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.agentsetu.R
import app.agentsetu.core.model.CommissionBasis
import app.agentsetu.core.model.StaffType
import app.agentsetu.data.db.AgentSetuDatabase
import app.agentsetu.data.db.CommissionRuleEntity
import app.agentsetu.data.db.ProductEntity
import app.agentsetu.data.seed.SeedLoader
import app.agentsetu.ui.common.AppScaffold
import app.agentsetu.ui.common.CheckRow
import app.agentsetu.ui.common.display
import app.agentsetu.ui.common.labelRes
import app.agentsetu.ui.common.localized
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RuleView(val rule: CommissionRuleEntity, val product: ProductEntity?, val ended: Boolean)

@HiltViewModel
class RatesViewModel @Inject constructor(
    db: AgentSetuDatabase,
    private val seedLoader: SeedLoader,
) : ViewModel() {
    val showHistory = MutableStateFlow(false)

    val rules = combine(
        db.commissionRuleDao().observeAllVersions(),
        db.productDao().observeAll(),
        showHistory,
    ) { rules, products, history ->
        val today = LocalDate.now()
        rules.map { rule ->
            // A family rule ("TD") has no product row of its own; show it under its first member.
            val product = products.firstOrNull { it.code == rule.productCode }
                ?: products.firstOrNull { it.code.startsWith(rule.productCode + "_") }
            RuleView(rule, product, ended = rule.effectiveTo?.isBefore(today) == true)
        }.filter { history || (!it.rule.deleted && !it.ended) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun resetToSample() {
        viewModelScope.launch { seedLoader.resetRatesToSample() }
    }
}

@Composable
fun RatesScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onAdd: () -> Unit,
    viewModel: RatesViewModel = hiltViewModel(),
) {
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val showHistory by viewModel.showHistory.collectAsStateWithLifecycle()
    var confirmReset by remember { mutableStateOf(false) }

    AppScaffold(
        title = stringResource(R.string.rates_title),
        onBack = onBack,
        actions = {
            IconButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.rule_add_title))
            }
            IconButton(onClick = { confirmReset = true }) {
                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.rates_reset))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Text(
                        stringResource(R.string.rates_sample_banner),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            item {
                Text(stringResource(R.string.rates_add_hint), style = MaterialTheme.typography.bodyMedium)
            }
            item {
                CheckRow(showHistory, { viewModel.showHistory.value = it }, stringResource(R.string.rates_show_history))
            }
            items(rules, key = { it.rule.id }) { view ->
                val editable = !view.rule.deleted && !view.ended
                RuleCard(view, modifier = if (editable) Modifier.clickable { onEdit(view.rule.id) } else Modifier)
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.rates_reset)) },
            text = { Text(stringResource(R.string.rates_reset_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetToSample()
                    confirmReset = false
                }) { Text(stringResource(R.string.rates_reset)) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
fun rateText(rule: CommissionRuleEntity): String {
    val plain = BigDecimal(rule.rate).stripTrailingZeros().toPlainString()
    return if (rule.basis == CommissionBasis.FLAT_PER_CASE) stringResource(R.string.rates_flat, plain) else stringResource(R.string.rates_rate, plain)
}

@Composable
private fun RuleCard(view: RuleView, modifier: Modifier) {
    val r = view.rule
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val productName = view.product?.let { localized(it.nameEn, it.nameHi) } ?: r.productCode
            // A family rule ("TD") covers every term: "Time Deposit (all terms)".
            val title = if (view.product == null || view.product.code == r.productCode) {
                productName
            } else {
                stringResource(R.string.rates_family, productName.substringBefore(" - "))
            }
            Text(title, style = MaterialTheme.typography.titleMedium)
            val details = buildList {
                r.policyCategory.labelRes()?.let { add(stringResource(it)) }
                when (r.yearOfPolicy) {
                    1 -> add(stringResource(R.string.rates_year_1))
                    2 -> add(stringResource(R.string.rates_year_2))
                }
                termText(r.minPremiumTermYears, r.maxPremiumTermYears)?.let { add(it) }
            }
            if (details.isNotEmpty()) Text(details.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
            Text(rateText(r), style = MaterialTheme.typography.headlineSmall)
            val staff = r.staffTypes.mapNotNull { name -> StaffType.entries.firstOrNull { it.name == name } }
            Text(staff.map { stringResource(it.labelRes()) }.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
            Text(
                r.effectiveTo?.let { stringResource(R.string.rates_from_to, r.effectiveFrom.display(), it.display()) }
                    ?: stringResource(R.string.rates_from, r.effectiveFrom.display()),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(stringResource(R.string.rates_order, r.orderRef), style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (r.isSample) Badge(stringResource(R.string.rates_badge_sample), MaterialTheme.colorScheme.secondary)
                if (view.product?.isCustom == true) Badge(stringResource(R.string.rates_badge_custom), MaterialTheme.colorScheme.secondary)
                if (r.verified) {
                    Badge(stringResource(R.string.rates_badge_verified), MaterialTheme.colorScheme.primary)
                } else {
                    Badge(stringResource(R.string.rates_badge_unverified), MaterialTheme.colorScheme.error)
                }
                if (r.deleted) Badge(stringResource(R.string.rates_badge_replaced), MaterialTheme.colorScheme.outline)
                else if (view.ended) Badge(stringResource(R.string.rates_badge_ended), MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun termText(min: Int?, max: Int?): String? = when {
    min != null && max != null -> stringResource(R.string.rates_term_range, min.toString(), max.toString())
    max != null -> stringResource(R.string.rates_term_upto, max.toString())
    min != null -> stringResource(R.string.rates_term_from, min.toString())
    else -> null
}

@Composable
private fun Badge(text: String, color: Color) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = color)
}
