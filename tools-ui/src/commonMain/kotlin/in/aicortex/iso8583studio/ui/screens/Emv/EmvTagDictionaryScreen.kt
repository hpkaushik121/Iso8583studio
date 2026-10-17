package `in`.aicortex.iso8583studio.ui.screens.Emv

import ai.cortex.core.EmvTags

import `in`.aicortex.iso8583studio.platform.nowLogTimestamp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.components.LogPanelWithAutoScroll
import kotlinx.coroutines.launch
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
    private val allTags: List<EmvTag> = EmvTags.all.map { EmvTag(it.tag, it.name, it.kernel, it.source, it.format, it.template, it.length, it.description) }

    fun searchTags(query: String, kernel: String): List<EmvTag> {
        val lowerCaseQuery = query.lowercase().trim()
        return allTags.filter { tag ->
            (kernel == "All" || tag.kernel.equals(kernel, ignoreCase = true)) &&
                (lowerCaseQuery.isEmpty() || tag.tag.lowercase().contains(lowerCaseQuery) || tag.name.lowercase().contains(lowerCaseQuery) || tag.description.lowercase().contains(lowerCaseQuery))
        }
    }

    fun getKernels(): List<String> = listOf("All") + EmvTags.kernels()
}


// --- Log Manager ---
object TagDictionaryLogManager {
    private val _logEntries = mutableStateListOf<LogEntry>()
    val logEntries: SnapshotStateList<LogEntry> get() = _logEntries

    fun clearLogs() {
        _logEntries.clear()
    }

    fun logMessage(message: String, type: LogType = LogType.INFO) {
        val timestamp = nowLogTimestamp()
        _logEntries.add(LogEntry(timestamp = timestamp, type = type, message = message, details = ""))
    }

    fun logTag(tag: EmvTag) {
        val timestamp = nowLogTimestamp()
        val details = """
            Kernel: ${tag.kernel}
            Source: ${tag.source}
            Format: ${tag.format}
            Template: ${tag.template}
            Length: ${tag.length}
            Description: ${tag.description}
        """.trimIndent()
        _logEntries.add(LogEntry(timestamp = timestamp, type = LogType.TRANSACTION, message =  "${tag.name} (${tag.tag})",details =  details))
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
            FixedOutlinedTextField(
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
private fun ModernDropdownField(label: String, value: String, options: List<String>, onSelectionChanged: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var textFieldWidth by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    Box(modifier = Modifier.onGloballyPositioned { textFieldWidth = it.size.width }) {
        FixedOutlinedTextField(
            value = value, onValueChange = {}, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), readOnly = true,
            trailingIcon = { Icon(imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = null) },
        )
        Box(modifier = Modifier.matchParentSize().clickable { expanded = !expanded })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.width(with(density) { textFieldWidth.toDp() }).heightIn(max = 300.dp)) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(onClick = { onSelectionChanged(index); expanded = false }) {
                    Text(text = option, style = MaterialTheme.typography.body2)
                }
            }
        }
    }
}
