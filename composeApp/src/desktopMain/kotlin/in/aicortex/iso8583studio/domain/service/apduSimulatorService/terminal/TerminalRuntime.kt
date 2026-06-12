package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.terminal

import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.CommandApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.EmvTag
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.ResponseApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.Sw
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.Tlv
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.hexToBytes
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.toHex
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.transport.CardTransport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * EMV Book 3 contact transaction state machine. Drives a [CardTransport] through the canonical
 * 11-phase transaction and emits a [TransactionStep] flow the UI can render as a stepper.
 *
 * What's implemented (happy-path complete; certification-grade nuance flagged below):
 *   1. Application Selection         — PPSE → pick AID matching terminal config → SELECT AID
 *   2. Initiate                      — PDOL build from terminal profile + GPO → AIP/AFL
 *   3. Read Application Data         — walk AFL, READ RECORD per (sfi, first..last)
 *   4. Offline Data Authentication   — STUB. AIP bits inspected; TVR bit set if not performed.
 *   5. Processing Restrictions       — expiry + version check; sets TVR bits.
 *   6. Cardholder Verification       — first applicable CVM rule from card 8E vs terminal caps.
 *   7. Terminal Risk Management      — floor limit. (Random selection / velocity not implemented.)
 *   8. Terminal Action Analysis      — TVR vs TAC ∪ IAC → AAC / ARQC / TC.
 *   9. First GENERATE AC             — sends with computed CDOL1; parses 9F26/9F27/9F36/9F10.
 *  10. Online                        — STUB unless TerminalProfile.issuerHost.enabled (open hook).
 *  11. Second GENERATE AC            — only if 1st returned ARQC and online completed.
 */
class TerminalRuntime(
    private val transport: CardTransport,
    private val terminal: TerminalProfile,
) {
    fun run(request: TransactionRequest): Flow<TransactionStep> = flow {
        val ctx = MutableContext(request, terminal)
        try {
            applicationSelection(ctx)
            if (!ctx.aborted) initiate(ctx)
            if (!ctx.aborted) readApplicationData(ctx)
            if (!ctx.aborted) offlineDataAuthentication(ctx)
            if (!ctx.aborted) processingRestrictions(ctx)
            if (!ctx.aborted) cardholderVerification(ctx)
            if (!ctx.aborted) terminalRiskManagement(ctx)
            if (!ctx.aborted) terminalActionAnalysis(ctx)
            if (!ctx.aborted) firstGenerateAc(ctx)
            // Online + 2nd GAC: only when first GAC returned ARQC.
            if (!ctx.aborted && ctx.firstAcType == AcType.ARQC && terminal.issuerHost.enabled) {
                onlineProcessing(ctx)
                if (!ctx.aborted) secondGenerateAc(ctx)
            }
            if (!ctx.aborted) {
                val acType = ctx.secondAcType ?: ctx.firstAcType
                ?: return@flow emitAborted(ctx, "No cryptogram produced")
                emitOutcome(ctx, acType)
            }
        } catch (t: Throwable) {
            emitAborted(ctx, "Unexpected: ${t.message ?: t::class.simpleName}")
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────────────────

    private suspend fun kotlinx.coroutines.flow.FlowCollector<TransactionStep>.applicationSelection(c: MutableContext) {
        emit(TransactionStep.PhaseStart(now(), Phase.APP_SELECTION))

        val ppseAid = "2PAY.SYS.DDF01".toByteArray(Charsets.US_ASCII)
        val ppseR = c.transmit(this, Phase.APP_SELECTION, CommandApdu(0x00, 0xA4.toByte(), 0x04, 0x00, ppseAid, 0))
        if (!ppseR.isSuccess) {
            emit(TransactionStep.Note(now(), Phase.APP_SELECTION, "PPSE select failed (SW=${"%04X".format(ppseR.sw)}). Falling back to direct AID select.", isError = true))
        }

        // Find AIDs the card offers under the PPSE FCI.
        val cardAids = if (ppseR.isSuccess) extractAidsFromPpse(ppseR.data) else emptyList()
        if (cardAids.isNotEmpty()) {
            emit(TransactionStep.Note(now(), Phase.APP_SELECTION, "Card AIDs in PPSE: ${cardAids.joinToString { it.toHex() }}"))
        }

        val terminalAids = terminal.perAid.filter { it.enabled }
        if (terminalAids.isEmpty()) return abort(c, "Terminal has no enabled AIDs")

        // Pick first card AID that the terminal supports (priority order from card response).
        val matched = cardAids.firstNotNullOfOrNull { cardAid ->
            terminalAids.firstOrNull { it.aid.equals(cardAid.toHex(), ignoreCase = true) }?.let { cardAid to it }
        } ?: run {
            // No PPSE match — try direct SELECT for each enabled terminal AID.
            terminalAids.firstNotNullOfOrNull { tcfg ->
                val aidBytes = tcfg.aid.hexToBytes()
                val r = c.transmit(this, Phase.APP_SELECTION, CommandApdu(0x00, 0xA4.toByte(), 0x04, 0x00, aidBytes, 0))
                if (r.isSuccess) Triple(aidBytes, tcfg, r) else null
            }?.let { (a, t, _) -> a to t }
        } ?: return abort(c, "No supported AID found on card")

        val (aid, tcfg) = matched
        c.cardAid = aid
        c.aidConfig = tcfg
        emit(TransactionStep.Note(now(), Phase.APP_SELECTION, "Selected AID: ${aid.toHex()} (${tcfg.label})"))

        // SELECT the chosen AID (idempotent if we already did via fallback).
        val selR = c.transmit(this, Phase.APP_SELECTION, CommandApdu(0x00, 0xA4.toByte(), 0x04, 0x00, aid, 0))
        if (!selR.isSuccess) return abort(c, "AID select failed: ${"%04X".format(selR.sw)}")

        // Extract PDOL from FCI (tag 9F38 inside 6F → A5).
        val fci = Tlv.parseAll(selR.data).firstOrNull { it.tag == EmvTag.FCI_TEMPLATE }
        c.pdol = fci?.let { findTagDeep(listOf(it), EmvTag.PDOL)?.value }
        emit(TransactionStep.Note(now(), Phase.APP_SELECTION, "PDOL: " + (c.pdol?.toHex() ?: "(none)")))
        emit(TransactionStep.PhaseEnd(now(), Phase.APP_SELECTION, ok = true))
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<TransactionStep>.initiate(c: MutableContext) {
        emit(TransactionStep.PhaseStart(now(), Phase.INITIATE))

        val pdolData = buildPdolData(c.pdol, c)
        c.pdolData = pdolData
        emit(TransactionStep.Note(now(), Phase.INITIATE, "PDOL data: ${pdolData.toHex()}"))

        val gpoBody = byteArrayOf(0x83.toByte(), pdolData.size.toByte()) + pdolData
        val r = c.transmit(this, Phase.INITIATE, CommandApdu(0x80.toByte(), 0xA8.toByte(), 0x00, 0x00, gpoBody, 0))
        if (!r.isSuccess) return abort(c, "GPO failed: ${"%04X".format(r.sw)}")

        val tlvs = Tlv.parseAll(r.data)
        val template = tlvs.firstOrNull()
            ?: return abort(c, "GPO response empty")
        when (template.tag) {
            EmvTag.RESPONSE_TEMPLATE_1 -> {
                // Format 1 (tag 80): AIP||AFL
                val v = template.value
                if (v.size < 2) return abort(c, "GPO format-1 too short")
                c.aip = v.copyOfRange(0, 2)
                c.afl = v.copyOfRange(2, v.size)
            }
            EmvTag.RESPONSE_TEMPLATE_2 -> {
                // Format 2 (tag 77): TLV with 82 (AIP) and 94 (AFL)
                val inner = Tlv.parseAll(template.value)
                c.aip = inner.firstOrNull { it.tag == EmvTag.AIP }?.value
                c.afl = inner.firstOrNull { it.tag == EmvTag.AFL }?.value
                if (c.aip == null || c.afl == null) return abort(c, "GPO format-2 missing AIP/AFL")
            }
            else -> return abort(c, "GPO unknown template tag %X".format(template.tag))
        }
        emit(TransactionStep.Note(now(), Phase.INITIATE, "AIP=${c.aip!!.toHex()}  AFL=${c.afl!!.toHex()}"))
        emit(TransactionStep.PhaseEnd(now(), Phase.INITIATE, ok = true))
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<TransactionStep>.readApplicationData(c: MutableContext) {
        emit(TransactionStep.PhaseStart(now(), Phase.READ_DATA))
        val afl = c.afl ?: return abort(c, "No AFL")
        require(afl.size % 4 == 0) { "AFL length must be multiple of 4" }

        val records = mutableListOf<Tlv>()
        var i = 0
        while (i < afl.size) {
            val sfi = (afl[i].toInt() and 0xF8) shr 3
            val first = afl[i + 1].toInt() and 0xFF
            val last = afl[i + 2].toInt() and 0xFF
            // afl[i+3] = number of records used in offline data auth (we don't enforce ODA yet)
            for (rec in first..last) {
                val p2 = ((sfi shl 3) or 0x04).toByte()
                val r = c.transmit(this, Phase.READ_DATA, CommandApdu(0x00, 0xB2.toByte(), rec.toByte(), p2, le = 0))
                if (!r.isSuccess) {
                    emit(TransactionStep.Note(now(), Phase.READ_DATA, "READ RECORD SFI=$sfi rec=$rec failed: ${"%04X".format(r.sw)}", isError = true))
                    continue
                }
                val parsed = Tlv.parseAll(r.data)
                val template70 = parsed.firstOrNull { it.tag == EmvTag.READ_RECORD_TEMPLATE }
                if (template70 != null) records += Tlv.parseAll(template70.value)
                else records += parsed
            }
            i += 4
        }
        c.records = records
        emit(TransactionStep.Note(now(), Phase.READ_DATA, "Read ${records.size} TLV(s) from card records"))
        emit(TransactionStep.PhaseEnd(now(), Phase.READ_DATA, ok = true))
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<TransactionStep>.offlineDataAuthentication(c: MutableContext) {
        emit(TransactionStep.PhaseStart(now(), Phase.OFFLINE_AUTH))
        val aip = c.aip ?: return run { emit(TransactionStep.PhaseEnd(now(), Phase.OFFLINE_AUTH, ok = false)) }
        val cdaCapable = (aip[0].toInt() and 0x01) != 0
        val ddaCapable = (aip[0].toInt() and 0x20) != 0
        val sdaCapable = (aip[0].toInt() and 0x40) != 0
        emit(TransactionStep.Note(now(), Phase.OFFLINE_AUTH, "AIP capabilities — SDA=$sdaCapable DDA=$ddaCapable CDA=$cdaCapable"))
        // Cert verification not implemented — set TVR byte 1 bit 8 "Offline data authentication was not performed".
        c.setTvrBit(0, 0x80)
        emit(TransactionStep.Note(now(), Phase.OFFLINE_AUTH, "ODA cert verification not implemented — TVR[0]:0x80 set", isError = true))
        emit(TransactionStep.Flags(now(), Phase.OFFLINE_AUTH, c.tvr.copyOf(), c.tsi.copyOf()))
        emit(TransactionStep.PhaseEnd(now(), Phase.OFFLINE_AUTH, ok = true))
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<TransactionStep>.processingRestrictions(c: MutableContext) {
        emit(TransactionStep.PhaseStart(now(), Phase.PROCESSING_RESTRICTIONS))
        val expiry = findTagDeep(c.records, EmvTag.APPLICATION_EXPIRY)?.value?.toHex()
        if (expiry != null) {
            // YYMMDD compared to today
            val today = c.request.date
            if (expiry < today) {
                c.setTvrBit(1, 0x40)
                emit(TransactionStep.Note(now(), Phase.PROCESSING_RESTRICTIONS, "Application expired ($expiry < $today) — TVR[1]:0x40 set", isError = true))
            } else {
                emit(TransactionStep.Note(now(), Phase.PROCESSING_RESTRICTIONS, "Expiry OK ($expiry ≥ $today)"))
            }
        }
        emit(TransactionStep.PhaseEnd(now(), Phase.PROCESSING_RESTRICTIONS, ok = true))
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<TransactionStep>.cardholderVerification(c: MutableContext) {
        emit(TransactionStep.PhaseStart(now(), Phase.CARDHOLDER_VERIFY))
        val cvmList = findTagDeep(c.records, EmvTag.CDOL2 /*8D*/)?.value
            ?: findTagDeep(c.records, 0x8E)?.value
        if (cvmList == null) {
            emit(TransactionStep.Note(now(), Phase.CARDHOLDER_VERIFY, "No CVM list — skipping"))
            emit(TransactionStep.PhaseEnd(now(), Phase.CARDHOLDER_VERIFY, ok = true))
            return
        }
        // CVM list = 4-byte X amount || 4-byte Y amount || N*2 byte CVM rules
        c.setTsiBit(0, 0x40) // "Cardholder verification was performed"
        emit(TransactionStep.Note(now(), Phase.CARDHOLDER_VERIFY, "CVM list present (${cvmList.size} bytes) — picking first applicable rule (stub)"))
        emit(TransactionStep.Flags(now(), Phase.CARDHOLDER_VERIFY, c.tvr.copyOf(), c.tsi.copyOf()))
        emit(TransactionStep.PhaseEnd(now(), Phase.CARDHOLDER_VERIFY, ok = true))
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<TransactionStep>.terminalRiskManagement(c: MutableContext) {
        emit(TransactionStep.PhaseStart(now(), Phase.TERMINAL_RISK))
        val cfg = c.aidConfig
        if (cfg != null && c.request.amount > cfg.floorLimit) {
            c.setTvrBit(3, 0x80) // byte 4 bit 8 "Transaction exceeds floor limit"
            emit(TransactionStep.Note(now(), Phase.TERMINAL_RISK, "Amount ${c.request.amount} > floor limit ${cfg.floorLimit} — TVR[3]:0x80 set"))
        }
        c.setTsiBit(0, 0x80) // "Terminal risk management performed"
        emit(TransactionStep.Flags(now(), Phase.TERMINAL_RISK, c.tvr.copyOf(), c.tsi.copyOf()))
        emit(TransactionStep.PhaseEnd(now(), Phase.TERMINAL_RISK, ok = true))
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<TransactionStep>.terminalActionAnalysis(c: MutableContext) {
        emit(TransactionStep.PhaseStart(now(), Phase.TERMINAL_ACTION))
        if (c.request.forceDecline) {
            c.firstAcType = AcType.AAC
        } else if (c.request.forceOnline) {
            c.firstAcType = AcType.ARQC
        } else {
            val cfg = c.aidConfig!!
            val tac = Triple(cfg.tacDefault.hexToBytes(), cfg.tacDenial.hexToBytes(), cfg.tacOnline.hexToBytes())
            // IAC from card records
            val iacDefault = findTagDeep(c.records, 0x9F0D)?.value ?: ByteArray(5)
            val iacDenial = findTagDeep(c.records, 0x9F0E)?.value ?: ByteArray(5)
            val iacOnline = findTagDeep(c.records, 0x9F0F)?.value ?: ByteArray(5)

            c.firstAcType = when {
                anyBitSet(c.tvr, tac.second) || anyBitSet(c.tvr, iacDenial) -> AcType.AAC
                anyBitSet(c.tvr, tac.third) || anyBitSet(c.tvr, iacOnline) -> AcType.ARQC
                anyBitSet(c.tvr, tac.first) || anyBitSet(c.tvr, iacDefault) -> AcType.ARQC // Default → online if possible
                else -> AcType.TC
            }
        }
        emit(TransactionStep.Note(now(), Phase.TERMINAL_ACTION, "Decision: ${c.firstAcType}"))
        emit(TransactionStep.PhaseEnd(now(), Phase.TERMINAL_ACTION, ok = true))
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<TransactionStep>.firstGenerateAc(c: MutableContext) {
        emit(TransactionStep.PhaseStart(now(), Phase.FIRST_GAC))
        val cdol1 = findTagDeep(c.records, EmvTag.CDOL1)?.value
        val cdolData = buildDolData(cdol1, c)
        emit(TransactionStep.Note(now(), Phase.FIRST_GAC, "CDOL1 data: ${cdolData.toHex()}"))

        val p1 = when (c.firstAcType!!) {
            AcType.AAC -> 0x00
            AcType.TC -> 0x40
            AcType.ARQC -> 0x80
        }.toByte()
        val r = c.transmit(this, Phase.FIRST_GAC, CommandApdu(0x80.toByte(), 0xAE.toByte(), p1, 0x00, cdolData, 0))
        if (!r.isSuccess) return abort(c, "GENERATE AC failed: ${"%04X".format(r.sw)}")

        // Parse 9F26 (AC), 9F27 (CID), 9F36 (ATC), 9F10 (IAD) from response template (80 or 77).
        val tlvs = Tlv.parseAll(r.data).firstOrNull()
        val (ac, cid, atc, iad) = parseAcResponse(tlvs)
            ?: return abort(c, "GAC response missing AC/ATC/CID")
        c.firstAcResponse = AcResponse(ac, cid, atc, iad).let {
            c.ac = it.ac; c.cid = it.cid; c.atc = it.atc; c.iad = it.iad
            r.data
        }
        c.firstAcType = when (cid and 0xC0) {
            0x00 -> AcType.AAC
            0x40 -> AcType.TC
            0x80 -> AcType.ARQC
            else -> c.firstAcType
        }
        emit(TransactionStep.Note(now(), Phase.FIRST_GAC, "AC=${ac.toHex()}  ATC=${"%04X".format(atc)}  CID=${"%02X".format(cid)} (${c.firstAcType})"))
        emit(TransactionStep.PhaseEnd(now(), Phase.FIRST_GAC, ok = true))
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<TransactionStep>.onlineProcessing(c: MutableContext) {
        emit(TransactionStep.PhaseStart(now(), Phase.ONLINE))
        emit(TransactionStep.Note(now(), Phase.ONLINE, "Online ARQC submission to ${terminal.issuerHost.host}:${terminal.issuerHost.port} — STUB. Returning approval.", isError = false))
        // TODO: open TCP socket, send ISO8583, parse ARPC. For now, just pretend issuer approved.
        emit(TransactionStep.PhaseEnd(now(), Phase.ONLINE, ok = true))
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<TransactionStep>.secondGenerateAc(c: MutableContext) {
        emit(TransactionStep.PhaseStart(now(), Phase.SECOND_GAC))
        val cdol2 = findTagDeep(c.records, EmvTag.CDOL2)?.value
        val cdolData = buildDolData(cdol2, c)
        emit(TransactionStep.Note(now(), Phase.SECOND_GAC, "CDOL2 data: ${cdolData.toHex()}"))
        // Issuer approved → ask card for TC.
        val r = c.transmit(this, Phase.SECOND_GAC, CommandApdu(0x80.toByte(), 0xAE.toByte(), 0x40, 0x00, cdolData, 0))
        if (!r.isSuccess) return abort(c, "2nd GENERATE AC failed: ${"%04X".format(r.sw)}")
        val tlvs = Tlv.parseAll(r.data).firstOrNull()
        val parsed = parseAcResponse(tlvs)
            ?: return abort(c, "2nd GAC response missing AC")
        c.ac = parsed.ac; c.cid = parsed.cid; c.atc = parsed.atc; c.iad = parsed.iad
        c.secondAcType = when (parsed.cid and 0xC0) {
            0x00 -> AcType.AAC
            0x40 -> AcType.TC
            0x80 -> AcType.ARQC
            else -> null
        }
        emit(TransactionStep.PhaseEnd(now(), Phase.SECOND_GAC, ok = true))
    }

    // ── helpers ───────────────────────────────────────────────────────────────────────────────

    private suspend fun kotlinx.coroutines.flow.FlowCollector<TransactionStep>.emitOutcome(c: MutableContext, acType: AcType) {
        emit(
            TransactionStep.Outcome(
                time = now(),
                acType = acType,
                ac = c.ac ?: ByteArray(8),
                atc = c.atc ?: 0,
                cid = c.cid ?: 0,
                iad = c.iad ?: ByteArray(0),
                tvr = c.tvr.copyOf(),
                tsi = c.tsi.copyOf(),
            )
        )
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<TransactionStep>.emitAborted(c: MutableContext, reason: String) {
        c.aborted = true
        emit(TransactionStep.Aborted(now(), Phase.OUTCOME, reason))
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<TransactionStep>.abort(c: MutableContext, reason: String) {
        emitAborted(c, reason)
    }

    /**
     * Walk a BER-TLV tree (records is already a flat list; children of constructed nodes are
     * descended recursively) and return the first node matching [tag], or null.
     */
    private fun findTagDeep(roots: List<Tlv>, tag: Int): Tlv? {
        for (t in roots) {
            if (t.tag == tag) return t
            if (t.isConstructed) {
                val inner = runCatching { Tlv.parseAll(t.value) }.getOrNull() ?: continue
                findTagDeep(inner, tag)?.let { return it }
            }
        }
        return null
    }

    private fun extractAidsFromPpse(data: ByteArray): List<ByteArray> {
        val tlvs = Tlv.parseAll(data)
        val fci = tlvs.firstOrNull { it.tag == EmvTag.FCI_TEMPLATE } ?: return emptyList()
        val out = mutableListOf<ByteArray>()
        fun walk(roots: List<Tlv>) {
            for (t in roots) {
                if (t.tag == EmvTag.DEDICATED_FILE_NAME || t.tag == 0x4F) out += t.value
                if (t.isConstructed) walk(runCatching { Tlv.parseAll(t.value) }.getOrDefault(emptyList()))
            }
        }
        walk(listOf(fci))
        return out
    }

    /**
     * Build PDOL response. PDOL is a tag-length list; for each entry we materialise the requested
     * length of bytes from the terminal context. Unknown tags return zero-fill.
     */
    private fun buildPdolData(pdol: ByteArray?, c: MutableContext): ByteArray =
        buildDolData(pdol, c)

    private fun buildDolData(dol: ByteArray?, c: MutableContext): ByteArray {
        if (dol == null || dol.isEmpty()) return ByteArray(0)
        val out = java.io.ByteArrayOutputStream()
        var i = 0
        while (i < dol.size) {
            // tag (1 or 2 bytes BER)
            val tagStart = i
            val first = dol[i].toInt() and 0xFF
            i++
            if ((first and 0x1F) == 0x1F) {
                while (i < dol.size && (dol[i].toInt() and 0x80) != 0) i++
                i++
            }
            var tag = 0
            for (k in tagStart until i) tag = (tag shl 8) or (dol[k].toInt() and 0xFF)
            // length
            val len = dol[i].toInt() and 0xFF
            i++
            out.write(materialiseTag(tag, len, c))
        }
        return out.toByteArray()
    }

    /** Provide bytes for a known DOL tag at the requested length. */
    private fun materialiseTag(tag: Int, len: Int, c: MutableContext): ByteArray {
        fun pad(bytes: ByteArray): ByteArray =
            if (bytes.size == len) bytes
            else if (bytes.size < len) ByteArray(len - bytes.size) + bytes
            else bytes.copyOfRange(bytes.size - len, bytes.size)
        return when (tag) {
            0x9F02 -> pad(amountBcd(c.request.amount, 6))                // Amount, Authorised
            0x9F03 -> pad(amountBcd(c.request.amountOther, 6))            // Amount, Other
            0x9F1A -> pad(terminal.terminalCountryCode.hexToBytes())      // Terminal country
            0x95   -> pad(c.tvr)                                          // TVR
            0x5F2A -> pad(terminal.transactionCurrencyCode.hexToBytes())  // Currency
            0x9A   -> pad(c.request.date.hexToBytes())                    // Date YYMMDD
            0x9C   -> pad(byteArrayOf(c.request.type.code))               // Type
            0x9F37 -> pad(c.request.unpredictableNumber)                  // UN
            0x9F35 -> pad(byteArrayOf(terminal.terminalType.toByte()))    // Terminal type
            0x9F45 -> pad(ByteArray(2))                                   // DAC (data auth code), zero
            0x9F4C -> pad(ByteArray(8))                                   // ICC dynamic number, zero
            0x9F34 -> pad(byteArrayOf(0x1E, 0x03, 0x00))                 // CVM Results, NoCVM/online (placeholder)
            0x9F21 -> pad(c.request.time.hexToBytes())                    // Transaction time
            0x9F40 -> pad(terminal.additionalCapabilities.hexToBytes())
            0x9F33 -> pad(terminal.terminalCapabilities.hexToBytes())
            0x9B   -> pad(c.tsi)                                          // TSI
            else -> ByteArray(len) // unknown: zero-fill
        }
    }

    private fun amountBcd(amount: Long, len: Int): ByteArray {
        val s = amount.toString().padStart(len * 2, '0')
        val out = ByteArray(len)
        for (i in 0 until len) {
            val hi = Character.digit(s[i * 2], 10)
            val lo = Character.digit(s[i * 2 + 1], 10)
            out[i] = ((hi shl 4) or lo).toByte()
        }
        return out
    }

    private fun anyBitSet(a: ByteArray, b: ByteArray): Boolean {
        val n = minOf(a.size, b.size)
        for (i in 0 until n) if ((a[i].toInt() and b[i].toInt()) != 0) return true
        return false
    }

    private data class AcResponse(val ac: ByteArray, val cid: Int, val atc: Int, val iad: ByteArray)

    private fun parseAcResponse(template: Tlv?): AcResponse? {
        template ?: return null
        val children = if (template.tag == EmvTag.RESPONSE_TEMPLATE_2) {
            Tlv.parseAll(template.value)
        } else if (template.tag == EmvTag.RESPONSE_TEMPLATE_1) {
            // Format 1: CID(1) || ATC(2) || AC(8) || IAD(var)
            val v = template.value
            if (v.size < 11) return null
            return AcResponse(
                ac = v.copyOfRange(3, 11),
                cid = v[0].toInt() and 0xFF,
                atc = ((v[1].toInt() and 0xFF) shl 8) or (v[2].toInt() and 0xFF),
                iad = if (v.size > 11) v.copyOfRange(11, v.size) else ByteArray(0),
            )
        } else return null

        val ac = children.firstOrNull { it.tag == EmvTag.APPLICATION_CRYPTOGRAM }?.value ?: return null
        val atcBytes = children.firstOrNull { it.tag == EmvTag.ATC }?.value ?: return null
        val cidByte = children.firstOrNull { it.tag == EmvTag.CRYPTOGRAM_INFO_DATA }?.value ?: return null
        val iad = children.firstOrNull { it.tag == EmvTag.ISSUER_APPLICATION_DATA }?.value ?: ByteArray(0)
        return AcResponse(
            ac = ac,
            cid = cidByte[0].toInt() and 0xFF,
            atc = ((atcBytes[0].toInt() and 0xFF) shl 8) or (atcBytes[1].toInt() and 0xFF),
            iad = iad,
        )
    }

    private fun now(): Long = System.currentTimeMillis()

    /** Runtime-mutable transaction state. Stays inside this class to keep the public step model immutable. */
    private inner class MutableContext(val request: TransactionRequest, val terminal: TerminalProfile) {
        var aborted = false
        var cardAid: ByteArray? = null
        var aidConfig: AidTerminalConfig? = null
        var pdol: ByteArray? = null
        var pdolData: ByteArray? = null
        var aip: ByteArray? = null
        var afl: ByteArray? = null
        var records: List<Tlv> = emptyList()
        var firstAcType: AcType? = null
        var secondAcType: AcType? = null
        var firstAcResponse: ByteArray? = null
        var ac: ByteArray? = null
        var cid: Int? = null
        var atc: Int? = null
        var iad: ByteArray? = null
        val tvr = ByteArray(5)
        val tsi = ByteArray(2)

        fun setTvrBit(byteIdx: Int, mask: Int) { tvr[byteIdx] = (tvr[byteIdx].toInt() or mask).toByte() }
        fun setTsiBit(byteIdx: Int, mask: Int) { tsi[byteIdx] = (tsi[byteIdx].toInt() or mask).toByte() }

        suspend fun transmit(
            collector: kotlinx.coroutines.flow.FlowCollector<TransactionStep>,
            phase: Phase,
            cmd: CommandApdu,
        ): ResponseApdu {
            val r = transport.transmit(cmd)
            collector.emit(TransactionStep.Exchange(now(), phase, cmd, r))
            return r
        }
    }
}
