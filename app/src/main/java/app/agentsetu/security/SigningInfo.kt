package app.agentsetu.security

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.security.MessageDigest

/**
 * SHA-256 fingerprint of the certificate this copy of the app was signed with. Shown on the About
 * screen so users can compare it with the fingerprint posted in the official group: a copy signed
 * by anyone else shows a different value.
 */
object SigningInfo {
    fun sha256(context: Context): String? = runCatching {
        val signature = signatures(context).firstOrNull() ?: return null
        MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
            .joinToString(":") { "%02X".format(it) }
    }.getOrNull()

    private fun signatures(context: Context): List<Signature> {
        val pm = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            info.signingInfo?.apkContentsSigners?.toList().orEmpty()
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures?.toList().orEmpty()
        }
    }
}
