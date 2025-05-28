package `in`.aicortex.iso8583studio.ui.screens.hostSimulator

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import `in`.aicortex.iso8583studio.domain.service.GatewayServiceImpl
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.components.PrimaryButton
import `in`.aicortex.iso8583studio.ui.screens.components.SecondaryButton
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Enhanced Unsolicited Message Tab with animated log panel transition
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun SendMessageTab(
    gw: GatewayServiceImpl,
    logText: String,
    onClearClick: () -> Unit = {},
) {
    var rawMessageBytes by remember { mutableStateOf(byteArrayOf()) }
    var rawMessageString by remember { mutableStateOf("") }
    var parsedMessageCreated by remember { mutableStateOf("") }
    var showCreateIsoDialog by remember { mutableStateOf(false) }
    var savedMessages =
        remember { gw.configuration.simulatedTransactionsToDest.toMutableStateList() }
    var selectedMessage by remember { mutableStateOf<Transaction?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var currentMessage by remember { mutableStateOf<Iso8583Data?>(null) }

    // Animation state for panel transition
    var showLogPanel by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left panel - Message content (expanded)
            Column(
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Raw message input
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
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Raw Message (Hex)",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colors.primary
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { showCreateIsoDialog = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Create ISO8583 Message",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colors.primary
                                    )
                                }

                                if (rawMessageString.isNotEmpty()) {
                                    IconButton(
                                        onClick = { showSaveDialog = true },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Save,
                                            contentDescription = "Save Message",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colors.primary
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            rawMessageString = ""
                                            parsedMessageCreated = ""
                                            rawMessageBytes = byteArrayOf()
                                            selectedMessage = null
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Clear Message",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colors.primary
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { showInfoDialog = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = "Information",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colors.primary
                                    )
                                }
                            }
                        }

                        TextField(
                            value = rawMessageString,
                            onValueChange = { rawMessageString = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(8.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            ),
                            colors = TextFieldDefaults.textFieldColors(
                                backgroundColor = Color.Transparent,
                                focusedIndicatorColor = MaterialTheme.colors.primary,
                                cursorColor = MaterialTheme.colors.primary
                            )
                        )
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PrimaryButton(
                        text = "Unpack",
                        onClick = {
                            try {
                                rawMessageBytes = IsoUtil.stringToBcd(rawMessageString,
                                    rawMessageString.length / 2)
                                val isoData = Iso8583Data(gw.configuration, isFirst = false)
                                isoData.unpack(
                                    rawMessageBytes
                                )
                                parsedMessageCreated = isoData.logFormat()
                            } catch (e: Exception) {
                                gw.resultDialogInterface?.onError {
                                    Text("Error parsing data: ${e.message}")
                                }
                            }
                        },
                        icon = Icons.Default.UnfoldMore,
                        enabled = rawMessageString.isNotEmpty()
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    PrimaryButton(
                        text = if (isSending) "Sending..." else "Send Message",
                        onClick = {
                            if (rawMessageString.isNotEmpty()) {
                                // Start sending process
                                isSending = true
                                showLogPanel = true

                                coroutineScope.launch {
                                    try {
                                        // Convert string to bytes
                                        rawMessageBytes =
                                            IsoUtil.stringToBcd(rawMessageString,
                                                rawMessageString.length / 2)

                                        // Create ISO8583 data object
                                        val isoData = Iso8583Data(
                                            config = gw.configuration,
                                            isFirst = false
                                        )
                                        isoData.unpack(rawMessageBytes)

                                        // Update parsed message
                                        parsedMessageCreated = isoData.logFormat()

                                        // Send the message
                                        gw.sendToSecondConnection(isoData)


                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        isSending = false
                                    }
                                }
                            }
                        },
                        icon = if (isSending) Icons.Default.Schedule else Icons.Default.Send,
                        enabled = rawMessageString.isNotEmpty() && !isSending
                    )
                }

                // Parsed message output
                Surface(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxWidth(),
                    elevation = 2.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column {
                        Text(
                            "Parsed Message",
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colors.secondary.copy(alpha = 0.1f))
                                .padding(8.dp),
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.secondary
                        )

                        val scrollState = rememberScrollState()
                        TextField(
                            value = parsedMessageCreated,
                            onValueChange = {},
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .background(Color.Transparent)
                                .verticalScroll(scrollState),
                            readOnly = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            ),
                            colors = TextFieldDefaults.textFieldColors(
                                backgroundColor = Color.Transparent,
                                focusedIndicatorColor = MaterialTheme.colors.primary,
                                cursorColor = MaterialTheme.colors.primary
                            )
                        )
                    }
                }
            }

            // Right panel - Animated transition between Saved Messages and Log Panel
            AnimatedContent(
                targetState = showLogPanel,
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxHeight(),
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { if (targetState) it else -it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeIn(
                        animationSpec = tween(300)
                    ) with slideOutHorizontally(
                        targetOffsetX = { if (targetState) -it else it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeOut(
                        animationSpec = tween(300)
                    )
                },
                label = "panel_transition"
            ) { showLog ->
                if (showLog) {
                    // Log Panel
                    Panel {
                        LogPanelWithAutoScroll(
                            onClearClick = onClearClick,
                            logText = logText,
                            onBack = {
                                showLogPanel = false
                                isSending = false
                            }

                        )
                    }


                } else {
                    // Saved Messages Panel
                    SavedMessagesPanel(
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
                            rawMessageBytes = message.pack()
                            rawMessageString = IsoUtil.bcdToString(rawMessageBytes)
                            parsedMessageCreated = message.logFormat()
                            currentMessage = message
                        },
                        onDeleteMessage = { message ->
                            val messageToDelete = savedMessages.firstOrNull { it.id == message.id }
                            if (selectedMessage?.id == message.id) {
                                selectedMessage = null
                            }
                            savedMessages.remove(messageToDelete)
                            gw.configuration.simulatedTransactionsToDest = savedMessages
                        },
                        onImportMessages = { showImportDialog = true },
                        onExportMessages = { showExportDialog = true }
                    )
                }
            }
        }

        // All your existing dialogs remain the same...
        // ISO8583 Editor Dialog
        if (showCreateIsoDialog) {
            Iso8583EditorDialog(
                isFirst = false,
                initialMessage = currentMessage,
                gw = gw,
                onDismiss = { showCreateIsoDialog = false },
                onConfirm = {
                    showCreateIsoDialog = false
                    rawMessageBytes = it.pack()
                    currentMessage = it
                    rawMessageString = IsoUtil.bcdToString(rawMessageBytes)

                    // Auto-parse the created message
                    try {
                        parsedMessageCreated = it.logFormat()
                    } catch (e: Exception) {
                        parsedMessageCreated = "Error parsing message: ${e.message}"
                    }
                }
            )
        }

        // Save Message Dialog
        if (showSaveDialog && currentMessage != null) {
            SaveMessageDialog(
                bitAttribute = currentMessage!!.bitAttributes,
                mti = currentMessage!!.messageType,
                processingCode = currentMessage!!.bitAttributes.getOrNull(2)?.getValue() ?: "",
                onDismiss = { showSaveDialog = false },
                onSave = { savedMessage ->
                    savedMessages.add(savedMessage)
                    showSaveDialog = false
                    gw.configuration.simulatedTransactionsToDest = savedMessages
                },
            )
        }

        // Import Dialog
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

        // Export Dialog
        if (showExportDialog) {
            ExportMessagesDialog(
                messages = savedMessages,
                onDismiss = { showExportDialog = false },
                onExport = {
                    showExportDialog = false
                }
            )
        }

        // Information Dialog
        if (showInfoDialog) {
            InformationDialog(
                onDismiss = { showInfoDialog = false }
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
    onDeleteMessage: (Transaction) -> Unit,
    onImportMessages: () -> Unit,
    onExportMessages: () -> Unit
) {
    Panel(
        modifier = Modifier.fillMaxSize()
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
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onSelect() },
        elevation = if (isSelected) 4.dp else 1.dp,
        backgroundColor = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.1f)
        else MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
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
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Message",
                    modifier = Modifier.size(14.dp),
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
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    var messageName by remember { mutableStateOf("") }
    var mti by remember { mutableStateOf(mti) }
    var procCode by remember { mutableStateOf(processingCode) }


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
                    text = "Save Message",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = messageName,
                    onValueChange = { messageName = it },
                    label = { Text("Message Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row {
                    OutlinedTextField(
                        value = mti,
                        onValueChange = { mti = it },
                        label = { Text("MTI") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    OutlinedTextField(
                        value = procCode,
                        onValueChange = { procCode = it },
                        label = { Text("Processing Code") },
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
                                id = UUID.randomUUID().toString(),
                                description = messageName,
                                mti = mti,
                                proCode = procCode,
                                fields = bitAttribute.toList()

                            )
                            onSave(savedMessage)
                        },
                        enabled = messageName.isNotEmpty()
                    ) {
                        Text("Save")
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

                OutlinedTextField(
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

                OutlinedTextField(
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
                        text = "Unsolicited Messages",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "This tab allows you to construct and send unsolicited ISO8583 messages to clients.",
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Medium
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InformationItem(
                        icon = Icons.Default.Add,
                        title = "Create Messages",
                        description = "Use the '+' icon to build messages with the built-in editor"
                    )

                    InformationItem(
                        icon = Icons.Default.Edit,
                        title = "Manual Entry",
                        description = "Enter raw hexadecimal data directly in the input field"
                    )

                    InformationItem(
                        icon = Icons.Default.Save,
                        title = "Save Messages",
                        description = "Save frequently used messages for quick access"
                    )

                    InformationItem(
                        icon = Icons.Default.ImportExport,
                        title = "Import/Export",
                        description = "Import and export message collections for backup"
                    )

                    InformationItem(
                        icon = Icons.Default.Send,
                        title = "Send Messages",
                        description = "Send unsolicited messages to test client behavior"
                    )
                }

                Text(
                    text = "Saved messages appear in the right panel for quick access and reuse.",
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