package com.sricharan.dailyschedule.backup

import com.sricharan.dailyschedule.data.Completion
import com.sricharan.dailyschedule.data.DayReflection
import com.sricharan.dailyschedule.data.ScheduleItem
import kotlinx.serialization.Serializable

/**
 * The full shape of a backup file. Versioned so future schema changes
 * can still restore older backups gracefully.
 *
 * v1 backups have no reflections; the default keeps them restorable.
 */
@Serializable
data class BackupData(
    val backupVersion: Int = 2,
    val exportedAt: Long = System.currentTimeMillis(),
    val items: List<ScheduleItem>,
    val completions: List<Completion>,
    val reflections: List<DayReflection> = emptyList()
)
