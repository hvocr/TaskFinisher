package com.taskfinisher.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskfinisher.ui.components.*
import com.taskfinisher.ui.theme.*
import com.taskfinisher.viewmodel.TodayViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(
    onEditTask: (String) -> Unit,
    onAddTask: () -> Unit,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val streak by viewModel.streak.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbars
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = SurfaceVariant,
                    contentColor = TextPrimary
                )
            }
        },
        containerColor = Background
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // ── Header: date + streak ──────────────────────────────────────────
            item {
                TodayHeader(streak = streak)
            }

            // ── Big 3 section ─────────────────────────────────────────────────
            item {
                SectionHeader(
                    icon = "⭐",
                    title = "Today's Big 3",
                    subtitle = "${state.big3Tasks.size}/3"
                )
            }

            if (state.big3Tasks.isEmpty()) {
                item { EmptyBig3Hint() }
            } else {
                items(state.big3Tasks, key = { it.id }) { task ->
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

            // ── Other Tasks section ────────────────────────────────────────────
            if (state.otherTasks.isNotEmpty()) {
                item {
                    SectionHeader(
                        icon = "📋",
                        title = "Other Tasks",
                        subtitle = "${state.otherTasks.size}"
                    )
                }
                items(state.otherTasks, key = { it.id }) { task ->
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

            // ── All-done empty state ───────────────────────────────────────────
            if (state.big3Tasks.isEmpty() && state.otherTasks.isEmpty()) {
                item { AllDoneEmptyState() }
            }
        }
    }
}

@Composable
private fun TodayHeader(streak: Int) {
    val today = LocalDate.now()
    val dayLabel = today.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Text(dayLabel, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(2.dp))
        Text("Today", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Spacer(Modifier.height(12.dp))

        // Streak pill
        if (streak > 0) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(StreakGold.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔥", fontSize = 16.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    "$streak day streak",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = StreakGold
                    )
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: String, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 16.sp)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary,
            modifier = Modifier.weight(1f))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

@Composable
private fun EmptyBig3Hint() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceVariant)
            .padding(16.dp)
    ) {
        Text(
            "Tap ⭐ on any task to add it to your Big 3.\nFocus on your 3 most important tasks today.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun AllDoneEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("✅", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text("You're all caught up!", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text("No tasks for today. Enjoy the day or add something new.",
            style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}
