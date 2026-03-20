package `in`.aicortex.iso8583studio.license

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.io.FilenameFilter

private val WarningOrange = Color(0xFFFF9800)
private val ErrorRed = Color(0xFFE53935)
private val SuccessGreen = Color(0xFF43A047)
private val InfoBlue = Color(0xFF1E88E5)
private val SubtleGray = Color(0xFF9E9E9E)

/**
 * Full-screen activation screen shown when the license is expired / activation required.
 * Provides CSR generation, cert import, and exit options.
 */
@Composable
fun LicenseActivationScreen(
    licenseSnapshot: LicenseSnapshot,
    onActivated: () -> Unit,
    onExit: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    var csrPem by remember { mutableStateOf("") }
    var certInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var isValidating by remember { mutableStateOf(false) }
    var validationResult by remember { mutableStateOf<LicenseSnapshot?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colors.primary
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "ISO8583Studio — License Required",
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = when (licenseSnapshot.state) {
                LicenseState.ACTIVATION_REQUIRED -> "Your trial period has ended. Please activate to continue."
                LicenseState.EXPIRED -> "Your license has expired. Please renew to continue."
                LicenseState.REVOKED -> "Your license has been revoked. Contact support."
                LicenseState.OFFLINE_GRACE_EXCEEDED -> "Offline grace period exceeded. Connect to the internet."
                else -> licenseSnapshot.message.ifBlank { "A valid license is required to use this application." }
            },
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )

        Spacer(Modifier.height(24.dp))

        when (currentStep) {
            0 -> ActivationChoiceStep(
                onGenerateCsr = {
                    scope.launch {
                        val result = CsrGenerator.generate()
                        LicenseStorage.savePrivateKeyPem(result.privateKeyPem)
                        csrPem = result.csrPem
                        currentStep = 1
                    }
                },
                onImportCert = { currentStep = 2 }
            )
            1 -> CsrDisplayStep(
                csrPem = csrPem,
                onNext = { currentStep = 2 },
                onBack = { currentStep = 0 }
            )
            2 -> CertImportStep(
                certInput = certInput,
                onCertInputChange = { certInput = it },
                statusMessage = statusMessage,
                isError = isError,
                isValidating = isValidating,
                validationResult = validationResult,
                onImport = {
                    scope.launch {
                        isValidating = true
                        statusMessage = ""
                        validationResult = null
                        try {
                            LicenseService.parseCertificate(certInput)
                            LicenseStorage.saveCertificatePem(certInput)
                            statusMessage = "Certificate saved. Validating with server..."
                            isError = false

                            val snapshot = LicenseService.validate()
                            validationResult = snapshot
                            isValidating = false

                            if (snapshot.isUsable()) {
                                statusMessage = "License activated successfully!"
                                isError = false
                                onActivated()
                            } else {
                                statusMessage = snapshot.message.ifBlank { "Validation failed: ${snapshot.state}" }
                                isError = true
                            }
                        } catch (e: Exception) {
                            isValidating = false
                            statusMessage = "Invalid certificate: ${e.message}"
                            isError = true
                        }
                    }
                },
                onBack = { currentStep = 0 }
            )
        }

        Spacer(Modifier.height(24.dp))

        TextButton(onClick = onExit) {
            Text("Exit Application")
        }
    }
}

@Composable
private fun ActivationChoiceStep(
    onGenerateCsr: () -> Unit,
    onImportCert: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Choose an activation method:",
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onGenerateCsr,
            modifier = Modifier.width(350.dp)
        ) {
            Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Step 1: Generate License Request (CSR)")
        }

        OutlinedButton(
            onClick = onImportCert,
            modifier = Modifier.width(350.dp)
        ) {
            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("I already have a certificate — Import")
        }
    }
}

@Composable
private fun CsrDisplayStep(
    csrPem: String,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var copied by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(max = 600.dp)
    ) {
        Text(
            text = "Your license request has been generated.",
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Save the CSR file and email it to license@aicortex.in",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )

        Spacer(Modifier.height(12.dp))

        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.05f),
            elevation = 0.dp
        ) {
            SelectionContainer {
                Text(
                    text = csrPem,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back") }

            Button(onClick = {
                val dest = showSaveDialog("iso8583studio_license_request.csr", "CSR Files" to "csr")
                if (dest != null) {
                    dest.writeText(csrPem)
                    saved = true
                }
            }) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (saved) "Saved!" else "Save as .csr")
            }

            OutlinedButton(onClick = {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                clipboard.setContents(StringSelection(csrPem), null)
                copied = true
            }) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (copied) "Copied!" else "Copy")
            }

            Button(onClick = onNext) {
                Text("Next: Import Certificate")
            }
        }
    }
}

@Composable
private fun CertImportStep(
    certInput: String,
    onCertInputChange: (String) -> Unit,
    statusMessage: String,
    isError: Boolean,
    isValidating: Boolean,
    validationResult: LicenseSnapshot?,
    onImport: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(max = 600.dp)
    ) {
        Text(
            text = "Import your signed certificate:",
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    val file = showOpenDialog("Certificate Files" to arrayOf("cer", "crt", "pem"))
                    if (file != null) {
                        onCertInputChange(file.readText().trim())
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Browse .cer / .crt / .pem file")
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "— or paste certificate PEM below —",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = certInput,
            onValueChange = onCertInputChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
            placeholder = { Text("-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----") },
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
            maxLines = 20,
            enabled = !isValidating
        )

        if (isValidating) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Validating certificate with server...",
                    style = MaterialTheme.typography.body2,
                    color = InfoBlue
                )
            }
        }

        if (!isValidating && validationResult != null) {
            Spacer(Modifier.height(8.dp))
            LicenseStatusCard(validationResult)
        } else if (!isValidating && statusMessage.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = statusMessage,
                color = if (isError) ErrorRed else SuccessGreen,
                style = MaterialTheme.typography.body2
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack, enabled = !isValidating) { Text("Back") }
            Button(
                onClick = onImport,
                enabled = !isValidating && certInput.contains("BEGIN CERTIFICATE")
            ) {
                if (isValidating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colors.onPrimary
                    )
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text("Import & Validate")
            }
        }
    }
}

/**
 * Redesigned License Management dialog — proper windowed dialog with
 * status header, machine info, and step-based activation flow.
 */
@Composable
fun LicenseDialog(
    licenseSnapshot: LicenseSnapshot,
    onDismiss: () -> Unit,
    onActivated: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    var csrPem by remember { mutableStateOf("") }
    var certInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var isValidating by remember { mutableStateOf(false) }
    var validationResult by remember { mutableStateOf<LicenseSnapshot?>(null) }
    val scope = rememberCoroutineScope()

    val machineId = remember { MachineFingerprint.compute() }

    DialogWindow(
        onCloseRequest = { if (!isValidating) onDismiss() },
        title = "License Management — ISO8583Studio",
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            width = 620.dp,
            height = 640.dp
        ),
        resizable = false
    ) {
        MaterialTheme(
            colors = MaterialTheme.colors,
            typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colors.background
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    // ── Status Header ──
                    LicenseStatusHeader(licenseSnapshot)

                    Spacer(Modifier.height(20.dp))

                    // ── Machine Info ──
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 1.dp,
                        shape = RoundedCornerShape(8.dp),
                        backgroundColor = MaterialTheme.colors.surface
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Machine Details",
                                style = MaterialTheme.typography.subtitle2,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.height(8.dp))
                            InfoRow(
                                icon = Icons.Default.Computer,
                                label = "Machine ID",
                                value = machineId.take(16) + "..." + machineId.takeLast(8)
                            )
                            InfoRow(
                                icon = Icons.Default.Memory,
                                label = "Platform",
                                value = System.getProperty("os.name", "Unknown") + " / " +
                                        System.getProperty("os.arch", "")
                            )
                            if (licenseSnapshot.state == LicenseState.VALID ||
                                licenseSnapshot.state == LicenseState.EXPIRING_SOON
                            ) {
                                InfoRow(
                                    icon = Icons.Default.CalendarToday,
                                    label = "Expires in",
                                    value = "${licenseSnapshot.daysUntilExpiry} days"
                                )
                            }
                            if (licenseSnapshot.state == LicenseState.TRIAL) {
                                InfoRow(
                                    icon = Icons.Default.Timer,
                                    label = "Trial remaining",
                                    value = "${licenseSnapshot.daysUntilExpiry} days"
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))

                    Spacer(Modifier.height(20.dp))

                    // ── Action Area ──
                    when (currentStep) {
                        0 -> DialogActionChoiceStep(
                            onGenerateCsr = {
                                scope.launch {
                                    val result = CsrGenerator.generate()
                                    LicenseStorage.savePrivateKeyPem(result.privateKeyPem)
                                    csrPem = result.csrPem
                                    currentStep = 1
                                }
                            },
                            onImportCert = { currentStep = 2 }
                        )
                        1 -> CsrDisplayStep(
                            csrPem = csrPem,
                            onNext = { currentStep = 2 },
                            onBack = { currentStep = 0 }
                        )
                        2 -> CertImportStep(
                            certInput = certInput,
                            onCertInputChange = { certInput = it },
                            statusMessage = statusMessage,
                            isError = isError,
                            isValidating = isValidating,
                            validationResult = validationResult,
                            onImport = {
                                scope.launch {
                                    isValidating = true
                                    statusMessage = ""
                                    validationResult = null
                                    try {
                                        LicenseService.parseCertificate(certInput)
                                        LicenseStorage.saveCertificatePem(certInput)
                                        statusMessage = "Validating..."
                                        isError = false

                                        val snapshot = LicenseService.validate()
                                        validationResult = snapshot
                                        isValidating = false

                                        if (snapshot.isUsable()) {
                                            statusMessage = ""
                                            isError = false
                                            onActivated()
                                        } else {
                                            statusMessage = snapshot.message.ifBlank { "Validation failed: ${snapshot.state}" }
                                            isError = true
                                        }
                                    } catch (e: Exception) {
                                        isValidating = false
                                        statusMessage = "Invalid certificate: ${e.message}"
                                        isError = true
                                    }
                                }
                            },
                            onBack = { currentStep = 0 }
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    // ── Footer ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { if (!isValidating) onDismiss() },
                            enabled = !isValidating
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

// ── Dialog-specific action choice (card-based) ──

@Composable
private fun DialogActionChoiceStep(
    onGenerateCsr: () -> Unit,
    onImportCert: () -> Unit
) {
    Text(
        "Activation",
        style = MaterialTheme.typography.subtitle1,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ActionCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.VpnKey,
            title = "Generate CSR",
            subtitle = "Create a license request to send to AICortex",
            onClick = onGenerateCsr
        )
        ActionCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.FileUpload,
            title = "Import Certificate",
            subtitle = "Import a signed .cer / .crt / .pem file",
            onClick = onImportCert
        )
    }
}

@Composable
private fun ActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        elevation = 2.dp,
        shape = RoundedCornerShape(10.dp),
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colors.primary
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                title,
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.elevation(0.dp, 0.dp)
            ) {
                Text(title, fontSize = 13.sp)
            }
        }
    }
}

// ── Status Header ──

@Composable
private fun LicenseStatusHeader(snapshot: LicenseSnapshot) {
    val (color, label, icon) = when (snapshot.state) {
        LicenseState.VALID -> Triple(SuccessGreen, "License Active", Icons.Default.CheckCircle)
        LicenseState.TRIAL -> Triple(InfoBlue, "Trial Mode", Icons.Default.Timer)
        LicenseState.EXPIRING_SOON -> Triple(WarningOrange, "Expiring Soon", Icons.Default.Warning)
        LicenseState.EXPIRED -> Triple(ErrorRed, "License Expired", Icons.Default.Error)
        LicenseState.REVOKED -> Triple(ErrorRed, "License Revoked", Icons.Default.Block)
        LicenseState.ACTIVATION_REQUIRED -> Triple(WarningOrange, "Activation Required", Icons.Default.LockOpen)
        LicenseState.OFFLINE_GRACE_EXCEEDED -> Triple(ErrorRed, "Offline Grace Exceeded", Icons.Default.WifiOff)
        LicenseState.NOT_FOUND -> Triple(SubtleGray, "Not Activated", Icons.Default.HelpOutline)
        LicenseState.INVALID -> Triple(ErrorRed, "Invalid", Icons.Default.Error)
        LicenseState.TAMPER_DETECTED -> Triple(ErrorRed, "Security Violation", Icons.Default.GppBad)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = color
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                if (snapshot.message.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        snapshot.message,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ── Machine Info Row ──

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.35f)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.width(100.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.body2,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── Validation result card (shown inline after import) ──

@Composable
internal fun LicenseStatusCard(snapshot: LicenseSnapshot) {
    val (color, label) = when (snapshot.state) {
        LicenseState.VALID -> SuccessGreen to "Active"
        LicenseState.TRIAL -> InfoBlue to "Trial (${snapshot.daysUntilExpiry} days left)"
        LicenseState.EXPIRING_SOON -> WarningOrange to "Expiring (${snapshot.daysUntilExpiry} days)"
        LicenseState.EXPIRED -> ErrorRed to "Expired"
        LicenseState.REVOKED -> ErrorRed to "Revoked"
        LicenseState.ACTIVATION_REQUIRED -> WarningOrange to "Activation Required"
        LicenseState.OFFLINE_GRACE_EXCEEDED -> ErrorRed to "Offline Grace Exceeded"
        LicenseState.NOT_FOUND -> Color.Gray to "Not Activated"
        else -> ErrorRed to snapshot.state.name
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    snapshot.isUsable() -> Icons.Default.CheckCircle
                    else -> Icons.Default.Error
                },
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "License Status: $label",
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
                if (snapshot.message.isNotBlank()) {
                    Text(
                        text = snapshot.message,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * Non-blocking trial nudge banner shown at the top of the main window.
 */
@Composable
fun LicenseTrialBanner(
    daysRemaining: Int,
    onActivateClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = InfoBlue.copy(alpha = 0.12f),
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = InfoBlue
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Trial: $daysRemaining day${if (daysRemaining != 1) "s" else ""} remaining",
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Medium,
                    color = InfoBlue
                )
            }
            TextButton(onClick = onActivateClick, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text("Activate Now", fontSize = 12.sp, color = InfoBlue)
            }
        }
    }
}

/**
 * Warning banner for licenses expiring within 30 days.
 */
@Composable
fun LicenseExpiryBanner(
    daysRemaining: Int,
    onRenewClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = WarningOrange.copy(alpha = 0.12f),
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = WarningOrange
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "License expires in $daysRemaining day${if (daysRemaining != 1) "s" else ""}",
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Medium,
                    color = WarningOrange
                )
            }
            TextButton(onClick = onRenewClick, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text("Renew", fontSize = 12.sp, color = WarningOrange)
            }
        }
    }
}

/**
 * Persistent error banner for invalid/expired/revoked licenses.
 * Shown at the top of the main window when the license state is blocking.
 */
@Composable
fun LicenseInvalidBanner(
    licenseSnapshot: LicenseSnapshot,
    onActivateClick: () -> Unit
) {
    val (color, label) = when (licenseSnapshot.state) {
        LicenseState.EXPIRED -> ErrorRed to "License Expired"
        LicenseState.REVOKED -> ErrorRed to "License Revoked"
        LicenseState.INVALID -> ErrorRed to "License Invalid"
        LicenseState.OFFLINE_GRACE_EXCEEDED -> ErrorRed to "Offline Grace Exceeded"
        LicenseState.TAMPER_DETECTED -> ErrorRed to "Security Violation"
        LicenseState.NOT_FOUND -> WarningOrange to "No License"
        LicenseState.ACTIVATION_REQUIRED -> WarningOrange to "Activation Required"
        else -> return
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = color.copy(alpha = 0.12f),
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = color
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = label + if (licenseSnapshot.message.isNotBlank()) " — ${licenseSnapshot.message}" else "",
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Medium,
                    color = color,
                    maxLines = 1
                )
            }
            TextButton(onClick = onActivateClick, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text("Activate", fontSize = 12.sp, color = color)
            }
        }
    }
}

// --- AWT File Dialog helpers (lightweight, no extra dependencies) ---

private fun showSaveDialog(defaultName: String, filter: Pair<String, String>): File? {
    val dialog = FileDialog(null as Frame?, "Save CSR File", FileDialog.SAVE)
    dialog.file = defaultName
    dialog.filenameFilter = FilenameFilter { _, name -> name.endsWith(".${filter.second}") }
    dialog.isVisible = true
    val dir = dialog.directory ?: return null
    val file = dialog.file ?: return null
    val target = File(dir, file)
    if (!target.name.endsWith(".${filter.second}")) {
        return File(target.parentFile, target.name + ".${filter.second}")
    }
    return target
}

private fun showOpenDialog(filter: Pair<String, Array<String>>): File? {
    val dialog = FileDialog(null as Frame?, "Open Certificate File", FileDialog.LOAD)
    dialog.filenameFilter = FilenameFilter { _, name ->
        val lower = name.lowercase()
        filter.second.any { lower.endsWith(".$it") }
    }
    dialog.isVisible = true
    val dir = dialog.directory ?: return null
    val file = dialog.file ?: return null
    return File(dir, file)
}
