package com.taskfinisher.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskfinisher.ui.theme.*
import com.taskfinisher.viewmodel.AddEditTaskViewModel
import java.time.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    editTaskId: String? = null,
    onDismiss: () -> Unit,
    viewModel: AddEditTaskViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Load existing task if editing
    LaunchedEffect(editTaskId) {
        if (editTaskId != null) viewModel.loadTask(editTaskId)
    }

    // Close when saved
    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) { viewModel.reset(); onDismiss() }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // DatePicker state (Material3)
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = state.deadline
            ?: LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = state.deadline?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).hour
        } ?: 9,
        initialMinute = state.deadline?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).minute
        } ?: 0
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Divider)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Header ─────────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (state.isEditMode) "Edit Task" else "New Task",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (state.isEditMode) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, "Delete task", tint = OverdueTint)
                    }
                }
            }

            // ── Title ──────────────────────────────────────────────────────────
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("Title *", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                isError = state.titleError != null,
                supportingText = { state.titleError?.let { Text(it, color = OverdueTint) } },
                singleLine = true,
                colors = taskTextFieldColors(),
                textStyle = MaterialTheme.typography.titleMedium
            )

            // ── Notes ──────────────────────────────────────────────────────────
            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text("Notes (optional)", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                maxLines = 5,
                colors = taskTextFieldColors()
            )

            // ── Deadline ───────────────────────────────────────────────────────
            Column {
                Text("Deadline", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Date button
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = BorderStroke(1.dp, Divider)
                    ) {
                        Icon(Icons.Filled.CalendarMonth, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            state.deadline?.let {
                                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
                                    .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                            } ?: "Set date",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    // Time button (only if date set)
                    if (state.deadline != null) {
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = BorderStroke(1.dp, Divider)
                        ) {
                            Icon(Icons.Filled.Schedule, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                Instant.ofEpochMilli(state.deadline!!)
                                    .atZone(ZoneId.systemDefault())
                                    .format(DateTimeFormatter.ofPattern("h:mm a")),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    // Clear deadline
                    if (state.deadline != null) {
                        IconButton(onClick = { viewModel.updateDeadline(null) }) {
                            Icon(Icons.Filled.Close, "Clear deadline", tint = TextSecondary)
                        }
                    }
                }
            }

            // ── Importance ─────────────────────────────────────────────────────
            ImportancePicker(
                selected = state.importance,
                onSelect = viewModel::updateImportance
            )

            // ── Energy ─────────────────────────────────────────────────────────
            EnergyPicker(
                selected = state.energy,
                onSelect = viewModel::updateEnergy
            )

            // ── Recurrence ─────────────────────────────────────────────────────
            HorizontalDivider(color = Divider)
            RecurrencePicker(
                rule = state.recurrenceRule,
                onRuleChanged = viewModel::updateRecurrenceRule
            )

            // ── Subtasks ───────────────────────────────────────────────────────
            HorizontalDivider(color = Divider)
            SubtaskList(
                subtasks = state.subtasks,
                onAdd = viewModel::addSubtask,
                onRemove = viewModel::removeSubtask,
                onToggle = viewModel::toggleSubtaskComplete
            )

            // ── Action buttons ─────────────────────────────────────────────────
            HorizontalDivider(color = Divider)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = BorderStroke(1.dp, Divider)
                ) { Text("Cancel") }

                Button(
                    onClick = viewModel::save,
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(Modifier.size(16.dp), color = OnAccent, strokeWidth = 2.dp)
                    } else {
                        Text(if (state.isEditMode) "Update" else "Save", color = OnAccent)
                    }
                }
            }
        }
    }

    // ── Date picker dialog ─────────────────────────────────────────────────────
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val ms = datePickerState.selectedDateMillis
                    if (ms != null) {
                        // Merge with existing time or default to 09:00
                        val existingTime = state.deadline?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
                        }
                        val hour = existingTime?.hour ?: 9
                        val minute = existingTime?.minute ?: 0
                        val newDeadline = Instant.ofEpochMilli(ms)
                            .atZone(ZoneId.of("UTC"))
                            .withZoneSameLocal(ZoneId.systemDefault())
                            .toLocalDate()
                            .atTime(hour, minute)
                            .atZone(ZoneId.systemDefault())
                            .toInstant().toEpochMilli()
                        viewModel.updateDeadline(newDeadline)
                    }
                    showDatePicker = false
                }) { Text("OK", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Surface)
        ) {
            DatePicker(state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = Accent,
                    todayDateBorderColor = Accent,
                    containerColor = Surface
                ))
        }
    }

    // ── Time picker dialog ─────────────────────────────────────────────────────
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = Surface,
            confirmButton = {
                TextButton(onClick = {
                    val existing = state.deadline ?: System.currentTimeMillis()
                    val date = Instant.ofEpochMilli(existing).atZone(ZoneId.systemDefault()).toLocalDate()
                    val newDeadline = date.atTime(timePickerState.hour, timePickerState.minute)
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    viewModel.updateDeadline(newDeadline)
                    showTimePicker = false
                }) { Text("OK", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    // ── Delete confirm dialog ─────────────────────────────────────────────────
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Surface,
            title = { Text("Delete Task", color = TextPrimary) },
            text = { Text("This action cannot be undone.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; viewModel.deleteTask() }) {
                    Text("Delete", color = OverdueTint)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun taskTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Accent, unfocusedBorderColor = Divider,
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
    cursorColor = Accent, focusedLabelColor = Accent
)
