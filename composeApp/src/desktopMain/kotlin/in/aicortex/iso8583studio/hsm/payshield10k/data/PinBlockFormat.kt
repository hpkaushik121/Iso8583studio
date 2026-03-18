package `in`.aicortex.iso8583studio.hsm.payshield10k.data

/**
 * PIN Block formats as supported by Thales payShield 10K.
 *
 * Code  Description
 * ----  ------------------------------------------
 * 01    ISO 9564-1 & ANSI X9.8 format 0
 * 02    Docutel ATM format
 * 03    Diebold & IBM ATM format
 * 04    PLUS Network format
 * 05    ISO 9564-1 format 1
 * 46    AS2805
 * 47    ISO 9564-1 & ANSI X9.8 format 3
 * 48    ISO 9564-1 format 4
 */
enum class PinBlockFormat(val code: String, val description: String) {
    ISO_FORMAT_0("01", "ISO 9564-1 & ANSI X9.8 format 0"),
    DOCUTEL("02",      "Docutel ATM format"),
    DIEBOLD_IBM("03",  "Diebold & IBM ATM format"),
    PLUS_NETWORK("04", "PLUS Network format"),
    ISO_FORMAT_1("05", "ISO 9564-1 format 1"),
    AS2805("46",       "AS2805"),
    ISO_FORMAT_3("47", "ISO 9564-1 & ANSI X9.8 format 3"),
    ISO_FORMAT_4("48", "ISO 9564-1 format 4"),
}
