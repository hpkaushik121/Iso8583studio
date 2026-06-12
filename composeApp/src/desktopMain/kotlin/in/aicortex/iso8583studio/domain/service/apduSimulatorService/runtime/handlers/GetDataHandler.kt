package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.handlers

import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.CommandApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.ResponseApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.Sw
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.Tlv
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.ApduHandler
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.CardSession

/**
 * GET DATA (80 CA xx xx). Supports tags 9F36 (ATC), 9F13 (last online ATC) and 9F17 (PIN try counter).
 */
class GetDataHandler : ApduHandler {

    override fun matches(command: CommandApdu, session: CardSession): Boolean =
        command.cla == 0x80.toByte() && command.ins == 0xCA.toByte()

    override suspend fun handle(command: CommandApdu, session: CardSession): ResponseApdu {
        val tag = ((command.p1.toInt() and 0xFF) shl 8) or (command.p2.toInt() and 0xFF)
        return when (tag) {
            0x9F36 -> {
                val atc = session.state.atc and 0xFFFF
                val data = byteArrayOf(((atc ushr 8) and 0xFF).toByte(), (atc and 0xFF).toByte())
                ResponseApdu.ok(Tlv.primitive(0x9F36, data).encode())
            }
            0x9F13 -> ResponseApdu.ok(Tlv.primitive(0x9F13, byteArrayOf(0x00, 0x00)).encode())
            0x9F17 -> ResponseApdu.ok(
                Tlv.primitive(0x9F17, byteArrayOf((session.state.pinTriesRemaining and 0xFF).toByte())).encode()
            )
            else -> ResponseApdu.error(Sw.REFERENCED_DATA_NOT_FOUND)
        }
    }
}
