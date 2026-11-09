package com.studentreminder.app.data

import androidx.room.withTransaction
import com.studentreminder.app.data.local.AppDatabase
import com.studentreminder.app.data.local.AssignmentEntity
import com.studentreminder.app.data.local.AssignmentWithDetails
import com.studentreminder.app.data.local.ChapterEntity
import com.studentreminder.app.data.local.ExamEntity
import com.studentreminder.app.data.local.ExamWithDetails
import com.studentreminder.app.data.local.ReminderEntity
import com.studentreminder.app.data.local.SubjectEntity
import com.studentreminder.app.data.local.SubjectWithUsage
import com.studentreminder.app.reminders.ReminderScheduler
import kotlinx.coroutines.flow.Flow

data class ChapterDraft(val id: Long = 0, val title: String, val isDone: Boolean = false)

/** Single source of truth for all academic data + reminder scheduling. */
class StudentRepository(
    private val db: AppDatabase,
    private val settings: SettingsRepository,
    private val scheduler: ReminderScheduler,
) {
    private val subjectDao = db.subjectDao()
    private val examDao = db.examDao()
    private val assignmentDao = db.assignmentDao()
    private val reminderDao = db.reminderDao()

    // ---------- Observe ----------
    val exams: Flow<List<ExamWithDetails>> = examDao.observeAll()
    val assignments: Flow<List<AssignmentWithDetails>> = assignmentDao.observeAll()
    val subjects: Flow<List<SubjectEntity>> = subjectDao.observeAll()
    val subjectsWithUsage: Flow<List<SubjectWithUsage>> = subjectDao.observeWithUsage()

    fun exam(id: Long): Flow<ExamWithDetails?> = examDao.observe(id)
    fun assignment(id: Long): Flow<AssignmentWithDetails?> = assignmentDao.observe(id)
    suspend fun getExam(id: Long) = examDao.get(id)
    suspend fun getAssignment(id: Long) = assignmentDao.get(id)

    // ---------- Subjects ----------
    private suspend fun subjectIdFor(name: String): Long {
        val clean = name.trim()
        subjectDao.findByName(clean)?.let { return it.id }
        val color = subjectDao.count() % SUBJECT_COLOR_COUNT
        return subjectDao.insert(SubjectEntity(name = clean, colorIndex = color))
    }

    suspend fun addSubject(name: String): Result<Unit> = runCatching {
        require(name.isNotBlank()) { "Subject name can't be empty" }
        require(subjectDao.findByName(name.trim()) == null) { "\"${name.trim()}\" already exists" }
        subjectIdFor(name)
        Unit
    }

    suspend fun updateSubject(subject: SubjectEntity): Result<Unit> = runCatching {
        require(subject.name.isNotBlank()) { "Subject name can't be empty" }
        val existing = subjectDao.findByName(subject.name.trim())
        require(existing == null || existing.id == subject.id) { "\"${subject.name.trim()}\" already exists" }
        subjectDao.update(subject.copy(name = subject.name.trim()))
    }

    suspend fun deleteSubject(id: Long): Result<Unit> = runCatching {
        val used = subjectDao.usageCount(id)
        require(used == 0) { "This subject is used by $used exam(s)/assignment(s). Delete or move them first." }
        subjectDao.delete(id)
    }

    // ---------- Exams ----------
    suspend fun saveExam(
        id: Long?,
        subjectName: String,
        dateTime: Long,
        location: String,
        notes: String,
        chapters: List<ChapterDraft>,
        reminderOffsets: List<Long>,
    ): Long {
        val oldReminderIds = if (id != null) reminderDao.forExam(id).map { it.id } else emptyList()
        val examId = db.withTransaction {
            val subjectId = subjectIdFor(subjectName)
            val examId = if (id == null) {
                examDao.insert(
                    ExamEntity(subjectId = subjectId, dateTime = dateTime, location = location.trim(), notes = notes.trim())
                )
            } else {
                val old = examDao.get(id)?.exam ?: error("Exam not found")
                examDao.update(old.copy(subjectId = subjectId, dateTime = dateTime, location = location.trim(), notes = notes.trim()))
                id
            }
            examDao.deleteChapters(examId)
            examDao.insertChapters(
                chapters.filter { it.title.isNotBlank() }.mapIndexed { i, c ->
                    ChapterEntity(examId = examId, title = c.title.trim(), position = i, isDone = c.isDone)
                }
            )
            reminderDao.deleteForExam(examId)
            reminderDao.insertAll(buildReminders(reminderOffsets, dateTime) { ReminderEntity(examId = examId, offsetMinutes = it.first, firedAt = it.second) })
            examId
        }
        oldReminderIds.forEach { scheduler.cancel(it) }
        scheduleExam(examId)
        return examId
    }

    suspend fun deleteExam(id: Long) {
        reminderDao.forExam(id).forEach { scheduler.cancel(it.id) }
        examDao.delete(id) // chapters + reminders cascade
    }

    suspend fun setChapterDone(chapterId: Long, done: Boolean) = examDao.setChapterDone(chapterId, done)

    // ---------- Assignments ----------
    suspend fun saveAssignment(
        id: Long?,
        title: String,
        subjectName: String,
        description: String,
        dueDateTime: Long,
        priority: Int,
        isCompleted: Boolean,
        reminderOffsets: List<Long>,
    ): Long {
        val oldReminderIds = if (id != null) reminderDao.forAssignment(id).map { it.id } else emptyList()
        val assignmentId = db.withTransaction {
            val subjectId = subjectIdFor(subjectName)
            val aId = if (id == null) {
                assignmentDao.insert(
                    AssignmentEntity(
                        title = title.trim(), subjectId = subjectId, description = description.trim(),
                        dueDateTime = dueDateTime, priority = priority, isCompleted = isCompleted,
                        completedAt = if (isCompleted) System.currentTimeMillis() else null,
                    )
                )
            } else {
                val old = assignmentDao.get(id)?.assignment ?: error("Assignment not found")
                assignmentDao.update(
                    old.copy(
                        title = title.trim(), subjectId = subjectId, description = description.trim(),
                        dueDateTime = dueDateTime, priority = priority, isCompleted = isCompleted,
                        completedAt = when {
                            !isCompleted -> null
                            old.completedAt != null -> old.completedAt
                            else -> System.currentTimeMillis()
                        },
                    )
                )
                id
            }
            reminderDao.deleteForAssignment(aId)
            reminderDao.insertAll(buildReminders(reminderOffsets, dueDateTime) { ReminderEntity(assignmentId = aId, offsetMinutes = it.first, firedAt = it.second) })
            aId
        }
        oldReminderIds.forEach { scheduler.cancel(it) }
        scheduleAssignment(assignmentId)
        return assignmentId
    }

    suspend fun setAssignmentCompleted(id: Long, completed: Boolean) {
        assignmentDao.setCompleted(id, completed, if (completed) System.currentTimeMillis() else null)
        if (completed) {
            reminderDao.forAssignment(id).forEach { scheduler.cancel(it.id) }
        } else {
            scheduleAssignment(id)
        }
    }

    suspend fun deleteAssignment(id: Long) {
        reminderDao.forAssignment(id).forEach { scheduler.cancel(it.id) }
        assignmentDao.delete(id)
    }

    // ---------- Reminder scheduling ----------

    /** Reminders whose trigger time has already passed are stored as "fired" so they never pop up late. */
    private suspend fun buildReminders(
        offsets: List<Long>,
        eventMillis: Long,
        make: (Pair<Long, Long?>) -> ReminderEntity,
    ): List<ReminderEntity> {
        val hour = settings.current().sameDayHour
        val now = System.currentTimeMillis()
        return offsets.distinct().map { offset ->
            val trigger = ReminderOption.triggerTime(eventMillis, offset, hour)
            make(offset to (if (trigger <= now) now else null))
        }
    }

    private suspend fun scheduleExam(examId: Long) {
        val exam = examDao.get(examId) ?: return
        val hour = settings.current().sameDayHour
        exam.reminders.forEach { scheduleOne(it, exam.exam.dateTime, hour, active = true) }
    }

    private suspend fun scheduleAssignment(assignmentId: Long) {
        val a = assignmentDao.get(assignmentId) ?: return
        val hour = settings.current().sameDayHour
        a.reminders.forEach { scheduleOne(it, a.assignment.dueDateTime, hour, active = !a.assignment.isCompleted) }
    }

    private fun scheduleOne(r: ReminderEntity, eventMillis: Long, sameDayHour: Int, active: Boolean) {
        val now = System.currentTimeMillis()
        val trigger = ReminderOption.triggerTime(eventMillis, r.offsetMinutes, sameDayHour)
        when {
            !active -> scheduler.cancel(r.id)
            trigger > now -> scheduler.schedule(r.id, trigger)
            // Missed while the phone was off: deliver it now if the event is still ahead
            // and the miss happened in the last 12 hours.
            r.firedAt == null && eventMillis > now && now - trigger < MISSED_WINDOW_MS ->
                scheduler.schedule(r.id, now + 3_000L)
            else -> scheduler.cancel(r.id)
        }
    }

    /** Re-registers every alarm. Safe to call any time (boot, app start, settings change, restore). */
    suspend fun rescheduleAll() {
        val hour = settings.current().sameDayHour
        examDao.getAll().forEach { e ->
            e.reminders.forEach { scheduleOne(it, e.exam.dateTime, hour, active = true) }
        }
        assignmentDao.getAll().forEach { a ->
            a.reminders.forEach { scheduleOne(it, a.assignment.dueDateTime, hour, active = !a.assignment.isCompleted) }
        }
    }

    suspend fun markReminderFired(id: Long) = reminderDao.markFired(id, System.currentTimeMillis())
    suspend fun getReminder(id: Long) = reminderDao.get(id)

    // ---------- Backup / restore helpers ----------
    suspend fun snapshot(): Triple<List<SubjectEntity>, List<ExamWithDetails>, List<AssignmentWithDetails>> =
        Triple(subjectDao.getAll(), examDao.getAll(), assignmentDao.getAll())

    suspend fun replaceAll(
        subjects: List<SubjectEntity>,
        exams: List<Triple<ExamEntity, List<ChapterEntity>, List<ReminderEntity>>>,
        assignments: List<Pair<AssignmentEntity, List<ReminderEntity>>>,
    ) {
        reminderDao.getAll().forEach { scheduler.cancel(it.id) }
        db.withTransaction {
            reminderDao.deleteAll()
            examDao.deleteAll()
            assignmentDao.deleteAll()
            subjectDao.deleteAll()
            val subjectIdMap = HashMap<Long, Long>()
            subjects.forEach { s -> subjectIdMap[s.id] = subjectDao.insert(s.copy(id = 0)) }
            exams.forEach { (exam, chapters, reminders) ->
                val sid = subjectIdMap[exam.subjectId] ?: return@forEach
                val newId = examDao.insert(exam.copy(id = 0, subjectId = sid))
                examDao.insertChapters(chapters.map { it.copy(id = 0, examId = newId) })
                reminderDao.insertAll(reminders.map { it.copy(id = 0, examId = newId, assignmentId = null) })
            }
            assignments.forEach { (a, reminders) ->
                val sid = subjectIdMap[a.subjectId] ?: return@forEach
                val newId = assignmentDao.insert(a.copy(id = 0, subjectId = sid))
                reminderDao.insertAll(reminders.map { it.copy(id = 0, assignmentId = newId, examId = null) })
            }
        }
        rescheduleAll()
    }

    companion object {
        const val SUBJECT_COLOR_COUNT = 10
        private const val MISSED_WINDOW_MS = 12 * 60 * 60 * 1000L
    }
}
