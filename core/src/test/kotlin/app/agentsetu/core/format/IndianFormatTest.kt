package app.agentsetu.core.format

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IndianFormatTest {

    @Test
    fun groupsDigitsTheIndianWay() {
        assertEquals("0", IndianFormat.number(0L))
        assertEquals("999", IndianFormat.number(999L))
        assertEquals("1,000", IndianFormat.number(1_000L))
        assertEquals("1,00,000", IndianFormat.number(100_000L))
        assertEquals("12,34,567", IndianFormat.number(1_234_567L))
        assertEquals("1,23,45,67,890", IndianFormat.number(1_234_567_890L))
        assertEquals("-10,00,000", IndianFormat.number(-1_000_000L))
    }

    @Test
    fun formatsDecimals() {
        assertEquals("1,00,000.50", IndianFormat.number(BigDecimal("100000.5")))
        assertEquals("1,234.57", IndianFormat.number(BigDecimal("1234.565")))
        assertEquals("1,235", IndianFormat.number(BigDecimal("1234.5"), decimals = 0))
    }

    @Test
    fun formatsRupees() {
        assertEquals("₹1,00,000", IndianFormat.rupees(BigDecimal("100000")))
        assertEquals("₹1,234.50", IndianFormat.rupees(BigDecimal("1234.5")))
        assertEquals("-₹500", IndianFormat.rupees(BigDecimal("-500")))
    }

    @Test
    fun formatsAndParsesDates() {
        val date = LocalDate.of(2026, 11, 2)
        assertEquals("02-11-2026", IndianFormat.date(date))
        assertEquals(date, IndianFormat.parseDate("02-11-2026"))
        assertNull(IndianFormat.parseDate("31-02-2026"))
        assertNull(IndianFormat.parseDate("2026-11-02"))
    }
}
