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
import com.sricharan.dailyschedule.viewmodel.ScheduleViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    viewModel: ScheduleViewModel,
    existingItemId: Long?,
    onDone: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var loaded by remember { mutableStateOf(existingItemId == null) }
    var existing by remember { mutableStateOf<ScheduleItem?>(null) }

    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isRecurring by remember { mutableStateOf(false) }
    var selectedDays by remember { mutableStateOf(setOf<String>()) }
    var recurrenceTime by remember { mutableStateOf("") }
    var reminderEnabled by remember { mutableStateOf(false) }

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
                recurrenceTime = item.recurrenceTime
                reminderEnabled = item.reminderEnabled
            }
            loaded = true
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
                SoftField(
                    value = recurrenceTime,
                    onValueChange = { recurrenceTime = it },
                    label = "Around what time?",
                    placeholder = "07:30 — or leave blank"
                )
            }

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
                        dateTime = existing?.dateTime,
                        isRecurring = isRecurring,
                        recurrenceDays = selectedDays.joinToString(","),
                        recurrenceTime = recurrenceTime.trim(),
                        reminderEnabled = reminderEnabled,
                        createdAt = existing?.createdAt ?: System.currentTimeMillis()
                    )
                    scope.launch {
                        viewModel.saveItem(item)
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
                        scope.launch {
                            viewModel.deleteItem(item)
                            onDone()
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
