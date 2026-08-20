package com.sricharan.dailyschedule.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sricharan.dailyschedule.domain.dateKey
import com.sricharan.dailyschedule.domain.occursOn
import com.sricharan.dailyschedule.domain.onDate
import com.sricharan.dailyschedule.ui.components.ItemCard
import com.sricharan.dailyschedule.viewmodel.ScheduleViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val MONTH_TITLE = DateTimeFormatter.ofPattern("MMMM yyyy")
private val DAY_HEADING = DateTimeFormatter.ofPattern("EEEE, d MMMM")

/**
 * The look-ahead view. Useful for seeing what's coming without the week strip's
 * narrow window — but styled to stay calm: no counts, no colour-coded urgency,
 * just soft dots where something exists.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthScreen(
    viewModel: ScheduleViewModel,
    onBack: () -> Unit,
    onItemClick: (Long) -> Unit
) {
    val allItems by viewModel.allItems.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val doneIds by viewModel.doneOnSelectedDate.collectAsState()
    val tendedDates by viewModel.tendedDates.collectAsState()

    var visibleMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    val dayItems = remember(allItems, selectedDate) { allItems.onDate(selectedDate) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Looking ahead", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                MonthHeader(
                    month = visibleMonth,
                    onPrevious = { visibleMonth = visibleMonth.minusMonths(1) },
                    onNext = { visibleMonth = visibleMonth.plusMonths(1) }
                )
            }

            item {
                MonthGrid(
                    month = visibleMonth,
                    selectedDate = selectedDate,
                    hasItemsOn = { date -> allItems.any { it.occursOn(date) } },
                    tendedDates = tendedDates,
                    onSelect = viewModel::selectDate
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedDate.format(DAY_HEADING),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (YearMonth.from(selectedDate) != visibleMonth ||
                        selectedDate != LocalDate.now()
                    ) {
                        TextButton(onClick = {
                            viewModel.selectDate(LocalDate.now())
                            visibleMonth = YearMonth.now()
                        }) { Text("Today") }
                    }
                }
            }

            if (dayItems.isEmpty()) {
                item {
                    Text(
                        text = "Nothing planned. Room to breathe.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                        textAlign = TextAlign.Center
                    )
                }
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
        }
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
        }
        Text(
            text = month.format(MONTH_TITLE),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Next month")
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    hasItemsOn: (LocalDate) -> Boolean,
    tendedDates: Set<String>,
    onSelect: (LocalDate) -> Unit
) {
    val firstOfMonth = month.atDay(1)
    // Monday-first grid; pad the leading days of the first week.
    val leadingBlanks = firstOfMonth.dayOfWeek.value - 1
    val daysInMonth = month.lengthOfMonth()
    val cells: List<LocalDate?> = buildList {
        repeat(leadingBlanks) { add(null) }
        for (day in 1..daysInMonth) add(month.atDay(day))
        while (size % 7 != 0) add(null)
    }

    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (date == null) {
                            Spacer(Modifier.aspectRatio(1f))
                        } else {
                            MonthDayCell(
                                date = date,
                                isSelected = date == selectedDate,
                                isToday = date == LocalDate.now(),
                                hasItems = hasItemsOn(date),
                                wasTended = date.dateKey() in tendedDates,
                                onClick = { onSelect(date) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthDayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    hasItems: Boolean,
    wasTended: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    val background by animateColorAsState(
        targetValue = when {
            isSelected -> scheme.primary
            isToday -> scheme.primaryContainer
            else -> Color.Transparent
        },
        animationSpec = tween(350),
        label = "monthCellBackground"
    )
    val contentColor = when {
        isSelected -> scheme.onPrimary
        isToday -> scheme.onPrimaryContainer
        else -> scheme.onSurface
    }

    Column(
        modifier = Modifier
            .padding(2.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier.size(4.dp).clip(CircleShape).background(
                when {
                    wasTended -> contentColor.copy(alpha = 0.9f)
                    hasItems -> contentColor.copy(alpha = 0.32f)
                    else -> Color.Transparent
                }
            )
        )
    }
}
