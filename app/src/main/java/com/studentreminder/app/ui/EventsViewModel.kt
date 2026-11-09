package com.studentreminder.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentreminder.app.data.StudentRepository
import com.studentreminder.app.data.local.AssignmentWithDetails
import com.studentreminder.app.data.local.ExamWithDetails
import com.studentreminder.app.data.local.SubjectEntity
import com.studentreminder.app.data.local.SubjectWithUsage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** An exam or an assignment, used wherever both are shown together. */
sealed interface AgendaItem {
    val time: Long
    val key: String

    data class Exam(val data: ExamWithDetails) : AgendaItem {
        override val time get() = data.exam.dateTime
        override val key get() = "exam-${data.exam.id}"
    }

    data class Assignment(val data: AssignmentWithDetails) : AgendaItem {
        override val time get() = data.assignment.dueDateTime
        override val key get() = "assignment-${data.assignment.id}"
    }
}

data class EventsState(
    val loaded: Boolean = false,
    val exams: List<ExamWithDetails> = emptyList(),
    val assignments: List<AssignmentWithDetails> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
) {
    /** Everything, sorted by date/time (nearest first). */
    val agenda: List<AgendaItem>
        get() = (exams.map { AgendaItem.Exam(it) } + assignments.map { AgendaItem.Assignment(it) })
            .sortedBy { it.time }
}

/** Activity-scoped view model shared by the list, calendar, home and search screens. */
class EventsViewModel(private val repository: StudentRepository) : ViewModel() {

    val state: StateFlow<EventsState> = combine(
        repository.exams,
        repository.assignments,
        repository.subjects,
    ) { exams, assignments, subjects ->
        EventsState(loaded = true, exams = exams, assignments = assignments, subjects = subjects)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventsState())

    val subjectsWithUsage: StateFlow<List<SubjectWithUsage>> =
        repository.subjectsWithUsage.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteExam(id: Long) = viewModelScope.launch { repository.deleteExam(id) }
    fun deleteAssignment(id: Long) = viewModelScope.launch { repository.deleteAssignment(id) }
    fun setAssignmentCompleted(id: Long, completed: Boolean) =
        viewModelScope.launch { repository.setAssignmentCompleted(id, completed) }
    fun setChapterDone(chapterId: Long, done: Boolean) =
        viewModelScope.launch { repository.setChapterDone(chapterId, done) }

    fun addSubject(name: String, onResult: (Result<Unit>) -> Unit) =
        viewModelScope.launch { onResult(repository.addSubject(name)) }
    fun updateSubject(subject: SubjectEntity, onResult: (Result<Unit>) -> Unit) =
        viewModelScope.launch { onResult(repository.updateSubject(subject)) }
    fun deleteSubject(id: Long, onResult: (Result<Unit>) -> Unit) =
        viewModelScope.launch { onResult(repository.deleteSubject(id)) }
}
