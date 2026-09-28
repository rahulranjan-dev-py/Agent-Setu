package app.agentsetu

import android.app.Application
import app.agentsetu.data.settings.AppSettings
import app.agentsetu.reminders.ReminderWorker
import app.agentsetu.report.CrashRecorder
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AgentSetuApplication : Application() {
    @Inject lateinit var settings: AppSettings

    override fun onCreate() {
        super.onCreate()
        CrashRecorder.install(this)
        settings.applyNightMode()
        ReminderWorker.schedule(this)
    }
}
