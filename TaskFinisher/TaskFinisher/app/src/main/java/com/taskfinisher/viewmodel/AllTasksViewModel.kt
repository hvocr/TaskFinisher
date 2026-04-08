package com.taskfinisher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskfinisher.data.model.Task
import com.taskfinisher.data.repository.TaskRepository
import com.taskfinisher.notifications.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TaskFilter { ALL, HIGH_IMPORTANCE, MEDIUM_IMPORTANCE, LOW_IMPORTANCE,
    HIGH_ENERGY, OVERDUE, RECURRING }

enum class TaskSort { DEADLINE, IMPORTANCE, DATE_CREATED }

data class AllTasksUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
    val snackbarMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AllTasksViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val activeFilter = MutableStateFlow(TaskFilter.ALL)
    val activeSort = MutableStateFlow(TaskSort.DEADLINE)
    private val _snackbar = MutableStateFlow<String?>(null)

    /** Merge all active tasks (non-recurring + recurring instances) */
    private val allTasks: Flow<List<Task>> = combine(
        repository.getNonRecurringActiveTasks(),
        repository.getRecurringInstances(),
        repository.getOverdueTasks()
    ) { regular, recurring, overdue ->
        (regular + recurring + overdue).distinctBy { it.id }
    }

    val uiState: StateFlow<AllTasksUiState> = combine(
        allTasks, searchQuery, activeFilter, activeSort, _snackbar
    ) { tasks, query, filter, sort, snackbar ->
        val filtered = tasks
            .filter { matchesSearch(it, query) }
            .filter { matchesFilter(it, filter) }
            .sortedWith(comparatorFor(sort))
        AllTasksUiState(tasks = filtered, isLoading = false, snackbarMessage = snackbar)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AllTasksUiState())

    private fun matchesSearch(task: Task, query: String): Boolean =
        query.isBlank() || task.title.contains(query, ignoreCase = true) ||
            task.notes?.contains(query, ignoreCase = true) == true

    private fun matchesFilter(task: Task, filter: TaskFilter): Boolean = when (filter) {
        TaskFilter.ALL -> true
        TaskFilter.HIGH_IMPORTANCE -> task.importance == 2
        TaskFilter.MEDIUM_IMPORTANCE -> task.importance == 1
        TaskFilter.LOW_IMPORTANCE -> task.importance == 0
        TaskFilter.HIGH_ENERGY -> task.energy == 2
        TaskFilter.OVERDUE -> task.deadline != null && task.deadline < System.currentTimeMillis()
        TaskFilter.RECURRING -> task.recurrenceRule != null
    }

    private fun comparatorFor(sort: TaskSort): Comparator<Task> = when (sort) {
        TaskSort.DEADLINE -> compareBy(nullsLast()) { it.deadline }
        TaskSort.IMPORTANCE -> compareByDescending { it.importance }
        TaskSort.DATE_CREATED -> compareByDescending { it.createdAt }
    }

    fun completeTask(task: Task) = viewModelScope.launch {
        repository.completeTask(task)
        notificationScheduler.cancelReminder(task.id)
    }

    fun deleteTask(task: Task) = viewModelScope.launch {
        repository.deleteTask(task)
        notificationScheduler.cancelReminder(task.id)
        _snackbar.value = "Task deleted"
    }

    fun archiveTask(task: Task) = viewModelScope.launch {
        repository.archiveTask(task)
        _snackbar.value = "Task archived"
    }

    fun pushToTomorrow(task: Task) = viewModelScope.launch {
        repository.pushToTomorrow(task)
        _snackbar.value = "Pushed to tomorrow"
    }

    fun toggleBig3(task: Task) = viewModelScope.launch {
        val ok = repository.toggleBig3(task)
        if (!ok) _snackbar.value = "Big 3 is full — remove one first"
    }

    fun clearSnackbar() { _snackbar.value = null }
}
