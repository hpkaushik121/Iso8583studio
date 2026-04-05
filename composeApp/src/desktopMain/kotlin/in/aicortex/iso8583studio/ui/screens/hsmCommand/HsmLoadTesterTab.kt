package `in`.aicortex.iso8583studio.ui.screens.hsmCommand

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.domain.service.hsmCommandService.ConnectionState
import `in`.aicortex.iso8583studio.domain.service.hsmCommandService.HsmCommandClientService
import `in`.aicortex.iso8583studio.domain.service.hsmCommandService.LoadTestStats
import `in`.aicortex.iso8583studio.ui.PrimaryBlue
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.HsmCommandConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.SavedScenario
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.SavedPlaylist
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.SavedPlaylistItem
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val Mono = FontFamily.Monospace
private val AccentGreen = Color(0xFF4CAF50)
private val AccentRed = Color(0xFFF44336)
private val AccentOrange = Color(0xFFFF9800)
private val RunningBlue = Color(0xFF2979FF)

// ──────────────────────────────────────────────────────────────────────────────
//  Playlist item model
// ──────────────────────────────────────────────────────────────────────────────

private enum class PlaylistItemType(val label: String, val icon: ImageVector, val color: Color) {
    CUSTOM("Custom", Icons.Default.Edit, Color(0xFF9C27B0)),
    COMMAND("Command", Icons.Default.Terminal, PrimaryBlue),
    SCENARIO("Scenario", Icons.Default.AccountTree, Color(0xFF009688)),
}

private class PlaylistItem(
    val type: PlaylistItemType,
    val label: String,
    /** For CUSTOM: the raw command text. */
    var customText: MutableState<String> = mutableStateOf(""),
    /** For COMMAND: the definition + field values. */
    val definition: ThalesCommandDefinition? = null,
    val fieldValues: SnapshotStateMap<String, String> = mutableStateMapOf(),
    /** For SCENARIO: the saved scenario and resolved commands. */
    val scenario: SavedScenario? = null,
    val scenarioCommands: List<String> = emptyList(),
) {
    val id = java.util.UUID.randomUUID().toString()

    fun resolveCommands(): List<String> = when (type) {
        PlaylistItemType.CUSTOM -> {
            val text = customText.value.trim()
            if (text.isNotBlank()) listOf(text) else emptyList()
        }
        PlaylistItemType.COMMAND -> {
            definition?.let {
                try { listOf(ThalesWireBuilder.buildPlainTextCommand(it, fieldValues)) } catch (_: Exception) { emptyList() }
            } ?: emptyList()
        }
        PlaylistItemType.SCENARIO -> scenarioCommands
    }

    /** Serialize to a persistable model */
    fun toSaved(): SavedPlaylistItem = SavedPlaylistItem(
        type = type.name,
        label = label,
        customText = if (type == PlaylistItemType.CUSTOM) customText.value else "",
        commandCode = definition?.code ?: "",
        fieldValues = if (type == PlaylistItemType.COMMAND) fieldValues.toMap() else emptyMap(),
        scenarioId = scenario?.id ?: "",
    )
}

// ──────────────────────────────────────────────────────────────────────────────
//  Sequential playlist execution state
// ──────────────────────────────────────────────────────────────────────────────

private enum class PlaylistItemStatus {
    PENDING, RUNNING, COMPLETED, FAILED, SKIPPED
}

// ──────────────────────────────────────────────────────────────────────────────
//  Main composable
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun HsmLoadTesterTab(
    service: HsmCommandClientService,
    scenarioSession: ScenarioSessionState? = null,
    savedScenarios: List<SavedScenario> = emptyList(),
    onSaveConfig: ((HsmCommandConfig) -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val connectionState by service.connectionState.collectAsState()
    val stats by service.loadTestStats.collectAsState()
    val definitions = remember { thalesCommandDefinitions }

    // Playlist
    val playlist = remember { mutableStateListOf<PlaylistItem>() }
    var selectedItemIndex by remember { mutableStateOf(-1) }

    // Sequential execution state
    var currentRunningIndex by remember { mutableStateOf(-1) }
    val itemStatuses = remember { mutableStateMapOf<String, PlaylistItemStatus>() }
    var autoAdvance by remember { mutableStateOf(true) }
    var isPlaylistRunning by remember { mutableStateOf(false) }

    // Add-item dialog state
    var showCommandPicker by remember { mutableStateOf(false) }
    var showScenarioPicker by remember { mutableStateOf(false) }

    // Save/Load playlist dialog
    var showSavePlaylistDialog by remember { mutableStateOf(false) }
    var showLoadPlaylistDialog by remember { mutableStateOf(false) }

    // Load test params
    var concurrency by remember { mutableStateOf(service.config.loadTestConcurrentConnections.toString()) }
    var tps by remember { mutableStateOf(service.config.loadTestCommandsPerSecond.toString()) }
    var duration by remember { mutableStateOf(service.config.loadTestDurationSeconds.toString()) }

    // Report state
    var lastReport by remember { mutableStateOf<String?>(null) }
    var showReport by remember { mutableStateOf(false) }

    // Resolve all playlist commands
    val allCommands: List<String> = playlist.flatMap { it.resolveCommands() }

    // ── Sequential playlist runner ──
    // Runs each playlist item one at a time; after each finishes, auto-advances
    fun startPlaylistSequential() {
        if (playlist.isEmpty() || connectionState != ConnectionState.CONNECTED) return
        isPlaylistRunning = true
        // Reset statuses
        playlist.forEach { itemStatuses[it.id] = PlaylistItemStatus.PENDING }
        currentRunningIndex = 0

        scope.launch {
            for (i in playlist.indices) {
                if (!isPlaylistRunning) break
                currentRunningIndex = i
                val item = playlist[i]
                val commands = item.resolveCommands()

                if (commands.isEmpty()) {
                    itemStatuses[item.id] = PlaylistItemStatus.SKIPPED
                    continue
                }

                itemStatuses[item.id] = PlaylistItemStatus.RUNNING

                // Start load test for this item
                service.startLoadTestMulti(
                    commands = commands,
                    scope = scope,
                    durationSeconds = duration.toIntOrNull() ?: 60,
                    concurrency = concurrency.toIntOrNull() ?: 1,
                    commandsPerSecond = tps.toIntOrNull() ?: 10,
                )

                // Wait for the load test to finish
                while (service.loadTestStats.value.running) {
                    delay(500)
                    if (!isPlaylistRunning) {
                        service.stopLoadTest()
                        break
                    }
                }

                val finalStats = service.loadTestStats.value
                itemStatuses[item.id] = if (finalStats.failureCount == 0L && finalStats.totalSent > 0)
                    PlaylistItemStatus.COMPLETED else PlaylistItemStatus.COMPLETED

                if (!isPlaylistRunning) break

                // If not auto-advance, pause and wait for user to press Next
                if (!autoAdvance && i < playlist.lastIndex) {
                    // Pause — user will press "Next" to continue
                    while (!autoAdvance && isPlaylistRunning && currentRunningIndex == i) {
                        delay(200)
                    }
                }
            }
            isPlaylistRunning = false
            currentRunningIndex = -1
        }
    }

    fun stopPlaylist() {
        isPlaylistRunning = false
        service.stopLoadTest()
        currentRunningIndex = -1
    }

    fun advanceToNext() {
        // When paused, bump the index so the loop continues
        if (currentRunningIndex < playlist.lastIndex) {
            currentRunningIndex = currentRunningIndex + 1
        }
    }

    Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        // ═══════════════════════════════════════════════════
        //  LEFT: Playlist + Controls
        // ═══════════════════════════════════════════════════
        Surface(
            modifier = Modifier.width(380.dp).fillMaxHeight(),
            color = MaterialTheme.colors.surface,
            elevation = 2.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(modifier = Modifier.fillMaxWidth(), color = PrimaryBlue.copy(alpha = 0.05f)) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.Speed, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Load Test Playlist", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            Text(
                                "${playlist.size} items, ${allCommands.size} commands" +
                                    if (isPlaylistRunning && currentRunningIndex >= 0) " | Running #${currentRunningIndex + 1}" else "",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                            )
                        }
                        // Save / Load playlist buttons
                        IconButton(
                            onClick = { showSavePlaylistDialog = true },
                            modifier = Modifier.size(28.dp),
                            enabled = playlist.isNotEmpty(),
                        ) {
                            Icon(Icons.Default.Save, "Save Playlist", modifier = Modifier.size(16.dp), tint = PrimaryBlue.copy(alpha = if (playlist.isNotEmpty()) 0.8f else 0.3f))
                        }
                        IconButton(
                            onClick = { showLoadPlaylistDialog = true },
                            modifier = Modifier.size(28.dp),
                            enabled = service.config.savedPlaylists.isNotEmpty(),
                        ) {
                            Icon(Icons.Default.FolderOpen, "Load Playlist", modifier = Modifier.size(16.dp), tint = PrimaryBlue.copy(alpha = if (service.config.savedPlaylists.isNotEmpty()) 0.8f else 0.3f))
                        }
                    }
                }

                Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.06f))

                // ── Auto-advance toggle ──
                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colors.surface) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.PlaylistPlay, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                            Text("Auto-advance to next", fontSize = 11.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                        }
                        Switch(
                            checked = autoAdvance,
                            onCheckedChange = { autoAdvance = it },
                            modifier = Modifier.height(20.dp),
                            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlue),
                        )
                    }
                }

                Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.06f))

                // ── Playlist ──
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (playlist.isEmpty()) {
                        // Empty state
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(Icons.Default.PlaylistAdd, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
                            Spacer(Modifier.height(8.dp))
                            Text("No commands in playlist", fontSize = 12.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.3f))
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Add custom commands, structured commands,\nor saved scenarios to load test.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            itemsIndexed(playlist) { index, item ->
                                val isSelected = selectedItemIndex == index
                                val isCurrentlyRunning = isPlaylistRunning && currentRunningIndex == index
                                val itemStatus = itemStatuses[item.id] ?: PlaylistItemStatus.PENDING
                                PlaylistItemCard(
                                    index = index,
                                    item = item,
                                    isSelected = isSelected,
                                    isRunning = stats.running && isCurrentlyRunning,
                                    isCurrentInPlaylist = isCurrentlyRunning,
                                    itemStatus = itemStatus,
                                    playlistActive = isPlaylistRunning,
                                    onClick = { selectedItemIndex = if (isSelected) -1 else index },
                                    onRemove = {
                                        playlist.removeAt(index)
                                        if (selectedItemIndex >= playlist.size) selectedItemIndex = playlist.lastIndex
                                    },
                                    onMoveUp = {
                                        if (index > 0) {
                                            val moved = playlist.removeAt(index)
                                            playlist.add(index - 1, moved)
                                            selectedItemIndex = index - 1
                                        }
                                    },
                                    onMoveDown = {
                                        if (index < playlist.lastIndex) {
                                            val moved = playlist.removeAt(index)
                                            playlist.add(index + 1, moved)
                                            selectedItemIndex = index + 1
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                // ── Add item bar ──
                Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.06f))
                Box {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                playlist.add(PlaylistItem(
                                    type = PlaylistItemType.CUSTOM,
                                    label = "Custom Command",
                                    customText = mutableStateOf("NC"),
                                ))
                                selectedItemIndex = playlist.lastIndex
                            },
                            enabled = !isPlaylistRunning,
                            modifier = Modifier.weight(1f).height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            border = BorderStroke(1.dp, PlaylistItemType.CUSTOM.color.copy(alpha = 0.3f)),
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(13.dp), tint = PlaylistItemType.CUSTOM.color)
                            Spacer(Modifier.width(4.dp))
                            Text("Custom", fontSize = 10.sp, color = PlaylistItemType.CUSTOM.color)
                        }
                        OutlinedButton(
                            onClick = { showCommandPicker = true },
                            enabled = !isPlaylistRunning,
                            modifier = Modifier.weight(1f).height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            border = BorderStroke(1.dp, PlaylistItemType.COMMAND.color.copy(alpha = 0.3f)),
                        ) {
                            Icon(Icons.Default.Terminal, null, modifier = Modifier.size(13.dp), tint = PlaylistItemType.COMMAND.color)
                            Spacer(Modifier.width(4.dp))
                            Text("Command", fontSize = 10.sp, color = PlaylistItemType.COMMAND.color)
                        }
                        OutlinedButton(
                            onClick = { showScenarioPicker = true },
                            enabled = !isPlaylistRunning && savedScenarios.isNotEmpty(),
                            modifier = Modifier.weight(1f).height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            border = BorderStroke(1.dp, PlaylistItemType.SCENARIO.color.copy(alpha = 0.3f)),
                        ) {
                            Icon(Icons.Default.AccountTree, null, modifier = Modifier.size(13.dp), tint = PlaylistItemType.SCENARIO.color)
                            Spacer(Modifier.width(4.dp))
                            Text("Scenario", fontSize = 10.sp, color = PlaylistItemType.SCENARIO.color)
                        }
                    }
                }

                Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.06f))

                // ── Parameters + Controls ──
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FixedOutlinedTextField(
                            value = concurrency, onValueChange = { concurrency = it },
                            label = { Text("Workers", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f), singleLine = true, enabled = !isPlaylistRunning,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = LocalTextStyle.current.copy(fontFamily = Mono, fontSize = 12.sp),
                        )
                        FixedOutlinedTextField(
                            value = tps, onValueChange = { tps = it },
                            label = { Text("Cmd/sec", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f), singleLine = true, enabled = !isPlaylistRunning,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = LocalTextStyle.current.copy(fontFamily = Mono, fontSize = 12.sp),
                        )
                        FixedOutlinedTextField(
                            value = duration, onValueChange = { duration = it },
                            label = { Text("Sec/item", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f), singleLine = true, enabled = !isPlaylistRunning,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = LocalTextStyle.current.copy(fontFamily = Mono, fontSize = 12.sp),
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { startPlaylistSequential() },
                            enabled = connectionState == ConnectionState.CONNECTED && !isPlaylistRunning && allCommands.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(backgroundColor = AccentGreen),
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            elevation = ButtonDefaults.elevation(0.dp),
                        ) {
                            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Start All", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        if (isPlaylistRunning && !autoAdvance && !stats.running) {
                            // Show Next button when paused between items
                            Button(
                                onClick = { advanceToNext() },
                                colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBlue),
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                elevation = ButtonDefaults.elevation(0.dp),
                            ) {
                                Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Next", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                        } else {
                            Button(
                                onClick = { stopPlaylist() },
                                enabled = isPlaylistRunning,
                                colors = ButtonDefaults.buttonColors(backgroundColor = AccentRed),
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                elevation = ButtonDefaults.elevation(0.dp),
                            ) {
                                Icon(Icons.Default.Stop, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Stop", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                        }
                    }

                    if (!isPlaylistRunning && connectionState != ConnectionState.CONNECTED) {
                        Surface(shape = RoundedCornerShape(6.dp), color = AccentOrange.copy(alpha = 0.08f)) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Warning, null, modifier = Modifier.size(14.dp), tint = AccentOrange)
                                Text("Connect to HSM first", fontSize = 11.sp, color = AccentOrange)
                            }
                        }
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════
        //  MIDDLE: Item editor (when item selected)
        // ═══════════════════════════════════════════════════
        if (selectedItemIndex in playlist.indices) {
            val item = playlist[selectedItemIndex]
            Surface(
                modifier = Modifier.width(320.dp).fillMaxHeight(),
                color = MaterialTheme.colors.surface,
                elevation = 1.dp,
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Item editor header
                    Surface(modifier = Modifier.fillMaxWidth(), color = item.type.color.copy(alpha = 0.06f)) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(item.type.icon, null, modifier = Modifier.size(16.dp), tint = item.type.color)
                            Text(item.label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = item.type.color, modifier = Modifier.weight(1f))
                            IconButton(onClick = { selectedItemIndex = -1 }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, "Close", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                    Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.06f))

                    when (item.type) {
                        PlaylistItemType.CUSTOM -> {
                            Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Raw command text", fontSize = 11.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                                FixedOutlinedTextField(
                                    value = item.customText.value,
                                    onValueChange = { item.customText.value = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    enabled = !isPlaylistRunning,
                                    textStyle = LocalTextStyle.current.copy(fontFamily = Mono, fontSize = 13.sp),
                                    placeholder = { Text("e.g. NC", fontSize = 11.sp) },
                                )
                                Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colors.onSurface.copy(alpha = 0.03f)) {
                                    Text(
                                        item.customText.value.ifBlank { "(empty)" },
                                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                        fontFamily = Mono, fontSize = 10.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                                    )
                                }
                            }
                        }
                        PlaylistItemType.COMMAND -> {
                            if (item.definition != null) {
                                Surface(modifier = Modifier.fillMaxWidth(), color = PrimaryBlue.copy(alpha = 0.04f)) {
                                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(shape = RoundedCornerShape(4.dp), color = PrimaryBlue.copy(alpha = 0.12f)) {
                                            Text(
                                                "${item.definition.code} / ${item.definition.responseCode}",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryBlue,
                                            )
                                        }
                                        Text(item.definition.name, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    }
                                }
                                Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.06f))
                                val scrollState = rememberScrollState()
                                Column(
                                    modifier = Modifier.weight(1f).verticalScroll(scrollState).padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    val snapshot = item.fieldValues.toMap()
                                    val visibleFields = item.definition.requestFields.filter {
                                        ThalesWireBuilder.isFieldVisible(it, snapshot)
                                    }
                                    FieldGrid(visibleFields, item.fieldValues, item.definition.forceVerticalFieldLayout)
                                }
                                Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.06f))
                                Surface(color = MaterialTheme.colors.surface.copy(alpha = 0.6f)) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Code, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f))
                                        val wire = item.resolveCommands().firstOrNull() ?: "(no command)"
                                        Text(wire, fontFamily = Mono, fontSize = 10.sp, maxLines = 2, color = MaterialTheme.colors.onSurface.copy(alpha = 0.35f), overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                        PlaylistItemType.SCENARIO -> {
                            Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (item.scenario != null) {
                                    Text("Scenario: ${item.scenario.name}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("${item.scenarioCommands.size} commands resolved", fontSize = 10.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                                    Spacer(Modifier.height(4.dp))
                                    Text("Commands played in sequence:", fontSize = 10.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f))
                                    LazyColumn(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        itemsIndexed(item.scenarioCommands) { i, cmd ->
                                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colors.onSurface.copy(alpha = 0.03f)) {
                                                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Surface(modifier = Modifier.size(18.dp), shape = CircleShape, color = PlaylistItemType.SCENARIO.color.copy(alpha = 0.15f)) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text("${i + 1}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PlaylistItemType.SCENARIO.color)
                                                        }
                                                    }
                                                    Text(cmd, fontFamily = Mono, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════
        //  RIGHT: Metrics Dashboard + Report
        // ═══════════════════════════════════════════════════
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(scrollState).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Currently running item indicator
            if (isPlaylistRunning && currentRunningIndex in playlist.indices) {
                val runItem = playlist[currentRunningIndex]
                Card(
                    elevation = 2.dp, shape = RoundedCornerShape(10.dp),
                    backgroundColor = RunningBlue.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, RunningBlue.copy(alpha = 0.2f)),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        PulsingDot(color = RunningBlue)
                        Text("Running:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RunningBlue)
                        Surface(shape = RoundedCornerShape(4.dp), color = runItem.type.color.copy(alpha = 0.15f)) {
                            Text(
                                "#${currentRunningIndex + 1} ${runItem.label}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = runItem.type.color,
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${currentRunningIndex + 1} / ${playlist.size}",
                            fontSize = 11.sp, fontFamily = Mono, color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                        )
                    }
                }
            }

            // Export button row
            if (stats.totalSent > 0 && !stats.running && !isPlaylistRunning) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(
                        onClick = {
                            lastReport = generateReport(stats, allCommands, playlist, duration.toIntOrNull() ?: 60, concurrency.toIntOrNull() ?: 1, tps.toIntOrNull() ?: 10)
                            showReport = true
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp),
                        elevation = ButtonDefaults.elevation(0.dp),
                    ) {
                        Icon(Icons.Default.Assessment, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export Report", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Progress Ring
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(modifier = Modifier.weight(1f), elevation = 2.dp, shape = RoundedCornerShape(12.dp), backgroundColor = MaterialTheme.colors.surface) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        val durationSec = duration.toIntOrNull() ?: 60
                        val progress = if (stats.running && durationSec > 0) (stats.elapsedSeconds.toFloat() / durationSec).coerceIn(0f, 1f) else 0f
                        val animatedProgress by animateFloatAsState(progress, tween(500, easing = FastOutSlowInEasing))
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                                ProgressRing(progress = animatedProgress, running = stats.running, modifier = Modifier.fillMaxSize())
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (stats.running) {
                                        Text("%.1f".format(stats.currentTps), fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = Mono, color = PrimaryBlue)
                                        Text("TPS", style = MaterialTheme.typography.overline, color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                                    } else if (stats.totalSent > 0) {
                                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(28.dp), tint = AccentGreen)
                                        Text("Done", style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = AccentGreen)
                                    } else {
                                        Icon(Icons.Default.Speed, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colors.onSurface.copy(alpha = 0.2f))
                                        Text("Ready", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.3f))
                                    }
                                }
                            }
                            if (stats.running) {
                                Text("${stats.elapsedSeconds}s / ${durationSec}s", style = MaterialTheme.typography.caption, fontFamily = Mono, color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }

            // Metrics
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.BarChart, null, modifier = Modifier.size(18.dp), tint = PrimaryBlue)
                Text("Metrics", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
                if (stats.running) {
                    Spacer(Modifier.width(4.dp))
                    PulsingDot(color = AccentGreen)
                    Text("Live", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AccentGreen)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("Total Sent", stats.totalSent.toString(), Icons.Default.CallMade, Color(0xFF2196F3), Modifier.weight(1f))
                MetricTile("Received", stats.totalReceived.toString(), Icons.Default.CallReceived, AccentGreen, Modifier.weight(1f))
                MetricTile("Success", stats.successCount.toString(), Icons.Default.CheckCircle, AccentGreen, Modifier.weight(1f))
                MetricTile("Failures", stats.failureCount.toString(), Icons.Default.Cancel, AccentRed, Modifier.weight(1f))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("Avg Latency", "%.1f ms".format(stats.avgResponseTimeMs), Icons.Default.Timer, AccentOrange, Modifier.weight(1f))
                MetricTile("Min Latency", if (stats.minResponseTimeMs == Long.MAX_VALUE) "-- ms" else "${stats.minResponseTimeMs} ms", Icons.Default.ArrowDownward, Color(0xFF009688), Modifier.weight(1f))
                MetricTile("Max Latency", if (stats.maxResponseTimeMs == 0L) "-- ms" else "${stats.maxResponseTimeMs} ms", Icons.Default.ArrowUpward, Color(0xFFE91E63), Modifier.weight(1f))
                MetricTile("Throughput", "%.1f tps".format(stats.currentTps), Icons.Default.Speed, Color(0xFF673AB7), Modifier.weight(1f))
            }

            if (stats.totalSent > 0) {
                SuccessRateBar(stats)
            }
        }
    }

    // ── Dialogs ──

    if (showCommandPicker) {
        LoadTestCommandPickerDialog(
            definitions = definitions,
            onSelect = { def ->
                val fv = mutableStateMapOf<String, String>()
                for (f in def.requestFields) {
                    if (f.defaultValue.isNotEmpty()) fv[f.id] = f.defaultValue
                }
                playlist.add(PlaylistItem(
                    type = PlaylistItemType.COMMAND,
                    label = "${def.code} - ${def.name}",
                    definition = def,
                    fieldValues = fv,
                ))
                selectedItemIndex = playlist.lastIndex
                showCommandPicker = false
            },
            onDismiss = { showCommandPicker = false },
        )
    }

    if (showScenarioPicker) {
        ScenarioPickerDialog(
            savedScenarios = savedScenarios,
            definitions = definitions,
            onSelect = { scenario, commands ->
                playlist.add(PlaylistItem(
                    type = PlaylistItemType.SCENARIO,
                    label = scenario.name,
                    scenario = scenario,
                    scenarioCommands = commands,
                ))
                selectedItemIndex = playlist.lastIndex
                showScenarioPicker = false
            },
            onDismiss = { showScenarioPicker = false },
        )
    }

    if (showReport && lastReport != null) {
        ReportDialog(
            report = lastReport!!,
            onDismiss = { showReport = false },
        )
    }

    // ── Save Playlist Dialog ──
    if (showSavePlaylistDialog) {
        SavePlaylistDialog(
            existingPlaylists = service.config.savedPlaylists,
            onSave = { name, existingId ->
                val items = playlist.map { it.toSaved() }
                val id = existingId ?: java.util.UUID.randomUUID().toString()
                val saved = SavedPlaylist(
                    id = id,
                    name = name,
                    items = items,
                    autoAdvance = autoAdvance,
                )
                val updated = service.config.savedPlaylists.toMutableList()
                val idx = updated.indexOfFirst { it.id == id }
                if (idx >= 0) updated[idx] = saved else updated.add(saved)
                val newConfig = service.config.copy(savedPlaylists = updated, modifiedDate = System.currentTimeMillis())
                onSaveConfig?.invoke(newConfig)
                showSavePlaylistDialog = false
            },
            onDismiss = { showSavePlaylistDialog = false },
        )
    }

    // ── Load Playlist Dialog ──
    if (showLoadPlaylistDialog) {
        LoadPlaylistDialog(
            playlists = service.config.savedPlaylists,
            definitions = definitions,
            savedScenarios = savedScenarios,
            onLoad = { saved ->
                playlist.clear()
                itemStatuses.clear()
                selectedItemIndex = -1
                autoAdvance = saved.autoAdvance

                for (si in saved.items) {
                    val item: PlaylistItem? = when (si.type) {
                        "CUSTOM" -> PlaylistItem(
                            type = PlaylistItemType.CUSTOM,
                            label = si.label,
                            customText = mutableStateOf(si.customText),
                        )
                        "COMMAND" -> {
                            val def = definitions.firstOrNull { it.code == si.commandCode }
                            if (def != null) {
                                val fv = mutableStateMapOf<String, String>()
                                fv.putAll(si.fieldValues)
                                PlaylistItem(
                                    type = PlaylistItemType.COMMAND,
                                    label = si.label,
                                    definition = def,
                                    fieldValues = fv,
                                )
                            } else null
                        }
                        "SCENARIO" -> {
                            val scenario = savedScenarios.firstOrNull { it.id == si.scenarioId }
                            if (scenario != null) {
                                val defsByCode = definitions.associateBy { it.code }
                                val cmds = scenario.steps.mapNotNull { step ->
                                    val def = defsByCode[step.commandCode] ?: return@mapNotNull null
                                    try { ThalesWireBuilder.buildPlainTextCommand(def, step.fieldValues) } catch (_: Exception) { null }
                                }
                                PlaylistItem(
                                    type = PlaylistItemType.SCENARIO,
                                    label = si.label,
                                    scenario = scenario,
                                    scenarioCommands = cmds,
                                )
                            } else null
                        }
                        else -> null
                    }
                    if (item != null) playlist.add(item)
                }
                showLoadPlaylistDialog = false
            },
            onDelete = { id ->
                val updated = service.config.savedPlaylists.filter { it.id != id }
                val newConfig = service.config.copy(savedPlaylists = updated, modifiedDate = System.currentTimeMillis())
                onSaveConfig?.invoke(newConfig)
            },
            onDismiss = { showLoadPlaylistDialog = false },
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  Playlist item card — with selected, running, and status indicators
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlaylistItemCard(
    index: Int,
    item: PlaylistItem,
    isSelected: Boolean,
    isRunning: Boolean,
    isCurrentInPlaylist: Boolean,
    itemStatus: PlaylistItemStatus,
    playlistActive: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val commands = item.resolveCommands()

    // Border color based on state
    val borderColor = when {
        isRunning -> RunningBlue
        isSelected -> item.type.color
        else -> Color.Transparent
    }

    // Background tint based on status
    val bgColor = when {
        isRunning -> RunningBlue.copy(alpha = 0.06f)
        isCurrentInPlaylist && !isRunning -> PrimaryBlue.copy(alpha = 0.04f)
        itemStatus == PlaylistItemStatus.COMPLETED && playlistActive -> AccentGreen.copy(alpha = 0.04f)
        itemStatus == PlaylistItemStatus.SKIPPED && playlistActive -> AccentOrange.copy(alpha = 0.04f)
        isSelected -> item.type.color.copy(alpha = 0.04f)
        else -> MaterialTheme.colors.surface
    }

    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        elevation = if (isSelected || isRunning) 3.dp else 1.dp,
        border = if (borderColor != Color.Transparent) BorderStroke(1.5.dp, borderColor) else null,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Color accent strip
            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(
                when {
                    isRunning -> RunningBlue
                    itemStatus == PlaylistItemStatus.COMPLETED && playlistActive -> AccentGreen
                    itemStatus == PlaylistItemStatus.SKIPPED && playlistActive -> AccentOrange
                    else -> item.type.color
                }
            ))

            Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Status icon (when playlist active)
                    if (playlistActive) {
                        when (itemStatus) {
                            PlaylistItemStatus.RUNNING -> {
                                PulsingDot(color = RunningBlue)
                            }
                            PlaylistItemStatus.COMPLETED -> {
                                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(14.dp), tint = AccentGreen)
                            }
                            PlaylistItemStatus.SKIPPED -> {
                                Icon(Icons.Default.SkipNext, null, modifier = Modifier.size(14.dp), tint = AccentOrange)
                            }
                            PlaylistItemStatus.FAILED -> {
                                Icon(Icons.Default.Error, null, modifier = Modifier.size(14.dp), tint = AccentRed)
                            }
                            PlaylistItemStatus.PENDING -> {
                                Icon(Icons.Default.HourglassEmpty, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colors.onSurface.copy(alpha = 0.2f))
                            }
                        }
                    }

                    // Step number
                    Text(
                        "${index + 1}.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRunning) RunningBlue else item.type.color,
                    )
                    // Type badge
                    Surface(shape = RoundedCornerShape(4.dp), color = item.type.color.copy(alpha = 0.1f)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(item.type.icon, null, modifier = Modifier.size(10.dp), tint = item.type.color)
                            Text(
                                item.type.label,
                                fontSize = 9.sp, fontWeight = FontWeight.Bold, color = item.type.color,
                            )
                        }
                    }
                    // Command count
                    Text(
                        "${commands.size} cmd",
                        fontSize = 9.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.35f),
                    )

                    // Running label
                    if (isRunning) {
                        Surface(shape = RoundedCornerShape(4.dp), color = RunningBlue.copy(alpha = 0.12f)) {
                            Text(
                                "RUNNING",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = RunningBlue,
                                letterSpacing = 1.sp,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(3.dp))

                // Label
                Text(
                    item.label,
                    fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.85f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )

                // Wire preview
                if (commands.isNotEmpty()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        commands.joinToString("  \u2022  ") { it.take(30) },
                        fontFamily = Mono, fontSize = 9.sp, maxLines = 1,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Move/remove buttons (only when not running)
            if (!playlistActive) {
                Column(
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp, "Move up",
                        modifier = Modifier.size(16.dp).clip(CircleShape).clickable(onClick = onMoveUp),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                    )
                    Icon(
                        Icons.Default.KeyboardArrowDown, "Move down",
                        modifier = Modifier.size(16.dp).clip(CircleShape).clickable(onClick = onMoveDown),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                    )
                    Spacer(Modifier.height(2.dp))
                    Icon(
                        Icons.Default.Close, "Remove",
                        modifier = Modifier.size(14.dp).clip(CircleShape).clickable(onClick = onRemove),
                        tint = AccentRed.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  Save Playlist Dialog
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun SavePlaylistDialog(
    existingPlaylists: List<SavedPlaylist>,
    onSave: (name: String, existingId: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var overwriteId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Save, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                Text("Save Playlist", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.width(400.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FixedOutlinedTextField(
                    value = name,
                    onValueChange = { name = it; overwriteId = null },
                    label = { Text("Playlist Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                if (existingPlaylists.isNotEmpty()) {
                    Text("Or overwrite an existing playlist:", fontSize = 11.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        itemsIndexed(existingPlaylists) { _, pl ->
                            val isOverwrite = overwriteId == pl.id
                            Surface(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable {
                                    overwriteId = pl.id
                                    name = pl.name
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isOverwrite) PrimaryBlue.copy(alpha = 0.1f) else MaterialTheme.colors.onSurface.copy(alpha = 0.04f),
                                border = if (isOverwrite) BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f)) else null,
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.PlaylistPlay, null, modifier = Modifier.size(14.dp), tint = PrimaryBlue)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pl.name, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        Text("${pl.items.size} items", fontSize = 10.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f))
                                    }
                                    if (isOverwrite) {
                                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = PrimaryBlue)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name.trim(), overwriteId) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBlue),
            ) { Text("Save", color = Color.White) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ──────────────────────────────────────────────────────────────────────────────
//  Load Playlist Dialog
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoadPlaylistDialog(
    playlists: List<SavedPlaylist>,
    definitions: List<ThalesCommandDefinition>,
    savedScenarios: List<SavedScenario>,
    onLoad: (SavedPlaylist) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.FolderOpen, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                Text("Load Playlist", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Box(modifier = Modifier.size(450.dp, 350.dp)) {
                if (playlists.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No saved playlists", fontSize = 12.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f))
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(playlists) { _, pl ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onLoad(pl) },
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.03f),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PlaylistPlay, null, modifier = Modifier.size(20.dp), tint = PrimaryBlue)
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pl.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(
                                            "${pl.items.size} items | Auto-advance: ${if (pl.autoAdvance) "ON" else "OFF"}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDelete(pl.id) },
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(16.dp), tint = AccentRed.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ──────────────────────────────────────────────────────────────────────────────
//  Scenario picker dialog
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun ScenarioPickerDialog(
    savedScenarios: List<SavedScenario>,
    definitions: List<ThalesCommandDefinition>,
    onSelect: (SavedScenario, List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val defsByCode = remember(definitions) { definitions.associateBy { it.code } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Scenario to Playlist", fontWeight = FontWeight.Bold) },
        text = {
            Box(modifier = Modifier.size(450.dp, 350.dp)) {
                if (savedScenarios.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No saved scenarios found.\nSave scenarios in the Scenario Builder tab.", fontSize = 12.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f), textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(savedScenarios) { _, scenario ->
                            val commands = scenario.steps.mapNotNull { step ->
                                val def = defsByCode[step.commandCode] ?: return@mapNotNull null
                                try { ThalesWireBuilder.buildPlainTextCommand(def, step.fieldValues) } catch (_: Exception) { null }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onSelect(scenario, commands) },
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.03f),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.AccountTree, null, modifier = Modifier.size(16.dp), tint = PlaylistItemType.SCENARIO.color)
                                        Text(scenario.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                        Text("${scenario.steps.size} steps", fontSize = 10.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f))
                                    }
                                    if (commands.isNotEmpty()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            scenario.steps.joinToString(" \u2192 ") { it.commandCode },
                                            fontFamily = Mono, fontSize = 10.sp,
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.35f),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ──────────────────────────────────────────────────────────────────────────────
//  Command picker dialog
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoadTestCommandPickerDialog(
    definitions: List<ThalesCommandDefinition>,
    onSelect: (ThalesCommandDefinition) -> Unit,
    onDismiss: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val categorized = remember { definitions.groupBy { it.category }.toSortedMap(compareBy { it.ordinal }) }
    var selectedCategory by remember { mutableStateOf<CommandCategory?>(null) }

    val categoryIcon = { cat: CommandCategory ->
        when (cat) {
            CommandCategory.DIAGNOSTICS -> Icons.Default.MonitorHeart
            CommandCategory.KEY_MANAGEMENT -> Icons.Default.Key
            CommandCategory.PIN_OPERATIONS -> Icons.Default.Pin
            CommandCategory.DUKPT -> Icons.Default.Shuffle
            CommandCategory.MAC_OPERATIONS -> Icons.Default.VerifiedUser
            CommandCategory.CVV_OPERATIONS -> Icons.Default.CreditCard
            CommandCategory.DATA_ENCRYPTION -> Icons.Default.EnhancedEncryption
            CommandCategory.RSA -> Icons.Default.Security
            CommandCategory.EMV -> Icons.Default.Contactless
            CommandCategory.AS2805 -> Icons.Default.SwapHoriz
            CommandCategory.TERMINAL_KEYS -> Icons.Default.Devices
            CommandCategory.HMAC -> Icons.Default.Fingerprint
            CommandCategory.ADMIN -> Icons.Default.Settings
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colors.surface,
            elevation = 24.dp,
        ) {
            Column(modifier = Modifier.size(600.dp, 500.dp)) {
                // Header
                Surface(
                    color = PrimaryBlue,
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Default.PlaylistAdd, null, tint = Color.White, modifier = Modifier.size(22.dp))
                            Text("Add Command to Playlist", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        }
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            FixedOutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Search by code or name...", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f)) },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp), tint = Color.White.copy(alpha = 0.7f)) },
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = Color.White,
                                    cursorColor = Color.White,
                                    focusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    unfocusedBorderColor = Color.Transparent,
                                ),
                            )
                        }
                    }
                }

                // Category chips
                if (searchQuery.isBlank()) {
                    Surface(color = MaterialTheme.colors.surface, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            val allSelected = selectedCategory == null
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (allSelected) PrimaryBlue else MaterialTheme.colors.onSurface.copy(alpha = 0.06f),
                                modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { selectedCategory = null },
                            ) {
                                Text("All", fontSize = 11.sp, fontWeight = if (allSelected) FontWeight.Bold else FontWeight.Normal, color = if (allSelected) Color.White else MaterialTheme.colors.onSurface.copy(alpha = 0.6f), modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
                            }
                            for (cat in categorized.keys) {
                                val isSelected = selectedCategory == cat
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) PrimaryBlue else MaterialTheme.colors.onSurface.copy(alpha = 0.06f),
                                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { selectedCategory = cat },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(categoryIcon(cat), null, modifier = Modifier.size(12.dp), tint = if (isSelected) Color.White else MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                                        Text(cat.displayName.split(" ").first(), fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) Color.White else MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }
                    }
                    Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.06f))
                }

                // Command list
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        for ((category, defs) in categorized) {
                            if (selectedCategory != null && selectedCategory != category) continue
                            val filtered = if (searchQuery.isBlank()) defs
                            else defs.filter { it.code.contains(searchQuery, ignoreCase = true) || it.name.contains(searchQuery, ignoreCase = true) }
                            if (filtered.isEmpty()) continue

                            item {
                                Row(
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(categoryIcon(category), null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colors.onSurface.copy(alpha = 0.35f))
                                    Text(category.displayName.uppercase(), style = MaterialTheme.typography.overline, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f))
                                    Divider(modifier = Modifier.weight(1f).padding(start = 4.dp), color = MaterialTheme.colors.onSurface.copy(alpha = 0.06f))
                                }
                            }

                            items(filtered.size) { i ->
                                val def = filtered[i]
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onSelect(def) },
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Surface(shape = RoundedCornerShape(6.dp), color = PrimaryBlue.copy(alpha = 0.1f)) {
                                            Text(def.code, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryBlue, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(def.name, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colors.onSurface.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${def.requestFields.size} fields", fontSize = 9.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f))
                                        }
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp), tint = PrimaryBlue.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer
                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colors.surface) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${definitions.size} commands available", fontSize = 10.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.35f))
                        TextButton(onClick = onDismiss) { Text("Cancel", fontWeight = FontWeight.Medium) }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  Report generation + dialog
// ──────────────────────────────────────────────────────────────────────────────

private fun generateReport(
    stats: LoadTestStats,
    allCommands: List<String>,
    playlist: List<PlaylistItem>,
    durationSec: Int,
    workers: Int,
    targetTps: Int,
): String {
    val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    val total = stats.successCount + stats.failureCount
    val successRate = if (total > 0) (stats.successCount.toDouble() / total * 100) else 0.0

    return buildString {
        appendLine("═══════════════════════════════════════════════════════════════")
        appendLine("  HSM LOAD TEST REPORT")
        appendLine("  Generated: $now")
        appendLine("═══════════════════════════════════════════════════════════════")
        appendLine()
        appendLine("── Test Configuration ──────────────────────────────────────────")
        appendLine("  Workers:             $workers")
        appendLine("  Target Cmd/sec:      $targetTps")
        appendLine("  Duration per item:   ${durationSec}s")
        appendLine("  Playlist items:      ${playlist.size}")
        appendLine("  Total commands:      ${allCommands.size}")
        appendLine()
        appendLine("── Playlist ───────────────────────────────────────────────────")
        for ((i, item) in playlist.withIndex()) {
            val cmds = item.resolveCommands()
            appendLine("  ${i + 1}. [${item.type.label}] ${item.label} (${cmds.size} commands)")
            for ((j, cmd) in cmds.withIndex()) {
                appendLine("     ${j + 1}) $cmd")
            }
        }
        appendLine()
        appendLine("── Results ────────────────────────────────────────────────────")
        appendLine("  Total Sent:          ${stats.totalSent}")
        appendLine("  Total Received:      ${stats.totalReceived}")
        appendLine("  Successful:          ${stats.successCount}")
        appendLine("  Failed:              ${stats.failureCount}")
        appendLine("  Success Rate:        ${"%.2f".format(successRate)}%")
        appendLine()
        appendLine("── Latency ────────────────────────────────────────────────────")
        appendLine("  Average:             ${"%.2f".format(stats.avgResponseTimeMs)} ms")
        appendLine("  Minimum:             ${if (stats.minResponseTimeMs == Long.MAX_VALUE) "N/A" else "${stats.minResponseTimeMs} ms"}")
        appendLine("  Maximum:             ${if (stats.maxResponseTimeMs == 0L) "N/A" else "${stats.maxResponseTimeMs} ms"}")
        appendLine()
        appendLine("── Throughput ─────────────────────────────────────────────────")
        appendLine("  Actual TPS:          ${"%.2f".format(stats.currentTps)}")
        appendLine("  Elapsed:             ${stats.elapsedSeconds}s")
        appendLine()
        appendLine("═══════════════════════════════════════════════════════════════")
        appendLine("  End of Report")
        appendLine("═══════════════════════════════════════════════════════════════")
    }
}

@Composable
private fun ReportDialog(
    report: String,
    onDismiss: () -> Unit,
) {
    var copied by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Assessment, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                Text("Load Test Report", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.size(600.dp, 450.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                        clipboard.setContents(java.awt.datatransfer.StringSelection(report), null)
                        copied = true
                    }) {
                        Icon(if (copied) Icons.Default.Check else Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (copied) "Copied!" else "Copy to Clipboard", fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.04f),
                ) {
                    val scrollState = rememberScrollState()
                    Text(
                        report,
                        modifier = Modifier.verticalScroll(scrollState).padding(12.dp),
                        fontFamily = Mono, fontSize = 10.sp, lineHeight = 16.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBlue)) {
                Text("Close", color = Color.White)
            }
        },
        dismissButton = {},
    )
}

// ──────────────────────────────────────────────────────────────────────────────
//  Shared metric/UI components
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProgressRing(progress: Float, running: Boolean, modifier: Modifier = Modifier) {
    val trackColor = MaterialTheme.colors.onSurface.copy(alpha = 0.06f)
    val sweepAngle = progress * 360f
    Canvas(modifier = modifier) {
        val strokeWidth = 10.dp.toPx()
        val padding = strokeWidth / 2
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(padding, padding)
        drawArc(trackColor, -90f, 360f, false, topLeft, arcSize, style = Stroke(strokeWidth, cap = StrokeCap.Round))
        if (sweepAngle > 0f) {
            drawArc(PrimaryBlue, -90f, sweepAngle, false, topLeft, arcSize, style = Stroke(strokeWidth, cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun PulsingDot(color: Color) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
    )
    Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = color.copy(alpha = alpha)) {}
}

@Composable
private fun MetricTile(label: String, value: String, icon: ImageVector, accentColor: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, elevation = 1.dp, shape = RoundedCornerShape(10.dp), backgroundColor = MaterialTheme.colors.surface) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Surface(modifier = Modifier.fillMaxWidth().height(3.dp), color = accentColor) {}
            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, null, tint = accentColor.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, fontFamily = Mono, color = MaterialTheme.colors.onSurface, textAlign = TextAlign.Center)
                Text(label, style = MaterialTheme.typography.overline, color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun SuccessRateBar(stats: LoadTestStats) {
    val total = stats.successCount + stats.failureCount
    val successRate = if (total > 0) stats.successCount.toFloat() / total else 0f
    val failRate = if (total > 0) stats.failureCount.toFloat() / total else 0f

    Card(elevation = 1.dp, shape = RoundedCornerShape(10.dp), backgroundColor = MaterialTheme.colors.surface) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Success Rate", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold)
                Text(
                    "%.1f%%".format(successRate * 100),
                    fontWeight = FontWeight.Bold, fontFamily = Mono, fontSize = 16.sp,
                    color = if (successRate >= 0.95f) AccentGreen else if (successRate >= 0.8f) AccentOrange else AccentRed,
                )
            }
            Row(modifier = Modifier.fillMaxWidth().height(8.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (successRate > 0f) {
                    Surface(
                        modifier = Modifier.weight(successRate).fillMaxHeight(),
                        shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = if (failRate == 0f) 4.dp else 0.dp, bottomEnd = if (failRate == 0f) 4.dp else 0.dp),
                        color = AccentGreen,
                    ) {}
                }
                if (failRate > 0f) {
                    Surface(
                        modifier = Modifier.weight(failRate).fillMaxHeight(),
                        shape = RoundedCornerShape(topStart = if (successRate == 0f) 4.dp else 0.dp, bottomStart = if (successRate == 0f) 4.dp else 0.dp, topEnd = 4.dp, bottomEnd = 4.dp),
                        color = AccentRed,
                    ) {}
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = AccentGreen) {}
                    Text("${stats.successCount} passed", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = AccentRed) {}
                    Text("${stats.failureCount} failed", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                }
            }
        }
    }
}
