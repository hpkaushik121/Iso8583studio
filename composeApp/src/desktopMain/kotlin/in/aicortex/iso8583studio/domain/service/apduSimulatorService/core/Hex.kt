package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core

internal fun String.hexToBytes(): ByteArray {
    val clean = filterNot { it.isWhitespace() }
    require(clean.length % 2 == 0) { "Hex string must have even length: '$this'" }
    return ByteArray(clean.length / 2) { i ->
        ((clean[i * 2].digitToInt(16) shl 4) or clean[i * 2 + 1].digitToInt(16)).toByte()
    }
}

internal fun ByteArray.toHex(): String =
    joinToString("") { "%02X".format(it) }

internal fun Byte.hex(): String = "%02X".format(this)
