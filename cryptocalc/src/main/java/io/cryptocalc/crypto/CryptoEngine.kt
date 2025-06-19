package io.cryptocalc.crypto

interface CryptoEngine {
    suspend fun encrypt(
        algorithm: String,
        data: ByteArray,
        key: ByteArray,
        options: Map<String, String> = emptyMap()
    ): ByteArray

    suspend fun decrypt(
        algorithm: String,
        data: ByteArray,
        key: ByteArray,
        options: Map<String, String> = emptyMap()
    ): ByteArray

    suspend fun hash(algorithm: String, data: ByteArray): ByteArray

    suspend fun hmac(algorithm: String, data: ByteArray, key: ByteArray): ByteArray

    suspend fun generateKey(algorithm: String, keySize: Int): ByteArray

    suspend fun deriveKey(
        algorithm: String,
        password: ByteArray,
        salt: ByteArray,
        iterations: Int,
        keyLength: Int
    ): ByteArray
}
