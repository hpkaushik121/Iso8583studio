package `in`.aicortex.iso8583studio.ui.screens.Emv

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.LogPanelWithAutoScroll
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// --- Data Model for EMV Tag ---
data class EmvTag(
    val tag: String,
    val name: String,
    val kernel: String,
    val source: String,
    val format: String,
    val template: String,
    val length: String,
    val description: String
)

// --- Service to hold and filter tag data ---
object EmvTagService {
    private val allTags = listOf(
        EmvTag("42", "Issuer Identification Number (IIN)", "Generic", "ICC", "n 6", "'BF0C' or '73'", "3 [B]", "The number that identifies the major industry and the card issuer and that forms the first part of the Primary Account Number (PAN)"),
        EmvTag("4F", "Application Identifier (ADF Name)", "VISA", "ICC", "binary 40-128", "'61'", "5-16 [B]", "The ADF Name identifies the application as described in [ISO 7816-5]. The AID is made up of the Registered Application Provider Identifier (RID) and the Proprietary Identifier Extension (PIX)."),
        EmvTag("50", "Application Label", "MasterCard", "ICC", "ans with the special character limited to space", "'61' or 'A5'", "1-16 [B]", "Mnemonic associated with the AID according to ISO/IEC 7816-5"),
        EmvTag("50", "Application Label", "VISA", "ICC", "ans 1-16 (special characters limited to spaces)", "N/A", "1-16 [B]", "Mnemonic associated with AID according to [ISO 7816-5]. Used in application selection. Application Label is optional in the File Control Information (FCI) of an Application Definition File (ADF) and optional in an ADF directory entry."),
        EmvTag("50", "Application Label", "JCB", "ICC", "ans 1-16 (special characters limited to spaces)", "N/A", "1-16 [B]", "Mnemonic associated with the AID according to ISO/IEC 7816-5 (with the special character limited to space)."),
        EmvTag("52", "Command to perform", "Generic", "ICC", "H", "N/A", "Variable", ""),
        EmvTag("56", "Track 1 Data", "MasterCard", "ICC", "ans", "N/A", "Variable", "Track 1 Data contains the data objects of the track 1 according to [ISO/IEC 7813] Structure B, excluding start sentinel, end sentinel and LRC. The Track 1 Data may be present in the file read using the READ RECORD command during a mag-stripe mode transaction."),
        EmvTag("57", "Track 2 Equivalent Data", "MasterCard", "ICC", "binary", "'70' or '77'", "Variable", "Contains the data objects of the track 2, in accordance with [ISO/IEC 7813], excluding start sentinel, end sentinel, and LRC."),
        EmvTag("5A", "Application Primary Account Number (PAN)", "MasterCard", "ICC", "cn variable up to 19", "'70' or '77'", "Variable", "Valid cardholder account number")
        // Add all other tags here...
    )

    fun searchTags(query: String, kernel: String): List<EmvTag> {
        val lowerCaseQuery = query.lowercase().trim()
        if (lowerCaseQuery.isEmpty()) {
            // Return all if search query is empty, respecting the kernel filter
            return allTags.filter { kernel == "All" || it.kernel.equals(kernel, ignoreCase = true) }
        }

        return allTags.filter { tag ->
            (tag.tag.lowercase().contains(lowerCaseQuery) ||
                    tag.name.lowercase().contains(lowerCaseQuery) ||
                    tag.description.lowercase().contains(lowerCaseQuery)) &&
                    (kernel == "All" || tag.kernel.equals(kernel, ignoreCase = true))
        }
    }

    fun getKernels(): List<String> {
        return listOf("All") + allTags.map { it.kernel }.distinct().sorted()
    }
}

// --- Log Manager ---
object TagDictionaryLogManager {
    private val _logEntries = mutableStateListOf<LogEntry>()
    val logEntries: SnapshotStateList<LogEntry> get() = _logEntries

    fun clearLogs() {
        _logEntries.clear()
    }

    fun logMessage(message: String, type: LogType = LogType.INFO) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
        _logEntries.add(0, LogEntry(timestamp = timestamp, type = type, message = message, details = ""))
    }

    fun logTag(tag: EmvTag) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
        val details = """
            Kernel: ${tag.kernel}
            Source: ${tag.source}
            Format: ${tag.format}
            Template: ${tag.template}
            Length: ${tag.length}
            Description: ${tag.description}
        """.trimIndent()
        _logEntries.add(0, LogEntry(timestamp = timestamp, type = LogType.TRANSACTION, message =  "${tag.name} (${tag.tag})",details =  details))
    }
}

// --- Main Screen ---
@Composable
fun EmvTagDictionaryScreen(
    
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "EMV Tag Dictionary",
                onBackClick = onBack,
            )
        },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Row(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Left Panel: Search and Filter
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                TagSearchCard()
            }
            // Right Panel: Results
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Panel {
                    LogPanelWithAutoScroll(
                        onClearClick = { TagDictionaryLogManager.clearLogs() },
                        logEntries = TagDictionaryLogManager.logEntries
                    )
                }
            }
        }
    }
}

@Composable
private fun TagSearchCard() {
    var searchQuery by remember { mutableStateOf("") }
    val kernels = remember { EmvTagService.getKernels() }
    var selectedKernel by remember { mutableStateOf(kernels.first()) }
    val coroutineScope = rememberCoroutineScope()

    ModernCryptoCard(
        title = "Search EMV Tags",
        subtitle = "Find tags by name, tag, or description",
        icon = Icons.Default.Search
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by Tag, Name, or Description...") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                }
            )

            ModernDropdownField(
                label = "Filter by Kernel",
                value = selectedKernel,
                options = kernels,
                onSelectionChanged = { index -> selectedKernel = kernels[index] }
            )

            Button(
                onClick = {
                    coroutineScope.launch {
                        TagDictionaryLogManager.clearLogs()
                        val results = EmvTagService.searchTags(searchQuery, selectedKernel)
                        if (results.isNotEmpty()) {
                            results.forEach { TagDictionaryLogManager.logTag(it) }
                        } else {
                            TagDictionaryLogManager.logMessage("No tags found for your criteria.", LogType.ERROR)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search Tags")
                Spacer(Modifier.width(8.dp))
                Text("Search Tags")
            }
        }
    }
}

// --- SHARED UI COMPONENTS ---

@Composable
private fun ModernCryptoCard(title: String, subtitle: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp, shape = RoundedCornerShape(12.dp), backgroundColor = MaterialTheme.colors.surface) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.h6, fontWeight = FontWeight.SemiBold)
                    Text(text = subtitle, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                }
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun ModernDropdownField(label: String, value: String, options: List<String>, onSelectionChanged: (Int) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value, onValueChange = {}, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), readOnly = true,
            trailingIcon = { Icon(imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.clickable { expanded = !expanded }) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.wrapContentWidth()) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(onClick = {
                    onSelectionChanged(index)
                    expanded = false
                }) {
                    Text(text = option, style = MaterialTheme.typography.body2)
                }
            }
        }
    }
}
