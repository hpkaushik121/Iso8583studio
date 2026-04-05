package `in`.aicortex.iso8583studio.ui.screens.hsmSimulator

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.domain.service.hsmSimulatorService.HsmServiceImpl
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Data model
// ─────────────────────────────────────────────────────────────────────────────

enum class HostParamType { TEXT, HEX, DROPDOWN, NUMBER, DELIMITER }

data class HostParamOption(val label: String, val value: String)

data class HostCommandParam(
    val id: String,
    val label: String,
    val description: String = "",
    val type: HostParamType = HostParamType.TEXT,
    val default: String = "",
    val required: Boolean = true,
    val placeholder: String = "",
    val options: List<HostParamOption> = emptyList(),
    val maxLength: Int = 256,
    /** For DELIMITER type: the delimiter char to wire (e.g. ";" or "%"). */
    val delimiterChar: String = ";",
    /** Non-empty = this param belongs to a delimiter group; only shown/wired when delimiter is checked. */
    val delimiterGroup: String = "",
)

enum class HostCommandCategory(
    val label: String,
    val icon: ImageVector,
    val color: Color
) {
    DIAGNOSTICS("Diagnostics",    Icons.Default.HealthAndSafety,  Color(0xFF607D8B)),
    PIN_TRANS  ("PIN Translation", Icons.Default.SwapHoriz,        Color(0xFF2196F3)),
    PIN_VERIFY ("PIN Verification",Icons.Default.VerifiedUser,     Color(0xFF4CAF50)),
    PIN_GEN    ("PIN Generation",  Icons.Default.Pin,              Color(0xFFFF9800)),
    KEY_MGMT   ("Key Management",  Icons.Default.VpnKey,           Color(0xFF9C27B0)),
    ENCRYPTION ("Encryption",      Icons.Default.EnhancedEncryption,Color(0xFF00BCD4)),
    MAC_OPS    ("MAC Operations",  Icons.Default.Security,         Color(0xFFE91E63)),
    HASH_OPS   ("Hash / Digest",   Icons.Default.Tag,              Color(0xFF795548)),
    RSA_CRYPTO ("RSA / Signature", Icons.Default.GppGood,          Color(0xFF3F51B5)),
    CVV_VERIFY ("CVV Verification",Icons.Default.CreditCard,       Color(0xFFFF5722)),
    USER_STORE ("User Storage",    Icons.Default.Storage,          Color(0xFF009688)),
}

data class HostCommand(
    val code: String,
    val responseCode: String,
    val name: String,
    val description: String,
    val category: HostCommandCategory,
    val params: List<HostCommandParam> = emptyList(),
    val wireFormatHint: String = "",
)

// ─────────────────────────────────────────────────────────────────────────────
// Reusable option lists
// ─────────────────────────────────────────────────────────────────────────────

private val KEY_SCHEME_OPTIONS = listOf(
    HostParamOption("U  – Double-length 3DES  (32H)",  "U"),
    HostParamOption("T  – Triple-length 3DES  (48H)",  "T"),
    HostParamOption("X  – Single DES          (16H)",  "X"),
)
private val PIN_BLOCK_FORMAT_OPTIONS = listOf(
    HostParamOption("01 – ISO 9564-1 & ANSI X9.8 format 0", "01"),
    HostParamOption("02 – Docutel ATM format",               "02"),
    HostParamOption("03 – Diebold & IBM ATM format",         "03"),
    HostParamOption("04 – PLUS Network format",              "04"),
    HostParamOption("05 – ISO 9564-1 format 1",             "05"),
    HostParamOption("46 – AS2805",                           "46"),
    HostParamOption("47 – ISO 9564-1 & ANSI X9.8 format 3", "47"),
    HostParamOption("48 – ISO 9564-1 format 4",             "48"),
)
private val KEY_TYPE_OPTIONS = listOf(
    HostParamOption("000 – ZMK  (Zone Master Key)",       "000"),
    HostParamOption("001 – ZPK  (Zone PIN Key)",          "001"),
    HostParamOption("002 – PVK / TPK",                    "002"),
    HostParamOption("003 – TAK  (Terminal Auth Key)",     "003"),
    HostParamOption("008 – ZAK  (Zone Auth Key)",         "008"),
    HostParamOption("009 – BDK  (Base Derivation Key)",   "009"),
    HostParamOption("00A – ZEK  (Zone Encryption Key)",   "00A"),
    HostParamOption("00B – DEK  (Data Encryption Key)",   "00B"),
    HostParamOption("302 – IKEY (DUKPT Initial Key)",     "302"),
)
private val HASH_ALGO_OPTIONS = listOf(
    HostParamOption("01 – SHA-1",    "01"),
    HostParamOption("02 – MD5",      "02"),
    HostParamOption("05 – SHA-224",  "05"),
    HostParamOption("06 – SHA-256",  "06"),
    HostParamOption("07 – SHA-384",  "07"),
    HostParamOption("08 – SHA-512",  "08"),
)
private val MAC_ALGO_OPTIONS = listOf(
    HostParamOption("1 – ISO 9797 Alg 1 (ANSI X9.9)", "1"),
    HostParamOption("3 – ISO 9797 Alg 3 (retail MAC)", "3"),
    HostParamOption("6 – HMAC",                         "6"),
)
private val MAC_PADDING_OPTIONS = listOf(
    HostParamOption("0 – No padding",         "0"),
    HostParamOption("1 – ISO 9797 Method 1",  "1"),
    HostParamOption("2 – ISO 9797 Method 2",  "2"),
)
private val CIPHER_MODE_OPTIONS = listOf(
    HostParamOption("00 – ECB", "00"),
    HostParamOption("01 – CBC", "01"),
)
private val RSA_HASH_OPTIONS = listOf(
    HostParamOption("01 – SHA-1",   "01"),
    HostParamOption("04 – No hash", "04"),
    HostParamOption("05 – SHA-224", "05"),
    HostParamOption("06 – SHA-256", "06"),
    HostParamOption("07 – SHA-384", "07"),
    HostParamOption("08 – SHA-512", "08"),
)
private val RSA_PAD_OPTIONS = listOf(
    HostParamOption("01 – PKCS#1 v1.5", "01"),
    HostParamOption("02 – OAEP",         "02"),
    HostParamOption("04 – EMSA-PSS",     "04"),
)

// ─────────────────────────────────────────────────────────────────────────────
// HOST COMMAND DEFINITIONS
// ─────────────────────────────────────────────────────────────────────────────

val HOST_COMMANDS: List<HostCommand> = listOf(

    // ── Diagnostics ──────────────────────────────────────────────────────────

    HostCommand(
        code = "NC", responseCode = "ND", name = "Diagnostic Test",
        category = HostCommandCategory.DIAGNOSTICS,
        description = "Health-check command. Confirms HSM connectivity and LMK status.",
        wireFormatHint = "0000NC  →  0000ND 00 [status]"
    ),

    // ── PIN Translation ───────────────────────────────────────────────────────

    HostCommand(
        code = "CA", responseCode = "CB", name = "Translate PIN: TPK → ZPK",
        category = HostCommandCategory.PIN_TRANS,
        description = "Re-encrypts a terminal PIN block from TPK to ZPK for network transmission.",
        wireFormatHint = "0000CA [TPK_1A+32H] [ZPK_1A+32H] [MaxPINLen_2N] [PINBlock_16H] [SrcFormat_2N] [DstFormat_2N] [Account_12N]",
        params = listOf(
            HostCommandParam("tpk",       "TPK (under LMK)",          "Terminal PIN Key — prefix U/T/X + 32/48/16 hex", HostParamType.TEXT,    "", true,  "U1234567890ABCDEF1234567890ABCDEF"),
            HostCommandParam("zpk",       "ZPK (under LMK)",          "Zone PIN Key — prefix U/T/X + key hex",           HostParamType.TEXT,    "", true,  "UFEDCBA9876543210FEDCBA9876543210"),
            HostCommandParam("maxPinLen", "Max PIN Length",            "04 – 12",                                         HostParamType.NUMBER,  "12", true, "12"),
            HostCommandParam("pinBlock",  "Source PIN Block",          "16 hex chars, encrypted under TPK",               HostParamType.HEX,     "", true,  "0412AC3700000000"),
            HostCommandParam("srcFmt",    "Source Format",             "PIN block format code",                           HostParamType.DROPDOWN,"01", true,  "", PIN_BLOCK_FORMAT_OPTIONS),
            HostCommandParam("dstFmt",    "Destination Format",        "PIN block format code",                           HostParamType.DROPDOWN,"01", true,  "", PIN_BLOCK_FORMAT_OPTIONS),
            HostCommandParam("account",   "Account Number (12N)",      "12 rightmost PAN digits excl. check digit",       HostParamType.HEX,     "", true,  "123456789012"),
        )
    ),

    HostCommand(
        code = "G0", responseCode = "G1", name = "Translate PIN: DUKPT (BDK) → BDK/ZPK",
        category = HostCommandCategory.PIN_TRANS,
        description = "Derives DUKPT session key from source BDK+KSN, decrypts PIN block, re-encrypts under destination ZPK or DUKPT-derived key.",
        wireFormatHint = "0000G0 [BDK_1A+32H] [DestKeyType_1A] [ZPK/BDK_1A+32H] [SrcKSNDesc_3H] [SrcKSN_20H] [DstKSNDesc_3H?] [DstKSN_20H?] [PINBlock_16H] [SrcFmt_2N] [DstFmt_2N] [Account_12N]",
        params = listOf(
            HostCommandParam("bdk",         "BDK (under LMK)",              "Source Base Derivation Key encrypted under LMK pair 28-29. Used with source KSN to derive the DUKPT session key that decrypts the incoming PIN block.",
                HostParamType.TEXT,    "", true,  "U0123456789ABCDEF0123456789ABCDEF"),
            HostCommandParam("destKeyType", "Destination Key Type",         "'0' = Not Set (destination is ZPK). '*' = BDK-1. '~' = BDK-2. '!' = BDK-4. When BDK is selected, destination KSN fields are required.",
                HostParamType.DROPDOWN,"0", true, "", listOf(
                HostParamOption("'0' - Not Set (ZPK)", "0"),
                HostParamOption("'*' (X'2A) - BDK-1", "*"),
                HostParamOption("'~' (X'7E) - BDK-2", "~"),
                HostParamOption("'!' (X'21) - BDK-4", "!"),
            )),
            HostCommandParam("destKey",     "ZPK / Dest BDK (under LMK)",  "Destination key under LMK. If Dest Key Type = '0', this is a ZPK under LMK pair 06-07. Otherwise, this is a BDK under LMK pair 28-29.",
                HostParamType.TEXT,    "", true,  "UFEDCBA9876543210FEDCBA9876543210"),
            HostCommandParam("srcKsnDesc",  "Source KSN Descriptor",        "3-digit KSN descriptor for the source BDK. Format: BDK_ID_len + Sub-Key_len + Device_ID_len (e.g. 906 = 9 bits BDK-ID, 0 sub-key, 6 device-ID). Determines counter bit width.",
                HostParamType.HEX,     "906", true, "906"),
            HostCommandParam("srcKsn",      "Key Serial Number",            "20H source Key Serial Number from the terminal. Includes BDK-ID, device-ID, and transaction counter.",
                HostParamType.HEX,     "FFFF0123456789E00002", true,  "FFFF0123456789E00002"),
            HostCommandParam("dstKsnDesc",  "Destination KSN Descriptor",   "3-digit KSN descriptor for the destination BDK. Only required when Destination Key Type is BDK (*,~,!).",
                HostParamType.HEX,     "906", false, "906"),
            HostCommandParam("dstKsn",      "Destination Key Serial Number","20H destination KSN. Only required when Destination Key Type is BDK (*,~,!). Used to derive the destination DUKPT session key.",
                HostParamType.HEX,     "FFFF0123456789E00002", false,  "FFFF0123456789E00002"),
            HostCommandParam("pinBlock",    "Source Encrypted Block",       "16H PIN block encrypted under the source DUKPT-derived session key (PEK variant).",
                HostParamType.HEX,     "", true,  "7C6E2C03F30AADBF"),
            HostCommandParam("srcFmt",      "Source PIN Block Format",      "PIN block format of the incoming encrypted PIN block.",
                HostParamType.DROPDOWN,"01", true, "", PIN_BLOCK_FORMAT_OPTIONS),
            HostCommandParam("dstFmt",      "Destination Format Code",      "PIN block format for the re-encrypted output PIN block.",
                HostParamType.DROPDOWN,"01", true, "", PIN_BLOCK_FORMAT_OPTIONS),
            HostCommandParam("account",     "Account Number",               "12 rightmost digits of the PAN excluding the check digit. Used for PIN block format 0/3 XOR masking.",
                HostParamType.HEX,     "999999999999", true,  "999999999999"),
        )
    ),

    HostCommand(
        code = "JC", responseCode = "JD", name = "Translate PIN: TPK → LMK",
        category = HostCommandCategory.PIN_TRANS,
        description = "Decrypts terminal PIN block and re-encrypts PIN under LMK pair 02-03.",
        wireFormatHint = "0000JC [TPK_1A+32H] [PINBlock_16H] [SrcFmt_2N] [Account_12N]",
        params = listOf(
            HostCommandParam("tpk",     "TPK (under LMK)",      "Terminal PIN Key",                           HostParamType.TEXT,    "", true, "U1234567890ABCDEF1234567890ABCDEF"),
            HostCommandParam("pinBlock","PIN Block (16H)",       "Encrypted under TPK",                        HostParamType.HEX,     "", true, "0412AC3700000000"),
            HostCommandParam("srcFmt",  "Source Format",         "",                                           HostParamType.DROPDOWN,"01", true, "", PIN_BLOCK_FORMAT_OPTIONS),
            HostCommandParam("account", "Account Number (12N)",  "12 rightmost PAN digits excl. check digit",  HostParamType.HEX,     "", true, "123456789012"),
        )
    ),

    HostCommand(
        code = "JE", responseCode = "JF", name = "Translate PIN: ZPK → LMK",
        category = HostCommandCategory.PIN_TRANS,
        description = "Converts a ZPK-encrypted PIN block to an LMK-encrypted PIN.",
        wireFormatHint = "0000JE [ZPK_1A+32H] [PINBlock_16H] [SrcFmt_2N] [Account_12N]",
        params = listOf(
            HostCommandParam("zpk",     "ZPK (under LMK)",      "Zone PIN Key",                               HostParamType.TEXT,    "", true, "U1234567890ABCDEF1234567890ABCDEF"),
            HostCommandParam("pinBlock","PIN Block (16H)",       "Encrypted under ZPK",                        HostParamType.HEX,     "", true, "0412AC3700000000"),
            HostCommandParam("srcFmt",  "Source Format",         "",                                           HostParamType.DROPDOWN,"01", true, "", PIN_BLOCK_FORMAT_OPTIONS),
            HostCommandParam("account", "Account Number (12N)",  "12 rightmost PAN digits excl. check digit",  HostParamType.HEX,     "", true, "123456789012"),
        )
    ),

    HostCommand(
        code = "JG", responseCode = "JH", name = "Translate PIN: LMK → ZPK",
        category = HostCommandCategory.PIN_TRANS,
        description = "Converts an LMK-encrypted PIN to a ZPK-encrypted PIN block for network transmission.",
        wireFormatHint = "0000JG [ZPK_1A+32H] [DstFmt_2N] [Account_12N] [LMK_PIN]",
        params = listOf(
            HostCommandParam("zpk",     "ZPK (under LMK)",       "Zone PIN Key",                               HostParamType.TEXT,    "", true, "U1234567890ABCDEF1234567890ABCDEF"),
            HostCommandParam("dstFmt",  "Destination Format",    "",                                           HostParamType.DROPDOWN,"01", true, "", PIN_BLOCK_FORMAT_OPTIONS),
            HostCommandParam("account", "Account Number (12N)",  "12 rightmost PAN digits excl. check digit",  HostParamType.HEX,     "", true, "123456789012"),
            HostCommandParam("lmkPin",  "LMK-Encrypted PIN",     "PIN encrypted under LMK pair 02-03",         HostParamType.TEXT,    "", true, "041234"),
        )
    ),

    // ── PIN Verification ──────────────────────────────────────────────────────

    HostCommand(
        code = "DA", responseCode = "DB", name = "Verify PIN (IBM 3624)",
        category = HostCommandCategory.PIN_VERIFY,
        description = "Verifies a TPK-encrypted PIN using the IBM 3624 natural PIN method.",
        wireFormatHint = "0000DA [TPK_1A+32H] [PVK_16H] [MaxPIN_2N] [PINBlock_16H] [PBFmt_2N] [Account_12N] [DecTable_16H] [PINValData_12A] [Offset_12H]",
        params = listOf(
            HostCommandParam("tpk",       "TPK (under LMK)",        "Terminal PIN Key",                         HostParamType.TEXT,   "", true,  "U1234567890ABCDEF1234567890ABCDEF"),
            HostCommandParam("pvk",       "PVK (16H)",              "PIN Verification Key, single-DES",         HostParamType.HEX,    "", true,  "0123456789ABCDEF"),
            HostCommandParam("maxPinLen", "Max PIN Length",          "04–12",                                    HostParamType.NUMBER, "12", true, "12"),
            HostCommandParam("pinBlock",  "PIN Block (16H)",         "Encrypted under TPK",                      HostParamType.HEX,    "", true,  "0412AC3700000000"),
            HostCommandParam("pinFmt",    "PIN Block Format",        "",                                         HostParamType.DROPDOWN,"01",true, "", PIN_BLOCK_FORMAT_OPTIONS),
            HostCommandParam("account",   "Account Number (12N)",    "12 rightmost digits excl. check",          HostParamType.HEX,    "", true,  "123456789012"),
            HostCommandParam("decTable",  "Decimalization Table (16H)","Encrypted decimalization table",         HostParamType.HEX,    "0123456789012345", true, "0123456789012345"),
            HostCommandParam("pinValData","PIN Validation Data (12A)","N = last 5 PAN digits",                   HostParamType.TEXT,   "FFFFFFFFFFFN", true, "FFFFFFFFFFFN"),
            HostCommandParam("offset",    "PIN Offset (12H)",         "IBM 3624 offset, F-padded",               HostParamType.HEX,    "FFFFFFFFFFFF", true, "FFFFFFFFFFFF"),
        )
    ),

    HostCommand(
        code = "DC", responseCode = "DD", name = "Verify PIN (VISA PVV)",
        category = HostCommandCategory.PIN_VERIFY,
        description = "Verifies a TPK-encrypted PIN using the VISA PVV method.",
        wireFormatHint = "0000DC [TPK_1A+32H] [PVKPair_32H] [PINBlock_16H] [PBFmt_2N] [Account_12N] [PVKI_1N] [PVV_4N]",
        params = listOf(
            HostCommandParam("tpk",      "TPK (under LMK)",       "Terminal PIN Key",                          HostParamType.TEXT,    "", true, "U1234567890ABCDEF1234567890ABCDEF"),
            HostCommandParam("pvkPair",  "PVK Pair (32H)",        "Two single-length DES keys under LMK 14-15",HostParamType.HEX,    "", true, "0123456789ABCDEFFEDCBA9876543210"),
            HostCommandParam("pinBlock", "PIN Block (16H)",        "Encrypted under TPK",                       HostParamType.HEX,    "", true, "0412AC3700000000"),
            HostCommandParam("pinFmt",   "PIN Block Format",       "",                                          HostParamType.DROPDOWN,"01",true, "", PIN_BLOCK_FORMAT_OPTIONS),
            HostCommandParam("account",  "Account Number (12N)",   "12 rightmost digits excl. check",           HostParamType.HEX,    "", true, "123456789012"),
            HostCommandParam("pvki",     "PVKI (0–6)",             "PIN Verification Key Indicator",            HostParamType.NUMBER, "1", true, "1"),
            HostCommandParam("pvv",      "PVV (4N)",               "PIN Verification Value to check",           HostParamType.TEXT,   "", true, "1234"),
        )
    ),

    // ── PIN Generation ─────────────────────────────────────────────────────────

    HostCommand(
        code = "DE", responseCode = "DF", name = "Generate IBM PIN Offset",
        category = HostCommandCategory.PIN_GEN,
        description = "Calculates the IBM 3624 PIN offset between a natural PIN and a customer-selected PIN.",
        wireFormatHint = "0000DE [PVK_16H] [LMK_PIN] [MinPIN_2N] [Account_12N] [DecTable_16H] [PINValData_12A]",
        params = listOf(
            HostCommandParam("pvk",       "PVK (16H)",              "PIN Verification Key under LMK 14-15",    HostParamType.HEX,    "", true,  "0123456789ABCDEF"),
            HostCommandParam("lmkPin",    "LMK-Encrypted PIN",      "PIN encrypted under LMK pair 02-03",      HostParamType.TEXT,   "", true,  "041234"),
            HostCommandParam("minPinLen", "Min PIN Length",          "04–12",                                   HostParamType.NUMBER, "04", true, "04"),
            HostCommandParam("account",   "Account Number (12N)",    "12 rightmost digits excl. check digit",   HostParamType.HEX,    "", true,  "123456789012"),
            HostCommandParam("decTable",  "Decimalization Table (16H)","Encrypted decimalization table",        HostParamType.HEX,    "0123456789012345", false, "0123456789012345"),
            HostCommandParam("pinValData","PIN Validation Data (12A)","N = last 5 PAN digits",                  HostParamType.TEXT,   "FFFFFFFFFFFN", false, "FFFFFFFFFFFN"),
        )
    ),

    HostCommand(
        code = "DG", responseCode = "DH", name = "Generate VISA PVV",
        category = HostCommandCategory.PIN_GEN,
        description = "Calculates the VISA PIN Verification Value (PVV) from an LMK-encrypted PIN.",
        wireFormatHint = "0000DG [PVKPair_32H] [LMK_PIN] [Account_12N] [PVKI_1N]",
        params = listOf(
            HostCommandParam("pvkPair", "PVK Pair (32H)",        "Two single-length DES PVKs under LMK 14-15", HostParamType.HEX,   "", true, "0123456789ABCDEFFEDCBA9876543210"),
            HostCommandParam("lmkPin",  "LMK-Encrypted PIN",     "PIN encrypted under LMK pair 02-03",          HostParamType.TEXT,  "", true, "041234"),
            HostCommandParam("account", "Account Number (12N)",   "12 rightmost digits excl. check digit",       HostParamType.HEX,  "", true, "123456789012"),
            HostCommandParam("pvki",    "PVKI (0–6)",             "PIN Verification Key Indicator",              HostParamType.NUMBER,"1",true, "1"),
        )
    ),

    HostCommand(
        code = "EE", responseCode = "EF", name = "Derive PIN (IBM 3624 Offset)",
        category = HostCommandCategory.PIN_GEN,
        description = "Derives the natural PIN from an account number, applies offset, returns LMK-encrypted PIN.",
        wireFormatHint = "0000EE [PVK_16H] [Offset_12H] [MinPIN_2N] [Account_12N] [DecTable_16H] [PINValData_12A]",
        params = listOf(
            HostCommandParam("pvk",       "PVK (16H)",              "PIN Verification Key under LMK 14-15",    HostParamType.HEX,    "", true,  "0123456789ABCDEF"),
            HostCommandParam("offset",    "Offset (12H)",            "IBM 3624 PIN offset (F-padded right)",    HostParamType.HEX,    "FFFFFFFFFFFF", true, "FFFFFFFFFFFF"),
            HostCommandParam("minPinLen", "Min PIN Length",          "04–12",                                   HostParamType.NUMBER, "04", true, "04"),
            HostCommandParam("account",   "Account Number (12N)",    "12 rightmost digits excl. check",         HostParamType.HEX,    "", true,  "123456789012"),
            HostCommandParam("decTable",  "Decimalization Table (16H)","Encrypted decimalization table",        HostParamType.HEX,    "0123456789012345", false, "0123456789012345"),
            HostCommandParam("pinValData","PIN Validation Data (12A)","N = last 5 PAN digits",                  HostParamType.TEXT,   "FFFFFFFFFFFN", false, "FFFFFFFFFFFN"),
        )
    ),

    // ── Key Management ────────────────────────────────────────────────────────

    HostCommand(
        code = "A0", responseCode = "A1", name = "Generate Key",
        category = HostCommandCategory.KEY_MGMT,
        description = "Generate or derive a key. Mode 0/1: random key. Mode A/B: derive IKEY from DUKPT BDK + KSN.",
        wireFormatHint = "0000A0 [Mode_1H] [KeyType_3H] [Scheme_1A] [DeriveKeyMode_1] [DukptKeyType_1] [BDK] [KSN] ; [ZmkFlag_1N] [ZMK] [ExportScheme_1A]",
        params = listOf(
            HostCommandParam("mode",     "Mode",         "0 = Generate under LMK. 1 = Generate + export under ZMK. A = Derive IKEY from BDK+KSN (LMK only). B = Derive IKEY + export under ZMK.",
                HostParamType.DROPDOWN,"B", true, "", listOf(
                HostParamOption("0 – Generate Under LMK", "0"),
                HostParamOption("1 – Generate under LMK and ZMK",     "1"),
                HostParamOption("A – Derive IKEY (DUKPT, LMK only)",     "A"),
                HostParamOption("B – Derive IKEY + export under ZMK",     "B"),
            )),
            HostCommandParam("keyType",  "Key Type",     "Output key type. FFF = KeyBlock (LMK pair from KeyBlock attributes). 302 = IKEY. Determines which LMK pair encrypts the output.",
                HostParamType.DROPDOWN,"FFF", true, "", KEY_TYPE_OPTIONS),
            HostCommandParam("scheme",   "Key Scheme (LMK)",   "Encryption scheme for output key under LMK. S = KeyBlock (for FFF), U = double-length, T = triple-length.",
                HostParamType.DROPDOWN,"S", true, "", KEY_SCHEME_OPTIONS),
            HostCommandParam("deriveKeyMode", "Derive Key Mode", "0 = DUKPT: derive IKEY/IPEK from BDK + KSN using ANSI X9.24. Only for Mode A/B.",
                HostParamType.DROPDOWN,"0", false, "", listOf(
                HostParamOption("0 – DUKPT", "0"),
                HostParamOption("1 – ZKA", "1"),
            )),
            HostCommandParam("dukptMasterKeyType", "DUKPT Master Key Type", "BDK type under LMK 28-29. Each type uses a different LMK variant. 1=BDK-1, 2=BDK-2, 3=BDK-3, 4=BDK-4 (AES).",
                HostParamType.DROPDOWN,"1", false, "", listOf(
                HostParamOption("1 – BDK-1", "1"), HostParamOption("2 – BDK-2", "2"),
                HostParamOption("3 – BDK-3", "3"), HostParamOption("4 – BDK-4 (AES)", "4"),
            )),
            HostCommandParam("bdk",      "BDK (under LMK)",    "DUKPT Base Derivation Key encrypted under LMK pair 28-29. S-block or prefix+hex. Used with KSN to derive IKEY via ANSI X9.24.",
                HostParamType.TEXT, "", false, "S10096B0EN00E0003..."),
            HostCommandParam("ksn",      "KSN",                "Key Serial Number. 15H for 3DES BDK (KSI+DID, F-padded left, last hex even). 16H for AES BDK-4.",
                HostParamType.TEXT, "FFFF9876543210E", false, "FFFF9876543210E"),
            HostCommandParam("a0ZmkDelim", "; Delimiter (ZMK section)", "Include ';' delimiter and ZMK/TMK export fields. Required for Mode 1/B.",
                HostParamType.DELIMITER, "Y", false, "", delimiterChar = ";"),
            HostCommandParam("zmkFlag",  "ZMK/TMK Flag",       "0 = ZMK, 1 = TMK. For Mode 1/B: the wrapping key type for the second output.",
                HostParamType.DROPDOWN,"0", false, "", listOf(
                HostParamOption("0 – ZMK", "0"),
                HostParamOption("1 – TMK", "1"),
            ), delimiterGroup = "a0ZmkDelim"),
            HostCommandParam("zmk",      "ZMK/TMK (under LMK)","ZMK/TMK encrypted under LMK pair 00-01. The derived IKEY will be re-encrypted under this key. For S scheme, used directly as KBPK.",
                HostParamType.TEXT, "", false, "S10096K0TN00E0003...", delimiterGroup = "a0ZmkDelim"),
            HostCommandParam("exportScheme","Key Scheme (ZMK/TMK)","Output format for key under ZMK: S = KeyBlock (ZMK as KBPK), U = single, T = double, X = triple.",
                HostParamType.DROPDOWN,"S", false, "", KEY_SCHEME_OPTIONS, delimiterGroup = "a0ZmkDelim"),
            HostCommandParam("a0LmkDelim", "% Delimiter (LMK Identifier)", "Include '%' delimiter and LMK Identifier field.",
                HostParamType.DELIMITER, "", false, "", delimiterChar = "%"),
            HostCommandParam("lmkId",    "LMK Identifier",     "2-digit LMK Identifier (00-99). Selects which LMK set to use.",
                HostParamType.TEXT, "00", false, "00", delimiterGroup = "a0LmkDelim"),
        )
    ),

    HostCommand(
        code = "A6", responseCode = "A7", name = "Import Key (ZMK → LMK)",
        category = HostCommandCategory.KEY_MGMT,
        description = "Imports a key that arrived encrypted under a ZMK and re-encrypts it under the LMK.",
        wireFormatHint = "0000A6 [KeyType_3H] [ZMKScheme_1A] [ZMK_32H] [LMKScheme_1A] [Key_32H]",
        params = listOf(
            HostCommandParam("keyType",   "Key Type",            "3H key type code",                      HostParamType.DROPDOWN,"001", true, "", KEY_TYPE_OPTIONS),
            HostCommandParam("zmkScheme", "ZMK Scheme",          "Scheme of the ZMK",                     HostParamType.DROPDOWN,"U",   true, "", KEY_SCHEME_OPTIONS),
            HostCommandParam("zmk",       "ZMK (under LMK)",     "Zone Master Key — prefix + hex",        HostParamType.TEXT,    "", true, "U...ZMK..."),
            HostCommandParam("lmkScheme", "Output LMK Scheme",   "How to encrypt result under LMK",       HostParamType.DROPDOWN,"U",   true, "", KEY_SCHEME_OPTIONS),
            HostCommandParam("keyToImport","Key to Import",      "Key encrypted under ZMK — prefix + hex",HostParamType.TEXT,    "", true, "U...KEY..."),
        )
    ),

    HostCommand(
        code = "A8", responseCode = "A9", name = "Export Key (LMK → ZMK)",
        category = HostCommandCategory.KEY_MGMT,
        description = "Exports a key encrypted under LMK, re-encrypts it under a ZMK/TMK for transport. Use FFF for KeyBlock keys.",
        wireFormatHint = "0000A8 [KeyType_3H] ;[ZMK/TMK Flag_1N] [ZMK] [Key] [ExportScheme_1A] %[LMK_ID_2N]",
        params = listOf(
            HostCommandParam("keyType",     "Key Type",           "Key type code. Use FFF for KeyBlock keys — LMK pair is derived from S-block key usage header. Other codes (001=ZPK, 002=TPK, etc.) use fixed LMK pairs.",
                HostParamType.DROPDOWN,"FFF",true, "", KEY_TYPE_OPTIONS),
            HostCommandParam("a8SemiDelim", "; Delimiter (ZMK/TMK section)", "Include ';' delimiter and ZMK/TMK export fields.",
                HostParamType.DELIMITER, "Y", false, "", delimiterChar = ";"),
            HostCommandParam("zmk",         "ZMK/TMK (under LMK)",    "Zone/Terminal Master Key encrypted under LMK. For KeyBlock (S-block) keys, the key usage in the header determines the LMK pair. This key will be used as KBPK when export scheme is S.",
                HostParamType.TEXT,    "", true, "S10096K0TN00E0003...", delimiterGroup = "a8SemiDelim"),
            HostCommandParam("keyToExport", "Key to Export (under LMK)",      "The key to export, encrypted under LMK. For FFF key type, this should be an S-block. The clear key inside will be re-encrypted under the ZMK/TMK using the export scheme.",
                HostParamType.TEXT,    "", true, "S10096P0TB00E0003...", delimiterGroup = "a8SemiDelim"),
            HostCommandParam("exportScheme","Export Scheme (output format)",       "Format of the output key under ZMK/TMK: S = KeyBlock (ZMK used as KBPK), U = single-length variant, T = double-length variant, X = triple-length variant.",
                HostParamType.DROPDOWN,"S",true, "", KEY_SCHEME_OPTIONS, delimiterGroup = "a8SemiDelim"),
            HostCommandParam("a8LmkDelim", "% Delimiter (LMK Identifier)", "Include '%' delimiter and LMK Identifier field.",
                HostParamType.DELIMITER, "", false, "", delimiterChar = "%"),
            HostCommandParam("lmkId",    "LMK Identifier",     "2-digit LMK Identifier (00-99). Selects which LMK set to use.",
                HostParamType.TEXT, "00", false, "00", delimiterGroup = "a8LmkDelim"),
        )
    ),

    HostCommand(
        code = "BU", responseCode = "BV", name = "Generate Key Check Value",
        category = HostCommandCategory.KEY_MGMT,
        description = "Generates the 3-byte Key Check Value (KCV) for any LMK-encrypted key.",
        wireFormatHint = "0000BU [KeyType_3H] [KeyScheme_1A] [Key_32H]",
        params = listOf(
            HostCommandParam("keyType", "Key Type",        "3H key type code",              HostParamType.DROPDOWN,"001",true, "", KEY_TYPE_OPTIONS),
            HostCommandParam("scheme",  "Key Scheme",      "Scheme of the key",             HostParamType.DROPDOWN,"U",  true, "", KEY_SCHEME_OPTIONS),
            HostCommandParam("key",     "Key (under LMK)", "Key to check — prefix + hex",   HostParamType.TEXT,    "", true, "U...KEY..."),
        )
    ),

    // ── Data Encryption ───────────────────────────────────────────────────────

    HostCommand(
        code = "M0", responseCode = "M1", name = "Encrypt Data Block",
        category = HostCommandCategory.ENCRYPTION,
        description = "Encrypts a data block using a ZEK/DEK or BDK (DUKPT) key (ECB or CBC). For BDK types (009/609/809/909), KSN Descriptor and KSN are required.",
        wireFormatHint = "0000M0 [Mode_2N] [InFmt_1N] [OutFmt_1N] [KeyType_3H] [Key_1A+32H] [KSNDesc_3H?] [KSN_20H?] [IV_16H?] [MsgLen_4H] [Data_nH]",
        params = listOf(
            HostCommandParam("mode",    "Cipher Mode",     "",              HostParamType.DROPDOWN,"00",true, "", CIPHER_MODE_OPTIONS),
            HostCommandParam("inFmt",   "Input Format",    "0=Binary 1=Hex",HostParamType.DROPDOWN,"1", true, "", listOf(
                HostParamOption("0 – Binary", "0"), HostParamOption("1 – Hex-encoded", "1"),
            )),
            HostCommandParam("outFmt",  "Output Format",   "0=Binary 1=Hex",HostParamType.DROPDOWN,"1", true, "", listOf(
                HostParamOption("0 – Binary", "0"), HostParamOption("1 – Hex-encoded", "1"),
            )),
            HostCommandParam("keyType", "Key Type",        "ZEK / DEK / BDK",HostParamType.DROPDOWN,"00A",true, "", KEY_TYPE_OPTIONS),
            HostCommandParam("key",     "Encryption Key",  "prefix + hex",  HostParamType.TEXT,    "", true, "U...ZEK..."),
            HostCommandParam("ksnDesc", "KSN Descriptor (3H)", "BDK only: BDK_ID len, Sub-Key len, Device_ID len", HostParamType.HEX, "609", false, "609"),
            HostCommandParam("ksn",     "KSN (20H)",       "BDK only: Key Serial Number",  HostParamType.HEX, "", false, "FFFF9876543210E00001"),
            HostCommandParam("iv",      "IV (16H)",        "CBC mode only", HostParamType.HEX,     "0000000000000000", false, "0000000000000000"),
            HostCommandParam("data",    "Plaintext Data (hex)","Data to encrypt",HostParamType.TEXT,"", true, "48656C6C6F"),
        )
    ),

    HostCommand(
        code = "M2", responseCode = "M3", name = "Decrypt Data Block",
        category = HostCommandCategory.ENCRYPTION,
        description = "Decrypts a data block using a ZEK/DEK or BDK (DUKPT) key. For BDK types (009/609/809/909), KSN Descriptor and KSN are required.",
        wireFormatHint = "0000M2 [Mode_2N] [InFmt_1N] [OutFmt_1N] [KeyType_3H] [Key_1A+32H] [KSNDesc_3H?] [KSN_20H?] [IV_16H?] [MsgLen_4H] [Data_nH]",
        params = listOf(
            HostCommandParam("mode",    "Cipher Mode",     "",              HostParamType.DROPDOWN,"00",true, "", CIPHER_MODE_OPTIONS),
            HostCommandParam("inFmt",   "Input Format",    "0=Binary 1=Hex",HostParamType.DROPDOWN,"1", true, "", listOf(
                HostParamOption("0 – Binary", "0"), HostParamOption("1 – Hex-encoded", "1"),
            )),
            HostCommandParam("outFmt",  "Output Format",   "0=Binary 1=Hex",HostParamType.DROPDOWN,"1", true, "", listOf(
                HostParamOption("0 – Binary", "0"), HostParamOption("1 – Hex-encoded", "1"),
            )),
            HostCommandParam("keyType", "Key Type",        "ZEK / DEK / BDK",HostParamType.DROPDOWN,"00A",true,"", KEY_TYPE_OPTIONS),
            HostCommandParam("key",     "Decryption Key",  "prefix + hex",  HostParamType.TEXT,    "", true, "U...ZEK..."),
            HostCommandParam("ksnDesc", "KSN Descriptor (3H)", "BDK only: BDK_ID len, Sub-Key len, Device_ID len", HostParamType.HEX, "609", false, "609"),
            HostCommandParam("ksn",     "KSN (20H)",       "BDK only: Key Serial Number",  HostParamType.HEX, "", false, "FFFF9876543210E00001"),
            HostCommandParam("iv",      "IV (16H)",        "CBC mode only", HostParamType.HEX,     "0000000000000000", false, "0000000000000000"),
            HostCommandParam("data",    "Ciphertext (hex)","Data to decrypt",HostParamType.TEXT,    "", true, ""),
        )
    ),

    HostCommand(
        code = "M4", responseCode = "M5", name = "Translate Data Block",
        category = HostCommandCategory.ENCRYPTION,
        description = "Re-encrypts data from one key (ZEK/BDK) to another — e.g., DUKPT → ZEK.",
        wireFormatHint = "0000M4 [SrcMode_2N] [DstMode_2N] [InFmt_1N] [OutFmt_1N] [SrcKeyType_3H] [SrcKey_1A+32H] [DstKeyType_3H] [DstKey_1A+32H] [MsgLen_4H] [Data]",
        params = listOf(
            HostCommandParam("srcMode",  "Source Cipher Mode",    "", HostParamType.DROPDOWN,"00",true, "", CIPHER_MODE_OPTIONS),
            HostCommandParam("dstMode",  "Dest Cipher Mode",      "", HostParamType.DROPDOWN,"00",true, "", CIPHER_MODE_OPTIONS),
            HostCommandParam("inFmt",    "Input Format",     "0=Bin 1=Hex",HostParamType.DROPDOWN,"1", true, "", listOf(
                HostParamOption("0 – Binary", "0"), HostParamOption("1 – Hex-encoded", "1"),
            )),
            HostCommandParam("outFmt",   "Output Format",    "0=Bin 1=Hex",HostParamType.DROPDOWN,"1", true, "", listOf(
                HostParamOption("0 – Binary", "0"), HostParamOption("1 – Hex-encoded", "1"),
            )),
            HostCommandParam("srcKeyType","Source Key Type", "",            HostParamType.DROPDOWN,"009",true,"", KEY_TYPE_OPTIONS),
            HostCommandParam("srcKey",    "Source Key",      "prefix + hex",HostParamType.TEXT,    "", true, "U...BDK..."),
            HostCommandParam("dstKeyType","Dest Key Type",   "",            HostParamType.DROPDOWN,"00A",true,"", KEY_TYPE_OPTIONS),
            HostCommandParam("dstKey",    "Dest Key",        "prefix + hex",HostParamType.TEXT,    "", true, "U...ZEK..."),
            HostCommandParam("data",      "Source Ciphertext (hex)","",     HostParamType.TEXT,    "", true, ""),
        )
    ),

    // ── MAC Operations ─────────────────────────────────────────────────────────

    HostCommand(
        code = "M6", responseCode = "M7", name = "Generate MAC",
        category = HostCommandCategory.MAC_OPS,
        description = "Calculates a Message Authentication Code over the supplied data using a ZAK or TAK.",
        wireFormatHint = "0000M6 [Mode_1N] [InFmt_1N] [MACSize_1N] [Algo_1N] [Padding_1N] [KeyType_3H] [Key_1A+32H] [MsgLen_4H] [Data]",
        params = listOf(
            HostCommandParam("mode",    "Mode",        "0=single 1=first 2=middle 3=last", HostParamType.DROPDOWN,"0",true, "", listOf(
                HostParamOption("0 – Single block",  "0"), HostParamOption("1 – First block",  "1"),
                HostParamOption("2 – Middle block",  "2"), HostParamOption("3 – Last block",   "3"),
            )),
            HostCommandParam("inFmt",   "Input Format","0=Binary 1=Hex",                  HostParamType.DROPDOWN,"1",true, "", listOf(
                HostParamOption("0 – Binary","0"), HostParamOption("1 – Hex-encoded","1"),
            )),
            HostCommandParam("macSize", "MAC Size",    "0=8 hex digits 1=4 hex digits",   HostParamType.DROPDOWN,"0",true, "", listOf(
                HostParamOption("0 – 8 hex digits (4 bytes)","0"), HostParamOption("1 – 4 hex digits (2 bytes)","1"),
            )),
            HostCommandParam("algo",    "MAC Algorithm","",                                HostParamType.DROPDOWN,"1",true, "", MAC_ALGO_OPTIONS),
            HostCommandParam("padding", "Padding Method","",                               HostParamType.DROPDOWN,"1",true, "", MAC_PADDING_OPTIONS),
            HostCommandParam("keyType", "Key Type",     "ZAK=008 TAK=003",                HostParamType.DROPDOWN,"008",true,"", KEY_TYPE_OPTIONS),
            HostCommandParam("macKey",  "MAC Key",      "Key under LMK — prefix + hex",   HostParamType.TEXT,    "", true, "U...ZAK..."),
            HostCommandParam("data",    "Message Data (hex)","Data to MAC",               HostParamType.TEXT,    "", true, "0102030405060708"),
        )
    ),

    HostCommand(
        code = "M8", responseCode = "M9", name = "Verify MAC",
        category = HostCommandCategory.MAC_OPS,
        description = "Verifies a MAC against the data. Returns error code 00 on success.",
        wireFormatHint = "0000M8 [Mode_1N] [InFmt_1N] [MACSize_1N] [Algo_1N] [Padding_1N] [KeyType_3H] [Key_1A+32H] [MsgLen_4H] [Data] [MAC_8H]",
        params = listOf(
            HostCommandParam("mode",    "Mode",        "0=single 1=first 2=middle 3=last", HostParamType.DROPDOWN,"0",true, "", listOf(
                HostParamOption("0 – Single block","0"), HostParamOption("1 – First block","1"),
                HostParamOption("2 – Middle block","2"), HostParamOption("3 – Last block","3"),
            )),
            HostCommandParam("inFmt",   "Input Format","0=Binary 1=Hex",                  HostParamType.DROPDOWN,"1",true, "", listOf(
                HostParamOption("0 – Binary","0"), HostParamOption("1 – Hex-encoded","1"),
            )),
            HostCommandParam("macSize", "MAC Size",    "0=8 hex digits",                  HostParamType.DROPDOWN,"0",true, "", listOf(
                HostParamOption("0 – 8 hex digits (4 bytes)","0"),
            )),
            HostCommandParam("algo",    "MAC Algorithm","",                                HostParamType.DROPDOWN,"1",true, "", MAC_ALGO_OPTIONS),
            HostCommandParam("padding", "Padding Method","",                               HostParamType.DROPDOWN,"1",true, "", MAC_PADDING_OPTIONS),
            HostCommandParam("keyType", "Key Type",     "ZAK=008 TAK=003",                HostParamType.DROPDOWN,"008",true,"", KEY_TYPE_OPTIONS),
            HostCommandParam("macKey",  "MAC Key",      "Key under LMK — prefix + hex",   HostParamType.TEXT,    "", true, "U...ZAK..."),
            HostCommandParam("data",    "Message Data (hex)","Data that was MAC'd",        HostParamType.TEXT,    "", true, "0102030405060708"),
            HostCommandParam("mac",     "MAC to Verify (8H)","Expected MAC value",         HostParamType.HEX,    "", true, "AABBCCDD"),
        )
    ),

    // ── Hash / Digest ─────────────────────────────────────────────────────────

    HostCommand(
        code = "GM", responseCode = "GN", name = "Hash Data",
        category = HostCommandCategory.HASH_OPS,
        description = "Computes a cryptographic hash (SHA-1, SHA-256, MD5, etc.) over the supplied data.",
        wireFormatHint = "0000GM [HashAlgo_2N] [MsgLen_5N] [Data_nB]",
        params = listOf(
            HostCommandParam("algo", "Hash Algorithm", "",                           HostParamType.DROPDOWN,"06", true, "", HASH_ALGO_OPTIONS),
            HostCommandParam("data", "Data to Hash (hex)", "Up to 32 000 bytes",    HostParamType.TEXT,    "", true, "48656C6C6F20576F726C64"),
        )
    ),

    // ── RSA / Asymmetric ──────────────────────────────────────────────────────

    HostCommand(
        code = "EI", responseCode = "EJ", name = "Generate RSA Key Pair",
        category = HostCommandCategory.RSA_CRYPTO,
        description = "Generates an RSA key pair. Returns the public key and the private key encrypted under LMK.",
        wireFormatHint = "0000EI [KeyType_1N] [ModulusLen_4N] [Encoding_2N]",
        params = listOf(
            HostCommandParam("keyType",    "Key Type",        "0=mgmt 1=mgmt+sign 2=sign only",  HostParamType.DROPDOWN,"1",true,"", listOf(
                HostParamOption("0 – Key management only",          "0"),
                HostParamOption("1 – Key management + signing",     "1"),
                HostParamOption("2 – Signing only",                 "2"),
            )),
            HostCommandParam("modulusLen", "Modulus Length",  "Bits: 1024/2048/4096",            HostParamType.DROPDOWN,"2048",true,"", listOf(
                HostParamOption("1024 bits", "1024"), HostParamOption("2048 bits", "2048"), HostParamOption("4096 bits", "4096"),
            )),
            HostCommandParam("encoding",   "Encoding Rules",  "01=DER unsigned 02=DER 2's comp", HostParamType.DROPDOWN,"01",true,"", listOf(
                HostParamOption("01 – DER unsigned",         "01"),
                HostParamOption("02 – DER 2's complement",   "02"),
            )),
        )
    ),

    HostCommand(
        code = "EO", responseCode = "EP", name = "Import RSA Public Key",
        category = HostCommandCategory.RSA_CRYPTO,
        description = "Imports a DER-encoded RSA public key and generates a MAC to protect it (used for CA keys).",
        wireFormatHint = "0000EO [Encoding_2N] [PublicKey_nB] [AuthData_nB]",
        params = listOf(
            HostCommandParam("encoding", "Encoding Rules", "01=DER unsigned 02=DER 2's comp",  HostParamType.DROPDOWN,"01",true,"", listOf(
                HostParamOption("01 – DER unsigned",       "01"),
                HostParamOption("02 – DER 2's complement", "02"),
            )),
            HostCommandParam("pubKey",   "Public Key (DER hex)", "Hex-encoded DER public key", HostParamType.TEXT,    "", true, ""),
            HostCommandParam("authData", "Auth Data (hex)",       "Additional auth data for MAC",HostParamType.TEXT,    "", false, ""),
        )
    ),

    HostCommand(
        code = "EW", responseCode = "EX", name = "Generate RSA/ECDSA Signature",
        category = HostCommandCategory.RSA_CRYPTO,
        description = "Signs data using an RSA or ECC private key encrypted under the LMK.",
        wireFormatHint = "0000EW [Hash_2N] [SigAlgo_2N] [PadMode_2N] [DataLen_4N] [Data] ; [PrivKeyFlag_2N] [PrivKeyLen_4N] [PrivKey]",
        params = listOf(
            HostCommandParam("hash",        "Hash Algorithm",   "",                                        HostParamType.DROPDOWN,"06",true,"", RSA_HASH_OPTIONS),
            HostCommandParam("sigAlgo",     "Signature Algo",   "01=RSA 02=ECDSA",                        HostParamType.DROPDOWN,"01",true,"", listOf(
                HostParamOption("01 – RSA",   "01"), HostParamOption("02 – ECDSA", "02"),
            )),
            HostCommandParam("padMode",     "Padding Mode",     "",                                        HostParamType.DROPDOWN,"01",true,"", RSA_PAD_OPTIONS),
            HostCommandParam("data",        "Data to Sign (hex)","Raw data (or pre-computed hash if algo=04)",HostParamType.TEXT,"",true,"48656C6C6F"),
            HostCommandParam("privKeyFlag", "Private Key Flag", "99=inline key; 00-20=stored at index N", HostParamType.TEXT,    "99",true,"99"),
            HostCommandParam("privKey",     "Private Key (hex)","LMK-encrypted RSA private key (binary hex)",HostParamType.TEXT, "",true,""),
        )
    ),

    HostCommand(
        code = "EY", responseCode = "EZ", name = "Validate RSA/ECDSA Signature",
        category = HostCommandCategory.RSA_CRYPTO,
        description = "Validates a digital signature using an RSA or ECC public key protected by MAC.",
        wireFormatHint = "0000EY [Hash_2N] [SigAlgo_2N] [PadMode_2N] [SigLen_4N] [Signature] ; [DataLen_4N] [Data] ; [MAC_4B] [PublicKey]",
        params = listOf(
            HostCommandParam("hash",      "Hash Algorithm",    "",                HostParamType.DROPDOWN,"06",true,"", RSA_HASH_OPTIONS),
            HostCommandParam("sigAlgo",   "Signature Algo",    "01=RSA 02=ECDSA", HostParamType.DROPDOWN,"01",true,"", listOf(
                HostParamOption("01 – RSA","01"), HostParamOption("02 – ECDSA","02"),
            )),
            HostCommandParam("padMode",   "Padding Mode",      "",                HostParamType.DROPDOWN,"01",true,"", RSA_PAD_OPTIONS),
            HostCommandParam("signature", "Signature (hex)",   "Signature to verify",HostParamType.TEXT, "",true,""),
            HostCommandParam("data",      "Signed Data (hex)", "Original message", HostParamType.TEXT,  "",true,"48656C6C6F"),
            HostCommandParam("mac",       "Public Key MAC (4B hex)","MAC from EO command",HostParamType.HEX,"",true,""),
            HostCommandParam("pubKey",    "Public Key (DER hex)","DER-encoded public key",HostParamType.TEXT,"",true,""),
        )
    ),

    // ── CVV Verification ──────────────────────────────────────────────────────

    HostCommand(
        code = "PM", responseCode = "PN", name = "Verify Dynamic CVV/CVC",
        category = HostCommandCategory.CVV_VERIFY,
        description = "Verifies a dCVV (Visa) or CVC3 (MasterCard PayPass) for contactless transactions.",
        wireFormatHint = "0000PM [SchemeID_1N] [Version_1N] [MK-DCVV_1A+32H] [DerivMethod_1A] [PAN_19N] ; [ExpDate/PSN] [TrackDataLen_3N] [StaticTrack_nB] [UnpredNum_10N] [ATC_5N] [CVC3_5A]",
        params = listOf(
            HostCommandParam("scheme",    "Scheme",         "0=Visa dCVV 1=MC CVC3",         HostParamType.DROPDOWN,"0",true,"", listOf(
                HostParamOption("0 – Visa dCVV",            "0"),
                HostParamOption("1 – MasterCard PayPass",   "1"),
            )),
            HostCommandParam("version",   "Version",        "0 for Visa; 2 for MC PayPass",  HostParamType.TEXT,    "0",true,"0"),
            HostCommandParam("mkDcvv",    "MK-DCVV (1A+32H)","Master key for dCVV under LMK",HostParamType.TEXT,    "",true,"U...MK..."),
            HostCommandParam("pan",       "PAN",            "Full PAN (up to 19 digits)",    HostParamType.TEXT,    "",true,"4111111111111111"),
            HostCommandParam("expDate",   "Exp Date (4N) or PSN (2N)","YYMM for Visa; PSN for MC",HostParamType.TEXT,"2512",true,"2512"),
            HostCommandParam("atc",       "ATC (5N)",       "Application Transaction Counter",HostParamType.NUMBER, "00001",true,"00001"),
            HostCommandParam("cvcToVerify","CVC3 / dCVV to verify","X = wildcard digit",      HostParamType.TEXT,   "",true,"123"),
        )
    ),

    // ── User Storage ──────────────────────────────────────────────────────────

    HostCommand(
        code = "LA", responseCode = "LB", name = "Load Data to User Storage",
        category = HostCommandCategory.USER_STORE,
        description = "Stores a key or data block in the HSM's user storage at a given index (000–FFF).",
        wireFormatHint = "0000LA [IndexFlag_1A] [Index_3H] [NumBlocks_2H] [Data_32H×N]",
        params = listOf(
            HostCommandParam("indexFlag","Index Flag",     "K = standard index",           HostParamType.TEXT,   "K",   true, "K"),
            HostCommandParam("index",    "Index (3H)",     "Storage slot 000–FFF",         HostParamType.HEX,    "000", true, "000"),
            HostCommandParam("numBlocks","Num Blocks (2H)","Number of data blocks (01–FF)",HostParamType.TEXT,   "01",  true, "01"),
            HostCommandParam("data",     "Data (32H/block)","Hex data blocks to store",    HostParamType.TEXT,   "", true, "0123456789ABCDEF0123456789ABCDEF"),
        )
    ),

    HostCommand(
        code = "LE", responseCode = "LF", name = "Read from User Storage",
        category = HostCommandCategory.USER_STORE,
        description = "Retrieves data previously stored in the HSM's user storage.",
        wireFormatHint = "0000LE [IndexFlag_1A] [Index_3H] [NumBlocks_2H]",
        params = listOf(
            HostCommandParam("indexFlag","Index Flag",     "K = standard index",           HostParamType.TEXT,  "K",  true, "K"),
            HostCommandParam("index",    "Index (3H)",     "Storage slot to read",         HostParamType.HEX,   "000",true, "000"),
            HostCommandParam("numBlocks","Num Blocks (2H)","Number of blocks to retrieve", HostParamType.TEXT,  "01", true, "01"),
        )
    ),
)

// ─────────────────────────────────────────────────────────────────────────────
// MAIN TAB COMPOSABLE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HsmHostCommandsTab(hsm: HsmServiceImpl) {
    val scope = rememberCoroutineScope()

    var selectedCommand  by remember { mutableStateOf<HostCommand?>(null) }
    var resultText       by remember { mutableStateOf("") }
    var isExecuting      by remember { mutableStateOf(false) }
    var searchQuery      by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<HostCommandCategory?>(null) }

    val filteredCommands = remember(searchQuery, selectedCategory) {
        HOST_COMMANDS.filter { cmd ->
            val matchesSearch = searchQuery.isBlank() ||
                cmd.code.contains(searchQuery, true) ||
                cmd.name.contains(searchQuery, true) ||
                cmd.description.contains(searchQuery, true)
            val matchesCat = selectedCategory == null || cmd.category == selectedCategory
            matchesSearch && matchesCat
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {

        // ── Left panel: categorised command list ──────────────────────────────
        Card(modifier = Modifier.width(280.dp).fillMaxHeight(), elevation = 2.dp) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Header
                Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colors.primary).padding(12.dp)) {
                    Column {
                        Text("Host Commands", style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colors.onPrimary)
                        Text("No smart-card authorization required",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onPrimary.copy(alpha = 0.8f))
                    }
                }

                // Search bar
                Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    FixedOutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search commands…", style = MaterialTheme.typography.caption) },
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp))
                                }
                            }
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.caption.copy(fontFamily = FontFamily.Default),
                    )
                }

                // Category filter chips
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CategoryChip(label = "All", color = MaterialTheme.colors.primary,
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null })
                    HostCommandCategory.values().forEach { cat ->
                        CategoryChip(label = cat.label, color = cat.color,
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat })
                    }
                }

                Divider()

                // Command list
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredCommands) { cmd ->
                        HostCommandListItem(
                            command = cmd,
                            isSelected = selectedCommand == cmd,
                            onClick = { selectedCommand = cmd; resultText = "" }
                        )
                    }
                    if (filteredCommands.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No commands match your search", style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.width(16.dp))

        // ── Right panel: form + result ────────────────────────────────────────
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            if (selectedCommand == null) {
                HostCommandsEmptyState()
            } else {
                val cmd = selectedCommand!!
                HostCommandForm(
                    command = cmd,
                    isExecuting = isExecuting,
                    resultText = resultText,
                    onExecute = { params ->
                        isExecuting = true
                        scope.launch {
                            resultText = try {
                                val raw = buildHostCommand(cmd, params)
                                val response = hsm.executeSecureCommand(raw, source = "HOST-CMD")
                                formatHostResponse(cmd, response)
                            } catch (e: Exception) {
                                "ERROR: ${e.message}"
                            } finally {
                                isExecuting = false
                            }
                        }
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HostCommandsEmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Terminal, null, modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.25f))
            Text("Select a host command from the list",
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
            Text("${HOST_COMMANDS.size} commands available across ${HostCommandCategory.values().size} categories",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.35f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Category filter chip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CategoryChip(label: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) color.copy(alpha = 0.2f) else Color.Transparent, tween(150))
    Surface(
        modifier = Modifier.height(24.dp).clickable(onClick = onClick),
        color = bg,
        border = BorderStroke(1.dp, if (selected) color else color.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)) {
            Text(label, style = MaterialTheme.typography.overline,
                color = if (selected) color else MaterialTheme.colors.onSurface.copy(alpha = 0.55f),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Command list item
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun HostCommandListItem(command: HostCommand, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isSelected) command.category.color.copy(alpha = 0.1f) else Color.Transparent,
        onClick = onClick
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Code badge
            Surface(
                color = command.category.color.copy(alpha = if (isSelected) 0.2f else 0.1f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(command.code, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.caption.copy(fontFamily = FontFamily.Monospace),
                    fontWeight = FontWeight.Bold, color = command.category.color)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(command.name, style = MaterialTheme.typography.caption,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) command.category.color else MaterialTheme.colors.onSurface,
                    maxLines = 1)
                Text(command.category.label, style = MaterialTheme.typography.overline,
                    color = command.category.color.copy(alpha = 0.7f))
            }
            if (isSelected) {
                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(14.dp),
                    tint = command.category.color)
            }
        }
    }
    Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.05f))
}

// ─────────────────────────────────────────────────────────────────────────────
// Command form
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HostCommandForm(
    command: HostCommand,
    isExecuting: Boolean,
    resultText: String,
    onExecute: (Map<String, String>) -> Unit
) {
    val paramValues = remember(command) {
        mutableStateMapOf<String, String>().also { map ->
            command.params.forEach { p -> map[p.id] = p.default }
        }
    }
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Command header card ──
        Card(elevation = 2.dp) {
            Row(modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = command.category.color.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                    Icon(command.category.icon, null,
                        modifier = Modifier.padding(8.dp).size(22.dp), tint = command.category.color)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(command.code, style = MaterialTheme.typography.h6,
                            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text("→", style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                        Text(command.responseCode, style = MaterialTheme.typography.subtitle2,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                        Text(command.name, style = MaterialTheme.typography.h6)
                    }
                    Text(command.description, style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // ── Parameters panel ──
            Card(modifier = Modifier.weight(1f).fillMaxHeight(), elevation = 1.dp) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp)) {
                    Text("Parameters", style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colors.primary)
                    Spacer(Modifier.height(12.dp))

                    if (command.params.isEmpty()) {
                        Text("No parameters required for this command.",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f))
                    } else {
                        command.params.forEach { param ->
                            val shouldShow = if (param.delimiterGroup.isNotEmpty()) {
                                val delimVal = paramValues[param.delimiterGroup]
                                    ?: command.params.find { it.id == param.delimiterGroup }?.default
                                    ?: ""
                                delimVal == "Y"
                            } else true

                            if (shouldShow) {
                                HostParamField(
                                    param = param,
                                    value = paramValues[param.id] ?: param.default,
                                    onValueChange = { paramValues[param.id] = it }
                                )
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    }

                    // Wire format hint
                    if (command.wireFormatHint.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Divider()
                        Spacer(Modifier.height(8.dp))
                        Text("Wire Format:", style = MaterialTheme.typography.caption,
                            fontWeight = FontWeight.Bold)
                        Text(command.wireFormatHint,
                            style = MaterialTheme.typography.caption.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f))
                        Spacer(Modifier.height(12.dp))
                    }

                    Button(
                        onClick = { onExecute(paramValues.toMap()) },
                        enabled = !isExecuting,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = command.category.color)
                    ) {
                        if (isExecuting) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp,
                                color = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Executing…", color = Color.White)
                        } else {
                            Icon(Icons.Default.PlayArrow, null,
                                modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Execute  ${command.code}", color = Color.White,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            // ── Response panel ──
            Card(modifier = Modifier.weight(1f).fillMaxHeight(), elevation = 1.dp) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Output, null, modifier = Modifier.size(18.dp))
                        Text("Response", style = MaterialTheme.typography.subtitle2,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text("${command.responseCode} response", style = MaterialTheme.typography.overline,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                    }
                    Spacer(Modifier.height(8.dp))

                    if (resultText.isBlank()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Terminal, null, modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.2f))
                                Text("Response will appear here",
                                    style = MaterialTheme.typography.body2,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f))
                            }
                        }
                    } else {
                        val scrollR = rememberScrollState()
                        Text(resultText,
                            modifier = Modifier.fillMaxSize().verticalScroll(scrollR),
                            style = MaterialTheme.typography.caption.copy(
                                fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                            color = if (resultText.startsWith("✗") || resultText.contains("ERROR"))
                                MaterialTheme.colors.error else MaterialTheme.colors.onSurface)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Parameter field rendering
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HostParamField(
    param: HostCommandParam,
    value: String,
    onValueChange: (String) -> Unit
) {
    // DELIMITER type: render as a checkbox with divider
    if (param.type == HostParamType.DELIMITER) {
        val isChecked = value == "Y"
        val toggle = { onValueChange(if (isChecked) "" else "Y") }
        val content: @Composable () -> Unit = {
            Column {
                Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { toggle() }
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { onValueChange(if (it) "Y" else "") },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.primary)
                    )
                    Text(
                        param.label,
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isChecked) MaterialTheme.colors.onSurface
                            else MaterialTheme.colors.onSurface.copy(alpha = 0.45f)
                    )
                    if (param.description.isNotBlank()) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Info, contentDescription = "Info",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                        )
                    }
                }
            }
        }
        if (param.description.isNotBlank()) {
            `in`.aicortex.iso8583studio.ui.screens.hsmCommand.AutoHideTooltip(text = param.description) { content() }
        } else {
            content()
        }
        return
    }

    val content: @Composable () -> Unit = {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(param.label, style = MaterialTheme.typography.caption, fontWeight = FontWeight.SemiBold)
                if (param.required) Text(" *", color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption)
                if (param.description.isNotBlank()) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Info",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                    )
                }
                Spacer(Modifier.weight(1f))
                if (param.type == HostParamType.HEX || param.type == HostParamType.TEXT) {
                    Text("${value.length} chars", style = MaterialTheme.typography.overline,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.35f))
                }
            }

            when (param.type) {
            HostParamType.DROPDOWN -> HostDropdownField(param, value, onValueChange)

            HostParamType.NUMBER -> FixedOutlinedTextField(
                value = value,
                onValueChange = { if (it.all { c -> c.isDigit() }) onValueChange(it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(param.placeholder.ifEmpty { param.label }, style = MaterialTheme.typography.caption) },
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            )

            else -> FixedOutlinedTextField(
                value = value,
                onValueChange = { v ->
                    val filtered = if (param.type == HostParamType.HEX)
                        v.filter { it.isDigit() || it in 'A'..'F' || it in 'a'..'f' }.uppercase()
                    else v
                    onValueChange(filtered)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(param.placeholder.ifEmpty { param.label }, style = MaterialTheme.typography.caption) },
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            )
        }
        }
    }

    if (param.description.isNotBlank()) {
        `in`.aicortex.iso8583studio.ui.screens.hsmCommand.AutoHideTooltip(text = param.description) { content() }
    } else {
        content()
    }
}

@Composable
private fun HostDropdownField(
    param: HostCommandParam,
    value: String,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var fieldWidthPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val selectedLabel = param.options.find { it.value == value }?.label ?: value
    Box(
        Modifier
            .fillMaxWidth()
            .onGloballyPositioned { fieldWidthPx = it.size.width }
    ) {
        FixedOutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            singleLine = true,
            trailingIcon = {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null,
                    modifier = Modifier.size(20.dp)
                )
            },
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
        )
        Box(
            Modifier
                .matchParentSize()
                .clickable { expanded = !expanded }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(with(density) { fieldWidthPx.toDp() })
                .heightIn(max = 300.dp)
        ) {
            param.options.forEach { opt ->
                DropdownMenuItem(onClick = { onValueChange(opt.value); expanded = false }) {
                    Text(opt.label, style = MaterialTheme.typography.caption)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Command string builders
// ─────────────────────────────────────────────────────────────────────────────

private fun buildHostCommand(cmd: HostCommand, p: Map<String, String>): String {
    val h = "0000"
    fun v(key: String, default: String = "") = p[key]?.trim() ?: default
    return when (cmd.code) {
        "NC" -> "${h}NC"

        "CA" -> buildString {
            append(h); append("CA")
            append(v("tpk")); append(v("zpk"))
            append(v("maxPinLen", "12").padStart(2, '0'))
            append(v("pinBlock")); append(v("srcFmt", "01")); append(v("dstFmt", "01"))
            append(v("account"))
        }

        "G0" -> buildString {
            append(h); append("G0")
            append(v("bdk"))
            val dkt = v("destKeyType", "0")
            append(dkt)
            append(v("destKey"))
            append(v("srcKsnDesc", "906"))
            append(v("srcKsn"))
            if (dkt != "0") {
                append(v("dstKsnDesc", "906"))
                append(v("dstKsn"))
            }
            append(v("pinBlock"))
            append(v("srcFmt", "01")); append(v("dstFmt", "01"))
            append(v("account"))
        }

        "JC" -> "${h}JC${v("tpk")}${v("pinBlock")}${v("srcFmt","01")}${v("account")}"
        "JE" -> "${h}JE${v("zpk")}${v("pinBlock")}${v("srcFmt","01")}${v("account")}"
        "JG" -> "${h}JG${v("zpk")}${v("dstFmt","01")}${v("account")}${v("lmkPin")}"

        "DA" -> buildString {
            append(h); append("DA")
            append(v("tpk")); append(v("pvk"))
            append(v("maxPinLen","12").padStart(2,'0'))
            append(v("pinBlock")); append(v("pinFmt","01"))
            append(v("account"))
            append(v("decTable","0123456789012345"))
            append(v("pinValData","FFFFFFFFFFFN"))
            append(v("offset","FFFFFFFFFFFF"))
        }

        "DC" -> buildString {
            append(h); append("DC")
            append(v("tpk")); append(v("pvkPair"))
            append(v("pinBlock")); append(v("pinFmt","01"))
            append(v("account"))
            append(v("pvki","1")); append(v("pvv"))
        }

        "DE" -> buildString {
            append(h); append("DE")
            append(v("pvk")); append(v("lmkPin"))
            append(v("minPinLen","04").padStart(2,'0'))
            append(v("account"))
            append(v("decTable","0123456789012345"))
            append(v("pinValData","FFFFFFFFFFFN"))
        }

        "DG" -> "${h}DG${v("pvkPair")}${v("lmkPin")}${v("account")}${v("pvki","1")}"

        "EE" -> buildString {
            append(h); append("EE")
            append(v("pvk")); append(v("offset","FFFFFFFFFFFF"))
            append(v("minPinLen","04").padStart(2,'0'))
            append(v("account"))
            append(v("decTable","0123456789012345"))
            append(v("pinValData","FFFFFFFFFFFN"))
        }

        "A0" -> buildString {
            append(h); append("A0")
            val mode = v("mode","B")
            append(mode); append(v("keyType","FFF")); append(v("scheme","S"))
            if (mode == "A" || mode == "B") {
                // Derive key mode + DUKPT fields
                append(v("deriveKeyMode","0"))
                append(v("dukptMasterKeyType","1"))
                append(v("bdk",""))
                append(v("ksn",""))
            }
            if (v("a0ZmkDelim","Y") == "Y" && (mode == "1" || mode == "B")) {
                append(";"); append(v("zmkFlag","0"))
                append(v("zmk",""))
                append(v("exportScheme","S"))
            }
            if (v("a0LmkDelim","") == "Y") {
                append("%"); append(v("lmkId","00"))
            }
        }

        "A6" -> "${h}A6${v("keyType","001")}${v("zmkScheme","U")}${v("zmk")}${v("lmkScheme","U")}${v("keyToImport")}"
        "A8" -> buildString {
            append(h); append("A8"); append(v("keyType","FFF"))
            if (v("a8SemiDelim","Y") == "Y") {
                append(";0"); append(v("zmk")); append(v("keyToExport"))
                append(v("exportScheme","S"))
            }
            if (v("a8LmkDelim","") == "Y") {
                append("%"); append(v("lmkId","00"))
            }
        }
        "BU" -> "${h}BU${v("keyType","001")}${v("scheme","U")}${v("key")}"

        "M0" -> buildString {
            val data = v("data")
            val dataLen = data.length.toString(16).padStart(4,'0').uppercase()
            val keyType = v("keyType","00A")
            val isBdk = keyType in listOf("009", "609", "809", "909")
            append(h); append("M0")
            append(v("mode","00")); append(v("inFmt","1")); append(v("outFmt","1"))
            append(keyType); append(v("key"))
            if (isBdk) {
                append(v("ksnDesc","609"))
                append(v("ksn"))
            }
            if (v("mode","00") == "01") append(v("iv","0000000000000000"))
            append(dataLen); append(data)
        }

        "M2" -> buildString {
            val data = v("data")
            val dataLen = data.length.toString(16).padStart(4,'0').uppercase()
            val keyType = v("keyType","00A")
            val isBdk = keyType in listOf("009", "609", "809", "909")
            append(h); append("M2")
            append(v("mode","00")); append(v("inFmt","1")); append(v("outFmt","1"))
            append(keyType); append(v("key"))
            if (isBdk) {
                append(v("ksnDesc","609"))
                append(v("ksn"))
            }
            if (!isBdk && v("mode","00") == "01") append(v("iv","0000000000000000"))
            append(dataLen); append(data)
        }

        "M4" -> buildString {
            val data = v("data")
            val dataLen = (data.length / 2).toString(16).padStart(4,'0').uppercase()
            append(h); append("M4")
            append(v("srcMode","00")); append(v("dstMode","00"))
            append(v("inFmt","1")); append(v("outFmt","1"))
            append(v("srcKeyType","009")); append(v("srcKey"))
            append(v("dstKeyType","00A")); append(v("dstKey"))
            append(dataLen); append(data)
        }

        "M6" -> buildString {
            val data = v("data")
            val dataLen = (data.length / 2).toString(16).padStart(4,'0').uppercase()
            append(h); append("M6")
            append(v("mode","0")); append(v("inFmt","1")); append(v("macSize","0"))
            append(v("algo","1")); append(v("padding","1"))
            append(v("keyType","008")); append(v("macKey"))
            append(dataLen); append(data)
        }

        "M8" -> buildString {
            val data = v("data")
            val dataLen = (data.length / 2).toString(16).padStart(4,'0').uppercase()
            append(h); append("M8")
            append(v("mode","0")); append(v("inFmt","1")); append(v("macSize","0"))
            append(v("algo","1")); append(v("padding","1"))
            append(v("keyType","008")); append(v("macKey"))
            append(dataLen); append(data)
            append(v("mac"))
        }

        "GM" -> buildString {
            val data = v("data")
            val dataLen = (data.length / 2).toString().padStart(5,'0')
            append(h); append("GM")
            append(v("algo","06")); append(dataLen); append(data)
        }

        "EI" -> "${h}EI${v("keyType","1")}${v("modulusLen","2048")}${v("encoding","01")}"
        "EO" -> "${h}EO${v("encoding","01")}${v("pubKey")}${v("authData","")}"

        "EW" -> buildString {
            val data = v("data")
            val dataLen = data.length.toString().padStart(4,'0')
            val privKey = v("privKey")
            val privKeyLen = privKey.length.toString().padStart(4,'0')
            append(h); append("EW")
            append(v("hash","06")); append(v("sigAlgo","01")); append(v("padMode","01"))
            append(dataLen); append(data); append(";")
            append(v("privKeyFlag","99")); append(privKeyLen); append(privKey)
        }

        "EY" -> buildString {
            val sig  = v("signature"); val sigLen  = sig.length.toString().padStart(4,'0')
            val data = v("data");      val dataLen = data.length.toString().padStart(4,'0')
            append(h); append("EY")
            append(v("hash","06")); append(v("sigAlgo","01")); append(v("padMode","01"))
            append(sigLen); append(sig); append(";")
            append(dataLen); append(data); append(";")
            append(v("mac")); append(v("pubKey"))
        }

        "PM" -> buildString {
            append(h); append("PM")
            append(v("scheme","0")); append(v("version","0"))
            append(v("mkDcvv")); append("A")
            append(v("pan")); append(";"); append(v("expDate","2512"))
            append("000")  // track data len placeholder
            append(v("atc","00001")); append(v("cvcToVerify"))
        }

        "LA" -> "${h}LA${v("indexFlag","K")}${v("index","000")}${v("numBlocks","01")}${v("data","")}"
        "LE" -> "${h}LE${v("indexFlag","K")}${v("index","000")}${v("numBlocks","01")}"

        else -> "${h}${cmd.code}"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Response formatter
// ─────────────────────────────────────────────────────────────────────────────

private val ERROR_CODES = mapOf(
    "00" to "✓  Success — no error",
    "01" to "Verification failure",
    "02" to "Key inappropriate length for algorithm",
    "04" to "Invalid PIN length",
    "05" to "Invalid key type code",
    "10" to "Source key parity error",
    "11" to "Destination key parity error",
    "12" to "Content error (PIN block format error)",
    "13" to "Invalid PIN block key length",
    "15" to "Invalid input data (length or format)",
    "16" to "Console or LMK key pair error",
    "17" to "Invalid key scheme",
    "19" to "PIN encrypted under LMK pair error",
    "20" to "Invalid PIN block format",
    "21" to "Invalid check value length",
    "22" to "Algorithm not permitted",
    "23" to "Invalid number of components",
    "24" to "Invalid PIN block format code",
    "26" to "Luhn check on PAN failed",
    "27" to "PIN block format error",
    "28" to "PEK error",
    "30" to "Specified key is not enabled",
    "40" to "Incorrect number of components",
    "42" to "Key not found",
    "45" to "Invalid key type / destination key type",
    "46" to "Invalid key scheme",
    "47" to "Key block version not supported",
    "48" to "Key block header error",
    "58" to "Invalid record number",
    "63" to "Invalid algorithm",
    "65" to "Invalid key type flag",
    "67" to "Key type not supported",
    "68" to "Command disabled / not authorized",
    "69" to "PIN authentication failure",
    "74" to "Invalid digest info",
    "75" to "Single DES key not permitted",
    "99" to "Internal HSM error",
)

private fun formatHostResponse(cmd: HostCommand, raw: String): String {
    if (raw.isBlank()) return "✗  Empty response"
    if (raw.startsWith("ERROR:")) return "✗  $raw"

    val header   = raw.take(4)
    val respCode = if (raw.length >= 6) raw.substring(4, 6) else "??"
    val errCode  = if (raw.length >= 8) raw.substring(6, 8) else "??"
    val body     = if (raw.length >  8) raw.substring(8) else ""

    val errDesc = ERROR_CODES[errCode] ?: "Unknown error code $errCode"
    val ok = errCode == "00"

    return buildString {
        appendLine("┌─── Response ──────────────────────────────────────────────┐")
        appendLine("│  Header       : $header")
        appendLine("│  Response Code: $respCode   (command ${cmd.code} → ${cmd.responseCode})")
        appendLine("│  Error Code   : $errCode   $errDesc")
        appendLine("└───────────────────────────────────────────────────────────┘")
        appendLine()

        if (!ok) {
            appendLine("✗  Command failed with error $errCode — $errDesc")
            return@buildString
        }

        // Per-command response parsing
        appendLine("✓  Command executed successfully")
        appendLine()

        when (cmd.code) {
            "NC" -> appendLine("  Status : $body")
            "CA", "CI", "G0" -> {
                val pinLen  = body.take(2)
                val pinBlk  = body.drop(2).take(16)
                val fmtCode = body.drop(18).take(2)
                appendLine("  PIN Length          : $pinLen")
                appendLine("  Output PIN Block    : $pinBlk")
                appendLine("  Output Format Code  : $fmtCode")
            }
            "JC", "JE" -> appendLine("  LMK-Encrypted PIN   : $body")
            "JG"        -> appendLine("  Output PIN Block    : $body")
            "DA", "DC"  -> appendLine("  Verification result : ${if (ok) "✓ PIN correct" else "✗ PIN incorrect"}")
            "DE"        -> appendLine("  IBM PIN Offset (12H): $body")
            "DG"        -> appendLine("  PVV (4N)            : $body")
            "EE"        -> appendLine("  Derived LMK PIN     : $body")
            "A0" -> {
                when {
                    body.length >= 40 -> {
                        val encLmk  = body.take(33)
                        val rest    = body.drop(33)
                        val kcv     = rest.takeLast(6)
                        val encTmk  = if (rest.length > 6) rest.dropLast(6) else ""
                        appendLine("  Key under LMK       : $encLmk")
                        if (encTmk.isNotBlank()) appendLine("  Key under TMK       : $encTmk")
                        appendLine("  KCV                 : $kcv")
                    }
                    else -> appendLine("  Response data       : $body")
                }
            }
            "A6", "A7" -> {
                val key = body.dropLast(6)
                val kcv = body.takeLast(6)
                appendLine("  Imported Key (LMK)  : $key")
                appendLine("  KCV                 : $kcv")
            }
            "A8", "A9" -> {
                val key = body.dropLast(6)
                val kcv = body.takeLast(6)
                appendLine("  Exported Key (ZMK)  : $key")
                appendLine("  KCV                 : $kcv")
            }
            "BU" -> appendLine("  KCV                 : $body")
            "M0", "M1" -> {
                val len  = body.take(4)
                val data = body.drop(4)
                appendLine("  Encrypted Length    : $len bytes")
                appendLine("  Encrypted Data      : $data")
            }
            "M2", "M3" -> {
                val iv  = body.take(16)
                val len = body.drop(16).take(4)
                val dec = body.drop(20)
                appendLine("  Output IV           : $iv")
                appendLine("  Decrypted Length    : $len bytes")
                appendLine("  Decrypted Data      : $dec")
            }
            "M4", "M5" -> {
                val len  = body.take(4)
                val data = body.drop(4)
                appendLine("  Translated Length   : $len bytes")
                appendLine("  Translated Data     : $data")
            }
            "M6", "M7" -> appendLine("  Calculated MAC      : $body")
            "M8", "M9" -> appendLine("  MAC verification    : ✓ MAC matches")
            "GM", "GN" -> appendLine("  Hash Value          : ${body.replace("\n","").trim()}")
            "EI", "EJ" -> {
                appendLine("  (Binary RSA key pair response)")
                appendLine("  Raw body length     : ${body.length} chars")
                appendLine("  Raw body (hex)      :")
                body.chunked(64).forEach { appendLine("    $it") }
            }
            "EO", "EP" -> {
                val mac    = body.take(8)
                val pubKey = body.drop(8)
                appendLine("  Public Key MAC (4B) : $mac")
                appendLine("  Public Key (hex)    : ${pubKey.take(64)}…")
            }
            "EW", "EX" -> {
                val sigLen = body.take(4).toIntOrNull() ?: 0
                val sig    = body.drop(4).take(sigLen * 2)
                appendLine("  Signature Length    : $sigLen bytes")
                appendLine("  Signature (hex)     :")
                sig.chunked(64).forEach { appendLine("    $it") }
            }
            "EY", "EZ" -> appendLine("  Signature validation: ✓ Signature valid")
            "PM", "PN" -> appendLine("  CVV/CVC3 validation : ✓ Verified successfully")
            "LA", "LB" -> appendLine("  Storage             : ✓ Data stored successfully")
            "LE", "LF" -> {
                appendLine("  Retrieved Data      : $body")
            }
            else -> {
                appendLine("  Raw response body   : $body")
            }
        }

        appendLine()
        appendLine("─── Raw Wire Response ─────────────────────────────────────")
        appendLine(raw.chunked(64).joinToString("\n"))
    }
}
