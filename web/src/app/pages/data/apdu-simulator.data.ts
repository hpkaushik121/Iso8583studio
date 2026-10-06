import { SimGuideData, def, shot, shots } from '../shared/simglass-pos/sim-guide';

/**
 * APDU Simulator guide (/simulator/apdu). The glass screens it names are in
 * shared/simglass-pos/apdu-glass.ts (session window) and apdu-glass-cfg.ts
 * (configuration tabs).
 */

const CONFIGURE = '01 · Configure';
const RUN = '02 · Run';

export const APDU_SIMULATOR_GUIDE: SimGuideData = {
  slug: 'apdu-simulator',
  title: 'APDU Simulator',
  meta: 'Loopback · PC/SC · STM32 card emulator',
  lede: 'Emulate an EMV card, drive a real one, or push a card profile to an STM32 board any terminal can read — with every APDU formatted and raw, a trace of the whole session, and scripted test plans from SELECT PSE to GENERATE AC.',
  video: { clip: 'apdu', poster: 'apdu', layout: 'right' },

  intro: {
    eyebrow: 'apdu-simulator · APDU Simulator - Card-1 · MasterCard Debit (Test)',
    paras: [
      'The APDU Simulator is the card side of an EMV transaction. It holds a card profile — ATR, applications, records, issuer keys — and answers the terminal’s commands from it: `SELECT`, `GET PROCESSING OPTIONS`, `READ RECORD`, `VERIFY`, `GENERATE AC`. That card can run in-process, be pushed to an STM32 board that presents a physical contact card to any terminal, or step aside while Studio drives a real card through a PC/SC reader.',
      'Every exchange is shown formatted and raw as it happens, kept in the session trace, and checkable against a scripted test plan.',
    ],
    shot: { id: 'session', sub: 'Card Session', caption: 'Connect, then a contact purchase from SELECT PSE to GENERATE AC — each command parsed to labelled TLV beside its raw hex, the ARQC coming back in the last response' },
    features: [
      ['sim-card', 'Three Operating Modes', 'Loopback in-process, a real card through a PC/SC reader, or the STM32 card emulator over USB-CDC — pick the role and the transport adapts.'],
      ['identification-card', 'Card Profiles', 'Scheme, ATR, applications with AID, label, PAN, expiry, records and issuer keys. Clone the test profile and personalize it, or start blank.'],
      ['storefront', 'Terminal Profiles', 'Terminal type, capabilities, country and currency, floor limit and action codes — the terminal Studio plays when it drives a card.'],
      ['arrows-left-right', 'Formatted and Raw APDUs', 'Every command and response parsed to labelled TLV beside its raw hex, from `SELECT PSE` to `GENERATE AC`.'],
      ['sliders-horizontal', 'Risk and Fault Injection', 'Cryptogram policy, PIN try counter, ATC and offline limits, plus wrong status words, tears and delays on demand.'],
      ['play', 'Test Plans', 'Scripted APDU sequences with expected status words and data checks, run step by step or end to end, with an L3-style report.'],
    ],
  },

  // The five tabs of APDU Simulator Configuration — each card links to the section that documents it.
  tabs: {
    lede: 'APDU Simulator Configuration has five tabs. Each card links to the section that documents it.',
    cards: [
      { id: 'modes', icon: 'plugs-connected', name: 'Mode & Transport', tag: 'Loopback · PC/SC · USB-CDC', desc: 'Which role the simulator plays and how it reaches the hardware — serial port, baud rate and ATR override for the STM32 emulator.' },
      { id: 'card-profile', icon: 'identification-card', name: 'Card Profile', tag: 'ATR · AID · PAN · Issuer keys', desc: 'The card the simulator emulates: scheme, ATR, applications and records, and the issuer master keys behind its cryptograms.' },
      { id: 'terminal', icon: 'storefront', name: 'Terminal Profile', tag: '9F35 · 9F33 · 9F1A · TAC', desc: 'The terminal Studio acts as in loopback and reader modes — type, capabilities, country, currency, floor limit and action codes.' },
      { id: 'risk', icon: 'sliders-horizontal', name: 'Risk & Behavior', tag: 'ARQC / TC / AAC · PTC · ATC', desc: 'How the emulated card decides — cryptogram policy, PIN and counters, issuer authentication, and the faults you want injected.' },
      { id: 'plans', icon: 'play', name: 'Test Plans', tag: 'Steps · Expected SW · Report', desc: 'Scripted sequences of APDUs with the status word and data each step must return, runnable from the session window.' },
    ],
  },

  sections: [
    {
      id: 'quick-start', title: 'Quick Start', rail: 'Quick start', icon: 'lightning', eyebrow: CONFIGURE,
      blocks: [
        { t: 'steps', items: [
          '**Create a card profile** — Open `APDU Simulator` and add a profile in the left rail, or keep `MasterCard Debit (Test)`.',
          '**Pick the mode** — On **Mode & Transport** choose Loopback, Reader (PC/SC) or Card emulator (USB-CDC to STM32). Loopback needs no hardware.',
          '**Pick the transport** — For the card emulator, select the port the firmware enumerated on (`cu.usbmodemXXXX` on macOS) and leave the baud at `115200`.',
          '**Check the profile** — **Card Profile** shows the ATR, AID, PAN and issuer keys the simulator will present; **Clone & personalize** to change them.',
          '**Launch** — **Launch APDU Simulator** opens the session window. **Connect**, then present the pinboard to the terminal — or run a test plan in loopback.',
          '**Read the exchange** — Every APDU lands in the four panes, formatted and raw; **Trace Log** keeps the whole session.',
        ] },
        { t: 'note', tone: 'teal', icon: 'lightbulb', title: 'Tip', x: 'Develop test plans and personalisation in Loopback first — the EMV runtime is the same one the hardware modes drive, so a plan that passes there is ready for the pinboard.' },
      ],
    },
    {
      // The prototype calls this section `mode`; the live page's id for it is `modes`.
      id: 'modes', title: 'Mode & Transport Tab', rail: 'Mode & Transport', icon: 'plugs-connected', eyebrow: CONFIGURE,
      intro: 'Pick the role this simulator plays. The transport section below adapts to it.',
      blocks: [
        shot('mode', 'Mode & Transport', 'the three operating modes, Card emulator selected, and the STM32 transport — port, baud rate and ATR override'),
        { t: 'h3', x: 'Operating modes' },
        { t: 'table', head: ['Mode', 'What happens'], cols: '210px minmax(0,1fr)', rows: [
          ['**Loopback** (software-only)', 'EMV runtime in-process against the active card profile. No hardware. Ideal for developing test plans and personalization.'],
          ['**Reader** (PC/SC)', 'Studio acts as the terminal and drives a physical card through a PC/SC reader such as the ACS ACR39U-I1. The card profile is informational only — the real card is the source of truth.'],
          ['**Card emulator** (USB-CDC to STM32)', 'Studio pushes APDU responses to the Nucleo-L432KC firmware over USB-CDC. The firmware emulates a contact card on the XCRFID pinboard, readable by an external POS terminal.'],
        ] },
        { t: 'custom', data: {
          k: 'listnote', tone: 'warn', icon: 'warning', title: 'The hardware modes need hardware you supply',
          lead: '**Loopback needs nothing** — it is where to start. The other two modes each need a piece of kit that does not ship with ISO8583Studio:',
          items: [
            ['Reader (PC/SC)', 'any PC/SC contact reader, for example an ACS ACR39U-I1.'],
            ['Card emulator', 'an ST Nucleo-L432KC, an XCRFID 4-in-1 SIM/smart-card pinboard, a USB micro-B cable, and a 3V3↔5V level shifter such as a TXS0108E if your terminal drives Class A 5 V cards.'],
          ],
          after: 'Order the parts yourself — or build the board in-house. ISO8583Studio is [AGPL v3](https://github.com/hpkaushik121/Iso8583studio/blob/main/LICENSE) open source and the emulator firmware ships with it, at `firmware/stm32-card/` in the repository: the C source, its PlatformIO project, the full bill of materials and the C1–C8 wiring table from the Nucleo to the ISO 7816 contacts. If you would rather have the board design than a parts list, [open an issue](https://github.com/hpkaushik121/Iso8583studio/issues) and ask for it.',
        } },
        { t: 'h3', x: 'Transport — STM32 / USB-CDC' },
        def([
          ['usb', 'Port', 'The serial port the firmware enumerated on. **Rescan** lists them again after you plug the board in.'],
          ['gauge', 'Baud rate', '`115200` by default. The framing is binary, so the baud only affects throughput, not the protocol.'],
          ['hash', 'ATR override', 'Hex; blank uses the card profile’s ATR. Set it when a terminal insists on a particular Answer To Reset.'],
        ]),
        { t: 'note', tone: 'blue', icon: 'info', title: 'macOS', x: 'The device shows up as `/dev/cu.usbmodemXXXX` after the firmware boots. If it is missing, check the Firmware tab of the session window — the board may still be in DFU mode.' },
      ],
    },
    {
      // The prototype calls this section `profile`; the live page's id for it is `card-profile`.
      id: 'card-profile', title: 'Card Profile Tab', rail: 'Card Profile', icon: 'identification-card', eyebrow: CONFIGURE,
      intro: 'The simulator emulates the active profile when running. The tab shows the key fields; the full editor opens from **Edit profile**.',
      blocks: [
        shot('profile', 'Card Profile', 'the active profile, MasterCard Debit (Test), and its summary — scheme, ATR, the first application and the issuer key it signs with'),
        def([
          ['hash', 'ATR', 'The Answer To Reset sent on power-up — `3B6500002063CB6800` for the Mastercard test profile. Mode & Transport can override it per port.'],
          ['identification-card', 'Applications', 'One or more ADFs, each with AID, label, priority, PAN, expiry, PAN sequence number and CVN — the Cryptogram Version Number selecting the derivation tree. The PSE directory is built from them.'],
          ['rows', 'Records', 'The SFI records returned to `READ RECORD` — CDOL1 and CDOL2, the CVM list, issuer action codes, application usage control and the rest of the application data.'],
          ['key', 'Issuer keys', 'The issuer master keys (`TDES_AC`, `TDES_SMI`, `TDES_SMC`) the card derives its UDKs from, referenced by id such as `mc-imk-1`.'],
        ]),
        { t: 'h3', x: 'Actions' },
        { t: 'bullets', items: [
          ['Edit profile', 'opens the full editor for the active profile — every tag, record and key.'],
          ['Clone & personalize', 'copies the profile and steps through PAN, expiry, name and keys so a test card becomes yours.'],
          ['New blank', 'starts an empty profile.'],
          ['Refresh', 're-reads the profile list from disk after an import.'],
        ] },
        { t: 'note', tone: 'blue', icon: 'info', title: 'Note', x: 'In **Reader (PC/SC)** mode the profile is informational only. The physical card answers, not this.' },
      ],
    },
    {
      // Live-only section: the short account of the three remaining tabs, each now documented in full below.
      id: 'config-tabs', title: 'The Other Configuration Tabs', rail: 'Other tabs', icon: 'list-bullets', eyebrow: CONFIGURE,
      blocks: [
        def([
          ['storefront', 'Terminal Profile', 'The terminal side of the exchange — what the simulated reader claims about itself when loopback drives a transaction. See [Terminal Profile Tab](/simulator/apdu#terminal).'],
          ['sliders-horizontal', 'Risk & Behavior', 'How the card decides, and where it misbehaves on purpose. See [Risk & Behavior Tab](/simulator/apdu#risk).'],
          ['play', 'Test Plans', 'Which plans are available to the runtime’s Test Plans tab. See [Test Plans Tab](/simulator/apdu#plans).'],
        ]),
      ],
    },
    {
      id: 'terminal', title: 'Terminal Profile Tab', rail: 'Terminal Profile', icon: 'storefront', eyebrow: CONFIGURE,
      intro: 'When Studio plays the terminal — in loopback and reader modes — this profile is what the card sees: the PDOL and CDOL data it is answered with, the CVMs the terminal supports, and the action codes that decide offline, online or decline.',
      blocks: [
        shot('terminal', 'Terminal Profile', 'identity, capability bytes with their decoded meaning, floor limit and action codes, and the AIDs the terminal accepts'),
        { t: 'table', head: ['Tag', 'Field', 'What it sets'], cols: '110px 190px minmax(0,1fr)', rows: [
          ['`9F35`', 'Terminal type', '`22` — attended, online capable, operated by the merchant.'],
          ['`9F33`', 'Terminal capabilities', 'Card data input, CVM and security capability bytes — which CVMs and which offline data authentication the terminal claims.'],
          ['`9F40`', 'Additional capabilities', 'Transaction types, input and output capabilities.'],
          ['`9F1A` · `5F2A`', 'Country and currency', '`0840` USD with exponent 2 in the test profile.'],
          ['`9F1B`', 'Floor limit', 'Amounts under it can be approved offline with a TC; over it the TVR bit sends the transaction online.'],
          ['TAC', 'Terminal action codes', 'Denial, Online and Default masks matched against the TVR during terminal action analysis.'],
        ] },
      ],
    },
    {
      id: 'risk', title: 'Risk & Behavior Tab', rail: 'Risk & Behavior', icon: 'sliders-horizontal', eyebrow: CONFIGURE,
      intro: 'How the emulated card decides. Everything here changes what `GENERATE AC` returns, how the card treats PIN, counters and issuer scripts — and what goes wrong when you ask it to.',
      blocks: [
        shot('risk', 'Risk & Behavior', 'cryptogram policy, PIN and counters, issuer authentication and the fault injection switches'),
        def([
          ['lightning', 'Cryptogram policy', 'Follow the terminal’s request, force an ARQC to go online every time, force an AAC to decline, or approve with a TC when the amount is under the floor limit.'],
          ['keyboard', 'PIN and counters', 'Offline PIN with a reference value and try counter, the starting ATC, and the lower and upper consecutive offline limits (`9F14`, `9F23`).'],
          ['shield-check', 'Issuer authentication', 'Verify the ARPC in `EXTERNAL AUTHENTICATE`, accept or refuse issuer scripts (`71` / `72`), and optionally require a valid ARPC before returning a TC.'],
          ['bug', 'Fault injection', 'A response delay, a wrong status word on a chosen command, a tear at `GENERATE AC`, or one corrupted cryptogram byte — to see how the terminal copes.'],
        ]),
      ],
    },
    {
      id: 'plans', title: 'Test Plans Tab', rail: 'Test Plans', icon: 'play', eyebrow: CONFIGURE,
      intro: 'A test plan is a scripted sequence of APDUs with the status word and data each step must return. Plans are stored with the profile and run from the session window, step by step or end to end.',
      blocks: [
        shot('plans', 'Test Plans', 'the built-in plans and the steps of the online ARQC purchase passing one by one'),
        { t: 'bullets', items: [
          ['Step', 'a command by name or raw hex, with the fields the simulator fills at run time — amount, date, unpredictable number.'],
          ['Expect SW', 'the status word the step must return; anything else fails the plan at that step.'],
          ['Expect data', 'a tag that must be present, or a tag with an exact value — `82 = 18 00`, `9F27 = 80`.'],
          ['Report', 'each run ends in a pass / fail line per step with timing, and feeds the L3 Report tab.'],
        ] },
        { t: 'note', tone: 'teal', icon: 'lightbulb', title: 'Tip', x: 'Start from the built-in plans — online ARQC purchase, offline TC under the floor limit, PIN try counter exhaustion, unknown AID — and clone one to add your own assertions.' },
      ],
    },
    {
      // The prototype calls this section `session-window`; the live page's id for it is `runtime`.
      // The tab descriptions, the status strip and the "still landing" note are the live page's.
      id: 'runtime', title: 'The Session Window', rail: 'Session window', icon: 'app-window', eyebrow: RUN,
      intro: '**Launch APDU Simulator** opens the session window for the profile. Seven tabs, all live:',
      blocks: [
        { t: 'table', head: ['Tab', 'What it does'], cols: '150px minmax(0,1fr)', rows: [
          ['**Card Session**', 'The live exchange. In loopback and reader modes it is *active* — a quick-action toolbar, a raw APDU composer, and the last exchange split into formatted and raw command and response. In card-emulator mode it is *passive*: the external terminal drives and Studio answers, so there is no composer.'],
          ['**Trace Log**', 'Every exchange as a log entry, with the same filter, auto-scroll and stats chrome as the other simulators. A non-`9000` status word is coloured as an error.'],
          ['**Test Plans**', 'Pick a built-in plan, run it through the connected transport, see pass/fail per case, export the report in the scheme’s preferred format.'],
          ['**Wire Sniff**', 'Imports a sigrok `.sr` or CSV capture from a 24 MHz logic analyzer and lines it up against the exchange log, so you can confirm the firmware emitted on I/O, CLK, RST and VCC what the runtime says it sent.'],
          ['**L3 Report**', 'Generates a certified-format report from the most recent test-plan run.'],
          ['**Firmware**', 'Builds and flashes the stm32-card firmware, with the build log in the shared log panel.'],
          ['**Settings**', 'A live view of how the running simulator is wired. Changing anything means going back to the configuration screen.'],
        ] },
        shots('Session window tabs', [
          { id: 'trace', icon: 'file-text', label: 'Trace Log', short: 'Trace', caption: 'the seven exchanges of a contact purchase arriving in order, with the L2 phases ticking off above them' },
          { id: 'sniff', icon: 'wifi-high', label: 'Wire Sniff', short: 'Sniff', caption: 'the USB-CDC link frame by frame — hello, ATR, reset, then every C-APDU in and R-APDU out with sizes and timing' },
          { id: 'l3', icon: 'seal-check', label: 'L3 Report', short: 'L3', caption: 'the ten checks of the last transaction, eight passed, offline data authentication skipped and online processing noted' },
          { id: 'firmware', icon: 'cpu', label: 'Firmware', caption: 'the NUCLEO-L432KC board, its pinboard map, and a firmware update flashed and verified from Studio' },
        ]),
        { t: 'h3', x: 'Card Session status strip' },
        { t: 'table', head: ['Field', 'What it shows'], cols: '150px minmax(0,1fr)', rows: [
          ['Status', 'Idle, or connected and exchanging.'],
          ['Phase', 'Which part of the EMV flow the session has reached.'],
          ['Last AID', 'The application most recently selected.'],
          ['Exchanges', 'How many command/response pairs this session has carried.'],
        ] },
        { t: 'note', tone: 'warn', icon: 'warning', title: 'Two things are still landing', x: '**Hold next APDU** in a passive session is a UI placeholder until the firmware-side interception ships, and **Wire Sniff** imports captures but is not yet a full overlay. **L3 Report** covers Visa VCPS and Mastercard M/Chip; other schemes can run plans but skip the certified export until their templates are in.' },
        { t: 'note', tone: 'blue', icon: 'link', title: 'Note', x: 'For the terminal side of the same transaction, pair this with the [POS Simulator](/simulator/pos); for key derivation and cryptogram checks, use the [EMV & Card Tools](/tools/emv-tools).' },
      ],
    },
    {
      // Live-only reference section.
      id: 'status-words', title: 'Common Status Words', rail: 'Status words', icon: 'list-numbers',
      blocks: [
        { t: 'table', head: ['SW1 SW2', 'Meaning'], cols: '130px minmax(0,1fr)', rows: [
          ['`9000`', 'Success.'],
          ['`61xx`', 'Response available; `xx` bytes remain. Issue `GET RESPONSE`.'],
          ['`6Cxx`', 'Wrong Le; correct Le is `xx`.'],
          ['`6300`', 'Authentication failed (PIN verification with no retry counter).'],
          ['`63Cx`', 'PIN verification failed; `x` tries remaining.'],
          ['`6700`', 'Wrong length.'],
          ['`6982`', 'Security status not satisfied.'],
          ['`6985`', 'Conditions of use not satisfied.'],
          ['`6A82`', 'File or application not found.'],
          ['`6A86`', 'Incorrect P1 / P2.'],
          ['`6D00`', 'Instruction code not supported.'],
          ['`6E00`', 'Class not supported.'],
        ] },
      ],
    },
    {
      // Live-only section.
      id: 'tips', title: 'Tips & Troubleshooting', rail: 'Tips', icon: 'lightbulb',
      blocks: [
        { t: 'custom', data: { k: 'tips', items: [
          '**No serial port listed** — The firmware has to boot before the device appears. Wait for `/dev/cu.usbmodemXXXX`, then press **Rescan**.',
          '**No PC/SC readers detected** — Check the PC/SC service is running and no other smartcard application is holding the reader exclusively.',
          '**ATR mismatch** — Some terminals validate the ATR against a list. Set an **ATR override** on Mode & Transport to match the card you are impersonating.',
          '**Terminal times out** — A passive session answers as fast as the firmware relays it; if the terminal still gives up, check the wire capture rather than the runtime.',
          '**Plan passes in loopback but fails on hardware** — Loopback and the hardware modes share the EMV runtime, so the difference is on the wire. Wire Sniff is the tab for that.',
        ] } },
      ],
    },
  ],

  cta: {
    heading: 'Try it on your own cards',
    text: 'Free and open source. Download the studio and run this simulator on your desk in minutes.',
  },
};
