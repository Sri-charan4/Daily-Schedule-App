package com.sricharan.dailyschedule.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sricharan.dailyschedule.data.ScheduleItem
import com.sricharan.dailyschedule.domain.DAY_CODES
import com.sricharan.dailyschedule.domain.recurrenceDaySet
import com.sricharan.dailyschedule.domain.scheduledDate
import com.sricharan.dailyschedule.domain.timeOfDay
import com.sricharan.dailyschedule.ui.components.DateChoiceDialog
import com.sricharan.dailyschedule.ui.components.LetItGoDialog
import com.sricharan.dailyschedule.ui.components.PickerRow
import com.sricharan.dailyschedule.ui.components.TimeChoiceDialog
import com.sricharan.dailyschedule.ui.components.soften
import com.sricharan.dailyschedule.viewmodel.ScheduleViewModel
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val STORED_TIME = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    viewModel: ScheduleViewModel,
    existingItemId: Long?,
    onDone: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val selectedDate by viewModel.selectedDate.collectAsState()

    var loaded by remember { mutableStateOf(existingItemId == null) }
    var existing by remember { mutableStateOf<ScheduleItem?>(null) }

    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isRecurring by remember { mutableStateOf(false) }
    var selectedDays by remember { mutableStateOf(setOf<String>()) }
    var reminderEnabled by remember { mutableStateOf(false) }

    // Something new lands on the day you were looking at — that's almost always
    // the day you meant. "Someday" stays one tap away in the date picker.
    var hasDate by remember { mutableStateOf(true) }
    var chosenDate by remember { mutableStateOf(selectedDate) }
    var chosenTime by remember { mutableStateOf<LocalTime?>(null) }

    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }
    var lettingGo by remember { mutableStateOf(false) }

    // Actually load the item being edited, rather than silently starting blank
    // and saving a duplicate (which is what this screen used to do).
    LaunchedEffect(existingItemId) {
        if (existingItemId != null) {
            viewModel.loadItem(existingItemId)?.let { item ->
                existing = item
                title = item.title
                notes = item.notes
                isRecurring = item.isRecurring
                selectedDays = item.recurrenceDaySet()
                reminderEnabled = item.reminderEnabled
                hasDate = item.dateTime != null
                chosenDate = item.scheduledDate() ?: selectedDate
                chosenTime = item.timeOfDay()
            }
            loaded = true
        }
    }

    if (pickingDate) {
        DateChoiceDialog(
            initial = if (hasDate) chosenDate else null,
            onDismiss = { pickingDate = false },
            onClear = { hasDate = false; pickingDate = false },
            onPick = { date -> chosenDate = date; hasDate = true; pickingDate = false }
        )
    }

    if (pickingTime) {
        TimeChoiceDialog(
            initial = chosenTime,
            onDismiss = { pickingTime = false },
            onClear = { chosenTime = null; pickingTime = false },
            onPick = { time -> chosenTime = time; pickingTime = false }
        )
    }

    existing?.let { item ->
        if (lettingGo) {
            LetItGoDialog(
                title = item.title,
                date = selectedDate,
                onSkipThisDay = {
                    lettingGo = false
                    viewModel.skipOnSelectedDate(item)
                    onDone()
                },
                onDeleteEntirely = {
                    lettingGo = false
                    viewModel.deleteItem(item)
                    onDone()
                },
                onDismiss = { lettingGo = false }
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (existingItemId == null) "Something new" else "Adjust this",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SoftField(
                value = title,
                onValueChange = { title = it },
                label = "What is it?",
                placeholder = "Morning walk, call a friend…"
            )

            SoftField(
                value = notes,
                onValueChange = { notes = it },
                label = "Anything to remember",
                placeholder = "Optional",
                minLines = 3
            )

            ToggleRow(
                title = "Repeat this",
                subtitle = "Comes back on the days you choose.",
                checked = isRecurring,
                onCheckedChange = { isRecurring = it }
            )

            if (isRecurring) {
                Text(
                    text = "On these days",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DayChips(selectedDays) { day, selected ->
                    selectedDays = if (selected) selectedDays + day else selectedDays - day
                }
                Text(
                    text = "Leave them all unpicked and it'll simply be there every day.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            } else {
                PickerRow(
                    label = "Which day",
                    value = if (hasDate) chosenDate.soften() else "Someday — no date on it",
                    onClick = { pickingDate = true }
                )
            }

            PickerRow(
                label = "Around what time",
                value = chosenTime?.soften() ?: "No set time",
                onClick = { pickingTime = true }
            )

            ToggleRow(
                title = "A gentle nudge",
                subtitle = "A quiet notification. Never a repeated alarm.",
                checked = reminderEnabled,
                onCheckedChange = { reminderEnabled = it }
            )

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    val item = ScheduleItem(
                        id = existing?.id ?: 0L,
                        title = title.trim(),
                        notes = notes.trim(),
                        category = existing?.category ?: "General",
                        dateTime = when {
                            // A routine is described by its days, not by a date.
                            isRecurring -> null
                            !hasDate -> null
                            else -> chosenDate
                                .atTime(chosenTime ?: LocalTime.MIDNIGHT)
                                .atZone(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()
                        },
                        isRecurring = isRecurring,
                        recurrenceDays = selectedDays.joinToString(","),
                        recurrenceTime = if (isRecurring) {
                            chosenTime?.format(STORED_TIME).orEmpty()
                        } else "",
                        reminderEnabled = reminderEnabled,
                        createdAt = existing?.createdAt ?: System.currentTimeMillis()
                    )
                    scope.launch {
                        viewModel.saveItem(item)
                        // Follow it to wherever it landed, so it isn't saved onto
                        // a day you then can't see.
                        if (!isRecurring && hasDate) viewModel.selectDate(chosenDate)
                        onDone()
                    }
                },
                enabled = loaded && title.isNotBlank(),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Text("Keep it")
            }

            existing?.let { item ->
                TextButton(
                    onClick = {
                        // A repeating item asks first: one day off and ending the
                        // routine are two very different things.
                        if (item.isRecurring) {
                            lettingGo = true
                        } else {
                            scope.launch {
                                viewModel.deleteItem(item)
                                onDone()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Let this one go",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SoftField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        minLines = minLines,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DayChips(selectedDays: Set<String>, onToggle: (String, Boolean) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        DAY_CODES.forEach { day ->
            val selected = day in selectedDays
            FilterChip(
                selected = selected,
                onClick = { onToggle(day, !selected) },
                label = { Text(day.take(1)) },
                shape = MaterialTheme.shapes.small,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
