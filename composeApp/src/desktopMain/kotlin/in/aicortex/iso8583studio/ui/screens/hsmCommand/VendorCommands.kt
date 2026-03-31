package `in`.aicortex.iso8583studio.ui.screens.hsmCommand

import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.HsmVendorType

data class VendorCommand(
    val code: String,
    val name: String,
    val description: String,
    val defaultData: String,
)

fun getVendorCommands(vendor: HsmVendorType): List<VendorCommand> {
    return when (vendor) {
        HsmVendorType.THALES_PAYSHIELD -> thalesCommands
        HsmVendorType.FUTUREX -> futurexCommands
        HsmVendorType.SAFENET_LUNA -> lunaCommands
        HsmVendorType.UTIMACO -> utimacoCommands
        HsmVendorType.NCIPHER -> ncipherCommands
        HsmVendorType.ATALLA -> atallaCommands
        HsmVendorType.GENERIC -> genericCommands
    }
}

private val thalesCommands = listOf(
    VendorCommand("NC", "Diagnostics", "Health check / diagnostics command", "4E43"),
    VendorCommand("A0", "Generate Key", "Generate a random key", "4130303030553131"),
    VendorCommand("A6", "Import Key", "Import a key under ZMK", "4136"),
    VendorCommand("A8", "Export Key", "Export a key under ZMK", "4138"),
    VendorCommand("BU", "Generate MAC (MAB)", "Generate a MAC using ANSI X9.19", "4255"),
    VendorCommand("BW", "Verify MAC (MAB)", "Verify a MAC using ANSI X9.19", "4257"),
    VendorCommand("CA", "Translate PIN (ZPK to LMK)", "Translate PIN from ZPK to LMK encryption", "4341"),
    VendorCommand("CC", "Translate PIN (LMK to ZPK)", "Translate PIN from LMK to ZPK encryption", "4343"),
    VendorCommand("CW", "Generate CVV", "Generate a Card Verification Value", "4357"),
    VendorCommand("CY", "Verify CVV", "Verify a Card Verification Value", "4359"),
    VendorCommand("DE", "Generate PIN Offset (IBM)", "Generate PIN offset using IBM 3624", "4445"),
    VendorCommand("EA", "Verify PIN (IBM offset)", "Verify a PIN using IBM 3624 method", "4541"),
    VendorCommand("EC", "Verify PIN (interchange)", "Verify an interchange PIN", "4543"),
    VendorCommand("EE", "Derive PIN Using Offset", "Derive a PIN from an offset", "4545"),
    VendorCommand("EI", "Generate PIN", "Generate a random PIN", "4549"),
    VendorCommand("FA", "Translate ZPK from ZMK to LMK", "Translate a ZPK from ZMK to LMK", "4641"),
    VendorCommand("FI", "Generate ZPK", "Generate a Zone PIN Key", "4649"),
    VendorCommand("GC", "Translate TAK from ZMK to LMK", "Translate TAK from ZMK to LMK", "4743"),
    VendorCommand("GI", "Generate TMK/TPK/PVK", "Generate Terminal Master Key", "4749"),
    VendorCommand("HA", "Generate TAK", "Generate Terminal Authentication Key", "4841"),
    VendorCommand("HC", "Generate TMK/TPK", "Generate Terminal Master/PIN Key", "4843"),
    VendorCommand("KQ", "Get HSM Loading", "Get current HSM loading information", "4B51"),
    VendorCommand("NO", "Get HSM Status", "Retrieve current HSM status", "4E4F"),
    VendorCommand("B2", "Echo Test", "Echo test command", "4232"),
    VendorCommand("MS", "Generate MAC (ISO 9797-1)", "Generate MAC using ISO 9797-1 Algorithm 1/3", "4D53"),
    VendorCommand("MW", "Verify MAC (ISO 9797-1)", "Verify MAC using ISO 9797-1", "4D57"),
    VendorCommand("EW", "Generate ARPC", "Generate Authorization Response Cryptogram", "4557"),
    VendorCommand("KA", "Derive DUKPT Key", "Derive a key using DUKPT method", "4B41"),
    VendorCommand("IA", "Generate ZAK", "Generate Zone Authentication Key", "4941"),
)

private val futurexCommands = listOf(
    VendorCommand("ECHO", "Echo Test", "Basic echo/health check", "4543484F"),
    VendorCommand("RAND", "Generate Random", "Generate random number/key", "52414E44"),
    VendorCommand("GKEY", "Generate Key", "Generate symmetric key", "474B4559"),
    VendorCommand("EKEY", "Export Key", "Export key encrypted under KEK", "454B4559"),
    VendorCommand("IKEY", "Import Key", "Import key encrypted under KEK", "494B4559"),
    VendorCommand("GMAC", "Generate MAC", "Generate MAC on data block", "474D4143"),
    VendorCommand("VMAC", "Verify MAC", "Verify MAC on data block", "564D4143"),
    VendorCommand("ENCR", "Encrypt Data", "Encrypt data block", "454E4352"),
    VendorCommand("DECR", "Decrypt Data", "Decrypt data block", "44454352"),
    VendorCommand("TPIN", "Translate PIN", "Translate PIN block between keys", "5450494E"),
    VendorCommand("VPIN", "Verify PIN", "Verify PIN against offset/PVV", "5650494E"),
    VendorCommand("GCVV", "Generate CVV", "Generate Card Verification Value", "47435656"),
    VendorCommand("VCVV", "Verify CVV", "Verify Card Verification Value", "56435656"),
    VendorCommand("STAT", "Status Check", "Query device status", "53544154"),
)

private val lunaCommands = listOf(
    VendorCommand("ECHO", "Echo", "Connectivity test", "4543484F"),
    VendorCommand("GKEY", "Generate Key", "Generate symmetric/asymmetric key", "474B4559"),
    VendorCommand("SIGN", "Digital Sign", "Create digital signature", "5349474E"),
    VendorCommand("VRFY", "Verify Signature", "Verify digital signature", "56524659"),
    VendorCommand("ENCR", "Encrypt", "Encrypt data block", "454E4352"),
    VendorCommand("DECR", "Decrypt", "Decrypt data block", "44454352"),
    VendorCommand("WRAP", "Wrap Key", "Wrap key for export", "57524150"),
    VendorCommand("UNWP", "Unwrap Key", "Unwrap imported key", "554E5750"),
    VendorCommand("HMAC", "Generate HMAC", "Generate HMAC value", "484D4143"),
    VendorCommand("RAND", "Generate Random", "Generate random bytes", "52414E44"),
    VendorCommand("STAT", "Device Status", "Get device status and info", "53544154"),
)

private val utimacoCommands = listOf(
    VendorCommand("PING", "Ping", "Health check ping", "50494E47"),
    VendorCommand("GKEY", "Generate Key", "Generate key on device", "474B4559"),
    VendorCommand("EBLK", "Encrypt Block", "Encrypt a data block", "45424C4B"),
    VendorCommand("DBLK", "Decrypt Block", "Decrypt a data block", "44424C4B"),
    VendorCommand("SIGN", "Sign Data", "Sign data with private key", "5349474E"),
    VendorCommand("VERI", "Verify Signature", "Verify signature with public key", "56455249"),
    VendorCommand("STAT", "Status", "Get CryptoServer status", "53544154"),
)

private val ncipherCommands = listOf(
    VendorCommand("ECHO", "Echo", "Basic connectivity test", "4543484F"),
    VendorCommand("STAT", "Status", "Get nShield module status", "53544154"),
    VendorCommand("GKEY", "Generate Key", "Generate key in security world", "474B4559"),
    VendorCommand("ENCR", "Encrypt", "Encrypt data", "454E4352"),
    VendorCommand("DECR", "Decrypt", "Decrypt data", "44454352"),
    VendorCommand("SIGN", "Sign", "Sign data", "5349474E"),
    VendorCommand("VRFY", "Verify", "Verify signature", "56524659"),
)

private val atallaCommands = listOf(
    VendorCommand("1#", "Health Check", "Basic health check", "3123"),
    VendorCommand("11#", "Encrypt Under MFK", "Encrypt data under MFK", "313123"),
    VendorCommand("12#", "Decrypt Under MFK", "Decrypt data under MFK", "313223"),
    VendorCommand("30#", "Translate PIN", "Translate PIN block", "333023"),
    VendorCommand("31#", "Verify PIN (IBM)", "Verify PIN using IBM method", "333123"),
    VendorCommand("3E0#", "Generate MAC", "Generate MAC on data", "334530"),
    VendorCommand("3E1#", "Verify MAC", "Verify MAC on data", "334531"),
    VendorCommand("71#", "Generate CVV", "Generate Card Verification Value", "373123"),
    VendorCommand("72#", "Verify CVV", "Verify Card Verification Value", "373223"),
    VendorCommand("7E#", "Generate ARQC/ARPC", "EMV cryptogram generation", "374523"),
)

private val genericCommands = listOf(
    VendorCommand("ECHO", "Echo/Ping", "Basic health check", "4543484F"),
    VendorCommand("STAT", "Status", "Get device status", "53544154"),
    VendorCommand("RAND", "Generate Random", "Generate random data", "52414E44"),
)
