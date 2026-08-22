package com.sricharan.dailyschedule.domain

/**
 * What to do when an alarm has rung its full length and nobody answered.
 *
 * Pulled out of the service because it is the one genuinely arguable rule in
 * the whole alarm feature — how many times is persistent, and how many is
 * nagging — and because the service it lives in is untestable without a
 * device.
 */
enum class UnansweredAlarm {
    /** Go quiet for a while and try again. */
    SNOOZE,

    /**
     * Stop for today. Ringing into an empty room a fourth time isn't going to
     * work either, and an alarm that never gives up is one you learn to
     * distrust. The day is let go of rather than left silently undone.
     */
    GIVE_UP
}

/**
 * At most [limit] rings, then the day is set down.
 *
 * [ringsSoFar] counts rings that have already timed out unanswered, including
 * the one just finished — so with a limit of 3 the sequence is ring, snooze,
 * ring, snooze, ring, give up. A fourth never happens.
 */
fun decideUnansweredAlarm(
    ringsSoFar: Int,
    limit: Int = MAX_UNANSWERED_RINGS
): UnansweredAlarm =
    if (ringsSoFar >= limit) UnansweredAlarm.GIVE_UP else UnansweredAlarm.SNOOZE

/** Three is enough to catch someone genuinely asleep, and short of badgering. */
const val MAX_UNANSWERED_RINGS = 3

/** How long a single ring lasts before it counts as unanswered. */
const val RING_DURATION_MINUTES = 2L
