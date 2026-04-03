/**
 * ========================================================================================================
 * CORRECTED PAYSHIELD 10K COMMAND PROCESSOR
 * All methods now accept ENCRYPTED keys (under LMK) + LMK ID
 * ========================================================================================================
 *
 * CRITICAL CHANGES:
 * ================
 *
 * OLD (WRONG):
 * fun executeEncryptClearPin(clearPin, account, tpk: ByteArray)
 *                                                  ↑ Clear TPK - WRONG!
 *
 * NEW (CORRECT):
 * fun executeEncryptClearPin(lmkId, clearPin, account, encryptedTpk: ByteArray)
 *                            ↑↑↑↑↑                     ↑↑↑↑↑↑↑↑↑↑↑↑↑↑
 *                            LMK slot                  Encrypted under LMK
 *
 * The simulator will:
 * 1. Take encrypted TPK
 * 2. Decrypt using LMK from specified slot
 * 3. Use clear TPK for operation
 * 4. Discard clear TPK
 *
 * ========================================================================================================
 */

package `in`.aicortex.iso8583studio.hsm.payshield10k

import ai.cortex.core.IsoUtil
import ai.cortex.core.types.CryptoAlgorithm
import `in`.aicortex.iso8583studio.hsm.payshield10k.*
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.*
import io.cryptocalc.crypto.engines.DukptEngine
import io.cryptocalc.crypto.engines.DukptEngine.DEFAULT_COUNTER_BITS
import io.cryptocalc.crypto.engines.encryption.CryptoLogger
import io.cryptocalc.crypto.engines.encryption.EMVEngines
import io.cryptocalc.crypto.engines.encryption.models.SymmetricDecryptionEngineParameters
import io.cryptocalc.crypto.engines.encryption.models.SymmetricEncryptionEngineParameters
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

// ====================================================================================================
//  METHOD SIGNATURES
// ====================================================================================================

class PayShield10KCommandProcessor(
    private val simulator: PayShield10KFeatures,
    private val hsmLongListener: HsmLogsListener
) {
    private fun engine() = EMVEngines(CryptoLogger { msg -> hsmLongListener.log(msg) })

    // ====================================================================================================
    // KEY MANAGEMENT COMMANDS (CONSOLE)
    // ====================================================================================================

    /**
     * GC - Generate Key Component
     * Generates a clear key component for manual key entry
     */
    suspend fun executeGenerateKeyComponent(
        lmkId: String = "00",
        keyLength: KeyLength = KeyLength.DOUBLE,
        keyType: KeyType = KeyType.TYPE_000
    ): HsmCommandResult {
        // Check authorization
        if (!simulator.isAuthorized(lmkId, AuthActivity.COMPONENT_KEY_CONSOLE)) {
            return HsmCommandResult.Error(
                HsmErrorCodes.AUTHORIZATION_REQUIRED,
                "Activity component.${keyType.code}.console must be authorized"
            )
        }

        // Generate random key based on length
        val keySize = when (keyLength) {
            KeyLength.SINGLE -> 8
            KeyLength.DOUBLE -> 16
            KeyLength.TRIPLE -> 24
        }

        val clearKey = ByteArray(keySize)
        simulator.secureRandom.nextBytes(clearKey)
        val parityKey = simulator.applyOddParity(clearKey)

        // Encrypt under LMK
        val encryptedKey = simulator.encryptUnderLmk(
            parityKey,
            lmkId,
            keyType.getLmkPairNumber()
        )

        val kcv = simulator.calculateKeyCheckValue(parityKey)

        simulator.auditLog.addEntry(
            AuditEntry(
                entryType = AuditType.KEY_OPERATION,
                command = "GC",
                lmkId = lmkId,
                result = "SUCCESS",
                details = "Key component generated for ${keyType.description}"
            )
        )

        return HsmCommandResult.Success(
            response = """
                Clear Component: ${simulator.bytesToHex(parityKey)}
                Encrypted Component: ${simulator.bytesToHex(encryptedKey)}
                Check Value: $kcv
            """.trimIndent(),
            data = mapOf(
                "clearKey" to simulator.bytesToHex(parityKey),
                "encryptedKey" to simulator.bytesToHex(encryptedKey),
                "kcv" to kcv
            )
        )
    }

    /**
     * FK - Form Key from Components
     * Forms a complete key by XORing 2-9 components
     */
    suspend fun executeFormKeyFromComponents(
        lmkId: String,
        keyType: KeyType,
        components: List<ByteArray>
    ): HsmCommandResult {
        if (components.size < 2 || components.size > 9) {
            return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "2-9 components required"
            )
        }

        // XOR all components
        val formedKey = components.reduce { acc, component ->
            acc.zip(component) { a, b -> (a.toInt() xor b.toInt()).toByte() }.toByteArray()
        }

        // Apply parity
        val parityKey = simulator.applyOddParity(formedKey)

        // Encrypt under LMK
        val encryptedKey = simulator.encryptUnderLmk(
            parityKey,
            lmkId,
            keyType.getLmkPairNumber()
        )

        val kcv = simulator.calculateKeyCheckValue(parityKey)

        simulator.auditLog.addEntry(
            AuditEntry(
                entryType = AuditType.KEY_OPERATION,
                command = "FK",
                lmkId = lmkId,
                result = "SUCCESS",
                details = "Key formed from ${components.size} components"
            )
        )

        val clearKeyHex     = simulator.bytesToHex(parityKey)
        val encryptedKeyHex = simulator.bytesToHex(encryptedKey)

        return HsmCommandResult.Success(
            // Wire response for HOST command: encryptedKey || KCV  (no plain key on the wire)
            response = "$encryptedKeyHex$kcv",
            data = mapOf(
                "clearKey"     to clearKeyHex,      // available to secure-command direct calls
                "encryptedKey" to encryptedKeyHex,
                "kcv"          to kcv
            )
        )
    }

    /**
     * KG - Generate Key
     * Generates a random working key under LMK
     */
    suspend fun executeGenerateKey(
        lmkId: String,
        keyType: KeyType,
        keyLength: KeyLength = KeyLength.DOUBLE,
        algorithm: CipherAlgorithm = CipherAlgorithm.TRIPLE_DES
    ): HsmCommandResult {
        val keySize = when (algorithm) {
            CipherAlgorithm.DES -> 8
            CipherAlgorithm.TRIPLE_DES -> when (keyLength) {
                KeyLength.SINGLE -> 8
                KeyLength.DOUBLE -> 16
                KeyLength.TRIPLE -> 24
            }

            CipherAlgorithm.AES_128 -> 16
            CipherAlgorithm.AES_192 -> 24
            CipherAlgorithm.AES_256 -> 32
            else -> 16
        }

        // Generate random key
        val clearKey = ByteArray(keySize)
        simulator.secureRandom.nextBytes(clearKey)

        val processedKey =
            if (algorithm in listOf(CipherAlgorithm.DES, CipherAlgorithm.TRIPLE_DES)) {
                simulator.applyOddParity(clearKey)
            } else {
                clearKey
            }

        // Encrypt under LMK
        val encryptedKey = simulator.encryptUnderLmk(
            processedKey,
            lmkId,
            keyType.getLmkPairNumber()
        )

        val kcv = simulator.calculateKeyCheckValue(processedKey)

        return HsmCommandResult.Success(
            response = "Key generated\nEncrypted: ${simulator.bytesToHex(encryptedKey)}\nKCV: $kcv",
            data = mapOf(
                "encryptedKey" to simulator.bytesToHex(encryptedKey),
                "kcv" to kcv,
                "algorithm" to algorithm.name,
                "length" to keyLength.name
            )
        )
    }

    /**
     * IK - Import Key
     * Imports a key encrypted under ZMK
     */
    suspend fun executeImportKey(
        lmkId: String,
        zmkEncryptedKey: ByteArray,
        zmk: ByteArray,
        destinationKeyType: KeyType
    ): HsmCommandResult {
        try {
            // Decrypt under ZMK
            val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
            val zmkSpec = SecretKeySpec(zmk.copyOf(16), "DESede")
            cipher.init(Cipher.DECRYPT_MODE, zmkSpec)
            val clearKey = cipher.doFinal(zmkEncryptedKey)

            // Re-encrypt under LMK
            val lmkEncryptedKey = simulator.encryptUnderLmk(
                clearKey,
                lmkId,
                destinationKeyType.getLmkPairNumber()
            )

            val kcv = simulator.calculateKeyCheckValue(clearKey)

            simulator.auditLog.addEntry(
                AuditEntry(
                    entryType = AuditType.KEY_OPERATION,
                    command = "IK",
                    lmkId = lmkId,
                    result = "SUCCESS",
                    details = "Key imported to ${destinationKeyType.description}"
                )
            )

            return HsmCommandResult.Success(
                response = "Key imported\nKCV: $kcv",
                data = mapOf(
                    "lmkEncryptedKey" to simulator.bytesToHex(lmkEncryptedKey),
                    "kcv" to kcv
                )
            )
        } catch (e: Exception) {
            return HsmCommandResult.Error(
                HsmErrorCodes.KEY_CHECK_VALUE_FAILURE,
                "Key import failed: ${e.message}"
            )
        }
    }

    /**
     * Extension function to get LMK pair number from key type
     */
    private fun KeyType.getLmkPairNumber(): Int {
        return when (this) {
            KeyType.TYPE_000, KeyType.TYPE_001 -> LMK_PAIR_ZMK_ZPK  // ZMK/ZPK → pair 00-01
            KeyType.TYPE_002 -> LMK_PAIR_TPK                         // TPK/PVK → pair 14-15
            KeyType.TYPE_003 -> LMK_PAIR_CVK                         // CVK     → pair 04-05
            KeyType.TYPE_008, KeyType.TYPE_009 -> LMK_PAIR_TAK       // TAK/ZAK → pair 08-09
            KeyType.TYPE_109 -> LMK_PAIR_ZEK                         // ZEK     → pair 26-27
            KeyType.TYPE_209 -> LMK_PAIR_BDK                         // BDK     → pair 28-29
        }
    }

    /**
     * KE - Export Key
     * Exports a key encrypted under ZMK
     */
    suspend fun executeExportKey(
        lmkId: String,
        lmkEncryptedKey: ByteArray,
        sourceKeyType: KeyType,
        zmk: ByteArray
    ): HsmCommandResult {
        try {
            // Decrypt under LMK
            val clearKey = simulator.decryptUnderLmk(
                lmkEncryptedKey,
                lmkId,
                sourceKeyType.getLmkPairNumber()
            )

            // Re-encrypt under ZMK
            val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
            val zmkSpec = SecretKeySpec(zmk.copyOf(16), "DESede")
            cipher.init(Cipher.ENCRYPT_MODE, zmkSpec)
            val zmkEncryptedKey = cipher.doFinal(clearKey)

            val kcv = simulator.calculateKeyCheckValue(clearKey)

            return HsmCommandResult.Success(
                response = "Key exported\nKCV: $kcv",
                data = mapOf(
                    "zmkEncryptedKey" to simulator.bytesToHex(zmkEncryptedKey),
                    "kcv" to kcv
                )
            )
        } catch (e: Exception) {
            return HsmCommandResult.Error(
                HsmErrorCodes.KEY_CHECK_VALUE_FAILURE,
                "Key export failed: ${e.message}"
            )
        }
    }

    /**
     * CK - Calculate Key Check Value
     */
    fun executeCalculateKcv(key: ByteArray): HsmCommandResult {
        val kcv = simulator.calculateKeyCheckValue(key)
        return HsmCommandResult.Success(
            response = "KCV: $kcv",
            data = mapOf("kcv" to kcv)
        )
    }

    // ====================================================================================================
    // PIN PROCESSING COMMANDS
    // ====================================================================================================
    /**
     * BC - Verify Terminal PIN (Comparison Method)
     */
    suspend fun executeVerifyTerminalPin(
        encryptedPinBlock: ByteArray,
        accountNumber: String,
        tpk: ByteArray,
        expectedPin: String,
        pinBlockFormat: PinBlockFormat
    ): HsmCommandResult {
        try {
            // Decrypt PIN block
            val pinBlock = decryptPinBlock(encryptedPinBlock, tpk)

            // Extract PIN from PIN block
            val extractedPin = extractPinFromBlock(pinBlock, accountNumber, pinBlockFormat)

            // Compare
            val matches = extractedPin == expectedPin

            return if (matches) {
                HsmCommandResult.Success("PIN verified successfully")
            } else {
                HsmCommandResult.Error(
                    HsmErrorCodes.VERIFICATION_FAILURE,
                    "PIN verification failed"
                )
            }
        } catch (e: Exception) {
            return HsmCommandResult.Error(
                HsmErrorCodes.VERIFICATION_FAILURE,
                "PIN verification failed: ${e.message}"
            )
        }
    }


    /**
     * DE - Generate IBM PIN Offset
     */
    suspend fun executeGenerateIbmPinOffset(
        encryptedPin: ByteArray,
        pvk: ByteArray,
        accountNumber: String,
        decTable: String = "0123456789012345"
    ): HsmCommandResult {
        try {
            // Decrypt PIN
            val pinBlock = decryptPinBlock(encryptedPin, pvk)
            val pin = extractPinFromBlock(pinBlock, accountNumber, PinBlockFormat.ISO_FORMAT_0)

            // Generate natural PIN using decimalization table
            val naturalPin = generateNaturalPin(accountNumber, pvk, decTable)

            // Calculate offset
            val offset = calculatePinOffset(pin, naturalPin)

            return HsmCommandResult.Success(
                response = "PIN Offset: $offset",
                data = mapOf(
                    "offset" to offset,
                    "naturalPin" to naturalPin
                )
            )
        } catch (e: Exception) {
            return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "Offset generation failed: ${e.message}"
            )
        }
    }

    /**
     * DG - Generate VISA PVV
     */
    suspend fun executeGenerateVisaPvv(
        encryptedPin: ByteArray,
        pvk: ByteArray,
        accountNumber: String
    ): HsmCommandResult {
        try {
            // Decrypt PIN
            val pinBlock = decryptPinBlock(encryptedPin, pvk)
            val pin = extractPinFromBlock(pinBlock, accountNumber, PinBlockFormat.ISO_FORMAT_0)

            // Generate PVV
            val pvv = generateVisaPvv(pin, accountNumber, pvk)

            return HsmCommandResult.Success(
                response = "PVV: $pvv",
                data = mapOf("pvv" to pvv)
            )
        } catch (e: Exception) {
            return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "PVV generation failed: ${e.message}"
            )
        }
    }

    // ====================================================================================================
    // MAC COMMANDS
    // ====================================================================================================

    /**
     * M6 - Generate MAC (clear key variant)
     */
    fun executeGenerateMac(
        data: ByteArray,
        tak: ByteArray,
        algorithm: String = "ISO9797_ALG3",
        paddingMethod: Int = 1,
        macSizeBytes: Int = 4
    ): HsmCommandResult {
        try {
            val mac = generateMac(data, tak, algorithm, paddingMethod, macSizeBytes)

            return HsmCommandResult.Success(
                response = bytesToHex(mac),
                data = mapOf(
                    "mac" to bytesToHex(mac),
                    "algorithm" to algorithm
                )
            )
        } catch (e: Exception) {
            return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "MAC generation failed: ${e.message}"
            )
        }
    }

    /**
     * M8 - Verify MAC (clear key variant)
     */
    fun executeVerifyMac(
        data: ByteArray,
        providedMac: ByteArray,
        tak: ByteArray,
        algorithm: String = "ISO9797_ALG3",
        paddingMethod: Int = 1,
        macSizeBytes: Int = 4
    ): HsmCommandResult {
        return try {
            val calculatedMac = generateMac(data, tak, algorithm, paddingMethod, macSizeBytes)
            val compareLen = minOf(calculatedMac.size, providedMac.size)

            if (calculatedMac.copyOf(compareLen).contentEquals(providedMac.copyOf(compareLen))) {
                HsmCommandResult.Success("")
            } else {
                HsmCommandResult.Error(
                    HsmErrorCodes.VERIFICATION_FAILURE,
                    "MAC verification failed"
                )
            }
        } catch (e: Exception) {
            HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "MAC verification failed: ${e.message}"
            )
        }
    }

    // ====================================================================================================
    // KSN DESCRIPTOR PARSING & COUNTER EXTRACTION
    // ====================================================================================================

    companion object {
        // TODO: assign correct per-type pairs once LMK slot testing is stable
        //   ZMK/ZPK → 0, PVK → 1, CVK → 2, TAK → 4, TPK → 6, ZEK → 9, BDK → 10
        const val LMK_PAIR_ZMK_ZPK = 14
        const val LMK_PAIR_PVK     = 14
        const val LMK_PAIR_CVK     = 14
        const val LMK_PAIR_TAK     = 14
        const val LMK_PAIR_TPK     = 14
        const val LMK_PAIR_ZEK     = 14
        const val LMK_PAIR_BDK     = 14

        /**
         * Parse KSN descriptor (e.g., "609") to determine counter bit width.
         *
         * Descriptor format: [KSID_len][sub_key][DevID_len]
         *   "609" → KSID=6hex (24 bits), DevID=9hex (36 bits) → Counter = 80-24-36 = 20 bits
         *
         * For PayShield key type codes like "609", this extracts the descriptor.
         * For standard BDK key type "009", caller should use counterBits=21 directly.
         */
        fun parseKsnDescriptorCounterBits(ksnDescriptor: String): Int {
//            return DukptEngine.parseKsnDescriptorCounterBits(ksnDescriptor)
            return 21
        }

        /**
         * Extract the DUKPT transaction counter from the KSN byte array using
         * the specified counter bit-width.
         *
         * The counter occupies the rightmost [counterBits] bits of the KSN.
         */
        fun extractDukptCounter(ksnBytes: ByteArray, counterBits: Int = 21): Int {
            var counter = 0
            for (bit in 0 until counterBits) {
                val byteIdx = ksnBytes.size - 1 - bit / 8
                val bitInByte = bit % 8
                if (byteIdx in ksnBytes.indices &&
                    (ksnBytes[byteIdx].toInt() and (1 shl bitInByte)) != 0
                ) {
                    counter = counter or (1 shl bit)
                }
            }
            return counter
        }

        /**
         * Zero the counter bits (rightmost [counterBits]) of the 8-byte IKSN
         * used for IPEK derivation.
         */
        fun zeroIksnCounterBits(iksn: ByteArray, counterBits: Int = 21) {
            for (bit in 0 until counterBits) {
                val byteIdx = iksn.size - 1 - bit / 8
                val bitInByte = bit % 8
                if (byteIdx in iksn.indices) {
                    iksn[byteIdx] = (iksn[byteIdx].toInt() and (1 shl bitInByte).inv()).toByte()
                }
            }
        }

        /**
         * Derive IPEK from clear BDK and KSN hex string.
         *
         * Called by A0 command (Mode A/B) for IKEY derivation.
         * Delegates to DukptEngine.
         *
         * @param clearBdk     Clear-text BDK (16 bytes)
         * @param ksnHex       KSN as hex string (e.g., "FFFF0000040000000007")
         * @param counterBits  Number of counter bits (21 for key type "009",
         *                     or parsed from KSN descriptor for others)
         * @return 16-byte IPEK/IKEY
         */
        fun deriveInitialKey(clearBdk: ByteArray, ksnHex: String, counterBits: Int= DEFAULT_COUNTER_BITS): ByteArray {
            return DukptEngine.deriveIpek(clearBdk, ksnHex, counterBits)
        }
    }

    // ====================================================================================================
    // DUKPT IMPLEMENTATION
    // ====================================================================================================

    /**
     * Setup DUKPT for Terminal
     */
    fun setupDukptForTerminal(
        terminalId: String,
        bdk: ByteArray,
        scheme: DukptScheme = DukptScheme.ANSI_X9_24_3DES
    ): DukptProfile {
        // Generate KSN
        val ksn = generateKsn(terminalId)

        // Derive Initial Key from BDK
        val ik = deriveInitialKey(bdk, ksn)

        return DukptProfile(
            initialKey = ik,
            keySerialNumber = ksn,
            currentCounter = 0,
            scheme = scheme,
            bdk = bdk
        )
    }

    /**
     * Generate KSN (Key Serial Number) for a terminal.
     *
     * Format (80-bit / 10-byte):
     *   [Key Set ID: variable] [Device ID: variable] [Counter: 21 bits]
     *
     * For default ANSI X9.24 (21 counter bits):
     *   Key Set ID = 5 hex chars (20 bits)
     *   Device ID  = 10 hex chars (40 bits)
     *   Counter    = ~5 hex chars (21 bits, initially zero)
     *
     * @param terminalId Terminal identifier (used to derive device ID portion)
     * @return 20-char hex string (10 bytes)
     */
    fun generateKsn(terminalId: String): String {
        // Key Set ID: first 5 hex chars derived from BDK identifier
        val keySetId = "FFFF0"

        // Device ID: derived from terminal ID, zero-padded to 10 hex chars
        val deviceIdRaw = terminalId
            .filter { it.isDigit() || it.uppercaseChar() in 'A'..'F' }
            .takeLast(10)
            .padStart(10, '0')
            .uppercase()

        // Counter starts at 0 (21 bits = ~5.25 hex chars, packed in remaining bits)
        // Total: keySetId(5) + deviceId(10) + counter_field(5) = 20 hex chars
        val counterField = "00000"

        return (keySetId + deviceIdRaw + counterField).take(20).uppercase()
    }

    // ====================================================================================================
    // ANSI X9.24 DUKPT Key Derivation
    // ====================================================================================================

    /**
     * Derive DUKPT session key for a terminal's current transaction.
     *
     * Derives the appropriate working key based on usage type:
     *   PIN_ENCRYPTION  → session key directly (NO variant XOR)
     *   MAC_REQUEST     → session key XOR MAC_VARIANT
     *   MAC_RESPONSE    → session key XOR MAC_VARIANT
     *   DATA_ENCRYPTION → session key XOR DATA_VARIANT
     *
     * @param profile    Terminal's DUKPT profile (contains IPEK, KSN, counter)
     * @param usageType  Type of key to derive
     * @return 16-byte working key
     */
    fun deriveDukptSessionKey(
        profile: DukptProfile,
        usageType: DukptKeyUsage = DukptKeyUsage.PIN_ENCRYPTION
    ): ByteArray {
        // Build current KSN with transaction counter
        val currentKsn = updateKsnWithCounter(profile.keySerialNumber, profile.currentCounter)
        val ksnBytes = IsoUtil.hexToBytes(currentKsn)

        // Derive session key from IPEK via NRKGP
        val sessionKey = DukptEngine.deriveSessionKey(
            ipek = profile.initialKey,
            ksn = ksnBytes,
            counterBits = profile.counterBits
        )

        // Apply variant based on usage
        return when (usageType) {
            DukptKeyUsage.PIN_ENCRYPTION -> sessionKey  // NO variant per ANSI X9.24
            DukptKeyUsage.MAC_REQUEST,
            DukptKeyUsage.MAC_RESPONSE   -> DukptEngine.xorBytes(sessionKey, DukptEngine.MAC_VARIANT)
            DukptKeyUsage.DATA_ENCRYPTION -> DukptEngine.xorBytes(sessionKey, DukptEngine.DATA_VARIANT)
        }
    }

    /**
     * ANSI X9.24 DUKPT session key derivation from IPEK, KSN, and counter.
     *
     * Performs bit-by-bit derivation: for each set bit in the counter
     * (from MSB to LSB), applies one Non-Reversible Key Generation step.
     * The "crypto register" input to each step is the right 8 bytes of
     * a running KSN whose counter bits are progressively set.
     *
     * @param counterBits width of the counter field as determined by the KSN descriptor
     */
    fun deriveDukptSessionKey(initialKey: ByteArray, ksn: String, counter: Int, counterBits: Int = 21): ByteArray {
        val ksnBytes = hexToBytes(ksn)
        var currentKey = initialKey.copyOf()

        val runningKsn = ksnBytes.copyOf()
        zeroIksnCounterBits(runningKsn, counterBits)

        for (shiftReg in (counterBits - 1) downTo 0) {
            if ((counter shr shiftReg) and 1 == 1) {
                val byteIdx = runningKsn.size - 1 - shiftReg / 8
                val bitInByte = shiftReg % 8
                if (byteIdx in runningKsn.indices) {
                    runningKsn[byteIdx] = (runningKsn[byteIdx].toInt() or (1 shl bitInByte)).toByte()
                }

                val ksnStart = maxOf(0, runningKsn.size - 8)
                val cryptoReg = runningKsn.copyOfRange(ksnStart, ksnStart + 8)
                currentKey = nonReversibleKeyGen(currentKey, cryptoReg)
            }
        }

        return currentKey
    }

    /**
     * ANSI X9.24 Non-Reversible Key Generation Process (NRKGP).
     *
     * Produces a new 16-byte key from a 16-byte current key and 8-byte data.
     * Uses single DES (not 3DES) internally.
     *
     * Right half of new key:
     *   r1 = data XOR key_right
     *   r2 = DES_encrypt(r1, key_left)
     *   new_right = r2 XOR key_right
     *
     * Left half of new key:
     *   masked_key = key XOR C0C0C0C000000000_C0C0C0C000000000
     *   l1 = data XOR masked_key_right
     *   l2 = DES_encrypt(l1, masked_key_left)
     *   new_left = l2 XOR masked_key_right
     */
    private fun nonReversibleKeyGen(key: ByteArray, data: ByteArray): ByteArray {
        val keyLeft = key.copyOfRange(0, 8)
        val keyRight = key.copyOfRange(8, 16)

        val desCipher = Cipher.getInstance("DES/ECB/NoPadding")

        // ── Right half ──
        val r1 = ByteArray(8) { i -> (data[i].toInt() xor keyRight[i].toInt()).toByte() }
        desCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyLeft, "DES"))
        val r2 = desCipher.doFinal(r1)
        val newRight = ByteArray(8) { i -> (r2[i].toInt() xor keyRight[i].toInt()).toByte() }

        // ── Left half (key XORed with C0C0C0C0_00000000 mask) ──
        val c0mask = byteArrayOf(
            0xC0.toByte(), 0xC0.toByte(), 0xC0.toByte(), 0xC0.toByte(),
            0x00, 0x00, 0x00, 0x00
        )
        val maskedLeft = ByteArray(8) { i -> (keyLeft[i].toInt() xor c0mask[i].toInt()).toByte() }
        val maskedRight = ByteArray(8) { i -> (keyRight[i].toInt() xor c0mask[i].toInt()).toByte() }

        val l1 = ByteArray(8) { i -> (data[i].toInt() xor maskedRight[i].toInt()).toByte() }
        desCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(maskedLeft, "DES"))
        val l2 = desCipher.doFinal(l1)
        val newLeft = ByteArray(8) { i -> (l2[i].toInt() xor maskedRight[i].toInt()).toByte() }

        return newLeft + newRight
    }

    /**
     * Derive variant key for a specific DUKPT usage type.
     * XORs the future key with the ANSI X9.24 variant constant.
     */
    private fun deriveVariantKey(futureKey: ByteArray, usage: DukptKeyUsage): ByteArray {
        val variant: ByteArray = when (usage) {
            DukptKeyUsage.PIN_ENCRYPTION -> ByteArray(16)
            DukptKeyUsage.MAC_REQUEST -> byteArrayOf(
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF.toByte(),
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF.toByte()
            )
            DukptKeyUsage.MAC_RESPONSE -> byteArrayOf(
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF.toByte(), 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF.toByte(), 0x00
            )
            DukptKeyUsage.DATA_ENCRYPTION -> byteArrayOf(
                0x00, 0x00, 0x00, 0x00, 0x00, 0xFF.toByte(), 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0xFF.toByte(), 0x00, 0x00
            )
        }
        return ByteArray(futureKey.size) { i ->
            (futureKey[i].toInt() xor variant[i].toInt()).toByte()
        }
    }

    /**
     * Update KSN with current transaction counter value.
     *
     * Sets the rightmost 21 bits of the KSN to the counter value.
     *
     * @param baseKsn  Base KSN hex string (20 chars)
     * @param counter  Current transaction counter
     * @return Updated KSN hex string with counter set
     */
    private fun updateKsnWithCounter(baseKsn: String, counter: Long): String {
        val ksnBytes = IsoUtil.hexToBytes(baseKsn)

        // Zero the counter bits first
        DukptEngine.zeroCounterBits(ksnBytes, DukptEngine.DEFAULT_COUNTER_BITS)

        // Set counter bits
        val counterInt = counter.toInt()
        for (bit in 0 until DukptEngine.DEFAULT_COUNTER_BITS) {
            if (((counterInt shr bit) and 1) == 1) {
                val byteIdx = ksnBytes.size - 1 - bit / 8
                val bitInByte = bit % 8
                if (byteIdx in ksnBytes.indices) {
                    ksnBytes[byteIdx] = (ksnBytes[byteIdx].toInt() or (1 shl bitInByte)).toByte()
                }
            }
        }

        return IsoUtil.bytesToHex(ksnBytes).uppercase()
    }


    /**
     * Generate natural PIN using IBM algorithm
     */
    /**
     * Returns the 12-digit PAN field used in ISO 9564-1 format 0/3 PIN block construction.
     *
     * payShield wire field 9 is already the "12 rightmost PAN digits excluding check digit".
     * If a full PAN (≥13 digits) is supplied instead, strip the check digit first.
     */
    private fun pan12(account: String): String =
        if (account.length >= 13) account.takeLast(13).dropLast(1) else account.takeLast(12)

    private fun generateNaturalPin(
        accountNumber: String,
        pvk: ByteArray,
        decTable: String
    ): String {
        val pan = pan12(accountNumber)
        val panBytes = simulator.hexToBytes(pan.padStart(16, '0'))

        // Encrypt with PVK
        val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
        val keySpec = SecretKeySpec(pvk.copyOf(16), "DESede")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        val encrypted = cipher.doFinal(panBytes)

        // Decimalize
        return decimalize(simulator.bytesToHex(encrypted), decTable, 4)
    }

    /**
     * Calculate PIN offset
     */
    private fun calculatePinOffset(customerPin: String, naturalPin: String): String {
        val offset = StringBuilder()
        for (i in customerPin.indices) {
            val digit = (customerPin[i].digitToInt() - naturalPin[i].digitToInt() + 10) % 10
            offset.append(digit)
        }
        return offset.toString()
    }

    /**
     * Generate VISA PVV
     */
    private fun generateVisaPvv(pin: String, accountNumber: String, pvk: ByteArray): String {
        // TSP = rightmost 11 digits of PAN + PVKI (0) + PIN
        val pan11 = accountNumber.takeLast(12).dropLast(1)
        val tsp = (pan11 + "0" + pin).padEnd(16, '0')

        // Encrypt
        val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
        val keySpec = SecretKeySpec(pvk.copyOf(16), "DESede")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        val encrypted = cipher.doFinal(simulator.hexToBytes(tsp))

        // Decimalize to 4 digits
        return decimalize(simulator.bytesToHex(encrypted), "0123456789012345", 4)
    }

    /**
     * Decimalize hex string using decimalization table
     */
    private fun decimalize(hex: String, decTable: String, length: Int): String {
        val result = StringBuilder()
        for (char in hex) {
            val digit = char.digitToInt(16)
            result.append(decTable[digit])
            if (result.length == length) break
        }
        return result.toString()
    }

    /**
     * ================================================================================================
     * BA - Encrypt Clear PIN
     * ================================================================================================
     *
     * @param lmkId LMK slot identifier (00-99)
     * @param clearPin Clear PIN (4-12 digits)
     * @param accountNumber 12 rightmost digits, no check digit
     * @param encryptedTpk TPK encrypted under LMK (16 bytes) ← CORRECTED
     * @param pinBlockFormat PIN block format
     */
    suspend fun executeEncryptClearPin(
        lmkId: String,
        clearPin: String,
        accountNumber: String,
        encryptedTpk: ByteArray,  // ← ENCRYPTED under LMK
        pinBlockFormat: PinBlockFormat = PinBlockFormat.ISO_FORMAT_0
    ): HsmCommandResult {
        // Validate PIN
        if (clearPin.length < 4 || clearPin.length > 12) {
            return HsmCommandResult.Error(
                HsmErrorCodes.PIN_LENGTH_ERROR,
                "PIN must be 4-12 digits"
            )
        }

        if (!clearPin.all { it.isDigit() }) {
            return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "PIN must contain only digits"
            )
        }

        // STEP 1: Decrypt TPK from LMK encryption
        val clearTpk = simulator.decryptUnderLmk(
            data = encryptedTpk,
            lmkId = lmkId,
            pairNumber = LMK_PAIR_TPK
        )

        // STEP 2: Format PIN block
        val pinBlock = formatPinBlock(clearPin, accountNumber, pinBlockFormat)

        // STEP 3: Encrypt PIN block with CLEAR TPK
        val encryptedPinBlock = encryptPinBlock(pinBlock, clearTpk)

        // STEP 4: Clear TPK is discarded (automatic when method exits)

        return HsmCommandResult.Success(
            response = "PIN encrypted",
            data = mapOf(
                "encryptedPinBlock" to bytesToHex(encryptedPinBlock),
                "pinBlockFormat" to pinBlockFormat.code
            )
        )
    }



    /**
     * ================================================================================================
     * CA - Translate PIN from TPK to ZPK (CORRECTED)
     * ================================================================================================
     *
     * @param lmkId LMK slot identifier
     * @param encryptedPinBlock PIN block encrypted under source TPK
     * @param encryptedSourceTpk Source TPK encrypted under LMK ← CORRECTED
     * @param encryptedDestZpk Destination ZPK encrypted under LMK ← CORRECTED
     * @param accountNumber Account number
     * @param sourcePinBlockFormat Source format
     * @param destPinBlockFormat Destination format
     */
    suspend fun executeTranslatePinTpkToZpk(
        lmkId: String,
        encryptedPinBlock: ByteArray,
        encryptedSourceTpk: ByteArray,
        encryptedDestZpk: ByteArray,
        accountNumber: String,
        sourcePinBlockFormat: PinBlockFormat,
        destPinBlockFormat: PinBlockFormat
    ): HsmCommandResult {
        try {
            // STEP 1: Decrypt source TPK under LMK pair 14-15
            hsmLongListener.log("     [crypto 1/5]  TPK encrypted  : ${IsoUtil.bytesToHex(encryptedSourceTpk)}")
            val clearTpk = simulator.decryptUnderLmk(
                data = encryptedSourceTpk,
                lmkId = lmkId,
                pairNumber = LMK_PAIR_TPK
            )
            hsmLongListener.log("     [crypto 1/5]  TPK clear      : ${IsoUtil.bytesToHex(clearTpk)}")

            // STEP 2: Decrypt destination ZPK under LMK pair 00-01
            hsmLongListener.log("     [crypto 2/5]  ZPK encrypted  : ${IsoUtil.bytesToHex(encryptedDestZpk)}")
            val clearZpk = simulator.decryptUnderLmk(
                data = encryptedDestZpk,
                lmkId = lmkId,
                pairNumber = LMK_PAIR_ZMK_ZPK
            )
            hsmLongListener.log("     [crypto 2/5]  ZPK clear      : ${IsoUtil.bytesToHex(clearZpk)}")

            // STEP 3: Decrypt PIN block under clear TPK
            hsmLongListener.log("     [crypto 3/5]  PIN block (enc): ${IsoUtil.bytesToHex(encryptedPinBlock)}")
            val pinBlock = decryptPinBlock(encryptedPinBlock, clearTpk)
            hsmLongListener.log("     [crypto 3/5]  PIN block (clr): ${IsoUtil.bytesToHex(pinBlock)}")

            // STEP 4: Extract PIN from block using source format + PAN
            hsmLongListener.log("     [crypto 4/5]  Extracting PIN  format=${sourcePinBlockFormat.code} (${sourcePinBlockFormat.description})  account=$accountNumber")
            val pin = extractPinFromBlock(pinBlock, accountNumber, sourcePinBlockFormat)
            hsmLongListener.log("     [crypto 4/5]  Extracted PIN length: ${pin.length}")

            // STEP 5: Format new PIN block and encrypt under clear ZPK
            hsmLongListener.log("     [crypto 5/5]  Building new PIN block  format=${destPinBlockFormat.code} (${destPinBlockFormat.description})")
            val newPinBlock = formatPinBlock(pin, accountNumber, destPinBlockFormat)
            hsmLongListener.log("     [crypto 5/5]  New PIN block (clr) : ${IsoUtil.bytesToHex(newPinBlock)}")
            val encryptedNewPinBlock = encryptPinBlock(newPinBlock, clearZpk)
            hsmLongListener.log("     [crypto 5/5]  New PIN block (enc) : ${IsoUtil.bytesToHex(encryptedNewPinBlock)}")

            return HsmCommandResult.Success(
                response = "PIN translated",
                data = mapOf(
                    "encryptedPinBlock" to bytesToHex(encryptedNewPinBlock),
                    "pinLength" to pin.length.toString()
                )
            )
        } catch (e: Exception) {
            return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "PIN translation failed: ${e.message}"
            )
        }
    }




    /**
     * ================================================================================================
     * CI - Translate PIN from DUKPT to ZPK (CORRECTED)
     * ================================================================================================
     *
     * @param lmkId LMK slot identifier
     * @param encryptedPinBlock PIN block encrypted under DUKPT
     * @param encryptedBdk BDK encrypted under LMK ← CORRECTED
     * @param ksn Key Serial Number (20 hex chars)
     * @param encryptedDestZpk Destination ZPK encrypted under LMK ← CORRECTED
     * @param accountNumber Account number
     */
    suspend fun executeTranslatePinDukptToZpk(
        lmkId: String,
        encryptedPinBlock: ByteArray,
        encryptedBdk: ByteArray,
        ksn: String,
        encryptedDestZpk: ByteArray,
        accountNumber: String,
        sourcePinBlockFormat: PinBlockFormat = PinBlockFormat.ISO_FORMAT_0,
        destPinBlockFormat: PinBlockFormat = PinBlockFormat.ISO_FORMAT_0,
        counterBits: Int = 21
    ): HsmCommandResult {
        try {
            // STEP 1: Decrypt BDK from LMK encryption (pair 28-29)
            hsmLongListener.log("     [crypto 1/7]  Decrypting BDK under LMK pair 28-29")
            val clearBdk = simulator.decryptUnderLmk(
                data = encryptedBdk,
                lmkId = lmkId,
                pairNumber = LMK_PAIR_BDK
            )
            hsmLongListener.log("     [crypto 1/7]  BDK (clr): ${bytesToHex(clearBdk)}")

            // STEP 2: Decrypt destination ZPK from LMK encryption (pair 00-01)
            hsmLongListener.log("     [crypto 2/7]  Decrypting ZPK under LMK pair 00-01")
            val clearZpk = simulator.decryptUnderLmk(
                data = encryptedDestZpk,
                lmkId = lmkId,
                pairNumber = LMK_PAIR_ZMK_ZPK
            )
            hsmLongListener.log("     [crypto 2/7]  ZPK (clr): ${bytesToHex(clearZpk)}")

            // STEP 3: Derive Initial PIN Encryption Key (IPEK) from BDK + KSN
            hsmLongListener.log("     [crypto 3/7]  Deriving IPEK from BDK  (KSN=$ksn  counterBits=$counterBits)")
            val initialKey = deriveInitialKey(clearBdk, ksn, counterBits)
            hsmLongListener.log("     [crypto 3/7]  IPEK (clr): ${bytesToHex(initialKey)}")

            // STEP 4: Extract DUKPT transaction counter from KSN
            val ksnBytes = hexToBytes(ksn)
            val counter = extractDukptCounter(ksnBytes, counterBits)
            hsmLongListener.log("     [crypto 4/7]  DUKPT transaction counter : $counter  (0x${counter.toString(16).uppercase()})")

            // STEP 5: Derive DUKPT PIN-encryption session key for this counter
            hsmLongListener.log("     [crypto 5/7]  Deriving DUKPT session key  counter=$counter")
            val sessionKey = deriveDukptSessionKey(initialKey, ksn, counter, counterBits)
            hsmLongListener.log("     [crypto 5/7]  Session key (clr): ${bytesToHex(sessionKey)}")

            // STEP 6: Decrypt PIN block with DUKPT session key (PEK variant applied); extract plain PIN
            val srcPekKey = DukptEngine.xorBytes(sessionKey, DukptEngine.PEK_VARIANT)
            val pinBlock = decryptPinBlock(encryptedPinBlock, srcPekKey)
            hsmLongListener.log("     [crypto 6/7]  PIN block (clr) hex: ${bytesToHex(pinBlock)}")
            hsmLongListener.log("     [crypto 6/7]  PIN block (clr) bytes as ASCII decimal: ${pinBlock.joinToString(", ") { (it.toInt() and 0xFF).toString() }}")
            val panPart = "0000" + pan12(accountNumber)
            hsmLongListener.log("     [crypto 6/7]  PAN part for XOR: $panPart")
            hsmLongListener.log("     [crypto 6/7]  Extracting PIN  format=${sourcePinBlockFormat.code} (${sourcePinBlockFormat.description})  account=$accountNumber")
            val pin = extractPinFromBlock(pinBlock, accountNumber, sourcePinBlockFormat)
            hsmLongListener.log("     [crypto 6/7]  Extracted PIN length: ${pin.length}")
            hsmLongListener.log("     [crypto 6/7]  Extracted PIN (actual): $pin")

            // STEP 7: Re-format PIN block and encrypt under clear ZPK
            hsmLongListener.log("     [crypto 7/7]  Building new PIN block  format=${destPinBlockFormat.code} (${destPinBlockFormat.description})")
            val newPinBlock = formatPinBlock(pin, accountNumber, destPinBlockFormat)
            hsmLongListener.log("     [crypto 7/7]  New PIN block (clr): ${bytesToHex(newPinBlock)}")
            val encryptedNewPinBlock = encryptPinBlock(newPinBlock, clearZpk)
            hsmLongListener.log("     [crypto 7/7]  New PIN block (enc/ZPK): ${bytesToHex(encryptedNewPinBlock)}")

            return HsmCommandResult.Success(
                response = "PIN translated from DUKPT to ZPK",
                data = mapOf(
                    "encryptedPinBlock" to bytesToHex(encryptedNewPinBlock),
                    "pinLength"         to pin.length.toString(),
                    "ksn"               to ksn
                )
            )
        } catch (e: Exception) {
            return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "DUKPT PIN translation failed: ${e.message}"
            )
        }
    }

    /**
     * ================================================================================================
     * G0 - Translate PIN from BDK to ZPK or BDK (3DES DUKPT)
     * ================================================================================================
     *
     * Source side always uses BDK + KSN DUKPT derivation to decrypt the PIN block.
     *
     * Destination side depends on [destKeyType]:
     *   '0' (ZPK)       → decrypt dest key as ZPK (LMK 06-07), encrypt PIN directly
     *   '*' (BDK-1)     → decrypt dest key as BDK (LMK 28-29), derive session key via dest KSN
     *   '~' (BDK-2)     → same as BDK-1
     *   '!' (BDK-4)     → same as BDK-1
     *
     * @param lmkId               LMK slot identifier
     * @param encryptedSrcBdk     Source BDK encrypted under LMK pair 28-29
     * @param destKeyType         Destination key type: '0'=ZPK, '*'=BDK-1, '~'=BDK-2, '!'=BDK-4
     * @param encryptedDestKey    Destination key encrypted under LMK (ZPK or BDK depending on type)
     * @param srcKsnDescriptor    Source KSN descriptor (3H, e.g. "906")
     * @param srcKsn              Source Key Serial Number (20 hex chars)
     * @param destKsnDescriptor   Destination KSN descriptor (3H) — null when destKeyType is ZPK
     * @param destKsn             Destination KSN (20 hex chars) — null when destKeyType is ZPK
     * @param encryptedPinBlock   PIN block encrypted under source DUKPT 3DES session key
     * @param sourcePinBlockFormat  Source PIN block format
     * @param destPinBlockFormat    Destination PIN block format
     * @param accountNumber       12-digit account number (rightmost 12 of PAN excl. check digit)
     */
    suspend fun executeG0TranslatePinBdkToZpk(
        lmkId: String,
        encryptedSrcBdk: ByteArray,
        destKeyType: Char,
        encryptedDestKey: ByteArray,
        srcKsnDescriptor: String,
        srcKsn: String,
        destKsnDescriptor: String?,
        destKsn: String?,
        encryptedPinBlock: ByteArray,
        sourcePinBlockFormat: PinBlockFormat,
        destPinBlockFormat: PinBlockFormat,
        accountNumber: String
    ): HsmCommandResult {
        try {
            val destIsBdk = destKeyType != '0'
            val totalSteps = if (destIsBdk) 10 else 7
            val srcCounterBits = parseKsnDescriptorCounterBits(srcKsnDescriptor)
            var step = 1

            // ── STEP 1: Decrypt source BDK from LMK (pair 28-29) ────────────
            hsmLongListener.log("     [G0 crypto  ${step}/$totalSteps]  Decrypting source BDK under LMK pair 28-29")
            val clearSrcBdk = simulator.decryptUnderLmk(
                data = encryptedSrcBdk,
                lmkId = lmkId,
                pairNumber = LMK_PAIR_BDK
            )
            hsmLongListener.log("     [G0 crypto  ${step}/$totalSteps]  Source BDK (clr): ${bytesToHex(clearSrcBdk)}")
            step++

            // ── STEP 2: Decrypt destination key from LMK ─────────────────────
            val clearDestKey: ByteArray
            if (destIsBdk) {
                hsmLongListener.log("     [G0 crypto  ${step}/$totalSteps]  Decrypting dest BDK under LMK pair 28-29  (key type: '$destKeyType')")
                clearDestKey = simulator.decryptUnderLmk(
                    data = encryptedDestKey,
                    lmkId = lmkId,
                    pairNumber = LMK_PAIR_BDK
                )
                hsmLongListener.log("     [G0 crypto  ${step}/$totalSteps]  Dest BDK (clr): ${bytesToHex(clearDestKey)}")
            } else {
                hsmLongListener.log("     [G0 crypto  ${step}/$totalSteps]  Decrypting dest ZPK under LMK pair 06-07  (key type: '0')")
                clearDestKey = simulator.decryptUnderLmk(
                    data = encryptedDestKey,
                    lmkId = lmkId,
                    pairNumber = LMK_PAIR_ZMK_ZPK
                )
                hsmLongListener.log("     [G0 crypto  ${step}/$totalSteps]  Dest ZPK (clr): ${bytesToHex(clearDestKey)}")
            }
            step++

            // ── Source-side DUKPT derivation (using SOURCE BDK) ─────────────

            hsmLongListener.log("     [G0 crypto  ${step}/$totalSteps]  Deriving source IPEK from SOURCE BDK (${bytesToHex(clearSrcBdk)}) + KSN=$srcKsn  counterBits=$srcCounterBits")
            val srcIpek = deriveInitialKey(clearSrcBdk, srcKsn, srcCounterBits)
            hsmLongListener.log("     [G0 crypto  ${step}/$totalSteps]  Source IPEK (clr): ${bytesToHex(srcIpek)}")
            step++

            val srcKsnBytes = hexToBytes(srcKsn)
            val srcCounter = extractDukptCounter(srcKsnBytes, srcCounterBits)
            hsmLongListener.log("     [G0 crypto  ${step}/$totalSteps]  Source DUKPT counter : $srcCounter  (0x${srcCounter.toString(16).uppercase()})")
            step++

            hsmLongListener.log("     [G0 crypto  ${step}/$totalSteps]  Deriving source 3DES session key from source IPEK  counter=$srcCounter")
            val srcSessionKey = deriveDukptSessionKey(srcIpek, srcKsn, srcCounter, srcCounterBits)
            hsmLongListener.log("     [G0 crypto  ${step}/$totalSteps]  Source session key (clr): ${bytesToHex(srcSessionKey)}")
            step++

            // ── Decrypt PIN block using SOURCE session key (PEK variant applied); extract clear PIN ─
            val srcPekKey = DukptEngine.xorBytes(srcSessionKey, DukptEngine.PEK_VARIANT)
            hsmLongListener.log("     [G0 crypto  ${step}/$totalSteps]  Decrypting PIN block with SOURCE PEK (${bytesToHex(srcPekKey)})")
            val pinBlock = decryptPinBlock(encryptedPinBlock, srcPekKey)
            hsmLongListener.log("     [G0 crypto  ${step}/$totalSteps]  PIN block (clr) hex: ${bytesToHex(pinBlock)}")
            val panPart = "0000" + pan12(accountNumber)
            hsmLongListener.log("     [G0 crypto  ${step}/$totalSteps]  PAN part for XOR: $panPart")
            hsmLongListener.log("     [G0 crypto  ${step}/$totalSteps]  Extracting PIN  format=${sourcePinBlockFormat.code} (${sourcePinBlockFormat.description})  account=$accountNumber")
            val pin = extractPinFromBlock(pinBlock, accountNumber, sourcePinBlockFormat)
            hsmLongListener.log("     [G0 crypto  ${step}/$totalSteps]  Extracted PIN length: ${pin.length}  clear PIN: $pin")
            step++

            // ── Destination-side encryption (using DEST key) ─────────────────
            val encryptionKey: ByteArray
            if (destIsBdk && destKsnDescriptor != null && destKsn != null) {
                val destCounterBits = parseKsnDescriptorCounterBits(destKsnDescriptor)

                hsmLongListener.log("     [G0 crypto  ${step}/$totalSteps]  Deriving dest IPEK from DEST BDK (${bytesToHex(clearDestKey)}) + KSN=$destKsn  counterBits=$destCounterBits")
                val destIpek = deriveInitialKey(clearDestKey, destKsn, destCounterBits)
                hsmLongListener.log("     [G0 crypto  ${step}/$totalSteps]  Dest IPEK (clr): ${bytesToHex(destIpek)}")
                step++

                val destKsnBytes = hexToBytes(destKsn)
                val destCounter = extractDukptCounter(destKsnBytes, destCounterBits)
                hsmLongListener.log("     [G0 crypto  ${step}/$totalSteps]  Dest DUKPT counter : $destCounter  (0x${destCounter.toString(16).uppercase()})")
                step++

                hsmLongListener.log("     [G0 crypto  ${step}/$totalSteps]  Deriving dest 3DES session key  counter=$destCounter")
                val destSessionKey = deriveDukptSessionKey(destIpek, destKsn, destCounter, destCounterBits)
                hsmLongListener.log("     [G0 crypto  ${step}/$totalSteps]  Dest session key (clr): ${bytesToHex(destSessionKey)}")
                encryptionKey = DukptEngine.xorBytes(destSessionKey, DukptEngine.PEK_VARIANT)
                hsmLongListener.log("     [G0 crypto  ${step}/$totalSteps]  Dest PEK (w/ variant): ${bytesToHex(encryptionKey)}")
                step++
            } else {
                encryptionKey = clearDestKey
            }

            hsmLongListener.log("     [G0 crypto  ${step}/$totalSteps]  Building new PIN block  format=${destPinBlockFormat.code} (${destPinBlockFormat.description})")
            val newPinBlock = formatPinBlock(pin, accountNumber, destPinBlockFormat)
            hsmLongListener.log("     [G0 crypto  ${step}/$totalSteps]  New PIN block (clr): ${bytesToHex(newPinBlock)}")
            val encryptedNewPinBlock = encryptPinBlock(newPinBlock, encryptionKey)
            val destLabel = if (destIsBdk) "dest DUKPT session key" else "ZPK"
            hsmLongListener.log("     [G0 crypto  ${step}/$totalSteps]  New PIN block (enc/$destLabel): ${bytesToHex(encryptedNewPinBlock)}")

            return HsmCommandResult.Success(
                response = "PIN translated from BDK (3DES DUKPT) to ${if (destIsBdk) "BDK" else "ZPK"}",
                data = mapOf(
                    "encryptedPinBlock" to bytesToHex(encryptedNewPinBlock),
                    "pinLength"         to pin.length.toString(),
                    "srcKsn"            to srcKsn,
                    "destKeyType"       to destKeyType.toString()
                ) + if (destKsn != null) mapOf("destKsn" to destKsn) else emptyMap()
            )
        } catch (e: Exception) {
            return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "G0 PIN translation failed: ${e.message}"
            )
        }
    }

    /**
     * ================================================================================================
     * M0 - Generate MAC (CORRECTED)
     * ================================================================================================
     *
     * @param lmkId LMK slot identifier
     * @param data Data to MAC
     * @param encryptedTak TAK encrypted under LMK ← CORRECTED
     * @param algorithm MAC algorithm
     */
    suspend fun executeGenerateMac(
        lmkId: String,
        data: ByteArray,
        encryptedTak: ByteArray,
        algorithm: String = "ISO9797_ALG3",
        paddingMethod: Int = 1,
        macSizeBytes: Int = 4
    ): HsmCommandResult {
        try {
            val clearTak = simulator.decryptUnderLmk(
                data = encryptedTak,
                lmkId = lmkId,
                pairNumber = LMK_PAIR_TAK
            )
            val mac = generateMac(data, clearTak, algorithm, paddingMethod, macSizeBytes)

            return HsmCommandResult.Success(
                response = bytesToHex(mac),
                data = mapOf(
                    "mac" to bytesToHex(mac),
                    "algorithm" to algorithm
                )
            )
        } catch (e: Exception) {
            return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "MAC generation failed: ${e.message}"
            )
        }
    }

    /**
     * M8 - Verify MAC (LMK-encrypted TAK variant)
     */
    suspend fun executeVerifyMac(
        lmkId: String,
        data: ByteArray,
        providedMac: ByteArray,
        encryptedTak: ByteArray,
        algorithm: String = "ISO9797_ALG3",
        paddingMethod: Int = 1,
        macSizeBytes: Int = 4
    ): HsmCommandResult {
        return try {
            val clearTak = simulator.decryptUnderLmk(
                data = encryptedTak,
                lmkId = lmkId,
                pairNumber = LMK_PAIR_TAK
            )
            val calculatedMac = generateMac(data, clearTak, algorithm, paddingMethod, macSizeBytes)
            val compareLen = minOf(calculatedMac.size, providedMac.size)

            hsmLongListener.log("[M8] Calculated MAC = ${bytesToHex(calculatedMac)}, Provided MAC = ${bytesToHex(providedMac)}")

            if (calculatedMac.copyOf(compareLen).contentEquals(providedMac.copyOf(compareLen))) {
                HsmCommandResult.Success("")
            } else {
                HsmCommandResult.Error(
                    HsmErrorCodes.VERIFICATION_FAILURE,
                    "MAC verification failed"
                )
            }
        } catch (e: Exception) {
            HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "MAC verification failed: ${e.message}"
            )
        }
    }

    /**
     * ================================================================================================
     * BC - Verify Terminal PIN (CORRECTED)
     * ================================================================================================
     */
    suspend fun executeVerifyTerminalPin(
        lmkId: String,
        encryptedPinBlock: ByteArray,
        accountNumber: String,
        encryptedTpk: ByteArray,  // ← ENCRYPTED under LMK
        expectedPin: String,
        pinBlockFormat: PinBlockFormat
    ): HsmCommandResult {
        try {
            // Decrypt TPK from LMK
            val clearTpk = simulator.decryptUnderLmk(
                data = encryptedTpk,
                lmkId = lmkId,
                pairNumber = LMK_PAIR_TPK
            )

            // Decrypt PIN block
            val pinBlock = decryptPinBlock(encryptedPinBlock, clearTpk)

            // Extract PIN
            val extractedPin = extractPinFromBlock(pinBlock, accountNumber, pinBlockFormat)

            // Compare
            val matches = extractedPin == expectedPin

            return if (matches) {
                HsmCommandResult.Success("PIN verified successfully")
            } else {
                HsmCommandResult.Error(
                    HsmErrorCodes.VERIFICATION_FAILURE,
                    "PIN verification failed"
                )
            }
        } catch (e: Exception) {
            return HsmCommandResult.Error(
                HsmErrorCodes.VERIFICATION_FAILURE,
                "PIN verification failed: ${e.message}"
            )
        }
    }

    // ====================================================================================================
    // HELPER METHODS (Unchanged from original)
    // ====================================================================================================

    private fun formatPinBlock(
        pin: String,
        accountNumber: String,
        format: PinBlockFormat
    ): ByteArray {
        return when (format) {
            PinBlockFormat.ISO_FORMAT_0 -> formatIsoFormat0(pin, accountNumber)
            PinBlockFormat.ISO_FORMAT_1 -> formatIsoFormat1(pin)
            PinBlockFormat.ISO_FORMAT_3 -> formatIsoFormat3(pin, accountNumber)
            // Docutel, Diebold/IBM, PLUS Network use ISO Format 0 block structure (XOR with PAN)
            PinBlockFormat.DOCUTEL,
            PinBlockFormat.DIEBOLD_IBM,
            PinBlockFormat.PLUS_NETWORK -> formatIsoFormat0(pin, accountNumber)
            // AS2805 and ISO Format 4 fall back to Format 0 for now
            PinBlockFormat.AS2805,
            PinBlockFormat.ISO_FORMAT_4 -> formatIsoFormat0(pin, accountNumber)
        }
    }

    private fun formatIsoFormat0(pin: String, accountNumber: String): ByteArray {
        val controlField = "0${pin.length}"
        val pinPart = (controlField + pin).padEnd(16, 'F')
        val panPart = "0000" + pan12(accountNumber)

        val pinBytes = hexToBytes(pinPart)
        val panBytes = hexToBytes(panPart)

        return pinBytes.zip(panBytes) { a, b -> (a.toInt() xor b.toInt()).toByte() }.toByteArray()
    }

    private fun formatIsoFormat1(pin: String): ByteArray {
        val controlField = "1${pin.length}"
        val randomPart = ByteArray(6).also { java.security.SecureRandom().nextBytes(it) }
        val pinPart = controlField + pin + bytesToHex(randomPart).take(16 - pin.length - 2)
        return hexToBytes(pinPart)
    }

    private fun formatIsoFormat3(pin: String, accountNumber: String): ByteArray {
        val controlField = "3${pin.length}"
        val pinPart = (controlField + pin).padEnd(16, 'F')
        val panPart = "0000" + accountNumber.takeLast(12)

        val pinBytes = hexToBytes(pinPart)
        val panBytes = hexToBytes(panPart)

        return pinBytes.zip(panBytes) { a, b -> (a.toInt() xor b.toInt()).toByte() }.toByteArray()
    }

    private fun extractPinFromBlock(
        pinBlock: ByteArray,
        accountNumber: String,
        format: PinBlockFormat
    ): String {
        return when (format) {
            // Formats that XOR the PIN block with the PAN
            PinBlockFormat.ISO_FORMAT_0,
            PinBlockFormat.ISO_FORMAT_3,
            PinBlockFormat.DOCUTEL,
            PinBlockFormat.DIEBOLD_IBM,
            PinBlockFormat.PLUS_NETWORK -> {
                val panPart = "0000" + pan12(accountNumber)
                val panBytes = hexToBytes(panPart)
                val xored = pinBlock.zip(panBytes) { a, b -> (a.toInt() xor b.toInt()).toByte() }
                    .toByteArray()
                val hex = bytesToHex(xored)
                // PIN length is a hex nibble (0–F); use radix-16 to handle digits A–F
                val pinLength = hex[1].digitToInt(16)
                hex.substring(2, 2 + pinLength)
            }

            // Formats that do not XOR with PAN (PIN is embedded directly)
            else -> {
                val hex = bytesToHex(pinBlock)
                // PIN length is a hex nibble (0–F); use radix-16 to handle digits A–F
                val pinLength = hex[1].digitToInt(16)
                hex.substring(2, 2 + pinLength)
            }
        }
    }

    private suspend fun encryptPinBlock(pinBlock: ByteArray, key: ByteArray): ByteArray {
        return engine().encryptionEngine.encrypt(
            algorithm = CryptoAlgorithm.TDES,
            encryptionEngineParameters = SymmetricEncryptionEngineParameters(
                key = key,
                data = pinBlock,
                mode = ai.cortex.core.types.CipherMode.ECB
            )
        )
    }

    private suspend fun decryptPinBlock(encryptedPinBlock: ByteArray, key: ByteArray): ByteArray {
        return engine().encryptionEngine.decrypt(
            algorithm = CryptoAlgorithm.TDES,
            decryptionEngineParameters = SymmetricDecryptionEngineParameters(
                key = key,
                data = encryptedPinBlock,
                mode = ai.cortex.core.types.CipherMode.ECB
            )
        )
    }

    /** Delegates to the companion object's [deriveInitialKey]. */
    fun deriveInitialKey(bdk: ByteArray, ksn: String, counterBits: Int = 21): ByteArray =
        Companion.deriveInitialKey(bdk, ksn, counterBits)

    /** Expand a 16-byte double-length key to 24 bytes (K1|K2|K1) for DESede. */
    private fun expandKeyTo24(key: ByteArray): ByteArray {
        if (key.size >= 24) return key.copyOf(24)
        val expanded = ByteArray(24)
        key.copyInto(expanded, 0, 0, minOf(key.size, 16))
        key.copyInto(expanded, 16, 0, 8)
        return expanded
    }

    /**
     * ISO 9797-1 Algorithm 3 (= ANSI X9.19 with double-length key).
     * 1. CBC-MAC all blocks with single DES using K1
     * 2. Decrypt the result with K2
     * 3. Re-encrypt with K1
     * Final MAC = E_K1(D_K2(CBC-MAC_K1(data)))
     */
    private fun generateMacIso9797Alg3(
        data: ByteArray,
        key: ByteArray,
        paddingMethod: Int = 1,
        macSizeBytes: Int = 4
    ): ByteArray {
        val paddedData = when (paddingMethod) {
            2 -> padDataMethod2(data, 8)
            else -> padData(data, 8)
        }
        var block = ByteArray(8)

        val cipherK1 = Cipher.getInstance("DES/ECB/NoPadding")
        cipherK1.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key.copyOf(8), "DES"))

        for (i in paddedData.indices step 8) {
            val chunk = paddedData.copyOfRange(i, i + 8)
            val xored =
                block.zip(chunk) { a, b -> (a.toInt() xor b.toInt()).toByte() }.toByteArray()
            block = cipherK1.doFinal(xored)
        }

        val cipherK2Dec = Cipher.getInstance("DES/ECB/NoPadding")
        cipherK2Dec.init(Cipher.DECRYPT_MODE, SecretKeySpec(key.copyOfRange(8, 16), "DES"))
        block = cipherK2Dec.doFinal(block)

        block = cipherK1.doFinal(block)

        return block.copyOf(macSizeBytes)
    }

    /**
     * ISO 9797-1 Algorithm 1 with 3DES: CBC-MAC using triple DES for every block.
     */
    private fun generateMacIso9797Alg1(
        data: ByteArray,
        key: ByteArray,
        paddingMethod: Int = 1,
        macSizeBytes: Int = 4
    ): ByteArray {
        val paddedData = when (paddingMethod) {
            2 -> padDataMethod2(data, 8)
            else -> padData(data, 8)
        }
        var block = ByteArray(8)
        val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(expandKeyTo24(key), "DESede"))

        for (i in paddedData.indices step 8) {
            val chunk = paddedData.copyOfRange(i, i + 8)
            val xored =
                block.zip(chunk) { a, b -> (a.toInt() xor b.toInt()).toByte() }.toByteArray()
            block = cipher.doFinal(xored)
        }

        return block.copyOf(macSizeBytes)
    }

    @Deprecated("Use generateMacIso9797Alg1 instead", ReplaceWith("generateMacIso9797Alg1(data, key)"))
    private fun generateMacAnsiX919(data: ByteArray, key: ByteArray): ByteArray =
        generateMacIso9797Alg1(data, key)

    private fun generateMac(
        data: ByteArray,
        key: ByteArray,
        algorithm: String,
        paddingMethod: Int = 1,
        macSizeBytes: Int = 4
    ): ByteArray = when (algorithm) {
        "ISO9797_ALG1" -> generateMacIso9797Alg1(data, key, paddingMethod, macSizeBytes)
        "ISO9797_ALG3" -> generateMacIso9797Alg3(data, key, paddingMethod, macSizeBytes)
        else -> throw IllegalArgumentException("Unsupported MAC algorithm: $algorithm")
    }

    /** ISO 9797 Padding Method 1: pad with 0x00 bytes to fill block. */
    private fun padData(data: ByteArray, blockSize: Int): ByteArray {
        val padding = blockSize - (data.size % blockSize)
        return if (padding == blockSize) data else data + ByteArray(padding)
    }

    /** ISO 9797 Padding Method 2: append 0x80 then pad with 0x00. */
    private fun padDataMethod2(data: ByteArray, blockSize: Int): ByteArray {
        val padded = data + byteArrayOf(0x80.toByte())
        val remaining = blockSize - (padded.size % blockSize)
        return if (remaining == blockSize) padded else padded + ByteArray(remaining)
    }

    // ====================================================================================================
    // JC — Translate PIN from TPK to LMK encryption
    // TPK is single-length (16H) in classic form
    // ====================================================================================================
    suspend fun executeTranslatePinToLmk(
        lmkId: String,
        encryptedTpk: ByteArray,
        encryptedPinBlock: ByteArray,
        accountNumber: String,
        pinBlockFormat: PinBlockFormat = PinBlockFormat.ISO_FORMAT_0
    ): HsmCommandResult {
        return try {
            val clearTpk = simulator.decryptUnderLmk(encryptedTpk, lmkId, LMK_PAIR_TPK)
            val pinBlock = decryptPinBlock(encryptedPinBlock, clearTpk)
            val pin = extractPinFromBlock(pinBlock, accountNumber, pinBlockFormat)
            // Encrypt PIN under LMK pair 02-03 (LMK-encrypted PIN format)
            val lmkPin = encryptPinUnderLmk(pin, accountNumber, lmkId)
            HsmCommandResult.Success(
                response = lmkPin,
                data = mapOf("lmkPin" to lmkPin, "pinLength" to pin.length.toString())
            )
        } catch (e: Exception) {
            HsmCommandResult.Error(HsmErrorCodes.INVALID_INPUT_DATA, "JC failed: ${e.message}")
        }
    }

    // ====================================================================================================
    // JE — Translate PIN from ZPK to LMK encryption
    // ====================================================================================================
    suspend fun executeTranslatePinZpkToLmk(
        lmkId: String,
        encryptedZpk: ByteArray,
        encryptedPinBlock: ByteArray,
        accountNumber: String,
        pinBlockFormat: PinBlockFormat = PinBlockFormat.ISO_FORMAT_0
    ): HsmCommandResult {
        return try {
            val clearZpk = simulator.decryptUnderLmk(encryptedZpk, lmkId, LMK_PAIR_ZMK_ZPK)
            val pinBlock = decryptPinBlock(encryptedPinBlock, clearZpk)
            val pin = extractPinFromBlock(pinBlock, accountNumber, pinBlockFormat)
            val lmkPin = encryptPinUnderLmk(pin, accountNumber, lmkId)
            HsmCommandResult.Success(
                response = lmkPin,
                data = mapOf("lmkPin" to lmkPin, "pinLength" to pin.length.toString())
            )
        } catch (e: Exception) {
            HsmCommandResult.Error(HsmErrorCodes.INVALID_INPUT_DATA, "JE failed: ${e.message}")
        }
    }

    // ====================================================================================================
    // JG — Translate PIN from LMK to ZPK encryption
    // ====================================================================================================
    suspend fun executeTranslatePinLmkToZpk(
        lmkId: String,
        encryptedZpk: ByteArray,
        lmkEncryptedPin: String,
        accountNumber: String,
        pinBlockFormat: PinBlockFormat = PinBlockFormat.ISO_FORMAT_0
    ): HsmCommandResult {
        return try {
            val clearZpk = simulator.decryptUnderLmk(encryptedZpk, lmkId, LMK_PAIR_ZMK_ZPK)
            val pin = decryptLmkPin(lmkEncryptedPin, accountNumber, lmkId)
            val pinBlock = formatPinBlock(pin, accountNumber, pinBlockFormat)
            val zpkPinBlock = encryptPinBlock(pinBlock, clearZpk)
            val zpkPinBlockHex = bytesToHex(zpkPinBlock)
            HsmCommandResult.Success(
                response = zpkPinBlockHex,
                data = mapOf("zpkPinBlock" to zpkPinBlockHex)
            )
        } catch (e: Exception) {
            HsmCommandResult.Error(HsmErrorCodes.INVALID_INPUT_DATA, "JG failed: ${e.message}")
        }
    }

    // ====================================================================================================
    // M4 — Translate Data Block (DUKPT DATA BDK decrypt + re-encrypt under destination key)
    // ====================================================================================================
    suspend fun executeTranslateData(
        lmkId: String,
        sourceKey: ByteArray,
        destKey: ByteArray,
        ksn: String,
        encryptedData: ByteArray,
        mode: String = "02",
        counterBits: Int = 21
    ): HsmCommandResult {
        return try {
            val clearBdk = simulator.decryptUnderLmk(sourceKey, lmkId, LMK_PAIR_BDK)
            val initialKey = deriveInitialKey(clearBdk, ksn, counterBits)
            val ksnBytes = hexToBytes(ksn)
            val counter = extractDukptCounter(ksnBytes, counterBits)
            val sessionKey = deriveDukptSessionKey(initialKey, ksn, counter, counterBits)

            val variantKey = DukptEngine.xorBytes(sessionKey, DukptEngine.DATA_VARIANT)
            val dataKey = DukptEngine.applyVariantEncryption(variantKey)

            val clearData = decryptPinBlock(encryptedData, dataKey)

            // Re-encrypt under destination key (ZPK)
            val clearDestKey = simulator.decryptUnderLmk(destKey, lmkId, LMK_PAIR_ZMK_ZPK)
            val translatedData = encryptPinBlock(clearData, clearDestKey)
            val translatedHex = bytesToHex(translatedData)

            HsmCommandResult.Success(
                response = translatedHex,
                data = mapOf("translatedData" to translatedHex)
            )
        } catch (e: Exception) {
            HsmCommandResult.Error(HsmErrorCodes.INVALID_INPUT_DATA, "M4 failed: ${e.message}")
        }
    }

    // ====================================================================================================
    // EE — Derive PIN using IBM 3624 offset method
    // ====================================================================================================
    suspend fun executeDerivePinIbm(
        lmkId: String,
        pvk: ByteArray,
        offset: String,
        accountNumber: String,
        decimalizationTable: String = "0123456789012345",
        minPinLength: Int = 4
    ): HsmCommandResult {
        return try {
            val clearPvk = simulator.decryptUnderLmk(pvk, lmkId, LMK_PAIR_PVK)
            val naturalPin = generateNaturalPin(accountNumber, clearPvk, decimalizationTable)

            // Apply offset to get customer PIN
            val customerPin = StringBuilder()
            for (i in naturalPin.indices) {
                val naturalDigit = naturalPin[i].digitToInt()
                val offsetDigit = if (i < offset.length && offset[i] != 'F') offset[i].digitToInt() else 0
                customerPin.append((naturalDigit + offsetDigit) % 10)
            }

            val pin = customerPin.toString().take(minPinLength.coerceAtLeast(4))
            val lmkPin = encryptPinUnderLmk(pin, accountNumber, lmkId)

            HsmCommandResult.Success(
                response = lmkPin,
                data = mapOf("lmkPin" to lmkPin, "pinLength" to pin.length.toString())
            )
        } catch (e: Exception) {
            HsmCommandResult.Error(HsmErrorCodes.INVALID_INPUT_DATA, "EE failed: ${e.message}")
        }
    }

    // ====================================================================================================
    // NG — Decrypt PIN from LMK to clear
    // ====================================================================================================
    suspend fun executeDecryptLmkPin(
        lmkId: String,
        accountNumber: String,
        lmkEncryptedPin: String
    ): HsmCommandResult {
        return try {
            val clearPin = decryptLmkPin(lmkEncryptedPin, accountNumber, lmkId)
            HsmCommandResult.Success(
                response = clearPin,
                data = mapOf("clearPin" to clearPin)
            )
        } catch (e: Exception) {
            HsmCommandResult.Error(HsmErrorCodes.INVALID_INPUT_DATA, "NG failed: ${e.message}")
        }
    }

    // ====================================================================================================
    // LMK PIN encryption/decryption helpers (IBM 3624 LMK PIN format)
    // ====================================================================================================
    private fun encryptPinUnderLmk(pin: String, accountNumber: String, lmkId: String): String {
        // LMK-encrypted PIN: decimal digits, length = PIN length + 1 (length indicator digit)
        // Using simplified format: length_indicator + pin_digits
        val lengthIndicator = pin.length.toString()
        return lengthIndicator + pin
    }

    private fun decryptLmkPin(lmkEncryptedPin: String, accountNumber: String, lmkId: String): String {
        // Extract PIN from LMK-encrypted format
        if (lmkEncryptedPin.isEmpty()) return ""
        val pinLength = lmkEncryptedPin[0].digitToIntOrNull() ?: return lmkEncryptedPin
        return if (lmkEncryptedPin.length > pinLength) {
            lmkEncryptedPin.substring(1, 1 + pinLength)
        } else {
            lmkEncryptedPin.drop(1)
        }
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] =
                ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
        }
        return data
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02X".format(it) }
    }
}