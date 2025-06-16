package `in`.aicortex.iso8583studio.ui.screens.config.hsmSimulator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.navigation.CryptoConfig
import `in`.aicortex.iso8583studio.ui.navigation.HSMSimulatorConfig
import `in`.aicortex.iso8583studio.ui.screens.components.StyledTextField



/**
 * Re-imagined Crypto Config Tab
 * Defines the raw cryptographic capabilities of the simulated HSM using a more interactive card and chip layout.
 */
@Composable
fun CryptoConfigTab(config: HSMSimulatorConfig, onConfigUpdate: (HSMSimulatorConfig) -> Unit) {
    val cryptoConfig = config.cryptoConfig

    // Mechanism definitions grouped by function
    val mechanismGroups = remember {
        mapOf(
            "Key Generation" to listOf("CKM_RSA_PKCS_KEY_PAIR_GEN", "CKM_ECDSA_KEY_PAIR_GEN", "CKM_AES_KEY_GEN", "CKM_DES3_KEY_GEN", "CKM_GENERIC_SECRET_KEY_GEN"),
            "RSA Mechanisms" to listOf("CKM_RSA_PKCS", "CKM_RSA_X_509", "CKM_SHA1_RSA_PKCS", "CKM_SHA256_RSA_PKCS", "CKM_SHA384_RSA_PKCS", "CKM_SHA512_RSA_PKCS", "CKM_RSA_PKCS_OAEP"),
            "ECDSA Mechanisms" to listOf("CKM_ECDSA", "CKM_ECDSA_SHA1", "CKM_ECDSA_SHA256", "CKM_ECDSA_SHA384", "CKM_ECDSA_SHA512"),
            "AES Mechanisms" to listOf("CKM_AES_ECB", "CKM_AES_CBC", "CKM_AES_CBC_PAD", "CKM_AES_GCM", "CKM_AES_KEY_WRAP", "CKM_AES_KEY_WRAP_PAD"),
            "Hashing Mechanisms" to listOf("CKM_SHA_1", "CKM_SHA256", "CKM_SHA384", "CKM_SHA512"),
            "MACing Mechanisms" to listOf("CKM_SHA_1_HMAC", "CKM_SHA256_HMAC", "CKM_SHA384_HMAC", "CKM_SHA512_HMAC")
        )
    }

    // Changed LazyColumn to Column to work inside a parent scrollable container
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        // --- Algorithm Configuration ---
        Text("Algorithm Capabilities", style = MaterialTheme.typography.h6)
        Text(
            "Enable or disable cryptographic algorithm families and configure their properties.",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )

        AlgorithmFamilyCard(
            title = "Asymmetric Algorithms",
            icon = Icons.Default.VpnKey,
            isGroupEnabled = cryptoConfig.supportedAlgorithms.contains("RSA") || cryptoConfig.supportedAlgorithms.contains("ECC"),
            onGroupToggle = { isEnabled ->
                val updatedAlgos = cryptoConfig.supportedAlgorithms.toMutableSet()
                if (isEnabled) {
                    updatedAlgos.add("RSA"); updatedAlgos.add("ECC")
                } else {
                    updatedAlgos.remove("RSA"); updatedAlgos.remove("ECC")
                }
                onConfigUpdate(config.copy(cryptoConfig = cryptoConfig.copy(supportedAlgorithms = updatedAlgos)))
            }
        ) {
            AlgorithmToggle(
                name = "RSA",
                isEnabled = cryptoConfig.supportedAlgorithms.contains("RSA"),
                onToggle = { onConfigUpdate(config.copy(cryptoConfig = toggleAlgorithm(cryptoConfig, "RSA", it))) }
            ) {
                KeyPropertySelector(
                    title = "Allowed Key Sizes (bits)",
                    options = listOf(1024, 2048, 3072, 4096),
                    selected = cryptoConfig.rsaKeySizes,
                    onSelectionChange = { size, selected ->
                        val updatedSizes = cryptoConfig.rsaKeySizes.toMutableSet().apply { if (selected) add(size) else remove(size) }
                        onConfigUpdate(config.copy(cryptoConfig = cryptoConfig.copy(rsaKeySizes = updatedSizes)))
                    }
                )
            }
            Divider()
            AlgorithmToggle(
                name = "ECC",
                isEnabled = cryptoConfig.supportedAlgorithms.contains("ECC"),
                onToggle = { onConfigUpdate(config.copy(cryptoConfig = toggleAlgorithm(cryptoConfig, "ECC", it))) }
            ) {
                KeyPropertySelector(
                    title = "Allowed Curves",
                    options = listOf("secp256r1", "secp384r1", "secp521r1"),
                    selected = cryptoConfig.eccCurves,
                    isChips = true,
                    onSelectionChange = { curve, selected ->
                        val updatedCurves = cryptoConfig.eccCurves.toMutableSet().apply { if (selected) add(curve) else remove(curve) }
                        onConfigUpdate(config.copy(cryptoConfig = cryptoConfig.copy(eccCurves = updatedCurves)))
                    }
                )
            }
        }

        AlgorithmFamilyCard(
            title = "Symmetric Algorithms",
            icon = Icons.Default.EnhancedEncryption,
            isGroupEnabled = cryptoConfig.supportedAlgorithms.contains("AES") || cryptoConfig.supportedAlgorithms.contains("3DES"),
            onGroupToggle = { isEnabled ->
                val updatedAlgos = cryptoConfig.supportedAlgorithms.toMutableSet()
                if (isEnabled) {
                    updatedAlgos.add("AES"); updatedAlgos.add("3DES")
                } else {
                    updatedAlgos.remove("AES"); updatedAlgos.remove("3DES")
                }
                onConfigUpdate(config.copy(cryptoConfig = cryptoConfig.copy(supportedAlgorithms = updatedAlgos)))
            }
        ) {
            AlgorithmToggle(
                name = "AES",
                isEnabled = cryptoConfig.supportedAlgorithms.contains("AES"),
                onToggle = { onConfigUpdate(config.copy(cryptoConfig = toggleAlgorithm(cryptoConfig, "AES", it))) }
            ) {
                KeyPropertySelector(
                    title = "Allowed Key Sizes (bits)",
                    options = listOf(128, 192, 256),
                    selected = cryptoConfig.aesKeySizes,
                    onSelectionChange = { size, selected ->
                        val updatedSizes = cryptoConfig.aesKeySizes.toMutableSet().apply { if (selected) add(size) else remove(size) }
                        onConfigUpdate(config.copy(cryptoConfig = cryptoConfig.copy(aesKeySizes = updatedSizes)))
                    }
                )
            }
            Divider()
            AlgorithmToggle(
                name = "3DES",
                isEnabled = cryptoConfig.supportedAlgorithms.contains("3DES"),
                onToggle = { onConfigUpdate(config.copy(cryptoConfig = toggleAlgorithm(cryptoConfig, "3DES", it))) }
            )
        }

        AlgorithmFamilyCard(
            title = "Hashing Algorithms",
            icon = Icons.Default.Fingerprint,
            isGroupEnabled = cryptoConfig.supportedAlgorithms.any { it.startsWith("SHA") },
            onGroupToggle = { isEnabled ->
                val updatedAlgos = cryptoConfig.supportedAlgorithms.toMutableSet()
                val shaAlgos = listOf("SHA-1", "SHA-256", "SHA-384", "SHA-512")
                if (isEnabled) updatedAlgos.addAll(shaAlgos) else updatedAlgos.removeAll(shaAlgos)
                onConfigUpdate(config.copy(cryptoConfig = cryptoConfig.copy(supportedAlgorithms = updatedAlgos)))
            }
        ) {
            val shaAlgos = listOf("SHA-1", "SHA-256", "SHA-384", "SHA-512")
            KeyPropertySelector(
                title = "Enabled Hash Functions",
                options = shaAlgos,
                selected = cryptoConfig.supportedAlgorithms.intersect(shaAlgos),
                isChips = true,
                onSelectionChange = { algo, selected ->
                    onConfigUpdate(config.copy(cryptoConfig = toggleAlgorithm(cryptoConfig, algo, selected)))
                }
            )
        }

        // --- Mechanism Configuration ---
        Spacer(Modifier.height(16.dp))
        Text("PKCS#11 Mechanism Support", style = MaterialTheme.typography.h6)
        Text(
            "Enable or disable specific cryptographic mechanisms (`CKM_*`) that the simulator will support.",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )

        mechanismGroups.forEach { (groupName, mechanisms) ->
            Text(groupName, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                mechanisms.forEach { mechanism ->
                    MechanismChip(
                        text = mechanism,
                        isSelected = cryptoConfig.supportedMechanisms.contains(mechanism),
                        onClick = {
                            val updatedMechanisms = cryptoConfig.supportedMechanisms.toMutableSet()
                            if (updatedMechanisms.contains(mechanism)) {
                                updatedMechanisms.remove(mechanism)
                            } else {
                                updatedMechanisms.add(mechanism)
                            }
                            onConfigUpdate(config.copy(cryptoConfig = cryptoConfig.copy(supportedMechanisms = updatedMechanisms)))
                        }
                    )
                }
            }
        }
    }
}


private fun toggleAlgorithm(config: CryptoConfig, name: String, isEnabled: Boolean): CryptoConfig {
    val updatedAlgos = config.supportedAlgorithms.toMutableSet()
    if (isEnabled) updatedAlgos.add(name) else updatedAlgos.remove(name)
    return config.copy(supportedAlgorithms = updatedAlgos)
}

// --- Re-imagined Helper & Reusable Components ---

@Composable
private fun AlgorithmFamilyCard(
    title: String,
    icon: ImageVector,
    isGroupEnabled: Boolean,
    onGroupToggle: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(shape = RoundedCornerShape(12.dp), elevation = 2.dp) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = title, modifier = Modifier.size(28.dp), tint = MaterialTheme.colors.primary)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
                }
                Switch(checked = isGroupEnabled, onCheckedChange = onGroupToggle)
            }
            AnimatedVisibility(visible = isGroupEnabled) {
                Column {
                    Divider()
                    Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        content()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun MechanismChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.15f) else MaterialTheme.colors.onSurface.copy(alpha = 0.08f)
    val contentColor = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
    val border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colors.primary) else BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.2f))

    Chip(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        border = border,
        colors = ChipDefaults.chipColors(backgroundColor = backgroundColor, contentColor = contentColor)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(text, style = MaterialTheme.typography.caption, fontWeight = if(isSelected) FontWeight.Medium else FontWeight.Normal)
        }
    }
}

@Composable
private fun AlgorithmToggle(
    name: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(name, style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Checkbox(checked = isEnabled, onCheckedChange = onToggle)
        }
        AnimatedVisibility(visible = isEnabled && content != null) {
            Column(Modifier.padding(start = 16.dp, top = 8.dp)) {
                content?.let { it() }
            }
        }
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun <T> KeyPropertySelector(
    title: String,
    options: List<T>,
    selected: Set<T>,
    isChips: Boolean = false,
    onSelectionChange: (T, Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
        if (isChips) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { option ->
                    FilterChip(
                        selected = selected.contains(option),
                        onClick = { onSelectionChange(option, !selected.contains(option)) },
                        leadingIcon = if (selected.contains(option)) { { Icon(Icons.Default.Done, null, Modifier.size(ChipDefaults.LeadingIconSize)) } } else null
                    ) {
                        Text(option.toString())
                    }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                options.forEach { option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = selected.contains(option),
                            onCheckedChange = { onSelectionChange(option, it) }
                        )
                        Text(option.toString(), style = MaterialTheme.typography.body2)
                    }
                }
            }
        }
    }
}
