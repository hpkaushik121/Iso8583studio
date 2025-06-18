package `in`.aicortex.iso8583studio.ui.screens.hostSimulator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.domain.service.hostSimulatorService.GatewayServiceImpl
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.WarningYellow
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.components.PrimaryButton
import `in`.aicortex.iso8583studio.ui.screens.components.SecondaryButton

@Composable
fun ISO8583TransactionTab(
    gw: GatewayServiceImpl,
    isStarted: Boolean,
    onStartStopClick: () -> Unit,
    onClearClick: () -> Unit,
    transactionCount: String,
    isHoldMessage: Boolean,
    onHoldMessageChange: (Boolean) -> Unit,
    holdMessageTime: String,
    onHoldMessageTimeChange: (String) -> Unit,
    waitingRemain: String,
    onSendClick: () -> Unit,
    request: String,
    rawRequest: String,
    response: String,
    rawResponse: String
) {
    // Minimize states for each quadrant (both vertical and horizontal)
    var isFormattedRequestMinimized by remember { mutableStateOf(false) }
    var isFormattedRequestMinimizedHorizontal by remember { mutableStateOf(false) }
    var isRawRequestMinimized by remember { mutableStateOf(false) }
    var isRawRequestMinimizedHorizontal by remember { mutableStateOf(false) }
    var isFormattedResponseMinimized by remember { mutableStateOf(false) }
    var isFormattedResponseMinimizedHorizontal by remember { mutableStateOf(false) }
    var isRawResponseMinimized by remember { mutableStateOf(false) }
    var isRawResponseMinimizedHorizontal by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Compact Status Bar
        if (gw.isStarted()) {
            CompactStatusBar(
                serverAddress = gw.configuration.serverAddress,
                serverPort = gw.configuration.serverPort,
                transactionCount = transactionCount
            )
        }

        // Compact Control Panel
        CompactControlPanel(
            isStarted = isStarted,
            onStartStopClick = onStartStopClick,
            onClearClick = onClearClick,
            isHoldMessage = isHoldMessage,
            onHoldMessageChange = onHoldMessageChange,
            holdMessageTime = holdMessageTime,
            onHoldMessageTimeChange = onHoldMessageTimeChange,
            waitingRemain = waitingRemain,
            onSendClick = onSendClick,
            gatewayStarted = gw.isStarted()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Quadrant Layout with Smart Expanding
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row - Request Quadrants
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(
                        when {
                            isRawRequestMinimized && isRawResponseMinimized -> 8f // Both bottom minimized
                            isRawRequestMinimized || isRawResponseMinimized -> 4f // One bottom minimized
                            else -> 1f // Normal
                        }
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top Left - Formatted Request
                if (!isFormattedRequestMinimizedHorizontal) {
                    if (!isFormattedRequestMinimized) {
                        QuadrantPanel(
                            modifier = Modifier.weight(
                                when {
                                    isFormattedResponseMinimizedHorizontal && (isRawRequestMinimizedHorizontal || isRawResponseMinimizedHorizontal) -> 8f
                                    isFormattedResponseMinimizedHorizontal -> 4f
                                    else -> 1f
                                }
                            ),
                            title = "Formatted Request",
                            icon = Icons.Default.CallMade,
                            content = request,
                            colorScheme = MaterialTheme.colors.primary,
                            onMinimizeVertical = { isFormattedRequestMinimized = true },
                            onMinimizeHorizontal = { isFormattedRequestMinimizedHorizontal = true }
                        )
                    } else {
                        MinimizedVerticalQuadrant(
                            modifier = Modifier.weight(
                                when {
                                    isFormattedResponseMinimizedHorizontal && (isRawRequestMinimizedHorizontal || isRawResponseMinimizedHorizontal) -> 8f
                                    isFormattedResponseMinimizedHorizontal -> 4f
                                    else -> 1f
                                }
                            ),
                            title = "Formatted Request",
                            icon = Icons.Default.CallMade,
                            colorScheme = MaterialTheme.colors.primary,
                            onClick = { isFormattedRequestMinimized = false }
                        )
                    }
                } else {
                    MinimizedHorizontalQuadrant(
                        title = "Formatted Request",
                        icon = Icons.Default.CallMade,
                        colorScheme = MaterialTheme.colors.primary,
                        onClick = { isFormattedRequestMinimizedHorizontal = false }
                    )
                }

                // Top Right - Formatted Response
                if (!isFormattedResponseMinimizedHorizontal) {
                    if (!isFormattedResponseMinimized) {
                        QuadrantPanel(
                            modifier = Modifier.weight(
                                when {
                                    isFormattedRequestMinimizedHorizontal && (isRawRequestMinimizedHorizontal || isRawResponseMinimizedHorizontal) -> 8f
                                    isFormattedRequestMinimizedHorizontal -> 4f
                                    else -> 1f
                                }
                            ),
                            title = "Formatted Response",
                            icon = Icons.Default.CallReceived,
                            content = response,
                            colorScheme = MaterialTheme.colors.secondary,
                            onMinimizeVertical = { isFormattedResponseMinimized = true },
                            onMinimizeHorizontal = { isFormattedResponseMinimizedHorizontal = true }
                        )
                    } else {
                        MinimizedVerticalQuadrant(
                            modifier = Modifier.weight(
                                when {
                                    isFormattedRequestMinimizedHorizontal && (isRawRequestMinimizedHorizontal || isRawResponseMinimizedHorizontal) -> 8f
                                    isFormattedRequestMinimizedHorizontal -> 4f
                                    else -> 1f
                                }
                            ),
                            title = "Formatted Response",
                            icon = Icons.Default.CallReceived,
                            colorScheme = MaterialTheme.colors.secondary,
                            onClick = { isFormattedResponseMinimized = false }
                        )
                    }
                } else {
                    MinimizedHorizontalQuadrant(
                        title = "Formatted Response",
                        icon = Icons.Default.CallReceived,
                        colorScheme = MaterialTheme.colors.secondary,
                        onClick = { isFormattedResponseMinimizedHorizontal = false }
                    )
                }
            }

            // Bottom Row - Raw Quadrants
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(
                        when {
                            isFormattedRequestMinimized && isFormattedResponseMinimized -> 8f // Both top minimized
                            isFormattedRequestMinimized || isFormattedResponseMinimized -> 4f // One top minimized
                            else -> 1f // Normal
                        }
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Bottom Left - Raw Request
                if (!isRawRequestMinimizedHorizontal) {
                    if (!isRawRequestMinimized) {
                        QuadrantPanel(
                            modifier = Modifier.weight(
                                when {
                                    isRawResponseMinimizedHorizontal && (isFormattedRequestMinimizedHorizontal || isFormattedResponseMinimizedHorizontal) -> 8f
                                    isRawResponseMinimizedHorizontal -> 4f
                                    else -> 1f
                                }
                            ),
                            title = "Raw Request (Hex)",
                            icon = Icons.Default.Code,
                            content = rawRequest,
                            colorScheme = MaterialTheme.colors.primary,
                            onMinimizeVertical = { isRawRequestMinimized = true },
                            onMinimizeHorizontal = { isRawRequestMinimizedHorizontal = true }
                        )
                    } else {
                        MinimizedVerticalQuadrant(
                            modifier = Modifier.weight(
                                when {
                                    isRawResponseMinimizedHorizontal && (isFormattedRequestMinimizedHorizontal || isFormattedResponseMinimizedHorizontal) -> 8f
                                    isRawResponseMinimizedHorizontal -> 4f
                                    else -> 1f
                                }
                            ),
                            title = "Raw Request",
                            icon = Icons.Default.Code,
                            colorScheme = MaterialTheme.colors.primary,
                            onClick = { isRawRequestMinimized = false }
                        )
                    }
                } else {
                    MinimizedHorizontalQuadrant(
                        title = "Raw Request",
                        icon = Icons.Default.Code,
                        colorScheme = MaterialTheme.colors.primary,
                        onClick = { isRawRequestMinimizedHorizontal = false }
                    )
                }

                // Bottom Right - Raw Response
                if (!isRawResponseMinimizedHorizontal) {
                    if (!isRawResponseMinimized) {
                        QuadrantPanel(
                            modifier = Modifier.weight(
                                when {
                                    isRawRequestMinimizedHorizontal && (isFormattedRequestMinimizedHorizontal || isFormattedResponseMinimizedHorizontal) -> 8f
                                    isRawRequestMinimizedHorizontal -> 4f
                                    else -> 1f
                                }
                            ),
                            title = "Raw Response (Hex)",
                            icon = Icons.Default.Code,
                            content = rawResponse,
                            colorScheme = MaterialTheme.colors.secondary,
                            onMinimizeVertical = { isRawResponseMinimized = true },
                            onMinimizeHorizontal = { isRawResponseMinimizedHorizontal = true }
                        )
                    } else {
                        MinimizedVerticalQuadrant(
                            modifier = Modifier.weight(
                                when {
                                    isRawRequestMinimizedHorizontal && (isFormattedRequestMinimizedHorizontal || isFormattedResponseMinimizedHorizontal) -> 8f
                                    isRawRequestMinimizedHorizontal -> 4f
                                    else -> 1f
                                }
                            ),
                            title = "Raw Response",
                            icon = Icons.Default.Code,
                            colorScheme = MaterialTheme.colors.secondary,
                            onClick = { isRawResponseMinimized = false }
                        )
                    }
                } else {
                    MinimizedHorizontalQuadrant(
                        title = "Raw Response",
                        icon = Icons.Default.Code,
                        colorScheme = MaterialTheme.colors.secondary,
                        onClick = { isRawResponseMinimizedHorizontal = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactStatusBar(
    serverAddress: String,
    serverPort: Int,
    transactionCount: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SuccessGreen.copy(alpha = 0.08f),
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = SuccessGreen,
                modifier = Modifier.size(5.dp)
            ) {}

            Text(
                "Active",
                style = MaterialTheme.typography.caption.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Medium,
                color = SuccessGreen
            )

            Text(
                "$serverAddress:$serverPort",
                style = MaterialTheme.typography.caption.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Medium,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = MaterialTheme.colors.onSurface
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                "Count: $transactionCount",
                style = MaterialTheme.typography.caption.copy(fontSize = 10.sp),
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun CompactControlPanel(
    isStarted: Boolean,
    onStartStopClick: () -> Unit,
    onClearClick: () -> Unit,
    isHoldMessage: Boolean,
    onHoldMessageChange: (Boolean) -> Unit,
    holdMessageTime: String,
    onHoldMessageTimeChange: (String) -> Unit,
    waitingRemain: String,
    onSendClick: () -> Unit,
    gatewayStarted: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left side - Gateway controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartStopClick,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isStarted) MaterialTheme.colors.error else MaterialTheme.colors.primary
                    ),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isStarted) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (isStarted) "Stop" else "Start",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Medium
                    )
                }

                OutlinedButton(
                    onClick = onClearClick,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Clear",
                        style = MaterialTheme.typography.caption
                    )
                }

                if (!gatewayStarted) {
                    Text(
                        "Gateway Stopped",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Right side - Response controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Switch(
                        checked = isHoldMessage,
                        onCheckedChange = onHoldMessageChange,
                        modifier = Modifier.height(20.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colors.primary
                        )
                    )
                    Text(
                        "Hold",
                        style = MaterialTheme.typography.caption
                    )
                }

                if (waitingRemain != "0") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Timer",
                            tint = WarningYellow,
                            modifier = Modifier.size(14.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = WarningYellow.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "${waitingRemain}s",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = WarningYellow,
                                style = MaterialTheme.typography.caption,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = holdMessageTime,
                    onValueChange = onHoldMessageTimeChange,
                    modifier = Modifier.width(70.dp),
                    textStyle = MaterialTheme.typography.body2,
                    singleLine = true,
                    placeholder = {
                        Text(
                            "5",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                        )
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = MaterialTheme.colors.onSurface,
                        backgroundColor = MaterialTheme.colors.surface
                    )
                )

                Button(
                    onClick = onSendClick,
                    enabled = gatewayStarted,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Send",
                        style = MaterialTheme.typography.caption
                    )
                }
            }
        }
    }
}

@Composable
private fun QuadrantPanel(
    modifier: Modifier = Modifier,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: String,
    colorScheme: Color,
    onMinimizeVertical: () -> Unit,
    onMinimizeHorizontal: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            // Compact Header with both minimize options
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.copy(alpha = 0.1f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colorScheme,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    title,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.caption,
                    color = colorScheme
                )

                Spacer(modifier = Modifier.weight(1f))

                // Character count
                Text(
                    "${content.length}",
                    style = MaterialTheme.typography.caption.copy(fontSize = 10.sp),
                    color = colorScheme.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Vertical minimize button (collapse height)
                IconButton(
                    onClick = onMinimizeVertical,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandLess,
                        contentDescription = "Minimize Height",
                        tint = colorScheme,
                        modifier = Modifier.size(12.dp)
                    )
                }

                // Horizontal minimize button (collapse width)
                IconButton(
                    onClick = onMinimizeHorizontal,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Minimize Width",
                        tint = colorScheme,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Content area
            TextField(
                value = content,
                readOnly = true,
                onValueChange = { },
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize = 13.sp
                )
            )
        }
    }
}

@Composable
private fun MinimizedVerticalQuadrant(
    modifier: Modifier = Modifier,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    colorScheme: Color,
    onClick: () -> Unit
) {
    // Determine if this is a top or bottom panel for proper positioning
    val isTopPanel = title.contains("Formatted")

    Column(
        modifier = modifier.fillMaxHeight()
    ) {
        if (!isTopPanel) {
            // For bottom panels, add spacer first to push content to bottom
            Spacer(modifier = Modifier.weight(1f))
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)  // Even smaller when minimized
                .clickable { onClick() },
            elevation = 2.dp,
            shape = RoundedCornerShape(6.dp),
            color = colorScheme.copy(alpha = 0.1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colorScheme,
                        modifier = Modifier.size(12.dp)
                    )

                    Text(
                        text = title,
                        style = MaterialTheme.typography.caption.copy(fontSize = 11.sp),
                        color = colorScheme,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Expand button
                Icon(
                    imageVector = if (isTopPanel) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                    contentDescription = if (isTopPanel) "Expand Down" else "Expand Up",
                    tint = colorScheme,
                    modifier = Modifier.size(10.dp)
                )
            }
        }

        if (isTopPanel) {
            // For top panels, add spacer after to push content to top
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MinimizedHorizontalQuadrant(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    colorScheme: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(32.dp)  // Fixed small width when minimized horizontally
            .fillMaxHeight()
            .clickable { onClick() },
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp),
        color = colorScheme.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colorScheme,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Vertical text (abbreviated)
            val abbreviation = when {
                title.contains("Formatted Request") -> "F-REQ"
                title.contains("Raw Request") -> "R-REQ"
                title.contains("Formatted Response") -> "F-RSP"
                title.contains("Raw Response") -> "R-RSP"
                else -> title.take(4)
            }

            abbreviation.forEach { char ->
                Text(
                    text = char.toString(),
                    style = MaterialTheme.typography.caption.copy(fontSize = 8.sp),
                    color = colorScheme,
                    modifier = Modifier.padding(vertical = 0.5.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Expand button at bottom
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Expand Width",
                tint = colorScheme,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}