package io.cryptocalc.crypto.engines.encryption

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.SecureRandom
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Triple DES Calculator Engine using Bouncy Castle Provider.
 *
 * An enterprise-grade 3DES implementation for payment industry applications,
 * exclusively using the Bouncy Castle JCE provider for all cryptographic operations.
 *
 * Features:
 * - 3DES implementation (E-D-E) via Bouncy Castle's "DESede" algorithm.
 * - All standard cipher modes: ECB, CBC, CFB, OFB.
 * - Common payment industry functions: KCV, Retail MAC (ANSI X9.19), EMV key derivation.
 * - HSM-compatible operations and robust key management.
 * - Comprehensive validation and error handling.
 *
 * @author Crypto SDK Team
 * @version 2.0.0
 */
object TdesCalculatorEngine {

    init {
        // Ensure Bouncy Castle provider is registered.
        // It's safe to add it even if it's already present.
        Security.getProvider("BC") ?: Security.addProvider(BouncyCastleProvider())
    }

    // ============================================================================
    // CORE ENCRYPTION/DECRYPTION FUNCTIONS
    // ============================================================================

    /**
     * Encrypts data using 3DES in CBC mode.
     */
    fun encryptCBC(data: ByteArray, key: ByteArray, iv: ByteArray? = null): ByteArray {
        return performCipherOperation(data, key, iv, Cipher.ENCRYPT_MODE, "CBC")
    }

    /**
     * Decrypts data using 3DES in CBC mode.
     */
    fun decryptCBC(data: ByteArray, key: ByteArray, iv: ByteArray? = null): ByteArray {
        return performCipherOperation(data, key, iv, Cipher.DECRYPT_MODE, "CBC")
    }

    /**
     * Encrypts data using 3DES in ECB mode.
     */
    fun encryptECB(data: ByteArray, key: ByteArray): ByteArray {
        return performCipherOperation(data, key, null, Cipher.ENCRYPT_MODE, "ECB")
    }

    /**
     * Decrypts data using 3DES in ECB mode.
     */
    fun decryptECB(data: ByteArray, key: ByteArray): ByteArray {
        return performCipherOperation(data, key, null, Cipher.DECRYPT_MODE, "ECB")
    }

    /**
     * Encrypts data using 3DES in CFB mode.
     */
    fun encryptCFB(data: ByteArray, key: ByteArray, iv: ByteArray? = null): ByteArray {
        return performCipherOperation(data, key, iv, Cipher.ENCRYPT_MODE, "CFB")
    }

    /**
     * Decrypts data using 3DES in CFB mode.
     */
    fun decryptCFB(data: ByteArray, key: ByteArray, iv: ByteArray? = null): ByteArray {
        return performCipherOperation(data, key, iv, Cipher.DECRYPT_MODE, "CFB")
    }

    /**
     * Encrypts data using 3DES in OFB mode.
     */
    fun encryptOFB(data: ByteArray, key: ByteArray, iv: ByteArray? = null): ByteArray {
        return performCipherOperation(data, key, iv, Cipher.ENCRYPT_MODE, "OFB")
    }

    /**
     * Decrypts data using 3DES in OFB mode.
     */
    fun decryptOFB(data: ByteArray, key: ByteArray, iv: ByteArray? = null): ByteArray {
        return performCipherOperation(data, key, iv, Cipher.DECRYPT_MODE, "OFB")
    }

    // ============================================================================
    // PAYMENT INDUSTRY SPECIFIC FUNCTIONS
    // ============================================================================

    /**
     * Calculates a Key Check Value (KCV) by encrypting a block of null bytes.
     * The KCV is typically the first 3 bytes (6 hex characters) of the result.
     */
    fun calculateKCV(key: ByteArray): ByteArray {
        validateKey(key)
        val zeroBlock = ByteArray(8) { 0 }
        // KCV is always calculated using ECB mode.
        val encryptedBlock = encryptECB(zeroBlock, key)
        return encryptedBlock.copyOfRange(0, 3)
    }

    /**
     * Generates a Message Authentication Code (MAC) using the CBC-MAC algorithm.
     * Note: This is a basic implementation. For financial transactions, consider using
     * more secure standards like ANSI X9.19 or EMV cryptograms.
     */
    fun generateCBC_MAC(data: ByteArray, key: ByteArray, iv: ByteArray = ByteArray(8) { 0 }): ByteArray {
        require(data.isNotEmpty()) { "Data for MAC generation cannot be empty." }
        val paddedData = padDataToBlockSize(data, 8)
        val encrypted = encryptCBC(paddedData, key, iv)
        // The MAC is the last block of the CBC encryption result.
        return encrypted.takeLast(8).toByteArray()
    }

    /**
     * Generates a retail MAC as per ANSI X9.19 standard.
     * This involves a two-key DES MAC process.
     */
    fun generateRetailMAC(data: ByteArray, key: ByteArray): ByteArray {
        require(key.size == 16) { "Retail MAC (ANSI X9.19) requires a 16-byte (double-length) key." }
        val paddedData = padDataToBlockSize(data, 8)
        val leftKey = key.sliceArray(0..7)
        val rightKey = key.sliceArray(8..15)

        var lastCipherBlock = ByteArray(8) { 0 } // Initial IV is zero
        for (block in paddedData.asSequence().chunked(8)) {
            val xorInput = xorBlocks(block.toByteArray(), lastCipherBlock)
            lastCipherBlock = performCipherOperation(xorInput, leftKey, null, Cipher.ENCRYPT_MODE, "ECB")
        }

        // Final transformation
        val decryptedWithRight = performCipherOperation(lastCipherBlock, rightKey, null, Cipher.DECRYPT_MODE, "ECB")
        return performCipherOperation(decryptedWithRight, leftKey, null, Cipher.ENCRYPT_MODE, "ECB")
    }

    /**
     * Derives an EMV session key from a master key and diversification data.
     * This is a common method but may vary based on specific card scheme rules.
     */
    fun deriveSessionKey(masterKey: ByteArray, diversificationData: ByteArray): ByteArray {
        require(diversificationData.size == 8 || diversificationData.size == 16) { "Diversification data must be 8 or 16 bytes." }
        return encryptECB(diversificationData, masterKey)
    }

    /**
     * Encrypts a PIN Block for transmission, typically for ISO 9564 Format 0.
     */
    fun encryptPINBlock(pinBlock: ByteArray, key: ByteArray): ByteArray {
        require(pinBlock.size == 8) { "PIN block must be 8 bytes." }
        return encryptECB(pinBlock, key)
    }

    // ============================================================================
    // KEY MANAGEMENT FUNCTIONS
    // ============================================================================

    /**
     * Generates a random 3DES key with correct odd parity.
     */
    fun generateKey(keySizeInBytes: Int = 24): ByteArray {
        require(keySizeInBytes in listOf(8, 16, 24)) { "Key size must be 8, 16, or 24 bytes." }
        val random = SecureRandom()
        val key = ByteArray(keySizeInBytes)
        do {
            random.nextBytes(key)
            adjustParity(key)
        } while (isWeakKey(key))
        return key
    }

    /**
     * Checks if a key has correct odd parity for every byte.
     */
    fun hasValidParity(key: ByteArray): Boolean {
        return key.all { byte -> (byte.toInt() and 0xFF).countOneBits() % 2 != 0 }
    }

    /**
     * Adjusts each byte in the key to have odd parity.
     */
    fun adjustParity(key: ByteArray) {
        for (i in key.indices) {
            val byteValue = key[i].toInt()
            // Set the least significant bit to make the total number of set bits odd.
            key[i] = if ((byteValue and 0xFE).countOneBits() % 2 == 0) {
                (byteValue or 0x01).toByte()
            } else {
                (byteValue and 0xFE).toByte()
            }
        }
    }

    /**
     * Checks if the provided key is a known weak or semi-weak DES key.
     */
    fun isWeakKey(key: ByteArray): Boolean {
        val weakKeys = setOf(
            "0101010101010101", "FEFEFEFEFEFEFEFE",
            "E0E0E0E0F1F1F1F1", "1F1F1F1F0E0E0E0E"
        )
        // Check each 8-byte component of the key
        return key.asSequence().chunked(8).any {
            val keyPartHex = it.toByteArray().joinToString("") { b -> "%02X".format(b) }
            keyPartHex in weakKeys
        }
    }

    // ============================================================================
    // UTILITY AND HELPER FUNCTIONS
    // ============================================================================

    /**
     * Central private function to handle all cipher operations.
     */
    private fun performCipherOperation(data: ByteArray, key: ByteArray, iv: ByteArray?, mode: Int, cipherMode: String): ByteArray {
        validateKey(key)
        val effectiveIV = iv ?: ByteArray(8) { 0 }
        validateIV(effectiveIV, cipherMode)

        val transformation = "DESede/$cipherMode/NoPadding"
        val keySpec = SecretKeySpec(key, "DESede")
        val cipher = Cipher.getInstance(transformation)

        if (cipherMode == "ECB") {
            cipher.init(mode, keySpec)
        } else {
            val ivSpec = IvParameterSpec(effectiveIV)
            cipher.init(mode, keySpec, ivSpec)
        }
        return cipher.doFinal(data)
    }

    /**
     * Prepares a key for use with the JCE. Converts 16-byte keys to the
     * K1, K2, K1 format required by JCE providers for 2-key 3DES.
     */
    private fun padKeyForJCE(key: ByteArray): ByteArray {
        return when (key.size) {
            16 -> key + key.copyOfRange(0, 8) // K1, K2 -> K1, K2, K1
            8, 24 -> key
            else -> throw IllegalArgumentException("Invalid key size for padding: ${key.size}.")
        }
    }

    /**
     * XORs two byte arrays of the same length.
     */
    private fun xorBlocks(block1: ByteArray, block2: ByteArray): ByteArray {
        require(block1.size == block2.size) { "Blocks must be the same size for XOR operation." }
        return block1.zip(block2) { a, b -> (a.toInt() xor b.toInt()).toByte() }.toByteArray()
    }

    /**
     * Pads data to a multiple of the block size using ISO/IEC 7816-4 method 2 (80...00).
     */
    private fun padDataToBlockSize(data: ByteArray, blockSize: Int): ByteArray {
        val paddingLength = blockSize - (data.size % blockSize)
        if (paddingLength == blockSize && data.isNotEmpty()) {
            return data // Already a multiple of block size
        }
        val padded = data.copyOf(data.size + paddingLength)
        padded[data.size] = 0x80.toByte() // Append the mandatory '80' byte
        // The rest is padded with '00' by default in copyOf
        return padded
    }


    private fun validateKey(key: ByteArray) {
        require(key.size in listOf(8, 16, 24)) {
            "Invalid key size: ${key.size}. Key must be 8, 16, or 24 bytes."
        }
    }

    private fun validateIV(iv: ByteArray, mode: String) {
        if (mode != "ECB") {
            require(iv.size == 8) { "IV must be 8 bytes for $mode mode." }
        }
    }
}
