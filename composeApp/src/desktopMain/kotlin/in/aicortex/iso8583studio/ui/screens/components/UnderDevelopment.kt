package `in`.aicortex.iso8583studio.ui.screens.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import `in`.aicortex.iso8583studio.ui.SuccessGreen

/**
 * Under Development Warning Composable for ISO8583Studio
 * Shows various types of development status warnings
 */

// Warning type enumeration
enum class DevelopmentStatus(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val description: String
) {
    UNDER_DEVELOPMENT(
        title = "Under Development",
        icon = Icons.Default.Build,
        color = Color(0xFFFF9800), // Orange
        description = "This feature is currently being developed and may not work as expected."
    ),
    COMING_SOON(
        title = "Coming Soon",
        icon = Icons.Default.Schedule,
        color = Color(0xFF2196F3), // Blue
        description = "This feature will be available in a future release."
    ),
    EXPERIMENTAL(
        title = "Experimental Feature",
        icon = Icons.Default.Science,
        color = Color(0xFF9C27B0), // Purple
        description = "This is an experimental feature. Use with caution in production."
    ),
    BETA(
        title = "Beta Feature",
        icon = Icons.Default.BugReport,
        color = Color(0xFFFF5722), // Deep Orange
        description = "This feature is in beta. Please report any issues you encounter."
    ),
    MAINTENANCE(
        title = "Under Maintenance",
        icon = Icons.Default.Engineering,
        color = Color(0xFF607D8B), // Blue Grey
        description = "This feature is temporarily unavailable for maintenance."
    ),
    STABLE(
        title = "STABLE",
        icon = Icons.Default.Check,
        color = SuccessGreen,
        description = "This feature is working in its full potential."
    )
}

/**
 * Simple banner-style warning
 */
@Composable
fun UnderDevelopmentBanner(
    status: DevelopmentStatus = DevelopmentStatus.UNDER_DEVELOPMENT,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
    onDismiss: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = 4.dp,
        backgroundColor = status.color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showIcon) {
                Icon(
                    imageVector = status.icon,
                    contentDescription = status.title,
                    tint = status.color,
                    modifier = Modifier
                        .size(24.dp)
                        .alpha(alpha)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = status.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = status.color
                )
                Text(
                    text = status.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }

            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Modal dialog warning
 */
@Composable
fun UnderDevelopmentDialog(
    isVisible: Boolean,
    status: DevelopmentStatus = DevelopmentStatus.UNDER_DEVELOPMENT,
    featureName: String = "Feature",
    onDismiss: () -> Unit,
    onProceed: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            UnderDevelopmentDialogContent(
                status = status,
                featureName = featureName,
                onDismiss = onDismiss,
                onProceed = onProceed,
                onCancel = onCancel
            )
        }
    }
}

@Composable
private fun UnderDevelopmentDialogContent(
    status: DevelopmentStatus,
    featureName: String,
    onDismiss: () -> Unit,
    onProceed: (() -> Unit)?,
    onCancel: (() -> Unit)?
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Card(
        modifier = Modifier
            .scale(scale)
            .width(400.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated icon
            val infiniteTransition = rememberInfiniteTransition()
            val iconScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                )
            )

            Icon(
                imageVector = status.icon,
                contentDescription = status.title,
                tint = status.color,
                modifier = Modifier
                    .size(48.dp)
                    .scale(iconScale)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = status.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = status.color
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = featureName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = status.description,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (onCancel != null) {
                    OutlinedButton(
                        onClick = {
                            onCancel()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                }

                if (onProceed != null) {
                    Button(
                        onClick = {
                            onProceed()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = status.color
                        )
                    ) {
                        Text(
                            "Continue Anyway",
                            color = Color.White
                        )
                    }
                } else {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = status.color
                        )
                    ) {
                        Text(
                            "OK",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Overlay warning that covers the entire content area
 */
@Composable
fun UnderDevelopmentOverlay(
    status: DevelopmentStatus = DevelopmentStatus.UNDER_DEVELOPMENT,
    featureName: String = "Feature",
    modifier: Modifier = Modifier,
    onProceed: (() -> Unit)? = null,
    content: @Composable () -> Unit = {}
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Blurred/disabled content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.3f)
        ) {
            content()
        }

        // Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.1f),
                            Color.Black.copy(alpha = 0.3f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(32.dp)
                    .widthIn(max = 400.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val infiniteTransition = rememberInfiniteTransition()
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        )
                    )

                    Icon(
                        imageVector = status.icon,
                        contentDescription = status.title,
                        tint = status.color,
                        modifier = Modifier
                            .size(64.dp)
                            .then(
                                if (status == DevelopmentStatus.UNDER_DEVELOPMENT) {
                                    Modifier.graphicsLayer { rotationZ = rotation }
                                } else Modifier
                            )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = status.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = status.color,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = featureName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = status.description,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                        lineHeight = 20.sp
                    )

                    if (onProceed != null) {
                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onProceed,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = status.color
                            )
                        ) {
                            Text(
                                "Continue Anyway",
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact inline warning
 */
@Composable
fun UnderDevelopmentChip(
    status: DevelopmentStatus = DevelopmentStatus.UNDER_DEVELOPMENT,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        color = status.color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, status.color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = status.icon,
                contentDescription = status.title,
                tint = status.color,
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = status.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = status.color,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        }
    }
}

/**
 * Usage Examples
 */
@Composable
fun UnderDevelopmentExamples() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Development Status Components",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        // Banner examples
        UnderDevelopmentBanner(
            status = DevelopmentStatus.UNDER_DEVELOPMENT
        )

        UnderDevelopmentBanner(
            status = DevelopmentStatus.COMING_SOON,
            onDismiss = { /* Handle dismiss */ }
        )

        UnderDevelopmentBanner(
            status = DevelopmentStatus.EXPERIMENTAL
        )

        // Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            UnderDevelopmentChip(status = DevelopmentStatus.BETA)
            UnderDevelopmentChip(status = DevelopmentStatus.MAINTENANCE)
        }

        // Example with dialog trigger
        var showDialog by remember { mutableStateOf(false) }

        Button(
            onClick = { showDialog = true }
        ) {
            Text("Show Dialog Example")
        }

        UnderDevelopmentDialog(
            isVisible = showDialog,
            status = DevelopmentStatus.UNDER_DEVELOPMENT,
            featureName = "XML Format Editor",
            onDismiss = { showDialog = false },
            onProceed = {
                // Handle proceed action
                println("User chose to proceed with under-development feature")
            },
            onCancel = {
                // Handle cancel action
                println("User cancelled")
            }
        )
    }
}