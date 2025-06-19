package io.cryptocalc.core.types

import kotlinx.serialization.Serializable

@Serializable
enum class CipherMode {
    ECB, CBC, CFB, OFB, GCM, CTR
}

@Serializable
enum class PaddingScheme {
    NONE, PKCS5, PKCS7, ANSI_X923, ISO10126, ZERO_PADDING
}

@Serializable
enum class KeySize(val bits: Int) {
    AES_128(128), AES_192(192), AES_256(256),
    DES_56(56), DES_112(112), DES_168(168),
    RSA_1024(1024), RSA_2048(2048), RSA_4096(4096)
}

@Serializable
data class CryptoOptions(
    val mode: CipherMode = CipherMode.CBC,
    val padding: PaddingScheme = PaddingScheme.PKCS7,
    val iv: String? = null,
    val aad: String? = null // For GCM mode
)
