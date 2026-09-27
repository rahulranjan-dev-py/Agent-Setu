package app.agentsetu.core.input

/** Indian mobile numbers: stored as 10 digits, dialled as-is, opened in WhatsApp with 91 in front. */
object MobileNumber {

    /** "+91 98765-43210", "098765 43210", "9876543210" -> "9876543210"; null if not a valid mobile. */
    fun normalize(text: String): String? {
        var digits = text.filter { it.isDigit() }
        if (digits.length == 12 && digits.startsWith("91")) digits = digits.substring(2)
        if (digits.length == 11 && digits.startsWith("0")) digits = digits.substring(1)
        return digits.takeIf { it.length == 10 && it[0] in '6'..'9' }
    }

    fun dialUri(mobile: String): String = "tel:$mobile"

    /** Opens a chat in WhatsApp (or the browser) without needing any permission. */
    fun whatsAppUri(mobile: String): String = "https://wa.me/91$mobile"
}
