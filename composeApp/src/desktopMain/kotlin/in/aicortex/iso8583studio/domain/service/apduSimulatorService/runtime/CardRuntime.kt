package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime

import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.CommandApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.ResponseApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.Sw
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.CardProfile

/**
 * Pure-Kotlin EMV card runtime. The runtime owns the active [CardSession] and dispatches incoming
 * APDUs to the first matching [ApduHandler]. Handlers are registered at construction; the order
 * of registration is the matching order.
 *
 * The runtime itself does not know about transports — it just consumes [CommandApdu] and produces
 * [ResponseApdu]. Use it directly via LoopbackTransport (Mode C) or feed it from a USB-CDC reader
 * loop on the STM32 bridge (Mode A).
 */
class CardRuntime(
    profile: CardProfile,
    private val handlers: List<ApduHandler>,
) {
    val session: CardSession = CardSession(profile)

    suspend fun process(command: CommandApdu): ResponseApdu {
        val handler = handlers.firstOrNull { it.matches(command, session) }
            ?: return ResponseApdu.error(Sw.INS_NOT_SUPPORTED)
        return runCatching { handler.handle(command, session) }
            .getOrElse { ResponseApdu.error(Sw.UNKNOWN) }
    }
}
