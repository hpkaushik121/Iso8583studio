package `in`.aicortex.iso8583studio.ui.screens.hostSimulator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.domain.service.GatewayServiceImpl
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.WarningYellow
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.components.PrimaryButton
import `in`.aicortex.iso8583studio.ui.screens.components.SecondaryButton

/**
 * ISO8583 Transaction Tab with improved server info placement
 */
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
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // OPTION 1: Status Bar at Top (Recommended)
        if (gw.isStarted()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SuccessGreen.copy(alpha = 0.1f),
                elevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Status indicator
                    Surface(
                        shape = CircleShape,
                        color = SuccessGreen,
                        modifier = Modifier.size(8.dp)
                    ) {}

                    Text(
                        "Gateway Active",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Medium,
                        color = SuccessGreen
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.Default.Router,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )

                    Text(
                        "${gw.configuration.serverAddress}:${gw.configuration.serverPort}",
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.Medium,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SuccessGreen.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Transactions: $transactionCount",
                                style = MaterialTheme.typography.caption,
                                color = SuccessGreen
                            )
                        }
                    }
                }
            }
        }

        // Control panel
        Panel(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Request controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text(
                            "REQUEST",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.subtitle2
                        )
                        // OPTION 2: Show transaction count here instead
                        if (!gw.isStarted()) {
                            Text(
                                "Gateway Stopped",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    PrimaryButton(
                        text = if (isStarted) "Stop Gateway" else "Start Gateway",
                        onClick = onStartStopClick,
                        icon = if (isStarted) Icons.Default.Stop else Icons.Default.PlayArrow
                    )

                    SecondaryButton(
                        text = "Clear",
                        onClick = onClearClick,
                        icon = Icons.Default.Clear
                    )
                }

                // Response controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "RESPONSE",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.subtitle2
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = isHoldMessage,
                            onCheckedChange = onHoldMessageChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colors.primary
                            )
                        )
                        Text("Hold Message")
                    }

                    if (waitingRemain != "0") {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = WarningYellow.copy(alpha = 0.2f)
                        ) {
                            Text(
                                waitingRemain,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = WarningYellow,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    OutlinedTextField(
                        value = holdMessageTime,
                        onValueChange = onHoldMessageTimeChange,
                        modifier = Modifier.width(80.dp),
                        label = { Text("Sec", style = MaterialTheme.typography.caption) },
                        singleLine = true
                    )

                    SecondaryButton(
                        text = "Send",
                        onClick = onSendClick,
                        icon = Icons.Default.Send,
                        enabled = gw.isStarted()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Request/Response area with enhanced headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Request column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Formatted request
                Surface(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxWidth(),
                    elevation = 2.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column {
                        // Enhanced header with icon
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colors.primary.copy(alpha = 0.1f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallMade,
                                contentDescription = null,
                                tint = MaterialTheme.colors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Formatted Request",
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.subtitle2,
                                color = MaterialTheme.colors.primary
                            )
                        }

                        TextField(
                            value = request,
                            readOnly = true,
                            onValueChange = { },
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .background(Color.Transparent),
                            colors = TextFieldDefaults.textFieldColors(
                                backgroundColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    }
                }

                // Raw request
                Surface(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxWidth(),
                    elevation = 2.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colors.primary.copy(alpha = 0.1f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Raw Request (Hex)",
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.subtitle2,
                                color = MaterialTheme.colors.primary
                            )
                        }

                        TextField(
                            value = rawRequest,
                            readOnly = true,
                            onValueChange = { },
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .background(Color.Transparent),
                            colors = TextFieldDefaults.textFieldColors(
                                backgroundColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }

            // Response column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Formatted response
                Surface(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxWidth(),
                    elevation = 2.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colors.secondary.copy(alpha = 0.1f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallReceived,
                                contentDescription = null,
                                tint = MaterialTheme.colors.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Formatted Response",
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.subtitle2,
                                color = MaterialTheme.colors.secondary
                            )
                        }

                        TextField(
                            value = response,
                            readOnly = true,
                            onValueChange = { },
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .background(Color.Transparent),
                            colors = TextFieldDefaults.textFieldColors(
                                backgroundColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    }
                }

                // Raw response
                Surface(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxWidth(),
                    elevation = 2.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colors.secondary.copy(alpha = 0.1f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colors.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Raw Response (Hex)",
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.subtitle2,
                                color = MaterialTheme.colors.secondary
                            )
                        }

                        TextField(
                            value = rawResponse,
                            readOnly = true,
                            onValueChange = { },
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .background(Color.Transparent),
                            colors = TextFieldDefaults.textFieldColors(
                                backgroundColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    }
}

// ALTERNATIVE OPTION: Floating Connection Status (can be used instead of status bar)
@Composable
fun FloatingConnectionStatus(
    gw: GatewayServiceImpl,
    modifier: Modifier = Modifier
) {
    if (gw.isStarted()) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(20.dp),
            elevation = 6.dp,
            color = MaterialTheme.colors.surface
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = SuccessGreen,
                    modifier = Modifier.size(6.dp)
                ) {}

                Text(
                    "${gw.configuration.serverAddress}:${gw.configuration.serverPort}",
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Medium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}