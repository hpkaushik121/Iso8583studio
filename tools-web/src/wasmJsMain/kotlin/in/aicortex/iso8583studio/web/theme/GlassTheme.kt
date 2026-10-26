package `in`.aicortex.iso8583studio.web.theme

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.LocalElevationOverlay
import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import `in`.aicortex.iso8583studio.ui.chrome.LocalToolColors
import `in`.aicortex.iso8583studio.ui.chrome.ToolColors

/** The two bundled families once loaded. */
class GlassFonts(val sans: FontFamily, val mono: FontFamily)

private val tokens = GlassColors()

/** Material colours for the web: dark only. `surface` stays opaque because menus and dialogs use it. */
val GlassMaterialColors = darkColors(
    primary = tokens.blue,
    primaryVariant = tokens.blueDeep,
    secondary = tokens.teal,
    secondaryVariant = tokens.tealHi,
    background = tokens.canvas,
    surface = tokens.cardDeep,
    error = tokens.red,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = tokens.text,
    onSurface = tokens.text,
    onError = Color.White,
)

val GlassToolColors = ToolColors(
    success = tokens.greenHi,
    warning = tokens.amberHi,
    onWarning = tokens.amberHi,
    warningSurface = tokens.warnBg,
    info = tokens.blueHi,
)

@OptIn(androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun GlassTheme(fonts: GlassFonts, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalGlassColors provides tokens,
        LocalGlassType provides GlassType(fonts.mono),
        LocalToolColors provides GlassToolColors,
        LocalElevationOverlay provides null,
        LocalMinimumInteractiveComponentEnforcement provides false,
        LocalTextSelectionColors provides TextSelectionColors(handleColor = tokens.blueHi, backgroundColor = tokens.blue.copy(alpha = 0.35f)),
    ) {
        MaterialTheme(colors = GlassMaterialColors, typography = glassTypography(fonts.sans), shapes = GlassShapes.material, content = content)
    }
}
