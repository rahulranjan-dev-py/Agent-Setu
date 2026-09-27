package app.agentsetu.ui.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.agentsetu.BuildConfig
import app.agentsetu.R
import app.agentsetu.backup.BackupManager
import app.agentsetu.backup.RestoreResult
import app.agentsetu.core.backup.BackupCrypto
import app.agentsetu.core.format.IndianFormat
import app.agentsetu.data.settings.AppSettings
import app.agentsetu.ui.common.AppScaffold
import app.agentsetu.ui.common.FormField
import app.agentsetu.ui.common.SectionTitle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface BackupStatus {
    data object Idle : BackupStatus
    data object Working : BackupStatus
    data class Done(val message: Int, val arg: Int? = null) : BackupStatus
    data class Failed(val message: Int) : BackupStatus
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backups: BackupManager,
    private val settings: AppSettings,
) : ViewModel() {
    var password by mutableStateOf("")
    var confirm by mutableStateOf("")
    var status by mutableStateOf<BackupStatus>(BackupStatus.Idle)
        private set

    val passwordTooShort get() = password.length < BackupCrypto.MIN_PASSWORD_LENGTH
    val mismatch get() = confirm != password
    val fileName: String get() = "AgentSetu-backup-${IndianFormat.date(LocalDate.now())}.asb"

    fun saveTo(uri: Uri) = launchTask {
        val bytes = backups.export(password)
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Cannot write")
        }
        settings.lastBackupAt = System.currentTimeMillis()
        BackupStatus.Done(R.string.backup_saved)
    }

    /** Writes the file to the app's private cache and opens the share sheet (WhatsApp, Drive, email...). */
    fun share(onReady: (Intent) -> Unit) = launchTask {
        val bytes = backups.export(password)
        val file = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "backups").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() } // keep only the newest copy
            File(dir, fileName).apply { writeBytes(bytes) }
        }
        val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("application/octet-stream")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        onReady(Intent.createChooser(send, null))
        settings.lastBackupAt = System.currentTimeMillis()
        BackupStatus.Done(R.string.backup_shared)
    }

    fun restore(uri: Uri, restorePassword: String) = launchTask {
        val bytes = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Cannot read")
        }
        when (val result = backups.restore(bytes, restorePassword)) {
            is RestoreResult.Restored -> BackupStatus.Done(R.string.restore_done, result.customers)
            RestoreResult.WrongPasswordOrDamaged -> BackupStatus.Failed(R.string.restore_wrong_password)
            RestoreResult.NotABackup -> BackupStatus.Failed(R.string.restore_not_backup)
            RestoreResult.FromNewerApp -> BackupStatus.Failed(R.string.restore_newer)
        }
    }

    private fun launchTask(block: suspend () -> BackupStatus) {
        status = BackupStatus.Working
        viewModelScope.launch {
            status = try {
                block()
            } catch (e: Exception) {
                BackupStatus.Failed(R.string.backup_failed)
            }
        }
    }
}

@Composable
fun BackupScreen(onBack: () -> Unit, viewModel: BackupViewModel = hiltViewModel()) {
    val vm = viewModel
    val context = LocalContext.current
    var tried by remember { mutableStateOf(false) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let(vm::saveTo)
    }
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        restoreUri = uri
    }
    fun passwordOk(): Boolean {
        tried = true
        return !vm.passwordTooShort && !vm.mismatch
    }

    AppScaffold(title = stringResource(R.string.backup_title), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatusCard(vm.status)

            SectionTitle(stringResource(R.string.backup_create))
            Text(stringResource(R.string.backup_intro), style = MaterialTheme.typography.bodyMedium)
            FormField(
                vm.password, { vm.password = it }, stringResource(R.string.backup_password),
                error = if (tried && vm.passwordTooShort) stringResource(R.string.backup_password_rule) else null,
                keyboardType = KeyboardType.Password,
            )
            FormField(
                vm.confirm, { vm.confirm = it }, stringResource(R.string.backup_password_confirm),
                error = if (tried && vm.mismatch) stringResource(R.string.pin_mismatch) else null,
                keyboardType = KeyboardType.Password,
            )
            Text(
                stringResource(R.string.backup_password_warning),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Button(
                onClick = { if (passwordOk()) saveLauncher.launch(vm.fileName) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) { Text(stringResource(R.string.backup_save_file)) }
            OutlinedButton(
                onClick = { if (passwordOk()) vm.share { context.startActivity(it) } },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text(stringResource(R.string.backup_share)) }

            SectionTitle(stringResource(R.string.restore_title))
            Text(stringResource(R.string.restore_intro), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(
                onClick = { openLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text(stringResource(R.string.restore_choose)) }
        }
    }

    restoreUri?.let { uri ->
        RestoreDialog(
            onDismiss = { restoreUri = null },
            onRestore = { pwd ->
                vm.restore(uri, pwd)
                restoreUri = null
            },
        )
    }
}

@Composable
private fun StatusCard(status: BackupStatus) {
    when (status) {
        BackupStatus.Idle -> Unit
        BackupStatus.Working -> CircularProgressIndicator()
        is BackupStatus.Done -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            val text = status.arg?.let { stringResource(status.message, it) } ?: stringResource(status.message)
            Text(text, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
        }
        is BackupStatus.Failed -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Text(stringResource(status.message), modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun RestoreDialog(onDismiss: () -> Unit, onRestore: (String) -> Unit) {
    var pwd by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.restore_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.restore_replace_warning), color = MaterialTheme.colorScheme.error)
                FormField(pwd, { pwd = it }, stringResource(R.string.backup_password), keyboardType = KeyboardType.Password)
            }
        },
        confirmButton = {
            TextButton(onClick = { onRestore(pwd) }, enabled = pwd.isNotEmpty()) { Text(stringResource(R.string.restore_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
