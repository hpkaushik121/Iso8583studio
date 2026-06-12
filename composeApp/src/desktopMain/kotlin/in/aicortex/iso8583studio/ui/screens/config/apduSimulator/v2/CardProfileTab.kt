package `in`.aicortex.iso8583studio.ui.screens.config.apduSimulator.v2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.FileCopy
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.CardProfile
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.ProfileStore
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.samples.SampleProfiles
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.apdu.APDUSimulatorConfig
import `in`.aicortex.iso8583studio.ui.screens.apduSimulator.v2.ProfileEditorScreen
import `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

/**
 * Card Profile tab. Picks the active CardProfile, shows its key contents, and lets the user open
 * a full editor or quick-personalize a clone (PAN / expiry / cardholder) to spawn a new profile
 * without leaving the config screen.
 */
@Composable
fun CardProfileTab(config: APDUSimulatorConfig, onConfigUpdate: (APDUSimulatorConfig) -> Unit) {
    val store = remember {
        val dir = Paths.get(System.getProperty("user.home"), ".iso8583studio", "card-profiles")
        Files.createDirectories(dir)
        ProfileStore(dir).also { s -> if (s.list().isEmpty()) SampleProfiles.all().forEach(s::save) }
    }
    var profiles by remember { mutableStateOf<List<CardProfile>>(emptyList()) }
    LaunchedEffect(Unit) { profiles = store.list() }
    val active = profiles.firstOrNull { it.id == config.activeProfileId }

    var showEditor by remember { mutableStateOf<CardProfile?>(null) }
    var showPersonalize by remember { mutableStateOf<CardProfile?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionCard(
            icon = Icons.Default.CreditCard,
            title = "Active card profile",
            subtitle = when (config.transportMode) {
                "PCSC" -> "Informational only — PC/SC reads whatever physical card is inserted."
                else -> "The simulator emulates this profile when running."
            },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PickerDropdown(
                    label = "Profile",
                    value = active?.let { "${it.name}  (${it.scheme.name})" },
                    options = profiles.map { "${it.name}  (${it.scheme.name})" },
                    onPick = { picked ->
                        val p = profiles.first { "${it.name}  (${it.scheme.name})" == picked }
                        onConfigUpdate(config.copy(activeProfileId = p.id))
                    },
                    empty = "No profiles — click \"New blank\"",
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { profiles = store.list() }) {
                    Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(4.dp)); Text("Refresh")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = active != null,
                    onClick = { showEditor = active },
                ) {
                    Icon(Icons.Default.Edit, null); Spacer(Modifier.width(4.dp)); Text("Edit profile")
                }
                OutlinedButton(
                    enabled = active != null,
                    onClick = { showPersonalize = active },
                ) {
                    Icon(Icons.Default.FileCopy, null); Spacer(Modifier.width(4.dp)); Text("Clone & personalize")
                }
                OutlinedButton(onClick = { showEditor = blankProfile() }) { Text("New blank") }
            }
        }

        if (active != null) {
            SectionCard(
                icon = Icons.Default.FactCheck,
                title = "Profile summary",
                subtitle = "Key fields — full editor available above.",
            ) {
                ProfileSummary(active)
            }
        }
    }

    showEditor?.let { initial ->
        Dialog(
            onDismissRequest = { showEditor = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(0.92f).heightIn(min = 600.dp).padding(24.dp),
                elevation = 8.dp,
                shape = RoundedCornerShape(8.dp),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ProfileEditorScreen(
                        initial = initial,
                        onSave = { saved ->
                            store.save(saved)
                            profiles = store.list()
                            onConfigUpdate(config.copy(activeProfileId = saved.id))
                            showEditor = null
                        },
                        onCancel = { showEditor = null },
                    )
                }
            }
        }
    }

    showPersonalize?.let { src ->
        PersonalizeDialog(
            source = src,
            onDismiss = { showPersonalize = null },
            onConfirm = { newProfile ->
                store.save(newProfile)
                profiles = store.list()
                onConfigUpdate(config.copy(activeProfileId = newProfile.id))
                showPersonalize = null
            },
        )
    }
}

@Composable
private fun ProfileSummary(p: CardProfile) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SummaryRow("Scheme", p.scheme.name)
        SummaryRow("ATR", p.atr)
        p.applications.forEachIndexed { idx, app ->
            Spacer(Modifier.width(0.dp))
            Text(
                "Application ${idx + 1}",
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
            )
            SummaryRow("AID", app.aid)
            SummaryRow("Label", app.label)
            SummaryRow("PAN", app.pan)
            SummaryRow("Expiry", app.expiryYyMmDd)
            SummaryRow("CVN", app.cvn.toString())
            SummaryRow("Records", app.records.size.toString() + " record(s)")
            SummaryRow("Issuer key id", app.issuerKeyId ?: "(none)")
        }
        if (p.keys.isNotEmpty()) {
            Spacer(Modifier.width(0.dp))
            Text(
                "Issuer keys: ${p.keys.size}",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
            )
            p.keys.forEach { k ->
                SummaryRow("  ${k.id}", "${k.kind.name} — UDK ${k.udk.take(8)}…")
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row {
        Text(
            label,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.width(140.dp),
        )
        Text(
            value,
            style = MaterialTheme.typography.caption,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun PersonalizeDialog(
    source: CardProfile,
    onDismiss: () -> Unit,
    onConfirm: (CardProfile) -> Unit,
) {
    val firstApp = source.applications.firstOrNull()
    var newName by remember { mutableStateOf(source.name + " (clone)") }
    var pan by remember { mutableStateOf(firstApp?.pan ?: "") }
    var expiry by remember { mutableStateOf(firstApp?.expiryYyMmDd ?: "") }
    var cardholder by remember { mutableStateOf(firstApp?.cardholderName ?: "") }
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.padding(16.dp), elevation = 8.dp, shape = RoundedCornerShape(8.dp)) {
            Column(modifier = Modifier.padding(20.dp).width(520.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Clone & personalize", style = MaterialTheme.typography.h6, fontWeight = FontWeight.SemiBold)
                Text(
                    "Creates a new profile based on \"${source.name}\". UDK is copied verbatim — for a real personalization run, edit the issuer keys after.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                )
                FixedOutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Profile name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                FixedOutlinedTextField(value = pan, onValueChange = { pan = it.filter { c -> c.isDigit() } }, label = { Text("PAN") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                FixedOutlinedTextField(value = expiry, onValueChange = { expiry = it.filter { c -> c.isDigit() }.take(6) }, label = { Text("Expiry (YYMMDD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                FixedOutlinedTextField(value = cardholder, onValueChange = { cardholder = it }, label = { Text("Cardholder name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.weight(1f))
                    Button(
                        enabled = pan.length in 12..19 && expiry.length == 6 && firstApp != null,
                        onClick = {
                            val newId = "${source.id}-${UUID.randomUUID().toString().take(6)}"
                            val updatedApp = firstApp!!.copy(
                                pan = pan,
                                expiryYyMmDd = expiry,
                                cardholderName = cardholder,
                            )
                            val cloned = source.copy(
                                id = newId,
                                name = newName,
                                applications = listOf(updatedApp) + source.applications.drop(1),
                            )
                            onConfirm(cloned)
                        },
                    ) { Text("Create profile") }
                }
            }
        }
    }
}

private fun blankProfile(): CardProfile = CardProfile(
    id = "new-${UUID.randomUUID().toString().take(6)}",
    name = "New profile",
    scheme = `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.Scheme.VISA,
    atr = "3B6500002063CB6800",
    applications = emptyList(),
    keys = emptyList(),
)
