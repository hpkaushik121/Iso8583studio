package `in`.aicortex.iso8583studio.ui.screens.components

import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp


/**
 * A custom ScrollbarStyle that aligns with the application's MaterialTheme.
 * This style provides a modern, subtle scrollbar that becomes more prominent on hover.
 *
 * @return A [ScrollbarStyle] instance.
 */
@Composable
fun themedScrollbarStyle(): ScrollbarStyle {
    val colors = MaterialTheme.colors
    return ScrollbarStyle(
        minimalHeight = 16.dp,
        thickness = 8.dp,
        shape = CircleShape,
        hoverDurationMillis = 300,
        unhoverColor = colors.onSurface.copy(alpha = 0.12f),
        hoverColor = colors.primary.copy(alpha = 0.75f)
    )
}
