package com.taskfinisher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskfinisher.data.model.Task
import com.taskfinisher.data.repository.TaskRepository
import com.taskfinisher.utils.toLocalDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val tasksForSelectedDay: List<Task> = emptyList(),
    // Set of dates (in current month) that have at least one task
    val datesWithTasks: Set<LocalDate> = emptySet(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _selectedDate = MutableStateFlow(LocalDate.now())

    /** Dates that have deadlines — used to render dots on the calendar grid */
    private val datesWithTasks: Flow<Set<LocalDate>> =
        repository.getAllDeadlineTimestamps().map { timestamps ->
            timestamps.map { it.toLocalDate() }.toSet()
        }

    val uiState: StateFlow<CalendarUiState> = combine(
        _currentMonth,
        _selectedDate,
        datesWithTasks
    ) { month, selected, dotDates ->
        Triple(month, selected, dotDates)
    }.flatMapLatest { (month, selected, dotDates) ->
        repository.getTasksForDay(selected).map { tasks ->
            CalendarUiState(
                currentMonth = month,
                selectedDate = selected,
                tasksForSelectedDay = tasks,
                datesWithTasks = dotDates,
                isLoading = false
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarUiState())

    fun selectDate(date: LocalDate) { _selectedDate.value = date }

    fun previousMonth() { _currentMonth.update { it.minusMonths(1) } }

    fun nextMonth() { _currentMonth.update { it.plusMonths(1) } }

    fun jumpToMonth(month: YearMonth) { _currentMonth.value = month }
}
