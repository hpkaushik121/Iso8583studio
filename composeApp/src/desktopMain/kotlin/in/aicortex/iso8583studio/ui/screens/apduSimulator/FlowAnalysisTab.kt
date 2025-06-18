package `in`.aicortex.iso8583studio.ui.screens.apduSimulator

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.CardServiceImpl
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Flow Analysis Tab - Professional EMV transaction flow visualization and analysis
 * Features comprehensive flow diagram, step-by-step analysis, and compliance checking
 */
@Composable
fun FlowAnalysisTab(
    cardService: CardServiceImpl,
    logEntries: List<LogEntry>,
    modifier: Modifier = Modifier
) {
    var selectedFlowTemplate by remember { mutableStateOf(EmvFlowTemplate.PURCHASE) }
    var analysisMode by remember { mutableStateOf(FlowAnalysisMode.VISUAL_DIAGRAM) }
    var showCompliancePanel by remember { mutableStateOf(true) }
    var selectedStep by remember { mutableStateOf<Int?>(null) }
    var autoRefresh by remember { mutableStateOf(true) }
    var flowSteps by remember { mutableStateOf<List<FlowStep>>(emptyList()) }
    var complianceResults by remember { mutableStateOf<List<ComplianceResult>>(emptyList()) }
    var flowDeviations by remember { mutableStateOf<List<FlowDeviation>>(emptyList()) }

    val coroutineScope = rememberCoroutineScope()

    // Auto-refresh flow analysis
    LaunchedEffect(logEntries, autoRefresh) {
        if (autoRefresh) {
            while (autoRefresh) {
                val analyzedFlow = FlowAnalyzer.analyzeLogEntries(logEntries)
                flowSteps = analyzedFlow.steps
                complianceResults = FlowValidator.validateFlow(analyzedFlow, selectedFlowTemplate)
                flowDeviations = FlowDeviationDetector.detectDeviations(analyzedFlow, selectedFlowTemplate)
                delay(2000)
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Control Panel
        FlowAnalysisControlPanel(
            selectedFlowTemplate = selectedFlowTemplate,
            onFlowTemplateChange = { selectedFlowTemplate = it },
            analysisMode = analysisMode,
            onAnalysisModeChange = { analysisMode = it },
            showCompliancePanel = showCompliancePanel,
            onShowCompliancePanelChange = { showCompliancePanel = it },
            autoRefresh = autoRefresh,
            onAutoRefreshChange = { autoRefresh = it },
            flowSteps = flowSteps,
            complianceResults = complianceResults,
            onRefreshClick = {
                coroutineScope.launch {
                    val analyzedFlow = FlowAnalyzer.analyzeLogEntries(logEntries)
                    flowSteps = analyzedFlow.steps
                    complianceResults = FlowValidator.validateFlow(analyzedFlow, selectedFlowTemplate)
                    flowDeviations = FlowDeviationDetector.detectDeviations(analyzedFlow, selectedFlowTemplate)
                }
            }
        )

        // Main Content Area
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Panel - Flow Visualization (65%)
            Card(
                modifier = Modifier
                    .weight(0.65f)
                    .fillMaxHeight(),
                elevation = 4.dp,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Flow Header
                    FlowVisualizationHeader(
                        analysisMode = analysisMode,
                        selectedFlowTemplate = selectedFlowTemplate,
                        stepCount = flowSteps.size,
                        selectedStep = selectedStep
                    )

                    Divider()

                    // Flow Content
                    when (analysisMode) {
                        FlowAnalysisMode.VISUAL_DIAGRAM -> {
                            EmvFlowDiagram(
                                flowSteps = flowSteps,
                                templateFlow = EmvFlowTemplates.getTemplate(selectedFlowTemplate),
                                selectedStep = selectedStep,
                                onStepClick = { selectedStep = it },
                                deviations = flowDeviations
                            )
                        }
                        FlowAnalysisMode.TIMELINE_VIEW -> {
                            TimelineView(
                                flowSteps = flowSteps,
                                selectedStep = selectedStep,
                                onStepClick = { selectedStep = it },
                                deviations = flowDeviations
                            )
                        }
                        FlowAnalysisMode.COMMAND_SEQUENCE -> {
                            CommandSequenceView(
                                flowSteps = flowSteps,
                                selectedStep = selectedStep,
                                onStepClick = { selectedStep = it }
                            )
                        }
                    }
                }
            }

            // Right Panel - Analysis & Compliance (35%)
            Card(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight(),
                elevation = 4.dp,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Analysis Header
                    AnalysisHeader(
                        showCompliancePanel = showCompliancePanel,
                        onToggleCompliance = { showCompliancePanel = !showCompliancePanel },
                        complianceResults = complianceResults,
                        deviations = flowDeviations
                    )

                    Divider()

                    // Analysis Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Step Details Panel
                        if (selectedStep != null && selectedStep!! < flowSteps.size) {
                            StepDetailsPanel(
                                step = flowSteps[selectedStep!!],
                                stepIndex = selectedStep!!,
                                templateStep = EmvFlowTemplates.getTemplate(selectedFlowTemplate)
                                    .steps.getOrNull(selectedStep!!)
                            )
                        }

                        // Compliance Results Panel
                        if (showCompliancePanel) {
                            ComplianceResultsPanel(
                                complianceResults = complianceResults,
                                selectedFlowTemplate = selectedFlowTemplate
                            )
                        }

                        // Flow Deviations Panel
                        FlowDeviationsPanel(
                            deviations = flowDeviations,
                            onDeviationClick = { deviation ->
                                selectedStep = deviation.stepIndex
                            }
                        )

                        // Flow Statistics Panel
                        FlowStatisticsPanel(
                            flowSteps = flowSteps,
                            templateFlow = EmvFlowTemplates.getTemplate(selectedFlowTemplate)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Flow Analysis Control Panel
 */
@Composable
private fun FlowAnalysisControlPanel(
    selectedFlowTemplate: EmvFlowTemplate,
    onFlowTemplateChange: (EmvFlowTemplate) -> Unit,
    analysisMode: FlowAnalysisMode,
    onAnalysisModeChange: (FlowAnalysisMode) -> Unit,
    showCompliancePanel: Boolean,
    onShowCompliancePanelChange: (Boolean) -> Unit,
    autoRefresh: Boolean,
    onAutoRefreshChange: (Boolean) -> Unit,
    flowSteps: List<FlowStep>,
    complianceResults: List<ComplianceResult>,
    onRefreshClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with statistics
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "EMV Flow Analysis",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                // Statistics
                if (flowSteps.isNotEmpty()) {
                    FlowStatusChip(
                        text = "${flowSteps.size} steps",
                        backgroundColor = MaterialTheme.colors.primary,
                        contentColor = Color.White
                    )
                }

                val errorCount = complianceResults.count { !it.isCompliant }
                if (errorCount > 0) {
                    FlowStatusChip(
                        text = "$errorCount errors",
                        backgroundColor = Color(0xFFF44336),
                        contentColor = Color.White
                    )
                }

                val warningCount = complianceResults.count { it.isWarning }
                if (warningCount > 0) {
                    FlowStatusChip(
                        text = "$warningCount warnings",
                        backgroundColor = Color(0xFFFF9800),
                        contentColor = Color.White
                    )
                }

                // Control buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onRefreshClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh analysis",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { /* Export flow */ },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export flow",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Configuration row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flow template selection
                Column {
                    Text(
                        text = "EMV Flow Template",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    var expanded by remember { mutableStateOf(false) }

                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.width(200.dp)
                        ) {
                            Text(selectedFlowTemplate.displayName)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            EmvFlowTemplate.values().forEach { template ->
                                DropdownMenuItem(
                                    onClick = {
                                        onFlowTemplateChange(template)
                                        expanded = false
                                    }
                                ) {
                                    Column {
                                        Text(
                                            text = template.displayName,
                                            style = MaterialTheme.typography.body2,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = template.description,
                                            style = MaterialTheme.typography.caption,
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Analysis mode selection
                Column {
                    Text(
                        text = "Analysis Mode",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(FlowAnalysisMode.values()) { mode ->
                            FilterChip(
                                selected = analysisMode == mode,
                                onClick = { onAnalysisModeChange(mode) },
                                text = mode.displayName,
                                icon = mode.icon
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Options
                Column {
                    Text(
                        text = "Options",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = showCompliancePanel,
                                onCheckedChange = onShowCompliancePanelChange,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colors.primary
                                )
                            )
                            Text(
                                text = "Compliance",
                                style = MaterialTheme.typography.body2,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = autoRefresh,
                                onCheckedChange = onAutoRefreshChange,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colors.primary
                                )
                            )
                            Text(
                                text = "Auto-refresh",
                                style = MaterialTheme.typography.body2,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Flow Visualization Header
 */
@Composable
private fun FlowVisualizationHeader(
    analysisMode: FlowAnalysisMode,
    selectedFlowTemplate: EmvFlowTemplate,
    stepCount: Int,
    selectedStep: Int?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colors.surface
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = analysisMode.icon,
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = analysisMode.displayName,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )

            FlowStatusChip(
                text = selectedFlowTemplate.displayName,
                backgroundColor = MaterialTheme.colors.secondary,
                contentColor = Color.White
            )

            Spacer(modifier = Modifier.weight(1f))

            if (selectedStep != null) {
                FlowStatusChip(
                    text = "Step ${selectedStep + 1}/$stepCount",
                    backgroundColor = MaterialTheme.colors.primary,
                    contentColor = Color.White
                )
            }
        }
    }
}

/**
 * EMV Flow Diagram - Visual representation of command sequences
 */
@Composable
private fun EmvFlowDiagram(
    flowSteps: List<FlowStep>,
    templateFlow: EmvFlowTemplateData,
    selectedStep: Int?,
    onStepClick: (Int) -> Unit,
    deviations: List<FlowDeviation>
) {
    if (flowSteps.isEmpty()) {
        EmptyFlowState()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(flowSteps) { index, step ->
            val isSelected = selectedStep == index
            val hasDeviation = deviations.any { it.stepIndex == index }
            val templateStep = templateFlow.steps.getOrNull(index)

            FlowStepCard(
                step = step,
                stepIndex = index,
                isSelected = isSelected,
                hasDeviation = hasDeviation,
                templateStep = templateStep,
                onClick = { onStepClick(index) },
                isLastStep = index == flowSteps.size - 1
            )
        }
    }
}

/**
 * Timeline View - Step-by-step analysis with timeline
 */
@Composable
private fun TimelineView(
    flowSteps: List<FlowStep>,
    selectedStep: Int?,
    onStepClick: (Int) -> Unit,
    deviations: List<FlowDeviation>
) {
    if (flowSteps.isEmpty()) {
        EmptyFlowState()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(flowSteps) { index, step ->
            val isSelected = selectedStep == index
            val hasDeviation = deviations.any { it.stepIndex == index }

            TimelineStepItem(
                step = step,
                stepIndex = index,
                isSelected = isSelected,
                hasDeviation = hasDeviation,
                onClick = { onStepClick(index) },
                isLastStep = index == flowSteps.size - 1
            )
        }
    }
}

/**
 * Command Sequence View - Detailed command analysis
 */
@Composable
private fun CommandSequenceView(
    flowSteps: List<FlowStep>,
    selectedStep: Int?,
    onStepClick: (Int) -> Unit
) {
    if (flowSteps.isEmpty()) {
        EmptyFlowState()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(flowSteps) { index, step ->
            val isSelected = selectedStep == index

            CommandStepCard(
                step = step,
                stepIndex = index,
                isSelected = isSelected,
                onClick = { onStepClick(index) }
            )
        }
    }
}

/**
 * Flow Step Card for diagram view
 */
@Composable
private fun FlowStepCard(
    step: FlowStep,
    stepIndex: Int,
    isSelected: Boolean,
    hasDeviation: Boolean,
    templateStep: EmvFlowStep?,
    onClick: () -> Unit,
    isLastStep: Boolean
) {
    Column {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            backgroundColor = when {
                isSelected -> MaterialTheme.colors.primary.copy(alpha = 0.15f)
                hasDeviation -> Color(0xFFFFEBEE)
                step.status == FlowStepStatus.SUCCESS -> Color(0xFFE8F5E8)
                step.status == FlowStepStatus.ERROR -> Color(0xFFFFEBEE)
                else -> MaterialTheme.colors.surface
            },
            elevation = if (isSelected) 6.dp else 2.dp,
            border = if (isSelected) {
                BorderStroke(2.dp, MaterialTheme.colors.primary)
            } else if (hasDeviation) {
                BorderStroke(1.dp, Color(0xFFF44336))
            } else null
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Step number and status
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            when (step.status) {
                                FlowStepStatus.SUCCESS -> Color(0xFF4CAF50)
                                FlowStepStatus.ERROR -> Color(0xFFF44336)
                                FlowStepStatus.WARNING -> Color(0xFFFF9800)
                                FlowStepStatus.PENDING -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${stepIndex + 1}",
                        style = MaterialTheme.typography.subtitle2,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Step information
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = step.command,
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary
                        )

                        if (hasDeviation) {
                            FlowStatusChip(
                                text = "DEVIATION",
                                backgroundColor = Color(0xFFF44336),
                                contentColor = Color.White
                            )
                        }

                        templateStep?.let { template ->
                            if (step.command != template.expectedCommand) {
                                FlowStatusChip(
                                    text = "UNEXPECTED",
                                    backgroundColor = Color(0xFFFF9800),
                                    contentColor = Color.White
                                )
                            }
                        }
                    }

                    Text(
                        text = step.description,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SW: ${step.statusWord}",
                            style = MaterialTheme.typography.caption,
                            fontFamily = FontFamily.Monospace,
                            color = if (step.statusWord == "9000") Color(0xFF4CAF50) else Color(0xFFF44336)
                        )

                        Text(
                            text = formatDuration(step.duration),
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )

                        Text(
                            text = formatTimestamp(step.timestamp),
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Step direction indicator
                Icon(
                    imageVector = if (step.direction == FlowDirection.OUTGOING) {
                        Icons.Default.ArrowForward
                    } else {
                        Icons.Default.ArrowBack
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Connection line to next step
        if (!isLastStep) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(16.dp)
                    .background(MaterialTheme.colors.onSurface.copy(alpha = 0.2f))
                    .offset(x = 36.dp)
            )
        }
    }
}

/**
 * Timeline Step Item
 */
@Composable
private fun TimelineStepItem(
    step: FlowStep,
    stepIndex: Int,
    isSelected: Boolean,
    hasDeviation: Boolean,
    onClick: () -> Unit,
    isLastStep: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Timeline track
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isSelected -> MaterialTheme.colors.primary
                            hasDeviation -> Color(0xFFF44336)
                            step.status == FlowStepStatus.SUCCESS -> Color(0xFF4CAF50)
                            step.status == FlowStepStatus.ERROR -> Color(0xFFF44336)
                            else -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                        }
                    )
            )

            if (!isLastStep) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(32.dp)
                        .background(MaterialTheme.colors.onSurface.copy(alpha = 0.2f))
                )
            }
        }

        // Step content
        Card(
            modifier = Modifier.weight(1f),
            backgroundColor = if (isSelected) {
                MaterialTheme.colors.primary.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colors.surface
            },
            elevation = if (isSelected) 2.dp else 1.dp
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = step.command,
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary
                    )

                    FlowStatusChip(
                        text = step.statusWord,
                        backgroundColor = if (step.statusWord == "9000") Color(0xFF4CAF50) else Color(0xFFF44336),
                        contentColor = Color.White
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = formatTimestamp(step.timestamp),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }

                Text(
                    text = step.description,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


/**
 * Command Step Card for sequence view
 */
@Composable
private fun CommandStepCard(
    step: FlowStep,
    stepIndex: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        backgroundColor = if (isSelected) {
            MaterialTheme.colors.primary.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colors.surface
        },
        elevation = if (isSelected) 4.dp else 2.dp,
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colors.primary)
        } else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${stepIndex + 1}.",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )

                Text(
                    text = step.command,
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                FlowStatusChip(
                    text = step.direction.name,
                    backgroundColor = if (step.direction == FlowDirection.OUTGOING) {
                        Color(0xFF2196F3)
                    } else {
                        Color(0xFF4CAF50)
                    },
                    contentColor = Color.White
                )

                FlowStatusChip(
                    text = step.statusWord,
                    backgroundColor = if (step.statusWord == "9000") Color(0xFF4CAF50) else Color(0xFFF44336),
                    contentColor = Color.White
                )
            }

            // Command details
            if (step.commandData.isNotEmpty()) {
                Card(
                    backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
                    elevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = "Command Data:",
                            style = MaterialTheme.typography.caption,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary
                        )
                        Text(
                            text = step.commandData,
                            style = MaterialTheme.typography.caption,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Response details
            if (step.responseData.isNotEmpty()) {
                Card(
                    backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
                    elevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = "Response Data:",
                            style = MaterialTheme.typography.caption,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.secondary
                        )
                        Text(
                            text = step.responseData,
                            style = MaterialTheme.typography.caption,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Timing information
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                )

                Text(
                    text = formatTimestamp(step.timestamp),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )

                Text(
                    text = "Duration: ${formatDuration(step.duration)}",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Empty Flow State
 */
@Composable
private fun EmptyFlowState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Timeline,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Flow Data",
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Start a card session to analyze transaction flow",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

/**
 * Analysis Header
 */
@Composable
private fun AnalysisHeader(
    showCompliancePanel: Boolean,
    onToggleCompliance: () -> Unit,
    complianceResults: List<ComplianceResult>,
    deviations: List<FlowDeviation>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colors.surface
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Analytics,
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = "Flow Analysis",
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            // Quick stats
            val complianceScore = if (complianceResults.isNotEmpty()) {
                (complianceResults.count { it.isCompliant }.toFloat() / complianceResults.size * 100).toInt()
            } else 0

            FlowStatusChip(
                text = "${complianceScore}% compliant",
                backgroundColor = when {
                    complianceScore >= 90 -> Color(0xFF4CAF50)
                    complianceScore >= 70 -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                },
                contentColor = Color.White
            )

            if (deviations.isNotEmpty()) {
                FlowStatusChip(
                    text = "${deviations.size} deviations",
                    backgroundColor = Color(0xFFFF5722),
                    contentColor = Color.White
                )
            }
        }
    }
}

/**
 * Step Details Panel
 */
@Composable
private fun StepDetailsPanel(
    step: FlowStep,
    stepIndex: Int,
    templateStep: EmvFlowStep?
) {
    Card(
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Step ${stepIndex + 1} Details",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary
            )

            StepDetailRow("Command", step.command)
            StepDetailRow("Status", step.status.name)
            StepDetailRow("Status Word", step.statusWord)
            StepDetailRow("Direction", step.direction.name)
            StepDetailRow("Duration", formatDuration(step.duration))
            StepDetailRow("Timestamp", formatTimestamp(step.timestamp))

            if (step.commandData.isNotEmpty()) {
                Divider()
                Text(
                    text = "Command Data:",
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Bold
                )
                Card(
                    backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.3f),
                    elevation = 0.dp
                ) {
                    Text(
                        text = step.commandData,
                        style = MaterialTheme.typography.caption,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            if (step.responseData.isNotEmpty()) {
                Text(
                    text = "Response Data:",
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Bold
                )
                Card(
                    backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.3f),
                    elevation = 0.dp
                ) {
                    Text(
                        text = step.responseData,
                        style = MaterialTheme.typography.caption,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Template comparison
            templateStep?.let { template ->
                Divider()
                Text(
                    text = "Template Comparison:",
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Bold
                )

                val isMatch = step.command == template.expectedCommand
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isMatch) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (isMatch) Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isMatch) "Matches template" else "Deviates from template",
                        style = MaterialTheme.typography.caption,
                        color = if (isMatch) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }

                if (!isMatch) {
                    Text(
                        text = "Expected: ${template.expectedCommand}",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * Compliance Results Panel
 */
@Composable
private fun ComplianceResultsPanel(
    complianceResults: List<ComplianceResult>,
    selectedFlowTemplate: EmvFlowTemplate
) {
    Card(
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "EMV Compliance",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )
            }

            Text(
                text = "Template: ${selectedFlowTemplate.displayName}",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )

            if (complianceResults.isEmpty()) {
                Text(
                    text = "No compliance data available",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            } else {
                complianceResults.forEach { result ->
                    ComplianceResultItem(result = result)
                }
            }
        }
    }
}

/**
 * Flow Deviations Panel
 */
@Composable
private fun FlowDeviationsPanel(
    deviations: List<FlowDeviation>,
    onDeviationClick: (FlowDeviation) -> Unit
) {
    Card(
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (deviations.isNotEmpty()) Color(0xFFFF9800) else MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Flow Deviations",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )

                if (deviations.isNotEmpty()) {
                    FlowStatusChip(
                        text = "${deviations.size}",
                        backgroundColor = Color(0xFFFF9800),
                        contentColor = Color.White
                    )
                }
            }

            if (deviations.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "No deviations detected",
                        style = MaterialTheme.typography.body2,
                        color = Color(0xFF4CAF50)
                    )
                }
            } else {
                deviations.forEach { deviation ->
                    DeviationItem(
                        deviation = deviation,
                        onClick = { onDeviationClick(deviation) }
                    )
                }
            }
        }
    }
}

/**
 * Flow Statistics Panel
 */
@Composable
private fun FlowStatisticsPanel(
    flowSteps: List<FlowStep>,
    templateFlow: EmvFlowTemplateData
) {
    Card(
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Flow Statistics",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )
            }

            if (flowSteps.isEmpty()) {
                Text(
                    text = "No flow data available",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            } else {
                val totalDuration = flowSteps.sumOf { it.duration }
                val successCount = flowSteps.count { it.status == FlowStepStatus.SUCCESS }
                val errorCount = flowSteps.count { it.status == FlowStepStatus.ERROR }
                val avgDuration = if (flowSteps.isNotEmpty()) totalDuration / flowSteps.size else 0L

                StatisticRow("Total Steps", "${flowSteps.size}")
                StatisticRow("Expected Steps", "${templateFlow.steps.size}")
                StatisticRow("Success Rate", "${(successCount.toFloat() / flowSteps.size * 100).toInt()}%")
                StatisticRow("Total Duration", formatDuration(totalDuration))
                StatisticRow("Average Duration", formatDuration(avgDuration))
                StatisticRow("Errors", "$errorCount")
            }
        }
    }
}

// Helper Composables
@Composable
private fun StepDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun ComplianceResultItem(
    result: ComplianceResult
) {
    Card(
        backgroundColor = when {
            result.isCompliant -> Color(0xFFE8F5E8)
            result.isWarning -> Color(0xFFFFF3E0)
            else -> Color(0xFFFFEBEE)
        },
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = when {
                    result.isCompliant -> Icons.Default.CheckCircle
                    result.isWarning -> Icons.Default.Warning
                    else -> Icons.Default.Error
                },
                contentDescription = null,
                tint = when {
                    result.isCompliant -> Color(0xFF4CAF50)
                    result.isWarning -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                },
                modifier = Modifier.size(16.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.rule,
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Medium
                )
                if (result.message.isNotEmpty()) {
                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviationItem(
    deviation: FlowDeviation,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        backgroundColor = Color(0xFFFFF3E0),
        elevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Step ${deviation.stepIndex + 1}",
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )

                FlowStatusChip(
                    text = deviation.type.name,
                    backgroundColor = Color(0xFFFF9800),
                    contentColor = Color.White
                )
            }

            Text(
                text = deviation.description,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
            )

            if (deviation.suggestion.isNotEmpty()) {
                Text(
                    text = "→ ${deviation.suggestion}",
                    style = MaterialTheme.typography.caption,
                    color = Color(0xFFE65100),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

@Composable
private fun StatisticRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun FlowStatusChip(
    text: String,
    backgroundColor: Color,
    contentColor: Color
) {
    Card(
        backgroundColor = backgroundColor,
        elevation = 0.dp,
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.caption,
            color = contentColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    icon: ImageVector
) {
    Card(
        backgroundColor = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
        elevation = if (selected) 2.dp else 0.dp,
        border = if (!selected) BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)) else null,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (selected) Color.White else MaterialTheme.colors.onSurface
            )
            Text(
                text = text,
                style = MaterialTheme.typography.caption,
                color = if (selected) Color.White else MaterialTheme.colors.onSurface
            )
        }
    }
}

// Data Classes and Enums
data class FlowStep(
    val command: String,
    val description: String,
    val commandData: String,
    val responseData: String,
    val statusWord: String,
    val status: FlowStepStatus,
    val direction: FlowDirection,
    val timestamp: Long,
    val duration: Long
)

data class ComplianceResult(
    val rule: String,
    val isCompliant: Boolean,
    val isWarning: Boolean = false,
    val message: String,
    val stepIndex: Int? = null
)

data class FlowDeviation(
    val stepIndex: Int,
    val type: DeviationType,
    val description: String,
    val expectedValue: String,
    val actualValue: String,
    val suggestion: String
)

data class EmvFlowTemplateData(
    val name: String,
    val description: String,
    val steps: List<EmvFlowStep>,
    val mandatorySteps: Set<String>,
    val optionalSteps: Set<String>
)

data class EmvFlowStep(
    val expectedCommand: String,
    val description: String,
    val isMandatory: Boolean,
    val expectedStatusWords: Set<String> = setOf("9000"),
    val nextPossibleSteps: Set<String> = emptySet()
)

data class AnalyzedFlow(
    val steps: List<FlowStep>,
    val startTime: Long,
    val endTime: Long,
    val totalDuration: Long
)

enum class FlowAnalysisMode(val displayName: String, val icon: ImageVector) {
    VISUAL_DIAGRAM("Visual Diagram", Icons.Default.AccountTree),
    TIMELINE_VIEW("Timeline View", Icons.Default.Timeline),
    COMMAND_SEQUENCE("Command Sequence", Icons.Default.List)
}

enum class EmvFlowTemplate(val displayName: String, val description: String) {
    PURCHASE("Purchase Transaction", "Standard EMV purchase flow"),
    CASH_ADVANCE("Cash Advance", "ATM cash withdrawal flow"),
    REFUND("Refund Transaction", "Transaction reversal flow"),
    BALANCE_INQUIRY("Balance Inquiry", "Account balance check flow"),
    PIN_VERIFICATION("PIN Verification", "Cardholder verification flow"),
    CUSTOM("Custom Flow", "User-defined flow template")
}

enum class FlowStepStatus {
    SUCCESS,
    ERROR,
    WARNING,
    PENDING
}

enum class FlowDirection {
    OUTGOING,
    INCOMING
}

enum class DeviationType {
    UNEXPECTED_COMMAND,
    MISSING_COMMAND,
    WRONG_SEQUENCE,
    INVALID_STATUS,
    TIMING_VIOLATION
}

// Utility Objects
object FlowAnalyzer {
    fun analyzeLogEntries(logEntries: List<LogEntry>): AnalyzedFlow {
        val flowSteps = mutableListOf<FlowStep>()

        var startTime = Long.MAX_VALUE
        var endTime = 0L

        // Group command-response pairs
        val commandResponses = mutableListOf<Pair<LogEntry?, LogEntry?>>()
        var currentCommand: LogEntry? = null

        logEntries.forEach { entry ->
            when (entry.type) {
                LogType.INFO -> {
                    if (entry.message.contains("Command:") || entry.message.contains("SELECT") ||
                        entry.message.contains("READ") || entry.message.contains("GET")) {
                        currentCommand = entry
                    } else if (entry.message.contains("Response:") && currentCommand != null) {
                        commandResponses.add(Pair(currentCommand, entry))
                        currentCommand = null
                    }
                }
                else -> { /* Handle other log types */ }
            }
        }

        // Convert to flow steps
        commandResponses.forEachIndexed { index, (command, response) ->
            if (command != null) {
                startTime = minOf(startTime, command.timestamp.toLong())
                endTime = maxOf(endTime, response?.timestamp?.toLong() ?: command.timestamp.toLong())

                val step = FlowStep(
                    command = extractCommand(command.message),
                    description = extractDescription(command.message),
                    commandData = extractCommandData(command.message),
                    responseData = extractResponseData(response?.message ?: ""),
                    statusWord = extractStatusWord(response?.message ?: ""),
                    status = determineStatus(response?.message ?: ""),
                    direction = FlowDirection.OUTGOING,
                    timestamp = command.timestamp.toLong(),
                    duration = (response?.timestamp?.toLong() ?: command.timestamp.toLong()) - command.timestamp.toLong()
                )

                flowSteps.add(step)
            }
        }

        return AnalyzedFlow(
            steps = flowSteps,
            startTime = if (startTime == Long.MAX_VALUE) 0L else startTime,
            endTime = endTime,
            totalDuration = endTime - (if (startTime == Long.MAX_VALUE) 0L else startTime)
        )
    }

    private fun extractCommand(message: String): String {
        return when {
            message.contains("SELECT") -> "SELECT"
            message.contains("READ", true) -> "READ RECORD"
            message.contains("GET DATA") -> "GET DATA"
            message.contains("VERIFY") -> "VERIFY PIN"
            message.contains("GENERATE") -> "GENERATE AC"
            else -> "UNKNOWN"
        }
    }

    private fun extractDescription(message: String): String {
        return when {
            message.contains("SELECT") -> "Application selection"
            message.contains("READ", true) -> "Read application data"
            message.contains("GET DATA") -> "Retrieve data object"
            message.contains("VERIFY") -> "PIN verification"
            message.contains("GENERATE") -> "Generate cryptogram"
            else -> "Command execution"
        }
    }

    private fun extractCommandData(message: String): String {
        val hexPattern = Regex("[0-9A-Fa-f]{8,}")
        return hexPattern.find(message)?.value ?: ""
    }

    private fun extractResponseData(message: String): String {
        val hexPattern = Regex("[0-9A-Fa-f]{8,}")
        return hexPattern.find(message)?.value ?: ""
    }

    private fun extractStatusWord(message: String): String {
        val swPattern = Regex("SW[:\\s]*([0-9A-Fa-f]{4})")
        return swPattern.find(message)?.groupValues?.get(1)?.uppercase() ?: "0000"
    }

    private fun determineStatus(message: String): FlowStepStatus {
        val statusWord = extractStatusWord(message)
        return when {
            statusWord == "9000" -> FlowStepStatus.SUCCESS
            statusWord.startsWith("61") || statusWord.startsWith("62") -> FlowStepStatus.WARNING
            statusWord == "0000" || message.isEmpty() -> FlowStepStatus.PENDING
            else -> FlowStepStatus.ERROR
        }
    }
}

object FlowValidator {
    fun validateFlow(flow: AnalyzedFlow, template: EmvFlowTemplate): List<ComplianceResult> {
        val results = mutableListOf<ComplianceResult>()
        val templateData = EmvFlowTemplates.getTemplate(template)

        // Check mandatory steps
        templateData.mandatorySteps.forEach { mandatoryStep ->
            val stepExists = flow.steps.any { it.command == mandatoryStep }
            results.add(
                ComplianceResult(
                    rule = "Mandatory Step: $mandatoryStep",
                    isCompliant = stepExists,
                    message = if (stepExists) "Required step present" else "Missing mandatory step",
                    stepIndex = if (stepExists) flow.steps.indexOfFirst { it.command == mandatoryStep } else null
                )
            )
        }

        // Check step sequence
        if (flow.steps.isNotEmpty()) {
            val expectedSequence = templateData.steps.map { it.expectedCommand }
            val actualSequence = flow.steps.map { it.command }

            if (expectedSequence.isNotEmpty()) {
                val sequenceMatch = compareSequences(expectedSequence, actualSequence)
                results.add(
                    ComplianceResult(
                        rule = "Command Sequence",
                        isCompliant = sequenceMatch.isCompliant,
                        isWarning = sequenceMatch.hasWarnings,
                        message = sequenceMatch.message
                    )
                )
            }
        }

        // Check status words
        flow.steps.forEachIndexed { index, step ->
            val templateStep = templateData.steps.getOrNull(index)
            templateStep?.let { template ->
                val isValidStatus = template.expectedStatusWords.contains(step.statusWord)
                if (!isValidStatus) {
                    results.add(
                        ComplianceResult(
                            rule = "Status Word Validation",
                            isCompliant = false,
                            message = "Unexpected status word ${step.statusWord} for ${step.command}",
                            stepIndex = index
                        )
                    )
                }
            }
        }

        // Check timing constraints
        val totalDuration = flow.totalDuration
        if (totalDuration > 30000) { // 30 seconds
            results.add(
                ComplianceResult(
                    rule = "Transaction Timing",
                    isCompliant = false,
                    isWarning = true,
                    message = "Transaction duration exceeds recommended time (${formatDuration(totalDuration)})"
                )
            )
        }

        return results
    }

    private fun compareSequences(expected: List<String>, actual: List<String>): SequenceComparison {
        if (expected == actual) {
            return SequenceComparison(
                isCompliant = true,
                hasWarnings = false,
                message = "Command sequence matches template exactly"
            )
        }

        // Check if all mandatory commands are present (order may vary)
        val missingCommands = expected.filter { it !in actual }
        val extraCommands = actual.filter { it !in expected }

        return when {
            missingCommands.isNotEmpty() -> SequenceComparison(
                isCompliant = false,
                hasWarnings = false,
                message = "Missing commands: ${missingCommands.joinToString()}"
            )
            extraCommands.isNotEmpty() -> SequenceComparison(
                isCompliant = true,
                hasWarnings = true,
                message = "Extra commands detected: ${extraCommands.joinToString()}"
            )
            else -> SequenceComparison(
                isCompliant = true,
                hasWarnings = true,
                message = "Commands present but sequence differs from template"
            )
        }
    }
}

object FlowDeviationDetector {
    fun detectDeviations(flow: AnalyzedFlow, template: EmvFlowTemplate): List<FlowDeviation> {
        val deviations = mutableListOf<FlowDeviation>()
        val templateData = EmvFlowTemplates.getTemplate(template)

        flow.steps.forEachIndexed { index, step ->
            val templateStep = templateData.steps.getOrNull(index)

            // Check for unexpected commands
            if (templateStep != null && step.command != templateStep.expectedCommand) {
                deviations.add(
                    FlowDeviation(
                        stepIndex = index,
                        type = DeviationType.UNEXPECTED_COMMAND,
                        description = "Unexpected command '${step.command}' at step ${index + 1}",
                        expectedValue = templateStep.expectedCommand,
                        actualValue = step.command,
                        suggestion = "Consider using '${templateStep.expectedCommand}' instead"
                    )
                )
            }

            // Check for invalid status words
            if (templateStep != null && step.statusWord !in templateStep.expectedStatusWords) {
                deviations.add(
                    FlowDeviation(
                        stepIndex = index,
                        type = DeviationType.INVALID_STATUS,
                        description = "Invalid status word '${step.statusWord}' for command '${step.command}'",
                        expectedValue = templateStep.expectedStatusWords.first(),
                        actualValue = step.statusWord,
                        suggestion = "Check command parameters and card state"
                    )
                )
            }

            // Check for timing violations (commands taking too long)
            if (step.duration > 5000) { // 5 seconds
                deviations.add(
                    FlowDeviation(
                        stepIndex = index,
                        type = DeviationType.TIMING_VIOLATION,
                        description = "Command '${step.command}' took ${formatDuration(step.duration)} to complete",
                        expectedValue = "< 5 seconds",
                        actualValue = formatDuration(step.duration),
                        suggestion = "Check for communication issues or card performance"
                    )
                )
            }
        }

        // Check for missing mandatory steps
        templateData.mandatorySteps.forEach { mandatoryStep ->
            if (!flow.steps.any { it.command == mandatoryStep }) {
                deviations.add(
                    FlowDeviation(
                        stepIndex = -1,
                        type = DeviationType.MISSING_COMMAND,
                        description = "Missing mandatory command '$mandatoryStep'",
                        expectedValue = mandatoryStep,
                        actualValue = "Not present",
                        suggestion = "Add the missing mandatory command to the flow"
                    )
                )
            }
        }

        return deviations
    }
}

object EmvFlowTemplates {
    fun getTemplate(template: EmvFlowTemplate): EmvFlowTemplateData {
        return when (template) {
            EmvFlowTemplate.PURCHASE -> getPurchaseTemplate()
            EmvFlowTemplate.CASH_ADVANCE -> getCashAdvanceTemplate()
            EmvFlowTemplate.REFUND -> getRefundTemplate()
            EmvFlowTemplate.BALANCE_INQUIRY -> getBalanceInquiryTemplate()
            EmvFlowTemplate.PIN_VERIFICATION -> getPinVerificationTemplate()
            EmvFlowTemplate.CUSTOM -> getCustomTemplate()
        }
    }

    private fun getPurchaseTemplate(): EmvFlowTemplateData {
        return EmvFlowTemplateData(
            name = "EMV Purchase Transaction",
            description = "Standard EMV purchase flow according to EMV Book 3",
            steps = listOf(
                EmvFlowStep(
                    expectedCommand = "SELECT",
                    description = "Select Payment System Environment (PSE)",
                    isMandatory = true,
                    expectedStatusWords = setOf("9000"),
                    nextPossibleSteps = setOf("SELECT")
                ),
                EmvFlowStep(
                    expectedCommand = "SELECT",
                    description = "Select Application (AID)",
                    isMandatory = true,
                    expectedStatusWords = setOf("9000"),
                    nextPossibleSteps = setOf("GET DATA", "READ RECORD")
                ),
                EmvFlowStep(
                    expectedCommand = "GET DATA",
                    description = "Get Processing Options (GPO)",
                    isMandatory = true,
                    expectedStatusWords = setOf("9000"),
                    nextPossibleSteps = setOf("READ RECORD")
                ),
                EmvFlowStep(
                    expectedCommand = "READ RECORD",
                    description = "Read Application Data",
                    isMandatory = true,
                    expectedStatusWords = setOf("9000", "6A83"),
                    nextPossibleSteps = setOf("READ RECORD", "GET DATA", "VERIFY PIN")
                ),
                EmvFlowStep(
                    expectedCommand = "GET DATA",
                    description = "Get Application Transaction Counter (ATC)",
                    isMandatory = false,
                    expectedStatusWords = setOf("9000", "6A88"),
                    nextPossibleSteps = setOf("VERIFY PIN", "GENERATE AC")
                ),
                EmvFlowStep(
                    expectedCommand = "VERIFY PIN",
                    description = "Cardholder Verification",
                    isMandatory = false,
                    expectedStatusWords = setOf("9000", "63C2", "63C1", "63C0"),
                    nextPossibleSteps = setOf("GENERATE AC")
                ),
                EmvFlowStep(
                    expectedCommand = "GENERATE AC",
                    description = "Generate Authorization Cryptogram",
                    isMandatory = true,
                    expectedStatusWords = setOf("9000"),
                    nextPossibleSteps = emptySet()
                )
            ),
            mandatorySteps = setOf("SELECT", "GET DATA", "READ RECORD", "GENERATE AC"),
            optionalSteps = setOf("VERIFY PIN")
        )
    }

    private fun getCashAdvanceTemplate(): EmvFlowTemplateData {
        return EmvFlowTemplateData(
            name = "EMV Cash Advance",
            description = "ATM cash withdrawal flow",
            steps = listOf(
                EmvFlowStep("SELECT", "Select Application", true, setOf("9000")),
                EmvFlowStep("GET DATA", "Get Processing Options", true, setOf("9000")),
                EmvFlowStep("READ RECORD", "Read Application Data", true, setOf("9000")),
                EmvFlowStep("VERIFY PIN", "PIN Verification", true, setOf("9000")),
                EmvFlowStep("GET DATA", "Get PIN Try Counter", false, setOf("9000", "6A88")),
                EmvFlowStep("GENERATE AC", "Generate AC for Cash", true, setOf("9000"))
            ),
            mandatorySteps = setOf("SELECT", "GET DATA", "READ RECORD", "VERIFY PIN", "GENERATE AC"),
            optionalSteps = emptySet()
        )
    }

    private fun getRefundTemplate(): EmvFlowTemplateData {
        return EmvFlowTemplateData(
            name = "EMV Refund Transaction",
            description = "Transaction reversal flow",
            steps = listOf(
                EmvFlowStep("SELECT", "Select Application", true, setOf("9000")),
                EmvFlowStep("GET DATA", "Get Processing Options", true, setOf("9000")),
                EmvFlowStep("READ RECORD", "Read Application Data", true, setOf("9000")),
                EmvFlowStep("GENERATE AC", "Generate Refund AC", true, setOf("9000"))
            ),
            mandatorySteps = setOf("SELECT", "GET DATA", "READ RECORD", "GENERATE AC"),
            optionalSteps = emptySet()
        )
    }

    private fun getBalanceInquiryTemplate(): EmvFlowTemplateData {
        return EmvFlowTemplateData(
            name = "Balance Inquiry",
            description = "Account balance check flow",
            steps = listOf(
                EmvFlowStep("SELECT", "Select Application", true, setOf("9000")),
                EmvFlowStep("VERIFY PIN", "PIN Verification", true, setOf("9000")),
                EmvFlowStep("GET DATA", "Get Account Balance", true, setOf("9000"))
            ),
            mandatorySteps = setOf("SELECT", "VERIFY PIN", "GET DATA"),
            optionalSteps = emptySet()
        )
    }

    private fun getPinVerificationTemplate(): EmvFlowTemplateData {
        return EmvFlowTemplateData(
            name = "PIN Verification",
            description = "Cardholder verification flow",
            steps = listOf(
                EmvFlowStep("SELECT", "Select Application", true, setOf("9000")),
                EmvFlowStep("GET DATA", "Get PIN Try Counter", false, setOf("9000", "6A88")),
                EmvFlowStep("VERIFY PIN", "Verify Cardholder PIN", true, setOf("9000", "63C2", "63C1", "63C0"))
            ),
            mandatorySteps = setOf("SELECT", "VERIFY PIN"),
            optionalSteps = setOf("GET DATA")
        )
    }

    private fun getCustomTemplate(): EmvFlowTemplateData {
        return EmvFlowTemplateData(
            name = "Custom Flow",
            description = "User-defined flow template",
            steps = emptyList(),
            mandatorySteps = emptySet(),
            optionalSteps = emptySet()
        )
    }
}

// Helper data class
data class SequenceComparison(
    val isCompliant: Boolean,
    val hasWarnings: Boolean,
    val message: String
)

// Utility Functions
private fun formatDuration(milliseconds: Long): String {
    return when {
        milliseconds < 1000 -> "${milliseconds}ms"
        milliseconds < 60000 -> String.format("%.1fs", milliseconds / 1000.0)
        else -> {
            val minutes = milliseconds / 60000
            val seconds = (milliseconds % 60000) / 1000
            "${minutes}m ${seconds}s"
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    return formatter.format(Date(timestamp))
}