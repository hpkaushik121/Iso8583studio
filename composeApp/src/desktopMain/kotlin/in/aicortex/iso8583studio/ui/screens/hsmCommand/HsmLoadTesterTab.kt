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
import `in`.aicortex.iso8583studio.domain.service.hsmCommandService.LoadTestCommandLog
import `in`.aicortex.iso8583studio.domain.service.hsmCommandService.LoadTestStats
import `in`.aicortex.iso8583studio.ui.PrimaryBlue
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.HsmCommandConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.SavedScenario
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.SavedPlaylist
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.SavedPlaylistItem
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

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

/** Captured result of a single playlist item execution. */
private data class PlaylistItemResult(
    val itemIndex: Int,
    val itemLabel: String,
    val itemType: String,
    val commands: List<String>,
    val stats: LoadTestStats,
    val commandLogs: List<LoadTestCommandLog>,
    val startTime: Long,
    val endTime: Long,
)

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

    // Per-item results collected during playlist run
    val itemResults = remember { mutableStateListOf<PlaylistItemResult>() }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    var exportStep by remember { mutableStateOf("") }
    var exportProgress by remember { mutableStateOf(0f) }

    // Resolve all playlist commands
    val allCommands: List<String> = playlist.flatMap { it.resolveCommands() }

    // ── Sequential playlist runner ──
    // Runs each playlist item one at a time; after each finishes, auto-advances
    fun startPlaylistSequential() {
        if (playlist.isEmpty() || connectionState != ConnectionState.CONNECTED) return
        isPlaylistRunning = true
        itemResults.clear()
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
                service.clearCommandLogs()
                val itemStartTime = System.currentTimeMillis()

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

                val itemEndTime = System.currentTimeMillis()
                val finalStats = service.loadTestStats.value
                val logs = service.drainCommandLogs()

                itemResults.add(PlaylistItemResult(
                    itemIndex = i,
                    itemLabel = item.label,
                    itemType = item.type.label,
                    commands = commands,
                    stats = finalStats,
                    commandLogs = logs,
                    startTime = itemStartTime,
                    endTime = itemEndTime,
                ))

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
                if (isExporting) {
                    // ── Export progress card ──────────────────────────────────────────
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 2.dp,
                        shape = RoundedCornerShape(10.dp),
                        backgroundColor = MaterialTheme.colors.surface,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = PrimaryBlue,
                                    )
                                    Text(
                                        "Preparing report…",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colors.onSurface,
                                    )
                                }
                                Text(
                                    "${(exportProgress * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = Mono,
                                    color = PrimaryBlue,
                                )
                            }
                            LinearProgressIndicator(
                                progress = exportProgress,
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = PrimaryBlue,
                                backgroundColor = PrimaryBlue.copy(alpha = 0.12f),
                            )
                            if (exportStep.isNotEmpty()) {
                                Text(
                                    exportStep,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f),
                                    fontFamily = Mono,
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (exportMessage != null) {
                            Text(
                                exportMessage!!,
                                fontSize = 11.sp,
                                color = if (exportMessage!!.startsWith("Saved")) AccentGreen else AccentRed,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    isExporting = true
                                    exportStep = ""
                                    exportProgress = 0f
                                    exportMessage = null
                                    val html = withContext(Dispatchers.Default) {
                                        generateHtmlReport(
                                            service = service,
                                            itemResults = itemResults.toList(),
                                            playlist = playlist.toList(),
                                            allCommands = allCommands,
                                            durationSec = duration.toIntOrNull() ?: 60,
                                            workers = concurrency.toIntOrNull() ?: 1,
                                            targetTps = tps.toIntOrNull() ?: 10,
                                            finalStats = stats,
                                            onProgress = { step, progress ->
                                                exportStep = step
                                                exportProgress = progress
                                            },
                                        )
                                    }
                                    exportStep = "Opening save dialog…"
                                    exportProgress = 0.95f
                                    val saved = withContext(Dispatchers.IO) { saveHtmlReport(html) }
                                    exportMessage = saved
                                    isExporting = false
                                    exportStep = ""
                                    exportProgress = 0f
                                }
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

/** Show a file-save dialog and write the HTML report to the chosen location. Returns a status message. */
private fun saveHtmlReport(html: String): String {
    val chooser = JFileChooser().apply {
        dialogTitle = "Export Load Test Report"
        fileSelectionMode = JFileChooser.FILES_ONLY
        isAcceptAllFileFilterUsed = false
        fileFilter = FileNameExtensionFilter("HTML Report (.html)", "html")
        val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        selectedFile = java.io.File("HSM_LoadTest_Report_$ts.html")
    }
    val result = chooser.showSaveDialog(null)
    if (result != JFileChooser.APPROVE_OPTION) return "Export cancelled"
    val file = chooser.selectedFile.let {
        if (it.name.lowercase().endsWith(".html")) it else java.io.File("${it.absolutePath}.html")
    }
    return try {
        file.writeText(html, Charsets.UTF_8)
        "Saved to ${file.absolutePath}"
    } catch (e: Exception) {
        "Export failed: ${e.message}"
    }
}

private fun generateHtmlReport(
    service: HsmCommandClientService,
    itemResults: List<PlaylistItemResult>,
    playlist: List<PlaylistItem>,
    allCommands: List<String>,
    durationSec: Int,
    workers: Int,
    targetTps: Int,
    finalStats: LoadTestStats,
    onProgress: ((step: String, progress: Float) -> Unit)? = null,
): String {
    val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    val tsFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    val cfg = service.config

    // ── Pre-compute aggregates ──────────────────────────────────────────────────
    onProgress?.invoke("Computing statistics…", 0.02f)
    val totalBytesSent = itemResults.sumOf { ir -> ir.commandLogs.sumOf { it.bytesSent.toLong() } }
    val totalBytesRecv = itemResults.sumOf { ir -> ir.commandLogs.sumOf { it.bytesReceived.toLong() } }
    val totalCommands = itemResults.sumOf { it.commandLogs.size.toLong() }
    val overallSuccess = itemResults.sumOf { it.stats.successCount }
    val overallFailure = itemResults.sumOf { it.stats.failureCount }
    val overallTotal = overallSuccess + overallFailure
    val overallSuccessRate = if (overallTotal > 0) overallSuccess.toDouble() / overallTotal * 100 else 0.0
    val overallStartTime = itemResults.minOfOrNull { it.startTime } ?: System.currentTimeMillis()
    val overallEndTime = itemResults.maxOfOrNull { it.endTime } ?: System.currentTimeMillis()
    val overallDurationSec = ((overallEndTime - overallStartTime) / 1000.0).coerceAtLeast(0.001)

    onProgress?.invoke("Sorting latency data for percentiles…", 0.06f)
    val allLogs = itemResults.flatMap { it.commandLogs }
    val avgMs = if (allLogs.isNotEmpty()) allLogs.map { it.elapsedMs }.average() else 0.0
    val minMs = allLogs.minOfOrNull { it.elapsedMs } ?: 0
    val maxMs = allLogs.maxOfOrNull { it.elapsedMs } ?: 0
    val p95Ms = if (allLogs.isNotEmpty()) {
        val sorted = allLogs.map { it.elapsedMs }.sorted()
        sorted[(sorted.size * 0.95).toInt().coerceAtMost(sorted.lastIndex)]
    } else 0L

    fun h(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
    fun ts(ms: Long): String = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault()).format(tsFormatter)
    fun fullTs(ms: Long): String = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault()).format(dateFormatter)
    fun bytes(b: Long): String = when {
        b < 1024 -> "$b B"
        b < 1048576 -> "%.1f KB".format(b / 1024.0)
        else -> "%.2f MB".format(b / 1048576.0)
    }

    // Detail sections contribute the bulk of the output (one section per item).
    // We emit progress as we build each item block so the user sees activity.
    val detailSectionProgressStart = 0.45f
    val detailSectionProgressEnd   = 0.90f

    return buildString {
        // ── HTML shell + styles ────────────────────────────────────────────────
        onProgress?.invoke("Writing report header and styles…", 0.10f)
        append("""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>HSM Load Test Report</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:-apple-system,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#1a1a1a;line-height:1.6;font-size:13px;background:#fff}
.page{max-width:1100px;margin:0 auto;padding:40px 32px}
header{border-bottom:2px solid #1a1a1a;padding-bottom:16px;margin-bottom:32px}
header h1{font-size:20px;font-weight:600;letter-spacing:-0.3px}
header p{font-size:12px;color:#666;margin-top:2px}
h2{font-size:14px;font-weight:600;text-transform:uppercase;letter-spacing:0.5px;color:#1a1a1a;margin:28px 0 12px;padding-bottom:6px;border-bottom:1px solid #ddd}
h3{font-size:13px;font-weight:600;margin:0 0 8px}
section{margin-bottom:24px}
.meta{display:grid;grid-template-columns:repeat(auto-fill,minmax(220px,1fr));gap:0;border:1px solid #ddd;border-radius:3px;overflow:hidden}
.meta-item{padding:10px 14px;border-bottom:1px solid #eee;display:flex;justify-content:space-between;font-size:12px}
.meta-item:nth-child(odd){background:#fafafa}
.meta-item .k{color:#666}
.meta-item .v{font-weight:500;font-family:'SF Mono','Cascadia Code','Consolas',monospace;font-size:11px}
.stats{display:grid;grid-template-columns:repeat(4,1fr);gap:1px;background:#ddd;border:1px solid #ddd;border-radius:3px;overflow:hidden;margin-bottom:16px}
.stat{background:#fff;padding:14px 16px;text-align:center}
.stat .n{font-size:22px;font-weight:600;font-family:'SF Mono','Cascadia Code','Consolas',monospace;line-height:1.2}
.stat .l{font-size:10px;text-transform:uppercase;letter-spacing:0.5px;color:#888;margin-top:2px}
.stat .s{font-size:10px;color:#999;margin-top:1px}
table{width:100%;border-collapse:collapse;font-size:11px;margin-bottom:4px}
th{text-align:left;font-weight:600;font-size:10px;text-transform:uppercase;letter-spacing:0.3px;color:#666;padding:7px 10px;border-bottom:2px solid #ddd;background:#fafafa;position:sticky;top:0;z-index:1}
td{padding:5px 10px;border-bottom:1px solid #eee;font-family:'SF Mono','Cascadia Code','Consolas',monospace;font-size:11px;white-space:nowrap;max-width:260px;overflow:hidden;text-overflow:ellipsis}
tr:hover td{background:#f8f8f8}
.t-wrap{border:1px solid #ddd;border-radius:3px;overflow:auto;max-height:460px}
.ok{color:#1a8a1a;font-weight:600}
.fail{color:#c00;font-weight:600}
.item-block{border:1px solid #ddd;border-radius:3px;margin-bottom:20px;overflow:hidden}
.item-hdr{background:#fafafa;padding:10px 14px;border-bottom:1px solid #ddd;display:flex;align-items:baseline;gap:10px}
.item-hdr .idx{font-weight:700;font-size:14px;color:#333;min-width:24px}
.item-hdr .lbl{font-weight:600;font-size:13px}
.item-hdr .info{font-size:11px;color:#888;margin-left:auto}
.item-body{padding:14px}
.item-stats{display:grid;grid-template-columns:repeat(6,1fr);gap:1px;background:#eee;border:1px solid #eee;border-radius:3px;overflow:hidden;margin-bottom:12px}
.item-stats .stat{padding:8px 10px}
.item-stats .stat .n{font-size:15px}
details{margin-bottom:10px}
summary{cursor:pointer;font-size:12px;font-weight:500;color:#333;padding:4px 0}
summary:hover{color:#000}
footer{border-top:1px solid #ddd;padding-top:16px;margin-top:40px;text-align:center;font-size:11px;color:#999}
@media(max-width:800px){.stats,.item-stats{grid-template-columns:repeat(2,1fr)}.meta{grid-template-columns:1fr}}
@media print{.t-wrap{max-height:none;overflow:visible}body{font-size:11px}.page{padding:20px}}
</style>
</head>
<body>
<div class="page">
""")

        // Header
        onProgress?.invoke("Writing test parameters…", 0.20f)
        appendLine("<header>")
        appendLine("<h1>HSM Load Test Report</h1>")
        appendLine("<p>Generated $now &mdash; ${h(cfg.hsmVendor.displayName)} &mdash; ${h(cfg.ipAddress)}:${cfg.port}</p>")
        appendLine("</header>")

        // Connection & Configuration
        appendLine("<h2>Test Parameters</h2>")
        appendLine("<div class=\"meta\">")
        appendLine("<div class=\"meta-item\"><span class=\"k\">HSM Vendor</span><span class=\"v\">${h(cfg.hsmVendor.displayName)}</span></div>")
        appendLine("<div class=\"meta-item\"><span class=\"k\">Host</span><span class=\"v\">${h(cfg.ipAddress)}:${cfg.port}</span></div>")
        appendLine("<div class=\"meta-item\"><span class=\"k\">Header Format</span><span class=\"v\">${cfg.hsmVendor.headerFormat}</span></div>")
        appendLine("<div class=\"meta-item\"><span class=\"k\">Message Header</span><span class=\"v\">${h(cfg.headerValue.ifBlank { "\u2014" })}</span></div>")
        appendLine("<div class=\"meta-item\"><span class=\"k\">SSL/TLS</span><span class=\"v\">${if (cfg.sslConfig.enabled) "Enabled" else "Disabled"}</span></div>")
        appendLine("<div class=\"meta-item\"><span class=\"k\">TCP Length Header</span><span class=\"v\">${if (cfg.tcpLengthHeaderEnabled) "Yes" else "No"}</span></div>")
        appendLine("<div class=\"meta-item\"><span class=\"k\">Timeout</span><span class=\"v\">${cfg.timeout}s</span></div>")
        appendLine("<div class=\"meta-item\"><span class=\"k\">Concurrent Workers</span><span class=\"v\">$workers</span></div>")
        appendLine("<div class=\"meta-item\"><span class=\"k\">Target Cmd/sec</span><span class=\"v\">$targetTps</span></div>")
        appendLine("<div class=\"meta-item\"><span class=\"k\">Duration per Item</span><span class=\"v\">${durationSec}s</span></div>")
        appendLine("<div class=\"meta-item\"><span class=\"k\">Playlist Items</span><span class=\"v\">${playlist.size}</span></div>")
        appendLine("<div class=\"meta-item\"><span class=\"k\">Total Commands</span><span class=\"v\">${allCommands.size}</span></div>")
        appendLine("</div>")

        // Overall Summary
        onProgress?.invoke("Writing summary statistics…", 0.30f)
        appendLine("<h2>Summary</h2>")
        appendLine("<div class=\"stats\">")
        fun stat(n: String, l: String, s: String = "") {
            append("<div class=\"stat\"><div class=\"n\">$n</div><div class=\"l\">$l</div>")
            if (s.isNotEmpty()) append("<div class=\"s\">$s</div>")
            appendLine("</div>")
        }
        stat("$overallTotal", "Total Sent")
        stat("$overallSuccess", "Successful", "${"%.1f".format(overallSuccessRate)}%")
        stat("$overallFailure", "Failed", "${"%.1f".format(100.0 - overallSuccessRate)}%")
        stat("${"%.1f".format(overallTotal / overallDurationSec)}", "Throughput (tps)")
        appendLine("</div>")
        appendLine("<div class=\"stats\">")
        stat("${"%.2f".format(avgMs)} ms", "Avg Latency")
        stat("$minMs / $maxMs ms", "Min / Max Latency", "P95: $p95Ms ms")
        stat(bytes(totalBytesSent), "Data Sent", "$totalBytesSent bytes")
        stat(bytes(totalBytesRecv), "Data Received", "$totalBytesRecv bytes")
        appendLine("</div>")
        appendLine("<div class=\"meta\" style=\"grid-template-columns:repeat(4,1fr)\">")
        appendLine("<div class=\"meta-item\"><span class=\"k\">Start</span><span class=\"v\">${fullTs(overallStartTime)}</span></div>")
        appendLine("<div class=\"meta-item\"><span class=\"k\">End</span><span class=\"v\">${fullTs(overallEndTime)}</span></div>")
        appendLine("<div class=\"meta-item\"><span class=\"k\">Duration</span><span class=\"v\">${"%.1f".format(overallDurationSec)}s</span></div>")
        appendLine("<div class=\"meta-item\"><span class=\"k\">Executions</span><span class=\"v\">$totalCommands</span></div>")
        appendLine("</div>")

        // Playlist Overview
        onProgress?.invoke("Writing playlist overview (${playlist.size} items)…", 0.40f)
        appendLine("<h2>Playlist Overview</h2>")
        appendLine("<div class=\"t-wrap\"><table>")
        appendLine("<tr><th>#</th><th>Type</th><th>Label</th><th>Cmds</th><th>Sent</th><th>OK</th><th>Fail</th><th>Avg ms</th><th>TPS</th><th>Sent</th><th>Recv</th><th>Duration</th></tr>")
        for ((i, item) in playlist.withIndex()) {
            val ir = itemResults.getOrNull(i)
            val cmds = item.resolveCommands()
            val s = ir?.stats
            val logs = ir?.commandLogs ?: emptyList()
            val bSent = logs.sumOf { it.bytesSent.toLong() }
            val bRecv = logs.sumOf { it.bytesReceived.toLong() }
            val dur = if (ir != null) "%.1fs".format((ir.endTime - ir.startTime) / 1000.0) else "\u2014"
            appendLine("<tr>")
            appendLine("<td>${i + 1}</td><td>${h(item.type.label)}</td><td style=\"font-family:inherit\">${h(item.label)}</td><td>${cmds.size}</td>")
            appendLine("<td>${s?.totalSent ?: "\u2014"}</td>")
            appendLine("<td class=\"ok\">${s?.successCount ?: "\u2014"}</td>")
            appendLine("<td class=\"fail\">${s?.failureCount ?: "\u2014"}</td>")
            appendLine("<td>${if (s != null) "%.2f".format(s.avgResponseTimeMs) else "\u2014"}</td>")
            appendLine("<td>${if (s != null) "%.1f".format(s.currentTps) else "\u2014"}</td>")
            appendLine("<td>${bytes(bSent)}</td><td>${bytes(bRecv)}</td><td>$dur</td>")
            appendLine("</tr>")
        }
        appendLine("</table></div>")

        // Per-item Details
        onProgress?.invoke("Writing execution details…", detailSectionProgressStart)
        appendLine("<h2>Execution Details</h2>")
        for ((irIdx, ir) in itemResults.withIndex()) {
            val itemProgressFraction = if (itemResults.size > 1)
                irIdx.toFloat() / (itemResults.size - 1) else 1f
            val sectionProgress = detailSectionProgressStart +
                itemProgressFraction * (detailSectionProgressEnd - detailSectionProgressStart)
            onProgress?.invoke(
                "Item ${irIdx + 1}/${itemResults.size}: \"${ir.itemLabel}\" — ${ir.commandLogs.size} log entries…",
                sectionProgress,
            )
            val s = ir.stats
            val logs = ir.commandLogs
            val bSent = logs.sumOf { it.bytesSent.toLong() }
            val bRecv = logs.sumOf { it.bytesReceived.toLong() }
            val itemTotal = s.successCount + s.failureCount
            val rate = if (itemTotal > 0) s.successCount.toDouble() / itemTotal * 100 else 0.0

            appendLine("<div class=\"item-block\">")
            appendLine("<div class=\"item-hdr\">")
            appendLine("<span class=\"idx\">${ir.itemIndex + 1}</span>")
            appendLine("<span class=\"lbl\">${h(ir.itemLabel)}</span>")
            appendLine("<span class=\"info\">${h(ir.itemType)} &middot; ${ir.commands.size} cmd &middot; ${fullTs(ir.startTime)} \u2014 ${fullTs(ir.endTime)}</span>")
            appendLine("</div>")
            appendLine("<div class=\"item-body\">")

            // Item stats row
            appendLine("<div class=\"item-stats\">")
            stat("${s.totalSent}", "Sent")
            stat("${s.successCount}", "OK", "${"%.1f".format(rate)}%")
            stat("${s.failureCount}", "Fail")
            stat("${"%.2f".format(s.avgResponseTimeMs)} ms", "Avg Latency", "${if (s.minResponseTimeMs == Long.MAX_VALUE) "\u2014" else "${s.minResponseTimeMs}"} / ${if (s.maxResponseTimeMs == 0L) "\u2014" else "${s.maxResponseTimeMs}"} ms")
            stat("${"%.1f".format(s.currentTps)}", "TPS")
            stat("${bytes(bSent)} / ${bytes(bRecv)}", "Data S/R")
            appendLine("</div>")

            // Commands
            if (ir.commands.size <= 5) {
                appendLine("<details open><summary>Commands (${ir.commands.size})</summary>")
            } else {
                appendLine("<details><summary>Commands (${ir.commands.size})</summary>")
            }
            appendLine("<table><tr><th>#</th><th>Command</th></tr>")
            for ((ci, cmd) in ir.commands.withIndex()) {
                appendLine("<tr><td>${ci + 1}</td><td>${h(cmd)}</td></tr>")
            }
            appendLine("</table></details>")

            // Execution log
            appendLine("<details open><summary>Execution Log (${logs.size} entries)</summary>")
            appendLine("<div class=\"t-wrap\"><table>")
            appendLine("<tr><th>#</th><th>Time</th><th>W</th><th>Command</th><th>Response</th><th>Status</th><th>ms</th><th>Sent</th><th>Recv</th><th>Error</th></tr>")
            for ((li, log) in logs.withIndex()) {
                appendLine("<tr>")
                appendLine("<td>${li + 1}</td>")
                appendLine("<td>${ts(log.timestamp)}</td>")
                appendLine("<td>${log.workerIndex}</td>")
                appendLine("<td title=\"${h(log.command)}\">${h(log.command)}</td>")
                appendLine("<td title=\"${h(log.response)}\">${h(log.response)}</td>")
                appendLine("<td class=\"${if (log.success) "ok" else "fail"}\">${if (log.success) "OK" else "FAIL"}</td>")
                appendLine("<td>${log.elapsedMs}</td>")
                appendLine("<td>${log.bytesSent}</td>")
                appendLine("<td>${log.bytesReceived}</td>")
                appendLine("<td style=\"font-family:inherit;color:#999\">${h(log.errorMessage ?: "")}</td>")
                appendLine("</tr>")
            }
            appendLine("</table></div></details>")
            appendLine("</div></div>") // item-body, item-block
        }

        // Footer
        onProgress?.invoke("Finalising HTML (${allLogs.size} log entries, ${itemResults.size} items)…", 0.92f)
        appendLine("<footer>ISO8583Studio &mdash; HSM Load Test Report &mdash; $now</footer>")
        appendLine("</div></body></html>")
    }
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
