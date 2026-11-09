@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.studentreminder.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.studentreminder.app.data.ReminderOption
import com.studentreminder.app.data.local.SubjectEntity
import com.studentreminder.app.ui.theme.AppColors
import com.studentreminder.app.ui.theme.LocalAppSettings
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** Tappable field that shows a value and opens a picker. */
@Composable
fun PickerField(
    label: String,
    value: String?,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    placeholder: String = "Select",
) {
    val borderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, borderColor),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    value ?: placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
fun DatePickerModal(initial: LocalDate?, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (initial ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
    }
}

@Composable
fun TimePickerModal(initial: LocalTime?, onPick: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val t = initial ?: LocalTime.of(9, 0)
    val state = rememberTimePickerState(t.hour, t.minute, is24Hour = LocalAppSettings.current.use24h)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select time") },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onPick(LocalTime.of(state.hour, state.minute)); onDismiss() }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Free-text subject with suggestions from existing subjects (so they can be reused). */
@Composable
fun SubjectField(
    value: String,
    onValueChange: (String) -> Unit,
    subjects: List<SubjectEntity>,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val suggestions = subjects.filter { value.isBlank() || it.name.contains(value.trim(), ignoreCase = true) }
        .filterNot { it.name.equals(value.trim(), ignoreCase = true) }
    ExposedDropdownMenuBox(
        expanded = expanded && suggestions.isNotEmpty(),
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it); expanded = true },
            label = { Text("Subject *") },
            placeholder = { Text("e.g. Database Systems") },
            singleLine = true,
            isError = isError,
            supportingText = if (isError) ({ Text("Please enter a subject") }) else null,
            trailingIcon = {
                if (subjects.isNotEmpty()) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && suggestions.isNotEmpty())
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded && suggestions.isNotEmpty(),
            onDismissRequest = { expanded = false },
        ) {
            suggestions.forEach { s ->
                DropdownMenuItem(
                    text = { Text(s.name) },
                    leadingIcon = {
                        Surface(shape = RoundedCornerShape(50), color = AppColors.subject(s.colorIndex), modifier = Modifier.size(12.dp)) {}
                    },
                    onClick = { onValueChange(s.name); expanded = false },
                )
            }
        }
    }
}

/** Multi-select reminder chips with presets + custom offsets. */
@Composable
fun ReminderSelector(selected: Set<Long>, onChange: (Set<Long>) -> Unit, modifier: Modifier = Modifier) {
    var showCustom by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ReminderOption.presets.forEach { p ->
                val on = p.minutes in selected
                FilterChip(
                    selected = on,
                    onClick = { onChange(if (on) selected - p.minutes else selected + p.minutes) },
                    label = { Text(p.label) },
                    leadingIcon = if (on) ({ Icon(Icons.Rounded.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }) else null,
                )
            }
            selected.filterNot { ReminderOption.isPreset(it) }.sortedDescending().forEach { m ->
                InputChip(
                    selected = true,
                    onClick = { onChange(selected - m) },
                    label = { Text(ReminderOption.label(m)) },
                    trailingIcon = { Icon(Icons.Rounded.Close, "Remove", Modifier.size(16.dp)) },
                )
            }
            AssistChip(
                onClick = { showCustom = true },
                label = { Text("Custom…") },
                leadingIcon = { Icon(Icons.Rounded.Tune, null, Modifier.size(18.dp)) },
            )
        }
        if (selected.isEmpty()) {
            Text(
                "No reminders — you won't get a notification for this.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
    if (showCustom) {
        CustomReminderDialog(
            onAdd = { onChange(selected + it) },
            onDismiss = { showCustom = false },
        )
    }
}

@Composable
private fun CustomReminderDialog(onAdd: (Long) -> Unit, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf("1") }
    var unit by remember { mutableIntStateOf(0) } // 0 days, 1 hours, 2 minutes
    val units = listOf("Days", "Hours", "Minutes")
    val n = amount.toLongOrNull()
    val valid = n != null && n in 1L..525_600L
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { v -> amount = v.filter { it.isDigit() }.take(5) },
                    label = { Text("How long before?") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !valid,
                    modifier = Modifier.fillMaxWidth(),
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    units.forEachIndexed { i, label ->
                        SegmentedButton(
                            selected = unit == i,
                            onClick = { unit = i },
                            shape = SegmentedButtonDefaults.itemShape(i, units.size),
                        ) { Text(label) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    val minutes = when (unit) {
                        0 -> n!! * 24 * 60
                        1 -> n!! * 60
                        else -> n!!
                    }
                    onAdd(minutes)
                    onDismiss()
                },
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun PrioritySelector(priority: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val labels = listOf("Low", "Medium", "High")
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        labels.forEachIndexed { i, label ->
            SegmentedButton(
                selected = priority == i,
                onClick = { onChange(i) },
                shape = SegmentedButtonDefaults.itemShape(i, labels.size),
                icon = {
                    Icon(Icons.Rounded.Flag, null, tint = AppColors.priority(i), modifier = Modifier.size(18.dp))
                },
                colors = SegmentedButtonDefaults.colors(),
            ) { Text(label) }
        }
    }
}
