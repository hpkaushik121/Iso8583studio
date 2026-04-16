package `in`.aicortex.iso8583studio.ui.screens.hostSimulator

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import `in`.aicortex.iso8583studio.data.BitAttribute
import `in`.aicortex.iso8583studio.data.Iso8583Data
import `in`.aicortex.iso8583studio.domain.service.hostSimulatorService.HostSimulator
import `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField
import ai.cortex.core.IsoUtil

/**
 * Sample ISO8583 message used by the editor dialog and Send Message tab defaults.
 */
internal fun createDefaultIso8583EditorMessage(gw: HostSimulator, isFirst: Boolean): Iso8583Data {
    return Iso8583Data(gw.configuration, isFirst).apply {
        messageType = "0200"
        tpduHeader.rawTPDU = IsoUtil.stringToBcd("6000000000", 5)
        bitAttributes.forEach { it.isSet = false }
        packBit(3, "000000")
        packBit(4, "000009999000")
        packBit(11, "000077")
        packBit(12, "111412")
        packBit(13, "0204")
        packBit(22, "0022")
        packBit(25, "00")
        packBit(35, "4541822000289640D100220120389421000000")
        packBit(41, "11111111")
        packBit(42, "111111112345678")
        packBit(60, "000003")
        packBit(62, "000023")
        createBitmap()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dialog
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Main composable for ISO8583 message editor dialog.
 */
@Composable
fun Iso8583EditorDialog(
    gw: HostSimulator,
    initialMessage: Iso8583Data? = null,
    onDismiss: () -> Unit,
    onConfirm: (Iso8583Data) -> Unit,
    isFirst: Boolean
) {
    val message = remember {
        mutableStateOf(initialMessage ?: createDefaultIso8583EditorMessage(gw, isFirst))
    }
    val bitAttributes = remember { mutableStateOf(message.value.bitAttributes.clone()) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.94f)
                    .widthIn(max = 960.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colors.surface,
                elevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // Title bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colors.primary)
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "ISO 8583 Message Editor",
                                style = MaterialTheme.typography.h6,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Scrollable body
                    val composerScroll = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(composerScroll)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Iso8583Header(
                            gw = gw,
                            message = message.value,
                            externalHeaderSync = 0,
                            onMessageTypeChanged = { message.value.messageType = it },
                            onTpduChanged = {
                                message.value.tpduHeader.rawTPDU =
                                    IsoUtil.stringToBcd(it, it.length / 2)
                            },
                            onUseSmartlinkChanged = {}
                        )

                        Iso8583FieldsEditor(
                            message = message.value,
                            bitAttributes = bitAttributes,
                            onFieldChanged = { index, newValue ->
                                message.value.packBit(index + 1, newValue)
                                bitAttributes.value = message.value.bitAttributes.clone()
                            },
                            onFieldAdded = { bitNumber, value ->
                                message.value.packBit(bitNumber, value)
                                bitAttributes.value = message.value.bitAttributes.clone()
                            },
                            onFieldRemoved = { index ->
                                message.value.bitAttributes[index].isSet = false
                                bitAttributes.value = message.value.bitAttributes.clone()
                            },
                            nestedLazyList = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Action bar
                    Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                        }
                        Button(
                            onClick = {
                                message.value.bitAttributes = bitAttributes.value
                                onConfirm(message.value)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Apply Message")
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Header section with TPDU and MTI. Local text state allows smooth typing.
 * Increment [externalHeaderSync] when the parent replaces the whole message.
 */
@Composable
internal fun Iso8583Header(
    gw: HostSimulator,
    message: Iso8583Data,
    externalHeaderSync: Int,
    onMessageTypeChanged: (String) -> Unit,
    onTpduChanged: (String) -> Unit,
    onUseSmartlinkChanged: (Boolean) -> Unit
) {
    var tpdu by remember { mutableStateOf(IsoUtil.bcdToString(message.tpduHeader.rawTPDU)) }
    var messageType by remember { mutableStateOf(message.messageType) }

    LaunchedEffect(externalHeaderSync) {
        tpdu = IsoUtil.bcdToString(message.tpduHeader.rawTPDU)
        messageType = message.messageType
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colors.primary.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, MaterialTheme.colors.primary.copy(alpha = 0.15f)),
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(
                text = "MESSAGE HEADER",
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!gw.configuration.doNotUseHeaderDest) {
                    FixedOutlinedTextField(
                        value = tpdu,
                        onValueChange = {
                            if (it.length <= 10) {
                                tpdu = it
                                onTpduChanged(it)
                            }
                        },
                        label = { Text("TPDU", style = MaterialTheme.typography.caption) },
                        modifier = Modifier.width(160.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colors.primary,
                            unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.25f),
                            focusedLabelColor = MaterialTheme.colors.primary
                        )
                    )
                }
                FixedOutlinedTextField(
                    value = messageType,
                    onValueChange = {
                        if (it.length <= 4) {
                            messageType = it
                            onMessageTypeChanged(it)
                        }
                    },
                    label = { Text("MTI", style = MaterialTheme.typography.caption) },
                    modifier = Modifier.width(130.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colors.primary,
                        unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.25f),
                        focusedLabelColor = MaterialTheme.colors.primary
                    )
                )
                if (messageType.length == 4) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colors.secondary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = mtiDescription(messageType),
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.secondary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Returns a short human-readable label for common MTI values. */
private fun mtiDescription(mti: String): String = when (mti) {
    "0100" -> "Auth Request"
    "0110" -> "Auth Response"
    "0200" -> "Financial Request"
    "0210" -> "Financial Response"
    "0400" -> "Reversal Request"
    "0410" -> "Reversal Response"
    "0420" -> "Reversal Advice"
    "0430" -> "Reversal Advice Response"
    "0800" -> "Network Mgmt Request"
    "0810" -> "Network Mgmt Response"
    else -> "MTI $mti"
}

// ─────────────────────────────────────────────────────────────────────────────
// Fields editor
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Fields editor: table of set fields (also used inline on Send Message tab).
 *
 * @param nestedLazyList If true (Send Message tab), uses a [LazyColumn] inside a weighted [Box].
 *   If false ([Iso8583EditorDialog]), renders plain rows so the parent column can own scroll.
 */
@Composable
internal fun Iso8583FieldsEditor(
    message: Iso8583Data,
    bitAttributes: MutableState<Array<BitAttribute>>,
    onFieldChanged: (Int, String) -> Unit,
    onFieldAdded: (Int, String) -> Unit,
    onFieldRemoved: (Int) -> Unit,
    modifier: Modifier = Modifier,
    nestedLazyList: Boolean = true,
    externalSyncKey: Int = 0
) {
    var showAddDialog by remember { mutableStateOf(false) }

    val rootModifier = if (nestedLazyList) modifier.fillMaxSize() else modifier.fillMaxWidth()

    Surface(
        modifier = rootModifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colors.surface,
        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.1f)),
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // Table header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.primary.copy(alpha = 0.08f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DE",
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.width(52.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Field Name",
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.width(180.dp)
                )
                Text(
                    text = "Value",
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(40.dp))
            }

            Divider(color = MaterialTheme.colors.primary.copy(alpha = 0.12f))

            if (nestedLazyList) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(
                            bitAttributes.value.toList(),
                            key = { index, _ -> index }
                        ) { index, attr ->
                            if (attr.isSet) {
                                Iso8583FieldRow(
                                    bitNumber = index + 1,
                                    bitAttribute = attr,
                                    value = message.getValue(index) ?: "",
                                    onValueChanged = { newValue -> onFieldChanged(index, newValue) },
                                    onRemove = { onFieldRemoved(index) },
                                    externalSyncKey = externalSyncKey
                                )
                                Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.06f))
                            }
                        }
                    }
                }
            } else {
                val setFields = bitAttributes.value.mapIndexed { index, attr -> index to attr }
                    .filter { it.second.isSet }

                if (setFields.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No data elements set. Use \"Add Data Element\" to begin.",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.45f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        setFields.forEach { (index, attr) ->
                            Iso8583FieldRow(
                                bitNumber = index + 1,
                                bitAttribute = attr,
                                value = message.getValue(index) ?: "",
                                onValueChanged = { newValue -> onFieldChanged(index, newValue) },
                                onRemove = { onFieldRemoved(index) },
                                externalSyncKey = externalSyncKey
                            )
                            Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.06f))
                        }
                    }
                }
            }

            // Add Field button bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.background.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                TextButton(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colors.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Add Data Element",
                        style = MaterialTheme.typography.button,
                        color = MaterialTheme.colors.primary
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddFieldDialog(
            bitAttributes = bitAttributes.value,
            onDismiss = { showAddDialog = false },
            onAdd = { bitNumber, value ->
                onFieldAdded(bitNumber, value)
                showAddDialog = false
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Add Field dialog — searchable list of unset fields
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AddFieldDialog(
    bitAttributes: Array<BitAttribute>,
    onDismiss: () -> Unit,
    onAdd: (Int, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var fieldValue by remember { mutableStateOf("") }

    val availableFields = remember(bitAttributes) {
        bitAttributes.mapIndexed { index, attr -> index to attr }.filter { !it.second.isSet }
    }

    val filtered = availableFields.filter { (index, attr) ->
        val de = (index + 1).toString()
        val desc = attr.description.lowercase()
        val q = searchQuery.trim().lowercase()
        q.isEmpty() || de.contains(q) || desc.contains(q)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .widthIn(min = 420.dp, max = 640.dp)
                .fillMaxHeight(0.78f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colors.surface,
            elevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Title bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colors.primary)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Add Data Element",
                        style = MaterialTheme.typography.h6,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Search field
                    FixedOutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            selectedIndex = null
                            fieldValue = ""
                        },
                        label = { Text("Search by DE number or field name") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colors.primary.copy(alpha = 0.7f)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colors.primary,
                            unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.25f),
                            focusedLabelColor = MaterialTheme.colors.primary
                        )
                    )

                    // Available field list
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                        color = MaterialTheme.colors.background.copy(alpha = 0.5f),
                        elevation = 0.dp
                    ) {
                        if (filtered.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (availableFields.isEmpty()) "All fields are already set."
                                    else "No fields match \"$searchQuery\".",
                                    style = MaterialTheme.typography.body2,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.45f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                itemsIndexed(filtered) { _, (index, attr) ->
                                    val isSelected = selectedIndex == index
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isSelected)
                                                    MaterialTheme.colors.primary.copy(alpha = 0.12f)
                                                else Color.Transparent
                                            )
                                            .clickable {
                                                selectedIndex = index
                                                fieldValue = ""
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isSelected) MaterialTheme.colors.primary
                                            else MaterialTheme.colors.primary.copy(alpha = 0.1f)
                                        ) {
                                            Text(
                                                text = "%03d".format(index + 1),
                                                style = MaterialTheme.typography.caption,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (isSelected) Color.White
                                                else MaterialTheme.colors.primary,
                                                modifier = Modifier.padding(
                                                    horizontal = 7.dp, vertical = 4.dp
                                                )
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = attr.description.ifEmpty { "Field ${index + 1}" },
                                                style = MaterialTheme.typography.body2,
                                                fontWeight = if (isSelected) FontWeight.SemiBold
                                                else FontWeight.Normal,
                                                color = MaterialTheme.colors.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${attr.typeAtribute} · max ${attr.maxLength} · ${attr.lengthAttribute}",
                                                style = MaterialTheme.typography.caption,
                                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                    Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.05f))
                                }
                            }
                        }
                    }

                    // Value field shown once a field is selected
                    if (selectedIndex != null) {
                        val selAttr = bitAttributes[selectedIndex!!]
                        FixedOutlinedTextField(
                            value = fieldValue,
                            onValueChange = { fieldValue = it },
                            label = {
                                Text(
                                    "Value for DE ${selectedIndex!! + 1} · ${selAttr.description.ifEmpty { "Field" }}",
                                    style = MaterialTheme.typography.caption
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = MaterialTheme.colors.primary,
                                unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.25f),
                                focusedLabelColor = MaterialTheme.colors.primary
                            )
                        )
                    }
                }

                Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                    }
                    Button(
                        onClick = {
                            val idx = selectedIndex
                            if (idx != null) {
                                onAdd(idx + 1, fieldValue)
                            }
                        },
                        enabled = selectedIndex != null,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Field")
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Field row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun Iso8583FieldRow(
    bitNumber: Int,
    bitAttribute: BitAttribute,
    value: String,
    onValueChanged: (String) -> Unit,
    onRemove: () -> Unit,
    externalSyncKey: Int = 0
) {
    // Local state decouples what the user is typing from packBit's transformed/padded value.
    // Re-initializes when bitNumber changes (different field slot) OR when externalSyncKey
    // increments (parent loaded a whole new message via Apply hex / Fill Fields / New message).
    var localValue by remember(bitNumber, externalSyncKey) { mutableStateOf(value) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // DE number badge
        Surface(
            shape = RoundedCornerShape(5.dp),
            color = MaterialTheme.colors.primary.copy(alpha = 0.10f),
            modifier = Modifier.width(52.dp)
        ) {
            Text(
                text = "%03d".format(bitNumber),
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colors.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 7.dp)
            )
        }

        // Field name + type info
        Column(modifier = Modifier.width(180.dp)) {
            Text(
                text = bitAttribute.description.ifEmpty { "Field $bitNumber" },
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${bitAttribute.typeAtribute} · max ${bitAttribute.maxLength}",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.45f),
                maxLines = 1
            )
        }

        // Editable value — drives off localValue so packBit zero-padding doesn't corrupt cursor
        FixedOutlinedTextField(
            value = localValue,
            onValueChange = { newText ->
                localValue = newText
                onValueChanged(newText)
            },
            modifier = Modifier.weight(1f),
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
            )
        )

        // Delete button
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Remove DE $bitNumber",
                tint = MaterialTheme.colors.error.copy(alpha = 0.65f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
