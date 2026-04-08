package com.taskfinisher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskfinisher.data.model.Task
import com.taskfinisher.data.repository.TaskRepository
import com.taskfinisher.utils.startOfDayMillis
import com.taskfinisher.utils.endOfDayMillis
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class ArchiveFilter { ALL, THIS_WEEK, THIS_MONTH, CUSTOM }

data class ArchiveUiState(
    val tasks: List<Task> = emptyList(),
    val filter: ArchiveFilter = ArchiveFilter.ALL,
    val customFrom: Long? = null,
    val customTo: Long? = null,
    val isLoading: Boolean = true,
    val showClearConfirm: Boolean = false,
    val snackbarMessage: String? = null
)

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(ArchiveFilter.ALL)
    private val _customFrom = MutableStateFlow<Long?>(null)
    private val _customTo = MutableStateFlow<Long?>(null)
    private val _showClearConfirm = MutableStateFlow(false)
    private val _snackbar = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ArchiveUiState> = combine(
        _filter, _customFrom, _customTo, _showClearConfirm, _snackbar
    ) { filter, from, to, showConfirm, snackbar ->
        // Return a key so flatMapLatest re-subscribes when filter changes
        listOf(filter, from, to, showConfirm, snackbar) to Pair(from, to)
    }.flatMapLatest { (args, _) ->
        val filter = args[0] as ArchiveFilter
        val from = args[1] as? Long
        val to = args[2] as? Long
        val showConfirm = args[3] as Boolean
        val snackbar = args[4] as? String

        val tasksFlow: Flow<List<Task>> = when (filter) {
            ArchiveFilter.ALL -> repository.getArchivedTasks()
            ArchiveFilter.THIS_WEEK -> {
                val end = LocalDate.now().endOfDayMillis()
                val start = LocalDate.now().minusDays(6).startOfDayMillis()
                repository.getArchivedTasksInRange(start, end)
            }
            ArchiveFilter.THIS_MONTH -> {
                val end = LocalDate.now().endOfDayMillis()
                val start = LocalDate.now().withDayOfMonth(1).startOfDayMillis()
                repository.getArchivedTasksInRange(start, end)
            }
            ArchiveFilter.CUSTOM -> {
                if (from != null && to != null)
                    repository.getArchivedTasksInRange(from, to)
                else
                    repository.getArchivedTasks()
            }
        }

        tasksFlow.map { tasks ->
            ArchiveUiState(
                tasks = tasks, filter = filter,
                customFrom = from, customTo = to,
                isLoading = false,
                showClearConfirm = showConfirm,
                snackbarMessage = snackbar
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ArchiveUiState())

    fun setFilter(f: ArchiveFilter) { _filter.value = f }
    fun setCustomRange(from: Long, to: Long) {
        _customFrom.value = from; _customTo.value = to
        _filter.value = ArchiveFilter.CUSTOM
    }

    fun requestClearArchive() { _showClearConfirm.value = true }
    fun dismissClearConfirm() { _showClearConfirm.value = false }

    fun clearArchive() = viewModelScope.launch {
        repository.clearAllArchived()
        _showClearConfirm.value = false
        _snackbar.value = "Archive cleared"
    }

    fun unarchiveTask(task: Task) = viewModelScope.launch {
        repository.unarchiveTask(task.id)
        _snackbar.value = "\"${task.title}\" moved back to active"
    }

    fun clearSnackbar() { _snackbar.value = null }
}
