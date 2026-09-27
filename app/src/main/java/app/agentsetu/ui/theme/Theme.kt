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

// Teal and indigo only: no postal red or yellow, and no dynamic colour so the brand stays neutral.
private val Teal = Color(0xFF00796B)
private val TealLight = Color(0xFF4DB6AC)
private val Indigo = Color(0xFF3949AB)
private val IndigoLight = Color(0xFF7986CB)

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB),
    onPrimaryContainer = Color(0xFF00251F),
    secondary = Indigo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC5CAE9),
    onSecondaryContainer = Color(0xFF0A1450),
)

private val DarkColors = darkColorScheme(
    primary = TealLight,
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005047),
    onPrimaryContainer = Color(0xFFB2DFDB),
    secondary = IndigoLight,
    onSecondary = Color(0xFF0A1450),
    secondaryContainer = Color(0xFF283593),
    onSecondaryContainer = Color(0xFFC5CAE9),
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
