package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine.reporters

import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.Scheme
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine.CaseResult
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine.RunResult
import java.time.Instant

/**
 * Renders a [RunResult] as a MasterCard MTIP-style XML report (M/Chip Test Interface Protocol).
 */
object McMtipReporter {

    fun render(result: RunResult): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<MtipReport plan=\"").append(escape(result.plan.id))
            .append("\" scheme=\"MASTERCARD\" generated=\"").append(escape(Instant.now().toString()))
            .append("\">\n")
        sb.append("  <Summary total=\"").append(result.cases.size)
            .append("\" passed=\"").append(result.passed)
            .append("\" failed=\"").append(result.failed)
            .append("\"/>\n")

        if (result.plan.scheme == Scheme.MASTERCARD) {
            for (case in result.cases) {
                renderCase(sb, case)
            }
        }
        sb.append("</MtipReport>\n")
        return sb.toString()
    }

    private fun renderCase(sb: StringBuilder, case: CaseResult) {
        sb.append("  <TestCase id=\"").append(escape(case.case.id))
            .append("\" name=\"").append(escape(case.case.name))
            .append("\" result=\"").append(if (case.passed) "PASS" else "FAIL")
            .append("\" durationMs=\"").append(case.durationMs)
            .append("\">\n")
        if (!case.passed) {
            sb.append("    <Failure><![CDATA[").append(sanitizeCdata(case.failureMessage ?: "failed"))
                .append("]]></Failure>\n")
        }
        sb.append("    <Trace>\n")
        for ((i, ex) in case.exchanges.withIndex()) {
            sb.append("      <Exchange seq=\"").append(i + 1).append("\">\n")
            sb.append("        <Command>").append(escape(ex.first)).append("</Command>\n")
            sb.append("        <Response>").append(escape(ex.second)).append("</Response>\n")
            sb.append("      </Exchange>\n")
        }
        sb.append("    </Trace>\n")
        sb.append("  </TestCase>\n")
    }

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

    /** Prevent breaking out of the CDATA block by neutralising any embedded "]]>". */
    private fun sanitizeCdata(text: String): String = text.replace("]]>", "]]]]><![CDATA[>")
}
