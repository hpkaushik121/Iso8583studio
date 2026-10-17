package io.cryptocalc.crypto.primitives

/**
 * A raw block cipher: one block in, one block out, no mode, no padding.
 * Modes of operation and padding live in [Modes]; MACs in [Mac] and [Cmac].
 */
interface BlockCipher {
    /** Block size in bytes: 8 for DES/TDES, 16 for AES. */
    val blockSize: Int

    fun encryptBlock(src: ByteArray, srcOff: Int, dst: ByteArray, dstOff: Int)
    fun decryptBlock(src: ByteArray, srcOff: Int, dst: ByteArray, dstOff: Int)

    /** Convenience: encrypt exactly one block given as a standalone array. */
    fun encryptBlock(block: ByteArray): ByteArray {
        require(block.size == blockSize) { "Block must be $blockSize bytes, got ${block.size}" }
        val out = ByteArray(blockSize)
        encryptBlock(block, 0, out, 0)
        return out
    }

    /** Convenience: decrypt exactly one block given as a standalone array. */
    fun decryptBlock(block: ByteArray): ByteArray {
        require(block.size == blockSize) { "Block must be $blockSize bytes, got ${block.size}" }
        val out = ByteArray(blockSize)
        decryptBlock(block, 0, out, 0)
        return out
    }
}
