package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine

import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.Scheme
import kotlinx.serialization.Serializable

/**
 * A collection of [TestCase]s scoped to a card scheme.
 */
@Serializable
data class TestPlan(
    val id: String,
    val name: String,
    val scheme: Scheme,
    val cases: List<TestCase>,
)
