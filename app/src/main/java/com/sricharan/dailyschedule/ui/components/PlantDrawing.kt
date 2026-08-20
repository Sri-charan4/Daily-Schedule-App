package com.sricharan.dailyschedule.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.sricharan.dailyschedule.domain.GrowthStage
import com.sricharan.dailyschedule.ui.theme.ForestAccents

/**
 * Draws one plant standing on [center], scaled by [unit]. Stages differ in
 * size and fullness so growth reads at a glance without any numbers.
 */
fun DrawScope.drawPlant(stage: GrowthStage, center: Offset, unit: Float) {
    val trunkColor = ForestAccents.Trunk
    val leaf = ForestAccents.Leaf
    val deepLeaf = ForestAccents.DeepLeaf
    val sprout = ForestAccents.Sprout

    when (stage) {
        GrowthStage.SEED -> {
            drawCircle(
                color = ForestAccents.Soil.copy(alpha = 0.75f),
                radius = unit * 0.22f,
                center = Offset(center.x, center.y - unit * 0.12f)
            )
        }

        GrowthStage.SPROUT -> {
            drawLine(
                color = sprout,
                start = center,
                end = Offset(center.x, center.y - unit * 0.75f),
                strokeWidth = unit * 0.10f
            )
            drawCircle(
                color = sprout,
                radius = unit * 0.20f,
                center = Offset(center.x - unit * 0.20f, center.y - unit * 0.62f)
            )
            drawCircle(
                color = sprout,
                radius = unit * 0.20f,
                center = Offset(center.x + unit * 0.20f, center.y - unit * 0.75f)
            )
        }

        GrowthStage.SAPLING -> {
            drawLine(
                color = trunkColor,
                start = center,
                end = Offset(center.x, center.y - unit * 1.0f),
                strokeWidth = unit * 0.12f
            )
            drawCircle(
                color = leaf,
                radius = unit * 0.42f,
                center = Offset(center.x, center.y - unit * 1.15f)
            )
        }

        GrowthStage.YOUNG -> {
            drawLine(
                color = trunkColor,
                start = center,
                end = Offset(center.x, center.y - unit * 1.25f),
                strokeWidth = unit * 0.16f
            )
            drawCircle(
                color = leaf,
                radius = unit * 0.62f,
                center = Offset(center.x, center.y - unit * 1.5f)
            )
            drawCircle(
                color = deepLeaf.copy(alpha = 0.55f),
                radius = unit * 0.34f,
                center = Offset(center.x - unit * 0.28f, center.y - unit * 1.34f)
            )
        }

        GrowthStage.GROWN -> {
            drawLine(
                color = trunkColor,
                start = center,
                end = Offset(center.x, center.y - unit * 1.5f),
                strokeWidth = unit * 0.20f
            )
            drawCircle(
                color = leaf,
                radius = unit * 0.85f,
                center = Offset(center.x, center.y - unit * 1.9f)
            )
            drawCircle(
                color = deepLeaf.copy(alpha = 0.5f),
                radius = unit * 0.5f,
                center = Offset(center.x - unit * 0.42f, center.y - unit * 1.68f)
            )
        }

        GrowthStage.ELDER -> {
            drawLine(
                color = trunkColor,
                start = center,
                end = Offset(center.x, center.y - unit * 1.7f),
                strokeWidth = unit * 0.26f
            )
            drawCircle(
                color = deepLeaf,
                radius = unit * 1.05f,
                center = Offset(center.x, center.y - unit * 2.2f)
            )
            drawCircle(
                color = leaf.copy(alpha = 0.85f),
                radius = unit * 0.62f,
                center = Offset(center.x - unit * 0.62f, center.y - unit * 1.92f)
            )
            drawCircle(
                color = leaf.copy(alpha = 0.85f),
                radius = unit * 0.55f,
                center = Offset(center.x + unit * 0.62f, center.y - unit * 2.05f)
            )
        }
    }
}

/**
 * A little tree for the toolbar — Material's core icon set has no leaf or
 * plant, and a gear would be a strange way in to a garden.
 */
@Composable
fun TreeGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val trunkWidth = size.minDimension * 0.11f
        drawLine(
            color = tint,
            start = Offset(size.width / 2f, size.height * 0.92f),
            end = Offset(size.width / 2f, size.height * 0.55f),
            strokeWidth = trunkWidth
        )
        drawCircle(
            color = tint,
            radius = size.minDimension * 0.27f,
            center = Offset(size.width / 2f, size.height * 0.38f)
        )
        drawCircle(
            color = tint.copy(alpha = 0.75f),
            radius = size.minDimension * 0.18f,
            center = Offset(size.width * 0.30f, size.height * 0.50f)
        )
        drawCircle(
            color = tint.copy(alpha = 0.75f),
            radius = size.minDimension * 0.16f,
            center = Offset(size.width * 0.70f, size.height * 0.52f)
        )
    }
}
