package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine.reporters

import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine.CaseResult
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine.RunResult

/**
 * Renders a [RunResult] as a JUnit-compatible `<testsuite>` XML document so CI systems
 * (GitHub Actions, Jenkins, GitLab) can surface L3 test outcomes alongside unit tests.
 */
object JUnitXmlReporter {

    fun render(result: RunResult): String {
        val totalSeconds = result.cases.sumOf { it.durationMs } / 1000.0
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<testsuite name=\"")
            .append(escape(result.plan.name))
            .append("\" tests=\"").append(result.cases.size)
            .append("\" failures=\"").append(result.failed)
            .append("\" time=\"").append(formatSeconds(totalSeconds))
            .append("\">\n")

        for (case in result.cases) {
            renderCase(sb, result.plan.id, case)
        }

        sb.append("</testsuite>\n")
        return sb.toString()
    }

    private fun renderCase(sb: StringBuilder, planId: String, case: CaseResult) {
        val seconds = case.durationMs / 1000.0
        sb.append("  <testcase classname=\"")
            .append(escape(planId))
            .append("\" name=\"")
            .append(escape(case.case.id))
            .append("\" time=\"").append(formatSeconds(seconds))
            .append("\"")
        if (case.passed) {
            sb.append("/>\n")
        } else {
            sb.append(">\n")
            sb.append("    <failure message=\"")
                .append(escape(case.failureMessage ?: "failed"))
                .append("\">")
            sb.append(escape(buildExchangeLog(case)))
            sb.append("</failure>\n")
            sb.append("  </testcase>\n")
        }
    }

    private fun buildExchangeLog(case: CaseResult): String = buildString {
        append("[exchange log]\n")
        for ((i, ex) in case.exchanges.withIndex()) {
            append(i + 1).append(". > ").append(ex.first).append('\n')
            append("   < ").append(ex.second).append('\n')
        }
    }

    private fun formatSeconds(s: Double): String = "%.3f".format(s)

    private fun escape(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text) {
            when (c) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }
}
