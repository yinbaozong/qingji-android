package com.dreamjournal.app.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dreamjournal.app.data.local.DreamEntryEntity
import com.dreamjournal.app.data.repository.DreamRepository
import com.dreamjournal.app.domain.model.RecordType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class DayMarkers(
    val hasDayRecord: Boolean = false,
    val hasDreamRecord: Boolean = false
)

data class CalendarUiState(
    val selectedDate: String = LocalDate.now().toString(),
    val entries: List<DreamEntryEntity> = emptyList(),
    val markedDates: Map<String, DayMarkers> = emptyMap()
)

class CalendarViewModel(
    private val dreamRepository: DreamRepository
) : ViewModel() {
    private val selectedDate = MutableStateFlow(LocalDate.now().toString())

    private val allEntries = dreamRepository.observeEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState = combine(selectedDate, allEntries) { date, entries ->
        val markers = entries.groupBy { it.dreamDate }.mapValues { (_, dayEntries) ->
            DayMarkers(
                hasDayRecord = dayEntries.any { RecordType.fromStorage(it.recordType) == RecordType.DAY },
                hasDreamRecord = dayEntries.any { RecordType.fromStorage(it.recordType) == RecordType.DREAM }
            )
        }
        CalendarUiState(
            selectedDate = date,
            entries = entries.filter { it.dreamDate == date },
            markedDates = markers
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CalendarUiState()
    )

    fun onDateSelected(date: String) {
        selectedDate.value = date
    }

    companion object {
        fun factory(dreamRepository: DreamRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CalendarViewModel(dreamRepository) as T
                }
            }
        }
    }
}
