import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { SitePage } from './site-page';
import { UiSection } from '../../ui/section';

@Component({
  selector: 'page-docs-hsm-command-console',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiSection],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-hsm-command-console' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<main class="doc-body">
        <div class="breadcrumb">
            <a href="/">Home</a><span class="breadcrumb-sep">/</span>
            <a href="/docs">Documentation</a><span class="breadcrumb-sep">/</span>
            <span>HSM Command Console</span>
        </div>

        <div class="page-title-row">
            <h1 class="page-title">HSM Command Console</h1>
            <span class="badge badge-purple">Beta</span>
        </div>
        <p class="page-description">A host-command client for hardware security modules. Connect to a real or simulated HSM, fire individual host commands, chain them into repeatable scenarios, and drive them under load &mdash; all over TCP/IP with optional TLS. Inside the app it is titled <strong>HSM Host Console</strong>.</p>

        <ui-section anchor="overview" heading="Overview">
            <p>The HSM Command Console acts as a <strong>client</strong> that talks to an HSM's host interface. It packs your command payload with the vendor's framing (length header, STX/ETX, etc.), sends it over the socket, and shows the raw request/response exchange. Point it at ISO8583Studio's own <a href="/docs/hsm-simulator">HSM Simulator</a> or at a physical device on your bench.</p>

            <div class="shot-grid">
                <figure class="shot-fig wide" style="--shot-w:1400px"><image-slot><img src="/images/docs/hsm-command-console/overview.png" alt="HSM Host Console connected to Thales payShield at 127.0.0.1:9090, with the 120-command list on the left and an NO / NP HSM Status exchange shown as formatted request and response" width="1919" height="1004" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">hsm-command-console</span> Overview</figcaption>
                </figure>
            </div>
            <div class="feature-grid">
                <div class="feature-item"><div class="feature-item-icon">🏦</div><h4>Multi-Vendor</h4><p>Thales payShield, Futurex, SafeNet Luna, Utimaco CryptoServer &amp; Atalla, and nCipher nShield &mdash; each with the correct default port and framing.</p></div>
                <div class="feature-item"><div class="feature-item-icon">⌨️</div><h4>Command Console</h4><p>Compose host commands from the vendor's command set, fill in fields, send, and read the decoded response.</p></div>
                <div class="feature-item"><div class="feature-item-icon">🌿</div><h4>Scenario Builder</h4><p>Chain commands into a named scenario (each step is a command code + field values) and replay it on demand.</p></div>
                <div class="feature-item"><div class="feature-item-icon">⚡</div><h4>Load Tester</h4><p>Run a scenario at a target rate with constant, ramp-up, spike, or burst patterns across concurrent connections.</p></div>
                <div class="feature-item"><div class="feature-item-icon">🔒</div><h4>TLS Transport</h4><p>Optional mutual TLS with configurable version, cipher suites, CA verification, and PKCS#12 client certificates.</p></div>
                <div class="feature-item"><div class="feature-item-icon">📜</div><h4>Exchange Log</h4><p>Every command and response is timestamped and captured in the Logs tab for review and export.</p></div>
            </div>
        </ui-section>

        <ui-section anchor="quick-start" heading="Quick Start">
            <ol class="steps">
                <li><strong>Create a console configuration</strong> &mdash; From the Home screen open <code>HSM Host Console</code> and add a new configuration.</li>
                <li><strong>Pick your HSM vendor</strong> &mdash; In <strong>Connection Settings</strong>, choose the HSM Type. The port and framing default to that vendor (e.g. Thales payShield &rarr; port <code>1500</code>, 2-byte binary length).</li>
                <li><strong>Set the address</strong> &mdash; Enter the HSM's <strong>IP Address</strong> and <strong>Port</strong>, and a connection <strong>Timeout</strong>.</li>
                <li><strong>(Optional) Enable TLS</strong> &mdash; In <strong>SSL/TLS Configuration</strong> turn on TLS, choose the version and certificate-verification mode, and attach your CA / client certificate.</li>
                <li><strong>Launch and connect</strong> &mdash; Open the console and click <strong>Connect</strong>. The header dot turns green when the socket is up.</li>
                <li><strong>Send a command</strong> &mdash; In the <strong>Console</strong> tab select a command from the vendor set, fill its fields, and send. The response appears in the exchange log.</li>
            </ol>
            <div class="info-card tip"><div class="info-card-title">Tip</div><p>No physical HSM? Start the <a href="/docs/hsm-simulator">HSM Simulator</a> as a server on the same machine and point the console at <code>127.0.0.1:1500</code> for a fully local key-management loop.</p></div>
        </ui-section>

        <ui-section anchor="vendors" heading="Supported HSM Vendors">
            <p>Selecting a vendor sets its default port and message framing automatically. All vendors are driven over the same socket client.</p>
            <div class="table-wrapper"><table>
                <thead><tr><th>Vendor</th><th>Model family</th><th>Default Port</th><th>Framing</th></tr></thead>
                <tbody>
                    <tr><td><strong>Thales payShield</strong></td><td>payShield 9000 / 10K</td><td><code>1500</code></td><td>2-byte binary length</td></tr>
                    <tr><td><strong>Futurex Excrypt</strong></td><td>KMES Series 3</td><td><code>2000</code></td><td>2-byte binary length</td></tr>
                    <tr><td><strong>SafeNet Luna</strong></td><td>Thales Luna Network HSM</td><td><code>1500</code></td><td>2-byte binary length</td></tr>
                    <tr><td><strong>Utimaco CryptoServer</strong></td><td>Se / CP5</td><td><code>3001</code></td><td>2-byte binary length</td></tr>
                    <tr><td><strong>nCipher nShield</strong></td><td>Entrust nShield Connect / Solo</td><td><code>9004</code></td><td>4-byte ASCII length</td></tr>
                    <tr><td><strong>Utimaco Atalla</strong></td><td>Atalla AT1000</td><td><code>7000</code></td><td>STX / ETX framing</td></tr>
                    <tr><td><strong>Generic HSM</strong></td><td>Custom / other</td><td><code>1500</code></td><td>Configurable</td></tr>
                </tbody>
            </table></div>
        </ui-section>

        <ui-section anchor="connection" heading="Connection Settings">
            <p>The <strong>Connection Settings</strong> configuration tab defines the transport to the HSM.</p>

            <div class="shot-grid">
                <figure class="shot-fig" style="--shot-w:936px"><image-slot><img src="/images/docs/hsm-command-console/connection.png" alt="Connection Settings tab with the console named HSM Host Console - local, HSM type Thales payShield, IP 127.0.0.1 port 9090, a 30 second timeout, and the TCP length header enabled" width="1528" height="950" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">hsm-command-console</span> Connection Settings</figcaption>
                </figure>
            </div>
            <div class="spec-list">
                <div class="spec-row"><div class="k">IP Address</div><div class="v">HSM host interface address. Default <code>127.0.0.1</code>.</div></div>
                <div class="spec-row"><div class="k">Port</div><div class="v">TCP port; auto-filled from the selected vendor (e.g. <code>1500</code> for Thales).</div></div>
                <div class="spec-row"><div class="k">Timeout</div><div class="v">Connection / response timeout in seconds. Default <code>30</code>.</div></div>
                <div class="spec-row"><div class="k">Name &amp; Description</div><div class="v">Identifiers for the saved configuration.</div></div>
            </div>
        </ui-section>

        <ui-section anchor="framing" heading="Message Framing">
            <p>HSMs delimit messages differently. The console supports the common framing schemes; the correct one is selected when you pick a vendor, and can be overridden.</p>

            <div class="shot-grid">
                <figure class="shot-fig" style="--shot-w:936px"><image-slot><img src="/images/docs/hsm-command-console/framing.png" alt="Message Framing controls: TCP length header enabled, header format 2-byte binary length, message header 0000, empty trailer, and message header length 4" width="1498" height="398" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">hsm-command-console</span> Message Framing</figcaption>
                </figure>
            </div>
            <div class="table-wrapper"><table>
                <thead><tr><th>Header Format</th><th>Description</th></tr></thead>
                <tbody>
                    <tr><td><code>2-byte Binary Length</code></td><td>Two-byte big-endian length prefix. Used by Thales, Futurex, Luna, Utimaco.</td></tr>
                    <tr><td><code>4-byte ASCII Length</code></td><td>Four ASCII digits of length. Used by nCipher nShield.</td></tr>
                    <tr><td><code>STX / ETX Framing</code></td><td>Start/end control bytes bracket the message. Used by Utimaco Atalla.</td></tr>
                    <tr><td><code>No Header / Framing</code></td><td>Raw payload with no length prefix.</td></tr>
                    <tr><td><code>Custom Header</code></td><td>User-defined header / trailer bytes.</td></tr>
                </tbody>
            </table></div>
            <p>Additional framing fields: <strong>TCP Length Header Enabled</strong> toggle, <strong>Message Header</strong> (hex, e.g. <code>0000</code>), <strong>Message Trailer</strong> (hex, optional), and <strong>Message Header Length</strong>.</p>
        </ui-section>

        <ui-section anchor="ssl" heading="SSL / TLS Configuration">
            <p>Enable encrypted transport to the HSM in the <strong>SSL/TLS Configuration</strong> tab.</p>

            <div class="shot-grid">
                <figure class="shot-fig" style="--shot-w:936px"><image-slot><img src="/images/docs/hsm-command-console/ssl.png" alt="SSL/TLS configuration with encryption enabled, TLS 1.2, a PKCS#12 bundle, CA-signed-only verification, and empty client certificate, private key and keystore password fields" width="1467" height="762" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">hsm-command-console</span> SSL / TLS Configuration</figcaption>
                </figure>
                <figure class="shot-fig" style="--shot-w:936px">
                    <image-slot><img src="/images/docs/hsm-command-console/ssl-ciphers.png" alt="Cipher suite checklist with TLS_AES_256_GCM_SHA384 and TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384 selected, each entry labelled with its strength" width="1468" height="509" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">hsm-command-console</span> SSL / TLS Configuration &mdash; cipher suites</figcaption>
                </figure>
            </div>
            <div class="table-wrapper"><table>
                <thead><tr><th>Setting</th><th>Options</th></tr></thead>
                <tbody>
                    <tr><td><strong>TLS Version</strong></td><td>TLS 1.2 (default), TLS 1.3, and earlier where required.</td></tr>
                    <tr><td><strong>Certificate Verification</strong></td><td>No Verification, Trust All Certificates, CA-Signed Only, Custom CA Authority.</td></tr>
                    <tr><td><strong>Certificate Type</strong></td><td>PKCS#12 client certificate with key-store password.</td></tr>
                    <tr><td><strong>Cipher Suites</strong></td><td>Selectable set, defaulting to <code>TLS_AES_256_GCM_SHA384</code> and <code>TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384</code>.</td></tr>
                    <tr><td><strong>Key material</strong></td><td>CA authority path, client public certificate, client private key.</td></tr>
                </tbody>
            </table></div>
        </ui-section>

        <ui-section anchor="console" heading="Command Console Tab">
            <p>The <strong>Console</strong> tab is the interactive workspace. Pick a host command from the active vendor's command set, populate its parameter fields, and send it. The request and the decoded response are appended to the exchange log with response codes and timing.</p>

            <div class="shot-grid">
                <figure class="shot-fig" style="--shot-w:1400px"><image-slot><img src="/images/docs/hsm-command-console/console.png" alt="Command Console with A0 Generate a Key selected, its parameter fields filled in for a TDES key block, and the response panel showing the decoded key block and its check value" width="1920" height="1011" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">hsm-command-console</span> Command Console Tab</figcaption>
                </figure>
            </div>
            <ul>
                <li><strong>Vendor command set</strong> &mdash; the available commands follow the selected HSM vendor (for Thales, the full payShield host-command set).</li>
                <li><strong>Field editor</strong> &mdash; each command exposes its parameters as labelled inputs so you don't hand-assemble the payload.</li>
                <li><strong>Send &amp; inspect</strong> &mdash; responses are shown raw and parsed; errors surface the HSM's response/error code.</li>
            </ul>
        </ui-section>

        <ui-section anchor="scenario" heading="Scenario Builder Tab">
            <p>A <strong>scenario</strong> is an ordered list of command steps &mdash; each step captures a <code>commandCode</code> and its field values. Build a sequence (for example: generate a key, export it under a ZMK, then translate a PIN block), name it, and save it into the configuration for reuse.</p>

            <div class="shot-grid">
                <figure class="shot-fig" style="--shot-w:1400px"><image-slot><img src="/images/docs/hsm-command-console/scenario.png" alt="Scenario Builder with a two-step flow - A0 Generate a Key then A6 Import a Key - where the import step references the generated key as [1][A0][KEY]" width="1858" height="917" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">hsm-command-console</span> Scenario Builder Tab</figcaption>
                </figure>
            </div>
            <ul>
                <li><strong>Steps</strong> &mdash; add, reorder, and edit command steps; values from earlier steps can feed later ones.</li>
                <li><strong>Saved scenarios</strong> &mdash; persisted with the configuration and available to both the console and the load tester.</li>
                <li><strong>Playlists</strong> &mdash; group custom text, single commands, and whole scenarios into a runnable playlist with optional auto-advance.</li>
            </ul>
        </ui-section>

        <ui-section anchor="load-test" heading="Load Testing Tab">
            <p>Drive a saved scenario against the HSM to measure throughput and stability. Configure it in the <strong>Load Test Settings</strong> tab and run it from the <strong>Load Test</strong> tab.</p>

            <div class="shot-grid">
                <figure class="shot-fig" style="--shot-w:1400px"><image-slot><img src="/images/docs/hsm-command-console/load-test.png" alt="Load test results for an NO HSM Status playlist: 96 sent, 96 received, 96 successes, 0 failures, 0.9 ms average latency and 9.6 tps at a 100% success rate" width="1859" height="916" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">hsm-command-console</span> Load Testing Tab</figcaption>
                </figure>
            </div>
            <div class="table-wrapper"><table>
                <thead><tr><th>Parameter</th><th>Description</th><th>Default</th></tr></thead>
                <tbody>
                    <tr><td>Concurrent Connections</td><td>Parallel sockets driving the load.</td><td><code>1</code></td></tr>
                    <tr><td>Commands / second</td><td>Target command rate.</td><td><code>10</code></td></tr>
                    <tr><td>Duration</td><td>Test length in seconds.</td><td><code>60</code></td></tr>
                    <tr><td>Pattern</td><td>Constant Rate, Ramp Up, Spike Test, or Burst Pattern.</td><td>Constant</td></tr>
                </tbody>
            </table></div>
        </ui-section>

        <ui-section anchor="logs" heading="Logs Tab">
            <p>The <strong>Logs</strong> tab streams every exchange &mdash; request, response, connection events &mdash; with timestamps. Clear the buffer, and see live connection count and byte counters. Global logging can be toggled in app settings.</p>

            <div class="shot-grid">
                <figure class="shot-fig" style="--shot-w:1400px"><image-slot><img src="/images/docs/hsm-command-console/logs.png" alt="Logs tab streaming connection events and a Thales exchange expanded into its formatted request, formatted response, raw hex and parsed views" width="1920" height="1008" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">hsm-command-console</span> Logs Tab</figcaption>
                </figure>
            </div>
        </ui-section>

        <ui-section anchor="tabs-reference" heading="Tabs Reference">
            <div class="table-wrapper"><table>
                <thead><tr><th>Tab</th><th>Purpose</th></tr></thead>
                <tbody>
                    <tr><td><strong>Console</strong></td><td>Compose and send individual host commands; read decoded responses.</td></tr>
                    <tr><td><strong>Scenario</strong></td><td>Build, save, and replay ordered command sequences.</td></tr>
                    <tr><td><strong>Load Test</strong></td><td>Run a scenario at a target rate and pattern across connections.</td></tr>
                    <tr><td><strong>Logs</strong></td><td>Timestamped exchange log with connection and byte counters.</td></tr>
                </tbody>
            </table></div>
            <div class="info-card note"><div class="info-card-title">Related</div><p>Pair this with the <a href="/docs/hsm-simulator">HSM Simulator</a> (the server side) for a complete, self-contained key-management test rig.</p></div>
        </ui-section>
    </main>

<aside class="pro-nudge"><span class="pn-tag">✦ Pro</span><p>Testing with a team, or certifying with a scheme? Pro raises the CPS ceiling, unlocks the full algorithm set and deep simulator tweaks, plus hosted endpoints and priority support.</p><a href="/pro">Register for Pro →</a></aside>`,
})
export class DocsHsmCommandConsolePage {}
