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
 * GET PROCESSING OPTIONS (80 A8 00 00). Returns response template format 1 (tag 80) with AIP||AFL.
 */
class GpoHandler : ApduHandler {

    override fun matches(command: CommandApdu, session: CardSession): Boolean =
        command.cla == 0x80.toByte() &&
            command.ins == 0xA8.toByte() &&
            command.p1 == 0x00.toByte() &&
            command.p2 == 0x00.toByte()

    override suspend fun handle(command: CommandApdu, session: CardSession): ResponseApdu {
        val app = session.state.selectedApplication
            ?: return ResponseApdu.error(Sw.CONDITIONS_NOT_SATISFIED)
        val body = command.data ?: return ResponseApdu.error(Sw.WRONG_LENGTH)

        // Expect a tag-83 wrapper carrying PDOL-related data; we don't validate its contents.
        runCatching { Tlv.parseAll(body).firstOrNull { it.tag == 0x83 } }

        val aip = app.aip.hexToBytes()
        val afl = app.afl.hexToBytes()
        val payload = aip + afl

        val tlv = Tlv.primitive(EmvTag.RESPONSE_TEMPLATE_1, payload)
        return ResponseApdu.ok(tlv.encode())
    }
}
