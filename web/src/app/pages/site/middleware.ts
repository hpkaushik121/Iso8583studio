import { ChangeDetectionStrategy, Component, ElementRef, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AccordionItem, UiAccordion, UiBadge, UiIcon, UiReveal, UiSectionHeading } from '../../ui';
import {
  ArtName, SolArt, SolBenefit, SolBenefits, SolBento, SolClose, SolHero, SolLive, SolMark, SolStrip, solCycle,
} from '../solutions';
import { SitePage } from './site-page';
import { LeadForm } from '../shared/lead-form';

interface Service { n: string; kicker: string; img: ArtName; title: string; desc: string; items: string[]; }
interface Stage {
  label: string; blurb: string; title: string; code: string; description: string; status: string;
  /** Which of channel (0), middleware (1) and provider (2) are lit. */
  nodes: number[];
}

const SERVICES: Service[] = [
  { n: '01', kicker: 'Connect & translate', img: 'bridge', title: 'Payment Bridge', desc: 'Connect systems that speak different payment languages, with protocol translation between channels and providers.', items: ['ISO 8583 dialect translation', 'JSON, XML and REST interfaces', 'Consistent message mapping'] },
  { n: '02', kicker: 'Decide & direct', img: 'core', title: 'Transaction Orchestrator', desc: 'Apply routing policies across providers and manage the transaction lifecycle, including exception handling.', items: ['Rules-driven provider selection', 'Retries, reversals and timeouts', 'Store-and-forward handling'] },
  { n: '03', kicker: 'Integrate & extend', img: 'hub', title: 'Integration Hub', desc: 'Use one integration surface to connect to acquiring hosts, issuers, payment switches and gateways.', items: ['Multiple endpoint connections', 'Provider-specific transports', 'Independent channel integration'] },
];

const STAGES: Stage[] = [
  { label: 'Transaction initiation', blurb: 'Accept a request from a POS, e-commerce or ATM channel.', title: 'Request received', code: 'MTI     0200 · Financial request\nDE 003  000000\nDE 004  000000001250\nDE 011  000001\nDE 049  356', description: 'A synthetic purchase request enters the middleware. Field values shown here are illustrative.', status: 'Message parsed · ready to route', nodes: [0] },
  { label: 'Intelligent routing', blurb: 'Apply rules for BIN, amount, scheme or provider availability.', title: 'Routing policy applied', code: 'RULE      currency = 356\nAMOUNT    12.50 INR\nROUTE     provider_a\nFORMAT    JSON / REST\nPOLICY    configured provider priority', description: 'The routing layer evaluates the request against a configured rule and selects an eligible provider.', status: 'Provider A selected', nodes: [0, 1] },
  { label: 'Provider integration', blurb: "Translate the message into the selected provider's format.", title: 'Message translated', code: '{\n  "type": "purchase",\n  "amountMinor": 1250,\n  "currency": "INR",\n  "reference": "000001"\n}', description: 'Payment fields map to an illustrative provider JSON format. Each integration defines its own contract.', status: 'Provider payload prepared', nodes: [1, 2] },
  { label: 'Real-time monitoring', blurb: 'Inspect message fields, raw data and each transaction hop.', title: 'Transaction observed', code: 'TRACE     demo_000001\nHOP 01    channel → middleware\nHOP 02    middleware → provider_a\nVIEW      parsed fields + raw message\nSTATE     awaiting provider response', description: 'A shared trace links both sides of the exchange so operators can inspect the message at each hop.', status: 'Request and provider hop correlated', nodes: [0, 1, 2] },
  { label: 'Response orchestration', blurb: 'Normalize the result and respond to the originating channel.', title: 'Response normalized', code: 'PROVIDER  status = approved\nMTI       0210 · Financial response\nDE 011    000001\nDE 039    00\nCHANNEL   response returned', description: 'The provider result maps back to the originating channel format and retains the request reference.', status: 'Response ready for the originating channel', nodes: [0, 1, 2] },
];

const BENEFITS: SolBenefit[] = [
  ['01', 'Swap providers, keep channels', 'Add or swap providers behind a consistent interface, keeping channel code independent.'],
  ['02', 'Complete transparency', 'Follow messages at field level, with formatted and raw views for troubleshooting.'],
  ['03', 'Intelligent routing', 'Choose providers by cost, latency or explicit business rules, according to your policy.'],
  ['04', 'Risk management', 'Apply velocity checks, transaction limits and blocklists at the switch layer.'],
  ['05', 'Performance analytics', 'Review throughput, approval rates and latency percentiles across individual routes.'],
  ['06', 'Operational efficiency', 'Manage store-and-forward, automatic reversals and connection recovery within one layer.'],
];

const FAQ: AccordionItem[] = [
  { q: 'What sits inside the middleware layer?', a: 'Three building blocks: a bridge for protocol translation, an orchestrator for routing policy and transaction lifecycle, and an integration hub that holds the provider connections. Channels talk to one interface; provider specifics stay behind it.' },
  { q: 'How are routing decisions configured?', a: 'As rules over the request: BIN, amount, currency, scheme or provider availability, with an explicit priority order. The selected route, the rule that matched and the resulting format are all visible in the transaction trace.' },
  { q: 'Which formats and transports are supported?', a: 'Binary ISO 8583 and its dialects, JSON, XML and REST, carried over the transports each provider requires — mapped on the way in and on the way out.' },
  { q: 'What happens when a provider times out?', a: 'The orchestrator owns retries, automatic reversals, store-and-forward and connection recovery, so a provider outage becomes a routing decision rather than a channel-side failure.' },
  { q: 'How much visibility do operators get?', a: 'Field-level views of every message with formatted and raw data side by side, correlated across hops, plus throughput, approval-rate and latency percentiles per route.' },
  { q: 'Is this the same engine as the Studio simulators?', a: 'Yes. The middleware is built on the Studio engine, so the same message parsing, cryptographic tooling and traces you use for testing carry into the production switch layer.' },
];

const MARKS: SolMark[] = [
  ['arrows-left-right', 'Switch'], ['git-branch', 'Routing'], ['swap', 'Translate'], ['broadcast', 'Host'],
  ['bank', 'Issuer'], ['storefront', 'Channel'], ['pulse', 'Monitor'], ['shield-check', 'Risk'],
  ['chart-line', 'Analytics'],
];

@Component({
  selector: 'page-middleware',
  imports: [
    LeadForm,
    RouterLink, UiAccordion, UiBadge, UiIcon, UiReveal, UiSectionHeading,
    SolArt, SolBenefits, SolBento, SolClose, SolHero, SolLive, SolStrip,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  hostDirectives: [SitePage],
  host: { class: 'static-page page-middleware' },
  template: `
    <sol-hero sect="Transaction Orchestration" crumb="Payment middleware" eyebrow="Route · Translate · Observe"
              heading="Route, translate and trace every ISO 8583 message"
              sub="Connect terminals, processors and schemes with rule-based routing, protocol translation and full transaction visibility — built on the Studio engine."
              cta="Plan your switch layer" [headingMax]="740" [subMax]="640" [padBottom]="46">
      <a solHeroCta class="btn btn--secondary" routerLink="/middleware" fragment="flow">Explore the middleware layer<ui-icon name="arrow-down" [size]="16" /></a>
      <div class="sol-mw-route" role="img"
           aria-label="Channel routed through the middleware layer to two providers, with a response returning to the channel">
        <div class="sol-mw-ambient"></div>
        <svg class="sol-mw-wires" viewBox="0 0 760 460" preserveAspectRatio="none" aria-hidden="true">
          <path class="sol-mw-track" d="M132 202H246" />
          <path class="sol-mw-track" d="M420 180H614Q628 180 628 166V144" />
          <path class="sol-mw-track" d="M420 224H520Q534 224 534 238V308Q534 322 548 322H600" />
          <path class="sol-mw-track" d="M628 392V416Q628 430 614 430H294Q280 430 280 416V300" />
          <path class="sol-mw-track" d="M246 230H132" />
          <path class="sol-mw-flow" d="M132 202H246" />
          <path class="sol-mw-flow sol-mw-flow--b" d="M420 180H614Q628 180 628 166V144" />
          <path class="sol-mw-flow sol-mw-flow--b" d="M420 224H520Q534 224 534 238V308Q534 322 548 322H600" />
          <path class="sol-mw-flow sol-mw-flow--back" d="M628 392V416Q628 430 614 430H294Q280 430 280 416V300" />
          <path class="sol-mw-flow sol-mw-flow--back2" d="M246 230H132" />
        </svg>
        <div class="sol-mw-obj sol-mw-o-channel"><img solArt="terminal" eager sizes="(max-width: 760px) 19vw, 145px"><span>CHANNEL</span></div>
        <div class="sol-mw-obj sol-mw-o-core"><img solArt="core" priority sizes="(max-width: 760px) 27vw, 205px"><span>MIDDLEWARE</span></div>
        <div class="sol-mw-obj sol-mw-o-prov-a"><img solArt="server" eager sizes="(max-width: 760px) 17vw, 130px"><span>PROVIDER A</span></div>
        <div class="sol-mw-obj sol-mw-o-prov-b"><img solArt="issuer" eager sizes="(max-width: 760px) 17vw, 130px"><span>PROVIDER B</span></div>
        <div class="sol-mw-chip sol-mw-chip--any">Any format</div>
        <div class="sol-mw-chip sol-mw-chip--iso">ISO 8583</div>
        <div class="sol-mw-chip sol-mw-chip--b24">Base 24</div>
        <div class="sol-mw-chip sol-mw-chip--rsp">RESPONSE</div>
        <span class="sol-mw-marker sol-mw-marker--l">ONE INTEGRATION.<br>MANY DESTINATIONS.</span>
        <span class="sol-mw-marker sol-mw-marker--r">SWITCH · ROUTE · TRANSLATE</span>
      </div>
    </sol-hero>

    <sol-strip [tight]="true" caption="ISO 8583 · JSON · XML · REST — one integration surface for every provider" [marks]="marks" />

    <section class="sol-sec" id="services" data-sect="Middleware Services Suite" uiReveal [uiRevealDelay]="250" solLive>
      <ui-section-heading heading="A bridge, an orchestrator and an integration hub"
                          sub="Bring payment channels and providers together through a shared layer for translation, routing and integration." />
      <div class="sol-grid3">
        @for (s of services; track s.n; let i = $index) {
          <sol-bento class="ds-item" [style.--d]="500 + i * 120" [heading]="s.title" [badge]="s.n" [desc]="s.desc"
                     [glow]="i === 0" [radial]="i === 1">
            <div class="sol-mono sol-mono--teal sol-kicker">{{ s.kicker }}</div>
            <ul class="sol-checks">
              @for (t of s.items; track t) { <li><ui-icon name="check" [size]="15" />{{ t }}</li> }
            </ul>
            <div class="sol-mw-svc" aria-hidden="true"><img [solArt]="s.img" sizes="160px"></div>
          </sol-bento>
        }
      </div>
    </section>

    <section class="sol-sec" id="flow" data-sect="Intelligent Transaction Flow" uiReveal [uiRevealDelay]="250" solLive>
      <ui-section-heading align="left" [heading]="flowHeading"
                          sub="From the incoming request to the final response, keep routing decisions and message transformations visible." />
      <div class="sol-picker ds-hold">
        <div class="sol-picker-list">
          @for (st of stages; track st.label; let n = $index) {
            <button type="button" class="sol-pick" aria-controls="stage-detail"
                    [class.is-active]="n === cycle.index()" [attr.aria-pressed]="n === cycle.index()"
                    (click)="cycle.pick(n)">
              <span class="sol-pick-n">{{ pad(n) }}</span>
              <span class="sol-pick-text">
                <span class="sol-pick-label">{{ st.label }}</span>
                <span class="sol-pick-blurb">{{ st.blurb }}</span>
              </span>
            </button>
          }
        </div>
        <div #scene class="sol-panel" id="stage-detail">
          <span class="sol-panel-radial" aria-hidden="true"></span>
          <div class="sol-mw-mini" aria-hidden="true">
            <svg class="sol-mw-mini-svg" viewBox="0 0 600 230" preserveAspectRatio="none">
              <path class="sol-mw-mini-track" d="M100 108H300H500M100 150H500" />
              <path class="sol-mw-live" d="M100 108H300H500" />
              @if (cycle.index() === 4) { <path class="sol-mw-live sol-mw-live--back" d="M500 150H100" /> }
            </svg>
            <div class="sol-mw-node sol-mw-n1" [class.is-on]="lit(0)"><img solArt="terminal" sizes="110px"><span>CHANNEL</span></div>
            <div class="sol-mw-node sol-mw-n2" [class.is-on]="lit(1)"><img solArt="core" sizes="110px"><span>MIDDLEWARE</span></div>
            <div class="sol-mw-node sol-mw-n3" [class.is-on]="lit(2)"><img solArt="server" sizes="110px"><span>PROVIDER A</span></div>
          </div>
          <div class="sol-detail-copy">
            @for (st of stages; track st.label; let n = $index) {
              <div class="sol-step-copy sol-mw-stage" [hidden]="n !== cycle.index()">
                <div class="sol-detail-head">
                  <h3>{{ st.title }}</h3>
                  <span class="sol-mono">{{ pad(n) }} / 05</span>
                </div>
                <pre class="sol-mw-code">{{ st.code }}</pre>
                <p>{{ st.description }}</p>
                <div class="sol-mw-status"><ui-badge tone="teal" icon="check-circle">{{ st.status }}</ui-badge></div>
              </div>
            }
          </div>
        </div>
      </div>
      <div class="sol-foot-row">
        <p class="sol-mono sol-mono--plain">Illustrative message flow using synthetic data. No payment requests are sent.</p>
        <button type="button" class="btn btn--secondary btn--sm sol-cycle-toggle" (click)="cycle.toggle()">
          @if (cycle.paused()) {
            <ui-icon name="play" [size]="14" />Play flow
          } @else {
            <ui-icon name="pause" [size]="14" />Pause flow
          }
        </button>
      </div>
    </section>

    <section class="sol-sec" id="benefits" data-sect="Middleware Advantages" uiReveal [uiRevealDelay]="250">
      <ui-section-heading align="left" heading="Provider quirks stay in the switch layer"
                          sub="Keep provider complexity in the switch layer and give your team a clear view of how transactions behave." />
      <sol-benefits [items]="benefits" />
    </section>

    <section class="sol-sec sol-sec--faq" id="faq" data-sect="faq" uiReveal [uiRevealDelay]="250">
      <ui-section-heading heading="Middleware questions" sub="Routing, formats, deployment and what the switch layer owns." />
      <div class="sol-faq"><ui-accordion [items]="faq" [stagger]="110" [base]="500" /></div>
    </section>

    <div class="sol-sec">
      <app-lead-form surface="middleware"
                     heading="Talk to us about middleware"
                     lede="Tell us what you are routing between, and the throughput you need to hold." />
    </div>

    <sol-close sect="Ready to orchestrate?" kicker="Your next payment connection" cta="Plan your switch layer"
               text="Let's design the switch layer around your transaction volume, provider connections and routing requirements.">
      <p solCloseFoot class="pro-nudge sol-pro-line">
        <a routerLink="/pro">ISO8583Studio Pro</a>
        <span>Advanced testing tools, hosted endpoints and priority support for your team.</span>
      </p>
      <div class="sol-mw-close">
        <img class="sol-i-base" solArt="base" sizes="(max-width: 900px) 65vw, 370px">
        <img class="sol-i-core" solArt="core" sizes="(max-width: 900px) 37vw, 210px">
      </div>
    </sol-close>
  `,
})
export class MiddlewarePage {
  protected readonly services = SERVICES;
  protected readonly stages = STAGES;
  protected readonly benefits = BENEFITS;
  protected readonly faq = FAQ;
  protected readonly marks = MARKS;
  protected readonly flowHeading = 'Trace a 0200 from request\nto normalized response';

  private readonly scene = viewChild<ElementRef<HTMLElement>>('scene');

  /** Stages advance every 4.6 s until the reader picks one or pauses. */
  protected readonly cycle = solCycle(
    { count: STAGES.length, interval: 4600 },
    () => this.scene()?.nativeElement,
  );

  protected pad(i: number): string {
    return String(i + 1).padStart(2, '0');
  }

  protected lit(node: number): boolean {
    return STAGES[this.cycle.index()].nodes.includes(node);
  }
}
