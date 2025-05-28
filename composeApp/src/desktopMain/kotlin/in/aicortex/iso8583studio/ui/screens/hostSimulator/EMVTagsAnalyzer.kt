package `in`.aicortex.iso8583studio.ui.screens.hostSimulator

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import `in`.aicortex.iso8583studio.domain.utils.EMVTag
import `in`.aicortex.iso8583studio.domain.utils.EMVTagParser
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun EMVTagDetailsDialog(
    tag: EMVTag,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Column(
            modifier = Modifier
                .background(color = MaterialTheme.colors.surface)
                .fillMaxWidth(0.4f)
                .fillMaxHeight(0.7f)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .clip(RoundedCornerShape(12.dp)),
            ) {
                // Clean Header
                MinimalHeader(tag = tag, onDismiss = onDismiss)

                Spacer(modifier = Modifier.height(20.dp))

                // Content
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Basic Information - always show
                    item {
                        BasicInfoSection(tag = tag)
                    }

                    // Value section - only if has meaningful data
                    if (shouldShowValueSection(tag)) {
                        item {
                            ValueSection(tag = tag)
                        }
                    }

                    // Bit analysis - only for complex EMV tags
                    if (requiresDetailedAnalysis(tag)) {
                        item {
                            BitAnalysisSection(tag = tag)
                        }
                    }

                    // Children - only for constructed tags
                    if (tag.isConstructed && tag.children.isNotEmpty()) {
                        item {
                            ChildrenSection(tag = tag)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Simple Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }

        }
    }
}

@Composable
private fun MinimalHeader(
    tag: EMVTag,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            ,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tag ${tag.tag}",
                    style = MaterialTheme.typography.h5.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colors.onSurface
                )

                if (tag.description.isNotEmpty()) {
                    Text(
                        text = tag.description,
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }

                Text(
                    text = "${tag.length} bytes • ${if (tag.isConstructed) "Constructed" else "Primitive"}",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }

}

@Composable
private fun BasicInfoSection(tag: EMVTag) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        backgroundColor = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Information",
                style = MaterialTheme.typography.subtitle2.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colors.onSurface
            )

            Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f))

            InfoRow("Tag", tag.tag, fontFamily = FontFamily.Monospace)
            InfoRow("Length", "${tag.length} bytes")
            InfoRow("Type", if (tag.isConstructed) "Constructed" else "Primitive")

            val tagClass = getTagClass(tag.tag)
            if (tagClass != "Unknown") {
                InfoRow("Class", tagClass)
            }
        }
    }
}

@Composable
private fun ValueSection(tag: EMVTag) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        backgroundColor = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Value",
                style = MaterialTheme.typography.subtitle2.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colors.onSurface
            )

            Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f))

            if (tag.value.isNotEmpty()) {
                // Always show hex
                InfoRow(
                    "Hex",
                    tag.getValueAsHex(),
                    fontFamily = FontFamily.Monospace
                )

                // Show ASCII only if meaningful
                val asciiValue = getCleanAscii(tag.value)
                if (asciiValue.isNotEmpty() && asciiValue.length > 2) {
                    InfoRow("ASCII", asciiValue)
                }

                // Show BCD only if all digits
                val bcdValue = getBcdValue(tag.value)
                if (bcdValue.isNotEmpty() && bcdValue != tag.getValueAsHex()) {
                    InfoRow("BCD", bcdValue, fontFamily = FontFamily.Monospace)
                }

                // Show interpretation only for complex tags
                val interpretation = getInterpretation(tag.tag, tag.value)
                if (interpretation.isNotEmpty()) {
                    InfoRow("Meaning", interpretation)
                }
            } else {
                Text(
                    text = "No value data",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

@Composable
private fun BitAnalysisSection(tag: EMVTag) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        backgroundColor = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Bit Analysis",
                style = MaterialTheme.typography.subtitle2.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colors.onSurface
            )

            Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f))

            when (tag.tag.uppercase()) {
                "95" -> TVRAnalysis(tag.value)
                "9B" -> TSIAnalysis(tag.value)
                "82" -> AIPAnalysis(tag.value)
                "9F33" -> TerminalCapabilitiesAnalysis(tag.value)
                "8E" -> CVMListAnalysis(tag.value)
                "9F34" -> CVMResultsAnalysis(tag.value)
                "9F27" -> CryptogramAnalysis(tag.value)
                else -> GenericBitAnalysis(tag.value)
            }
        }
    }
}

@Composable
private fun TVRAnalysis(tvr: ByteArray) {
    if (tvr.size != 5) {
        Text(
            text = "Invalid TVR length: expected 5 bytes",
            color = MaterialTheme.colors.error,
            style = MaterialTheme.typography.body2
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tvr.forEachIndexed { index, byte ->
            val title = when (index) {
                0 -> "Byte 1: Offline Data Authentication"
                1 -> "Byte 2: Application Usage Control"
                2 -> "Byte 3: Cardholder Verification"
                3 -> "Byte 4: Terminal Risk Management"
                4 -> "Byte 5: Issuer Authentication"
                else -> "Byte ${index + 1}"
            }

            val descriptions = getTVRBitDescriptions(index)
            SimpleBitAnalysis(byte, title, descriptions)
        }
    }
}

@Composable
private fun TSIAnalysis(tsi: ByteArray) {
    if (tsi.size != 2) {
        Text(
            text = "Invalid TSI length: expected 2 bytes",
            color = MaterialTheme.colors.error,
            style = MaterialTheme.typography.body2
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SimpleBitAnalysis(
            tsi[0],
            "Byte 1: Functions Performed",
            listOf(
                "Offline data authentication performed",
                "Cardholder verification performed",
                "Card risk management performed",
                "Issuer authentication performed",
                "Terminal risk management performed",
                "Script processing performed",
                "RFU", "RFU"
            )
        )

        if (tsi[1].toInt() != 0) {
            SimpleBitAnalysis(tsi[1], "Byte 2: Reserved", emptyList())
        }
    }
}

@Composable
private fun AIPAnalysis(aip: ByteArray) {
    if (aip.size != 2) {
        Text(
            text = "Invalid AIP length: expected 2 bytes",
            color = MaterialTheme.colors.error,
            style = MaterialTheme.typography.body2
        )
        return
    }

    SimpleBitAnalysis(
        aip[0],
        "Application Capabilities",
        listOf(
            "RFU",
            "SDA supported",
            "DDA supported",
            "Cardholder verification supported",
            "Terminal risk management required",
            "Issuer authentication supported",
            "On-device CVM supported",
            "CDA supported"
        )
    )

    if (aip[1].toInt() != 0) {
        SimpleBitAnalysis(aip[1], "Reserved", emptyList())
    }
}

@Composable
private fun TerminalCapabilitiesAnalysis(capabilities: ByteArray) {
    if (capabilities.size != 3) {
        Text(
            text = "Invalid Terminal Capabilities length: expected 3 bytes",
            color = MaterialTheme.colors.error,
            style = MaterialTheme.typography.body2
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SimpleBitAnalysis(
            capabilities[0],
            "Card Data Input",
            listOf(
                "Manual entry",
                "Magnetic stripe",
                "IC with contacts",
                "RFU",
                "RFU",
                "RFU",
                "RFU",
                "RFU"
            )
        )

        SimpleBitAnalysis(
            capabilities[1],
            "CVM Capability",
            listOf(
                "Plaintext PIN (ICC)",
                "Enciphered PIN (online)",
                "Signature",
                "Enciphered PIN (offline)",
                "No CVM required",
                "RFU", "RFU", "RFU"
            )
        )

        SimpleBitAnalysis(
            capabilities[2],
            "Security Capability",
            listOf("SDA", "DDA", "Card capture", "RFU", "CDA", "RFU", "RFU", "RFU")
        )
    }
}

@Composable
private fun CVMListAnalysis(cvmList: ByteArray) {
    if (cvmList.size < 10) {
        Text(
            text = "Invalid CVM List: minimum 10 bytes required",
            color = MaterialTheme.colors.error,
            style = MaterialTheme.typography.body2
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val amountX = cvmList.copyOfRange(0, 4)
        val amountY = cvmList.copyOfRange(4, 8)

        InfoRow("Amount X", amountX.joinToString("") { "%02X".format(it) }, FontFamily.Monospace)
        InfoRow("Amount Y", amountY.joinToString("") { "%02X".format(it) }, FontFamily.Monospace)

        val rules = cvmList.drop(8).chunked(2)
        Text(
            text = "${rules.size} CVM rules defined",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )

        rules.take(3).forEachIndexed { index, rule ->
            if (rule.size == 2) {
                val method = interpretCVMCode(rule[0].toInt() and 0x3F)
                val condition = interpretCVMCondition(rule[1].toInt() and 0xFF)

                Text(
                    text = "Rule ${index + 1}: $method, $condition",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                )
            }
        }

        if (rules.size > 3) {
            Text(
                text = "... and ${rules.size - 3} more rules",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun CVMResultsAnalysis(cvmResults: ByteArray) {
    if (cvmResults.size != 3) {
        Text(
            text = "Invalid CVM Results length: expected 3 bytes",
            color = MaterialTheme.colors.error,
            style = MaterialTheme.typography.body2
        )
        return
    }

    val method = interpretCVMCode(cvmResults[0].toInt() and 0x3F)
    val condition = interpretCVMCondition(cvmResults[1].toInt() and 0xFF)
    val result = interpretCVMResult(cvmResults[2].toInt() and 0xFF)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        InfoRow("Method", method)
        InfoRow("Condition", condition)
        InfoRow("Result", result)
    }
}

@Composable
private fun CryptogramAnalysis(cid: ByteArray) {
    if (cid.size != 1) {
        Text(
            text = "Invalid CID length: expected 1 byte",
            color = MaterialTheme.colors.error,
            style = MaterialTheme.typography.body2
        )
        return
    }

    val cidValue = cid[0].toInt() and 0xFF
    val cryptogramType = when ((cidValue and 0xC0) shr 6) {
        0 -> "AAC"
        1 -> "TC"
        2 -> "ARQC"
        3 -> "RFU"
        else -> "Unknown"
    }

    val adviceRequired = (cidValue and 0x08) != 0
    val reasonCode = cidValue and 0x07

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        InfoRow("Type", cryptogramType)
        InfoRow("Advice Required", if (adviceRequired) "Yes" else "No")
        InfoRow("Reason Code", reasonCode.toString())
    }
}

@Composable
private fun GenericBitAnalysis(value: ByteArray) {
    if (value.size <= 4) {
        value.forEachIndexed { index, byte ->
            SimpleBitAnalysis(byte, "Byte ${index + 1}", emptyList())
        }
    } else {
        Text(
            text = "${value.size} bytes of data",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )

        Text(
            text = value.take(8).joinToString(" ") { "%02X".format(it) } +
                    if (value.size > 8) "..." else "",
            style = MaterialTheme.typography.body2.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun SimpleBitAnalysis(
    byteValue: Byte,
    title: String,
    bitDescriptions: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.body2.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colors.onSurface
            )
            Text(
                text = "0x${String.format("%02X", byteValue.toInt() and 0xFF)}",
                style = MaterialTheme.typography.caption.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }

        if (bitDescriptions.isNotEmpty()) {
            val binaryString =
                String.format("%8s", Integer.toBinaryString(byteValue.toInt() and 0xFF))
                    .replace(' ', '0')

            bitDescriptions.forEachIndexed { index, description ->
                val bitNumber = 7 - index
                val isSet = binaryString[index] == '1'

                if (isSet && description != "RFU") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$bitNumber",
                            style = MaterialTheme.typography.caption.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.width(12.dp)
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChildrenSection(tag: EMVTag) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        backgroundColor = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Child Tags (${tag.children.size})",
                style = MaterialTheme.typography.subtitle2.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colors.onSurface
            )

            Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f))

            tag.children.forEach { child ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = child.tag,
                            style = MaterialTheme.typography.body2.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colors.onSurface
                        )
                        if (child.description.isNotEmpty()) {
                            Text(
                                text = child.description,
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Text(
                        text = "${child.length}B",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }

                if (child != tag.children.last()) {
                    Divider(
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.05f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    fontFamily: FontFamily = FontFamily.Default
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )
        SelectionContainer {
            Text(
                text = value,
                style = MaterialTheme.typography.body2.copy(
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colors.onSurface,
                textAlign = TextAlign.End
            )
        }
    }
}

// Helper Functions
private fun shouldShowValueSection(tag: EMVTag): Boolean {
    // Don't show value section for simple fields that don't need explanation
    return when (tag.tag.uppercase()) {
        "5F2A", "9F1A", "9A", "9F21", "9F36", "9F13", "9F17", "9F41" -> false // Currency code, country code, date, time, counters
        else -> tag.value.isNotEmpty()
    }
}

private fun requiresDetailedAnalysis(tag: EMVTag): Boolean {
    // Only show bit analysis for tags that actually need detailed explanation
    return when (tag.tag.uppercase()) {
        "95", "9B", "82", "9F33", "8E", "9F34", "9F27" -> true
        else -> false
    }
}

private fun getTagClass(tag: String): String {
    if (tag.isEmpty()) return "Unknown"
    val firstByte = tag.substring(0, 2).toInt(16)
    return when ((firstByte and 0xC0) shr 6) {
        0 -> "Universal"
        1 -> "Application"
        2 -> "Context-specific"
        3 -> "Private"
        else -> "Unknown"
    }
}

private fun getCleanAscii(value: ByteArray): String {
    return try {
        String(value, Charsets.US_ASCII).filter {
            it.isLetterOrDigit() || it.isWhitespace() || it in ".,;:!?-_()[]{}@#$%^&*+=<>/"
        }
    } catch (e: Exception) {
        ""
    }
}

private fun getBcdValue(value: ByteArray): String {
    return try {
        val bcdValue = IsoUtil.bcdToString(value)
        if (bcdValue.all { it.isDigit() }) bcdValue else ""
    } catch (e: Exception) {
        ""
    }
}

private fun getInterpretation(tag: String, value: ByteArray): String {
    // Only provide interpretation for complex tags that need it
    return when (tag.uppercase()) {
        "95" -> "Terminal verification status"
        "9B" -> "Transaction processing status"
        "82" -> "Application capabilities"
        "9F27" -> "Cryptogram type and advice"
        "8E" -> "Cardholder verification methods"
        "9F34" -> "CVM processing result"
        else -> ""
    }
}

private fun getTVRBitDescriptions(byteIndex: Int): List<String> {
    return when (byteIndex) {
        0 -> listOf(
            "Offline data authentication not performed",
            "SDA failed",
            "ICC data missing",
            "Card on exception file",
            "DDA failed",
            "CDA failed",
            "SDA selected",
            "RFU"
        )

        1 -> listOf(
            "ICC and terminal different versions",
            "Expired application",
            "Application not yet effective",
            "Service not allowed",
            "New card",
            "RFU", "RFU", "RFU"
        )

        2 -> listOf(
            "Cardholder verification failed",
            "Unrecognised CVM",
            "PIN Try Limit exceeded",
            "PIN pad not present",
            "PIN not entered",
            "Online PIN entered",
            "RFU", "RFU"
        )

        3 -> listOf(
            "Transaction exceeds floor limit",
            "Lower offline limit exceeded",
            "Upper offline limit exceeded",
            "Random selection for online",
            "Merchant forced online",
            "RFU", "RFU", "RFU"
        )

        4 -> listOf(
            "Default TDOL used",
            "Issuer authentication failed",
            "Script failed before GENERATE AC",
            "Script failed after GENERATE AC",
            "RFU", "RFU", "RFU", "RFU"
        )

        else -> emptyList()
    }
}

private fun interpretCVMCode(code: Int): String {
    return when (code) {
        0x00 -> "Fail CVM"
        0x01 -> "Plaintext PIN (ICC)"
        0x02 -> "Enciphered PIN (online)"
        0x03 -> "Plaintext PIN + Signature"
        0x04 -> "Enciphered PIN (ICC)"
        0x05 -> "Enciphered PIN + Signature"
        0x1E -> "Signature"
        0x1F -> "No CVM required"
        else -> if (code in 0x80..0xBF) "Issuer defined" else "Unknown"
    }
}

private fun interpretCVMCondition(condition: Int): String {
    return when (condition) {
        0x00 -> "Always"
        0x01 -> "If unattended cash"
        0x02 -> "If not unattended cash/manual cash/cashback"
        0x03 -> "If terminal supports CVM"
        0x04 -> "If manual cash"
        0x05 -> "If purchase with cashback"
        0x06 -> "If under amount X"
        0x07 -> "If over amount X"
        0x08 -> "If under amount Y"
        0x09 -> "If over amount Y"
        else -> "Unknown"
    }
}

private fun interpretCVMResult(result: Int): String {
    return when (result) {
        0x00 -> "Unknown"
        0x01 -> "Failed"
        0x02 -> "Successful"
        else -> "Unknown"
    }
}
