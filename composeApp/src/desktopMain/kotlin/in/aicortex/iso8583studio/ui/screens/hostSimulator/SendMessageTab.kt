package `in`.aicortex.iso8583studio.ui.screens.hostSimulator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Input
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import `in`.aicortex.iso8583studio.data.BitAttribute
import `in`.aicortex.iso8583studio.data.Iso8583Data
import `in`.aicortex.iso8583studio.data.getValue
import `in`.aicortex.iso8583studio.domain.service.hostSimulatorService.HostSimulator
import ai.cortex.core.IsoUtil
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.components.PrimaryButton
import `in`.aicortex.iso8583studio.ui.screens.components.SecondaryButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField
import `in`.aicortex.iso8583studio.ui.screens.components.FixedTextField

internal fun normalizeSendTabHex(s: String): String =
    s.replace("\\s".toRegex(), "").lowercase()

/**
 * Send unsolicited ISO8583 messages: wire hex, structured field editor, saved library, activity log.
 * Hex and fields stay in sync: editing fields updates hex; use Paste & Decode to load hex into fields.
 *
 * The five key states ([liveMessageState], [bitAttributesState], [rawMessageStringState],
 * [lastPackedHexNormState], [selectedMessageState]) are hoisted to the parent so they survive
 * tab switches without resetting.
 */
@Composable
internal fun SendMessageTab(
    gw: HostSimulator,
    logText: List<LogEntry>,
    onClearClick: () -> Unit = {},
    liveMessageState: MutableState<Iso8583Data>,
    bitAttributesState: MutableState<Array<BitAttribute>>,
    rawMessageStringState: MutableState<String>,
    lastPackedHexNormState: MutableState<String>,
    selectedMessageState: MutableState<Transaction?>,
) {
    // Delegate to hoisted state so variable names throughout the function body are unchanged
    var liveMessage: Iso8583Data by liveMessageState
    val bitAttributes: MutableState<Array<BitAttribute>> = bitAttributesState
    var rawMessageString: String by rawMessageStringState
    var lastPackedHexNorm: String by lastPackedHexNormState

    var hexParseError by remember { mutableStateOf<String?>(null) }
    var showDecodedLog by remember { mutableStateOf(false) }

    var savedMessages =
        remember { gw.configuration.simulatedTransactionsToDest.toMutableStateList() }
    var selectedMessage: Transaction? by selectedMessageState
    var showSaveDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    var rightSideTab by remember { mutableIntStateOf(0) }
    var isSending by remember { mutableStateOf(false) }
    var headerSyncToken by remember { mutableIntStateOf(0) }
    var showHexPasteDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    fun syncHexFromLiveMessage() {
        val packed = liveMessage.pack()
        val hex = IsoUtil.bcdToString(packed)
        lastPackedHexNorm = normalizeSendTabHex(hex)
        rawMessageString = hex
        hexParseError = null
    }

    fun applyLiveMessage(m: Iso8583Data) {
        liveMessage = m
        bitAttributes.value = m.bitAttributes.clone()
        headerSyncToken++
        syncHexFromLiveMessage()
    }

    LaunchedEffect(rawMessageString) {
        delay(400)
        val norm = normalizeSendTabHex(rawMessageString)
        if (norm.isEmpty()) {
            hexParseError = null
            return@LaunchedEffect
        }
        if (norm == lastPackedHexNorm) {
            return@LaunchedEffect
        }
        if (norm.length % 2 != 0) {
            hexParseError = "Odd number of hex digits"
            return@LaunchedEffect
        }
        try {
            val bytes = IsoUtil.stringToBcd(norm, norm.length / 2)
            val parsed = Iso8583Data(gw.configuration, isFirst = false)
            parsed.unpackByteArray(bytes)
            applyLiveMessage(parsed)
        } catch (e: Exception) {
            hexParseError = e.message ?: "Parse error"
        }
    }

    val cardShape = RoundedCornerShape(12.dp)
    val insetShape = RoundedCornerShape(10.dp)
    val subtleLine = MaterialTheme.colors.onSurface.copy(alpha = 0.08f)

    // Single full-height row: left panel = message composer + fields + wire (one card); right = library/activity.
    // No window scroll — only the field LazyColumn scrolls inside the weighted inset.
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(0.73f)
                    .fillMaxHeight(),
                shape = cardShape,
                color = MaterialTheme.colors.surface,
                elevation = 3.dp,
                border = BorderStroke(1.dp, subtleLine)
            ) {
                val composerScroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(composerScroll),
                    verticalArrangement = Arrangement.Top
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colors.secondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "Message composer",
                                    style = MaterialTheme.typography.subtitle1,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colors.onSurface
                                )
                                Text(
                                    text = "MTI · TPDU · bitmap & fields",
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f)
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Paste & Decode — outlined ghost button
                            OutlinedButton(
                                onClick = { showHexPasteDialog = true },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colors.primary.copy(alpha = 0.55f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.06f),
                                    contentColor = MaterialTheme.colors.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Input,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Paste & Decode",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.2.sp
                                )
                            }

                            // New message — subtle text-style button with border
                            OutlinedButton(
                                onClick = {
                                    selectedMessage = null
                                    applyLiveMessage(
                                        createDefaultIso8583EditorMessage(gw, isFirst = false)
                                    )
                                },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.2f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    backgroundColor = Color.Transparent,
                                    contentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.75f)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Message,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "New message",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.2.sp
                                )
                            }

                            // Save — icon-chip button, only shown when there's content
                            if (rawMessageString.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { showSaveDialog = true },
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colors.secondary.copy(alpha = 0.5f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        backgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.07f),
                                        contentColor = MaterialTheme.colors.secondary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Save,
                                        contentDescription = "Save to library",
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        "Save",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = 0.2.sp
                                    )
                                }
                            }

                            // Send — filled primary CTA
                            Button(
                                onClick = {
                                    val norm = normalizeSendTabHex(rawMessageString)
                                    if (norm.isEmpty()) return@Button
                                    if (norm.length % 2 != 0) {
                                        gw.resultDialogInterface?.onError {
                                            Text("Invalid hex length")
                                        }
                                        return@Button
                                    }
                                    isSending = true
                                    rightSideTab = 1
                                    coroutineScope.launch {
                                        try {
                                            val bytes = IsoUtil.stringToBcd(norm, norm.length / 2)
                                            gw.sendRawToConnection(bytes)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        } finally {
                                            isSending = false
                                        }
                                    }
                                },
                                enabled = rawMessageString.isNotBlank() && !isSending,
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = MaterialTheme.colors.primary,
                                    contentColor = Color.White,
                                    disabledBackgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.38f),
                                    disabledContentColor = Color.White.copy(alpha = 0.6f)
                                ),
                                elevation = ButtonDefaults.elevation(
                                    defaultElevation = 2.dp,
                                    pressedElevation = 6.dp,
                                    disabledElevation = 0.dp
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                if (isSending) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Sending…",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.3.sp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Send,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Send",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.3.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = subtleLine, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = insetShape,
                        color = MaterialTheme.colors.background.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, MaterialTheme.colors.primary.copy(alpha = 0.1f)),
                        elevation = 0.dp
                    ) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            Iso8583Header(
                                gw = gw,
                                message = liveMessage,
                                externalHeaderSync = headerSyncToken,
                                onMessageTypeChanged = {
                                    liveMessage.messageType = it
                                    bitAttributes.value = liveMessage.bitAttributes.clone()
                                    syncHexFromLiveMessage()
                                },
                                onTpduChanged = {
                                    if (it.length <= 10) {
                                        liveMessage.tpduHeader.rawTPDU =
                                            IsoUtil.stringToBcd(it, it.length / 2)
                                    }
                                    bitAttributes.value = liveMessage.bitAttributes.clone()
                                    syncHexFromLiveMessage()
                                },
                                onUseSmartlinkChanged = { }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Iso8583FieldsEditor(
                                message = liveMessage,
                                bitAttributes = bitAttributes,
                                onFieldChanged = { index, newValue ->
                                    liveMessage.packBit(index + 1, newValue)
                                    bitAttributes.value = liveMessage.bitAttributes.clone()
                                    syncHexFromLiveMessage()
                                },
                                onFieldAdded = { bitNumber, value ->
                                    liveMessage.packBit(bitNumber, value)
                                    bitAttributes.value = liveMessage.bitAttributes.clone()
                                    syncHexFromLiveMessage()
                                },
                                onFieldRemoved = { index ->
                                    liveMessage.bitAttributes[index].isSet = false
                                    bitAttributes.value = liveMessage.bitAttributes.clone()
                                    syncHexFromLiveMessage()
                                },
                                nestedLazyList = false,
                                modifier = Modifier.fillMaxWidth(),
                                externalSyncKey = headerSyncToken
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(onClick = { showDecodedLog = !showDecodedLog }) {
                                    Text(
                                        if (showDecodedLog) "Hide decoded log"
                                        else "Show decoded log"
                                    )
                                }
                            }
                            AnimatedVisibility(visible = showDecodedLog) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colors.secondary.copy(alpha = 0.06f),
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colors.secondary.copy(alpha = 0.2f)
                                    )
                                ) {
                                    val scroll = rememberScrollState()
                                    Text(
                                        text = try {
                                            liveMessage.logFormat()
                                        } catch (e: Exception) {
                                            e.message ?: "Log error"
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 140.dp)
                                            .verticalScroll(scroll)
                                            .padding(12.dp),
                                        style = MaterialTheme.typography.caption,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.88f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(MaterialTheme.colors.primary.copy(alpha = 0.85f))
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colors.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "Wire transport",
                                    style = MaterialTheme.typography.subtitle1,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colors.onSurface
                                )
                                Text(
                                    text = "Raw hex · paste a capture or edit; auto-parse after pause",
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f)
                                )
                            }
                        }
                        IconButton(
                            onClick = { showInfoDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Help",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colors.primary
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colors.background.copy(alpha = 0.9f),
                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.1f)),
                        elevation = 0.dp
                    ) {
                        FixedTextField(
                            value = rawMessageString,
                            onValueChange = { rawMessageString = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 80.dp, max = 128.dp)
                                .padding(10.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            ),
                            colors = TextFieldDefaults.textFieldColors(
                                backgroundColor = Color.Transparent,
                                focusedIndicatorColor = MaterialTheme.colors.primary,
                                cursorColor = MaterialTheme.colors.primary
                            )
                        )
                    }

                    if (hexParseError != null) {
                        Text(
                            text = hexParseError!!,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.error,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    Text(
                        text = "Read-only wire preview · use Paste & Decode to load hex into fields",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(0.27f)
                    .fillMaxHeight(),
                shape = cardShape,
                color = MaterialTheme.colors.surface,
                elevation = 3.dp,
                border = BorderStroke(1.dp, subtleLine)
            ) {
                Column(Modifier.fillMaxSize()) {
                    TabRow(
                        selectedTabIndex = rightSideTab,
                        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.08f),
                        contentColor = MaterialTheme.colors.primary
                    ) {
                        Tab(
                            selected = rightSideTab == 0,
                            onClick = { rightSideTab = 0 },
                            text = {
                                Text(
                                    "Saved",
                                    fontWeight = if (rightSideTab == 0) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        )
                        Tab(
                            selected = rightSideTab == 1,
                            onClick = { rightSideTab = 1 },
                            text = {
                                Text(
                                    "Activity",
                                    fontWeight = if (rightSideTab == 1) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when (rightSideTab) {
                            0 -> SavedMessagesPanel(
                                savedMessages = savedMessages,
                                selectedMessage = selectedMessage,
                                onMessageSelected = { item ->
                                    selectedMessage = item
                                    val message = Iso8583Data(
                                        template = item.fields!!.toTypedArray(),
                                        config = gw.configuration,
                                        isFirst = false,
                                    )
                                    message.messageType = item.mti
                                    applyLiveMessage(message)
                                },
                                onEditSavedMessage = { item ->
                                    selectedMessage = item
                                    val message = Iso8583Data(
                                        template = item.fields!!.toTypedArray(),
                                        config = gw.configuration,
                                        isFirst = false,
                                    )
                                    message.messageType = item.mti
                                    applyLiveMessage(message)
                                },
                                onDeleteMessage = { message ->
                                    val messageToDelete =
                                        savedMessages.firstOrNull { it.id == message.id }
                                    if (selectedMessage?.id == message.id) {
                                        selectedMessage = null
                                    }
                                    savedMessages.remove(messageToDelete)
                                    gw.configuration.simulatedTransactionsToDest = savedMessages
                                },
                                onImportMessages = { showImportDialog = true },
                                onExportMessages = { showExportDialog = true },
                                modifier = Modifier.fillMaxSize()
                            )

                            1 -> Panel(modifier = Modifier.fillMaxSize()) {
                                LogPanelWithAutoScroll(
                                    onClearClick = onClearClick,
                                    logEntries = logText,
                                    onBack = { rightSideTab = 0 }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showSaveDialog) {
            SaveMessageDialog(
                bitAttribute = liveMessage.bitAttributes,
                mti = liveMessage.messageType,
                processingCode = liveMessage.bitAttributes.getOrNull(2)?.getValue() ?: "",
                existing = selectedMessage,
                onDismiss = { showSaveDialog = false },
                onSave = { savedMessage ->
                    val replaceIdx =
                        savedMessages.indexOfFirst { it.id == savedMessage.id }.takeIf { it >= 0 }
                    if (replaceIdx != null) {
                        savedMessages[replaceIdx] = savedMessage
                    } else {
                        savedMessages.add(savedMessage)
                    }
                    showSaveDialog = false
                    gw.configuration.simulatedTransactionsToDest = savedMessages
                    selectedMessage = savedMessage
                },
            )
        }

        if (showImportDialog) {
            ImportMessagesDialog(
                onDismiss = { showImportDialog = false },
                onImport = { importedMessages ->
                    savedMessages.addAll(importedMessages)
                    showImportDialog = false
                    gw.configuration.simulatedTransactionsToDest = savedMessages
                }
            )
        }

        if (showExportDialog) {
            ExportMessagesDialog(
                messages = savedMessages,
                onDismiss = { showExportDialog = false },
                onExport = {
                    showExportDialog = false
                }
            )
        }

        if (showInfoDialog) {
            InformationDialog(
                onDismiss = { showInfoDialog = false }
            )
        }

        if (showHexPasteDialog) {
            HexPasteDecodeDialog(
                gw = gw,
                onDismiss = { showHexPasteDialog = false },
                onDecoded = { parsed ->
                    applyLiveMessage(parsed)
                    showHexPasteDialog = false
                }
            )
        }
    }
}


/**
 * Saved Messages Panel Component
 */
@Composable
fun SavedMessagesPanel(
    savedMessages: List<Transaction>,
    selectedMessage: Transaction?,
    onMessageSelected: (Transaction) -> Unit,
    onEditSavedMessage: (Transaction) -> Unit,
    onDeleteMessage: (Transaction) -> Unit,
    onImportMessages: () -> Unit,
    onExportMessages: () -> Unit,
    modifier: Modifier = Modifier
) {
    Panel(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.primary.copy(alpha = 0.1f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Saved Messages (${savedMessages.size})",
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.primary,
                    fontSize = 14.sp
                )

                Row {
                    IconButton(
                        onClick = onImportMessages,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Upload,
                            contentDescription = "Import Messages",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colors.primary
                        )
                    }

                    IconButton(
                        onClick = onExportMessages,
                        modifier = Modifier.size(24.dp),
                        enabled = savedMessages.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Export Messages",
                            modifier = Modifier.size(14.dp),
                            tint = if (savedMessages.isNotEmpty()) MaterialTheme.colors.primary
                            else MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            Divider()

            // Messages list
            if (savedMessages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Message,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                        )
                        Text(
                            "No saved messages",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Create and save messages to see them here",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(savedMessages.size) { i ->
                        SavedMessageItem(
                            message = savedMessages[i],
                            isSelected = selectedMessage?.id == savedMessages[i].id,
                            onSelect = { onMessageSelected(savedMessages[i]) },
                            onEdit = { onEditSavedMessage(savedMessages[i]) },
                            onDelete = { onDeleteMessage(savedMessages[i]) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual Saved Message Item
 */
@Composable
private fun SavedMessageItem(
    message: Transaction,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        elevation = if (isSelected) 4.dp else 1.dp,
        backgroundColor = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.1f)
        else MaterialTheme.colors.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect() }
            ) {
                Text(
                    text = message.description,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (message.mti.isNotEmpty()) {
                    Text(
                        text = "MTI: ${message.mti}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit in ISO builder",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colors.primary
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Message",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colors.error
                )
            }
        }
    }
}


/**
 * Save Message Dialog
 */
@Composable
private fun SaveMessageDialog(
    bitAttribute: Array<BitAttribute>,
    mti: String,
    processingCode: String,
    existing: Transaction?,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    var messageName by remember(existing?.id) { mutableStateOf(existing?.description ?: "") }
    var mtiField by remember(existing?.id) { mutableStateOf(mti) }
    var procCode by remember(existing?.id) { mutableStateOf(processingCode) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (existing != null) "Update saved message" else "Save message",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                FixedOutlinedTextField(
                    value = messageName,
                    onValueChange = { messageName = it },
                    label = { Text("Message name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row {
                    FixedOutlinedTextField(
                        value = mtiField,
                        onValueChange = { mtiField = it },
                        label = { Text("MTI") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    FixedOutlinedTextField(
                        value = procCode,
                        onValueChange = { procCode = it },
                        label = { Text("Processing code") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val savedMessage = Transaction(
                                id = existing?.id ?: UUID.randomUUID().toString(),
                                description = messageName,
                                mti = mtiField,
                                proCode = procCode,
                                fields = bitAttribute.toMutableList(),
                                restApiMatching = existing?.restApiMatching ?: RestApiMatching(),
                                responseMapping = existing?.responseMapping ?: ResponseMapping()
                            )
                            onSave(savedMessage)
                        },
                        enabled = messageName.isNotEmpty()
                    ) {
                        Text(if (existing != null) "Update" else "Save")
                    }
                }
            }
        }
    }
}

/**
 * Import Messages Dialog
 */
@Composable
private fun ImportMessagesDialog(
    onDismiss: () -> Unit,
    onImport: (List<Transaction>) -> Unit
) {
    var importedContent by remember { mutableStateOf("") }
    var importedParsedContent by remember { mutableStateOf<List<Transaction>?>(null) }
    val coroutineScope = rememberCoroutineScope()
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Import Messages",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Paste JSON content or select a file to import saved messages.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SecondaryButton(
                        text = "Choose File",
                        onClick = {

                            coroutineScope.launch {
                                val fileChooser = JFileChooser().apply {
                                    fileSelectionMode = JFileChooser.FILES_ONLY
                                    dialogTitle = "Select Configuration File"
                                    currentDirectory = File(System.getProperty("user.home"))

                                    // Add file filters
                                    addChoosableFileFilter(
                                        FileNameExtensionFilter(
                                            "Json files",
                                            "json"
                                        )
                                    )
                                }

                                val result = fileChooser.showOpenDialog(null)
                                val file = if (result == JFileChooser.APPROVE_OPTION) {
                                    fileChooser.selectedFile
                                } else {
                                    null
                                }
                                file?.let {
                                    val content = it.readText()
                                    importedParsedContent = parseImportedMessages(content)
                                    importedContent = content

                                }
                            }


                        },
                        icon = Icons.Default.FileOpen,
                        modifier = Modifier.weight(1f)
                    )

                    SecondaryButton(
                        text = "Clear",
                        onClick = { importedContent = "" },
                        icon = Icons.Default.Clear,
                        modifier = Modifier.weight(1f),
                        enabled = importedContent.isNotEmpty()
                    )
                }

                FixedOutlinedTextField(
                    value = importedContent,
                    onValueChange = { importedContent = it },
                    label = { Text("Json Content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            try {


                                importedParsedContent?.let { onImport(it) }
                            } catch (e: Exception) {
                                // Show error
                                println("Error selecting file: ${e.message}")
                            }
                        },
                        enabled = importedContent.isNotEmpty()
                    ) {
                        Text("Import")
                    }
                }
            }
        }
    }
}

/**
 * Export Messages Dialog
 */
@Composable
private fun ExportMessagesDialog(
    messages: List<Transaction>,
    onDismiss: () -> Unit,
    onExport: () -> Unit
) {
    val exportContent = remember { generateExportContent(messages) }
    val coroutineScope = rememberCoroutineScope()
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Export Messages",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Copy the content below or save it to a file to backup your saved messages.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SecondaryButton(
                        text = "Copy to Clipboard",
                        onClick = {
                            // Copy to clipboard logic
                        },
                        icon = Icons.Default.ContentCopy,
                        modifier = Modifier.weight(1f)
                    )

                    SecondaryButton(
                        text = "Save to File",
                        onClick = {
                            // Save to file logic
                            coroutineScope.launch {
                                try {
                                    val fileChooser = JFileChooser().apply {
                                        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                                        dialogTitle = "Select Export Directory"
                                        currentDirectory = File(System.getProperty("user.home"))
                                    }

                                    val result = fileChooser.showOpenDialog(null)
                                    val directoryPath = if (result == JFileChooser.APPROVE_OPTION) {
                                        fileChooser.selectedFile.absolutePath
                                    } else {
                                        null
                                    }

                                    val content = generateExportContent(messages)
                                    val fileName = "messages.json"
                                    val filePath = File(directoryPath, fileName)

                                    filePath.writeText(content)
                                    onExport()
                                } catch (e: Exception) {
                                    println("Error selecting directory: ${e.message}")
                                }
                            }


                        },
                        icon = Icons.Default.Save,
                        modifier = Modifier.weight(1f)
                    )
                }

                FixedOutlinedTextField(
                    value = exportContent,
                    onValueChange = { },
                    label = { Text("Export Content (${messages.size} messages)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    readOnly = true,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

// Helper functions
private fun parseImportedMessages(content: String): List<Transaction> {
    // Implementation for parsing JSON content to SavedMessage list
    // This would use your preferred JSON library
    return Json.decodeFromString(content)
}

private fun generateExportContent(messages: List<Transaction>): String {
    return Json.encodeToString(messages)
}




/**
 * Information Dialog Component
 */
@Composable
fun InformationDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary
                    )
                    Text(
                        text = "Send Message",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Edit MTI, TPDU, and ISO fields in the left panel. Wire hex is shown below the field list and stays in sync as you edit. To load a captured hex into the fields, use Paste & Decode.",
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Medium
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InformationItem(
                        icon = Icons.Default.UnfoldMore,
                        title = "Wire vs fields",
                        description = "Changing fields updates hex. Use Paste & Decode to paste raw wire hex and fill all fields at once."
                    )

                    InformationItem(
                        icon = Icons.Default.Edit,
                        title = "New message",
                        description = "Starts from the sample MTI 0200 template. Use Save to add or update entries in the Saved library."
                    )

                    InformationItem(
                        icon = Icons.Default.Save,
                        title = "Saved library",
                        description = "Save frequently used messages for quick access"
                    )

                    InformationItem(
                        icon = Icons.Default.ImportExport,
                        title = "Import/Export",
                        description = "Import and export message collections for backup"
                    )

                    InformationItem(
                        icon = Icons.Default.Send,
                        title = "Send",
                        description = "Sends the current wire hex to connected clients. Activity tab shows the log."
                    )
                }

                Text(
                    text = "Use the Saved tab for your library; Activity shows the send log without hiding saved messages.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    fontStyle = FontStyle.Italic
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text("Got it")
                    }
                }
            }
        }
    }
}

/**
 * Information Item Component
 */
@Composable
fun InformationItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colors.primary.copy(alpha = 0.7f)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Text(
                text = description,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Paste & Decode dialog
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Paste & Decode (PAD) dialog: user pastes raw wire hex, sees a live field preview,
 * and clicks "Fill Fields" to push the parsed message into the composer.
 */
@Composable
private fun HexPasteDecodeDialog(
    gw: HostSimulator,
    onDismiss: () -> Unit,
    onDecoded: (Iso8583Data) -> Unit
) {
    var hexInput by remember { mutableStateOf("") }
    var parseError by remember { mutableStateOf<String?>(null) }
    var parsedMessage by remember { mutableStateOf<Iso8583Data?>(null) }
    var previewLines by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    fun tryParse(raw: String) {
        val norm = raw.replace("\\s".toRegex(), "").lowercase()
        if (norm.isEmpty()) {
            parseError = null
            parsedMessage = null
            previewLines = emptyList()
            return
        }
        if (norm.length % 2 != 0) {
            parseError = "Odd number of hex digits (${norm.length})"
            parsedMessage = null
            previewLines = emptyList()
            return
        }
        try {
            val bytes = IsoUtil.stringToBcd(norm, norm.length / 2)
            val msg = Iso8583Data(gw.configuration, isFirst = false)
            msg.unpackByteArray(bytes)
            parsedMessage = msg
            parseError = null
            val lines = mutableListOf<Pair<String, String>>()
            lines += "MTI" to msg.messageType
            if (!gw.configuration.doNotUseHeaderDest) {
                lines += "TPDU" to IsoUtil.bcdToString(msg.tpduHeader.rawTPDU)
            }
            msg.bitAttributes.forEachIndexed { idx, attr ->
                if (attr.isSet) {
                    val de = "DE%03d · %s".format(idx + 1, attr.description.ifEmpty { "Field ${idx + 1}" })
                    val v = msg.getValue(idx) ?: ""
                    lines += de to v
                }
            }
            previewLines = lines
        } catch (e: Exception) {
            parseError = e.message ?: "Parse error"
            parsedMessage = null
            previewLines = emptyList()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colors.surface,
            elevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Title bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colors.primary)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Input,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Paste & Decode",
                                style = MaterialTheme.typography.h6,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Paste raw ISO 8583 wire hex to fill all fields",
                                style = MaterialTheme.typography.caption,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    // Hex input area
                    Text(
                        text = "RAW HEX INPUT",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary,
                        letterSpacing = 0.8.sp
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colors.background.copy(alpha = 0.85f),
                        border = BorderStroke(
                            1.dp,
                            if (parseError != null) MaterialTheme.colors.error.copy(alpha = 0.6f)
                            else if (parsedMessage != null) Color(0xFF4CAF50).copy(alpha = 0.5f)
                            else MaterialTheme.colors.onSurface.copy(alpha = 0.15f)
                        ),
                        elevation = 0.dp
                    ) {
                        FixedTextField(
                            value = hexInput,
                            onValueChange = { raw ->
                                hexInput = raw
                                tryParse(raw)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 90.dp, max = 140.dp)
                                .padding(12.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            ),
                            colors = TextFieldDefaults.textFieldColors(
                                backgroundColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = MaterialTheme.colors.primary
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (parseError != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colors.error.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "Parse error",
                                        style = MaterialTheme.typography.caption,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colors.error,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = parseError!!,
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.error
                                )
                            }
                        } else if (parsedMessage != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF4CAF50).copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "Parsed OK",
                                        style = MaterialTheme.typography.caption,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = "${previewLines.size - (if (!gw.configuration.doNotUseHeaderDest) 2 else 1)} data elements",
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            Text(
                                text = "Spaces are ignored · must be even-length hex",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.45f)
                            )
                        }
                        if (hexInput.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    hexInput = ""
                                    parseError = null
                                    parsedMessage = null
                                    previewLines = emptyList()
                                }
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear", style = MaterialTheme.typography.caption)
                            }
                        }
                    }

                    // Decoded field preview
                    if (previewLines.isNotEmpty()) {
                        Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
                        Text(
                            text = "DECODED FIELDS PREVIEW",
                            style = MaterialTheme.typography.caption,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary,
                            letterSpacing = 0.8.sp
                        )
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colors.background.copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.1f)),
                            elevation = 0.dp
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(vertical = 4.dp)
                            ) {
                                items(previewLines.size) { i ->
                                    val (label, value) = previewLines[i]
                                    val isHeader = label == "MTI" || label == "TPDU"
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (i % 2 == 0) Color.Transparent
                                                else MaterialTheme.colors.onSurface.copy(alpha = 0.025f)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isHeader)
                                                MaterialTheme.colors.secondary.copy(alpha = 0.15f)
                                            else
                                                MaterialTheme.colors.primary.copy(alpha = 0.1f)
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.caption,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (isHeader)
                                                    MaterialTheme.colors.secondary
                                                else
                                                    MaterialTheme.colors.primary,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = value,
                                            style = MaterialTheme.typography.caption,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.85f),
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (i < previewLines.size - 1) {
                                        Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.05f))
                                    }
                                }
                            }
                        }
                    } else if (hexInput.isBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                            Icon(
                                Icons.Default.Input,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
                            )
                                Text(
                                    text = "Paste hex above to see a field preview",
                                    style = MaterialTheme.typography.body2,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                // Action bar
                Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                    }
                    Button(
                        onClick = { parsedMessage?.let { onDecoded(it) } },
                        enabled = parsedMessage != null,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Input,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Fill Fields")
                    }
                }
            }
        }
    }
}