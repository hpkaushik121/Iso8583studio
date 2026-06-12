package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.handlers

import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.CommandApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.EmvTag
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.ResponseApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.Sw
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.Tlv
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.hexToBytes
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.CardApplication
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.ApduHandler
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.CardSession

/**
 * SELECT (00 A4 04 00). Handles PSE (`1PAY.SYS.DDF01`), PPSE (`2PAY.SYS.DDF01`) and AID select.
 */
class SelectHandler : ApduHandler {

    override fun matches(command: CommandApdu, session: CardSession): Boolean =
        command.cla == 0x00.toByte() &&
            command.ins == 0xA4.toByte() &&
            command.p1 == 0x04.toByte() &&
            command.p2 == 0x00.toByte()

    override suspend fun handle(command: CommandApdu, session: CardSession): ResponseApdu {
        val name = command.data ?: return ResponseApdu.error(Sw.WRONG_LENGTH)
        val nameStr = runCatching { String(name, Charsets.US_ASCII) }.getOrNull()

        if (nameStr == PSE || nameStr == PPSE) {
            return selectDirectory(session, nameStr)
        }

        val aidHex = name.toUpperHex()
        val app = session.profile.applications.firstOrNull { it.aid.uppercase().startsWith(aidHex) }
            ?: return ResponseApdu.error(Sw.FILE_NOT_FOUND)

        session.selectApplication(app)
        return ResponseApdu.ok(buildAidFci(app))
    }

    private fun selectDirectory(session: CardSession, dfName: String): ResponseApdu {
        val apps = session.profile.applications
        if (apps.isEmpty()) return ResponseApdu.error(Sw.FILE_NOT_FOUND)

        // FCI Issuer Discretionary Data (BF0C) for PPSE / SFI for PSE both wrap directory entries (61).
        val entries = apps.map { app ->
            val children = mutableListOf<Tlv>()
            children += Tlv.primitive(EmvTag.DEDICATED_FILE_NAME, app.aid.hexToBytes())
            if (app.label.isNotEmpty()) {
                children += Tlv.primitive(EmvTag.APPLICATION_LABEL, app.label.toByteArray(Charsets.US_ASCII))
            }
            children += Tlv.primitive(EmvTag.APPLICATION_PRIORITY, byteArrayOf(app.priority.toByte()))
            Tlv.constructed(EmvTag.APPLICATION_TEMPLATE, children)
        }

        val proprietary = Tlv.constructed(
            EmvTag.FCI_PROPRIETARY,
            listOf(Tlv.constructed(EmvTag.FCI_ISSUER_DISCRETIONARY, entries)),
        )

        val fci = Tlv.constructed(
            EmvTag.FCI_TEMPLATE,
            listOf(
                Tlv.primitive(EmvTag.DEDICATED_FILE_NAME, dfName.toByteArray(Charsets.US_ASCII)),
                proprietary,
            ),
        )
        return ResponseApdu.ok(fci.encode())
    }

    private fun buildAidFci(app: CardApplication): ByteArray {
        val a5Children = mutableListOf<Tlv>()
        if (app.label.isNotEmpty()) {
            a5Children += Tlv.primitive(EmvTag.APPLICATION_LABEL, app.label.toByteArray(Charsets.US_ASCII))
        }
        a5Children += Tlv.primitive(EmvTag.APPLICATION_PRIORITY, byteArrayOf(app.priority.toByte()))
        if (app.pdol.isNotEmpty()) {
            a5Children += Tlv.primitive(EmvTag.PDOL, app.pdol.hexToBytes())
        }

        val fci = Tlv.constructed(
            EmvTag.FCI_TEMPLATE,
            listOf(
                Tlv.primitive(EmvTag.DEDICATED_FILE_NAME, app.aid.hexToBytes()),
                Tlv.constructed(EmvTag.FCI_PROPRIETARY, a5Children),
            ),
        )
        return fci.encode()
    }

    private fun ByteArray.toUpperHex(): String =
        joinToString("") { "%02X".format(it) }

    companion object {
        const val PSE = "1PAY.SYS.DDF01"
        const val PPSE = "2PAY.SYS.DDF01"
    }
}
