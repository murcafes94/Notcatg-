package com.murcafes.notcatg

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightPalette = lightColorScheme(
    primary = Color(0xFF27645D), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E9DE), onPrimaryContainer = Color(0xFF123C35),
    secondary = Color(0xFF796039), secondaryContainer = Color(0xFFF0E1C5),
    background = Color(0xFFF4F0E7), onBackground = Color(0xFF252C29),
    surface = Color(0xFFFAF6EE), onSurface = Color(0xFF252C29),
    surfaceVariant = Color(0xFFE5E9DE), onSurfaceVariant = Color(0xFF4D5852),
    outline = Color(0xFF77847A), error = Color(0xFFAC3935)
)
private val DarkPalette = darkColorScheme(
    primary = Color(0xFFA3D3C1), onPrimary = Color(0xFF0B372D),
    primaryContainer = Color(0xFF244E42), onPrimaryContainer = Color(0xFFD5EDE1),
    secondary = Color(0xFFDFC18A), secondaryContainer = Color(0xFF52442B),
    background = Color(0xFF141D1B), onBackground = Color(0xFFE5EAE2),
    surface = Color(0xFF1E2925), onSurface = Color(0xFFE5EAE2),
    surfaceVariant = Color(0xFF303D35), onSurfaceVariant = Color(0xFFC0CCC1),
    outline = Color(0xFF89968B), error = Color(0xFFFFB4AB)
)

@Composable
fun NotcatgTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkPalette else LightPalette,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp)),
        content = content
    )
}
