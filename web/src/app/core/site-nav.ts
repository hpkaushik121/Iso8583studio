/**
 * The navigation model. Header, mobile menu and footer all read from here, so
 * a link can never exist in one and be missing from another — which is how
 * card-validation, dukpt-tools and mac-tools previously fell out of the nav
 * while still being live pages.
 */

export interface NavLink {
  label: string;
  link: string;
  /** Short descriptor shown in the mega-menus. */
  desc?: string;
  /** Glyph shown in the icon tile. */
  glyph?: string;
  /** Status or count chip, e.g. 'Available', 'Beta', '12'. */
  chip?: string;
}

export interface NavGroup {
  label: string;
  mega: boolean;
  items: NavLink[];
}

export const SIMULATORS: NavLink[] = [
  { label: 'Host Simulator', link: '/docs/host-simulator', glyph: '⇄', chip: 'Available', desc: 'Acquirer / issuer host, proxy' },
  { label: 'HSM Simulator', link: '/docs/hsm-simulator', glyph: '⚿', chip: 'Available', desc: 'payShield 10K keys, PIN, MAC' },
  { label: 'HSM Command Console', link: '/docs/hsm-command-console', glyph: '›_', chip: 'Beta', desc: 'Host-command client' },
  { label: 'POS Simulator', link: '/docs/pos-simulator', glyph: '▤', chip: 'Available', desc: 'Terminal, EMV & contactless' },
  { label: 'APDU Simulator', link: '/docs/apdu-simulator', glyph: '▣', chip: 'Available', desc: 'Card session & TLV' },
  { label: 'Switch Simulator', link: '/docs/payment-switch', glyph: '⇆', chip: 'Dev', desc: 'Routing & translation' },
  { label: 'Issuer System', link: '/docs/issuer-simulator', glyph: '◈', chip: 'Dev', desc: 'Authorization decisioning' },
  { label: 'ATM Simulator', link: '/docs/atm-simulator', glyph: '▧', chip: 'Dev', desc: 'Cash withdrawal, NDC/DDC' },
  { label: 'ECR Simulator', link: '/docs/ecr-simulator', glyph: '▦', chip: 'Dev', desc: 'Register ↔ POS integration' },
];

export const TOOLS: NavLink[] = [
  { label: 'Payment Simulators', link: '/docs/payment-simulators', glyph: '⇄', chip: '9', desc: 'Host, HSM, POS, ATM, switch & scheme' },
  { label: 'EMV & Card Tools', link: '/docs/emv-tools', glyph: '▣', chip: '12', desc: 'Cryptograms, SDA/DDA, ATR, tags, CVV' },
  { label: 'Cryptographic Tools', link: '/docs/cipher-tools', glyph: '⬡', chip: '7', desc: 'AES, DES/3DES, RSA, FPE, hashing' },
  { label: 'Key Management', link: '/docs/key-tools', glyph: '⚿', chip: '10', desc: 'DUKPT, TR-31, shares, Thales, Futurex' },
  { label: 'Payment Utilities', link: '/docs/pin-tools', glyph: '▤', chip: '21', desc: 'PIN blocks, PVV, MAC, parsing' },
  { label: 'Data Converters', link: '/docs/utility-tools', glyph: '⇋', chip: '5', desc: 'Base64, hex, EBCDIC, BCD' },
  // Previously absent from both nav and footer despite being live pages.
  { label: 'Card Validation', link: '/docs/card-validation', glyph: '◫', chip: '4', desc: 'PAN, Luhn, IIN and check digits' },
  { label: 'DUKPT Tools', link: '/docs/dukpt-tools', glyph: '⚙', chip: '6', desc: 'BDK, IPEK and transaction keys' },
  { label: 'MAC Tools', link: '/docs/mac-tools', glyph: '⛊', chip: '5', desc: 'ISO 9797, X9.9/X9.19, retail MAC' },
];

export const SOLUTIONS: NavLink[] = [
  { label: 'EMV Certification', link: '/emv-certification', glyph: '✓', desc: 'L1/L2/L3 & scheme certification' },
  { label: 'Cloud Simulators', link: '/cloud-simulators', glyph: '☁', desc: 'Hosted test endpoints for CI' },
  { label: 'Payment Middleware', link: '/middleware', glyph: '⇄', desc: 'Switching, routing, translation' },
  { label: 'Kernel Development', link: '/kernel', glyph: '▦', desc: 'EMV L2 kernel engineering' },
];

export const RESOURCES: NavLink[] = [
  { label: 'Documentation', link: '/docs' },
  { label: 'Installation', link: '/docs/installation' },
  { label: 'Versions', link: '/docs/versions' },
  { label: 'Contribute', link: '/docs/contributing' },
  { label: 'Contact', link: '/contact' },
  { label: 'Blog', link: '/blogs' },
  { label: 'ISO8583Studio Pro', link: '/pro' },
];

export const LEGAL: NavLink[] = [
  { label: 'Privacy Policy', link: '/privacy-policy' },
  { label: 'Terms & Conditions', link: '/terms-and-conditions' },
];

export const NAV_GROUPS: NavGroup[] = [
  { label: 'Simulators', mega: true, items: SIMULATORS },
  { label: 'Tools', mega: true, items: TOOLS },
  { label: 'Solutions', mega: false, items: SOLUTIONS },
];

export const EXTERNAL = {
  repo: 'https://github.com/hpkaushik121/Iso8583studio',
  releases: 'https://github.com/hpkaushik121/Iso8583studio/releases/latest',
  roadmap: 'https://github.com/users/hpkaushik121/projects/1',
  linkedin: 'https://www.linkedin.com/company/iso8583-studio',
  github: 'https://github.com/hpkaushik121',
  medium: 'https://medium.com/@iso8583.studio',
} as const;
