package `in`.aicortex.iso8583studio.domain.utils

import ai.cortex.core.IsoUtil
import ai.cortex.core.types.AlgorithmType
import ai.cortex.core.types.CipherMode
import ai.cortex.core.types.CryptoAlgorithm
import ai.cortex.core.types.Key
import io.cryptocalc.crypto.engines.encryption.EMVEngines
import io.cryptocalc.crypto.engines.encryption.models.SymmetricDecryptionEngineParameters
import io.cryptocalc.crypto.engines.encryption.models.SymmetricEncryptionEngineParameters

// ═══════════════════════════════════════════════════════════════════════════════════
// CRYPTO SERVICE — Delegates to EMVEngines (EncryptionEngine + KeysEngine)
//
// Pattern follows existing codebase:
//   val calculator = EMVEngines()
//   calculator.encryptionEngine.encrypt(CryptoAlgorithm.TDES, SymmetricEncryptionEngineParameters(...))
//
// Padding is handled at this layer because TdesCalculatorEngine uses NoPadding.
// ═══════════════════════════════════════════════════════════════════════════════════

object DesCryptoService {

    private val emvEngines = EMVEngines()

    // ── Mode Mapping ────────────────────────────────────────────────────────────
    // UI modes → ai.cortex.core.types.CipherMode
    // CFB-8/CFB-64 both map to CipherMode.CFB (TdesCalculatorEngine uses JCE CFB)
    // OFB-8/OFB-64 both map to CipherMode.OFB

    private fun mapToCipherMode(uiMode: String): CipherMode {
        return when (uiMode) {
            "ECB" -> CipherMode.ECB
            "CBC" -> CipherMode.CBC
            "CFB-8", "CFB-64" -> CipherMode.CFB
            "OFB-8", "OFB-64" -> CipherMode.OFB
            else -> throw IllegalArgumentException("Unsupported cipher mode: $uiMode")
        }
    }
    // ── Algorithm Selection ─────────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    private fun resolveAlgorithm(keySize: Int): CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK> {
        return (if (keySize == 8) CryptoAlgorithm.DES else CryptoAlgorithm.TDES)
    }


    // ── Padding ─────────────────────────────────────────────────────────────────
    // TdesCalculatorEngine (inside EMVEngines) uses JCE NoPadding, so all padding
    // must be applied before encrypt and removed after decrypt at this layer.

    fun applyPadding(data: ByteArray, paddingMethod: String, blockSize: Int = 8): ByteArray {
        return when (paddingMethod) {
            "None" -> {
                if (data.size % blockSize != 0) {
                    throw IllegalArgumentException(
                        "Data length (${data.size} bytes) is not a multiple of block size ($blockSize). " +
                                "Select a padding method or provide correctly sized data."
                    )
                }
                data
            }
            "Zeros" -> padToBlockSize(data, blockSize) { ByteArray(it) { 0x00 } }
            "Spaces" -> padToBlockSize(data, blockSize) { ByteArray(it) { 0x20 } }
            "ANSI X9.23" -> {
                val padLen = blockSize - (data.size % blockSize)
                val padding = ByteArray(padLen)
                java.security.SecureRandom().nextBytes(padding)
                padding[padLen - 1] = padLen.toByte()
                data + padding
            }
            "ISO 10126" -> {
                val padLen = blockSize - (data.size % blockSize)
                val padding = ByteArray(padLen)
                java.security.SecureRandom().nextBytes(padding)
                padding[padLen - 1] = padLen.toByte()
                data + padding
            }
            "PKCS#5", "PKCS#7", "Rijndael" -> {
                val padLen = blockSize - (data.size % blockSize)
                data + ByteArray(padLen) { padLen.toByte() }
            }
            "ISO7816-4", "ISO9797-1 (Method 2)" -> {
                val withMandatory = data + byteArrayOf(0x80.toByte())
                val rem = withMandatory.size % blockSize
                if (rem == 0) withMandatory
                else withMandatory + ByteArray(blockSize - rem) { 0x00 }
            }
            "ISO9797-1 (Method 1)" -> padToBlockSize(data, blockSize) { ByteArray(it) { 0x00 } }
            else -> throw IllegalArgumentException("Unknown padding method: $paddingMethod")
        }
    }

    private fun padToBlockSize(data: ByteArray, blockSize: Int, paddingFactory: (Int) -> ByteArray): ByteArray {
        val rem = data.size % blockSize
        if (rem == 0 && data.isNotEmpty()) return data
        return data + paddingFactory(blockSize - rem)
    }

    fun removePadding(data: ByteArray, paddingMethod: String): ByteArray {
        if (data.isEmpty()) return data
        return when (paddingMethod) {
            "None" -> data
            "Zeros", "ISO9797-1 (Method 1)" -> {
                var end = data.size
                while (end > 0 && data[end - 1] == 0x00.toByte()) end--
                data.copyOfRange(0, end)
            }
            "Spaces" -> {
                var end = data.size
                while (end > 0 && data[end - 1] == 0x20.toByte()) end--
                data.copyOfRange(0, end)
            }
            "ANSI X9.23", "ISO 10126", "PKCS#5", "PKCS#7", "Rijndael" -> {
                val padLen = data.last().toInt() and 0xFF
                if (padLen in 1..8 && padLen <= data.size) {
                    data.copyOfRange(0, data.size - padLen)
                } else data
            }
            "ISO7816-4", "ISO9797-1 (Method 2)" -> {
                var end = data.size - 1
                while (end >= 0 && data[end] == 0x00.toByte()) end--
                if (end >= 0 && data[end] == 0x80.toByte()) {
                    data.copyOfRange(0, end)
                } else data
            }
            else -> data
        }
    }



    // ── Core Encrypt via EMVEngines ─────────────────────────────────────────────

    suspend fun encrypt(
        data: ByteArray,
        key: ByteArray,
        iv: ByteArray?,
        uiMode: String,
        padding: String
    ): ByteArray {
        val paddedData = applyPadding(data, padding)
        val cipherMode = mapToCipherMode(uiMode)
        val algorithm = resolveAlgorithm(key.size)
        val effectiveIv = iv ?: ByteArray(8) { 0 }

        return emvEngines.encryptionEngine.encrypt(
            algorithm = algorithm,
            encryptionEngineParameters = SymmetricEncryptionEngineParameters(
                data = paddedData,
                key = key,
                iv = effectiveIv,
                mode = cipherMode
            )
        )
    }

    // ── Core Decrypt via EMVEngines ─────────────────────────────────────────────

    suspend fun decrypt(
        data: ByteArray,
        key: ByteArray,
        iv: ByteArray?,
        uiMode: String,
        padding: String
    ): ByteArray {
        if (data.size % 8 != 0) {
            throw IllegalArgumentException(
                "Ciphertext length (${data.size} bytes) is not a multiple of 8. Ensure valid encrypted data."
            )
        }

        val cipherMode = mapToCipherMode(uiMode)
        val algorithm = resolveAlgorithm(key.size)
        val effectiveIv = iv ?: ByteArray(8) { 0 }

        val decrypted = emvEngines.encryptionEngine.decrypt(
            algorithm = algorithm,
            decryptionEngineParameters = SymmetricDecryptionEngineParameters(
                data = data,
                key = key,
                iv = effectiveIv,
                mode = cipherMode
            )
        )

        return removePadding(decrypted, padding)
    }

    // ── KCV via KeysEngine ──────────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    suspend fun calculateKCV(keyBytes: ByteArray): String {
        val algorithm = resolveAlgorithm(keyBytes.size)
        val key = Key(value = keyBytes, cryptoAlgorithm = algorithm)
        val kcvBytes = emvEngines.keysEngine.calculateKcv(key)
        return IsoUtil.bytesToHex(kcvBytes)
    }
}
