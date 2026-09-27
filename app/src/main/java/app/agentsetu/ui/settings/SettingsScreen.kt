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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.agentsetu.BuildConfig
import app.agentsetu.R
import app.agentsetu.security.DataEraser
import app.agentsetu.ui.common.AppScaffold
import app.agentsetu.ui.common.LanguageChips
import app.agentsetu.ui.common.SectionTitle
import app.agentsetu.ui.lock.EraseDialog

@Composable
fun SettingsScreen(
    onProfile: () -> Unit,
    onRates: () -> Unit,
    onAppLock: () -> Unit,
    onBackup: () -> Unit,
    onErrorReport: () -> Unit,
) {
    val context = LocalContext.current
    var erasing by remember { mutableStateOf(false) }

    AppScaffold(title = stringResource(R.string.settings_title), aboveBottomBar = true) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Item(R.string.settings_profile, null, onProfile)
            Item(R.string.settings_rates, R.string.settings_rates_summary, onRates)
            Item(R.string.settings_app_lock, R.string.settings_app_lock_summary, onAppLock)
            Item(R.string.settings_backup, R.string.settings_backup_summary, onBackup)
            Item(R.string.settings_error_report, R.string.settings_error_report_summary, onErrorReport)
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_delete_all), color = MaterialTheme.colorScheme.error) },
                supportingContent = { Text(stringResource(R.string.settings_delete_all_summary)) },
                modifier = Modifier.clickable { erasing = true },
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

    if (erasing) {
        EraseDialog(
            title = R.string.settings_delete_all,
            body = R.string.delete_all_body,
            onDismiss = { erasing = false },
        ) { DataEraser.eraseEverything(context) }
    }
}

@Composable
private fun Item(title: Int, summary: Int?, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        supportingContent = summary?.let { { Text(stringResource(it)) } },
        modifier = Modifier.clickable(onClick = onClick),
    )
    HorizontalDivider()
}
