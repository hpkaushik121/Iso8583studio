package io.cryptocalc.crypto.engines.encryption

import ai.cortex.core.IsoUtil
import ai.cortex.core.IsoUtil.hexToBytes
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * SHA1 Calculator Engine
 *
 * Enterprise-grade custom SHA1 implementation for payment industry applications.
 * Provides comprehensive SHA1-based cryptographic operations with HSM compatibility.
 *
 * Features:
 * - SHA1 hashing with multiple input/output formats
 * - HMAC-SHA1 for message authentication
 * - PBKDF1 key derivation function
 * - Payment industry MAC calculations (ISO 9797-1, ANSI X9.19)
 * - EMV cryptogram validation functions
 * - HSM-compatible operations
 * - Comprehensive validation and error handling
 * - Performance optimizations for enterprise environments
 *
 * @author Crypto SDK Team
 * @version 1.0.0
 */
internal object SHA1CalculatorEngine {

    // ============================================================================
    // CONSTANTS AND CONFIGURATION
    // ============================================================================

    const val ALGORITHM = "SHA1"
    const val HMAC_ALGORITHM = "HmacSHA1"
    const val DIGEST_LENGTH = 20 // SHA1 produces 160-bit (20-byte) digest
    const val BLOCK_SIZE = 64 // SHA1 block size is 512 bits (64 bytes)
    const val MAC_LENGTH = 8 // Standard MAC length for payment industry

    // ISO padding schemes
    const val ISO_9797_1_METHOD_1 = "ISO9797_1_M1"
    const val ISO_9797_1_METHOD_2 = "ISO9797_1_M2"
    const val ANSI_X9_19 = "ANSI_X9_19"
    const val PKCS7_PADDING = "PKCS7"

    // Output formats
    const val FORMAT_HEX = "HEX"
    const val FORMAT_BASE64 = "BASE64"
    const val FORMAT_BINARY = "BINARY"

    // ============================================================================
    // CORE SHA1 OPERATIONS
    // ============================================================================

    /**
     * Core SHA1 hash calculation with fallback implementation
     */
    fun calculateSHA1(data: ByteArray): ByteArray {
        return try {
            val digest = MessageDigest.getInstance("SHA-1")
            digest.digest(data)
        } catch (e: Exception) {
            // Fallback to custom implementation if JCE fails
            customSHA1Implementation(data)
        }
    }

    /**
     * SHA1 hash with multiple input formats
     */
    fun hash(
        data: String,
        inputFormat: String = FORMAT_HEX,
        outputFormat: String = FORMAT_HEX
    ): String {
        val inputBytes = when (inputFormat.uppercase()) {
            FORMAT_HEX -> hexToBytes(data)
            FORMAT_BASE64 -> IsoUtil.base64ToBytes(data)
            "ASCII", "UTF8" -> data.toByteArray(Charsets.UTF_8)
            FORMAT_BINARY -> data.toByteArray(Charsets.ISO_8859_1)
            else -> throw IllegalArgumentException("Unsupported input format: $inputFormat")
        }

        val hashBytes = calculateSHA1(inputBytes)

        return when (outputFormat.uppercase()) {
            FORMAT_HEX -> IsoUtil.bytesToHex(hashBytes)
            FORMAT_BASE64 -> IsoUtil.bytesToBase64(hashBytes)
            FORMAT_BINARY -> String(hashBytes, Charsets.ISO_8859_1)
            else -> throw IllegalArgumentException("Unsupported output format: $outputFormat")
        }
    }

    /**
     * SHA1 hash with padding schemes for payment industry
     */
    fun hashWithPadding(
        data: ByteArray,
        paddingScheme: String = ISO_9797_1_METHOD_2
    ): ByteArray {
        val paddedData = applyPadding(data, paddingScheme)
        return calculateSHA1(paddedData)
    }

    /**
     * Iterative SHA1 hashing (PBKDF1-style)
     */
    fun iterativeHash(
        data: ByteArray,
        iterations: Int,
        salt: ByteArray? = null
    ): ByteArray {
        require(iterations > 0) { "Iterations must be positive" }

        var input = if (salt != null) salt + data else data
        repeat(iterations) {
            input = calculateSHA1(input)
        }
        return input
    }

    /**
     * Verify SHA1 hash against expected value
     */
    fun verifyHash(
        data: ByteArray,
        expectedHash: ByteArray,
        paddingScheme: String? = null
    ): Boolean {
        val actualHash = if (paddingScheme != null) {
            hashWithPadding(data, paddingScheme)
        } else {
            calculateSHA1(data)
        }
        return actualHash.contentEquals(expectedHash)
    }

    // ============================================================================
    // HMAC-SHA1 OPERATIONS
    // ============================================================================

    /**
     * Calculate HMAC-SHA1
     */
    fun calculateHMAC(data: ByteArray, key: ByteArray): ByteArray {
        return try {
            val mac = Mac.getInstance(HMAC_ALGORITHM)
            val keySpec = SecretKeySpec(key, HMAC_ALGORITHM)
            mac.init(keySpec)
            mac.doFinal(data)
        } catch (e: Exception) {
            // Fallback to custom HMAC implementation
            customHMACImplementation(data, key)
        }
    }

    /**
     * Calculate HMAC-SHA1 with string inputs
     */
    fun calculateHMAC(
        data: String,
        key: String,
        inputFormat: String = FORMAT_HEX,
        outputFormat: String = FORMAT_HEX
    ): String {
        val dataBytes = when (inputFormat.uppercase()) {
            FORMAT_HEX -> hexToBytes(data)
            FORMAT_BASE64 -> IsoUtil.base64ToBytes(data)
            else -> data.toByteArray(Charsets.UTF_8)
        }

        val keyBytes = when (inputFormat.uppercase()) {
            FORMAT_HEX -> hexToBytes(key)
            FORMAT_BASE64 -> IsoUtil.base64ToBytes(key)
            else -> key.toByteArray(Charsets.UTF_8)
        }

        val hmacBytes = calculateHMAC(dataBytes, keyBytes)

        return when (outputFormat.uppercase()) {
            FORMAT_HEX -> IsoUtil.bytesToHex(hmacBytes)
            FORMAT_BASE64 -> IsoUtil.bytesToBase64(hmacBytes)
            else -> String(hmacBytes, Charsets.UTF_8)
        }
    }

    /**
     * Verify HMAC-SHA1
     */
    fun verifyHMAC(
        data: ByteArray,
        key: ByteArray,
        expectedHMAC: ByteArray
    ): Boolean {
        val calculatedHMAC = calculateHMAC(data, key)
        return calculatedHMAC.contentEquals(expectedHMAC)
    }

    // ============================================================================
    // PAYMENT INDUSTRY SPECIFIC FUNCTIONS
    // ============================================================================

    /**
     * Generate MAC using SHA1 for payment transactions
     */
    fun generatePaymentMAC(
        data: ByteArray,
        key: ByteArray,
        paddingScheme: String = ISO_9797_1_METHOD_2,
        macLength: Int = MAC_LENGTH
    ): ByteArray {
        val paddedData = applyPadding(data, paddingScheme)
        val hmac = calculateHMAC(paddedData, key)
        return hmac.sliceArray(0 until minOf(macLength, hmac.size))
    }

    /**
     * Generate retail MAC as per ANSI X9.19 using SHA1
     */
    fun generateRetailMAC(
        data: ByteArray,
        key: ByteArray
    ): ByteArray {
        require(key.size >= 16) { "Retail MAC requires at least 16-byte key" }

        val paddedData = applyPadding(data, ANSI_X9_19)
        val leftKey = key.sliceArray(0..7)
        val rightKey = key.sliceArray(8..15)

        // Calculate intermediate MAC with left key
        val intermediateMac = calculateHMAC(paddedData, leftKey)

        // XOR with right key and hash again
        val xorResult = IsoUtil.xorByteArray(intermediateMac.sliceArray(0..7), rightKey)
        val finalHash = calculateSHA1(xorResult)

        return finalHash.sliceArray(0 until MAC_LENGTH)
    }

    /**
     * EMV Application Cryptogram validation using SHA1
     */
    fun validateEMVCryptogram(
        transactionData: ByteArray,
        sessionKey: ByteArray,
        expectedCryptogram: ByteArray,
        cryptogramType: String = "ARQC"
    ): Boolean {
        val paddedData = applyPadding(transactionData, ISO_9797_1_METHOD_2)
        val calculatedCryptogram = when (cryptogramType.uppercase()) {
            "ARQC", "TC", "AAC" -> {
                val mac = calculateHMAC(paddedData, sessionKey)
                mac.sliceArray(0 until 8) // EMV cryptograms are 8 bytes
            }
            "ARPC" -> {
                // ARPC uses different calculation
                val arqcData = expectedCryptogram + transactionData.sliceArray(0 until 2)
                val mac = calculateHMAC(arqcData, sessionKey)
                mac.sliceArray(0 until 4) // ARPC is 4 bytes
            }
            else -> throw IllegalArgumentException("Unsupported cryptogram type: $cryptogramType")
        }

        return calculatedCryptogram.contentEquals(expectedCryptogram)
    }

    /**
     * Derive session key using SHA1-based key derivation
     */
    fun deriveSessionKey(
        masterKey: ByteArray,
        diversificationData: ByteArray,
        keyType: String = "ENC"
    ): ByteArray {
        val keyTypeBytes = when (keyType.uppercase()) {
            "ENC" -> byteArrayOf(0x00, 0x00, 0x00, 0x01) // Encryption key
            "MAC" -> byteArrayOf(0x00, 0x00, 0x00, 0x02) // MAC key
            "DEK" -> byteArrayOf(0x00, 0x00, 0x00, 0x03) // Data encryption key
            else -> throw IllegalArgumentException("Unsupported key type: $keyType")
        }

        val derivationData = diversificationData + keyTypeBytes
        return calculateHMAC(derivationData, masterKey).sliceArray(0..15) // 16-byte key
    }

    /**
     * PIN verification using SHA1-based PIN block validation
     */
    fun verifyPINBlock(
        pinBlock: ByteArray,
        pan: String,
        expectedPINHash: ByteArray
    ): Boolean {
        val panData = pan.takeLast(12).padStart(12, '0').toByteArray()
        val pinData = pinBlock + panData
        val calculatedHash = calculateSHA1(pinData)
        return calculatedHash.sliceArray(0 until expectedPINHash.size).contentEquals(expectedPINHash)
    }

    /**
     * Key Check Value calculation using SHA1
     */
    fun calculateKeyCheckValue(key: ByteArray, length: Int = 3): ByteArray {
        val testData = ByteArray(16) // 16 bytes of zeros
        val hash = calculateHMAC(testData, key)
        return hash.sliceArray(0 until length)
    }

    // ============================================================================
    // KEY DERIVATION FUNCTIONS
    // ============================================================================

    /**
     * PBKDF1 implementation using SHA1
     */
    fun pbkdf1(
        password: ByteArray,
        salt: ByteArray,
        iterations: Int,
        keyLength: Int
    ): ByteArray {
        require(iterations > 0) { "Iterations must be positive" }
        require(keyLength <= DIGEST_LENGTH) { "Key length cannot exceed digest length ($DIGEST_LENGTH)" }

        var derived = password + salt
        repeat(iterations) {
            derived = calculateSHA1(derived)
        }

        return derived.sliceArray(0 until keyLength)
    }

    /**
     * Custom key derivation for payment systems
     */
    fun derivePaymentKey(
        masterKey: ByteArray,
        keySerial: ByteArray,
        keyVersion: Int = 1
    ): ByteArray {
        val versionBytes = byteArrayOf(
            ((keyVersion shr 24) and 0xFF).toByte(),
            ((keyVersion shr 16) and 0xFF).toByte(),
            ((keyVersion shr 8) and 0xFF).toByte(),
            (keyVersion and 0xFF).toByte()
        )

        val derivationData = keySerial + versionBytes
        val hash1 = calculateHMAC(derivationData, masterKey)
        val hash2 = calculateHMAC(hash1, masterKey)

        return (hash1.sliceArray(0..7) + hash2.sliceArray(0..7))
    }

    /**
     * Hierarchical key derivation
     */
    fun deriveHierarchicalKey(
        parentKey: ByteArray,
        path: List<Int>
    ): ByteArray {
        var currentKey = parentKey

        for (index in path) {
            val indexBytes = byteArrayOf(
                ((index shr 24) and 0xFF).toByte(),
                ((index shr 16) and 0xFF).toByte(),
                ((index shr 8) and 0xFF).toByte(),
                (index and 0xFF).toByte()
            )
            currentKey = calculateHMAC(indexBytes, currentKey)
        }

        return currentKey
    }

    // ============================================================================
    // PADDING IMPLEMENTATIONS
    // ============================================================================

    /**
     * Apply various padding schemes
     */
    fun applyPadding(data: ByteArray, paddingScheme: String): ByteArray {
        return when (paddingScheme.uppercase()) {
            ISO_9797_1_METHOD_1 -> applyISO9797Method1Padding(data)
            ISO_9797_1_METHOD_2 -> applyISO9797Method2Padding(data)
            ANSI_X9_19 -> applyANSIX919Padding(data)
            PKCS7_PADDING -> applyPKCS7Padding(data, BLOCK_SIZE)
            "NONE" -> data
            else -> throw IllegalArgumentException("Unsupported padding scheme: $paddingScheme")
        }
    }

    /**
     * ISO/IEC 9797-1 Method 1 padding (zero padding to block boundary)
     */
    private fun applyISO9797Method1Padding(data: ByteArray, blockSize: Int = 8): ByteArray {
        val paddingLength = blockSize - (data.size % blockSize)
        if (paddingLength == blockSize) return data

        return data + ByteArray(paddingLength) { 0 }
    }

    /**
     * ISO/IEC 9797-1 Method 2 padding (1 bit followed by zeros)
     */
    private fun applyISO9797Method2Padding(data: ByteArray, blockSize: Int = 8): ByteArray {
        val result = data.toMutableList()
        result.add(0x80.toByte()) // Add '1' bit

        val paddingLength = blockSize - (result.size % blockSize)
        if (paddingLength != blockSize) {
            repeat(paddingLength) { result.add(0) }
        }

        return result.toByteArray()
    }

    /**
     * ANSI X9.19 padding
     */
    private fun applyANSIX919Padding(data: ByteArray): ByteArray {
        return applyISO9797Method2Padding(data)
    }

    /**
     * PKCS#7 padding
     */
    private fun applyPKCS7Padding(data: ByteArray, blockSize: Int): ByteArray {
        val paddingLength = blockSize - (data.size % blockSize)
        val paddedData = ByteArray(data.size + paddingLength)
        System.arraycopy(data, 0, paddedData, 0, data.size)

        for (i in data.size until paddedData.size) {
            paddedData[i] = paddingLength.toByte()
        }

        return paddedData
    }

    // ============================================================================
    // CUSTOM IMPLEMENTATIONS (FALLBACK)
    // ============================================================================

    /**
     * Custom SHA1 implementation for environments without JCE
     */
    private fun customSHA1Implementation(data: ByteArray): ByteArray {
        // Initialize hash values (SHA1 constants)
        var h0 = 0x67452301.toInt()
        var h1 = 0xEFCDAB89.toInt()
        var h2 = 0x98BADCFE.toInt()
        var h3 = 0x10325476.toInt()
        var h4 = 0xC3D2E1F0.toInt()

        // Pre-process message
        val ml = data.size * 8L // Message length in bits
        val message = data.toMutableList()

        // Append '1' bit
        message.add(0x80.toByte())

        // Append zeros
        while ((message.size % 64) != 56) {
            message.add(0)
        }

        // Append original length as 64-bit big-endian integer
        for (i in 7 downTo 0) {
            message.add(((ml shr (i * 8)) and 0xFF).toByte())
        }

        // Process message in 512-bit chunks
        for (chunk in 0 until message.size step 64) {
            val w = IntArray(80)

            // Break chunk into sixteen 32-bit big-endian words
            for (i in 0..15) {
                val offset = chunk + i * 4
                w[i] = ((message[offset].toInt() and 0xFF) shl 24) or
                        ((message[offset + 1].toInt() and 0xFF) shl 16) or
                        ((message[offset + 2].toInt() and 0xFF) shl 8) or
                        (message[offset + 3].toInt() and 0xFF)
            }

            // Extend the sixteen 32-bit words into eighty 32-bit words
            for (i in 16..79) {
                w[i] = IsoUtil.leftRotate(w[i - 3] xor w[i - 8] xor w[i - 14] xor w[i - 16], 1)
            }

            // Initialize hash value for this chunk
            var a = h0
            var b = h1
            var c = h2
            var d = h3
            var e = h4

            // Main loop
            for (i in 0..79) {
                val (f, k) = when (i) {
                    in 0..19 -> Pair((b and c) or ((b.inv()) and d), 0x5A827999)
                    in 20..39 -> Pair(b xor c xor d, 0x6ED9EBA1)
                    in 40..59 -> Pair((b and c) or (b and d) or (c and d), 0x8F1BBCDC)
                    else -> Pair(b xor c xor d, 0xCA62C1D6)
                }

                val temp = IsoUtil.leftRotate(a, 5) + f + e + k.toInt() + w[i]
                e = d
                d = c
                c = IsoUtil.leftRotate(b, 30)
                b = a
                a = temp
            }

            // Add this chunk's hash to result
            h0 += a
            h1 += b
            h2 += c
            h3 += d
            h4 += e
        }

        // Produce final hash value as byte array
        val result = ByteArray(20)
        IsoUtil.intToBytes(h0, result, 0)
        IsoUtil.intToBytes(h1, result, 4)
        IsoUtil.intToBytes(h2, result, 8)
        IsoUtil.intToBytes(h3, result, 12)
        IsoUtil.intToBytes(h4, result, 16)

        return result
    }

    /**
     * Custom HMAC implementation
     */
    private fun customHMACImplementation(data: ByteArray, key: ByteArray): ByteArray {
        var actualKey = key

        // If key is longer than block size, hash it
        if (actualKey.size > BLOCK_SIZE) {
            actualKey = calculateSHA1(actualKey)
        }

        // If key is shorter than block size, pad with zeros
        if (actualKey.size < BLOCK_SIZE) {
            actualKey = actualKey + ByteArray(BLOCK_SIZE - actualKey.size)
        }

        // Create inner and outer padding
        val innerPad = ByteArray(BLOCK_SIZE) { 0x36 }
        val outerPad = ByteArray(BLOCK_SIZE) { 0x5C }

        // XOR key with padding
        val innerKey = IsoUtil.xorByteArray(actualKey, innerPad)
        val outerKey = IsoUtil.xorByteArray(actualKey, outerPad)

        // Calculate HMAC
        val innerHash = calculateSHA1(innerKey + data)
        return calculateSHA1(outerKey + innerHash)
    }


    /**
     * Generate secure random bytes
     */
    fun generateRandomBytes(length: Int): ByteArray {
        val bytes = ByteArray(length)
        SecureRandom().nextBytes(bytes)
        return bytes
    }

    /**
     * Generate salt for key derivation
     */
    fun generateSalt(length: Int = 8): ByteArray {
        return generateRandomBytes(length)
    }

    // ============================================================================
    // VALIDATION FUNCTIONS
    // ============================================================================

    /**
     * Validate input data format
     */
    fun validateFormat(data: String, format: String): Boolean {
        return when (format.uppercase()) {
            FORMAT_HEX -> data.matches(Regex("^[0-9A-Fa-f\\s-]*$"))
            FORMAT_BASE64 -> try {
                IsoUtil.base64ToBytes(data)
                true
            } catch (e: Exception) {
                false
            }
            else -> true
        }
    }

    /**
     * Validate key strength
     */
    fun validateKeyStrength(key: ByteArray): Map<String, Boolean> {
        val results = mutableMapOf<String, Boolean>()

        // Check minimum length
        results["minimumLength"] = key.size >= 16

        // Check for weak patterns
        results["notAllZeros"] = !key.all { it == 0.toByte() }
        results["notAllOnes"] = !key.all { it == 0xFF.toByte() }
        results["notRepeating"] = !(key.size >= 2 && key.all { it == key[0] })

        // Check entropy (basic)
        val uniqueBytes = key.toSet().size
        results["sufficientEntropy"] = uniqueBytes >= (key.size / 4)

        return results
    }

    // ============================================================================
    // COMPREHENSIVE TEST FUNCTIONS
    // ============================================================================

    /**
     * Self-test function to verify implementation
     */
    fun runSelfTest(): Boolean {
        return try {
            // Test vectors from RFC 3174
            val testCases = listOf(
                "abc" to "A9993E364706816ABA3E25717850C26C9CD0D89D",
                "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq" to
                        "84983E441C3BD26EBAAE4AA1F95129E5E54670F1",
                "" to "DA39A3EE5E6B4B0D3255BFEF95601890AFD80709"
            )

            // Test basic SHA1
            testCases.forEach { (input, expected) ->
                val result = hash(IsoUtil.bytesToHex(input.toByteArray()), "HEX", "HEX")
                if (result != expected) return false
            }

            // Test HMAC-SHA1
            val hmacKey = "0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B"
            val hmacData = "4869205468657265"
            val expectedHMAC = "B617318655057264E28BC0B6FB378C8EF146BE00"
            val hmacResult = calculateHMAC(hmacData, hmacKey, "HEX", "HEX")
            if (hmacResult != expectedHMAC) return false

            // Test padding
            val testData = "Hello".toByteArray()
            val paddedData = applyPadding(testData, ISO_9797_1_METHOD_2)
            if (paddedData.size % 8 != 0) return false

            // Test key derivation
            val masterKey = generateRandomBytes(32)
            val salt = generateRandomBytes(8)
            val derivedKey = pbkdf1(masterKey, salt, 1000, 16)
            if (derivedKey.size != 16) return false

            true
        } catch (e: Exception) {
            println("Self-test failed: ${e.message}")
            false
        }
    }

    /**
     * Extended test function with comprehensive coverage
     */
    fun runExtendedTest(): Map<String, Boolean> {
        val results = mutableMapOf<String, Boolean>()

        try {
            // Test core hashing
            val testData = "Payment System Test Data 123456789"
            val hash1 = hash(IsoUtil.bytesToHex(testData.toByteArray()), "HEX", "HEX")
            val hash2 = hash(IsoUtil.bytesToHex(testData.toByteArray()), "HEX", "HEX")
            results["consistentHashing"] = hash1 == hash2

            // Test different input formats
            val hexInput = IsoUtil.bytesToHex(testData.toByteArray())
            val base64Input = IsoUtil.bytesToBase64(testData.toByteArray())
            val hashFromHex = hash(hexInput, "HEX")
            val hashFromBase64 = hash(base64Input, "BASE64")
            results["formatConsistency"] = hashFromHex == hashFromBase64

            // Test HMAC
            val key = generateRandomBytes(32)
            val data = testData.toByteArray()
            val hmac1 = calculateHMAC(data, key)
            val hmac2 = calculateHMAC(data, key)
            results["hmacConsistency"] = hmac1.contentEquals(hmac2)

            // Test HMAC verification
            val hmacResult = calculateHMAC(data, key)
            results["hmacVerification"] = verifyHMAC(data, key, hmacResult)

            // Test padding schemes
            results["iso9797Method1"] = try {
                val padded = applyPadding(data, ISO_9797_1_METHOD_1)
                padded.size % 8 == 0
            } catch (e: Exception) { false }

            results["iso9797Method2"] = try {
                val padded = applyPadding(data, ISO_9797_1_METHOD_2)
                padded.size % 8 == 0 && padded[data.size] == 0x80.toByte()
            } catch (e: Exception) { false }

            // Test payment industry functions
            val paymentKey = generateRandomBytes(24)
            val transactionData = generateRandomBytes(64)

            val paymentMAC = generatePaymentMAC(transactionData, paymentKey)
            results["paymentMAC"] = paymentMAC.size == MAC_LENGTH

            val retailMAC = generateRetailMAC(transactionData, paymentKey)
            results["retailMAC"] = retailMAC.size == MAC_LENGTH

            val kcv = calculateKeyCheckValue(paymentKey)
            results["keyCheckValue"] = kcv.size == 3

            // Test session key derivation
            val diversificationData = generateRandomBytes(16)
            val sessionKey = deriveSessionKey(paymentKey, diversificationData)
            results["sessionKeyDerivation"] = sessionKey.size == 16

            // Test PBKDF1
            val password = "password".toByteArray()
            val salt = generateRandomBytes(8)
            val derivedKey = pbkdf1(password, salt, 1000, 16)
            results["pbkdf1"] = derivedKey.size == 16

            // Test hierarchical key derivation
            val parentKey = generateRandomBytes(32)
            val path = listOf(0x80000000.toInt(), 0, 0, 0)
            val childKey = deriveHierarchicalKey(parentKey, path)
            results["hierarchicalDerivation"] = childKey.size == DIGEST_LENGTH

            // Test EMV cryptogram validation
            val emvKey = generateRandomBytes(16)
            val emvData = generateRandomBytes(32)
            val cryptogram = generatePaymentMAC(emvData, emvKey, ISO_9797_1_METHOD_2, 8)
            results["emvCryptogram"] = validateEMVCryptogram(emvData, emvKey, cryptogram)

            // Test utility functions
            val hexString = "48656C6C6F20576F726C64"
            val bytes = hexToBytes(hexString)
            val backToHex = IsoUtil.bytesToHex(bytes)
            results["hexConversion"] = hexString.equals(backToHex, ignoreCase = true)

            val base64String = IsoUtil.bytesToBase64(bytes)
            val backToBytes = IsoUtil.base64ToBytes(base64String)
            results["base64Conversion"] = bytes.contentEquals(backToBytes)

            // Test validation functions
            results["hexValidation"] = validateFormat("ABCDEF123456", FORMAT_HEX)
            results["base64Validation"] = validateFormat(base64String, FORMAT_BASE64)

            val keyStrength = validateKeyStrength(paymentKey)
            results["keyStrengthValidation"] = keyStrength.values.all { it }

            // Test custom implementation fallback
            val customHash = customSHA1Implementation(testData.toByteArray())
            val standardHash = calculateSHA1(testData.toByteArray())
            results["customImplementation"] = customHash.contentEquals(standardHash)

            val customHMAC = customHMACImplementation(data, key)
            val standardHMAC = calculateHMAC(data, key)
            results["customHMAC"] = customHMAC.contentEquals(standardHMAC)

        } catch (e: Exception) {
            println("Extended test failed: ${e.message}")
            e.printStackTrace()
        }

        return results
    }

    /**
     * Performance benchmark test
     */
    fun runPerformanceTest(iterations: Int = 1000): Map<String, Long> {
        val results = mutableMapOf<String, Long>()
        val testData = ByteArray(1024) // 1KB of data
        SecureRandom().nextBytes(testData)
        val key = generateRandomBytes(32)

        // SHA1 hashing performance
        val sha1Start = System.nanoTime()
        repeat(iterations) {
            calculateSHA1(testData)
        }
        results["sha1Hashing"] = (System.nanoTime() - sha1Start) / 1_000_000

        // HMAC performance
        val hmacStart = System.nanoTime()
        repeat(iterations) {
            calculateHMAC(testData, key)
        }
        results["hmacCalculation"] = (System.nanoTime() - hmacStart) / 1_000_000

        // Payment MAC performance
        val macStart = System.nanoTime()
        repeat(iterations) {
            generatePaymentMAC(testData, key)
        }
        results["paymentMAC"] = (System.nanoTime() - macStart) / 1_000_000

        // Key derivation performance
        val derivationStart = System.nanoTime()
        repeat(iterations / 10) { // Fewer iterations for expensive operations
            pbkdf1(key, testData.sliceArray(0..7), 100, 16)
        }
        results["keyDerivation"] = (System.nanoTime() - derivationStart) / 1_000_000

        // Padding performance
        val paddingStart = System.nanoTime()
        repeat(iterations) {
            applyPadding(testData, ISO_9797_1_METHOD_2)
        }
        results["paddingOperations"] = (System.nanoTime() - paddingStart) / 1_000_000

        return results
    }

    /**
     * Memory usage test
     */
    fun runMemoryTest(): Map<String, Long> {
        val results = mutableMapOf<String, Long>()
        val runtime = Runtime.getRuntime()

        // Test with large data sets
        val sizes = listOf(1024, 10240, 102400, 1048576) // 1KB to 1MB

        sizes.forEach { size ->
            runtime.gc()
            val beforeMemory = runtime.totalMemory() - runtime.freeMemory()

            val testData = ByteArray(size)
            SecureRandom().nextBytes(testData)
            val key = generateRandomBytes(32)

            // Perform various operations
            calculateSHA1(testData)
            calculateHMAC(testData, key)
            generatePaymentMAC(testData, key)

            runtime.gc()
            val afterMemory = runtime.totalMemory() - runtime.freeMemory()
            results["memoryUsage_${size}B"] = (afterMemory - beforeMemory) / 1024 // KB
        }

        return results
    }
}

