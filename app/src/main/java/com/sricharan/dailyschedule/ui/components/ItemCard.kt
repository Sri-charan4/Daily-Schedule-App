package com.sricharan.dailyschedule.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sricharan.dailyschedule.data.ScheduleItem
import com.sricharan.dailyschedule.domain.whenLabel

/**
 * One thing you might do. Completed items soften and settle rather than being
 * struck through — the visual language is "tended", not "crossed off".
 */
@Composable
fun ItemCard(
    item: ScheduleItem,
    isDone: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    val containerColor by animateColorAsState(
        targetValue = if (isDone) scheme.primaryContainer.copy(alpha = 0.5f) else scheme.surface,
        animationSpec = tween(500),
        label = "cardContainer"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (isDone) 0.62f else 1f,
        animationSpec = tween(500),
        label = "cardAlpha"
    )

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TendCircle(isDone = isDone, onToggle = onToggle)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f).alpha(contentAlpha)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurface
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = item.whenLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
                if (item.notes.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = item.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

/** A soft circle that fills like a leaf when tapped. No hard checkbox edges. */
@Composable
private fun TendCircle(isDone: Boolean, onToggle: () -> Unit) {
    val scheme = MaterialTheme.colorScheme

    val fill by animateColorAsState(
        targetValue = if (isDone) scheme.primary else Color.Transparent,
        animationSpec = tween(400),
        label = "tendFill"
    )
    val checkScale by animateFloatAsState(
        targetValue = if (isDone) 1f else 0f,
        animationSpec = tween(400),
        label = "tendCheck"
    )

    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(fill)
            .border(
                width = 1.5.dp,
                color = if (isDone) Color.Transparent else scheme.outline.copy(alpha = 0.55f),
                shape = CircleShape
            )
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = if (isDone) "Tended" else "Mark as tended",
            tint = scheme.onPrimary,
            modifier = Modifier.size(17.dp).scale(checkScale)
        )
    }
}
