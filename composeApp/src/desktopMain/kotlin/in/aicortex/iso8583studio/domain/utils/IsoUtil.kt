package `in`.aicortex.iso8583studio.domain.utils

import `in`.aicortex.iso8583studio.data.model.BitLength
import `in`.aicortex.iso8583studio.data.model.BitType
import `in`.aicortex.iso8583studio.data.model.CipherType
import `in`.aicortex.iso8583studio.data.model.MessageLengthType
import java.nio.charset.Charset


/**
 * Helper class for IsoUtil functions
 */
object IsoUtil {
    fun getBytesFromBytes(source: ByteArray, offset: Int, length: Int): ByteArray {
        return source.copyOfRange(offset, offset + length)
    }

    fun messageLengthToInt(bytes: ByteArray, type: MessageLengthType): Int {
        // Implementation depends on the specific format used
        return when (type) {
            MessageLengthType.BCD -> {
                // BCD decoding logic
                val value = bytes[0].toInt() and 0xFF
                value * 100 + ((bytes[1].toInt() and 0xF0) shr 4) * 10 + (bytes[1].toInt() and 0x0F)
            }

            MessageLengthType.HEX_HL -> {
                // High byte, Low byte format
                (bytes[0].toInt() and 0xFF) * 256 + (bytes[1].toInt() and 0xFF)
            }

            MessageLengthType.HEX_LH -> {
                // Low byte, High byte format
                (bytes[1].toInt() and 0xFF) * 256 + (bytes[0].toInt() and 0xFF)
            }

            MessageLengthType.NONE -> TODO()
            MessageLengthType.STRING_4 -> TODO()
        }
    }

    fun intToMessageLength(value: Int, type: MessageLengthType): ByteArray {
        val result = ByteArray(2)
        when (type) {
            MessageLengthType.BCD -> {
                // BCD encoding logic
                result[0] = (value / 100).toByte()
                result[1] = (((value % 100) / 10) shl 4 or (value % 10)).toByte()
            }

            MessageLengthType.HEX_HL -> {
                // High byte, Low byte format
                result[0] = (value shr 8).toByte()
                result[1] = (value and 0xFF).toByte()
            }

            MessageLengthType.HEX_LH -> {
                // Low byte, High byte format
                result[0] = (value and 0xFF).toByte()
                result[1] = (value shr 8).toByte()
            }

            MessageLengthType.NONE -> TODO()
            MessageLengthType.STRING_4 -> TODO()
        }
        return result
    }

    fun bytesCopy(
        source: ByteArray,
        destination: ByteArray,
        sourcePos: Int,
        destPos: Int,
        length: Int
    ) {
        source.copyInto(destination, destPos, sourcePos, sourcePos + length)
    }

    fun ascToString(data: ByteArray): String {
        return String(data, Charset.defaultCharset())
    }

    /**
     * Converts a BCD (Binary Coded Decimal) byte array to a hexadecimal string.
     *
     * @param bcdBytes The BCD byte array to convert
     * @return String containing the hexadecimal representation
     */
    fun bcdToString(bcdBytes: ByteArray): String {
        return bcdBytes.joinToString("") { byte ->
            "%02X".format(byte.toInt() and 0xFF)
        }
    }
    fun bytesToHexString(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02X", b))
        }
        return sb.toString()
    }

    fun bytesToHexString(data: ByteArray, columnCount: Int, showAscii: Boolean): String {
        val sb = StringBuilder()
        for (i in data.indices) {
            val hex = data[i].toInt() and 0xFF
            sb.append(String.format("%02X ", hex))

            if ((i + 1) % (columnCount / 2) == 0) {
                if (showAscii) {
                    sb.append("  ")
                    for (j in i - (columnCount / 2) + 1..i) {
                        val ch = data[j].toInt() and 0xFF
                        sb.append(if (ch in 32..126) ch.toChar() else '.')
                    }
                }
                sb.append("\n")
            }
        }
        return sb.toString()
    }

    fun kvc(key: ByteArray, cipherType: CipherType): ByteArray {
        // Placeholder for KVC calculation
        return ByteArray(3)
    }

    fun creatBytesFromArray(src: ByteArray, offset: Int, length: Int): ByteArray {
        return src.copyOfRange(offset, offset + length)
    }


    fun bytesEqualled(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        for (i in a.indices) {
            if (a[i] != b[i]) return false
        }
        return true
    }


    fun convertToTIDIALERRule(buf: ByteArray, lengthType: MessageLengthType): ByteArray {
        val result = ByteArray(buf.size + 2) // STX + ETX
        result[0] = 2 // STX
        buf.copyInto(result, 1, 0, buf.size)
        result[result.size - 1] = 3 // ETX
        return result
    }

    /**
     * Converts a hexadecimal string to BCD (Binary Coded Decimal) format
     *
     * @param str The hexadecimal string to convert
     * @param length The length of the resulting byte array
     * @return ByteArray containing the BCD representation
     */
    fun stringToBCD(str: String, length: Int): ByteArray {
        val result = ByteArray(length)

        // Convert non-hex characters to their ASCII values first
        val hexStr = str.map { char ->
            if (char.isDigit() || char.uppercaseChar() in 'A'..'F') {
                char.toString()
            } else {
                // Convert to hex representation of ASCII value
                char.code.toString(16).uppercase()
            }
        }.joinToString("")

        val paddedStr = hexStr.padStart(length * 2, '0')

        for (i in 0 until length) {
            val highNibble = Character.digit(paddedStr[i * 2], 16)
            val lowNibble = Character.digit(paddedStr[i * 2 + 1], 16)

            result[i] = ((highNibble shl 4) or lowNibble).toByte()
        }

        return result
    }


    /**
     * Alternative implementation of bcdToString that handles special BCD format
     * where odd length data can have a padding nibble
     *
     * @param bytes The BCD byte array to convert
     * @param oddLength Set to true if the BCD represents an odd number of digits
     * @return String representation in hexadecimal
     */
    fun bcdToString(bytes: ByteArray, oddLength: Boolean = false): String {
        val result = StringBuilder(bytes.size * 2)

        for (i in bytes.indices) {
            // For odd length, the last byte has padding in the low nibble
            if (oddLength && i == bytes.size - 1) {
                val highNibble = (bytes[i].toInt() shr 4) and 0xF
                result.append(highNibble.toString(16).uppercase())
            } else {
                val highNibble = (bytes[i].toInt() shr 4) and 0xF
                val lowNibble = bytes[i].toInt() and 0xF

                result.append(highNibble.toString(16).uppercase())
                result.append(lowNibble.toString(16).uppercase())
            }
        }

        return result.toString()
    }

    /**
     * Converts BCD (Binary Coded Decimal) formatted bytes to a binary integer
     *
     * @param bcdBytes The BCD formatted bytes
     * @return The binary integer value
     */
    fun bcdToBin(bcdBytes: ByteArray): Int {
        var result = 0

        for (i in bcdBytes.indices) {
            val byte = bcdBytes[i].toInt() and 0xFF
            val high = (byte shr 4) and 0x0F
            val low = byte and 0x0F

            result = result * 100 + high * 10 + low
        }

        return result
    }

    /**
     * Overload that takes a specific length or region of BCD bytes from a larger array
     *
     * @param bcdBytes The array containing BCD formatted bytes
     * @param offset The starting position in the array
     * @param length The number of bytes to process
     * @return The binary integer value
     */
    fun bcdToBin(bcdBytes: ByteArray, offset: Int, length: Int): Int {
        var result = 0
        val end = minOf(offset + length, bcdBytes.size)

        for (i in offset until end) {
            val byte = bcdBytes[i].toInt() and 0xFF
            val high = (byte shr 4) and 0x0F
            val low = byte and 0x0F

            result = result * 100 + high * 10 + low
        }

        return result
    }

    /**
     * Converts a BCD nibble (4 bits) to a decimal digit
     *
     * @param nibble The BCD nibble (0-15)
     * @return The decimal digit (0-9)
     * @throws IllegalArgumentException if the nibble is not a valid BCD digit (0-9)
     */
    fun bcdNibbleToBin(nibble: Int): Int {
        if (nibble < 0 || nibble > 9) {
            throw IllegalArgumentException("Invalid BCD nibble: $nibble")
        }
        return nibble
    }

    /**
     * Converts a BCD value to an ASCII digit character
     *
     * @param bcd The BCD value (0-9)
     * @return The ASCII character representation ('0'-'9')
     */
    fun bcdToAscii(bcd: Int): Char {
        return (bcd + '0'.toInt()).toChar()
    }

    /**
     * Converts BCD bytes to a string of decimal digits
     *
     * @param bcdBytes The BCD formatted bytes
     * @param offset The starting position in the array
     * @param length The number of bytes to process
     * @return String representation of the BCD bytes
     */
    fun bcdToString(bcdBytes: ByteArray, offset: Int = 0, length: Int = bcdBytes.size - offset): String {
        val result = StringBuilder(length * 2)
        val end = minOf(offset + length, bcdBytes.size)

        for (i in offset until end) {
            val byte = bcdBytes[i].toInt() and 0xFF
            result.append(bcdToAscii((byte shr 4) and 0x0F))
            result.append(bcdToAscii(byte and 0x0F))
        }

        return result.toString()
    }

    /**
     * Converts a binary integer to BCD formatted bytes
     *
     * @param value The binary integer to convert
     * @param length The number of bytes to produce (default: calculated based on value)
     * @return The BCD formatted bytes
     */
    fun binToBcd(value: Int, length: Int = -1): ByteArray {
        if (value < 0) {
            throw IllegalArgumentException("Cannot convert negative value to BCD")
        }

        // Count digits needed to represent the value
        var digits = if (value == 0) 1 else (Math.log10(value.toDouble()).toInt() + 1)

        // If length is specified, ensure we have enough bytes
        val bytesNeeded = (digits + 1) / 2
        val resultLength = if (length > 0) maxOf(length, bytesNeeded) else bytesNeeded

        val result = ByteArray(resultLength)
        var tempValue = value
        var position = resultLength - 1

        // Handle least significant digit differently if we have an odd number of digits
        if (digits % 2 == 1) {
            result[position] = (tempValue % 10).toByte()
            tempValue /= 10
            position--
        }

        // Process the remaining digits two at a time
        while (tempValue > 0 && position >= 0) {
            val low = tempValue % 10
            tempValue /= 10
            val high = tempValue % 10
            tempValue /= 10

            result[position] = ((high shl 4) or low).toByte()
            position--
        }

        // Pad with zeros if necessary
        while (position >= 0) {
            result[position] = 0
            position--
        }

        return result
    }

    /**
     * Converts a string of hexadecimal digits to a BCD (Binary Coded Decimal) byte array.
     * Each byte in the result will contain two decimal digits, with the higher
     * order digit in the upper 4 bits and the lower order digit in the lower 4 bits.
     *
     * @param hexString The hexadecimal string to convert
     * @param length Optional expected length of the result in bytes.
     *        If provided, the result will be padded or truncated to this length.
     * @return ByteArray containing the BCD representation
     */
    fun stringToBcd(hexString: String, length: Int = -1): ByteArray {
        // Remove any non-hex characters
        val cleanHex = hexString.replace(Regex("[^0-9A-Fa-f]"), "")

        // If the string has odd length, pad it with a leading zero
        val paddedHex = if (cleanHex.length % 2 == 0) cleanHex else "0$cleanHex"

        // Calculate the actual byte array size
        val actualLength = if (length > 0) length else paddedHex.length / 2

        // Create a byte array and fill it with zeros
        val result = ByteArray(actualLength)

        // Convert each pair of hexadecimal digits to a byte
        val bytesToFill = minOf(actualLength, paddedHex.length / 2)
        for (i in 0 until bytesToFill) {
            val startIndex = paddedHex.length - (i + 1) * 2
            val byteStr = paddedHex.substring(startIndex, startIndex + 2)
            result[actualLength - i - 1] = byteStr.toInt(16).toByte()
        }

        return result
    }

    /**
     * Converts a string to an ASCII byte array
     *
     * @param str The string to convert
     * @return Byte array containing ASCII representation
     */
    fun stringToAsc(str: String): ByteArray {
        return str.toByteArray(Charset.forName("ASCII"))
    }

    /**
     * More complete version with character set options
     *
     * @param str The string to convert
     * @param charset The character set to use (defaults to ASCII)
     * @return Byte array containing text in the specified encoding
     */
    fun stringToAsc(str: String, charset: Charset = Charset.forName("ASCII")): ByteArray {
        return str.toByteArray(charset)
    }

    /**
     * Converts a string to an ASCII byte array with padding
     *
     * @param str The string to convert
     * @param length The desired length of the output array
     * @param padChar The character to use for padding (default is space)
     * @param padLeft If true, pad on the left; if false, pad on the right
     * @return Byte array containing ASCII representation with padding
     */
    fun stringToAsc(str: String, length: Int, padChar: Char = ' ', padLeft: Boolean = false): ByteArray {
        val paddedStr = if (padLeft) {
            str.padStart(length, padChar)
        } else {
            str.padEnd(length, padChar)
        }

        // If the string is longer than the requested length, truncate it
        val finalStr = if (paddedStr.length > length) {
            paddedStr.substring(0, length)
        } else {
            paddedStr
        }

        return finalStr.toByteArray(Charset.forName("ASCII"))
    }



    /**
     * Converts an ASCII byte array to a string with specific offset and length
     *
     * @param bytes The ASCII bytes to convert
     * @param offset The starting position in the array
     * @param length The number of bytes to convert
     * @return String representation
     */
    fun ascToString(bytes: ByteArray, offset: Int, length: Int): String {
        return String(bytes, offset, length, Charset.forName("ASCII"))
    }

    /**
     * Converts an ASCII byte array to a string, handling trailing padding characters
     *
     * @param bytes The ASCII bytes to convert
     * @param padChar The character used for padding (default is space)
     * @return String with padding removed
     */
    fun ascToStringTrimmed(bytes: ByteArray, padChar: Char = ' '): String {
        val str = String(bytes, Charset.forName("ASCII"))

        return if (padChar == ' ') {
            str.trim()
        } else {
            str.trimEnd { it == padChar }
        }
    }

    /**
     * Utility function to check if a byte array contains valid ASCII characters
     *
     * @param bytes The byte array to check
     * @return True if all bytes are valid ASCII characters, false otherwise
     */
    fun isValidAscii(bytes: ByteArray): Boolean {
        return bytes.all { it in 0..127 }
    }

    /**
     * Converts a string containing decimal digits to a BCD (Binary Coded Decimal) byte array.
     * Each byte in the result will contain two decimal digits.
     *
     * @param input The string of decimal digits to convert (e.g., "1234567890")
     * @param length The expected length of the resulting byte array
     * @return ByteArray containing the BCD representation of the input string
     */
    fun decimalToBCD(input: String, length: Int): ByteArray {
        // Remove any non-digit characters
        val digitsOnly = input.filter { it.isDigit() }

        // Ensure input is even length
        val paddedInput = if (digitsOnly.length % 2 == 0) digitsOnly else "0$digitsOnly"

        // Pad or truncate to match expected length
        val paddedString = paddedInput.padStart(length * 2, '0')
            .take(length * 2)

        // Convert pairs of digits to bytes
        return ByteArray(paddedString.length / 2) { i ->
            val index = i * 2
            val firstDigit = paddedString[index].digitToInt()
            val secondDigit = paddedString[index + 1].digitToInt()
            ((firstDigit shl 4) or secondDigit).toByte()
        }
    }


    /**
     * Converts a BCD (Binary Coded Decimal) byte array to a string of decimal digits.
     *
     * @param data The BCD byte array to convert
     * @return String of decimal digits
     */
    fun bcdToDecimal(data: ByteArray): String {
        return data.joinToString("") { byte ->
            val high = (byte.toInt() shr 4) and 0x0F
            val low = byte.toInt() and 0x0F
            "$high$low"
        }
    }

    /**
     * Converts a string to an AN (Alphanumeric) byte array using ASCII encoding.
     * AN fields in ISO8583 only contain letters and numbers.
     *
     * @param input The alphanumeric string to convert
     * @return ByteArray containing the ASCII representation of the input string
     */
    fun stringToAN(input: String): ByteArray {
        // Filter to keep only letters and numbers
        val anOnly = input.filter { it.isLetterOrDigit() }
        return anOnly.toByteArray(Charsets.US_ASCII)
    }

    /**
     * Converts a string to an AN (Alphanumeric) byte array with specified length.
     * If input is shorter than length, it's right-padded with spaces.
     * If input is longer than length, it's truncated.
     *
     * @param input The alphanumeric string to convert
     * @param length The required length of the output byte array
     * @return ByteArray containing the ASCII representation, with padding if needed
     */
    fun stringToAN(input: String, length: Int): ByteArray {
        // Filter to keep only letters and numbers
        val anOnly = input.filter { it.isLetterOrDigit() }

        // Pad or truncate to required length
        val paddedString = anOnly.padEnd(length, ' ').take(length)
        return paddedString.toByteArray(Charsets.US_ASCII)
    }

    /**
     * Converts an AN (Alphanumeric) byte array to a string.
     *
     * @param data The AN byte array to convert
     * @return String representation of the AN byte array
     */
    fun anToString(data: ByteArray): String {
        return data.toString(Charsets.US_ASCII).trim()
    }

    /**
     * Converts a string to an ANS (Alphanumeric Special) byte array using ASCII encoding.
     * ANS fields in ISO8583 can contain letters, numbers, and special characters.
     *
     * @param input The string to convert
     * @return ByteArray containing the ASCII representation of the input string
     */
    fun stringToANS(input: String): ByteArray {
        return input.toByteArray(Charsets.US_ASCII)
    }

    /**
     * Converts a string to an ANS (Alphanumeric Special) byte array with specified length.
     * If input is shorter than length, it's right-padded with spaces.
     * If input is longer than length, it's truncated.
     *
     * @param input The string to convert
     * @param length The required length of the output byte array
     * @return ByteArray containing the ASCII representation, with padding if needed
     */
    fun stringToANS(input: String, length: Int): ByteArray {
        // Pad or truncate to required length
        val paddedString = input.padEnd(length, ' ').take(length)
        return paddedString.toByteArray(Charsets.US_ASCII)
    }

    /**
     * Converts an ANS (Alphanumeric Special) byte array to a string.
     *
     * @param data The ANS byte array to convert
     * @return String representation of the ANS byte array
     */
    fun ansToString(data: ByteArray): String {
        return data.toString(Charsets.US_ASCII).trim()
    }

    /**
     * Converts a hexadecimal string to a binary byte array.
     *
     * @param hexString The hex string to convert (e.g., "0A1B2C3D")
     * @return ByteArray representing the binary data
     */
    fun hexStringToBinary(hexString: String): ByteArray {
        // Remove any spaces or non-hex characters
        val cleanHex = hexString.replace(Regex("[^0-9A-Fa-f]"), "")

        // Ensure even length
        val paddedHex = if (cleanHex.length % 2 == 0) cleanHex else "0$cleanHex"

        return ByteArray(paddedHex.length / 2) { i ->
            val index = i * 2
            val byteStr = paddedHex.substring(index, index + 2)
            byteStr.toInt(16).toByte()
        }
    }

    /**
     * Converts a binary byte array to a hexadecimal string.
     *
     * @param data The binary byte array to convert
     * @param insertSpaces Whether to insert spaces between bytes in the output string
     * @return Hexadecimal string representation of the binary data
     */
    fun binaryToHexString(data: ByteArray, insertSpaces: Boolean = false): String {
        val separator = if (insertSpaces) " " else ""
        return data.joinToString(separator) { byte ->
            byte.toUByte().toString(16).padStart(2, '0')
        }
    }

    /**
     * Formats a byte array as a readable hex dump with both hex and ASCII representation.
     * Useful for debugging ISO8583 messages.
     *
     * @param data The byte array to format
     * @param bytesPerLine Number of bytes to display per line
     * @return Formatted hex dump string
     */
    fun formatHexDump(data: ByteArray, bytesPerLine: Int = 16): String {
        val sb = StringBuilder()

        for (i in data.indices step bytesPerLine) {
            // Add offset
            sb.append(String.format("%04X: ", i))

            // Add hex representation
            for (j in 0 until bytesPerLine) {
                if (i + j < data.size) {
                    sb.append(String.format("%02X ", data[i + j]))
                } else {
                    sb.append("   ")
                }
            }

            sb.append(" ")

            // Add ASCII representation
            for (j in 0 until bytesPerLine) {
                if (i + j < data.size) {
                    val c = data[i + j].toInt().toChar()
                    if (c.code in 32..126) { // Printable ASCII
                        sb.append(c)
                    } else {
                        sb.append('.')
                    }
                }
            }

            sb.append("\n")
        }

        return sb.toString()
    }

    fun stringToRequiredByteArray(type: BitType, length: BitLength, string: String): ByteArray {
        return when (type) {
            BitType.BINARY -> hexStringToBinary(
                bytesToHexString(
                    stringToBcd(
                        string,
                        string.length
                    )
                )
            )

            BitType.BCD -> stringToBcd(string, string.length)
            BitType.NOT_SPECIFIC -> string.toByteArray()

            BitType.AN -> stringToAN(string, string.length)
            BitType.ANS -> stringToANS(string, string.length)
        }
    }

    /**
     * Converts hexadecimal string to ASCII string
     * Handles ISO8583 message processing requirements with proper error handling
     *
     * @param hexString The hexadecimal string to convert (e.g., "48656C6C6F")
     * @param removeNonPrintable Whether to remove non-printable ASCII characters (default: false)
     * @return ASCII string representation
     * @throws IllegalArgumentException if hex string is invalid
     */
    fun hexToAscii(
        hexString: String,
        removeNonPrintable: Boolean = false
    ): String {
        // Validate input
        if (hexString.isBlank()) {
            return ""
        }

        // Remove any whitespace and convert to uppercase
        val cleanHex = hexString.replace("\\s".toRegex(), "").uppercase()

        // Validate hex string format
        if (!cleanHex.matches("^[0-9A-F]*$".toRegex())) {
            throw IllegalArgumentException("Invalid hexadecimal string: contains non-hex characters")
        }

        // Ensure even length (hex strings should have pairs of characters)
        if (cleanHex.length % 2 != 0) {
            throw IllegalArgumentException("Invalid hexadecimal string: odd length")
        }

        return try {
            val result = StringBuilder()

            // Process hex string in pairs
            for (i in cleanHex.indices step 2) {
                val hexPair = cleanHex.substring(i, i + 2)
                val asciiValue = hexPair.toInt(16)

                // Convert to ASCII character
                val char = asciiValue.toChar()

                if (removeNonPrintable) {
                    // Only include printable ASCII characters (32-126)
                    if (asciiValue in 32..126) {
                        result.append(char)
                    } else {
                        // Replace non-printable with dot or space
                        result.append('.')
                    }
                } else {
                    result.append(char)
                }
            }

            result.toString()

        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Invalid hexadecimal string: ${e.message}")
        }
    }

    /**
     * Overloaded method for ByteArray input
     * Converts byte array to ASCII string
     *
     * @param hexBytes The byte array containing hex values
     * @param removeNonPrintable Whether to remove non-printable ASCII characters
     * @return ASCII string representation
     */
    fun hexToAscii(
        hexBytes: ByteArray,
        removeNonPrintable: Boolean = false
    ): String {
        if (hexBytes.isEmpty()) {
            return ""
        }

        val result = StringBuilder()

        for (byte in hexBytes) {
            val asciiValue = byte.toInt() and 0xFF // Convert to unsigned
            val char = asciiValue.toChar()

            if (removeNonPrintable) {
                // Only include printable ASCII characters (32-126)
                if (asciiValue in 32..126) {
                    result.append(char)
                } else {
                    result.append('.')
                }
            } else {
                result.append(char)
            }
        }

        return result.toString()
    }

    /**
     * Extended method with encoding options for ISO8583 processing
     * Supports different character encodings commonly used in payment systems
     *
     * @param hexString The hexadecimal string to convert
     * @param encoding Character encoding to use (default: UTF-8)
     * @param removeNonPrintable Whether to remove non-printable characters
     * @return Encoded ASCII string
     */
    fun hexToAscii(
        hexString: String,
        encoding: String = "UTF-8",
        removeNonPrintable: Boolean = false
    ): String {
        if (hexString.isBlank()) {
            return ""
        }

        val cleanHex = hexString.replace("\\s".toRegex(), "").uppercase()

        if (!cleanHex.matches("^[0-9A-F]*$".toRegex())) {
            throw IllegalArgumentException("Invalid hexadecimal string: contains non-hex characters")
        }

        if (cleanHex.length % 2 != 0) {
            throw IllegalArgumentException("Invalid hexadecimal string: odd length")
        }

        return try {
            // Convert hex to byte array
            val bytes = ByteArray(cleanHex.length / 2)
            for (i in cleanHex.indices step 2) {
                val hexPair = cleanHex.substring(i, i + 2)
                bytes[i / 2] = hexPair.toInt(16).toByte()
            }

            // Convert using specified encoding
            val result = String(bytes, charset(encoding))

            if (removeNonPrintable) {
                // Filter non-printable characters
                result.filter { char ->
                    char.code in 32..126
                }.ifEmpty { "." }
            } else {
                result
            }

        } catch (e: Exception) {
            throw IllegalArgumentException("Conversion failed: ${e.message}")
        }
    }
}

