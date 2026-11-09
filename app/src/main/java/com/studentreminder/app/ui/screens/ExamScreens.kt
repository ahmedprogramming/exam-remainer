@file:OptIn(ExperimentalMaterial3Api::class)

package com.studentreminder.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.studentreminder.app.data.ReminderOption
import com.studentreminder.app.data.local.ExamWithDetails
import com.studentreminder.app.data.local.SubjectEntity
import com.studentreminder.app.ui.AppNavigator
import com.studentreminder.app.ui.EventsState
import com.studentreminder.app.ui.EventsViewModel
import com.studentreminder.app.ui.ExamEditorViewModel
import com.studentreminder.app.ui.components.AddEventFab
import com.studentreminder.app.ui.components.ConfirmDialog
import com.studentreminder.app.ui.components.DatePickerModal
import com.studentreminder.app.ui.components.EmptyState
import com.studentreminder.app.ui.components.ExamCard
import com.studentreminder.app.ui.components.PickerField
import com.studentreminder.app.ui.components.ReminderSelector
import com.studentreminder.app.ui.components.SectionHeader
import com.studentreminder.app.ui.components.SubjectField
import com.studentreminder.app.ui.components.TimePickerModal
import com.studentreminder.app.ui.components.rememberNow
import com.studentreminder.app.ui.theme.AppColors
import com.studentreminder.app.ui.theme.LocalAppSettings
import com.studentreminder.app.ui.theme.LocalFormatters
import com.studentreminder.app.util.TimeUtils

// ---------------------------------------------------------------- Exams list

@Composable
fun ExamsScreen(state: EventsState, navigator: AppNavigator) {
    val now = rememberNow()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var subjectFilter by rememberSaveable { mutableStateOf<Long?>(null) }

    val filtered = state.exams.filter { subjectFilter == null || it.exam.subjectId == subjectFilter }
    val upcoming = filtered.filter { it.exam.dateTime >= now }.sortedBy { it.exam.dateTime }
    val past = filtered.filter { it.exam.dateTime < now }.sortedByDescending { it.exam.dateTime }
    val shown = if (tab == 0) upcoming else past
    val usedSubjects = state.subjects.filter { s -> state.exams.any { it.exam.subjectId == s.id } }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Exams") },
                actions = {
                    IconButton(onClick = navigator::search) { Icon(Icons.Rounded.Search, "Search") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        floatingActionButton = {
            AddEventFab(onAddExam = { navigator.addExam() }, onAddAssignment = { navigator.addAssignment() })
        },
    ) { inner ->
        Column(Modifier.padding(inner)) {
            PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surface) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Upcoming (${upcoming.size})") },
                    icon = { Icon(Icons.Rounded.Event, null) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Past (${past.size})") },
                    icon = { Icon(Icons.Rounded.History, null) })
            }
            if (usedSubjects.size > 1) {
                SubjectFilterRow(usedSubjects, subjectFilter) { subjectFilter = it }
            }
            if (state.loaded && shown.isEmpty()) {
                EmptyState(
                    icon = if (tab == 0) Icons.Rounded.School else Icons.Rounded.History,
                    title = if (tab == 0) "No upcoming exams" else "No past exams",
                    message = if (tab == 0) "Add an exam with its chapters and choose when you want to be reminded."
                    else "Exams move here automatically once their date has passed.",
                    action = if (tab == 0) ({ Button(onClick = { navigator.addExam() }) { Text("Add exam") } }) else null,
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(shown, key = { it.exam.id }) { exam ->
                        ExamCard(
                            exam = exam,
                            now = now,
                            onClick = { navigator.openExam(exam.exam.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubjectFilterRow(subjects: List<SubjectEntity>, selected: Long?, onSelect: (Long?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("All subjects") }) }
        items(subjects, key = { it.id }) { s ->
            FilterChip(
                selected = selected == s.id,
                onClick = { onSelect(if (selected == s.id) null else s.id) },
                label = { Text(s.name) },
                leadingIcon = {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(AppColors.subject(s.colorIndex))
                    )
                },
            )
        }
    }
}

// ---------------------------------------------------------------- Exam detail

@Composable
fun ExamDetailScreen(examId: Long, state: EventsState, navigator: AppNavigator, events: EventsViewModel) {
    val exam = state.exams.firstOrNull { it.exam.id == examId }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.loaded, exam == null) {
        if (state.loaded && exam == null) navigator.closeExamDetail()
    }

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            TopAppBar(
                title = { Text("Exam details") },
                navigationIcon = {
                    IconButton(onClick = navigator::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { navigator.editExam(examId) }) { Icon(Icons.Rounded.Edit, "Edit") }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, "Delete") }
                },
            )
        },
    ) { inner ->
        if (exam == null) return@Scaffold
        ExamDetailContent(exam, events, Modifier.padding(inner))
    }

    if (confirmDelete && exam != null) {
        ConfirmDialog(
            title = "Delete exam?",
            message = "\"${exam.subjectName}\" and its chapters and reminders will be permanently deleted.",
            onConfirm = { events.deleteExam(examId); navigator.closeExamDetail() },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun ExamDetailContent(exam: ExamWithDetails, events: EventsViewModel, modifier: Modifier) {
    val now = rememberNow(10_000L)
    val f = LocalFormatters.current
    val settings = LocalAppSettings.current
    val e = exam.exam
    val color = AppColors.subject(exam.subjectColor)
    val past = e.dateTime < now
    val diff = (e.dateTime - now).coerceAtLeast(0)

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(color, MaterialTheme.colorScheme.tertiary)))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(exam.subjectName, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(f.dateTime(e.dateTime), color = Color.White.copy(alpha = 0.9f))
                    if (e.location.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LocationOn, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(e.location, color = Color.White)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (past) {
                        Text("This exam has finished.", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HeroCount(diff / 86_400_000L, "Days", Modifier.weight(1f))
                            HeroCount((diff / 3_600_000L) % 24, "Hours", Modifier.weight(1f))
                            HeroCount((diff / 60_000L) % 60, "Minutes", Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item {
            DetailCard(title = "Chapters to study", icon = Icons.Rounded.Book) {
                val chapters = exam.sortedChapters
                if (chapters.isEmpty()) {
                    Text("No chapters added. Tap edit to add the chapters for this exam.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("${exam.chaptersDone} of ${chapters.size} studied", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { exam.chaptersDone.toFloat() / chapters.size },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = color,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    chapters.forEach { c ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.animateContentSize()) {
                            Checkbox(
                                checked = c.isDone,
                                onCheckedChange = { events.setChapterDone(c.id, it) },
                                colors = CheckboxDefaults.colors(checkedColor = color),
                            )
                            Text(
                                c.title,
                                textDecoration = if (c.isDone) TextDecoration.LineThrough else null,
                                color = if (c.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }

        item {
            DetailCard(title = "Reminders", icon = Icons.Rounded.NotificationsActive) {
                val reminders = exam.reminders.sortedByDescending { ReminderOption.sortKey(it.offsetMinutes) }
                if (reminders.isEmpty()) {
                    Text("No reminders set for this exam.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                reminders.forEach { r ->
                    val at = ReminderOption.triggerTime(e.dateTime, r.offsetMinutes, settings.sameDayHour)
                    val sent = at <= now
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (sent) Icons.Rounded.CheckCircle else Icons.Rounded.Notifications,
                            null,
                            tint = if (sent) AppColors.completed else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(ReminderOption.label(r.offsetMinutes), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                (if (sent) "Sent · " else "") + f.dateTime(at),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        if (e.notes.isNotBlank()) {
            item {
                DetailCard(title = "Notes", icon = Icons.Rounded.Description) { Text(e.notes) }
            }
        }
    }
}

@Composable
private fun HeroCount(value: Long, label: String, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$value", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
    }
}

@Composable
fun DetailCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

// ---------------------------------------------------------------- Exam editor

@Composable
fun ExamEditScreen(
    vm: ExamEditorViewModel,
    subjects: List<SubjectEntity>,
    navigator: AppNavigator,
    events: EventsViewModel,
    examId: Long?,
) {
    val f = LocalFormatters.current
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            TopAppBar(
                title = { Text(if (vm.isNew) "New exam" else "Edit exam") },
                navigationIcon = {
                    IconButton(onClick = navigator::back) { Icon(Icons.Rounded.Close, "Cancel") }
                },
                actions = {
                    if (!vm.isNew) {
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, "Delete") }
                    }
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
            SubjectField(vm.subject, { vm.subject = it }, subjects, vm.subjectError)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PickerField(
                    label = "Exam date *",
                    value = vm.date?.let { f.date(it) },
                    icon = Icons.Rounded.Event,
                    onClick = { showDate = true },
                    isError = vm.dateError,
                    modifier = Modifier.weight(1.4f),
                )
                PickerField(
                    label = "Time *",
                    value = vm.time?.let { f.time(it) },
                    icon = Icons.Rounded.Schedule,
                    onClick = { showTime = true },
                    isError = vm.timeError,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = vm.location,
                onValueChange = { vm.location = it },
                label = { Text("Location (optional)") },
                placeholder = { Text("e.g. Hall B, Room 204") },
                leadingIcon = { Icon(Icons.Rounded.LocationOn, null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionHeader("Chapters") {
                Text("${vm.chapters.count { it.title.isNotBlank() }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            vm.chapters.forEachIndexed { index, row ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = row.title,
                        onValueChange = { vm.updateChapter(row.key, it) },
                        placeholder = { Text("Chapter ${index + 1}") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f),
                    )
                    Column {
                        IconButton(onClick = { vm.moveChapter(row.key, up = true) }, enabled = index > 0, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.KeyboardArrowUp, "Move up")
                        }
                        IconButton(
                            onClick = { vm.moveChapter(row.key, up = false) },
                            enabled = index < vm.chapters.lastIndex,
                            modifier = Modifier.size(32.dp),
                        ) { Icon(Icons.Rounded.KeyboardArrowDown, "Move down") }
                    }
                    IconButton(onClick = { vm.removeChapter(row.key) }) { Icon(Icons.Rounded.Close, "Remove chapter") }
                }
            }
            OutlinedButton(onClick = vm::addChapter, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Add chapter")
            }

            SectionHeader("Remind me")
            ReminderSelector(vm.reminders, { vm.reminders = it })

            OutlinedTextField(
                value = vm.notes,
                onValueChange = { vm.notes = it },
                label = { Text("Notes (optional)") },
                leadingIcon = { Icon(Icons.Rounded.Description, null) },
                minLines = 3,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()
            Button(
                onClick = { vm.save { navigator.back() } },
                enabled = !vm.saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(if (vm.isNew) "Save exam" else "Save changes", style = MaterialTheme.typography.titleMedium)
            }
            if (vm.showErrors && (vm.subjectError || vm.dateError || vm.timeError)) {
                Text("Please fill in the required fields marked *", color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDate) DatePickerModal(vm.date, { vm.date = it }, { showDate = false })
    if (showTime) TimePickerModal(vm.time, { vm.time = it }, { showTime = false })
    if (confirmDelete && examId != null) {
        ConfirmDialog(
            title = "Delete exam?",
            message = "This exam, its chapters and its reminders will be permanently deleted.",
            onConfirm = {
                events.deleteExam(examId)
                navigator.back() // the details screen (if open) closes itself once the exam is gone
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
