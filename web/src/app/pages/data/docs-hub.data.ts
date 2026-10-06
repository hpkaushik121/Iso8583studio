/**
 * The documentation hub's index: every guide, reference and resource the
 * /docs page links to. Icon names are Phosphor slugs; they are written here
 * as quoted literals so the sprite builder picks them up.
 *
 * Titles and badges are what analytics reports for hub_card_click, so they
 * stay as the previous hub had them.
 */

export type HubStatus = 'Available' | 'Beta' | 'In development';

export interface HubStart {
  icon: string;
  title: string;
  body: string;
  href: string;
  cta: string;
  steps?: readonly string[];
}

export interface HubGuide { icon: string; title: string; desc: string; status: HubStatus; href: string; }

/** `count` is the number of tools the reference covers, where one is published. */
export interface HubTool { icon: string; title: string; count: number | null; desc: string; href: string; }

export interface HubSolution { icon: string; title: string; desc: string; href: string; }

export interface HubResource { icon: string; title: string; desc: string; href: string; }

export const HUB_START: readonly HubStart[] = [
  {
    icon: 'download-simple',
    title: 'Install the studio',
    body: 'One cross-platform JAR for Windows, macOS and Linux on a Java 11+ runtime, or build it yourself from the Kotlin Multiplatform sources.',
    href: '/docs/installation',
    cta: 'Installation guide',
    steps: ['Install a Java 11+ runtime', 'Download ISO8583Studio.jar from the latest release', 'Run it, open a simulator and start a session'],
  },
  {
    icon: 'arrows-left-right',
    title: 'Run your first host session',
    body: 'Configure a gateway as server, client or proxy, then answer a 0200 with a 0210 from the built-in host.',
    href: '/simulator/host',
    cta: 'Host Simulator guide',
  },
  {
    icon: 'tag',
    title: 'Check the release',
    body: 'v1.0.14 is current. Release history, the release channel and upgrade notes.',
    href: '/docs/versions',
    cta: 'Versions',
  },
];

export const HUB_SIMULATORS: readonly HubGuide[] = [
  { icon: 'arrows-left-right', title: 'Host Simulator', status: 'Available', href: '/simulator/host', desc: 'Acquirer and issuer host responses: Server, Client or Proxy over TCP/IP, REST, RS232 and dial-up.' },
  { icon: 'key', title: 'HSM Simulator', status: 'Available', href: '/simulator/hsm', desc: 'Thales payShield 10K emulation: host commands, LMK storage, keys, PIN and MAC.' },
  { icon: 'terminal-window', title: 'HSM Command Console', status: 'Beta', href: '/simulator/hsm-command-console', desc: 'Host-command client for Thales, Futurex, Luna, Utimaco and nCipher: console, scenarios, load tests.' },
  { icon: 'credit-card', title: 'POS Simulator', status: 'Beta', href: '/simulator/pos', desc: 'A configurable point-of-sale terminal, with hardware, EMV and contactless, driving ISO 8583 to a host.' },
  { icon: 'sim-card', title: 'APDU Simulator', status: 'Beta', href: '/simulator/apdu', desc: 'EMV smart-card sessions: APDU exchange, TLV parsing, flow analysis and test plans.' },
  { icon: 'shuffle', title: 'Switch Simulator', status: 'In development', href: '/simulator/payment-switch', desc: 'BIN routing, protocol translation and stand-in between acquirers and issuers.' },
  { icon: 'bank', title: 'Issuer System', status: 'In development', href: '/simulator/issuer', desc: 'Issuer-side authorization: PIN and ARQC verification, limits, 0210 decisioning.' },
  { icon: 'money', title: 'ATM Simulator', status: 'In development', href: '/simulator/atm', desc: 'Cash withdrawal, balance and PIN-change flows with NDC/DDC device states.' },
  { icon: 'printer', title: 'ECR Simulator', status: 'In development', href: '/simulator/ecr', desc: 'Electronic cash register driving sale, void and refund to a payment terminal.' },
];

export const HUB_TOOLS: readonly HubTool[] = [
  { icon: 'cards', title: 'EMV & Card Tools', count: 12, href: '/tools/emv-tools', desc: 'Cryptograms (ARQC/TC), SDA/DDA, ATR parsing, tag dictionary, CAP tokens, secure messaging.' },
  { icon: 'lock-key', title: 'Cryptographic Tools', count: 7, href: '/tools/cipher-tools', desc: 'AES, DES/3DES, RSA, Thales RSA, ECDSA and format-preserving encryption calculators.' },
  { icon: 'key', title: 'Key Management', count: 10, href: '/tools/key-tools', desc: 'DEA keys, key shares, SSL certificates, Atalla and Futurex key calculators.' },
  { icon: 'keyboard', title: 'Payment Utilities', count: 21, href: '/tools/pin-tools', desc: 'PIN blocks (ISO 9564 and OEM), AES PIN blocks, TPK-to-ZPK translation and DUKPT PIN encryption.' },
  { icon: 'swap', title: 'Data Converters', count: 6, href: '/tools/utility-tools', desc: 'Base64, Base94, BCD, character encoding, check digits and the Track 2 codec.' },
  { icon: 'gear', title: 'DUKPT Tools', count: null, href: '/tools/dukpt-tools', desc: 'Key derivation per ANSI X9.24: IPEK, KSN walks and transaction keys.' },
  { icon: 'shield-check', title: 'MAC Tools', count: null, href: '/tools/mac-tools', desc: 'Message authentication: ISO 9797 algorithms, retail MAC and HMAC.' },
  { icon: 'seal-check', title: 'Card Validation', count: null, href: '/tools/card-validation', desc: 'PAN validation, CVV/CVC computation and card-number utilities.' },
];

export const HUB_SOLUTIONS: readonly HubSolution[] = [
  { icon: 'check-circle', title: 'EMV Certification', href: '/emv-certification', desc: 'L1 / L2 / L3 and scheme certification, from gap analysis to lab sign-off.' },
  { icon: 'cloud', title: 'Cloud Simulators', href: '/cloud-simulators', desc: 'Hosted host and HSM endpoints for CI pipelines and distributed teams.' },
  { icon: 'arrows-left-right', title: 'Payment Middleware', href: '/middleware', desc: 'Switching, routing and protocol translation built on the Studio engine.' },
  { icon: 'cpu', title: 'Kernel Development', href: '/kernel', desc: 'EMV L2 kernel engineering for terminals, contact to contactless.' },
];

export const HUB_RESOURCES: readonly HubResource[] = [
  { icon: 'download-simple', title: 'Installation', href: '/docs/installation', desc: 'Prerequisites and install steps for Windows, macOS and Linux, plus building from source.' },
  { icon: 'tag', title: 'Versions', href: '/docs/versions', desc: 'Current release (v1.0.14), the release channel and upgrade notes.' },
  { icon: 'git-pull-request', title: 'How to Contribute', href: '/docs/contributing', desc: 'Kotlin Multiplatform dev setup, project layout, code style and the PR flow.' },
  { icon: 'envelope-simple', title: 'Contact', href: '/contact', desc: 'Email, issues, discussions and LinkedIn.' },
  { icon: 'article', title: 'Blog', href: '/blogs', desc: 'Deep dives on ISO 8583, EMV and payment cryptography.' },
  { icon: 'github-logo', title: 'GitHub', href: 'https://github.com/hpkaushik121/Iso8583studio', desc: 'Source code, issues and discussions. AGPL v3-licensed and open source.' },
  { icon: 'package', title: 'Downloads', href: 'https://github.com/hpkaushik121/Iso8583studio/releases', desc: 'Latest releases for Windows, macOS and Linux.' },
  { icon: 'map-trifold', title: 'Roadmap', href: 'https://github.com/users/hpkaushik121/projects/1', desc: "What's shipping next across simulators and tools." },
];
