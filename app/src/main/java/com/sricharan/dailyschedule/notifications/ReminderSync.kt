package com.sricharan.dailyschedule.notifications

import android.content.Context
import com.sricharan.dailyschedule.data.AppDatabase
import com.sricharan.dailyschedule.domain.key

/**
 * Rebuilds alarms from what's actually in the database.
 *
 * Alarms live in the system, not in the app, and the system loses them on
 * reboot, on an app update, and whenever the clock jumps. Rather than trying
 * to track every one of those cases individually, the app just rebuilds the
 * whole set from the only thing that is truly durable — the saved items.
 */
object ReminderSync {

    /** Re-books every reminder the database says should exist. */
    suspend fun syncAll(context: Context) {
        val dao = AppDatabase.getInstance(context).scheduleDao()
        val skipped = dao.getAllSkipsOnce().map { it.key() }.toSet()

        Reminders.ensureChannel(context)
        dao.getAllItemsOnce().forEach { item ->
            if (item.reminderEnabled) {
                ReminderScheduler.scheduleNext(context, item, skipped)
            } else {
                ReminderScheduler.cancel(context, item.id)
            }
        }
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
}
