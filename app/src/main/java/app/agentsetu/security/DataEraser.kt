package app.agentsetu.security

import android.app.ActivityManager
import android.content.Context

object DataEraser {
    /**
     * "Delete all my data": Android wipes the database, settings, PIN, backup copies in the app's
     * own storage and scheduled reminders, then closes the app. Backup files the user saved
     * elsewhere are not touched.
     */
    fun eraseEverything(context: Context) {
        context.getSystemService(ActivityManager::class.java).clearApplicationUserData()
    }
}
