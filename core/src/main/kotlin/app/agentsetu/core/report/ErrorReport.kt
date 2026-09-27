package app.agentsetu.core.report

/**
 * Text of a manual error report. The user sees it before sharing. Long digit runs (mobile or account
 * numbers that might appear in an error message) are masked as a precaution.
 */
object ErrorReport {
    private val LONG_DIGITS = Regex("[0-9]{5,}")

    fun build(appVersion: String, androidVersion: String, device: String, time: String, stackTrace: String): String =
        buildString {
            appendLine("Agent Setu error report")
            appendLine("App version: $appVersion")
            appendLine("Android: $androidVersion")
            appendLine("Device: $device")
            appendLine("Time: $time")
            appendLine()
            append(redact(stackTrace).take(MAX_TRACE_CHARS))
        }

    fun redact(text: String): String = LONG_DIGITS.replace(text) { "#".repeat(it.value.length) }

    private const val MAX_TRACE_CHARS = 12_000
}
