package `in`.aicortex.iso8583studio.ui.screens.config

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.components.*

/**
 * Advanced Options Tab - Fifth tab in the Security Gateway configuration
 * Contains advanced configuration settings presented as a property grid
 */
@Composable
fun AdvancedOptionsTab() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header with description
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Advanced Configuration",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "This section contains advanced settings for the gateway. Modify these settings with caution.",
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }

        // Advanced Options Panel
        Panel(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SectionHeader(title = "Advanced Properties")

                // Property grid with improved styling
                PropertyGrid(
                    properties = listOf(
                        "SpecialFeature" to "None",
                        "EnableHSM" to "False",
                        "HSMPassword" to "********",
                        "LLRange" to "80-99",
                        "MaxBinaryLength" to "999",
                        "CommandTimeout" to "15s",
                        "AcquiringId" to "",
                        "UsageControl" to "False",
                        "CustomField" to ""
                    )
                )
            }
        }

        // HSM Configuration Panel (example of additional settings section)
        Panel(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SectionHeader(
                    title = "HSM Configuration",
                    actionContent = {
                        Switch(
                            checked = false, // This would be a state in real implementation
                            onCheckedChange = { /* Toggle HSM */ },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colors.primary
                            )
                        )
                    }
                )

                Text(
                    "Hardware Security Module configuration is disabled. Enable the toggle to configure HSM settings.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        // Performance Tuning Panel
        Panel(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SectionHeader(title = "Performance Tuning")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Thread Pool Size",
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Controls the number of worker threads for handling concurrent requests",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Slider(
                        value = 0.5f, // This would be a state in real implementation
                        onValueChange = { /* Update thread pool size */ },
                        modifier = Modifier.width(200.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colors.primary,
                            activeTrackColor = MaterialTheme.colors.primary
                        )
                    )

                    Text(
                        "8", // This would be calculated from the slider value
                        modifier = Modifier.width(40.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Connection Timeout",
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Maximum time to wait for connection establishment (seconds)",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    StyledTextField(
                        value = "30", // This would be a state in real implementation
                        onValueChange = { /* Update timeout */ },
                        modifier = Modifier.width(80.dp)
                    )
                }
            }
        }

        // Action buttons at the bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SecondaryButton(
                text = "Reset to Defaults",
                onClick = { /* Reset settings */ },
                icon = Icons.Default.RestartAlt,
                modifier = Modifier.weight(1f)
            )

            PrimaryButton(
                text = "Apply Changes",
                onClick = { /* Apply settings */ },
                icon = Icons.Default.Check,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PropertyGrid(
    properties: List<Pair<String, String>>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = ButtonDefaults.outlinedBorder,
        color = MaterialTheme.colors.surface.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            properties.forEachIndexed { index, (name, value) ->
                PropertyRow(
                    name = name,
                    value = value,
                    isEven = index % 2 == 0
                )

                if (index < properties.size - 1) {
                    Divider(
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PropertyRow(
    name: String,
    value: String,
    isEven: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isEven) Color.Transparent
                else MaterialTheme.colors.onSurface.copy(alpha = 0.05f)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.body2
        )

        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.body2,
            color = if (value.isEmpty()) MaterialTheme.colors.onSurface.copy(alpha = 0.5f) else MaterialTheme.colors.onSurface
        )
    }
}