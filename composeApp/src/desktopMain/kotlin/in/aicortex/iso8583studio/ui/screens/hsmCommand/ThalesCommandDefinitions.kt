package `in`.aicortex.iso8583studio.ui.screens.hsmCommand

private val keySchemeOptions = listOf(
    CodeOption("Z", "Z - Single-length DES"),
    CodeOption("U", "U - Double-length TDES"),
    CodeOption("T", "T - Triple-length TDES"),
    CodeOption("X", "X - Double-length TDES (variant)"),
    CodeOption("Y", "Y - Triple-length TDES (variant)"),
    CodeOption("S", "S - Keyblock"),
)

private val a0UnsetOption = CodeOption("", "— (not set) —")

private val a0ModeOptions = listOf(
    a0UnsetOption,
    CodeOption("0", "0 - Generate key"),
    CodeOption("1", "1 - Generate key and encrypt under ZMK / TMK / current BDK"),
    CodeOption("A", "A - Derive IKEY from BDK (DUKPT)"),
    CodeOption("B", "B - Derive IKEY + Export (DUKPT)"),
)

private val a0KeySchemeOptions = listOf(a0UnsetOption) + keySchemeOptions

/** No key type selects this; hides Atalla variant from the A0 builder (wire still omits it). */
private val a0NeverKeyType = FieldCondition("keyType", emptySet())

/** Mode A/B only, DUKPT derivation path (derive key mode 0). */
private val a0DukptDerivePath = listOf(
    FieldCondition("mode", setOf("A", "B")),
    FieldCondition("deriveKeyMode", setOf("0")),
)

/** Mode A/B only, ZKA derivation path (derive key mode 1). */
private val a0ZkaDerivePath = listOf(
    FieldCondition("mode", setOf("A", "B")),
    FieldCondition("deriveKeyMode", setOf("1")),
)

private val pinBlockFormatOptions = listOf(
    CodeOption("01", "01 - ISO 9564-1 Format 0 (ANSI)"),
    CodeOption("02", "02 - Docutel"),
    CodeOption("03", "03 - Diebold/IBM 3624"),
    CodeOption("04", "04 - Plus Network"),
    CodeOption("05", "05 - ISO 9564-1 Format 1"),
    CodeOption("34", "34 - EMV 1996"),
    CodeOption("35", "35 - EMV 4.x (ISO 9564-1 Format 2)"),
    CodeOption("41", "41 - Mastercard Pay-Now"),
    CodeOption("47", "47 - ISO 9564-1 Format 3"),
    CodeOption("48", "48 - ISO 9564-1 Format 4"),
)

private val kcvTypeOptions = listOf(
    CodeOption("0", "0 - KCV (6H, VISA)"),
    CodeOption("1", "1 - KCV (16H)"),
)

/** BI command: key scheme under ZMK (host-specific; 0 = default in reference traces). */
private val biKeySchemeZmkOptions = listOf(
    CodeOption("S", "S - Key Block"),
    CodeOption("Z", "Z - Single-length DES"),
    CodeOption("U", "U - Double-length TDES"),
    CodeOption("T", "T - Triple-length TDES"),
    CodeOption("X", "X - Double-length TDES (variant)"),
    CodeOption("Y", "Y - Triple-length TDES (variant)"),
)

private val hashAlgorithmOptions = listOf(
    CodeOption("01", "01 - SHA-1"),
    CodeOption("02", "02 - MD5"),
    CodeOption("06", "06 - SHA-224"),
    CodeOption("07", "07 - SHA-256"),
    CodeOption("08", "08 - SHA-384"),
    CodeOption("09", "09 - SHA-512"),
)

private val macAlgorithmOptions = listOf(
    CodeOption("01", "01 - ANSI X9.19"),
    CodeOption("03", "03 - ISO 9797-1 Alg 3 PAD 2"),
    CodeOption("04", "04 - HMAC-SHA-1"),
    CodeOption("05", "05 - HMAC-SHA-256"),
)

private val cipherModeOptions = listOf(
    CodeOption("00", "00 - ECB"),
    CodeOption("01", "01 - CBC"),
    CodeOption("02", "02 - CFB-8"),
    CodeOption("03", "03 - OFB"),
)

private val dataFormatOptions = listOf(
    CodeOption("0", "0 - Binary"),
    CodeOption("1", "1 - Hex-Encoded Binary"),
    CodeOption("2", "2 - Text"),
)

private val keyTypeOptions = listOf(
    CodeOption("FFF", "FFF - Ignore Key Type (Key Block)"),
    CodeOption("000", "000 - Zone Master Key (ZMK)"),
    CodeOption("001", "001 - Zone PIN Key (ZPK)"),
    CodeOption("002", "002 - PIN Verification Key (PVK)"),
    CodeOption("003", "003 - Terminal Auth Key (TAK)"),
    CodeOption("006", "006 - Watchword Key"),
    CodeOption("008", "008 - Zone Auth Key (ZAK)"),
    CodeOption("009", "009 - BDK (DUKPT Base Derivation)"),
    CodeOption("00A", "00A - Data Encryption Key (ZEK)"),
    CodeOption("00B", "00B - Data Encryption Key (DEK-D)"),
    CodeOption("109", "109 - EMV Key AC"),
    CodeOption("200", "200 - Visa Cash Master Key"),
    CodeOption("209", "209 - EMV Key SMI"),
    CodeOption("302", "302 - DUKPT Initial Key"),
    CodeOption("309", "309 - EMV Key SMC"),
    CodeOption("30B", "30B - Data Encryption Key (TEK)"),
    CodeOption("402", "402 - Card Verification Key (CVK)"),
    CodeOption("409", "409 - EMV Key DAC"),
    CodeOption("509", "509 - EMV Key DN"),
    CodeOption("607", "607 - ZKA Master Key"),
    CodeOption("609", "609 - BDK2"),
    CodeOption("709", "709 - dCVV Master Key"),
    CodeOption("70D", "70D - Terminal PIN Key (TPK)"),
    CodeOption("809", "809 - BDK3"),
    CodeOption("80D", "80D - Terminal Master Key (TMK)"),
    CodeOption("909", "909 - BDK4"),
)

private val a0KeyTypeOptions = listOf(a0UnsetOption) + keyTypeOptions

private val keyBlockKeyUsageOptions = listOf(
    CodeOption("51", "51 - AES/DES/3DES Terminal Key Encryption (TMK)"),
    CodeOption("P0", "P0 - PIN Encryption"),
    CodeOption("K0", "K0 - Key Encryption / Wrapping"),
    CodeOption("K1", "K1 - TR-31 Key Block Protection"),
    CodeOption("D0", "D0 - Data Encryption"),
    CodeOption("D1", "D1 - Data Encryption (Decimalize)"),
    CodeOption("M0", "M0 - ISO 9797-1 MAC Alg 1"),
    CodeOption("M1", "M1 - ISO 9797-1 MAC Alg 1"),
    CodeOption("M3", "M3 - ISO 9797-1 MAC Alg 3"),
    CodeOption("M6", "M6 - ISO 9797-1:2011 CMAC"),
    CodeOption("M7", "M7 - HMAC"),
    CodeOption("C0", "C0 - CVV / CVC / CSC"),
    CodeOption("B0", "B0 - BDK Base Derivation Key"),
    CodeOption("B1", "B1 - Initial DUKPT Key"),
    CodeOption("V0", "V0 - PIN Verification IBM"),
    CodeOption("V1", "V1 - PIN Verification IBM (other)"),
    CodeOption("V2", "V2 - PIN Verification VISA PVV"),
    CodeOption("E0", "E0 - EMV/chip Issuer Master-Derive"),
    CodeOption("E1", "E1 - EMV/chip Issuer Master-Session"),
    CodeOption("E2", "E2 - EMV/chip Issuer Master-Common"),
    CodeOption("E4", "E4 - EMV/chip PIN Change"),
    CodeOption("00", "00 - Not Used / No Restrictions"),
)

private val keyBlockAlgorithmOptions = listOf(
    CodeOption("D1", "D1 - Single-length DES"),
    CodeOption("T2", "T2 - Double-length TDES"),
    CodeOption("T3", "T3 - Triple-length TDES"),
    CodeOption("A1", "A1 - AES-128"),
    CodeOption("A2", "A2 - AES-192"),
    CodeOption("A3", "A3 - AES-256"),
)

private val keyBlockModeOfUseOptions = listOf(
    CodeOption("N", "N - No Restrictions"),
    CodeOption("B", "B - Both Encrypt & Decrypt"),
    CodeOption("E", "E - Encrypt / Wrap Only"),
    CodeOption("D", "D - Decrypt / Unwrap Only"),
    CodeOption("G", "G - Generate (MAC/sign)"),
    CodeOption("V", "V - Verify (MAC/sign)"),
    CodeOption("C", "C - Compute / Generate"),
    CodeOption("S", "S - Signature Only"),
    CodeOption("T", "T - Both Sign & Decrypt"),
    CodeOption("X", "X - Derive Key"),
)

private val keyBlockExportabilityOptions = listOf(
    CodeOption("E", "E - Exportable (under trusted key)"),
    CodeOption("N", "N - Not Exportable"),
    CodeOption("S", "S - Sensitive (exportable under KEK)"),
)

private val keyBlockVisibility = listOf(
    FieldCondition("keyScheme", setOf("S")),
)

/** Standard key block optional fields appended after keyScheme when scheme = S. */
private fun keyBlockFields(
    defaultUsage: String = "51",
    defaultAlgorithm: String = "T2",
    defaultModeOfUse: String = "E",
    defaultExportability: String = "S",
) = listOf(
    f("keyBlockDelimiter", "Key Block Delimiter", FieldType.CODE, 1, default = "#",
        req = FieldRequirement.OPTIONAL,
        conds = keyBlockVisibility,
        options = listOf(
            CodeOption("#", "# - AES Key Block"),
            CodeOption(";", "; - Variant LMK Key Block"),
        )),
    f("keyBlockKeyUsage", "Key Block Key Usage", FieldType.CODE, 2, default = defaultUsage,
        req = FieldRequirement.OPTIONAL, conds = keyBlockVisibility,
        options = keyBlockKeyUsageOptions),
    f("keyBlockAlgorithm", "Key Block Algorithm", FieldType.CODE, 2, default = defaultAlgorithm,
        req = FieldRequirement.OPTIONAL, conds = keyBlockVisibility,
        options = keyBlockAlgorithmOptions),
    f("keyBlockModeOfUse", "Key Block Mode of Use", FieldType.CODE, 1, default = defaultModeOfUse,
        req = FieldRequirement.OPTIONAL, conds = keyBlockVisibility,
        options = keyBlockModeOfUseOptions),
    f("keyBlockKeyVersionNumber", "Key Block Key Version Number", FieldType.DEC, 2, default = "00",
        req = FieldRequirement.OPTIONAL, conds = keyBlockVisibility),
    f("keyBlockExportability", "Key Block Exportability", FieldType.CODE, 1, default = defaultExportability,
        req = FieldRequirement.OPTIONAL, conds = keyBlockVisibility,
        options = keyBlockExportabilityOptions),
    f("keyBlockNumberOfOptionalBlocks", "Key Block Num Optional Blocks", FieldType.DEC, 2, default = "00",
        req = FieldRequirement.OPTIONAL, conds = keyBlockVisibility),
)

private val lmkIdentifierOptions = listOf(
    CodeOption("", "(none) - Default LMK"),
    CodeOption("%00", "%00 - LMK Pair 00"),
    CodeOption("%01", "%01 - LMK Pair 01"),
    CodeOption("%02", "%02 - LMK Pair 02"),
    CodeOption("%03", "%03 - LMK Pair 03"),
    CodeOption("%04", "%04 - LMK Pair 04"),
)

private val lmkPairIdOptions = listOf(
    CodeOption("00", "00 - LMK Pair 00"),
    CodeOption("01", "01 - LMK Pair 01"),
    CodeOption("02", "02 - LMK Pair 02"),
    CodeOption("03", "03 - LMK Pair 03"),
    CodeOption("04", "04 - LMK Pair 04"),
)

private val kaKeyTypeCodeOptions = listOf(
    CodeOption("00", "00 - LMK pair 04-05"),
    CodeOption("01", "01 - LMK pair 06-07"),
    CodeOption("02", "02 - LMK pair 14-15"),
    CodeOption("03", "03 - LMK pair 16-17"),
    CodeOption("04", "04 - LMK pair 18-19"),
    CodeOption("05", "05 - LMK pair 20-21"),
    CodeOption("06", "06 - LMK pair 22-23"),
    CodeOption("07", "07 - LMK pair 24-25"),
    CodeOption("08", "08 - LMK pair 26-27"),
    CodeOption("09", "09 - LMK pair 28-29"),
    CodeOption("0A", "0A - LMK pair 30-31"),
    CodeOption("0B", "0B - LMK pair 32-33"),
    CodeOption("10", "10 - Variant 1 of LMK pair 04-05"),
    CodeOption("42", "42 - Variant 4 of LMK pair 14-15"),
    CodeOption("FF", "FF - Use key type specified after delimiter."),
)

private val kaKeyLengthFlagOptions = listOf(
    CodeOption("0", "0 - for single length key"),
    CodeOption("1", "1 - for double length key"),
    CodeOption("2", "2 - for triple length key"),
    CodeOption("F", "F - for a key block LMK"),
)

private val kaKcvTypeOptions = listOf(
    CodeOption("0", "0 : No KCV"),
    CodeOption("1", "1 : 6 digit KCV"),
)

private val kaKeyTypeOptions = listOf(
    CodeOption("00", "00-ZMK (encrypted under LMK pair 04-05)"),
    CodeOption("01", "01-ZPK (encrypted under LMK pair 06-07)"),
    CodeOption("02", "02-TMK, TPK or PVK (encrypted under LMK pair 14-15)"),
    CodeOption("03", "03-TAK (encrypted under LMK pair 16-17)"),
    CodeOption("04", "04-PVK (encrypted under LMK pair 14-15)"),
    CodeOption("05", "05-ZEK (encrypted under LMK pair 26-27)"),
    CodeOption("06", "06-ZAK (encrypted under LMK pair 26-27)"),
    CodeOption("07", "07-BDK (encrypted under LMK pair 14-15)"),
    CodeOption("08", "08-ZMK (encrypted under LMK pair 04-05)"),
    CodeOption("09", "09-ZPK (encrypted under LMK pair 06-07)"),
    CodeOption("0A", "0A-TMK/ZEK (encrypted under LMK pair 14-15)"),
    CodeOption("0B", "0B-ZAK (encrypted under LMK pair 14-15)"),
    CodeOption("0C", "0C-ZEK (encrypted under LMK pair 22-23)"),
    CodeOption("0D", "0D-ZAK (encrypted under LMK pair 22-23)"),
    CodeOption("0E", "0E-DEK (encrypted under LMK pair 28-29)"),
    CodeOption("0F", "0F-RSA Key (encrypted under LMK pair 28-29)"),
)

private val reservedZeroOptions = listOf(
    CodeOption("0", "0"),
    CodeOption("1", "1"),
)

private fun f(
    id: String,
    name: String,
    type: FieldType = FieldType.HEX,
    length: Int = 0,
    req: FieldRequirement = FieldRequirement.MANDATORY,
    default: String = "",
    desc: String = "",
    options: List<CodeOption>? = null,
    cond: FieldCondition? = null,
    conds: List<FieldCondition> = emptyList(),
    omitWireIfBlank: Boolean = false,
) = ThalesCommandField(id, name, type, length, req, default, desc, options, cond, conds, omitWireIfBlank)

val thalesCommandDefinitions: List<ThalesCommandDefinition> = listOf(

    // ==================== DIAGNOSTICS ====================

    ThalesCommandDefinition(
        code = "NC", responseCode = "ND", name = "Diagnostics",
        description = "Health check / diagnostics command",
        category = CommandCategory.DIAGNOSTICS,
        requestFields = emptyList(),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("lmkCheckValue", "LMK Check Value", FieldType.HEX, 6),
            f("commandCounter", "Command Counter", FieldType.DEC, 10),
            f("firmwareNumber", "Firmware Number", FieldType.ASCII, 9),
        ),
    ),

    ThalesCommandDefinition(
        code = "NO", responseCode = "NP", name = "HSM Status",
        description = "Retrieve current HSM status",
        category = CommandCategory.DIAGNOSTICS,
        requestFields = listOf(
            f("modeFlag", "Mode Flag", FieldType.CODE, 2, default = "00", options = listOf(
                CodeOption("00", "00 - Return Status Information"),
                CodeOption("01", "01 - Return PCI HSM Compliance"),
            )),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("ioBufferSize", "I/O Buffer Size", FieldType.DEC, 1),
            f("ethernetType", "Ethernet Type", FieldType.DEC, 1),
            f("numberOfTcpSockets", "Number of TCP Sockets", FieldType.DEC, 2),
            f("firmwareNumber", "Firmware Number", FieldType.ASCII, 9),
            f("dspFitted", "DSP Fitted", FieldType.DEC, 1, req = FieldRequirement.OPTIONAL),
            f("dspFirmwareVersion", "DSP Firmware Version", FieldType.ASCII, 4, req = FieldRequirement.OPTIONAL),
        ),
    ),

    ThalesCommandDefinition(
        code = "B2", responseCode = "B3", name = "Echo Test",
        description = "Echo test command - data is echoed back",
        category = CommandCategory.DIAGNOSTICS,
        requestFields = listOf(
            f("dataLength", "Data Length", FieldType.HEX, 4, default = "0008", desc = "Length of echo data in hex (4H)"),
            f("echoData", "Echo Data", FieldType.ASCII, 0, default = "TESTDATA"),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("echoData", "Echo Data", FieldType.ASCII, 0),
        ),
    ),

    ThalesCommandDefinition(
        code = "LG", responseCode = "LH", name = "Set HSM Response Delay",
        description = "Set the delay for which the HSM waits before responding",
        category = CommandCategory.DIAGNOSTICS,
        requestFields = listOf(
            f("delay", "Delay in ms", FieldType.DEC, 3, default = "000", desc = "Delay 000-999 ms"),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
        ),
    ),

    // ==================== KEY MANAGEMENT ====================

    ThalesCommandDefinition(
        code = "A0", responseCode = "A1", name = "Generate a Key",
        description = "Generate or derive a key. Mode 0/1: random key. Mode A/B: derive IKEY from DUKPT BDK + KSN.",
        category = CommandCategory.KEY_MANAGEMENT,
        requestFields = listOf(
            f("mode", "Mode", FieldType.CODE, 1, default = "", options = a0ModeOptions, omitWireIfBlank = true,
                desc = "0 = Generate key under LMK only. 1 = Generate + encrypt under ZMK/TMK. A = Derive IKEY from DUKPT BDK+KSN under LMK. B = Derive IKEY + encrypt under ZMK/TMK."),
            f("keyType", "Key Type", FieldType.CODE, 3, default = "", options = a0KeyTypeOptions, omitWireIfBlank = true,
                desc = "Output key type. Determines which LMK pair encrypts the key. FFF = KeyBlock (LMK pair from S-block header / KeyBlock attributes). For Mode A/B with DUKPT, this is the output IKEY type (e.g. 302=IKEY)."),
            f("keyScheme", "Key Scheme (LMK)", FieldType.CODE, 1, default = "", options = a0KeySchemeOptions, omitWireIfBlank = true,
                desc = "Encryption scheme for the output key under LMK. S = KeyBlock (when Key Type is FFF), U = double-length TDES variant, T = triple-length variant."),

            f("deriveKeyMode", "Derive Key Mode", FieldType.CODE, 1, default = "0",
                cond = FieldCondition("mode", setOf("A", "B")),
                desc = "Key derivation method. 0 = DUKPT: derive IKEY/IPEK from BDK + KSN using ANSI X9.24. 1 = ZKA: derive key from ZKA master key.",
                options = listOf(
                    CodeOption("0", "0 - DUKPT — derive IKEY from BDK + KSN"),
                    CodeOption("1", "1 - ZKA — derive ZKA key from ZKA master key"),
                )),
            f("dukptMasterKeyType", "DUKPT Master Key Type", FieldType.CODE, 1, default = "1",
                conds = a0DukptDerivePath,
                desc = "Type of DUKPT master key (BDK) under LMK pair 28-29. Each type uses a different LMK variant for decryption. 1=BDK-1, 2=BDK-2, 3=BDK-3, 4=BDK-4 (AES).",
                options = listOf( CodeOption("1", "1 - BDK-1"),
                    CodeOption("2", "2 - BDK-2"), CodeOption("3", "3 - BDK-3"),CodeOption("4", "4 - BDK-4")
                )),
            f("dukptMasterKey", "DUKPT Master Key (BDK under LMK)", FieldType.HEX, 0,
                conds = a0DukptDerivePath,
                desc = "BDK (Base Derivation Key) encrypted under LMK pair 28-29. S-block or scheme-prefixed hex (U/T/X). This key is used with the KSN to derive the IKEY via ANSI X9.24."),
            f("ksnTdes", "KSN (3DES BDK)", FieldType.HEX, 15,
                default = "FFFF9876543210E",
                conds = a0DukptDerivePath + listOf(FieldCondition("dukptMasterKeyType", setOf("1", "2", "3"))),
                desc = "15H Key Serial Number for 3DES DUKPT: KSI(6H)+DID(8H) right-justified, F-padded left. Last hex must be even. No transaction counter. E.g. KSI=303950, DID=12342468 → F30395012342468"),
            f("ksnAes", "KSN (AES BDK)", FieldType.HEX, 16,
                default = "FFFF987654321000",
                conds = a0DukptDerivePath + listOf(FieldCondition("dukptMasterKeyType", setOf("4"))),
                desc = "16H Key Serial Number for AES DUKPT: BDK_ID+DID right-justified, F-padded left. No transaction counter. E.g. BDK_ID=3039505, DID=12345678 → F303950512345678"),

            f("zkaMasterKeyType", "ZKA Master Key Type", FieldType.CODE, 3, default = "FFF",
                conds = a0ZkaDerivePath,
                desc = "Type of ZKA master key. FFF = KeyBlock, 607 = ZKA Master Key.",
                options = listOf(
                    CodeOption("FFF", "FFF - Key Block"),
                    CodeOption("607", "607 - ZKA Master Key"),
                )),
            f("zkaMasterKey", "Key Block ZKA Master Key", FieldType.HEX, 0,
                conds = a0ZkaDerivePath,
                desc = "ZKA master key encrypted under LMK (S-block or hex). Used as seed for ZKA key derivation."),
            f("zkaOption", "ZKA Option", FieldType.CODE, 1, default = "0",
                conds = a0ZkaDerivePath,
                desc = "0 = Derive key using the supplied RNDI value. 1 = HSM generates a new random RNDI.",
                options = listOf(
                    CodeOption("0", "0 - Derive key using supplied RNDI"),
                    CodeOption("1", "1 - Derive key using new RNDI"),
                )),
            f("zkaRandomNumberInput", "ZKA RNDI", FieldType.HEX, 32,
                default = "00000000000000000000000000000000",
                conds = a0ZkaDerivePath,
                desc = "32 hex digits (16 bytes) random number input for ZKA key derivation."),
            f("delimiterAfterLmkScheme", "Delimiter", FieldType.FLAG, 0, default = ";",
                req = FieldRequirement.OPTIONAL,
                cond = FieldCondition("mode", setOf("1", "B")),
                desc = "Separator ';' between key derivation fields and ZMK/TMK export fields."),
            f("zmkTmkFlag", "ZMK/TMK Flag", FieldType.CODE, 1, default = "0",
                conds = listOf(FieldCondition("mode", setOf("1", "B")), FieldCondition("delimiterAfterLmkScheme", setOf(";"))),
                desc = "0 = ZMK (Zone Master Key). 1 = TMK (Terminal Master Key).",
                options = listOf(CodeOption("0", "0 - ZMK"), CodeOption("1", "1 - TMK"))),
            f("zmkTmkBdk", "ZMK/TMK (under LMK)", FieldType.HEX, 0,
                cond = FieldCondition("mode", setOf("1", "B")),
                desc = "ZMK or TMK encrypted under LMK pair 00-01. The derived IKEY (Mode B) or generated key (Mode 1) will be re-encrypted under this key. For KeyBlock export (scheme S), this key is used directly as KBPK."),
            f("bdkInitialKsn", "Current BDK's Initial KSN", FieldType.HEX, 20,
                req = FieldRequirement.OPTIONAL,
                desc = "20H Initial KSN of the current BDK. Only used when ZMK/TMK flag = 1 (TMK mode) in Mode B.",
                conds = listOf(
                    FieldCondition("mode", setOf("B")),
                    FieldCondition("zmkTmkFlag", setOf("1")),
                )),

            f("zmkTmkBdkKeyScheme", "Key Scheme (ZMK/TMK/Curr. BDK)", FieldType.CODE, 1, default = "S",
                options = keySchemeOptions,
                cond = FieldCondition("mode", setOf("1", "B")),
                desc = "Encryption scheme for the output key under ZMK/TMK. S = KeyBlock (ZMK used as KBPK), U = single-length, T = double-length, X = triple-length. This controls the format of the second output key."),
            f("atallaVariant", "Atalla Variant", FieldType.ASCII, 1, req = FieldRequirement.OPTIONAL,
                conds = listOf(a0NeverKeyType)),
            f("lmkDelimiter", "Delimiter", FieldType.FLAG, 0, req = FieldRequirement.OPTIONAL,
                default = "%", desc = "Prefix '%' before the 2-digit LMK Identifier."),
            f("lmkPairId", "LMK Identifier", FieldType.CODE, 2, req = FieldRequirement.OPTIONAL,
                default = "03", options = lmkPairIdOptions,
                conds = listOf(FieldCondition("lmkDelimiter", setOf("%"))),
                desc = "LMK pair ID selecting which LMK set to use (e.g. 03). Each LMK ID can have different algorithm (TDES/AES)."),
        ) + keyBlockFields(defaultUsage = "51", defaultModeOfUse = "E", defaultExportability = "S"),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("keyUnderLMK", "Key under LMK", FieldType.HEX, 0,
                desc = "Generated/derived key encrypted under LMK. Format depends on Key Type: FFF → KeyBlock S-block, others → variant-prefixed hex."),
            f("keyUnderZmkTmkBdk", "Key under ZMK/TMK/BDK", FieldType.HEX, 0,
                req = FieldRequirement.OPTIONAL, cond = FieldCondition("mode", setOf("1", "B")),
                desc = "Same key re-encrypted under ZMK/TMK. Format depends on Key Scheme (ZMK/TMK) field: S → KeyBlock, U/T/X → variant-prefixed hex."),
            f("kcv", "Key Check Value", FieldType.HEX, 6,
                desc = "6H Key Check Value of the clear key. TDES: first 3 bytes of TDES-ECB(zeros). AES: first 3 bytes of CKCV."),
            f("zkaRandomNumberOutput", "ZKA Random Number", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL,
                desc = "Random number output from ZKA derivation (only present when ZKA option = 1)."),
        ),
        forceVerticalFieldLayout = true,
    ),

    ThalesCommandDefinition(
        code = "A4", responseCode = "A5", name = "Form Key from Components",
        description = "Form a key from 2 to 9 encrypted key components (XOR)",
        category = CommandCategory.KEY_MANAGEMENT,
        requestFields = listOf(
            f("numComponents", "Number of Components", FieldType.CODE, 1, default = "2",
                options = listOf(
                    CodeOption("2", "2"), CodeOption("3", "3"), CodeOption("4", "4"),
                    CodeOption("5", "5"), CodeOption("6", "6"), CodeOption("7", "7"),
                    CodeOption("8", "8"), CodeOption("9", "9"),
                )),
            f("keyType", "Key Type", FieldType.CODE, 3, default = "FFF", options = keyTypeOptions),
            f("keyScheme", "Key Scheme (LMK)", FieldType.CODE, 1, default = "S", options = keySchemeOptions),
            f("component1", "Component 1", FieldType.HEX, 0,
                default = "S10096P0TN00E0003DDAD808976114849F5464E8944D2205F25A308863340D066ADCDA79E3B1F08AC034229C023D01106",
                desc = "Key component encrypted under LMK (S-block or hex)"),
            f("component2", "Component 2", FieldType.HEX, 0,
                default = "S10096P0TN00E00032064E90DF52228DC26D06EB751D3E3533D7B156209CAFDDBB067BB71275981C2F6BA951AC759190D",
                desc = "Key component encrypted under LMK (S-block or hex)"),
            f("component3", "Component 3", FieldType.HEX, 0,
                cond = FieldCondition("numComponents", setOf("3","4","5","6","7","8","9")),
                desc = "Key component encrypted under LMK"),
            f("component4", "Component 4", FieldType.HEX, 0,
                cond = FieldCondition("numComponents", setOf("4","5","6","7","8","9")),
                desc = "Key component encrypted under LMK"),
            f("component5", "Component 5", FieldType.HEX, 0,
                cond = FieldCondition("numComponents", setOf("5","6","7","8","9")),
                desc = "Key component encrypted under LMK"),
            f("component6", "Component 6", FieldType.HEX, 0,
                cond = FieldCondition("numComponents", setOf("6","7","8","9")),
                desc = "Key component encrypted under LMK"),
            f("component7", "Component 7", FieldType.HEX, 0,
                cond = FieldCondition("numComponents", setOf("7","8","9")),
                desc = "Key component encrypted under LMK"),
            f("component8", "Component 8", FieldType.HEX, 0,
                cond = FieldCondition("numComponents", setOf("8","9")),
                desc = "Key component encrypted under LMK"),
            f("component9", "Component 9", FieldType.HEX, 0,
                cond = FieldCondition("numComponents", setOf("9")),
                desc = "Key component encrypted under LMK"),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL,
                options = lmkIdentifierOptions, default = "%03",
                desc = "Alternate LMK pair (%nn)"),
        ) + keyBlockFields(defaultUsage = "P0", defaultModeOfUse = "N", defaultExportability = "E"),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("keyUnderLMK", "Key under LMK", FieldType.HEX, 0),
            f("kcv", "Key Check Value", FieldType.HEX, 6),
        ),
    ),

    ThalesCommandDefinition(
        code = "A6", responseCode = "A7", name = "Import a Key",
        description = "Import a key encrypted under a ZMK and re-encrypt under LMK",
        category = CommandCategory.KEY_MANAGEMENT,
        requestFields = listOf(
            f("keyType", "Key Type", FieldType.CODE, 3, default = "FFF", options = keyTypeOptions),
            f("zmk", "ZMK (under LMK)", FieldType.HEX, 0,
                default = "S10096K0TN00E000353C5E6C6C42DDCD48EA0E2EF6ED4D0CF81F71EFEA8E7C0DFF7A1E68F14B72082CA2265868C04F806",
                desc = "Zone Master Key encrypted under LMK (S-block or hex)"),
            f("key", "Key under ZMK", FieldType.HEX, 0,
                default = "S00072P0TB00E00FF5C6A503F02A1E535507F981CE9A790B648896D0698A3CDAEF2AE5133",
                desc = "Key to import, encrypted under the ZMK (S-block or hex)"),
            f("keyScheme", "Key Scheme (LMK)", FieldType.CODE, 1, default = "S", options = keySchemeOptions),
            f("kcvType", "Key Check Value Type", FieldType.CODE, 1, req = FieldRequirement.OPTIONAL,
                options = kcvTypeOptions),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL,
                default = "%03", options = lmkIdentifierOptions,
                desc = "Alternate LMK pair (%nn)"),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("keyUnderLMK", "Key under LMK", FieldType.HEX, 0),
            f("kcv", "Key Check Value", FieldType.HEX, 6),
        ),
    ),

    ThalesCommandDefinition(
        code = "A8", responseCode = "A9", name = "Export a Key",
        description = "Export a key under LMK, re-encrypt it under a ZMK/TMK for transport",
        category = CommandCategory.KEY_MANAGEMENT,
        requestFields = listOf(
            f("keyType", "Key Type", FieldType.CODE, 3, default = "FFF", options = keyTypeOptions,
                desc = "Key type code defining the kind of key. FFF = KeyBlock (LMK pair derived from S-block key usage). Other codes (001=ZPK, 002=TPK, etc.) use fixed LMK pairs."),
            f("delimiter1", "Delimiter", FieldType.FLAG, 0, req = FieldRequirement.OPTIONAL,
                default = ";", desc = "; delimiter before ZMK/TMK Flag"),
            f("zmkTmkFlag", "ZMK/TMK Flag", FieldType.CODE, 1, default = "0",
                options = listOf(CodeOption("0", "0 - ZMK"), CodeOption("1", "1 - TMK")),
                cond = FieldCondition("delimiter1", setOf(";")),
                desc = "0 = Zone Master Key, 1 = Terminal Master Key"),
            f("zmk", "ZMK/TMK (under LMK)", FieldType.HEX, 0,
                default = "S10096K0TN00E000353C5E6C6C42DDCD48EA0E2EF6ED4D0CF81F71EFEA8E7C0DFF7A1E68F14B72082CA2265868C04F806",
                desc = "ZMK/TMK encrypted under LMK. Used to encrypt the exported key. When export scheme is S, this key is used directly as KBPK (no derivation). For FFF key type, the S-block header determines the LMK pair."),
            f("key", "Key to Export (under LMK)", FieldType.HEX, 0,
                default = "S10096P0TB00E0003DBDDDC2ECFE9A09FBAE85D30BE33F2C3A36F90F9376D51B08107C94589829CB0EF854682CA6FA90A",
                desc = "The key to export, encrypted under LMK. For FFF key type, this should be an S-block key. The clear key is decrypted from LMK and re-encrypted under the ZMK/TMK using the export scheme."),
            f("keyScheme", "Key Scheme (output format)", FieldType.CODE, 1, default = "S", options = keySchemeOptions,
                desc = "Encryption scheme for the output key under ZMK/TMK. S = KeyBlock (ZMK used as KBPK), U = single-length, T = double-length, X = triple-length."),
            f("atallaVariant", "Atalla variant", FieldType.ASCII, 0, req = FieldRequirement.OPTIONAL,
                desc = "Optional Atalla variant string"),
            f("lmkDelimiter", "Delimiter", FieldType.FLAG, 0, req = FieldRequirement.OPTIONAL,
                default = "%", desc = "Prefix % before 2-digit LMK Identifier"),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 2, req = FieldRequirement.OPTIONAL,
                default = "03", options = lmkPairIdOptions,
                conds = listOf(FieldCondition("lmkDelimiter", setOf("%"))),
                desc = "LMK pair ID for identifying which LMK set to use (e.g. 03)"),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("keyUnderZMK", "Key under ZMK", FieldType.HEX, 0),
            f("kcv", "Key Check Value", FieldType.HEX, 6),
        ),
    ),

    ThalesCommandDefinition(
        code = "AE", responseCode = "AF", name = "Translate TMK/TPK/PVK (LMK to TMK)",
        description = "Translate a TMK, TPK or PVK from LMK to another TMK/TPK/PVK",
        category = CommandCategory.KEY_MANAGEMENT,
        requestFields = listOf(
            f("sourceTMK", "Source TMK", FieldType.HEX, 0, desc = "Source TMK/TPK/PVK under LMK"),
            f("destTMK", "Destination TMK", FieldType.HEX, 0, desc = "Destination TMK/TPK/PVK under LMK"),
            f("keyUnderSourceTMK", "Key under Source TMK", FieldType.HEX, 0),
            f("keySchemeTMK", "Key Scheme (TMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("keySchemeLMK", "Key Scheme (LMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("kcvType", "Key Check Value Type", FieldType.CODE, 1, FieldRequirement.OPTIONAL, options = kcvTypeOptions),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("keyUnderDestTMK", "Key under Dest TMK", FieldType.HEX, 0),
            f("keyUnderLMK", "Key under LMK", FieldType.HEX, 0),
            f("kcv", "Key Check Value", FieldType.HEX, 6),
        ),
    ),

    ThalesCommandDefinition(
        code = "AG", responseCode = "AH", name = "Translate TAK (LMK to TMK)",
        description = "Translate a TAK from LMK to TMK encryption",
        category = CommandCategory.KEY_MANAGEMENT,
        requestFields = listOf(
            f("tmk", "TMK", FieldType.HEX, 0, desc = "TMK under LMK"),
            f("tak", "TAK", FieldType.HEX, 0, desc = "TAK under LMK"),
            f("keySchemeTMK", "Key Scheme (TMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("kcvType", "Key Check Value Type", FieldType.CODE, 1, FieldRequirement.OPTIONAL, options = kcvTypeOptions),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("takUnderTMK", "TAK under TMK", FieldType.HEX, 0),
            f("kcv", "Key Check Value", FieldType.HEX, 6),
        ),
    ),

    ThalesCommandDefinition(
        code = "AQ", responseCode = "AR", name = "Translate RSA-encrypted PIN",
        description = "Translate an RSA-encrypted PIN to ZPK or TPK encryption",
        category = CommandCategory.PIN_OPERATIONS,
        requestFields = listOf(
            f("flag", "Flag", FieldType.DEC, 2, default = "01"),
            f("zpkTpk", "ZPK/TPK", FieldType.HEX, 0, desc = "ZPK or TPK under LMK"),
            f("rsaPrivKeyIndex", "RSA Private Key Index", FieldType.DEC, 2, default = "00"),
            f("pinBlock", "Encrypted PIN Block", FieldType.HEX, 0),
            f("destPinBlockFmt", "Dest PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("accountNumber", "Account Number", FieldType.DEC, 12),
            f("keySpecFlag", "Key Specifier Flag", FieldType.FLAG, 1, default = "0"),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("pinBlock", "PIN Block", FieldType.HEX, 16),
        ),
    ),

    ThalesCommandDefinition(
        code = "AU", responseCode = "AV", name = "Translate CVK (LMK to ZMK)",
        description = "Translate a CVK pair from LMK to ZMK encryption",
        category = CommandCategory.KEY_MANAGEMENT,
        requestFields = listOf(
            f("zmk", "ZMK", FieldType.HEX, 0, desc = "ZMK under LMK"),
            f("cvkPair", "CVK Pair", FieldType.HEX, 0, desc = "CVK pair under LMK"),
            f("keySchemeZMK", "Key Scheme (ZMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("kcvType", "Key Check Value Type", FieldType.CODE, 1, FieldRequirement.OPTIONAL, options = kcvTypeOptions),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("cvkUnderZMK", "CVK Pair under ZMK", FieldType.HEX, 0),
            f("kcv", "Key Check Value", FieldType.HEX, 6),
        ),
    ),

    ThalesCommandDefinition(
        code = "AW", responseCode = "AX", name = "Translate CVK (ZMK to LMK)",
        description = "Translate a CVK pair from ZMK to LMK encryption",
        category = CommandCategory.KEY_MANAGEMENT,
        requestFields = listOf(
            f("zmk", "ZMK", FieldType.HEX, 0, desc = "ZMK under LMK"),
            f("cvkUnderZMK", "CVK Pair under ZMK", FieldType.HEX, 0, desc = "CVK pair under ZMK"),
            f("keySchemeLMK", "Key Scheme (LMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("kcvType", "Key Check Value Type", FieldType.CODE, 1, FieldRequirement.OPTIONAL, options = kcvTypeOptions),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("cvkUnderLMK", "CVK Pair under LMK", FieldType.HEX, 0),
            f("kcv", "Key Check Value", FieldType.HEX, 6),
        ),
    ),

    ThalesCommandDefinition(
        code = "B0", responseCode = "B1", name = "Translate Key Scheme",
        description = "Translate a key from one LMK scheme to another",
        category = CommandCategory.KEY_MANAGEMENT,
        requestFields = listOf(
            f("keyType", "Key Type", FieldType.CODE, 3, default = "001", options = keyTypeOptions),
            f("key", "Key under LMK", FieldType.HEX, 0),
            f("keyScheme", "Key Scheme (LMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("kcvType", "Key Check Value Type", FieldType.CODE, 1, FieldRequirement.OPTIONAL, options = kcvTypeOptions),
        ) + keyBlockFields(defaultUsage = "51", defaultModeOfUse = "E", defaultExportability = "S"),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("keyUnderLMK", "Key under LMK (new scheme)", FieldType.HEX, 0),
            f("kcv", "Key Check Value", FieldType.HEX, 6),
        ),
    ),

    ThalesCommandDefinition(
        code = "BI", responseCode = "BJ", name = "Generate a BDK",
        description = "Generate a BDK and encrypt under LMK pair 28-29.",
        category = CommandCategory.KEY_MANAGEMENT,
        requestFields = listOf(
            f("leadingDelimiter", "Delimiter", FieldType.ASCII, 1, default = ";",
                desc = "First separator (e.g. ; before ZMK/LMK fields)"),
            f("keySchemeZmk", "Key Scheme ZMK", FieldType.CODE, 1, default = "0", options = biKeySchemeZmkOptions),
            f("keySchemeLmk", "Key Scheme LMK", FieldType.CODE, 1, default = "T", options = keySchemeOptions),
            f("kcvType", "Key Check Value Type", FieldType.CODE, 1, default = "0", options = kcvTypeOptions),
            f("lmkDelimiter", "Delimiter", FieldType.FLAG, 0, req = FieldRequirement.OPTIONAL,
                default = "%", desc = "Prefix % before LMK pair identifier"),
            f("lmkPairId", "LMK Identifier", FieldType.CODE, 2, req = FieldRequirement.OPTIONAL,
                default = "03", options = lmkPairIdOptions,
                cond = FieldCondition("lmkDelimiter", setOf("%"))),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("bdkUnderLMK", "BDK under LMK", FieldType.HEX, 0),
            f("kcv", "Key Check Value", FieldType.HEX, 6),
        ),
        forceVerticalFieldLayout = true,
    ),

    ThalesCommandDefinition(
        code = "BS", responseCode = "BT", name = "Erase Key Change Storage",
        description = "Erase the key change storage area",
        category = CommandCategory.KEY_MANAGEMENT,
        requestFields = emptyList(),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
        ),
    ),

    ThalesCommandDefinition(
        code = "BU", responseCode = "BV", name = "Generate Key Check Value",
        description = "Generate a check value for a key encrypted under LMK",
        category = CommandCategory.KEY_MANAGEMENT,
        requestFields = listOf(
            f("keyTypeCode", "Key Type Code", FieldType.CODE, 2, default = "00", options = kaKeyTypeCodeOptions),
            f("keyLengthFlag", "Key Length Flag", FieldType.CODE, 1, default = "1", options = kaKeyLengthFlagOptions),
            f("key", "Key", FieldType.HEX, 0),
            f("delimiter0", "Delimiter", FieldType.FLAG, 0, req = FieldRequirement.OPTIONAL, default = ";",
                desc = "; separator before Key Type"),
            f("keyType", "Key Type", FieldType.CODE, 3, default = "000", options = keyTypeOptions,
                cond = FieldCondition("delimiter0", setOf(";"))),
            f("delimiter1", "Delimiter", FieldType.FLAG, 0, req = FieldRequirement.OPTIONAL, default = ";",
                desc = "; separator before Reserved fields"),
            f("reserved1", "Reserved", FieldType.CODE, 1, req = FieldRequirement.OPTIONAL, default = "0",
                options = reservedZeroOptions, cond = FieldCondition("delimiter1", setOf(";"))),
            f("reserved2", "Reserved", FieldType.CODE, 1, req = FieldRequirement.OPTIONAL, default = "0",
                options = reservedZeroOptions, cond = FieldCondition("delimiter1", setOf(";"))),
            f("kcvType", "Key Check Value Type", FieldType.CODE, 1, req = FieldRequirement.OPTIONAL,
                default = "1", options = kaKcvTypeOptions, cond = FieldCondition("delimiter1", setOf(";"))),
            f("delimiter2", "Delimiter", FieldType.FLAG, 0, req = FieldRequirement.OPTIONAL, default = "%",
                desc = "% separator before LMK Identifier"),
            f("lmkIdentifier", "LMK Identifier", FieldType.ASCII, 2, req = FieldRequirement.OPTIONAL, default = "00",
                cond = FieldCondition("delimiter2", setOf("%"))),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("kcv", "Key Check Value", FieldType.HEX, 0),
        ),
    ),

    ThalesCommandDefinition(
        code = "BW", responseCode = "BX", name = "Translate Keys (Old LMK to New LMK)",
        description = "Translate keys from old LMK to new LMK and migrate to new key type",
        category = CommandCategory.KEY_MANAGEMENT,
        requestFields = listOf(
            f("keyTypeCode", "Key Type Code (2-digit)", FieldType.DEC, 2, default = "00"),
            f("keyLengthFlag", "Key Length Flag", FieldType.CODE, 1, default = "1", options = listOf(
                CodeOption("0", "0 - Single length"), CodeOption("1", "1 - Double length"), CodeOption("2", "2 - Triple length"))),
            f("key", "Key under Old LMK", FieldType.HEX, 0),
            f("keyType", "Key Type (3-digit)", FieldType.CODE, 3, default = "FFF", options = keyTypeOptions,
                desc = "Preceded by ; delimiter"),
            f("keySchemeLmk", "Key Scheme (LMK)", FieldType.CODE, 1, default = "S", options = keySchemeOptions,
                desc = "Preceded by ; and '0' reserved byte"),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("keyUnderNewLMK", "Key under New LMK", FieldType.HEX, 0),
        ),
    ),

    ThalesCommandDefinition(
        code = "FA", responseCode = "FB", name = "Translate ZPK (ZMK to LMK)",
        description = "Translate a ZPK from ZMK to LMK encryption",
        category = CommandCategory.KEY_MANAGEMENT,
        requestFields = listOf(
            f("zmk", "ZMK", FieldType.HEX, 0),
            f("zpkUnderZMK", "ZPK under ZMK", FieldType.HEX, 0),
            f("keySchemeLMK", "Key Scheme (LMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("atallaVariant", "Atalla Variant", FieldType.DEC, 1, FieldRequirement.OPTIONAL),
            f("kcvType", "Key Check Value Type", FieldType.CODE, 1, FieldRequirement.OPTIONAL, options = kcvTypeOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("zpkUnderLMK", "ZPK under LMK", FieldType.HEX, 0),
            f("kcv", "Key Check Value", FieldType.HEX, 6),
        ),
    ),

    ThalesCommandDefinition(
        code = "FC", responseCode = "FD", name = "Translate TMK/TPK/PVK (ZMK to LMK)",
        description = "Translate a TMK, TPK or PVK from ZMK to LMK encryption",
        category = CommandCategory.KEY_MANAGEMENT,
        requestFields = listOf(
            f("zmk", "ZMK", FieldType.HEX, 0),
            f("keyUnderZMK", "Key under ZMK", FieldType.HEX, 0),
            f("keySchemeLMK", "Key Scheme (LMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("atallaVariant", "Atalla Variant", FieldType.DEC, 1, FieldRequirement.OPTIONAL),
            f("kcvType", "Key Check Value Type", FieldType.CODE, 1, FieldRequirement.OPTIONAL, options = kcvTypeOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("keyUnderLMK", "Key under LMK", FieldType.HEX, 0),
            f("kcv", "Key Check Value", FieldType.HEX, 6),
        ),
    ),

    ThalesCommandDefinition(
        code = "FE", responseCode = "FF", name = "Translate TMK/TPK/PVK (LMK to ZMK)",
        description = "Translate a TMK, TPK or PVK from LMK to ZMK encryption",
        category = CommandCategory.KEY_MANAGEMENT,
        requestFields = listOf(
            f("zmk", "ZMK", FieldType.HEX, 0),
            f("keyUnderLMK", "Key under LMK", FieldType.HEX, 0),
            f("keySchemeZMK", "Key Scheme (ZMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("atallaVariant", "Atalla Variant", FieldType.DEC, 1, FieldRequirement.OPTIONAL),
            f("kcvType", "Key Check Value Type", FieldType.CODE, 1, FieldRequirement.OPTIONAL, options = kcvTypeOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("keyUnderZMK", "Key under ZMK", FieldType.HEX, 0),
            f("kcv", "Key Check Value", FieldType.HEX, 6),
        ),
    ),

    ThalesCommandDefinition(
        code = "FG", responseCode = "FH", name = "Generate Pair of PVKs",
        description = "Generate a pair of PVKs encrypted under LMK",
        category = CommandCategory.KEY_MANAGEMENT,
        requestFields = listOf(
            f("keyScheme", "Key Scheme (LMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("kcvType", "Key Check Value Type", FieldType.CODE, 1, FieldRequirement.OPTIONAL, options = kcvTypeOptions),
        ) + keyBlockFields(defaultUsage = "V2", defaultModeOfUse = "C", defaultExportability = "S"),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("pvkPair", "PVK Pair under LMK", FieldType.HEX, 0),
            f("kcv", "Key Check Value", FieldType.HEX, 6),
        ),
    ),

    ThalesCommandDefinition(
        code = "FI", responseCode = "FJ", name = "Generate ZEK/ZAK",
        description = "Generate a ZEK or ZAK",
        category = CommandCategory.KEY_MANAGEMENT,
        requestFields = listOf(
            f("flag", "Flag", FieldType.CODE, 1, default = "0", options = listOf(
                CodeOption("0", "0 - ZEK"), CodeOption("1", "1 - ZAK"))),
            f("zmk", "ZMK", FieldType.HEX, 0),
            f("keySchemeZMK", "Key Scheme (ZMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("keySchemeLMK", "Key Scheme (LMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("atallaVariant", "Atalla Variant", FieldType.DEC, 1, FieldRequirement.OPTIONAL),
            f("kcvType", "Key Check Value Type", FieldType.CODE, 1, FieldRequirement.OPTIONAL, options = kcvTypeOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("keyUnderZMK", "Key under ZMK", FieldType.HEX, 0),
            f("keyUnderLMK", "Key under LMK", FieldType.HEX, 0),
            f("kcv", "Key Check Value", FieldType.HEX, 6),
        ),
    ),

    ThalesCommandDefinition(
        code = "FK", responseCode = "FL", name = "Translate ZEK/ZAK (ZMK to LMK)",
        description = "Translate a ZEK/ZAK from ZMK to LMK encryption",
        category = CommandCategory.KEY_MANAGEMENT,
        requestFields = listOf(
            f("flag", "Flag", FieldType.CODE, 1, default = "0", options = listOf(
                CodeOption("0", "0 - ZEK"), CodeOption("1", "1 - ZAK"))),
            f("zmk", "ZMK", FieldType.HEX, 0),
            f("keyUnderZMK", "Key under ZMK", FieldType.HEX, 0),
            f("keySchemeLMK", "Key Scheme (LMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("atallaVariant", "Atalla Variant", FieldType.DEC, 1, FieldRequirement.OPTIONAL),
            f("kcvType", "Key Check Value Type", FieldType.CODE, 1, FieldRequirement.OPTIONAL, options = kcvTypeOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("keyUnderLMK", "Key under LMK", FieldType.HEX, 0),
            f("kcv", "Key Check Value", FieldType.HEX, 6),
        ),
    ),

    ThalesCommandDefinition(
        code = "FM", responseCode = "FN", name = "Translate ZEK/ZAK (LMK to ZMK)",
        description = "Translate a ZEK/ZAK from LMK to ZMK encryption",
        category = CommandCategory.KEY_MANAGEMENT,
        requestFields = listOf(
            f("flag", "Flag", FieldType.CODE, 1, default = "0", options = listOf(
                CodeOption("0", "0 - ZEK"), CodeOption("1", "1 - ZAK"))),
            f("zmk", "ZMK", FieldType.HEX, 0),
            f("zek", "ZEK under LMK", FieldType.HEX, 0, desc = "ZEK key under LMK"),
            f("zak", "ZAK under LMK", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL, desc = "ZAK key under LMK"),
            f("atallaVariant", "Atalla Variant", FieldType.ASCII, 0, req = FieldRequirement.OPTIONAL),
            f("delimiter", "Delimiter", FieldType.ASCII, 0, req = FieldRequirement.OPTIONAL),
            f("keySchemeZmk", "Key Scheme (ZMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("reserved", "Reserved", FieldType.ASCII, 0, req = FieldRequirement.OPTIONAL),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("keyUnderZMK", "Key under ZMK", FieldType.HEX, 0),
            f("kcv", "Key Check Value", FieldType.HEX, 6),
        ),
    ),

    ThalesCommandDefinition(
        code = "FQ", responseCode = "FR", name = "Translate WWK (LMK to ZMK)",
        description = "Translate a WWK from LMK to ZMK encryption",
        category = CommandCategory.KEY_MANAGEMENT,
        requestFields = listOf(
            f("zmk", "ZMK", FieldType.HEX, 0),
            f("wwkUnderLMK", "WWK under LMK", FieldType.HEX, 0),
            f("keySchemeZMK", "Key Scheme (ZMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("wwkUnderZMK", "WWK under ZMK", FieldType.HEX, 0),
            f("kcv", "Key Check Value", FieldType.HEX, 6),
        ),
    ),

    ThalesCommandDefinition(
        code = "FS", responseCode = "FT", name = "Translate WWK (ZMK to LMK)",
        description = "Translate a WWK from ZMK to LMK encryption",
        category = CommandCategory.KEY_MANAGEMENT,
        requestFields = listOf(
            f("zmk", "ZMK", FieldType.HEX, 0),
            f("wwkUnderZMK", "WWK under ZMK", FieldType.HEX, 0),
            f("keySchemeLMK", "Key Scheme (LMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("wwkUnderLMK", "WWK under LMK", FieldType.HEX, 0),
            f("kcv", "Key Check Value", FieldType.HEX, 6),
        ),
    ),

    ThalesCommandDefinition(
        code = "GC", responseCode = "GD", name = "Translate ZPK (LMK to ZMK)",
        description = "Translate a ZPK from LMK to ZMK encryption",
        category = CommandCategory.KEY_MANAGEMENT,
        requestFields = listOf(
            f("zmk", "ZMK", FieldType.HEX, 0),
            f("zpkUnderLMK", "ZPK under LMK", FieldType.HEX, 0),
            f("keySchemeZMK", "Key Scheme (ZMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("atallaVariant", "Atalla Variant", FieldType.DEC, 1, FieldRequirement.OPTIONAL),
            f("kcvType", "Key Check Value Type", FieldType.CODE, 1, FieldRequirement.OPTIONAL, options = kcvTypeOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("zpkUnderZMK", "ZPK under ZMK", FieldType.HEX, 0),
            f("kcv", "Key Check Value", FieldType.HEX, 6),
        ),
    ),

    ThalesCommandDefinition(
        code = "GY", responseCode = "GZ", name = "Form ZMK from Components",
        description = "Form a ZMK from 2 to 9 encrypted components",
        category = CommandCategory.KEY_MANAGEMENT,
        requestFields = listOf(
            f("numComponents", "Number of Components", FieldType.DEC, 1, default = "2"),
            f("keyScheme", "Key Scheme", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("component1", "Component 1", FieldType.HEX, 0),
            f("component2", "Component 2", FieldType.HEX, 0),
            f("component3", "Component 3", FieldType.HEX, 0, FieldRequirement.OPTIONAL),
        ) + keyBlockFields(defaultUsage = "K0", defaultModeOfUse = "B", defaultExportability = "S"),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("zmkUnderLMK", "ZMK under LMK", FieldType.HEX, 0),
            f("kcv", "Key Check Value", FieldType.HEX, 6),
        ),
    ),

    ThalesCommandDefinition(
        code = "HA", responseCode = "HB", name = "Generate a TAK",
        description = "Generate a random TAK",
        category = CommandCategory.TERMINAL_KEYS,
        requestFields = listOf(
            f("tmk", "TMK", FieldType.HEX, 0),
            f("keySchemeTMK", "Key Scheme (TMK)", FieldType.CODE, 1, default = "Z", options = keySchemeOptions),
            f("keySchemeLMK", "Key Scheme (LMK)", FieldType.CODE, 1, default = "Z", options = keySchemeOptions),
            f("kcvType", "Key Check Value Type", FieldType.CODE, 1, FieldRequirement.OPTIONAL, options = kcvTypeOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("takUnderTMK", "TAK under TMK", FieldType.HEX, 0),
            f("takUnderLMK", "TAK under LMK", FieldType.HEX, 0),
            f("kcv", "Key Check Value", FieldType.HEX, 6),
        ),
    ),

    ThalesCommandDefinition(
        code = "HC", responseCode = "HD", name = "Generate TMK/TPK/PVK",
        description = "Generate a random TMK, TPK or PVK",
        category = CommandCategory.TERMINAL_KEYS,
        requestFields = listOf(
            f("currentTmkOrTpkOrPvk", "Current TMK/TPK/PVK", FieldType.HEX, 0),
            f("keySchemeTmk", "Key Scheme (TMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("keySchemeLmk", "Key Scheme (LMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("kcvType", "Key Check Value Type", FieldType.CODE, 1, FieldRequirement.OPTIONAL, options = kcvTypeOptions),
            f("reserved", "Reserved", FieldType.ASCII, 1, req = FieldRequirement.OPTIONAL),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("keyUnderTMK", "Key under TMK", FieldType.HEX, 0),
            f("keyUnderLMK", "Key under LMK", FieldType.HEX, 0),
            f("kcv", "Key Check Value", FieldType.HEX, 6),
        ),
    ),

    ThalesCommandDefinition(
        code = "IA", responseCode = "IB", name = "Generate a ZPK",
        description = "Generate a random ZPK",
        category = CommandCategory.KEY_MANAGEMENT,
        requestFields = listOf(
            f("zmk", "ZMK", FieldType.HEX, 0),
            f("keySchemeZMK", "Key Scheme (ZMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("keySchemeLMK", "Key Scheme (LMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("atallaVariant", "Atalla Variant", FieldType.DEC, 1, FieldRequirement.OPTIONAL),
            f("kcvType", "Key Check Value Type", FieldType.CODE, 1, FieldRequirement.OPTIONAL, options = kcvTypeOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("zpkUnderZMK", "ZPK under ZMK", FieldType.HEX, 0),
            f("zpkUnderLMK", "ZPK under LMK", FieldType.HEX, 0),
            f("kcv", "Key Check Value", FieldType.HEX, 6),
        ),
    ),

    ThalesCommandDefinition(
        code = "KA", responseCode = "KB", name = "Generate KCV",
        description = "Generate a key check value (not double-length ZMK)",
        category = CommandCategory.KEY_MANAGEMENT,
        requestFields = listOf(
            f("key", "KEY", FieldType.HEX, 0),
            f("keyType", "Key Type", FieldType.CODE, 2, default = "00", options = kaKeyTypeOptions),
            f("delimiter1", "Delimiter", FieldType.FLAG, 0, req = FieldRequirement.OPTIONAL, default = ";",
                desc = "; separator before Reserved fields"),
            f("reserved1", "Reserved", FieldType.CODE, 1, req = FieldRequirement.OPTIONAL, default = "0",
                options = reservedZeroOptions, cond = FieldCondition("delimiter1", setOf(";"))),
            f("reserved2", "Reserved", FieldType.CODE, 1, req = FieldRequirement.OPTIONAL, default = "0",
                options = reservedZeroOptions, cond = FieldCondition("delimiter1", setOf(";"))),
            f("kcvType", "Key Check Value Type", FieldType.CODE, 1, req = FieldRequirement.OPTIONAL,
                default = "1", options = kaKcvTypeOptions, cond = FieldCondition("delimiter1", setOf(";"))),
            f("delimiter2", "Delimiter", FieldType.FLAG, 0, req = FieldRequirement.OPTIONAL, default = "%",
                desc = "% separator before LMK Identifier"),
            f("lmkIdentifier", "LMK Identifier", FieldType.ASCII, 2, req = FieldRequirement.OPTIONAL, default = "00",
                cond = FieldCondition("delimiter2", setOf("%"))),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("kcv", "Key Check Value", FieldType.HEX, 6),
        ),
    ),

    // ==================== PIN OPERATIONS ====================

    ThalesCommandDefinition(
        code = "BA", responseCode = "BB", name = "Encrypt a Clear PIN",
        description = "Encrypt a clear PIN under LMK",
        category = CommandCategory.PIN_OPERATIONS,
        requestFields = listOf(
            f("clearPIN", "Clear PIN", FieldType.ASCII, 0, default = "1234"),
            f("accountNumber", "Account Number", FieldType.DEC, 12, default = "123456789012"),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("pinUnderLMK", "PIN under LMK", FieldType.HEX, 0),
        ),
    ),

    ThalesCommandDefinition(
        code = "BC", responseCode = "BD", name = "Verify Terminal PIN (Comparison)",
        description = "Verify a terminal PIN using the comparison method",
        category = CommandCategory.PIN_OPERATIONS,
        requestFields = listOf(
            f("tpk", "TPK", FieldType.HEX, 0),
            f("pinBlock", "PIN Block", FieldType.HEX, 16),
            f("pinBlockFmt", "PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("accountNumber", "Account Number", FieldType.DEC, 12),
            f("pinUnderLMK", "PIN under LMK", FieldType.HEX, 0),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
        ),
    ),

    ThalesCommandDefinition(
        code = "BE", responseCode = "BF", name = "Verify Interchange PIN (Comparison)",
        description = "Verify an interchange PIN using the comparison method",
        category = CommandCategory.PIN_OPERATIONS,
        requestFields = listOf(
            f("zpk", "ZPK", FieldType.HEX, 0),
            f("pinBlock", "PIN Block", FieldType.HEX, 16),
            f("pinBlockFmt", "PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("pan", "PAN", FieldType.DEC, 12),
            f("pin", "PIN under LMK", FieldType.HEX, 0),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
        ),
    ),

    ThalesCommandDefinition(
        code = "BG", responseCode = "BH", name = "Translate PIN and PIN Length",
        description = "Translate a PIN from one LMK-encrypted form to another (change account number)",
        category = CommandCategory.PIN_OPERATIONS,
        requestFields = listOf(
            f("account", "Account Number", FieldType.DEC, 12),
            f("pin", "PIN under LMK", FieldType.HEX, 0),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("pin", "PIN under LMK", FieldType.HEX, 0),
        ),
    ),

    ThalesCommandDefinition(
        code = "BK", responseCode = "BL", name = "Generate IBM PIN Offset (Customer)",
        description = "Generate an IBM PIN offset of a customer selected PIN",
        category = CommandCategory.PIN_OPERATIONS,
        requestFields = listOf(
            f("keyType", "Key Type", FieldType.CODE, 3, default = "FFF", options = keyTypeOptions),
            f("key", "Key (TPK/ZPK)", FieldType.HEX, 0, desc = "PIN encryption key under LMK"),
            f("pvk", "PVK Pair", FieldType.HEX, 0),
            f("pinBlock", "PIN Block", FieldType.HEX, 16),
            f("pinBlockFormat", "PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("checkLength", "Check Length", FieldType.DEC, 2, default = "04"),
            f("pan", "PAN (Account Number)", FieldType.DEC, 12),
            f("decimalisationTable", "Decimalisation Table", FieldType.HEX, 16),
            f("pinValidationData", "PIN Validation Data", FieldType.ASCII, 12),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("offset", "Offset", FieldType.ASCII, 12),
        ),
    ),

    ThalesCommandDefinition(
        code = "CA", responseCode = "CB", name = "Translate PIN (TPK to ZPK/BDK)",
        description = "Translate a PIN from TPK to ZPK/BDK (3DES DUKPT) encryption",
        category = CommandCategory.PIN_OPERATIONS,
        requestFields = listOf(
            f("sourceTpk", "Source TPK", FieldType.HEX, 0),
            f("destinationKeyFlag", "Destination Key Flag", FieldType.CODE, 0,
                req = FieldRequirement.OPTIONAL, options = listOf(
                    CodeOption("", "(none) - ZPK"),
                    CodeOption("*", "* - DUKPT BDK")),
                desc = "Omit for ZPK, * for DUKPT BDK"),
            f("destinationKey", "Destination Key (ZPK/BDK)", FieldType.HEX, 0),
            f("destinationKsnDescriptor", "Dest KSN Descriptor", FieldType.HEX, 3,
                req = FieldRequirement.OPTIONAL, desc = "Required for DUKPT destination"),
            f("destinationKsn", "Destination KSN", FieldType.HEX, 0,
                req = FieldRequirement.OPTIONAL, desc = "Required for DUKPT destination"),
            f("maximumPinLength", "Max PIN Length", FieldType.DEC, 2, default = "12"),
            f("sourcePinBlock", "Source PIN Block", FieldType.HEX, 16),
            f("sourcePinBlockFormat", "Source PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("destinationPinBlockFormat", "Dest PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("pan", "PAN (Account Number)", FieldType.DEC, 12),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("checkLength", "PIN Length", FieldType.DEC, 2),
            f("destinationPinBlock", "Destination PIN Block", FieldType.HEX, 16),
            f("destinationPinBlockFormat", "Dest PIN Block Format", FieldType.DEC, 2),
        ),
    ),

    ThalesCommandDefinition(
        code = "CC", responseCode = "CD", name = "Translate PIN (ZPK to ZPK)",
        description = "Translate a PIN from one ZPK to another",
        category = CommandCategory.PIN_OPERATIONS,
        requestFields = listOf(
            f("sourceZpk", "Source ZPK", FieldType.HEX, 0),
            f("destinationZpk", "Destination ZPK", FieldType.HEX, 0),
            f("maximumPinLength", "Max PIN Length", FieldType.DEC, 2, default = "12"),
            f("sourcePinBlock", "Source PIN Block", FieldType.HEX, 16),
            f("sourcePinBlockFormat", "Source PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("destinationPinBlockFormat", "Dest PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("pan", "PAN (Account Number)", FieldType.DEC, 12),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("checkLength", "PIN Length", FieldType.DEC, 2),
            f("pinBlock", "Destination PIN Block", FieldType.HEX, 16),
            f("pinBlockFormat", "Dest PIN Block Format", FieldType.DEC, 2),
        ),
    ),

    ThalesCommandDefinition(
        code = "CG", responseCode = "CH", name = "Verify Terminal PIN (Diebold)",
        description = "Verify a terminal PIN using the Diebold method",
        category = CommandCategory.PIN_OPERATIONS,
        requestFields = listOf(
            f("tpk", "TPK", FieldType.HEX, 0),
            f("pvk", "PVK", FieldType.HEX, 0),
            f("pinBlock", "PIN Block", FieldType.HEX, 16),
            f("pinBlockFmt", "PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("accountNumber", "Account Number", FieldType.DEC, 12),
            f("decTable", "Decimalization Table", FieldType.HEX, 16),
            f("pinValData", "PIN Validation Data", FieldType.ASCII, 12),
            f("offset", "Offset", FieldType.ASCII, 12),
        ),
        responseFields = listOf(f("errorCode", "Error Code", FieldType.DEC, 2)),
    ),

    ThalesCommandDefinition(
        code = "CI", responseCode = "CJ", name = "Translate PIN (BDK to ZPK - DUKPT)",
        description = "Translate a PIN from BDK to ZPK using DUKPT",
        category = CommandCategory.DUKPT,
        requestFields = listOf(
            f("bdk", "BDK", FieldType.HEX, 0),
            f("zpk", "ZPK", FieldType.HEX, 0),
            f("ksn", "KSN", FieldType.HEX, 20),
            f("pinBlock", "PIN Block", FieldType.HEX, 16),
            f("srcPinBlockFmt", "Source PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("destPinBlockFmt", "Dest PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("accountNumber", "Account Number", FieldType.DEC, 12),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("pinLength", "PIN Length", FieldType.DEC, 2),
            f("pinBlock", "PIN Block", FieldType.HEX, 16),
            f("pinBlockFmt", "PIN Block Format", FieldType.DEC, 2),
        ),
    ),

    ThalesCommandDefinition(
        code = "CU", responseCode = "CV", name = "Verify & Generate VISA PVV",
        description = "Verify existing VISA PVV and generate PVV for customer selected PIN",
        category = CommandCategory.PIN_OPERATIONS,
        requestFields = listOf(
            f("keyType", "Key Type", FieldType.CODE, 3, default = "FFF", options = keyTypeOptions),
            f("key", "Key (TPK/ZPK)", FieldType.HEX, 0, desc = "PIN encryption key under LMK"),
            f("pvk", "PVK Pair", FieldType.HEX, 0),
            f("sourcePinBlock", "Source PIN Block (Current)", FieldType.HEX, 16),
            f("sourcePinBlockFormat", "Source PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("pan", "PAN (Account Number)", FieldType.DEC, 12),
            f("pvki", "PVKI", FieldType.DEC, 1, default = "1"),
            f("sourcePvv", "Source PVV", FieldType.DEC, 4),
            f("destinationPinBlock", "Dest PIN Block (New)", FieldType.HEX, 16),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("pvv", "New PVV", FieldType.DEC, 4),
        ),
    ),

    ThalesCommandDefinition(
        code = "DA", responseCode = "DB", name = "Verify Terminal PIN (IBM)",
        description = "Verify a terminal PIN using the IBM 3624 method",
        category = CommandCategory.PIN_OPERATIONS,
        requestFields = listOf(
            f("tpk", "TPK", FieldType.HEX, 0),
            f("pvk", "PVK Pair", FieldType.HEX, 0),
            f("maximumPinLength", "Max PIN Length", FieldType.DEC, 2, default = "12"),
            f("pinBlock", "PIN Block", FieldType.HEX, 16),
            f("pinBlockFormat", "PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("checkLength", "Check Length", FieldType.DEC, 2, default = "04"),
            f("pan", "PAN (Account Number)", FieldType.DEC, 12),
            f("decimalisationTable", "Decimalisation Table", FieldType.HEX, 16),
            f("pinValidationData", "PIN Validation Data", FieldType.ASCII, 12),
            f("offset", "Offset", FieldType.ASCII, 12),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(f("errorCode", "Error Code", FieldType.DEC, 2)),
    ),

    ThalesCommandDefinition(
        code = "DC", responseCode = "DD", name = "Verify Terminal PIN (VISA)",
        description = "Verify a terminal PIN using the VISA PVV method",
        category = CommandCategory.PIN_OPERATIONS,
        requestFields = listOf(
            f("tpk", "TPK", FieldType.HEX, 0),
            f("pvk", "PVK", FieldType.HEX, 0),
            f("pinBlock", "PIN Block", FieldType.HEX, 16),
            f("pinBlockFmt", "PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("accountNumber", "Account Number", FieldType.DEC, 12),
            f("pvki", "PVKI", FieldType.DEC, 1, default = "1"),
            f("pvv", "PVV", FieldType.DEC, 4),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(f("errorCode", "Error Code", FieldType.DEC, 2)),
    ),

    ThalesCommandDefinition(
        code = "DE", responseCode = "DF", name = "Generate IBM PIN Offset (LMK)",
        description = "Generate an IBM PIN offset of an LMK encrypted PIN",
        category = CommandCategory.PIN_OPERATIONS,
        requestFields = listOf(
            f("pvk", "PVK Pair", FieldType.HEX, 0),
            f("pin", "PIN under LMK", FieldType.HEX, 0),
            f("checkLength", "Check Length", FieldType.DEC, 2, default = "04"),
            f("account", "Account Number", FieldType.DEC, 12),
            f("decimalisationTable", "Decimalisation Table", FieldType.HEX, 16),
            f("pinValidationData", "PIN Validation Data", FieldType.ASCII, 12),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("offset", "Offset", FieldType.ASCII, 12),
        ),
    ),

    ThalesCommandDefinition(
        code = "DG", responseCode = "DH", name = "Generate VISA PVV",
        description = "Generate a VISA PVV of an LMK encrypted PIN",
        category = CommandCategory.PIN_OPERATIONS,
        requestFields = listOf(
            f("pvk", "PVK", FieldType.HEX, 0),
            f("pin", "PIN under LMK", FieldType.HEX, 0),
            f("accountNumber", "Account Number", FieldType.DEC, 12),
            f("pvki", "PVKI", FieldType.DEC, 1, default = "1"),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("pvv", "PVV", FieldType.DEC, 4),
        ),
    ),

    ThalesCommandDefinition(
        code = "DU", responseCode = "DV", name = "Verify & Generate IBM PIN Offset",
        description = "Verify and generate an IBM PIN offset of a customer selected PIN",
        category = CommandCategory.PIN_OPERATIONS,
        requestFields = listOf(
            f("keyType", "Key Type", FieldType.CODE, 3, default = "FFF", options = keyTypeOptions),
            f("key", "Key (TPK/ZPK)", FieldType.HEX, 0, desc = "PIN encryption key under LMK"),
            f("pvk", "PVK Pair", FieldType.HEX, 0),
            f("sourcePinBlock", "Source PIN Block", FieldType.HEX, 16),
            f("sourcePinBlockFormat", "Source PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("checkLength", "Check Length", FieldType.DEC, 2, default = "04"),
            f("pan", "PAN (Account Number)", FieldType.DEC, 12),
            f("decimalisationTable", "Decimalisation Table", FieldType.HEX, 16),
            f("pinValidationData", "PIN Validation Data", FieldType.ASCII, 12),
            f("sourceOffset", "Source Offset", FieldType.ASCII, 12),
            f("destinationPinBlock", "Dest PIN Block (New)", FieldType.HEX, 16),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("offset", "New Offset", FieldType.ASCII, 12),
        ),
    ),

    ThalesCommandDefinition(
        code = "DW", responseCode = "DX", name = "Translate BDK (ZMK to LMK)",
        description = "Translate a BDK from ZMK to LMK encryption",
        category = CommandCategory.DUKPT,
        requestFields = listOf(
            f("zmk", "ZMK", FieldType.HEX, 0),
            f("bdkUnderZMK", "BDK under ZMK", FieldType.HEX, 0),
            f("keySchemeLMK", "Key Scheme (LMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("bdkUnderLMK", "BDK under LMK", FieldType.HEX, 0),
            f("kcv", "Key Check Value", FieldType.HEX, 6),
        ),
    ),

    ThalesCommandDefinition(
        code = "DY", responseCode = "DZ", name = "Translate BDK (LMK to ZMK)",
        description = "Translate a BDK from LMK to ZMK encryption",
        category = CommandCategory.DUKPT,
        requestFields = listOf(
            f("zmk", "ZMK", FieldType.HEX, 0),
            f("bdkUnderLMK", "BDK under LMK", FieldType.HEX, 0),
            f("keySchemeZMK", "Key Scheme (ZMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("bdkUnderZMK", "BDK under ZMK", FieldType.HEX, 0),
            f("kcv", "Key Check Value", FieldType.HEX, 6),
        ),
    ),

    ThalesCommandDefinition(
        code = "EA", responseCode = "EB", name = "Verify Interchange PIN (IBM)",
        description = "Verify an interchange PIN using the IBM 3624 method",
        category = CommandCategory.PIN_OPERATIONS,
        requestFields = listOf(
            f("zpk", "ZPK", FieldType.HEX, 0),
            f("pvk", "PVK Pair", FieldType.HEX, 0),
            f("maximumPinLength", "Max PIN Length", FieldType.DEC, 2, default = "12"),
            f("pinBlock", "PIN Block", FieldType.HEX, 16),
            f("pinBlockFormat", "PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("checkLength", "Check Length", FieldType.DEC, 2, default = "04"),
            f("pan", "PAN (Account Number)", FieldType.DEC, 12),
            f("decimalisationTable", "Decimalisation Table", FieldType.HEX, 16),
            f("pinValidationData", "PIN Validation Data", FieldType.ASCII, 12),
            f("offset", "Offset", FieldType.ASCII, 12),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(f("errorCode", "Error Code", FieldType.DEC, 2)),
    ),

    ThalesCommandDefinition(
        code = "EC", responseCode = "ED", name = "Verify Interchange PIN (VISA)",
        description = "Verify an interchange PIN using the VISA PVV method",
        category = CommandCategory.PIN_OPERATIONS,
        requestFields = listOf(
            f("zpk", "ZPK", FieldType.HEX, 0),
            f("pvk", "PVK Pair", FieldType.HEX, 0),
            f("pinBlock", "PIN Block", FieldType.HEX, 16),
            f("pinBlockFormat", "PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("pan", "PAN (Account Number)", FieldType.DEC, 12),
            f("delimiter", "Delimiter", FieldType.ASCII, 1, req = FieldRequirement.OPTIONAL, default = ";",
                desc = "; delimiter before optional verification PAN"),
            f("verificationPan", "Verification PAN", FieldType.DEC, 0, req = FieldRequirement.OPTIONAL,
                desc = "Optional different PAN for PVV verification"),
            f("pvki", "PVKI", FieldType.DEC, 1, default = "1"),
            f("pvv", "PVV", FieldType.DEC, 4),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(f("errorCode", "Error Code", FieldType.DEC, 2)),
    ),

    ThalesCommandDefinition(
        code = "EE", responseCode = "EF", name = "Derive PIN (IBM Method)",
        description = "Derive a PIN using the IBM 3624 method",
        category = CommandCategory.PIN_OPERATIONS,
        requestFields = listOf(
            f("pvk", "PVK", FieldType.HEX, 0),
            f("offset", "Offset", FieldType.ASCII, 12),
            f("checkLength", "Check Length", FieldType.DEC, 2, default = "04"),
            f("account", "Account Number", FieldType.DEC, 12),
            f("decimalisationTable", "Decimalisation Table", FieldType.HEX, 16),
            f("pinValidationData", "PIN Validation Data", FieldType.ASCII, 12),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("pin", "PIN under LMK", FieldType.HEX, 0),
        ),
    ),

    ThalesCommandDefinition(
        code = "EG", responseCode = "EH", name = "Verify Interchange PIN (Diebold)",
        description = "Verify an interchange PIN using the Diebold method",
        category = CommandCategory.PIN_OPERATIONS,
        requestFields = listOf(
            f("zpk", "ZPK", FieldType.HEX, 0),
            f("pvk", "PVK", FieldType.HEX, 0),
            f("pinBlock", "PIN Block", FieldType.HEX, 16),
            f("pinBlockFmt", "PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("accountNumber", "Account Number", FieldType.DEC, 12),
            f("decTable", "Decimalization Table", FieldType.HEX, 16),
            f("pinValData", "PIN Validation Data", FieldType.ASCII, 12),
            f("offset", "Offset", FieldType.ASCII, 12),
        ),
        responseFields = listOf(f("errorCode", "Error Code", FieldType.DEC, 2)),
    ),

    ThalesCommandDefinition(
        code = "FW", responseCode = "FX", name = "Generate VISA PVV (Customer)",
        description = "Generate a VISA PVV of a customer selected PIN",
        category = CommandCategory.PIN_OPERATIONS,
        requestFields = listOf(
            f("keyType", "Key Type", FieldType.CODE, 3, default = "FFF", options = keyTypeOptions),
            f("key", "Key (TPK/ZPK)", FieldType.HEX, 0, desc = "PIN encryption key under LMK"),
            f("pvk", "PVK Pair", FieldType.HEX, 0),
            f("pinBlock", "PIN Block", FieldType.HEX, 16),
            f("pinBlockFormat", "PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("pan", "PAN (Account Number)", FieldType.DEC, 12),
            f("pvki", "PVKI", FieldType.DEC, 1, default = "1"),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("pvv", "PVV", FieldType.DEC, 4),
        ),
    ),

    ThalesCommandDefinition(
        code = "G0", responseCode = "G1", name = "Translate PIN (BDK to BDK/ZPK)",
        description = "Translate a PIN block from DUKPT BDK encryption to ZPK or another DUKPT BDK encryption (3DES DUKPT)",
        category = CommandCategory.PIN_OPERATIONS,
        requestFields = listOf(
            f("bdk", "BDK", FieldType.HEX, 0,
                desc = "Source Base Derivation Key encrypted under LMK pair 28-29. Scheme prefix (U/T/X) + key hex. Used with KSN to derive the DUKPT session key for decrypting the incoming PIN block."),
            f("destKeyType", "Destination Key Type", FieldType.CODE, 1, default = "0",
                options = listOf(
                    CodeOption("0", "'0' - Not Set (ZPK)"),
                    CodeOption("*", "'*' (X'2A) - BDK-1"),
                    CodeOption("~", "'~' (X'7E) - BDK-2"),
                    CodeOption("!", "'!' (X'21) - BDK-4"),
                ),
                desc = "'0' = destination is a ZPK (under LMK 06-07). '*'/'~'/'!' = destination is a BDK (under LMK 28-29), and destination KSN fields are required."),
            f("destKey", "ZPK / Dest BDK", FieldType.HEX, 0,
                desc = "Destination key encrypted under LMK. If Destination Key Type = '0', this is a ZPK under LMK pair 06-07. Otherwise, this is a destination BDK under LMK pair 28-29."),
            f("srcKsnDescriptor", "Source KSN Descriptor", FieldType.HEX, 3, default = "906",
                desc = "3-digit descriptor for the source KSN. Format: BDK_ID_len + Sub-Key_len + Device_ID_len (e.g. 906). Determines the number of counter bits in the KSN."),
            f("srcKsn", "Key Serial Number", FieldType.HEX, 20,
                desc = "20H source Key Serial Number from the terminal. Contains BDK-ID, device-ID, and transaction counter. Used to derive the DUKPT session key."),
            f("dstKsnDescriptor", "Destination KSN Descriptor", FieldType.HEX, 3, default = "906",
                req = FieldRequirement.OPTIONAL,
                cond = FieldCondition("destKeyType", setOf("*", "~", "!")),
                desc = "3-digit KSN descriptor for the destination BDK. Only required when Destination Key Type is BDK (*,~,!)."),
            f("dstKsn", "Destination Key Serial Number", FieldType.HEX, 20,
                req = FieldRequirement.OPTIONAL,
                cond = FieldCondition("destKeyType", setOf("*", "~", "!")),
                desc = "20H destination KSN. Only required when Destination Key Type is BDK. Used to derive the destination DUKPT session key for re-encrypting the PIN block."),
            f("sourcePinBlock", "Source Encrypted Block", FieldType.HEX, 16,
                desc = "16H PIN block encrypted under the source DUKPT-derived session key (PEK variant)."),
            f("sourcePinBlockFormat", "Source PIN block Format", FieldType.CODE, 2, default = "01",
                options = pinBlockFormatOptions,
                desc = "PIN block format of the incoming encrypted PIN block."),
            f("destinationPinBlockFormat", "Destination Format Code", FieldType.CODE, 2, default = "01",
                options = pinBlockFormatOptions,
                desc = "PIN block format for the re-encrypted output PIN block."),
            f("pan", "Account Number", FieldType.DEC, 12, default = "999999999999",
                desc = "12 rightmost digits of the PAN excluding the check digit. Used for PIN block format 0/3 XOR masking."),
            f("lmkDelimiter", "Delimiter", FieldType.FLAG, 0, req = FieldRequirement.OPTIONAL,
                default = "", desc = "Prefix '%' before 2-digit LMK Identifier."),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 2, req = FieldRequirement.OPTIONAL,
                default = "00", options = lmkPairIdOptions,
                conds = listOf(FieldCondition("lmkDelimiter", setOf("%"))),
                desc = "LMK pair ID selecting which LMK set to use."),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2,
                desc = "00 = No error."),
            f("checkLength", "PIN Length", FieldType.DEC, 2,
                desc = "Length of the clear PIN (2 digits)."),
            f("pinBlock", "Destination PIN Block", FieldType.HEX, 16,
                desc = "16H re-encrypted PIN block under the destination key."),
            f("pinBlockFormat", "Dest PIN Block Format", FieldType.CODE, 2,
                desc = "Format code of the output PIN block (echoed from input)."),
        ),
        forceVerticalFieldLayout = true,
    ),

    ThalesCommandDefinition(
        code = "JA", responseCode = "JB", name = "Generate a Random PIN",
        description = "Generate a random PIN of 4 to 12 digits",
        category = CommandCategory.PIN_OPERATIONS,
        requestFields = listOf(
            f("accountNumber", "Account Number", FieldType.DEC, 12, default = "123456789012"),
            f("pinLength", "PIN Length", FieldType.DEC, 2, default = "04"),
            f("excludedPinTable", "Excluded PIN Table", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL),
            f("excludedPinLength", "Excluded PIN Length", FieldType.DEC, 2, req = FieldRequirement.OPTIONAL),
            f("excludedPinCount", "Excluded PIN Count", FieldType.DEC, 2, req = FieldRequirement.OPTIONAL),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("pin", "PIN under LMK", FieldType.HEX, 0),
        ),
    ),

    ThalesCommandDefinition(
        code = "JC", responseCode = "JD", name = "Translate PIN (TPK to LMK)",
        description = "Translate a PIN from TPK to LMK encryption",
        category = CommandCategory.PIN_OPERATIONS,
        requestFields = listOf(
            f("tpk", "TPK", FieldType.HEX, 0),
            f("pinBlock", "PIN Block", FieldType.HEX, 16),
            f("pinBlockFormat", "PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("pan", "PAN (Account Number)", FieldType.DEC, 12),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("pinUnderLMK", "PIN under LMK", FieldType.HEX, 0),
        ),
    ),

    ThalesCommandDefinition(
        code = "JE", responseCode = "JF", name = "Translate PIN (ZPK to LMK)",
        description = "Translate a PIN from ZPK to LMK encryption",
        category = CommandCategory.PIN_OPERATIONS,
        requestFields = listOf(
            f("zpk", "ZPK", FieldType.HEX, 0),
            f("pinBlock", "PIN Block", FieldType.HEX, 16),
            f("pinBlockFormat", "PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("pan", "PAN (Account Number)", FieldType.DEC, 12),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("pinUnderLMK", "PIN under LMK", FieldType.HEX, 0),
        ),
    ),

    ThalesCommandDefinition(
        code = "JG", responseCode = "JH", name = "Translate PIN (LMK to ZPK)",
        description = "Translate a PIN from LMK to ZPK encryption",
        category = CommandCategory.PIN_OPERATIONS,
        requestFields = listOf(
            f("zpk", "Destination ZPK", FieldType.HEX, 0),
            f("pinBlockFormat", "PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("pan", "PAN (Account Number)", FieldType.DEC, 12),
            f("delimiter", "Delimiter", FieldType.ASCII, 1, req = FieldRequirement.OPTIONAL, default = ";",
                desc = "; delimiter before PIN under LMK"),
            f("pin", "PIN under LMK", FieldType.HEX, 0),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("pinBlock", "PIN Block", FieldType.HEX, 16),
        ),
    ),

    ThalesCommandDefinition(
        code = "NG", responseCode = "NH", name = "Decrypt an Encrypted PIN",
        description = "Decrypt an encrypted PIN and return the clear text PIN",
        category = CommandCategory.PIN_OPERATIONS,
        requestFields = listOf(
            f("accountNumber", "Account Number", FieldType.DEC, 12, default = "123456789012"),
            f("pinUnderLMK", "PIN under LMK", FieldType.HEX, 0,
                default = "JC3999796741E7075ADEAF47C43A226BF",
                desc = "PIN encrypted under LMK (J-scheme or 16H)"),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("clearPIN", "Clear PIN", FieldType.ASCII, 0),
        ),
    ),

    // ==================== DUKPT PIN VERIFICATION ====================

    ThalesCommandDefinition(
        code = "CK", responseCode = "CL", name = "Verify PIN IBM (DUKPT)",
        description = "Verify a PIN using the IBM method with DUKPT",
        category = CommandCategory.DUKPT,
        requestFields = listOf(
            f("bdk", "BDK", FieldType.HEX, 0),
            f("pvkPair", "PVK Pair", FieldType.HEX, 0),
            f("ksn", "KSN", FieldType.HEX, 20),
            f("pinBlock", "PIN Block", FieldType.HEX, 16),
            f("pinBlockFmt", "PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("accountNumber", "Account Number", FieldType.DEC, 12),
            f("decTable", "Decimalization Table", FieldType.HEX, 16),
            f("pinValData", "PIN Validation Data", FieldType.ASCII, 12),
            f("offset", "Offset", FieldType.ASCII, 12),
        ),
        responseFields = listOf(f("errorCode", "Error Code", FieldType.DEC, 2)),
    ),

    ThalesCommandDefinition(
        code = "CM", responseCode = "CN", name = "Verify PIN VISA PVV (DUKPT)",
        description = "Verify a PIN using the VISA PVV method with DUKPT",
        category = CommandCategory.DUKPT,
        requestFields = listOf(
            f("bdk", "BDK", FieldType.HEX, 0),
            f("pvkPair", "PVK Pair", FieldType.HEX, 0),
            f("ksn", "KSN", FieldType.HEX, 20),
            f("pinBlock", "PIN Block", FieldType.HEX, 16),
            f("pinBlockFmt", "PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("accountNumber", "Account Number", FieldType.DEC, 12),
            f("pvki", "PVKI", FieldType.DEC, 1, default = "1"),
            f("pvv", "PVV", FieldType.DEC, 4),
        ),
        responseFields = listOf(f("errorCode", "Error Code", FieldType.DEC, 2)),
    ),

    ThalesCommandDefinition(
        code = "GO", responseCode = "GP", name = "Verify PIN IBM (3DES DUKPT)",
        description = "Verify a PIN using the IBM offset method with 3DES DUKPT",
        category = CommandCategory.DUKPT,
        requestFields = listOf(
            f("mode", "Mode", FieldType.CODE, 1, default = "0", options = listOf(
                CodeOption("0", "0 - PIN only"),
                CodeOption("1", "1 - PIN + MAC verify"))),
            f("macMode", "MAC Mode", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL,
                cond = FieldCondition("mode", setOf("1")),
                desc = "MAC mode (only when mode=1)"),
            f("macMethod", "MAC Method", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL,
                cond = FieldCondition("mode", setOf("1")),
                desc = "MAC method (only when mode=1)"),
            f("bdk", "BDK", FieldType.HEX, 0),
            f("pvk", "PVK Pair", FieldType.HEX, 0),
            f("ksnDescriptor", "KSN Descriptor", FieldType.HEX, 3),
            f("ksn", "KSN", FieldType.HEX, 0),
            f("pinBlock", "PIN Block", FieldType.HEX, 16),
            f("pinBlockFormat", "PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("checkLength", "Check Length", FieldType.DEC, 2, default = "04"),
            f("pan", "PAN (Account Number)", FieldType.DEC, 12),
            f("decimalisationTable", "Decimalisation Table", FieldType.HEX, 16),
            f("pinValidationData", "PIN Validation Data", FieldType.ASCII, 12),
            f("offset", "Offset", FieldType.ASCII, 12),
            f("mac", "MAC", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL,
                cond = FieldCondition("mode", setOf("1"))),
            f("messageDataLengthBytes", "Message Data Length", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL,
                cond = FieldCondition("mode", setOf("1"))),
            f("messageData", "Message Data", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL,
                cond = FieldCondition("mode", setOf("1"))),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(f("errorCode", "Error Code", FieldType.DEC, 2)),
    ),

    ThalesCommandDefinition(
        code = "GQ", responseCode = "GR", name = "Verify PIN ABA PVV (3DES DUKPT)",
        description = "Verify a PIN using the ABA PVV method with 3DES DUKPT",
        category = CommandCategory.DUKPT,
        requestFields = listOf(
            f("mode", "Mode", FieldType.CODE, 1, default = "0", options = listOf(
                CodeOption("0", "0 - PIN only"),
                CodeOption("1", "1 - PIN + MAC verify"))),
            f("macMode", "MAC Mode", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL,
                cond = FieldCondition("mode", setOf("1")),
                desc = "MAC mode (only when mode=1)"),
            f("macMethod", "MAC Method", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL,
                cond = FieldCondition("mode", setOf("1")),
                desc = "MAC method (only when mode=1)"),
            f("bdk", "BDK", FieldType.HEX, 0),
            f("pvk", "PVK Pair", FieldType.HEX, 0),
            f("ksnDescriptor", "KSN Descriptor", FieldType.HEX, 3),
            f("ksn", "KSN", FieldType.HEX, 0),
            f("pinBlock", "PIN Block", FieldType.HEX, 16),
            f("pinBlockFormat", "PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("pan", "PAN (Account Number)", FieldType.DEC, 12),
            f("delimiter", "Delimiter", FieldType.ASCII, 1, req = FieldRequirement.OPTIONAL, default = ";",
                desc = "; delimiter before verification PAN"),
            f("verificationPan", "Verification PAN", FieldType.DEC, 0, req = FieldRequirement.OPTIONAL),
            f("pvki", "PVKI", FieldType.DEC, 1, default = "1"),
            f("pvv", "PVV", FieldType.DEC, 4),
            f("mac", "MAC", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL,
                cond = FieldCondition("mode", setOf("1"))),
            f("messageDataLengthBytes", "Message Data Length", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL,
                cond = FieldCondition("mode", setOf("1"))),
            f("messageData", "Message Data", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL,
                cond = FieldCondition("mode", setOf("1"))),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(f("errorCode", "Error Code", FieldType.DEC, 2)),
    ),

    ThalesCommandDefinition(
        code = "GU", responseCode = "GV", name = "Verify PIN Encrypted (3DES DUKPT)",
        description = "Verify a PIN using the encrypted PIN method with 3DES DUKPT",
        category = CommandCategory.DUKPT,
        requestFields = listOf(
            f("bdk", "BDK", FieldType.HEX, 0),
            f("zpk", "ZPK", FieldType.HEX, 0),
            f("ksn", "KSN", FieldType.HEX, 20),
            f("pinBlockSrc", "PIN Block (Source)", FieldType.HEX, 16),
            f("pinBlockDest", "PIN Block (Dest)", FieldType.HEX, 16),
            f("pinBlockFmt", "PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("accountNumber", "Account Number", FieldType.DEC, 12),
        ),
        responseFields = listOf(f("errorCode", "Error Code", FieldType.DEC, 2)),
    ),

    // ==================== CVV OPERATIONS ====================

    ThalesCommandDefinition(
        code = "CW", responseCode = "CX", name = "Generate CVV/CVC",
        description = "Generate a Card Verification Code/Value",
        category = CommandCategory.CVV_OPERATIONS,
        requestFields = listOf(
            f("cvk", "CVK Pair A+B", FieldType.HEX, 0,
                default = "S10096C0TG00E00033C0789B4F31EAF09C2369718E56042A7F25CE120A1490CF25D5AC9A96242903D2D7B92AB580BFBAA",
                desc = "CVK pair encrypted under LMK (S-block or hex)"),
            f("primaryAccountNumber", "PAN", FieldType.DEC, 0, default = "4111111111111111",
                desc = "Right-justified, up to 19 digits"),
            f("separator", "Separator", FieldType.ASCII, 1, default = ";"),
            f("expirationDate", "Expiry Date (YYMM)", FieldType.DEC, 4, default = "2612"),
            f("serviceCode", "Service Code", FieldType.DEC, 3, default = "000"),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL,
                default = "%03", options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("cvv", "CVV", FieldType.DEC, 3),
        ),
    ),

    ThalesCommandDefinition(
        code = "CY", responseCode = "CZ", name = "Verify CVV/CVC",
        description = "Verify a Card Verification Code/Value",
        category = CommandCategory.CVV_OPERATIONS,
        requestFields = listOf(
            f("cvkPair", "CVK Pair A+B", FieldType.HEX, 0,
                default = "S10096C0TG00E00033C0789B4F31EAF09C2369718E56042A7F25CE120A1490CF25D5AC9A96242903D2D7B92AB580BFBAA",
                desc = "CVK pair encrypted under LMK (S-block or hex)"),
            f("cvv", "CVV", FieldType.DEC, 3),
            f("pan", "PAN", FieldType.DEC, 0, default = "4111111111111111"),
            f("separator", "Separator", FieldType.ASCII, 1, default = ";"),
            f("expiryDate", "Expiry Date (YYMM)", FieldType.DEC, 4, default = "2612"),
            f("serviceCode", "Service Code", FieldType.DEC, 3, default = "000"),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL,
                default = "%03", options = lmkIdentifierOptions),
        ),
        responseFields = listOf(f("errorCode", "Error Code", FieldType.DEC, 2)),
    ),

    ThalesCommandDefinition(
        code = "PM", responseCode = "PN", name = "Verify Dynamic CVV (dCVV)",
        description = "Verify a dynamic CVV",
        category = CommandCategory.CVV_OPERATIONS,
        requestFields = listOf(
            f("imkCvc3", "IMK-CVC3", FieldType.HEX, 0),
            f("pan", "PAN", FieldType.DEC, 0),
            f("separator", "Separator", FieldType.ASCII, 1, default = ";"),
            f("atc", "App Transaction Counter", FieldType.HEX, 4),
            f("un", "Unpredictable Number", FieldType.HEX, 8),
            f("trackData", "Track Data", FieldType.HEX, 0),
        ),
        responseFields = listOf(f("errorCode", "Error Code", FieldType.DEC, 2)),
    ),

    // ==================== MAC OPERATIONS ====================

    ThalesCommandDefinition(
        code = "C2", responseCode = "C3", name = "Generate MAC",
        description = "Generate MAC on an outgoing message",
        category = CommandCategory.MAC_OPERATIONS,
        requestFields = listOf(
            f("keyType", "Key Type", FieldType.CODE, 1, default = "0", options = listOf(
                CodeOption("0", "0 - TAK"), CodeOption("1", "1 - ZAK"),
                CodeOption("2", "2 - TAK (Send)"), CodeOption("3", "3 - ZAK (Send)"))),
            f("key", "Key", FieldType.HEX, 0),
            f("macGenerationMode", "MAC Generation Mode", FieldType.CODE, 1, default = "0", options = listOf(
                CodeOption("0", "0 - ANSI X9.9"), CodeOption("1", "1 - ANSI X9.19"),
                CodeOption("2", "2 - AS2805.4.1 MAB"))),
            f("messageFormatFlag", "Message Format Flag", FieldType.DEC, 1, default = "0"),
            f("messageBlockNumber", "Message Block Number", FieldType.CODE, 1, default = "0", options = listOf(
                CodeOption("0", "0 - Only block"), CodeOption("1", "1 - First block"),
                CodeOption("2", "2 - Middle block"), CodeOption("3", "3 - Last block"))),
            f("messageLength", "Message Length", FieldType.HEX, 4),
            f("messageBlock", "Message Block", FieldType.HEX, 0),
            f("iv", "IV", FieldType.HEX, 16, default = "0000000000000000"),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("mabOrMac", "MAB/MAC", FieldType.HEX, 0),
        ),
    ),

    ThalesCommandDefinition(
        code = "C4", responseCode = "C5", name = "Verify MAC",
        description = "Verify MAC on an incoming message",
        category = CommandCategory.MAC_OPERATIONS,
        requestFields = listOf(
            f("keyType", "Key Type", FieldType.CODE, 1, default = "0", options = listOf(
                CodeOption("0", "0 - TAK"), CodeOption("1", "1 - ZAK"),
                CodeOption("2", "2 - TAK (Receive)"), CodeOption("3", "3 - ZAK (Receive)"))),
            f("key", "Key", FieldType.HEX, 0),
            f("mac", "MAC", FieldType.HEX, 16),
            f("macVerificationMode", "MAC Verification Mode", FieldType.CODE, 1, default = "0", options = listOf(
                CodeOption("0", "0 - ANSI X9.9"), CodeOption("1", "1 - ANSI X9.19"),
                CodeOption("2", "2 - AS2805.4.1-2001"))),
            f("messageFormatFlag", "Message Format Flag", FieldType.DEC, 1, default = "0"),
            f("messageBlockNumber", "Message Block Number", FieldType.CODE, 1, default = "0", options = listOf(
                CodeOption("0", "0 - Only block"), CodeOption("1", "1 - First block"),
                CodeOption("2", "2 - Middle block"), CodeOption("3", "3 - Last block"))),
            f("messageLength", "Message Length", FieldType.HEX, 4),
            f("messageBlock", "Message Block", FieldType.HEX, 0),
            f("iv", "IV", FieldType.HEX, 16, default = "0000000000000000"),
            f("initializationValue", "Initialization Value", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("mab", "MAB", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL),
        ),
    ),

    ThalesCommandDefinition(
        code = "M6", responseCode = "M7", name = "Generate MAC (Extended)",
        description = "Generate MAC with extended algorithm support including KeyBlock keys",
        category = CommandCategory.MAC_OPERATIONS,
        requestFields = listOf(
            f("modeFlag", "Mode Flag", FieldType.CODE, 1, default = "0", options = listOf(
                CodeOption("0", "0 - Only block (single-block message)"),
                CodeOption("1", "1 - First block"),
                CodeOption("2", "2 - Middle block"),
                CodeOption("3", "3 - Last block"))),
            f("inputFormatFlag", "Input Format Flag", FieldType.CODE, 1, default = "1", options = listOf(
                CodeOption("0", "0 - Binary"),
                CodeOption("1", "1 - Hex-Encoded Binary"),
                CodeOption("2", "2 - Text"))),
            f("macSize", "MAC Size", FieldType.CODE, 1, default = "1", options = listOf(
                CodeOption("0", "0 - Half MAC (4 bytes / 8H)"),
                CodeOption("1", "1 - Full MAC (8 bytes / 16H)"))),
            f("macAlgorithm", "MAC Algorithm", FieldType.CODE, 1, default = "3", options = listOf(
                CodeOption("1", "1 - ISO 9797-1 Algorithm 1"),
                CodeOption("3", "3 - ISO 9797-1 Algorithm 3 (ANSI X9.19 / Retail MAC)"))),
            f("paddingMethod", "Padding Method", FieldType.CODE, 1, default = "1", options = listOf(
                CodeOption("1", "1 - ISO 9797 Method 1 (zero pad)"),
                CodeOption("2", "2 - ISO 9797 Method 2 (0x80 + zeros)"))),
            f("keyType", "Key Type", FieldType.CODE, 3, default = "FFF", options = keyTypeOptions),
            f("macKey", "MAC Key", FieldType.HEX, 0, desc = "MAC key under LMK (S-block or variant+hex)"),
            f("iv", "IV / OCD", FieldType.HEX, 16, req = FieldRequirement.OPTIONAL, default = "0000000000000000",
                cond = FieldCondition("modeFlag", setOf("1", "2", "3"))),
            f("msgLength", "Message Length", FieldType.HEX, 4),
            f("messageBlock", "Message Block", FieldType.HEX, 0),
            f("delimiter", "Delimiter", FieldType.FLAG, 0, req = FieldRequirement.OPTIONAL, default = "%"),
            f("lmkIdentifier", "LMK Identifier", FieldType.ASCII, 2, req = FieldRequirement.OPTIONAL,
                default = "00", cond = FieldCondition("delimiter", setOf("%"))),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("mac", "MAC", FieldType.HEX, 0),
            f("ocd", "Output Chaining Data", FieldType.HEX, 16, req = FieldRequirement.OPTIONAL),
        ),
    ),

    ThalesCommandDefinition(
        code = "M8", responseCode = "M9", name = "Verify MAC (Extended)",
        description = "Verify MAC with extended algorithm support",
        category = CommandCategory.MAC_OPERATIONS,
        requestFields = listOf(
            f("modeFlag", "Mode Flag", FieldType.CODE, 1, default = "0", options = listOf(
                CodeOption("0", "0 - Verify MAC"), CodeOption("1", "1 - Generate MAC"),
                CodeOption("2", "2 - Generate + Verify"), CodeOption("3", "3 - Translate MAC"))),
            f("inputFormatFlag", "Input Format Flag", FieldType.DEC, 1, default = "0"),
            f("macSize", "MAC Size", FieldType.CODE, 1, default = "4", options = listOf(
                CodeOption("0", "0 - Full"), CodeOption("4", "4 - Default (4 bytes)"),
                CodeOption("8", "8 - 8 bytes"))),
            f("macAlgorithm", "MAC Algorithm", FieldType.CODE, 1, default = "0", options = listOf(
                CodeOption("0", "0 - ISO 9797-1 Alg 1"), CodeOption("1", "1 - ISO 9797-1 Alg 3"),
                CodeOption("2", "2 - AES-CMAC"), CodeOption("3", "3 - HMAC-SHA-1"),
                CodeOption("4", "4 - HMAC-SHA-224"), CodeOption("5", "5 - HMAC-SHA-256"),
                CodeOption("6", "6 - HMAC-SHA-384"), CodeOption("7", "7 - HMAC-SHA-512"))),
            f("paddingMethod", "Padding Method", FieldType.CODE, 1, default = "0", options = listOf(
                CodeOption("0", "0 - Pad 0x00"), CodeOption("1", "1 - ISO 9797 Method 1"),
                CodeOption("2", "2 - ISO 9797 Method 2"))),
            f("keyType", "Key Type", FieldType.CODE, 3, default = "FFF", options = keyTypeOptions),
            f("macKey", "MAC Key", FieldType.HEX, 0, desc = "MAC key under LMK (S-block or hex)"),
            f("msgLength", "Message Length", FieldType.HEX, 4),
            f("messageBlock", "Message Block", FieldType.HEX, 0),
            f("mac", "MAC to Verify", FieldType.HEX, 0),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("iv", "IV", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL),
        ),
    ),

    ThalesCommandDefinition(
        code = "MS", responseCode = "MT", name = "Generate MAC (Large Message)",
        description = "Generate MAC using ANSI X9.19 for large messages",
        category = CommandCategory.MAC_OPERATIONS,
        requestFields = listOf(
            f("mode", "Mode", FieldType.CODE, 1, default = "0", options = listOf(
                CodeOption("0", "0 - Final block"), CodeOption("1", "1 - First block"), CodeOption("2", "2 - Middle block"))),
            f("inputFormat", "Input Format", FieldType.DEC, 1, default = "0"),
            f("keyType", "Key Type", FieldType.DEC, 2, default = "01"),
            f("keyLength", "Key Length", FieldType.DEC, 1, default = "1"),
            f("macKey", "MAC Key", FieldType.HEX, 0),
            f("iv", "IV", FieldType.HEX, 16, default = "0000000000000000"),
            f("msgLength", "Message Length", FieldType.HEX, 4),
            f("messageBlock", "Message Block", FieldType.HEX, 0),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("macOrOcd", "MAC / Output Chaining Data", FieldType.HEX, 0),
        ),
    ),

    ThalesCommandDefinition(
        code = "MY", responseCode = "MZ", name = "Verify and Translate MAC",
        description = "Verify a MAC and generate MAC under a different key",
        category = CommandCategory.MAC_OPERATIONS,
        requestFields = listOf(
            f("mode", "Mode", FieldType.DEC, 1, default = "1"),
            f("inputFormatFlag", "Input Format Flag", FieldType.DEC, 1, default = "0"),
            f("macAlgorithm", "MAC Algorithm", FieldType.CODE, 2, default = "01", options = macAlgorithmOptions),
            f("paddingMethod", "Padding Method", FieldType.DEC, 2, default = "01"),
            f("keyTypeIn", "Key Type (In)", FieldType.DEC, 2, default = "01"),
            f("keyLengthIn", "Key Length (In)", FieldType.DEC, 1, default = "1"),
            f("macKeyIn", "MAC Key (In)", FieldType.HEX, 0),
            f("keyTypeOut", "Key Type (Out)", FieldType.DEC, 2, default = "01"),
            f("keyLengthOut", "Key Length (Out)", FieldType.DEC, 1, default = "1"),
            f("macKeyOut", "MAC Key (Out)", FieldType.HEX, 0),
            f("ivIn", "IV (In)", FieldType.HEX, 0, default = "0000000000000000"),
            f("ivOut", "IV (Out)", FieldType.HEX, 0, default = "0000000000000000"),
            f("msgLength", "Message Length", FieldType.HEX, 4),
            f("messageBlock", "Message Block", FieldType.HEX, 0),
            f("mac", "MAC", FieldType.HEX, 0),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("mac", "MAC", FieldType.HEX, 0),
        ),
    ),

    ThalesCommandDefinition(
        code = "GW", responseCode = "GX", name = "Generate/Verify MAC (DUKPT)",
        description = "Generate or verify a MAC using 3DES DUKPT MAC key",
        category = CommandCategory.DUKPT,
        requestFields = listOf(
            f("macMode", "MAC Mode", FieldType.CODE, 1, default = "1", options = listOf(
                CodeOption("0", "0 - Verify"), CodeOption("1", "1 - Generate"))),
            f("macMethod", "MAC Method", FieldType.CODE, 1, default = "1", options = listOf(
                CodeOption("1", "1 - Method 1"), CodeOption("2", "2 - Method 2"),
                CodeOption("3", "3 - Method 3"), CodeOption("4", "4 - Method 4"))),
            f("bdk", "BDK", FieldType.HEX, 0),
            f("ksnDescriptor", "KSN Descriptor", FieldType.HEX, 3),
            f("ksn", "KSN", FieldType.HEX, 0),
            f("mac", "MAC", FieldType.HEX, 0, FieldRequirement.OPTIONAL,
                cond = FieldCondition("macMode", setOf("0")),
                desc = "MAC to verify (only when macMode=0)"),
            f("messageDataLengthBytes", "Message Data Length (bytes)", FieldType.HEX, 4),
            f("messageData", "Message Data", FieldType.HEX, 0),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("mac", "MAC", FieldType.HEX, 0),
        ),
    ),

    ThalesCommandDefinition(
        code = "MG", responseCode = "MH", name = "Translate TAK (LMK to ZMK)",
        description = "Translate a TAK from LMK to ZMK encryption",
        category = CommandCategory.MAC_OPERATIONS,
        requestFields = listOf(
            f("zmk", "ZMK", FieldType.HEX, 0),
            f("tak", "TAK", FieldType.HEX, 0),
            f("keySchemeZMK", "Key Scheme (ZMK)", FieldType.CODE, 1, default = "Z", options = keySchemeOptions),
            f("kcvType", "Key Check Value Type", FieldType.CODE, 1, FieldRequirement.OPTIONAL, options = kcvTypeOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("takUnderZMK", "TAK under ZMK", FieldType.HEX, 0),
            f("kcv", "Key Check Value", FieldType.HEX, 6),
        ),
    ),

    ThalesCommandDefinition(
        code = "MI", responseCode = "MJ", name = "Translate TAK (ZMK to LMK)",
        description = "Translate a TAK from ZMK to LMK encryption",
        category = CommandCategory.MAC_OPERATIONS,
        requestFields = listOf(
            f("zmk", "ZMK", FieldType.HEX, 0),
            f("takUnderZMK", "TAK under ZMK", FieldType.HEX, 0),
            f("keySchemeLMK", "Key Scheme (LMK)", FieldType.CODE, 1, default = "Z", options = keySchemeOptions),
            f("kcvType", "Key Check Value Type", FieldType.CODE, 1, FieldRequirement.OPTIONAL, options = kcvTypeOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("takUnderLMK", "TAK under LMK", FieldType.HEX, 0),
            f("kcv", "Key Check Value", FieldType.HEX, 6),
        ),
    ),

    // ==================== DATA ENCRYPTION ====================

    ThalesCommandDefinition(
        code = "M0", responseCode = "M1", name = "Encrypt Data Block",
        description = "Encrypt a block of data",
        category = CommandCategory.DATA_ENCRYPTION,
        requestFields = listOf(
            f("modeFlag", "Mode Flag", FieldType.CODE, 2, default = "01", options = listOf(
                CodeOption("00", "00 - ECB"), CodeOption("01", "01 - CBC"),
                CodeOption("02", "02 - CFB-8"), CodeOption("03", "03 - OFB"),
                CodeOption("04", "04 - CTR"), CodeOption("05", "05 - FPE (FF1)"),
                CodeOption("10", "10 - ECB (AES)"), CodeOption("11", "11 - CBC (AES)"),
                CodeOption("13", "13 - GCM (AES)"))),
            f("inputFormatFlag", "Input Format Flag", FieldType.CODE, 1, default = "1", options = dataFormatOptions),
            f("outputFormatFlag", "Output Format Flag", FieldType.CODE, 1, default = "1", options = dataFormatOptions),
            f("keyType", "Key Type", FieldType.CODE, 3, default = "FFF", options = keyTypeOptions),
            f("key", "Key", FieldType.HEX, 0,
                default = "S10096D0TB00N0003E28EE3EE557B3CD4D1239C692FCF9792F3C7F0D77952A57A7C64B422A8673FDD663F5FCD88D7FE80",
                desc = "Encryption key under LMK (S-block or hex)"),
            f("ksnDescriptor", "KSN Descriptor", FieldType.HEX, 3, req = FieldRequirement.OPTIONAL,
                desc = "3-char descriptor (e.g. 609), omit for non-DUKPT; required for DUKPT modes"),
            f("ksn", "Key Serial Number", FieldType.HEX, 20, req = FieldRequirement.OPTIONAL,
                desc = "20H KSN; omit for non-DUKPT; required for DUKPT modes"),
            f("iv", "IV", FieldType.HEX, 0, FieldRequirement.OPTIONAL, default = "0000000000000000",
                desc = "Required for non-ECB modes (CBC, CFB, OFB, CTR, FPE, CBC-AES, GCM)",
                cond = FieldCondition(
                    "modeFlag",
                    setOf("01", "02", "03", "04", "05", "11", "13"),
                )),
            f("msgLength", "Message Length", FieldType.HEX, 4, default = "0030",
                desc = "4H hex character count of message data (auto-calculated from message block)"),
            f("messageBlock", "Message Block", FieldType.HEX, 0,
                default = "5217301101010202D2020201121212121FFFFFFFFFFFFFFF"),
            f("lmkDelimiter", "Delimiter", FieldType.FLAG, 0, req = FieldRequirement.OPTIONAL,
                default = "%", desc = "Prefix % before LMK pair identifier"),
            f("lmkPairId", "LMK Identifier", FieldType.CODE, 2, req = FieldRequirement.OPTIONAL,
                default = "03", options = lmkPairIdOptions,
                cond = FieldCondition("lmkDelimiter", setOf("%"))),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("iv", "IV", FieldType.HEX, 16, req = FieldRequirement.OPTIONAL,
                desc = "16H (8 bytes) for CBC path; omitted on wire for ECB responses"),
            f("encryptedMessageLength", "Encrypted Message Length", FieldType.HEX, 4,
                desc = "4H hex character count of ciphertext"),
            f("encryptedData", "Encrypted Data", FieldType.HEX, 0,
                desc = "Ciphertext (length from previous field)"),
        ),
    ),

    ThalesCommandDefinition(
        code = "M2", responseCode = "M3", name = "Decrypt Data Block",
        description = "Decrypt a block of data",
        category = CommandCategory.DATA_ENCRYPTION,
        requestFields = listOf(
            f("modeFlag", "Mode Flag", FieldType.CODE, 2, default = "01", options = listOf(
                CodeOption("00", "00 - ECB"), CodeOption("01", "01 - CBC"),
                CodeOption("02", "02 - CFB-8"), CodeOption("03", "03 - OFB"),
                CodeOption("04", "04 - CTR"), CodeOption("05", "05 - FPE (FF1)"),
                CodeOption("10", "10 - ECB (AES)"), CodeOption("11", "11 - CBC (AES)"),
                CodeOption("13", "13 - GCM (AES)"))),
            f("inputFormatFlag", "Input Format Flag", FieldType.CODE, 1, default = "1", options = dataFormatOptions),
            f("outputFormatFlag", "Output Format Flag", FieldType.CODE, 1, default = "1", options = dataFormatOptions),
            f("keyType", "Key Type", FieldType.CODE, 3, default = "FFF", options = keyTypeOptions),
            f("key", "Key", FieldType.HEX, 0,
                default = "S10096D0TB00N0003E28EE3EE557B3CD4D1239C692FCF9792F3C7F0D77952A57A7C64B422A8673FDD663F5FCD88D7FE80",
                desc = "Decryption key under LMK (S-block or hex)"),
            f("ksnDescriptor", "KSN Descriptor", FieldType.HEX, 3, req = FieldRequirement.OPTIONAL,
                desc = "3-char descriptor (e.g. 609), omit for non-DUKPT; required for DUKPT modes"),
            f("ksn", "Key Serial Number", FieldType.HEX, 20, req = FieldRequirement.OPTIONAL,
                desc = "20H KSN; omit for non-DUKPT; required for DUKPT modes"),
            f("iv", "IV", FieldType.HEX, 0, FieldRequirement.OPTIONAL, default = "0000000000000000",
                desc = "Required for non-ECB modes (CBC, CFB, OFB, CTR, FPE, CBC-AES, GCM)",
                cond = FieldCondition(
                    "modeFlag",
                    setOf("01", "02", "03", "04", "05", "11", "13"),
                )),
            f("encryptedMessageLength", "Message Length", FieldType.HEX, 4, default = "0030",
                desc = "4H hex character count of ciphertext (computed from message block in builder)"),
            f("encryptedData", "Message Block", FieldType.HEX, 0,
                default = "87D891D2876B7494BF4CBC9CCBB37F389833DFB152432B28",
                desc = "Encrypted data to decrypt"),
            f("lmkDelimiter", "Delimiter", FieldType.FLAG, 0, req = FieldRequirement.OPTIONAL,
                default = "%", desc = "Prefix % before LMK pair identifier"),
            f("lmkPairId", "LMK Identifier", FieldType.CODE, 2, req = FieldRequirement.OPTIONAL,
                default = "03", options = lmkPairIdOptions,
                cond = FieldCondition("lmkDelimiter", setOf("%"))),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("iv", "IV", FieldType.HEX, 16, req = FieldRequirement.OPTIONAL,
                desc = "16H (8 bytes) for CBC path; omitted on wire for ECB responses"),
            f("decryptedMessageLength", "Decrypted Message Length", FieldType.HEX, 4,
                desc = "4H hex character count of plaintext"),
            f("decryptedData", "Decrypted Data", FieldType.HEX, 0,
                desc = "Plaintext (length from previous field)"),
        ),
    ),

    ThalesCommandDefinition(
        code = "M4", responseCode = "M5", name = "Translate Data Block",
        description = "Translate a block of data from one key to another",
        category = CommandCategory.DATA_ENCRYPTION,
        requestFields = listOf(
            f("sourceModeFlag", "Source Mode Flag", FieldType.CODE, 2, default = "00", options = listOf(
                CodeOption("00", "00 - ECB"), CodeOption("01", "01 - CBC"),
                CodeOption("02", "02 - CFB-8"), CodeOption("03", "03 - OFB"),
                CodeOption("04", "04 - CTR"))),
            f("destinationModeFlag", "Destination Mode Flag", FieldType.CODE, 2, default = "00", options = listOf(
                CodeOption("00", "00 - ECB"), CodeOption("01", "01 - CBC"),
                CodeOption("02", "02 - CFB-8"), CodeOption("03", "03 - OFB"),
                CodeOption("04", "04 - CTR"))),
            f("inputFormatFlag", "Input Format Flag", FieldType.CODE, 1, default = "1", options = dataFormatOptions),
            f("outputFormatFlag", "Output Format Flag", FieldType.CODE, 1, default = "1", options = dataFormatOptions),
            f("srcKeyType", "Source Key Type", FieldType.CODE, 3, default = "FFF", options = keyTypeOptions),
            f("srcKey", "Source Key", FieldType.HEX, 0, desc = "Source decryption key under LMK"),
            f("srcKsnDescriptor", "Source KSN Descriptor", FieldType.HEX, 3, req = FieldRequirement.OPTIONAL),
            f("srcKsn", "Source KSN", FieldType.HEX, 20, req = FieldRequirement.OPTIONAL),
            f("destKeyType", "Dest Key Type", FieldType.CODE, 3, default = "FFF", options = keyTypeOptions),
            f("destKey", "Dest Key", FieldType.HEX, 0, desc = "Destination encryption key under LMK"),
            f("destKsnDescriptor", "Dest KSN Descriptor", FieldType.HEX, 3, req = FieldRequirement.OPTIONAL),
            f("destKsn", "Dest KSN", FieldType.HEX, 20, req = FieldRequirement.OPTIONAL),
            f("srcIV", "Source IV", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL, default = "0000000000000000",
                desc = "Source IV for non-ECB modes",
                cond = FieldCondition("sourceModeFlag", setOf("01", "02", "03", "04"))),
            f("destIV", "Dest IV", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL, default = "0000000000000000",
                desc = "Destination IV for non-ECB modes",
                cond = FieldCondition("destinationModeFlag", setOf("01", "02", "03", "04"))),
            f("msgLength", "Message Length", FieldType.HEX, 4,
                desc = "4H hex character count of encrypted data (auto-calculated)"),
            f("encryptedData", "Encrypted Data", FieldType.HEX, 0),
            f("lmkDelimiter", "Delimiter", FieldType.FLAG, 0, req = FieldRequirement.OPTIONAL,
                default = "", desc = "Prefix % before LMK pair identifier"),
            f("lmkPairId", "LMK Identifier", FieldType.CODE, 2, req = FieldRequirement.OPTIONAL,
                default = "03", options = lmkPairIdOptions,
                cond = FieldCondition("lmkDelimiter", setOf("%"))),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("outputIV", "Output IV", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL,
                desc = "Output IV; present for non-ECB destination modes"),
            f("msgLength", "Translated Message Length", FieldType.HEX, 4,
                desc = "4H hex character count of translated data"),
            f("translatedData", "Translated Data", FieldType.HEX, 0),
        ),
    ),

    ThalesCommandDefinition(
        code = "PU", responseCode = "PV", name = "Encrypt Data (ZEK/TEK)",
        description = "Encrypt data using a ZEK or TEK",
        category = CommandCategory.DATA_ENCRYPTION,
        requestFields = listOf(
            f("encryptionMode", "Encryption Mode", FieldType.CODE, 2, default = "00", options = cipherModeOptions),
            f("keyType", "Key Type", FieldType.CODE, 1, default = "0", options = listOf(
                CodeOption("0", "0 - ZEK"), CodeOption("1", "1 - TEK"))),
            f("key", "Key", FieldType.HEX, 0),
            f("iv", "IV", FieldType.HEX, 16, FieldRequirement.OPTIONAL, default = "0000000000000000"),
            f("msgLength", "Message Length", FieldType.HEX, 4),
            f("plaintextData", "Plaintext Data", FieldType.HEX, 0),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("iv", "IV", FieldType.HEX, 16),
            f("encryptedData", "Encrypted Data", FieldType.HEX, 0),
        ),
    ),

    ThalesCommandDefinition(
        code = "PW", responseCode = "PX", name = "Decrypt Data (ZEK/TEK)",
        description = "Decrypt data using a ZEK or TEK",
        category = CommandCategory.DATA_ENCRYPTION,
        requestFields = listOf(
            f("encryptionMode", "Encryption Mode", FieldType.CODE, 2, default = "00", options = cipherModeOptions),
            f("keyFlag", "Key Flag", FieldType.CODE, 1, default = "0", options = listOf(
                CodeOption("0", "0 - ZEK"), CodeOption("1", "1 - TEK"),
                CodeOption("2", "2 - ZEKr"), CodeOption("3", "3 - TEKr"))),
            f("encryptionKey", "Encryption Key", FieldType.HEX, 0),
            f("initializationValue", "IV", FieldType.HEX, 0, FieldRequirement.OPTIONAL, default = "0000000000000000"),
            f("length", "Data Length", FieldType.HEX, 4),
            f("encryptedData", "Encrypted Data", FieldType.HEX, 0),
            f("plaintextValue", "Plaintext Value", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("plaintextData", "Plaintext Data", FieldType.HEX, 0),
            f("ocv", "OCV", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL),
        ),
    ),

    ThalesCommandDefinition(
        code = "GM", responseCode = "GN", name = "Hash a Block of Data",
        description = "Hash a block of data using specified algorithm",
        category = CommandCategory.DATA_ENCRYPTION,
        requestFields = listOf(
            f("hashId", "Hash Identifier", FieldType.CODE, 2, default = "01", options = hashAlgorithmOptions),
            f("messageDataLength", "Message Data Length", FieldType.HEX, 5, desc = "5-digit hex length in bytes"),
            f("messageData", "Message Data", FieldType.HEX, 0),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("hashValue", "Hash Value", FieldType.HEX, 0),
        ),
    ),

    // ==================== RSA OPERATIONS ====================

    ThalesCommandDefinition(
        code = "EI", responseCode = "EJ", name = "Generate RSA Key Pair",
        description = "Generate an RSA public/private key pair",
        category = CommandCategory.RSA,
        requestFields = listOf(
            f("keyTypeIndicator", "Key Type Indicator", FieldType.CODE, 1, default = "1", options = listOf(
                CodeOption("1", "1 - Signature Only"), CodeOption("2", "2 - Key Management Only"),
                CodeOption("3", "3 - ICC"), CodeOption("4", "4 - Data Encryption"),
                CodeOption("5", "5 - PIN Encryption"))),
            f("keyLength", "Key Length", FieldType.DEC, 4, default = "2048"),
            f("publicKeyEncoding", "Public Key Encoding", FieldType.DEC, 2, default = "01", options = listOf(
                CodeOption("01", "01 - DER ASN.1 unsigned"), CodeOption("02", "02 - DER ASN.1 2's complement"))),
            f("publicExponentLength", "Public Exponent Length", FieldType.DEC, 4, req = FieldRequirement.OPTIONAL),
            f("publicExponent", "Public Exponent", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
            f("keyBlockKeyVersionNumber", "Key Block Key Version Number", FieldType.DEC, 0, req = FieldRequirement.OPTIONAL),
            f("keyBlockOptionalBlocksCount", "Key Block Opt Blocks Count", FieldType.DEC, 0, req = FieldRequirement.OPTIONAL),
            f("keyBlockExportability", "Key Block Exportability", FieldType.CODE, 1, req = FieldRequirement.OPTIONAL, options = keyBlockExportabilityOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("publicKey", "Public Key", FieldType.HEX, 0),
            f("privateKeyLength", "Private Key Length", FieldType.DEC, 4),
            f("privateKey", "Private Key under LMK", FieldType.HEX, 0),
        ),
    ),

    ThalesCommandDefinition(
        code = "EO", responseCode = "EP", name = "Import a Public Key",
        description = "Import an RSA public key",
        category = CommandCategory.RSA,
        requestFields = listOf(
            f("publicKeyEncoding", "Public Key Encoding", FieldType.DEC, 2, default = "01", options = listOf(
                CodeOption("01", "01 - DER ASN.1 unsigned"), CodeOption("02", "02 - DER ASN.1 2's complement"))),
            f("publicKey", "Public Key (DER)", FieldType.HEX, 0),
            f("authenticationData", "Authentication Data", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL),
            f("keyBlockExportability", "Key Block Exportability", FieldType.CODE, 1, req = FieldRequirement.OPTIONAL, options = keyBlockExportabilityOptions),
            f("keyBlockKeyVersionNumber", "Key Block Key Version Number", FieldType.DEC, 2, req = FieldRequirement.OPTIONAL),
            f("keyBlockModeOfUse", "Key Block Mode of Use", FieldType.CODE, 1, req = FieldRequirement.OPTIONAL, options = keyBlockModeOfUseOptions),
            f("keyBlockNumberOfOptionalBlocks", "Key Block Num Optional Blocks", FieldType.DEC, 2, req = FieldRequirement.OPTIONAL),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("mac", "MAC", FieldType.HEX, 4),
            f("publicKey", "Public Key", FieldType.HEX, 0),
        ),
    ),

    ThalesCommandDefinition(
        code = "EW", responseCode = "EX", name = "Generate RSA Signature",
        description = "Generate a digital signature using an RSA private key",
        category = CommandCategory.RSA,
        requestFields = listOf(
            f("hashIdentifier", "Hash Identifier", FieldType.CODE, 2, default = "01", options = hashAlgorithmOptions),
            f("signatureIdentifier", "Signature Identifier", FieldType.CODE, 2, default = "01", options = listOf(
                CodeOption("01", "01 - RSASSA-PKCS1-v1_5"), CodeOption("02", "02 - RSASSA-PSS"))),
            f("padModeIdentifier", "Pad Mode Identifier", FieldType.DEC, 2, default = "01"),
            f("messageDataLength", "Message Data Length", FieldType.HEX, 4, desc = "Length of message data in bytes"),
            f("messageData", "Message Data", FieldType.HEX, 0),
            f("separator", "Separator", FieldType.ASCII, 1, default = ";",
                desc = "; delimiter before private key"),
            f("privateKeyFlag", "Private Key Flag", FieldType.DEC, 2, default = "00"),
            f("privateKeyLength", "Private Key Length", FieldType.DEC, 4),
            f("privateKey", "Private Key under LMK", FieldType.HEX, 0),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("signatureLength", "Signature Length", FieldType.DEC, 4),
            f("signature", "Signature", FieldType.HEX, 0),
        ),
    ),

    ThalesCommandDefinition(
        code = "EY", responseCode = "EZ", name = "Validate RSA Signature",
        description = "Validate a digital signature using an RSA public key",
        category = CommandCategory.RSA,
        requestFields = listOf(
            f("hashIdentifier", "Hash Identifier", FieldType.CODE, 2, default = "01", options = hashAlgorithmOptions),
            f("signatureIdentifier", "Signature Identifier", FieldType.CODE, 2, default = "01", options = listOf(
                CodeOption("01", "01 - RSASSA-PKCS1-v1_5"), CodeOption("02", "02 - RSASSA-PSS"))),
            f("padModeIdentifier", "Pad Mode Identifier", FieldType.DEC, 2, default = "01"),
            f("publicKey", "Public Key (DER)", FieldType.HEX, 0),
            f("mac", "MAC", FieldType.HEX, 4),
            f("signatureLength", "Signature Length", FieldType.DEC, 4),
            f("signature", "Signature", FieldType.HEX, 0),
            f("dataLength", "Data Length", FieldType.HEX, 4),
            f("data", "Data", FieldType.HEX, 0),
            f("authenticationData", "Authentication Data", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(f("errorCode", "Error Code", FieldType.DEC, 2)),
    ),

    ThalesCommandDefinition(
        code = "GI", responseCode = "GJ", name = "Import Key under RSA",
        description = "Import a key or data under an RSA public key",
        category = CommandCategory.RSA,
        requestFields = listOf(
            f("flag", "Flag", FieldType.DEC, 2, default = "01"),
            f("rsaPrivKeyIndex", "RSA Private Key Index", FieldType.DEC, 2, default = "00"),
            f("encryptedData", "Encrypted Data", FieldType.HEX, 0),
            f("keyType", "Key Type", FieldType.CODE, 3, default = "001", options = keyTypeOptions),
            f("keyScheme", "Key Scheme (LMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
        ) + keyBlockFields(defaultUsage = "51", defaultModeOfUse = "E", defaultExportability = "S"),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("keyUnderLMK", "Key under LMK", FieldType.HEX, 0),
            f("kcv", "Key Check Value", FieldType.HEX, 6),
        ),
    ),

    ThalesCommandDefinition(
        code = "GK", responseCode = "GL", name = "Export Key under RSA",
        description = "Export a DES/AES key encrypted under an RSA public key",
        category = CommandCategory.RSA,
        requestFields = listOf(
            f("keyType", "Key Type", FieldType.CODE, 3, default = "001", options = keyTypeOptions),
            f("desKeyFlag", "DES Key Flag", FieldType.CODE, 1, default = "1", options = listOf(
                CodeOption("0", "0 - Single-length DES"),
                CodeOption("1", "1 - Double-length DES"),
                CodeOption("2", "2 - Triple-length DES"),
                CodeOption("F", "F - Key Block"),
            )),
            f("desAesKey", "DES/AES Key", FieldType.HEX, 0, desc = "Key under LMK to export"),
            f("hmacKeyFormat", "HMAC Key Format", FieldType.CODE, 2, default = "00", req = FieldRequirement.OPTIONAL, options = listOf(
                CodeOption("00", "00 - Variant LMK"),
                CodeOption("04", "04 - Key Block"),
            )),
            f("hmacKey", "HMAC Key", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL),
            f("hmacKeyLength", "HMAC Key Length", FieldType.DEC, 4, req = FieldRequirement.OPTIONAL),
            f("keyBlockType", "Key Block Type", FieldType.ASCII, 1, req = FieldRequirement.OPTIONAL),
            f("keyBlockTemplateLength", "Key Block Template Length", FieldType.DEC, 4, req = FieldRequirement.OPTIONAL),
            f("keyBlockTemplate", "Key Block Template", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL),
            f("publicKey", "RSA Public Key", FieldType.HEX, 0, desc = "DER-encoded public key"),
            f("privateKeyFlag", "Private Key Flag", FieldType.DEC, 2, default = "00", req = FieldRequirement.OPTIONAL),
            f("privateKeyLength", "Private Key Length", FieldType.DEC, 4, req = FieldRequirement.OPTIONAL),
            f("privateKey", "RSA Private Key", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL),
            f("mac", "MAC", FieldType.HEX, 4, desc = "MAC of the public key"),
            f("authenticationData", "Authentication Data", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL),
            f("encryptionIdentifier", "Encryption Identifier", FieldType.DEC, 2, default = "01", req = FieldRequirement.OPTIONAL),
            f("padModeIdentifier", "Pad Mode Identifier", FieldType.DEC, 2, default = "01", req = FieldRequirement.OPTIONAL),
            f("oaepParamsLength", "OAEP Params Length", FieldType.DEC, 4, req = FieldRequirement.OPTIONAL),
            f("oaepParams", "OAEP Encoding Parameters", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL),
            f("maskGenFunction", "Mask Generation Function", FieldType.DEC, 2, req = FieldRequirement.OPTIONAL),
            f("mgfHashFunction", "MGF Hash Function", FieldType.DEC, 2, req = FieldRequirement.OPTIONAL),
            f("sigHashIdentifier", "Signature Hash Identifier", FieldType.DEC, 2, req = FieldRequirement.OPTIONAL),
            f("sigIdentifier", "Signature Identifier", FieldType.DEC, 2, req = FieldRequirement.OPTIONAL),
            f("sigPadModeIdentifier", "Signature Pad Mode", FieldType.DEC, 2, req = FieldRequirement.OPTIONAL),
            f("keyOffset", "Key Offset", FieldType.DEC, 4, req = FieldRequirement.OPTIONAL),
            f("checkValueLength", "Check Value Length", FieldType.DEC, 4, req = FieldRequirement.OPTIONAL),
            f("checkValueOffset", "Check Value Offset", FieldType.DEC, 4, req = FieldRequirement.OPTIONAL),
            f("checkValue", "Check Value", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL),
            f("footerDataBlockLength", "Footer Data Block Length", FieldType.DEC, 4, req = FieldRequirement.OPTIONAL),
            f("footerDataBlock", "Footer Data Block", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL),
            f("sigHeaderBlockLength", "Signature Header Block Length", FieldType.DEC, 4, req = FieldRequirement.OPTIONAL),
            f("sigHeaderBlock", "Signature Header Block", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("encryptedKeyLength", "Encrypted Key Length", FieldType.DEC, 4),
            f("encryptedKey", "Encrypted Key", FieldType.HEX, 0),
            f("signatureLength", "Signature Length", FieldType.DEC, 4, req = FieldRequirement.OPTIONAL),
            f("signature", "Signature", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL),
            f("initializationValue", "Initialization Value", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL),
        ),
    ),

    // ==================== EMV ====================

    ThalesCommandDefinition(
        code = "KQ", responseCode = "KR", name = "ARQC/ARPC (Static/MasterCard SKD)",
        description = "ARQC verification and/or ARPC generation (Static or MasterCard Proprietary SKD)",
        category = CommandCategory.EMV,
        requestFields = listOf(
            f("modeFlag", "Mode Flag", FieldType.CODE, 1, default = "0", options = listOf(
                CodeOption("0", "0 - Verify ARQC only"),
                CodeOption("1", "1 - Verify ARQC + Generate ARPC"),
                CodeOption("2", "2 - Verify ARQC + Generate ARPC (Method 2)"),
                CodeOption("3", "3 - Generate ARPC only"),
                CodeOption("4", "4 - Generate ARPC only (Method 2)"))),
            f("schemeId", "Scheme ID", FieldType.CODE, 1, default = "0", options = listOf(
                CodeOption("0", "0 - Visa/MasterCard SKD"),
                CodeOption("1", "1 - EMV (Option A)"),
                CodeOption("2", "2 - EMV (Option B)"))),
            f("mkac", "MK-AC", FieldType.HEX, 0, desc = "MK-AC under LMK"),
            f("mksmi", "MK-SMI", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL,
                desc = "MK-SMI under LMK (for discretionary data MAC)"),
            f("panSeqNo", "PAN + Seq No", FieldType.HEX, 8, desc = "8 bytes (16H): PAN + PAN Sequence Number"),
            f("atc", "ATC", FieldType.HEX, 2, desc = "2 bytes (4H)"),
            f("unpredictableNumber", "Unpredictable Number", FieldType.HEX, 4, desc = "4 bytes (8H)"),
            f("transactionDataLength", "Transaction Data Length", FieldType.HEX, 0,
                desc = "Length of transaction data in hex"),
            f("transactionData", "Transaction Data", FieldType.HEX, 0),
            f("delimiter", "Delimiter", FieldType.ASCII, 1, default = ";"),
            f("arqc", "ARQC", FieldType.HEX, 8, desc = "8 bytes (16H)"),
            f("arc", "ARC", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL,
                desc = "Authorization Response Code"),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("arpc", "ARPC", FieldType.HEX, 0),
        ),
    ),

    ThalesCommandDefinition(
        code = "KS", responseCode = "KT", name = "DAC/DN Verification (EMV 3.1.1)",
        description = "Data Authentication Code and Dynamic Number verification",
        category = CommandCategory.EMV,
        requestFields = listOf(
            f("mkMethod", "MK Method", FieldType.DEC, 1, default = "0"),
            f("mkType", "MK Type", FieldType.HEX, 2),
            f("mk", "MK", FieldType.HEX, 0, desc = "Master Key under LMK"),
            f("pan", "PAN", FieldType.DEC, 0),
            f("panSeqNo", "PAN Sequence Number", FieldType.DEC, 2, default = "00"),
            f("dataLength", "Data Length", FieldType.HEX, 4),
            f("data", "Data", FieldType.HEX, 0),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("dac", "DAC", FieldType.HEX, 4),
        ),
    ),

    ThalesCommandDefinition(
        code = "KW", responseCode = "KX", name = "ARQC/ARPC (EMV/Cloud-Based SKD)",
        description = "ARQC verification and/or ARPC generation (EMV or Cloud-Based SKD Methods)",
        category = CommandCategory.EMV,
        requestFields = listOf(
            f("modeFlag", "Mode Flag", FieldType.CODE, 1, default = "1", options = listOf(
                CodeOption("0", "0 - Verify ARQC only"),
                CodeOption("1", "1 - Verify ARQC + Generate ARPC"),
                CodeOption("2", "2 - Verify ARQC + Generate ARPC (Method 2)"),
            )),
            f("schemeId", "Scheme ID", FieldType.CODE, 1, default = "0", options = listOf(
                CodeOption("0", "0 - Visa/MasterCard SKD"),
                CodeOption("1", "1 - EMV (Option A)"),
                CodeOption("2", "2 - EMV (Option B)"),
                CodeOption("3", "3 - Cloud Based Payments"))),
            f("mkac", "MK-AC", FieldType.HEX, 0, desc = "MK-AC under LMK"),
            f("ivac", "IV-AC", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL,
                desc = "IV for AC computation"),
            f("panLength", "PAN Length", FieldType.DEC, 0, req = FieldRequirement.OPTIONAL),
            f("panSeqNo", "PAN + Seq No", FieldType.HEX, 0, desc = "PAN + PAN Sequence Number (binary)"),
            f("delimiter1", "Delimiter 1", FieldType.ASCII, 1, req = FieldRequirement.OPTIONAL, default = ";"),
            f("branchHeightParams", "Branch/Height Params", FieldType.ASCII, 0, req = FieldRequirement.OPTIONAL),
            f("atc", "ATC", FieldType.HEX, 2, desc = "2 bytes (4H)"),
            f("transactionDataLength", "Transaction Data Length", FieldType.HEX, 0,
                desc = "Length of transaction data in hex"),
            f("transactionData", "Transaction Data", FieldType.HEX, 0),
            f("delimiter2", "Delimiter 2", FieldType.ASCII, 1, default = ";"),
            f("arqc", "ARQC", FieldType.HEX, 8, desc = "8 bytes (16H)"),
            f("arc", "ARC", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL),
            f("csu", "CSU", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL,
                desc = "Card Status Update"),
            f("padLength", "Pad Length", FieldType.DEC, 0, req = FieldRequirement.OPTIONAL),
            f("pad", "Pad Data", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("arpc", "ARPC", FieldType.HEX, 0),
        ),
    ),

    // ==================== AS2805 ====================

    ThalesCommandDefinition(
        code = "C0", responseCode = "C1", name = "Generate Initial TMKs (AS2805)",
        description = "Generate two random initial Terminal Master Keys",
        category = CommandCategory.AS2805,
        requestFields = listOf(
            f("kia", "KIA", FieldType.HEX, 0),
            f("keySchemeKIA", "Key Scheme (KIA)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("keySchemeLMK", "Key Scheme (LMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("ppAsnFlag", "PP-ASN Flag", FieldType.DEC, 1, req = FieldRequirement.OPTIONAL),
            f("keyCheckValueType", "KCV Type", FieldType.CODE, 1, req = FieldRequirement.OPTIONAL, options = kcvTypeOptions),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("tmk1UnderKIA", "TMK1 under KIA", FieldType.HEX, 0),
            f("tmk1UnderLMK", "TMK1 under LMK", FieldType.HEX, 0),
            f("tmk1KCV", "TMK1 Check Value", FieldType.HEX, 6),
            f("tmk2UnderKIA", "TMK2 under KIA", FieldType.HEX, 0),
            f("tmk2UnderLMK", "TMK2 under LMK", FieldType.HEX, 0),
            f("tmk2KCV", "TMK2 Check Value", FieldType.HEX, 6),
            f("ppAsnKIA", "PP-ASN (KIA)", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL),
            f("ppAsnLMK", "PP-ASN (LMK)", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL),
        ),
    ),

    ThalesCommandDefinition(
        code = "C6", responseCode = "C7", name = "Generate Random Number",
        description = "Generate a random number",
        category = CommandCategory.AS2805,
        requestFields = emptyList(),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("randomNumber", "Random Number", FieldType.HEX, 16),
        ),
    ),

    ThalesCommandDefinition(
        code = "C8", responseCode = "C9", name = "Generate KIA (AS2805)",
        description = "Generate an Acquirer Master Key Encrypting Key",
        category = CommandCategory.AS2805,
        requestFields = listOf(
            f("kca", "KCA", FieldType.HEX, 0),
            f("aiic", "AIIC", FieldType.DEC, 11),
            f("flag", "AIIC Flag", FieldType.DEC, 1, req = FieldRequirement.OPTIONAL),
            f("keySchemeLMK", "Key Scheme (LMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("keySchemeZMK", "Key Scheme (ZMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions, req = FieldRequirement.OPTIONAL),
            f("keyCheckValueType", "KCV Type", FieldType.CODE, 1, req = FieldRequirement.OPTIONAL, options = kcvTypeOptions),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("kia", "KIA under LMK", FieldType.HEX, 0),
        ),
    ),

    ThalesCommandDefinition(
        code = "F0", responseCode = "F1", name = "Verify Terminal PIN IBM (AS2805)",
        description = "Verify a terminal PIN using IBM method (AS2805 6.4)",
        category = CommandCategory.AS2805,
        requestFields = listOf(
            f("kma", "KMA", FieldType.HEX, 0),
            f("pvk", "PVK", FieldType.HEX, 0),
            f("pinBlock", "PIN Block", FieldType.HEX, 16),
            f("pinBlockFmt", "PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("accountNumber", "Account Number", FieldType.DEC, 12),
            f("decTable", "Decimalization Table", FieldType.HEX, 16),
            f("pinValData", "PIN Validation Data", FieldType.ASCII, 12),
            f("offset", "Offset", FieldType.ASCII, 12),
        ),
        responseFields = listOf(f("errorCode", "Error Code", FieldType.DEC, 2)),
    ),

    ThalesCommandDefinition(
        code = "F2", responseCode = "F3", name = "Verify Terminal PIN VISA (AS2805)",
        description = "Verify a terminal PIN using VISA method (AS2805 6.4)",
        category = CommandCategory.AS2805,
        requestFields = listOf(
            f("kma", "KMA", FieldType.HEX, 0),
            f("pvkPair", "PVK Pair", FieldType.HEX, 0),
            f("pinBlock", "PIN Block", FieldType.HEX, 16),
            f("pinBlockFmt", "PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("accountNumber", "Account Number", FieldType.DEC, 12),
            f("pvki", "PVKI", FieldType.DEC, 1, default = "1"),
            f("pvv", "PVV", FieldType.DEC, 4),
        ),
        responseFields = listOf(f("errorCode", "Error Code", FieldType.DEC, 2)),
    ),

    ThalesCommandDefinition(
        code = "F4", responseCode = "F5", name = "Generate KMACI (AS2805)",
        description = "Generate KMACI for AS2805",
        category = CommandCategory.AS2805,
        requestFields = listOf(
            f("kma", "KMA", FieldType.HEX, 0),
            f("messageData", "Message Data", FieldType.HEX, 0),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("kmaci", "KMACI", FieldType.HEX, 16),
        ),
    ),

    ThalesCommandDefinition(
        code = "H8", responseCode = "H9", name = "Encrypt KCA under KI (AS2805)",
        description = "Encrypt KCA under KI for AS2805",
        category = CommandCategory.AS2805,
        requestFields = listOf(
            f("ki", "KI", FieldType.HEX, 0),
            f("kca", "KCA", FieldType.HEX, 0),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("kcaUnderKI", "KCA under KI", FieldType.HEX, 0),
        ),
    ),

    // ==================== TERMINAL KEYS ====================

    ThalesCommandDefinition(
        code = "D0", responseCode = "D1", name = "Generate PPAC",
        description = "Generate a PIN Pad Authentication Code",
        category = CommandCategory.TERMINAL_KEYS,
        requestFields = listOf(
            f("tpk", "TPK", FieldType.HEX, 0),
            f("kmac", "KMAC", FieldType.HEX, 0),
            f("pinBlock", "PIN Block", FieldType.HEX, 16),
            f("pinBlockFmt", "PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("accountNumber", "Account Number", FieldType.DEC, 12),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("translatedPinBlock", "Translated PIN Block", FieldType.HEX, 16),
            f("kmacKCV", "KMAC KCV", FieldType.HEX, 4),
        ),
    ),

    ThalesCommandDefinition(
        code = "D2", responseCode = "D3", name = "Verify PPAC",
        description = "Verify a PIN Pad Authentication Code",
        category = CommandCategory.TERMINAL_KEYS,
        requestFields = listOf(
            f("tpk", "TPK", FieldType.HEX, 0),
            f("kmac", "KMAC", FieldType.HEX, 0),
            f("pinBlock", "PIN Block", FieldType.HEX, 16),
            f("pinBlockFmt", "PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("accountNumber", "Account Number", FieldType.DEC, 12),
            f("mac", "MAC", FieldType.HEX, 8),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("translatedPinBlock", "Translated PIN Block", FieldType.HEX, 16),
            f("kmacKCV", "KMAC KCV", FieldType.HEX, 4),
        ),
    ),

    ThalesCommandDefinition(
        code = "E0", responseCode = "E1", name = "Generate and Export Key",
        description = "Generate a random key and export it under a KEK (ZMK/TMK)",
        category = CommandCategory.KEY_MANAGEMENT,
        requestFields = listOf(
            f("keyTypeFlag", "Key Type Flag", FieldType.DEC, 3, default = "001", desc = "e.g. 000=ZMK, 001=ZPK, 002=PVK"),
            f("key", "KEK (ZMK/TMK)", FieldType.HEX, 0, desc = "Key-encrypting key under LMK"),
            f("keySchemeLmk", "Key Scheme (LMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("keySchemeKeksZmk", "Key Scheme (KEK/ZMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("kcvType", "KCV Type", FieldType.CODE, 1, default = "0", options = kcvTypeOptions, req = FieldRequirement.OPTIONAL),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("randomKey", "Key under KEK/ZMK", FieldType.HEX, 0),
            f("invertedKey", "Key under LMK", FieldType.HEX, 0),
        ),
    ),

    ThalesCommandDefinition(
        code = "E2", responseCode = "E3", name = "Import Key",
        description = "Import a key encrypted under a KEK (ZMK/TMK) and re-encrypt under LMK",
        category = CommandCategory.KEY_MANAGEMENT,
        requestFields = listOf(
            f("keyTypeFlag", "Key Type Flag", FieldType.DEC, 3, default = "001", desc = "e.g. 000=ZMK, 001=ZPK, 002=PVK"),
            f("key", "KEK (ZMK/TMK)", FieldType.HEX, 0, desc = "Key-encrypting key under LMK"),
            f("randomKey", "Key under KEK", FieldType.HEX, 0, desc = "Key encrypted under the KEK"),
            f("keySchemeLmk", "Key Scheme (LMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("keySchemeKekr", "Key Scheme (KEKr)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("kcvType", "KCV Type", FieldType.CODE, 1, default = "0", options = kcvTypeOptions, req = FieldRequirement.OPTIONAL),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("invertedKey", "Key under LMK", FieldType.HEX, 0),
        ),
    ),

    ThalesCommandDefinition(
        code = "E4", responseCode = "E5", name = "Verify PIN Pad Proof of End Point",
        description = "Verify a PIN pad proof of end point",
        category = CommandCategory.TERMINAL_KEYS,
        requestFields = listOf(
            f("tmk", "TMK", FieldType.HEX, 0),
            f("krsUnderTMK", "KRs under TMK", FieldType.HEX, 0),
            f("krrUnderLMK", "KRr under LMK", FieldType.HEX, 0),
            f("poep", "POEP", FieldType.HEX, 16),
        ),
        responseFields = listOf(f("errorCode", "Error Code", FieldType.DEC, 2)),
    ),

    ThalesCommandDefinition(
        code = "E6", responseCode = "E7", name = "Verify TMK/TEK (POEP)",
        description = "Verify a TMK or TEK using proof of end point",
        category = CommandCategory.TERMINAL_KEYS,
        requestFields = listOf(
            f("flag", "Flag", FieldType.CODE, 1, default = "1", options = listOf(
                CodeOption("1", "1 - TMK1"), CodeOption("2", "2 - TMK2"), CodeOption("3", "3 - TEK"))),
            f("tmkOrTek", "TMK/TEK", FieldType.HEX, 0, desc = "TMK or TEK under LMK"),
            f("ppAsn", "PP-ASN", FieldType.HEX, 0),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("poep", "POEP", FieldType.HEX, 0),
        ),
    ),

    ThalesCommandDefinition(
        code = "OI", responseCode = "OJ", name = "Generate AS2805 Terminal Key Sets",
        description = "Generate terminal key sets (ZPK, ZEK, ZAK) under KEKs",
        category = CommandCategory.AS2805,
        requestFields = listOf(
            f("key", "KEK", FieldType.HEX, 0),
            f("keyTypeFlag", "Key Type Flag", FieldType.DEC, 3, default = "000"),
            f("keySchemeLmk", "Key Scheme (LMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("keySchemeKeksZmk", "Key Scheme (KEK/ZMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("kcvType", "KCV Type", FieldType.CODE, 1, default = "0", options = kcvTypeOptions, req = FieldRequirement.OPTIONAL),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("authKeyLmk", "Auth Key under LMK", FieldType.HEX, 0),
            f("authKeyZmk", "Auth Key under ZMK", FieldType.HEX, 0),
            f("encKeyLmk", "Enc Key under LMK", FieldType.HEX, 0),
            f("encKeyZmk", "Enc Key under ZMK", FieldType.HEX, 0),
            f("pinKeyLmk", "PIN Key under LMK", FieldType.HEX, 0),
            f("pinKeyZmk", "PIN Key under ZMK", FieldType.HEX, 0),
            f("zpkCheckValue", "ZPK Check Value", FieldType.HEX, 6),
            f("zekCheckValue", "ZEK Check Value", FieldType.HEX, 6),
            f("zakCheckValue", "ZAK Check Value", FieldType.HEX, 6),
        ),
    ),

    ThalesCommandDefinition(
        code = "OK", responseCode = "OL", name = "Import AS2805 Terminal Key Sets",
        description = "Import terminal key sets (ZPK, ZEK, ZAK) under a ZMK",
        category = CommandCategory.AS2805,
        requestFields = listOf(
            f("key", "ZMK/KEK", FieldType.HEX, 0),
            f("keyTypeFlag", "Key Type Flag", FieldType.DEC, 3, default = "000"),
            f("keySchemeLmk", "Key Scheme (LMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("keySchemeZmk", "Key Scheme (ZMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("kcvType", "KCV Type", FieldType.CODE, 1, default = "0", options = kcvTypeOptions, req = FieldRequirement.OPTIONAL),
            f("kcvProcessingFlag", "KCV Processing Flag", FieldType.CODE, 1, default = "0", options = listOf(
                CodeOption("0", "0 - No KCV processing"), CodeOption("1", "1 - Check KCV"),
                CodeOption("2", "2 - Generate KCV"))),
            f("zpk", "ZPK under ZMK", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL),
            f("zpkFlag", "ZPK Flag", FieldType.DEC, 1, req = FieldRequirement.OPTIONAL),
            f("zpkCheckValue", "ZPK Check Value", FieldType.HEX, 6, req = FieldRequirement.OPTIONAL),
            f("zek", "ZEK under ZMK", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL),
            f("zekFlag", "ZEK Flag", FieldType.DEC, 1, req = FieldRequirement.OPTIONAL),
            f("zekCheckValue", "ZEK Check Value", FieldType.HEX, 6, req = FieldRequirement.OPTIONAL),
            f("zak", "ZAK under ZMK", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL),
            f("zakFlag", "ZAK Flag", FieldType.DEC, 1, req = FieldRequirement.OPTIONAL),
            f("zakCheckValue", "ZAK Check Value", FieldType.HEX, 6, req = FieldRequirement.OPTIONAL),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("zpk", "ZPK under LMK", FieldType.HEX, 0),
            f("zpkCheckValue", "ZPK Check Value", FieldType.HEX, 6),
            f("zek", "ZEK under LMK", FieldType.HEX, 0),
            f("zekCheckValue", "ZEK Check Value", FieldType.HEX, 6),
            f("zak", "ZAK under LMK", FieldType.HEX, 0),
            f("zakCheckValue", "ZAK Check Value", FieldType.HEX, 6),
            f("kcvProcessingFlag", "KCV Processing Flag", FieldType.DEC, 1, req = FieldRequirement.OPTIONAL),
        ),
    ),

    ThalesCommandDefinition(
        code = "OU", responseCode = "OV", name = "Update TMK1",
        description = "Update Terminal Master Key 1",
        category = CommandCategory.TERMINAL_KEYS,
        requestFields = listOf(
            f("ppAsn", "PP-ASN", FieldType.HEX, 0),
            f("terminalMasterKey1", "TMK1", FieldType.HEX, 0),
            f("keyUpdateProcess", "Key Update Process", FieldType.DEC, 1, req = FieldRequirement.OPTIONAL),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("terminalMasterKey1", "TMK1 under LMK", FieldType.HEX, 0),
            f("tmk1CheckValue", "TMK1 Check Value", FieldType.HEX, 6),
        ),
    ),

    ThalesCommandDefinition(
        code = "OW", responseCode = "OX", name = "Update TMK2",
        description = "Update Terminal Master Key 2",
        category = CommandCategory.TERMINAL_KEYS,
        requestFields = listOf(
            f("ppAsn", "PP-ASN", FieldType.HEX, 0),
            f("terminalMasterKey2", "TMK2", FieldType.HEX, 0),
            f("keyUpdateProcess", "Key Update Process", FieldType.DEC, 1, req = FieldRequirement.OPTIONAL),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("terminalMasterKey1", "TMK1 under LMK", FieldType.HEX, 0),
            f("terminalMasterKey2", "TMK2 under LMK", FieldType.HEX, 0),
            f("tmk1CheckValue", "TMK1 Check Value", FieldType.HEX, 6),
            f("tmk2CheckValue", "TMK2 Check Value", FieldType.HEX, 6),
        ),
    ),

    ThalesCommandDefinition(
        code = "PI", responseCode = "PJ", name = "Generate Terminal Crypto Keys",
        description = "Generate terminal authentication, encryption, and PIN keys",
        category = CommandCategory.TERMINAL_KEYS,
        requestFields = listOf(
            f("terminalMasterKey", "Terminal Master Key", FieldType.HEX, 0),
            f("flag", "Flag", FieldType.CODE, 1, default = "1", options = listOf(
                CodeOption("0", "0 - KMA"), CodeOption("1", "1 - TMK1"), CodeOption("2", "2 - TMK2"))),
            f("keySchemeLmk", "Key Scheme (LMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("keySchemeTmk", "Key Scheme (TMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("keyCheckValueType", "KCV Type", FieldType.CODE, 1, default = "0", options = kcvTypeOptions, req = FieldRequirement.OPTIONAL),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("authKeyLmk", "Auth Key under LMK", FieldType.HEX, 0),
            f("authKeyTmk", "Auth Key under TMK", FieldType.HEX, 0),
            f("encKeyLmk", "Enc Key under LMK", FieldType.HEX, 0),
            f("encKeyTmk", "Enc Key under TMK", FieldType.HEX, 0),
            f("pinKeyLmk", "PIN Key under LMK", FieldType.HEX, 0),
            f("pinKeyTmk", "PIN Key under TMK", FieldType.HEX, 0),
            f("tpkCheckValue", "TPK Check Value", FieldType.HEX, 6),
            f("takCheckValue", "TAK Check Value", FieldType.HEX, 6),
            f("tekCheckValue", "TEK Check Value", FieldType.HEX, 6),
        ),
    ),

    ThalesCommandDefinition(
        code = "PO", responseCode = "PP", name = "Translate PIN (AS2805)",
        description = "Translate a PIN block from terminal PIN key to zone PIN key",
        category = CommandCategory.AS2805,
        requestFields = listOf(
            f("terminalPinKey", "Terminal PIN Key", FieldType.HEX, 0),
            f("zonePinKey", "Zone PIN Key", FieldType.HEX, 0),
            f("incomingPinBlock", "Incoming PIN Block", FieldType.HEX, 16),
            f("incomingPinBlockFmtCode", "Incoming PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("outgoingPinBlockFmtCode", "Outgoing PIN Block Format", FieldType.CODE, 2, default = "01", options = pinBlockFormatOptions),
            f("accountNumber", "Account Number", FieldType.DEC, 12),
            f("stan", "STAN", FieldType.DEC, 12, req = FieldRequirement.OPTIONAL),
            f("transactionAmount", "Transaction Amount", FieldType.DEC, 12, req = FieldRequirement.OPTIONAL),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("outgoingPinBlock", "Outgoing PIN Block", FieldType.HEX, 16),
        ),
    ),

    ThalesCommandDefinition(
        code = "ES", responseCode = "ET", name = "Generate RSA Certificate",
        description = "Generate an RSA certificate (sign a public key)",
        category = CommandCategory.RSA,
        requestFields = listOf(
            f("hashIdentifier", "Hash Identifier", FieldType.CODE, 2, default = "01", options = hashAlgorithmOptions),
            f("signatureIdentifier", "Signature Identifier", FieldType.CODE, 2, default = "01"),
            f("padModeIdentifier", "Pad Mode Identifier", FieldType.DEC, 2, default = "01"),
            f("privateKeyLength", "Private Key Length", FieldType.DEC, 4),
            f("privateKey", "Private Key under LMK", FieldType.HEX, 0),
            f("publicKeyEncoding", "Public Key Encoding", FieldType.DEC, 2, default = "01"),
            f("publicKey", "Public Key (to certify)", FieldType.HEX, 0),
            f("certificateLength", "Certificate Length", FieldType.DEC, 4, req = FieldRequirement.OPTIONAL),
            f("certificate", "Certificate Template", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL),
            f("mac", "MAC", FieldType.HEX, 4, req = FieldRequirement.OPTIONAL),
            f("authenticationData", "Authentication Data", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL),
            f("authDataForImportedKey", "Auth Data for Imported Key", FieldType.HEX, 0, req = FieldRequirement.OPTIONAL),
            f("signatureOffset", "Signature Offset", FieldType.DEC, 4, req = FieldRequirement.OPTIONAL),
            f("signatureLength", "Signature Length", FieldType.DEC, 4, req = FieldRequirement.OPTIONAL),
            f("hashOffset", "Hash Offset", FieldType.DEC, 4, req = FieldRequirement.OPTIONAL),
            f("hashLength", "Hash Length", FieldType.DEC, 4, req = FieldRequirement.OPTIONAL),
            f("publicKeyOffset", "Public Key Offset", FieldType.DEC, 4, req = FieldRequirement.OPTIONAL),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("mac", "MAC", FieldType.HEX, 4),
            f("publicKey", "Certified Public Key", FieldType.HEX, 0),
        ),
    ),

    // ==================== HMAC ====================

    ThalesCommandDefinition(
        code = "L0", responseCode = "L1", name = "Generate HMAC Secret Key",
        description = "Generate a private key for HMAC",
        category = CommandCategory.HMAC,
        requestFields = listOf(
            f("mode", "Mode", FieldType.CODE, 1, default = "0", options = listOf(
                CodeOption("0", "0 - LMK only"), CodeOption("1", "1 - LMK + ZMK"))),
            f("keyType", "Key Type", FieldType.CODE, 3, default = "001", options = keyTypeOptions),
            f("keyScheme", "Key Scheme (LMK)", FieldType.CODE, 1, default = "U", options = keySchemeOptions),
            f("keyLength", "Key Length (bits)", FieldType.DEC, 4, default = "0256"),
            f("zmkScheme", "Key Scheme (ZMK)", FieldType.CODE, 1, FieldRequirement.CONDITIONAL,
                options = keySchemeOptions, cond = FieldCondition("mode", setOf("1"))),
            f("zmk", "ZMK", FieldType.HEX, 32, FieldRequirement.CONDITIONAL,
                cond = FieldCondition("mode", setOf("1"))),
        ) + keyBlockFields(defaultUsage = "51", defaultModeOfUse = "E", defaultExportability = "S"),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("keyUnderLMK", "Key under LMK", FieldType.HEX, 0),
            f("keyUnderZMK", "Key under ZMK", FieldType.HEX, 0),
            f("kcv", "Key Check Value", FieldType.HEX, 6),
        ),
    ),

    // ==================== ADMIN ====================

    ThalesCommandDefinition(
        code = "LO", responseCode = "LP", name = "Translate Decimalisation Table",
        description = "Translate a decimalisation table from old LMK to new LMK",
        category = CommandCategory.ADMIN,
        requestFields = listOf(
            f("decimalisationTable", "Decimalisation Table", FieldType.HEX, 16),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("decimalisationTableTranslated", "Translated Decimalisation Table", FieldType.HEX, 16),
        ),
    ),

    ThalesCommandDefinition(
        code = "QK", responseCode = "QL", name = "Translate Account Number",
        description = "Translate account number for LMK-encrypted PIN",
        category = CommandCategory.PIN_OPERATIONS,
        requestFields = listOf(
            f("pin", "PIN under LMK", FieldType.HEX, 0),
            f("oldAccountNumber", "Old Account Number", FieldType.DEC, 12),
            f("newAccountNumber", "New Account Number", FieldType.DEC, 12),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("pin", "PIN under LMK (new)", FieldType.HEX, 0),
        ),
    ),

    ThalesCommandDefinition(
        code = "TA", responseCode = "TB", name = "Print TMK Mailer",
        description = "Print a TMK mailer",
        category = CommandCategory.ADMIN,
        requestFields = listOf(
            f("tmk", "TMK", FieldType.HEX, 0),
            f("printFields", "Print Fields", FieldType.ASCII, 0, req = FieldRequirement.OPTIONAL),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
        ),
    ),

    ThalesCommandDefinition(
        code = "PA", responseCode = "PB", name = "Load Formatting Data",
        description = "Load formatting data to HSM",
        category = CommandCategory.ADMIN,
        requestFields = listOf(
            f("data", "Formatting Data", FieldType.ASCII, 0, desc = "Formatting data to load"),
        ),
        responseFields = listOf(f("errorCode", "Error Code", FieldType.DEC, 2)),
    ),

    ThalesCommandDefinition(
        code = "PE", responseCode = "PF", name = "Print PIN",
        description = "Print PIN / PIN and solicitation data",
        category = CommandCategory.ADMIN,
        requestFields = listOf(
            f("documentType", "Document Type", FieldType.DEC, 1, default = "1"),
            f("account", "Account Number", FieldType.DEC, 12),
            f("pin", "PIN under LMK", FieldType.HEX, 0),
            f("printFields", "Print Fields", FieldType.ASCII, 0, req = FieldRequirement.OPTIONAL,
                desc = "Terminated by ~ character"),
            f("lmkIdentifier", "LMK Identifier", FieldType.CODE, 0, req = FieldRequirement.OPTIONAL, options = lmkIdentifierOptions),
        ),
        responseFields = listOf(f("errorCode", "Error Code", FieldType.DEC, 2)),
    ),

    ThalesCommandDefinition(
        code = "NK", responseCode = "NL", name = "Command Chaining",
        description = "Bundle up to 99 commands together",
        category = CommandCategory.ADMIN,
        requestFields = listOf(
            f("numSubCommands", "Number of Sub-commands", FieldType.DEC, 2, default = "01"),
            f("subCommands", "Sub-commands", FieldType.HEX, 0),
        ),
        responseFields = listOf(
            f("errorCode", "Error Code", FieldType.DEC, 2),
            f("subCmdResponses", "Sub-command Responses", FieldType.HEX, 0),
        ),
    ),
)

val thalesCommandDefinitionMap: Map<String, ThalesCommandDefinition> =
    thalesCommandDefinitions.associateBy { it.code }
