package com.sricharan.dailyschedule.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sricharan.dailyschedule.data.AppDatabase
import com.sricharan.dailyschedule.data.DayReflection
import com.sricharan.dailyschedule.data.ScheduleItem
import com.sricharan.dailyschedule.data.Thought
import com.sricharan.dailyschedule.domain.Garden
import com.sricharan.dailyschedule.domain.buildGarden
import com.sricharan.dailyschedule.domain.dateKey
import com.sricharan.dailyschedule.domain.key
import com.sricharan.dailyschedule.domain.onDate
import com.sricharan.dailyschedule.domain.someday
import com.sricharan.dailyschedule.notifications.ReminderScheduler
import com.sricharan.dailyschedule.notifications.ReminderSync
import com.sricharan.dailyschedule.repository.ScheduleRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ScheduleRepository(
        AppDatabase.getInstance(application).scheduleDao()
    )

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val allItems = repository.getAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Single days of a routine that were let go of. Held as "id@yyyy-MM-dd"
     * keys so every screen can ask about a day without another query.
     */
    val skippedKeys: StateFlow<Set<String>> =
        repository.getAllSkips()
            .map { skips -> skips.map { it.key() }.toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    /** Items belonging to whichever day is currently in focus. */
    val itemsForSelectedDate: StateFlow<List<ScheduleItem>> =
        combine(allItems, _selectedDate, skippedKeys) { items, date, skipped ->
            items.onDate(date, skipped)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Undated intentions — always available, never late. */
    val somedayItems: StateFlow<List<ScheduleItem>> =
        allItems.map { it.someday() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Ids completed on the selected day, so cards can show their tended state. */
    val doneOnSelectedDate: StateFlow<Set<Long>> =
        _selectedDate
            .flatMapLatest { date -> repository.getCompletionsOnDate(date.dateKey()) }
            .map { completions -> completions.map { it.scheduleItemId }.toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    /** Every date that has at least one completion — used for calendar dots. */
    val tendedDates: StateFlow<Set<String>> =
        repository.getAllCompletions()
            .map { completions -> completions.map { it.date }.toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val garden: StateFlow<Garden> =
        combine(allItems, repository.getAllCompletions()) { items, completions ->
            buildGarden(items, completions)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Garden(emptyList()))

    val reflectionForSelectedDate: StateFlow<DayReflection?> =
        _selectedDate
            .flatMapLatest { date -> repository.getReflection(date.dateKey()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Everything written down on the day in focus, oldest first. */
    val thoughtsForSelectedDate: StateFlow<List<Thought>> =
        _selectedDate
            .flatMapLatest { date -> repository.getThoughts(date.dateKey()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun saveItem(item: ScheduleItem) = viewModelScope.launch {
        // The new id matters for a freshly inserted item — its alarm is keyed
        // by it, and 0 would book the alarm against the wrong row.
        val id = repository.saveItem(item)
        ReminderSync.syncOne(getApplication(), id)
    }

    suspend fun loadItem(id: Long): ScheduleItem? = repository.getItemById(id)

    fun deleteItem(item: ScheduleItem) = viewModelScope.launch {
        repository.deleteItem(item)
        ReminderScheduler.cancel(getApplication(), item.id)
    }

    /** Tend (or un-tend) an item on the day currently in focus. */
    fun setDone(item: ScheduleItem, done: Boolean) = viewModelScope.launch {
        repository.setDone(item.id, _selectedDate.value.dateKey(), done)
    }

    fun saveReflection(note: String) = viewModelScope.launch {
        repository.saveReflection(_selectedDate.value.dateKey(), note)
    }

    fun addThought(text: String) = viewModelScope.launch {
        repository.addThought(_selectedDate.value.dateKey(), text)
    }

    fun deleteThought(thought: Thought) = viewModelScope.launch {
        repository.deleteThought(thought)
    }

    /**
     * Lets go of this item on the day in focus only. The routine itself stays
     * exactly as it was, so tomorrow comes back around as usual.
     */
    fun skipOnSelectedDate(item: ScheduleItem) = viewModelScope.launch {
        repository.skipOccurrence(item.id, _selectedDate.value.dateKey())
        // The skipped day may have been the one the pending alarm was set for.
        ReminderSync.syncOne(getApplication(), item.id)
    }
}
