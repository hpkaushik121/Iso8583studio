package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine.reporters

import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.Scheme
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine.RunResult

/**
 * Renders a [RunResult] in an Amex AEIPS-style CSV log: one line per APDU exchange
 * with `case_id,seq,command,sw,response_data,result`. Quoting follows RFC 4180.
 */
object AmexAeipsReporter {

    fun render(result: RunResult): String {
        val sb = StringBuilder()
        sb.append("case_id,seq,command,sw,response_data,result\n")

        if (result.plan.scheme != Scheme.AMEX) {
            return sb.toString()
        }

        for (case in result.cases) {
            val resultText = if (case.passed) "PASS" else "FAIL"
            for ((i, ex) in case.exchanges.withIndex()) {
                val command = ex.first
                val response = ex.second
                val sw: String
                val data: String
                if (response.length >= 4) {
                    sw = response.substring(response.length - 4)
                    data = response.substring(0, response.length - 4)
                } else {
                    sw = response
                    data = ""
                }
                sb.append(quote(case.case.id)).append(',')
                    .append(i + 1).append(',')
                    .append(quote(command)).append(',')
                    .append(quote(sw)).append(',')
                    .append(quote(data)).append(',')
                    .append(quote(resultText)).append('\n')
            }
        }
        return sb.toString()
    }

    private fun quote(field: String): String {
        val needsQuoting = field.contains(',') || field.contains('"') ||
                field.contains('\n') || field.contains('\r')
        if (!needsQuoting) return field
        val escaped = field.replace("\"", "\"\"")
        return "\"" + escaped + "\""
    }
}
