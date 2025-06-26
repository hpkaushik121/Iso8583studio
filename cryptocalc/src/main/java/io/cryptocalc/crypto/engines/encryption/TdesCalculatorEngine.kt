package io.cryptocalc.crypto.engines.encryption

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

/**
 * Triple DES Calculator Engine
 *
 * Enterprise-grade custom 3DES implementation for payment industry applications.
 * Provides fallback implementation when standard JCE fails or is unavailable.
 *
 * Features:
 * - 3DES implementation (E-D-E and D-E-D operations)
 * - All cipher modes: ECB, CBC, CFB, OFB with custom implementations
 * - Payment industry functions (KCV, MAC, key derivation)
 * - HSM-compatible operations
 * - Comprehensive validation and error handling
 *
 * @author Crypto SDK Team
 * @version 1.0.0
 */
internal object TdesCalculatorEngine {

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
    private fun padKey(key: ByteArray): Triple<ByteArray, ByteArray, ByteArray> {
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

        val transformation = getTransformation(key.size, "CBC")
        val algorithm = getAlgorithm(key.size)

        val keySpec = SecretKeySpec(key, algorithm)
        val ivSpec = IvParameterSpec(actualIV)
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        return cipher.doFinal(data)
    }


    /**
     * Decrypt data using 3DES in CBC mode with custom fallback
     */
    fun decryptCBC(data: ByteArray, key: ByteArray, iv: ByteArray? = ByteArray(8)): ByteArray {
        validateKey(key)
        val actualIV = iv ?: ByteArray(8)
        validateIV(actualIV)

        val transformation = getTransformation(key.size, "CBC")
        val algorithm = getAlgorithm(key.size)

        val keySpec = SecretKeySpec(key, algorithm)
        val ivSpec = IvParameterSpec(actualIV)
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        return cipher.doFinal(data)
    }


    /**
     * Encrypt data using 3DES in ECB mode with custom fallback
     */
    fun encryptECB(data: ByteArray, key: ByteArray): ByteArray {
        validateKey(key)

        val transformation = getTransformation(key.size, "ECB")
        val algorithm = getAlgorithm(key.size)

        val keySpec = SecretKeySpec(key, algorithm)
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        return cipher.doFinal(data)
    }


    /**
     * Decrypt data using 3DES in ECB mode with custom fallback
     */
    fun decryptECB(data: ByteArray, key: ByteArray): ByteArray {
        validateKey(key)

        val transformation = getTransformation(key.size, "ECB")
        val algorithm = getAlgorithm(key.size)

        val keySpec = SecretKeySpec(key, algorithm)
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.DECRYPT_MODE, keySpec)
        return cipher.doFinal(data)
    }

    /**
     * Encrypt data using 3DES in CFB mode with custom implementation
     */
    fun encryptCFB(data: ByteArray, key: ByteArray, iv: ByteArray? = ByteArray(8)): ByteArray {
        validateKey(key)
        val actualIV = iv ?: ByteArray(8)
        validateIV(actualIV)

        val transformation = getTransformation(key.size, "CFB")
        val algorithm = getAlgorithm(key.size)

        val keySpec = SecretKeySpec(key, algorithm)
        val ivSpec = IvParameterSpec(actualIV)
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        return cipher.doFinal(data)
    }

    /**
     * Decrypt data using 3DES in CFB mode with custom implementation
     */
    fun decryptCFB(data: ByteArray, key: ByteArray, iv: ByteArray? = ByteArray(8)): ByteArray {
        validateKey(key)
        val actualIV = iv ?: ByteArray(8)
        validateIV(actualIV)

        val transformation = getTransformation(key.size, "CFB")
        val algorithm = getAlgorithm(key.size)

        val keySpec = SecretKeySpec(key, algorithm)
        val ivSpec = IvParameterSpec(actualIV)
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        return cipher.doFinal(data)
    }

    /**
     * Encrypt data using 3DES in OFB mode with custom implementation
     */
    fun encryptOFB(data: ByteArray, key: ByteArray, iv: ByteArray? = ByteArray(8)): ByteArray {
        validateKey(key)
        val actualIV = iv ?: ByteArray(8)
        validateIV(actualIV)

        val transformation = getTransformation(key.size, "OFB")
        val algorithm = getAlgorithm(key.size)

        val keySpec = SecretKeySpec(key, algorithm)
        val ivSpec = IvParameterSpec(actualIV)
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        return cipher.doFinal(data)
    }

    /**
     * Decrypt data using 3DES in OFB mode with custom implementation
     */
    fun decryptOFB(data: ByteArray, key: ByteArray, iv: ByteArray? = ByteArray(8)): ByteArray {
        validateKey(key)
        val actualIV = iv ?: ByteArray(8)
        validateIV(actualIV)

        // Try standard JCE first
        val transformation = getTransformation(key.size, "OFB")
        val algorithm = getAlgorithm(key.size)

        val keySpec = SecretKeySpec(key, algorithm)
        val ivSpec = IvParameterSpec(actualIV)
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        return cipher.doFinal(data)
    }


    // ============================================================================
    // PAYMENT INDUSTRY SPECIFIC FUNCTIONS
    // ============================================================================

    /**
     * Calculate Key Check Value (KCV) - First 3 bytes of encrypting 8 zeros
     */
    fun calculateKCV(key: ByteArray): ByteArray {
        val zeroBlock = ByteArray(8) // 8 bytes of zeros for DES block size

        val encrypted = when (key.size) {
            8 -> {
                // Single DES - create Triple DES key by repeating
                val tripleDesKey = key + key + key
                encryptECB(zeroBlock, tripleDesKey)
            }
            16 -> {
                // Double DES - create Triple DES key K1+K2+K1
                val tripleDesKey = key + key.copyOfRange(0, 8)
                encryptECB(zeroBlock, tripleDesKey)
            }
            24,64 -> {
                // Triple DES - use key as-is
                encryptECB(zeroBlock, key)
            }
            else -> throw IllegalArgumentException("Unsupported key size: ${key.size}")
        }
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
}

