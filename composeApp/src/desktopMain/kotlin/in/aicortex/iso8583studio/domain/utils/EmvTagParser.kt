package `in`.aicortex.iso8583studio.domain.utils

import ai.cortex.core.IsoUtil
import `in`.aicortex.iso8583studio.data.model.EMVShowOption
import kotlinx.serialization.Serializable
import java.nio.charset.StandardCharsets

/**
 * EMV Tag representation
 */
@Serializable
data class EMVTag(
    val tag: String,
    val length: Int,
    val value: ByteArray,
    val description: String = "",
    val isConstructed: Boolean = false,
    val children: List<EMVTag> = emptyList(),
    val rawData: ByteArray = byteArrayOf()
) {
    fun getValueAsString(): String = IsoUtil.bcdToString(value)
    fun getValueAsAscii(): String = String(value, StandardCharsets.US_ASCII)
    fun getValueAsHex(): String = value.joinToString("") { "%02X".format(it) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EMVTag
        return tag == other.tag && length == other.length && value.contentEquals(other.value)
    }

    override fun hashCode(): Int {
        var result = tag.hashCode()
        result = 31 * result + length
        result = 31 * result + value.contentHashCode()
        return result
    }
}



/**
 * Comprehensive EMV Tag Parser Utility
 * Handles all types of EMV tags including primitive, constructed, and template tags
 * Supports BER-TLV, proprietary formats, and various EMV data structures
 */
object EMVTagParser {


    /**
     * Parse result containing all parsed tags and metadata
     */
    data class EMVParseResult(
        val tags: List<EMVTag>,
        val totalBytesProcessed: Int,
        val errors: List<String> = emptyList(),
        val warnings: List<String> = emptyList()
    )

    /**
     * Main parsing method - handles all EMV tag formats
     */
    fun parseEMVTags(
        data: ByteArray,
        offset: Int = 0,
        showOption: EMVShowOption = EMVShowOption.NAME
    ): EMVParseResult {
        val tags = mutableListOf<EMVTag>()
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        var position = offset

        try {
            while (position < data.size) {
                val parseResult = parseNextTag(data, position)

                if (parseResult != null) {
                    tags.add(parseResult.first)
                    position = parseResult.second
                } else {
                    // Skip invalid byte and continue
                    warnings.add("Skipped invalid byte at position $position: 0x${String.format("%02X", data[position])}")
                    position++
                }
            }
        } catch (e: Exception) {
            errors.add("Parsing error at position $position: ${e.message}")
        }

        return EMVParseResult(
            tags = filterTagsByShowOption(tags, showOption),
            totalBytesProcessed = position - offset,
            errors = errors,
            warnings = warnings
        )
    }

    /**
     * Parse a single EMV tag starting at the given position
     * Returns Pair<EMVTag, NextPosition> or null if parsing fails
     */
    private fun parseNextTag(data: ByteArray, position: Int): Pair<EMVTag, Int>? {
        if (position >= data.size) return null

        try {
            // Parse tag
            val tagResult = parseTagIdentifier(data, position)
            val tag = tagResult.first
            var currentPos = tagResult.second

            // Parse length
            val lengthResult = parseLength(data, currentPos)
            val length = lengthResult.first
            currentPos = lengthResult.second

            // Validate length
            if (currentPos + length > data.size) {
                return null // Invalid length
            }

            // Extract value
            val value = data.copyOfRange(currentPos, currentPos + length)
            val rawData = data.copyOfRange(position, currentPos + length)

            // Determine if tag is constructed
            val isConstructed = isConstructedTag(tag)

            // Parse children if constructed
            val children = if (isConstructed && length > 0) {
                parseEMVTags(value, 0).tags
            } else {
                emptyList()
            }

            val emvTag = EMVTag(
                tag = tag,
                length = length,
                value = value,
                description = getTagDescription(tag),
                isConstructed = isConstructed,
                children = children,
                rawData = rawData
            )

            return Pair(emvTag, currentPos + length)
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Parse tag identifier (supports 1-4 byte tags)
     */
    private fun parseTagIdentifier(data: ByteArray, position: Int): Pair<String, Int> {
        if (position >= data.size) throw IllegalArgumentException("Invalid position")

        val firstByte = data[position].toInt() and 0xFF
        var currentPos = position + 1
        val tagBytes = mutableListOf<Byte>()
        tagBytes.add(data[position])

        // Check if this is a multi-byte tag
        if ((firstByte and 0x1F) == 0x1F) {
            // Multi-byte tag
            while (currentPos < data.size) {
                val nextByte = data[currentPos].toInt() and 0xFF
                tagBytes.add(data[currentPos])
                currentPos++

                // If bit 8 is 0, this is the last byte of the tag
                if ((nextByte and 0x80) == 0) break

                // Prevent infinite loop
                if (tagBytes.size > 4) break
            }
        }

        val tag = tagBytes.map { String.format("%02X", it.toInt() and 0xFF) }.joinToString("")
        return Pair(tag, currentPos)
    }

    /**
     * Parse length field (supports definite and indefinite forms)
     */
    private fun parseLength(data: ByteArray, position: Int): Pair<Int, Int> {
        if (position >= data.size) throw IllegalArgumentException("Invalid position")

        val firstByte = data[position].toInt() and 0xFF
        var currentPos = position + 1

        return when {
            // Short form (0-127)
            (firstByte and 0x80) == 0 -> {
                Pair(firstByte, currentPos)
            }
            // Long form
            firstByte == 0x80 -> {
                // Indefinite form (not typically used in EMV)
                throw IllegalArgumentException("Indefinite length not supported")
            }
            else -> {
                // Long definite form
                val lengthBytes = firstByte and 0x7F
                if (lengthBytes > 4 || currentPos + lengthBytes > data.size) {
                    throw IllegalArgumentException("Invalid length encoding")
                }

                var length = 0
                for (i in 0 until lengthBytes) {
                    length = (length shl 8) or (data[currentPos + i].toInt() and 0xFF)
                }

                Pair(length, currentPos + lengthBytes)
            }
        }
    }

    /**
     * Check if a tag represents a constructed data object
     */
    private fun isConstructedTag(tag: String): Boolean {
        if (tag.isEmpty()) return false

        val firstByte = tag.substring(0, 2).toInt(16)
        return (firstByte and 0x20) != 0 || isKnownConstructedTag(tag)
    }

    /**
     * Check if tag is known to be constructed based on EMV specifications
     */
    private fun isKnownConstructedTag(tag: String): Boolean {
        return when (tag.uppercase()) {
            "70", "77", "E1", "E2", "E3", "E4", "E5", "E6" -> true // EMV templates
            "61", "6F", "A5", "BF0C" -> true // FCI templates
            "73", "72" -> true // Directory discretionary templates
            else -> false
        }
    }

    /**
     * Filter tags based on show option
     */
    private fun filterTagsByShowOption(tags: List<EMVTag>, showOption: EMVShowOption): List<EMVTag> {
        return when (showOption) {
            EMVShowOption.None -> emptyList()
            EMVShowOption.NAME -> tags
            EMVShowOption.DESCRIPTION -> tags.filter { getTagDescription(it.tag).isNotEmpty() }
            EMVShowOption.Len -> tags.filter { getTagDescription(it.tag).isEmpty() }
            else -> { emptyList()}
        }
    }

    /**
     * Get human-readable description for EMV tags
     */
    fun getTagDescription(tag: String): String {
        return emvTagDescriptions[tag.uppercase()] ?: ""
    }

    /**
     * Format EMV tags for display
     */
    fun formatEMVTags(
        parseResult: EMVParseResult,
        includeHex: Boolean = true,
        includeAscii: Boolean = true,
        maxDepth: Int = 10
    ): String {
        return buildString {
            appendLine("=== EMV Tag Analysis ===")
            appendLine("Total tags found: ${parseResult.tags.size}")
            appendLine("Bytes processed: ${parseResult.totalBytesProcessed}")

            if (parseResult.errors.isNotEmpty()) {
                appendLine("\nErrors:")
                parseResult.errors.forEach { appendLine("  - $it") }
            }

            if (parseResult.warnings.isNotEmpty()) {
                appendLine("\nWarnings:")
                parseResult.warnings.forEach { appendLine("  - $it") }
            }

            appendLine("\n=== Tag Details ===")
            parseResult.tags.forEach { tag ->
                append(formatTag(tag, 0, includeHex, includeAscii, maxDepth))
            }
        }
    }

    /**
     * Format a single tag with proper indentation
     */
    fun formatTag(
        tag: EMVTag,
        depth: Int,
        includeHex: Boolean,
        includeAscii: Boolean,
        maxDepth: Int
    ): String {
        if (depth > maxDepth) return ""

        val indent = "  ".repeat(depth)
        return buildString {
            appendLine("${indent}Tag: ${tag.tag} [${tag.length} bytes]")

            val description = tag.description.ifEmpty { "Unknown tag" }
            appendLine("${indent}Description: $description")

            if (tag.isConstructed) {
                appendLine("${indent}Type: Constructed")
                if (tag.children.isNotEmpty()) {
                    appendLine("${indent}Children:")
                    tag.children.forEach { child ->
                        append(formatTag(child, depth + 1, includeHex, includeAscii, maxDepth))
                    }
                }
            } else {
                appendLine("${indent}Type: Primitive")

                if (includeHex && tag.value.isNotEmpty()) {
                    appendLine("${indent}Value (Hex): ${tag.getValueAsHex()}")
                }

                if (includeAscii && tag.value.isNotEmpty()) {
                    val asciiValue = try {
                        tag.getValueAsAscii().filter { it.isLetterOrDigit() || it.isWhitespace() || it in ".,;:!?-_()[]{}@#$%^&*+=<>/" }
                    } catch (e: Exception) { "" }

                    if (asciiValue.isNotEmpty()) {
                        appendLine("${indent}Value (ASCII): $asciiValue")
                    }
                }

                // Add interpretation for specific tags
                val interpretation = interpretTagValue(tag.tag, tag.value)
                if (interpretation.isNotEmpty()) {
                    appendLine("${indent}Interpretation: $interpretation")
                }
            }

            appendLine()
        }
    }

    /**
     * Interpret specific tag values based on EMV specifications
     */
    internal fun interpretTagValue(tag: String, value: ByteArray): String {
        if (value.isEmpty()) return ""

        return when (tag.uppercase()) {
            "95" -> interpretTVR(value)
            "9B" -> interpretTSI(value)
            "82" -> interpretAIP(value)
            "84" -> "AID: ${value.joinToString("") { "%02X".format(it) }}"
            "5A" -> "PAN: ${IsoUtil.bcdToString(value)}"
            "5F24" -> interpretExpiryDate(value)
            "9F02" -> "Amount: ${IsoUtil.bcdToString(value)}"
            "9F03" -> "Other Amount: ${IsoUtil.bcdToString(value)}"
            "9F1A" -> "Terminal Country Code: ${String(value, StandardCharsets.US_ASCII)}"
            "5F2A" -> "Transaction Currency Code: ${String(value, StandardCharsets.US_ASCII)}"
            "9A" -> interpretTransactionDate(value)
            "9F21" -> interpretTransactionTime(value)
            else -> ""
        }
    }

    /**
     * Interpret Terminal Verification Results (TVR)
     */
    private fun interpretTVR(tvr: ByteArray): String {
        if (tvr.size != 5) return "Invalid TVR length"

        val results = mutableListOf<String>()
        val byte1 = tvr[0].toInt() and 0xFF
        val byte2 = tvr[1].toInt() and 0xFF
        val byte3 = tvr[2].toInt() and 0xFF
        val byte4 = tvr[3].toInt() and 0xFF
        val byte5 = tvr[4].toInt() and 0xFF

        // Byte 1
        if ((byte1 and 0x80) != 0) results.add("Offline data authentication not performed")
        if ((byte1 and 0x40) != 0) results.add("SDA failed")
        if ((byte1 and 0x20) != 0) results.add("ICC data missing")
        if ((byte1 and 0x10) != 0) results.add("Card on exception file")
        if ((byte1 and 0x08) != 0) results.add("DDA failed")
        if ((byte1 and 0x04) != 0) results.add("CDA failed")

        // Byte 2
        if ((byte2 and 0x80) != 0) results.add("ICC and terminal have different application versions")
        if ((byte2 and 0x40) != 0) results.add("Expired application")
        if ((byte2 and 0x20) != 0) results.add("Application not yet effective")
        if ((byte2 and 0x10) != 0) results.add("Requested service not allowed for card product")
        if ((byte2 and 0x08) != 0) results.add("New card")

        return if (results.isEmpty()) "All verifications passed" else results.joinToString("; ")
    }

    /**
     * Interpret Transaction Status Information (TSI)
     */
    private fun interpretTSI(tsi: ByteArray): String {
        if (tsi.size != 2) return "Invalid TSI length"

        val results = mutableListOf<String>()
        val byte1 = tsi[0].toInt() and 0xFF
        val byte2 = tsi[1].toInt() and 0xFF

        if ((byte1 and 0x80) != 0) results.add("Offline data authentication performed")
        if ((byte1 and 0x40) != 0) results.add("Cardholder verification performed")
        if ((byte1 and 0x20) != 0) results.add("Card risk management performed")
        if ((byte1 and 0x10) != 0) results.add("Issuer authentication performed")
        if ((byte1 and 0x08) != 0) results.add("Terminal risk management performed")
        if ((byte1 and 0x04) != 0) results.add("Script processing performed")

        return results.joinToString("; ")
    }

    /**
     * Interpret Application Interchange Profile (AIP)
     */
    private fun interpretAIP(aip: ByteArray): String {
        if (aip.size != 2) return "Invalid AIP length"

        val results = mutableListOf<String>()
        val byte1 = aip[0].toInt() and 0xFF
        val byte2 = aip[1].toInt() and 0xFF

        if ((byte1 and 0x40) != 0) results.add("SDA supported")
        if ((byte1 and 0x20) != 0) results.add("DDA supported")
        if ((byte1 and 0x10) != 0) results.add("Cardholder verification supported")
        if ((byte1 and 0x08) != 0) results.add("Terminal risk management to be performed")
        if ((byte1 and 0x04) != 0) results.add("Issuer authentication supported")
        if ((byte1 and 0x02) != 0) results.add("CDA supported")

        return results.joinToString("; ")
    }

    /**
     * Interpret expiry date (YYMM format)
     */
    private fun interpretExpiryDate(date: ByteArray): String {
        return try {
            val dateStr = IsoUtil.bcdToString(date)
            if (dateStr.length >= 4) {
                val year = "20${dateStr.substring(0, 2)}"
                val month = dateStr.substring(2, 4)
                "Expires: $month/$year"
            } else {
                "Invalid date format"
            }
        } catch (e: Exception) {
            "Invalid date"
        }
    }

    /**
     * Interpret transaction date (YYMMDD format)
     */
    private fun interpretTransactionDate(date: ByteArray): String {
        return try {
            val dateStr = IsoUtil.bcdToString(date)
            if (dateStr.length >= 6) {
                val year = "20${dateStr.substring(0, 2)}"
                val month = dateStr.substring(2, 4)
                val day = dateStr.substring(4, 6)
                "Date: $day/$month/$year"
            } else {
                "Invalid date format"
            }
        } catch (e: Exception) {
            "Invalid date"
        }
    }

    /**
     * Interpret transaction time (HHMMSS format)
     */
    private fun interpretTransactionTime(time: ByteArray): String {
        return try {
            val timeStr = IsoUtil.bcdToString(time)
            if (timeStr.length >= 6) {
                val hour = timeStr.substring(0, 2)
                val minute = timeStr.substring(2, 4)
                val second = timeStr.substring(4, 6)
                "Time: $hour:$minute:$second"
            } else {
                "Invalid time format"
            }
        } catch (e: Exception) {
            "Invalid time"
        }
    }

    /**
     * Comprehensive EMV tag descriptions database
     */
    private val emvTagDescriptions = mapOf(
        // Payment System Environment
        "4F" to "Application Identifier (AID)",
        "50" to "Application Label",
        "87" to "Application Priority Indicator",
        "61" to "Application Template",
        "6F" to "File Control Information (FCI) Template",
        "84" to "Dedicated File (DF) Name",
        "A5" to "File Control Information (FCI) Proprietary Template",
        "88" to "Short File Identifier (SFI)",
        "5F2D" to "Language Preference",
        "9F11" to "Issuer Code Table Index",
        "9F12" to "Application Preferred Name",
        "BF0C" to "File Control Information (FCI) Issuer Discretionary Data",

        // Cardholder, Application, and Issuer Data
        "5A" to "Application Primary Account Number (PAN)",
        "5F34" to "Application Primary Account Number (PAN) Sequence Number",
        "5F24" to "Application Expiration Date",
        "5F25" to "Application Effective Date",
        "5F20" to "Cardholder Name",
        "9F0B" to "Cardholder Name Extended",
        "57" to "Track 2 Equivalent Data",
        "9F1F" to "Track 1 Discretionary Data",
        "9F20" to "Track 2 Discretionary Data",
        "56" to "Track 1 Data",

        // Application Data
        "82" to "Application Interchange Profile",
        "94" to "Application File Locator (AFL)",
        "8C" to "Card Risk Management Data Object List 1 (CDOL1)",
        "8D" to "Card Risk Management Data Object List 2 (CDOL2)",
        "8E" to "Cardholder Verification Method (CVM) List",
        "8F" to "Certification Authority Public Key Index",
        "90" to "Issuer Public Key Certificate",
        "92" to "Issuer Public Key Remainder",
        "93" to "Signed Static Application Data",
        "9F32" to "Issuer Public Key Exponent",
        "9F46" to "ICC Public Key Certificate",
        "9F47" to "ICC Public Key Exponent",
        "9F48" to "ICC Public Key Remainder",
        "9F49" to "Dynamic Data Authentication Data Object List (DDOL)",

        // Terminal Data
        "9F1A" to "Terminal Country Code",
        "9F35" to "Terminal Type",
        "9F33" to "Terminal Capabilities",
        "9F40" to "Additional Terminal Capabilities",
        "9F1C" to "Terminal Identification",
        "9F1E" to "Interface Device (IFD) Serial Number",
        "9F15" to "Merchant Category Code",
        "9F16" to "Merchant Identifier",
        "9F4E" to "Merchant Name and Location",

        // Transaction Data
        "9F02" to "Amount, Authorised (Numeric)",
        "9F03" to "Amount, Other (Numeric)",
        "9F04" to "Amount, Other (Binary)",
        "9F06" to "Application Identifier (AID) - Terminal",
        "9A" to "Transaction Date",
        "9F21" to "Transaction Time",
        "9C" to "Transaction Type",
        "5F2A" to "Transaction Currency Code",
        "5F36" to "Transaction Currency Exponent",
        "9F1A" to "Terminal Country Code",
        "95" to "Terminal Verification Results",
        "9B" to "Transaction Status Information",
        "9F10" to "Issuer Application Data",
        "9F26" to "Application Cryptogram",
        "9F27" to "Cryptogram Information Data",
        "9F36" to "Application Transaction Counter (ATC)",
        "9F13" to "Last Online Application Transaction Counter (ATC) Register",
        "9F17" to "Personal Identification Number (PIN) Try Counter",
        "9F41" to "Transaction Sequence Counter",

        // Processing and Security Data
        "91" to "Issuer Authentication Data",
        "71" to "Issuer Script Template 1",
        "72" to "Issuer Script Template 2",
        "9F18" to "Issuer Script Identifier",
        "86" to "Issuer Script Command",
        "9F5B" to "Issuer Script Results",

        // Card Data Management
        "70" to "EMV Proprietary Template",
        "77" to "Response Message Template Format 2",
        "80" to "Response Message Template Format 1",
        "73" to "Directory Discretionary Template",

        // Proprietary and Additional Tags
        "9F7C" to "Customer Exclusive Data",
        "DF01" to "Proprietary Data Element",
        "C1" to "Application Control",
        "C2" to "Application Control",
        "C3" to "Card Data Input Capability",
        "C4" to "CVM Capability - CVM Required",
        "C5" to "CVM Capability - No CVM Required",
        "C6" to "CVM Capability - No CVM Required",

        // Contactless specific
        "9F66" to "Terminal Transaction Qualifiers (TTQ)",
        "9F6C" to "Card Transaction Qualifiers (CTQ)",
        "9F02" to "Amount Authorised",
        "9F03" to "Amount Other",
        "9F1A" to "Terminal Country Code",
        "5F2A" to "Transaction Currency Code",
        "9A" to "Transaction Date",
        "9C" to "Transaction Type",
        "9F37" to "Unpredictable Number",
        "9F6E" to "Third Party Data",
        "9F7A" to "VLP Funds Limit",
        "9F7B" to "VLP Single Transaction Limit",

        // Additional EMV tags
        "E1" to "Template E1",
        "E2" to "Template E2",
        "E3" to "Template E3",
        "E4" to "Template E4",
        "E5" to "Template E5",
        "E6" to "Template E6"
    )

    /**
     * Extract specific tag from parsed results
     */
    fun findTag(parseResult: EMVParseResult, tagToFind: String): EMVTag? {
        return findTagRecursive(parseResult.tags, tagToFind.uppercase())
    }

    private fun findTagRecursive(tags: List<EMVTag>, tagToFind: String): EMVTag? {
        for (tag in tags) {
            if (tag.tag.uppercase() == tagToFind) {
                return tag
            }
            if (tag.isConstructed) {
                val found = findTagRecursive(tag.children, tagToFind)
                if (found != null) return found
            }
        }
        return null
    }

    /**
     * Get all tags of a specific type
     */
    fun findAllTags(parseResult: EMVParseResult, tagToFind: String): List<EMVTag> {
        val results = mutableListOf<EMVTag>()
        findAllTagsRecursive(parseResult.tags, tagToFind.uppercase(), results)
        return results
    }

    /**
     * Get all tags of a specific type
     */
    fun getAllTags(parseResult: EMVParseResult): List<EMVTag> {
        val results = mutableListOf<EMVTag>()
        emvTagDescriptions.forEach {
            findAllTagsRecursive(parseResult.tags, it.key.uppercase(), results)
        }

        return results
    }

    private fun findAllTagsRecursive(tags: List<EMVTag>, tagToFind: String, results: MutableList<EMVTag>) {
        for (tag in tags) {
            if (tag.tag.uppercase() == tagToFind) {
                results.add(tag)
            }
            if (tag.isConstructed) {
                findAllTagsRecursive(tag.children, tagToFind, results)
            }
        }
    }
}

