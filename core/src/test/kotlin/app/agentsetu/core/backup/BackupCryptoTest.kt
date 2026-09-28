package app.agentsetu.core.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test

class BackupCryptoTest {
    private val payload = """{"customers":[{"name":"राम कुमार","mobile":"9876543210"}]}""".toByteArray()

    private fun expectFailure(reason: BackupException.Reason, block: () -> Unit) {
        try {
            block()
            fail("expected $reason")
        } catch (e: BackupException) {
            assertEquals(reason, e.reason)
        }
    }

    @Test
    fun roundTrip() {
        val file = BackupCrypto.encrypt(payload, "correct horse")
        assertArrayEquals(payload, BackupCrypto.decrypt(file, "correct horse"))
        assertFalse("plain text must not be visible", String(file, Charsets.ISO_8859_1).contains("9876543210"))
    }

    @Test
    fun wrongPasswordIsRejected() {
        val file = BackupCrypto.encrypt(payload, "correct horse")
        expectFailure(BackupException.Reason.WRONG_PASSWORD_OR_DAMAGED) { BackupCrypto.decrypt(file, "wrong horse!") }
    }

    @Test
    fun tamperingIsDetected() {
        val file = BackupCrypto.encrypt(payload, "correct horse")
        val body = file.copyOf().also { it[it.size - 5] = (it[it.size - 5] + 1).toByte() }
        expectFailure(BackupException.Reason.WRONG_PASSWORD_OR_DAMAGED) { BackupCrypto.decrypt(body, "correct horse") }
        val header = file.copyOf().also { it[20] = (it[20] + 1).toByte() } // inside the salt
        expectFailure(BackupException.Reason.WRONG_PASSWORD_OR_DAMAGED) { BackupCrypto.decrypt(header, "correct horse") }
    }

    @Test
    fun otherFilesAreNotBackups() {
        expectFailure(BackupException.Reason.NOT_A_BACKUP) { BackupCrypto.decrypt("hello world, not a backup at all".toByteArray(), "x") }
        val file = BackupCrypto.encrypt(payload, "correct horse").also { it[6] = 9 }
        expectFailure(BackupException.Reason.NEWER_FORMAT) { BackupCrypto.decrypt(file, "correct horse") }
    }

    @Test(expected = IllegalArgumentException::class)
    fun shortPasswordRefused() {
        BackupCrypto.encrypt(payload, "short")
    }

    @Test
    fun hostileIterationCountRefused() {
        val file = BackupCrypto.encrypt("x".repeat(100).toByteArray(), "correct horse")
        // Bytes 7..10 hold the iteration count; a file demanding 50 million rounds is not ours.
        val hostile = file.copyOf()
        java.nio.ByteBuffer.wrap(hostile, 7, 4).putInt(50_000_000)
        expectFailure(BackupException.Reason.NOT_A_BACKUP) { BackupCrypto.decrypt(hostile, "correct horse") }
    }
}
