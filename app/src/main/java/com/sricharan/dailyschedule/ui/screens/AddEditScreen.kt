package com.sricharan.dailyschedule.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sricharan.dailyschedule.data.ScheduleItem
import com.sricharan.dailyschedule.domain.DAY_CODES
import com.sricharan.dailyschedule.domain.DEFAULT_REMINDER_TIME
import com.sricharan.dailyschedule.domain.recurrenceDaySet
import com.sricharan.dailyschedule.domain.scheduledDate
import com.sricharan.dailyschedule.domain.timeOfDay
import com.sricharan.dailyschedule.ui.components.DateChoiceDialog
import com.sricharan.dailyschedule.ui.components.LetItGoDialog
import com.sricharan.dailyschedule.notifications.Reminders
import com.sricharan.dailyschedule.ui.components.PickerRow
import com.sricharan.dailyschedule.ui.components.TimeChoiceDialog
import com.sricharan.dailyschedule.domain.ReminderStyle
import com.sricharan.dailyschedule.domain.reminderStyle
import com.sricharan.dailyschedule.ui.components.rememberReminderPermissions
import com.sricharan.dailyschedule.ui.components.soften
import com.sricharan.dailyschedule.viewmodel.ScheduleViewModel
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.layout.width
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.selection.selectable

private val STORED_TIME = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    viewModel: ScheduleViewModel,
    existingItemId: Long?,
    onDone: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val selectedDate by viewModel.selectedDate.collectAsState()
    val permissions = rememberReminderPermissions()

    // Android 13+ shows nothing at all until this is granted, so it's asked for
    // at the moment the user says they want to be nudged — not on first launch,
    // where it would arrive with no explanation.
    val notificationRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Either way the note below reflects the outcome. */ }

    var loaded by remember { mutableStateOf(existingItemId == null) }
    var existing by remember { mutableStateOf<ScheduleItem?>(null) }

    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isRecurring by remember { mutableStateOf(false) }
    var selectedDays by remember { mutableStateOf(setOf<String>()) }
    var reminderStyle by remember { mutableStateOf(ReminderStyle.NONE) }

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
                reminderStyle = item.reminderStyle
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

            ReminderStylePicker(
                selected = reminderStyle,
                onSelect = { wanted ->
                    reminderStyle = wanted
                    if (wanted != ReminderStyle.NONE &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !permissions.canNotify
                    ) {
                        notificationRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )

            if (reminderStyle != ReminderStyle.NONE) {
                ReminderNote(
                    style = reminderStyle,
                    arrivesAt = (chosenTime ?: DEFAULT_REMINDER_TIME).soften(),
                    usingDefaultTime = chosenTime == null,
                    hasSomewhereToLand = isRecurring || hasDate,
                    canNotify = permissions.canNotify,
                    // An alarm books through setAlarmClock, which is exact by
                    // definition — the exact-alarm caveat only applies to nudges.
                    canBeExact = permissions.canBeExact || reminderStyle == ReminderStyle.ALARM,
                    onFixNotifications = {
                        context.startActivity(Reminders.notificationSettingsIntent(context))
                    },
                    onFixExactAlarms = {
                        Reminders.exactAlarmSettingsIntent(context)
                            ?.let { context.startActivity(it) }
                    }
                )
            }

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
                        reminderEnabled = reminderStyle != ReminderStyle.NONE,
                        alarmEnabled = reminderStyle == ReminderStyle.ALARM,
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
private fun ReminderStylePicker(
    selected: ReminderStyle,
    onSelect: (ReminderStyle) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "When it's time",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))

        // Laid out top to bottom in increasing insistence, so picking the loud
        // one is a deliberate step down the list rather than a default.
        ReminderStyle.entries.forEach { style ->
            val isSelected = style == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(style) }
                    )
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = isSelected, onClick = null)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = style.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = style.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
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

/**
 * Says exactly what will happen, including when it won't.
 *
 * A reminder that quietly fails is the worst outcome here, so anything the
 * system is currently blocking is stated plainly with the way to fix it,
 * rather than left for the user to discover by not being reminded.
 */
@Composable
private fun ReminderNote(
    style: ReminderStyle,
    arrivesAt: String,
    usingDefaultTime: Boolean,
    hasSomewhereToLand: Boolean,
    canNotify: Boolean,
    canBeExact: Boolean,
    onFixNotifications: () -> Unit,
    onFixExactAlarms: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (!hasSomewhereToLand) {
                Text(
                    text = "This hasn't got a day yet, so there's nothing to count " +
                        "down to. Give it a day, or let it repeat, and the nudge " +
                        "will find it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = when {
                        style == ReminderStyle.ALARM && usingDefaultTime ->
                            "No time set, so this will ring at $arrivesAt."
                        style == ReminderStyle.ALARM ->
                            "This will ring at $arrivesAt, and keep ringing until you answer."
                        usingDefaultTime -> "No time set, so this will arrive at $arrivesAt."
                        else -> "This will arrive at $arrivesAt."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!canNotify) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Notifications are switched off for this app, so nothing " +
                        "can reach you yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = onFixNotifications,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) { Text("Turn them on") }
            }

            if (!canBeExact) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Exact timing isn't allowed yet, so this will usually " +
                        "arrive within a few minutes of the time you chose " +
                        "rather than exactly on it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = onFixExactAlarms,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) { Text("Allow exact timing") }
            }
        }
    }
}
