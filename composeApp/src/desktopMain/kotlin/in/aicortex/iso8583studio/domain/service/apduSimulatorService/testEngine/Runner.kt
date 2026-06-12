package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine

import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.CommandApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.ResponseApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.Tlv
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.hexToBytes
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.toHex
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.transport.CardTransport

/**
 * Walks a [TestPlan] against a [CardTransport]. Each case is independent: an exception or assertion
 * failure in one case does not stop the rest. Within a case, the first failing step short-circuits
 * the remaining steps for that case.
 */
class Runner(private val transport: CardTransport) {

    suspend fun run(plan: TestPlan): RunResult {
        val results = ArrayList<CaseResult>(plan.cases.size)
        for (case in plan.cases) {
            results += runCase(case)
        }
        return RunResult(plan, results)
    }

    private suspend fun runCase(case: TestCase): CaseResult {
        val started = System.currentTimeMillis()
        val exchanges = ArrayList<Pair<String, String>>()
        var lastResponse: ResponseApdu? = null
        var lastCommandHex: String? = null
        var failureMessage: String? = null

        try {
            stepLoop@ for ((index, step) in case.steps.withIndex()) {
                when (step) {
                    is Step.Send -> {
                        val raw = step.commandHex.hexToBytes()
                        val cmd = CommandApdu.parse(raw)
                        val resp = transport.transmit(cmd)
                        lastCommandHex = step.commandHex.uppercase()
                        lastResponse = resp
                        exchanges += lastCommandHex!! to resp.toBytes().toHex()
                    }
                    is Step.Expect -> {
                        val resp = lastResponse
                        if (resp == null) {
                            failureMessage = "step $index: expect with no prior send"
                            break@stepLoop
                        }
                        val detail = checkExpect(step, resp)
                        if (detail != null) {
                            failureMessage = "step $index: $detail"
                            break@stepLoop
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            failureMessage = t.message ?: t::class.simpleName ?: "unknown error"
        }

        val duration = System.currentTimeMillis() - started
        return CaseResult(
            case = case,
            passed = failureMessage == null,
            failureMessage = failureMessage,
            durationMs = duration,
            exchanges = exchanges,
        )
    }

    /** Returns null on success or a human-readable detail on failure. */
    private fun checkExpect(expect: Step.Expect, resp: ResponseApdu): String? {
        val expectedSw = parseSw(expect.swHex) ?: return "invalid swHex '${expect.swHex}'"
        if (resp.sw != expectedSw) {
            return "SW mismatch: expected %04X, got %04X".format(expectedSw, resp.sw)
        }

        val dataHex = resp.data.toHex()
        if (expect.dataContains != null) {
            val needle = expect.dataContains.filterNot { it.isWhitespace() }.uppercase()
            if (!dataHex.contains(needle)) {
                return "data does not contain '$needle' (data=$dataHex)"
            }
        }

        if (expect.tlvAssertions.isNotEmpty()) {
            val tlvs = try {
                Tlv.parseAll(resp.data)
            } catch (t: Throwable) {
                return "TLV parse failed: ${t.message}"
            }
            for ((tagHex, expectedValue) in expect.tlvAssertions) {
                val tag = parseTag(tagHex) ?: return "invalid tlv tag '$tagHex'"
                val found = findTagDeep(tlvs, tag)
                    ?: return "tlv tag $tagHex not found"
                if (expectedValue != "*") {
                    val expectedHex = expectedValue.filterNot { it.isWhitespace() }.uppercase()
                    val actualHex = found.value.toHex()
                    if (actualHex != expectedHex) {
                        return "tlv $tagHex value mismatch: expected $expectedHex, got $actualHex"
                    }
                }
            }
        }

        return null
    }

    private fun parseSw(swHex: String): Int? {
        val clean = swHex.filterNot { it.isWhitespace() }
        if (clean.length != 4) return null
        return try {
            clean.toInt(16)
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun parseTag(tagHex: String): Int? {
        val clean = tagHex.filterNot { it.isWhitespace() }
        if (clean.isEmpty() || clean.length % 2 != 0) return null
        return try {
            clean.toLong(16).toInt()
        } catch (_: NumberFormatException) {
            null
        }
    }

    /** Search constructed templates recursively for the requested tag. */
    private fun findTagDeep(tlvs: List<Tlv>, tag: Int): Tlv? {
        for (t in tlvs) {
            if (t.tag == tag) return t
            if (t.isConstructed) {
                val nested = try {
                    Tlv.parseAll(t.value)
                } catch (_: Throwable) {
                    emptyList()
                }
                val hit = findTagDeep(nested, tag)
                if (hit != null) return hit
            }
        }
        return null
    }
}
