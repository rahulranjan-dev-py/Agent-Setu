package app.agentsetu.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import app.agentsetu.BuildConfig
import app.agentsetu.security.SigningInfo
import java.io.File

/**
 * Is the downloaded file really this app? The checksum in version.json proves the file arrived
 * intact; this proves it is Agent Setu (same package name), the version announced, and signed with
 * the same key as the running copy. Android enforces the signer too, but checking here means a
 * foreign APK is never even handed to the installer.
 */
object ApkCheck {
    fun isGenuine(context: Context, apk: File, expectedVersionCode: Int): Boolean = runCatching {
        val info = archiveInfo(context, apk) ?: return false
        if (info.packageName != BuildConfig.APPLICATION_ID) return false
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
        if (code != expectedVersionCode.toLong()) return false
        val own = SigningInfo.signatures(context).map(SigningInfo::fingerprint).toSet()
        val theirs = SigningInfo.signaturesOf(info).map(SigningInfo::fingerprint).toSet()
        own.isNotEmpty() && theirs == own
    }.getOrDefault(false)

    private fun archiveInfo(context: Context, apk: File): PackageInfo? {
        val pm = context.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getPackageArchiveInfo(apk.path, PackageManager.GET_SIGNING_CERTIFICATES)?.let { info ->
                if (SigningInfo.signaturesOf(info).isNotEmpty()) return info
            }
        }
        @Suppress("DEPRECATION")
        return pm.getPackageArchiveInfo(apk.path, PackageManager.GET_SIGNATURES)
    }
}
