package com.sricharan.dailyschedule.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sricharan.dailyschedule.R
import com.sricharan.dailyschedule.data.ScheduleItem
import com.sricharan.dailyschedule.ui.AlarmActivity

/**
 * The loud half of reminders.
 *
 * Deliberately separate from [Reminders] rather than a flag on it, because
 * almost nothing is shared: a different channel, different audio routing,
 * a different scheduling API, and a notification whose whole job is to launch
 * a full-screen activity rather than to be read in the shade.
 *
 * The channel matters more than it looks. USAGE_ALARM is what makes the sound
 * come out at alarm volume and survive Do Not Disturb — a notification-usage
 * sound is silenced by exactly the settings someone asleep would have on.
 */
object Alarms {

    const val CHANNEL_ID = "schedule_alarms"

    /** Ten minutes, the same as nearly every clock app, so it needs no explaining. */
    const val SNOOZE_MINUTES = 10L

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alarms",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "For the things you asked to be woken for."
            setSound(
                alarmSound(),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            // An alarm the user set is precisely the thing DND is not meant to
            // swallow, and the platform allows this for alarm-usage channels.
            setBypassDnd(true)
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    /** The user's chosen alarm tone, falling back to the ringtone, then any notification. */
    fun alarmSound(): Uri =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    val VIBRATION_PATTERN = longArrayOf(0, 600, 400, 600, 400)

    /**
     * The notification that carries the full-screen intent.
     *
     * Even for a full-screen alarm the notification is required — it is the
     * carrier, and it's also the entire fallback if the system declines to
     * launch the activity (permission refused, or the user is mid-call). So it
     * is built to stand on its own: ongoing, non-dismissable by swipe, and
     * showing the same two choices the alarm screen offers.
     */
    fun buildRingingNotification(
        context: Context,
        item: ScheduleItem
    ): android.app.Notification {
        val fullScreen = PendingIntent.getActivity(
            context,
            item.id.toInt(),
            AlarmActivity.intent(context, item.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(item.title)
            .setContentText(item.notes.ifBlank { "It's time." })
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(0, "Snooze", AlarmService.action(context, item.id, AlarmService.ACTION_SNOOZE))
            .addAction(0, "Dismiss", AlarmService.action(context, item.id, AlarmService.ACTION_DISMISS))
            .build()
    }

    /** Takes the ringing notification down once the alarm has been answered. */
    fun clear(context: Context, itemId: Long) {
        NotificationManagerCompat.from(context).cancel(itemId.toInt())
    }

    /**
     * Android 14+ made full-screen intents a permission for apps that aren't
     * declared alarm or calling apps. Without it the alarm degrades to a
     * heads-up notification that still rings — worth saying, not worth
     * blocking on.
     */
    fun canUseFullScreen(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 34) return true
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        return manager.canUseFullScreenIntent()
    }

    fun fullScreenSettingsIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < 34) return null
        return Intent(android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
