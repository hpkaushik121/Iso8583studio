package io.cryptocalc.emv

import ai.cortex.core.CryptoUtils
import ai.cortex.core.xorWith
import io.cryptocalc.crypto.primitives.Tdes

/** Session key derivations beyond the common and Mastercard SKD methods in [EmvKeys]. */
object EmvSessionKeys {

    /**
     * EMV 2000 tree session key derivation (EMV 4.1 Book 2 Annex A1.3), superseded by the common
     * session key method in SU-46. [branchFactor]^[height] must cover every ATC value.
     */
    fun deriveEmv2000TreeSessionKey(
        masterKey: ByteArray, atc: ByteArray, height: Int = 8, branchFactor: Int = 4,
        iv: ByteArray = ByteArray(16), parity: Boolean = true,
    ): ByteArray {
        require(masterKey.size == 16) { "Master key must be 16 bytes" }
        require(atc.size == 2) { "ATC must be 2 bytes" }
        require(iv.size == 16) { "IV must be 16 bytes" }
        require(branchFactor >= 2 && height >= 1) { "Branch factor must be >= 2 and height >= 1" }
        var combos = 1.0
        repeat(height) { combos *= branchFactor }
        require(combos >= 65535) { "branchFactor^height must exceed the maximum ATC (65535)" }

        // F(X, Y, j) = 3DES(X)[YL XOR (j mod b)] ‖ 3DES(X)[YR XOR (j mod b) XOR F0]
        fun derive(x: ByteArray, y: ByteArray, j: Int): ByteArray {
            val jModB = ByteArray(8).also { it[7] = (j % branchFactor).toByte() }
            val cipher = Tdes(x)
            val left = cipher.encryptBlock(y.copyOfRange(0, 8).xorWith(jModB))
            val rMask = jModB.copyOf().also { it[7] = (it[7].toInt() xor 0xF0).toByte() }
            val right = cipher.encryptBlock(y.copyOfRange(8, 16).xorWith(rMask))
            return left + right
        }

        // walk(j, h) returns (parent, grandparent) at level h for index j.
        fun walk(j: Int, h: Int): Pair<ByteArray, ByteArray> {
            if (h == 0) return masterKey to iv
            val (p, gp) = walk(j / branchFactor, h - 1)
            return derive(p, gp, j) to p
        }

        val atcNum = ((atc[0].toInt() and 0xFF) shl 8) or (atc[1].toInt() and 0xFF)
        val (p, gp) = walk(atcNum / branchFactor, height - 1)
        val sk = derive(p, gp, atcNum).xorWith(gp)
        return if (parity) CryptoUtils.applyParity(sk, isOdd = true) else sk
    }

    /**
     * Visa secure-messaging session key: each 8-byte half of the ICC master key gets the ATC
     * (left) or its complement (right) XORed into its last two bytes, then odd parity.
     */
    fun deriveVisaSmSessionKey(masterKey: ByteArray, atc: ByteArray): ByteArray {
        require(masterKey.size == 16) { "Master key must be 16 bytes" }
        require(atc.size == 2) { "ATC must be 2 bytes" }
        val rA = ByteArray(6) + atc
        val rB = ByteArray(6) + byteArrayOf((atc[0].toInt() xor 0xFF).toByte(), (atc[1].toInt() xor 0xFF).toByte())
        val sk = masterKey.copyOfRange(0, 8).xorWith(rA) + masterKey.copyOfRange(8, 16).xorWith(rB)
        return CryptoUtils.applyParity(sk, isOdd = true)
    }

    /** Common session key with an arbitrary 8-byte diversifier R (byte 2 is replaced by F0 / 0F). */
    fun deriveCommonSessionKeyR(masterKey: ByteArray, r: ByteArray, parity: Boolean = true): ByteArray {
        require(masterKey.size == 16) { "Master key must be 16 bytes" }
        require(r.size == 8) { "Diversifier must be 8 bytes" }
        val mk = Tdes(masterKey)
        val rA = r.copyOf().also { it[2] = 0xF0.toByte() }
        val rB = r.copyOf().also { it[2] = 0x0F }
        val sk = mk.encryptBlock(rA) + mk.encryptBlock(rB)
        return if (parity) CryptoUtils.applyParity(sk, isOdd = true) else sk
    }
}
