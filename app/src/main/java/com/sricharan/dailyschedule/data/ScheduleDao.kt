package com.sricharan.dailyschedule.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Query("SELECT * FROM schedule_items ORDER BY dateTime ASC")
    fun getAllItems(): Flow<List<ScheduleItem>>

    @Query("SELECT * FROM schedule_items WHERE id = :id")
    suspend fun getItemById(id: Long): ScheduleItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ScheduleItem): Long

    @Update
    suspend fun updateItem(item: ScheduleItem)

    @Delete
    suspend fun deleteItem(item: ScheduleItem)

    @Query("SELECT * FROM completions WHERE scheduleItemId = :itemId ORDER BY date DESC")
    fun getCompletionsForItem(itemId: Long): Flow<List<Completion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: Completion)

    @Query("SELECT * FROM completions WHERE scheduleItemId = :itemId AND date = :date LIMIT 1")
    suspend fun getCompletionForDate(itemId: Long, date: String): Completion?

    @Query("SELECT * FROM completions WHERE date = :date AND isCompleted = 1")
    fun getCompletionsOnDate(date: String): Flow<List<Completion>>

    @Query("SELECT * FROM completions WHERE date BETWEEN :startDate AND :endDate AND isCompleted = 1")
    fun getCompletionsBetween(startDate: String, endDate: String): Flow<List<Completion>>

    @Query("SELECT * FROM completions WHERE isCompleted = 1")
    fun getAllCompletions(): Flow<List<Completion>>

    @Query("DELETE FROM completions WHERE scheduleItemId = :itemId AND date = :date")
    suspend fun clearCompletion(itemId: Long, date: String)

    // --- Reflections ---

    @Query("SELECT * FROM day_reflections WHERE date = :date LIMIT 1")
    fun getReflection(date: String): Flow<DayReflection?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveReflection(reflection: DayReflection)

    @Query("DELETE FROM day_reflections WHERE date = :date")
    suspend fun deleteReflection(date: String)

    // --- Used by backup/restore ---

    @Query("SELECT * FROM schedule_items")
    suspend fun getAllItemsOnce(): List<ScheduleItem>

    @Query("SELECT * FROM completions")
    suspend fun getAllCompletionsOnce(): List<Completion>

    @Query("SELECT * FROM day_reflections")
    suspend fun getAllReflectionsOnce(): List<DayReflection>

    @Query("DELETE FROM schedule_items")
    suspend fun clearAllItems()

    @Query("DELETE FROM completions")
    suspend fun clearAllCompletions()

    @Query("DELETE FROM day_reflections")
    suspend fun clearAllReflections()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReflections(reflections: List<DayReflection>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllItems(items: List<ScheduleItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCompletions(completions: List<Completion>)
}
