package `in`.aicortex.iso8583studio.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ──────────────────────────────────────────────────────────────────────────────
//  Custom Compose menu bar with dark-theme support and glossy look.
//  Replaces the native Swing MenuBar so the menu follows the app theme
//  and works identically on Windows, macOS, and Linux.
// ──────────────────────────────────────────────────────────────────────────────

/** Shared state that tracks which top-level menu is open (-1 = none). */
private val LocalOpenMenuIndex = compositionLocalOf { mutableStateOf(-1) }

/**
 * Glossy, themed menu bar that renders inside the Compose content area.
 * Supports dark/light theming and a subtle gradient highlight.
 */
@Composable
fun GlossyMenuBar(content: @Composable RowScope.() -> Unit) {
    val openIndex = remember { mutableStateOf(-1) }
    val isDark = MaterialTheme.colors.isLight.not()

    val barBg = if (isDark)
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colors.surface.copy(alpha = 0.95f),
                MaterialTheme.colors.surface.copy(alpha = 0.85f),
            )
        )
    else
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colors.surface,
                MaterialTheme.colors.surface.copy(alpha = 0.97f),
            )
        )

    val borderColor = MaterialTheme.colors.onSurface.copy(alpha = if (isDark) 0.08f else 0.10f)

    CompositionLocalProvider(LocalOpenMenuIndex provides openIndex) {
        Surface(elevation = 1.dp) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(barBg)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content,
                )
                // Bottom highlight line
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(borderColor)
                )
            }
        }
    }
}

/**
 * A top-level menu button in the [GlossyMenuBar].
 * Clicking opens its dropdown; hovering switches to it if another menu is already open.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RowScope.MenuBarMenu(
    text: String,
    index: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    val openIndex = LocalOpenMenuIndex.current
    val isOpen = openIndex.value == index
    var hovered by remember { mutableStateOf(false) }

    val hoverAlpha by animateFloatAsState(if (hovered || isOpen) 1f else 0f, tween(120))
    val textColor = if (isOpen) PrimaryBlue else MaterialTheme.colors.onSurface

    Box {
        Box(
            modifier = Modifier
                .height(24.dp)
                .clickable {
                    openIndex.value = if (isOpen) -1 else index
                }
                .onPointerEvent(PointerEventType.Enter) {
                    hovered = true
                    // Open on hover — either switch or open fresh
                    if (openIndex.value != index) {
                        openIndex.value = index
                    }
                }
                .onPointerEvent(PointerEventType.Exit) { hovered = false }
                .drawBehind {
                    if (hoverAlpha > 0f) {
                        drawRoundRect(
                            color = PrimaryBlue.copy(alpha = 0.10f * hoverAlpha),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                        )
                    }
                }
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = if (isOpen) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor,
                maxLines = 1,
            )
        }

        DropdownMenu(
            expanded = isOpen,
            onDismissRequest = { openIndex.value = -1 },
            offset = DpOffset(0.dp, 0.dp),
            modifier = Modifier.background(MaterialTheme.colors.surface)
                .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.10f), RoundedCornerShape(6.dp))
                .widthIn(min = 180.dp),
        ) {
            content()
        }
    }
}

/**
 * A clickable menu item inside a dropdown.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MenuBarItem(
    text: String,
    onClick: () -> Unit,
) {
    val openIndex = LocalOpenMenuIndex.current
    var hovered by remember { mutableStateOf(false) }

    DropdownMenuItem(
        onClick = {
            onClick()
            openIndex.value = -1
        },
        modifier = Modifier
            .height(24.dp)
            .onPointerEvent(PointerEventType.Enter) { hovered = true }
            .onPointerEvent(PointerEventType.Exit) { hovered = false }
            .background(if (hovered) PrimaryBlue.copy(alpha = 0.10f) else Color.Transparent),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = MaterialTheme.colors.onSurface,
            maxLines = 1,
        )
    }
}

/**
 * A submenu trigger that expands a nested dropdown to the right on hover.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MenuBarSubMenu(
    text: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var hovered by remember { mutableStateOf(false) }

    Box {
        DropdownMenuItem(
            onClick = { expanded = !expanded },
            modifier = Modifier
                .height(24.dp)
                .onPointerEvent(PointerEventType.Enter) {
                    hovered = true
                    expanded = true
                }
                .onPointerEvent(PointerEventType.Exit) { hovered = false }
                .background(if (hovered || expanded) PrimaryBlue.copy(alpha = 0.10f) else Color.Transparent),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                color = MaterialTheme.colors.onSurface,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            // Position to the right of the parent item
            offset = DpOffset(180.dp, (-24).dp),
            modifier = Modifier.background(MaterialTheme.colors.surface)
                .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.10f), RoundedCornerShape(6.dp))
                .widthIn(min = 180.dp),
        ) {
            content()
        }
    }
}

/** A thin horizontal separator line inside a dropdown menu. */
@Composable
fun MenuBarSeparator() {
    Divider(
        modifier = Modifier.padding(vertical = 4.dp),
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f),
        thickness = 1.dp,
    )
}
