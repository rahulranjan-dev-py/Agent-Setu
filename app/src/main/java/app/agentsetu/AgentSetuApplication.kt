package app.agentsetu

import android.app.Application
import app.agentsetu.reminders.ReminderWorker
import app.agentsetu.report.CrashRecorder
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AgentSetuApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashRecorder.install(this)
        ReminderWorker.schedule(this)
    }
}
