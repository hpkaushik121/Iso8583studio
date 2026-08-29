import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { SitePage } from './site-page';

@Component({
  selector: 'page-docs-payment-switch',
  changeDetection: ChangeDetectionStrategy.OnPush,
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-payment-switch' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<main class="doc-body">
        <div class="breadcrumb">
            <a href="/">Home</a><span class="breadcrumb-sep">/</span>
            <a href="/docs">Documentation</a><span class="breadcrumb-sep">/</span>
            <span>Payment Switch</span>
        </div>

        <div class="page-title-row">
            <h1 class="page-title">Payment Switch</h1>
            <span class="badge badge-yellow">In Development</span>
        </div>
        <p class="page-description">Route and translate ISO 8583 traffic between acquirers and issuers. The Payment Switch is scaffolded in the app and actively being built; today, proxy-style routing is available in the Host Simulator.</p>

        <div class="status-banner">
            <div class="sb-icon">🚧</div>
            <div>
                <h3>This simulator is under active development</h3>
                <p>The <code>Payment Switch</code> type is scaffolded in ISO8583Studio, and its configuration &amp; runtime screens are being built. This page describes the intended capabilities. In the meantime, the &ldquo;Available Now&rdquo; simulators cover the same message flows &mdash; see <em>Where It Fits</em> below.</p>
            </div>
        </div>

        <section class="doc-section" id="overview">
            <h2>Overview</h2>
            <p>The Payment Switch will sit in the middle of the network, receiving transactions from acquiring endpoints and routing them to the correct issuer — with BIN-based routing, protocol translation, and stand-in decisioning.</p>

        </section>

        <section class="doc-section" id="planned">
            <h2>Planned Capabilities</h2>
            <ul>
                <li>BIN / IIN-based routing tables to multiple destinations</li>
                <li>ISO 8583 protocol and format translation between endpoints</li>
                <li>Stand-in processing when a downstream host is unavailable</li>
                <li>Message enrichment, field mapping and MTI translation</li>
                <li>Per-route monitoring and throughput metrics</li>
            </ul>
            <div class="info-card note"><div class="info-card-title">Note</div><p>Scope is indicative and may change as the feature is built. Follow the <a href="https://github.com/users/hpkaushik121/projects/1">roadmap</a> for status.</p></div>
        </section>

        <section class="doc-section" id="fit">
            <h2>Where It Fits</h2>
            <p>Until the dedicated switch ships, the <a href="/docs/host-simulator">Host Simulator</a> in <strong>Proxy</strong> mode already bridges two endpoints and can inspect or modify traffic in transit.</p>
        </section>
    </main>

<aside class="pro-nudge"><span class="pn-tag">✦ Pro</span><p>Testing with a team, or certifying with a scheme? Pro raises the CPS ceiling, unlocks the full algorithm set and deep simulator tweaks, plus hosted endpoints and priority support.</p><a href="/pro">Register for Pro →</a></aside>`,
})
export class DocsPaymentSwitchPage {}
