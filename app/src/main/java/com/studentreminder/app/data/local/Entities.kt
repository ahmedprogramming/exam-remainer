package com.studentreminder.app.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "subjects",
    indices = [Index(value = ["name"], unique = true)]
)
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorIndex: Int = 0,
)

@Entity(
    tableName = "exams",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.RESTRICT,
        )
    ],
    indices = [Index("subjectId"), Index("dateTime")]
)
data class ExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    /** Exam date + time as epoch milliseconds (device time zone). */
    val dateTime: Long,
    val location: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = ExamEntity::class,
            parentColumns = ["id"],
            childColumns = ["examId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("examId")]
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examId: Long,
    val title: String,
    val position: Int,
    val isDone: Boolean = false,
)

@Entity(
    tableName = "assignments",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.RESTRICT,
        )
    ],
    indices = [Index("subjectId"), Index("dueDateTime")]
)
data class AssignmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subjectId: Long,
    val description: String = "",
    /** Due date + time as epoch milliseconds (device time zone). */
    val dueDateTime: Long,
    /** 0 = Low, 1 = Medium, 2 = High */
    val priority: Int = 1,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * One reminder for one exam OR one assignment.
 * [offsetMinutes] = minutes before the event. 0 = at event time,
 * [com.studentreminder.app.data.ReminderOption.SAME_DAY] = morning of the event day.
 */
@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = ExamEntity::class,
            parentColumns = ["id"],
            childColumns = ["examId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AssignmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["assignmentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("examId"), Index("assignmentId")]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examId: Long? = null,
    val assignmentId: Long? = null,
    val offsetMinutes: Long,
    /** When the notification was shown; null = not yet fired. */
    val firedAt: Long? = null,
)

data class ExamWithDetails(
    @Embedded val exam: ExamEntity,
    @Relation(parentColumn = "subjectId", entityColumn = "id")
    val subject: SubjectEntity?,
    @Relation(parentColumn = "id", entityColumn = "examId")
    val chapters: List<ChapterEntity>,
    @Relation(parentColumn = "id", entityColumn = "examId")
    val reminders: List<ReminderEntity>,
) {
    val subjectName: String get() = subject?.name ?: "Unknown subject"
    val subjectColor: Int get() = subject?.colorIndex ?: 0
    val sortedChapters: List<ChapterEntity> get() = chapters.sortedBy { it.position }
    val chaptersDone: Int get() = chapters.count { it.isDone }
}

data class AssignmentWithDetails(
    @Embedded val assignment: AssignmentEntity,
    @Relation(parentColumn = "subjectId", entityColumn = "id")
    val subject: SubjectEntity?,
    @Relation(parentColumn = "id", entityColumn = "assignmentId")
    val reminders: List<ReminderEntity>,
) {
    val subjectName: String get() = subject?.name ?: "Unknown subject"
    val subjectColor: Int get() = subject?.colorIndex ?: 0
}

data class SubjectWithUsage(
    @Embedded val subject: SubjectEntity,
    val examCount: Int,
    val assignmentCount: Int,
)
