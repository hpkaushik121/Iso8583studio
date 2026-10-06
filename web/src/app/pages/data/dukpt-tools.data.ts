/**
 * DUKPT Tools — page content. Imported only by pages/site/docs-dukpt-tools.ts.
 * Strings are GuideRich markdown.
 */
import { RefGuide, shot } from './docs-reference.data';

const ISO = 'DUKPT ISO 9797 (3DES)';
const AES = 'DUKPT AES';

export const DUKPT_TOOLS_GUIDE: RefGuide = {
  slug: 'dukpt-tools',
  crumb: 'DUKPT Tools',
  meta: 'Tool reference · 2 calculators · 9 tabs',
  title: 'DUKPT Tools',
  lede: 'Derived Unique Key Per Transaction (DUKPT) is the standard mechanism for protecting card data and PINs at the point of sale. ISO8583Studio includes calculators for both DUKPT AES (ANSI X9.24-3) and DUKPT ISO 9797 / 3DES (ANSI X9.24-1).',
  browse: 'iso9797-overview',
  sections: [
    {
      id: 'overview', title: 'What is DUKPT?', rail: 'What is DUKPT?', icon: 'book-open',
      intro: 'DUKPT generates a unique cryptographic key for every transaction without ever transmitting that key. The terminal stores a single Initial PIN Encryption Key (IPEK) derived from a Base Derivation Key (BDK) and a Key Serial Number (KSN). For each transaction the terminal advances the KSN counter and derives a fresh transaction key. The host, knowing only the BDK and the KSN it received, derives the same transaction key independently.',
      blocks: [
        { t: 'note', tone: 'blue', icon: 'info', title: 'Why use DUKPT?', x: 'If a terminal is compromised, only future transaction keys can be derived (forward secrecy is built in). Past transactions remain protected because the terminal never stored the keys it used.' },
      ],
    },
    {
      id: 'variants', title: 'DUKPT Variants', rail: 'Variants', icon: 'git-branch',
      blocks: [
        {
          t: 'table', cols: '150px 120px auto auto auto',
          head: ['Variant', 'Standard', 'Cipher', 'BDK Length', 'KSN Length'],
          rows: [
            ['**DUKPT ISO 9797**', 'ANSI X9.24-1', '3DES', '16 bytes (32 hex)', '10 bytes (20 hex)'],
            ['**DUKPT AES**', 'ANSI X9.24-3', 'AES-128 / 192 / 256', '16 / 24 / 32 bytes', '12 bytes (24 hex)'],
          ],
        },
        { t: 'p', x: 'If you’re working with legacy 3DES terminals, use the ISO 9797 variant. New deployments should use AES DUKPT.' },
      ],
    },
    {
      id: 'concepts', title: 'Key Concepts', rail: 'Key concepts', icon: 'key',
      blocks: [
        {
          t: 'bullets', items: [
            ['BDK (Base Derivation Key)', 'A master key shared between terminal manufacturer / acquirer and the host. Never used directly to encrypt data.'],
            ['KSN (Key Serial Number)', 'A unique identifier for a terminal + transaction counter. Increments with every transaction.'],
            ['IPEK (Initial PIN Encryption Key)', 'First key loaded into a terminal, derived from BDK + KSN.'],
            ['Transaction Key', 'The key actually used for a single transaction; derived from the IPEK and the current KSN counter.'],
            ['Working Keys', 'Purpose-bound keys (PIN, MAC, Data) derived by applying variant XOR masks to the transaction key.'],
          ],
        },
      ],
    },
    {
      id: 'iso9797-overview', title: 'DUKPT ISO 9797 (3DES) Tool', rail: 'ISO 9797 (3DES) tool', icon: 'tree-structure',
      intro: 'The DUKPT ISO 9797 calculator implements ANSI X9.24-1 with 3DES. It is split across **five tabs** — two derivation tabs that produce working keys, and three operation tabs that consume those keys.',
      blocks: [
        { t: 'h3', x: 'Tabs (5)' },
        {
          t: 'bullets', items: [
            ['PEK Derivation', 'Derive a PIN Encryption Key from BDK or IPEK + KSN.'],
            ['DEK Derivation', 'Derive a Data Encryption Key from BDK or IPEK + KSN.'],
            ['DUKPT PIN', 'Encrypt or decrypt a PIN block with a previously derived PEK.'],
            ['DUKPT MAC', 'Generate a MAC over hex data with a previously derived PEK / MAC key.'],
            ['DUKPT Data', 'Encrypt or decrypt arbitrary data with a previously derived key.'],
          ],
        },
        { t: 'note', tone: 'blue', icon: 'info', title: 'Two-step workflow', x: 'The tool deliberately separates derivation from use: derive the working key in *PEK Derivation* or *DEK Derivation* first, then paste the result into the *DUKPT PIN* / *MAC* / *Data* tab. This mirrors how a host-side stack stages keys.' },
      ],
    },
    {
      id: 'iso9797-pek', title: 'PEK Derivation Tab', rail: 'PEK Derivation', icon: 'password', eyebrow: ISO,
      blocks: [
        shot('dukpt-pek-derivation', 1600, 979, 1512, 'PEK Derivation Tab',
          'DUKPT Utilities on the PEK Derivation tab with BDK selected as the input key designation and empty BDK and KSN fields, beside the activity log'),
        { t: 'h3', x: 'Inputs' },
        {
          t: 'bullets', items: [
            ['Input Key Designation', 'Radio / toggle: `BDK` or `IPEK`. Determines which key field is shown.'],
            ['BDK (32 Hex Chars)', 'Visible when input is `BDK`.'],
            ['IPEK (32 Hex Chars)', 'Visible when input is `IPEK`.'],
            ['KSN (20 Hex Chars)', 'Always visible.'],
          ],
        },
        { t: 'p', x: 'Button: **Derive PEK**.' },
        { t: 'h3', x: 'Walk-through' },
        {
          t: 'steps', items: [
            '**Pick Input Key Designation** — `BDK` if you have the base key; `IPEK` if a previous step already produced the initial key.',
            '**Enter the key** in the resulting *BDK* or *IPEK* field (32 hex chars).',
            '**Enter KSN** — 20 hex chars (rightmost 21 bits are the transaction counter).',
            '**Click `Derive PEK`** — The activity log shows the resulting PIN Encryption Key. Copy it for the operation tabs.',
          ],
        },
      ],
    },
    {
      id: 'iso9797-dek', title: 'DEK Derivation Tab', rail: 'DEK Derivation', icon: 'database', eyebrow: ISO,
      intro: 'Identical fields and flow to *PEK Derivation* — the only difference is the variant applied to produce a Data Encryption Key.',
      blocks: [
        shot('dukpt-dek-derivation', 1508, 840, 754, 'DEK Derivation Tab',
          'DEK Derivation form with BDK selected as the input key designation, a 32-hex BDK field and a 20-hex KSN field above the Derive DEK button',
          [
            { t: 'h3', x: 'Inputs' },
            {
              t: 'bullets', items: [
                ['Input Key Designation', '`BDK` or `IPEK`.'],
                '**BDK (32 Hex Chars)** / **IPEK (32 Hex Chars)**.',
                '**KSN (20 Hex Chars)**.',
              ],
            },
            { t: 'p', x: 'Button: **Derive DEK**.' },
          ]),
      ],
    },
    {
      id: 'iso9797-pin', title: 'DUKPT PIN Tab', rail: 'DUKPT PIN', icon: 'numpad', eyebrow: ISO,
      intro: 'Encrypt or decrypt a PIN block using a working key you derived in *PEK Derivation*.',
      blocks: [
        shot('dukpt-pin', 1494, 692, 747, 'DUKPT PIN Tab',
          'DUKPT PIN form with a 32-hex PEK field and a 16-hex PIN Block field above Encrypt and Decrypt buttons',
          [
            { t: 'h3', x: 'Inputs' },
            {
              t: 'bullets', items: [
                ['PEK (32 Hex Chars)', 'Paste the PEK from the PEK Derivation tab.'],
                ['PIN Block (16 Hex Chars)', 'Clear (encrypt) or encrypted (decrypt) PIN block.'],
              ],
            },
            { t: 'p', x: 'Buttons: **Encrypt**, **Decrypt**.' },
          ]),
      ],
    },
    {
      id: 'iso9797-mac', title: 'DUKPT MAC Tab', rail: 'DUKPT MAC', icon: 'seal-check', eyebrow: ISO,
      intro: 'Compute a MAC over hex data with a previously derived working key.',
      blocks: [
        shot('dukpt-mac', 1494, 820, 747, 'DUKPT MAC Tab',
          'DUKPT MAC Generation form with a 32-hex PEK field, a DES / 3DES algorithm radio pair and a hex Data field above the Generate MAC button',
          [
            { t: 'h3', x: 'Inputs' },
            {
              t: 'bullets', items: [
                ['PEK (32 Hex Chars)', 'The MAC key (typically derived in the PEK Derivation tab).'],
                ['Algorithm', 'Radio: `DES` or `3DES`.'],
                ['Data (Hex)', 'Multi-line hex input.'],
              ],
            },
            { t: 'p', x: 'Button: **Generate MAC**.' },
          ]),
      ],
    },
    {
      id: 'iso9797-data', title: 'DUKPT Data Tab', rail: 'DUKPT Data', icon: 'lock-key', eyebrow: ISO,
      intro: 'Encrypt or decrypt sensitive data fields (track 2, EMV data) with a derived key.',
      blocks: [
        shot('dukpt-data', 1506, 1128, 753, 'DUKPT Data Tab',
          'DUKPT Data form with a 32-hex PEK field, a Use Data Variant Key switch, an ASCII data input type, a CBC / ECB cipher mode radio pair and a Data field above Encrypt and Decrypt buttons',
          [
            { t: 'h3', x: 'Inputs' },
            {
              t: 'bullets', items: [
                ['PEK (32 Hex Chars)', 'Working key from PEK Derivation (or DEK Derivation when the data variant key is required).'],
                ['Use Data Variant Key', 'Switch. When on, the tool applies the data-encryption variant XOR before encrypting.'],
                ['Data Input Type', '`ASCII` or `Hex`.'],
                ['Cipher Mode', 'Radio: `CBC` or `ECB`.'],
                ['Data', 'Multi-line input matching the chosen format.'],
              ],
            },
            { t: 'p', x: 'Buttons: **Encrypt**, **Decrypt**.' },
          ]),
      ],
    },
    {
      id: 'aes-overview', title: 'DUKPT AES Tool', rail: 'DUKPT AES tool', icon: 'shield-check',
      intro: 'Implements ANSI X9.24-3 with AES. Four tabs split derivation from operations.',
      blocks: [
        { t: 'h3', x: 'Tabs (4)' },
        {
          t: 'bullets', items: [
            ['Key Derivation', 'Derive a working key from BDK or IK + KSN.'],
            ['DUKPT PIN', 'PIN block encrypt / decrypt.'],
            ['DUKPT MAC', 'MAC generation.'],
            ['DUKPT Data', 'Data encrypt / decrypt.'],
          ],
        },
      ],
    },
    {
      id: 'aes-derive', title: 'Key Derivation Tab', rail: 'Key Derivation', icon: 'flow-arrow', eyebrow: AES,
      blocks: [
        shot('dukpt-aes-key-derivation', 1600, 976, 1510, 'Key Derivation Tab',
          'DUKPT AES Utilities on the Key Derivation tab with BDK as the input key designation, AES-128 initial and working key types, and BDK / IK and KSN fields, beside the activity log'),
        { t: 'h3', x: 'Inputs' },
        {
          t: 'bullets', items: [
            ['Input Key Designation', 'Radio / toggle: `BDK` or `IK`.'],
            ['Initial Key Type', 'Drop-down: `AES-128`, `AES-192`, `AES-256`.'],
            ['BDK / IK', 'Hex; length matches the initial key type.'],
            ['Working Key Type', 'Drop-down: `2TDEA`, `3TDEA`, `AES-128`, `AES-192`, `AES-256`.'],
            ['KSN', '24 hex chars (12 bytes: 4-byte BDK ID + 4-byte derivation ID + 4-byte counter).'],
          ],
        },
        { t: 'p', x: 'Button: **Derive Keys**.' },
        { t: 'h3', x: 'Walk-through' },
        {
          t: 'steps', items: [
            '**Pick Input Key Designation** — `BDK` or `IK`.',
            '**Pick Initial Key Type** — AES-128 / 192 / 256.',
            '**Enter the BDK / IK**.',
            '**Pick Working Key Type** — The tool can derive both AES and TDES working keys for backwards compatibility.',
            '**Enter KSN**.',
            '**Click `Derive Keys`** — The activity log lists the IK (when starting from BDK) and the working key.',
          ],
        },
        { t: 'note', tone: 'warn', icon: 'warning', title: 'Counter Field', x: 'The 32-bit counter only uses values with at most 16 set bits to allow efficient forward derivation. The tool flags invalid counters.' },
      ],
    },
    {
      id: 'ksn-format', title: 'KSN Structure', rail: 'KSN structure', icon: 'rows',
      blocks: [
        { t: 'h3', x: '3DES KSN (10 bytes)' },
        {
          t: 'code', lines: [
            '| 5-byte BDK ID + Device ID  |   2-byte counter (high)   | 21-bit Tx Counter |',
            '|--------------------------- |---------------------------|-------------------|',
            '|         59 bits            |          remaining         |     21 bits        |',
          ],
        },
        { t: 'h3', x: 'AES KSN (12 bytes)' },
        { t: 'code', lines: ['| 4-byte BDK ID | 4-byte Derivation ID | 4-byte Transaction Counter |'] },
        { t: 'p', x: 'Increment the counter by one for every transaction. After exhausting the counter space, the device must be re-keyed.' },
      ],
    },
    {
      id: 'tips', title: 'Tips & Pitfalls', rail: 'Tips & pitfalls', icon: 'lightbulb',
      tips: [
        'The KSN you receive in field 53 / 60 of an ISO 8583 message is what the host uses to derive the same key. Keep them in sync — off-by-one is the most common bug.',
        'For PIN translation tests, capture the PIN block at the same instant as the KSN. Re-using a KSN with a different PIN block will fail.',
        'If you suddenly start getting wrong MACs, check whether your terminal advanced the counter without you advancing yours. Use the increment button to re-sync.',
        'Keep BDKs out of source control. The activity log persists keys in memory during the session but never writes them to disk.',
      ],
    },
  ],
  cta: {
    heading: 'Try it on your own transactions',
    text: 'Free and open source. Download the studio and run this simulator on your desk in minutes.',
  },
};
