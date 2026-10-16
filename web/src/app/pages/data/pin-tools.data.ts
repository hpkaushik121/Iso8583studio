/**
 * Payment Utilities (PIN tools) — page content and the specs its glass panels render from.
 * Ported from the prototype's pin-tools-data.js; imported only by
 * pages/site/docs-pin-tools.ts.
 *
 * Every panel output that can be computed is a real value for the inputs shown (verified with
 * openssl against the app's algorithms); outputs that depend on random padding or vendor secrets
 * describe the result's layout instead of inventing hex. Field defaults still come from hx().
 */
import { GlassSpec, H, HubStage, HubTool, S, T, grp, hx } from '../shared/glass/glass-model';
import { ToolBlock, ToolGuideData, panel, toFeatures } from '../shared/glass/tool-guide';

interface RawCard {
  id: string;
  /** Where the screen lives in the app: "Tool · Encode & Decode tabs". */
  where: string;
  icon: string;
  name: string;
  desc: string;
}

interface RawSection {
  id: string;
  where: string;
  icon: string;
  title: string;
  rail?: string;
  intro?: string;
  blocks: ToolBlock[];
}

// Luhn-valid PAN whose rightmost 12 digits (check digit excluded) are 123456789012 — the doc's worked ISO-0 example
const PAN = '4001 2345 6789 0120';
const ISO0 = '041226CBA9876FED';
const dg = (seed: string, n: number): string => { const h = hx(seed, n * 2); let s = ''; for (let i = 0; i < n; i++) s += String(parseInt(h.slice(i * 2, i * 2 + 2), 16) % 10); return s; };
// ANSI X9.24-1 sample BDK / KSN, the IPEK they derive to, and the PEK (future key XOR the PIN variant 00…FF00…FF) at counter 1; the X9.24-3 sample BDK / KSN and IK; RFC 4231 HMAC test case 2
const BDK = '0123456789ABCDEFFEDCBA9876543210', KSN = 'FFFF9876543210E00001', IPEK = '6AC292FAA1315B4D858AB3A3D7D5933A', PEK = '042666B49184CF5C68DE9628D0397B36';
const ABDK = 'FEDCBA9876543210F1F1F1F1F1F1F1F1', AKSN = '123456789012345600000001', AIK = '1273671EA26AC29AFA4D1084127652A1';
const HMAC = '5BDCC146BF60754E6A042426089575C75A003F089D2739839DEC58B964EC3843';
const AMEX = '3714 4963 5398 431', MC = '5500 0000 0000 0004', TRACK = '5500000000000004D25121011234567890F';
const CSC5 = dg('csc5', 5), CSC4 = dg('csc4', 4), CSC3 = dg('csc3', 3), CVC3 = dg('cvc3', 3);
const TABS_PB = ['Encode', 'Decode'];
const TABS_AS = ['Generate Terminal Key Set', 'Translate PIN Block', 'MAC', 'OWF'];
const TABS_DK = ['PEK Derivation', 'DEK Derivation', 'DUKPT PIN', 'DUKPT MAC', 'DUKPT Data'];
const TABS_DA = ['Key Derivation', 'DUKPT PIN', 'DUKPT MAC', 'DUKPT Data'];
const TABS_GV = ['Generate', 'Validate'];
const app = 'pin-tools';

const PANELS: Record<string, GlassSpec> = {
  'pin-encode': { app, badge: 'PIN', title: 'PIN Block Calculator', sub: 'Encode · Format 0 (ISO-0)', tabs: TABS_PB, tab: 0, icon: 'arrow-right', hint: 'PAN-bound format · rightmost 12 digits, check digit excluded', fields: [S('PIN block format', 'Format 0 (ISO-0)', { w: 6 }), T('PAN', PAN, { w: 3 }), T('PIN', '1234', { w: 3 })], button: 'Encode', result: [['PIN field', '0412 34FF FFFF FFFF'], ['PAN block', '0000 1234 5678 9012'], ['PIN block', grp(ISO0) + ' · ISO-0']] },
  'pin-decode': { app, badge: 'PIN', title: 'PIN Block Calculator', sub: 'Decode · Format 0 (ISO-0)', tabs: TABS_PB, tab: 1, icon: 'arrow-right', hint: 'same format both ways · the PIN recovered from the block', fields: [S('PIN block format', 'Format 0 (ISO-0)', { w: 6 }), T('PAN', PAN, { w: 3 }), H('PIN Block', 16, { v: ISO0, w: 3 })], button: 'Decode', result: [['PIN', '1234'], ['Check', 'length nibble 4 · fill FF · PAN block matches']] },
  'aes-pin': { app, badge: 'PIN', title: 'PIN Block (AES) Calculator', sub: 'AES PIN Block Operations', tabs: null, icon: 'lock', hint: 'ISO 9564-1 Format 4 · AES-128 · PAN block XOR', fields: [H('Key (32 Hex Chars)', 32, { v: 'D30D421EC808407A61972EDBA36482FF', w: 6 }), T('PIN (Encode) / PIN Block (Decode)', '1234', { w: 3 }), T('PAN', PAN, { w: 3 })], button: 'Encode', alt: 'Decode', result: [['Clear PIN block', grp('441234AAAAAAAAAA7BFDA91F39F63284')], ['PAN block', grp('44001234567890120000000000000000')], ['Intermediate', grp('5EA985DAD0358E6BC29639CB1D5395D3') + ' · AES(K, PIN block)'], ['PIN block', grp('21969613F4EB77C208147888EB674457') + ' · AES(K, intermediate ⊕ PAN block)']] },
  translate: { app, badge: 'AS2805', title: 'AS2805 Calculator', sub: 'Translate PIN Block', tabs: TABS_AS, tab: 1, icon: 'swap', hint: 'TPK → ZPK · the clear PIN never leaves the operation', fields: [H('System ZPK (Hex)', 32, { v: '21FF8CDA1F622AAF4D324C6C99297E2E', w: 3 }), H('Terminal TPK (Hex)', 32, { v: 'D63AEDC8D1F871C794E8DF811BC3CA8C', w: 3 }), T('STAN', '000123', { w: 3 }), T('Transaction Amount', '000000010000', { w: 3 }), S('Incoming PIN Block Format', '01', { w: 3 }), S('Outgoing PIN Block Format', '01', { w: 3 }), H('Incoming PIN Block (Hex)', 16, { v: '5D431FE36DD18C29', w: 3 }), T('Account Number', PAN, { w: 3 })], button: 'Translate', result: [['Outgoing PIN block', grp('D2BE84619EF6A10F')], ['Translation', 'TPK → ZPK · format 01 → 01 · clear block 0412 26CB A987 6FED']] },
  'dukpt-pin': { app, badge: 'DUKPT', title: 'DUKPT Utilities', sub: 'DUKPT PIN', tabs: TABS_DK, tab: 2, icon: 'lock', hint: 'PEK from the PEK Derivation tab · BDK or IPEK + KSN', fields: [H('PEK (32 Hex Chars)', 32, { v: PEK, w: 6 }), H('PIN Block (16 Hex Chars)', 16, { v: ISO0, w: 6 })], button: 'Encrypt', alt: 'Decrypt', result: [['Encrypted PIN block', grp('7B088E2BD17127FD')], ['Key', 'PEK from KSN ' + KSN + ' · 3DES']] },
  // AS2805 Calculator — the three tabs beside Translate PIN Block
  'as-keys': { app, badge: 'AS2805', title: 'AS2805 Calculator', sub: 'Generate Terminal Key Set', tabs: TABS_AS, tab: 0, icon: 'key', hint: 'TPK and TAK under the KEKr · key check values alongside', fields: [S('Key Flag', '1', { w: 2 }), H('KEKr Key (Hex)', 32, { w: 4 }), S('Key Scheme KEKE', 'B', { w: 2 }), S('Key Scheme LMK', 'Z', { w: 2 }), S('Key Check Value Type', '1', { w: 2 })], button: 'Generate Keys', result: [['TPK', grp('A7AB893E9D4F8FD507898F7C7951C4C2')], ['TAK', grp('4A8C8CA851B3A4AE75F440464ADC510B')], ['KCV', 'TPK 5C418C · TAK 40CC56']] },
  'as-mac': { app, badge: 'AS2805', title: 'AS2805 Calculator', sub: 'MAC', tabs: TABS_AS, tab: 2, icon: 'shield-check', hint: 'AS 2805.4 MAC · 3DES · both fields required', fields: [H('Key (Hex)', 32, { v: '4D64AA87925831C1BF2109ED08B44845', w: 3 }), H('Data (Hex)', 32, { v: '8C9ED6C8616BF534B77D339BE0C2CB5C', w: 3 })], button: 'Calculate MAC', result: [['MAC', grp('2BDEEC7BF0FCA759')], ['Method', 'ISO 9797-1 algorithm 1 · 3DES CBC · zero IV · last block']] },
  'as-owf': { app, badge: 'AS2805', title: 'AS2805 Calculator', sub: 'OWF', tabs: TABS_AS, tab: 3, icon: 'hash', hint: 'one-way function · key and data in, digest out', fields: [H('Key (Hex)', 32, { v: '4D64AA87925831C1BF2109ED08B44845', w: 3 }), H('Data (Hex)', 16, { v: 'E6882238A196768E', w: 3 })], button: 'Calculate OWF', result: [['OWF', grp('2613753DBE88DD57')], ['Method', 'E_K(D) ⊕ D · 3DES']] },
  // DUKPT Utilities — the derivation and operation tabs around DUKPT PIN, and the AES tool's Key Derivation
  'dukpt-pek': { app, badge: 'DUKPT', title: 'DUKPT Utilities', sub: 'PEK Derivation', tabs: TABS_DK, tab: 0, icon: 'arrow-right', hint: 'BDK or IPEK + KSN · ANSI X9.24-1 · 3DES', fields: [S('Input Key Designation', 'BDK', { w: 6 }), H('BDK (32 Hex Chars)', 32, { v: BDK, w: 3 }), H('KSN (20 Hex Chars)', 20, { v: KSN, w: 3 })], button: 'Derive PEK', result: [['IPEK', grp(IPEK)], ['PEK', grp(PEK)], ['Counter', 'KSN counter 00001 · PIN variant applied']] },
  'dukpt-dek': { app, badge: 'DUKPT', title: 'DUKPT Utilities', sub: 'DEK Derivation', tabs: TABS_DK, tab: 1, icon: 'arrow-right', hint: 'same inputs as PEK Derivation · data variant instead', fields: [S('Input Key Designation', 'BDK', { w: 6 }), H('BDK (32 Hex Chars)', 32, { v: BDK, w: 3 }), H('KSN (20 Hex Chars)', 20, { v: KSN, w: 3 })], button: 'Derive DEK', result: [['IPEK', grp(IPEK)], ['DEK', grp('448D3F076D8304036A55A3D7E0055A78')], ['Counter', 'KSN counter 00001 · data variant applied']] },
  'dukpt-mac': { app, badge: 'DUKPT', title: 'DUKPT Utilities', sub: 'DUKPT MAC', tabs: TABS_DK, tab: 3, icon: 'shield-check', hint: 'PEK-derived MAC key · DES or 3DES', fields: [H('PEK (32 Hex Chars)', 32, { v: PEK, w: 6 }), S('Algorithm', '3DES', { tag: 'radio', w: 2 }), H('Data (Hex)', 32, { v: '8C9ED6C8616BF534B77D339BE0C2CB5C', w: 4 })], button: 'Generate MAC', result: [['MAC', grp('6E606C403D2911A4')], ['Key', 'input key ⊕ MAC variant 00…FF00 · ISO 9797-1 algorithm 3']] },
  'dukpt-data': { app, badge: 'DUKPT', title: 'DUKPT Utilities', sub: 'DUKPT Data', tabs: TABS_DK, tab: 4, icon: 'lock', hint: 'CBC or ECB · zero IV · data variant optional', fields: [H('PEK (32 Hex Chars)', 32, { v: PEK, w: 6 }), S('Use Data Variant Key', 'Off', { tag: 'switch', w: 2 }), S('Data Input Type', 'ASCII', { w: 2 }), S('Cipher Mode (zero IV)', 'CBC', { tag: 'radio', w: 2 }), T('Data', 'Hello, World!', { w: 6 })], button: 'Encrypt', alt: 'Decrypt', result: [['Ciphertext', grp('147B5C117701541B4763707E3FA0F3EE')], ['Mode', 'CBC · zero IV · PKCS#5 · 2 blocks']] },
  'dukpt-aes': { app, badge: 'DUKPT AES', title: 'DUKPT AES Utilities', sub: 'Key Derivation', tabs: TABS_DA, tab: 0, icon: 'arrow-right', hint: 'ANSI X9.24-3 · BDK or IK · AES or TDES working keys', fields: [S('Input Key Designation', 'BDK', { w: 3 }), S('Initial Key Type', 'AES-128', { w: 3 }), H('BDK / IK', 32, { v: ABDK, w: 3 }), S('Working Key Type', 'AES-128', { w: 3 }), H('KSN', 24, { v: AKSN, w: 6 })], button: 'Derive Keys', result: [['IK', grp(AIK)], ['Working key', grp('BFAD76FCA1119BEC21A8B79E3481490F')], ['Usage', 'PIN encryption · AES-128 · counter 00000001']] },
  // Card verification — Amex CSC and MasterCard CVC3, Generate and Validate tabs
  'csc-gen': { app, badge: 'AMEX', title: 'Amex CSC Calculator', sub: 'Generate CSC', tabs: TABS_GV, tab: 0, icon: 'sparkle', hint: 'CSC-5, CSC-4 and CSC-3 from one key · every value logged', fields: [S('CSC Version', 'Version 1', { w: 3 }), H('CSC Key (Hex)', 32, { w: 3 }), T('PAN', AMEX, { w: 3 }), T('Expiration Date (YYMM)', '2812', { w: 3 }), T('Service Code', '101', { w: 3 }), S('Verification Value Type', 'CSC', { w: 3 })], button: 'Generate', result: [['CSC-5', '5 digits'], ['CSC-4', '4 digits'], ['CSC-3', '3 digits']] },
  'csc-val': { app, badge: 'AMEX', title: 'Amex CSC Calculator', sub: 'Validate CSC', tabs: TABS_GV, tab: 1, icon: 'check', hint: 'the three values checked in one pass', fields: [T('PAN', AMEX, { w: 3 }), T('Expiration Date (YYMM)', '2812', { w: 3 }), T('Service Code', '101', { w: 3 }), S('Verification Value Type', 'CSC', { w: 3 }), T('CSC-5', CSC5, { w: 2 }), T('CSC-4', CSC4, { w: 2 }), T('CSC-3', CSC3, { w: 2 })], button: 'Validate', result: [['Result', 'CSC-5 ✓ · CSC-4 ✓ · CSC-3 ✓'], ['Check', 'Version 1 · PAN ' + AMEX.replace(/\s/g, '') + ' · exp 2812']] },
  'cvc3-gen': { app, badge: 'MC', title: 'MasterCard CVC3', sub: 'Generate CVC3', tabs: TABS_GV, tab: 0, icon: 'arrow-right', hint: 'IMK → KDCVC3 → IVCVC3 → CVC3 · each step logged', fields: [H('IMK', 32, { v: '10A38A3C2F12188A19C4C7FEC19D1172', w: 6 }), T('PAN', MC, { w: 3 }), T('PAN Seq No', '01', { w: 3 }), T('Track 1/2 Data', TRACK, { w: 6 }), H('Unpredictable Num', 8, { v: '00000899', w: 2 }), H('ATC', 4, { v: '005E', w: 2 }), S('CVC3 Type', 'Dynamic CVC3', { w: 2 })], button: 'Generate', result: [['KDCVC3', grp('C16B23F1E9FD25EF049B5867DAFB866B') + ' · from the IMK, PAN and PSN'], ['IVCVC3', '4 hex · last two bytes of the MAC over the track data'], ['CVC3', '3 digits · decimalised']] },
  'cvc3-val': { app, badge: 'MC', title: 'MasterCard CVC3', sub: 'Validate CVC3', tabs: TABS_GV, tab: 1, icon: 'check', hint: 'recomputes the CVC3 from the same inputs and compares', fields: [H('IMK', 32, { v: '10A38A3C2F12188A19C4C7FEC19D1172', w: 6 }), T('PAN', MC, { w: 3 }), T('PAN Seq No', '01', { w: 3 }), T('Track 1/2 Data', TRACK, { w: 6 }), H('Unpredictable Num', 8, { v: '00000899', w: 2 }), H('ATC', 4, { v: '005E', w: 2 }), T('Dynamic CVC3', CVC3, { w: 2 }), S('CVC3 Type', 'Dynamic CVC3', { w: 6 })], button: 'Validate', result: [['Result', 'CVC3 valid · ' + CVC3 + ' = ' + CVC3], ['Check', 'ATC 005E · UN 00000899 · PSN 01']] },
  // MAC calculators — the four with screens in the group
  'mac-9797': { app, badge: 'MAC', title: 'ISO/IEC 9797-1 MAC', sub: 'Generate a Message Authentication Code', tabs: null, icon: 'shield-check', hint: 'Algorithm 1–6 · padding Method 1–3 · truncated to length', fields: [S('MAC Algorithm', 'Algorithm 1', { w: 3 }), H("Key (K')", 32, { v: BDK, w: 3 }), S('Padding', 'Method 2', { w: 2 }), H('Data (Hex)', 10, { v: '48656C6C6F', w: 2 }), T('Truncation Length (Chars)', '8', { w: 2 })], button: 'Generate MAC', result: [['Padded', '4865 6C6C 6F80 0000'], ['MAC', '8F9E 4CCD D623 E590'], ['Truncated', '8F9E4CCD · 8 chars']] },
  'mac-ansi': { app, badge: 'MAC', title: 'ANSI X9.9 & X9.19', sub: 'ANSI MAC Generation', tabs: null, icon: 'shield-check', hint: 'X9.9 wholesale or X9.19 retail · 8-char truncation', fields: [S('MAC Algorithm', 'ANSI MAC X9.9 (Wholesale MAC)', { w: 6 }), H('Key (K)', 16, { v: 'B3B9A0C43FB377DB', w: 2 }), H('Data (Hex)', 32, { v: '8C9ED6C8616BF534B77D339BE0C2CB5C', w: 2 }), T('Truncation Length (Chars)', '8', { w: 2 })], button: 'Generate MAC', result: [['MAC', grp('FFAC1C92DCB1D80D')], ['Truncated', 'FFAC1C92 · X9.9 · single DES CBC']] },
  'mac-tdes': { app, badge: 'MAC', title: 'TDES-CBC MAC Calculator', sub: 'TDES CBC-MAC Generation', tabs: null, icon: 'shield-check', hint: 'fixed algorithm · 2-key 3DES · ISO 9797-1 padding', fields: [S('MAC Algorithm', 'TDES CBC-MAC', { w: 3 }), H('Key (K) - 32 Hex Chars', 32, { v: '8A6B8B307A8916476E11B3D544287558', w: 3 }), S('Padding', 'ISO9797-1 (Padding Method 1)', { w: 3 }), H('Data (Hex)', 32, { v: '8C9ED6C8616BF534B77D339BE0C2CB5C', w: 2 }), T('Truncation Length (Chars)', '8', { w: 1 })], button: 'Generate MAC', result: [['MAC', grp('E0B0B747E8994118')], ['Truncated', 'E0B0B747 · 8 chars']] },
  'mac-hmac': { app, badge: 'MAC', title: 'HMAC MAC Calculator', sub: 'HMAC Generation', tabs: null, icon: 'shield-check', hint: 'MD5 · SHA-1 · SHA-2 · RIPEMD-160 · ASCII or hex inputs', fields: [S('Hash Type', 'SHA-256', { w: 3 }), S('Key Input', 'Hexadecimal', { w: 3 }), H('HMAC Key', 8, { v: '4A656665', w: 2 }), S('Data Input', 'ASCII', { w: 1 }), T('Data', 'what do ya want for nothing?', { w: 3 })], button: 'Generate HMAC', result: [['HMAC-SHA256', grp(HMAC)], ['Length', '32 bytes · RFC 4231 test case 2']] },
  // Bitmap Calculator — the field grids light up under the form (GlassPanel `grid`)
  bitmap: { app, badge: 'ISO 8583', title: 'Bitmap Calculator', sub: 'Calculated Bitmap', tabs: null, icon: 'grid-four', hint: 'toggle a field and the bitmap recalculates · copy it out', fields: [T('Fields present', '1 2 3 10 11 12 20 21 35 37 53 55 · 66 67 128', { w: 6 })], button: 'Calculate bitmap', alt: 'Copy', result: [['Calculated Bitmap', 'E0701800 28000A00 60000000 00000001'], ['Primary', 'E0701800 28000A00 · field 1 set, secondary follows'], ['Secondary', '60000000 00000001 · 66 67 128']], grid: [1, 2, 3, 10, 11, 12, 20, 21, 35, 37, 53, 55, 66, 67, 128] },
};

// The whole Payment Utilities group as the studio lists it (21 tools), in five families — what the hero hub shows.
const HUB_STAGES: HubStage[] = [['01', 'PIN blocks', 'ISO 9564 · AES · offset · PVV · ZKA'], ['02', 'Card verification', 'CVV · CSC · CVC3'], ['03', 'MAC & hash', '9797-1 · X9.9 · HMAC · CMAC · MDC'], ['04', 'DUKPT', 'ISO 9797 · AES'], ['05', 'ISO 8583', 'AS2805 · bitmap · parser']];
const HUB: HubTool[] = [
  { id: 'pin-block-general', stage: 0, icon: 'keyboard', name: 'PIN Block Calculator', desc: 'Encoding and Decoding PIN Blocks' },
  { id: 'pin-block-aes', stage: 0, icon: 'dots-nine', name: 'PIN Block(AES) Calculator', desc: 'Encoding and Decoding PIN Blocks using AES' },
  { id: 'pin-offset', stage: 0, icon: 'calculator', name: 'PIN Offset (IBM 3624)', desc: 'Generating and recovering PINs using the IBM 3624 PIN Offset method' },
  { id: 'pin-pvv', stage: 0, icon: 'fingerprint', name: 'PIN PVV Calculator', desc: 'PIN PVV (PIN Verification Value) Calculator' },
  { id: 'zka', stage: 0, icon: 'lock-key', name: 'ZKA', desc: 'ZKA (Zone Key Administration) operations' },
  { id: 'cvv', stage: 1, icon: 'password', name: 'CVV Calculator', desc: 'Card Verification Value' },
  { id: 'amex-csc', stage: 1, icon: 'credit-card', name: 'AMEX CSC', desc: 'American Express CSC' },
  { id: 'mc-cvc', stage: 1, icon: 'cards', name: 'MasterCard CVC', desc: 'Master Card CVC3' },
  { id: 'iso9797', stage: 2, icon: 'shield', name: 'ISO/IEC 9797-1', desc: 'MACs according to the ISO/IEC 9797-1 standard' },
  { id: 'ansi-mac', stage: 2, icon: 'shield-check', name: 'ANSI X9.9 & X9.19', desc: 'MACs according to the ISO/IEC 9797-1 standard' },
  { id: 'as2805-mac', stage: 2, icon: 'seal-check', name: 'AS2805.4.1 MAC Calculator', desc: 'MACs according to the AS2805.4.1 standard' },
  { id: 'tdes-cbc-mac', stage: 2, icon: 'lock', name: 'TDES-CBC MAC Calculator', desc: 'MACs according to the TDES-CBC standard' },
  { id: 'hmac', stage: 2, icon: 'key', name: 'HMAC MAC Calculator', desc: 'MACs according to the HMAC standard' },
  { id: 'cmac', stage: 2, icon: 'lock-simple', name: 'CMAC MAC Calculator', desc: 'MACs according to the CMAC standard' },
  { id: 'retail-mac', stage: 2, icon: 'shopping-cart', name: 'Retail MAC Calculator', desc: 'MACs according to the Retail standard' },
  { id: 'mdc-hash', stage: 2, icon: 'hash', name: 'MDC Hash Calculator', desc: 'MDC (Modification Detection Code) Hash Calculator' },
  { id: 'dukpt-9797', stage: 3, icon: 'key', name: 'DUKPT ISO 9797', desc: 'Derived unique key per transaction (ISO 9797)' },
  { id: 'dukpt-aes', stage: 3, icon: 'key', name: 'DUKPT ISO AES', desc: 'Derived unique key per transaction (AES)' },
  { id: 'as2805', stage: 4, icon: 'map-pin', name: 'AS2805 Calculator', desc: 'Australian payment standard' },
  { id: 'bitmap', stage: 4, icon: 'grid-four', name: 'Bitmap Calculator', desc: 'ISO8583 bitmap utilities' },
  { id: 'parser', stage: 4, icon: 'brackets-curly', name: 'Message Parser', desc: 'ISO8583 message parser' },
];

const INTRO: string[] = [
  'A PIN block is a fixed-format encoding of a cardholder PIN designed to be encrypted under a key (TPK, ZPK, or DUKPT-derived) and transmitted across a payment network. ISO 9564 defines the canonical formats; vendors and legacy networks add a few non-standard variants you may still encounter.',
  'The PIN calculators live in `Tools → Payment Utilities`, alongside the card-verification, DUKPT and MAC tools. Two of them are dedicated PIN tools — **PIN Block Calculator** and **PIN Block (AES)** — and two more tools in the group carry PIN block operations of their own, covered in [PIN Block Translation](/tools/pin-tools#translate) and [DUKPT PIN](/tools/pin-tools#dukpt-pin) below.',
  'The rest of the group follows, one screen at a time: the other three [AS2805 Calculator](/tools/pin-tools#as2805) tabs, the [DUKPT Utilities](/tools/pin-tools#dukpt) derivation and operation tabs and their AES counterpart, the [AMEX CSC and MasterCard CVC3](/tools/pin-tools#card-verification) calculators, the four [MAC calculators](/tools/pin-tools#mac) and the [Bitmap Calculator](/tools/pin-tools#bitmap).',
];
const FAMILIES: [string, string, string][] = [
  ['keyboard', 'PIN Block Calculator', 'Encode or decode a PIN block in any ISO 9564 or OEM format — the format picked once, Encode and Decode as a tab pair.'],
  ['dots-nine', 'PIN Block (AES)', 'ISO Format 4 encode and decode under an AES-128 key, with every intermediate value logged.'],
  ['swap', 'AS2805 Calculator · Translate PIN Block', 'Re-encrypt a block from the terminal key to the zone key, changing its format on the way.'],
  ['key', 'DUKPT Utilities · DUKPT PIN', 'Encrypt or decrypt a PIN block under a PIN Entry Key derived per transaction.'],
];

// The doc's own map of the page — each card links to its section.
const TOOLS: RawCard[] = [
  { id: 'formats', where: 'Reference · ISO 9564-1', icon: 'table', name: 'PIN Block Formats', desc: 'Reference of ISO 9564 formats 0–4 plus OEM variants — which are PAN-bound, how padding works, and when each is used.' },
  { id: 'pin-block', where: 'Tool · Encode & Decode tabs', icon: 'keyboard', name: 'PIN Block Calculator', desc: 'Builds a formatted PIN block, or recovers the PIN from one — the format picked once from a drop-down, Encode and Decode as a tab pair.' },
  { id: 'format-walkthroughs', where: 'Reference · byte by byte', icon: 'list-numbers', name: 'Format Walk-throughs', desc: 'ISO-0 step by step — PIN field, PAN block, XOR, encrypt — and what changes for ISO-1, ISO-3, ISO-4 and the OEM / ECI variants.' },
  { id: 'aes-pin', where: 'Tool · Encode & Decode buttons', icon: 'dots-nine', name: 'AES PIN Block (ISO-4) Calculator', desc: 'The PIN Block (AES) tool focuses on the modern ISO Format 4 design — both directions from one form.' },
  { id: 'translate', where: 'Tab · AS2805 Calculator', icon: 'swap', name: 'PIN Block Translation (AS2805)', desc: 'Re-encrypt a PIN block from a terminal key to a zone key — and change its format on the way — without exposing the clear PIN.' },
  { id: 'dukpt-pin', where: 'Tab · DUKPT Utilities', icon: 'key', name: 'DUKPT PIN', desc: 'Encrypt or decrypt a PIN block under a per-transaction PIN Entry Key derived from a BDK or IPEK.' },
  { id: 'as2805', where: 'Tool · four tabs', icon: 'map-pin', name: 'AS2805 Calculator', desc: 'Generate terminal key sets under a KEKr, compute AS 2805.4 MACs and one-way functions — the three tabs beside Translate PIN Block.' },
  { id: 'dukpt', where: 'Tools · 3DES and AES', icon: 'key', name: 'DUKPT Utilities', desc: 'Derive PEK and DEK working keys from a BDK or IPEK plus KSN, then MAC or encrypt data with them; the AES tool covers X9.24-3.' },
  { id: 'card-verification', where: 'Tools · Generate & Validate tabs', icon: 'credit-card', name: 'AMEX CSC & MasterCard CVC3', desc: 'Generate or validate the Amex card security codes and MasterCard dynamic CVC3 values from the issuer keys.' },
  { id: 'mac', where: 'Tools · four calculators', icon: 'shield-check', name: 'MAC Calculators', desc: 'ISO/IEC 9797-1, ANSI X9.9 / X9.19, TDES CBC-MAC and HMAC — hex inputs, configurable padding and a truncation length.' },
  { id: 'bitmap', where: 'Tool · ISO 8583', icon: 'grid-four', name: 'Bitmap Calculator', desc: 'Toggle the fields present in a message and read the primary and secondary bitmaps, or paste a bitmap and see the fields it declares.' },
];

const SECTIONS: RawSection[] = [
  { id: 'formats', where: 'Reference · ISO 9564-1', icon: 'table', rail: 'PIN block formats', title: 'PIN Block Formats', blocks: [
    { t: 'table', head: ['Format', 'Source', 'PAN-bound?', 'Notes'], cols: '96px 200px 96px minmax(0,1fr)', rows: [['**ISO-0**', 'ISO 9564-1', 'Yes', 'PIN XOR PAN. Most common in legacy systems. Equivalent to ANSI X9.8.'], ['**ISO-1**', 'ISO 9564-1', 'No', 'PIN + random padding. Used when PAN is not available.'], ['**ISO-2**', 'ISO 9564-1', 'No', 'PIN + `F` padding. EMV ICC offline PIN.'], ['**ISO-3**', 'ISO 9564-1', 'Yes', 'Like ISO-0 but with random fill nibbles instead of zeros.'], ['**ISO-4**', 'ISO 9564-1 (2017)', 'Yes', '16-byte block; AES-only. Currently mandated for new deployments.'], ['**OEM-1**', 'Diebold / Docutel / NCR', 'Varies', 'Vendor-specific historical formats — rarely needed for new work.'], ['**ECI 1-4**', 'Eurocheque / EFT', 'Varies', 'European legacy variants.']] },
    { t: 'note', tone: 'teal', icon: 'check-circle', title: 'Use ISO-4 for new work', x: 'If you have flexibility, target ISO Format 4 with AES — it’s the only format approved for new PCI-PIN evaluations.' },
  ] },
  { id: 'pin-block', where: 'Tool · Encode & Decode tabs', icon: 'keyboard', title: 'PIN Block Calculator', intro: 'The PIN Block Calculator builds a formatted PIN block, or recovers the PIN from one. The format is picked once from the **PIN block format** drop-down at the top of the screen; **Encode** and **Decode** sit below it as a tab pair, so the same format applies in both directions.', blocks: [
    panel('pin-encode', 'Format 0 (ISO-0) selected in the PIN block format drop-down, the Encode tab active, and PAN and PIN fields above the Encode button'),
    { t: 'h3', x: 'Inputs' },
    { t: 'bullets', items: [['PIN block format', 'Drop-down covering the ISO 9564 formats and the OEM / ECI variants. `Format 0 (ISO-0)` is the default.'], ['PAN', 'Only the PAN-bound formats ask for it; the field reads *PAN is required for this format* when the current selection needs one.'], ['PIN', '4–12 numeric digits.']] },
    { t: 'p', x: 'The rest of the form follows the format you pick, so a format that carries no PAN simply drops that field.' },
    { t: 'h3', x: 'Walk-through (Encode, ISO-0)' },
    { t: 'steps', items: ['Open the PIN Block Calculator from `Tools → Payment Utilities → PIN Block Calculator`.', 'Pick **Format 0 (ISO-0)** in the **PIN block format** drop-down.', 'Stay on the **Encode** tab.', 'Enter the PAN. ISO-0 uses the rightmost 12 digits excluding the check digit.', 'Enter the PIN (e.g. `1234`).', 'Click **Encode**. The formatted PIN block is written to the activity log with its inputs.'] },
    { t: 'p', x: 'The **Decode** tab reverses the same format: give it the PIN block and it recovers the PIN.' },
    panel('pin-decode', 'the Decode tab of the same tool — the PIN block in, the PIN out'),
    { t: 'note', tone: 'blue', icon: 'info', title: 'Formatting and encryption are separate steps', x: 'This tool produces the *formatted* PIN block. To encrypt it under a working key, take the block into the [DES / 3DES or AES calculator](/tools/cipher-tools), or use [DUKPT PIN](/tools/pin-tools#dukpt-pin) when the key comes from a DUKPT derivation.' },
  ] },
  { id: 'format-walkthroughs', where: 'Reference · byte by byte', icon: 'list-numbers', rail: 'Format walk-throughs', title: 'Format Walk-throughs', blocks: [
    { t: 'h3', x: 'ISO-0 / ANSI X9.8' },
    { t: 'code', bold: [8], lines: ['Step 1 (PIN block):  04 12 34 FF FF FF FF FF', '   - 04 = PIN length (4 digits)', '   - 1234 = PIN', '   - FF padding to 8 bytes', '', 'Step 2 (PAN block):  00 00 12 34 56 78 90 12', '   - leading zeros + rightmost 12 digits of PAN excluding check digit', '', 'Step 3 (XOR):        04 12 26 CB A9 87 6F ED', 'Step 4 (Encrypt):    encrypted under TPK / ZPK'] },
    { t: 'h3', x: 'ISO-1' },
    { t: 'p', x: 'No PAN; the PIN is followed by random fill bytes. Use when PAN is not transmitted (e.g. some IVR / VRU flows).' },
    { t: 'h3', x: 'ISO-3' },
    { t: 'p', x: 'Like ISO-0 but the padding nibbles are random in the range `0xA`–`0xF`. Each generated PIN block is unique even for the same PIN + PAN combination.' },
    { t: 'h3', x: 'ISO-4 (AES)' },
    { t: 'p', x: '16-byte clear PIN block: control field, PIN length, PIN digits, then a random fill. Encrypted with AES (128 / 192 / 256-bit) and XOR-combined with a derived PAN block. Use the dedicated [AES PIN Block](/tools/pin-tools#aes-pin) tab for ISO-4 work.' },
    { t: 'h3', x: 'OEM-1 / ECI' },
    { t: 'p', x: 'Reserved for compatibility with legacy ATM and POS networks. The exact layout differs per vendor; the calculator labels each tab with the vendor name.' },
  ] },
  { id: 'aes-pin', where: 'Tool · Encode & Decode buttons', icon: 'dots-nine', rail: 'AES PIN Block (ISO-4)', title: 'AES PIN Block (ISO-4) Calculator', intro: 'The PIN Block (AES) tool focuses on the modern ISO Format 4 design. One form covers both directions — **Encode** and **Decode** are buttons rather than tabs, and the middle field changes meaning between them.', blocks: [
    panel('aes-pin', 'a 32-hex Key field, the combined PIN (Encode) / PIN Block (Decode) field, a PAN field, and Encode and Decode buttons'),
    { t: 'h3', x: 'Inputs' },
    { t: 'bullets', items: [['Key', '32 hex chars (AES-128).'], ['PIN (Encode) / PIN Block (Decode)', 'One field serving both directions: the PIN going in, the encrypted block coming back out.'], ['PAN', 'Required for the PAN block XOR step.']] },
    { t: 'p', x: 'Buttons: **Encode**, **Decode**.' },
    { t: 'h3', x: 'Output' },
    { t: 'bullets', items: ['Clear PIN block (16 bytes)', 'PAN block (16 bytes)', 'Intermediate ciphertext (after AES of clear PIN block)', 'Final encrypted PIN block (after XOR with PAN block)'] },
    { t: 'note', tone: 'teal', icon: 'key', title: 'DUKPT-AES', x: 'For DUKPT AES PIN translation, derive the AES PIN working key in the DUKPT Tools first, then plug it in here.' },
  ] },
  { id: 'translate', where: 'Tab · AS2805 Calculator', icon: 'swap', rail: 'PIN block translation', title: 'PIN Block Translation (AS2805)', intro: 'Translation re-encrypts a PIN block from one key to another without ever exposing the clear PIN — the acquiring step that moves a PIN from the terminal key (TPK) it arrived under to the zone key (ZPK) shared with the next node. The **AS2805 Calculator** carries it on its *Translate PIN Block* tab, and can change the PIN block format in the same operation.', blocks: [
    panel('translate', 'System ZPK and Terminal TPK hex fields, STAN, Transaction Amount, incoming and outgoing PIN block format selectors, the incoming PIN block and an account number, above a Translate button'),
    { t: 'h3', x: 'Inputs' },
    { t: 'bullets', items: [['System ZPK (Hex)', 'The zone key the block is translated *to*.'], ['Terminal TPK (Hex)', 'The terminal key the block arrived under.'], ['STAN', 'System trace audit number of the transaction.'], '**Transaction Amount**', ['Incoming / Outgoing PIN Block Format', 'Two-digit format codes, defaulting to `01`. Set them differently to translate the format as well as the key.'], '**Incoming PIN Block (Hex)**', ['Account Number', 'Binds the block for the PAN-bound formats.']] },
    { t: 'p', x: 'Button: **Translate**.' },
    { t: 'note', tone: 'blue', icon: 'map-pin', title: 'Where it lives', x: 'Open `Tools → Payment Utilities → AS2805 Calculator` and pick the **Translate PIN Block** tab. The same tool also generates terminal key sets and computes AS2805 MACs and one-way functions.' },
  ] },
  { id: 'as2805', where: 'Tool · four tabs', icon: 'map-pin', rail: 'AS2805 Calculator', title: 'AS2805 Calculator', intro: 'The AS2805 Calculator is one tool with four tabs — **Generate Terminal Key Set**, **Translate PIN Block**, **MAC** and **OWF**. Translation is covered [above](/tools/pin-tools#translate); the other three tabs share the same form-and-log layout.', blocks: [
    { t: 'h3', x: 'Generate Terminal Key Set' },
    { t: 'p', x: 'Creates the terminal keys — TPK and TAK — an AS2805 terminal receives at logon, wrapped under the receiving key-encrypting key.' },
    panel('as-keys', 'Key Flag, KEKr Key, the KEKE and LMK key schemes and the key check value type above Generate Keys'),
    { t: 'bullets', items: [['Key Flag', 'Drop-down; `1` by default.'], ['KEKr Key (Hex)', 'The receiving KEK the new set is wrapped under.'], ['Key Scheme KEKE / Key Scheme LMK', 'Drop-downs, `B` and `Z` by default — the scheme tags for the KEK-wrapped and LMK-wrapped copies.'], ['Key Check Value Type', 'Drop-down; `1` by default.']] },
    { t: 'steps', items: ['Open `Tools → Payment Utilities → AS2805 Calculator`; the tool opens on **Generate Terminal Key Set**.', 'Enter the KEKr key. Leave the key flag, schemes and KCV type at their defaults unless your host specifies otherwise.', 'Click **Generate Keys**. The TPK and TAK land in the activity log with their key check values.'] },
    { t: 'h3', x: 'MAC' },
    panel('as-mac', 'Key (Hex) and Data (Hex) above Calculate MAC'),
    { t: 'p', x: 'Two hex fields — **Key (Hex)** and **Data (Hex)** — and a **Calculate MAC** button. The MAC is computed to AS 2805.4 over the data under the key you give it.' },
    { t: 'h3', x: 'OWF' },
    panel('as-owf', 'Key (Hex) and Data (Hex) above Calculate OWF'),
    { t: 'p', x: 'The one-way function used in AS2805 key management. Same two fields; **Calculate OWF** writes the result to the log.' },
    { t: 'note', tone: 'blue', icon: 'info', title: 'Both fields are required', x: 'Every field on these tabs validates as you type — an empty key or data field reads *cannot be empty* and the button stays disabled until both are filled.' },
  ] },
  { id: 'dukpt-pin', where: 'Tab · DUKPT Utilities', icon: 'key', title: 'DUKPT PIN', intro: 'Under DUKPT, the PIN is encrypted with a PIN Entry Key (PEK) derived per transaction rather than a static TPK. The **DUKPT PIN** tab takes a PEK you have already derived and encrypts or decrypts a PIN block with it.', blocks: [
    panel('dukpt-pin', 'a 32-hex PEK field and a 16-hex PIN Block field above Encrypt and Decrypt buttons'),
    { t: 'h3', x: 'Inputs' },
    { t: 'bullets', items: [['PEK', '32 hex chars. Derive it first on the *PEK Derivation* tab from a BDK or IPEK plus the KSN.'], ['PIN Block', '16 hex chars: the clear block to encrypt, or the encrypted block to recover.']] },
    { t: 'p', x: 'Buttons: **Encrypt**, **Decrypt**.' },
    { t: 'note', tone: 'teal', icon: 'list-numbers', title: 'Two-step flow', x: 'Build the block in the [PIN Block Calculator](/tools/pin-tools#pin-block), then encrypt it here under the derived PEK. Key derivation itself — PEK, DEK, and the AES variants — is covered in [DUKPT Utilities](/tools/pin-tools#dukpt) below.' },
  ] },
  { id: 'dukpt', where: 'Tools · DUKPT Utilities & DUKPT AES Utilities', icon: 'key', rail: 'DUKPT Utilities', title: 'DUKPT Utilities', intro: 'Derived Unique Key Per Transaction generates a fresh key for every transaction without ever transmitting it: the terminal holds an IPEK derived from a BDK and a KSN, advances the KSN counter each time, and the host — knowing only the BDK and the KSN it received — derives the same key independently. The **DUKPT Utilities** tool implements ANSI X9.24-1 with 3DES across five tabs: two derivation tabs that produce working keys and three operation tabs that consume them. **DUKPT AES Utilities** does the same for ANSI X9.24-3. For the derivation walk-through, KSN layout and the X9.24 test vectors, the reference page is [DUKPT Tools](/tools/dukpt-tools); the MAC calculators have their own page at [MAC Tools](/tools/mac-tools).', blocks: [
    { t: 'table', head: ['Variant', 'Standard', 'Cipher', 'BDK', 'KSN'], cols: '150px 120px 150px minmax(0,1fr) minmax(0,1fr)', rows: [['**DUKPT ISO 9797**', 'ANSI X9.24-1', '3DES', '16 bytes (32 hex)', '10 bytes (20 hex)'], ['**DUKPT AES**', 'ANSI X9.24-3', 'AES-128 / 192 / 256', '16 / 24 / 32 bytes', '12 bytes (24 hex)']] },
    { t: 'h3', x: 'PEK Derivation' },
    panel('dukpt-pek', 'BDK selected as the input key designation, the BDK and KSN fields and Derive PEK — run on the X9.24 sample BDK and KSN'),
    { t: 'bullets', items: [['Input Key Designation', 'Drop-down: `BDK` or `IPEK`. Determines which key field is shown.'], ['BDK (32 Hex Chars) / IPEK (32 Hex Chars)', 'Whichever the designation asks for.'], ['KSN (20 Hex Chars)', 'Always visible; the rightmost 21 bits are the transaction counter.']] },
    { t: 'steps', items: ['Pick **Input Key Designation** — `BDK` if you have the base key, `IPEK` if a previous step already produced the initial key.', 'Enter the key in the resulting **BDK** or **IPEK** field (32 hex chars).', 'Enter the **KSN** (20 hex chars).', 'Click **Derive PEK**. The activity log shows the resulting PIN Encryption Key — copy it for the operation tabs.'] },
    { t: 'h3', x: 'DEK Derivation' },
    panel('dukpt-dek', 'the same designation, BDK and KSN fields above Derive DEK'),
    { t: 'p', x: 'Identical fields and flow to PEK Derivation — the only difference is the variant applied, which produces a Data Encryption Key instead.' },
    { t: 'h3', x: 'DUKPT MAC' },
    panel('dukpt-mac', 'a PEK field, the DES / 3DES algorithm radio pair and a hex Data field above Generate MAC'),
    { t: 'bullets', items: [['PEK (32 Hex Chars)', 'The MAC key, typically derived on the PEK Derivation tab.'], ['Algorithm', 'Radio: `DES` or `3DES`.'], ['Data (Hex)', 'Multi-line hex input.']] },
    { t: 'p', x: 'Button: **Generate MAC**.' },
    { t: 'h3', x: 'DUKPT Data' },
    panel('dukpt-data', 'a PEK field, the Use Data Variant Key switch, the ASCII data input type, the CBC / ECB cipher-mode radio pair and a Data field above Encrypt and Decrypt'),
    { t: 'bullets', items: [['PEK (32 Hex Chars)', 'Working key from PEK Derivation, or from DEK Derivation when the data variant is required.'], ['Use Data Variant Key', 'Switch. When on, the tool applies the data-encryption variant XOR before encrypting.'], ['Data Input Type', '`ASCII` or `Hex`.'], ['Cipher Mode (zero IV)', 'Radio: `CBC` or `ECB`.'], ['Data', 'Multi-line input in the chosen format.']] },
    { t: 'p', x: 'Buttons: **Encrypt**, **Decrypt**.' },
    { t: 'note', tone: 'teal', icon: 'list-numbers', title: 'Two-step workflow', x: 'The tool deliberately separates derivation from use: derive the working key on *PEK Derivation* or *DEK Derivation* first, then paste the result into the *DUKPT PIN*, *MAC* or *Data* tab. This mirrors how a host-side stack stages keys.' },
    { t: 'h3', x: 'DUKPT AES Utilities · Key Derivation' },
    { t: 'p', x: 'The AES tool implements ANSI X9.24-3. Four tabs — **Key Derivation**, **DUKPT PIN**, **DUKPT MAC**, **DUKPT Data** — split derivation from operations the same way.' },
    panel('dukpt-aes', 'the Key Derivation tab — BDK as the input key designation, AES-128 initial and working key types, BDK / IK and KSN fields above Derive Keys'),
    { t: 'bullets', items: [['Input Key Designation', '`BDK` or `IK`.'], ['Initial Key Type', 'Drop-down: `AES-128`, `AES-192`, `AES-256`.'], ['BDK / IK', 'Hex; length matches the initial key type.'], ['Working Key Type', 'Drop-down: `2TDEA`, `3TDEA`, `AES-128`, `AES-192`, `AES-256` — TDES working keys too, for backwards compatibility.'], ['KSN', '24 hex chars: 4-byte BDK ID, 4-byte derivation ID, 4-byte counter.']] },
    { t: 'steps', items: ['Pick **Input Key Designation** and **Initial Key Type**.', 'Enter the **BDK / IK**.', 'Pick **Working Key Type** and enter the **KSN**.', 'Click **Derive Keys**. The activity log lists the IK (when starting from a BDK) and the working key.'] },
    { t: 'code', bold: [0, 3], lines: ['3DES KSN (10 bytes)', '| 5-byte BDK ID + Device ID | 2-byte counter (high) | 21-bit Tx Counter |', '', 'AES KSN (12 bytes)', '| 4-byte BDK ID | 4-byte Derivation ID | 4-byte Transaction Counter |'] },
    { t: 'note', tone: 'warn', icon: 'warning', title: 'Keep the counters in sync', x: 'The KSN you receive in field 53 / 60 of an ISO 8583 message is what the host uses to derive the same key. Off-by-one is the most common bug — if MACs suddenly go wrong, check whether the terminal advanced its counter without you advancing yours.' },
  ] },
  { id: 'card-verification', where: 'Tools · Generate & Validate tabs', icon: 'credit-card', rail: 'Card verification', title: 'AMEX CSC & MasterCard CVC3', intro: 'Two issuer-side calculators for card security values. Each has a **Generate** tab and a **Validate** tab: Validate takes the same inputs plus the value under test and reports whether it matches. The card-verification reference page is [Card Validation](/tools/card-validation).', blocks: [
    { t: 'h3', x: 'AMEX CSC Calculator' },
    panel('csc-gen', 'the Generate tab — CSC Version, CSC Key, PAN, Expiration Date, Service Code and Verification Value Type above Generate'),
    { t: 'bullets', items: [['CSC Version', 'Drop-down; `Version 1` by default.'], ['CSC Key (Hex)', 'The Amex CSC key.'], ['PAN', '15-digit Amex account number.'], ['Expiration Date (YYMM)', 'Four digits.'], ['Service Code', 'Three digits.'], ['Verification Value Type', 'Drop-down; `CSC` by default.']] },
    { t: 'steps', items: ['Open `Tools → Payment Utilities → AMEX CSC`; the tool opens on **Generate**.', 'Pick the **CSC Version**, then enter the **CSC Key**, PAN, expiry and service code.', 'Click **Generate**. The activity log lists the CSC-5, CSC-4 and CSC-3 values.', 'To check values from a card, switch to **Validate**: enter the same PAN, expiry and service code, then the CSC-5, CSC-4 and CSC-3 you were given, and click **Validate**.'] },
    panel('csc-val', 'the Validate tab — PAN, Expiration Date, Service Code and Verification Value Type, then the three values to validate'),
    { t: 'h3', x: 'MasterCard CVC3' },
    panel('cvc3-gen', 'the Generate tab — IMK, PAN, PAN Seq No, Track 1/2 Data, Unpredictable Num, ATC and CVC3 Type above Generate'),
    { t: 'bullets', items: [['IMK', 'The issuer master key for CVC3.'], ['PAN / PAN Seq No', 'Derive the card key from the IMK.'], ['Track 1/2 Data', 'The track data the CVC3 is computed over.'], ['Unpredictable Num', 'The terminal’s unpredictable number, hex.'], ['ATC', 'Application transaction counter, hex.'], ['CVC3 Type', 'Drop-down; `Dynamic CVC3` by default.']] },
    { t: 'steps', items: ['Open `Tools → Payment Utilities → MasterCard CVC` on the **Generate** tab.', 'Enter the IMK, PAN and PAN sequence number, then the track data, unpredictable number and ATC from the transaction.', 'Click **Generate**. The log shows the derived key, the IVCVC3 and the CVC3.', 'On **Validate**, add the **Dynamic CVC3** received from the card and click **Validate** to compare.'] },
    panel('cvc3-val', 'the Validate tab — the same inputs plus a Dynamic CVC3 field to check'),
    { t: 'note', tone: 'blue', icon: 'info', title: 'Static CVV / CVC / iCVV', x: 'The static three-digit values are the **CVV Calculator**’s job — first tile in the hub, and covered in [EMV & Card Tools](/tools/emv-tools). The two tools here handle the Amex CSC family and MasterCard’s dynamic CVC3.' },
  ] },
  { id: 'mac', where: 'Tools · four calculators', icon: 'shield-check', rail: 'MAC calculators', title: 'MAC Calculators', intro: 'A Message Authentication Code is a short tag computed from a message and a secret key; the receiver recomputes it to confirm the message was not altered and came from someone holding the same key. Payment networks rely on MACs on ISO 8583 messages, terminal-host links and PIN-translation pipelines. The group carries a calculator for every MAC algorithm commonly seen in payment specifications — the block-cipher family, and HMAC for hash-based tags. The dedicated reference for ISO 9797-1, X9.9/X9.19 and HMAC — with the worked vectors — is [MAC Tools](/tools/mac-tools).', blocks: [
    { t: 'table', head: ['Algorithm', 'Standard', 'Block / Cipher', 'Common use'], cols: '150px 170px 170px minmax(0,1fr)', rows: [['**HMAC**', 'RFC 2104, FIPS 198-1', 'Hash (SHA-256, etc.)', 'API authentication, JWS, payment APIs.'], ['**CMAC**', 'NIST SP 800-38B', 'AES / TDES', 'EMV-like cryptograms, modern PIN translation.'], ['**TDES CBC-MAC**', 'ANSI X9.9 (legacy)', '3DES', 'Older banking integrations.'], ['**ANSI X9.19 MAC**', 'ANSI X9.19', 'Single DES + 3DES finalize', 'U.S. retail / banking ISO 8583.'], ['**ISO 9797 MAC**', 'ISO/IEC 9797-1', 'DES / 3DES / AES', 'Cross-network ISO 8583 MACs.'], ['**Retail MAC**', 'ISO 9797-1 Algorithm 3', '3DES', 'European retail payments.'], ['**AS2805 MAC**', 'AS 2805.4', '3DES', 'Australian payment systems.']] },
    { t: 'h3', x: 'ISO/IEC 9797-1' },
    { t: 'p', x: 'ISO/IEC 9797-1 standardises six MAC algorithms over block ciphers; the calculator exposes each from the **MAC Algorithm** drop-down.' },
    panel('mac-9797', 'Algorithm 1 selected, the Key (K′) field, Padding, Data and Truncation Length above Generate MAC — the worked example below, "Hello" under Method 2 padding'),
    { t: 'table', head: ['Variant', 'Description', 'Typical cipher'], cols: '120px minmax(0,1fr) 150px', rows: [['**Algorithm 1**', 'Plain CBC-MAC. Single key. Last block is the MAC.', 'DES / 3DES / AES'], ['**Algorithm 2**', 'Last block encrypted with a derived key (`K′`).', 'DES'], ['**Algorithm 3**', 'Retail MAC: single DES CBC-MAC, then 3DES finalize. Equivalent to ANSI X9.19.', 'DES + 3DES'], ['**Algorithm 4**', 'CBC-MAC with two parallel CBC-MAC chains XOR-combined.', 'DES / 3DES'], ['**Algorithm 5**', 'EMAC: CBC-MAC re-encrypted with a second key.', 'AES'], ['**Algorithm 6**', 'MAC double-CBC encryption with separate keys.', 'AES']] },
    { t: 'bullets', items: [['MAC Algorithm', 'Drop-down, `Algorithm 1` through `Algorithm 6`.'], ['Key (K′)', 'Hex; the algorithms that need a second key derive it from this one.'], ['Padding', 'Drop-down: `Method 1`, `Method 2`, `Method 3`.'], ['Data (Hex)', 'The message to authenticate.'], ['Truncation Length (Chars)', 'Defaults to `8`.']] },
    { t: 'steps', items: ['Open `Tools → Payment Utilities → ISO/IEC 9797-1`.', 'Pick the **MAC Algorithm** and **Padding** your host specifies — Algorithm 1 with TDES is widespread for ISO 8583 MAC fields (bit 64 / 128); Algorithm 3 is the European retail standard.', 'Enter the key and the hex data, and set the truncation length.', 'Click **Generate MAC**. The padded data, the full MAC and the truncated MAC are written to the log.'] },
    { t: 'code', bold: [7], lines: ['Key:    0123456789ABCDEFFEDCBA9876543210', 'IV:     0000000000000000', 'Method: 2 (0x80 followed by zero bytes)', 'Data:   48656C6C6F     (ASCII "Hello")', '', 'Padded: 48656C6C6F800000', '', 'Output: 8F9E4CCDD623E590'] },
    { t: 'h3', x: 'ANSI X9.9 & X9.19' },
    { t: 'p', x: 'One tool covers both ANSI schemes — **X9.9** (wholesale) and **X9.19** (retail) — selected from the MAC Algorithm drop-down. X9.19 runs single-DES CBC-MAC across the message with a final 3DES step over the last block, which makes it equivalent to ISO 9797-1 Algorithm 3.' },
    panel('mac-ansi', 'ANSI MAC X9.9 (Wholesale MAC) selected, Key (K), Data and Truncation Length above Generate MAC'),
    { t: 'bullets', items: [['MAC Algorithm', '`ANSI MAC X9.9 (Wholesale MAC)` or the X9.19 retail variant.'], ['Key (K)', 'Hex key; the 3DES halves are taken from it in order.'], ['Data (Hex)', 'The message to authenticate.'], ['Truncation Length (Chars)', 'Defaults to `8`.']] },
    { t: 'h3', x: 'TDES-CBC MAC' },
    { t: 'p', x: 'Triple-DES CBC-MAC: encrypt the message under TDES in CBC mode and take the last block as the MAC. Common in legacy ISO 8583 implementations.' },
    panel('mac-tdes', 'the fixed TDES CBC-MAC algorithm, a 32-hex Key (K), ISO9797-1 Padding Method 1, Data and Truncation Length above Generate MAC'),
    { t: 'bullets', items: [['MAC Algorithm', 'Fixed to `TDES CBC-MAC` on this tool.'], ['Key (K)', '32 hex chars (2-key 3DES).'], ['Padding', 'Drop-down; defaults to `ISO9797-1 (Padding Method 1)`.'], ['Data (Hex)', 'The message to authenticate.'], ['Truncation Length (Chars)', 'Hex characters of MAC to keep; defaults to `8`.']] },
    { t: 'note', tone: 'warn', icon: 'warning', title: 'Security note', x: 'Pure CBC-MAC is vulnerable to length extension when the message length is variable. For variable-length messages, use ISO 9797-1 Algorithm 3 (Retail MAC) instead.' },
    { t: 'h3', x: 'HMAC' },
    { t: 'p', x: 'Hash-based MAC defined by RFC 2104. Key and data are each entered as ASCII or hexadecimal; the output is hex of the digest length — 32 bytes for SHA-256, 64 for SHA-512.' },
    panel('mac-hmac', 'SHA-256 as the hash type, a hex key and ASCII data, the HMAC Key and Data fields above Generate HMAC — RFC 4231 test case 2'),
    { t: 'bullets', items: [['Hash Type', 'Drop-down: `MD5`, `SHA-1`, `SHA-224`, `SHA-256`, `SHA-384`, `SHA-512`, `RIPEMD-160`.'], ['Key Input / Data Input', 'Drop-downs: `ASCII` or `Hexadecimal`.'], ['HMAC Key', 'Single-line text in the chosen format.'], ['Data', 'Multi-line text in the chosen format.']] },
    { t: 'steps', items: ['Pick **Hash Type** — `SHA-256` is a safe default for new work.', 'Pick **Key Input** and enter the **HMAC Key** in that format.', 'Pick **Data Input** and enter the **Data**.', 'Click **Generate HMAC**. The digest is appended to the activity log.'] },
    { t: 'h4', x: 'Padding methods (ISO 9797-1)' },
    { t: 'table', head: ['Method', 'Rule', 'Notes'], cols: '110px minmax(0,1fr) minmax(0,1fr)', rows: [['**Method 1**', 'Append `00` bytes to the next block boundary.', 'Simple but ambiguous — cannot distinguish trailing zeros in plaintext.'], ['**Method 2**', 'Append a single `80` byte, then `00` bytes.', 'Self-describing and unambiguous. Recommended.'], ['**Method 3**', 'Prefix the message with its length, then pad with `00`.', 'Used in some legacy systems; rare in payments.']] },
    { t: 'note', tone: 'blue', icon: 'info', title: 'Match the padding to the host spec', x: 'A wrong padding method produces a deterministic but wrong MAC — one of the most common debugging traps. Confirm the IV too: many hosts default to all zeros, but some chain the previous transaction’s MAC.' },
  ] },
  { id: 'bitmap', where: 'Tool · ISO 8583', icon: 'grid-four', rail: 'Bitmap Calculator', title: 'Bitmap Calculator', intro: 'An ISO 8583 bitmap declares which data elements follow the message type: 64 bits, one per field, with bit 1 announcing that a secondary bitmap for fields 65–128 is present. The Bitmap Calculator works in both directions — toggle the fields present and read the hex, or paste a bitmap and see the fields it declares.', blocks: [
    panel('bitmap', 'the calculated bitmap with its copy button, and the primary (1–64) and secondary (65–128) grids with the present fields lit'),
    { t: 'steps', items: ['Open `Tools → Payment Utilities → Bitmap Calculator`.', 'Click the fields present in your message in the **Primary Bitmap (1-64)** grid. Click any field above 64 in the **Secondary Bitmap (65-128)** grid and field 1 switches on by itself.', 'Read the **Calculated Bitmap** at the top and copy it with the button at its right.', 'Going the other way, paste a 16- or 32-hex bitmap into the field and the grids light up the fields it declares. The refresh button in the title bar clears the grid.'] },
    { t: 'h3', x: 'Byte walk-through' },
    { t: 'code', bold: [0, 9], lines: ['E0701800 28000A00 60000000 00000001', '', 'E0 = 1110 0000  →  1  2  3     secondary bitmap present · PAN · processing code', '70 = 0111 0000  →  10 11 12    billing conversion rate · STAN · local time', '18 = 0001 1000  →  20 21       PAN country code · forwarding institution country', '28 = 0010 1000  →  35 37       Track 2 · RRN', '0A = 0000 1010  →  53 55       security control info · ICC data', '00 bytes        →  no fields in that run of eight', '', '60 = 0110 0000  →  66 67       settlement code · extended payment code', '01 = 0000 0001  →  128         MAC'] },
    { t: 'note', tone: 'blue', icon: 'info', title: 'Reading a captured message', x: 'Paste the bitmap from a captured 0200 into the calculator to see which fields to expect before running the message through the **Message Parser** in the same group.' },
  ] },
];

const TIPS: string[] = [
  'If the host rejects your PIN block, verify the format on both sides — ISO-0 and ISO-3 look identical at a glance but produce different blocks.',
  'When testing DUKPT-protected PIN flows, derive the working key first ([PEK Derivation](/tools/pin-tools#dukpt)), then encrypt or decrypt with it under [DUKPT PIN](/tools/pin-tools#dukpt-pin). The activity log shows both the input PIN block and the decrypted clear PIN for cross-checking.',
  'For DUKPT-derived MAC keys, derive the session key in [DUKPT Utilities](/tools/pin-tools#dukpt) first, then plug the result into the corresponding [MAC calculator](/tools/pin-tools#mac).',
  'Keep BDKs out of source control. The activity log holds keys in memory for the session but never writes them to disk.',
];


export const PIN_TOOLS_GUIDE: ToolGuideData = {
  slug: 'pin-tools',
  crumb: 'Payment Utilities',
  meta: 'PIN blocks · DUKPT · MACs · CVC3 · bitmaps',
  title: 'Payment Utilities',
  lede: 'PIN block encoding across the ISO 9564 formats and OEM variants, AES-encrypted PIN blocks, TPK-to-ZPK translation and DUKPT PIN encryption — alongside the AS2805, DUKPT, card-verification, MAC and bitmap utilities in the same group. Each tool validates inputs in real time and logs every operation for audit.',
  hub: {
    title: 'Payment Utilities',
    sub: 'Payment processing and validation utilities',
    tools: HUB,
    stages: HUB_STAGES,
    columns: 3,
    category: 4,
    dense: true,
    search: 'Search payment utilities…',
    badge: '21 tools',
    label: 'PIN blocks, card verification, MACs, DUKPT and ISO 8583 helpers',
    note: 'Every operation logged with its inputs.',
    aria: 'The Payment Utilities hub: tool categories, five tool families and the 21 tools in the group',
  },
  panels: PANELS,
  panelBadge: 'PIN',
  captionFrom: 'title',
  intro: {
    eyebrow: 'pin-tools · Payment Utilities hub — above',
    paras: INTRO,
    features: toFeatures(FAMILIES),
  },
  allTools: {
    lede: 'Every tool in this category — each card links to the detailed reference below.',
    cards: TOOLS.map((tool) => ({ id: tool.id, icon: tool.icon, name: tool.name, desc: tool.desc, tag: tool.where })),
  },
  sections: SECTIONS.map((section) => ({
    id: section.id,
    title: section.title,
    rail: section.rail,
    icon: section.icon,
    eyebrow: section.where,
    intro: section.intro,
    blocks: section.blocks,
  })),
  tips: TIPS,
  cta: {
    heading: 'Try it on your own transactions',
    text: 'Free and open source. Download the studio and run these calculators on your desk in minutes.',
  },
};
