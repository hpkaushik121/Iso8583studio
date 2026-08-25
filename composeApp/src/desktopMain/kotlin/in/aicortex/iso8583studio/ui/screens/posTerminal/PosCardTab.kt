package `in`.aicortex.iso8583studio.ui.screens.posTerminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.ProfileStore
import `in`.aicortex.iso8583studio.domain.store.JsonDirStore
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.DeviceFeature
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.ResolvedTerminal
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.pos.POSSimulatorConfig

/**
 * Card presence and source.
 *
 * The controls are inert until the device bridge lands (M4) — there is nothing to present a card
 * *to* yet — but the source, profile and slots shown here are the real resolved configuration.
 */
@Composable
fun PosCardTab(config: POSSimulatorConfig, resolved: ResolvedTerminal?) {
    val profile = remember(config.activeProfileId) {
        runCatching {
            ProfileStore(JsonDirStore.appDir("card-profiles")).find(config.activeProfileId)
        }.getOrNull()
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Card(elevation = 1.dp, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    "CARD SOURCE",
                    style = MaterialTheme.typography.overline,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary,
                )
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        "Mode",
                        Modifier.fillMaxWidth(0.3f),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    )
                    Text(
                        when (config.transportMode) {
                            "PCSC" -> "Real card via USB reader${config.pcscReaderName.let { if (it.isBlank()) "" else " — $it" }}"
                            "SERIAL" -> "STM32 card emulator on ${config.serialPortName.ifBlank { "(no port set)" }}"
                            else -> "Software card"
                        },
                        style = MaterialTheme.typography.caption,
                    )
                }
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        "Profile",
                        Modifier.fillMaxWidth(0.3f),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    )
                    Text(
                        profile?.let { "${it.name} (${it.scheme.name})" }
                            ?: config.activeProfileId.ifBlank { "None selected" },
                        style = MaterialTheme.typography.caption,
                    )
                }
                profile?.let {
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            "ATR",
                            Modifier.fillMaxWidth(0.3f),
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        )
                        Text(
                            it.atr,
                            style = MaterialTheme.typography.caption.copy(fontFamily = FontFamily.Monospace),
                        )
                    }
                }
            }
        }

        Card(elevation = 1.dp, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    "PRESENTATION",
                    style = MaterialTheme.typography.overline,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val features = resolved?.terminal?.features ?: emptySet()
                    if (DeviceFeature.ICC in features) {
                        OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.height(32.dp)) {
                            Text("Insert", style = MaterialTheme.typography.caption)
                        }
                        OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.height(32.dp)) {
                            Text("Remove", style = MaterialTheme.typography.caption)
                        }
                    }
                    if (DeviceFeature.PICC in features) {
                        OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.height(32.dp)) {
                            Text("Tap", style = MaterialTheme.typography.caption)
                        }
                    }
                    if (DeviceFeature.MSR in features) {
                        OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.height(32.dp)) {
                            Text("Swipe", style = MaterialTheme.typography.caption)
                        }
                    }
                }
                Text(
                    "Card presentation needs the device bridge to relay APDUs into the emulator. " +
                        "Arrives with M4.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}
