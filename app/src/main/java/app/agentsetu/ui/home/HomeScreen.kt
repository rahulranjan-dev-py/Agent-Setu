package app.agentsetu.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.agentsetu.R
import app.agentsetu.core.ledger.Ledger
import app.agentsetu.data.db.AgentSetuDatabase
import app.agentsetu.ui.common.AppScaffold
import app.agentsetu.ui.common.rupees
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(db: AgentSetuDatabase) : ViewModel() {
    val customerCount = db.customerDao().observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val monthTotals = db.commissionEntryDao().observeLedger(YearMonth.now().toString())
        .map { rows -> Ledger.totals(rows.map { Ledger.Line(it.expectedPaise, it.receivedPaise, it.status) }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Ledger.totals(emptyList()))
}

/** "Today": this month's earnings for now; follow-ups, dues and maturities arrive with reminders. */
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val customers by viewModel.customerCount.collectAsStateWithLifecycle()
    val totals by viewModel.monthTotals.collectAsStateWithLifecycle()

    AppScaffold(title = stringResource(R.string.home_title), aboveBottomBar = true) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.home_month_card), style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.commission_expected), style = MaterialTheme.typography.bodyLarge)
                        Text(rupees(totals.expectedPaise), style = MaterialTheme.typography.titleMedium)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.commission_received), style = MaterialTheme.typography.bodyLarge)
                        Text(rupees(totals.receivedPaise), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            Text(stringResource(R.string.home_customers, customers), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.home_placeholder), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(R.string.home_data_private),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
