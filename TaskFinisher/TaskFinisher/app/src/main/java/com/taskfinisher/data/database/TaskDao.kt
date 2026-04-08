package com.taskfinisher.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // ─── Insert / Update / Delete ────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    // ─── Active tasks ────────────────────────────────────────────────────────

    /**
     * Returns all tasks that are NOT archived AND have no parent (i.e. main tasks only).
     * Recurrence templates (recurrenceRule != null, isCompleted = false, isArchived = false)
     * are also excluded here — they are surfaced only via their spawned instances.
     * We identify "template" rows as: recurrenceRule != null AND parentTaskId IS NULL
     * AND isCompleted = 0. Generated instances have parentTaskId set to the template id.
     *
     * NOTE: For simplicity in this implementation, templates are distinguished by
     * having a recurrenceRule but parentTaskId IS NULL. Instances always carry
     * parentTaskId pointing to their template.
     */
    @Query("""
        SELECT * FROM tasks
        WHERE isArchived = 0
          AND parentTaskId IS NULL
          AND isCompleted = 0
        ORDER BY
          CASE WHEN deadline IS NULL THEN 1 ELSE 0 END,
          deadline ASC,
          importance DESC
    """)
    fun getActiveTasks(): Flow<List<TaskEntity>>

    /**
     * Returns active tasks that are NOT recurring templates and have no parent.
     * Used in AllTasksScreen to avoid showing template rows.
     */
    @Query("""
        SELECT * FROM tasks
        WHERE isArchived = 0
          AND parentTaskId IS NULL
          AND isCompleted = 0
          AND recurrenceRule IS NULL
        ORDER BY
          CASE WHEN deadline IS NULL THEN 1 ELSE 0 END,
          deadline ASC,
          importance DESC
    """)
    fun getNonRecurringActiveTasks(): Flow<List<TaskEntity>>

    /** Recurring task instances (parentTaskId is NOT null, spawned from a template) */
    @Query("""
        SELECT * FROM tasks
        WHERE isArchived = 0
          AND parentTaskId IS NOT NULL
          AND isCompleted = 0
        ORDER BY deadline ASC
    """)
    fun getRecurringInstances(): Flow<List<TaskEntity>>

    // ─── Today's tasks ───────────────────────────────────────────────────────

    /**
     * Tasks for "today": deadline falls within [dayStart, dayEnd], OR has no deadline.
     * Does not include archived/completed/template rows.
     */
    @Query("""
        SELECT * FROM tasks
        WHERE isArchived = 0
          AND isCompleted = 0
          AND parentTaskId IS NULL
          AND recurrenceRule IS NULL
          AND (deadline IS NULL OR (deadline >= :dayStart AND deadline <= :dayEnd))
        ORDER BY isBig3 DESC, importance DESC, deadline ASC
    """)
    fun getTodayTasks(dayStart: Long, dayEnd: Long): Flow<List<TaskEntity>>

    /** Recurring instances scheduled for today */
    @Query("""
        SELECT * FROM tasks
        WHERE isArchived = 0
          AND isCompleted = 0
          AND parentTaskId IS NOT NULL
          AND deadline >= :dayStart AND deadline <= :dayEnd
        ORDER BY isBig3 DESC, importance DESC
    """)
    fun getTodayRecurringInstances(dayStart: Long, dayEnd: Long): Flow<List<TaskEntity>>

    // ─── Big 3 ───────────────────────────────────────────────────────────────

    @Query("SELECT * FROM tasks WHERE isBig3 = 1 AND isCompleted = 0 AND isArchived = 0")
    fun getBig3Tasks(): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET isBig3 = 0 WHERE isCompleted = 0")
    suspend fun clearAllBig3()

    @Query("UPDATE tasks SET isBig3 = 1 WHERE id = :id")
    suspend fun markBig3(id: String)

    @Query("UPDATE tasks SET isBig3 = 0 WHERE id = :id")
    suspend fun unmarkBig3(id: String)

    /** Count of current Big 3 tasks (max 3 enforced in repository) */
    @Query("SELECT COUNT(*) FROM tasks WHERE isBig3 = 1 AND isCompleted = 0 AND isArchived = 0")
    suspend fun getBig3Count(): Int

    // ─── Calendar ────────────────────────────────────────────────────────────

    @Query("""
        SELECT * FROM tasks
        WHERE isArchived = 0
          AND isCompleted = 0
          AND deadline >= :dayStart AND deadline <= :dayEnd
        ORDER BY deadline ASC
    """)
    fun getTasksForDay(dayStart: Long, dayEnd: Long): Flow<List<TaskEntity>>

    /** All distinct deadline timestamps (epoch ms) for dot indicators on calendar */
    @Query("""
        SELECT deadline FROM tasks
        WHERE isArchived = 0 AND isCompleted = 0 AND deadline IS NOT NULL
    """)
    fun getAllDeadlineTimestamps(): Flow<List<Long>>

    // ─── Archive ─────────────────────────────────────────────────────────────

    @Query("""
        SELECT * FROM tasks
        WHERE isArchived = 1 OR isCompleted = 1
        ORDER BY completedAt DESC, createdAt DESC
    """)
    fun getArchivedTasks(): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks
        WHERE (isArchived = 1 OR isCompleted = 1)
          AND completedAt >= :from AND completedAt <= :to
        ORDER BY completedAt DESC
    """)
    fun getArchivedTasksInRange(from: Long, to: Long): Flow<List<TaskEntity>>

    @Query("DELETE FROM tasks WHERE isArchived = 1 OR isCompleted = 1")
    suspend fun clearAllArchived()

    @Query("UPDATE tasks SET isArchived = 0, isCompleted = 0, completedAt = NULL WHERE id = :id")
    suspend fun unarchiveTask(id: String)

    // ─── Filters ─────────────────────────────────────────────────────────────

    @Query("""
        SELECT * FROM tasks
        WHERE isArchived = 0 AND isCompleted = 0 AND importance = :importance
        ORDER BY deadline ASC
    """)
    fun getTasksByImportance(importance: Int): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks
        WHERE isArchived = 0 AND isCompleted = 0 AND energy = :energy
        ORDER BY deadline ASC
    """)
    fun getTasksByEnergy(energy: Int): Flow<List<TaskEntity>>

    /** Overdue tasks: deadline is in the past and not completed/archived */
    @Query("""
        SELECT * FROM tasks
        WHERE isArchived = 0 AND isCompleted = 0
          AND deadline IS NOT NULL AND deadline < :now
        ORDER BY deadline ASC
    """)
    fun getOverdueTasks(now: Long): Flow<List<TaskEntity>>

    // ─── Subtasks ─────────────────────────────────────────────────────────────

    @Query("SELECT * FROM tasks WHERE parentTaskId = :parentId ORDER BY createdAt ASC")
    fun getSubtasksFor(parentId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE parentTaskId = :parentId")
    suspend fun getSubtasksForSync(parentId: String): List<TaskEntity>

    // ─── Streak calculation ───────────────────────────────────────────────────

    /**
     * Returns all completedAt timestamps ordered DESC.
     * Used by StreakCalculator to walk backwards through completion history.
     */
    @Query("""
        SELECT completedAt FROM tasks
        WHERE completedAt IS NOT NULL
        ORDER BY completedAt DESC
    """)
    suspend fun getAllCompletionTimestamps(): List<Long>

    // ─── Notifications ────────────────────────────────────────────────────────

    /** Fetch all tasks that have upcoming deadlines (for scheduling notifications on startup) */
    @Query("""
        SELECT * FROM tasks
        WHERE isCompleted = 0 AND isArchived = 0
          AND deadline IS NOT NULL AND deadline > :now
    """)
    suspend fun getTasksWithFutureDeadlines(now: Long): List<TaskEntity>

    // ─── Single task ─────────────────────────────────────────────────────────

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): TaskEntity?
}
