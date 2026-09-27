package app.agentsetu.core.commission

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleRevisionTest {

    private val current = rule("old", "PLI", "1", yearOfPolicy = 2, from = "2025-04-01")

    @Test
    fun laterChangeClosesOldRuleTheDayBefore() {
        val out = RuleRevision.changeRate(current, BigDecimal("2"), LocalDate.parse("2026-10-01"), "new")
        assertFalse(out.oldDeleted)
        assertEquals(LocalDate.parse("2026-09-30"), out.old.effectiveTo)
        assertEquals(BigDecimal("1"), out.old.rate)
        assertEquals("new", out.new.id)
        assertEquals(LocalDate.parse("2026-10-01"), out.new.effectiveFrom)
        assertEquals(BigDecimal("2"), out.new.rate)

        // Both versions together give one answer per date.
        val rules = listOf(out.old, out.new)
        val before = RuleMatcher.match(rules, query("PLI", policyYear = 3, date = "2026-09-30")) as RuleMatch.Found
        val after = RuleMatcher.match(rules, query("PLI", policyYear = 3, date = "2026-10-01")) as RuleMatch.Found
        assertEquals("old", before.rule.id)
        assertEquals("new", after.rule.id)
    }

    @Test
    fun sameStartDateReplacesAndSoftDeletesOld() {
        val out = RuleRevision.changeRate(current, BigDecimal("2"), LocalDate.parse("2025-04-01"), "fixed")
        assertTrue(out.oldDeleted)
        assertEquals(current, out.old) // kept unchanged for history
        assertEquals(BigDecimal("2"), out.new.rate)
    }

    @Test(expected = IllegalArgumentException::class)
    fun cannotReviseAfterRuleEnded() {
        val ended = current.copy(effectiveTo = LocalDate.parse("2025-12-31"))
        RuleRevision.changeRate(ended, BigDecimal("2"), LocalDate.parse("2026-01-01"), "new")
    }

    @Test(expected = IllegalArgumentException::class)
    fun newRuleNeedsNewId() {
        RuleRevision.changeRate(current, BigDecimal("2"), LocalDate.parse("2026-01-01"), "old")
    }
}
