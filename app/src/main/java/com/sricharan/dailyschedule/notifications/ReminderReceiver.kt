package com.sricharan.dailyschedule.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sricharan.dailyschedule.data.AppDatabase
import com.sricharan.dailyschedule.domain.dateKey
import com.sricharan.dailyschedule.domain.key
import com.sricharan.dailyschedule.domain.skipKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Where an alarm lands. Two jobs, in this order of importance:
 *
 *  1. Say the thing.
 *  2. Book the next one — a routine keeps going only because each firing
 *     arranges its successor.
 *
 * Step 2 happens even when step 1 is deliberately skipped, so a day you'd
 * already dealt with can't quietly end the whole routine.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getLongExtra(ReminderScheduler.EXTRA_ITEM_ID, -1L)
        if (itemId < 0) return

        // Database work can't happen on the main thread, and a receiver is dead
        // the moment onReceive returns — goAsync() buys us that time.
        val pending = goAsync()
        val appContext = context.applicationContext

        scope.launch {
            try {
                val dao = AppDatabase.getInstance(appContext).scheduleDao()
                val item = dao.getItemById(itemId)

                if (item == null || !item.reminderEnabled) {
                    ReminderScheduler.cancel(appContext, itemId)
                    return@launch
                }

                val today = LocalDate.now()
                val skipped = dao.getAllSkipsOnce().map { it.key() }.toSet()

                val alreadyTended =
                    dao.getCompletionForDate(itemId, today.dateKey())?.isCompleted == true
                val letGoToday = skipKey(itemId, today) in skipped

                // Nothing to say if it's already done, or if this particular day
                // was set down on purpose.
                if (!alreadyTended && !letGoToday) {
                    Reminders.notify(appContext, item)
                }

                ReminderScheduler.scheduleNext(appContext, item, skipped)
            } catch (e: Exception) {
                Log.e(TAG, "Reminder for item $itemId failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "ReminderReceiver"
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
