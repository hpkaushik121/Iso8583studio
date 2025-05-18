package `in`.aicortex.iso8583studio.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.material3.Shapes
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Define the color palette for light theme
private val LightColors = lightColors(
    primary = Color(0xFF2E7D32),        // Green 800
    primaryVariant = Color(0xFF1B5E20), // Green 900
    secondary = Color(0xFF1976D2),      // Blue 700
    secondaryVariant = Color(0xFF0D47A1), // Blue 900
    background = Color(0xFFF5F5F5),     // Grey 100
    surface = Color.White,
    error = Color(0xFFB71C1C),          // Red 900
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF212121),   // Grey 900
    onSurface = Color(0xFF212121),      // Grey 900
    onError = Color.White
)

// Define the color palette for dark theme
private val DarkColors = darkColors(
    primary = Color(0xFF66BB6A),        // Green 400
    primaryVariant = Color(0xFF4CAF50), // Green 500
    secondary = Color(0xFF42A5F5),      // Blue 400
    secondaryVariant = Color(0xFF2196F3), // Blue 500
    background = Color(0xFF121212),     // Dark background
    surface = Color(0xFF1E1E1E),        // Dark surface
    error = Color(0xFFEF5350),          // Red 400
    onPrimary = Color(0xFF212121),      // Grey 900
    onSecondary = Color(0xFF212121),    // Grey 900
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color(0xFF212121)         // Grey 900
)

/**
 * Application theme composable that wraps content with the appropriate Material theme
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colors = colors,
        content = content
    )
}