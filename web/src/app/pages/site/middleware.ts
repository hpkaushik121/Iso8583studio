import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { SitePage } from './site-page';

@Component({
  selector: 'page-middleware',
  changeDetection: ChangeDetectionStrategy.OnPush,
  hostDirectives: [SitePage],
  host: { class: 'static-page page-middleware' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<section class="page-hero">
    <div class="wrap ph-grid">
        <div>
            <div class="crumb"><a href="/">HOME</a> / <span>PAYMENT MIDDLEWARE</span></div>
            <span class="kicker">Transaction Orchestration</span>
            <h1 style="margin-top:12px">Payment middleware &amp; <span class="gr">transaction orchestration</span></h1>
            <p class="ph-sub">Bridge terminals, processors and schemes with intelligent routing, protocol translation and full transaction transparency — built on the Studio engine.</p>
            <div class="ph-ctas"><a class="btn btn-blue btn-lg" href="/contact">Talk to us</a><a class="btn btn-ghost btn-lg" href="https://github.com/hpkaushik121/Iso8583studio/releases/latest">Download Studio</a></div>
        </div>
        <div class="ph-3d"><div class="ph-emblem">
    <span class="emb-corner tl"></span><span class="emb-corner tr"></span><span class="emb-corner bl"></span><span class="emb-corner br"></span>
    <span class="emb-tag" style="top:16px;left:18px">SWITCH · ROUTE</span>
    <span class="emb-tag" style="bottom:16px;right:18px">ISO 8583</span>
    <span class="emb-scan"></span>
    <div class="emb-badge"><svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.4"><path d="M8 3L4 7l4 4M4 7h16M16 21l4-4-4-4M20 17H4"/></svg></div>
    <div class="emb-chips"><span>ISO</span><span>JSON</span><span>REST</span></div>
  </div></div>
    </div>
</section>
<section class="sec-pad">
    <div class="wrap">
        <span class="kicker">Middleware Services Suite</span>
        <h2 class="sec">Three building blocks</h2>
        <div class="grid-3">
            <div class="tile reveal"><span class="mi">⇄</span><h3>Payment Bridge</h3><p>Protocol translation between ISO 8583 dialects, JSON, XML and REST — connect anything to anything.</p></div>
            <div class="tile reveal"><span class="mi">◉</span><h3>Transaction Orchestrator</h3><p>Rules-driven routing, retries, reversals and store-and-forward across multiple providers.</p></div>
            <div class="tile reveal"><span class="mi">⬡</span><h3>Integration Hub</h3><p>One integration surface for many endpoints — acquirers, issuers, switches and gateways.</p></div>
        </div>
    </div>
</section>
<section class="sec-pad" style="background:var(--bg-deep);border-top:1px solid var(--line-soft);border-bottom:1px solid var(--line-soft)">
    <div class="wrap">
        <span class="kicker">Intelligent Transaction Flow</span>
        <h2 class="sec">The life of a transaction</h2>
        <div class="tl">
            <div class="tl-item reveal"><span class="tl-n">01</span><div><h3>Transaction Initiation</h3><p>Accept messages from POS, e-commerce or ATM channels in any supported format and length type.</p></div></div>
            <div class="tl-item reveal"><span class="tl-n">02</span><div><h3>Intelligent Routing</h3><p>Route by BIN, amount, scheme or availability — with automatic failover between providers.</p></div></div>
            <div class="tl-item reveal"><span class="tl-n">03</span><div><h3>Provider Integration</h3><p>Translate to each provider's dialect and transport: TCP/IP, RS232, dial-up or REST.</p></div></div>
            <div class="tl-item reveal"><span class="tl-n">04</span><div><h3>Real-time Monitoring</h3><p>Every hop logged with formatted and raw views — latency, response codes and traffic stats live.</p></div></div>
            <div class="tl-item reveal"><span class="tl-n">05</span><div><h3>Response Orchestration</h3><p>Normalize responses, handle reversals and timeouts, and answer the originating channel correctly.</p></div></div>
        </div>
    </div>
</section>
<section class="sec-pad">
    <div class="wrap">
        <span class="kicker">Middleware Advantages</span>
        <h2 class="sec">Transparency &amp; control</h2>
        <div class="grid-3">
            <div class="tile reveal"><span class="mi">⇋</span><h3>Seamless Integration</h3><p>Add or swap providers without touching channel code.</p></div>
            <div class="tile reveal"><span class="mi">◉</span><h3>Complete Transparency</h3><p>Field-level visibility of every message, in flight and at rest.</p></div>
            <div class="tile reveal"><span class="mi">◈</span><h3>Intelligent Routing</h3><p>Least-cost, least-latency or rule-based — your policy, enforced.</p></div>
            <div class="tile reveal"><span class="mi">▣</span><h3>Risk Management</h3><p>Velocity checks, limits and blocklists at the switch layer.</p></div>
            <div class="tile reveal"><span class="mi">▲</span><h3>Performance Analytics</h3><p>TPS, approval rates and latency percentiles per route.</p></div>
            <div class="tile reveal"><span class="mi">⟳</span><h3>Operational Efficiency</h3><p>Store-and-forward, auto-reversals and self-healing connections.</p></div>
        </div>
    </div>
</section>
<section class="cta">
    <div class="wrap">
        <h2>Ready to orchestrate?</h2>
        <p>Let's design the switch layer your transaction volume deserves.</p>
        <div class="row"><a class="btn btn-blue btn-lg" href="/contact">Talk to us</a><a class="btn btn-ghost btn-lg" href="https://github.com/hpkaushik121/Iso8583studio/releases/latest">Download Studio</a></div>
    </div>
</section>
<aside class="pro-nudge"><span class="pn-tag">✦ Pro</span><p>Testing with a team, or certifying with a scheme? Pro raises the CPS ceiling, unlocks the full algorithm set and deep simulator tweaks, plus hosted endpoints and priority support.</p><a href="/pro">Register for Pro →</a></aside>`,
})
export class MiddlewarePage {}
