import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { SitePage } from './site-page';
import { UiSection } from '../../ui/section';

@Component({
  selector: 'page-docs-mac-tools',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiSection],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-mac-tools' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<main class="doc-body">
        <div class="breadcrumb">
            <a href="/">Home</a>
            <span class="breadcrumb-sep">/</span>
            <a href="/docs">Documentation</a>
            <span class="breadcrumb-sep">/</span>
            <span>MAC Tools</span>
        </div>

        <h1 class="page-title">MAC Tools</h1>
        <p class="page-description">Generate and verify Message Authentication Codes used across payment systems &mdash; HMAC, the six ISO/IEC 9797-1 algorithms, ANSI X9.9 / X9.19, and TDES CBC-MAC. Each tool offers hex inputs, configurable padding, a truncation length, and a step-by-step audit log.</p>

        <ui-section anchor="overview" heading="Introduction">
            <p>A Message Authentication Code (MAC) is a short tag computed from a message and a secret key. It lets a receiver verify that a message has not been altered and that it came from someone holding the same key. Payment networks rely on MACs heavily &mdash; on ISO 8583 messages, on terminal-host links, and on PIN-translation pipelines.</p>

            <p>ISO8583Studio includes calculators for every MAC algorithm commonly seen in payment specifications, organised into two families: hash-based (HMAC) and block-cipher-based (CMAC, CBC-MAC and its variants).</p>
        </ui-section>

        <ui-section anchor="choosing" heading="Choosing a MAC">
            <div class="table-wrapper">
                <table>
                    <thead><tr><th>Algorithm</th><th>Standard</th><th>Block / Cipher</th><th>Common Use</th></tr></thead>
                    <tbody>
                        <tr><td><strong>HMAC</strong></td><td>RFC 2104, FIPS 198-1</td><td>Hash (SHA-256, etc.)</td><td>API authentication, JWS, payment APIs.</td></tr>
                        <tr><td><strong>CMAC</strong></td><td>NIST SP 800-38B</td><td>AES / TDES</td><td>EMV-like cryptograms, modern PIN translation.</td></tr>
                        <tr><td><strong>TDES CBC-MAC</strong></td><td>ANSI X9.9 (legacy)</td><td>3DES</td><td>Older banking integrations.</td></tr>
                        <tr><td><strong>ANSI X9.19 MAC</strong></td><td>ANSI X9.19</td><td>Single DES + 3DES finalize</td><td>U.S. retail / banking ISO 8583.</td></tr>
                        <tr><td><strong>ISO 9797 MAC</strong></td><td>ISO/IEC 9797-1</td><td>DES / 3DES / AES</td><td>Cross-network ISO 8583 MACs.</td></tr>
                        <tr><td><strong>Retail MAC</strong></td><td>ISO 9797-1 Algorithm 3</td><td>3DES</td><td>European retail payments.</td></tr>
                        <tr><td><strong>AS2805 MAC</strong></td><td>AS 2805.4</td><td>3DES</td><td>Australian payment systems.</td></tr>
                    </tbody>
                </table>
            </div>
        </ui-section>

        <ui-section anchor="hmac" heading="HMAC Calculator">
            <p>Hash-based MAC defined by RFC 2104.</p>

            <div class="shot-grid">
                <figure class="shot-fig" data-shot="hmac" data-alt="HMAC Generation form with SHA-256 as the hash type, ASCII key and data input types, and HMAC Key and Data fields above the Generate HMAC button" style="--shot-w:749px">
                    <image-slot><img src="/images/docs/payment-utilities/mac-hmac.png"
                                     alt="HMAC Generation form with SHA-256 as the hash type, ASCII key and data input types, and HMAC Key and Data fields above the Generate HMAC button"
                                     width="1498" height="1144" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">mac-tools</span> HMAC Calculator</figcaption>
                </figure>
            </div>

            <h3>Inputs</h3>
            <div class="table-wrapper">
                <table>
                    <thead><tr><th>Field</th><th>Description</th></tr></thead>
                    <tbody>
                        <tr><td><strong>Hash Type</strong></td><td>Drop-down: <code>MD5</code>, <code>SHA-1</code>, <code>SHA-224</code>, <code>SHA-256</code>, <code>SHA-384</code>, <code>SHA-512</code>, <code>RIPEMD-160</code>.</td></tr>
                        <tr><td><strong>Key Input</strong></td><td>Drop-down: <code>ASCII</code> or <code>Hexadecimal</code>.</td></tr>
                        <tr><td><strong>HMAC Key</strong></td><td>Single-line text in the chosen format.</td></tr>
                        <tr><td><strong>Data Input</strong></td><td>Drop-down: <code>ASCII</code> or <code>Hexadecimal</code>.</td></tr>
                        <tr><td><strong>Data</strong></td><td>Multi-line text in the chosen format.</td></tr>
                    </tbody>
                </table>
            </div>

            <h3>Walk-through</h3>
            <ol class="steps">
                <li><strong>Pick Hash Type</strong> &mdash; <code>SHA-256</code> is a safe default for new work.</li>
                <li><strong>Pick Key Input</strong> &mdash; <code>ASCII</code> or <code>Hexadecimal</code>.</li>
                <li><strong>Enter HMAC Key</strong> in the format you selected.</li>
                <li><strong>Pick Data Input</strong> &mdash; <code>ASCII</code> or <code>Hexadecimal</code>.</li>
                <li><strong>Enter Data</strong>.</li>
                <li><strong>Click <code>Generate HMAC</code></strong> &mdash; Output is hex of the digest length (32 bytes for SHA-256, 64 for SHA-512, etc.) and is appended to the activity log.</li>
            </ol>

            <pre><code>Hash Type:  SHA-256
Key Input:  Hexadecimal
HMAC Key:   4A656665
Data Input: ASCII
Data:       what do ya want for nothing?

Output:     5BDCC146BF60754E6A042426089575C75A003F089D2739839DEC58B964EC3843</code></pre>

            <div class="info-card note">
                <div class="info-card-title">No SHA-3 / SM3</div>
                <p>The current build does not expose SHA-3 or SM3. Use the supported algorithms above.</p>
            </div>
        </ui-section>

        <ui-section anchor="tdes-cbc" heading="TDES CBC-MAC Calculator">
            <p>Triple-DES CBC-MAC: encrypt the message under TDES in CBC mode and take the last block as the MAC. Common in legacy ISO 8583 implementations.</p>

            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="tdes-cbc" data-alt="TDES CBC-MAC Generation form with a 32-hex Key field, ISO9797-1 Padding Method 1, a hex Data field and a truncation length of 8 above the Generate MAC button" style="--shot-w:749px">
                    <image-slot><img src="/images/docs/payment-utilities/mac-tdes-cbc.png"
                                     alt="TDES CBC-MAC Generation form with a 32-hex Key field, ISO9797-1 Padding Method 1, a hex Data field and a truncation length of 8 above the Generate MAC button"
                                     width="1498" height="1150" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">mac-tools</span> TDES CBC-MAC Calculator</figcaption>
                </figure>
                <div class="shot-split-body">
                <h3>Inputs</h3>
                <ul>
                    <li><strong>MAC Algorithm</strong> &mdash; Fixed to <code>TDES CBC-MAC</code> on this tool.</li>
                    <li><strong>Key (K)</strong> &mdash; 32 hex chars (2-key 3DES).</li>
                    <li><strong>Padding</strong> &mdash; Drop-down; defaults to <code>ISO9797-1 (Padding Method 1)</code>.</li>
                    <li><strong>Data (Hex)</strong> &mdash; The message to authenticate.</li>
                    <li><strong>Truncation Length (Chars)</strong> &mdash; Hex characters of MAC to keep; defaults to <code>8</code>.</li>
                </ul>
                <p>Button: <strong>Generate MAC</strong>.</p>
                </div>
            </div>

            <div class="info-card warning">
                <div class="info-card-title">Security note</div>
                <p>Pure CBC-MAC is vulnerable to length-extension when the message length is variable. For variable-length messages, use ISO 9797-1 Algorithm 3 (Retail MAC) instead.</p>
            </div>
        </ui-section>

        <ui-section anchor="ansi-mac" heading="ANSI X9.9 / X9.19 MAC Calculator">
            <p>One tool covers both ANSI schemes: <strong>X9.9</strong> (wholesale) and <strong>X9.19</strong> (retail), selected from the MAC Algorithm drop-down. X9.19 runs single-DES CBC-MAC across the message with a final 3DES &ldquo;finalize&rdquo; step over the last block, which makes it equivalent to ISO 9797-1 Algorithm 3.</p>

            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="ansi-mac" data-alt="ANSI MAC Generation form with ANSI MAC X9.9 (Wholesale MAC) selected, a Key (K) field, a hex Data field and a truncation length of 8 above the Generate MAC button" style="--shot-w:750px">
                    <image-slot><img src="/images/docs/payment-utilities/mac-ansi-x9-9-x9-19.png"
                                     alt="ANSI MAC Generation form with ANSI MAC X9.9 (Wholesale MAC) selected, a Key (K) field, a hex Data field and a truncation length of 8 above the Generate MAC button"
                                     width="1500" height="992" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">mac-tools</span> ANSI X9.9 / X9.19 MAC Calculator</figcaption>
                </figure>
                <div class="shot-split-body">
                <h3>Inputs</h3>
                <ul>
                    <li><strong>MAC Algorithm</strong> &mdash; <code>ANSI MAC X9.9 (Wholesale MAC)</code> or the X9.19 retail variant.</li>
                    <li><strong>Key (K)</strong> &mdash; Hex key; the 3DES halves are taken from it in order.</li>
                    <li><strong>Data (Hex)</strong> &mdash; The message to authenticate.</li>
                    <li><strong>Truncation Length (Chars)</strong> &mdash; Defaults to <code>8</code>.</li>
                </ul>
                <p>Button: <strong>Generate MAC</strong>.</p>
                </div>
            </div>

            <h3>Algorithm Summary</h3>
            <ol>
                <li>Split message into 8-byte blocks.</li>
                <li>CBC-encrypt each block under <code>KL</code> (single DES), feeding output into the next block.</li>
                <li>Decrypt the final intermediate value with <code>KR</code>.</li>
                <li>Encrypt that result with <code>KL</code> again. The output is the MAC.</li>
            </ol>
        </ui-section>

        <ui-section anchor="iso9797" heading="ISO 9797 MAC Calculator">
            <p>ISO/IEC 9797-1 standardises six MAC algorithms over block ciphers. The calculator exposes each variant by tab.</p>

            <div class="shot-grid">
                <figure class="shot-fig" data-shot="iso9797" data-alt="ISO/IEC 9797-1 MAC form with Algorithm 1 selected, a Key (K prime) field, Padding Method 1, a hex Data field and a truncation length of 8 above the Generate MAC button" style="--shot-w:750px">
                    <image-slot><img src="/images/docs/payment-utilities/mac-iso9797-1.png"
                                     alt="ISO/IEC 9797-1 MAC form with Algorithm 1 selected, a Key (K prime) field, Padding Method 1, a hex Data field and a truncation length of 8 above the Generate MAC button"
                                     width="1500" height="1142" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">mac-tools</span> ISO 9797 MAC Calculator</figcaption>
                </figure>
            </div>

            <h3>Algorithms</h3>
            <div class="table-wrapper">
                <table>
                    <thead><tr><th>Variant</th><th>Description</th><th>Typical Cipher</th></tr></thead>
                    <tbody>
                        <tr><td><strong>Algorithm 1</strong></td><td>Plain CBC-MAC. Single key. Last block is the MAC.</td><td>DES / 3DES / AES</td></tr>
                        <tr><td><strong>Algorithm 2</strong></td><td>Last-block encrypted with a derived key (<code>K&prime;</code>).</td><td>DES</td></tr>
                        <tr><td><strong>Algorithm 3</strong></td><td>Retail MAC: single DES CBC-MAC, then 3DES finalize. Equivalent to ANSI X9.19.</td><td>DES + 3DES</td></tr>
                        <tr><td><strong>Algorithm 4</strong></td><td>CBC-MAC with two parallel CBC-MAC chains XOR-combined.</td><td>DES / 3DES</td></tr>
                        <tr><td><strong>Algorithm 5</strong></td><td>EMAC: CBC-MAC re-encrypted with a second key.</td><td>AES</td></tr>
                        <tr><td><strong>Algorithm 6</strong></td><td>MAC double-CBC encryption with separate keys.</td><td>AES</td></tr>
                    </tbody>
                </table>
            </div>

            <h3>Inputs</h3>
            <ul>
                <li><strong>MAC Algorithm</strong> &mdash; Drop-down, <code>Algorithm 1</code> through <code>Algorithm 6</code>.</li>
                <li><strong>Key (K&prime;)</strong> &mdash; Hex; the algorithms that need a second key derive it from this one.</li>
                <li><strong>Padding</strong> &mdash; Drop-down: <code>Method 1</code>, <code>Method 2</code>, <code>Method 3</code>.</li>
                <li><strong>Data (Hex)</strong> &mdash; The message to authenticate.</li>
                <li><strong>Truncation Length (Chars)</strong> &mdash; Defaults to <code>8</code>.</li>
            </ul>
            <p>Button: <strong>Generate MAC</strong>.</p>

            <div class="info-card note">
                <div class="info-card-title">Most common in payments</div>
                <p>Algorithm 1 with TDES is widespread for ISO 8583 MAC fields (bit 64 / 128). Algorithm 3 (Retail MAC) is the European retail standard.</p>
            </div>

            <h3>Example: Algorithm 1, TDES, Method 2 padding</h3>
            <pre><code>Key:    0123456789ABCDEFFEDCBA9876543210
IV:     0000000000000000
Method: 2 (0x80 followed by zero bytes)
Data:   48656C6C6F     (ASCII "Hello")

Padded: 48656C6C6F800000
Output: B11FFC78A4FB1B5A</code></pre>
        </ui-section>

        <ui-section anchor="padding" heading="Padding Methods (ISO 9797-1)">
            <div class="table-wrapper">
                <table>
                    <thead><tr><th>Method</th><th>Rule</th><th>Notes</th></tr></thead>
                    <tbody>
                        <tr><td><strong>Method 1</strong></td><td>Append <code>00</code> bytes to next block boundary.</td><td>Simple but ambiguous &mdash; cannot distinguish trailing zeros in plaintext.</td></tr>
                        <tr><td><strong>Method 2</strong></td><td>Append a single <code>80</code> byte, then <code>00</code> bytes.</td><td>Self-describing and unambiguous. Recommended.</td></tr>
                        <tr><td><strong>Method 3</strong></td><td>Prefix message with its length, then pad with <code>00</code>.</td><td>Used in some legacy systems; rare in payments.</td></tr>
                    </tbody>
                </table>
            </div>
        </ui-section>

        <ui-section anchor="tips" heading="Tips">
            <ul>
                <li>Always confirm the IV the host expects &mdash; many hosts default to all zeros, but some use the previous transaction&rsquo;s MAC as a chaining vector.</li>
                <li>Match the padding method to your host spec. A wrong padding method produces a deterministic but wrong MAC, which is one of the most common debugging traps.</li>
                <li>For DUKPT-derived MAC keys, generate the session key in the DUKPT Tools first, then plug the result into the corresponding MAC calculator.</li>
            </ul>
        </ui-section>
    </main>`,
})
export class DocsMacToolsPage {}
