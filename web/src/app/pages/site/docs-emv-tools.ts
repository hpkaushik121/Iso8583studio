import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { SitePage } from './site-page';

@Component({
  selector: 'page-docs-emv-tools',
  changeDetection: ChangeDetectionStrategy.OnPush,
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-emv-tools' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<main class="doc-body">
        <div class="breadcrumb">
            <a href="/">Home</a>
            <span class="breadcrumb-sep">/</span>
            <a href="/docs">Documentation</a>
            <span class="breadcrumb-sep">/</span>
            <span>EMV Tools</span>
        </div>

        <h1 class="page-title">EMV Tools</h1>
        <p class="page-description">The EMV &amp; Card Tools group &mdash; offline authentication verifiers (SDA, DDA), cryptogram calculators for EMV 4.1, EMV 4.2, M/Chip and VSDC, issuer-script secure messaging, CAP tokens and HCE contactless keys. Each tool is hex-driven, with annotated outputs and an audit log.</p>

        <section class="doc-section" id="overview">
            <h2>Introduction</h2>
            <p>The <strong>EMV &amp; Card Tools</strong> group &mdash; smart card, EMV and contactless payment tools &mdash; is organised by the stage of a transaction it belongs to: offline authentication first (SDA, DDA), then the cryptogram calculators for each scheme, then the issuer-script and token tools that run after authorisation.</p>

            <div class="shot-grid">
                <figure class="shot-fig wide" data-shot="overview" data-alt="The EMV &amp; Card Tools hub with cards for the EMV 4.1, EMV 4.2, MasterCard and VSDC crypto calculators, SDA and DDA verification, CAP Token, HCE Visa and Secure Messaging" style="--shot-w:1078px">
                    <image-slot><img src="/images/docs/emv-tools/emv-card-tools-hub.png"
                                     alt="The EMV &amp; Card Tools hub with cards for the EMV 4.1, EMV 4.2, MasterCard and VSDC crypto calculators, SDA and DDA verification, CAP Token, HCE Visa and Secure Messaging"
                                     width="2156" height="1186" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">emv-tools</span> EMV &amp; Card Tools hub</figcaption>
                </figure>
            </div>
            <p>Every calculator carries its own activity log on the right, so each intermediate value &mdash; derived key, session key, recovered certificate &mdash; is visible rather than just the final result.</p>
        </section>

        <section class="doc-section" id="all-tools">
            <h2>All tools</h2>
            <p>Every tool in this group, in the order an EMV transaction reaches them &mdash; each card links to the detailed reference below.</p>
            <div class="hub-grid">
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">SDA Verification</div>
                        <p class="hub-desc">Verify issuer-signed static data against a CA public key.</p>
                        <a class="hub-link" href="/docs/emv-tools#sda">View details &rarr;</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">DDA Verification</div>
                        <p class="hub-desc">Check the card&rsquo;s signature over terminal-supplied dynamic data.</p>
                        <a class="hub-link" href="/docs/emv-tools#dda">View details &rarr;</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">EMV 4.1 Crypto Calculator</div>
                        <p class="hub-desc">The reference cryptogram chain: UDK, session keys, ARQC/TC/AAC, ARPC and key utilities.</p>
                        <a class="hub-link" href="/docs/emv-tools#app-crypto">View details &rarr;</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">EMV 4.2 Crypto Calculator</div>
                        <p class="hub-desc">The same chain under the EMV 4.2 rules, derivation stated on the form.</p>
                        <a class="hub-link" href="/docs/emv-tools#emv42">View details &rarr;</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">MasterCard M/Chip Crypto</div>
                        <p class="hub-desc">M/Chip derivation, with a separate EMV 2000 session key tab.</p>
                        <a class="hub-link" href="/docs/emv-tools#mchip">View details &rarr;</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">VSDC Crypto Calculator</div>
                        <p class="hub-desc">Visa Smart Debit/Credit: UDK, session keys, AAC/ARQC/TC and ARPC.</p>
                        <a class="hub-link" href="/docs/emv-tools#vsdc">View details &rarr;</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">Secure Messaging</div>
                        <p class="hub-desc">Build and verify SMC / SMI protection on issuer scripts.</p>
                        <a class="hub-link" href="/docs/emv-tools#secure-msg">View details &rarr;</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">CAP Token Computation</div>
                        <p class="hub-desc">Compute Chip Authentication Programme tokens for banking 2FA.</p>
                        <a class="hub-link" href="/docs/emv-tools#cap">View details &rarr;</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">HCE (Host Card Emulation)</div>
                        <p class="hub-desc">Generate the Limited-Use and Single-Use Keys HCE tokenisation needs.</p>
                        <a class="hub-link" href="/docs/emv-tools#hce">View details &rarr;</a>
                    </div>
                </div>
            </div>
        </section>

        <section class="doc-section" id="sda">
            <h2>SDA Verification</h2>
            <p>Static Data Authentication: the terminal verifies issuer-signed static data against a CA public key. The tool splits the job into the two steps the terminal performs &mdash; recover the issuer public key from its certificate, then use that key to check the signature over the card&rsquo;s static data.</p>

            <h3>Retrieve Issuer Public Key</h3>
            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="sda-issuer-pk" data-alt="Retrieve Issuer Public Key form with CA PK modulus and exponent, issuer PK certificate, remainder and exponent fields, each showing a live character count" style="--shot-w:747px">
                    <image-slot><img src="/images/docs/emv-tools/sda-retrieve-issuer-pk.png"
                                     alt="Retrieve Issuer Public Key form with CA PK modulus and exponent, issuer PK certificate, remainder and exponent fields, each showing a live character count"
                                     width="1494" height="1328" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">emv-tools</span> Retrieve Issuer Public Key</figcaption>
                </figure>
                <div class="shot-split-body">
    <h3>Inputs</h3>
    <ul>
        <li><strong>CA PK Modulus</strong> and <strong>CA PK Exponent</strong> &mdash; From the Visa / MasterCard CA hierarchy, for the index in tag <code>8F</code>.</li>
        <li><strong>Issuer PK Certificate</strong> &mdash; Tag <code>90</code>.</li>
        <li><strong>Issuer PK Remainder</strong> &mdash; Tag <code>92</code>; optional.</li>
        <li><strong>Issuer PK Exponent</strong> &mdash; Tag <code>9F32</code>.</li>
    </ul>
    <p>Button: <strong>Retrieve Key</strong>. Every field carries a live character count, and one with an odd number of hex characters is flagged before you run it &mdash; the usual cause of a certificate that will not recover.</p>
                </div>
            </div>

            <h3>Verify SSAD</h3>
            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="sda-verify-ssad" data-alt="Verify SSAD form with the recovered issuer PK modulus and the signed static application data, above the Verify SSAD button" style="--shot-w:748px">
                    <image-slot><img src="/images/docs/emv-tools/sda-verify-ssad.png"
                                     alt="Verify SSAD form with the recovered issuer PK modulus and the signed static application data, above the Verify SSAD button"
                                     width="1496" height="890" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">emv-tools</span> Verify SSAD</figcaption>
                </figure>
                <div class="shot-split-body">
    <h3>Inputs</h3>
    <ul>
        <li><strong>Issuer PK Modulus</strong> &mdash; The key recovered in the previous step.</li>
        <li><strong>SSAD (Signed Static Data)</strong> &mdash; Tag <code>93</code>, built from the AFL records.</li>
    </ul>
    <p>Button: <strong>Verify SSAD</strong>. The log reports the recovered hash alongside the pass/fail result, so a mismatch can be traced to the data rather than the key.</p>
                </div>
            </div>
        </section>

        <section class="doc-section" id="dda">
            <h2>DDA Verification</h2>
            <p>Dynamic Data Authentication: the card signs terminal-supplied dynamic data, proving it holds the ICC private key. Three tabs walk the chain down &mdash; issuer key, then card key, then the signature itself.</p>

            <div class="shot-grid">
                <figure class="shot-fig" data-shot="dda" data-alt="DDA - Dynamic Data Authentication with its three tabs (Retrieve Issuer PK, Retrieve ICC PK, Verify SDAD), the Retrieve Issuer PK form filled in, beside the activity log" style="--shot-w:1512px">
                    <image-slot><img src="/images/docs/emv-tools/dda-retrieve-issuer-pk.png"
                                     alt="DDA - Dynamic Data Authentication with its three tabs (Retrieve Issuer PK, Retrieve ICC PK, Verify SDAD), the Retrieve Issuer PK form filled in, beside the activity log"
                                     width="3024" height="1842" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">emv-tools</span> DDA Verification</figcaption>
                </figure>
            </div>

            <h3>Retrieve ICC Public Key</h3>
            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="dda-icc-pk" data-alt="Retrieve ICC Public Key form with issuer PK modulus and exponent, ICC PK certificate, remainder and exponent, static data to authenticate and the AIP" style="--shot-w:751px">
                    <image-slot><img src="/images/docs/emv-tools/dda-retrieve-icc-pk.png"
                                     alt="Retrieve ICC Public Key form with issuer PK modulus and exponent, ICC PK certificate, remainder and exponent, static data to authenticate and the AIP"
                                     width="1502" height="1622" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">emv-tools</span> Retrieve ICC Public Key</figcaption>
                </figure>
                <div class="shot-split-body">
    <h3>Inputs</h3>
    <ul>
        <li><strong>Issuer PK Modulus / Exponent</strong> &mdash; Recovered on the first tab.</li>
        <li><strong>ICC PK Certificate</strong> &mdash; Tag <code>9F46</code>.</li>
        <li><strong>ICC PK Remainder</strong> &mdash; Tag <code>9F48</code>; optional.</li>
        <li><strong>ICC PK Exponent</strong> &mdash; Tag <code>9F47</code>.</li>
        <li><strong>Static Data To Authenticate</strong> &mdash; The AFL-built record the certificate hash covers.</li>
        <li><strong>AIP</strong> &mdash; Tag <code>82</code>, which the hash includes when the card asks for it.</li>
    </ul>
    <p>Button: <strong>Retrieve ICC Key</strong>.</p>
                </div>
            </div>

            <h3>Verify SDAD</h3>
            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="dda-verify-sdad" data-alt="Verify SDAD form with the ICC PK modulus and exponent, the signed dynamic data, and the dynamic data from tag 9F37" style="--shot-w:749px">
                    <image-slot><img src="/images/docs/emv-tools/dda-verify-sdad.png"
                                     alt="Verify SDAD form with the ICC PK modulus and exponent, the signed dynamic data, and the dynamic data from tag 9F37"
                                     width="1498" height="1038" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">emv-tools</span> Verify SDAD</figcaption>
                </figure>
                <div class="shot-split-body">
    <h3>Inputs</h3>
    <ul>
        <li><strong>ICC PK Modulus / Exponent</strong> &mdash; From the previous tab.</li>
        <li><strong>SDAD (Signed Dynamic Data)</strong> &mdash; Tag <code>9F4B</code>, returned by INTERNAL AUTHENTICATE.</li>
        <li><strong>Dynamic Data</strong> &mdash; The terminal&rsquo;s unpredictable number, e.g. from tag <code>9F37</code>.</li>
    </ul>
    <p>Button: <strong>Verify Dynamic Signature</strong>.</p>
                </div>
            </div>
        </section>

        <section class="doc-section" id="app-crypto">
            <h2>EMV 4.1 Crypto Calculator</h2>
            <p>The reference cryptogram calculator, and the one to learn the flow on: five tabs that follow the key hierarchy from the issuer master key down to the response cryptogram &mdash; <strong>UDK Derivation</strong>, <strong>Session Keys</strong>, <strong>Cryptogram</strong>, <strong>ARPC</strong>, <strong>Utilities</strong>. The scheme-specific calculators below are the same shape with their own derivations.</p>

            <div class="shot-grid">
                <figure class="shot-fig" data-shot="emv41" data-alt="EMV 4.1 Crypto Calculator on the UDK Derivation tab with a master derivation key, PAN, PAN sequence, derivation option and key parity, beside the activity log" style="--shot-w:1512px">
                    <image-slot><img src="/images/docs/emv-tools/emv41-udk.png"
                                     alt="EMV 4.1 Crypto Calculator on the UDK Derivation tab with a master derivation key, PAN, PAN sequence, derivation option and key parity, beside the activity log"
                                     width="3024" height="1840" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">emv-tools</span> EMV 4.1 Crypto Calculator</figcaption>
                </figure>
            </div>

            <h3>UDK Derivation</h3>
            <ul>
                <li><strong>Master Derivation Key (MDK)</strong> &mdash; 32 hex characters; the counter turns green at the right length.</li>
                <li><strong>PAN</strong> and <strong>PAN Sequence</strong> &mdash; What binds the derived key to one card. The PAN is Luhn-checked, and a failure is a warning rather than a block: the banner reads <em>PAN Luhn checksum failed, but calculation will proceed</em>, which is what you want for test PANs.</li>
                <li><strong>Derivation Option</strong> &mdash; <code>OPTION_A</code> or <code>OPTION_B</code>.</li>
                <li><strong>Key Parity</strong> &mdash; <code>ODD</code>, <code>EVEN</code> or none.</li>
            </ul>
            <p>Button: <strong>Calculate UDK</strong>.</p>

            <h3>Session Keys</h3>
            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="emv41-session-key" data-alt="Session Key Derivation form with the master key (UDK), an all-zero initial vector, ATC, branch factor, height and key parity above the Generate Session Key button" style="--shot-w:749px">
                    <image-slot><img src="/images/docs/emv-tools/emv41-session-key.png"
                                     alt="Session Key Derivation form with the master key (UDK), an all-zero initial vector, ATC, branch factor, height and key parity above the Generate Session Key button"
                                     width="1498" height="1134" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">emv-tools</span> Session Key Derivation</figcaption>
                </figure>
                <div class="shot-split-body">
    <ul>
        <li><strong>Master Key (UDK)</strong> &mdash; The card key from the previous tab.</li>
        <li><strong>Initial Vector (IV)</strong> &mdash; 32 hex characters; zeros for the common case.</li>
        <li><strong>ATC</strong> &mdash; The transaction counter the session key is diversified on.</li>
        <li><strong>Branch Factor</strong> and <strong>Height</strong> &mdash; The EMV tree parameters, defaulting to 50 and 8.</li>
        <li><strong>Key Parity</strong></li>
    </ul>
    <p>Button: <strong>Generate Session Key</strong>.</p>
                </div>
            </div>

            <h3>Cryptogram</h3>
            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="emv41-cryptogram" data-alt="Application Cryptogram form with a session key, terminal data, ICC data, a cryptogram type of ARQC and ISO 9797 method 1 padding, above the Generate ARQC button" style="--shot-w:754px">
                    <image-slot><img src="/images/docs/emv-tools/emv41-cryptogram.png"
                                     alt="Application Cryptogram form with a session key, terminal data, ICC data, a cryptogram type of ARQC and ISO 9797 method 1 padding, above the Generate ARQC button"
                                     width="1508" height="1104" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">emv-tools</span> Application Cryptogram</figcaption>
                </figure>
                <div class="shot-split-body">
    <ul>
        <li><strong>Session Key</strong> &mdash; From the Session Keys tab.</li>
        <li><strong>Terminal Data</strong> &mdash; The CDOL-built terminal side of the request.</li>
        <li><strong>ICC Data</strong> &mdash; AIP, ATC, CVR and the rest of the card side.</li>
        <li><strong>Cryptogram Type</strong> &mdash; <code>ARQC</code>, <code>TC</code> or <code>AAC</code>. The button label follows the selection.</li>
        <li><strong>Padding Method</strong> &mdash; e.g. <code>METHOD_1_ISO_9797</code>.</li>
    </ul>
                </div>
            </div>

            <h3>ARPC</h3>
            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="emv41-arpc" data-alt="ARPC Generation form with a session key, the transaction cryptogram, a Y3 response code and an ARPC method, above the Generate ARPC button" style="--shot-w:751px">
                    <image-slot><img src="/images/docs/emv-tools/emv41-arpc.png"
                                     alt="ARPC Generation form with a session key, the transaction cryptogram, a Y3 response code and an ARPC method, above the Generate ARPC button"
                                     width="1502" height="926" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">emv-tools</span> ARPC Generation</figcaption>
                </figure>
                <div class="shot-split-body">
    <ul>
        <li><strong>Session Key</strong> and <strong>Transaction Cryptogram</strong> &mdash; The ARQC the card produced.</li>
        <li><strong>Response Code</strong> &mdash; The two-character ARC, e.g. <code>Y3</code>.</li>
        <li><strong>ARPC Method</strong> &mdash; Method 1 or 2.</li>
    </ul>
    <p>Button: <strong>Generate ARPC</strong>.</p>
                </div>
            </div>

            <h3>Utilities</h3>
            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="emv41-utilities" data-alt="Cryptographic Utilities panel with a single hex key field above the Calculate KCV button" style="--shot-w:747px">
                    <image-slot><img src="/images/docs/emv-tools/emv41-utilities-kcv.png"
                                     alt="Cryptographic Utilities panel with a single hex key field above the Calculate KCV button"
                                     width="1494" height="536" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">emv-tools</span> Cryptographic Utilities</figcaption>
                </figure>
                <div class="shot-split-body">
    <p>Key validation helpers that sit alongside the derivation tabs. Paste a <strong>Key (Hex)</strong> and <strong>Calculate KCV</strong> returns its check value &mdash; the quickest way to confirm the key you loaded is the key you meant.</p>
                </div>
            </div>
        </section>

        <section class="doc-section" id="emv42">
            <h2>EMV 4.2 Crypto Calculator</h2>
            <p>The same five tabs against the EMV 4.2 rules. The UDK tab states its derivation in the panel subtitle &mdash; <em>EMV 4.2 Option A</em> &mdash; and drops the separate parity selector, so the form is just the master key and the card identifiers.</p>

            <div class="shot-grid">
                <figure class="shot-fig" data-shot="emv42" data-alt="EMV 4.2 Crypto Calculator on the UDK Derivation tab, labelled EMV 4.2 Option A, with master derivation key, PAN and PAN sequence number fields, beside the activity log" style="--shot-w:1511px">
                    <image-slot><img src="/images/docs/emv-tools/emv42-udk.png"
                                     alt="EMV 4.2 Crypto Calculator on the UDK Derivation tab, labelled EMV 4.2 Option A, with master derivation key, PAN and PAN sequence number fields, beside the activity log"
                                     width="3022" height="1842" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">emv-tools</span> EMV 4.2 Crypto Calculator</figcaption>
                </figure>
            </div>

            <ul>
                <li><strong>Master Derivation Key (MDK)</strong> &mdash; 32 characters.</li>
                <li><strong>Primary Account Number (PAN)</strong></li>
                <li><strong>PAN Sequence Number</strong></li>
            </ul>
            <p>Button: <strong>Calculate UDK</strong>. Session key, cryptogram and ARPC follow on their own tabs exactly as in 4.1.</p>
        </section>

        <section class="doc-section" id="mchip">
            <h2>MasterCard M/Chip Crypto Calculator</h2>
            <p>M/Chip&rsquo;s own derivation, with a fifth tab that 4.1 and 4.2 do not have: <strong>Session Key (EMV 2000)</strong>, kept for cards personalised against the older scheme.</p>

            <div class="shot-grid">
                <figure class="shot-fig" data-shot="mchip" data-alt="MasterCard M/Chip Crypto Calculator with UDK, Session Key (EMV 2000), Session Keys, AAC/ARQC/TC and ARPC tabs, the UDK form filled in, beside the activity log" style="--shot-w:1512px">
                    <image-slot><img src="/images/docs/emv-tools/mchip-udk.png"
                                     alt="MasterCard M/Chip Crypto Calculator with UDK, Session Key (EMV 2000), Session Keys, AAC/ARQC/TC and ARPC tabs, the UDK form filled in, beside the activity log"
                                     width="3024" height="1848" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">emv-tools</span> MasterCard M/Chip Crypto Calculator</figcaption>
                </figure>
            </div>

            <ul>
                <li><strong>MDK</strong>, <strong>PAN</strong>, <strong>PAN Sequence No.</strong></li>
                <li><strong>UDK Derivation Option</strong> &mdash; <code>Option A</code> or <code>Option B</code>.</li>
                <li><strong>Key Parity</strong> &mdash; <code>NONE</code> by default here, unlike the EMV 4.1 tool.</li>
            </ul>
            <p>Button: <strong>Generate UDK</strong>. The cryptogram tab is labelled <strong>AAC/ARQC/TC</strong> &mdash; one tab covering all three types.</p>
        </section>

        <section class="doc-section" id="vsdc">
            <h2>VSDC Crypto Calculator</h2>
            <p>Visa Smart Debit/Credit, in four tabs: <strong>UDK</strong>, <strong>Session Keys</strong>, <strong>AAC/ARQC/TC</strong>, <strong>ARPC</strong>.</p>

            <div class="shot-grid">
                <figure class="shot-fig" data-shot="vsdc" data-alt="VSDC Crypto Calculator on the UDK tab with MDK, PAN, PAN sequence number, derivation option and odd key parity, beside the activity log" style="--shot-w:1512px">
                    <image-slot><img src="/images/docs/emv-tools/vsdc-udk.png"
                                     alt="VSDC Crypto Calculator on the UDK tab with MDK, PAN, PAN sequence number, derivation option and odd key parity, beside the activity log"
                                     width="3024" height="1842" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">emv-tools</span> VSDC Crypto Calculator</figcaption>
                </figure>
            </div>

            <ul>
                <li><strong>MDK</strong>, <strong>PAN</strong>, <strong>PAN Sequence No.</strong></li>
                <li><strong>UDK Derivation Option</strong> &mdash; <code>Option A</code> by default.</li>
                <li><strong>Key Parity</strong> &mdash; <code>Odd</code> by default.</li>
            </ul>
            <p>Button: <strong>Generate UDK</strong>.</p>
        </section>

        <section class="doc-section" id="secure-msg">
            <h2>Secure Messaging</h2>
            <p>Issuer scripts reach the card after authorisation, and Secure Messaging is what protects them: SMI for integrity, SMC for confidentiality. Three tabs cover the sequence &mdash; derive the session keys, build the encrypted PIN block a PIN-change script carries, then MAC the command.</p>

            <div class="shot-grid">
                <figure class="shot-fig" data-shot="secure-msg" data-alt="MasterCard Secure Messaging on the Session Key tab with an MK input key type, MK-SMI and MK-SMC keys, the application cryptogram and a command number, beside the activity log" style="--shot-w:1512px">
                    <image-slot><img src="/images/docs/emv-tools/secure-messaging-session-key.png"
                                     alt="MasterCard Secure Messaging on the Session Key tab with an MK input key type, MK-SMI and MK-SMC keys, the application cryptogram and a command number, beside the activity log"
                                     width="3024" height="1842" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">emv-tools</span> MasterCard Secure Messaging</figcaption>
                </figure>
            </div>

            <h3>Session Key</h3>
            <ul>
                <li><strong>Input Key Type</strong> &mdash; <code>MK</code> to start from the master keys.</li>
                <li><strong>MK-SMI</strong> and <strong>MK-SMC</strong> &mdash; The integrity and confidentiality master keys, 32 characters each.</li>
                <li><strong>Application Cryptogram (AC)</strong> &mdash; Diversifies the session keys onto this transaction.</li>
                <li><strong>Command Number</strong> &mdash; Advances with each script command in the sequence.</li>
            </ul>
            <p>Button: <strong>Generate Session Keys</strong>.</p>

            <h3>PIN Block</h3>
            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="secure-msg-pin" data-alt="PIN Block Generation form with a Standard EMV PIN Block output format, an SK-ENC session key and a new PIN, above the Generate PIN Block button" style="--shot-w:747px">
                    <image-slot><img src="/images/docs/emv-tools/secure-messaging-pin-block.png"
                                     alt="PIN Block Generation form with a Standard EMV PIN Block output format, an SK-ENC session key and a new PIN, above the Generate PIN Block button"
                                     width="1494" height="816" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">emv-tools</span> PIN Block Generation</figcaption>
                </figure>
                <div class="shot-split-body">
    <ul>
        <li><strong>Output PIN Block Format</strong> &mdash; <code>Standard EMV PIN Block</code> by default.</li>
        <li><strong>SK-ENC</strong> &mdash; The encryption session key from the previous tab.</li>
        <li><strong>New PIN</strong> &mdash; The value the script will set.</li>
    </ul>
    <p>Button: <strong>Generate PIN Block</strong>.</p>
                </div>
            </div>

            <h3>MAC</h3>
            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="secure-msg-mac" data-alt="MAC Calculation form with an SK-MAC key, the APDU header fields Class, INS, P1, P2, Lc and Le, an ARC, an application cryptogram and a payload, above the Generate MAC button" style="--shot-w:748px">
                    <image-slot><img src="/images/docs/emv-tools/secure-messaging-mac.png"
                                     alt="MAC Calculation form with an SK-MAC key, the APDU header fields Class, INS, P1, P2, Lc and Le, an ARC, an application cryptogram and a payload, above the Generate MAC button"
                                     width="1496" height="1236" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">emv-tools</span> MAC Calculation</figcaption>
                </figure>
                <div class="shot-split-body">
    <p>The MAC covers the command as the card will see it, so the form takes the APDU apart rather than asking for one hex blob:</p>
    <ul>
        <li><strong>SK-MAC</strong> &mdash; The integrity session key.</li>
        <li><strong>Class</strong>, <strong>INS</strong>, <strong>P1</strong>, <strong>P2</strong>, <strong>Lc</strong>, <strong>Le</strong> &mdash; The APDU header and lengths.</li>
        <li><strong>ARC</strong> and <strong>AC</strong> &mdash; Authorisation response code and application cryptogram.</li>
        <li><strong>Payload</strong> &mdash; The script data itself.</li>
    </ul>
                </div>
            </div>
        </section>

        <section class="doc-section" id="cap">
            <h2>CAP Token Computation</h2>
            <p>Computes the Chip Authentication Programme token some banks ask for as an online-banking second factor &mdash; the number a customer reads off a handheld reader and types into the website.</p>

            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="cap" data-alt="CAP Token Computation form with IPB, IAF, PAN plus sequence number, CID, ATC, application cryptogram and issuer application data fields above the Generate Token button" style="--shot-w:751px">
                    <image-slot><img src="/images/docs/emv-tools/cap-token.png"
                                     alt="CAP Token Computation form with IPB, IAF, PAN plus sequence number, CID, ATC, application cryptogram and issuer application data fields above the Generate Token button"
                                     width="1502" height="1718" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">emv-tools</span> CAP Token Computation</figcaption>
                </figure>
                <div class="shot-split-body">
    <h3>Inputs</h3>
    <ul>
        <li><strong>IPB</strong> &mdash; Issuer Processing Base, the mask that selects which bits reach the token.</li>
        <li><strong>IAF</strong> &mdash; Issuer Action Format.</li>
        <li><strong>PAN + SN</strong> &mdash; PAN with its sequence number appended.</li>
        <li><strong>CID</strong> &mdash; Cryptogram Information Data.</li>
        <li><strong>ATC</strong> &mdash; Application Transaction Counter.</li>
        <li><strong>AC</strong> &mdash; The application cryptogram the card generated.</li>
        <li><strong>IAD</strong> &mdash; Issuer Application Data.</li>
    </ul>
    <p>Button: <strong>Generate Token</strong>.</p>
                </div>
            </div>
        </section>

        <section class="doc-section" id="hce">
            <h2>HCE (Host Card Emulation)</h2>
            <p>Host Card Emulation puts the card credential in a phone instead of a chip, so the key that signs a tap is short-lived by design. The Visa HCE calculator follows that chain across four tabs: card key, then the Limited-Use Key it produces, then the two contactless cryptograms an HCE wallet can present.</p>

            <div class="shot-grid">
                <figure class="shot-fig" data-shot="hce" data-alt="Visa HCE Crypto Calculator with UDK, LUK Key, MSD and qVSDC tabs, the UDK form filled in with a master key, PAN and PAN sequence number, beside the activity log" style="--shot-w:1512px">
                    <image-slot><img src="/images/docs/emv-tools/hce-udk.png"
                                     alt="Visa HCE Crypto Calculator with UDK, LUK Key, MSD and qVSDC tabs, the UDK form filled in with a master key, PAN and PAN sequence number, beside the activity log"
                                     width="3024" height="1842" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">emv-tools</span> Visa HCE Crypto Calculator</figcaption>
                </figure>
            </div>

            <h3>LUK Key</h3>
            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="hce-luk" data-alt="LUK Key Generation form with a UDK, current year, current hours and an hourly counter above the Generate LUK button" style="--shot-w:749px">
                    <image-slot><img src="/images/docs/emv-tools/hce-luk.png"
                                     alt="LUK Key Generation form with a UDK, current year, current hours and an hourly counter above the Generate LUK button"
                                     width="1498" height="1038" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">emv-tools</span> LUK Key Generation</figcaption>
                </figure>
                <div class="shot-split-body">
    <p>The Limited-Use Key is bound to a point in time, which is what limits it:</p>
    <ul>
        <li><strong>UDK</strong> &mdash; The card key from the first tab.</li>
        <li><strong>Current Year (YY)</strong> and <strong>Current Hours (HH)</strong> &mdash; The window the key belongs to.</li>
        <li><strong>Hourly Counter</strong> &mdash; Which key within that hour.</li>
    </ul>
    <p>Button: <strong>Generate LUK</strong>.</p>
                </div>
            </div>

            <h3>MSD</h3>
            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="hce-msd" data-alt="MSD Cryptogram form with a LUK_ATC value and an MSD device type selector above the Generate MSD Cryptogram button" style="--shot-w:751px">
                    <image-slot><img src="/images/docs/emv-tools/hce-msd.png"
                                     alt="MSD Cryptogram form with a LUK_ATC value and an MSD device type selector above the Generate MSD Cryptogram button"
                                     width="1502" height="636" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">emv-tools</span> MSD Cryptogram</figcaption>
                </figure>
                <div class="shot-split-body">
    <p>Magnetic Stripe Data mode &mdash; the legacy contactless path, where the phone presents a dynamic value in a magstripe-shaped message.</p>
    <ul>
        <li><strong>LUK_ATC</strong> &mdash; The limited-use key with its counter.</li>
        <li><strong>MSD Device Type</strong> &mdash; Drop-down.</li>
    </ul>
                </div>
            </div>

            <h3>qVSDC</h3>
            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="hce-qvsdc" data-alt="qVSDC Cryptogram form with the LUK and the transaction fields amount, amount other, country code, TVR, currency code, transaction date, transaction type, unpredictable number, AIP, ATC and CVR" style="--shot-w:749px">
                    <image-slot><img src="/images/docs/emv-tools/hce-qvsdc.png"
                                     alt="qVSDC Cryptogram form with the LUK and the transaction fields amount, amount other, country code, TVR, currency code, transaction date, transaction type, unpredictable number, AIP, ATC and CVR"
                                     width="1498" height="1578" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">emv-tools</span> qVSDC Cryptogram</figcaption>
                </figure>
                <div class="shot-split-body">
    <p>The full contactless cryptogram. Each field is labelled with the EMV tag it comes from, so a capture can be transcribed straight in:</p>
    <ul>
        <li><strong>LUK</strong></li>
        <li><strong>Amount (9F02)</strong>, <strong>Amount, Other (9F03)</strong></li>
        <li><strong>Country Code (9F1A)</strong>, <strong>Currency Code (5F2A)</strong></li>
        <li><strong>TVR (95)</strong>, <strong>AIP (82)</strong>, <strong>CVR (from 9F10)</strong></li>
        <li><strong>Transaction Date (9A)</strong>, <strong>Transaction Type (9C)</strong></li>
        <li><strong>Unpredictable No. (9F37)</strong>, <strong>ATC (9F36)</strong></li>
    </ul>
                </div>
            </div>
        </section>

        <section class="doc-section" id="tips">
            <h2>Tips</h2>
            <ul>
                <li>For SDA / DDA work, double-check the CA public key index in tag <code>8F</code> matches the CA you supply &mdash; mismatched indices is the most common SDA failure.</li>
                <li>Cryptogram versions vary by issuer, and the CVN is encoded inside the IAD (tag <code>9F10</code>) &mdash; read it before picking a calculator, because the derivation differs.</li>
                <li>Work the tabs left to right. Each one consumes what the previous produced, and the activity log keeps every intermediate key so you can restart mid-chain rather than from the master key.</li>
            </ul>
        </section>
    </main>

<aside class="pro-nudge"><span class="pn-tag">✦ Pro</span><p>Testing with a team, or certifying with a scheme? Pro raises the CPS ceiling, unlocks the full algorithm set and deep simulator tweaks, plus hosted endpoints and priority support.</p><a href="/pro">Register for Pro →</a></aside>`,
})
export class DocsEmvToolsPage {}
