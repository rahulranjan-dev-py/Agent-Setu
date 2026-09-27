package app.agentsetu.core.model

/**
 * Guardrail: the app keeps at most the last 4 digits of a policy or account number, never the full
 * number. Anything that is not exactly four digits is rejected rather than trimmed, so a full number
 * pasted by mistake is never silently stored in part.
 */
object RefLast4 {
    private val FOUR_DIGITS = Regex("^[0-9]{4}$")

    fun isValid(value: String?): Boolean = value == null || FOUR_DIGITS.matches(value)

    fun require(value: String?) {
        require(isValid(value)) { "Only the last 4 digits of a policy/account number may be stored" }
    }
}
