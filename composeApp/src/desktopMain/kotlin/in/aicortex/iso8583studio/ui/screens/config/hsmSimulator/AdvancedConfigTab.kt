package `in`.aicortex.iso8583studio.ui.screens.config.hsmSimulator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.AdvancedOptionsConfiguration
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.CustomCommandHandler
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.DatabaseIntegration
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.DebugConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.ErrorInjectionConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.ErrorInjectionType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.EventHookConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.EventHookType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.ExternalIntegrationConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.ExternalIntegrationType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.FailureSimulationConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.FailureSimulationType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.LoadTestingConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.LoadTestingPattern
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.LogLevel
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.MockResponseConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.MockResponseType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.PluginConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.ResponseDelayConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.ResponseDelayType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.RestApiIntegration
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.TestDataGenerationConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.TestDataType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.WebhookIntegration


@Composable
fun AdvancedOptionsTab(
    advancedConfig: AdvancedOptionsConfiguration,
    onConfigUpdated: (AdvancedOptionsConfiguration) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentConfig by remember { mutableStateOf(advancedConfig) }

    LaunchedEffect(currentConfig) {
        onConfigUpdated(currentConfig)
    }

    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Simulation Behavior
        AdvancedSection(
            title = "Simulation Behavior",
            icon = Icons.Default.Psychology
        ) {
            SimulationBehaviorSection(
                responseDelayConfig = currentConfig.responseDelayConfig,
                errorInjectionConfig = currentConfig.errorInjectionConfig,
                loadTestingConfig = currentConfig.loadTestingConfig,
                failureSimulationConfig = currentConfig.failureSimulationConfig,
                onResponseDelayChanged = {
                    currentConfig = currentConfig.copy(responseDelayConfig = it)
                },
                onErrorInjectionChanged = {
                    currentConfig = currentConfig.copy(errorInjectionConfig = it)
                },
                onLoadTestingChanged = {
                    currentConfig = currentConfig.copy(loadTestingConfig = it)
                },
                onFailureSimulationChanged = {
                    currentConfig = currentConfig.copy(failureSimulationConfig = it)
                }
            )
        }

        // Integration Settings
        AdvancedSection(
            title = "Integration Settings",
            icon = Icons.Default.Link
        ) {
            IntegrationSettingsSection(
                pluginConfig = currentConfig.pluginConfig,
                customCommandHandlers = currentConfig.customCommandHandlers,
                eventHookConfig = currentConfig.eventHookConfig,
                externalIntegrationConfig = currentConfig.externalIntegrationConfig,
                onPluginConfigChanged = {
                    currentConfig = currentConfig.copy(pluginConfig = it)
                },
                onCommandHandlersChanged = {
                    currentConfig = currentConfig.copy(customCommandHandlers = it)
                },
                onEventHookConfigChanged = {
                    currentConfig = currentConfig.copy(eventHookConfig = it)
                },
                onExternalIntegrationChanged = {
                    currentConfig = currentConfig.copy(externalIntegrationConfig = it)
                }
            )
        }

        // Development Features
        AdvancedSection(
            title = "Development Features",
            icon = Icons.Default.Code
        ) {
            DevelopmentFeaturesSection(
                debugConfig = currentConfig.debugConfig,
                testDataGenerationConfig = currentConfig.testDataGenerationConfig,
                mockResponseConfig = currentConfig.mockResponseConfig,
                onDebugConfigChanged = {
                    currentConfig = currentConfig.copy(debugConfig = it)
                },
                onTestDataConfigChanged = {
                    currentConfig = currentConfig.copy(testDataGenerationConfig = it)
                },
                onMockResponseConfigChanged = {
                    currentConfig = currentConfig.copy(mockResponseConfig = it)
                }
            )
        }
    }
}

@Composable
private fun AdvancedSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp,
        backgroundColor = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colors.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colors.onSurface
                )
            }
            content()
        }
    }
}

@Composable
private fun SimulationBehaviorSection(
    responseDelayConfig: ResponseDelayConfig,
    errorInjectionConfig: ErrorInjectionConfig,
    loadTestingConfig: LoadTestingConfig,
    failureSimulationConfig: FailureSimulationConfig,
    onResponseDelayChanged: (ResponseDelayConfig) -> Unit,
    onErrorInjectionChanged: (ErrorInjectionConfig) -> Unit,
    onLoadTestingChanged: (LoadTestingConfig) -> Unit,
    onFailureSimulationChanged: (FailureSimulationConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Response Delay Simulation
        ResponseDelaySection(
            config = responseDelayConfig,
            onConfigChanged = onResponseDelayChanged
        )

        // Error Injection Settings
        ErrorInjectionSection(
            config = errorInjectionConfig,
            onConfigChanged = onErrorInjectionChanged
        )

        // Load Testing Parameters
        LoadTestingSection(
            config = loadTestingConfig,
            onConfigChanged = onLoadTestingChanged
        )

        // Failure Simulation
        FailureSimulationSection(
            config = failureSimulationConfig,
            onConfigChanged = onFailureSimulationChanged
        )
    }
}

@Composable
private fun ResponseDelaySection(
    config: ResponseDelayConfig,
    onConfigChanged: (ResponseDelayConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colors.primary
                )
                Text(
                    "Response Delay Simulation",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Medium
                )
            }

            DropdownSelector(
                label = "Delay Type",
                options = ResponseDelayType.values().toList(),
                selectedOption = config.delayType,
                onOptionSelected = { onConfigChanged(config.copy(delayType = it)) },
                displayName = { it.displayName },
                icon = Icons.Default.Timer
            )

            // Delay Type Description
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 0.dp,
                backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.05f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colors.primary
                    )
                    Text(
                        config.delayType.description,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            // Delay Configuration based on type
            when (config.delayType) {
                ResponseDelayType.FIXED -> {
                    OutlinedTextField(
                        value = config.fixedDelayMs.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { delay ->
                                if (delay >= 0) {
                                    onConfigChanged(config.copy(fixedDelayMs = delay))
                                }
                            }
                        },
                        label = { Text("Fixed Delay (ms)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(Icons.Default.Timer, null, modifier = Modifier.size(20.dp))
                        }
                    )
                }

                ResponseDelayType.RANDOM -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = config.minDelayMs.toString(),
                            onValueChange = {
                                it.toIntOrNull()?.let { delay ->
                                    if (delay >= 0) {
                                        onConfigChanged(config.copy(minDelayMs = delay))
                                    }
                                }
                            },
                            label = { Text("Min Delay (ms)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = {
                                Icon(Icons.Default.Schedule, null, modifier = Modifier.size(20.dp))
                            }
                        )

                        OutlinedTextField(
                            value = config.maxDelayMs.toString(),
                            onValueChange = {
                                it.toIntOrNull()?.let { delay ->
                                    if (delay >= config.minDelayMs) {
                                        onConfigChanged(config.copy(maxDelayMs = delay))
                                    }
                                }
                            },
                            label = { Text("Max Delay (ms)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = {
                                Icon(Icons.Default.Schedule, null, modifier = Modifier.size(20.dp))
                            }
                        )
                    }
                }

                ResponseDelayType.REALISTIC -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = config.networkLatencyMs.toString(),
                            onValueChange = {
                                it.toIntOrNull()?.let { latency ->
                                    if (latency >= 0) {
                                        onConfigChanged(config.copy(networkLatencyMs = latency))
                                    }
                                }
                            },
                            label = { Text("Network Latency (ms)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = {
                                Icon(Icons.Default.Wifi, null, modifier = Modifier.size(20.dp))
                            }
                        )

                        OutlinedTextField(
                            value = config.processingDelayMs.toString(),
                            onValueChange = {
                                it.toIntOrNull()?.let { delay ->
                                    if (delay >= 0) {
                                        onConfigChanged(config.copy(processingDelayMs = delay))
                                    }
                                }
                            },
                            label = { Text("Processing Delay (ms)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = {
                                Icon(Icons.Default.Memory, null, modifier = Modifier.size(20.dp))
                            }
                        )
                    }
                }

                else -> {
                    // No additional configuration needed
                }
            }

            // Jitter Configuration
            if (config.delayType != ResponseDelayType.NONE) {
                JitterConfiguration(
                    config = config,
                    onConfigChanged = onConfigChanged
                )
            }
        }
    }
}

@Composable
private fun JitterConfiguration(
    config: ResponseDelayConfig,
    onConfigChanged: (ResponseDelayConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colors.primary
                )
                Column {
                    Text(
                        "Enable Jitter",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Add random variation to delays",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            Switch(
                checked = config.enableJitter,
                onCheckedChange = { onConfigChanged(config.copy(enableJitter = it)) }
            )
        }

        if (config.enableJitter) {
            OutlinedTextField(
                value = config.jitterPercentage.toString(),
                onValueChange = {
                    it.toIntOrNull()?.let { percentage ->
                        if (percentage in 0..100) {
                            onConfigChanged(config.copy(jitterPercentage = percentage))
                        }
                    }
                },
                label = { Text("Jitter Percentage") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = {
                    Icon(Icons.Default.Percent, null, modifier = Modifier.size(20.dp))
                }
            )
        }
    }
}

@Composable
private fun ErrorInjectionSection(
    config: ErrorInjectionConfig,
    onConfigChanged: (ErrorInjectionConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colors.primary
                    )
                    Column {
                        Text(
                            "Error Injection Settings",
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Simulate various error conditions for testing",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                Switch(
                    checked = config.enableErrorInjection,
                    onCheckedChange = { onConfigChanged(config.copy(enableErrorInjection = it)) }
                )
            }

            if (config.enableErrorInjection) {
                // Error Rate Configuration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = (config.errorRate * 100).toString(),
                        onValueChange = {
                            it.toDoubleOrNull()?.let { rate ->
                                if (rate in 0.0..100.0) {
                                    onConfigChanged(config.copy(errorRate = rate / 100.0))
                                }
                            }
                        },
                        label = { Text("Error Rate (%)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(Icons.Default.Percent, null, modifier = Modifier.size(20.dp))
                        }
                    )

                    // Error Burst Configuration
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Error Burst Mode",
                                style = MaterialTheme.typography.caption,
                                fontWeight = FontWeight.Medium
                            )
                            Switch(
                                checked = config.errorBurstMode,
                                onCheckedChange = { onConfigChanged(config.copy(errorBurstMode = it)) }
                            )
                        }
                    }
                }

                if (config.errorBurstMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = config.errorBurstDuration.toString(),
                            onValueChange = {
                                it.toIntOrNull()?.let { duration ->
                                    if (duration > 0) {
                                        onConfigChanged(config.copy(errorBurstDuration = duration))
                                    }
                                }
                            },
                            label = { Text("Burst Duration (s)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = {
                                Icon(Icons.Default.Timer, null, modifier = Modifier.size(20.dp))
                            }
                        )

                        OutlinedTextField(
                            value = (config.errorBurstRate * 100).toString(),
                            onValueChange = {
                                it.toDoubleOrNull()?.let { rate ->
                                    if (rate in 0.0..100.0) {
                                        onConfigChanged(config.copy(errorBurstRate = rate / 100.0))
                                    }
                                }
                            },
                            label = { Text("Burst Error Rate (%)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = {
                                Icon(Icons.Default.Speed, null, modifier = Modifier.size(20.dp))
                            }
                        )
                    }
                }

                // Error Types Selection
                ErrorTypesSelector(
                    selectedTypes = config.enabledErrorTypes,
                    onTypesChanged = { onConfigChanged(config.copy(enabledErrorTypes = it)) }
                )
            }
        }
    }
}

@Composable
private fun ErrorTypesSelector(
    selectedTypes: Set<ErrorInjectionType>,
    onTypesChanged: (Set<ErrorInjectionType>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Error Types",
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = { onTypesChanged(ErrorInjectionType.values().toSet()) }
                ) {
                    Text("Select All", style = MaterialTheme.typography.caption)
                }
                TextButton(
                    onClick = { onTypesChanged(emptySet()) }
                ) {
                    Text("Clear All", style = MaterialTheme.typography.caption)
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.height(200.dp)
        ) {
            items(ErrorInjectionType.values()) { errorType ->
                ErrorTypeCard(
                    errorType = errorType,
                    isSelected = errorType in selectedTypes,
                    onClick = {
                        val newTypes = if (errorType in selectedTypes) {
                            selectedTypes - errorType
                        } else {
                            selectedTypes + errorType
                        }
                        onTypesChanged(newTypes)
                    }
                )
            }
        }
    }
}

@Composable
private fun ErrorTypeCard(
    errorType: ErrorInjectionType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        elevation = if (isSelected) 2.dp else 0.dp,
        backgroundColor = if (isSelected) MaterialTheme.colors.error.copy(alpha = 0.1f) else MaterialTheme.colors.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(8.dp),
            elevation = ButtonDefaults.elevation(0.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent,
                contentColor = if (isSelected) MaterialTheme.colors.error else MaterialTheme.colors.onSurface
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    errorType.displayName,
                    style = MaterialTheme.typography.caption,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun LoadTestingSection(
    config: LoadTestingConfig,
    onConfigChanged: (LoadTestingConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colors.primary
                    )
                    Column {
                        Text(
                            "Load Testing Parameters",
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Configure load testing scenarios",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                Switch(
                    checked = config.enableLoadTesting,
                    onCheckedChange = { onConfigChanged(config.copy(enableLoadTesting = it)) }
                )
            }

            if (config.enableLoadTesting) {
                DropdownSelector(
                    label = "Testing Pattern",
                    options = LoadTestingPattern.values().toList(),
                    selectedOption = config.testingPattern,
                    onOptionSelected = { onConfigChanged(config.copy(testingPattern = it)) },
                    displayName = { it.displayName },
                    icon = Icons.Default.TrendingUp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = config.baseTransactionRate.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { rate ->
                                if (rate > 0) {
                                    onConfigChanged(config.copy(baseTransactionRate = rate))
                                }
                            }
                        },
                        label = { Text("Base TPS") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(Icons.Default.Speed, null, modifier = Modifier.size(20.dp))
                        }
                    )

                    OutlinedTextField(
                        value = config.maxTransactionRate.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { rate ->
                                if (rate >= config.baseTransactionRate) {
                                    onConfigChanged(config.copy(maxTransactionRate = rate))
                                }
                            }
                        },
                        label = { Text("Max TPS") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(Icons.Default.TrendingUp, null, modifier = Modifier.size(20.dp))
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = config.concurrentUsers.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { users ->
                                if (users > 0) {
                                    onConfigChanged(config.copy(concurrentUsers = users))
                                }
                            }
                        },
                        label = { Text("Concurrent Users") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(Icons.Default.Group, null, modifier = Modifier.size(20.dp))
                        }
                    )

                    OutlinedTextField(
                        value = config.testDuration.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { duration ->
                                if (duration > 0) {
                                    onConfigChanged(config.copy(testDuration = duration))
                                }
                            }
                        },
                        label = { Text("Test Duration (s)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(Icons.Default.Timer, null, modifier = Modifier.size(20.dp))
                        }
                    )
                }

                if (config.testingPattern == LoadTestingPattern.RAMP_UP) {
                    OutlinedTextField(
                        value = config.rampUpDuration.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { duration ->
                                if (duration > 0) {
                                    onConfigChanged(config.copy(rampUpDuration = duration))
                                }
                            }
                        },
                        label = { Text("Ramp Up Duration (s)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(Icons.Default.TrendingUp, null, modifier = Modifier.size(20.dp))
                        }
                    )
                }

                AdvancedOption(
                    title = "Enable Metrics Collection",
                    description = "Collect performance metrics during load testing",
                    checked = config.enableMetrics,
                    onCheckedChange = { onConfigChanged(config.copy(enableMetrics = it)) },
                    icon = Icons.Default.Analytics
                )
            }
        }
    }
}

@Composable
private fun FailureSimulationSection(
    config: FailureSimulationConfig,
    onConfigChanged: (FailureSimulationConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colors.error
                    )
                    Column {
                        Text(
                            "Failure Simulation",
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Simulate system failures for resilience testing",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                Switch(
                    checked = config.enableFailureSimulation,
                    onCheckedChange = { onConfigChanged(config.copy(enableFailureSimulation = it)) }
                )
            }

            if (config.enableFailureSimulation) {
                // Failure Types Selection
                FailureTypesSelector(
                    selectedTypes = config.enabledFailureTypes,
                    onTypesChanged = { onConfigChanged(config.copy(enabledFailureTypes = it)) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = (config.failureRate * 100).toString(),
                        onValueChange = {
                            it.toDoubleOrNull()?.let { rate ->
                                if (rate in 0.0..100.0) {
                                    onConfigChanged(config.copy(failureRate = rate / 100.0))
                                }
                            }
                        },
                        label = { Text("Failure Rate (%)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(Icons.Default.Percent, null, modifier = Modifier.size(20.dp))
                        }
                    )

                    OutlinedTextField(
                        value = config.meanTimeToFailure.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { time ->
                                if (time > 0) {
                                    onConfigChanged(config.copy(meanTimeToFailure = time))
                                }
                            }
                        },
                        label = { Text("MTTF (s)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(Icons.Default.Timer, null, modifier = Modifier.size(20.dp))
                        }
                    )
                }

                OutlinedTextField(
                    value = config.meanTimeToRepair.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let { time ->
                            if (time > 0) {
                                onConfigChanged(config.copy(meanTimeToRepair = time))
                            }
                        }
                    },
                    label = { Text("Mean Time to Repair (s)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = {
                        Icon(Icons.Default.Build, null, modifier = Modifier.size(20.dp))
                    }
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdvancedOption(
                        title = "Cascading Failures",
                        description = "Enable cascading failure scenarios",
                        checked = config.enableCascadingFailures,
                        onCheckedChange = { onConfigChanged(config.copy(enableCascadingFailures = it)) },
                        icon = Icons.Default.AccountTree
                    )

                    AdvancedOption(
                        title = "Failure Recovery",
                        description = "Enable automatic failure recovery",
                        checked = config.failureRecoveryEnabled,
                        onCheckedChange = { onConfigChanged(config.copy(failureRecoveryEnabled = it)) },
                        icon = Icons.Default.Restore
                    )
                }
            }
        }
    }
}

@Composable
private fun FailureTypesSelector(
    selectedTypes: Set<FailureSimulationType>,
    onTypesChanged: (Set<FailureSimulationType>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Failure Types",
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.height(160.dp)
        ) {
            items(FailureSimulationType.values()) { failureType ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Checkbox(
                        checked = failureType in selectedTypes,
                        onCheckedChange = { checked ->
                            val newTypes = if (checked) {
                                selectedTypes + failureType
                            } else {
                                selectedTypes - failureType
                            }
                            onTypesChanged(newTypes)
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        failureType.displayName,
                        style = MaterialTheme.typography.caption,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun IntegrationSettingsSection(
    pluginConfig: PluginConfig,
    customCommandHandlers: List<CustomCommandHandler>,
    eventHookConfig: EventHookConfig,
    externalIntegrationConfig: ExternalIntegrationConfig,
    onPluginConfigChanged: (PluginConfig) -> Unit,
    onCommandHandlersChanged: (List<CustomCommandHandler>) -> Unit,
    onEventHookConfigChanged: (EventHookConfig) -> Unit,
    onExternalIntegrationChanged: (ExternalIntegrationConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Plugin Configuration
        PluginConfigurationSection(
            config = pluginConfig,
            onConfigChanged = onPluginConfigChanged
        )

        // Custom Command Handlers
        CustomCommandHandlersSection(
            handlers = customCommandHandlers,
            onHandlersChanged = onCommandHandlersChanged
        )

        // Event Hooks
        EventHooksSection(
            config = eventHookConfig,
            onConfigChanged = onEventHookConfigChanged
        )

        // External Integrations
        ExternalIntegrationsSection(
            config = externalIntegrationConfig,
            onConfigChanged = onExternalIntegrationChanged
        )
    }
}

@Composable
private fun PluginConfigurationSection(
    config: PluginConfig,
    onConfigChanged: (PluginConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colors.primary
                    )
                    Column {
                        Text(
                            "Plugin Configuration",
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Enable and configure custom plugins",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                Switch(
                    checked = config.enablePlugins,
                    onCheckedChange = { onConfigChanged(config.copy(enablePlugins = it)) }
                )
            }

            if (config.enablePlugins) {
                OutlinedTextField(
                    value = config.pluginDirectory,
                    onValueChange = { onConfigChanged(config.copy(pluginDirectory = it)) },
                    label = { Text("Plugin Directory") },
                    placeholder = { Text("./plugins") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Folder, null, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        IconButton(onClick = { /* File picker */ }) {
                            Icon(Icons.Default.FolderOpen, null)
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = config.maxPluginMemory.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { memory ->
                                if (memory > 0) {
                                    onConfigChanged(config.copy(maxPluginMemory = memory))
                                }
                            }
                        },
                        label = { Text("Max Memory (MB)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(Icons.Default.Memory, null, modifier = Modifier.size(20.dp))
                        }
                    )

                    OutlinedTextField(
                        value = config.pluginTimeout.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { timeout ->
                                if (timeout > 0) {
                                    onConfigChanged(config.copy(pluginTimeout = timeout))
                                }
                            }
                        },
                        label = { Text("Timeout (s)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(Icons.Default.Timer, null, modifier = Modifier.size(20.dp))
                        }
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdvancedOption(
                        title = "Hot Reloading",
                        description = "Enable hot reloading of plugins during runtime",
                        checked = config.enableHotReloading,
                        onCheckedChange = { onConfigChanged(config.copy(enableHotReloading = it)) },
                        icon = Icons.Default.Refresh
                    )

                    AdvancedOption(
                        title = "Plugin Sandboxing",
                        description = "Run plugins in isolated sandboxed environment",
                        checked = config.enableSandboxing,
                        onCheckedChange = { onConfigChanged(config.copy(enableSandboxing = it)) },
                        icon = Icons.Default.Security
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomCommandHandlersSection(
    handlers: List<CustomCommandHandler>,
    onHandlersChanged: (List<CustomCommandHandler>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colors.primary
                    )
                    Text(
                        "Custom Command Handlers",
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Medium
                    )
                }

                OutlinedButton(
                    onClick = {
                        // Add new handler logic
                        val newHandler = CustomCommandHandler(
                            name = "New Handler",
                            command = "",
                            handler = "",
                            description = ""
                        )
                        onHandlersChanged(handlers + newHandler)
                    }
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", style = MaterialTheme.typography.caption)
                }
            }

            if (handlers.isEmpty()) {
                Text(
                    "No custom command handlers configured",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    handlers.forEachIndexed { index, handler ->
                        CommandHandlerItem(
                            handler = handler,
                            onHandlerChanged = { updatedHandler ->
                                val newHandlers = handlers.toMutableList()
                                newHandlers[index] = updatedHandler
                                onHandlersChanged(newHandlers)
                            },
                            onDelete = {
                                onHandlersChanged(handlers - handler)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandHandlerItem(
    handler: CustomCommandHandler,
    onHandlerChanged: (CustomCommandHandler) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp,
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    handler.name,
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Medium
                )

                Row {
                    Switch(
                        checked = handler.enabled,
                        onCheckedChange = { onHandlerChanged(handler.copy(enabled = it)) },
                        modifier = Modifier.size(24.dp)
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colors.error
                        )
                    }
                }
            }

            Text(
                "Command: ${handler.command}",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun EventHooksSection(
    config: EventHookConfig,
    onConfigChanged: (EventHookConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colors.primary
                    )
                    Column {
                        Text(
                            "Event Hooks",
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Configure event-driven hooks and triggers",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                Switch(
                    checked = config.enableEventHooks,
                    onCheckedChange = { onConfigChanged(config.copy(enableEventHooks = it)) }
                )
            }

            if (config.enableEventHooks) {
                OutlinedTextField(
                    value = config.hookScriptDirectory,
                    onValueChange = { onConfigChanged(config.copy(hookScriptDirectory = it)) },
                    label = { Text("Hook Script Directory") },
                    placeholder = { Text("./hooks") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Folder, null, modifier = Modifier.size(20.dp))
                    }
                )

                OutlinedTextField(
                    value = config.hookTimeout.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let { timeout ->
                            if (timeout > 0) {
                                onConfigChanged(config.copy(hookTimeout = timeout))
                            }
                        }
                    },
                    label = { Text("Hook Timeout (seconds)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = {
                        Icon(Icons.Default.Timer, null, modifier = Modifier.size(20.dp))
                    }
                )

                // Event Hook Types
                EventHookTypesSelector(
                    enabledHooks = config.enabledHooks,
                    onHooksChanged = { onConfigChanged(config.copy(enabledHooks = it)) }
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdvancedOption(
                        title = "Asynchronous Hooks",
                        description = "Execute hooks asynchronously without blocking",
                        checked = config.enableAsynchronousHooks,
                        onCheckedChange = { onConfigChanged(config.copy(enableAsynchronousHooks = it)) },
                        icon = Icons.Default.Schedule
                    )

                    AdvancedOption(
                        title = "Hook Chaining",
                        description = "Enable chaining of multiple hooks for same event",
                        checked = config.enableHookChaining,
                        onCheckedChange = { onConfigChanged(config.copy(enableHookChaining = it)) },
                        icon = Icons.Default.Link
                    )
                }
            }
        }
    }
}

@Composable
private fun EventHookTypesSelector(
    enabledHooks: Map<EventHookType, Boolean>,
    onHooksChanged: (Map<EventHookType, Boolean>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Event Hook Types",
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            EventHookType.values().forEach { hookType ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        hookType.displayName,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = enabledHooks[hookType] ?: false,
                        onCheckedChange = { checked ->
                            onHooksChanged(enabledHooks + (hookType to checked))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExternalIntegrationsSection(
    config: ExternalIntegrationConfig,
    onConfigChanged: (ExternalIntegrationConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Api,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colors.primary
                )
                Text(
                    "External Integrations",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Medium
                )
            }

            // Integration Types Selection
            ExternalIntegrationTypesSelector(
                selectedIntegrations = config.enabledIntegrations,
                onIntegrationsChanged = { onConfigChanged(config.copy(enabledIntegrations = it)) }
            )

            // Configuration for enabled integrations
            if (ExternalIntegrationType.REST_API in config.enabledIntegrations) {
                RestApiIntegrationConfig(
                    config = config.restApiConfig,
                    onConfigChanged = { onConfigChanged(config.copy(restApiConfig = it)) }
                )
            }

            if (ExternalIntegrationType.DATABASE in config.enabledIntegrations) {
                DatabaseIntegrationConfig(
                    config = config.databaseConfig,
                    onConfigChanged = { onConfigChanged(config.copy(databaseConfig = it)) }
                )
            }

            if (ExternalIntegrationType.WEBHOOK in config.enabledIntegrations) {
                WebhookIntegrationConfig(
                    config = config.webhookConfig,
                    onConfigChanged = { onConfigChanged(config.copy(webhookConfig = it)) }
                )
            }
        }
    }
}

@Composable
private fun ExternalIntegrationTypesSelector(
    selectedIntegrations: Set<ExternalIntegrationType>,
    onIntegrationsChanged: (Set<ExternalIntegrationType>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Integration Types",
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(200.dp)
        ) {
            items(ExternalIntegrationType.values()) { integrationType ->
                IntegrationTypeCard(
                    integrationType = integrationType,
                    isSelected = integrationType in selectedIntegrations,
                    onClick = {
                        val newIntegrations = if (integrationType in selectedIntegrations) {
                            selectedIntegrations - integrationType
                        } else {
                            selectedIntegrations + integrationType
                        }
                        onIntegrationsChanged(newIntegrations)
                    }
                )
            }
        }
    }
}

@Composable
private fun IntegrationTypeCard(
    integrationType: ExternalIntegrationType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        elevation = if (isSelected) 2.dp else 0.dp,
        backgroundColor = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.1f) else MaterialTheme.colors.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(8.dp),
            elevation = ButtonDefaults.elevation(0.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent,
                contentColor = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else integrationType.icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    integrationType.displayName,
                    style = MaterialTheme.typography.caption,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun RestApiIntegrationConfig(
    config: RestApiIntegration,
    onConfigChanged: (RestApiIntegration) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "REST API Configuration",
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Medium
            )

            OutlinedTextField(
                value = config.baseUrl,
                onValueChange = { onConfigChanged(config.copy(baseUrl = it)) },
                label = { Text("Base URL") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Http, null, modifier = Modifier.size(20.dp))
                }
            )
        }
    }
}

@Composable
private fun DatabaseIntegrationConfig(
    config: DatabaseIntegration,
    onConfigChanged: (DatabaseIntegration) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Database Configuration",
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Medium
            )

            OutlinedTextField(
                value = config.connectionString,
                onValueChange = { onConfigChanged(config.copy(connectionString = it)) },
                label = { Text("Connection String") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Storage, null, modifier = Modifier.size(20.dp))
                }
            )
        }
    }
}

@Composable
private fun WebhookIntegrationConfig(
    config: WebhookIntegration,
    onConfigChanged: (WebhookIntegration) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Webhook Configuration",
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Medium
            )

            OutlinedTextField(
                value = config.webhookUrl,
                onValueChange = { onConfigChanged(config.copy(webhookUrl = it)) },
                label = { Text("Webhook URL") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Webhook, null, modifier = Modifier.size(20.dp))
                }
            )
        }
    }
}

@Composable
private fun DevelopmentFeaturesSection(
    debugConfig: DebugConfig,
    testDataGenerationConfig: TestDataGenerationConfig,
    mockResponseConfig: MockResponseConfig,
    onDebugConfigChanged: (DebugConfig) -> Unit,
    onTestDataConfigChanged: (TestDataGenerationConfig) -> Unit,
    onMockResponseConfigChanged: (MockResponseConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Debug Mode Settings
        DebugModeSection(
            config = debugConfig,
            onConfigChanged = onDebugConfigChanged
        )

        // Test Data Generation
        TestDataGenerationSection(
            config = testDataGenerationConfig,
            onConfigChanged = onTestDataConfigChanged
        )

        // Mock Responses
        MockResponseSection(
            config = mockResponseConfig,
            onConfigChanged = onMockResponseConfigChanged
        )
    }
}

@Composable
private fun DebugModeSection(
    config: DebugConfig,
    onConfigChanged: (DebugConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colors.primary
                    )
                    Column {
                        Text(
                            "Debug Mode Settings",
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Enhanced debugging and logging features",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                Switch(
                    checked = config.enableDebugMode,
                    onCheckedChange = { onConfigChanged(config.copy(enableDebugMode = it)) }
                )
            }

            if (config.enableDebugMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DropdownSelector(
                        label = "Log Level",
                        options = LogLevel.values().toList(),
                        selectedOption = config.logLevel,
                        onOptionSelected = { onConfigChanged(config.copy(logLevel = it)) },
                        displayName = { it.displayName },
                        icon = Icons.Default.List,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = config.debugPort.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { port ->
                                if (port in 1024..65535) {
                                    onConfigChanged(config.copy(debugPort = port))
                                }
                            }
                        },
                        label = { Text("Debug Port") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(Icons.Default.Router, null, modifier = Modifier.size(20.dp))
                        }
                    )
                }

                OutlinedTextField(
                    value = config.logFilePath,
                    onValueChange = { onConfigChanged(config.copy(logFilePath = it)) },
                    label = { Text("Log File Path") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.FileCopy, null, modifier = Modifier.size(20.dp))
                    }
                )

                OutlinedTextField(
                    value = config.maxLogFileSize.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let { size ->
                            if (size > 0) {
                                onConfigChanged(config.copy(maxLogFileSize = size))
                            }
                        }
                    },
                    label = { Text("Max Log File Size (MB)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = {
                        Icon(Icons.Default.Storage, null, modifier = Modifier.size(20.dp))
                    }
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdvancedOption(
                        title = "Verbose Logging",
                        description = "Enable detailed verbose logging output",
                        checked = config.enableVerboseLogging,
                        onCheckedChange = { onConfigChanged(config.copy(enableVerboseLogging = it)) },
                        icon = Icons.Default.Article
                    )

                    AdvancedOption(
                        title = "Network Tracing",
                        description = "Trace all network communications",
                        checked = config.enableNetworkTracing,
                        onCheckedChange = { onConfigChanged(config.copy(enableNetworkTracing = it)) },
                        icon = Icons.Default.NetworkCheck
                    )

                    AdvancedOption(
                        title = "Memory Profiling",
                        description = "Enable memory usage profiling",
                        checked = config.enableMemoryProfiling,
                        onCheckedChange = { onConfigChanged(config.copy(enableMemoryProfiling = it)) },
                        icon = Icons.Default.Memory
                    )

                    AdvancedOption(
                        title = "Performance Profiling",
                        description = "Enable performance profiling and metrics",
                        checked = config.enablePerformanceProfiling,
                        onCheckedChange = { onConfigChanged(config.copy(enablePerformanceProfiling = it)) },
                        icon = Icons.Default.Speed
                    )

                    AdvancedOption(
                        title = "Remote Debugging",
                        description = "Enable remote debugging capabilities",
                        checked = config.enableRemoteDebugging,
                        onCheckedChange = { onConfigChanged(config.copy(enableRemoteDebugging = it)) },
                        icon = Icons.Default.DesktopWindows
                    )

                    AdvancedOption(
                        title = "Stack Traces",
                        description = "Include stack traces in error logs",
                        checked = config.enableStackTraces,
                        onCheckedChange = { onConfigChanged(config.copy(enableStackTraces = it)) },
                        icon = Icons.Default.List
                    )
                }
            }
        }
    }
}

@Composable
private fun TestDataGenerationSection(
    config: TestDataGenerationConfig,
    onConfigChanged: (TestDataGenerationConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DataArray,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colors.primary
                    )
                    Column {
                        Text(
                            "Test Data Generation",
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Generate realistic test data for development",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                Switch(
                    checked = config.enableTestDataGeneration,
                    onCheckedChange = { onConfigChanged(config.copy(enableTestDataGeneration = it)) }
                )
            }

            if (config.enableTestDataGeneration) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = config.dataSetSize.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { size ->
                                if (size > 0) {
                                    onConfigChanged(config.copy(dataSetSize = size))
                                }
                            }
                        },
                        label = { Text("Data Set Size") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(Icons.Default.Numbers, null, modifier = Modifier.size(20.dp))
                        }
                    )

                    OutlinedTextField(
                        value = config.seedValue.toString(),
                        onValueChange = {
                            it.toLongOrNull()?.let { seed ->
                                onConfigChanged(config.copy(seedValue = seed))
                            }
                        },
                        label = { Text("Seed Value") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(Icons.Default.Casino, null, modifier = Modifier.size(20.dp))
                        }
                    )
                }

                OutlinedTextField(
                    value = config.outputDirectory,
                    onValueChange = { onConfigChanged(config.copy(outputDirectory = it)) },
                    label = { Text("Output Directory") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Folder, null, modifier = Modifier.size(20.dp))
                    }
                )

                // Test Data Types Selection
                TestDataTypesSelector(
                    selectedTypes = config.enabledDataTypes,
                    onTypesChanged = { onConfigChanged(config.copy(enabledDataTypes = it)) }
                )

                AdvancedOption(
                    title = "Generate Realistic Data",
                    description = "Generate realistic data patterns and relationships",
                    checked = config.generateRealisticData,
                    onCheckedChange = { onConfigChanged(config.copy(generateRealisticData = it)) },
                    icon = Icons.Default.Psychology
                )
            }
        }
    }
}

@Composable
private fun TestDataTypesSelector(
    selectedTypes: Set<TestDataType>,
    onTypesChanged: (Set<TestDataType>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Test Data Types",
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.height(160.dp)
        ) {
            items(TestDataType.values()) { dataType ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Checkbox(
                        checked = dataType in selectedTypes,
                        onCheckedChange = { checked ->
                            val newTypes = if (checked) {
                                selectedTypes + dataType
                            } else {
                                selectedTypes - dataType
                            }
                            onTypesChanged(newTypes)
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        dataType.displayName,
                        style = MaterialTheme.typography.caption,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun MockResponseSection(
    config: MockResponseConfig,
    onConfigChanged: (MockResponseConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colors.primary
                    )
                    Column {
                        Text(
                            "Mock Responses",
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Configure simulated response patterns",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                Switch(
                    checked = config.enableMockResponses,
                    onCheckedChange = { onConfigChanged(config.copy(enableMockResponses = it)) }
                )
            }

            if (config.enableMockResponses) {
                DropdownSelector(
                    label = "Default Response Type",
                    options = MockResponseType.values().toList(),
                    selectedOption = config.defaultResponseType,
                    onOptionSelected = { onConfigChanged(config.copy(defaultResponseType = it)) },
                    displayName = { it.displayName },
                    icon = Icons.Default.Reply
                )

                // Response Distribution
                ResponseDistributionSection(
                    distribution = config.responseDistribution,
                    onDistributionChanged = { onConfigChanged(config.copy(responseDistribution = it)) }
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdvancedOption(
                        title = "Response Variation",
                        description = "Add variation to mock responses",
                        checked = config.enableResponseVariation,
                        onCheckedChange = { onConfigChanged(config.copy(enableResponseVariation = it)) },
                        icon = Icons.Default.Shuffle
                    )

                    AdvancedOption(
                        title = "Realistic Timing",
                        description = "Use realistic response timing patterns",
                        checked = config.enableRealisticTiming,
                        onCheckedChange = { onConfigChanged(config.copy(enableRealisticTiming = it)) },
                        icon = Icons.Default.Schedule
                    )
                }
            }
        }
    }
}

@Composable
private fun ResponseDistributionSection(
    distribution: Map<MockResponseType, Double>,
    onDistributionChanged: (Map<MockResponseType, Double>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Response Distribution (%)",
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Medium
            )

            distribution.forEach { (responseType, percentage) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        responseType.displayName,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${(percentage * 100).toInt()}%",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun AdvancedOption(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colors.primary
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Medium
            )
            Text(
                description,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

// NOTE: The DropdownSelector composable was missing from your provided jumbled code
// I'm adding a minimal version here so the rest of the code can compile.
// You might need to replace this with your actual DropdownSelector implementation.
@Composable
private fun <T> DropdownSelector(
    label: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    displayName: (T) -> String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = displayName(selectedOption),
        onValueChange = { /* Read-only */ },
        label = { Text(label) },
        readOnly = true,
        modifier = modifier.fillMaxWidth(),
        leadingIcon = {
            Icon(icon, null, modifier = Modifier.size(20.dp))
        },
        trailingIcon = {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.ArrowDropDown, null)
            }
        }
    )
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        options.forEach { option ->
            DropdownMenuItem(onClick = {
                onOptionSelected(option)
                expanded = false
            }) {
                Text(displayName(option))
            }
        }
    }
}