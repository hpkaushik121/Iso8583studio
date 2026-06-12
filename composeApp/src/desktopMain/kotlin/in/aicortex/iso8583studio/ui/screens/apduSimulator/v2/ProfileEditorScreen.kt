package `in`.aicortex.iso8583studio.ui.screens.apduSimulator.v2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.AppRecord
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.CardApplication
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.CardProfile
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.IssuerKey
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.KeyKind
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.Scheme
import `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField

/**
 * Mutable working copy of [CardApplication] used by the editor — primitive and string fields are
 * unwrapped so each Compose state slot can drive a single field independently.
 */
private class ApplicationState(src: CardApplication) {
    var aid by mutableStateOf(src.aid)
    var label by mutableStateOf(src.label)
    var priority by mutableStateOf(src.priority.toString())
    var aip by mutableStateOf(src.aip)
    var afl by mutableStateOf(src.afl)
    var pdol by mutableStateOf(src.pdol)
    var cdol1 by mutableStateOf(src.cdol1)
    var cdol2 by mutableStateOf(src.cdol2)
    var pan by mutableStateOf(src.pan)
    var panSequenceNumber by mutableStateOf(src.panSequenceNumber.toString())
    var expiryYyMmDd by mutableStateOf(src.expiryYyMmDd)
    var track2Equivalent by mutableStateOf(src.track2Equivalent)
    var cardholderName by mutableStateOf(src.cardholderName)
    var issuerKeyId by mutableStateOf<String?>(src.issuerKeyId)
    var cvn by mutableStateOf(src.cvn.toString())
    var atcStart by mutableStateOf(src.atcStart.toString())
    var pinTryLimit by mutableStateOf(src.pinTryLimit.toString())
    val records: MutableList<RecordState> = mutableStateListOf<RecordState>().also {
        src.records.forEach { r -> it.add(RecordState(r)) }
    }
    var expanded by mutableStateOf(true)

    fun toModel(): CardApplication = CardApplication(
        aid = aid,
        label = label,
        priority = priority.toIntOrNull() ?: 1,
        aip = aip,
        afl = afl,
        records = records.map { it.toModel() },
        pdol = pdol,
        cdol1 = cdol1,
        cdol2 = cdol2,
        pan = pan,
        panSequenceNumber = panSequenceNumber.toIntOrNull() ?: 0,
        expiryYyMmDd = expiryYyMmDd,
        track2Equivalent = track2Equivalent,
        cardholderName = cardholderName,
        issuerKeyId = issuerKeyId?.takeIf { it.isNotBlank() },
        cvn = cvn.toIntOrNull() ?: 18,
        atcStart = atcStart.toIntOrNull() ?: 0,
        pinTryLimit = pinTryLimit.toIntOrNull() ?: 3,
    )
}

private class RecordState(src: AppRecord) {
    var sfi by mutableStateOf(src.sfi.toString())
    var record by mutableStateOf(src.record.toString())
    var tlvHex by mutableStateOf(src.tlvHex)

    fun toModel() = AppRecord(
        sfi = sfi.toIntOrNull() ?: 0,
        record = record.toIntOrNull() ?: 0,
        tlvHex = tlvHex,
    )
}

private class IssuerKeyState(src: IssuerKey) {
    var id by mutableStateOf(src.id)
    var kind by mutableStateOf(src.kind)
    var imk by mutableStateOf(src.imk)
    var udk by mutableStateOf(src.udk)
    var issuerRsaModulus by mutableStateOf(src.issuerRsaModulus)
    var issuerRsaPrivExponent by mutableStateOf(src.issuerRsaPrivExponent)
    var issuerRsaPubExponent by mutableStateOf(src.issuerRsaPubExponent)

    fun toModel() = IssuerKey(
        id = id,
        kind = kind,
        imk = imk,
        udk = udk,
        issuerRsaModulus = issuerRsaModulus,
        issuerRsaPrivExponent = issuerRsaPrivExponent,
        issuerRsaPubExponent = issuerRsaPubExponent,
    )
}

private fun emptyApplication() = CardApplication(
    aid = "",
    label = "",
    aip = "",
    afl = "",
    records = emptyList(),
    pan = "",
    expiryYyMmDd = "",
    track2Equivalent = "",
)

private fun emptyIssuerKey() = IssuerKey(id = "key-${System.currentTimeMillis()}", kind = KeyKind.TDES_AC)

@Composable
fun ProfileEditorScreen(
    initial: CardProfile?,
    onSave: (CardProfile) -> Unit,
    onCancel: () -> Unit,
) {
    val seed = initial ?: CardProfile(
        id = "new-profile",
        name = "",
        scheme = Scheme.VISA,
        atr = "",
        applications = listOf(emptyApplication()),
        keys = emptyList(),
        notes = "",
    )

    var profileId by remember { mutableStateOf(seed.id) }
    var name by remember { mutableStateOf(seed.name) }
    var scheme by remember { mutableStateOf(seed.scheme) }
    var atr by remember { mutableStateOf(seed.atr) }
    var notes by remember { mutableStateOf(seed.notes) }

    val applications = remember {
        mutableStateListOf<ApplicationState>().also { list ->
            seed.applications.forEach { list.add(ApplicationState(it)) }
        }
    }
    val keys = remember {
        mutableStateListOf<IssuerKeyState>().also { list ->
            seed.keys.forEach { list.add(IssuerKeyState(it)) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HeaderCard(
                profileId = profileId,
                onProfileIdChange = { profileId = it },
                name = name,
                onNameChange = { name = it },
                scheme = scheme,
                onSchemeChange = { scheme = it },
                atr = atr,
                onAtrChange = { atr = it },
                notes = notes,
                onNotesChange = { notes = it },
            )

            ApplicationsSection(
                applications = applications,
                keys = keys,
                onAdd = { applications.add(ApplicationState(emptyApplication())) },
                onRemove = { applications.remove(it) },
            )

            IssuerKeysSection(
                keys = keys,
                onAdd = { keys.add(IssuerKeyState(emptyIssuerKey())) },
                onRemove = { keys.remove(it) },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        onSave(
                            CardProfile(
                                id = profileId,
                                name = name,
                                scheme = scheme,
                                atr = atr,
                                applications = applications.map { it.toModel() },
                                keys = keys.map { it.toModel() },
                                notes = notes,
                            )
                        )
                    }
                ) { Text("Save") }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                trailing?.invoke()
            }
            content()
        }
    }
}

@Composable
private fun HeaderCard(
    profileId: String,
    onProfileIdChange: (String) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    scheme: Scheme,
    onSchemeChange: (Scheme) -> Unit,
    atr: String,
    onAtrChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
) {
    SectionCard(title = "Profile") {
        FixedOutlinedTextField(
            value = profileId,
            onValueChange = onProfileIdChange,
            label = { Text("ID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        FixedOutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        SchemeDropdown(scheme = scheme, onSchemeChange = onSchemeChange)
        FixedOutlinedTextField(
            value = atr,
            onValueChange = onAtrChange,
            label = { Text("ATR (hex)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        FixedOutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Notes") },
            singleLine = false,
            minLines = 3,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SchemeDropdown(scheme: Scheme, onSchemeChange: (Scheme) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(
            text = "Scheme",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
        )
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(scheme.name, modifier = Modifier.weight(1f))
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 300.dp),
            ) {
                Scheme.values().forEach { value ->
                    DropdownMenuItem(onClick = {
                        onSchemeChange(value)
                        expanded = false
                    }) { Text(value.name) }
                }
            }
        }
    }
}

@Composable
private fun ApplicationsSection(
    applications: MutableList<ApplicationState>,
    keys: MutableList<IssuerKeyState>,
    onAdd: () -> Unit,
    onRemove: (ApplicationState) -> Unit,
) {
    SectionCard(
        title = "Applications (${applications.size})",
        trailing = {
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Add application")
            }
        },
    ) {
        if (applications.isEmpty()) {
            Text(
                text = "No applications. Click + to add one.",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            )
        } else {
            applications.forEachIndexed { index, app ->
                ApplicationCard(
                    index = index,
                    app = app,
                    keys = keys,
                    onRemove = { onRemove(app) },
                )
            }
        }
    }
}

@Composable
private fun ApplicationCard(
    index: Int,
    app: ApplicationState,
    keys: MutableList<IssuerKeyState>,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { app.expanded = !app.expanded }) {
                    Icon(
                        imageVector = if (app.expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                    )
                }
                Text(
                    text = "App #${index + 1}${if (app.label.isNotBlank()) " — ${app.label}" else ""}",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove application")
                }
            }

            if (app.expanded) {
                FixedOutlinedTextField(
                    value = app.aid, onValueChange = { app.aid = it },
                    label = { Text("AID (hex)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FixedOutlinedTextField(
                    value = app.label, onValueChange = { app.label = it },
                    label = { Text("Label") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FixedOutlinedTextField(
                        value = app.priority, onValueChange = { app.priority = it },
                        label = { Text("Priority") }, singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    FixedOutlinedTextField(
                        value = app.cvn, onValueChange = { app.cvn = it },
                        label = { Text("CVN") }, singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    FixedOutlinedTextField(
                        value = app.atcStart, onValueChange = { app.atcStart = it },
                        label = { Text("ATC Start") }, singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    FixedOutlinedTextField(
                        value = app.pinTryLimit, onValueChange = { app.pinTryLimit = it },
                        label = { Text("PIN Try Limit") }, singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                FixedOutlinedTextField(
                    value = app.aip, onValueChange = { app.aip = it },
                    label = { Text("AIP (hex)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FixedOutlinedTextField(
                    value = app.afl, onValueChange = { app.afl = it },
                    label = { Text("AFL (hex)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FixedOutlinedTextField(
                        value = app.pan, onValueChange = { app.pan = it },
                        label = { Text("PAN") }, singleLine = true,
                        modifier = Modifier.weight(2f),
                    )
                    FixedOutlinedTextField(
                        value = app.panSequenceNumber, onValueChange = { app.panSequenceNumber = it },
                        label = { Text("PAN Seq #") }, singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FixedOutlinedTextField(
                        value = app.expiryYyMmDd, onValueChange = { app.expiryYyMmDd = it },
                        label = { Text("Expiry YYMMDD") }, singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    FixedOutlinedTextField(
                        value = app.cardholderName, onValueChange = { app.cardholderName = it },
                        label = { Text("Cardholder Name") }, singleLine = true,
                        modifier = Modifier.weight(2f),
                    )
                }
                FixedOutlinedTextField(
                    value = app.track2Equivalent, onValueChange = { app.track2Equivalent = it },
                    label = { Text("Track 2 Equivalent (hex)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FixedOutlinedTextField(
                    value = app.pdol, onValueChange = { app.pdol = it },
                    label = { Text("PDOL (hex)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FixedOutlinedTextField(
                    value = app.cdol1, onValueChange = { app.cdol1 = it },
                    label = { Text("CDOL1 (hex)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FixedOutlinedTextField(
                    value = app.cdol2, onValueChange = { app.cdol2 = it },
                    label = { Text("CDOL2 (hex)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                IssuerKeyDropdown(
                    selectedId = app.issuerKeyId,
                    keys = keys,
                    onSelect = { app.issuerKeyId = it },
                )

                RecordsList(records = app.records)
            }
        }
    }
}

@Composable
private fun IssuerKeyDropdown(
    selectedId: String?,
    keys: List<IssuerKeyState>,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(
            text = "Issuer Key",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
        )
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = selectedId ?: "(none)",
                    modifier = Modifier.weight(1f),
                )
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 300.dp),
            ) {
                DropdownMenuItem(onClick = {
                    onSelect(null)
                    expanded = false
                }) { Text("(none)") }
                keys.forEach { k ->
                    DropdownMenuItem(onClick = {
                        onSelect(k.id)
                        expanded = false
                    }) {
                        Text(text = "${k.id} [${k.kind.name}]")
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordsList(records: MutableList<RecordState>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Records (${records.size})",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = {
                records.add(RecordState(AppRecord(sfi = 1, record = 1, tlvHex = "")))
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add record")
            }
        }
        records.forEachIndexed { i, rec ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FixedOutlinedTextField(
                    value = rec.sfi, onValueChange = { rec.sfi = it },
                    label = { Text("SFI") }, singleLine = true,
                    modifier = Modifier.width(80.dp),
                )
                FixedOutlinedTextField(
                    value = rec.record, onValueChange = { rec.record = it },
                    label = { Text("Rec") }, singleLine = true,
                    modifier = Modifier.width(80.dp),
                )
                FixedOutlinedTextField(
                    value = rec.tlvHex, onValueChange = { rec.tlvHex = it },
                    label = { Text("TLV (hex)") }, singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { records.remove(rec) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove record")
                }
            }
        }
    }
}

@Composable
private fun IssuerKeysSection(
    keys: MutableList<IssuerKeyState>,
    onAdd: () -> Unit,
    onRemove: (IssuerKeyState) -> Unit,
) {
    SectionCard(
        title = "Issuer Keys (${keys.size})",
        trailing = {
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Add key")
            }
        },
    ) {
        if (keys.isEmpty()) {
            Text(
                text = "No issuer keys. Click + to add one.",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            )
        } else {
            keys.forEachIndexed { index, key ->
                IssuerKeyCard(index = index, key = key, onRemove = { onRemove(key) })
            }
        }
    }
}

@Composable
private fun IssuerKeyCard(index: Int, key: IssuerKeyState, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = 1.dp) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Key #${index + 1}",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove key")
                }
            }
            FixedOutlinedTextField(
                value = key.id, onValueChange = { key.id = it },
                label = { Text("ID") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            KeyKindDropdown(kind = key.kind, onChange = { key.kind = it })
            FixedOutlinedTextField(
                value = key.imk, onValueChange = { key.imk = it },
                label = { Text("IMK (hex)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            FixedOutlinedTextField(
                value = key.udk, onValueChange = { key.udk = it },
                label = { Text("UDK (hex)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            FixedOutlinedTextField(
                value = key.issuerRsaModulus, onValueChange = { key.issuerRsaModulus = it },
                label = { Text("Issuer RSA Modulus (hex)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            FixedOutlinedTextField(
                value = key.issuerRsaPrivExponent, onValueChange = { key.issuerRsaPrivExponent = it },
                label = { Text("Issuer RSA Priv Exponent (hex)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            FixedOutlinedTextField(
                value = key.issuerRsaPubExponent, onValueChange = { key.issuerRsaPubExponent = it },
                label = { Text("Issuer RSA Pub Exponent (hex)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun KeyKindDropdown(kind: KeyKind, onChange: (KeyKind) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(
            text = "Kind",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
        )
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(kind.name, modifier = Modifier.weight(1f))
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                KeyKind.values().forEach { value ->
                    DropdownMenuItem(onClick = {
                        onChange(value)
                        expanded = false
                    }) { Text(value.name) }
                }
            }
        }
    }
}
