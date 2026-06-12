package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine.reporters

import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.Scheme
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine.CaseResult
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine.RunResult
import java.time.Instant

/**
 * Renders a [RunResult] in a Visa ADVT-style chip card L3 certification log layout:
 * a fixed header followed by per-test-case blocks containing the APDU exchange transcript.
 */
object VisaAdvtReporter {

    fun render(result: RunResult): String {
        val sb = StringBuilder()
        sb.append("==============================================\n")
        sb.append("VISA ADVT TEST LOG\n")
        sb.append("Plan: ").append(result.plan.id).append(" — ").append(result.plan.name).append('\n')
        sb.append("Scheme: VISA\n")
        sb.append("Generated: ").append(Instant.now().toString()).append('\n')
        sb.append("Tests: ").append(result.cases.size)
            .append("  Passed: ").append(result.passed)
            .append("  Failed: ").append(result.failed).append('\n')
        sb.append("==============================================\n")

        if (result.plan.scheme == Scheme.VISA) {
            for (case in result.cases) {
                sb.append('\n')
                renderCase(sb, case)
            }
        }
        return sb.toString()
    }

    private fun renderCase(sb: StringBuilder, case: CaseResult) {
        sb.append("[TEST ").append(case.case.id).append("] ").append(case.case.name).append('\n')
        sb.append("Result: ").append(if (case.passed) "PASS" else "FAIL").append('\n')
        sb.append("Duration: ").append(case.durationMs).append(" ms\n")
        if (!case.passed) {
            sb.append("Failure: ").append(case.failureMessage ?: "failed").append('\n')
        }
        sb.append("--- APDU Trace ---\n")
        for ((i, ex) in case.exchanges.withIndex()) {
            val seq = "%03d".format(i + 1)
            sb.append('#').append(seq).append(" > ").append(ex.first).append('\n')
            sb.append("     < ").append(ex.second).append('\n')
        }
        sb.append("----------------------------------------------\n")
    }
}
