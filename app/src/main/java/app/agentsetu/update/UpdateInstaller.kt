package app.agentsetu.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import app.agentsetu.BuildConfig
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
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

sealed interface DownloadState {
    data object Idle : DownloadState

    /** [fraction] is null until the server says how big the file is. */
    data class Downloading(val fraction: Float?) : DownloadState
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

    fun start(info: VersionInfo) {
        if (!UpdateCheck.canInstallInApp(info)) return
        if (_state.value is DownloadState.Downloading || _state.value is DownloadState.Verifying) return
        job = scope.launch {
            _state.value = DownloadState.Downloading(null)
            val file = File(directory(), "AgentSetu-v${info.latestVersionName}.apk")
            try {
                download(info.apkUrl, file)
                _state.value = DownloadState.Verifying
                if (!sha256(file).equals(info.sha256, ignoreCase = true)) {
                    file.delete()
                    _state.value = DownloadState.Failed(DownloadState.Reason.CHECKSUM)
                    return@launch
                }
                _state.value = DownloadState.Ready(file)
            } catch (e: Exception) {
                file.delete()
                _state.value = if (e is kotlinx.coroutines.CancellationException) DownloadState.Idle else DownloadState.Failed(DownloadState.Reason.NETWORK)
            }
        }
    }

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

    private fun directory(): File = File(context.cacheDir, "updates").apply {
        mkdirs()
        // Only one update file is kept at a time.
        listFiles()?.forEach { it.delete() }
    }

    private suspend fun download(url: String, target: File) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "AgentSetu")
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) throw IllegalStateException("HTTP ${connection.responseCode}")
            val total = connection.contentLengthLong.takeIf { it > 0 }
            if (total != null && total > MAX_BYTES) throw IllegalStateException("File too large")
            var read = 0L
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        coroutineContext.ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        read += n
                        if (read > MAX_BYTES) throw IllegalStateException("File too large")
                        _state.value = DownloadState.Downloading(total?.let { read.toFloat() / it })
                    }
                }
            }
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
        const val TIMEOUT_MS = 20_000
        const val MAX_BYTES = 60L * 1024 * 1024
    }
}
