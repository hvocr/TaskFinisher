package com.taskfinisher.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskfinisher.data.model.Task
import com.taskfinisher.ui.components.*
import com.taskfinisher.ui.theme.*
import com.taskfinisher.utils.formatDeadline
import com.taskfinisher.utils.isOverdue
import com.taskfinisher.viewmodel.CalendarViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(
    onEditTask: (String) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // ── Month header + nav ─────────────────────────────────────────────────
        MonthHeader(
            month = state.currentMonth,
            onPrevious = viewModel::previousMonth,
            onNext = viewModel::nextMonth
        )

        // ── Day-of-week labels ─────────────────────────────────────────────────
        DayOfWeekRow()

        // ── Calendar grid ──────────────────────────────────────────────────────
        CalendarGrid(
            month = state.currentMonth,
            selectedDate = state.selectedDate,
            datesWithTasks = state.datesWithTasks,
            onDateSelected = viewModel::selectDate
        )

        HorizontalDivider(color = Divider, modifier = Modifier.padding(vertical = 8.dp))

        // ── Tasks for selected day ─────────────────────────────────────────────
        val dayLabel = when {
            state.selectedDate == LocalDate.now() -> "Today"
            state.selectedDate == LocalDate.now().plusDays(1) -> "Tomorrow"
            state.selectedDate == LocalDate.now().minusDays(1) -> "Yesterday"
            else -> state.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
        }

        Text(
            dayLabel,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        if (state.tasksForSelectedDay.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📅", fontSize = 32.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("No tasks this day", style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary)
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                items(state.tasksForSelectedDay, key = { it.id }) { task ->
                    CalendarTaskRow(task = task, onEdit = { onEditTask(task.id) })
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Filled.ChevronLeft, "Previous month", tint = TextPrimary)
        }
        Text(
            month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.ChevronRight, "Next month", tint = TextPrimary)
        }
    }
}

@Composable
private fun DayOfWeekRow() {
    val days = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        days.forEach { day ->
            Text(
                day, style = MaterialTheme.typography.labelSmall,
                color = TextSecondary, textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    datesWithTasks: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val firstDay = month.atDay(1)
    // Sunday = 0 offset (java DayOfWeek is 1=Mon..7=Sun, so Sunday needs special handling)
    val startOffset = (firstDay.dayOfWeek.value % 7)  // Sun=0,Mon=1..Sat=6
    val daysInMonth = month.lengthOfMonth()
    val totalCells = startOffset + daysInMonth
    val rows = (totalCells + 6) / 7

    Column(Modifier.padding(horizontal = 8.dp)) {
        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val cellIndex = row * 7 + col
                    val dayOfMonth = cellIndex - startOffset + 1
                    if (dayOfMonth < 1 || dayOfMonth > daysInMonth) {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = month.atDay(dayOfMonth)
                        val isSelected = date == selectedDate
                        val isToday = date == today
                        val isOverdue = date.isBefore(today) && datesWithTasks.contains(date)
                        val hasTasks = datesWithTasks.contains(date)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> Accent
                                        isToday -> Accent.copy(alpha = 0.2f)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { onDateSelected(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isToday || isSelected) FontWeight.Bold
                                                    else FontWeight.Normal
                                    ),
                                    color = when {
                                        isSelected -> OnAccent
                                        isToday -> Accent
                                        isOverdue && !isSelected -> OverdueTint
                                        else -> TextPrimary
                                    }
                                )
                                // Task dot
                                if (hasTasks) {
                                    Box(
                                        Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) OnAccent
                                                else if (isOverdue) OverdueTint
                                                else Accent
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarTaskRow(task: Task, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(importanceColor(task.importance))
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(task.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            task.deadline?.let {
                Text(it.formatDeadline(), style = MaterialTheme.typography.bodyMedium,
                    color = if (it.isOverdue()) OverdueTint else TextSecondary)
            }
        }
        Icon(Icons.Filled.ChevronRight, null, tint = TextSecondary)
    }
    HorizontalDivider(color = Divider, modifier = Modifier.padding(horizontal = 20.dp))
}
