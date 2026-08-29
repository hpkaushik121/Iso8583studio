import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { SitePage } from './site-page';
import { UiSection } from '../../ui/section';

@Component({
  selector: 'page-docs-key-tools',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiSection],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-key-tools' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<main class="doc-body">
        <div class="breadcrumb">
            <a href="/">Home</a>
            <span class="breadcrumb-sep">/</span>
            <a href="/docs">Documentation</a>
            <span class="breadcrumb-sep">/</span>
            <span>Key Management Tools</span>
        </div>

        <h1 class="page-title">Key Management Tools</h1>
        <p class="page-description">Generate, validate, wrap, share, and verify cryptographic keys used across payment systems &mdash; from raw 3DES key generation and parity enforcement to TR-31 / Thales key blocks, vendor HSM-specific calculators, keyshare splitting, and X.509 certificate workflows.</p>

        <ui-section anchor="overview" heading="Introduction">
            <p>Key Management Tools cluster around four jobs:</p>

            <div class="shot-grid">
                <figure class="shot-fig wide" data-shot="overview" data-alt="The Key Management hub with cards for DEA Keys, Keyshare Generator, Thales, Futurex, Atalla and SafeNet key calculators, Thales and TR-31 key blocks, SSL certificates and RSA DER keys" style="--shot-w:1079px">
                    <image-slot><img src="/images/docs/key-management-tools/key-management-hub.png"
                                     alt="The Key Management hub with cards for DEA Keys, Keyshare Generator, Thales, Futurex, Atalla and SafeNet key calculators, Thales and TR-31 key blocks, SSL certificates and RSA DER keys"
                                     width="2158" height="1190" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">key-tools</span> Key Management hub</figcaption>
                </figure>
            </div>
            <ul>
                <li><strong>Build keys</strong> &mdash; generate and combine raw key material.</li>
                <li><strong>Wrap keys</strong> &mdash; bind a key to its usage / algorithm via TR-31 or Thales key blocks.</li>
                <li><strong>Distribute keys</strong> &mdash; split into shares for multi-custodian loading and validate the resulting halves.</li>
                <li><strong>Compute key check values</strong> &mdash; for vendor HSMs and operational sign-off.</li>
            </ul>
            <p>SSL / X.509 certificate handling is also grouped here for projects that need terminal or host-to-host TLS.</p>
        </ui-section>

        <ui-section anchor="all-tools" heading="All tools">
            <p>Every tool in this category &mdash; each card links to the detailed reference below.</p>
            <div class="hub-grid">
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">DEA Keys (DES / 3DES Utility)</div>
                        <p class="hub-desc">A multi-tab tool focused on raw 3DES key material.</p>
                        <a class="hub-link" href="/docs/key-tools#dea">View details →</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">Keyshare Generator</div>
                        <p class="hub-desc">Splits a single key into n shares such that all n are required to reconstruct the key (XOR-based component scheme).</p>
                        <a class="hub-link" href="/docs/key-tools#keyshare">View details →</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">TR-31 Key Block</div>
                        <p class="hub-desc">ASC X9.143 (formerly TR-31) defines a key block format that binds a key to its allowable usage, algorithm, mode, and…</p>
                        <a class="hub-link" href="/docs/key-tools#tr31">View details →</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">Thales Key Block</div>
                        <p class="hub-desc">The Thales-specific key block format used by PayShield HSMs.</p>
                        <a class="hub-link" href="/docs/key-tools#thales-block">View details →</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">Thales Key Calculator</div>
                        <p class="hub-desc">Vendor-aware calculations matching Thales PayShield host commands.</p>
                        <a class="hub-link" href="/docs/key-tools#thales-keys">View details →</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">Atalla Key Calculator</div>
                        <p class="hub-desc">Atalla / Utimaco AKB-style key block helpers, including AKB header construction and KCV verification.</p>
                        <a class="hub-link" href="/docs/key-tools#atalla">View details →</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">Safenet Key Calculator</div>
                        <p class="hub-desc">Safenet / Thales Luna key calculations for legacy and modern formats.</p>
                        <a class="hub-link" href="/docs/key-tools#safenet">View details →</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">RSA DER Public Key Tool</div>
                        <p class="hub-desc">Wrap a modulus and exponent into DER, with the sign-byte case handled.</p>
                        <a class="hub-link" href="/docs/key-tools#rsa-der">View details &rarr;</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">SSL / X.509 Certificate Tool</div>
                        <p class="hub-desc">An end-to-end certificate workflow tool for terminal-host TLS.</p>
                        <a class="hub-link" href="/docs/key-tools#ssl">View details →</a>
                    </div>
                </div>
            </div>
        </ui-section>

        <ui-section anchor="dea" heading="DEA Keys (DES / 3DES Utility)">
            <p>A multi-tab tool focused on raw 3DES key material.</p>

            <div class="shot-grid">
                <figure class="shot-fig" data-shot="dea" data-alt="DEA Keys Calculator on the Key Generator tab with keys-to-generate, 128-bit key length and odd key parity, beside the activity log" style="--shot-w:1512px">
                    <image-slot><img src="/images/docs/key-management-tools/dea-key-generator.png"
                                     alt="DEA Keys Calculator on the Key Generator tab with keys-to-generate, 128-bit key length and odd key parity, beside the activity log"
                                     width="3024" height="1844" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">key-tools</span> DEA Keys Calculator</figcaption>
                </figure>
            </div>

            <h3>Tabs</h3>
            <ul>
                <li><strong>Key Generator</strong> &mdash; Generate cryptographically random DES, 2-key 3DES, or 3-key 3DES keys.</li>
                <li><strong>Key Combination</strong> &mdash; XOR multiple key components together to reconstruct a key from shares.</li>
                <li><strong>Parity Enforcement</strong> &mdash; Adjust the LSB of each byte so each byte has odd parity (DES requirement).</li>
                <li><strong>Key Validation</strong> &mdash; Check parity, detect weak keys, and compute KCV (Key Check Value) using <code>00 00 00 00 00 00 00 00</code>.</li>
            </ul>

            <h3>Key Combination</h3>
            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="dea-combination" data-alt="Key Combination form with a TDES double-length key type and eight component fields, each with its own KCV box alongside" style="--shot-w:748px">
                    <image-slot><img src="/images/docs/key-management-tools/dea-key-combination.png"
                                     alt="Key Combination form with a TDES double-length key type and eight component fields, each with its own KCV box alongside"
                                     width="1496" height="1612" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">key-tools</span> Key Combination</figcaption>
                </figure>
                <div class="shot-split-body">
    <p>Components XOR together into the key. Pick the <strong>Key Type / Length</strong> &mdash; e.g. <code>TDES &mdash; Double length (16B / 32H)</code> &mdash; and the fields resize to match.</p>
    <p>There is room for <strong>eight</strong> components, and each carries its own <strong>KCV</strong> box beside it, with one more for the combined result. That is what makes a bad component obvious: check each custodian&rsquo;s KCV against their envelope before you trust the total.</p>
                </div>
            </div>

            <h3>Parity Enforcement</h3>
            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="dea-parity" data-alt="Parity Enforcement form with a hex key field and an odd or even key parity selector above the Enforce Parity button" style="--shot-w:746px">
                    <image-slot><img src="/images/docs/key-management-tools/dea-parity-enforcement.png"
                                     alt="Parity Enforcement form with a hex key field and an odd or even key parity selector above the Enforce Parity button"
                                     width="1492" height="644" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">key-tools</span> Parity Enforcement</figcaption>
                </figure>
                <div class="shot-split-body">
    <p>DES ignores the low bit of each byte, so specs use it as a parity bit. Paste a <strong>Key (Hex)</strong>, choose <strong>Odd</strong> or <strong>Even</strong>, and <strong>Enforce Parity</strong> adjusts each byte to match &mdash; the key value is unchanged as far as the cipher is concerned.</p>
                </div>
            </div>

            <h3>Key Validation</h3>
            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="dea-lookup" data-alt="Key Lookup form with a hex key, a Check KCV checkbox, an optional KCV field and Any / Odd / Even parity radios above the Lookup Key button" style="--shot-w:747px">
                    <image-slot><img src="/images/docs/key-management-tools/dea-key-lookup.png"
                                     alt="Key Lookup form with a hex key, a Check KCV checkbox, an optional KCV field and Any / Odd / Even parity radios above the Lookup Key button"
                                     width="1494" height="1002" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">key-tools</span> Key Lookup</figcaption>
                </figure>
                <div class="shot-split-body">
    <p>The validation tab is a <strong>Key Lookup</strong>: give it a key and it reports what the key actually is.</p>
    <ul>
        <li><strong>Key (Hex)</strong></li>
        <li><strong>Check KCV?</strong> &mdash; When ticked, the <strong>KCV (Optional)</strong> field is compared against the computed value instead of just reporting it.</li>
        <li><strong>Parity</strong> &mdash; <code>Any</code>, <code>Odd</code> or <code>Even</code>; the check fails if the key does not match the parity you assert.</li>
    </ul>
    <p>Button: <strong>Lookup Key</strong>.</p>
                </div>
            </div>
        </ui-section>

        <ui-section anchor="keyshare" heading="Keyshare Generator">
            <p>Splits a single key into <em>n</em> shares such that all <em>n</em> are required to reconstruct the key (XOR-based component scheme). Useful for multi-custodian key loading.</p>

            <div class="shot-grid">
                <figure class="shot-fig" data-shot="keyshare" data-alt="Keyshare Generator with global parity and key type options above a 2 Parts / 3 Parts tab pair, showing part fields with a combined key and its KCV" style="--shot-w:748px">
                    <image-slot><img src="/images/docs/key-management-tools/keyshare-2-part.png"
                                     alt="Keyshare Generator with global parity and key type options above a 2 Parts / 3 Parts tab pair, showing part fields with a combined key and its KCV"
                                     width="1496" height="1326" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">key-tools</span> Keyshare Generator</figcaption>
                </figure>
            </div>

            <h3>Inputs</h3>
            <ul>
                <li><strong>Global Options</strong> &mdash; <strong>Parity</strong> (<code>Ignore</code> by default) and <strong>Key Type</strong> (<code>DES/TDES</code>), applied to the whole operation.</li>
                <li><strong>2 Parts / 3 Parts</strong> &mdash; A tab pair rather than a count field; each tab shows exactly that many part fields.</li>
                <li><strong>Part 1</strong>, <strong>Part 2</strong> (and <strong>Part 3</strong>) &mdash; Leave them empty to have the tool generate them.</li>
            </ul>
            <p>Button: <strong>Generate 2 Parts</strong> (or 3). The <strong>Combined Key</strong> and its <strong>KCV</strong> appear together at the bottom, so the reconstructed value can be checked before it leaves the screen.</p>

            <h3>Output</h3>
            <ul>
                <li>Random components for shares 1 to <em>n</em>&minus;1.</li>
                <li>Final component computed so XOR of all components = the master key.</li>
                <li>KCV of each share for safe transport verification.</li>
            </ul>

            <div class="info-card warning">
                <div class="info-card-title">Custody</div>
                <p>Components must be transported and stored separately under the control of different custodians. Recombining shares brings them under the control of a single trustee, so do this only inside the HSM during loading.</p>
            </div>
        </ui-section>

        <ui-section anchor="tr31" heading="TR-31 Key Block">
            <p>ASC X9.143 (formerly TR-31) defines a key block format that binds a key to its allowable usage, algorithm, mode, and exportability. A TR-31 block is opaque to anything outside the issuing HSM but lets two HSMs exchange keys without losing metadata.</p>

            <div class="shot-grid">
                <figure class="shot-fig" data-shot="tr31" data-alt="TR-31 Key block on the Encode tab with KBPK, plain key, header, version id, key usage, algorithm, mode of use, key version and exportability fields, beside the activity log" style="--shot-w:1510px">
                    <image-slot><img src="/images/docs/key-management-tools/tr31-encode.png"
                                     alt="TR-31 Key block on the Encode tab with KBPK, plain key, header, version id, key usage, algorithm, mode of use, key version and exportability fields, beside the activity log"
                                     width="3020" height="1842" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">key-tools</span> TR-31 Key Block</figcaption>
                </figure>
            </div>

            <h3>Tabs</h3>
            <ul>
                <li><strong>Wrap</strong> &mdash; Build a key block from a clear key under a Key Block Protection Key (KBPK).</li>
                <li><strong>Unwrap</strong> &mdash; Decode a key block, validate its MAC, and reveal the contents.</li>
            </ul>

            <h3>Inputs (Wrap)</h3>
            <ul>
                <li><strong>KBPK</strong> &mdash; 32 / 48 hex chars (3DES) or 64 hex (AES).</li>
                <li><strong>Clear Key</strong> &mdash; Key to wrap.</li>
                <li><strong>Key Usage</strong> &mdash; Two-character code (e.g. <code>P0</code> = PIN encryption, <code>M0</code> = MAC, <code>K0</code> = Key Encryption Key).</li>
                <li><strong>Algorithm</strong> &mdash; <code>D</code> = DES, <code>T</code> = TDES, <code>A</code> = AES, etc.</li>
                <li><strong>Mode of Use</strong> &mdash; <code>E</code> = encrypt, <code>D</code> = decrypt, <code>B</code> = both, <code>N</code> = no restriction.</li>
                <li><strong>Key Version Number</strong> &mdash; Two characters.</li>
                <li><strong>Exportability</strong> &mdash; <code>E</code> = exportable, <code>S</code> = sensitive (no clear export), <code>N</code> = no export.</li>
            </ul>

            <h3>Output</h3>
            <p>An ASCII key block string starting with the version (<code>A</code>, <code>B</code>, <code>C</code>, <code>D</code>) plus encrypted key, MAC, and optional optional blocks.</p>

            <pre><code>A0072P0TE00E0000ABC...   (D variant TR-31 block)</code></pre>
        </ui-section>

        <ui-section anchor="thales-block" heading="Thales Key Block">
            <p>The Thales-specific key block format used by PayShield HSMs. Similar in concept to TR-31 but with Thales&rsquo; own header and key usage codes.</p>

            <div class="shot-grid">
                <figure class="shot-fig" data-shot="thales-block" data-alt="Thales Key Block on the Encode tab with a 3DES / AES KBPK version selector, key block protection key with KCV, clear key, and the key block header attributes" style="--shot-w:756px">
                    <image-slot><img src="/images/docs/key-management-tools/thales-key-block-encode.png"
                                     alt="Thales Key Block on the Encode tab with a 3DES / AES KBPK version selector, key block protection key with KCV, clear key, and the key block header attributes"
                                     width="1512" height="1614" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">key-tools</span> Thales Key Block</figcaption>
                </figure>
            </div>

            <h3>Workflow</h3>
            <ul>
                <li>Pick a <strong>Thales Key Type</strong> &mdash; ZMK, ZPK, TMK, BDK, ZEK, etc.</li>
                <li>Provide the <strong>LMK Variant</strong> applicable to that key type.</li>
                <li>Provide the <strong>clear key</strong> material.</li>
                <li>The tool returns the encrypted key under LMK along with its KCV.</li>
            </ul>

            <div class="info-card tip">
                <div class="info-card-title">Tip</div>
                <p>Use this when you have a clear key and want to import it under the HSM Simulator&rsquo;s LMK without typing it through the console.</p>
            </div>
        </ui-section>

        <ui-section anchor="thales-keys" heading="Thales Key Calculator">
            <p>Vendor-aware calculations matching Thales PayShield host commands.</p>

            <div class="shot-grid">
                <figure class="shot-fig" data-shot="thales-keys" data-alt="Thales Keys Encryption/Decoding with a hex key, key scheme, double or triple LMK size, an LMK pair selector and a variant, above Encrypt and Decrypt buttons" style="--shot-w:757px">
                    <image-slot><img src="/images/docs/key-management-tools/thales-key-encryption.png"
                                     alt="Thales Keys Encryption/Decoding with a hex key, key scheme, double or triple LMK size, an LMK pair selector and a variant, above Encrypt and Decrypt buttons"
                                     width="1514" height="1388" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">key-tools</span> Thales Key Calculator</figcaption>
                </figure>
            </div>

            <h3>Operations</h3>
            <ul>
                <li><strong>Key Generation</strong> &mdash; Equivalent to <code>A0</code> (Generate a Key) host command.</li>
                <li><strong>Key Translation</strong> &mdash; Equivalent to <code>A6</code> (Translate a Key from One ZMK to Another).</li>
                <li><strong>KCV Computation</strong> &mdash; Match Thales-style 6-digit KCVs for operational sign-off.</li>
                <li><strong>Variant Application</strong> &mdash; Apply LMK and ZMK variants used during key wrapping.</li>
            </ul>
        </ui-section>

        <ui-section anchor="atalla" heading="Atalla Key Calculator">
            <p>Atalla / Utimaco AKB-style key block helpers, in both directions: build a key block from a clear key, or take one apart.</p>

            <div class="shot-grid">
                <figure class="shot-fig" data-shot="atalla" data-alt="Atalla Keys Calculator on the Key Encryption tab with hex key, AKB header and MFK key fields above the Encrypt Key button" style="--shot-w:752px">
                    <image-slot><img src="/images/docs/key-management-tools/atalla-key-encryption.png"
                                     alt="Atalla Keys Calculator on the Key Encryption tab with hex key, AKB header and MFK key fields above the Encrypt Key button"
                                     width="1504" height="1152" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">key-tools</span> Atalla Key Calculator</figcaption>
                </figure>
            </div>

            <h3>Key Encryption</h3>
            <ul>
                <li><strong>Key (Hex)</strong> &mdash; The clear key to wrap.</li>
                <li><strong>AKB Header (Hex)</strong> &mdash; The attribute header that travels inside the block.</li>
                <li><strong>MFK Key (Hex)</strong> &mdash; The Master File Key the block is encrypted under.</li>
            </ul>
            <p>Button: <strong>Encrypt Key</strong>.</p>

            <h3>AKB Decode</h3>
            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="atalla-decode" data-alt="AKB Decode form with an Atalla Key Block field, a Check KCV checkbox, an optional KCV, a parity selector and an MFK key, above the Decode AKB button" style="--shot-w:750px">
                    <image-slot><img src="/images/docs/key-management-tools/atalla-akb-decode.png"
                                     alt="AKB Decode form with an Atalla Key Block field, a Check KCV checkbox, an optional KCV, a parity selector and an MFK key, above the Decode AKB button"
                                     width="1500" height="1234" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">key-tools</span> AKB Decode</figcaption>
                </figure>
                <div class="shot-split-body">
    <ul>
        <li><strong>AKB (Atalla Key Block)</strong> &mdash; The block to open.</li>
        <li><strong>Check KCV?</strong> and <strong>KCV (S)</strong> &mdash; Verify the recovered key against a known check value rather than trusting the decode.</li>
        <li><strong>Parity</strong> &mdash; <code>None</code> by default.</li>
        <li><strong>MFK Key (Hex)</strong></li>
    </ul>
    <p>Button: <strong>Decode AKB</strong>.</p>
                </div>
            </div>
        </ui-section>

        <ui-section anchor="safenet" heading="Safenet Key Calculator">
            <p>Safenet / Thales Luna key calculations for legacy and modern formats.</p>

            <div class="shot-grid">
                <figure class="shot-fig" data-shot="safenet" data-alt="Safenet Keys Calculator with a hex key, a key format of single length DES, a DPK variant, hexadecimal key input format and a KM key, above Encrypt and Decrypt buttons" style="--shot-w:757px">
                    <image-slot><img src="/images/docs/key-management-tools/safenet-key-encryption.png"
                                     alt="Safenet Keys Calculator with a hex key, a key format of single length DES, a DPK variant, hexadecimal key input format and a KM key, above Encrypt and Decrypt buttons"
                                     width="1514" height="1450" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">key-tools</span> Safenet Key Calculator</figcaption>
                </figure>
            </div>
        </ui-section>

        <ui-section anchor="ssl" heading="SSL / X.509 Certificate Tool">
            <p>An end-to-end certificate workflow tool for terminal-host TLS.</p>

            <div class="shot-grid">
                <figure class="shot-fig" data-shot="ssl" data-alt="SSL Certificate (X.509) Utility on the Keys tab with an RSA key type, 2048-bit length and public and private key fields, beside the activity log" style="--shot-w:1512px">
                    <image-slot><img src="/images/docs/key-management-tools/ssl-key-pair.png"
                                     alt="SSL Certificate (X.509) Utility on the Keys tab with an RSA key type, 2048-bit length and public and private key fields, beside the activity log"
                                     width="3024" height="1842" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">key-tools</span> SSL Certificate (X.509) Utility</figcaption>
                </figure>
            </div>

            <h3>Tabs</h3>
            <ul>
                <li><strong>Keys</strong> &mdash; Generate RSA key pairs (2048 / 3072 / 4096-bit) or read existing keys.</li>
                <li><strong>CSRs</strong> &mdash; Build a Certificate Signing Request from a key and DN parameters (CN, OU, O, L, S, C).</li>
                <li><strong>Read CSR</strong> &mdash; Parse and display the contents of an existing CSR.</li>
                <li><strong>Self-Signed</strong> &mdash; Issue a self-signed certificate from a key + DN, with configurable validity.</li>
                <li><strong>Read Certificate</strong> &mdash; Parse and display an X.509 certificate, including extensions.</li>
            </ul>

            <h3>Inputs</h3>
            <ul>
                <li><strong>Common Name (CN)</strong></li>
                <li><strong>Organisation (O), Org. Unit (OU)</strong></li>
                <li><strong>Locality (L), State (S), Country (C)</strong></li>
                <li><strong>Validity (Days)</strong></li>
                <li><strong>Key Size</strong> &mdash; 2048 / 3072 / 4096.</li>
            </ul>

            <p>Outputs are displayed as PEM and as parsed fields side-by-side, with a copy button per artifact.</p>
        </ui-section>

        <ui-section anchor="rsa-der" heading="RSA DER Public Key Tool">
            <p>An RSA public key is a modulus and an exponent, but what a host expects on the wire is those two numbers wrapped in DER. This encoder does that wrapping, and is the tool to reach for when a certificate library rejects a key you know is correct.</p>

            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="rsa-der" data-alt="DER Public Key Encoder with modulus and exponent fields, their own encoding selectors and a toggle modulus negative checkbox above the Encode Key button" style="--shot-w:758px">
                    <image-slot><img src="/images/docs/key-management-tools/rsa-der-encoder.png"
                                     alt="DER Public Key Encoder with modulus and exponent fields, their own encoding selectors and a toggle modulus negative checkbox above the Encode Key button"
                                     width="1516" height="1420" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">key-tools</span> DER Public Key Encoder</figcaption>
                </figure>
                <div class="shot-split-body">
                    <h3>Inputs</h3>
                    <ul>
                        <li><strong>Modulus</strong> with its own <strong>Modulus Encoding</strong> selector.</li>
                        <li><strong>Exponent</strong> with an <strong>Exponent Encoding</strong> selector &mdash; commonly <code>03</code> or <code>010001</code>.</li>
                        <li><strong>Toggle Modulus Negative</strong> &mdash; DER reads the leading bit as a sign, so a modulus starting above <code>0x7F</code> needs a leading zero byte. This is the switch for that case.</li>
                    </ul>
                    <p>Button: <strong>Encode Key</strong>.</p>
                </div>
            </div>
        </ui-section>

        <ui-section anchor="tips" heading="Tips">
            <ul>
                <li>Always verify the KCV after combining shares or unwrapping a key block. A wrong KCV almost always means a typo in one share.</li>
                <li>For TR-31, watch out for case sensitivity in the header &mdash; the key usage and mode codes are uppercase.</li>
                <li>If your HSM rejects an imported key, compare the version byte (<code>A</code> vs <code>B</code> vs <code>C</code> vs <code>D</code>) &mdash; older HSMs may only accept specific versions.</li>
                <li>For SSL, generate the key first, then the CSR, then the cert &mdash; the tool will pre-fill DN parameters from a previous CSR if you stay on the same session.</li>
            </ul>
        </ui-section>
    </main>

<aside class="pro-nudge"><span class="pn-tag">✦ Pro</span><p>Testing with a team, or certifying with a scheme? Pro raises the CPS ceiling, unlocks the full algorithm set and deep simulator tweaks, plus hosted endpoints and priority support.</p><a href="/pro">Register for Pro →</a></aside>`,
})
export class DocsKeyToolsPage {}
