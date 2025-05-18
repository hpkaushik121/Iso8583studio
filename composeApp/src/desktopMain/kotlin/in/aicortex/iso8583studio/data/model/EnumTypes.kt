package `in`.aicortex.iso8583studio.data.model

import java.nio.charset.Charset


/**
 * Enumeration for gateway types
 */
enum class GatewayType {
    SERVER,
    CLIENT,
    PROXY
}

/**
 * Enumeration for transmission types
 */
enum class TransmissionType {
    SYNCHRONOUS,
    ASYNCHRONOUS
}

/**
 * Enumeration for connection types
 */
enum class ConnectionType {
    TCP_IP,
    COM,
    DIAL_UP
}

/**
 * Enumeration for cipher types
 */
enum class CipherType {
    DES,
    TRIPLE_DES,
    AES_128,
    AES_192,
    AES_256,
    RSA
}
enum class CipherMode { ECB, CBC, CFB, OFB, CTS }

/**
 * Enum for message length type
 */
enum class MessageLengthType(val value: Int) {
    BCD(0),
    NONE(3),
    STRING_4(5),
    HEX_HL(1),
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
enum class MessageEncoding(name: String?=null) {
    ASCII,
    BigEndianUnicode,
    ANSI,
    Unicode,
    UTF32,
    UTF7,
    UTF8(Charsets.UTF_8.name())
}


enum class ParsingFeature{
    NONE,
    InASCII
}


/**
 * Enum for verification errors
 */
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
enum class SignatureChecking {
    NONE,
    ONE_PASSWORD,
    CLIENT_PASSWORD
}

/**
 * Enum for transaction status
 */
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
enum class BitLength {
    FIXED, LLVAR, LLLVAR
}

enum class BitType {
    BCD, BINARY, AN, ANS, NOT_SPECIFIC
}

// Update AddtionalOption to include all options from the original code
enum class AddtionalOption {
    None,
    HideAll,
    Hide12DigitsOfTrack2
}

enum class ActionWhenDisconnect{
    DisconnectFromSourceOnly,
    DisconnectFromDestOnly,
    DisconnectFromBoth,
    NoDisconnect,
}

enum class EMVShowOption(val value: Int){
    None(0),
    Len(1),
    VALUE(2),
    NAME(4),
    DESCRIPTION(8),
    BITS(16), // 0x00000010
}

enum class ObscureType{
    None,
    ReplacedByEncryptedData,
    ReplacedByEncryptedDataSimple,
    ReplacedByZero,
}



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

enum class HashAlgorithm {
    SHA1, MD5
}

enum class EDialupStatus {
    Nothing,
    Dialing,
    WaitForCall,
    Connected
}

@ExperimentalUnsignedTypes
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