package app.agentsetu.core.commission

import app.agentsetu.core.model.CommissionBasis
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommissionCalculatorTest {

    @Test
    fun percentageRoundsHalfUpToPaise() {
        val r = rule("r", "RPLI", "2.5")
        // 2.5% of 1,234.50 = 30.8625 -> 30.86
        assertEquals(BigDecimal("30.86"), CommissionCalculator.amountFor(r, BigDecimal("1234.50")))
        // 10% of 4,567.85 = 456.785 -> 456.79
        assertEquals(BigDecimal("456.79"), CommissionCalculator.amountFor(r.copy(rate = BigDecimal("10")), BigDecimal("4567.85")))
    }

    @Test
    fun flatPerCaseIgnoresBase() {
        val r = rule("flat", "SB", "48.43", basis = CommissionBasis.FLAT_PER_CASE)
        assertEquals(BigDecimal("48.43"), CommissionCalculator.amountFor(r, BigDecimal("99999")))
    }

    @Test
    fun noRuleNeverProducesAnAmount() {
        val result = CommissionCalculator.expected(emptyList(), query("PLI", policyYear = 1), BigDecimal("1000"))
        assertEquals(CommissionResult.NoRule, result)
    }

    @Test
    fun expectedCarriesTheRuleUsed() {
        val r = rule("r", "RPLI", "10", yearOfPolicy = 1)
        val result = CommissionCalculator.expected(listOf(r), query("RPLI", policyYear = 1), BigDecimal("6000"))
        assertTrue(result is CommissionResult.Expected)
        result as CommissionResult.Expected
        assertEquals("r", result.rule.id)
        assertEquals(BigDecimal("600.00"), result.amount)
    }
}
