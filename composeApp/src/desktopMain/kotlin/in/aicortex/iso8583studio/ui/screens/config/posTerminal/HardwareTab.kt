package `in`.aicortex.iso8583studio.ui.screens.config.posTerminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.HardwarePropertyCatalog
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.PropGroup
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.Severity
import `in`.aicortex.iso8583studio.ui.ErrorRed
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.WarningYellow
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.pos.POSSimulatorConfig

/**
 * Real emulator hardware, replacing the display-string dropdowns this tab used to hold.
 *
 * Every control is driven by [HardwarePropertyCatalog]'s curated facets over the emulator's own
 * `hardware-properties.ini` schema, so the values written here are exactly the `config.ini` keys
 * the AVD will boot with. Anything not curated stays reachable in the Advanced tab.
 */
@Composable
fun HardwareTab(config: POSSimulatorConfig, onConfigUpdate: (POSSimulatorConfig) -> Unit) {
    val schema = remember { PosConfigEditing.schema() }
    val effective = PosConfigEditing.effective(config, schema)
    val issues = remember(config, effective) { PosConfigEditing.validate(config, schema) }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {

        ValidationStrip(issues.map { it.severity to "${it.message} — ${it.remediation}" })

        PropertyGroup(config, onConfigUpdate, schema, effective, PropGroup.MEMORY_STORAGE, Icons.Default.Memory)
        PropertyGroup(config, onConfigUpdate, schema, effective, PropGroup.DISPLAY, Icons.Default.Monitor)
        PropertyGroup(config, onConfigUpdate, schema, effective, PropGroup.GRAPHICS, Icons.Default.DeveloperBoard)
        PropertyGroup(config, onConfigUpdate, schema, effective, PropGroup.INPUT, Icons.Default.TouchApp)
        PropertyGroup(config, onConfigUpdate, schema, effective, PropGroup.CAMERA, Icons.Default.CameraAlt)
        PropertyGroup(config, onConfigUpdate, schema, effective, PropGroup.SENSORS, Icons.Default.Sensors)
    }
}

@Composable
internal fun PropertyGroup(
    config: POSSimulatorConfig,
    onConfigUpdate: (POSSimulatorConfig) -> Unit,
    schema: `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.HardwarePropertiesSchema,
    effective: Map<String, String>,
    group: PropGroup,
    icon: ImageVector,
) {
    val facets = HardwarePropertyCatalog.group(group)
    if (facets.isEmpty()) return

    ConfigSection(group.label, icon) {
        Column(modifier = Modifier.fillMaxWidth()) {
            facets.forEachIndexed { index, facet ->
                if (index > 0) Divider(modifier = Modifier.fillMaxWidth())
                PropertyFacetField(
                    facet = facet,
                    schema = schema,
                    value = effective[facet.key].orEmpty(),
                    isOverridden = PosConfigEditing.isOverridden(config, facet.key),
                    onChange = { onConfigUpdate(PosConfigEditing.withOverride(config, facet.key, it)) },
                    onReset = { onConfigUpdate(PosConfigEditing.clearOverride(config, facet.key)) },
                )
            }
        }
    }
}

/**
 * Live output from `AvdValidator`, errors first.
 *
 * This is what turns a mistyped partition size or a Play Store image into an explanation at edit
 * time rather than a ten-minute non-boot later.
 */
@Composable
internal fun ValidationStrip(issues: List<Pair<Severity, String>>) {
    if (issues.isEmpty()) {
        PosNoticeStrip(
            text = "Configuration looks valid.",
            color = SuccessGreen,
            icon = Icons.Default.CheckCircle,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        issues.sortedBy { it.first.ordinal }.forEach { (severity, message) ->
            PosNoticeStrip(
                text = message,
                color = when (severity) {
                    Severity.ERROR -> ErrorRed
                    Severity.WARNING -> WarningYellow
                    Severity.INFO -> MaterialTheme.colors.primary
                },
            )
        }
    }
}
