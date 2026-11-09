@file:OptIn(ExperimentalMaterial3Api::class)

package com.studentreminder.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.studentreminder.app.data.local.AssignmentWithDetails
import com.studentreminder.app.data.local.SubjectEntity
import com.studentreminder.app.ui.AppNavigator
import com.studentreminder.app.ui.AssignmentEditorViewModel
import com.studentreminder.app.ui.EventsState
import com.studentreminder.app.ui.EventsViewModel
import com.studentreminder.app.ui.components.AddEventFab
import com.studentreminder.app.ui.components.AssignmentCard
import com.studentreminder.app.ui.components.AssignmentStatus
import com.studentreminder.app.ui.components.ConfirmDialog
import com.studentreminder.app.ui.components.DatePickerModal
import com.studentreminder.app.ui.components.EmptyState
import com.studentreminder.app.ui.components.PickerField
import com.studentreminder.app.ui.components.PrioritySelector
import com.studentreminder.app.ui.components.ReminderSelector
import com.studentreminder.app.ui.components.SectionHeader
import com.studentreminder.app.ui.components.SubjectField
import com.studentreminder.app.ui.components.TimePickerModal
import com.studentreminder.app.ui.components.rememberNow
import com.studentreminder.app.ui.components.status
import com.studentreminder.app.ui.theme.AppColors
import com.studentreminder.app.ui.theme.LocalFormatters

private enum class AssignmentFilter(val label: String) { ALL("All"), PENDING("Pending"), OVERDUE("Overdue"), COMPLETED("Completed") }

// ---------------------------------------------------------------- Assignments list

@Composable
fun AssignmentsScreen(state: EventsState, navigator: AppNavigator, events: EventsViewModel) {
    val now = rememberNow()
    var filter by rememberSaveable { mutableStateOf(AssignmentFilter.ALL) }
    var subjectFilter by rememberSaveable { mutableStateOf<Long?>(null) }

    val bySubject = state.assignments.filter { subjectFilter == null || it.assignment.subjectId == subjectFilter }
    val overdue = bySubject.filter { it.status(now) == AssignmentStatus.OVERDUE }.sortedBy { it.assignment.dueDateTime }
    val pending = bySubject.filter { it.status(now).let { s -> s == AssignmentStatus.PENDING || s == AssignmentStatus.DUE_TODAY } }
        .sortedBy { it.assignment.dueDateTime }
    val completed = bySubject.filter { it.assignment.isCompleted }
        .sortedByDescending { it.assignment.completedAt ?: it.assignment.dueDateTime }
    val usedSubjects = state.subjects.filter { s -> state.assignments.any { it.assignment.subjectId == s.id } }

    val sections: List<Pair<String, List<AssignmentWithDetails>>> = when (filter) {
        AssignmentFilter.ALL -> listOf("Overdue" to overdue, "Pending" to pending, "Completed" to completed)
        AssignmentFilter.PENDING -> listOf("" to pending)
        AssignmentFilter.OVERDUE -> listOf("" to overdue)
        AssignmentFilter.COMPLETED -> listOf("" to completed)
    }
    val isEmpty = sections.all { it.second.isEmpty() }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Assignments") },
                actions = { IconButton(onClick = navigator::search) { Icon(Icons.Rounded.Search, "Search") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        floatingActionButton = {
            AddEventFab(onAddExam = { navigator.addExam() }, onAddAssignment = { navigator.addAssignment() })
        },
    ) { inner ->
        Column(Modifier.padding(inner)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(AssignmentFilter.entries.toList()) { f ->
                    val count = when (f) {
                        AssignmentFilter.ALL -> bySubject.size
                        AssignmentFilter.PENDING -> pending.size
                        AssignmentFilter.OVERDUE -> overdue.size
                        AssignmentFilter.COMPLETED -> completed.size
                    }
                    FilterChip(
                        selected = filter == f,
                        onClick = { filter = f },
                        label = { Text("${f.label} ($count)") },
                        leadingIcon = when (f) {
                            AssignmentFilter.OVERDUE -> ({ Icon(Icons.Rounded.Warning, null, tint = AppColors.overdue) })
                            AssignmentFilter.COMPLETED -> ({ Icon(Icons.Rounded.CheckCircle, null, tint = AppColors.completed) })
                            else -> null
                        },
                    )
                }
            }
            if (usedSubjects.size > 1) SubjectFilterRow(usedSubjects, subjectFilter) { subjectFilter = it }

            if (state.loaded && isEmpty) {
                EmptyState(
                    icon = if (filter == AssignmentFilter.COMPLETED) Icons.Rounded.CheckCircle else Icons.AutoMirrored.Rounded.Assignment,
                    title = when (filter) {
                        AssignmentFilter.ALL -> "No assignments yet"
                        AssignmentFilter.PENDING -> "Nothing pending"
                        AssignmentFilter.OVERDUE -> "No overdue work 🎉"
                        AssignmentFilter.COMPLETED -> "Nothing completed yet"
                    },
                    message = when (filter) {
                        AssignmentFilter.ALL -> "Add an assignment with its due date and priority, and we'll remind you in time."
                        AssignmentFilter.OVERDUE -> "You're on top of everything. Keep it up!"
                        else -> "Assignments you add will show up here."
                    },
                    action = if (filter == AssignmentFilter.ALL) ({
                        Button(onClick = { navigator.addAssignment() }) { Text("Add assignment") }
                    }) else null,
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    sections.forEach { (title, list) ->
                        if (list.isNotEmpty()) {
                            if (title.isNotEmpty()) {
                                item(key = "h-$title") { SectionHeader("$title · ${list.size}", Modifier.animateItem()) }
                            }
                            items(list, key = { it.assignment.id }) { a ->
                                AssignmentCard(
                                    item = a,
                                    now = now,
                                    onToggleComplete = { events.setAssignmentCompleted(a.assignment.id, !a.assignment.isCompleted) },
                                    onClick = { navigator.openAssignment(a.assignment.id) },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Assignment editor

@Composable
fun AssignmentEditScreen(
    vm: AssignmentEditorViewModel,
    subjects: List<SubjectEntity>,
    navigator: AppNavigator,
    events: EventsViewModel,
    assignmentId: Long?,
) {
    val f = LocalFormatters.current
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            TopAppBar(
                title = { Text(if (vm.isNew) "New assignment" else "Edit assignment") },
                navigationIcon = { IconButton(onClick = navigator::back) { Icon(Icons.Rounded.Close, "Cancel") } },
                actions = {
                    if (!vm.isNew) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, "Delete") }
                },
            )
        },
    ) { inner ->
        if (!vm.loaded) return@Scaffold
        Column(
            Modifier
                .padding(inner)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = vm.title,
                onValueChange = { vm.title = it },
                label = { Text("Assignment title *") },
                placeholder = { Text("e.g. Lab report 3") },
                leadingIcon = { Icon(Icons.Rounded.Title, null) },
                singleLine = true,
                isError = vm.titleError,
                supportingText = if (vm.titleError) ({ Text("Please enter a title") }) else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            SubjectField(vm.subject, { vm.subject = it }, subjects, vm.subjectError)
            OutlinedTextField(
                value = vm.description,
                onValueChange = { vm.description = it },
                label = { Text("Description") },
                leadingIcon = { Icon(Icons.Rounded.Description, null) },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PickerField(
                    label = "Due date *",
                    value = vm.date?.let { f.date(it) },
                    icon = Icons.Rounded.Event,
                    onClick = { showDate = true },
                    isError = vm.dateError,
                    modifier = Modifier.weight(1.4f),
                )
                PickerField(
                    label = "Due time",
                    value = vm.time?.let { f.time(it) },
                    icon = Icons.Rounded.Schedule,
                    onClick = { showTime = true },
                    modifier = Modifier.weight(1f),
                )
            }

            SectionHeader("Priority")
            PrioritySelector(vm.priority, { vm.priority = it })

            if (!vm.isNew) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = AppColors.completed)
                        Spacer(Modifier.padding(6.dp))
                        Text("Completed", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        Switch(checked = vm.completed, onCheckedChange = { vm.completed = it })
                    }
                }
            }

            SectionHeader("Remind me")
            ReminderSelector(vm.reminders, { vm.reminders = it })

            HorizontalDivider()
            Button(
                onClick = { vm.save { navigator.back() } },
                enabled = !vm.saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(if (vm.isNew) "Save assignment" else "Save changes", style = MaterialTheme.typography.titleMedium)
            }
            if (vm.showErrors && (vm.titleError || vm.subjectError || vm.dateError)) {
                Text("Please fill in the required fields marked *", color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDate) DatePickerModal(vm.date, { vm.date = it }, { showDate = false })
    if (showTime) TimePickerModal(vm.time, { vm.time = it }, { showTime = false })
    if (confirmDelete && assignmentId != null) {
        ConfirmDialog(
            title = "Delete assignment?",
            message = "\"${vm.title}\" and its reminders will be permanently deleted.",
            onConfirm = { events.deleteAssignment(assignmentId); navigator.back() },
            onDismiss = { confirmDelete = false },
        )
    }
}
