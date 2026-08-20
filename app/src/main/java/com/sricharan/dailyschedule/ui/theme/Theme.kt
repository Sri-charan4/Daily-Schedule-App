package com.sricharan.dailyschedule.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import java.time.LocalTime

/** Lets any screen ask "what does the light look like right now?" */
val LocalTimeOfDay = compositionLocalOf { TimeOfDay.DAY }

/**
 * Re-reads the clock every minute so the palette drifts on its own while the
 * app is open — dusk shouldn't wait for a restart.
 */
@Composable
private fun rememberTimeOfDay(): TimeOfDay {
    var timeOfDay by remember { mutableStateOf(TimeOfDay.fromHour(LocalTime.now().hour)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            timeOfDay = TimeOfDay.fromHour(LocalTime.now().hour)
        }
    }
    return timeOfDay
}

/** Colours cross-fade over a few seconds rather than snapping. */
@Composable
private fun Color.drift(): State<Color> =
    animateColorAsState(targetValue = this, animationSpec = tween(3000), label = "palette")

@Composable
private fun ColorScheme.drifting(): ColorScheme {
    val primary by primary.drift()
    val onPrimary by onPrimary.drift()
    val primaryContainer by primaryContainer.drift()
    val onPrimaryContainer by onPrimaryContainer.drift()
    val secondary by secondary.drift()
    val onSecondary by onSecondary.drift()
    val secondaryContainer by secondaryContainer.drift()
    val onSecondaryContainer by onSecondaryContainer.drift()
    val tertiary by tertiary.drift()
    val onTertiary by onTertiary.drift()
    val tertiaryContainer by tertiaryContainer.drift()
    val onTertiaryContainer by onTertiaryContainer.drift()
    val background by background.drift()
    val onBackground by onBackground.drift()
    val surface by surface.drift()
    val onSurface by onSurface.drift()
    val surfaceVariant by surfaceVariant.drift()
    val onSurfaceVariant by onSurfaceVariant.drift()
    val outline by outline.drift()
    val outlineVariant by outlineVariant.drift()

    return copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        outlineVariant = outlineVariant
    )
}

@Composable
fun DailyScheduleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val timeOfDay = rememberTimeOfDay()
    val scheme = colorSchemeFor(timeOfDay, darkTheme).drifting()

    CompositionLocalProvider(LocalTimeOfDay provides timeOfDay) {
        MaterialTheme(
            colorScheme = scheme,
            shapes = ForestShapes,
            typography = ForestTypography,
            content = content
        )
    }
}
