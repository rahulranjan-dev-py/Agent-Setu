package app.agentsetu.core.model

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyAndRefTest {

    @Test
    fun paiseRoundTrip() {
        assertEquals(123456L, Money.toPaise(BigDecimal("1234.56")))
        assertEquals(123457L, Money.toPaise(BigDecimal("1234.565")))
        assertEquals(BigDecimal("1234.56"), Money.fromPaise(123456L))
    }

    @Test
    fun onlyFourDigitReferencesAreAllowed() {
        assertTrue(RefLast4.isValid(null))
        assertTrue(RefLast4.isValid("0421"))
        assertFalse(RefLast4.isValid("421"))
        assertFalse(RefLast4.isValid("R-12345678")) // full number pasted by mistake
        assertFalse(RefLast4.isValid("12345"))
    }

    @Test
    fun refNumber() {
        assertTrue(RefNumber.isValid(null))
        assertTrue(RefNumber.isValid("R-12345678"))
        assertTrue(RefNumber.isValid("PLI/DHN/00123456"))
        assertFalse(RefNumber.isValid(""))
        assertFalse(RefNumber.isValid("x".repeat(41)))
        assertFalse(RefNumber.isValid("1234;DROP"))
        assertEquals("5678", RefNumber.last4("R-12345678"))
        assertEquals("3456", RefNumber.last4("PLI/DHN/00123456"))
        assertEquals(null, RefNumber.last4("AB-12"))
        assertEquals(null, RefNumber.last4(null))
        assertEquals("••••5678", RefNumber.masked("12345678"))
    }
}
