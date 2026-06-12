package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core

/**
 * Minimal BER-TLV node. `tag` is the full identifier including class+constructed bits, encoded in
 * big-endian (e.g. tag 0x9F02 is stored as the int 0x9F02). `value` is the raw value bytes; for
 * constructed tags, callers may parse it again via [Tlv.parseAll].
 */
data class Tlv(val tag: Int, val value: ByteArray) {
    val isConstructed: Boolean get() = (tagFirstByte().toInt() and 0x20) != 0

    fun encode(): ByteArray {
        val tagBytes = encodeTag(tag)
        val lenBytes = encodeLength(value.size)
        return tagBytes + lenBytes + value
    }

    private fun tagFirstByte(): Byte {
        var t = tag
        while (t > 0xFF) t = t ushr 8
        return t.toByte()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Tlv) return false
        return tag == other.tag && value.contentEquals(other.value)
    }

    override fun hashCode(): Int = 31 * tag + value.contentHashCode()

    override fun toString(): String = "Tlv(%X, ${value.toHex()})".format(tag)

    companion object {
        fun primitive(tag: Int, value: ByteArray) = Tlv(tag, value)

        fun constructed(tag: Int, children: List<Tlv>): Tlv {
            require(((encodeTag(tag)[0].toInt() and 0x20) != 0)) {
                "Tag %X is not marked constructed".format(tag)
            }
            val body = ByteArray(children.sumOf { it.encode().size }).also { buf ->
                var off = 0
                for (c in children) {
                    val enc = c.encode()
                    enc.copyInto(buf, off); off += enc.size
                }
            }
            return Tlv(tag, body)
        }

        /** Parse exactly one TLV from [bytes] starting at [offset]. Returns Tlv + bytes consumed. */
        fun parseOne(bytes: ByteArray, offset: Int = 0): Pair<Tlv, Int> {
            var i = offset
            // skip leading 0x00 / 0xFF padding bytes per ISO 7816-4
            while (i < bytes.size && (bytes[i] == 0x00.toByte() || bytes[i] == 0xFF.toByte())) i++
            require(i < bytes.size) { "TLV: no tag at offset $offset" }

            val tagStart = i
            val first = bytes[i].toInt() and 0xFF
            i++
            if ((first and 0x1F) == 0x1F) {
                // multi-byte tag
                while (i < bytes.size && (bytes[i].toInt() and 0x80) != 0) i++
                require(i < bytes.size) { "TLV: truncated multi-byte tag" }
                i++
            }
            var tag = 0
            for (k in tagStart until i) tag = (tag shl 8) or (bytes[k].toInt() and 0xFF)

            require(i < bytes.size) { "TLV: missing length" }
            val lenFirst = bytes[i].toInt() and 0xFF
            i++
            val length: Int = if ((lenFirst and 0x80) == 0) {
                lenFirst
            } else {
                val n = lenFirst and 0x7F
                require(n in 1..3) { "TLV: unsupported length-of-length $n" }
                require(i + n <= bytes.size) { "TLV: truncated length" }
                var v = 0
                repeat(n) { v = (v shl 8) or (bytes[i++].toInt() and 0xFF) }
                v
            }
            require(i + length <= bytes.size) { "TLV: value truncated (need $length, have ${bytes.size - i})" }
            val value = bytes.copyOfRange(i, i + length)
            return Tlv(tag, value) to (i + length - offset)
        }

        fun parseAll(bytes: ByteArray): List<Tlv> {
            val out = mutableListOf<Tlv>()
            var i = 0
            while (i < bytes.size) {
                // skip padding
                if (bytes[i] == 0x00.toByte() || bytes[i] == 0xFF.toByte()) { i++; continue }
                val (tlv, used) = parseOne(bytes, i)
                out += tlv
                i += used
            }
            return out
        }

        private fun encodeTag(tag: Int): ByteArray {
            if (tag <= 0xFF) return byteArrayOf(tag.toByte())
            if (tag <= 0xFFFF) return byteArrayOf((tag ushr 8).toByte(), tag.toByte())
            if (tag <= 0xFFFFFF) return byteArrayOf((tag ushr 16).toByte(), (tag ushr 8).toByte(), tag.toByte())
            return byteArrayOf((tag ushr 24).toByte(), (tag ushr 16).toByte(), (tag ushr 8).toByte(), tag.toByte())
        }

        private fun encodeLength(len: Int): ByteArray = when {
            len < 0x80 -> byteArrayOf(len.toByte())
            len <= 0xFF -> byteArrayOf(0x81.toByte(), len.toByte())
            len <= 0xFFFF -> byteArrayOf(0x82.toByte(), (len ushr 8).toByte(), len.toByte())
            else -> byteArrayOf(0x83.toByte(), (len ushr 16).toByte(), (len ushr 8).toByte(), len.toByte())
        }
    }
}

/**
 * Common EMV tags. Not exhaustive — handlers add more as needed.
 */
object EmvTag {
    const val APPLICATION_TEMPLATE = 0x61
    const val FCI_TEMPLATE = 0x6F
    const val READ_RECORD_TEMPLATE = 0x70
    const val RESPONSE_TEMPLATE_1 = 0x80
    const val RESPONSE_TEMPLATE_2 = 0x77
    const val DEDICATED_FILE_NAME = 0x84
    const val FCI_PROPRIETARY = 0xA5
    const val APPLICATION_LABEL = 0x50
    const val TRACK_2_EQUIVALENT = 0x57
    const val APPLICATION_PAN = 0x5A
    const val CARDHOLDER_NAME = 0x5F20
    const val APPLICATION_EXPIRY = 0x5F24
    const val APPLICATION_PAN_SEQUENCE = 0x5F34
    const val ISSUER_COUNTRY_CODE = 0x5F28
    const val APPLICATION_PRIORITY = 0x87
    const val PDOL = 0x9F38
    const val CDOL1 = 0x8C
    const val CDOL2 = 0x8D
    const val AIP = 0x82
    const val AFL = 0x94
    const val CRYPTOGRAM_INFO_DATA = 0x9F27
    const val APPLICATION_CRYPTOGRAM = 0x9F26
    const val ISSUER_APPLICATION_DATA = 0x9F10
    const val ATC = 0x9F36
    const val UNPREDICTABLE_NUMBER = 0x9F37
    const val FCI_ISSUER_DISCRETIONARY = 0xBF0C
    const val DIRECTORY_ENTRY = 0x61
    const val DIRECTORY_DEFINITION_FILE = 0x9D
}
