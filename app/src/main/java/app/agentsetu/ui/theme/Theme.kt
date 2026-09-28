package app.agentsetu.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

// Navy and sea green (chosen 28-09-2026): no postal red or yellow, no IPPB purple, and no dynamic
// colour so the brand stays neutral. Navy is the main colour; sea green marks the add button and
// the selected tab.
private val Navy = Color(0xFF1F4E79)
private val NavyLight = Color(0xFF8FB8E0)
private val SeaGreen = Color(0xFF2A9D8F)
private val SeaGreenLight = Color(0xFF7ED3C7)

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4F2),
    onPrimaryContainer = Color(0xFF0E2A44),
    secondary = SeaGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCEBE6),
    onSecondaryContainer = Color(0xFF0B3F38),
    background = Color(0xFFF7F9FC),
    surface = Color(0xFFF7F9FC),
    surfaceVariant = Color(0xFFE6ECF3),
    onSurfaceVariant = Color(0xFF44505C),
    outline = Color(0xFF74808D),
)

private val DarkColors = darkColorScheme(
    primary = NavyLight,
    onPrimary = Color(0xFF0B2740),
    primaryContainer = Color(0xFF173C5E),
    onPrimaryContainer = Color(0xFFD6E4F2),
    secondary = SeaGreenLight,
    onSecondary = Color(0xFF05332D),
    secondaryContainer = Color(0xFF1D6A60),
    onSecondaryContainer = Color(0xFFCCEBE6),
    background = Color(0xFF0F1318),
    surface = Color(0xFF0F1318),
    surfaceVariant = Color(0xFF28313C),
    onSurfaceVariant = Color(0xFFC3CBD5),
    outline = Color(0xFF8D98A4),
)

// Slightly larger body text than the Material default: field users, small phones, bright sunlight.
private val base = Typography()
private val AgentSetuTypography = base.copy(
    bodyLarge = base.bodyLarge.merge(TextStyle(fontSize = 18.sp, lineHeight = 26.sp)),
    bodyMedium = base.bodyMedium.merge(TextStyle(fontSize = 16.sp, lineHeight = 23.sp)),
    labelLarge = base.labelLarge.merge(TextStyle(fontSize = 16.sp)),
)

@Composable
fun AgentSetuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AgentSetuTypography,
        content = content,
    )
}
