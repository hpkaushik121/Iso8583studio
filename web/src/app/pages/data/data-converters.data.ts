/**
 * Data Converters — page content and the specs its glass panels render from.
 * Ported from the prototype's data-converters-data.js; imported only by
 * pages/site/docs-utility-tools.ts.
 *
 * Panel values come from the seeded hx() helper, so they are the same on the
 * server and in the browser.
 */
import { A, GlassField, GlassSpec, HubStage, HubTool, S, T, grp } from '../shared/glass/glass-model';
import { ToolBlock, ToolGuideData, panel, stageTag, toFeatures } from '../shared/glass/tool-guide';

interface RawTool extends HubTool {
  badge?: 'Popular' | 'New';
}

interface RawSection {
  id: string;
  stage: number;
  title: string;
  intro?: string;
  blocks: ToolBlock[];
}

/** Hex field holding a given value (the converters show real encodings, not generated hex). */
const HV = (l: string, v: string, o: Partial<GlassField> = {}): GlassField =>
  ({ l, n: v.length, kind: 'hex', v: v.length > 8 ? grp(v) : v, w: 3, ...o });

const HELLO_HEX = '4865 6C6C 6F2C 2057 6F72 6C64 21';
const B64 = 'SGVsbG8sIFdvcmxkIQ==', B94 = '/P|?l:+>N>\\ftB[Z';
const T2 = { pan: '1234567890123456', exp: '2512', svc: '201', dd: '123456789' };
const T2_HEX = T2.pan + 'D' + T2.exp + T2.svc + T2.dd + 'F';
const base = { app: 'utility-tools' };

const PANELS: Record<string, GlassSpec> = {
  'b64-enc': { ...base, badge: 'Base64', title: 'Encode to Base64', sub: 'Encode', icon: 'arrow-right', hint: 'ASCII or hex in · Base64 out', fields: [S('Input Encoding', 'ASCII', { w: 2 }), A('Input Data', 'Hello, World!', { w: 4 })], button: 'Encode', result: [['Base64', B64], ['Bytes', '13 in · 20 chars out · 2 pad']] },
  'b64-dec': { ...base, badge: 'Base64', title: 'Decode from Base64', sub: 'Decode', icon: 'arrow-left', hint: 'result returned as hexadecimal', fields: [A('Base64 Data', B64)], button: 'Decode', result: [['Hex', HELLO_HEX], ['ASCII', 'Hello, World! · 13 bytes']] },
  'b94-enc': { ...base, badge: 'Base94', title: 'Encode to Base94', sub: 'Encode', icon: 'arrow-right', hint: 'printable ASCII 0x21–0x7E · denser than Base64', fields: [S('Input Encoding', 'ASCII', { w: 2 }), A('Input Data', 'Hello, World!', { w: 4 })], button: 'Encode', result: [['Base94', B94], ['Chars', '13 bytes → 16 chars']] },
  'b94-dec': { ...base, badge: 'Base94', title: 'Decode from Base94', sub: 'Decode', icon: 'arrow-left', hint: 'result returned as hexadecimal', fields: [A('Base94 Data', B94)], button: 'Decode', result: [['Hex', HELLO_HEX], ['ASCII', 'Hello, World! · 13 bytes']] },
  'bcd-enc': { ...base, badge: 'BCD', title: 'Encode to BCD', sub: 'Encode', icon: 'arrow-right', hint: 'two digits per byte · odd count padded', fields: [T('Decimal Data', '1234567890', { w: 6 })], button: 'Encode', result: [['BCD', '12 34 56 78 90'], ['Bytes', '10 digits → 5 bytes']] },
  'bcd-dec': { ...base, badge: 'BCD', title: 'Decode from BCD', sub: 'Decode', icon: 'arrow-left', hint: 'packed hex in · decimal string out', fields: [S('Input Format', 'Hexadecimal', { w: 2 }), HV('BCD Data', '1234567890', { w: 4 })], button: 'Decode', result: [['Decimal', '1234567890'], ['Bytes', '5 bytes → 10 digits']] },
  encoding: { ...base, badge: 'Encoder', title: 'Encoding Converter', sub: 'Binary → Hexadecimal', icon: 'arrows-left-right', hint: 'source format set by the conversion type', fields: [S('Conversion Type', 'Binary -> Hexadecimal', { w: 3 }), A('Input Data', '01001000 01101001', { w: 3 })], button: 'Convert', result: [['Hexadecimal', '4869'], ['ASCII', 'Hi · 2 bytes']] },
  'check-digit': { ...base, badge: 'Luhn', title: 'Check Digit Calculator', sub: 'Luhn (Mod 10)', icon: 'check-circle', hint: 'include the check digit to validate · omit it to generate', fields: [T('Input Number', '49927398716', { w: 4 }), S('Algorithm', 'Luhn (Mod 10)', { w: 2 })], button: 'Validate', alt: 'Generate', result: [['Luhn', 'VALID · check digit 6'], ['Sum', '70 · 70 mod 10 = 0']] },
  't2-enc': { ...base, badge: 'Track 2', title: 'Encode Track 2', sub: 'Encode', icon: 'play', hint: 'D separates the fields · F pads to a whole byte', fields: [T('PAN (8-19 digits)', T2.pan, { w: 3 }), T('Expiry YYMM (4 digits)', T2.exp, { w: 3 }), T('Service Code (3 digits)', T2.svc, { w: 2 }), T('Discretionary Data (digits)', T2.dd, { w: 2, opt: true }), S('Output Format', 'BCD/Hex (EMV Tag 57)', { w: 2 })], button: 'Encode', result: [['Tag 57', grp(T2_HEX)], ['Length', '33 nibbles + F pad → 17 bytes']] },
  't2-dec': { ...base, badge: 'Track 2', title: 'Decode Track 2', sub: 'Decode', icon: 'play', hint: 'ASCII raw, ASCII without sentinels or BCD/hex — detected, not asked', fields: [A('Track 2 Data (any format)', ';' + T2.pan + '=' + T2.exp + T2.svc + T2.dd + '?')], button: 'Decode', result: [['Format', 'ASCII raw · ; and ? sentinels'], ['PAN', T2.pan], ['Expiry', T2.exp + ' · 12/2025'], ['Service code', T2.svc + ' · international · IC preferred'], ['Discretionary', T2.dd]] },
};

const TOOLS: RawTool[] = [
  { id: 'base64', stage: 0, icon: 'code', badge: 'Popular', name: 'Base64 Encoder', desc: 'Encode bytes to Base64 or decode a Base64 string back to hex.' },
  { id: 'base94', stage: 0, icon: 'brackets-angle', name: 'Base94 Encoder', desc: 'The denser printable-ASCII variant, encoded and decoded the same way.' },
  { id: 'bcd', stage: 1, icon: 'hash', badge: 'Popular', name: 'BCD Converter', desc: 'Pack a decimal string two digits per byte, or unpack one.' },
  { id: 'encoding', stage: 1, icon: 'text-t', name: 'Character Encoder', desc: 'Convert between binary, hexadecimal, decimal and ASCII.' },
  { id: 'check-digit', stage: 2, icon: 'check-circle', badge: 'Popular', name: 'Check Digit Calculator', desc: 'Generate or validate a Luhn (Mod 10) check digit.' },
  { id: 'track2', stage: 2, icon: 'credit-card', badge: 'New', name: 'Track 2 Codec', desc: 'Build Track 2 from its parts, or decode any of its three shapes.' },
];
const STAGES: HubStage[] = [['01', 'Text-safe encodings', 'Base64 · Base94'], ['02', 'Wire representations', 'BCD · binary · hex · ASCII'], ['03', 'Card data', 'Luhn · Track 2 / tag 57']];
const SHAPES: [string, string, string][] = [
  ['brackets-curly', 'Base64 on an API', 'Text-safe bytes crossing a JSON or XML boundary.'],
  ['hash', 'BCD in an ISO 8583 field', 'Two digits per byte, the way numeric fields and EMV amounts travel on the wire.'],
  ['credit-card', 'Packed hex in EMV tag 57', 'Track 2 equivalent data, D-separated and F-padded to a whole byte.'],
  ['check-circle', 'Check digits', 'The Luhn digit that rejects a wrong PAN before any cryptography runs.'],
];

const SECTIONS: RawSection[] = [
  { id: 'base64', stage: 0, title: 'Base64 Encoder', intro: 'Base64 turns arbitrary bytes into a text-safe alphabet — the form payment data usually takes when it crosses a JSON or XML boundary. Encode and decode sit side by side.', blocks: [
    panel('b64-enc', 'Encode to Base64 with an ASCII input encoding drop-down and an Input Data field above the Encode button'),
    panel('b64-dec', 'Decode from Base64 with a single Base64 Data field above the Decode button'),
    { t: 'h3', x: 'Inputs' },
    { t: 'bullets', items: [['Input Encoding', 'Drop-down: `ASCII` or hexadecimal. It tells the encoder how to read what you paste, so a hex key and a plain string both work.'], ['Input Data', 'The bytes to encode, in the format above.'], ['Base64 Data', 'On the decode side; the result comes back as hexadecimal.']] },
  ] },
  { id: 'base94', stage: 0, title: 'Base94 Encoder', intro: 'Base94 packs bytes into the full printable ASCII range, so it fits more data into the same number of characters than Base64. Some key-injection and terminal-management protocols use it for exactly that reason.', blocks: [
    panel('b94-enc', 'Encode to Base94 with an ASCII input encoding drop-down and an Input Data field above the Encode button'),
    panel('b94-dec', 'Decode from Base94 with a single Base94 Data field above the Decode button'),
    { t: 'p', x: 'The fields mirror the Base64 tool exactly — an **Input Encoding** drop-down and **Input Data** going in, **Base94 Data** coming back, decoded to hexadecimal.' },
  ] },
  { id: 'bcd', stage: 1, title: 'BCD Converter', intro: 'Binary Coded Decimal stores two digits per byte, which is how numeric ISO 8583 fields and EMV amounts are carried on the wire. The converter goes both ways between a decimal string and its packed form.', blocks: [
    panel('bcd-enc', 'Encode to BCD with a Decimal Data field holding 1234567890 above the Encode button'),
    panel('bcd-dec', 'Decode from BCD with a Hexadecimal input format drop-down and a BCD Data field holding 1234567890 above the Decode button'),
    { t: 'h3', x: 'Inputs' },
    { t: 'bullets', items: [['Decimal Data', 'Digits to pack. An odd digit count is padded to a whole byte.'], ['Input Format', 'On the decode side: `Hexadecimal` by default.'], ['BCD Data', 'The packed value to unpack.']] },
  ] },
  { id: 'encoding', stage: 1, title: 'Character Encoder', intro: 'A general conversion bench for the representations that are not a payment format in their own right — binary, hexadecimal, decimal and ASCII. One drop-down picks the direction.', blocks: [
    panel('encoding', 'Encoding Converter with a Conversion Type drop-down set to Binary to Hexadecimal and an Input Data field above the Convert button'),
    { t: 'h3', x: 'Inputs' },
    { t: 'bullets', items: [['Conversion Type', 'Drop-down naming both ends of the conversion, e.g. `Binary -> Hexadecimal`. Pick the pair and the tool applies it in that direction.'], ['Input Data', 'The value in the source format.']] },
    { t: 'p', x: 'Button: **Convert**.' },
  ] },
  { id: 'check-digit', stage: 2, title: 'Check Digit Calculator', intro: 'The trailing digit on a PAN is a Luhn checksum, and a wrong one is rejected before any cryptography runs. This tool computes it, or checks the one you already have.', blocks: [
    panel('check-digit', 'Check Digit Calculator with an Input Number of 49927398716, the Luhn (Mod 10) algorithm selected, and Validate and Generate buttons'),
    { t: 'h3', x: 'Inputs' },
    { t: 'bullets', items: [['Input Number', 'The digits to check. For validation, include the check digit; to generate one, leave it off.'], ['Algorithm', 'Drop-down; `Luhn (Mod 10)` is the default and the one card numbers use.']] },
    { t: 'p', x: 'Buttons: **Validate**, **Generate**.' },
  ] },
  { id: 'track2', stage: 2, title: 'Track 2 Codec', intro: 'Track 2 is the magstripe-equivalent record that also travels in EMV tag `57`, and it appears in at least three shapes depending on where you captured it. The codec builds one from its parts, or takes any of those shapes apart.', blocks: [
    { t: 'h3', x: 'Encode' },
    panel('t2-enc', 'Encode Track 2 with PAN, expiry YYMM, service code and discretionary data fields, a BCD/Hex (EMV Tag 57) output format, and an Encode button'),
    { t: 'h4', x: 'Inputs' },
    { t: 'bullets', items: [['PAN', '8–19 digits.'], ['Expiry YYMM', '4 digits.'], ['Service Code', '3 digits.'], ['Discretionary Data', 'Digits; optional.'], ['Output Format', 'Drop-down; `BCD/Hex (EMV Tag 57)` for chip data, or the ASCII magstripe forms.']] },
    { t: 'h3', x: 'Decode' },
    panel('t2-dec', 'Decode Track 2 with a Track 2 Data field that accepts any format, worked examples of the three accepted shapes, and a Decode button'),
    { t: 'p', x: 'Paste the record in **any** of its shapes — the tool detects which one it is rather than asking you. The form lists all three:' },
    { t: 'bullets', items: [['ASCII raw', 'with the `;` start sentinel and `?` end sentinel.'], ['ASCII, no sentinels', 'PAN, `=`, then the rest.'], ['BCD / hex (Tag 57)', 'packed, with `D` as the field separator and a trailing `F` pad.']] },
    { t: 'code', lines: ['ASCII raw:          ;' + T2.pan + '=' + T2.exp + T2.svc + T2.dd + '?', 'ASCII no sentinels: ' + T2.pan + '=' + T2.exp + T2.svc + T2.dd, 'BCD/Hex (Tag 57):   ' + T2_HEX] },
  ] },
];

const TIPS: string[] = [
  'When a field will not parse, check its representation before its value — a PAN that looks wrong is often BCD read as ASCII, or the other way round.',
  'Round-trip anything you are unsure of: encode, then decode the result. If you do not land back on the input, the format assumption is what is wrong.',
  'The Track 2 decoder accepts all three shapes, so paste a capture straight in rather than converting it by hand first.',
  'The activity logs in each tool persist until you clear them — useful for capturing a sequence of intermediate values to share with a vendor support ticket.',
];


export const DATA_CONVERTERS_GUIDE: ToolGuideData = {
  slug: 'utility-tools',
  crumb: 'Data Converters',
  meta: '6 tools · both directions · hex & ASCII',
  title: 'Data Converters',
  lede: 'Format conversion and encoding utilities — Base64, Base94, BCD, character encoding, check digits and the Track 2 codec. Each one works in both directions, on the representations payment data actually arrives in.',
  hub: {
    title: 'Data Converters',
    sub: 'Format conversion and encoding utilities',
    tools: TOOLS,
    stages: STAGES,
    columns: 3,
    category: 5,
    search: 'Search converters…',
    badge: '6 tools',
    label: 'Every tool is a two-direction pair',
    note: 'Encode, then decode the result to prove the format.',
    aria: 'The Data Converters hub: tool categories, three format groups and the six converters in the group',
  },
  panels: PANELS,
  captionFrom: 'title',
  intro: {
    eyebrow: 'utility-tools · Data Converters hub — above',
    paras: [
      'The converters live under `Tools → Data Converters` and handle format conversion and encoding: the representations payment data arrives in — Base64 on an API, BCD in an ISO 8583 field, packed hex in EMV tag `57` — and the check digits that guard them. Every tool is a two-direction pair: one card encodes, the other decodes.',
    ],
    features: toFeatures(SHAPES),
  },
  allTools: {
    lede: 'Every converter in this category — each card links to the detailed reference below.',
    cards: TOOLS.map((tool) => ({
      id: tool.id, icon: tool.icon, name: tool.name, desc: tool.desc, tag: stageTag(STAGES, tool.stage),
      badge: tool.badge, badgeTone: tool.badge === 'New' ? 'blue' : 'teal',
    })),
  },
  sections: SECTIONS.map((section) => ({
    id: section.id,
    title: section.title,
    icon: TOOLS.find((tool) => tool.id === section.id)?.icon || 'circle',
    eyebrow: stageTag(STAGES, section.stage),
    intro: section.intro,
    blocks: section.blocks,
  })),
  tips: TIPS,
  cta: {
    heading: 'Try it on your own captures',
    text: 'Free and open source. Download the studio and run these converters on your desk in minutes.',
  },
};
