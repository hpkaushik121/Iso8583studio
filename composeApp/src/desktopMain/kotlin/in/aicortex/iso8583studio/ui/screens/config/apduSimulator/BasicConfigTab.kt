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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

// --- DATA MODELS ---

enum class CardType(
    val displayName: String,
    val prefix: String,
    val icon: ImageVector,
    val color: Color
) {
    VISA("Visa", "4", Icons.Outlined.CreditCard, Color(0xFF1A1F71)),
    MASTERCARD("Mastercard", "5", Icons.Outlined.CreditCard, Color(0xFFEB001B)),
    AMERICAN_EXPRESS("American Express", "3", Icons.Outlined.CreditCard, Color(0xFF006FCF)),
    DISCOVER("Discover", "6", Icons.Outlined.CreditCard, Color(0xFFFF6000)),
    DINERS_CLUB("Diners Club", "3", Icons.Outlined.CreditCard, Color(0xFF0079BE)),
    JCB("JCB", "3", Icons.Outlined.CreditCard, Color(0xFF006FC7)),
    UNIONPAY("UnionPay", "6", Icons.Outlined.CreditCard, Color(0xFFE21836)),
    CUSTOM("Custom", "", Icons.Outlined.CreditCard, Color(0xFF795548))
}

enum class EmvVersion(val displayName: String, val version: String) {
    EMV_4_1("EMV 4.1", "4.1"),
    EMV_4_2("EMV 4.2", "4.2"),
    EMV_4_3("EMV 4.3", "4.3"),
    EMV_4_4("EMV 4.4", "4.4")
}

enum class CardState(val displayName: String, val description: String, val color: Color) {
    ACTIVE("Active", "Card is ready for transactions", Color(0xFF197A3E)),
    BLOCKED("Blocked", "Card is temporarily blocked", Color(0xFFED6C02)),
    EXPIRED("Expired", "Card has expired", Color(0xFFD32F2F)),
    SUSPENDED("Suspended", "Card is suspended by issuer", Color(0xFF795548)),
    LOST_STOLEN("Lost/Stolen", "Card reported as lost or stolen", Color(0xFFD32F2F))
}

enum class CommandType(val displayName: String, val defaultTimeout: Int) {
    SELECT("SELECT", 3000),
    READ_RECORD("READ RECORD", 2000),
    GET_PROCESSING_OPTIONS("GET PROCESSING OPTIONS", 2500),
    GENERATE_AC("GENERATE AC", 4000),
    VERIFY_PIN("VERIFY PIN", 5000),
    INTERNAL_AUTHENTICATE("INTERNAL AUTHENTICATE", 3500)
}

data class InterfaceSettings(
    val contactEnabled: Boolean = true,
    val contactlessEnabled: Boolean = true,
    val magneticStripeEnabled: Boolean = false,
    val contactAdvanced: ContactSettings = ContactSettings(),
    val contactlessAdvanced: ContactlessSettings = ContactlessSettings(),
    val magneticStripeAdvanced: MagneticStripeSettings = MagneticStripeSettings()
)

data class ContactSettings(
    val voltage: String = "1.8V",
    val frequency: String = "4.0MHz",
    val baudRate: String = "9600",
    val protocol: String = "T=0"
)

data class ContactlessSettings(
    val technology: String = "ISO 14443 Type A",
    val dataRate: String = "106 kbps",
    val maxDistance: String = "4 cm",
    val powerSaving: Boolean = true
)

data class MagneticStripeSettings(
    val track1Enabled: Boolean = true,
    val track2Enabled: Boolean = true,
    val track3Enabled: Boolean = false,
    val coercivity: String = "300 Oe"
)

data class ResponseTiming(
    val processingDelay: Int = 500,
    val randomVariance: Boolean = false,
    val varianceRange: IntRange = 0..100,
    val commandTimeouts: Map<CommandType, Int> = CommandType.values()
        .associateWith { it.defaultTimeout }
)

data class CardStateManagement(
    val initialState: CardState = CardState.ACTIVE,
    val pinAttemptsRemaining: Int = 3,
    val dailyTransactionLimit: Int = 50,
    val dailyTransactionCount: Int = 0,
    val blockingConditions: BlockingConditions = BlockingConditions()
)

data class BlockingConditions(
    val maxFailedPinAttempts: Int = 3,
    val maxDailyTransactions: Boolean = true,
    val suspiciousActivityDetection: Boolean = true,
    val velocityChecking: Boolean = false,
    val geographicRestrictions: Boolean = false
)

data class BasicCardConfiguration(
    val id: String = UUID.randomUUID().toString(),
    val profileName: String = "",
    val cardType: CardType = CardType.VISA,
    val emvVersion: EmvVersion = EmvVersion.EMV_4_3,
    val cardNumber: String = "",
    val expiryDate: LocalDate = LocalDate.now().plusYears(3),
    val cvv: String = "",
    val cardholderName: String = "",
    val interfaceSettings: InterfaceSettings = InterfaceSettings(),
    val responseTiming: ResponseTiming = ResponseTiming(),
    val stateManagement: CardStateManagement = CardStateManagement(),
    val createdDate: LocalDate = LocalDate.now(),
    val lastModified: LocalDate = LocalDate.now()
)

// --- MAIN BASIC CARD CONFIGURATION TAB ---

@Composable
fun BasicConfigTab() {
    var configuration by remember { mutableStateOf(BasicCardConfiguration()) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showValidationDialog by remember { mutableStateOf(false) }
    var validationErrors by remember { mutableStateOf<List<String>>(emptyList()) }

    fun updateConfiguration(newConfig: BasicCardConfiguration) {
        configuration = newConfig
        hasUnsavedChanges = true
    }

    fun validateConfiguration(): List<String> {
        val errors = mutableListOf<String>()

        if (configuration.profileName.isBlank()) {
            errors.add("Profile name is required")
        }

        if (configuration.cardNumber.isBlank()) {
            errors.add("Card number is required")
        } else if (!isValidCardNumber(configuration.cardNumber)) {
            errors.add("Invalid card number format")
        }

        if (configuration.cvv.isBlank()) {
            errors.add("CVV is required")
        } else if (configuration.cvv.length !in 3..4) {
            errors.add("CVV must be 3 or 4 digits")
        }

        if (configuration.cardholderName.isBlank()) {
            errors.add("Cardholder name is required")
        }

        return errors
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Header with Configuration Status
        BasicCardHeader(
            configuration = configuration,
            hasUnsavedChanges = hasUnsavedChanges,
            isLoading = isLoading,
            onSave = {
                val errors = validateConfiguration()
                if (errors.isEmpty()) {
                    isLoading = true
                    MainScope().launch {
                        delay(1000)
                        hasUnsavedChanges = false
                        isLoading = false
                    }
                } else {
                    validationErrors = errors
                    showValidationDialog = true
                }
            },
            onValidate = {
                validationErrors = validateConfiguration()
                showValidationDialog = true
            },
            onReset = {
                configuration = BasicCardConfiguration()
                hasUnsavedChanges = false
            }
        )

        // Main Configuration Content
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 500.dp, max = 700.dp),
            elevation = 2.dp,
            shape = RoundedCornerShape(12.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card Identity Section
                item {
                    CardIdentitySection(
                        configuration = configuration,
                        onConfigurationChange = ::updateConfiguration
                    )
                }

                // Interface Configuration Section
                item {
                    InterfaceConfigurationSection(
                        interfaceSettings = configuration.interfaceSettings,
                        onSettingsChange = { newSettings ->
                            updateConfiguration(configuration.copy(interfaceSettings = newSettings))
                        }
                    )
                }

                // Response Timing Section
                item {
                    ResponseTimingSection(
                        timing = configuration.responseTiming,
                        onTimingChange = { newTiming ->
                            updateConfiguration(configuration.copy(responseTiming = newTiming))
                        }
                    )
                }

                // Card State Management Section
                item {
                    CardStateManagementSection(
                        stateManagement = configuration.stateManagement,
                        onStateManagementChange = { newStateManagement ->
                            updateConfiguration(configuration.copy(stateManagement = newStateManagement))
                        }
                    )
                }

                // Add some bottom padding for better scroll experience
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Quick Actions
        BasicCardQuickActions(
            configuration = configuration,
            hasUnsavedChanges = hasUnsavedChanges,
            onQuickAction = { action ->
                when (action) {
                    "generate_card_number" -> {
                        val newCardNumber = generateCardNumber(configuration.cardType)
                        updateConfiguration(configuration.copy(cardNumber = newCardNumber))
                    }

                    "generate_cvv" -> {
                        val newCvv = generateCvv()
                        updateConfiguration(configuration.copy(cvv = newCvv))
                    }

                    "quick_setup" -> {
                        val quickConfig = createQuickSetupConfiguration()
                        updateConfiguration(quickConfig)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Validation Dialog
    if (showValidationDialog) {
        ValidationDialog(
            errors = validationErrors,
            onDismiss = { showValidationDialog = false }
        )
    }
}

// --- BASIC CARD HEADER ---

@Composable
fun BasicCardHeader(
    configuration: BasicCardConfiguration,
    hasUnsavedChanges: Boolean,
    isLoading: Boolean,
    onSave: () -> Unit,
    onValidate: () -> Unit,
    onReset: () -> Unit
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
                        text = "Basic Card Configuration",
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onValidate,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(
                            Icons.Outlined.VerifiedUser,
                            contentDescription = "Validate",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Validate")
                    }

                    OutlinedButton(
                        onClick = onReset,
                        enabled = hasUnsavedChanges,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = "Reset",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset")
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
                        Text("Save Configuration")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Configuration Summary Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ConfigSummaryCard(
                    title = "Profile",
                    value = configuration.profileName.ifEmpty { "Unnamed" },
                    subtitle = configuration.cardType.displayName,
                    icon = Icons.Outlined.Person,
                    color = Color(0xFF6750A4)
                )

                ConfigSummaryCard(
                    title = "Card Number",
                    value = if (configuration.cardNumber.isNotEmpty())
                        "**** **** **** ${configuration.cardNumber.takeLast(4)}"
                    else "Not Set",
                    subtitle = "Expires ${
                        configuration.expiryDate.format(
                            DateTimeFormatter.ofPattern(
                                "MM/yy"
                            )
                        )
                    }",
                    icon = Icons.Outlined.CreditCard,
                    color = Color(0xFF0B6BCB)
                )

                ConfigSummaryCard(
                    title = "Interfaces",
                    value = getEnabledInterfacesCount(configuration.interfaceSettings).toString(),
                    subtitle = "Active interfaces",
                    icon = Icons.Outlined.Contactless,
                    color = Color(0xFF197A3E)
                )

                ConfigSummaryCard(
                    title = "Card State",
                    value = configuration.stateManagement.initialState.displayName,
                    subtitle = "${configuration.stateManagement.pinAttemptsRemaining} PIN attempts",
                    icon = Icons.Outlined.Security,
                    color = configuration.stateManagement.initialState.color
                )
            }
        }
    }
}



@Composable
fun ConfigSummaryCard(
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
        modifier = Modifier.width(140.dp)
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
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// --- CARD IDENTITY SECTION ---

@Composable
fun CardIdentitySection(
    configuration: BasicCardConfiguration,
    onConfigurationChange: (BasicCardConfiguration) -> Unit
) {
    var showCardNumber by remember { mutableStateOf(false) }
    var showCvv by remember { mutableStateOf(false) }

    ConfigurationSection(
        title = "Card Identity",
        icon = Icons.Outlined.CreditCard,
        description = "Configure basic card information and identity details"
    ) {
        // Profile Name
        ConfigTextField(
            label = "Profile Name *",
            value = configuration.profileName,
            onValueChange = {
                onConfigurationChange(configuration.copy(profileName = it))
            },
            placeholder = "Enter profile name",
            leadingIcon = Icons.Outlined.Person
        )

        // Card Type and EMV Version Row
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                CardTypeDropdown(
                    selectedType = configuration.cardType,
                    onTypeChange = {
                        onConfigurationChange(configuration.copy(cardType = it))
                    }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                EmvVersionDropdown(
                    selectedVersion = configuration.emvVersion,
                    onVersionChange = {
                        onConfigurationChange(configuration.copy(emvVersion = it))
                    }
                )
            }
        }

        // Card Number with Show/Hide Toggle
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ConfigTextField(
                label = "Card Number *",
                value = configuration.cardNumber,
                onValueChange = {
                    val cleaned = it.replace(" ", "").filter { char -> char.isDigit() }
                    if (cleaned.length <= 19) {
                        onConfigurationChange(configuration.copy(cardNumber = cleaned))
                    }
                },
                placeholder = "Enter card number",
                leadingIcon = configuration.cardType.icon,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = { showCardNumber = !showCardNumber },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    if (showCardNumber) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (showCardNumber) "Hide card number" else "Show card number",
                    modifier = Modifier.size(20.dp)
                )
            }

            OutlinedButton(
                onClick = {
                    val newCardNumber = generateCardNumber(configuration.cardType)
                    onConfigurationChange(configuration.copy(cardNumber = newCardNumber))
                },
                modifier = Modifier.height(56.dp)
            ) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = "Generate",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Generate")
            }
        }

        // Expiry Date and CVV Row
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                ExpiryDatePicker(
                    selectedDate = configuration.expiryDate,
                    onDateChange = {
                        onConfigurationChange(configuration.copy(expiryDate = it))
                    }
                )
            }

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                ConfigTextField(
                    label = "CVV *",
                    value = configuration.cvv,
                    onValueChange = {
                        val cleaned = it.filter { char -> char.isDigit() }
                        if (cleaned.length <= 4) {
                            onConfigurationChange(configuration.copy(cvv = cleaned))
                        }
                    },
                    placeholder = "CVV",
                    leadingIcon = Icons.Outlined.Security,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { showCvv = !showCvv },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        if (showCvv) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (showCvv) "Hide CVV" else "Show CVV",
                        modifier = Modifier.size(20.dp)
                    )
                }

                OutlinedButton(
                    onClick = {
                        val newCvv = generateCvv()
                        onConfigurationChange(configuration.copy(cvv = newCvv))
                    },
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "Generate",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Cardholder Name
        ConfigTextField(
            label = "Cardholder Name *",
            value = configuration.cardholderName,
            onValueChange = {
                onConfigurationChange(configuration.copy(cardholderName = it.uppercase()))
            },
            placeholder = "Enter cardholder name",
            leadingIcon = Icons.Outlined.Badge
        )

        // Card Preview
        CardPreview(configuration = configuration)
    }
}

// --- CARD PREVIEW ---

@Composable
fun CardPreview(configuration: BasicCardConfiguration) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = configuration.cardType.color.copy(alpha = 0.1f)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = configuration.profileName.ifEmpty { "Card Preview" },
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Bold,
                        color = configuration.cardType.color
                    )

                    Icon(
                        imageVector = configuration.cardType.icon,
                        contentDescription = configuration.cardType.displayName,
                        tint = configuration.cardType.color,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = if (configuration.cardNumber.isNotEmpty())
                            formatCardNumber(configuration.cardNumber)
                        else "•••• •••• •••• ••••",
                        style = MaterialTheme.typography.body1,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = configuration.cardholderName.ifEmpty { "CARDHOLDER NAME" },
                            style = MaterialTheme.typography.caption,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                        )

                        Text(
                            text = configuration.expiryDate.format(DateTimeFormatter.ofPattern("MM/yy")),
                            style = MaterialTheme.typography.caption,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

// --- INTERFACE CONFIGURATION SECTION ---

@Composable
fun InterfaceConfigurationSection(
    interfaceSettings: InterfaceSettings,
    onSettingsChange: (InterfaceSettings) -> Unit
) {
    var showAdvancedSettings by remember { mutableStateOf(false) }

    ConfigurationSection(
        title = "Interface Configuration",
        icon = Icons.Outlined.Contactless,
        description = "Configure supported card interfaces and communication protocols"
    ) {

        // Interface Checkboxes
        Text(
            "Enabled Interfaces",
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            InterfaceCheckbox(
                title = "Contact Interface",
                description = "ISO 7816 contact-based communication",
                icon = Icons.Outlined.Cable,
                checked = interfaceSettings.contactEnabled,
                onCheckedChange = {
                    onSettingsChange(interfaceSettings.copy(contactEnabled = it))
                },
                color = Color(0xFF0B6BCB)
            )

            InterfaceCheckbox(
                title = "Contactless Interface",
                description = "NFC/RFID contactless communication",
                icon = Icons.Outlined.Contactless,
                checked = interfaceSettings.contactlessEnabled,
                onCheckedChange = {
                    onSettingsChange(interfaceSettings.copy(contactlessEnabled = it))
                },
                color = Color(0xFF197A3E)
            )

            InterfaceCheckbox(
                title = "Magnetic Stripe Simulation",
                description = "Legacy magnetic stripe emulation",
                icon = Icons.Outlined.CreditCard,
                checked = interfaceSettings.magneticStripeEnabled,
                onCheckedChange = {
                    onSettingsChange(interfaceSettings.copy(magneticStripeEnabled = it))
                },
                color = Color(0xFFED6C02)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

// Advanced Settings Toggle
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAdvancedSettings = !showAdvancedSettings },
            backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.05f),
            elevation = 0.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = "Advanced Settings",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Advanced Interface Settings",
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.primary
                    )
                }

                Icon(
                    if (showAdvancedSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (showAdvancedSettings) "Hide" else "Show",
                    tint = MaterialTheme.colors.primary
                )
            }
        }

// Advanced Settings Content
        AnimatedVisibility(
            visible = showAdvancedSettings,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (interfaceSettings.contactEnabled) {
                    ContactAdvancedSettings(
                        settings = interfaceSettings.contactAdvanced,
                        onSettingsChange = { newSettings ->
                            onSettingsChange(interfaceSettings.copy(contactAdvanced = newSettings))
                        }
                    )
                }

                if (interfaceSettings.contactlessEnabled) {
                    ContactlessAdvancedSettings(
                        settings = interfaceSettings.contactlessAdvanced,
                        onSettingsChange = { newSettings ->
                            onSettingsChange(interfaceSettings.copy(contactlessAdvanced = newSettings))
                        }
                    )
                }

                if (interfaceSettings.magneticStripeEnabled) {
                    MagneticStripeAdvancedSettings(
                        settings = interfaceSettings.magneticStripeAdvanced,
                        onSettingsChange = { newSettings ->
                            onSettingsChange(interfaceSettings.copy(magneticStripeAdvanced = newSettings))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun InterfaceCheckbox(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = if (checked) color.copy(alpha = 0.08f) else MaterialTheme.colors.surface,
        elevation = if (checked) 1.dp else 0.dp,
        shape = RoundedCornerShape(8.dp),
        border = if (!checked) BorderStroke(
            1.dp,
            MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
        ) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (checked) color else MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Medium,
                    color = if (checked) color else MaterialTheme.colors.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = color,
                    checkedTrackColor = color.copy(alpha = 0.5f)
                )
            )
        }
    }
}

// --- ADVANCED INTERFACE SETTINGS ---

@Composable
fun ContactAdvancedSettings(
    settings: ContactSettings,
    onSettingsChange: (ContactSettings) -> Unit
) {
    AdvancedSettingsCard(
        title = "Contact Interface Settings",
        icon = Icons.Outlined.Cable,
        color = Color(0xFF0B6BCB)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ConfigDropdown(
                label = "Voltage",
                selectedValue = settings.voltage,
                options = listOf("1.8V", "3.0V", "5.0V"),
                onSelectionChange = { onSettingsChange(settings.copy(voltage = it)) },
                modifier = Modifier.weight(1f)
            )

            ConfigDropdown(
                label = "Frequency",
                selectedValue = settings.frequency,
                options = listOf("1.0MHz", "4.0MHz", "8.0MHz"),
                onSelectionChange = { onSettingsChange(settings.copy(frequency = it)) },
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ConfigDropdown(
                label = "Baud Rate",
                selectedValue = settings.baudRate,
                options = listOf("9600", "19200", "38400", "115200"),
                onSelectionChange = { onSettingsChange(settings.copy(baudRate = it)) },
                modifier = Modifier.weight(1f)
            )

            ConfigDropdown(
                label = "Protocol",
                selectedValue = settings.protocol,
                options = listOf("T=0", "T=1", "T=14"),
                onSelectionChange = { onSettingsChange(settings.copy(protocol = it)) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ContactlessAdvancedSettings(
    settings: ContactlessSettings,
    onSettingsChange: (ContactlessSettings) -> Unit
) {
    AdvancedSettingsCard(
        title = "Contactless Interface Settings",
        icon = Icons.Outlined.Contactless,
        color = Color(0xFF197A3E)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ConfigDropdown(
                label = "Technology",
                selectedValue = settings.technology,
                options = listOf("ISO 14443 Type A", "ISO 14443 Type B", "ISO 15693"),
                onSelectionChange = { onSettingsChange(settings.copy(technology = it)) },
                modifier = Modifier.weight(1f)
            )

            ConfigDropdown(
                label = "Data Rate",
                selectedValue = settings.dataRate,
                options = listOf("106 kbps", "212 kbps", "424 kbps", "848 kbps"),
                onSelectionChange = { onSettingsChange(settings.copy(dataRate = it)) },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConfigDropdown(
                label = "Max Distance",
                selectedValue = settings.maxDistance,
                options = listOf("2 cm", "4 cm", "6 cm", "10 cm"),
                onSelectionChange = { onSettingsChange(settings.copy(maxDistance = it)) },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Power Saving Mode",
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Reduce power consumption",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }

                Switch(
                    checked = settings.powerSaving,
                    onCheckedChange = { onSettingsChange(settings.copy(powerSaving = it)) }
                )
            }
        }
    }
}

@Composable
fun MagneticStripeAdvancedSettings(
    settings: MagneticStripeSettings,
    onSettingsChange: (MagneticStripeSettings) -> Unit
) {
    AdvancedSettingsCard(
        title = "Magnetic Stripe Settings",
        icon = Icons.Outlined.CreditCard,
        color = Color(0xFFED6C02)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Track Configuration",
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = settings.track1Enabled,
                        onCheckedChange = { onSettingsChange(settings.copy(track1Enabled = it)) }
                    )
                    Text("Track 1", style = MaterialTheme.typography.body2)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = settings.track2Enabled,
                        onCheckedChange = { onSettingsChange(settings.copy(track2Enabled = it)) }
                    )
                    Text("Track 2", style = MaterialTheme.typography.body2)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = settings.track3Enabled,
                        onCheckedChange = { onSettingsChange(settings.copy(track3Enabled = it)) }
                    )
                    Text("Track 3", style = MaterialTheme.typography.body2)
                }
            }

            ConfigDropdown(
                label = "Coercivity",
                selectedValue = settings.coercivity,
                options = listOf("300 Oe", "600 Oe", "900 Oe", "2750 Oe"),
                onSelectionChange = { onSettingsChange(settings.copy(coercivity = it)) }
            )
        }
    }
}

@Composable
fun AdvancedSettingsCard(
    title: String,
    icon: ImageVector,
    color: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = color.copy(alpha = 0.05f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            content()
        }
    }
}

// --- RESPONSE TIMING SECTION ---

@Composable
fun ResponseTimingSection(
    timing: ResponseTiming,
    onTimingChange: (ResponseTiming) -> Unit
) {
    var showCommandTimeouts by remember { mutableStateOf(false) }

    ConfigurationSection(
        title = "Response Timing",
        icon = Icons.Outlined.Timer,
        description = "Configure processing delays and timeout behaviors"
    ) {

        // Processing Delay
        Text(
            "Processing Delay",
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.05f),
            elevation = 0.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Delay: ${timing.processingDelay}ms",
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.Medium
                    )

                    OutlinedTextField(
                        value = timing.processingDelay.toString(),
                        onValueChange = { value ->
                            val delay = value.toIntOrNull()
                            if (delay != null && delay in 0..5000) {
                                onTimingChange(timing.copy(processingDelay = delay))
                            }
                        },
                        label = { Text("ms") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(100.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = timing.processingDelay.toFloat(),
                    onValueChange = {
                        onTimingChange(timing.copy(processingDelay = it.toInt()))
                    },
                    valueRange = 0f..5000f,
                    steps = 49,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colors.primary,
                        activeTrackColor = MaterialTheme.colors.primary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0ms", style = MaterialTheme.typography.caption)
                    Text("2500ms", style = MaterialTheme.typography.caption)
                    Text("5000ms", style = MaterialTheme.typography.caption)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Random Variance
        Card(
            backgroundColor = if (timing.randomVariance)
                MaterialTheme.colors.secondary.copy(alpha = 0.08f)
            else MaterialTheme.colors.surface,
            elevation = if (timing.randomVariance) 1.dp else 0.dp,
            shape = RoundedCornerShape(8.dp),
            border = if (!timing.randomVariance) BorderStroke(
                1.dp,
                MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
            ) else null
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Random Delay Variance",
                            style = MaterialTheme.typography.body2,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Add randomness to response timing",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Switch(
                        checked = timing.randomVariance,
                        onCheckedChange = {
                            onTimingChange(timing.copy(randomVariance = it))
                        }
                    )
                }

                AnimatedVisibility(
                    visible = timing.randomVariance,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Variance Range:",
                                style = MaterialTheme.typography.caption,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = timing.varianceRange.first.toString(),
                                onValueChange = { value ->
                                    val min = value.toIntOrNull()
                                    if (min != null && min >= 0) {
                                        onTimingChange(
                                            timing.copy(
                                                varianceRange = min..timing.varianceRange.last
                                            )
                                        )
                                    }
                                },
                                label = { Text("Min") },
                                modifier = Modifier.width(80.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            Text("-", style = MaterialTheme.typography.body2)

                            OutlinedTextField(
                                value = timing.varianceRange.last.toString(),
                                onValueChange = { value ->
                                    val max = value.toIntOrNull()
                                    if (max != null && max >= timing.varianceRange.first) {
                                        onTimingChange(
                                            timing.copy(
                                                varianceRange = timing.varianceRange.first..max
                                            )
                                        )
                                    }
                                },
                                label = { Text("Max") },
                                modifier = Modifier.width(80.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            Text("ms", style = MaterialTheme.typography.caption)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Command Timeouts
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showCommandTimeouts = !showCommandTimeouts },
            backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.05f),
            elevation = 0.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.AccessTime,
                        contentDescription = "Command Timeouts",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Command-Specific Timeouts",
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.primary
                    )
                }

                Icon(
                    if (showCommandTimeouts) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (showCommandTimeouts) "Hide" else "Show",
                    tint = MaterialTheme.colors.primary
                )
            }
        }

        AnimatedVisibility(
            visible = showCommandTimeouts,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CommandType.values().forEach { commandType ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = timing.commandTimeouts[commandType]?.toString()
                                    ?: commandType.defaultTimeout.toString(),
                                onValueChange = { value ->
                                    val timeout = value.toIntOrNull()
                                    if (timeout != null && timeout > 0) {
                                        onTimingChange(
                                            timing.copy(
                                                commandTimeouts = timing.commandTimeouts + (commandType to timeout)
                                            )
                                        )
                                    }
                                },
                                label = { Text(commandType.displayName) },
                                trailingIcon = {
                                    Text(
                                        "ms",
                                        style = MaterialTheme.typography.caption
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- CARD STATE MANAGEMENT SECTION ---

@Composable
fun CardStateManagementSection(
    stateManagement: CardStateManagement,
    onStateManagementChange: (CardStateManagement) -> Unit
) {
    var showBlockingConditions by remember { mutableStateOf(false) }

    ConfigurationSection(
        title = "Card State Management",
        icon = Icons.Outlined.Security,
        description = "Configure card state, PIN management, and blocking conditions"
    ) {

        // Initial Card State and PIN Attempts
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                CardStateDropdown(
                    selectedState = stateManagement.initialState,
                    onStateChange = {
                        onStateManagementChange(stateManagement.copy(initialState = it))
                    }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                PinAttemptsSpinner(
                    value = stateManagement.pinAttemptsRemaining,
                    onValueChange = {
                        onStateManagementChange(stateManagement.copy(pinAttemptsRemaining = it))
                    }
                )
            }
        }

        // Transaction Limits
        Text(
            "Transaction Limits",
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ConfigTextField(
                label = "Daily Transaction Limit",
                value = stateManagement.dailyTransactionLimit.toString(),
                onValueChange = { value ->
                    val limit = value.toIntOrNull()
                    if (limit != null && limit >= 0) {
                        onStateManagementChange(stateManagement.copy(dailyTransactionLimit = limit))
                    }
                },
                keyboardType = KeyboardType.Number,
                leadingIcon = Icons.Outlined.AccountBalance,
                modifier = Modifier.weight(1f)
            )

            ConfigTextField(
                label = "Current Count",
                value = stateManagement.dailyTransactionCount.toString(),
                onValueChange = { value ->
                    val count = value.toIntOrNull()
                    if (count != null && count >= 0) {
                        onStateManagementChange(stateManagement.copy(dailyTransactionCount = count))
                    }
                },
                keyboardType = KeyboardType.Number,
                leadingIcon = Icons.Outlined.Analytics,
                modifier = Modifier.weight(1f)
            )
        }

        // Progress indicator for daily transactions
        val progress = if (stateManagement.dailyTransactionLimit > 0) {
            stateManagement.dailyTransactionCount.toFloat() / stateManagement.dailyTransactionLimit.toFloat()
        } else 0f

        Card(
            backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.05f),
            elevation = 0.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Daily Usage",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.caption,
                        color = when {
                            progress >= 1.0f -> Color(0xFFD32F2F)
                            progress >= 0.8f -> Color(0xFFED6C02)
                            else -> Color(0xFF197A3E)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = progress.coerceAtMost(1.0f),
                    modifier = Modifier.fillMaxWidth(),
                    color = when {
                        progress >= 1.0f -> Color(0xFFD32F2F)
                        progress >= 0.8f -> Color(0xFFED6C02)
                        else -> Color(0xFF197A3E)
                    },
                    backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Blocking Conditions
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showBlockingConditions = !showBlockingConditions },
            backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.05f),
            elevation = 0.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Block,
                        contentDescription = "Blocking Conditions",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Card Blocking Conditions",
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFD32F2F)
                    )
                }

                Icon(
                    if (showBlockingConditions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (showBlockingConditions) "Hide" else "Show",
                    tint = Color(0xFFD32F2F)
                )
            }
        }

        AnimatedVisibility(
            visible = showBlockingConditions,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BlockingConditionCard(
                    title = "Geographic Restrictions",
                    description = "Block transactions from restricted regions",
                    enabled = stateManagement.blockingConditions.geographicRestrictions,
                    value = "Location-based blocking",
                    onValueChange = { enabled ->
                        onStateManagementChange(
                            stateManagement.copy(
                                blockingConditions = stateManagement.blockingConditions.copy(
                                    geographicRestrictions = enabled
                                )
                            )
                        )
                    },
                    icon = Icons.Outlined.LocationOn
                )


                BlockingConditionCard(
                    title = "Daily Transaction Limit",
                    description = "Block when daily transaction limit exceeded",
                    enabled = stateManagement.blockingConditions.maxDailyTransactions,
                    value = "Enabled",
                    onValueChange = { enabled ->
                        onStateManagementChange(
                            stateManagement.copy(
                                blockingConditions = stateManagement.blockingConditions.copy(
                                    maxDailyTransactions = enabled
                                )
                            )
                        )
                    },
                    icon = Icons.Outlined.DateRange
                )

                BlockingConditionCard(
                    title = "Suspicious Activity Detection",
                    description = "Monitor for unusual transaction patterns",
                    enabled = stateManagement.blockingConditions.suspiciousActivityDetection,
                    value = "AI-powered detection",
                    onValueChange = { enabled ->
                        onStateManagementChange(
                            stateManagement.copy(
                                blockingConditions = stateManagement.blockingConditions.copy(
                                    suspiciousActivityDetection = enabled
                                )
                            )
                        )
                    },
                    icon = Icons.Outlined.Psychology
                )

                BlockingConditionCard(
                    title = "Velocity Checking",
                    description = "Monitor transaction frequency and amounts",
                    enabled = stateManagement.blockingConditions.velocityChecking,
                    value = "Real-time monitoring",
                    onValueChange = { enabled ->
                        onStateManagementChange(
                            stateManagement.copy(
                                blockingConditions = stateManagement.blockingConditions.copy(
                                    velocityChecking = enabled
                                )
                            )
                        )
                    },
                    icon = Icons.Outlined.Speed
                )

                BlockingConditionCard(
                    title = "Failed PIN Attempts",
                    description = "Block card after consecutive failed PIN attempts",
                    enabled = true,
                    value = "Max ${stateManagement.blockingConditions.maxFailedPinAttempts} attempts",
                    onValueChange = { /* TODO */ },
                    icon = Icons.Outlined.Pin
                )
            }
        }
    }
}

@Composable
fun BlockingConditionCard(
    title: String,
    description: String,
    enabled: Boolean,
    value: String,
    onValueChange: (Boolean) -> Unit,
    icon: ImageVector
) {
    Card(
        backgroundColor = if (enabled) Color(0xFFD32F2F).copy(alpha = 0.08f) else MaterialTheme.colors.surface,
        elevation = if (enabled) 1.dp else 0.dp,
        shape = RoundedCornerShape(8.dp),
        border = if (!enabled) BorderStroke(
            1.dp,
            MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
        ) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (enabled) Color(0xFFD32F2F) else MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) Color(0xFFD32F2F) else MaterialTheme.colors.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
                if (enabled) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.caption,
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Switch(
                checked = enabled,
                onCheckedChange = onValueChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFD32F2F),
                    checkedTrackColor = Color(0xFFD32F2F).copy(alpha = 0.5f)
                )
            )
        }
    }
}

// --- QUICK ACTIONS ---

@Composable
fun BasicCardQuickActions(
    configuration: BasicCardConfiguration,
    hasUnsavedChanges: Boolean,
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
                    if (configuration.profileName.isNotEmpty())
                        "Profile: ${configuration.profileName}"
                    else
                        "Configure your card profile",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onQuickAction("generate_card_number") },
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.CreditCard,
                        contentDescription = "Generate Card",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Generate Card")
                }

                OutlinedButton(
                    onClick = { onQuickAction("generate_cvv") },
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.Security,
                        contentDescription = "Generate CVV",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Generate CVV")
                }

                Button(
                    onClick = { onQuickAction("quick_setup") },
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = "Quick Setup",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Quick Setup")
                }
            }
        }
    }
}

// --- UTILITY COMPONENTS ---

@Composable
fun ConfigurationSection(
    title: String,
    icon: ImageVector,
    description: String,
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
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Divider(
                color = MaterialTheme.colors.primary.copy(alpha = 0.2f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            content()
        }
    }
}

@Composable
fun ConfigTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        leadingIcon = leadingIcon?.let { icon ->
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(8.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = MaterialTheme.colors.primary,
            unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
        )
    )
}

@Composable
fun ConfigDropdown(
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

@Composable
fun CardTypeDropdown(
    selectedType: CardType,
    onTypeChange: (CardType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = selectedType.displayName,
            onValueChange = { },
            label = { Text("Card Type") },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            leadingIcon = {
                Icon(
                    imageVector = selectedType.icon,
                    contentDescription = null,
                    tint = selectedType.color,
                    modifier = Modifier.size(20.dp)
                )
            },
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
            CardType.values().forEach { cardType ->
                DropdownMenuItem(
                    onClick = {
                        onTypeChange(cardType)
                        expanded = false
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = cardType.icon,
                            contentDescription = null,
                            tint = cardType.color,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(cardType.displayName)
                    }
                }
            }
        }
    }
}

@Composable
fun EmvVersionDropdown(
    selectedVersion: EmvVersion,
    onVersionChange: (EmvVersion) -> Unit
) {
    ConfigDropdown(
        label = "EMV Version",
        selectedValue = selectedVersion.displayName,
        options = EmvVersion.values().map { it.displayName },
        onSelectionChange = { versionName ->
            val version = EmvVersion.values().find { it.displayName == versionName }
            version?.let { onVersionChange(it) }
        }
    )
}

@Composable
fun CardStateDropdown(
    selectedState: CardState,
    onStateChange: (CardState) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = selectedState.displayName,
            onValueChange = { },
            label = { Text("Initial Card State") },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Security,
                    contentDescription = null,
                    tint = selectedState.color,
                    modifier = Modifier.size(20.dp)
                )
            },
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
            CardState.values().forEach { state ->
                DropdownMenuItem(
                    onClick = {
                        onStateChange(state)
                        expanded = false
                    }
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = state.color,
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                state.displayName,
                                style = MaterialTheme.typography.body2,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            state.description,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PinAttemptsSpinner(
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = value.toString(),
            onValueChange = { newValue ->
                val attempts = newValue.toIntOrNull()
                if (attempts != null && attempts in 0..10) {
                    onValueChange(attempts)
                }
            },
            label = { Text("PIN Attempts Remaining") },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Pin,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
            )
        )

        Column {
            IconButton(
                onClick = { if (value < 10) onValueChange(value + 1) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = "Increase",
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(
                onClick = { if (value > 0) onValueChange(value - 1) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Decrease",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ExpiryDatePicker(
    selectedDate: LocalDate,
    onDateChange: (LocalDate) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = selectedDate.format(DateTimeFormatter.ofPattern("MM/yy")),
        onValueChange = { },
        label = { Text("Expiry Date") },
        readOnly = true,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDatePicker = true },
        leadingIcon = {
            Icon(
                Icons.Outlined.DateRange,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            Icon(
                Icons.Default.DateRange,
                contentDescription = "Pick date"
            )
        },
        shape = RoundedCornerShape(8.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = MaterialTheme.colors.primary,
            unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
        )
    )

    if (showDatePicker) {
        DatePickerDialog(
            selectedDate = selectedDate,
            onDateSelected = { newDate ->
                onDateChange(newDate)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
fun DatePickerDialog(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Select Expiry Date",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Simple year/month picker
                var selectedYear by remember { mutableStateOf(selectedDate.year) }
                var selectedMonth by remember { mutableStateOf(selectedDate.monthValue) }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Month picker
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Month", style = MaterialTheme.typography.caption)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (selectedMonth > 1) selectedMonth-- }) {
                                Icon(
                                    Icons.Default.KeyboardArrowLeft,
                                    contentDescription = "Previous month"
                                )
                            }
                            Text(
                                "%02d".format(selectedMonth),
                                style = MaterialTheme.typography.h6,
                                modifier = Modifier.width(40.dp),
                                textAlign = TextAlign.Center
                            )
                            IconButton(onClick = { if (selectedMonth < 12) selectedMonth++ }) {
                                Icon(
                                    Icons.Default.KeyboardArrowRight,
                                    contentDescription = "Next month"
                                )
                            }
                        }
                    }

                    // Year picker
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Year", style = MaterialTheme.typography.caption)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { selectedYear-- }) {
                                Icon(
                                    Icons.Default.KeyboardArrowLeft,
                                    contentDescription = "Previous year"
                                )
                            }
                            Text(
                                selectedYear.toString(),
                                style = MaterialTheme.typography.h6,
                                modifier = Modifier.width(60.dp),
                                textAlign = TextAlign.Center
                            )
                            IconButton(onClick = { selectedYear++ }) {
                                Icon(
                                    Icons.Default.KeyboardArrowRight,
                                    contentDescription = "Next year"
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val newDate = LocalDate.of(selectedYear, selectedMonth, 1)
                            onDateSelected(newDate)
                        }
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

@Composable
fun ValidationDialog(
    errors: List<String>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (errors.isEmpty()) Icons.Outlined.CheckCircle else Icons.Outlined.Error,
                        contentDescription = null,
                        tint = if (errors.isEmpty()) Color(0xFF197A3E) else Color(0xFFD32F2F),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (errors.isEmpty()) "Validation Passed" else "Validation Errors",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold,
                        color = if (errors.isEmpty()) Color(0xFF197A3E) else Color(0xFFD32F2F)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (errors.isEmpty()) {
                    Text(
                        "Configuration is valid and ready to save.",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                } else {
                    Text(
                        "Please fix the following issues:",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    errors.forEach { error ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(
                                        color = Color(0xFFD32F2F),
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                error,
                                style = MaterialTheme.typography.body2,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text("OK")
                    }
                }
            }
        }
    }
}


fun formatCardNumber(cardNumber: String): String {
    return cardNumber.chunked(4).joinToString(" ")
}

fun generateCardNumber(cardType: CardType): String {
    val prefix = cardType.prefix
    val random = Random()

    return when (cardType) {
        CardType.VISA -> "4" + (1..15).map { random.nextInt(10) }.joinToString("")
        CardType.MASTERCARD -> "5" + (1..15).map { random.nextInt(10) }.joinToString("")
        CardType.AMERICAN_EXPRESS -> "3" + (1..14).map { random.nextInt(10) }.joinToString("")
        else -> prefix + (1..15).map { random.nextInt(10) }.joinToString("")
    }
}

fun generateCvv(): String {
    val random = Random()
    return (1..3).map { random.nextInt(10) }.joinToString("")
}

fun isValidCardNumber(cardNumber: String): Boolean {
    return cardNumber.length in 13..19 && cardNumber.all { it.isDigit() }
}

fun getEnabledInterfacesCount(settings: InterfaceSettings): Int {
    var count = 0
    if (settings.contactEnabled) count++
    if (settings.contactlessEnabled) count++
    if (settings.magneticStripeEnabled) count++
    return count
}

fun createQuickSetupConfiguration(): BasicCardConfiguration {
    return BasicCardConfiguration(
        profileName = "Quick Setup Profile",
        cardType = CardType.VISA,
        emvVersion = EmvVersion.EMV_4_3,
        cardNumber = generateCardNumber(CardType.VISA),
        expiryDate = LocalDate.now().plusYears(3),
        cvv = generateCvv(),
        cardholderName = "JOHN DOE",
        interfaceSettings = InterfaceSettings(
            contactEnabled = true,
            contactlessEnabled = true,
            magneticStripeEnabled = false
        ),
        responseTiming = ResponseTiming(
            processingDelay = 500,
            randomVariance = true,
            varianceRange = 0..200
        ),
        stateManagement = CardStateManagement(
            initialState = CardState.ACTIVE,
            pinAttemptsRemaining = 3,
            dailyTransactionLimit = 50,
            dailyTransactionCount = 0
        )
    )
}
