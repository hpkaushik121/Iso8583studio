/**
 * HSM Simulator guide (/simulator/hsm) — page content. Ported from the
 * prototype's hsm-simulator-data.js, with the live page's fuller secure command
 * list; imported only by pages/site/docs-hsm-simulator.ts. The glass screens
 * the blocks refer to are in pages/shared/simglass/hsm-screens.ts.
 */
import { GuideCard } from '../shared/guide';
import { HsmScreenId } from '../shared/simglass/hsm-screens';
import { SimGuideData, SimSection, screen, simFeatures, stage } from '../shared/simglass/sim-guide';

const STAGES = [['01', 'Configure', 'Profile · Network · LMK'], ['02', 'Host commands', 'Thales payShield command set'], ['03', 'Reference', 'Key types · PIN formats · Errors']];

const CMD_HEAD = ['Command', 'Response', 'Name', 'Description'];
const CMD_COLS = '80px 84px 200px minmax(0,1fr)';

const CARDS: GuideCard[] = [
  { id: 'cmd-diagnostics', icon: 'pulse', name: 'Diagnostics & Info', tag: 'NC · NO · VR · VT · GK', desc: 'Health check, version and serial number, the LMK table and random LMK components for key loading ceremonies.' },
  { id: 'cmd-key-mgmt', icon: 'key', name: 'Key Management', tag: 'A0 · A6 · A8 · BU · BW · GC · FK', desc: 'Generate, import, export and translate keys, compute KCVs and run split-knowledge component ceremonies.' },
  { id: 'cmd-pin-block', icon: 'keyboard', name: 'PIN Block Operations', tag: 'BA · CA · CI · BC · G0 · JC · JE · JG · NG', desc: 'Encrypt PIN blocks and translate them between TPK, ZPK, LMK and DUKPT keys.' },
  { id: 'cmd-pin-verify', icon: 'seal-check', name: 'PIN Verification', tag: 'DA · DC', desc: 'Verify a PIN with the IBM 3624 natural PIN method or against a VISA PVV.' },
  { id: 'cmd-pin-gen', icon: 'list-numbers', name: 'PIN Generation', tag: 'DE · DG · EE', desc: 'Generate IBM 3624 offsets and VISA PVVs, or derive the natural PIN from an offset.' },
  { id: 'cmd-encrypt', icon: 'lock', name: 'Data Encryption / Decryption', tag: 'M0 · M2 · M4', desc: 'Encrypt, decrypt and translate data blocks under ZEK, DEK or BDK in ECB or CBC mode.' },
  { id: 'cmd-mac', icon: 'shield-check', name: 'MAC Operations', tag: 'M6 · M8 · MY', desc: 'Generate and verify MACs with ISO 9797 Algorithm 1 or 3 under ZAK or TAK keys.' },
  { id: 'cmd-hash', icon: 'fingerprint', name: 'Hashing', tag: 'GM', desc: 'Compute SHA-1, SHA-256 or MD5 digests over the input data.' },
  { id: 'cmd-rsa', icon: 'certificate', name: 'RSA / Asymmetric', tag: 'EI · EO · EW · EY', desc: 'Key pairs under LMK, DER public key import, and RSA or ECDSA signature generation and validation.' },
  { id: 'cmd-cvv', icon: 'credit-card', name: 'Dynamic CVV/CVC', tag: 'PM', desc: 'Verify Visa dCVV or MasterCard CVC3 values for contactless transactions.' },
  { id: 'cmd-storage', icon: 'archive', name: 'User Storage', tag: 'LA · LE · LD', desc: 'Load, read and delete keys or data in indexed user storage slots 000–FFF.' },
];

const SECTIONS: SimSection<HsmScreenId>[] = [
  {
    id: 'profile', title: 'Device Profiles', rail: 'Device profiles', icon: 'identification-card', eyebrow: stage(STAGES, 0),
    intro: 'The **Profile** tab of the HSM Simulator Configuration screen describes the device the simulator presents. The left rail lists every profile you have created, with buttons to add, import, export and delete; **Launch HSM Simulator** starts the selected one.',
    blocks: [
      screen('profile', 'the profile list, basic information, six vendor tiles with Thales selected, model and firmware'),
      { t: 'h3', x: 'Basic Information' },
      { t: 'bullets', items: [
        ['HSM Name', 'The profile name, shown in the profile list and in the simulator’s title bar.'],
        ['Description', 'Free text for your own reference.'],
        ['Serial Number', 'The device serial the profile carries (e.g. `HSM001234567890`).'],
      ] },
      { t: 'h3', x: 'Vendor & Model' },
      { t: 'p', x: 'Pick a vendor tile and the model drop-down below it changes to that vendor’s device list. **Firmware Version** is a free-text field on the profile.' },
      { t: 'table', head: ['Vendor', 'Models'], rows: [
        ['Thales', '`payShield 9000`, `payShield 10K`'],
        ['SafeNet Luna', '`Network Attached`, `PCIe`, `USB`'],
        ['Utimaco CryptoServer', '`Se`, `CP5`'],
        ['Futurex Excrypt', '`KMES`, `VirtuCrypt`'],
        ['nCipher nShield', '`Connect`, `Solo`, `Edge`'],
        ['Generic/Custom HSM', '`Custom Model`'],
      ] },
      { t: 'note', tone: 'warn', icon: 'warning', title: 'Which vendors answer commands today', x: 'Only the **Thales** profile has a command engine behind it. The other five are selectable and store their vendor, model and firmware on the profile, but no command processor is wired up for them yet — a simulator launched on one of those profiles accepts the connection and answers `ERROR: No active HSM`. Use the Thales profile for anything you intend to actually drive.' },
      { t: 'note', tone: 'blue', icon: 'info', title: 'Note', x: 'To talk to a non-Thales HSM, use the [HSM Command Console](/simulator/hsm-command-console) instead — that tool is a *client*, and it does carry per-vendor framing and command sets for Thales payShield, Futurex, SafeNet Luna, Utimaco and nCipher.' },
      { t: 'h3', x: 'Saving and launching' },
      { t: 'bullets', items: [
        ['Save All Configurations', 'Persists every profile in the list.'],
        ['Launch HSM Simulator', 'Opens the selected profile’s simulator, with the HSM Handler, Key Management, Host Commands, Secure Commands and Logs tabs.'],
      ] },
    ],
  },
  {
    id: 'quick-start', title: 'Quick Start Guide', rail: 'Quick start', icon: 'lightning', eyebrow: stage(STAGES, 0),
    blocks: [
      { t: 'steps', items: [
        '**Create an HSM configuration** — From the Home screen, create a new HSM Simulator configuration. Set the profile name and device details.',
        '**Configure network settings** — Set the TCP/IP bind address and port (e.g. `0.0.0.0:9090`). Configure the message header length (default: 4 characters).',
        '**Launch the simulator** — Open the HSM Simulator screen and start the server from the **HSM Handler** tab.',
        '**Connect your application** — Point your host application to `host:port` and send PayShield host commands.',
        '**Test with built-in commands** — Use the **Host Commands** tab to send commands interactively and inspect responses.',
      ] },
      { t: 'note', tone: 'teal', icon: 'lightbulb', title: 'Tip', x: 'Start with the `NC` (Diagnostic Test) command to verify the simulator is running and LMK is loaded. A response of `ND00` confirms everything is healthy.' },
    ],
  },
  {
    id: 'protocol', title: 'Message Protocol', rail: 'Message protocol', icon: 'arrows-left-right', eyebrow: stage(STAGES, 1),
    intro: 'The HSM Simulator uses the standard Thales PayShield host command protocol over TCP/IP.',
    blocks: [
      screen('logs', 'an NC diagnostic and a VT LMK table exchange as they go over the wire, each with its audit entries'),
      { t: 'h3', x: 'Request Format' },
      { t: 'code', lines: [
        '[Length Header (2 bytes, optional)][Message Header (4 chars)][Command Code (2 chars)][Data Fields][%LMK_ID][Trailer]',
      ] },
      { t: 'table', head: ['Component', 'Size', 'Description'], cols: '170px 96px minmax(0,1fr)', rows: [
        ['**Length Header**', '2 bytes', 'Optional. Big-endian message length (excluding the length header itself).'],
        ['**Message Header**', '4 chars', 'Configurable header echoed back in response (e.g. `0000`).'],
        ['**Command Code**', '2 chars', 'Two-character command identifier (e.g. `NC`, `A0`).'],
        ['**Data Fields**', 'Variable', 'Command-specific data parameters.'],
        ['**LMK ID**', '3 chars', 'Optional `%XX` suffix to select a specific LMK slot.'],
        ['**Trailer**', 'Variable', 'Optional end-of-message marker (`0x19` + trailer data).'],
      ] },
      { t: 'h3', x: 'Response Format' },
      { t: 'code', lines: [
        '[Length Header (2 bytes, optional)][Message Header][Response Code][Error Code (2 chars)][Response Data]',
      ] },
      { t: 'p', x: 'The response code is the command code incremented by one character (e.g. `NC` → `ND`, `A0` → `A1`). An error code of `00` indicates success.' },
      { t: 'h3', x: 'Example Exchange' },
      { t: 'code', lines: [
        'Request:  0000NC',
        'Response: 0000ND00LMK12345678901234567890123456789012345678',
        '',
        'Request:  0000A0001U',
        'Response: 0000A100U1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF',
      ] },
      { t: 'h3', x: 'Encoding' },
      { t: 'p', x: 'All messages use **ISO-8859-1** encoding. Keys are represented as hexadecimal strings in the protocol.' },
    ],
  },
  {
    id: 'network', title: 'Network Settings', rail: 'Network settings', icon: 'plugs-connected', eyebrow: stage(STAGES, 0),
    blocks: [
      screen('network', 'Connection Configuration — TCP_IP selected, 0.0.0.0:9090, the length header on with a 4-character message header, SSL/TLS off'),
      { t: 'h3', x: 'TCP/IP Configuration' },
      { t: 'table', head: ['Setting', 'Default', 'Description'], cols: '200px 96px minmax(0,1fr)', rows: [
        ['**Bind Address**', '`0.0.0.0`', 'IP address to listen on. Use `0.0.0.0` for all interfaces.'],
        ['**Port**', '`9090`', 'TCP port for the HSM server.'],
        ['**Length Header**', 'Optional', 'Enable 2-byte big-endian length prefix before each message.'],
        ['**Message Header Length**', '4', 'Number of characters in the message header (echoed back in response).'],
      ] },
      { t: 'h3', x: 'Additional Connection Types' },
      { t: 'bullets', items: [
        ['Serial', 'RS232 connection with configurable port, baud rate, data bits, stop bits, and parity.'],
        ['REST API', 'HTTP endpoint for HSM commands.'],
        ['WebSocket', 'Real-time bidirectional HSM communication.'],
      ] },
    ],
  },
  {
    id: 'lmk', title: 'LMK Storage', rail: 'LMK storage', icon: 'hard-drives', eyebrow: stage(STAGES, 0),
    intro: 'The Local Master Key (LMK) is the foundation of HSM security. All working keys are encrypted under LMK pairs.',
    blocks: [
      screen('lmk', 'LMK Slot 00 — scheme, algorithm, pair count, check value and the derived key block protection key'),
      { t: 'h3', x: 'LMK Structure' },
      { t: 'bullets', items: [
        ['40 LMK Pairs', 'Pairs 00–39, each consisting of a left key and right key. Different key types are encrypted under specific LMK pairs.'],
        ['Multiple LMK Slots', 'Slots 00–99 allow independent LMK sets. Select the slot using `%XX` in commands or via port mapping.'],
        ['Old / New LMK', 'Support for LMK migration with the `BW` (Translate Key) command.'],
      ] },
      screen('keys', 'Key Management → Overview — 99 slots, two loaded, the LMK slot map'),
      { t: 'h3', x: 'LMK Pair Assignments' },
      { t: 'table', head: ['LMK Pair', 'Key Type Encrypted'], cols: '160px minmax(0,1fr)', rows: [
        ['Pair 02–03', 'PIN Encryption Keys (PINs under LMK)'],
        ['Pair 04–05', 'ZMK (Zone Master Keys)'],
        ['Pair 06–07', 'ZPK, TPK (Zone/Terminal PIN Keys)'],
        ['Pair 14–15', 'PVK (PIN Verification Keys)'],
        ['Pair 16–17', 'TAK (Terminal Authentication Keys)'],
        ['Pair 22–23', 'BDK (Base Derivation Keys)'],
        ['Pair 26–27', 'ZEK, DEK (Data Encryption Keys)'],
        ['Pair 28–29', 'ZAK (Zone Authentication Keys)'],
      ] },
      { t: 'h3', x: 'Persistence' },
      { t: 'p', x: 'LMK data is stored in `payShield10k_lmkStorage.json` and within the simulator configuration JSON. Keys persist across restarts.' },
    ],
  },
  {
    id: 'cmd-diagnostics', title: 'Diagnostics & Info Commands', rail: 'Diagnostics', icon: 'pulse', eyebrow: stage(STAGES, 1),
    blocks: [
      screen('host-nc', 'Host Commands — NC Diagnostic Test and its ND00 response, formatted and on the wire'),
      { t: 'table', head: CMD_HEAD, cols: CMD_COLS, rows: [
        ['`NC`', '`ND`', 'Diagnostic Test', 'Health check. Returns `00` if the HSM and LMK are functioning correctly.'],
        ['`NO`', '`NP`', 'Reserved', 'Reserved diagnostic command.'],
        ['`VR`', '`VS`', 'Version Info', 'Returns firmware version and serial number of the emulated HSM.'],
        ['`VT`', '`VU`', 'View LMK Table', 'Displays Key Check Values (KCVs) of all 40 loaded LMK pairs.'],
        ['`GK`', '`GL`', 'Generate LMK Component', 'Generates a random LMK component for key loading ceremonies.'],
      ] },
      { t: 'h3', x: 'Example: Diagnostic Test' },
      { t: 'code', lines: [
        'Request:  0000NC',
        'Response: 0000ND00',
        '',
        'Error code 00 = HSM is healthy, LMK loaded.',
      ] },
    ],
  },
  {
    id: 'cmd-key-mgmt', title: 'Key Management Commands', rail: 'Key management', icon: 'key', eyebrow: stage(STAGES, 1),
    blocks: [
      screen('host-a0', 'Host Commands — A0 Generate Key, a ZPK under the LMK with the double-length scheme'),
      { t: 'table', head: CMD_HEAD, cols: CMD_COLS, rows: [
        ['`A0`', '`A1`', 'Generate Key', 'Generates a new DES/3DES key encrypted under LMK. Optionally also encrypted under a ZMK for transport.'],
        ['`A6`', '`A7`', 'Import Key', 'Imports a key encrypted under ZMK and re-encrypts it under LMK.'],
        ['`A8`', '`A9`', 'Export Key', 'Exports a key from LMK encryption to ZMK encryption for transport to another zone.'],
        ['`BU`', '`BV`', 'Generate KCV', 'Generates a Key Check Value (KCV) for a given key. Used to verify key integrity.'],
        ['`BW`', '`BX`', 'Translate Key (LMK)', 'Translates a key from old LMK encryption to new LMK encryption during LMK migration.'],
        ['`GC`', '`GD`', 'Generate Components', 'Generates N random key components. Used in split-knowledge key ceremonies.'],
        ['`FK`', '`FL`', 'Form Key from Components', 'XORs 2 or 3 key components together to form a working key under LMK.'],
      ] },
      { t: 'h3', x: 'Example: Generate a ZPK' },
      { t: 'code', lines: [
        'Request:  0000A0001U',
        '                ^^^ ^^^',
        '                |   |-- Key scheme: U = double-length',
        '                |------ Key type: 001 = ZPK',
        '',
        'Response: 0000A100U<key-under-LMK><key-under-ZMK><KCV>',
      ] },
      { t: 'h3', x: 'Example: Generate KCV' },
      { t: 'code', lines: [
        'Request:  0000BU0U<key-under-LMK>',
        'Response: 0000BV00<6-char-KCV>',
      ] },
    ],
  },
  {
    id: 'cmd-pin-block', title: 'PIN Block Operations', rail: 'PIN blocks', icon: 'keyboard', eyebrow: stage(STAGES, 1),
    blocks: [
      screen('host-ca', 'Host Commands — CA Translate PIN from TPK to ZPK, ISO Format 0 on both sides'),
      { t: 'table', head: CMD_HEAD, cols: CMD_COLS, rows: [
        ['`BA`', '`BB`', 'Encrypt PIN Block', 'Encrypts a clear-text PIN into a PIN block under TPK or ZPK.'],
        ['`CA`', '`CB`', 'Translate PIN (TPK → ZPK)', 'Re-encrypts a PIN block from Terminal PIN Key to Zone PIN Key.'],
        ['`CI`', '`CJ`', 'Translate PIN (ZPK → TPK)', 'Re-encrypts a PIN block from Zone PIN Key to Terminal PIN Key.'],
        ['`BC`', '`BD`', 'Translate PIN Block', 'Translates PIN block between encryption keys.'],
        ['`G0`', '`G1`', 'Translate PIN (DUKPT → ZPK)', 'Translates a DUKPT-encrypted PIN block to ZPK encryption.'],
        ['`JC`', '`JD`', 'Translate PIN (TPK → LMK)', 'Re-encrypts a TPK-encrypted PIN block under LMK.'],
        ['`JE`', '`JF`', 'Translate PIN (ZPK → LMK)', 'Re-encrypts a ZPK-encrypted PIN block under LMK.'],
        ['`JG`', '`JH`', 'Translate PIN (LMK → ZPK)', 'Re-encrypts an LMK-encrypted PIN to a ZPK PIN block.'],
        ['`NG`', '`NH`', 'Generate DUKPT Key', 'Derives a DUKPT session key from a BDK and Key Serial Number.'],
      ] },
      { t: 'h3', x: 'Example: Translate PIN (TPK → ZPK)' },
      { t: 'code', lines: [
        'Request:  0000CA<TPK><ZPK><max-pin-length><pin-block>01<account-number>01<account-number>',
        'Response: 0000CB00<pin-length><translated-pin-block>',
      ] },
      { t: 'note', tone: 'blue', icon: 'info', title: 'Note', x: 'The `CA` command is the most commonly used PIN translation in payment processing. It converts a PIN block received from the terminal (encrypted under TPK) into a block suitable for sending to the card issuer (encrypted under ZPK).' },
    ],
  },
  {
    id: 'cmd-pin-verify', title: 'PIN Verification Commands', rail: 'PIN verification', icon: 'seal-check', eyebrow: stage(STAGES, 1),
    blocks: [
      screen('host-dc', 'Host Commands — DC Verify PIN against a VISA PVV'),
      { t: 'table', head: CMD_HEAD, cols: CMD_COLS, rows: [
        ['`DA`', '`DB`', 'Verify PIN (IBM 3624)', 'Verifies a PIN using the IBM 3624 natural PIN method with offset data.'],
        ['`DC`', '`DD`', 'Verify PIN (VISA PVV)', 'Verifies a PIN against a VISA Pin Verification Value (PVV).'],
      ] },
      { t: 'h3', x: 'Example: VISA PVV Verification' },
      { t: 'code', lines: [
        'Request:  0000DC<ZPK>01<pin-block><account-number><PVK-pair><PVV>',
        'Response: 0000DD00   (00 = PIN verified successfully)',
        'Response: 0000DD01   (01 = PIN verification failed)',
      ] },
    ],
  },
  {
    id: 'cmd-pin-gen', title: 'PIN Generation Commands', rail: 'PIN generation', icon: 'list-numbers', eyebrow: stage(STAGES, 1),
    blocks: [
      { t: 'table', head: CMD_HEAD, cols: CMD_COLS, rows: [
        ['`DE`', '`DF`', 'Generate IBM PIN Offset', 'Generates an IBM 3624 PIN offset. Used for PIN issuance.'],
        ['`DG`', '`DH`', 'Generate VISA PVV', 'Generates a VISA Pin Verification Value for a given PIN.'],
        ['`EE`', '`EF`', 'Derive PIN from Offset', 'Derives the natural PIN from an IBM 3624 offset and validation data.'],
      ] },
    ],
  },
  {
    id: 'cmd-encrypt', title: 'Data Encryption / Decryption Commands', rail: 'Encryption', icon: 'lock', eyebrow: stage(STAGES, 1),
    blocks: [
      { t: 'table', head: CMD_HEAD, cols: CMD_COLS, rows: [
        ['`M0`', '`M1`', 'Encrypt Data Block', 'Encrypts a data block using ZEK, DEK, or BDK in ECB or CBC mode.'],
        ['`M2`', '`M3`', 'Decrypt Data Block', 'Decrypts a data block using ZEK, DEK, or BDK.'],
        ['`M4`', '`M5`', 'Translate Data Block', 'Re-encrypts data from one key to another (e.g. DUKPT to ZEK).'],
      ] },
      { t: 'h3', x: 'Cipher Modes' },
      { t: 'bullets', items: [
        ['ECB (Electronic Codebook)', 'Each block encrypted independently. Simpler but less secure for repetitive data.'],
        ['CBC (Cipher Block Chaining)', 'Each block XORed with the previous ciphertext block. Requires an IV (Initialization Vector).'],
      ] },
    ],
  },
  {
    id: 'cmd-mac', title: 'MAC Operations', rail: 'MAC', icon: 'shield-check', eyebrow: stage(STAGES, 1),
    blocks: [
      { t: 'table', head: CMD_HEAD, cols: CMD_COLS, rows: [
        ['`M6`', '`M7`', 'Generate MAC', 'Generates a MAC using ZAK or TAK with ISO 9797 Algorithm 1 or 3.'],
        ['`M8`', '`M9`', 'Verify MAC', 'Verifies a MAC value against the original data and key.'],
        ['`MY`', '`MZ`', 'MAC Variants', 'Additional MAC algorithm and format variants.'],
      ] },
      { t: 'h3', x: 'MAC Algorithms' },
      { t: 'bullets', items: [
        ['ISO 9797 Algorithm 1', 'Single DES CBC-MAC. Standard for most payment applications.'],
        ['ISO 9797 Algorithm 3', 'Retail MAC (DES CBC-MAC with final 3DES step). Provides stronger security.'],
      ] },
    ],
  },
  {
    id: 'cmd-hash', title: 'Hashing Commands', rail: 'Hashing', icon: 'fingerprint', eyebrow: stage(STAGES, 1),
    blocks: [
      { t: 'table', head: CMD_HEAD, cols: CMD_COLS, rows: [
        ['`GM`', '`GN`', 'Hash Data', 'Computes a hash digest of the input data.'],
      ] },
      { t: 'h3', x: 'Supported Algorithms' },
      { t: 'bullets', items: [
        'SHA-1',
        'SHA-256',
        'MD5',
      ] },
    ],
  },
  {
    id: 'cmd-rsa', title: 'RSA / Asymmetric Commands', rail: 'RSA', icon: 'certificate', eyebrow: stage(STAGES, 1),
    blocks: [
      { t: 'table', head: CMD_HEAD, cols: CMD_COLS, rows: [
        ['`EI`', '`EJ`', 'Generate RSA Key Pair', 'Generates an RSA public/private key pair. Private key encrypted under LMK.'],
        ['`EO`', '`EP`', 'Import RSA Public Key', 'Imports a DER-encoded RSA public key into the HSM.'],
        ['`EW`', '`EX`', 'Generate Signature', 'Creates an RSA or ECDSA digital signature over input data.'],
        ['`EY`', '`EZ`', 'Validate Signature', 'Validates an RSA or ECDSA signature against the original data.'],
      ] },
    ],
  },
  {
    id: 'cmd-cvv', title: 'Dynamic CVV/CVC', rail: 'Dynamic CVV', icon: 'credit-card', eyebrow: stage(STAGES, 1),
    blocks: [
      { t: 'table', head: CMD_HEAD, cols: CMD_COLS, rows: [
        ['`PM`', '`PN`', 'Verify Dynamic CVV/CVC', 'Verifies Visa dCVV or MasterCard CVC3 values for contactless transactions.'],
      ] },
    ],
  },
  {
    id: 'cmd-storage', title: 'User Storage Commands', rail: 'User storage', icon: 'archive', eyebrow: stage(STAGES, 1),
    intro: 'Store, retrieve, and delete keys or data in indexed user storage slots (000–FFF).',
    blocks: [
      { t: 'table', head: CMD_HEAD, cols: CMD_COLS, rows: [
        ['`LA`', '`LB`', 'Load to Storage', 'Stores a key or data at a specified storage index (000–FFF).'],
        ['`LE`', '`LF`', 'Read from Storage', 'Retrieves data from a specified storage index.'],
        ['`LD`', '`LM`', 'Delete from Storage', 'Deletes the entry at a specified storage index.'],
      ] },
    ],
  },
  {
    id: 'key-types', title: 'Key Types & Schemes', rail: 'Key types & schemes', icon: 'stack', eyebrow: stage(STAGES, 2),
    blocks: [
      { t: 'h3', x: 'Key Types' },
      { t: 'table', head: ['Code', 'Name', 'Purpose'], cols: '80px 120px minmax(0,1fr)', rows: [
        ['`000`', 'ZMK', 'Zone Master Key — Key-encrypting key for secure key exchange between zones.'],
        ['`001`', 'ZPK', 'Zone PIN Key — Encrypts PIN blocks for transmission between zones.'],
        ['`002`', 'TPK / PVK', 'Terminal PIN Key / PIN Verification Key — Terminal PIN encryption or PIN verification.'],
        ['`003`', 'TAK', 'Terminal Authentication Key — MAC generation at the terminal.'],
        ['`008`', 'ZAK', 'Zone Authentication Key — MAC generation between zones.'],
        ['`009`', 'BDK', 'Base Derivation Key — Master key for DUKPT key derivation.'],
        ['`00A`', 'ZEK', 'Zone Encryption Key — Data encryption between zones.'],
        ['`00B`', 'DEK', 'Data Encryption Key — General-purpose data encryption.'],
        ['`302`', 'IKEY', 'Intermediate Key — Internal processing key.'],
      ] },
      { t: 'h3', x: 'Key Schemes' },
      { t: 'table', head: ['Scheme', 'Hex Length', 'Description'], cols: '80px 130px minmax(0,1fr)', rows: [
        ['`X`', '16 characters', 'Single-length DES key (56 effective bits).'],
        ['`U`', '32 characters', 'Double-length 3DES key (112 effective bits). Most common for payment.'],
        ['`T`', '48 characters', 'Triple-length 3DES key (168 effective bits). Maximum DES security.'],
      ] },
    ],
  },
  {
    id: 'pin-formats', title: 'PIN Block Formats', rail: 'PIN block formats', icon: 'rows', eyebrow: stage(STAGES, 2),
    blocks: [
      { t: 'table', head: ['Code', 'Format', 'Description'], cols: '80px 190px minmax(0,1fr)', rows: [
        ['`01`', 'ISO 9564-1 Format 0', 'PAN-bound: the PIN field XORed with the rightmost 12 PAN digits. The most widely used format.'],
        ['`02`', 'Docutel', 'Legacy Docutel ATM format.'],
        ['`03`', 'Diebold & IBM', 'Legacy Diebold / IBM 3624 format.'],
        ['`04`', 'PLUS Network', 'PLUS network PIN block format.'],
        ['`05`', 'ISO 9564-1 Format 1', 'No PAN dependency; the PIN is padded with random digits.'],
        ['`34`', 'ISO 9564-1 Format 2 (EMV)', 'The format carried in an EMV VERIFY command: PIN padded with F, no PAN.'],
        ['`35`', 'Mastercard Pay Now & Pay Later', 'Mastercard PNPL format.'],
        ['`41`', 'Visa / Amex', 'Visa and American Express PIN block formats (`42` for Amex).'],
        ['`46`', 'AS 2805.3 format 8', 'Australian standard format (ISO format 3 with the Australian field layout).'],
        ['`47`', 'ISO 9564-1 Format 3', 'PAN-bound like Format 0, but padded with random digits A–F.'],
        ['`48`', 'ISO 9564-1 Format 4 (AES)', 'The AES PIN block: a 16-byte PIN field bound to a 16-byte PAN field.'],
      ] },
    ],
  },
  {
    id: 'error-codes', title: 'Error Codes', rail: 'Error codes', icon: 'warning-circle', eyebrow: stage(STAGES, 2),
    intro: 'Every HSM response includes a 2-character error code. `00` indicates success.',
    blocks: [
      { t: 'table', head: ['Code', 'Meaning'], cols: '90px minmax(0,1fr)', rows: [
        ['`00`', 'No error — command executed successfully.'],
        ['`01`', 'Verification failure (PIN or MAC mismatch).'],
        ['`02`', 'Key Check Value (KCV) failure.'],
        ['`04`', 'Cryptographic algorithm not supported.'],
        ['`10`', 'PIN block length error.'],
        ['`15`', 'Invalid input data.'],
        ['`17`', 'Invalid message length.'],
        ['`23`', 'PIN length error.'],
        ['`26`', 'Invalid LMK identifier.'],
        ['`27`', 'LMK check value failure.'],
        ['`39`', 'Console authorization required.'],
        ['`42`', 'Invalid LMK type code.'],
        ['`68`', 'Command is disabled.'],
        ['`74`', 'Data parity error in key or data block.'],
        ['`75`', 'Invalid message length field.'],
        ['`80`', 'Unknown command or invalid format.'],
        ['`82`', 'Function not permitted by current configuration.'],
        ['`A1`', 'Console not authorized. Grant authorization first.'],
        ['`A2`', 'HSM not authorized.'],
        ['`A3`', 'Command only available in secure state.'],
        ['`A4`', 'Invalid key type for this command.'],
        ['`B1`', 'Invalid key scheme code.'],
        ['`C1`', 'LMK is not loaded. Load LMK before issuing commands.'],
      ] },
    ],
  },
  {
    id: 'tabs-reference', title: 'UI Tabs Reference', rail: 'UI tabs', icon: 'layout', eyebrow: stage(STAGES, 0),
    blocks: [
      { t: 'table', head: ['Tab', 'Purpose'], cols: '190px minmax(0,1fr)', rows: [
        ['**HSM Handler**', 'Start/stop the HSM server. View live request/response traffic, connection count, and server status.'],
        ['**Key Management**', 'View loaded LMK status, key check values, and manage key components.'],
        ['**Host Commands**', 'Interactive command forms for the 41 documented host commands. Grouped by category with search and filtering. Shows wire-format hints and formatted results.'],
        ['**Secure Commands**', 'Administrative and LMK-related operations requiring console authorization (GC, FK, GK, A0, etc.).'],
        ['**Logs**', 'Full request/response log of all HSM traffic.'],
      ] },
    ],
  },
  {
    id: 'secure-commands', title: 'Secure Commands', rail: 'Secure commands', icon: 'shield-check', eyebrow: stage(STAGES, 1),
    intro: 'Certain operations require console authorization before they can be executed, simulating the real PayShield security model.',
    blocks: [
      screen('secure', 'console authorization through the custodian dialog, then VT View LMK Table'),
      { t: 'h3', x: 'Authorization Flow' },
      { t: 'steps', items: [
        '**Grant Authorization** — In the Secure Commands tab, click the authorization button. This simulates inserting a custodian smart card.',
        '**Execute Commands** — While authorized, you can run secure commands like `GC`, `FK`, `GK`, and LMK management operations.',
        '**Authorization Timeout** — Authorization expires after a configurable period (default: 8 hours) or when manually revoked.',
      ] },
      { t: 'h3', x: 'Secure Command List' },
      { t: 'p', x: 'The following commands are available in the Secure Commands tab:' },
      { t: 'table', head: ['Command', 'Name', 'Purpose'], cols: '90px 220px minmax(0,1fr)', rows: [
        ['`NC`', 'Diagnostic Test', 'Verify HSM health.'],
        ['`VR`', 'Version Info', 'Check firmware version.'],
        ['`VT`', 'View LMK Table', 'Display all LMK KCVs.'],
        ['`GK`', 'Generate LMK Component', 'Create LMK key component.'],
        ['`GC`', 'Generate Key Components', 'Generate N random components for split knowledge.'],
        ['`FK`', 'Form Key from Components', 'XOR components to form a key.'],
        ['`A0`', 'Generate Key', 'Generate working keys under LMK.'],
        ['`LA`', 'Load to Storage', 'Store data in HSM user storage.'],
        ['`LE`', 'Read from Storage', 'Retrieve from user storage.'],
        ['`LD`', 'Delete from Storage', 'Remove user storage entry.'],
        ['`BW`', 'Translate Key', 'LMK migration — old to new LMK.'],
        ['`DE`', 'Generate IBM PIN Offset', 'PIN issuance with IBM 3624.'],
        ['`DG`', 'Generate VISA PVV', 'PIN issuance with VISA PVV.'],
        ['`EI`', 'Generate RSA Key Pair', 'Asymmetric key generation.'],
      ] },
      { t: 'note', tone: 'warn', icon: 'warning', title: 'Important', x: 'Attempting to run a secure command without authorization will return error code `A1` (Console not authorized). Always grant authorization before executing these operations.' },
    ],
  },
];

export const HSM_SIMULATOR_GUIDE: SimGuideData<HsmScreenId> = {
  slug: 'hsm-simulator',
  meta: 'Thales · SafeNet Luna · Utimaco · Futurex · nCipher · Generic',
  title: 'HSM Simulator',
  lede: 'A free, open-source payShield 10K simulator for host application development: emulate a payment Hardware Security Module on your own machine, with no appliance and no vendor licence. Device profiles cover six HSM vendors and their models, with a Thales payShield command engine behind them — key management, PIN operations, encryption, MAC generation, and more.',
  clips: ['hsm-1', 'hsm-2'],
  poster: 'hsm',
  browse: 'Browse the commands',
  intro: {
    eyebrow: 'hsm-simulator · Thales PayShield 10k profile',
    paras: [
      'The HSM Simulator in ISO8583Studio stands in for a payment Hardware Security Module, so you can develop and test host applications that need HSM integration without physical hardware. Each simulator is a **profile** — a named device with its own vendor, model, serial number and network settings — and you can keep as many profiles as you need side by side.',
      'Six vendor profiles are selectable, covering Thales, SafeNet, Utimaco, Futurex, nCipher and a generic device. The command engine behind them implements the **Thales payShield** host command set, which is what the rest of this page documents.',
    ],
    screen: { kind: 'screen', id: 'handler', caption: 'running as Thales PayShield 10k on 0.0.0.0:9090 — an NO / NP HSM State exchange as formatted request and response beside their raw hex' },
    features: simFeatures([
      ['key', 'Key Management', 'Generate, import, export, and translate cryptographic keys including ZMK, ZPK, TPK, BDK, ZEK, and more.'],
      ['keyboard', 'PIN Operations', 'Translate PIN blocks between keys, verify PINs using IBM 3624 and VISA PVV methods, and generate PIN offsets.'],
      ['lock', 'Encryption / Decryption', 'Encrypt and decrypt data blocks with DES/3DES in ECB and CBC modes using ZEK, DEK, or BDK keys.'],
      ['shield-check', 'MAC Generation', 'Generate and verify MACs using ISO 9797 Algorithm 1 and Algorithm 3 with ZAK or TAK keys.'],
      ['certificate', 'RSA Support', 'Generate RSA key pairs, import public keys, and create/validate digital signatures.'],
      ['hard-drives', 'LMK Storage', 'Multiple LMK slots, persistent key storage, and full LMK lifecycle management.'],
    ]),
  },
  cards: { heading: 'Command set', rail: 'Command set', lede: 'Every payShield host command the simulator answers, grouped the way the Host Commands tab groups them — each card links to the command table below.', more: 'View commands', items: CARDS },
  sections: SECTIONS,
  cta: { heading: 'Run it against your own host', text: 'Free and open source. Download the studio, create a Thales profile and point your host application at 0.0.0.0:9090 in minutes.' },
};
