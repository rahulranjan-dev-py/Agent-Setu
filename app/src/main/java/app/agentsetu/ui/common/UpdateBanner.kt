package app.agentsetu.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.agentsetu.BuildConfig
import app.agentsetu.R
import app.agentsetu.core.update.DownloadPolicy
import app.agentsetu.core.update.UpdateCheck
import app.agentsetu.core.update.UpdateStatus
import app.agentsetu.core.update.VersionInfo
import app.agentsetu.update.DownloadState
import app.agentsetu.update.UpdateInstaller
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UpdateInstallViewModel @Inject constructor(val installer: UpdateInstaller) : ViewModel()

/**
 * "Download" either fetches and verifies the APK inside the app (when version.json gives an apkUrl
 * and checksum) or opens the release page in the browser.
 */
@Composable
private fun DownloadButton(info: VersionInfo, afterClick: () -> Unit = {}) {
    val context = LocalContext.current
    val vm: UpdateInstallViewModel = hiltViewModel()
    Button(onClick = {
        if (UpdateCheck.canInstallInApp(info, BuildConfig.APK_URL_PREFIX)) vm.installer.start(info) else context.openUri(info.downloadUrl)
        afterClick()
    }) { Text(stringResource(R.string.update_download)) }
}

/** "New version available" (or "please update"). */
@Composable
fun UpdateBanner(status: UpdateStatus?) {
    val (info, required) = when (status) {
        is UpdateStatus.Available -> status.info to false
        is UpdateStatus.Required -> status.info to true
        else -> return
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (required) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (required) stringResource(R.string.update_required_title) else stringResource(R.string.update_available_title, info.latestVersionName),
                style = MaterialTheme.typography.titleMedium,
            )
            val changelog = localized(info.changelogEn, info.changelogHi.ifBlank { info.changelogEn })
            if (changelog.isNotBlank()) Text(changelog, style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.update_only_official), style = MaterialTheme.typography.bodySmall)
            DownloadButton(info)
        }
    }
}

/** The version an update status points at, or null when up to date / unknown. */
fun UpdateStatus?.newVersion(): VersionInfo? = when (this) {
    is UpdateStatus.Available -> info
    is UpdateStatus.Required -> info
    else -> null
}

/** Pop-up version of the banner, shown once per launch on Today when an update is found. */
@Composable
fun UpdateDialog(status: UpdateStatus, onDismiss: () -> Unit) {
    val (info, required) = when (status) {
        is UpdateStatus.Available -> status.info to false
        is UpdateStatus.Required -> status.info to true
        UpdateStatus.UpToDate -> return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (required) stringResource(R.string.update_required_title) else stringResource(R.string.update_available_title, info.latestVersionName))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val changelog = localized(info.changelogEn, info.changelogHi.ifBlank { info.changelogEn })
                if (changelog.isNotBlank()) Text(changelog, style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.update_only_official), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { DownloadButton(info, afterClick = onDismiss) },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_later)) } },
    )
}

/** Progress of the in-app download, then Install; failures offer the download page instead. */
@Composable
fun UpdateDownloadDialog(info: VersionInfo) {
    val vm: UpdateInstallViewModel = hiltViewModel()
    val state by vm.installer.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    if (state is DownloadState.Idle) return
    val busy = state is DownloadState.Downloading || state is DownloadState.Verifying
    AlertDialog(
        onDismissRequest = { if (!busy) vm.installer.dismiss() },
        title = { Text(stringResource(R.string.update_download_title, info.latestVersionName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (val s = state) {
                    is DownloadState.Downloading -> {
                        val fraction = s.fraction
                        if (fraction == null) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        } else {
                            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                        }
                        Text(
                            when {
                                s.retry > 0 -> stringResource(R.string.update_reconnecting, s.retry, DownloadPolicy.MAX_RETRIES)
                                fraction == null -> stringResource(R.string.update_downloading)
                                else -> stringResource(R.string.update_downloading_percent, (fraction * 100).toInt())
                            },
                        )
                    }
                    DownloadState.Verifying -> {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(stringResource(R.string.update_verifying))
                    }
                    is DownloadState.Ready -> Text(stringResource(R.string.update_ready))
                    is DownloadState.Failed -> {
                        Text(
                            stringResource(
                                when (s.reason) {
                                    DownloadState.Reason.CHECKSUM -> R.string.update_failed_checksum
                                    DownloadState.Reason.NOT_GENUINE -> R.string.update_failed_not_genuine
                                    DownloadState.Reason.NETWORK -> R.string.update_failed_download
                                },
                            ),
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = {
                            vm.installer.dismiss()
                            context.openUri(info.downloadUrl)
                        }) { Text(stringResource(R.string.update_open_page)) }
                    }
                    DownloadState.Idle -> Unit
                }
            }
        },
        confirmButton = {
            when (val s = state) {
                is DownloadState.Ready -> Button(onClick = { vm.installer.install(s.file) }) { Text(stringResource(R.string.update_install)) }
                // After a checksum or genuineness failure the file is gone, so Retry starts afresh.
                is DownloadState.Failed -> Button(onClick = { vm.installer.start(info) }) { Text(stringResource(R.string.update_retry)) }
                else -> Unit
            }
        },
        dismissButton = {
            if (busy) {
                TextButton(onClick = { vm.installer.cancel() }) { Text(stringResource(R.string.action_cancel)) }
            } else {
                TextButton(onClick = { vm.installer.dismiss() }) { Text(stringResource(R.string.action_close)) }
            }
        },
    )
}
