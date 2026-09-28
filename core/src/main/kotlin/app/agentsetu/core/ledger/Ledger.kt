package app.agentsetu.core.ledger

import app.agentsetu.core.model.CommissionStatus

object Ledger {

    /** Status after the user records [receivedPaise] against an entry expecting [expectedPaise]. */
    fun statusAfterReceipt(expectedPaise: Long?, receivedPaise: Long): CommissionStatus = when {
        receivedPaise <= 0 -> if (expectedPaise == null) CommissionStatus.NO_RULE else CommissionStatus.EXPECTED
        expectedPaise == null -> CommissionStatus.RECEIVED
        receivedPaise >= expectedPaise -> CommissionStatus.RECEIVED
        else -> CommissionStatus.PARTLY_RECEIVED
    }

    /** What is still to come for one entry: never negative, and nothing when no rule matched. */
    fun outstanding(expectedPaise: Long?, receivedPaise: Long?): Long =
        ((expectedPaise ?: 0) - (receivedPaise ?: 0)).coerceAtLeast(0)

    /** Sum of the receipts recorded against an entry; null when there are none (nothing received yet). */
    fun receivedTotal(receiptPaise: List<Long>): Long? = receiptPaise.takeIf { it.isNotEmpty() }?.sum()

    data class Line(val expectedPaise: Long?, val receivedPaise: Long?, val status: CommissionStatus)

    data class Totals(val expectedPaise: Long, val receivedPaise: Long, val pendingCount: Int, val noRuleCount: Int) {
        val shortfallPaise: Long get() = (expectedPaise - receivedPaise).coerceAtLeast(0)
    }

    fun totals(lines: List<Line>): Totals = Totals(
        expectedPaise = lines.sumOf { it.expectedPaise ?: 0 },
        receivedPaise = lines.sumOf { it.receivedPaise ?: 0 },
        pendingCount = lines.count { it.status == CommissionStatus.EXPECTED || it.status == CommissionStatus.PARTLY_RECEIVED },
        noRuleCount = lines.count { it.status == CommissionStatus.NO_RULE },
    )

    /**
     * Monthly incentive statement against the ledger. Positive [vsExpected] means the statement
     * pays more than the app expected (a rule may be low or an entry missing); negative, less.
     */
    data class StatementMatch(val statementPaise: Long, val vsExpected: Long, val vsReceived: Long)

    fun matchStatement(statementPaise: Long, totals: Totals): StatementMatch =
        StatementMatch(statementPaise, statementPaise - totals.expectedPaise, statementPaise - totals.receivedPaise)
}
