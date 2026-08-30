package `in`.aicortex.iso8583studio.data

import `in`.aicortex.iso8583studio.data.model.CipherMode
import `in`.aicortex.iso8583studio.data.model.CipherType
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class EncrDecrHandler(
    val cipherType: CipherType,
    var key: ByteArray,
    val iv: ByteArray,
    val cipherMode: CipherMode
) {
    private val cipher: Cipher

    init {
        // Initialize the cipher based on type and mode
        val algorithm = when (cipherType) {
            CipherType.DES -> "DES"
            CipherType.RSA -> "RSA"
            CipherType.TRIPLE_DES,  -> "DESede"
            CipherType.AES_128, CipherType.AES_192, CipherType.AES_256 -> "AES"
        }

        val transformationMode = when (cipherMode) {
            CipherMode.CBC -> "CBC"
            CipherMode.ECB -> "ECB"
            CipherMode.CFB -> "CFB"
            CipherMode.OFB -> "OFB"
            CipherMode.CTS -> "CTS"
        }

        val padding = "PKCS5Padding"
        val transformation = if (cipherMode == CipherMode.ECB)
            "$algorithm/$transformationMode/$padding"
        else
            "$algorithm/$transformationMode/$padding"

        cipher = Cipher.getInstance(transformation)
    }

    val blockSize: Int
        get() = cipher.blockSize

    fun encrypt(data: ByteArray): ByteArray {
        return encrypt(data, key)
    }

    fun encrypt(data: ByteArray, encKey: ByteArray): ByteArray {
        val keySpec = createKeySpec(encKey)

        if (cipherMode == CipherMode.ECB) {
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        } else {
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        }

        return cipher.doFinal(data)
    }

    fun decrypt(data: ByteArray): ByteArray {
        return decrypt(data, key)
    }

    fun decrypt(data: ByteArray, from: Int, length: Int): ByteArray {
        val dataToDecrypt = data.copyOfRange(from, from + length)
        return decrypt(dataToDecrypt, key)
    }

    fun decrypt(data: ByteArray, decKey: ByteArray): ByteArray {
        val keySpec = createKeySpec(decKey)

        if (cipherMode == CipherMode.ECB) {
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
        } else {
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        }

        return cipher.doFinal(data)
    }

    private fun createKeySpec(keyData: ByteArray): SecretKeySpec {
        // Format the key according to the cipher type
        val algorithm = when (cipherType) {
            CipherType.DES -> "DES"
            CipherType.RSA -> "RSA"
            CipherType.TRIPLE_DES-> "DESede"
            CipherType.AES_128, CipherType.AES_192, CipherType.AES_256 -> "AES"
        }

        // For Triple DES, ensure the key has the right format
        val formattedKey = when (cipherType) {
            CipherType.TRIPLE_DES -> {
                // Convert 16-byte key to 24-byte key by repeating first 8 bytes
                val result = ByteArray(24)
                keyData.copyInto(result, 0, 0, 16)
                keyData.copyInto(result, 16, 0, 8)
                result
            }
            else -> keyData
        }

        return SecretKeySpec(formattedKey, algorithm)
    }
}
