package com.sricharan.dailyschedule.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sricharan.dailyschedule.domain.occursOn
import com.sricharan.dailyschedule.ui.components.BreathingMoment
import com.sricharan.dailyschedule.ui.components.ItemCard
import com.sricharan.dailyschedule.ui.components.ReflectionCard
import com.sricharan.dailyschedule.ui.components.TreeGlyph
import com.sricharan.dailyschedule.ui.components.WeekLabel
import com.sricharan.dailyschedule.ui.components.WeekStrip
import com.sricharan.dailyschedule.ui.theme.LocalTimeOfDay
import com.sricharan.dailyschedule.viewmodel.ScheduleViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DAY_HEADING = DateTimeFormatter.ofPattern("EEEE, d MMMM")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ScheduleViewModel,
    onAddClick: () -> Unit,
    onItemClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    onMonthClick: () -> Unit,
    onGardenClick: () -> Unit
) {
    val allItems by viewModel.allItems.collectAsState()
    val dayItems by viewModel.itemsForSelectedDate.collectAsState()
    val somedayItems by viewModel.somedayItems.collectAsState()
    val doneIds by viewModel.doneOnSelectedDate.collectAsState()
    val tendedDates by viewModel.tendedDates.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val reflection by viewModel.reflectionForSelectedDate.collectAsState()

    val timeOfDay = LocalTimeOfDay.current
    var breathing by remember { mutableStateOf(false) }

    if (breathing) {
        BreathingMoment(onDismiss = { breathing = false })
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Your days", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    IconButton(onClick = { breathing = true }) {
                        Icon(
                            Icons.Outlined.FavoriteBorder,
                            contentDescription = "Take a breath"
                        )
                    }
                    IconButton(onClick = onGardenClick) {
                        TreeGlyph(tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onMonthClick) {
                        Icon(Icons.Filled.DateRange, contentDescription = "Month view")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.height(0.dp))
                Text("  Add something")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column {
                    Text(
                        text = timeOfDay.greeting,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = timeOfDay.blurb,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(18.dp))
                }
            }

            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WeekLabel(selectedDate)
                        TextButton(onClick = onMonthClick) { Text("See the month") }
                    }
                    Spacer(Modifier.height(4.dp))
                    WeekStrip(
                        selectedDate = selectedDate,
                        hasItemsOn = { date -> allItems.any { it.occursOn(date) } },
                        tendedDates = tendedDates,
                        onSelect = viewModel::selectDate
                    )
                    Spacer(Modifier.height(18.dp))
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedDate == LocalDate.now()) "Today"
                        else selectedDate.format(DAY_HEADING),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (selectedDate != LocalDate.now()) {
                        TextButton(onClick = { viewModel.selectDate(LocalDate.now()) }) {
                            Text("Back to today")
                        }
                    }
                }
            }

            if (dayItems.isEmpty()) {
                item { EmptyDayNote(isToday = selectedDate == LocalDate.now()) }
            } else {
                items(dayItems, key = { it.id }) { item ->
                    ItemCard(
                        item = item,
                        isDone = item.id in doneIds,
                        onToggle = { viewModel.setDone(item, item.id !in doneIds) },
                        onClick = { onItemClick(item.id) }
                    )
                }
            }

            if (somedayItems.isNotEmpty()) {
                item {
                    Column {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Someday",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "No date on these. They'll wait as long as you need.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
                items(somedayItems, key = { "someday-${it.id}" }) { item ->
                    ItemCard(
                        item = item,
                        isDone = item.id in doneIds,
                        onToggle = { viewModel.setDone(item, item.id !in doneIds) },
                        onClick = { onItemClick(item.id) }
                    )
                }
            }

            item {
                Spacer(Modifier.height(14.dp))
                ReflectionCard(
                    savedNote = reflection?.note,
                    onSave = viewModel::saveReflection
                )
            }

            item {
                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = { breathing = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Feeling heavy? Take a moment.")
                }
            }
        }
    }
}

/**
 * An empty day is presented as space, not as a failure to plan. This is the
 * one place the app is most tempted to nag, so it deliberately doesn't.
 */
@Composable
private fun EmptyDayNote(isToday: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isToday) "Nothing planned today." else "Nothing planned for this day.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "An open day is a good thing to have.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )
    }
}
