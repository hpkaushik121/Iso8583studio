package io.cryptocalc.emv

/** Bit-field decoders for TVR, TSI, AIP and the Visa / Mastercard card verification results. */
object EmvDecoders {

    private fun bits(value: Int, names: Array<out String?>): List<String> =
        (7 downTo 0).mapNotNull { bit -> if ((value shr bit) and 1 == 1) names[7 - bit] else null }

    private fun byteRows(data: ByteArray, tables: List<Array<out String?>>, label: String): String = buildString {
        for (i in tables.indices) {
            if (i >= data.size) break
            val v = data[i].toInt() and 0xFF
            val set = bits(v, tables[i])
            appendLine("$label byte ${i + 1} = ${v.toString(16).padStart(2, '0').uppercase()}: ${if (set.isEmpty()) "no bits set" else set.joinToString("; ")}")
        }
    }.trimEnd()

    /** Terminal Verification Results, EMV 4.3 Book 3 Annex C5. */
    fun tvr(tvr: ByteArray): String {
        require(tvr.size == 5) { "TVR must be 5 bytes" }
        val t = listOf(
            arrayOf("Offline data authentication was not performed", "SDA failed", "ICC data missing", "Card appears on terminal exception file", "DDA failed", "CDA failed", "SDA selected", null),
            arrayOf("ICC and terminal have different application versions", "Expired application", "Application not yet effective", "Requested service not allowed for card product", "New card", null, null, null),
            arrayOf("Cardholder verification was not successful", "Unrecognised CVM", "PIN Try Limit exceeded", "PIN entry required and PIN pad not present or not working", "PIN entry required, PIN pad present, but PIN was not entered", "Online PIN entered", null, null),
            arrayOf("Transaction exceeds floor limit", "Lower consecutive offline limit exceeded", "Upper consecutive offline limit exceeded", "Transaction selected randomly for online processing", "Merchant forced transaction online", null, null, null),
            arrayOf("Default TDOL used", "Issuer authentication failed", "Script processing failed before final GENERATE AC", "Script processing failed after final GENERATE AC", "Relay resistance threshold exceeded", "Relay resistance time limits exceeded", "RRP performed (bit 2)", "RRP performed (bit 1)"),
        )
        return byteRows(tvr, t, "TVR")
    }

    /** Transaction Status Information, Book 3 Annex C6. */
    fun tsi(tsi: ByteArray): String {
        require(tsi.size == 2) { "TSI must be 2 bytes" }
        val t = listOf(arrayOf("Offline data authentication was performed", "Cardholder verification was performed", "Card risk management was performed", "Issuer authentication was performed", "Terminal risk management was performed", "Script processing was performed", null, null), arrayOf<String?>(null, null, null, null, null, null, null, null))
        return byteRows(tsi, t, "TSI")
    }

    /** Application Interchange Profile, Book 3 Annex C1. */
    fun aip(aip: ByteArray): String {
        require(aip.size == 2) { "AIP must be 2 bytes" }
        val t = listOf(arrayOf("XDA supported", "SDA supported", "DDA supported", "Cardholder verification is supported", "Terminal risk management is to be performed", "Issuer authentication is supported", "On device cardholder verification is supported", "CDA supported"), arrayOf("EMV mode is supported (contactless)", null, null, null, null, null, null, "Relay resistance protocol is supported"))
        return byteRows(aip, t, "AIP")
    }

    /** Visa CVR (4 bytes from the IAD, VIS 1.4/1.5). */
    fun visaCvr(cvr: ByteArray): String {
        require(cvr.size == 4) { "Visa CVR is 4 bytes" }
        val b1 = cvr[0].toInt() and 0xFF
        val b2 = cvr[1].toInt() and 0xFF
        val b3 = cvr[2].toInt() and 0xFF
        val b4 = cvr[3].toInt() and 0xFF
        fun acType(v: Int) = when (v) { 0 -> "AAC"; 1 -> "TC"; 2 -> "ARQC"; else -> "RFU" }
        return buildString {
            appendLine("CVR byte 1 = ${b1.toString(16).padStart(2, '0').uppercase()}: length of CVR = ${b1 and 0x0F}")
            appendLine("CVR byte 2 = ${b2.toString(16).padStart(2, '0').uppercase()}: second GENERATE AC returned ${acType((b2 shr 6) and 3)}; first GENERATE AC returned ${acType((b2 shr 4) and 3)}" +
                    (if (b2 and 0x08 != 0) "; issuer authentication performed and failed" else "") + (if (b2 and 0x04 != 0) "; offline PIN verification performed" else "") +
                    (if (b2 and 0x02 != 0) "; offline PIN verification failed" else "") + (if (b2 and 0x01 != 0) "; unable to go online" else ""))
            appendLine("CVR byte 3 = ${b3.toString(16).padStart(2, '0').uppercase()}: " + listOfNotNull(
                if (b3 and 0x80 != 0) "last online transaction not completed" else null, if (b3 and 0x40 != 0) "PIN try limit exceeded" else null,
                if (b3 and 0x20 != 0) "velocity checking counters exceeded" else null, if (b3 and 0x10 != 0) "new card" else null,
                if (b3 and 0x08 != 0) "issuer authentication failure on last online transaction" else null, if (b3 and 0x04 != 0) "issuer authentication not performed after online authorisation" else null,
                if (b3 and 0x02 != 0) "application blocked by card because PIN try limit exceeded" else null, if (b3 and 0x01 != 0) "offline static data authentication failed on last transaction and transaction declined offline" else null,
            ).ifEmpty { listOf("no bits set") }.joinToString("; "))
            append("CVR byte 4 = ${b4.toString(16).padStart(2, '0').uppercase()}: number of issuer script commands = ${(b4 shr 4) and 0x0F}" +
                    (if (b4 and 0x08 != 0) "; issuer script processing failed" else "") + (if (b4 and 0x04 != 0) "; offline dynamic data authentication failed on last transaction and transaction declined offline" else "") +
                    (if (b4 and 0x02 != 0) "; offline dynamic data authentication performed" else ""))
        }
    }

    /** Mastercard M/Chip CVR (6 bytes from the IAD). */
    fun mastercardCvr(cvr: ByteArray): String {
        require(cvr.size == 6) { "Mastercard CVR is 6 bytes" }
        val b1 = cvr[0].toInt() and 0xFF
        val b2 = cvr[1].toInt() and 0xFF
        fun acType(v: Int) = when (v) { 0 -> "AAC"; 1 -> "TC"; 2 -> "ARQC"; else -> "RFU" }
        val t = listOf(
            arrayOf("Date check failed", "Offline PIN verification performed", "Offline encrypted PIN verification performed", "Offline PIN verification successful", "DDA returned", "Combined DDA/AC generation returned in first GENERATE AC", "Combined DDA/AC generation returned in second GENERATE AC", "Issuer authentication performed"),
            arrayOf("Script received", "Script failed", "Offline PIN verification performed (byte 4)", "Match found in additional check table", "No match found in additional check table", "Offline change PIN result", null, null),
            arrayOf("Lower consecutive offline limit exceeded", "Upper consecutive offline limit exceeded", "Lower cumulative offline limit exceeded", "Upper cumulative offline limit exceeded", "Go online on next transaction was set", "Issuer authentication failed", "Script received", "Script failed"),
            arrayOf("Domestic transaction", "International transaction", "Terminal erroneously considers offline PIN OK", "Lower consecutive counter exceeded", "Upper consecutive counter exceeded", "Lower cumulative amount exceeded", "Upper cumulative amount exceeded", "Issuer discretionary bit"),
        )
        return buildString {
            appendLine("CVR byte 1 = ${b1.toString(16).padStart(2, '0').uppercase()}: second GENERATE AC returned ${acType((b1 shr 6) and 3)}; first GENERATE AC returned ${acType((b1 shr 4) and 3)}" +
                    (if (b1 and 0x08 != 0) "; date check failed" else "") + (if (b1 and 0x04 != 0) "; offline PIN verification performed" else "") +
                    (if (b1 and 0x02 != 0) "; offline encrypted PIN verification performed" else "") + (if (b1 and 0x01 != 0) "; offline PIN verification successful" else ""))
            appendLine("CVR byte 2 = ${b2.toString(16).padStart(2, '0').uppercase()}: " + listOfNotNull(
                if (b2 and 0x80 != 0) "DDA returned" else null, if (b2 and 0x40 != 0) "combined DDA/AC generation returned in first GENERATE AC" else null,
                if (b2 and 0x20 != 0) "combined DDA/AC generation returned in second GENERATE AC" else null, if (b2 and 0x10 != 0) "issuer authentication performed" else null,
                if (b2 and 0x08 != 0) "CIAC-default skipped on CAT3" else null, if (b2 and 0x04 != 0) "offline change PIN result (bit 3)" else null,
            ).ifEmpty { listOf("no bits set") }.joinToString("; ") + "; PIN try counter = ${b2 and 0x03} (low bits)")
            append(byteRows(cvr.copyOfRange(2, 6), t, "CVR (from byte 3)"))
        }
    }
}
