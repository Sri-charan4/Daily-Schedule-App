package com.sricharan.dailyschedule.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.Canvas
import com.sricharan.dailyschedule.ui.theme.ForestAccents

/** One full breath cycle: in, hold, out, rest. Slow on purpose. */
private const val BREATH_CYCLE_MS = 12_000

/**
 * A pause, not a feature. There's no timer, no count of how many breaths you
 * took, and nothing is recorded — you close it whenever you want.
 */
@Composable
fun BreathingMoment(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "A moment",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Follow the circle if you'd like. Or just watch it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(28.dp))

                BreathingCircle()

                Spacer(Modifier.height(28.dp))
                TextButton(onClick = onDismiss) {
                    Text("I'm ready")
                }
            }
        }
    }
}

@Composable
private fun BreathingCircle() {
    val transition = rememberInfiniteTransition(label = "breath")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(BREATH_CYCLE_MS, easing = LinearEasing)
        ),
        label = "breathPhase"
    )

    // 0.00-0.33 inhale, 0.33-0.50 hold, 0.50-0.83 exhale, 0.83-1.00 rest.
    val scale = when {
        phase < 0.33f -> 0.6f + (phase / 0.33f) * 0.4f
        phase < 0.50f -> 1f
        phase < 0.83f -> 1f - ((phase - 0.50f) / 0.33f) * 0.4f
        else -> 0.6f
    }
    val label = when {
        phase < 0.33f -> "breathe in"
        phase < 0.50f -> "hold"
        phase < 0.83f -> "breathe out"
        else -> "rest"
    }

    val glow = MaterialTheme.colorScheme.primary

    Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(200.dp).scale(scale)) {
            val radius = size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glow.copy(alpha = 0.35f),
                        glow.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = radius
                ),
                radius = radius
            )
            drawCircle(
                color = ForestAccents.Leaf.copy(alpha = 0.55f),
                radius = radius * 0.72f,
                style = Stroke(width = 2.5f)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
