package com.sricharan.dailyschedule.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sricharan.dailyschedule.MainActivity
import com.sricharan.dailyschedule.R
import com.sricharan.dailyschedule.data.ScheduleItem

/**
 * Everything to do with actually reaching the user: the channel, the two
 * permissions Android wants before any of this works, and the notification.
 *
 * Both permissions are checked rather than assumed. A reminder that silently
 * does nothing is worse than one the app admits it can't deliver, so the UI
 * asks these same questions and says so plainly.
 */
object Reminders {

    const val CHANNEL_ID = "schedule_reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Reminders",
            // High so a nudge can actually surface while the phone is in use.
            // The app never repeats or re-alerts; one quiet arrival is it.
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Gentle nudges for the things you asked to be reminded about."
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    /** Android 13+ won't show anything until the user has said yes. */
    fun canPostNotifications(context: Context): Boolean =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            true
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }

    /**
     * Android 12+ treats exact alarms as a privilege the user grants in system
     * settings. Without it a reminder still arrives, just whenever the system
     * feels like batching it.
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    /** The system screen where the user turns exact alarms on. */
    fun exactAlarmSettingsIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /** This app's notification settings, for when a request was already refused. */
    fun notificationSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun notify(context: Context, item: ScheduleItem) {
        if (!canPostNotifications(context)) return
        ensureChannel(context)

        val openApp = PendingIntent.getActivity(
            context,
            item.id.toInt(),
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            // The item's own words, not the app shouting "REMINDER" at you.
            .setContentTitle(item.title)
            .setContentText(item.notes.ifBlank { "A gentle nudge." })
            .setStyle(NotificationCompat.BigTextStyle().bigText(item.notes.ifBlank { "A gentle nudge." }))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()

        // Guarded above, but the platform still wants the try on 13+.
        runCatching {
            NotificationManagerCompat.from(context).notify(item.id.toInt(), notification)
        }
    }
}
