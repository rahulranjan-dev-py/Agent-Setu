package app.agentsetu.core.ledger

import app.agentsetu.core.catalogue.CatalogueProduct
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object PolicyDates {

    /** 1 during the first 12 months from [start], 2 in the next 12, and so on. */
    fun policyYear(start: LocalDate, on: LocalDate): Int {
        require(!on.isBefore(start)) { "Date is before the policy started" }
        return (ChronoUnit.MONTHS.between(start, on) / 12).toInt() + 1
    }

    /** A suggestion only; the user can always change the maturity date. */
    fun suggestedMaturity(product: CatalogueProduct, start: LocalDate): LocalDate? =
        product.defaultTermMonths?.let { start.plusMonths(it.toLong()) }
}
