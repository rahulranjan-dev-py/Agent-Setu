package app.agentsetu.core.ledger

import app.agentsetu.core.catalogue.DefaultProducts
import app.agentsetu.core.model.CommissionStatus
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LedgerTest {

    @Test
    fun policyYears() {
        val start = LocalDate.of(2024, 3, 15)
        assertEquals(1, PolicyDates.policyYear(start, start))
        assertEquals(1, PolicyDates.policyYear(start, LocalDate.of(2025, 3, 14)))
        assertEquals(2, PolicyDates.policyYear(start, LocalDate.of(2025, 3, 15)))
        assertEquals(3, PolicyDates.policyYear(start, LocalDate.of(2026, 9, 28)))
    }

    @Test
    fun maturitySuggestions() {
        val start = LocalDate.of(2026, 1, 10)
        assertEquals(LocalDate.of(2031, 1, 10), PolicyDates.suggestedMaturity(DefaultProducts.byCode("TD_5Y")!!, start))
        assertEquals(LocalDate.of(2035, 8, 10), PolicyDates.suggestedMaturity(DefaultProducts.byCode("KVP")!!, start))
        assertNull(PolicyDates.suggestedMaturity(DefaultProducts.byCode("PLI")!!, start))
    }

    @Test
    fun receiptStatus() {
        assertEquals(CommissionStatus.RECEIVED, Ledger.statusAfterReceipt(60000, 60000))
        assertEquals(CommissionStatus.RECEIVED, Ledger.statusAfterReceipt(60000, 61000))
        assertEquals(CommissionStatus.PARTLY_RECEIVED, Ledger.statusAfterReceipt(60000, 30000))
        assertEquals(CommissionStatus.RECEIVED, Ledger.statusAfterReceipt(null, 5000))
        assertEquals(CommissionStatus.EXPECTED, Ledger.statusAfterReceipt(60000, 0))
        assertEquals(CommissionStatus.NO_RULE, Ledger.statusAfterReceipt(null, 0))
    }

    @Test
    fun monthTotals() {
        val t = Ledger.totals(
            listOf(
                Ledger.Line(120000, 120000, CommissionStatus.RECEIVED),
                Ledger.Line(60000, 20000, CommissionStatus.PARTLY_RECEIVED),
                Ledger.Line(5000, null, CommissionStatus.EXPECTED),
                Ledger.Line(null, null, CommissionStatus.NO_RULE),
            ),
        )
        assertEquals(185000, t.expectedPaise)
        assertEquals(140000, t.receivedPaise)
        assertEquals(45000, t.shortfallPaise)
        assertEquals(2, t.pendingCount)
        assertEquals(1, t.noRuleCount)
    }

    @Test
    fun outstandingAndReceipts() {
        assertEquals(40000, Ledger.outstanding(60000, 20000))
        assertEquals(0, Ledger.outstanding(60000, 70000))
        assertEquals(60000, Ledger.outstanding(60000, null))
        assertEquals(0, Ledger.outstanding(null, 5000))
        assertNull(Ledger.receivedTotal(emptyList()))
        assertEquals(30000L, Ledger.receivedTotal(listOf(10000, 20000)))
    }

    @Test
    fun statementMatch() {
        val totals = Ledger.Totals(expectedPaise = 185000, receivedPaise = 140000, pendingCount = 2, noRuleCount = 0)
        val m = Ledger.matchStatement(180000, totals)
        assertEquals(-5000, m.vsExpected)
        assertEquals(40000, m.vsReceived)
    }
}
