package com.sricharan.dailyschedule.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * A single entity models both:
 *  - one-off tasks (isRecurring = false, dateTime = specific date/time)
 *  - recurring routines (isRecurring = true, recurrenceDays = e.g. "MON,TUE,WED..." )
 *
 * Keeping one table (instead of separate Task/Routine tables) makes the
 * "today's schedule" query trivial and keeps backup/restore simple.
 */
@Entity(tableName = "schedule_items")
@Serializable
data class ScheduleItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,
    val notes: String = "",

    // For one-off tasks: epoch millis of the due date/time. Null if no fixed date.
    val dateTime: Long? = null,

    // Category tag for simple filtering ("Work", "Personal", "Health"...)
    val category: String = "General",

    val isRecurring: Boolean = false,
    // Comma-separated day codes when recurring, e.g. "MON,WED,FRI". Empty otherwise.
    val recurrenceDays: String = "",
    // Time-of-day for recurring items, stored as "HH:mm" (24h). Empty if none.
    val recurrenceTime: String = "",

    /**
     * Whether anything at all arrives. Kept as the original boolean rather
     * than folded into an enum column so that every backup ever written by
     * this app still restores with its reminders intact.
     */
    val reminderEnabled: Boolean = false,
    /**
     * Whether that something rings rather than whispers. Only meaningful when
     * [reminderEnabled] is true — read the pair through
     * `ScheduleItem.reminderStyle` instead of touching either directly.
     */
    val alarmEnabled: Boolean = false,

    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Tracks per-day completion for recurring routines, so streaks/stats
 * can be computed without mutating the routine definition itself.
 */
@Entity(tableName = "completions")
@Serializable
data class Completion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scheduleItemId: Long,
    // Date this completion applies to, formatted "yyyy-MM-dd"
    val date: String,
    val isCompleted: Boolean = true
)

/**
 * A single occurrence of a recurring item that was let go of for one day only.
 *
 * Recurring items are generated from a rule rather than stored per-day, so
 * "not today, but keep the routine" needs somewhere to live. Keyed by item +
 * date so skipping the same day twice is a no-op.
 */
@Entity(tableName = "skipped_occurrences", primaryKeys = ["scheduleItemId", "date"])
@Serializable
data class SkippedOccurrence(
    val scheduleItemId: Long,
    // "yyyy-MM-dd"
    val date: String
)
