package `in`.aicortex.iso8583studio.hsm.payshield10k.commands
import `in`.aicortex.iso8583studio.data.EncrDecrHandler
import `in`.aicortex.iso8583studio.data.model.CipherMode
import `in`.aicortex.iso8583studio.data.model.CipherType
import ai.cortex.core.IsoUtil.bytesToHexString
import ai.cortex.core.IsoUtil.hexStringToBytes
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.A0Request
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.HsmCommandResult
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.LmkPair
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.LmkStorage
import java.security.SecureRandom

import `in`.aicortex.iso8583studio.hsm.payshield10k.PayShield10KFeatures
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

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
 * - Key Type: 3 H (defines key type and LMK encryption pair)
 * - Key Scheme (LMK): 1 A (encryption scheme under LMK)
 * - [Conditional fields based on Mode]
 * - [Optional Delimiter and LMK ID]
 *
 * Response Message:
 * - Message Header (variable)
 * - Response Code: "A1"
 * - Error Code: 2 N ("00" for success)
 * - Key under LMK: 16H/32H/48H or 'S' + Key Block
 * - [Key under ZMK if Mode=1]
 * - Key Check Value: 6 H or 16 H
 */
class A0GenerateKeyCommand(private val hsm: PayShield10KFeatures) {

    companion object {
        const val COMMAND_CODE = "A0"
        const val RESPONSE_CODE = "A1"

        // Key Type to LMK Pair mapping (pairNumber, variant)
        // Based on PayShield Host Programmer's Manual Key Type Table
        val KEY_TYPE_LMK_MAP = mapOf(
            // Zone Keys
            "000" to KeyTypeLmkInfo(0, 0, "ZMK"),           // Zone Master Key - LMK 00-01
            "001" to KeyTypeLmkInfo(6, 0, "ZPK"),           // Zone PIN Key - LMK 06-07 (pair index 3)

            // Terminal/PIN Keys (Legacy)
            "002" to KeyTypeLmkInfo(14, 0, "PVK/TPK/TMK"),  // PIN Verification/Terminal PIN/Master - LMK 14-15 (pair index 7)

            // Authentication Keys
            "003" to KeyTypeLmkInfo(16, 0, "TAK"),          // Terminal Authentication Key - LMK 16-17 (pair index 8)
            "008" to KeyTypeLmkInfo(26, 0, "ZAK"),          // Zone Authentication Key - LMK 26-27 (pair index 13)

            // DUKPT/EMV Keys - LMK 28-29 (pair index 14)
            "009" to KeyTypeLmkInfo(28, 0, "BDK"),          // Base Derivation Key (type 1)
            "109" to KeyTypeLmkInfo(28, 1, "MK-AC"),        // Master Key for Application Cryptograms
            "209" to KeyTypeLmkInfo(28, 2, "MK-SMI"),       // Master Key for Secure Messaging (Integrity)
            "309" to KeyTypeLmkInfo(28, 3, "MK-SMC"),       // Master Key for Secure Messaging (Confidentiality)
            "409" to KeyTypeLmkInfo(28, 4, "MK-DAC"),       // Master Key for Data Authentication Codes
            "509" to KeyTypeLmkInfo(28, 5, "MK-DN"),        // Master Key for Dynamic Numbers
            "609" to KeyTypeLmkInfo(28, 6, "BDK-2"),        // Base Derivation Key (type 2)
            "709" to KeyTypeLmkInfo(28, 7, "MK-CVC3"),      // Master Key for CVC3 (Contactless)
            "809" to KeyTypeLmkInfo(28, 8, "BDK-3"),        // Base Derivation Key (type 3)
            "909" to KeyTypeLmkInfo(28, 9, "BDK-4"),        // Base Derivation Key (type 4)

            // Encryption Keys
            "00A" to KeyTypeLmkInfo(30, 0, "ZEK"),          // Zone Encryption Key - LMK 30-31
            "00B" to KeyTypeLmkInfo(32, 0, "DEK"),          // Data Encryption Key - LMK 32-33

            // Card Verification
            "402" to KeyTypeLmkInfo(14, 4, "CVK"),          // Card Verification Key - LMK 14-15 var 4

            // PCI HSM Compliant Key Types (LMK 36-37)
            "70D" to KeyTypeLmkInfo(14, 0, "TPK-PCI"),      // Terminal PIN Key (PCI compliant)
            "80D" to KeyTypeLmkInfo(14, 0, "TMK-PCI"),      // Terminal Master Key (PCI compliant)
            "90D" to KeyTypeLmkInfo(14, 0, "TKR"),          // Terminal Key Register

            // DUKPT Initial Key
            "302" to KeyTypeLmkInfo(28, 0, "IKEY")          // Initial Key (IPEK)
        )

        // LMK Variant masks (XOR with first byte of LMK to create variant)
        val LMK_VARIANTS = mapOf(
            0 to 0x00,  // No variant
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
    }

    private val secureRandom = SecureRandom()

    /**
     * Execute the A0 command
     */
    fun execute(commandData: String): HsmCommandResult {
        return try {
            val request = parseRequest(commandData)

            // Validate key type
            val keyTypeInfo = KEY_TYPE_LMK_MAP[request.keyType]
                ?: return HsmCommandResult.Error(
                    HsmErrorCodes.INVALID_KEY_TYPE_CODE,
                    "Invalid key type code: ${request.keyType}"
                )

            // Validate key scheme and get key length
            val keyLength = getKeyLengthFromScheme(request.keyScheme)
            if (keyLength == 0) {
                return HsmCommandResult.Error(
                    HsmErrorCodes.INVALID_KEY_SCHEME,
                    "Invalid key scheme: ${request.keyScheme}"
                )
            }

            // Verify LMK is loaded
            val lmk = hsm.lmkStorage.getLmk(request.lmkId)
                ?: return HsmCommandResult.Error(
                    HsmErrorCodes.LMK_NOT_LOADED,
                    "No LMK loaded for identifier: ${request.lmkId}"
                )

            // Get LMK pair for this key type
            val lmkPairNumber = keyTypeInfo.lmkPairNumber / 2  // Convert to pair index (0-13)
            val lmkPair = lmk.getPair(lmkPairNumber)
                ?: return HsmCommandResult.Error(
                    HsmErrorCodes.LMK_NOT_LOADED,
                    "LMK pair $lmkPairNumber not available"
                )

            // Generate random key with proper parity
            val clearKey = generateRandomKey(keyLength)

            // Get LMK with variant applied
            val variantLmk = applyLmkVariant(lmkPair.getCombinedKey(), keyTypeInfo.variant)

            // Encrypt key under LMK
            val keyUnderLmk = encryptKey(clearKey, variantLmk, request.keyScheme)

            // Calculate Key Check Value (KCV)
            val kcv = hsm.calculateKeyCheckValue(clearKey)

            // Build response based on mode
            val result = when (request.mode) {
                '0' -> buildMode0Response(keyUnderLmk, kcv, request, keyTypeInfo)
                '1' -> buildMode1Response(clearKey, keyUnderLmk, kcv, request, keyTypeInfo)
                'A', 'B' -> buildIkeyResponse(clearKey, keyUnderLmk, kcv, request, keyTypeInfo)
                else -> return HsmCommandResult.Error(
                    HsmErrorCodes.FUNCTION_NOT_PERMITTED,
                    "Invalid mode: ${request.mode}"
                )
            }

            // Add audit log entry
            hsm.auditLog.addEntry(AuditEntry(
                entryType = AuditType.KEY_OPERATION,
                command = COMMAND_CODE,
                lmkId = request.lmkId,
                result = "SUCCESS",
                details = "Generated ${keyTypeInfo.description} key, Scheme=${request.keyScheme}, KCV=$kcv"
            ))

            result

        } catch (e: Exception) {
            HsmCommandResult.Error(
                HsmErrorCodes.INVALID_MESSAGE_LENGTH,
                "Parse error: ${e.message}"
            )
        }
    }

    /**
     * Parse the A0 command request
     */
    private fun parseRequest(data: String): A0Request {
        var offset = 0

        // Mode (1 character)
        val mode = data[offset]
        offset += 1

        // Key Type (3 characters)
        val keyType = data.substring(offset, offset + 3).uppercase()
        offset += 3

        // Key Scheme under LMK (1 character)
        val keyScheme = data[offset].uppercaseChar()
        offset += 1

        // Parse conditional fields based on mode
        var keySchemeZmk: Char? = null
        var zmk: String? = null
        var zmkKeyScheme: Char? = null
        var ksn: String? = null

        when (mode) {
            '1' -> {
                // Mode 1: Export under ZMK
                // Key Scheme under ZMK (1 character)
                if (offset < data.length) {
                    keySchemeZmk = data[offset].uppercaseChar()
                    offset += 1

                    // ZMK (variable length based on scheme prefix)
                    val (zmkValue, zmkLen) = extractKey(data, offset)
                    zmk = zmkValue
                    offset += zmkLen
                }
            }
            'A', 'B' -> {
                // Mode A/B: Generate IKEY from BDK
                // KSN (20 characters for DUKPT - 10 bytes hex)
                if (offset + 20 <= data.length) {
                    ksn = data.substring(offset, offset + 20)
                    offset += 20
                }

                if (mode == 'B' && offset < data.length) {
                    // Mode B also exports under ZMK/TMK
                    keySchemeZmk = data[offset].uppercaseChar()
                    offset += 1

                    val (zmkValue, zmkLen) = extractKey(data, offset)
                    zmk = zmkValue
                    offset += zmkLen
                }
            }
        }

        // Check for optional delimiter and LMK ID
        var lmkId = data.substring(offset, offset + 2).uppercase()
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
            lmkId = lmkId
        )
    }

    /**
     * Extract key from data with scheme prefix handling
     */
    private fun extractKey(data: String, offset: Int): Pair<String, Int> {
        if (offset >= data.length) return Pair("", 0)

        return when (val prefix = data[offset]) {
            'U', 'X' -> {
                // Double-length key: prefix + 32 hex chars
                val len = minOf(33, data.length - offset)
                Pair(data.substring(offset, offset + len), len)
            }
            'T', 'Y' -> {
                // Triple-length key: prefix + 48 hex chars
                val len = minOf(49, data.length - offset)
                Pair(data.substring(offset, offset + len), len)
            }
            'S' -> {
                // Thales Key Block - find delimiter or end
                var endIdx = offset + 1
                while (endIdx < data.length && data[endIdx] != '%' && data[endIdx] != ';') {
                    endIdx++
                }
                Pair(data.substring(offset, endIdx), endIdx - offset)
            }
            else -> {
                // Legacy format - 32 hex chars (double-length)
                val len = minOf(32, data.length - offset)
                Pair(data.substring(offset, offset + len), len)
            }
        }
    }

    /**
     * Generate a random DES/TDES key with correct odd parity
     */
    private fun generateRandomKey(length: Int): ByteArray {
        var key: ByteArray

        do {
            key = ByteArray(length)
            secureRandom.nextBytes(key)
            key = hsm.applyOddParity(key)
        } while (isWeakOrSemiWeakKey(key))

        return key
    }

    /**
     * Check if key is a DES weak or semi-weak key
     */
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

        // Check each 8-byte block
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

    /**
     * Apply LMK variant (XOR variant mask with first byte)
     */
    private fun applyLmkVariant(lmkKey: ByteArray, variant: Int): ByteArray {
        if (variant == 0) return lmkKey

        val variantMask = LMK_VARIANTS[variant] ?: return lmkKey
        val variantedKey = lmkKey.copyOf()

        // XOR variant with first byte of the key
        variantedKey[0] = (variantedKey[0].toInt() xor variantMask).toByte()

        return variantedKey
    }

    /**
     * Encrypt key under LMK (or other key-encrypting key)
     */
    private fun encryptKey(clearKey: ByteArray, encryptingKey: ByteArray, scheme: Char): String {
        val encrypted = performTdesEncrypt(clearKey, encryptingKey)

        return when (scheme) {
            'Z' -> hsm.bytesToHex(encrypted)              // Single-length, no prefix
            'U' -> "U" + hsm.bytesToHex(encrypted)        // Double-length variant
            'T' -> "T" + hsm.bytesToHex(encrypted)        // Triple-length variant
            'X' -> "X" + hsm.bytesToHex(encrypted)        // X9.17 double
            'Y' -> "Y" + hsm.bytesToHex(encrypted)        // X9.17 triple
            'S' -> buildThalesKeyBlock(clearKey, encryptingKey) // Thales Key Block
            else -> hsm.bytesToHex(encrypted)
        }
    }

    /**
     * Perform Triple-DES encryption
     */
    private fun performTdesEncrypt(data: ByteArray, key: ByteArray): ByteArray {
        return try {
            val cipher = Cipher.getInstance("DESede/ECB/NoPadding")

            // Ensure key is proper length for 3DES (16 or 24 bytes)
            val tdesKey = when {
                key.size == 16 -> key + key.sliceArray(0..7) // Double to triple
                key.size >= 24 -> key.sliceArray(0..23)
                else -> throw IllegalArgumentException("Invalid key size: ${key.size}")
            }

            val keySpec = SecretKeySpec(tdesKey, "DESede")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)

            // Pad data to block size if needed
            val paddedData = if (data.size % 8 != 0) {
                data + ByteArray(8 - (data.size % 8))
            } else {
                data
            }

            cipher.doFinal(paddedData)
        } catch (e: Exception) {
            data // Return original on error
        }
    }

    /**
     * Perform Triple-DES decryption
     */
    private fun performTdesDecrypt(data: ByteArray, key: ByteArray): ByteArray {
        return try {
            val cipher = Cipher.getInstance("DESede/ECB/NoPadding")

            val tdesKey = when {
                key.size == 16 -> key + key.sliceArray(0..7)
                key.size >= 24 -> key.sliceArray(0..23)
                else -> throw IllegalArgumentException("Invalid key size")
            }

            val keySpec = SecretKeySpec(tdesKey, "DESede")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            cipher.doFinal(data)
        } catch (e: Exception) {
            data
        }
    }

    /**
     * Build Thales Key Block format
     */
    private fun buildThalesKeyBlock(clearKey: ByteArray, lmk: ByteArray): String {
        // Simplified Thales Key Block structure
        // Format: S + Version(1) + Length(5) + Usage(2) + Algorithm(1) + ModeOfUse(1) +
        //         KeyVersionNumber(2) + Exportability(1) + OptBlockNum(2) + Reserved(2) +
        //         OptionalBlocks + EncryptedKey + MAC

        val keyBits = clearKey.size * 8
        val header = String.format("00%03dP0TN00N0000", keyBits)
        val encryptedKey = performTdesEncrypt(clearKey, lmk)

        // Simple MAC (in production, use CMAC)
        val mac = hsm.calculateKeyCheckValue(clearKey)

        return "S$header${hsm.bytesToHex(encryptedKey)}$mac"
    }

    /**
     * Decrypt ZMK from LMK encryption
     */
    private fun decryptZmkFromLmk(encryptedZmk: String, lmkId: String): ByteArray {
        val lmk = hsm.lmkStorage.getLmk(lmkId) ?: throw IllegalStateException("LMK not found")

        // ZMK uses LMK pair 00-01 (pair index 0)
        val zmkPair = lmk.getPair(0) ?: throw IllegalStateException("LMK pair 0 not found")

        // Remove scheme prefix if present
        val zmkHex = if (encryptedZmk[0].isLetter()) {
            encryptedZmk.substring(1)
        } else {
            encryptedZmk
        }

        val zmkBytes = hsm.hexToBytes(zmkHex)
        return performTdesDecrypt(zmkBytes, zmkPair.getCombinedKey())
    }

    /**
     * Get key length in bytes from scheme character
     */
    private fun getKeyLengthFromScheme(scheme: Char): Int {
        return when (scheme.uppercaseChar()) {
            'Z' -> 8   // Single-length
            'U' -> 16  // Double-length
            'T' -> 24  // Triple-length
            'X' -> 16  // X9.17 double
            'Y' -> 24  // X9.17 triple
            'S' -> 16  // Key Block (default double)
            'R' -> 16  // TR-31 Key Block
            else -> 0
        }
    }

    /**
     * Build response for Mode 0 (LMK only)
     */
    private fun buildMode0Response(
        keyUnderLmk: String,
        kcv: String,
        request: A0Request,
        keyTypeInfo: KeyTypeLmkInfo
    ): HsmCommandResult {
        val responseData = "$keyUnderLmk$kcv"

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

    /**
     * Build response for Mode 1 (LMK + ZMK export)
     */
    private fun buildMode1Response(
        clearKey: ByteArray,
        keyUnderLmk: String,
        kcv: String,
        request: A0Request,
        keyTypeInfo: KeyTypeLmkInfo
    ): HsmCommandResult {
        // Decrypt ZMK to get clear ZMK
        val clearZmk = decryptZmkFromLmk(request.zmk!!, request.lmkId)

        // Encrypt generated key under ZMK
        val keyUnderZmk = encryptKey(clearKey, clearZmk, request.keySchemeZmk!!)

        val responseData = "$keyUnderLmk$keyUnderZmk$kcv"

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
     * Build response for Mode A/B (IKEY generation for DUKPT)
     */
    private fun buildIkeyResponse(
        clearKey: ByteArray,
        keyUnderLmk: String,
        kcv: String,
        request: A0Request,
        keyTypeInfo: KeyTypeLmkInfo
    ): HsmCommandResult {
        val responseData = if (request.mode == 'B' && request.zmk != null) {
            val clearZmk = decryptZmkFromLmk(request.zmk, request.lmkId)
            val keyUnderZmk = encryptKey(clearKey, clearZmk, request.keySchemeZmk ?: 'U')
            "$keyUnderLmk$keyUnderZmk$kcv"
        } else {
            "$keyUnderLmk$kcv"
        }

        return HsmCommandResult.Success(
            response = responseData,
            data = mapOf(
                "keyUnderLmk" to keyUnderLmk,
                "kcv" to kcv,
                "keyType" to keyTypeInfo.description,
                "ksn" to (request.ksn ?: "N/A")
            )
        )
    }
}



/**
 * Data class for Key Type to LMK mapping info
 */
data class KeyTypeLmkInfo(
    val lmkPairNumber: Int,  // LMK pair number (e.g., 14 for LMK 14-15)
    val variant: Int,         // Variant number (0-9)
    val description: String   // Human-readable key type name
)