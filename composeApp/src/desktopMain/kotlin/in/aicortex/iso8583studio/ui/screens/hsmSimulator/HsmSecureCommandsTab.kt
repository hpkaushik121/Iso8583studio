package `in`.aicortex.iso8583studio.ui.screens.hsmSimulator

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.domain.service.hsmSimulatorService.HsmServiceImpl
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.AuthActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Secure Commands Tab — provides GUI access to offline/administrator HSM commands
 * that require physical presence (LMK management, key formation, key loading).
 *
 * In the real PayShield 10K, these operations require smart cards (custodian cards)
 * and physical access to the HSM console. In the simulator, the GUI serves as that
 * secure interface.
 *
 * Commands available here:
 *   GK - Generate LMK Component
 *   GC - Generate Key Component
 *   FK - Form Key from Components
 *   A0 - Generate Key (under LMK)
 *   LA - Load Data to User Storage
 *   LE - Read Data from User Storage
 *   LD - Delete Data from User Storage
 *   BW - Translate Key (old → new LMK)
 *   BG - Translate PIN (old → new LMK)
 *   DK - Decrypt Key Under LMK (get plain key)
 *   EI - Generate RSA Key Pair
 *   VT - View LMK Table
 *   VR - Version Information
 *   NC - Diagnostic Test
 */
@Composable
fun HsmSecureCommandsTab(hsm: HsmServiceImpl) {
    val scope = rememberCoroutineScope()

    var selectedCommand by remember { mutableStateOf<SecureCommand?>(null) }
    var resultText by remember { mutableStateOf("") }
    var isExecuting by remember { mutableStateOf(false) }
    val commandLog = remember { mutableStateListOf<String>() }

    Column(modifier = Modifier.fillMaxSize()) {
        // Authorization status banner — always shown at the top
        ConsoleAuthorizationBanner(hsm = hsm, scope = scope)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.weight(1f)) {
        // Left panel — command list
        Card(
            modifier = Modifier.width(260.dp).fillMaxHeight(),
            elevation = 2.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colors.primary)
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            "Secure Commands",
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.onPrimary
                        )
                        Text(
                            "Administrator / LMK Operations",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(SECURE_COMMANDS) { cmd ->
                        SecureCommandListItem(
                            command = cmd,
                            isSelected = selectedCommand == cmd,
                            onClick = {
                                selectedCommand = cmd
                                resultText = ""
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Right panel — command form + result
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            if (selectedCommand == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Select a secure command from the list",
                            style = MaterialTheme.typography.subtitle1,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                val cmd = selectedCommand!!
                SecureCommandForm(
                    command = cmd,
                    isExecuting = isExecuting,
                    resultText = resultText,
                    onExecute = { params ->
                        isExecuting = true
                        scope.launch {
                            try {
                                val result = when (cmd.code) {
                                    "GC" -> executeGcMultiple(cmd, params, hsm)
                                    "FK" -> executeFkDirect(params, hsm)
                                    "DK" -> executeDecryptKeyDirect(params, hsm)
                                    else -> {
                                        val rawCmd = buildHsmCommand(cmd, params, hsm)
                                        hsm.executeSecureCommand(rawCmd, source = "SECURE-CMD")
                                    }
                                }
                                resultText = result
                                commandLog.add("[${cmd.code}] OK: ${result.take(200)}")
                            } catch (e: Exception) {
                                resultText = "ERROR: ${e.message}"
                                commandLog.add("[${cmd.code}] ERROR: ${e.message}")
                            } finally {
                                isExecuting = false
                            }
                        }
                    }
                )
            }
        }
        } // end Row
    } // end Column
}

// ====================================================================================================
// Console Authorization Banner
// ====================================================================================================

private val CONSOLE_ACTIVITIES = listOf(
    AuthActivity.COMPONENT_KEY_CONSOLE,
    AuthActivity.ADMIN_CONSOLE,
    AuthActivity.AUDIT_CONSOLE,
    AuthActivity.MISC_CONSOLE,
    AuthActivity.CLEAR_PIN_CONSOLE,
)

@Composable
private fun ConsoleAuthorizationBanner(
    hsm: HsmServiceImpl,
    scope: kotlinx.coroutines.CoroutineScope
) {
    // Tick every second to update the countdown
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            nowMs = System.currentTimeMillis()
        }
    }

    val expiryMs = remember(nowMs) { hsm.consoleAuthExpiry(CONSOLE_ACTIVITIES) }
    val isAuthorized = expiryMs != null && expiryMs > nowMs

    // Authorize dialog state
    var showAuthorizeDialog by remember { mutableStateOf(false) }
    var officerCount by remember { mutableStateOf(1) }
    var durationHours by remember { mutableStateOf(8) }

    val bannerColor by animateColorAsState(
        targetValue = if (isAuthorized) Color(0xFF1B5E20) else Color(0xFF4E342E),
        animationSpec = tween(400)
    )
    val accentColor = if (isAuthorized) Color(0xFF4CAF50) else Color(0xFFFF7043)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = bannerColor,
        shape = RoundedCornerShape(8.dp),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isAuthorized) Icons.Default.LockOpen else Icons.Default.Lock,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isAuthorized) "Console Authorized" else "Console Not Authorized",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (isAuthorized) {
                        val remaining = (expiryMs!! - nowMs) / 1000L
                        val h = remaining / 3600; val m = (remaining % 3600) / 60; val s = remaining % 60
                        "Valid for %02d:%02d:%02d  ·  Granted to all console activities".format(h, m, s)
                    } else {
                        "Secure commands require console authorization. In a real HSM, custodian cards must be inserted."
                    },
                    style = MaterialTheme.typography.caption,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }

            if (isAuthorized) {
                OutlinedButton(
                    onClick = { hsm.revokeConsole(CONSOLE_ACTIVITIES) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF7043)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF7043))
                ) {
                    Text("Revoke", style = MaterialTheme.typography.caption)
                }
            } else {
                Button(
                    onClick = { showAuthorizeDialog = true },
                    colors = ButtonDefaults.buttonColors(backgroundColor = accentColor)
                ) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Authorize Console", style = MaterialTheme.typography.caption)
                }
            }
        }
    }

    // ── Authorize Dialog ──────────────────────────────────────────────────────
    if (showAuthorizeDialog) {
        AlertDialog(
            onDismissRequest = { showAuthorizeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colors.primary)
                    Text("Authorize HSM Console")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Simulates custodian smart card insertion. In a real PayShield 10K, " +
                        "this requires physical presence of authorized officers with their cards.",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )

                    // Officer count
                    Column {
                        Text(
                            "Number of Officers: $officerCount",
                            style = MaterialTheme.typography.caption,
                            fontWeight = FontWeight.SemiBold
                        )
                        Slider(
                            value = officerCount.toFloat(),
                            onValueChange = { officerCount = it.toInt() },
                            valueRange = 1f..3f,
                            steps = 1
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("1 Officer", "2 Officers", "3 Officers").forEach {
                                Text(it, style = MaterialTheme.typography.overline)
                            }
                        }
                    }

                    // Duration
                    Column {
                        Text(
                            "Authorization Duration: $durationHours hours",
                            style = MaterialTheme.typography.caption,
                            fontWeight = FontWeight.SemiBold
                        )
                        Slider(
                            value = durationHours.toFloat(),
                            onValueChange = { durationHours = it.toInt().coerceAtLeast(1) },
                            valueRange = 1f..24f,
                            steps = 22
                        )
                    }

                    // Activities to authorize
                    Surface(color = MaterialTheme.colors.primary.copy(alpha = 0.08f), shape = RoundedCornerShape(6.dp)) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Authorizing:", style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
                            CONSOLE_ACTIVITIES.forEach { act ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null,
                                        tint = MaterialTheme.colors.primary, modifier = Modifier.size(12.dp))
                                    Text(act.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.overline)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val officers = (1..officerCount).map { "SIM-OFFICER-$it" }
                        hsm.authorizeConsole(
                            activities = CONSOLE_ACTIVITIES,
                            durationMs = durationHours * 60L * 60 * 1000,
                            officerNames = officers
                        )
                        showAuthorizeDialog = false
                    }
                ) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Authorize")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAuthorizeDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ====================================================================================================
// Command List Item
// ====================================================================================================

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SecureCommandListItem(
    command: SecureCommand,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.1f) else Color.Transparent,
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = command.category.icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = command.category.color
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = command.code,
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Chip(
                    text = command.category.label,
                    color = command.category.color
                )
            }
            Text(
                text = command.name,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 24.dp)
            )
        }
    }
    Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.05f))
}

@Composable
private fun Chip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.overline,
            color = color,
            fontSize = 9.sp
        )
    }
}

// ====================================================================================================
// Command Form
// ====================================================================================================

@Composable
private fun SecureCommandForm(
    command: SecureCommand,
    isExecuting: Boolean,
    resultText: String,
    onExecute: (Map<String, String>) -> Unit
) {
    val params = remember(command) { mutableStateMapOf<String, String>() }
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Card(elevation = 2.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = command.category.icon,
                    contentDescription = null,
                    tint = command.category.color,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row {
                        Text(
                            command.code,
                            style = MaterialTheme.typography.h6,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            command.name,
                            style = MaterialTheme.typography.h6
                        )
                    }
                    Text(
                        command.description,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Split layout: form on left, output on right
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Parameters form
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                elevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    Text(
                        "Parameters",
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    command.parameters.forEach { param ->
                        ParamField(
                            param = param,
                            value = params[param.name] ?: param.default,
                            onValueChange = { params[param.name] = it }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Wire format hint
                    if (command.wireFormat.isNotBlank()) {
                        Text(
                            "Wire format:",
                            style = MaterialTheme.typography.caption,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            command.wireFormat,
                            style = MaterialTheme.typography.caption,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Button(
                        onClick = { onExecute(params.toMap()) },
                        enabled = !isExecuting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isExecuting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colors.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Execute ${command.code}")
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Output panel
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                elevation = 1.dp
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Output, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Response",
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (resultText.isBlank()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Response will appear here after executing",
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                                style = MaterialTheme.typography.body2
                            )
                        }
                    } else {
                        TextField(
                            value = resultText,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxSize(),
                            textStyle = LocalTextStyle.current.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            ),
                            colors = TextFieldDefaults.textFieldColors(
                                backgroundColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ParamField(
    param: CommandParam,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                param.name,
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.SemiBold
            )
            if (param.required) {
                Text(" *", color = MaterialTheme.colors.error, style = MaterialTheme.typography.caption)
            }
        }
        if (param.description.isNotBlank()) {
            Text(
                param.description,
                style = MaterialTheme.typography.overline,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )
        }

        if (param.options.isNotEmpty()) {
            var expanded by remember { mutableStateOf(false) }
            val selectedLabel = param.options.find { it.first == value }?.second
                ?: param.options.find { it.first == param.default }?.second
                ?: value

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedLabel,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    param.options.forEach { (code, label) ->
                        DropdownMenuItem(onClick = {
                            onValueChange(code)
                            expanded = false
                        }) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.body2,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = !param.multiline,
                maxLines = if (param.multiline) 4 else 1,
                placeholder = { Text(param.placeholder, style = MaterialTheme.typography.caption) },
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            )
        }
    }
}

// ====================================================================================================
// Build HSM Command strings from form parameters
// ====================================================================================================

private suspend fun buildHsmCommand(
    cmd: SecureCommand,
    params: Map<String, String>,
    hsm: HsmServiceImpl
): String {
    val header = "0000"
    return when (cmd.code) {
        "NC" -> "${header}NC"
        "VR" -> "${header}VR"
        "VT" -> "${header}VT"
        "GK" -> "${header}GK"
        "GC" -> buildGCCommand(header, params)
        "A0" -> buildA0Command(header, params)
        "FK" -> buildFKCommand(header, params)
        "LA" -> buildLACommand(header, params)
        "LE" -> buildLECommand(header, params)
        "LD" -> buildLDCommand(header, params)
        "BW" -> buildBWCommand(header, params)
        "BG" -> buildBGCommand(header, params)
        "EI" -> buildEICommand(header, params)
        "DE" -> buildDECommand(header, params)
        "DG" -> buildDGCommand(header, params)
        else -> "${header}NC"
    }
}

/**
 * Secure-command FK execution (direct, bypasses wire format).
 * Shows: plain working key  +  key under LMK  +  KCV.
 * The plain key is intentionally visible here because this is the HSM console
 * (secure room, physical access required). On the host-command wire it is never exposed.
 */
private suspend fun executeFkDirect(
    params: Map<String, String>,
    hsm: HsmServiceImpl
): String {
    val numComponents = (params["numComponents"] ?: "2").toIntOrNull()?.coerceIn(2, 3) ?: 2
    val keyType       = params["keyType"]  ?: "001"
    val scheme        = params["scheme"]   ?: "U"

    // Collect whichever component fields are filled in
    val componentList = (1..numComponents).mapNotNull { i ->
        params["component$i"]?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
    }

    if (componentList.size < 2) {
        return buildString {
            appendLine("✗  Form Key failed")
            appendLine()
            appendLine("  Need at least 2 component values.")
            appendLine("  Fill in component1, component2 (and optionally component3).")
            appendLine("  Each component is a hex string — e.g. use the GC command output.")
        }
    }

    val keyLabel = when (keyType) {
        "000" -> "ZMK  (Zone Master Key)"
        "001" -> "ZPK  (Zone PIN Key)"
        "002" -> "TPK / PVK  (Terminal PIN / Verification Key)"
        "003" -> "CVK  (Card Verification Key)"
        "008" -> "TAK  (Terminal Authentication Key)"
        "009" -> "ZAK / BDK  (Zone Auth / Base Derivation Key)"
        "109" -> "ZEK  (Zone Encryption Key)"
        "209" -> "BDK – DUKPT  (Base Derivation Key)"
        else  -> "Key type $keyType"
    }
    val schemeLabel = when (scheme.uppercase()) {
        "U" -> "Double-length 3DES  (16 bytes / 32 hex)"
        "X" -> "Triple-length 3DES  (24 bytes / 48 hex)"
        "T" -> "Single-length DES   (8 bytes  / 16 hex)"
        else -> scheme
    }

    val data = hsm.formKeyFromComponents(keyType, scheme, componentList)

    return buildString {
        appendLine("╔════════════════════════════════════════════════════╗")
        appendLine("║              FORM KEY FROM COMPONENTS              ║")
        appendLine("╚════════════════════════════════════════════════════╝")
        appendLine()
        appendLine("  Key Type : $keyLabel")
        appendLine("  Scheme   : $schemeLabel")
        appendLine("  Input    : ${componentList.size} components XOR'd together")
        appendLine()

        if (data == null) {
            appendLine("  ✗ HSM not started or not available.")
        } else if (data.containsKey("error")) {
            appendLine("  ✗ Error: ${data["error"]}")
        } else {
            val clearKey = data["clearKey"]     ?: "?"
            val encKey   = data["encryptedKey"] ?: "?"
            val kcv      = data["kcv"]          ?: "?"

            appendLine("  ┌─── Components Used ────────────────────────────────┐")
            componentList.forEachIndexed { i, comp ->
                appendLine("  │  Component ${i + 1} : ${comp.chunked(8).joinToString(" ")}")
            }
            appendLine("  └──────────────────────────────────────────────────┘")
            appendLine()
            appendLine("  ════════════════════════════════════════════════════")
            appendLine("   RESULT")
            appendLine("  ════════════════════════════════════════════════════")
            appendLine()
            appendLine("  Plain Working Key (XOR result — console only):")
            appendLine("    ${clearKey.chunked(8).joinToString(" ")}")
            appendLine()
            appendLine("  Key Under LMK (use this in transactions):")
            appendLine("    ${encKey.chunked(8).joinToString(" ")}")
            appendLine()
            appendLine("  Key Check Value (KCV) : $kcv")
            appendLine()
            appendLine("  ✓ Key formed successfully.")
            appendLine()
            appendLine("  ─── IMPORTANT ───────────────────────────────────────")
            appendLine("  • Distribute 'Key Under LMK' to terminals/hosts.")
            appendLine("  • Verify KCV with each custodian before distributing.")
            appendLine("  • The plain key above must NEVER leave the secure room.")
        }
    }
}

/**
 * Secure-command DK execution (direct, bypasses wire format).
 * Decrypts a key encrypted under LMK and shows the plain (clear) key + KCV.
 */
private suspend fun executeDecryptKeyDirect(
    params: Map<String, String>,
    hsm: HsmServiceImpl
): String {
    val encryptedKey = params["encryptedKey"]?.trim()?.uppercase() ?: ""
    val keyType      = params["keyType"]?.trim() ?: "001"

    if (encryptedKey.isBlank()) {
        return buildString {
            appendLine("✗  Decrypt Key failed")
            appendLine()
            appendLine("  The encrypted key field is empty.")
            appendLine("  Enter the LMK-encrypted key (hex) — with or without scheme prefix (U/X/T).")
        }
    }

    val keyTypeLabels = mapOf(
        "000" to "ZMK  (Zone Master Key)",
        "001" to "ZPK  (Zone PIN Key)",
        "002" to "PVK / TPK / TMK",
        "003" to "TAK  (Terminal Authentication Key)",
        "008" to "ZAK  (Zone Authentication Key)",
        "009" to "BDK  (Base Derivation Key)",
        "109" to "MK-AC",
        "209" to "MK-SMI",
        "309" to "MK-SMC",
        "409" to "MK-DAC",
        "509" to "MK-DN",
        "609" to "BDK-2",
        "709" to "MK-CVC3",
        "809" to "BDK-3",
        "909" to "BDK-4",
        "00A" to "ZEK  (Zone Encryption Key)",
        "00B" to "DEK  (Data Encryption Key)",
        "402" to "CVK  (Card Verification Key)",
        "302" to "IKEY (Initial Key for DUKPT)"
    )

    val keyLabel = keyTypeLabels[keyType] ?: "Key type $keyType"

    val data = hsm.decryptKeyUnderLmk(encryptedKey, keyType)

    return buildString {
        appendLine("╔════════════════════════════════════════════════════╗")
        appendLine("║           DECRYPT KEY UNDER LMK                    ║")
        appendLine("╚════════════════════════════════════════════════════╝")
        appendLine()
        appendLine("  Key Type : $keyLabel")
        appendLine()

        if (data == null) {
            appendLine("  ✗ HSM not started or not available.")
        } else if (data.containsKey("error")) {
            appendLine("  ✗ Error: ${data["error"]}")
        } else {
            val clearKey    = data["clearKey"]            ?: "?"
            val kcv         = data["kcv"]                 ?: "?"
            val lmkPair     = data["lmkPair"]             ?: "?"
            val variant     = data["variant"]             ?: "0"
            val description = data["keyTypeDescription"]  ?: ""

            appendLine("  ┌─── Input ──────────────────────────────────────────┐")
            appendLine("  │  Encrypted Key (under LMK):")
            appendLine("  │    ${encryptedKey.chunked(8).joinToString(" ")}")
            appendLine("  │")
            appendLine("  │  LMK Pair Used : $lmkPair ($description)")
            if (variant != "0") {
                appendLine("  │  LMK Variant   : $variant")
            }
            appendLine("  └──────────────────────────────────────────────────┘")
            appendLine()
            appendLine("  ════════════════════════════════════════════════════")
            appendLine("   RESULT")
            appendLine("  ════════════════════════════════════════════════════")
            appendLine()
            appendLine("  Plain Key (clear — console only):")
            appendLine("    ${clearKey.chunked(8).joinToString(" ")}")
            appendLine()
            appendLine("  Key Check Value (KCV) : $kcv")
            appendLine()
            appendLine("  ✓ Key decrypted successfully.")
            appendLine()
            appendLine("  ─── WARNING ─────────────────────────────────────────")
            appendLine("  • The plain key above must NEVER leave the secure room.")
            appendLine("  • Use the KCV to verify the key matches your records.")
            appendLine("  • This operation is for diagnostic/audit purposes only.")
        }
    }
}

private fun buildGCCommand(header: String, params: Map<String, String>): String {
    val keyType = params["keyType"] ?: "001"
    val scheme   = params["scheme"]  ?: "U"
    return "${header}GC$keyType$scheme"
}

/**
 * Generates [numComponents] independent key components by calling the HSM directly
 * (bypassing the wire protocol), then:
 *  - Shows each component's clear value and its individual KCV
 *  - XORs all clear components to derive the final working key
 *  - Computes and shows the final key's KCV
 */
private suspend fun executeGcMultiple(
    cmd: SecureCommand,
    params: Map<String, String>,
    hsm: HsmServiceImpl
): String {
    val n        = (params["numComponents"] ?: "2").toIntOrNull()?.coerceIn(2, 3) ?: 2
    val keyType  = params["keyType"] ?: "001"
    val scheme   = params["scheme"]  ?: "U"

    val keyLabel = when (keyType) {
        "000" -> "ZMK (Zone Master Key)"
        "001" -> "ZPK (Zone PIN Key)"
        "002" -> "TPK / PVK (Terminal PIN / Verification Key)"
        "003" -> "CVK (Card Verification Key)"
        "008" -> "TAK (Terminal Authentication Key)"
        "009" -> "ZAK / BDK (Zone Auth / Base Derivation Key)"
        "109" -> "ZEK (Zone Encryption Key)"
        "209" -> "BDK – DUKPT (Base Derivation Key)"
        else  -> "Key type $keyType"
    }
    val schemeLabel = when (scheme.uppercase()) {
        "U" -> "Double-length 3DES  (16 bytes / 32 hex)"
        "X" -> "Triple-length 3DES  (24 bytes / 48 hex)"
        "T" -> "Single-length DES   (8 bytes  / 16 hex)"
        else -> scheme
    }

    val sb = StringBuilder()
    sb.appendLine("╔════════════════════════════════════════════════════╗")
    sb.appendLine("║         KEY COMPONENT GENERATION REPORT            ║")
    sb.appendLine("╚════════════════════════════════════════════════════╝")
    sb.appendLine()
    sb.appendLine("  Key Type  : $keyLabel")
    sb.appendLine("  Scheme    : $schemeLabel")
    sb.appendLine("  Components: $n")
    sb.appendLine()

    val clearComponents = mutableListOf<String>()
    val encComponents   = mutableListOf<String>()
    val kcvs            = mutableListOf<String>()
    var allOk = true

    for (i in 1..n) {
        val data = hsm.generateKeyComponent(keyType, scheme)

        sb.appendLine("  ┌─── Component $i of $n ─────────────────────────────────┐")
        if (data == null) {
            sb.appendLine("  │  ✗ HSM not started or authorization missing")
            allOk = false
        } else {
            val clear = data["clearKey"] ?: ""
            val enc   = data["encryptedKey"] ?: ""
            val kcv   = data["kcv"] ?: "??????"
            clearComponents += clear
            encComponents   += enc
            kcvs            += kcv
            sb.appendLine("  │  Clear Component (share with custodian):")
            sb.appendLine("  │    ${clear.chunked(8).joinToString(" ")}")
            sb.appendLine("  │")
            sb.appendLine("  │  Encrypted Component (under LMK):")
            sb.appendLine("  │    ${enc.chunked(8).joinToString(" ")}")
            sb.appendLine("  │")
            sb.appendLine("  │  Component KCV : $kcv")
        }
        sb.appendLine("  └──────────────────────────────────────────────────┘")
        sb.appendLine()
    }

    // ── Final Key (XOR of all clear components) ──────────────────────────────
    if (allOk && clearComponents.size == n) {
        sb.appendLine("  ════════════════════════════════════════════════════")
        sb.appendLine("   FINAL KEY  (XOR of all $n components)")
        sb.appendLine("  ════════════════════════════════════════════════════")

        val finalKey = try { hsm.xorHexKeys(clearComponents) } catch (_: Exception) { "" }
        val finalKcv = if (finalKey.isNotEmpty()) hsm.computeKeyKcv(finalKey) else "??????"

        sb.appendLine()
        sb.appendLine("  Final Working Key (clear):")
        sb.appendLine("    ${finalKey.chunked(8).joinToString(" ")}")
        sb.appendLine()
        sb.appendLine("  Final Key KCV  : $finalKcv")
        sb.appendLine()
        sb.appendLine("  ─── Component KCV Summary ────────────────────────")
        for ((idx, kcv) in kcvs.withIndex()) {
            sb.appendLine("  Component ${idx + 1} KCV : $kcv  →  ${clearComponents[idx].take(8)}…")
        }
        sb.appendLine()
        sb.appendLine("  ✓ All $n components generated successfully.")
        sb.appendLine()
        sb.appendLine("  ─── NEXT STEPS ───────────────────────────────────")
        sb.appendLine("  • Each custodian receives their printed component.")
        sb.appendLine("  • Verify each component KCV independently.")
        sb.appendLine("  • Use command FK (Form Key) to load the working key.")
        sb.appendLine()
        sb.appendLine("  FK inputs:")
        sb.appendLine("    numComponents = $n    keyType = $keyType    scheme = $scheme")
        for ((idx, comp) in clearComponents.withIndex()) {
            sb.appendLine("    component${idx + 1}   = $comp")
            sb.appendLine("                (KCV: ${kcvs[idx]})")
        }
    } else if (!allOk) {
        sb.appendLine("  ✗ One or more components failed — review errors above.")
    }

    return sb.toString()
}

private fun buildA0Command(header: String, params: Map<String, String>): String {
    val mode = params["mode"] ?: "0"
    val keyType = params["keyType"] ?: "002"
    val scheme = params["scheme"] ?: "U"
    val tmkFlag = params["tmkFlag"] ?: "0"
    return buildString {
        append(header)
        append("A0")
        append(mode)
        append(keyType)
        append(scheme)
        if (tmkFlag == "1") {
            append(";")
            append(tmkFlag)
            val tmk = params["tmk"] ?: ""
            append("U$tmk")
            append(params["exportScheme"] ?: "U")
        }
    }
}

private fun buildFKCommand(header: String, params: Map<String, String>): String {
    val keyType = params["keyType"] ?: "002"
    val scheme = params["scheme"] ?: "U"
    val numComponents = (params["numComponents"] ?: "2").toIntOrNull() ?: 2
    val lmkScheme = params["lmkScheme"] ?: "0"
    return buildString {
        append(header)
        append("FK")
        append(numComponents)
        append(keyType)
        append(scheme)
        append(lmkScheme)
        for (i in 1..numComponents) {
            append(params["component$i"] ?: "")
        }
    }
}

private fun buildLACommand(header: String, params: Map<String, String>): String {
    val index = params["index"] ?: "000"
    val data = params["data"] ?: ""
    val indexFlag = params["indexFlag"] ?: "K"
    return "${header}LA$indexFlag$index$data"
}

private fun buildLECommand(header: String, params: Map<String, String>): String {
    val index = params["index"] ?: "000"
    val indexFlag = params["indexFlag"] ?: "K"
    return "${header}LE$indexFlag$index"
}

private fun buildLDCommand(header: String, params: Map<String, String>): String {
    val index = params["index"] ?: "000"
    val indexFlag = params["indexFlag"] ?: "K"
    return "${header}LD$indexFlag$index"
}

private fun buildBWCommand(header: String, params: Map<String, String>): String {
    val keyType = params["keyType"] ?: "002"
    val srcScheme = params["sourceScheme"] ?: "U"
    val srcKey = params["sourceKey"] ?: ""
    val dstScheme = params["destScheme"] ?: "U"
    return "${header}BW$keyType$srcScheme$srcKey$dstScheme"
}

private fun buildBGCommand(header: String, params: Map<String, String>): String {
    val account = params["account"] ?: ""
    val pin = params["pin"] ?: ""
    return "${header}BG$account$pin"
}

private fun buildEICommand(header: String, params: Map<String, String>): String {
    val keyType = params["keyType"] ?: "1"
    val modulusLen = params["modulusLength"] ?: "2048"
    val encoding = params["encoding"] ?: "01"
    return "${header}EI$keyType${modulusLen}$encoding"
}

private fun buildDECommand(header: String, params: Map<String, String>): String {
    val pvk = params["pvk"] ?: ""
    val lmkPin = params["lmkPin"] ?: ""
    val minPinLen = params["minPinLength"] ?: "04"
    val account = params["account"] ?: ""
    val decTable = params["decTable"] ?: "0123456789012345"
    val userData = params["userData"] ?: "FFFFFFFFFFFN"
    return "${header}DE$pvk$lmkPin$minPinLen$account$decTable$userData"
}

private fun buildDGCommand(header: String, params: Map<String, String>): String {
    val pvkPair = params["pvkPair"] ?: ""
    val lmkPin = params["lmkPin"] ?: ""
    val account = params["account"] ?: ""
    val pvki = params["pvki"] ?: "0"
    return "${header}DG$pvkPair$lmkPin$account$pvki"
}

// ====================================================================================================
// Command Definitions
// ====================================================================================================

enum class CommandCategory(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val color: Color) {
    LMK("LMK", Icons.Default.Lock, Color(0xFF9C27B0)),
    KEY_GEN("Key Gen", Icons.Default.VpnKey, Color(0xFF2196F3)),
    KEY_MGMT("Key Mgmt", Icons.Default.ManageAccounts, Color(0xFF009688)),
    PIN("PIN", Icons.Default.Pin, Color(0xFFFF9800)),
    CRYPTO("Crypto", Icons.Default.Calculate, Color(0xFF4CAF50)),
    DIAG("Diagnostic", Icons.Default.HealthAndSafety, Color(0xFF607D8B))
}

data class CommandParam(
    val name: String,
    val description: String = "",
    val placeholder: String = "",
    val default: String = "",
    val required: Boolean = true,
    val multiline: Boolean = false,
    val options: List<Pair<String, String>> = emptyList()
)

data class SecureCommand(
    val code: String,
    val name: String,
    val description: String,
    val category: CommandCategory,
    val parameters: List<CommandParam> = emptyList(),
    val wireFormat: String = ""
)

val LMK_KEY_TYPE_OPTIONS = listOf(
    "000" to "000 — ZMK  (Zone Master Key)            LMK 14-15",
    "001" to "001 — ZPK  (Zone PIN Key)               LMK 14-15",
    "002" to "002 — PVK / TPK / TMK                   LMK 14-15",
    "003" to "003 — TAK  (Terminal Auth Key)           LMK 14-15",
    "008" to "008 — ZAK  (Zone Auth Key)               LMK 14-15",
    "009" to "009 — BDK  (Base Derivation Key)         LMK 14-15",
    "109" to "109 — MK-AC                              LMK 14-15 var 1",
    "209" to "209 — MK-SMI                             LMK 14-15 var 2",
    "309" to "309 — MK-SMC                             LMK 14-15 var 3",
    "409" to "409 — MK-DAC                             LMK 14-15 var 4",
    "509" to "509 — MK-DN                              LMK 14-15 var 5",
    "609" to "609 — BDK-2                              LMK 14-15 var 6",
    "709" to "709 — MK-CVC3                            LMK 14-15 var 7",
    "809" to "809 — BDK-3                              LMK 14-15 var 8",
    "909" to "909 — BDK-4                              LMK 14-15 var 9",
    "00A" to "00A — ZEK  (Zone Encryption Key)         LMK 14-15",
    "00B" to "00B — DEK  (Data Encryption Key)         LMK 14-15",
    "402" to "402 — CVK  (Card Verification Key)       LMK 14-15 var 4",
    "302" to "302 — IKEY (Initial Key / DUKPT)         LMK 14-15",
    "70D" to "70D — TPK-PCI                            LMK 14-15",
    "80D" to "80D — TMK-PCI                            LMK 14-15",
    "90D" to "90D — TKR                                LMK 14-15",
)

val SECURE_COMMANDS = listOf(
    // Diagnostics
    SecureCommand(
        code = "NC", name = "Diagnostic Test", category = CommandCategory.DIAG,
        description = "Tests HSM communication and verifies the LMK is loaded and valid",
        wireFormat = "HEADNC → HEADND 00"
    ),
    SecureCommand(
        code = "VR", name = "Version Info", category = CommandCategory.DIAG,
        description = "Returns the firmware version and serial number of the HSM",
        wireFormat = "HEADVR → HEADVS 00 [VERSION]"
    ),
    SecureCommand(
        code = "VT", name = "View LMK Table", category = CommandCategory.LMK,
        description = "Displays the KCVs of all loaded LMK pairs",
        wireFormat = "HEADVT → HEADVU 00 [KCV_TABLE]"
    ),
    // LMK Operations
    SecureCommand(
        code = "GK", name = "Generate LMK Component", category = CommandCategory.LMK,
        description = "Generates a new random LMK component that can be printed on a card and used to load the LMK",
        wireFormat = "HEADGK → HEADGL 00 [COMPONENT] [KCV]"
    ),
    SecureCommand(
        code = "GC", name = "Generate Key Component", category = CommandCategory.KEY_GEN,
        description = "Generates N random key components for the key ceremony. Each component is independent — XOR all with FK to form the working key.",
        wireFormat = "HEAD GC [KEY_TYPE_3H] [SCHEME_1A] → HEAD GD 00 [COMPONENT_32H] [KCV_6H]  (executed N times)",
        parameters = listOf(
            CommandParam(
                name = "numComponents",
                description = "How many components to generate (2 or 3 custodians)",
                placeholder = "2 or 3",
                default = "2",
                required = true
            ),
            CommandParam(
                name = "keyType",
                description = "Key type code (3 hex digits)",
                placeholder = "001=ZPK  002=TPK  008=ZMK  009=BDK",
                default = "001",
                required = true
            ),
            CommandParam(
                name = "scheme",
                description = "Key scheme / length",
                placeholder = "U=double-length TDES  X=triple-length  T=single",
                default = "U",
                required = true
            )
        )
    ),
    // Key Generation
    SecureCommand(
        code = "A0", name = "Generate Key", category = CommandCategory.KEY_GEN,
        description = "Generates a symmetric key (DES/TDES) encrypted under the LMK",
        wireFormat = "HEAD A0 [MODE] [KEY_TYPE] [SCHEME] [;] [TMK_FLAG] [TMK] [EXPORT_SCHEME]",
        parameters = listOf(
            CommandParam("mode", "Generation mode", "0 = generate under TMK, 1 = generate under LMK only", "0"),
            CommandParam("keyType", "Key type code (3H)", "002=TPK, 001=ZPK, 009=BDK", "002"),
            CommandParam("scheme", "LMK key scheme", "U=double-length TDES, X=triple-length", "U"),
            CommandParam("tmkFlag", "Export under TMK?", "0=no export, 1=export under TMK", "0"),
            CommandParam("tmk", "TMK (32H) if exporting", "", "", false)
        )
    ),
    // Form Key from Components (key ceremony)
    SecureCommand(
        code = "FK", name = "Form Key from Components", category = CommandCategory.KEY_GEN,
        description = "XORs 2 or 3 key components to form a working key under the LMK (key ceremony)",
        wireFormat = "HEAD FK [N_COMPONENTS] [KEY_TYPE] [SCHEME] [LMK_SCHEME] [COMP1] [COMP2] [COMP3?]",
        parameters = listOf(
            CommandParam("numComponents", "Number of components (2 or 3)", "2 or 3", "2"),
            CommandParam("keyType", "Key type code (3H)", "002=TPK, 001=ZPK", "001"),
            CommandParam("scheme", "Key scheme", "U=TDES double-length", "U"),
            CommandParam("lmkScheme", "LMK scheme flag", "0=variant LMK", "0"),
            CommandParam("component1", "Component 1 (16H or 32H hex)", "", "", true),
            CommandParam("component2", "Component 2 (16H or 32H hex)", "", "", true),
            CommandParam("component3", "Component 3 (optional)", "", "", false)
        )
    ),
    // User Storage
    SecureCommand(
        code = "LA", name = "Load Data to User Storage", category = CommandCategory.KEY_MGMT,
        description = "Stores a key or data block in the HSM's user storage area at a given index",
        wireFormat = "HEAD LA [INDEX_FLAG] [INDEX_3H] [DATA]",
        parameters = listOf(
            CommandParam("indexFlag", "Index flag (K=key reference)", "K", "K"),
            CommandParam("index", "Storage index (3H hex)", "000–FFF", "000"),
            CommandParam("data", "Data to store (hex or key)", "", "", true)
        )
    ),
    SecureCommand(
        code = "LE", name = "Read from User Storage", category = CommandCategory.KEY_MGMT,
        description = "Reads data from the HSM's user storage at a given index",
        wireFormat = "HEAD LE [INDEX_FLAG] [INDEX_3H]",
        parameters = listOf(
            CommandParam("indexFlag", "Index flag", "K", "K"),
            CommandParam("index", "Storage index (3H hex)", "000–FFF", "000")
        )
    ),
    SecureCommand(
        code = "LD", name = "Delete from User Storage", category = CommandCategory.KEY_MGMT,
        description = "Deletes data from HSM user storage at a given index",
        wireFormat = "HEAD LD [INDEX_FLAG] [INDEX_3H]",
        parameters = listOf(
            CommandParam("indexFlag", "Index flag", "K", "K"),
            CommandParam("index", "Storage index (3H hex)", "000–FFF", "000")
        )
    ),
    // LMK Migration
    SecureCommand(
        code = "BW", name = "Translate Key (LMK Migration)", category = CommandCategory.LMK,
        description = "Translates a key from the old LMK to a new LMK (key migration)",
        wireFormat = "HEAD BW [KEY_TYPE] [SRC_SCHEME+SRC_KEY] [DEST_SCHEME]",
        parameters = listOf(
            CommandParam("keyType", "Key type code", "002=TPK, 001=ZPK", "002"),
            CommandParam("sourceScheme", "Source key scheme", "U=TDES", "U"),
            CommandParam("sourceKey", "Key under old LMK (32H)", "", "", true),
            CommandParam("destScheme", "Destination scheme", "U=TDES", "U")
        )
    ),
    SecureCommand(
        code = "BG", name = "Translate PIN (LMK Migration)", category = CommandCategory.PIN,
        description = "Re-encrypts a PIN block from the old LMK to the new LMK",
        wireFormat = "HEAD BG [ACCOUNT_12N] [LMK_PIN]",
        parameters = listOf(
            CommandParam("account", "Account number (12N)", "12 rightmost digits excl. check", "", true),
            CommandParam("pin", "LMK-encrypted PIN", "", "", true)
        )
    ),
    // Decrypt Key under LMK (console utility)
    SecureCommand(
        code = "DK", name = "Decrypt Key Under LMK", category = CommandCategory.KEY_MGMT,
        description = "Decrypts a key encrypted under the LMK to reveal the plain (clear) key. " +
                "This is a console-only operation — the clear key is only shown on the secure console.",
        wireFormat = "Console-only (not a wire command)",
        parameters = listOf(
            CommandParam(
                name = "encryptedKey",
                description = "Key encrypted under LMK (hex, with optional U/X/T prefix)",
                placeholder = "U1234567890ABCDEF1234567890ABCDEF",
                default = "",
                required = true
            ),
            CommandParam(
                name = "keyType",
                description = "Key type — determines which LMK pair and variant to use for decryption",
                default = "001",
                required = true,
                options = LMK_KEY_TYPE_OPTIONS
            )
        )
    ),
    // RSA
    SecureCommand(
        code = "EI", name = "Generate RSA Key Pair", category = CommandCategory.CRYPTO,
        description = "Generates an RSA key pair; private key is encrypted under the LMK",
        wireFormat = "HEAD EI [TYPE_1N] [MODULUS_4N] [ENCODING_2N]",
        parameters = listOf(
            CommandParam("keyType", "Key type", "1=key management, 2=signature", "1"),
            CommandParam("modulusLength", "Modulus length in bits", "1024, 2048, 4096", "2048"),
            CommandParam("encoding", "Public key encoding", "01=DER ASN.1", "01")
        )
    ),
    // IBM PIN operations (offline)
    SecureCommand(
        code = "DE", name = "Generate IBM PIN Offset", category = CommandCategory.PIN,
        description = "Generates the IBM 3624 PIN offset between a natural PIN and customer-selected PIN",
        wireFormat = "HEAD DE [PVK_16H] [LMK_PIN] [MIN_LEN_2N] [ACCOUNT_12N] [DEC_TABLE_16H] [USER_DATA_12A]",
        parameters = listOf(
            CommandParam("pvk", "PVK under LMK (16H)", "Single-length DES PVK", "", true),
            CommandParam("lmkPin", "LMK-encrypted PIN", "", "", true),
            CommandParam("minPinLength", "Minimum PIN length", "04–12", "04"),
            CommandParam("account", "Account number (12N)", "12 rightmost excl. check digit", "", true),
            CommandParam("decTable", "Decimalization table (16H)", "", "0123456789012345"),
            CommandParam("userData", "User PIN validation data (12A)", "'N' = last 5 PAN digits", "FFFFFFFFFFFN")
        )
    ),
    SecureCommand(
        code = "DG", name = "Generate VISA PVV", category = CommandCategory.PIN,
        description = "Generates the VISA PIN Verification Value (PVV) from an LMK-encrypted PIN",
        wireFormat = "HEAD DG [PVK_PAIR_32H] [LMK_PIN] [ACCOUNT_12N] [PVKI]",
        parameters = listOf(
            CommandParam("pvkPair", "PVK pair under LMK (32H)", "Two single-length DES keys", "", true),
            CommandParam("lmkPin", "LMK-encrypted PIN", "", "", true),
            CommandParam("account", "Account number (12N)", "", "", true),
            CommandParam("pvki", "PVKI (0–6)", "", "0")
        )
    )
)
