package com.sricharan.dailyschedule.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.sricharan.dailyschedule.data.Thought
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ENTRY_TIME = DateTimeFormatter.ofPattern("h:mm a")

private fun Thought.timeLabel(): String =
    Instant.ofEpochMilli(writtenAt)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .format(ENTRY_TIME)
        .lowercase()

/**
 * Somewhere to put a thought down the moment it turns up. Unlike the day's
 * reflection this stacks — several through a day is the normal case, and each
 * one keeps the time it was written so the day can be read back in order.
 */
@Composable
fun ThoughtsCard(
    thoughts: List<Thought>,
    onAdd: (String) -> Unit,
    onDelete: (Thought) -> Unit,
    modifier: Modifier = Modifier
) {
    var writing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var letting by remember { mutableStateOf<Thought?>(null) }

    letting?.let { thought ->
        ConfirmLetGo(
            onConfirm = { onDelete(thought); letting = null },
            onDismiss = { letting = null }
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Something on your mind?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Get it off your chest, write it out",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (thoughts.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                thoughts.forEachIndexed { index, thought ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                    ThoughtEntry(thought = thought, onLetGo = { letting = thought })
                }
            }

            Spacer(Modifier.height(if (thoughts.isEmpty()) 6.dp else 12.dp))

            if (writing) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Whatever it is. No one else reads this.") },
                    minLines = 3,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { draft = ""; writing = false }) {
                        Text("Never mind")
                    }
                    TextButton(
                        onClick = { onAdd(draft); draft = ""; writing = false },
                        enabled = draft.isNotBlank()
                    ) {
                        Text("Write it down")
                    }
                }
            } else {
                if (thoughts.isEmpty()) {
                    Text(
                        text = "Nothing here yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                }
                TextButton(
                    onClick = { writing = true },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(if (thoughts.isEmpty()) "Write something" else "Add another")
                }
            }
        }
    }
}

@Composable
private fun ThoughtEntry(thought: Thought, onLetGo: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = thought.timeLabel(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(3.dp))
            SelectionContainer {
                Text(
                    text = thought.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        IconButton(onClick = onLetGo, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Let this one go",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

/** Written words don't come back, so this one asks first. */
@Composable
private fun ConfirmLetGo(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
                Text(
                    text = "Let this thought go?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "It won't be kept anywhere after this.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Keep it") }
                    TextButton(onClick = onConfirm) {
                        Text(
                            text = "Let it go",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
