package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.transport

/**
 * CRC-16/CCITT-FALSE: poly 0x1021, init 0xFFFF, no input/output reflection, no final xor.
 *
 * Used by the STM32 firmware framing protocol to validate frame payloads.
 */
internal object Crc16Ccitt {
    fun compute(data: ByteArray, off: Int = 0, len: Int = data.size - off): Int {
        var crc = 0xFFFF
        val end = off + len
        for (i in off until end) {
            crc = crc xor ((data[i].toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if ((crc and 0x8000) != 0) ((crc shl 1) xor 0x1021) and 0xFFFF
                else (crc shl 1) and 0xFFFF
            }
        }
        return crc and 0xFFFF
    }
}
