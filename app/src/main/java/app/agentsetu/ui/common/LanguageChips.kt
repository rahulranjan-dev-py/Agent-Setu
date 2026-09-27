package app.agentsetu.ui.common

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import app.agentsetu.R

/** Hindi / English switch. Changing it restarts the activity in the chosen language. */
@Composable
fun LanguageChips() {
    val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LanguageChip(stringResource(R.string.language_hindi), "hi", current)
        LanguageChip(stringResource(R.string.language_english), "en", current)
    }
}

@Composable
private fun LanguageChip(label: String, tag: String, current: String) {
    FilterChip(
        selected = current.startsWith(tag),
        onClick = { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag)) },
        label = { Text(label, style = MaterialTheme.typography.titleMedium) },
        modifier = Modifier.heightIn(min = 48.dp),
    )
}
