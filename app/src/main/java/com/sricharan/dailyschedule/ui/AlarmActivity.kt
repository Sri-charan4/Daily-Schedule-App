package com.sricharan.dailyschedule.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sricharan.dailyschedule.data.AppDatabase
import com.sricharan.dailyschedule.data.ScheduleItem
import com.sricharan.dailyschedule.notifications.AlarmService
import com.sricharan.dailyschedule.notifications.Alarms
import com.sricharan.dailyschedule.ui.theme.DailyScheduleTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * What a ringing alarm looks like.
 *
 * Shown over the lock screen, which is the only reason a full-screen alarm is
 * worth having at all — the flags below are what let it appear without the
 * phone being unlocked first.
 *
 * The sound is not started or stopped here. [AlarmService] owns it, so the
 * alarm behaves identically whether it was answered on this screen, from the
 * notification's buttons, or not at all.
 */
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()

        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
        if (itemId < 0) {
            finish()
            return
        }

        setContent {
            DailyScheduleTheme {
                var item by remember { mutableStateOf<ScheduleItem?>(null) }

                LaunchedEffect(itemId) {
                    item = withContext(Dispatchers.IO) {
                        runCatching {
                            AppDatabase.getInstance(applicationContext)
                                .scheduleDao()
                                .getItemById(itemId)
                        }.getOrNull()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RingingAlarm(
                        title = item?.title ?: "Time's up",
                        notes = item?.notes.orEmpty(),
                        onSnooze = {
                            send(itemId, AlarmService.ACTION_SNOOZE)
                            finish()
                        },
                        onDismiss = {
                            send(itemId, AlarmService.ACTION_DISMISS)
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun send(itemId: Long, action: String) {
        runCatching {
            startService(
                Intent(this, AlarmService::class.java)
                    .setAction(action)
                    .putExtra(AlarmService.EXTRA_ITEM_ID, itemId)
            )
        }
    }

    /**
     * Turn the screen on and show in front of the keyguard. The two API paths
     * do the same thing; the flags were deprecated in favour of the setters in
     * Android 8.1, and minSdk is below that.
     */
    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            getSystemService(KeyguardManager::class.java)?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    companion object {
        const val EXTRA_ITEM_ID = "itemId"

        fun intent(context: Context, itemId: Long): Intent =
            Intent(context, AlarmActivity::class.java).apply {
                putExtra(EXTRA_ITEM_ID, itemId)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NO_USER_ACTION
                )
            }
    }
}

@Composable
private fun RingingAlarm(
    title: String,
    notes: String,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    val now = remember { LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = now.lowercase(),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (notes.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(48.dp))

        // Dismiss is the larger, primary one: snoozing is the choice you make
        // half-asleep, and it shouldn't be the easiest thing to hit by accident.
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("I'm up", modifier = Modifier.padding(vertical = 8.dp))
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onSnooze,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(
                "Another ${Alarms.SNOOZE_MINUTES} minutes",
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}
