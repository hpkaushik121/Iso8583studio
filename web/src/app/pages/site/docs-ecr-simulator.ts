import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { SitePage } from './site-page';
import { UiSection } from '../../ui/section';

@Component({
  selector: 'page-docs-ecr-simulator',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiSection],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-ecr-simulator' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<main class="doc-body">
        <div class="breadcrumb">
            <a href="/">Home</a><span class="breadcrumb-sep">/</span>
            <a href="/docs">Documentation</a><span class="breadcrumb-sep">/</span>
            <span>ECR Simulator</span>
        </div>

        <div class="page-title-row">
            <h1 class="page-title">ECR Simulator</h1>
            <span class="badge badge-yellow">In Development</span>
        </div>
        <p class="page-description">Simulate an electronic cash register (ECR) integrated with a payment terminal — the register that sends sale, void and refund requests to the POS and prints the outcome. Scaffolded in the app and actively being built.</p>

        <div class="status-banner">
            <div class="sb-icon">🚧</div>
            <div>
                <h3>This simulator is under active development</h3>
                <p>The <code>ECR Simulator</code> type is scaffolded in ISO8583Studio, and its configuration &amp; runtime screens are being built. This page describes the intended capabilities. In the meantime, the &ldquo;Available Now&rdquo; simulators cover the same message flows &mdash; see <em>Where It Fits</em> below.</p>
            </div>
        </div>

        <ui-section anchor="overview" heading="Overview">
            <p>The ECR Simulator will model the cash-register side of an ECR&harr;POS integration: it issues transaction requests (sale, void, refund, settlement) to a connected terminal and consumes the results, over serial (RS232) or TCP links.</p>

        </ui-section>

        <ui-section anchor="planned" heading="Planned Capabilities">
            <ul>
                <li>Sale, void, refund, pre-auth and settlement request messages</li>
                <li>ECR &harr; POS integration over RS232 serial and TCP/IP</li>
                <li>Configurable register protocol framing and field mapping</li>
                <li>Basket / line-item and tender modelling</li>
                <li>Response handling and receipt data capture</li>
            </ul>
            <div class="info-card note"><div class="info-card-title">Note</div><p>Scope is indicative and may change as the feature is built. Follow the <a href="https://github.com/users/hpkaushik121/projects/1">roadmap</a> for status.</p></div>
        </ui-section>

        <ui-section anchor="fit" heading="Where It Fits">
            <p>The ECR drives the <a href="/simulator/pos">POS Simulator</a> from the merchant application side, completing the register &rarr; terminal &rarr; host chain.</p>
        </ui-section>
    </main>

<aside class="pro-nudge"><span class="pn-tag">✦ Pro</span><p>Testing with a team, or certifying with a scheme? Pro raises the CPS ceiling, unlocks the full algorithm set and deep simulator tweaks, plus hosted endpoints and priority support.</p><a href="/pro">Register for Pro →</a></aside>`,
})
export class DocsEcrSimulatorPage {}
