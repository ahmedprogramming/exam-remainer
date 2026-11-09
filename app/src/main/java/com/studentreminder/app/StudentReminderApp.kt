package com.studentreminder.app

import android.app.Application
import android.content.Context
import com.studentreminder.app.data.BackupManager
import com.studentreminder.app.data.SettingsRepository
import com.studentreminder.app.data.StudentRepository
import com.studentreminder.app.data.local.AppDatabase
import com.studentreminder.app.reminders.NotificationHelper
import com.studentreminder.app.reminders.ReminderScheduler
import com.studentreminder.app.reminders.RescheduleWorker

/** Simple manual dependency container (no DI framework needed for an app this size). */
class AppContainer(context: Context) {
    val database: AppDatabase = AppDatabase.build(context)
    val settingsRepository = SettingsRepository(context)
    val scheduler = ReminderScheduler(context)
    val repository = StudentRepository(database, settingsRepository, scheduler)
    val backupManager = BackupManager(context, repository, settingsRepository)
}

class StudentReminderApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannels(this)
        // Make sure alarms exist even if the system dropped them (e.g. after a force-stop).
        RescheduleWorker.runNow(this)
        RescheduleWorker.schedulePeriodic(this)
    }
}
