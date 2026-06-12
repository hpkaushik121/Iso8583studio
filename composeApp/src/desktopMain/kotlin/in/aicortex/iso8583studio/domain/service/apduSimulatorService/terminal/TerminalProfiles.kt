package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.terminal

/**
 * Sensible default terminal profiles to seed a new APDUSimulatorConfig with.
 */
object TerminalProfiles {

    fun attendedRetailIN(): TerminalProfile = TerminalProfile(
        id = "default-attended-in",
        name = "Attended retail (India)",
        terminalType = 0x22,
        terminalCapabilities = "E0F8C8",
        additionalCapabilities = "6000F0A001",
        terminalCountryCode = "0356",
        transactionCurrencyCode = "0356",
        transactionCurrencyExp = 2,
        ifdSerialNumber = "STUDIO01",
        merchantCategoryCode = "5411",
        merchantId = "ISO8583STUDIO  ",
        terminalIdentification = "TRM00001",
        perAid = listOf(
            visaAidConfig(),
            mastercardAidConfig(),
            rupayAidConfig(),
        ),
        capks = TestCapks.all(),
    )

    fun unattendedKioskUS(): TerminalProfile = attendedRetailIN().copy(
        id = "default-unattended-us",
        name = "Unattended kiosk (US)",
        terminalType = 0x21,
        terminalCountryCode = "0840",
        transactionCurrencyCode = "0840",
    )

    private fun visaAidConfig() = AidTerminalConfig(
        aid = "A0000000031010",
        label = "VISA CREDIT",
        tacDefault = "DC4000A800",
        tacDenial = "0010000000",
        tacOnline = "DC4004F800",
        floorLimit = 200000,        // 2000.00
        targetPercent = 50,
        threshold = 100000,
        cvmOnlinePin = true,
        cvmSignature = true,
        kernelId = 3,
    )

    private fun mastercardAidConfig() = AidTerminalConfig(
        aid = "A0000000041010",
        label = "MASTERCARD",
        tacDefault = "F45084A800",
        tacDenial = "0010000000",
        tacOnline = "F450849800",
        floorLimit = 200000,
        targetPercent = 50,
        threshold = 100000,
        cvmOnlinePin = true,
        cvmSignature = true,
        kernelId = 2,
    )

    private fun rupayAidConfig() = AidTerminalConfig(
        aid = "A0000005241010",
        label = "RUPAY",
        tacDefault = "DC4000A800",
        tacDenial = "0010000000",
        tacOnline = "DC4004F800",
        floorLimit = 200000,
        targetPercent = 50,
        threshold = 100000,
        cvmOnlinePin = true,
        cvmSignature = true,
        kernelId = 7,
    )
}

/**
 * Placeholder CA public keys (clearly synthetic — for development only). Real test CA keys are
 * published by the schemes. Replace via the Terminal Profile tab when running against a real
 * scheme test bench.
 */
object TestCapks {
    fun all(): List<Capk> = listOf(
        Capk(
            rid = "A000000003",
            index = 0x99,
            modulus = "00".repeat(176),
            exponent = "03",
            expiryYyMmDd = "311231",
            source = "Synthetic Visa test (replace before use)",
        ),
        Capk(
            rid = "A000000004",
            index = 0xF1,
            modulus = "00".repeat(176),
            exponent = "03",
            expiryYyMmDd = "311231",
            source = "Synthetic MasterCard test (replace before use)",
        ),
        Capk(
            rid = "A000000524",
            index = 0x01,
            modulus = "00".repeat(176),
            exponent = "03",
            expiryYyMmDd = "311231",
            source = "Synthetic RuPay test (replace before use)",
        ),
    )
}
