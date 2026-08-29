import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { SitePage } from './site-page';
import { UiSection } from '../../ui/section';

@Component({
  selector: 'page-docs-host-simulator',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiSection],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-host-simulator' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<main class="doc-body">
        <div class="breadcrumb">
            <a href="/">Home</a>
            <span class="breadcrumb-sep">/</span>
            <span>Documentation</span>
            <span class="breadcrumb-sep">/</span>
            <span>Host Simulator</span>
        </div>

        <h1 class="page-title">Host Simulator</h1>
        <p class="page-description">Simulate acquirer and issuer host responses for payment terminals, ATMs, and client applications. Test your ISO8583 integrations without a production host.</p>

        <!-- Overview -->
        <ui-section anchor="overview" heading="Overview">
            <p>The Host Simulator in ISO8583Studio acts as a payment host simulator for development and testing. It accepts incoming connections from POS terminals, ATMs, or other financial clients and returns configurable responses based on matching rules. This eliminates the need for a real host environment during development and QA testing.</p>

            <div class="shot-grid">
                <figure class="shot-fig wide" style="--shot-w:1400px">
                    <image-slot><img src="/images/docs/host-simulator/overview.png" alt="ISO8583Studio Host Simulator running as Hitachi - 1, ISO8583 Transaction tab showing a formatted 0200 request and 0210 response beside their raw hex" width="2800" height="1683" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">host-simulator</span> Overview</figcaption>
                </figure>
            </div>

            <div class="feature-grid">
                <div class="feature-item">
                    <div class="feature-item-icon">🖥️</div>
                    <h4>Multi-Mode Gateway</h4>
                    <p>Run as Server, Client, or Proxy with support for synchronous and asynchronous transmission.</p>
                </div>
                <div class="feature-item">
                    <div class="feature-item-icon">🔌</div>
                    <h4>Multiple Protocols</h4>
                    <p>TCP/IP, REST/HTTP, RS232 serial, and dial-up connections all supported out of the box.</p>
                </div>
                <div class="feature-item">
                    <div class="feature-item-icon">📋</div>
                    <h4>Configurable Rules</h4>
                    <p>Match requests by MTI, processing code, REST paths, headers, and return dynamic responses.</p>
                </div>
                <div class="feature-item">
                    <div class="feature-item-icon">🔄</div>
                    <h4>Format Support</h4>
                    <p>Handle ISO8583 binary, JSON, XML, HEX, and plain text message formats with conversion.</p>
                </div>
                <div class="feature-item">
                    <div class="feature-item-icon">📨</div>
                    <h4>Unsolicited Messages</h4>
                    <p>Send and receive unsolicited messages for network management and terminal notifications.</p>
                </div>
                <div class="feature-item">
                    <div class="feature-item-icon">📊</div>
                    <h4>Live Monitoring</h4>
                    <p>Real-time transaction monitoring with request/response inspection and hex dump views.</p>
                </div>
            </div>
        </ui-section>


        <!-- Quick Start -->
        <ui-section anchor="quick-start" heading="Quick Start Guide">
            <p>Follow these steps to get a basic host simulator up and running:</p>

            <ol class="steps">
                <li><strong>Create a new simulator</strong> &mdash; From the Home screen, navigate to <code>Simulator &rarr; Host Simulator</code> and create a new configuration.</li>
                <li><strong>Choose a Gateway Type</strong> &mdash; Select <strong>Server</strong> to accept incoming connections, <strong>Client</strong> to connect outward, or <strong>Proxy</strong> to bridge two sides.</li>
                <li><strong>Configure Transmission Settings</strong> &mdash; Set the IP address, port, connection type (TCP/IP or REST), and message length type.</li>
                <li><strong>Configure the ISO8583 Template</strong> &mdash; Open the <strong>ISO8583 Template</strong> tab and import a YAML template via <strong>Upload YAML</strong>, or build the bit definitions manually. Save with the top-bar <strong>Save</strong> button.</li>
                <li><strong>Add Transactions and Responses</strong> &mdash; In the <strong>Settings</strong> tab, click <strong>+</strong> in the transaction list to add a transaction (Description, MTI, Processing Code). Select it, then in the right panel use the <strong>Fields</strong> tab to set the response field values, the <strong>Config</strong> tab for transaction-level options, and the <strong>API</strong> tab for REST matching. Click <strong>Save All</strong> in the header.</li>
                <li><strong>Start the Simulator</strong> &mdash; Open the <strong>ISO8583 Transaction</strong> tab and click <strong>Start</strong>. Incoming traffic appears live in the request / response split view.</li>
            </ol>

            <div class="info-card tip">
                <div class="info-card-title">Tip</div>
                <p>Use placeholders like <code>[SV]</code> in response field values to echo back request values. Useful for fields like System Trace or RRN that must match the original request.</p>
            </div>
        </ui-section>

        <!-- Gateway Types -->
        <ui-section anchor="gateway-types" heading="Gateway Types">
            <p>The host simulator supports three gateway modes, each suited to different testing scenarios:</p>

            <div class="shot-grid">
                <figure class="shot-fig" style="--shot-w:936px">
                    <image-slot><img src="/images/docs/host-simulator/gateway-types.png" alt="Gateway Type selector with Server, Client and Proxy tiles and the Synchronous / Asynchronous transmission type below" width="1872" height="1256" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">host-simulator</span> Gateway Types</figcaption>
                </figure>
            </div>

            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr>
                            <th>Gateway Type</th>
                            <th>Mode</th>
                            <th>Description</th>
                            <th>Use Case</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td><span class="badge badge-blue">Server</span></td>
                            <td>Listens for connections</td>
                            <td>Binds to a port and accepts incoming connections from clients (POS, ATM, payment apps). Parses incoming ISO8583 requests and returns configured responses.</td>
                            <td>Simulating an acquirer or issuer host during terminal testing.</td>
                        </tr>
                        <tr>
                            <td><span class="badge badge-teal">Client</span></td>
                            <td>Initiates connections</td>
                            <td>Connects outward to an external host. Allows sending crafted messages and inspecting responses.</td>
                            <td>Testing outbound integrations, sending test transactions to a real or simulated host.</td>
                        </tr>
                        <tr>
                            <td><span class="badge badge-yellow">Proxy</span></td>
                            <td>Bridges two sides</td>
                            <td>Receives from one side (source), optionally modifies, and forwards to the other side (destination). Returns destination responses to source.</td>
                            <td>Man-in-the-middle testing, message inspection, protocol translation between endpoints.</td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <div class="info-card note">
                <div class="info-card-title">Note</div>
                <p>For <strong>Server</strong> and <strong>Proxy</strong> modes, you can choose between <strong>Synchronous</strong> (one request at a time per connection) and <strong>Asynchronous</strong> (multiple concurrent requests) transmission modes.</p>
            </div>
        </ui-section>

        <!-- Gateway Configuration -->
        <ui-section anchor="gateway-config" heading="Gateway Configuration">
            <p>The Gateway Type tab provides the core configuration for your simulator instance.</p>

            <div class="shot-grid">
                <figure class="shot-fig" style="--shot-w:936px">
                    <image-slot><img src="/images/docs/host-simulator/gateway-config.png" alt="Response Settings with the Realistic response delay selected, Max Concurrent set to 50, and Advanced Options showing Enable Detailed Logging checked" width="1872" height="879" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">host-simulator</span> Gateway Configuration</figcaption>
                </figure>
            </div>

            <h3>Basic Settings</h3>
            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr><th>Setting</th><th>Description</th></tr>
                    </thead>
                    <tbody>
                        <tr><td><strong>Name</strong></td><td>A descriptive name for your simulator instance.</td></tr>
                        <tr><td><strong>Description</strong></td><td>Optional description for documentation purposes.</td></tr>
                        <tr><td><strong>Gateway Type</strong></td><td>Server, Client, or Proxy.</td></tr>
                        <tr><td><strong>Auto-Start</strong></td><td>Automatically start the simulator when the configuration is loaded.</td></tr>
                    </tbody>
                </table>
            </div>

            <h3>Response Delay</h3>
            <p>Control how quickly the simulator responds to incoming requests. Useful for simulating real-world network conditions.</p>
            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr><th>Option</th><th>Delay</th><th>Description</th></tr>
                    </thead>
                    <tbody>
                        <tr><td>Instant</td><td>0 ms</td><td>Responds immediately. Ideal for high-throughput testing.</td></tr>
                        <tr><td>Realistic</td><td>~200-500 ms</td><td>Simulates typical production host response times.</td></tr>
                        <tr><td>Slow</td><td>~1-3 s</td><td>Simulates slow networks or stressed hosts.</td></tr>
                        <tr><td>Custom</td><td>User-defined</td><td>Set an exact delay in milliseconds.</td></tr>
                    </tbody>
                </table>
            </div>

            <h3>Performance Settings</h3>
            <ul>
                <li><strong>Max Concurrent Transactions</strong> &mdash; Limit the number of simultaneous transactions the simulator will process.</li>
                <li><strong>Detailed Logging</strong> &mdash; Enable verbose logging for debugging purposes.</li>
            </ul>
        </ui-section>

        <!-- Transmission Settings -->
        <ui-section anchor="transmission" heading="Transmission Settings">
            <p>Configure how the simulator communicates over the network. Settings differ based on your gateway type.</p>

            <div class="shot-grid">
                <figure class="shot-fig" style="--shot-w:1400px">
                    <image-slot><img src="/images/docs/host-simulator/transmission.png" alt="Incoming and outgoing connection settings side by side, each with a TCP/IP, RS232, Dial-up or REST API connection type and a message length format" width="2800" height="895" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">host-simulator</span> Transmission Settings</figcaption>
                </figure>
            </div>

            <h3>Incoming Connection (Server / Proxy)</h3>
            <p>Configure the listener that accepts incoming connections:</p>
            <ul>
                <li><strong>IP Address</strong> &mdash; Bind address (e.g. <code>0.0.0.0</code> for all interfaces, or a specific IP).</li>
                <li><strong>Port</strong> &mdash; TCP port to listen on.</li>
                <li><strong>Message Length Type</strong> &mdash; How message boundaries are determined.</li>
                <li><strong>Max Concurrent Connections</strong> &mdash; Limit on simultaneous client connections.</li>
                <li><strong>Timeout</strong> &mdash; Connection idle timeout in seconds.</li>
            </ul>

            <h3>Outgoing Connection (Client / Proxy)</h3>
            <p>Configure the connection to the destination host:</p>
            <ul>
                <li><strong>Target Address</strong> &mdash; Hostname or IP of the target.</li>
                <li><strong>Target Port</strong> &mdash; Port to connect to.</li>
                <li><strong>Terminate on Error</strong> &mdash; Whether to close the connection on errors.</li>
            </ul>

            <h3>Message Length Types</h3>
            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr><th>Type</th><th>Description</th></tr>
                    </thead>
                    <tbody>
                        <tr><td><code>BCD</code></td><td>Binary-coded decimal length header.</td></tr>
                        <tr><td><code>NONE</code></td><td>No length header; messages are delimited by the stream itself.</td></tr>
                        <tr><td><code>STRING_4</code></td><td>4-character ASCII string length prefix.</td></tr>
                        <tr><td><code>HEX_HL</code></td><td>2-byte hex length header, high byte first.</td></tr>
                        <tr><td><code>HEX_LH</code></td><td>2-byte hex length header, low byte first.</td></tr>
                    </tbody>
                </table>
            </div>
        </ui-section>

        <!-- Connection Types -->
        <ui-section anchor="connection-types" heading="Connection Types">
            <p>The simulator supports multiple connection protocols for maximum flexibility:</p>

            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr><th>Type</th><th>Protocol</th><th>Status</th><th>Description</th></tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td><span class="badge badge-green">TCP/IP</span></td>
                            <td>TCP Socket</td>
                            <td>Full support</td>
                            <td>Standard TCP/IP socket connections. Most common for ISO8583 host communication.</td>
                        </tr>
                        <tr>
                            <td><span class="badge badge-blue">REST</span></td>
                            <td>HTTP/HTTPS</td>
                            <td>Full support</td>
                            <td>REST API endpoints supporting JSON, XML, HEX, BASE64, BINARY, and FORM_DATA formats.</td>
                        </tr>
                        <tr>
                            <td><span class="badge badge-yellow">COM</span></td>
                            <td>RS232 Serial</td>
                            <td>Available</td>
                            <td>Serial port communication with configurable baud rate, data bits, stop bits, and parity.</td>
                        </tr>
                        <tr>
                            <td><span class="badge badge-yellow">Dial-Up</span></td>
                            <td>Modem</td>
                            <td>Available</td>
                            <td>Legacy modem dial-up connections via phone number.</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </ui-section>

        <!-- Message Formats -->
        <ui-section anchor="message-formats" heading="Message Formats">
            <p>The simulator can handle multiple message encoding formats for both source and destination.</p>

            <h3>ISO8583 Formats</h3>
            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr><th>Format</th><th>Description</th></tr>
                    </thead>
                    <tbody>
                        <tr><td><code>BYTE_ARRAY</code></td><td>Standard binary ISO8583 encoding. Default for most payment hosts.</td></tr>
                        <tr><td><code>JSON</code></td><td>JSON representation with configurable field mapping.</td></tr>
                        <tr><td><code>XML</code></td><td>XML representation with configurable field mapping.</td></tr>
                        <tr><td><code>HEX</code></td><td>Hexadecimal string representation of the binary message.</td></tr>
                        <tr><td><code>PLAIN_TEXT</code></td><td>Key-value pairs with configurable delimiters.</td></tr>
                    </tbody>
                </table>
            </div>

            <h3>REST Message Formats</h3>
            <p>When using REST connections, the following content types are supported:</p>
            <ul>
                <li><strong>JSON</strong> &mdash; Application/json content type</li>
                <li><strong>XML</strong> &mdash; Application/xml content type</li>
                <li><strong>HEX</strong> &mdash; Hex-encoded message body</li>
                <li><strong>BASE64</strong> &mdash; Base64-encoded message body</li>
                <li><strong>BINARY</strong> &mdash; Raw binary body</li>
                <li><strong>FORM_DATA</strong> &mdash; Multipart form data</li>
            </ul>

            <h3>Format Mapping</h3>
            <p>For non-binary formats (JSON, XML, PlainText), use the <strong>Format Mapping Config</strong> in the ISO8583 Template tab to map ISO8583 fields to JSON keys, XML elements, or delimited fields.</p>
        </ui-section>

        <!-- Log Settings -->
        <ui-section anchor="log-settings" heading="Log Settings">
            <p>Configure how the simulator logs transactions and connections.</p>

            <div class="shot-grid">
                <figure class="shot-fig" style="--shot-w:936px">
                    <image-slot><img src="/images/docs/host-simulator/log-settings.png" alt="Logging Options with logfile name and max size, and Logging Content set to Parsed data using the ISO8583 protocol" width="1464" height="820" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">host-simulator</span> Log Settings</figcaption>
                </figure>
            </div>

            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr><th>Setting</th><th>Description</th></tr>
                    </thead>
                    <tbody>
                        <tr><td><strong>Log Filename</strong></td><td>Output file path for transaction logs.</td></tr>
                        <tr><td><strong>Max Log Size</strong></td><td>Maximum log file size in MB before rotation.</td></tr>
                        <tr><td><strong>Simple</strong></td><td>Basic transaction summaries (MTI, response code, timestamps).</td></tr>
                        <tr><td><strong>Raw Data</strong></td><td>Full hex dump of request/response messages.</td></tr>
                        <tr><td><strong>Text Data</strong></td><td>Text-decoded message content with configurable encoding.</td></tr>
                        <tr><td><strong>Parsed Data</strong></td><td>Fully parsed ISO8583 fields using a template file for structured output.</td></tr>
                    </tbody>
                </table>
            </div>
        </ui-section>

        <!-- Host Handler -->
        <ui-section anchor="host-handler" heading="ISO8583 Transaction Tab">
            <p>The <strong>ISO8583 Transaction</strong> tab (visible for Server and Proxy gateway types) is the main runtime interface for controlling and monitoring the simulator.</p>

            <div class="shot-grid shot-row" style="--row-w:1400px">
                <figure class="shot-fig" style="--shot-w:898px">
                    <image-slot><img src="/images/docs/host-simulator/host-handler.png" alt="Message Parser showing a raw ISO8583 hex message parsed into 12 fields, with field 25 selected and its point of service condition code decoded" width="2800" height="1281" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">host-simulator</span> Message Parser &mdash; parsed fields and per-field detail</figcaption>
                </figure>
                <figure class="shot-fig" style="--shot-w:484px">
                    <image-slot><img src="/images/docs/host-simulator/host-handler-bitmap.png" alt="Bitmap Analysis grid for hex bitmap 3038048020C00014, highlighting the twelve set fields out of 64" width="1118" height="948" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">host-simulator</span> Bitmap Analysis</figcaption>
                </figure>
            </div>

            <h3>Controls</h3>
            <ul>
                <li><strong>Start / Stop</strong> &mdash; Launch or shut down the simulator server.</li>
                <li><strong>Hold Message</strong> &mdash; When enabled, the simulator will not auto-respond. Responses are sent only when you manually click "Send" or after a configurable delay. Useful for inspecting requests before responding.</li>
            </ul>

            <h3>Live View</h3>
            <p>The transaction view provides real-time visibility into all traffic:</p>
            <ul>
                <li><strong>Formatted View</strong> &mdash; Parsed ISO8583 fields shown in a readable table.</li>
                <li><strong>Raw Hex View</strong> &mdash; Full hexadecimal dump of the message bytes.</li>
                <li><strong>Request / Response Split</strong> &mdash; Side-by-side view of incoming request and outgoing response.</li>
            </ul>
        </ui-section>

        <!-- Transaction Rules -->
        <ui-section anchor="transaction-rules" heading="Transaction Rules (Settings Tab)">
            <p>Transaction rules define how the simulator matches incoming requests and what responses to return. Configure them in the <strong>Settings</strong> tab.</p>

            <div class="shot-grid">
                <figure class="shot-fig" style="--shot-w:936px">
                    <image-slot><img src="/images/docs/host-simulator/transaction-rules.png" alt="Transaction Simulator with SALE, REVERSAL and VOID transactions listed on the left and the Fields tab on the right showing 13 configured ISO8583 fields" width="1872" height="878" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">host-simulator</span> Transaction Rules (Settings Tab)</figcaption>
                </figure>
            </div>

            <h3>Settings Tab Layout</h3>
            <ul>
                <li><strong>Header bar</strong> &mdash; Field info, Export, Import, <strong>Save All</strong>, plus a <strong>Source / Destination</strong> toggle (Server simulators default to Source; Proxy mode lets you stage rules for either side).</li>
                <li><strong>Left panel: Transaction list</strong> &mdash; Add, edit, duplicate, delete transactions. Click a row to select it.</li>
                <li><strong>Right panel: Per-transaction tabs</strong> &mdash; <code>Fields</code>, <code>Config</code>, and (REST connections only) <code>API</code>.</li>
            </ul>

            <h3>Adding a Transaction</h3>
            <ol class="steps">
                <li>Click <strong>+</strong> at the top of the transaction list. The <em>Add Transaction</em> dialog opens.</li>
                <li>Fill in <strong>Description *</strong> (e.g. <code>Purchase Transaction</code>).</li>
                <li>Fill in <strong>MTI *</strong> (e.g. <code>0200</code>).</li>
                <li>Fill in <strong>Processing Code *</strong> (e.g. <code>000000</code>; use <code>*</code> as a wildcard for any code).</li>
                <li>Click <strong>Save</strong>. The new transaction appears in the list.</li>
            </ol>

            <h3>Setting up the Response (Fields tab)</h3>
            <ol class="steps">
                <li>Select the transaction in the left list.</li>
                <li>Open the <strong>Fields</strong> tab on the right.</li>
                <li>Click <strong>Add Fields</strong> to pick which ISO 8583 bits the response should contain.</li>
                <li>For each added field, type its response value. Use placeholders &mdash; <code>[SV]</code> echoes the request value, <code>[TIME]</code> generates a timestamp, <code>[RAND]</code> generates random digits.</li>
                <li>Click <strong>Save All</strong> in the header bar to persist the rules.</li>
            </ol>

            <h3>Per-Transaction Config &amp; API tabs</h3>
            <ul>
                <li><strong>Config tab</strong> &mdash; Transaction-level options such as response delay overrides and conditional logic.</li>
                <li><strong>API tab (REST connections only)</strong> &mdash; <em>API Path</em>, request matchers (key path / operator / value), response mappers, and headers.</li>
            </ul>

            <h3>Matching Logic</h3>
            <p>When a request arrives, the simulator evaluates rules in order:</p>
            <ol>
                <li>Parse the incoming message to extract MTI and processing code (Field 3).</li>
                <li>Compare against each configured transaction rule.</li>
                <li>The <strong>first matching rule</strong> wins and its response fields are used to build the response.</li>
            </ol>

            <div class="info-card tip">
                <div class="info-card-title">Tip</div>
                <p>Place more specific rules (with exact processing codes) before generic ones (with <code>*</code> wildcard) to ensure correct matching priority.</p>
            </div>

            <h3>Example Rule</h3>
            <pre><code>Description:     Purchase Transaction
MTI:             0200
Processing Code: 000000

Response Fields (Fields tab):
  Field 39 (Response Code):     00
  Field 38 (Auth Code):         [RAND]
  Field 11 (System Trace):      [SV]
  Field 37 (RRN):               [SV]
  Field 12 (Local Time):        [TIME]
  Field 13 (Local Date):        [TIME]</code></pre>
        </ui-section>

        <!-- Placeholders -->
        <ui-section anchor="placeholders" heading="Dynamic Placeholders">
            <p>Use placeholders in response field values to generate dynamic content at runtime instead of static values.</p>

            <div class="ph-grid">
                <div class="ph-card ph-sv">
                    <div class="ph-tok">[SV]</div>
                    <h4>Source Value</h4>
                    <p>Copies data from the corresponding field in the request message.</p>
                    <div class="ph-ex">
                        <b>Examples</b>
                        <ul>
                            <li>Field 3: [SV] <span>&rarr; Processing Code</span></li>
                            <li>Field 11: [SV] <span>&rarr; STAN</span></li>
                            <li>Field 37: [SV] <span>&rarr; RRN</span></li>
                        </ul>
                    </div>
                    <p class="ph-note">Useful for response messages where you need to echo back values from the original request.</p>
                </div>
                <div class="ph-card ph-time">
                    <div class="ph-tok">[TIME]</div>
                    <h4>Current Time</h4>
                    <p>Generates a current timestamp based on the field&rsquo;s expected length.</p>
                    <div class="ph-ex">
                        <b>Examples</b>
                        <ul>
                            <li>Field 12 (6): [TIME] <span>&rarr; 143052 (HHmmss)</span></li>
                            <li>Field 13 (4): [TIME] <span>&rarr; 1204 (MMdd)</span></li>
                            <li>Field 7 (10): [TIME] <span>&rarr; 1204143052</span></li>
                        </ul>
                    </div>
                    <p class="ph-note">Automatically formats the current time according to ISO8583 field specifications.</p>
                </div>
                <div class="ph-card ph-rand">
                    <div class="ph-tok">[RAND]</div>
                    <h4>Random Number</h4>
                    <p>Generates a random numeric value based on the field&rsquo;s maximum length.</p>
                    <div class="ph-ex">
                        <b>Examples</b>
                        <ul>
                            <li>Field 11 (6): [RAND] <span>&rarr; 123456</span></li>
                            <li>Field 37 (12): [RAND] <span>&rarr; 987654321098</span></li>
                            <li>Field 38 (6): [RAND] <span>&rarr; 456789</span></li>
                        </ul>
                    </div>
                    <p class="ph-note">Generates unique values for fields like STAN, RRN or authorization codes during testing.</p>
                </div>
            </div>

            <div class="info-card note">
                <div class="info-card-title">Note</div>
                <p>Placeholders can be combined with static text. Any field value that does not contain a placeholder is sent as a literal static value.</p>
            </div>
        </ui-section>

        <!-- REST Matching -->
        <ui-section anchor="rest-matching" heading="REST API Matching">
            <p>When using REST connections, the simulator provides advanced request matching beyond MTI/processing code.</p>

            <h3>Match Criteria</h3>
            <ul>
                <li><strong>Path</strong> &mdash; Match the URL path exactly or use <code>*</code> as a wildcard.</li>
                <li><strong>HTTP Method</strong> &mdash; Match by GET, POST, PUT, DELETE, etc.</li>
                <li><strong>Query Parameters</strong> &mdash; Match by URL query parameters.</li>
                <li><strong>Headers</strong> &mdash; Match by HTTP request headers.</li>
                <li><strong>Body Fields</strong> &mdash; Match by fields in the request body (JSON/XML).</li>
            </ul>

            <h3>Match Operators</h3>
            <p>Each matcher supports these comparison operators:</p>
            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr><th>Operator</th><th>Description</th></tr>
                    </thead>
                    <tbody>
                        <tr><td><code>EQUALS</code></td><td>Exact string match.</td></tr>
                        <tr><td><code>NOT_EQUALS</code></td><td>Value does not equal the specified string.</td></tr>
                        <tr><td><code>STARTS_WITH</code></td><td>Value starts with the specified prefix.</td></tr>
                        <tr><td><code>ENDS_WITH</code></td><td>Value ends with the specified suffix.</td></tr>
                        <tr><td><code>CONTAINS</code></td><td>Value contains the specified substring.</td></tr>
                        <tr><td><code>REGEX</code></td><td>Value matches a regular expression pattern.</td></tr>
                        <tr><td><code>GREATER_THAN</code></td><td>Numeric comparison (value > threshold).</td></tr>
                        <tr><td><code>LESS_THAN</code></td><td>Numeric comparison (value < threshold).</td></tr>
                    </tbody>
                </table>
            </div>

            <h3>Response Mapping</h3>
            <p>For REST responses, use <strong>Response Mapping</strong> to define which fields appear in the response body and headers:</p>
            <ul>
                <li><strong>targetKey</strong> &mdash; JSON/XML key in the response body.</li>
                <li><strong>value</strong> &mdash; Static value or placeholder (<code>[SV]</code>, <code>[TIME]</code>, <code>[RAND]</code>).</li>
                <li><strong>targetHeader</strong> &mdash; Set a response HTTP header instead of a body field.</li>
            </ul>
        </ui-section>

        <!-- ISO8583 Template -->
        <ui-section anchor="iso8583-template" heading="ISO8583 Template Tab">
            <p>The <strong>ISO8583 Template</strong> tab defines the message specification used for parsing and building messages.</p>

            <div class="shot-grid">
                <figure class="shot-fig" style="--shot-w:936px">
                    <image-slot><img src="/images/docs/host-simulator/iso8583-template.png" alt="Source Bit Templates table listing each bit with its format type, length type, max length and description, beside the source advanced options and message format panels" width="1872" height="868" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">host-simulator</span> ISO8583 Template Tab</figcaption>
                </figure>
            </div>

            <h3>Top-bar actions</h3>
            <ul>
                <li><strong>Save</strong> &mdash; Persist template changes. Send Message and other tabs re-sync immediately.</li>
                <li><strong>Upload YAML</strong> &mdash; Import a YAML template (the fastest way to bootstrap a known message spec).</li>
                <li><strong>Download Template</strong> &mdash; Export the current template to YAML.</li>
            </ul>

            <h3>Per-Bit Editor</h3>
            <p>Click any bit number to edit it in a side dialog with three required attributes:</p>
            <ul>
                <li><strong>Bit Length</strong> &mdash; Maximum length (in bytes or characters depending on type).</li>
                <li><strong>Bit Type</strong> &mdash; Field type (numeric, alphanumeric, binary, etc.).</li>
                <li><strong>Max Length</strong> &mdash; Used together with the length type (fixed, LLVAR, LLLVAR, LLLLVAR).</li>
            </ul>
            <p>Add or remove bits with the <strong>Add</strong> / <strong>Delete</strong> buttons.</p>

            <h3>Template-level Options (toggles)</h3>
            <ul>
                <li><strong>Iso8583 use Ascii</strong> &mdash; Treat numeric fields as ASCII rather than packed BCD.</li>
                <li><strong>Don&rsquo;t use TPDU Header</strong> &mdash; Skip the 5-byte TPDU prefix.</li>
                <li><strong>Respond same message if unrecognized</strong> &mdash; Echo unmatched requests instead of returning an error.</li>
                <li><strong>Metfone message</strong> &mdash; Enable Metfone-specific framing.</li>
                <li><strong>Not update screen</strong> &mdash; Suppress UI updates for high-throughput tests.</li>
            </ul>

            <div class="info-card warning">
                <div class="info-card-title">Important</div>
                <p>The template must match the message specification used by the connecting client. Mismatched templates will cause parsing errors. Ensure field lengths, formats, and encoding match your ISO8583 specification.</p>
            </div>
        </ui-section>

        <!-- Unsolicited Messages -->
        <ui-section anchor="unsolicited" heading="Unsolicited Messages">
            <p>Unsolicited messages are messages sent outside the normal request-response flow, typically for network management, notifications, or terminal updates.</p>

            <div class="shot-grid">
                <figure class="shot-fig" style="--shot-w:1400px">
                    <image-slot><img src="/images/docs/host-simulator/host-handler.png" alt="Message Parser showing a raw ISO8583 hex message parsed into 12 fields, with field 25 selected and its point of service condition code decoded" width="2800" height="1281" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">host-simulator</span> Message Parser &mdash; incoming messages parsed against the active template</figcaption>
                </figure>
            </div>

            <h3>Send Message (Client / Proxy)</h3>
            <p>Craft and send outbound unsolicited messages to connected hosts. Available in <strong>Client</strong> and <strong>Proxy</strong> modes. Use the message builder to construct ISO8583 messages with specific fields and send them on demand.</p>

            <h3>Receive Unsolicited (Server / Proxy)</h3>
            <p>In <strong>Server</strong> and <strong>Proxy</strong> modes, incoming unsolicited messages from clients are displayed in the Unsolicited Message tab. Messages are parsed using the active ISO8583 template.</p>
        </ui-section>

        <!-- Security -->
        <ui-section anchor="security" heading="Security Options">
            <p>The simulator supports several security features for testing encrypted and authenticated connections.</p>

            <h3>Cipher Types</h3>
            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr><th>Algorithm</th><th>Key Sizes</th></tr>
                    </thead>
                    <tbody>
                        <tr><td>DES</td><td>56-bit</td></tr>
                        <tr><td>Triple DES (3DES)</td><td>112/168-bit</td></tr>
                        <tr><td>AES</td><td>128, 192, 256-bit</td></tr>
                        <tr><td>RSA</td><td>Variable</td></tr>
                    </tbody>
                </table>
            </div>

            <h3>Cipher Modes</h3>
            <p>ECB, CBC, CFB, OFB, and CTS modes are available for block cipher operations.</p>

            <h3>Authentication</h3>
            <ul>
                <li><strong>None</strong> &mdash; No authentication required.</li>
                <li><strong>Single Password</strong> &mdash; One shared password for all clients.</li>
                <li><strong>Client Password</strong> &mdash; Per-client authentication credentials.</li>
            </ul>

            <h3>SSL/TLS</h3>
            <p>Enable SSL for both the server listener and REST API client connections for encrypted transport.</p>
        </ui-section>

        <!-- Tabs Reference -->
        <ui-section anchor="tabs-reference" heading="Tabs Reference">
            <p>The runtime simulator screen exposes a different subset of tabs depending on the gateway type.</p>

            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr>
                            <th>Tab</th>
                            <th>Server</th>
                            <th>Client</th>
                            <th>Proxy</th>
                            <th>Description</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td><strong>ISO8583 Transaction</strong></td>
                            <td>&#10003;</td>
                            <td>&mdash;</td>
                            <td>&#10003;</td>
                            <td>Start / stop, live request / response view, hold message control.</td>
                        </tr>
                        <tr>
                            <td><strong>Logs</strong></td>
                            <td>&#10003;</td>
                            <td>&#10003;</td>
                            <td>&#10003;</td>
                            <td>Transaction and connection logs, connection count, bytes in / out.</td>
                        </tr>
                        <tr>
                            <td><strong>Settings</strong></td>
                            <td>&#10003;</td>
                            <td>&mdash;</td>
                            <td>&#10003;</td>
                            <td>Transaction list with Fields / Config / API tabs for each transaction.</td>
                        </tr>
                        <tr>
                            <td><strong>ISO8583 Template</strong></td>
                            <td>&#10003;</td>
                            <td>&#10003;</td>
                            <td>&#10003;</td>
                            <td>Message specification, bit definitions, YAML import / export, template-level toggles.</td>
                        </tr>
                        <tr>
                            <td><strong>Send Message</strong></td>
                            <td>&#10003;</td>
                            <td>&#10003;</td>
                            <td>&mdash;</td>
                            <td>Craft and send outbound ISO 8583 messages; inspect responses.</td>
                        </tr>
                        <tr>
                            <td><strong>Load Test</strong></td>
                            <td>&mdash;</td>
                            <td>&#10003;</td>
                            <td>&mdash;</td>
                            <td>Concurrency / throughput testing for client-mode integrations.</td>
                        </tr>
                        <tr>
                            <td><strong>Unsolicited Message</strong></td>
                            <td>&#10003;</td>
                            <td>&#10003;</td>
                            <td>&#10003;</td>
                            <td>View incoming unsolicited messages from connected peers.</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </ui-section>

    </main>

<aside class="pro-nudge"><span class="pn-tag">✦ Pro</span><p>Testing with a team, or certifying with a scheme? Pro raises the CPS ceiling, unlocks the full algorithm set and deep simulator tweaks, plus hosted endpoints and priority support.</p><a href="/pro">Register for Pro →</a></aside>`,
})
export class DocsHostSimulatorPage {}
