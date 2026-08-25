package `in`.aicortex.iso8583studio.hsmCommand

import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.HeaderFormat
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.HsmVendorType
import `in`.aicortex.iso8583studio.ui.screens.hsmCommand.HsmFraming
import `in`.aicortex.iso8583studio.ui.screens.hsmCommand.HsmMessageDirection
import `in`.aicortex.iso8583studio.ui.screens.hsmCommand.HsmVendorParsers
import `in`.aicortex.iso8583studio.ui.screens.hsmCommand.InputEncoding
import `in`.aicortex.iso8583studio.ui.screens.hsmCommand.decodeInput
import `in`.aicortex.iso8583studio.ui.screens.hsmCommand.detectDirection
import `in`.aicortex.iso8583studio.ui.screens.hsmCommand.hexDump
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HsmVendorParserTest {

    /** Builds a frame the way HsmCommandClientService.buildFrame does. */
    private fun frame(payload: String, framing: HsmFraming, includeStxEtx: Boolean = true): ByteArray {
        val body = (framing.messageHeader + payload + framing.trailer).toByteArray(Charsets.US_ASCII)
        return when (framing.headerFormat) {
            HeaderFormat.TWO_BYTE_LENGTH ->
                if (framing.tcpLengthEnabled)
                    byteArrayOf(((body.size shr 8) and 0xFF).toByte(), (body.size and 0xFF).toByte()) + body
                else body
            HeaderFormat.FOUR_BYTE_ASCII_LENGTH ->
                "%04d".format(body.size).toByteArray(Charsets.US_ASCII) + body
            HeaderFormat.STX_ETX ->
                if (includeStxEtx) byteArrayOf(0x02) + body + byteArrayOf(0x03) else body
            HeaderFormat.NONE, HeaderFormat.CUSTOM -> body
        }
    }

    private val thalesFraming = HsmFraming(HeaderFormat.TWO_BYTE_LENGTH, true, "0000")

    @Test
    fun `thales request is split into its command definition fields`() {
        val parser = HsmVendorParsers.forVendor(HsmVendorType.THALES_PAYSHIELD)
        // A0 = Generate Key: mode "0", key type "001", key scheme "U"
        val parsed = parser.parseRequest(frame("A00001U", thalesFraming), thalesFraming)

        assertEquals("A0", parsed.commandCode)
        assertEquals("Generate a Key", parsed.commandName)
        assertEquals(listOf("0", "001", "U"), parsed.fields.take(3).map { it.second })
        assertTrue(parsed.formatted.contains("Command Code"), parsed.formatted)
        assertTrue(parsed.formatted.contains("[0000]"), parsed.formatted)
    }

    @Test
    fun `thales response error code is decoded and described`() {
        val parser = HsmVendorParsers.forVendor(HsmVendorType.THALES_PAYSHIELD)
        val request = parser.parseRequest(frame("NC", thalesFraming), thalesFraming)
        val response = parser.parseResponse(frame("ND15", thalesFraming), thalesFraming, request)

        assertEquals("ND", response.commandCode)
        assertEquals("15", response.status?.code)
        assertEquals(false, response.status?.success)
        assertTrue(response.status!!.description.contains("Invalid input data"))
    }

    @Test
    fun `thales success response reports no error`() {
        val parser = HsmVendorParsers.forVendor(HsmVendorType.THALES_PAYSHIELD)
        val response = parser.parseResponse(frame("ND00", thalesFraming), thalesFraming)
        assertEquals(true, response.status?.success)
    }

    @Test
    fun `futurex splits semicolon delimited tag fields`() {
        val framing = HsmFraming(HeaderFormat.TWO_BYTE_LENGTH, true, "")
        val parser = HsmVendorParsers.forVendor(HsmVendorType.FUTUREX)
        val parsed = parser.parseRequest(frame("[AOBN1234;ASABCD;]", framing), framing)

        assertEquals("AO", parsed.commandCode)
        assertEquals(listOf("Tag BN", "Tag AS"), parsed.fields.map { it.first.name })
        assertEquals(listOf("1234", "ABCD"), parsed.fields.map { it.second })
    }

    @Test
    fun `futurex recognises the console's own command mnemonics`() {
        val framing = HsmFraming(HeaderFormat.TWO_BYTE_LENGTH, true, "")
        val parser = HsmVendorParsers.forVendor(HsmVendorType.FUTUREX)
        val parsed = parser.parseRequest(frame("ECHO", framing), framing)

        assertEquals("ECHO", parsed.commandCode)
        assertEquals("Echo Test", parsed.commandName)
    }

    @Test
    fun `atalla splits hash delimited positional parameters`() {
        val framing = HsmFraming(HeaderFormat.STX_ETX, false, "")
        val parser = HsmVendorParsers.forVendor(HsmVendorType.ATALLA)
        val parsed = parser.parseRequest(frame("11#AABB#CCDD#", framing), framing)

        assertEquals("11#", parsed.commandCode)
        assertEquals("Encrypt Under MFK", parsed.commandName)
        assertEquals(listOf("AABB", "CCDD"), parsed.fields.map { it.second })
    }

    @Test
    fun `atalla response without stx etx is still decoded`() {
        // readResponse() consumes the STX/ETX bytes, so responses reach the parser bare.
        val framing = HsmFraming(HeaderFormat.STX_ETX, false, "")
        val parser = HsmVendorParsers.forVendor(HsmVendorType.ATALLA)
        val parsed = parser.parseResponse(frame("21#OK#", framing, includeStxEtx = false), framing)

        assertEquals("21#", parsed.commandCode)
        assertEquals(listOf("OK"), parsed.fields.map { it.second })
    }

    @Test
    fun `ncipher four byte ascii length header is stripped`() {
        val framing = HsmFraming(HeaderFormat.FOUR_BYTE_ASCII_LENGTH, true, "")
        val parser = HsmVendorParsers.forVendor(HsmVendorType.NCIPHER)
        val parsed = parser.parseRequest(frame("STAT01", framing), framing)

        assertEquals("STAT", parsed.commandCode)
        assertEquals("Status", parsed.commandName)
        assertEquals(listOf("01"), parsed.fields.map { it.second })
        assertTrue(parsed.formatted.contains("Length Header"), parsed.formatted)
    }

    @Test
    fun `header format from connection settings wins over the vendor default`() {
        // Thales defaults to a 2-byte binary length, but the config may say otherwise.
        val framing = HsmFraming(HeaderFormat.NONE, tcpLengthEnabled = false, messageHeader = "0000")
        val parser = HsmVendorParsers.forVendor(HsmVendorType.THALES_PAYSHIELD)
        val parsed = parser.parseRequest(frame("NC", framing), framing)

        assertEquals("NC", parsed.commandCode)
        assertTrue(!parsed.formatted.contains("TCP/IP Header"), parsed.formatted)
    }

    @Test
    fun `response header is measured by the configured header length`() {
        // The device echoes a header of the configured length that differs from the one we send.
        val framing = HsmFraming(
            HeaderFormat.TWO_BYTE_LENGTH,
            tcpLengthEnabled = true,
            messageHeader = "0000",
            messageHeaderLength = 4,
        )
        val parser = HsmVendorParsers.forVendor(HsmVendorType.THALES_PAYSHIELD)
        val echoed = framing.copy(messageHeader = "HDR1")
        val response = parser.parseResponse(frame("ND00", echoed), framing)

        assertEquals("ND", response.commandCode)
        assertEquals("00", response.status?.code)
        assertTrue(response.formatted.contains("[HDR1]"), response.formatted)
    }

    @Test
    fun `a header length wider than the header value still splits correctly`() {
        val framing = HsmFraming(
            HeaderFormat.TWO_BYTE_LENGTH,
            tcpLengthEnabled = true,
            messageHeader = "",
            messageHeaderLength = 6,
        )
        val parser = HsmVendorParsers.forVendor(HsmVendorType.THALES_PAYSHIELD)
        val sent = framing.copy(messageHeader = "HEADER")
        val response = parser.parseResponse(frame("ND00", sent), framing)

        assertEquals("ND", response.commandCode)
        assertEquals("00", response.status?.code)
    }

    @Test
    fun `non thales vendors never report a status`() {
        for (vendor in HsmVendorType.entries - HsmVendorType.THALES_PAYSHIELD) {
            assertNull(HsmVendorParsers.forVendor(vendor).statusOf("ND15"), vendor.name)
        }
    }

    // ── Paste-and-parse tab ───────────────────────────────────────────────

    @Test
    fun `pasted hex is accepted with any common separator style`() {
        val expected = byteArrayOf(0x00, 0x08, 0x4E, 0x43)
        for (text in listOf("0008 4E43", "00:08:4E:43", "00-08-4e-43", "0x00084E43", "0008\n4E43")) {
            val decoded = decodeInput(text, InputEncoding.AUTO).getOrThrow()
            assertContentEquals(expected, decoded, text)
        }
    }

    @Test
    fun `auto encoding falls back to ascii for text that is not hex`() {
        val decoded = decodeInput("NC", InputEncoding.AUTO).getOrThrow()
        assertContentEquals("NC".toByteArray(Charsets.US_ASCII), decoded)
    }

    @Test
    fun `a word that happens to be all hex digits is read as hex on auto`() {
        // "DECADE" is valid hex, so Auto reads it as bytes; forcing ASCII is the escape hatch.
        assertEquals(3, decodeInput("DECADE", InputEncoding.AUTO).getOrThrow().size)
        assertEquals(6, decodeInput("DECADE", InputEncoding.ASCII).getOrThrow().size)
    }

    @Test
    fun `forced hex reports why bad input failed`() {
        val odd = decodeInput("0008 4E4", InputEncoding.HEX).exceptionOrNull()
        assertTrue(odd!!.message!!.contains("odd number"), odd.message)

        val bad = decodeInput("00ZZ", InputEncoding.HEX).exceptionOrNull()
        assertTrue(bad!!.message!!.contains("not a hex digit"), bad.message)
    }

    @Test
    fun `direction is detected from thales command and response codes`() {
        val bytes = frame("A00001U", thalesFraming)
        assertEquals(
            HsmMessageDirection.REQUEST,
            detectDirection(HsmVendorType.THALES_PAYSHIELD, bytes, thalesFraming),
        )
        assertEquals(
            HsmMessageDirection.RESPONSE,
            detectDirection(HsmVendorType.THALES_PAYSHIELD, frame("A100", thalesFraming), thalesFraming),
        )
        // An unknown code is undecidable rather than guessed at.
        assertNull(detectDirection(HsmVendorType.THALES_PAYSHIELD, frame("ZZ00", thalesFraming), thalesFraming))
        // Other vendors have no disjoint code sets to match on.
        assertNull(detectDirection(HsmVendorType.FUTUREX, bytes, thalesFraming))
    }

    @Test
    fun `pasting a bare payload parses when framing is switched off`() {
        val bare = HsmFraming(HeaderFormat.NONE, tcpLengthEnabled = false, messageHeader = "", messageHeaderLength = 0)
        val parser = HsmVendorParsers.forVendor(HsmVendorType.THALES_PAYSHIELD)
        val bytes = decodeInput("4E4430 30", InputEncoding.AUTO).getOrThrow() // "ND00"

        val parsed = parser.parseResponse(bytes, bare)
        assertEquals("ND", parsed.commandCode)
        assertEquals("00", parsed.status?.code)
    }

    @Test
    fun `hex dump lays out offset hex and printable ascii`() {
        val dump = hexDump("ND00".toByteArray(Charsets.US_ASCII) + byteArrayOf(0x00))
        assertTrue(dump.startsWith("0000  4E 44 30 30 00"), dump)
        assertTrue(dump.endsWith("ND00."), dump)
    }
}
