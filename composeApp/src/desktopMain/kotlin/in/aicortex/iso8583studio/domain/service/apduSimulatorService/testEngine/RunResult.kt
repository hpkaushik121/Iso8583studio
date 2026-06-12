package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine

/**
 * Aggregate outcome of running a [TestPlan].
 */
data class RunResult(
    val plan: TestPlan,
    val cases: List<CaseResult>,
) {
    val passed: Int get() = cases.count { it.passed }
    val failed: Int get() = cases.count { !it.passed }
}

/**
 * Outcome of running a single [TestCase].
 *
 * @property exchanges Ordered (commandHex, responseHex) pairs captured during the run.
 */
data class CaseResult(
    val case: TestCase,
    val passed: Boolean,
    val failureMessage: String?,
    val durationMs: Long,
    val exchanges: List<Pair<String, String>>,
)
