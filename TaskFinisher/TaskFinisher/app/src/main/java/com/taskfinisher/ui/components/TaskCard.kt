package com.taskfinisher.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.taskfinisher.data.model.Task
import com.taskfinisher.ui.theme.*
import com.taskfinisher.utils.RecurrenceHelper
import com.taskfinisher.utils.formatDeadline
import com.taskfinisher.utils.isOverdue

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskCard(
    task: Task,
    onComplete: (Task) -> Unit,
    onBig3Toggle: (Task) -> Unit,
    onEdit: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onArchive: (Task) -> Unit,
    onPushToTomorrow: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var showContextMenu by remember { mutableStateOf(false) }

    // Animate checkbox scale on complete
    var checkAnimTrigger by remember { mutableStateOf(false) }
    val checkScale by animateFloatAsState(
        targetValue = if (checkAnimTrigger) 1.25f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        finishedListener = { checkAnimTrigger = false },
        label = "checkScale"
    )

    val cardBg = if (task.isBig3) Big3Glow else CardColor
    val borderMod = if (task.isBig3)
        Modifier.border(1.dp, Accent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
    else Modifier

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(borderMod)
            .combinedClickable(
                onClick = { onEdit(task) },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showContextMenu = true
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Checkbox ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .scale(checkScale)
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable {
                        checkAnimTrigger = true
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onComplete(task)
                    }
                    .border(2.dp, importanceColor(task.importance), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(Icons.Filled.Check, null,
                        tint = importanceColor(task.importance), modifier = Modifier.size(14.dp))
                }
            }

            Spacer(Modifier.width(12.dp))

            // ── Title + metadata ─────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (task.isCompleted) TextSecondary else TextPrimary,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Deadline chip
                    task.deadline?.let { dl ->
                        val overdue = dl.isOverdue() && !task.isCompleted
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Schedule, null,
                                tint = if (overdue) OverdueTint else TextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = dl.formatDeadline(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (overdue) OverdueTint else TextSecondary
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }

                    // Recurrence indicator
                    if (task.recurrenceRule != null) {
                        Icon(Icons.Filled.Refresh, null,
                            tint = AccentTeal, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(
                            RecurrenceHelper.describeRule(task.recurrenceRule),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AccentTeal
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Importance dot + energy icon row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(importanceColor(task.importance))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = importanceLabel(task.importance),
                        style = MaterialTheme.typography.labelSmall,
                        color = importanceColor(task.importance)
                    )
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        energyIcon(task.energy), null,
                        tint = energyColor(task.energy),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        energyLabel(task.energy),
                        style = MaterialTheme.typography.labelSmall,
                        color = energyColor(task.energy)
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // ── Big 3 star ────────────────────────────────────────────
            IconButton(
                onClick = { onBig3Toggle(task) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    if (task.isBig3) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Toggle Big 3",
                    tint = if (task.isBig3) StreakGold else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // ── Long-press context menu ───────────────────────────────────────────────
    if (showContextMenu) {
        TaskContextMenu(
            task = task,
            onDismiss = { showContextMenu = false },
            onEdit = { showContextMenu = false; onEdit(task) },
            onDelete = { showContextMenu = false; onDelete(task) },
            onArchive = { showContextMenu = false; onArchive(task) },
            onPushToTomorrow = { showContextMenu = false; onPushToTomorrow(task) }
        )
    }
}

@Composable
private fun TaskContextMenu(
    task: Task,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onPushToTomorrow: () -> Unit
) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(SurfaceVariant)
    ) {
        DropdownMenuItem(
            text = { Text("Edit", color = TextPrimary) },
            leadingIcon = { Icon(Icons.Filled.Edit, null, tint = TextPrimary) },
            onClick = onEdit
        )
        DropdownMenuItem(
            text = { Text("Push to Tomorrow", color = TextPrimary) },
            leadingIcon = { Icon(Icons.Filled.ArrowForward, null, tint = TextPrimary) },
            onClick = onPushToTomorrow
        )
        DropdownMenuItem(
            text = { Text("Archive", color = TextPrimary) },
            leadingIcon = { Icon(Icons.Filled.Archive, null, tint = TextPrimary) },
            onClick = onArchive
        )
        HorizontalDivider(color = Divider)
        DropdownMenuItem(
            text = { Text("Delete", color = OverdueTint) },
            leadingIcon = { Icon(Icons.Filled.Delete, null, tint = OverdueTint) },
            onClick = onDelete
        )
    }
}

// ─── Color/label helpers ──────────────────────────────────────────────────────

fun importanceColor(v: Int): Color = when (v) {
    2 -> ColorHighImportance; 1 -> ColorMediumImportance; else -> ColorLowImportance
}
fun importanceLabel(v: Int): String = when (v) { 2 -> "High"; 1 -> "Medium"; else -> "Low" }
fun energyColor(v: Int): Color = when (v) {
    2 -> ColorHighEnergy; 1 -> ColorMediumEnergy; else -> ColorLowEnergy
}
fun energyLabel(v: Int): String = when (v) { 2 -> "High"; 1 -> "Medium"; else -> "Low" }
fun energyIcon(v: Int) = when (v) {
    2 -> Icons.Filled.ElectricBolt; 1 -> Icons.Filled.BatteryChargingFull; else -> Icons.Filled.Battery0Bar
}
