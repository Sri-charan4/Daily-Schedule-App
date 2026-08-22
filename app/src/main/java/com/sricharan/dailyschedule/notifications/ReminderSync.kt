package com.sricharan.dailyschedule.notifications

import android.content.Context
import android.util.Log
import com.sricharan.dailyschedule.data.ScheduleDao
import com.sricharan.dailyschedule.data.ScheduleItem
import com.sricharan.dailyschedule.data.AppDatabase
import com.sricharan.dailyschedule.domain.dateKey
import com.sricharan.dailyschedule.domain.key
import com.sricharan.dailyschedule.domain.missedReminderBefore
import com.sricharan.dailyschedule.domain.skipKey
import java.time.LocalDateTime

/**
 * Rebuilds alarms from what's actually in the database.
 *
 * Alarms live in the system, not in the app, and the system loses them on
 * reboot, on an app update, and whenever the clock jumps. Rather than trying
 * to track every one of those cases individually, the app just rebuilds the
 * whole set from the only thing that is truly durable — the saved items.
 */
object ReminderSync {

    /**
     * Re-books every reminder the database says should exist.
     *
     * [deliverMissed] also hands over anything whose moment passed while the
     * alarm couldn't run — after a reboot, or when the heartbeat finds a
     * broken chain. It's off by default because the other caller is the app
     * starting up, and notifying someone who is already looking at the app is
     * just noise.
     */
    suspend fun syncAll(context: Context, deliverMissed: Boolean = false) {
        val dao = AppDatabase.getInstance(context).scheduleDao()
        val skipped = dao.getAllSkipsOnce().map { it.key() }.toSet()
        val now = LocalDateTime.now()

        Reminders.ensureChannel(context)
        Alarms.ensureChannel(context)
        dao.getAllItemsOnce().forEach { item ->
            if (item.reminderEnabled) {
                if (deliverMissed) deliverIfMissed(context, dao, item, skipped, now)
                ReminderScheduler.scheduleNext(context, item, skipped, now)
            } else {
                ReminderScheduler.cancel(context, item.id)
            }
        }
        ReminderScheduler.scheduleHeartbeat(context)
    }

    /** The same, for a single item that was just saved, skipped, or edited. */
    suspend fun syncOne(context: Context, itemId: Long) {
        val dao = AppDatabase.getInstance(context).scheduleDao()
        val item = dao.getItemById(itemId)

        if (item == null || !item.reminderEnabled) {
            ReminderScheduler.cancel(context, itemId)
            return
        }
        Reminders.ensureChannel(context)
        ReminderScheduler.scheduleNext(
            context,
            item,
            dao.getAllSkipsOnce().map { it.key() }.toSet()
        )
    }

    /**
     * Posts a nudge whose moment slipped past while nothing was running.
     *
     * Held to the same two conditions the live alarm applies, so a day already
     * dealt with stays dealt with: nothing arrives for something tended, and
     * nothing arrives for a day deliberately let go of.
     */
    private suspend fun deliverIfMissed(
        context: Context,
        dao: ScheduleDao,
        item: ScheduleItem,
        skipped: Set<String>,
        now: LocalDateTime
    ) {
        val missed = item.missedReminderBefore(now, skipped) ?: return
        val date = missed.toLocalDate()

        val alreadyTended = dao.getCompletionForDate(item.id, date.dateKey())?.isCompleted == true
        if (alreadyTended || skipKey(item.id, date) in skipped) return

        Log.i(TAG, "Delivering missed reminder for item ${item.id}, due $missed")

        // A missed alarm arrives as a notification rather than ringing. The
        // moment it was for has gone; starting a full alarm hours late would
        // be alarming in the wrong sense, and there is nothing left to be
        // woken up for.
        Reminders.notify(context, item)
    }

    private const val TAG = "ReminderSync"
}
