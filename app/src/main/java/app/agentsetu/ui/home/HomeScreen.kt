package app.agentsetu.ui.home

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import app.agentsetu.core.input.MobileNumber
import app.agentsetu.core.ledger.Ledger
import app.agentsetu.core.model.ReminderType
import app.agentsetu.data.db.AgentSetuDatabase
import app.agentsetu.data.db.ReminderRow
import app.agentsetu.data.repo.MaturityOutcome
import app.agentsetu.data.repo.ReminderRepository
import app.agentsetu.reminders.ReminderNotifier
import app.agentsetu.ui.common.AppScaffold
import app.agentsetu.ui.common.FormField
import app.agentsetu.ui.common.display
import app.agentsetu.ui.common.localized
import app.agentsetu.ui.common.openAppSettings
import app.agentsetu.ui.common.openUri
import app.agentsetu.ui.common.rupees
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodaySections(
    val overdue: List<ReminderRow> = emptyList(),
    val today: List<ReminderRow> = emptyList(),
    val week: List<ReminderRow> = emptyList(),
    val maturities: List<ReminderRow> = emptyList(),
) {
    val isEmpty get() = overdue.isEmpty() && today.isEmpty() && week.isEmpty() && maturities.isEmpty()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    db: AgentSetuDatabase,
    private val reminders: ReminderRepository,
) : ViewModel() {
    val today: LocalDate = LocalDate.now()

    val sections = db.reminderDao().observeOpen(reminders.horizon(today))
        .map { rows ->
            TodaySections(
                overdue = rows.filter { it.dueDate.isBefore(today) },
                today = rows.filter { it.dueDate == today },
                week = rows.filter { it.dueDate.isAfter(today) && it.type != ReminderType.MATURITY },
                maturities = rows.filter { it.dueDate.isAfter(today) && it.type == ReminderType.MATURITY },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodaySections())

    val monthTotals = db.commissionEntryDao().observeLedger(YearMonth.now().toString())
        .map { rows -> Ledger.totals(rows.map { Ledger.Line(it.expectedPaise, it.receivedPaise, it.status) }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Ledger.totals(emptyList()))

    init {
        // Opening the app also brings reminders up to date, in case the daily job was held back.
        viewModelScope.launch { reminders.regenerate(today) }
    }

    fun collected(id: String) = viewModelScope.launch { reminders.markCollected(id) }
    fun dismiss(id: String) = viewModelScope.launch { reminders.markDone(id) }
    fun maturity(id: String, outcome: MaturityOutcome) = viewModelScope.launch { reminders.maturityHandled(id, outcome) }

    /** Returns false for an unreadable date; blank means no next follow-up. */
    fun followUpDone(id: String, nextText: String): Boolean {
        val next = if (nextText.isBlank()) null else IndianFormat.parseDate(nextText) ?: return false
        viewModelScope.launch { reminders.followUpDone(id, next) }
        return true
    }
}

@Composable
fun HomeScreen(
    onOpenCustomer: (String) -> Unit,
    onAddBusiness: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val totals by viewModel.monthTotals.collectAsStateWithLifecycle()
    var acting by remember { mutableStateOf<ReminderRow?>(null) }
    val today = viewModel.today

    AppScaffold(title = stringResource(R.string.home_title), aboveBottomBar = true) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { NotificationPermissionCard() }
            item { MonthCard(totals.expectedPaise, totals.receivedPaise) }
            if (sections.isEmpty) {
                item { Text(stringResource(R.string.reminders_empty), style = MaterialTheme.typography.bodyLarge) }
            }
            section(R.string.reminders_overdue, sections.overdue, today, onOpenCustomer) { acting = it }
            section(R.string.reminders_today, sections.today, today, onOpenCustomer) { acting = it }
            section(R.string.reminders_coming, sections.week, today, onOpenCustomer) { acting = it }
            section(R.string.reminders_maturities, sections.maturities, today, onOpenCustomer) { acting = it }
            item { BatteryHelp() }
            item {
                Text(
                    stringResource(R.string.home_data_private),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    acting?.let { row ->
        ActionDialog(
            row = row,
            onDismiss = { acting = null },
            onCollected = { viewModel.collected(row.id); acting = null },
            onSkip = { viewModel.dismiss(row.id); acting = null },
            onFollowUpDone = { next -> if (viewModel.followUpDone(row.id, next)) acting = null },
            onMaturity = { outcome ->
                viewModel.maturity(row.id, outcome)
                acting = null
                if (outcome == MaturityOutcome.REINVESTED) onAddBusiness(row.customerId)
            },
        )
    }
}

private fun LazyListScope.section(
    title: Int,
    rows: List<ReminderRow>,
    today: LocalDate,
    onOpenCustomer: (String) -> Unit,
    onDone: (ReminderRow) -> Unit,
) {
    if (rows.isEmpty()) return
    item(key = "header-$title") {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
    }
    items(rows, key = { it.id }) { row -> ReminderCard(row, today, onOpenCustomer, onDone) }
}

@Composable
private fun ReminderCard(row: ReminderRow, today: LocalDate, onOpenCustomer: (String) -> Unit, onDone: (ReminderRow) -> Unit) {
    val context = LocalContext.current
    val overdue = row.dueDate.isBefore(today)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(ReminderNotifier.typeLabel(row.type)),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
                val days = ChronoUnit.DAYS.between(today, row.dueDate)
                Text(
                    if (days > 0) "${row.dueDate.display()} · ${stringResource(R.string.reminder_in_days, days.toInt())}" else row.dueDate.display(),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Text(
                row.customerName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.clickable { onOpenCustomer(row.customerId) },
            )
            val product = row.productNameEn?.let { localized(it, row.productNameHi ?: it) }
            val amount = when (row.type) {
                ReminderType.MATURITY -> row.amountPaise
                ReminderType.FOLLOW_UP -> null
                else -> row.instalmentPaise
            }
            val detail = listOfNotNull(product, amount?.let(::rupees), row.note?.takeIf { it.isNotBlank() })
            if (detail.isNotEmpty()) Text(detail.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                row.mobile?.let { mobile ->
                    IconButton(onClick = { context.openUri(MobileNumber.dialUri(mobile), Intent.ACTION_DIAL) }) {
                        Icon(Icons.Filled.Call, contentDescription = stringResource(R.string.customer_call))
                    }
                    TextButton(onClick = { context.openUri(MobileNumber.whatsAppUri(mobile)) }) {
                        Text(stringResource(R.string.customer_whatsapp))
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = { onDone(row) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.action_done))
                }
            }
        }
    }
}

@Composable
private fun MonthCard(expectedPaise: Long, receivedPaise: Long) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.home_month_card), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.commission_expected), style = MaterialTheme.typography.bodyLarge)
                Text(rupees(expectedPaise), style = MaterialTheme.typography.titleMedium)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.commission_received), style = MaterialTheme.typography.bodyLarge)
                Text(rupees(receivedPaise), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** Android 13+ asks for notification permission at runtime; older versions allow it by default. */
@Composable
private fun NotificationPermissionCard() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    var granted by remember { mutableStateOf(ReminderNotifier.canNotify(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    if (granted) return
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.notif_permission_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.notif_permission_body), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                Text(stringResource(R.string.notif_permission_allow))
            }
        }
    }
}

@Composable
private fun BatteryHelp() {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.battery_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { open = !open },
            )
            if (open) {
                Text(stringResource(R.string.battery_body), style = MaterialTheme.typography.bodyMedium)
                brandTip()?.let { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium) }
                OutlinedButton(onClick = { context.openAppSettings() }) {
                    Text(stringResource(R.string.battery_open_settings))
                }
            }
        }
    }
}

private fun brandTip(): Int? = when (Build.MANUFACTURER.lowercase()) {
    "xiaomi", "redmi", "poco" -> R.string.battery_xiaomi
    "vivo", "oppo", "realme", "oneplus", "iqoo" -> R.string.battery_vivo_oppo
    "samsung" -> R.string.battery_samsung
    else -> null
}

@Composable
private fun ActionDialog(
    row: ReminderRow,
    onDismiss: () -> Unit,
    onCollected: () -> Unit,
    onSkip: () -> Unit,
    onFollowUpDone: (String) -> Unit,
    onMaturity: (MaturityOutcome) -> Unit,
) {
    when (row.type) {
        ReminderType.PREMIUM_DUE, ReminderType.INSTALMENT_DUE -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.collect_title)) },
            text = { Text(stringResource(R.string.collect_body)) },
            confirmButton = { TextButton(onClick = onCollected) { Text(stringResource(R.string.collect_yes)) } },
            dismissButton = { TextButton(onClick = onSkip) { Text(stringResource(R.string.collect_skip)) } },
        )

        ReminderType.FOLLOW_UP -> {
            var next by remember { mutableStateOf("") }
            var tried by remember { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.followup_done_title)) },
                text = {
                    FormField(
                        next, { next = it }, stringResource(R.string.followup_next),
                        error = if (tried && next.isNotBlank() && IndianFormat.parseDate(next) == null) stringResource(R.string.error_date) else null,
                        supporting = stringResource(R.string.date_hint),
                        keyboardType = KeyboardType.Number,
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        tried = true
                        onFollowUpDone(next)
                    }) { Text(stringResource(R.string.action_save)) }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
            )
        }

        ReminderType.MATURITY -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.maturity_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { onMaturity(MaturityOutcome.REINVESTED) }) { Text(stringResource(R.string.maturity_reinvested)) }
                    TextButton(onClick = { onMaturity(MaturityOutcome.WITHDRAWN) }) { Text(stringResource(R.string.maturity_withdrawn)) }
                    TextButton(onClick = { onMaturity(MaturityOutcome.UNDECIDED) }) { Text(stringResource(R.string.maturity_later)) }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
