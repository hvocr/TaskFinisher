package com.taskfinisher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskfinisher.data.model.Task
import com.taskfinisher.data.repository.TaskRepository
import com.taskfinisher.notifications.NotificationScheduler
import com.taskfinisher.utils.newId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditUiState(
    val id: String = newId(),
    val title: String = "",
    val notes: String = "",
    val deadline: Long? = null,
    val importance: Int = 1,
    val energy: Int = 1,
    val recurrenceRule: String? = null,
    val subtasks: List<Task> = emptyList(),
    val parentTaskId: String? = null,
    val isEditMode: Boolean = false,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val titleError: String? = null
)

@HiltViewModel
class AddEditTaskViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditUiState())
    val state: StateFlow<AddEditUiState> = _state

    fun loadTask(taskId: String) = viewModelScope.launch {
        val task = repository.getTaskById(taskId) ?: return@launch
        _state.value = AddEditUiState(
            id = task.id, title = task.title, notes = task.notes ?: "",
            deadline = task.deadline, importance = task.importance,
            energy = task.energy, recurrenceRule = task.recurrenceRule,
            parentTaskId = task.parentTaskId, isEditMode = true
        )
        // Load existing subtasks
        repository.getSubtasksFor(task.id).collect { subtasks ->
            _state.value = _state.value.copy(subtasks = subtasks)
        }
    }

    fun updateTitle(v: String) { _state.value = _state.value.copy(title = v.take(100), titleError = null) }
    fun updateNotes(v: String) { _state.value = _state.value.copy(notes = v) }
    fun updateDeadline(v: Long?) { _state.value = _state.value.copy(deadline = v) }
    fun updateImportance(v: Int) { _state.value = _state.value.copy(importance = v) }
    fun updateEnergy(v: Int) { _state.value = _state.value.copy(energy = v) }
    fun updateRecurrenceRule(v: String?) { _state.value = _state.value.copy(recurrenceRule = v) }

    fun addSubtask(title: String) {
        if (title.isBlank()) return
        val subtask = Task(
            id = newId(), title = title, parentTaskId = _state.value.id,
            createdAt = System.currentTimeMillis()
        )
        _state.value = _state.value.copy(subtasks = _state.value.subtasks + subtask)
    }

    fun removeSubtask(subtaskId: String) {
        _state.value = _state.value.copy(
            subtasks = _state.value.subtasks.filter { it.id != subtaskId }
        )
    }

    fun toggleSubtaskComplete(subtaskId: String) {
        _state.value = _state.value.copy(
            subtasks = _state.value.subtasks.map {
                if (it.id == subtaskId) it.copy(isCompleted = !it.isCompleted) else it
            }
        )
    }

    fun save() = viewModelScope.launch {
        val s = _state.value
        if (s.title.isBlank()) {
            _state.value = s.copy(titleError = "Title is required")
            return@launch
        }
        _state.value = s.copy(isSaving = true)

        val task = Task(
            id = s.id, title = s.title.trim(),
            notes = s.notes.trim().takeIf { it.isNotEmpty() },
            deadline = s.deadline, importance = s.importance, energy = s.energy,
            recurrenceRule = s.recurrenceRule, parentTaskId = s.parentTaskId,
            createdAt = if (s.isEditMode) {
                repository.getTaskById(s.id)?.createdAt ?: System.currentTimeMillis()
            } else System.currentTimeMillis()
        )

        repository.saveTask(task)

        // Save subtasks
        s.subtasks.forEach { repository.saveTask(it) }

        // Schedule notification for deadline
        s.deadline?.let { notificationScheduler.scheduleDeadlineReminder(task) }

        _state.value = _state.value.copy(isSaving = false, savedSuccessfully = true)
    }

    fun deleteTask() = viewModelScope.launch {
        val s = _state.value
        repository.deleteTaskById(s.id)
        notificationScheduler.cancelReminder(s.id)
        // Also delete subtasks
        s.subtasks.forEach { repository.deleteTaskById(it.id) }
        _state.value = _state.value.copy(savedSuccessfully = true)
    }

    fun reset() { _state.value = AddEditUiState() }
}
