package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.handlers

import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.CommandApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.ResponseApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.Sw
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.hexToBytes
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.ApduHandler
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.CardSession

/**
 * VERIFY (00 20 00 80) — plaintext offline PIN. Compares the command body against the selected
 * application's offline PIN block. Decrements the per-session PIN try counter on mismatch.
 */
class VerifyHandler : ApduHandler {

    override fun matches(command: CommandApdu, session: CardSession): Boolean =
        command.cla == 0x00.toByte() &&
            command.ins == 0x20.toByte() &&
            command.p1 == 0x00.toByte() &&
            command.p2 == 0x80.toByte()

    override suspend fun handle(command: CommandApdu, session: CardSession): ResponseApdu {
        val app = session.state.selectedApplication
            ?: return ResponseApdu.error(Sw.CONDITIONS_NOT_SATISFIED)
        val expected = app.offlinePinBlock
            ?: return ResponseApdu.error(Sw.CONDITIONS_NOT_SATISFIED)

        val supplied = command.data ?: return ResponseApdu.error(Sw.WRONG_LENGTH)

        if (session.state.pinTriesRemaining <= 0) {
            return ResponseApdu.error(Sw.AUTH_METHOD_BLOCKED)
        }

        val expectedBytes = expected.hexToBytes()
        if (supplied.contentEquals(expectedBytes)) {
            session.state.pinTriesRemaining = app.pinTryLimit
            return ResponseApdu.ok()
        }

        val remaining = (session.state.pinTriesRemaining - 1).coerceAtLeast(0)
        session.state.pinTriesRemaining = remaining
        return if (remaining == 0) {
            ResponseApdu.error(Sw.AUTH_METHOD_BLOCKED)
        } else {
            ResponseApdu.error(Sw.pinTriesRemaining(remaining))
        }
    }
}
