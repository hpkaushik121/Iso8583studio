package io.cryptocalc.crypto.engines.keys

import ai.cortex.core.types.AlgorithmType
import ai.cortex.core.types.CipherMode
import ai.cortex.core.types.CryptoAlgorithm
import io.cryptocalc.crypto.engines.encryption.models.SymmetricEncryptionEngineParameters
import io.cryptocalc.emv.calculators.emv41.SessionKeyType

internal fun KeysEngineImpl.createEMVDiversificationValue(
    atc: ByteArray?=null,
    applicationCryptogram: ByteArray?=null,
    sessionKeyType: SessionKeyType,
    blockSize: Int
): ByteArray {

    return when (sessionKeyType) {
        SessionKeyType.APPLICATION_CRYPTOGRAM -> {
            // For AC/ARPC: R := ATC || '00' || '00' || … || '00' || '00' || '00'
            // ATC followed by n-2 bytes of '00'
            requireNotNull(atc) { "ATC is required for Application Cryptogram session key" }

            val diversificationValue = ByteArray(blockSize)

            // Copy ATC to first bytes (ATC is typically 2 bytes)
            val atcSize = minOf(atc.size, blockSize)
            atc.copyInto(diversificationValue, 0, 0, atcSize)

            // Remaining bytes are already zero-filled (n-2 bytes of '00')
            diversificationValue
        }

        SessionKeyType.SECURE_MESSAGING_MAC,
        SessionKeyType.SECURE_MESSAGING_ENC -> {
            // For SM: R := Application Cryptogram || '00' || '00' || '00'
            // AC followed by n-8 bytes of '00'
            requireNotNull(applicationCryptogram) { "Application Cryptogram is required for Secure Messaging session keys" }

            val diversificationValue = ByteArray(blockSize)

            // Copy Application Cryptogram to first bytes (AC is typically 8 bytes)
            val acSize = minOf(applicationCryptogram.size, 8, blockSize)
            applicationCryptogram.copyInto(diversificationValue, 0, 0, acSize)

            // Remaining bytes are already zero-filled (n-8 bytes of '00')
            diversificationValue
        }
    }
}

/**
 * Performs EMV session key derivation according to specification
 */
suspend fun KeysEngineImpl.performSessionKeyDerivation(
    masterKey: ByteArray,
    diversificationValue: ByteArray,
    iv: ByteArray,
    algorithm: CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>,
    keyBits: Int,
    blockSize: Int
): ByteArray {

    val n = blockSize
    val k = keyBits

    return when {
        // For k = 8n (AES with k=128): SK := ALG (MK) [ R ]
        k == 8 * n -> {
            emvEngines.encryptionEngine.encrypt(
                algorithm = algorithm,
                encryptionEngineParameters = SymmetricEncryptionEngineParameters(
                    data = diversificationValue,
                    key = masterKey,
                    mode = CipherMode.ECB,
                    iv = iv
                )
            )
        }

        // For 16n ≥ k > 8n (Triple DES with k=128 or AES with k=192 or 256)
        k > 8 * n && k <= 16 * n -> {
            // F1 = R0 || R1 || 'F0' || … || Rn-1
            val f1 = createF1Block(diversificationValue, n)

            // F2 = R0 || R1 || '0F' || … || Rn-1
            val f2 = createF2Block(diversificationValue, n)

            // SK := Leftmost k-bits of {ALG (MK) [F1] || ALG (MK) [F2] }
            val result1 = emvEngines.encryptionEngine.encrypt(
                    algorithm = algorithm,
                    encryptionEngineParameters = SymmetricEncryptionEngineParameters(
                        data = f1,
                        key = masterKey,
                        mode = CipherMode.ECB,
                        iv = iv
                    )
                )

            val result2 = emvEngines.encryptionEngine.encrypt(
                algorithm = algorithm,
                encryptionEngineParameters = SymmetricEncryptionEngineParameters(
                    data = f2,
                    key = masterKey,
                    mode = CipherMode.ECB,
                    iv = iv
                )
            )

            val combinedResult = result1 + result2
            val keyBytes = k / 8

            // Return leftmost k-bits (k/8 bytes)
            combinedResult.sliceArray(0 until keyBytes)
        }

        else -> throw IllegalArgumentException("Unsupported key configuration: k=$k, n=$n")
    }
}

/**
 * Creates F1 block: F1 = R0 || R1 || 'F0' || … || Rn-1
 */
private fun createF1Block(diversificationValue: ByteArray, blockSize: Int): ByteArray {
    val f1 = diversificationValue.copyOf()
    if (f1.size >= 3) {
        f1[2] = 0xF0.toByte()
    }
    return f1
}

/**
 * Creates F2 block: F2 = R0 || R1 || '0F' || … || Rn-1
 */
private fun createF2Block(diversificationValue: ByteArray, blockSize: Int): ByteArray {
    val f2 = diversificationValue.copyOf()
    if (f2.size >= 3) {
        f2[2] = 0x0F.toByte()
    }
    return f2
}