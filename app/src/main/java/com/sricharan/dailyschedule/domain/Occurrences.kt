package com.sricharan.dailyschedule.domain

import com.sricharan.dailyschedule.data.ScheduleItem
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val DATE_KEY: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/** Day codes as stored in [ScheduleItem.recurrenceDays], e.g. "MON,WED,FRI". */
val DAY_CODES = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

fun LocalDate.dateKey(): String = format(DATE_KEY)

fun DayOfWeek.code(): String = DAY_CODES[value - 1]

fun ScheduleItem.recurrenceDaySet(): Set<String> =
    recurrenceDays.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }.toSet()

/** The date a one-off item is pinned to, if it has one. */
fun ScheduleItem.scheduledDate(): LocalDate? =
    dateTime?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }

/**
 * Parses "HH:mm" leniently — a blank or malformed time just means "no
 * particular time", which is a perfectly valid way to plan a day.
 */
fun ScheduleItem.timeOfDay(): LocalTime? {
    val raw = if (isRecurring) recurrenceTime else null
    if (!raw.isNullOrBlank()) {
        runCatching { return LocalTime.parse(raw.trim()) }
    }
    return dateTime
        ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime() }
        ?.takeIf { it != LocalTime.MIDNIGHT }
}

/** A human, unhurried rendering of when something happens. */
fun ScheduleItem.whenLabel(): String {
    val time = timeOfDay() ?: return "whenever it suits"
    return "around " + time.format(DateTimeFormatter.ofPattern("h:mm a")).lowercase()
}

/** Does this item belong on [date]? */
fun ScheduleItem.occursOn(date: LocalDate): Boolean = when {
    isRecurring -> {
        val days = recurrenceDaySet()
        // A recurring item with no days chosen is treated as an every-day
        // routine rather than one that silently never appears.
        days.isEmpty() || date.dayOfWeek.code() in days
    }
    // A one-off with a date shows on that date; one with no date at all is
    // an open intention, always available but never overdue.
    else -> scheduledDate()?.let { it == date } ?: false
}

/** Items with no date attached — things to get to eventually, not "late". */
fun List<ScheduleItem>.someday(): List<ScheduleItem> =
    filter { !it.isRecurring && it.dateTime == null }

fun List<ScheduleItem>.onDate(date: LocalDate): List<ScheduleItem> =
    filter { it.occursOn(date) }.sortedWith(
        compareBy(nullsLast()) { it.timeOfDay() }
    )

/** The seven days containing [date], starting Monday. */
fun weekOf(date: LocalDate): List<LocalDate> {
    val monday = date.minusDays((date.dayOfWeek.value - 1).toLong())
    return (0..6).map { monday.plusDays(it.toLong()) }
}
