package `in`.aicortex.iso8583studio.ui.screens.hsmSimulator

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

    Row(modifier = Modifier.fillMaxSize()) {
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
                                val rawCmd = buildHsmCommand(cmd, params, hsm)
                                val result = hsm.executeSecureCommand(rawCmd)
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
        "GC" -> "${header}GC"
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
    val multiline: Boolean = false
)

data class SecureCommand(
    val code: String,
    val name: String,
    val description: String,
    val category: CommandCategory,
    val parameters: List<CommandParam> = emptyList(),
    val wireFormat: String = ""
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
        description = "Generates a random key component for the key ceremony",
        wireFormat = "HEADGC [KEY_TYPE] [SCHEME] → HEADGD 00 [COMPONENT] [KCV]"
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
