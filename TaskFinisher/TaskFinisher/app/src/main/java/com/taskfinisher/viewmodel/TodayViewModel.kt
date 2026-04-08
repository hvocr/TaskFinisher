package com.taskfinisher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskfinisher.data.model.Task
import com.taskfinisher.data.repository.TaskRepository
import com.taskfinisher.notifications.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TodayUiState(
    val big3Tasks: List<Task> = emptyList(),
    val otherTasks: List<Task> = emptyList(),
    val streak: Int = 0,
    val isLoading: Boolean = true,
    val snackbarMessage: String? = null
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {

    private val _snackbarMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TodayUiState> = combine(
        repository.getTodayTasks(),
        repository.getTodayRecurringInstances(),
        _snackbarMessage
    ) { regularTasks, recurringInstances, snackbar ->
        val allToday = (regularTasks + recurringInstances)
            .sortedWith(compareByDescending<Task> { it.isBig3 }.thenByDescending { it.importance })
        TodayUiState(
            big3Tasks = allToday.filter { it.isBig3 },
            otherTasks = allToday.filter { !it.isBig3 },
            isLoading = false,
            snackbarMessage = snackbar
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState())

    init {
        // Load streak separately (suspend call)
        viewModelScope.launch {
            // streak is merged in a separate flow below
        }
    }

    val streak: StateFlow<Int> = flow {
        // Recompute once on start; a production app could invalidate on each completion
        emit(repository.getCurrentStreak())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun completeTask(task: Task) = viewModelScope.launch {
        repository.completeTask(task)
        notificationScheduler.cancelReminder(task.id)
        _snackbarMessage.value = "\"${task.title}\" completed 🎉"
    }

    fun toggleBig3(task: Task) = viewModelScope.launch {
        val success = repository.toggleBig3(task)
        if (!success) _snackbarMessage.value = "Big 3 is full — remove one first"
    }

    fun deleteTask(task: Task) = viewModelScope.launch {
        repository.deleteTask(task)
        notificationScheduler.cancelReminder(task.id)
        _snackbarMessage.value = "Task deleted"
    }

    fun archiveTask(task: Task) = viewModelScope.launch {
        repository.archiveTask(task)
        notificationScheduler.cancelReminder(task.id)
        _snackbarMessage.value = "Task archived"
    }

    fun pushToTomorrow(task: Task) = viewModelScope.launch {
        repository.pushToTomorrow(task)
        _snackbarMessage.value = "Pushed to tomorrow"
    }

    fun clearSnackbar() { _snackbarMessage.value = null }
}
