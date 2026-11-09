package com.studentreminder.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<SubjectEntity>>

    @Query(
        """
        SELECT s.*,
          (SELECT COUNT(*) FROM exams e WHERE e.subjectId = s.id) AS examCount,
          (SELECT COUNT(*) FROM assignments a WHERE a.subjectId = s.id) AS assignmentCount
        FROM subjects s ORDER BY s.name COLLATE NOCASE ASC
        """
    )
    fun observeWithUsage(): Flow<List<SubjectWithUsage>>

    @Query("SELECT * FROM subjects")
    suspend fun getAll(): List<SubjectEntity>

    @Query("SELECT * FROM subjects WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): SubjectEntity?

    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(subject: SubjectEntity): Long

    @Update
    suspend fun update(subject: SubjectEntity)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT (SELECT COUNT(*) FROM exams WHERE subjectId = :id) + (SELECT COUNT(*) FROM assignments WHERE subjectId = :id)")
    suspend fun usageCount(id: Long): Int

    @Query("DELETE FROM subjects")
    suspend fun deleteAll()
}

@Dao
interface ExamDao {
    @Transaction
    @Query("SELECT * FROM exams ORDER BY dateTime ASC")
    fun observeAll(): Flow<List<ExamWithDetails>>

    @Transaction
    @Query("SELECT * FROM exams WHERE id = :id")
    fun observe(id: Long): Flow<ExamWithDetails?>

    @Transaction
    @Query("SELECT * FROM exams WHERE id = :id")
    suspend fun get(id: Long): ExamWithDetails?

    @Transaction
    @Query("SELECT * FROM exams ORDER BY dateTime ASC")
    suspend fun getAll(): List<ExamWithDetails>

    @Insert
    suspend fun insert(exam: ExamEntity): Long

    @Update
    suspend fun update(exam: ExamEntity)

    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM exams")
    suspend fun deleteAll()

    // Chapters
    @Insert
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Query("DELETE FROM chapters WHERE examId = :examId")
    suspend fun deleteChapters(examId: Long)

    @Query("UPDATE chapters SET isDone = :done WHERE id = :chapterId")
    suspend fun setChapterDone(chapterId: Long, done: Boolean)
}

@Dao
interface AssignmentDao {
    @Transaction
    @Query("SELECT * FROM assignments ORDER BY dueDateTime ASC")
    fun observeAll(): Flow<List<AssignmentWithDetails>>

    @Transaction
    @Query("SELECT * FROM assignments WHERE id = :id")
    fun observe(id: Long): Flow<AssignmentWithDetails?>

    @Transaction
    @Query("SELECT * FROM assignments WHERE id = :id")
    suspend fun get(id: Long): AssignmentWithDetails?

    @Transaction
    @Query("SELECT * FROM assignments ORDER BY dueDateTime ASC")
    suspend fun getAll(): List<AssignmentWithDetails>

    @Insert
    suspend fun insert(assignment: AssignmentEntity): Long

    @Update
    suspend fun update(assignment: AssignmentEntity)

    @Query("UPDATE assignments SET isCompleted = :completed, completedAt = :completedAt WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean, completedAt: Long?)

    @Query("DELETE FROM assignments WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM assignments")
    suspend fun deleteAll()
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders")
    suspend fun getAll(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun get(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE examId = :examId")
    suspend fun forExam(examId: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE assignmentId = :assignmentId")
    suspend fun forAssignment(assignmentId: Long): List<ReminderEntity>

    @Insert
    suspend fun insertAll(reminders: List<ReminderEntity>): List<Long>

    @Query("DELETE FROM reminders WHERE examId = :examId")
    suspend fun deleteForExam(examId: Long)

    @Query("DELETE FROM reminders WHERE assignmentId = :assignmentId")
    suspend fun deleteForAssignment(assignmentId: Long)

    @Query("UPDATE reminders SET firedAt = :firedAt WHERE id = :id")
    suspend fun markFired(id: Long, firedAt: Long)

    @Query("DELETE FROM reminders")
    suspend fun deleteAll()
}
