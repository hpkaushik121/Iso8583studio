package io.cryptocalc.hsm

import ai.cortex.core.hexToBytes
import ai.cortex.core.toHexUpper
import ai.cortex.core.types.CipherMode
import io.cryptocalc.crypto.primitives.Mac
import io.cryptocalc.crypto.primitives.Modes
import io.cryptocalc.crypto.primitives.Tdes

/**
 * Reference implementations of vendor key-wrapping conventions from public descriptions.
 * None of these has been verified against the vendor's own specification; the calculators
 * that use them say so.
 */
object VendorKeys {

    // ------------------------------------------------------------------ Futurex

    /**
     * Futurex Excrypt style: the key is TDES-ECB enciphered under the MFK (or KEK/BEK) after the
     * modifier byte has been XORed into every byte of the master key.
     */
    fun futurexEncrypt(key: ByteArray, masterKey: ByteArray, modifier: Int): ByteArray =
        Modes.encrypt(Tdes(futurexVariant(masterKey, modifier)), key, CipherMode.ECB)

    fun futurexDecrypt(encrypted: ByteArray, masterKey: ByteArray, modifier: Int): ByteArray =
        Modes.decrypt(Tdes(futurexVariant(masterKey, modifier)), encrypted, CipherMode.ECB)

    private fun futurexVariant(masterKey: ByteArray, modifier: Int): ByteArray {
        require(modifier in 0..0x1F) { "Modifier must be 0..1F" }
        return ByteArray(masterKey.size) { (masterKey[it].toInt() xor modifier).toByte() }
    }

    // ------------------------------------------------------------------ Atalla AKB

    /**
     * Atalla Key Block: `header,encryptedKey,mac`. The 8-character ASCII header is the variant:
     * its bytes are XORed into the MFK before the key is TDES-CBC enciphered (zero IV), and the
     * MAC is an ISO 9797-1 Algorithm 1 TDES MAC over header ‖ encrypted key under the same key.
     */
    fun atallaEncode(key: ByteArray, header: String, mfk: ByteArray): String {
        require(header.length == 8) { "AKB header must be 8 characters" }
        require(mfk.size == 16 || mfk.size == 24) { "MFK must be 32 or 48 hex characters" }
        val variant = atallaVariant(mfk, header)
        val enc = Modes.encrypt(Tdes(variant), key, CipherMode.CBC, ByteArray(8))
        val mac = Mac.algorithm1(Tdes(variant), header.encodeToByteArray() + enc, Mac.Padding.METHOD_1)
        return "$header,${enc.toHexUpper()},${mac.toHexUpper()}"
    }

    data class AtallaDecoded(val header: String, val key: ByteArray, val macValid: Boolean)

    fun atallaDecode(akb: String, mfk: ByteArray): AtallaDecoded {
        val parts = akb.trim().split(',')
        require(parts.size == 3 && parts[0].length == 8) { "AKB must be header,encryptedKey,mac" }
        val variant = atallaVariant(mfk, parts[0])
        val enc = parts[1].hexToBytes()
        val key = Modes.decrypt(Tdes(variant), enc, CipherMode.CBC, ByteArray(8))
        val mac = Mac.algorithm1(Tdes(variant), parts[0].encodeToByteArray() + enc, Mac.Padding.METHOD_1)
        return AtallaDecoded(parts[0], key, mac.toHexUpper().equals(parts[2].trim(), ignoreCase = true))
    }

    private fun atallaVariant(mfk: ByteArray, header: String): ByteArray {
        val h = header.encodeToByteArray()
        return ByteArray(mfk.size) { (mfk[it].toInt() xor h[it % 8].toInt()).toByte() }
    }

    // ------------------------------------------------------------------ SafeNet

    /**
     * SafeNet (ProtectServer / Luna EFT) key formats 10–14: the key is enciphered under the KM
     * key XORed with the variant byte, single DES (format 10) or TDES, in ECB (11, 12) or CBC
     * with a zero IV (13, 14).
     */
    fun safenetEncrypt(key: ByteArray, km: ByteArray, variant: Int, format: Int): ByteArray =
        Modes.encrypt(safenetCipher(km, variant, format), key, if (format == 13 || format == 14) CipherMode.CBC else CipherMode.ECB, ByteArray(8))

    fun safenetDecrypt(encrypted: ByteArray, km: ByteArray, variant: Int, format: Int): ByteArray =
        Modes.decrypt(safenetCipher(km, variant, format), encrypted, if (format == 13 || format == 14) CipherMode.CBC else CipherMode.ECB, ByteArray(8))

    private fun safenetCipher(km: ByteArray, variant: Int, format: Int): Tdes {
        require(format in 10..14) { "Format must be 10..14" }
        require(variant in 0..0xFF) { "Variant must be 00..FF" }
        val varied = ByteArray(km.size) { (km[it].toInt() xor variant).toByte() }
        return Tdes(if (format == 10) varied.copyOf(8) else varied)
    }

}
