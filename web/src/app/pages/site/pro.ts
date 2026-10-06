import { ChangeDetectionStrategy, Component, afterNextRender, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SitePage } from './site-page';
import {
  AccordionItem, Crumb, UiAccordion, UiBadge, UiBreadcrumb, UiIcon, UiReveal, UiWords,
} from '../../ui';
import { ProReserve } from '../pro/pro-reserve';
import {
  GST_PERCENT, PRO_MONTHLY_LABEL, PRO_MONTHLY_USD, PRO_RESERVE_USD, RESERVE_LABEL,
} from '../pro/pro-pricing';
import { CheckoutOutcome } from '../pro/checkout-outcome';
import { ProOrbit } from '../pro/pro-orbit';
import { ProAlgos, ProLane, ProSpotlight } from '../pro/pro-motion';
import { AnalyticsService } from '../../core/analytics';

interface Tier { kind: 'free' | 'pro'; name: string; text: string; height: number; }
interface Addition { icon: string; title: string; body: string; tint?: 'teal' | 'blue'; }

/** The two builds, side by side over the packet lanes. */
const TIERS: Tier[] = [
  {
    kind: 'free', name: 'Free', height: 64,
    text: 'All 9 simulators, all 64 tools, standard algorithms, throttled cryptographic throughput, '
      + 'local configs, manual test runs, community support on GitHub.',
  },
  {
    kind: 'pro', name: 'Pro', height: 128,
    text: 'Everything in Free, plus a raised CPS ceiling, the full algorithm set, deep per-simulator '
      + 'tweaks, hosted endpoints, certification packs, scripted suites in CI, config sync, extended '
      + 'HSM coverage and priority support.',
  },
];

/** The bento after its first cell, which carries the algorithm chips. */
const ADDITIONS: Addition[] = [
  {
    icon: 'sliders-horizontal', title: 'Deep simulator tweaks', tint: 'teal',
    body: 'Per-field ISO 8583 overrides, custom bitmap and header handling, latency and error '
      + 'injection, partial-response and timeout scenarios, and editable payShield command behaviour.',
  },
  {
    icon: 'cloud', title: 'Hosted simulator endpoints',
    body: 'Host, HSM and switch simulators running as always-on endpoints with static addresses, so '
      + 'pipelines and remote teammates hit the same test bed instead of someone\'s desktop.',
  },
  {
    icon: 'seal-check', title: 'Scheme certification packs', tint: 'blue',
    body: 'Curated Visa, Mastercard, RuPay and NPCI message sets with expected responses, run as a '
      + 'suite with a pass/fail report you can attach to a certification submission.',
  },
  {
    icon: 'terminal-window', title: 'Unlimited scripted suites',
    body: 'Chain transactions, assert on any field or TLV tag, parameterise with data files, and run '
      + 'the whole thing headless from the CLI in CI.',
  },
  {
    icon: 'users-three', title: 'Team configuration sync',
    body: 'Share gateway, HSM and terminal configurations across a team with versioning and rollback, '
      + 'instead of passing YAML files around.',
  },
  {
    icon: 'key', title: 'Full HSM key ceremony',
    body: 'Extended payShield command coverage, LMK sets per environment, TR-31 key blocks, component '
      + 'printing and audited key ceremonies.',
  },
  {
    icon: 'headset', title: 'Priority engineering support',
    body: 'Direct channel to the engineers who wrote the simulators, with same-business-day response '
      + 'and help reading real traces.',
  },
];


const FAQ: AccordionItem[] = [
  {
    q: 'Does the free studio change?',
    a: 'No. Every simulator and tool that is free today stays free, offline and unrestricted. Pro is additive.',
  },
  {
    q: 'How much does Pro cost?',
    a: `${PRO_MONTHLY_LABEL} per month (about $${PRO_MONTHLY_USD}), billed once Pro launches. Reserving a `
      + `seat today costs ${RESERVE_LABEL} (about $${PRO_RESERVE_USD}), including ${GST_PERCENT}% GST, `
      + 'charged in rupees at checkout.',
  },
  {
    q: 'What happens after I pay?',
    a: 'Your workspace is provisioned by hand, and your credentials are sent to the work email you '
      + 'registered with.',
  },
  {
    q: 'Which platforms does Pro run on?',
    a: 'The same as the free build: Windows 10+, macOS 10.14+ and Linux from Ubuntu 18.04+, as native '
      + 'installers with the runtime bundled.',
  },
  {
    q: 'Need an invoice, PO or annual contract?',
    a: 'Write to admin@iso8583.studio and we\'ll bill your organisation directly instead.',
  },
];

@Component({
  selector: 'page-pro',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink, UiAccordion, UiBadge, UiBreadcrumb, UiIcon, UiReveal, UiWords,
    ProOrbit, ProLane, ProAlgos, ProSpotlight, ProReserve,
  ],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-pro' },
  template: `
    <!-- A return from checkout is shown in the Pro card itself (#register),
         which scrolls into view with the outcome. -->
      <section class="page-hero pro-hero" data-sect="hero" proSpotlight>
        <div class="pro-hero-dots" aria-hidden="true"><span></span><span class="pro-hero-dots-hot"></span></div>
        <span class="pro-hero-wash" aria-hidden="true"></span>
        <div class="pro-hero-in">
          <ui-breadcrumb [items]="crumbs" />
          <div class="pro-hero-copy">
            <div class="pro-hero-badge ds-fade"><ui-badge tone="blue" icon="sparkle">Early access</ui-badge></div>
            <h1 class="ds-in"><ui-words text="ISO8583Studio Pro" [base]="180" /></h1>
            <p class="pro-hero-lede ds-fade" [style.--d]="420">The studio stays free. Pro raises the ceiling: production-rate cryptographic throughput, the full algorithm set, certification packs and an engineer to call when a cryptogram won't verify.</p>
            <div class="ph-ctas ds-fade" [style.--d]="560">
              <a class="btn btn--primary btn--lg btn--glow" routerLink="/pro" fragment="register">Register for Pro<ui-icon name="arrow-right" [size]="16" /></a>
              <a class="btn btn--outline btn--lg" routerLink="/pro" fragment="what">What's included</a>
            </div>
          </div>
          <app-pro-orbit class="pro-hero-orbit ds-fade" [style.--d]="300" />
          <span class="pro-hero-veil" aria-hidden="true"></span>
        </div>
      </section>

      <div class="pro-main">
        <section id="why" class="pro-sec" uiReveal [uiRevealThreshold]="0.15">
          <div class="pro-sec-head">
            <h2 class="pro-h2"><ui-words text="The free build is throttled. Pro is not." /></h2>
            <p class="pro-lede ds-hold" [style.--d]="240">The free build throttles cryptographic operations per second. Pro raises the CPS ceiling and runs multi-threaded across cores, for load and soak testing at production-like rates.</p>
          </div>
          <div class="pro-tiers ds-hold" [style.--d]="320">
            @for (tier of tiers; track tier.kind) {
              <div class="pro-tier" [class.pro-tier--pro]="tier.kind === 'pro'">
                <div class="pro-tier-copy">
                  <span class="pro-tier-name">{{ tier.name }}@if (tier.kind === 'pro') {<ui-badge tone="blue">Early access</ui-badge>}</span>
                  <span class="pro-tier-text">{{ tier.text }}</span>
                </div>
                <app-pro-lane [kind]="tier.kind" [height]="tier.height" />
              </div>
            }
          </div>
        </section>

        <section id="what" class="pro-sec" uiReveal [uiRevealThreshold]="0.05">
          <div class="pro-sec-head">
            <h2 class="pro-h2"><ui-words text="What Pro adds" /></h2>
          </div>
          <div class="pro-bento">
            <div class="pro-cell pro-cell--big pro-cell--algo pro-spot ds-item" proSpotlight [style.--d]="140">
              <span class="pro-cell-icon"><ui-icon name="lock-key" [size]="17" /></span>
              <h3 class="pro-cell-title">Full algorithm set</h3>
              <p class="pro-cell-body">Beyond the free 3DES and AES basics: RSA and ECC key operations, SHA-3, FPE (FF1/FF3-1), Poly1305, ChaCha20, DUKPT AES, and vendor-specific key derivations.</p>
              <app-pro-algos />
            </div>
            @for (add of additions; track add.title; let i = $index) {
              <div class="pro-cell pro-spot ds-item" proSpotlight [class.pro-cell--big]="i === 0"
                   [class.pro-cell--teal]="add.tint === 'teal'" [class.pro-cell--blue]="add.tint === 'blue'"
                   [style.--d]="200 + i * 60">
                <span class="pro-cell-icon"><ui-icon [name]="add.icon" [size]="17" /></span>
                <h3 class="pro-cell-title">{{ add.title }}</h3>
                <p class="pro-cell-body">{{ add.body }}</p>
              </div>
            }
          </div>
        </section>

        <section id="register" class="pro-sec" uiReveal [uiRevealThreshold]="0.05">
          <div class="pro-sec-head">
            <span class="pro-eyebrow ds-hold">Early-access registration</span>
            <h2 class="pro-h2"><ui-words text="Reserve your seat" /></h2>
            <p class="pro-lede ds-hold" [style.--d]="220">Reserve today with your work email. Your invite arrives when Pro launches.</p>
          </div>
          <app-pro-reserve class="pro-reserve" />
        </section>

        <section id="faq" class="pro-sec pro-sec--faq" uiReveal [uiRevealThreshold]="0.05">
          <div class="pro-sec-head">
            <h2 class="pro-h2"><ui-words text="Questions" /></h2>
            <p class="pro-lede ds-hold" [style.--d]="220">Pricing, tax, what happens after you pay and how Pro fits alongside the free build.</p>
          </div>
          <ui-accordion [items]="faq" [defaultOpen]="-1" [stagger]="60" [base]="160" />
          <p class="pro-faq-more">Something else? Write to <a href="mailto:admin@iso8583.studio">admin&#64;iso8583.studio</a>.</p>
        </section>
      </div>
  `,
})
export class ProPage {
  /** Set once, after the first render, from the `?payment=` return URL. */
  protected readonly outcome = inject(CheckoutOutcome);
  private readonly analytics = inject(AnalyticsService);

  protected readonly crumbs: Crumb[] = [{ label: 'Home', link: '/' }, { label: 'Pro' }];
  protected readonly tiers = TIERS;
  protected readonly additions = ADDITIONS;
  protected readonly faq = FAQ;

  constructor() {
    // After hydration, and only for a genuine pitch view — someone returning
    // from checkout is not viewing a product.
    afterNextRender(() => {
      if (!this.outcome.active()) this.analytics.reportProView();
    });
  }
}
