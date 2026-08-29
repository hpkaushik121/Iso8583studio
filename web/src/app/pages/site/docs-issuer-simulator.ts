import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { SitePage } from './site-page';
import { UiSection } from '../../ui/section';

@Component({
  selector: 'page-docs-issuer-simulator',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiSection],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-issuer-simulator' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<main class="doc-body">
        <div class="breadcrumb">
            <a href="/">Home</a><span class="breadcrumb-sep">/</span>
            <a href="/docs">Documentation</a><span class="breadcrumb-sep">/</span>
            <span>Issuer System</span>
        </div>

        <div class="page-title-row">
            <h1 class="page-title">Issuer System</h1>
            <span class="badge badge-yellow">In Development</span>
        </div>
        <p class="page-description">An issuer-side authorization host that approves or declines transactions, verifies PINs and cryptograms, and returns ISO 8583 responses. Scaffolded in the app and actively being built; today, issuer responses can be modelled with the Host Simulator.</p>

        <div class="status-banner">
            <div class="sb-icon">🚧</div>
            <div>
                <h3>This simulator is under active development</h3>
                <p>The <code>Issuer System</code> type is scaffolded in ISO8583Studio, and its configuration &amp; runtime screens are being built. This page describes the intended capabilities. In the meantime, the &ldquo;Available Now&rdquo; simulators cover the same message flows &mdash; see <em>Where It Fits</em> below.</p>
            </div>
        </div>

        <ui-section anchor="overview" heading="Overview">
            <p>The Issuer System will represent the card issuer at the far end of the network. It will authorize or decline requests against card/account state, verify online PINs and ARQCs, apply limits, and return <code>0210</code> responses with the right field 39 codes.</p>

        </ui-section>

        <ui-section anchor="planned" heading="Planned Capabilities">
            <ul>
                <li>Authorization decisioning against configurable card / account records</li>
                <li>Online PIN and ARQC (cryptogram) verification</li>
                <li>Balance, limit and velocity checks with stand-in rules</li>
                <li>Full ISO 8583 0200 &rarr; 0210 response construction</li>
                <li>Configurable approval / decline response-code scenarios</li>
            </ul>
            <div class="info-card note"><div class="info-card-title">Note</div><p>Scope is indicative and may change as the feature is built. Follow the <a href="https://github.com/users/hpkaushik121/projects/1">roadmap</a> for status.</p></div>
        </ui-section>

        <ui-section anchor="fit" heading="Where It Fits">
            <p>The issuer answers what the acquiring side sends. Today you can model issuer responses with the <a href="/docs/host-simulator">Host Simulator</a> in Server mode; the dedicated Issuer System will add account-aware decisioning.</p>
        </ui-section>
    </main>

<aside class="pro-nudge"><span class="pn-tag">✦ Pro</span><p>Testing with a team, or certifying with a scheme? Pro raises the CPS ceiling, unlocks the full algorithm set and deep simulator tweaks, plus hosted endpoints and priority support.</p><a href="/pro">Register for Pro →</a></aside>`,
})
export class DocsIssuerSimulatorPage {}
