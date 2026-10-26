package ai.cortex.core

private const val HEX_UPPER = "0123456789ABCDEF"
private const val HEX_LOWER = "0123456789abcdef"

/** Two upper-case hex digits for one byte, e.g. 0x0A -> "0A". */
fun Byte.toHex2(): String {
    val v = toInt() and 0xFF
    return buildString(2) { append(HEX_UPPER[v ushr 4]); append(HEX_UPPER[v and 0x0F]) }
}

/** Upper-case hex string with no separators. Replaces the JVM-only `"%02X".format`. */
fun ByteArray.toHexUpper(): String {
    val sb = StringBuilder(size * 2)
    for (b in this) {
        val v = b.toInt() and 0xFF
        sb.append(HEX_UPPER[v ushr 4]).append(HEX_UPPER[v and 0x0F])
    }
    return sb.toString()
}

/** Lower-case hex string with no separators. */
fun ByteArray.toHexLower(): String {
    val sb = StringBuilder(size * 2)
    for (b in this) {
        val v = b.toInt() and 0xFF
        sb.append(HEX_LOWER[v ushr 4]).append(HEX_LOWER[v and 0x0F])
    }
    return sb.toString()
}

/** Upper-case hex of the low [width] nibbles of this value, zero padded (e.g. 0x1A, 4 -> "001A"). */
fun Int.toHex(width: Int): String = toString(16).uppercase().padStart(width, '0')

/** Parses a hex string (case-insensitive, optional spaces) into bytes. Throws on odd length or bad digits. */
fun String.hexToBytes(): ByteArray {
    val clean = filterNot { it.isWhitespace() }
    require(clean.length % 2 == 0) { "Hex string must have an even number of digits" }
    return ByteArray(clean.length / 2) { i ->
        val hi = clean[2 * i].digitToIntOrNull(16) ?: throw IllegalArgumentException("Invalid hex digit '${clean[2 * i]}'")
        val lo = clean[2 * i + 1].digitToIntOrNull(16) ?: throw IllegalArgumentException("Invalid hex digit '${clean[2 * i + 1]}'")
        ((hi shl 4) or lo).toByte()
    }
}

/** ISO-8859-1: every char is exactly one byte. Chars above U+00FF are truncated like the JVM does. */
fun String.toLatin1Bytes(): ByteArray = ByteArray(length) { this[it].code.toByte() }

/** ISO-8859-1 decode: every byte becomes one char. */
fun ByteArray.latin1ToString(): String {
    val sb = StringBuilder(size)
    for (b in this) sb.append((b.toInt() and 0xFF).toChar())
    return sb.toString()
}

/** Byte-wise XOR of equal-length arrays (shorter length wins if they differ). */
fun ByteArray.xorWith(other: ByteArray): ByteArray =
    ByteArray(minOf(size, other.size)) { (this[it].toInt() xor other[it].toInt()).toByte() }

/** US-ASCII decode as the JVM does it: bytes above 0x7F become U+FFFD. */
fun ByteArray.asciiToString(offset: Int = 0, length: Int = size - offset): String {
    val sb = StringBuilder(length)
    for (i in offset until offset + length) {
        val v = this[i].toInt() and 0xFF
        sb.append(if (v < 0x80) v.toChar() else '�')
    }
    return sb.toString()
}

/** US-ASCII encode as the JVM does it: chars above 0x7F become '?'. */
fun String.toAsciiBytes(): ByteArray = ByteArray(length) { i ->
    val c = this[i].code
    (if (c < 0x80) c else '?'.code).toByte()
}
