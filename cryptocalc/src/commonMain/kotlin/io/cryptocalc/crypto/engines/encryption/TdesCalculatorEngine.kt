package io.cryptocalc.crypto.engines.encryption

import ai.cortex.core.toHexUpper
import ai.cortex.core.types.CipherMode
import ai.cortex.core.types.PaddingMethods
import ai.cortex.core.types.PaddingMethods.NONE
import io.cryptocalc.crypto.primitives.Mac
import io.cryptocalc.crypto.primitives.Modes
import io.cryptocalc.crypto.primitives.Tdes
import io.cryptocalc.crypto.primitives.secureRandomBytes

/**
 * DES / Triple DES engine for payment-industry calculators, backed by the pure-Kotlin [Tdes]
 * block cipher so the desktop app and the browser tools compute identical results.
 *
 * - 8-byte keys run single DES, 16-byte keys two-key TDES (K1 K2 K1), 24-byte keys three-key TDES.
 * - All standard cipher modes: ECB, CBC, CFB, OFB.
 * - Common payment functions: KCV, CBC-MAC, Retail MAC (ANSI X9.19), session key derivation.
 */
object TdesCalculatorEngine {

    // ============================================================================
    // CORE ENCRYPTION/DECRYPTION FUNCTIONS
    // ============================================================================

    fun encryptCBC(data: ByteArray, key: ByteArray, iv: ByteArray? = null, padding: PaddingMethods = NONE): ByteArray =
        Modes.encrypt(Tdes(key), data, CipherMode.CBC, iv, padding)

    fun decryptCBC(data: ByteArray, key: ByteArray, iv: ByteArray? = null, padding: PaddingMethods = NONE): ByteArray =
        Modes.decrypt(Tdes(key), data, CipherMode.CBC, iv, padding)

    fun encryptECB(data: ByteArray, key: ByteArray, padding: PaddingMethods = NONE): ByteArray =
        Modes.encrypt(Tdes(key), data, CipherMode.ECB, null, padding)

    fun decryptECB(data: ByteArray, key: ByteArray, padding: PaddingMethods = NONE): ByteArray =
        Modes.decrypt(Tdes(key), data, CipherMode.ECB, null, padding)

    fun encryptCFB(data: ByteArray, key: ByteArray, iv: ByteArray? = null, padding: PaddingMethods = NONE): ByteArray =
        Modes.encrypt(Tdes(key), data, CipherMode.CFB, iv, padding)

    fun decryptCFB(data: ByteArray, key: ByteArray, iv: ByteArray? = null, padding: PaddingMethods = NONE): ByteArray =
        Modes.decrypt(Tdes(key), data, CipherMode.CFB, iv, padding)

    fun encryptOFB(data: ByteArray, key: ByteArray, iv: ByteArray? = null, padding: PaddingMethods = NONE): ByteArray =
        Modes.encrypt(Tdes(key), data, CipherMode.OFB, iv, padding)

    fun decryptOFB(data: ByteArray, key: ByteArray, iv: ByteArray? = null, padding: PaddingMethods = NONE): ByteArray =
        Modes.decrypt(Tdes(key), data, CipherMode.OFB, iv, padding)

    /** Generic entry point: any supported [mode] with optional [iv] and [padding]. */
    fun encrypt(data: ByteArray, key: ByteArray, mode: CipherMode, iv: ByteArray? = null, padding: PaddingMethods = NONE): ByteArray =
        Modes.encrypt(Tdes(key), data, mode, iv, padding)

    fun decrypt(data: ByteArray, key: ByteArray, mode: CipherMode, iv: ByteArray? = null, padding: PaddingMethods = NONE): ByteArray =
        Modes.decrypt(Tdes(key), data, mode, iv, padding)

    // ============================================================================
    // PAYMENT INDUSTRY SPECIFIC FUNCTIONS
    // ============================================================================

    /** Key Check Value: first 3 bytes of the encryption of an all-zero block. */
    fun calculateKCV(key: ByteArray): ByteArray = Mac.kcvTdes(key)

    /**
     * CBC-MAC (ISO 9797-1 Algorithm 1) with ISO 9797-1 Method 2 padding ('80' then zeros),
     * returning the last 8-byte block.
     */
    fun generateCBC_MAC(data: ByteArray, key: ByteArray, iv: ByteArray = ByteArray(8)): ByteArray {
        require(data.isNotEmpty()) { "Data for MAC generation cannot be empty." }
        return Mac.algorithm1(Tdes(key), padLegacy(data), Mac.Padding.METHOD_1, iv)
    }

    /** Retail MAC (ANSI X9.19 / ISO 9797-1 Algorithm 3) with Method 2 padding and a 16-byte key. */
    fun generateRetailMAC(data: ByteArray, key: ByteArray): ByteArray {
        require(key.size == 16) { "Retail MAC (ANSI X9.19) requires a 16-byte (double-length) key." }
        return Mac.ansiX919(key, padLegacy(data), Mac.Padding.METHOD_1)
    }

    /**
     * Historical behaviour of this engine: data that is already a multiple of 8 bytes is left
     * alone, otherwise ISO 9797-1 Method 2 ('80' then zeros) is applied. Kept so desktop
     * results stay byte-identical.
     */
    private fun padLegacy(data: ByteArray): ByteArray {
        val rem = data.size % 8
        if (rem == 0 && data.isNotEmpty()) return data
        val padded = data.copyOf(data.size + (8 - rem))
        padded[data.size] = 0x80.toByte()
        return padded
    }

    /** Derives a session key by encrypting 8 or 16 bytes of diversification data under the master key. */
    fun deriveSessionKey(masterKey: ByteArray, diversificationData: ByteArray): ByteArray {
        require(diversificationData.size == 8 || diversificationData.size == 16) { "Diversification data must be 8 or 16 bytes." }
        return encryptECB(diversificationData, masterKey)
    }

    /** Encrypts an 8-byte PIN block (ISO 9564 formats 0-3) under [key]. */
    fun encryptPINBlock(pinBlock: ByteArray, key: ByteArray): ByteArray {
        require(pinBlock.size == 8) { "PIN block must be 8 bytes." }
        return encryptECB(pinBlock, key)
    }

    // ============================================================================
    // KEY MANAGEMENT FUNCTIONS
    // ============================================================================

    /** Generates a random DES/TDES key with odd parity that is not a known weak key. */
    fun generateKey(keySizeInBytes: Int = 24): ByteArray {
        require(keySizeInBytes in listOf(8, 16, 24)) { "Key size must be 8, 16, or 24 bytes." }
        var key: ByteArray
        do {
            key = secureRandomBytes(keySizeInBytes)
            adjustParity(key)
        } while (isWeakKey(key))
        return key
    }

    /** True when every byte of [key] has odd parity. */
    fun hasValidParity(key: ByteArray): Boolean =
        key.all { byte -> (byte.toInt() and 0xFF).countOneBits() % 2 != 0 }

    /** Adjusts the least significant bit of each byte so the byte has odd parity. */
    fun adjustParity(key: ByteArray) {
        for (i in key.indices) {
            val byteValue = key[i].toInt()
            key[i] = if ((byteValue and 0xFE).countOneBits() % 2 == 0) {
                (byteValue or 0x01).toByte()
            } else {
                (byteValue and 0xFE).toByte()
            }
        }
    }

    /** True when any 8-byte component of [key] is a known weak or semi-weak DES key. */
    fun isWeakKey(key: ByteArray): Boolean =
        key.asSequence().chunked(8).any { it.toByteArray().toHexUpper() in Tdes.WEAK_KEYS }
}
