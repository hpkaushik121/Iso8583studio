package `in`.aicortex.iso8583studio.ui.screens.config.posTerminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.DeviceCatalog
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.DeviceFeature
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.DeviceVendor
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.SpecConfidence
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.TerminalId
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.TerminalModel
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.density
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.ramMb
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.resolutionSummary
import `in`.aicortex.iso8583studio.ui.WarningYellow
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.pos.POSSimulatorConfig
import `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField

/**
 * Vendor → model → variant.
 *
 * Variants are the reason this is three levels rather than two: the PAX A910S ships with a 5" HD
 * display *or* an optional 5.5" one, and Ingenico's DX8000 series has DX8000/DX8005/DX8010 SKUs.
 * Those change screen geometry and peripherals, so they are genuinely different devices to emulate.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceTab(
    config: POSSimulatorConfig,
    onConfigUpdate: (POSSimulatorConfig) -> Unit,
) {
    val selectedModelId = TerminalId.modelOf(config.terminalProfileId)
    val selectedVariantId = TerminalId.variantOf(config.terminalProfileId)

    var vendorFilter by remember { mutableStateOf<DeviceVendor?>(null) }
    var search by remember { mutableStateOf("") }

    val matching = remember(vendorFilter, search) {
        DeviceCatalog.search(search).let { found ->
            vendorFilter?.let { v -> found.filter { it.vendor == v } } ?: found
        }
    }
    val selectedModel = DeviceCatalog[selectedModelId]
    val resolved = DeviceCatalog.resolve(config.terminalProfileId)

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {

        // ---------------- Vendor + search ----------------
        ConfigSection("Vendor", Icons.Default.Storefront) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ConfigDropdown(
                    label = "Manufacturer",
                    currentValue = vendorFilter?.displayName ?: ALL_VENDORS,
                    options = listOf(ALL_VENDORS) + DeviceCatalog.vendors.map { it.displayName },
                    onValueChange = { picked ->
                        vendorFilter = DeviceCatalog.vendors.firstOrNull { it.displayName == picked }
                    },
                )
                FixedOutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Search model, variant or SKU") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        }

        // ---------------- Model ----------------
        ConfigSection("Model", Icons.Default.PhoneAndroid) {
            if (matching.isEmpty()) {
                Text(
                    "No models match \"$search\".",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    matching.forEach { model ->
                        PosOptionCard(
                            selected = model.id == selectedModelId,
                            title = model.label,
                            subtitle = model.summaryLine(),
                            onSelect = {
                                // Selecting a model always lands on its default variant, so the
                                // resolved id is never left pointing at a variant of another model.
                                val defaultVariant = model.defaultVariant?.id
                                onConfigUpdate(
                                    config.copy(
                                        terminalProfileId = TerminalId.of(model.id, defaultVariant),
                                        modifiedDate = System.currentTimeMillis(),
                                    )
                                )
                            },
                            modifier = Modifier.width(260.dp),
                            badges = { DalStatusBadge(model.dalStatus) },
                        )
                    }
                }
            }
        }

        // ---------------- Variant ----------------
        if (selectedModel != null && selectedModel.variants.isNotEmpty()) {
            ConfigSection("Variant", Icons.Default.Category) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Variants of the same model differ in screen, memory, peripherals or " +
                            "Android version — pick the one matching the unit you are targeting.",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        selectedModel.variants.forEach { variant ->
                            val variantResolved = selectedModel.resolve(variant.id)
                            PosOptionCard(
                                selected = variant.id == selectedVariantId,
                                title = variant.displayLabel,
                                subtitle = variantResolved.hardware.resolutionSummary,
                                onSelect = {
                                    onConfigUpdate(
                                        config.copy(
                                            terminalProfileId = TerminalId.of(selectedModel.id, variant.id),
                                            modifiedDate = System.currentTimeMillis(),
                                        )
                                    )
                                },
                                modifier = Modifier.width(260.dp),
                                badges = { ProvenanceBadge(variantResolved.hardware.provenance) },
                            )
                        }
                    }
                }
            }
        }

        // ---------------- Resolved spec ----------------
        ConfigSection("Resolved device", Icons.Default.DeveloperBoard) {
            if (resolved == null) {
                Text(
                    "Unknown terminal \"${config.terminalProfileId}\". Pick a model above.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.error,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val hw = resolved.hardware
                    val term = resolved.terminal

                    if (hw.provenance.confidence != SpecConfidence.VERIFIED_FROM_DEVICE) {
                        PosNoticeStrip(
                            text = "Unverified geometry — these numbers are inferred from published " +
                                "specifications, not read from hardware. Import from a connected " +
                                "device to confirm them.",
                            color = WarningYellow,
                        )
                    }

                    PosSpecRow("Terminal", term.displayName)
                    PosSpecRow("Identity", resolved.id, mono = true)
                    PosSpecRow("Screen", hw.resolutionSummary ?: "—", mono = true)
                    PosSpecRow("Density", hw.density?.let { "$it dpi" } ?: "—", mono = true)
                    PosSpecRow("Memory", hw.ramMb?.let { "$it MB" } ?: "—", mono = true)
                    PosSpecRow("System image", term.recommendedImage.toString(), mono = true)
                    if (term.realAndroidVersion.isNotBlank()) {
                        PosSpecRow("Device Android", term.realAndroidVersion)
                    }
                    PosSpecRow("Payment SDK", term.dal.displayName)
                    PosSpecRow("Peripherals", term.features.featureSummary())

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProvenanceBadge(hw.provenance)
                        DalStatusBadge(term.dalStatus)
                    }
                    if (hw.provenance.source.isNotBlank()) {
                        Text(
                            "Source: ${hw.provenance.source}",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f),
                        )
                    }
                    if (term.notes.isNotBlank()) {
                        PosNoticeStrip(term.notes, MaterialTheme.colors.primary, icon = Icons.Default.Info)
                    }
                }
            }
        }
    }
}

private const val ALL_VENDORS = "All vendors"

/** `4 variants · 720 x 1440 @ 320 dpi` — enough to tell two models apart in the grid. */
private fun TerminalModel.summaryLine(): String {
    val geometry = resolve().hardware.resolutionSummary ?: "unknown geometry"
    return if (variants.isEmpty()) geometry else "${variants.size} variants · $geometry"
}

private fun Set<DeviceFeature>.featureSummary(): String =
    if (isEmpty()) "—" else sortedBy { it.name }.joinToString(", ") { it.name }
