package app.agentsetu.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import app.agentsetu.core.format.IndianFormat
import app.agentsetu.core.model.Money
import java.time.LocalDate
import java.util.Locale

fun rupees(paise: Long): String = IndianFormat.rupees(Money.fromPaise(paise))

fun LocalDate.display(): String = IndianFormat.date(this)

/** Plain amount for pre-filling an input field, e.g. 1234.5 -> "1234.50", 1200 -> "1200". */
fun editableAmount(paise: Long): String {
    val value = Money.fromPaise(paise)
    return if (paise % 100 == 0L) value.toBigInteger().toString() else value.toPlainString()
}

@Composable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

@Composable
fun isHindi(): Boolean = currentLocale().language == "hi"

@Composable
fun localized(en: String, hi: String): String = if (isHindi()) hi else en
