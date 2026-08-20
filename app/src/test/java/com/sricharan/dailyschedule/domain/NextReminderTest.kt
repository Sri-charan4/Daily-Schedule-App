package com.sricharan.dailyschedule.domain

import com.sricharan.dailyschedule.data.ScheduleItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * The alarm chain is only as good as this calculation: every firing books the
 * next one from it, so an off-by-one here doesn't just misfire once, it ends
 * the routine silently.
 */
class NextReminderTest {

    private val wednesday = LocalDate.of(2026, 8, 19)
    private fun at(date: LocalDate, h: Int, m: Int = 0): LocalDateTime = date.atTime(h, m)

    private fun oneOff(
        at: LocalDateTime?,
        reminder: Boolean = true
    ) = ScheduleItem(
        id = 1,
        title = "Dentist",
        dateTime = at?.toEpochMillis(),
        isRecurring = false,
        reminderEnabled = reminder
    )

    private fun routine(
        days: String,
        time: String,
        reminder: Boolean = true
    ) = ScheduleItem(
        id = 2,
        title = "Walk",
        isRecurring = true,
        recurrenceDays = days,
        recurrenceTime = time,
        reminderEnabled = reminder
    )

    // --- one-off items ------------------------------------------------------

    @Test
    fun `one-off in the future returns its own moment`() {
        val item = oneOff(at(wednesday, 14, 30))
        assertEquals(
            at(wednesday, 14, 30),
            item.nextReminderAfter(at(wednesday, 9, 0))
        )
    }

    @Test
    fun `one-off already past never fires again`() {
        val item = oneOff(at(wednesday, 8, 0))
        assertNull(item.nextReminderAfter(at(wednesday, 9, 0)))
    }

    @Test
    fun `one-off with a day but no time falls back to the default hour`() {
        // Midnight is how "no particular time" is stored.
        val item = oneOff(at(wednesday.plusDays(1), 0, 0))
        assertEquals(
            wednesday.plusDays(1).atTime(DEFAULT_REMINDER_TIME),
            item.nextReminderAfter(at(wednesday, 9, 0))
        )
    }

    @Test
    fun `someday item has nothing to count down to`() {
        assertNull(oneOff(null).nextReminderAfter(at(wednesday, 9, 0)))
    }

    // --- routines -----------------------------------------------------------

    @Test
    fun `daily routine fires later the same day when the time is still ahead`() {
        val item = routine(days = "", time = "18:00")
        assertEquals(
            at(wednesday, 18, 0),
            item.nextReminderAfter(at(wednesday, 9, 0))
        )
    }

    @Test
    fun `daily routine rolls to tomorrow once today's time has passed`() {
        val item = routine(days = "", time = "07:00")
        assertEquals(
            at(wednesday.plusDays(1), 7, 0),
            item.nextReminderAfter(at(wednesday, 9, 0))
        )
    }

    @Test
    fun `weekly routine finds its next chosen day`() {
        // Wednesday 19th; next Friday is the 21st.
        val item = routine(days = "MON,FRI", time = "07:00")
        assertEquals(
            at(LocalDate.of(2026, 8, 21), 7, 0),
            item.nextReminderAfter(at(wednesday, 9, 0))
        )
    }

    @Test
    fun `weekly routine wraps around the end of the week`() {
        // Wednesday; the only chosen day is Monday, so it wraps to the 24th.
        val item = routine(days = "MON", time = "07:00")
        assertEquals(
            at(LocalDate.of(2026, 8, 24), 7, 0),
            item.nextReminderAfter(at(wednesday, 9, 0))
        )
    }

    @Test
    fun `a let-go day is stepped over, not fired`() {
        val item = routine(days = "", time = "07:00")
        val tomorrow = wednesday.plusDays(1)
        val skipped = setOf(skipKey(item.id, tomorrow))

        assertEquals(
            at(wednesday.plusDays(2), 7, 0),
            item.nextReminderAfter(at(wednesday, 9, 0), skipped)
        )
    }

    @Test
    fun `several let-go days in a row are all stepped over`() {
        val item = routine(days = "", time = "07:00")
        val skipped = (1..5).map { skipKey(item.id, wednesday.plusDays(it.toLong())) }.toSet()

        assertEquals(
            at(wednesday.plusDays(6), 7, 0),
            item.nextReminderAfter(at(wednesday, 9, 0), skipped)
        )
    }

    @Test
    fun `routine with no time set uses the default hour`() {
        val item = routine(days = "", time = "")
        assertEquals(
            wednesday.atTime(DEFAULT_REMINDER_TIME),
            item.nextReminderAfter(at(wednesday, 7, 0))
        )
    }

    // --- the switch itself --------------------------------------------------

    @Test
    fun `nothing is scheduled when the nudge is switched off`() {
        assertNull(
            oneOff(at(wednesday, 14, 0), reminder = false)
                .nextReminderAfter(at(wednesday, 9, 0))
        )
        assertNull(
            routine(days = "", time = "18:00", reminder = false)
                .nextReminderAfter(at(wednesday, 9, 0))
        )
    }

    @Test
    fun `the moment returned is always strictly in the future`() {
        val item = routine(days = "", time = "09:00")
        // Firing exactly on time must book tomorrow, not re-book right now —
        // otherwise the alarm chain spins on the same instant forever.
        assertEquals(
            at(wednesday.plusDays(1), 9, 0),
            item.nextReminderAfter(at(wednesday, 9, 0))
        )
    }
}
