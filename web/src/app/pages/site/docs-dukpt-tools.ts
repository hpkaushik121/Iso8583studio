import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { SitePage } from './site-page';
import { UiSection } from '../../ui/section';

@Component({
  selector: 'page-docs-dukpt-tools',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiSection],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-dukpt-tools' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<main class="doc-body">
        <div class="breadcrumb">
            <a href="/">Home</a>
            <span class="breadcrumb-sep">/</span>
            <a href="/docs">Documentation</a>
            <span class="breadcrumb-sep">/</span>
            <span>DUKPT Tools</span>
        </div>

        <h1 class="page-title">DUKPT Tools</h1>
        <p class="page-description">Derived Unique Key Per Transaction (DUKPT) is the standard mechanism for protecting card data and PINs at the point of sale. ISO8583Studio includes calculators for both DUKPT AES (ANSI X9.24-3) and DUKPT ISO 9797 / 3DES (ANSI X9.24-1).</p>

        <ui-section anchor="overview" heading="What is DUKPT?">
            <p>DUKPT generates a unique cryptographic key for every transaction without ever transmitting that key. The terminal stores a single Initial PIN Encryption Key (IPEK) derived from a Base Derivation Key (BDK) and a Key Serial Number (KSN). For each transaction the terminal advances the KSN counter and derives a fresh transaction key. The host, knowing only the BDK and the KSN it received, derives the same transaction key independently.</p>

            <div class="info-card note">
                <div class="info-card-title">Why use DUKPT?</div>
                <p>If a terminal is compromised, only future transaction keys can be derived (forward secrecy is built in). Past transactions remain protected because the terminal never stored the keys it used.</p>
            </div>
        </ui-section>

        <ui-section anchor="variants" heading="DUKPT Variants">
            <div class="table-wrapper">
                <table>
                    <thead><tr><th>Variant</th><th>Standard</th><th>Cipher</th><th>BDK Length</th><th>KSN Length</th></tr></thead>
                    <tbody>
                        <tr><td><strong>DUKPT ISO 9797</strong></td><td>ANSI X9.24-1</td><td>3DES</td><td>16 bytes (32 hex)</td><td>10 bytes (20 hex)</td></tr>
                        <tr><td><strong>DUKPT AES</strong></td><td>ANSI X9.24-3</td><td>AES-128 / 192 / 256</td><td>16 / 24 / 32 bytes</td><td>12 bytes (24 hex)</td></tr>
                    </tbody>
                </table>
            </div>

            <p>If you&rsquo;re working with legacy 3DES terminals, use the ISO 9797 variant. New deployments should use AES DUKPT.</p>
        </ui-section>

        <ui-section anchor="concepts" heading="Key Concepts">
            <ul>
                <li><strong>BDK (Base Derivation Key)</strong> &mdash; A master key shared between terminal manufacturer / acquirer and the host. Never used directly to encrypt data.</li>
                <li><strong>KSN (Key Serial Number)</strong> &mdash; A unique identifier for a terminal + transaction counter. Increments with every transaction.</li>
                <li><strong>IPEK (Initial PIN Encryption Key)</strong> &mdash; First key loaded into a terminal, derived from BDK + KSN.</li>
                <li><strong>Transaction Key</strong> &mdash; The key actually used for a single transaction; derived from the IPEK and the current KSN counter.</li>
                <li><strong>Working Keys</strong> &mdash; Purpose-bound keys (PIN, MAC, Data) derived by applying variant XOR masks to the transaction key.</li>
            </ul>
        </ui-section>

        <ui-section anchor="iso9797-overview" heading="DUKPT ISO 9797 (3DES) Tool">
            <p>The DUKPT ISO 9797 calculator implements ANSI X9.24-1 with 3DES. It is split across <strong>five tabs</strong> &mdash; two derivation tabs that produce working keys, and three operation tabs that consume those keys.</p>

            <h3>Tabs (5)</h3>
            <ul>
                <li><strong>PEK Derivation</strong> &mdash; Derive a PIN Encryption Key from BDK or IPEK + KSN.</li>
                <li><strong>DEK Derivation</strong> &mdash; Derive a Data Encryption Key from BDK or IPEK + KSN.</li>
                <li><strong>DUKPT PIN</strong> &mdash; Encrypt or decrypt a PIN block with a previously derived PEK.</li>
                <li><strong>DUKPT MAC</strong> &mdash; Generate a MAC over hex data with a previously derived PEK / MAC key.</li>
                <li><strong>DUKPT Data</strong> &mdash; Encrypt or decrypt arbitrary data with a previously derived key.</li>
            </ul>

            <div class="info-card note">
                <div class="info-card-title">Two-step workflow</div>
                <p>The tool deliberately separates derivation from use: derive the working key in <em>PEK Derivation</em> or <em>DEK Derivation</em> first, then paste the result into the <em>DUKPT PIN</em> / <em>MAC</em> / <em>Data</em> tab. This mirrors how a host-side stack stages keys.</p>
            </div>
        </ui-section>

        <ui-section anchor="iso9797-pek" heading="PEK Derivation Tab">

            <div class="shot-grid">
                <figure class="shot-fig" data-shot="iso9797-pek" data-alt="DUKPT Utilities on the PEK Derivation tab with BDK selected as the input key designation and empty BDK and KSN fields, beside the activity log" style="--shot-w:1512px">
                    <image-slot><img src="/images/docs/payment-utilities/dukpt-pek-derivation.png"
                                     alt="DUKPT Utilities on the PEK Derivation tab with BDK selected as the input key designation and empty BDK and KSN fields, beside the activity log"
                                     width="3024" height="1850" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">dukpt-tools</span> PEK Derivation Tab</figcaption>
                </figure>
            </div>

            <h3>Inputs</h3>
            <ul>
                <li><strong>Input Key Designation</strong> &mdash; Radio / toggle: <code>BDK</code> or <code>IPEK</code>. Determines which key field is shown.</li>
                <li><strong>BDK (32 Hex Chars)</strong> &mdash; Visible when input is <code>BDK</code>.</li>
                <li><strong>IPEK (32 Hex Chars)</strong> &mdash; Visible when input is <code>IPEK</code>.</li>
                <li><strong>KSN (20 Hex Chars)</strong> &mdash; Always visible.</li>
            </ul>

            <p>Button: <strong>Derive PEK</strong>.</p>

            <h3>Walk-through</h3>
            <ol class="steps">
                <li><strong>Pick Input Key Designation</strong> &mdash; <code>BDK</code> if you have the base key; <code>IPEK</code> if a previous step already produced the initial key.</li>
                <li><strong>Enter the key</strong> in the resulting <em>BDK</em> or <em>IPEK</em> field (32 hex chars).</li>
                <li><strong>Enter KSN</strong> &mdash; 20 hex chars (rightmost 21 bits are the transaction counter).</li>
                <li><strong>Click <code>Derive PEK</code></strong> &mdash; The activity log shows the resulting PIN Encryption Key. Copy it for the operation tabs.</li>
            </ol>
        </ui-section>

        <ui-section anchor="iso9797-dek" heading="DEK Derivation Tab">
            <p>Identical fields and flow to <em>PEK Derivation</em> &mdash; the only difference is the variant applied to produce a Data Encryption Key.</p>

            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="iso9797-dek" data-alt="DEK Derivation form with BDK selected as the input key designation, a 32-hex BDK field and a 20-hex KSN field above the Derive DEK button" style="--shot-w:754px">
                    <image-slot><img src="/images/docs/payment-utilities/dukpt-dek-derivation.png"
                                     alt="DEK Derivation form with BDK selected as the input key designation, a 32-hex BDK field and a 20-hex KSN field above the Derive DEK button"
                                     width="1508" height="840" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">dukpt-tools</span> DEK Derivation Tab</figcaption>
                </figure>
                <div class="shot-split-body">
                <h3>Inputs</h3>
                <ul>
                    <li><strong>Input Key Designation</strong> &mdash; <code>BDK</code> or <code>IPEK</code>.</li>
                    <li><strong>BDK (32 Hex Chars)</strong> / <strong>IPEK (32 Hex Chars)</strong>.</li>
                    <li><strong>KSN (20 Hex Chars)</strong>.</li>
                </ul>
                <p>Button: <strong>Derive DEK</strong>.</p>
                </div>
            </div>
        </ui-section>

        <ui-section anchor="iso9797-pin" heading="DUKPT PIN Tab">
            <p>Encrypt or decrypt a PIN block using a working key you derived in <em>PEK Derivation</em>.</p>

            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="iso9797-pin" data-alt="DUKPT PIN form with a 32-hex PEK field and a 16-hex PIN Block field above Encrypt and Decrypt buttons" style="--shot-w:747px">
                    <image-slot><img src="/images/docs/payment-utilities/dukpt-pin.png"
                                     alt="DUKPT PIN form with a 32-hex PEK field and a 16-hex PIN Block field above Encrypt and Decrypt buttons"
                                     width="1494" height="692" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">dukpt-tools</span> DUKPT PIN Tab</figcaption>
                </figure>
                <div class="shot-split-body">
                <h3>Inputs</h3>
                <ul>
                    <li><strong>PEK (32 Hex Chars)</strong> &mdash; Paste the PEK from the PEK Derivation tab.</li>
                    <li><strong>PIN Block (16 Hex Chars)</strong> &mdash; Clear (encrypt) or encrypted (decrypt) PIN block.</li>
                </ul>
                <p>Buttons: <strong>Encrypt</strong>, <strong>Decrypt</strong>.</p>
                </div>
            </div>
        </ui-section>

        <ui-section anchor="iso9797-mac" heading="DUKPT MAC Tab">
            <p>Compute a MAC over hex data with a previously derived working key.</p>

            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="iso9797-mac" data-alt="DUKPT MAC Generation form with a 32-hex PEK field, a DES / 3DES algorithm radio pair and a hex Data field above the Generate MAC button" style="--shot-w:747px">
                    <image-slot><img src="/images/docs/payment-utilities/dukpt-mac.png"
                                     alt="DUKPT MAC Generation form with a 32-hex PEK field, a DES / 3DES algorithm radio pair and a hex Data field above the Generate MAC button"
                                     width="1494" height="820" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">dukpt-tools</span> DUKPT MAC Tab</figcaption>
                </figure>
                <div class="shot-split-body">
                <h3>Inputs</h3>
                <ul>
                    <li><strong>PEK (32 Hex Chars)</strong> &mdash; The MAC key (typically derived in the PEK Derivation tab).</li>
                    <li><strong>Algorithm</strong> &mdash; Radio: <code>DES</code> or <code>3DES</code>.</li>
                    <li><strong>Data (Hex)</strong> &mdash; Multi-line hex input.</li>
                </ul>
                <p>Button: <strong>Generate MAC</strong>.</p>
                </div>
            </div>
        </ui-section>

        <ui-section anchor="iso9797-data" heading="DUKPT Data Tab">
            <p>Encrypt or decrypt sensitive data fields (track 2, EMV data) with a derived key.</p>

            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="iso9797-data" data-alt="DUKPT Data form with a 32-hex PEK field, a Use Data Variant Key switch, an ASCII data input type, a CBC / ECB cipher mode radio pair and a Data field above Encrypt and Decrypt buttons" style="--shot-w:753px">
                    <image-slot><img src="/images/docs/payment-utilities/dukpt-data.png"
                                     alt="DUKPT Data form with a 32-hex PEK field, a Use Data Variant Key switch, an ASCII data input type, a CBC / ECB cipher mode radio pair and a Data field above Encrypt and Decrypt buttons"
                                     width="1506" height="1128" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">dukpt-tools</span> DUKPT Data Tab</figcaption>
                </figure>
                <div class="shot-split-body">
                <h3>Inputs</h3>
                <ul>
                    <li><strong>PEK (32 Hex Chars)</strong> &mdash; Working key from PEK Derivation (or DEK Derivation when the data variant key is required).</li>
                    <li><strong>Use Data Variant Key</strong> &mdash; Switch. When on, the tool applies the data-encryption variant XOR before encrypting.</li>
                    <li><strong>Data Input Type</strong> &mdash; <code>ASCII</code> or <code>Hex</code>.</li>
                    <li><strong>Cipher Mode</strong> &mdash; Radio: <code>CBC</code> or <code>ECB</code>.</li>
                    <li><strong>Data</strong> &mdash; Multi-line input matching the chosen format.</li>
                </ul>
                <p>Buttons: <strong>Encrypt</strong>, <strong>Decrypt</strong>.</p>
                </div>
            </div>
        </ui-section>

        <ui-section anchor="aes-overview" heading="DUKPT AES Tool">
            <p>Implements ANSI X9.24-3 with AES. Four tabs split derivation from operations.</p>

            <h3>Tabs (4)</h3>
            <ul>
                <li><strong>Key Derivation</strong> &mdash; Derive a working key from BDK or IK + KSN.</li>
                <li><strong>DUKPT PIN</strong> &mdash; PIN block encrypt / decrypt.</li>
                <li><strong>DUKPT MAC</strong> &mdash; MAC generation.</li>
                <li><strong>DUKPT Data</strong> &mdash; Data encrypt / decrypt.</li>
            </ul>
        </ui-section>

        <ui-section anchor="aes-derive" heading="Key Derivation Tab">

            <div class="shot-grid">
                <figure class="shot-fig" data-shot="aes-derive" data-alt="DUKPT AES Utilities on the Key Derivation tab with BDK as the input key designation, AES-128 initial and working key types, and BDK / IK and KSN fields, beside the activity log" style="--shot-w:1510px">
                    <image-slot><img src="/images/docs/payment-utilities/dukpt-aes-key-derivation.png"
                                     alt="DUKPT AES Utilities on the Key Derivation tab with BDK as the input key designation, AES-128 initial and working key types, and BDK / IK and KSN fields, beside the activity log"
                                     width="3020" height="1842" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">dukpt-tools</span> Key Derivation Tab</figcaption>
                </figure>
            </div>

            <h3>Inputs</h3>
            <ul>
                <li><strong>Input Key Designation</strong> &mdash; Radio / toggle: <code>BDK</code> or <code>IK</code>.</li>
                <li><strong>Initial Key Type</strong> &mdash; Drop-down: <code>AES-128</code>, <code>AES-192</code>, <code>AES-256</code>.</li>
                <li><strong>BDK / IK</strong> &mdash; Hex; length matches the initial key type.</li>
                <li><strong>Working Key Type</strong> &mdash; Drop-down: <code>2TDEA</code>, <code>3TDEA</code>, <code>AES-128</code>, <code>AES-192</code>, <code>AES-256</code>.</li>
                <li><strong>KSN</strong> &mdash; 24 hex chars (12 bytes: 4-byte BDK ID + 4-byte derivation ID + 4-byte counter).</li>
            </ul>
            <p>Button: <strong>Derive Keys</strong>.</p>

            <h3>Walk-through</h3>
            <ol class="steps">
                <li><strong>Pick Input Key Designation</strong> &mdash; <code>BDK</code> or <code>IK</code>.</li>
                <li><strong>Pick Initial Key Type</strong> &mdash; AES-128 / 192 / 256.</li>
                <li><strong>Enter the BDK / IK</strong>.</li>
                <li><strong>Pick Working Key Type</strong> &mdash; The tool can derive both AES and TDES working keys for backwards compatibility.</li>
                <li><strong>Enter KSN</strong>.</li>
                <li><strong>Click <code>Derive Keys</code></strong> &mdash; The activity log lists the IK (when starting from BDK) and the working key.</li>
            </ol>

            <div class="info-card warning">
                <div class="info-card-title">Counter Field</div>
                <p>The 32-bit counter only uses values with at most 16 set bits to allow efficient forward derivation. The tool flags invalid counters.</p>
            </div>
        </ui-section>

        <ui-section anchor="ksn-format" heading="KSN Structure">

            <h3>3DES KSN (10 bytes)</h3>
            <pre><code>| 5-byte BDK ID + Device ID  |   2-byte counter (high)   | 21-bit Tx Counter |
|--------------------------- |---------------------------|-------------------|
|         59 bits            |          remaining         |     21 bits        |</code></pre>

            <h3>AES KSN (12 bytes)</h3>
            <pre><code>| 4-byte BDK ID | 4-byte Derivation ID | 4-byte Transaction Counter |</code></pre>

            <p>Increment the counter by one for every transaction. After exhausting the counter space, the device must be re-keyed.</p>
        </ui-section>

        <ui-section anchor="tips" heading="Tips &amp; Pitfalls">
            <ul>
                <li>The KSN you receive in field 53 / 60 of an ISO 8583 message is what the host uses to derive the same key. Keep them in sync &mdash; off-by-one is the most common bug.</li>
                <li>For PIN translation tests, capture the PIN block at the same instant as the KSN. Re-using a KSN with a different PIN block will fail.</li>
                <li>If you suddenly start getting wrong MACs, check whether your terminal advanced the counter without you advancing yours. Use the increment button to re-sync.</li>
                <li>Keep BDKs out of source control. The activity log persists keys in memory during the session but never writes them to disk.</li>
            </ul>
        </ui-section>
        <section class="cta">
            <h2>Try it on your own transactions</h2>
            <p>Free and open source. Download the studio and run this simulator on your desk in minutes.</p>
            <div class="row"><a class="btn btn--primary btn-lg" href="/download">⬇ Download Studio</a>
            <a class="btn btn-ghost btn-lg" href="/docs/installation">Installation guide</a></div>
        </section>
    </main>`,
})
export class DocsDukptToolsPage {}
