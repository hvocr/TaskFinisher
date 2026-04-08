package com.taskfinisher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskfinisher.data.model.Task
import com.taskfinisher.ui.theme.*
import com.taskfinisher.utils.formatDate
import com.taskfinisher.viewmodel.ArchiveFilter
import com.taskfinisher.viewmodel.ArchiveViewModel

@Composable
fun ArchiveScreen(viewModel: ArchiveViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // Clear archive confirmation
    if (state.showClearConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissClearConfirm,
            containerColor = Surface,
            title = { Text("Clear Archive?", color = TextPrimary) },
            text = { Text("This will permanently delete all archived and completed tasks. This cannot be undone.",
                color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = viewModel::clearArchive) {
                    Text("Clear All", color = OverdueTint)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissClearConfirm) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data, containerColor = SurfaceVariant, contentColor = TextPrimary)
            }
        },
        containerColor = Background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // ── Header ─────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Archive", style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary, modifier = Modifier.weight(1f))
                if (state.tasks.isNotEmpty()) {
                    OutlinedButton(
                        onClick = viewModel::requestClearArchive,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OverdueTint),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OverdueTint.copy(alpha = 0.4f))
                    ) {
                        Icon(Icons.Filled.DeleteSweep, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Clear", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // ── Filter pills ───────────────────────────────────────────────────
            ArchiveFilterRow(activeFilter = state.filter, onSelect = viewModel::setFilter)

            Spacer(Modifier.height(4.dp))

            // ── Task count ─────────────────────────────────────────────────────
            Text(
                "${state.tasks.size} completed task${if (state.tasks.size != 1) "s" else ""}",
                style = MaterialTheme.typography.bodyMedium, color = TextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            // ── List ───────────────────────────────────────────────────────────
            if (state.tasks.isEmpty()) {
                ArchiveEmptyState()
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                    items(state.tasks, key = { it.id }) { task ->
                        ArchivedTaskRow(
                            task = task,
                            onUnarchive = { viewModel.unarchiveTask(task) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveFilterRow(activeFilter: ArchiveFilter, onSelect: (ArchiveFilter) -> Unit) {
    val options = listOf(
        ArchiveFilter.ALL to "All",
        ArchiveFilter.THIS_WEEK to "This Week",
        ArchiveFilter.THIS_MONTH to "This Month"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (filter, label) ->
            val selected = activeFilter == filter
            FilterChip(
                selected = selected,
                onClick = { onSelect(filter) },
                label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Accent.copy(alpha = 0.2f),
                    selectedLabelColor = Accent,
                    containerColor = SurfaceVariant,
                    labelColor = TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true, selected = selected,
                    selectedBorderColor = Accent, borderColor = Divider
                )
            )
        }
    }
}

@Composable
private fun ArchivedTaskRow(task: Task, onUnarchive: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Completed check indicator
        Icon(Icons.Filled.CheckCircle, null, tint = AccentTeal, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))

        Column(Modifier.weight(1f)) {
            Text(task.title, style = MaterialTheme.typography.titleMedium, color = TextSecondary)
            task.completedAt?.let {
                Text("Completed ${it.formatDate()}", style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary.copy(alpha = 0.6f))
            }
        }

        // Unarchive button
        IconButton(onClick = onUnarchive, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Unarchive, "Restore task", tint = Accent, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ArchiveEmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🗃️", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text("Archive is empty", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text("Completed and archived tasks will appear here.",
            style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}
