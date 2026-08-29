import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { SitePage } from './site-page';

@Component({
  selector: 'page-docs-card-validation',
  changeDetection: ChangeDetectionStrategy.OnPush,
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-card-validation' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<main class="doc-body">
        <div class="breadcrumb">
            <a href="/">Home</a>
            <span class="breadcrumb-sep">/</span>
            <a href="/docs">Documentation</a>
            <span class="breadcrumb-sep">/</span>
            <span>Card Validation</span>
        </div>

        <h1 class="page-title">Card Validation Tools</h1>
        <p class="page-description">Generate and validate the card security codes used by MasterCard and American Express &mdash; dynamic CVC3 for contactless taps, and the Amex CSC in its 3, 4 and 5-digit forms.</p>

        <section class="doc-section" id="overview">
            <h2>Introduction</h2>
            <p>Card security codes are short numeric values printed on a card or computed dynamically that prove the cardholder physically possesses the card or a valid token. They&rsquo;re the first line of defence in card-not-present transactions and are checked on every authorisation alongside expiry date and address verification.</p>

            <p>ISO8583Studio includes dedicated calculators per scheme so you can simulate issuer behaviour without an HSM and validate codes returned by your authorisation pipeline.</p>
        </section>

        <section class="doc-section" id="concepts">
            <h2>Key Concepts</h2>
            <div class="table-wrapper">
                <table>
                    <thead><tr><th>Code</th><th>Scheme</th><th>Where it lives</th><th>Length</th></tr></thead>
                    <tbody>
                        <tr><td><strong>CVV</strong></td><td>Visa</td><td>Magstripe (track 1 / 2)</td><td>3 digits</td></tr>
                        <tr><td><strong>CVV2</strong></td><td>Visa</td><td>Card back (signature panel)</td><td>3 digits</td></tr>
                        <tr><td><strong>iCVV</strong></td><td>Visa</td><td>EMV chip</td><td>3 digits (computed with service code 999)</td></tr>
                        <tr><td><strong>CVC</strong></td><td>MasterCard</td><td>Magstripe</td><td>3 digits</td></tr>
                        <tr><td><strong>CVC2</strong></td><td>MasterCard</td><td>Card back</td><td>3 digits</td></tr>
                        <tr><td><strong>CVC3</strong></td><td>MasterCard PayPass</td><td>Computed dynamically per tap</td><td>3 digits</td></tr>
                        <tr><td><strong>CSC</strong></td><td>American Express</td><td>Card front</td><td>4 digits</td></tr>
                    </tbody>
                </table>
            </div>
        </section>

        <section class="doc-section" id="cvc-mc">
            <h2>MasterCard CVC3 Calculator</h2>
            <p>CVC3 is the dynamic card verification code a contactless (PayPass) card computes for every tap, from a master key, the terminal&rsquo;s unpredictable number and the Application Transaction Counter. The calculator has a <strong>Generate</strong> tab and a <strong>Validate</strong> tab, with the activity log beside them.</p>

            <div class="shot-grid">
                <figure class="shot-fig" data-shot="cvc3-generate" data-alt="MasterCard CVC3 calculator on the Generate tab with IMK, PAN, PAN Seq No, Track 1/2 Data, Unpredictable Num and ATC fields and Dynamic CVC3 selected as the CVC3 type, beside the activity log" style="--shot-w:1512px">
                    <image-slot><img src="/images/docs/payment-utilities/mastercard-cvc3-generate.png"
                                     alt="MasterCard CVC3 calculator on the Generate tab with IMK, PAN, PAN Seq No, Track 1/2 Data, Unpredictable Num and ATC fields and Dynamic CVC3 selected as the CVC3 type, beside the activity log"
                                     width="3024" height="1964" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">card-validation</span> Generate CVC3</figcaption>
                </figure>
            </div>

            <h3>Inputs</h3>
            <ul>
                <li><strong>IMK</strong> &mdash; The issuer master key the card&rsquo;s CVC3 key is derived from.</li>
                <li><strong>PAN</strong> and <strong>PAN Seq No</strong> &mdash; Together they identify the individual card, which is what the derivation is bound to.</li>
                <li><strong>Track 1/2 Data</strong> &mdash; The track template the CVC3 digits are placed into.</li>
                <li><strong>Unpredictable Num</strong> &mdash; The number supplied by the terminal for this tap.</li>
                <li><strong>ATC</strong> &mdash; Application Transaction Counter, which advances every tap.</li>
                <li><strong>CVC3 Type</strong> &mdash; Drop-down; <code>Dynamic CVC3</code> is the default.</li>
            </ul>
            <p>Button: <strong>Generate</strong>.</p>

            <h3>Validate</h3>
            <p>The <strong>Validate</strong> tab takes the same card and transaction inputs plus the <strong>Dynamic CVC3</strong> value that arrived, recomputes it, and reports whether the two agree.</p>

            <div class="shot-grid">
                <figure class="shot-fig" data-shot="cvc3-validate" data-alt="MasterCard CVC3 calculator on the Validate tab, with the same card and transaction fields plus a Dynamic CVC3 value field to check, beside the activity log" style="--shot-w:1512px">
                    <image-slot><img src="/images/docs/payment-utilities/mastercard-cvc3-validate.png"
                                     alt="MasterCard CVC3 calculator on the Validate tab, with the same card and transaction fields plus a Dynamic CVC3 value field to check, beside the activity log"
                                     width="3024" height="1964" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">card-validation</span> Validate CVC3</figcaption>
                </figure>
            </div>

            <div class="info-card note">
                <div class="info-card-title">CVC3 placement</div>
                <p>The CVC3 digits replace discretionary data positions in the magstripe-equivalent track the contactless card emits. Combined with the ATC, every tap produces a different track 2.</p>
            </div>
        </section>

        <section class="doc-section" id="amex">
            <h2>AMEX CSC Calculator</h2>
            <p>American Express uses a Card Security Code (CSC) printed on the card front above the embossed PAN. The calculator generates one from the card data, or checks a presented value.</p>

            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="amex-generate" data-alt="Amex CSC Calculator on the Generate tab with a CSC Version drop-down set to Version 1, a hex CSC Key field, PAN, expiration date, service code and a CSC verification value type above the Generate button" style="--shot-w:758px">
                    <image-slot><img src="/images/docs/payment-utilities/amex-csc-generate.png"
                                     alt="Amex CSC Calculator on the Generate tab with a CSC Version drop-down set to Version 1, a hex CSC Key field, PAN, expiration date, service code and a CSC verification value type above the Generate button"
                                     width="1516" height="1632" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">card-validation</span> Generate CSC</figcaption>
                </figure>
                <div class="shot-split-body">
                    <h3>Inputs</h3>
                    <ul>
                        <li><strong>CSC Version</strong> &mdash; Drop-down; <code>Version 1</code> is the default.</li>
                        <li><strong>CSC Key (Hex)</strong> &mdash; The key the code is computed under.</li>
                        <li><strong>PAN</strong> &mdash; 15 digits for Amex.</li>
                        <li><strong>Expiration Date (YYMM)</strong></li>
                        <li><strong>Service Code</strong></li>
                        <li><strong>Verification Value Type</strong> &mdash; Drop-down; <code>CSC</code> by default.</li>
                    </ul>
                    <p>Button: <strong>Generate</strong>. Every field is validated before the button becomes active, so an empty form shows its own reasons.</p>
                </div>
            </div>

            <h3>Validate</h3>
            <p>The <strong>Validate</strong> tab drops the key and asks instead for the values that arrived. Under <em>Values to Validate</em> it takes <strong>CSC-5</strong>, <strong>CSC-4</strong> and <strong>CSC-3</strong> &mdash; the three code lengths Amex uses across magstripe, card-front and contactless flows &mdash; and reports each against the recomputed value in the log.</p>

            <div class="shot-grid">
                <figure class="shot-fig" data-shot="amex-validate" data-alt="Amex CSC Calculator on the Validate tab with PAN, expiration date, service code and verification value type, then CSC-5, CSC-4 and CSC-3 fields under a Values to Validate heading, beside the activity log" style="--shot-w:1512px">
                    <image-slot><img src="/images/docs/payment-utilities/amex-csc-validate.png"
                                     alt="Amex CSC Calculator on the Validate tab with PAN, expiration date, service code and verification value type, then CSC-5, CSC-4 and CSC-3 fields under a Values to Validate heading, beside the activity log"
                                     width="3024" height="1848" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">card-validation</span> Validate CSC</figcaption>
                </figure>
            </div>
        </section>

        <section class="doc-section" id="service-codes">
            <h2>Service Codes</h2>
            <p>The 3-digit service code is fed into CVV/CVC algorithms. Each digit has independent meaning:</p>

            <div class="table-wrapper">
                <table>
                    <thead><tr><th>Position</th><th>Common Values</th><th>Meaning</th></tr></thead>
                    <tbody>
                        <tr><td>1st digit</td><td>1, 2, 5, 6, 7, 9</td><td>Interchange and technology (international, EMV, etc.).</td></tr>
                        <tr><td>2nd digit</td><td>0, 2, 4</td><td>Authorisation processing (online, offline, by issuer).</td></tr>
                        <tr><td>3rd digit</td><td>0&ndash;7</td><td>Range of services and PIN requirement.</td></tr>
                    </tbody>
                </table>
            </div>

            <ul>
                <li><strong>101</strong> &mdash; International, normal authorisation, no restrictions.</li>
                <li><strong>201</strong> &mdash; Same but EMV-capable.</li>
                <li><strong>120</strong> &mdash; Online authorisation only, PIN required.</li>
                <li><strong>999</strong> &mdash; Special value used by Visa for iCVV computation.</li>
                <li><strong>000</strong> &mdash; Special value for CVV2 / CVC2.</li>
            </ul>
        </section>

        <section class="doc-section" id="tips">
            <h2>Tips</h2>
            <ul>
                <li>Use the same CVK / CVC key for CVV, CVV2, and iCVV &mdash; only the service code changes.</li>
                <li>If your CVV2 doesn&rsquo;t match across systems, check whether the integrator strips the trailing PAN check digit before computation. Different specs handle that differently.</li>
                <li>For CVC3 testing, capture the UN and ATC from your terminal log alongside the track data &mdash; off-by-one ATC is a common error.</li>
            </ul>
        </section>
    </main>`,
})
export class DocsCardValidationPage {}
