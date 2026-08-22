package com.sricharan.dailyschedule.domain

import com.sricharan.dailyschedule.data.AlarmPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The snooze setting is user-facing and stored as a raw int, so the bounds are
 * worth pinning down — a zero-minute snooze would busy-loop the alarm and a
 * negative one would schedule it in the past.
 */
class SnoozeRangeTest {

    @Test
    fun `the offered range is one to twenty five minutes`() {
        assertEquals(1, AlarmPreferences.SNOOZE_RANGE.first)
        assertEquals(25, AlarmPreferences.SNOOZE_RANGE.last)
    }

    @Test
    fun `the default sits inside the range`() {
        assertTrue(AlarmPreferences.DEFAULT_SNOOZE_MINUTES in AlarmPreferences.SNOOZE_RANGE)
    }

    @Test
    fun `every offered value is a usable delay`() {
        AlarmPreferences.SNOOZE_RANGE.forEach { minutes ->
            assertTrue("$minutes should be positive", minutes * 60_000L > 0)
        }
    }

    @Test
    fun `the slider offers a stop for every whole minute`() {
        // steps excludes both endpoints, so the count must line up or the
        // slider would land on values the range doesn't contain.
        val steps = AlarmPreferences.SNOOZE_RANGE.count() - 2
        assertEquals(23, steps)
        assertEquals(AlarmPreferences.SNOOZE_RANGE.count(), steps + 2)
    }
}
