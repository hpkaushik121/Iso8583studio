import { SimGuideData, def, shot, shots } from '../shared/simglass-pos/sim-guide';

/** POS Simulator guide (/simulator/pos). The glass screens it names are in shared/simglass-pos/pos-glass.ts. */

const CONFIGURE = '01 · Configure';
const RUN = '02 · Run';

export const POS_SIMULATOR_GUIDE: SimGuideData = {
  slug: 'pos-simulator',
  title: 'POS Simulator',
  meta: 'PAX · Ingenico · Sunmi · Verifone · Kozen · Newland',
  lede: 'Boot a real Android terminal in an emulator — a PAX, Ingenico, Sunmi, Verifone or Kozen device with its own screen, memory, peripherals and spoofed build identity — and run your payment app against it without the hardware on your desk.',
  video: { clip: 'pos', poster: 'pos' },

  intro: {
    eyebrow: 'pos-simulator · POS Terminal - POS - 1 · PAX A910S',
    paras: [
      'The POS Simulator boots an Android Virtual Device shaped like a specific payment terminal. You pick a vendor, model and variant; the simulator creates the AVD, applies the device’s screen, memory and peripheral profile, spoofs its `ro.product.*` build identity, and powers it on. Your APK then runs believing it is on that terminal.',
      'It is a development and debugging rig, not a certification tool — but it is the real Android emulator underneath, so the app under test behaves as it would on the device.',
    ],
    shot: { id: 'overview', sub: 'Device', caption: 'prerequisites, resolved device and spoofed identity — Power on creates and boots the PAX A910S AVD, then Install APK… pushes the build' },
    features: [
      ['device-mobile', 'Terminal Catalog', 'PAX, Ingenico, Kozen, Newland, Sunmi, Verifone, NexGo, Castles and Telpo models, plus generic shapes, each with its own variants.'],
      ['sliders-horizontal', 'Real Emulator Hardware', 'Memory, display, graphics, input, cameras and sensors written straight into the AVD’s `config.ini`.'],
      ['identification-badge', 'Spoofed Identity', '`ro.product.manufacturer`, `brand`, `model`, `device` and `name` set to the target terminal’s values.'],
      ['printer', 'Peripheral Profile', 'Thermal printer, barcode scanner and PIN entry device described per model — dots per line, paper width, PIN block formats.'],
      ['check-square', 'Prerequisite Checks', 'SDK, emulator binary, command-line tools, adb, system image and host ABI all validated before boot.'],
      ['credit-card', 'Card Source', 'Software card profile, a real card through a PC/SC reader, or a serial link — configured per terminal.'],
    ],
  },

  // The five configuration tabs of POS Terminal Configuration — each card links to the section that documents it.
  tabs: {
    lede: 'POS Terminal Configuration has five tabs. Each card links to the section that documents it.',
    cards: [
      { id: 'device', icon: 'device-mobile', name: 'Device', tag: 'Vendor · Model · Variant', desc: 'Which terminal the emulator will pretend to be — vendor filter, model grid, variant chooser and the resolved device summary.' },
      { id: 'hardware', icon: 'sliders-horizontal', name: 'Hardware', tag: 'config.ini · hardware-properties.ini', desc: 'Memory, display, graphics, input, cameras and sensors — every control writes a real AVD key.' },
      { id: 'peripherals', icon: 'printer', name: 'Peripherals', tag: 'Printer · Scanner · PED', desc: 'What the terminal has bolted to it beyond the Android hardware; confirm or override the model’s profile.' },
      { id: 'system-boot', icon: 'power', name: 'System & Boot', tag: 'SDK · System image · AVD', desc: 'Where the Android toolchain lives, how the AVD boots, and the spoofed build identity applied after boot.' },
      { id: 'card-host', icon: 'credit-card', name: 'Card & Host', tag: 'Card source · Acquirer host', desc: 'Where card data comes from — software profile, PC/SC, serial or loopback — and where transactions are meant to go.' },
    ],
  },

  sections: [
    {
      id: 'quick-start', title: 'Quick Start', rail: 'Quick start', icon: 'lightning', eyebrow: CONFIGURE,
      blocks: [
        { t: 'steps', items: [
          '**Create a terminal profile** — Open `POS Simulator` and add a profile in the left rail.',
          '**Pick the device** — On the **Device** tab choose a manufacturer, a model, and the variant matching the unit you are targeting.',
          '**Adjust hardware if needed** — The **Hardware** tab carries the model’s defaults; override only what you need.',
          '**Point at your SDK** — On **System & Boot** confirm the Android SDK, system image and AVD name.',
          '**Launch** — **Launch POS Simulator** opens the terminal window. Check the green prerequisites list, then **Power on** — the AVD is created if needed, booted, and given the device identity.',
          '**Install your app** — **Install APK…** pushes your build onto the running terminal.',
        ] },
        { t: 'note', tone: 'teal', icon: 'lightbulb', title: 'Tip', x: 'Boot is fastest when the system image ABI matches your host — the prerequisites list reports `arm64-v8a — hardware accelerated` when it does.' },
      ],
    },
    {
      id: 'device', title: 'Device Tab', rail: 'Device', icon: 'device-mobile', eyebrow: CONFIGURE,
      intro: 'The Device tab picks which terminal the emulator will pretend to be. It has four parts: a **Vendor** filter with a model/variant/SKU search, the **Model** grid, the **Variant** chooser, and a **Resolved device** summary of what the AVD will actually be built from.',
      blocks: [
        shot('device', 'Device', 'vendor, model and variant — PAX A910S selected from the catalog, its 5" HD · Android 12 variant verified from a real device, the other variants flagged Unverified'),
        { t: 'h3', x: 'Vendors in the catalog' },
        { t: 'p', x: 'PAX, Ingenico, Kozen, Newland, Sunmi, Verifone, NexGo, Castles Technology, Telpo, plus two generic shapes — a 720×1440 Android terminal and a 480×480 square one.' },
        { t: 'h3', x: 'Variants' },
        { t: 'p', x: 'Variants of one model differ in screen, memory, peripherals or Android version. Each carries a confidence badge:' },
        { t: 'table', head: ['Badge', 'Meaning'], cols: '150px minmax(0,1fr)', rows: [
          ['**Verified**', 'The numbers came from a real adb probe against the hardware.'],
          ['**Unverified**', 'Derived from a datasheet. Vendors publish “5″ HD” and rarely the exact pixel dimensions or density bucket, so these are flagged rather than presented as fact.'],
          ['**Hardware only**', 'The model has a hardware profile but no payment-SDK integration yet.'],
        ] },
      ],
    },
    {
      id: 'hardware', title: 'Hardware Tab', rail: 'Hardware', icon: 'sliders-horizontal', eyebrow: CONFIGURE,
      intro: 'Every control here writes an actual key into the AVD’s `config.ini`, driven by the emulator’s own `hardware-properties.ini` schema — the property name sits under each label so you can see exactly what is being set. Values come from the selected model; anything you change is stored as an override and can be reset. A validation strip at the top of the tab reports problems at edit time rather than as a failed boot ten minutes later.',
      blocks: [
        shots('Hardware groups', [
          { id: 'hw-memory', icon: 'memory', label: 'Memory & Storage', short: 'Memory', caption: 'RAM 2048, VM heap 0, internal storage 6G, SD card size 512M and SD card present enabled' },
          { id: 'hw-display', icon: 'monitor', label: 'Display', caption: 'screen width 720, height 1280, density 320 dpi, 16-bit colour depth, portrait orientation and a multi-touch screen' },
          { id: 'hw-graphics', icon: 'cpu', label: 'Graphics & CPU', short: 'Graphics', caption: 'hardware GPU enabled, GPU mode auto and 8 CPU cores' },
          { id: 'hw-input', icon: 'hand-tap', label: 'Input', caption: 'hardware keyboard on, hardware back and home keys, D-pad and trackball off' },
          { id: 'hw-cameras', icon: 'camera', label: 'Cameras', caption: 'the rear and front cameras both set to emulated' },
          { id: 'hw-sensors', icon: 'broadcast', label: 'Sensors', caption: 'accelerometer, gyroscope, GPS, battery, microphone, proximity and light, each labelled with its hw property key' },
        ]),
        { t: 'note', tone: 'blue', icon: 'info', title: 'Note', x: 'Some changes can be patched into an existing AVD; others force a destructive recreate. The tab tells you which before you commit.' },
      ],
    },
    {
      id: 'peripherals', title: 'Peripherals Tab', rail: 'Peripherals', icon: 'printer', eyebrow: CONFIGURE,
      intro: 'What the terminal has bolted to it, beyond the Android hardware. The model supplies these; the tab lets you confirm or override them.',
      blocks: [
        def([
          ['list-checks', 'Peripherals', 'The device’s feature summary — contact and contactless readers, magnetic stripe, NFC, SAM slots, status LEDs, beeper, camera, cellular, GPS, Wi-Fi.'],
          ['printer', 'Thermal printer', 'Present, dots per line, paper width, greyscale support.'],
          ['barcode', 'Barcode scanner', 'Present or absent.'],
          ['keyboard', 'PIN entry (PED)', 'Supported PIN block formats, DUKPT support, key slots, offline PIN.'],
        ]),
      ],
    },
    {
      id: 'system-boot', title: 'System & Boot Tab', rail: 'System & Boot', icon: 'power', eyebrow: CONFIGURE,
      intro: 'Where the Android toolchain lives and how the AVD boots.',
      blocks: [
        def([
          ['android-logo', 'Android SDK', 'SDK root and how it was found. The tab reports when all required tools are present.'],
          ['hard-drives', 'System image', 'API level, tag (for example `google_apis`) and ABI, checked against the host ABI so you know whether boot will be hardware accelerated.'],
          ['power', 'AVD & boot', 'AVD name and AVD home, plus cold boot every start, headless (`-no-window`), writable `/system` and SELinux permissive.'],
          ['identification-badge', 'Spoofed device identity', 'The `ro.product.manufacturer`, `brand`, `model`, `device` and `name` values applied after boot — what an app reads when it asks which terminal it is running on.'],
        ]),
      ],
    },
    {
      id: 'card-host', title: 'Card & Host Tab', rail: 'Card & Host', icon: 'credit-card', eyebrow: CONFIGURE,
      intro: 'Where card data comes from, and where transactions are meant to go.',
      blocks: [
        def([
          ['credit-card', 'Card source', 'A software card profile, a real card through a PC/SC reader, a serial link, or loopback.'],
          ['identification-card', 'Software card profile', 'The card the terminal sees when the source is software.'],
          ['usb', 'PC/SC reader', 'Reader selection. The tab says so plainly when no PC/SC readers are attached to the machine.'],
          ['plug', 'Serial port', 'Port and baud rate.'],
          ['globe', 'Acquirer host', 'Host, port, Terminal ID and Merchant ID for the outgoing link.'],
        ]),
        { t: 'note', tone: 'warn', icon: 'warning', title: 'Host uplink is not wired up yet', x: 'The acquirer host fields are stored on the profile, but the terminal does not yet send ISO 8583 to them — that arrives with the ISO 8583 uplink milestone. To exercise a host today, drive the [Host Simulator](/simulator/host) directly.' },
      ],
    },
    {
      // The prototype calls this section `terminal-window`; the live page's id for it is `runtime`.
      id: 'runtime', title: 'The Terminal Window', rail: 'Terminal window', icon: 'app-window', eyebrow: RUN,
      intro: '**Launch POS Simulator** opens the terminal itself. Two tabs are live today; the rest are placeholders that name what they will hold and which milestone brings them.',
      blocks: [
        { t: 'custom', data: { k: 'states', head: ['Tab', 'State', 'What it holds'], cols: '130px 96px minmax(0,1fr)', rows: [
          ['**Device**', 'Live', 'Power on / Prepare AVD / Install APK, the prerequisites checklist, the resolved device summary and the spoofed identity.'],
          ['**Card**', 'Live', 'The card the terminal will present, from the configured card source.'],
          ['SDK Trace', 'Pending', 'Every DAL call the payment app makes — interface, method, arguments, latency, result — with the matching APDU exchange beside it.'],
          ['PIN Pad', 'Pending', 'Soft keypad, PIN block format selector, and the live clear block, encrypted block, KSN and KCV.'],
          ['Receipts', 'Pending', 'A thermal-paper roll rendering what the app printed, at this device’s dots per line, with paper-out and overheat fault injection.'],
          ['Scanner', 'Pending', 'Barcode injection into the running app, with presets, history and a queue for scripted runs.'],
          ['Transactions', 'Pending', 'Completed transactions with their EMV tags and the ISO 8583 request and response that carried them.'],
          ['Scenarios', 'Pending', 'Fault injection — card yanked mid-transaction, comm errors, PIN timeout, paper out, host no-response — plus scripted runs with deterministic replay.'],
          ['Logs', 'Pending', 'Boot log, bridge frames and DAL calls in the shared log panel with filtering and auto-scroll.'],
        ] } },
        { t: 'note', tone: 'blue', icon: 'link', title: 'Note', x: 'For chip-level cryptogram and TLV work today, pair the terminal with the [APDU Simulator](/simulator/apdu) and the [EMV & Card Tools](/tools/emv-tools).' },
      ],
    },
  ],

  cta: {
    heading: 'Try it on your own transactions',
    text: 'Free and open source. Download the studio and run this simulator on your desk in minutes.',
  },
};
