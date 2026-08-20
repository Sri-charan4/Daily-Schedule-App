package com.sricharan.dailyschedule.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sricharan.dailyschedule.backup.BackupManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupManager = remember { BackupManager(context) }
    var isWorking by remember { mutableStateOf(false) }

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
