package com.sricharan.dailyschedule.backup

import com.sricharan.dailyschedule.data.Completion
import com.sricharan.dailyschedule.data.DayReflection
import com.sricharan.dailyschedule.data.ScheduleItem
import com.sricharan.dailyschedule.data.SkippedOccurrence
import com.sricharan.dailyschedule.data.Thought
import kotlinx.serialization.Serializable

/**
 * The full shape of a backup file. Versioned so future schema changes
 * can still restore older backups gracefully.
 *
 * v1 backups have no reflections and v2 none of the written thoughts or
 * skipped days; the defaults keep both restorable.
 *
 * v3 backups predate alarms. Their items carry reminderEnabled but no
 * alarmEnabled, which defaults to false on restore — so a nudge comes back as
 * a nudge, and nothing restored from an old file starts ringing unasked.
 */
@Serializable
data class BackupData(
    val backupVersion: Int = 4,
    val exportedAt: Long = System.currentTimeMillis(),
    val items: List<ScheduleItem>,
    val completions: List<Completion>,
    val reflections: List<DayReflection> = emptyList(),
    val thoughts: List<Thought> = emptyList(),
    val skips: List<SkippedOccurrence> = emptyList()
)
