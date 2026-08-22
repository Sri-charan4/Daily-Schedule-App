package com.sricharan.dailyschedule.notifications

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.sricharan.dailyschedule.data.AlarmPreferences
import com.sricharan.dailyschedule.data.AppDatabase
import com.sricharan.dailyschedule.data.SkippedOccurrence
import com.sricharan.dailyschedule.domain.RING_DURATION_MINUTES
import com.sricharan.dailyschedule.domain.UnansweredAlarm
import com.sricharan.dailyschedule.domain.dateKey
import com.sricharan.dailyschedule.domain.decideUnansweredAlarm
import com.sricharan.dailyschedule.domain.key
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Holds a ringing alarm up for as long as it takes to answer it.
 *
 * A notification's sound plays once and stops, which is fine for a nudge and
 * useless for an alarm — so the sound is played here instead, looping, and the
 * service exists to keep the process alive while it does. Being a foreground
 * service is what stops the system reclaiming us mid-ring.
 *
 * This is also the only place that decides an alarm is over, whether that came
 * from the full-screen screen, the notification's buttons, or the item being
 * deleted underneath us.
 */
class AlarmService : Service() {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var ringingItemId: Long = -1L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val itemId = intent?.getLongExtra(EXTRA_ITEM_ID, -1L) ?: -1L

        when (intent?.action) {
            ACTION_DISMISS -> {
                answer(itemId, snooze = false)
                return START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                answer(itemId, snooze = true)
                return START_NOT_STICKY
            }
        }

        if (itemId < 0) {
            stopSelf()
            return START_NOT_STICKY
        }
        startRinging(itemId)
        return START_STICKY
    }

    private fun startRinging(itemId: Long) {
        ringingItemId = itemId
        Alarms.ensureChannel(this)

        scope.launch {
            val item = runCatching {
                AppDatabase.getInstance(applicationContext).scheduleDao().getItemById(itemId)
            }.getOrNull()

            if (item == null) {
                // Deleted between the alarm being set and it going off.
                stopSelf()
                return@launch
            }

            val notification = Alarms.buildRingingNotification(applicationContext, item)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        itemId.toInt(),
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(itemId.toInt(), notification)
                }
            }.onFailure {
                Log.e(TAG, "Could not go foreground for item $itemId", it)
                stopSelf()
                return@launch
            }

            beginSound()
        }
    }

    private fun beginSound() {
        // Screen-off is the case this whole feature exists for, so take a wake
        // lock rather than trusting the alarm to have woken anything up.
        runCatching {
            val power = getSystemService(PowerManager::class.java)
            wakeLock = power?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "YourDays:alarm"
            )?.apply { acquire(RING_DURATION_MINUTES * 60_000L + WAKE_LOCK_SLACK_MS) }
        }

        runCatching {
            player = MediaPlayer().apply {
                setDataSource(applicationContext, Alarms.alarmSound())
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        }.onFailure { Log.e(TAG, "Alarm sound failed", it) }

        runCatching {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Vibrator::class.java)
            }
            vibrator?.vibrate(
                VibrationEffect.createWaveform(Alarms.VIBRATION_PATTERN, 0),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
            )
        }

        // Two minutes of ringing, then it decides for itself whether to try
        // again later or leave the day alone.
        scope.launch {
            kotlinx.coroutines.delay(RING_DURATION_MINUTES * 60_000L)
            val id = ringingItemId
            if (id >= 0) wentUnanswered(id)
        }
    }

    /**
     * Ends the ring, one way or the other.
     *
     * Dismissing leaves the routine entirely alone — no completion is written
     * and no day is skipped, because silencing an alarm says nothing about
     * whether the thing got done. The next occurrence is re-booked either way,
     * which is what keeps a daily alarm daily.
     */
    private fun answer(itemId: Long, snooze: Boolean) {
        val id = if (itemId >= 0) itemId else ringingItemId
        stopSound()

        scope.launch {
            try {
                val context = applicationContext
                Alarms.clear(context, id)

                if (snooze) {
                    // A snooze the user asked for isn't evidence they're
                    // absent, so the unanswered run starts over.
                    AlarmPreferences(context).clear(id)
                    ReminderScheduler.scheduleSnooze(context, id)
                } else {
                    AlarmPreferences(context).clear(id)
                    val dao = AppDatabase.getInstance(context).scheduleDao()
                    val item = dao.getItemById(id)
                    if (item != null) {
                        ReminderScheduler.scheduleNext(
                            context,
                            item,
                            dao.getAllSkipsOnce().map { it.key() }.toSet(),
                            LocalDateTime.now()
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Could not settle alarm for item $id", e)
            } finally {
                ringingItemId = -1L
                stopForegroundCompat()
                stopSelf()
            }
        }
    }

    /**
     * Nobody picked this up within the ring window.
     *
     * Counting happens here rather than in the alarm itself because each ring
     * is a separate service instance — the count is the only thread connecting
     * them, and it has to survive the process dying in between.
     */
    private fun wentUnanswered(itemId: Long) {
        stopSound()

        scope.launch {
            try {
                val context = applicationContext
                val prefs = AlarmPreferences(context)
                val rings = prefs.recordUnansweredRing(itemId)

                Alarms.clear(context, itemId)

                when (decideUnansweredAlarm(rings)) {
                    UnansweredAlarm.SNOOZE -> {
                        Log.i(TAG, "Alarm $itemId unanswered ($rings), snoozing")
                        ReminderScheduler.scheduleSnooze(context, itemId)
                    }
                    UnansweredAlarm.GIVE_UP -> {
                        Log.i(TAG, "Alarm $itemId unanswered $rings times, letting the day go")
                        giveUpOnToday(itemId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Could not settle unanswered alarm $itemId", e)
            } finally {
                ringingItemId = -1L
                stopForegroundCompat()
                stopSelf()
            }
        }
    }

    /**
     * Sets today's occurrence down and moves on to the next one.
     *
     * Recorded as an ordinary skip — the same thing "let it go for today"
     * writes — so the routine survives intact, the day simply isn't counted
     * against you, and the Garden treats it exactly as it treats a day you
     * chose to let go of.
     */
    private suspend fun giveUpOnToday(itemId: Long) {
        val context = applicationContext
        val dao = AppDatabase.getInstance(context).scheduleDao()
        val prefs = AlarmPreferences(context)

        val today = LocalDate.now()
        runCatching { dao.insertSkip(SkippedOccurrence(itemId, today.dateKey())) }
        prefs.clear(itemId)

        val item = dao.getItemById(itemId) ?: return
        ReminderScheduler.scheduleNext(
            context,
            item,
            dao.getAllSkipsOnce().map { it.key() }.toSet(),
            LocalDateTime.now()
        )
    }

    private fun stopSound() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        runCatching { vibrator?.cancel() }
        vibrator = null
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        wakeLock = null
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        stopSound()
        super.onDestroy()
    }

    companion object {
        const val ACTION_DISMISS = "com.sricharan.dailyschedule.action.ALARM_DISMISS"
        const val ACTION_SNOOZE = "com.sricharan.dailyschedule.action.ALARM_SNOOZE"
        const val EXTRA_ITEM_ID = "itemId"

        /** A little past the ring itself, so the hand-off to a snooze isn't cut short. */
        private const val WAKE_LOCK_SLACK_MS = 30_000L
        private const val TAG = "AlarmService"

        fun start(context: Context, itemId: Long) {
            val intent = Intent(context, AlarmService::class.java)
                .putExtra(EXTRA_ITEM_ID, itemId)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }.onFailure { Log.e(TAG, "Could not start the alarm service", it) }
        }

        /** A button on the notification, or on the full-screen alarm itself. */
        fun action(context: Context, itemId: Long, action: String): PendingIntent {
            val intent = Intent(context, AlarmService::class.java)
                .setAction(action)
                .putExtra(EXTRA_ITEM_ID, itemId)
            // Distinct request codes so snooze and dismiss don't overwrite one
            // another, and neither collides with another item's pair.
            val request = (itemId.toInt() * 2) + if (action == ACTION_SNOOZE) 1 else 0
            return PendingIntent.getService(
                context,
                request,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
