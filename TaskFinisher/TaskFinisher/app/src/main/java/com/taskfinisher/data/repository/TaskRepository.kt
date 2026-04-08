package com.taskfinisher.data.repository

import com.taskfinisher.data.database.TaskDao
import com.taskfinisher.data.database.TaskEntity
import com.taskfinisher.data.database.toDomain
import com.taskfinisher.data.database.toEntity
import com.taskfinisher.data.model.Task
import com.taskfinisher.utils.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val dao: TaskDao
) {

    // ─── Reads ───────────────────────────────────────────────────────────────

    fun getTodayTasks(): Flow<List<Task>> {
        val today = LocalDate.now()
        val start = today.startOfDayMillis()
        val end = today.endOfDayMillis()
        return dao.getTodayTasks(start, end).map { it.map(TaskEntity::toDomain) }
    }

    fun getTodayRecurringInstances(): Flow<List<Task>> {
        val today = LocalDate.now()
        return dao.getTodayRecurringInstances(today.startOfDayMillis(), today.endOfDayMillis())
            .map { it.map(TaskEntity::toDomain) }
    }

    fun getAllActiveTasks(): Flow<List<Task>> =
        dao.getActiveTasks().map { it.map(TaskEntity::toDomain) }

    fun getNonRecurringActiveTasks(): Flow<List<Task>> =
        dao.getNonRecurringActiveTasks().map { it.map(TaskEntity::toDomain) }

    fun getRecurringInstances(): Flow<List<Task>> =
        dao.getRecurringInstances().map { it.map(TaskEntity::toDomain) }

    fun getArchivedTasks(): Flow<List<Task>> =
        dao.getArchivedTasks().map { it.map(TaskEntity::toDomain) }

    fun getArchivedTasksInRange(from: Long, to: Long): Flow<List<Task>> =
        dao.getArchivedTasksInRange(from, to).map { it.map(TaskEntity::toDomain) }

    fun getBig3Tasks(): Flow<List<Task>> =
        dao.getBig3Tasks().map { it.map(TaskEntity::toDomain) }

    fun getTasksForDay(day: LocalDate): Flow<List<Task>> =
        dao.getTasksForDay(day.startOfDayMillis(), day.endOfDayMillis())
            .map { it.map(TaskEntity::toDomain) }

    fun getAllDeadlineTimestamps(): Flow<List<Long>> =
        dao.getAllDeadlineTimestamps()

    fun getSubtasksFor(parentId: String): Flow<List<Task>> =
        dao.getSubtasksFor(parentId).map { it.map(TaskEntity::toDomain) }

    fun getOverdueTasks(): Flow<List<Task>> =
        dao.getOverdueTasks(System.currentTimeMillis()).map { it.map(TaskEntity::toDomain) }

    suspend fun getTaskById(id: String): Task? =
        dao.getTaskById(id)?.toDomain()

    // ─── Writes ──────────────────────────────────────────────────────────────

    suspend fun saveTask(task: Task) = dao.insertTask(task.toEntity())

    suspend fun updateTask(task: Task) = dao.updateTask(task.toEntity())

    suspend fun deleteTask(task: Task) = dao.deleteTask(task.toEntity())

    suspend fun deleteTaskById(id: String) = dao.deleteTaskById(id)

    suspend fun archiveTask(task: Task) {
        dao.updateTask(task.copy(isArchived = true).toEntity())
    }

    suspend fun unarchiveTask(id: String) = dao.unarchiveTask(id)

    suspend fun clearAllArchived() = dao.clearAllArchived()

    // ─── Complete task (handles recurrence) ──────────────────────────────────

    /**
     * Marks a task as completed. If the task has a recurrenceRule:
     *  1. Mark the current instance as archived (so it leaves the active list).
     *  2. Spawn a new instance for the next occurrence with a fresh deadline.
     *
     * If the task is one-time: simply mark isCompleted = true.
     */
    suspend fun completeTask(task: Task) {
        val now = System.currentTimeMillis()

        if (task.recurrenceRule != null) {
            // Archive the completed instance
            dao.updateTask(
                task.copy(
                    isCompleted = true,
                    isArchived = true,
                    completedAt = now,
                    isBig3 = false
                ).toEntity()
            )

            // Calculate next deadline based on rule
            val baseDeadline = task.deadline ?: now
            val nextDeadline = RecurrenceHelper.nextOccurrence(task.recurrenceRule, baseDeadline)

            if (nextDeadline != null) {
                // Spawn next instance
                val nextInstance = task.copy(
                    id = newId(),
                    deadline = nextDeadline,
                    isCompleted = false,
                    isArchived = false,
                    isBig3 = false,
                    createdAt = now,
                    completedAt = null
                )
                dao.insertTask(nextInstance.toEntity())
            }
        } else {
            // Plain task — just mark completed
            dao.updateTask(
                task.copy(
                    isCompleted = true,
                    completedAt = now,
                    isBig3 = false
                ).toEntity()
            )
        }
    }

    // ─── Big 3 ───────────────────────────────────────────────────────────────

    /**
     * Toggles Big 3 status. Enforces max of 3.
     * Returns false if limit reached and task wasn't already Big 3.
     */
    suspend fun toggleBig3(task: Task): Boolean {
        return if (task.isBig3) {
            dao.unmarkBig3(task.id)
            true
        } else {
            val count = dao.getBig3Count()
            if (count >= 3) {
                false  // Caller should show a Snackbar / toast
            } else {
                dao.markBig3(task.id)
                true
            }
        }
    }

    // ─── Reschedule ──────────────────────────────────────────────────────────

    /** Push task deadline to tomorrow (same time if deadline had time, else midnight) */
    suspend fun pushToTomorrow(task: Task) {
        val tomorrow = LocalDate.now().plusDays(1)
        val newDeadline = if (task.deadline != null) {
            // Keep the same time-of-day, just advance the date
            val ldt = task.deadline.toLocalDateTime()
            val newLdt = tomorrow.atTime(ldt.toLocalTime())
            newLdt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        } else {
            tomorrow.startOfDayMillis()
        }
        dao.updateTask(task.copy(deadline = newDeadline).toEntity())
    }

    // ─── Streak ──────────────────────────────────────────────────────────────

    suspend fun getCurrentStreak(): Int {
        val timestamps = dao.getAllCompletionTimestamps()
        return StreakCalculator.calculate(timestamps)
    }

    // ─── Notifications ───────────────────────────────────────────────────────

    suspend fun getTasksWithFutureDeadlines(): List<Task> =
        dao.getTasksWithFutureDeadlines(System.currentTimeMillis()).map(TaskEntity::toDomain)
}
