package com.sricharan.dailyschedule.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sricharan.dailyschedule.data.ScheduleItem
import com.sricharan.dailyschedule.domain.nextReminderAfter
import com.sricharan.dailyschedule.domain.toEpochMillis
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
            if (Reminders.canScheduleExactAlarms(context)) {
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

    private const val TAG = "ReminderScheduler"
}
