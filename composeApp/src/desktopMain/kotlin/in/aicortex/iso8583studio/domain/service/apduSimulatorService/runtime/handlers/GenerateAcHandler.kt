package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.handlers

import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.CommandApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.EmvTag
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.ResponseApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.Sw
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.Tlv
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.hexToBytes
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.ApduHandler
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.CardSession
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.crypto.AcGeneration
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.crypto.SessionKey

/**
 * GENERATE AC (CLA=80 INS=AE). Returns a format-2 (tag 77) response template containing CID, ATC,
 * AC, and a static Issuer Application Data placeholder.
 *
 * AC type from P1 bits 7..6:
 *  - 00xxxxxx -> AAC (decline)         CID = 0x00
 *  - 01xxxxxx -> TC  (offline approve) CID = 0x40
 *  - 10xxxxxx -> ARQC (online auth)    CID = 0x80
 */
class GenerateAcHandler : ApduHandler {

    override fun matches(command: CommandApdu, session: CardSession): Boolean =
        command.cla == 0x80.toByte() && command.ins == 0xAE.toByte()

    override suspend fun handle(command: CommandApdu, session: CardSession): ResponseApdu {
        val cdolData = command.data ?: return ResponseApdu.error(Sw.WRONG_LENGTH)
        val app = session.state.selectedApplication
            ?: return ResponseApdu.error(Sw.CONDITIONS_NOT_SATISFIED)

        // Increment ATC before use; clamp to 16 bits.
        session.state.atc = (session.state.atc + 1) and 0xFFFF
        val atc = session.state.atc
        val atcBytes = byteArrayOf(((atc ushr 8) and 0xFF).toByte(), (atc and 0xFF).toByte())

        val cid: Byte = when ((command.p1.toInt() and 0xC0)) {
            0x00 -> 0x00.toByte() // AAC
            0x40 -> 0x40.toByte() // TC
            0x80 -> 0x80.toByte() // ARQC
            else -> 0x80.toByte()
        }

        // Locate UDK (or fall back to a 16-byte zero key for test mode).
        val udk: ByteArray = run {
            val keyId = app.issuerKeyId
            val key = if (keyId != null) session.profile.keys.firstOrNull { it.id == keyId } else null
            val hex = key?.udk
            if (hex.isNullOrEmpty()) ByteArray(16) else hex.hexToBytes()
        }

        // Pick session-key derivation by CVN.
        val unBytes = session.state.lastChallenge ?: ByteArray(4)
        val sessionKey: ByteArray = when (app.cvn) {
            10 -> SessionKey.visaCommon(udk, atc, unBytes)
            18, 1 -> SessionKey.emvCommon(udk, atc)
            else -> SessionKey.emvCommon(udk, atc)
        }

        val macInput = cdolData + atcBytes
        val ac = AcGeneration.computeTdesMac(sessionKey, macInput).copyOfRange(0, 8)

        // 9F10 Issuer Application Data — 7-byte static placeholder for now.
        val iad = "06010A03A00000".hexToBytes()

        val template = Tlv.constructed(
            EmvTag.RESPONSE_TEMPLATE_2,
            listOf(
                Tlv.primitive(EmvTag.CRYPTOGRAM_INFO_DATA, byteArrayOf(cid)),
                Tlv.primitive(EmvTag.ATC, atcBytes),
                Tlv.primitive(EmvTag.APPLICATION_CRYPTOGRAM, ac),
                Tlv.primitive(EmvTag.ISSUER_APPLICATION_DATA, iad),
            ),
        ).encode()

        return ResponseApdu.ok(template)
    }
}
