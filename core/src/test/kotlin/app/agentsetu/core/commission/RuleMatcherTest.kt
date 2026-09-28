package app.agentsetu.core.commission

import app.agentsetu.core.model.PolicyCategory
import app.agentsetu.core.model.StaffType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleMatcherTest {

    private fun foundId(rules: List<RuleSpec>, q: CommissionQuery): String {
        val m = RuleMatcher.match(rules, q)
        assertTrue("expected a single match, got $m", m is RuleMatch.Found)
        return (m as RuleMatch.Found).rule.id
    }

    @Test
    fun exactProductCodeBeatsFamilyRule() {
        val rules = listOf(rule("td-all", "TD", "0.5"), rule("td-5", "TD_5Y", "2"))
        assertEquals("td-5", foundId(rules, query("TD_5Y")))
        assertEquals("td-all", foundId(rules, query("TD_3Y")))
    }

    @Test
    fun familyPrefixNeedsUnderscoreBoundary() {
        val rules = listOf(rule("rd", "RD", "4"))
        assertEquals(RuleMatch.NoRule, RuleMatcher.match(rules, query("RDX")))
        assertEquals("rd", foundId(rules, query("RD_5Y")))
    }

    @Test
    fun termBandsAndCategories() {
        val rules = listOf(
            rule("le15", "PLI", "4", PolicyCategory.NON_AEA, yearOfPolicy = 1, maxTerm = 15),
            rule("16to25", "PLI", "10", PolicyCategory.NON_AEA, yearOfPolicy = 1, minTerm = 16, maxTerm = 25),
            rule("aea", "PLI", "5", PolicyCategory.AEA, yearOfPolicy = 1, maxTerm = 15),
        )
        assertEquals("le15", foundId(rules, query("PLI", policyCategory = PolicyCategory.NON_AEA, term = 15, policyYear = 1)))
        assertEquals("16to25", foundId(rules, query("PLI", policyCategory = PolicyCategory.NON_AEA, term = 16, policyYear = 1)))
        assertEquals("aea", foundId(rules, query("PLI", policyCategory = PolicyCategory.AEA, term = 10, policyYear = 1)))
        // Term unknown -> a banded rule cannot be chosen.
        assertEquals(RuleMatch.NoRule, RuleMatcher.match(rules, query("PLI", policyCategory = PolicyCategory.NON_AEA, policyYear = 1)))
    }

    @Test
    fun renewalRuleCoversEveryLaterYear() {
        val rules = listOf(rule("y1", "RPLI", "10", yearOfPolicy = 1), rule("ren", "RPLI", "2.5", yearOfPolicy = 2))
        assertEquals("y1", foundId(rules, query("RPLI", policyYear = 1)))
        assertEquals("ren", foundId(rules, query("RPLI", policyYear = 2)))
        assertEquals("ren", foundId(rules, query("RPLI", policyYear = 17)))
        assertEquals(RuleMatch.NoRule, RuleMatcher.match(rules, query("RPLI", policyYear = null)))
    }

    @Test
    fun staffTypeAndBpmInheritsGds() {
        val rules = listOf(rule("gds-only", "RPLI", "10", staffTypes = setOf(StaffType.GDS)))
        assertEquals("gds-only", foundId(rules, query("RPLI", staffType = StaffType.GDS_BPM)))
        assertEquals(RuleMatch.NoRule, RuleMatcher.match(rules, query("RPLI", staffType = StaffType.SAS_AGENT)))
    }

    @Test
    fun effectiveDatesAreInclusive() {
        val rules = listOf(
            rule("old", "MIS", "1", from = "2020-01-01", to = "2025-03-31"),
            rule("new", "MIS", "0.5", from = "2025-04-01"),
        )
        assertEquals("old", foundId(rules, query("MIS", date = "2025-03-31")))
        assertEquals("new", foundId(rules, query("MIS", date = "2025-04-01")))
        assertEquals(RuleMatch.NoRule, RuleMatcher.match(rules, query("MIS", date = "2019-12-31")))
    }

    @Test
    fun equallySpecificOverlapIsReportedNotGuessed() {
        val rules = listOf(rule("a", "NSC", "0.5"), rule("b", "NSC", "1"))
        val m = RuleMatcher.match(rules, query("NSC"))
        assertTrue(m is RuleMatch.Ambiguous)
        assertEquals(setOf("a", "b"), (m as RuleMatch.Ambiguous).rules.map { it.id }.toSet())
    }

    @Test
    fun userRuleWinsTieAgainstSample() {
        val sample = rule("sample", "TD_5Y", "0.5")
        val mine = rule("mine", "TD_5Y", "1").copy(userDefined = true)
        assertEquals("mine", foundId(listOf(sample, mine), query("TD_5Y")))
        // Two user rules of the same shape are still a clash the user must fix.
        val other = rule("other", "TD_5Y", "2").copy(userDefined = true)
        assertTrue(RuleMatcher.match(listOf(sample, mine, other), query("TD_5Y")) is RuleMatch.Ambiguous)
    }
}
