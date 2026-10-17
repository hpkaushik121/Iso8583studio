package io.cryptocalc.hsm

import ai.cortex.core.hexToBytes
import ai.cortex.core.toHexUpper
import ai.cortex.core.types.CipherMode
import io.cryptocalc.crypto.primitives.Aes
import io.cryptocalc.crypto.primitives.BlockCipher
import io.cryptocalc.crypto.primitives.Cmac
import io.cryptocalc.crypto.primitives.Modes
import io.cryptocalc.crypto.primitives.Tdes
import io.cryptocalc.crypto.primitives.secureRandomBytes

/**
 * ANSI X9.143 / TR-31 key blocks with key-derivation binding: version B (TDES KBPK) and
 * version D (AES KBPK). Variant-binding versions A and C are rejected.
 *
 * The confidential payload is `length(16 bits) ‖ key ‖ random pad`; KBEK/KBAK are derived
 * from the KBPK with CMAC counter-mode derivation, the MAC is CMAC(KBAK, header ‖ payload)
 * and the payload is encrypted in CBC with the MAC as IV.
 */
object Tr31KeyBlock {

    class Wrapped(val keyBlock: String, val header: String, val kbek: ByteArray, val kbak: ByteArray, val mac: ByteArray)
    class Unwrapped(val key: ByteArray, val header: String, val kbek: ByteArray, val kbak: ByteArray, val mac: ByteArray, val macValid: Boolean) {
        val keyUsage: String get() = header.substring(5, 7)
        val algorithm: String get() = header.substring(7, 8)
        val modeOfUse: String get() = header.substring(8, 9)
    }

    private fun normalizeTdesKey(key: ByteArray): ByteArray = when (key.size) {
        24 -> key
        16 -> key + key.copyOfRange(0, 8)
        8 -> key + key + key
        else -> throw IllegalArgumentException("TDES key must be 8, 16 or 24 bytes (got ${key.size}).")
    }

    private fun cipher(key: ByteArray, aes: Boolean): BlockCipher = if (aes) Aes(key) else Tdes(normalizeTdesKey(key))

    private fun cmac(key: ByteArray, msg: ByteArray, aes: Boolean): ByteArray = Cmac(cipher(key, aes)).compute(msg)

    /** usage: 0x0000 = encryption key (KBEK), 0x0001 = MAC key (KBAK). */
    fun deriveKey(kbpk: ByteArray, usage: Int, aes: Boolean): ByteArray {
        val keyBits = kbpk.size * 8
        val algo = if (aes) when (kbpk.size) {
            16 -> 0x0002; 24 -> 0x0003; 32 -> 0x0004
            else -> throw IllegalArgumentException("AES KBPK must be 16, 24 or 32 bytes.")
        } else when (kbpk.size) {
            16 -> 0x0000; 24 -> 0x0001
            else -> throw IllegalArgumentException("TDES KBPK must be 16 or 24 bytes.")
        }
        val bs = if (aes) 16 else 8
        val out = ByteArray(kbpk.size)
        var produced = 0
        var counter = 1
        while (produced < out.size) {
            val data = byteArrayOf(
                counter.toByte(), ((usage ushr 8) and 0xFF).toByte(), (usage and 0xFF).toByte(), 0x00,
                ((algo ushr 8) and 0xFF).toByte(), (algo and 0xFF).toByte(),
                ((keyBits ushr 8) and 0xFF).toByte(), (keyBits and 0xFF).toByte()
            )
            val block = cmac(kbpk, data, aes)
            val take = minOf(bs, out.size - produced)
            block.copyInto(out, produced, 0, take)
            produced += take
            counter++
        }
        return out
    }

    private fun isAes(version: Char): Boolean = when (version) {
        'B' -> false
        'D' -> true
        'A', 'C' -> throw IllegalArgumentException("Version '$version' (variant binding) is not supported. Use B (TDES) or D (AES) key derivation binding.")
        else -> throw IllegalArgumentException("Unsupported key block version '$version'. Use B or D.")
    }

    /**
     * Wrap [key] under [kbpk]. [header] is the ASCII header (16 fixed chars plus optional blocks);
     * the length field is recomputed. The version letter must agree with [aes] when given.
     */
    fun wrap(kbpk: ByteArray, header: String, key: ByteArray, aes: Boolean? = null): Wrapped {
        require(header.length >= 16) { "Header must be at least 16 characters." }
        val useAes = isAes(header[0])
        if (aes != null) require(aes == useAes) { "Header version '${header[0]}' does not match the selected KBPK type" }
        val bs = if (useAes) 16 else 8
        val kbek = deriveKey(kbpk, 0x0000, useAes)
        val kbak = deriveKey(kbpk, 0x0001, useAes)
        val bitLen = key.size * 8
        val core = byteArrayOf(((bitLen ushr 8) and 0xFF).toByte(), (bitLen and 0xFF).toByte()) + key
        val padLen = (bs - (core.size % bs)) % bs
        val pt = core + secureRandomBytes(padLen)
        val totalLen = header.length + pt.size * 2 + bs * 2
        val fixedHeader = header[0] + totalLen.toString().padStart(4, '0') + header.substring(5)
        val mac = cmac(kbak, fixedHeader.encodeToByteArray() + pt, useAes)
        val enc = Modes.encrypt(cipher(kbek, useAes), pt, CipherMode.CBC, mac)
        return Wrapped(fixedHeader + enc.toHexUpper() + mac.toHexUpper(), fixedHeader, kbek, kbak, mac)
    }

    /** Unwrap [keyBlock] under [kbpk], verifying the MAC. */
    fun unwrap(kbpk: ByteArray, keyBlock: String, aes: Boolean? = null): Unwrapped {
        require(keyBlock.length >= 16) { "Key block is too short." }
        val useAes = isAes(keyBlock[0])
        if (aes != null) require(aes == useAes) { "Key block version '${keyBlock[0]}' does not match the selected KBPK type" }
        val bs = if (useAes) 16 else 8
        val numOpt = keyBlock.substring(12, 14).toIntOrNull(16) ?: 0
        var headerLen = 16
        repeat(numOpt) {
            require(keyBlock.length >= headerLen + 4) { "Malformed optional block in header." }
            headerLen += keyBlock.substring(headerLen + 2, headerLen + 4).toInt(16)
        }
        require(keyBlock.length > headerLen + bs * 2) { "Key block payload is too short." }
        val header = keyBlock.substring(0, headerLen)
        val payload = keyBlock.substring(headerLen)
        val mac = payload.takeLast(bs * 2).hexToBytes()
        val enc = payload.dropLast(bs * 2).hexToBytes()
        val kbek = deriveKey(kbpk, 0x0000, useAes)
        val kbak = deriveKey(kbpk, 0x0001, useAes)
        val pt = Modes.decrypt(cipher(kbek, useAes), enc, CipherMode.CBC, mac)
        val macValid = cmac(kbak, header.encodeToByteArray() + pt, useAes).contentEquals(mac)
        val bitLen = ((pt[0].toInt() and 0xFF) shl 8) or (pt[1].toInt() and 0xFF)
        val keyLen = bitLen / 8
        if (keyLen !in 1..(pt.size - 2)) {
            // Garbage after decryption: report the MAC failure instead of a misleading length error.
            if (!macValid) return Unwrapped(ByteArray(0), header, kbek, kbak, mac, false)
            throw IllegalArgumentException("Decoded key length ($bitLen bits) is invalid - wrong KBPK?")
        }
        return Unwrapped(pt.copyOfRange(2, 2 + keyLen), header, kbek, kbak, mac, macValid)
    }
}
