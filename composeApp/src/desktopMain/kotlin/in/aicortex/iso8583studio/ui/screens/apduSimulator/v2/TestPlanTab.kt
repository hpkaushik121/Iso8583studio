package `in`.aicortex.iso8583studio.ui.screens.apduSimulator.v2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine.RunResult
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine.Runner
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine.TestPlan
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine.plans.BuiltInPlans
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine.reporters.SchemeReporter
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/**
 * Test plan runner: pick a built-in plan, run it through the currently connected transport, see
 * pass/fail per case, export the report in the scheme's preferred format.
 */
@Composable
fun TestPlanTab(controller: SimulatorController) {
    val plans = remember { BuiltInPlans.all() }
    var selected by remember { mutableStateOf(plans.first()) }
    var running by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<RunResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionCard(title = "Plan", subtitle = "Pick a plan to run against the connected transport.") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PickerDropdown(
                    label = "Plan",
                    value = "${selected.scheme}: ${selected.name}",
                    options = plans.map { "${it.scheme}: ${it.name}" },
                    onPick = { picked -> selected = plans.first { "${it.scheme}: ${it.name}" == picked } },
                    empty = "—",
                    modifier = Modifier.weight(1f),
                    enabled = !running,
                )
            }
            Text(
                "${selected.cases.size} cases — id: ${selected.id}",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
            )
        }

        SectionCard(title = "Run", subtitle = null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = !running && controller.conn == ConnState.CONNECTED,
                    onClick = {
                        scope.launch {
                            running = true
                            error = null
                            runCatching {
                                val transport = controller.transport ?: error("not connected")
                                Runner(transport).run(selected)
                            }.onSuccess { lastResult = it }
                                .onFailure { error = it.message ?: "run failed" }
                            running = false
                        }
                    },
                ) {
                    if (running) CircularProgressIndicator(modifier = Modifier.width(16.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(4.dp))
                    Text(if (running) "Running…" else "Run plan")
                }
                OutlinedButton(
                    enabled = lastResult != null && !running,
                    onClick = {
                        val r = lastResult ?: return@OutlinedButton
                        val report = SchemeReporter.render(r)
                        saveToFile(suggestedName(r), report)?.let { /* saved */ }
                    },
                ) {
                    Icon(Icons.Default.Save, null); Spacer(Modifier.width(4.dp)); Text("Export report")
                }
                Spacer(Modifier.weight(1f))
                if (controller.conn != ConnState.CONNECTED) {
                    Text(
                        "Connect a transport first.",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
            error?.let {
                Text(it, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.error)
            }
        }

        Card(modifier = Modifier.fillMaxWidth().weight(1f), elevation = 2.dp, shape = RoundedCornerShape(8.dp)) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                val r = lastResult
                if (r == null) {
                    Text(
                        "No run yet.",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    )
                } else {
                    Text(
                        "Result: ${r.passed}/${r.cases.size} passed",
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.SemiBold,
                        color = if (r.failed == 0) MaterialTheme.colors.primary else MaterialTheme.colors.error,
                    )
                    Spacer(Modifier.width(0.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(r.cases) { c ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = 1.dp,
                                shape = RoundedCornerShape(6.dp),
                            ) {
                                Column(Modifier.padding(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (c.passed) Icons.Default.Check else Icons.Default.Close,
                                            null,
                                            tint = if (c.passed) MaterialTheme.colors.primary else MaterialTheme.colors.error,
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "${c.case.id} — ${c.case.name}",
                                            style = MaterialTheme.typography.body2,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Text(
                                            "${c.durationMs} ms",
                                            style = MaterialTheme.typography.caption,
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                        )
                                    }
                                    if (!c.passed) c.failureMessage?.let {
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.caption,
                                            color = MaterialTheme.colors.error,
                                            fontFamily = FontFamily.Monospace,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun suggestedName(r: RunResult): String {
    val ext = when (r.plan.scheme.name) {
        "MASTERCARD" -> "xml"
        "AMEX" -> "csv"
        "VISA" -> "txt"
        else -> "xml"
    }
    return "${r.plan.id}-report.$ext"
}

/**
 * Native Save dialog using AWT FileDialog (Compose Desktop friendly). Returns the saved file or
 * null if the user cancelled.
 */
private fun saveToFile(suggested: String, content: String): File? {
    val dlg = FileDialog(null as Frame?, "Save report", FileDialog.SAVE)
    dlg.file = suggested
    dlg.isVisible = true
    val name = dlg.file ?: return null
    val dir = dlg.directory ?: return null
    val out = File(dir, name)
    out.writeText(content)
    return out
}

/** Forwarded to TestPlan-aware places that don't import Plan directly. */
@Suppress("unused")
private fun unusedReference(p: TestPlan) = p
