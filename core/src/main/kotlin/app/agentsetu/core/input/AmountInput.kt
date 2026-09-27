package app.agentsetu.core.input

import java.math.BigDecimal

/** Parses what users type for money: "1,00,000", "₹ 2500.50", "1200". Returns null if invalid. */
object AmountInput {
    private val PLAIN = Regex("^[0-9]{1,12}(\\.[0-9]{1,2})?$")

    fun parse(text: String): BigDecimal? {
        val cleaned = text.trim().removePrefix("₹").replace(",", "").replace(" ", "")
        return if (PLAIN.matches(cleaned)) BigDecimal(cleaned) else null
    }

    /** Rates and percentages: up to 4 decimals, 0 to 100. */
    fun parsePercent(text: String): BigDecimal? {
        val cleaned = text.trim().removeSuffix("%").trim()
        if (!Regex("^[0-9]{1,3}(\\.[0-9]{1,4})?$").matches(cleaned)) return null
        return BigDecimal(cleaned).takeIf { it <= BigDecimal(100) }
    }
}
