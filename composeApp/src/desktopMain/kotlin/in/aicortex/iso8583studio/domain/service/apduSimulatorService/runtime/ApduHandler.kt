package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime

import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.CommandApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.ResponseApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.CardApplication
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.CardProfile

/**
 * Handles one ISO 7816 INS. Implementations must be stateless across calls — all mutable state
 * lives in [CardSession].
 *
 * `matches(c)` returns true if this handler should serve [c] given the current session. Only one
 * handler is selected per APDU (first-match wins via dispatcher registration order).
 */
interface ApduHandler {
    fun matches(command: CommandApdu, session: CardSession): Boolean
    suspend fun handle(command: CommandApdu, session: CardSession): ResponseApdu
}

/**
 * Mutable per-session state held by the runtime. Lives for the lifetime of one card power cycle.
 *
 * The session never mutates [profile] — that's a read-only template. Anything that changes during
 * a transaction (ATC, PIN tries, currently-selected app, GPO output buffer) lives in [state].
 */
class CardSession(
    val profile: CardProfile,
    var state: SessionState = SessionState(),
) {
    fun selectApplication(app: CardApplication?) {
        state.selectedApplication = app
    }
}

class SessionState {
    var selectedApplication: CardApplication? = null
    var atc: Int = 0
    var pinTriesRemaining: Int = 3
    /** Last challenge issued by GET CHALLENGE; consumed by EXTERNAL AUTHENTICATE / GAC. */
    var lastChallenge: ByteArray? = null
    /** Buffered response too long for one APDU; subsequent GET RESPONSE drains it. */
    var pendingResponse: ByteArray? = null
}
