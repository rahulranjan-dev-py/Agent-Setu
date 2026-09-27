package app.agentsetu.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** Opens a dialler, WhatsApp link or settings page. Needs no permission; does nothing if no app can handle it. */
fun Context.openUri(uri: String, action: String = Intent.ACTION_VIEW) {
    try {
        startActivity(Intent(action, Uri.parse(uri)))
    } catch (_: ActivityNotFoundException) {
    }
}

fun Context.openAppSettings() {
    try {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
    } catch (_: ActivityNotFoundException) {
    }
}
