package com.sricharan.dailyschedule.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Android drops every pending alarm on reboot, and again when the app is
 * updated. The clock or time zone moving also invalidates alarms that were
 * calculated against the old wall time.
 *
 * All four cases have the same answer: throw away what we think we know and
 * rebuild every alarm from the database.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val relevant = intent.action in setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )
        if (!relevant) return

        val pending = goAsync()
        val appContext = context.applicationContext

        scope.launch {
            try {
                ReminderSync.syncAll(appContext)
            } catch (e: Exception) {
                Log.e(TAG, "Could not rebuild reminders after ${intent.action}", e)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "BootReceiver"
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
