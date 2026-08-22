package com.sricharan.dailyschedule.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sricharan.dailyschedule.backup.BackupManager
import com.sricharan.dailyschedule.notifications.Reminders
import com.sricharan.dailyschedule.ui.components.rememberReminderPermissions
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.sricharan.dailyschedule.domain.RING_DURATION_MINUTES
import com.sricharan.dailyschedule.domain.MAX_UNANSWERED_RINGS
import com.sricharan.dailyschedule.data.AlarmPreferences
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.Slider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupManager = remember { BackupManager(context) }
    var isWorking by remember { mutableStateOf(false) }
    val permissions = rememberReminderPermissions()
    val alarmPrefs = remember { AlarmPreferences(context) }
    var snoozeMinutes by remember { mutableIntStateOf(alarmPrefs.snoozeMinutes) }

    val notificationRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            // Android stops showing the dialog after a refusal; the only route
            // left is the app's own settings page.
            context.startActivity(Reminders.notificationSettingsIntent(context))
        }
    }

    // Restore: user picks any file from their file manager / WhatsApp downloads / Drive etc.
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isWorking = true
        scope.launch {
            val result = backupManager.restoreFromUri(uri)
            isWorking = false
            result.onSuccess { count ->
                Toast.makeText(context, "Restored $count items", Toast.LENGTH_LONG).show()
            }.onFailure {
                Toast.makeText(context, "Restore failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Reminders",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                when {
                    permissions.allGood ->
                        "Reminders can reach you, at the times you set."
                    !permissions.canReachYou ->
                        "Notifications are switched off for this app, so no reminder " +
                            "can arrive at all. That one matters."
                    else ->
                        "Reminders will arrive. The settings below only affect how " +
                            "close to the minute they land."
                },
                style = MaterialTheme.typography.bodyMedium
            )

            PermissionRow(
                label = "Show notifications",
                granted = permissions.canNotify,
                fixLabel = "Allow",
                onFix = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        context.startActivity(Reminders.notificationSettingsIntent(context))
                    }
                }
            )
            PermissionRow(
                label = "Ring at the exact time",
                granted = permissions.canBeExact,
                fixLabel = "Allow",
                onFix = {
                    Reminders.exactAlarmSettingsIntent(context)?.let { context.startActivity(it) }
                }
            )
            if (!permissions.canBeExact) {
                Text(
                    "Without this, a nudge still arrives — usually within a few " +
                        "minutes of the time you chose, rather than exactly on it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            PermissionRow(
                label = "Keep running in the background",
                granted = permissions.isUnrestricted,
                fixLabel = "Settings",
                onFix = { context.startActivity(Reminders.batterySettingsIntent()) }
            )
            if (!permissions.isUnrestricted) {
                Text(
                    "Some phones — Samsung especially — put apps to sleep after a " +
                        "few days unused, which cancels their reminders. Choosing " +
                        "this app and setting it to \"Unrestricted\" prevents that.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // The whole point: reminders are otherwise unverifiable until the
            // moment they were meant to arrive, and a no-show tells you nothing
            // about which of the settings above was the reason.
            TextButton(
                onClick = {
                    val sent = Reminders.sendTest(context)
                    Toast.makeText(
                        context,
                        if (sent) {
                            "Sent — check your notification shade."
                        } else {
                            "Nothing was sent: notifications are switched off for this app."
                        },
                        Toast.LENGTH_LONG
                    ).show()
                }
            ) { Text("Send a test reminder") }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                "Alarms",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "An alarm rings for $RING_DURATION_MINUTES minutes. If you don't answer it, " +
                    "it waits and tries again — up to $MAX_UNANSWERED_RINGS times in all, " +
                    "and then it lets the day go rather than keep at you.",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = if (snoozeMinutes == 1) {
                    "Snooze for 1 minute"
                } else {
                    "Snooze for $snoozeMinutes minutes"
                },
                style = MaterialTheme.typography.bodyLarge
            )
            Slider(
                value = snoozeMinutes.toFloat(),
                onValueChange = { snoozeMinutes = it.roundToInt() },
                // Committed on release rather than on every drag, so the
                // setting is written once instead of two dozen times.
                onValueChangeFinished = { alarmPrefs.snoozeMinutes = snoozeMinutes },
                valueRange = AlarmPreferences.SNOOZE_RANGE.first.toFloat()..
                    AlarmPreferences.SNOOZE_RANGE.last.toFloat(),
                steps = AlarmPreferences.SNOOZE_RANGE.count() - 2,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                "Your data",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Everything in this app stays on your device. No account, no internet " +
                    "connection required. Use these to move your data between phones.",
                style = MaterialTheme.typography.bodyMedium
            )

            Button(
                onClick = {
                    isWorking = true
                    scope.launch {
                        val file = backupManager.createBackupFile()
                        isWorking = false
                        val shareIntent = backupManager.buildShareIntent(file)
                        context.startActivity(
                            android.content.Intent.createChooser(shareIntent, "Share backup via")
                        )
                    }
                },
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Backup")
            }

            OutlinedButton(
                onClick = {
                    restoreLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                },
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restore Backup")
            }

            if (isWorking) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** One permission, its current state, and the way to grant it if it's missing. */
@Composable
private fun PermissionRow(
    label: String,
    granted: Boolean,
    fixLabel: String,
    onFix: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (granted) "Allowed" else "Not allowed yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!granted) {
            TextButton(onClick = onFix) { Text(fixLabel) }
        }
    }
}
