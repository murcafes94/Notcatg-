package com.murcafes.notcatg

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

data class ThemePalette(val id: String, val name: String, val primary: Color, val darkPrimary: Color, val swatches: List<Color>)

val THEME_PALETTES = listOf(
    ThemePalette("parchment", "Verde y pergamino", Color(0xFF27645D), Color(0xFFA3D3C1), listOf(Color(0xFF27645D), Color(0xFFA3D3C1), Color(0xFFDFC18A), Color(0xFFF4F0E7))),
    ThemePalette("lilac", "Lila y turquesa", Color(0xFF675481), Color(0xFFD2B8E4), listOf(Color(0xFFCCABD8), Color(0xFF84739B), Color(0xFF6EC6CA), Color(0xFF00979D), Color(0xFF07585C))),
    ThemePalette("olive", "Turquesa y oliva", Color(0xFF4F640E), Color(0xFFD1DC83), listOf(Color(0xFF54C0CC), Color(0xFF1F4F58), Color(0xFF7EA00E), Color(0xFFDCDD64), Color(0xFF233502))),
    ThemePalette("mint", "Menta y coral", Color(0xFF326653), Color(0xFF98DFC9), listOf(Color(0xFF86E3CE), Color(0xFFD0E8A5), Color(0xFFFFDD94), Color(0xFFFA887B), Color(0xFFCCABD8))),
    ThemePalette("ocean", "Azul océano", Color(0xFF005A7D), Color(0xFF95D1E2), listOf(Color(0xFF001B48), Color(0xFF04547A), Color(0xFF008AB0), Color(0xFF97CADB), Color(0xFFD6E9EE))),
    ThemePalette("sunset", "Coral y arena", Color(0xFFA6432F), Color(0xFFFFB99D), listOf(Color(0xFFE25B45), Color(0xFFFF8357), Color(0xFFFACC72), Color(0xFF8DD5CB), Color(0xFFA9C865))),
    ThemePalette("rose", "Rosa y melocotón", Color(0xFF944C5A), Color(0xFFF2B9BE), listOf(Color(0xFFF5CDC7), Color(0xFFE39796), Color(0xFFFFCD88), Color(0xFFFFB284), Color(0xFFC6C09C)))
)

private fun ThemePalette.scheme(dark: Boolean): ColorScheme {
    val background = lerp(if (dark) Color(0xFF131B20) else Color(0xFFF5F2EB), primary, 0.04f)
    val surface = lerp(if (dark) Color(0xFF212B30) else Color(0xFFFAF7F1), primary, 0.06f)
    val container = lerp(surface, if (dark) darkPrimary else primary, if (dark) 0.15f else 0.12f)
    return if (dark) darkColorScheme(
        primary = darkPrimary, onPrimary = Color(0xFF182421),
        primaryContainer = lerp(surface, primary, 0.35f), onPrimaryContainer = Color(0xFFF0EEE9),
        secondary = darkPrimary, onSecondary = Color(0xFF182421),
        secondaryContainer = container, onSecondaryContainer = Color(0xFFF0EEE9),
        background = background, onBackground = Color(0xFFE7EAE6),
        surface = surface, onSurface = Color(0xFFE7EAE6),
        surfaceVariant = container, onSurfaceVariant = Color(0xFFC7D0CC),
        surfaceContainerLowest = background, surfaceContainerLow = surface, surfaceContainer = surface,
        surfaceContainerHigh = container, surfaceContainerHighest = container,
        outline = Color(0xFF93A39C), outlineVariant = Color(0xFF52635C), error = Color(0xFFFFB4AB)
    ) else lightColorScheme(
        primary = primary, onPrimary = Color.White,
        primaryContainer = container, onPrimaryContainer = Color(0xFF202D28),
        secondary = primary, onSecondary = Color.White,
        secondaryContainer = container, onSecondaryContainer = Color(0xFF202D28),
        background = background, onBackground = Color(0xFF252C29),
        surface = surface, onSurface = Color(0xFF252C29),
        surfaceVariant = container, onSurfaceVariant = Color(0xFF4B5851),
        surfaceContainerLowest = background, surfaceContainerLow = surface, surfaceContainer = surface,
        surfaceContainerHigh = container, surfaceContainerHighest = container,
        outline = Color(0xFF718277), outlineVariant = Color(0xFFB3BFB7), error = Color(0xFFAC3935)
    )
}

@Composable
fun NotcatgTheme(paletteId: String = "parchment", mode: String = "system", content: @Composable () -> Unit) {
    val palette = THEME_PALETTES.firstOrNull { it.id == paletteId } ?: THEME_PALETTES.first()
    val dark = when (mode) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    MaterialTheme(colorScheme = palette.scheme(dark),
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp)),
        content = content)
}

fun importanceLabel(level: String): String = when (level) {
    "low" -> "Baja"; "medium" -> "Media"; "high" -> "Alta"; else -> "Sin color"
}

@Composable
fun importanceColor(level: String): Color? {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return when (level) {
        "low" -> if (dark) Color(0xFF97CADB) else Color(0xFF005A7D)
        "medium" -> if (dark) Color(0xFFFACC72) else Color(0xFF785800)
        "high" -> if (dark) Color(0xFFFFB4A8) else Color(0xFFA6432F)
        else -> null
    }
}
