package app.agentsetu.core.input

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InputTest {

    @Test
    fun amounts() {
        assertEquals(BigDecimal("100000"), AmountInput.parse("1,00,000"))
        assertEquals(BigDecimal("2500.50"), AmountInput.parse("₹ 2,500.50"))
        assertNull(AmountInput.parse("-500"))
        assertNull(AmountInput.parse("12.345"))
        assertNull(AmountInput.parse("abc"))
        assertNull(AmountInput.parse(""))
    }

    @Test
    fun percentages() {
        assertEquals(BigDecimal("2.5"), AmountInput.parsePercent("2.5"))
        assertEquals(BigDecimal("10"), AmountInput.parsePercent("10 %"))
        assertNull(AmountInput.parsePercent("101"))
        assertNull(AmountInput.parsePercent("1.23456"))
    }

    @Test
    fun mobiles() {
        assertEquals("9876543210", MobileNumber.normalize("+91 98765-43210"))
        assertEquals("9876543210", MobileNumber.normalize("098765 43210"))
        assertEquals("6123456789", MobileNumber.normalize("6123456789"))
        assertNull(MobileNumber.normalize("5123456789")) // Indian mobiles start 6-9
        assertNull(MobileNumber.normalize("98765"))
        assertEquals("https://wa.me/919876543210", MobileNumber.whatsAppUri("9876543210"))
    }
}
