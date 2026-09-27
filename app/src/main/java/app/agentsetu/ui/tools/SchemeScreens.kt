package app.agentsetu.ui.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.agentsetu.R
import app.agentsetu.core.catalogue.DefaultProducts
import app.agentsetu.data.db.InterestRateEntity
import app.agentsetu.data.repo.InterestRateRepository
import app.agentsetu.share.ShareCard
import app.agentsetu.ui.common.AppScaffold
import app.agentsetu.ui.common.display
import app.agentsetu.ui.common.localized
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.launch

/** The schemes with a card, and which interest-rate rows (product codes) each one shows. */
private val SCHEMES = listOf(
    "PLI" to emptyList(),
    "RPLI" to emptyList(),
    "TD" to listOf("TD_1Y", "TD_2Y", "TD_3Y", "TD_5Y"),
    "RD" to listOf("RD_5Y"),
    "MIS" to listOf("MIS"),
    "NSC" to listOf("NSC"),
    "KVP" to listOf("KVP"),
    "SCSS" to listOf("SCSS"),
    "PPF" to listOf("PPF"),
    "SSA" to listOf("SSA"),
)

private fun pointsRes(code: String): Int = when (code) {
    "PLI" -> R.string.scheme_PLI_points
    "RPLI" -> R.string.scheme_RPLI_points
    "TD" -> R.string.scheme_TD_points
    "RD" -> R.string.scheme_RD_points
    "MIS" -> R.string.scheme_MIS_points
    "NSC" -> R.string.scheme_NSC_points
    "KVP" -> R.string.scheme_KVP_points
    "SCSS" -> R.string.scheme_SCSS_points
    "PPF" -> R.string.scheme_PPF_points
    else -> R.string.scheme_SSA_points
}

@Composable
private fun schemeTitle(code: String): String = when (code) {
    "TD" -> stringResource(R.string.scheme_title_TD)
    "RD" -> stringResource(R.string.scheme_title_RD)
    else -> DefaultProducts.byCode(code)?.let { localized(it.nameEn, it.nameHi) } ?: code
}

@Composable
fun SchemeListScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    AppScaffold(title = stringResource(R.string.schemes_title), onBack = onBack) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(SCHEMES, key = { it.first }) { (code, _) ->
                ListItem(
                    headlineContent = { Text(schemeTitle(code)) },
                    modifier = Modifier.clickable { onOpen(code) },
                )
                HorizontalDivider()
            }
        }
    }
}

@HiltViewModel
class SchemeViewModel @Inject constructor(
    rates: InterestRateRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val code: String = checkNotNull(savedStateHandle["code"])
    var rateRows by mutableStateOf<List<InterestRateEntity>>(emptyList())
        private set

    init {
        val schemes = SCHEMES.firstOrNull { it.first == code }?.second.orEmpty()
        viewModelScope.launch { rateRows = schemes.mapNotNull { rates.inForce(it) } }
    }
}

@Composable
fun SchemeDetailScreen(onBack: () -> Unit, viewModel: SchemeViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val title = schemeTitle(viewModel.code)
    val points = stringResource(pointsRes(viewModel.code)).split('\n').filter { it.isNotBlank() }
    val rateLines = viewModel.rateRows.map { row ->
        val rate = BigDecimal(row.rate).stripTrailingZeros().toPlainString()
        val name = DefaultProducts.byCode(row.scheme)?.let { localized(it.nameEn, it.nameHi) } ?: row.scheme
        val text = stringResource(R.string.scheme_current_rate, rate, row.effectiveFrom.display())
        if (viewModel.rateRows.size > 1) "$name: $text" else text
    }
    val footer = listOf(
        stringResource(R.string.share_footer_verify),
        stringResource(R.string.share_footer_unofficial),
    )
    val checked = stringResource(R.string.scheme_checked)

    AppScaffold(title = title, onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    points.forEach { Text("•  $it", style = MaterialTheme.typography.bodyLarge) }
                    rateLines.forEach { Text("•  $it", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary) }
                }
            }
            footer.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Text(checked, style = MaterialTheme.typography.bodySmall)
            Button(
                onClick = { ShareCard.share(context, title, points + rateLines, footer) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) { Text(stringResource(R.string.action_share_image)) }
        }
    }
}
