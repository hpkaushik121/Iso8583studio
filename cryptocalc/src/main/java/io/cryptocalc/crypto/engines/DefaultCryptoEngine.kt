package io.cryptocalc.crypto.engines

import io.cryptocalc.crypto.CryptoEngine

class DefaultCryptoEngine() : CryptoEngine {
    override suspend fun encrypt(
        algorithm: String,
        data: ByteArray,
        key: ByteArray,
        options: Map<String, String>
    ): ByteArray {
        TODO("Not yet implemented")
    }

    override suspend fun decrypt(
        algorithm: String,
        data: ByteArray,
        key: ByteArray,
        options: Map<String, String>
    ): ByteArray {
        TODO("Not yet implemented")
    }

    override suspend fun hash(algorithm: String, data: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }

    override suspend fun hmac(
        algorithm: String,
        data: ByteArray,
        key: ByteArray
    ): ByteArray {
        TODO("Not yet implemented")
    }

    override suspend fun generateKey(
        algorithm: String,
        keySize: Int
    ): ByteArray {
        TODO("Not yet implemented")
    }

    override suspend fun deriveKey(
        algorithm: String,
        password: ByteArray,
        salt: ByteArray,
        iterations: Int,
        keyLength: Int
    ): ByteArray {
        TODO("Not yet implemented")
    }
}
