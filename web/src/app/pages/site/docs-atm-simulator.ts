import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { SitePage } from './site-page';

@Component({
  selector: 'page-docs-atm-simulator',
  changeDetection: ChangeDetectionStrategy.OnPush,
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-atm-simulator' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<main class="doc-body">
        <div class="breadcrumb">
            <a href="/">Home</a><span class="breadcrumb-sep">/</span>
            <a href="/docs">Documentation</a><span class="breadcrumb-sep">/</span>
            <span>ATM Simulator</span>
        </div>

        <div class="page-title-row">
            <h1 class="page-title">ATM Simulator</h1>
            <span class="badge badge-yellow">In Development</span>
        </div>
        <p class="page-description">Model an automated teller machine driving cash-withdrawal, balance-inquiry and PIN-change transactions to an authorization host. The ATM Simulator is scaffolded in the app and actively being built.</p>

        <div class="status-banner">
            <div class="sb-icon">🚧</div>
            <div>
                <h3>This simulator is under active development</h3>
                <p>The <code>ATM Simulator</code> type is scaffolded in ISO8583Studio, and its configuration &amp; runtime screens are being built. This page describes the intended capabilities. In the meantime, the &ldquo;Available Now&rdquo; simulators cover the same message flows &mdash; see <em>Where It Fits</em> below.</p>
            </div>
        </div>

        <section class="doc-section" id="overview">
            <h2>Overview</h2>
            <p>The ATM Simulator will represent a self-service cash machine. It will originate ISO 8583 financial requests (withdrawal, balance, transfer, PIN change), model device-level NDC/DDC state flows, and drive them to a host or switch — the same way the <a href="/docs/pos-simulator">POS Simulator</a> models an attended terminal.</p>

        </section>

        <section class="doc-section" id="planned">
            <h2>Planned Capabilities</h2>
            <ul>
                <li>Cash withdrawal, balance inquiry, mini-statement and PIN-change transaction flows</li>
                <li>NDC / DDC device state modelling (card read, PIN entry, dispense, eject)</li>
                <li>Configurable cassettes and note denominations with dispense simulation</li>
                <li>ISO 8583 messaging to a host or switch over TCP/IP</li>
                <li>Electronic journal / transaction log</li>
            </ul>
            <div class="info-card note"><div class="info-card-title">Note</div><p>Scope is indicative and may change as the feature is built. Follow the <a href="https://github.com/users/hpkaushik121/projects/1">roadmap</a> for status.</p></div>
        </section>

        <section class="doc-section" id="fit">
            <h2>Where It Fits</h2>
            <p>The ATM sits at the acceptance edge of the network, alongside the <a href="/docs/pos-simulator">POS Simulator</a>. Pair it with the <a href="/docs/host-simulator">Host Simulator</a> to authorize its transactions.</p>
        </section>
    </main>

<aside class="pro-nudge"><span class="pn-tag">✦ Pro</span><p>Testing with a team, or certifying with a scheme? Pro raises the CPS ceiling, unlocks the full algorithm set and deep simulator tweaks, plus hosted endpoints and priority support.</p><a href="/pro">Register for Pro →</a></aside>`,
})
export class DocsAtmSimulatorPage {}
