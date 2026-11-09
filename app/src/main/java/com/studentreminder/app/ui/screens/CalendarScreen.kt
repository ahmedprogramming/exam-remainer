@file:OptIn(ExperimentalMaterial3Api::class)

package com.studentreminder.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.studentreminder.app.ui.AgendaItem
import com.studentreminder.app.ui.AppNavigator
import com.studentreminder.app.ui.EventsState
import com.studentreminder.app.ui.components.AddEventFab
import com.studentreminder.app.ui.components.AgendaRow
import com.studentreminder.app.ui.components.rememberNow
import com.studentreminder.app.ui.theme.AppColors
import com.studentreminder.app.ui.theme.LocalAppSettings
import com.studentreminder.app.ui.theme.LocalFormatters
import com.studentreminder.app.util.TimeUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(state: EventsState, navigator: AppNavigator) {
    val now = rememberNow()
    val f = LocalFormatters.current
    val firstDay = LocalAppSettings.current.firstDayOfWeek
    var selectedEpochDay by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    val selected = LocalDate.ofEpochDay(selectedEpochDay)
    var monthIndex by rememberSaveable { mutableStateOf(YearMonth.now().let { it.year * 12 + it.monthValue - 1 }) }
    val month = YearMonth.of(monthIndex / 12, monthIndex % 12 + 1)

    // date -> items on that date
    val byDate = remember(state) { state.agenda.groupBy { TimeUtils.toLocalDate(it.time) } }
    val dayItems = byDate[selected].orEmpty().sortedBy { it.time }

    fun goTo(date: LocalDate) {
        selectedEpochDay = date.toEpochDay()
        monthIndex = date.year * 12 + date.monthValue - 1
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                actions = {
                    TextButton(onClick = { goTo(LocalDate.now()) }) { Text("Today") }
                    IconButton(onClick = navigator::search) { Icon(Icons.Rounded.Search, "Search") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        floatingActionButton = {
            AddEventFab(
                onAddExam = { navigator.addExam(selected) },
                onAddAssignment = { navigator.addAssignment(selected) },
            )
        },
    ) { inner ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "calendar") {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(1.dp),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { monthIndex-- }) {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Previous month")
                            }
                            Text(
                                f.month(month.atDay(1)),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { monthIndex++ }) {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Next month")
                            }
                        }
                        WeekdayHeader(firstDay)
                        AnimatedContent(
                            targetState = monthIndex,
                            transitionSpec = {
                                val forward = targetState > initialState
                                (slideInHorizontally { if (forward) it else -it }) togetherWith
                                    (slideOutHorizontally { if (forward) -it else it })
                            },
                            label = "month",
                        ) { idx ->
                            val ym = YearMonth.of(idx / 12, idx % 12 + 1)
                            MonthGrid(
                                month = ym,
                                firstDay = firstDay,
                                selected = selected,
                                byDate = byDate,
                                onSelect = { selectedEpochDay = it.toEpochDay() },
                                onSwipe = { forward -> if (forward) monthIndex++ else monthIndex-- },
                            )
                        }
                        Legend()
                    }
                }
            }

            item(key = "day-header") {
                Row(Modifier.padding(top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(f.longDate(selected), style = MaterialTheme.typography.titleMedium)
                        Text(
                            when (dayItems.size) {
                                0 -> "Nothing scheduled"
                                1 -> "1 event"
                                else -> "${dayItems.size} events"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (selected == LocalDate.now()) {
                        Text("Today", color = AppColors.today, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            if (dayItems.isEmpty()) {
                item(key = "day-empty") {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.EventAvailable, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Text("A free day! Add something for this date:")
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssistChip(onClick = { navigator.addExam(selected) }, label = { Text("+ Exam") })
                                AssistChip(onClick = { navigator.addAssignment(selected) }, label = { Text("+ Assignment") })
                            }
                        }
                    }
                }
            }
            items(dayItems, key = { "day-" + it.key }) { item ->
                AgendaRow(item, now, onClick = { navigator.open(item) }, showDate = false, modifier = Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun WeekdayHeader(firstDay: DayOfWeek) {
    Row(Modifier.padding(vertical = 6.dp)) {
        repeat(7) { i ->
            val d = firstDay.plus(i.toLong())
            Text(
                d.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = if (d == DayOfWeek.FRIDAY || d == DayOfWeek.SATURDAY)
                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    firstDay: DayOfWeek,
    selected: LocalDate,
    byDate: Map<LocalDate, List<AgendaItem>>,
    onSelect: (LocalDate) -> Unit,
    onSwipe: (forward: Boolean) -> Unit,
) {
    val first = month.atDay(1)
    val leading = (first.dayOfWeek.value - firstDay.value + 7) % 7
    val start = first.minusDays(leading.toLong())
    val today = LocalDate.now()
    var dragTotal by remember { mutableStateOf(0f) }

    Column(
        Modifier.pointerInput(month) {
            detectHorizontalDragGestures(
                onDragStart = { dragTotal = 0f },
                onDragEnd = {
                    if (dragTotal < -120f) onSwipe(true) else if (dragTotal > 120f) onSwipe(false)
                    dragTotal = 0f
                },
                onHorizontalDrag = { _, amount -> dragTotal += amount },
            )
        }
    ) {
        repeat(6) { week ->
            Row {
                repeat(7) { d ->
                    val date = start.plusDays((week * 7 + d).toLong())
                    DayCell(
                        date = date,
                        inMonth = date.month == month.month,
                        isToday = date == today,
                        isSelected = date == selected,
                        items = byDate[date].orEmpty(),
                        onClick = { onSelect(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    items: List<AgendaItem>,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val hasExam = items.any { it is AgendaItem.Exam }
    val hasAssignment = items.any { it is AgendaItem.Assignment && !it.data.assignment.isCompleted }
    val hasDone = items.any { it is AgendaItem.Assignment && it.data.assignment.isCompleted }
    Column(
        modifier
            .aspectRatio(0.9f)
            .padding(2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    isSelected -> scheme.primary
                    hasExam && inMonth -> AppColors.exam.copy(alpha = 0.10f)
                    else -> Color.Transparent
                }
            )
            .then(if (isToday && !isSelected) Modifier.border(1.5.dp, scheme.primary, RoundedCornerShape(14.dp)) else Modifier)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "${date.dayOfMonth}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isSelected -> scheme.onPrimary
                !inMonth -> scheme.onSurfaceVariant.copy(alpha = 0.4f)
                else -> scheme.onSurface
            },
        )
        Spacer(Modifier.height(3.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.height(6.dp)) {
            if (hasExam) Dot(if (isSelected) Color.White else AppColors.exam)
            if (hasAssignment) Dot(if (isSelected) Color.White else AppColors.assignment)
            if (hasDone && !hasAssignment) Dot(if (isSelected) Color.White else AppColors.completed)
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun Legend() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(AppColors.exam, "Exam")
        Spacer(Modifier.width(16.dp))
        LegendItem(AppColors.assignment, "Assignment")
        Spacer(Modifier.width(16.dp))
        LegendItem(AppColors.completed, "Completed")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Dot(color)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
