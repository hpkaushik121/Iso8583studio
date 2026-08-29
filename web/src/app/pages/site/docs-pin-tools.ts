import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { SitePage } from './site-page';
import { UiSection } from '../../ui/section';

@Component({
  selector: 'page-docs-pin-tools',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiSection],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-pin-tools' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<main class="doc-body">
        <div class="breadcrumb">
            <a href="/">Home</a>
            <span class="breadcrumb-sep">/</span>
            <a href="/docs">Documentation</a>
            <span class="breadcrumb-sep">/</span>
            <span>Payment Utilities</span>
        </div>

        <h1 class="page-title">Payment Utilities</h1>
        <p class="page-description">The PIN half of the Payment Utilities group &mdash; PIN block encoding across the ISO 9564 formats and OEM variants, AES-encrypted PIN blocks, TPK-to-ZPK translation and DUKPT PIN encryption. Each tool validates inputs in real time and logs every operation for audit.</p>

        <ui-section anchor="overview" heading="Introduction">
            <p>A PIN block is a fixed-format encoding of a cardholder PIN designed to be encrypted under a key (TPK, ZPK, or DUKPT-derived) and transmitted across a payment network. ISO 9564 defines the canonical formats; vendors and legacy networks add a few non-standard variants you may still encounter.</p>

            <p>The PIN calculators live in <code>Tools &rarr; Payment Utilities</code>, alongside the card-verification, DUKPT and MAC tools. Two of them are dedicated PIN tools &mdash; <strong>PIN Block Calculator</strong> and <strong>PIN Block (AES)</strong> &mdash; and two more tools in the group carry PIN block operations of their own, covered in <a href="/docs/pin-tools#translate">PIN Block Translation</a> and <a href="/docs/pin-tools#dukpt-pin">DUKPT PIN</a> below.</p>

            <div class="shot-grid">
                <figure class="shot-fig wide" data-shot="overview" data-alt="The Payment Utilities hub listing the calculators in the group, including PIN Block Calculator and PIN Block (AES) alongside the CVV, AMEX CSC, MasterCard CVC, DUKPT and MAC tools" style="--shot-w:1100px">
                    <image-slot><img src="/images/docs/payment-utilities/payment-utilities-hub.png"
                                     alt="The Payment Utilities hub listing the calculators in the group, including PIN Block Calculator and PIN Block (AES) alongside the CVV, AMEX CSC, MasterCard CVC, DUKPT and MAC tools"
                                     width="2200" height="1820" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">pin-tools</span> Payment Utilities hub</figcaption>
                </figure>
            </div>
        </ui-section>

        <ui-section anchor="all-tools" heading="All tools">
            <p>Every tool in this category &mdash; each card links to the detailed reference below.</p>
            <div class="hub-grid">
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">PIN Block Formats</div>
                        <p class="hub-desc">Reference of ISO 9564 formats 0–4 plus OEM variants — which are PAN-bound, how padding works, and when each is used.</p>
                        <a class="hub-link" href="/docs/pin-tools#formats">View details →</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">PIN Block Calculator</div>
                        <p class="hub-desc">The PIN Block Calculator is a multi-tab tool with one tab per supported format.</p>
                        <a class="hub-link" href="/docs/pin-tools#pin-block">View details →</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">Format Walk-throughs</div>
                        <p class="hub-desc">No PAN; the PIN is followed by random fill bytes.</p>
                        <a class="hub-link" href="/docs/pin-tools#format-walkthroughs">View details →</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">AES PIN Block (ISO-4) Calculator</div>
                        <p class="hub-desc">The PIN Block AES tab focuses on the modern ISO Format 4 design.</p>
                        <a class="hub-link" href="/docs/pin-tools#aes-pin">View details →</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">PIN Block Translation (AS2805)</div>
                        <p class="hub-desc">Re-encrypt a PIN block from a terminal key to a zone key — and change its format on the way — without exposing the clear PIN.</p>
                        <a class="hub-link" href="/docs/pin-tools#translate">View details →</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">DUKPT PIN</div>
                        <p class="hub-desc">Encrypt or decrypt a PIN block under a per-transaction PIN Entry Key derived from a BDK or IPEK.</p>
                        <a class="hub-link" href="/docs/pin-tools#dukpt-pin">View details →</a>
                    </div>
                </div>
            </div>
        </ui-section>

        <ui-section anchor="formats" heading="PIN Block Formats">
            <div class="table-wrapper">
                <table>
                    <thead><tr><th>Format</th><th>Source</th><th>PAN-bound?</th><th>Notes</th></tr></thead>
                    <tbody>
                        <tr><td><strong>ISO-0</strong></td><td>ISO 9564-1</td><td>Yes</td><td>PIN XOR PAN. Most common in legacy systems. Equivalent to ANSI X9.8.</td></tr>
                        <tr><td><strong>ISO-1</strong></td><td>ISO 9564-1</td><td>No</td><td>PIN + random padding. Used when PAN is not available.</td></tr>
                        <tr><td><strong>ISO-2</strong></td><td>ISO 9564-1</td><td>No</td><td>PIN + <code>F</code> padding. EMV ICC offline PIN.</td></tr>
                        <tr><td><strong>ISO-3</strong></td><td>ISO 9564-1</td><td>Yes</td><td>Like ISO-0 but with random fill nibbles instead of zeros.</td></tr>
                        <tr><td><strong>ISO-4</strong></td><td>ISO 9564-1 (2017)</td><td>Yes</td><td>16-byte block; AES-only. Currently mandated for new deployments.</td></tr>
                        <tr><td><strong>OEM-1</strong></td><td>Diebold / Docutel / NCR</td><td>Varies</td><td>Vendor-specific historical formats &mdash; rarely needed for new work.</td></tr>
                        <tr><td><strong>ECI 1-4</strong></td><td>Eurocheque / EFT</td><td>Varies</td><td>European legacy variants.</td></tr>
                    </tbody>
                </table>
            </div>

            <div class="info-card warning">
                <div class="info-card-title">Use ISO-4 for new work</div>
                <p>If you have flexibility, target ISO Format 4 with AES &mdash; it&rsquo;s the only format approved for new PCI-PIN evaluations.</p>
            </div>
        </ui-section>

        <ui-section anchor="pin-block" heading="PIN Block Calculator">
            <p>The PIN Block Calculator builds a formatted PIN block, or recovers the PIN from one. The format is picked once from the <strong>PIN block format</strong> drop-down at the top of the screen; <strong>Encode</strong> and <strong>Decode</strong> sit below it as a tab pair, so the same format applies in both directions.</p>

            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="pin-block" data-alt="PIN Block Calculator with Format 0 (ISO-0) selected in the PIN block format drop-down, the Encode tab active, and PAN and PIN fields above the Encode button" style="--shot-w:749px">
                    <image-slot><img src="/images/docs/payment-utilities/pin-block-encode.png"
                                     alt="PIN Block Calculator with Format 0 (ISO-0) selected in the PIN block format drop-down, the Encode tab active, and PAN and PIN fields above the Encode button"
                                     width="1498" height="1032" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">pin-tools</span> PIN Block Calculator</figcaption>
                </figure>
                <div class="shot-split-body">
                    <h3>Inputs</h3>
                    <ul>
                        <li><strong>PIN block format</strong> &mdash; Drop-down covering the ISO 9564 formats and the OEM / ECI variants. <code>Format 0 (ISO-0)</code> is the default.</li>
                        <li><strong>PAN</strong> &mdash; Only the PAN-bound formats ask for it; the field reads <em>PAN is required for this format</em> when the current selection needs one.</li>
                        <li><strong>PIN</strong> &mdash; 4&ndash;12 numeric digits.</li>
                    </ul>
                    <p>The rest of the form follows the format you pick, so a format that carries no PAN simply drops that field.</p>
                </div>
            </div>

            <h3>Walk-through (Encode, ISO-0)</h3>
            <ol class="steps">
                <li>Open the PIN Block Calculator from <code>Tools &rarr; Payment Utilities &rarr; PIN Block Calculator</code>.</li>
                <li>Pick <strong>Format 0 (ISO-0)</strong> in the <strong>PIN block format</strong> drop-down.</li>
                <li>Stay on the <strong>Encode</strong> tab.</li>
                <li>Enter the PAN. ISO-0 uses the rightmost 12 digits excluding the check digit.</li>
                <li>Enter the PIN (e.g. <code>1234</code>).</li>
                <li>Click <strong>Encode</strong>. The formatted PIN block is written to the activity log with its inputs.</li>
            </ol>

            <p>The <strong>Decode</strong> tab reverses the same format: give it the PIN block and it recovers the PIN.</p>

            <div class="info-card note">
                <div class="info-card-title">Formatting and encryption are separate steps</div>
                <p>This tool produces the <em>formatted</em> PIN block. To encrypt it under a working key, take the block into the <a href="/docs/cipher-tools">DES / 3DES or AES calculator</a>, or use <a href="/docs/pin-tools#dukpt-pin">DUKPT PIN</a> when the key comes from a DUKPT derivation.</p>
            </div>
        </ui-section>

        <ui-section anchor="format-walkthroughs" heading="Format Walk-throughs">

            <h3>ISO-0 / ANSI X9.8</h3>
            <pre><code>Step 1 (PIN block):  04 12 34 FF FF FF FF FF
   - 04 = PIN length (4 digits)
   - 1234 = PIN
   - FF padding to 8 bytes

Step 2 (PAN block):  00 00 12 34 56 78 90 12
   - leading zeros + rightmost 12 digits of PAN excluding check digit

Step 3 (XOR):        04 12 26 CB A9 87 6F ED
Step 4 (Encrypt):    encrypted under TPK / ZPK</code></pre>

            <h3>ISO-1</h3>
            <p>No PAN; the PIN is followed by random fill bytes. Use when PAN is not transmitted (e.g. some IVR / VRU flows).</p>

            <h3>ISO-3</h3>
            <p>Like ISO-0 but the padding nibbles are random in the range <code>0xA</code>&ndash;<code>0xF</code>. Each generated PIN block is unique even for the same PIN + PAN combination.</p>

            <h3>ISO-4 (AES)</h3>
            <p>16-byte clear PIN block: control field, PIN length, PIN digits, then a random fill. Encrypted with AES (128 / 192 / 256-bit) and XOR-combined with a derived PAN block. Use the dedicated <a href="/docs/pin-tools#aes-pin">AES PIN Block</a> tab for ISO-4 work.</p>

            <h3>OEM-1 / ECI</h3>
            <p>Reserved for compatibility with legacy ATM and POS networks. The exact layout differs per vendor; the calculator labels each tab with the vendor name.</p>
        </ui-section>

        <ui-section anchor="aes-pin" heading="AES PIN Block (ISO-4) Calculator">
            <p>The PIN Block (AES) tool focuses on the modern ISO Format 4 design. One form covers both directions &mdash; <strong>Encode</strong> and <strong>Decode</strong> are buttons rather than tabs, and the middle field changes meaning between them.</p>

            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="aes-pin" data-alt="AES PIN Block Operations form with a 32-hex Key field, a combined PIN (Encode) / PIN Block (Decode) field, a PAN field, and Encode and Decode buttons" style="--shot-w:748px">
                    <image-slot><img src="/images/docs/payment-utilities/aes-pin-block.png"
                                     alt="AES PIN Block Operations form with a 32-hex Key field, a combined PIN (Encode) / PIN Block (Decode) field, a PAN field, and Encode and Decode buttons"
                                     width="1496" height="760" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">pin-tools</span> AES PIN Block (ISO-4)</figcaption>
                </figure>
                <div class="shot-split-body">
                    <h3>Inputs</h3>
                    <ul>
                        <li><strong>Key</strong> &mdash; 32 hex chars (AES-128).</li>
                        <li><strong>PIN (Encode) / PIN Block (Decode)</strong> &mdash; One field serving both directions: the PIN going in, the encrypted block coming back out.</li>
                        <li><strong>PAN</strong> &mdash; Required for the PAN block XOR step.</li>
                    </ul>
                    <p>Buttons: <strong>Encode</strong>, <strong>Decode</strong>.</p>
                </div>
            </div>

            <h3>Output</h3>
            <ul>
                <li>Clear PIN block (16 bytes)</li>
                <li>PAN block (16 bytes)</li>
                <li>Intermediate ciphertext (after AES of clear PIN block)</li>
                <li>Final encrypted PIN block (after XOR with PAN block)</li>
            </ul>

            <div class="info-card tip">
                <div class="info-card-title">DUKPT-AES</div>
                <p>For DUKPT AES PIN translation, derive the AES PIN working key in the DUKPT Tools first, then plug it in here.</p>
            </div>
        </ui-section>

        <ui-section anchor="translate" heading="PIN Block Translation (AS2805)">
            <p>Translation re-encrypts a PIN block from one key to another without ever exposing the clear PIN &mdash; the acquiring step that moves a PIN from the terminal key (TPK) it arrived under to the zone key (ZPK) shared with the next node. The <strong>AS2805 Calculator</strong> carries it on its <em>Translate PIN Block</em> tab, and can change the PIN block format in the same operation.</p>

            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="translate" data-alt="Translate PIN Block form with System ZPK and Terminal TPK hex fields, STAN, Transaction Amount, incoming and outgoing PIN block format selectors, the incoming PIN block and an account number, above a Translate button" style="--shot-w:746px">
                    <image-slot><img src="/images/docs/payment-utilities/as2805-translate-pin-block.png"
                                     alt="Translate PIN Block form with System ZPK and Terminal TPK hex fields, STAN, Transaction Amount, incoming and outgoing PIN block format selectors, the incoming PIN block and an account number, above a Translate button"
                                     width="1492" height="1608" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">pin-tools</span> Translate PIN Block</figcaption>
                </figure>
                <div class="shot-split-body">
                    <h3>Inputs</h3>
                    <ul>
                        <li><strong>System ZPK (Hex)</strong> &mdash; The zone key the block is translated <em>to</em>.</li>
                        <li><strong>Terminal TPK (Hex)</strong> &mdash; The terminal key the block arrived under.</li>
                        <li><strong>STAN</strong> &mdash; System trace audit number of the transaction.</li>
                        <li><strong>Transaction Amount</strong></li>
                        <li><strong>Incoming / Outgoing PIN Block Format</strong> &mdash; Two-digit format codes, defaulting to <code>01</code>. Set them differently to translate the format as well as the key.</li>
                        <li><strong>Incoming PIN Block (Hex)</strong></li>
                        <li><strong>Account Number</strong> &mdash; Binds the block for the PAN-bound formats.</li>
                    </ul>
                    <p>Button: <strong>Translate</strong>.</p>
                </div>
            </div>

            <div class="info-card note">
                <div class="info-card-title">Where it lives</div>
                <p>Open <code>Tools &rarr; Payment Utilities &rarr; AS2805 Calculator</code> and pick the <strong>Translate PIN Block</strong> tab. The same tool also generates terminal key sets and computes AS2805 MACs and one-way functions.</p>
            </div>
        </ui-section>

        <ui-section anchor="dukpt-pin" heading="DUKPT PIN">
            <p>Under DUKPT, the PIN is encrypted with a PIN Entry Key (PEK) derived per transaction rather than a static TPK. The <strong>DUKPT PIN</strong> tab takes a PEK you have already derived and encrypts or decrypts a PIN block with it.</p>

            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="dukpt-pin" data-alt="DUKPT PIN form with a 32-hex PEK field and a 16-hex PIN Block field above Encrypt and Decrypt buttons" style="--shot-w:747px">
                    <image-slot><img src="/images/docs/payment-utilities/dukpt-pin.png"
                                     alt="DUKPT PIN form with a 32-hex PEK field and a 16-hex PIN Block field above Encrypt and Decrypt buttons"
                                     width="1494" height="692" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">pin-tools</span> DUKPT PIN</figcaption>
                </figure>
                <div class="shot-split-body">
                    <h3>Inputs</h3>
                    <ul>
                        <li><strong>PEK</strong> &mdash; 32 hex chars. Derive it first on the <em>PEK Derivation</em> tab from a BDK or IPEK plus the KSN.</li>
                        <li><strong>PIN Block</strong> &mdash; 16 hex chars: the clear block to encrypt, or the encrypted block to recover.</li>
                    </ul>
                    <p>Buttons: <strong>Encrypt</strong>, <strong>Decrypt</strong>.</p>
                </div>
            </div>

            <div class="info-card tip">
                <div class="info-card-title">Two-step flow</div>
                <p>Build the block in the <a href="/docs/pin-tools#pin-block">PIN Block Calculator</a>, then encrypt it here under the derived PEK. Key derivation itself &mdash; PEK, DEK, and the AES variants &mdash; is covered in <a href="/docs/dukpt-tools">DUKPT Tools</a>.</p>
            </div>
        </ui-section>

        <ui-section anchor="tips" heading="Tips">
            <ul>
                <li>If the host rejects your PIN block, verify the format on both sides &mdash; ISO-0 and ISO-3 look identical at a glance but produce different blocks.</li>
                <li>When testing DUKPT-protected PIN flows, derive the working key first (<a href="/docs/dukpt-tools">DUKPT Tools</a>), then encrypt or decrypt with it under <a href="/docs/pin-tools#dukpt-pin">DUKPT PIN</a>. The activity log shows both the input PIN block and the decrypted clear PIN for cross-checking.</li>
            </ul>
        </ui-section>
    </main>

<aside class="pro-nudge"><span class="pn-tag">✦ Pro</span><p>Testing with a team, or certifying with a scheme? Pro raises the CPS ceiling, unlocks the full algorithm set and deep simulator tweaks, plus hosted endpoints and priority support.</p><a href="/pro">Register for Pro →</a></aside>`,
})
export class DocsPinToolsPage {}
