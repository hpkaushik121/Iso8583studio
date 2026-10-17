package ai.cortex.core

object CryptoUtils {

    /**
     * Converts a hexadecimal string to a "decimalized" string.
     *
     * This function processes a hexadecimal string in two passes:
     * 1. It collects all existing decimal digits (0-9) from the input string.
     * 2. It then iterates through the input string again. For any characters
     *    that are hexadecimal letters (A-F, case-insensitive), it converts them
     *    to their corresponding "decimalized" digit (A->0, B->1, ..., F->5).
     *    If a character is not a digit and not a valid hex letter, it defaults to '0'.
     *
     * The resulting string is a concatenation of the collected digits from the first pass
     * followed by the converted hex letters from the second pass.
     *
     * @param hexString The input string, expected to contain hexadecimal characters.
     *                  It can also contain non-hexadecimal characters, which will be
     *                  handled according to the conversion rules.
     * @return A string where hexadecimal letters are converted to digits 0-5,
     *         and existing digits are preserved. The order is digits first, then
     *         converted letters.
     *
     * @sample
     * ```
     * decimalize("1A2B") == "1201"
     * decimalize("FF00") == "0055"
     * decimalize("Hello") == "40" // H is not a hex letter, defaults to 0. e, l, o are not hex letters.
     * decimalize("123") == "123"
     * decimalize("abcDEF") == "012345"
     * decimalize("") == ""
     * ```
     */
    fun decimalize(hexString: String): String {
        val result = StringBuilder()

        // First pass: collect existing digits (0-9)
        for (char in hexString) {
            if (char.isDigit()) {
                result.append(char)
            }
        }

        // Second pass: convert A-F to 0-5
        val hexToDecimalMap = mapOf(
            'A' to '0', 'B' to '1', 'C' to '2',
            'D' to '3', 'E' to '4', 'F' to '5'
        )

        for (char in hexString) {
            if (!char.isDigit()) {
                result.append(hexToDecimalMap[char.uppercaseChar()] ?: '0')
            }
        }

        return result.toString()
    }


    fun applyParity(data: ByteArray, isOdd: Boolean): ByteArray {
        val hexString = IsoUtil.bytesToHexString(data).uppercase()

        // Bit count lookup table for hex nibbles
        val bitCounts = mapOf(
            '0' to 0, '1' to 1, '2' to 1, '3' to 2, '4' to 1, '5' to 2, '6' to 2, '7' to 3,
            '8' to 1, '9' to 2, 'A' to 2, 'B' to 3, 'C' to 2, 'D' to 3, 'E' to 3, 'F' to 4
        )

        // Nibble flip lookup table
        val flipMap = mapOf(
            '0' to '1', '1' to '0', '2' to '3', '3' to '2', '4' to '5', '5' to '4',
            '6' to '7', '7' to '6', '8' to '9', '9' to '8', 'A' to 'B', 'B' to 'A',
            'C' to 'D', 'D' to 'C', 'E' to 'F', 'F' to 'E'
        )

        // JavaScript uses 0 for odd parity target, 1 for even
        val targetParity = if (isOdd) 0 else 1

        val result = StringBuilder()
        for (i in hexString.indices step 2) {
            val highNibble = hexString[i]
            val lowNibble = hexString[i + 1]
            val totalBits = bitCounts[highNibble]!! + bitCounts[lowNibble]!!

            if (totalBits % 2 == targetParity) {
                // Need to flip the low nibble to adjust parity
                result.append(highNibble)
                result.append(flipMap[lowNibble]!!)
            } else {
                // Parity is already correct
                result.append(highNibble)
                result.append(lowNibble)
            }
        }

        return IsoUtil.hexStringToBytes(result.toString())
    }

    /**
     * Pads data to specified block size
     */
    fun padToBlockSize(data: ByteArray, blockSize: Int): ByteArray {
        val remainder = data.size % blockSize
        if (remainder == 0) return data

        val paddingLength = blockSize - remainder
        val paddedData = ByteArray(data.size + paddingLength)
        data.copyInto(paddedData)

        return paddedData
    }
}