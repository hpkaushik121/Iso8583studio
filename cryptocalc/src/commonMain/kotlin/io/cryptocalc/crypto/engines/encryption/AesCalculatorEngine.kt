package io.cryptocalc.crypto.engines.encryption

import ai.cortex.core.types.CipherMode
import ai.cortex.core.types.PaddingMethods
import ai.cortex.core.types.PaddingMethods.NONE
import io.cryptocalc.crypto.primitives.Aes
import io.cryptocalc.crypto.primitives.Cmac
import io.cryptocalc.crypto.primitives.Modes

/**
 * AES engine backed by the pure-Kotlin [Aes] block cipher, so results are identical on the
 * desktop (JVM) and in the browser (Wasm).
 */
object AesCalculatorEngine {

    fun encryptECB(data: ByteArray, key: ByteArray, padding: PaddingMethods = NONE): ByteArray =
        Modes.encrypt(Aes(key), data, CipherMode.ECB, null, padding)

    fun decryptECB(data: ByteArray, key: ByteArray, padding: PaddingMethods = NONE): ByteArray =
        Modes.decrypt(Aes(key), data, CipherMode.ECB, null, padding)

    fun encryptCBC(data: ByteArray, key: ByteArray, iv: ByteArray? = null, padding: PaddingMethods = NONE): ByteArray =
        Modes.encrypt(Aes(key), data, CipherMode.CBC, iv, padding)

    fun decryptCBC(data: ByteArray, key: ByteArray, iv: ByteArray? = null, padding: PaddingMethods = NONE): ByteArray =
        Modes.decrypt(Aes(key), data, CipherMode.CBC, iv, padding)

    fun encryptCFB(data: ByteArray, key: ByteArray, iv: ByteArray? = null): ByteArray =
        Modes.encrypt(Aes(key), data, CipherMode.CFB, iv, NONE)

    fun decryptCFB(data: ByteArray, key: ByteArray, iv: ByteArray? = null): ByteArray =
        Modes.decrypt(Aes(key), data, CipherMode.CFB, iv, NONE)

    fun encryptOFB(data: ByteArray, key: ByteArray, iv: ByteArray? = null): ByteArray =
        Modes.encrypt(Aes(key), data, CipherMode.OFB, iv, NONE)

    fun decryptOFB(data: ByteArray, key: ByteArray, iv: ByteArray? = null): ByteArray =
        Modes.decrypt(Aes(key), data, CipherMode.OFB, iv, NONE)

    /** Generic entry point used by the calculators: any supported [mode] with optional [iv] and [padding]. */
    fun encrypt(data: ByteArray, key: ByteArray, mode: CipherMode, iv: ByteArray? = null, padding: PaddingMethods = NONE): ByteArray =
        Modes.encrypt(Aes(key), data, mode, iv, padding)

    fun decrypt(data: ByteArray, key: ByteArray, mode: CipherMode, iv: ByteArray? = null, padding: PaddingMethods = NONE): ByteArray =
        Modes.decrypt(Aes(key), data, mode, iv, padding)

    /**
     * Compute AES-CMAC (RFC 4493 / NIST SP 800-38B) over [data] using [key].
     * Returns the full 16-byte CMAC.
     * @param key 16, 24, or 32 bytes (AES-128, AES-192, AES-256)
     */
    fun computeCmac(data: ByteArray, key: ByteArray): ByteArray = Cmac(Aes(key)).compute(data)

    /** Key check value: first 3 bytes of AES-encrypting a zero block. */
    fun calculateKCV(key: ByteArray): ByteArray = Aes(key).encryptBlock(ByteArray(16)).copyOf(3)
}
