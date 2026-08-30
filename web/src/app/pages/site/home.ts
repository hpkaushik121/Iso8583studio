import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { SitePage } from './site-page';

@Component({
  selector: 'page-home',
  changeDetection: ChangeDetectionStrategy.OnPush,
  hostDirectives: [SitePage],
  host: { class: 'static-page page-home' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<section class="hero" data-sect="hero">
    <div class="hero-top">
        <span class="hero-badge"><i></i>NINE SIMULATORS · ALL RUNNING</span>
        <h1>Every field. Every bit.<br><span class="gr">Nothing hidden.</span></h1>
        <p class="hero-sub">The payment engineer's workbench: <b>develop, test and certify</b> ISO 8583 integrations without a production host, HSM or card in sight. <b>Nine simulators</b> stand in for every party on the network, and <b>64 tools</b> handle the crypto, keys and parsing in between — free, open source, on your desktop.</p>
        <div class="hero-ctas">
            <a class="btn btn-blue btn-lg" href="https://github.com/hpkaushik121/Iso8583studio/releases/latest"><svg width="15" height="15" viewBox="0 0 24 24" fill="none"><path d="M12 3v12m0 0l-4.5-4.5M12 15l4.5-4.5M4 19h16" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>Download Studio</a>
            <a class="btn btn-ghost btn-lg" href="/#toolbox">Explore 64 tools</a>
        </div>
        <div class="hero-meta"><span>◆ Windows</span><span>◆ macOS</span><span>◆ Linux</span><span>◆ Apache · open source</span></div>
    </div>
    <div class="monitor-wrap">
        <span class="con-float" style="top:-13px;left:14px;animation-delay:.3s">SERVER · 0.0.0.0:8583</span>
        <span class="con-float t" style="bottom:-13px;right:22px;animation-delay:1.6s">ISO 9797-1 MAC ✓</span>
        <div class="simcon">
            <div class="sim-head">
                <div class="con-dots"><i></i><i></i><i></i></div>
                <div class="ttl"><b>ISO8583Studio</b> · Simulators</div>
                <div class="sim-run"><i></i>9 RUNNING</div>
            </div>
            <div class="simgrid" id="simGrid">
                <div class="simtile">
                    <div class="st-top"><span class="st-ic"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="5" y="2" width="14" height="20" rx="2.5"/><rect x="8" y="5" width="8" height="4.5" rx="1"/><path d="M8.5 13h.01M12 13h.01M15.5 13h.01M8.5 16h.01M12 16h.01M15.5 16h.01"/></svg></span><div><div class="st-name">POS Simulator</div><div class="st-sub">EMV · entry 051</div></div><span class="st-led"></span></div>
                    <div class="st-bot"><div class="st-meter"></div><span class="st-metric">chip · 051</span></div>
                </div>
                <div class="simtile">
                    <div class="st-top"><span class="st-ic"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="2" y="5" width="20" height="14" rx="2.5"/><rect x="5" y="9" width="5" height="5" rx="1"/><path d="M14 10h5M14 14h5"/></svg></span><div><div class="st-name">APDU Simulator</div><div class="st-sub">smart card · TLV</div></div><span class="st-led"></span></div>
                    <div class="st-bot"><div class="st-meter"></div><span class="st-metric">TLV</span></div>
                </div>
                <div class="simtile">
                    <div class="st-top"><span class="st-ic"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M6 3h12v18l-2-1.4-2 1.4-2-1.4-2 1.4-2-1.4L6 21z"/><path d="M9 8h6M9 12h6"/></svg></span><div><div class="st-name">ECR Simulator</div><div class="st-sub">cash register · RS232</div></div><span class="st-led"></span></div>
                    <div class="st-bot"><div class="st-meter"></div><span class="st-metric">232</span></div>
                </div>
                <div class="simtile">
                    <div class="st-top"><span class="st-ic"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="2" y="8" width="20" height="8" rx="2"/><path d="M6 12h.01M9 12h.01"/><path d="M16 12h4M16 9v6"/></svg></span><div><div class="st-name">Switch Simulator</div><div class="st-sub">routing · F32</div></div><span class="st-led"></span></div>
                    <div class="st-bot"><div class="st-meter"></div><span class="st-metric">route</span></div>
                </div>
                <div class="simtile">
                    <div class="st-top"><span class="st-ic"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="3" y="4" width="18" height="6" rx="1.5"/><rect x="3" y="14" width="18" height="6" rx="1.5"/><path d="M7 7h.01M7 17h.01"/></svg></span><div><div class="st-name">Host Simulator</div><div class="st-sub">TCP/IP · :8583</div></div><span class="st-led"></span></div>
                    <div class="st-bot"><div class="st-meter"></div><span class="st-metric">1,284 txns</span></div>
                </div>
                <div class="simtile">
                    <div class="st-top"><span class="st-ic"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3c2.5 2.5 2.5 15 0 18M12 3c-2.5 2.5-2.5 15 0 18"/></svg></span><div><div class="st-name">Scheme Simulator</div><div class="st-sub">auth → settle</div></div><span class="st-led"></span></div>
                    <div class="st-bot"><div class="st-meter"></div><span class="st-metric">0210</span></div>
                </div>
                <div class="simtile">
                    <div class="st-top"><span class="st-ic"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="4" y="9" width="16" height="11" rx="2"/><path d="M8 9V6a4 4 0 018 0v3"/><path d="M12 13v3"/></svg></span><div><div class="st-name">HSM Simulator</div><div class="st-sub">payShield 10K · :1500</div></div><span class="st-led"></span></div>
                    <div class="st-bot"><div class="st-meter"></div><span class="st-metric">35 cmds</span></div>
                </div>
                <div class="simtile">
                    <div class="st-top"><span class="st-ic"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="M6 9l3 3-3 3M12 15h5"/></svg></span><div><div class="st-name">HSM Command Console</div><div class="st-sub">interactive · console</div></div><span class="st-led"></span></div>
                    <div class="st-bot"><div class="st-meter"></div><span class="st-metric">GC · A0</span></div>
                </div>
                <div class="simtile">
                    <div class="st-top"><span class="st-ic"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="4" y="3" width="16" height="18" rx="2"/><rect x="7" y="6" width="10" height="6" rx="1"/><path d="M8 16h8"/></svg></span><div><div class="st-name">ATM Simulator</div><div class="st-sub">NDC / DDC</div></div><span class="st-led"></span></div>
                    <div class="st-bot"><div class="st-meter"></div><span class="st-metric">NDC</span></div>
                </div>
            </div>
            <div class="sim-data">
                <span class="proto iso" id="simProto">ISO 8583</span>
                <span class="pkt-name" id="simActive">Host Simulator</span>
                <div class="data-chips" id="simData"></div>
                <span class="rcpill ok" id="simPill">00</span>
            </div>
            <div class="sim-foot">
                <span>TCP/IP · RS232 · REST · dial-up</span>
                <span class="res">ISO 8583 · APDU · payShield 10K · NDC/DDC</span>
            </div>
        </div>
    </div>
</section>

<!-- 2D transaction path -->
<section class="solid flow sec-pad" data-sect="transaction_path">
    <div class="wrap">
        <span class="kicker">What ISO8583Studio does</span>
        <h2 class="sec">Model every hop of a transaction</h2>
        <p class="sec-sub">From the chip on the card to the issuer's authorization host — spin up a simulator for every stage and watch the data packets flow. Scroll the rail to walk the path.</p>
        <div class="rail-wrap">
            <div class="rail" id="flowRail">
                <div class="rail-track">
                    <div class="rail-line"></div>
                    <a class="node" href="/simulator/apdu">
                        <div class="disc"><span class="n">1</span><svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="2" y="5" width="20" height="14" rx="2.5"/><rect x="5" y="9" width="5" height="4" rx="1"/><path d="M14 9h5M14 13h5"/></svg></div>
                        <h3>Card &amp; EMV</h3><p>Cryptograms, SDA/DDA, ATR &amp; tag parsing.</p>
                        <div class="simchip"><span class="s">APDU Simulator</span><span class="pk">C-APDU · TLV</span></div>
                    </a>
                    <a class="node" href="/simulator/pos">
                        <div class="disc"><span class="n">2</span><svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="5" y="2" width="14" height="20" rx="2.5"/><rect x="8" y="5" width="8" height="5" rx="1"/><path d="M8.5 13h.01M12 13h.01M15.5 13h.01M8.5 16h.01M12 16h.01M15.5 16h.01"/></svg></div>
                        <h3>Terminal</h3><p>POS, ECR &amp; APDU acceptance flows.</p>
                        <div class="simchip"><span class="s">POS Simulator</span><span class="pk">EMV 9F02 · ARQC</span></div>
                    </a>
                    <a class="node" href="/simulator/hsm">
                        <div class="disc"><span class="n">3</span><svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="4" y="9" width="16" height="11" rx="2"/><path d="M8 9V6a4 4 0 018 0v3"/><path d="M12 13v3"/></svg></div>
                        <h3>HSM &amp; keys</h3><p>PIN, MAC, DUKPT &amp; TR-31.</p>
                        <div class="simchip"><span class="s">HSM Simulator</span><span class="pk">payShield M4 · CW</span></div>
                    </a>
                    <a class="node" href="/simulator/host">
                        <div class="disc"><span class="n">4</span><svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="2" y="8" width="20" height="8" rx="2"/><path d="M6 12h.01M10 12h.01"/><path d="M17 12h3M17 9v6"/></svg></div>
                        <h3>Switch &amp; host</h3><p>Server, client or proxy routing.</p>
                        <div class="simchip"><span class="s">Host Simulator</span><span class="pk">ISO 8583 · 0200</span></div>
                    </a>
                    <a class="node" href="/cloud-simulators">
                        <div class="disc"><span class="n">5</span><svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3c2.5 2.5 2.5 15 0 18M12 3c-2.5 2.5-2.5 15 0 18"/></svg></div>
                        <h3>Scheme</h3><p>Network authorization &amp; clearing.</p>
                        <div class="simchip"><span class="s">Scheme Simulator</span><span class="pk">Scheme · 0100→0110</span></div>
                    </a>
                    <a class="node" href="/simulator/issuer">
                        <div class="disc"><span class="n">6</span><svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M3 9l9-5 9 5"/><path d="M4 9v10M9 9v10M15 9v10M20 9v10"/><path d="M3 20h18"/></svg></div>
                        <h3>Issuer</h3><p>Authorization, PIN &amp; stand-in decisioning.</p>
                        <div class="simchip"><span class="s">Issuer Host</span><span class="pk">ISO 8583 · 0210 · F39</span></div>
                    </a>
                </div>
            </div>
        </div>
    </div>
</section>

<!-- toolbox -->
<section class="solid sec-pad" id="toolbox" data-sect="toolbox">
    <div class="wrap">
        <span class="kicker">The full toolbox</span>
        <h2 class="sec">64 tools, six disciplines</h2>
        <p class="sec-sub">The same categories as the studio sidebar — every tool documented, every workflow covered.</p>
        <div class="cat-grid">
            <a class="cat reveal" href="/simulator/host"><span class="arrow">→</span>
                <div class="cat-top"><span class="cat-ic">⇄</span><span class="cnt">9 tools</span></div>
                <h3>Payment Simulators</h3><p>Host, HSM, POS, ATM, ECR, switch &amp; scheme over TCP/IP, RS232 and REST.</p>
                <div class="chips"><span class="chip">Host</span><span class="chip">HSM</span><span class="chip">POS</span><span class="chip">APDU</span></div>
            </a>
            <a class="cat reveal" href="/tools/emv-tools"><span class="arrow">→</span>
                <div class="cat-top"><span class="cat-ic">▣</span><span class="cnt">12 tools</span></div>
                <h3>EMV &amp; Card Tools</h3><p>ARQC/TC validation, SDA &amp; DDA, ATR parsing, tag decoding, CVV/CVC3.</p>
                <div class="chips"><span class="chip">EMV 4.1</span><span class="chip">SDA/DDA</span><span class="chip">Tags</span><span class="chip">CVV</span></div>
            </a>
            <a class="cat reveal" href="/tools/cipher-tools"><span class="arrow">→</span>
                <div class="cat-top"><span class="cat-ic">⬡</span><span class="cnt">7 tools</span></div>
                <h3>Cryptographic Tools</h3><p>AES, DES/3DES, RSA, format-preserving encryption, MD5/SHA hashing.</p>
                <div class="chips"><span class="chip">AES</span><span class="chip">3DES</span><span class="chip">RSA</span><span class="chip">FPE</span></div>
            </a>
            <a class="cat reveal" href="/tools/key-tools"><span class="arrow">→</span>
                <div class="cat-top"><span class="cat-ic">⚿</span><span class="cnt">10 tools</span></div>
                <h3>Key Management</h3><p>DUKPT derivation, TR-31 key blocks, shares, KCVs, Thales &amp; Futurex.</p>
                <div class="chips"><span class="chip">DUKPT</span><span class="chip">TR-31</span><span class="chip">Thales</span><span class="chip">Futurex</span></div>
            </a>
            <a class="cat reveal" href="/tools/pin-tools"><span class="arrow">→</span>
                <div class="cat-top"><span class="cat-ic">▤</span><span class="cnt">21 tools</span></div>
                <h3>Payment Utilities</h3><p>PIN blocks (ISO 9564 and OEM), AES PIN blocks, PIN translation, DUKPT PIN.</p>
                <div class="chips"><span class="chip">PIN</span><span class="chip">ISO 9564</span><span class="chip">AES</span><span class="chip">DUKPT</span></div>
            </a>
            <a class="cat reveal" href="/tools/utility-tools"><span class="arrow">→</span>
                <div class="cat-top"><span class="cat-ic">⇋</span><span class="cnt">6 tools</span></div>
                <h3>Data Converters</h3><p>Base64, Base94, BCD, character encodings, check digits and Track 2.</p>
                <div class="chips"><span class="chip">Base64</span><span class="chip">BCD</span><span class="chip">Luhn</span><span class="chip">Track 2</span></div>
            </a>
        </div>
    </div>
</section>

<!-- screenshots -->
<section class="solid sec-pad" style="padding-top:0" data-sect="screenshots">
    <div class="wrap">
        <span class="kicker">Inside the studio</span>
        <h2 class="sec">A real desktop workbench</h2>
        <p class="sec-sub">Configure gateways, watch live transactions, and edit ISO 8583 fields — cross-platform, built with Kotlin &amp; Compose.</p>
        <div class="shots">
            <div class="shot wide reveal">
                <div class="shot-bar"><i></i><i></i><i></i><span>ISO8583Studio — Dashboard</span></div>
                <img src="/images/img.png" alt="ISO8583Studio dashboard with tool categories and quick access" loading="lazy">
            </div>
            <div class="shot reveal">
                <div class="shot-bar"><i></i><i></i><i></i><span>Host Simulator — Configuration</span></div>
                <img src="/images/img_1.png" alt="Host Simulator gateway configuration" loading="lazy">
            </div>
            <div class="shot reveal">
                <div class="shot-bar"><i></i><i></i><i></i><span>Transaction — Field editor</span></div>
                <img src="/images/img_3.png" alt="ISO8583 transaction template field editor" loading="lazy">
            </div>
        </div>
    </div>
</section>

<!-- formats -->
<section class="solid sec-pad" style="padding-top:0" data-sect="formats">
    <div class="wrap">
        <span class="kicker">Interoperable by default</span>
        <h2 class="sec">Any format. Any channel.</h2>
        <div class="fmt-band">
            <span class="fmt"><i></i>Binary ISO 8583</span>
            <span class="fmt"><i></i>Hexadecimal</span>
            <span class="fmt t"><i></i>JSON</span>
            <span class="fmt t"><i></i>XML</span>
            <span class="fmt t"><i></i>Key-Value</span>
            <span class="fmt g"><i></i>YAML mapping</span>
            <span class="fmt a"><i></i>TCP/IP</span>
            <span class="fmt a"><i></i>RS232</span>
            <span class="fmt a"><i></i>Dial-up</span>
            <span class="fmt a"><i></i>REST API</span>
        </div>
    </div>
</section>

<!-- workflow -->
<section class="solid sec-pad" style="padding-top:0" data-sect="lifecycle">
    <div class="wrap">
        <span class="kicker">One studio, whole lifecycle</span>
        <h2 class="sec">Develop → Test → Certify</h2>
        <p class="sec-sub">For the engineer writing the integration, the QA team proving it, and the manager signing it off.</p>
        <div class="flow-grid">
            <div class="step reveal"><span class="sn"></span><h3>Develop against simulators</h3><p>No test host? Simulate one. Build against local host, HSM and scheme endpoints with realistic responses.</p><span class="aud">FOR PAYMENT DEVELOPERS</span></div>
            <div class="step reveal"><span class="sn"></span><h3>Test to the bit</h3><p>Craft edge cases field by field, validate cryptograms and MACs, replay reversals — deterministic and logged.</p><span class="aud">FOR QA &amp; TEST ENGINEERS</span></div>
            <div class="step reveal"><span class="sn"></span><h3>Certify with confidence</h3><p>EMV L2/L3 and scheme certification prep with kernel-level tooling — cut lab time and re-submission cycles.</p><span class="aud">FOR CERTIFICATION TEAMS</span></div>
        </div>
    </div>
</section>

<!-- solutions -->
<section class="solid sec-pad" style="padding-top:0" data-sect="solutions">
    <div class="wrap">
        <span class="kicker">Beyond the toolbox</span>
        <h2 class="sec">Solutions &amp; services</h2>
        <div class="sol-grid">
            <a class="sol reveal" href="/emv-certification"><span class="mi">✓</span><h3>EMV Certification</h3><p>End-to-end L1/L2/L3 and scheme certification, from test plans to sign-off.</p><span class="go">Learn more →</span></a>
            <a class="sol reveal" href="/cloud-simulators"><span class="mi">☁</span><h3>Cloud Simulators</h3><p>Hosted host &amp; HSM endpoints for CI pipelines and distributed teams.</p><span class="go">Learn more →</span></a>
            <a class="sol reveal" href="/middleware"><span class="mi">⇄</span><h3>Payment Middleware</h3><p>Switching, routing and protocol translation built on the Studio engine.</p><span class="go">Learn more →</span></a>
            <a class="sol reveal" href="/kernel"><span class="mi">▦</span><h3>Kernel Development</h3><p>EMV L2 kernel engineering for terminals, from contact to contactless.</p><span class="go">Learn more →</span></a>
        </div>
    </div>
</section>

<section class="solid cta" data-sect="final_cta">
    <div class="wrap">
        <h2>Ship payment software<br>that just works</h2>
        <p>Free and open source under Apache. Download the studio, or star the repo and follow the roadmap.</p>
        <div class="row">
            <a class="btn btn-blue btn-lg" href="https://github.com/hpkaushik121/Iso8583studio/releases/latest">Download for free</a>
            <a class="btn btn-ghost btn-lg" href="https://github.com/hpkaushik121/Iso8583studio">★ Star on GitHub</a>
        </div>
        <div class="fine">java -jar ISO8583Studio.jar · Windows 10+ / macOS 10.14+ / Ubuntu 18.04+</div>
    </div>
</section>

<aside class="pro-nudge"><span class="pn-tag">✦ Pro</span><p>Testing with a team, or certifying with a scheme? Pro raises the CPS ceiling, unlocks the full algorithm set and deep simulator tweaks, plus hosted endpoints and priority support.</p><a href="/pro">Register for Pro →</a></aside>`,
})
export class HomePage {}
