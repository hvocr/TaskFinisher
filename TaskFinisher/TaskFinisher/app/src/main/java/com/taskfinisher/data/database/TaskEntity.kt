package com.taskfinisher.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.taskfinisher.data.model.Task

/**
 * Room entity — maps 1-to-1 to the `tasks` SQLite table.
 * Subtasks share the same table; parentTaskId distinguishes them.
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val notes: String?,
    val deadline: Long?,
    val importance: Int,
    val energy: Int,
    val isCompleted: Boolean,
    val isArchived: Boolean,
    val isBig3: Boolean,
    val createdAt: Long,
    val completedAt: Long?,
    val parentTaskId: String?,
    val recurrenceRule: String?
)

// ─── Mapping helpers ────────────────────────────────────────────────────────

fun TaskEntity.toDomain(): Task = Task(
    id = id, title = title, notes = notes, deadline = deadline,
    importance = importance, energy = energy, isCompleted = isCompleted,
    isArchived = isArchived, isBig3 = isBig3, createdAt = createdAt,
    completedAt = completedAt, parentTaskId = parentTaskId,
    recurrenceRule = recurrenceRule
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id, title = title, notes = notes, deadline = deadline,
    importance = importance, energy = energy, isCompleted = isCompleted,
    isArchived = isArchived, isBig3 = isBig3, createdAt = createdAt,
    completedAt = completedAt, parentTaskId = parentTaskId,
    recurrenceRule = recurrenceRule
)
