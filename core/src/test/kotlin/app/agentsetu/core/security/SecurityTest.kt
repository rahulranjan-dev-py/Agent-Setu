package app.agentsetu.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityTest {

    @Test
    fun pinHashVerifies() {
        val stored = PinHasher.hash("4821")
        assertTrue(stored.startsWith("pbkdf2$"))
        assertFalse("PIN must not appear in the stored hash", stored.contains("4821"))
        assertTrue(PinHasher.verify("4821", stored))
        assertFalse(PinHasher.verify("4822", stored))
        assertFalse(PinHasher.verify("4821", "garbage"))
        assertNotEquals("salted: same PIN, different hash", stored, PinHasher.hash("4821"))
    }

    @Test
    fun pinFormat() {
        assertTrue(PinHasher.isValidPin("0000"))
        assertTrue(PinHasher.isValidPin("123456"))
        assertFalse(PinHasher.isValidPin("123"))
        assertFalse(PinHasher.isValidPin("1234567"))
        assertFalse(PinHasher.isValidPin("12a4"))
    }

    @Test
    fun lockoutGrows() {
        assertEquals(0, LockPolicy.lockoutMs(4))
        assertEquals(30_000, LockPolicy.lockoutMs(5))
        assertEquals(60_000, LockPolicy.lockoutMs(6))
        assertEquals(15 * 60_000L, LockPolicy.lockoutMs(50))
    }

    @Test
    fun locksAfterTwoMinutesInBackground() {
        assertFalse(LockPolicy.shouldLockOnReturn(null, 10_000_000))
        assertFalse(LockPolicy.shouldLockOnReturn(1_000_000, 1_000_000 + 119_000))
        assertTrue(LockPolicy.shouldLockOnReturn(1_000_000, 1_000_000 + 120_000))
    }
}
