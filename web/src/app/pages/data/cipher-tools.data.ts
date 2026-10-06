/**
 * Cryptographic Tools — page content and the specs its glass panels render from.
 * Ported from the prototype's cipher-tools-data.js; imported only by
 * pages/site/docs-cipher-tools.ts.
 *
 * Panel values come from the seeded hx() helper, so they are the same on the
 * server and in the browser.
 */
import { A, C, GlassSpec, H, HubStage, HubTool, S, T, grp, hx } from '../shared/glass/glass-model';
import { ToolBlock, ToolGuideData, panel, stageTag, toFeatures } from '../shared/glass/tool-guide';

interface RawSection {
  id: string;
  stage?: number;
  title: string;
  rail?: string;
  icon?: string;
  intro?: string;
  blocks: ToolBlock[];
}

const Z32 = '0'.repeat(32);
const TABS_FPE = ['FPE-FF1', 'FPE-FF2 (VAES3)', 'FPE-FF3', 'FPE-FF3-1', 'FPE-DFF[OFF-2]'];
const TABS_RSA = ['Keys', 'Encrypt', 'Decrypt', 'Sign', 'Verify', 'OAEP'];
const TABS_TH = ['Generate', 'Thales Key Block', 'Thales LMK Variant'];
const TABS_EC = ['Keys', 'Sign', 'Verify'];

const PANELS: Record<string, GlassSpec> = {
  aes: { title: 'AES Calculator', sub: 'AES-128 · CBC', tabs: null, icon: 'lock', hint: 'key length validated live · IV shown for chaining modes', fields: [S('AES Type', 'AES-128', { w: 2 }), S('Mode', 'CBC', { w: 2 }), S('Data Input Type', 'Hexadecimal', { w: 2 }), A('Input Data (Hex)', '6BC1BEE22E409F96E93D7E117393172A'), H('Key (Hex)', 32, { v: '000102030405060708090A0B0C0D0E0F', w: 3 }), H('Initial Vector (IV) (Hex)', 32, { v: Z32, w: 3 })], button: 'Encrypt', alt: 'Decrypt', result: [['Output', '7649 ABAC 8119 B246 CEE9 8E9B 12E9 197D'], ['Bytes', '16 in · 16 out · CBC']] },
  des: { title: 'DES / 3DES Calculator', sub: 'DES · ECB · PKCS#5', tabs: null, icon: 'shield', hint: 'variant inferred from key length · 16 / 32 / 48 hex', fields: [S('Algorithm', 'DES', { w: 2 }), S('Mode', 'ECB', { w: 2 }), S('Padding', 'PKCS#5', { w: 2 }), S('Data Input Type', 'ASCII', { w: 2 }), A('Input Data (ASCII)', 'Hello, World!', { w: 4 }), H('Key (Hex)', 16, { w: 6 })], button: 'Encrypt', alt: 'Decrypt', result: [['Output (HEX)', grp(hx('desout', 32))], ['Padding', 'PKCS#5 · 13 → 16 bytes']] },
  'fpe-ff1': { title: 'FPE Calculator', sub: 'FPE-FF1', tabs: TABS_FPE, tab: 0, icon: 'hash', hint: 'output keeps the input length and alphabet', fields: [S('Radix', '10', { w: 2 }), S('Encryption Type', 'AES-128', { w: 2 }), S('Key Input Type', 'Hexadecimal', { w: 2 }), H('Key', 32, { v: '2B7E151628AED2A6ABF7158809CF4F3C', w: 6 }), C('Use Tweak?', 'on', { w: 2 }), H('Tweak', 20, { v: '39383736353433323130', w: 4 }), T('Data', '0123456789012345', { w: 6 })], button: 'Encrypt', alt: 'Decrypt', result: [['Output', '6124200211725605'], ['Format', '16 digits in · 16 digits out']] },
  'fpe-ff2': { title: 'FPE Calculator', sub: 'FPE-FF2 (VAES3)', tabs: TABS_FPE, tab: 1, icon: 'hash', hint: 'output keeps the input length and alphabet', fields: [S('Radix', '10', { w: 2 }), S('Tweak Radix', '10', { w: 2 }), S('Encryption Type', 'AES-128', { w: 2 }), S('Key Input Type', 'Hexadecimal', { w: 2 }), H('Key', 32, { w: 4 }), T('Data', '4761730000000011', { w: 3 }), T('Tweak', '2609202612', { w: 3 })], button: 'Encrypt', alt: 'Decrypt', result: [['Output', '5391028477160059'], ['Format', 'radix 10 · tweak radix 10']] },
  'rsa-keys': { title: 'RSA Calculator', sub: 'Keys', tabs: TABS_RSA, tab: 0, icon: 'key', hint: 'paste existing components or generate a fresh pair', fields: [S('Key Length (bits)', '2048', { w: 2 }), H('Public Exponent (e)', 6, { v: '010001', w: 4 }), H('Modulus', 512, { w: 6 }), H('Private Exponent (d)', 512, { w: 6 })], button: 'Generate Keys', result: [['Key pair', '2048-bit · e = 010001'], ['Modulus', '256 bytes · ' + hx('rsamod', 12) + '…']] },
  'thales-gen': { title: 'Thales RSA Calculator', sub: 'Generate', tabs: TABS_TH, tab: 0, icon: 'key', hint: 'CRT components · d rebuilt from p, q and the exponents', fields: [H('Private Exp. (d)', 512, { w: 6 }), H('Prime 1 (p)', 256, { w: 3 }), H('Prime 2 (q)', 256, { w: 3 }), H('Exponent 1 (dModP1)', 256, { w: 3 }), H('Exponent 2 (dModQ1)', 256, { w: 3 }), H('Coefficient (iqmp)', 256, { w: 4 }), T('Key Length (1-4096)', '2048', { w: 2 })], button: 'Generate (d) from Components', alt: 'Generate New Random Key', result: [['d', '2048-bit private exponent · CRT consistent'], ['Check', 'n = p·q verified']] },
  'thales-kb': { title: 'Thales RSA Calculator', sub: 'Thales Key Block', tabs: TABS_TH, tab: 1, icon: 'lock-key', hint: 'wrap under the KBPK the HSM holds', fields: [H('DES KBPK', 32, { w: 3 }), H('AES KBPK', 64, { w: 3 }), T('Public Key Header', 'S10096RS00S0000', { w: 3 }), T('Private Key Header', 'S10096RD00S0000', { w: 3 }), S('Key Block Encryption', 'AES', { w: 3 }), S('Input Format', 'Hexadecimal', { w: 3 }), H('Public Key', 270, { w: 3 }), H('Private Key', 600, { w: 3 })], button: 'Wrap Key Block', alt: 'Unwrap Key Block', result: [['Key block', 'S1 · AES KBPK · ' + hx('tkb', 12) + '…'], ['MAC', 'verified · 16 bytes']] },
  'ecdsa-keys': { title: 'ECDSA Key Management', sub: 'Keys', tabs: TABS_EC, tab: 0, icon: 'function', hint: 'point checked against the selected curve', fields: [S('ECC Curve Name', 'NIST P-256', { w: 3 }), S('Public Key Form', 'Uncompressed', { w: 3 }), H('Private Key (Hex)', 64, { w: 6 }), H('Public Key (Hex)', 130, { v: '04' + hx('ecpub', 128), w: 6 })], button: 'Generate Random Key Pair', alt: 'Validate Current Key Pair', result: [['Point', 'on curve · NIST P-256'], ['Key pair', 'valid · uncompressed · 65 bytes']] },
  hash: { title: 'Hash Calculator', sub: 'SHA-256', tabs: null, icon: 'fingerprint', hint: 'digest written to the log beside its input', fields: [S('Data Input Type', 'ASCII', { w: 3 }), S('Hash Type', 'SHA-256', { w: 3 }), A('Input Data (ASCII)', 'Hello, World!')], button: 'Calculate Hash', result: [['SHA-256', 'DFFD 6021 BB2B D5B0 AF67 6290 809E C3A5 3191 DD81 C7F7 0A4B 2868 8A36 2182 986F'], ['Input', '13 bytes · ASCII']] },
};

const TOOLS: HubTool[] = [
  { id: 'aes', stage: 0, icon: 'lock', name: 'AES Calculator', desc: 'Encrypt or decrypt data with AES, or compute a Key Check Value over a known key.' },
  { id: 'des', stage: 0, icon: 'shield', name: 'DES / 3DES Calculator', desc: 'Single DES or Triple DES with a wide selection of cipher modes and padding schemes commonly used by legacy payment hosts.' },
  { id: 'fpe', stage: 0, icon: 'hash', name: 'FPE Calculator', desc: 'Format-Preserving Encryption keeps the output in the same format as the input — e.g. encrypting a 16-digit PAN into another 16-digit numeric string.' },
  { id: 'rsa', stage: 1, icon: 'key', name: 'RSA Calculator', desc: 'Six-tab tool covering the full RSA workflow from key generation through padding-aware encryption and signing.' },
  { id: 'hash', stage: 2, icon: 'fingerprint', name: 'Hash Calculator', desc: 'Generate an MD5, SHA-1 or SHA-256 digest over ASCII or hex input.' },
  { id: 'thales-rsa', stage: 1, icon: 'buildings', name: 'Thales RSA Calculator', desc: 'Vendor-aware RSA helpers tailored to Thales PayShield workflows.' },
  { id: 'ecdsa', stage: 1, icon: 'function', name: 'ECDSA Calculator', desc: 'Elliptic Curve Digital Signature Algorithm with three workflow tabs.' },
];
const STAGES: HubStage[] = [['01', 'Symmetric ciphers', 'AES · DES/3DES · FPE'], ['02', 'Asymmetric & signatures', 'RSA · Thales RSA · ECDSA'], ['03', 'Digests', 'MD5 · SHA-1 · SHA-256']];
const FAMILIES: [string, string, string][] = [
  ['lock', 'AES', '128 / 192 / 256-bit AES with selectable cipher modes for both encryption and decryption.'],
  ['shield', 'DES / 3DES', 'Single, double, and triple DES with ECB / CBC modes and automatic padding.'],
  ['key', 'RSA', 'RSA encryption, decryption, signing, and verification with custom modulus and exponents.'],
  ['function', 'ECDSA', 'Elliptic Curve Digital Signature Algorithm for key generation, signing, and verification.'],
  ['hash', 'FPE', 'Format-Preserving Encryption (FF1, FF2 / VAES3, FF3, FF3-1, DFF) for tokenization use cases.'],
  ['buildings', 'Thales RSA', 'Vendor-aware RSA helpers for Thales key blocks and LMK variant operations.'],
];

const SECTIONS: RawSection[] = [
  { id: 'common', title: 'Common UI Patterns', rail: 'UI patterns', icon: 'layout', blocks: [
    { t: 'p', x: 'All cipher calculators share the same conventions:' },
    { t: 'bullets', items: [['Two-pane layout', 'Inputs on the left, an activity log on the right that records every operation with timestamps and inputs.'], ['Data Input Type', 'Most tools have an *ASCII* / *Hexadecimal* drop-down so you can paste data either way; the field label changes based on the selection.'], ['Hex keys', 'Keys are always entered as continuous hexadecimal (no spaces, no `0x` prefix). Length is validated live.'], ['Encrypt / Decrypt buttons', 'Pair of explicit buttons rather than an Encrypt/Decrypt toggle.'], ['Copy buttons', 'Each output supports one-click copy to clipboard.']] },
    { t: 'note', tone: 'teal', icon: 'lightbulb', title: 'Tip', x: 'Use the activity log to compare consecutive runs side-by-side. The log persists until you clear it or close the tool.' },
  ] },
  { id: 'aes', stage: 0, title: 'AES Calculator', intro: 'Encrypt or decrypt data with AES, or compute a Key Check Value over a known key.', blocks: [
    panel('aes', 'AES-128 in CBC mode with hexadecimal input — the known-answer example below, run live'),
    { t: 'h3', x: 'Inputs' },
    { t: 'table', head: ['Field', 'Description'], rows: [['**AES Type**', 'Drop-down: `AES-128`, `AES-192`, `AES-256`.'], ['**Mode**', 'Drop-down: `ECB`, `CBC`, `CFB`, `OFB`, `KCV`.'], ['**Data Input Type**', 'Drop-down: `ASCII` or `Hexadecimal`. Changes the input field label.'], ['**Input Data**', 'Multi-line text. The field accepts ASCII or hex per the input type.'], ['**Key (Hex)**', '32 / 48 / 64 hex chars matching the chosen AES type.'], ['**Initial Vector (IV) (Hex)**', 'Only shown for `CBC`, `CFB`, `OFB`. 32 hex chars.']] },
    { t: 'h3', x: 'Walk-through' },
    { t: 'steps', items: ['**Pick AES Type** — `AES-128`, `AES-192`, or `AES-256`.', '**Pick Mode** — Choose a cipher mode, or pick `KCV` to compute a key check value.', '**Pick Data Input Type** — `ASCII` or `Hexadecimal`.', '**Enter Input Data** — In KCV mode the input is ignored; otherwise this is the plaintext or ciphertext.', '**Enter Key (Hex)** — A hex key whose length matches the AES type.', '**Enter IV** — If the mode requires one, enter 32 hex chars in the *Initial Vector (IV) (Hex)* field.', '**Click `Encrypt` / `Decrypt`** — Or `Calculate KCV` when the mode is `KCV`. The result is appended to the right-hand activity log with a byte count.'] },
    { t: 'note', tone: 'blue', icon: 'info', title: 'KCV Mode', x: 'Selecting **KCV** swaps the Encrypt/Decrypt buttons for a single **Calculate KCV** button and hides the IV field. The output is the standard 3-byte (6-hex) check value computed over zero plaintext.' },
    { t: 'h3', x: 'Example' },
    { t: 'code', lines: ['AES Type:        AES-128', 'Mode:            CBC', 'Data Input Type: Hexadecimal', 'Key (Hex):       000102030405060708090A0B0C0D0E0F', 'IV (Hex):        00000000000000000000000000000000', 'Input Data:      6BC1BEE22E409F96E93D7E117393172A', '', 'Output:          7649ABAC8119B246CEE98E9B12E9197D'] },
  ] },
  { id: 'des', stage: 0, title: 'DES / 3DES Calculator', intro: 'Single DES or Triple DES with a wide selection of cipher modes and padding schemes commonly used by legacy payment hosts.', blocks: [
    panel('des', 'DES with ECB mode, PKCS#5 padding and ASCII input'),
    { t: 'h3', x: 'Inputs' },
    { t: 'table', head: ['Field', 'Description'], rows: [['**Algorithm**', 'Drop-down: `DES` or `3DES`.'], ['**Mode**', 'Drop-down: `ECB`, `CBC`, `CFB-8`, `CFB-64`, `OFB-8`, `OFB-64`.'], ['**Padding**', 'Drop-down: `None`, `Zeros`, `Spaces`, `ANSI X9.23`, `ISO 10126`, `PKCS#5`, `PKCS#7`, `ISO 7816-4`, `Rijndael`, `ISO 9797-1 Method 1`, `ISO 9797-1 Method 2`.'], ['**Data Input Type**', 'Drop-down: `ASCII` or `Hexadecimal`. The input data field label updates accordingly (*Input Data (ASCII)* / *Input Data (Hex)*).'], ['**Input Data**', 'Multi-line text.'], ['**Key (Hex)**', '16 hex (DES), 32 hex (2-key 3DES) or 48 hex (3-key 3DES).'], ['**Initialization Vector (IV)**', '16 hex chars (8 bytes). Shown for non-ECB modes; a KCV chip displayed alongside.']] },
    { t: 'h3', x: 'Walk-through' },
    { t: 'steps', items: ['**Pick Algorithm** — `DES` for single DES, `3DES` for double or triple length keys.', '**Pick Mode** — `ECB` or `CBC` for the typical case; `CFB-8`/`CFB-64`/`OFB-8`/`OFB-64` for streaming variants.', '**Pick Padding** — Pick the scheme expected by your host. `ISO 9797-1 Method 1` or `Method 2` are common in payments.', '**Pick Data Input Type** — `ASCII` or `Hexadecimal`.', '**Enter Input Data** in the matching format.', '**Enter Key (Hex)** — The variant (DES, 2-key 3DES, 3-key 3DES) is inferred from the key length you provide.', '**Enter IV** if a chaining mode is selected.', '**Click `Encrypt` / `Decrypt`** — The result card displays the output in HEX, plus an ASCII view if the bytes are printable, with copy / clear actions.'] },
    { t: 'h3', x: 'Use Cases' },
    { t: 'bullets', items: ['Encrypting / decrypting PIN blocks under a working key (TPK / PEK).', 'Generating 3DES MACs by chaining ECB operations.', 'Verifying ZPK / ZMK translations during HSM integration.'] },
  ] },
  { id: 'fpe', stage: 0, title: 'FPE Calculator', intro: 'Format-Preserving Encryption keeps the output in the same format as the input — e.g. encrypting a 16-digit PAN into another 16-digit numeric string. Useful for tokenisation and PCI-scope reduction.', blocks: [
    panel('fpe-ff1', 'FPE-FF1 with radix 10, AES-128, a hexadecimal key, the Use Tweak option on and the tweak field'),
    panel('fpe-ff2', 'FPE-FF2 (VAES3) with separate Radix and Tweak Radix selectors'),
    { t: 'h3', x: 'Tabs (5)' },
    { t: 'p', x: 'Each tab is a self-contained calculator for the named variant:' },
    { t: 'bullets', items: ['**FPE-FF1**', '**FPE-FF2 (VAES3)**', '**FPE-FF3**', '**FPE-FF3-1**', '**FPE-DFF[OFF-2]**'] },
    { t: 'h3', x: 'Inputs (per tab)' },
    { t: 'table', head: ['Field', 'Description'], rows: [['**Radix**', 'Drop-down: `10` (digits), `26` (lower-case alpha), `36` (alphanumeric).'], ['**Tweak Radix**', 'Drop-down. Only shown for `FPE-FF2` and `FPE-DFF`.'], ['**Encryption Type**', 'Drop-down: `AES-128`, `AES-192`, `AES-256`.'], ['**Key Input Type**', 'Drop-down: `ASCII` or `Hexadecimal`.'], ['**Key**', 'Length matches the encryption type and input format.'], ['**Use Tweak?**', 'Checkbox — only on `FPE-FF1`. When on, an animated *Tweak* field appears.'], ['**Tweak**', 'Visible when applicable; format depends on the variant.'], ['**Data**', 'Multi-line input matching the chosen radix alphabet.']] },
    { t: 'h3', x: 'Walk-through' },
    { t: 'steps', items: ['**Pick the FPE tab** matching your spec — `FPE-FF1` or `FPE-FF3-1` for new work.', '**Pick Radix** — `10` for numeric tokens such as PANs.', '**Pick Encryption Type** — the AES variant for the underlying block cipher.', '**Pick Key Input Type** and enter the **Key**.', '(**FF1 only**) tick **Use Tweak?** if you have one, then enter it.', '**Enter Data** — The string must contain only characters in the radix alphabet.', '**Click `Encrypt` or `Decrypt`** — The output preserves the original length and alphabet.'] },
    { t: 'h3', x: 'Example: Tokenize a PAN (FPE-FF1)' },
    { t: 'code', lines: ['Radix:           10', 'Encryption Type: AES-128', 'Key Input Type:  Hexadecimal', 'Key:             2B7E151628AED2A6ABF7158809CF4F3C', 'Use Tweak?:      on', 'Tweak:           39383736353433323130', 'Data:            0123456789012345', '', 'Output:          6124200211725605'] },
  ] },
  { id: 'rsa', stage: 1, title: 'RSA Calculator', intro: 'Six-tab tool covering the full RSA workflow from key generation through padding-aware encryption and signing.', blocks: [
    panel('rsa-keys', 'The Keys tab with a 2048-bit key length above the Modulus, Public Exponent and Private Exponent fields'),
    { t: 'h3', x: 'Tabs (6)' },
    { t: 'bullets', items: [['Keys', 'Generate or paste a key pair.'], ['Encrypt', 'Encrypt with PKCS1 or no padding.'], ['Decrypt', 'Decrypt ciphertext with the corresponding key.'], ['Sign', 'Produce a signature over a message or hash.'], ['Verify', 'Verify a signature against a hash.'], ['OAEP', 'Encode / decode with OAEP padding.']] },
    { t: 'h3', x: 'Keys Tab' },
    { t: 'bullets', items: ['**Key Length (bits)** — `1024`, `2048`, `3072`, `4096`.', '**Generate Keys** button.', '**Modulus**, **Public Exponent (e)**, **Private Exponent (d)** — All hex; you can also paste pre-existing components instead of generating.'] },
    { t: 'h3', x: 'Encrypt Tab' },
    { t: 'bullets', items: ['**Encoding Method** — `Public` (encrypt with public key) or `Private`.', '**Padding** — `PKCS1` or `No Padding`.', '**Input Data Format** — `ASCII` or `Hex`.', '**Data to Encrypt**.', 'Button: **Encrypt**.'] },
    { t: 'h3', x: 'Decrypt Tab' },
    { t: 'bullets', items: ['**Decoding Method** — `Private` or `Public`.', '**Padding** — `PKCS1` or `No Padding`.', '**Data to Decrypt (Hex)**.', 'Button: **Decrypt**.'] },
    { t: 'h3', x: 'Sign / Verify Tabs' },
    { t: 'bullets', items: ['**Sign** — *Input Data Format* (ASCII / Hex) and *Data to Sign*; click **Sign**.', '**Verify** — *Hash (Hex)* and *Signature (Hex)*; click **Verify**.'] },
    { t: 'h3', x: 'OAEP Tab' },
    { t: 'bullets', items: ['**Method** — `Encode` or `Decode`.', '**Hash Function** — `SHA-1`, `SHA-224`, `SHA-256`, `SHA-384`, `SHA-512`.', '**Result Length (bits)** — `1024`, `2048`, `4096`.', '**Data (Hex)**, **Encoding Parameters (Label, Hex)**.', 'Button label changes between **Encode** and **Decode** based on the selected method.'] },
  ] },
  { id: 'thales-rsa', stage: 1, title: 'Thales RSA Calculator', intro: 'Vendor-aware RSA helpers tailored to Thales PayShield workflows.', blocks: [
    panel('thales-gen', 'The Generate tab: private exponent, primes, exponents and coefficient with a 2048-bit key length'),
    { t: 'h3', x: 'Tabs (3)' },
    { t: 'h4', x: 'Generate' },
    { t: 'p', x: 'Construct an RSA key pair from CRT components (or generate a fresh one). Inputs:' },
    { t: 'bullets', items: ['**Private Exp. (d)**', '**Prime 1 (p)**', '**Prime 2 (q)**', '**Exponent 1 (dModP1)**', '**Exponent 2 (dModQ1)**', '**Coefficient (iqmp)**', '**Key Length (1-4096)**'], cols: 2 },
    { t: 'p', x: 'Buttons: **Generate (d) from Components**, **Generate New Random Key**.' },
    panel('thales-kb', 'Thales Key Block: DES and AES KBPK fields, public and private key headers, AES key block encryption and Wrap / Unwrap'),
    { t: 'h4', x: 'Thales Key Block' },
    { t: 'p', x: 'Wrap or unwrap a key pair under a Key Block Protection Key. Inputs:' },
    { t: 'bullets', items: ['**DES KBPK**, **AES KBPK**', '**Public Key Header**, **Private Key Header**', '**Key Block Encryption** — `AES` or `DES`.', '**Input Format** — `ASCII` or `Hex`.', '**Public Key**, **Private Key**'] },
    { t: 'p', x: 'Buttons: **Wrap Key Block**, **Unwrap Key Block**.' },
    { t: 'h4', x: 'Thales LMK Variant' },
    { t: 'p', x: 'Apply Thales LMK variants. Inputs:' },
    { t: 'bullets', items: ['**LMK Pair 34-35**', '**LMK Pair 36-37**', '**Authentication Data**', '**Modulus Encoding** — `DEC` / `DER` variants.', '**Public Key**, **Private Key**'] },
    { t: 'p', x: 'Button: **Process LMK Variant**.' },
  ] },
  { id: 'ecdsa', stage: 1, title: 'ECDSA Calculator', intro: 'Elliptic Curve Digital Signature Algorithm with three workflow tabs.', blocks: [
    panel('ecdsa-keys', 'ECDSA Key Management on the Keys tab with NIST P-256 selected and the uncompressed public key form'),
    { t: 'h3', x: 'Keys Tab' },
    { t: 'bullets', items: ['**ECC Curve Name** — `NIST P-256`, `NIST P-384`, `NIST P-521`, plus the Brainpool curve variants.', '**Private Key (Hex)**.', '**Public Key (Hex)**.', '**Public Key Form** — `Uncompressed` or `Compressed`.'] },
    { t: 'p', x: 'Buttons: **Generate New Public Key**, **Is Point on Curve?**, **Generate Random Key Pair**, **Validate Current Key Pair**.' },
    { t: 'h3', x: 'Sign Tab' },
    { t: 'bullets', items: ['**Hash Type** — `SHA-1`, `SHA-256`, `SHA-384`, `SHA-512`.', '**Input Data Format** — `ASCII` or `Hex`.', '**Data to Sign**.'] },
    { t: 'p', x: 'Button: **Sign Data**. Output is the `(r, s)` pair in hex.' },
    { t: 'h3', x: 'Verify Tab' },
    { t: 'bullets', items: ['**Hash (Hex)**.', '**Signature (Hex)**.'] },
    { t: 'p', x: 'Button: **Verify Signature**.' },
  ] },
  { id: 'cipher-modes', title: 'Cipher Modes Reference', rail: 'Cipher modes', icon: 'list-dashes', blocks: [
    { t: 'p', x: 'Mode availability depends on the calculator. AES exposes ECB / CBC / CFB / OFB / KCV. DES / 3DES exposes ECB, CBC, and the CFB-8 / CFB-64 / OFB-8 / OFB-64 byte/feedback variants.' },
    { t: 'table', head: ['Mode', 'IV?', 'Properties'], cols: '160px 56px minmax(0,1fr)', rows: [['`ECB`', 'No', 'Each block independent. Simple but leaks plaintext patterns.'], ['`CBC`', 'Yes', 'Each block XOR-chained with the previous ciphertext. Secure with unique IV.'], ['`CFB / CFB-8 / CFB-64`', 'Yes', 'Self-synchronising stream mode. The numeric variants set the feedback width in bits.'], ['`OFB / OFB-8 / OFB-64`', 'Yes', 'Key-stream mode independent of plaintext.'], ['`KCV`', 'No', 'AES-only. Computes the standard 3-byte Key Check Value over zero plaintext.']] },
  ] },
  { id: 'hash', stage: 2, title: 'Hash Calculator', intro: 'Generate a message digest over arbitrary input. Useful for checking a known-answer test vector, or for producing the hash an RSA or ECDSA *Verify* step expects as its input.', blocks: [
    panel('hash', 'ASCII input, SHA-256 selected, the text Hello, World! in the input field'),
    { t: 'h3', x: 'Inputs' },
    { t: 'table', head: ['Field', 'Description'], rows: [['`Data Input Type`', 'Drop-down: `ASCII` or `Hexadecimal`. Choose `Hexadecimal` when hashing raw bytes such as a key or a block of card data.'], ['`Hash Type`', 'Drop-down: `MD5`, `SHA-1`, `SHA-256`.'], ['`Input Data`', 'The message to digest, in the format chosen above.']] },
    { t: 'h3', x: 'Walk-through' },
    { t: 'steps', items: ['**Pick Data Input Type** — ASCII for text, Hexadecimal for raw bytes.', '**Pick Hash Type**.', '**Enter Input Data**.', 'Click **Calculate Hash**. The digest is written to the activity log alongside the input, so a run can be compared against a published vector.'] },
    { t: 'note', tone: 'warn', icon: 'warning', title: 'Note', x: 'MD5 and SHA-1 are here because payment protocols and test packs still reference them. Neither is collision-resistant — use them to reproduce an existing vector, not to secure new data.' },
  ] },
  { id: 'padding', title: 'Padding Schemes (DES / 3DES)', rail: 'Padding schemes', icon: 'rows', blocks: [
    { t: 'p', x: 'The DES / 3DES calculator exposes the full set of padding schemes used across legacy and modern payment protocols. Pick the one your host expects:' },
    { t: 'table', head: ['Scheme', 'Notes'], cols: '200px minmax(0,1fr)', rows: [['`None`', 'Input must be an exact multiple of 8 bytes.'], ['`Zeros`', 'Pads with `00` bytes. Cannot recover trailing zero bytes.'], ['`Spaces`', 'Pads with ASCII space (`0x20`).'], ['`ANSI X9.23`', 'Random bytes followed by a length byte.'], ['`ISO 10126`', 'Random bytes followed by a length byte (similar to X9.23).'], ['`PKCS#5` / `PKCS#7`', 'Self-describing: pad bytes equal the pad length.'], ['`ISO 7816-4`', 'Single `0x80` followed by zero bytes.'], ['`Rijndael`', 'Variant used in the original Rijndael spec.'], ['`ISO 9797-1 Method 1`', 'Zero-pad to next block boundary (no length signal).'], ['`ISO 9797-1 Method 2`', 'Single `0x80` then zeros — recommended for MAC inputs.']] },
  ] },
];

const TIPS: string[] = [
  'Use the **Bitmap Calculator** under `Tools → Payment Utilities` to inspect block alignment when sizes look odd.',
  'For HSM-bound keys, prefer **Thales RSA → Thales Key Block** over the raw RSA tool to keep wrapping consistent.',
  'If a known answer test fails, double-check the IV is in hex (not ASCII) and the data length matches the mode requirements.',
];


export const CIPHER_TOOLS_GUIDE: ToolGuideData = {
  slug: 'cipher-tools',
  crumb: 'Cryptographic Tools',
  meta: '7 tools · hex inputs · audit logs',
  title: 'Cryptographic Tools',
  lede: 'Symmetric and asymmetric encryption calculators for testing payment cryptography. AES, DES/3DES, RSA, ECDSA, and Format-Preserving Encryption (FPE) variants are all supported with hex inputs and detailed audit logs.',
  hub: {
    title: 'Cryptographic Tools',
    sub: 'Encryption, decryption and security utilities',
    tools: TOOLS,
    stages: STAGES,
    columns: 4,
    category: 2,
    search: 'Search cipher tools…',
    badge: '7 tools',
    label: 'One calculator per algorithm family',
    note: 'Timestamps and round-trip values on every operation.',
    aria: 'The Cryptographic Tools hub: tool categories, three algorithm families and the seven calculators in the group',
  },
  panels: PANELS,
  panelBadge: 'CIPHER',
  captionFrom: 'title',
  intro: {
    eyebrow: 'cipher-tools · Cryptographic Tools hub — above',
    paras: [
      'The **Cryptographic Tools** hub — encryption, decryption and security utilities — gathers them in one place. Each calculator is dedicated to a single algorithm family and exposes the inputs typically required for payment-system testing: hex keys, hex data, IV/tweak values, and mode/padding selectors. Every operation is recorded in the activity log with timestamps and round-trip values.',
    ],
    features: toFeatures(FAMILIES),
  },
  allTools: {
    lede: 'Every tool in this category — each card links to the detailed reference below.',
    cards: TOOLS.map((tool) => ({ id: tool.id, icon: tool.icon, name: tool.name, desc: tool.desc, tag: stageTag(STAGES, tool.stage) })),
  },
  sections: SECTIONS.map((section) => ({
    id: section.id,
    title: section.title,
    rail: section.rail,
    icon: section.icon || TOOLS.find((tool) => tool.id === section.id)?.icon || 'circle',
    eyebrow: section.stage !== undefined ? stageTag(STAGES, section.stage) : undefined,
    intro: section.intro,
    blocks: section.blocks,
  })),
  tips: TIPS,
  cta: {
    heading: 'Try it on your own transactions',
    text: 'Free and open source. Download the studio and run these calculators on your desk in minutes.',
  },
};
