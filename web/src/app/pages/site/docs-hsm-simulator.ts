import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { SitePage } from './site-page';
import { UiSection } from '../../ui/section';

@Component({
  selector: 'page-docs-hsm-simulator',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiSection],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-hsm-simulator' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<main class="doc-body">
        <div class="breadcrumb">
            <a href="/">Home</a>
            <span class="breadcrumb-sep">/</span>
            <span>Documentation</span>
            <span class="breadcrumb-sep">/</span>
            <span>HSM Simulator</span>
        </div>

        <h1 class="page-title">HSM Simulator</h1>
        <p class="page-description">Emulate a payment Hardware Security Module for host application development. Device profiles cover six HSM vendors and their models, with a Thales payShield command engine behind them &mdash; key management, PIN operations, encryption, MAC generation, and more.</p>

        <!-- Overview -->
        <ui-section anchor="overview" heading="Overview">
            <p>The HSM Simulator in ISO8583Studio stands in for a payment Hardware Security Module, so you can develop and test host applications that need HSM integration without physical hardware. Each simulator is a <strong>profile</strong> &mdash; a named device with its own vendor, model, serial number and network settings &mdash; and you can keep as many profiles as you need side by side.</p>
            <p>Six vendor profiles are selectable, covering Thales, SafeNet, Utimaco, Futurex, nCipher and a generic device. The command engine behind them implements the <strong>Thales payShield</strong> host command set, which is what the rest of this page documents.</p>

            <div class="shot-grid">
                <figure class="shot-fig wide" style="--shot-w:1400px"><image-slot><img src="/images/docs/hsm-simulator/overview.png" alt="HSM Simulator running as Thales PayShield 10k on 0.0.0.0:9090, showing an NO / NP HSM State exchange as a formatted request and response beside their raw hex" width="2800" height="1510" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">hsm-simulator</span> Overview</figcaption>
                </figure>
            </div>

            <div class="feature-grid">
                <div class="feature-item">
                    <div class="feature-item-icon">🔐</div>
                    <h4>Key Management</h4>
                    <p>Generate, import, export, and translate cryptographic keys including ZMK, ZPK, TPK, BDK, ZEK, and more.</p>
                </div>
                <div class="feature-item">
                    <div class="feature-item-icon">🔢</div>
                    <h4>PIN Operations</h4>
                    <p>Translate PIN blocks between keys, verify PINs using IBM 3624 and VISA PVV methods, and generate PIN offsets.</p>
                </div>
                <div class="feature-item">
                    <div class="feature-item-icon">🔒</div>
                    <h4>Encryption / Decryption</h4>
                    <p>Encrypt and decrypt data blocks with DES/3DES in ECB and CBC modes using ZEK, DEK, or BDK keys.</p>
                </div>
                <div class="feature-item">
                    <div class="feature-item-icon">📝</div>
                    <h4>MAC Generation</h4>
                    <p>Generate and verify MACs using ISO 9797 Algorithm 1 and Algorithm 3 with ZAK or TAK keys.</p>
                </div>
                <div class="feature-item">
                    <div class="feature-item-icon">🔑</div>
                    <h4>RSA Support</h4>
                    <p>Generate RSA key pairs, import public keys, and create/validate digital signatures.</p>
                </div>
                <div class="feature-item">
                    <div class="feature-item-icon">💾</div>
                    <h4>LMK Storage</h4>
                    <p>Multiple LMK slots, persistent key storage, and full LMK lifecycle management.</p>
                </div>
            </div>
        </ui-section>

        <!-- Profiles -->
        <ui-section anchor="profile" heading="Device Profiles">
            <p>The <strong>Profile</strong> tab of the HSM Simulator Configuration screen describes the device the simulator presents. The left rail lists every profile you have created, with buttons to add, import, export and delete; <strong>Launch HSM Simulator</strong> starts the selected one.</p>

            <div class="shot-grid">
                <figure class="shot-fig wide" style="--shot-w:1400px">
                    <image-slot><img src="/images/docs/hsm-simulator/profile.png" alt="HSM Simulator Configuration on the Profile tab: a two-profile list on the left, basic information fields, the six vendor tiles with Thales selected, and the payShield 10K model and firmware version" width="2800" height="1635" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">hsm-simulator</span> Profile tab &mdash; vendor, model and device identity</figcaption>
                </figure>
            </div>

            <h3>Basic Information</h3>
            <ul>
                <li><strong>HSM Name</strong> &mdash; The profile name, shown in the profile list and in the simulator&rsquo;s title bar.</li>
                <li><strong>Description</strong> &mdash; Free text for your own reference.</li>
                <li><strong>Serial Number</strong> &mdash; The device serial the profile carries (e.g. <code>HSM001234567890</code>).</li>
            </ul>

            <h3>Vendor &amp; Model</h3>
            <p>Pick a vendor tile and the model drop-down below it changes to that vendor&rsquo;s device list. <strong>Firmware Version</strong> is a free-text field on the profile.</p>

            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr><th>Vendor</th><th>Models</th></tr>
                    </thead>
                    <tbody>
                        <tr><td>Thales</td><td><code>payShield 9000</code>, <code>payShield 10K</code></td></tr>
                        <tr><td>SafeNet Luna</td><td><code>Network Attached</code>, <code>PCIe</code>, <code>USB</code></td></tr>
                        <tr><td>Utimaco CryptoServer</td><td><code>Se</code>, <code>CP5</code></td></tr>
                        <tr><td>Futurex Excrypt</td><td><code>KMES</code>, <code>VirtuCrypt</code></td></tr>
                        <tr><td>nCipher nShield</td><td><code>Connect</code>, <code>Solo</code>, <code>Edge</code></td></tr>
                        <tr><td>Generic/Custom HSM</td><td><code>Custom Model</code></td></tr>
                    </tbody>
                </table>
            </div>

            <div class="info-card warning">
                <div class="info-card-title">Which vendors answer commands today</div>
                <p>Only the <strong>Thales</strong> profile has a command engine behind it. The other five are selectable and store their vendor, model and firmware on the profile, but no command processor is wired up for them yet &mdash; a simulator launched on one of those profiles accepts the connection and answers <code>ERROR: No active HSM</code>. Use the Thales profile for anything you intend to actually drive.</p>
            </div>

            <div class="info-card note">
                <div class="info-card-title">Note</div>
                <p>To talk to a non-Thales HSM, use the <a href="/simulator/hsm-command-console">HSM Command Console</a> instead &mdash; that tool is a <em>client</em>, and it does carry per-vendor framing and command sets for Thales payShield, Futurex, SafeNet Luna, Utimaco and nCipher.</p>
            </div>

            <h3>Saving and launching</h3>
            <ul>
                <li><strong>Save All Configurations</strong> &mdash; Persists every profile in the list.</li>
                <li><strong>Launch HSM Simulator</strong> &mdash; Opens the selected profile&rsquo;s simulator, with the HSM Handler, Key Management, Host Commands, Secure Commands and Logs tabs.</li>
            </ul>
        </ui-section>

        <!-- Quick Start -->
        <ui-section anchor="quick-start" heading="Quick Start Guide">

            <ol class="steps">
                <li><strong>Create an HSM configuration</strong> &mdash; From the Home screen, create a new HSM Simulator configuration. Set the profile name and device details.</li>
                <li><strong>Configure network settings</strong> &mdash; Set the TCP/IP bind address and port (e.g. <code>0.0.0.0:9090</code>). Configure the message header length (default: 4 characters).</li>
                <li><strong>Launch the simulator</strong> &mdash; Open the HSM Simulator screen and start the server from the <strong>HSM Handler</strong> tab.</li>
                <li><strong>Connect your application</strong> &mdash; Point your host application to <code>host:port</code> and send PayShield host commands.</li>
                <li><strong>Test with built-in commands</strong> &mdash; Use the <strong>Host Commands</strong> tab to send commands interactively and inspect responses.</li>
            </ol>

            <div class="info-card tip">
                <div class="info-card-title">Tip</div>
                <p>Start with the <code>NC</code> (Diagnostic Test) command to verify the simulator is running and LMK is loaded. A response of <code>ND00</code> confirms everything is healthy.</p>
            </div>
        </ui-section>

        <!-- Protocol -->
        <ui-section anchor="protocol" heading="Message Protocol">
            <p>The HSM Simulator uses the standard Thales PayShield host command protocol over TCP/IP.</p>

            <div class="shot-grid">
                <figure class="shot-fig" style="--shot-w:1400px"><image-slot><img src="/images/docs/hsm-simulator/protocol.png" alt="Log lines for two HDR1 NO commands and their NP responses, each with a matching HOST_COMMAND audit entry" width="2800" height="396" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">hsm-simulator</span> Logs &mdash; an NO / NP exchange as it goes over the wire</figcaption>
                </figure>
            </div>

            <h3>Request Format</h3>
            <pre><code>[Length Header (2 bytes, optional)][Message Header (4 chars)][Command Code (2 chars)][Data Fields][%LMK_ID][Trailer]</code></pre>

            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr><th>Component</th><th>Size</th><th>Description</th></tr>
                    </thead>
                    <tbody>
                        <tr><td><strong>Length Header</strong></td><td>2 bytes</td><td>Optional. Big-endian message length (excluding the length header itself).</td></tr>
                        <tr><td><strong>Message Header</strong></td><td>4 chars</td><td>Configurable header echoed back in response (e.g. <code>0000</code>).</td></tr>
                        <tr><td><strong>Command Code</strong></td><td>2 chars</td><td>Two-character command identifier (e.g. <code>NC</code>, <code>A0</code>).</td></tr>
                        <tr><td><strong>Data Fields</strong></td><td>Variable</td><td>Command-specific data parameters.</td></tr>
                        <tr><td><strong>LMK ID</strong></td><td>3 chars</td><td>Optional <code>%XX</code> suffix to select a specific LMK slot.</td></tr>
                        <tr><td><strong>Trailer</strong></td><td>Variable</td><td>Optional end-of-message marker (<code></code> + trailer data).</td></tr>
                    </tbody>
                </table>
            </div>

            <h3>Response Format</h3>
            <pre><code>[Length Header (2 bytes, optional)][Message Header][Response Code][Error Code (2 chars)][Response Data]</code></pre>
            <p>The response code is the command code incremented by one character (e.g. <code>NC</code> &rarr; <code>ND</code>, <code>A0</code> &rarr; <code>A1</code>). An error code of <code>00</code> indicates success.</p>

            <h3>Example Exchange</h3>
            <pre><code>Request:  0000NC
Response: 0000ND00LMK12345678901234567890123456789012345678

Request:  0000A0001U
Response: 0000A100U1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF</code></pre>

            <h3>Encoding</h3>
            <p>All messages use <strong>ISO-8859-1</strong> encoding. Keys are represented as hexadecimal strings in the protocol.</p>
        </ui-section>

        <!-- Network Settings -->
        <ui-section anchor="network" heading="Network Settings">

            <div class="shot-grid">
                <figure class="shot-fig" style="--shot-w:936px"><image-slot><img src="/images/docs/hsm-simulator/network.png" alt="Connection Configuration on the Network tab with TCP_IP selected, port 9090, the TCP/IP length header enabled with a 4-byte message header, and SSL/TLS off" width="1872" height="1232" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">hsm-simulator</span> Network Settings</figcaption>
                </figure>
            </div>

            <h3>TCP/IP Configuration</h3>
            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr><th>Setting</th><th>Default</th><th>Description</th></tr>
                    </thead>
                    <tbody>
                        <tr><td><strong>Bind Address</strong></td><td><code>0.0.0.0</code></td><td>IP address to listen on. Use <code>0.0.0.0</code> for all interfaces.</td></tr>
                        <tr><td><strong>Port</strong></td><td><code>9090</code></td><td>TCP port for the HSM server.</td></tr>
                        <tr><td><strong>Length Header</strong></td><td>Optional</td><td>Enable 2-byte big-endian length prefix before each message.</td></tr>
                        <tr><td><strong>Message Header Length</strong></td><td>4</td><td>Number of characters in the message header (echoed back in response).</td></tr>
                    </tbody>
                </table>
            </div>

            <h3>Additional Connection Types</h3>
            <ul>
                <li><strong>Serial</strong> &mdash; RS232 connection with configurable port, baud rate, data bits, stop bits, and parity.</li>
                <li><strong>REST API</strong> &mdash; HTTP endpoint for HSM commands.</li>
                <li><strong>WebSocket</strong> &mdash; Real-time bidirectional HSM communication.</li>
            </ul>
        </ui-section>

        <!-- LMK Storage -->
        <ui-section anchor="lmk" heading="LMK Storage">
            <p>The Local Master Key (LMK) is the foundation of HSM security. All working keys are encrypted under LMK pairs.</p>

            <div class="shot-grid">
                <figure class="shot-fig" style="--shot-w:1400px"><image-slot><img src="/images/docs/hsm-simulator/lmk.png" alt="LMK Slot 00 loaded, showing the VARIANT scheme, 3DES 2-key algorithm, 40 pairs, its check value, and the derived key block protection key" width="2800" height="1177" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">hsm-simulator</span> LMK Slot 00 &mdash; scheme, algorithm, pair count and check value</figcaption>
                </figure>
            </div>

            <h3>LMK Structure</h3>
            <ul>
                <li><strong>40 LMK Pairs</strong> &mdash; Pairs 00&ndash;39, each consisting of a left key and right key. Different key types are encrypted under specific LMK pairs.</li>
                <li><strong>Multiple LMK Slots</strong> &mdash; Slots 00&ndash;99 allow independent LMK sets. Select the slot using <code>%XX</code> in commands or via port mapping.</li>
                <li><strong>Old / New LMK</strong> &mdash; Support for LMK migration with the <code>BW</code> (Translate Key) command.</li>
            </ul>

            <h3>LMK Pair Assignments</h3>
            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr><th>LMK Pair</th><th>Key Type Encrypted</th></tr>
                    </thead>
                    <tbody>
                        <tr><td>Pair 02&ndash;03</td><td>PIN Encryption Keys (PINs under LMK)</td></tr>
                        <tr><td>Pair 04&ndash;05</td><td>ZMK (Zone Master Keys)</td></tr>
                        <tr><td>Pair 06&ndash;07</td><td>ZPK, TPK (Zone/Terminal PIN Keys)</td></tr>
                        <tr><td>Pair 14&ndash;15</td><td>PVK (PIN Verification Keys)</td></tr>
                        <tr><td>Pair 16&ndash;17</td><td>TAK (Terminal Authentication Keys)</td></tr>
                        <tr><td>Pair 22&ndash;23</td><td>BDK (Base Derivation Keys)</td></tr>
                        <tr><td>Pair 26&ndash;27</td><td>ZEK, DEK (Data Encryption Keys)</td></tr>
                        <tr><td>Pair 28&ndash;29</td><td>ZAK (Zone Authentication Keys)</td></tr>
                    </tbody>
                </table>
            </div>

            <h3>Persistence</h3>
            <p>LMK data is stored in <code>payShield10k_lmkStorage.json</code> and within the simulator configuration JSON. Keys persist across restarts.</p>
        </ui-section>

        <!-- Commands: Diagnostics -->
        <ui-section anchor="cmd-diagnostics" heading="Diagnostics &amp; Info Commands">

            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr><th>Command</th><th>Response</th><th>Name</th><th>Description</th></tr>
                    </thead>
                    <tbody>
                        <tr><td><code>NC</code></td><td><code>ND</code></td><td>Diagnostic Test</td><td>Health check. Returns <code>00</code> if the HSM and LMK are functioning correctly.</td></tr>
                        <tr><td><code>NO</code></td><td><code>NP</code></td><td>Reserved</td><td>Reserved diagnostic command.</td></tr>
                        <tr><td><code>VR</code></td><td><code>VS</code></td><td>Version Info</td><td>Returns firmware version and serial number of the emulated HSM.</td></tr>
                        <tr><td><code>VT</code></td><td><code>VU</code></td><td>View LMK Table</td><td>Displays Key Check Values (KCVs) of all 40 loaded LMK pairs.</td></tr>
                        <tr><td><code>GK</code></td><td><code>GL</code></td><td>Generate LMK Component</td><td>Generates a random LMK component for key loading ceremonies.</td></tr>
                    </tbody>
                </table>
            </div>

            <h3>Example: Diagnostic Test</h3>
            <pre><code>Request:  0000NC
Response: 0000ND00

Error code 00 = HSM is healthy, LMK loaded.</code></pre>
        </ui-section>

        <!-- Commands: Key Management -->
        <ui-section anchor="cmd-key-mgmt" heading="Key Management Commands">

            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr><th>Command</th><th>Response</th><th>Name</th><th>Description</th></tr>
                    </thead>
                    <tbody>
                        <tr><td><code>A0</code></td><td><code>A1</code></td><td>Generate Key</td><td>Generates a new DES/3DES key encrypted under LMK. Optionally also encrypted under a ZMK for transport.</td></tr>
                        <tr><td><code>A6</code></td><td><code>A7</code></td><td>Import Key</td><td>Imports a key encrypted under ZMK and re-encrypts it under LMK.</td></tr>
                        <tr><td><code>A8</code></td><td><code>A9</code></td><td>Export Key</td><td>Exports a key from LMK encryption to ZMK encryption for transport to another zone.</td></tr>
                        <tr><td><code>BU</code></td><td><code>BV</code></td><td>Generate KCV</td><td>Generates a Key Check Value (KCV) for a given key. Used to verify key integrity.</td></tr>
                        <tr><td><code>BW</code></td><td><code>BX</code></td><td>Translate Key (LMK)</td><td>Translates a key from old LMK encryption to new LMK encryption during LMK migration.</td></tr>
                        <tr><td><code>GC</code></td><td><code>GD</code></td><td>Generate Components</td><td>Generates N random key components. Used in split-knowledge key ceremonies.</td></tr>
                        <tr><td><code>FK</code></td><td><code>FL</code></td><td>Form Key from Components</td><td>XORs 2 or 3 key components together to form a working key under LMK.</td></tr>
                    </tbody>
                </table>
            </div>

            <h3>Example: Generate a ZPK</h3>
            <pre><code>Request:  0000A0001U
                ^^^ ^^^
                |   |-- Key scheme: U = double-length
                |------ Key type: 001 = ZPK

Response: 0000A100U&lt;key-under-LMK&gt;&lt;key-under-ZMK&gt;&lt;KCV&gt;</code></pre>

            <h3>Example: Generate KCV</h3>
            <pre><code>Request:  0000BU0U&lt;key-under-LMK&gt;
Response: 0000BV00&lt;6-char-KCV&gt;</code></pre>
        </ui-section>

        <!-- Commands: PIN Block -->
        <ui-section anchor="cmd-pin-block" heading="PIN Block Operations">

            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr><th>Command</th><th>Response</th><th>Name</th><th>Description</th></tr>
                    </thead>
                    <tbody>
                        <tr><td><code>BA</code></td><td><code>BB</code></td><td>Encrypt PIN Block</td><td>Encrypts a clear-text PIN into a PIN block under TPK or ZPK.</td></tr>
                        <tr><td><code>CA</code></td><td><code>CB</code></td><td>Translate PIN (TPK &rarr; ZPK)</td><td>Re-encrypts a PIN block from Terminal PIN Key to Zone PIN Key.</td></tr>
                        <tr><td><code>CI</code></td><td><code>CJ</code></td><td>Translate PIN (ZPK &rarr; TPK)</td><td>Re-encrypts a PIN block from Zone PIN Key to Terminal PIN Key.</td></tr>
                        <tr><td><code>BC</code></td><td><code>BD</code></td><td>Translate PIN Block</td><td>Translates PIN block between encryption keys.</td></tr>
                        <tr><td><code>G0</code></td><td><code>G1</code></td><td>Translate PIN (DUKPT &rarr; ZPK)</td><td>Translates a DUKPT-encrypted PIN block to ZPK encryption.</td></tr>
                        <tr><td><code>JC</code></td><td><code>JD</code></td><td>Translate PIN (TPK &rarr; LMK)</td><td>Re-encrypts a TPK-encrypted PIN block under LMK.</td></tr>
                        <tr><td><code>JE</code></td><td><code>JF</code></td><td>Translate PIN (ZPK &rarr; LMK)</td><td>Re-encrypts a ZPK-encrypted PIN block under LMK.</td></tr>
                        <tr><td><code>JG</code></td><td><code>JH</code></td><td>Translate PIN (LMK &rarr; ZPK)</td><td>Re-encrypts an LMK-encrypted PIN to a ZPK PIN block.</td></tr>
                        <tr><td><code>NG</code></td><td><code>NH</code></td><td>Generate DUKPT Key</td><td>Derives a DUKPT session key from a BDK and Key Serial Number.</td></tr>
                    </tbody>
                </table>
            </div>

            <h3>Example: Translate PIN (TPK &rarr; ZPK)</h3>
            <pre><code>Request:  0000CA&lt;TPK&gt;&lt;ZPK&gt;&lt;max-pin-length&gt;&lt;pin-block&gt;01&lt;account-number&gt;01&lt;account-number&gt;
Response: 0000CB00&lt;pin-length&gt;&lt;translated-pin-block&gt;</code></pre>

            <div class="info-card note">
                <div class="info-card-title">Note</div>
                <p>The <code>CA</code> command is the most commonly used PIN translation in payment processing. It converts a PIN block received from the terminal (encrypted under TPK) into a block suitable for sending to the card issuer (encrypted under ZPK).</p>
            </div>
        </ui-section>

        <!-- Commands: PIN Verification -->
        <ui-section anchor="cmd-pin-verify" heading="PIN Verification Commands">

            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr><th>Command</th><th>Response</th><th>Name</th><th>Description</th></tr>
                    </thead>
                    <tbody>
                        <tr><td><code>DA</code></td><td><code>DB</code></td><td>Verify PIN (IBM 3624)</td><td>Verifies a PIN using the IBM 3624 natural PIN method with offset data.</td></tr>
                        <tr><td><code>DC</code></td><td><code>DD</code></td><td>Verify PIN (VISA PVV)</td><td>Verifies a PIN against a VISA Pin Verification Value (PVV).</td></tr>
                    </tbody>
                </table>
            </div>

            <h3>Example: VISA PVV Verification</h3>
            <pre><code>Request:  0000DC&lt;ZPK&gt;01&lt;pin-block&gt;&lt;account-number&gt;&lt;PVK-pair&gt;&lt;PVV&gt;
Response: 0000DD00   (00 = PIN verified successfully)
Response: 0000DD01   (01 = PIN verification failed)</code></pre>
        </ui-section>

        <!-- Commands: PIN Generation -->
        <ui-section anchor="cmd-pin-gen" heading="PIN Generation Commands">

            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr><th>Command</th><th>Response</th><th>Name</th><th>Description</th></tr>
                    </thead>
                    <tbody>
                        <tr><td><code>DE</code></td><td><code>DF</code></td><td>Generate IBM PIN Offset</td><td>Generates an IBM 3624 PIN offset. Used for PIN issuance.</td></tr>
                        <tr><td><code>DG</code></td><td><code>DH</code></td><td>Generate VISA PVV</td><td>Generates a VISA Pin Verification Value for a given PIN.</td></tr>
                        <tr><td><code>EE</code></td><td><code>EF</code></td><td>Derive PIN from Offset</td><td>Derives the natural PIN from an IBM 3624 offset and validation data.</td></tr>
                    </tbody>
                </table>
            </div>
        </ui-section>

        <!-- Commands: Encryption -->
        <ui-section anchor="cmd-encrypt" heading="Data Encryption / Decryption Commands">

            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr><th>Command</th><th>Response</th><th>Name</th><th>Description</th></tr>
                    </thead>
                    <tbody>
                        <tr><td><code>M0</code></td><td><code>M1</code></td><td>Encrypt Data Block</td><td>Encrypts a data block using ZEK, DEK, or BDK in ECB or CBC mode.</td></tr>
                        <tr><td><code>M2</code></td><td><code>M3</code></td><td>Decrypt Data Block</td><td>Decrypts a data block using ZEK, DEK, or BDK.</td></tr>
                        <tr><td><code>M4</code></td><td><code>M5</code></td><td>Translate Data Block</td><td>Re-encrypts data from one key to another (e.g. DUKPT to ZEK).</td></tr>
                    </tbody>
                </table>
            </div>

            <h3>Cipher Modes</h3>
            <ul>
                <li><strong>ECB</strong> (Electronic Codebook) &mdash; Each block encrypted independently. Simpler but less secure for repetitive data.</li>
                <li><strong>CBC</strong> (Cipher Block Chaining) &mdash; Each block XORed with the previous ciphertext block. Requires an IV (Initialization Vector).</li>
            </ul>
        </ui-section>

        <!-- Commands: MAC -->
        <ui-section anchor="cmd-mac" heading="MAC Operations">

            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr><th>Command</th><th>Response</th><th>Name</th><th>Description</th></tr>
                    </thead>
                    <tbody>
                        <tr><td><code>M6</code></td><td><code>M7</code></td><td>Generate MAC</td><td>Generates a MAC using ZAK or TAK with ISO 9797 Algorithm 1 or 3.</td></tr>
                        <tr><td><code>M8</code></td><td><code>M9</code></td><td>Verify MAC</td><td>Verifies a MAC value against the original data and key.</td></tr>
                        <tr><td><code>MY</code></td><td><code>MZ</code></td><td>MAC Variants</td><td>Additional MAC algorithm and format variants.</td></tr>
                    </tbody>
                </table>
            </div>

            <h3>MAC Algorithms</h3>
            <ul>
                <li><strong>ISO 9797 Algorithm 1</strong> &mdash; Single DES CBC-MAC. Standard for most payment applications.</li>
                <li><strong>ISO 9797 Algorithm 3</strong> &mdash; Retail MAC (DES CBC-MAC with final 3DES step). Provides stronger security.</li>
            </ul>
        </ui-section>

        <!-- Commands: Hash -->
        <ui-section anchor="cmd-hash" heading="Hashing Commands">

            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr><th>Command</th><th>Response</th><th>Name</th><th>Description</th></tr>
                    </thead>
                    <tbody>
                        <tr><td><code>GM</code></td><td><code>GN</code></td><td>Hash Data</td><td>Computes a hash digest of the input data.</td></tr>
                    </tbody>
                </table>
            </div>

            <h3>Supported Algorithms</h3>
            <ul>
                <li>SHA-1</li>
                <li>SHA-256</li>
                <li>MD5</li>
            </ul>
        </ui-section>

        <!-- Commands: RSA -->
        <ui-section anchor="cmd-rsa" heading="RSA / Asymmetric Commands">

            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr><th>Command</th><th>Response</th><th>Name</th><th>Description</th></tr>
                    </thead>
                    <tbody>
                        <tr><td><code>EI</code></td><td><code>EJ</code></td><td>Generate RSA Key Pair</td><td>Generates an RSA public/private key pair. Private key encrypted under LMK.</td></tr>
                        <tr><td><code>EO</code></td><td><code>EP</code></td><td>Import RSA Public Key</td><td>Imports a DER-encoded RSA public key into the HSM.</td></tr>
                        <tr><td><code>EW</code></td><td><code>EX</code></td><td>Generate Signature</td><td>Creates an RSA or ECDSA digital signature over input data.</td></tr>
                        <tr><td><code>EY</code></td><td><code>EZ</code></td><td>Validate Signature</td><td>Validates an RSA or ECDSA signature against the original data.</td></tr>
                    </tbody>
                </table>
            </div>
        </ui-section>

        <!-- Commands: CVV -->
        <ui-section anchor="cmd-cvv" heading="Dynamic CVV/CVC">

            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr><th>Command</th><th>Response</th><th>Name</th><th>Description</th></tr>
                    </thead>
                    <tbody>
                        <tr><td><code>PM</code></td><td><code>PN</code></td><td>Verify Dynamic CVV/CVC</td><td>Verifies Visa dCVV or MasterCard CVC3 values for contactless transactions.</td></tr>
                    </tbody>
                </table>
            </div>
        </ui-section>

        <!-- Commands: Storage -->
        <ui-section anchor="cmd-storage" heading="User Storage Commands">
            <p>Store, retrieve, and delete keys or data in indexed user storage slots (000&ndash;FFF).</p>

            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr><th>Command</th><th>Response</th><th>Name</th><th>Description</th></tr>
                    </thead>
                    <tbody>
                        <tr><td><code>LA</code></td><td><code>LB</code></td><td>Load to Storage</td><td>Stores a key or data at a specified storage index (000&ndash;FFF).</td></tr>
                        <tr><td><code>LE</code></td><td><code>LF</code></td><td>Read from Storage</td><td>Retrieves data from a specified storage index.</td></tr>
                        <tr><td><code>LD</code></td><td><code>LM</code></td><td>Delete from Storage</td><td>Deletes the entry at a specified storage index.</td></tr>
                    </tbody>
                </table>
            </div>
        </ui-section>

        <!-- Key Types -->
        <ui-section anchor="key-types" heading="Key Types &amp; Schemes">

            <h3>Key Types</h3>
            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr><th>Code</th><th>Name</th><th>Purpose</th></tr>
                    </thead>
                    <tbody>
                        <tr><td><code>000</code></td><td>ZMK</td><td>Zone Master Key &mdash; Key-encrypting key for secure key exchange between zones.</td></tr>
                        <tr><td><code>001</code></td><td>ZPK</td><td>Zone PIN Key &mdash; Encrypts PIN blocks for transmission between zones.</td></tr>
                        <tr><td><code>002</code></td><td>TPK / PVK</td><td>Terminal PIN Key / PIN Verification Key &mdash; Terminal PIN encryption or PIN verification.</td></tr>
                        <tr><td><code>003</code></td><td>TAK</td><td>Terminal Authentication Key &mdash; MAC generation at the terminal.</td></tr>
                        <tr><td><code>008</code></td><td>ZAK</td><td>Zone Authentication Key &mdash; MAC generation between zones.</td></tr>
                        <tr><td><code>009</code></td><td>BDK</td><td>Base Derivation Key &mdash; Master key for DUKPT key derivation.</td></tr>
                        <tr><td><code>00A</code></td><td>ZEK</td><td>Zone Encryption Key &mdash; Data encryption between zones.</td></tr>
                        <tr><td><code>00B</code></td><td>DEK</td><td>Data Encryption Key &mdash; General-purpose data encryption.</td></tr>
                        <tr><td><code>302</code></td><td>IKEY</td><td>Intermediate Key &mdash; Internal processing key.</td></tr>
                    </tbody>
                </table>
            </div>

            <h3>Key Schemes</h3>
            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr><th>Scheme</th><th>Hex Length</th><th>Description</th></tr>
                    </thead>
                    <tbody>
                        <tr><td><code>X</code></td><td>16 characters</td><td>Single-length DES key (56 effective bits).</td></tr>
                        <tr><td><code>U</code></td><td>32 characters</td><td>Double-length 3DES key (112 effective bits). Most common for payment.</td></tr>
                        <tr><td><code>T</code></td><td>48 characters</td><td>Triple-length 3DES key (168 effective bits). Maximum DES security.</td></tr>
                    </tbody>
                </table>
            </div>
        </ui-section>

        <!-- PIN Block Formats -->
        <ui-section anchor="pin-formats" heading="PIN Block Formats">

            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr><th>Code</th><th>Format</th><th>Description</th></tr>
                    </thead>
                    <tbody>
                        <tr><td><code>01</code></td><td>ISO 9564-1 Format 0</td><td>Standard ISO format. PIN XORed with PAN. Most widely used.</td></tr>
                        <tr><td><code>02</code></td><td>ISO 9564-1 Format 1</td><td>ISO format without PAN dependency.</td></tr>
                        <tr><td><code>03</code></td><td>Docutel</td><td>Legacy Docutel ATM format.</td></tr>
                        <tr><td><code>04</code></td><td>PLUS</td><td>PLUS network PIN block format.</td></tr>
                        <tr><td><code>05</code></td><td>ISO 9564-1 Format 3</td><td>ISO format with random fill.</td></tr>
                        <tr><td><code>06</code></td><td>Diebold</td><td>Legacy Diebold ATM format.</td></tr>
                        <tr><td><code>47</code></td><td>ISO 9564-1 Format 4</td><td>AES-based PIN block format (newer, more secure).</td></tr>
                        <tr><td><code>46</code></td><td>AS2805</td><td>Australian standard PIN block format.</td></tr>
                    </tbody>
                </table>
            </div>
        </ui-section>

        <!-- Error Codes -->
        <ui-section anchor="error-codes" heading="Error Codes">
            <p>Every HSM response includes a 2-character error code. <code>00</code> indicates success.</p>

            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr><th>Code</th><th>Meaning</th></tr>
                    </thead>
                    <tbody>
                        <tr><td><code>00</code></td><td>No error &mdash; command executed successfully.</td></tr>
                        <tr><td><code>01</code></td><td>Verification failure (PIN or MAC mismatch).</td></tr>
                        <tr><td><code>02</code></td><td>Key Check Value (KCV) failure.</td></tr>
                        <tr><td><code>04</code></td><td>Cryptographic algorithm not supported.</td></tr>
                        <tr><td><code>10</code></td><td>PIN block length error.</td></tr>
                        <tr><td><code>15</code></td><td>Invalid input data.</td></tr>
                        <tr><td><code>17</code></td><td>Invalid message length.</td></tr>
                        <tr><td><code>23</code></td><td>PIN length error.</td></tr>
                        <tr><td><code>26</code></td><td>Invalid LMK identifier.</td></tr>
                        <tr><td><code>27</code></td><td>LMK check value failure.</td></tr>
                        <tr><td><code>39</code></td><td>Console authorization required.</td></tr>
                        <tr><td><code>42</code></td><td>Invalid LMK type code.</td></tr>
                        <tr><td><code>68</code></td><td>Command is disabled.</td></tr>
                        <tr><td><code>74</code></td><td>Data parity error in key or data block.</td></tr>
                        <tr><td><code>75</code></td><td>Invalid message length field.</td></tr>
                        <tr><td><code>80</code></td><td>Unknown command or invalid format.</td></tr>
                        <tr><td><code>82</code></td><td>Function not permitted by current configuration.</td></tr>
                        <tr><td><code>A1</code></td><td>Console not authorized. Grant authorization first.</td></tr>
                        <tr><td><code>A2</code></td><td>HSM not authorized.</td></tr>
                        <tr><td><code>A3</code></td><td>Command only available in secure state.</td></tr>
                        <tr><td><code>A4</code></td><td>Invalid key type for this command.</td></tr>
                        <tr><td><code>B1</code></td><td>Invalid key scheme code.</td></tr>
                        <tr><td><code>C1</code></td><td>LMK is not loaded. Load LMK before issuing commands.</td></tr>
                    </tbody>
                </table>
            </div>
        </ui-section>

        <!-- Tabs Reference -->
        <ui-section anchor="tabs-reference" heading="UI Tabs Reference">

            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr><th>Tab</th><th>Description</th></tr>
                    </thead>
                    <tbody>
                        <tr><td><strong>HSM Handler</strong></td><td>Start/stop the HSM server. View live request/response traffic, connection count, and server status.</td></tr>
                        <tr><td><strong>Key Management</strong></td><td>View loaded LMK status, key check values, and manage key components.</td></tr>
                        <tr><td><strong>Host Commands</strong></td><td>Interactive command forms for all ~35 supported commands. Grouped by category with search and filtering. Shows wire-format hints and formatted results.</td></tr>
                        <tr><td><strong>Secure Commands</strong></td><td>Administrative and LMK-related operations requiring console authorization (GC, FK, GK, A0, etc.).</td></tr>
                        <tr><td><strong>Logs</strong></td><td>Full request/response log of all HSM traffic.</td></tr>
                    </tbody>
                </table>
            </div>
        </ui-section>

        <!-- Secure Commands -->
        <ui-section anchor="secure-commands" heading="Secure Commands">
            <p>Certain operations require console authorization before they can be executed, simulating the real PayShield security model.</p>

            <h3>Authorization Flow</h3>
            <ol class="steps">
                <li><strong>Grant Authorization</strong> &mdash; In the Secure Commands tab, click the authorization button. This simulates inserting a custodian smart card.</li>
                <li><strong>Execute Commands</strong> &mdash; While authorized, you can run secure commands like <code>GC</code>, <code>FK</code>, <code>GK</code>, and LMK management operations.</li>
                <li><strong>Authorization Timeout</strong> &mdash; Authorization expires after a configurable period (default: 8 hours) or when manually revoked.</li>
            </ol>

            <h3>Secure Command List</h3>
            <p>The following commands are available in the Secure Commands tab:</p>
            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr><th>Command</th><th>Name</th><th>Purpose</th></tr>
                    </thead>
                    <tbody>
                        <tr><td><code>NC</code></td><td>Diagnostic Test</td><td>Verify HSM health.</td></tr>
                        <tr><td><code>VR</code></td><td>Version Info</td><td>Check firmware version.</td></tr>
                        <tr><td><code>VT</code></td><td>View LMK Table</td><td>Display all LMK KCVs.</td></tr>
                        <tr><td><code>GK</code></td><td>Generate LMK Component</td><td>Create LMK key component.</td></tr>
                        <tr><td><code>GC</code></td><td>Generate Key Components</td><td>Generate N random components for split knowledge.</td></tr>
                        <tr><td><code>FK</code></td><td>Form Key from Components</td><td>XOR components to form a key.</td></tr>
                        <tr><td><code>A0</code></td><td>Generate Key</td><td>Generate working keys under LMK.</td></tr>
                        <tr><td><code>LA</code></td><td>Load to Storage</td><td>Store data in HSM user storage.</td></tr>
                        <tr><td><code>LE</code></td><td>Read from Storage</td><td>Retrieve from user storage.</td></tr>
                        <tr><td><code>LD</code></td><td>Delete from Storage</td><td>Remove user storage entry.</td></tr>
                        <tr><td><code>BW</code></td><td>Translate Key</td><td>LMK migration &mdash; old to new LMK.</td></tr>
                        <tr><td><code>DE</code></td><td>Generate IBM PIN Offset</td><td>PIN issuance with IBM 3624.</td></tr>
                        <tr><td><code>DG</code></td><td>Generate VISA PVV</td><td>PIN issuance with VISA PVV.</td></tr>
                        <tr><td><code>EI</code></td><td>Generate RSA Key Pair</td><td>Asymmetric key generation.</td></tr>
                    </tbody>
                </table>
            </div>

            <div class="info-card warning">
                <div class="info-card-title">Important</div>
                <p>Attempting to run a secure command without authorization will return error code <code>A1</code> (Console not authorized). Always grant authorization before executing these operations.</p>
            </div>
        </ui-section>

        <section class="cta">
            <h2>Try it on your own transactions</h2>
            <p>Free and open source. Download the studio and run this simulator on your desk in minutes.</p>
            <div class="row"><a class="btn btn--primary btn-lg" href="/download">⬇ Download Studio</a>
            <a class="btn btn-ghost btn-lg" href="/docs/installation">Installation guide</a></div>
        </section>
    </main>

<aside class="pro-nudge"><span class="pn-tag">✦ Pro</span><p>Testing with a team, or certifying with a scheme? Pro raises the CPS ceiling, unlocks the full algorithm set and deep simulator tweaks, plus hosted endpoints and priority support.</p><a href="/pro">Register for Pro →</a></aside>`,
})
export class DocsHsmSimulatorPage {}
