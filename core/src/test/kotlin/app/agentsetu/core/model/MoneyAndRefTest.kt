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
}
