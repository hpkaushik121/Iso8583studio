package `in`.aicortex.iso8583studio.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import `in`.aicortex.iso8583studio.ui.screens.components.LocalToolLogPanel
import `in`.aicortex.iso8583studio.ui.screens.components.ToolLogPanel
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.LogPanelWithAutoScroll as SimulatorLogPanel

/** The calculators' log panel on desktop is the host simulator's full one, as it always was. */
private val DesktopToolLogPanel = ToolLogPanel { label, onClear, entries ->
    SimulatorLogPanel(label = label, onClearClick = onClear, logEntries = entries)
}

/**
 * Detect system dark mode. Uses Compose's isSystemInDarkTheme() first,
 * then falls back to Windows registry check on Windows.
 */
@Composable
private fun detectDarkTheme(): Boolean {
    val composeDark = isSystemInDarkTheme()
    if (composeDark) return true
    // Fallback: check Windows registry (cached)
    return windowsDarkModeCache
}

/** Cached Windows dark mode detection — checked once at startup */
private val windowsDarkModeCache: Boolean by lazy {
    detectWindowsDarkMode() ?: false
}

@Composable
fun AppTheme(darkTheme: Boolean = detectDarkTheme(), content: @Composable () -> Unit) {
    val glassActive by isGlassEffectActive

    val colors = when {
        darkTheme && glassActive -> DarkColorPaletteGlass
        darkTheme -> DarkColorPalette
        glassActive -> LightColorPaletteGlass
        else -> LightColorPalette
    }

    CompositionLocalProvider(LocalToolLogPanel provides DesktopToolLogPanel) {
        MaterialTheme(
            colors = colors,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

