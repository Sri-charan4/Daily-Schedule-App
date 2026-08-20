package com.sricharan.dailyschedule.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * A soft note about how a day felt. Deliberately has no rating, score, or
 * required fields — it exists to be written in, not measured.
 *
 * Keyed by date so there's exactly one per day and writing again just
 * replaces it.
 */
@Entity(tableName = "day_reflections")
@Serializable
data class DayReflection(
    @PrimaryKey
    // "yyyy-MM-dd"
    val date: String,
    val note: String = "",
    val savedAt: Long = System.currentTimeMillis()
)
