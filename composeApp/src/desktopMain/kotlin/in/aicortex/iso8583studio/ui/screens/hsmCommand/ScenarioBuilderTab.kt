package `in`.aicortex.iso8583studio.ui.screens.hsmCommand

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import `in`.aicortex.iso8583studio.domain.service.hsmCommandService.ConnectionState
import `in`.aicortex.iso8583studio.domain.service.hsmCommandService.HsmCommandClientService
import `in`.aicortex.iso8583studio.ui.PrimaryBlue
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.HeaderFormat
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.HsmCommandConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.SavedScenario
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.SavedScenarioStep
import kotlinx.coroutines.launch

private val Mono = FontFamily.Monospace
private val AccentGreen = Color(0xFF4CAF50)
private val AccentRed = Color(0xFFF44336)
private val AccentOrange = Color(0xFFFF9800)

// ──────────────────────────────────────────────────────────────────────────────
//  Short codes for response fields — used in reference syntax [CMD][CODE]
// ──────────────────────────────────────────────────────────────────────────────

/** Generate a short uppercase code for a response field, e.g. "errorCode" -> "ERRCODE", "key" -> "KEY". */
private fun generateFieldCode(field: ThalesCommandField, index: Int): String {
    val id = field.id
    return when {
        id.equals("errorCode", true) -> "ERRCODE"
        id.contains("key", true) && id.contains("check", true) -> "KCV"
        id.contains("kcv", true) -> "KCV"
        id.contains("key", true) -> "KEY${if (index > 1) "$index" else ""}"
        id.contains("iv", true) -> "IV"
        id.contains("mac", true) -> "MAC"
        id.contains("pin", true) -> "PIN"
        id.contains("data", true) -> "DATA"
        id.contains("length", true) || id.contains("len", true) -> "LEN"
        id.contains("scheme", true) -> "SCHEME"
        else -> id.uppercase().take(8)
    }
}

/** Build a map of response field codes for a definition. */
fun buildResponseFieldCodes(def: ThalesCommandDefinition): Map<String, String> {
    val codes = mutableMapOf<String, String>()
    val usedCodes = mutableSetOf<String>()
    for ((i, field) in def.responseFields.withIndex()) {
        var code = generateFieldCode(field, i)
        var attempt = 1
        while (code in usedCodes) {
            attempt++
            code = "${generateFieldCode(field, i)}$attempt"
        }
        usedCodes.add(code)
        codes[field.id] = code
    }
    return codes
}

// ──────────────────────────────────────────────────────────────────────────────
//  Data model
// ──────────────────────────────────────────────────────────────────────────────

/** Reference pattern: [STEP_NUM][CMD_CODE][FIELD_CODE] e.g. [1][A0][KEY] */
private val REF_PATTERN = Regex("""\[(\d+)]\[([A-Z0-9]{2,4})]\[([A-Z0-9_]+)]""")

/** A single step in the scenario. */
class ScenarioStep(
    val definition: ThalesCommandDefinition,
    val fieldValues: SnapshotStateMap<String, String> = mutableStateMapOf(),
)

/** Result of executing one step. */
data class StepExecutionResult(
    val stepIndex: Int,
    val success: Boolean,
    val parsedResponse: List<Pair<ThalesCommandField, String>>,
    val responseValueMap: Map<String, String>,
    val rawRequest: String,
    val rawResponse: String,
    val errorMessage: String? = null,
    val elapsedMs: Long = 0,
)

// ──────────────────────────────────────────────────────────────────────────────
//  Session state
// ──────────────────────────────────────────────────────────────────────────────

class ScenarioSessionState {
    val steps = mutableStateListOf<ScenarioStep>()
    var isRunning by mutableStateOf(false)
    val results = mutableStateListOf<StepExecutionResult>()
    var selectedStepIndex by mutableStateOf(-1)

    /** Currently loaded scenario id (null = unsaved new scenario). */
    var currentScenarioId by mutableStateOf<String?>(null)
    /** Name of the current scenario being edited. */
    var currentScenarioName by mutableStateOf("New Scenario")

    /** Convert current session state into a SavedScenario. */
    fun toSavedScenario(): SavedScenario {
        val id = currentScenarioId ?: java.util.UUID.randomUUID().toString()
        return SavedScenario(
            id = id,
            name = currentScenarioName,
            steps = steps.map { step ->
                SavedScenarioStep(
                    commandCode = step.definition.code,
                    fieldValues = step.fieldValues.toMap(),
                )
            },
            createdDate = System.currentTimeMillis(),
            modifiedDate = System.currentTimeMillis(),
        )
    }

    /** Load a saved scenario into this session. */
    fun loadScenario(saved: SavedScenario, definitions: List<ThalesCommandDefinition>) {
        steps.clear()
        results.clear()
        selectedStepIndex = -1
        currentScenarioId = saved.id
        currentScenarioName = saved.name
        val defsByCode = definitions.associateBy { it.code }
        for (savedStep in saved.steps) {
            val def = defsByCode[savedStep.commandCode] ?: continue
            steps.add(ScenarioStep(
                definition = def,
                fieldValues = mutableStateMapOf<String, String>().apply { putAll(savedStep.fieldValues) },
            ))
        }
        if (steps.isNotEmpty()) selectedStepIndex = 0
    }

    /** Clear session to start a fresh new scenario. */
    fun newScenario() {
        steps.clear()
        results.clear()
        selectedStepIndex = -1
        currentScenarioId = null
        currentScenarioName = "New Scenario"
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  Main composable
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun ScenarioBuilderTab(
    service: HsmCommandClientService,
    session: ScenarioSessionState,
    savedScenarios: List<SavedScenario> = emptyList(),
    onSaveScenario: (SavedScenario) -> Unit = {},
    onDeleteScenario: (String) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val connectionState by service.connectionState.collectAsState()
    val definitions = remember { thalesCommandDefinitions }

    var showCommandPicker by remember { mutableStateOf(false) }
    var showScenarioManager by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    val runScenario: () -> Unit = {
        session.isRunning = true
        session.results.clear()
        scope.launch {
            try {
                executeScenario(service, session)
            } catch (_: Exception) { }
            session.isRunning = false
        }
    }

    val saveCurrentScenario: () -> Unit = {
        if (session.steps.isNotEmpty()) {
            onSaveScenario(session.toSavedScenario())
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        // ═══════════════════════════════════════════════════
        //  TOP BAR — Scenario Manager
        // ═══════════════════════════════════════════════════
        ScenarioManagerBar(
            session = session,
            savedScenarios = savedScenarios,
            showManager = showScenarioManager,
            onToggleManager = { showScenarioManager = !showScenarioManager },
            onNewScenario = { session.newScenario() },
            onLoadScenario = { saved ->
                session.loadScenario(saved, definitions)
                showScenarioManager = false
            },
            onSave = saveCurrentScenario,
            onRename = { showRenameDialog = true },
            onDelete = { id ->
                onDeleteScenario(id)
                if (session.currentScenarioId == id) session.newScenario()
            },
        )

        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
        // ═══════════════════════════════════════════════════
        //  LEFT PANEL — Flow Graph
        // ═══════════════════════════════════════════════════
        Surface(
            modifier = Modifier.width(320.dp).fillMaxHeight(),
            color = MaterialTheme.colors.surface,
            elevation = 2.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(modifier = Modifier.fillMaxWidth(), color = PrimaryBlue.copy(alpha = 0.06f)) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.AccountTree, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Scenario Flow", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            Text("${session.steps.size} step(s)", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                        }
                    }
                }

                // Flow graph
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    itemsIndexed(session.steps) { index, step ->
                        val result = session.results.getOrNull(index)
                        val isSelected = session.selectedStepIndex == index
                        // Connections: show which fields flow into this step
                        val incomingRefs = collectReferences(step, session.steps, index)

                        // Connector line from previous step
                        if (index > 0) {
                            FlowConnector(
                                mappings = incomingRefs,
                                result = session.results.getOrNull(index - 1),
                            )
                        }

                        FlowStepNode(
                            index = index,
                            step = step,
                            result = result,
                            isSelected = isSelected,
                            onClick = { session.selectedStepIndex = index },
                            onRemove = {
                                session.steps.removeAt(index)
                                session.results.clear()
                                if (session.selectedStepIndex >= session.steps.size) {
                                    session.selectedStepIndex = session.steps.lastIndex
                                }
                            },
                            onMoveUp = {
                                if (index > 0) {
                                    val item = session.steps.removeAt(index)
                                    session.steps.add(index - 1, item)
                                    session.selectedStepIndex = index - 1
                                    session.results.clear()
                                }
                            },
                            onMoveDown = {
                                if (index < session.steps.lastIndex) {
                                    val item = session.steps.removeAt(index)
                                    session.steps.add(index + 1, item)
                                    session.selectedStepIndex = index + 1
                                    session.results.clear()
                                }
                            },
                        )
                    }

                    item {
                        if (session.steps.isNotEmpty()) {
                            // Connector to add button
                            Box(modifier = Modifier.fillMaxWidth().height(20.dp)) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawLine(
                                        color = Color.Gray.copy(alpha = 0.3f),
                                        start = Offset(size.width / 2, 0f),
                                        end = Offset(size.width / 2, size.height),
                                        strokeWidth = 2f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)),
                                    )
                                }
                            }
                        }
                        // Add step button
                        OutlinedButton(
                            onClick = { showCommandPicker = true },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue),
                            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f)),
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add Command Step", fontSize = 12.sp)
                        }
                    }
                }

                // Run controls
                Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = runScenario,
                        enabled = session.steps.isNotEmpty() && !session.isRunning && connectionState == ConnectionState.CONNECTED,
                        colors = ButtonDefaults.buttonColors(backgroundColor = AccentGreen),
                        modifier = Modifier.weight(1f).height(36.dp),
                        elevation = ButtonDefaults.elevation(0.dp),
                    ) {
                        if (session.isRunning) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp), tint = Color.White)
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(if (session.isRunning) "Running..." else "Run Scenario", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = { session.steps.clear(); session.results.clear(); session.selectedStepIndex = -1 },
                        enabled = !session.isRunning && session.steps.isNotEmpty(),
                        modifier = Modifier.height(36.dp),
                    ) {
                        Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════
        //  RIGHT PANEL — Field Editor / Results
        // ═══════════════════════════════════════════════════
        if (session.selectedStepIndex in session.steps.indices) {
            val stepIndex = session.selectedStepIndex
            val step = session.steps[stepIndex]
            StepFieldEditor(
                stepIndex = stepIndex,
                step = step,
                allSteps = session.steps,
                result = session.results.getOrNull(stepIndex),
                service = service,
            )
        } else {
            // Placeholder
            Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AccountTree, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
                    Spacer(Modifier.height(12.dp))
                    Text("Build your test scenario", style = MaterialTheme.typography.h6, color = MaterialTheme.colors.onSurface.copy(alpha = 0.25f))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Add command steps, link output fields using\n[STEP][CMD][CODE] syntax, then run the scenario.",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.04f),
                    ) {
                        Column(modifier = Modifier.padding(16.dp).width(360.dp)) {
                            Text("Reference Syntax", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryBlue)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "To use output from a previous step as input:\n" +
                                    "  [1][A0][KEY]   \u2190 Key from step 1 (A0)\n" +
                                    "  [1][A0][KCV]   \u2190 KCV from step 1 (A0)\n" +
                                    "  [2][M0][DATA]  \u2190 Data from step 2 (M0)\n\n" +
                                    "Autocomplete hints appear when you type [",
                                fontSize = 11.sp,
                                fontFamily = Mono,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }
        }
        } // Row
    } // Column (outer)

    // Command picker dialog
    if (showCommandPicker) {
        CommandPickerDialog(
            definitions = definitions,
            onSelect = { def ->
                val step = ScenarioStep(
                    definition = def,
                    fieldValues = mutableStateMapOf<String, String>().apply {
                        for (f in def.requestFields) {
                            if (f.defaultValue.isNotEmpty()) put(f.id, f.defaultValue)
                        }
                    },
                )
                session.steps.add(step)
                session.selectedStepIndex = session.steps.lastIndex
                session.results.clear()
                showCommandPicker = false
            },
            onDismiss = { showCommandPicker = false },
        )
    }

    // Rename scenario dialog
    if (showRenameDialog) {
        RenameScenarioDialog(
            currentName = session.currentScenarioName,
            onConfirm = { newName ->
                session.currentScenarioName = newName
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false },
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  Scenario manager bar & dialogs
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun ScenarioManagerBar(
    session: ScenarioSessionState,
    savedScenarios: List<SavedScenario>,
    showManager: Boolean,
    onToggleManager: () -> Unit,
    onNewScenario: () -> Unit,
    onLoadScenario: (SavedScenario) -> Unit,
    onSave: () -> Unit,
    onRename: () -> Unit,
    onDelete: (String) -> Unit,
) {
    var showDeleteConfirmId by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colors.surface,
        elevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {


            // Unsaved indicator
            if (session.steps.isNotEmpty()) {
                val isSaved = session.currentScenarioId != null &&
                    savedScenarios.any { it.id == session.currentScenarioId }
                if (!isSaved) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = AccentOrange.copy(alpha = 0.1f),
                    ) {
                        Text(
                            "Unsaved",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentOrange,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Rename button
            IconButton(onClick = onRename, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Edit, "Rename", modifier = Modifier.size(14.dp), tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
            }

            // Save button
            IconButton(
                onClick = onSave,
                modifier = Modifier.size(28.dp),
                enabled = session.steps.isNotEmpty(),
            ) {
                Icon(Icons.Default.Save, "Save", modifier = Modifier.size(15.dp), tint = PrimaryBlue)
            }

            // New scenario
            IconButton(onClick = onNewScenario, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.NoteAdd, "New", modifier = Modifier.size(15.dp), tint = AccentGreen)
            }

            // ── Scenario dropdown selector ──
            Box {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.04f),
                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.1f)),
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onToggleManager),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp).widthIn(max = 180.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Default.AccountTree, null, modifier = Modifier.size(15.dp), tint = PrimaryBlue)
                        Column(modifier = Modifier) {
                            Text(
                                session.currentScenarioName,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (session.steps.isNotEmpty()) {
                                Text(
                                    "${session.steps.size} steps",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                                )
                            }
                        }
                        Icon(
                            if (showManager) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                        )
                    }
                }

                // Dropdown menu
                DropdownMenu(
                    expanded = showManager,
                    onDismissRequest = onToggleManager,
                    modifier = Modifier.widthIn(min = 280.dp),
                ) {
                    // New scenario option
//                    DropdownMenuItem(onClick = {
//                        onNewScenario()
//                        onToggleManager()
//                    }) {
//                        Icon(Icons.Default.NoteAdd, null, modifier = Modifier.size(16.dp), tint = AccentGreen)
//                        Spacer(Modifier.width(8.dp))
//                        Text("New Scenario", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = AccentGreen)
//                    }

                    if (savedScenarios.isNotEmpty()) {
                        Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colors.onSurface.copy(alpha = 0.06f))

                        Text(
                            "SAVED SCENARIOS",
                            style = MaterialTheme.typography.overline,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.35f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )

                        for (scenario in savedScenarios) {
                            val isActive = scenario.id == session.currentScenarioId
                            val isDeleting = showDeleteConfirmId == scenario.id

                            DropdownMenuItem(
                                onClick = {
                                    if (!isDeleting) {
                                        onLoadScenario(scenario)
                                        onToggleManager()
                                    }
                                },
                                modifier = if (isActive) Modifier.background(PrimaryBlue.copy(alpha = 0.06f)) else Modifier,
                            ) {
                                if (isDeleting) {
                                    // Delete confirmation inline
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text("Delete \"${scenario.name}\"?", fontSize = 11.sp, color = AccentRed, modifier = Modifier.weight(1f))
                                        Button(
                                            onClick = {
                                                onDelete(scenario.id)
                                                showDeleteConfirmId = null
                                            },
                                            colors = ButtonDefaults.buttonColors(backgroundColor = AccentRed),
                                            modifier = Modifier.height(24.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp),
                                            elevation = ButtonDefaults.elevation(0.dp),
                                        ) {
                                            Text("Yes", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = { showDeleteConfirmId = null },
                                            modifier = Modifier.height(24.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp),
                                        ) {
                                            Text("No", fontSize = 10.sp)
                                        }
                                    }
                                } else {
                                    // Normal scenario item
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.AccountTree, null,
                                            modifier = Modifier.size(14.dp),
                                            tint = if (isActive) PrimaryBlue else MaterialTheme.colors.onSurface.copy(alpha = 0.35f),
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                scenario.name,
                                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 12.sp,
                                                color = if (isActive) PrimaryBlue else MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    "${scenario.steps.size} steps",
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.35f),
                                                )
                                                if (scenario.steps.isNotEmpty()) {
                                                    Text(
                                                        "\u2022 ${scenario.steps.joinToString(" \u2192 ") { it.commandCode }}",
                                                        fontSize = 9.sp,
                                                        fontFamily = Mono,
                                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }
                                            }
                                        }
                                        if (isActive) {
                                            Surface(
                                                shape = RoundedCornerShape(3.dp),
                                                color = PrimaryBlue.copy(alpha = 0.1f),
                                            ) {
                                                Text(
                                                    "Active",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PrimaryBlue,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                )
                                            }
                                        }
                                        Icon(
                                            Icons.Default.Delete, "Delete",
                                            modifier = Modifier.size(14.dp).clip(CircleShape).clickable { showDeleteConfirmId = scenario.id },
                                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.25f),
                                        )
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

@Composable
private fun RenameScenarioDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Scenario", fontWeight = FontWeight.Bold) },
        text = {
            FixedOutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Scenario name") },
                leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp)) },
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBlue),
            ) {
                Text("Rename", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

// ──────────────────────────────────────────────────────────────────────────────
//  Flow graph components
// ──────────────────────────────────────────────────────────────────────────────

/** Build a preview of the wire command, showing references in braces. */
private fun buildWirePreview(step: ScenarioStep): String {
    val def = step.definition
    val sb = StringBuilder(def.code)
    for (field in def.requestFields) {
        if (!ThalesWireBuilder.isFieldVisible(field, step.fieldValues)) continue
        val raw = step.fieldValues[field.id].orEmpty()
        if (raw.isBlank() && field.requirement == FieldRequirement.OPTIONAL) continue
        if (raw.isBlank() && field.omitFromWireWhenBlank) continue
        if (REF_PATTERN.containsMatchIn(raw)) sb.append("\u00AB$raw\u00BB") else sb.append(raw)
    }
    return sb.toString()
}

/** Connector line between nodes with animated gradient feel and data flow labels. */
@Composable
private fun FlowConnector(
    mappings: List<String>,
    result: StepExecutionResult?,
) {
    val lineColor = when {
        result?.success == true -> AccentGreen
        result?.success == false -> AccentRed
        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
    }

    val connectorHeight = if (mappings.isEmpty()) 24.dp else (28 + mappings.size * 13).dp

    Row(
        modifier = Modifier.fillMaxWidth().height(connectorHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left spacer to align with step number circle center (26dp badge, 12dp pad = ~25dp center)
        Spacer(Modifier.width(24.dp))

        // Vertical line column
        Box(modifier = Modifier.width(2.dp).fillMaxHeight()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Gradient-like line with rounded dots at ends
                drawLine(
                    color = lineColor.copy(alpha = 0.6f),
                    start = Offset(size.width / 2, 0f),
                    end = Offset(size.width / 2, size.height),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round,
                )
                // Arrow head
                val cx = size.width / 2
                val ay = size.height
                val as2 = 5f
                drawLine(lineColor, Offset(cx - as2, ay - as2 * 2), Offset(cx, ay), strokeWidth = 2.5f, cap = StrokeCap.Round)
                drawLine(lineColor, Offset(cx + as2, ay - as2 * 2), Offset(cx, ay), strokeWidth = 2.5f, cap = StrokeCap.Round)
            }
        }

        // Data flow labels
        if (mappings.isNotEmpty()) {
            Spacer(Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                for (label in mappings.take(4)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Surface(
                            modifier = Modifier.size(4.dp),
                            shape = CircleShape,
                            color = AccentOrange.copy(alpha = 0.5f),
                        ) {}
                        Text(
                            label,
                            fontSize = 8.sp,
                            fontFamily = Mono,
                            color = AccentOrange.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (mappings.size > 4) {
                    Text("  +${mappings.size - 4} more", fontSize = 8.sp, color = AccentOrange.copy(alpha = 0.4f))
                }
            }
        }
    }
}

/** A step node in the flow graph. */
@Composable
private fun FlowStepNode(
    index: Int,
    step: ScenarioStep,
    result: StepExecutionResult?,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val statusColor = when {
        result?.success == true -> AccentGreen
        result?.success == false -> AccentRed
        else -> null
    }
    val borderColor = when {
        isSelected -> PrimaryBlue
        statusColor != null -> statusColor
        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = if (isSelected) 4.dp else 1.dp,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        backgroundColor = MaterialTheme.colors.surface,
    ) {
        Column {
            // Colored top accent bar
            if (statusColor != null) {
                Surface(modifier = Modifier.fillMaxWidth().height(3.dp), color = statusColor) {}
            }

            Column(modifier = Modifier.padding(10.dp)) {
                // Header row: step badge + command info + result
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Step number circle
                    Surface(
                        modifier = Modifier.size(30.dp),
                        shape = CircleShape,
                        color = if (isSelected) PrimaryBlue else if (statusColor != null) statusColor.copy(alpha = 0.15f) else MaterialTheme.colors.onSurface.copy(alpha = 0.08f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (statusColor != null && !isSelected) {
                                Icon(
                                    if (result?.success == true) Icons.Default.Check else Icons.Default.Close,
                                    null, modifier = Modifier.size(14.dp),
                                    tint = statusColor,
                                )
                            } else {
                                Text(
                                    "${index + 1}",
                                    color = if (isSelected) Color.White else MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(shape = RoundedCornerShape(4.dp), color = PrimaryBlue.copy(alpha = 0.1f)) {
                                Text(
                                    step.definition.code,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryBlue,
                                )
                            }
                            Text(
                                step.definition.name,
                                fontSize = 11.sp, fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    // Timing badge
                    if (result != null) {
                        Text(
                            "${result.elapsedMs}ms",
                            fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = Mono,
                            color = (statusColor ?: MaterialTheme.colors.onSurface).copy(alpha = 0.7f),
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Wire command preview
                val wirePreview = remember(step.fieldValues.toMap(), step.definition) {
                    try { buildWirePreview(step) } catch (_: Exception) { step.definition.code }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.04f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Default.Code, null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colors.onSurface.copy(alpha = 0.25f))
                        Text(
                            wirePreview,
                            fontSize = 9.sp,
                            fontFamily = Mono,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.45f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Info pills row: references + error
                val refCount = step.fieldValues.values.count { REF_PATTERN.containsMatchIn(it) }
                if (refCount > 0 || (result?.success == false)) {
                    Spacer(Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (refCount > 0) {
                            Surface(shape = RoundedCornerShape(10.dp), color = AccentOrange.copy(alpha = 0.1f)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Icon(Icons.Default.Link, null, modifier = Modifier.size(9.dp), tint = AccentOrange)
                                    Text("$refCount ref", fontSize = 8.sp, color = AccentOrange, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        if (result?.success == false && result.errorMessage != null) {
                            Surface(shape = RoundedCornerShape(10.dp), color = AccentRed.copy(alpha = 0.1f)) {
                                Text(
                                    result.errorMessage.take(30),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    fontSize = 8.sp, color = AccentRed, fontWeight = FontWeight.Medium,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                // Action buttons — compact, at bottom
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Move buttons
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.04f),
                    ) {
                        Row {
                            IconButton(onClick = onMoveUp, modifier = Modifier.size(22.dp)) {
                                Icon(Icons.Default.KeyboardArrowUp, "Move up", modifier = Modifier.size(13.dp), tint = MaterialTheme.colors.onSurface.copy(alpha = 0.35f))
                            }
                            IconButton(onClick = onMoveDown, modifier = Modifier.size(22.dp)) {
                                Icon(Icons.Default.KeyboardArrowDown, "Move down", modifier = Modifier.size(13.dp), tint = MaterialTheme.colors.onSurface.copy(alpha = 0.35f))
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onRemove, modifier = Modifier.size(22.dp)) {
                        Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(13.dp), tint = AccentRed.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  Right panel: Step field editor (reuses Console-style field inputs)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun StepFieldEditor(
    stepIndex: Int,
    step: ScenarioStep,
    allSteps: List<ScenarioStep>,
    result: StepExecutionResult?,
    service: HsmCommandClientService,
) {
    val def = step.definition
    val fieldValues = step.fieldValues

    // Build available reference codes from all prior steps
    val availableRefs = remember(allSteps.size, stepIndex) {
        buildAvailableReferences(allSteps, stepIndex)
    }

    // Validate references in current field values
    val validationErrors = remember(fieldValues.toMap(), allSteps.size, stepIndex) {
        validateReferences(fieldValues, allSteps, stepIndex)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header bar
        Surface(color = MaterialTheme.colors.surface, elevation = 1.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = RoundedCornerShape(4.dp), color = PrimaryBlue.copy(alpha = 0.12f)) {
                        Text(
                            "Step ${stepIndex + 1}: ${def.code} / ${def.responseCode}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryBlue,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(def.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (def.description.isNotBlank()) {
                            Text(def.description, fontSize = 11.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                // Validation warnings
                if (validationErrors.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Surface(shape = RoundedCornerShape(6.dp), color = AccentRed.copy(alpha = 0.08f)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            for (err in validationErrors.take(3)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Warning, null, modifier = Modifier.size(12.dp), tint = AccentRed)
                                    Text(err, fontSize = 10.sp, color = AccentRed)
                                }
                            }
                        }
                    }
                }
            }
        }

        Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))

        // Scrollable field editor + reference help
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier.weight(1f).verticalScroll(scrollState).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Available references helper
            if (stepIndex > 0) {
                Surface(shape = RoundedCornerShape(8.dp), color = AccentOrange.copy(alpha = 0.06f)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Link, null, modifier = Modifier.size(14.dp), tint = AccentOrange)
                            Text("Available References", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentOrange)
                        }
                        Spacer(Modifier.height(4.dp))
                        for (i in 0 until stepIndex) {
                            val priorStep = allSteps[i]
                            val codes = buildResponseFieldCodes(priorStep.definition)
                            Row(modifier = Modifier.padding(vertical = 1.dp)) {
                                Text("[${i + 1}][${priorStep.definition.code}]  ", fontFamily = Mono, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                Text(
                                    codes.values.joinToString("  ") { "[$it]" },
                                    fontFamily = Mono, fontSize = 10.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Type [STEP][CMD][CODE] in any field. e.g. [1][A0][KEY]",
                            fontSize = 9.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.35f),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // Render fields exactly like Console
            val snapshot = fieldValues.toMap()
            val isM0 = def.code == "M0"
            val isM2 = def.code == "M2"
            val isM4 = def.code == "M4"
            val visibleFields = def.requestFields.filter {
                ThalesWireBuilder.isFieldVisible(it, snapshot) &&
                    !(isM0 && it.id == "msgLength") &&
                    !(isM2 && it.id == "encryptedMessageLength") &&
                    !(isM4 && it.id == "msgLength")
            }
            ScenarioFieldGrid(visibleFields, fieldValues, def.forceVerticalFieldLayout, availableRefs, allSteps, stepIndex)
        }

        // Wire preview
        Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.06f))
        Surface(color = MaterialTheme.colors.surface.copy(alpha = 0.6f)) {
            val wirePreview = remember(fieldValues.toMap(), def) {
                try {
                    // Show raw values with references unresolved for preview
                    val preview = StringBuilder(def.code)
                    for (field in def.requestFields) {
                        if (!ThalesWireBuilder.isFieldVisible(field, fieldValues)) continue
                        val raw = fieldValues[field.id].orEmpty()
                        if (raw.isBlank() && field.requirement == FieldRequirement.OPTIONAL) continue
                        if (REF_PATTERN.containsMatchIn(raw)) {
                            preview.append("{${raw}}")
                        } else {
                            preview.append(raw)
                        }
                    }
                    preview.toString()
                } catch (_: Exception) { "..." }
            }
            Text(
                wirePreview.ifEmpty { "..." },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                fontFamily = Mono, fontSize = 10.sp, maxLines = 2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.35f),
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Response section (if result available)
        if (result != null) {
            Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
            StepResultPanel(result, step.definition)
        }
    }
}

/** Render fields like Console's FieldGrid, but with reference-aware text fields highlighting [CMD][CODE]. */
@Composable
private fun ScenarioFieldGrid(
    visibleFields: List<ThalesCommandField>,
    fieldValues: MutableMap<String, String>,
    forceVerticalFieldLayout: Boolean = false,
    availableRefs: Map<String, Map<String, String>>,
    allSteps: List<ScenarioStep>,
    currentStepIndex: Int,
) {
    var i = 0
    while (i < visibleFields.size) {
        val field = visibleFields[i]
        val isFlag = field.type == FieldType.FLAG
        val isDropdown = !field.options.isNullOrEmpty()

        if (isFlag) {
            CompactFlagField(field, fieldValues[field.id].orEmpty(), { fieldValues[field.id] = it }, Modifier.fillMaxWidth())
            i++
            continue
        }

        if (!forceVerticalFieldLayout && isDropdown && i + 1 < visibleFields.size) {
            val next = visibleFields[i + 1]
            if (!next.options.isNullOrEmpty()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactDropdown(field, fieldValues[field.id].orEmpty(), { fieldValues[field.id] = it }, Modifier.weight(1f))
                    CompactDropdown(next, fieldValues[next.id].orEmpty(), { fieldValues[next.id] = it }, Modifier.weight(1f))
                }
                i += 2
                continue
            }
        }

        if (isDropdown) {
            CompactDropdown(field, fieldValues[field.id].orEmpty(), { fieldValues[field.id] = it }, Modifier.fillMaxWidth())
        } else {
            ScenarioTextField(
                field = field,
                value = fieldValues[field.id].orEmpty(),
                onValueChange = { fieldValues[field.id] = it },
                availableRefs = availableRefs,
                modifier = Modifier.fillMaxWidth(),
                allSteps = allSteps,
                currentStepIndex = currentStepIndex,
            )
        }
        i++
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  Autocomplete data structures
// ──────────────────────────────────────────────────────────────────────────────

/** A single autocomplete suggestion. */
private data class RefSuggestion(
    val insertText: String,   // e.g. "[1][A0][KEY]"
    val stepNum: Int,         // 1-based step number
    val cmdCode: String,
    val fieldCode: String,
    val fieldName: String,
)

/** Build a flat list of all possible [STEP][CMD][CODE] suggestions. */
private fun buildSuggestionList(
    allSteps: List<ScenarioStep>,
    currentIndex: Int,
): List<RefSuggestion> {
    val suggestions = mutableListOf<RefSuggestion>()
    for (i in 0 until currentIndex.coerceAtMost(allSteps.size)) {
        val step = allSteps[i]
        val codes = buildResponseFieldCodes(step.definition)
        for (respField in step.definition.responseFields) {
            val code = codes[respField.id] ?: continue
            val stepNum = i + 1
            suggestions.add(
                RefSuggestion(
                    insertText = "[$stepNum][${step.definition.code}][$code]",
                    stepNum = stepNum,
                    cmdCode = step.definition.code,
                    fieldCode = code,
                    fieldName = respField.name,
                )
            )
        }
    }
    return suggestions
}

/**
 * Detect what the user is currently typing for autocomplete:
 *  - After `[` → show step-level hints (filter by step number digits)
 *  - After `[1][` → show cmd-level hints for step 1
 *  - After `[1][A0][` → show field-level hints for step 1, cmd A0
 *  - Otherwise null (no autocomplete active)
 */
private enum class AutocompletePhase { STEP, CMD, FIELD }

private data class AutocompleteContext(
    val phase: AutocompletePhase,
    val partialStep: String,
    val partialCmd: String,
    val partialField: String,
    val replaceStart: Int,
)

private fun detectAutocompleteContext(text: String, cursorPos: Int): AutocompleteContext? {
    if (cursorPos <= 0 || cursorPos > text.length) return null
    val before = text.substring(0, cursorPos)

    // Check for [STEP][CMD][ pattern → field phase
    val fieldMatch = Regex("""\[(\d+)]\[([A-Z0-9]{2,4})]\[([A-Z0-9_]*)$""").find(before)
    if (fieldMatch != null) {
        return AutocompleteContext(
            phase = AutocompletePhase.FIELD,
            partialStep = fieldMatch.groupValues[1],
            partialCmd = fieldMatch.groupValues[2],
            partialField = fieldMatch.groupValues[3],
            replaceStart = fieldMatch.range.first,
        )
    }

    // Check for [STEP][ pattern → cmd phase
    val cmdMatch = Regex("""\[(\d+)]\[([A-Z0-9]{0,4})$""").find(before)
    if (cmdMatch != null) {
        return AutocompleteContext(
            phase = AutocompletePhase.CMD,
            partialStep = cmdMatch.groupValues[1],
            partialCmd = cmdMatch.groupValues[2],
            partialField = "",
            replaceStart = cmdMatch.range.first,
        )
    }

    // Check for [ pattern → step phase
    val stepMatch = Regex("""\[(\d*)$""").find(before)
    if (stepMatch != null) {
        return AutocompleteContext(
            phase = AutocompletePhase.STEP,
            partialStep = stepMatch.groupValues[1],
            partialCmd = "",
            partialField = "",
            replaceStart = stepMatch.range.first,
        )
    }

    return null
}

/** Text field with inline autocomplete popup for [STEP][CMD][CODE] references. */
@Composable
private fun ScenarioTextField(
    field: ThalesCommandField,
    value: String,
    onValueChange: (String) -> Unit,
    availableRefs: Map<String, Map<String, String>>,
    modifier: Modifier = Modifier,
    allSteps: List<ScenarioStep> = emptyList(),
    currentStepIndex: Int = 0,
) {
    val isOpt = field.requirement == FieldRequirement.OPTIONAL
    val label = field.name + if (isOpt) " (opt)" else ""
    val hint = if (field.length > 0) "${field.length} chars" else "variable"
    val hasRef = REF_PATTERN.containsMatchIn(value)
    val allSuggestions = remember(allSteps.size, currentStepIndex) {
        buildSuggestionList(allSteps, currentStepIndex)
    }
    val refValid = if (hasRef) {
        REF_PATTERN.findAll(value).all { match ->
            val stepNum = match.groupValues[1].toIntOrNull() ?: return@all false
            val cmd = match.groupValues[2]
            val code = match.groupValues[3]
            allSuggestions.any { it.stepNum == stepNum && it.cmdCode == cmd && it.fieldCode == code }
        }
    } else true

    val borderColor = when {
        hasRef && refValid -> AccentOrange
        hasRef && !refValid -> AccentRed
        else -> PrimaryBlue
    }

    var tfv by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    if (tfv.text != value) {
        tfv = TextFieldValue(value, TextRange(value.length))
    }

    val acContext = remember(tfv.text, tfv.selection) {
        if (tfv.selection.collapsed) detectAutocompleteContext(tfv.text, tfv.selection.start) else null
    }
    val filteredSuggestions = remember(acContext, allSuggestions) {
        if (acContext == null) emptyList()
        else when (acContext.phase) {
            AutocompletePhase.STEP -> {
                if (acContext.partialStep.isEmpty()) allSuggestions
                else allSuggestions.filter { it.stepNum.toString().startsWith(acContext.partialStep) }
            }
            AutocompletePhase.CMD -> {
                val stepNum = acContext.partialStep.toIntOrNull()
                val forStep = if (stepNum != null) allSuggestions.filter { it.stepNum == stepNum } else allSuggestions
                if (acContext.partialCmd.isEmpty()) forStep
                else forStep.filter { it.cmdCode.startsWith(acContext.partialCmd, ignoreCase = true) }
            }
            AutocompletePhase.FIELD -> {
                val stepNum = acContext.partialStep.toIntOrNull()
                allSuggestions.filter {
                    (stepNum == null || it.stepNum == stepNum) &&
                        it.cmdCode == acContext.partialCmd &&
                        it.fieldCode.startsWith(acContext.partialField, ignoreCase = true)
                }
            }
        }
    }

    var selectedHintIndex by remember { mutableStateOf(0) }
    // Reset selection when suggestions change
    LaunchedEffect(filteredSuggestions) { selectedHintIndex = 0 }

    val showPopup = filteredSuggestions.isNotEmpty() && acContext != null

    fun applySuggestion(suggestion: RefSuggestion) {
        val ctx = acContext ?: return
        val before = tfv.text.substring(0, ctx.replaceStart)
        val after = tfv.text.substring(tfv.selection.start)
        val newText = before + suggestion.insertText + after
        val newCursor = before.length + suggestion.insertText.length
        tfv = TextFieldValue(newText, TextRange(newCursor))
        onValueChange(newText)
    }

    Column(modifier = modifier) {
        Box {
            FixedOutlinedTextField(
                value = tfv,
                onValueChange = { newTfv ->
                    tfv = newTfv
                    onValueChange(newTfv.text)
                },
                modifier = Modifier.fillMaxWidth()
                    .onPreviewKeyEvent { event ->
                        if (!showPopup || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.Tab, Key.Enter -> {
                                if (filteredSuggestions.isNotEmpty()) {
                                    applySuggestion(filteredSuggestions[selectedHintIndex.coerceIn(filteredSuggestions.indices)])
                                    true
                                } else false
                            }
                            Key.DirectionDown -> {
                                selectedHintIndex = (selectedHintIndex + 1).coerceAtMost(filteredSuggestions.lastIndex)
                                true
                            }
                            Key.DirectionUp -> {
                                selectedHintIndex = (selectedHintIndex - 1).coerceAtLeast(0)
                                true
                            }
                            Key.Escape -> {
                                // Close popup by moving cursor to clear context
                                true
                            }
                            else -> false
                        }
                    },
                singleLine = true,
                label = { Text(label, fontSize = 11.sp) },
                placeholder = {
                    Text(hint, fontSize = 10.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.25f))
                },
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = Mono,
                    fontSize = 12.sp,
                    color = if (hasRef) borderColor else MaterialTheme.colors.onSurface,
                ),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = borderColor,
                    unfocusedBorderColor = if (hasRef) borderColor.copy(alpha = 0.5f)
                    else MaterialTheme.colors.onSurface.copy(alpha = if (isOpt) 0.1f else 0.2f),
                    focusedLabelColor = borderColor,
                ),
            )

            // Autocomplete dropdown
            if (showPopup) {
                Popup(
                    alignment = Alignment.TopStart,
                    onDismissRequest = { },
                    properties = PopupProperties(focusable = false),
                ) {
                    Surface(
                        modifier = Modifier
                            .widthIn(min = 200.dp, max = 420.dp)
                            .heightIn(max = 220.dp)
                            .padding(top = 56.dp), // offset below the text field
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colors.surface,
                        elevation = 8.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.1f)),
                    ) {
                        Column {
                            // Hint header
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .background(PrimaryBlue.copy(alpha = 0.06f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(Icons.Default.Lightbulb, null, modifier = Modifier.size(12.dp), tint = AccentOrange)
                                Text(
                                    when (acContext?.phase) {
                                        AutocompletePhase.STEP -> "Select step"
                                        AutocompletePhase.CMD -> "Select command"
                                        AutocompletePhase.FIELD -> "Select field"
                                        else -> ""
                                    },
                                    fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                )
                                Spacer(Modifier.weight(1f))
                                Text("↑↓ navigate  ⏎/Tab accept", fontSize = 8.sp, fontFamily = Mono, color = MaterialTheme.colors.onSurface.copy(alpha = 0.3f))
                            }

                            val scrollState = rememberScrollState()
                            Column(
                                modifier = Modifier.verticalScroll(scrollState).padding(4.dp),
                            ) {
                                filteredSuggestions.forEachIndexed { idx, suggestion ->
                                    val isSelected = idx == selectedHintIndex
                                    Surface(
                                        modifier = Modifier.fillMaxWidth()
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { applySuggestion(suggestion) },
                                        color = if (isSelected) PrimaryBlue.copy(alpha = 0.1f) else Color.Transparent,
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            // Step number badge
                                            Surface(
                                                shape = RoundedCornerShape(3.dp),
                                                color = AccentGreen.copy(alpha = 0.12f),
                                            ) {
                                                Text(
                                                    "${suggestion.stepNum}",
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                    fontFamily = Mono, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccentGreen,
                                                )
                                            }
                                            // Command code badge
                                            Surface(
                                                shape = RoundedCornerShape(3.dp),
                                                color = PrimaryBlue.copy(alpha = 0.12f),
                                            ) {
                                                Text(
                                                    suggestion.cmdCode,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                    fontFamily = Mono, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue,
                                                )
                                            }
                                            // Field code badge
                                            Surface(
                                                shape = RoundedCornerShape(3.dp),
                                                color = AccentOrange.copy(alpha = 0.12f),
                                            ) {
                                                Text(
                                                    suggestion.fieldCode,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                    fontFamily = Mono, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccentOrange,
                                                )
                                            }
                                            // Field name
                                            Text(
                                                suggestion.fieldName,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            if (isSelected) {
                                                Spacer(Modifier.weight(1f))
                                                Icon(Icons.Default.KeyboardReturn, null, modifier = Modifier.size(12.dp), tint = PrimaryBlue.copy(alpha = 0.5f))
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

        // Status text below field
        if (hasRef && !refValid) {
            Text("Invalid reference — step, command, or field code not found", fontSize = 9.sp, color = AccentRed, modifier = Modifier.padding(start = 4.dp, top = 1.dp))
        } else if (hasRef && refValid) {
            val refDescs = REF_PATTERN.findAll(value).mapNotNull { match ->
                val stepNum = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
                val cmd = match.groupValues[2]
                val code = match.groupValues[3]
                allSuggestions.find { it.stepNum == stepNum && it.cmdCode == cmd && it.fieldCode == code }?.let {
                    "${it.insertText} \u2192 ${it.fieldName}"
                }
            }.toList()
            Column(modifier = Modifier.padding(start = 4.dp, top = 1.dp)) {
                for (desc in refDescs) {
                    Text(desc, fontSize = 9.sp, color = AccentOrange.copy(alpha = 0.7f))
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  Step result panel
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun StepResultPanel(result: StepExecutionResult, def: ThalesCommandDefinition) {
    val isSuccess = result.success
    val codes = remember(def) { buildResponseFieldCodes(def) }

    Surface(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp), color = MaterialTheme.colors.surface) {
        Column {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = (if (isSuccess) AccentGreen else AccentRed).copy(alpha = 0.06f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        if (isSuccess) Icons.Default.Check else Icons.Default.Close,
                        null, modifier = Modifier.size(14.dp),
                        tint = if (isSuccess) AccentGreen else AccentRed,
                    )
                    Text(
                        "Response (${result.elapsedMs}ms)",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = if (isSuccess) AccentGreen else AccentRed,
                    )
                }
            }

            if (result.parsedResponse.isNotEmpty()) {
                val scrollState = rememberScrollState()
                Column(modifier = Modifier.verticalScroll(scrollState).padding(10.dp)) {
                    // Column headers
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("CODE", Modifier.width(60.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f))
                        Text("FIELD", Modifier.width(120.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f))
                        Text("VALUE", Modifier.weight(1f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f))
                    }
                    Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.06f))

                    for ((field, value) in result.parsedResponse) {
                        val code = codes[field.id] ?: field.id.uppercase()
                        val isError = field.id == "errorCode" && value != "00"

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = PrimaryBlue.copy(alpha = 0.1f),
                            ) {
                                Text(
                                    code,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp).width(52.dp),
                                    fontFamily = Mono, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue,
                                )
                            }
                            Text(
                                field.name, Modifier.width(120.dp),
                                fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                            )
                            Text(
                                if (field.id == "errorCode") "$value (${ThalesErrorCodes.getDescription(value)})" else value,
                                modifier = Modifier.weight(1f),
                                fontSize = 10.sp, fontFamily = Mono,
                                fontWeight = if (isError) FontWeight.Bold else FontWeight.Normal,
                                color = if (isError) AccentRed else MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  Command picker dialog
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun CommandPickerDialog(
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
                            Icon(Icons.Default.AddCircle, null, tint = Color.White, modifier = Modifier.size(22.dp))
                            Text(
                                "Add Command Step",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        // Search bar
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
                    Surface(
                        color = MaterialTheme.colors.surface,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            // "All" chip
                            val allSelected = selectedCategory == null
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (allSelected) PrimaryBlue else MaterialTheme.colors.onSurface.copy(alpha = 0.06f),
                                modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { selectedCategory = null },
                            ) {
                                Text(
                                    "All",
                                    fontSize = 11.sp,
                                    fontWeight = if (allSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (allSelected) Color.White else MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                )
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
                                        Icon(
                                            categoryIcon(cat), null,
                                            modifier = Modifier.size(12.dp),
                                            tint = if (isSelected) Color.White else MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                        )
                                        Text(
                                            cat.displayName.split(" ").first(),
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                        )
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
                            else defs.filter {
                                it.code.contains(searchQuery, ignoreCase = true) || it.name.contains(searchQuery, ignoreCase = true)
                            }
                            if (filtered.isEmpty()) continue

                            // Category header
                            item {
                                Row(
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        categoryIcon(category), null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.35f),
                                    )
                                    Text(
                                        category.displayName.uppercase(),
                                        style = MaterialTheme.typography.overline,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                                    )
                                    Divider(
                                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.06f),
                                    )
                                }
                            }

                            items(filtered.size) { i ->
                                val def = filtered[i]
                                val codes = buildResponseFieldCodes(def)
                                val hasOutputFields = def.responseFields.isNotEmpty()

                                Surface(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onSelect(def) },
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.03f),
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        // Command code badge
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = PrimaryBlue.copy(alpha = 0.1f),
                                        ) {
                                            Text(
                                                def.code,
                                                fontFamily = Mono,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = PrimaryBlue,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            // Command name
                                            Text(
                                                def.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.85f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Spacer(Modifier.height(3.dp))

                                            // Field counts
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Icon(Icons.Default.Input, null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colors.onSurface.copy(alpha = 0.35f))
                                                    Text(
                                                        "${def.requestFields.size} input",
                                                        fontSize = 9.sp,
                                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                                                    )
                                                }
                                                if (hasOutputFields) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        Icon(Icons.Default.Output, null, modifier = Modifier.size(10.dp), tint = AccentGreen.copy(alpha = 0.6f))
                                                        Text(
                                                            "${def.responseFields.size} output",
                                                            fontSize = 9.sp,
                                                            color = AccentGreen.copy(alpha = 0.7f),
                                                        )
                                                    }
                                                }
                                            }

                                            // Response field codes
                                            if (hasOutputFields) {
                                                Spacer(Modifier.height(4.dp))
                                                Row(
                                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                ) {
                                                    for ((_, code) in codes.entries.take(6)) {
                                                        Surface(
                                                            shape = RoundedCornerShape(3.dp),
                                                            color = AccentGreen.copy(alpha = 0.08f),
                                                        ) {
                                                            Text(
                                                                code,
                                                                fontSize = 8.sp,
                                                                fontFamily = Mono,
                                                                fontWeight = FontWeight.Medium,
                                                                color = AccentGreen.copy(alpha = 0.7f),
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                            )
                                                        }
                                                    }
                                                    if (codes.size > 6) {
                                                        Text(
                                                            "+${codes.size - 6}",
                                                            fontSize = 8.sp,
                                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Arrow icon
                                        Icon(
                                            Icons.Default.ChevronRight, null,
                                            modifier = Modifier.size(18.dp).align(Alignment.CenterVertically),
                                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer with cancel
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colors.surface,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${definitions.size} commands available",
                            fontSize = 10.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.35f),
                        )
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  Execution engine
// ──────────────────────────────────────────────────────────────────────────────

private suspend fun executeScenario(
    service: HsmCommandClientService,
    session: ScenarioSessionState,
) {
    // Collect per-step response data for resolving references
    // Index = step index (0-based), Value = map of field code → value
    val stepResponses = mutableListOf<Map<String, String>>() // index-aligned with session.steps

    val tcpFrameBytes = when (service.config.hsmVendor.headerFormat) {
        HeaderFormat.TWO_BYTE_LENGTH -> if (service.config.tcpLengthHeaderEnabled) 2 else 0
        HeaderFormat.FOUR_BYTE_ASCII_LENGTH -> 4
        else -> 0
    }
    val msgHeaderBytes = service.config.headerValue.length

    for ((index, step) in session.steps.withIndex()) {
        val codes = buildResponseFieldCodes(step.definition)

        // Resolve [STEP][CMD][CODE] references in field values
        val resolvedValues = step.fieldValues.toMutableMap()
        for ((fieldId, value) in step.fieldValues) {
            if (REF_PATTERN.containsMatchIn(value)) {
                var resolved = value
                for (match in REF_PATTERN.findAll(value)) {
                    val refStepNum = match.groupValues[1].toIntOrNull() ?: continue
                    val refCode = match.groupValues[3]
                    val refStepIdx = refStepNum - 1 // convert 1-based to 0-based
                    if (refStepIdx in stepResponses.indices) {
                        val sourceValue = stepResponses[refStepIdx][refCode]
                        if (sourceValue != null) {
                            resolved = resolved.replace(match.value, sourceValue)
                        }
                    }
                }
                resolvedValues[fieldId] = resolved
            }
        }

        val commandText = ThalesWireBuilder.buildPlainTextCommand(step.definition, resolvedValues)

        try {
            val result = service.sendCommand(commandText)

            val parsed = try {
                ThalesWireBuilder.parseResponseFields(
                    step.definition, result.rawResponse,
                    tcpFrameBytes + msgHeaderBytes,
                    requestFieldValues = resolvedValues,
                )
            } catch (_: Exception) { emptyList() }

            // Build code→value map for this step's response
            val codeToValue = mutableMapOf<String, String>()
            val responseValueMap = mutableMapOf<String, String>()
            for ((field, value) in parsed) {
                responseValueMap[field.id] = value
                codes[field.id]?.let { code -> codeToValue[code] = value }
            }
            stepResponses.add(codeToValue)

            val errorCode = parsed.find { it.first.id == "errorCode" }?.second
            val isSuccess = result.success && (errorCode == null || errorCode == "00")

            session.results.add(
                StepExecutionResult(
                    stepIndex = index,
                    success = isSuccess,
                    parsedResponse = parsed,
                    responseValueMap = responseValueMap,
                    rawRequest = result.formattedRequest,
                    rawResponse = result.formattedResponse,
                    elapsedMs = result.elapsedMs,
                    errorMessage = if (!isSuccess) {
                        errorCode?.let { "Error $it: ${ThalesErrorCodes.getDescription(it)}" } ?: result.errorMessage
                    } else null,
                )
            )

            if (!isSuccess) break
        } catch (e: Exception) {
            stepResponses.add(emptyMap())
            session.results.add(
                StepExecutionResult(
                    stepIndex = index,
                    success = false,
                    parsedResponse = emptyList(),
                    responseValueMap = emptyMap(),
                    rawRequest = commandText,
                    rawResponse = "",
                    errorMessage = e.message ?: "Unknown error",
                )
            )
            break
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  Utilities
// ──────────────────────────────────────────────────────────────────────────────

/** Build available reference codes from all steps before [currentIndex]. Keyed by step number (1-based). */
private fun buildAvailableReferences(
    steps: List<ScenarioStep>,
    currentIndex: Int,
): Map<String, Map<String, String>> {
    val result = linkedMapOf<String, Map<String, String>>()
    for (i in 0 until currentIndex.coerceAtMost(steps.size)) {
        val step = steps[i]
        val codes = buildResponseFieldCodes(step.definition)
        result[step.definition.code] = codes // still keyed by cmd for the reference panel display
    }
    return result
}

/** Validate all [STEP][CMD][CODE] references and return error messages. */
private fun validateReferences(
    fieldValues: Map<String, String>,
    allSteps: List<ScenarioStep>,
    currentStepIndex: Int,
): List<String> {
    val errors = mutableListOf<String>()
    val suggestions = buildSuggestionList(allSteps, currentStepIndex)
    for ((fieldId, value) in fieldValues) {
        for (match in REF_PATTERN.findAll(value)) {
            val stepNum = match.groupValues[1].toIntOrNull()
            val cmd = match.groupValues[2]
            val code = match.groupValues[3]
            if (stepNum == null || !suggestions.any { it.stepNum == stepNum && it.cmdCode == cmd && it.fieldCode == code }) {
                errors.add("[${match.groupValues[1]}][$cmd][$code] in \"$fieldId\": not found in prior steps")
            }
        }
    }
    return errors
}

/** Collect human-readable reference descriptions flowing into a step. */
private fun collectReferences(
    step: ScenarioStep,
    allSteps: List<ScenarioStep>,
    stepIndex: Int,
): List<String> {
    val labels = mutableListOf<String>()
    for ((fieldId, value) in step.fieldValues) {
        for (match in REF_PATTERN.findAll(value)) {
            val stepNum = match.groupValues[1]
            val cmd = match.groupValues[2]
            val code = match.groupValues[3]
            labels.add("S$stepNum.$cmd.$code \u2192 $fieldId")
        }
    }
    return labels
}
