package app.agentsetu.ui.customers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import app.agentsetu.data.db.AgentSetuDatabase
import app.agentsetu.ui.common.AppScaffold
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class CustomersViewModel @Inject constructor(db: AgentSetuDatabase) : ViewModel() {
    val query = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val customers = query
        .flatMapLatest { q -> if (q.isBlank()) db.customerDao().observeAll() else db.customerDao().search(q.trim()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun CustomersScreen(onAdd: () -> Unit, onOpen: (String) -> Unit, viewModel: CustomersViewModel = hiltViewModel()) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()

    AppScaffold(
        title = stringResource(R.string.customers_title),
        aboveBottomBar = true,
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.customer_add))
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.query.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.customers_search), maxLines = 1) },
                textStyle = MaterialTheme.typography.bodyMedium,
                singleLine = true,
            )
            if (customers.isEmpty() && query.isBlank()) {
                Text(
                    stringResource(R.string.customers_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(20.dp),
                )
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(customers, key = { it.id }) { customer ->
                    ListItem(
                        headlineContent = { Text(customer.name, style = MaterialTheme.typography.titleMedium) },
                        supportingContent = {
                            val details = listOfNotNull(customer.village, customer.mobile).filter { it.isNotBlank() }
                            if (details.isNotEmpty()) Text(details.joinToString(" · "))
                        },
                        modifier = Modifier.clickable { onOpen(customer.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
