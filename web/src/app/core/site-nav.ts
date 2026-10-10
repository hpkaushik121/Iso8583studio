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
  /** Phosphor icon shown in the menu tile and the drawer. */
  icon?: string;
  /** Status or count chip, e.g. 'Available', 'Beta', '12'. */
  chip?: string;
}

export interface NavGroup {
  label: string;
  mega: boolean;
  items: NavLink[];
}

export const SIMULATORS: NavLink[] = [
  { label: 'Host Simulator', link: '/simulator/host', icon: 'arrows-left-right', chip: 'Available', desc: 'Acquirer / issuer host, proxy' },
  { label: 'HSM Simulator', link: '/simulator/hsm', icon: 'key', chip: 'Available', desc: 'payShield 10K keys, PIN, MAC' },
  { label: 'HSM Command Console', link: '/simulator/hsm-command-console', icon: 'terminal-window', chip: 'Beta', desc: 'Host-command client' },
  { label: 'POS Simulator', link: '/simulator/pos', icon: 'credit-card', chip: 'Beta', desc: 'Terminal, EMV & contactless' },
  { label: 'APDU Simulator', link: '/simulator/apdu', icon: 'sim-card', chip: 'Beta', desc: 'Card session & TLV' },
  { label: 'Switch Simulator', link: '/simulator/payment-switch', icon: 'shuffle', chip: 'Dev', desc: 'Routing & translation' },
  { label: 'Issuer System', link: '/simulator/issuer', icon: 'bank', chip: 'Dev', desc: 'Authorization decisioning' },
  { label: 'ATM Simulator', link: '/simulator/atm', icon: 'money', chip: 'Dev', desc: 'Cash withdrawal, NDC/DDC' },
  { label: 'ECR Simulator', link: '/simulator/ecr', icon: 'printer', chip: 'Dev', desc: 'Register ↔ POS integration' },
];

export const TOOLS: NavLink[] = [
  { label: 'Payment Simulators', link: '/simulator', icon: 'plugs-connected', chip: '9', desc: 'Host, HSM, POS, ATM, switch & scheme' },
  { label: 'EMV & Card Tools', link: '/tools/emv-tools', icon: 'cards', chip: '9', desc: 'Cryptograms, SDA/DDA, CAP, HCE' },
  { label: 'Cryptographic Tools', link: '/tools/cipher-tools', icon: 'lock-key', chip: '7', desc: 'AES, DES/3DES, RSA, FPE, hashing' },
  { label: 'Key Management', link: '/tools/key-tools', icon: 'key', chip: '9', desc: 'DUKPT, TR-31, shares, Thales, Atalla' },
  { label: 'Payment Utilities', link: '/tools/pin-tools', icon: 'keyboard', chip: '21', desc: 'PIN blocks, PVV, MAC, parsing' },
  { label: 'Data Converters', link: '/tools/utility-tools', icon: 'swap', chip: '6', desc: 'Base64, Base94, BCD, check digits, Track 2' },
  // Previously absent from both nav and footer despite being live pages.
  { label: 'Card Validation', link: '/tools/card-validation', icon: 'identification-card', chip: '4', desc: 'PAN, Luhn, IIN and check digits' },
  { label: 'DUKPT Tools', link: '/tools/dukpt-tools', icon: 'tree-structure', chip: '6', desc: 'BDK, IPEK and transaction keys' },
  { label: 'MAC Tools', link: '/tools/mac-tools', icon: 'seal-check', chip: '5', desc: 'ISO 9797, X9.9/X9.19, retail MAC' },
];

export const SOLUTIONS: NavLink[] = [
  { label: 'EMV Certification', link: '/emv-certification', icon: 'check-circle', desc: 'L1/L2/L3 & scheme certification' },
  { label: 'Cloud Simulators', link: '/cloud-simulators', icon: 'cloud', desc: 'Hosted test endpoints for CI' },
  { label: 'Payment Middleware', link: '/middleware', icon: 'arrows-left-right', desc: 'Switching, routing, translation' },
  { label: 'Kernel Development', link: '/kernel', icon: 'cpu', desc: 'EMV L2 kernel engineering' },
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
  // The site's own download page: installers for Windows and macOS at pinned
  // URLs, build instructions for Linux. The GitHub releases/latest URL this
  // used to point at does not resolve (the only release is a pre-release), so
  // every Download CTA was landing on a bare release list.
  releases: '/download',
  roadmap: 'https://github.com/users/hpkaushik121/projects/1',
  linkedin: 'https://www.linkedin.com/company/iso8583-studio',
  github: 'https://github.com/hpkaushik121',
  medium: 'https://medium.com/@iso8583.studio',
} as const;
