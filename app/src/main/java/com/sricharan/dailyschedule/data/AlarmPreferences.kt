package com.sricharan.dailyschedule.data

import android.content.Context

/**
 * The small amount of alarm state that has to outlive the process.
 *
 * A ringing alarm is not one continuous thing — it is a series of short-lived
 * services, minutes apart, with nothing running in between. So "how many times
 * has this gone unanswered" and "is a snooze already pending" can't live in
 * memory. They live here, keyed per item, and are cleared the moment the alarm
 * is finally answered.
 *
 * SharedPreferences rather than Room on purpose: this is throwaway state about
 * an alarm in flight, not part of the schedule, and it has no business in a
 * backup.
 */
class AlarmPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("alarm_state", Context.MODE_PRIVATE)

    /** How long a snooze lasts, in minutes. The user's choice, within [SNOOZE_RANGE]. */
    var snoozeMinutes: Int
        get() = prefs.getInt(KEY_SNOOZE_MINUTES, DEFAULT_SNOOZE_MINUTES)
            .coerceIn(SNOOZE_RANGE)
        set(value) {
            prefs.edit().putInt(KEY_SNOOZE_MINUTES, value.coerceIn(SNOOZE_RANGE)).apply()
        }

    /** Rings for this item that timed out without anyone answering. */
    fun unansweredRings(itemId: Long): Int = prefs.getInt(ringsKey(itemId), 0)

    fun recordUnansweredRing(itemId: Long): Int {
        val next = unansweredRings(itemId) + 1
        prefs.edit().putInt(ringsKey(itemId), next).apply()
        return next
    }

    /**
     * When a snooze is due to go off, as epoch millis, or 0 if none is pending.
     *
     * Needed because a full resync rebuilds every alarm from the schedule, and
     * a snooze isn't in the schedule — without this, opening the app during a
     * snooze would quietly replace it with tomorrow's alarm.
     */
    fun snoozeUntil(itemId: Long): Long = prefs.getLong(snoozeKey(itemId), 0L)

    fun setSnoozeUntil(itemId: Long, atEpochMillis: Long) {
        prefs.edit().putLong(snoozeKey(itemId), atEpochMillis).apply()
    }

    /** Called once an alarm has been answered, skipped, or has given up. */
    fun clear(itemId: Long) {
        prefs.edit().remove(ringsKey(itemId)).remove(snoozeKey(itemId)).apply()
    }

    private fun ringsKey(itemId: Long) = "rings_$itemId"
    private fun snoozeKey(itemId: Long) = "snooze_until_$itemId"

    companion object {
        const val DEFAULT_SNOOZE_MINUTES = 10
        val SNOOZE_RANGE = 1..25

        private const val KEY_SNOOZE_MINUTES = "snooze_minutes"
    }
}
