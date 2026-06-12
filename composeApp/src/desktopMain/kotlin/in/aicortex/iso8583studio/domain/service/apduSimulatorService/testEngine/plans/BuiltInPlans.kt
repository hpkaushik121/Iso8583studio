package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine.plans

import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.Scheme
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine.Step
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine.TestCase
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine.TestPlan

/**
 * Minimal smoke-level EMV plans bundled with the app. These are NOT L3 certification scripts —
 * they are just the canonical happy-path APDU sequence per scheme so the user can verify a card
 * (or an emulator) responds plausibly. Real scheme test plans live as separate JSON files loaded
 * by the runner.
 */
object BuiltInPlans {

    fun all(): List<TestPlan> = listOf(visaBasic(), mastercardBasic(), ppseDiscovery())

    fun byId(id: String): TestPlan? = all().firstOrNull { it.id == id }

    /** PPSE select + assert that 6F template is returned. Works against any EMV contactless profile. */
    fun ppseDiscovery(): TestPlan = TestPlan(
        id = "smoke-ppse",
        name = "PPSE discovery (any scheme)",
        scheme = Scheme.OTHER,
        cases = listOf(
            TestCase(
                id = "ppse-select",
                name = "SELECT 2PAY.SYS.DDF01 returns FCI",
                profileId = "*",
                steps = listOf(
                    // SELECT by name: 00 A4 04 00 0E "2PAY.SYS.DDF01" 00
                    Step.Send("00A404000E325041592E5359532E444446303100"),
                    Step.Expect(swHex = "9000", tlvAssertions = mapOf("6F" to "*")),
                ),
            ),
        ),
    )

    fun visaBasic(): TestPlan = TestPlan(
        id = "visa-smoke-basic",
        name = "Visa basic transaction (smoke)",
        scheme = Scheme.VISA,
        cases = listOf(
            TestCase(
                id = "select-aid",
                name = "SELECT Visa AID",
                profileId = "visa-credit-test",
                steps = listOf(
                    Step.Send("00A4040007A000000003101000"),
                    Step.Expect(swHex = "9000", tlvAssertions = mapOf("6F" to "*", "84" to "A0000000031010")),
                ),
            ),
            TestCase(
                id = "gpo",
                name = "GET PROCESSING OPTIONS returns AIP+AFL",
                profileId = "visa-credit-test",
                steps = listOf(
                    // Re-select AID so the case is independent
                    Step.Send("00A4040007A000000003101000"),
                    Step.Expect(swHex = "9000"),
                    // GPO with empty PDOL data (tag 83 length 0)
                    Step.Send("80A800000283 00".replace(" ", "")),
                    Step.Expect(swHex = "9000"),
                ),
            ),
            TestCase(
                id = "read-record-1-1",
                name = "READ RECORD SFI=1 record 1 returns tag 70",
                profileId = "visa-credit-test",
                steps = listOf(
                    Step.Send("00A4040007A000000003101000"),
                    Step.Expect(swHex = "9000"),
                    Step.Send("80A8000002 8300".replace(" ", "")),
                    Step.Expect(swHex = "9000"),
                    Step.Send("00B2010C00"),
                    Step.Expect(swHex = "9000", tlvAssertions = mapOf("70" to "*")),
                ),
            ),
            TestCase(
                id = "generate-ac-arqc",
                name = "GENERATE AC (ARQC) returns 9F26 cryptogram",
                profileId = "visa-credit-test",
                steps = listOf(
                    Step.Send("00A4040007A000000003101000"),
                    Step.Expect(swHex = "9000"),
                    Step.Send("80A8000002 8300".replace(" ", "")),
                    Step.Expect(swHex = "9000"),
                    // GENERATE AC (ARQC, P1=0x80) with 32 bytes of dummy CDOL1 data
                    Step.Send(
                        "80AE8000 20 0000000000000000000000000000000000000000000000000000000000000000 00"
                            .replace(" ", "")
                    ),
                    Step.Expect(swHex = "9000", tlvAssertions = mapOf("9F26" to "*", "9F36" to "*", "9F27" to "*")),
                ),
            ),
        ),
    )

    fun mastercardBasic(): TestPlan = TestPlan(
        id = "mc-smoke-basic",
        name = "MasterCard basic transaction (smoke)",
        scheme = Scheme.MASTERCARD,
        cases = listOf(
            TestCase(
                id = "select-aid",
                name = "SELECT MasterCard AID",
                profileId = "mc-debit-test",
                steps = listOf(
                    Step.Send("00A4040007A000000004101000"),
                    Step.Expect(swHex = "9000", tlvAssertions = mapOf("6F" to "*", "84" to "A0000000041010")),
                ),
            ),
            TestCase(
                id = "gpo-and-read",
                name = "GPO + READ RECORD",
                profileId = "mc-debit-test",
                steps = listOf(
                    Step.Send("00A4040007A000000004101000"),
                    Step.Expect(swHex = "9000"),
                    Step.Send("80A8000002 8300".replace(" ", "")),
                    Step.Expect(swHex = "9000"),
                    Step.Send("00B2010C00"),
                    Step.Expect(swHex = "9000", tlvAssertions = mapOf("70" to "*")),
                ),
            ),
        ),
    )
}
