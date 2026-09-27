package app.agentsetu.ui.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.agentsetu.R
import app.agentsetu.ui.common.AppScaffold

@Composable
fun ToolsScreen(onCalculator: (CalcKind) -> Unit, onSchemes: () -> Unit, onRates: () -> Unit) {
    AppScaffold(title = stringResource(R.string.tools_title), aboveBottomBar = true) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            ToolRow(R.string.tools_calc_td, R.string.tools_calc_summary) { onCalculator(CalcKind.TD) }
            ToolRow(R.string.tools_calc_rd, R.string.tools_calc_summary) { onCalculator(CalcKind.RD) }
            ToolRow(R.string.tools_calc_mis, R.string.tools_calc_summary) { onCalculator(CalcKind.MIS) }
            ToolRow(R.string.tools_schemes, R.string.tools_schemes_summary, onSchemes)
            ToolRow(R.string.tools_rates, R.string.tools_rates_summary, onRates)
        }
    }
}

@Composable
private fun ToolRow(title: Int, summary: Int, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        supportingContent = { Text(stringResource(summary)) },
        modifier = Modifier.clickable(onClick = onClick),
    )
    HorizontalDivider()
}
