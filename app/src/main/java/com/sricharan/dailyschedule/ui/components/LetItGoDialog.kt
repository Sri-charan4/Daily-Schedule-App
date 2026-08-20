package com.sricharan.dailyschedule.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DAY_NAME = DateTimeFormatter.ofPattern("EEEE")

/**
 * Asked when letting go of something that repeats: skipping one day and ending
 * a routine are very different intentions, and the app shouldn't guess which
 * one you meant.
 *
 * The choices are stacked rather than sat side by side so the permanent one is
 * never a thumb's-width away from the harmless one.
 */
@Composable
fun LetItGoDialog(
    title: String,
    date: LocalDate,
    onSkipThisDay: () -> Unit,
    onDeleteEntirely: () -> Unit,
    onDismiss: () -> Unit
) {
    val dayLabel = if (date == LocalDate.now()) "today" else "on ${date.format(DAY_NAME)}"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Let this one go?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "“$title” comes back around. You can set it down just for " +
                        "this day, or end the routine altogether.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))

                TextButton(
                    onClick = onSkipThisDay,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Just $dayLabel",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                TextButton(
                    onClick = onDeleteEntirely,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Every day — remove it for good",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Never mind",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
