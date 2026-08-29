import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { SitePage } from './site-page';

@Component({
  selector: 'page-docs-cipher-tools',
  changeDetection: ChangeDetectionStrategy.OnPush,
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-cipher-tools' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<main class="doc-body">
        <div class="breadcrumb">
            <a href="/">Home</a>
            <span class="breadcrumb-sep">/</span>
            <a href="/docs">Documentation</a>
            <span class="breadcrumb-sep">/</span>
            <span>Cryptographic Tools</span>
        </div>

        <h1 class="page-title">Cryptographic Tools</h1>
        <p class="page-description">Symmetric and asymmetric encryption calculators for testing payment cryptography. AES, DES/3DES, RSA, ECDSA, and Format-Preserving Encryption (FPE) variants are all supported with hex inputs and detailed audit logs.</p>

        <section class="doc-section" id="overview">
            <h2>Introduction</h2>
            <p>The <strong>Cryptographic Tools</strong> hub &mdash; encryption, decryption and security utilities &mdash; gathers them in one place. Each calculator is dedicated to a single algorithm family and exposes the inputs typically required for payment-system testing: hex keys, hex data, IV/tweak values, and mode/padding selectors. Every operation is recorded in the activity log with timestamps and round-trip values.</p>

            <div class="shot-grid">
                <figure class="shot-fig wide" data-shot="overview" data-alt="The Cryptographic Tools hub showing seven cards — AES, DES, RSA, ECDSA, Hash, Thales RSA and FPE calculators — each with a short description" style="--shot-w:1072px">
                    <image-slot><img src="/images/docs/cipher-tools/overview.png"
                                     alt="The Cryptographic Tools hub showing seven cards — AES, DES, RSA, ECDSA, Hash, Thales RSA and FPE calculators — each with a short description"
                                     width="2144" height="928" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">cipher-tools</span> Introduction</figcaption>
                </figure>
            </div>

            <div class="feature-grid">
                <div class="feature-item">
                    <div class="feature-item-icon">🔒</div>
                    <h4>AES</h4>
                    <p>128 / 192 / 256-bit AES with selectable cipher modes for both encryption and decryption.</p>
                </div>
                <div class="feature-item">
                    <div class="feature-item-icon">🛡️</div>
                    <h4>DES / 3DES</h4>
                    <p>Single, double, and triple DES with ECB / CBC modes and automatic padding.</p>
                </div>
                <div class="feature-item">
                    <div class="feature-item-icon">🔑</div>
                    <h4>RSA</h4>
                    <p>RSA encryption, decryption, signing, and verification with custom modulus and exponents.</p>
                </div>
                <div class="feature-item">
                    <div class="feature-item-icon">📐</div>
                    <h4>ECDSA</h4>
                    <p>Elliptic Curve Digital Signature Algorithm for key generation, signing, and verification.</p>
                </div>
                <div class="feature-item">
                    <div class="feature-item-icon">🔢</div>
                    <h4>FPE</h4>
                    <p>Format-Preserving Encryption (FF1, FF2 / VAES3, FF3, FF3-1, DFF) for tokenization use cases.</p>
                </div>
                <div class="feature-item">
                    <div class="feature-item-icon">🏛️</div>
                    <h4>Thales RSA</h4>
                    <p>Vendor-aware RSA helpers for Thales key blocks and LMK variant operations.</p>
                </div>
            </div>
        </section>

        <section class="doc-section" id="all-tools">
            <h2>All tools</h2>
            <p>Every tool in this category &mdash; each card links to the detailed reference below.</p>
            <div class="hub-grid">
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">AES Calculator</div>
                        <p class="hub-desc">Encrypt or decrypt data with AES, or compute a Key Check Value over a known key.</p>
                        <a class="hub-link" href="/docs/cipher-tools#aes">View details →</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">DES / 3DES Calculator</div>
                        <p class="hub-desc">Single DES or Triple DES with a wide selection of cipher modes and padding schemes commonly used by legacy payment hosts.</p>
                        <a class="hub-link" href="/docs/cipher-tools#des">View details →</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">FPE Calculator</div>
                        <p class="hub-desc">Format-Preserving Encryption keeps the output in the same format as the input — e.g.</p>
                        <a class="hub-link" href="/docs/cipher-tools#fpe">View details →</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">RSA Calculator</div>
                        <p class="hub-desc">Six-tab tool covering the full RSA workflow from key generation through padding-aware encryption and signing.</p>
                        <a class="hub-link" href="/docs/cipher-tools#rsa">View details →</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">Hash Calculator</div>
                        <p class="hub-desc">Generate an MD5, SHA-1 or SHA-256 digest over ASCII or hex input.</p>
                        <a class="hub-link" href="/docs/cipher-tools#hash">View details &rarr;</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">Thales RSA Calculator</div>
                        <p class="hub-desc">Vendor-aware RSA helpers tailored to Thales PayShield workflows.</p>
                        <a class="hub-link" href="/docs/cipher-tools#thales-rsa">View details →</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">ECDSA Calculator</div>
                        <p class="hub-desc">Elliptic Curve Digital Signature Algorithm with three workflow tabs.</p>
                        <a class="hub-link" href="/docs/cipher-tools#ecdsa">View details →</a>
                    </div>
                </div>
            </div>
        </section>

        <section class="doc-section" id="common">
            <h2>Common UI Patterns</h2>
            <p>All cipher calculators share the same conventions:</p>
            <ul>
                <li><strong>Two-pane layout</strong> &mdash; Inputs on the left, an activity log on the right that records every operation with timestamps and inputs.</li>
                <li><strong>Data Input Type</strong> &mdash; Most tools have an <em>ASCII</em> / <em>Hexadecimal</em> drop-down so you can paste data either way; the field label changes based on the selection.</li>
                <li><strong>Hex keys</strong> &mdash; Keys are always entered as continuous hexadecimal (no spaces, no <code>0x</code> prefix). Length is validated live.</li>
                <li><strong>Encrypt / Decrypt buttons</strong> &mdash; Pair of explicit buttons rather than an Encrypt/Decrypt toggle.</li>
                <li><strong>Copy buttons</strong> &mdash; Each output supports one-click copy to clipboard.</li>
            </ul>

            <div class="info-card tip">
                <div class="info-card-title">Tip</div>
                <p>Use the activity log to compare consecutive runs side-by-side. The log persists until you clear it or close the tool.</p>
            </div>
        </section>

        <section class="doc-section" id="aes">
            <h2>AES Calculator</h2>
            <p>Encrypt or decrypt data with AES, or compute a Key Check Value over a known key.</p>

            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="aes" data-alt="AES Calculator with AES-128 and ECB mode selected, ASCII data input, and empty Input Data and hex Key fields above the Encrypt and Decrypt buttons" style="--shot-w:743px">
                    <image-slot><img src="/images/docs/cipher-tools/aes.png"
                                     alt="AES Calculator with AES-128 and ECB mode selected, ASCII data input, and empty Input Data and hex Key fields above the Encrypt and Decrypt buttons"
                                     width="1486" height="948" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">cipher-tools</span> AES Calculator</figcaption>
                </figure>
                <div class="shot-split-body">
                    <h3>Inputs</h3>
                    <div class="table-wrapper">
                        <table>
                            <thead><tr><th>Field</th><th>Description</th></tr></thead>
                            <tbody>
                                <tr><td><strong>AES Type</strong></td><td>Drop-down: <code>AES-128</code>, <code>AES-192</code>, <code>AES-256</code>.</td></tr>
                                <tr><td><strong>Mode</strong></td><td>Drop-down: <code>ECB</code>, <code>CBC</code>, <code>CFB</code>, <code>OFB</code>, <code>KCV</code>.</td></tr>
                                <tr><td><strong>Data Input Type</strong></td><td>Drop-down: <code>ASCII</code> or <code>Hexadecimal</code>. Changes the input field label.</td></tr>
                                <tr><td><strong>Input Data</strong></td><td>Multi-line text. The field accepts ASCII or hex per the input type.</td></tr>
                                <tr><td><strong>Key (Hex)</strong></td><td>32 / 48 / 64 hex chars matching the chosen AES type.</td></tr>
                                <tr><td><strong>Initial Vector (IV) (Hex)</strong></td><td>Only shown for <code>CBC</code>, <code>CFB</code>, <code>OFB</code>. 32 hex chars.</td></tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            <h3>Walk-through</h3>
            <ol class="steps">
                <li><strong>Pick AES Type</strong> &mdash; <code>AES-128</code>, <code>AES-192</code>, or <code>AES-256</code>.</li>
                <li><strong>Pick Mode</strong> &mdash; Choose a cipher mode, or pick <code>KCV</code> to compute a key check value.</li>
                <li><strong>Pick Data Input Type</strong> &mdash; <code>ASCII</code> or <code>Hexadecimal</code>.</li>
                <li><strong>Enter Input Data</strong> &mdash; In KCV mode the input is ignored; otherwise this is the plaintext or ciphertext.</li>
                <li><strong>Enter Key (Hex)</strong> &mdash; A hex key whose length matches the AES type.</li>
                <li><strong>Enter IV</strong> &mdash; If the mode requires one, enter 32 hex chars in the <em>Initial Vector (IV) (Hex)</em> field.</li>
                <li><strong>Click <code>Encrypt</code> / <code>Decrypt</code></strong> &mdash; Or <code>Calculate KCV</code> when the mode is <code>KCV</code>. The result is appended to the right-hand activity log with a byte count.</li>
            </ol>

            <div class="info-card note">
                <div class="info-card-title">KCV Mode</div>
                <p>Selecting <strong>KCV</strong> swaps the Encrypt/Decrypt buttons for a single <strong>Calculate KCV</strong> button and hides the IV field. The output is the standard 3-byte (6-hex) check value computed over zero plaintext.</p>
            </div>

            <h3>Example</h3>
            <pre><code>AES Type:        AES-128
Mode:            CBC
Data Input Type: Hexadecimal
Key (Hex):       000102030405060708090A0B0C0D0E0F
IV (Hex):        00000000000000000000000000000000
Input Data:      6BC1BEE22E409F96E93D7E117393172A

Output:          7649ABAC8119B246CEE98E9B12E9197D</code></pre>
        </section>

        <section class="doc-section" id="des">
            <h2>DES / 3DES Calculator</h2>
            <p>Single DES or Triple DES with a wide selection of cipher modes and padding schemes commonly used by legacy payment hosts.</p>

            <div class="shot-grid">
                <figure class="shot-fig" data-shot="des" data-alt="DES/3DES Calculator set to DES with ECB mode, PKCS#5 padding and ASCII input, showing the Input Data and hex Key fields" style="--shot-w:749px">
                    <image-slot><img src="/images/docs/cipher-tools/des.png"
                                     alt="DES/3DES Calculator set to DES with ECB mode, PKCS#5 padding and ASCII input, showing the Input Data and hex Key fields"
                                     width="1498" height="952" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">cipher-tools</span> DES / 3DES Calculator</figcaption>
                </figure>
            </div>

            <h3>Inputs</h3>
            <div class="table-wrapper">
                <table>
                    <thead><tr><th>Field</th><th>Description</th></tr></thead>
                    <tbody>
                        <tr><td><strong>Algorithm</strong></td><td>Drop-down: <code>DES</code> or <code>3DES</code>.</td></tr>
                        <tr><td><strong>Mode</strong></td><td>Drop-down: <code>ECB</code>, <code>CBC</code>, <code>CFB-8</code>, <code>CFB-64</code>, <code>OFB-8</code>, <code>OFB-64</code>.</td></tr>
                        <tr><td><strong>Padding</strong></td><td>Drop-down: <code>None</code>, <code>Zeros</code>, <code>Spaces</code>, <code>ANSI X9.23</code>, <code>ISO 10126</code>, <code>PKCS#5</code>, <code>PKCS#7</code>, <code>ISO 7816-4</code>, <code>Rijndael</code>, <code>ISO 9797-1 Method 1</code>, <code>ISO 9797-1 Method 2</code>.</td></tr>
                        <tr><td><strong>Data Input Type</strong></td><td>Drop-down: <code>ASCII</code> or <code>Hexadecimal</code>. The input data field label updates accordingly (<em>Input Data (ASCII)</em> / <em>Input Data (Hex)</em>).</td></tr>
                        <tr><td><strong>Input Data</strong></td><td>Multi-line text.</td></tr>
                        <tr><td><strong>Key (Hex)</strong></td><td>16 hex (DES), 32 hex (2-key 3DES) or 48 hex (3-key 3DES).</td></tr>
                        <tr><td><strong>Initialization Vector (IV)</strong></td><td>16 hex chars (8 bytes). Shown for non-ECB modes; a KCV chip displayed alongside.</td></tr>
                    </tbody>
                </table>
            </div>

            <h3>Walk-through</h3>
            <ol class="steps">
                <li><strong>Pick Algorithm</strong> &mdash; <code>DES</code> for single DES, <code>3DES</code> for double or triple length keys.</li>
                <li><strong>Pick Mode</strong> &mdash; <code>ECB</code> or <code>CBC</code> for the typical case; <code>CFB-8</code>/<code>CFB-64</code>/<code>OFB-8</code>/<code>OFB-64</code> for streaming variants.</li>
                <li><strong>Pick Padding</strong> &mdash; Pick the scheme expected by your host. <code>ISO 9797-1 Method 1</code> or <code>Method 2</code> are common in payments.</li>
                <li><strong>Pick Data Input Type</strong> &mdash; <code>ASCII</code> or <code>Hexadecimal</code>.</li>
                <li><strong>Enter Input Data</strong> in the matching format.</li>
                <li><strong>Enter Key (Hex)</strong> &mdash; The variant (DES, 2-key 3DES, 3-key 3DES) is inferred from the key length you provide.</li>
                <li><strong>Enter IV</strong> if a chaining mode is selected.</li>
                <li><strong>Click <code>Encrypt</code> / <code>Decrypt</code></strong> &mdash; The result card displays the output in HEX, plus an ASCII view if the bytes are printable, with copy / clear actions.</li>
            </ol>

            <h3>Use Cases</h3>
            <ul>
                <li>Encrypting / decrypting PIN blocks under a working key (TPK / PEK).</li>
                <li>Generating 3DES MACs by chaining ECB operations.</li>
                <li>Verifying ZPK / ZMK translations during HSM integration.</li>
            </ul>
        </section>

        <section class="doc-section" id="fpe">
            <h2>FPE Calculator</h2>
            <p>Format-Preserving Encryption keeps the output in the same format as the input &mdash; e.g. encrypting a 16-digit PAN into another 16-digit numeric string. Useful for tokenisation and PCI-scope reduction.</p>

            <div class="shot-grid shot-row shot-even" style="--row-w:936px">
                <figure class="shot-fig" data-shot="fpe-ff1" data-alt="FPE-FF1 tab with radix 10, AES-128 encryption, hexadecimal key input, Key and Data fields, a checked Use Tweak option and a Tweak field" style="--shot-w:459px;--ar:1.073">
                    <image-slot><img src="/images/docs/cipher-tools/fpe-ff1.png"
                                     alt="FPE-FF1 tab with radix 10, AES-128 encryption, hexadecimal key input, Key and Data fields, a checked Use Tweak option and a Tweak field"
                                     width="1494" height="1392" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">cipher-tools</span> FPE-FF1</figcaption>
                </figure>
                <figure class="shot-fig" data-shot="fpe-ff2" data-alt="FPE-FF2 (VAES3) tab with separate Radix and Tweak Radix selectors, AES-128 encryption, hexadecimal key input, and Key, Data and Tweak fields" style="--shot-w:459px;--ar:1.191">
                    <image-slot><img src="/images/docs/cipher-tools/fpe-ff2.png"
                                     alt="FPE-FF2 (VAES3) tab with separate Radix and Tweak Radix selectors, AES-128 encryption, hexadecimal key input, and Key, Data and Tweak fields"
                                     width="1494" height="1254" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">cipher-tools</span> FPE-FF2 (VAES3)</figcaption>
                </figure>
            </div>

            <h3>Tabs (5)</h3>
            <p>Each tab is a self-contained calculator for the named variant:</p>
            <ul>
                <li><strong>FPE-FF1</strong></li>
                <li><strong>FPE-FF2 (VAES3)</strong></li>
                <li><strong>FPE-FF3</strong></li>
                <li><strong>FPE-FF3-1</strong></li>
                <li><strong>FPE-DFF[OFF-2]</strong></li>
            </ul>

            <h3>Inputs (per tab)</h3>
            <div class="table-wrapper">
                <table>
                    <thead><tr><th>Field</th><th>Description</th></tr></thead>
                    <tbody>
                        <tr><td><strong>Radix</strong></td><td>Drop-down: <code>10</code> (digits), <code>26</code> (lower-case alpha), <code>36</code> (alphanumeric).</td></tr>
                        <tr><td><strong>Tweak Radix</strong></td><td>Drop-down. Only shown for <code>FPE-FF2</code> and <code>FPE-DFF</code>.</td></tr>
                        <tr><td><strong>Encryption Type</strong></td><td>Drop-down: <code>AES-128</code>, <code>AES-192</code>, <code>AES-256</code>.</td></tr>
                        <tr><td><strong>Key Input Type</strong></td><td>Drop-down: <code>ASCII</code> or <code>Hexadecimal</code>.</td></tr>
                        <tr><td><strong>Key</strong></td><td>Length matches the encryption type and input format.</td></tr>
                        <tr><td><strong>Use Tweak?</strong></td><td>Checkbox &mdash; only on <code>FPE-FF1</code>. When on, an animated <em>Tweak</em> field appears.</td></tr>
                        <tr><td><strong>Tweak</strong></td><td>Visible when applicable; format depends on the variant.</td></tr>
                        <tr><td><strong>Data</strong></td><td>Multi-line input matching the chosen radix alphabet.</td></tr>
                    </tbody>
                </table>
            </div>

            <h3>Walk-through</h3>
            <ol class="steps">
                <li><strong>Pick the FPE tab</strong> matching your spec &mdash; <code>FPE-FF1</code> or <code>FPE-FF3-1</code> for new work.</li>
                <li><strong>Pick Radix</strong> &mdash; <code>10</code> for numeric tokens such as PANs.</li>
                <li><strong>Pick Encryption Type</strong> &mdash; the AES variant for the underlying block cipher.</li>
                <li><strong>Pick Key Input Type</strong> and enter the <strong>Key</strong>.</li>
                <li>(<strong>FF1 only</strong>) tick <strong>Use Tweak?</strong> if you have one, then enter it.</li>
                <li><strong>Enter Data</strong> &mdash; The string must contain only characters in the radix alphabet.</li>
                <li><strong>Click <code>Encrypt</code> or <code>Decrypt</code></strong> &mdash; The output preserves the original length and alphabet.</li>
            </ol>

            <h3>Example: Tokenize a PAN (FPE-FF1)</h3>
            <pre><code>Radix:           10
Encryption Type: AES-128
Key Input Type:  Hexadecimal
Key:             2B7E151628AED2A6ABF7158809CF4F3C
Use Tweak?:      on
Tweak:           39383736353433323130
Data:            0123456789012345

Output:          6124200211725605</code></pre>
        </section>

        <section class="doc-section" id="rsa">
            <h2>RSA Calculator</h2>
            <p>Six-tab tool covering the full RSA workflow from key generation through padding-aware encryption and signing.</p>

            <div class="shot-grid">
                <figure class="shot-fig" data-shot="rsa" data-alt="RSA Calculator on the Keys tab with a 2048-bit key length and a Generate Keys button above empty Modulus, Public Exponent and Private Exponent fields, beside the activity log" style="--shot-w:1512px">
                    <image-slot><img src="/images/docs/cipher-tools/rsa.png"
                                     alt="RSA Calculator on the Keys tab with a 2048-bit key length and a Generate Keys button above empty Modulus, Public Exponent and Private Exponent fields, beside the activity log"
                                     width="3024" height="1844" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">cipher-tools</span> RSA Calculator</figcaption>
                </figure>
            </div>

            <h3>Tabs (6)</h3>
            <ul>
                <li><strong>Keys</strong> &mdash; Generate or paste a key pair.</li>
                <li><strong>Encrypt</strong> &mdash; Encrypt with PKCS1 or no padding.</li>
                <li><strong>Decrypt</strong> &mdash; Decrypt ciphertext with the corresponding key.</li>
                <li><strong>Sign</strong> &mdash; Produce a signature over a message or hash.</li>
                <li><strong>Verify</strong> &mdash; Verify a signature against a hash.</li>
                <li><strong>OAEP</strong> &mdash; Encode / decode with OAEP padding.</li>
            </ul>

            <h3>Keys Tab</h3>
            <ul>
                <li><strong>Key Length (bits)</strong> &mdash; <code>1024</code>, <code>2048</code>, <code>3072</code>, <code>4096</code>.</li>
                <li><strong>Generate Keys</strong> button.</li>
                <li><strong>Modulus</strong>, <strong>Public Exponent (e)</strong>, <strong>Private Exponent (d)</strong> &mdash; All hex; you can also paste pre-existing components instead of generating.</li>
            </ul>

            <h3>Encrypt Tab</h3>
            <ul>
                <li><strong>Encoding Method</strong> &mdash; <code>Public</code> (encrypt with public key) or <code>Private</code>.</li>
                <li><strong>Padding</strong> &mdash; <code>PKCS1</code> or <code>No Padding</code>.</li>
                <li><strong>Input Data Format</strong> &mdash; <code>ASCII</code> or <code>Hex</code>.</li>
                <li><strong>Data to Encrypt</strong>.</li>
                <li>Button: <strong>Encrypt</strong>.</li>
            </ul>

            <h3>Decrypt Tab</h3>
            <ul>
                <li><strong>Decoding Method</strong> &mdash; <code>Private</code> or <code>Public</code>.</li>
                <li><strong>Padding</strong> &mdash; <code>PKCS1</code> or <code>No Padding</code>.</li>
                <li><strong>Data to Decrypt (Hex)</strong>.</li>
                <li>Button: <strong>Decrypt</strong>.</li>
            </ul>

            <h3>Sign / Verify Tabs</h3>
            <ul>
                <li><strong>Sign</strong> &mdash; <em>Input Data Format</em> (ASCII / Hex) and <em>Data to Sign</em>; click <strong>Sign</strong>.</li>
                <li><strong>Verify</strong> &mdash; <em>Hash (Hex)</em> and <em>Signature (Hex)</em>; click <strong>Verify</strong>.</li>
            </ul>

            <h3>OAEP Tab</h3>
            <ul>
                <li><strong>Method</strong> &mdash; <code>Encode</code> or <code>Decode</code>.</li>
                <li><strong>Hash Function</strong> &mdash; <code>SHA-1</code>, <code>SHA-224</code>, <code>SHA-256</code>, <code>SHA-384</code>, <code>SHA-512</code>.</li>
                <li><strong>Result Length (bits)</strong> &mdash; <code>1024</code>, <code>2048</code>, <code>4096</code>.</li>
                <li><strong>Data (Hex)</strong>, <strong>Encoding Parameters (Label, Hex)</strong>.</li>
                <li>Button label changes between <strong>Encode</strong> and <strong>Decode</strong> based on the selected method.</li>
            </ul>
        </section>

        <section class="doc-section" id="thales-rsa">
            <h2>Thales RSA Calculator</h2>
            <p>Vendor-aware RSA helpers tailored to Thales PayShield workflows.</p>

            <div class="shot-grid">
                <figure class="shot-fig" data-shot="thales-rsa" data-alt="Thales RSA Calculator on the Generate tab showing private exponent, prime, exponent and coefficient fields with a 2048-bit key length, beside the activity log" style="--shot-w:1501px">
                    <image-slot><img src="/images/docs/cipher-tools/thales-rsa.png"
                                     alt="Thales RSA Calculator on the Generate tab showing private exponent, prime, exponent and coefficient fields with a 2048-bit key length, beside the activity log"
                                     width="3002" height="1732" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">cipher-tools</span> Thales RSA Calculator</figcaption>
                </figure>
            </div>

            <h3>Tabs (3)</h3>

            <h4>Generate</h4>
            <p>Construct an RSA key pair from CRT components (or generate a fresh one). Inputs:</p>
            <ul>
                <li><strong>Private Exp. (d)</strong></li>
                <li><strong>Prime 1 (p)</strong></li>
                <li><strong>Prime 2 (q)</strong></li>
                <li><strong>Exponent 1 (dModP1)</strong></li>
                <li><strong>Exponent 2 (dModQ1)</strong></li>
                <li><strong>Coefficient (iqmp)</strong></li>
                <li><strong>Key Length (1-4096)</strong></li>
            </ul>
            <p>Buttons: <strong>Generate (d) from Components</strong>, <strong>Generate New Random Key</strong>.</p>

            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="thales-key-block" data-alt="Thales Key Block form with DES and AES KBPK fields, public and private key headers, AES key block encryption, hexadecimal input format, and Wrap and Unwrap Key Block buttons" style="--shot-w:560px">
                    <image-slot><img src="/images/docs/cipher-tools/thales-key-block.png"
                                     alt="Thales Key Block form with DES and AES KBPK fields, public and private key headers, AES key block encryption, hexadecimal input format, and Wrap and Unwrap Key Block buttons"
                                     width="1496" height="1552" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">cipher-tools</span> Thales Key Block</figcaption>
                </figure>
                <div class="shot-split-body">
                    <h4>Thales Key Block</h4>
                    <p>Wrap or unwrap a key pair under a Key Block Protection Key. Inputs:</p>
                    <ul>
                        <li><strong>DES KBPK</strong>, <strong>AES KBPK</strong></li>
                        <li><strong>Public Key Header</strong>, <strong>Private Key Header</strong></li>
                        <li><strong>Key Block Encryption</strong> &mdash; <code>AES</code> or <code>DES</code>.</li>
                        <li><strong>Input Format</strong> &mdash; <code>ASCII</code> or <code>Hex</code>.</li>
                        <li><strong>Public Key</strong>, <strong>Private Key</strong></li>
                    </ul>
                    <p>Buttons: <strong>Wrap Key Block</strong>, <strong>Unwrap Key Block</strong>.</p>
                </div>
            </div>

            <h4>Thales LMK Variant</h4>
            <p>Apply Thales LMK variants. Inputs:</p>
            <ul>
                <li><strong>LMK Pair 34-35</strong></li>
                <li><strong>LMK Pair 36-37</strong></li>
                <li><strong>Authentication Data</strong></li>
                <li><strong>Modulus Encoding</strong> &mdash; <code>DEC</code> / <code>DER</code> variants.</li>
                <li><strong>Public Key</strong>, <strong>Private Key</strong></li>
            </ul>
            <p>Button: <strong>Process LMK Variant</strong>.</p>
        </section>

        <section class="doc-section" id="ecdsa">
            <h2>ECDSA Calculator</h2>
            <p>Elliptic Curve Digital Signature Algorithm with three workflow tabs.</p>

            <div class="shot-grid">
                <figure class="shot-fig" data-shot="ecdsa" data-alt="ECDSA Key Management on the Keys tab with NIST P-256 selected, private and public key fields, uncompressed public key form, and buttons to generate and validate key pairs" style="--shot-w:1512px">
                    <image-slot><img src="/images/docs/cipher-tools/ecdsa.png"
                                     alt="ECDSA Key Management on the Keys tab with NIST P-256 selected, private and public key fields, uncompressed public key form, and buttons to generate and validate key pairs"
                                     width="3024" height="1844" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">cipher-tools</span> ECDSA Calculator</figcaption>
                </figure>
            </div>

            <h3>Keys Tab</h3>
            <ul>
                <li><strong>ECC Curve Name</strong> &mdash; <code>NIST P-256</code>, <code>NIST P-384</code>, <code>NIST P-521</code>, plus the Brainpool curve variants.</li>
                <li><strong>Private Key (Hex)</strong>.</li>
                <li><strong>Public Key (Hex)</strong>.</li>
                <li><strong>Public Key Form</strong> &mdash; <code>Uncompressed</code> or <code>Compressed</code>.</li>
            </ul>
            <p>Buttons: <strong>Generate New Public Key</strong>, <strong>Is Point on Curve?</strong>, <strong>Generate Random Key Pair</strong>, <strong>Validate Current Key Pair</strong>.</p>

            <h3>Sign Tab</h3>
            <ul>
                <li><strong>Hash Type</strong> &mdash; <code>SHA-1</code>, <code>SHA-256</code>, <code>SHA-384</code>, <code>SHA-512</code>.</li>
                <li><strong>Input Data Format</strong> &mdash; <code>ASCII</code> or <code>Hex</code>.</li>
                <li><strong>Data to Sign</strong>.</li>
            </ul>
            <p>Button: <strong>Sign Data</strong>. Output is the <code>(r, s)</code> pair in hex.</p>

            <h3>Verify Tab</h3>
            <ul>
                <li><strong>Hash (Hex)</strong>.</li>
                <li><strong>Signature (Hex)</strong>.</li>
            </ul>
            <p>Button: <strong>Verify Signature</strong>.</p>
        </section>

        <section class="doc-section" id="cipher-modes">
            <h2>Cipher Modes Reference</h2>
            <p>Mode availability depends on the calculator. AES exposes ECB / CBC / CFB / OFB / KCV. DES / 3DES exposes ECB, CBC, and the CFB-8 / CFB-64 / OFB-8 / OFB-64 byte/feedback variants.</p>
            <div class="table-wrapper">
                <table>
                    <thead><tr><th>Mode</th><th>IV?</th><th>Properties</th></tr></thead>
                    <tbody>
                        <tr><td><code>ECB</code></td><td>No</td><td>Each block independent. Simple but leaks plaintext patterns.</td></tr>
                        <tr><td><code>CBC</code></td><td>Yes</td><td>Each block XOR-chained with the previous ciphertext. Secure with unique IV.</td></tr>
                        <tr><td><code>CFB / CFB-8 / CFB-64</code></td><td>Yes</td><td>Self-synchronising stream mode. The numeric variants set the feedback width in bits.</td></tr>
                        <tr><td><code>OFB / OFB-8 / OFB-64</code></td><td>Yes</td><td>Key-stream mode independent of plaintext.</td></tr>
                        <tr><td><code>KCV</code></td><td>No</td><td>AES-only. Computes the standard 3-byte Key Check Value over zero plaintext.</td></tr>
                    </tbody>
                </table>
            </div>
        </section>

        <section class="doc-section" id="hash">
            <h2>Hash Calculator</h2>
            <p>Generate a message digest over arbitrary input. Useful for checking a known-answer test vector, or for producing the hash an RSA or ECDSA <em>Verify</em> step expects as its input.</p>

            <div class="shot-split" style="--fig-col:500px">
                <figure class="shot-fig" data-shot="hash" data-alt="Hash Calculator with ASCII data input, SHA-256 selected as the hash type, the text Hello, World! in the input field, and a Calculate Hash button" style="--shot-w:713px">
                    <image-slot><img src="/images/docs/cipher-tools/hash.png"
                                     alt="Hash Calculator with ASCII data input, SHA-256 selected as the hash type, the text Hello, World! in the input field, and a Calculate Hash button"
                                     width="1426" height="618" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">cipher-tools</span> Hash Calculator</figcaption>
                </figure>
                <div class="shot-split-body">
                    <h3>Inputs</h3>
                    <div class="table-wrapper">
                        <table>
                            <thead><tr><th>Field</th><th>Description</th></tr></thead>
                            <tbody>
                                <tr><td><code>Data Input Type</code></td><td>Drop-down: <code>ASCII</code> or <code>Hexadecimal</code>. Choose <code>Hexadecimal</code> when hashing raw bytes such as a key or a block of card data.</td></tr>
                                <tr><td><code>Hash Type</code></td><td>Drop-down: <code>MD5</code>, <code>SHA-1</code>, <code>SHA-256</code>.</td></tr>
                                <tr><td><code>Input Data</code></td><td>The message to digest, in the format chosen above.</td></tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            <h3>Walk-through</h3>
            <ol class="steps">
                <li><strong>Pick Data Input Type</strong> &mdash; ASCII for text, Hexadecimal for raw bytes.</li>
                <li><strong>Pick Hash Type</strong>.</li>
                <li><strong>Enter Input Data</strong>.</li>
                <li>Click <strong>Calculate Hash</strong>. The digest is written to the activity log alongside the input, so a run can be compared against a published vector.</li>
            </ol>

            <div class="info-card note">
                <div class="info-card-title">Note</div>
                <p>MD5 and SHA-1 are here because payment protocols and test packs still reference them. Neither is collision-resistant &mdash; use them to reproduce an existing vector, not to secure new data.</p>
            </div>
        </section>

        <section class="doc-section" id="padding">
            <h2>Padding Schemes (DES / 3DES)</h2>
            <p>The DES / 3DES calculator exposes the full set of padding schemes used across legacy and modern payment protocols. Pick the one your host expects:</p>
            <div class="table-wrapper">
                <table>
                    <thead><tr><th>Scheme</th><th>Notes</th></tr></thead>
                    <tbody>
                        <tr><td><code>None</code></td><td>Input must be an exact multiple of 8 bytes.</td></tr>
                        <tr><td><code>Zeros</code></td><td>Pads with <code>00</code> bytes. Cannot recover trailing zero bytes.</td></tr>
                        <tr><td><code>Spaces</code></td><td>Pads with ASCII space (<code>0x20</code>).</td></tr>
                        <tr><td><code>ANSI X9.23</code></td><td>Random bytes followed by a length byte.</td></tr>
                        <tr><td><code>ISO 10126</code></td><td>Random bytes followed by a length byte (similar to X9.23).</td></tr>
                        <tr><td><code>PKCS#5</code> / <code>PKCS#7</code></td><td>Self-describing: pad bytes equal the pad length.</td></tr>
                        <tr><td><code>ISO 7816-4</code></td><td>Single <code>0x80</code> followed by zero bytes.</td></tr>
                        <tr><td><code>Rijndael</code></td><td>Variant used in the original Rijndael spec.</td></tr>
                        <tr><td><code>ISO 9797-1 Method 1</code></td><td>Zero-pad to next block boundary (no length signal).</td></tr>
                        <tr><td><code>ISO 9797-1 Method 2</code></td><td>Single <code>0x80</code> then zeros &mdash; recommended for MAC inputs.</td></tr>
                    </tbody>
                </table>
            </div>
        </section>

        <section class="doc-section" id="tips">
            <h2>Tips</h2>
            <ul>
                <li>Use the <strong>Bitmap Calculator</strong> under <code>Tools &rarr; Payment Utilities</code> to inspect block alignment when sizes look odd.</li>
                <li>For HSM-bound keys, prefer <strong>Thales RSA &rarr; Thales Key Block</strong> over the raw RSA tool to keep wrapping consistent.</li>
                <li>If a known answer test fails, double-check the IV is in hex (not ASCII) and the data length matches the mode requirements.</li>
            </ul>
        </section>
    </main>

<aside class="pro-nudge"><span class="pn-tag">✦ Pro</span><p>Testing with a team, or certifying with a scheme? Pro raises the CPS ceiling, unlocks the full algorithm set and deep simulator tweaks, plus hosted endpoints and priority support.</p><a href="/pro">Register for Pro →</a></aside>`,
})
export class DocsCipherToolsPage {}
