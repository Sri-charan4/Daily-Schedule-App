package com.sricharan.dailyschedule.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Android clears AlarmManager alarms on reboot. This receiver is the hook
 * where you'd re-read all reminder-enabled items from Room and re-schedule
 * them via ReminderScheduler.schedule(...) once the DB/repository is wired
 * up outside of Compose (e.g. via a small WorkManager one-off job).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // TODO: query all ScheduleItems with reminderEnabled = true and reschedule
        }
    }
}
