package io.cryptocalc.core.types

import kotlinx.serialization.Serializable

@Serializable
enum class CryptogramType {
    ARQC, TC, AAC, ARPC
}

@Serializable
enum class CryptogramVersion {
    CVN_10, CVN_14, CVN_17, CVN_18
}

@Serializable
data class EMVData(
    val pan: String,
    val panSequenceNumber: String = "00",
    val atc: String,
    val unpredictableNumber: String,
    val amount: String = "",
    val otherAmount: String = "",
    val terminalCountryCode: String = "",
    val terminalVerificationResults: String = "",
    val transactionCurrencyCode: String = "",
    val transactionDate: String = "",
    val transactionType: String = ""
)
