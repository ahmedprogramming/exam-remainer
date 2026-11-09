package com.studentreminder.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log

/**
 * Thin wrapper around AlarmManager. Each reminder row gets its own alarm, identified by
 * the reminder id (used both as request code and as a unique data URI).
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun schedule(reminderId: Long, triggerAtMillis: Long) {
        val pi = pendingIntent(reminderId, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        try {
            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } else {
                // Falls back to an inexact alarm (may be delayed a few minutes by the system).
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm not permitted, using inexact alarm", e)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    fun cancel(reminderId: Long) {
        val pi = pendingIntent(reminderId, PendingIntent.FLAG_NO_CREATE) ?: return
        alarmManager.cancel(pi)
        pi.cancel()
    }

    private fun pendingIntent(reminderId: Long, flag: Int): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_FIRE_REMINDER
            data = Uri.parse("studentreminder://reminder/$reminderId")
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            flag or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val TAG = "ReminderScheduler"
        const val ACTION_FIRE_REMINDER = "com.studentreminder.app.FIRE_REMINDER"
        const val EXTRA_REMINDER_ID = "reminder_id"
    }
}
