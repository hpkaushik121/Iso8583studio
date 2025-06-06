package `in`.aicortex.iso8583studio.ui.screens.config

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Router
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.data.model.GatewayType
import `in`.aicortex.iso8583studio.data.model.TransmissionType
import `in`.aicortex.iso8583studio.domain.utils.Utils
import `in`.aicortex.iso8583studio.ui.screens.components.DevelopmentStatus
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.components.PrimaryButton
import `in`.aicortex.iso8583studio.ui.screens.components.SecondaryButton
import `in`.aicortex.iso8583studio.ui.screens.components.SectionHeader
import `in`.aicortex.iso8583studio.ui.screens.components.StyledTextField
import `in`.aicortex.iso8583studio.ui.screens.components.UnderDevelopmentBanner
import `in`.aicortex.iso8583studio.ui.screens.components.UnderDevelopmentChip
import org.jetbrains.exposed.v1.core.Column
import org.springframework.security.crypto.codec.Hex

/**
 * Gateway Type tab - First tab in the Security Gateway configuration
 * Contains the basic gateway configuration settings
 */
@Composable
fun GatewayTypeTab(config: GatewayConfig, onConfigChange: (GatewayConfig) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Gateway Type selection
        Panel(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SectionHeader(title = "GATEWAY TYPE")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GatewayTypeOption(
                        selected = config.gatewayType == GatewayType.SERVER,
                        onSelect = {
                            onConfigChange(
                                config.copy(
                                    gatewayType = GatewayType.SERVER,
                                    transmissionType = TransmissionType.SYNCHRONOUS
                                )
                            )
                        },
                        title = "Server",
                        icon = Icons.Default.Computer,
                        modifier = Modifier.weight(1f)
                    )

                    GatewayTypeOption(
                        selected = config.gatewayType == GatewayType.CLIENT,
                        onSelect = {
                            onConfigChange(
                                config.copy(
                                    gatewayType = GatewayType.CLIENT,
                                    transmissionType = TransmissionType.SYNCHRONOUS
                                )
                            )
                        },
                        title = "Client",
                        icon = Icons.Default.SettingsInputComponent,
                        modifier = Modifier.weight(1f),
                        isUnderDevelopment = true
                    )

                    GatewayTypeOption(
                        selected = config.gatewayType == GatewayType.PROXY,
                        onSelect = { onConfigChange(config.copy(gatewayType = GatewayType.PROXY)) },
                        title = "Proxy",
                        icon = Icons.Default.Router,
                        modifier = Modifier.weight(1f),
                        isUnderDevelopment = true
                    )
                }
            }
        }

        // Creating Info section
        Panel(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SectionHeader(title = "Configuration Details")

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Name field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Name",
                            modifier = Modifier.width(120.dp),
                            fontWeight = FontWeight.Medium
                        )
                        StyledTextField(
                            value = config.name,
                            onValueChange = {
                                onConfigChange(config.copy(name = it))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Modified date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Modified date",
                            modifier = Modifier.width(120.dp),
                            fontWeight = FontWeight.Medium
                        )
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(4.dp),
                            border = ButtonDefaults.outlinedBorder,
                            color = MaterialTheme.colors.surface
                        ) {
                            Text(
                                Utils.formatDate(config.createDate),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    // Description
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            "Description",
                            modifier = Modifier.width(120.dp),
                            fontWeight = FontWeight.Medium
                        )
                        StyledTextField(
                            value = config.description,
                            onValueChange = {
                                onConfigChange(config.copy(description = it))
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = false,
                            maxLines = 3
                        )
                    }

                    // Enable checkbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = config.enable,
                            onCheckedChange = {
                                onConfigChange(config.copy(enable = it))
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colors.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enable when Windows starts")
                    }
                }
            }
        }

        // Server or Client settings based on gateway type
        if (config.gatewayType == GatewayType.SERVER) {
            Panel(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SectionHeader(title = "Server Security Settings")

                    Text(
                        "This section is configured in web based user interface",
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        } else if (config.gatewayType == GatewayType.CLIENT) {
            Panel(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SectionHeader(title = "Client Authentication")

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Client ID
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Client ID",
                                modifier = Modifier.width(120.dp),
                                fontWeight = FontWeight.Medium
                            )
                            StyledTextField(
                                value = config.clientID,
                                onValueChange = {
                                    onConfigChange(config.copy(clientID = it))
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Location ID
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Location ID",
                                modifier = Modifier.width(120.dp),
                                fontWeight = FontWeight.Medium
                            )
                            StyledTextField(
                                value = config.locationID,
                                onValueChange = {
                                    onConfigChange(config.copy(locationID = it))
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Password
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Password",
                                modifier = Modifier.width(120.dp),
                                fontWeight = FontWeight.Medium
                            )

                            OutlinedTextField(
                                value = config.password?.let { String(Hex.encode(it)) } ?: "",
                                onValueChange = { newValue ->
                                    try {
                                        // Only update if valid hex
                                        val newPasswordBytes =
                                            if (newValue.isEmpty()) null else Hex.decode(newValue)
                                        onConfigChange(config.copy(password = newPasswordBytes))
                                    } catch (e: Exception) {
                                        // Handle invalid hex input if needed
                                    }
                                },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = MaterialTheme.colors.primary,
                                    cursorColor = MaterialTheme.colors.primary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Special test buttons
        Panel(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SectionHeader(title = "Testing Tools")
                UnderDevelopmentChip()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SecondaryButton(
                        text = "Test PosGateway",
                        onClick = { /* Test POS Gateway logic */ },
                        modifier = Modifier.weight(1f)
                    )

                    SecondaryButton(
                        text = "Test EVN Component",
                        onClick = { /* Test EVN Component logic */ },
                        modifier = Modifier.weight(1f)
                    )

                    PrimaryButton(
                        text = "Configure Switch",
                        onClick = { /* Configure Switch logic */ },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun GatewayTypeOption(
    selected: Boolean,
    onSelect: () -> Unit,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    isUnderDevelopment: Boolean = false
) {
    Surface(
        modifier = modifier
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = Role.RadioButton
            ),
        shape = RoundedCornerShape(8.dp),
        elevation = if (selected) 2.dp else 0.dp,
        border = ButtonDefaults.outlinedBorder,
        color = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.surface
    ) {
        if(isUnderDevelopment){
            Column(
                modifier = Modifier.padding(15.dp)
            ) {
                UnderDevelopmentChip(status = DevelopmentStatus.UNDER_DEVELOPMENT,
                    modifier = Modifier.width(30.dp))
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) Color.White else MaterialTheme.colors.primary,
                modifier = Modifier.size(32.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Medium,
                color = if (selected) Color.White else MaterialTheme.colors.onSurface
            )
        }
    }
}