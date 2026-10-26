package `in`.aicortex.iso8583studio.ui.chrome

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import `in`.aicortex.iso8583studio.ui.SuccessGreen

/** Semantic colours the chromes use for validation and status; desktop values are the literals the screens have always used. */
@Immutable
data class ToolColors(
    val success: Color,
    val warning: Color,
    val onWarning: Color,
    val warningSurface: Color,
    val info: Color,
)

val DesktopToolColors = ToolColors(
    success = SuccessGreen,
    warning = Color(0xFFFFC107),
    onWarning = Color(0xFF856404),
    warningSurface = Color(0xFFFFF3CD),
    info = Color(0xFF2196F3),
)

val LocalToolColors = staticCompositionLocalOf { DesktopToolColors }

val MaterialTheme.toolColors: ToolColors
    @Composable @ReadOnlyComposable get() = LocalToolColors.current
