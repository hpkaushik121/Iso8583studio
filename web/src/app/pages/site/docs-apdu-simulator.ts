import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { SitePage } from './site-page';
import { UiSection } from '../../ui/section';

@Component({
  selector: 'page-docs-apdu-simulator',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiSection],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-apdu-simulator' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<main class="doc-body">
        <div class="breadcrumb">
            <a href="/">Home</a>
            <span class="breadcrumb-sep">/</span>
            <a href="/docs">Documentation</a>
            <span class="breadcrumb-sep">/</span>
            <span>APDU Simulator</span>
        </div>

        <h1 class="page-title">APDU Simulator</h1>
        <p class="page-description">Play the card. Run an EMV card profile in-process, drive a real card through a PC/SC reader, or push responses to STM32 firmware so an external POS terminal reads your card off a pinboard &mdash; with every APDU exchange traced, replayable as a test plan, and exportable as an L3 report.</p>

        <ui-section anchor="overview" heading="Overview">
            <p>The APDU Simulator is the card side of the network. It runs an EMV card profile &mdash; scheme, ATR, applications, records, issuer keys &mdash; and answers ISO 7816-4 APDUs from whatever is asking, whether that is Studio itself, a physical reader, or a real POS terminal talking to emulator firmware.</p>

            <div class="shot-grid">
                <figure class="shot-fig" style="--shot-w:958px">
                    <image-slot><img src="/images/docs/apdu-simulator/overview.png" alt="APDU Simulator window on the Card Session tab in emulate mode, showing the connect controls, the status strip with phase, last AID and exchange count, and the formatted and raw command and response panels" width="1916" height="1005" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">apdu-simulator</span> Card Session &mdash; emulate mode over USB-CDC</figcaption>
                </figure>
            </div>

            <div class="feature-grid">
                <div class="feature-item"><div class="feature-item-icon">🔀</div><h4>Three Operating Modes</h4><p>Loopback in-process, a real card through a PC/SC reader, or card emulation over USB-CDC to STM32 firmware.</p></div>
                <div class="feature-item"><div class="feature-item-icon">💳</div><h4>Card Profiles</h4><p>Scheme, ATR, AIDs, labels, PAN, expiry, CVN, records and issuer keys &mdash; edit, clone and personalise.</p></div>
                <div class="feature-item"><div class="feature-item-icon">▶️</div><h4>Test Plans</h4><p>Run a built-in plan through the connected transport and see pass/fail per case.</p></div>
                <div class="feature-item"><div class="feature-item-icon">✅</div><h4>L3 Report</h4><p>Export the last run in a certified format &mdash; Visa VCPS and Mastercard M/Chip today.</p></div>
                <div class="feature-item"><div class="feature-item-icon">📈</div><h4>Wire Sniff</h4><p>Line a logic-analyzer capture up against the exchange log to check what the firmware really put on the wire.</p></div>
                <div class="feature-item"><div class="feature-item-icon">🔧</div><h4>Firmware</h4><p>Build and flash the stm32-card firmware without leaving the app.</p></div>
            </div>
        </ui-section>

        <ui-section anchor="quick-start" heading="Quick Start">
            <ol class="steps">
                <li><strong>Create a configuration</strong> &mdash; Open <code>APDU Simulator</code> and add one.</li>
                <li><strong>Pick the mode</strong> &mdash; On <strong>Mode &amp; Transport</strong> choose Loopback to start; it needs no hardware.</li>
                <li><strong>Choose a card</strong> &mdash; On <strong>Card Profile</strong> select a built-in profile such as <code>MasterCard Debit (Test)</code>, or clone one and personalise it.</li>
                <li><strong>Launch and connect</strong> &mdash; Open the simulator and press <strong>Connect</strong>. The status strip shows the phase, the last AID selected, and the exchange count.</li>
                <li><strong>Drive it</strong> &mdash; In loopback and reader modes the Card Session tab gives you quick actions and a raw APDU composer; every exchange lands in <strong>Trace Log</strong>.</li>
            </ol>
            <div class="info-card tip">
                <div class="info-card-title">Tip</div>
                <p>Develop test plans and personalisation in Loopback first &mdash; the EMV runtime is the same one the hardware modes drive, so a plan that passes there is ready for the pinboard.</p>
            </div>
        </ui-section>

        <ui-section anchor="modes" heading="Mode &amp; Transport">
            <p>The operating mode decides which role the simulator plays; the transport section below it adapts to that choice.</p>

            <div class="shot-grid">
                <figure class="shot-fig" style="--shot-w:936px">
                    <image-slot><img src="/images/docs/apdu-simulator/mode-transport.png" alt="Mode and Transport tab with the three operating modes listed - loopback, PC/SC reader and card emulator - with card emulator selected, and the STM32 USB-CDC transport showing port, baud rate and an ATR override" width="1483" height="484" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">apdu-simulator</span> Mode &amp; Transport</figcaption>
                </figure>
            </div>

            <div class="table-wrapper">
                <table>
                    <thead><tr><th>Mode</th><th>What Studio is</th><th>Hardware</th></tr></thead>
                    <tbody>
                        <tr>
                            <td><span class="badge badge-blue">Loopback</span></td>
                            <td>Both ends. The EMV runtime runs in-process against the active card profile.</td>
                            <td>None. Ideal for developing test plans and personalisation.</td>
                        </tr>
                        <tr>
                            <td><span class="badge badge-teal">Reader (PC/SC)</span></td>
                            <td>The terminal, driving a physical card.</td>
                            <td>A PC/SC reader such as an ACS ACR39U-I1. The real card is the source of truth &mdash; the card profile is informational only.</td>
                        </tr>
                        <tr>
                            <td><span class="badge badge-yellow">Card emulator</span></td>
                            <td>The card. Studio pushes APDU responses to the firmware over USB-CDC.</td>
                            <td>A Nucleo-L432KC running the stm32-card firmware, emulating a contact card on the XCRFID pinboard so an external POS terminal can read it.</td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <div class="info-card warning">
                <div class="info-card-title">The hardware modes need hardware you supply</div>
                <p><strong>Loopback needs nothing</strong> &mdash; it is where to start. The other two modes each need a piece of kit that does not ship with ISO8583Studio:</p>
                <ul>
                    <li><strong>Reader (PC/SC)</strong> &mdash; any PC/SC contact reader, for example an ACS ACR39U-I1.</li>
                    <li><strong>Card emulator</strong> &mdash; an ST Nucleo-L432KC, an XCRFID 4-in-1 SIM/smart-card pinboard, a USB micro-B cable, and a 3V3&harr;5V level shifter such as a TXS0108E if your terminal drives Class&nbsp;A 5&nbsp;V cards.</li>
                </ul>
                <p>Order the parts yourself &mdash; or build the board in-house. ISO8583Studio is <a href="https://github.com/hpkaushik121/Iso8583studio/blob/main/LICENSE">Apache&nbsp;2.0</a> open source and the emulator firmware ships with it, at <code>firmware/stm32-card/</code> in the repository: the C source, its PlatformIO project, the full bill of materials and the C1&ndash;C8 wiring table from the Nucleo to the ISO 7816 contacts. If you would rather have the board design than a parts list, <a href="https://github.com/hpkaushik121/Iso8583studio/issues">open an issue</a> and ask for it.</p>
            </div>

            <h3>Transport &mdash; STM32 / USB-CDC</h3>
            <div class="spec-list">
                <div class="spec-row"><div class="k">Port</div><div class="v">The serial port the firmware enumerated on. On macOS it appears as <code>/dev/cu.usbmodemXXXX</code> once the firmware boots. <strong>Rescan</strong> re-enumerates.</div></div>
                <div class="spec-row"><div class="k">Baud rate</div><div class="v">Default <code>115200</code>. The firmware framing is binary, so baud affects throughput, not protocol.</div></div>
                <div class="spec-row"><div class="k">ATR override</div><div class="v">Hex ATR presented to the terminal. Leave blank to use the card profile&rsquo;s own ATR.</div></div>
            </div>
        </ui-section>

        <ui-section anchor="card-profile" heading="Card Profile">
            <p>The card the simulator emulates while running. Pick a profile, then <strong>Edit profile</strong> for the full editor, <strong>Clone &amp; personalize</strong> to derive a new card from it, or <strong>New blank</strong> to start from nothing.</p>

            <div class="shot-grid">
                <figure class="shot-fig" style="--shot-w:936px">
                    <image-slot><img src="/images/docs/apdu-simulator/card-profile.png" alt="Card Profile tab with MasterCard Debit (Test) active, and a profile summary listing scheme, ATR, and for application 1 the AID, label, PAN, expiry, CVN, record count and issuer key" width="1492" height="544" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">apdu-simulator</span> Card Profile</figcaption>
                </figure>
            </div>

            <p>The profile summary shows the key fields at a glance:</p>
            <div class="spec-list">
                <div class="spec-row"><div class="k">Scheme</div><div class="v">The payment scheme the card belongs to, e.g. <code>MASTERCARD</code>.</div></div>
                <div class="spec-row"><div class="k">ATR</div><div class="v">Answer To Reset returned on power-up, e.g. <code>3B6500002063CB6800</code>.</div></div>
                <div class="spec-row"><div class="k">AID / Label</div><div class="v">Application Identifier and its label per application &mdash; <code>A0000000041010</code> / <code>DEBIT MASTERCARD</code>.</div></div>
                <div class="spec-row"><div class="k">PAN / Expiry / CVN</div><div class="v">Card number, expiry, and the Cryptogram Version Number selecting the derivation tree.</div></div>
                <div class="spec-row"><div class="k">Records</div><div class="v">How many records the application exposes through <code>READ RECORD</code>.</div></div>
                <div class="spec-row"><div class="k">Issuer keys</div><div class="v">The issuer master keys the card signs with, by key id and type &mdash; e.g. <code>TDES_AC</code> with its UDK.</div></div>
            </div>

            <div class="info-card note">
                <div class="info-card-title">Note</div>
                <p>In <strong>Reader (PC/SC)</strong> mode the profile is informational only. The physical card answers, not this.</p>
            </div>
        </ui-section>

        <ui-section anchor="config-tabs" heading="The Other Configuration Tabs">
            <div class="spec-list">
                <div class="spec-row"><div class="k">Terminal Profile</div><div class="v">The terminal side of the exchange &mdash; what the simulated reader claims about itself when loopback drives a transaction.</div></div>
                <div class="spec-row"><div class="k">Risk &amp; Behavior</div><div class="v">How the card decides, and where it misbehaves on purpose.</div></div>
                <div class="spec-row"><div class="k">Test Plans</div><div class="v">Which plans are available to the runtime&rsquo;s Test Plans tab.</div></div>
            </div>
        </ui-section>

        <ui-section anchor="runtime" heading="The Simulator Window">
            <p>Seven tabs, all live.</p>
            <div class="table-wrapper">
                <table>
                    <thead><tr><th>Tab</th><th>What it does</th></tr></thead>
                    <tbody>
                        <tr><td><strong>Card Session</strong></td><td>The live exchange. In loopback and reader modes it is <em>active</em> &mdash; a quick-action toolbar, a raw APDU composer, and the last exchange split into formatted and raw command and response. In card-emulator mode it is <em>passive</em>: the external terminal drives and Studio answers, so there is no composer.</td></tr>
                        <tr><td><strong>Trace Log</strong></td><td>Every exchange as a log entry, with the same filter, auto-scroll and stats chrome as the other simulators. A non-<code>9000</code> status word is coloured as an error.</td></tr>
                        <tr><td><strong>Test Plans</strong></td><td>Pick a built-in plan, run it through the connected transport, see pass/fail per case, export the report in the scheme&rsquo;s preferred format.</td></tr>
                        <tr><td><strong>Wire Sniff</strong></td><td>Imports a sigrok <code>.sr</code> or CSV capture from a 24&nbsp;MHz logic analyzer and lines it up against the exchange log, so you can confirm the firmware emitted on I/O, CLK, RST and VCC what the runtime says it sent.</td></tr>
                        <tr><td><strong>L3 Report</strong></td><td>Generates a certified-format report from the most recent test-plan run.</td></tr>
                        <tr><td><strong>Firmware</strong></td><td>Builds and flashes the stm32-card firmware, with the build log in the shared log panel.</td></tr>
                        <tr><td><strong>Settings</strong></td><td>A live view of how the running simulator is wired. Changing anything means going back to the configuration screen.</td></tr>
                    </tbody>
                </table>
            </div>

            <h3>Card Session status strip</h3>
            <div class="spec-list">
                <div class="spec-row"><div class="k">Status</div><div class="v">Idle, or connected and exchanging.</div></div>
                <div class="spec-row"><div class="k">Phase</div><div class="v">Which part of the EMV flow the session has reached.</div></div>
                <div class="spec-row"><div class="k">Last AID</div><div class="v">The application most recently selected.</div></div>
                <div class="spec-row"><div class="k">Exchanges</div><div class="v">How many command/response pairs this session has carried.</div></div>
            </div>

            <div class="info-card warning">
                <div class="info-card-title">Two things are still landing</div>
                <p><strong>Hold next APDU</strong> in a passive session is a UI placeholder until the firmware-side interception ships, and <strong>Wire Sniff</strong> imports captures but is not yet a full overlay. <strong>L3 Report</strong> covers Visa VCPS and Mastercard M/Chip; other schemes can run plans but skip the certified export until their templates are in.</p>
            </div>
        </ui-section>

        <ui-section anchor="status-words" heading="Common Status Words">
            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr><th>SW1 SW2</th><th>Meaning</th></tr>
                    </thead>
                    <tbody>
                        <tr><td><code>9000</code></td><td>Success.</td></tr>
                        <tr><td><code>61xx</code></td><td>Response available; <code>xx</code> bytes remain. Issue <code>GET RESPONSE</code>.</td></tr>
                        <tr><td><code>6Cxx</code></td><td>Wrong Le; correct Le is <code>xx</code>.</td></tr>
                        <tr><td><code>6300</code></td><td>Authentication failed (PIN verification with no retry counter).</td></tr>
                        <tr><td><code>63Cx</code></td><td>PIN verification failed; <code>x</code> tries remaining.</td></tr>
                        <tr><td><code>6700</code></td><td>Wrong length.</td></tr>
                        <tr><td><code>6982</code></td><td>Security status not satisfied.</td></tr>
                        <tr><td><code>6985</code></td><td>Conditions of use not satisfied.</td></tr>
                        <tr><td><code>6A82</code></td><td>File or application not found.</td></tr>
                        <tr><td><code>6A86</code></td><td>Incorrect P1 / P2.</td></tr>
                        <tr><td><code>6D00</code></td><td>Instruction code not supported.</td></tr>
                        <tr><td><code>6E00</code></td><td>Class not supported.</td></tr>
                    </tbody>
                </table>
            </div>
        </ui-section>

        <ui-section anchor="tips" heading="Tips &amp; Troubleshooting">
            <ul>
                <li><strong>No serial port listed</strong> &mdash; The firmware has to boot before the device appears. Wait for <code>/dev/cu.usbmodemXXXX</code>, then press <strong>Rescan</strong>.</li>
                <li><strong>No PC/SC readers detected</strong> &mdash; Check the PC/SC service is running and no other smartcard application is holding the reader exclusively.</li>
                <li><strong>ATR mismatch</strong> &mdash; Some terminals validate the ATR against a list. Set an <strong>ATR override</strong> on Mode &amp; Transport to match the card you are impersonating.</li>
                <li><strong>Terminal times out</strong> &mdash; A passive session answers as fast as the firmware relays it; if the terminal still gives up, check the wire capture rather than the runtime.</li>
                <li><strong>Plan passes in loopback but fails on hardware</strong> &mdash; Loopback and the hardware modes share the EMV runtime, so the difference is on the wire. Wire Sniff is the tab for that.</li>
            </ul>
        </ui-section>
    </main>

<aside class="pro-nudge"><span class="pn-tag">✦ Pro</span><p>Testing with a team, or certifying with a scheme? Pro raises the CPS ceiling, unlocks the full algorithm set and deep simulator tweaks, plus hosted endpoints and priority support.</p><a href="/pro">Register for Pro →</a></aside>`,
})
export class DocsApduSimulatorPage {}
