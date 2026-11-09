@file:OptIn(ExperimentalMaterial3Api::class)

package com.studentreminder.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.studentreminder.app.ui.AgendaItem
import com.studentreminder.app.ui.AppNavigator
import com.studentreminder.app.ui.EventsState
import com.studentreminder.app.ui.components.AgendaRow
import com.studentreminder.app.ui.components.EmptyState
import com.studentreminder.app.ui.components.rememberNow

private enum class TypeFilter(val label: String) { ALL("All"), EXAMS("Exams"), ASSIGNMENTS("Assignments") }
private enum class StatusFilter(val label: String) { ANY("Any status"), UPCOMING("Upcoming"), PAST("Past"), PENDING("Pending"), COMPLETED("Completed"), OVERDUE("Overdue") }

@Composable
fun SearchScreen(state: EventsState, navigator: AppNavigator) {
    val now = rememberNow()
    var query by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(TypeFilter.ALL) }
    var status by rememberSaveable { mutableStateOf(StatusFilter.ANY) }
    var subject by rememberSaveable { mutableStateOf<Long?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    val q = query.trim()
    val results = state.agenda.filter { item ->
        val typeOk = when (type) {
            TypeFilter.ALL -> true
            TypeFilter.EXAMS -> item is AgendaItem.Exam
            TypeFilter.ASSIGNMENTS -> item is AgendaItem.Assignment
        }
        val completed = item is AgendaItem.Assignment && item.data.assignment.isCompleted
        val statusOk = when (status) {
            StatusFilter.ANY -> true
            StatusFilter.UPCOMING -> item.time >= now && !completed
            StatusFilter.PAST -> item.time < now
            StatusFilter.PENDING -> item is AgendaItem.Assignment && !completed
            StatusFilter.COMPLETED -> completed
            StatusFilter.OVERDUE -> item is AgendaItem.Assignment && !completed && item.time < now
        }
        val subjectId = when (item) {
            is AgendaItem.Exam -> item.data.exam.subjectId
            is AgendaItem.Assignment -> item.data.assignment.subjectId
        }
        val subjectOk = subject == null || subjectId == subject
        val textOk = q.isEmpty() || when (item) {
            is AgendaItem.Exam -> listOf(
                item.data.subjectName, item.data.exam.location, item.data.exam.notes,
            ).plus(item.data.chapters.map { it.title }).any { it.contains(q, ignoreCase = true) }
            is AgendaItem.Assignment -> listOf(
                item.data.assignment.title, item.data.subjectName, item.data.assignment.description,
            ).any { it.contains(q, ignoreCase = true) }
        }
        typeOk && statusOk && subjectOk && textOk
    }
    // Upcoming first (nearest), then past (most recent first).
    val sorted = results.filter { it.time >= now }.sortedBy { it.time } + results.filter { it.time < now }.sortedByDescending { it.time }

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = navigator::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                title = {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search subjects, titles, chapters…") },
                        singleLine = true,
                        trailingIcon = {
                            if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Clear") }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus),
                    )
                },
            )
        },
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .imePadding()
        ) {
            ChipRow(TypeFilter.entries.map { it to it.label }, type) { type = it }
            ChipRow(StatusFilter.entries.map { it to it.label }, status) { status = it }
            if (state.subjects.isNotEmpty()) SubjectFilterRow(state.subjects, subject) { subject = it }

            if (sorted.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.SearchOff,
                    title = "No results",
                    message = if (state.agenda.isEmpty()) "You haven't added any exams or assignments yet."
                    else "Try a different word or clear some filters.",
                )
            } else {
                Text(
                    "${sorted.size} result" + if (sorted.size == 1) "" else "s",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(sorted, key = { it.key }) { item ->
                        AgendaRow(item, now, onClick = { navigator.open(item) }, modifier = Modifier.animateItem())
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> ChipRow(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options) { (value, label) ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(label) },
                shape = RoundedCornerShape(12.dp),
            )
        }
    }
}
