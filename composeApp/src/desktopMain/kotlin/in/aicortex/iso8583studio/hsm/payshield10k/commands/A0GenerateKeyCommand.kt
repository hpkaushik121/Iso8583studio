package `in`.aicortex.iso8583studio.hsm.payshield10k.commands
import ai.cortex.core.IsoUtil
import `in`.aicortex.iso8583studio.data.EncrDecrHandler
import `in`.aicortex.iso8583studio.data.model.CipherMode
import `in`.aicortex.iso8583studio.data.model.CipherType
import ai.cortex.core.IsoUtil.bytesToHexString
import ai.cortex.core.IsoUtil.hexStringToBytes
import ai.cortex.core.types.CryptoAlgorithm
import ai.cortex.core.types.OperationType
import `in`.aicortex.iso8583studio.hsm.payshield10k.PayShield10KCommandProcessor
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.A0Request
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.HsmCommandResult
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.LmkPair
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.LmkStorage
import java.security.SecureRandom

import `in`.aicortex.iso8583studio.hsm.payshield10k.PayShield10KFeatures
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.*
import io.cryptocalc.crypto.engines.encryption.EMVEngines
import io.cryptocalc.crypto.engines.encryption.models.EncryptionEngineParameters
import io.cryptocalc.crypto.engines.encryption.models.SymmetricDecryptionEngineParameters
import io.cryptocalc.crypto.engines.encryption.models.SymmetricEncryptionEngineParameters
import io.cryptocalc.emv.calculators.emv41.EMVCalculatorInput
import io.cryptocalc.emv.calculators.emv41.Emv41CryptoCalculator

/**
 * A0 - Generate a Key Command Implementation
 *
 * PayShield 10K Host Command: Generate a random key and return it encrypted under
 * the Local Master Key (LMK), and optionally also encrypted under a Zone Master Key
 * (ZMK), Terminal Master Key (TMK), or as a TR-31 key block.
 *
 * COMMAND FORMAT:
 * ================
 * Request Message:
 * - Message Header (variable)
 * - Command Code: "A0"
 * - Mode: 1 N (determines key output options)
 *     0 = Generate key, encrypt under LMK only
 *     1 = Generate key, encrypt under LMK and ZMK
 *     A = Derive IKEY/IPEK from BDK
 *     B = Derive IKEY/IPEK from BDK and export under ZMK
 * - Key Type: 3 H (defines key type and LMK encryption pair)
 * - Key Scheme (LMK): 1 A (encryption scheme under LMK)
 * - [Mode 1] Key Scheme (ZMK): 1 A, then ZMK (variable)
 * - [Mode A/B] Derive Key Mode: 1 A, DUKPT Master Key Type: 2 N,
 *              BDK (variable), KSN (variable)
 * - [Optional Delimiter and LMK ID]
 *
 * Response Message:
 * - Message Header (variable)
 * - Response Code: "A1"
 * - Error Code: 2 N ("00" for success)
 * - Key under LMK: 16H/32H/48H or 'S' + Key Block
 * - [Key under ZMK if Mode=1 or B]
 * - Key Check Value: 6 H or 16 H
 */
class A0GenerateKeyCommand(private val hsm: PayShield10KFeatures) {

    companion object {
        const val COMMAND_CODE = "A0"
        const val RESPONSE_CODE = "A1"

        val KEY_TYPE_LMK_MAP = mapOf(
            "000" to KeyTypeLmkInfo(14, 0, "ZMK"),
            "001" to KeyTypeLmkInfo(14, 0, "ZPK"),
            "002" to KeyTypeLmkInfo(14, 0, "PVK/TPK/TMK"),
            "003" to KeyTypeLmkInfo(14, 0, "TAK"),
            "008" to KeyTypeLmkInfo(14, 0, "ZAK"),
            "009" to KeyTypeLmkInfo(14, 0, "BDK"),
            "109" to KeyTypeLmkInfo(14, 1, "MK-AC"),
            "209" to KeyTypeLmkInfo(14, 2, "MK-SMI"),
            "309" to KeyTypeLmkInfo(14, 3, "MK-SMC"),
            "409" to KeyTypeLmkInfo(14, 4, "MK-DAC"),
            "509" to KeyTypeLmkInfo(14, 5, "MK-DN"),
            "609" to KeyTypeLmkInfo(14, 6, "BDK-2"),
            "709" to KeyTypeLmkInfo(14, 7, "MK-CVC3"),
            "809" to KeyTypeLmkInfo(14, 8, "BDK-3"),
            "909" to KeyTypeLmkInfo(14, 9, "BDK-4"),
            "00A" to KeyTypeLmkInfo(14, 0, "ZEK"),
            "00B" to KeyTypeLmkInfo(14, 0, "DEK"),
            "402" to KeyTypeLmkInfo(14, 4, "CVK"),
            "70D" to KeyTypeLmkInfo(14, 0, "TPK-PCI"),
            "80D" to KeyTypeLmkInfo(14, 0, "TMK-PCI"),
            "90D" to KeyTypeLmkInfo(14, 0, "TKR"),
            "302" to KeyTypeLmkInfo(14, 0, "IKEY")
        )

        val LMK_VARIANTS = mapOf(
            0 to 0x00,
            1 to 0xA6,
            2 to 0x5A,
            3 to 0x6A,
            4 to 0xDE,
            5 to 0x2B,
            6 to 0x50,
            7 to 0x74,
            8 to 0x9C,
            9 to 0xFA
        )

        /**
         * Maps DUKPT Master Key Type (from A0 Mode A/B) to the LMK variant
         * used for encrypting that BDK type under LMK pair 28-29.
         */
        val DUKPT_KEY_TYPE_VARIANT = mapOf(
            "00" to 0,   // Type 1 BDK  → LMK 28-29 variant 0
            "01" to 1,   // MK-AC       → LMK 28-29 variant 1
            "02" to 6,   // Type 2 BDK  → LMK 28-29 variant 6
            "03" to 3,   // MK-SMC      → LMK 28-29 variant 3
            "04" to 4,   // MK-DAC      → LMK 28-29 variant 4
            "05" to 5,   // MK-DN       → LMK 28-29 variant 5
        )
    }

    private val secureRandom = SecureRandom()

    /**
     * Execute the A0 command.
     * @param commandData The data portion of the command (after header + "A0")
     * @param parentLmkId The LMK ID resolved by the outer command parser (from %XX delimiter or port mapping)
     */
    suspend fun execute(commandData: String, parentLmkId: String = "00"): HsmCommandResult {
        return try {
            val request = parseRequest(commandData, parentLmkId)

            val keyTypeInfo = KEY_TYPE_LMK_MAP[request.keyType]
                ?: return HsmCommandResult.Error(
                    HsmErrorCodes.INVALID_KEY_TYPE_CODE,
                    "Invalid key type code: ${request.keyType}"
                )

            val keyLength = getKeyLengthFromScheme(request.keyScheme)
            if (keyLength == 0) {
                return HsmCommandResult.Error(
                    HsmErrorCodes.INVALID_KEY_SCHEME,
                    "Invalid key scheme: ${request.keyScheme}"
                )
            }

            val lmk = hsm.lmkStorage.getLmk(request.lmkId)
                ?: return HsmCommandResult.Error(
                    HsmErrorCodes.LMK_NOT_LOADED,
                    "No LMK loaded for identifier: ${request.lmkId}"
                )

            val lmkPairNumber = keyTypeInfo.lmkPairNumber
            val lmkPair = lmk.getPair(lmkPairNumber)
                ?: return HsmCommandResult.Error(
                    HsmErrorCodes.LMK_NOT_LOADED,
                    "LMK pair $lmkPairNumber not available"
                )
            hsm.hsmLogsListener.onFormattedRequest(
                """
                    mode: ${request.mode}
                    keyType: ${request.keyType} (${keyTypeInfo.description})
                    keyScheme: ${request.keyScheme} (${getKeyDescriptionFromScheme(request.keyScheme)})
                    keyScheme (tmk/zmk): ${request.keySchemeZmk} (${getKeyDescriptionFromScheme(request.keySchemeZmk ?: '0')})
                    lmk: ${IsoUtil.bytesToHex(lmkPair.getCombinedKey())}
                    tmk/zmk: ${request.zmk}
                    bdk: ${request.bdk ?: "N/A"}
                    ksn: ${request.ksn ?: "N/A"}
                    lmkId: ${request.lmkId}  
                """.trimIndent()
            )
            hsm.hsmLogsListener.log(
                """
                    mode: ${request.mode}
                    keyType: ${request.keyType} (${keyTypeInfo.description})
                    keyScheme: ${request.keyScheme} (${getKeyDescriptionFromScheme(request.keyScheme)})
                    keyScheme (tmk/zmk): ${request.keySchemeZmk} (${getKeyDescriptionFromScheme(request.keySchemeZmk ?: '0')})
                    lmk: ${IsoUtil.bytesToHex(lmkPair.getCombinedKey())}
                    tmk/zmk: ${request.zmk}
                    bdk: ${request.bdk ?: "N/A"}
                    ksn: ${request.ksn ?: "N/A"}
                    lmkId: ${request.lmkId}  
                """.trimIndent()
            )

            val result = when (request.mode) {
                '0' -> {
                    val clearKey = generateRandomKey(keyLength)
                    val variantLmk = applyLmkVariant(lmkPair.getCombinedKey(), keyTypeInfo.variant)
                    val keyUnderLmk = encryptKey(clearKey, variantLmk, request.keyScheme)
                    val kcv = hsm.calculateKeyCheckValue(clearKey)
                    buildMode0Response(keyUnderLmk, kcv, request, keyTypeInfo)
                }
                '1' -> {
                    val clearKey = generateRandomKey(keyLength)
                    val variantLmk = applyLmkVariant(lmkPair.getCombinedKey(), keyTypeInfo.variant)
                    val keyUnderLmk = encryptKey(clearKey, variantLmk, request.keyScheme)
                    val kcv = hsm.calculateKeyCheckValue(clearKey)
                    buildMode1Response(clearKey, keyUnderLmk, kcv, request, keyTypeInfo)
                }
                'A', 'B' -> buildDeriveIkeyResponse(request, keyTypeInfo, lmk)
                else -> return HsmCommandResult.Error(
                    HsmErrorCodes.FUNCTION_NOT_PERMITTED,
                    "Invalid mode: ${request.mode}"
                )
            }
            hsm.hsmLogsListener.log("A0 completed: mode=${request.mode}, keyType=${keyTypeInfo.description}, scheme=${request.keyScheme}")

            result

        } catch (e: Exception) {
            HsmCommandResult.Error(
                HsmErrorCodes.INVALID_MESSAGE_LENGTH,
                "Parse error: ${e.message}"
            )
        }
    }

    /**
     * Parse the A0 command request.
     *
     * Format (Mode 0): Mode(1) + KeyType(3) + KeyScheme(1)
     * Format (Mode 1): Mode(1) + KeyType(3) + KeyScheme(1) + KeySchemeZmk(1) + ZMK(variable)
     * Format (Mode A): Mode(1) + KeyType(3) + KeyScheme(1) + DeriveKeyMode(1) + DukptKeyType(2) + BDK(variable) + KSN(variable)
     * Format (Mode B): Mode(1) + KeyType(3) + KeyScheme(1) + DeriveKeyMode(1) + DukptKeyType(2) + BDK(variable) + KSN(variable) + KeySchemeZmk(1) + ZMK(variable)
     */
    private fun parseRequest(data: String, parentLmkId: String): A0Request {
        var offset = 0

        val mode = data[offset]
        offset += 1

        val keyType = data.substring(offset, offset + 3).uppercase()
        offset += 3

        val keyScheme = data[offset].uppercaseChar()
        offset += 1

        var keySchemeZmk: Char? = null
        var zmk: String? = null
        var ksn: String? = null
        var deriveKeyMode: Char? = null
        var dukptMasterKeyType: String? = null
        var bdk: String? = null

        when (mode) {
            '1' -> {
                if (offset < data.length) {
                    keySchemeZmk = data[offset].uppercaseChar()
                    offset += 1

                    val (zmkValue, zmkLen) = extractKey(data, offset)
                    zmk = zmkValue
                    offset += zmkLen
                }
            }
            'A', 'B' -> {
                // Derive Key Mode (1 char): 0=Derive IKEY/IPEK from BDK
                if (offset < data.length) {
                    deriveKeyMode = data[offset]
                    offset += 1
                }

                // DUKPT Master Key Type (2 chars): 00=Type 1 BDK, 02=Type 2 BDK
                if (offset + 2 <= data.length) {
                    dukptMasterKeyType = data.substring(offset, offset + 2)
                    offset += 2
                }

                // BDK encrypted under LMK pair 28-29 (variable length based on scheme prefix)
                if (offset < data.length) {
                    val (bdkValue, bdkLen) = extractKey(data, offset)
                    bdk = bdkValue
                    offset += bdkLen
                }

                // KSN — read remaining data up to any delimiter (% or ;) as KSN
                val semiIdx = data.indexOf(';', offset)
                val pctIdx = data.indexOf('%', offset)
                val delimIdx = when {
                    semiIdx in offset..data.lastIndex && pctIdx in offset..data.lastIndex -> minOf(semiIdx, pctIdx)
                    semiIdx in offset..data.lastIndex -> semiIdx
                    pctIdx in offset..data.lastIndex -> pctIdx
                    else -> -1
                }
                val ksnEnd = if (delimIdx >= offset) delimIdx else data.length
                if (offset < ksnEnd) {
                    ksn = data.substring(offset, ksnEnd)
                    offset = ksnEnd
                }

                // Skip ';' delimiter separating KSN from ZMK section in Mode B
                if (offset < data.length && data[offset] == ';') {
                    offset += 1
                }

                // Mode B also exports under ZMK/TMK
                if (mode == 'B' && offset < data.length && data[offset] != '%') {
                    keySchemeZmk = data[offset].uppercaseChar()
                    offset += 1

                    val (zmkValue, zmkLen) = extractKey(data, offset)
                    zmk = zmkValue
                    offset += zmkLen
                }
            }
        }

        // LMK ID: use parent-provided lmkId (from outer %XX parsing) as default.
        // Only override if this data segment itself contains a %XX delimiter.
        var lmkId = parentLmkId
        if (offset < data.length && data[offset] == '%') {
            offset += 1
            if (offset + 2 <= data.length) {
                lmkId = data.substring(offset, offset + 2)
            }
        }

        return A0Request(
            mode = mode,
            keyType = keyType,
            keyScheme = keyScheme,
            keySchemeZmk = keySchemeZmk,
            zmk = zmk,
            ksn = ksn,
            lmkId = lmkId,
            deriveKeyMode = deriveKeyMode,
            dukptMasterKeyType = dukptMasterKeyType,
            bdk = bdk
        )
    }

    /**
     * Extract key from data with scheme prefix handling.
     */
    private fun extractKey(data: String, offset: Int): Pair<String, Int> {
        if (offset >= data.length) return Pair("", 0)

        return when (val prefix = data[offset]) {
            'U', 'X' -> {
                val len = minOf(33, data.length - offset)
                Pair(data.substring(offset, offset + len), len)
            }
            'T', 'Y' -> {
                val len = minOf(49, data.length - offset)
                Pair(data.substring(offset, offset + len), len)
            }
            'S' -> {
                var endIdx = offset + 1
                while (endIdx < data.length && data[endIdx] != '%' && data[endIdx] != ';') {
                    endIdx++
                }
                Pair(data.substring(offset, endIdx), endIdx - offset)
            }
            else -> {
                val len = minOf(32, data.length - offset)
                Pair(data.substring(offset, offset + len), len)
            }
        }
    }

    private fun generateRandomKey(length: Int): ByteArray {
        var key: ByteArray

        do {
            key = ByteArray(length)
            secureRandom.nextBytes(key)
            key = hsm.applyOddParity(key)
        } while (isWeakOrSemiWeakKey(key))

        return key
    }

    private fun isWeakOrSemiWeakKey(key: ByteArray): Boolean {
        val weakKeys = listOf(
            "0101010101010101", "FEFEFEFEFEFEFEFE",
            "E0E0E0E0F1F1F1F1", "1F1F1F1F0E0E0E0E"
        )

        val semiWeakKeys = listOf(
            "01FE01FE01FE01FE", "FE01FE01FE01FE01",
            "1FE01FE00EF10EF1", "E01FE01FF10EF10E",
            "01E001E001F101F1", "E001E001F101F101",
            "1FFE1FFE0EFE0EFE", "FE1FFE1FFE0EFE0E",
            "011F011F010E010E", "1F011F010E010E01",
            "E0FEE0FEF1FEF1FE", "FEE0FEE0FEF1FEF1"
        )

        for (i in key.indices step 8) {
            if (i + 8 <= key.size) {
                val block = hsm.bytesToHex(key.sliceArray(i until i + 8))
                if (weakKeys.contains(block) || semiWeakKeys.contains(block)) {
                    return true
                }
            }
        }

        return false
    }

    private fun applyLmkVariant(lmkKey: ByteArray, variant: Int): ByteArray {
        if (variant == 0) return lmkKey

        val variantMask = LMK_VARIANTS[variant] ?: return lmkKey
        val variantedKey = lmkKey.copyOf()
        variantedKey[0] = (variantedKey[0].toInt() xor variantMask).toByte()

        return variantedKey
    }

    private suspend fun encryptKey(clearKey: ByteArray, encryptingKey: ByteArray, scheme: Char): String {
        val encrypted = performTdesEncrypt(clearKey, encryptingKey)

        return when (scheme) {
            'Z' -> hsm.bytesToHex(encrypted)
            'U' -> "U" + hsm.bytesToHex(encrypted)
            'T' -> "T" + hsm.bytesToHex(encrypted)
            'X' -> "X" + hsm.bytesToHex(encrypted)
            'Y' -> "Y" + hsm.bytesToHex(encrypted)
            'S' -> buildThalesKeyBlock(clearKey, encryptingKey)
            else -> hsm.bytesToHex(encrypted)
        }
    }

    private suspend fun performTdesEncrypt(data: ByteArray, key: ByteArray): ByteArray {
        return try {
            hsm.hsmLogsListener.log("=======================================================================")
            hsm.hsmLogsListener.log("Plain=${IsoUtil.bytesToHex(data)}")
            val calculator = EMVEngines()
            val data =  calculator.encryptionEngine.encrypt(
                algorithm = CryptoAlgorithm.TDES,
                encryptionEngineParameters = SymmetricEncryptionEngineParameters(
                    data = data,
                    key = key,
                    mode = ai.cortex.core.types.CipherMode.ECB
                )
            )
            hsm.hsmLogsListener.log("encrypted=${IsoUtil.bytesToHex(data)}, Key=${IsoUtil.bytesToHex(key)}")
            hsm.hsmLogsListener.log("=======================================================================")
            return data
        } catch (e: Exception) {
            data
        }
    }

    private suspend fun performTdesDecrypt(data: ByteArray, key: ByteArray): ByteArray {
        return try {
            hsm.hsmLogsListener.log("encrypted ${IsoUtil.bytesToHex(data)} data, Key=${IsoUtil.bytesToHex(key)}")
            val calculator = EMVEngines()
            val data =  calculator.encryptionEngine.decrypt(
                algorithm = CryptoAlgorithm.TDES,
                decryptionEngineParameters = SymmetricDecryptionEngineParameters(
                    data = data,
                    key = key,
                    mode = ai.cortex.core.types.CipherMode.ECB
                )
            )
            hsm.hsmLogsListener.log("decrypted ${IsoUtil.bytesToHex(data)} data, Key=${IsoUtil.bytesToHex(key)}")
            data
        } catch (e: Exception) {
            data
        }
    }

    private suspend fun buildThalesKeyBlock(clearKey: ByteArray, lmk: ByteArray): String {
        val keyBits = clearKey.size * 8
        val header = String.format("00%03dP0TN00N0000", keyBits)
        val encryptedKey = performTdesEncrypt(clearKey, lmk)
        val mac = hsm.calculateKeyCheckValue(clearKey)
        return "S$header${hsm.bytesToHex(encryptedKey)}$mac"
    }

    private suspend fun decryptZmkFromLmk(encryptedZmk: String, lmkId: String): ByteArray {
        val lmk = hsm.lmkStorage.getLmk(lmkId) ?: throw IllegalStateException("LMK not found")
        val zmkPair = lmk.getPair(14) ?: throw IllegalStateException("LMK pair 0 not found")
        val zmkHex = if (encryptedZmk[0].uppercaseChar() in "UXTYSR") {
            encryptedZmk.substring(1)
        } else {
            encryptedZmk
        }
        val zmkBytes = hsm.hexToBytes(zmkHex)
        return performTdesDecrypt(zmkBytes, zmkPair.getCombinedKey())
    }

    private fun buildMode0Response(
        keyUnderLmk: String,
        kcv: String,
        request: A0Request,
        keyTypeInfo: KeyTypeLmkInfo
    ): HsmCommandResult {
        val responseData = "$keyUnderLmk$kcv"
        hsm.hsmLogsListener.onFormattedResponse(
            """
                    type: ${KEY_TYPE_LMK_MAP[request.keyType]}
                    key under LMK: $keyUnderLmk
                    kcv: $kcv  
                """.trimIndent()
        )
        return HsmCommandResult.Success(
            response = responseData,
            data = mapOf(
                "keyUnderLmk" to keyUnderLmk,
                "kcv" to kcv,
                "keyType" to keyTypeInfo.description,
                "keyScheme" to request.keyScheme.toString()
            )
        )
    }

    private suspend fun buildMode1Response(
        clearKey: ByteArray,
        keyUnderLmk: String,
        kcv: String,
        request: A0Request,
        keyTypeInfo: KeyTypeLmkInfo
    ): HsmCommandResult {
        val clearZmk = decryptZmkFromLmk(request.zmk!!, request.lmkId)
        val keyUnderZmk = encryptKey(clearKey, clearZmk, request.keySchemeZmk!!)

        val responseData = "$keyUnderLmk$keyUnderZmk$kcv"
        hsm.hsmLogsListener.onFormattedResponse(
            """
                    type: ${KEY_TYPE_LMK_MAP[request.keyType]}
                    clear ZMK: ${IsoUtil.bytesToHex(clearZmk)}
                    key under LMK: $keyUnderLmk
                    key under ZMK: $keyUnderZmk
                    kcv: $kcv  
                """.trimIndent())
        hsm.hsmLogsListener.log(
            """
                    type: ${KEY_TYPE_LMK_MAP[request.keyType]}
                    clear ZMK: ${IsoUtil.bytesToHex(clearZmk)}
                    key under LMK: $keyUnderLmk
                    key under ZMK: $keyUnderZmk
                    kcv: $kcv  
                """.trimIndent())
        return HsmCommandResult.Success(
            response = responseData,
            data = mapOf(
                "keyUnderLmk" to keyUnderLmk,
                "keyUnderZmk" to keyUnderZmk,
                "kcv" to kcv,
                "keyType" to keyTypeInfo.description
            )
        )
    }

    /**
     * Mode A/B: Derive IKEY/IPEK from BDK using KSN, then encrypt under LMK.
     *
     * Steps:
     * 1. Decrypt BDK under LMK pair 28-29 (variant based on DUKPT Master Key Type)
     * 2. Derive IKEY from BDK + KSN using ANSI X9.24 algorithm
     * 3. Encrypt IKEY under LMK (using the pair for the output key type, e.g. 302→LMK 14-15)
     * 4. Calculate KCV
     * 5. [Mode B] Also encrypt IKEY under ZMK
     */
    private suspend fun buildDeriveIkeyResponse(
        request: A0Request,
        keyTypeInfo: KeyTypeLmkInfo,
        lmk: LmkSet
    ): HsmCommandResult {
        val bdkStr = request.bdk
            ?: return HsmCommandResult.Error(HsmErrorCodes.INVALID_INPUT_DATA, "BDK is required for mode A/B")
        val ksnStr = request.ksn
            ?: return HsmCommandResult.Error(HsmErrorCodes.INVALID_INPUT_DATA, "KSN is required for mode A/B")

        // Determine LMK variant for BDK decryption based on DUKPT Master Key Type
        val dukptType = request.dukptMasterKeyType ?: "00"
        val bdkVariant = DUKPT_KEY_TYPE_VARIANT[dukptType] ?: 0
        val bdkLmkPair = lmk.getPair(14)
            ?: return HsmCommandResult.Error(HsmErrorCodes.LMK_NOT_LOADED, "LMK pair 28-29 not available")

        // Step 1: Decrypt BDK under LMK pair 28-29 with appropriate variant
        val bdkHex = if (bdkStr[0].uppercaseChar() in "UXTYSR") bdkStr.substring(1) else bdkStr
        val encryptedBdk = hsm.hexToBytes(bdkHex)
        val variantBdkLmk = applyLmkVariant(bdkLmkPair.getCombinedKey(), bdkVariant)
        val clearBdk = performTdesDecrypt(encryptedBdk, variantBdkLmk)

        hsm.hsmLogsListener.log("[A0 Mode ${request.mode}] Step 1: Decrypted BDK under LMK 28-29 variant $bdkVariant")
        hsm.hsmLogsListener.log("  Clear BDK: ${IsoUtil.bytesToHex(clearBdk)}")

        // Step 2: Derive IKEY/IPEK from BDK + KSN
        val normalizedKsn = if (ksnStr.length % 2 != 0) "0$ksnStr" else ksnStr
        hsm.hsmLogsListener.log("  Normalized KSN: $normalizedKsn")
        val counterBits = if (request.keyType == "009") 21
            else PayShield10KCommandProcessor.parseKsnDescriptorCounterBits(request.keyType)
        val clearIkey = PayShield10KCommandProcessor.deriveInitialKey(clearBdk, normalizedKsn, counterBits)

        hsm.hsmLogsListener.log("[A0 Mode ${request.mode}] Step 2: Derived IKEY from BDK + KSN")
        hsm.hsmLogsListener.log("  KSN: $ksnStr")
        hsm.hsmLogsListener.log("  Clear IKEY: ${IsoUtil.bytesToHex(clearIkey)}")

        // Step 3: Encrypt IKEY under LMK for the output key type
        val outputLmkPair = lmk.getPair(keyTypeInfo.lmkPairNumber)
            ?: return HsmCommandResult.Error(HsmErrorCodes.LMK_NOT_LOADED, "LMK pair ${keyTypeInfo.lmkPairNumber} not available")
        val variantOutputLmk = applyLmkVariant(outputLmkPair.getCombinedKey(), keyTypeInfo.variant)
        val ikeyUnderLmk = encryptKey(clearIkey, variantOutputLmk, request.keyScheme)

        // Step 4: KCV
        val kcv = hsm.calculateKeyCheckValue(clearIkey)

        hsm.hsmLogsListener.log("[A0 Mode ${request.mode}] Step 3: Encrypted IKEY under LMK ${keyTypeInfo.lmkPairNumber}-${keyTypeInfo.lmkPairNumber + 1}")
        hsm.hsmLogsListener.log("  IKEY under LMK: $ikeyUnderLmk")
        hsm.hsmLogsListener.log("  KCV: $kcv")

        // Step 5 [Mode B]: Also export under ZMK
        val responseData = if (request.mode == 'B' && request.zmk != null) {
            val clearZmk = decryptZmkFromLmk(request.zmk, request.lmkId)
            val ikeyUnderZmk = encryptKey(clearIkey, clearZmk, request.keySchemeZmk ?: 'U')
            hsm.hsmLogsListener.onFormattedResponse(
                """
                    type: ${keyTypeInfo.description}
                    IKEY under LMK: $ikeyUnderLmk
                    IKEY under ZMK: $ikeyUnderZmk
                    KCV: $kcv  
                """.trimIndent())
            "$ikeyUnderLmk$ikeyUnderZmk$kcv"
        } else {
            hsm.hsmLogsListener.onFormattedResponse(
                """
                    type: ${keyTypeInfo.description}
                    IKEY under LMK: $ikeyUnderLmk
                    KCV: $kcv  
                """.trimIndent())
            "$ikeyUnderLmk$kcv"
        }

        return HsmCommandResult.Success(
            response = responseData,
            data = mapOf(
                "keyUnderLmk" to ikeyUnderLmk,
                "kcv" to kcv,
                "keyType" to keyTypeInfo.description,
                "ksn" to ksnStr
            )
        )
    }

    private fun getKeyLengthFromScheme(scheme: Char) = when (scheme.uppercaseChar()) {
        'Z' -> 8; 'U' -> 16; 'T' -> 24; 'X' -> 16; 'Y' -> 24; 'S' -> 16; 'R' -> 16; else -> 0
    }

    private fun getKeyDescriptionFromScheme(scheme: Char) = when (scheme.uppercaseChar()) {
        'Z' -> "Single-length"; 'U' -> "Double-length"; 'T' -> "Triple-length"
        'X' -> "X9.17 double"; 'Y' -> "X9.17 triple"
        'S' -> "Key Block"; 'R' -> "TR-31 Key Block"; else -> ""
    }
}

data class KeyTypeLmkInfo(
    val lmkPairNumber: Int,
    val variant: Int,
    val description: String
)
