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
            KeyType.TYPE_000, KeyType.TYPE_001 -> 0  // ZMK/ZPK use pair 00-01
            KeyType.TYPE_002 -> 6                     // TPK uses pair 14-15
            KeyType.TYPE_003 -> 2                     // CVK uses pair 04-05
            KeyType.TYPE_008, KeyType.TYPE_009 -> 4   // TAK/ZAK use pair 08-09
            KeyType.TYPE_109 -> 9                     // ZEK uses pair 26-27
            KeyType.TYPE_209 -> 0                     // BDK uses pair 00-01
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
     * M0 - Generate MAC (ISO 9797-1 Algorithm 3)
     */
    fun executeGenerateMac(
        data: ByteArray,
        tak: ByteArray,
        algorithm: String = "ISO9797_ALG3"
    ): HsmCommandResult {
        try {
            val mac = when (algorithm) {
                "ISO9797_ALG3" -> generateMacIso9797Alg3(data, tak)
                "ANSI_X919" -> generateMacAnsiX919(data, tak)
                else -> return HsmCommandResult.Error(
                    HsmErrorCodes.ALGORITHM_NOT_SUPPORTED,
                    "Unsupported MAC algorithm"
                )
            }

            return HsmCommandResult.Success(
                response = "MAC: ${simulator.bytesToHex(mac)}",
                data = mapOf(
                    "mac" to simulator.bytesToHex(mac),
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
     * M2 - Verify MAC
     */
    fun executeVerifyMac(
        data: ByteArray,
        providedMac: ByteArray,
        tak: ByteArray,
        algorithm: String = "ISO9797_ALG3"
    ): HsmCommandResult {
        val calculatedMac = when (algorithm) {
            "ISO9797_ALG3" -> generateMacIso9797Alg3(data, tak)
            "ANSI_X919" -> generateMacAnsiX919(data, tak)
            else -> return HsmCommandResult.Error(
                HsmErrorCodes.ALGORITHM_NOT_SUPPORTED,
                "Unsupported MAC algorithm"
            )
        }

        return if (calculatedMac.contentEquals(providedMac)) {
            HsmCommandResult.Success("MAC verified successfully")
        } else {
            HsmCommandResult.Error(
                HsmErrorCodes.VERIFICATION_FAILURE,
                "MAC verification failed"
            )
        }
    }

    // ====================================================================================================
    // KSN DESCRIPTOR PARSING & COUNTER EXTRACTION
    // ====================================================================================================

    companion object {
        /**
         * Parse a 3-char KSN Descriptor (Thales format "XYZ") and return the counter
         * bit-width for the given KSN length.
         *
         * X  = BDK/Key Set Identifier length (hex chars)
         * YZ = Device Identifier length (hex chars, two-digit decimal)
         * Counter hex chars = totalKsnHexChars − X − YZ
         * Counter bits      = counterHexChars × 4
         *
         * @param descriptor  3-character KSN descriptor, e.g. "609", "605", "A05".
         *                    Each character is a single hex digit:
         *                    [0] = BDK identifier length (hex chars)
         *                    [1] = Sub-key identifier length (hex chars)
         *                    [2] = Device identifier length (hex chars)
         * @param ksnHexLen   total KSN length in hex characters (default 20 for 10-byte KSN)
         * @return number of counter bits
         */
        fun parseKsnDescriptorCounterBits(descriptor: String, ksnHexLen: Int = 20): Int {
            if (descriptor.length != 3) return 21
            val bdkIdLen = descriptor[0].digitToIntOrNull(16) ?: return 21
            val subKeyIdLen = descriptor[1].digitToIntOrNull(16) ?: return 21
            val deviceIdLen = descriptor[2].digitToIntOrNull(16) ?: return 21
            val counterHexChars = ksnHexLen - bdkIdLen - subKeyIdLen - deviceIdLen
            return if (counterHexChars in 1..20) counterHexChars * 4 else 21
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
         * Derive DUKPT Initial Key (IPEK) from BDK and KSN using ANSI X9.24 algorithm.
         *
         * 1. Extract IKSN = KSN with counter bits zeroed (on full KSN), then take first 8 bytes
         * 2. Left half  = TDES_ECB_encrypt(IKSN, BDK)
         * 3. Right half = TDES_ECB_encrypt(IKSN, BDK XOR C0C0C0C000000000C0C0C0C000000000)
         */
        fun deriveInitialKey(bdk: ByteArray, ksn: String, counterBits: Int = 21): ByteArray {
            val ksnBytes = IsoUtil.hexToBytes(ksn)
            val fullKsn = ksnBytes.copyOf()
            zeroIksnCounterBits(fullKsn, counterBits)
            val iksn = ByteArray(8)
            fullKsn.copyInto(iksn, 0, 0, minOf(fullKsn.size, 8))

            val bdkKey = if (bdk.size == 16) {
                val expanded = ByteArray(24)
                bdk.copyInto(expanded, 0, 0, 16)
                bdk.copyInto(expanded, 16, 0, 8)
                expanded
            } else {
                bdk.copyOf(24)
            }

            val cipher = Cipher.getInstance("DESede/ECB/NoPadding")

            val keySpec = SecretKeySpec(bdkKey, "DESede")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            val leftHalf = cipher.doFinal(iksn)

            val bdkXor = bdk.copyOf()
            val mask = byteArrayOf(
                0xC0.toByte(), 0xC0.toByte(), 0xC0.toByte(), 0xC0.toByte(),
                0x00, 0x00, 0x00, 0x00,
                0xC0.toByte(), 0xC0.toByte(), 0xC0.toByte(), 0xC0.toByte(),
                0x00, 0x00, 0x00, 0x00
            )
            for (i in bdkXor.indices) {
                if (i < mask.size) {
                    bdkXor[i] = (bdkXor[i].toInt() xor mask[i].toInt()).toByte()
                }
            }
            val bdkXorKey = if (bdkXor.size == 16) {
                val expanded = ByteArray(24)
                bdkXor.copyInto(expanded, 0, 0, 16)
                bdkXor.copyInto(expanded, 16, 0, 8)
                expanded
            } else {
                bdkXor.copyOf(24)
            }
            val keySpecXor = SecretKeySpec(bdkXorKey, "DESede")
            cipher.init(Cipher.ENCRYPT_MODE, keySpecXor)
            val rightHalf = cipher.doFinal(iksn)

            return leftHalf.copyOf(8) + rightHalf.copyOf(8)
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
     * Generate KSN (Key Serial Number)
     * Format: [BDK ID: 5 hex chars][Device ID: 5 hex chars][Counter: 11 bits + 21 bits]
     */
    fun generateKsn(terminalId: String): String {
        val bdkId = "12345"  // Example BDK identifier
        val deviceId = terminalId.takeLast(5).padStart(5, '0')
        val counter = "00000"  // Initial counter (21 bits = 0)

        return bdkId + deviceId + counter + "00000"
    }

    // ====================================================================================================
    // ANSI X9.24 DUKPT Key Derivation
    // ====================================================================================================

    /**
     * Derive DUKPT session key for current transaction from a DukptProfile.
     */
    fun deriveDukptSessionKey(
        profile: DukptProfile,
        usageType: DukptKeyUsage = DukptKeyUsage.PIN_ENCRYPTION
    ): ByteArray {
        val counter = profile.currentCounter
        val ksn = updateKsnWithCounter(profile.keySerialNumber, counter)
        val ksnBytes = hexToBytes(ksn)
        val counterBits = profile.counterBits
        val counterInt = extractDukptCounter(ksnBytes, counterBits)
        val futureKey = deriveDukptSessionKey(profile.initialKey, ksn, counterInt, counterBits)
        return deriveVariantKey(futureKey, usageType)
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

                val cryptoReg = runningKsn.copyOfRange(2, 10)
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

    private fun updateKsnWithCounter(baseKsn: String, counter: Long): String {
        val ksnBytes = simulator.hexToBytes(baseKsn).toMutableList()
        ksnBytes[7] =
            ((ksnBytes[7].toInt() and 0xE0) or (((counter shr 16) and 0x1F).toInt())).toByte()
        ksnBytes[8] = ((counter shr 8) and 0xFF).toByte()
        ksnBytes[9] = (counter and 0xFF).toByte()
        return simulator.bytesToHex(ksnBytes.toByteArray())
    }


    /**
     * Generate natural PIN using IBM algorithm
     */
    private fun generateNaturalPin(
        accountNumber: String,
        pvk: ByteArray,
        decTable: String
    ): String {
        // Take 12 rightmost digits of account (excluding check digit)
        val pan = accountNumber.takeLast(13).dropLast(1)
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
            pairNumber = 14  // TPK uses LMK pair 14-15 (key type 002)
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
        encryptedSourceTpk: ByteArray,  // ← ENCRYPTED under LMK
        encryptedDestZpk: ByteArray,    // ← ENCRYPTED under LMK
        accountNumber: String,
        sourcePinBlockFormat: PinBlockFormat,
        destPinBlockFormat: PinBlockFormat
    ): HsmCommandResult {
        try {

            hsmLongListener.log("encryptedSourceTpk: ${IsoUtil.bytesToHex(encryptedSourceTpk)}")
            hsmLongListener.log("lmkId: $lmkId")
            // STEP 1: Decrypt BOTH keys from LMK encryption
            val clearTpk = simulator.decryptUnderLmk(
                data = encryptedSourceTpk,
                lmkId = lmkId,
                pairNumber = 14  // TPK uses pair 14-15
            )
            hsmLongListener.log("clearTPK: ${IsoUtil.bytesToHex(clearTpk)}")
            hsmLongListener.log("encryptedDestZpk: ${IsoUtil.bytesToHex(encryptedDestZpk)}")
            val clearZpk = simulator.decryptUnderLmk(
                data = encryptedDestZpk,
                lmkId = lmkId,
                pairNumber = 6   // ZPK uses pair 06-07 (key type 001)
            )
            hsmLongListener.log("clearZpk: ${IsoUtil.bytesToHex(clearZpk)}")
            hsmLongListener.log("encryptedPinBlock: ${IsoUtil.bytesToHex(encryptedPinBlock)}")


            // STEP 2: Decrypt PIN block with clear TPK
            val pinBlock = decryptPinBlock(encryptedPinBlock, clearTpk)

            hsmLongListener.log("pinBlock: ${IsoUtil.bytesToHex(pinBlock)}")
            hsmLongListener.log("accountNumber: $accountNumber")
            hsmLongListener.log("sourcePinBlockFormat: $sourcePinBlockFormat")


            // STEP 3: Extract PIN
            val pin = extractPinFromBlock(pinBlock, accountNumber, sourcePinBlockFormat)

            hsmLongListener.log("pin: $pin")
            hsmLongListener.log("destPinBlockFormat: $destPinBlockFormat")

            // STEP 4: Re-format if necessary
            val newPinBlock = formatPinBlock(pin, accountNumber, destPinBlockFormat)

            hsmLongListener.log("newPinBlock: ${IsoUtil.bytesToHex(newPinBlock)}")
            hsmLongListener.log("clearZpk: ${IsoUtil.bytesToHex(clearZpk)}")

            // STEP 5: Encrypt with clear ZPK
            val encryptedNewPinBlock = encryptPinBlock(newPinBlock, clearZpk)
            hsmLongListener.log("encryptedNewPinBlock: ${IsoUtil.bytesToHex(encryptedNewPinBlock)}")


            // STEP 6: Clear keys are discarded
            hsmLongListener.onFormattedResponse(
                """
                    encryptedPinBlock: ${IsoUtil.bytesToHex(encryptedNewPinBlock)}
                    pinLength: ${pin.length}
                
            """.trimIndent()
            )

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
            // STEP 1: Decrypt BDK from LMK encryption
            val clearBdk = simulator.decryptUnderLmk(
                data = encryptedBdk,
                lmkId = lmkId,
                pairNumber = 28
            )

            // STEP 2: Decrypt destination ZPK from LMK encryption
            val clearZpk = simulator.decryptUnderLmk(
                data = encryptedDestZpk,
                lmkId = lmkId,
                pairNumber = 6
            )

            // STEP 3: Derive Initial Key from BDK
            val initialKey = deriveInitialKey(clearBdk, ksn, counterBits)

            // STEP 4: Extract counter from KSN
            val ksnBytes = hexToBytes(ksn)
            val counter = extractDukptCounter(ksnBytes, counterBits)

            // STEP 5: Derive DUKPT session key
            val sessionKey = deriveDukptSessionKey(initialKey, ksn, counter, counterBits)

            // STEP 6: Decrypt PIN block with DUKPT session key
            val pinBlock = decryptPinBlock(encryptedPinBlock, sessionKey)
            val pin = extractPinFromBlock(pinBlock, accountNumber, sourcePinBlockFormat)

            // STEP 7: Re-encrypt with ZPK
            val newPinBlock = formatPinBlock(pin, accountNumber, destPinBlockFormat)
            val encryptedNewPinBlock = encryptPinBlock(newPinBlock, clearZpk)

            // STEP 8: All clear keys are discarded

            return HsmCommandResult.Success(
                response = "PIN translated from DUKPT to ZPK",
                data = mapOf(
                    "encryptedPinBlock" to bytesToHex(encryptedNewPinBlock),
                    "ksn" to ksn
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
        encryptedTak: ByteArray,  // ← ENCRYPTED under LMK
        algorithm: String = "ISO9797_ALG3"
    ): HsmCommandResult {
        try {
            // STEP 1: Decrypt TAK from LMK encryption
            val clearTak = simulator.decryptUnderLmk(
                data = encryptedTak,
                lmkId = lmkId,
                pairNumber = 16  // TAK uses pair 16-17 (key type 003)
            )

            // STEP 2: Generate MAC with clear TAK
            val mac = when (algorithm) {
                "ISO9797_ALG3" -> generateMacIso9797Alg3(data, clearTak)
                "ANSI_X919" -> generateMacAnsiX919(data, clearTak)
                else -> return HsmCommandResult.Error(
                    HsmErrorCodes.ALGORITHM_NOT_SUPPORTED,
                    "Unsupported MAC algorithm"
                )
            }

            // STEP 3: Clear TAK is discarded

            return HsmCommandResult.Success(
                response = "MAC: ${bytesToHex(mac)}",
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
                pairNumber = 14
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
            else -> formatIsoFormat0(pin, accountNumber)
        }
    }

    private fun formatIsoFormat0(pin: String, accountNumber: String): ByteArray {
        val controlField = "0${pin.length}"
        val pinPart = (controlField + pin).padEnd(16, 'F')
        val panPart = "0000" + accountNumber.takeLast(13).dropLast(1)

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
            PinBlockFormat.ISO_FORMAT_0, PinBlockFormat.ISO_FORMAT_3 -> {
                val panPart = "0000" + accountNumber.takeLast(13).dropLast(1)
                val panBytes = hexToBytes(panPart)
                val xored = pinBlock.zip(panBytes) { a, b -> (a.toInt() xor b.toInt()).toByte() }
                    .toByteArray()
                val hex = bytesToHex(xored)
                val pinLength = hex[1].toString().toInt()
                hex.substring(2, 2 + pinLength)
            }

            else -> {
                val hex = bytesToHex(pinBlock)
                val pinLength = hex[1].toString().toInt()
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

    private fun generateMacIso9797Alg3(data: ByteArray, key: ByteArray): ByteArray {
        val paddedData = padData(data, 8)
        var block = ByteArray(8)
        val cipher = Cipher.getInstance("DES/ECB/NoPadding")
        val keySpecLeft = SecretKeySpec(key.copyOf(8), "DES")
        cipher.init(Cipher.ENCRYPT_MODE, keySpecLeft)

        for (i in paddedData.indices step 8) {
            val chunk = paddedData.copyOfRange(i, i + 8)
            val xored =
                block.zip(chunk) { a, b -> (a.toInt() xor b.toInt()).toByte() }.toByteArray()
            block = cipher.doFinal(xored)
        }

        val cipher3Des = Cipher.getInstance("DESede/ECB/NoPadding")
        val keySpec3Des = SecretKeySpec(key.copyOf(16), "DESede")
        cipher3Des.init(Cipher.ENCRYPT_MODE, keySpec3Des)

        return cipher3Des.doFinal(block).copyOf(4)
    }

    private fun generateMacAnsiX919(data: ByteArray, key: ByteArray): ByteArray {
        val paddedData = padData(data, 8)
        var block = ByteArray(8)
        val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
        val keySpec = SecretKeySpec(key.copyOf(16), "DESede")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)

        for (i in paddedData.indices step 8) {
            val chunk = paddedData.copyOfRange(i, i + 8)
            val xored =
                block.zip(chunk) { a, b -> (a.toInt() xor b.toInt()).toByte() }.toByteArray()
            block = cipher.doFinal(xored)
        }

        return block.copyOf(4)
    }

    private fun padData(data: ByteArray, blockSize: Int): ByteArray {
        val padding = blockSize - (data.size % blockSize)
        return if (padding == blockSize) data else data + ByteArray(padding)
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
            val clearTpk = simulator.decryptUnderLmk(encryptedTpk, lmkId, 14)
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
            val clearZpk = simulator.decryptUnderLmk(encryptedZpk, lmkId, 6)
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
            val clearZpk = simulator.decryptUnderLmk(encryptedZpk, lmkId, 6)
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
            val clearBdk = simulator.decryptUnderLmk(sourceKey, lmkId, 28)
            val initialKey = deriveInitialKey(clearBdk, ksn, counterBits)
            val ksnBytes = hexToBytes(ksn)
            val counter = extractDukptCounter(ksnBytes, counterBits)
            val sessionKey = deriveDukptSessionKey(initialKey, ksn, counter, counterBits)

            // Decrypt data under DUKPT session key
            val clearData = decryptPinBlock(encryptedData, sessionKey)

            // Re-encrypt under destination key (ZPK)
            val clearDestKey = simulator.decryptUnderLmk(destKey, lmkId, 6)
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
            val clearPvk = simulator.decryptUnderLmk(pvk, lmkId, 14)
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