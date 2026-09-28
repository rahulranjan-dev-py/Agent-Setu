package app.agentsetu.ui.commission

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.agentsetu.R
import app.agentsetu.core.format.IndianFormat
import app.agentsetu.core.input.AmountInput
import app.agentsetu.core.ledger.Ledger
import app.agentsetu.core.model.CommissionStatus
import app.agentsetu.core.model.Money
import app.agentsetu.data.db.AgentSetuDatabase
import app.agentsetu.data.db.LedgerRow
import app.agentsetu.ui.common.AppScaffold
import app.agentsetu.ui.common.DateField
import app.agentsetu.ui.common.FormField
import app.agentsetu.ui.common.currentLocale
import app.agentsetu.ui.common.editableAmount
import app.agentsetu.ui.common.labelRes
import app.agentsetu.ui.common.localized
import app.agentsetu.ui.common.rupees
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CommissionViewModel @Inject constructor(private val db: AgentSetuDatabase) : ViewModel() {
    val month = MutableStateFlow(YearMonth.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    val rows = month
        .flatMapLatest { db.commissionEntryDao().observeLedger(it.toString()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun shiftMonth(by: Long) {
        month.value = month.value.plusMonths(by)
    }

    /** Returns false if the input is invalid; the dialog then stays open. */
    fun markReceived(entryId: String, amountText: String, dateText: String): Boolean {
        val amount = AmountInput.parse(amountText) ?: return false
        val date = IndianFormat.parseDate(dateText) ?: return false
        viewModelScope.launch {
            val entry = db.commissionEntryDao().get(entryId) ?: return@launch
            val received = Money.toPaise(amount)
            db.commissionEntryDao().upsert(
                entry.copy(
                    receivedPaise = received,
                    receivedDate = date,
                    status = Ledger.statusAfterReceipt(entry.expectedPaise, received),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
        return true
    }
}

@Composable
fun CommissionScreen(viewModel: CommissionViewModel = hiltViewModel()) {
    val month by viewModel.month.collectAsStateWithLifecycle()
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<LedgerRow?>(null) }
    val totals = Ledger.totals(rows.map { Ledger.Line(it.expectedPaise, it.receivedPaise, it.status) })
    val monthLabel = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", currentLocale()))

    AppScaffold(title = stringResource(R.string.commission_title), aboveBottomBar = true) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = { viewModel.shiftMonth(-1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.previous_month))
                    }
                    Text(monthLabel, style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = { viewModel.shiftMonth(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.next_month))
                    }
                }
            }
            item {
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        TotalLine(stringResource(R.string.commission_expected), rupees(totals.expectedPaise))
                        TotalLine(stringResource(R.string.commission_received), rupees(totals.receivedPaise))
                        TotalLine(stringResource(R.string.commission_shortfall), rupees(totals.shortfallPaise))
                        if (totals.noRuleCount > 0) {
                            Text(
                                stringResource(R.string.commission_no_rule_count, totals.noRuleCount),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            if (rows.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.commission_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            }
            items(rows, key = { it.id }) { row ->
                LedgerItem(row, onClick = { editing = row })
                HorizontalDivider()
            }
        }
    }

    editing?.let { row ->
        ReceivedDialog(
            row = row,
            onDismiss = { editing = null },
            onSave = { amount, date -> if (viewModel.markReceived(row.id, amount, date)) editing = null },
        )
    }
}

@Composable
private fun TotalLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun LedgerItem(row: LedgerRow, onClick: () -> Unit) {
    val product = localized(row.productNameEn, row.productNameHi)
    ListItem(
        headlineContent = {
            Text(row.customerName.ifBlank { stringResource(R.string.customer_deleted) }, style = MaterialTheme.typography.titleMedium)
        },
        supportingContent = {
            Text(row.policyYear?.let { stringResource(R.string.commission_line_detail, product, it) } ?: product)
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(row.expectedPaise?.let(::rupees) ?: "-", style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(row.status.labelRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (row.status) {
                        CommissionStatus.RECEIVED -> MaterialTheme.colorScheme.primary
                        CommissionStatus.NO_RULE -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.secondary
                    },
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun ReceivedDialog(row: LedgerRow, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var amount by remember { mutableStateOf((row.receivedPaise ?: row.expectedPaise)?.let(::editableAmount).orEmpty()) }
    var date by remember { mutableStateOf(IndianFormat.date(row.receivedDate ?: LocalDate.now())) }
    var tried by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.commission_mark_received)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(row.customerName.ifBlank { stringResource(R.string.customer_deleted) }, style = MaterialTheme.typography.bodyLarge)
                FormField(
                    amount, { amount = it }, stringResource(R.string.commission_received_amount),
                    error = if (tried && AmountInput.parse(amount) == null) stringResource(R.string.error_amount) else null,
                    keyboardType = KeyboardType.Decimal,
                )
                DateField(
                    date, { date = it }, stringResource(R.string.commission_received_date),
                    error = if (tried && IndianFormat.parseDate(date) == null) stringResource(R.string.error_date) else null,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                tried = true
                onSave(amount, date)
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
