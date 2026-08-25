package `in`.aicortex.iso8583studio.ui.screens.config.posTerminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Sd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.AndroidSdkLocator
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.SdkResolution
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.SystemImageCatalog
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.AvdProperties
import `in`.aicortex.iso8583studio.ui.ErrorRed
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.WarningYellow
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.pos.POSSimulatorConfig
import `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField

/**
 * System image, AVD identity and boot options — everything that decides what actually gets created
 * on disk and how it starts.
 *
 * Replaces the old connectivity/OS-type dropdowns, which were display strings nothing read.
 */
@Composable
fun NetworkSoftwareTab(config: POSSimulatorConfig, onConfigUpdate: (POSSimulatorConfig) -> Unit) {
    val resolution = remember { AndroidSdkLocator.locate() }
    val sdk = (resolution as? SdkResolution.Found)?.sdk
    val installed = remember(sdk) { sdk?.let { SystemImageCatalog.scanInstalled(it) } ?: emptyList() }
    val hostAbi = remember { SystemImageCatalog.hostAbi() }
    val resolved = PosConfigEditing.resolved(config)
    val image = PosConfigEditing.effectiveImage(config)

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {

        // ---------------- SDK ----------------
        ConfigSection("Android SDK", Icons.Default.Album) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                when (resolution) {
                    is SdkResolution.Missing -> PosNoticeStrip(
                        "${resolution.reason}\n${resolution.remediation}",
                        ErrorRed,
                    )

                    is SdkResolution.Found -> {
                        PosSpecRow("SDK root", resolution.sdk.root.toString(), mono = true)
                        PosSpecRow("Found via", resolution.sdk.source.label)
                        PosSpecRow("AVD home", resolution.sdk.avdHome.toString(), mono = true)
                        PosSpecRow("Host ABI", hostAbi, mono = true)
                        resolution.warnings.forEach { PosNoticeStrip(it, WarningYellow) }
                        if (resolution.warnings.isEmpty()) {
                            PosNoticeStrip("All required SDK tools found.", SuccessGreen)
                        }
                    }
                }
            }
        }

        // ---------------- System image ----------------
        ConfigSection("System image", Icons.Default.Sd) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                resolved?.terminal?.realAndroidVersion?.takeIf { it.isNotBlank() }?.let { real ->
                    PosNoticeStrip(
                        "The real device runs Android $real. Google publishes no arm64 emulator " +
                            "image for API 29, so API ${image.apiLevel} is used instead.",
                        WarningYellow,
                    )
                }

                if (installed.isEmpty()) {
                    PosNoticeStrip(
                        "No system images installed. Install one with: sdkmanager \"${image.sdkPackage}\"",
                        ErrorRed,
                    )
                } else {
                    Text(
                        "Installed images — pick the one this terminal boots.",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    )
                    installed.forEach { candidate ->
                        val selected = candidate.ref.apiLevel == image.apiLevel &&
                            candidate.ref.tag == image.tag &&
                            (image.abi.isEmpty() || candidate.ref.abi == image.abi)
                        val warning = when {
                            candidate.ref.isPlayStore ->
                                "Play Store image — `adb root` is refused, so the device host cannot be installed."
                            candidate.ref.abi != hostAbi ->
                                "ABI does not match this host — software emulation, unusably slow."
                            !candidate.ref.hasGoogleApis -> "No Google APIs."
                            else -> null
                        }
                        PosOptionCard(
                            selected = selected,
                            title = "Android ${candidate.ref.apiLevel} · ${candidate.tagDisplay} · ${candidate.ref.abi}",
                            subtitle = warning ?: "rev ${candidate.revision}",
                            onSelect = {
                                onConfigUpdate(
                                    config.copy(avd = config.avd.copy(systemImage = candidate.ref))
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        // ---------------- AVD identity + boot ----------------
        ConfigSection("AVD & boot", Icons.Default.PlayCircle) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val suggested = resolved?.terminal?.let { AvdProperties.suggestAvdName(it) } ?: ""
                FixedOutlinedTextField(
                    value = config.avd.avdName,
                    onValueChange = { onConfigUpdate(config.copy(avd = config.avd.copy(avdName = it))) },
                    label = { Text("AVD name") },
                    placeholder = { Text(suggested) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Text(
                    "Managed AVDs must start with \"${AvdProperties.MANAGED_PREFIX}\". Delete and " +
                        "recreate refuse anything without it, so your own AVDs can never be touched.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )

                Divider()

                BootToggle(
                    "Cold boot every start",
                    "Deterministic, and snapshots silently revert the writable /system overlay.",
                    config.avd.coldBoot,
                ) { onConfigUpdate(config.copy(avd = config.avd.copy(coldBoot = it))) }

                BootToggle(
                    "Writable /system",
                    "Required to install the payment SDK host and spoof ro.product.* values.",
                    config.avd.writableSystem,
                ) { onConfigUpdate(config.copy(avd = config.avd.copy(writableSystem = it))) }

                BootToggle(
                    "SELinux permissive",
                    "Needed until a proper policy exists for the device host.",
                    config.avd.selinuxPermissive,
                ) { onConfigUpdate(config.copy(avd = config.avd.copy(selinuxPermissive = it))) }

                BootToggle(
                    "Headless (-no-window)",
                    "Drives the device entirely through the bridge. The mode CI uses.",
                    config.avd.headless,
                ) { onConfigUpdate(config.copy(avd = config.avd.copy(headless = it))) }
            }
        }

        // ---------------- Spoofed identity ----------------
        if (resolved != null) {
            ConfigSection("Spoofed device identity", Icons.Default.Fingerprint) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Injected with `emulator -prop` before init, because ro.* properties are " +
                            "immutable once set.",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    )
                    resolved.terminal.effectiveBootProps().forEach { (key, value) ->
                        PosSpecRow(key, value, mono = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun BootToggle(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.body2)
            Text(
                subtitle,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
        )
    }
}
