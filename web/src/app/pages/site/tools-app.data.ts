import { PageSeo, SITE_ORIGIN, canonicalUrl } from '../../core/seo';

/** A category of the in-browser app and the documentation page that covers it. */
export interface ToolsAppCategory {
  id: string;
  slug: string;
  label: string;
  docs: string;
}

/** One web-ready tool of the in-browser app, with the words people search for it by. */
export interface ToolsAppTool {
  id: string;
  label: string;
  desc: string;
  category: string;
  /** Searched-for names and phrases for this tool, most common first. */
  keywords: string[];
  /** One or two plain sentences on what the tool does, for the page copy and the description. */
  blurb: string;
}

export const TOOLS_APP_CATEGORIES: ToolsAppCategory[] = [
  { id: 'EMV', slug: 'emv-tools', label: 'EMV & Card Tools', docs: '/tools/emv-tools' },
  { id: 'CIPHER', slug: 'cipher-tools', label: 'Cryptographic Tools', docs: '/tools/cipher-tools' },
  { id: 'KEYS', slug: 'key-tools', label: 'Key Management', docs: '/tools/key-tools' },
  { id: 'PIN', slug: 'pin-tools', label: 'Payment Utilities', docs: '/tools/pin-tools' },
  { id: 'CARD_VALIDATION', slug: 'card-validation', label: 'Card Validation', docs: '/tools/card-validation' },
  { id: 'DUKPT', slug: 'dukpt-tools', label: 'DUKPT Tools', docs: '/tools/dukpt-tools' },
  { id: 'MAC', slug: 'mac-tools', label: 'MAC Tools', docs: '/tools/mac-tools' },
  { id: 'CONVERTERS', slug: 'utility-tools', label: 'Data Converters', docs: '/tools/utility-tools' },
];

const T = (id: string, label: string, desc: string, category: string, keywords: string[], blurb: string): ToolsAppTool =>
  ({ id, label, desc, category, keywords, blurb });

/** The 48 web-ready tools, in the order of the Kotlin catalog (tools-ui/…/tools/ToolCatalog.kt). */
export const TOOLS_APP_TOOLS: ToolsAppTool[] = [
  T('emv-4-1-crypto', 'EMV 4.1 Crypto', 'EMV 4.1 cryptogram validation', 'EMV',
    ['EMV 4.1 cryptogram calculator', 'ARQC calculator', 'ARPC generator', 'EMV session key derivation', 'UDK derivation EMV', 'application cryptogram verification', 'EMV CVN 10', 'EMV 4.1 Book 2 cryptography'],
    'Derive UDK and session keys, generate and verify ARQC, TC and AAC cryptograms and build ARPC responses the way EMV 4.1 Book 2 specifies.'),
  T('emv-4-2-crypto', 'EMV 4.2 Crypto', 'EMV 4.2 cryptogram validation', 'EMV',
    ['EMV 4.2 cryptogram calculator', 'ARQC verification tool', 'EMV common session key', 'ARPC method 1 method 2', 'EMV CVN 17 CVN 18', 'issuer master key derivation', 'EMV cryptogram online', 'EMV 4.2 Book 2 Annex A1'],
    'The EMV 4.2 common session key method end to end: UDK derivation, session keys from the ATC, ARQC generation and verification, and ARPC methods 1 and 2.'),
  T('mastercard-crypto', 'MasterCard Crypto', 'MasterCard cryptography', 'EMV',
    ['M/Chip cryptogram calculator', 'Mastercard ARQC calculator', 'M/Chip 4 session key', 'Mastercard EMV cryptogram', 'M/Chip Advance cryptography', 'Mastercard ARPC', 'M/Chip CVR decoder', 'Mastercard IMK derivation'],
    'M/Chip cryptography: issuer master key and UDK derivation, M/Chip session keys, application cryptograms and ARPC for Mastercard chip transactions.'),
  T('vsdc-crypto', 'VSDC Crypto', 'Visa Smart Debit/Credit', 'EMV',
    ['VSDC cryptogram calculator', 'Visa ARQC calculator', 'Visa CVN 10 CVN 18', 'VSDC session key', 'Visa EMV cryptogram', 'Visa ARPC', 'Visa IAD decoder', 'Visa Smart Debit Credit cryptography'],
    'Visa Smart Debit/Credit cryptograms for CVN 10 and CVN 18: key derivation, ARQC generation and verification, and ARPC.'),
  T('sda-verification', 'SDA Verification', 'Static Data Authentication', 'EMV',
    ['SDA verification tool', 'EMV static data authentication', 'issuer public key recovery', 'signed static application data', 'CA public key EMV', 'EMV offline data authentication', 'SSAD verification', 'EMV Book 2 SDA'],
    'Recover the issuer public key from its certificate and verify the signed static application data, the full EMV static data authentication chain.'),
  T('dda-verification', 'DDA Verification', 'Dynamic Data Authentication', 'EMV',
    ['DDA verification tool', 'EMV dynamic data authentication', 'ICC public key recovery', 'signed dynamic application data', 'CDA verification', 'internal authenticate EMV', 'EMV offline authentication DDA', 'EMV Book 2 DDA'],
    'Recover issuer and ICC public keys and verify the signed dynamic application data an INTERNAL AUTHENTICATE returns, for DDA and CDA.'),
  T('cap-token', 'CAP Token', 'Chip Authentication Program', 'EMV',
    ['CAP token calculator', 'Mastercard CAP', 'Chip Authentication Program', 'CAP OTP generator', 'EMV CAP mode', 'card reader OTP', 'CAP identify respond sign', 'EMV one time password'],
    'Compute Chip Authentication Program tokens, the one-time codes a card reader shows for identify, respond and sign modes.'),
  T('atr-parser', 'ATR Parser', 'Answer To Reset parser', 'EMV',
    ['ATR parser', 'answer to reset decoder', 'smart card ATR', 'ISO 7816 ATR', 'ATR interface bytes', 'ATR historical bytes', 'parse ATR online', 'chip card ATR analysis'],
    'Decode a smart card answer to reset: protocol, interface bytes, historical bytes and the check byte, as ISO 7816-3 defines them.'),
  T('emv-data-parser', 'EMV Data Parser', 'EMV data structure parser', 'EMV',
    ['EMV TLV parser', 'EMV data parser online', 'TLV decoder', 'BER-TLV parser', 'EMV tag decoder', 'parse EMV response', 'APDU response TLV', 'EMV field 55 parser'],
    'Parse BER-TLV EMV data into tags, lengths and values with every tag named, from a GPO or READ RECORD response to an ISO 8583 field 55.'),
  T('emv-tag-dictionary', 'EMV Tag Dictionary', 'EMV tag reference', 'EMV',
    ['EMV tag list', 'EMV tag dictionary', 'EMV tag 9F02', 'EMV tags explained', 'EMV tag 5A', 'EMV tag reference', 'EMV data elements', 'EMV tag lookup'],
    'Look up any EMV tag by number or name, with its format, length, source and meaning.'),
  T('apdu-response-query', 'APDU Response Query', 'APDU response code lookup', 'EMV',
    ['APDU status word lookup', 'SW1 SW2 meaning', 'APDU response codes', '9000 status word', '6A82 meaning', 'ISO 7816 status words', 'smart card error codes', 'APDU 6A86'],
    'Explain any APDU status word, from 9000 to the 6A and 6D families, with what the card is telling you.'),
  T('aes-calculator', 'AES Calculator', 'AES encryption/decryption', 'CIPHER',
    ['AES calculator', 'AES encryption online', 'AES decrypt hex', 'AES-128 AES-256 calculator', 'AES CBC ECB', 'AES key check value', 'AES test vectors', 'AES encrypt decrypt tool'],
    'Encrypt and decrypt with AES-128, AES-192 and AES-256 in ECB, CBC and other modes, with hex in and hex out and the key check value alongside.'),
  T('des-calculator', 'DES Calculator', 'DES/3DES operations', 'CIPHER',
    ['3DES calculator', 'DES encryption online', 'triple DES calculator', 'DES ECB CBC', 'TDES encrypt decrypt', 'DES key check value', 'double length key 3DES', 'DES hex calculator'],
    'DES and triple DES with single, double and triple length keys in ECB and CBC, plus the KCV, the way payment HSMs and test labs use them.'),
  T('rsa-calculator', 'RSA Calculator', 'RSA public key operations', 'CIPHER',
    ['RSA calculator', 'RSA encrypt decrypt online', 'RSA key generator', 'RSA sign verify', 'RSA modulus exponent', 'RSA 2048 key pair', 'RSA OAEP PKCS1', 'RSA hex calculator'],
    'Generate RSA key pairs, encrypt, decrypt, sign and verify with raw modulus and exponent values or PKCS#1 and OAEP padding.'),
  T('ecdsa-calculator', 'ECDSA Calculator', 'Elliptic curve cryptography', 'CIPHER',
    ['ECDSA calculator', 'elliptic curve signature tool', 'ECDSA sign verify online', 'secp256k1 secp256r1', 'EC key generator', 'ECDSA P-256', 'elliptic curve cryptography tool', 'ECDSA test vectors'],
    'Elliptic curve key generation, signing and verification on the common NIST and SEC curves.'),
  T('hash-calculator', 'Hash Calculator', 'MD4/MD5, SHA-1/2, CRC32, MDC-2', 'CIPHER',
    ['hash calculator', 'SHA-256 online', 'MD5 hash generator', 'SHA-1 calculator', 'CRC32 calculator', 'hash of hex string', 'SHA-512 online', 'MDC-2 hash'],
    'MD4, MD5, SHA-1, SHA-2, CRC32 and MDC-2 digests of ASCII or hex input.'),
  T('thales-rsa', 'Thales RSA', 'Thales HSM operations', 'CIPHER',
    ['Thales RSA key generation', 'payShield RSA', 'Thales HSM RSA key', 'EI command RSA', 'Thales RSA signature', 'payShield 9000 RSA', 'HSM RSA key block', 'Thales private key import'],
    'Thales payShield RSA operations: key generation, signature and verification as the HSM commands express them. A reference implementation from public material.'),
  T('fpe-calculator', 'FPE Calculator', 'Format Preserving Encryption', 'CIPHER',
    ['format preserving encryption calculator', 'FF1 FF3 encryption', 'FPE PAN tokenization', 'NIST SP 800-38G', 'FF3-1 calculator', 'format preserving encryption online', 'encrypt card number keep format', 'FPE tweak'],
    'FF1 and FF3-1 format-preserving encryption from NIST SP 800-38G, for PANs and other digit strings that must keep their shape.'),
  T('dea-keys', 'DEA Keys', 'Data Encryption Algorithm', 'KEYS',
    ['DES key parity', 'DEA key calculator', 'key check value KCV', 'adjust key parity odd', 'DES key generator', 'TDES key KCV', 'random DES key', 'weak DES key check'],
    'Generate DEA keys, fix odd parity, flag weak keys and compute key check values.'),
  T('keyshare-generator', 'Keyshare Generator', 'Key component generation', 'KEYS',
    ['key component generator', 'key share XOR', 'split key into components', 'combine key components', 'ZMK components', 'key ceremony tool', 'key component KCV', 'three component key'],
    'Split a key into XOR components for a key ceremony, or combine components back into the key, each with its KCV.'),
  T('thales-keys', 'Thales Keys', 'Thales HSM operations', 'KEYS',
    ['Thales key calculator', 'payShield key encryption', 'LMK variant', 'Thales key under LMK', 'ZPK ZMK TMK', 'Thales HSM key generation', 'payShield key check value', 'Thales variant LMK'],
    'Thales payShield key handling: encrypt and decrypt keys under LMK variants, derive types such as ZMK, ZPK and TMK, and check KCVs.'),
  T('thales-key-blocks', 'Thales Key Blocks', 'Thales key block format', 'KEYS',
    ['Thales key block', 'payShield key block decoder', 'Thales key block LMK', 'key block header Thales', 'key block vs variant', 'Thales S key block', 'payShield 10K key block', 'decode Thales key block'],
    'Build and decode Thales key blocks: header fields, key usage, algorithm, mode of use and the wrapped key.'),
  T('tr31-key-blocks', 'TR-31 Key Blocks', 'ANSI TR-31 key blocks', 'KEYS',
    ['TR-31 key block calculator', 'TR-31 decoder', 'ANSI X9.143 key block', 'TR-31 key block version B D', 'key block protection key KBPK', 'TR-31 key usage codes', 'TR-31 wrap unwrap', 'TR-31 header'],
    'Wrap and unwrap ANSI TR-31 key blocks under a KBPK: versions A to D, usage, algorithm, mode of use and exportability fields.'),
  T('ssl-certificate', 'SSL Certificate', 'SSL/TLS utilities', 'KEYS',
    ['SSL certificate decoder', 'X.509 certificate parser', 'PEM certificate viewer', 'CSR decoder', 'certificate chain check', 'TLS certificate tool', 'decode PEM online', 'certificate fingerprint'],
    'Decode X.509 certificates and CSRs from PEM or DER: subject, issuer, validity, key, extensions and fingerprints.'),
  T('rsa-der-keys', 'RSA DER Keys', 'RSA DER format', 'KEYS',
    ['RSA DER key decoder', 'PKCS#1 PKCS#8 converter', 'RSA public key ASN.1', 'DER to modulus exponent', 'PEM to DER RSA', 'RSA key format converter', 'SubjectPublicKeyInfo decode', 'RSA private key DER'],
    'Decode and convert RSA keys between PKCS#1, PKCS#8 and SubjectPublicKeyInfo DER, exposing modulus, exponent and primes.'),
  T('pin-block-calculator', 'PIN Block Calculator', 'Encoding and Decoding PIN Blocks', 'PIN',
    ['PIN block calculator', 'ISO 9564 PIN block', 'PIN block format 0', 'encode PIN block online', 'decode PIN block', 'PIN block format 1 2 3', 'encrypted PIN block 3DES', 'clear PIN block'],
    'Build and decode ISO 9564 PIN blocks in formats 0 to 3 from PIN and PAN, and encrypt or decrypt them under a PIN key.'),
  T('pin-block-aes', 'PIN Block(AES) Calculator', 'Encoding and Decoding PIN Blocks using AES', 'PIN',
    ['ISO 9564 format 4 PIN block', 'AES PIN block calculator', 'PIN block format 4', 'AES PIN encryption', 'format 4 PIN block decode', 'AES PEK PIN block', 'PCI PIN format 4', 'AES-128 PIN block'],
    'ISO 9564 format 4 PIN blocks: the AES construction with its PAN binding, encoded and decoded under an AES PIN key.'),
  T('pin-offset-ibm-3624', 'PIN Offset (IBM 3624)', 'Generating and recovering PINs using the IBM 3624 PIN Offset method', 'PIN',
    ['IBM 3624 PIN offset calculator', 'PIN offset generation', 'natural PIN IBM 3624', 'PIN verification key PVK', 'decimalization table', 'IBM 3624 PIN verification', 'PIN offset validation', 'natural PIN calculator'],
    'Generate PIN offsets and recover natural PINs with the IBM 3624 method, PVK and decimalisation table included.'),
  T('pin-pvv', 'PIN PVV Calculator', 'PIN PVV (PIN Verification Value) Calculator', 'PIN',
    ['PVV calculator', 'PIN verification value', 'Visa PVV calculator', 'PVK PVKI PVV', 'generate PVV online', 'PIN PVV verification', 'Visa PVV method', 'PVV from PIN and PAN'],
    'Compute and verify Visa PIN verification values from PIN, PAN, PVKI and the PVK pair.'),
  T('bitmap-calculator', 'Bitmap Calculator', 'ISO8583 bitmap utilities', 'PIN',
    ['ISO 8583 bitmap calculator', 'ISO 8583 bitmap decoder', 'primary secondary bitmap', 'ISO 8583 fields present', 'bitmap to field list', 'build ISO 8583 bitmap', 'ISO 8583 bitmap hex binary', 'ISO 8583 message bitmap'],
    'Decode an ISO 8583 primary and secondary bitmap into the fields present, or build the bitmap from a field list.'),
  T('mdc-hash', 'MDC Hash Calculator', 'MDC (Modification Detection Code) Hash Calculator', 'PIN',
    ['MDC-2 hash calculator', 'modification detection code', 'MDC-2 DES hash', 'MDC-4 hash', 'ISO 10118-2 hash', 'MDC hash EMV', 'DES based hash', 'MDC2 online'],
    'MDC-2 and MDC-4 modification detection codes, the DES-based hashes from ISO/IEC 10118-2.'),
  T('cvv-calculator', 'CVV Calculator', 'Card Verification Value', 'CARD_VALIDATION',
    ['CVV calculator', 'CVV2 generator', 'card verification value calculation', 'CVK A CVK B', 'iCVV calculator', 'CVC2 calculator', 'generate CVV from PAN expiry service code', 'CVV verification tool'],
    'Generate and verify CVV, CVV2 and iCVV from PAN, expiry and service code under the CVK pair.'),
  T('mastercard-cvc3', 'MasterCard CVC', 'Master Card CVC3', 'CARD_VALIDATION',
    ['CVC3 calculator', 'Mastercard dynamic CVC3', 'CVC3 ATC UN', 'PayPass CVC3', 'contactless magstripe CVC3', 'Mastercard CVC3 verification', 'IVCVC3', 'CVC3 track 2'],
    'Mastercard dynamic CVC3 for contactless magstripe mode: IVCVC3 and CVC3 from the track data, ATC and unpredictable number.'),
  T('dukpt-iso-9797', 'DUKPT ISO 9797', 'Derived unique key per transaction (ISO 9797)', 'DUKPT',
    ['DUKPT calculator', 'DUKPT key derivation', 'BDK IPEK KSN', 'DUKPT PIN encryption key', 'ANSI X9.24 DUKPT', 'DUKPT MAC key', 'DUKPT data encryption key', 'derive IPEK from BDK'],
    'ANSI X9.24-1 DUKPT with TDES: IPEK from the BDK, the transaction key from the KSN, and the PIN, MAC and data variants.'),
  T('dukpt-aes', 'DUKPT ISO AES', 'Derived unique key per transaction (AES)', 'DUKPT',
    ['AES DUKPT calculator', 'ANSI X9.24-3 DUKPT', 'AES DUKPT key derivation', 'AES BDK IPEK', 'AES DUKPT working key', 'AES-128 DUKPT', 'DUKPT 2017 AES', 'AES DUKPT KSN'],
    'ANSI X9.24-3 AES DUKPT: initial and working keys for PIN, MAC and data encryption from an AES BDK and a KSN.'),
  T('iso-9797-1-mac', 'ISO/IEC 9797-1', 'MACs according to the ISO/IEC 9797-1 standard', 'MAC',
    ['ISO 9797-1 MAC calculator', 'MAC algorithm 1 algorithm 3', 'ISO 9797 padding method 1 2', 'retail MAC ISO 9797', 'DES MAC calculator', 'CBC-MAC ISO 9797', 'MAC calculation payment', 'ISO 9797-1 MAC online'],
    'Message authentication codes per ISO/IEC 9797-1: algorithms 1 and 3 with padding methods 1 and 2.'),
  T('ansi-x9-9-x9-19-mac', 'ANSI X9.9 & X9.19', 'ANSI X9.9 and X9.19 MACs', 'MAC',
    ['ANSI X9.9 MAC calculator', 'X9.19 MAC calculator', 'ANSI X9.19 retail MAC', 'X9.9 DES MAC', 'financial message MAC', 'X9.19 3DES MAC', 'MAC ISO 8583 field 64', 'ANSI MAC calculation'],
    'ANSI X9.9 single-DES and X9.19 retail MACs for financial messages, including ISO 8583 field 64 and 128 MACs.'),
  T('as2805-4-1-mac', 'AS2805.4.1 MAC Calculator', 'MACs according to the AS2805.4.1 standard', 'MAC',
    ['AS2805 MAC calculator', 'AS2805.4.1 MAC', 'Australian payments MAC', 'AS2805 MAC key', 'AS2805 session key MAC', 'eftpos MAC calculation', 'AS2805 part 4', 'AS2805 MAC verification'],
    'MAC generation and verification per AS2805.4.1, the Australian interchange standard used by eftpos networks.'),
  T('tdes-cbc-mac', 'TDES-CBC MAC Calculator', 'MACs according to the TDES-CBC standard', 'MAC',
    ['TDES CBC MAC calculator', '3DES CBC-MAC', 'triple DES MAC', 'CBC MAC hex', 'TDES MAC online', 'DES CBC MAC last block', 'EMV TDES MAC', 'CBC-MAC calculation'],
    'Full triple DES CBC-MAC over a message, with the padding and truncation options you need for EMV and host work.'),
  T('hmac', 'HMAC MAC Calculator', 'MACs according to the HMAC standard', 'MAC',
    ['HMAC calculator', 'HMAC SHA256 online', 'HMAC SHA1 generator', 'HMAC hex key', 'HMAC-SHA512', 'HMAC MD5', 'RFC 2104 HMAC', 'HMAC verification tool'],
    'HMAC with MD5, SHA-1 and SHA-2 over ASCII or hex data, keys in hex.'),
  T('cmac', 'CMAC MAC Calculator', 'MACs according to the CMAC standard', 'MAC',
    ['CMAC calculator', 'AES CMAC online', 'AES-CMAC NIST SP 800-38B', 'CMAC subkey K1 K2', 'OMAC1 calculator', 'TDES CMAC', 'AES CMAC verification', 'CMAC hex'],
    'AES and TDES CMAC per NIST SP 800-38B, with the derived subkeys shown.'),
  T('retail-mac', 'Retail MAC Calculator', 'MACs according to the Retail standard', 'MAC',
    ['retail MAC calculator', 'ISO 9797-1 algorithm 3', 'retail MAC 3DES', 'EMV retail MAC', 'DES retail MAC last block 3DES', 'MAC retail method', 'retail MAC verification', 'ANSI X9.19 retail'],
    'The retail MAC, ISO/IEC 9797-1 algorithm 3: single DES CBC over the message with a final triple DES block.'),
  T('base64', 'Base64 Encoder', 'Base64 encoding/decoding', 'CONVERTERS',
    ['Base64 encoder', 'Base64 decode', 'Base64 to hex', 'hex to Base64', 'Base64 online tool', 'Base64 encode string', 'Base64 decoder hex output', 'URL safe Base64'],
    'Encode and decode Base64 from ASCII or hex, with hex and text views of the result.'),
  T('base94', 'Base94 Encoder', 'Base94 encoding/decoding', 'CONVERTERS',
    ['Base94 encoder', 'Base94 decode', 'Base94 encoding', 'printable ASCII encoding', 'base 94 converter', 'Base94 hex', 'compact ASCII encoding', 'Base94 online'],
    'Encode bytes as printable ASCII with Base94 and decode them back, reversibly.'),
  T('bcd-converter', 'BCD Converter', 'Binary Coded Decimal', 'CONVERTERS',
    ['BCD converter', 'binary coded decimal calculator', 'packed BCD', 'unpacked BCD', 'decimal to BCD', 'BCD to hex', 'ISO 8583 BCD fields', 'BCD encoding online'],
    'Packed and unpacked BCD in both directions, with the padding conventions ISO 8583 fields use.'),
  T('character-encoder', 'Character Encoder', 'Character encoding utilities', 'CONVERTERS',
    ['EBCDIC to ASCII converter', 'ASCII to EBCDIC', 'hex to ASCII', 'ASCII to hex', 'binary to hex converter', 'UTF-16 hex', 'EBCDIC hex converter', 'character encoding converter'],
    'Convert between ASCII, EBCDIC, hex, binary, UTF-16 and Latin-1, for the encodings host interfaces still use.'),
  T('check-digit', 'Check Digit', 'Luhn and other algorithms', 'CONVERTERS',
    ['Luhn check digit calculator', 'Luhn algorithm online', 'mod 10 check digit', 'credit card number validator', 'Verhoeff check digit', 'Damm algorithm', 'mod 97 IBAN check', 'ISBN check digit'],
    'Generate and validate check digits with Luhn, mod 11, mod 97, Verhoeff and Damm, including PAN and IBAN checks.'),
  T('track2-codec', 'Track 2 Codec', 'Encode / decode Track 2 (ASCII & EMV Tag 57)', 'CONVERTERS',
    ['Track 2 decoder', 'track 2 data parser', 'EMV tag 57 track 2 equivalent', 'track 2 PAN expiry service code', 'magnetic stripe track 2', 'track 2 discretionary data', 'parse track 2 online', 'track 2 format'],
    'Decode Track 2 and the EMV tag 57 equivalent into PAN, expiry, service code and discretionary data, or build it from the parts.'),
];

export const toolsAppCategory = (id: string): ToolsAppCategory => TOOLS_APP_CATEGORIES.find((c) => c.id === id)!;
export const toolsAppTool = (id: string): ToolsAppTool | undefined => TOOLS_APP_TOOLS.find((t) => t.id === id);
export const toolsAppPath = (id: string): string => `/tools/app/${id}`;

/** Head metadata and structured data for one tool's page. */
export function toolsAppSeo(tool: ToolsAppTool): PageSeo {
  const cat = toolsAppCategory(tool.category);
  const path = toolsAppPath(tool.id);
  const url = canonicalUrl(path);
  const name = `${tool.label} — ${tool.keywords[0]}`;
  return {
    title: `${tool.label} Online — ${tool.desc} | ISO8583Studio`,
    description: `${tool.blurb} Free ${tool.keywords[0]} that runs in your browser: nothing you enter leaves the page.`,
    keywords: [...tool.keywords, `${tool.label.toLowerCase()} online`, 'payment cryptography tool', 'ISO8583Studio'].join(', '),
    path,
    ogType: 'website',
    robots: 'index, follow',
    jsonLd: [
      {
        '@context': 'https://schema.org',
        '@type': 'SoftwareApplication',
        name,
        alternateName: tool.keywords.slice(1, 4),
        description: tool.blurb,
        url,
        applicationCategory: 'DeveloperApplication',
        applicationSubCategory: cat.label,
        operatingSystem: 'Web browser (WebAssembly)',
        browserRequirements: 'Chrome/Edge 119+, Firefox 120+, Safari 18.2+',
        isAccessibleForFree: true,
        offers: { '@type': 'Offer', price: '0', priceCurrency: 'USD' },
        keywords: tool.keywords.join(', '),
        isPartOf: { '@type': 'WebApplication', name: 'ISO8583Studio tools in the browser', url: canonicalUrl('/tools/app') },
        publisher: { '@id': `${SITE_ORIGIN}/#organization` },
      },
      {
        '@context': 'https://schema.org',
        '@type': 'BreadcrumbList',
        itemListElement: [
          { '@type': 'ListItem', position: 1, name: 'Home', item: canonicalUrl('/') },
          { '@type': 'ListItem', position: 2, name: 'Tools in the browser', item: canonicalUrl('/tools/app') },
          { '@type': 'ListItem', position: 3, name: cat.label, item: canonicalUrl(cat.docs) },
          { '@type': 'ListItem', position: 4, name: tool.label, item: url },
        ],
      },
    ],
  };
}
