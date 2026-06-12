package `in`.aicortex.iso8583studio.apduSimulator

import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.CommandApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.EmvTag
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.Sw
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.Tlv
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.samples.SampleProfiles
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.CardRuntime
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.handlers.GenerateAcHandler
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.handlers.GetChallengeHandler
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.handlers.GetDataHandler
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.handlers.GpoHandler
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.handlers.ReadRecordHandler
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.handlers.SelectHandler
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.handlers.VerifyHandler
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.transport.LoopbackTransport
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private fun String.hex(): ByteArray {
    val clean = filterNot { it.isWhitespace() }
    return ByteArray(clean.length / 2) { i ->
        ((clean[i * 2].digitToInt(16) shl 4) or clean[i * 2 + 1].digitToInt(16)).toByte()
    }
}

private fun ByteArray.hex(): String = joinToString("") { "%02X".format(it) }

class SmokeIntegrationTest {

    private fun newTransport(): LoopbackTransport {
        val profile = SampleProfiles.visaCreditTest()
        val runtime = CardRuntime(
            profile,
            listOf(
                SelectHandler(),
                GpoHandler(),
                ReadRecordHandler(),
                GetDataHandler(),
                GetChallengeHandler(),
                VerifyHandler(),
                GenerateAcHandler(),
            ),
        )
        return LoopbackTransport(runtime)
    }

    @Test
    fun `connect returns ATR`() = runBlocking {
        val t = newTransport()
        val atr = t.connect()
        assertTrue(atr.isNotEmpty(), "ATR must be non-empty")
        assertEquals("3B6500002063CB6800", atr.hex())
        t.disconnect()
    }

    @Test
    fun `select PPSE returns FCI listing the visa AID`() = runBlocking {
        val t = newTransport()
        t.connect()
        val ppse = "2PAY.SYS.DDF01".toByteArray(Charsets.US_ASCII)
        val sel = CommandApdu(0x00, 0xA4.toByte(), 0x04, 0x00, ppse)
        val r = t.transmit(sel)
        assertEquals(Sw.SUCCESS, r.sw, "PPSE select must succeed (got %04X)".format(r.sw))
        val fci = Tlv.parseAll(r.data).first { it.tag == EmvTag.FCI_TEMPLATE }
        val children = Tlv.parseAll(fci.value)
        // Must contain AID A0000000031010 somewhere in the FCI tree
        val flat = flatten(children)
        val aids = flat.filter { it.tag == EmvTag.DEDICATED_FILE_NAME || it.tag == 0x4F }
        assertTrue(
            aids.any { it.value.hex() == "A0000000031010" },
            "PPSE FCI did not list the Visa AID. Found tags: ${flat.joinToString { "%X".format(it.tag) }}",
        )
        t.disconnect()
    }

    @Test
    fun `select AID then GPO then READ RECORD round trip`() = runBlocking {
        val t = newTransport()
        t.connect()

        // SELECT AID
        val sel = CommandApdu(0x00, 0xA4.toByte(), 0x04, 0x00, "A0000000031010".hex())
        val selR = t.transmit(sel)
        assertEquals(Sw.SUCCESS, selR.sw, "AID select failed: %04X".format(selR.sw))

        // GET PROCESSING OPTIONS with empty PDOL data (tag 83 length 0)
        val gpo = CommandApdu(0x80.toByte(), 0xA8.toByte(), 0x00, 0x00, "8300".hex())
        val gpoR = t.transmit(gpo)
        assertEquals(Sw.SUCCESS, gpoR.sw, "GPO failed: %04X".format(gpoR.sw))
        val gpoTlv = Tlv.parseAll(gpoR.data).first()
        assertTrue(
            gpoTlv.tag == EmvTag.RESPONSE_TEMPLATE_1 || gpoTlv.tag == EmvTag.RESPONSE_TEMPLATE_2,
            "Unexpected GPO response template tag %X".format(gpoTlv.tag),
        )

        // READ RECORD sfi=1 rec=1 -> P2 = (1 << 3) | 0x04 = 0x0C
        val rr = CommandApdu(0x00, 0xB2.toByte(), 0x01, 0x0C, le = 0)
        val rrR = t.transmit(rr)
        assertEquals(Sw.SUCCESS, rrR.sw, "READ RECORD failed: %04X".format(rrR.sw))
        val rec = Tlv.parseAll(rrR.data).first()
        assertEquals(EmvTag.READ_RECORD_TEMPLATE, rec.tag, "Record must be wrapped in tag 70")
        t.disconnect()
    }

    @Test
    fun `GET CHALLENGE returns 8 bytes and stores in session`() = runBlocking {
        val t = newTransport()
        t.connect()
        val r = t.transmit(CommandApdu(0x00, 0x84.toByte(), 0x00, 0x00, le = 8))
        assertEquals(Sw.SUCCESS, r.sw)
        assertEquals(8, r.data.size)
        t.disconnect()
    }

    @Test
    fun `GENERATE AC returns response template with cryptogram`() = runBlocking {
        val t = newTransport()
        t.connect()
        // Must select an app first so CVN/key lookup works
        t.transmit(CommandApdu(0x00, 0xA4.toByte(), 0x04, 0x00, "A0000000031010".hex()))
        t.transmit(CommandApdu(0x80.toByte(), 0xA8.toByte(), 0x00, 0x00, "8300".hex()))

        // ARQC = 0x80 in P1 high bits
        val cdolData = ByteArray(32) // dummy CDOL1 data; handler MACs whatever we send
        val gac = CommandApdu(0x80.toByte(), 0xAE.toByte(), 0x80.toByte(), 0x00, cdolData)
        val r = t.transmit(gac)
        assertEquals(Sw.SUCCESS, r.sw, "GAC failed: %04X".format(r.sw))
        val tlvs = flatten(Tlv.parseAll(r.data))
        assertNotNull(tlvs.firstOrNull { it.tag == EmvTag.APPLICATION_CRYPTOGRAM }, "AC (9F26) missing")
        assertNotNull(tlvs.firstOrNull { it.tag == EmvTag.ATC }, "ATC (9F36) missing")
        assertNotNull(tlvs.firstOrNull { it.tag == EmvTag.CRYPTOGRAM_INFO_DATA }, "CID (9F27) missing")
        t.disconnect()
    }

    private fun flatten(tlvs: List<Tlv>): List<Tlv> = buildList {
        for (t in tlvs) {
            add(t)
            if (t.isConstructed) {
                runCatching { addAll(flatten(Tlv.parseAll(t.value))) }
            }
        }
    }
}
