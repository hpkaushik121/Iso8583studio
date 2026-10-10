/**
 * EMV & Card Tools — page content and the specs its glass panels render from.
 * Ported from the prototype's emv-tools-data.js; imported only by
 * pages/site/docs-emv-tools.ts.
 *
 * Every panel output that can be computed is a real value for the inputs shown (verified with
 * openssl against the app's algorithms); outputs that depend on random padding or vendor secrets
 * describe the result's layout instead of inventing hex. Field defaults still come from hx().
 */
import { GlassField, GlassSpec, H, HubStage, HubTool, S, T, grp } from '../shared/glass/glass-model';
import { ToolBlock, ToolGuideData, panel, stageTag } from '../shared/glass/tool-guide';

/** One screen of a tool: its heading, the panel, the inputs it takes and the button that runs it. */
interface RawPart {
  h: string | null;
  panel: string;
  lead?: string;
  caption?: string;
  inputs?: [string, string][];
  button?: string;
  note?: string;
}

interface RawSection {
  id: string;
  stage: number;
  title: string;
  intro: string;
  parts: RawPart[];
}

const zeros = '0'.repeat(32);

const TABS_SDA = ['Retrieve Issuer PK', 'Verify SSAD'];
const TABS_DDA = ['Retrieve Issuer PK', 'Retrieve ICC PK', 'Verify SDAD'];
const TABS_41 = ['UDK Derivation', 'Session Keys', 'Cryptogram', 'ARPC', 'Utilities'];
const TABS_MC = ['UDK', 'Session Key (EMV 2000)', 'Session Keys', 'AAC/ARQC/TC', 'ARPC'];
const TABS_V = ['UDK', 'Session Keys', 'AAC/ARQC/TC', 'ARPC'];
const TABS_SM = ['Session Key', 'PIN Block', 'MAC'];
const TABS_HCE = ['UDK', 'LUK Key', 'MSD', 'qVSDC'];
const issuerPk = (): GlassField[] => [H('CA PK Modulus', 248, { w: 4 }), H('CA PK Exponent', 2, { v: '03', w: 2 }), H('Issuer PK Certificate', 248, { tag: '90', w: 6 }), H('Issuer PK Remainder', 72, { tag: '92', opt: true, w: 4 }), H('Issuer PK Exponent', 2, { tag: '9F32', v: '03', w: 2 })];
const issuerOut: [string, string][] = [['Issuer PK', '1024-bit modulus recovered from the certificate'], ['Certificate', 'hash matches · valid to 12/29']];

const PANELS: Record<string, GlassSpec> = {
  'sda-retrieve': { title: 'SDA Verification', sub: 'Retrieve Issuer Public Key', tabs: TABS_SDA, tab: 0, icon: 'key', fields: issuerPk(), button: 'Retrieve Key', result: issuerOut },
  'sda-verify': { title: 'SDA Verification', sub: 'Verify SSAD', tabs: TABS_SDA, tab: 1, icon: 'seal-check', fields: [H('Issuer PK Modulus', 248, { w: 6 }), H('SSAD (Signed Static Data)', 248, { tag: '93', w: 6 })], button: 'Verify SSAD', result: [['Hash', '20 bytes · SHA-1 over the static data'], ['DAC', '2 bytes · from the recovered SSAD'], ['SSAD', 'verified · PASS']] },
  'dda-issuer': { title: 'DDA – Dynamic Data Authentication', sub: 'Retrieve Issuer PK', tabs: TABS_DDA, tab: 0, icon: 'key', fields: issuerPk(), button: 'Retrieve Key', result: issuerOut },
  'dda-icc': { title: 'DDA – Dynamic Data Authentication', sub: 'Retrieve ICC Public Key', tabs: TABS_DDA, tab: 1, icon: 'key', fields: [H('Issuer PK Modulus', 248, { w: 4 }), H('Issuer PK Exponent', 2, { v: '03', w: 2 }), H('ICC PK Certificate', 208, { tag: '9F46', w: 6 }), H('ICC PK Remainder', 42, { tag: '9F48', opt: true, w: 4 }), H('ICC PK Exponent', 2, { tag: '9F47', v: '03', w: 2 }), H('Static Data To Authenticate', 120, { w: 4 }), H('AIP', 4, { tag: '82', v: '3900', w: 2 })], button: 'Retrieve ICC Key', result: [['ICC PK', '896-bit modulus recovered from the ICC certificate'], ['Certificate', 'hash matches · static data covered']] },
  'dda-verify': { title: 'DDA – Dynamic Data Authentication', sub: 'Verify SDAD', tabs: TABS_DDA, tab: 2, icon: 'seal-check', fields: [H('ICC PK Modulus', 224, { w: 4 }), H('ICC PK Exponent', 2, { v: '03', w: 2 }), H('SDAD (Signed Dynamic Data)', 224, { tag: '9F4B', w: 6 }), H('Dynamic Data', 8, { tag: '9F37', w: 6 })], button: 'Verify Dynamic Signature', result: [['ICC Dynamic Number', '8 bytes · recovered from the SDAD'], ['Signature', 'verified · PASS']] },
  'emv41-udk': { title: 'EMV 4.1 Crypto Calculator', sub: 'UDK Derivation', tabs: TABS_41, tab: 0, icon: 'calculator', fields: [H('Master Derivation Key (MDK)', 32, { v: '8A7A9ED9A4E379EAF46A820CBD06CC4C', w: 6 }), T('PAN', '4761 7300 0000 0011', { w: 4 }), T('PAN Sequence', '00', { w: 2 }), S('Derivation Option', 'OPTION_A'), S('Key Parity', 'ODD')], button: 'Calculate UDK', result: [['UDK', grp('D385233DBC320D32D058EC54021F92AE')], ['KCV', 'B1109A']] },
  'emv41-sk': { title: 'EMV 4.1 Crypto Calculator', sub: 'Session Key Derivation', tabs: TABS_41, tab: 1, icon: 'calculator', fields: [H('Master Key (UDK)', 32, { v: 'A8BC2D57A964BA9AC37E6073B03062E3', w: 6 }), H('Initial Vector (IV)', 32, { v: zeros, w: 6 }), H('ATC', 4, { v: '001C', w: 2 }), T('Branch Factor', '50', { w: 2 }), T('Height', '8', { w: 2 }), S('Key Parity', 'ODD', { w: 6 })], button: 'Generate Session Key', result: [['Session Key', grp('6DC48AB6528649FEB5A4B543A226F4CD')], ['KCV', 'CB6AD7']] },
  'emv41-ac': { title: 'EMV 4.1 Crypto Calculator', sub: 'Application Cryptogram', tabs: TABS_41, tab: 2, icon: 'calculator', fields: [H('Session Key', 32, { v: 'BF38CC4988C08B3F0F18884CF9FA594D', w: 6 }), H('Terminal Data', 58, { v: '1052D94A2665CBB64B7AB4D045FE988175F87B1276588EFEA5F62A2261', w: 6 }), H('ICC Data', 46, { v: '84FB820141E6F778C953226736D4D8A923FBE1AB1771FA', w: 6 }), S('Cryptogram Type', 'ARQC'), S('Padding Method', 'METHOD_1_ISO_9797')], button: 'Generate ARQC', result: [['ARQC', grp('A5091304C0517CF5')], ['Padding', 'ISO 9797-1 method 1 · 52 → 56 bytes']] },
  'emv41-arpc': { title: 'EMV 4.1 Crypto Calculator', sub: 'ARPC Generation', tabs: TABS_41, tab: 3, icon: 'calculator', fields: [H('Session Key', 32, { v: 'BF38CC4988C08B3F0F18884CF9FA594D', w: 6 }), H('Transaction Cryptogram', 16, { v: '256E1910EDF91562', w: 6 }), T('Response Code', 'Y3', { w: 3 }), S('ARPC Method', 'Method 1')], button: 'Generate ARPC', result: [['ARPC', grp('3AA4186A51D93742')], ['ARC', 'Y3 · online approved']] },
  'emv41-util': { title: 'EMV 4.1 Crypto Calculator', sub: 'Cryptographic Utilities', tabs: TABS_41, tab: 4, icon: 'hash', fields: [H('Key (Hex)', 32, { v: '4D64AA87925831C1BF2109ED08B44845', w: 6 })], button: 'Calculate KCV', result: [['KCV', 'A40CE6'], ['Parity', 'odd · 16 bytes']] },
  'emv42-udk': { title: 'EMV 4.2 Crypto Calculator', sub: 'UDK Derivation · EMV 4.2 Option A', tabs: TABS_41, tab: 0, icon: 'calculator', fields: [H('Master Derivation Key (MDK)', 32, { v: '8A7A9ED9A4E379EAF46A820CBD06CC4C', w: 6 }), T('Primary Account Number (PAN)', '5413 3300 8960 1111', { w: 4 }), T('PAN Sequence Number', '01', { w: 2 })], banner: { tone: 'warn', text: 'PAN Luhn checksum failed, but calculation will proceed' }, button: 'Calculate UDK', result: [['UDK', grp('D908D9C84FB59720DCB3A19EB3578631')], ['KCV', 'FFEEF3']] },
  'mchip-udk': { title: 'MasterCard M/Chip Crypto Calculator', sub: 'UDK', tabs: TABS_MC, tab: 0, icon: 'calculator', fields: [H('MDK', 32, { v: 'C8C78DECFEF44FE19529E8C492BA2A02', w: 6 }), T('PAN', '5413 3300 8960 1111', { w: 4 }), T('PAN Sequence No.', '00', { w: 2 }), S('UDK Derivation Option', 'Option A'), S('Key Parity', 'NONE')], banner: { tone: 'warn', text: 'PAN Luhn checksum failed, but calculation will proceed' }, button: 'Generate UDK', result: [['UDK', grp('A6A515C26B994FDF27531333BF38E5BE')], ['KCV', '9B6270']] },
  'vsdc-udk': { title: 'VSDC Crypto Calculator', sub: 'UDK', tabs: TABS_V, tab: 0, icon: 'calculator', fields: [H('MDK', 32, { v: 'C8C78DECFEF44FE19529E8C492BA2A02', w: 6 }), T('PAN', '4761 7300 0000 0011', { w: 4 }), T('PAN Sequence No.', '00', { w: 2 }), S('UDK Derivation Option', 'Option A'), S('Key Parity', 'Odd')], button: 'Generate UDK', result: [['UDK', grp('5BECB343168A0D61E5F49E758CA40D61')], ['KCV', 'F496AE']] },
  'sm-sk': { title: 'MasterCard Secure Messaging', sub: 'Session Key', tabs: TABS_SM, tab: 0, icon: 'key', fields: [S('Input Key Type', 'MK', { w: 6 }), H('MK-SMI', 32, { w: 6 }), H('MK-SMC', 32, { w: 6 }), H('Application Cryptogram (AC)', 16, { w: 4 }), T('Command Number', '01', { w: 2 })], button: 'Generate Session Keys', result: [['SK-SMI', '16 bytes · from MK-SMI and the AC'], ['SK-SMC', '16 bytes · from MK-SMC and the AC']] },
  'sm-pin': { title: 'MasterCard Secure Messaging', sub: 'PIN Block Generation', tabs: TABS_SM, tab: 1, icon: 'lock', fields: [S('Output PIN Block Format', 'Standard EMV PIN Block', { w: 6 }), H('SK-ENC', 32, { w: 6 }), T('New PIN', '••••', { w: 2, n: 4 })], button: 'Generate PIN Block', result: [['PIN Block', '8 bytes · EMV PIN block under SK-ENC'], ['Format', 'ISO 9564-1 format 2 · encrypted']] },
  'sm-mac': { title: 'MasterCard Secure Messaging', sub: 'MAC Calculation', tabs: TABS_SM, tab: 2, icon: 'shield-check', fields: [H('SK-MAC', 32, { w: 6 }), H('Class', 2, { v: '84', w: 1 }), H('INS', 2, { v: '24', w: 1 }), H('P1', 2, { v: '00', w: 1 }), H('P2', 2, { v: '02', w: 1 }), H('Lc', 2, { v: '18', w: 1 }), H('Le', 2, { v: '00', w: 1 }), T('ARC', '3030', { w: 2 }), H('AC', 16, { w: 4 }), H('Payload', 32, { w: 6 })], button: 'Generate MAC', result: [['MAC', '8 bytes · over the header, ARC, AC and payload'], ['Command', '84 24 00 02 18 · MAC appended']] },
  cap: { title: 'CAP Token Computation', sub: 'Chip Authentication Programme', tabs: null, icon: 'device-mobile', fields: [H('IPB', 40, { w: 6 }), H('IAF', 2, { v: '80', w: 2 }), T('PAN + SN', '5413330089601111 01', { w: 4 }), H('CID', 2, { v: '80', w: 2 }), H('ATC', 4, { v: '0A3C', w: 2 }), H('AC', 16, { w: 2 }), H('IAD', 36, { w: 6 })], button: 'Generate Token', result: [['Token', '8 digits'], ['Bits', 'selected by the IPB · decimalised']] },
  'hce-udk': { title: 'Visa HCE Crypto Calculator', sub: 'UDK', tabs: TABS_HCE, tab: 0, icon: 'calculator', fields: [H('Master Key', 32, { v: 'D3D5D756E8A4D3307A12061476E7A6B7', w: 6 }), T('PAN', '4761 7300 0000 0011', { w: 4 }), T('PAN Sequence Number', '00', { w: 2 })], button: 'Generate UDK', result: [['UDK', grp('F24A19836B61EF5E1F70CB4F0B3B7F1F')], ['KCV', 'D39FD6']] },
  'hce-luk': { title: 'Visa HCE Crypto Calculator', sub: 'LUK Key Generation', tabs: TABS_HCE, tab: 1, icon: 'clock', fields: [H('UDK', 32, { w: 6 }), T('Current Year (YY)', '26', { w: 2 }), T('Current Hours (HH)', '14', { w: 2 }), T('Hourly Counter', '03', { w: 2 })], button: 'Generate LUK', result: [['LUK', '16 bytes · limited-use key for the hour window'], ['Window', 'YY 26 · HH 14 · counter 03']] },
  'hce-msd': { title: 'Visa HCE Crypto Calculator', sub: 'MSD Cryptogram', tabs: TABS_HCE, tab: 2, icon: 'wifi-high', fields: [H('LUK_ATC', 36, { w: 6 }), S('MSD Device Type', 'Mobile phone')], button: 'Generate MSD Cryptogram', result: [['MSD', '4 bytes · dynamic CVV'], ['Mode', 'magstripe · from the LUK and ATC']] },
  'hce-qvsdc': { title: 'Visa HCE Crypto Calculator', sub: 'qVSDC Cryptogram', tabs: TABS_HCE, tab: 3, icon: 'wifi-high', fields: [H('LUK', 32, { w: 6 }), T('Amount', '000000001250', { tag: '9F02', w: 3 }), T('Amount, Other', '000000000000', { tag: '9F03', w: 3 }), T('Country Code', '0356', { tag: '9F1A', w: 2 }), T('Currency Code', '0356', { tag: '5F2A', w: 2 }), H('TVR', 10, { tag: '95', v: '0000008000', w: 2 }), T('Transaction Date', '260920', { tag: '9A', w: 2 }), T('Transaction Type', '00', { tag: '9C', w: 2 }), H('Unpredictable No.', 8, { tag: '9F37', w: 2 }), H('AIP', 4, { tag: '82', v: '2000', w: 2 }), H('ATC', 4, { tag: '9F36', v: '0031', w: 2 }), H('CVR', 8, { tag: '9F10', v: '03A48000', w: 2 })], button: 'Generate Cryptogram', result: [['ARQC', '8 bytes · CVN 18 cryptogram'], ['CVN', '18 · qVSDC · IAD built']] },
};

const TOOLS: HubTool[] = [
  { id: 'sda', stage: 0, icon: 'certificate', name: 'SDA Verification', desc: 'Verify issuer-signed static data against a CA public key.' },
  { id: 'dda', stage: 0, icon: 'fingerprint', name: 'DDA Verification', desc: 'Check the card’s signature over terminal-supplied dynamic data.' },
  { id: 'app-crypto', stage: 1, icon: 'calculator', name: 'EMV 4.1 Crypto Calculator', desc: 'The reference cryptogram chain: UDK, session keys, ARQC/TC/AAC, ARPC and key utilities.' },
  { id: 'emv42', stage: 1, icon: 'calculator', name: 'EMV 4.2 Crypto Calculator', desc: 'The same chain under the EMV 4.2 rules, derivation stated on the form.' },
  { id: 'mchip', stage: 1, icon: 'credit-card', name: 'MasterCard M/Chip Crypto', desc: 'M/Chip derivation, with a separate EMV 2000 session key tab.' },
  { id: 'vsdc', stage: 1, icon: 'credit-card', name: 'VSDC Crypto Calculator', desc: 'Visa Smart Debit/Credit: UDK, session keys, AAC/ARQC/TC and ARPC.' },
  { id: 'secure-msg', stage: 2, icon: 'envelope-simple', name: 'Secure Messaging', desc: 'Build and verify SMC / SMI protection on issuer scripts.' },
  { id: 'cap', stage: 2, icon: 'device-mobile', name: 'CAP Token Computation', desc: 'Compute Chip Authentication Programme tokens for banking 2FA.' },
  { id: 'hce', stage: 2, icon: 'wifi-high', name: 'HCE (Host Card Emulation)', desc: 'Generate the Limited-Use and Single-Use Keys HCE tokenisation needs.' },
];
const STAGES: HubStage[] = [['01', 'Offline authentication', 'SDA · DDA'], ['02', 'Cryptogram calculators', 'EMV 4.1 · 4.2 · M/Chip · VSDC'], ['03', 'After authorisation', 'Secure messaging · CAP · HCE']];

const SECTIONS: RawSection[] = [
  { id: 'sda', stage: 0, title: 'SDA Verification', intro: 'Static Data Authentication: the terminal verifies issuer-signed static data against a CA public key. The tool splits the job into the two steps the terminal performs — recover the issuer public key from its certificate, then use that key to check the signature over the card’s static data.', parts: [
    { h: 'Retrieve Issuer Public Key', panel: 'sda-retrieve', inputs: [['CA PK Modulus and CA PK Exponent', 'From the Visa / MasterCard CA hierarchy, for the index in tag `8F`.'], ['Issuer PK Certificate', 'Tag `90`.'], ['Issuer PK Remainder', 'Tag `92`; optional.'], ['Issuer PK Exponent', 'Tag `9F32`.']], button: 'Retrieve Key', note: 'Every field carries a live character count, and one with an odd number of hex characters is flagged before you run it — the usual cause of a certificate that will not recover.' },
    { h: 'Verify SSAD', panel: 'sda-verify', inputs: [['Issuer PK Modulus', 'The key recovered in the previous step.'], ['SSAD (Signed Static Data)', 'Tag `93`, built from the AFL records.']], button: 'Verify SSAD', note: 'The log reports the recovered hash alongside the pass/fail result, so a mismatch can be traced to the data rather than the key.' },
  ] },
  { id: 'dda', stage: 0, title: 'DDA Verification', intro: 'Dynamic Data Authentication: the card signs terminal-supplied dynamic data, proving it holds the ICC private key. Three tabs walk the chain down — issuer key, then card key, then the signature itself.', parts: [
    { h: null, panel: 'dda-issuer', caption: 'The Retrieve Issuer PK form filled in, beside the activity log' },
    { h: 'Retrieve ICC Public Key', panel: 'dda-icc', inputs: [['Issuer PK Modulus / Exponent', 'Recovered on the first tab.'], ['ICC PK Certificate', 'Tag `9F46`.'], ['ICC PK Remainder', 'Tag `9F48`; optional.'], ['ICC PK Exponent', 'Tag `9F47`.'], ['Static Data To Authenticate', 'The AFL-built record the certificate hash covers.'], ['AIP', 'Tag `82`, which the hash includes when the card asks for it.']], button: 'Retrieve ICC Key' },
    { h: 'Verify SDAD', panel: 'dda-verify', inputs: [['ICC PK Modulus / Exponent', 'From the previous tab.'], ['SDAD (Signed Dynamic Data)', 'Tag `9F4B`, returned by INTERNAL AUTHENTICATE.'], ['Dynamic Data', 'The terminal’s unpredictable number, e.g. from tag `9F37`.']], button: 'Verify Dynamic Signature' },
  ] },
  { id: 'app-crypto', stage: 1, title: 'EMV 4.1 Crypto Calculator', intro: 'The reference cryptogram calculator, and the one to learn the flow on: five tabs that follow the key hierarchy from the issuer master key down to the response cryptogram — UDK Derivation, Session Keys, Cryptogram, ARPC, Utilities. The scheme-specific calculators below are the same shape with their own derivations.', parts: [
    { h: 'UDK Derivation', panel: 'emv41-udk', inputs: [['Master Derivation Key (MDK)', '32 hex characters; the counter turns green at the right length.'], ['PAN and PAN Sequence', 'What binds the derived key to one card. The PAN is Luhn-checked, and a failure is a warning rather than a block: the banner reads *PAN Luhn checksum failed, but calculation will proceed*, which is what you want for test PANs.'], ['Derivation Option', '`OPTION_A` or `OPTION_B`.'], ['Key Parity', '`ODD`, `EVEN` or none.']], button: 'Calculate UDK' },
    { h: 'Session Keys', panel: 'emv41-sk', inputs: [['Master Key (UDK)', 'The card key from the previous tab.'], ['Initial Vector (IV)', '32 hex characters; zeros for the common case.'], ['ATC', 'The transaction counter the session key is diversified on.'], ['Branch Factor and Height', 'The EMV tree parameters, defaulting to 50 and 8.'], ['Key Parity', '']], button: 'Generate Session Key' },
    { h: 'Cryptogram', panel: 'emv41-ac', inputs: [['Session Key', 'From the Session Keys tab.'], ['Terminal Data', 'The CDOL-built terminal side of the request.'], ['ICC Data', 'AIP, ATC, CVR and the rest of the card side.'], ['Cryptogram Type', '`ARQC`, `TC` or `AAC`. The button label follows the selection.'], ['Padding Method', 'e.g. `METHOD_1_ISO_9797`.']] },
    { h: 'ARPC', panel: 'emv41-arpc', inputs: [['Session Key and Transaction Cryptogram', 'The ARQC the card produced.'], ['Response Code', 'The two-character ARC, e.g. `Y3`.'], ['ARPC Method', 'Method 1 or 2.']], button: 'Generate ARPC' },
    { h: 'Utilities', panel: 'emv41-util', lead: 'Key validation helpers that sit alongside the derivation tabs. Paste a **Key (Hex)** and **Calculate KCV** returns its check value — the quickest way to confirm the key you loaded is the key you meant.' },
  ] },
  { id: 'emv42', stage: 1, title: 'EMV 4.2 Crypto Calculator', intro: 'The same five tabs against the EMV 4.2 rules. The UDK tab states its derivation in the panel subtitle — EMV 4.2 Option A — and drops the separate parity selector, so the form is just the master key and the card identifiers.', parts: [
    { h: null, panel: 'emv42-udk', inputs: [['Master Derivation Key (MDK)', '32 characters.'], ['Primary Account Number (PAN)', ''], ['PAN Sequence Number', '']], button: 'Calculate UDK', note: 'Session key, cryptogram and ARPC follow on their own tabs exactly as in 4.1.' },
  ] },
  { id: 'mchip', stage: 1, title: 'MasterCard M/Chip Crypto Calculator', intro: 'M/Chip’s own derivation, with a fifth tab that 4.1 and 4.2 do not have: Session Key (EMV 2000), kept for cards personalised against the older scheme.', parts: [
    { h: null, panel: 'mchip-udk', inputs: [['MDK, PAN, PAN Sequence No.', ''], ['UDK Derivation Option', '`Option A` or `Option B`.'], ['Key Parity', '`NONE` by default here, unlike the EMV 4.1 tool.']], button: 'Generate UDK', note: 'The cryptogram tab is labelled AAC/ARQC/TC — one tab covering all three types.' },
  ] },
  { id: 'vsdc', stage: 1, title: 'VSDC Crypto Calculator', intro: 'Visa Smart Debit/Credit, in four tabs: UDK, Session Keys, AAC/ARQC/TC, ARPC.', parts: [
    { h: null, panel: 'vsdc-udk', inputs: [['MDK, PAN, PAN Sequence No.', ''], ['UDK Derivation Option', '`Option A` by default.'], ['Key Parity', '`Odd` by default.']], button: 'Generate UDK' },
  ] },
  { id: 'secure-msg', stage: 2, title: 'Secure Messaging', intro: 'Issuer scripts reach the card after authorisation, and Secure Messaging is what protects them: SMI for integrity, SMC for confidentiality. Three tabs cover the sequence — derive the session keys, build the encrypted PIN block a PIN-change script carries, then MAC the command.', parts: [
    { h: 'Session Key', panel: 'sm-sk', inputs: [['Input Key Type', '`MK` to start from the master keys.'], ['MK-SMI and MK-SMC', 'The integrity and confidentiality master keys, 32 characters each.'], ['Application Cryptogram (AC)', 'Diversifies the session keys onto this transaction.'], ['Command Number', 'Advances with each script command in the sequence.']], button: 'Generate Session Keys' },
    { h: 'PIN Block', panel: 'sm-pin', inputs: [['Output PIN Block Format', '`Standard EMV PIN Block` by default.'], ['SK-ENC', 'The encryption session key from the previous tab.'], ['New PIN', 'The value the script will set.']], button: 'Generate PIN Block' },
    { h: 'MAC', panel: 'sm-mac', lead: 'The MAC covers the command as the card will see it, so the form takes the APDU apart rather than asking for one hex blob:', inputs: [['SK-MAC', 'The integrity session key.'], ['Class, INS, P1, P2, Lc, Le', 'The APDU header and lengths.'], ['ARC and AC', 'Authorisation response code and application cryptogram.'], ['Payload', 'The script data itself.']] },
  ] },
  { id: 'cap', stage: 2, title: 'CAP Token Computation', intro: 'Computes the Chip Authentication Programme token some banks ask for as an online-banking second factor — the number a customer reads off a handheld reader and types into the website.', parts: [
    { h: null, panel: 'cap', inputs: [['IPB', 'Issuer Processing Base, the mask that selects which bits reach the token.'], ['IAF', 'Issuer Action Format.'], ['PAN + SN', 'PAN with its sequence number appended.'], ['CID', 'Cryptogram Information Data.'], ['ATC', 'Application Transaction Counter.'], ['AC', 'The application cryptogram the card generated.'], ['IAD', 'Issuer Application Data.']], button: 'Generate Token' },
  ] },
  { id: 'hce', stage: 2, title: 'HCE (Host Card Emulation)', intro: 'Host Card Emulation puts the card credential in a phone instead of a chip, so the key that signs a tap is short-lived by design. The Visa HCE calculator follows that chain across four tabs: card key, then the Limited-Use Key it produces, then the two contactless cryptograms an HCE wallet can present.', parts: [
    { h: null, panel: 'hce-udk', caption: 'The UDK form filled in with a master key, PAN and PAN sequence number, beside the activity log' },
    { h: 'LUK Key', panel: 'hce-luk', lead: 'The Limited-Use Key is bound to a point in time, which is what limits it:', inputs: [['UDK', 'The card key from the first tab.'], ['Current Year (YY) and Current Hours (HH)', 'The window the key belongs to.'], ['Hourly Counter', 'Which key within that hour.']], button: 'Generate LUK' },
    { h: 'MSD', panel: 'hce-msd', lead: 'Magnetic Stripe Data mode — the legacy contactless path, where the phone presents a dynamic value in a magstripe-shaped message.', inputs: [['LUK_ATC', 'The limited-use key with its counter.'], ['MSD Device Type', 'Drop-down.']] },
    { h: 'qVSDC', panel: 'hce-qvsdc', lead: 'The full contactless cryptogram. Each field is labelled with the EMV tag it comes from, so a capture can be transcribed straight in:', inputs: [['LUK', ''], ['Amount (9F02), Amount, Other (9F03)', ''], ['Country Code (9F1A), Currency Code (5F2A)', ''], ['TVR (95), AIP (82), CVR (from 9F10)', ''], ['Transaction Date (9A), Transaction Type (9C)', ''], ['Unpredictable No. (9F37), ATC (9F36)', '']] },
  ] },
];

const TIPS: string[] = [
  'For SDA / DDA work, double-check the CA public key index in tag `8F` matches the CA you supply — mismatched indices is the most common SDA failure.',
  'Cryptogram versions vary by issuer, and the CVN is encoded inside the IAD (tag `9F10`) — read it before picking a calculator, because the derivation differs.',
  'Work the tabs left to right. Each one consumes what the previous produced, and the activity log keeps every intermediate key so you can restart mid-chain rather than from the master key.',
];


/** A part, written out as the shell's blocks: heading, lead, panel, "Inputs" spec rows, closing line. */
function partBlocks(part: RawPart): ToolBlock[] {
  const blocks: ToolBlock[] = [];
  if (part.h) blocks.push({ t: 'h3', x: part.h });
  if (part.lead) blocks.push({ t: 'p', x: part.lead });
  blocks.push(panel(part.panel, part.caption));
  if (part.inputs) blocks.push({ t: 'label', x: 'Inputs' }, { t: 'specs', items: part.inputs });
  if (part.button || part.note) {
    blocks.push({ t: 'p', x: [part.button ? `Button: **${part.button}**.` : '', part.note ?? ''].filter(Boolean).join(' ') });
  }
  return blocks;
}

export const EMV_TOOLS_GUIDE: ToolGuideData = {
  slug: 'emv-tools',
  crumb: 'EMV Tools',
  meta: '9 tools · hex-driven',
  title: 'EMV & Card Tools',
  lede: 'The EMV & Card Tools group — offline authentication verifiers (SDA, DDA), cryptogram calculators for EMV 4.1, EMV 4.2, M/Chip and VSDC, issuer-script secure messaging, CAP tokens and HCE contactless keys. Each tool is hex-driven, with annotated outputs and an audit log.',
  hub: {
    title: 'EMV & Card Tools',
    sub: 'Smart card, EMV and contactless payment tools',
    tools: TOOLS,
    stages: STAGES,
    columns: 3,
    category: 1,
    search: 'Search EMV tools…',
    badge: '9 tools',
    label: 'In the order an EMV transaction reaches them',
    note: 'Every intermediate value stays visible.',
    aria: 'The EMV & Card Tools hub: tool categories, the three transaction stages and the nine calculators documented on this page',
  },
  panels: PANELS,
  panelBadge: 'EMV',
  captionFrom: 'sub',
  intro: {
    eyebrow: 'emv-tools · EMV & Card Tools hub — above',
    paras: [
      'The **EMV & Card Tools** group — smart card, EMV and contactless payment tools — is organised by the stage of a transaction it belongs to: offline authentication first (SDA, DDA), then the cryptogram calculators for each scheme, then the issuer-script and token tools that run after authorisation.',
      'Every calculator carries its own activity log on the right, so each intermediate value — derived key, session key, recovered certificate — is visible rather than just the final result.',
    ],
  },
  allTools: {
    lede: 'Every tool in this group, in the order an EMV transaction reaches them — each card links to the detailed reference below.',
    cards: TOOLS.map((tool) => ({ id: tool.id, icon: tool.icon, name: tool.name, desc: tool.desc, tag: stageTag(STAGES, tool.stage) })),
  },
  sections: SECTIONS.map((section) => ({
    id: section.id,
    title: section.title,
    icon: TOOLS.find((tool) => tool.id === section.id)?.icon || 'circle',
    eyebrow: stageTag(STAGES, section.stage),
    intro: section.intro,
    blocks: section.parts.flatMap(partBlocks),
  })),
  tips: TIPS,
  cta: {
    heading: 'Try it on your own transactions',
    text: 'Free and open source. Download the studio and run these calculators on your desk in minutes.',
  },
};
