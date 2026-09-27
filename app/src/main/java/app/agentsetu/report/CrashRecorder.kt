package app.agentsetu.report

import android.content.Context
import android.os.Build
import app.agentsetu.BuildConfig
import app.agentsetu.core.format.IndianFormat
import app.agentsetu.core.report.ErrorReport
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Keeps the last crash on the phone so the user can choose to send it (Settings -> Send error
 * report). Nothing is sent automatically; there is no crash-reporting SDK.
 */
object CrashRecorder {
    private const val FILE_NAME = "last_error.txt"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { file(appContext).writeText(buildReport(error)) }
            previous?.uncaughtException(thread, error)
        }
    }

    fun read(context: Context): String? = file(context).takeIf { it.exists() }?.readText()

    fun clear(context: Context) {
        file(context).delete()
    }

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)

    private fun buildReport(error: Throwable): String {
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        val now = LocalDateTime.now()
        return ErrorReport.build(
            appVersion = BuildConfig.VERSION_NAME,
            androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            device = "${Build.MANUFACTURER} ${Build.MODEL}",
            time = IndianFormat.date(now.toLocalDate()) + " " + now.format(DateTimeFormatter.ofPattern("HH:mm")),
            stackTrace = trace,
        )
    }
}
