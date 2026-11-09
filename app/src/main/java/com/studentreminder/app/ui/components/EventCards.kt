package com.studentreminder.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.studentreminder.app.data.local.AssignmentWithDetails
import com.studentreminder.app.data.local.ExamWithDetails
import com.studentreminder.app.reminders.NotificationHelper
import com.studentreminder.app.ui.AgendaItem
import com.studentreminder.app.ui.theme.AppColors
import com.studentreminder.app.ui.theme.LocalFormatters
import com.studentreminder.app.util.TimeUtils

enum class AssignmentStatus { COMPLETED, OVERDUE, DUE_TODAY, PENDING }

fun AssignmentWithDetails.status(now: Long): AssignmentStatus = when {
    assignment.isCompleted -> AssignmentStatus.COMPLETED
    assignment.dueDateTime < now -> AssignmentStatus.OVERDUE
    TimeUtils.isToday(assignment.dueDateTime) -> AssignmentStatus.DUE_TODAY
    else -> AssignmentStatus.PENDING
}

@Composable
private fun AccentCard(
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    content: @Composable () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (highlighted) 2.dp else 1.dp),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(accent)
            )
            Box(Modifier.padding(16.dp)) { content() }
        }
    }
}

@Composable
private fun InfoLine(icon: ImageVector, text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun ExamCard(exam: ExamWithDetails, now: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val f = LocalFormatters.current
    val e = exam.exam
    val past = e.dateTime < now
    val today = TimeUtils.isToday(e.dateTime)
    val subjectColor = AppColors.subject(exam.subjectColor)
    AccentCard(
        accent = subjectColor,
        onClick = onClick,
        modifier = modifier.alpha(if (past) 0.65f else 1f),
        highlighted = today && !past,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.School, subjectColor, size = 40)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(exam.subjectName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        TimeUtils.relativeDay(e.dateTime),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                when {
                    past -> Pill("Done", Color.Gray)
                    today -> Pill("Today", AppColors.today)
                    else -> Pill(TimeUtils.countdown(e.dateTime, now), AppColors.exam, icon = Icons.Rounded.Schedule)
                }
            }
            Spacer(Modifier.height(2.dp))
            InfoLine(Icons.Rounded.Event, "${f.date(e.dateTime)} · ${f.time(e.dateTime)}")
            if (e.location.isNotBlank()) InfoLine(Icons.Rounded.LocationOn, e.location)
            if (exam.chapters.isNotEmpty()) {
                InfoLine(Icons.Rounded.Book, "${exam.chaptersDone}/${exam.chapters.size} chapters studied")
                LinearProgressIndicator(
                    progress = { exam.chaptersDone.toFloat() / exam.chapters.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = subjectColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

@Composable
fun AssignmentCard(
    item: AssignmentWithDetails,
    now: Long,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val f = LocalFormatters.current
    val a = item.assignment
    val status = item.status(now)
    val accent = when (status) {
        AssignmentStatus.COMPLETED -> AppColors.completed
        AssignmentStatus.OVERDUE -> AppColors.overdue
        else -> AppColors.subject(item.subjectColor)
    }
    AccentCard(
        accent = accent,
        onClick = onClick,
        modifier = modifier.alpha(if (status == AssignmentStatus.COMPLETED) 0.7f else 1f),
        highlighted = status == AssignmentStatus.DUE_TODAY,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            IconButton(onClick = onToggleComplete, modifier = Modifier.size(40.dp)) {
                Icon(
                    if (a.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = if (a.isCompleted) "Mark as not done" else "Mark as done",
                    tint = if (a.isCompleted) AppColors.completed else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    a.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (a.isCompleted) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(item.subjectName, style = MaterialTheme.typography.labelLarge, color = AppColors.subject(item.subjectColor))
                InfoLine(
                    Icons.Rounded.Event,
                    "Due ${f.date(a.dueDateTime)} · ${f.time(a.dueDateTime)}",
                    color = if (status == AssignmentStatus.OVERDUE) AppColors.overdue else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                    when (status) {
                        AssignmentStatus.COMPLETED -> Pill("Completed", AppColors.completed, icon = Icons.Rounded.CheckCircle)
                        AssignmentStatus.OVERDUE -> Pill("Overdue", AppColors.overdue, icon = Icons.Rounded.Warning)
                        AssignmentStatus.DUE_TODAY -> Pill("Due today", AppColors.today)
                        AssignmentStatus.PENDING -> Pill(TimeUtils.countdown(a.dueDateTime, now), AppColors.assignment, icon = Icons.Rounded.Schedule)
                    }
                    Pill(NotificationHelper.priorityLabel(a.priority), AppColors.priority(a.priority))
                }
            }
        }
    }
}

/** Compact timeline row used on the dashboard, calendar and search. */
@Composable
fun AgendaRow(item: AgendaItem, now: Long, onClick: () -> Unit, modifier: Modifier = Modifier, showDate: Boolean = true) {
    val f = LocalFormatters.current
    val (title, subtitle, color, icon) = when (item) {
        is AgendaItem.Exam -> Quad(
            item.data.subjectName,
            buildString {
                append("Exam")
                if (item.data.exam.location.isNotBlank()) append(" · ${item.data.exam.location}")
            },
            AppColors.exam,
            Icons.Rounded.School,
        )
        is AgendaItem.Assignment -> Quad(
            item.data.assignment.title,
            "Assignment · ${item.data.subjectName}",
            AppColors.assignment,
            Icons.AutoMirrored.Rounded.Assignment,
        )
    }
    val past = item.time < now
    val done = item is AgendaItem.Assignment && item.data.assignment.isCompleted
    val overdue = item is AgendaItem.Assignment && !done && past
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .alpha(if (done || (past && !overdue)) 0.6f else 1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(icon, color, size = 40)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(f.time(item.time), style = MaterialTheme.typography.labelLarge)
                when {
                    done -> Text("Done", style = MaterialTheme.typography.labelSmall, color = AppColors.completed)
                    overdue -> Text("Overdue", style = MaterialTheme.typography.labelSmall, color = AppColors.overdue)
                    showDate -> Text(
                        if (TimeUtils.daysUntil(item.time) in -1L..1L) TimeUtils.relativeDay(item.time) else f.shortDate(item.time),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (TimeUtils.isToday(item.time)) AppColors.today else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private data class Quad(val a: String, val b: String, val c: Color, val d: ImageVector)
