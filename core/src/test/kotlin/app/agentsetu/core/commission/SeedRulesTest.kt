package app.agentsetu.core.commission

import app.agentsetu.core.catalogue.DefaultProducts
import app.agentsetu.core.model.PolicyCategory
import app.agentsetu.core.model.StaffType
import app.agentsetu.core.seed.SeedJson
import app.agentsetu.core.seed.toSpec
import java.io.File
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Worked examples against the bundled sample rules, as a user would see them. */
class SeedRulesTest {

    private val rules = SeedJson.parseCommission(
        File(System.getProperty("agentsetu.seedDir"), SeedJson.COMMISSION_FILE).readText(),
    ).commissionRules.filterNot { it.deleted }.map { it.toSpec() }

    private fun rate(q: CommissionQuery): BigDecimal? =
        (RuleMatcher.match(rules, q) as? RuleMatch.Found)?.rule?.rate

    private fun pli(category: PolicyCategory, term: Int, year: Int, staff: StaffType = StaffType.GDS) =
        query("PLI", staff, category, term, year)

    @Test
    fun pliProcurementBands() {
        assertEquals(BigDecimal("4.0"), rate(pli(PolicyCategory.NON_AEA, 15, 1)))
        assertEquals(BigDecimal("10.0"), rate(pli(PolicyCategory.NON_AEA, 20, 1, StaffType.DEPARTMENTAL)))
        assertEquals(BigDecimal("20.0"), rate(pli(PolicyCategory.NON_AEA, 30, 1)))
        assertEquals(BigDecimal("5.0"), rate(pli(PolicyCategory.AEA, 15, 1)))
        assertEquals(BigDecimal("7.0"), rate(pli(PolicyCategory.AEA, 20, 1, StaffType.ODE)))
    }

    @Test
    fun renewals() {
        assertEquals(BigDecimal("1.0"), rate(pli(PolicyCategory.NON_AEA, 20, 2)))
        assertEquals(BigDecimal("1.0"), rate(pli(PolicyCategory.AEA, 10, 9)))
        assertEquals(BigDecimal("2.5"), rate(query("RPLI", policyYear = 4)))
        assertEquals(BigDecimal("10.0"), rate(query("RPLI", StaffType.GDS_BPM, policyYear = 1)))
    }

    @Test
    fun savingsAgentsAndBpm() {
        assertEquals(BigDecimal("0.5"), rate(query("TD_5Y", StaffType.SAS_AGENT)))
        assertEquals(BigDecimal("4.0"), rate(query("RD_5Y", StaffType.MPKBY_AGENT)))
        assertEquals(BigDecimal("0.0"), rate(query("PPF", StaffType.SAS_AGENT)))
        assertEquals(BigDecimal("2.0"), rate(query("TD_5Y", StaffType.GDS_BPM)))
        assertEquals(BigDecimal("0.5"), rate(query("TD_2Y", StaffType.GDS_BPM)))
        assertEquals(BigDecimal("1.0"), rate(query("SB", StaffType.GDS_BPM)))
        // Departmental employees and ordinary GDS are not small-savings agents.
        assertEquals(null, rate(query("TD_5Y", StaffType.DEPARTMENTAL)))
        assertEquals(null, rate(query("TD_5Y", StaffType.GDS)))
    }

    @Test
    fun workedExample() {
        // GDS, non-AEA PLI, 20-year term, first-year premium of 12,000 -> 10% = 1,200.
        val result = CommissionCalculator.expected(rules, pli(PolicyCategory.NON_AEA, 20, 1), BigDecimal("12000"))
        assertEquals(BigDecimal("1200.00"), (result as CommissionResult.Expected).amount)
    }

    @Test
    fun seedNeverGivesAnAmbiguousAnswer() {
        for (product in DefaultProducts.all) for (staff in StaffType.entries) for (cat in PolicyCategory.entries)
            for (term in listOf(null, 5, 15, 16, 25, 26, 40)) for (year in listOf(null, 1, 2, 5)) {
                val q = query(product.code, staff, cat, term, year)
                assertFalse("ambiguous for $q", RuleMatcher.match(rules, q) is RuleMatch.Ambiguous)
            }
    }
}
