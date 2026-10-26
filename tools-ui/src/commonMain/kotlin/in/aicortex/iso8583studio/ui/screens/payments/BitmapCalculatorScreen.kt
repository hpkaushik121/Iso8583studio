package `in`.aicortex.iso8583studio.ui.screens.payments

import `in`.aicortex.iso8583studio.ui.chrome.toolScroll

import `in`.aicortex.iso8583studio.ui.chrome.ToolScreenSpec

import `in`.aicortex.iso8583studio.ui.chrome.LocalToolChrome

import androidx.compose.ui.text.AnnotatedString

import androidx.compose.ui.platform.LocalClipboardManager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
// --- BITMAP SCREEN ---

private object BitmapHelper {
    /** Bits from a 16- or 32-hex-digit bitmap; bit 1 is the most significant bit of the first byte. */
    fun fromHex(hex: String): List<Boolean> {
        val h = hex.filter { !it.isWhitespace() }.uppercase()
        require(h.length == 16 || h.length == 32) { "Bitmap must be 16 (primary) or 32 (primary + secondary) hex digits" }
        require(h.all { it.isDigit() || it in 'A'..'F' }) { "Bitmap must be hexadecimal" }
        val bits = h.flatMap { c -> c.digitToInt(16).let { v -> listOf(v and 8 != 0, v and 4 != 0, v and 2 != 0, v and 1 != 0) } }
        return bits + List(128 - bits.size) { false }
    }

    fun toHex(bits: List<Boolean>): String {
        val hasSecondaryBitmap = bits.getOrElse(0) { false }
        val bitsToConvert = if (hasSecondaryBitmap) bits else bits.take(64)

        if (bitsToConvert.none { it }) return ""

        val binaryString = bitsToConvert.joinToString("") { if (it) "1" else "0" }

        return binaryString.chunked(4).joinToString("") { it.toInt(2).toString(16) }.uppercase().padStart(if (hasSecondaryBitmap) 32 else 16, '0')
    }
}

@Composable
fun BitmapScreen(
    
    onBack: () -> Unit
) {
    val bits = remember { mutableStateListOf<Boolean>().apply { addAll(List(128) { false }) } }
    val hexBitmap by remember { derivedStateOf { BitmapHelper.toHex(bits) } }
    val clipboard = LocalClipboardManager.current
    var pasteHex by remember { mutableStateOf("") }
    var pasteError by remember { mutableStateOf<String?>(null) }
    val fieldsSet by remember { derivedStateOf { bits.withIndex().filter { it.value && it.index > 0 }.map { it.index + 1 } } }

    fun clearAll() {
        for (i in 0 until 128) {
            bits[i] = false
        }
    }

    fun handleBitToggle(index: Int) {
        val newValue = !bits[index]
        bits[index] = newValue

        // If a secondary bit (65-128) is turned on, ensure bit 1 is also on.
        if (newValue && index >= 64) {
            bits[0] = true
        }

        // If bit 1 is turned off, clear all secondary bits.
        if (index == 0 && !newValue) {
            for (i in 64 until 128) {
                bits[i] = false
            }
        }
    }

    val body: @Composable () -> Unit = {
            // --- Output Card ---
            SectionHeader(title = "Calculated Bitmap", icon = Icons.Default.DataObject)
            FixedOutlinedTextField(
                value = hexBitmap,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                placeholder = { Text("Hex representation will appear here")},
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                trailingIcon = {
                    IconButton(onClick = { if (hexBitmap.isNotEmpty()) clipboard.setText(AnnotatedString(hexBitmap)) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy to Clipboard")
                    }
                }
            )
            Text(
                text = if (fieldsSet.isEmpty()) "No fields set" else "Fields present: ${fieldsSet.joinToString(", ")}" + if (bits[0]) " (secondary bitmap present)" else "",
                style = MaterialTheme.typography.caption,
                fontFamily = FontFamily.Monospace
            )

            SectionHeader(title = "Load from Hex", icon = Icons.Default.ContentPaste)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FixedOutlinedTextField(
                    value = pasteHex,
                    onValueChange = { pasteHex = it.uppercase(); pasteError = null },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Paste a 16- or 32-digit hex bitmap") },
                    isError = pasteError != null,
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                    singleLine = true
                )
                Button(onClick = {
                    try {
                        val parsed = BitmapHelper.fromHex(pasteHex)
                        for (i in 0 until 128) bits[i] = parsed[i]
                        pasteError = null
                    } catch (e: Exception) { pasteError = e.message }
                }, enabled = pasteHex.isNotBlank()) { Text("Apply") }
            }
            if (pasteError != null) Text(pasteError!!, color = MaterialTheme.colors.error, style = MaterialTheme.typography.caption)

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // --- Two-Column Layout for Bitmaps ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Primary Bitmap
                Column(modifier = Modifier.weight(1f)) {
                    SectionHeader(title = "Primary Bitmap (1-64)")
                    BitmapGrid(
                        range = 1..64,
                        bits = bits,
                        onBitToggle = ::handleBitToggle
                    )
                }

                // Right Column: Secondary Bitmap
                Column(modifier = Modifier.weight(1f)) {
                    SectionHeader(title = "Secondary Bitmap (65-128)")
                    BitmapGrid(
                        range = 65..128,
                        bits = bits,
                        onBitToggle = ::handleBitToggle
                    )
                }
            }
        
    }
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) {
        chrome.Screen(
            ToolScreenSpec(
                title = "Bitmap Calculator", onBack = onBack, log = null,
                actions = { IconButton(onClick = ::clearAll) { Icon(Icons.Default.Refresh, contentDescription = "Clear All") } },
            )
        ) {
            Column(modifier = Modifier.fillMaxSize().toolScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) { body() }
        }
        return
    }
    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "Bitmap Calculator",
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = ::clearAll) {
                        Icon(Icons.Default.Refresh, contentDescription = "Clear All")
                    }
                }
            )
        },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .toolScroll(rememberScrollState()), // Keep scroll for smaller window sizes
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            body()
        }
    }
}

@Composable
private fun BitmapGrid(
    range: IntRange,
    bits: List<Boolean>,
    onBitToggle: (Int) -> Unit
) {
    Card(
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha=0.12f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp), // Reduced padding
            verticalArrangement = Arrangement.spacedBy(4.dp) // Reduced spacing
        ) {
            val fields = range.toList()
            val rows = fields.chunked(8)
            rows.forEach { rowFields ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp) // Reduced spacing
                ) {
                    rowFields.forEach { fieldNumber ->
                        val index = fieldNumber - 1
                        BitmapBit(
                            fieldNumber = fieldNumber,
                            isSelected = bits.getOrElse(index) { false },
                            onClick = { onBitToggle(index) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BitmapBit(
    fieldNumber: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.surface
    val contentColor = if (isSelected) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
    val border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colors.primary.copy(alpha=0.5f) else MaterialTheme.colors.onSurface.copy(alpha = 0.12f))

    Card(
        modifier = modifier
            .height(40.dp) // Set a fixed height to make tiles smaller
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp), // Slightly smaller radius
        backgroundColor = backgroundColor,
        border = border,
        elevation = if (isSelected) 4.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = fieldNumber.toString(),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp, // Smaller font
                color = contentColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

// --- SHARED UI COMPONENTS (PRIVATE TO THIS FILE) ---

@Composable
private fun SectionHeader(title: String, icon: ImageVector? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)) {
        if(icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.SemiBold
        )
    }
}
