package `in`.aicortex.iso8583studio.ui.screens.config.apduSimulator.v2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.CardProfile
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.ProfileStore
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.samples.SampleProfiles
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.apdu.APDUSimulatorConfig
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Profile tab — selects which CardProfile the simulator emulates. Only meaningful in LOOPBACK and
 * SERIAL modes; in PCSC mode the card is whatever's in the reader.
 */
@Composable
fun ProfileTab(config: APDUSimulatorConfig, onConfigUpdate: (APDUSimulatorConfig) -> Unit) {
    val store = remember {
        val dir = Paths.get(System.getProperty("user.home"), ".iso8583studio", "card-profiles")
        Files.createDirectories(dir)
        ProfileStore(dir).also { s -> if (s.list().isEmpty()) SampleProfiles.all().forEach(s::save) }
    }
    var profiles by remember { mutableStateOf<List<CardProfile>>(emptyList()) }
    LaunchedEffect(Unit) { profiles = store.list() }

    Column(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionCard(
            title = "Active card profile",
            subtitle = when (config.transportMode) {
                "PCSC" -> "Informational only — PC/SC reads whatever physical card is inserted."
                else -> "The simulator emulates this profile. Manage profiles from the running simulator's Profiles tab."
            },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val selected = profiles.firstOrNull { it.id == config.activeProfileId }
                PickerDropdown(
                    label = "Profile",
                    value = selected?.let { "${it.name}  (${it.scheme.name})" },
                    options = profiles.map { "${it.name}  (${it.scheme.name})" },
                    onPick = { picked ->
                        val p = profiles.first { "${it.name}  (${it.scheme.name})" == picked }
                        onConfigUpdate(config.copy(activeProfileId = p.id))
                    },
                    empty = "No profiles — open the simulator and use the Profiles tab",
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { profiles = store.list() }) {
                    Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(4.dp)); Text("Refresh")
                }
            }
            val selected = profiles.firstOrNull { it.id == config.activeProfileId }
            selected?.let { p ->
                Text(
                    "AIDs: " + p.applications.joinToString { "${it.aid} (${it.label})" },
                    style = MaterialTheme.typography.caption,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                )
                Text(
                    "ATR: ${p.atr}",
                    style = MaterialTheme.typography.caption,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                )
            }
        }
    }
}
