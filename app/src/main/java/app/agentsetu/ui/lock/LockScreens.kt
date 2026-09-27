package app.agentsetu.ui.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.agentsetu.R
import app.agentsetu.core.security.PinHasher
import app.agentsetu.security.AppLock
import app.agentsetu.security.Biometrics
import app.agentsetu.security.DataEraser
import app.agentsetu.security.PinAttempt
import app.agentsetu.ui.common.AppScaffold
import app.agentsetu.ui.common.CheckRow
import kotlinx.coroutines.launch

@Composable
fun PinField(value: String, onValueChange: (String) -> Unit, label: String, error: String? = null) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(6)) },
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun LockScreen(appLock: AppLock) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var forgot by remember { mutableStateOf(false) }
    val wrong = stringResource(R.string.lock_wrong_pin)
    val waitText = stringResource(R.string.lock_wait)
    val bioTitle = stringResource(R.string.lock_biometric_title)
    val cancel = stringResource(R.string.action_cancel)
    val canUseBiometric = appLock.biometricEnabled && Biometrics.isAvailable(context)

    fun useBiometric() = Biometrics.prompt(context, bioTitle, cancel) { appLock.unlockWithBiometric() }

    LaunchedEffect(Unit) { if (canUseBiometric) useBiometric() }

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
            Text(stringResource(R.string.lock_enter_pin), style = MaterialTheme.typography.titleMedium)
            PinField(pin, { pin = it }, stringResource(R.string.lock_pin), message)
            Button(
                onClick = {
                    scope.launch {
                        when (val result = appLock.tryPin(pin)) {
                            PinAttempt.Ok -> Unit
                            PinAttempt.Wrong -> { message = wrong; pin = "" }
                            is PinAttempt.Wait -> {
                                val seconds = ((result.untilMs - System.currentTimeMillis()) / 1000).coerceAtLeast(1)
                                message = waitText.format(seconds)
                                pin = ""
                            }
                        }
                    }
                },
                enabled = PinHasher.isValidPin(pin),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) { Text(stringResource(R.string.lock_unlock)) }
            if (canUseBiometric) {
                OutlinedButton(onClick = { useBiometric() }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text(stringResource(R.string.lock_use_biometric))
                }
            }
            TextButton(onClick = { forgot = true }) { Text(stringResource(R.string.lock_forgot)) }
        }
    }

    if (forgot) EraseDialog(title = R.string.lock_forgot, body = R.string.lock_forgot_body, onDismiss = { forgot = false }) {
        DataEraser.eraseEverything(context)
    }
}

/** Confirmation for wiping every piece of data on this phone. */
@Composable
fun EraseDialog(title: Int, body: Int, onDismiss: () -> Unit, onErase: () -> Unit) {
    var understood by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(body))
                CheckRow(understood, { understood = it }, stringResource(R.string.erase_understood))
            }
        },
        confirmButton = {
            Button(
                onClick = onErase,
                enabled = understood,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(R.string.erase_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/**
 * Set or change the PIN. [onSkip] is shown during onboarding only. When a PIN already exists, the
 * current PIN is asked first and the PIN can also be removed.
 */
@Composable
fun PinSetupScreen(appLock: AppLock, onDone: () -> Unit, onSkip: (() -> Unit)?, onBack: (() -> Unit)?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hasPin = appLock.isPinSet
    var current by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var biometric by remember { mutableStateOf(appLock.biometricEnabled) }
    var tried by remember { mutableStateOf(false) }
    var currentError by remember { mutableStateOf<String?>(null) }
    val wrong = stringResource(R.string.lock_wrong_pin)
    val bioAvailable = Biometrics.isAvailable(context)

    suspend fun currentOk(): Boolean {
        if (!hasPin) return true
        return (appLock.tryPin(current) == PinAttempt.Ok).also { if (!it) currentError = wrong }
    }

    AppScaffold(title = stringResource(R.string.pin_title), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.pin_intro), style = MaterialTheme.typography.bodyLarge)
            if (hasPin) PinField(current, { current = it; currentError = null }, stringResource(R.string.pin_current), currentError)
            PinField(
                pin, { pin = it }, stringResource(R.string.pin_new),
                if (tried && !PinHasher.isValidPin(pin)) stringResource(R.string.pin_rule) else null,
            )
            PinField(
                confirm, { confirm = it }, stringResource(R.string.pin_confirm),
                if (tried && confirm != pin) stringResource(R.string.pin_mismatch) else null,
            )
            if (bioAvailable) CheckRow(biometric, { biometric = it }, stringResource(R.string.pin_biometric))
            Text(stringResource(R.string.pin_forget_warning), style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = {
                    tried = true
                    if (PinHasher.isValidPin(pin) && confirm == pin) {
                        scope.launch {
                            if (currentOk()) {
                                appLock.setPin(pin)
                                appLock.biometricEnabled = biometric && bioAvailable
                                onDone()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) { Text(stringResource(R.string.action_save)) }
            if (hasPin) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            if (currentOk()) {
                                appLock.removePin()
                                onDone()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.pin_remove)) }
            }
            if (onSkip != null) {
                TextButton(onClick = onSkip) { Text(stringResource(R.string.pin_skip)) }
            }
        }
    }
}
