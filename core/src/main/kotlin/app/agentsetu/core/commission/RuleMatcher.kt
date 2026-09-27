package app.agentsetu.core.commission

import app.agentsetu.core.catalogue.ProductCodes
import app.agentsetu.core.model.PolicyCategory

sealed interface RuleMatch {
    data class Found(val rule: RuleSpec) : RuleMatch

    /** Nothing applies. The app shows "No rule - add one"; it never guesses an amount. */
    data object NoRule : RuleMatch

    /** Two or more equally specific rules apply (usually overlapping user edits); the user must pick. */
    data class Ambiguous(val rules: List<RuleSpec>) : RuleMatch
}

object RuleMatcher {

    /** [rules] must already exclude soft-deleted rows. */
    fun match(rules: List<RuleSpec>, query: CommissionQuery): RuleMatch {
        val candidates = rules.filter { applies(it, query) }
        if (candidates.isEmpty()) return RuleMatch.NoRule
        val best = candidates.maxOf { specificity(it, query) }
        val top = candidates.filter { specificity(it, query) == best }
        return if (top.size == 1) RuleMatch.Found(top.single()) else RuleMatch.Ambiguous(top)
    }

    fun applies(rule: RuleSpec, query: CommissionQuery): Boolean {
        if (!ProductCodes.covers(rule.productCode, query.productCode)) return false
        if (rule.policyCategory != PolicyCategory.ANY && rule.policyCategory != query.policyCategory) return false
        if (rule.staffTypes.none { query.staffType.isCoveredBy(it) }) return false
        if (!rule.isInForceOn(query.date)) return false
        if (!yearMatches(rule.yearOfPolicy, query.policyYear)) return false
        return termMatches(rule, query.premiumTermYears)
    }

    private fun yearMatches(ruleYear: Int?, policyYear: Int?): Boolean = when {
        ruleYear == null -> true
        policyYear == null -> false
        ruleYear == 1 -> policyYear == 1
        else -> policyYear >= 2
    }

    private fun termMatches(rule: RuleSpec, term: Int?): Boolean {
        val min = rule.minPremiumTermYears
        val max = rule.maxPremiumTermYears
        if (min == null && max == null) return true
        if (term == null) return false // a term-banded rule needs the policy's term
        return (min == null || term >= min) && (max == null || term <= max)
    }

    /** Higher is more specific: exact product code, then named policy category, term band, year. */
    private fun specificity(rule: RuleSpec, query: CommissionQuery): Int {
        var score = 0
        if (ProductCodes.isExact(rule.productCode, query.productCode)) score += 8
        if (rule.policyCategory != PolicyCategory.ANY) score += 4
        if (rule.minPremiumTermYears != null || rule.maxPremiumTermYears != null) score += 2
        if (rule.yearOfPolicy != null) score += 1
        return score
    }
}
