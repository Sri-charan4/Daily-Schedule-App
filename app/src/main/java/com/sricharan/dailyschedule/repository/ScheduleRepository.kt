package com.sricharan.dailyschedule.repository

import com.sricharan.dailyschedule.data.Completion
import com.sricharan.dailyschedule.data.DayReflection
import com.sricharan.dailyschedule.data.ScheduleDao
import com.sricharan.dailyschedule.data.ScheduleItem
import com.sricharan.dailyschedule.data.SkippedOccurrence
import com.sricharan.dailyschedule.data.Thought
import kotlinx.coroutines.flow.Flow

class ScheduleRepository(private val dao: ScheduleDao) {

    fun getAllItems(): Flow<List<ScheduleItem>> = dao.getAllItems()

    suspend fun getItemById(id: Long): ScheduleItem? = dao.getItemById(id)

    suspend fun saveItem(item: ScheduleItem): Long =
        if (item.id == 0L) dao.insertItem(item) else { dao.updateItem(item); item.id }

    suspend fun deleteItem(item: ScheduleItem) {
        dao.clearSkipsForItem(item.id)
        dao.deleteItem(item)
    }

    fun getCompletionsForItem(itemId: Long): Flow<List<Completion>> =
        dao.getCompletionsForItem(itemId)

    fun getAllCompletions(): Flow<List<Completion>> = dao.getAllCompletions()

    fun getCompletionsOnDate(date: String): Flow<List<Completion>> =
        dao.getCompletionsOnDate(date)

    /**
     * Completion is per-day and reversible in both directions — un-ticking
     * something is just as ordinary as ticking it, with no penalty attached.
     */
    suspend fun setDone(itemId: Long, date: String, done: Boolean) {
        if (done) {
            dao.insertCompletion(
                Completion(scheduleItemId = itemId, date = date, isCompleted = true)
            )
        } else {
            dao.clearCompletion(itemId, date)
        }
    }

    suspend fun isDone(itemId: Long, date: String): Boolean =
        dao.getCompletionForDate(itemId, date)?.isCompleted == true

    // --- Reflections ---

    fun getReflection(date: String): Flow<DayReflection?> = dao.getReflection(date)

    suspend fun saveReflection(date: String, note: String) {
        if (note.isBlank()) dao.deleteReflection(date)
        else dao.saveReflection(DayReflection(date = date, note = note.trim()))
    }

    // --- Thoughts ---

    fun getThoughts(date: String): Flow<List<Thought>> = dao.getThoughts(date)

    suspend fun addThought(date: String, text: String) {
        if (text.isBlank()) return
        dao.insertThought(Thought(date = date, text = text.trim()))
    }

    suspend fun deleteThought(thought: Thought) = dao.deleteThought(thought)

    // --- Skipped occurrences ---

    fun getAllSkips(): Flow<List<SkippedOccurrence>> = dao.getAllSkips()

    /**
     * Lets go of one day of a routine without touching the routine itself.
     * Any completion recorded for that day goes with it, so the skipped day
     * doesn't keep counting as tended.
     */
    suspend fun skipOccurrence(itemId: Long, date: String) {
        dao.insertSkip(SkippedOccurrence(scheduleItemId = itemId, date = date))
        dao.clearCompletion(itemId, date)
    }

    suspend fun unskipOccurrence(itemId: Long, date: String) = dao.clearSkip(itemId, date)
}
