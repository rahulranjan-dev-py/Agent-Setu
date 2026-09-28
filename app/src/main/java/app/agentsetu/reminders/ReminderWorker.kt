package app.agentsetu.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.agentsetu.data.repo.ReminderRepository
import app.agentsetu.data.settings.AppSettings
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Once a day (07:00 by default; changeable in Settings): creates the reminders that are now due and posts one summary
 * notification. Everything happens on the phone; nothing is sent anywhere.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun reminderRepository(): ReminderRepository
        fun appSettings(): AppSettings
    }

    override suspend fun doWork(): Result {
        val repo = EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java).reminderRepository()
        val today = LocalDate.now()
        repo.regenerate(today)
        ReminderNotifier.show(applicationContext, repo.alertsToday(today))
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "daily-reminders"

        /** Safe to call on every app start: an existing schedule is kept. */
        fun schedule(context: Context) = enqueue(context, ExistingPeriodicWorkPolicy.KEEP)

        /**
         * After the user changes the reminder time. UPDATE would keep the original period start and
         * ignore the new initial delay once the work has run, so the schedule is replaced outright.
         */
        fun reschedule(context: Context) = enqueue(context, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE)

        private fun enqueue(context: Context, policy: ExistingPeriodicWorkPolicy) {
            val settings = EntryPointAccessors.fromApplication(context.applicationContext, Dependencies::class.java).appSettings()
            val minuteOfDay = settings.reminderMinuteOfDay.value
            val runAt = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayUntilNextRun(LocalDateTime.now(), runAt).toMinutes(), TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, policy, request)
        }

        private fun delayUntilNextRun(now: LocalDateTime, runAt: LocalTime): Duration {
            var next = now.toLocalDate().atTime(runAt)
            if (!next.isAfter(now)) next = next.plusDays(1)
            return Duration.between(now, next)
        }
    }
}
