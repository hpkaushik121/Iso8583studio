package io.cryptocalc.crypto.engines.applicationCryptogram

import ai.cortex.core.types.AlgorithmType
import ai.cortex.core.types.CryptoAlgorithm
import io.cryptocalc.crypto.engines.Engine
import io.cryptocalc.crypto.engines.encryption.EMVEngines
import io.cryptocalc.emv.calculators.emv41.CryptogramInput

interface AcEngine: Engine {

    /**
     * First and second AC Generation using 8-byte block cipher
     * Triple DES with ISO/IEC 9797-1 Algorithm 3. Same process for:
     *
     * - Authorisation Request Cryptogram (ARQC)
     * - Transaction Cryptogram (TC)
     * - Application Authentication Cryptogram (AAC)
     *
     * @param skAc Binary ICC Session Key for AC. Must be a valid 16-byte DES key.
     * @param data Binary data from tags used in AC generation. Minimum recommended
     *             tags by EMV Book2 Application Cryptogram Data Selection:
     *             - 9F02 (6 bytes): Amount, Authorized
     *             - 9F03 (6 bytes): Amount, Other
     *             - 9F1A (2 bytes): Terminal Country Code
     *             - 95   (5 bytes): Terminal Verification Results
     *             - 5F2A (2 bytes): Transaction Currency Code
     *             - 9A   (3 bytes): Transaction Date
     *             - 9C   (1 byte):  Transaction Type
     *             - 9F37 (4 bytes): Unpredictable Number
     *             - 82   (2 bytes): Application Interchange Profile
     *             - 9F36 (2 bytes): Application Transaction Counter
     * @param paddingType Padding method applied to the data (default EMV)
     * @param length Desired length of AC [4 <= length <= 8] (default 8 bytes)
     * @return length-byte cryptogram (ARQC, TC, AAC)
     * @throws IllegalArgumentException Session Key must be a double length DES key
     * @throws IllegalArgumentException Invalid padding type
     * @throws IllegalArgumentException Length must be between 4 and 8 bytes
     */
    suspend fun generateAC(
        algorithm: CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>,
        cryptogramInput: CryptogramInput
    )
}