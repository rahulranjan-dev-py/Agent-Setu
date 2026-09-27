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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.font.FontFamily
import app.agentsetu.security.DataEraser
import app.agentsetu.security.SigningInfo
import app.agentsetu.ui.common.AppScaffold
import app.agentsetu.ui.common.LanguageChips
import app.agentsetu.ui.common.SectionTitle
import app.agentsetu.ui.lock.EraseDialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.agentsetu.core.update.UpdateStatus
import app.agentsetu.update.UpdateChecker
import app.agentsetu.ui.common.UpdateBanner
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(val updates: UpdateChecker) : ViewModel()

@Composable
fun SettingsScreen(
    onProfile: () -> Unit,
    onRates: () -> Unit,
    onAppLock: () -> Unit,
    onBackup: () -> Unit,
    onErrorReport: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var erasing by remember { mutableStateOf(false) }
    val update by viewModel.updates.status.collectAsStateWithLifecycle()
    // null = not checked in this visit; otherwise the message to show.
    var checkMessage by remember { mutableStateOf<Int?>(null) }

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
                headlineContent = { Text(stringResource(R.string.settings_updates)) },
                supportingContent = checkMessage?.let { { Text(stringResource(it)) } },
                modifier = Modifier.clickable {
                    checkMessage = R.string.update_checking
                    scope.launch {
                        val ok = viewModel.updates.check(force = true)
                        checkMessage = when {
                            !ok -> R.string.update_failed
                            viewModel.updates.status.value is UpdateStatus.UpToDate -> R.string.update_up_to_date
                            else -> null
                        }
                    }
                },
            )
            if (checkMessage == null) Column(Modifier.padding(horizontal = 16.dp)) { UpdateBanner(update) }
            HorizontalDivider()
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
                val fingerprint = remember { SigningInfo.sha256(context) }
                if (fingerprint != null) {
                    Text(stringResource(R.string.about_signing), style = MaterialTheme.typography.titleSmall)
                    Text(fingerprint, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.about_signing_help), style = MaterialTheme.typography.bodySmall)
                }
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
