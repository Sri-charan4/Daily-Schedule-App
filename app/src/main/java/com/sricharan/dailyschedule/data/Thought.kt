package com.sricharan.dailyschedule.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * A single thought written down on a given day. Unlike [DayReflection] there
 * can be as many of these per day as you like — the point is to be able to put
 * something down the moment it's on your mind, not to compose one tidy summary.
 *
 * [writtenAt] is kept so entries can be shown in the order they arrived, with
 * the time of day they were written; it also survives backup/restore.
 */
@Entity(tableName = "thoughts", indices = [Index("date")])
@Serializable
data class Thought(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    // "yyyy-MM-dd" — the day this belongs to.
    val date: String,
    val text: String,
    val writtenAt: Long = System.currentTimeMillis()
)
