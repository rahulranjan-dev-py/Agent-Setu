package app.agentsetu.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.agentsetu.MainActivity
import app.agentsetu.R
import app.agentsetu.core.format.IndianFormat
import app.agentsetu.core.model.ReminderType
import app.agentsetu.data.db.ReminderRow

object ReminderNotifier {
    private const val CHANNEL_ID = "reminders"
    private const val NOTIFICATION_ID = 1001
    private const val MAX_LINES = 6

    fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun show(context: Context, alerts: List<ReminderRow>) {
        if (alerts.isEmpty() || !canNotify(context)) return
        ensureChannel(context)

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val title = context.getString(R.string.notif_title, alerts.size)
        val style = NotificationCompat.InboxStyle().setBigContentTitle(title)
        alerts.take(MAX_LINES).forEach { style.addLine(line(context, it)) }

        // Customer names stay off the lock screen: the public version only says there are reminders.
        val publicVersion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.notif_public))
            .build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(line(context, alerts.first()))
            .setStyle(style)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission withdrawn between the check and the call; nothing to do.
        }
    }

    private fun line(context: Context, row: ReminderRow): String {
        val type = context.getString(typeLabel(row.type))
        return "$type · ${row.customerName} · ${IndianFormat.date(row.dueDate)}"
    }

    fun typeLabel(type: ReminderType): Int = when (type) {
        ReminderType.PREMIUM_DUE -> R.string.type_PREMIUM_DUE
        ReminderType.INSTALMENT_DUE -> R.string.type_INSTALMENT_DUE
        ReminderType.MATURITY -> R.string.type_MATURITY
        ReminderType.FOLLOW_UP -> R.string.type_FOLLOW_UP
    }

    private fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.notif_channel_desc) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
