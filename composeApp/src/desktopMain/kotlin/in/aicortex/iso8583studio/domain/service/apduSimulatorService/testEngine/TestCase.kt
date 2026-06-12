package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One scripted L3-style test case: an ordered series of [Step]s sent against a card transport.
 */
@Serializable
data class TestCase(
    val id: String,
    val name: String,
    val profileId: String,
    val steps: List<Step>,
)

/**
 * A single step inside a [TestCase]. Polymorphic; serialised with a `type` discriminator
 * (`send` or `expect`).
 */
@Serializable
sealed class Step {
    /** Transmit a raw C-APDU (hex). The runner stores the resulting R-APDU as "most recent". */
    @Serializable
    @SerialName("send")
    data class Send(val commandHex: String) : Step()

    /**
     * Assert properties of the most recent response.
     *
     * - [swHex]: required SW, e.g. "9000".
     * - [dataContains]: optional hex substring that must appear in the response data.
     * - [tlvAssertions]: tag (hex) -> expected value (hex), or "*" for "tag must be present, any value".
     */
    @Serializable
    @SerialName("expect")
    data class Expect(
        val swHex: String,
        val dataContains: String? = null,
        val tlvAssertions: Map<String, String> = emptyMap(),
    ) : Step()
}
