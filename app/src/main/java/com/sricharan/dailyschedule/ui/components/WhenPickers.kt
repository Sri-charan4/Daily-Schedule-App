package com.sricharan.dailyschedule.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val DATE_LABEL = DateTimeFormatter.ofPattern("EEEE, d MMMM")
private val TIME_LABEL = DateTimeFormatter.ofPattern("h:mm a")

/** How a chosen time reads on screen — lowercase, unhurried. */
fun LocalTime.soften(): String = format(TIME_LABEL).lowercase()

fun LocalDate.soften(): String = format(DATE_LABEL)

/**
 * A tappable row that shows what's currently chosen. Used instead of a text
 * field so a date or time is always picked, never typed and never mistyped.
 */
@Composable
fun PickerRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * The clock face. [onClear] is offered right alongside it, because "no
 * particular time" is a real answer rather than a failure to answer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeChoiceDialog(
    initial: LocalTime?,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onPick: (LocalTime) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initial?.hour ?: 8,
        initialMinute = initial?.minute ?: 0
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Around what time?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(18.dp))
                TimePicker(state = state)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onClear) { Text("No set time") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Never mind") }
                    TextButton(onClick = { onPick(LocalTime.of(state.hour, state.minute)) }) {
                        Text("Set it")
                    }
                }
            }
        }
    }
}

/**
 * The calendar. Clearing the date sends something to "Someday" rather than
 * deleting it — an intention with no date is still an intention.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateChoiceDialog(
    initial: LocalDate?,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onPick: (LocalDate) -> Unit
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (initial ?: LocalDate.now()).toEpochDay() * MILLIS_PER_DAY
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onPick(LocalDate.ofEpochDay(millis / MILLIS_PER_DAY))
                    } ?: onDismiss()
                }
            ) { Text("Set it") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onClear) { Text("Someday") }
                TextButton(onClick = onDismiss) { Text("Never mind") }
            }
        }
    ) {
        DatePicker(state = state, title = null)
    }
}

/**
 * The date picker works in UTC-midnight millis regardless of where you are,
 * so days convert straight through epoch-day rather than a local time zone.
 */
private const val MILLIS_PER_DAY = 86_400_000L
