import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { SitePage } from './site-page';

@Component({
  selector: 'page-emv-certification',
  changeDetection: ChangeDetectionStrategy.OnPush,
  hostDirectives: [SitePage],
  host: { class: 'static-page page-emv-certification' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<section class="page-hero">
    <div class="wrap ph-grid">
        <div>
            <div class="crumb"><a href="/">HOME</a> / <span>EMV CERTIFICATION</span></div>
            <span class="kicker">Certification Services</span>
            <h1 style="margin-top:12px">EMV L1 · L2 · L3 <span class="gr">certification</span> &amp; development</h1>
            <p class="ph-sub">From electrical protocol to end-to-end scheme sign-off — kernel-level expertise and Studio tooling that shorten every certification cycle.</p>
            <div class="ph-ctas"><a class="btn btn-blue btn-lg" href="/contact">Talk to us</a><a class="btn btn-ghost btn-lg" href="https://github.com/hpkaushik121/Iso8583studio/releases/latest">Download Studio</a></div>
        </div>
        <div class="ph-3d"><div class="ph-emblem">
    <span class="emb-corner tl"></span><span class="emb-corner tr"></span><span class="emb-corner bl"></span><span class="emb-corner br"></span>
    <span class="emb-tag" style="top:16px;left:18px">CERT · SPEC</span>
    <span class="emb-tag" style="bottom:16px;right:18px">ISO 8583</span>
    <span class="emb-scan"></span>
    <div class="emb-badge"><svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.4"><path d="M12 2l8 3v6c0 5-3.5 8.5-8 11-4.5-2.5-8-6-8-11V5l8-3z"/><path d="M9 12l2 2 4-4"/></svg></div>
    <div class="emb-chips"><span>L1</span><span>L2</span><span>L3</span></div>
  </div></div>
    </div>
</section>
<section class="sec-pad">
    <div class="wrap">
        <span class="kicker">Complete EMV Certification Suite</span>
        <h2 class="sec">Every level, covered</h2>
        <div class="grid-3">
            <div class="tile reveal"><span class="tag">HARDWARE</span><span class="mi">L1</span><h3>EMV Level 1</h3><p>Electromechanical and protocol layer — ATR handling, T=0/T=1, contactless RF interface conformance for terminal hardware.</p></div>
            <div class="tile reveal"><span class="tag">KERNEL</span><span class="mi">L2</span><h3>EMV Level 2</h3><p>Kernel certification — application selection, offline data authentication (SDA/DDA/CDA), cardholder verification, terminal risk management.</p></div>
            <div class="tile reveal"><span class="tag">END-TO-END</span><span class="mi">L3</span><h3>EMV Level 3</h3><p>End-to-end acquirer &amp; scheme testing — real transaction flows against host systems, from sale to settlement.</p></div>
        </div>
    </div>
</section>
<section class="sec-pad" style="background:var(--bg-deep);border-top:1px solid var(--line-soft);border-bottom:1px solid var(--line-soft)">
    <div class="wrap">
        <span class="kicker">Our Proven Methodology</span>
        <h2 class="sec">Six steps to sign-off</h2>
        <div class="tl">
            <div class="tl-item reveal"><span class="tl-n">01</span><div><h3>Assessment &amp; Planning</h3><p>Gap analysis of your terminal or host stack against target scheme requirements; a certification plan with realistic milestones.</p></div></div>
            <div class="tl-item reveal"><span class="tl-n">02</span><div><h3>Design &amp; Architecture</h3><p>Kernel configuration, parameter profiles and EMV data flows designed for the acceptance environment you ship to.</p></div></div>
            <div class="tl-item reveal"><span class="tl-n">03</span><div><h3>Development &amp; Integration</h3><p>Hands-on kernel and host integration work — tags, cryptograms, CVM lists — validated continuously in ISO8583Studio.</p></div></div>
            <div class="tl-item reveal"><span class="tl-n">04</span><div><h3>Testing &amp; Validation</h3><p>Full pre-certification passes with simulated hosts, HSMs and card profiles to catch failures before the lab does.</p></div></div>
            <div class="tl-item reveal"><span class="tl-n">05</span><div><h3>Certification Submission</h3><p>Lab liaison, log packages and defect turnaround — we drive the submission until the report says pass.</p></div></div>
            <div class="tl-item reveal"><span class="tl-n">06</span><div><h3>Deployment &amp; Support</h3><p>Post-certification rollout support, parameter management and re-certification planning for kernel updates.</p></div></div>
        </div>
    </div>
</section>
<section class="sec-pad">
    <div class="wrap">
        <span class="kicker">Deep Technical Expertise</span>
        <h2 class="sec">What we bring to the table</h2>
        <div class="grid-3">
            <div class="tile reveal"><span class="mi">⬡</span><h3>Security &amp; Cryptography</h3><p>ARQC/ARPC validation, SDA/DDA/CDA, key ceremonies and HSM integration across 15+ vendors.</p></div>
            <div class="tile reveal"><span class="mi">⇋</span><h3>Mobile &amp; Contactless</h3><p>Contactless kernels, mobile wallets and tokenized flows for tap-to-pay acceptance.</p></div>
            <div class="tile reveal"><span class="mi">◈</span><h3>Global Standards</h3><p>EMVCo, ISO 8583, ISO 9564 and scheme-specific requirements — tracked and applied.</p></div>
            <div class="tile reveal"><span class="mi">▲</span><h3>Performance Engineering</h3><p>Transaction timing budgets, EMV tag optimisation and terminal responsiveness under load.</p></div>
            <div class="tile reveal"><span class="mi">⇄</span><h3>Integration &amp; APIs</h3><p>Host, switch and gateway integration over TCP/IP, RS232 and REST.</p></div>
            <div class="tile reveal"><span class="mi">✓</span><h3>Testing &amp; QA</h3><p>Regression suites built on Studio simulators — deterministic, repeatable, auditable.</p></div>
        </div>
    </div>
</section>
<section class="cta">
    <div class="wrap">
        <h2>Ready to certify?</h2>
        <p>Tell us about your terminal, kernel or host project — we'll map the fastest route to certification.</p>
        <div class="row"><a class="btn btn-blue btn-lg" href="/contact">Talk to us</a><a class="btn btn-ghost btn-lg" href="https://github.com/hpkaushik121/Iso8583studio/releases/latest">Download Studio</a></div>
    </div>
</section>
<aside class="pro-nudge"><span class="pn-tag">✦ Pro</span><p>Testing with a team, or certifying with a scheme? Pro raises the CPS ceiling, unlocks the full algorithm set and deep simulator tweaks, plus hosted endpoints and priority support.</p><a href="/pro">Register for Pro →</a></aside>`,
})
export class EmvCertificationPage {}
