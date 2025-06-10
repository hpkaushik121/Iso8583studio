package ai.cortex.payment.crypto

import ai.cortex.core.crypto.MacCalculator
import ai.cortex.core.crypto.adjustParity
import ai.cortex.core.crypto.data.ArpcMethod
import ai.cortex.core.crypto.data.CryptogramType
import ai.cortex.core.crypto.data.KcvType
import ai.cortex.core.crypto.data.KeyParity
import ai.cortex.core.crypto.data.PaddingMethod
import ai.cortex.core.crypto.data.SessionKeyDerivationMethod
import ai.cortex.core.crypto.data.UdkDerivationOption
import ai.cortex.core.crypto.getSymmetricCipher
import ai.cortex.core.crypto.hexToByteArray
import ai.cortex.core.crypto.toHexString
import java.security.MessageDigest
import kotlin.experimental.inv

class EmvProcessor {
    private val cipher = getSymmetricCipher()
    private val zeroIv = ByteArray(8)

    /**
     * Derives a 128-bit (16-byte) ICC Master Key (also known as UDK) from a 128-bit Issuer Master Key (MDK).
     * This implementation follows the JavaScript reference exactly for compatibility.
     *
     * @param mdk The 16-byte Issuer Master Key (MDK).
     * @param pan The Application Primary Account Number (PAN).
     * @param panSequenceNumber The 2-digit PAN Sequence Number. Defaults to "00".
     * @param derivationOption The derivation algorithm to use, either OPTION_A or OPTION_B.
     * @param keyParity The parity to apply to the final key. Defaults to NONE.
     * @return The derived 16-byte ICC Master Key (UDK).
     */
    fun deriveUdk(
        mdk: ByteArray,
        pan: String,
        panSequenceNumber: String = "00",
        derivationOption: UdkDerivationOption = UdkDerivationOption.OPTION_A,
        keyParity: KeyParity = KeyParity.NONE
    ): ByteArray {
        require(mdk.size == 16) { "MDK must be 16 bytes (128 bits)." }
        require(pan.isNotEmpty()) { "PAN cannot be empty." }

        println("=== UDK Derivation Debug ===")
        println("MDK: ${mdk.joinToString("") { "%02X".format(it) }}")
        println("PAN: $pan")
        println("PAN Seq: $panSequenceNumber")
        println("Option: $derivationOption")
        println("Parity: $keyParity")

        val panDigits = pan.filter { it.isDigit() }
        val panSeq = panSequenceNumber.padStart(2, '0')

        val result = when (derivationOption) {
            UdkDerivationOption.OPTION_A -> {
                deriveUdkOptionA(mdk, panDigits, panSeq)
            }
            UdkDerivationOption.OPTION_B -> {
                deriveUdkOptionB(mdk, panDigits, panSeq)
            }
        }

        println("Before parity: ${result.joinToString("") { "%02X".format(it) }}")

        val finalResult = when (keyParity) {
            KeyParity.RIGHT_ODD -> applyJavaScriptStyleParity(result, isOdd = true)
            KeyParity.RIGHT_EVEN -> applyJavaScriptStyleParity(result, isOdd = false)
            else -> result
        }

        println("After parity: ${finalResult.joinToString("") { "%02X".format(it) }}")
        println("KCV: ${calculateKcvHex(finalResult.joinToString("") { "%02X".format(it) })}")

        return finalResult
    }

    /**
     * Option A UDK derivation - EMV standard approach
     */
    private fun deriveUdkOptionA(mdk: ByteArray, panDigits: String, panSeq: String): ByteArray {
        val combinedPan = (panDigits + panSeq).padStart(16, '0').takeLast(16)
        val y = stringToBcd(combinedPan)

        return performTripleDes(mdk, y)
    }

    /**
     * Option B UDK derivation - For PANs > 16 digits
     * Follows JavaScript implementation exactly
     *
     * JavaScript code analysis:
     * - n = panDigits + panSeq
     * - if (n.length % 2 == 1) n = "0" + n
     * - bytes = hex2a(n) // converts hex string pairs to bytes
     * - hash = sha1(bytes)
     * - decimalized = decimalize(hash.toHex())
     * - y = decimalized.substr(0, 16)
     */
    private fun deriveUdkOptionB(mdk: ByteArray, panDigits: String, panSeq: String): ByteArray {
        // Step 1: Combine PAN + PAN sequence
        var n = panDigits + panSeq
        println("Debug Option B - Combined: $n (length: ${n.length})")

        // Step 2: Ensure even length by prepending "0" if needed
        if (n.length % 2 == 1) {
            n = "0$n"
        }
        println("Debug Option B - After padding: $n (length: ${n.length})")

        // Step 3: JavaScript hex2a - convert hex string to bytes
        // Each pair of characters represents a hex byte
        val bytes = ByteArray(n.length / 2)
        for (i in bytes.indices) {
            val hexPair = n.substring(i * 2, i * 2 + 2)
            bytes[i] = hexPair.toInt(16).toByte()
        }
        println("Debug Option B - Hex bytes: ${bytes.joinToString("") { "%02X".format(it) }}")

        // Step 4: SHA-1 hash
        val sha1Hash = sha1(bytes)
        val sha1Hex = sha1Hash.joinToString("") { "%02X".format(it) }
        println("Debug Option B - SHA-1: $sha1Hex")

        // Step 5: Decimalize the hash
        val decimalizedString = decimalizeJavaScriptStyle(sha1Hex)
        println("Debug Option B - Decimalized: $decimalizedString")

        // Step 6: Take first 16 digits and convert to BCD
        val y16 = decimalizedString.take(16)
        println("Debug Option B - Y (16 digits): $y16")

        val y = stringToBcd(y16)
        println("Debug Option B - Y BCD: ${y.joinToString("") { "%02X".format(it) }}")

        return performTripleDes(mdk, y)
    }

    /**
     * Performs the Triple DES operation as per EMV specification
     */
    private fun performTripleDes(mdk: ByteArray, y: ByteArray): ByteArray {
        // Create 24-byte Triple DES key from 16-byte MDK
        val desKey = mdk + mdk.copyOfRange(0, 8)
        val xorMask = "FFFFFFFFFFFFFFFF".hexToByteArray()

        println("Debug Triple DES - MDK: ${mdk.joinToString("") { "%02X".format(it) }}")
        println("Debug Triple DES - Y: ${y.joinToString("") { "%02X".format(it) }}")
        println("Debug Triple DES - DES Key: ${desKey.joinToString("") { "%02X".format(it) }}")

        val yXor = xorByteArray(y, xorMask)
        println("Debug Triple DES - Y XOR FFFF...: ${yXor.joinToString("") { "%02X".format(it) }}")

        val zl = cipher.encryptEcb(y, desKey)
        val zr = cipher.encryptEcb(yXor, desKey)

        println("Debug Triple DES - ZL: ${zl.joinToString("") { "%02X".format(it) }}")
        println("Debug Triple DES - ZR: ${zr.joinToString("") { "%02X".format(it) }}")

        val result = zl + zr
        println("Debug Triple DES - ZL+ZR: ${result.joinToString("") { "%02X".format(it) }}")

        return result
    }

    /**
     * JavaScript-style decimalization
     * First pass: collect all digits 0-9
     * Second pass: convert A-F to 0-5 respectively
     */
    private fun decimalizeJavaScriptStyle(hexString: String): String {
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

    /**
     * JavaScript-style parity adjustment
     * Implements the exact bit counting and nibble flipping logic
     */
    private fun applyJavaScriptStyleParity(data: ByteArray, isOdd: Boolean): ByteArray {
        val hexString = data.toHexString().uppercase()

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

        return result.toString().hexToByteArray()
    }

    /**
     * Calculate Key Check Value following DES standards
     * JavaScript uses des_key_kcv which encrypts 8 zero bytes and takes first 6 hex chars (3 bytes)
     */
    fun calculateKcv(key: ByteArray, kcvType: KcvType = KcvType.STANDARD): ByteArray {
        require(key.size in listOf(8, 16, 24)) { "Key must be 8, 16, or 24 bytes" }

        val zeroBlock = ByteArray(8) // 8 bytes of zeros

        val encrypted = when (key.size) {
            8 -> {
                // Single DES - create Triple DES key by repeating
                val tripleDesKey = key + key + key
                cipher.encryptEcb(zeroBlock, tripleDesKey)
            }
            16 -> {
                // Double DES - create Triple DES key K1+K2+K1
                val tripleDesKey = key + key.copyOfRange(0, 8)
                cipher.encryptEcb(zeroBlock, tripleDesKey)
            }
            24 -> {
                // Triple DES - use key as-is
                cipher.encryptEcb(zeroBlock, key)
            }
            else -> throw IllegalArgumentException("Unsupported key size: ${key.size}")
        }

        return when (kcvType) {
            KcvType.STANDARD -> encrypted.copyOfRange(0, 3) // First 3 bytes (6 hex chars)
            KcvType.VISA -> encrypted.copyOfRange(0, 4)     // First 4 bytes (8 hex chars)
        }
    }

    /**
     * Calculate KCV as hex string (for compatibility with JavaScript)
     */
    fun calculateKcvHex(key: String, kcvType: KcvType = KcvType.STANDARD): String {
        val keyBytes = key.hexToByteArray()
        val kcvBytes = calculateKcv(keyBytes, kcvType)
        return kcvBytes.toHexString().uppercase()
    }

    /**
     * EMV Common Session Key derivation.
     */
    fun deriveCommonSessionKey(
        masterKey: ByteArray,
        atc: ByteArray,
        keyParity: KeyParity = KeyParity.NONE
    ): ByteArray {
        require(masterKey.size == 16) { "Master key must be 16 bytes" }
        require(atc.size == 2) { "ATC must be 2 bytes" }

        val diversificationData = atc + atc.map { it.inv() }.toByteArray() + ByteArray(12)
        val desKey = masterKey + masterKey.copyOfRange(0, 8)
        val sessionKey = cipher.encryptEcb(diversificationData.copyOfRange(0, 8), desKey)

        return sessionKey.adjustParity(keyParity)
    }

    // Helper functions

    /**
     * Computes the SHA-1 hash of the input data.
     */
    private fun sha1(data: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-1")
        return digest.digest(data)
    }

    /**
     * Converts a string of digits into an 8-byte BCD (Binary Coded Decimal) array.
     */
    private fun stringToBcd(digits: String): ByteArray {
        require(digits.length == 16) { "Input for BCD conversion must be 16 digits, got: ${digits.length}" }
        require(digits.all { it.isDigit() }) { "Input must contain only digits: $digits" }

        val bcd = ByteArray(8)
        for (i in bcd.indices) {
            val high = Character.digit(digits[i * 2], 10)
            val low = Character.digit(digits[i * 2 + 1], 10)
            bcd[i] = ((high shl 4) or low).toByte()
        }
        return bcd
    }

    /**
     * Performs a bitwise XOR operation on two byte arrays.
     */
    private fun xorByteArray(a: ByteArray, b: ByteArray): ByteArray {
        require(a.size == b.size) { "Arrays must be same size: ${a.size} vs ${b.size}" }

        val result = ByteArray(a.size)
        for (i in result.indices) {
            result[i] = (a[i].toInt() xor b[i].toInt()).toByte()
        }
        return result
    }

    /**
     * Legacy decimalize method - kept for backwards compatibility
     */
    @Deprecated("Use decimalizeJavaScriptStyle for JavaScript compatibility")
    private fun decimalize(sha1Hash: ByteArray): String {
        val decimalized = StringBuilder(16)
        val hexString = sha1Hash.toHexString()

        // First pass: collect all existing decimal digits (0-9).
        for (char in hexString) {
            if (char.isDigit()) {
                decimalized.append(char)
                if (decimalized.length == 16) return decimalized.toString()
            }
        }

        // Second pass: convert non-decimal nibbles (A-F) to decimals per the spec table.
        for (char in hexString) {
            if (!char.isDigit()) {
                val convertedDigit = char.uppercaseChar() - 'A'
                decimalized.append(convertedDigit)
                if (decimalized.length == 16) return decimalized.toString()
            }
        }
        return decimalized.toString()
    }
}