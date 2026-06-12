package `in`.aicortex.iso8583studio.ui.screens.config.apduSimulator.v2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.terminal.AidTerminalConfig
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.terminal.Capk
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.terminal.IssuerHostConfig
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.terminal.TerminalProfile
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.terminal.TestCapks
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.apdu.APDUSimulatorConfig
import `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField

/**
 * Editor for the terminal-side EMV profile (TerminalProfile) attached to an APDU simulator
 * configuration. Surfaces every field of the data model: terminal identity tags, per-AID
 * Terminal Action Codes / floor limits / kernel selection, CA Public Keys, and online-issuer
 * connection settings.
 */
@Composable
fun TerminalProfileTab(config: APDUSimulatorConfig, onConfigUpdate: (APDUSimulatorConfig) -> Unit) {
    val profile = config.terminalProfile
    val updateProfile: (TerminalProfile) -> Unit = { onConfigUpdate(config.copy(terminalProfile = it)) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IdentitySection(profile, updateProfile)
        PerAidSection(profile, updateProfile)
        CapkSection(profile, updateProfile)
        IssuerHostSection(profile.issuerHost) { updateProfile(profile.copy(issuerHost = it)) }
    }
}

// -------- 1. Identity --------

@Composable
private fun IdentitySection(profile: TerminalProfile, onChange: (TerminalProfile) -> Unit) {
    SectionCard(
        icon = Icons.Default.Badge,
        title = "Terminal identity",
        subtitle = "EMV terminal-level tags emitted in PDOL/CDOL responses.",
    ) {
        TextRow(label = "Name", value = profile.name) { onChange(profile.copy(name = it)) }
        TextRow(
            label = "Terminal type (9F35, 0..255)",
            value = profile.terminalType.toString(),
            isError = profile.terminalType !in 0..255,
        ) { v -> v.toIntOrNull()?.let { onChange(profile.copy(terminalType = it)) } }
        TextRow(
            label = "Terminal capabilities (9F33, 3 bytes hex)",
            value = profile.terminalCapabilities,
            isError = profile.terminalCapabilities.length != 6 || !isHex(profile.terminalCapabilities),
        ) { onChange(profile.copy(terminalCapabilities = it.uppercase())) }
        TextRow(
            label = "Additional capabilities (9F40, 5 bytes hex)",
            value = profile.additionalCapabilities,
            isError = profile.additionalCapabilities.length != 10 || !isHex(profile.additionalCapabilities),
        ) { onChange(profile.copy(additionalCapabilities = it.uppercase())) }
        TextRow(
            label = "Terminal country code (9F1A, 2 bytes hex)",
            value = profile.terminalCountryCode,
            isError = profile.terminalCountryCode.length != 4 || !isHex(profile.terminalCountryCode),
        ) { onChange(profile.copy(terminalCountryCode = it.uppercase())) }
        TextRow(
            label = "Transaction currency code (5F2A, 2 bytes hex)",
            value = profile.transactionCurrencyCode,
            isError = profile.transactionCurrencyCode.length != 4 || !isHex(profile.transactionCurrencyCode),
        ) { onChange(profile.copy(transactionCurrencyCode = it.uppercase())) }
        TextRow(
            label = "Currency exponent (5F36, 0..3)",
            value = profile.transactionCurrencyExp.toString(),
            isError = profile.transactionCurrencyExp !in 0..3,
        ) { v -> v.toIntOrNull()?.let { onChange(profile.copy(transactionCurrencyExp = it)) } }
        TextRow(label = "IFD serial number (9F1E)", value = profile.ifdSerialNumber) {
            onChange(profile.copy(ifdSerialNumber = it))
        }
        TextRow(label = "Merchant category code (9F15)", value = profile.merchantCategoryCode) {
            onChange(profile.copy(merchantCategoryCode = it))
        }
        TextRow(label = "Merchant ID (9F16)", value = profile.merchantId) {
            onChange(profile.copy(merchantId = it))
        }
        TextRow(label = "Terminal identification (9F1C)", value = profile.terminalIdentification) {
            onChange(profile.copy(terminalIdentification = it))
        }
    }
}

// -------- 2. Per-AID --------

@Composable
private fun PerAidSection(profile: TerminalProfile, onChange: (TerminalProfile) -> Unit) {
    SectionCard(
        icon = Icons.Default.PointOfSale,
        title = "Per-AID configuration",
        subtitle = "Terminal Action Codes, floor limits, CVM and kernel choice for each accepted AID.",
    ) {
        profile.perAid.forEachIndexed { idx, row ->
            AidRowEditor(
                row = row,
                onUpdate = { updated ->
                    onChange(profile.copy(perAid = profile.perAid.toMutableList().also { it[idx] = updated }))
                },
                onRemove = {
                    onChange(profile.copy(perAid = profile.perAid.toMutableList().also { it.removeAt(idx) }))
                },
            )
        }
        OutlinedButton(onClick = {
            val next = profile.perAid + AidTerminalConfig(aid = "A0000000000000")
            onChange(profile.copy(perAid = next))
        }) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Add AID")
        }
    }
}

@Composable
private fun AidRowEditor(row: AidTerminalConfig, onUpdate: (AidTerminalConfig) -> Unit, onRemove: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth(), elevation = 1.dp, shape = RoundedCornerShape(6.dp)) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "toggle",
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        if (row.aid.isEmpty()) "(no AID)" else row.aid,
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                    )
                    if (row.label.isNotEmpty()) {
                        Text(row.label, style = MaterialTheme.typography.caption)
                    }
                }
                Checkbox(checked = row.enabled, onCheckedChange = { onUpdate(row.copy(enabled = it)) })
                Text("Enabled", style = MaterialTheme.typography.caption)
                IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, contentDescription = "remove") }
            }
            if (expanded) {
                TextRow(label = "AID", value = row.aid) { onUpdate(row.copy(aid = it.uppercase())) }
                TextRow(label = "Label", value = row.label) { onUpdate(row.copy(label = it)) }
                TextRow(
                    label = "TAC Default (5 bytes hex)",
                    value = row.tacDefault,
                    isError = row.tacDefault.length != 10 || !isHex(row.tacDefault),
                ) { onUpdate(row.copy(tacDefault = it.uppercase())) }
                TextRow(
                    label = "TAC Denial (5 bytes hex)",
                    value = row.tacDenial,
                    isError = row.tacDenial.length != 10 || !isHex(row.tacDenial),
                ) { onUpdate(row.copy(tacDenial = it.uppercase())) }
                TextRow(
                    label = "TAC Online (5 bytes hex)",
                    value = row.tacOnline,
                    isError = row.tacOnline.length != 10 || !isHex(row.tacOnline),
                ) { onUpdate(row.copy(tacOnline = it.uppercase())) }
                TextRow(label = "Floor limit (minor units)", value = row.floorLimit.toString()) { v ->
                    v.toLongOrNull()?.let { onUpdate(row.copy(floorLimit = it)) }
                }
                TextRow(
                    label = "Target percent (0..99)",
                    value = row.targetPercent.toString(),
                    isError = row.targetPercent !in 0..99,
                ) { v -> v.toIntOrNull()?.let { onUpdate(row.copy(targetPercent = it)) } }
                TextRow(label = "Threshold (minor units)", value = row.threshold.toString()) { v ->
                    v.toLongOrNull()?.let { onUpdate(row.copy(threshold = it)) }
                }
                TextRow(
                    label = "Max target percent (0..99)",
                    value = row.maxTargetPercent.toString(),
                    isError = row.maxTargetPercent !in 0..99,
                ) { v -> v.toIntOrNull()?.let { onUpdate(row.copy(maxTargetPercent = it)) } }

                Text("CVM capabilities", style = MaterialTheme.typography.caption, fontWeight = FontWeight.SemiBold)
                CheckboxRow("Online PIN", row.cvmOnlinePin) { onUpdate(row.copy(cvmOnlinePin = it)) }
                CheckboxRow("Offline PIN", row.cvmOfflinePin) { onUpdate(row.copy(cvmOfflinePin = it)) }
                CheckboxRow("Signature", row.cvmSignature) { onUpdate(row.copy(cvmSignature = it)) }
                CheckboxRow("No CVM", row.cvmNoCvm) { onUpdate(row.copy(cvmNoCvm = it)) }

                KernelDropdown(row.kernelId) { onUpdate(row.copy(kernelId = it)) }
            }
        }
    }
}

private val KERNELS = listOf(
    0 to "Contact (Book 3)",
    2 to "MC kernel-2",
    3 to "Visa kernel-3",
    4 to "Amex kernel-4",
    5 to "JCB kernel-5",
    6 to "Discover kernel-6",
    7 to "UnionPay kernel-7",
)

@Composable
private fun KernelDropdown(selected: Int, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val label = KERNELS.firstOrNull { it.first == selected }?.let { "${it.first} — ${it.second}" } ?: selected.toString()
    Box {
        OutlinedButton(onClick = { open = true }) {
            Text("Kernel: $label")
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            KERNELS.forEach { (id, name) ->
                DropdownMenuItem(onClick = {
                    onSelect(id)
                    open = false
                }) { Text("$id — $name") }
            }
        }
    }
}

// -------- 3. CAPK --------

@Composable
private fun CapkSection(profile: TerminalProfile, onChange: (TerminalProfile) -> Unit) {
    SectionCard(
        icon = Icons.Default.Key,
        title = "CA Public Keys (CAPK)",
        subtitle = "Scheme CA keys used to verify issuer-public-key certificates.",
    ) {
        profile.capks.forEachIndexed { idx, capk ->
            CapkRowEditor(
                capk = capk,
                onUpdate = { updated ->
                    onChange(profile.copy(capks = profile.capks.toMutableList().also { it[idx] = updated }))
                },
                onRemove = {
                    onChange(profile.copy(capks = profile.capks.toMutableList().also { it.removeAt(idx) }))
                },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                val next = profile.capks + Capk(rid = "A000000000", index = 0, modulus = "")
                onChange(profile.copy(capks = next))
            }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Add CAPK")
            }
            OutlinedButton(onClick = { onChange(profile.copy(capks = TestCapks.all())) }) {
                Text("Import test set")
            }
        }
    }
}

@Composable
private fun CapkRowEditor(capk: Capk, onUpdate: (Capk) -> Unit, onRemove: () -> Unit) {
    var modulusDialogOpen by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth(), elevation = 1.dp, shape = RoundedCornerShape(6.dp)) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "RID ${capk.rid} index ${capk.index}",
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, contentDescription = "remove") }
            }
            TextRow(
                label = "RID (5 bytes hex)",
                value = capk.rid,
                isError = capk.rid.length != 10 || !isHex(capk.rid),
            ) { onUpdate(capk.copy(rid = it.uppercase())) }
            TextRow(
                label = "Index (0..255)",
                value = capk.index.toString(),
                isError = capk.index !in 0..255,
            ) { v -> v.toIntOrNull()?.let { onUpdate(capk.copy(index = it)) } }
            TextRow(label = "Exponent (hex)", value = capk.exponent) { onUpdate(capk.copy(exponent = it.uppercase())) }
            TextRow(label = "Expiry (YYMMDD)", value = capk.expiryYyMmDd) {
                onUpdate(capk.copy(expiryYyMmDd = it))
            }
            TextRow(label = "Source", value = capk.source) { onUpdate(capk.copy(source = it)) }

            Text(
                "Modulus: ${truncate(capk.modulus)}",
                style = MaterialTheme.typography.caption,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                "Checksum: ${truncate(capk.checksum)}",
                style = MaterialTheme.typography.caption,
                fontFamily = FontFamily.Monospace,
            )
            OutlinedButton(onClick = { modulusDialogOpen = true }) { Text("Edit modulus") }
        }
    }

    if (modulusDialogOpen) {
        ModulusDialog(
            initial = capk.modulus,
            onDismiss = { modulusDialogOpen = false },
            onConfirm = {
                onUpdate(capk.copy(modulus = it.uppercase()))
                modulusDialogOpen = false
            },
        )
    }
}

@Composable
private fun ModulusDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var draft by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit RSA modulus") },
        text = {
            FixedOutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.body2.copy(fontFamily = FontFamily.Monospace),
                singleLine = false,
                minLines = 6,
                maxLines = 12,
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(draft) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// -------- 4. Issuer host --------

@Composable
private fun IssuerHostSection(host: IssuerHostConfig, onChange: (IssuerHostConfig) -> Unit) {
    SectionCard(
        icon = Icons.Default.Cloud,
        title = "Online issuer host",
        subtitle = "Where ARQC requests are sent for online authorization.",
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = host.enabled, onCheckedChange = { onChange(host.copy(enabled = it)) })
            Spacer(Modifier.width(8.dp))
            Text("Enabled", style = MaterialTheme.typography.body2)
        }
        TextRow(label = "Host", value = host.host) { onChange(host.copy(host = it)) }
        TextRow(label = "Port", value = host.port.toString()) { v ->
            v.toIntOrNull()?.let { onChange(host.copy(port = it)) }
        }
        TextRow(label = "Connect timeout (ms)", value = host.connectTimeoutMs.toString()) { v ->
            v.toIntOrNull()?.let { onChange(host.copy(connectTimeoutMs = it)) }
        }
        TextRow(label = "Response timeout (ms)", value = host.responseTimeoutMs.toString()) { v ->
            v.toIntOrNull()?.let { onChange(host.copy(responseTimeoutMs = it)) }
        }
    }
}

// -------- helpers --------

@Composable
private fun TextRow(label: String, value: String, isError: Boolean = false, onValueChange: (String) -> Unit) {
    FixedOutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = isError,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CheckboxRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, style = MaterialTheme.typography.body2)
    }
}

private fun isHex(s: String): Boolean = s.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }

private fun truncate(s: String, max: Int = 32): String =
    if (s.length <= max) s.ifEmpty { "(empty)" } else s.substring(0, max) + "..."
