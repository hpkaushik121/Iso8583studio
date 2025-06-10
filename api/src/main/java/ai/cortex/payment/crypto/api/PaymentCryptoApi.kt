package ai.cortex.payment.crypto.api

import ai.cortex.core.crypto.MacCalculator
import ai.cortex.core.crypto.data.ArpcMethod
import ai.cortex.core.crypto.data.CryptogramType
import ai.cortex.core.crypto.data.KcvType
import ai.cortex.core.crypto.data.KeyParity
import ai.cortex.core.crypto.data.PaddingMethod
import ai.cortex.core.crypto.data.SessionKeyDerivationMethod
import ai.cortex.core.crypto.data.UdkDerivationOption
import ai.cortex.core.crypto.getSymmetricCipher
import ai.cortex.payment.crypto.EmvProcessor
import ai.cortex.core.crypto.hexToByteArray
import ai.cortex.core.crypto.toHexString
import ai.cortex.payment.crypto.api.data.SessionKeyResult
import ai.cortex.payment.crypto.api.data.UdkResult

object PaymentCryptoApi {

    private val emvProcessor = EmvProcessor()

    // ========== UDK DERIVATION (UDK Tab) ==========

    /**
     * Derive UDK with exact EFTLab options
     *
     * Example from EFTLab:
     * MDK: 0123456789ABCDEF0123456789ABCDEF
     * PAN: 43219876543210987
     * PAN sequence nr.: 00
     * Derivation option: Option A
     * Key parity: Right odd
     * Result: C8B507136D921FD05864C81F79F2D30B
     */
    fun deriveUdk(
        mdkHex: String,
        pan: String,
        panSequenceNumber: String = "00",
        derivationOption: UdkDerivationOption = UdkDerivationOption.OPTION_A,
        keyParity: KeyParity = KeyParity.RIGHT_ODD
    ):UdkResult {
        val mdk = mdkHex.hexToByteArray()
        val udkBytes = emvProcessor.deriveUdk(mdk, pan, panSequenceNumber, derivationOption, keyParity)

        // Fix: Handle 16-byte UDK for KCV calculation
        val kcvKey = if (udkBytes.size == 16) {
            udkBytes + udkBytes.copyOfRange(0, 8) // 3DES EDE format
        } else {
            udkBytes
        }
        val kcvBytes = emvProcessor.calculateKcv(kcvKey)

        return UdkResult(
            udk = udkBytes.toHexString().uppercase(),
            kcv = kcvBytes.toHexString().uppercase()
        )
    }

    // ========== SESSION KEY DERIVATION ==========

    /**
     * Common Session Key (Common Session key tab)
     *
     * Example from EFTLab:
     * Master key: C8B507136D921FD05864C81F79F2D30B
     * ATC: 0001
     * Key parity: (none)
     * Result: D920B6730B9267079220F8491F2FCD68
     */
    fun deriveCommonSessionKey(
        masterKeyHex: String,
        atcHex: String,
        keyParity: KeyParity = KeyParity.NONE
    ): SessionKeyResult {
        val masterKey = masterKeyHex.hexToByteArray()
        val atc = atcHex.hexToByteArray()

        val sessionKeyBytes = emvProcessor.deriveCommonSessionKey(masterKey, atc, keyParity)
        val kcvBytes = emvProcessor.calculateKcv(sessionKeyBytes)

        return SessionKeyResult(
            sessionKey = sessionKeyBytes.toHexString().uppercase(),
            kcv = kcvBytes.toHexString().uppercase()
        )
    }

    /**
     * EMV Session Key with IV/Branch/Height (Session key tab)
     *
     * Example from EFTLab:
     * Master key: 0123456789ABCDEF0123456789ABCDEF
     * Initial vector: 00000000000000000000000000000000
     * Branch factor: 4
     * Height: 8
     * ATC: 0001
     * Key parity: Right odd
     * Result: 022551C4FDF76E45988089BA31DC077C
     */
//    fun deriveEmvSessionKey(
//        masterKeyHex: String,
//        initialVectorHex: String = "00000000000000000000000000000000",
//        branchFactor: Int = 4,
//        height: Int = 8,
//        atcHex: String,
//        keyParity: KeyParity = KeyParity.RIGHT_ODD
//    ): SessionKeyResult {
//        val masterKey = masterKeyHex.hexToByteArray()
//        val initialVector = initialVectorHex.hexToByteArray()
//        val atc = atcHex.hexToByteArray()
//
//        val sessionKeyBytes = emvProcessor.deriveSessionKey(
//            masterKey, initialVector, branchFactor, height, atc, keyParity
//        )
//        val kcvBytes = emvProcessor.calculateKcv(sessionKeyBytes)
//
//        return SessionKeyResult(
//            sessionKey = sessionKeyBytes.toHexString().uppercase(),
//            kcv = kcvBytes.toHexString().uppercase()
//        )
//    }

    /**
     * MasterCard Session Key (MasterCard Session key tab)
     *
     * Example from EFTLab:
     * UDK: C86ED652D5C2CBA21FC175191A5DCBCD
     * ATC: 0001
     * Unpredictable nr.: 30901B6A
     * Result: 45C44343B64A58B3BF8046F75D943BEA
     */
//    fun deriveMasterCardSessionKey(
//        udkHex: String,
//        atcHex: String,
//        unpredictableNumberHex: String,
//        keyParity: KeyParity = KeyParity.NONE
//    ): SessionKeyResult {
//        val udk = udkHex.hexToByteArray()
//        val atc = atcHex.hexToByteArray()
//        val unpredictableNumber = unpredictableNumberHex.hexToByteArray()
//
//        val sessionKeyBytes = emvProcessor.deriveMasterCardSessionKey(
//            udk, atc, unpredictableNumber, keyParity
//        )
//        val kcvBytes = emvProcessor.calculateKcv(sessionKeyBytes)
//
//        return SessionKeyResult(
//            sessionKey = sessionKeyBytes.toHexString().uppercase(),
//            kcv = kcvBytes.toHexString().uppercase()
//        )
//    }

    // ========== APPLICATION CRYPTOGRAM ==========

    /**
     * Generate Application Cryptogram (AAC/ARQC/TC tab)
     *
     * Example from EFTLab:
     * Session key: 022551C4FDF76E45988089BA31DC077C
     * Terminal data: 0000000010000000000000000710000000000007101302050030901B6A
     * ICC data: 3C00000103A4A082
     * Padding method: Method 1 (ISO-9797)
     * Result: 92791D36B5CC31B5
     */
//    fun generateApplicationCryptogram(
//        sessionKeyHex: String,
//        terminalDataHex: String,
//        iccDataHex: String,
//        paddingMethod: PaddingMethod = PaddingMethod.METHOD_1_ISO_9797,
//        cryptogramType: CryptogramType = CryptogramType.ARQC
//    ): String {
//        val sessionKey = sessionKeyHex.hexToByteArray()
//        val terminalData = terminalDataHex.hexToByteArray()
//        val iccData = iccDataHex.hexToByteArray()
//
//        val cryptogramBytes = emvProcessor.generateApplicationCryptogram(
//            sessionKey, terminalData, iccData, paddingMethod, cryptogramType
//        )
//
//        return cryptogramBytes.toHexString().uppercase()
//    }

    // ========== ARPC GENERATION ==========

    /**
     * Generate ARPC (ARPC tab)
     *
     * Example from EFTLab:
     * ARPC Method: 1
     * Response Code: Y3
     * Session key: 022551C4FDF76E45988089BA31DC077C
     * Transaction Cryptogram: 92791D36B5CC31B5
     * Result: E4E0BD6721957E2B
     */
//    fun generateArpc(
//        sessionKeyHex: String,
//        transactionCryptogramHex: String,
//        responseCode: String,
//        arpcMethod: ArpcMethod = ArpcMethod.METHOD_1
//    ): String {
//        val sessionKey = sessionKeyHex.hexToByteArray()
//        val transactionCryptogram = transactionCryptogramHex.hexToByteArray()
//
//        val arpcBytes = emvProcessor.generateArpc(
//            sessionKey, transactionCryptogram, responseCode, arpcMethod
//        )
//
//        return arpcBytes.toHexString().uppercase()
//    }

    // ========== VISA HCE FUNCTIONS ==========

    /**
     * VISA HCE LUK Key derivation
     */
//    fun deriveVisaHceLukKey(
//        udkHex: String,
//        currentYear: Int = 0,
//        currentHours: Int,
//        hourlyCounterHex: String
//    ): String {
//        val udk = udkHex.hexToByteArray()
//        val hourlyCounter = hourlyCounterHex.hexToByteArray()
//
//        val lukKeyBytes = emvProcessor.deriveVisaHceLukKey(
//            udk, currentYear, currentHours, hourlyCounter
//        )
//
//        return lukKeyBytes.toHexString().uppercase()
//    }

    /**
     * VISA HCE MSD Verification Value
     */
//    fun deriveVisaHceMsdVerificationValue(
//        lukKeyHex: String,
//        atcHex: String,
//        deviceTypeHex: String
//    ): String {
//        val lukKey = lukKeyHex.hexToByteArray()
//        val atc = atcHex.hexToByteArray()
//        val deviceType = deviceTypeHex.hexToByteArray()
//
//        val verificationValueBytes = emvProcessor.deriveVisaHceMsdVerificationValue(
//            lukKey, atc, deviceType
//        )
//
//        // Return as 3-digit string
//        return verificationValueBytes.joinToString("") { (it.toInt() and 0xFF).toString() }
//    }
}
