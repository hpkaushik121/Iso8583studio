import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA, inject } from '@angular/core';
import { SitePage } from './site-page';
import { UiSection } from '../../ui/section';
import { ProForm } from '../pro/pro-form';
import { PaymentResult } from '../pro/payment-result';
import { CheckoutOutcome } from '../pro/checkout-outcome';

@Component({
  selector: 'page-pro',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiSection, ProForm, PaymentResult],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-pro' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<main class="doc-body">
    <!-- Coming back from checkout replaces the page rather than adding to it.
         Someone who has just paid is not shopping: the pitch, the plan table
         and the registration form are all answers to a question they have
         already settled, and the form is an invitation to pay twice. -->
    @if (outcome.active()) {
      <app-payment-result />
    } @else {
    <div class="breadcrumb">
        <a href="/">Home</a><span class="breadcrumb-sep">/</span>
        <span>Pro</span>
    </div>

    <div class="page-title-row"><h1 class="page-title">ISO8583Studio Pro</h1><span class="badge badge-purple">Early access</span></div>
    <p class="page-description">The studio stays free. Pro raises the ceiling: higher cryptographic throughput, the full algorithm set, deeper tuning of every simulator — plus hosted endpoints, certification-grade test packs and an engineer to call when a cryptogram won't verify.</p>

    <ui-section anchor="what" heading="What Pro adds">
        <div class="feature-grid">
            <div class="feature-item"><h4>Higher cryptographic throughput</h4><p>Raised CPS ceiling — the free build throttles cryptographic operations per second; Pro runs multi-threaded across cores for load and soak testing at production-like rates.</p></div>
            <div class="feature-item"><h4>Full algorithm set</h4><p>Beyond the free 3DES/AES basics: RSA and ECC key operations, SHA-3, FPE (FF1/FF3-1), Poly1305, ChaCha20, DUKPT AES, and vendor-specific key derivations.</p></div>
            <div class="feature-item"><h4>Deep simulator tweaks</h4><p>Per-field ISO 8583 overrides, custom bitmap and header handling, latency and error injection, partial-response and timeout scenarios, and editable payShield command behaviour.</p></div>
            <div class="feature-item"><h4>Hosted simulator endpoints</h4><p>Host, HSM and switch simulators running as always-on endpoints with static addresses, so pipelines and remote teammates hit the same test bed instead of someone's desktop.</p></div>
            <div class="feature-item"><h4>Scheme certification packs</h4><p>Curated Visa, Mastercard, RuPay and NPCI message sets with expected responses, run as a suite with a pass/fail report you can attach to a certification submission.</p></div>
            <div class="feature-item"><h4>Unlimited scripted suites</h4><p>Chain transactions, assert on any field or TLV tag, parameterise with data files, and run the whole thing headless from the CLI in CI.</p></div>
            <div class="feature-item"><h4>Team configuration sync</h4><p>Share gateway, HSM and terminal configurations across a team with versioning and rollback, instead of passing YAML files around.</p></div>
            <div class="feature-item"><h4>Full HSM key ceremony</h4><p>Extended payShield command coverage, LMK sets per environment, TR-31 key blocks, component printing and audited key ceremonies.</p></div>
            <div class="feature-item"><h4>Priority engineering support</h4><p>Direct channel to the engineers who wrote the simulators, with same-business-day response and help reading real traces.</p></div>
        </div>

        <div class="spec-list">
            <div class="spec-row"><div class="k">Free</div><div class="v">All 9 simulators, all 64 tools, standard algorithms, throttled cryptographic throughput, local configs, manual test runs, community support on GitHub.</div></div>
            <div class="spec-row"><div class="k">Pro</div><div class="v">Everything in Free, plus a raised CPS ceiling, the full algorithm set, deep per-simulator tweaks, hosted endpoints, certification packs, scripted suites in CI, config sync, extended HSM coverage and priority support.</div></div>
        </div>
    </ui-section>

    <ui-section anchor="register" heading="Register for Pro">
        <p>Tell us who you are and what you're certifying. We provision your workspace and send credentials by email.</p>

        <app-pro-form />
    </ui-section>

    <ui-section anchor="faq" heading="Questions">
        <div class="info-card"><div class="info-card-title">Does the free studio change?</div><p>No. Every simulator and tool that is free today stays free, offline and unrestricted. Pro is additive.</p></div>
        <div class="info-card note"><div class="info-card-title">Need an invoice, PO or annual contract?</div><p>Write to <a href="mailto:admin@iso8583.studio">admin@iso8583.studio</a> and we'll bill your organisation directly instead.</p></div>
    </ui-section>
    }
</main>`,
})
export class ProPage {
  /** Set once, after the first render, from the `?payment=` return URL. */
  protected readonly outcome = inject(CheckoutOutcome);
}
