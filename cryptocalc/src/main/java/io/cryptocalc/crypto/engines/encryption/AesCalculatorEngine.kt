package io.cryptocalc.crypto.engines.encryption

import ai.cortex.core.types.CipherMode
import ai.cortex.core.types.PaddingMethods
import ai.cortex.core.types.PaddingMethods.*
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.macs.CMac
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object AesCalculatorEngine {

    init {
        Security.getProvider("BC") ?: Security.addProvider(BouncyCastleProvider())
    }

    fun encryptECB(data: ByteArray, key: ByteArray, padding: PaddingMethods = NONE): ByteArray {
        return performCipherOperation(data, key, null, Cipher.ENCRYPT_MODE, CipherMode.ECB, padding)
    }

    fun decryptECB(data: ByteArray, key: ByteArray, padding: PaddingMethods = NONE): ByteArray {
        return performCipherOperation(data, key, null, Cipher.DECRYPT_MODE, CipherMode.ECB, padding)
    }

    fun encryptCBC(data: ByteArray, key: ByteArray, iv: ByteArray? = null, padding: PaddingMethods = NONE): ByteArray {
        return performCipherOperation(data, key, iv, Cipher.ENCRYPT_MODE, CipherMode.CBC, padding)
    }

    fun decryptCBC(data: ByteArray, key: ByteArray, iv: ByteArray? = null, padding: PaddingMethods = NONE): ByteArray {
        return performCipherOperation(data, key, iv, Cipher.DECRYPT_MODE, CipherMode.CBC, padding)
    }

    /**
     * Compute AES-CMAC (RFC 4493 / NIST SP 800-38B) over [data] using [key].
     * Returns the full 16-byte CMAC.
     * @param key 16, 24, or 32 bytes (AES-128, AES-192, AES-256)
     */
    fun computeCmac(data: ByteArray, key: ByteArray): ByteArray {
        validateKey(key)
        val cmac = CMac(AESEngine(), 128)
        cmac.init(KeyParameter(key))
        cmac.update(data, 0, data.size)
        val result = ByteArray(16)
        cmac.doFinal(result, 0)
        return result
    }

    private fun performCipherOperation(
        data: ByteArray,
        key: ByteArray,
        iv: ByteArray?,
        mode: Int,
        cipherMode: CipherMode,
        padding: PaddingMethods = NONE
    ): ByteArray {
        validateKey(key)
        val effectiveIV = iv ?: ByteArray(16) { 0 }

        val paddingSuffix = when (padding) {
            PKCS5 -> "PKCS5Padding"
            PKCS7 -> "PKCS7Padding"
            else -> "NoPadding"
        }
        val transformation = "AES/$cipherMode/$paddingSuffix"
        val keySpec = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance(transformation, "BC")

        if (cipherMode == CipherMode.ECB) {
            cipher.init(mode, keySpec)
        } else {
            val ivSpec = IvParameterSpec(effectiveIV)
            cipher.init(mode, keySpec, ivSpec)
        }
        return cipher.doFinal(data)
    }

    private fun validateKey(key: ByteArray) {
        require(key.size in listOf(16, 24, 32)) {
            "Invalid AES key size: ${key.size}. Key must be 16, 24, or 32 bytes."
        }
    }
}
