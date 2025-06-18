package `in`.aicortex.iso8583studio.ui.screens.config.apduSimulator

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.sortBy

// --- DATA MODELS ---

enum class RuleCategory(val displayName: String, val icon: ImageVector, val color: Color) {
    SECURITY("Security Rules", Icons.Outlined.Security, Color(0xFFD32F2F)),
    TRANSACTION_LIMITS("Transaction Limits", Icons.Outlined.AccountBalance, Color(0xFF0B6BCB)),
    ERROR_SIMULATION("Error Simulation", Icons.Outlined.BugReport, Color(0xFFED6C02)),
    PERFORMANCE("Performance Control", Icons.Outlined.Speed, Color(0xFF197A3E)),
    COMPLIANCE("Compliance", Icons.Outlined.Gavel, Color(0xFF6750A4)),
    CUSTOM("Custom Rules", Icons.Outlined.Extension, Color(0xFF795548))
}

enum class ConditionType(val displayName: String, val description: String) {
    COMMAND_MATCH("Command Match", "Match specific ISO8583 command types"),
    AMOUNT_RANGE("Amount Range", "Check transaction amount within range"),
    TIME_BASED("Time Based", "Time or date-based conditions"),
    FIELD_VALUE("Field Value", "Check specific field values"),
    CARD_NUMBER("Card Number", "PAN or card number patterns"),
    MERCHANT_ID("Merchant ID", "Merchant or terminal identification"),
    FREQUENCY("Frequency", "Transaction frequency limits"),
    GEO_LOCATION("Geo Location", "Geographic location-based rules")
}

enum class ActionType(val displayName: String, val description: String) {
    RETURN_STATUS("Return Status", "Return specific response status"),
    MODIFY_RESPONSE("Modify Response", "Modify response fields"),
    DELAY_RESPONSE("Delay Response", "Add processing delay"),
    LOG_EVENT("Log Event", "Log transaction for audit"),
    BLOCK_TRANSACTION("Block Transaction", "Block/reject transaction"),
    ROUTE_TO_HSM("Route to HSM", "Route to hardware security module"),
    TRIGGER_ALERT("Trigger Alert", "Send alert notification"),
    INCREMENT_COUNTER("Increment Counter", "Update transaction counters")
}

enum class LogicOperator(val displayName: String, val symbol: String) {
    AND("AND", "&&"),
    OR("OR", "||")
}

data class RuleCondition(
    val id: String = UUID.randomUUID().toString(),
    val type: ConditionType,
    val parameters: Map<String, String> = emptyMap(),
    val operator: LogicOperator = LogicOperator.AND,
    val isValid: Boolean = true
)

data class RuleAction(
    val id: String = UUID.randomUUID().toString(),
    val type: ActionType,
    val parameters: Map<String, String> = emptyMap(),
    val isValid: Boolean = true
)

data class BehaviorRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val category: RuleCategory,
    val conditions: List<RuleCondition> = emptyList(),
    val actions: List<RuleAction> = emptyList(),
    val enabled: Boolean = true,
    val priority: Int = 0,
    val createdDate: LocalDateTime = LocalDateTime.now(),
    val lastModified: LocalDateTime = LocalDateTime.now(),
    val executionCount: Int = 0,
    val isTemplate: Boolean = false
)

data class RuleTestResult(
    val ruleId: String,
    val ruleName: String,
    val matched: Boolean,
    val executionTime: Long,
    val actionsExecuted: List<String>,
    val errorMessage: String? = null
)

// --- SAMPLE DATA ---

fun getSampleRules(): List<BehaviorRule> = listOf(
    BehaviorRule(
        name = "High Value Transaction Security",
        description = "Enhanced security checks for high-value transactions",
        category = RuleCategory.SECURITY,
        conditions = listOf(
            RuleCondition(
                type = ConditionType.AMOUNT_RANGE,
                parameters = mapOf("minAmount" to "10000", "currency" to "USD")
            ),
            RuleCondition(
                type = ConditionType.TIME_BASED,
                parameters = mapOf("startTime" to "22:00", "endTime" to "06:00"),
                operator = LogicOperator.OR
            )
        ),
        actions = listOf(
            RuleAction(
                type = ActionType.ROUTE_TO_HSM,
                parameters = mapOf("hsmSlot" to "1", "requirePin" to "true")
            ),
            RuleAction(
                type = ActionType.LOG_EVENT,
                parameters = mapOf("level" to "WARNING", "category" to "HIGH_VALUE")
            )
        ),
        enabled = true,
        priority = 1,
        executionCount = 247
    ),
    BehaviorRule(
        name = "Daily Transaction Limit",
        description = "Enforce daily transaction limits per card",
        category = RuleCategory.TRANSACTION_LIMITS,
        conditions = listOf(
            RuleCondition(
                type = ConditionType.FREQUENCY,
                parameters = mapOf("maxCount" to "50", "timeWindow" to "24h")
            )
        ),
        actions = listOf(
            RuleAction(
                type = ActionType.RETURN_STATUS,
                parameters = mapOf("responseCode" to "61", "message" to "Daily limit exceeded")
            )
        ),
        enabled = true,
        priority = 2,
        executionCount = 89
    ),
    BehaviorRule(
        name = "Network Timeout Simulation",
        description = "Simulate network timeouts for testing",
        category = RuleCategory.ERROR_SIMULATION,
        conditions = listOf(
            RuleCondition(
                type = ConditionType.COMMAND_MATCH,
                parameters = mapOf("mti" to "0200", "processingCode" to "000000")
            )
        ),
        actions = listOf(
            RuleAction(
                type = ActionType.DELAY_RESPONSE,
                parameters = mapOf("delayMs" to "30000", "reason" to "Network timeout test")
            )
        ),
        enabled = false,
        priority = 10,
        executionCount = 15
    ),
    BehaviorRule(
        name = "Merchant Risk Assessment",
        description = "Risk-based routing for high-risk merchants",
        category = RuleCategory.COMPLIANCE,
        conditions = listOf(
            RuleCondition(
                type = ConditionType.MERCHANT_ID,
                parameters = mapOf("merchantCategory" to "7995,7801", "riskLevel" to "HIGH")
            )
        ),
        actions = listOf(
            RuleAction(
                type = ActionType.TRIGGER_ALERT,
                parameters = mapOf("alertType" to "RISK_ASSESSMENT", "priority" to "HIGH")
            ),
            RuleAction(
                type = ActionType.MODIFY_RESPONSE,
                parameters = mapOf("field39" to "05", "additionalData" to "Risk review required")
            )
        ),
        enabled = true,
        priority = 3,
        executionCount = 156
    )
)

fun getRuleTemplates(): List<BehaviorRule> = listOf(
    BehaviorRule(
        name = "Amount Limit Template",
        description = "Template for amount-based transaction limits",
        category = RuleCategory.TRANSACTION_LIMITS,
        conditions = listOf(
            RuleCondition(
                type = ConditionType.AMOUNT_RANGE,
                parameters = mapOf("minAmount" to "0", "maxAmount" to "1000")
            )
        ),
        actions = listOf(
            RuleAction(
                type = ActionType.RETURN_STATUS,
                parameters = mapOf("responseCode" to "61")
            )
        ),
        isTemplate = true
    ),
    BehaviorRule(
        name = "Time-based Access Template",
        description = "Template for time-based access control",
        category = RuleCategory.SECURITY,
        conditions = listOf(
            RuleCondition(
                type = ConditionType.TIME_BASED,
                parameters = mapOf("startTime" to "09:00", "endTime" to "17:00")
            )
        ),
        actions = listOf(
            RuleAction(
                type = ActionType.BLOCK_TRANSACTION,
                parameters = mapOf("reason" to "Outside business hours")
            )
        ),
        isTemplate = true
    )
)

// --- MAIN BEHAVIOR RULE TAB ---

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun BehaviorRuleManagementTab() {
    var activeView by remember { mutableStateOf("list") } // "list" or "editor"
    var rules by remember { mutableStateOf(getSampleRules()) }
    var selectedRule by remember { mutableStateOf<BehaviorRule?>(null) }
    var selectedCategory by remember { mutableStateOf<RuleCategory?>(null) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showTestDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Header with Statistics
        BehaviorRuleHeader(
            totalRules = rules.size,
            enabledRules = rules.count { it.enabled },
            totalExecutions = rules.sumOf { it.executionCount },
            hasUnsavedChanges = hasUnsavedChanges,
            isLoading = isLoading,
            activeView = activeView,
            onSave = {
                isLoading = true
                kotlinx.coroutines.MainScope().launch {
                    kotlinx.coroutines.delay(1000)
                    hasUnsavedChanges = false
                    isLoading = false
                }
            },
            onViewChange = { view ->
                activeView = view
                if (view == "list") {
                    selectedRule = null
                }
            }
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
                targetState = activeView,
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { width -> if (targetState == "editor") width else -width },
                        animationSpec = tween(300)
                    ) + fadeIn() with
                            slideOutHorizontally(
                                targetOffsetX = { width -> if (targetState == "editor") -width else width },
                                animationSpec = tween(300)
                            ) + fadeOut()
                }
            ) { view ->
                when (view) {
                    "list" -> {
                        RuleListView(
                            rules = rules,
                            selectedCategory = selectedCategory,
                            onCategoryChange = { selectedCategory = it },
                            onRuleSelected = { rule ->
                                selectedRule = rule
                                activeView = "editor"
                            },
                            onAddRule = {
                                selectedRule = null
                                activeView = "editor"
                            },
                            onDeleteRule = { ruleToDelete ->
                                rules = rules.filterNot { it.id == ruleToDelete.id }
                                hasUnsavedChanges = true
                            },
                            onToggleRule = { rule ->
                                rules = rules.map {
                                    if (it.id == rule.id) it.copy(enabled = !it.enabled) else it
                                }
                                hasUnsavedChanges = true
                            },
                            onImportRules = { /* TODO */ },
                            onExportRules = { /* TODO */ },
                            onShowTemplates = { showTemplateDialog = true }
                        )
                    }

                    "editor" -> {
                        RuleEditorView(
                            rule = selectedRule,
                            onRuleChange = { updatedRule ->
                                if (selectedRule != null) {
                                    rules =
                                        rules.map { if (it.id == updatedRule.id) updatedRule else it }
                                } else {
                                    rules = rules + updatedRule
                                }
                                selectedRule = updatedRule
                                hasUnsavedChanges = true
                            },
                            onTestRule = { showTestDialog = true },
                            onBack = { activeView = "list" }
                        )
                    }
                }
            }
        }

        // Quick Actions
        RuleQuickActions(
            activeView = activeView,
            selectedRule = selectedRule,
            onQuickAction = { action ->
                when (action) {
                    "add_rule" -> {
                        selectedRule = null
                        activeView = "editor"
                    }

                    "templates" -> showTemplateDialog = true
                    "test_all" -> showTestDialog = true
                    "import" -> { /* TODO */
                    }

                    "export" -> { /* TODO */
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Dialogs
    if (showTemplateDialog) {
        RuleTemplateDialog(
            templates = getRuleTemplates(),
            onDismiss = { showTemplateDialog = false },
            onSelectTemplate = { template ->
                selectedRule = template.copy(
                    id = UUID.randomUUID().toString(),
                    isTemplate = false,
                    createdDate = LocalDateTime.now()
                )
                activeView = "editor"
                showTemplateDialog = false
            }
        )
    }

    if (showTestDialog) {
        RuleTestDialog(
            rule = selectedRule,
            onDismiss = { showTestDialog = false }
        )
    }
}

// --- BEHAVIOR RULE HEADER ---

@Composable
fun BehaviorRuleHeader(
    totalRules: Int,
    enabledRules: Int,
    totalExecutions: Int,
    hasUnsavedChanges: Boolean,
    isLoading: Boolean,
    activeView: String,
    onSave: () -> Unit,
    onViewChange: (String) -> Unit
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
                        text = "Behavior Rule Management",
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
                    // View Toggle
                    Row(
                        modifier = Modifier.background(
                            color = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        ViewToggleButton(
                            text = "Rules List",
                            icon = Icons.Outlined.List,
                            selected = activeView == "list",
                            onClick = { onViewChange("list") }
                        )
                        ViewToggleButton(
                            text = "Rule Editor",
                            icon = Icons.Outlined.Edit,
                            selected = activeView == "editor",
                            onClick = { onViewChange("editor") }
                        )
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Statistics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RuleStatCard(
                    title = "Total Rules",
                    value = totalRules.toString(),
                    subtitle = "$enabledRules enabled",
                    icon = Icons.Outlined.Rule,
                    color = Color(0xFF6750A4)
                )

                RuleStatCard(
                    title = "Active Rules",
                    value = enabledRules.toString(),
                    subtitle = "${((enabledRules.toFloat() / totalRules.coerceAtLeast(1)) * 100).toInt()}% active",
                    icon = Icons.Outlined.PlayArrow,
                    color = Color(0xFF197A3E)
                )

                RuleStatCard(
                    title = "Executions",
                    value = formatNumber(totalExecutions),
                    subtitle = "Total processed",
                    icon = Icons.Outlined.Analytics,
                    color = Color(0xFF0B6BCB)
                )

                RuleStatCard(
                    title = "Categories",
                    value = RuleCategory.values().size.toString(),
                    subtitle = "Rule types",
                    icon = Icons.Outlined.Category,
                    color = Color(0xFFED6C02)
                )
            }
        }
    }
}



@Composable
fun ViewToggleButton(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colors.primary else Color.Transparent,
        animationSpec = tween(200)
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colors.primary,
        animationSpec = tween(200)
    )

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        ),
        modifier = Modifier.height(36.dp),
        shape = RoundedCornerShape(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun RuleStatCard(
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

// --- RULE LIST VIEW ---

@Composable
fun RuleListView(
    rules: List<BehaviorRule>,
    selectedCategory: RuleCategory?,
    onCategoryChange: (RuleCategory?) -> Unit,
    onRuleSelected: (BehaviorRule) -> Unit,
    onAddRule: () -> Unit,
    onDeleteRule: (BehaviorRule) -> Unit,
    onToggleRule: (BehaviorRule) -> Unit,
    onImportRules: () -> Unit,
    onExportRules: () -> Unit,
    onShowTemplates: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("priority") }

    val filteredRules = rules.filter { rule ->
        val matchesSearch = searchQuery.isEmpty() ||
                rule.name.contains(searchQuery, ignoreCase = true) ||
                rule.description.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == null || rule.category == selectedCategory

        matchesSearch && matchesCategory
    }.sortedWith(
        when (sortBy) {
            "priority" -> compareBy { it.priority }
            "name" -> compareBy { it.name }
            "category" -> compareBy { it.category.displayName }
            "executions" -> compareByDescending { it.executionCount }
            else -> compareBy { it.priority }
        }
    )

    Column(modifier = Modifier.padding(16.dp)) {

        // Header with Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Behavior Rules",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${filteredRules.size} of ${rules.size} rules",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onShowTemplates,
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

                OutlinedButton(
                    onClick = onImportRules,
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
                    onClick = onAddRule,
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = "Add Rule",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Rule")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search and Filters
        RuleSearchAndFilters(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            selectedCategory = selectedCategory,
            onCategoryChange = onCategoryChange,
            sortBy = sortBy,
            onSortChange = { sortBy = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Category Overview
        if (selectedCategory == null) {
            RuleCategoryOverview(rules = rules, onCategorySelect = onCategoryChange)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Rules Table
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
                    RuleTableHeader()
                }

                // Rule Rows
                items(filteredRules, key = { it.id }) { rule ->
                    RuleTableRow(
                        rule = rule,
                        onSelect = { onRuleSelected(rule) },
                        onToggle = { onToggleRule(rule) },
                        onDelete = { onDeleteRule(rule) }
                    )
                }

                if (filteredRules.isEmpty()) {
                    item {
                        EmptyRulesState(
                            searchQuery = searchQuery,
                            selectedCategory = selectedCategory,
                            onAddRule = onAddRule,
                            onClearFilters = {
                                searchQuery = ""
                                onCategoryChange(null)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConditionBuilder(
    condition: RuleCondition,
    showOperator: Boolean,
    onConditionChange: (RuleCondition) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.05f),
        elevation = 1.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // Operator Row (for chained conditions)
            if (showOperator) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.background(
                            color = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        )
                    ) {
                        LogicOperator.values().forEach { operator ->
                            val selected = condition.operator == operator

                            TextButton(
                                onClick = {
                                    onConditionChange(condition.copy(operator = operator))
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    backgroundColor = if (selected) MaterialTheme.colors.primary else Color.Transparent,
                                    contentColor = if (selected) Color.White else MaterialTheme.colors.primary
                                ),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = operator.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Condition Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Condition",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colors.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Condition Type Dropdown
            ConditionTypeDropdown(
                selectedType = condition.type,
                onTypeChange = { newType ->
                    onConditionChange(condition.copy(type = newType, parameters = emptyMap()))
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Dynamic Parameter Fields based on condition type
            ConditionParameterFields(
                conditionType = condition.type,
                parameters = condition.parameters,
                onParametersChange = { newParameters ->
                    onConditionChange(condition.copy(parameters = newParameters))
                }
            )
        }
    }
}

@Composable
fun ConditionTypeDropdown(
    selectedType: ConditionType,
    onTypeChange: (ConditionType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = selectedType.displayName,
            onValueChange = { },
            label = { Text("Condition Type") },
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
            shape = RoundedCornerShape(8.dp)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ConditionType.values().forEach { conditionType ->
                DropdownMenuItem(
                    onClick = {
                        onTypeChange(conditionType)
                        expanded = false
                    }
                ) {
                    Column {
                        Text(
                            conditionType.displayName,
                            style = MaterialTheme.typography.body2,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            conditionType.description,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConditionParameterFields(
    conditionType: ConditionType,
    parameters: Map<String, String>,
    onParametersChange: (Map<String, String>) -> Unit
) {
    fun updateParameter(key: String, value: String) {
        onParametersChange(parameters + (key to value))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (conditionType) {
            ConditionType.COMMAND_MATCH -> {
                RuleTextField(
                    label = "Message Type (MTI)",
                    value = parameters["mti"] ?: "",
                    onValueChange = { updateParameter("mti", it) }
                )
                RuleTextField(
                    label = "Processing Code",
                    value = parameters["processingCode"] ?: "",
                    onValueChange = { updateParameter("processingCode", it) }
                )
            }

            ConditionType.AMOUNT_RANGE -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RuleTextField(
                        label = "Minimum Amount",
                        value = parameters["minAmount"] ?: "",
                        onValueChange = { updateParameter("minAmount", it) },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    RuleTextField(
                        label = "Maximum Amount",
                        value = parameters["maxAmount"] ?: "",
                        onValueChange = { updateParameter("maxAmount", it) },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                }
                RuleTextField(
                    label = "Currency Code",
                    value = parameters["currency"] ?: "",
                    onValueChange = { updateParameter("currency", it) }
                )
            }

            ConditionType.TIME_BASED -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RuleTextField(
                        label = "Start Time (HH:mm)",
                        value = parameters["startTime"] ?: "",
                        onValueChange = { updateParameter("startTime", it) },
                        modifier = Modifier.weight(1f)
                    )
                    RuleTextField(
                        label = "End Time (HH:mm)",
                        value = parameters["endTime"] ?: "",
                        onValueChange = { updateParameter("endTime", it) },
                        modifier = Modifier.weight(1f)
                    )
                }
                RuleTextField(
                    label = "Days of Week (1-7, comma separated)",
                    value = parameters["daysOfWeek"] ?: "",
                    onValueChange = { updateParameter("daysOfWeek", it) }
                )
            }

            ConditionType.FIELD_VALUE -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RuleTextField(
                        label = "Field Number",
                        value = parameters["fieldNumber"] ?: "",
                        onValueChange = { updateParameter("fieldNumber", it) },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    RuleTextField(
                        label = "Expected Value",
                        value = parameters["expectedValue"] ?: "",
                        onValueChange = { updateParameter("expectedValue", it) },
                        modifier = Modifier.weight(2f)
                    )
                }
            }

            ConditionType.CARD_NUMBER -> {
                RuleTextField(
                    label = "PAN Pattern (use * for wildcards)",
                    value = parameters["panPattern"] ?: "",
                    onValueChange = { updateParameter("panPattern", it) }
                )
                RuleTextField(
                    label = "Card Type",
                    value = parameters["cardType"] ?: "",
                    onValueChange = { updateParameter("cardType", it) }
                )
            }

            ConditionType.MERCHANT_ID -> {
                RuleTextField(
                    label = "Merchant ID",
                    value = parameters["merchantId"] ?: "",
                    onValueChange = { updateParameter("merchantId", it) }
                )
                RuleTextField(
                    label = "Merchant Category Codes",
                    value = parameters["merchantCategory"] ?: "",
                    onValueChange = { updateParameter("merchantCategory", it) }
                )
            }

            ConditionType.FREQUENCY -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RuleTextField(
                        label = "Max Count",
                        value = parameters["maxCount"] ?: "",
                        onValueChange = { updateParameter("maxCount", it) },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    RuleTextField(
                        label = "Time Window",
                        value = parameters["timeWindow"] ?: "",
                        onValueChange = { updateParameter("timeWindow", it) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            ConditionType.GEO_LOCATION -> {
                RuleTextField(
                    label = "Country Codes (comma separated)",
                    value = parameters["countryCodes"] ?: "",
                    onValueChange = { updateParameter("countryCodes", it) }
                )
                RuleTextField(
                    label = "Restricted Regions",
                    value = parameters["restrictedRegions"] ?: "",
                    onValueChange = { updateParameter("restrictedRegions", it) }
                )
            }
        }
    }
}

// --- ACTION BUILDER ---

@Composable
fun ActionBuilder(
    action: RuleAction,
    onActionChange: (RuleAction) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        backgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.05f),
        elevation = 1.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // Action Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Action",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colors.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Action Type Dropdown
            ActionTypeDropdown(
                selectedType = action.type,
                onTypeChange = { newType ->
                    onActionChange(action.copy(type = newType, parameters = emptyMap()))
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Dynamic Parameter Fields based on action type
            ActionParameterFields(
                actionType = action.type,
                parameters = action.parameters,
                onParametersChange = { newParameters ->
                    onActionChange(action.copy(parameters = newParameters))
                }
            )
        }
    }
}

@Composable
fun ActionTypeDropdown(
    selectedType: ActionType,
    onTypeChange: (ActionType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = selectedType.displayName,
            onValueChange = { },
            label = { Text("Action Type") },
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
            shape = RoundedCornerShape(8.dp)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ActionType.values().forEach { actionType ->
                DropdownMenuItem(
                    onClick = {
                        onTypeChange(actionType)
                        expanded = false
                    }
                ) {
                    Column {
                        Text(
                            actionType.displayName,
                            style = MaterialTheme.typography.body2,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            actionType.description,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActionParameterFields(
    actionType: ActionType,
    parameters: Map<String, String>,
    onParametersChange: (Map<String, String>) -> Unit
) {
    fun updateParameter(key: String, value: String) {
        onParametersChange(parameters + (key to value))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (actionType) {
            ActionType.RETURN_STATUS -> {
                RuleTextField(
                    label = "Response Code",
                    value = parameters["responseCode"] ?: "",
                    onValueChange = { updateParameter("responseCode", it) }
                )
                RuleTextField(
                    label = "Response Message",
                    value = parameters["message"] ?: "",
                    onValueChange = { updateParameter("message", it) }
                )
            }

            ActionType.MODIFY_RESPONSE -> {
                RuleTextField(
                    label = "Field Number to Modify",
                    value = parameters["fieldNumber"] ?: "",
                    onValueChange = { updateParameter("fieldNumber", it) },
                    keyboardType = KeyboardType.Number
                )
                RuleTextField(
                    label = "New Value",
                    value = parameters["newValue"] ?: "",
                    onValueChange = { updateParameter("newValue", it) }
                )
                RuleTextField(
                    label = "Additional Data",
                    value = parameters["additionalData"] ?: "",
                    onValueChange = { updateParameter("additionalData", it) }
                )
            }

            ActionType.DELAY_RESPONSE -> {
                RuleTextField(
                    label = "Delay (milliseconds)",
                    value = parameters["delayMs"] ?: "",
                    onValueChange = { updateParameter("delayMs", it) },
                    keyboardType = KeyboardType.Number
                )
                RuleTextField(
                    label = "Reason",
                    value = parameters["reason"] ?: "",
                    onValueChange = { updateParameter("reason", it) }
                )
            }

            ActionType.LOG_EVENT -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LogLevelDropdown(
                        selectedLevel = parameters["level"] ?: "INFO",
                        onLevelChange = { updateParameter("level", it) },
                        modifier = Modifier.weight(1f)
                    )
                    RuleTextField(
                        label = "Category",
                        value = parameters["category"] ?: "",
                        onValueChange = { updateParameter("category", it) },
                        modifier = Modifier.weight(1f)
                    )
                }
                RuleTextField(
                    label = "Custom Message",
                    value = parameters["customMessage"] ?: "",
                    onValueChange = { updateParameter("customMessage", it) }
                )
            }

            ActionType.BLOCK_TRANSACTION -> {
                RuleTextField(
                    label = "Block Reason",
                    value = parameters["reason"] ?: "",
                    onValueChange = { updateParameter("reason", it) }
                )
                RuleTextField(
                    label = "Block Duration (minutes, 0 = permanent)",
                    value = parameters["blockDuration"] ?: "",
                    onValueChange = { updateParameter("blockDuration", it) },
                    keyboardType = KeyboardType.Number
                )
            }

            ActionType.ROUTE_TO_HSM -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RuleTextField(
                        label = "HSM Slot",
                        value = parameters["hsmSlot"] ?: "",
                        onValueChange = { updateParameter("hsmSlot", it) },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    RuleTextField(
                        label = "Operation Type",
                        value = parameters["operationType"] ?: "",
                        onValueChange = { updateParameter("operationType", it) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            ActionType.TRIGGER_ALERT -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AlertTypeDropdown(
                        selectedType = parameters["alertType"] ?: "INFO",
                        onTypeChange = { updateParameter("alertType", it) },
                        modifier = Modifier.weight(1f)
                    )
                    AlertPriorityDropdown(
                        selectedPriority = parameters["priority"] ?: "MEDIUM",
                        onPriorityChange = { updateParameter("priority", it) },
                        modifier = Modifier.weight(1f)
                    )
                }
                RuleTextField(
                    label = "Alert Message",
                    value = parameters["alertMessage"] ?: "",
                    onValueChange = { updateParameter("alertMessage", it) }
                )
            }

            ActionType.INCREMENT_COUNTER -> {
                RuleTextField(
                    label = "Counter Name",
                    value = parameters["counterName"] ?: "",
                    onValueChange = { updateParameter("counterName", it) }
                )
                RuleTextField(
                    label = "Increment Value",
                    value = parameters["incrementValue"] ?: "1",
                    onValueChange = { updateParameter("incrementValue", it) },
                    keyboardType = KeyboardType.Number
                )
            }
        }
    }
}

@Composable
fun LogLevelDropdown(
    selectedLevel: String,
    onLevelChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val levels = listOf("DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL")

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedLevel,
            onValueChange = { },
            label = { Text("Log Level") },
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
            shape = RoundedCornerShape(8.dp)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            levels.forEach { level ->
                DropdownMenuItem(
                    onClick = {
                        onLevelChange(level)
                        expanded = false
                    }
                ) {
                    Text(level)
                }
            }
        }
    }
}

@Composable
fun AlertTypeDropdown(
    selectedType: String,
    onTypeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val types = listOf("INFO", "WARNING", "SECURITY", "FRAUD", "PERFORMANCE", "COMPLIANCE")

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedType,
            onValueChange = { },
            label = { Text("Alert Type") },
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
            shape = RoundedCornerShape(8.dp)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            types.forEach { type ->
                DropdownMenuItem(
                    onClick = {
                        onTypeChange(type)
                        expanded = false
                    }
                ) {
                    Text(type)
                }
            }
        }
    }
}

@Composable
fun AlertPriorityDropdown(
    selectedPriority: String,
    onPriorityChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val priorities = listOf("LOW", "MEDIUM", "HIGH", "CRITICAL")

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedPriority,
            onValueChange = { },
            label = { Text("Priority") },
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
            shape = RoundedCornerShape(8.dp)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            priorities.forEach { priority ->
                DropdownMenuItem(
                    onClick = {
                        onPriorityChange(priority)
                        expanded = false
                    }
                ) {
                    Text(priority)
                }
            }
        }
    }
}

// --- QUICK ACTIONS ---

@Composable
fun RuleQuickActions(
    activeView: String,
    selectedRule: BehaviorRule?,
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
                    when (activeView) {
                        "list" -> "Manage behavior rules and categories"
                        "editor" -> if (selectedRule != null) "Editing: ${selectedRule.name}" else "Creating new rule"
                        else -> "Rule management operations"
                    },
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (activeView) {
                    "list" -> {
                        OutlinedButton(
                            onClick = { onQuickAction("import") },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Upload,
                                contentDescription = "Import",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import Rules")
                        }

                        OutlinedButton(
                            onClick = { onQuickAction("templates") },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Notes,
                                contentDescription = "Templates",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Templates")
                        }

                        Button(
                            onClick = { onQuickAction("add_rule") },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Add,
                                contentDescription = "Add",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Rule")
                        }
                    }
                    "editor" -> {
                        OutlinedButton(
                            onClick = { onQuickAction("test_rule") },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                Icons.Outlined.PlayArrow,
                                contentDescription = "Test",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Rule")
                        }

                        Button(
                            onClick = { onQuickAction("save_rule") },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Save,
                                contentDescription = "Save",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Rule")
                        }
                    }
                }
            }
        }
    }
}

// --- DIALOGS ---

@Composable
fun RuleTemplateDialog(
    templates: List<BehaviorRule>,
    onDismiss: () -> Unit,
    onSelectTemplate: (BehaviorRule) -> Unit
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
                        text = "Rule Templates",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Choose a template to get started quickly",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Templates List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(templates, key = { it.id }) { template ->
                        RuleTemplateCard(
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
fun RuleTemplateCard(
    template: BehaviorRule,
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
                imageVector = template.category.icon,
                contentDescription = template.category.displayName,
                tint = template.category.color,
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
                    text = template.description,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(
                        text = template.category.displayName,
                        backgroundColor = template.category.color.copy(alpha = 0.1f),
                        textColor = template.category.color
                    )
                    Chip(
                        text = "${template.conditions.size} conditions",
                        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                        textColor = MaterialTheme.colors.primary
                    )
                    Chip(
                        text = "${template.actions.size} actions",
                        backgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.1f),
                        textColor = MaterialTheme.colors.secondary
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
fun RuleTestDialog(
    rule: BehaviorRule?,
    onDismiss: () -> Unit
) {
    var testData by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    var testResults by remember { mutableStateOf<List<RuleTestResult>>(emptyList()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(600.dp)
                .heightIn(max = 700.dp),
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
                            text = "Rule Testing & Simulation",
                            style = MaterialTheme.typography.h6,
                            fontWeight = FontWeight.Bold
                        )
                        if (rule != null) {
                            Text(
                                text = "Testing: ${rule.name}",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Test Data Input
                Text(
                    "Test Transaction Data",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = testData,
                    onValueChange = { testData = it },
                    label = { Text("ISO8583 Message (Hex)") },
                    placeholder = { Text("Enter test transaction data...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Test Data Templates
                Text(
                    "Quick Templates",
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(getTestDataTemplates()) { template ->
                        OutlinedButton(
                            onClick = { testData = template.data },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                template.name,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Test Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Test Results",
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = {
                            isRunning = true
                            // Simulate rule testing
                            kotlinx.coroutines.MainScope().launch {
                                kotlinx.coroutines.delay(1500)
                                testResults = if (rule != null) {
                                    listOf(
                                        RuleTestResult(
                                            ruleId = rule.id,
                                            ruleName = rule.name,
                                            matched = true,
                                            executionTime = 45,
                                            actionsExecuted = rule.actions.map { it.type.displayName }
                                        )
                                    )
                                } else {
                                    // Test all rules
                                    getSampleRules().map { testRule ->
                                        RuleTestResult(
                                            ruleId = testRule.id,
                                            ruleName = testRule.name,
                                            matched = Math.random() > 0.5,
                                            executionTime = (10..100).random().toLong(),
                                            actionsExecuted = if (Math.random() > 0.5) testRule.actions.map { it.type.displayName } else emptyList()
                                        )
                                    }
                                }
                                isRunning = false
                            }
                        },
                        enabled = testData.isNotBlank() && !isRunning
                    ) {
                        if (isRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colors.onPrimary
                            )
                        } else {
                            Icon(
                                Icons.Outlined.PlayArrow,
                                contentDescription = "Run Test",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isRunning) "Testing..." else "Run Test")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Test Results
                Card(
                    modifier = Modifier.fillMaxSize(),
                    elevation = 1.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (testResults.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.PlayArrow,
                                    contentDescription = "No tests",
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                                )
                                Text(
                                    "No test results yet",
                                    style = MaterialTheme.typography.body2,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    "Enter test data and click 'Run Test' to see results",
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(testResults, key = { it.ruleId }) { result ->
                                RuleTestResultCard(result)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RuleTestResultCard(result: RuleTestResult) {
    Card(
        backgroundColor = if (result.matched)
            Color(0xFF197A3E).copy(alpha = 0.1f)
        else
            MaterialTheme.colors.surface,
        elevation = if (result.matched) 1.dp else 0.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (result.matched) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
                        contentDescription = if (result.matched) "Matched" else "No match",
                        tint = if (result.matched) Color(0xFF197A3E) else MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = result.ruleName,
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "${result.executionTime}ms",
                    style = MaterialTheme.typography.caption,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            if (result.matched && result.actionsExecuted.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Actions Executed:",
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Bold
                )

                result.actionsExecuted.forEach { action ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                    ) {
                        Icon(
                            Icons.Outlined.PlayArrow,
                            contentDescription = "Action",
                            modifier = Modifier.size(10.dp),
                            tint = MaterialTheme.colors.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = action,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            if (result.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.1f),
                    elevation = 0.dp
                ) {
                    Text(
                        text = "Error: ${result.errorMessage}",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.error
                    )
                }
            }
        }
    }
}

// --- UTILITY FUNCTIONS ---

data class TestDataTemplate(
    val name: String,
    val data: String
)

fun getTestDataTemplates(): List<TestDataTemplate> = listOf(
    TestDataTemplate(
        "Purchase $100",
        "0200B220000108000000000000001000040000000100001234567890123456120112341234123412345000"
    ),
    TestDataTemplate(
        "ATM Withdrawal",
        "0200B220000108000000000000002000040000000050001234567890123456120112341234123412345001"
    ),
    TestDataTemplate(
        "Balance Inquiry",
        "0200B220000108000000000000003100040000000000001234567890123456120112341234123412345000"
    ),
    TestDataTemplate(
        "High Amount $5000",
        "0200B220000108000000000000001000040000005000001234567890123456120112341234123412345000"
    )
)

fun formatNumber(number: Int): String {
    return when {
        number >= 1000000 -> "${number / 1000000}M"
        number >= 1000 -> "${number / 1000}K"
        else -> number.toString()
    }
}

@Composable
fun RuleCategoryFilterDropdown(
    selectedCategory: RuleCategory?,
    onSelectionChange: (RuleCategory?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = selectedCategory?.displayName ?: "All Categories",
            onValueChange = { },
            label = { Text("Category") },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            leadingIcon = selectedCategory?.let { category ->
                {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = category.color,
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
                Text("All Categories")
            }

            Divider()

            RuleCategory.values().forEach { category ->
                DropdownMenuItem(
                    onClick = {
                        onSelectionChange(category)
                        expanded = false
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            tint = category.color,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(category.displayName)
                    }
                }
            }
        }
    }
}

@Composable
fun RuleSortDropdown(
    sortBy: String,
    onSortChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val sortOptions = mapOf(
        "priority" to "Priority",
        "name" to "Name",
        "category" to "Category",
        "executions" to "Executions"
    )

    Box {
        OutlinedTextField(
            value = "Sort by ${sortOptions[sortBy]}",
            onValueChange = { },
            label = { Text("Sort") },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Sort,
                    contentDescription = "Sort",
                    modifier = Modifier.size(20.dp)
                )
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
            sortOptions.forEach { (key, displayName) ->
                DropdownMenuItem(
                    onClick = {
                        onSortChange(key)
                        expanded = false
                    }
                ) {
                    Text("Sort by $displayName")
                }
            }
        }
    }
}

// --- RULE CATEGORY OVERVIEW ---

@Composable
fun RuleCategoryOverview(
    rules: List<BehaviorRule>,
    onCategorySelect: (RuleCategory) -> Unit
) {
    Text(
        "Rule Categories",
        style = MaterialTheme.typography.subtitle2,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colors.primary
    )

    Spacer(modifier = Modifier.height(8.dp))

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(RuleCategory.values()) { category ->
            val categoryRules = rules.filter { it.category == category }
            val enabledCount = categoryRules.count { it.enabled }

            RuleCategoryCard(
                category = category,
                totalRules = categoryRules.size,
                enabledRules = enabledCount,
                onClick = { onCategorySelect(category) }
            )
        }
    }
}

@Composable
fun RuleCategoryCard(
    category: RuleCategory,
    totalRules: Int,
    enabledRules: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() },
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = category.color.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.displayName,
                tint = category.color,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = category.displayName,
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = enabledRules.toString(),
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Bold,
                    color = category.color
                )
                Text(
                    text = "/ $totalRules",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// --- RULE TABLE COMPONENTS ---

@Composable
fun RuleTableHeader() {
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
                text = "Rule Name",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(2f)
            )
            Text(
                text = "Category",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1.5f)
            )
            Text(
                text = "Conditions",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1.5f)
            )
            Text(
                text = "Actions",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1.5f)
            )
            Text(
                text = "Status",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Priority",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.8f)
            )
            Text(
                text = "Actions",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1.2f)
            )
        }
    }
}

@Composable
fun RuleTableRow(
    rule: BehaviorRule,
    onSelect: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rule Name with Icon
            Row(
                modifier = Modifier.weight(2f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = rule.category.icon,
                    contentDescription = rule.category.displayName,
                    tint = rule.category.color,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = rule.name,
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (rule.description.isNotEmpty()) {
                        Text(
                            text = rule.description,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Category
            Text(
                text = rule.category.displayName,
                style = MaterialTheme.typography.body2,
                modifier = Modifier.weight(1.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Conditions Summary
            Column(modifier = Modifier.weight(1.5f)) {
                Text(
                    text = "${rule.conditions.size} condition${if (rule.conditions.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Medium
                )
                if (rule.conditions.isNotEmpty()) {
                    Text(
                        text = rule.conditions.first().type.displayName,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Actions Summary
            Column(modifier = Modifier.weight(1.5f)) {
                Text(
                    text = "${rule.actions.size} action${if (rule.actions.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Medium
                )
                if (rule.actions.isNotEmpty()) {
                    Text(
                        text = rule.actions.first().type.displayName,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Status
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = rule.enabled,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.size(32.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF197A3E),
                        checkedTrackColor = Color(0xFF197A3E).copy(alpha = 0.5f)
                    )
                )
            }

            // Priority
            Card(
                backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp),
                elevation = 0.dp,
                modifier = Modifier.weight(0.8f)
            ) {
                Text(
                    text = rule.priority.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            // Actions
            Row(
                modifier = Modifier.weight(1.2f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onSelect,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Edit",
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
fun EmptyRulesState(
    searchQuery: String,
    selectedCategory: RuleCategory?,
    onAddRule: () -> Unit,
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
                Icons.Outlined.Rule,
                contentDescription = "No rules",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
            )

            Text(
                text = when {
                    searchQuery.isNotEmpty() -> "No rules match your search"
                    selectedCategory != null -> "No rules in this category"
                    else -> "No behavior rules found"
                },
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Text(
                text = when {
                    searchQuery.isNotEmpty() || selectedCategory != null -> "Try adjusting your search or filters"
                    else -> "Create your first behavior rule to get started"
                },
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (searchQuery.isNotEmpty() || selectedCategory != null) {
                    OutlinedButton(onClick = onClearFilters) {
                        Text("Clear Filters")
                    }
                }

                Button(onClick = onAddRule) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = "Add",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add First Rule")
                }
            }
        }
    }
}

// --- RULE EDITOR VIEW ---

@Composable
fun RuleEditorView(
    rule: BehaviorRule?,
    onRuleChange: (BehaviorRule) -> Unit,
    onTestRule: () -> Unit,
    onBack: () -> Unit
) {
    val isEditing = rule != null

    var ruleName by remember(rule) { mutableStateOf(rule?.name ?: "") }
    var ruleDescription by remember(rule) { mutableStateOf(rule?.description ?: "") }
    var selectedCategory by remember(rule) { mutableStateOf(rule?.category ?: RuleCategory.CUSTOM) }
    var priority by remember(rule) { mutableStateOf(rule?.priority?.toString() ?: "0") }
    var enabled by remember(rule) { mutableStateOf(rule?.enabled ?: true) }
    var conditions by remember(rule) { mutableStateOf(rule?.conditions ?: emptyList()) }
    var actions by remember(rule) { mutableStateOf(rule?.actions ?: emptyList()) }

    fun updateRule() {
        val updatedRule = (rule ?: BehaviorRule(
            name = "",
            category = RuleCategory.CUSTOM
        )).copy(
            name = ruleName,
            description = ruleDescription,
            category = selectedCategory,
            priority = priority.toIntOrNull() ?: 0,
            enabled = enabled,
            conditions = conditions,
            actions = actions,
            lastModified = LocalDateTime.now()
        )
        onRuleChange(updatedRule)
    }

    Column(modifier = Modifier.padding(16.dp)) {

        // Header with Back Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Outlined.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isEditing) "Edit Rule" else "Create New Rule",
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold
                    )
                    if (isEditing && rule != null) {
                        Text(
                            text = "Last modified: ${rule.lastModified.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))}",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onTestRule,
                    enabled = ruleName.isNotBlank() && conditions.isNotEmpty() && actions.isNotEmpty(),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.PlayArrow,
                        contentDescription = "Test",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Rule")
                }

                Button(
                    onClick = { updateRule() },
                    enabled = ruleName.isNotBlank(),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.Save,
                        contentDescription = "Save",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Rule")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Rule Editor Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Basic Information
            item {
                RuleEditorSection(title = "Basic Information", icon = Icons.Outlined.Info) {
                    RuleTextField(
                        label = "Rule Name *",
                        value = ruleName,
                        onValueChange = {
                            ruleName = it
                            updateRule()
                        }
                    )

                    RuleTextField(
                        label = "Description",
                        value = ruleDescription,
                        onValueChange = {
                            ruleDescription = it
                            updateRule()
                        }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            RuleCategoryDropdown(
                                selectedCategory = selectedCategory,
                                onSelectionChange = {
                                    selectedCategory = it
                                    updateRule()
                                }
                            )
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            RuleTextField(
                                label = "Priority",
                                value = priority,
                                onValueChange = {
                                    priority = it
                                    updateRule()
                                },
                                keyboardType = KeyboardType.Number
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Rule Enabled",
                                style = MaterialTheme.typography.body2,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Rule will be active and process transactions",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        Switch(
                            checked = enabled,
                            onCheckedChange = {
                                enabled = it
                                updateRule()
                            }
                        )
                    }
                }
            }

            // Conditions Builder
            item {
                RuleEditorSection(title = "Conditions", icon = Icons.Outlined.FilterList) {
                    Text(
                        "Define when this rule should be triggered",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    conditions.forEachIndexed { index, condition ->
                        ConditionBuilder(
                            condition = condition,
                            showOperator = index > 0,
                            onConditionChange = { updatedCondition ->
                                conditions = conditions.mapIndexed { i, c ->
                                    if (i == index) updatedCondition else c
                                }
                                updateRule()
                            },
                            onDelete = {
                                conditions = conditions.filterIndexed { i, _ -> i != index }
                                updateRule()
                            }
                        )

                        if (index < conditions.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            conditions = conditions + RuleCondition(
                                type = ConditionType.COMMAND_MATCH
                            )
                            updateRule()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = "Add Condition",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Condition")
                    }
                }
            }

            // Actions Builder
            item {
                RuleEditorSection(title = "Actions", icon = Icons.Outlined.PlaylistPlay) {
                    Text(
                        "Define what happens when conditions are met",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    actions.forEachIndexed { index, action ->
                        ActionBuilder(
                            action = action,
                            onActionChange = { updatedAction ->
                                actions = actions.mapIndexed { i, a ->
                                    if (i == index) updatedAction else a
                                }
                                updateRule()
                            },
                            onDelete = {
                                actions = actions.filterIndexed { i, _ -> i != index }
                                updateRule()
                            }
                        )

                        if (index < actions.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            actions = actions + RuleAction(
                                type = ActionType.RETURN_STATUS
                            )
                            updateRule()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = "Add Action",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Action")
                    }
                }
            }
        }
    }
}

@Composable
fun RuleEditorSection(
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
fun RuleTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(8.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = MaterialTheme.colors.primary,
            unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
        )
    )
}

@Composable
fun RuleCategoryDropdown(
    selectedCategory: RuleCategory,
    onSelectionChange: (RuleCategory) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = selectedCategory.displayName,
            onValueChange = { },
            label = { Text("Category") },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            leadingIcon = {
                Icon(
                    imageVector = selectedCategory.icon,
                    contentDescription = null,
                    tint = selectedCategory.color,
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
            RuleCategory.values().forEach { category ->
                DropdownMenuItem(
                    onClick = {
                        onSelectionChange(category)
                        expanded = false
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            tint = category.color,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(category.displayName)
                    }
                }
            }
        }
    }
}

@Composable
fun RuleSearchAndFilters(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: RuleCategory?,
    onCategoryChange: (RuleCategory?) -> Unit,
    sortBy: String,
    onSortChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search rules by name or description...") },
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
            // Category Filter
            Box(modifier = Modifier.weight(1f)) {
                RuleCategoryFilterDropdown(
                    selectedCategory = selectedCategory,
                    onSelectionChange = onCategoryChange
                )
            }

            // Sort Filter
            Box(modifier = Modifier.weight(1f)) {
                RuleSortDropdown(
                    sortBy = sortBy,
                    onSortChange = onSortChange
                )
            }

            // Clear Filters Button
            if (selectedCategory != null || searchQuery.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        onCategoryChange(null)
                        onSearchQueryChange("")
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