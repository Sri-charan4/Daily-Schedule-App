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
import android.os.PowerManager
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

    /**
     * Whether a notification posted right now would actually appear.
     *
     * Two separate things can stop it, and only checking one of them is how a
     * reminder ends up firing into nothing: Android 13+ has a runtime
     * permission, but on *every* version the user can also switch this app's
     * notifications off in system settings. areNotificationsEnabled() is the
     * check that covers both, so it comes first and applies at any API level.
     */
    fun canPostNotifications(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
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

    /**
     * Whether the system has this app exempt from battery optimisation.
     *
     * Not required for reminders to work — [android.app.AlarmManager] handles
     * ordinary Doze on its own. It matters because aggressive OEM battery
     * managers (Samsung's "put unused apps to sleep" especially) will stop the
     * app outright, and an exempt app is generally left alone.
     */
    fun isUnrestricted(context: Context): Boolean {
        val power = context.getSystemService(PowerManager::class.java) ?: return true
        return power.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * The system's battery-optimisation list.
     *
     * Deliberately the list rather than a direct "exempt me" prompt: that
     * prompt needs REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, which Play restricts
     * to a short list of app types this one isn't clearly on. Walking the user
     * to the screen costs a tap and carries no policy risk.
     */
    fun batterySettingsIntent(): Intent =
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    /**
     * Posts a notification right now, by the same route a real reminder takes.
     *
     * Reminders are otherwise unverifiable until the moment they were meant to
     * arrive — and if they don't, there's nothing to see. Returns false when
     * the system would have swallowed it, so the UI can say so instead of
     * leaving the user watching for something that was never sent.
     */
    fun sendTest(context: Context): Boolean {
        if (!canPostNotifications(context)) return false
        ensureChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("This is what a nudge looks like")
            .setContentText("If you can see this, reminders can reach you.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .build()

        return runCatching {
            NotificationManagerCompat.from(context).notify(TEST_NOTIFICATION_ID, notification)
        }.isSuccess
    }

    /** Well clear of item ids, which are what every real reminder uses. */
    private const val TEST_NOTIFICATION_ID = Int.MAX_VALUE

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
