package `in`.aicortex.iso8583studio.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.data.model.AppSettings
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.components.SectionHeader
import `in`.aicortex.iso8583studio.ui.screens.components.StyledTextField

@Composable
fun GlobalSettingsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "Settings",
                onBackClick = onBack
            )
        },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Global Logging section
            Panel(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SectionHeader(title = "Global Logging")

                    Text(
                        "These settings apply to all simulators (Host, HSM, POS, APDU). " +
                                "When the in-memory log buffer grows large, it can slow down the UI. " +
                                "Use these options to control logging behaviour globally.",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )

                    Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f))

                    // Enable / disable global logging
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable Global Logging", fontWeight = FontWeight.Medium)
                            Text(
                                "When disabled, logs will not be written to the Logs tab or the log file. " +
                                        "Disabling this can significantly improve performance under heavy load.",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = AppSettings.enableGlobalLogging,
                            onCheckedChange = { AppSettings.updateEnableGlobalLogging(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary)
                        )
                    }
                }
            }

            // Auto-clear section
            Panel(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SectionHeader(title = "Auto-Clear Logs")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable Auto-Clear", fontWeight = FontWeight.Medium)
                            Text(
                                "Automatically clear the in-memory log buffer at a regular interval " +
                                        "to prevent performance degradation.",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = AppSettings.autoClearLogsEnabled,
                            onCheckedChange = { AppSettings.updateAutoClearLogsEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary)
                        )
                    }

                    if (AppSettings.autoClearLogsEnabled) {
                        Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f))

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Clear interval (minutes)",
                                modifier = Modifier.width(180.dp),
                                fontWeight = FontWeight.Medium
                            )
                            StyledTextField(
                                value = AppSettings.autoClearLogsIntervalMinutes.toString(),
                                onValueChange = {
                                    val minutes = it.toIntOrNull() ?: return@StyledTextField
                                    AppSettings.updateAutoClearLogsIntervalMinutes(minutes)
                                },
                                modifier = Modifier.width(100.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Delete log file on auto-clear", fontWeight = FontWeight.Medium)
                                Text(
                                    "Also delete the log file and rotated log files from disk when " +
                                            "auto-clearing, to free storage space.",
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Switch(
                                checked = AppSettings.deleteLogFileOnClear,
                                onCheckedChange = { AppSettings.updateDeleteLogFileOnClear(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
