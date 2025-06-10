package ai.cortex.core.crypto

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

fun getSymmetricCipher(): SymmetricCipher = JvmSymmetricCipher

private object JvmSymmetricCipher : SymmetricCipher {
    override fun encrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val transformation = when (key.size) {
            8 -> "DES/CBC/NoPadding"
            16, 24 -> "DESede/CBC/NoPadding"
            else -> throw IllegalArgumentException("Unsupported key size: ${key.size}")
        }

        val algorithm = if (key.size == 8) "DES" else "DESede"
        val keySpec = SecretKeySpec(key, algorithm)
        val ivSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        return cipher.doFinal(data)
    }

    override fun decrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val transformation = when (key.size) {
            8 -> "DES/CBC/NoPadding"
            16, 24 -> "DESede/CBC/NoPadding"
            else -> throw IllegalArgumentException("Unsupported key size: ${key.size}")
        }

        val algorithm = if (key.size == 8) "DES" else "DESede"
        val keySpec = SecretKeySpec(key, algorithm)
        val ivSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        return cipher.doFinal(data)
    }

    override fun encryptEcb(data: ByteArray, key: ByteArray): ByteArray {
        val transformation = when (key.size) {
            8 -> "DES/ECB/NoPadding"
            16, 24 -> "DESede/ECB/NoPadding"
            else -> throw IllegalArgumentException("Unsupported key size: ${key.size}")
        }

        val algorithm = if (key.size == 8) "DES" else "DESede"
        val keySpec = SecretKeySpec(key, algorithm)
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        return cipher.doFinal(data)
    }

    override fun decryptEcb(data: ByteArray, key: ByteArray): ByteArray {
        val transformation = when (key.size) {
            8 -> "DES/ECB/NoPadding"
            16, 24 -> "DESede/ECB/NoPadding"
            else -> throw IllegalArgumentException("Unsupported key size: ${key.size}")
        }

        val algorithm = if (key.size == 8) "DES" else "DESede"
        val keySpec = SecretKeySpec(key, algorithm)
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.DECRYPT_MODE, keySpec)
        return cipher.doFinal(data)
    }
}