package io.cryptocalc.emv

import ai.cortex.core.toHexUpper

/** ISO/IEC 7816-3 Answer To Reset parser. */
object Atr {

    class InterfaceByte(val name: String, val value: Int, val meaning: String)

    class Parsed(
        val convention: String, val interfaceBytes: List<InterfaceByte>, val protocols: List<Int>,
        val historicalBytes: ByteArray, val historicalNotes: List<String>, val tck: Int?, val tckValid: Boolean?, val warnings: List<String>,
    ) {
        fun describe(): String = buildString {
            appendLine("Convention: $convention")
            appendLine("Protocols offered: ${if (protocols.isEmpty()) "T=0 (default)" else protocols.joinToString(", ") { "T=$it" }}")
            interfaceBytes.forEach { appendLine("${it.name} = ${it.value.toString(16).padStart(2, '0').uppercase()}: ${it.meaning}") }
            appendLine("Historical bytes (${historicalBytes.size}): ${historicalBytes.toHexUpper()}")
            historicalNotes.forEach { appendLine("  $it") }
            if (tck != null) appendLine("TCK = ${tck.toString(16).padStart(2, '0').uppercase()} (${if (tckValid == true) "valid" else "INVALID"})")
            else appendLine("TCK: absent (only T=0 offered)")
            warnings.forEach { appendLine("Warning: $it") }
        }.trimEnd()
    }

    private val FI = intArrayOf(372, 372, 558, 744, 1116, 1488, 1860, -1, -1, 512, 768, 1024, 1536, 2048, -1, -1)
    private val FMAX = doubleArrayOf(4.0, 5.0, 6.0, 8.0, 12.0, 16.0, 20.0, 0.0, 0.0, 5.0, 7.5, 10.0, 15.0, 20.0, 0.0, 0.0)
    private val DI = intArrayOf(-1, 1, 2, 4, 8, 16, 32, 64, 12, 20, -1, -1, -1, -1, -1, -1)

    fun parse(atr: ByteArray): Parsed {
        require(atr.size >= 2) { "ATR must have at least TS and T0" }
        val warnings = mutableListOf<String>()
        val ifaces = mutableListOf<InterfaceByte>()
        val protocols = mutableListOf<Int>()
        val convention = when (atr[0].toInt() and 0xFF) { 0x3B -> "Direct (3B)"; 0x3F -> "Inverse (3F)"; else -> "Unknown TS ${atr[0].toInt() and 0xFF}" }
        var pos = 1
        var y = (atr[pos].toInt() and 0xF0) shr 4
        val k = atr[pos].toInt() and 0x0F
        pos++
        var i = 1
        var td: Int? = null
        var firstTa = true
        while (true) {
            if (y and 1 != 0) { val v = atr[pos++].toInt() and 0xFF; ifaces += InterfaceByte("TA$i", v, taMeaning(i, v, td, firstTa)); if (i == 1) firstTa = false }
            if (y and 2 != 0) { val v = atr[pos++].toInt() and 0xFF; ifaces += InterfaceByte("TB$i", v, tbMeaning(i, v, td)) }
            if (y and 4 != 0) { val v = atr[pos++].toInt() and 0xFF; ifaces += InterfaceByte("TC$i", v, tcMeaning(i, v, td)) }
            if (y and 8 != 0) {
                val v = atr[pos++].toInt() and 0xFF
                val t = v and 0x0F
                if (t !in protocols) protocols += t
                ifaces += InterfaceByte("TD$i", v, "next interface bytes: ${yBits((v shr 4) and 0x0F, i + 1)}; protocol T=$t")
                td = t
                y = (v shr 4) and 0x0F
                i++
            } else break
            if (pos > atr.size) { warnings += "ATR truncated inside the interface bytes"; break }
        }
        val histEnd = minOf(pos + k, atr.size)
        if (pos + k > atr.size) warnings += "T0 announces $k historical bytes but only ${atr.size - pos} are present"
        val hist = atr.copyOfRange(minOf(pos, atr.size), histEnd)
        pos = histEnd
        val needsTck = protocols.any { it != 0 }
        var tck: Int? = null
        var tckValid: Boolean? = null
        if (pos < atr.size) {
            tck = atr[pos].toInt() and 0xFF
            var x = 0
            for (j in 1 until atr.size) x = x xor (atr[j].toInt() and 0xFF)
            tckValid = x == 0
            if (!needsTck) warnings += "TCK present although only T=0 is offered"
            if (pos + 1 < atr.size) warnings += "${atr.size - pos - 1} trailing byte(s) after TCK"
        } else if (needsTck) warnings += "TCK missing although a protocol other than T=0 is offered"
        return Parsed(convention, ifaces, protocols, hist, historicalNotes(hist), tck, tckValid, warnings)
    }

    private fun yBits(y: Int, i: Int): String = listOfNotNull(if (y and 1 != 0) "TA$i" else null, if (y and 2 != 0) "TB$i" else null, if (y and 4 != 0) "TC$i" else null, if (y and 8 != 0) "TD$i" else null).ifEmpty { listOf("none") }.joinToString(", ")

    private fun taMeaning(i: Int, v: Int, td: Int?, first: Boolean): String = when {
        i == 1 -> {
            val fi = FI[(v shr 4) and 0x0F]; val di = DI[v and 0x0F]
            "Fi=${if (fi < 0) "RFU" else fi.toString()}, Di=${if (di < 0) "RFU" else di.toString()}, f(max)=${FMAX[(v shr 4) and 0x0F]} MHz" + if (fi > 0 && di > 0) ", ${fi / di} clock cycles per ETU" else ""
        }
        i == 2 -> "specific mode: protocol T=${v and 0x0F}${if (v and 0x80 != 0) ", cannot change" else ", can change to negotiable"}${if (v and 0x10 != 0) ", implicit Fi/Di" else ", TA1 Fi/Di apply"}"
        td == 1 -> "T=1 IFSC (information field size) = $v bytes"
        td == 15 -> "class indicator: ${when (v and 0x3F) { 1 -> "A (5 V)"; 2 -> "B (3 V)"; 3 -> "A or B"; 4 -> "C (1.8 V)"; 6 -> "B or C"; 7 -> "A, B or C"; else -> "RFU" }}, clock stop ${when ((v shr 6) and 3) { 0 -> "not supported"; 1 -> "state L"; 2 -> "state H"; else -> "no preference" }}"
        else -> "protocol-specific (T=$td)"
    }

    private fun tbMeaning(i: Int, v: Int, td: Int?): String = when {
        i == 1 -> if (v == 0) "no VPP (deprecated)" else "VPP programming (deprecated): PI1=${v and 0x1F}, II=${(v shr 5) and 3}"
        i == 2 -> "VPP PI2 (deprecated)"
        td == 1 -> "T=1 CWI=${v and 0x0F}, BWI=${(v shr 4) and 0x0F}"
        td == 15 -> "SPU: ${if (v == 0) "not used" else if (v and 0x80 != 0) "proprietary" else "standard"} (${v.toString(16).uppercase()})"
        else -> "protocol-specific (T=$td)"
    }

    private fun tcMeaning(i: Int, v: Int, td: Int?): String = when {
        i == 1 -> "extra guard time N=$v${if (v == 255) " (minimum: 1 ETU for T=1, 2 for T=0)" else ""}"
        i == 2 -> "T=0 work waiting time WI=$v"
        td == 1 -> "T=1 error detection: ${if (v and 1 != 0) "CRC" else "LRC"}"
        else -> "protocol-specific (T=$td)"
    }

    private fun historicalNotes(h: ByteArray): List<String> {
        if (h.isEmpty()) return emptyList()
        val notes = mutableListOf<String>()
        val cat = h[0].toInt() and 0xFF
        when {
            cat == 0x00 -> { notes += "Category 00: COMPACT-TLV followed by a 3-byte status indicator"; notes += compactTlv(h.copyOfRange(1, maxOf(1, h.size - 3))); if (h.size >= 4) notes += "Status: ${h.copyOfRange(h.size - 3, h.size).toHexUpper()}" }
            cat == 0x80 -> { notes += "Category 80: COMPACT-TLV"; notes += compactTlv(h.copyOfRange(1, h.size)) }
            cat == 0x10 -> notes += "Category 10: DIR data reference ${if (h.size > 1) h[1].toHexUpper() else "?"}"
            cat in 0x81..0x8F -> notes += "Category ${h[0].toHexUpper()}: RFU"
            else -> notes += "Proprietary historical bytes (category ${h[0].toHexUpper()}); ASCII: \"${h.map { if (it.toInt() in 32..126) it.toInt().toChar() else '.' }.joinToString("")}\""
        }
        return notes
    }

    private fun Byte.toHexUpper() = byteArrayOf(this).toHexUpper()

    private fun compactTlv(data: ByteArray): List<String> {
        val out = mutableListOf<String>()
        var p = 0
        while (p < data.size) {
            val tag = (data[p].toInt() shr 4) and 0x0F
            val len = data[p].toInt() and 0x0F
            p++
            val v = data.copyOfRange(p, minOf(p + len, data.size))
            p += len
            val name = when (tag) { 1 -> "country code"; 2 -> "issuer identification number"; 3 -> "card service data"; 4 -> "initial access data"; 5 -> "card issuer's data"; 6 -> "pre-issuing data"; 7 -> "card capabilities"; 8 -> "status indicator"; 15 -> "application identifier"; else -> "tag $tag" }
            out += "  $name (${tag.toString(16).uppercase()}${len.toString(16).uppercase()}): ${v.toHexUpper()}" + if (tag == 7 && v.isNotEmpty()) cardCapabilities(v) else ""
        }
        return out
    }

    private fun cardCapabilities(v: ByteArray): String {
        val b1 = v[0].toInt() and 0xFF
        val sel = listOfNotNull(if (b1 and 0x80 != 0) "DF by full name" else null, if (b1 and 0x40 != 0) "DF by partial name" else null, if (b1 and 0x20 != 0) "DF by path" else null, if (b1 and 0x10 != 0) "DF by file id" else null, if (b1 and 0x08 != 0) "implicit DF" else null, if (b1 and 0x04 != 0) "short EF id" else null, if (b1 and 0x02 != 0) "record number" else null, if (b1 and 0x01 != 0) "record identifier" else null)
        var s = " - selection: ${sel.joinToString(", ")}"
        if (v.size >= 3) {
            val b3 = v[2].toInt() and 0xFF
            s += "; ${if (b3 and 0x80 != 0) "command chaining" else "no command chaining"}, ${if (b3 and 0x40 != 0) "extended Lc/Le" else "short Lc/Le"}, logical channels: ${when ((b3 shr 3) and 3) { 0 -> "none"; 1 -> "assigned by card"; 2 -> "assigned by interface"; else -> "both" }}, max ${(b3 and 7).let { if (it == 7) ">= 8" else (it + 1).toString() }}"
        }
        return s
    }
}
