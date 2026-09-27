package app.agentsetu.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.agentsetu.BuildConfig
import app.agentsetu.R
import app.agentsetu.ui.common.AppScaffold
import app.agentsetu.ui.common.LanguageChips
import app.agentsetu.ui.common.SectionTitle

@Composable
fun SettingsScreen(onProfile: () -> Unit, onRates: () -> Unit) {
    AppScaffold(title = stringResource(R.string.settings_title), aboveBottomBar = true) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_profile)) },
                modifier = Modifier.clickable(onClick = onProfile),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_rates)) },
                supportingContent = { Text(stringResource(R.string.settings_rates_summary)) },
                modifier = Modifier.clickable(onClick = onRates),
            )
            HorizontalDivider()
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(stringResource(R.string.settings_language))
                LanguageChips()
                SectionTitle(stringResource(R.string.settings_about))
                Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.disclaimer_title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.disclaimer_body), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
