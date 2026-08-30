import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { SitePage } from './site-page';
import { UiSection } from '../../ui/section';

@Component({
  selector: 'page-docs-utility-tools',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiSection],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-utility-tools' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<main class="doc-body">
        <div class="breadcrumb">
            <a href="/">Home</a>
            <span class="breadcrumb-sep">/</span>
            <a href="/docs">Documentation</a>
            <span class="breadcrumb-sep">/</span>
            <span>Data Converters</span>
        </div>

        <h1 class="page-title">Data Converters</h1>
        <p class="page-description">Format conversion and encoding utilities &mdash; Base64, Base94, BCD, character encoding, check digits and the Track 2 codec. Each one works in both directions, on the representations payment data actually arrives in.</p>

        <ui-section anchor="overview" heading="Introduction">
            <p>The converters live under <code>Tools &rarr; Data Converters</code> and handle format conversion and encoding: the representations payment data arrives in &mdash; Base64 on an API, BCD in an ISO 8583 field, packed hex in EMV tag <code>57</code> &mdash; and the check digits that guard them. Every tool is a two-direction pair: one card encodes, the other decodes.</p>

            <div class="shot-grid">
                <figure class="shot-fig wide" data-shot="converters-hub" data-alt="The Data Converters hub with cards for the Base64 Encoder, Base94 Encoder, BCD Converter, Character Encoder, Check Digit calculator and Track 2 Codec" style="--shot-w:1078px">
                    <image-slot><img src="/images/docs/data-converter/data-converters-hub.png"
                                     alt="The Data Converters hub with cards for the Base64 Encoder, Base94 Encoder, BCD Converter, Character Encoder, Check Digit calculator and Track 2 Codec"
                                     width="2156" height="646" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">utility-tools</span> Data Converters hub</figcaption>
                </figure>
            </div>
        </ui-section>

        <ui-section anchor="all-tools" heading="All tools">
            <p>Every converter in this category &mdash; each card links to the detailed reference below.</p>
            <div class="hub-grid">
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">Base64 Encoder</div>
                        <p class="hub-desc">Encode bytes to Base64 or decode a Base64 string back to hex.</p>
                        <a class="hub-link" href="/tools/utility-tools#base64">View details &rarr;</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">Base94 Encoder</div>
                        <p class="hub-desc">The denser printable-ASCII variant, encoded and decoded the same way.</p>
                        <a class="hub-link" href="/tools/utility-tools#base94">View details &rarr;</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">BCD Converter</div>
                        <p class="hub-desc">Pack a decimal string two digits per byte, or unpack one.</p>
                        <a class="hub-link" href="/tools/utility-tools#bcd">View details &rarr;</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">Character Encoder</div>
                        <p class="hub-desc">Convert between binary, hexadecimal, decimal and ASCII.</p>
                        <a class="hub-link" href="/tools/utility-tools#encoding">View details &rarr;</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">Check Digit Calculator</div>
                        <p class="hub-desc">Generate or validate a Luhn (Mod 10) check digit.</p>
                        <a class="hub-link" href="/tools/utility-tools#check-digit">View details &rarr;</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">Track 2 Codec</div>
                        <p class="hub-desc">Build Track 2 from its parts, or decode any of its three shapes.</p>
                        <a class="hub-link" href="/tools/utility-tools#track2">View details &rarr;</a>
                    </div>
                </div>
            </div>
        </ui-section>

        <ui-section anchor="base64" heading="Base64 Encoder">
            <p>Base64 turns arbitrary bytes into a text-safe alphabet &mdash; the form payment data usually takes when it crosses a JSON or XML boundary. Encode and decode sit side by side.</p>

            <div class="shot-grid shot-row shot-even" style="--row-w:936px">
                <figure class="shot-fig" data-shot="base64-encode" data-alt="Encode to Base64 form with an ASCII input encoding drop-down and an Input Data field above the Encode button" style="--shot-w:750px;--ar:2.315">
                    <image-slot><img src="/images/docs/data-converter/base64-encode.png"
                                     alt="Encode to Base64 form with an ASCII input encoding drop-down and an Input Data field above the Encode button"
                                     width="1500" height="648" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">utility-tools</span> Encode to Base64</figcaption>
                </figure>
                <figure class="shot-fig" data-shot="base64-decode" data-alt="Decode from Base64 form with a single Base64 Data field above the Decode button" style="--shot-w:748px;--ar:3.028">
                    <image-slot><img src="/images/docs/data-converter/base64-decode.png"
                                     alt="Decode from Base64 form with a single Base64 Data field above the Decode button"
                                     width="1496" height="494" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">utility-tools</span> Decode from Base64</figcaption>
                </figure>
            </div>

            <h3>Inputs</h3>
            <ul>
                <li><strong>Input Encoding</strong> &mdash; Drop-down: <code>ASCII</code> or hexadecimal. It tells the encoder how to read what you paste, so a hex key and a plain string both work.</li>
                <li><strong>Input Data</strong> &mdash; The bytes to encode, in the format above.</li>
                <li><strong>Base64 Data</strong> &mdash; On the decode side; the result comes back as hexadecimal.</li>
            </ul>
        </ui-section>

        <ui-section anchor="base94" heading="Base94 Encoder">
            <p>Base94 packs bytes into the full printable ASCII range, so it fits more data into the same number of characters than Base64. Some key-injection and terminal-management protocols use it for exactly that reason.</p>

            <div class="shot-grid shot-row shot-even" style="--row-w:936px">
                <figure class="shot-fig" data-shot="base94-encode" data-alt="Encode to Base94 form with an ASCII input encoding drop-down and an Input Data field above the Encode button" style="--shot-w:745px;--ar:2.328">
                    <image-slot><img src="/images/docs/data-converter/base94-encode.png"
                                     alt="Encode to Base94 form with an ASCII input encoding drop-down and an Input Data field above the Encode button"
                                     width="1490" height="640" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">utility-tools</span> Encode to Base94</figcaption>
                </figure>
                <figure class="shot-fig" data-shot="base94-decode" data-alt="Decode from Base94 form with a single Base94 Data field above the Decode button" style="--shot-w:751px;--ar:3.065">
                    <image-slot><img src="/images/docs/data-converter/base94-decode.png"
                                     alt="Decode from Base94 form with a single Base94 Data field above the Decode button"
                                     width="1502" height="490" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">utility-tools</span> Decode from Base94</figcaption>
                </figure>
            </div>

            <p>The fields mirror the Base64 tool exactly &mdash; an <strong>Input Encoding</strong> drop-down and <strong>Input Data</strong> going in, <strong>Base94 Data</strong> coming back, decoded to hexadecimal.</p>
        </ui-section>

        <ui-section anchor="bcd" heading="BCD Converter">
            <p>Binary Coded Decimal stores two digits per byte, which is how numeric ISO 8583 fields and EMV amounts are carried on the wire. The converter goes both ways between a decimal string and its packed form.</p>

            <div class="shot-grid shot-row shot-even" style="--row-w:936px">
                <figure class="shot-fig" data-shot="bcd-encode" data-alt="Encode to BCD form with a Decimal Data field holding 1234567890 above the Encode button" style="--shot-w:750px;--ar:3.275">
                    <image-slot><img src="/images/docs/data-converter/bcd-encode.png"
                                     alt="Encode to BCD form with a Decimal Data field holding 1234567890 above the Encode button"
                                     width="1500" height="458" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">utility-tools</span> Encode to BCD</figcaption>
                </figure>
                <figure class="shot-fig" data-shot="bcd-decode" data-alt="Decode from BCD form with a Hexadecimal input format drop-down and a BCD Data field holding 1234567890 above the Decode button" style="--shot-w:750px;--ar:2.475">
                    <image-slot><img src="/images/docs/data-converter/bcd-decode.png"
                                     alt="Decode from BCD form with a Hexadecimal input format drop-down and a BCD Data field holding 1234567890 above the Decode button"
                                     width="1500" height="606" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">utility-tools</span> Decode from BCD</figcaption>
                </figure>
            </div>

            <h3>Inputs</h3>
            <ul>
                <li><strong>Decimal Data</strong> &mdash; Digits to pack. An odd digit count is padded to a whole byte.</li>
                <li><strong>Input Format</strong> &mdash; On the decode side: <code>Hexadecimal</code> by default.</li>
                <li><strong>BCD Data</strong> &mdash; The packed value to unpack.</li>
            </ul>
        </ui-section>

        <ui-section anchor="encoding" heading="Character Encoder">
            <p>A general conversion bench for the representations that are not a payment format in their own right &mdash; binary, hexadecimal, decimal and ASCII. One drop-down picks the direction.</p>

            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="encoding-converter" data-alt="Encoding Converter with a Conversion Type drop-down set to Binary to Hexadecimal and an Input Data field above the Convert button" style="--shot-w:717px">
                    <image-slot><img src="/images/docs/data-converter/encoding-converter.png"
                                     alt="Encoding Converter with a Conversion Type drop-down set to Binary to Hexadecimal and an Input Data field above the Convert button"
                                     width="1434" height="606" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">utility-tools</span> Encoding Converter</figcaption>
                </figure>
                <div class="shot-split-body">
    <h3>Inputs</h3>
    <ul>
        <li><strong>Conversion Type</strong> &mdash; Drop-down naming both ends of the conversion, e.g. <code>Binary -&gt; Hexadecimal</code>. Pick the pair and the tool applies it in that direction.</li>
        <li><strong>Input Data</strong> &mdash; The value in the source format.</li>
    </ul>
    <p>Button: <strong>Convert</strong>.</p>
                </div>
            </div>
        </ui-section>

        <ui-section anchor="check-digit" heading="Check Digit Calculator">
            <p>The trailing digit on a PAN is a Luhn checksum, and a wrong one is rejected before any cryptography runs. This tool computes it, or checks the one you already have.</p>

            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="check-digit" data-alt="Check Digit Calculator with an Input Number of 49927398716, the Luhn (Mod 10) algorithm selected, and Validate and Generate buttons" style="--shot-w:719px">
                    <image-slot><img src="/images/docs/data-converter/check-digit.png"
                                     alt="Check Digit Calculator with an Input Number of 49927398716, the Luhn (Mod 10) algorithm selected, and Validate and Generate buttons"
                                     width="1438" height="580" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">utility-tools</span> Check Digit Calculator</figcaption>
                </figure>
                <div class="shot-split-body">
    <h3>Inputs</h3>
    <ul>
        <li><strong>Input Number</strong> &mdash; The digits to check. For validation, include the check digit; to generate one, leave it off.</li>
        <li><strong>Algorithm</strong> &mdash; Drop-down; <code>Luhn (Mod 10)</code> is the default and the one card numbers use.</li>
    </ul>
    <p>Buttons: <strong>Validate</strong>, <strong>Generate</strong>.</p>
                </div>
            </div>
        </ui-section>

        <ui-section anchor="track2" heading="Track 2 Codec">
            <p>Track 2 is the magstripe-equivalent record that also travels in EMV tag <code>57</code>, and it appears in at least three shapes depending on where you captured it. The codec builds one from its parts, or takes any of those shapes apart.</p>

            <h3>Encode</h3>
            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="track2-encode" data-alt="Encode Track 2 form with PAN, expiry YYMM, service code and discretionary data fields, a BCD/Hex (EMV Tag 57) output format, and an Encode button" style="--shot-w:754px">
                    <image-slot><img src="/images/docs/data-converter/track2-encode.png"
                                     alt="Encode Track 2 form with PAN, expiry YYMM, service code and discretionary data fields, a BCD/Hex (EMV Tag 57) output format, and an Encode button"
                                     width="1508" height="1080" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">utility-tools</span> Encode Track 2</figcaption>
                </figure>
                <div class="shot-split-body">
    <h3>Inputs</h3>
    <ul>
        <li><strong>PAN</strong> &mdash; 8&ndash;19 digits.</li>
        <li><strong>Expiry YYMM</strong> &mdash; 4 digits.</li>
        <li><strong>Service Code</strong> &mdash; 3 digits.</li>
        <li><strong>Discretionary Data</strong> &mdash; Digits; optional.</li>
        <li><strong>Output Format</strong> &mdash; Drop-down; <code>BCD/Hex (EMV Tag 57)</code> for chip data, or the ASCII magstripe forms.</li>
    </ul>
                </div>
            </div>

            <h3>Decode</h3>
            <div class="shot-split" style="--fig-col:430px">
                <figure class="shot-fig" data-shot="track2-decode" data-alt="Decode Track 2 form with a Track 2 Data field that accepts any format, worked examples of the three accepted shapes, and a Decode button" style="--shot-w:746px">
                    <image-slot><img src="/images/docs/data-converter/track2-decode.png"
                                     alt="Decode Track 2 form with a Track 2 Data field that accepts any format, worked examples of the three accepted shapes, and a Decode button"
                                     width="1492" height="660" loading="lazy"></image-slot>
                    <figcaption class="shot-cap"><span class="mono">utility-tools</span> Decode Track 2</figcaption>
                </figure>
                <div class="shot-split-body">
    <p>Paste the record in <strong>any</strong> of its shapes &mdash; the tool detects which one it is rather than asking you. The form lists all three:</p>
    <ul>
        <li><strong>ASCII raw</strong> &mdash; with the <code>;</code> start sentinel and <code>?</code> end sentinel.</li>
        <li><strong>ASCII, no sentinels</strong> &mdash; PAN, <code>=</code>, then the rest.</li>
        <li><strong>BCD / hex (Tag 57)</strong> &mdash; packed, with <code>D</code> as the field separator and a trailing <code>F</code> pad.</li>
    </ul>
                </div>
            </div>
        </ui-section>

        <ui-section anchor="tips" heading="Tips">
            <ul>
                <li>When a field will not parse, check its representation before its value &mdash; a PAN that looks wrong is often BCD read as ASCII, or the other way round.</li>
                <li>Round-trip anything you are unsure of: encode, then decode the result. If you do not land back on the input, the format assumption is what is wrong.</li>
                <li>The Track 2 decoder accepts all three shapes, so paste a capture straight in rather than converting it by hand first.</li>
                <li>The activity logs in each tool persist until you clear them &mdash; useful for capturing a sequence of intermediate values to share with a vendor support ticket.</li>
            </ul>
        </ui-section>
    </main>

<aside class="pro-nudge"><span class="pn-tag">✦ Pro</span><p>Testing with a team, or certifying with a scheme? Pro raises the CPS ceiling, unlocks the full algorithm set and deep simulator tweaks, plus hosted endpoints and priority support.</p><a href="/pro">Register for Pro →</a></aside>`,
})
export class DocsUtilityToolsPage {}
