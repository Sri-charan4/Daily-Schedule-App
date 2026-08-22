package com.sricharan.dailyschedule.domain

import com.sricharan.dailyschedule.data.ScheduleItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Catching up on a nudge the system never delivered.
 *
 * The risk being tested is a day going by in silence: alarms don't survive the
 * phone being off, and [nextReminderAfter] only ever looks forward, so without
 * this the morning's reminder is simply gone. The opposite mistake matters
 * just as much — resurfacing something already tended, or a day deliberately
 * let go of, turns a quiet app into a nagging one.
 */
class MissedReminderTest {

    private val wednesday = LocalDate.of(2026, 8, 19)

    private fun routine(
        days: String = "",
        time: String = "09:00",
        reminder: Boolean = true
    ) = ScheduleItem(
        id = 2,
        title = "Walk",
        isRecurring = true,
        recurrenceDays = days,
        recurrenceTime = time,
        reminderEnabled = reminder
    )

    private fun oneOff(at: LocalDateTime?, reminder: Boolean = true) = ScheduleItem(
        id = 1,
        title = "Dentist",
        dateTime = at?.toEpochMillis(),
        isRecurring = false,
        reminderEnabled = reminder
    )

    @Test
    fun `a routine whose time passed an hour ago is missed`() {
        val missed = routine().missedReminderBefore(wednesday.atTime(10, 0))
        assertEquals(wednesday.atTime(9, 0), missed)
    }

    @Test
    fun `a routine whose time has not come yet is not missed`() {
        assertNull(routine().missedReminderBefore(wednesday.atTime(8, 0)))
    }

    @Test
    fun `a routine older than the grace window is left alone`() {
        // 9am seen at 9pm: real, but raising it now would only make the
        // evening feel like a backlog.
        assertNull(routine().missedReminderBefore(wednesday.atTime(21, 0)))
    }

    @Test
    fun `the moment itself counts as missed`() {
        val missed = routine().missedReminderBefore(wednesday.atTime(9, 0))
        assertEquals(wednesday.atTime(9, 0), missed)
    }

    @Test
    fun `a day let go of is not delivered late`() {
        val item = routine()
        val skipped = setOf(skipKey(item.id, wednesday))
        assertNull(item.missedReminderBefore(wednesday.atTime(10, 0), skipped))
    }

    @Test
    fun `a routine that does not run today is not missed today`() {
        // Wednesday is 2026-08-19; this one only runs Mon and Fri.
        assertNull(routine(days = "MON,FRI").missedReminderBefore(wednesday.atTime(10, 0)))
    }

    @Test
    fun `reminders switched off are never delivered late`() {
        assertNull(routine(reminder = false).missedReminderBefore(wednesday.atTime(10, 0)))
    }

    @Test
    fun `a one-off from this morning is missed`() {
        val item = oneOff(wednesday.atTime(9, 0))
        assertEquals(wednesday.atTime(9, 0), item.missedReminderBefore(wednesday.atTime(11, 0)))
    }

    @Test
    fun `a one-off from last week is not resurrected`() {
        val item = oneOff(wednesday.minusDays(7).atTime(9, 0))
        assertNull(item.missedReminderBefore(wednesday.atTime(10, 0)))
    }

    @Test
    fun `a one-off with no date has nothing to miss`() {
        assertNull(oneOff(null).missedReminderBefore(wednesday.atTime(10, 0)))
    }

    @Test
    fun `an overnight window reaches back to yesterday`() {
        // Booted at 1am having been off since before yesterday's 11pm routine.
        val item = routine(time = "23:00")
        val missed = item.missedReminderBefore(
            wednesday.plusDays(1).atTime(1, 0),
            graceWindow = Duration.ofHours(4)
        )
        assertEquals(wednesday.atTime(23, 0), missed)
    }

    @Test
    fun `a routine with no time set uses the default reminder time`() {
        val item = routine(time = "")
        val missed = item.missedReminderBefore(wednesday.atTime(10, 0))
        assertEquals(wednesday.atTime(DEFAULT_REMINDER_TIME), missed)
    }

    @Test
    fun `nothing is both missed and next at once`() {
        // The two halves must not overlap: whatever is handed over as missed
        // must be strictly before whatever gets booked as the next alarm.
        val item = routine()
        val now = wednesday.atTime(10, 0)
        val missed = item.missedReminderBefore(now)!!
        val next = item.nextReminderAfter(now)!!
        assertEquals(true, missed.isBefore(next))
    }
}
