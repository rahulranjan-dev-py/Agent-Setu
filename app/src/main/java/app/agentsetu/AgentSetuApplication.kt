package app.agentsetu

import android.app.Application
import app.agentsetu.reminders.ReminderWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AgentSetuApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderWorker.schedule(this)
    }
}
