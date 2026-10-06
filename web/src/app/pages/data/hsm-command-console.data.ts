/**
 * HSM Command Console guide (/simulator/hsm-command-console) — page content.
 * Ported from the prototype's hsm-command-console-data.js; imported only by
 * pages/site/docs-hsm-command-console.ts. The glass screens the blocks refer
 * to are in pages/shared/simglass/console-screens.ts.
 */
import { GuideCard } from '../shared/guide';
import { ConsoleScreenId } from '../shared/simglass/console-screens';
import { SimGuideData, SimSection, screen, simFeatures, stage } from '../shared/simglass/sim-guide';

const STAGES = [['01', 'Configure', 'Connection · Framing · TLS'], ['02', 'Run', 'Console · Scenario · Load test · Logs'], ['03', 'Reference', 'Tabs']];

const CARDS: GuideCard[] = [
  { id: 'console', icon: 'terminal-window', name: 'Console', tag: 'Compose · Send · Decode', desc: 'Compose and send individual host commands; read decoded responses.' },
  { id: 'scenario', icon: 'flow-arrow', name: 'Scenario', tag: 'Steps · Playlists', desc: 'Build, save, and replay ordered command sequences.' },
  { id: 'load-test', icon: 'gauge', name: 'Load Test', tag: 'Constant · Ramp · Spike · Burst', desc: 'Run a scenario at a target rate and pattern across connections.' },
  { id: 'logs', icon: 'list-dashes', name: 'Logs', tag: 'Timestamps · Bytes in / out', desc: 'Timestamped exchange log with connection and byte counters.' },
];

const SECTIONS: SimSection<ConsoleScreenId>[] = [
  {
    id: 'quick-start', title: 'Quick Start', rail: 'Quick start', icon: 'lightning', eyebrow: stage(STAGES, 0),
    intro: 'From a new configuration to the first decoded response:',
    blocks: [
      { t: 'steps', items: [
        '**Create a console configuration** — From the Home screen open `HSM Host Console` and add a new configuration.',
        '**Pick your HSM vendor** — In **Connection Settings**, choose the HSM Type. The port and framing default to that vendor (e.g. Thales payShield → port `1500`, 2-byte binary length).',
        '**Set the address** — Enter the HSM’s **IP Address** and **Port**, and a connection **Timeout**.',
        '**(Optional) Enable TLS** — In **SSL/TLS Configuration** turn on TLS, choose the version and certificate-verification mode, and attach your CA / client certificate.',
        '**Launch and connect** — Open the console and click **Connect**. The header dot turns green when the socket is up.',
        '**Send a command** — In the **Console** tab select a command from the vendor set, fill its fields, and send. The response appears in the exchange log.',
      ] },
      { t: 'note', tone: 'teal', icon: 'lightbulb', title: 'Tip', x: 'No physical HSM? Start the [HSM Simulator](/simulator/hsm) as a server on the same machine and point the console at `127.0.0.1:1500` for a fully local key-management loop.' },
    ],
  },
  {
    id: 'vendors', title: 'Supported HSM Vendors', rail: 'HSM vendors', icon: 'buildings', eyebrow: stage(STAGES, 0),
    intro: 'Selecting a vendor sets its default port and message framing automatically. All vendors are driven over the same socket client.',
    blocks: [
      { t: 'table', head: ['Vendor', 'Model family', 'Default Port', 'Framing'], cols: '180px minmax(0,1.1fr) 100px minmax(0,1fr)', rows: [
        ['**Thales payShield**', 'payShield 9000 / 10K', '`1500`', '2-byte binary length'],
        ['**Futurex Excrypt**', 'KMES Series 3', '`2000`', '2-byte binary length'],
        ['**SafeNet Luna**', 'Thales Luna Network HSM', '`1500`', '2-byte binary length'],
        ['**Utimaco CryptoServer**', 'Se / CP5', '`3001`', '2-byte binary length'],
        ['**nCipher nShield**', 'Entrust nShield Connect / Solo', '`9004`', '4-byte ASCII length'],
        ['**Utimaco Atalla**', 'Atalla AT1000', '`7000`', 'STX / ETX framing'],
        ['**Generic HSM**', 'Custom / other', '`1500`', 'Configurable'],
      ] },
    ],
  },
  {
    id: 'connection', title: 'Connection Settings', rail: 'Connection settings', icon: 'plugs-connected', eyebrow: stage(STAGES, 0),
    intro: 'The **Connection Settings** configuration tab defines the transport to the HSM.',
    blocks: [
      screen('connection', 'the console named HSM Host Console - local, HSM type Thales payShield, 127.0.0.1:9090, a 30 second timeout and the TCP length header enabled'),
      { t: 'table', head: ['Setting', 'Description'], rows: [
        ['**IP Address**', 'HSM host interface address. Default `127.0.0.1`.'],
        ['**Port**', 'TCP port; auto-filled from the selected vendor (e.g. `1500` for Thales).'],
        ['**Timeout**', 'Connection / response timeout in seconds. Default `30`.'],
        ['**Name & Description**', 'Identifiers for the saved configuration.'],
      ] },
    ],
  },
  {
    id: 'framing', title: 'Message Framing', rail: 'Message framing', icon: 'brackets-square', eyebrow: stage(STAGES, 0),
    intro: 'HSMs delimit messages differently. The console supports the common framing schemes; the correct one is selected when you pick a vendor, and can be overridden.',
    blocks: [
      screen('framing', 'TCP length header enabled, header format 2-byte binary length, message header 0000, empty trailer and message header length 4 — with the framed NO command previewed byte by byte'),
      { t: 'table', head: ['Header Format', 'Description'], cols: '210px minmax(0,1fr)', rows: [
        ['`2-byte Binary Length`', 'Two-byte big-endian length prefix. Used by Thales, Futurex, Luna, Utimaco.'],
        ['`4-byte ASCII Length`', 'Four ASCII digits of length. Used by nCipher nShield.'],
        ['`STX / ETX Framing`', 'Start/end control bytes bracket the message. Used by Utimaco Atalla.'],
        ['`No Header / Framing`', 'Raw payload with no length prefix.'],
        ['`Custom Header`', 'User-defined header / trailer bytes.'],
      ] },
      { t: 'p', x: 'Additional framing fields: **TCP Length Header Enabled** toggle, **Message Header** (hex, e.g. `0000`), **Message Trailer** (hex, optional), and **Message Header Length**.' },
    ],
  },
  {
    id: 'ssl', title: 'SSL / TLS Configuration', rail: 'SSL / TLS', icon: 'shield-check', eyebrow: stage(STAGES, 0),
    intro: 'Enable encrypted transport to the HSM in the **SSL/TLS Configuration** tab.',
    blocks: [
      screen('ssl', 'encryption enabled, TLS 1.2, a PKCS#12 bundle, CA-signed-only verification, and empty client certificate, private key and keystore password fields'),
      screen('ciphers', 'cipher suites — TLS_AES_256_GCM_SHA384 and TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384 selected, each entry labelled with its strength'),
      { t: 'table', head: ['Setting', 'Options'], cols: '200px minmax(0,1fr)', rows: [
        ['**TLS Version**', 'TLS 1.2 (default), TLS 1.3, and earlier where required.'],
        ['**Certificate Verification**', 'No Verification, Trust All Certificates, CA-Signed Only, Custom CA Authority.'],
        ['**Certificate Type**', 'PKCS#12 client certificate with key-store password.'],
        ['**Cipher Suites**', 'Selectable set, defaulting to `TLS_AES_256_GCM_SHA384` and `TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384`.'],
        ['**Key material**', 'CA authority path, client public certificate, client private key.'],
      ] },
    ],
  },
  {
    id: 'console', title: 'Command Console Tab', rail: 'Console', icon: 'terminal-window', eyebrow: stage(STAGES, 1),
    intro: 'The **Console** tab is the interactive workspace. Pick a host command from the active vendor’s command set, populate its parameter fields, and send it. The request and the decoded response are appended to the exchange log with response codes and timing.',
    blocks: [
      screen('console', 'A0 Generate a Key selected, its parameter fields filled in for a TDES key block, and the response panel showing the decoded key block and its check value'),
      { t: 'bullets', items: [
        ['Vendor command set', 'the available commands follow the selected HSM vendor (for Thales, the full payShield host-command set).'],
        ['Field editor', 'each command exposes its parameters as labelled inputs so you don’t hand-assemble the payload.'],
        ['Send & inspect', 'responses are shown raw and parsed; errors surface the HSM’s response/error code.'],
      ] },
    ],
  },
  {
    id: 'scenario', title: 'Scenario Builder Tab', rail: 'Scenario', icon: 'flow-arrow', eyebrow: stage(STAGES, 1),
    intro: 'A **scenario** is an ordered list of command steps — each step captures a `commandCode` and its field values. Build a sequence (for example: generate a key, export it under a ZMK, then translate a PIN block), name it, and save it into the configuration for reuse.',
    blocks: [
      screen('scenario', 'a two-step flow — A0 Generate a Key then A6 Import a Key — where the import step references the generated key as [1][A0][KEY]'),
      { t: 'bullets', items: [
        ['Steps', 'add, reorder, and edit command steps; values from earlier steps can feed later ones.'],
        ['Saved scenarios', 'persisted with the configuration and available to both the console and the load tester.'],
        ['Playlists', 'group custom text, single commands, and whole scenarios into a runnable playlist with optional auto-advance.'],
      ] },
    ],
  },
  {
    id: 'load-test', title: 'Load Testing Tab', rail: 'Load test', icon: 'gauge', eyebrow: stage(STAGES, 1),
    intro: 'Drive a saved scenario against the HSM to measure throughput and stability. Configure it in the **Load Test Settings** tab and run it from the **Load Test** tab.',
    blocks: [
      screen('load', 'results for an NO HSM Status playlist — 96 sent, 96 received, 96 successes, 0 failures, 0.9 ms average latency and 9.6 tps at a 100% success rate'),
      { t: 'table', head: ['Parameter', 'Description', 'Default'], cols: '210px minmax(0,1fr) 90px', rows: [
        ['Concurrent Connections', 'Parallel sockets driving the load.', '`1`'],
        ['Commands / second', 'Target command rate.', '`10`'],
        ['Duration', 'Test length in seconds.', '`60`'],
        ['Pattern', 'Constant Rate, Ramp Up, Spike Test, or Burst Pattern.', 'Constant'],
      ] },
    ],
  },
  {
    id: 'logs', title: 'Logs Tab', rail: 'Logs', icon: 'list-dashes', eyebrow: stage(STAGES, 1),
    intro: 'The **Logs** tab streams every exchange — request, response, connection events — with timestamps. Clear the buffer, and see live connection count and byte counters. Global logging can be toggled in app settings.',
    blocks: [
      screen('logs', 'connection events and a Thales exchange expanded into its formatted request, formatted response, raw hex and parsed views'),
    ],
  },
  {
    id: 'tabs-reference', title: 'Tabs Reference', rail: 'Tabs reference', icon: 'layout', eyebrow: stage(STAGES, 2),
    intro: 'Inside the app the console is titled **HSM Host Console** and exposes four runtime tabs.',
    blocks: [
      { t: 'table', head: ['Tab', 'Purpose'], cols: '150px minmax(0,1fr)', rows: [
        ['**Console**', 'Compose and send individual host commands; read decoded responses.'],
        ['**Scenario**', 'Build, save, and replay ordered command sequences.'],
        ['**Load Test**', 'Run a scenario at a target rate and pattern across connections.'],
        ['**Logs**', 'Timestamped exchange log with connection and byte counters.'],
      ] },
      { t: 'note', tone: 'blue', icon: 'link', title: 'Related', x: 'Pair this with the [HSM Simulator](/simulator/hsm) (the server side) for a complete, self-contained key-management test rig.' },
    ],
  },
];

export const HSM_COMMAND_CONSOLE_GUIDE: SimGuideData<ConsoleScreenId> = {
  slug: 'hsm-command-console',
  meta: 'Thales · Futurex · SafeNet Luna · Utimaco · nCipher · Generic',
  title: 'HSM Command Console',
  lede: 'A host-command client for hardware security modules. Connect to a real or simulated HSM, fire individual host commands, chain them into repeatable scenarios, and drive them under load — all over TCP/IP with optional TLS. Inside the app it is titled HSM Host Console.',
  clips: ['console'],
  poster: 'console',
  browse: 'Browse the tabs',
  intro: {
    eyebrow: 'hsm-command-console · HSM Host Console - local · Thales payShield',
    paras: [
      'The HSM Command Console acts as a **client** that talks to an HSM’s host interface. It packs your command payload with the vendor’s framing (length header, STX/ETX, etc.), sends it over the socket, and shows the raw request/response exchange. Point it at ISO8583Studio’s own [HSM Simulator](/simulator/hsm) or at a physical device on your bench.',
    ],
    screen: { kind: 'screen', id: 'overview', caption: 'connected to Thales payShield at 127.0.0.1:9090 — the 120-command list on the left and an NO / NP HSM Status exchange as formatted request and response' },
    features: simFeatures([
      ['buildings', 'Multi-Vendor', 'Thales payShield, Futurex, SafeNet Luna, Utimaco CryptoServer & Atalla, and nCipher nShield — each with the correct default port and framing.'],
      ['terminal-window', 'Command Console', 'Compose host commands from the vendor’s command set, fill in fields, send, and read the decoded response.'],
      ['flow-arrow', 'Scenario Builder', 'Chain commands into a named scenario (each step is a command code + field values) and replay it on demand.'],
      ['gauge', 'Load Tester', 'Run a scenario at a target rate with constant, ramp-up, spike, or burst patterns across concurrent connections.'],
      ['lock', 'TLS Transport', 'Optional mutual TLS with configurable version, cipher suites, CA verification, and PKCS#12 client certificates.'],
      ['scroll', 'Exchange Log', 'Every command and response is timestamped and captured in the Logs tab for review and export.'],
    ]),
  },
  cards: { heading: 'Runtime tabs', rail: 'Runtime tabs', lede: 'The console screen has four tabs. Each card links to the section that documents it.', more: 'Read the section', items: CARDS },
  sections: SECTIONS,
  cta: { heading: 'Run it against your own HSM', text: 'Free and open source. Download the studio, start the HSM Simulator as a server and point the console at 127.0.0.1:1500 in minutes.', secondaryLabel: 'HSM Simulator guide', secondaryLink: '/simulator/hsm' },
};
