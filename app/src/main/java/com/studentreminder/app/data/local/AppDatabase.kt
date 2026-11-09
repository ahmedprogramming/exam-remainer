package com.studentreminder.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SubjectEntity::class,
        ExamEntity::class,
        ChapterEntity::class,
        AssignmentEntity::class,
        ReminderEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun examDao(): ExamDao
    abstract fun assignmentDao(): AssignmentDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        const val NAME = "student_reminder.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                .build()
    }
}
