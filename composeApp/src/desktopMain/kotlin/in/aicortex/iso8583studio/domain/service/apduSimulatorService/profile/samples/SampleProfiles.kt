package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.samples

import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.AppRecord
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.CardApplication
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.CardProfile
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.IssuerKey
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.KeyKind
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.Scheme

/**
 * Built-in sample profiles seeded into a fresh ProfileStore so users have something to play with
 * out of the box. Values are *test* values — well-known dummy PANs, no real keys.
 */
object SampleProfiles {

    fun visaCreditTest(): CardProfile {
        // Inner-tag-70 body: 5A (PAN), 5F24 (expiry), 5F20 (cardholder),
        // 57 (track 2 equivalent), 8C (CDOL1), 8D (CDOL2), 8E (CVM list), 9F08 (app version).
        val tlv = buildString {
            // 5A 08 4111111111111111
            append("5A084111111111111111")
            // 5F24 03 291231
            append("5F2403291231")
            // 5F20 09 "TEST/VISA" -> 54 45 53 54 2F 56 49 53 41
            append("5F2009544553542F56495341")
            // 57 13 4111111111111111 D2912201 1000000000 000F (track2 equivalent)
            append("57134111111111111111D29122011000000000000F")
            // 8C 21 - CDOL1 (33 bytes of tag-length entries)
            append("8C219F02069F03069F1A0295055F2A029A039C019F37049F35019F45029F4C089F3403")
            // 8D 0C - CDOL2
            append("8D0C910A8A0295059F37049F4C08")
            // 8E 0E - CVM list (no amounts, simple online-PIN/signature/no-cvm)
            append("8E0E0000000000000000420341031E03")
            // 9F08 02 008C - application version number
            append("9F0802008C")
        }

        val app = CardApplication(
            aid = "A0000000031010",
            label = "VISA CREDIT",
            priority = 1,
            aip = "1980",
            afl = "08010100",
            records = listOf(AppRecord(sfi = 1, record = 1, tlvHex = tlv)),
            pdol = "9F66049F02069F03069F1A0295055F2A029A039C019F37049F4E14",
            cdol1 = "9F02069F03069F1A0295055F2A029A039C019F37049F35019F45029F4C089F3403",
            cdol2 = "9F02069F03069F1A0295055F2A029A039C019F37049F35019F45029F4C089F3403",
            pan = "4111111111111111",
            panSequenceNumber = 0,
            expiryYyMmDd = "291231",
            track2Equivalent = "4111111111111111D29122011000000000000F",
            cardholderName = "TEST/VISA",
            issuerKeyId = "visa-imk-1",
            cvn = 18,
            atcStart = 0,
            pinTryLimit = 3,
        )

        val key = IssuerKey(
            id = "visa-imk-1",
            kind = KeyKind.TDES_AC,
            imk = "0123456789ABCDEFFEDCBA9876543210",
            udk = "11111111111111112222222222222222",
        )

        return CardProfile(
            id = "visa-credit-test",
            name = "Visa Credit (Test)",
            scheme = Scheme.VISA,
            atr = "3B6500002063CB6800",
            applications = listOf(app),
            keys = listOf(key),
            notes = "Built-in sample. Test PAN 4111... — do not use for production.",
        )
    }

    fun mastercardDebitTest(): CardProfile {
        val cardholderHex = "DEBIT MASTERCARD".toByteArray(Charsets.US_ASCII)
            .joinToString("") { "%02X".format(it) }
        val cardholderTlv = "5F20" + "%02X".format(cardholderHex.length / 2) + cardholderHex

        val tlvFinal = buildString {
            append("5A085555555555554444")
            append("5F2403291231")
            append(cardholderTlv)
            // Track 2 equivalent: 5555555555554444 D 29122011000000000000 F
            append("57135555555555554444D29122011000000000000F")
            // 8C / 8D / 8E / 9F08 — same shape as Visa sample
            append("8C1B9F02069F03069F1A0295055F2A029A039C019F37049F35019F45029F4C089F3403")
            append("8D0C910A8A0295059F37049F4C08")
            append("8E0E0000000000000000420341031E03")
            append("9F08020002")
        }

        val app = CardApplication(
            aid = "A0000000041010",
            label = "DEBIT MASTERCARD",
            priority = 1,
            aip = "1980",
            afl = "08010100",
            records = listOf(AppRecord(sfi = 1, record = 1, tlvHex = tlvFinal)),
            pdol = "9F66049F02069F03069F1A0295055F2A029A039C019F37049F4E14",
            cdol1 = "9F02069F03069F1A0295055F2A029A039C019F37049F35019F45029F4C089F3403",
            cdol2 = "9F02069F03069F1A0295055F2A029A039C019F37049F35019F45029F4C089F3403",
            pan = "5555555555554444",
            panSequenceNumber = 0,
            expiryYyMmDd = "291231",
            track2Equivalent = "5555555555554444D29122011000000000000F",
            cardholderName = "DEBIT MASTERCARD",
            issuerKeyId = "mc-imk-1",
            cvn = 1,
            atcStart = 0,
            pinTryLimit = 3,
        )

        val key = IssuerKey(
            id = "mc-imk-1",
            kind = KeyKind.TDES_AC,
            imk = "0123456789ABCDEFFEDCBA9876543210",
            udk = "33333333333333334444444444444444",
        )

        return CardProfile(
            id = "mastercard-debit-test",
            name = "MasterCard Debit (Test)",
            scheme = Scheme.MASTERCARD,
            atr = "3B6500002063CB6800",
            applications = listOf(app),
            keys = listOf(key),
            notes = "Built-in sample. Test PAN 5555...4444 — do not use for production.",
        )
    }

    fun all(): List<CardProfile> = listOf(visaCreditTest(), mastercardDebitTest())
}
