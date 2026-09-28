package app.agentsetu.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import app.agentsetu.BuildConfig
import app.agentsetu.core.update.DownloadPolicy
import app.agentsetu.core.update.UpdateCheck
import app.agentsetu.core.update.VersionInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

sealed interface DownloadState {
    data object Idle : DownloadState

    /**
     * [fraction] is null until the server says how big the file is; [retry] > 0 while reconnecting
     * automatically after a dropped connection (attempt number, up to DownloadPolicy.MAX_RETRIES).
     */
    data class Downloading(val fraction: Float?, val retry: Int = 0) : DownloadState
    data object Verifying : DownloadState
    data class Ready(val file: File) : DownloadState
    data class Failed(val reason: Reason) : DownloadState

    enum class Reason { NETWORK, CHECKSUM }
}

/**
 * Downloads a release APK into the app's private cache, checks its SHA-256 against the value
 * published in version.json, and hands the file to Android's installer. Android then verifies the
 * signature itself: only an APK signed with the same release key can update the installed app.
 *
 * Network use is limited to the apkUrl from version.json (https, .apk); nothing is sent.
 */
@Singleton
class UpdateInstaller @Inject constructor(@ApplicationContext private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state: StateFlow<DownloadState> = _state.asStateFlow()

    /**
     * Starts, or after a failure resumes, the download of [info]. A partial file from an earlier
     * attempt at the same version is continued, not restarted; the checksum is always computed on
     * the complete file.
     */
    fun start(info: VersionInfo) {
        if (!UpdateCheck.canInstallInApp(info)) return
        if (_state.value is DownloadState.Downloading || _state.value is DownloadState.Verifying) return
        job = scope.launch {
            _state.value = DownloadState.Downloading(null)
            val name = "AgentSetu-v${info.latestVersionName}.apk"
            val file = File(directory(keep = name), name)
            try {
                downloadWithRetries(info.apkUrl, file)
                _state.value = DownloadState.Verifying
                if (!sha256(file).equals(info.sha256, ignoreCase = true)) {
                    file.delete()
                    _state.value = DownloadState.Failed(DownloadState.Reason.CHECKSUM)
                    return@launch
                }
                _state.value = DownloadState.Ready(file)
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Cancelled by the user: the partial file is kept so a later Download resumes it.
                _state.value = DownloadState.Idle
            } catch (e: Exception) {
                _state.value = DownloadState.Failed(DownloadState.Reason.NETWORK)
            }
        }
    }

    private suspend fun downloadWithRetries(url: String, target: File) {
        var retry = 0
        while (true) {
            try {
                download(url, target)
                return
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                if (retry >= DownloadPolicy.MAX_RETRIES) throw e
                retry++
                _state.value = DownloadState.Downloading(fractionOf(target.length(), lastTotal), retry)
                delay(DownloadPolicy.retryDelayMs(retry))
            }
        }
    }

    /** Size of the whole file once any response told us; survives retries for the progress bar. */
    @Volatile private var lastTotal: Long? = null

    private fun fractionOf(read: Long, total: Long?): Float? = total?.let { (read.toFloat() / it).coerceIn(0f, 1f) }

    fun cancel() {
        job?.cancel()
        job = null
        _state.value = DownloadState.Idle
    }

    fun dismiss() {
        if (_state.value !is DownloadState.Downloading && _state.value !is DownloadState.Verifying) _state.value = DownloadState.Idle
    }

    /** Opens Android's package installer for the verified file. */
    fun install(file: File) {
        val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    /** The cache folder, keeping only the partial or finished file for the version being fetched. */
    private fun directory(keep: String): File = File(context.cacheDir, "updates").apply {
        mkdirs()
        listFiles()?.filter { it.name != keep }?.forEach { it.delete() }
    }

    /** One attempt: resumes [target] if it already has bytes and the server honours ranges. */
    private suspend fun download(url: String, target: File) {
        val already = target.length()
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = DownloadPolicy.STALL_TIMEOUT_MS
            readTimeout = DownloadPolicy.STALL_TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "AgentSetu")
            DownloadPolicy.rangeHeader(already)?.let { setRequestProperty("Range", it) }
        }
        try {
            val code = connection.responseCode
            val resuming: Boolean
            val total: Long?
            when (code) {
                HttpURLConnection.HTTP_PARTIAL -> {
                    resuming = true
                    // "bytes 1000-6893661/6893662"
                    total = connection.getHeaderField("Content-Range")?.substringAfter('/')?.trim()?.toLongOrNull()
                        ?: connection.contentLengthLong.takeIf { it > 0 }?.plus(already)
                }
                HttpURLConnection.HTTP_OK -> {
                    resuming = false
                    total = connection.contentLengthLong.takeIf { it > 0 }
                }
                416 -> return // Requested range not satisfiable: the file is already complete.
                else -> throw IllegalStateException("HTTP $code")
            }
            if (total != null && total > MAX_BYTES) throw IllegalStateException("File too large")
            lastTotal = total
            var read = if (resuming) already else 0L
            connection.inputStream.use { input ->
                java.io.FileOutputStream(target, resuming).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        coroutineContext.ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        read += n
                        if (read > MAX_BYTES) throw IllegalStateException("File too large")
                        _state.value = DownloadState.Downloading(fractionOf(read, total))
                    }
                }
            }
            if (total != null && read < total) throw IllegalStateException("Connection closed early")
        } finally {
            connection.disconnect()
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val MAX_BYTES = 60L * 1024 * 1024
    }
}
