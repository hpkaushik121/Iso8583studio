package io.cryptocalc.asn1

import com.ionspin.kotlin.bignum.integer.BigInteger
import io.cryptocalc.math.BigInt
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** A decoded DER element: tag, whether it is constructed, its raw content and parsed children. */
class DerNode(val tag: Int, val constructed: Boolean, val content: ByteArray, val children: List<DerNode>, val encoded: ByteArray) {
    val isSequence get() = tag == Der.SEQUENCE
    val isSet get() = tag == Der.SET
    fun child(i: Int): DerNode = children.getOrNull(i) ?: throw IllegalArgumentException("Missing DER element $i")
    fun integer(): BigInteger {
        require(tag == Der.INTEGER) { "Expected INTEGER, found tag 0x${tag.toString(16)}" }
        return BigInt.fromUnsignedBytes(if (content.size > 1 && content[0] == 0.toByte()) content.copyOfRange(1, content.size) else content)
    }
    fun oid(): String {
        require(tag == Der.OID) { "Expected OBJECT IDENTIFIER" }
        return Der.decodeOid(content)
    }
    fun bitStringBytes(): ByteArray {
        require(tag == Der.BIT_STRING) { "Expected BIT STRING" }
        return content.copyOfRange(1, content.size)
    }
    fun string(): String = content.decodeToString()
}

/** Minimal DER encoder/decoder and PEM helpers. Enough for keys, certificates and CSRs. */
@OptIn(ExperimentalEncodingApi::class)
object Der {
    const val BOOLEAN = 0x01
    const val INTEGER = 0x02
    const val BIT_STRING = 0x03
    const val OCTET_STRING = 0x04
    const val NULL = 0x05
    const val OID = 0x06
    const val UTF8_STRING = 0x0C
    const val PRINTABLE_STRING = 0x13
    const val IA5_STRING = 0x16
    const val UTC_TIME = 0x17
    const val GENERALIZED_TIME = 0x18
    const val SEQUENCE = 0x30
    const val SET = 0x31

    fun length(n: Int): ByteArray = when {
        n < 0x80 -> byteArrayOf(n.toByte())
        n < 0x100 -> byteArrayOf(0x81.toByte(), n.toByte())
        n < 0x10000 -> byteArrayOf(0x82.toByte(), (n ushr 8).toByte(), n.toByte())
        else -> byteArrayOf(0x83.toByte(), (n ushr 16).toByte(), (n ushr 8).toByte(), n.toByte())
    }

    fun tlv(tag: Int, content: ByteArray): ByteArray = byteArrayOf(tag.toByte()) + length(content.size) + content

    fun sequence(vararg items: ByteArray): ByteArray = tlv(SEQUENCE, items.fold(ByteArray(0)) { a, b -> a + b })
    fun set(vararg items: ByteArray): ByteArray = tlv(SET, items.fold(ByteArray(0)) { a, b -> a + b })

    /** INTEGER with two's-complement sign handling for non-negative values. */
    fun integer(value: BigInteger): ByteArray {
        var b = BigInt.toUnsignedBytes(value)
        if (b[0].toInt() and 0x80 != 0) b = byteArrayOf(0) + b
        return tlv(INTEGER, b)
    }

    fun integer(value: Int): ByteArray = integer(BigInteger.fromInt(value))
    fun bitString(data: ByteArray, unusedBits: Int = 0): ByteArray = tlv(BIT_STRING, byteArrayOf(unusedBits.toByte()) + data)
    fun octetString(data: ByteArray): ByteArray = tlv(OCTET_STRING, data)
    fun nullValue(): ByteArray = byteArrayOf(NULL.toByte(), 0)
    fun boolean(v: Boolean): ByteArray = tlv(BOOLEAN, byteArrayOf(if (v) 0xFF.toByte() else 0))
    fun utf8(s: String): ByteArray = tlv(UTF8_STRING, s.encodeToByteArray())
    fun printable(s: String): ByteArray = tlv(PRINTABLE_STRING, s.encodeToByteArray())
    fun ia5(s: String): ByteArray = tlv(IA5_STRING, s.encodeToByteArray())
    fun utcTime(yyMMddHHmmssZ: String): ByteArray = tlv(UTC_TIME, yyMMddHHmmssZ.encodeToByteArray())
    fun generalizedTime(yyyyMMddHHmmssZ: String): ByteArray = tlv(GENERALIZED_TIME, yyyyMMddHHmmssZ.encodeToByteArray())
    /** Context-specific explicit tag [n] wrapping [content]. */
    fun explicit(n: Int, content: ByteArray): ByteArray = tlv(0xA0 or n, content)

    fun oid(dotted: String): ByteArray {
        val parts = dotted.split('.').map { it.toLong() }
        require(parts.size >= 2) { "Invalid OID $dotted" }
        val out = ArrayList<Byte>()
        fun emit(v: Long) {
            var value = v
            val stack = ArrayList<Int>()
            do { stack.add((value and 0x7F).toInt()); value = value ushr 7 } while (value > 0)
            for (i in stack.indices.reversed()) out.add((stack[i] or if (i == 0) 0 else 0x80).toByte())
        }
        emit(parts[0] * 40 + parts[1])
        for (i in 2 until parts.size) emit(parts[i])
        return tlv(OID, out.toByteArray())
    }

    fun decodeOid(content: ByteArray): String {
        val parts = ArrayList<Long>()
        var value = 0L
        for (b in content) {
            value = (value shl 7) or (b.toLong() and 0x7F)
            if (b.toInt() and 0x80 == 0) { parts.add(value); value = 0 }
        }
        require(parts.isNotEmpty()) { "Empty OID" }
        val first = parts[0]
        val sb = StringBuilder()
        when {
            first < 40 -> sb.append("0.").append(first)
            first < 80 -> sb.append("1.").append(first - 40)
            else -> sb.append("2.").append(first - 80)
        }
        for (i in 1 until parts.size) sb.append('.').append(parts[i])
        return sb.toString()
    }

    fun decode(data: ByteArray): DerNode {
        val (node, used) = decodeAt(data, 0)
        require(used == data.size) { "Trailing bytes after DER element" }
        return node
    }

    /** Decodes one element at [pos], returning it and the number of bytes consumed. */
    fun decodeAt(data: ByteArray, pos: Int): Pair<DerNode, Int> {
        require(pos < data.size) { "Unexpected end of DER data" }
        val tag = data[pos].toInt() and 0xFF
        require(tag and 0x1F != 0x1F) { "Multi-byte DER tags are not supported" }
        var p = pos + 1
        val first = data[p].toInt() and 0xFF
        var len: Int
        if (first < 0x80) { len = first; p++ } else {
            val count = first and 0x7F
            require(count in 1..4) { "Unsupported DER length" }
            len = 0
            for (i in 1..count) len = (len shl 8) or (data[p + i].toInt() and 0xFF)
            p += 1 + count
        }
        require(p + len <= data.size) { "DER element exceeds the data" }
        val content = data.copyOfRange(p, p + len)
        val constructed = tag and 0x20 != 0
        val children = if (constructed) decodeAll(content) else emptyList()
        val encoded = data.copyOfRange(pos, p + len)
        return DerNode(tag, constructed, content, children, encoded) to (p + len - pos)
    }

    fun decodeAll(data: ByteArray): List<DerNode> {
        val out = ArrayList<DerNode>()
        var pos = 0
        while (pos < data.size) {
            val (node, used) = decodeAt(data, pos)
            out.add(node)
            pos += used
        }
        return out
    }

    // ------------------------------------------------------------------ PEM

    fun toPem(label: String, der: ByteArray): String {
        val b64 = Base64.encode(der).chunked(64).joinToString("\n")
        return "-----BEGIN $label-----\n$b64\n-----END $label-----"
    }

    /** Accepts PEM (any label) or bare hex / base64 DER. Returns the DER bytes and the PEM label if present. */
    fun fromPemOrDer(text: String): Pair<ByteArray, String?> {
        val t = text.trim()
        val m = Regex("-----BEGIN ([A-Z0-9 ]+)-----([\\s\\S]*?)-----END \\1-----").find(t)
        if (m != null) {
            val body = m.groupValues[2].filterNot { it.isWhitespace() }
            return Base64.decode(body) to m.groupValues[1]
        }
        val compact = t.filterNot { it.isWhitespace() }
        if (compact.isNotEmpty() && compact.all { it in "0123456789abcdefABCDEF" } && compact.length % 2 == 0) {
            return ByteArray(compact.length / 2) { compact.substring(2 * it, 2 * it + 2).toInt(16).toByte() } to null
        }
        return Base64.decode(compact) to null
    }
}
