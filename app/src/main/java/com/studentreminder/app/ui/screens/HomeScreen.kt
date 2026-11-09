package com.studentreminder.app.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.studentreminder.app.data.AppSettings
import com.studentreminder.app.data.local.AssignmentWithDetails
import com.studentreminder.app.data.local.ExamWithDetails
import com.studentreminder.app.reminders.NotificationHelper
import com.studentreminder.app.ui.AgendaItem
import com.studentreminder.app.ui.AppNavigator
import com.studentreminder.app.ui.EventsState
import com.studentreminder.app.ui.EventsViewModel
import com.studentreminder.app.ui.components.AddEventFab
import com.studentreminder.app.ui.components.AgendaRow
import com.studentreminder.app.ui.components.AssignmentCard
import com.studentreminder.app.ui.components.EmptyState
import com.studentreminder.app.ui.components.Pill
import com.studentreminder.app.ui.components.SectionHeader
import com.studentreminder.app.ui.components.rememberNow
import com.studentreminder.app.ui.theme.AppColors
import com.studentreminder.app.ui.theme.LocalFormatters
import com.studentreminder.app.util.TimeUtils
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun HomeScreen(state: EventsState, settings: AppSettings, navigator: AppNavigator, events: EventsViewModel) {
    val now = rememberNow()
    val upcomingExams = state.exams.filter { it.exam.dateTime >= now }
    val pending = state.assignments.filter { !it.assignment.isCompleted }
    val overdue = pending.filter { it.assignment.dueDateTime < now }.sortedBy { it.assignment.dueDateTime }
    val upcomingPending = pending.filter { it.assignment.dueDateTime >= now }
    val completed = state.assignments.count { it.assignment.isCompleted }
    val nextExam = upcomingExams.minByOrNull { it.exam.dateTime }
    val nextAssignment = upcomingPending.minByOrNull { it.assignment.dueDateTime }
    val today = LocalDate.now()
    val todayItems = state.agenda.filter {
        TimeUtils.toLocalDate(it.time) == today &&
            !(it is AgendaItem.Assignment && it.data.assignment.isCompleted)
    }
    val upcoming = state.agenda.filter { item ->
        item.time >= now && TimeUtils.toLocalDate(item.time) != today &&
            !(item is AgendaItem.Assignment && item.data.assignment.isCompleted)
    }.take(12)

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            AddEventFab(onAddExam = { navigator.addExam() }, onAddAssignment = { navigator.addAssignment() })
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item(key = "header") {
                DashboardHeader(
                    name = settings.userName,
                    upcomingExams = upcomingExams.size,
                    pending = pending.size,
                    completed = completed,
                    overdue = overdue.size,
                    onSearch = navigator::search,
                    onExams = { navigator.topLevel(com.studentreminder.app.ui.Routes.EXAMS) },
                    onAssignments = { navigator.topLevel(com.studentreminder.app.ui.Routes.ASSIGNMENTS) },
                )
            }

            if (state.loaded && state.exams.isEmpty() && state.assignments.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Welcome to Student Reminder",
                        message = "Add your first exam or assignment and we'll remind you before it's due — even offline.",
                        action = {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = { navigator.addExam() }) { Text("Add exam") }
                                FilledTonalButton(onClick = { navigator.addAssignment() }) { Text("Add assignment") }
                            }
                        },
                    )
                }
                return@LazyColumn
            }

            if (todayItems.isNotEmpty()) {
                item(key = "today-h") {
                    SectionHeader("Today · ${todayItems.size}", Modifier.padding(horizontal = 20.dp)) {
                        Pill("Focus", AppColors.today, icon = Icons.Rounded.Today)
                    }
                }
                items(todayItems, key = { "today-" + it.key }) { item ->
                    AgendaRow(
                        item = item, now = now, onClick = { navigator.open(item) }, showDate = false,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .animateItem(),
                    )
                }
            }

            item(key = "next-exam-h") { SectionHeader("Next exam", Modifier.padding(horizontal = 20.dp)) }
            item(key = "next-exam") {
                if (nextExam != null) {
                    NextExamCard(nextExam, now, onClick = { navigator.openExam(nextExam.exam.id) })
                } else {
                    MiniEmpty("No upcoming exams", "Add exam") { navigator.addExam() }
                }
            }

            item(key = "next-asg-h") { SectionHeader("Next deadline", Modifier.padding(horizontal = 20.dp)) }
            item(key = "next-asg") {
                if (nextAssignment != null) {
                    NextAssignmentCard(nextAssignment, now, onClick = { navigator.openAssignment(nextAssignment.assignment.id) })
                } else {
                    MiniEmpty("No pending assignments 🎉", "Add assignment") { navigator.addAssignment() }
                }
            }

            if (overdue.isNotEmpty()) {
                item(key = "overdue-h") {
                    SectionHeader("Overdue · ${overdue.size}", Modifier.padding(horizontal = 20.dp)) {
                        Pill("Needs attention", AppColors.overdue, icon = Icons.Rounded.Warning)
                    }
                }
                items(overdue, key = { "overdue-" + it.assignment.id }) { a ->
                    AssignmentCard(
                        item = a, now = now,
                        onToggleComplete = { events.setAssignmentCompleted(a.assignment.id, true) },
                        onClick = { navigator.openAssignment(a.assignment.id) },
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .animateItem(),
                    )
                }
            }

            item(key = "upcoming-h") { SectionHeader("Upcoming", Modifier.padding(horizontal = 20.dp)) }
            if (upcoming.isEmpty()) {
                item(key = "upcoming-empty") {
                    Text(
                        "Nothing else coming up. Enjoy the calm!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }
            items(upcoming, key = { "up-" + it.key }) { item ->
                AgendaRow(
                    item = item, now = now, onClick = { navigator.open(item) },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .animateItem(),
                )
            }
        }
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Hello"
}

@Composable
private fun DashboardHeader(
    name: String,
    upcomingExams: Int,
    pending: Int,
    completed: Int,
    overdue: Int,
    onSearch: () -> Unit,
    onExams: () -> Unit,
    onAssignments: () -> Unit,
) {
    val f = LocalFormatters.current
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.linearGradient(listOf(scheme.primary, scheme.tertiary)))
    ) {
        Column(
            Modifier
                .statusBarsPadding()
                .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        f.longDate(LocalDate.now()),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                    Text(
                        greeting() + (if (name.isNotBlank()) ", ${name.trim()}" else "") + " 👋",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onSearch) {
                    Icon(Icons.Rounded.Search, contentDescription = "Search", tint = Color.White)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.padding(end = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(upcomingExams, "Upcoming\nexams", Icons.Rounded.School, Modifier.weight(1f), onExams)
                StatTile(pending, "Pending\ntasks", Icons.Rounded.HourglassBottom, Modifier.weight(1f), onAssignments)
                StatTile(completed, "Completed\ntasks", Icons.Rounded.CheckCircle, Modifier.weight(1f), onAssignments)
                StatTile(overdue, "Overdue\ntasks", Icons.Rounded.Warning, Modifier.weight(1f), onAssignments, alert = overdue > 0)
            }
        }
    }
}

@Composable
private fun StatTile(
    value: Int,
    label: String,
    icon: ImageVector,
    modifier: Modifier,
    onClick: () -> Unit,
    alert: Boolean = false,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alert) Color(0x55EF4444) else Color.White.copy(alpha = 0.16f),
            contentColor = Color.White,
        ),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 12.dp)) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = Color.White.copy(alpha = 0.9f))
            Spacer(Modifier.height(6.dp))
            Text("$value", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f), maxLines = 2)
        }
    }
}

@Composable
private fun NextExamCard(exam: ExamWithDetails, now: Long, onClick: () -> Unit) {
    val f = LocalFormatters.current
    val color = AppColors.subject(exam.subjectColor)
    val diff = (exam.exam.dateTime - now).coerceAtLeast(0)
    val days = diff / 86_400_000L
    val hours = (diff / 3_600_000L) % 24
    val minutes = (diff / 60_000L) % 60
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    exam.subjectName, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (TimeUtils.isToday(exam.exam.dateTime)) Pill("Today", AppColors.today)
            }
            Text(
                "${f.date(exam.exam.dateTime)} · ${f.time(exam.exam.dateTime)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (exam.exam.location.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LocationOn, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(exam.exam.location, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CountdownBox(days, "Days", color, Modifier.weight(1f))
                CountdownBox(hours, "Hours", color, Modifier.weight(1f))
                CountdownBox(minutes, "Minutes", color, Modifier.weight(1f))
            }
            if (exam.chapters.isNotEmpty()) {
                Text(
                    "${exam.chaptersDone} of ${exam.chapters.size} chapters studied",
                    style = MaterialTheme.typography.labelLarge,
                )
                LinearProgressIndicator(
                    progress = { exam.chaptersDone.toFloat() / exam.chapters.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

@Composable
fun CountdownBox(value: Long, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0.08f))))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$value", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NextAssignmentCard(a: AssignmentWithDetails, now: Long, onClick: () -> Unit) {
    val f = LocalFormatters.current
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppColors.assignment.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Rounded.Assignment, null, tint = AppColors.assignment)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(a.assignment.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(a.subjectName, style = MaterialTheme.typography.labelLarge, color = AppColors.subject(a.subjectColor))
                Text(
                    "Due ${f.date(a.assignment.dueDateTime)} · ${f.time(a.assignment.dueDateTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    TimeUtils.countdown(a.assignment.dueDateTime, now),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Pill(NotificationHelper.priorityLabel(a.assignment.priority), AppColors.priority(a.assignment.priority))
            }
        }
    }
}

@Composable
private fun MiniEmpty(text: String, action: String, onAction: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.EventAvailable, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}
