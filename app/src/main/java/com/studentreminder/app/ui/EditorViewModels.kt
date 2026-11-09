package com.studentreminder.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentreminder.app.data.ChapterDraft
import com.studentreminder.app.data.SettingsRepository
import com.studentreminder.app.data.StudentRepository
import com.studentreminder.app.util.TimeUtils
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/** A chapter row being edited; [key] keeps list animations stable. */
data class ChapterRow(val key: Long, val id: Long, val title: String, val isDone: Boolean)

class ExamEditorViewModel(
    private val examId: Long?,
    initialDate: LocalDate?,
    private val repository: StudentRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    val isNew = examId == null
    var loaded by mutableStateOf(false)
        private set
    var subject by mutableStateOf("")
    var date by mutableStateOf<LocalDate?>(initialDate)
    var time by mutableStateOf<LocalTime?>(LocalTime.of(9, 0))
    var location by mutableStateOf("")
    var notes by mutableStateOf("")
    val chapters = mutableStateListOf<ChapterRow>()
    var reminders by mutableStateOf(setOf<Long>())
    var showErrors by mutableStateOf(false)
    var saving by mutableStateOf(false)
        private set

    private var nextKey = 1L

    init {
        viewModelScope.launch {
            if (examId != null) {
                repository.getExam(examId)?.let { e ->
                    subject = e.subjectName
                    val dt = TimeUtils.toLocalDateTime(e.exam.dateTime)
                    date = dt.toLocalDate()
                    time = dt.toLocalTime()
                    location = e.exam.location
                    notes = e.exam.notes
                    chapters.clear()
                    e.sortedChapters.forEach { c -> chapters.add(ChapterRow(nextKey++, c.id, c.title, c.isDone)) }
                    reminders = e.reminders.map { it.offsetMinutes }.toSet()
                }
            } else {
                reminders = settings.current().defaultExamReminders.toSet()
                chapters.add(ChapterRow(nextKey++, 0, "", false))
            }
            loaded = true
        }
    }

    fun addChapter() { chapters.add(ChapterRow(nextKey++, 0, "", false)) }
    fun updateChapter(key: Long, title: String) {
        val i = chapters.indexOfFirst { it.key == key }
        if (i >= 0) chapters[i] = chapters[i].copy(title = title)
    }
    fun removeChapter(key: Long) { chapters.removeAll { it.key == key } }
    fun moveChapter(key: Long, up: Boolean) {
        val i = chapters.indexOfFirst { it.key == key }
        val j = if (up) i - 1 else i + 1
        if (i < 0 || j !in chapters.indices) return
        val tmp = chapters[i]; chapters[i] = chapters[j]; chapters[j] = tmp
    }

    val subjectError get() = showErrors && subject.isBlank()
    val dateError get() = showErrors && date == null
    val timeError get() = showErrors && time == null

    fun save(onSaved: () -> Unit) {
        showErrors = true
        val d = date ?: return
        val t = time ?: return
        if (subject.isBlank() || saving) return
        saving = true
        viewModelScope.launch {
            repository.saveExam(
                id = examId,
                subjectName = subject,
                dateTime = TimeUtils.toMillis(d, t),
                location = location,
                notes = notes,
                chapters = chapters.map { ChapterDraft(it.id, it.title, it.isDone) },
                reminderOffsets = reminders.toList(),
            )
            saving = false
            onSaved()
        }
    }
}

class AssignmentEditorViewModel(
    private val assignmentId: Long?,
    initialDate: LocalDate?,
    private val repository: StudentRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    val isNew = assignmentId == null
    var loaded by mutableStateOf(false)
        private set
    var title by mutableStateOf("")
    var subject by mutableStateOf("")
    var description by mutableStateOf("")
    var date by mutableStateOf<LocalDate?>(initialDate)
    var time by mutableStateOf<LocalTime?>(LocalTime.of(23, 59))
    var priority by mutableStateOf(1)
    var completed by mutableStateOf(false)
    var reminders by mutableStateOf(setOf<Long>())
    var showErrors by mutableStateOf(false)
    var saving by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            if (assignmentId != null) {
                repository.getAssignment(assignmentId)?.let { a ->
                    title = a.assignment.title
                    subject = a.subjectName
                    description = a.assignment.description
                    val dt = TimeUtils.toLocalDateTime(a.assignment.dueDateTime)
                    date = dt.toLocalDate()
                    time = dt.toLocalTime()
                    priority = a.assignment.priority
                    completed = a.assignment.isCompleted
                    reminders = a.reminders.map { it.offsetMinutes }.toSet()
                }
            } else {
                reminders = settings.current().defaultAssignmentReminders.toSet()
            }
            loaded = true
        }
    }

    val titleError get() = showErrors && title.isBlank()
    val subjectError get() = showErrors && subject.isBlank()
    val dateError get() = showErrors && date == null

    fun save(onSaved: () -> Unit) {
        showErrors = true
        val d = date ?: return
        val t = time ?: LocalTime.of(23, 59)
        if (title.isBlank() || subject.isBlank() || saving) return
        saving = true
        viewModelScope.launch {
            repository.saveAssignment(
                id = assignmentId,
                title = title,
                subjectName = subject,
                description = description,
                dueDateTime = TimeUtils.toMillis(d, t),
                priority = priority,
                isCompleted = completed,
                reminderOffsets = reminders.toList(),
            )
            saving = false
            onSaved()
        }
    }
}
