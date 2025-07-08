package io.cryptocalc.crypto.engines.applicationCryptogram

import ai.cortex.core.types.AlgorithmType
import ai.cortex.core.types.CipherMode
import ai.cortex.core.types.CryptoAlgorithm
import ai.cortex.core.types.PaddingMethods
import io.cryptocalc.crypto.engines.encryption.EMVEngines
import io.cryptocalc.crypto.engines.encryption.models.SymmetricDecryptionEngineParameters
import io.cryptocalc.crypto.engines.encryption.models.SymmetricEncryptionEngineParameters
import io.cryptocalc.emv.calculators.acCalculator.AcCalculatorInput

internal class AcEngineImpl(override val emvEngines: EMVEngines) : AcEngine {

    override suspend fun generateAC(
        algorithm: CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>,
        acCalculatorInput: AcCalculatorInput
    ): ByteArray {
        // Validate session key length
        if (acCalculatorInput.sessionKey.size != 16) {
            throw IllegalArgumentException("Session Key must be a double length DES key (16 bytes), got ${acCalculatorInput.sessionKey.size} bytes")
        }
        return generateAc(
            algorithm = algorithm,
            skAc = acCalculatorInput.sessionKey,
            data = acCalculatorInput.terminalData + acCalculatorInput.iccData,
            paddingType = acCalculatorInput.paddingMethods,
            length = 8
        )
    }


    suspend fun generateAc(
        algorithm: CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>,
        skAc: ByteArray,
        data: ByteArray,
        paddingType: PaddingMethods = PaddingMethods.METHOD_1_ISO_9797,
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

        // Generate MAC using ISO/IEC 9797-1 Algorithm
        return macIso9797Algorithm(algorithm,leftKey, rightKey, data, paddingType, length)
    }


    /**
     * MAC algorithm using ISO/IEC 9797-1 Algorithm 3 with Triple DES
     * This is the core MAC calculation for EMV Application Cryptograms
     */
    private suspend fun macIso9797Algorithm(
        algorithm: CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>,
        leftKey: ByteArray,
        rightKey: ByteArray,
        data: ByteArray,
        paddingType: PaddingMethods,
        length: Int
    ): ByteArray {
        // Apply padding according to the specified method
        val paddedData = when (paddingType) {
            PaddingMethods.METHOD_1_ISO_9797 -> {
                val remainder = data.size % 8
                if (remainder > 0){
                    val paddingSize = 8 - remainder
                    val padding = ByteArray(paddingSize) { 0x00 }
                    val paddedData = data + padding
                    paddedData
                }else if(data.isEmpty()){
                    ByteArray(8)
                }else{
                    data
                }
            }
            PaddingMethods.METHOD_2_ISO_9797  -> {
                val withMandatoryPadding = data + byteArrayOf(0x80.toByte())
                val remainder = withMandatoryPadding.size % 8
                if (remainder > 0){
                    val paddingSize = 8 - remainder
                    val padding = ByteArray(paddingSize) { 0x00 }
                    val paddedData = withMandatoryPadding + padding
                    paddedData
                }else if(withMandatoryPadding.isEmpty()){
                    ByteArray(8)
                }else{
                    withMandatoryPadding
                }
            }
            else -> throw IllegalArgumentException("Invalid padding type: $paddingType")
        }

        // Process data in 8-byte blocks using CBC mode with left key
        var result = emvEngines.encryptionEngine.encrypt(
            algorithm = algorithm,
            encryptionEngineParameters = SymmetricEncryptionEngineParameters(
                data = paddedData,
                key = leftKey,
                mode = CipherMode.CBC
            )
        ).takeLast(8).toByteArray()

        // Final step: decrypt with right key, then encrypt with left key (Algorithm 3)
        val decrypted = emvEngines.encryptionEngine.decrypt(
            algorithm = algorithm,
            decryptionEngineParameters = SymmetricDecryptionEngineParameters(
                data = result,
                key = rightKey,
                mode = CipherMode.CBC
            )
        ).take(8).toByteArray()

        val finalResult = emvEngines.encryptionEngine.encrypt(
            algorithm = algorithm,
            encryptionEngineParameters = SymmetricEncryptionEngineParameters(
                data = decrypted,
                key = leftKey,
                mode = CipherMode.CBC
            )
        )

        // Return the requested number of bytes
        return finalResult.sliceArray(0 until length)
    }
}