package `in`.aicortex.iso8583studio.web.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.web.theme.Glass
import `in`.aicortex.iso8583studio.web.theme.GlassShapes

/** The `.gp-btn` pill: blue gradient when enabled, the ghost look when not, a spinner while loading. */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    primary: Boolean = enabled,
) {
    val c = Glass.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val active = enabled && !isLoading
    Box(
        modifier = modifier
            .heightIn(min = 36.dp)
            .then(if (pressed && active) Modifier.glow(c.btnGlow, 18.dp, 22.dp, inflate = 2.dp).graphicsLayer { scaleX = 0.97f; scaleY = 0.97f } else Modifier)
            .clip(GlassShapes.pill)
            .then(if (primary) Modifier.background(Brush.verticalGradient(listOf(c.blueHover, c.blue))) else Modifier.background(c.ghostBg).border(1.dp, c.ghostBorder, GlassShapes.pill))
            .drawWithContent {
                drawContent()
                if (primary) drawRect(c.btnInsetHi, size = Size(size.width, 1f))
                if (hovered && active) drawRect(Color.White.copy(alpha = 0.06f))
            }
            .hoverable(interaction)
            .pointerHoverIcon(if (active) PointerIcon.Hand else PointerIcon.Default)
            .clickable(enabled = active, interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else 0.55f)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(14.dp), color = c.onAccent, strokeWidth = 2.dp)
            else icon?.let { Icon(it, contentDescription = null, tint = if (primary) c.onAccent else c.text, modifier = Modifier.size(12.dp)) }
            Text(if (isLoading) "Processing…" else text, style = MaterialTheme.typography.button, color = if (primary) c.onAccent else c.text, maxLines = 1)
        }
    }
}
