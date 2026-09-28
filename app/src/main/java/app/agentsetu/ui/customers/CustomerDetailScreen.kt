package app.agentsetu.ui.customers

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.agentsetu.R
import app.agentsetu.core.format.IndianFormat
import app.agentsetu.core.input.MobileNumber
import app.agentsetu.core.ledger.Ledger
import app.agentsetu.core.model.CommissionStatus
import app.agentsetu.data.db.LedgerRow
import app.agentsetu.ui.common.currentLocale
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import app.agentsetu.data.db.AgentSetuDatabase
import app.agentsetu.data.db.HoldingEntity
import app.agentsetu.data.db.ProductEntity
import app.agentsetu.data.repo.ReminderRepository
import app.agentsetu.ui.common.AppScaffold
import app.agentsetu.ui.common.DateField
import app.agentsetu.ui.common.FormField
import app.agentsetu.ui.common.display
import app.agentsetu.ui.common.labelRes
import app.agentsetu.ui.common.localized
import app.agentsetu.ui.common.openUri
import app.agentsetu.ui.common.rupees
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HoldingView(val holding: HoldingEntity, val product: ProductEntity?)

@HiltViewModel
class CustomerDetailViewModel @Inject constructor(
    db: AgentSetuDatabase,
    private val reminders: ReminderRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val customerId: String = checkNotNull(savedStateHandle["id"])

    val customer = db.customerDao().observe(customerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val holdings = combine(db.holdingDao().observeForCustomer(customerId), db.productDao().observeAll()) { holdings, products ->
        val byId = products.associateBy { it.id }
        holdings.map { HoldingView(it, byId[it.productId]) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Every commission entry recorded for this customer, newest month first. */
    val ledger = db.commissionEntryDao().observeLedgerForCustomer(customerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Returns false for an unreadable date. */
    fun addFollowUp(dateText: String, note: String): Boolean {
        val date = IndianFormat.parseDate(dateText) ?: return false
        viewModelScope.launch { reminders.addFollowUp(customerId, date, note) }
        return true
    }
}

@Composable
fun CustomerDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onAddBusiness: (String) -> Unit,
    onOpenHolding: (String) -> Unit,
    viewModel: CustomerDetailViewModel = hiltViewModel(),
) {
    val customer by viewModel.customer.collectAsStateWithLifecycle()
    val holdings by viewModel.holdings.collectAsStateWithLifecycle()
    val ledger by viewModel.ledger.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var addingFollowUp by remember { mutableStateOf(false) }
    val c = customer ?: return

    AppScaffold(
        title = c.name,
        onBack = onBack,
        actions = {
            IconButton(onClick = { onEdit(c.id) }) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOfNotNull(c.village, c.mobile).filter { it.isNotBlank() }.forEach {
                        Text(it, style = MaterialTheme.typography.bodyLarge)
                    }
                    if (c.notes.isNotBlank()) Text(c.notes, style = MaterialTheme.typography.bodyMedium)
                }
            }
            c.mobile?.let { mobile ->
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { context.openUri(MobileNumber.dialUri(mobile), Intent.ACTION_DIAL) },
                            modifier = Modifier.heightIn(min = 52.dp),
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = null)
                            Text(stringResource(R.string.customer_call), modifier = Modifier.padding(start = 8.dp))
                        }
                        OutlinedButton(
                            onClick = { context.openUri(MobileNumber.whatsAppUri(mobile)) },
                            modifier = Modifier.heightIn(min = 52.dp),
                        ) {
                            Text(stringResource(R.string.customer_whatsapp))
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = { onAddBusiness(c.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Text(stringResource(R.string.customer_add_business))
                }
            }
            item {
                OutlinedButton(
                    onClick = { addingFollowUp = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                ) {
                    Text(stringResource(R.string.followup_add))
                }
            }
            item { Text(stringResource(R.string.customer_holdings), style = MaterialTheme.typography.titleMedium) }
            if (holdings.isEmpty()) {
                item { Text(stringResource(R.string.customer_no_holdings), style = MaterialTheme.typography.bodyLarge) }
            }
            items(holdings, key = { it.holding.id }) { view -> HoldingCard(view, onClick = { onOpenHolding(view.holding.id) }) }
            item { Text(stringResource(R.string.customer_ledger), style = MaterialTheme.typography.titleMedium) }
            if (ledger.isEmpty()) {
                item { Text(stringResource(R.string.customer_ledger_empty), style = MaterialTheme.typography.bodyLarge) }
            } else {
                val totals = Ledger.totals(ledger.map { Ledger.Line(it.expectedPaise, it.receivedPaise, it.status) })
                item {
                    Text(
                        stringResource(R.string.customer_ledger_totals, rupees(totals.expectedPaise), rupees(totals.receivedPaise)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            items(ledger, key = { "ledger-" + it.id }) { row -> LedgerLine(row) }
        }
    }

    if (addingFollowUp) {
        FollowUpDialog(
            onDismiss = { addingFollowUp = false },
            onSave = { date, note -> if (viewModel.addFollowUp(date, note)) addingFollowUp = false },
        )
    }
}

@Composable
private fun FollowUpDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var date by remember { mutableStateOf(IndianFormat.date(java.time.LocalDate.now().plusDays(1))) }
    var note by remember { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.followup_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField(
                    date, { date = it }, stringResource(R.string.followup_date),
                    error = if (tried && IndianFormat.parseDate(date) == null) stringResource(R.string.error_date) else null,
                )
                FormField(note, { note = it }, stringResource(R.string.followup_note))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                tried = true
                onSave(date, note)
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun HoldingCard(view: HoldingView, onClick: () -> Unit) {
    val h = view.holding
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val name = view.product?.let { localized(it.nameEn, it.nameHi) } ?: "-"
            Text(name, style = MaterialTheme.typography.titleMedium)
            h.policyCategory.labelRes()?.let { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium) }
            val money = h.instalmentPaise?.let { "${rupees(it)} · ${stringResource(h.frequency.labelRes())}" }
                ?: rupees(h.amountPaise)
            Text(money, style = MaterialTheme.typography.bodyLarge)
            h.refLast4?.let { Text(stringResource(R.string.holding_ref, it), style = MaterialTheme.typography.bodyMedium) }
            h.maturityDate?.let {
                Text(stringResource(R.string.holding_matures, it.display()), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun LedgerLine(row: LedgerRow) {
    val month = YearMonth.parse(row.period).format(DateTimeFormatter.ofPattern("MMM yyyy", currentLocale()))
    val product = localized(row.productNameEn, row.productNameHi)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(month, style = MaterialTheme.typography.bodyLarge)
            Text(
                row.policyYear?.let { stringResource(R.string.commission_line_detail, product, it) } ?: product,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
            Text(row.expectedPaise?.let(::rupees) ?: "-", style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(row.status.labelRes()),
                style = MaterialTheme.typography.bodySmall,
                color = when (row.status) {
                    CommissionStatus.RECEIVED -> MaterialTheme.colorScheme.primary
                    CommissionStatus.NO_RULE -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.secondary
                },
            )
        }
    }
}
