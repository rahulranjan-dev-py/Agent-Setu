package app.agentsetu.core.model

import java.math.BigDecimal
import java.math.RoundingMode

/** Money is stored as whole paise (Long) in the database and handled as BigDecimal rupees in logic. */
object Money {
    private val HUNDRED = BigDecimal(100)

    fun toPaise(rupees: BigDecimal): Long =
        rupees.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()

    fun fromPaise(paise: Long): BigDecimal = BigDecimal.valueOf(paise, 2)

    fun percentOf(amount: BigDecimal, percent: BigDecimal): BigDecimal =
        amount.multiply(percent).divide(HUNDRED, 2, RoundingMode.HALF_UP)
}
