package io.cryptocalc.crypto.primitives

/** MD4 (RFC 1320), CRC-32 (IEEE) and the Kerberos CRC-32 of RFC 1510 §6.4.2, for the hash tool. */
object ExtraDigests {

    fun md4(input: ByteArray): ByteArray {
        val msgLen = input.size
        val padLen = ((56 - (msgLen + 1) % 64) + 64) % 64
        val msg = ByteArray(msgLen + 1 + padLen + 8)
        input.copyInto(msg)
        msg[msgLen] = 0x80.toByte()
        val bits = msgLen.toLong() * 8
        for (i in 0 until 8) msg[msg.size - 8 + i] = (bits ushr (8 * i)).toByte()
        var a = 0x67452301
        var b = 0xEFCDAB89.toInt()
        var c = 0x98BADCFE.toInt()
        var d = 0x10325476
        val x = IntArray(16)
        for (off in msg.indices step 64) {
            for (i in 0 until 16) {
                x[i] = (msg[off + 4 * i].toInt() and 0xFF) or ((msg[off + 4 * i + 1].toInt() and 0xFF) shl 8) or
                        ((msg[off + 4 * i + 2].toInt() and 0xFF) shl 16) or ((msg[off + 4 * i + 3].toInt() and 0xFF) shl 24)
            }
            val aa = a; val bb = b; val cc = c; val dd = d
            fun f(x: Int, y: Int, z: Int) = (x and y) or (x.inv() and z)
            fun g(x: Int, y: Int, z: Int) = (x and y) or (x and z) or (y and z)
            fun h(x: Int, y: Int, z: Int) = x xor y xor z
            fun rot(v: Int, s: Int) = (v shl s) or (v ushr (32 - s))
            val r1 = intArrayOf(3, 7, 11, 19)
            for (i in 0 until 16) {
                val t = rot(a + f(b, c, d) + x[i], r1[i % 4]); a = d; d = c; c = b; b = t
            }
            val r2 = intArrayOf(3, 5, 9, 13)
            val o2 = intArrayOf(0, 4, 8, 12, 1, 5, 9, 13, 2, 6, 10, 14, 3, 7, 11, 15)
            for (i in 0 until 16) {
                val t = rot(a + g(b, c, d) + x[o2[i]] + 0x5A827999, r2[i % 4]); a = d; d = c; c = b; b = t
            }
            val r3 = intArrayOf(3, 9, 11, 15)
            val o3 = intArrayOf(0, 8, 4, 12, 2, 10, 6, 14, 1, 9, 5, 13, 3, 11, 7, 15)
            for (i in 0 until 16) {
                val t = rot(a + h(b, c, d) + x[o3[i]] + 0x6ED9EBA1, r3[i % 4]); a = d; d = c; c = b; b = t
            }
            a += aa; b += bb; c += cc; d += dd
        }
        val out = ByteArray(16)
        for ((i, v) in listOf(a, b, c, d).withIndex()) for (j in 0 until 4) out[4 * i + j] = (v ushr (8 * j)).toByte()
        return out
    }

    private val CRC_TABLE: IntArray = IntArray(256) { n ->
        var c = n
        repeat(8) { c = if (c and 1 != 0) 0xEDB88320.toInt() xor (c ushr 1) else c ushr 1 }
        c
    }

    /** Standard CRC-32 (IEEE 802.3), big-endian 4 bytes. */
    fun crc32(data: ByteArray): ByteArray {
        var crc = 0xFFFFFFFF.toInt()
        for (b in data) crc = CRC_TABLE[(crc xor b.toInt()) and 0xFF] xor (crc ushr 8)
        crc = crc.inv()
        return byteArrayOf((crc ushr 24).toByte(), (crc ushr 16).toByte(), (crc ushr 8).toByte(), crc.toByte())
    }

    /** Kerberos "crc32" checksum (RFC 1510 §6.4.2): zero initial value, no final complement, little-endian output. */
    fun crc32Rfc1510(data: ByteArray): ByteArray {
        var crc = 0
        for (b in data) crc = CRC_TABLE[(crc xor b.toInt()) and 0xFF] xor (crc ushr 8)
        return byteArrayOf(crc.toByte(), (crc ushr 8).toByte(), (crc ushr 16).toByte(), (crc ushr 24).toByte())
    }
}
