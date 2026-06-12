package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.transport

/**
 * Wire framing for the STM32 USB-CDC bridge:
 *
 *     0xA5 | len_lo | len_hi | <payload bytes...> | crc16_lo | crc16_hi
 *
 * `len` is the payload length (little-endian, NOT including framing or CRC).
 * `payload[0]` is a type byte; the rest depends on the type. CRC-16/CCITT-FALSE is
 * computed over the payload bytes only.
 */
internal object SerialFraming {
    const val START_BYTE: Byte = 0xA5.toByte()

    /** Build a complete frame from a type byte and an optional body. */
    fun encode(type: Byte, body: ByteArray): ByteArray {
        val payloadLen = 1 + body.size
        val payload = ByteArray(payloadLen)
        payload[0] = type
        if (body.isNotEmpty()) System.arraycopy(body, 0, payload, 1, body.size)
        val crc = Crc16Ccitt.compute(payload)
        val out = ByteArray(3 + payloadLen + 2)
        out[0] = START_BYTE
        out[1] = (payloadLen and 0xFF).toByte()
        out[2] = ((payloadLen shr 8) and 0xFF).toByte()
        System.arraycopy(payload, 0, out, 3, payloadLen)
        out[3 + payloadLen] = (crc and 0xFF).toByte()
        out[3 + payloadLen + 1] = ((crc shr 8) and 0xFF).toByte()
        return out
    }

    /** A successfully decoded frame. `body` is the payload bytes after the type byte. */
    data class Frame(val type: Byte, val body: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Frame) return false
            return type == other.type && body.contentEquals(other.body)
        }

        override fun hashCode(): Int = 31 * type.toInt() + body.contentHashCode()
    }

    /**
     * Incremental, byte-fed parser. Call [feed] with each byte that arrives; it returns a
     * complete frame when one is available, or null otherwise. On CRC mismatch the parser
     * resyncs by hunting for the next 0xA5 start byte.
     */
    class Decoder {
        private enum class State { HUNT, LEN_LO, LEN_HI, PAYLOAD, CRC_LO, CRC_HI }

        private var state: State = State.HUNT
        private var lenLo: Int = 0
        private var expectedLen: Int = 0
        private var payload: ByteArray = ByteArray(0)
        private var payloadIdx: Int = 0
        private var crcLo: Int = 0

        fun feed(b: Byte): Frame? {
            val v = b.toInt() and 0xFF
            when (state) {
                State.HUNT -> {
                    if (v == 0xA5) state = State.LEN_LO
                }

                State.LEN_LO -> {
                    lenLo = v
                    state = State.LEN_HI
                }

                State.LEN_HI -> {
                    expectedLen = lenLo or (v shl 8)
                    if (expectedLen <= 0 || expectedLen > 4096) {
                        // Implausible length: resync.
                        resyncOn(v)
                    } else {
                        payload = ByteArray(expectedLen)
                        payloadIdx = 0
                        state = State.PAYLOAD
                    }
                }

                State.PAYLOAD -> {
                    payload[payloadIdx++] = b
                    if (payloadIdx >= expectedLen) state = State.CRC_LO
                }

                State.CRC_LO -> {
                    crcLo = v
                    state = State.CRC_HI
                }

                State.CRC_HI -> {
                    val received = crcLo or (v shl 8)
                    val computed = Crc16Ccitt.compute(payload)
                    val finishedPayload = payload
                    state = State.HUNT
                    payload = ByteArray(0)
                    payloadIdx = 0
                    expectedLen = 0
                    if (received == computed && finishedPayload.isNotEmpty()) {
                        val type = finishedPayload[0]
                        val body = if (finishedPayload.size > 1)
                            finishedPayload.copyOfRange(1, finishedPayload.size)
                        else ByteArray(0)
                        return Frame(type, body)
                    }
                    // CRC mismatch (or empty payload) — drop and resync.
                }
            }
            return null
        }

        private fun resyncOn(v: Int) {
            state = if (v == 0xA5) State.LEN_LO else State.HUNT
            payload = ByteArray(0)
            payloadIdx = 0
            expectedLen = 0
        }
    }
}
