package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.handlers

import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.CommandApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.EmvTag
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.ResponseApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.Sw
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.Tlv
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.hexToBytes
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.ApduHandler
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.CardSession

/**
 * READ RECORD (00 B2 rr ss). P1 = record number, P2 = (SFI<<3) | 0x04.
 */
class ReadRecordHandler : ApduHandler {

    override fun matches(command: CommandApdu, session: CardSession): Boolean =
        command.cla == 0x00.toByte() && command.ins == 0xB2.toByte()

    override suspend fun handle(command: CommandApdu, session: CardSession): ResponseApdu {
        val app = session.state.selectedApplication
            ?: return ResponseApdu.error(Sw.CONDITIONS_NOT_SATISFIED)

        val record = command.p1.toInt() and 0xFF
        val p2 = command.p2.toInt() and 0xFF
        if ((p2 and 0x07) != 0x04) return ResponseApdu.error(Sw.INCORRECT_P1P2)
        val sfi = (p2 ushr 3) and 0x1F
        if (sfi == 0 || record == 0) return ResponseApdu.error(Sw.INCORRECT_P1P2)

        val rec = app.records.firstOrNull { it.sfi == sfi && it.record == record }
            ?: return ResponseApdu.error(Sw.RECORD_NOT_FOUND)

        val body = rec.tlvHex.hexToBytes()
        val template = Tlv.primitive(EmvTag.READ_RECORD_TEMPLATE, body)
        return ResponseApdu.ok(template.encode())
    }
}
