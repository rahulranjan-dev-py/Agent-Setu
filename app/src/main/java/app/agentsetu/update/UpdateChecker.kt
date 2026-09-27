package app.agentsetu.update

import android.content.Context
import app.agentsetu.BuildConfig
import app.agentsetu.core.update.UpdateCheck
import app.agentsetu.core.update.UpdateStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Reads the public release/version.json at most once a day (or when the user asks). This is the
 * app's only network request: a plain GET that sends no user or device data (the User-Agent is just
 * "AgentSetu"). The app never downloads or installs anything itself; it opens the link in the browser.
 */
@Singleton
class UpdateChecker @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _status = MutableStateFlow(cached())
    val status: StateFlow<UpdateStatus?> = _status.asStateFlow()

    /** Returns false if the check could not be done (offline, file missing, bad file). */
    suspend fun check(force: Boolean): Boolean {
        val now = System.currentTimeMillis()
        if (!force && now - prefs.getLong(KEY_LAST_CHECK, 0) < DAY_MS) return true
        val text = withContext(Dispatchers.IO) { fetch() } ?: return false
        val info = UpdateCheck.parse(text) ?: return false
        prefs.edit().putLong(KEY_LAST_CHECK, now).putString(KEY_LAST_FILE, text).apply()
        _status.value = UpdateCheck.evaluate(BuildConfig.VERSION_CODE, info)
        return true
    }

    private fun cached(): UpdateStatus? =
        prefs.getString(KEY_LAST_FILE, null)
            ?.let(UpdateCheck::parse)
            ?.let { UpdateCheck.evaluate(BuildConfig.VERSION_CODE, it) }

    private fun fetch(): String? {
        if (!BuildConfig.UPDATE_URL.startsWith("https://")) return null
        return try {
            val connection = (URL(BuildConfig.UPDATE_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                useCaches = false
                setRequestProperty("User-Agent", "AgentSetu")
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
                connection.inputStream.use { stream ->
                    val bytes = stream.readNBytesCompat(MAX_BYTES)
                    String(bytes, Charsets.UTF_8)
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun java.io.InputStream.readNBytesCompat(max: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (out.size() < max) {
            val n = read(buffer, 0, minOf(buffer.size, max - out.size()))
            if (n < 0) break
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }

    private companion object {
        const val PREFS_NAME = "updates"
        const val KEY_LAST_CHECK = "last_check"
        const val KEY_LAST_FILE = "last_file"
        const val DAY_MS = 24 * 60 * 60 * 1000L
        const val TIMEOUT_MS = 10_000
        const val MAX_BYTES = 64 * 1024
    }
}
