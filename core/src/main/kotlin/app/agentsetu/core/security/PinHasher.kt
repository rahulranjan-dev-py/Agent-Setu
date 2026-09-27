package app.agentsetu.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * The app PIN is never stored, only a salted PBKDF2 hash: "pbkdf2$<iterations>$<salt>$<hash>".
 * The PIN guards the screen; the database itself is encrypted with a separate Keystore-held key.
 */
object PinHasher {
    private const val ITERATIONS = 120_000
    private const val SALT_BYTES = 16
    private const val HASH_BITS = 256
    private val PIN = Regex("^[0-9]{4,6}$")

    fun isValidPin(pin: String): Boolean = PIN.matches(pin)

    fun hash(pin: String, random: SecureRandom = SecureRandom()): String {
        require(isValidPin(pin)) { "PIN must be 4 to 6 digits" }
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        return encode(ITERATIONS, salt, derive(pin, salt, ITERATIONS))
    }

    fun verify(pin: String, stored: String): Boolean {
        val parts = stored.split('$')
        if (parts.size != 4 || parts[0] != "pbkdf2") return false
        val iterations = parts[1].toIntOrNull() ?: return false
        val decoder = Base64.getDecoder()
        val salt = runCatching { decoder.decode(parts[2]) }.getOrNull() ?: return false
        val expected = runCatching { decoder.decode(parts[3]) }.getOrNull() ?: return false
        return MessageDigest.isEqual(expected, derive(pin, salt, iterations))
    }

    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, HASH_BITS)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(iterations: Int, salt: ByteArray, hash: ByteArray): String {
        val e = Base64.getEncoder()
        return "pbkdf2$$iterations$${e.encodeToString(salt)}$${e.encodeToString(hash)}"
    }
}
