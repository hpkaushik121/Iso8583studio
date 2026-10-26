package io.cryptocalc.emv

import ai.cortex.core.CryptoUtils
import ai.cortex.core.hexToBytes
import ai.cortex.core.toHexUpper
import io.cryptocalc.crypto.primitives.Digests
import io.cryptocalc.crypto.primitives.Tdes

/**
 * EMV key derivations that several tools share (EMV 4.x Book 2 Annex A1.4): the ICC master key
 * from an issuer master key and the PAN, and the common session key from a master key and ATC.
 * Pure functions over bytes; the calculators do the hex plumbing.
 */
object EmvKeys {

    /** Adjusts every byte of [key] to odd parity (DES convention). */
    fun oddParity(key: ByteArray): ByteArray = CryptoUtils.applyParity(key, isOdd = true)

    /**
     * Option A (A1.4.1): Y = rightmost 16 digits of PAN ‖ PSN; ZL = 3DES(MK)[Y], ZR = 3DES(MK)[Y XOR FF..FF].
     * [pan] and [psn] are decimal strings; a missing PSN is "00".
     */
    fun deriveUdkOptionA(masterKey: ByteArray, pan: String, psn: String = "00", parity: Boolean = true): ByteArray {
        require(masterKey.size == 16) { "Master key must be 16 bytes" }
        val digits = (pan.filter { it.isDigit() } + psn.filter { it.isDigit() }.padStart(2, '0'))
        val y = digits.takeLast(16).padStart(16, '0').hexToBytes()
        return derive(masterKey, y, parity)
    }

    /**
     * Option B (A1.4.2), for PANs longer than 16 digits: SHA-1 over PAN ‖ PSN, decimalise, take 16
     * digits, then continue as Option A.
     */
    fun deriveUdkOptionB(masterKey: ByteArray, pan: String, psn: String = "00", parity: Boolean = true): ByteArray {
        require(masterKey.size == 16) { "Master key must be 16 bytes" }
        val digits = pan.filter { it.isDigit() } + psn.filter { it.isDigit() }.padStart(2, '0')
        val input = (if (digits.length % 2 == 0) digits else "0$digits").hexToBytes()
        val sha = Digests.sha1(input).toHexUpper()
        val y16 = CryptoUtils.decimalize(sha).take(16)
        return derive(masterKey, y16.hexToBytes(), parity)
    }

    private fun derive(masterKey: ByteArray, y: ByteArray, parity: Boolean): ByteArray {
        val mk = Tdes(masterKey)
        val zl = mk.encryptBlock(y)
        val zr = mk.encryptBlock(ByteArray(8) { (y[it].toInt() xor 0xFF).toByte() })
        val key = zl + zr
        return if (parity) oddParity(key) else key
    }

    /**
     * EMV common session key (A1.3.1): SK = 3DES(MK)[ATC ‖ F0 ‖ 00×5] ‖ 3DES(MK)[ATC ‖ 0F ‖ 00×5].
     * [atc] is 2 bytes.
     */
    fun deriveCommonSessionKey(masterKey: ByteArray, atc: ByteArray, parity: Boolean = false): ByteArray {
        require(masterKey.size == 16) { "Master key must be 16 bytes" }
        require(atc.size == 2) { "ATC must be 2 bytes" }
        val mk = Tdes(masterKey)
        val left = mk.encryptBlock(atc + byteArrayOf(0xF0.toByte(), 0, 0, 0, 0, 0))
        val right = mk.encryptBlock(atc + byteArrayOf(0x0F, 0, 0, 0, 0, 0))
        val key = left + right
        return if (parity) oddParity(key) else key
    }

    /**
     * Mastercard proprietary session key derivation (M/Chip SKD):
     * SK = 3DES(MK)[ATC ‖ F0 ‖ 00 ‖ UN] ‖ 3DES(MK)[ATC ‖ 0F ‖ 00 ‖ UN]. [un] is 4 bytes.
     */
    fun deriveMastercardSessionKey(masterKey: ByteArray, atc: ByteArray, un: ByteArray, parity: Boolean = false): ByteArray {
        require(masterKey.size == 16) { "Master key must be 16 bytes" }
        require(atc.size == 2 && un.size == 4) { "ATC must be 2 bytes and UN 4 bytes" }
        val mk = Tdes(masterKey)
        val left = mk.encryptBlock(atc + byteArrayOf(0xF0.toByte(), 0) + un)
        val right = mk.encryptBlock(atc + byteArrayOf(0x0F, 0) + un)
        val key = left + right
        return if (parity) oddParity(key) else key
    }
}
