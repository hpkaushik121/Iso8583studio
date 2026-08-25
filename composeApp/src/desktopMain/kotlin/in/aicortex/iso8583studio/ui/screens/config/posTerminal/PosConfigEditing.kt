package `in`.aicortex.iso8583studio.ui.screens.config.posTerminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.AvdValidationInput
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.AvdValidator
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.AndroidSdkLocator
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.HardwarePropertiesSchema
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.PropertyType
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.PropertyFacet
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.SdkResolution
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.SkinCatalog
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.SystemImageCatalog
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.ValidationIssue
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.AvdProperties
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.DeviceCatalog
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.ResolvedTerminal
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.pos.POSSimulatorConfig
import `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField

/**
 * Bridges a [POSSimulatorConfig] to the AVD/device layer.
 *
 * Edits land in `config.avd.overrides` — a delta on top of the resolved hardware profile — so the
 * catalog entry is never mutated and "reset to default" is just removing a key.
 */
object PosConfigEditing {

    fun resolved(config: POSSimulatorConfig): ResolvedTerminal? =
        DeviceCatalog.resolve(config.terminalProfileId)

    /** The SDK's own schema when one is installed, otherwise the bundled snapshot. */
    fun schema(): HardwarePropertiesSchema {
        val sdk = (AndroidSdkLocator.locate() as? SdkResolution.Found)?.sdk
        return HardwarePropertiesSchema.load(sdk?.root)
    }

    /** Schema defaults, then the resolved hardware profile, then this config's overrides. */
    fun effective(config: POSSimulatorConfig, schema: HardwarePropertiesSchema): Map<String, String> {
        val hardware = resolved(config)?.hardware ?: return schema.defaults() + config.avd.overrides
        return AvdProperties.effective(config.avd, hardware, schema)
    }

    /** Only the keys that will actually be written to `config.ini`. */
    fun configIniOverlay(config: POSSimulatorConfig): Map<String, String> {
        val hardware = resolved(config)?.hardware ?: return config.avd.overrides
        return AvdProperties.configIniOverlay(config.avd, hardware)
    }

    fun withOverride(config: POSSimulatorConfig, key: String, value: String): POSSimulatorConfig =
        config.copy(avd = config.avd.copy(overrides = config.avd.overrides + (key to value)))

    fun clearOverride(config: POSSimulatorConfig, key: String): POSSimulatorConfig =
        config.copy(avd = config.avd.copy(overrides = config.avd.overrides - key))

    /** True when this config has moved a property away from the profile/schema value. */
    fun isOverridden(config: POSSimulatorConfig, key: String): Boolean = key in config.avd.overrides

    fun validate(config: POSSimulatorConfig, schema: HardwarePropertiesSchema): List<ValidationIssue> {
        val res = resolved(config) ?: return emptyList()
        val sdk = (AndroidSdkLocator.locate() as? SdkResolution.Found)?.sdk
        val installed = sdk?.let { SystemImageCatalog.scanInstalled(it).map { img -> img.ref } } ?: emptyList()
        val skin = res.hardware.skin
            ?.takeIf { it.path.isNotBlank() }
            ?.let { runCatching { SkinCatalog.read(java.nio.file.Path.of(it.path)) }.getOrNull() }

        return AvdValidator.validate(
            AvdValidationInput(
                avdName = config.avd.avdName.ifBlank { AvdProperties.suggestAvdName(res.terminal) },
                properties = effective(config, schema),
                image = config.avd.systemImage.takeIf { it.abi.isNotEmpty() || it.apiLevel > 0 }
                    ?: res.terminal.recommendedImage,
                installedImages = installed,
                skin = skin,
            )
        )
    }

    /**
     * The image this config actually boots: the AVD spec's own choice when set, otherwise the
     * terminal's recommendation.
     */
    fun effectiveImage(config: POSSimulatorConfig) =
        config.avd.systemImage.takeIf { it.apiLevel > 0 }
            ?: resolved(config)?.terminal?.recommendedImage
            ?: config.avd.systemImage
}

/**
 * One editable emulator property, rendered from its schema type.
 *
 * Booleans become switches, closed enums become dropdowns, everything else a text field. Open-ended
 * enums (the three that end in `...` in `hardware-properties.ini`) deliberately stay free-text —
 * the emulator accepts values beyond the listed ones.
 */
@Composable
fun PropertyFacetField(
    facet: PropertyFacet,
    schema: HardwarePropertiesSchema,
    value: String,
    isOverridden: Boolean,
    onChange: (String) -> Unit,
    onReset: () -> Unit,
) {
    val property = schema[facet.key]
    val type = property?.type ?: PropertyType.STRING
    val options = facet.enumOverride ?: property?.enumValues.orEmpty()
    val closedEnum = options.isNotEmpty() && (facet.enumOverride != null || property?.isClosedEnum == true)

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            when {
                type == PropertyType.BOOLEAN -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(facet.label, style = MaterialTheme.typography.body2)
                            Text(
                                facet.key,
                                style = MaterialTheme.typography.caption.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.45f),
                            )
                        }
                        Switch(
                            checked = HardwarePropertiesSchema.parseBoolean(value) ?: false,
                            onCheckedChange = { onChange(HardwarePropertiesSchema.formatBoolean(it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                        )
                    }
                }

                closedEnum -> ConfigDropdown(
                    label = facet.label,
                    currentValue = value,
                    options = options,
                    onValueChange = onChange,
                )

                else -> FixedOutlinedTextField(
                    value = value,
                    onValueChange = onChange,
                    label = { Text(facet.label) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        }

        // Reset appears only once the value has actually been overridden, so the row stays quiet
        // until there is something to undo.
        if (isOverridden) {
            IconButton(onClick = onReset, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Reset ${facet.key} to the device default",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colors.primary,
                )
            }
        }
    }
}
