package ai.cortex.core.types

import kotlinx.serialization.Serializable

@Serializable
enum class CipherMode {
    ECB, CBC, CFB, OFB, GCM, CTR
}

@Serializable
sealed class CryptoAlgorithm<T : AlgorithmType>(
    val detail: String,
    val type: T,
    val keySizes: List<Int> = emptyList(),
    val blockSize: Int? = null,
    val outputSize: Int? = null,
    val standards: List<String> = emptyList(),
    val paymentIndustryUse: Boolean = false,
    val emvCompatible: Boolean = false,
    val hsmSupport: Boolean = true,
    val deprecated: Boolean = false
) {

    /**
     * Represents the Advanced Encryption Standard (AES) algorithm.
     *
     * AES is a symmetric block cipher widely adopted as the encryption standard by the U.S. government.
     * It operates on fixed-size blocks of data (128 bits) and uses symmetric keys of 128, 192, or 256 bits.
     *
     * @property detail Advanced Encryption Standard.
     * @property type [AlgorithmType.SYMMETRIC_BLOCK].
     * @property keySizes 128, 192, 256.
     * @property blockSize (128).
     * @property standards NIST FIPS 197, ISO/IEC 18033-3.
     * @property paymentIndustryUse true.
     * @property emvCompatible true.
     */// ============================================================================
    // SYMMETRIC BLOCK CIPHERS
    // ============================================================================
    object AES : CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>(
        detail = "Advanced Encryption Standard",
        type = AlgorithmType.SYMMETRIC_BLOCK,
        keySizes = listOf(128, 192, 256),
        blockSize = 128,
        standards = listOf("NIST FIPS 197", "ISO/IEC 18033-3"),
        paymentIndustryUse = true,
        emvCompatible = true
    )

    /**
     * Represents the Data Encryption Standard (DES) algorithm.
     *
     * DES is a symmetric block cipher widely adopted as the encryption standard by the U.S. government.
     *
     * @property detail Data Encryption Standard.
     * @property type [AlgorithmType.SYMMETRIC_BLOCK].
     * @property keySizes 8, 16, 24.
     * @property blockSize (16).
     * @property standards NIST FIPS 46-3, ISO/IEC 18033-3.
     * @property paymentIndustryUse true.
     * @property emvCompatible true.
     * @property deprecated true.
     */
    object DES : CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>(
        detail = "Data Encryption Standard",
        type = AlgorithmType.SYMMETRIC_BLOCK,
        keySizes = listOf(8, 16, 24),
        blockSize = 16,
        standards = listOf("NIST FIPS 46-3", "ISO/IEC 18033-3"),
        paymentIndustryUse = true,
        emvCompatible = true,
        deprecated = true
    )

    /**
     * Represents the Triple Data Encryption Standard (TDES) algorithm.
     *
     * TDES is a symmetric block cipher widely adopted as the encryption standard by the U.S. government.
     *
     * @property detail Triple Data Encryption Standard.
     * @property type [AlgorithmType.SYMMETRIC_BLOCK].
     * @property keySizes 8, 16, 24.
     * @property blockSize (16).
     * @property standards NIST SP 800-67, ISO/IEC 18033-3.
     * @property paymentIndustryUse true.
     * @property emvCompatible true.
     */
    object TDES : CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>(
        detail = "Triple Data Encryption Standard",
        type = AlgorithmType.SYMMETRIC_BLOCK,
        keySizes = listOf(8, 16, 24),
        blockSize = 16,
        standards = listOf("NIST SP 800-67", "ISO/IEC 18033-3"),
        paymentIndustryUse = true,
        emvCompatible = true
    )

    object BLOWFISH : CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>(
        detail = "Blowfish symmetric cipher",
        type = AlgorithmType.SYMMETRIC_BLOCK,
        keySizes = (32..448 step 8).toList(),
        blockSize = 64,
        standards = listOf("Bruce Schneier")
    )

    object TWOFISH : CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>(
        detail = "Twofish symmetric cipher",
        type = AlgorithmType.SYMMETRIC_BLOCK,
        keySizes = listOf(128, 192, 256),
        blockSize = 128,
        standards = listOf("AES Finalist")
    )

    object SERPENT : CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>(
        detail = "Serpent symmetric cipher",
        type = AlgorithmType.SYMMETRIC_BLOCK,
        keySizes = listOf(128, 192, 256),
        blockSize = 128,
        standards = listOf("AES Finalist")
    )

    object CAMELLIA : CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>(
        detail = "Camellia symmetric cipher",
        type = AlgorithmType.SYMMETRIC_BLOCK,
        keySizes = listOf(128, 192, 256),
        blockSize = 128,
        standards = listOf("ISO/IEC 18033-3", "RFC 3713")
    )

    object ARIA : CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>(
        detail = "ARIA symmetric cipher",
        type = AlgorithmType.SYMMETRIC_BLOCK,
        keySizes = listOf(128, 192, 256),
        blockSize = 128,
        standards = listOf("RFC 5794", "KS X 1213")
    )

    object SEED : CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>(
        detail = "SEED symmetric cipher",
        type = AlgorithmType.SYMMETRIC_BLOCK,
        keySizes = listOf(128),
        blockSize = 128,
        standards = listOf("RFC 4269", "KS X 1004")
    )

    // ============================================================================
    // ASYMMETRIC / PUBLIC KEY ALGORITHMS
    // ============================================================================

    object RSA : CryptoAlgorithm<AlgorithmType.ASYMMETRIC>(
        detail = "RSA public key cryptosystem",
        type = AlgorithmType.ASYMMETRIC,
        keySizes = listOf(1024, 2048, 3072, 4096, 8192),
        standards = listOf("RFC 3447", "NIST SP 800-56B"),
        paymentIndustryUse = true,
        emvCompatible = true
    )

    object DSA : CryptoAlgorithm<AlgorithmType.ASYMMETRIC>(
        detail = "Digital Signature Algorithm",
        type = AlgorithmType.ASYMMETRIC,
        keySizes = listOf(1024, 2048, 3072),
        standards = listOf("NIST FIPS 186-4")
    )

    object ECDSA : CryptoAlgorithm<AlgorithmType.ASYMMETRIC>(
        detail = "Elliptic Curve Digital Signature Algorithm",
        type = AlgorithmType.ASYMMETRIC,
        keySizes = listOf(224, 256, 384, 521),
        standards = listOf("NIST FIPS 186-4", "ISO/IEC 14888-3"),
        paymentIndustryUse = true,
        emvCompatible = true
    )

    object ECDH : CryptoAlgorithm<AlgorithmType.KEY_EXCHANGE>(
        detail = "Elliptic Curve Diffie-Hellman",
        type = AlgorithmType.KEY_EXCHANGE,
        keySizes = listOf(224, 256, 384, 521),
        standards = listOf("NIST SP 800-56A", "RFC 3279")
    )

    object ECIES : CryptoAlgorithm<AlgorithmType.ASYMMETRIC>(
        detail = "Elliptic Curve Integrated Encryption Scheme",
        type = AlgorithmType.ASYMMETRIC,
        keySizes = listOf(224, 256, 384, 521),
        standards = listOf("IEEE 1363", "ISO/IEC 18033-2")
    )

    object DH : CryptoAlgorithm<AlgorithmType.KEY_EXCHANGE>(
        detail = "Diffie-Hellman key exchange",
        type = AlgorithmType.KEY_EXCHANGE,
        keySizes = listOf(1024, 2048, 3072, 4096),
        standards = listOf("RFC 2631", "NIST SP 800-56A")
    )

    object ELGAMAL : CryptoAlgorithm<AlgorithmType.ASYMMETRIC>(
        detail = "ElGamal encryption",
        type = AlgorithmType.ASYMMETRIC,
        keySizes = listOf(1024, 2048, 3072, 4096),
        standards = listOf("Taher ElGamal")
    )

    // ============================================================================
    // HASH FUNCTIONS
    // ============================================================================

    object MD5 : CryptoAlgorithm<AlgorithmType.HASH>(
        detail = "Message Digest 5",
        type = AlgorithmType.HASH,
        outputSize = 128,
        standards = listOf("RFC 1321"),
        deprecated = true
    )

    object SHA1 : CryptoAlgorithm<AlgorithmType.HASH>(
        detail = "Secure Hash Algorithm 1",
        type = AlgorithmType.HASH,
        outputSize = 160,
        standards = listOf("NIST FIPS 180-4", "RFC 3174"),
        paymentIndustryUse = true,
        deprecated = true
    )

    object SHA224 : CryptoAlgorithm<AlgorithmType.HASH>(
        detail = "Secure Hash Algorithm 224",
        type = AlgorithmType.HASH,
        outputSize = 224,
        standards = listOf("NIST FIPS 180-4")
    )

    object SHA256 : CryptoAlgorithm<AlgorithmType.HASH>(
        detail = "Secure Hash Algorithm 256",
        type = AlgorithmType.HASH,
        outputSize = 256,
        standards = listOf("NIST FIPS 180-4", "RFC 6234"),
        paymentIndustryUse = true,
        emvCompatible = true
    )

    object SHA384 : CryptoAlgorithm<AlgorithmType.HASH>(
        detail = "Secure Hash Algorithm 384",
        type = AlgorithmType.HASH,
        outputSize = 384,
        standards = listOf("NIST FIPS 180-4")
    )

    object SHA512 : CryptoAlgorithm<AlgorithmType.HASH>(
        detail = "Secure Hash Algorithm 512",
        type = AlgorithmType.HASH,
        outputSize = 512,
        standards = listOf("NIST FIPS 180-4", "RFC 6234"),
        paymentIndustryUse = true
    )

    object SHA3_224 : CryptoAlgorithm<AlgorithmType.HASH>(
        detail = "SHA-3 with 224-bit output",
        type = AlgorithmType.HASH,
        outputSize = 224,
        standards = listOf("NIST FIPS 202")
    )

    object SHA3_256 : CryptoAlgorithm<AlgorithmType.HASH>(
        detail = "SHA-3 with 256-bit output",
        type = AlgorithmType.HASH,
        outputSize = 256,
        standards = listOf("NIST FIPS 202")
    )

    object SHA3_384 : CryptoAlgorithm<AlgorithmType.HASH>(
        detail = "SHA-3 with 384-bit output",
        type = AlgorithmType.HASH,
        outputSize = 384,
        standards = listOf("NIST FIPS 202")
    )

    object SHA3_512 : CryptoAlgorithm<AlgorithmType.HASH>(
        detail = "SHA-3 with 512-bit output",
        type = AlgorithmType.HASH,
        outputSize = 512,
        standards = listOf("NIST FIPS 202")
    )

    object KECCAK_256 : CryptoAlgorithm<AlgorithmType.HASH>(
        detail = "Keccak-256 (pre-standardization SHA-3)",
        type = AlgorithmType.HASH,
        outputSize = 256,
        standards = listOf("Keccak Team")
    )

    object BLAKE2B : CryptoAlgorithm<AlgorithmType.HASH>(
        detail = "BLAKE2b hash function",
        type = AlgorithmType.HASH,
        outputSize = 512,
        standards = listOf("RFC 7693")
    )

    object BLAKE2S : CryptoAlgorithm<AlgorithmType.HASH>(
        detail = "BLAKE2s hash function",
        type = AlgorithmType.HASH,
        outputSize = 256,
        standards = listOf("RFC 7693")
    )

    object RIPEMD160 : CryptoAlgorithm<AlgorithmType.HASH>(
        detail = "RIPEMD-160 hash function",
        type = AlgorithmType.HASH,
        outputSize = 160,
        standards = listOf("ISO/IEC 10118-3")
    )

    object WHIRLPOOL : CryptoAlgorithm<AlgorithmType.HASH>(
        detail = "Whirlpool hash function",
        type = AlgorithmType.HASH,
        outputSize = 512,
        standards = listOf("ISO/IEC 10118-3")
    )

    // ============================================================================
    // MESSAGE AUTHENTICATION CODES (MAC)
    // ============================================================================

    object HMAC_MD5 : CryptoAlgorithm<AlgorithmType.MAC>(
        detail = "HMAC with MD5",
        type = AlgorithmType.MAC,
        outputSize = 128,
        standards = listOf("RFC 2104"),
        deprecated = true
    )

    object HMAC_SHA1 : CryptoAlgorithm<AlgorithmType.MAC>(
        detail = "HMAC with SHA-1",
        type = AlgorithmType.MAC,
        outputSize = 160,
        standards = listOf("RFC 2104", "NIST FIPS 198-1"),
        paymentIndustryUse = true
    )

    object HMAC_SHA256 : CryptoAlgorithm<AlgorithmType.MAC>(
        detail = "HMAC with SHA-256",
        type = AlgorithmType.MAC,
        outputSize = 256,
        standards = listOf("RFC 2104", "NIST FIPS 198-1"),
        paymentIndustryUse = true,
        emvCompatible = true
    )

    object HMAC_SHA384 : CryptoAlgorithm<AlgorithmType.MAC>(
        detail = "HMAC with SHA-384",
        type = AlgorithmType.MAC,
        outputSize = 384,
        standards = listOf("RFC 2104", "NIST FIPS 198-1")
    )

    object HMAC_SHA512 : CryptoAlgorithm<AlgorithmType.MAC>(
        detail = "HMAC with SHA-512",
        type = AlgorithmType.MAC,
        outputSize = 512,
        standards = listOf("RFC 2104", "NIST FIPS 198-1"),
        paymentIndustryUse = true
    )

    object CMAC_AES : CryptoAlgorithm<AlgorithmType.MAC>(
        detail = "CMAC with AES",
        type = AlgorithmType.MAC,
        keySizes = listOf(128, 192, 256),
        outputSize = 128,
        standards = listOf("NIST SP 800-38B", "RFC 4493"),
        paymentIndustryUse = true,
        emvCompatible = true
    )

    object CMAC_TDES : CryptoAlgorithm<AlgorithmType.MAC>(
        detail = "CMAC with Triple DES",
        type = AlgorithmType.MAC,
        keySizes = listOf(112, 168),
        outputSize = 64,
        standards = listOf("NIST SP 800-38B"),
        paymentIndustryUse = true
    )

    object GMAC : CryptoAlgorithm<AlgorithmType.MAC>(
        detail = "Galois Message Authentication Code",
        type = AlgorithmType.MAC,
        keySizes = listOf(128, 192, 256),
        outputSize = 128,
        standards = listOf("NIST SP 800-38D")
    )

    // ============================================================================
    // STREAM CIPHERS
    // ============================================================================

    object RC4 : CryptoAlgorithm<AlgorithmType.STREAM>(
        detail = "RC4 stream cipher",
        type = AlgorithmType.STREAM,
        keySizes = (40..2048 step 8).toList(),
        standards = listOf("RFC 6229"),
        deprecated = true
    )

    object CHACHA20 : CryptoAlgorithm<AlgorithmType.STREAM>(
        detail = "ChaCha20 stream cipher",
        type = AlgorithmType.STREAM,
        keySizes = listOf(256),
        standards = listOf("RFC 8439")
    )

    object SALSA20 : CryptoAlgorithm<AlgorithmType.STREAM>(
        detail = "Salsa20 stream cipher",
        type = AlgorithmType.STREAM,
        keySizes = listOf(128, 256),
        standards = listOf("Daniel J. Bernstein")
    )

    // ============================================================================
    // KEY DERIVATION FUNCTIONS
    // ============================================================================

    object PBKDF2 : CryptoAlgorithm<AlgorithmType.KDF>(
        detail = "Password-Based Key Derivation Function 2",
        type = AlgorithmType.KDF,
        standards = listOf("RFC 2898", "NIST SP 800-132"),
        paymentIndustryUse = true
    )

    object SCRYPT : CryptoAlgorithm<AlgorithmType.KDF>(
        detail = "Scrypt key derivation function",
        type = AlgorithmType.KDF,
        standards = listOf("RFC 7914")
    )

    object ARGON2 : CryptoAlgorithm<AlgorithmType.KDF>(
        detail = "Argon2 key derivation function",
        type = AlgorithmType.KDF,
        standards = listOf("RFC 9106")
    )

    object HKDF : CryptoAlgorithm<AlgorithmType.KDF>(
        detail = "HMAC-based Key Derivation Function",
        type = AlgorithmType.KDF,
        standards = listOf("RFC 5869")
    )

    object BCRYPT : CryptoAlgorithm<AlgorithmType.KDF>(
        detail = "bcrypt password hashing function",
        type = AlgorithmType.KDF,
        standards = listOf("Niels Provos")
    )

    // ============================================================================
    // PAYMENT INDUSTRY SPECIFIC ALGORITHMS
    // ============================================================================
    object CVV_ALGORITHM : CryptoAlgorithm<AlgorithmType.PAYMENT_SPECIFIC>(
        detail = "Card Verification Value algorithm",
        type = AlgorithmType.PAYMENT_SPECIFIC,
        standards = listOf("ISO/IEC 7813"),
        paymentIndustryUse = true,
        emvCompatible = true
    )

    object CVC_ALGORITHM : CryptoAlgorithm<AlgorithmType.PAYMENT_SPECIFIC>(
        detail = "Card Validation Code algorithm",
        type = AlgorithmType.PAYMENT_SPECIFIC,
        standards = listOf("ISO/IEC 7813"),
        paymentIndustryUse = true,
        emvCompatible = true
    )

    object PIN_VERIFICATION_ALGORITHM : CryptoAlgorithm<AlgorithmType.PAYMENT_SPECIFIC>(
        detail = "PIN verification algorithm",
        type = AlgorithmType.PAYMENT_SPECIFIC,
        standards = listOf("ISO 9564"),
        paymentIndustryUse = true,
        emvCompatible = true
    )

    object DUKPT : CryptoAlgorithm<AlgorithmType.KEY_MANAGEMENT>(
        detail = "Derived Unique Key Per Transaction",
        type = AlgorithmType.KEY_MANAGEMENT,
        standards = listOf("ANSI X9.24"),
        paymentIndustryUse = true
    )

    object EMV_MAC : CryptoAlgorithm<AlgorithmType.MAC>(
        detail = "EMV Message Authentication Code",
        type = AlgorithmType.MAC,
        standards = listOf("EMV 4.3"),
        paymentIndustryUse = true,
        emvCompatible = true
    )

    object EMV_CRYPTOGRAM : CryptoAlgorithm<AlgorithmType.PAYMENT_SPECIFIC>(
        detail = "EMV Application Cryptogram",
        type = AlgorithmType.PAYMENT_SPECIFIC,
        standards = listOf("EMV 4.3"),
        paymentIndustryUse = true,
        emvCompatible = true
    )

    // ============================================================================
    // SMARTCARD SPECIFIC ALGORITHMS
    //  ============================================================================
    object ISO_7816_MAC : CryptoAlgorithm<AlgorithmType.MAC>(
        detail = "ISO 7816 MAC algorithm",
        type = AlgorithmType.MAC,
        standards = listOf("ISO/IEC 7816-4"),
        paymentIndustryUse = true,
        emvCompatible = true
    )

    object GLOBAL_PLATFORM_SCP02 : CryptoAlgorithm<AlgorithmType.KEY_MANAGEMENT>(
        detail = "GlobalPlatform SCP02 algorithm",
        type = AlgorithmType.KEY_MANAGEMENT,
        standards = listOf("GlobalPlatform SCP02"),
        paymentIndustryUse = true
    )

    object GLOBAL_PLATFORM_SCP03 : CryptoAlgorithm<AlgorithmType.KEY_MANAGEMENT>(
        detail = "GlobalPlatform SCP03 algorithm",
        type = AlgorithmType.KEY_MANAGEMENT,
        standards = listOf("GlobalPlatform SCP03"),
        paymentIndustryUse = true
    )

    // ============================================================================
    // POST-QUANTUM CRYPTOGRAPHY
    //  ============================================================================
    object CRYSTALS_KYBER : CryptoAlgorithm<AlgorithmType.POST_QUANTUM>(
        detail = "CRYSTALS-Kyber post-quantum KEM",
        type = AlgorithmType.POST_QUANTUM,
        keySizes = listOf(512, 768, 1024),
        standards = listOf("NIST Post-Quantum Cryptography")
    )

    object CRYSTALS_DILITHIUM : CryptoAlgorithm<AlgorithmType.POST_QUANTUM>(
        detail = "CRYSTALS-Dilithium post-quantum signature",
        type = AlgorithmType.POST_QUANTUM,
        standards = listOf("NIST Post-Quantum Cryptography")
    )

    object FALCON : CryptoAlgorithm<AlgorithmType.POST_QUANTUM>(
        detail = "FALCON post-quantum signature",
        type = AlgorithmType.POST_QUANTUM,
        standards = listOf("NIST Post-Quantum Cryptography")
    )

    object SPHINCS_PLUS : CryptoAlgorithm<AlgorithmType.POST_QUANTUM>(
        detail = "SPHINCS+ post-quantum signature",
        type = AlgorithmType.POST_QUANTUM,
        standards = listOf("NIST Post-Quantum Cryptography")
    )

    // ============================================================================
    // AUTHENTICATED ENCRYPTION
    // ============================================================================
    object AES_GCM : CryptoAlgorithm<AlgorithmType.AUTHENTICATED_ENCRYPTION>(
        detail = "AES Galois/Counter Mode",
        type = AlgorithmType.AUTHENTICATED_ENCRYPTION,
        keySizes = listOf(128, 192, 256),
        blockSize = 128,
        standards = listOf("NIST SP 800-38D"),
        paymentIndustryUse = true
    )

    object AES_CCM : CryptoAlgorithm<AlgorithmType.AUTHENTICATED_ENCRYPTION>(
        detail = "AES Counter with CBC-MAC",
        type = AlgorithmType.AUTHENTICATED_ENCRYPTION,
        keySizes = listOf(128, 192, 256),
        blockSize = 128,
        standards = listOf("NIST SP 800-38C")
    )

    object CHACHA20_POLY1305 : CryptoAlgorithm<AlgorithmType.AUTHENTICATED_ENCRYPTION>(
        detail = "ChaCha20-Poly1305 AEAD",
        type = AlgorithmType.AUTHENTICATED_ENCRYPTION,
        keySizes = listOf(256),
        standards = listOf("RFC 8439")
    )

    // ============================================================================
    // LEGACY ALGORITHMS
    // ============================================================================

    object IDEA : CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>(
        detail = "International Data Encryption Algorithm",
        type = AlgorithmType.SYMMETRIC_BLOCK,
        keySizes = listOf(128),
        blockSize = 64,
        standards = listOf("IDEA"),
        deprecated = true
    )

    object RC2 : CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>(
        detail = "RC2 block cipher",
        type = AlgorithmType.SYMMETRIC_BLOCK,
        keySizes = (8..1024 step 8).toList(),
        blockSize = 64,
        standards = listOf("RFC 2268"),
        deprecated = true
    )

    object RC5 : CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>(
        detail = "RC5 block cipher",
        type = AlgorithmType.SYMMETRIC_BLOCK,
        keySizes = (0..2040 step 8).toList(),
        blockSize = 64,
        standards = listOf("RFC 2040")
    )

    object CAST5 : CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>(
        detail = "CAST-128 block cipher",
        type = AlgorithmType.SYMMETRIC_BLOCK,
        keySizes = (40..128 step 8).toList(),
        blockSize = 64,
        standards = listOf("RFC 2144")
    );

    // Helper methods
    fun isSymmetric(): Boolean = type in listOf(
        AlgorithmType.SYMMETRIC_BLOCK, AlgorithmType.STREAM
    )

    fun isAsymmetric(): Boolean = type == AlgorithmType.ASYMMETRIC

    fun isHash(): Boolean = type == AlgorithmType.HASH

    fun isMAC(): Boolean = type == AlgorithmType.MAC

    fun isPaymentIndustryStandard(): Boolean = paymentIndustryUse

    fun isEMVCompatible(): Boolean = emvCompatible

    fun isDeprecated(): Boolean = deprecated

    fun getDefaultKeySize(): Int? = keySizes.firstOrNull()

    fun supportsKeySize(size: Int): Boolean = keySizes.isEmpty() || size in keySizes

    fun getBlockSizeBytes(): Int? = blockSize?.div(8)

    fun getOutputSizeBytes(): Int? = outputSize?.div(8)

}

@Serializable
sealed interface AlgorithmType {
    @Serializable
    object SYMMETRIC_BLOCK : AlgorithmType

    @Serializable
    object SYMMETRIC_STREAM : AlgorithmType

    @Serializable
    object STREAM : AlgorithmType

    @Serializable
    object ASYMMETRIC : AlgorithmType

    @Serializable
    object HASH : AlgorithmType

    @Serializable
    object MAC : AlgorithmType

    @Serializable
    object KDF : AlgorithmType

    @Serializable
    object KEY_EXCHANGE : AlgorithmType

    @Serializable
    object KEY_MANAGEMENT : AlgorithmType

    @Serializable
    object AUTHENTICATED_ENCRYPTION : AlgorithmType

    @Serializable
    object PAYMENT_SPECIFIC : AlgorithmType

    @Serializable
    object POST_QUANTUM : AlgorithmType
}

@Serializable
enum class PaddingMethods(
    val description: String,
    val standardName: String,
    val blockSizeRequired: Boolean = true,
    val applicableAlgorithms: List<CryptoAlgorithm<*>> = listOf()
) {
    NONE(
        description = "No padding applied",
        standardName = "None",
        blockSizeRequired = false,
        applicableAlgorithms = listOf()
    ),

    // ============================================================================
    // ISO 9797 PADDING METHODS
    // ============================================================================
    METHOD_1_ISO_9797(
        description = "Padding with zeros to complete the block",
        standardName = "ISO/IEC 9797-1",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.DES, CryptoAlgorithm.TDES, CryptoAlgorithm.AES
        )
    ),

    METHOD_2_ISO_9797(
        description = "Padding with a single '1' bit followed by zeros",
        standardName = "ISO/IEC 9797-1",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.DES, CryptoAlgorithm.TDES, CryptoAlgorithm.AES
        )
    ),

    METHOD_3_ISO_9797(
        description = "Padding with length indicator followed by zeros",
        standardName = "ISO/IEC 9797-1",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.DES, CryptoAlgorithm.TDES, CryptoAlgorithm.AES
        )
    ),

    // ============================================================================
    // PKCS PADDING METHODS
    // ============================================================================
    PKCS1_V15(
        description = "PKCS #1 v1.5 padding for RSA encryption",
        standardName = "RFC 3447",
        blockSizeRequired = false,
        applicableAlgorithms = listOf(CryptoAlgorithm.RSA)
    ),

    PKCS1_OAEP(
        description = "PKCS #1 OAEP (Optimal Asymmetric Encryption Padding)",
        standardName = "RFC 3447",
        blockSizeRequired = false,
        applicableAlgorithms = listOf(CryptoAlgorithm.RSA)
    ),

    PKCS1_PSS(
        description = "PKCS #1 PSS (Probabilistic Signature Scheme)",
        standardName = "RFC 3447",
        blockSizeRequired = false,
        applicableAlgorithms = listOf(CryptoAlgorithm.RSA)
    ),

    PKCS5(
        description = "PKCS #5 padding (same as PKCS #7 for 8-byte blocks)",
        standardName = "PKCS5Padding",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.DES, CryptoAlgorithm.TDES
        )
    ),

    PKCS7(
        description = "PKCS #7 padding for any block size",
        standardName = "RFC 5652",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.DES,
            CryptoAlgorithm.TDES,
            CryptoAlgorithm.AES,
            CryptoAlgorithm.BLOWFISH,
        )
    ),

    // ============================================================================
    // ANSI PADDING METHODS
    // ============================================================================
    ANSI_X923(
        description = "ANSI X9.23 padding with zeros and length byte",
        standardName = "ANSI X9.23",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.DES,
            CryptoAlgorithm.TDES,
            CryptoAlgorithm.AES,
        )
    ),

    ANSI_X931(
        description = "ANSI X9.31 padding for digital signatures",
        standardName = "ANSI X9.31",
        blockSizeRequired = false,
        applicableAlgorithms = listOf(
            CryptoAlgorithm.RSA
        )
    ),

    // ============================================================================
    // ZERO PADDING METHODS
    // ============================================================================
    ZERO_PADDING(
        description = "Padding with zero bytes",
        standardName = "Common Practice",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.DES,
            CryptoAlgorithm.TDES,
            CryptoAlgorithm.AES,
        )
    ),

    NULL_PADDING(
        description = "No padding applied",
        standardName = "None",
        blockSizeRequired = false,
        applicableAlgorithms = listOf(
            CryptoAlgorithm.DES,
            CryptoAlgorithm.TDES,
            CryptoAlgorithm.AES,
        )
    ),

    // ============================================================================
    // ISO 10126 PADDING
    // ============================================================================
    ISO_10126(
        description = "Random padding with length byte at the end",
        standardName = "ISO/IEC 10126",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.DES,
            CryptoAlgorithm.TDES,
            CryptoAlgorithm.AES,
        )
    ),

    // ============================================================================
    // EMV SPECIFIC PADDING METHODS
    // ============================================================================
    EMV_PADDING_METHOD_1(
        description = "EMV padding method 1 (ISO 9797-1 Method 2)",
        standardName = "EMV 4.3",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.DES,
            CryptoAlgorithm.TDES,
        )
    ),

    EMV_PADDING_METHOD_2(
        description = "EMV padding method 2 for cryptograms",
        standardName = "EMV 4.3",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.DES,
            CryptoAlgorithm.TDES,
            CryptoAlgorithm.AES,
        )
    ),

    // ============================================================================
    // TLS/SSL PADDING
    // ============================================================================
    TLS_PADDING(
        description = "TLS/SSL padding scheme",
        standardName = "RFC 5246",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.TDES,
            CryptoAlgorithm.AES,
        )
    ),

    // ============================================================================
    // CRYPTOGRAPHIC MESSAGE SYNTAX (CMS) PADDING
    // ============================================================================
    CMS_PADDING(
        description = "Cryptographic Message Syntax padding",
        standardName = "RFC 5652",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.DES,
            CryptoAlgorithm.TDES,
            CryptoAlgorithm.AES,
        )
    ),

    // ============================================================================
    // PAYMENT CARD INDUSTRY SPECIFIC
    // ============================================================================
    PCI_DSS_PADDING(
        description = "PCI DSS compliant padding method",
        standardName = "PCI DSS",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.TDES,
            CryptoAlgorithm.AES,
        )
    ),

    // ============================================================================
    // MASTERCARD SPECIFIC PADDING
    // ============================================================================
    MASTERCARD_M_CHIP_PADDING(
        description = "MasterCard M/Chip specific padding",
        standardName = "MasterCard M/Chip",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.DES,
            CryptoAlgorithm.TDES,
        )
    ),

    // ============================================================================
    // VISA SPECIFIC PADDING
    // ============================================================================
    VISA_CVV_PADDING(
        description = "Visa CVV generation padding method",
        standardName = "Visa CVV",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.DES,
            CryptoAlgorithm.TDES,
        )
    ),

    VISA_PIN_PADDING(
        description = "Visa PIN verification padding method",
        standardName = "Visa PIN",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.DES,
            CryptoAlgorithm.TDES,
        )
    ),

    // ============================================================================
    // BANKING INDUSTRY SPECIFIC
    // ============================================================================
    DUKPT_PADDING(
        description = "Derived Unique Key Per Transaction padding",
        standardName = "ANSI X9.24",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.TDES,
        )
    ),

    MAC_PADDING_ISO_9797_1(
        description = "MAC padding as per ISO 9797-1",
        standardName = "ISO/IEC 9797-1",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.DES,
            CryptoAlgorithm.TDES,
            CryptoAlgorithm.AES,
        )
    ),

    MAC_PADDING_ISO_16609(
        description = "MAC padding as per ISO 16609",
        standardName = "ISO/IEC 16609",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.AES,
        )
    ),

    // ============================================================================
    // SMARTCARD SPECIFIC PADDING
    // ============================================================================
    ISO_7816_PADDING(
        description = "ISO 7816-4 padding for smartcard operations",
        standardName = "ISO/IEC 7816-4",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.DES,
            CryptoAlgorithm.TDES,
            CryptoAlgorithm.AES,
        )
    ),

    GLOBAL_PLATFORM_PADDING(
        description = "GlobalPlatform SCP padding methods",
        standardName = "GlobalPlatform",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.DES,
            CryptoAlgorithm.TDES,
            CryptoAlgorithm.AES,
        )
    ),

    // ============================================================================
    // CONTACTLESS PAYMENT SPECIFIC
    // ============================================================================
    ISO_14443_PADDING(
        description = "ISO 14443 contactless card padding",
        standardName = "ISO/IEC 14443",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.DES,
            CryptoAlgorithm.TDES,
            CryptoAlgorithm.AES,
        )
    ),

    NFC_PADDING(
        description = "NFC payment padding method",
        standardName = "NFC Forum",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.AES,
        )
    ),

    // ============================================================================
    // ADVANCED ENCRYPTION STANDARD SPECIFIC
    // ============================================================================
    AES_CBC_PADDING(
        description = "Standard AES CBC mode padding",
        standardName = "NIST SP 800-38A",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.AES,
        )
    ),

    AES_CTR_NO_PADDING(
        description = "AES CTR mode (no padding required)",
        standardName = "NIST SP 800-38A",
        blockSizeRequired = false,
        applicableAlgorithms = listOf(
            CryptoAlgorithm.AES,
        )
    ),

    AES_GCM_NO_PADDING(
        description = "AES GCM mode (no padding required)",
        standardName = "NIST SP 800-38D",
        blockSizeRequired = false,
        applicableAlgorithms = listOf(
            CryptoAlgorithm.AES,
        )
    ),

    // ============================================================================
    // DATA ENCRYPTION STANDARD SPECIFIC
    // ============================================================================
    DES_CBC_PADDING(
        description = "DES CBC mode standard padding",
        standardName = "NIST FIPS 46-3",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.DES,
        )
    ),

    TRIPLE_DES_PADDING(
        description = "Triple DES standard padding",
        standardName = "NIST SP 800-67",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.TDES,
        )
    ),

    // ============================================================================
    // HASH-BASED MESSAGE AUTHENTICATION CODE
    // ============================================================================
    HMAC_PADDING(
        description = "HMAC padding method",
        standardName = "RFC 2104",
        blockSizeRequired = false,
        applicableAlgorithms = listOf(
            CryptoAlgorithm.SHA1,
            CryptoAlgorithm.SHA256,
            CryptoAlgorithm.SHA512,

            )
    ),

    // ============================================================================
    // CUSTOM VENDOR SPECIFIC
    // ============================================================================
    SAFENET_PADDING(
        description = "SafeNet HSM specific padding",
        standardName = "SafeNet",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.AES,
            CryptoAlgorithm.DES,
            CryptoAlgorithm.TDES,
        )
    ),

    THALES_PADDING(
        description = "Thales HSM specific padding",
        standardName = "Thales",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.AES,
            CryptoAlgorithm.DES,
            CryptoAlgorithm.TDES,
        )
    ),

    UTIMACO_PADDING(
        description = "Utimaco HSM specific padding",
        standardName = "Utimaco",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.AES,
            CryptoAlgorithm.DES,
            CryptoAlgorithm.TDES,
        )
    ),

    // ============================================================================
    // LEGACY AND PROPRIETARY METHODS
    // ============================================================================
    PROPRIETARY_PADDING_1(
        description = "Custom proprietary padding method 1",
        standardName = "Proprietary",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.DES,
            CryptoAlgorithm.TDES,
            CryptoAlgorithm.AES,
        )
    ),

    PROPRIETARY_PADDING_2(
        description = "Custom proprietary padding method 2",
        standardName = "Proprietary",
        applicableAlgorithms = listOf(
            CryptoAlgorithm.DES,
            CryptoAlgorithm.TDES,
            CryptoAlgorithm.AES,
        )
    ),

    // ============================================================================
    // QUANTUM-RESISTANT PADDING
    // ============================================================================
    POST_QUANTUM_PADDING(
        description = "Post-quantum cryptography padding",
        standardName = "NIST Post-Quantum",
        blockSizeRequired = false,
        applicableAlgorithms = listOf(
            CryptoAlgorithm.CRYSTALS_KYBER,
            CryptoAlgorithm.CRYSTALS_DILITHIUM,
        )
    );

    // Helper methods
    fun isApplicableFor(algorithm: CryptoAlgorithm<*>): Boolean {
        return applicableAlgorithms.isEmpty() || applicableAlgorithms.any { it.equals(algorithm) }
    }

    fun requiresBlockSize(): Boolean = blockSizeRequired

    companion object {
        fun getForAlgorithm(algorithm: CryptoAlgorithm<*>): List<PaddingMethods> {
            return values().filter { it.isApplicableFor(algorithm) }
        }

        fun getByStandard(standard: String): List<PaddingMethods> {
            return values().filter { it.standardName.contains(standard, ignoreCase = true) }
        }

        fun getEMVCompatible(): List<PaddingMethods> {
            return listOf(
                METHOD_1_ISO_9797,
                METHOD_2_ISO_9797,
                EMV_PADDING_METHOD_1,
                EMV_PADDING_METHOD_2,
                PKCS5,
                PKCS7
            )
        }

        fun getPCICompliant(): List<PaddingMethods> {
            return listOf(
                PKCS7, AES_CBC_PADDING, PCI_DSS_PADDING, ISO_10126
            )
        }

        fun getPaymentIndustryStandard(): List<PaddingMethods> {
            return listOf(
                METHOD_1_ISO_9797,
                METHOD_2_ISO_9797,
                METHOD_3_ISO_9797,
                EMV_PADDING_METHOD_1,
                EMV_PADDING_METHOD_2,
                MASTERCARD_M_CHIP_PADDING,
                VISA_CVV_PADDING,
                VISA_PIN_PADDING,
                DUKPT_PADDING,
                MAC_PADDING_ISO_9797_1
            )
        }

        fun getSmartCardCompatible(): List<PaddingMethods> {
            return listOf(
                ISO_7816_PADDING, GLOBAL_PLATFORM_PADDING, METHOD_2_ISO_9797, PKCS7
            )
        }

        fun getContactlessCompatible(): List<PaddingMethods> {
            return listOf(
                ISO_14443_PADDING, NFC_PADDING, AES_CBC_PADDING, EMV_PADDING_METHOD_2
            )
        }
    }
}

@Serializable
enum class KeySize(val bits: Int) {
    AES_128(128), AES_192(192), AES_256(256), DES_56(56), DES_112(112), DES_168(168), RSA_1024(1024), RSA_2048(
        2048
    ),
    RSA_4096(4096)
}

@Serializable
data class CryptoOptions(
    val mode: CipherMode = CipherMode.CBC,
    val padding: PaddingMethods = PaddingMethods.PKCS7,
    val iv: String? = null,
    val aad: String? = null // For GCM mode
)
