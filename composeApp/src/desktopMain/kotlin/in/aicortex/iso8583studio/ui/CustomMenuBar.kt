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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.max
import kotlinx.coroutines.delay

// ──────────────────────────────────────────────────────────────────────────────
//  Custom Compose menu bar with dark-theme support and glossy look.
//  Replaces the native Swing MenuBar so the menu follows the app theme
//  and works identically on Windows, macOS, and Linux.
// ──────────────────────────────────────────────────────────────────────────────

private val LocalOpenMenuIndex = compositionLocalOf { mutableStateOf(-1) }
/** Pointer is over the open top-level dropdown surface (not nested flyouts). */
private val LocalPointerOverMainDropdown = compositionLocalOf { mutableStateOf(false) }
/** Number of nested submenu flyouts currently open (visibility-based, not pointer-based). */
private val LocalNestedFlyoutDepth = compositionLocalOf { mutableStateOf(0) }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GlossyMenuBar(content: @Composable RowScope.() -> Unit) {
    val openIndex = remember { mutableStateOf(-1) }
    val pointerOverBar = remember { mutableStateOf(false) }
    val pointerOverMainDropdown = remember { mutableStateOf(false) }
    val nestedFlyoutDepth = remember { mutableStateOf(0) }
    val isDark = MaterialTheme.colors.isLight.not()

    LaunchedEffect(pointerOverBar.value, pointerOverMainDropdown.value, nestedFlyoutDepth.value) {
        if (!pointerOverBar.value && !pointerOverMainDropdown.value && nestedFlyoutDepth.value == 0) {
            delay(200)
            if (!pointerOverBar.value && !pointerOverMainDropdown.value && nestedFlyoutDepth.value == 0) {
                openIndex.value = -1
            }
        }
    }

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

    CompositionLocalProvider(
        LocalOpenMenuIndex provides openIndex,
        LocalPointerOverMainDropdown provides pointerOverMainDropdown,
        LocalNestedFlyoutDepth provides nestedFlyoutDepth,
    ) {
        Surface(elevation = 1.dp) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(barBg)
                        .onPointerEvent(PointerEventType.Enter) { pointerOverBar.value = true }
                        .onPointerEvent(PointerEventType.Exit) { pointerOverBar.value = false }
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content,
                )
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RowScope.MenuBarMenu(
    text: String,
    index: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    val openIndex = LocalOpenMenuIndex.current
    val pointerOverMainDropdown = LocalPointerOverMainDropdown.current
    val isOpen = openIndex.value == index
    var hovered by remember { mutableStateOf(false) }
    var anchorHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    val hoverAlpha by animateFloatAsState(if (hovered || isOpen) 1f else 0f, tween(120))
    val textColor = if (isOpen) PrimaryBlue else MaterialTheme.colors.onSurface

    Box(
        modifier = Modifier.onGloballyPositioned { anchorHeightPx = it.size.height }
    ) {
        Box(
            modifier = Modifier
                .height(24.dp)
                .onPointerEvent(PointerEventType.Enter) {
                    hovered = true
                    openIndex.value = index
                }
                .onPointerEvent(PointerEventType.Exit) {
                    hovered = false
                }
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

        if (isOpen) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, anchorHeightPx),
                properties = PopupProperties(focusable = false),
            ) {
                DisposableEffect(Unit) {
                    onDispose { pointerOverMainDropdown.value = false }
                }
                Surface(
                    modifier = Modifier
                        .shadow(8.dp, RoundedCornerShape(6.dp))
                        .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.10f), RoundedCornerShape(6.dp))
                        .onPointerEvent(PointerEventType.Enter) { pointerOverMainDropdown.value = true }
                        .onPointerEvent(PointerEventType.Exit) { pointerOverMainDropdown.value = false },
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colors.surface,
                    elevation = 8.dp,
                ) {
                    Column(modifier = Modifier.width(IntrinsicSize.Max).widthIn(min = 180.dp)) {
                        content()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MenuBarItem(
    text: String,
    onClick: () -> Unit,
) {
    val openIndex = LocalOpenMenuIndex.current
    var hovered by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .onPointerEvent(PointerEventType.Enter) { hovered = true }
            .onPointerEvent(PointerEventType.Exit) { hovered = false }
            .background(if (hovered) PrimaryBlue.copy(alpha = 0.10f) else Color.Transparent)
            .clickable {
                onClick()
                openIndex.value = -1
            }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = MaterialTheme.colors.onSurface,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MenuBarSubMenu(
    text: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val nestedFlyoutDepth = LocalNestedFlyoutDepth.current
    var expanded by remember { mutableStateOf(false) }
    var localHoverCount by remember { mutableStateOf(0) }
    var anchorWidthPx by remember { mutableStateOf(0) }

    LaunchedEffect(localHoverCount) {
        if (localHoverCount <= 0) {
            delay(150)
            if (localHoverCount <= 0) {
                expanded = false
            }
        }
    }

    val expandedSnapshot = expanded
    DisposableEffect(expandedSnapshot) {
        if (expandedSnapshot) {
            nestedFlyoutDepth.value++
        }
        onDispose {
            if (expandedSnapshot) {
                nestedFlyoutDepth.value = max(0, nestedFlyoutDepth.value - 1)
            }
        }
    }

    Box(
        modifier = Modifier.onGloballyPositioned { anchorWidthPx = it.size.width }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .onPointerEvent(PointerEventType.Enter) {
                    localHoverCount++
                    expanded = true
                }
                .onPointerEvent(PointerEventType.Exit) {
                    localHoverCount--
                }
                .background(if (expanded || localHoverCount > 0) PrimaryBlue.copy(alpha = 0.10f) else Color.Transparent)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
        }

        if (expanded) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(anchorWidthPx, 0),
                properties = PopupProperties(focusable = false),
            ) {
                Surface(
                    modifier = Modifier
                        .shadow(8.dp, RoundedCornerShape(6.dp))
                        .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.10f), RoundedCornerShape(6.dp))
                        .onPointerEvent(PointerEventType.Enter) {
                            localHoverCount++
                        }
                        .onPointerEvent(PointerEventType.Exit) {
                            localHoverCount--
                        },
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colors.surface,
                    elevation = 8.dp,
                ) {
                    Column(modifier = Modifier.width(IntrinsicSize.Max).widthIn(min = 180.dp)) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun MenuBarSeparator() {
    Divider(
        modifier = Modifier.padding(vertical = 4.dp),
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f),
        thickness = 1.dp,
    )
}
