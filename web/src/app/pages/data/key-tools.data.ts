/**
 * Key Management Tools — page content and the specs its glass panels render from.
 * Ported from the prototype's key-tools-data.js; imported only by
 * pages/site/docs-key-tools.ts.
 *
 * Every panel output that can be computed is a real value for the inputs shown (verified with
 * openssl against the app's algorithms); outputs that depend on random padding or vendor secrets
 * describe the result's layout instead of inventing hex. Field defaults still come from hx().
 */
import { A, C, GlassSpec, H, HubStage, HubTool, S, T, grp } from '../shared/glass/glass-model';
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

// DES parity: the low bit of each byte is set so the byte has an odd number of 1 bits.
const odd = (h: string): string => h.match(/../g)!.map((b) => { const v = parseInt(b, 16); let n = 0; for (let x = v >> 1; x; x >>= 1) n += x & 1; return ((n & 1) ? v & 0xFE : v | 1).toString(16).toUpperCase().padStart(2, '0'); }).join('');
const xor = (a: string, b: string): string => a.match(/../g)!.map((x, i) => (parseInt(x, 16) ^ parseInt(b.slice(i * 2, i * 2 + 2), 16)).toString(16).toUpperCase().padStart(2, '0')).join('');
// Example keys with their real key check values: TDES-ECB of eight zero bytes, first three bytes
// (single DES for the Safenet key). Recompute with `openssl enc -des-ede3 -K <key> -nopad` over 0000000000000000.
const KEY = '8CB045316DFBB0C1F19D7F2FA76179AD', KEY_KCV = '4BDF55';
const KBPK = '573ED683BE9C1FE2CF4F0FB89A25AF41', KBPK_KCV = '9F1B58', MFK = '66BA595A761561D49797211430EC05EA';
const K1 = 'DF92135BB9736289855794F1F279C194', K2 = 'E6649D8AC15E679873925BE9C7940816', K3 = 'EF3826BCC74C6DA862CD20E09BAE519B';
const KCV = { k1: '0AF4A5', k2: 'DBA8F5', k3: '617F0D', c1: 'DF76AF', c2: '83F69B', c3: 'D0CDBA', comb: 'C02DBB', share: '6C43E7', sn: '941099' };
const RAW = '107C8D6ACEC530917170AA7970CD5F8E', FIXED = odd(RAW), CHANGED = RAW.match(/../g)!.filter((b, i) => b !== FIXED.slice(i * 2, i * 2 + 2)).length;
const C1 = '4F7CF7D3DC8016C4389EBF3EC2D537DA', C2 = '38045740C7BA08946BEF6D5746835E51', C3 = '3ED6E070CEA80DA45B2A324F1A9EA7D5', COMB = xor(xor(C1, C2), C3);
const P1 = 'B99E6EAA64AB28A713BBFEC3E0445F2A', P2 = 'A226CF174FE51977460BACDA63F287A0', SHARE = xor(P1, P2);
// Key blocks carry random padding and vendor-encrypted bodies, so the panels show each block's layout rather than one wrapping.
const AKB = '1PUNE000,<48 hex: key under the MFK>,<16 hex: MAC>';
// A real RSA-2048 modulus (openssl genrsa), so the DER example encodes a genuine key.
const MOD = 'DD597D633DE2DF0F08CCFD84587996476EE11335CC344044561E69CD36111C7FFA9833A7811BDF9227057B29F618A78F1F201C251A653EFB0DB0F7C62C098009AFA555B3F8AFE90EE0C22E41EDE052AE89979DD006A207588B03DF5205CE545309AAEA19B36041B61DE32A987FCEF6A37A2BFE588ADBF301FF4F754C1A4C5BF0E0B5AC7C49FC68A69D79D31273D9D0713494C92A1CDCE375893DD4ABE946AA9E22EF003BF7235542904CF7C7C8601D35575787E37BDD06A5D2AFE38F7FC60162957309C6BDBCC1A894735C5AF494CBAACF9BFB3F6A3E125E08F9DC94C967B3685AF2718DEEED3A20A57C0936840C18493C7AA68029332084C2AB89551123FE5B';
const TABS_DEA = ['Key Generator', 'Key Combination', 'Parity Enforcement', 'Key Validation'];
const TABS_SHARE = ['2 Parts', '3 Parts'];
const TABS_TR31 = ['Wrap', 'Unwrap'];
const TABS_TKB = ['Encode', 'Decode'];
const TABS_ATALLA = ['Key Encryption', 'AKB Decode'];
const TABS_SSL = ['Keys', 'CSRs', 'Read CSR', 'Self-Signed', 'Read Certificate'];

const PANELS: Record<string, GlassSpec> = {
  'dea-gen': { title: 'DEA Keys Calculator', sub: 'Key Generator', tabs: TABS_DEA, tab: 0, icon: 'key', hint: 'odd parity enforced · KCV over 00 00 00 00 00 00 00 00', fields: [S('Keys to Generate', '3', { w: 2 }), S('Key Length', '128-bit (TDES double)', { w: 2 }), S('Key Parity', 'Odd', { w: 2 })], button: 'Generate Keys', result: [['Key 1', grp(K1) + ' · KCV ' + KCV.k1], ['Key 2', grp(K2) + ' · KCV ' + KCV.k2], ['Key 3', grp(K3) + ' · KCV ' + KCV.k3]] },
  'dea-combine': { title: 'DEA Keys Calculator', sub: 'Key Combination', tabs: TABS_DEA, tab: 1, icon: 'arrows-merge', hint: 'eight component slots · each with its own KCV', fields: [S('Key Type / Length', 'TDES — Double length (16B / 32H)', { w: 6 }), H('Component 1', 32, { v: C1, w: 4 }), T('KCV 1', KCV.c1, { w: 2 }), H('Component 2', 32, { v: C2, w: 4 }), T('KCV 2', KCV.c2, { w: 2 }), H('Component 3', 32, { v: C3, w: 4 }), T('KCV 3', KCV.c3, { w: 2 })], button: 'Combine Components', result: [['Combined Key', grp(COMB)], ['KCV', KCV.comb + ' · 3 of 8 components used']] },
  'dea-parity': { title: 'DEA Keys Calculator', sub: 'Parity Enforcement', tabs: TABS_DEA, tab: 2, icon: 'scales', hint: 'only the low bit of each byte moves · cipher value unchanged', fields: [H('Key (Hex)', 32, { v: RAW, w: 4 }), S('Key Parity', 'Odd', { w: 2 })], button: 'Enforce Parity', result: [['Adjusted Key', grp(FIXED)], ['Changed', CHANGED + ' of 16 bytes · KCV unchanged']] },
  'dea-lookup': { title: 'DEA Keys Calculator', sub: 'Key Validation', tabs: TABS_DEA, tab: 3, icon: 'magnifying-glass', hint: 'reports what the key actually is', fields: [H('Key (Hex)', 32, { v: K1, w: 4 }), C('Check KCV?', 'on', { w: 2 }), H('KCV (Optional)', 6, { v: KCV.k1, w: 3, opt: true }), S('Parity', 'Odd', { w: 3 })], button: 'Lookup Key', result: [['Key', 'TDES double length · odd parity · not weak'], ['KCV', 'matches ' + KCV.k1]] },
  keyshare: { title: 'Keyshare Generator', sub: '2 Parts', tabs: TABS_SHARE, tab: 0, icon: 'arrows-split', hint: 'leave parts empty to have them generated', fields: [S('Parity', 'Ignore', { w: 3 }), S('Key Type', 'DES/TDES', { w: 3 }), H('Part 1', 32, { v: P1, w: 3, opt: true }), H('Part 2', 32, { v: P2, w: 3, opt: true })], button: 'Generate 2 Parts', result: [['Combined Key', grp(SHARE)], ['KCV', KCV.share + ' · XOR of both parts']] },
  tr31: { title: 'TR-31 Key Block', sub: 'Wrap', tabs: TABS_TR31, tab: 0, icon: 'package', hint: 'header codes are uppercase · version B for a TDES KBPK', fields: [H('KBPK', 32, { v: KBPK, w: 3 }), H('Clear Key', 32, { v: KEY, w: 3 }), S('Version ID', 'B', { w: 2 }), S('Key Usage', 'P0 — PIN encryption', { w: 2 }), S('Algorithm', 'T — TDES', { w: 2 }), S('Mode of Use', 'E — encrypt', { w: 2 }), T('Key Version Number', '00', { w: 2 }), S('Exportability', 'E — exportable', { w: 2 })], button: 'Wrap', alt: 'Unwrap', result: [['Key Block', 'B0080P0TE00E0000 + 48 hex wrapped key + 16 hex CMAC · padding is random, so every wrap differs'], ['MAC', 'CMAC verified · 8 bytes · 80 chars']] },
  'thales-kb': { title: 'Thales Key Block', sub: 'Encode', tabs: TABS_TKB, tab: 0, icon: 'cube', hint: 'imports a clear key under the HSM Simulator’s LMK', fields: [S('Key Block Version', '0 — 3DES KBPK', { w: 3 }), S('Thales Key Type', 'ZPK', { w: 3 }), H('Key Block Protection Key', 32, { v: KBPK, w: 4 }), T('KBPK KCV', KBPK_KCV, { w: 2 }), H('Clear Key', 32, { v: KEY, w: 4 }), S('LMK Variant', '00', { w: 2 })], button: 'Encode', alt: 'Decode', result: [['Key under LMK', 'S0… · 16-char header, encrypted key, 8-hex MAC · padding is random, so every encode differs'], ['KCV', KEY_KCV]] },
  'thales-keys': { title: 'Thales Key Calculator', sub: 'Keys Encryption / Decoding', tabs: null, icon: 'calculator', hint: 'matches A0 / A6 host-command results · 6-digit KCVs', fields: [H('Key (Hex)', 32, { v: KEY, w: 6 }), S('Key Scheme', 'U — double length TDES', { w: 3 }), S('LMK Size', 'Double', { w: 3 }), S('LMK Pair', '06-07 (ZPK)', { w: 3 }), S('Variant', '0', { w: 3 })], button: 'Encrypt', alt: 'Decrypt', result: [['Key under LMK', 'U + 32 hex · under LMK pair 06-07, variant 0'], ['KCV', KEY_KCV]] },
  'atalla-enc': { title: 'Atalla Keys Calculator', sub: 'Key Encryption', tabs: TABS_ATALLA, tab: 0, icon: 'vault', hint: 'header, encrypted key and MAC · comma separated', fields: [H('Key (Hex)', 32, { v: KEY, w: 6 }), H('AKB Header (Hex)', 16, { v: '3150554E45303030', w: 2, tag: '1PUNE000' }), H('MFK Key (Hex)', 32, { v: MFK, w: 4 })], button: 'Encrypt Key', result: [['AKB', AKB], ['KCV', KEY_KCV]] },
  'atalla-dec': { title: 'Atalla Keys Calculator', sub: 'AKB Decode', tabs: TABS_ATALLA, tab: 1, icon: 'lock-key-open', hint: 'verify against a known KCV rather than trusting the decode', fields: [A('AKB (Atalla Key Block)', AKB), C('Check KCV?', 'on', { w: 2 }), H('KCV (S)', 6, { v: KEY_KCV, w: 2, opt: true }), S('Parity', 'None', { w: 2 }), H('MFK Key (Hex)', 32, { v: MFK, w: 6 })], button: 'Decode AKB', result: [['Clear Key', grp(KEY)], ['KCV', 'matches ' + KEY_KCV + ' · parity not checked']] },
  safenet: { title: 'Safenet Keys Calculator', sub: 'Key Encryption', tabs: null, icon: 'shield-check', hint: 'legacy and modern Luna formats', fields: [H('Key (Hex)', 16, { v: KEY.slice(0, 16), w: 6 }), S('Key Format', 'Single length DES', { w: 3 }), S('Variant', 'DPK', { w: 3 }), S('Key Input Format', 'Hexadecimal', { w: 2 }), H('KM Key (Hex)', 32, { w: 4 })], button: 'Encrypt', alt: 'Decrypt', result: [['Key under KM', '16 hex · single DES under the KM key'], ['KCV', KCV.sn]] },
  'ssl-keys': { title: 'SSL Certificate (X.509) Utility', sub: 'Keys', tabs: TABS_SSL, tab: 0, icon: 'certificate', hint: 'PEM and parsed fields side by side · copy per artifact', fields: [S('Key Type', 'RSA', { w: 3 }), S('Key Size', '2048', { w: 3 }), A('Public Key (PEM)', '-----BEGIN PUBLIC KEY----- MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA…', { w: 3 }), A('Private Key (PEM)', '-----BEGIN PRIVATE KEY----- MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQ…', { w: 3 })], button: 'Generate Key Pair', alt: 'Read Keys', result: [['Key pair', 'RSA 2048 · e = 010001'], ['SHA-256', '32 bytes, colon separated · over the DER certificate']] },
  'rsa-der': { title: 'DER Public Key Encoder', sub: 'RSA DER', tabs: null, icon: 'brackets-curly', hint: 'a modulus above 0x7F needs a leading zero byte', fields: [H('Modulus', 512, { v: MOD, w: 4 }), S('Modulus Encoding', 'Hexadecimal', { w: 2 }), H('Exponent', 6, { v: '010001', w: 4 }), S('Exponent Encoding', 'Hexadecimal', { w: 2 }), C('Toggle Modulus Negative', 'on', { w: 6 })], button: 'Encode Key', result: [['DER', '30820122300D06092A864886F70D01010105000382010F003082010A0282010100' + MOD.slice(0, 24) + '…'], ['Length', '294 bytes · leading 00 added before the modulus']] },
};
Object.values(PANELS).forEach((spec) => { spec.app = 'key-tools'; spec.badge = 'KEYS'; });

const TOOLS: HubTool[] = [
  { id: 'dea', stage: 0, icon: 'key', name: 'DEA Keys (DES / 3DES Utility)', desc: 'A multi-tab tool focused on raw 3DES key material.' },
  { id: 'keyshare', stage: 0, icon: 'users-three', name: 'Keyshare Generator', desc: 'Splits a single key into n shares such that all n are required to reconstruct the key (XOR-based component scheme).' },
  { id: 'tr31', stage: 1, icon: 'package', name: 'TR-31 Key Block', desc: 'ASC X9.143 (formerly TR-31) defines a key block format that binds a key to its allowable usage, algorithm, mode, and exportability.' },
  { id: 'thales-block', stage: 1, icon: 'cube', name: 'Thales Key Block', desc: 'The Thales-specific key block format used by PayShield HSMs.' },
  { id: 'thales-keys', stage: 1, icon: 'calculator', name: 'Thales Key Calculator', desc: 'Vendor-aware calculations matching Thales PayShield host commands.' },
  { id: 'atalla', stage: 1, icon: 'vault', name: 'Atalla Key Calculator', desc: 'Atalla / Utimaco AKB-style key block helpers, including AKB header construction and KCV verification.' },
  { id: 'safenet', stage: 1, icon: 'shield-check', name: 'Safenet Key Calculator', desc: 'Safenet / Thales Luna key calculations for legacy and modern formats.' },
  { id: 'rsa-der', stage: 2, icon: 'brackets-curly', name: 'RSA DER Public Key Tool', desc: 'Wrap a modulus and exponent into DER, with the sign-byte case handled.' },
  { id: 'ssl', stage: 2, icon: 'certificate', name: 'SSL / X.509 Certificate Tool', desc: 'An end-to-end certificate workflow tool for terminal-host TLS.' },
];
// The app's hub also carries a Futurex calculator. It is not listed here until it has a
// reference section: advertising a tool the page does not document inflated the count
// to 10 and put a vendor in the metadata with zero prose behind it.
const HUB_TOOLS: HubTool[] = TOOLS;
const STAGES: HubStage[] = [['01', 'Build & share', 'DEA keys · shares · parity'], ['02', 'Key blocks & HSMs', 'TR-31 · Thales · Atalla · Safenet'], ['03', 'Certificates & DER', 'X.509 · CSR · DER']];
const FAMILIES: [string, string, string][] = [
  ['key', 'Build keys', 'Generate and combine raw key material.'],
  ['package', 'Wrap keys', 'Bind a key to its usage / algorithm via TR-31 or Thales key blocks.'],
  ['users-three', 'Distribute keys', 'Split into shares for multi-custodian loading and validate the resulting halves.'],
  ['seal-check', 'Compute key check values', 'For vendor HSMs and operational sign-off.'],
];

const SECTIONS: RawSection[] = [
  { id: 'dea', stage: 0, title: 'DEA Keys (DES / 3DES Utility)', rail: 'DEA Keys', intro: 'A multi-tab tool focused on raw 3DES key material.', blocks: [
    panel('dea-gen', 'DEA Keys Calculator on the Key Generator tab with keys-to-generate, 128-bit key length and odd key parity, beside the activity log'),
    { t: 'h3', x: 'Tabs' },
    { t: 'bullets', items: [['Key Generator', 'Generate cryptographically random DES, 2-key 3DES, or 3-key 3DES keys.'], ['Key Combination', 'XOR multiple key components together to reconstruct a key from shares.'], ['Parity Enforcement', 'Adjust the LSB of each byte so each byte has odd parity (DES requirement).'], ['Key Validation', 'Check parity, detect weak keys, and compute KCV (Key Check Value) using `00 00 00 00 00 00 00 00`.']] },
    { t: 'h3', x: 'Key Combination' },
    panel('dea-combine', 'Key Combination form with a TDES double-length key type and eight component fields, each with its own KCV box alongside'),
    { t: 'p', x: 'Components XOR together into the key. Pick the **Key Type / Length** — e.g. `TDES — Double length (16B / 32H)` — and the fields resize to match.' },
    { t: 'p', x: 'There is room for **eight** components, and each carries its own **KCV** box beside it, with one more for the combined result. That is what makes a bad component obvious: check each custodian’s KCV against their envelope before you trust the total.' },
    { t: 'h3', x: 'Parity Enforcement' },
    panel('dea-parity', 'Parity Enforcement form with a hex key field and an odd or even key parity selector above the Enforce Parity button'),
    { t: 'p', x: 'DES ignores the low bit of each byte, so specs use it as a parity bit. Paste a **Key (Hex)**, choose **Odd** or **Even**, and **Enforce Parity** adjusts each byte to match — the key value is unchanged as far as the cipher is concerned.' },
    { t: 'h3', x: 'Key Validation' },
    panel('dea-lookup', 'Key Lookup form with a hex key, a Check KCV checkbox, an optional KCV field and Any / Odd / Even parity radios above the Lookup Key button'),
    { t: 'p', x: 'The validation tab is a **Key Lookup**: give it a key and it reports what the key actually is.' },
    { t: 'bullets', items: ['**Key (Hex)**', ['Check KCV?', 'When ticked, the **KCV (Optional)** field is compared against the computed value instead of just reporting it.'], ['Parity', '`Any`, `Odd` or `Even`; the check fails if the key does not match the parity you assert.']] },
    { t: 'p', x: 'Button: **Lookup Key**.' },
  ] },
  { id: 'keyshare', stage: 0, title: 'Keyshare Generator', intro: 'Splits a single key into *n* shares such that all *n* are required to reconstruct the key (XOR-based component scheme). Useful for multi-custodian key loading.', blocks: [
    panel('keyshare', 'Keyshare Generator with global parity and key type options above a 2 Parts / 3 Parts tab pair, showing part fields with a combined key and its KCV'),
    { t: 'h3', x: 'Inputs' },
    { t: 'bullets', items: [['Global Options', '**Parity** (`Ignore` by default) and **Key Type** (`DES/TDES`), applied to the whole operation.'], ['2 Parts / 3 Parts', 'A tab pair rather than a count field; each tab shows exactly that many part fields.'], ['Part 1, Part 2 (and Part 3)', 'Leave them empty to have the tool generate them.']] },
    { t: 'p', x: 'Button: **Generate 2 Parts** (or 3). The **Combined Key** and its **KCV** appear together at the bottom, so the reconstructed value can be checked before it leaves the screen.' },
    { t: 'h3', x: 'Output' },
    { t: 'bullets', items: ['Random components for shares 1 to *n*−1.', 'Final component computed so XOR of all components = the master key.', 'KCV of each share for safe transport verification.'] },
    { t: 'note', tone: 'warn', icon: 'warning', title: 'Custody', x: 'Components must be transported and stored separately under the control of different custodians. Recombining shares brings them under the control of a single trustee, so do this only inside the HSM during loading.' },
  ] },
  { id: 'tr31', stage: 1, title: 'TR-31 Key Block', intro: 'ASC X9.143 (formerly TR-31) defines a key block format that binds a key to its allowable usage, algorithm, mode, and exportability. A TR-31 block is opaque to anything outside the issuing HSM but lets two HSMs exchange keys without losing metadata.', blocks: [
    panel('tr31', 'TR-31 Key block on the Wrap tab with KBPK, clear key, version id, key usage, algorithm, mode of use, key version and exportability fields, beside the activity log'),
    { t: 'h3', x: 'Tabs' },
    { t: 'bullets', items: [['Wrap', 'Build a key block from a clear key under a Key Block Protection Key (KBPK).'], ['Unwrap', 'Decode a key block, validate its MAC, and reveal the contents.']] },
    { t: 'h3', x: 'Inputs (Wrap)' },
    { t: 'table', head: ['Field', 'Description'], rows: [['**KBPK**', '32 / 48 hex chars (3DES) or 64 hex (AES).'], ['**Clear Key**', 'Key to wrap.'], ['**Key Usage**', 'Two-character code (e.g. `P0` = PIN encryption, `M0` = MAC, `K0` = Key Encryption Key).'], ['**Algorithm**', '`D` = DES, `T` = TDES, `A` = AES, etc.'], ['**Mode of Use**', '`E` = encrypt, `D` = decrypt, `B` = both, `N` = no restriction.'], ['**Key Version Number**', 'Two characters.'], ['**Exportability**', '`E` = exportable, `S` = sensitive (no clear export), `N` = no export.']] },
    { t: 'h3', x: 'Output' },
    { t: 'p', x: 'An ASCII key block string starting with the version (`A`, `B`, `C`, `D`) plus encrypted key, MAC, and optional blocks.' },
    { t: 'code', lines: ['A0072P0TE00E0000ABC...   (D variant TR-31 block)'] },
  ] },
  { id: 'thales-block', stage: 1, title: 'Thales Key Block', intro: 'The Thales-specific key block format used by PayShield HSMs. Similar in concept to TR-31 but with Thales’ own header and key usage codes.', blocks: [
    panel('thales-kb', 'Thales Key Block on the Encode tab with a 3DES / AES KBPK version selector, key block protection key with KCV, clear key, and the key block header attributes'),
    { t: 'h3', x: 'Workflow' },
    { t: 'steps', items: ['Pick a **Thales Key Type** — ZMK, ZPK, TMK, BDK, ZEK, etc.', 'Provide the **LMK Variant** applicable to that key type.', 'Provide the **clear key** material.', 'The tool returns the encrypted key under LMK along with its KCV.'] },
    { t: 'note', tone: 'teal', icon: 'lightbulb', title: 'Tip', x: 'Use this when you have a clear key and want to import it under the HSM Simulator’s LMK without typing it through the console.' },
  ] },
  { id: 'thales-keys', stage: 1, title: 'Thales Key Calculator', intro: 'Vendor-aware calculations matching Thales PayShield host commands.', blocks: [
    panel('thales-keys', 'Thales Keys Encryption / Decoding with a hex key, key scheme, double or triple LMK size, an LMK pair selector and a variant, above Encrypt and Decrypt buttons'),
    { t: 'h3', x: 'Operations' },
    { t: 'bullets', items: [['Key Generation', 'Equivalent to `A0` (Generate a Key) host command.'], ['Key Translation', 'Equivalent to `A6` (Translate a Key from One ZMK to Another).'], ['KCV Computation', 'Match Thales-style 6-digit KCVs for operational sign-off.'], ['Variant Application', 'Apply LMK and ZMK variants used during key wrapping.']] },
  ] },
  { id: 'atalla', stage: 1, title: 'Atalla Key Calculator', intro: 'Atalla / Utimaco AKB-style key block helpers, in both directions: build a key block from a clear key, or take one apart.', blocks: [
    panel('atalla-enc', 'Atalla Keys Calculator on the Key Encryption tab with hex key, AKB header and MFK key fields above the Encrypt Key button'),
    { t: 'h3', x: 'Key Encryption' },
    { t: 'bullets', items: [['Key (Hex)', 'The clear key to wrap.'], ['AKB Header (Hex)', 'The attribute header that travels inside the block.'], ['MFK Key (Hex)', 'The Master File Key the block is encrypted under.']] },
    { t: 'p', x: 'Button: **Encrypt Key**.' },
    { t: 'h3', x: 'AKB Decode' },
    panel('atalla-dec', 'AKB Decode form with an Atalla Key Block field, a Check KCV checkbox, an optional KCV, a parity selector and an MFK key, above the Decode AKB button'),
    { t: 'bullets', items: [['AKB (Atalla Key Block)', 'The block to open.'], ['Check KCV? and KCV (S)', 'Verify the recovered key against a known check value rather than trusting the decode.'], ['Parity', '`None` by default.'], '**MFK Key (Hex)**'] },
    { t: 'p', x: 'Button: **Decode AKB**.' },
  ] },
  { id: 'safenet', stage: 1, title: 'Safenet Key Calculator', intro: 'Safenet / Thales Luna key calculations for legacy and modern formats.', blocks: [
    panel('safenet', 'Safenet Keys Calculator with a hex key, a key format of single length DES, a DPK variant, hexadecimal key input format and a KM key, above Encrypt and Decrypt buttons'),
  ] },
  { id: 'ssl', stage: 2, title: 'SSL / X.509 Certificate Tool', rail: 'SSL / X.509', intro: 'An end-to-end certificate workflow tool for terminal-host TLS.', blocks: [
    panel('ssl-keys', 'SSL Certificate (X.509) Utility on the Keys tab with an RSA key type, 2048-bit length and public and private key fields, beside the activity log'),
    { t: 'h3', x: 'Tabs' },
    { t: 'bullets', items: [['Keys', 'Generate RSA key pairs (2048 / 3072 / 4096-bit) or read existing keys.'], ['CSRs', 'Build a Certificate Signing Request from a key and DN parameters (CN, OU, O, L, S, C).'], ['Read CSR', 'Parse and display the contents of an existing CSR.'], ['Self-Signed', 'Issue a self-signed certificate from a key + DN, with configurable validity.'], ['Read Certificate', 'Parse and display an X.509 certificate, including extensions.']] },
    { t: 'h3', x: 'Inputs' },
    { t: 'bullets', items: ['**Common Name (CN)**', '**Organisation (O), Org. Unit (OU)**', '**Locality (L), State (S), Country (C)**', '**Validity (Days)**', ['Key Size', '2048 / 3072 / 4096.']], cols: 2 },
    { t: 'p', x: 'Outputs are displayed as PEM and as parsed fields side-by-side, with a copy button per artifact.' },
  ] },
  { id: 'rsa-der', stage: 2, title: 'RSA DER Public Key Tool', rail: 'RSA DER', intro: 'An RSA public key is a modulus and an exponent, but what a host expects on the wire is those two numbers wrapped in DER. This encoder does that wrapping, and is the tool to reach for when a certificate library rejects a key you know is correct.', blocks: [
    panel('rsa-der', 'DER Public Key Encoder with modulus and exponent fields, their own encoding selectors and a toggle modulus negative checkbox above the Encode Key button'),
    { t: 'h3', x: 'Inputs' },
    { t: 'bullets', items: [['Modulus', 'with its own **Modulus Encoding** selector.'], ['Exponent', 'with an **Exponent Encoding** selector — commonly `03` or `010001`.'], ['Toggle Modulus Negative', 'DER reads the leading bit as a sign, so a modulus starting above `0x7F` needs a leading zero byte. This is the switch for that case.']] },
    { t: 'p', x: 'Button: **Encode Key**.' },
  ] },
];

const TIPS: string[] = [
  'Always verify the KCV after combining shares or unwrapping a key block. A wrong KCV almost always means a typo in one share.',
  'For TR-31, watch out for case sensitivity in the header — the key usage and mode codes are uppercase.',
  'If your HSM rejects an imported key, compare the version byte (`A` vs `B` vs `C` vs `D`) — older HSMs may only accept specific versions.',
  'For SSL, generate the key first, then the CSR, then the cert — the tool will pre-fill DN parameters from a previous CSR if you stay on the same session.',
];


export const KEY_TOOLS_GUIDE: ToolGuideData = {
  slug: 'key-tools',
  crumb: 'Key Management Tools',
  meta: '9 tools · key blocks · KCVs · X.509',
  title: 'Key Management Tools',
  lede: 'Generate, validate, wrap, share, and verify cryptographic keys used across payment systems — from raw 3DES key generation and parity enforcement to TR-31 / Thales key blocks, vendor HSM-specific calculators, keyshare splitting, and X.509 certificate workflows.',
  hub: {
    title: 'Key Management Tools',
    sub: 'Generate, wrap, share and verify payment keys',
    tools: HUB_TOOLS,
    stages: STAGES,
    columns: 4,
    category: 3,
    search: 'Search key tools…',
    badge: '9 tools',
    label: 'Build, wrap, distribute, check',
    note: 'A KCV beside every key that leaves the screen.',
    aria: 'The Key Management hub: tool categories, three job groups and the ten tools in the group',
  },
  panels: PANELS,
  panelBadge: 'KEYS',
  captionFrom: 'title',
  intro: {
    eyebrow: 'key-tools · Key Management hub — above',
    paras: ['**Key Management Tools** cluster around four jobs:'],
    features: toFeatures(FAMILIES),
    after: ['SSL / X.509 certificate handling is also grouped here for projects that need terminal or host-to-host TLS.'],
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
    heading: 'Try it on your own keys',
    text: 'Free and open source. Download the studio and run these calculators on your desk in minutes.',
  },
};
