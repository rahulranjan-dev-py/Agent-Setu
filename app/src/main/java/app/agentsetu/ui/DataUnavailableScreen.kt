package app.agentsetu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import app.agentsetu.R
import app.agentsetu.security.DataEraser
import app.agentsetu.ui.lock.EraseDialog

/**
 * Shown instead of the app when the database key can no longer be opened on this phone (see
 * DatabaseKeyProvider.isUsable). The data is unreadable for good; the only way on is to erase and
 * restore from a backup file.
 */
@Composable
fun DataUnavailableScreen() {
    val context = LocalContext.current
    var confirm by remember { mutableStateOf(false) }
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.data_unavailable_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.data_unavailable_body), style = MaterialTheme.typography.bodyLarge)
            Button(
                onClick = { confirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) { Text(stringResource(R.string.data_unavailable_erase)) }
        }
    }
    if (confirm) {
        EraseDialog(title = R.string.data_unavailable_erase, body = R.string.lock_erase_body, onDismiss = { confirm = false }) {
            DataEraser.eraseEverything(context)
        }
    }
}
