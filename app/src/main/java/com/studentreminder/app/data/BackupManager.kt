package com.studentreminder.app.data

import android.content.Context
import android.net.Uri
import com.studentreminder.app.data.local.AssignmentEntity
import com.studentreminder.app.data.local.ChapterEntity
import com.studentreminder.app.data.local.ExamEntity
import com.studentreminder.app.data.local.ReminderEntity
import com.studentreminder.app.data.local.SubjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek

data class BackupSummary(val subjects: Int, val exams: Int, val assignments: Int)

/**
 * Exports everything (subjects, exams, chapters, assignments, reminder configuration and
 * settings) to a single JSON file chosen by the user, and restores it again.
 */
class BackupManager(
    private val context: Context,
    private val repository: StudentRepository,
    private val settingsRepository: SettingsRepository,
) {

    suspend fun export(uri: Uri): Result<BackupSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val (subjects, exams, assignments) = repository.snapshot()
            val s = settingsRepository.current()
            val root = JSONObject().apply {
                put("format", FORMAT)
                put("version", VERSION)
                put("exportedAt", System.currentTimeMillis())
                put("subjects", JSONArray().apply {
                    subjects.forEach { put(JSONObject().put("id", it.id).put("name", it.name).put("colorIndex", it.colorIndex)) }
                })
                put("exams", JSONArray().apply {
                    exams.forEach { e ->
                        put(JSONObject().apply {
                            put("subjectId", e.exam.subjectId)
                            put("dateTime", e.exam.dateTime)
                            put("location", e.exam.location)
                            put("notes", e.exam.notes)
                            put("createdAt", e.exam.createdAt)
                            put("chapters", JSONArray().apply {
                                e.sortedChapters.forEach { c ->
                                    put(JSONObject().put("title", c.title).put("position", c.position).put("isDone", c.isDone))
                                }
                            })
                            put("reminders", remindersJson(e.reminders))
                        })
                    }
                })
                put("assignments", JSONArray().apply {
                    assignments.forEach { a ->
                        put(JSONObject().apply {
                            put("title", a.assignment.title)
                            put("subjectId", a.assignment.subjectId)
                            put("description", a.assignment.description)
                            put("dueDateTime", a.assignment.dueDateTime)
                            put("priority", a.assignment.priority)
                            put("isCompleted", a.assignment.isCompleted)
                            a.assignment.completedAt?.let { put("completedAt", it) }
                            put("createdAt", a.assignment.createdAt)
                            put("reminders", remindersJson(a.reminders))
                        })
                    }
                })
                put("settings", JSONObject().apply {
                    put("userName", s.userName)
                    put("themeMode", s.themeMode.name)
                    put("accent", s.accent.name)
                    put("dynamicColor", s.dynamicColor)
                    put("use24h", s.use24h)
                    put("firstDayOfWeek", s.firstDayOfWeek.name)
                    put("notificationsEnabled", s.notificationsEnabled)
                    put("sameDayHour", s.sameDayHour)
                    put("defaultExamReminders", JSONArray(s.defaultExamReminders))
                    put("defaultAssignmentReminders", JSONArray(s.defaultAssignmentReminders))
                })
            }
            val out = context.contentResolver.openOutputStream(uri, "wt")
                ?: error("Couldn't open the selected file")
            out.use { it.write(root.toString(2).toByteArray(Charsets.UTF_8)) }
            BackupSummary(subjects.size, exams.size, assignments.size)
        }
    }

    suspend fun restore(uri: Uri): Result<BackupSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: error("Couldn't open the selected file")
            val root = JSONObject(text)
            require(root.optString("format") == FORMAT) { "This file is not a Student Reminder backup" }

            val subjects = root.getJSONArray("subjects").objects().map {
                SubjectEntity(id = it.getLong("id"), name = it.getString("name"), colorIndex = it.optInt("colorIndex", 0))
            }
            val exams = root.getJSONArray("exams").objects().map { o ->
                val exam = ExamEntity(
                    subjectId = o.getLong("subjectId"),
                    dateTime = o.getLong("dateTime"),
                    location = o.optString("location", ""),
                    notes = o.optString("notes", ""),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                )
                val chapters = o.optJSONArray("chapters")?.objects().orEmpty().map { c ->
                    ChapterEntity(examId = 0, title = c.getString("title"), position = c.optInt("position", 0), isDone = c.optBoolean("isDone", false))
                }
                Triple(exam, chapters, parseReminders(o.optJSONArray("reminders")))
            }
            val assignments = root.getJSONArray("assignments").objects().map { o ->
                AssignmentEntity(
                    title = o.getString("title"),
                    subjectId = o.getLong("subjectId"),
                    description = o.optString("description", ""),
                    dueDateTime = o.getLong("dueDateTime"),
                    priority = o.optInt("priority", 1),
                    isCompleted = o.optBoolean("isCompleted", false),
                    completedAt = if (o.has("completedAt")) o.getLong("completedAt") else null,
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                ) to parseReminders(o.optJSONArray("reminders"))
            }

            root.optJSONObject("settings")?.let { js ->
                settingsRepository.update { s ->
                    s.copy(
                        userName = js.optString("userName", s.userName),
                        themeMode = ThemeMode.entries.firstOrNull { it.name == js.optString("themeMode") } ?: s.themeMode,
                        accent = AccentColor.entries.firstOrNull { it.name == js.optString("accent") } ?: s.accent,
                        dynamicColor = js.optBoolean("dynamicColor", s.dynamicColor),
                        use24h = js.optBoolean("use24h", s.use24h),
                        firstDayOfWeek = DayOfWeek.values().firstOrNull { it.name == js.optString("firstDayOfWeek") } ?: s.firstDayOfWeek,
                        notificationsEnabled = js.optBoolean("notificationsEnabled", s.notificationsEnabled),
                        sameDayHour = js.optInt("sameDayHour", s.sameDayHour),
                        defaultExamReminders = js.optJSONArray("defaultExamReminders")?.longs() ?: s.defaultExamReminders,
                        defaultAssignmentReminders = js.optJSONArray("defaultAssignmentReminders")?.longs() ?: s.defaultAssignmentReminders,
                    )
                }
            }

            repository.replaceAll(subjects, exams, assignments)
            BackupSummary(subjects.size, exams.size, assignments.size)
        }
    }

    private fun remindersJson(list: List<ReminderEntity>) = JSONArray().apply {
        list.forEach { r ->
            put(JSONObject().apply {
                put("offsetMinutes", r.offsetMinutes)
                r.firedAt?.let { put("firedAt", it) }
            })
        }
    }

    private fun parseReminders(arr: JSONArray?): List<ReminderEntity> =
        arr?.objects().orEmpty().map {
            ReminderEntity(
                offsetMinutes = it.getLong("offsetMinutes"),
                firedAt = if (it.has("firedAt")) it.getLong("firedAt") else null,
            )
        }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
    private fun JSONArray.longs(): List<Long> = (0 until length()).map { getLong(it) }

    companion object {
        const val FORMAT = "student-reminder-backup"
        const val VERSION = 1
    }
}
