package com.taskfinisher.data.model

/**
 * Domain model used throughout the app (UI, ViewModel).
 * Kept separate from TaskEntity so the database layer can evolve independently.
 */
data class Task(
    val id: String,
    val title: String,
    val notes: String? = null,
    val deadline: Long? = null,           // epoch millis, null = no deadline
    val importance: Int = 1,              // 0=Low, 1=Medium, 2=High
    val energy: Int = 1,                  // 0=Low, 1=Medium, 2=High
    val isCompleted: Boolean = false,
    val isArchived: Boolean = false,
    val isBig3: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val parentTaskId: String? = null,     // non-null → this is a subtask
    val recurrenceRule: String? = null    // null = one-time task
)

/** Convenience enum wrappers for type-safe importance/energy access in UI */
enum class Importance(val value: Int, val label: String) {
    LOW(0, "Low"), MEDIUM(1, "Medium"), HIGH(2, "High");
    companion object { fun from(v: Int) = entries.first { it.value == v } }
}

enum class Energy(val value: Int, val label: String) {
    LOW(0, "Low"), MEDIUM(1, "Medium"), HIGH(2, "High");
    companion object { fun from(v: Int) = entries.first { it.value == v } }
}
