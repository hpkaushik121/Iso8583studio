package `in`.aicortex.iso8583studio.ui.screens.config.apduSimulator


import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

// --- DATA MODELS ---

enum class KeyType(val displayName: String, val icon: ImageVector, val color: Color) {
    MASTER("Master Key", Icons.Outlined.AdminPanelSettings, Color(0xFF6750A4)),
    APPLICATION("Application Key", Icons.Outlined.Apps, Color(0xFF0B6BCB)),
    SESSION("Session Key", Icons.Outlined.Schedule, Color(0xFF197A3E)),
    WORKING("Working Key", Icons.Outlined.Work, Color(0xFFED6C02)),
    PIN("PIN Key", Icons.Outlined.Pin, Color(0xFFD32F2F)),
    MAC("MAC Key", Icons.Outlined.Security, Color(0xFF795548))
}

enum class Algorithm(val displayName: String, val category: String) {
    DES("DES", "Symmetric"),
    TRIPLE_DES("3DES", "Symmetric"),
    AES_128("AES-128", "Symmetric"),
    AES_192("AES-192", "Symmetric"),
    AES_256("AES-256", "Symmetric"),
    RSA_1024("RSA-1024", "Asymmetric"),
    RSA_2048("RSA-2048", "Asymmetric"),
    RSA_4096("RSA-4096", "Asymmetric"),
    ECC_P256("ECC-P256", "Asymmetric"),
    ECC_P384("ECC-P384", "Asymmetric")
}

enum class KeyStatus(val displayName: String, val color: Color, val icon: ImageVector) {
    VALID("Valid", Color(0xFF197A3E), Icons.Outlined.CheckCircle),
    EXPIRED("Expired", Color(0xFFED6C02), Icons.Outlined.Schedule),
    INVALID("Invalid", Color(0xFFD32F2F), Icons.Outlined.Error),
    REVOKED("Revoked", Color(0xFF795548), Icons.Outlined.Block)
}

data class CryptoKey(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: KeyType,
    val algorithm: Algorithm,
    val keyLength: Int,
    val status: KeyStatus,
    val createdDate: LocalDateTime = LocalDateTime.now(),
    val expiryDate: LocalDateTime? = null,
    val keyValue: String = "",
    val checkValue: String = "",
    val usage: String = "",
    val isHsmStored: Boolean = false
)

data class Certificate(
    val id: String = UUID.randomUUID().toString(),
    val commonName: String,
    val issuer: String,
    val subject: String,
    val serialNumber: String,
    val algorithm: String,
    val keyLength: Int,
    val validFrom: LocalDateTime,
    val validTo: LocalDateTime,
    val fingerprint: String,
    val usage: List<String>,
    val status: KeyStatus,
    val certificateChain: List<String> = emptyList()
)

data class CryptoSettings(
    val defaultSymmetricAlgorithm: Algorithm = Algorithm.AES_256,
    val defaultAsymmetricAlgorithm: Algorithm = Algorithm.RSA_2048,
    val keyDerivationFunction: String = "PBKDF2",
    val keyDerivationIterations: Int = 10000,
    val randomNumberGenerator: String = "SecureRandom",
    val hsmEnabled: Boolean = false,
    val hsmProvider: String = "SafeNet Luna",
    val hsmSlot: Int = 0,
    val hsmPin: String = ""
)

// --- SAMPLE DATA ---

fun getSampleKeys(): List<CryptoKey> = listOf(
    CryptoKey(
        name = "MASTER_KEY_001",
        type = KeyType.MASTER,
        algorithm = Algorithm.AES_256,
        keyLength = 256,
        status = KeyStatus.VALID,
        keyValue = "0123456789ABCDEF0123456789ABCDEF",
        checkValue = "A1B2C3",
        usage = "Key derivation and encryption"
    ),
    CryptoKey(
        name = "APP_KEY_VISA",
        type = KeyType.APPLICATION,
        algorithm = Algorithm.TRIPLE_DES,
        keyLength = 192,
        status = KeyStatus.VALID,
        keyValue = "FEDCBA9876543210FEDCBA9876543210",
        checkValue = "D4E5F6",
        usage = "VISA application processing"
    ),
    CryptoKey(
        name = "SESSION_001",
        type = KeyType.SESSION,
        algorithm = Algorithm.AES_128,
        keyLength = 128,
        status = KeyStatus.EXPIRED,
        expiryDate = LocalDateTime.now().minusDays(5),
        keyValue = "0123456789ABCDEF",
        checkValue = "123ABC",
        usage = "Session encryption"
    ),
    CryptoKey(
        name = "PIN_VERIFY_KEY",
        type = KeyType.PIN,
        algorithm = Algorithm.TRIPLE_DES,
        keyLength = 192,
        status = KeyStatus.VALID,
        keyValue = "ABCDEF0123456789ABCDEF0123456789",
        checkValue = "AB12CD",
        usage = "PIN verification and encryption",
        isHsmStored = true
    ),
    CryptoKey(
        name = "MAC_GEN_KEY",
        type = KeyType.MAC,
        algorithm = Algorithm.AES_128,
        keyLength = 128,
        status = KeyStatus.VALID,
        keyValue = "FEDCBA9876543210",
        checkValue = "FE9876",
        usage = "Message authentication"
    )
)

fun getSampleCertificates(): List<Certificate> = listOf(
    Certificate(
        commonName = "Payment Terminal CA",
        issuer = "Root Certificate Authority",
        subject = "CN=Payment Terminal CA, O=Financial Services, C=US",
        serialNumber = "1234567890ABCDEF",
        algorithm = "RSA with SHA-256",
        keyLength = 2048,
        validFrom = LocalDateTime.now().minusYears(2),
        validTo = LocalDateTime.now().plusYears(3),
        fingerprint = "A1:B2:C3:D4:E5:F6:07:08:09:0A:1B:2C:3D:4E:5F:60",
        usage = listOf("Digital Signature", "Key Encipherment", "Certificate Signing"),
        status = KeyStatus.VALID
    ),
    Certificate(
        commonName = "EMV Application Certificate",
        issuer = "Payment Terminal CA",
        subject = "CN=EMV Application Certificate, O=Card Processor, C=US",
        serialNumber = "FEDCBA0987654321",
        algorithm = "ECC with SHA-256",
        keyLength = 256,
        validFrom = LocalDateTime.now().minusMonths(6),
        validTo = LocalDateTime.now().plusYears(1),
        fingerprint = "F1:E2:D3:C4:B5:A6:97:88:79:6A:5B:4C:3D:2E:1F:00",
        usage = listOf("Digital Signature", "Non Repudiation"),
        status = KeyStatus.VALID
    ),
    Certificate(
        commonName = "Legacy SSL Certificate",
        issuer = "Old CA Authority",
        subject = "CN=Legacy SSL Certificate, O=Legacy Systems, C=US",
        serialNumber = "9876543210FEDCBA",
        algorithm = "RSA with SHA-1",
        keyLength = 1024,
        validFrom = LocalDateTime.now().minusYears(5),
        validTo = LocalDateTime.now().minusMonths(1),
        fingerprint = "99:88:77:66:55:44:33:22:11:00:AA:BB:CC:DD:EE:FF",
        usage = listOf("Server Authentication"),
        status = KeyStatus.EXPIRED
    )
)

// --- MAIN CRYPTO TAB ---

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CryptoManagementTab() {
    var activeSection by remember { mutableStateOf("keys") }
    var keys by remember { mutableStateOf(getSampleKeys()) }
    var certificates by remember { mutableStateOf(getSampleCertificates()) }
    var settings by remember { mutableStateOf(CryptoSettings()) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Dialog states
    var showKeyWizard by remember { mutableStateOf(false) }
    var showCertificateImport by remember { mutableStateOf(false) }
    var selectedKey by remember { mutableStateOf<CryptoKey?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Header with Statistics
        CryptoManagementHeader(
            keysCount = keys.size,
            certificatesCount = certificates.size,
            validKeysCount = keys.count { it.status == KeyStatus.VALID },
            validCertificatesCount = certificates.count { it.status == KeyStatus.VALID },
            hasUnsavedChanges = hasUnsavedChanges,
            isLoading = isLoading,
            onSave = {
                isLoading = true
                kotlinx.coroutines.MainScope().launch {
                    kotlinx.coroutines.delay(1000)
                    hasUnsavedChanges = false
                    isLoading = false
                }
            }
        )

        // Section Navigation
        CryptoSectionNavigation(
            activeSection = activeSection,
            onSectionChange = { activeSection = it }
        )

        // Main Content Area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 500.dp, max = 700.dp),
            elevation = 2.dp,
            shape = RoundedCornerShape(12.dp)
        ) {
            AnimatedContent(
                targetState = activeSection,
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { width -> width / 3 },
                        animationSpec = tween(300)
                    ) + fadeIn() with
                            slideOutHorizontally(
                                targetOffsetX = { width -> -width / 3 },
                                animationSpec = tween(300)
                            ) + fadeOut()
                }
            ) { section ->
                when (section) {
                    "keys" -> {
                        KeyManagementSection(
                            keys = keys,
                            onAddKey = { showKeyWizard = true },
                            onEditKey = { key ->
                                selectedKey = key
                                showKeyWizard = true
                            },
                            onDeleteKey = { keyToDelete ->
                                keys = keys.filterNot { it.id == keyToDelete.id }
                                hasUnsavedChanges = true
                            },
                            onTestKey = { /* TODO: Implement key testing */ },
                            onImportKeys = { /* TODO: Implement key import */ }
                        )
                    }
                    "certificates" -> {
                        CertificateManagementSection(
                            certificates = certificates,
                            onImportCertificate = { showCertificateImport = true },
                            onExportCertificate = { /* TODO: Implement export */ },
                            onValidateChain = { /* TODO: Implement chain validation */ }
                        )
                    }
                    "settings" -> {
                        CryptographicSettingsSection(
                            settings = settings,
                            onSettingsChange = { newSettings ->
                                settings = newSettings
                                hasUnsavedChanges = true
                            }
                        )
                    }
                }
            }
        }

        // Quick Actions
        CryptoQuickActions(
            activeSection = activeSection,
            onQuickAction = { action ->
                when (action) {
                    "generate_key" -> showKeyWizard = true
                    "import_certificate" -> showCertificateImport = true
                    "validate_all" -> {
                        // TODO: Implement bulk validation
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Dialogs
    if (showKeyWizard) {
        KeyGenerationWizard(
            existingKey = selectedKey,
            onDismiss = {
                showKeyWizard = false
                selectedKey = null
            },
            onSave = { newKey ->
                if (selectedKey != null) {
                    keys = keys.map { if (it.id == selectedKey!!.id) newKey else it }
                } else {
                    keys = keys + newKey
                }
                hasUnsavedChanges = true
                showKeyWizard = false
                selectedKey = null
            }
        )
    }

    if (showCertificateImport) {
        CertificateImportDialog(
            onDismiss = { showCertificateImport = false },
            onImport = { newCertificate ->
                certificates = certificates + newCertificate
                hasUnsavedChanges = true
                showCertificateImport = false
            }
        )
    }
}

// --- CRYPTO MANAGEMENT HEADER ---

@Composable
fun CryptoManagementHeader(
    keysCount: Int,
    certificatesCount: Int,
    validKeysCount: Int,
    validCertificatesCount: Int,
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
                        text = "Cryptographic Management",
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
                CryptoStatCard(
                    title = "Cryptographic Keys",
                    value = keysCount.toString(),
                    subtitle = "$validKeysCount valid",
                    icon = Icons.Outlined.Key,
                    color = Color(0xFF6750A4)
                )

                CryptoStatCard(
                    title = "Certificates",
                    value = certificatesCount.toString(),
                    subtitle = "$validCertificatesCount valid",
                    icon = Icons.Outlined.Note,
                    color = Color(0xFF0B6BCB)
                )

                CryptoStatCard(
                    title = "Security Level",
                    value = "High",
                    subtitle = "AES-256 default",
                    icon = Icons.Outlined.Shield,
                    color = Color(0xFF197A3E)
                )

                CryptoStatCard(
                    title = "HSM Status",
                    value = "Connected",
                    subtitle = "SafeNet Luna",
                    icon = Icons.Outlined.Hardware,
                    color = Color(0xFFED6C02)
                )
            }
        }
    }
}



@Composable
fun CryptoStatCard(
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
        modifier = Modifier.width(120.dp)
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

// --- SECTION NAVIGATION ---

@Composable
fun CryptoSectionNavigation(
    activeSection: String,
    onSectionChange: (String) -> Unit
) {
    val sections = listOf(
        Triple("keys", "Key Management", Icons.Outlined.Key),
        Triple("certificates", "Certificates", Icons.Outlined.Note),
        Triple("settings", "Crypto Settings", Icons.Outlined.Settings)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sections.forEach { (key, title, icon) ->
            CryptoSectionTab(
                text = title,
                icon = icon,
                selected = activeSection == key,
                onClick = { onSectionChange(key) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun CryptoSectionTab(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colors.primary.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = tween(200)
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
        animationSpec = tween(200)
    )

    Card(
        modifier = modifier
            .clickable { onClick() }
            .height(56.dp),
        backgroundColor = backgroundColor,
        elevation = if (selected) 2.dp else 0.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor,
                style = MaterialTheme.typography.body2
            )
        }
    }
}

// --- KEY MANAGEMENT SECTION ---

@Composable
fun KeyManagementSection(
    keys: List<CryptoKey>,
    onAddKey: () -> Unit,
    onEditKey: (CryptoKey) -> Unit,
    onDeleteKey: (CryptoKey) -> Unit,
    onTestKey: (CryptoKey) -> Unit,
    onImportKeys: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedKeyType by remember { mutableStateOf<KeyType?>(null) }
    var selectedStatus by remember { mutableStateOf<KeyStatus?>(null) }

    val filteredKeys = keys.filter { key ->
        val matchesSearch = searchQuery.isEmpty() ||
                key.name.contains(searchQuery, ignoreCase = true) ||
                key.type.displayName.contains(searchQuery, ignoreCase = true)
        val matchesType = selectedKeyType == null || key.type == selectedKeyType
        val matchesStatus = selectedStatus == null || key.status == selectedStatus

        matchesSearch && matchesType && matchesStatus
    }

    Column(modifier = Modifier.padding(16.dp)) {

        // Header with Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Cryptographic Keys",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${filteredKeys.size} of ${keys.size} keys",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onImportKeys,
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.Upload,
                        contentDescription = "Import",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import")
                }

                Button(
                    onClick = onAddKey,
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = "Add Key",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Key")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search and Filters
        KeySearchAndFilters(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            selectedKeyType = selectedKeyType,
            onKeyTypeChange = { selectedKeyType = it },
            selectedStatus = selectedStatus,
            onStatusChange = { selectedStatus = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Keys Table
        Card(
            modifier = Modifier.fillMaxSize(),
            elevation = 1.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                // Table Header
                item {
                    KeyTableHeader()
                }

                // Key Rows
                items(filteredKeys, key = { it.id }) { key ->
                    KeyTableRow(
                        key = key,
                        onEdit = { onEditKey(key) },
                        onDelete = { onDeleteKey(key) },
                        onTest = { onTestKey(key) }
                    )
                }

                if (filteredKeys.isEmpty()) {
                    item {
                        EmptyKeysState(
                            searchQuery = searchQuery,
                            onAddKey = onAddKey,
                            onClearFilters = {
                                searchQuery = ""
                                selectedKeyType = null
                                selectedStatus = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KeySearchAndFilters(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedKeyType: KeyType?,
    onKeyTypeChange: (KeyType?) -> Unit,
    selectedStatus: KeyStatus?,
    onStatusChange: (KeyStatus?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search keys by name or type...") },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = "Search",
                    modifier = Modifier.size(20.dp)
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
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Clear,
                            contentDescription = "Clear",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        // Filters Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Key Type Filter
            Box(modifier = Modifier.weight(1f)) {
                KeyTypeFilterDropdown(
                    selectedKeyType = selectedKeyType,
                    onSelectionChange = onKeyTypeChange
                )
            }

            // Status Filter
            Box(modifier = Modifier.weight(1f)) {
                StatusFilterDropdown(
                    selectedStatus = selectedStatus,
                    onSelectionChange = onStatusChange
                )
            }

            // Clear Filters Button
            if (selectedKeyType != null || selectedStatus != null) {
                OutlinedButton(
                    onClick = {
                        onKeyTypeChange(null)
                        onStatusChange(null)
                    },
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(
                        Icons.Outlined.FilterAltOff,
                        contentDescription = "Clear Filters",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear")
                }
            }
        }
    }
}

@Composable
fun KeyTypeFilterDropdown(
    selectedKeyType: KeyType?,
    onSelectionChange: (KeyType?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = selectedKeyType?.displayName ?: "All Key Types",
            onValueChange = { },
            label = { Text("Key Type") },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            leadingIcon = selectedKeyType?.let { keyType ->
                {
                    Icon(
                        imageVector = keyType.icon,
                        contentDescription = null,
                        tint = keyType.color,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            trailingIcon = {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand"
                )
            },
            shape = RoundedCornerShape(8.dp)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                onClick = {
                    onSelectionChange(null)
                    expanded = false
                }
            ) {
                Text("All Key Types")
            }

            Divider()

            KeyType.values().forEach { keyType ->
                DropdownMenuItem(
                    onClick = {
                        onSelectionChange(keyType)
                        expanded = false
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = keyType.icon,
                            contentDescription = null,
                            tint = keyType.color,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(keyType.displayName)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusFilterDropdown(
    selectedStatus: KeyStatus?,
    onSelectionChange: (KeyStatus?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = selectedStatus?.displayName ?: "All Statuses",
            onValueChange = { },
            label = { Text("Status") },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            leadingIcon = selectedStatus?.let { status ->
                {
                    Icon(
                        imageVector = status.icon,
                        contentDescription = null,
                        tint = status.color,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            trailingIcon = {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand"
                )
            },
            shape = RoundedCornerShape(8.dp)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                onClick = {
                    onSelectionChange(null)
                    expanded = false
                }
            ) {
                Text("All Statuses")
            }

            Divider()

            KeyStatus.values().forEach { status ->
                DropdownMenuItem(
                    onClick = {
                        onSelectionChange(status)
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
                        Text(status.displayName)
                    }
                }
            }
        }
    }
}

// --- KEY TABLE COMPONENTS ---

@Composable
fun KeyTableHeader() {
    Card(
        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.08f),
        elevation = 0.dp,
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Key Name",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(2f)
            )
            Text(
                text = "Type",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1.5f)
            )
            Text(
                text = "Algorithm",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1.5f)
            )
            Text(
                text = "Length",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Status",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Actions",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1.5f)
            )
        }
    }
}

@Composable
fun KeyTableRow(
    key: CryptoKey,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Key Name with Icon
            Row(
                modifier = Modifier.weight(2f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = key.type.icon,
                    contentDescription = key.type.displayName,
                    tint = key.type.color,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = key.name,
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (key.isHsmStored) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Hardware,
                                contentDescription = "HSM Stored",
                                tint = Color(0xFFED6C02),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "HSM",
                                style = MaterialTheme.typography.caption,
                                color = Color(0xFFED6C02)
                            )
                        }
                    }
                }
            }

            // Key Type
            Text(
                text = key.type.displayName,
                style = MaterialTheme.typography.body2,
                modifier = Modifier.weight(1.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Algorithm
            Text(
                text = key.algorithm.displayName,
                style = MaterialTheme.typography.body2,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1.5f)
            )

            // Key Length
            Text(
                text = "${key.keyLength} bits",
                style = MaterialTheme.typography.body2,
                modifier = Modifier.weight(1f)
            )

            // Status
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = key.status.icon,
                    contentDescription = key.status.displayName,
                    tint = key.status.color,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = key.status.displayName,
                    style = MaterialTheme.typography.caption,
                    color = key.status.color,
                    fontWeight = FontWeight.Medium
                )
            }

            // Actions
            Row(
                modifier = Modifier.weight(1.5f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onTest,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.PlayArrow,
                        contentDescription = "Test",
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colors.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
}

@Composable
fun EmptyKeysState(
    searchQuery: String,
    onAddKey: () -> Unit,
    onClearFilters: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Outlined.Key,
                contentDescription = "No keys",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
            )

            Text(
                text = if (searchQuery.isNotEmpty()) "No keys match your search" else "No cryptographic keys found",
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Text(
                text = if (searchQuery.isNotEmpty())
                    "Try adjusting your search or filters"
                else
                    "Get started by adding your first cryptographic key",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (searchQuery.isNotEmpty()) {
                    OutlinedButton(onClick = onClearFilters) {
                        Text("Clear Filters")
                    }
                }

                Button(onClick = onAddKey) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = "Add",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add First Key")
                }
            }
        }
    }
}

// --- CERTIFICATE MANAGEMENT SECTION ---

@Composable
fun CertificateManagementSection(
    certificates: List<Certificate>,
    onImportCertificate: () -> Unit,
    onExportCertificate: (Certificate) -> Unit,
    onValidateChain: (Certificate) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<KeyStatus?>(null) }

    val filteredCertificates = certificates.filter { cert ->
        val matchesSearch = searchQuery.isEmpty() ||
                cert.commonName.contains(searchQuery, ignoreCase = true) ||
                cert.issuer.contains(searchQuery, ignoreCase = true)
        val matchesStatus = selectedStatus == null || cert.status == selectedStatus

        matchesSearch && matchesStatus
    }

    Column(modifier = Modifier.padding(16.dp)) {

        // Header with Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Digital Certificates",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${filteredCertificates.size} of ${certificates.size} certificates",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { /* TODO: Validate all chains */ },
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.VerifiedUser,
                        contentDescription = "Validate",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Validate All")
                }

                Button(
                    onClick = onImportCertificate,
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.Upload,
                        contentDescription = "Import",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import Cert")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search and Filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search certificates...") },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(20.dp)
                    )
                },
                modifier = Modifier.weight(2f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            Box(modifier = Modifier.weight(1f)) {
                StatusFilterDropdown(
                    selectedStatus = selectedStatus,
                    onSelectionChange = { selectedStatus = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Certificates List
        Card(
            modifier = Modifier.fillMaxSize(),
            elevation = 1.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(filteredCertificates, key = { it.id }) { certificate ->
                    CertificateCard(
                        certificate = certificate,
                        onExport = { onExportCertificate(certificate) },
                        onValidateChain = { onValidateChain(certificate) }
                    )
                }

                if (filteredCertificates.isEmpty()) {
                    item {
                        EmptyCertificatesState(
                            searchQuery = searchQuery,
                            onImportCertificate = onImportCertificate,
                            onClearFilters = {
                                searchQuery = ""
                                selectedStatus = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CertificateCard(
    certificate: Certificate,
    onExport: () -> Unit,
    onValidateChain: () -> Unit
) {
    val isExpiringSoon = certificate.validTo.isBefore(LocalDateTime.now().plusDays(30))

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = when {
            certificate.status == KeyStatus.EXPIRED -> Color(0xFFD32F2F).copy(alpha = 0.05f)
            isExpiringSoon -> Color(0xFFED6C02).copy(alpha = 0.05f)
            else -> MaterialTheme.colors.surface
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Outlined.Note,
                        contentDescription = "Certificate",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = certificate.commonName,
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Issued by: ${certificate.issuer}",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Status Badge
                Card(
                    backgroundColor = certificate.status.color,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = certificate.status.icon,
                            contentDescription = certificate.status.displayName,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = certificate.status.displayName.uppercase(),
                            style = MaterialTheme.typography.caption,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Certificate Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CertificateDetailItem("Algorithm", certificate.algorithm)
                CertificateDetailItem("Key Length", "${certificate.keyLength} bits")
                CertificateDetailItem(
                    "Valid Until",
                    certificate.validTo.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                )
            }

            if (certificate.usage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Usage: ${certificate.usage.joinToString(", ")}",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onValidateChain,
                    modifier = Modifier.weight(1f).height(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.VerifiedUser,
                        contentDescription = "Validate",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Validate", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onExport,
                    modifier = Modifier.weight(1f).height(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.Download,
                        contentDescription = "Export",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun CertificateDetailItem(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EmptyCertificatesState(
    searchQuery: String,
    onImportCertificate: () -> Unit,
    onClearFilters: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Outlined.Note,
                contentDescription = "No certificates",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
            )

            Text(
                text = if (searchQuery.isNotEmpty()) "No certificates match your search" else "No certificates found",
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Text(
                text = if (searchQuery.isNotEmpty())
                    "Try adjusting your search criteria"
                else
                    "Import certificates to get started",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (searchQuery.isNotEmpty()) {
                    OutlinedButton(onClick = onClearFilters) {
                        Text("Clear Search")
                    }
                }

                Button(onClick = onImportCertificate) {
                    Icon(
                        Icons.Outlined.Upload,
                        contentDescription = "Import",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import Certificate")
                }
            }
        }
    }
}

// --- CRYPTOGRAPHIC SETTINGS SECTION ---

@Composable
fun CryptographicSettingsSection(
    settings: CryptoSettings,
    onSettingsChange: (CryptoSettings) -> Unit
) {
    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Cryptographic Configuration",
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            CryptoSettingsSection(
                title = "Default Algorithms",
                icon = Icons.Outlined.EnhancedEncryption
            ) {
                CryptoDropdownField(
                    label = "Default Symmetric Algorithm",
                    selectedValue = settings.defaultSymmetricAlgorithm.displayName,
                    options = Algorithm.values().filter { it.category == "Symmetric" }.map { it.displayName },
                    onSelectionChange = { algorithmName ->
                        val algorithm = Algorithm.values().find { it.displayName == algorithmName }
                        algorithm?.let {
                            onSettingsChange(settings.copy(defaultSymmetricAlgorithm = it))
                        }
                    }
                )

                CryptoDropdownField(
                    label = "Default Asymmetric Algorithm",
                    selectedValue = settings.defaultAsymmetricAlgorithm.displayName,
                    options = Algorithm.values().filter { it.category == "Asymmetric" }.map { it.displayName },
                    onSelectionChange = { algorithmName ->
                        val algorithm = Algorithm.values().find { it.displayName == algorithmName }
                        algorithm?.let {
                            onSettingsChange(settings.copy(defaultAsymmetricAlgorithm = it))
                        }
                    }
                )
            }
        }

        item {
            CryptoSettingsSection(
                title = "Key Derivation",
                icon = Icons.Outlined.Transform
            ) {
                CryptoDropdownField(
                    label = "Key Derivation Function",
                    selectedValue = settings.keyDerivationFunction,
                    options = listOf("PBKDF2", "HKDF", "scrypt", "Argon2"),
                    onSelectionChange = { function ->
                        onSettingsChange(settings.copy(keyDerivationFunction = function))
                    }
                )

                CryptoTextField(
                    label = "Derivation Iterations",
                    value = settings.keyDerivationIterations.toString(),
                    onValueChange = { value ->
                        val iterations = value.toIntOrNull() ?: settings.keyDerivationIterations
                        onSettingsChange(settings.copy(keyDerivationIterations = iterations))
                    },
                    keyboardType = KeyboardType.Number
                )
            }
        }

        item {
            CryptoSettingsSection(
                title = "Random Number Generation",
                icon = Icons.Outlined.Shuffle
            ) {
                CryptoDropdownField(
                    label = "RNG Provider",
                    selectedValue = settings.randomNumberGenerator,
                    options = listOf("SecureRandom", "Hardware RNG", "TRNG", "PRNG"),
                    onSelectionChange = { rng ->
                        onSettingsChange(settings.copy(randomNumberGenerator = rng))
                    }
                )
            }
        }

        item {
            CryptoSettingsSection(
                title = "Hardware Security Module (HSM)",
                icon = Icons.Outlined.Hardware
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Enable HSM Integration",
                            style = MaterialTheme.typography.body2,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Store and manage keys in hardware security module",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Switch(
                        checked = settings.hsmEnabled,
                        onCheckedChange = { enabled ->
                            onSettingsChange(settings.copy(hsmEnabled = enabled))
                        }
                    )
                }

                AnimatedVisibility(
                    visible = settings.hsmEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CryptoDropdownField(
                            label = "HSM Provider",
                            selectedValue = settings.hsmProvider,
                            options = listOf("SafeNet Luna", "Thales nShield", "Utimaco CryptoServer", "AWS CloudHSM"),
                            onSelectionChange = { provider ->
                                onSettingsChange(settings.copy(hsmProvider = provider))
                            }
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CryptoTextField(
                                label = "HSM Slot",
                                value = settings.hsmSlot.toString(),
                                onValueChange = { value ->
                                    val slot = value.toIntOrNull() ?: settings.hsmSlot
                                    onSettingsChange(settings.copy(hsmSlot = slot))
                                },
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(1f)
                            )

                            CryptoTextField(
                                label = "HSM PIN",
                                value = settings.hsmPin,
                                onValueChange = { pin ->
                                    onSettingsChange(settings.copy(hsmPin = pin))
                                },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { /* TODO: Test HSM connection */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Outlined.Cable,
                                    contentDescription = "Test Connection",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test Connection")
                            }

                            OutlinedButton(
                                onClick = { /* TODO: HSM diagnostics */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Outlined.Dock,
                                    contentDescription = "Diagnostics",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Diagnostics")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CryptoSettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )
            }
            content()
        }
    }
}

@Composable
fun CryptoTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(8.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = MaterialTheme.colors.primary,
            unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
        )
    )
}

@Composable
fun CryptoDropdownField(
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
            label = { Text(label) },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            trailingIcon = {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand"
                )
            },
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
            )
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
                    Text(option)
                }
            }
        }
    }
}

// --- QUICK ACTIONS ---

@Composable
fun CryptoQuickActions(
    activeSection: String,
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
                    "Common cryptographic operations",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (activeSection) {
                    "keys" -> {
                        OutlinedButton(
                            onClick = { onQuickAction("validate_all") },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                Icons.Outlined.VerifiedUser,
                                contentDescription = "Validate All",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Validate All")
                        }

                        Button(
                            onClick = { onQuickAction("generate_key") },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Add,
                                contentDescription = "Generate",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Generate Key")
                        }
                    }
                    "certificates" -> {
                        OutlinedButton(
                            onClick = { onQuickAction("validate_all") },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                Icons.Outlined.VerifiedUser,
                                contentDescription = "Validate Chains",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Validate Chains")
                        }

                        Button(
                            onClick = { onQuickAction("import_certificate") },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Upload,
                                contentDescription = "Import",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import Cert")
                        }
                    }
                    "settings" -> {
                        OutlinedButton(
                            onClick = { onQuickAction("reset_defaults") },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Refresh,
                                contentDescription = "Reset",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Defaults")
                        }

                        Button(
                            onClick = { onQuickAction("apply_settings") },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = "Apply",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Apply Settings")
                        }
                    }
                }
            }
        }
    }
}

// --- KEY GENERATION WIZARD DIALOG ---

@Composable
fun KeyGenerationWizard(
    existingKey: CryptoKey?,
    onDismiss: () -> Unit,
    onSave: (CryptoKey) -> Unit
) {
    val isEditing = existingKey != null

    var keyName by remember { mutableStateOf(existingKey?.name ?: "") }
    var selectedKeyType by remember { mutableStateOf(existingKey?.type ?: KeyType.APPLICATION) }
    var selectedAlgorithm by remember { mutableStateOf(existingKey?.algorithm ?: Algorithm.AES_256) }
    var keyLength by remember { mutableStateOf(existingKey?.keyLength?.toString() ?: "256") }
    var usage by remember { mutableStateOf(existingKey?.usage ?: "") }
    var storeInHsm by remember { mutableStateOf(existingKey?.isHsmStored ?: false) }
    var currentStep by remember { mutableStateOf(0) }

    val steps = listOf("Basic Info", "Algorithm", "Options", "Review")

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
                    Column {
                        Text(
                            text = if (isEditing) "Edit Cryptographic Key" else "Key Generation Wizard",
                            style = MaterialTheme.typography.h6,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Step ${currentStep + 1} of ${steps.size}: ${steps[currentStep]}",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Indicator
                LinearProgressIndicator(
                    progress = (currentStep + 1).toFloat() / steps.size,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colors.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Step Content
                Box(modifier = Modifier.weight(1f)) {
                    when (currentStep) {
                        0 -> {
                            KeyWizardBasicInfo(
                                keyName = keyName,
                                onKeyNameChange = { keyName = it },
                                selectedKeyType = selectedKeyType,
                                onKeyTypeChange = { selectedKeyType = it },
                                usage = usage,
                                onUsageChange = { usage = it }
                            )
                        }
                        1 -> {
                            KeyWizardAlgorithm(
                                selectedAlgorithm = selectedAlgorithm,
                                onAlgorithmChange = { selectedAlgorithm = it },
                                keyLength = keyLength,
                                onKeyLengthChange = { keyLength = it }
                            )
                        }
                        2 -> {
                            KeyWizardOptions(
                                storeInHsm = storeInHsm,
                                onStoreInHsmChange = { storeInHsm = it }
                            )
                        }
                        3 -> {
                            KeyWizardReview(
                                keyName = keyName,
                                keyType = selectedKeyType,
                                algorithm = selectedAlgorithm,
                                keyLength = keyLength,
                                usage = usage,
                                storeInHsm = storeInHsm
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Navigation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = {
                            if (currentStep > 0) {
                                currentStep--
                            } else {
                                onDismiss()
                            }
                        }
                    ) {
                        Text(if (currentStep == 0) "Cancel" else "Previous")
                    }

                    Button(
                        onClick = {
                            if (currentStep < steps.size - 1) {
                                currentStep++
                            } else {
                                // Generate/Save key
                                val newKey = CryptoKey(
                                    id = existingKey?.id ?: UUID.randomUUID().toString(),
                                    name = keyName,
                                    type = selectedKeyType,
                                    algorithm = selectedAlgorithm,
                                    keyLength = keyLength.toIntOrNull() ?: 256,
                                    status = KeyStatus.VALID,
                                    usage = usage,
                                    isHsmStored = storeInHsm,
                                    keyValue = "Generated_Key_Value_Placeholder", // In real app, this would be generated
                                    checkValue = "ABC123" // In real app, this would be calculated
                                )
                                onSave(newKey)
                            }
                        },
                        enabled = keyName.isNotBlank()
                    ) {
                        Text(if (currentStep == steps.size - 1) "Generate Key" else "Next")
                    }
                }
            }
        }
    }
}

@Composable
fun KeyWizardBasicInfo(
    keyName: String,
    onKeyNameChange: (String) -> Unit,
    selectedKeyType: KeyType,
    onKeyTypeChange: (KeyType) -> Unit,
    usage: String,
    onUsageChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CryptoTextField(
            label = "Key Name *",
            value = keyName,
            onValueChange = onKeyNameChange
        )

        CryptoDropdownField(
            label = "Key Type",
            selectedValue = selectedKeyType.displayName,
            options = KeyType.values().map { it.displayName },
            onSelectionChange = { typeName ->
                val keyType = KeyType.values().find { it.displayName == typeName }
                keyType?.let { onKeyTypeChange(it) }
            }
        )

        CryptoTextField(
            label = "Usage Description",
            value = usage,
            onValueChange = onUsageChange
        )
    }
}

@Composable
fun KeyWizardAlgorithm(
    selectedAlgorithm: Algorithm,
    onAlgorithmChange: (Algorithm) -> Unit,
    keyLength: String,
    onKeyLengthChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Algorithm Selection",
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.Bold
        )

        CryptoDropdownField(
            label = "Cryptographic Algorithm",
            selectedValue = selectedAlgorithm.displayName,
            options = Algorithm.values().map { it.displayName },
            onSelectionChange = { algorithmName ->
                val algorithm = Algorithm.values().find { it.displayName == algorithmName }
                algorithm?.let { onAlgorithmChange(it) }
            }
        )

        CryptoTextField(
            label = "Key Length (bits)",
            value = keyLength,
            onValueChange = onKeyLengthChange,
            keyboardType = KeyboardType.Number
        )

        Card(
            backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.05f),
            elevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "Algorithm Info",
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Category: ${selectedAlgorithm.category}",
                    style = MaterialTheme.typography.caption
                )
                Text(
                    "Recommended for: ${getAlgorithmRecommendation(selectedAlgorithm)}",
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}

@Composable
fun KeyWizardOptions(
    storeInHsm: Boolean,
    onStoreInHsmChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Key Storage Options",
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Store in Hardware Security Module",
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Enhanced security with hardware-based key storage",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }

                Switch(
                    checked = storeInHsm,
                    onCheckedChange = onStoreInHsmChange
                )
            }
        }
    }
}

@Composable
fun KeyWizardReview(
    keyName: String,
    keyType: KeyType,
    algorithm: Algorithm,
    keyLength: String,
    usage: String,
    storeInHsm: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Review Key Configuration",
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                KeyReviewItem("Key Name", keyName)
                KeyReviewItem("Key Type", keyType.displayName)
                KeyReviewItem("Algorithm", algorithm.displayName)
                KeyReviewItem("Key Length", "$keyLength bits")
                if (usage.isNotBlank()) {
                    KeyReviewItem("Usage", usage)
                }
                KeyReviewItem("Storage", if (storeInHsm) "Hardware Security Module" else "Software")
            }
        }

        Card(
            backgroundColor = Color(0xFF197A3E).copy(alpha = 0.1f),
            elevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = "Info",
                    tint = Color(0xFF197A3E),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "The key will be generated securely and stored according to your specifications.",
                    style = MaterialTheme.typography.caption,
                    color = Color(0xFF197A3E)
                )
            }
        }
    }
}

@Composable
fun KeyReviewItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Medium
        )
    }
}

// --- CERTIFICATE IMPORT DIALOG ---

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CertificateImportDialog(
    onDismiss: () -> Unit,
    onImport: (Certificate) -> Unit
) {
    var importType by remember { mutableStateOf("file") }
    var certificateData by remember { mutableStateOf("") }
    var certificateName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(450.dp)
                .heightIn(max = 500.dp),
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
                        text = "Import Certificate",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Import Type Selection
                Text(
                    "Import Method",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = importType == "file",
                        onClick = { importType = "file" },
                    ){ Text("From File") }
                    FilterChip(
                        selected = importType == "paste",
                        onClick = { importType = "paste" },
                    ){ Text("Paste Data") }
                    FilterChip(
                        selected = importType == "url",
                        onClick = { importType = "url" },
                    ){ Text("From URL") }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Import Content
                when (importType) {
                    "file" -> {
                        OutlinedButton(
                            onClick = { /* TODO: File picker */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Outlined.Upload,
                                contentDescription = "Upload",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Certificate File")
                        }
                    }
                    "paste" -> {
                        OutlinedTextField(
                            value = certificateData,
                            onValueChange = { certificateData = it },
                            label = { Text("Certificate Data (PEM/DER)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            placeholder = { Text("-----BEGIN CERTIFICATE-----\n...") }
                        )
                    }
                    "url" -> {
                        CryptoTextField(
                            label = "Certificate URL",
                            value = certificateData,
                            onValueChange = { certificateData = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                CryptoTextField(
                    label = "Certificate Name (Optional)",
                    value = certificateName,
                    onValueChange = { certificateName = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            // Create mock certificate for demo
                            val certificate = Certificate(
                                commonName = certificateName.ifEmpty { "Imported Certificate" },
                                issuer = "Unknown Issuer",
                                subject = "CN=${certificateName.ifEmpty { "Imported Certificate" }}",
                                serialNumber = UUID.randomUUID().toString().take(16).uppercase(),
                                algorithm = "RSA with SHA-256",
                                keyLength = 2048,
                                validFrom = LocalDateTime.now(),
                                validTo = LocalDateTime.now().plusYears(1),
                                fingerprint = "XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX",
                                usage = listOf("Digital Signature"),
                                status = KeyStatus.VALID
                            )
                            onImport(certificate)
                        },
                        enabled = certificateData.isNotBlank() || importType == "file"
                    ) {
                        Text("Import Certificate")
                    }
                }
            }
        }
    }
}

// --- UTILITY FUNCTIONS ---

fun getAlgorithmRecommendation(algorithm: Algorithm): String {
    return when (algorithm) {
        Algorithm.AES_256 -> "High security symmetric encryption"
        Algorithm.AES_128 -> "Standard symmetric encryption, good performance"
        Algorithm.TRIPLE_DES -> "Legacy systems, consider upgrading"
        Algorithm.RSA_2048 -> "Standard asymmetric encryption"
        Algorithm.RSA_4096 -> "High security asymmetric encryption"
        Algorithm.ECC_P256 -> "Efficient asymmetric encryption"
        else -> "General purpose cryptographic operations"
    }
}