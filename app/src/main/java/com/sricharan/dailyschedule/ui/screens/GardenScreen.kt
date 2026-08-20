package com.sricharan.dailyschedule.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sricharan.dailyschedule.domain.Plant
import com.sricharan.dailyschedule.ui.components.drawPlant
import com.sricharan.dailyschedule.ui.theme.ForestAccents
import com.sricharan.dailyschedule.viewmodel.ScheduleViewModel

/**
 * Progress, but only ever upward. There is no streak counter here, nothing
 * turns red or brown, and a routine you haven't touched in months simply sits
 * at the size it reached — waiting, not withering.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenScreen(
    viewModel: ScheduleViewModel,
    onBack: () -> Unit
) {
    val garden by viewModel.garden.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Your garden", style = MaterialTheme.typography.titleLarge) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column {
                        Meadow(
                            plants = garden.plants,
                            modifier = Modifier.fillMaxWidth().height(180.dp)
                        )
                        Text(
                            text = garden.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(18.dp)
                        )
                    }
                }
            }

            if (garden.plants.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "What's growing",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                items(garden.plants, key = { it.item.id }) { plant ->
                    PlantRow(plant)
                }
            }
        }
    }
}

@Composable
private fun PlantRow(plant: Plant) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(52.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawPlant(
                        stage = plant.stage,
                        center = Offset(size.width / 2f, size.height),
                        unit = size.minDimension / 2.6f
                    )
                }
            }
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plant.item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Now ${plant.stage.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                // "Tended" rather than "completed" — and no target to hit.
                text = "tended ${plant.timesTended}×",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

/** All the plants together on one patch of ground. */
@Composable
private fun Meadow(plants: List<Plant>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val groundY = size.height * 0.86f

        drawRect(
            color = ForestAccents.Soil.copy(alpha = 0.18f),
            topLeft = Offset(0f, groundY),
            size = Size(size.width, size.height - groundY)
        )

        if (plants.isEmpty()) return@Canvas

        // Spread whatever exists evenly across the patch.
        val shown = plants.take(9)
        val slot = size.width / (shown.size + 1)
        shown.forEachIndexed { index, plant ->
            val x = slot * (index + 1)
            // Slight vertical jitter so the row doesn't look machine-planted.
            val jitter = ((plant.item.id % 5) - 2) * 2f
            drawPlant(
                stage = plant.stage,
                center = Offset(x, groundY + jitter),
                unit = (size.height * 0.16f).coerceAtMost(slot * 0.5f)
            )
        }
    }
}
