package com.studentreminder.app.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.studentreminder.app.StudentReminderApp
import java.util.concurrent.TimeUnit

/** Re-registers every pending reminder alarm from the Room database. */
class RescheduleWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as StudentReminderApp
        return try {
            app.container.repository.rescheduleAll()
            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }

    companion object {
        private const val ONE_TIME = "reschedule_reminders_now"
        private const val PERIODIC = "reschedule_reminders_periodic"

        fun runNow(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<RescheduleWorker>().build(),
            )
        }

        /** Safety net: every 12 hours make sure all alarms are still registered. */
        fun schedulePeriodic(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<RescheduleWorker>(12, TimeUnit.HOURS).build(),
            )
        }
    }
}
