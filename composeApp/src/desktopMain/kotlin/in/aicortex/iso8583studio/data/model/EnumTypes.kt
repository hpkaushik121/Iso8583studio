package `in`.aicortex.iso8583studio.data.model

import RestAuthConfig
import RestMessageFormat
import RestRetryConfig
import RestSslConfig
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.ui.graphics.vector.ImageVector
import `in`.aicortex.iso8583studio.ui.screens.components.DevelopmentStatus
import kotlinx.serialization.Serializable
import androidx.compose.material.icons.filled.*
import cafe.adriel.voyager.core.screen.Screen
import `in`.aicortex.iso8583studio.ui.navigation.Destination

enum class StudioTool(
    val label: String,
    val description: String,
    val icon: ImageVector,
    val isPopular: Boolean = false,
    val isNew: Boolean = false,
    val status: DevelopmentStatus = DevelopmentStatus.STABLE,
    val screen: Screen,
){
    // SIMULATORS
    HOST_SIMULATOR(
        "Host Simulator",
        "Payment host response simulation",
        Icons.Default.Router,
        isPopular = true,
        screen = Destination.HostSimulatorConfig,
    ),

    HSM_SIMULATOR(
        "HSM Simulator",
        "Hardware Security Module simulation",
        Icons.Default.Security,
        status = DevelopmentStatus.BETA,
        screen = Destination.HSMSimulatorConfig
    ),
    ECR_SIMULATOR(
        "ECR Simulstor",
        "Electronic Cash Register simulation",
        Icons.Default.PointOfSale,
        status = DevelopmentStatus.UNDER_DEVELOPMENT,
        screen = Destination.EcrSimulatorConfigScreen
    ),


    POS_TERMINAL(
        "POS Terminal",
        "Point of Sale terminal simulation",
        Icons.Default.PhoneAndroid,
        status = DevelopmentStatus.UNDER_DEVELOPMENT,
        screen = Destination.POSTerminalConfig
    ),

    ATM_SIMULATOR(
        "ATM Simulator",
        "ATM transaction simulation",
        Icons.Default.LocalAtm,
        status = DevelopmentStatus.UNDER_DEVELOPMENT,
        screen = Destination.ATMSimulatorConfig
    ),

    PAYMENT_SWITCH(
        "Payment Switch",
        "Transaction routing simulation",
        Icons.Default.Hub,
        status = DevelopmentStatus.COMING_SOON,
        screen = Destination.PaymentSwitchConfig
    ),

    ACQUIRER_GATEWAY(
        "Acquirer Gateway",
        "Merchant processing simulation",
        Icons.Default.AccountBalance,
        status = DevelopmentStatus.COMING_SOON,
        screen = Destination.AcquirerGatewayConfig

    ),

    ISSUER_SYSTEM(
        "Issuer System",
        "Card issuer simulation",
        Icons.Default.CorporateFare,
        status = DevelopmentStatus.COMING_SOON,
        screen = Destination.IssuerSystemConfig
    ),

    // EMV TOOLS
    APDU_SIMULATOR(
        "APDU Simulator",
        "Smart card APDU commands",
        Icons.Default.CreditCard,
        isPopular = true,
        screen = Destination.ApduSimulatorConfig
    ),

    EMV_41_CRYPTO(
        "EMV 4.1 Crypto",
        "EMV 4.1 cryptogram validation",
        Icons.Default.Lock,
        screen = Destination.EMV4_1
    ),

    EMV_42_CRYPTO(
        "EMV 4.2 Crypto",
        "EMV 4.2 cryptogram validation",
        Icons.Default.Lock,
        screen = Destination.EMV4_2
    ),

    MASTERCARD_CRYPTO(
        "MasterCard Crypto",
        "MasterCard cryptography",
        Icons.Default.Payment,
        screen = Destination.EMVMasterCardCrypto
    ),

    VSDC_CRYPTO(
        "VSDC Crypto",
        "Visa Smart Debit/Credit",
        Icons.Default.Payment,
        screen = Destination.EMVVsdcCrypto
    ),

    SDA_VERIFICATION(
        "SDA Verification",
        "Static Data Authentication",
        Icons.Default.VerifiedUser,
        screen = Destination.SDA
    ),

    DDA_VERIFICATION(
        "DDA Verification",
        "Dynamic Data Authentication",
        Icons.Default.VerifiedUser,
        screen = Destination.DDA
    ),

    CAP_TOKEN(
        "CAP Token",
        "Chip Authentication Program",
        Icons.Default.Token,
        screen = Destination.CapTokenComputation
    ),

    HCE_VISA(
        "HCE Visa",
        "Host Card Emulation",
        Icons.Default.Nfc,
        isNew = true,
        screen = Destination.HceVisa
    ),

    SECURE_MESSAGING(
        "Secure Messaging",
        "EMV secure messaging",
        Icons.Default.Message,
        screen = Destination.SecureMessagingMasterCard
    ),

    ATR_PARSER(
        "ATR Parser",
        "Answer To Reset parser",
        Icons.Default.Code,
        screen = Destination.AtrParser
    ),

    EMV_DATA_PARSER(
        "EMV Data Parser",
        "EMV data structure parser",
        Icons.Default.DataObject,
        screen = Destination.EmvDataParser
    ),

    EMV_TAG_DICTIONARY(
        "EMV Tag Dictionary",
        "EMV tag reference",
        Icons.Default.Book,
        screen = Destination.EmvTagDictionary
    ),

    // CRYPTOGRAPHY TOOLS
    AES_CALCULATOR(
        "AES Calculator",
        "AES encryption/decryption",
        Icons.Default.Lock,
        isPopular = true,
        screen = Destination.AesCalculator
    ),

    DES_CALCULATOR(
        "DES Calculator",
        "DES/3DES operations",
        Icons.Default.Lock,
        screen = Destination.DesCalculator
    ),

    RSA_CALCULATOR(
        "RSA Calculator",
        "RSA public key operations",
        Icons.Default.Key,
        screen = Destination.RsaCalculator
    ),

    ECDSA_CALCULATOR(
        "ECDSA Calculator",
        "Elliptic curve cryptography",
        Icons.Default.Timeline,
        screen = Destination.EcdsaCalculator
    ),

    HASH_CALCULATOR(
        "Hash Calculator",
        "MD5, SHA-1, SHA-256",
        Icons.Default.Tag,
        isPopular = true,
        screen = Destination.HashCalculator
    ),

    THALES_RSA(
        "Thales RSA",
        "Thales HSM operations",
        Icons.Default.Security,
        screen = Destination.ThalesRsaCalculator
    ),

    FPE_CALCULATOR(
        "FPE Calculator",
        "Format Preserving Encryption",
        Icons.Default.FormatPaint,
        isNew = true,
        screen = Destination.FpeCalculator
    ),

    // KEY MANAGEMENT TOOLS
    DEA_KEYS(
        "DEA Keys",
        "Data Encryption Algorithm",
        Icons.Default.VpnKey,
        screen = Destination.DeaKeyCalculator
    ),

    KEYSHARE_GENERATOR(
        "Keyshare Generator",
        "Key component generation",
        Icons.Default.Share,
        screen = Destination.KeyshareGenerator
    ),

    THALES_KEYS(
        "Thales Keys",
        "Thales HSM operations",
        Icons.Default.Security,
        screen = Destination.ThalesKeyCalculator
    ),

    FUTUREX_KEYS(
        "Futurex Keys",
        "Futurex HSM operations",
        Icons.Default.Security,
        screen = Destination.FuturexKeyCalculator
    ),

    ATALLA_KEYS(
        "Atalla Keys",
        "Atalla HSM operations",
        Icons.Default.Security,
        screen = Destination.AtallaKeyCalculator
    ),

    SAFENET_KEYS(
        "SafeNet Keys",
        "SafeNet HSM operations",
        Icons.Default.Security,
        screen = Destination.SafeNetKeyCalculator
    ),

    THALES_KEY_BLOCKS(
        "Thales Key Blocks",
        "Thales key block format",
        Icons.Default.ViewModule,
        screen = Destination.ThalesKeyBlockCalculator
    ),

    TR31_KEY_BLOCKS(
        "TR-31 Key Blocks",
        "ANSI TR-31 key blocks",
        Icons.Default.ViewModule,
        screen = Destination.TR31KeyBlockCalculator
    ),

    SSL_CERTIFICATE(
        "SSL Certificate",
        "SSL/TLS utilities",
        Icons.Default.Note,
        screen = Destination.SslCertificate
    ),

    RSA_DER_KEYS(
        "RSA DER Keys",
        "RSA DER format",
        Icons.Default.Code,
        screen = Destination.RsaDerPubKeyCalculator
    ),

    // ISO8583 UTILITIES
    CVV_CALCULATOR(
        "CVV Calculator",
        "Card Verification Value",
        Icons.Default.Password,
        isPopular = true,
        screen = Destination.CvvCalculator
    ),

    AMEX_CSC(
        "AMEX CSC",
        "American Express CSC",
        Icons.Default.CreditCard,
        screen = Destination.AmexCscCalculator
    ),
    MASTERCARD_CSC(
        "MasterCard CVC",
        "Master Card CVC3",
        Icons.Default.Style,
        screen = Destination.Cvc3MasterCardScreen
    ),
    DUKPT_ISO_9797(
        "DUKPT ISO 9797",
        "Derived unique key per transaction (ISO 9797)",
        Icons.Default.VpnKey,
        screen = Destination.DukptIso9797
    ),
    DUKPT_ISO_AES(
        "DUKPT ISO AES",
        "Derived unique key per transaction (AES)",
        Icons.Default.VpnKey,
        screen = Destination.DukptIsoAES
    ),
    ISO_IES_9797_1_MAC(
        "ISO/IEC 9797-1",
        "MACs according to the ISO/IEC 9797-1 standard",
        Icons.Default.Security,
        screen = Destination.Isoies97971mac
    ),
    ANSI_MAC(
        "ANSI X9.9 & X9.19",
        "MACs according to the ISO/IEC 9797-1 standard",
        Icons.Default.VerifiedUser,
        screen = Destination.AnsiMac
    ),
    AS2805_MAC(
        "AS2805.4.1 MAC Calculator",
        "MACs according to the AS2805.4.1 standard",
        Icons.Default.Verified,
        screen = Destination.AS2805MacScreen
    ),
    TDES_CBC_MAC(
        "TDES-CBC MAC Calculator",
        "MACs according to the TDES-CBC standard",
        Icons.Default.EnhancedEncryption,
        screen = Destination.TDESCBCMACScreen
    ),
    HMAC_MAC(
        "HMAC MAC Calculator",
        "MACs according to the HMAC standard",
        Icons.Default.Key,
        screen = Destination.HMACScreen
    ),
    CMAC_MAC(
        "CMAC MAC Calculator",
        "MACs according to the CMAC standard",
        Icons.Default.EnhancedEncryption,
        screen = Destination.CMACScreen
    ),
    RETAIL_MAC(
        "Retail MAC Calculator",
        "MACs according to the Retail standard",
        Icons.Default.ShoppingCart,
        screen = Destination.RetailMACScreen
    ),
    MDC_HASH(
        "MDC Hash Calculator",
        "MDC (Modification Detection Code) Hash Calculator",
        Icons.Default.Password,
        screen = Destination.MdcHashCalculatorScreen
    ),
    PIN_BLOCK_GENERAL(
        "PIN Block Calculator",
        "Encoding and Decoding PIN Blocks",
        Icons.Default.Pin,
        screen = Destination.PinBlockGeneralScreen
    ),
    PIN_BLOCK_AES(
        "PIN Block(AES) Calculator",
        "Encoding and Decoding PIN Blocks using AES",
        Icons.Default.Dialpad,
        screen = Destination.AESPinBlockScreen
    ),
    PIN_OFFSET_IBM(
        "PIN Offset (IBM 3624)",
        "Generating and recovering PINs using the IBM 3624 PIN Offset method",
        Icons.Default.Calculate,
        screen = Destination.PinOffsetScreen
    ),
    PIN_PVV(
        "PIN PVV Calculator",
        "PIN PVV (PIN Verification Value) Calculator",
        Icons.Default.Fingerprint,
        screen = Destination.PinPvvScreen
    ),
    ZKA(
        "ZKA",
        "ZKA (Zone Key Administration) operations",
        Icons.Default.VpnLock,
        screen = Destination.ZKAScreen
    ),

    AS2805_CALCULATOR(
        "AS2805 Calculator",
        "Australian payment standard",
        Icons.Default.LocationOn,
        screen = Destination.As2805Calculator
    ),

    BITMAP_CALCULATOR(
        "Bitmap Calculator",
        "ISO8583 bitmap utilities",
        Icons.Default.GridOn,
        screen = Destination.BitmapCalculator
    ),

    MESSAGE_PARSER(
        "Message Parser",
        "ISO8583 message parser",
        Icons.Default.DataObject,
        screen = Destination.MessageParser
    ),

    // CONVERTERS AND UTILITIES
    BASE64_ENCODER(
        "Base64 Encoder",
        "Base64 encoding/decoding",
        Icons.Default.Code,
        isPopular = true,
        screen = Destination.Base64Calculator
    ),

    BASE94_ENCODER(
        "Base94 Encoder",
        "Base94 encoding/decoding",
        Icons.Default.Code,
        screen = Destination.Base94Calculator
    ),

    BCD_CONVERTER(
        "BCD Converter",
        "Binary Coded Decimal",
        Icons.Default.Numbers,
        isPopular = true,
        screen = Destination.BcdCalculator
    ),

    CHARACTER_ENCODER(
        "Character Encoder",
        "Character encoding utilities",
        Icons.Default.TextFields,
        screen = Destination.CharacterEncoder
    ),

    CHECK_DIGIT(
        "Check Digit",
        "Luhn and other algorithms",
        Icons.Default.CheckCircle,
        isPopular = true,
        screen = Destination.CheckDigit
    );}


/**
 * Enumeration for gateway types
 */
@Serializable
enum class GatewayType {
    SERVER,
    CLIENT,
    PROXY
}

/**
 * Enumeration for transmission types
 */
@Serializable
enum class TransmissionType {
    SYNCHRONOUS,
    ASYNCHRONOUS
}

/**
 * Enumeration for connection types
 */
@Serializable
enum class ConnectionType {
    TCP_IP,
    COM,
    DIAL_UP,
    REST
}

/**
 * Enumeration for cipher types
 */
@Serializable
enum class CipherType {
    DES,
    TRIPLE_DES,
    AES_128,
    AES_192,
    AES_256,
    RSA
}
@Serializable
enum class CipherMode { ECB, CBC, CFB, OFB, CTS }

/**
 * Enum for message length type
 */
@Serializable
enum class MessageLengthType(val value: Int) {
    BCD(2),
    NONE(0),
    STRING_4(4),
    HEX_HL(2),
    HEX_LH(2);
    companion object {
        fun fromValue(value: Int): MessageLengthType {
            return entries.firstOrNull { it.value == value } ?: BCD
        }
    }
}
/**
 * Enumeration for special features
 */
@Serializable
enum class SpecialFeature {
    None,
    SimpleEncryptionForProxy,
    SimpleDecryptionForProxy,
    POSGateway,
    EVNComponent,
    LogAllTransaction,
    RemoveTPDU,
}

/**
 * Enumeration for special features
 */
@Serializable
enum class MessageEncoding(name: String?=null) {
    ASCII,
    BigEndianUnicode,
    ANSI,
    Unicode,
    UTF32,
    UTF7,
    UTF8(Charsets.UTF_8.name())
}


@Serializable
enum class ParsingFeature{
    NONE,
    InASCII
}


/**
 * Enum for verification errors
 */
@Serializable
enum class VerificationError {
    TIMEOUT,
    INVALID_NII,
    SOCKET_ERROR,
    OTHERS,
    DISCONNECTED_FROM_SOURCE,
    DISCONNECTED_FROM_DESTINATION,
    WRONG_MAC,
    WRONG_SIGNATURE,
    WRONG_HEADER,
    NOT_SEND_LOGON_BEFORE,
    DECLINED,
    NO_LICENSE,
    EXCEPTION_HANDLED,
    SSL_ERROR,
    WRONG_CONFIGURATION,
    MESSAGE_LENGTH_ERROR,
    PACK_DATA_ERROR,
    RELEASE_CONNECTION
}

/**
 * Enum for logging options
 */
@Serializable
enum class LoggingOption(val value: Int) {
    NONE(0),
    SIMPLE(1),
    RAW_DATA(2),
    TEXT_DATA(4),
    PARSED_DATA(8);

    infix fun and(other: LoggingOption): Int = this.value and other.value
}

/**
 * Enum for signature checking modes
 */
@Serializable
enum class SignatureChecking {
    NONE,
    ONE_PASSWORD,
    CLIENT_PASSWORD
}

/**
 * Enum for transaction status
 */
@Serializable
enum class TransactionStatus {
    NONE,
    RECEIVED_REQUEST,
    GATEWAY_HEADER_UNPACKED,
    AUTHENTICATED,
    SENT_TO_DESTINATION,
    RECEIVED_RESPONSE,
    SUCCESSFUL
}

/**
 * Enum for gateway message types
 */
@Serializable
enum class GatewayMessageType(val value: Int) {
    NORMAL_REQUEST(256),
    NORMAL_RESPONSE(272),
    LOGON_REQUEST(4096),
    LOGON_RESPONSE(4112),
    GET_KEK_REQUEST(4352),
    GET_KEK_RESPONSE(4368),
    ADMIN_REQUEST(45056),
    ADMIN_RESPONSE(45072),
    ERROR_RESPONSE(65280);

    companion object {
        fun fromValue(value: Int): GatewayMessageType {
            return values().find { it.value == value }
                ?: throw IllegalArgumentException("Unknown EGatewayMessageType value: $value")
        }
    }
}

// Enums to replace the C# enums
@Serializable
enum class BitLength {
    FIXED, LLVAR, LLLVAR
}

@Serializable
enum class BitType {
    BCD, BINARY, AN, ANS, NOT_SPECIFIC
}

// Update AddtionalOption to include all options from the original code
@Serializable
enum class AddtionalOption {
    None,
    HideAll,
    Hide12DigitsOfTrack2
}

@Serializable
enum class ActionWhenDisconnect{
    DisconnectFromSourceOnly,
    DisconnectFromDestOnly,
    DisconnectFromBoth,
    NoDisconnect,
}

@Serializable
enum class EMVShowOption(val value: Int){
    None(0),
    Len(1),
    VALUE(2),
    NAME(4),
    DESCRIPTION(8),
    BITS(16), // 0x00000010
}

@Serializable
enum class ObscureType{
    None,
    ReplacedByEncryptedData,
    ReplacedByEncryptedDataSimple,
    ReplacedByZero,
}



@Serializable
enum class GWHeaderTAG(val value: UShort) {
    TAG_START_INDICATOR(13398u),    // 0x3456
    TAG_END_INDICATOR(22136u),      // 0x5678
    TAG_CIPHER_INFO(42320u),        // 0xA550
    TAG_MESSAGETYPE(42321u),        // 0xA551
    TAG_LENGTH_OF_MESSAGE(42322u),  // 0xA552
    TAG_ENCRYPTED_CSK(42323u),      // 0xA553
    TAG_ENCRYPTED_MAC(42324u),      // 0xA554
    TAG_CLIENTID(42325u),           // 0xA555
    TAG_MERCHANTID(42326u),         // 0xA556
    TAG_Encrypted_DEK(42339u),      // 0xA563
    TAG_ENCRYPTED_MPK(42340u),      // 0xA564
    TAG_ERROR_CODE(42341u),         // 0xA565
    TAG_CLIENT_VERSION(42401u),     // 0xA5A1
    TAG_CSK(42496u),                // 0xA600
    TAG_ENCRYPTED_KEK(42752u),      // 0xA700
    TAG_IV(42753u),                 // 0xA701
    TAG_ADDITION_MESSAGE(42754u),   // 0xA702
    TAG_FILE_NAME(45056u),          // 0xB000
    TAG_FILE_OFFSET(45057u),        // 0xB001
    TAG_FILE_LENGTH(45058u),        // 0xB002
    TAG_FILE_TRANSFERED_COUNT(45059u), // 0xB003
    TAG_ADMIN_COMMAND(45824u),      // 0xB300
    TAG_AMDIN_CONTENT(45825u),      // 0xB301
    TAG_ADMIN_SECRET(45826u);       // 0xB302

    companion object {
        // Find enum by value
        fun fromValue(value: UShort): GWHeaderTAG? = values().find { it.value == value }

        // Or as an extension function on UShort
        fun UShort.toGWHeaderTAG(): GWHeaderTAG? = values().find { it.value == this }
    }
}

@Serializable
enum class HashAlgorithm {
    SHA1, MD5
}

@Serializable
enum class EDialupStatus {
    Nothing,
    Dialing,
    WaitForCall,
    Connected
}

@ExperimentalUnsignedTypes
@Serializable
enum class CKM(val value: Long) {
    RSA_PKCS_KEY_PAIR_GEN(0L),
    RSA_PKCS(1L),
    RSA_9796(2L),
    RSA_X_509(3L),
    MD2_RSA_PKCS(4L),
    MD5_RSA_PKCS(5L),
    SHA1_RSA_PKCS(6L),
    RIPEMD128_RSA_PKCS(7L),
    RIPEMD160_RSA_PKCS(8L),
    RSA_PKCS_OAEP(9L),
    RSA_X9_31_KEY_PAIR_GEN(10L),
    RSA_X9_31(11L),
    SHA1_RSA_X9_31(12L),
    RSA_PKCS_PSS(13L),
    SHA1_RSA_PKCS_PSS(14L),
    DSA_KEY_PAIR_GEN(16L),
    DSA(17L),
    DSA_SHA1(18L),
    DH_PKCS_KEY_PAIR_GEN(32L),
    DH_PKCS_DERIVE(33L),
    X9_42_DH_KEY_PAIR_GEN(48L),
    X9_42_DH_DERIVE(49L),
    X9_42_DH_HYBRID_DERIVE(50L),
    X9_42_MQV_DERIVE(51L),
    SHA256_RSA_PKCS(64L),
    SHA384_RSA_PKCS(65L),
    SHA512_RSA_PKCS(66L),
    SHA256_RSA_PKCS_PSS(67L),
    SHA384_RSA_PKCS_PSS(68L),
    SHA512_RSA_PKCS_PSS(69L),
    RC2_KEY_GEN(256L),
    RC2_ECB(257L),
    RC2_CBC(258L),
    RC2_MAC(259L),
    RC2_MAC_GENERAL(260L),
    RC2_CBC_PAD(261L),
    RC4_KEY_GEN(272L),
    RC4(273L),
    DES_KEY_GEN(288L),
    DES_ECB(289L),
    DES_CBC(290L),
    DES_MAC(291L),
    DES_MAC_GENERAL(292L),
    DES_CBC_PAD(293L),
    DES2_KEY_GEN(304L),
    DES3_KEY_GEN(305L),
    DES3_ECB(306L),
    DES3_CBC(307L),
    DES3_MAC(308L),
    DES3_MAC_GENERAL(309L),
    DES3_CBC_PAD(310L),
    CDMF_KEY_GEN(320L),
    CDMF_ECB(321L),
    CDMF_CBC(322L),
    CDMF_MAC(323L),
    CDMF_MAC_GENERAL(324L),
    CDMF_CBC_PAD(325L),
    DES_OFB64(336L),
    DES_OFB8(337L),
    DES_CFB64(338L),
    DES_CFB8(339L),
    MD2(512L),
    MD2_HMAC(513L),
    MD2_HMAC_GENERAL(514L),
    MD5(528L),
    MD5_HMAC(529L),
    MD5_HMAC_GENERAL(530L),
    SHA_1(544L),
    SHA_1_HMAC(545L),
    SHA_1_HMAC_GENERAL(546L),
    RIPEMD128(560L),
    RIPEMD128_HMAC(561L),
    RIPEMD128_HMAC_GENERAL(562L),
    RIPEMD160(576L),
    RIPEMD160_HMAC(577L),
    RIPEMD160_HMAC_GENERAL(578L),
    SHA256(592L),
    SHA256_HMAC(593L),
    SHA256_HMAC_GENERAL(594L),
    SHA384(608L),
    SHA384_HMAC(609L),
    SHA384_HMAC_GENERAL(610L),
    SHA512(624L),
    SHA512_HMAC(625L),
    SHA512_HMAC_GENERAL(626L),
    CAST_KEY_GEN(768L),
    CAST_ECB(769L),
    CAST_CBC(770L),
    CAST_MAC(771L),
    CAST_MAC_GENERAL(772L),
    CAST_CBC_PAD(773L),
    CAST3_KEY_GEN(784L),
    CAST3_ECB(785L),
    CAST3_CBC(786L),
    CAST3_MAC(787L),
    CAST3_MAC_GENERAL(788L),
    CAST3_CBC_PAD(789L),
    CAST128_KEY_GEN(800L),
    CAST5_KEY_GEN(800L),
    CAST128_ECB(801L),
    CAST5_ECB(801L),
    CAST128_CBC(802L),
    CAST5_CBC(802L),
    CAST128_MAC(803L),
    CAST5_MAC(803L),
    CAST128_MAC_GENERAL(804L),
    CAST5_MAC_GENERAL(804L),
    CAST128_CBC_PAD(805L),
    CAST5_CBC_PAD(805L),
    RC5_KEY_GEN(816L),
    RC5_ECB(817L),
    RC5_CBC(818L),
    RC5_MAC(819L),
    RC5_MAC_GENERAL(820L),
    RC5_CBC_PAD(821L),
    IDEA_KEY_GEN(832L),
    IDEA_ECB(833L),
    IDEA_CBC(834L),
    IDEA_MAC(835L),
    IDEA_MAC_GENERAL(836L),
    IDEA_CBC_PAD(837L),
    GENERIC_SECRET_KEY_GEN(848L),
    CONCATENATE_BASE_AND_KEY(864L),
    CONCATENATE_BASE_AND_DATA(866L),
    CONCATENATE_DATA_AND_BASE(867L),
    XOR_BASE_AND_DATA(868L),
    EXTRACT_KEY_FROM_KEY(869L),
    SSL3_PRE_MASTER_KEY_GEN(880L),
    SSL3_MASTER_KEY_DERIVE(881L),
    SSL3_KEY_AND_MAC_DERIVE(882L),
    SSL3_MASTER_KEY_DERIVE_DH(883L),
    TLS_PRE_MASTER_KEY_GEN(884L),
    TLS_MASTER_KEY_DERIVE(885L),
    TLS_KEY_AND_MAC_DERIVE(886L),
    TLS_MASTER_KEY_DERIVE_DH(887L),
    TLS_PRF(888L),
    SSL3_MD5_MAC(896L),
    SSL3_SHA1_MAC(897L),
    MD5_KEY_DERIVATION(912L),
    MD2_KEY_DERIVATION(913L),
    SHA1_KEY_DERIVATION(914L),
    SHA256_KEY_DERIVATION(915L),
    SHA384_KEY_DERIVATION(916L),
    SHA512_KEY_DERIVATION(917L),
    PBE_MD2_DES_CBC(928L),
    PBE_MD5_DES_CBC(929L),
    PBE_MD5_CAST_CBC(930L),
    PBE_MD5_CAST3_CBC(931L),
    PBE_MD5_CAST128_CBC(932L),
    PBE_MD5_CAST5_CBC(932L),
    PBE_SHA1_CAST128_CBC(933L),
    PBE_SHA1_CAST5_CBC(933L),
    PBE_SHA1_RC4_128(934L),
    PBE_SHA1_RC4_40(935L),
    PBE_SHA1_DES3_EDE_CBC(936L),
    PBE_SHA1_DES2_EDE_CBC(937L),
    PBE_SHA1_RC2_128_CBC(938L),
    PBE_SHA1_RC2_40_CBC(939L),
    PKCS5_PBKD2(944L),
    PBA_SHA1_WITH_SHA1_HMAC(960L),
    WTLS_PRE_MASTER_KEY_GEN(976L),
    WTLS_MASTER_KEY_DERIVE(977L),
    WTLS_MASTER_KEY_DERVIE_DH_ECC(978L),
    WTLS_PRF(979L),
    WTLS_SERVER_KEY_AND_MAC_DERIVE(980L),
    WTLS_CLIENT_KEY_AND_MAC_DERIVE(981L),
    KEY_WRAP_LYNKS(1024L),
    KEY_WRAP_SET_OAEP(1025L),
    CMS_SIG(1280L),
    SKIPJACK_KEY_GEN(4096L),
    SKIPJACK_ECB64(4097L),
    SKIPJACK_CBC64(4098L),
    SKIPJACK_OFB64(4099L),
    SKIPJACK_CFB64(4100L),
    SKIPJACK_CFB32(4101L),
    SKIPJACK_CFB16(4102L),
    SKIPJACK_CFB8(4103L),
    SKIPJACK_WRAP(4104L),
    SKIPJACK_PRIVATE_WRAP(4105L),
    SKIPJACK_RELAYX(4106L),
    KEA_KEY_PAIR_GEN(4112L),
    KEA_KEY_DERIVE(4113L),
    FORTEZZA_TIMESTAMP(4128L),
    BATON_KEY_GEN(4144L),
    BATON_ECB128(4145L),
    BATON_ECB96(4146L),
    BATON_CBC128(4147L),
    BATON_COUNTER(4148L),
    BATON_SHUFFLE(4149L),
    BATON_WRAP(4150L),
    ECDSA_KEY_PAIR_GEN(4160L),
    EC_KEY_PAIR_GEN(4160L),
    ECDSA(4161L),
    ECDSA_SHA1(4162L),
    ECDH1_DERIVE(4176L),
    ECDH1_COFACTOR_DERIVE(4177L),
    ECMQV_DERIVE(4178L),
    JUNIPER_KEY_GEN(4192L),
    JUNIPER_ECB128(4193L),
    JUNIPER_CBC128(4194L),
    JUNIPER_COUNTER(4195L),
    JUNIPER_SHUFFLE(4196L),
    JUNIPER_WRAP(4197L),
    FASTHASH(4208L),
    AES_KEY_GEN(4224L),
    AES_ECB(4225L),
    AES_CBC(4226L),
    AES_MAC(4227L),
    AES_MAC_GENERAL(4228L),
    AES_CBC_PAD(4229L),
    BLOWFISH_KEY_GEN(4240L),
    BLOWFISH_CBC(4241L),
    TWOFISH_KEY_GEN(4242L),
    TWOFISH_CBC(4243L),
    DES_ECB_ENCRYPT_DATA(4352L),
    DES_CBC_ENCRYPT_DATA(4353L),
    DES3_ECB_ENCRYPT_DATA(4354L),
    DES3_CBC_ENCRYPT_DATA(4355L),
    AES_ECB_ENCRYPT_DATA(4356L),
    AES_CBC_ENCRYPT_DATA(4357L),
    GOSTR3410_KEY_PAIR_GEN(4608L),
    GOSTR3410(4609L),
    GOSTR3410_WITH_GOSTR3411(4610L),
    GOSTR3410_DERIVE(4612L),
    GOSTR3411(4624L),
    GOST28147_KEY_GEN(4640L),
    GOST28147_ECB(4641L),
    GOST28147(4642L),
    GOST28147_KEY_WRAP(4644L),
    DSA_PARAMETER_GEN(8192L),
    DH_PKCS_PARAMETER_GEN(8193L),
    X9_42_DH_PARAMETER_GEN(8194L),
    VENDOR_DEFINED(2147483648L),
    GOSTR3410_KEY_PAIR_GEN_EX(3560050704L);

    companion object {
        // Helper method to convert from uint to enum
        fun fromValue(value: Long): CKM? = values().find { it.value == value }
    }
}
@Serializable
data class HttpHeader(
    val name: String,
    val value: String
)
@Serializable
enum class HttpMethod {
    GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS
}

@Serializable
data class RestConfiguration(
    val url: String = "",
    val method: HttpMethod = HttpMethod.POST,
    val headers: Map<String, String> = emptyMap(),
    val messageFormat: RestMessageFormat = RestMessageFormat.JSON,
    val authConfig: RestAuthConfig? = null,
    val retryConfig: RestRetryConfig = RestRetryConfig(),
    val sslConfig: RestSslConfig? = null
) {
    fun isValid(): Boolean {
        return url.isNotBlank() &&
                (url.startsWith("http://") || url.startsWith("https://"))
    }
}

// CodeFormat enum - EXACTLY matching your existing dialog
@Serializable
enum class CodeFormat(val displayName: String, val requiresYamlConfig: Boolean) {
    BYTE_ARRAY("Default", false), JSON("Json", true), XML("Xml", true), HEX("Hex", false), PLAIN_TEXT("Misc.", true)
}


enum class ValidationState {
    VALID, WARNING, ERROR, EMPTY
}

data class FieldValidation(
    val state: ValidationState,
    val message: String = "",
    val helperText: String = ""
){
    fun isValid(): Boolean{
        return true
    }
}