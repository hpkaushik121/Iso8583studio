package `in`.aicortex.iso8583studio.ui.screens.hostSimulator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.WarningYellow
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.components.PrimaryButton
import `in`.aicortex.iso8583studio.ui.screens.components.SecondaryButton

/**
 * ISO8583 Transaction Tab
 */
@Composable
fun ISO8583TransactionTab(
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
                        Text(
                            "Transaction Count: $transactionCount",
                            style = MaterialTheme.typography.caption
                        )
                    }

                    PrimaryButton(
                        text = if (isStarted) "Stop" else "Start",
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
                        modifier = Modifier.width(60.dp),
                        label = { Text("Seconds") },
                        singleLine = true
                    )

                    SecondaryButton(
                        text = "Send",
                        onClick = onSendClick,
                        icon = Icons.Default.Send
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Request/Response area
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
                    elevation = 1.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column {
                        Text(
                            "Formatted Request",
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colors.primary.copy(alpha = 0.1f))
                                .padding(8.dp),
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.primary
                        )

                        val scrollState = rememberScrollState()
                        TextField(
                            value = request,
                            readOnly = true,
                            onValueChange = { },
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .background(Color.Transparent),
                        )
                    }
                }

                // Raw request
                Surface(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxWidth(),
                    elevation = 1.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column {
                        Text(
                            "Raw Request",
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colors.primary.copy(alpha = 0.1f))
                                .padding(8.dp),
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.primary
                        )

                        val scrollState = rememberScrollState()
                        TextField(
                            value = rawRequest,
                            readOnly = true,
                            onValueChange = { },
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .background(Color.Transparent),
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
                    elevation = 1.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column {
                        Text(
                            "Formatted Response",
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colors.secondary.copy(alpha = 0.1f))
                                .padding(8.dp),
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.secondary
                        )

                        val scrollState = rememberScrollState()
                        TextField(
                            value = response,
                            readOnly = true,
                            onValueChange = { },
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .background(Color.Transparent),
                        )
                    }
                }

                // Raw response
                Surface(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxWidth(),
                    elevation = 1.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column {
                        Text(
                            "Raw Response",
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colors.secondary.copy(alpha = 0.1f))
                                .padding(8.dp),
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.secondary
                        )

                        val scrollState = rememberScrollState()

                        TextField(
                            value = rawResponse,
                            readOnly = true,
                            onValueChange = { },
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .background(Color.Transparent),
                        )
                    }
                }
            }
        }
    }
}