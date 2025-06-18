package `in`.aicortex.iso8583studio.ui.screens.config.apduSimulator

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

// --- DATA MODELS ---

enum class ApplicationStatus(val displayName: String, val color: Color, val icon: ImageVector) {
    ACTIVE("Active", Color(0xFF197A3E), Icons.Outlined.PlayArrow),
    INACTIVE("Inactive", Color(0xFF795548), Icons.Outlined.Pause),
    TESTING("Testing", Color(0xFFED6C02), Icons.Outlined.BugReport),
    BLOCKED("Blocked", Color(0xFFD32F2F), Icons.Outlined.Block)
}

enum class ApplicationType(val displayName: String, val color: Color, val icon: ImageVector) {
    PAYMENT("Payment", Color(0xFF0B6BCB), Icons.Outlined.Payment),
    LOYALTY("Loyalty", Color(0xFF6750A4), Icons.Outlined.Stars),
    IDENTITY("Identity", Color(0xFF197A3E), Icons.Outlined.Badge),
    TRANSPORT("Transport", Color(0xFFED6C02), Icons.Outlined.Train),
    ACCESS("Access Control", Color(0xFFD32F2F), Icons.Outlined.Key),
    CUSTOM("Custom", Color(0xFF795548), Icons.Outlined.Extension)
}

data class ApplicationCapabilities(
    val contactInterface: Boolean = true,
    val contactlessInterface: Boolean = true,
    val magneticStripe: Boolean = false,
    val pinSupport: Boolean = true,
    val biometricSupport: Boolean = false,
    val offlineAuth: Boolean = true,
    val onlineAuth: Boolean = true,
    val cashback: Boolean = false,
    val refunds: Boolean = true
)

data class ProcessingOptions(
    val sda: Boolean = true, // Static Data Authentication
    val dda: Boolean = true, // Dynamic Data Authentication
    val cda: Boolean = false, // Combined Dynamic Application Data Authentication
    val pinBypass: Boolean = false,
    val onlinePinRequired: Boolean = false,
    val issuerAuthenticationRequired: Boolean = true,
    val terminalRiskManagement: Boolean = true
)

data class ApplicationUsageControl(
    val domesticTransactions: Boolean = true,
    val internationalTransactions: Boolean = true,
    val domesticAtm: Boolean = true,
    val internationalAtm: Boolean = true,
    val domesticCashback: Boolean = false,
    val internationalCashback: Boolean = false,
    val domesticGoods: Boolean = true,
    val internationalGoods: Boolean = true,
    val domesticServices: Boolean = true,
    val internationalServices: Boolean = true
)

data class FileReference(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val fileId: String,
    val fileType: String,
    val description: String,
    val isRequired: Boolean = true
)

data class EmvApplication(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val aid: String,
    val label: String,
    val priority: Int,
    val version: String,
    val type: ApplicationType,
    val status: ApplicationStatus,
    val capabilities: ApplicationCapabilities = ApplicationCapabilities(),
    val processingOptions: ProcessingOptions = ProcessingOptions(),
    val usageControl: ApplicationUsageControl = ApplicationUsageControl(),
    val fileReferences: List<FileReference> = emptyList(),
    val createdDate: String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
    val lastModified: String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
)

// --- SAMPLE DATA ---

fun getSampleApplications(): List<EmvApplication> = listOf(
    EmvApplication(
        name = "Visa Credit",
        aid = "A0000000031010",
        label = "VISA CREDIT",
        priority = 1,
        version = "1.0",
        type = ApplicationType.PAYMENT,
        status = ApplicationStatus.ACTIVE,
        capabilities = ApplicationCapabilities(
            contactInterface = true,
            contactlessInterface = true,
            pinSupport = true,
            offlineAuth = true,
            onlineAuth = true
        ),
        fileReferences = listOf(
            FileReference(name = "EF.PPSE", fileId = "2PAY.SYS.DDF01", fileType = "Directory", description = "Payment System Environment"),
            FileReference(name = "EF.DIR", fileId = "2F00", fileType = "Directory", description = "Master File Directory")
        )
    ),
    EmvApplication(
        name = "Mastercard Debit",
        aid = "A0000000041010",
        label = "MASTERCARD DEBIT",
        priority = 2,
        version = "2.1",
        type = ApplicationType.PAYMENT,
        status = ApplicationStatus.ACTIVE,
        capabilities = ApplicationCapabilities(
            contactInterface = true,
            contactlessInterface = true,
            magneticStripe = true,
            pinSupport = true,
            cashback = true
        ),
        fileReferences = listOf(
            FileReference(name = "EF.CDOL1", fileId = "8C", fileType = "Data Object", description = "Card Risk Management Data Object List 1"),
            FileReference(name = "EF.CDOL2", fileId = "8D", fileType = "Data Object", description = "Card Risk Management Data Object List 2")
        )
    ),
    EmvApplication(
        name = "Loyalty Program",
        aid = "A0000001234567",
        label = "STORE LOYALTY",
        priority = 5,
        version = "1.5",
        type = ApplicationType.LOYALTY,
        status = ApplicationStatus.TESTING,
        capabilities = ApplicationCapabilities(
            contactInterface = false,
            contactlessInterface = true,
            pinSupport = false,
            offlineAuth = true,
            onlineAuth = false
        )
    ),
    EmvApplication(
        name = "Transit Pass",
        aid = "A0000002345678",
        label = "CITY TRANSIT",
        priority = 3,
        version = "3.0",
        type = ApplicationType.TRANSPORT,
        status = ApplicationStatus.INACTIVE,
        capabilities = ApplicationCapabilities(
            contactInterface = false,
            contactlessInterface = true,
            pinSupport = false,
            offlineAuth = true,
            onlineAuth = false
        )
    )
)

fun getApplicationTemplates(): List<EmvApplication> = listOf(
    EmvApplication(
        name = "Visa Payment Template",
        aid = "A0000000031010",
        label = "VISA",
        priority = 1,
        version = "1.0",
        type = ApplicationType.PAYMENT,
        status = ApplicationStatus.ACTIVE,
        capabilities = ApplicationCapabilities(
            contactInterface = true,
            contactlessInterface = true,
            pinSupport = true,
            offlineAuth = true,
            onlineAuth = true
        )
    ),
    EmvApplication(
        name = "Contactless Only Template",
        aid = "A0000000000000",
        label = "CONTACTLESS APP",
        priority = 1,
        version = "1.0",
        type = ApplicationType.CUSTOM,
        status = ApplicationStatus.ACTIVE,
        capabilities = ApplicationCapabilities(
            contactInterface = false,
            contactlessInterface = true,
            pinSupport = false,
            offlineAuth = true,
            onlineAuth = false
        )
    ),
    EmvApplication(
        name = "Basic Loyalty Template",
        aid = "A0000000000001",
        label = "LOYALTY",
        priority = 2,
        version = "1.0",
        type = ApplicationType.LOYALTY,
        status = ApplicationStatus.ACTIVE,
        capabilities = ApplicationCapabilities(
            contactInterface = false,
            contactlessInterface = true,
            pinSupport = false,
            offlineAuth = true,
            onlineAuth = true
        )
    )
)

// --- MAIN AID/APDU SIMULATOR TAB ---

@Composable
fun ApplicationAidTab() {
    var applications by remember { mutableStateOf(getSampleApplications()) }
    var selectedApplication by remember { mutableStateOf<EmvApplication?>(null) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }

    fun updateApplication(updatedApp: EmvApplication) {
        applications = applications.map { if (it.id == updatedApp.id) updatedApp else it }
        selectedApplication = updatedApp
        hasUnsavedChanges = true
    }

    fun addApplication(newApp: EmvApplication) {
        applications = applications + newApp
        selectedApplication = newApp
        hasUnsavedChanges = true
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Header with Statistics
        AidApduHeader(
            totalApps = applications.size,
            activeApps = applications.count { it.status == ApplicationStatus.ACTIVE },
            hasUnsavedChanges = hasUnsavedChanges,
            isLoading = isLoading,
            onSave = {
                isLoading = true
                MainScope().launch {
                    delay(1000)
                    hasUnsavedChanges = false
                    isLoading = false
                }
            }
        )

        // Main Content - Master-Detail Layout
        Card(
            modifier = Modifier
                .fillMaxSize(),
            elevation = 2.dp,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column {
                // Application List (Top Section)
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    backgroundColor = MaterialTheme.colors.surface,
                    elevation = 0.dp,
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                ) {
                    ApplicationListSection(
                        applications = applications,
                        selectedApplication = selectedApplication,
                        onApplicationSelected = { selectedApplication = it },
                        onAddApplication = {
                            val newApp = EmvApplication(
                                name = "New Application",
                                aid = "A0000000000000",
                                label = "NEW APP",
                                priority = applications.size + 1,
                                version = "1.0",
                                type = ApplicationType.CUSTOM,
                                status = ApplicationStatus.INACTIVE
                            )
                            addApplication(newApp)
                        },
                        onDeleteApplication = { appToDelete ->
                            applications = applications.filterNot { it.id == appToDelete.id }
                            if (selectedApplication?.id == appToDelete.id) {
                                selectedApplication = null
                            }
                            hasUnsavedChanges = true
                        },
                        onMoveApplication = { app, direction ->
                            val currentIndex = applications.indexOf(app)
                            val newIndex = if (direction == "up") currentIndex - 1 else currentIndex + 1
                            if (newIndex in applications.indices) {
                                val mutableList = applications.toMutableList()
                                mutableList.removeAt(currentIndex)
                                mutableList.add(newIndex, app)
                                applications = mutableList
                                hasUnsavedChanges = true
                            }
                        },
                        onShowTemplates = { showTemplateDialog = true }
                    )
                }

                Divider(color = MaterialTheme.colors.primary.copy(alpha = 0.2f), thickness = 2.dp)

                // Application Details (Bottom Section)
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    backgroundColor = MaterialTheme.colors.surface,
                    elevation = 0.dp,
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                ) {
                    if (selectedApplication != null) {
                        ApplicationDetailsSection(
                            application = selectedApplication!!,
                            onApplicationChange = ::updateApplication,
                            onSaveAsTemplate = { showSaveTemplateDialog = true }
                        )
                    } else {
                        EmptySelectionState(
                            onAddApplication = {
                                val newApp = EmvApplication(
                                    name = "New Application",
                                    aid = "A0000000000000",
                                    label = "NEW APP",
                                    priority = applications.size + 1,
                                    version = "1.0",
                                    type = ApplicationType.CUSTOM,
                                    status = ApplicationStatus.INACTIVE
                                )
                                addApplication(newApp)
                            }
                        )
                    }
                }
            }
        }

        // Quick Actions
        AidApduQuickActions(
            selectedApplication = selectedApplication,
            onQuickAction = { action ->
                when (action) {
                    "add_application" -> {
                        val newApp = EmvApplication(
                            name = "New Application",
                            aid = "A0000000000000",
                            label = "NEW APP",
                            priority = applications.size + 1,
                            version = "1.0",
                            type = ApplicationType.CUSTOM,
                            status = ApplicationStatus.INACTIVE
                        )
                        addApplication(newApp)
                    }
                    "templates" -> showTemplateDialog = true
                    "save_template" -> showSaveTemplateDialog = true
                    "test_application" -> { /* TODO: Implement testing */ }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Template Dialog
    if (showTemplateDialog) {
        ApplicationTemplateDialog(
            templates = getApplicationTemplates(),
            onDismiss = { showTemplateDialog = false },
            onSelectTemplate = { template ->
                val newApp = template.copy(
                    id = UUID.randomUUID().toString(),
                    createdDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                )
                addApplication(newApp)
                showTemplateDialog = false
            }
        )
    }

    // Save Template Dialog
    if (showSaveTemplateDialog && selectedApplication != null) {
        SaveTemplateDialog(
            application = selectedApplication!!,
            onDismiss = { showSaveTemplateDialog = false },
            onSave = { templateName ->
                // TODO: Save as template
                showSaveTemplateDialog = false
            }
        )
    }
}

// --- AID/APDU HEADER ---

@Composable
fun AidApduHeader(
    totalApps: Int,
    activeApps: Int,
    hasUnsavedChanges: Boolean,
    isLoading: Boolean,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AID/APDU Simulator",
                        style = MaterialTheme.typography.h5,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        StatusIndicator(
                            hasUnsavedChanges = hasUnsavedChanges,
                            isLoading = isLoading
                        )
                    }
                }

                Button(
                    onClick = onSave,
                    enabled = hasUnsavedChanges && !isLoading,
                    modifier = Modifier.height(40.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colors.onPrimary
                        )
                    } else {
                        Icon(
                            Icons.Outlined.Save,
                            contentDescription = "Save",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save All")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Statistics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AidStatCard(
                    title = "Total Applications",
                    value = totalApps.toString(),
                    subtitle = "Configured apps",
                    icon = Icons.Outlined.Apps,
                    color = Color(0xFF6750A4)
                )

                AidStatCard(
                    title = "Active Applications",
                    value = activeApps.toString(),
                    subtitle = "${if (totalApps > 0) (activeApps * 100 / totalApps) else 0}% active",
                    icon = Icons.Outlined.PlayArrow,
                    color = Color(0xFF197A3E)
                )

                AidStatCard(
                    title = "Application Types",
                    value = ApplicationType.values().size.toString(),
                    subtitle = "Supported types",
                    icon = Icons.Outlined.Category,
                    color = Color(0xFF0B6BCB)
                )

                AidStatCard(
                    title = "EMV Compliance",
                    value = "4.3",
                    subtitle = "EMV version",
                    icon = Icons.Outlined.VerifiedUser,
                    color = Color(0xFFED6C02)
                )
            }
        }
    }
}



@Composable
fun AidStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        backgroundColor = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp),
        elevation = 0.dp,
        modifier = Modifier.width(130.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.caption,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.overline,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

// --- APPLICATION LIST SECTION ---

@Composable
fun ApplicationListSection(
    applications: List<EmvApplication>,
    selectedApplication: EmvApplication?,
    onApplicationSelected: (EmvApplication) -> Unit,
    onAddApplication: () -> Unit,
    onDeleteApplication: (EmvApplication) -> Unit,
    onMoveApplication: (EmvApplication, String) -> Unit,
    onShowTemplates: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf<ApplicationType?>(null) }
    var filterStatus by remember { mutableStateOf<ApplicationStatus?>(null) }

    val filteredApplications = applications.filter { app ->
        val matchesSearch = searchQuery.isEmpty() ||
                app.name.contains(searchQuery, ignoreCase = true) ||
                app.aid.contains(searchQuery, ignoreCase = true) ||
                app.label.contains(searchQuery, ignoreCase = true)
        val matchesType = filterType == null || app.type == filterType
        val matchesStatus = filterStatus == null || app.status == filterStatus

        matchesSearch && matchesType && matchesStatus
    }.sortedBy { it.priority }

    Column(modifier = Modifier.padding(16.dp)) {

        // Header with Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "EMV Applications",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${filteredApplications.size} of ${applications.size} applications",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onShowTemplates,
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.Notes,
                        contentDescription = "Templates",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Templates", fontSize = 12.sp)
                }

                Button(
                    onClick = onAddApplication,
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = "Add",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add App", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search and Filters - Compact
        ApplicationFilters(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            filterType = filterType,
            onFilterTypeChange = { filterType = it },
            filterStatus = filterStatus,
            onFilterStatusChange = { filterStatus = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Applications List
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            filteredApplications.forEach{ application ->
                ApplicationListItem(
                    application = application,
                    isSelected = selectedApplication?.id == application.id,
                    onSelect = { onApplicationSelected(application) },
                    onDelete = { onDeleteApplication(application) },
                    onMoveUp = { onMoveApplication(application, "up") },
                    onMoveDown = { onMoveApplication(application, "down") },
                    canMoveUp = applications.indexOf(application) > 0,
                    canMoveDown = applications.indexOf(application) < applications.size - 1
                )
            }

            if (filteredApplications.isEmpty()) {
                EmptyApplicationsState(
                    searchQuery = searchQuery,
                    onAddApplication = onAddApplication,
                    onClearFilters = {
                        searchQuery = ""
                        filterType = null
                        filterStatus = null
                    }
                )
            }
        }
    }
}

@Composable
fun ApplicationFilters(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filterType: ApplicationType?,
    onFilterTypeChange: (ApplicationType?) -> Unit,
    filterStatus: ApplicationStatus?,
    onFilterStatusChange: (ApplicationStatus?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search applications...") },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = "Search",
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                AnimatedVisibility(
                    visible = searchQuery.isNotEmpty(),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Clear,
                            contentDescription = "Clear",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
        )

        // Compact Filters Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Type Filter
            CompactFilterDropdown(
                label = "Type",
                selectedValue = filterType?.displayName ?: "All",
                options = listOf("All") + ApplicationType.values().map { it.displayName },
                onSelectionChange = { value ->
                    onFilterTypeChange(
                        if (value == "All") null
                        else ApplicationType.values().find { it.displayName == value }
                    )
                },
                modifier = Modifier.weight(1f)
            )

            // Status Filter
            CompactFilterDropdown(
                label = "Status",
                selectedValue = filterStatus?.displayName ?: "All",
                options = listOf("All") + ApplicationStatus.values().map { it.displayName },
                onSelectionChange = { value ->
                    onFilterStatusChange(
                        if (value == "All") null
                        else ApplicationStatus.values().find { it.displayName == value }
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun CompactFilterDropdown(
    label: String,
    selectedValue: String,
    options: List<String>,
    onSelectionChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = { },
            label = { Text(label, fontSize = 12.sp) },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable { expanded = true },
            trailingIcon = {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    modifier = Modifier.size(16.dp)
                )
            },
            shape = RoundedCornerShape(8.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    onClick = {
                        onSelectionChange(option)
                        expanded = false
                    }
                ) {
                    Text(option, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun ApplicationListItem(
    application: EmvApplication,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        backgroundColor = if (isSelected)
            MaterialTheme.colors.primary.copy(alpha = 0.12f)
        else
            MaterialTheme.colors.surface,
        elevation = if (isSelected) 2.dp else 0.dp,
        shape = RoundedCornerShape(8.dp),
        border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.1f)) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority Badge
            Card(
                backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp),
                elevation = 0.dp,
                modifier = Modifier.size(32.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = application.priority.toString(),
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Application Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = application.type.icon,
                        contentDescription = application.type.displayName,
                        tint = application.type.color,
                        modifier = Modifier.size(16.dp)
                    )

                    Text(
                        text = application.name,
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Card(
                        backgroundColor = application.status.color.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        elevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = application.status.icon,
                                contentDescription = application.status.displayName,
                                tint = application.status.color,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = application.status.displayName,
                                style = MaterialTheme.typography.overline,
                                color = application.status.color,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AID: ${application.aid}",
                        style = MaterialTheme.typography.caption,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                    )

                    Text(
                        text = application.label,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move Up",
                        modifier = Modifier.size(14.dp),
                        tint = if (canMoveUp) MaterialTheme.colors.onSurface else MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                    )
                }

                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move Down",
                        modifier = Modifier.size(14.dp),
                        tint = if (canMoveDown) MaterialTheme.colors.onSurface else MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colors.error,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyApplicationsState(
    searchQuery: String,
    onAddApplication: () -> Unit,
    onClearFilters: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Outlined.Apps,
                contentDescription = "No applications",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
            )

            Text(
                text = if (searchQuery.isNotEmpty()) "No applications match your search" else "No applications configured",
                style = MaterialTheme.typography.subtitle2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Text(
                text = if (searchQuery.isNotEmpty())
                    "Try adjusting your search or filters"
                else
                    "Add your first EMV application to get started",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (searchQuery.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onClearFilters,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Clear Filters", fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = onAddApplication,
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = "Add",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Application", fontSize = 12.sp)
                }
            }
        }
    }
}

// --- APPLICATION DETAILS SECTION ---

@Composable
fun ApplicationDetailsSection(
    application: EmvApplication,
    onApplicationChange: (EmvApplication) -> Unit,
    onSaveAsTemplate: () -> Unit
) {
    var activeTab by remember { mutableStateOf("basic") }

    Column(modifier = Modifier.padding(16.dp)) {

        // Header with Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Application Details",
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )

            OutlinedButton(
                onClick = onSaveAsTemplate,
                modifier = Modifier.height(32.dp)
            ) {
                Icon(
                    Icons.Outlined.Save,
                    contentDescription = "Save Template",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save as Template", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Compact Tab Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                "basic" to "Basic",
                "capabilities" to "Capabilities",
                "processing" to "Processing",
                "usage" to "Usage",
                "files" to "Files"
            ).forEach { (key, title) ->
                CompactTab(
                    text = title,
                    selected = activeTab == key,
                    onClick = { activeTab = key },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Content
        Box(modifier = Modifier.fillMaxSize()) {
            when (activeTab) {
                "basic" -> {
                    BasicDetailsTab(
                        application = application,
                        onApplicationChange = onApplicationChange
                    )
                }
                "capabilities" -> {
                    CapabilitiesTab(
                        capabilities = application.capabilities,
                        onCapabilitiesChange = { newCapabilities ->
                            onApplicationChange(application.copy(capabilities = newCapabilities))
                        }
                    )
                }
                "processing" -> {
                    ProcessingOptionsTab(
                        options = application.processingOptions,
                        onOptionsChange = { newOptions ->
                            onApplicationChange(application.copy(processingOptions = newOptions))
                        }
                    )
                }
                "usage" -> {
                    UsageControlTab(
                        usageControl = application.usageControl,
                        onUsageControlChange = { newUsageControl ->
                            onApplicationChange(application.copy(usageControl = newUsageControl))
                        }
                    )
                }
                "files" -> {
                    FileReferencesTab(
                        fileReferences = application.fileReferences,
                        onFileReferencesChange = { newFileReferences ->
                            onApplicationChange(application.copy(fileReferences = newFileReferences))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CompactTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colors.primary.copy(alpha = 0.1f) else Color.Transparent,
        animationSpec = tween(200)
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
        animationSpec = tween(200)
    )

    Card(
        modifier = modifier
            .clickable { onClick() }
            .height(36.dp),
        backgroundColor = backgroundColor,
        elevation = if (selected) 1.dp else 0.dp,
        shape = RoundedCornerShape(6.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor,
                style = MaterialTheme.typography.caption,
                fontSize = 11.sp
            )
        }
    }
}

// --- BASIC DETAILS TAB ---

@Composable
fun BasicDetailsTab(
    application: EmvApplication,
    onApplicationChange: (EmvApplication) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AidTextField(
                label = "Application Name",
                value = application.name,
                onValueChange = { onApplicationChange(application.copy(name = it)) },
                modifier = Modifier.weight(2f)
            )

            AidTextField(
                label = "Priority",
                value = application.priority.toString(),
                onValueChange = { value ->
                    val priority = value.toIntOrNull() ?: application.priority
                    onApplicationChange(application.copy(priority = priority))
                },
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
        }

        AidTextField(
            label = "AID (Application Identifier)",
            value = application.aid,
            onValueChange = { value ->
                val cleaned = value.replace(" ", "").filter {
                    it.isDigit() || it in 'a'..'f' || it in 'A'..'F'
                }.uppercase()
                if (cleaned.length <= 32) {
                    onApplicationChange(application.copy(aid = cleaned))
                }
            },
            placeholder = "Enter AID in hex format",
            fontFamily = FontFamily.Monospace
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AidTextField(
                label = "Application Label",
                value = application.label,
                onValueChange = { onApplicationChange(application.copy(label = it.uppercase())) },
                modifier = Modifier.weight(2f)
            )

            AidTextField(
                label = "Version",
                value = application.version,
                onValueChange = { onApplicationChange(application.copy(version = it)) },
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ApplicationTypeDropdown(
                selectedType = application.type,
                onTypeChange = { onApplicationChange(application.copy(type = it)) },
                modifier = Modifier.weight(1f)
            )

            ApplicationStatusDropdown(
                selectedStatus = application.status,
                onStatusChange = { onApplicationChange(application.copy(status = it)) },
                modifier = Modifier.weight(1f)
            )
        }

        // Application Info Card
        Card(
            backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.05f),
            elevation = 0.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "Application Information",
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                InfoRow("Created", application.createdDate)
                InfoRow("Last Modified", application.lastModified)
                InfoRow("AID Length", "${application.aid.length / 2} bytes")
                InfoRow("Type", application.type.displayName)
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Medium
        )
    }
}

// --- CAPABILITIES TAB ---

@Composable
fun CapabilitiesTab(
    capabilities: ApplicationCapabilities,
    onCapabilitiesChange: (ApplicationCapabilities) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            "Interface Support",
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary
        )

        CapabilityCheckbox(
            title = "Contact Interface",
            description = "ISO 7816 contact interface support",
            icon = Icons.Outlined.Cable,
            checked = capabilities.contactInterface,
            onCheckedChange = {
                onCapabilitiesChange(capabilities.copy(contactInterface = it))
            }
        )
        CapabilityCheckbox(
            title = "Contactless Interface",
            description = "NFC/RFID contactless interface support",
            icon = Icons.Outlined.Contactless,
            checked = capabilities.contactlessInterface,
            onCheckedChange = {
                onCapabilitiesChange(capabilities.copy(contactlessInterface = it))
            }
        )

        CapabilityCheckbox(
            title = "Magnetic Stripe",
            description = "Legacy magnetic stripe emulation",
            icon = Icons.Outlined.CreditCard,
            checked = capabilities.magneticStripe,
            onCheckedChange = {
                onCapabilitiesChange(capabilities.copy(magneticStripe = it))
            }
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Authentication & Security",
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary
        )

        CapabilityCheckbox(
            title = "PIN Support",
            description = "PIN verification capability",
            icon = Icons.Outlined.Pin,
            checked = capabilities.pinSupport,
            onCheckedChange = {
                onCapabilitiesChange(capabilities.copy(pinSupport = it))
            }
        )

        CapabilityCheckbox(
            title = "Biometric Support",
            description = "Fingerprint/biometric authentication",
            icon = Icons.Outlined.Fingerprint,
            checked = capabilities.biometricSupport,
            onCheckedChange = {
                onCapabilitiesChange(capabilities.copy(biometricSupport = it))
            }
        )

        CapabilityCheckbox(
            title = "Offline Authentication",
            description = "Offline transaction authorization",
            icon = Icons.Outlined.CloudOff,
            checked = capabilities.offlineAuth,
            onCheckedChange = {
                onCapabilitiesChange(capabilities.copy(offlineAuth = it))
            }
        )

        CapabilityCheckbox(
            title = "Online Authentication",
            description = "Online transaction authorization",
            icon = Icons.Outlined.Cloud,
            checked = capabilities.onlineAuth,
            onCheckedChange = {
                onCapabilitiesChange(capabilities.copy(onlineAuth = it))
            }
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Transaction Features",
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary
        )

        CapabilityCheckbox(
            title = "Cashback Support",
            description = "Cash withdrawal during purchase",
            icon = Icons.Outlined.AttachMoney,
            checked = capabilities.cashback,
            onCheckedChange = {
                onCapabilitiesChange(capabilities.copy(cashback = it))
            }
        )

        CapabilityCheckbox(
            title = "Refunds Support",
            description = "Transaction refund processing",
            icon = Icons.Outlined.Undo,
            checked = capabilities.refunds,
            onCheckedChange = {
                onCapabilitiesChange(capabilities.copy(refunds = it))
            }
        )
    }
}

@Composable
fun CapabilityCheckbox(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        backgroundColor = if (checked)
            MaterialTheme.colors.primary.copy(alpha = 0.08f)
        else
            MaterialTheme.colors.surface,
        elevation = if (checked) 1.dp else 0.dp,
        shape = RoundedCornerShape(6.dp),
        border = if (!checked) BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.1f)) else null
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (checked) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }

            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// --- PROCESSING OPTIONS TAB ---

@Composable
fun ProcessingOptionsTab(
    options: ProcessingOptions,
    onOptionsChange: (ProcessingOptions) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            "Data Authentication",
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary
        )

        ProcessingOptionCheckbox(
            title = "SDA (Static Data Authentication)",
            description = "Basic offline data authentication",
            checked = options.sda,
            onCheckedChange = {
                onOptionsChange(options.copy(sda = it))
            }
        )

        ProcessingOptionCheckbox(
            title = "DDA (Dynamic Data Authentication)",
            description = "Enhanced offline data authentication",
            checked = options.dda,
            onCheckedChange = {
                onOptionsChange(options.copy(dda = it))
            }
        )
        ProcessingOptionCheckbox(
            title = "CDA (Combined DDA/AC Generation)",
            description = "Combined authentication and cryptogram",
            checked = options.cda,
            onCheckedChange = {
                onOptionsChange(options.copy(cda = it))
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "PIN Processing",
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary
        )

        ProcessingOptionCheckbox(
            title = "PIN Bypass",
            description = "Allow transactions without PIN",
            checked = options.pinBypass,
            onCheckedChange = {
                onOptionsChange(options.copy(pinBypass = it))
            }
        )

        ProcessingOptionCheckbox(
            title = "Online PIN Required",
            description = "Force online PIN verification",
            checked = options.onlinePinRequired,
            onCheckedChange = {
                onOptionsChange(options.copy(onlinePinRequired = it))
            }
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Risk Management",
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary
        )

        ProcessingOptionCheckbox(
            title = "Issuer Authentication Required",
            description = "Require issuer authentication",
            checked = options.issuerAuthenticationRequired,
            onCheckedChange = {
                onOptionsChange(options.copy(issuerAuthenticationRequired = it))
            }
        )
        ProcessingOptionCheckbox(
            title = "Terminal Risk Management",
            description = "Enable terminal-based risk management",
            checked = options.terminalRiskManagement,
            onCheckedChange = {
                onOptionsChange(options.copy(terminalRiskManagement = it))
            }
        )
    }
}

@Composable
fun ProcessingOptionCheckbox(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
            Text(
                text = description,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        }
    }
}

// --- USAGE CONTROL TAB ---

@Composable
fun UsageControlTab(
    usageControl: ApplicationUsageControl,
    onUsageControlChange: (ApplicationUsageControl) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            "Transaction Types",
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary
        )

        UsageControlSection(
            title = "Purchase Transactions",
            domesticChecked = usageControl.domesticTransactions,
            internationalChecked = usageControl.internationalTransactions,
            onDomesticChange = {
                onUsageControlChange(usageControl.copy(domesticTransactions = it))
            },
            onInternationalChange = {
                onUsageControlChange(usageControl.copy(internationalTransactions = it))
            }
        )

        UsageControlSection(
            title = "ATM Withdrawals",
            domesticChecked = usageControl.domesticAtm,
            internationalChecked = usageControl.internationalAtm,
            onDomesticChange = {
                onUsageControlChange(usageControl.copy(domesticAtm = it))
            },
            onInternationalChange = {
                onUsageControlChange(usageControl.copy(internationalAtm = it))
            }
        )

        UsageControlSection(
            title = "Cashback",
            domesticChecked = usageControl.domesticCashback,
            internationalChecked = usageControl.internationalCashback,
            onDomesticChange = {
                onUsageControlChange(usageControl.copy(domesticCashback = it))
            },
            onInternationalChange = {
                onUsageControlChange(usageControl.copy(internationalCashback = it))
            }
        )

        UsageControlSection(
            title = "Goods & Services",
            domesticChecked = usageControl.domesticGoods,
            internationalChecked = usageControl.internationalGoods,
            onDomesticChange = {
                onUsageControlChange(usageControl.copy(domesticGoods = it))
            },
            onInternationalChange = {
                onUsageControlChange(usageControl.copy(internationalGoods = it))
            }
        )
        UsageControlSection(
            title = "Service Payments",
            domesticChecked = usageControl.domesticServices,
            internationalChecked = usageControl.internationalServices,
            onDomesticChange = {
                onUsageControlChange(usageControl.copy(domesticServices = it))
            },
            onInternationalChange = {
                onUsageControlChange(usageControl.copy(internationalServices = it))
            }
        )
    }
}

@Composable
fun UsageControlSection(
    title: String,
    domesticChecked: Boolean,
    internationalChecked: Boolean,
    onDomesticChange: (Boolean) -> Unit,
    onInternationalChange: (Boolean) -> Unit
) {
    Card(
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 1.dp,
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = domesticChecked,
                        onCheckedChange = onDomesticChange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Domestic",
                        style = MaterialTheme.typography.caption,
                        fontSize = 12.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = internationalChecked,
                        onCheckedChange = onInternationalChange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "International",
                        style = MaterialTheme.typography.caption,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// --- FILE REFERENCES TAB ---

@Composable
fun FileReferencesTab(
    fileReferences: List<FileReference>,
    onFileReferencesChange: (List<FileReference>) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Associated Files",
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary
            )

            OutlinedButton(
                onClick = {
                    val newFile = FileReference(
                        name = "New File",
                        fileId = "0000",
                        fileType = "Data Object",
                        description = "New file reference"
                    )
                    onFileReferencesChange(fileReferences + newFile)
                },
                modifier = Modifier.height(28.dp)
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = "Add File",
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add File", fontSize = 11.sp)
            }
        }

        fileReferences.forEach { fileRef ->
            FileReferenceCard(
                fileReference = fileRef,
                onUpdate = { updatedFile ->
                    onFileReferencesChange(
                        fileReferences.map { if (it.id == updatedFile.id) updatedFile else it }
                    )
                },
                onDelete = {
                    onFileReferencesChange(fileReferences.filterNot { it.id == fileRef.id })
                }
            )
        }

        if (fileReferences.isEmpty()) {
            Card(
                backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.05f),
                elevation = 0.dp,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.FolderOpen,
                        contentDescription = "No files",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No file references",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        "Add associated files for this application",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun FileReferenceCard(
    fileReference: FileReference,
    onUpdate: (FileReference) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 1.dp,
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Description,
                        contentDescription = "File",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colors.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        fileReference.name,
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )

                    if (fileReference.isRequired) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Card(
                            backgroundColor = Color(0xFFD32F2F).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp),
                            elevation = 0.dp
                        ) {
                            Text(
                                "Required",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.overline,
                                color = Color(0xFFD32F2F),
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colors.error,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                "File ID: ${fileReference.fileId} • Type: ${fileReference.fileType}",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                fontSize = 11.sp
            )

            if (fileReference.description.isNotEmpty()) {
                Text(
                    fileReference.description,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

// --- EMPTY SELECTION STATE ---

@Composable
fun EmptySelectionState(
    onAddApplication: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(30.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Outlined.Apps,
                contentDescription = "No selection",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
            )

            Text(
                "Select an application to view details",
                style = MaterialTheme.typography.subtitle2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Text(
                "Choose an application from the list above or create a new one",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onAddApplication,
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = "Add",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Application")
            }
        }
    }
}

// --- UTILITY COMPONENTS ---

@Composable
fun AidTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    fontFamily: FontFamily? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        placeholder = { Text(placeholder, fontSize = 12.sp) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = LocalTextStyle.current.copy(
            fontSize = 13.sp,
            fontFamily = fontFamily ?: FontFamily.Default
        ),
        shape = RoundedCornerShape(6.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = MaterialTheme.colors.primary,
            unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
        )
    )
}

@Composable
fun ApplicationTypeDropdown(
    selectedType: ApplicationType,
    onTypeChange: (ApplicationType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedType.displayName,
            onValueChange = { },
            label = { Text("Application Type", fontSize = 12.sp) },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            leadingIcon = {
                Icon(
                    imageVector = selectedType.icon,
                    contentDescription = null,
                    tint = selectedType.color,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    modifier = Modifier.size(16.dp)
                )
            },
            shape = RoundedCornerShape(6.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
            )
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ApplicationType.values().forEach { type ->
                DropdownMenuItem(
                    onClick = {
                        onTypeChange(type)
                        expanded = false
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = type.icon,
                            contentDescription = null,
                            tint = type.color,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(type.displayName, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ApplicationStatusDropdown(
    selectedStatus: ApplicationStatus,
    onStatusChange: (ApplicationStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedStatus.displayName,
            onValueChange = { },
            label = { Text("Status", fontSize = 12.sp) },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            leadingIcon = {
                Icon(
                    imageVector = selectedStatus.icon,
                    contentDescription = null,
                    tint = selectedStatus.color,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    modifier = Modifier.size(16.dp)
                )
            },
            shape = RoundedCornerShape(6.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
            )
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ApplicationStatus.values().forEach { status ->
                DropdownMenuItem(
                    onClick = {
                        onStatusChange(status)
                        expanded = false
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = status.icon,
                            contentDescription = null,
                            tint = status.color,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(status.displayName, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// --- QUICK ACTIONS ---

@Composable
fun AidApduQuickActions(
    selectedApplication: EmvApplication?,
    onQuickAction: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Quick Actions",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (selectedApplication != null)
                        "Working on: ${selectedApplication.name}"
                    else
                        "EMV application management",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onQuickAction("templates") },
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.Notes,
                        contentDescription = "Templates",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Templates")
                }

                if (selectedApplication != null) {
                    OutlinedButton(
                        onClick = { onQuickAction("test_application") },
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            Icons.Outlined.PlayArrow,
                            contentDescription = "Test",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test App")
                    }

                    OutlinedButton(
                        onClick = { onQuickAction("save_template") },
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Save,
                            contentDescription = "Save Template",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Template")
                    }
                }

                Button(
                    onClick = { onQuickAction("add_application") },
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = "Add",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Application")
                }
            }
        }
    }
}

// --- DIALOGS ---

@Composable
fun ApplicationTemplateDialog(
    templates: List<EmvApplication>,
    onDismiss: () -> Unit,
    onSelectTemplate: (EmvApplication) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(500.dp)
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Application Templates",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Choose a template to create a new application",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Templates List
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    templates.forEach { template ->
                        ApplicationTemplateCard(
                            template = template,
                            onSelect = { onSelectTemplate(template) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ApplicationTemplateCard(
    template: EmvApplication,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = template.type.icon,
                contentDescription = template.type.displayName,
                tint = template.type.color,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "AID: ${template.aid}",
                    style = MaterialTheme.typography.body2,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TemplateChip(
                        text = template.type.displayName,
                        backgroundColor = template.type.color.copy(alpha = 0.1f),
                        textColor = template.type.color
                    )
                    TemplateChip(
                        text = template.label,
                        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                        textColor = MaterialTheme.colors.primary
                    )
                }
            }

            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = "Select",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun TemplateChip(
    text: String,
    backgroundColor: Color,
    textColor: Color
) {
    Card(
        backgroundColor = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        elevation = 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.caption,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SaveTemplateDialog(
    application: EmvApplication,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var templateName by remember { mutableStateOf("${application.name} Template") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.width(400.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                Text(
                    text = "Save as Template",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Save the current application configuration as a reusable template",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text("Template Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onSave(templateName) },
                        enabled = templateName.isNotBlank()
                    ) {
                        Text("Save Template")
                    }
                }
            }
        }
    }
}