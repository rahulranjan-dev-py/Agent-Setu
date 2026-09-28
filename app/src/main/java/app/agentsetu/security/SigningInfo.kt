package app.agentsetu.security

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.security.MessageDigest

/**
 * SHA-256 fingerprint of the certificate this copy of the app was signed with. Shown on the About
 * screen so users can compare it with the fingerprint pinned in the Agent Setu group: a copy signed
 * by anyone else shows a different value. Also used to check a downloaded update before installing.
 */
object SigningInfo {
    fun sha256(context: Context): String? = runCatching {
        val signature = signatures(context).firstOrNull() ?: return null
        fingerprint(signature)
    }.getOrNull()

    fun fingerprint(signature: Signature): String =
        MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
            .joinToString(":") { "%02X".format(it) }

    fun signatures(context: Context): List<Signature> {
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        }
        return signaturesOf(info)
    }

    fun signaturesOf(info: PackageInfo): List<Signature> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners?.toList()?.let { if (it.isNotEmpty()) return it }
        }
        @Suppress("DEPRECATION")
        return info.signatures?.toList().orEmpty()
    }
}
