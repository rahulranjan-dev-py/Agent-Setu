package app.agentsetu.core.security

import java.security.SecureRandom

/**
 * 8-digit code shown once when a PIN is set; only its PBKDF2 hash is stored (see PinHasher).
 * Entering it lets the user set a new PIN. Attempts share the PIN's lock-out counter.
 */
object RecoveryCode {
    const val LENGTH = 8
    private val DIGITS = Regex("^[0-9]{$LENGTH}$")

    fun generate(random: SecureRandom = SecureRandom()): String =
        buildString { repeat(LENGTH) { append(random.nextInt(10)) } }

    /** Accepts what the user typed with spaces or dashes: "1234 5678". */
    fun normalize(input: String): String = input.filter(Char::isDigit)

    fun isValid(code: String): Boolean = DIGITS.matches(code)

    /** "1234 5678": easier to write down. */
    fun format(code: String): String = code.chunked(4).joinToString(" ")
}
