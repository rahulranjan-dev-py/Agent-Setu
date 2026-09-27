package app.agentsetu.core.calc

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.pow

/**
 * Indicative Post Office scheme calculators. The rate always comes from the user's editable
 * InterestRate table (or what they type); nothing here holds a rate. Results are estimates to
 * explain a scheme to a customer, not a statement of what the Post Office will pay.
 */
object Calculators {

    data class TdResult(val yearlyInterest: BigDecimal, val totalInterest: BigDecimal, val principal: BigDecimal)

    /**
     * Time Deposit: interest is calculated quarterly and paid out every year, so the yearly payout
     * is P × ((1 + r/400)^4 − 1) and the principal comes back at maturity.
     */
    fun timeDeposit(principal: BigDecimal, ratePercent: BigDecimal, years: Int): TdResult {
        require(years in 1..10)
        val factor = (1 + ratePercent.toDouble() / 400).pow(4) - 1
        val yearly = money(principal.toDouble() * factor)
        return TdResult(yearly, yearly.multiply(BigDecimal(years)), principal)
    }

    data class RdResult(val deposited: BigDecimal, val maturity: BigDecimal) {
        val interest: BigDecimal get() = maturity - deposited
    }

    /**
     * Recurring Deposit: each monthly instalment grows with quarterly compounding for the months
     * left until maturity. Matches the Post Office table (₹10,000 a month for 5 years at 6.7%
     * gives ₹7,13,659).
     */
    fun recurringDeposit(monthly: BigDecimal, ratePercent: BigDecimal, months: Int = 60): RdResult {
        require(months in 1..120)
        val q = 1 + ratePercent.toDouble() / 400
        val total = (0 until months).sumOf { i -> monthly.toDouble() * q.pow((months - i) / 3.0) }
        return RdResult(monthly.multiply(BigDecimal(months)), money(total))
    }

    data class MisResult(val monthlyInterest: BigDecimal, val totalInterest: BigDecimal, val principal: BigDecimal)

    /** Monthly Income Scheme: simple interest paid every month; principal back after 5 years. */
    fun monthlyIncome(principal: BigDecimal, ratePercent: BigDecimal, months: Int = 60): MisResult {
        val monthly = principal.multiply(ratePercent).divide(BigDecimal(1200), 2, RoundingMode.HALF_UP)
        return MisResult(monthly, monthly.multiply(BigDecimal(months)), principal)
    }

    private fun money(value: Double): BigDecimal = BigDecimal(value).setScale(2, RoundingMode.HALF_UP)
}
