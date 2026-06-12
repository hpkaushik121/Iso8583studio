package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine.reporters

import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.Scheme
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine.RunResult

/**
 * Dispatches a [RunResult] to the appropriate scheme-specific reporter, falling back
 * to [JUnitXmlReporter] for schemes without a dedicated certification log format.
 */
object SchemeReporter {
    fun render(result: RunResult): String = when (result.plan.scheme) {
        Scheme.VISA -> VisaAdvtReporter.render(result)
        Scheme.MASTERCARD -> McMtipReporter.render(result)
        Scheme.AMEX -> AmexAeipsReporter.render(result)
        else -> JUnitXmlReporter.render(result)
    }
}
