package com.sricharan.dailyschedule.domain

import com.sricharan.dailyschedule.data.ScheduleItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Two stored booleans stand in for three states, which is a shape that invites
 * drift. These pin down the mapping in both directions, including the one
 * combination that shouldn't be reachable.
 */
class ReminderStyleTest {

    private fun item(reminder: Boolean, alarm: Boolean) = ScheduleItem(
        id = 1,
        title = "Take the bins out",
        reminderEnabled = reminder,
        alarmEnabled = alarm
    )

    @Test
    fun `no reminder means none`() {
        assertEquals(ReminderStyle.NONE, item(reminder = false, alarm = false).reminderStyle)
    }

    @Test
    fun `reminder without alarm is a nudge`() {
        assertEquals(ReminderStyle.NUDGE, item(reminder = true, alarm = false).reminderStyle)
    }

    @Test
    fun `reminder with alarm rings`() {
        assertEquals(ReminderStyle.ALARM, item(reminder = true, alarm = true).reminderStyle)
    }

    @Test
    fun `a contradiction is read as silence`() {
        // Shouldn't be reachable through withReminderStyle, but a hand-edited
        // backup could carry it. Silence is the safer reading.
        assertEquals(ReminderStyle.NONE, item(reminder = false, alarm = true).reminderStyle)
    }

    @Test
    fun `every style survives a round trip`() {
        val blank = item(reminder = false, alarm = false)
        ReminderStyle.entries.forEach { style ->
            assertEquals(style, blank.withReminderStyle(style).reminderStyle)
        }
    }

    @Test
    fun `switching down from alarm clears the alarm flag`() {
        val ringing = item(reminder = true, alarm = true)
        val quieted = ringing.withReminderStyle(ReminderStyle.NUDGE)
        assertTrue(quieted.reminderEnabled)
        assertFalse(quieted.alarmEnabled)
    }

    @Test
    fun `switching to none clears both flags`() {
        val silenced = item(reminder = true, alarm = true).withReminderStyle(ReminderStyle.NONE)
        assertFalse(silenced.reminderEnabled)
        assertFalse(silenced.alarmEnabled)
    }

    @Test
    fun `an alarm is still scheduled by the shared timing rules`() {
        // The style decides how it arrives, never whether it is booked at all —
        // both levels must go through the same nextReminderAfter.
        val alarm = ScheduleItem(
            id = 2,
            title = "Wake up",
            isRecurring = true,
            recurrenceTime = "06:30",
            reminderEnabled = true,
            alarmEnabled = true
        )
        val next = alarm.nextReminderAfter(java.time.LocalDate.of(2026, 8, 19).atTime(5, 0))
        assertEquals(java.time.LocalDate.of(2026, 8, 19).atTime(6, 30), next)
    }

    @Test
    fun `an item with reminders off is never booked whatever the alarm flag says`() {
        val contradictory = ScheduleItem(
            id = 3,
            title = "Nothing",
            isRecurring = true,
            recurrenceTime = "06:30",
            reminderEnabled = false,
            alarmEnabled = true
        )
        assertEquals(null, contradictory.nextReminderAfter(
            java.time.LocalDate.of(2026, 8, 19).atTime(5, 0)
        ))
    }
}
