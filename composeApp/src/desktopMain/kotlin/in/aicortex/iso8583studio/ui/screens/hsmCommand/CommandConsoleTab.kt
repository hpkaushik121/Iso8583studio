package `in`.aicortex.iso8583studio.ui.screens.hsmCommand

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.domain.service.hsmCommandService.ConnectionState
import `in`.aicortex.iso8583studio.domain.service.hsmCommandService.HsmCommandClientService
import `in`.aicortex.iso8583studio.domain.service.hsmCommandService.HsmCommandResult
import `in`.aicortex.iso8583studio.ui.PrimaryBlue
import kotlinx.coroutines.launch

@Composable
fun CommandConsoleTab(
    service: HsmCommandClientService,
    vendorCommands: List<VendorCommand>,
) {
    val scope = rememberCoroutineScope()
    val connectionState by service.connectionState.collectAsState()

    var selectedCommand by remember { mutableStateOf<VendorCommand?>(null) }
    var commandInput by remember { mutableStateOf("") }
    var lastResult by remember { mutableStateOf<HsmCommandResult?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var responseViewMode by remember { mutableStateOf(0) } // 0=Formatted, 1=Raw Hex

    Row(modifier = Modifier.fillMaxSize()) {
        // --- Left: Command Palette ---
        Surface(
            modifier = Modifier.width(260.dp).fillMaxHeight(),
            color = MaterialTheme.colors.surface,
            elevation = 2.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = PrimaryBlue.copy(alpha = 0.06f),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Command Palette",
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.onSurface,
                        )
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colors.background,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            "Search commands...",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                                        )
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.caption.copy(
                                            color = MaterialTheme.colors.onSurface,
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                        Icon(Icons.Default.Clear, null, modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f))
                                    }
                                }
                            }
                        }
                    }
                }

                val filtered = vendorCommands.filter {
                    searchQuery.isBlank() ||
                            it.code.contains(searchQuery, ignoreCase = true) ||
                            it.name.contains(searchQuery, ignoreCase = true) ||
                            it.description.contains(searchQuery, ignoreCase = true)
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    item {
                        CommandPaletteItem(
                            code = null,
                            name = "Custom Command",
                            description = "Enter raw hex data",
                            selected = selectedCommand == null,
                            onClick = {
                                selectedCommand = null
                                commandInput = ""
                            }
                        )
                    }
                    items(filtered) { cmd ->
                        CommandPaletteItem(
                            code = cmd.code,
                            name = cmd.name,
                            description = cmd.description,
                            selected = selectedCommand == cmd,
                            onClick = {
                                selectedCommand = cmd
                                commandInput = cmd.defaultData
                            }
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colors.background,
                ) {
                    Text(
                        "${filtered.size} commands",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.overline,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                    )
                }
            }
        }

        // --- Center: Main Area ---
        Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(12.dp)) {
            // Command input bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 2.dp,
                shape = RoundedCornerShape(10.dp),
                backgroundColor = MaterialTheme.colors.surface,
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (selectedCommand != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = PrimaryBlue.copy(alpha = 0.1f),
                            ) {
                                Text(
                                    selectedCommand!!.code,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = PrimaryBlue,
                                )
                            }
                            Text(
                                selectedCommand!!.name,
                                style = MaterialTheme.typography.subtitle2,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                selectedCommand!!.description,
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FixedOutlinedTextField(
                            value = commandInput,
                            onValueChange = { commandInput = it },
                            label = { Text("Hex Payload") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text("Enter hex data...") },
                        )
                        Button(
                            onClick = {
                                if (commandInput.isNotBlank() && connectionState == ConnectionState.CONNECTED) {
                                    isSending = true
                                    scope.launch {
                                        try {
                                            val result = service.sendCommand(commandInput)
                                            lastResult = result
                                        } catch (_: Exception) { }
                                        isSending = false
                                    }
                                }
                            },
                            enabled = connectionState == ConnectionState.CONNECTED && commandInput.isNotBlank() && !isSending,
                            colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBlue),
                            modifier = Modifier.height(56.dp),
                            shape = RoundedCornerShape(8.dp),
                            elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Send, "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("Send", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Status strip
            lastResult?.let { result ->
                StatusStrip(result)
                Spacer(Modifier.height(8.dp))
            }

            // Response view mode tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ResponseModeChip("Formatted", selected = responseViewMode == 0, onClick = { responseViewMode = 0 })
                Spacer(Modifier.width(6.dp))
                ResponseModeChip("Raw Hex", selected = responseViewMode == 1, onClick = { responseViewMode = 1 })
                Spacer(Modifier.weight(1f))
                if (lastResult != null) {
                    Text(
                        "${lastResult!!.elapsedMs}ms",
                        style = MaterialTheme.typography.caption,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Request / Response split
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (responseViewMode) {
                    0 -> {
                        ResponsePanel(
                            label = "REQUEST",
                            icon = Icons.Default.CallMade,
                            accentColor = PrimaryBlue,
                            content = lastResult?.formattedRequest ?: "",
                            isMonospace = false,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                        ResponsePanel(
                            label = "RESPONSE",
                            icon = Icons.Default.CallReceived,
                            accentColor = if (lastResult?.success == false) Color(0xFFF44336) else Color(0xFF4CAF50),
                            content = lastResult?.formattedResponse ?: "",
                            isMonospace = false,
                            isError = lastResult?.success == false,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                    1 -> {
                        ResponsePanel(
                            label = "RAW REQUEST",
                            icon = Icons.Default.CallMade,
                            accentColor = PrimaryBlue,
                            content = lastResult?.rawRequest?.let { HsmCommandClientService.bytesToHex(it) } ?: "",
                            isMonospace = true,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                        ResponsePanel(
                            label = "RAW RESPONSE",
                            icon = Icons.Default.CallReceived,
                            accentColor = if (lastResult?.success == false) Color(0xFFF44336) else Color(0xFF4CAF50),
                            content = lastResult?.rawResponse?.let { HsmCommandClientService.bytesToHex(it) } ?: "",
                            isMonospace = true,
                            isError = lastResult?.success == false,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            }
        }

    }
}

@Composable
private fun CommandPaletteItem(
    code: String?,
    name: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = if (selected) PrimaryBlue.copy(alpha = 0.08f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (code != null) {
            Surface(
                shape = RoundedCornerShape(3.dp),
                color = if (selected) PrimaryBlue.copy(alpha = 0.15f) else MaterialTheme.colors.onSurface.copy(alpha = 0.06f),
            ) {
                Text(
                    code,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) PrimaryBlue else MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                )
            }
        } else {
            Icon(
                Icons.Default.Edit,
                null,
                modifier = Modifier.size(14.dp),
                tint = if (selected) PrimaryBlue else MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) PrimaryBlue else MaterialTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                description,
                fontSize = 10.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (selected) {
            Icon(
                Icons.Default.ChevronRight,
                null,
                modifier = Modifier.size(14.dp),
                tint = PrimaryBlue.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun StatusStrip(result: HsmCommandResult) {
    val (bgColor, textColor) = if (result.success)
        Color(0xFF4CAF50).copy(alpha = 0.08f) to Color(0xFF4CAF50)
    else
        Color(0xFFF44336).copy(alpha = 0.08f) to Color(0xFFF44336)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    if (result.success) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    null,
                    modifier = Modifier.size(14.dp),
                    tint = textColor,
                )
                Text(
                    if (result.success) "SUCCESS" else "FAILED",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = textColor,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Divider(modifier = Modifier.height(14.dp).width(1.dp), color = textColor.copy(alpha = 0.2f))
            InfoChip(Icons.Default.Timer, "${result.elapsedMs}ms")
            InfoChip(Icons.Default.Upload, "${result.rawRequest.size}B sent")
            InfoChip(Icons.Default.Download, "${result.rawResponse.size}B recv")
            result.errorMessage?.let {
                Divider(modifier = Modifier.height(14.dp).width(1.dp), color = textColor.copy(alpha = 0.2f))
                Text(it, fontSize = 11.sp, color = Color(0xFFF44336), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(icon, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f))
        Text(text, fontSize = 11.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun ResponseModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = if (selected) PrimaryBlue.copy(alpha = 0.12f) else Color.Transparent,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) PrimaryBlue else MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun ResponsePanel(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    content: String,
    isMonospace: Boolean,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    Card(
        modifier = modifier,
        elevation = 1.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = MaterialTheme.colors.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = accentColor.copy(alpha = 0.06f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(icon, null, modifier = Modifier.size(14.dp), tint = accentColor)
                    Text(
                        label,
                        style = MaterialTheme.typography.overline,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        letterSpacing = 1.sp,
                    )
                }
            }

            SelectionContainer {
                val scrollState = rememberScrollState()
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Text(
                        text = content.ifBlank { "(awaiting data...)" },
                        modifier = Modifier.padding(10.dp).verticalScroll(scrollState).fillMaxSize(),
                        fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = if (content.isBlank()) MaterialTheme.colors.onSurface.copy(alpha = 0.25f)
                        else if (isError) Color(0xFFF44336).copy(alpha = 0.85f)
                        else MaterialTheme.colors.onSurface.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

