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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupManager = remember { BackupManager(context) }
    var isWorking by remember { mutableStateOf(false) }
    val permissions = rememberReminderPermissions()

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
                if (permissions.allGood) {
                    "Reminders can reach you, at the times you set."
                } else {
                    "Android needs a couple of permissions before a reminder can " +
                        "actually arrive."
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
