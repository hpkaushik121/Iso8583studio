package `in`.aicortex.iso8583studio.ui.session

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            activeColor = Color(0xFF42A5F5),  // PrimaryBlue
            pulseColor = Color(0xFF90CAF9),
            shortLabel = "HOST"
        )
        SimulatorType.HSM -> SimulatorVisualConfig(
            icon = Icons.Default.Security,
            activeColor = Color(0xFF8E24AA),  // SecurityPurple
            pulseColor = Color(0xFFCE93D8),
            shortLabel = "HSM"
        )
        SimulatorType.POS -> SimulatorVisualConfig(
            icon = Icons.Default.PointOfSale,
            activeColor = Color(0xFF43A047),  // SuccessGreen
            pulseColor = Color(0xFFA5D6A7),
            shortLabel = "POS"
        )
        SimulatorType.APDU -> SimulatorVisualConfig(
            icon = Icons.Default.CreditCard,
            activeColor = Color(0xFFFF7043),
            pulseColor = Color(0xFFFFAB91),
            shortLabel = "APDU"
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
                            SimulatorSessionTab(
                                session = session,
                                isActive = session.id == activeSessionId,
                                onClick = { SimulatorSessionManager.activateSession(session.id) },
                                onClose = { SimulatorSessionManager.closeSession(session.id) }
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // ── Session Count Badge ──
                    if (sessions.size > 1) {
                        SessionCountBadge(count = sessions.size)
                    }
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
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val visual = getSimulatorVisual(session.simulatorType)

    val bgColor by animateColorAsState(
        targetValue = if (isActive)
            visual.activeColor.copy(alpha = 0.12f)
        else
            Color.Transparent,
        animationSpec = tween(200)
    )

    val borderColor by animateColorAsState(
        targetValue = if (isActive) visual.activeColor.copy(alpha = 0.3f)
        else Color.Transparent,
        animationSpec = tween(200)
    )

    Surface(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .then(
                if (isActive) Modifier.border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(6.dp)
                ) else Modifier
            )
            .clickable(onClick = onClick),
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Running indicator dot
            RunningIndicatorDot(color = visual.activeColor, isActive = isActive)

            // Simulator type icon
            Icon(
                imageVector = visual.icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (isActive) visual.activeColor
                else MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )

            // Session name
            Text(
                text = session.config.name,
                style = MaterialTheme.typography.caption.copy(
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 11.sp
                ),
                color = if (isActive) visual.activeColor
                else MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.dp)
            )

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