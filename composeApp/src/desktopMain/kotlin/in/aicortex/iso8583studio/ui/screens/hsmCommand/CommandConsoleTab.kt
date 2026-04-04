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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.domain.service.hsmCommandService.ConnectionState
import `in`.aicortex.iso8583studio.domain.service.hsmCommandService.HsmCommandClientService
import `in`.aicortex.iso8583studio.domain.service.hsmCommandService.HsmCommandResult
import `in`.aicortex.iso8583studio.ui.PrimaryBlue
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.HeaderFormat
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.HsmVendorType
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.createLogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val Mono = FontFamily.Monospace
private val AccentGreen = Color(0xFF4CAF50)
private val AccentRed = Color(0xFFF44336)
private val AccentOrange = Color(0xFFFF9800)

/**
 * Survives HSM Commander tab switches (Console / Load Test / Logs) and preserves
 * per–Thales-command field maps when changing the selected command.
 */
class CommandConsoleSessionState {
    var searchQuery by mutableStateOf("")
    var responseTab by mutableStateOf(0)
    /** Raw command line when using Custom Command (non-structured). */
    var rawCommandInput by mutableStateOf("")
    var selectedThalesCode: String? by mutableStateOf(null)
    var selectedVendorCommandCode: String? by mutableStateOf(null)
    var lastResult by mutableStateOf<HsmCommandResult?>(null)
    var parsedResponse by mutableStateOf<List<Pair<ThalesCommandField, String>>>(emptyList())
    var bpFormattedRequest by mutableStateOf("")
    var bpFormattedResponse by mutableStateOf("")

    val thalesFieldSnapshots: MutableMap<String, Map<String, String>> = mutableMapOf()
}

@Composable
fun CommandConsoleTab(
    service: HsmCommandClientService,
    vendorCommands: List<VendorCommand>,
    session: CommandConsoleSessionState,
    exchangeLog: SnapshotStateList<LogEntry>,
    logFileSessionName: String,
) {
    val scope = rememberCoroutineScope()
    val connectionState by service.connectionState.collectAsState()
    val isThales = service.config.hsmVendor == HsmVendorType.THALES_PAYSHIELD

    val definitions = remember { thalesCommandDefinitions }
    val categorizedDefs = remember {
        definitions.groupBy { it.category }.toSortedMap(compareBy { it.ordinal })
    }

    var selectedDefinition by remember(session) {
        mutableStateOf(session.selectedThalesCode?.let { c -> definitions.find { it.code == c } })
    }
    var selectedCommand by remember(session, vendorCommands) {
        mutableStateOf(session.selectedVendorCommandCode?.let { c -> vendorCommands.find { it.code == c } })
    }
    var commandInput by remember(session) {
        mutableStateOf(session.rawCommandInput)
    }
    val fieldValues = remember(session) {
        mutableStateMapOf<String, String>().apply {
            session.selectedThalesCode?.let { code ->
                definitions.find { it.code == code }?.let { def ->
                    session.thalesFieldSnapshots[code]?.let { putAll(it) }
                        ?: run {
                            for (field in def.requestFields) {
                                if (field.defaultValue.isNotEmpty()) this[field.id] = field.defaultValue
                            }
                        }
                }
            }
        }
    }
    var isSending by remember { mutableStateOf(false) }

    fun recordExchangeInLogsAndFile() {
        val body = HsmCommandExchangeLog.buildExchangeLogBody(session, service)
        val r = session.lastResult
        val msg = r?.let { res ->
            "Thales exchange (${res.elapsedMs} ms, ${if (res.success) "OK" else "FAIL"})"
        } ?: "Thales exchange"
        exchangeLog.add(createLogEntry(LogType.HSM, msg, body))
        scope.launch(Dispatchers.IO) {
            try {
                HsmCommandExchangeLog.appendToFile(logFileSessionName, body)
            } catch (_: Exception) { }
        }
    }

    SideEffect { session.rawCommandInput = commandInput }

    DisposableEffect(Unit) {
        onDispose {
            selectedDefinition?.let { def ->
                session.thalesFieldSnapshots[def.code] = fieldValues.toMap()
            }
            session.rawCommandInput = commandInput
            session.selectedThalesCode = selectedDefinition?.code
            session.selectedVendorCommandCode = selectedCommand?.code
        }
    }

    fun selectDefinition(def: ThalesCommandDefinition?) {
        selectedDefinition?.let { prev ->
            session.thalesFieldSnapshots[prev.code] = fieldValues.toMap()
        }
        selectedDefinition = def
        session.selectedThalesCode = def?.code
        fieldValues.clear()
        session.parsedResponse = emptyList()
        if (def != null) {
            session.thalesFieldSnapshots[def.code]?.let { fieldValues.putAll(it) }
                ?: run {
                    for (field in def.requestFields) {
                        if (field.defaultValue.isNotEmpty()) {
                            fieldValues[field.id] = field.defaultValue
                        }
                    }
                }
        }
    }

    val onSendStructured: (ThalesCommandDefinition, String) -> Unit = { def, commandText ->
        isSending = true
        scope.launch {
            try {
                val result = service.sendCommand(commandText)
                session.lastResult = result

                session.bpFormattedRequest = try {
                    ThalesWireBuilder.formatRequestBpStyle(
                        def, fieldValues, result.rawRequest,
                        service.config.tcpLengthHeaderEnabled,
                        service.config.headerValue,
                    )
                } catch (_: Exception) { "" }

                val tcpFrameBytes = when (service.config.hsmVendor.headerFormat) {
                    HeaderFormat.TWO_BYTE_LENGTH ->
                        if (service.config.tcpLengthHeaderEnabled) 2 else 0
                    HeaderFormat.FOUR_BYTE_ASCII_LENGTH -> 4
                    else -> 0
                }
                val msgHeaderBytes = service.config.headerValue.length

                session.parsedResponse = try {
                    ThalesWireBuilder.parseResponseFields(
                        def, result.rawResponse, tcpFrameBytes + msgHeaderBytes,
                        requestFieldValues = fieldValues,
                    )
                } catch (_: Exception) { emptyList() }

                session.bpFormattedResponse = try {
                    ThalesWireBuilder.formatResponseBpStyle(
                        def, session.parsedResponse, result.rawResponse,
                        service.config.tcpLengthHeaderEnabled,
                        service.config.headerValue,
                    )
                } catch (_: Exception) { "" }
                recordExchangeInLogsAndFile()
            } catch (e: Exception) {
                exchangeLog.add(
                    createLogEntry(LogType.ERROR, "HSM command failed: ${e.message}", null),
                )
            }
            isSending = false
        }
    }

    val onSendRaw: () -> Unit = {
        isSending = true
        scope.launch {
            try {
                session.lastResult = service.sendCommand(commandInput)
                session.parsedResponse = emptyList()
                session.bpFormattedRequest = ""
                session.bpFormattedResponse = ""
                recordExchangeInLogsAndFile()
            } catch (e: Exception) {
                exchangeLog.add(
                    createLogEntry(LogType.ERROR, "HSM command failed: ${e.message}", null),
                )
            }
            isSending = false
        }
    }

    val dividerColor = MaterialTheme.colors.onSurface.copy(alpha = 0.08f)

    Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {

        // ╔══════════════════════════════════════╗
        // ║  PANEL 1 — COMMAND SIDEBAR           ║
        // ╚══════════════════════════════════════╝
        Surface(
            modifier = Modifier.width(230.dp).fillMaxHeight(),
            color = MaterialTheme.colors.surface,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Sidebar header
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Commands",
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.weight(1f))
                        val count = if (isThales) {
                            categorizedDefs.values.sumOf { defs ->
                                defs.count {
                                    session.searchQuery.isBlank() ||
                                            it.code.contains(session.searchQuery, true) ||
                                            it.name.contains(session.searchQuery, true)
                                }
                            }
                        } else vendorCommands.size
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PrimaryBlue.copy(alpha = 0.1f),
                        ) {
                            Text(
                                "$count",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    SidebarSearch(session.searchQuery) { session.searchQuery = it }
                }

                Divider(color = dividerColor)

                // Sidebar list
                if (isThales) {
                    val scrollState = rememberScrollState()
                    Column(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {
                        PaletteItem(
                            code = null, name = "Custom Command",
                            desc = "Raw command text",
                            selected = selectedDefinition == null && selectedCommand == null,
                            onClick = {
                                selectedDefinition?.let { prev ->
                                    session.thalesFieldSnapshots[prev.code] = fieldValues.toMap()
                                }
                                selectedCommand = null
                                session.selectedVendorCommandCode = null
                                selectDefinition(null)
                                commandInput = session.rawCommandInput
                            },
                        )
                        categorizedDefs.forEach { (category, defs) ->
                            val filtered = defs.filter {
                                session.searchQuery.isBlank() ||
                                        it.code.contains(session.searchQuery, true) ||
                                        it.name.contains(session.searchQuery, true) ||
                                        it.description.contains(session.searchQuery, true)
                            }
                            if (filtered.isNotEmpty()) {
                                SidebarCategory(category.displayName)
                                filtered.forEach { def ->
                                    PaletteItem(
                                        code = def.code, name = def.name,
                                        desc = def.description,
                                        selected = selectedDefinition == def,
                                        onClick = {
                                            selectedCommand = null
                                            session.selectedVendorCommandCode = null
                                            selectDefinition(def)
                                        },
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val filtered = vendorCommands.filter {
                        session.searchQuery.isBlank() ||
                                it.code.contains(session.searchQuery, true) ||
                                it.name.contains(session.searchQuery, true) ||
                                it.description.contains(session.searchQuery, true)
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 2.dp),
                    ) {
                        item {
                            PaletteItem(
                                code = null, name = "Custom Command",
                                desc = "Raw command text",
                                selected = selectedCommand == null,
                                onClick = {
                                    session.rawCommandInput = commandInput
                                    selectedCommand = null
                                    session.selectedVendorCommandCode = null
                                    commandInput = session.rawCommandInput
                                },
                            )
                        }
                        items(filtered) { cmd ->
                            PaletteItem(
                                code = cmd.code, name = cmd.name,
                                desc = cmd.description,
                                selected = selectedCommand == cmd,
                                onClick = {
                                    session.rawCommandInput = commandInput
                                    selectedCommand = cmd
                                    session.selectedVendorCommandCode = cmd.code
                                    commandInput = cmd.defaultData
                                },
                            )
                        }
                    }
                }
            }
        }

        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = dividerColor)

        // ╔══════════════════════════════════════╗
        // ║  PANEL 2 — COMMAND BUILDER           ║
        // ╚══════════════════════════════════════╝
        Column(
            modifier = Modifier.weight(0.42f).fillMaxHeight().background(MaterialTheme.colors.background),
        ) {
            if (isThales && selectedDefinition != null) {
                val def = selectedDefinition!!
                StructuredBuilderPanel(
                    definition = def,
                    fieldValues = fieldValues,
                    connectionState = connectionState,
                    isSending = isSending,
                    onSend = { cmd -> onSendStructured(def, cmd) },
                )
            } else {
                RawBuilderPanel(
                    commandInput = commandInput,
                    onCommandInputChange = { commandInput = it },
                    selectedCommand = selectedCommand,
                    connectionState = connectionState,
                    isSending = isSending,
                    onSend = onSendRaw,
                )
            }
        }

        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = dividerColor)

        // ╔══════════════════════════════════════╗
        // ║  PANEL 3 — RESPONSE AREA             ║
        // ╚══════════════════════════════════════╝
        Column(
            modifier = Modifier.weight(0.58f).fillMaxHeight().background(MaterialTheme.colors.background),
        ) {
            // Status + tabs bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colors.surface,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TabChip("Formatted", selected = session.responseTab == 0) { session.responseTab = 0 }
                    Spacer(Modifier.width(4.dp))
                    TabChip("Parsed", selected = session.responseTab == 2) { session.responseTab = 2 }
                    Spacer(Modifier.width(4.dp))
                    TabChip("Raw Hex", selected = session.responseTab == 1) { session.responseTab = 1 }
                    Spacer(Modifier.weight(1f))

                    session.lastResult?.let { r ->
                        StatusBadge(r)
                    }
                }
            }
            Divider(color = dividerColor)

            // Response content
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (session.responseTab) {
                    0 -> {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                        ) {
                            MonospacePanel(
                                label = "REQUEST",
                                icon = Icons.Default.CallMade,
                                accentColor = PrimaryBlue,
                                content = HsmCommandExchangeLog.formattedRequestPanelText(session, service),
                                isError = false,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                            Divider(
                                modifier = Modifier.fillMaxHeight().width(1.dp),
                                color = dividerColor,
                            )
                            MonospacePanel(
                                label = "RESPONSE",
                                icon = Icons.Default.CallReceived,
                                accentColor = if (session.lastResult?.success == false) AccentRed else AccentGreen,
                                content = HsmCommandExchangeLog.formattedResponsePanelText(session),
                                isError = session.lastResult?.success == false,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                    1 -> {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                        ) {
                            MonospacePanel(
                                label = "RAW REQUEST",
                                icon = Icons.Default.CallMade,
                                accentColor = PrimaryBlue,
                                content = HsmCommandExchangeLog.rawRequestHex(session),
                                isError = false,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                            Divider(
                                modifier = Modifier.fillMaxHeight().width(1.dp),
                                color = dividerColor,
                            )
                            MonospacePanel(
                                label = "RAW RESPONSE",
                                icon = Icons.Default.CallReceived,
                                accentColor = if (session.lastResult?.success == false) AccentRed else AccentGreen,
                                content = HsmCommandExchangeLog.rawResponseHex(session),
                                isError = session.lastResult?.success == false,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                    2 -> {
                        ParsedFieldsView(
                            parsedFields = session.parsedResponse,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
//  STRUCTURED BUILDER PANEL
// ─────────────────────────────────────────────────────────

@Composable
private fun StructuredBuilderPanel(
    definition: ThalesCommandDefinition,
    fieldValues: MutableMap<String, String>,
    connectionState: ConnectionState,
    isSending: Boolean,
    onSend: (String) -> Unit,
) {
    val isM0 = definition.code == "M0"
    val isM2 = definition.code == "M2"
    val modeFlag = fieldValues["modeFlag"].orEmpty()
    val messageBlock = fieldValues["messageBlock"].orEmpty()
    val encryptedData = fieldValues["encryptedData"].orEmpty()
    val inputFormatFlag = fieldValues["inputFormatFlag"].orEmpty()

    LaunchedEffect(messageBlock, inputFormatFlag, definition.code) {
        if (definition.code == "M0") {
            fieldValues["msgLength"] = computeM0MessageLengthField(messageBlock, inputFormatFlag)
        }
    }

    LaunchedEffect(encryptedData, inputFormatFlag, definition.code) {
        if (definition.code == "M2") {
            fieldValues["encryptedMessageLength"] =
                computeM0MessageLengthField(encryptedData, inputFormatFlag)
        }
    }

    LaunchedEffect(modeFlag, definition.code) {
        if ((definition.code == "M0" || definition.code == "M2") && modeFlag in setOf("00", "10")) {
            fieldValues["iv"] = ""
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Pinned header: command info + Send
        Surface(color = MaterialTheme.colors.surface) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = PrimaryBlue.copy(alpha = 0.12f),
                    ) {
                        Text(
                            "${definition.code} / ${definition.responseCode}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontFamily = Mono, fontWeight = FontWeight.Bold,
                            fontSize = 11.sp, color = PrimaryBlue,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            definition.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        if (definition.description.isNotBlank()) {
                            Text(
                                definition.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    val a0CoreReady = if (definition.code != "A0") true else {
                        listOf("mode", "keyType", "keyScheme").all { fieldValues[it]?.isNotBlank() == true }
                    }
                    Button(
                        onClick = {
                            val cmd = try {
                                ThalesWireBuilder.buildPlainTextCommand(definition, fieldValues)
                            } catch (_: Exception) { "" }
                            if (cmd.isNotBlank()) onSend(cmd)
                        },
                        enabled = connectionState == ConnectionState.CONNECTED && !isSending && a0CoreReady,
                        colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBlue),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp),
                        elevation = ButtonDefaults.elevation(0.dp),
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(Modifier.size(14.dp), Color.White, 2.dp)
                        } else {
                            Icon(Icons.Default.Send, null, Modifier.size(14.dp), tint = Color.White)
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Send", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))

        // Scrollable fields area
        if (definition.requestFields.isNotEmpty()) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val visibleFields = definition.requestFields.filter {
                    ThalesWireBuilder.isFieldVisible(it, fieldValues) &&
                        !(isM0 && it.id == "msgLength") &&
                        !(isM2 && it.id == "encryptedMessageLength")
                }
                FieldGrid(visibleFields, fieldValues, definition.forceVerticalFieldLayout)
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        // Pinned wire preview footer
        Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.06f))
        Surface(color = MaterialTheme.colors.surface.copy(alpha = 0.6f)) {
            val plainCmd = remember(fieldValues.toMap(), definition) {
                try {
                    ThalesWireBuilder.buildPlainTextCommand(definition, fieldValues)
                } catch (_: Exception) { "" }
            }
            SelectionContainer {
                Text(
                    text = plainCmd.ifEmpty { "..." },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    fontFamily = Mono, fontSize = 10.sp, maxLines = 2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.35f),
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun FieldGrid(
    visibleFields: List<ThalesCommandField>,
    fieldValues: MutableMap<String, String>,
    forceVerticalFieldLayout: Boolean = false,
) {
    var i = 0
    while (i < visibleFields.size) {
        val field = visibleFields[i]
        val isFlag = field.type == FieldType.FLAG
        val isDropdown = !field.options.isNullOrEmpty()

        if (isFlag) {
            CompactFlagField(
                field, fieldValues[field.id].orEmpty(),
                { fieldValues[field.id] = it },
                Modifier.fillMaxWidth(),
            )
            i++
            continue
        }

        if (!forceVerticalFieldLayout && isDropdown && i + 1 < visibleFields.size) {
            val next = visibleFields[i + 1]
            val nextIsDropdown = !next.options.isNullOrEmpty()
            if (nextIsDropdown) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CompactDropdown(
                        field, fieldValues[field.id].orEmpty(),
                        { fieldValues[field.id] = it },
                        Modifier.weight(1f),
                    )
                    CompactDropdown(
                        next, fieldValues[next.id].orEmpty(),
                        { fieldValues[next.id] = it },
                        Modifier.weight(1f),
                    )
                }
                i += 2
                continue
            }
        }

        if (isDropdown) {
            CompactDropdown(
                field, fieldValues[field.id].orEmpty(),
                { fieldValues[field.id] = it },
                Modifier.fillMaxWidth(),
            )
        } else {
            CompactTextField(
                field, fieldValues[field.id].orEmpty(),
                { fieldValues[field.id] = it },
                Modifier.fillMaxWidth(),
            )
        }
        i++
    }
}

// ─────────────────────────────────────────────────────────
//  RAW BUILDER PANEL
// ─────────────────────────────────────────────────────────

@Composable
private fun RawBuilderPanel(
    commandInput: String,
    onCommandInputChange: (String) -> Unit,
    selectedCommand: VendorCommand?,
    connectionState: ConnectionState,
    isSending: Boolean,
    onSend: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colors.surface) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                if (selectedCommand != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = PrimaryBlue.copy(alpha = 0.1f),
                        ) {
                            Text(
                                selectedCommand.code,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontFamily = Mono, fontWeight = FontWeight.Bold,
                                fontSize = 11.sp, color = PrimaryBlue,
                            )
                        }
                        Text(
                            selectedCommand.name,
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            selectedCommand.description,
                            fontSize = 11.sp, maxLines = 1,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    Text(
                        "Custom Command",
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FixedOutlinedTextField(
                        value = commandInput,
                        onValueChange = onCommandInputChange,
                        label = { Text("Command", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontFamily = Mono, fontSize = 13.sp),
                        placeholder = { Text("e.g. NC", fontSize = 11.sp) },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = PrimaryBlue,
                            focusedLabelColor = PrimaryBlue,
                        ),
                    )
                    Button(
                        onClick = onSend,
                        enabled = connectionState == ConnectionState.CONNECTED && commandInput.isNotBlank() && !isSending,
                        colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBlue),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp),
                        elevation = ButtonDefaults.elevation(0.dp),
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(Modifier.size(14.dp), Color.White, 2.dp)
                        } else {
                            Icon(Icons.Default.Send, null, Modifier.size(14.dp), tint = Color.White)
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Send", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
        Spacer(Modifier.weight(1f))
    }
}

// ─────────────────────────────────────────────────────────
//  COMPACT FIELD INPUTS (for builder panel)
// ─────────────────────────────────────────────────────────

@Composable
private fun CompactTextField(
    field: ThalesCommandField,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOpt = field.requirement == FieldRequirement.OPTIONAL
    val label = field.name + if (isOpt) " (opt)" else ""
    val hint = if (field.length > 0) "${field.length} chars" else "variable"

    FixedOutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        label = { Text(label, fontSize = 11.sp) },
        placeholder = {
            Text(hint, fontSize = 10.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.25f))
        },
        textStyle = LocalTextStyle.current.copy(fontFamily = Mono, fontSize = 12.sp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = PrimaryBlue,
            unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = if (isOpt) 0.1f else 0.2f),
            focusedLabelColor = PrimaryBlue,
        ),
    )
}

@Composable
private fun CompactFlagField(
    field: ThalesCommandField,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOpt = field.requirement == FieldRequirement.OPTIONAL
    val checked = if (isOpt) value.isNotEmpty() else value != "0"
    val label = field.name + if (isOpt) " (opt)" else ""

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { on ->
                if (isOpt) onValueChange(if (on) "%" else "")
                else onValueChange(if (on) "1" else "0")
            },
            colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue),
        )
        Text(label, fontSize = 11.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.85f))
    }
}

@Composable
private fun CompactDropdown(
    field: ThalesCommandField,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = field.options ?: return
    var expanded by remember { mutableStateOf(false) }
    var boxWidth by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val selectedLabel = options.find { it.value == value }?.label ?: value
    val isOpt = field.requirement == FieldRequirement.OPTIONAL
    val label = field.name + if (isOpt) " (opt)" else ""
    val canClear = field.omitFromWireWhenBlank && value.isNotEmpty()

    Row(modifier = modifier.onGloballyPositioned { boxWidth = it.size.width }, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f)) {
            FixedOutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                enabled = false,
                label = { Text(label, fontSize = 11.sp) },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontFamily = Mono, fontSize = 12.sp),
                trailingIcon = {
                    Icon(
                        if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        null, Modifier.size(18.dp),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                    )
                },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    disabledBorderColor = MaterialTheme.colors.onSurface.copy(alpha = if (isOpt) 0.1f else 0.2f),
                    disabledTextColor = MaterialTheme.colors.onSurface,
                    disabledLabelColor = MaterialTheme.colors.onSurface.copy(alpha = 0.55f),
                ),
            )
            Box(modifier = Modifier.matchParentSize().clickable { expanded = !expanded })
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width(with(density) { boxWidth.toDp() })
                    .heightIn(max = 400.dp),
            ) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        onClick = { onValueChange(opt.value); expanded = false },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp),
                    ) {
                        Text(opt.label, fontSize = 11.sp, fontFamily = Mono, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        if (canClear) {
            IconButton(
                onClick = { onValueChange("") },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = "Clear",
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.45f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
//  RESPONSE PANELS
// ─────────────────────────────────────────────────────────

@Composable
private fun MonospacePanel(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    content: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.background(MaterialTheme.colors.surface)) {
        Surface(modifier = Modifier.fillMaxWidth(), color = accentColor.copy(alpha = 0.05f)) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(icon, null, Modifier.size(12.dp), tint = accentColor)
                Text(
                    label, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = accentColor, letterSpacing = 1.sp,
                )
            }
        }
        Divider(color = accentColor.copy(alpha = 0.08f))
        SelectionContainer {
            val scrollState = rememberScrollState()
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Text(
                    text = content.ifBlank { "(awaiting data...)" },
                    modifier = Modifier.padding(10.dp).verticalScroll(scrollState).fillMaxSize(),
                    fontFamily = Mono, fontSize = 11.sp, lineHeight = 16.sp,
                    color = when {
                        content.isBlank() -> MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
                        isError -> AccentRed.copy(alpha = 0.85f)
                        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.85f)
                    },
                )
            }
        }
    }
}

@Composable
private fun ParsedFieldsView(
    parsedFields: List<Pair<ThalesCommandField, String>>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.background(MaterialTheme.colors.surface)) {
        Surface(modifier = Modifier.fillMaxWidth(), color = AccentGreen.copy(alpha = 0.05f)) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Default.TableChart, null, Modifier.size(12.dp), tint = AccentGreen)
                Text(
                    "PARSED FIELDS", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = AccentGreen, letterSpacing = 1.sp,
                )
            }
        }
        Divider(color = AccentGreen.copy(alpha = 0.08f))

        if (parsedFields.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.TableChart, null,
                        Modifier.size(32.dp),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Send a command to see parsed response fields",
                        fontSize = 12.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
                    )
                }
            }
        } else {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier.weight(1f).verticalScroll(scrollState).padding(10.dp),
            ) {
                // Column headers
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "FIELD", Modifier.width(140.dp),
                        fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                    )
                    Text(
                        "TYPE", Modifier.width(36.dp),
                        fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                    )
                    Text(
                        "VALUE", Modifier.weight(1f),
                        fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                    )
                }
                Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.06f))

                parsedFields.forEach { (field, value) ->
                    val isError = field.id == "errorCode" && value != "00"
                    val isSuccess = field.id == "errorCode" && value == "00"
                    val valueColor = when {
                        isError -> AccentRed
                        isSuccess -> AccentGreen
                        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                    }

                    val displayValue = if (field.id == "errorCode") {
                        val desc = ThalesErrorCodes.getDescription(value)
                        "$value ($desc)"
                    } else {
                        val optLabel = field.options?.find { it.value == value }?.label
                        if (optLabel != null) "$value ($optLabel)" else value
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            field.name, Modifier.width(140.dp),
                            fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                        )
                        Text(
                            field.type.name, Modifier.width(36.dp),
                            fontSize = 9.sp, fontFamily = Mono,
                            color = PrimaryBlue.copy(alpha = 0.5f),
                        )
                        SelectionContainer {
                            Text(
                                displayValue, modifier = Modifier.weight(1f),
                                fontSize = 11.sp, fontFamily = Mono,
                                fontWeight = if (isError) FontWeight.Bold else FontWeight.Normal,
                                color = valueColor,
                            )
                        }
                    }

                    val kb = KeyBlockInfo.parse(value)
                    if (kb != null) {
                        KeyBlockDecodedPanel(kb)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
//  KEY BLOCK DECODED
// ─────────────────────────────────────────────────────────

@Composable
private fun KeyBlockDecodedPanel(kb: KeyBlockInfo) {
    val kbColor = Color(0xFF2196F3)

    Surface(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 2.dp, bottom = 6.dp),
        shape = RoundedCornerShape(6.dp),
        color = kbColor.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, kbColor.copy(alpha = 0.12f)),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Default.Key, null, Modifier.size(11.dp), tint = kbColor)
                Text(
                    "KEY BLOCK DECODED", fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    color = kbColor, letterSpacing = 0.5.sp,
                )
            }
            Spacer(Modifier.height(4.dp))

            @Composable
            fun kbRow(label: String, code: String, desc: String = "") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        label, Modifier.width(110.dp), fontSize = 9.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                    )
                    Text(
                        code, fontSize = 10.sp, fontFamily = Mono,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.85f),
                    )
                    if (desc.isNotBlank()) {
                        Text(desc, fontSize = 9.sp, color = kbColor.copy(alpha = 0.7f))
                    }
                }
            }

            kbRow("Version", kb.version, "Thales AES Key Block")
            kbRow("Block Length", "${kb.blockLength}")
            kbRow("Key Usage", kb.keyUsage, kb.keyUsageDesc)
            kbRow("Algorithm", kb.algorithm, kb.algorithmDesc)
            kbRow("Mode of Use", kb.modeOfUse, kb.modeOfUseDesc)
            kbRow("Key Version", kb.keyVersionNumber)
            kbRow("Exportability", kb.exportability, kb.exportabilityDesc)
            kbRow("Optional Blocks", String.format("%02d", kb.numOptionalBlocks))
            kbRow("Reserved/LMK", kb.reserved)

            Spacer(Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(3.dp),
                color = AccentGreen.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Default.VpnKey, null, Modifier.size(10.dp), tint = AccentGreen)
                    Text(
                        "Clear Key: ${kb.clearKeyBits} bits (${kb.clearKeyBytes} bytes / ${kb.clearKeyHexChars}H chars)",
                        fontSize = 10.sp, fontWeight = FontWeight.Medium, color = AccentGreen,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Divider(color = kbColor.copy(alpha = 0.1f))
            Spacer(Modifier.height(4.dp))

            val padBytes = kb.encryptedKeyBytes - kb.clearKeyBytes
            Text(
                "Encrypted Key — ${kb.encryptedKeyBytes}B (${kb.encryptedKeyHex.length}H)",
                fontSize = 9.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f),
            )
            Text(
                "${kb.clearKeyBytes}B key + ${padBytes}B PKCS#7 pad → AES-CBC",
                fontSize = 8.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.35f),
            )
            SelectionContainer {
                Text(
                    kb.encryptedKeyHex, fontSize = 9.sp, fontFamily = Mono,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
                    modifier = Modifier.padding(top = 1.dp),
                )
            }

            Spacer(Modifier.height(3.dp))
            Text(
                "MAC — ${kb.macBytes}B (${kb.macHex.length}H)",
                fontSize = 9.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f),
            )
            SelectionContainer {
                Text(
                    kb.macHex, fontSize = 9.sp, fontFamily = Mono,
                    fontWeight = FontWeight.Medium,
                    color = AccentOrange.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
//  SIDEBAR COMPONENTS
// ─────────────────────────────────────────────────────────

@Composable
private fun SidebarSearch(query: String, onChange: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colors.background,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Default.Search, null, Modifier.size(14.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.35f),
            )
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        "Search...", fontSize = 11.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.35f),
                    )
                }
                BasicTextField(
                    value = query, onValueChange = onChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.caption.copy(
                        color = MaterialTheme.colors.onSurface, fontSize = 11.sp,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { onChange("") }, modifier = Modifier.size(16.dp)) {
                    Icon(
                        Icons.Default.Clear, null, Modifier.size(12.dp),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.35f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarCategory(name: String) {
    Text(
        name.uppercase(),
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 4.dp),
        fontSize = 9.sp, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.35f),
        letterSpacing = 1.2.sp,
    )
}

@Composable
private fun PaletteItem(
    code: String?,
    name: String,
    desc: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) PrimaryBlue.copy(alpha = 0.08f) else Color.Transparent
    val leftBorder = if (selected) PrimaryBlue else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(bg)
            .padding(start = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Active indicator bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(36.dp)
                .background(leftBorder, RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp)),
        )

        Spacer(Modifier.width(8.dp))

        if (code != null) {
            Surface(
                shape = RoundedCornerShape(3.dp),
                color = if (selected) PrimaryBlue.copy(alpha = 0.15f)
                else MaterialTheme.colors.onSurface.copy(alpha = 0.06f),
            ) {
                Text(
                    code,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                    fontFamily = Mono, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    color = if (selected) PrimaryBlue else MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
            }
            Spacer(Modifier.width(8.dp))
        } else {
            Icon(
                Icons.Default.Edit, null, Modifier.size(12.dp),
                tint = if (selected) PrimaryBlue else MaterialTheme.colors.onSurface.copy(alpha = 0.35f),
            )
            Spacer(Modifier.width(8.dp))
        }

        Column(modifier = Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(
                name, fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) PrimaryBlue else MaterialTheme.colors.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                desc, fontSize = 9.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.35f),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
    }
}

// ─────────────────────────────────────────────────────────
//  STATUS + TAB COMPONENTS
// ─────────────────────────────────────────────────────────

@Composable
private fun StatusBadge(result: HsmCommandResult) {
    val color = if (result.success) AccentGreen else AccentRed
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(
                if (result.success) Icons.Default.CheckCircle else Icons.Default.Cancel,
                null, Modifier.size(12.dp), tint = color,
            )
            Text(
                if (result.success) "OK" else "ERR",
                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                fontFamily = Mono, color = color,
            )
        }
        Text(
            "${result.elapsedMs}ms",
            fontSize = 10.sp, fontFamily = Mono,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
        )
        Text(
            "${result.rawRequest.size}B↑ ${result.rawResponse.size}B↓",
            fontSize = 10.sp, fontFamily = Mono,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
        )
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        color = if (selected) PrimaryBlue.copy(alpha = 0.1f) else Color.Transparent,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) PrimaryBlue else MaterialTheme.colors.onSurface.copy(alpha = 0.45f),
        )
    }
}
