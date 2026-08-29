package `in`.aicortex.iso8583studio.ui.screens.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import java.awt.Desktop
import java.net.URI

private const val PRIVACY_URL = "https://iso8583.studio/privacy-policy.html"

/**
 * First-run opt-in for usage analytics.
 *
 * Shown once, before anything is transmitted. Declining is a first-class choice: no data
 * leaves the machine, and the answer is remembered so this never asks again.
 */
@Composable
fun AnalyticsConsentDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    DialogWindow(
        onCloseRequest = onDecline,
        state = rememberDialogState(width = 560.dp, height = 520.dp),
        title = "Help improve ISO8583Studio",
        resizable = false,
    ) {
        Surface(color = MaterialTheme.colors.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Insights,
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Share anonymous usage analytics?",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f))

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Knowing which tools and simulators actually get used tells us where to " +
                            "spend our time. This is entirely optional and off unless you turn it on.",
                        style = MaterialTheme.typography.body2,
                    )

                    Text("If you agree, the app reports:", fontWeight = FontWeight.Medium)
                    BulletList(
                        "Which tools and simulators you open, and how long you use them",
                        "Whether a calculation succeeded or failed, and how long it took",
                        "App version, operating system, CPU architecture, Java version, " +
                            "language, timezone and screen size",
                        "Approximate location (city, region and country). Your IP address is " +
                            "used to work this out and is not stored as an analytics attribute; " +
                            "GPS coordinates are never collected.",
                        "A random device identifier, created on this machine and resettable " +
                            "at any time from Settings",
                        "The type name of an unexpected error, without its message or stack trace",
                    )

                    Card(
                        backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.08f),
                        elevation = 0.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colors.error,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Never collected", fontWeight = FontWeight.Medium)
                                Text(
                                    "Card numbers, PINs or PIN blocks, cryptographic keys or key " +
                                        "components, cryptograms, MACs, the contents of any ISO 8583 " +
                                        "or HSM message, file paths, hostnames, the addresses of " +
                                        "systems you connect to, or the names of your profiles. " +
                                        "None of that ever leaves this machine.",
                                    style = MaterialTheme.typography.caption,
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            runCatching { Desktop.getDesktop().browse(URI(PRIVACY_URL)) }
                        },
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text("Read the privacy policy", style = MaterialTheme.typography.caption)
                    }
                }

                Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    OutlinedButton(onClick = onDecline) {
                        Text("No thanks")
                    }
                    Button(onClick = onAccept) {
                        Text("Enable analytics")
                    }
                }
            }
        }
    }
}

@Composable
private fun BulletList(vararg items: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { item ->
            Row {
                Text("•", modifier = Modifier.width(16.dp), style = MaterialTheme.typography.body2)
                Text(item, style = MaterialTheme.typography.body2)
            }
        }
    }
}
