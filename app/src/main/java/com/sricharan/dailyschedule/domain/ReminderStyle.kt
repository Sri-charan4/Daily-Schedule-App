package com.sricharan.dailyschedule.domain

import com.sricharan.dailyschedule.data.ScheduleItem

/**
 * How loudly, if at all, an item asks for attention.
 *
 * These are genuinely three different things rather than three volumes of the
 * same thing: [NONE] never schedules an alarm, [NUDGE] posts a quiet
 * notification and lets you find it whenever, and [ALARM] takes over the
 * screen and rings until answered. They use different scheduling APIs and
 * different notification channels, because the system treats "a reminder" and
 * "an alarm" as separate categories and so should we.
 */
enum class ReminderStyle {
    /** On the schedule, but it never interrupts. */
    NONE,

    /** A quiet notification, delivered once. Missable on purpose. */
    NUDGE,

    /** Rings at alarm volume, through Do Not Disturb, until dismissed. */
    ALARM;

    val label: String
        get() = when (this) {
            NONE -> "Nothing"
            NUDGE -> "A gentle nudge"
            ALARM -> "An alarm"
        }

    val description: String
        get() = when (this) {
            NONE -> "It sits on your day, quietly. Nothing will interrupt you."
            NUDGE -> "A quiet notification. Easy to miss, and that's alright."
            ALARM -> "Rings until you answer it, even on silent. For the ones that matter."
        }
}

/**
 * The single place the two stored booleans are turned back into a decision.
 *
 * Storage keeps them separate for backup compatibility; nothing outside this
 * file should read either one. An item that somehow has [ScheduleItem.alarmEnabled]
 * set without [ScheduleItem.reminderEnabled] is treated as silent, since
 * "don't remind me" is the safer reading of a contradiction.
 */
val ScheduleItem.reminderStyle: ReminderStyle
    get() = when {
        !reminderEnabled -> ReminderStyle.NONE
        alarmEnabled -> ReminderStyle.ALARM
        else -> ReminderStyle.NUDGE
    }

/** The only supported way to change it, so the pair can never drift apart. */
fun ScheduleItem.withReminderStyle(style: ReminderStyle): ScheduleItem = copy(
    reminderEnabled = style != ReminderStyle.NONE,
    alarmEnabled = style == ReminderStyle.ALARM
)
