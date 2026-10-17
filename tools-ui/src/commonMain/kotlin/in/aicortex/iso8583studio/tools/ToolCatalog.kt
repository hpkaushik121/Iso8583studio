package `in`.aicortex.iso8583studio.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Token
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import `in`.aicortex.iso8583studio.ui.screens.Emv.ApduResponseQueryScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.AtrParserScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.CapTokenComputationScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.EmvDataParserScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.EmvTagDictionaryScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.applicationCryptogram.EmvCrypto4_2
import `in`.aicortex.iso8583studio.ui.screens.Emv.applicationCryptogram.MastercardCryptoScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.applicationCryptogram.VsdcCryptoScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.applicationCryptogram.emvCrypto41.EmvCrypto4_1
import `in`.aicortex.iso8583studio.ui.screens.Emv.dda.DdaScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.dsPartialKey.DsPartialKeyScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.hce.VisaHceCryptoScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.iccDynamicNumber.IccDynamicNumberScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.sda.SdaScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.secureMessaging.MastercardSecureMessagingScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.secureMessaging.VisaSecureMessagingScreen
import `in`.aicortex.iso8583studio.ui.screens.cipher.AesCalculatorScreen
import `in`.aicortex.iso8583studio.ui.screens.cipher.DesCalculatorScreen
import `in`.aicortex.iso8583studio.ui.screens.cipher.EcdsaCalculatorScreen
import `in`.aicortex.iso8583studio.ui.screens.cipher.RsaCalculatorScreen
import `in`.aicortex.iso8583studio.ui.screens.cipher.ThalesRsaScreen
import `in`.aicortex.iso8583studio.ui.screens.fpe.FpeCalculatorScreen
import `in`.aicortex.iso8583studio.ui.screens.generic.AESPinBlockScreen
import `in`.aicortex.iso8583studio.ui.screens.generic.Base64EncoderDecoderScreen
import `in`.aicortex.iso8583studio.ui.screens.generic.Base94EncoderDecoderScreen
import `in`.aicortex.iso8583studio.ui.screens.generic.BcdConverterScreen
import `in`.aicortex.iso8583studio.ui.screens.generic.CharacterEncodingScreen
import `in`.aicortex.iso8583studio.ui.screens.generic.CheckDigitScreen
import `in`.aicortex.iso8583studio.ui.screens.generic.HashCalculatorScreen
import `in`.aicortex.iso8583studio.ui.screens.generic.PinBlockGeneralScreen
import `in`.aicortex.iso8583studio.ui.screens.generic.Track2Screen
import `in`.aicortex.iso8583studio.ui.screens.keys.DeaKeysScreen
import `in`.aicortex.iso8583studio.ui.screens.keys.KeyshareGeneratorScreen
import `in`.aicortex.iso8583studio.ui.screens.keys.SslUtilityScreen
import `in`.aicortex.iso8583studio.ui.screens.keys.hsmKeys.AtallaKeysScreen
import `in`.aicortex.iso8583studio.ui.screens.keys.hsmKeys.FuturexKeysScreen
import `in`.aicortex.iso8583studio.ui.screens.keys.hsmKeys.SafenetKeysScreen
import `in`.aicortex.iso8583studio.ui.screens.keys.keyBlocks.ThalesKeyBlockScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.As2805CalculatorScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.BitmapScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.MDCHashScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.PinOffsetScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.PinPvvScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.ZKAScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.cardValidation.AmexCvvScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.cardValidation.CVC3Screen
import `in`.aicortex.iso8583studio.ui.screens.payments.cardValidation.CvvCalculatorScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.dukpt.DUKPTAESScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.dukpt.DukptIso9797Screen
import `in`.aicortex.iso8583studio.ui.screens.payments.macAlgorithms.ANSIMACScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.macAlgorithms.AS2805MACScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.macAlgorithms.CMACScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.macAlgorithms.HMACScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.macAlgorithms.ISO9797Screen
import `in`.aicortex.iso8583studio.ui.screens.payments.macAlgorithms.RetailMACScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.macAlgorithms.TDESCBCMACScreen
import `in`.aicortex.iso8583studio.ui.screens.rsader.RsaDerKeyScreen
import `in`.aicortex.iso8583studio.ui.screens.thaleskeys.ThalesKeysScreen
import `in`.aicortex.iso8583studio.ui.screens.tr31keyblock.Tr31KeyBlockScreen

/** The eight tool hubs of iso8583.studio; [slug] is the hub's URL segment under /tools/. */
enum class ToolCategory(val slug: String, val label: String, val description: String) {
    EMV("emv-tools", "EMV & Card Tools", "Cryptograms, SDA/DDA, CAP, HCE, parsers"),
    CIPHER("cipher-tools", "Cryptographic Tools", "AES, DES/3DES, RSA, ECDSA, FPE, hashing"),
    KEYS("key-tools", "Key Management", "HSM keys, key blocks, components"),
    PIN("pin-tools", "Payment Utilities", "PIN blocks, PVV, offsets, bitmaps"),
    CARD_VALIDATION("card-validation", "Card Validation", "CVV, CVC3, Amex CSC"),
    DUKPT("dukpt-tools", "DUKPT Tools", "BDK, IPEK and transaction keys"),
    MAC("mac-tools", "MAC Tools", "ISO 9797, X9.9/X9.19, CMAC, HMAC"),
    CONVERTERS("utility-tools", "Data Converters", "Base64, Base94, BCD, check digits, Track 2"),
}

/**
 * One calculator as the browser shell sees it. [studioToolName] is the desktop registry's
 * `StudioTool` constant (null for the three screens only reachable through a Destination),
 * which a desktop test uses to keep the two in sync.
 *
 * @property webReady false hides the tool from the public web grid (placeholder logic, or a
 *   platform limitation) while the desktop app keeps showing it.
 * @property referenceImpl the algorithm follows public references for a vendor-specific scheme
 *   and has not been confirmed against the vendor's own specification.
 */
data class WebTool(
    val id: String,
    val studioToolName: String?,
    val label: String,
    val description: String,
    val category: ToolCategory,
    val icon: ImageVector,
    val webReady: Boolean = false,
    val referenceImpl: Boolean = false,
    val content: @Composable (onBack: () -> Unit) -> Unit,
)

object ToolCatalog {
    val all: List<WebTool> = listOf(
        // ---- EMV & Card Tools ----------------------------------------------------------
        WebTool("emv-4-1-crypto", "EMV_41_CRYPTO", "EMV 4.1 Crypto", "EMV 4.1 cryptogram validation", ToolCategory.EMV, Icons.Default.Lock, webReady = true) { EmvCrypto4_1(onBack = it) },
        WebTool("emv-4-2-crypto", "EMV_42_CRYPTO", "EMV 4.2 Crypto", "EMV 4.2 cryptogram validation", ToolCategory.EMV, Icons.Default.Lock, webReady = true) { EmvCrypto4_2(onBack = it) },
        WebTool("mastercard-crypto", "MASTERCARD_CRYPTO", "MasterCard Crypto", "MasterCard cryptography", ToolCategory.EMV, Icons.Default.Payment, webReady = true) { MastercardCryptoScreen(onBack = it) },
        WebTool("vsdc-crypto", "VSDC_CRYPTO", "VSDC Crypto", "Visa Smart Debit/Credit", ToolCategory.EMV, Icons.Default.Payment, webReady = true) { VsdcCryptoScreen(onBack = it) },
        WebTool("sda-verification", "SDA_VERIFICATION", "SDA Verification", "Static Data Authentication", ToolCategory.EMV, Icons.Default.VerifiedUser, webReady = true) { SdaScreen(onBack = it) },
        WebTool("dda-verification", "DDA_VERIFICATION", "DDA Verification", "Dynamic Data Authentication", ToolCategory.EMV, Icons.Default.VerifiedUser, webReady = true) { DdaScreen(onBack = it) },
        WebTool("cap-token", "CAP_TOKEN", "CAP Token", "Chip Authentication Program", ToolCategory.EMV, Icons.Default.Token, webReady = true) { CapTokenComputationScreen(it) },
        WebTool("hce-visa", "HCE_VISA", "HCE Visa", "Host Card Emulation", ToolCategory.EMV, Icons.Default.Nfc, referenceImpl = true) { VisaHceCryptoScreen(it) },
        WebTool("secure-messaging-mastercard", "SECURE_MESSAGING", "Secure Messaging", "EMV secure messaging (Mastercard)", ToolCategory.EMV, Icons.Default.Message, referenceImpl = true) { MastercardSecureMessagingScreen(it) },
        WebTool("secure-messaging-visa", null, "Secure Messaging (Visa)", "EMV secure messaging (Visa)", ToolCategory.EMV, Icons.Default.Message, referenceImpl = true) { VisaSecureMessagingScreen(it) },
        WebTool("icc-dynamic-number", null, "ICC Dynamic Number", "Mastercard ICC dynamic number", ToolCategory.EMV, Icons.Default.Numbers, referenceImpl = true) { IccDynamicNumberScreen(onBack = it) },
        WebTool("ds-partial-key", null, "Data Storage Partial Key", "Mastercard data storage partial key", ToolCategory.EMV, Icons.Default.Storage, referenceImpl = true) { DsPartialKeyScreen(onBack = it) },
        WebTool("atr-parser", "ATR_PARSER", "ATR Parser", "Answer To Reset parser", ToolCategory.EMV, Icons.Default.Code, webReady = true) { AtrParserScreen(it) },
        WebTool("emv-data-parser", "EMV_DATA_PARSER", "EMV Data Parser", "EMV data structure parser", ToolCategory.EMV, Icons.Default.DataObject, webReady = true) { EmvDataParserScreen(it) },
        WebTool("emv-tag-dictionary", "EMV_TAG_DICTIONARY", "EMV Tag Dictionary", "EMV tag reference", ToolCategory.EMV, Icons.Default.Book, webReady = true) { EmvTagDictionaryScreen(it) },
        WebTool("apdu-response-query", "APDU_RESPONSE_QUERY", "APDU Response Query", "APDU response code lookup", ToolCategory.EMV, Icons.Default.Search, webReady = true) { ApduResponseQueryScreen(it) },

        // ---- Cryptographic Tools -------------------------------------------------------
        WebTool("aes-calculator", "AES_CALCULATOR", "AES Calculator", "AES encryption/decryption", ToolCategory.CIPHER, Icons.Default.Lock, webReady = true) { AesCalculatorScreen(it) },
        WebTool("des-calculator", "DES_CALCULATOR", "DES Calculator", "DES/3DES operations", ToolCategory.CIPHER, Icons.Default.Lock, webReady = true) { DesCalculatorScreen(it) },
        WebTool("rsa-calculator", "RSA_CALCULATOR", "RSA Calculator", "RSA public key operations", ToolCategory.CIPHER, Icons.Default.Key, webReady = true) { RsaCalculatorScreen(it) },
        WebTool("ecdsa-calculator", "ECDSA_CALCULATOR", "ECDSA Calculator", "Elliptic curve cryptography", ToolCategory.CIPHER, Icons.Default.Timeline, webReady = true) { EcdsaCalculatorScreen(it) },
        WebTool("hash-calculator", "HASH_CALCULATOR", "Hash Calculator", "MD4/MD5, SHA-1/2, CRC32, MDC-2", ToolCategory.CIPHER, Icons.Default.Tag, webReady = true) { HashCalculatorScreen(it) },
        WebTool("thales-rsa", "THALES_RSA", "Thales RSA", "Thales HSM operations", ToolCategory.CIPHER, Icons.Default.Security, webReady = true) { ThalesRsaScreen(it) },
        WebTool("fpe-calculator", "FPE_CALCULATOR", "FPE Calculator", "Format Preserving Encryption", ToolCategory.CIPHER, Icons.Default.FormatPaint, webReady = true) { FpeCalculatorScreen(it) },

        // ---- Key Management ------------------------------------------------------------
        WebTool("dea-keys", "DEA_KEYS", "DEA Keys", "Data Encryption Algorithm", ToolCategory.KEYS, Icons.Default.VpnKey, webReady = true) { DeaKeysScreen(it) },
        WebTool("keyshare-generator", "KEYSHARE_GENERATOR", "Keyshare Generator", "Key component generation", ToolCategory.KEYS, Icons.Default.Share, webReady = true) { KeyshareGeneratorScreen(it) },
        WebTool("thales-keys", "THALES_KEYS", "Thales Keys", "Thales HSM operations", ToolCategory.KEYS, Icons.Default.Security, webReady = true) { ThalesKeysScreen(it) },
        WebTool("futurex-keys", "FUTUREX_KEYS", "Futurex Keys", "Futurex HSM operations", ToolCategory.KEYS, Icons.Default.Security, referenceImpl = true) { FuturexKeysScreen(it) },
        WebTool("atalla-keys", "ATALLA_KEYS", "Atalla Keys", "Atalla HSM operations", ToolCategory.KEYS, Icons.Default.Security, referenceImpl = true) { AtallaKeysScreen(it) },
        WebTool("safenet-keys", "SAFENET_KEYS", "SafeNet Keys", "SafeNet HSM operations", ToolCategory.KEYS, Icons.Default.Security, referenceImpl = true) { SafenetKeysScreen(it) },
        WebTool("thales-key-blocks", "THALES_KEY_BLOCKS", "Thales Key Blocks", "Thales key block format", ToolCategory.KEYS, Icons.Default.ViewModule, webReady = true) { ThalesKeyBlockScreen(it) },
        WebTool("tr31-key-blocks", "TR31_KEY_BLOCKS", "TR-31 Key Blocks", "ANSI TR-31 key blocks", ToolCategory.KEYS, Icons.Default.ViewModule, webReady = true) { Tr31KeyBlockScreen(it) },
        WebTool("ssl-certificate", "SSL_CERTIFICATE", "SSL Certificate", "SSL/TLS utilities", ToolCategory.KEYS, Icons.Default.Note, webReady = true) { SslUtilityScreen(it) },
        WebTool("rsa-der-keys", "RSA_DER_KEYS", "RSA DER Keys", "RSA DER format", ToolCategory.KEYS, Icons.Default.Code, webReady = true) { RsaDerKeyScreen(it) },

        // ---- Payment Utilities ---------------------------------------------------------
        WebTool("pin-block-calculator", "PIN_BLOCK_GENERAL", "PIN Block Calculator", "Encoding and Decoding PIN Blocks", ToolCategory.PIN, Icons.Default.Pin, webReady = true) { PinBlockGeneralScreen(it) },
        WebTool("pin-block-aes", "PIN_BLOCK_AES", "PIN Block(AES) Calculator", "Encoding and Decoding PIN Blocks using AES", ToolCategory.PIN, Icons.Default.Dialpad, webReady = true) { AESPinBlockScreen(it) },
        WebTool("pin-offset-ibm-3624", "PIN_OFFSET_IBM", "PIN Offset (IBM 3624)", "Generating and recovering PINs using the IBM 3624 PIN Offset method", ToolCategory.PIN, Icons.Default.Calculate, webReady = true) { PinOffsetScreen(it) },
        WebTool("pin-pvv", "PIN_PVV", "PIN PVV Calculator", "PIN PVV (PIN Verification Value) Calculator", ToolCategory.PIN, Icons.Default.Fingerprint, webReady = true) { PinPvvScreen(it) },
        WebTool("zka", "ZKA", "ZKA", "ZKA (Deutsche Kreditwirtschaft) session keys, PIN and MAC", ToolCategory.PIN, Icons.Default.VpnLock, referenceImpl = true) { ZKAScreen(it) },
        WebTool("as2805-calculator", "AS2805_CALCULATOR", "AS2805 Calculator", "Australian payment standard", ToolCategory.PIN, Icons.Default.LocationOn, referenceImpl = true) { As2805CalculatorScreen(it) },
        WebTool("bitmap-calculator", "BITMAP_CALCULATOR", "Bitmap Calculator", "ISO8583 bitmap utilities", ToolCategory.PIN, Icons.Default.GridOn, webReady = true) { BitmapScreen(it) },
        WebTool("mdc-hash", "MDC_HASH", "MDC Hash Calculator", "MDC (Modification Detection Code) Hash Calculator", ToolCategory.PIN, Icons.Default.Password, webReady = true) { MDCHashScreen(it) },

        // ---- Card Validation -----------------------------------------------------------
        WebTool("cvv-calculator", "CVV_CALCULATOR", "CVV Calculator", "Card Verification Value", ToolCategory.CARD_VALIDATION, Icons.Default.Password, webReady = true) { CvvCalculatorScreen(it) },
        WebTool("amex-csc", "AMEX_CSC", "AMEX CSC", "American Express CSC", ToolCategory.CARD_VALIDATION, Icons.Default.CreditCard, referenceImpl = true) { AmexCvvScreen(it) },
        WebTool("mastercard-cvc3", "MASTERCARD_CSC", "MasterCard CVC", "Master Card CVC3", ToolCategory.CARD_VALIDATION, Icons.Default.Style, webReady = true) { CVC3Screen(it) },

        // ---- DUKPT ---------------------------------------------------------------------
        WebTool("dukpt-iso-9797", "DUKPT_ISO_9797", "DUKPT ISO 9797", "Derived unique key per transaction (ISO 9797)", ToolCategory.DUKPT, Icons.Default.VpnKey, webReady = true) { DukptIso9797Screen(it) },
        WebTool("dukpt-aes", "DUKPT_ISO_AES", "DUKPT ISO AES", "Derived unique key per transaction (AES)", ToolCategory.DUKPT, Icons.Default.VpnKey, webReady = true) { DUKPTAESScreen(it) },

        // ---- MAC -----------------------------------------------------------------------
        WebTool("iso-9797-1-mac", "ISO_IES_9797_1_MAC", "ISO/IEC 9797-1", "MACs according to the ISO/IEC 9797-1 standard", ToolCategory.MAC, Icons.Default.Security, webReady = true) { ISO9797Screen(it) },
        WebTool("ansi-x9-9-x9-19-mac", "ANSI_MAC", "ANSI X9.9 & X9.19", "ANSI X9.9 and X9.19 MACs", ToolCategory.MAC, Icons.Default.VerifiedUser, webReady = true) { ANSIMACScreen(it) },
        WebTool("as2805-4-1-mac", "AS2805_MAC", "AS2805.4.1 MAC Calculator", "MACs according to the AS2805.4.1 standard", ToolCategory.MAC, Icons.Default.Verified, webReady = true) { AS2805MACScreen(it) },
        WebTool("tdes-cbc-mac", "TDES_CBC_MAC", "TDES-CBC MAC Calculator", "MACs according to the TDES-CBC standard", ToolCategory.MAC, Icons.Default.EnhancedEncryption, webReady = true) { TDESCBCMACScreen(it) },
        WebTool("hmac", "HMAC_MAC", "HMAC MAC Calculator", "MACs according to the HMAC standard", ToolCategory.MAC, Icons.Default.Key, webReady = true) { HMACScreen(it) },
        WebTool("cmac", "CMAC_MAC", "CMAC MAC Calculator", "MACs according to the CMAC standard", ToolCategory.MAC, Icons.Default.EnhancedEncryption, webReady = true) { CMACScreen(it) },
        WebTool("retail-mac", "RETAIL_MAC", "Retail MAC Calculator", "MACs according to the Retail standard", ToolCategory.MAC, Icons.Default.ShoppingCart, webReady = true) { RetailMACScreen(it) },

        // ---- Data Converters -----------------------------------------------------------
        WebTool("base64", "BASE64_ENCODER", "Base64 Encoder", "Base64 encoding/decoding", ToolCategory.CONVERTERS, Icons.Default.Code, webReady = true) { Base64EncoderDecoderScreen(it) },
        WebTool("base94", "BASE94_ENCODER", "Base94 Encoder", "Base94 encoding/decoding", ToolCategory.CONVERTERS, Icons.Default.Code, webReady = true) { Base94EncoderDecoderScreen(it) },
        WebTool("bcd-converter", "BCD_CONVERTER", "BCD Converter", "Binary Coded Decimal", ToolCategory.CONVERTERS, Icons.Default.Numbers, webReady = true) { BcdConverterScreen(it) },
        WebTool("character-encoder", "CHARACTER_ENCODER", "Character Encoder", "Character encoding utilities", ToolCategory.CONVERTERS, Icons.Default.TextFields, webReady = true) { CharacterEncodingScreen(it) },
        WebTool("check-digit", "CHECK_DIGIT", "Check Digit", "Luhn and other algorithms", ToolCategory.CONVERTERS, Icons.Default.CheckCircle, webReady = true) { CheckDigitScreen(it) },
        WebTool("track2-codec", "TRACK2_CODEC", "Track 2 Codec", "Encode / decode Track 2 (ASCII & EMV Tag 57)", ToolCategory.CONVERTERS, Icons.Default.CreditCard, webReady = true) { Track2Screen(it) },
    )

    val webReady: List<WebTool> get() = all.filter { it.webReady }

    fun byId(id: String): WebTool? = all.firstOrNull { it.id == id }

    fun byCategory(category: ToolCategory): List<WebTool> = all.filter { it.category == category }
}
