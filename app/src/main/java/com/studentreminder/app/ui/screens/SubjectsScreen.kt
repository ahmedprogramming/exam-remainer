@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.studentreminder.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studentreminder.app.data.local.SubjectEntity
import com.studentreminder.app.data.local.SubjectWithUsage
import com.studentreminder.app.ui.AppNavigator
import com.studentreminder.app.ui.EventsViewModel
import com.studentreminder.app.ui.components.ConfirmDialog
import com.studentreminder.app.ui.components.EmptyState
import com.studentreminder.app.ui.theme.AppColors
import kotlinx.coroutines.launch

@Composable
fun SubjectsScreen(events: EventsViewModel, navigator: AppNavigator) {
    val subjects by events.subjectsWithUsage.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<SubjectEntity?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<SubjectWithUsage?>(null) }

    fun report(r: Result<Unit>, ok: String? = null) {
        r.exceptionOrNull()?.let { e -> scope.launch { snackbar.showSnackbar(e.message ?: "Something went wrong") } }
            ?: ok?.let { scope.launch { snackbar.showSnackbar(it) } }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Subjects") },
                navigationIcon = { IconButton(onClick = navigator::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text("New subject") },
            )
        },
    ) { inner ->
        if (subjects.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Category,
                title = "No subjects yet",
                message = "Subjects are created automatically when you add an exam or assignment, or you can add them here.",
                modifier = Modifier.padding(inner),
            )
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(subjects, key = { it.subject.id }) { s ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier.animateItem(),
                    ) {
                        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.subject(s.subject.colorIndex))
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.subject.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${s.examCount} exam(s) · ${s.assignmentCount} assignment(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { editing = s.subject }) { Icon(Icons.Rounded.Edit, "Edit") }
                            IconButton(onClick = { deleting = s }) { Icon(Icons.Rounded.Delete, "Delete") }
                        }
                    }
                }
            }
        }
    }

    if (adding) {
        SubjectDialog(
            initial = null,
            onDismiss = { adding = false },
            onSave = { name, _ -> events.addSubject(name) { report(it, "Subject added") } },
        )
    }
    editing?.let { subject ->
        SubjectDialog(
            initial = subject,
            onDismiss = { editing = null },
            onSave = { name, color -> events.updateSubject(subject.copy(name = name, colorIndex = color)) { report(it) } },
        )
    }
    deleting?.let { s ->
        val used = s.examCount + s.assignmentCount
        if (used > 0) {
            AlertDialog(
                onDismissRequest = { deleting = null },
                title = { Text("Can't delete \"${s.subject.name}\"") },
                text = { Text("It is used by $used exam(s)/assignment(s). Delete those first, or rename this subject instead.") },
                confirmButton = { TextButton(onClick = { deleting = null }) { Text("OK") } },
            )
        } else {
            ConfirmDialog(
                title = "Delete subject?",
                message = "\"${s.subject.name}\" will be removed.",
                onConfirm = { events.deleteSubject(s.subject.id) { report(it) } },
                onDismiss = { deleting = null },
            )
        }
    }
}

@Composable
private fun SubjectDialog(initial: SubjectEntity?, onDismiss: () -> Unit, onSave: (String, Int) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var color by remember { mutableIntStateOf(initial?.colorIndex ?: 0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New subject" else "Edit subject") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (initial != null) {
                    Text("Color", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AppColors.subjectPalette.forEachIndexed { i, c ->
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .then(if (color == i) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                    .clickable { color = i },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (color == i) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(name.trim(), color); onDismiss() }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
