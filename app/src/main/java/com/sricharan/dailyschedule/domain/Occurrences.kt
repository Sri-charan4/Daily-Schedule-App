package com.sricharan.dailyschedule.domain

import com.sricharan.dailyschedule.data.ScheduleItem
import com.sricharan.dailyschedule.data.SkippedOccurrence
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
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

/** Identifies one day of one item, so skipped days can be looked up cheaply. */
fun skipKey(itemId: Long, date: LocalDate): String = "$itemId@${date.dateKey()}"

fun SkippedOccurrence.key(): String = "$scheduleItemId@$date"

/**
 * Does this item belong on [date]?
 *
 * [skipped] holds the individual days that were let go of one-at-a-time (see
 * [SkippedOccurrence]); a day in there is simply absent, with the underlying
 * routine left completely intact.
 */
fun ScheduleItem.occursOn(date: LocalDate, skipped: Set<String> = emptySet()): Boolean = when {
    skipKey(id, date) in skipped -> false
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

fun List<ScheduleItem>.onDate(
    date: LocalDate,
    skipped: Set<String> = emptySet()
): List<ScheduleItem> =
    filter { it.occursOn(date, skipped) }.sortedWith(
        compareBy(nullsLast()) { it.timeOfDay() }
    )

/** The seven days containing [date], starting Monday. */
fun weekOf(date: LocalDate): List<LocalDate> {
    val monday = date.minusDays((date.dayOfWeek.value - 1).toLong())
    return (0..6).map { monday.plusDays(it.toLong()) }
}

/**
 * Where a reminder lands when the item itself has no particular time. Late
 * enough not to wake anyone, early enough to still be worth knowing.
 */
val DEFAULT_REMINDER_TIME: LocalTime = LocalTime.of(9, 0)

fun LocalDateTime.toEpochMillis(): Long =
    atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

/**
 * The next moment this item should nudge, strictly after [after], or null if
 * it never will again.
 *
 * This is the only place reminder timing is decided. The initial scheduling,
 * the re-arming that happens once an alarm fires, and the rebuild after a
 * reboot all ask this same question, so they cannot drift apart.
 *
 * Days let go of individually are stepped over rather than fired, and a
 * one-off whose moment has passed returns null instead of nagging about
 * something already behind you.
 */
fun ScheduleItem.nextReminderAfter(
    after: LocalDateTime,
    skipped: Set<String> = emptySet()
): LocalDateTime? {
    if (!reminderEnabled) return null
    val time = timeOfDay() ?: DEFAULT_REMINDER_TIME

    if (!isRecurring) {
        val date = scheduledDate() ?: return null
        return date.atTime(time).takeIf { it.isAfter(after) }
    }

    // Walk forward far enough to clear any weekly pattern plus a long run of
    // skipped days, stopping at the first day that actually holds this item.
    var date = after.toLocalDate()
    repeat(SEARCH_HORIZON_DAYS) {
        if (occursOn(date, skipped)) {
            val moment = date.atTime(time)
            if (moment.isAfter(after)) return moment
        }
        date = date.plusDays(1)
    }
    return null
}

private const val SEARCH_HORIZON_DAYS = 400

/**
 * How long after a missed moment it's still worth mentioning it.
 *
 * Alarms don't survive the phone being off, so a 9am nudge on a phone that
 * boots at 10am is simply gone: [nextReminderAfter] looks forward, sees the
 * moment has passed, and quietly books tomorrow. Four hours is long enough to
 * cover an overnight charge or a flat battery, and short enough that a morning
 * routine never resurfaces in the evening as something to feel behind on.
 */
val MISSED_REMINDER_GRACE: Duration = Duration.ofHours(4)

/**
 * The moment this item *should* have nudged at, if that moment has already
 * passed but is still within [graceWindow] of [now]. Null when nothing was
 * missed, or when what was missed is old enough to leave alone.
 *
 * The counterpart to [nextReminderAfter]: that one looks forward to book the
 * next alarm, this one looks back to catch the one the system never delivered.
 * Both read the same item fields, so a day either arrives or is deliberately
 * let go of -- it can't fall between the two.
 */
fun ScheduleItem.missedReminderBefore(
    now: LocalDateTime,
    skipped: Set<String> = emptySet(),
    graceWindow: Duration = MISSED_REMINDER_GRACE
): LocalDateTime? {
    if (!reminderEnabled) return null
    val earliest = now.minus(graceWindow)
    val time = timeOfDay() ?: DEFAULT_REMINDER_TIME

    // A moment counts as missed if it is at or before now, and after the point
    // where we stop caring.
    fun LocalDateTime.wasMissed() = !isAfter(now) && isAfter(earliest)

    if (!isRecurring) {
        val date = scheduledDate() ?: return null
        return date.atTime(time).takeIf { it.wasMissed() }
    }

    // Walk backwards from today and stop at the first day that holds this
    // item -- that's the most recent occurrence, which is the only one worth
    // raising. A window measured in hours can only ever reach back a day or
    // two, plus one for the walk to land on it.
    var date = now.toLocalDate()
    repeat(graceWindow.toDays().toInt() + 2) {
        if (occursOn(date, skipped)) {
            val moment = date.atTime(time)
            if (moment.wasMissed()) return moment
            // Today's occurrence still being ahead of us says nothing about
            // yesterday's -- at 1am, an 11pm routine was missed last night.
            // So keep walking rather than stopping here.
        }
        date = date.minusDays(1)
    }
    return null
}
