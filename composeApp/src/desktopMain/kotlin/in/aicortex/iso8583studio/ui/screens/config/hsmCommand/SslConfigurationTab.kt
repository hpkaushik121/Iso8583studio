package `in`.aicortex.iso8583studio.ui.screens.config.hsmCommand

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.PrimaryBlue
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.CertVerificationMethod
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.HsmCommandConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.CertificateType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.CipherSuite
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.SSLTLSVersion

@Composable
fun SslConfigurationTab(
    config: HsmCommandConfig,
    onConfigChange: (HsmCommandConfig) -> Unit
) {
    val ssl = config.sslConfig
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Enable/Disable
        Text("SSL/TLS", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 1.dp,
            backgroundColor = MaterialTheme.colors.surface
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = ssl.enabled,
                        onCheckedChange = { onConfigChange(config.copy(sslConfig = ssl.copy(enabled = it))) },
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlue)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Enable SSL/TLS", fontWeight = FontWeight.Medium)
                        Text("Encrypt connection to HSM", style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
        }

        if (ssl.enabled) {
            // TLS Version & Certificate Type
            Text("Protocol Settings", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 1.dp,
                backgroundColor = MaterialTheme.colors.surface
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // TLS Version
                        Box(modifier = Modifier.weight(1f)) {
                            var tlsExpanded by remember { mutableStateOf(false) }
                            var tlsFieldWidth by remember { mutableStateOf(0.dp) }
                            val density = LocalDensity.current
                            FixedOutlinedTextField(
                                value = ssl.tlsVersion.displayName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("TLS/SSL Version") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { tlsFieldWidth = with(density) { it.size.width.toDp() } },
                                trailingIcon = {
                                    IconButton(onClick = { tlsExpanded = !tlsExpanded }) {
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { tlsExpanded = !tlsExpanded }
                            )
                            DropdownMenu(
                                expanded = tlsExpanded,
                                onDismissRequest = { tlsExpanded = false },
                                modifier = Modifier
                                    .then(if (tlsFieldWidth > 0.dp) Modifier.width(tlsFieldWidth) else Modifier.fillMaxWidth())
                                    .heightIn(max = 300.dp)
                            ) {
                                SSLTLSVersion.entries.forEach { v ->
                                    DropdownMenuItem(onClick = {
                                        onConfigChange(config.copy(sslConfig = ssl.copy(tlsVersion = v)))
                                        tlsExpanded = false
                                    }) { Text(v.displayName) }
                                }
                            }
                        }

                        // Certificate Type
                        Box(modifier = Modifier.weight(1f)) {
                            var certTypeExpanded by remember { mutableStateOf(false) }
                            var certFieldWidth by remember { mutableStateOf(0.dp) }
                            val certDensity = LocalDensity.current
                            FixedOutlinedTextField(
                                value = ssl.certificateType.displayName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Certificate Type") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { certFieldWidth = with(certDensity) { it.size.width.toDp() } },
                                trailingIcon = {
                                    IconButton(onClick = { certTypeExpanded = !certTypeExpanded }) {
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { certTypeExpanded = !certTypeExpanded }
                            )
                            DropdownMenu(
                                expanded = certTypeExpanded,
                                onDismissRequest = { certTypeExpanded = false },
                                modifier = Modifier
                                    .then(if (certFieldWidth > 0.dp) Modifier.width(certFieldWidth) else Modifier.fillMaxWidth())
                                    .heightIn(max = 300.dp)
                            ) {
                                CertificateType.entries.forEach { ct ->
                                    DropdownMenuItem(onClick = {
                                        onConfigChange(config.copy(sslConfig = ssl.copy(certificateType = ct)))
                                        certTypeExpanded = false
                                    }) { Text(ct.displayName) }
                                }
                            }
                        }
                    }
                }
            }

            // Certificate Verification
            Text("Certificate Verification", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 1.dp,
                backgroundColor = MaterialTheme.colors.surface
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    var verifyExpanded by remember { mutableStateOf(false) }
                    var verifyFieldWidth by remember { mutableStateOf(0.dp) }
                    val verifyDensity = LocalDensity.current
                    Box(modifier = Modifier.fillMaxWidth()) {
                        FixedOutlinedTextField(
                            value = ssl.certificateVerification.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Verification Method") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { verifyFieldWidth = with(verifyDensity) { it.size.width.toDp() } },
                            trailingIcon = {
                                IconButton(onClick = { verifyExpanded = !verifyExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                            }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { verifyExpanded = !verifyExpanded }
                        )
                        DropdownMenu(
                            expanded = verifyExpanded,
                            onDismissRequest = { verifyExpanded = false },
                            modifier = Modifier
                                .then(if (verifyFieldWidth > 0.dp) Modifier.width(verifyFieldWidth) else Modifier.fillMaxWidth())
                                .heightIn(max = 300.dp)
                        ) {
                            CertVerificationMethod.entries.forEach { m ->
                                DropdownMenuItem(onClick = {
                                    onConfigChange(config.copy(sslConfig = ssl.copy(certificateVerification = m)))
                                    verifyExpanded = false
                                }) { Text(m.displayName) }
                            }
                        }
                    }

                    if (ssl.certificateVerification == CertVerificationMethod.CUSTOM_CA) {
                        FixedOutlinedTextField(
                            value = ssl.caAuthorityPath,
                            onValueChange = { onConfigChange(config.copy(sslConfig = ssl.copy(caAuthorityPath = it))) },
                            label = { Text("CA Certificate Path") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Folder, null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }

            // Client Certificates
            Text("Client Certificates", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 1.dp,
                backgroundColor = MaterialTheme.colors.surface
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FixedOutlinedTextField(
                        value = ssl.clientPublicCertPath,
                        onValueChange = { onConfigChange(config.copy(sslConfig = ssl.copy(clientPublicCertPath = it))) },
                        label = { Text("Client Certificate / KeyStore Path") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.VpnKey, null, modifier = Modifier.size(18.dp)) }
                    )
                    FixedOutlinedTextField(
                        value = ssl.clientPrivateKeyPath,
                        onValueChange = { onConfigChange(config.copy(sslConfig = ssl.copy(clientPrivateKeyPath = it))) },
                        label = { Text("Client Private Key Path") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp)) }
                    )

                    var passwordVisible by remember { mutableStateOf(false) }
                    FixedOutlinedTextField(
                        value = ssl.keyStorePassword,
                        onValueChange = { onConfigChange(config.copy(sslConfig = ssl.copy(keyStorePassword = it))) },
                        label = { Text("KeyStore Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    "Toggle password"
                                )
                            }
                        }
                    )
                }
            }

            // Cipher Suites
            Text("Cipher Suites", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 1.dp,
                backgroundColor = MaterialTheme.colors.surface
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CipherSuite.entries.forEach { suite ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = suite in ssl.cipherSuites,
                                onCheckedChange = { checked ->
                                    val newSuites = if (checked) ssl.cipherSuites + suite else ssl.cipherSuites - suite
                                    onConfigChange(config.copy(sslConfig = ssl.copy(cipherSuites = newSuites)))
                                },
                                colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                            )
                            Column {
                                Text(suite.displayName, style = MaterialTheme.typography.body2)
                                Text("Strength: ${suite.strength}", style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}
