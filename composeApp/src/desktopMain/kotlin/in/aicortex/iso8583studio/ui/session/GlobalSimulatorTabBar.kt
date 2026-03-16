package `in`.aicortex.iso8583studio.ui.session

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import `in`.aicortex.iso8583studio.data.model.StudioTool
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.SimulatorType

// ─── Simulator Type Visual Config ────────────────────────────────────────
private data class SimulatorVisualConfig(
    val icon: ImageVector,
    val activeColor: Color,
    val pulseColor: Color,
    val shortLabel: String
)

private fun getSimulatorVisual(type: SimulatorType): SimulatorVisualConfig {
    return when (type) {
        SimulatorType.HOST -> SimulatorVisualConfig(
            icon = Icons.Default.Dns,
            activeColor = Color(0xFF42A5F5),
            pulseColor = Color(0xFF90CAF9),
            shortLabel = "HOST"
        )
        SimulatorType.HSM -> SimulatorVisualConfig(
            icon = Icons.Default.Security,
            activeColor = Color(0xFF8E24AA),
            pulseColor = Color(0xFFCE93D8),
            shortLabel = "HSM"
        )
        SimulatorType.POS -> SimulatorVisualConfig(
            icon = Icons.Default.PointOfSale,
            activeColor = Color(0xFF43A047),
            pulseColor = Color(0xFFA5D6A7),
            shortLabel = "POS"
        )
        SimulatorType.APDU -> SimulatorVisualConfig(
            icon = Icons.Default.CreditCard,
            activeColor = Color(0xFFFF7043),
            pulseColor = Color(0xFFFFAB91),
            shortLabel = "APDU"
        )
        SimulatorType.TOOL -> SimulatorVisualConfig(
            icon = Icons.Default.Build,
            activeColor = Color(0xFF00ACC1),
            pulseColor = Color(0xFF80DEEA),
            shortLabel = "TOOL"
        )
        else -> SimulatorVisualConfig(
            icon = Icons.Default.Computer,
            activeColor = Color(0xFF78909C),
            pulseColor = Color(0xFFB0BEC5),
            shortLabel = type.name.take(4)
        )
    }
}

// ─── Main Global Tab Bar ────────────────────────────────────────────────

/**
 * Global Tab Bar that sits above the main content area.
 * Shows:
 * 1. A "Studio" home tab (always present) — returns to main navigation
 * 2. One tab per running simulator session — click to switch, X to close
 *
 * The bar is only visible when at least one simulator is running,
 * keeping the UI clean during normal tool usage.
 */
@Composable
fun GlobalSimulatorTabBar() {
    val sessions = SimulatorSessionManager.sessions
    val activeSessionId = SimulatorSessionManager.activeSessionId.value

    // Only show the tab bar when there are running sessions
    AnimatedVisibility(
        visible = sessions.isNotEmpty(),
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(
            animationSpec = tween(200)
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colors.surface,
            elevation = 4.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colors.surface,
                                    MaterialTheme.colors.surface.copy(alpha = 0.95f)
                                )
                            )
                        )
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // ── Studio Home Tab ──
                    StudioHomeTab(
                        isActive = activeSessionId == null,
                        onClick = { SimulatorSessionManager.activateMainContent() }
                    )

                    // ── Separator ──
                    if (sessions.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(
                                    MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
                                )
                        )
                        Spacer(Modifier.width(2.dp))
                    }

                    // ── Running Session Tabs ──
                    sessions.forEach { session ->
                        key(session.id) {
                            val isPoppedOut = SimulatorSessionManager.isPoppedOut(session.id)
                            SimulatorSessionTab(
                                session = session,
                                isActive = session.id == activeSessionId,
                                isPoppedOut = isPoppedOut,
                                onClick = { SimulatorSessionManager.activateSession(session.id) },
                                onClose = { SimulatorSessionManager.closeSession(session.id) },
                                onPopOut = { SimulatorSessionManager.popOutSession(session.id) },
                                onDock = { SimulatorSessionManager.dockSession(session.id) }
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // ── Session Count Badge ──
                    if (sessions.size > 1) {
                        SessionCountBadge(count = sessions.size)
                    }

                    // ── Global Tool Search ──
                    Spacer(Modifier.width(8.dp))
                    TabBarSearch(
                        onToolSelected = { tool ->
                            SimulatorSessionManager.openTool(tool)
                        }
                    )
                }

                // Subtle bottom accent line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            if (activeSessionId != null) {
                                val activeSession = sessions.find { it.id == activeSessionId }
                                val visual = activeSession?.let { getSimulatorVisual(it.simulatorType) }
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        visual?.activeColor ?: MaterialTheme.colors.primary,
                                        Color.Transparent
                                    )
                                )
                            } else {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colors.primary,
                                        Color.Transparent
                                    )
                                )
                            }
                        )
                )
            }
        }
    }
}

// ─── Studio Home Tab ────────────────────────────────────────────────────

@Composable
private fun StudioHomeTab(
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive)
            MaterialTheme.colors.primary.copy(alpha = 0.12f)
        else
            Color.Transparent,
        animationSpec = tween(200)
    )

    Surface(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Dashboard,
                contentDescription = "Studio",
                modifier = Modifier.size(16.dp),
                tint = if (isActive) MaterialTheme.colors.primary
                else MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = "Studio",
                style = MaterialTheme.typography.caption.copy(
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp
                ),
                color = if (isActive) MaterialTheme.colors.primary
                else MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

// ─── Simulator Session Tab ──────────────────────────────────────────────

@Composable
private fun SimulatorSessionTab(
    session: SimulatorSession,
    isActive: Boolean,
    isPoppedOut: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    onPopOut: () -> Unit,
    onDock: () -> Unit
) {
    val visual = getSimulatorVisual(session.simulatorType)

    val bgColor by animateColorAsState(
        targetValue = when {
            isPoppedOut -> visual.activeColor.copy(alpha = 0.06f)
            isActive -> visual.activeColor.copy(alpha = 0.12f)
            else -> Color.Transparent
        },
        animationSpec = tween(200)
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isPoppedOut -> visual.activeColor.copy(alpha = 0.15f)
            isActive -> visual.activeColor.copy(alpha = 0.3f)
            else -> Color.Transparent
        },
        animationSpec = tween(200)
    )

    Surface(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .then(
                if (isActive || isPoppedOut) Modifier.border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(6.dp)
                ) else Modifier
            )
            .clickable(onClick = if (isPoppedOut) onDock else onClick),
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RunningIndicatorDot(color = visual.activeColor, isActive = isActive && !isPoppedOut)

            Icon(
                imageVector = if (isPoppedOut) Icons.Default.OpenInNew else visual.icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (isPoppedOut) visual.activeColor.copy(alpha = 0.5f)
                else if (isActive) visual.activeColor
                else MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )

            Text(
                text = session.config.name,
                style = MaterialTheme.typography.caption.copy(
                    fontWeight = if (isActive && !isPoppedOut) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 11.sp
                ),
                color = if (isPoppedOut) visual.activeColor.copy(alpha = 0.5f)
                else if (isActive) visual.activeColor
                else MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.dp)
            )

            if (isPoppedOut) {
                // Show a "dock back" button for popped-out tabs
                IconButton(
                    onClick = onDock,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureInPicture,
                        contentDescription = "Dock ${session.displayName} back",
                        modifier = Modifier.size(12.dp),
                        tint = visual.activeColor.copy(alpha = 0.5f)
                    )
                }
            } else {
                // Pop-out button
                IconButton(
                    onClick = onPopOut,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Pop out ${session.displayName}",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                    )
                }

                // Close button
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close ${session.displayName}",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

// ─── Running Indicator Dot (animated pulse) ─────────────────────────────

@Composable
private fun RunningIndicatorDot(color: Color, isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = if (isActive) alpha else 0.35f))
    )
}

// ─── Tab Bar Search ─────────────────────────────────────────────────────

/**
 * Compact inline search field that sits at the right end of the tab bar.
 * Typing opens a dropdown of matching StudioTools; clicking one opens it in a new tab.
 */
@Composable
private fun TabBarSearch(onToolSelected: (StudioTool) -> Unit) {
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val allTools = remember { StudioTool.values().toList() }
    val results = remember(query) {
        if (query.isBlank()) emptyList()
        else allTools.filter {
            it.label.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
        }.take(8)
    }

    // Keep dropdown in sync with results
    LaunchedEffect(results) {
        expanded = results.isNotEmpty()
    }

    Box {
        Surface(
            modifier = Modifier
                .width(200.dp)
                .height(28.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.06f),
            elevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                )
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                                query = ""
                                expanded = false
                                true
                            } else false
                        },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.caption.copy(
                        color = MaterialTheme.colors.onSurface,
                        fontSize = 11.sp
                    ),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                "Search tools…",
                                style = MaterialTheme.typography.caption.copy(
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.35f),
                                    fontSize = 11.sp
                                )
                            )
                        }
                        inner()
                    }
                )
                if (query.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        modifier = Modifier
                            .size(12.dp)
                            .clickable { query = ""; expanded = false },
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }

        // Results dropdown
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false; query = "" },
            offset = DpOffset(0.dp, 4.dp),
            properties = PopupProperties(focusable = false)
        ) {
            results.forEach { tool ->
                DropdownMenuItem(
                    onClick = {
                        onToolSelected(tool)
                        query = ""
                        expanded = false
                    },
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colors.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = tool.label,
                            style = MaterialTheme.typography.caption.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = tool.description,
                            style = MaterialTheme.typography.overline.copy(fontSize = 9.sp),
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ─── Session Count Badge ────────────────────────────────────────────────

@Composable
private fun SessionCountBadge(count: Int) {
    Surface(
        modifier = Modifier.padding(end = 8.dp),
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Layers,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = "$count running",
                style = MaterialTheme.typography.overline,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}