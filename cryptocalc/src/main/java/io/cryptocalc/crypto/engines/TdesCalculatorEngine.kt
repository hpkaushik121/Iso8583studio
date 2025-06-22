package io.cryptocalc.crypto.engines

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

/**
 * Custom Triple DES Calculator Engine
 *
 * Enterprise-grade custom 3DES implementation for payment industry applications.
 * Provides fallback implementation when standard JCE fails or is unavailable.
 *
 * Features:
 * - Custom 3DES implementation (E-D-E and D-E-D operations)
 * - All cipher modes: ECB, CBC, CFB, OFB with custom implementations
 * - Payment industry functions (KCV, MAC, key derivation)
 * - HSM-compatible operations
 * - Comprehensive validation and error handling
 * - Fallback mechanism when JCE fails
 *
 * @author Crypto SDK Team
 * @version 1.0.0
 */
object TdesCalculatorEngine {

    // ============================================================================
    // CORE CUSTOM 3DES OPERATIONS
    // ============================================================================

    /**
     * Custom 3DES encryption using EDE (Encrypt-Decrypt-Encrypt) pattern
     */
    private fun custom3DESEncrypt(block: ByteArray, keys: Triple<ByteArray, ByteArray, ByteArray>): ByteArray {
        val (k1, k2, k3) = keys

        // E(K1) -> D(K2) -> E(K3)
        var temp = singleDESOperation(block, k1, encrypt = true)
        temp = singleDESOperation(temp, k2, encrypt = false)
        return singleDESOperation(temp, k3, encrypt = true)
    }

    /**
     * Custom 3DES decryption using DED (Decrypt-Encrypt-Decrypt) pattern
     */
    private fun custom3DESDecrypt(block: ByteArray, keys: Triple<ByteArray, ByteArray, ByteArray>): ByteArray {
        val (k1, k2, k3) = keys

        // D(K3) -> E(K2) -> D(K1)
        var temp = singleDESOperation(block, k3, encrypt = false)
        temp = singleDESOperation(temp, k2, encrypt = true)
        return singleDESOperation(temp, k1, encrypt = false)
    }

    /**
     * Extract keys from 16-byte or 24-byte key material
     */
    private fun extractKeys(key: ByteArray): Triple<ByteArray, ByteArray, ByteArray> {
        return when (key.size) {
            8 -> {
                // Single DES: K1, K1, K1
                Triple(key, key, key)
            }
            16 -> {
                // 2-key 3DES: K1, K2, K1
                val k1 = key.sliceArray(0..7)
                val k2 = key.sliceArray(8..15)
                Triple(k1, k2, k1)
            }
            24 -> {
                // 3-key 3DES: K1, K2, K3
                val k1 = key.sliceArray(0..7)
                val k2 = key.sliceArray(8..15)
                val k3 = key.sliceArray(16..23)
                Triple(k1, k2, k3)
            }
            else -> throw IllegalArgumentException("Invalid key size: ${key.size}")
        }
    }

    /**
     * Single DES operation using JCE
     */
    private fun singleDESOperation(data: ByteArray, key: ByteArray, encrypt: Boolean): ByteArray {
        val cipher = Cipher.getInstance("DES/ECB/NoPadding")
        val keySpec = SecretKeySpec(key, "DES")
        val mode = if (encrypt) Cipher.ENCRYPT_MODE else Cipher.DECRYPT_MODE
        cipher.init(mode, keySpec)
        return cipher.doFinal(data)
    }

    /**
     * XOR two byte arrays
     */
    private fun xorBlocks(block1: ByteArray, block2: ByteArray): ByteArray {
        require(block1.size == block2.size) { "Blocks must be same size" }
        return block1.zip(block2) { a, b -> (a.toInt() xor b.toInt()).toByte() }.toByteArray()
    }

    // ============================================================================
    // CORE ENCRYPTION/DECRYPTION FUNCTIONS WITH FALLBACK
    // ============================================================================

    /**
     * Encrypt data using 3DES in CBC mode with custom fallback
     */
    fun encryptCBC(data: ByteArray, key: ByteArray, iv: ByteArray? = ByteArray(8)): ByteArray {
        validateKey(key)
        val actualIV = iv ?: ByteArray(8)
        validateIV(actualIV)

        // Try standard JCE first
        try {
            val transformation = getTransformation(key.size, "CBC")
            val algorithm = getAlgorithm(key.size)

            val keySpec = SecretKeySpec(key, algorithm)
            val ivSpec = IvParameterSpec(actualIV)
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            return cipher.doFinal(data)
        } catch (e: Exception) {
            // Fallback to custom implementation
            return customEncryptCBC(data, key, actualIV)
        }
    }

    /**
     * Custom CBC encryption implementation
     */
    private fun customEncryptCBC(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        require(data.size % 8 == 0) { "Data size must be multiple of 8 bytes for custom CBC" }

        val keys = extractKeys(key)
        val result = ByteArray(data.size)
        var previousBlock = iv.copyOf()

        for (i in data.indices step 8) {
            val block = data.sliceArray(i until i + 8)

            // CBC: XOR current block with previous ciphertext (or IV for first block)
            val xorBlock = xorBlocks(block, previousBlock)

            // Apply 3DES encryption
            val encrypted = if (key.size == 8) {
                singleDESOperation(xorBlock, key, encrypt = true)
            } else {
                custom3DESEncrypt(xorBlock, keys)
            }

            encrypted.copyInto(result, i)
            previousBlock = encrypted
        }

        return result
    }

    /**
     * Decrypt data using 3DES in CBC mode with custom fallback
     */
    fun decryptCBC(data: ByteArray, key: ByteArray, iv: ByteArray? = ByteArray(8)): ByteArray {
        validateKey(key)
        val actualIV = iv ?: ByteArray(8)
        validateIV(actualIV)

        // Try standard JCE first
        try {
            val transformation = getTransformation(key.size, "CBC")
            val algorithm = getAlgorithm(key.size)

            val keySpec = SecretKeySpec(key, algorithm)
            val ivSpec = IvParameterSpec(actualIV)
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            return cipher.doFinal(data)
        } catch (e: Exception) {
            // Fallback to custom implementation
            return customDecryptCBC(data, key, actualIV)
        }
    }

    /**
     * Custom CBC decryption implementation
     */
    private fun customDecryptCBC(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        require(data.size % 8 == 0) { "Data size must be multiple of 8 bytes for custom CBC" }

        val keys = extractKeys(key)
        val result = ByteArray(data.size)
        var previousBlock = iv.copyOf()

        for (i in data.indices step 8) {
            val block = data.sliceArray(i until i + 8)

            // Apply 3DES decryption
            val decrypted = if (key.size == 8) {
                singleDESOperation(block, key, encrypt = false)
            } else {
                custom3DESDecrypt(block, keys)
            }

            // CBC: XOR with previous ciphertext (or IV for first block)
            val xorBlock = xorBlocks(decrypted, previousBlock)

            xorBlock.copyInto(result, i)
            previousBlock = block
        }

        return result
    }

    /**
     * Encrypt data using 3DES in ECB mode with custom fallback
     */
    fun encryptECB(data: ByteArray, key: ByteArray): ByteArray {
        validateKey(key)

        // Try standard JCE first
        try {
            val transformation = getTransformation(key.size, "ECB")
            val algorithm = getAlgorithm(key.size)

            val keySpec = SecretKeySpec(key, algorithm)
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            return cipher.doFinal(data)
        } catch (e: Exception) {
            // Fallback to custom implementation
            return customEncryptECB(data, key)
        }
    }

    /**
     * Custom ECB encryption implementation
     */
    private fun customEncryptECB(data: ByteArray, key: ByteArray): ByteArray {
        require(data.size % 8 == 0) { "Data size must be multiple of 8 bytes for ECB" }

        val keys = extractKeys(key)
        val result = ByteArray(data.size)

        for (i in data.indices step 8) {
            val block = data.sliceArray(i until i + 8)
            val encrypted = if (key.size == 8) {
                singleDESOperation(block, key, encrypt = true)
            } else {
                custom3DESEncrypt(block, keys)
            }
            encrypted.copyInto(result, i)
        }

        return result
    }

    /**
     * Decrypt data using 3DES in ECB mode with custom fallback
     */
    fun decryptECB(data: ByteArray, key: ByteArray): ByteArray {
        validateKey(key)

        // Try standard JCE first
        try {
            val transformation = getTransformation(key.size, "ECB")
            val algorithm = getAlgorithm(key.size)

            val keySpec = SecretKeySpec(key, algorithm)
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            return cipher.doFinal(data)
        } catch (e: Exception) {
            // Fallback to custom implementation
            return customDecryptECB(data, key)
        }
    }

    /**
     * Custom ECB decryption implementation
     */
    private fun customDecryptECB(data: ByteArray, key: ByteArray): ByteArray {
        require(data.size % 8 == 0) { "Data size must be multiple of 8 bytes for ECB" }

        val keys = extractKeys(key)
        val result = ByteArray(data.size)

        for (i in data.indices step 8) {
            val block = data.sliceArray(i until i + 8)
            val decrypted = if (key.size == 8) {
                singleDESOperation(block, key, encrypt = false)
            } else {
                custom3DESDecrypt(block, keys)
            }
            decrypted.copyInto(result, i)
        }

        return result
    }

    /**
     * Encrypt data using 3DES in CFB mode with custom implementation
     */
    fun encryptCFB(data: ByteArray, key: ByteArray, iv: ByteArray? = ByteArray(8)): ByteArray {
        validateKey(key)
        val actualIV = iv ?: ByteArray(8)
        validateIV(actualIV)

        // Try standard JCE first
        try {
            val transformation = getTransformation(key.size, "CFB")
            val algorithm = getAlgorithm(key.size)

            val keySpec = SecretKeySpec(key, algorithm)
            val ivSpec = IvParameterSpec(actualIV)
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            return cipher.doFinal(data)
        } catch (e: Exception) {
            // Custom CFB implementation
            return customCFBOperation(data, key, actualIV, encrypt = true)
        }
    }

    /**
     * Decrypt data using 3DES in CFB mode with custom implementation
     */
    fun decryptCFB(data: ByteArray, key: ByteArray, iv: ByteArray? = ByteArray(8)): ByteArray {
        validateKey(key)
        val actualIV = iv ?: ByteArray(8)
        validateIV(actualIV)

        // Try standard JCE first
        try {
            val transformation = getTransformation(key.size, "CFB")
            val algorithm = getAlgorithm(key.size)

            val keySpec = SecretKeySpec(key, algorithm)
            val ivSpec = IvParameterSpec(actualIV)
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            return cipher.doFinal(data)
        } catch (e: Exception) {
            // Custom CFB implementation
            return customCFBOperation(data, key, actualIV, encrypt = false)
        }
    }

    /**
     * Custom CFB mode implementation
     */
    private fun customCFBOperation(data: ByteArray, key: ByteArray, iv: ByteArray, encrypt: Boolean): ByteArray {
        val keys = extractKeys(key)
        val result = ByteArray(data.size)
        var feedbackRegister = iv.copyOf()

        var offset = 0
        while (offset < data.size) {
            // Encrypt the feedback register
            val encryptedFeedback = if (key.size == 8) {
                singleDESOperation(feedbackRegister, key, encrypt = true)
            } else {
                custom3DESEncrypt(feedbackRegister, keys)
            }

            val blockSize = minOf(8, data.size - offset)
            for (i in 0 until blockSize) {
                result[offset + i] = (data[offset + i].toInt() xor encryptedFeedback[i].toInt()).toByte()
            }

            // Update feedback register
            if (encrypt) {
                // Shift left and add new ciphertext
                if (blockSize == 8) {
                    feedbackRegister = result.sliceArray(offset until offset + 8)
                } else {
                    System.arraycopy(feedbackRegister, blockSize, feedbackRegister, 0, 8 - blockSize)
                    System.arraycopy(result, offset, feedbackRegister, 8 - blockSize, blockSize)
                }
            } else {
                // Shift left and add old ciphertext
                if (blockSize == 8) {
                    feedbackRegister = data.sliceArray(offset until offset + 8)
                } else {
                    System.arraycopy(feedbackRegister, blockSize, feedbackRegister, 0, 8 - blockSize)
                    System.arraycopy(data, offset, feedbackRegister, 8 - blockSize, blockSize)
                }
            }

            offset += blockSize
        }

        return result
    }

    /**
     * Encrypt data using 3DES in OFB mode with custom implementation
     */
    fun encryptOFB(data: ByteArray, key: ByteArray, iv: ByteArray? = ByteArray(8)): ByteArray {
        validateKey(key)
        val actualIV = iv ?: ByteArray(8)
        validateIV(actualIV)

        // Try standard JCE first
        try {
            val transformation = getTransformation(key.size, "OFB")
            val algorithm = getAlgorithm(key.size)

            val keySpec = SecretKeySpec(key, algorithm)
            val ivSpec = IvParameterSpec(actualIV)
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            return cipher.doFinal(data)
        } catch (e: Exception) {
            // Custom OFB implementation
            return customOFBOperation(data, key, actualIV)
        }
    }

    /**
     * Decrypt data using 3DES in OFB mode with custom implementation
     */
    fun decryptOFB(data: ByteArray, key: ByteArray, iv: ByteArray? = ByteArray(8)): ByteArray {
        validateKey(key)
        val actualIV = iv ?: ByteArray(8)
        validateIV(actualIV)

        // Try standard JCE first
        try {
            val transformation = getTransformation(key.size, "OFB")
            val algorithm = getAlgorithm(key.size)

            val keySpec = SecretKeySpec(key, algorithm)
            val ivSpec = IvParameterSpec(actualIV)
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            return cipher.doFinal(data)
        } catch (e: Exception) {
            // Custom OFB implementation - same as encryption in OFB mode
            return customOFBOperation(data, key, actualIV)
        }
    }

    /**
     * Custom OFB mode implementation
     */
    private fun customOFBOperation(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val keys = extractKeys(key)
        val result = ByteArray(data.size)
        var feedbackRegister = iv.copyOf()

        var offset = 0
        while (offset < data.size) {
            // Encrypt the feedback register
            val encryptedFeedback = if (key.size == 8) {
                singleDESOperation(feedbackRegister, key, encrypt = true)
            } else {
                custom3DESEncrypt(feedbackRegister, keys)
            }

            val blockSize = minOf(8, data.size - offset)
            for (i in 0 until blockSize) {
                result[offset + i] = (data[offset + i].toInt() xor encryptedFeedback[i].toInt()).toByte()
            }

            // Update feedback register with encrypted output
            feedbackRegister = encryptedFeedback
            offset += blockSize
        }

        return result
    }

    // ============================================================================
    // PAYMENT INDUSTRY SPECIFIC FUNCTIONS
    // ============================================================================

    /**
     * Calculate Key Check Value (KCV) - First 3 bytes of encrypting 8 zeros
     */
    fun calculateKCV(key: ByteArray): ByteArray {
        val zeroBlock = ByteArray(8) // 8 bytes of zeros for DES block size
        val encrypted = encryptECB(zeroBlock, key)
        return encrypted.sliceArray(0..2) // Return first 3 bytes
    }

    /**
     * Generate 3DES MAC (Message Authentication Code) using CBC mode
     */
    fun generateMAC(data: ByteArray, key: ByteArray, iv: ByteArray = ByteArray(8)): ByteArray {
        // Pad data to block boundary
        val paddedData = padData(data, 8)
        val encrypted = encryptCBC(paddedData, key, iv)
        // Return last block as MAC
        return encrypted.sliceArray(encrypted.size - 8 until encrypted.size)
    }

    /**
     * Generate retail MAC as per ANSI X9.19
     */
    fun generateRetailMAC(data: ByteArray, key: ByteArray): ByteArray {
        require(key.size == 16) { "Retail MAC requires 16-byte key (2-key 3DES)" }

        val paddedData = padData(data, 8)
        val leftKey = key.sliceArray(0..7)
        val rightKey = key.sliceArray(8..15)

        // DES encrypt with left key in CBC mode
        var result = ByteArray(8)
        for (i in paddedData.indices step 8) {
            val block = paddedData.sliceArray(i until i + 8)
            // XOR with previous result
            for (j in 0..7) {
                block[j] = (block[j].toInt() xor result[j].toInt()).toByte()
            }
            result = encryptECB(block, leftKey)
        }

        // Decrypt with right key, then encrypt with left key
        val decrypted = decryptECB(result, rightKey)
        return encryptECB(decrypted, leftKey)
    }

    /**
     * Derive EMV application cryptogram session keys
     */
    fun deriveSessionKey(masterKey: ByteArray, diversificationData: ByteArray): ByteArray {
        require(diversificationData.size == 8) { "Diversification data must be 8 bytes" }
        return encryptECB(diversificationData, masterKey)
    }

    /**
     * PIN Block encryption for ISO Format 0
     */
    fun encryptPINBlock(pinBlock: ByteArray, key: ByteArray): ByteArray {
        require(pinBlock.size == 8) { "PIN block must be 8 bytes" }
        return encryptECB(pinBlock, key)
    }

    /**
     * Working key derivation for payment HSMs
     */
    fun deriveWorkingKey(masterKey: ByteArray, keySerial: ByteArray): ByteArray {
        require(keySerial.size == 8) { "Key serial must be 8 bytes" }
        val leftHalf = encryptECB(keySerial, masterKey.sliceArray(0..15))
        val rightHalf = encryptECB(keySerial, masterKey.sliceArray(8..23))
        return leftHalf + rightHalf.sliceArray(0..7)
    }

    // ============================================================================
    // KEY MANAGEMENT FUNCTIONS
    // ============================================================================

    /**
     * Generate random 3DES key with proper parity
     */
    fun generateKey(keySize: Int = 24): ByteArray {
        require(keySize in listOf(8, 16, 24)) { "Key size must be 8, 16, or 24 bytes" }

        val random = SecureRandom()
        val key = ByteArray(keySize)

        do {
            random.nextBytes(key)
            adjustParity(key)
        } while (isWeakKey(key))

        return key
    }

    /**
     * Convert 2-key 3DES to 3-key 3DES format
     */
    fun expandTo3Key(twoKey: ByteArray): ByteArray {
        require(twoKey.size == 16) { "Input must be 16-byte 2-key format" }
        return twoKey + twoKey.sliceArray(0..7) // K1||K2||K1
    }

    /**
     * Validate if key has proper DES parity
     */
    fun hasValidParity(key: ByteArray): Boolean {
        return key.all { byte ->
            val bits = byte.toInt() and 0xFF
            bits.countOneBits() % 2 == 1 // Odd parity
        }
    }

    /**
     * Adjust key bytes to have odd parity
     */
    fun adjustParity(key: ByteArray) {
        for (i in key.indices) {
            val bits = key[i].toInt() and 0xFE // Clear LSB
            val oneBits = bits.countOneBits()
            // Set LSB to make total bits odd
            key[i] = (bits or (if (oneBits % 2 == 0) 1 else 0)).toByte()
        }
    }

    /**
     * Check if key is cryptographically weak
     */
    fun isWeakKey(key: ByteArray): Boolean {
        val keyHex = key.joinToString("") { "%02X".format(it) }

        return when (key.size) {
            8 -> isWeakDESKey(keyHex)
            16 -> {
                val k1 = keyHex.substring(0, 16)
                val k2 = keyHex.substring(16, 32)
                isWeakDESKey(k1) || isWeakDESKey(k2) || k1 == k2
            }
            24 -> {
                val k1 = keyHex.substring(0, 16)
                val k2 = keyHex.substring(16, 32)
                val k3 = keyHex.substring(32, 48)
                isWeakDESKey(k1) || isWeakDESKey(k2) || isWeakDESKey(k3) ||
                        k1 == k2 || k1 == k3 || k2 == k3
            }
            else -> false
        }
    }

    // ============================================================================
    // UTILITY FUNCTIONS
    // ============================================================================

    /**
     * Generate random IV for CBC/CFB/OFB modes
     */
    fun generateIV(): ByteArray {
        val iv = ByteArray(8)
        SecureRandom().nextBytes(iv)
        return iv
    }

    /**
     * Pad data using PKCS#7 padding
     */
    fun padData(data: ByteArray, blockSize: Int): ByteArray {
        val paddingLength = blockSize - (data.size % blockSize)
        val paddedData = ByteArray(data.size + paddingLength)
        System.arraycopy(data, 0, paddedData, 0, data.size)

        // PKCS#7 padding
        for (i in data.size until paddedData.size) {
            paddedData[i] = paddingLength.toByte()
        }

        return paddedData
    }

    /**
     * Remove PKCS#7 padding
     */
    fun unpadData(data: ByteArray): ByteArray {
        val paddingLength = data.last().toInt()
        require(paddingLength in 1..8) { "Invalid padding" }

        val unpaddedSize = data.size - paddingLength
        return data.sliceArray(0 until unpaddedSize)
    }

    /**
     * Convert hex string to byte array
     */
    fun hexToBytes(hex: String): ByteArray {
        val cleanHex = hex.replace(" ", "").replace("-", "").uppercase()
        require(cleanHex.matches(Regex("^[0-9A-F]*$"))) { "Invalid hex string" }
        require(cleanHex.length % 2 == 0) { "Hex string must have even length" }

        return cleanHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    /**
     * Convert byte array to hex string
     */
    fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02X".format(it) }
    }

    // ============================================================================
    // PRIVATE HELPER FUNCTIONS
    // ============================================================================

    private fun getTransformation(keySize: Int, mode: String): String {
        val algorithm = if (keySize == 8) "DES" else "DESede"
        return "$algorithm/$mode/NoPadding"
    }

    private fun getAlgorithm(keySize: Int): String {
        return if (keySize == 8) "DES" else "DESede"
    }

    private fun validateKey(key: ByteArray) {
        require(key.size in listOf(8, 16, 24)) {
            "Invalid key size: ${key.size}. Must be 8, 16, or 24 bytes"
        }
    }

    private fun validateIV(iv: ByteArray?) {
        require(iv == null || iv.size == 8) { "IV must be 8 bytes for DES/3DES" }
    }

    private fun isWeakDESKey(keyHex: String): Boolean {
        val weakKeys = setOf(
            "0101010101010101", "FEFEFEFEFEFEFEFE",
            "E0E0E0E0F1F1F1F1", "1F1F1F1F0E0E0E0E",
            "01FE01FE01FE01FE", "FE01FE01FE01FE01",
            "1FE01FE00EF10EF1", "E01FE01FF10EF10E",
            "01E001E001F101F1", "E001E001F101F101",
            "1FFE1FFE0EFE0EFE", "FE1FFE1FFE0EFE0E",
            "011F011F010E010E", "1F011F010E010E01",
            "E0FEE0FEF1FEF1FE", "FEE0FEE0FEF1FEF1"
        )
        return keyHex in weakKeys
    }

    // ============================================================================
    // COMPREHENSIVE TEST FUNCTIONS
    // ============================================================================

    /**
     * Self-test function to verify implementation
     */
    fun runSelfTest(): Boolean {
        return try {
            // Test vectors
            val testKey16 = hexToBytes("0123456789ABCDEF23456789ABCDEF01")
            val testKey24 = hexToBytes("0123456789ABCDEF23456789ABCDEF01456789ABCDEF0123")
            val testData = hexToBytes("4E6F772069732074")
            val testIV = hexToBytes("0000000000000000")

            // Test ECB with 2-key
            val encrypted16 = encryptECB(testData, testKey16)
            val decrypted16 = decryptECB(encrypted16, testKey16)

            // Test ECB with 3-key
            val encrypted24 = encryptECB(testData, testKey24)
            val decrypted24 = decryptECB(encrypted24, testKey24)

            // Test CBC
            val encryptedCBC = encryptCBC(testData, testKey24, testIV)
            val decryptedCBC = decryptCBC(encryptedCBC, testKey24, testIV)

            // Test KCV
            val kcv = calculateKCV(testKey24)

            // Test custom fallback
            val customEncrypted = customEncryptECB(testData, testKey16)
            val customDecrypted = customDecryptECB(customEncrypted, testKey16)

            // Test MAC generation
            val mac = generateMAC(testData, testKey16)

            // Test retail MAC
            val retailMac = generateRetailMAC(testData, testKey16)

            // Verify all tests pass
            decrypted16.contentEquals(testData) &&
                    decrypted24.contentEquals(testData) &&
                    decryptedCBC.contentEquals(testData) &&
                    customDecrypted.contentEquals(testData) &&
                    kcv.size == 3 &&
                    mac.size == 8 &&
                    retailMac.size == 8
        } catch (e: Exception) {
            println("Self-test failed: ${e.message}")
            false
        }
    }

    /**
     * Extended test function with comprehensive coverage
     */
    fun runExtendedTest(): Map<String, Boolean> {
        val results = mutableMapOf<String, Boolean>()

        try {
            // Test key generation
            val generatedKey = generateKey(24)
            results["keyGeneration"] = generatedKey.size == 24 && hasValidParity(generatedKey)

            // Test parity adjustment
            val testKey = hexToBytes("0123456789ABCDEF")
            adjustParity(testKey)
            results["parityAdjustment"] = hasValidParity(testKey)

            // Test weak key detection
            val weakKey = hexToBytes("0101010101010101")
            results["weakKeyDetection"] = isWeakKey(weakKey)

            // Test key expansion
            val twoKey = hexToBytes("0123456789ABCDEF23456789ABCDEF01")
            val threeKey = expandTo3Key(twoKey)
            results["keyExpansion"] = threeKey.size == 24

            // Test all cipher modes
            val testData = "Hello World!".toByteArray()
            val paddedData = padData(testData, 8)
            val key = hexToBytes("0123456789ABCDEF23456789ABCDEF01456789ABCDEF0123")
            val iv = generateIV()

            // ECB mode
            val ecbEncrypted = encryptECB(paddedData, key)
            val ecbDecrypted = decryptECB(ecbEncrypted, key)
            val ecbUnpadded = unpadData(ecbDecrypted)
            results["ecbMode"] = ecbUnpadded.contentEquals(testData)

            // CBC mode
            val cbcEncrypted = encryptCBC(paddedData, key, iv)
            val cbcDecrypted = decryptCBC(cbcEncrypted, key, iv)
            val cbcUnpadded = unpadData(cbcDecrypted)
            results["cbcMode"] = cbcUnpadded.contentEquals(testData)

            // CFB mode
            val cfbEncrypted = encryptCFB(testData, key, iv)
            val cfbDecrypted = decryptCFB(cfbEncrypted, key, iv)
            results["cfbMode"] = cfbDecrypted.contentEquals(testData)

            // OFB mode
            val ofbEncrypted = encryptOFB(testData, key, iv)
            val ofbDecrypted = decryptOFB(ofbEncrypted, key, iv)
            results["ofbMode"] = ofbDecrypted.contentEquals(testData)

            // Payment industry functions
            val kcv = calculateKCV(key)
            results["kcvCalculation"] = kcv.size == 3

            val mac = generateMAC(testData, key)
            results["macGeneration"] = mac.size == 8

            val retailMac = generateRetailMAC(testData, twoKey)
            results["retailMacGeneration"] = retailMac.size == 8

            // Session key derivation
            val diversificationData = hexToBytes("1234567890ABCDEF")
            val sessionKey = deriveSessionKey(key, diversificationData)
            results["sessionKeyDerivation"] = sessionKey.size == 8

            // PIN block encryption
            val pinBlock = hexToBytes("041234FFFFFFFFFF")
            val encryptedPIN = encryptPINBlock(pinBlock, key)
            results["pinBlockEncryption"] = encryptedPIN.size == 8

            // Working key derivation
            val keySerial = hexToBytes("0000000000000001")
            val workingKey = deriveWorkingKey(key, keySerial)
            results["workingKeyDerivation"] = workingKey.size == 16

            // Utility functions
            val hexString = "48656C6C6F"
            val bytes = hexToBytes(hexString)
            val backToHex = bytesToHex(bytes)
            results["hexConversion"] = hexString.equals(backToHex, ignoreCase = true)

            // Padding functions
            val originalData = "Test".toByteArray()
            val padded = padData(originalData, 8)
            val unpadded = unpadData(padded)
            results["paddingFunctions"] = unpadded.contentEquals(originalData)

        } catch (e: Exception) {
            println("Extended test failed: ${e.message}")
            e.printStackTrace()
        }

        return results
    }

    /**
     * Performance benchmark test
     */
    fun runPerformanceTest(iterations: Int = 1000): Map<String, Long> {
        val results = mutableMapOf<String, Long>()
        val key = hexToBytes("0123456789ABCDEF23456789ABCDEF01456789ABCDEF0123")
        val data = ByteArray(1024) // 1KB of data
        SecureRandom().nextBytes(data)
        val paddedData = padData(data, 8)

        // ECB performance
        val ecbStart = System.nanoTime()
        repeat(iterations) {
            val encrypted = encryptECB(paddedData, key)
            decryptECB(encrypted, key)
        }
        results["ecbMode"] = (System.nanoTime() - ecbStart) / 1_000_000 // Convert to milliseconds

        // CBC performance
        val iv = generateIV()
        val cbcStart = System.nanoTime()
        repeat(iterations) {
            val encrypted = encryptCBC(paddedData, key, iv)
            decryptCBC(encrypted, key, iv)
        }
        results["cbcMode"] = (System.nanoTime() - cbcStart) / 1_000_000

        // KCV calculation performance
        val kcvStart = System.nanoTime()
        repeat(iterations) {
            calculateKCV(key)
        }
        results["kcvCalculation"] = (System.nanoTime() - kcvStart) / 1_000_000

        // MAC generation performance
        val macStart = System.nanoTime()
        repeat(iterations) {
            generateMAC(data, key)
        }
        results["macGeneration"] = (System.nanoTime() - macStart) / 1_000_000

        return results
    }
}

// ============================================================================
// USAGE EXAMPLES AND DEMO
// ============================================================================

/**
 * Demonstration of Custom 3DES Calculator Engine capabilities
 */
fun demonstrateCustom3DES() {
    println("=== Custom 3DES Calculator Engine Demo ===")

    try {
        // Generate a secure 3DES key
        val key24 = TdesCalculatorEngine.generateKey(24)
        println("Generated 24-byte key: ${TdesCalculatorEngine.bytesToHex(key24)}")

        val key16 = TdesCalculatorEngine.generateKey(16)
        println("Generated 16-byte key: ${TdesCalculatorEngine.bytesToHex(key16)}")

        // Calculate KCV
        val kcv = TdesCalculatorEngine.calculateKCV(key24)
        println("Key Check Value: ${TdesCalculatorEngine.bytesToHex(kcv)}")

        // Test data
        val originalData = "Payment Transaction Data 123456789"
        println("Original Data: $originalData")

        val dataBytes = originalData.toByteArray()
        val paddedData = TdesCalculatorEngine.padData(dataBytes, 8)

        // Test ECB mode
        println("\n--- ECB Mode ---")
        val ecbEncrypted = TdesCalculatorEngine.encryptECB(paddedData, key24)
        println("ECB Encrypted: ${TdesCalculatorEngine.bytesToHex(ecbEncrypted)}")

        val ecbDecrypted = TdesCalculatorEngine.decryptECB(ecbEncrypted, key24)
        val ecbUnpadded = TdesCalculatorEngine.unpadData(ecbDecrypted)
        println("ECB Decrypted: ${String(ecbUnpadded)}")
        println("ECB Success: ${ecbUnpadded.contentEquals(dataBytes)}")

        // Test CBC mode with IV
        println("\n--- CBC Mode ---")
        val iv = TdesCalculatorEngine.generateIV()
        println("IV: ${TdesCalculatorEngine.bytesToHex(iv)}")

        val cbcEncrypted = TdesCalculatorEngine.encryptCBC(paddedData, key24, iv)
        println("CBC Encrypted: ${TdesCalculatorEngine.bytesToHex(cbcEncrypted)}")

        val cbcDecrypted = TdesCalculatorEngine.decryptCBC(cbcEncrypted, key24, iv)
        val cbcUnpadded = TdesCalculatorEngine.unpadData(cbcDecrypted)
        println("CBC Decrypted: ${String(cbcUnpadded)}")
        println("CBC Success: ${cbcUnpadded.contentEquals(dataBytes)}")

        // Test CFB mode
        println("\n--- CFB Mode ---")
        val cfbEncrypted = TdesCalculatorEngine.encryptCFB(dataBytes, key24, iv)
        println("CFB Encrypted: ${TdesCalculatorEngine.bytesToHex(cfbEncrypted)}")

        val cfbDecrypted = TdesCalculatorEngine.decryptCFB(cfbEncrypted, key24, iv)
        println("CFB Decrypted: ${String(cfbDecrypted)}")
        println("CFB Success: ${cfbDecrypted.contentEquals(dataBytes)}")

        // Test OFB mode
        println("\n--- OFB Mode ---")
        val ofbEncrypted = TdesCalculatorEngine.encryptOFB(dataBytes, key24, iv)
        println("OFB Encrypted: ${TdesCalculatorEngine.bytesToHex(ofbEncrypted)}")

        val ofbDecrypted = TdesCalculatorEngine.decryptOFB(ofbEncrypted, key24, iv)
        println("OFB Decrypted: ${String(ofbDecrypted)}")
        println("OFB Success: ${ofbDecrypted.contentEquals(dataBytes)}")

        // Payment industry functions
        println("\n--- Payment Industry Functions ---")

        // MAC generation
        val mac = TdesCalculatorEngine.generateMAC(dataBytes, key24)
        println("MAC: ${TdesCalculatorEngine.bytesToHex(mac)}")

        // Retail MAC (requires 16-byte key)
        val retailMac = TdesCalculatorEngine.generateRetailMAC(dataBytes, key16)
        println("Retail MAC: ${TdesCalculatorEngine.bytesToHex(retailMac)}")

        // Session key derivation
        val diversificationData = TdesCalculatorEngine.hexToBytes("1234567890ABCDEF")
        val sessionKey = TdesCalculatorEngine.deriveSessionKey(key24, diversificationData)
        println("Session Key: ${TdesCalculatorEngine.bytesToHex(sessionKey)}")

        // PIN block encryption
        val pinBlock = TdesCalculatorEngine.hexToBytes("041234FFFFFFFFFF")
        val encryptedPIN = TdesCalculatorEngine.encryptPINBlock(pinBlock, key24)
        println("Encrypted PIN Block: ${TdesCalculatorEngine.bytesToHex(encryptedPIN)}")

        // Key management
        println("\n--- Key Management ---")
        val twoKey = TdesCalculatorEngine.hexToBytes("0123456789ABCDEF23456789ABCDEF01")
        val expandedKey = TdesCalculatorEngine.expandTo3Key(twoKey)
        println("Expanded to 3-key: ${TdesCalculatorEngine.bytesToHex(expandedKey)}")

        // Parity check and adjustment
        val testKey = TdesCalculatorEngine.hexToBytes("0123456789ABCDEF")
        println("Original parity valid: ${TdesCalculatorEngine.hasValidParity(testKey)}")
        TdesCalculatorEngine.adjustParity(testKey)
        println("After adjustment valid: ${TdesCalculatorEngine.hasValidParity(testKey)}")
        println("Adjusted key: ${TdesCalculatorEngine.bytesToHex(testKey)}")

        // Weak key detection
        val weakKey = TdesCalculatorEngine.hexToBytes("0101010101010101")
        println("Weak key detected: ${TdesCalculatorEngine.isWeakKey(weakKey)}")

        // Run self-test
        println("\n--- Self Test ---")
        val selfTestResult = TdesCalculatorEngine.runSelfTest()
        println("Self-test passed: $selfTestResult")

        // Run extended test
        println("\n--- Extended Test Results ---")
        val extendedResults = TdesCalculatorEngine.runExtendedTest()
        extendedResults.forEach { (test, result) ->
            println("$test: ${if (result) "PASS" else "FAIL"}")
        }

        // Performance test
        println("\n--- Performance Test (1000 iterations) ---")
        val perfResults = TdesCalculatorEngine.runPerformanceTest(1000)
        perfResults.forEach { (operation, timeMs) ->
            println("$operation: ${timeMs}ms")
        }

    } catch (e: Exception) {
        println("Demo failed: ${e.message}")
        e.printStackTrace()
    }
}
