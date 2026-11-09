package com.studentreminder.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.studentreminder.app.StudentReminderApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Runs [block] off the main thread while keeping the broadcast alive. */
private fun BroadcastReceiver.runAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try {
            block()
        } catch (t: Throwable) {
            Log.e("StudentReminder", "Receiver failed", t)
        } finally {
            pending.finish()
        }
    }
}

/** Fired by AlarmManager when a reminder is due. Loads fresh data and shows the notification. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(ReminderScheduler.EXTRA_REMINDER_ID, -1L)
        if (reminderId < 0) return
        val container = (context.applicationContext as StudentReminderApp).container
        runAsync {
            val repo = container.repository
            val settings = container.settingsRepository.current()
            val reminder = repo.getReminder(reminderId) ?: return@runAsync
            repo.markReminderFired(reminderId)
            if (!settings.notificationsEnabled) return@runAsync

            reminder.examId?.let { examId ->
                val exam = repo.getExam(examId) ?: return@runAsync
                NotificationHelper.showExamReminder(context, exam, settings.use24h)
            }
            reminder.assignmentId?.let { assignmentId ->
                val a = repo.getAssignment(assignmentId) ?: return@runAsync
                if (a.assignment.isCompleted) return@runAsync
                NotificationHelper.showAssignmentReminder(context, a, reminderId, settings.use24h)
            }
        }
    }
}

/** Handles the "Mark as done" and "Snooze" buttons on assignment notifications. */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val assignmentId = intent.getLongExtra(EXTRA_ASSIGNMENT_ID, -1L)
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val container = (context.applicationContext as StudentReminderApp).container
        NotificationHelper.cancel(context, notifId)
        when (intent.action) {
            ACTION_MARK_DONE -> if (assignmentId >= 0) runAsync {
                container.repository.setAssignmentCompleted(assignmentId, true)
            }
            ACTION_SNOOZE -> if (reminderId >= 0) {
                container.scheduler.schedule(reminderId, System.currentTimeMillis() + 60 * 60_000L)
            }
        }
    }

    companion object {
        const val ACTION_MARK_DONE = "com.studentreminder.app.MARK_DONE"
        const val ACTION_SNOOZE = "com.studentreminder.app.SNOOZE"
        const val EXTRA_ASSIGNMENT_ID = "assignment_id"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}

/**
 * Alarms are wiped when the phone restarts, the app is updated, or the clock/time zone
 * changes. This receiver re-registers every reminder via WorkManager.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED" ->
                RescheduleWorker.runNow(context)
        }
    }
}
