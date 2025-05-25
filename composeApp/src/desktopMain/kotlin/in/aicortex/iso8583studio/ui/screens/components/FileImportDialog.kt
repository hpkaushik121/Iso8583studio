package `in`.aicortex.iso8583studio.ui.screens.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import `in`.aicortex.iso8583studio.data.model.CodeFormat
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.domain.utils.FormatMappingConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Import validation states
 */
sealed class ImportValidationState {
    object Idle : ImportValidationState()
    object Validating : ImportValidationState()
    data class Valid(val config: FormatMappingConfig, val preview: String) : ImportValidationState()
    data class Invalid(val error: String) : ImportValidationState()
}

/**
 * Import configuration data
 */
data class ImportConfig(
    val selectedFile: File? = null,
    val detectedFormat: CodeFormat? = null,
    val validationState: ImportValidationState = ImportValidationState.Idle,
    val overwriteExisting: Boolean = false,
    val backupCurrent: Boolean = true
)

/**
 * File Import Dialog Component
 */
@Composable
fun FileImportDialog(
    gatewayConfig: GatewayConfig,
    onDismiss: () -> Unit,
    onImportComplete: (FormatMappingConfig) -> Unit
) {
    var importConfig by remember { mutableStateOf(ImportConfig()) }
    var isImporting by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Animation states
    val dialogScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .scale(dialogScale)
                .width(800.dp)
                .heightIn(max = 700.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                ImportDialogHeader()

                // File Selection Section
                FileSelectionSection(
                    selectedFile = importConfig.selectedFile,
                    onFileSelected = { file ->
                        importConfig = importConfig.copy(
                            selectedFile = file,
                            detectedFormat = detectFileFormat(file),
                            validationState = ImportValidationState.Idle
                        )
                    }
                )

                // Validation Section
                if (importConfig.selectedFile != null) {
                    ValidationSection(
                        importConfig = importConfig,
                        onValidate = { file ->
                            coroutineScope.launch {
                                importConfig = importConfig.copy(
                                    validationState = ImportValidationState.Validating
                                )

                                val validationResult = validateImportFile(file)
                                importConfig = importConfig.copy(
                                    validationState = validationResult
                                )
                            }
                        }
                    )
                }

                // Import Options
                if (importConfig.validationState is ImportValidationState.Valid) {
                    ImportOptionsSection(
                        importConfig = importConfig,
                        onConfigChange = { importConfig = it },
                        hasExistingConfig = gatewayConfig.formatMappingConfigSource.fieldMappings.isNotEmpty()
                    )
                }

                // Preview Section
                if (importConfig.validationState is ImportValidationState.Valid) {
                    PreviewSection(
                        preview = (importConfig.validationState as ImportValidationState.Valid).preview
                    )
                }

                // Action Buttons
                ActionButtonsSection(
                    importConfig = importConfig,
                    isImporting = isImporting,
                    onImport = {
                        if (importConfig.validationState is ImportValidationState.Valid) {
                            coroutineScope.launch {
                                isImporting = true
                                try {
                                    performImport(
                                        config = (importConfig.validationState as ImportValidationState.Valid).config,
                                        gatewayConfig = gatewayConfig,
                                        importConfig = importConfig
                                    )
                                    onImportComplete((importConfig.validationState as ImportValidationState.Valid).config)
                                } catch (e: Exception) {
                                    // Handle import error
                                    importConfig = importConfig.copy(
                                        validationState = ImportValidationState.Invalid(
                                            "Import failed: ${e.message}"
                                        )
                                    )
                                } finally {
                                    isImporting = false
                                }
                            }
                        }
                    },
                    onCancel = onDismiss
                )
            }
        }
    }
}

@Composable
private fun ImportDialogHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.FileUpload,
            contentDescription = null,
            tint = MaterialTheme.colors.primary,
            modifier = Modifier.size(32.dp)
        )
        Column {
            Text(
                text = "Import Configuration",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary
            )
            Text(
                text = "Import ISO8583 format mapping configuration",
                fontSize = 14.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun FileSelectionSection(
    selectedFile: File?,
    onFileSelected: (File) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Select Configuration File",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // File drop zone
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .border(
                        width = 2.dp,
                        color = if (selectedFile != null)
                            MaterialTheme.colors.primary
                        else
                            MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .background(
                        color = if (selectedFile != null)
                            MaterialTheme.colors.primary.copy(alpha = 0.1f)
                        else
                            MaterialTheme.colors.surface,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        val selectedFile = selectImportFile()
                        if (selectedFile != null) {
                            onFileSelected(selectedFile)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedFile != null) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = selectedFile.name,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.primary
                        )
                        Text(
                            text = "File size: ${formatFileSize(selectedFile.length())}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    } else {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Click to select file",
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Supported: .yaml, .yml",
                            fontSize = 12.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ValidationSection(
    importConfig: ImportConfig,
    onValidate: (File) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "File Validation",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                if (importConfig.validationState == ImportValidationState.Idle) {
                    Button(
                        onClick = {
                            importConfig.selectedFile?.let { onValidate(it) }
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Validate")
                    }
                }
            }

            // Validation Status
            when (val state = importConfig.validationState) {
                is ImportValidationState.Idle -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colors.primary.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Click validate to check file format and structure",
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                is ImportValidationState.Validating -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Text("Validating file structure...")
                    }
                }

                is ImportValidationState.Valid -> {
                    Card(
                        backgroundColor = Color.Green.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.Green.copy(alpha = 0.8f)
                            )
                            Column {
                                Text(
                                    text = "File is valid and ready to import",
                                    color = Color.Green.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Medium
                                )
                                importConfig.detectedFormat?.let { format ->
                                    Text(
                                        text = "Detected format: ${format.displayName}",
                                        fontSize = 12.sp,
                                        color = Color.Green.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }

                is ImportValidationState.Invalid -> {
                    Card(
                        backgroundColor = Color.Red.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = Color.Red.copy(alpha = 0.8f)
                            )
                            Column {
                                Text(
                                    text = "Validation failed",
                                    color = Color.Red.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = state.error,
                                    fontSize = 12.sp,
                                    color = Color.Red.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportOptionsSection(
    importConfig: ImportConfig,
    onConfigChange: (ImportConfig) -> Unit,
    hasExistingConfig: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Import Options",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            if (hasExistingConfig) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = importConfig.overwriteExisting,
                        onCheckedChange = {
                            onConfigChange(importConfig.copy(overwriteExisting = it))
                        }
                    )
                    Text("Overwrite existing configuration")
                }

                if (importConfig.overwriteExisting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp)
                    ) {
                        Checkbox(
                            checked = importConfig.backupCurrent,
                            onCheckedChange = {
                                onConfigChange(importConfig.copy(backupCurrent = it))
                            }
                        )
                        Text("Create backup of current configuration")
                    }
                }
            }

            if (hasExistingConfig && !importConfig.overwriteExisting) {
                Card(
                    backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colors.error.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "Existing configuration found. Enable overwrite to proceed.",
                            color = MaterialTheme.colors.error.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewSection(preview: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Configuration Preview",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            TextField(
                value = preview,
                onValueChange = { },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                ),
                maxLines = Int.MAX_VALUE,
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Gray.copy(alpha = 0.05f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
private fun ActionButtonsSection(
    importConfig: ImportConfig,
    isImporting: Boolean,
    onImport: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Cancel")
        }

        Button(
            onClick = onImport,
            modifier = Modifier.weight(1f),
            enabled = (!isImporting &&
                    importConfig.validationState is ImportValidationState.Valid &&
                    importConfig.overwriteExisting ), // Adjust logic based on requirements
            shape = RoundedCornerShape(8.dp)
        ) {
            if (isImporting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colors.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Importing...")
            } else {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import")
            }
        }
    }
}

// Helper Functions

private fun selectImportFile(): File? {
    return try {
        val fileChooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.FILES_ONLY
            dialogTitle = "Select Configuration File"
            currentDirectory = File(System.getProperty("user.home"))

            // Add file filters
            addChoosableFileFilter(FileNameExtensionFilter("YAML files", "yaml", "yml"))
            addChoosableFileFilter(FileNameExtensionFilter("JSON files", "json"))
            addChoosableFileFilter(FileNameExtensionFilter("All supported", "yaml", "yml", "json"))
        }

        val result = fileChooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            fileChooser.selectedFile
        } else {
            null
        }
    } catch (e: Exception) {
        println("Error selecting file: ${e.message}")
        null
    }
}

private fun detectFileFormat(file: File): CodeFormat? {
    return when (file.extension.lowercase()) {
        "yaml", "yml" -> CodeFormat.JSON // YAML configs typically map to JSON format
        "json" -> CodeFormat.JSON
        else -> null
    }
}

private suspend fun validateImportFile(file: File): ImportValidationState {
    return withContext(Dispatchers.IO) {
        try {
            val content = file.readText()
            val mapper = when (file.extension.lowercase()) {
                "yaml", "yml" -> ObjectMapper(YAMLFactory()).registerModule(kotlinModule())
                "json" -> ObjectMapper().registerModule(kotlinModule())
                else -> throw IllegalArgumentException("Unsupported file format")
            }

            // Try to parse as FormatMappingConfig
            val config = mapper.readValue<FormatMappingConfig>(content)

            // Validate the structure
            validateConfigStructure(config)

            // Generate preview
            val preview = generateConfigPreview(config)

            ImportValidationState.Valid(config, preview)
        } catch (e: Exception) {
            ImportValidationState.Invalid("Invalid configuration file: ${e.message}")
        }
    }
}

private fun validateConfigStructure(config: FormatMappingConfig) {
    // Add validation logic here
    if (config.formatType == null) {
        throw IllegalArgumentException("Missing formatType")
    }

    if (config.fieldMappings.isEmpty()) {
        throw IllegalArgumentException("No field mappings found")
    }

    // Add more validation as needed
}

private fun generateConfigPreview(config: FormatMappingConfig): String {
    return buildString {
        appendLine("Format Type: ${config.formatType}")
        appendLine("Field Mappings: ${config.fieldMappings.size} fields")
        appendLine()
        appendLine("Sample Field Mappings:")
        config.fieldMappings.entries.take(5).forEach { (field, mapping) ->
            appendLine("  Field $field: ${mapping}")
        }
        if (config.fieldMappings.size > 5) {
            appendLine("  ... and ${config.fieldMappings.size - 5} more fields")
        }
    }
}

private suspend fun performImport(
    config: FormatMappingConfig,
    gatewayConfig: GatewayConfig,
    importConfig: ImportConfig
) {
    withContext(Dispatchers.IO) {
        // Create backup if requested
        if (importConfig.backupCurrent) {
            createConfigBackup(gatewayConfig.formatMappingConfigSource)
        }

        // Set the new configuration
        gatewayConfig.formatMappingConfigSource = config

        // Save the updated gateway config
        // This would typically involve saving to your data store
        saveGatewayConfig(gatewayConfig)
    }
}

private fun createConfigBackup(config: FormatMappingConfig) {
    // Implementation for creating backup
    val backupFile = File(System.getProperty("user.home"), "iso8583_config_backup_${System.currentTimeMillis()}.yaml")
    // Save backup logic here
}

private fun saveGatewayConfig(gatewayConfig: GatewayConfig) {
    // Implementation for saving gateway config
    // This would typically save to your preferred data store
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}
