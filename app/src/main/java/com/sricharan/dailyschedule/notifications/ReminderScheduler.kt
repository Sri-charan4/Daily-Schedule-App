package com.sricharan.dailyschedule.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sricharan.dailyschedule.data.ScheduleItem
import com.sricharan.dailyschedule.domain.ReminderStyle
import com.sricharan.dailyschedule.domain.nextReminderAfter
import com.sricharan.dailyschedule.domain.reminderStyle
import com.sricharan.dailyschedule.domain.toEpochMillis
import android.app.PendingIntent as AndroidPendingIntent
import java.time.LocalDateTime

/**
 * Puts a single alarm in the system's hands.
 *
 * Only ever one alarm per item is outstanding: the next one. When it fires,
 * [ReminderReceiver] books the following one. That keeps a routine going
 * indefinitely without asking AlarmManager to hold hundreds of future alarms,
 * and means an edit only ever has one alarm to replace.
 */
object ReminderScheduler {

    const val EXTRA_ITEM_ID = "itemId"

    /** Marks the periodic self-check apart from a real item's alarm. */
    const val ACTION_HEARTBEAT = "com.sricharan.dailyschedule.action.HEARTBEAT"

    /**
     * Books this item's next nudge. Returns the moment it was set for, or null
     * if there's nothing left to remind about.
     */
    fun scheduleNext(
        context: Context,
        item: ScheduleItem,
        skipped: Set<String>,
        from: LocalDateTime = LocalDateTime.now()
    ): LocalDateTime? {
        cancel(context, item.id)

        val next = item.nextReminderAfter(from, skipped) ?: return null
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return null
        val triggerAt = next.toEpochMillis()

        try {
            if (item.reminderStyle == ReminderStyle.ALARM) {
                // The strongest thing AlarmManager offers: always exact, and
                // exempt from Doze and battery saver rather than merely allowed
                // through them. It also puts the alarm icon in the status bar,
                // which is the honest signal that something is set to ring.
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAt, showAlarmIntent(context)),
                    pendingIntent(context, item.id)
                )
            } else if (Reminders.canScheduleExactAlarms(context)) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent(context, item.id)
                )
            } else {
                // Not permitted to be exact — still worth arriving late rather
                // than not arriving at all.
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent(context, item.id)
                )
            }
        } catch (e: SecurityException) {
            // The permission can be revoked between the check and the call.
            Log.w(TAG, "Exact alarm refused for item ${item.id}, falling back", e)
            runCatching {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent(context, item.id)
                )
            }
        }
        return next
    }

    /**
     * A slow, repeating tick that just rebuilds every alarm from the database.
     *
     * The one-alarm-at-a-time chain has no redundancy: each firing books its
     * successor, so a single lost firing ends that item's reminders for good.
     * Plenty of things lose one -- an OEM battery manager killing the process
     * mid-delivery, the system dropping an alarm under pressure. This is the
     * net under that. It's deliberately inexact and infrequent, because being
     * a few hours late to notice a problem costs nothing.
     *
     * It cannot help after a user force-stop: Android cancels every alarm the
     * app holds and blocks its receivers until the app is opened by hand.
     * Nothing scheduled from inside the app can survive that.
     */
    fun scheduleHeartbeat(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        runCatching {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + AlarmManager.INTERVAL_HALF_DAY,
                AlarmManager.INTERVAL_HALF_DAY,
                heartbeatIntent(context)
            )
        }.onFailure { Log.w(TAG, "Could not book the heartbeat", it) }
    }

    /**
     * Re-rings one item a short while from now.
     *
     * Deliberately not routed through [nextReminderAfter]: a snooze is a fixed
     * offset from the moment it was asked for, not a recalculation of the
     * item's schedule. The next real occurrence gets booked when the alarm is
     * finally dismissed.
     */
    fun scheduleSnooze(
        context: Context,
        itemId: Long,
        minutes: Long = Alarms.SNOOZE_MINUTES
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        runCatching {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAt, showAlarmIntent(context)),
                pendingIntent(context, itemId)
            )
        }.onFailure { Log.w(TAG, "Could not snooze item $itemId", it) }
    }

    fun cancel(context: Context, itemId: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(pendingIntent(context, itemId))
    }

    /**
     * Request code is the item's own id, so re-scheduling an item always
     * replaces its previous alarm instead of stacking a second one.
     */
    private fun pendingIntent(context: Context, itemId: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            itemId.toInt(),
            Intent(context, ReminderReceiver::class.java).apply {
                putExtra(EXTRA_ITEM_ID, itemId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /**
     * Its own request code, far away from any item id, and an action that no
     * item alarm sets -- so the heartbeat can never replace a real reminder.
     */
    private fun heartbeatIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            HEARTBEAT_REQUEST_CODE,
            Intent(context, ReminderReceiver::class.java).setAction(ACTION_HEARTBEAT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /**
     * Where the system's own "next alarm" chip goes when tapped. Opening the
     * app is the truthful answer — that's where the alarm can be changed.
     */
    private fun showAlarmIntent(context: Context): AndroidPendingIntent =
        AndroidPendingIntent.getActivity(
            context,
            SHOW_ALARM_REQUEST_CODE,
            Intent(context, com.sricharan.dailyschedule.MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            AndroidPendingIntent.FLAG_UPDATE_CURRENT or AndroidPendingIntent.FLAG_IMMUTABLE
        )

    private const val SHOW_ALARM_REQUEST_CODE = Int.MIN_VALUE + 1
    private const val HEARTBEAT_REQUEST_CODE = Int.MIN_VALUE
    private const val TAG = "ReminderScheduler"
}
