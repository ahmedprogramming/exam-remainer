package com.studentreminder.app.reminders

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
import com.studentreminder.app.MainActivity
import com.studentreminder.app.R
import com.studentreminder.app.data.local.AssignmentWithDetails
import com.studentreminder.app.data.local.ExamWithDetails
import com.studentreminder.app.util.Formatters
import com.studentreminder.app.util.TimeUtils

object NotificationHelper {
    const val CHANNEL_EXAMS = "exam_reminders"
    const val CHANNEL_ASSIGNMENTS = "assignment_reminders"
    const val CHANNEL_GENERAL = "general"

    const val EXTRA_OPEN_ROUTE = "open_route"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_EXAMS, "Exam reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Reminders before your exams"
                    enableVibration(true)
                },
                NotificationChannel(CHANNEL_ASSIGNMENTS, "Assignment reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Reminders before assignment deadlines"
                    enableVibration(true)
                },
                NotificationChannel(CHANNEL_GENERAL, "General", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Test notifications and app messages"
                },
            )
        )
    }

    fun canPostNotifications(context: Context): Boolean {
        val permissionOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permissionOk && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun examNotificationId(examId: Long) = (examId * 2).toInt()
    fun assignmentNotificationId(assignmentId: Long) = (assignmentId * 2 + 1).toInt()

    fun showExamReminder(context: Context, exam: ExamWithDetails, use24h: Boolean) {
        val f = Formatters(use24h)
        val e = exam.exam
        val remaining = TimeUtils.remainingLong(e.dateTime)
        val title = "📚 Exam: ${exam.subjectName}"
        val text = "${f.dateTime(e.dateTime)} — starts $remaining"
        val big = buildString {
            append(text)
            if (e.location.isNotBlank()) append("\n📍 ${e.location}")
            val chapters = exam.sortedChapters
            if (chapters.isNotEmpty()) {
                append("\nChapters (${exam.chaptersDone}/${chapters.size} studied): ")
                append(chapters.joinToString(", ") { it.title })
            }
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_EXAMS)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(big))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setWhen(e.dateTime)
            .setShowWhen(true)
            .setContentIntent(openAppIntent(context, "exam/${e.id}", examNotificationId(e.id)))
        post(context, examNotificationId(e.id), builder)
    }

    fun showAssignmentReminder(context: Context, a: AssignmentWithDetails, reminderId: Long, use24h: Boolean) {
        val f = Formatters(use24h)
        val asg = a.assignment
        val due = asg.dueDateTime
        val overdue = due <= System.currentTimeMillis()
        val title = if (overdue) "⏰ Due now: ${asg.title}" else "📝 Assignment due: ${asg.title}"
        val text = "${a.subjectName} · ${f.dateTime(due)} — " + if (overdue) "due now" else "due ${TimeUtils.remainingLong(due)}"
        val big = buildString {
            append(text)
            if (asg.description.isNotBlank()) append("\n${asg.description}")
            append("\nPriority: ${priorityLabel(asg.priority)}")
        }
        val notifId = assignmentNotificationId(asg.id)
        val builder = NotificationCompat.Builder(context, CHANNEL_ASSIGNMENTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(big))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setWhen(due)
            .setShowWhen(true)
            .setContentIntent(openAppIntent(context, "assignment_edit?id=${asg.id}", notifId))
            .addAction(
                0, "Mark as done",
                actionIntent(context, NotificationActionReceiver.ACTION_MARK_DONE, asg.id, reminderId, notifId)
            )
            .addAction(
                0, "Snooze 1 hour",
                actionIntent(context, NotificationActionReceiver.ACTION_SNOOZE, asg.id, reminderId, notifId)
            )
        post(context, notifId, builder)
    }

    fun showTest(context: Context) {
        val builder = NotificationCompat.Builder(context, CHANNEL_GENERAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle("✅ Notifications are working")
            .setContentText("Student Reminder will alert you before exams and deadlines.")
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context, "home", 999_999))
        post(context, 999_999, builder)
    }

    fun cancel(context: Context, id: Int) = NotificationManagerCompat.from(context).cancel(id)

    private fun post(context: Context, id: Int, builder: NotificationCompat.Builder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        NotificationManagerCompat.from(context).notify(id, builder.build())
    }

    private fun openAppIntent(context: Context, route: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_ROUTE, route)
        }
        return PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun actionIntent(context: Context, action: String, assignmentId: Long, reminderId: Long, notifId: Int): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(NotificationActionReceiver.EXTRA_ASSIGNMENT_ID, assignmentId)
            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notifId)
        }
        val code = notifId * 4 + if (action == NotificationActionReceiver.ACTION_MARK_DONE) 1 else 2
        return PendingIntent.getBroadcast(
            context, code, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun priorityLabel(p: Int) = when (p) {
        0 -> "Low"
        2 -> "High"
        else -> "Medium"
    }
}
