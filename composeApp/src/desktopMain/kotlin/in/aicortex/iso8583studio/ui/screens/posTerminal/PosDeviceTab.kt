package `in`.aicortex.iso8583studio.ui.screens.posTerminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.POSSimulatorService
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.PosDeviceState
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.PosPhase
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.AvdStep
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.groupAvdSteps
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.AndroidSdkLocator
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.SdkResolution
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.SystemImageCatalog
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.AvdProperties
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.ResolvedTerminal
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.density
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.ramMb
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.resolutionSummary
import `in`.aicortex.iso8583studio.ui.ErrorRed
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.WarningYellow
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.pos.POSSimulatorConfig

/**
 * The device console home: what this terminal is, whether the machine can actually boot it, and
 * the controls to do so.
 *
 * Power on creates the AVD if needed, boots it, and applies the device identity by rewriting the
 * partition build.prop files — `emulator -prop` does not set `ro.product.*`. Progress is a live
 * phase stepper rather than a spinner, because a cold boot can legitimately take a minute and a
 * failure needs to say which phase it failed in.
 */
@Composable
fun PosDeviceTab(service: POSSimulatorService, resolved: ResolvedTerminal?) {
    val config = service.config
    val state by service.state.collectAsState()
    val resolution = remember { AndroidSdkLocator.locate() }
    val sdk = (resolution as? SdkResolution.Found)?.sdk
    val installed = remember(sdk) { sdk?.let { SystemImageCatalog.scanInstalled(it) } ?: emptyList() }
    val hostAbi = remember { SystemImageCatalog.hostAbi() }

    val image = config.avd.systemImage.takeIf { it.apiLevel > 0 }
        ?: resolved?.terminal?.recommendedImage
    val imageInstalled = image != null && installed.any {
        it.ref.apiLevel == image.apiLevel && it.ref.tag == image.tag &&
            (image.abi.isEmpty() || it.ref.abi == image.abi)
    }
    val avdName = service.avdName.ifBlank { "—" }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DeviceStatusBar(resolved, avdName, image?.toString() ?: "—", hostAbi, state)
        PowerPanel(
            state = state,
            canBoot = sdk != null && imageInstalled && resolved != null,
            onPowerOn = service::powerOn,
            onPowerOff = service::powerOff,
            onPrepare = service::prepare,
        )

        if (service.steps.isNotEmpty()) {
            PhaseStepper(service.steps)
        }
        state.error?.let { error ->
            ConsoleCard("Problem") {
                Text(error, style = MaterialTheme.typography.caption, color = ErrorRed)
                state.remediation?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
        }

        ConsoleCard("Prerequisites") {
            PrerequisiteRow(
                ok = sdk != null,
                label = "Android SDK",
                detail = sdk?.root?.toString() ?: (resolution as? SdkResolution.Missing)?.remediation.orEmpty(),
            )
            PrerequisiteRow(
                ok = sdk?.emulator != null,
                label = "Emulator binary",
                detail = sdk?.emulator?.toString() ?: "Install the Emulator package in the SDK Manager.",
            )
            PrerequisiteRow(
                ok = sdk?.avdmanager != null && sdk.sdkmanager != null,
                label = "Command-line tools",
                detail = sdk?.avdmanager?.toString()
                    ?: "Install \"Android SDK Command-line Tools\" in the SDK Manager.",
            )
            PrerequisiteRow(
                ok = sdk?.adb != null,
                label = "adb",
                detail = sdk?.adb?.toString() ?: "Install Android SDK Platform-Tools.",
            )
            PrerequisiteRow(
                ok = imageInstalled,
                label = "System image",
                detail = if (imageInstalled) image.toString()
                else "Not installed. Run: sdkmanager \"${image?.sdkPackage}\"",
            )
            PrerequisiteRow(
                ok = image?.abi.isNullOrEmpty() || image?.abi == hostAbi,
                label = "ABI matches host",
                detail = if (image?.abi.isNullOrEmpty() || image?.abi == hostAbi) {
                    "$hostAbi — hardware accelerated"
                } else {
                    "${image?.abi} on a $hostAbi host falls back to software emulation, 10-50x slower."
                },
            )
        }

        if (resolved != null) {
            ConsoleCard("Device") {
                SpecLine("Terminal", resolved.terminal.displayName)
                SpecLine("Identity", resolved.id, mono = true)
                SpecLine("Screen", resolved.hardware.resolutionSummary ?: "—", mono = true)
                SpecLine("Density", resolved.hardware.density?.let { "$it dpi" } ?: "—", mono = true)
                SpecLine("Memory", resolved.hardware.ramMb?.let { "$it MB" } ?: "—", mono = true)
                SpecLine("AVD name", avdName, mono = true)
                SpecLine("Payment SDK", resolved.terminal.dal.displayName)
                SpecLine("SDK status", resolved.terminal.dalStatus.label)
                SpecLine(
                    "Peripherals",
                    resolved.terminal.features.sortedBy { it.name }.joinToString(", ") { it.label },
                )
            }

            ConsoleCard("Spoofed identity") {
                resolved.terminal.effectiveBootProps().forEach { (k, v) -> SpecLine(k, v, mono = true) }
            }
        }
    }
}

@Composable
private fun DeviceStatusBar(
    resolved: ResolvedTerminal?,
    avdName: String,
    image: String,
    hostAbi: String,
    state: PosDeviceState,
) {
    val tint = when (state.phase) {
        PosPhase.READY -> SuccessGreen
        PosPhase.PREPARING, PosPhase.BOOTING -> WarningYellow
        PosPhase.ERROR -> ErrorRed
        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.45f)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = tint.copy(alpha = 0.08f),
        elevation = 1.dp,
        shape = RoundedCornerShape(4.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(5.dp).background(tint, CircleShape))
            Small(resolved?.terminal?.displayName ?: "No device", FontWeight.Medium)
            Small(image, mono = true)
            Small("host $hostAbi", mono = true)
            Box(Modifier.weight(1f))
            state.serial?.let { Small(it, mono = true) }
            Small(avdName, mono = true)
            Small(
                if (state.phase == PosPhase.READY && state.identityTotal > 0) {
                    "${state.phase.label} · identity ${state.identityMatched}/${state.identityTotal}"
                } else {
                    state.phase.label
                },
                FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun PowerPanel(
    state: PosDeviceState,
    canBoot: Boolean,
    onPowerOn: () -> Unit,
    onPowerOff: () -> Unit,
    onPrepare: () -> Unit,
) {
    val running = state.running
    Surface(elevation = 1.dp, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = if (running) onPowerOff else onPowerOn,
                    enabled = canBoot && !state.busy,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(12.dp, 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (running) MaterialTheme.colors.error else MaterialTheme.colors.primary,
                    ),
                ) {
                    if (state.busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            if (running) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            Modifier.size(14.dp),
                        )
                    }
                    Text(
                        if (running) "  Power off" else "  Power on",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Medium,
                    )
                }
                OutlinedButton(
                    onClick = onPrepare,
                    enabled = canBoot && !state.busy && !running,
                    modifier = Modifier.height(32.dp),
                ) {
                    Text("Prepare AVD", style = MaterialTheme.typography.caption)
                }
                OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.height(32.dp)) {
                    Text("Install APK…", style = MaterialTheme.typography.caption)
                }
            }
            // The house convention: a disabled control always says why, right beneath it.
            Text(
                text = when {
                    !canBoot -> "Resolve the prerequisites below before this device can be created."
                    state.busy -> "${state.phase.label}… this can take a minute on a cold boot."
                    running -> "Running. Power on auto-prepares the AVD first, so it is safe to press again after changes."
                    else -> "Power on will create the AVD if needed, boot it, and apply the device identity."
                },
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/**
 * Live phase list for prepare and boot.
 *
 * Tri-state per phase — done, failed, still running — matching the EMV transaction stepper, so a
 * long boot shows where it is rather than a spinner with no information.
 */
@Composable
private fun PhaseStepper(steps: List<AvdStep>) {
    val blocks = remember(steps.size) { groupAvdSteps(steps) }
    ConsoleCard("Progress") {
        blocks.forEach { block ->
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                when (block.ok) {
                    true -> Icon(
                        Icons.Default.CheckCircle, null,
                        tint = SuccessGreen, modifier = Modifier.size(14.dp).padding(top = 1.dp),
                    )
                    false -> Icon(
                        Icons.Default.Cancel, null,
                        tint = ErrorRed, modifier = Modifier.size(14.dp).padding(top = 1.dp),
                    )
                    null -> CircularProgressIndicator(
                        modifier = Modifier.size(12.dp).padding(top = 1.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colors.primary,
                    )
                }
                Column {
                    Text(
                        block.phase.label,
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Medium,
                    )
                    block.commands.forEach {
                        Text(
                            "$ ${it.commandLine}",
                            style = MaterialTheme.typography.caption.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f),
                        )
                    }
                    block.lines.takeLast(6).forEach {
                        Text(
                            it.text,
                            style = MaterialTheme.typography.caption.copy(fontFamily = FontFamily.Monospace),
                            color = if (it.isError) ErrorRed
                            else MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    block.remediation?.let {
                        Text(it, style = MaterialTheme.typography.caption, color = WarningYellow)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsoleCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(elevation = 1.dp, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.overline,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary,
            )
            content()
        }
    }
}

@Composable
private fun PrerequisiteRow(ok: Boolean, label: String, detail: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(
            imageVector = if (ok) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (ok) SuccessGreen else ErrorRed,
            modifier = Modifier.size(14.dp).padding(top = 1.dp),
        )
        Column {
            Text(label, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Medium)
            if (detail.isNotBlank()) {
                Text(
                    detail,
                    style = MaterialTheme.typography.caption.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun SpecLine(label: String, value: String, mono: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            Modifier.width(160.dp),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
        )
        Text(
            value,
            style = if (mono) {
                MaterialTheme.typography.caption.copy(fontFamily = FontFamily.Monospace)
            } else {
                MaterialTheme.typography.caption
            },
        )
    }
}

@Composable
private fun Small(text: String, weight: FontWeight = FontWeight.Normal, mono: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.caption.copy(
            fontSize = 10.sp,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
        ),
        fontWeight = weight,
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
    )
}

/**
 * The convention for a tab whose backend has not landed: say what it will do and which milestone
 * brings it, rather than rendering a bare "Pending".
 */
@Composable
fun PosPendingTab(title: String, description: String, milestone: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Card(elevation = 1.dp, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    title.uppercase(),
                    style = MaterialTheme.typography.overline,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary,
                )
                Text(description, style = MaterialTheme.typography.body2)
                Box(
                    Modifier
                        .background(WarningYellow.copy(alpha = 0.10f), RoundedCornerShape(4.dp))
                        .border(
                            BorderStroke(1.dp, WarningYellow.copy(alpha = 0.30f)),
                            RoundedCornerShape(4.dp),
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = WarningYellow,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(milestone, style = MaterialTheme.typography.caption)
                    }
                }
                OutlinedButton(onClick = {}, enabled = false) {
                    Text("Not available yet", style = MaterialTheme.typography.caption)
                }
            }
        }
    }
}

