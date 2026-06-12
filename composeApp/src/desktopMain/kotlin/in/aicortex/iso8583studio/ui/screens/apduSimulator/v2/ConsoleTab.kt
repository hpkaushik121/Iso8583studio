package `in`.aicortex.iso8583studio.ui.screens.apduSimulator.v2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.CommandApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.hexToBytes
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.toHex
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.transport.ApduExchange
import `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Live APDU console: free-text C-APDU input + scrolling exchange log. Operates on whatever
 * transport [SimulatorController] currently has connected.
 */
@Composable
fun ConsoleTab(controller: SimulatorController) {
    var input by remember { mutableStateOf("00A4040007A0000000031010") }
    var localError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(controller.exchanges.size) {
        if (controller.exchanges.isNotEmpty()) {
            listState.animateScrollToItem(controller.exchanges.size - 1)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp, shape = RoundedCornerShape(8.dp)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FixedOutlinedTextField(
                    value = input,
                    onValueChange = { input = it.uppercase() },
                    label = { Text("C-APDU (hex)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = controller.conn == ConnState.CONNECTED,
                        onClick = {
                            scope.launch {
                                runCatching {
                                    val cmd = CommandApdu.parse(input.hexToBytes())
                                    controller.transmit(cmd)
                                    localError = null
                                }.onFailure { localError = it.message ?: "send failed" }
                            }
                        },
                    ) {
                        Icon(Icons.Default.Send, null); Spacer(Modifier.width(4.dp)); Text("Send")
                    }
                    OutlinedButton(onClick = {
                        controller.exchanges.clear()
                        localError = null
                    }) {
                        Icon(Icons.Default.Clear, null); Spacer(Modifier.width(4.dp)); Text("Clear log")
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Exchanges: ${controller.exchanges.size}",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    )
                }
                localError?.let {
                    Text(it, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.error)
                }
                if (controller.conn != ConnState.CONNECTED) {
                    Text(
                        "Not connected — go to Setup, pick a mode and transport, then Connect.",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            elevation = 2.dp,
            shape = RoundedCornerShape(8.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Text(
                    "APDU Trace",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(0.dp))
                if (controller.exchanges.isEmpty()) {
                    Text(
                        "No APDUs yet.",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(controller.exchanges) { ExchangeRow(it) }
                    }
                }
            }
        }
    }
}

private val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

@Composable
private fun ExchangeRow(ex: ApduExchange) {
    val cmdHex = ex.command.toBytes().toHex()
    val rspBody = ex.response.data.toHex()
    val sw = "%04X".format(ex.response.sw)
    Card(modifier = Modifier.fillMaxWidth(), elevation = 1.dp, shape = RoundedCornerShape(6.dp)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row {
                Text(
                    timeFmt.format(Date(ex.sentAt)),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${ex.durationMs} ms",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
            }
            Text("→ $cmdHex", style = MaterialTheme.typography.body2, fontFamily = FontFamily.Monospace)
            Text(
                "← ${if (rspBody.isEmpty()) "" else "$rspBody  "}SW=$sw",
                style = MaterialTheme.typography.body2,
                fontFamily = FontFamily.Monospace,
                color = if (ex.response.isSuccess) MaterialTheme.colors.onSurface else MaterialTheme.colors.error,
            )
        }
    }
}
