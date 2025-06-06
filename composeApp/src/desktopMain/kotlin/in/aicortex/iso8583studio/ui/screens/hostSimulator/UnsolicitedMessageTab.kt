package `in`.aicortex.iso8583studio.ui.screens.hostSimulator


import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import `in`.aicortex.iso8583studio.data.BitAttribute
import `in`.aicortex.iso8583studio.data.BitSpecific
import `in`.aicortex.iso8583studio.data.Iso8583Data
import `in`.aicortex.iso8583studio.data.getValue
import `in`.aicortex.iso8583studio.data.model.AddtionalOption
import `in`.aicortex.iso8583studio.data.model.BitLength
import `in`.aicortex.iso8583studio.data.model.BitType
import `in`.aicortex.iso8583studio.data.model.GatewayType
import `in`.aicortex.iso8583studio.domain.service.GatewayServiceImpl
import `in`.aicortex.iso8583studio.domain.utils.EMVTag
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil
import `in`.aicortex.iso8583studio.ui.ErrorRed
import `in`.aicortex.iso8583studio.ui.screens.components.StatusBadge
import `in`.aicortex.iso8583studio.ui.screens.config.GatewayTypeTab
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue


enum class ParseStatus { SUCCESS, WARNING, ERROR }


@Composable
fun UnsolicitedMessageTab(
    gw: GatewayServiceImpl,
    selectedField: MutableState<BitAttribute?>,
    selectedFieldIndex: MutableState<Int?>,
    showBitmapAnalysis: MutableState<Boolean>,
    showMessageParser: MutableState<Boolean>,
    isFirst: MutableState<Boolean>,
    animationTrigger: MutableState<Int>,
    rawMessage: MutableState<String>,
    parseError: MutableState<String?>,
    currentFields: MutableState<Array<BitAttribute>?>,
    currentBitmap: MutableState<ByteArray?>,
    searchQuery: MutableState<String>,
    initialMessage: Iso8583Data? = null,
    modifier: Modifier = Modifier
) {

    var message by remember {
        mutableStateOf<Iso8583Data?>(
            initialMessage
        )
    }


    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            animationTrigger.value++
        }
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colors.surface, MaterialTheme.colors.background.copy(alpha = 0.8f)
        )
    )
    Box(modifier = modifier.fillMaxSize().background(gradient)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            ProfessionalHeader(
                currentFields = currentFields.value,
                showBitmapAnalysis = showBitmapAnalysis.value,
                showMessageParser = showMessageParser.value,
                isFirst = isFirst.value,
                onBitmapToggle = { showBitmapAnalysis.value = !showBitmapAnalysis.value },
                onParserToggle = { showMessageParser.value = !showMessageParser.value },
                onFormatToggle = { isFirst.value = !isFirst.value },
                gw = gw
            )

            Spacer(modifier = Modifier.height(8.dp))



            AnimatedVisibility(
                visible = showMessageParser.value,
                enter = slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                EnhancedMessageParserCard(
                    rawMessage = rawMessage.value,
                    onMessageChange = { newMessage ->
                        rawMessage.value = newMessage
                        if (newMessage.isNotBlank()) {
                            try {
                                val parsed = Iso8583Data(gw.configuration, isFirst = isFirst.value)
                                parsed.unpack(IsoUtil.stringToBcd(newMessage))
                                currentFields.value = parsed.bitAttributes
                                currentBitmap.value = parsed.bitmap
                                message = parsed
                                parseError.value = null
                                selectedField.value = null
                            } catch (e: Exception) {
                                parseError.value = e.message ?: "Unknown parsing error"
                                message = null
                                currentFields.value = arrayOf()
                                currentBitmap.value = null
                            }
                        } else {
                            message = null
                            currentFields.value = arrayOf()
                            currentBitmap.value = null
                            parseError.value = null
                            selectedField.value = null
                        }
                    },
                    parsedMessage = message,
                    parseError = parseError.value,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (showBitmapAnalysis.value) {
                Dialog(
                    onDismissRequest = {
                        showBitmapAnalysis.value = false
                    }
                ) {
                    Card(

                        backgroundColor = MaterialTheme.colors.surface
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                        ) {
                            val bitmapFields =
                                remember(currentBitmap.value) {
                                    analyzeBitmap(currentBitmap.value ?: byteArrayOf())
                                }
                            EnhancedBitmapAnalysisCard(
                                bitmap = currentBitmap.value,
                                bitmapFields = bitmapFields,
                                presentFields = currentFields.value?.mapIndexed { i, item ->
                                    Pair(
                                        i,
                                        item
                                    )
                                }?.filter { it.second.isSet && (it.second.data?.size ?: 0) > 0 }
                                    ?.map { it.first.plus(1) }
                                    ?: emptyList(),
                                modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    showBitmapAnalysis.value = false
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Close")
                            }


                        }
                    }
                }

            }

            if (showMessageParser.value) Spacer(modifier = Modifier.height(8.dp))


            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    SearchAndFilterBar(
                        searchQuery = searchQuery.value,
                        onSearchChange = { searchQuery.value = it },
                        fieldCount = currentFields.value?.count { it.isSet } ?: 0,
                        modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(8.dp))




                    EnhancedFieldsListCard(
                        fields = currentFields.value ?: arrayOf(),
                        searchQuery = searchQuery.value,
                        selectedField = selectedField.value,
                        onFieldSelected = { field, index ->
                            selectedField.value = field
                            selectedFieldIndex.value = index
                        },
                        animationTrigger = animationTrigger.value,
                        modifier = Modifier.fillMaxWidth(),
                        gw = gw
                    )
                }
                var showEditDialog by remember { mutableStateOf(false) }
                if (showEditDialog) {
                    BitEditDialog(
                        bit = BitSpecific(
                            bitNumber = selectedFieldIndex.value?.plus(1)?.toByte() ?: 0.toByte(),
                            bitLength = selectedField.value?.lengthAttribute ?: BitLength.LLLVAR,
                            bitType = selectedField.value?.typeAtribute ?: BitType.BCD,
                            maxLength = selectedField.value?.maxLength ?: 0,
                            addtionalOption = selectedField.value?.additionalOption
                                ?: AddtionalOption.None
                        ),
                        onDismiss = {
                            showEditDialog = false
                        }
                    ) {
                        gw.configuration.getBitTemplate()?.map { item ->
                            if (item.bitNumber == it.bitNumber) it else item
                        }?.toTypedArray()?.let {
                            gw.configuration.setBitTemplate(template = it)
                        }
                        if (rawMessage.value.isNotBlank()) {
                            try {
                                val parsed = Iso8583Data(gw.configuration, isFirst = isFirst.value)
                                parsed.unpack(IsoUtil.stringToBcd(rawMessage.value))
                                currentFields.value = parsed.bitAttributes
                                currentBitmap.value = parsed.bitmap
                                message = parsed
                                parseError.value = null
                                selectedField.value = null
                            } catch (e: Exception) {
                                parseError.value = e.message ?: "Unknown parsing error"
                                message = null
                                currentFields.value = arrayOf()
                                currentBitmap.value = null
                            }
                        } else {
                            message = null
                            currentFields.value = arrayOf()
                            currentBitmap.value = null
                            parseError.value = null
                            selectedField.value = null
                        }
                        showEditDialog = false

                    }
                }

                EnhancedFieldDetailsPanel(
                    selectedField = selectedField.value,
                    selectedFieldIndex = selectedFieldIndex.value,
                    onFieldSelect = { field, index ->
                        selectedField.value = field
                        selectedFieldIndex.value = index
                    },
                    availableFields = currentFields.value?.filter { it.isSet } ?: emptyList(),
                    modifier = Modifier.weight(1f),
                    gw = gw,
                    onEdit = {
                        showEditDialog = true
                    }
                )
            }
        }
    }
}

@Composable
fun ProfessionalHeader(
    currentFields: Array<BitAttribute>?,
    showBitmapAnalysis: Boolean,
    showMessageParser: Boolean,
    gw: GatewayServiceImpl,
    isFirst: Boolean,
    onBitmapToggle: () -> Unit,
    onParserToggle: () -> Unit,
    onFormatToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 12.dp,
        shape = RoundedCornerShape(8.dp)
    ) {

        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "ISO8583 Unsolicited Message",
                            style = MaterialTheme.typography.h5.copy(
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colors.primary
                            )
                        )
                        Text(
                            text = "Professional Payment Message Analysis",
                            style = MaterialTheme.typography.subtitle1.copy(
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                            )
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (gw.configuration.gatewayType == GatewayType.PROXY) {
                            SourceToggler(
                                onSelected = {
                                    onFormatToggle()
                                },
                                isFirst = isFirst
                            )
                        }


                        EnhancedFilterChip(
                            selected = showBitmapAnalysis,
                            onClick = onBitmapToggle,
                            icon = Icons.Default.GridOn,
                            label = "Bitmap",
                            badge = "${currentFields?.count { it.isSet } ?: 0}/64"
                        )


                        EnhancedFilterChip(
                            selected = showMessageParser,
                            onClick = onParserToggle,
                            icon = Icons.Default.Code,
                            label = "Parser",
                            badge = if ((currentFields?.count { it.isSet } ?: 0) > 0) "✓" else null)
                    }
                }

//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    Row(
//                        modifier = Modifier.width(350.dp),
//                        horizontalArrangement = Arrangement.SpaceEvenly
//                    ) {
//                        StatCard(
//                            "Fields",
//                            "${currentFields?.count { it.isSet }}",
//                            "Present",
//                            Icons.Default.List,
//                            MaterialTheme.colors.primary
//                        )
//                        StatCard(
//                            "Critical",
//                            "${currentFields.count { it.criticality == FieldCriticality.CRITICAL }}",
//                            "Fields",
//                            Icons.Default.PriorityHigh,
//                            Color(0xFFF44336)
//                        )
//                        StatCard(
//                            "EMV Tags",
//                            "${currentFields.flatMap { it.emvTags }.size}",
//                            "Total",
//                            Icons.Default.Memory,
//                            Color(0xFF9C27B0)
//                        )
//                        parsedMessage?.let { msg ->
//                            StatCard(
//                                "Parse Time",
//                                "${msg.parsingDetails.parseTimeMs}",
//                                "ms",
//                                Icons.Default.Speed,
//                                Color(0xFF4CAF50)
//                            )
//                        }
//                    }
//                }
            }


        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SourceToggler(
    onSelected: () -> Unit,
    isFirst: Boolean,
    modifier: Modifier = Modifier
) {


    Column(
        modifier = modifier,
    ) {
        Box(modifier = Modifier.padding(12.dp).clickable { onSelected() }
            .background(
                color = MaterialTheme.colors.primary,
                shape = RoundedCornerShape(20.dp)
            )) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = if (isFirst) "Source" else "Destination",
                    style = MaterialTheme.typography.subtitle2.copy(
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    ),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isFirst) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                    contentDescription = if (isFirst) "Source" else "Destination",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )


            }
        }
    }
}

@Composable
fun EnhancedFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    badge: String? = null,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier,
    ) {
        Box(modifier = Modifier.padding(12.dp).clickable { onClick() }
            .background(
                color = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
                shape = RoundedCornerShape(20.dp)
            )) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) Color.White else MaterialTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label, style = MaterialTheme.typography.subtitle2.copy(
                        fontWeight = FontWeight.Medium,
                        color = if (selected) Color.White else MaterialTheme.colors.onSurface
                    )
                )
            }

            if (badge != null) {
                Card(
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-8).dp),
                    backgroundColor = MaterialTheme.colors.secondary,
                    shape = CircleShape,
                    elevation = 4.dp
                ) {
                    Text(
                        text = badge,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.caption.copy(
                            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}



@Composable
fun SearchAndFilterBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    fieldCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search fields...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search, contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    backgroundColor = MaterialTheme.colors.background.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Card(
                backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "$fieldCount Fields",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.caption.copy(
                        color = MaterialTheme.colors.primary, fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
fun EnhancedMessageParserCard(
    rawMessage: String,
    onMessageChange: (String) -> Unit,
    parsedMessage: Iso8583Data?,
    parseError: String?,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier,
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 8.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Parser",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(8.dp).size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Message Parser",
                            style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Real-time ISO8583 analysis",
                            style = MaterialTheme.typography.caption.copy(
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    AnimatedVisibility(
                        visible = parseError == null && parsedMessage != null,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            backgroundColor = Color(0xFF4CAF50).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("✓ Parsed successfully - ${parsedMessage?.bitAttributes?.count { it.isSet } ?: 0} fields",
                                    style = MaterialTheme.typography.caption.copy(
                                        color = Color(0xFF4CAF50), fontWeight = FontWeight.Medium
                                    ))
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    parsedMessage?.let { msg ->

                        Card(
                            backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = "MTI",
                                    tint = MaterialTheme.colors.secondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "MTI: ${msg.messageType}",
                                    style = MaterialTheme.typography.caption.copy(
                                        color = MaterialTheme.colors.secondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        if (msg.hasHeader) {
                            Card(
                                backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = 8.dp,
                                        vertical = 4.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = "tpdu",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))

                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "TPDU: ${IsoUtil.bytesToHexString(msg.tpduHeader.rawTPDU)}",
                                        style = MaterialTheme.typography.caption.copy(
                                            color = Color(0xFF4CAF50),
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }

                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = MaterialTheme.colors.primary,
                                    modifier = Modifier.size(14.dp)
                                )

                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${msg.bitAttributes.count { it.isSet }} fields",
                                    style = MaterialTheme.typography.caption.copy(
                                        color = MaterialTheme.colors.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                backgroundColor = MaterialTheme.colors.background.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                elevation = 2.dp
            ) {
                OutlinedTextField(
                    value = rawMessage,
                    onValueChange = onMessageChange,
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Input,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Raw ISO8583 Message (Hex)")
                        }
                    },
                    placeholder = {
                        Text(
                            "Enter hex message: 60000000000200B23C800002E00000040000000000000012345...",
                            style = MaterialTheme.typography.body2.copy(
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 18.sp
                    ),
                    maxLines = 8,
                    trailingIcon = {
                        if (rawMessage.isNotEmpty()) {
                            Row {
                                IconButton(onClick = {
                                    val formatted =
                                        rawMessage.replace("\\s".toRegex(), "").chunked(2)
                                            .chunked(16).joinToString("\n") { line ->
                                                line.joinToString(" ")
                                            }
                                    onMessageChange(formatted)
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.FormatAlignLeft,
                                        contentDescription = "Format",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(onClick = { onMessageChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear"
                                    )
                                }
                            }
                        }
                    },
                    isError = parseError != null,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        backgroundColor = Color.Transparent,
                        focusedBorderColor = if (parseError != null) MaterialTheme.colors.error else MaterialTheme.colors.primary,
                        unfocusedBorderColor = if (parseError != null) MaterialTheme.colors.error else MaterialTheme.colors.onSurface.copy(
                            alpha = 0.12f
                        )
                    )
                )
            }



            AnimatedVisibility(
                visible = rawMessage.isEmpty(), enter = fadeIn(), exit = fadeOut()
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Enter hex message with TPDU, MTI, bitmap and field data",
                        style = MaterialTheme.typography.caption.copy(
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }
            }

            AnimatedVisibility(
                visible = parseError != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colors.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Parsing Error",
                                style = MaterialTheme.typography.subtitle2.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colors.error
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = parseError ?: "",
                                style = MaterialTheme.typography.body2.copy(
                                    color = MaterialTheme.colors.error.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedBitmapAnalysisCard(
    bitmap: ByteArray?,
    bitmapFields: List<BitmapField>, presentFields: List<Int>, modifier: Modifier = Modifier
) {

    if (bitmapFields.isNotEmpty()) {
        Card(
            modifier = modifier,
            backgroundColor = MaterialTheme.colors.surface,
            elevation = 8.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Card(
                        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.12f),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "Bitmap",
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.padding(8.dp).size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Bitmap Analysis",
                            style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${presentFields.size} of 64 fields present",
                            style = MaterialTheme.typography.caption.copy(
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    backgroundColor = MaterialTheme.colors.background.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Hex: ${IsoUtil.bytesToHexString(bitmap ?: byteArrayOf())}",
                        style = MaterialTheme.typography.body2.copy(
                            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    backgroundColor = MaterialTheme.colors.background.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val gridResult = divideNumberIntoGrid(128)
                        val grid = gridResult.grid
                        val (rows, columns) = gridResult.dimensions

                        for (col in 0 until columns) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (row in 0 until rows) {
                                    val value = grid[row][col]?.toString()
                                    if (value != null) {
                                        val index = col * 16 + row
                                        val fieldNumber = index + 1
                                        val isSet =
                                            bitmapFields.any { it.fieldNumber == fieldNumber && it.isSet }
                                        val isPresent = presentFields.contains(fieldNumber)

                                        EnhancedBitmapBit(
                                            fieldNumber = fieldNumber,
                                            isSet = isSet,
                                            isPresent = isPresent,
                                            modifier = Modifier.weight(1f).aspectRatio(1f)
                                        )
                                    }
                                }
                            }
                        }
                        repeat(gridResult.dimensions.first) { row ->

                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EnhancedLegendItem(
                        color = MaterialTheme.colors.primary,
                        label = "Set & Present",
                        icon = Icons.Default.CheckCircle
                    )
                    EnhancedLegendItem(
                        color = MaterialTheme.colors.error,
                        label = "Set but Missing",
                        icon = Icons.Default.Warning
                    )
                    EnhancedLegendItem(
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                        label = "Not Set",
                        icon = Icons.Default.Circle
                    )
                }
            }
        }
    } else {
        Column {
            StatusBadge(
                text = "ERROR",
                color = ErrorRed,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No bitmap analysis available",
                style = MaterialTheme.typography.body2.copy(
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            )

        }
    }

}

data class GridResult(
    val grid: Array<Array<Char?>>,
    val dimensions: Pair<Int, Int> // (rows, columns)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GridResult

        if (!grid.contentDeepEquals(other.grid)) return false
        if (dimensions != other.dimensions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = grid.contentDeepHashCode()
        result = 31 * result + dimensions.hashCode()
        return result
    }
}

fun divideNumberIntoGrid(digitCount: Int): GridResult {
    // Convert number to string and filter valid characters

    // Always use 16 rows
    val rows = 16

    // Calculate required columns: ceil(digitCount / 16)
    val columns = if (digitCount % rows == 0) {
        digitCount / rows
    } else {
        (digitCount / rows) + 1
    }

    // Create 16×n grid initialized with null
    val grid = Array(rows) { Array<Char?>(columns) { null } }

    // Fill the grid column by column (top to bottom, left to right)
    var digitIndex = 0
    for (col in 0 until columns) {
        for (row in 0 until rows) {
            if (digitIndex < digitCount) {
                grid[row][col] = '1'
                digitIndex++
            }
        }
    }

    return GridResult(grid, Pair(rows, columns))
}

@Composable
fun EnhancedBitmapBit(
    fieldNumber: Int, isSet: Boolean, isPresent: Boolean, modifier: Modifier = Modifier
) {

    val backgroundColor = when {
        isSet && isPresent -> MaterialTheme.colors.primary
        isSet && !isPresent -> MaterialTheme.colors.error
        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
    }

    val borderColor = when {
        isSet && isPresent -> MaterialTheme.colors.primary
        isSet && !isPresent -> MaterialTheme.colors.error
        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
    }

    Card(
        modifier = modifier,
        backgroundColor = backgroundColor,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = fieldNumber.toString(), style = MaterialTheme.typography.caption.copy(
                    color = if (isSet) Color.White else MaterialTheme.colors.onSurface,
                    fontSize = 10.sp,
                    fontWeight = if (isSet) FontWeight.Bold else FontWeight.Normal
                )
            )
        }
    }
}

@Composable
fun EnhancedLegendItem(
    color: Color, label: String, icon: ImageVector, modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically, modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
fun EnhancedFieldsListCard(
    gw: GatewayServiceImpl,
    fields: Array<BitAttribute>,
    searchQuery: String,
    selectedField: BitAttribute?,
    onFieldSelected: (BitAttribute, Int) -> Unit,
    animationTrigger: Int,
    modifier: Modifier = Modifier
) {
    val filteredFields = remember(fields, searchQuery) {
        fields.filter { it.isSet }.filterIndexed { i, field ->
            if (searchQuery.isEmpty()) true
            else {
                i.plus(1).toString()
                    .contains(searchQuery, ignoreCase = true) || field.description.contains(
                    searchQuery,
                    ignoreCase = true
                ) /*||
                        (field.interpretation?.contains(searchQuery, ignoreCase = true) == true)*/
            }
        }
    }

    Card(
        modifier = modifier,
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 8.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "Fields",
                    tint = MaterialTheme.colors.secondary,
                    modifier = Modifier.padding(8.dp).size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Message Fields",
                        style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${filteredFields.size} fields ${if (searchQuery.isNotEmpty()) "found" else "present"}",
                        style = MaterialTheme.typography.caption.copy(
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                var anim = 0
                fields.forEachIndexed { index, field ->
                    if (field.isSet && field.data != null) {
                        val animationDelay = (anim * 80).coerceAtMost(400)
                        EnhancedFieldItem(
                            field = field,
                            fieldNumber = index + 1,
                            isSelected = selectedField == field,
                            onClick = { onFieldSelected(field, index) },
                            animationTrigger = animationTrigger,
                            animationDelay = animationDelay,
                            modifier = Modifier.fillMaxWidth(),
                            gw = gw
                        )
                        anim += 1
                    }

                }

                if (filteredFields.isEmpty() && searchQuery.isNotEmpty()) {
                    Card(
                        backgroundColor = MaterialTheme.colors.background.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = "No results",
                                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No fields found",
                                style = MaterialTheme.typography.subtitle1.copy(
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                )
                            )
                            Text(
                                text = "Try adjusting your search query",
                                style = MaterialTheme.typography.caption.copy(
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedFieldItem(
    field: BitAttribute,
    isSelected: Boolean,
    gw: GatewayServiceImpl,
    fieldNumber: Int,
    onClick: () -> Unit,
    animationTrigger: Int,
    animationDelay: Int,
    modifier: Modifier = Modifier
) {


    Card(
        modifier = modifier.clickable { onClick() },

        shape = RoundedCornerShape(8.dp), border = if (isSelected) BorderStroke(
            2.dp, MaterialTheme.colors.primary.copy(alpha = 0.3f)
        ) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "F${fieldNumber}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.caption.copy(
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colors.primary
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))
            val interpretation =
                gw.configuration.getBitTemplate()
                    ?.first { it.bitNumber.toInt().absoluteValue == fieldNumber }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = field.description,
                        style = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    EnhancedFieldTypeChip(field.typeAtribute)
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (interpretation != null) {
                    Text(
                        text = interpretation.description,
                        style = MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.secondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (fieldNumber == 55) {
                    val categories = field.getEmvTags()
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "EMV Tags",
                            tint = Color(0xFF9C27B0),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${categories.tags.size} EMV tags",
                            style = MaterialTheme.typography.caption.copy(
                                color = Color(0xFF9C27B0), fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.ExpandLess else Icons.Default.ChevronRight,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )

                if (field.length != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${field.length}B",
                        style = MaterialTheme.typography.caption.copy(
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedFieldTypeChip(type: BitType, modifier: Modifier = Modifier) {
    val (color, icon) = when (type) {
        BitType.AN -> Pair(MaterialTheme.colors.primary, Icons.Default.Numbers)
        BitType.NOT_SPECIFIC -> Pair(MaterialTheme.colors.secondary, Icons.Default.TextFields)
        BitType.BINARY -> Pair(Color(0xFF9C27B0), Icons.Default.Memory)
        BitType.ANS -> Pair(Color(0xFF607D8B), Icons.Default.LinearScale)
        BitType.BCD -> Pair(Color(0xFFF44336), Icons.Default.FormatListNumbered)
    }

    Card(
        backgroundColor = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = type.name,
                modifier = Modifier.size(10.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = type.name.take(3), style = MaterialTheme.typography.caption.copy(
                    color = color, fontWeight = FontWeight.Medium, fontSize = 8.sp
                )
            )
        }
    }
}

@Composable
fun EnhancedFieldDetailsPanel(
    gw: GatewayServiceImpl,
    selectedField: BitAttribute?,
    selectedFieldIndex: Int?,
    onFieldSelect: (BitAttribute, Int) -> Unit,
    onEdit: (BitAttribute) -> Unit,
    availableFields: List<BitAttribute>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 8.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            if (selectedField != null) {
                EnhancedFieldDetailContent(
                    field = selectedField,
                    onFieldSelect = onFieldSelect,
                    availableFields = availableFields,
                    selectedFieldIndex = selectedFieldIndex,
                    gw = gw,
                    onEdit = onEdit
                )
            } else {
                EnhancedEmptyFieldSelection(
                    availableFields = availableFields,
                    onFieldSelect = onFieldSelect
                )
            }
        }
    }
}

@Composable
fun EnhancedFieldDetailContent(
    gw: GatewayServiceImpl,
    selectedFieldIndex: Int?,
    field: BitAttribute,
    onFieldSelect: (BitAttribute, Int) -> Unit,
    onEdit: (BitAttribute) -> Unit,
    availableFields: List<BitAttribute>
) {
    Column(
        modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(8.dp), elevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.Transparent),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Card(
                            backgroundColor = MaterialTheme.colors.primary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "F${selectedFieldIndex?.plus(1) ?: "__"}",
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 6.dp
                                ),
                                style = MaterialTheme.typography.h6.copy(
                                    fontWeight = FontWeight.Bold, color = Color.White
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {

                            EnhancedFieldTypeChip(field.typeAtribute)
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${field.lengthAttribute.name} ",
                                    modifier = Modifier.padding(
                                        horizontal = 8.dp,
                                        vertical = 4.dp
                                    ),
                                    style = MaterialTheme.typography.caption.copy(
                                        color = MaterialTheme.colors.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentIndex = availableFields.indexOf(field)
                        val prevField = availableFields.getOrNull(currentIndex - 1)
                        val nextField = availableFields.getOrNull(currentIndex + 1)

                        IconButton(
                            onClick = { prevField?.let { onFieldSelect(it, currentIndex) } },
                            enabled = prevField != null
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous Field"
                            )
                        }

                        IconButton(
                            onClick = { nextField?.let { onFieldSelect(it, currentIndex) } },
                            enabled = nextField != null
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next Field"
                            )
                        }

                        Icon(
                            modifier = Modifier.clickable(onClick = {
                                onEdit(field)
                            }),
                            contentDescription = "",
                            imageVector = Icons.Default.Edit
                        )
                    }
                }
                if (field.description.isNotEmpty()) {
                    Text(
                        text = field.description,
                        style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Medium)
                    )
                }

                val interpretation = gw.configuration.getBitTemplate()
                    ?.first { it.bitNumber.toInt().absoluteValue == selectedFieldIndex?.plus(1) }

                if (interpretation?.description != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = interpretation.description,
                        style = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.secondary)
                    )
                }


                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    backgroundColor = MaterialTheme.colors.background.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    elevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        TextField(
                            value = field.getValue() ?: "",
                            modifier = Modifier.fillMaxWidth(),
                            onValueChange = { },
                            readOnly = true
                        )


                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            InfoChip("Length", "${field.length} bytes")
                            InfoChip("Type", field.typeAtribute.name)
                            InfoChip("Hex Length", "${field.getValue()?.length} chars")
                        }

                    }
                }
            }
        }
        if (selectedFieldIndex?.plus(1) == 55) {
            val categories = field.getEmvTags()
            EnhancedDetailSection(
                title = "EMV Tags Analysis",
                icon = Icons.Default.Memory,
                color = Color(0xFF9C27B0)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.tags.forEach { item ->
                            Card(
                                backgroundColor = Color(0xFF9C27B0).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${item.tag}: ${categories.tags.size}",
                                    modifier = Modifier.padding(
                                        horizontal = 8.dp,
                                        vertical = 4.dp
                                    ),
                                    style = MaterialTheme.typography.caption.copy(
                                        color = Color(0xFF9C27B0),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    categories.tags.forEach { tag -> EnhancedEMVTagCard(tag) }
                }
            }
        }

        when (selectedFieldIndex?.plus(1)) {
            22 -> EnhancedPosEntryModeAnalysis(field.getValue() ?: "")
            25 -> EnhancedPosConditionCodeAnalysis(field.getValue() ?: "")
            35 -> EnhancedTrack2DataAnalysis(field.getValue() ?: "")
        }
    }
}

@Composable
fun InfoChip(label: String, value: String) {
    Card(
        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.08f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ", style = MaterialTheme.typography.caption.copy(
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            )
            Text(
                text = value,
                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Medium)
            )
        }
    }
}

@Composable
fun EnhancedDetailSection(
    title: String,
    icon: ImageVector,
    color: Color,
    content: @Composable () -> Unit
) {
    Card(
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.padding(8.dp).size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold)
                    )
                }


            }

            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun EnhancedEMVTagCard(tag: EMVTag) {
    var showTagInfo by remember { mutableStateOf(false) }

    if (showTagInfo) {
        EMVTagDetailsDialog(
            tag = tag,
            onDismiss = { showTagInfo = false }
        )
    }

    Card(
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Card(shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = "Tag ${tag.tag}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.caption.copy(
                                fontWeight = FontWeight.Bold, color = Color.White
                            )
                        )
                    }
                }

                Row {
                    Text(
                        text = IsoUtil.bcdToString(tag.value),
                        style = MaterialTheme.typography.caption.copy(
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Information",
                        modifier = Modifier.clickable {
                            showTagInfo = true
                        }.size(16.dp),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tag.description,
                style = MaterialTheme.typography.body2
            )
        }
    }
}

@Composable
fun EnhancedPosEntryModeAnalysis(value: String) {
    val analysis = analyzePosEntryMode(value)

    EnhancedDetailSection(
        title = "POS Entry Mode Analysis", icon = Icons.Default.Input, color = Color(0xFF4CAF50)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            analysis.forEach { (key, description) ->
                Card(
                    backgroundColor = Color(0xFF4CAF50).copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.body2.copy(color = Color(0xFF4CAF50))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedPosConditionCodeAnalysis(value: String) {
    val analysis = analyzePosConditionCode(value)

    EnhancedDetailSection(
        title = "POS Condition Code Analysis",
        icon = Icons.Default.Store,
        color = Color(0xFFFF9800)
    ) {
        Card(
            backgroundColor = Color(0xFFFF9800).copy(alpha = 0.08f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = analysis,
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun EnhancedTrack2DataAnalysis(value: String) {
    val analysis = analyzeTrack2Data(value)

    EnhancedDetailSection(
        title = "Track 2 Data Analysis",
        icon = Icons.Default.CreditCard,
        color = Color(0xFF2196F3)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            analysis.forEach { (key, description) ->
                Card(
                    backgroundColor = Color(0xFF2196F3).copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = description, style = MaterialTheme.typography.body2.copy(
                                color = Color(0xFF2196F3),
                                fontFamily = if (key == "Raw Data") FontFamily.Monospace else FontFamily.Default
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedEmptyFieldSelection(
    availableFields: List<BitAttribute>, onFieldSelect: (BitAttribute, Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.TouchApp,
            contentDescription = "Select Field",
            modifier = Modifier.padding(24.dp).size(64.dp),
            tint = MaterialTheme.colors.primary.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Select a field to analyze", style = MaterialTheme.typography.h6.copy(
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Choose any field from the list to see comprehensive analysis including EMV tag interpretations and field-specific breakdowns",
            style = MaterialTheme.typography.body2.copy(
                color = MaterialTheme.colors.onSurface.copy(
                    alpha = 0.6f
                )
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

//        Text(
//            text = "Quick Access", style = MaterialTheme.typography.subtitle2.copy(
//                fontWeight = FontWeight.Medium, color = MaterialTheme.colors.primary
//            )
//        )
//
//        Spacer(modifier = Modifier.height(12.dp))
//
//        Row(
//            horizontalArrangement = Arrangement.spacedBy(8.dp),
//            modifier = Modifier.horizontalScroll(rememberScrollState())
//        ) {
//            availableFields.filter { it.criticality == FieldCriticality.CRITICAL }.take(3)
//                .forEach { field ->
//                    Card(
//                        modifier = Modifier.clickable { onFieldSelect(field) },
//                        backgroundColor = MaterialTheme.colors.surface,
//                        elevation = 2.dp,
//                        shape = RoundedCornerShape(8.dp)
//                    ) {
//                        Column(
//                            modifier = Modifier.padding(12.dp),
//                            horizontalAlignment = Alignment.CenterHorizontally
//                        ) {
//                            Text(
//                                text = "F${field.fieldNumber}",
//                                style = MaterialTheme.typography.caption.copy(
//                                    fontWeight = FontWeight.Bold,
//                                    color = MaterialTheme.colors.primary
//                                )
//                            )
//                            Text(
//                                text = field.description.take(15) + if (field.description.length > 15) "..." else "",
//                                style = MaterialTheme.typography.caption.copy(fontSize = 10.sp),
//                                textAlign = TextAlign.Center
//                            )
//                        }
//                    }
//                }
//        }
    }
}

data class BitmapField(val position: Int, val isSet: Boolean, val fieldNumber: Int)


fun analyzeBitmap(array: ByteArray): List<BitmapField> {
    val bitmapFields = mutableListOf<BitmapField>()
    if (array.isEmpty()) {
        return bitmapFields
    }
    // Process primary bitmap (first 8 bytes)
    val primaryBytes = array.copyOfRange(0, 8)
    for (i in 0 until 64) {
        val byteIndex = i / 8
        val bitIndex = 7 - (i % 8)  // Bits are ordered from most to least significant
        val bitValue = (primaryBytes[byteIndex].toInt() and (1 shl bitIndex)) != 0
        bitmapFields.add(BitmapField(position = i, isSet = bitValue, fieldNumber = i + 1))
    }

    // Process secondary bitmap if present (next 8 bytes)
    if (array.size >= 16) {
        val secondaryBytes = array.copyOfRange(8, 16)
        for (i in 0 until 64) {
            val byteIndex = i / 8
            val bitIndex = 7 - (i % 8)
            val bitValue = (secondaryBytes[byteIndex].toInt() and (1 shl bitIndex)) != 0
            bitmapFields.add(
                BitmapField(
                    position = i + 64,
                    isSet = bitValue,
                    fieldNumber = i + 64 + 1
                )
            )
        }
    }
    return bitmapFields
}

fun analyzePosEntryMode(data: String): List<Pair<String, String>> {
    var value = data.takeLast(3)
    val panEntryMode = value.substring(0, 2)
    val pinEntryCapability = value.substring(2, 3)

    val panDescription = when (panEntryMode) {
        "00" -> "Unknown"
        "01" -> "Manual key entry"
        "02" -> "Magnetic stripe read"
        "05" -> "Integrated circuit card (ICC) read"
        "07" -> "Auto entry via contactless M/Chip"
        "90" -> "Magnetic stripe read, track data could not be read"
        "91" -> "Contactless magnetic stripe data read"
        "95" -> "Integrated circuit card read; CVV can be checked"
        else -> "Reserved/Unknown"
    }

    val pinDescription = when (pinEntryCapability) {
        "0" -> "Unknown"
        "1" -> "PIN entry capability"
        "2" -> "No PIN entry capability"
        else -> "Reserved"
    }

    return listOf(
        "PAN Entry Mode" to "$panEntryMode - $panDescription",
        "PIN Entry Capability" to "$pinEntryCapability - $pinDescription"
    )
}

fun analyzePosConditionCode(value: String): String {
    return when (value) {
        "00" -> "Normal presentment"
        "01" -> "Customer not present"
        "02" -> "Unattended terminal, customer present"
        "03" -> "Merchant suspicious"
        "04" -> "Customer present, card not present"
        "05" -> "Preauthorized request"
        "06" -> "Preauthorized request, customer not present"
        "08" -> "Magnetic stripe could not be read"
        "10" -> "Customer present, signature required"
        "11" -> "Customer present, signature not required"
        else -> "Reserved/Unknown condition code"
    }
}

fun analyzeTrack2Data(value: String): List<Pair<String, String>> {
    val parts = value.split("=")
    if (parts.size != 2) return listOf("Error" to "Invalid Track 2 format")

    val pan = parts[0]
    val additionalData = parts[1]

    val result = mutableListOf<Pair<String, String>>()
    result.add("PAN" to pan)

    if (additionalData.length >= 4) {
        val expiryDate = additionalData.substring(0, 4)
        result.add(
            "Expiry Date" to "${expiryDate.substring(2, 4)}/${
                expiryDate.substring(
                    0,
                    2
                )
            }"
        )
    }

    if (additionalData.length >= 7) {
        val serviceCode = additionalData.substring(4, 7)
        result.add("Service Code" to "$serviceCode - ${analyzeServiceCode(serviceCode)}")
    }

    result.add("Raw Data" to value)
    return result
}

fun analyzeServiceCode(serviceCode: String): String {
    if (serviceCode.length != 3) return "Invalid service code"

    val first = serviceCode[0]
    val second = serviceCode[1]
    val third = serviceCode[2]

    val authMethod = when (first) {
        '1' -> "No restrictions"
        '2' -> "No restrictions"
        '5' -> "Goods and services only"
        '6' -> "ATM only"
        '7' -> "Goods and services only"
        else -> "Unknown"
    }

    val pinRequirement = when (second) {
        '0' -> "PIN required"
        '2' -> "PIN required if PED present"
        '3' -> "PIN required if PED present"
        '4' -> "PIN required"
        else -> "No PIN required"
    }

    val authLocation = when (third) {
        '0' -> "PIN verification not required"
        '1' -> "PIN verification required"
        '2' -> "PIN verification not required"
        '3' -> "PIN verification required"
        '4' -> "PIN verification required"
        '5' -> "PIN verification not required"
        '6' -> "PIN verification required"
        '7' -> "PIN verification not required"
        else -> "Unknown"
    }

    return "$authMethod, $pinRequirement, $authLocation"
}
