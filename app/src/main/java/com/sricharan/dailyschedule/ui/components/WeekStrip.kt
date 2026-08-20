package com.sricharan.dailyschedule.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sricharan.dailyschedule.domain.dateKey
import com.sricharan.dailyschedule.domain.weekOf
import java.time.LocalDate

/**
 * A week at a glance. Days that have something on them get a soft dot rather
 * than a count — a number invites comparison, a dot just says "something's
 * here". Past days look no different from future ones; nothing is "missed".
 */
@Composable
fun WeekStrip(
    selectedDate: LocalDate,
    hasItemsOn: (LocalDate) -> Boolean,
    tendedDates: Set<String>,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val week = weekOf(selectedDate)
    val today = LocalDate.now()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        week.forEach { date ->
            DayPebble(
                date = date,
                isSelected = date == selectedDate,
                isToday = date == today,
                hasItems = hasItemsOn(date),
                wasTended = date.dateKey() in tendedDates,
                onClick = { onSelect(date) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DayPebble(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    hasItems: Boolean,
    wasTended: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    val background by animateColorAsState(
        targetValue = when {
            isSelected -> scheme.primary
            isToday -> scheme.primaryContainer
            else -> Color.Transparent
        },
        animationSpec = tween(400),
        label = "pebbleBackground"
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            isSelected -> scheme.onPrimary
            isToday -> scheme.onPrimaryContainer
            else -> scheme.onSurfaceVariant
        },
        animationSpec = tween(400),
        label = "pebbleContent"
    )
    val verticalPadding by animateDpAsState(
        targetValue = if (isSelected) 12.dp else 9.dp,
        animationSpec = tween(400),
        label = "pebblePadding"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = verticalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = date.dayOfWeek.name.take(1),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = contentColor,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(5.dp))
        // Tended days get a filled dot, days with plans get a hollow one,
        // empty days get nothing at all — no empty-state guilt.
        Box(
            modifier = Modifier.size(5.dp).clip(CircleShape).background(
                when {
                    wasTended -> contentColor.copy(alpha = 0.9f)
                    hasItems -> contentColor.copy(alpha = 0.35f)
                    else -> Color.Transparent
                }
            )
        )
    }
}

/** Small header showing the month/year for whichever week is in view. */
@Composable
fun WeekLabel(selectedDate: LocalDate, modifier: Modifier = Modifier) {
    val week = weekOf(selectedDate)
    val first = week.first()
    val last = week.last()
    val label = if (first.month == last.month) {
        "${first.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${first.year}"
    } else {
        val a = first.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        val b = last.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        "$a – $b ${last.year}"
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
    }
}
