package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.terminal

import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One transaction the terminal will attempt against a card. All amounts are minor units
 * (paise / cents) per EMV n12 amount fields.
 */
data class TransactionRequest(
    val amount: Long,
    val amountOther: Long = 0,
    val type: TransactionType = TransactionType.PURCHASE,
    /** 9A — yyMMdd. Defaults to current date in terminal locale. */
    val date: String = todayYyMmDd(),
    /** 9F21 — HHmmss. Defaults to current time. */
    val time: String = nowHhMmSs(),
    /** 9F37 — 4 random bytes. Auto-generated if not supplied. */
    val unpredictableNumber: ByteArray = ByteArray(4).also { SecureRandom().nextBytes(it) },
    /** Force online — sets TVR bit-7 byte-4 ("Online cryptogram required") before terminal action analysis. */
    val forceOnline: Boolean = false,
    /** Force offline decline — terminal goes straight to AAC. */
    val forceDecline: Boolean = false,
)

enum class TransactionType(val code: Byte, val label: String) {
    PURCHASE(0x00, "Purchase"),
    CASH_ADVANCE(0x01, "Cash advance"),
    PURCHASE_WITH_CASHBACK(0x09, "Purchase with cashback"),
    REFUND(0x20, "Refund"),
    BALANCE_INQUIRY(0x30, "Balance inquiry"),
    ;

    val byte: Byte get() = code
}

private fun todayYyMmDd(): String =
    SimpleDateFormat("yyMMdd", Locale.US).format(Date())

private fun nowHhMmSs(): String =
    SimpleDateFormat("HHmmss", Locale.US).format(Date())
