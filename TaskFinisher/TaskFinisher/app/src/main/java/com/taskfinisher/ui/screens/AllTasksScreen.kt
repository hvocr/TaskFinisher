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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskfinisher.ui.components.*
import com.taskfinisher.ui.theme.*
import com.taskfinisher.viewmodel.AllTasksViewModel
import com.taskfinisher.viewmodel.TaskSort

@Composable
fun AllTasksScreen(
    onEditTask: (String) -> Unit,
    viewModel: AllTasksViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()
    val activeSort by viewModel.activeSort.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSortMenu by remember { mutableStateOf(false) }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data, containerColor = SurfaceVariant, contentColor = TextPrimary)
            }
        },
        containerColor = Background
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Search bar ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search tasks…", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = TextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Filled.Close, null, tint = TextSecondary)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent, unfocusedBorderColor = Divider,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        cursorColor = Accent, unfocusedContainerColor = SurfaceVariant,
                        focusedContainerColor = SurfaceVariant
                    )
                )
                Spacer(Modifier.width(8.dp))
                // Sort dropdown button
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .background(SurfaceVariant, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Filled.Sort, "Sort", tint = TextPrimary)
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(SurfaceVariant)
                    ) {
                        listOf(
                            TaskSort.DEADLINE to "By Deadline",
                            TaskSort.IMPORTANCE to "By Importance",
                            TaskSort.DATE_CREATED to "By Date Created"
                        ).forEach { (sort, label) ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (activeSort == sort)
                                            Icon(Icons.Filled.Check, null, tint = Accent, modifier = Modifier.size(16.dp))
                                        else Spacer(Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(label, color = TextPrimary)
                                    }
                                },
                                onClick = {
                                    viewModel.activeSort.value = sort
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // ── Filter chips ───────────────────────────────────────────────────
            FilterBar(
                activeFilter = activeFilter,
                onFilterSelect = { viewModel.activeFilter.value = it }
            )

            Spacer(Modifier.height(8.dp))

            // ── Task count ─────────────────────────────────────────────────────
            Text(
                "${state.tasks.size} task${if (state.tasks.size != 1) "s" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            // ── Task list ──────────────────────────────────────────────────────
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }
            } else if (state.tasks.isEmpty()) {
                AllTasksEmptyState(hasFilter = activeFilter.name != "ALL" || searchQuery.isNotEmpty())
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                    items(state.tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onComplete = viewModel::completeTask,
                            onBig3Toggle = viewModel::toggleBig3,
                            onEdit = { onEditTask(it.id) },
                            onDelete = viewModel::deleteTask,
                            onArchive = viewModel::archiveTask,
                            onPushToTomorrow = viewModel::pushToTomorrow,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .animateItem()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AllTasksEmptyState(hasFilter: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(if (hasFilter) "🔍" else "📭", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            if (hasFilter) "No tasks match your filter" else "No tasks yet",
            style = MaterialTheme.typography.titleMedium, color = TextPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (hasFilter) "Try a different filter or search term"
            else "Tap + to create your first task",
            style = MaterialTheme.typography.bodyMedium, color = TextSecondary
        )
    }
}
