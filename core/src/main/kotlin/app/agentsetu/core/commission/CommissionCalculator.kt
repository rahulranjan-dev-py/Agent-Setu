package app.agentsetu.core.commission

import app.agentsetu.core.model.CommissionBasis
import app.agentsetu.core.model.Money
import java.math.BigDecimal

sealed interface CommissionResult {
    data class Expected(val rule: RuleSpec, val amount: BigDecimal) : CommissionResult
    data object NoRule : CommissionResult
    data class Ambiguous(val rules: List<RuleSpec>) : CommissionResult
}

object CommissionCalculator {

    /**
     * Expected commission on [baseAmount] rupees: the premium, deposit or net accretion the rule's
     * basis refers to. For FLAT_PER_CASE the base is ignored and the rule's rupee amount is used.
     */
    fun expected(rules: List<RuleSpec>, query: CommissionQuery, baseAmount: BigDecimal): CommissionResult =
        when (val match = RuleMatcher.match(rules, query)) {
            is RuleMatch.Found -> CommissionResult.Expected(match.rule, amountFor(match.rule, baseAmount))
            RuleMatch.NoRule -> CommissionResult.NoRule
            is RuleMatch.Ambiguous -> CommissionResult.Ambiguous(match.rules)
        }

    fun amountFor(rule: RuleSpec, baseAmount: BigDecimal): BigDecimal = when (rule.basis) {
        CommissionBasis.FLAT_PER_CASE -> rule.rate.setScale(2)
        CommissionBasis.PERCENT_OF_PREMIUM,
        CommissionBasis.PERCENT_OF_DEPOSIT,
        CommissionBasis.PERCENT_OF_NET_ACCRETION,
        -> Money.percentOf(baseAmount, rule.rate)
    }
}
