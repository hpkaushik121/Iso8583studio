import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { SitePage } from './site-page';

@Component({
  selector: 'page-docs-pos-simulator',
  changeDetection: ChangeDetectionStrategy.OnPush,
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-pos-simulator' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<main class="doc-body">
        <div class="breadcrumb">
            <a href="/">Home</a><span class="breadcrumb-sep">/</span>
            <a href="/docs">Documentation</a><span class="breadcrumb-sep">/</span>
            <span>POS Simulator</span>
        </div>

        <div class="page-title-row">
            <h1 class="page-title">POS Simulator</h1>
            <span class="badge badge-teal">Available</span>
        </div>
        <p class="page-description">Boot a real Android terminal in an emulator &mdash; a PAX, Ingenico, Sunmi, Verifone or Kozen device with its own screen, memory, peripherals and spoofed build identity &mdash; and run your payment app against it without the hardware on your desk.</p>

        <section class="doc-section" id="overview">
            <h2>Overview</h2>
            <p>The POS Simulator boots an Android Virtual Device shaped like a specific payment terminal. You pick a vendor, model and variant; the simulator creates the AVD, applies the device&rsquo;s screen, memory and peripheral profile, spoofs its <code>ro.product.*</code> build identity, and powers it on. Your APK then runs believing it is on that terminal.</p>
            <p>It is a development and debugging rig, not a certification tool &mdash; but it is the real Android emulator underneath, so the app under test behaves as it would on the device.</p>

            <div class="shot-grid">
                <figure class="shot-fig" style="--shot-w:961px">
                    <image-slot><img src="/images/docs/pos-simulator/overview.png" alt="POS Terminal window on the Device tab: a PAX A910S profile with Power on, Prepare AVD and Install APK actions, a green prerequisites checklist, the resolved device table and the spoofed ro.product identity" width="1922" height="834" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">pos-simulator</span> Device tab &mdash; prerequisites, resolved device and spoofed identity</figcaption>
                </figure>
            </div>

            <div class="feature-grid">
                <div class="feature-item"><div class="feature-item-icon">📱</div><h4>Terminal Catalog</h4><p>PAX, Ingenico, Kozen, Newland, Sunmi, Verifone, NexGo, Castles and Telpo models, plus generic shapes, each with its own variants.</p></div>
                <div class="feature-item"><div class="feature-item-icon">🎛️</div><h4>Real Emulator Hardware</h4><p>Memory, display, graphics, input, cameras and sensors written straight into the AVD&rsquo;s <code>config.ini</code>.</p></div>
                <div class="feature-item"><div class="feature-item-icon">🆔</div><h4>Spoofed Identity</h4><p><code>ro.product.manufacturer</code>, <code>brand</code>, <code>model</code>, <code>device</code> and <code>name</code> set to the target terminal&rsquo;s values.</p></div>
                <div class="feature-item"><div class="feature-item-icon">🖨️</div><h4>Peripheral Profile</h4><p>Thermal printer, barcode scanner and PIN entry device described per model &mdash; dots per line, paper width, PIN block formats.</p></div>
                <div class="feature-item"><div class="feature-item-icon">✅</div><h4>Prerequisite Checks</h4><p>SDK, emulator binary, command-line tools, adb, system image and host ABI all validated before boot.</p></div>
                <div class="feature-item"><div class="feature-item-icon">💳</div><h4>Card Source</h4><p>Software card profile, a real card through a PC/SC reader, or a serial link &mdash; configured per terminal.</p></div>
            </div>
        </section>

        <section class="doc-section" id="quick-start">
            <h2>Quick Start</h2>
            <ol class="steps">
                <li><strong>Create a terminal profile</strong> &mdash; Open <code>POS Simulator</code> and add a profile in the left rail.</li>
                <li><strong>Pick the device</strong> &mdash; On the <strong>Device</strong> tab choose a manufacturer, a model, and the variant matching the unit you are targeting.</li>
                <li><strong>Adjust hardware if needed</strong> &mdash; The <strong>Hardware</strong> tab carries the model&rsquo;s defaults; override only what you need.</li>
                <li><strong>Point at your SDK</strong> &mdash; On <strong>System &amp; Boot</strong> confirm the Android SDK, system image and AVD name.</li>
                <li><strong>Launch</strong> &mdash; <strong>Launch POS Simulator</strong> opens the terminal window. Check the green prerequisites list, then <strong>Power on</strong> &mdash; the AVD is created if needed, booted, and given the device identity.</li>
                <li><strong>Install your app</strong> &mdash; <strong>Install APK&hellip;</strong> pushes your build onto the running terminal.</li>
            </ol>
            <div class="info-card tip">
                <div class="info-card-title">Tip</div>
                <p>Boot is fastest when the system image ABI matches your host &mdash; the prerequisites list reports <code>arm64-v8a &mdash; hardware accelerated</code> when it does.</p>
            </div>
        </section>

        <section class="doc-section" id="device">
            <h2>Device Tab</h2>
            <p>The Device tab picks which terminal the emulator will pretend to be. It has four parts: a <strong>Vendor</strong> filter with a model/variant/SKU search, the <strong>Model</strong> grid, the <strong>Variant</strong> chooser, and a <strong>Resolved device</strong> summary of what the AVD will actually be built from.</p>

            <div class="shot-grid">
                <figure class="shot-fig" style="--shot-w:960px">
                    <image-slot><img src="/images/docs/pos-simulator/device.png" alt="POS Terminal Configuration on the Device tab, showing the vendor filter, a grid of terminal models from PAX, Ingenico, Kozen, Newland, Sunmi, Verifone, NexGo, Castles and Telpo, and the variant chooser with verified and unverified badges" width="1920" height="1052" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">pos-simulator</span> Device tab &mdash; vendor, model and variant</figcaption>
                </figure>
            </div>

            <h3>Vendors in the catalog</h3>
            <p>PAX, Ingenico, Kozen, Newland, Sunmi, Verifone, NexGo, Castles Technology, Telpo, plus two generic shapes &mdash; a 720&times;1440 Android terminal and a 480&times;480 square one.</p>

            <h3>Variants</h3>
            <p>Variants of one model differ in screen, memory, peripherals or Android version. Each carries a confidence badge:</p>
            <div class="table-wrapper">
                <table>
                    <thead><tr><th>Badge</th><th>Meaning</th></tr></thead>
                    <tbody>
                        <tr><td><strong>Verified</strong></td><td>The numbers came from a real adb probe against the hardware.</td></tr>
                        <tr><td><strong>Unverified</strong></td><td>Derived from a datasheet. Vendors publish &ldquo;5&Prime; HD&rdquo; and rarely the exact pixel dimensions or density bucket, so these are flagged rather than presented as fact.</td></tr>
                        <tr><td><strong>Hardware only</strong></td><td>The model has a hardware profile but no payment-SDK integration yet.</td></tr>
                    </tbody>
                </table>
            </div>
        </section>

        <section class="doc-section" id="hardware">
            <h2>Hardware Tab</h2>
            <p>Every control here writes an actual key into the AVD&rsquo;s <code>config.ini</code>, driven by the emulator&rsquo;s own <code>hardware-properties.ini</code> schema &mdash; the property name sits under each label so you can see exactly what is being set. Values come from the selected model; anything you change is stored as an override and can be reset. A validation strip at the top of the tab reports problems at edit time rather than as a failed boot ten minutes later.</p>

            <div class="shot-grid">
                <figure class="shot-fig" style="--shot-w:936px">
                    <image-slot><img src="/images/docs/pos-simulator/hw-memory.png" alt="Memory and storage settings: RAM 2048, VM heap 0, internal storage 6G, SD card size 512M and SD card present enabled" width="1493" height="432" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">pos-simulator</span> Memory &amp; Storage</figcaption>
                </figure>
                <figure class="shot-fig" style="--shot-w:936px">
                    <image-slot><img src="/images/docs/pos-simulator/hw-display.png" alt="Display settings: screen width 720, height 1280, density 320 dpi, 16-bit colour depth, portrait orientation and a multi-touch screen" width="1491" height="517" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">pos-simulator</span> Display</figcaption>
                </figure>
                <figure class="shot-fig" style="--shot-w:936px">
                    <image-slot><img src="/images/docs/pos-simulator/hw-graphics.png" alt="Graphics and CPU settings: hardware GPU enabled, GPU mode auto and 8 CPU cores" width="1490" height="295" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">pos-simulator</span> Graphics &amp; CPU</figcaption>
                </figure>
                <figure class="shot-fig" style="--shot-w:936px">
                    <image-slot><img src="/images/docs/pos-simulator/hw-input.png" alt="Input settings: hardware keyboard on, hardware back and home keys, D-pad and trackball off" width="1492" height="307" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">pos-simulator</span> Input</figcaption>
                </figure>
                <figure class="shot-fig" style="--shot-w:936px">
                    <image-slot><img src="/images/docs/pos-simulator/hw-cameras.png" alt="Camera settings with the rear and front cameras both set to emulated" width="1498" height="238" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">pos-simulator</span> Cameras</figcaption>
                </figure>
                <figure class="shot-fig" style="--shot-w:936px">
                    <image-slot><img src="/images/docs/pos-simulator/hw-sensors.png" alt="Sensor toggles for accelerometer, gyroscope, GPS, battery, microphone, proximity and light, each labelled with its hw property key" width="1493" height="478" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">pos-simulator</span> Sensors</figcaption>
                </figure>
            </div>

            <div class="info-card note">
                <div class="info-card-title">Note</div>
                <p>Some changes can be patched into an existing AVD; others force a destructive recreate. The tab tells you which before you commit.</p>
            </div>
        </section>

        <section class="doc-section" id="peripherals">
            <h2>Peripherals Tab</h2>
            <p>What the terminal has bolted to it, beyond the Android hardware. The model supplies these; the tab lets you confirm or override them.</p>
            <div class="spec-list">
                <div class="spec-row"><div class="k">Peripherals</div><div class="v">The device&rsquo;s feature summary &mdash; contact and contactless readers, magnetic stripe, NFC, SAM slots, status LEDs, beeper, camera, cellular, GPS, Wi-Fi.</div></div>
                <div class="spec-row"><div class="k">Thermal printer</div><div class="v">Present, dots per line, paper width, greyscale support.</div></div>
                <div class="spec-row"><div class="k">Barcode scanner</div><div class="v">Present or absent.</div></div>
                <div class="spec-row"><div class="k">PIN entry (PED)</div><div class="v">Supported PIN block formats, DUKPT support, key slots, offline PIN.</div></div>
            </div>
        </section>

        <section class="doc-section" id="system-boot">
            <h2>System &amp; Boot Tab</h2>
            <p>Where the Android toolchain lives and how the AVD boots.</p>
            <div class="spec-list">
                <div class="spec-row"><div class="k">Android SDK</div><div class="v">SDK root and how it was found. The tab reports when all required tools are present.</div></div>
                <div class="spec-row"><div class="k">System image</div><div class="v">API level, tag (for example <code>google_apis</code>) and ABI, checked against the host ABI so you know whether boot will be hardware accelerated.</div></div>
                <div class="spec-row"><div class="k">AVD &amp; boot</div><div class="v">AVD name and AVD home, plus cold boot every start, headless (<code>-no-window</code>), writable <code>/system</code> and SELinux permissive.</div></div>
                <div class="spec-row"><div class="k">Spoofed device identity</div><div class="v">The <code>ro.product.manufacturer</code>, <code>brand</code>, <code>model</code>, <code>device</code> and <code>name</code> values applied after boot &mdash; what an app reads when it asks which terminal it is running on.</div></div>
            </div>
        </section>

        <section class="doc-section" id="card-host">
            <h2>Card &amp; Host Tab</h2>
            <p>Where card data comes from, and where transactions are meant to go.</p>
            <div class="spec-list">
                <div class="spec-row"><div class="k">Card source</div><div class="v">A software card profile, a real card through a PC/SC reader, a serial link, or loopback.</div></div>
                <div class="spec-row"><div class="k">Software card profile</div><div class="v">The card the terminal sees when the source is software.</div></div>
                <div class="spec-row"><div class="k">PC/SC reader</div><div class="v">Reader selection. The tab says so plainly when no PC/SC readers are attached to the machine.</div></div>
                <div class="spec-row"><div class="k">Serial port</div><div class="v">Port and baud rate.</div></div>
                <div class="spec-row"><div class="k">Acquirer host</div><div class="v">Host, port, Terminal ID and Merchant ID for the outgoing link.</div></div>
            </div>
            <div class="info-card warning">
                <div class="info-card-title">Host uplink is not wired up yet</div>
                <p>The acquirer host fields are stored on the profile, but the terminal does not yet send ISO 8583 to them &mdash; that arrives with the ISO 8583 uplink milestone. To exercise a host today, drive the <a href="/docs/host-simulator">Host Simulator</a> directly.</p>
            </div>
        </section>

        <section class="doc-section" id="runtime">
            <h2>The Terminal Window</h2>
            <p><strong>Launch POS Simulator</strong> opens the terminal itself. Two tabs are live today; the rest are placeholders that name what they will hold and which milestone brings them.</p>
            <div class="table-wrapper">
                <table>
                    <thead><tr><th>Tab</th><th>State</th><th>What it holds</th></tr></thead>
                    <tbody>
                        <tr><td><strong>Device</strong></td><td>Live</td><td>Power on / Prepare AVD / Install APK, the prerequisites checklist, the resolved device summary and the spoofed identity.</td></tr>
                        <tr><td><strong>Card</strong></td><td>Live</td><td>The card the terminal will present, from the configured card source.</td></tr>
                        <tr><td>SDK Trace</td><td>Pending</td><td>Every DAL call the payment app makes &mdash; interface, method, arguments, latency, result &mdash; with the matching APDU exchange beside it.</td></tr>
                        <tr><td>PIN Pad</td><td>Pending</td><td>Soft keypad, PIN block format selector, and the live clear block, encrypted block, KSN and KCV.</td></tr>
                        <tr><td>Receipts</td><td>Pending</td><td>A thermal-paper roll rendering what the app printed, at this device&rsquo;s dots per line, with paper-out and overheat fault injection.</td></tr>
                        <tr><td>Scanner</td><td>Pending</td><td>Barcode injection into the running app, with presets, history and a queue for scripted runs.</td></tr>
                        <tr><td>Transactions</td><td>Pending</td><td>Completed transactions with their EMV tags and the ISO 8583 request and response that carried them.</td></tr>
                        <tr><td>Scenarios</td><td>Pending</td><td>Fault injection &mdash; card yanked mid-transaction, comm errors, PIN timeout, paper out, host no-response &mdash; plus scripted runs with deterministic replay.</td></tr>
                        <tr><td>Logs</td><td>Pending</td><td>Boot log, bridge frames and DAL calls in the shared log panel with filtering and auto-scroll.</td></tr>
                    </tbody>
                </table>
            </div>
            <div class="info-card note">
                <div class="info-card-title">Note</div>
                <p>For chip-level cryptogram and TLV work today, pair the terminal with the <a href="/docs/apdu-simulator">APDU Simulator</a> and the <a href="/docs/emv-tools">EMV &amp; Card Tools</a>.</p>
            </div>
        </section>

    </main>

<aside class="pro-nudge"><span class="pn-tag">✦ Pro</span><p>Testing with a team, or certifying with a scheme? Pro raises the CPS ceiling, unlocks the full algorithm set and deep simulator tweaks, plus hosted endpoints and priority support.</p><a href="/pro">Register for Pro →</a></aside>`,
})
export class DocsPosSimulatorPage {}
