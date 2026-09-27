package app.agentsetu.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.agentsetu.R
import app.agentsetu.core.update.UpdateStatus

/** "New version available" (or "please update"). Download opens the release page in the browser. */
@Composable
fun UpdateBanner(status: UpdateStatus?) {
    val (info, required) = when (status) {
        is UpdateStatus.Available -> status.info to false
        is UpdateStatus.Required -> status.info to true
        else -> return
    }
    val context = LocalContext.current
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
            Button(onClick = { context.openUri(info.downloadUrl) }) { Text(stringResource(R.string.update_download)) }
        }
    }
}
