package ai.cortex.core.crypto

/**
 * Common interface for symmetric encryption operations
 */
interface SymmetricCipher {
    fun encrypt(data: ByteArray, key: ByteArray, iv: ByteArray = ByteArray(8)): ByteArray
    fun decrypt(data: ByteArray, key: ByteArray, iv: ByteArray = ByteArray(8)): ByteArray
    fun encryptEcb(data: ByteArray, key: ByteArray): ByteArray
    fun decryptEcb(data: ByteArray, key: ByteArray): ByteArray
}