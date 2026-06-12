package `in`.aicortex.iso8583studio.ui.screens.config.apduSimulator.v2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine.TestPlan
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine.plans.BuiltInPlans
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine.plans.PlanLoader
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.apdu.APDUSimulatorConfig
import java.awt.Desktop

private data class PlanEntry(val plan: TestPlan, val builtIn: Boolean)

/**
 * Plans tab — chooses which test plans are enabled for this simulator and which one (if any)
 * auto-runs after a successful Connect. Custom JSON plans are loaded from a configurable directory.
 */
@Composable
fun PlansTab(config: APDUSimulatorConfig, onConfigUpdate: (APDUSimulatorConfig) -> Unit) {
    var customPlans by remember { mutableStateOf<List<TestPlan>>(emptyList()) }
    var reloadTick by remember { mutableStateOf(0) }
    val resolvedDir = remember(config.customTestPlanDir) {
        PlanLoader.resolveDir(config.customTestPlanDir)
    }

    LaunchedEffect(config.customTestPlanDir, reloadTick) {
        customPlans = runCatching { PlanLoader.load(resolvedDir) }.getOrDefault(emptyList())
    }

    val builtIns = remember { BuiltInPlans.all() }
    val entries: List<PlanEntry> = remember(builtIns, customPlans) {
        builtIns.map { PlanEntry(it, builtIn = true) } +
            customPlans.map { PlanEntry(it, builtIn = false) }
    }

    val enabledIds = config.enabledTestPlanIds.toSet()
    val autoRunId = config.autoRunPlanId
    val enabledCount = entries.count { it.plan.id in enabledIds }
    val autoRunCount = if (autoRunId != null && entries.any { it.plan.id == autoRunId }) 1 else 0

    Column(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionCard(
            icon = Icons.Default.Folder,
            title = "Custom plan directory",
            subtitle = "Drop *.json files here to make additional test plans available.",
        ) {
            OutlinedTextField(
                value = config.customTestPlanDir,
                onValueChange = { onConfigUpdate(config.copy(customTestPlanDir = it)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Directory path") },
                placeholder = {
                    Text(PlanLoader.resolveDir("").toString())
                },
            )
            Text(
                "Resolved: $resolvedDir",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = {
                    runCatching {
                        if (Desktop.isDesktopSupported() &&
                            java.nio.file.Files.isDirectory(resolvedDir)
                        ) {
                            Desktop.getDesktop().open(resolvedDir.toFile())
                        }
                    }
                }) { Text("Reveal") }
                Button(onClick = { reloadTick += 1 }) { Text("Reload") }
                Text(
                    "${customPlans.size} custom plan(s) loaded",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
            }
        }

        SectionCard(
            icon = Icons.Default.PlaylistAddCheck,
            title = "Available plans",
            subtitle = "Toggle to enable a plan in the runtime Plans tab. Pick one to auto-run on connect.",
        ) {
            if (entries.isEmpty()) {
                Text(
                    "No plans available.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(entries, key = { it.plan.id + if (it.builtIn) "#b" else "#c" }) { entry ->
                        PlanRow(
                            entry = entry,
                            enabled = entry.plan.id in enabledIds,
                            autoRun = autoRunId == entry.plan.id,
                            onEnableToggle = { newEnabled ->
                                val newIds = if (newEnabled) {
                                    (config.enabledTestPlanIds + entry.plan.id).distinct()
                                } else {
                                    config.enabledTestPlanIds.filterNot { it == entry.plan.id }
                                }
                                val clearedAutoRun =
                                    if (!newEnabled && config.autoRunPlanId == entry.plan.id) null
                                    else config.autoRunPlanId
                                onConfigUpdate(
                                    config.copy(
                                        enabledTestPlanIds = newIds,
                                        autoRunPlanId = clearedAutoRun,
                                    )
                                )
                            },
                            onAutoRunSelect = {
                                onConfigUpdate(config.copy(autoRunPlanId = entry.plan.id))
                            },
                        )
                    }
                }
                Spacer(Modifier.width(0.dp))
                OutlinedButton(
                    onClick = { onConfigUpdate(config.copy(autoRunPlanId = null)) },
                    enabled = autoRunId != null,
                ) { Text("Clear auto-run") }
            }
        }

        SectionCard(icon = Icons.Default.Insights, title = "Summary", subtitle = null) {
            Text(
                "$enabledCount plans enabled, $autoRunCount will auto-run",
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Enabled plans appear in the runtime Plans tab. Auto-run plan executes automatically after the first successful Connect; useful for regression.",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun PlanRow(
    entry: PlanEntry,
    enabled: Boolean,
    autoRun: Boolean,
    onEnableToggle: (Boolean) -> Unit,
    onAutoRunSelect: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp,
        shape = RoundedCornerShape(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.plan.name,
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(8.dp))
                    SourceBadge(builtIn = entry.builtIn)
                }
                Text(
                    "${entry.plan.scheme.name} - ${entry.plan.cases.size} case(s)",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                )
                Text(
                    entry.plan.id,
                    style = MaterialTheme.typography.caption.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Auto",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
                RadioButton(
                    selected = autoRun,
                    onClick = onAutoRunSelect,
                    enabled = enabled,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Enable",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnableToggle,
                )
            }
        }
    }
}

@Composable
private fun SourceBadge(builtIn: Boolean) {
    val (label, bg) = if (builtIn) {
        "built-in" to MaterialTheme.colors.primary.copy(alpha = 0.12f)
    } else {
        "custom" to MaterialTheme.colors.secondary.copy(alpha = 0.18f)
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(4.dp),
    ) {
        Box(Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.85f),
            )
        }
    }
}
