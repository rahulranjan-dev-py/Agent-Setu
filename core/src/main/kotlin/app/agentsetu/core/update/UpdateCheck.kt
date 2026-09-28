package app.agentsetu.core.update

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Contents of release/version.json, published with every release. */
@Serializable
data class VersionInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    /** Must be an https link; the app only ever opens it in the browser. */
    val downloadUrl: String,
    val sha256: String = "",
    /** Versions below this have a serious problem and users are told to update. */
    val minSupportedVersionCode: Int = 0,
    val releasedOn: String = "",
    val changelogEn: String = "",
    val changelogHi: String = "",
    /** Direct https link to the APK (a GitHub release asset). Empty: the app can only open [downloadUrl]. */
    val apkUrl: String = "",
)

sealed interface UpdateStatus {
    data object UpToDate : UpdateStatus
    data class Available(val info: VersionInfo) : UpdateStatus
    data class Required(val info: VersionInfo) : UpdateStatus
}

object UpdateCheck {
    private val json = Json { ignoreUnknownKeys = true }

    /** Returns null for anything that is not a valid version file (the app then says "could not check"). */
    fun parse(text: String): VersionInfo? = runCatching { json.decodeFromString(VersionInfo.serializer(), text) }
        .getOrNull()
        ?.takeIf { it.latestVersionCode > 0 && it.downloadUrl.startsWith("https://") }

    private val SHA256_HEX = Regex("^[0-9a-fA-F]{64}$")

    /**
     * The app downloads and installs the APK itself only when it can verify it: an https link to an
     * .apk file and a published SHA-256 to compare against. Otherwise it opens the download page.
     */
    fun canInstallInApp(info: VersionInfo): Boolean =
        info.apkUrl.startsWith("https://") && info.apkUrl.endsWith(".apk") && SHA256_HEX.matches(info.sha256)

    fun evaluate(currentVersionCode: Int, info: VersionInfo): UpdateStatus = when {
        currentVersionCode < info.minSupportedVersionCode -> UpdateStatus.Required(info)
        currentVersionCode < info.latestVersionCode -> UpdateStatus.Available(info)
        else -> UpdateStatus.UpToDate
    }
}
