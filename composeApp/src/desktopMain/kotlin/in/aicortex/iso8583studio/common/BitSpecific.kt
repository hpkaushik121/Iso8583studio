package `in`.aicortex.iso8583studio.data

import `in`.aicortex.iso8583studio.data.model.AddtionalOption
import `in`.aicortex.iso8583studio.data.model.BitLength
import `in`.aicortex.iso8583studio.data.model.BitType
import kotlinx.serialization.Serializable
import org.springframework.context.annotation.Description
import kotlin.math.absoluteValue

private val descriptionMap = mapOf<Int, String>(
    1 to "Bitmap",
    2 to "19	Primary account number (PAN)",
    3 to "Processing Code",
    4 to "Amount Transaction",
    5 to "Amount, settlement",
    6 to "Amount, cardholder billing",
    7 to "Transmission date & time",
    8 to "Amount, cardholder billing fee",
    9 to "Conversion rate, settlement",
    10 to "Conversion rate, cardholder billing",
    11 to "System trace audit number (STAN)",
    12 to "Local transaction time (hhmmss)",
    13 to "Local transaction date (MMDD)",
    14 to "Expiration date (YYMM)",
    15 to "Settlement date",
    16 to "Currency conversion date",
    17 to "Capture date",
    18 to "Merchant type, or merchant category code",
    19 to "Acquiring institution (country code)",
    20 to "PAN extended (country code)",
    21 to "Forwarding institution (country code)",
    22 to "Point of service entry mode",
    23 to "Application PAN sequence number",
    24 to "Function code (ISO 8583:1993), or network international identifier (NII)",
    25 to "Point of service condition code",
    26 to "Point of service capture code",
    27 to "Authorizing identification response length",
    28 to "Amount, transaction fee",
    29 to "Amount, settlement fee",
    30 to "Amount, transaction processing fee",
    31 to "Amount, settlement processing fee",
    32 to "Acquiring institution identification code",
    33 to "Forwarding institution identification code",
    34 to "Primary account number, extended",
    35 to "Track 2 data",
    36 to "Track 3 data",
    37 to "Retrieval reference number",
    38 to "Authorization identification response",
    39 to "Response code",
    40 to "Service restriction code",
    41 to "Card acceptor terminal identification",
    42 to "Card acceptor identification code",
    43 to "Card acceptor name/location",
    44 to "Additional response data",
    45 to "Track 1 data",
    46 to "Additional data (ISO)",
    47 to "Additional data (national)",
    48 to "Additional data (private)",
    49 to "Currency code, transaction",
    50 to "Currency code, settlement",
    51 to "Currency code, cardholder billing",
    52 to "Personal identification number data",
    53 to "Security related control information",
    54 to "Additional amounts",
    55 to "ICC data – EMV having multiple tags",
    56 to "Reserved (ISO)",
    57 to "Reserved (national)",
    58 to "",
    59 to "",
    60 to "Reserved (national) (e.g. settlement request: batch number, advice transactions: original transaction amount, batch upload: original MTI plus original RRN plus original STAN, etc.)",
    61 to "Reserved (private) (e.g. CVV2/service code   transactions)",
    62 to "Reserved (private) (e.g. transactions: invoice number, key exchange transactions: TPK key, etc.)",
    63 to "Reserved (private)",
    64 to "Message authentication code (MAC)",
    65 to "Extended bitmap indicator",
    66 to "Settlement code",
    67 to "Extended payment code",
    68 to "Receiving institution country code",
    69 to "Settlement institution country code",
    70 to "Network management information code",
    71 to "Message number",
    72 to "Last message's number",
    73 to "Action date (YYMMDD)",
    74 to "Number of credits",
    75 to "Credits, reversal number",
    76 to "Number of debits",
    77 to "Debits, reversal number",
    78 to "Transfer number",
    79 to "Transfer, reversal number",
    80 to "Number of inquiries",
    81 to "Number of authorizations",
    82 to "Credits, processing fee amount",
    83 to "Credits, transaction fee amount",
    84 to "Debits, processing fee amount",
    85 to "Debits, transaction fee amount",
    86 to "Total amount of credits",
    87 to "Credits, reversal amount",
    88 to "Total amount of debits",
    89 to "Debits, reversal amount",
    90 to "Original data elements",
    91 to "File update code",
    92 to "File security code",
    93 to "Response indicator",
    94 to "Service indicator",
    95 to "Replacement amounts",
    96 to "Message security code",
    97 to "Net settlement amount",
    98 to "Payee",
    99 to "Settlement institution identification code",
    100 to "Receiving institution identification code",
    101 to "File name",
    102 to "Account identification 1",
    103 to "Account identification 2",
    104 to "Transaction description",
    105 to "Reserved for ISO use",
    106 to "",
    107 to "",
    108 to "",
    109 to "",
    110 to "",
    111 to "",
    112 to "Reserved for national use",
    113 to "",
    114 to "",
    115 to "",
    116 to "",
    117 to "",
    118 to "",
    119 to "",
    120 to "Reserved for private use",
    121 to "",
    122 to "",
    123 to "",
    124 to "",
    125 to "",
    126 to "",
    127 to "",
    128 to "Message authentication code"
)

@Serializable
data class BitSpecific(
    var bitNumber: Byte = 0,
    var bitLength: BitLength = BitLength.FIXED,
    var bitType: BitType = BitType.NOT_SPECIFIC,
    var maxLength: Int = 0,
    var addtionalOption: AddtionalOption = AddtionalOption.None,
) {
    private var m_Description:String =""

    var description
        get() = if(m_Description.isEmpty()){
            descriptionMap[bitNumber.toInt().absoluteValue] ?: ""
        } else{
            m_Description
        }
        set(value) { m_Description = value}


    var lengthType: BitLength
        get() = bitLength
        set(value) {
            bitLength = value
        }

    var formatType: BitType
        get() = bitType
        set(value) {
            bitType = value
        }


    override fun toString(): String {
        return "${bitNumber.toString().padEnd(4)}${
            bitLength.toString().padEnd(10)
        }${maxLength.toString().padEnd(4)}${bitType.toString().padEnd(10)}"
    }

    // Secondary constructors to match original code
    constructor(bitNo: Byte, bitLenAtrr: BitLength, bitTypeAtrr: BitType, maxLen: Int) : this() {
        bitNumber = bitNo
        bitLength = bitLenAtrr
        bitType = bitTypeAtrr
        maxLength = maxLen
        addtionalOption = AddtionalOption.None
    }
}