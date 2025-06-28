package io.cryptocalc.crypto.engines.applicationCryptogram

import ai.cortex.core.types.AlgorithmType
import ai.cortex.core.types.CryptoAlgorithm
import ai.cortex.core.types.PaddingMethods
import io.cryptocalc.crypto.engines.encryption.EMVEngines
import io.cryptocalc.emv.calculators.emv41.CryptogramInput

class AcEngineImpl(override val emvEngines: EMVEngines) : AcEngine {

    override suspend fun generateAC(
        algorithm: CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>,
        cryptogramInput: CryptogramInput
    ) {
        // Validate session key length
        if (cryptogramInput.sessionKey.size != 16) {
            throw IllegalArgumentException("Session Key must be a double length DES key (16 bytes), got ${cryptogramInput.sessionKey.size} bytes")
        }
    }


    suspend fun generateAc(
        skAc: ByteArray,
        data: ByteArray,
        paddingType: PaddingType = PaddingType.EMV,
        length: Int = 8
    ): ByteArray {
        // Validate session key length
        if (skAc.size != 16) {
            throw IllegalArgumentException("Session Key must be a double length DES key (16 bytes), got ${skAc.size} bytes")
        }

        // Validate length parameter
        if (length < 4 || length > 8) {
            throw IllegalArgumentException("Length must be between 4 and 8 bytes, got $length")
        }

        // Extract left and right key parts
        val leftKey = skAc.sliceArray(0..7)
        val rightKey = skAc.sliceArray(8..15)

        // Generate MAC using ISO/IEC 9797-1 Algorithm 3
        return macIso9797Algorithm3(leftKey, rightKey, data, paddingType, length)
    }


    /**
     * MAC algorithm using ISO/IEC 9797-1 Algorithm 3 with Triple DES
     * This is the core MAC calculation for EMV Application Cryptograms
     */
    private suspend fun macIso9797Algorithm3(
        leftKey: ByteArray,
        rightKey: ByteArray,
        data: ByteArray,
        paddingType: PaddingMethods,
        length: Int
    ): ByteArray {
        // Apply padding according to the specified method
        val paddedData = when (paddingType) {
            PaddingMethods.METHOD_1_ISO_9797 -> applyVisaPadding(data)
            PaddingMethods.METHOD_2_ISO_9797  -> applyEmvPadding(data)
            else -> throw IllegalArgumentException("Invalid padding type: $paddingType")
        }

        // Process data in 8-byte blocks using CBC mode with left key
        var result = ByteArray(8) // Initialize with zeros

        for (i in paddedData.indices step 8) {
            val block = paddedData.sliceArray(i until minOf(i + 8, paddedData.size))

            // XOR with previous result
            val xorBlock = ByteArray(8)
            for (j in block.indices) {
                xorBlock[j] = (block[j].toInt() xor result[j].toInt()).toByte()
            }

            // Encrypt with left key
            result = cryptoEngine.encrypt("DES", xorBlock, leftKey, mapOf("mode" to "ECB"))
        }

        // Final step: decrypt with right key, then encrypt with left key (Algorithm 3)
        val decrypted = cryptoEngine.decrypt("DES", result, rightKey, mapOf("mode" to "ECB"))
        val finalResult = cryptoEngine.encrypt("DES", decrypted, leftKey, mapOf("mode" to "ECB"))

        // Return the requested number of bytes
        return finalResult.sliceArray(0 until length)
    }
}