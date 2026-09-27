package app.agentsetu.core.format

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

/** Indian-style number and date formatting: 1,00,000 and DD-MM-YYYY. */
object IndianFormat {

    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT)

    /** Groups digits as 12,34,56,789 (last three, then pairs). */
    fun number(value: Long): String {
        if (value == Long.MIN_VALUE) return "-" + groupDigits(value.toString().substring(1))
        val digits = groupDigits(kotlin.math.abs(value).toString())
        return if (value < 0) "-$digits" else digits
    }

    /** Groups the integer part and keeps [decimals] fraction digits, rounding half-up. */
    fun number(value: BigDecimal, decimals: Int = 2): String {
        val scaled = value.setScale(decimals, RoundingMode.HALF_UP)
        val negative = scaled.signum() < 0
        val plain = scaled.abs().toPlainString()
        val intPart = plain.substringBefore('.')
        val fracPart = if (decimals > 0) "." + plain.substringAfter('.') else ""
        return (if (negative) "-" else "") + groupDigits(intPart) + fracPart
    }

    /** ₹ amount with paise shown only when non-zero, e.g. ₹1,00,000 or ₹1,234.50. */
    fun rupees(value: BigDecimal): String {
        val scaled = value.setScale(2, RoundingMode.HALF_UP)
        val decimals = if (scaled.remainder(BigDecimal.ONE).signum() == 0) 0 else 2
        val text = number(scaled, decimals)
        return if (text.startsWith("-")) "-₹" + text.substring(1) else "₹$text"
    }

    fun date(value: LocalDate): String = value.format(dateFormatter)

    /** Parses DD-MM-YYYY; returns null for anything else, including impossible dates like 31-02-2026. */
    fun parseDate(text: String): LocalDate? = try {
        LocalDate.parse(text.trim(), dateFormatter)
    } catch (e: DateTimeParseException) {
        null
    }

    private fun groupDigits(digits: String): String {
        if (digits.length <= 3) return digits
        val lastThree = digits.takeLast(3)
        val rest = digits.dropLast(3)
        val pairs = rest.reversed().chunked(2).joinToString(",").reversed()
        return "$pairs,$lastThree"
    }
}
