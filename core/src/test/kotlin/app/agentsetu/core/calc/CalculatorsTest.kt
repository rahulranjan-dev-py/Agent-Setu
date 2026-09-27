package app.agentsetu.core.calc

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculatorsTest {

    @Test
    fun recurringDepositMatchesPostOfficeTable() {
        // Widely published: ₹10,000 a month for 5 years at 6.7% -> ₹7,13,659.
        val r = Calculators.recurringDeposit(BigDecimal("10000"), BigDecimal("6.7"))
        assertEquals(BigDecimal("713658.29"), r.maturity)
        assertEquals(BigDecimal("600000"), r.deposited)
        assertEquals(BigDecimal("113658.29"), r.interest)
    }

    @Test
    fun timeDepositPaysYearly() {
        val r = Calculators.timeDeposit(BigDecimal("100000"), BigDecimal("7.5"), 5)
        assertEquals(BigDecimal("7713.59"), r.yearlyInterest)
        assertEquals(BigDecimal("38567.95"), r.totalInterest)
        assertEquals(BigDecimal("100000"), r.principal)
    }

    @Test
    fun monthlyIncome() {
        val r = Calculators.monthlyIncome(BigDecimal("900000"), BigDecimal("7.4"))
        assertEquals(BigDecimal("5550.00"), r.monthlyInterest)
        assertEquals(BigDecimal("333000.00"), r.totalInterest)
    }

    @Test
    fun datedRevision() {
        val from = LocalDate.of(2026, 7, 1)
        assertEquals(
            DatedRevision.Old.CloseOn(LocalDate.of(2026, 9, 30)),
            DatedRevision.plan(from, null, LocalDate.of(2026, 10, 1)),
        )
        assertEquals(DatedRevision.Old.SoftDelete, DatedRevision.plan(from, null, from))
    }

    @Test(expected = IllegalArgumentException::class)
    fun cannotReviseEndedValue() {
        DatedRevision.plan(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30), LocalDate.of(2026, 10, 1))
    }
}
