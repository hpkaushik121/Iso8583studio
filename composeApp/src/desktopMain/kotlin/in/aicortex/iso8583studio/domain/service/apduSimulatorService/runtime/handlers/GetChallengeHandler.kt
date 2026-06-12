package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.handlers

import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.CommandApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.ResponseApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.ApduHandler
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.CardSession
import java.security.SecureRandom

/**
 * GET CHALLENGE (00 84 00 00 08). Returns 8 random bytes and stores them in the session.
 */
class GetChallengeHandler(
    private val random: SecureRandom = SecureRandom(),
) : ApduHandler {

    override fun matches(command: CommandApdu, session: CardSession): Boolean =
        command.cla == 0x00.toByte() &&
            command.ins == 0x84.toByte() &&
            command.p1 == 0x00.toByte() &&
            command.p2 == 0x00.toByte()

    override suspend fun handle(command: CommandApdu, session: CardSession): ResponseApdu {
        val challenge = ByteArray(CHALLENGE_LEN)
        random.nextBytes(challenge)
        session.state.lastChallenge = challenge
        return ResponseApdu.ok(challenge)
    }

    companion object {
        const val CHALLENGE_LEN = 8
    }
}
