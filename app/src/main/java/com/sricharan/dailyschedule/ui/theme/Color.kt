package com.sricharan.dailyschedule.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * The app shifts palette through the day so it feels alive without ever
 * demanding anything. Nothing here is alarming — there is deliberately no
 * red/error-looking colour in the scheme, because nothing the user does
 * (or doesn't do) in this app is an error.
 */
enum class TimeOfDay(val greeting: String, val blurb: String) {
    DAWN("Good morning", "The forest is just waking up."),
    DAY("Hello", "A quiet, open stretch of day."),
    DUSK("Good evening", "The light is going golden."),
    NIGHT("Winding down", "The fire's lit. Rest is enough.");

    companion object {
        fun fromHour(hour: Int): TimeOfDay = when (hour) {
            in 5..8 -> DAWN
            in 9..16 -> DAY
            in 17..19 -> DUSK
            else -> NIGHT
        }
    }
}

// --- Dawn: mist over damp moss -------------------------------------------

private val DawnLight = lightColorScheme(
    primary = Color(0xFF5B7355),          // dewy moss
    onPrimary = Color(0xFFF6F2EA),
    primaryContainer = Color(0xFFD9E3D2),
    onPrimaryContainer = Color(0xFF26331F),
    secondary = Color(0xFF9A8467),        // damp bark
    onSecondary = Color(0xFFFAF6F0),
    secondaryContainer = Color(0xFFE7DBC9),
    onSecondaryContainer = Color(0xFF362C1E),
    tertiary = Color(0xFFC08466),         // first light
    onTertiary = Color(0xFFFDF7F2),
    tertiaryContainer = Color(0xFFF0DDD0),
    onTertiaryContainer = Color(0xFF43281A),
    background = Color(0xFFF2EDE6),       // soft mist
    onBackground = Color(0xFF2F2A24),
    surface = Color(0xFFFBF7F1),
    onSurface = Color(0xFF2F2A24),
    surfaceVariant = Color(0xFFE6DED2),
    onSurfaceVariant = Color(0xFF574F45),
    outline = Color(0xFFA79C8D),
    outlineVariant = Color(0xFFD3C9BA)
)

private val DawnDark = darkColorScheme(
    primary = Color(0xFF9DBDA5),
    onPrimary = Color(0xFF1E2A21),
    primaryContainer = Color(0xFF324335),
    onPrimaryContainer = Color(0xFFCFE3D3),
    secondary = Color(0xFFC9B394),
    onSecondary = Color(0xFF2B2318),
    secondaryContainer = Color(0xFF3E3527),
    onSecondaryContainer = Color(0xFFE6D8C2),
    tertiary = Color(0xFFD9A08A),
    onTertiary = Color(0xFF332016),
    tertiaryContainer = Color(0xFF4A3125),
    onTertiaryContainer = Color(0xFFF2D8CA),
    background = Color(0xFF191D1E),
    onBackground = Color(0xFFE6E1D8),
    surface = Color(0xFF212728),
    onSurface = Color(0xFFE6E1D8),
    surfaceVariant = Color(0xFF2E3634),
    onSurfaceVariant = Color(0xFFBFB8AC),
    outline = Color(0xFF7D8580),
    outlineVariant = Color(0xFF414947)
)

// --- Day: sunlit glade, warm parchment -----------------------------------

private val DayLight = lightColorScheme(
    primary = Color(0xFF4A6741),          // moss green
    onPrimary = Color(0xFFF7F3E9),
    primaryContainer = Color(0xFFD5E0CB),
    onPrimaryContainer = Color(0xFF1F2E1A),
    secondary = Color(0xFF8B6F47),        // bark
    onSecondary = Color(0xFFFBF7EF),
    secondaryContainer = Color(0xFFE9DCC4),
    onSecondaryContainer = Color(0xFF322614),
    tertiary = Color(0xFFC07F3E),         // warm sun
    onTertiary = Color(0xFFFDF8F0),
    tertiaryContainer = Color(0xFFF3E1C6),
    onTertiaryContainer = Color(0xFF432B10),
    background = Color(0xFFF6F1E3),       // warm parchment
    onBackground = Color(0xFF2E2A22),
    surface = Color(0xFFFFFBF2),
    onSurface = Color(0xFF2E2A22),
    surfaceVariant = Color(0xFFE8E0CD),
    onSurfaceVariant = Color(0xFF554E40),
    outline = Color(0xFFA69C87),
    outlineVariant = Color(0xFFD5CBB4)
)

private val DayDark = darkColorScheme(
    primary = Color(0xFFA8C79B),
    onPrimary = Color(0xFF1D2A18),
    primaryContainer = Color(0xFF33452C),
    onPrimaryContainer = Color(0xFFD3E6C8),
    secondary = Color(0xFFD2B78E),
    onSecondary = Color(0xFF2C2316),
    secondaryContainer = Color(0xFF413424),
    onSecondaryContainer = Color(0xFFEBDBBF),
    tertiary = Color(0xFFDCA05C),
    onTertiary = Color(0xFF362408),
    tertiaryContainer = Color(0xFF4E3617),
    onTertiaryContainer = Color(0xFFF5DCB9),
    background = Color(0xFF1A1E19),
    onBackground = Color(0xFFE7E3D6),
    surface = Color(0xFF222722),
    onSurface = Color(0xFFE7E3D6),
    surfaceVariant = Color(0xFF303629),
    onSurfaceVariant = Color(0xFFC0BAA6),
    outline = Color(0xFF7F857A),
    outlineVariant = Color(0xFF434941)
)

// --- Dusk: golden hour ----------------------------------------------------

private val DuskLight = lightColorScheme(
    primary = Color(0xFF5A6B47),
    onPrimary = Color(0xFFF8F3E8),
    primaryContainer = Color(0xFFDBE1CB),
    onPrimaryContainer = Color(0xFF252E1A),
    secondary = Color(0xFFA6763F),
    onSecondary = Color(0xFFFDF7EE),
    secondaryContainer = Color(0xFFF0DDC0),
    onSecondaryContainer = Color(0xFF3D2A11),
    tertiary = Color(0xFFCE7B36),         // golden ember
    onTertiary = Color(0xFFFEF8F1),
    tertiaryContainer = Color(0xFFF7DFC3),
    onTertiaryContainer = Color(0xFF48280B),
    background = Color(0xFFF1E6D6),       // amber wash
    onBackground = Color(0xFF322A20),
    surface = Color(0xFFFBF2E4),
    onSurface = Color(0xFF322A20),
    surfaceVariant = Color(0xFFE4D5BF),
    onSurfaceVariant = Color(0xFF5C503E),
    outline = Color(0xFFAD9E85),
    outlineVariant = Color(0xFFD9C9B0)
)

private val DuskDark = darkColorScheme(
    primary = Color(0xFFA2B78E),
    onPrimary = Color(0xFF212A17),
    primaryContainer = Color(0xFF37432B),
    onPrimaryContainer = Color(0xFFD0DFC0),
    secondary = Color(0xFFD9B489),
    onSecondary = Color(0xFF2F2314),
    secondaryContainer = Color(0xFF463522),
    onSecondaryContainer = Color(0xFFF0D9BB),
    tertiary = Color(0xFFE0965A),
    onTertiary = Color(0xFF39220A),
    tertiaryContainer = Color(0xFF523317),
    onTertiaryContainer = Color(0xFFF8D9B8),
    background = Color(0xFF1E1A17),
    onBackground = Color(0xFFEAE0D2),
    surface = Color(0xFF272220),
    onSurface = Color(0xFFEAE0D2),
    surfaceVariant = Color(0xFF383029),
    onSurfaceVariant = Color(0xFFC7B9A6),
    outline = Color(0xFF87796C),
    outlineVariant = Color(0xFF4B423A)
)

// --- Night: campfire ------------------------------------------------------

private val NightPalette = darkColorScheme(
    primary = Color(0xFFA3C69B),          // moonlit leaf
    onPrimary = Color(0xFF1B2A18),
    primaryContainer = Color(0xFF2F432A),
    onPrimaryContainer = Color(0xFFCFE4C7),
    secondary = Color(0xFFD4B896),        // warm tan
    onSecondary = Color(0xFF2A2117),
    secondaryContainer = Color(0xFF3D3223),
    onSecondaryContainer = Color(0xFFEEDCC2),
    tertiary = Color(0xFFE8944F),         // ember glow
    onTertiary = Color(0xFF34200A),
    tertiaryContainer = Color(0xFF4C3115),
    onTertiaryContainer = Color(0xFFF9D7B2),
    background = Color(0xFF171C18),       // deep forest night
    onBackground = Color(0xFFE9E2D4),
    surface = Color(0xFF1F2620),
    onSurface = Color(0xFFE9E2D4),
    surfaceVariant = Color(0xFF2C3529),
    onSurfaceVariant = Color(0xFFC2BBAA),
    outline = Color(0xFF7C8479),
    outlineVariant = Color(0xFF3F4740)
)

/**
 * Night is always dark — a campfire at noon makes no sense, and neither does
 * a blazing white screen at 2am.
 */
fun colorSchemeFor(timeOfDay: TimeOfDay, darkTheme: Boolean): ColorScheme = when (timeOfDay) {
    TimeOfDay.DAWN -> if (darkTheme) DawnDark else DawnLight
    TimeOfDay.DAY -> if (darkTheme) DayDark else DayLight
    TimeOfDay.DUSK -> if (darkTheme) DuskDark else DuskLight
    TimeOfDay.NIGHT -> NightPalette
}

/** Warm accents used by the garden and the breathing circle. */
object ForestAccents {
    val Leaf = Color(0xFF6E9B62)
    val DeepLeaf = Color(0xFF4A6741)
    val Trunk = Color(0xFF7A5C3E)
    val Ember = Color(0xFFE8944F)
    val EmberSoft = Color(0xFFF0B27A)
    val Sprout = Color(0xFF9BC48D)
    val Soil = Color(0xFF6B573F)
}
