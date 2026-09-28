package app.agentsetu.core.model

/**
 * Optional full policy or account number (decided 28-09-2026). It is shown in full only on the
 * policy's own screen; everywhere else the app shows [RefLast4]. Aadhaar, PAN and bank account
 * numbers still have no place to go: this field is for the policy/account the user services.
 */
object RefNumber {
    const val MAX_LENGTH = 40
    private val ALLOWED = Regex("^[A-Za-z0-9 /-]{1,$MAX_LENGTH}$")

    fun isValid(value: String?): Boolean = value == null || ALLOWED.matches(value)

    fun require(value: String?) {
        require(isValid(value)) { "Policy/account number: up to $MAX_LENGTH letters, digits, / or -" }
    }

    /** What the rest of the app may show: the last 4 digits, or nothing if there are fewer. */
    fun last4(value: String?): String? {
        val digits = value.orEmpty().filter(Char::isDigit)
        return if (digits.length >= 4) digits.takeLast(4) else null
    }

    fun masked(value: String?): String? = last4(value)?.let { "••••$it" }
}
