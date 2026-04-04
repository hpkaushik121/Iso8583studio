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

private val thalesCommands: List<VendorCommand> = thalesCommandDefinitions.map { def ->
    VendorCommand(def.code, def.name, def.description, def.code)
}

private val futurexCommands = listOf(
    VendorCommand("ECHO", "Echo Test", "Basic echo/health check", "ECHO"),
    VendorCommand("RAND", "Generate Random", "Generate random number/key", "RAND"),
    VendorCommand("GKEY", "Generate Key", "Generate symmetric key", "GKEY"),
    VendorCommand("EKEY", "Export Key", "Export key encrypted under KEK", "EKEY"),
    VendorCommand("IKEY", "Import Key", "Import key encrypted under KEK", "IKEY"),
    VendorCommand("GMAC", "Generate MAC", "Generate MAC on data block", "GMAC"),
    VendorCommand("VMAC", "Verify MAC", "Verify MAC on data block", "VMAC"),
    VendorCommand("ENCR", "Encrypt Data", "Encrypt data block", "ENCR"),
    VendorCommand("DECR", "Decrypt Data", "Decrypt data block", "DECR"),
    VendorCommand("TPIN", "Translate PIN", "Translate PIN block between keys", "TPIN"),
    VendorCommand("VPIN", "Verify PIN", "Verify PIN against offset/PVV", "VPIN"),
    VendorCommand("GCVV", "Generate CVV", "Generate Card Verification Value", "GCVV"),
    VendorCommand("VCVV", "Verify CVV", "Verify Card Verification Value", "VCVV"),
    VendorCommand("STAT", "Status Check", "Query device status", "STAT"),
)

private val lunaCommands = listOf(
    VendorCommand("ECHO", "Echo", "Connectivity test", "ECHO"),
    VendorCommand("GKEY", "Generate Key", "Generate symmetric/asymmetric key", "GKEY"),
    VendorCommand("SIGN", "Digital Sign", "Create digital signature", "SIGN"),
    VendorCommand("VRFY", "Verify Signature", "Verify digital signature", "VRFY"),
    VendorCommand("ENCR", "Encrypt", "Encrypt data block", "ENCR"),
    VendorCommand("DECR", "Decrypt", "Decrypt data block", "DECR"),
    VendorCommand("WRAP", "Wrap Key", "Wrap key for export", "WRAP"),
    VendorCommand("UNWP", "Unwrap Key", "Unwrap imported key", "UNWP"),
    VendorCommand("HMAC", "Generate HMAC", "Generate HMAC value", "HMAC"),
    VendorCommand("RAND", "Generate Random", "Generate random bytes", "RAND"),
    VendorCommand("STAT", "Device Status", "Get device status and info", "STAT"),
)

private val utimacoCommands = listOf(
    VendorCommand("PING", "Ping", "Health check ping", "PING"),
    VendorCommand("GKEY", "Generate Key", "Generate key on device", "GKEY"),
    VendorCommand("EBLK", "Encrypt Block", "Encrypt a data block", "EBLK"),
    VendorCommand("DBLK", "Decrypt Block", "Decrypt a data block", "DBLK"),
    VendorCommand("SIGN", "Sign Data", "Sign data with private key", "SIGN"),
    VendorCommand("VERI", "Verify Signature", "Verify signature with public key", "VERI"),
    VendorCommand("STAT", "Status", "Get CryptoServer status", "STAT"),
)

private val ncipherCommands = listOf(
    VendorCommand("ECHO", "Echo", "Basic connectivity test", "ECHO"),
    VendorCommand("STAT", "Status", "Get nShield module status", "STAT"),
    VendorCommand("GKEY", "Generate Key", "Generate key in security world", "GKEY"),
    VendorCommand("ENCR", "Encrypt", "Encrypt data", "ENCR"),
    VendorCommand("DECR", "Decrypt", "Decrypt data", "DECR"),
    VendorCommand("SIGN", "Sign", "Sign data", "SIGN"),
    VendorCommand("VRFY", "Verify", "Verify signature", "VRFY"),
)

private val atallaCommands = listOf(
    VendorCommand("1#", "Health Check", "Basic health check", "1#"),
    VendorCommand("11#", "Encrypt Under MFK", "Encrypt data under MFK", "11#"),
    VendorCommand("12#", "Decrypt Under MFK", "Decrypt data under MFK", "12#"),
    VendorCommand("30#", "Translate PIN", "Translate PIN block", "30#"),
    VendorCommand("31#", "Verify PIN (IBM)", "Verify PIN using IBM method", "31#"),
    VendorCommand("3E0#", "Generate MAC", "Generate MAC on data", "3E0#"),
    VendorCommand("3E1#", "Verify MAC", "Verify MAC on data", "3E1#"),
    VendorCommand("71#", "Generate CVV", "Generate Card Verification Value", "71#"),
    VendorCommand("72#", "Verify CVV", "Verify Card Verification Value", "72#"),
    VendorCommand("7E#", "Generate ARQC/ARPC", "EMV cryptogram generation", "7E#"),
)

private val genericCommands = listOf(
    VendorCommand("ECHO", "Echo/Ping", "Basic health check", "ECHO"),
    VendorCommand("STAT", "Status", "Get device status", "STAT"),
    VendorCommand("RAND", "Generate Random", "Generate random data", "RAND"),
)
