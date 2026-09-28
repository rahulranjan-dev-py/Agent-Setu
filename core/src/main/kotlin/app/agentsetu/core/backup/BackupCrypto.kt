package app.agentsetu.core.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupException(val reason: Reason) : Exception(reason.name) {
    enum class Reason { NOT_A_BACKUP, NEWER_FORMAT, WRONG_PASSWORD_OR_DAMAGED }
}

/**
 * Backup file format (version 1):
 *   "AGSETU" (6 bytes) | format version (1 byte) | iterations (4 bytes) | salt (16) | IV (12) | ciphertext
 * The payload is gzip-compressed, then encrypted with AES-256-GCM using a key derived from the
 * user's backup password with PBKDF2-HMAC-SHA256. GCM detects a wrong password and any tampering.
 * The header is authenticated too, so it cannot be altered without detection.
 */
object BackupCrypto {
    private val MAGIC = "AGSETU".toByteArray(Charsets.US_ASCII)
    private const val FORMAT_VERSION: Byte = 1
    /** 600,000 rounds (OWASP 2023 guidance for PBKDF2-HMAC-SHA256). Older files carry their own count. */
    private const val ITERATIONS = 600_000

    /** A file may claim any count in this range; a hostile file cannot make decryption take minutes. */
    private const val MIN_ACCEPTED_ITERATIONS = 10_000
    private const val MAX_ACCEPTED_ITERATIONS = 1_000_000

    /** Caps against decompression bombs and accidental huge files. */
    const val MAX_FILE_BYTES = 64L * 1024 * 1024
    const val MAX_PLAIN_BYTES = 256L * 1024 * 1024
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private const val HEADER_BYTES = 6 + 1 + 4 + SALT_BYTES + IV_BYTES
    const val MIN_PASSWORD_LENGTH = 8

    fun encrypt(plain: ByteArray, password: String, random: SecureRandom = SecureRandom()): ByteArray {
        require(password.length >= MIN_PASSWORD_LENGTH) { "Backup password too short" }
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val header = ByteBuffer.allocate(HEADER_BYTES)
            .put(MAGIC).put(FORMAT_VERSION).putInt(ITERATIONS).put(salt).put(iv)
            .array()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt, ITERATIONS), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(header)
        return header + cipher.doFinal(gzip(plain))
    }

    fun decrypt(file: ByteArray, password: String): ByteArray {
        if (file.size <= HEADER_BYTES || !file.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            throw BackupException(BackupException.Reason.NOT_A_BACKUP)
        }
        if (file.size > MAX_FILE_BYTES) throw BackupException(BackupException.Reason.NOT_A_BACKUP)
        val buffer = ByteBuffer.wrap(file, MAGIC.size, HEADER_BYTES - MAGIC.size)
        val version = buffer.get()
        if (version > FORMAT_VERSION) throw BackupException(BackupException.Reason.NEWER_FORMAT)
        if (version < 1) throw BackupException(BackupException.Reason.NOT_A_BACKUP)
        val iterations = buffer.getInt()
        if (iterations !in MIN_ACCEPTED_ITERATIONS..MAX_ACCEPTED_ITERATIONS) throw BackupException(BackupException.Reason.NOT_A_BACKUP)
        val salt = ByteArray(SALT_BYTES).also { buffer.get(it) }
        val iv = ByteArray(IV_BYTES).also { buffer.get(it) }
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(password, salt, iterations), GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(file, 0, HEADER_BYTES)
            gunzip(cipher.doFinal(file, HEADER_BYTES, file.size - HEADER_BYTES))
        } catch (e: GeneralSecurityException) {
            throw BackupException(BackupException.Reason.WRONG_PASSWORD_OR_DAMAGED)
        }
    }

    private fun key(password: String, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun gzip(data: ByteArray): ByteArray =
        ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(data) } }.toByteArray()

    /** Stops at MAX_PLAIN_BYTES so a crafted file cannot exhaust memory. */
    private fun gunzip(data: ByteArray): ByteArray = GZIPInputStream(ByteArrayInputStream(data)).use { input ->
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            out.write(buffer, 0, n)
            if (out.size() > MAX_PLAIN_BYTES) throw BackupException(BackupException.Reason.NOT_A_BACKUP)
        }
        out.toByteArray()
    }
}
