package io.cryptocalc.crypto.engines

import ai.cortex.core.IsoUtil
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * ════════════════════════════════════════════════════════════════════════
 * DukptEngine — ANSI X9.24-2004 3DES DUKPT Core Engine
 * ════════════════════════════════════════════════════════════════════════
 *
 * Single source of truth for all DUKPT derivation operations.
 *
 * Used by:
 *   • DUKPTService             (DUKPT ISO 9797 Tool UI)
 *   • PayShield10KCommandProcessor  (A0 mode A/B, G0, M2, M4 HSM commands)
 *
 * Implements:
 *   • IPEK derivation from BDK + KSN (Annex A)
 *   • Session key derivation via NRKGP (Non-Reversible Key Generation Process)
 *   • PIN Encryption Key (PEK) derivation — session key with NO variant
 *   • MAC working key derivation — session key XOR MAC variant
 *   • Data Encryption working key derivation — session key XOR Data variant
 *   • KSN descriptor parsing for PayShield compatibility
 *
 * KSN Structure (80-bit / 10-byte):
 *   ┌──────────────┬───────────────┬────────────┐
 *   │ Key Set ID   │  Device ID    │  Counter   │
 *   │ (variable)   │  (variable)   │ (variable) │
 *   └──────────────┴───────────────┴────────────┘
 *
 * Default ANSI X9.24: 21 counter bits (2^21 = 2,097,152 transactions)
 * PayShield "609" descriptor: 6H KSID + 0 sub + 9H DevID = 20 counter bits
 *
 * ════════════════════════════════════════════════════════════════════════
 */
object DukptEngine {

    // ─── Constants ───────────────────────────────────────────────────

    /**
     * C0C0C0C0 00000000 C0C0C0C0 00000000
     * Used in IPEK derivation (right half) and NRKGP (left half).
     */
    private val KEY_MASK_16 = byteArrayOf(
        0xC0.toByte(), 0xC0.toByte(), 0xC0.toByte(), 0xC0.toByte(),
        0x00, 0x00, 0x00, 0x00,
        0xC0.toByte(), 0xC0.toByte(), 0xC0.toByte(), 0xC0.toByte(),
        0x00, 0x00, 0x00, 0x00
    )

    /**
     * C0C0C0C0 00000000
     * 8-byte mask used inside NRKGP for left-half key derivation.
     */
    private val KEY_MASK_8 = byteArrayOf(
        0xC0.toByte(), 0xC0.toByte(), 0xC0.toByte(), 0xC0.toByte(),
        0x00, 0x00, 0x00, 0x00
    )

    /** Default counter width per ANSI X9.24-2004 for 80-bit KSN. */
    const val DEFAULT_COUNTER_BITS = 21

    // ─── Variant masks for working key derivation ────────────────────

    /**
     * PIN Encryption Key variant mask.
     * Applied: PEK = session_key XOR PEK_VARIANT
     */
    val PEK_VARIANT = byteArrayOf(
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF.toByte(),
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF.toByte()
    )

    /**
     * MAC working key variant mask.
     * Applied: MAC_key = session_key XOR MAC_VARIANT
     */
    val MAC_VARIANT = byteArrayOf(
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF.toByte(), 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF.toByte(), 0x00
    )

    /**
     * Data Encryption working key variant mask.
     * Applied: DEK = session_key XOR DATA_VARIANT
     */
    val DATA_VARIANT = byteArrayOf(
        0x00, 0x00, 0x00, 0x00, 0x00, 0xFF.toByte(), 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0xFF.toByte(), 0x00, 0x00
    )


    // ═══════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Derive IPEK (Initial PIN Encryption Key) from BDK and KSN.
     *
     * Per ANSI X9.24-2004 Annex A:
     *   1. Zero the rightmost [counterBits] in the 80-bit KSN → IKSN
     *   2. Take the LEFTMOST 8 bytes of the 10-byte IKSN
     *   3. Left  = TDES_ECB(BDK, IKSN_left8)
     *   4. Right = TDES_ECB(BDK XOR KEY_MASK, IKSN_left8)
     *   5. IPEK  = Left || Right
     *
     * @param bdk         Clear-text BDK (16 bytes / double-length 3DES)
     * @param ksn         Full KSN (10 bytes / 80 bits)
     * @param counterBits Number of counter bits to zero (default 21)
     * @return 16-byte IPEK
     */
    fun deriveIpek(bdk: ByteArray, ksn: ByteArray, counterBits: Int = DEFAULT_COUNTER_BITS): ByteArray {
        require(bdk.size == 16) { "BDK must be 16 bytes (double-length 3DES key), got ${bdk.size}" }
        require(ksn.size == 10) { "KSN must be 10 bytes (80 bits), got ${ksn.size}" }
        require(counterBits in 1..31) { "counterBits must be 1..31, got $counterBits" }

        // Step 1: Zero counter bits → IKSN
        val iksn = ksn.copyOf()
        zeroCounterBits(iksn, counterBits)

        // Step 2: LEFTMOST 8 bytes — includes Key Set ID prefix
        // ──────────────────────────────────────────────────────────
        // CRITICAL: This MUST be bytes[0..8], NOT bytes[size-8..size].
        // The crypto register in deriveSessionKey uses the RIGHTMOST 8,
        // but IPEK derivation is different — it includes the KSID.
        // ──────────────────────────────────────────────────────────
        val iksn8 = iksn.copyOfRange(0, 8)

        val cipher = Cipher.getInstance("DESede/ECB/NoPadding")

        // Step 3: Left half = TDES(BDK, IKSN8)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(expandTo24(bdk), "DESede"))
        val left = cipher.doFinal(iksn8)

        // Step 4: Right half = TDES(BDK XOR KEY_MASK, IKSN8)
        val bdkXor = xorBytes(bdk, KEY_MASK_16)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(expandTo24(bdkXor), "DESede"))
        val right = cipher.doFinal(iksn8)

        // Step 5: IPEK = Left || Right
        return left + right
    }

    /**
     * Derive IPEK from BDK and hex-encoded KSN string.
     * Convenience overload for PayShield command processor.
     *
     * @param bdk              Clear-text BDK (16 bytes)
     * @param ksnHex           KSN as hex string (20 hex chars = 10 bytes)
     * @param counterBits      Counter bits to zero (default 21)
     * @return 16-byte IPEK
     */
    fun deriveIpek(bdk: ByteArray, ksnHex: String, counterBits: Int = DEFAULT_COUNTER_BITS): ByteArray {
        val normalizedKsn = if (ksnHex.length % 2 != 0) "0$ksnHex" else ksnHex
        val ksnBytes = IsoUtil.hexToBytes(normalizedKsn.replace(" ", ""))
        return deriveIpek(bdk, ksnBytes, counterBits)
    }

    /**
     * Derive session key from IPEK and KSN using NRKGP.
     *
     * Iterates through counter bits from MSB to LSB. For each set bit,
     * sets that bit in the running KSN and applies NRKGP to derive
     * the next intermediate key.
     *
     * @param ipek         16-byte IPEK
     * @param ksn          10-byte KSN with transaction counter
     * @param counterBits  Number of counter bits (default 21)
     * @return 16-byte session key
     */
    fun deriveSessionKey(
        ipek: ByteArray,
        ksn: ByteArray,
        counterBits: Int = DEFAULT_COUNTER_BITS
    ): ByteArray {
        require(ipek.size == 16) { "IPEK must be 16 bytes, got ${ipek.size}" }
        require(ksn.size == 10) { "KSN must be 10 bytes, got ${ksn.size}" }

        var currentKey = ipek.copyOf()
        val runningKsn = ksn.copyOf()
        zeroCounterBits(runningKsn, counterBits)

        val counter = extractCounter(ksn, counterBits)

        for (shiftReg in (counterBits - 1) downTo 0) {
            if (((counter shr shiftReg) and 1) == 1) {
                // Set this bit in the running KSN
                val byteIdx = runningKsn.size - 1 - shiftReg / 8
                val bitInByte = shiftReg % 8
                if (byteIdx in runningKsn.indices) {
                    runningKsn[byteIdx] = (runningKsn[byteIdx].toInt() or (1 shl bitInByte)).toByte()
                }
                // Crypto register = RIGHTMOST 8 bytes of running KSN
                val cryptoReg = runningKsn.copyOfRange(
                    maxOf(0, runningKsn.size - 8), runningKsn.size
                )
                currentKey = nonReversibleKeyGen(currentKey, cryptoReg)
            }
        }
        return currentKey
    }

    /**
     * Derive the DUKPT session key (current key) from BDK and KSN.
     *
     * Returns the raw session key. Callers must apply the appropriate
     * variant mask before use (PEK_VARIANT for PIN, MAC_VARIANT for MAC, etc.).
     *
     * @return 16-byte session key
     */
    fun derivePek(
        bdk: ByteArray,
        ksn: ByteArray,
        counterBits: Int = DEFAULT_COUNTER_BITS
    ): ByteArray {
        val ipek = deriveIpek(bdk, ksn, counterBits)
        return deriveSessionKey(ipek, ksn, counterBits)
    }

    /**
     * Derive the DUKPT session key when IPEK is already known.
     * Skips IPEK derivation — useful when input is IPEK, not BDK.
     *
     * @return 16-byte session key
     */
    fun derivePekFromIpek(
        ipek: ByteArray,
        ksn: ByteArray,
        counterBits: Int = DEFAULT_COUNTER_BITS
    ): ByteArray {
        return deriveSessionKey(ipek, ksn, counterBits)
    }

    /**
     * Derive MAC working key from BDK and KSN.
     *
     * MAC key = session key XOR MAC_VARIANT
     */
    fun deriveMacKey(
        bdk: ByteArray,
        ksn: ByteArray,
        counterBits: Int = DEFAULT_COUNTER_BITS
    ): ByteArray {
        val ipek = deriveIpek(bdk, ksn, counterBits)
        val sessionKey = deriveSessionKey(ipek, ksn, counterBits)
        return xorBytes(sessionKey, MAC_VARIANT)
    }

    /**
     * Derive Data Encryption working key from BDK and KSN.
     *
     * Per ANSI X9.24-2004:
     *   1. variant_key = session_key XOR DATA_VARIANT
     *   2. DEK_left  = 3DES_Encrypt(variant_key, variant_key[0..8])
     *   3. DEK_right = 3DES_Encrypt(variant_key, variant_key[8..16])
     *   4. DEK = DEK_left || DEK_right
     */
    fun deriveDataKey(
        bdk: ByteArray,
        ksn: ByteArray,
        counterBits: Int = DEFAULT_COUNTER_BITS
    ): ByteArray {
        val ipek = deriveIpek(bdk, ksn, counterBits)
        val sessionKey = deriveSessionKey(ipek, ksn, counterBits)
        val variantKey = xorBytes(sessionKey, DATA_VARIANT)
        return applyVariantEncryption(variantKey)
    }

    /**
     * Apply the ANSI X9.24 "one-way function" for data variant key derivation.
     *
     * 3DES-encrypts each 8-byte half of the 16-byte variant key using the full
     * variant key itself, producing a new 16-byte key that cannot be reversed
     * back to the session key.
     */
    fun applyVariantEncryption(variantKey: ByteArray): ByteArray {
        require(variantKey.size == 16) { "Variant key must be 16 bytes, got ${variantKey.size}" }
        val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(expandTo24(variantKey), "DESede"))
        val left = cipher.doFinal(variantKey.copyOfRange(0, 8))
        val right = cipher.doFinal(variantKey.copyOfRange(8, 16))
        return left + right
    }


    // ═══════════════════════════════════════════════════════════════════
    // KSN DESCRIPTOR (PayShield compatibility)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Parse PayShield KSN descriptor to determine counter bit width.
     *
     * KSN descriptor format: 3 digits [KSID_len][sub_key][DevID_len]
     *   - KSID_len:  number of hex chars for Key Set ID (0-9, A-F)
     *   - sub_key:   sub-key indicator (0 = none)
     *   - DevID_len: number of hex chars for Device ID (0-9, A-F)
     *
     * Counter bits = 80 - (KSID_len × 4) - (DevID_len × 4)
     *
     * Example "609": KSID=6 hex (24 bits), DevID=9 hex (36 bits)
     *   → Counter = 80 - 24 - 36 = 20 bits
     *
     * @param descriptor 3-char KSN descriptor string (e.g., "609")
     * @return number of counter bits
     */
    fun parseKsnDescriptorCounterBits(descriptor: String): Int {
        require(descriptor.length == 3) { "KSN descriptor must be 3 chars, got '${descriptor}'" }

        val ksidHexChars = descriptor[0].digitToIntOrNull(16)
            ?: throw IllegalArgumentException("Invalid KSID length in descriptor: '${descriptor[0]}'")
        val devIdHexChars = descriptor[2].digitToIntOrNull(16)
            ?: throw IllegalArgumentException("Invalid DevID length in descriptor: '${descriptor[2]}'")

        val ksidBits = ksidHexChars * 4
        val devIdBits = devIdHexChars * 4
        val counterBits = 80 - ksidBits - devIdBits

        require(counterBits in 1..31) {
            "Computed counter bits ($counterBits) out of range for descriptor '$descriptor'"
        }
        return counterBits
    }

    /**
     * Build a KSN from Key Set ID, Device ID, and counter value.
     *
     * @param keySetId   Key Set Identifier (hex string, length per descriptor)
     * @param deviceId   Device Identifier (hex string, length per descriptor)
     * @param counter    Transaction counter value
     * @param counterBits Number of counter bits
     * @return 20-char hex string (10 bytes)
     */
    fun buildKsn(
        keySetId: String,
        deviceId: String,
        counter: Long,
        counterBits: Int = DEFAULT_COUNTER_BITS
    ): String {
        val totalHexChars = 20 // 10 bytes = 20 hex
        val counterHexChars = (counterBits + 3) / 4 // round up to hex boundary
        val expectedLen = totalHexChars

        // Build full KSN hex string: [KSID][DevID][Counter]
        val counterHex = counter.toString(16).uppercase().padStart(counterHexChars, '0')
        val baseHex = (keySetId + deviceId).padEnd(totalHexChars - counterHexChars, '0')
        val ksnHex = (baseHex + counterHex).take(totalHexChars)

        return ksnHex.uppercase()
    }


    // ═══════════════════════════════════════════════════════════════════
    // KCV
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Compute Key Check Value: encrypt 8 zero bytes, return first 3 bytes as hex.
     */
    fun computeKcv(key: ByteArray): String {
        val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(expandTo24(key), "DESede"))
        val kcv = cipher.doFinal(ByteArray(8))
        return IsoUtil.bytesToHex(kcv.copyOfRange(0, 3)).uppercase()
    }


    // ═══════════════════════════════════════════════════════════════════
    // INTERNAL — NRKGP & Utilities
    // ═══════════════════════════════════════════════════════════════════

    /**
     * ANSI X9.24 Non-Reversible Key Generation Process (NRKGP).
     *
     * Produces a new 16-byte key from a current key and 8-byte crypto register.
     *
     * Right half:
     *   R1 = data XOR key_right
     *   R2 = DES_Encrypt(key_left, R1)
     *   new_right = R2 XOR key_right
     *
     * Left half (apply C0C0C0C000000000 mask to both key halves):
     *   L1 = data XOR masked_right
     *   L2 = DES_Encrypt(masked_left, L1)
     *   new_left = L2 XOR masked_right
     */
    internal fun nonReversibleKeyGen(key: ByteArray, data: ByteArray): ByteArray {
        require(key.size == 16) { "Key must be 16 bytes" }
        require(data.size == 8) { "Data (crypto register) must be 8 bytes" }

        val keyLeft = key.copyOfRange(0, 8)
        val keyRight = key.copyOfRange(8, 16)
        val desCipher = Cipher.getInstance("DES/ECB/NoPadding")

        // ── Right half ──
        val r1 = xorBytes(data, keyRight)
        desCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyLeft, "DES"))
        val r2 = desCipher.doFinal(r1)
        val newRight = xorBytes(r2, keyRight)

        // ── Left half (with C0 mask) ──
        val maskedLeft = xorBytes(keyLeft, KEY_MASK_8)
        val maskedRight = xorBytes(keyRight, KEY_MASK_8)
        val l1 = xorBytes(data, maskedRight)
        desCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(maskedLeft, "DES"))
        val l2 = desCipher.doFinal(l1)
        val newLeft = xorBytes(l2, maskedRight)

        return newLeft + newRight
    }

    /**
     * Zero the rightmost [counterBits] bits in a byte array (in-place).
     * Bit 0 = LSB of the last byte.
     */
    fun zeroCounterBits(ksn: ByteArray, counterBits: Int) {
        for (bit in 0 until counterBits) {
            val byteIdx = ksn.size - 1 - bit / 8
            val bitInByte = bit % 8
            if (byteIdx in ksn.indices) {
                ksn[byteIdx] = (ksn[byteIdx].toInt() and (1 shl bitInByte).inv()).toByte()
            }
        }
    }

    /**
     * Extract the counter value (rightmost [counterBits]) from KSN.
     */
    internal fun extractCounter(ksn: ByteArray, counterBits: Int): Int {
        var counter = 0
        for (bit in 0 until counterBits) {
            val byteIdx = ksn.size - 1 - bit / 8
            val bitInByte = bit % 8
            if (byteIdx in ksn.indices && (ksn[byteIdx].toInt() and (1 shl bitInByte)) != 0) {
                counter = counter or (1 shl bit)
            }
        }
        return counter
    }

    /**
     * Expand 16-byte double-length 3DES key to 24-byte Java DESede key.
     * K1-K2 → K1-K2-K1
     */
    fun expandTo24(key: ByteArray): ByteArray {
        return if (key.size == 16) {
            ByteArray(24).apply {
                key.copyInto(this, 0, 0, 16)
                key.copyInto(this, 16, 0, 8)  // K1-K2-K1
            }
        } else {
            key.copyOf(24)
        }
    }

    /** XOR two byte arrays of the same length. */
    fun xorBytes(a: ByteArray, b: ByteArray): ByteArray {
        require(a.size == b.size) { "XOR arrays must be same length: ${a.size} vs ${b.size}" }
        return ByteArray(a.size) { i -> (a[i].toInt() xor b[i].toInt()).toByte() }
    }
}