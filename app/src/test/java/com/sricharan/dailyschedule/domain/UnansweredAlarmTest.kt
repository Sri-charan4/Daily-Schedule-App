package com.sricharan.dailyschedule.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * How long an alarm keeps trying before it gives up on the day.
 *
 * The failure this guards against is an alarm that never stops — ringing every
 * few minutes into an empty house until the battery goes. The opposite failure
 * matters too: giving up on the first unanswered ring makes the alarm useless
 * for the one case it exists for, which is someone genuinely asleep.
 */
class UnansweredAlarmTest {

    @Test
    fun `the first unanswered ring snoozes`() {
        assertEquals(UnansweredAlarm.SNOOZE, decideUnansweredAlarm(ringsSoFar = 1))
    }

    @Test
    fun `the second unanswered ring snoozes`() {
        assertEquals(UnansweredAlarm.SNOOZE, decideUnansweredAlarm(ringsSoFar = 2))
    }

    @Test
    fun `the third unanswered ring gives up`() {
        assertEquals(UnansweredAlarm.GIVE_UP, decideUnansweredAlarm(ringsSoFar = 3))
    }

    @Test
    fun `there is never a fourth ring`() {
        // The whole point of the rule: whatever happens, it stops.
        (4..10).forEach { rings ->
            assertEquals(UnansweredAlarm.GIVE_UP, decideUnansweredAlarm(ringsSoFar = rings))
        }
    }

    @Test
    fun `exactly three rings happen before giving up`() {
        val rings = generateSequence(1) { it + 1 }
            .takeWhile { decideUnansweredAlarm(it) == UnansweredAlarm.SNOOZE }
            .count() + 1
        assertEquals(MAX_UNANSWERED_RINGS, rings)
    }

    @Test
    fun `the limit is configurable for its own sake`() {
        assertEquals(UnansweredAlarm.SNOOZE, decideUnansweredAlarm(ringsSoFar = 4, limit = 5))
        assertEquals(UnansweredAlarm.GIVE_UP, decideUnansweredAlarm(ringsSoFar = 5, limit = 5))
    }

    @Test
    fun `a single ring limit gives up immediately`() {
        assertEquals(UnansweredAlarm.GIVE_UP, decideUnansweredAlarm(ringsSoFar = 1, limit = 1))
    }

    @Test
    fun `a ring lasts two minutes`() {
        // Long enough to wake someone, short enough that a mistaken alarm in a
        // meeting is survivable.
        assertEquals(2L, RING_DURATION_MINUTES)
    }
}
