import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { SitePage } from './site-page';

@Component({
  selector: 'page-cloud-simulators',
  changeDetection: ChangeDetectionStrategy.OnPush,
  hostDirectives: [SitePage],
  host: { class: 'static-page page-cloud-simulators' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<section class="page-hero">
    <div class="wrap ph-grid">
        <div>
            <div class="crumb"><a href="/">HOME</a> / <span>CLOUD SIMULATORS</span></div>
            <span class="kicker">Hosted Test Infrastructure</span>
            <h1 style="margin-top:12px">Cloud simulators for <span class="gr">payment ecosystem</span> testing</h1>
            <p class="ph-sub">Host, HSM, POS, card, acquirer and issuer simulators — hosted, scriptable and CI-ready. Test the whole transaction chain without a single piece of hardware.</p>
            <div class="ph-ctas"><a class="btn btn-blue btn-lg" href="/contact">Talk to us</a><a class="btn btn-ghost btn-lg" href="https://github.com/hpkaushik121/Iso8583studio/releases/latest">Download Studio</a></div>
        </div>
        <div class="ph-3d"><div class="ph-emblem">
    <span class="emb-corner tl"></span><span class="emb-corner tr"></span><span class="emb-corner bl"></span><span class="emb-corner br"></span>
    <span class="emb-tag" style="top:16px;left:18px">SIM · NET</span>
    <span class="emb-tag" style="bottom:16px;right:18px">ISO 8583</span>
    <span class="emb-scan"></span>
    <div class="emb-badge"><svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.4"><path d="M7 18a5 5 0 01-.9-9.92 7 7 0 0113.7 1.42A4.5 4.5 0 0118.5 18H7z"/></svg></div>
    <div class="emb-chips"><span>HOST</span><span>HSM</span><span>POS</span></div>
  </div></div>
    </div>
</section>
<section class="sec-pad">
    <div class="wrap">
        <span class="kicker">Complete Simulator Suite</span>
        <h2 class="sec">Six simulators, one ecosystem</h2>
        <div class="grid-3">
            <div class="tile reveal"><span class="mi">⇄</span><h3>Host Simulator</h3><p>Authorization host endpoints with configurable response rules, timeouts and unsolicited messages.</p></div>
            <div class="tile reveal"><span class="mi">⬡</span><h3>HSM Simulator</h3><p>PayShield-style command sets, PIN operations and key management without physical HSMs.</p></div>
            <div class="tile reveal"><span class="mi">▤</span><h3>POS Simulator</h3><p>Terminal-side flows — sales, reversals, settlements — driven manually or scripted.</p></div>
            <div class="tile reveal"><span class="mi">▣</span><h3>APDU / Card Simulator</h3><p>Chip-card conversations at APDU level: ATR, application selection, GENERATE AC.</p></div>
            <div class="tile reveal"><span class="mi">◧</span><h3>Acquirer Simulator</h3><p>Acquiring host behaviour for issuer-side testing, with scheme-realistic flows.</p></div>
            <div class="tile reveal"><span class="mi">◨</span><h3>Issuer Simulator</h3><p>Issuer authorization logic with configurable approval rules and response codes.</p></div>
        </div>
    </div>
</section>
<section class="sec-pad" style="background:var(--bg-deep);border-top:1px solid var(--line-soft);border-bottom:1px solid var(--line-soft)">
    <div class="wrap">
        <span class="kicker">Testing-First Architecture</span>
        <h2 class="sec">Built for automation</h2>
        <div class="grid-3">
            <div class="tile reveal"><span class="mi">>_</span><h3>CLI &amp; Scripting Support</h3><p>Drive every simulator from the command line — perfect for CI pipelines and nightly regression.</p></div>
            <div class="tile reveal"><span class="mi">▦</span><h3>Test Scenario Engine</h3><p>Compose multi-step scenarios: auth, reversal, repeat — with assertions on every field.</p></div>
            <div class="tile reveal"><span class="mi">◉</span><h3>Real-time Debugging</h3><p>Live formatted + raw hex views of every message crossing the wire.</p></div>
            <div class="tile reveal"><span class="mi">⚄</span><h3>Mock Data Generation</h3><p>Realistic PANs, tracks, amounts and EMV data generated on demand.</p></div>
            <div class="tile reveal"><span class="mi">▲</span><h3>Load Testing Tools</h3><p>Sustained TPS against your endpoints to expose pool and timeout issues early.</p></div>
            <div class="tile reveal"><span class="mi">⇋</span><h3>API Testing Suite</h3><p>REST APIs alongside ISO 8583 — test modern gateways and legacy rails together.</p></div>
        </div>
    </div>
</section>
<section class="sec-pad">
    <div class="wrap">
        <span class="kicker">Why Cloud Simulator?</span>
        <h2 class="sec">Test the chain, not your patience</h2>
        <div class="grid-3">
            <div class="tile reveal"><span class="mi">◷</span><h3>Rapid Test Setup</h3><p>Minutes from sign-up to first 0200 — no hardware procurement, no lab scheduling.</p></div>
            <div class="tile reveal"><span class="mi">⟳</span><h3>Automated Testing</h3><p>Hook simulators into CI so every commit runs against a full payment ecosystem.</p></div>
            <div class="tile reveal"><span class="mi">⌘</span><h3>Developer-Friendly APIs</h3><p>Clean, documented endpoints for provisioning and control.</p></div>
            <div class="tile reveal"><span class="mi">▲</span><h3>Performance Testing</h3><p>Measure latency and throughput against realistic host behaviour.</p></div>
            <div class="tile reveal"><span class="mi">▣</span><h3>Safe Environment</h3><p>Isolated tenants and synthetic data — nothing touches production rails.</p></div>
            <div class="tile reveal"><span class="mi">⚠</span><h3>Edge Case Simulation</h3><p>Timeouts, partial responses, malformed fields — break it here, not in production.</p></div>
        </div>
    </div>
</section>
<section class="cta">
    <div class="wrap">
        <h2>Ready to simulate?</h2>
        <p>Get hosted endpoints for your team, or run the same simulators locally with the free desktop studio.</p>
        <div class="row"><a class="btn btn-blue btn-lg" href="/contact">Talk to us</a><a class="btn btn-ghost btn-lg" href="https://github.com/hpkaushik121/Iso8583studio/releases/latest">Download Studio</a></div>
    </div>
</section>
<aside class="pro-nudge"><span class="pn-tag">✦ Pro</span><p>Testing with a team, or certifying with a scheme? Pro raises the CPS ceiling, unlocks the full algorithm set and deep simulator tweaks, plus hosted endpoints and priority support.</p><a href="/pro">Register for Pro →</a></aside>`,
})
export class CloudSimulatorsPage {}
