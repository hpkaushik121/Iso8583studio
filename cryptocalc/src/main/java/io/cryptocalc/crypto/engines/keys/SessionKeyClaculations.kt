package io.cryptocalc.crypto.engines.keys

import ai.cortex.core.IsoUtil
import ai.cortex.core.types.AlgorithmType
import ai.cortex.core.types.CipherMode
import ai.cortex.core.types.CryptoAlgorithm
import io.cryptocalc.crypto.engines.encryption.models.SymmetricEncryptionEngineParameters
import io.cryptocalc.emv.calculators.emv41.SessionKeyType
import java.nio.ByteBuffer


/**
 * Convert integer to bytes in BIG ENDIAN format (critical for matching Python)
 */
private fun intToByteBigEndian(value: Int, size: Int): ByteArray {
    // Add a safety check. An Int cannot meaningfully fill more than 4 bytes.

    val result = ByteArray(size)
    // Iterate from left to right in the destination array
    for (i in 0 until size) {
        // Calculate how many bits to shift to get the correct byte
        val shift = 8 * (size - 1 - i)
        result[i] = (value shr shift).toByte()
    }
    return result
}

private fun longToBigEndianBytes(value: Long, size: Int): ByteArray {
    // The buffer for the full 8-byte long. ByteBuffer is big-endian by default.
    val longBuffer = ByteBuffer.allocate(Long.SIZE_BYTES).putLong(value)

    // Handle different requested sizes
    return when {
        // If the same size, just return the array
        size == Long.SIZE_BYTES -> longBuffer.array()

        // If a larger size is requested, pad with leading zeros
        size > Long.SIZE_BYTES -> {
            val dest = ByteArray(size)
            // Copy the 8 bytes of the long to the end of the destination array
            System.arraycopy(longBuffer.array(), 0, dest, size - Long.SIZE_BYTES, Long.SIZE_BYTES)
            dest
        }

        // If a smaller size is requested, truncate to the most significant bytes
        else -> {
            // copyOfRange is simpler here for truncation
            val start = Long.SIZE_BYTES - size
            longBuffer.array().copyOfRange(start, Long.SIZE_BYTES)
        }
    }
}

/**
 * CORRECTED: F(X,Y,j) function that matches Python exactly
 */
internal suspend fun KeysEngineImpl.derive(
    algorithm: CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>,
    x: ByteArray,
    y: ByteArray,
    j: Int,
    branchFactor: Int
): ByteArray {

    // CRITICAL: Use BIG ENDIAN byte ordering like Python

    val jModB = longToBigEndianBytes((j % branchFactor).toLong(), 8)
    print(IsoUtil.bytesToHex(jModB))

    // Left part: DES3(X)[YL XOR (j mod b)]
    val yL = y.sliceArray(0..7)  // First 8 bytes
    val lData = IsoUtil.xorByteArray(yL, jModB)

    // CRITICAL: Pass key X directly without modification
    val lResult = emvEngines.encryptionEngine.encrypt(
        algorithm = algorithm,
        encryptionEngineParameters = SymmetricEncryptionEngineParameters(
            data = lData,
            key = x,  // Use X as-is, no padding/modification
            mode = CipherMode.ECB
        )
    )
    print(IsoUtil.bytesToHex(lResult))

    // Right part: DES3(X)[YR XOR (j mod b) XOR 'F0']
    val yR = y.sliceArray(8..15) // Last 8 bytes
    val rData1 = IsoUtil.xorByteArray(yR, jModB)

    // CRITICAL: XOR with exactly 7 zeros + F0 (not 00,00,00,00,00,00,00,F0)
    val f0Mask = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xF0.toByte())
    val rData2 = IsoUtil.xorByteArray(rData1, f0Mask)

    val rResult = emvEngines.encryptionEngine.encrypt(
        algorithm = algorithm,
        encryptionEngineParameters = SymmetricEncryptionEngineParameters(
            data = rData2,
            key = x,  // Use X as-is, no padding/modification
            mode = CipherMode.ECB
        )
    )
    print(IsoUtil.bytesToHex(rResult))


    return lResult + rResult
}

/**
 * CORRECTED: Walk function
 */
internal suspend fun KeysEngineImpl.walk(
    algorithm: CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>,
    j: Int,
    h: Int,
    iccMK: ByteArray,
    iv: ByteArray,
    branchFactor: Int
): Pair<ByteArray, ByteArray> {

    // Base case: P = ICC MK, GP = IV
    if (h == 0) {
        return Pair(iccMK, iv)
    }

    val (p, gp) = walk(algorithm, j / branchFactor, h - 1, iccMK, iv, branchFactor)
    print(IsoUtil.bytesToHex(p))
    print(IsoUtil.bytesToHex(gp))
    // Derives an IK from P and GP
    val newParent = derive(algorithm, p, gp, j, branchFactor)
    return Pair(newParent, p)
}