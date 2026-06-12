package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core

/**
 * ISO 7816-4 command APDU. Supports short Lc/Le only (extended length is not part of EMV contact).
 *
 *   CLA INS P1 P2 [Lc Data] [Le]
 *
 * `data` is null when there is no command body. `le` is null when no response body is expected.
 */
data class CommandApdu(
    val cla: Byte,
    val ins: Byte,
    val p1: Byte,
    val p2: Byte,
    val data: ByteArray? = null,
    val le: Int? = null,
) {
    init {
        data?.let { require(it.size in 1..255) { "Lc must be 1..255, was ${it.size}" } }
        le?.let { require(it in 0..256) { "Le must be 0..256, was $it" } }
    }

    fun toBytes(): ByteArray {
        val out = ArrayList<Byte>(5 + (data?.size ?: 0) + (if (le != null) 1 else 0))
        out += cla; out += ins; out += p1; out += p2
        if (data != null) {
            out += data.size.toByte()
            out += data.toList()
        }
        if (le != null) {
            out += (if (le == 256) 0 else le).toByte()
        }
        return out.toByteArray()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CommandApdu) return false
        return cla == other.cla && ins == other.ins && p1 == other.p1 && p2 == other.p2 &&
            (data?.contentEquals(other.data) ?: (other.data == null)) && le == other.le
    }

    override fun hashCode(): Int {
        var r = cla.toInt()
        r = 31 * r + ins; r = 31 * r + p1; r = 31 * r + p2
        r = 31 * r + (data?.contentHashCode() ?: 0)
        r = 31 * r + (le ?: -1)
        return r
    }

    override fun toString(): String = buildString {
        append("%02X%02X%02X%02X".format(cla, ins, p1, p2))
        if (data != null) append(" Lc=%02X ".format(data.size)).also { append(data.toHex()) }
        if (le != null) append(" Le=%02X".format(if (le == 256) 0 else le))
    }

    companion object {
        fun parse(raw: ByteArray): CommandApdu {
            require(raw.size >= 4) { "APDU too short: ${raw.size}" }
            val cla = raw[0]; val ins = raw[1]; val p1 = raw[2]; val p2 = raw[3]
            return when (raw.size) {
                4 -> CommandApdu(cla, ins, p1, p2)
                5 -> {
                    val le = raw[4].toInt() and 0xFF
                    CommandApdu(cla, ins, p1, p2, le = if (le == 0) 256 else le)
                }
                else -> {
                    val lc = raw[4].toInt() and 0xFF
                    require(lc > 0) { "Lc=0 in case-3/4 APDU is invalid" }
                    when (raw.size) {
                        5 + lc -> CommandApdu(cla, ins, p1, p2, raw.copyOfRange(5, 5 + lc))
                        6 + lc -> {
                            val le = raw[5 + lc].toInt() and 0xFF
                            CommandApdu(cla, ins, p1, p2, raw.copyOfRange(5, 5 + lc), if (le == 0) 256 else le)
                        }
                        else -> error("APDU length mismatch: size=${raw.size}, lc=$lc")
                    }
                }
            }
        }
    }
}

/**
 * ISO 7816-4 response APDU.
 */
data class ResponseApdu(
    val data: ByteArray,
    val sw1: Byte,
    val sw2: Byte,
) {
    val sw: Int get() = ((sw1.toInt() and 0xFF) shl 8) or (sw2.toInt() and 0xFF)
    val isSuccess: Boolean get() = sw == Sw.SUCCESS
    fun toBytes(): ByteArray = data + byteArrayOf(sw1, sw2)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ResponseApdu) return false
        return sw1 == other.sw1 && sw2 == other.sw2 && data.contentEquals(other.data)
    }

    override fun hashCode(): Int = 31 * (31 * data.contentHashCode() + sw1) + sw2

    override fun toString(): String =
        if (data.isEmpty()) "SW=%04X".format(sw)
        else "${data.toHex()} SW=%04X".format(sw)

    companion object {
        fun ok(data: ByteArray = ByteArray(0)) =
            ResponseApdu(data, ((Sw.SUCCESS shr 8) and 0xFF).toByte(), (Sw.SUCCESS and 0xFF).toByte())

        fun error(sw: Int, data: ByteArray = ByteArray(0)) = ResponseApdu(
            data,
            ((sw shr 8) and 0xFF).toByte(),
            (sw and 0xFF).toByte(),
        )

        fun parse(raw: ByteArray): ResponseApdu {
            require(raw.size >= 2) { "Response too short: ${raw.size}" }
            return ResponseApdu(raw.copyOfRange(0, raw.size - 2), raw[raw.size - 2], raw[raw.size - 1])
        }
    }
}
