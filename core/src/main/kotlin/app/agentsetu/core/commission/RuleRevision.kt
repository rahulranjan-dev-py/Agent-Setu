package app.agentsetu.core.commission

import java.time.LocalDate

/**
 * How a rate change is stored without losing history. A rule is never edited in place:
 *  - a change from a later date closes the old rule the day before and adds a new rule;
 *  - a change from the same (or an earlier) start date replaces the old rule, which is soft-deleted
 *    and kept, e.g. when a sample rate turns out to be wrong from the start.
 * Commission entries store the rule id and rate they used, so past entries never change.
 */
object RuleRevision {

    data class Outcome(
        /** The old rule as it should now be saved (closed or soft-deleted). */
        val old: RuleSpec,
        val oldDeleted: Boolean,
        val new: RuleSpec,
    )

    fun revise(current: RuleSpec, changed: RuleSpec, newId: String): Outcome {
        require(newId != current.id) { "The new rule needs a new id" }
        val from = changed.effectiveFrom
        current.effectiveTo?.let {
            require(!from.isAfter(it)) { "Rule ended on $it; add a new rule instead of revising it" }
        }
        val new = changed.copy(id = newId, effectiveTo = changed.effectiveTo ?: current.effectiveTo)
        return if (from.isAfter(current.effectiveFrom)) {
            Outcome(old = current.copy(effectiveTo = from.minusDays(1)), oldDeleted = false, new = new)
        } else {
            Outcome(old = current, oldDeleted = true, new = new)
        }
    }

    /** Convenience for the common case: same rule, new rate from [from]. */
    fun changeRate(current: RuleSpec, newRate: java.math.BigDecimal, from: LocalDate, newId: String): Outcome =
        revise(current, current.copy(rate = newRate, effectiveFrom = from, effectiveTo = null), newId)
}
