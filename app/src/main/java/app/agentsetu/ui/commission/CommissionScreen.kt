package app.agentsetu.ui.commission

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import app.agentsetu.core.model.ReceiptMode
import app.agentsetu.data.db.AgentSetuDatabase
import app.agentsetu.data.db.CommissionReceiptEntity
import app.agentsetu.data.db.IncentiveStatementEntity
import app.agentsetu.data.db.LedgerRow
import app.agentsetu.data.repo.CommissionRepository
import app.agentsetu.ui.common.AppScaffold
import app.agentsetu.ui.common.DateField
import app.agentsetu.ui.common.FormField
import app.agentsetu.ui.common.RadioGroup
import app.agentsetu.ui.common.currentLocale
import app.agentsetu.ui.common.display
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the user typed in a receipt or statement form; parsed by the view model. */
data class PaymentInput(
    val amount: String,
    val date: String,
    val mode: ReceiptMode,
    val reference: String,
    val note: String,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CommissionViewModel @Inject constructor(
    private val db: AgentSetuDatabase,
    private val commission: CommissionRepository,
) : ViewModel() {
    val month = MutableStateFlow(YearMonth.now())

    val rows = month
        .flatMapLatest { db.commissionEntryDao().observeLedger(it.toString()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val statements = month
        .flatMapLatest { db.incentiveStatementDao().observeForMonth(it.toString()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The entry whose receipts dialog is open. */
    val selectedEntryId = MutableStateFlow<String?>(null)

    val receipts = selectedEntryId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else db.commissionReceiptDao().observeForEntry(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Number of entries settled from a statement, for the snackbar. */
    private val settledEvents = Channel<Int>(Channel.BUFFERED)
    val settled = settledEvents.receiveAsFlow()

    fun shiftMonth(by: Long) {
        month.value = month.value.plusMonths(by)
    }

    /** Returns false if the input is invalid; the form then stays open. */
    fun addReceipt(entryId: String, input: PaymentInput): Boolean {
        val amount = AmountInput.parse(input.amount)?.takeIf { it.signum() > 0 } ?: return false
        val date = IndianFormat.parseDate(input.date) ?: return false
        viewModelScope.launch { commission.addReceipt(entryId, amount, date, input.mode, input.reference, input.note) }
        return true
    }

    fun deleteReceipt(receiptId: String) {
        viewModelScope.launch { commission.deleteReceipt(receiptId) }
    }

    fun saveStatement(id: String?, input: PaymentInput): Boolean {
        val amount = AmountInput.parse(input.amount)?.takeIf { it.signum() > 0 } ?: return false
        val date = IndianFormat.parseDate(input.date) ?: return false
        val forMonth = month.value
        viewModelScope.launch { commission.saveStatement(id, forMonth, amount, date, input.mode, input.reference, input.note) }
        return true
    }

    fun deleteStatement(id: String) {
        viewModelScope.launch { commission.deleteStatement(id) }
    }

    fun settleFromStatement(id: String) {
        viewModelScope.launch { settledEvents.send(commission.settleMonthFromStatement(id)) }
    }
}

@Composable
fun CommissionScreen(viewModel: CommissionViewModel = hiltViewModel()) {
    val month by viewModel.month.collectAsStateWithLifecycle()
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val statements by viewModel.statements.collectAsStateWithLifecycle()
    val receipts by viewModel.receipts.collectAsStateWithLifecycle()
    val selectedEntryId by viewModel.selectedEntryId.collectAsStateWithLifecycle()
    var statementDialog by remember { mutableStateOf<StatementDialogState?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val totals = Ledger.totals(rows.map { Ledger.Line(it.expectedPaise, it.receivedPaise, it.status) })
    val monthLabel = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", currentLocale()))

    LaunchedEffect(Unit) {
        viewModel.settled.collect { count ->
            snackbar.showSnackbar(context.getString(R.string.statement_settled, count))
        }
    }

    AppScaffold(title = stringResource(R.string.commission_title), aboveBottomBar = true, snackbarHostState = snackbar) { padding ->
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
            item {
                StatementCard(
                    statements = statements,
                    totals = totals,
                    onAdd = { statementDialog = StatementDialogState(null) },
                    onOpen = { statementDialog = StatementDialogState(it) },
                )
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
                LedgerItem(row, onClick = { viewModel.selectedEntryId.value = row.id })
                HorizontalDivider()
            }
        }
    }

    // The row is looked up again on every recomposition so the dialog shows the latest totals.
    val selectedRow = rows.firstOrNull { it.id == selectedEntryId }
    LaunchedEffect(selectedEntryId, selectedRow == null) {
        // The month changed or the entry vanished: close the dialog.
        if (selectedEntryId != null && selectedRow == null) viewModel.selectedEntryId.value = null
    }
    if (selectedRow != null) {
        ReceiptsDialog(
            row = selectedRow,
            receipts = receipts,
            onDismiss = { viewModel.selectedEntryId.value = null },
            onAdd = { viewModel.addReceipt(selectedRow.id, it) },
            onDelete = viewModel::deleteReceipt,
        )
    }

    statementDialog?.let { state ->
        StatementDialog(
            existing = state.statement,
            pendingCount = totals.pendingCount,
            onDismiss = { statementDialog = null },
            onSave = { input -> if (viewModel.saveStatement(state.statement?.id, input)) statementDialog = null },
            onDelete = { id ->
                viewModel.deleteStatement(id)
                statementDialog = null
            },
            onSettle = { id ->
                viewModel.settleFromStatement(id)
                statementDialog = null
            },
        )
    }
}

private data class StatementDialogState(val statement: IncentiveStatementEntity?)

@Composable
private fun TotalLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun StatementCard(
    statements: List<IncentiveStatementEntity>,
    totals: Ledger.Totals,
    onAdd: () -> Unit,
    onOpen: (IncentiveStatementEntity) -> Unit,
) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.statement_title), style = MaterialTheme.typography.titleMedium)
            if (statements.isEmpty()) {
                Text(stringResource(R.string.statement_none), style = MaterialTheme.typography.bodyMedium)
            } else {
                statements.forEach { s ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(s) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                listOf(s.date.display(), stringResource(s.mode.labelRes())).joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (s.reference.isNotBlank()) Text(s.reference, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(rupees(s.amountPaise), style = MaterialTheme.typography.titleMedium)
                    }
                }
                val match = Ledger.matchStatement(statements.sumOf { it.amountPaise }, totals)
                HorizontalDivider()
                DifferenceLine(stringResource(R.string.statement_vs_expected), match.vsExpected)
                DifferenceLine(stringResource(R.string.statement_vs_received), match.vsReceived)
            }
            TextButton(onClick = onAdd) { Text(stringResource(R.string.statement_add)) }
        }
    }
}

@Composable
private fun DifferenceLine(label: String, paise: Long) {
    val text = when {
        paise == 0L -> stringResource(R.string.statement_matches)
        paise > 0 -> stringResource(R.string.statement_more, rupees(paise))
        else -> stringResource(R.string.statement_less, rupees(-paise))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (paise == 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
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
            Column {
                Text(row.policyYear?.let { stringResource(R.string.commission_line_detail, product, it) } ?: product)
                if (row.status == CommissionStatus.PARTLY_RECEIVED) {
                    Text(
                        stringResource(R.string.commission_received_so_far, rupees(row.receivedPaise ?: 0)),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
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

/** Amount, date, mode, reference and note fields shared by receipts and statements. */
@Composable
private fun PaymentFields(input: PaymentInput, onChange: (PaymentInput) -> Unit, tried: Boolean, amountLabel: String) {
    FormField(
        input.amount, { onChange(input.copy(amount = it)) }, amountLabel,
        error = if (tried && AmountInput.parse(input.amount)?.signum() != 1) stringResource(R.string.error_amount) else null,
        keyboardType = KeyboardType.Decimal,
    )
    DateField(
        input.date, { onChange(input.copy(date = it)) }, stringResource(R.string.commission_received_date),
        error = if (tried && IndianFormat.parseDate(input.date) == null) stringResource(R.string.error_date) else null,
    )
    Text(stringResource(R.string.receipt_mode), style = MaterialTheme.typography.bodyMedium)
    RadioGroup(ReceiptMode.entries, input.mode, { stringResource(it.labelRes()) }) { onChange(input.copy(mode = it)) }
    FormField(input.reference, { onChange(input.copy(reference = it)) }, stringResource(R.string.receipt_reference))
    FormField(input.note, { onChange(input.copy(note = it)) }, stringResource(R.string.receipt_note))
}

@Composable
private fun ReceiptsDialog(
    row: LedgerRow,
    receipts: List<CommissionReceiptEntity>,
    onDismiss: () -> Unit,
    onAdd: (PaymentInput) -> Boolean,
    onDelete: (String) -> Unit,
) {
    val outstanding = Ledger.outstanding(row.expectedPaise, row.receivedPaise)
    // Nothing received yet: open straight on the form. Otherwise show the receipts first.
    var adding by remember(row.id) { mutableStateOf(row.receivedPaise == null) }
    var input by remember(row.id) {
        mutableStateOf(
            PaymentInput(
                amount = if (outstanding > 0) editableAmount(outstanding) else "",
                date = IndianFormat.date(LocalDate.now()),
                mode = ReceiptMode.POSB,
                reference = "",
                note = "",
            ),
        )
    }
    var tried by remember(row.id) { mutableStateOf(false) }
    val product = localized(row.productNameEn, row.productNameHi)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(row.customerName.ifBlank { stringResource(R.string.customer_deleted) }) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(row.policyYear?.let { stringResource(R.string.commission_line_detail, product, it) } ?: product)
                TotalLine(stringResource(R.string.commission_expected), row.expectedPaise?.let(::rupees) ?: "-")
                TotalLine(stringResource(R.string.commission_received), rupees(row.receivedPaise ?: 0))
                if (outstanding > 0) TotalLine(stringResource(R.string.commission_shortfall), rupees(outstanding))
                HorizontalDivider()
                Text(stringResource(R.string.receipts_title), style = MaterialTheme.typography.titleSmall)
                if (receipts.isEmpty()) {
                    Text(stringResource(R.string.receipts_none), style = MaterialTheme.typography.bodyMedium)
                }
                receipts.forEach { r ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${rupees(r.amountPaise)} · ${r.date.display()} · ${stringResource(r.mode.labelRes())}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            listOf(r.reference, r.note).filter { it.isNotBlank() }.forEach {
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        IconButton(onClick = { onDelete(r.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.receipt_delete))
                        }
                    }
                }
                if (adding) {
                    HorizontalDivider()
                    Text(stringResource(R.string.receipt_add), style = MaterialTheme.typography.titleSmall)
                    PaymentFields(input, { input = it }, tried, stringResource(R.string.commission_received_amount))
                } else {
                    OutlinedButton(
                        onClick = { adding = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) { Text(stringResource(R.string.receipt_add)) }
                }
            }
        },
        confirmButton = {
            if (adding) {
                TextButton(onClick = {
                    tried = true
                    if (onAdd(input)) {
                        adding = false
                        tried = false
                        input = input.copy(amount = "", reference = "", note = "")
                    }
                }) { Text(stringResource(R.string.action_save)) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) }
            }
        },
        dismissButton = {
            if (adding) TextButton(onClick = { adding = false }) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun StatementDialog(
    existing: IncentiveStatementEntity?,
    pendingCount: Int,
    onDismiss: () -> Unit,
    onSave: (PaymentInput) -> Unit,
    onDelete: (String) -> Unit,
    onSettle: (String) -> Unit,
) {
    var input by remember {
        mutableStateOf(
            PaymentInput(
                amount = existing?.let { editableAmount(it.amountPaise) } ?: "",
                date = IndianFormat.date(existing?.date ?: LocalDate.now()),
                mode = existing?.mode ?: ReceiptMode.POSB,
                reference = existing?.reference.orEmpty(),
                note = existing?.note.orEmpty(),
            ),
        )
    }
    var tried by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing == null) R.string.statement_add else R.string.statement_edit)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.statement_help), style = MaterialTheme.typography.bodyMedium)
                PaymentFields(input, { input = it }, tried, stringResource(R.string.statement_amount))
                if (existing != null) {
                    HorizontalDivider()
                    if (pendingCount > 0) {
                        OutlinedButton(
                            onClick = { onSettle(existing.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                        ) { Text(stringResource(R.string.statement_settle, pendingCount)) }
                        Text(stringResource(R.string.statement_settle_help), style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { onDelete(existing.id) }) {
                        Text(stringResource(R.string.statement_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                tried = true
                onSave(input)
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
