package app.agentsetu.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.agentsetu.R
import app.agentsetu.report.CrashRecorder
import app.agentsetu.ui.common.AppScaffold

/** The user reads the report, then decides whether and where to send it. Nothing is sent automatically. */
@Composable
fun ErrorReportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var report by remember { mutableStateOf(CrashRecorder.read(context)) }

    AppScaffold(title = stringResource(R.string.report_title), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val text = report
            if (text == null) {
                Text(stringResource(R.string.report_none), style = MaterialTheme.typography.bodyLarge)
            } else {
                Text(stringResource(R.string.report_intro), style = MaterialTheme.typography.bodyMedium)
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        text,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }
                Button(onClick = {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                    context.startActivity(Intent.createChooser(send, null))
                }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.report_send)) }
                OutlinedButton(onClick = {
                    CrashRecorder.clear(context)
                    report = null
                }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.report_delete)) }
            }
        }
    }
}
