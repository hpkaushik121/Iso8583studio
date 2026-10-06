import { ChangeDetectionStrategy, Component, ElementRef, signal, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EXTERNAL } from '../../core/site-nav';
import { AccordionItem, UiAccordion, UiBadge, UiIcon, UiReveal, UiSectionHeading } from '../../ui';
import {
  ArtName, SolArt, SolBenefit, SolBenefits, SolBento, SolClose, SolHero, SolLive, SolMark, SolStrip, solCycle,
} from '../solutions';
import { SitePage } from './site-page';

interface EcoNode { key: string; img: ArtName; label: string; }
interface Sim { n: string; kicker: string; img: ArtName; title: string; desc: string; }
interface Scenario {
  name: string; command: string; request: string; response: string;
  /** Text, highlighted value, text. */
  result: [string, string?, string?];
  outcome: string; requestChip: string; responseChip: string; direction: string;
  /** Whether a response travels back along the return path. */
  ret: boolean;
}
interface Automation { icon: string; title: string; desc: string; at?: string; }

const NODES: EcoNode[] = [
  { key: 'host', img: 'server', label: 'HOST' },
  { key: 'hsm', img: 'hsm', label: 'HSM' },
  { key: 'pos', img: 'terminal', label: 'POS' },
  { key: 'card', img: 'chip', label: 'CARD' },
  { key: 'acquirer', img: 'acquirer', label: 'ACQUIRER' },
  { key: 'issuer', img: 'issuer', label: 'ISSUER' },
];

/** Hub to each node, in a 100 × 100 box. */
const WIRES = ['M50 48 L50 10.5', 'M50 48 L82 29.5', 'M50 48 L82 66.5', 'M50 48 L50 85.5', 'M50 48 L18 66.5', 'M50 48 L18 29.5'];

const SIMS: Sim[] = [
  { n: '01', kicker: 'Message processing', img: 'server', title: 'Host Simulator', desc: 'Model authorization endpoints with custom responses, timeouts and unsolicited messages.' },
  { n: '02', kicker: 'Cryptography', img: 'hsm', title: 'HSM Simulator', desc: 'Test payShield-style commands, PIN operations and key management without a physical HSM.' },
  { n: '03', kicker: 'Terminal flows', img: 'terminal', title: 'POS Simulator', desc: 'Run terminal-side sales, reversals and settlement flows through manual or scripted execution.' },
  { n: '04', kicker: 'Card conversations', img: 'chip', title: 'APDU / Card Simulator', desc: 'Explore chip-card exchanges, from ATR and application selection through GENERATE AC.' },
  { n: '05', kicker: 'Acquiring flows', img: 'acquirer', title: 'Acquirer Simulator', desc: 'Recreate acquiring host behavior and scheme-oriented transaction flows for issuer-side testing.' },
  { n: '06', kicker: 'Authorization', img: 'issuer', title: 'Issuer System', desc: 'Configure authorization decisions, approval rules and response codes for predictable scenarios.' },
];

const SCENARIOS: Scenario[] = [
  { name: 'Authorization', command: 'authorization / expected approval', request: '0200 · Financial request', response: '0210 · Financial response', result: ['DE 39 ', '00', ' · Approved'], outcome: 'Expected response matched', requestChip: '0200 →', responseChip: '← 0210', direction: 'IN', ret: true },
  { name: 'Timeout', command: 'timeout / expected no response', request: '0200 · Financial request', response: 'Response window expired', result: ['TIMEOUT · No response received'], outcome: 'Timeout branch exercised', requestChip: '0200 →', responseChip: 'NO RESPONSE', direction: 'WAIT', ret: false },
  { name: 'Reversal', command: 'reversal / expected acknowledgement', request: '0400 · Reversal request', response: '0410 · Reversal response', result: ['DE 39 ', '00', ' · Acknowledged'], outcome: 'Reversal response matched', requestChip: '0400 →', responseChip: '← 0410', direction: 'IN', ret: true },
];

const AUTOMATION: Automation[] = [
  { icon: 'terminal-window', title: 'CLI & scripting', desc: 'Drive repeatable simulator runs from command-line workflows and CI pipelines.', at: '88% 6%' },
  { icon: 'git-branch', title: 'Scenario engine', desc: 'Compose multi-step flows and verify the fields that matter at each stage.', at: '6% 96%' },
  { icon: 'bug-beetle', title: 'Real-time debugging', desc: 'Inspect formatted messages alongside raw hexadecimal data as they cross the wire.' },
  { icon: 'database', title: 'Mock data generation', desc: 'Use synthetic card, track, amount and EMV data to exercise different test conditions.', at: '104% 52%' },
  { icon: 'gauge', title: 'Load testing', desc: 'Exercise sustained transaction volumes to expose connection, pool and timeout issues.', at: '50% 108%' },
  { icon: 'plugs-connected', title: 'API testing', desc: 'Test REST integrations alongside ISO 8583 flows across modern and legacy systems.', at: '-4% 12%' },
];

const BENEFITS: SolBenefit[] = [
  ['01', 'Rapid test setup', 'Start from hosted endpoints and prepared scenarios, without procuring physical test equipment.'],
  ['02', 'Automated regression', 'Connect simulator runs to CI so repeatable payment checks become part of every build.'],
  ['03', 'Developer-friendly APIs', 'Use documented provisioning and control interfaces within your existing testing workflow.'],
  ['04', 'Performance visibility', 'Observe throughput and response timing under realistic host behavior and transaction loads.'],
  ['05', 'Isolated test environments', 'Exercise your integration with synthetic data in test environments separate from production rails.'],
  ['06', 'Hard-to-reproduce cases', 'Introduce timeouts, partial responses and malformed fields before those failures reach production.'],
];

const FAQ: AccordionItem[] = [
  { q: 'What runs in the cloud, and what runs locally?', a: 'The same simulators ship in the desktop Studio and as hosted endpoints. Locally they run on your machine for development; hosted, they give pipelines and distributed teams a stable URL instead of an environment per branch.' },
  { q: 'Can I drive the simulators from CI?', a: 'Yes. Provisioning and control interfaces are documented, and simulator runs can be scripted from command-line workflows, so repeatable payment checks become part of every build.' },
  { q: 'Which simulators are included?', a: 'Host, HSM, POS, APDU / card, acquirer and issuer — exercised individually or connected for an end-to-end chain from terminal to authorization decision.' },
  { q: 'Is any real card or production data involved?', a: 'No. Hosted environments run on synthetic card, track, amount and EMV data, isolated from production rails.' },
  { q: 'Can I reproduce timeouts and malformed responses?', a: 'That is the point of a simulator. Custom responses, response-window expiry, partial responses and malformed fields are all configurable, so those branches get exercised before production does.' },
  { q: 'How do I get endpoints for my team?', a: 'Hosted endpoints come with Pro. Tell us the simulators and the volume you need and we will provision the environment; the desktop Studio stays free and open source under the GNU AGPL v3.' },
];

const MARKS: SolMark[] = [
  ['broadcast', 'Host'], ['key', 'HSM'], ['credit-card', 'POS'], ['sim-card', 'Card'],
  ['arrows-left-right', 'Acquirer'], ['bank', 'Issuer'], ['cloud', 'Hosted'],
  ['terminal-window', 'CLI'], ['infinity', 'CI'],
];

/** The log has five lines; three more beats hold the finished trace before it replays. */
const TRACE_FRAMES = 8;

@Component({
  selector: 'page-cloud-simulators',
  imports: [
    RouterLink, UiAccordion, UiBadge, UiIcon, UiReveal, UiSectionHeading,
    SolArt, SolBenefits, SolBento, SolClose, SolHero, SolLive, SolStrip,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  hostDirectives: [SitePage],
  host: { class: 'static-page page-cloud-simulators' },
  template: `
    <sol-hero sect="Hosted Test Infrastructure" crumb="Cloud simulators" eyebrow="6 simulators"
              heading="Hosted payment simulators your CI can call"
              sub="Host, HSM, POS, card, acquirer and issuer simulators. Hosted, scriptable and ready for CI—so you can test the whole transaction chain."
              cta="Request a hosted endpoint" [headingMax]="760" [subMax]="620">
      <a solHeroCta class="btn btn--secondary" [href]="releases">Download Studio<ui-icon name="arrow-up-right" [size]="16" /></a>
      <div solHeroFacts class="sol-facts ds-fade" [style.--d]="1250">
        <span class="sol-fact"><ui-icon name="cloud" [size]="17" />Hosted endpoints</span>
        <span class="sol-fact"><ui-icon name="code" [size]="17" />Scriptable flows</span>
        <span class="sol-fact"><ui-icon name="infinity" [size]="17" />CI-ready</span>
      </div>
      <div class="sol-cs-eco" role="img"
           aria-label="Cloud simulator hub connected to host, HSM, POS, card, acquirer and issuer nodes">
        <div class="sol-cs-ambient"></div>
        <div class="sol-cs-ring"></div>
        <div class="sol-cs-ring sol-cs-ring--inner"></div>
        <svg class="sol-cs-wires" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">
          @for (w of wires; track w) { <path class="sol-cs-wire" [attr.d]="w" /> }
          @for (w of wires; track w; let i = $index) { <path class="sol-cs-pulse" [style.--i]="i" [attr.d]="w" /> }
        </svg>
        <div class="sol-cs-hub"><img solArt="cloud" priority sizes="(max-width: 600px) 42vw, 252px"></div>
        <div class="sol-cs-hub-label">CLOUD SIMULATORS<br><span>ONE CONNECTED ECOSYSTEM</span></div>
        @for (n of nodes; track n.key) {
          <div [class]="'sol-cs-node sol-cs-n-' + n.key"><img [solArt]="n.img" eager sizes="102px"><span>{{ n.label }}</span></div>
        }
      </div>
    </sol-hero>

    <sol-strip caption="ISO 8583 · APDU · HSM commands · hosted endpoints for every branch" [marks]="marks" />

    <section class="sol-sec" id="suite" data-sect="Complete Simulator Suite" uiReveal [uiRevealDelay]="250" solLive>
      <ui-section-heading heading="Host, HSM, POS, card, acquirer and issuer"
                          sub="Exercise each component on its own, then connect the pieces for end-to-end payment testing." />
      <div class="sol-grid3">
        @for (s of sims; track s.n; let i = $index) {
          <sol-bento class="ds-item" [style.--d]="500 + (i >= 3 ? 370 : 0) + (i % 3) * 120"
                     [heading]="s.title" [badge]="s.n" [desc]="s.desc" [glow]="i % 3 === 0" [radial]="i % 3 === 1">
            <div class="sol-mono sol-mono--teal sol-kicker">{{ s.kicker }}</div>
            <div class="sol-cs-sim" aria-hidden="true"><img [solArt]="s.img" sizes="154px"></div>
          </sol-bento>
        }
      </div>
    </section>

    <section class="sol-sec" id="architecture" data-sect="Testing-First Architecture" uiReveal [uiRevealDelay]="250" solLive>
      <ui-section-heading align="left" [heading]="traceHeading"
                          sub="Script the flow. Inspect the message. Repeat the scenario with a clear view of every step." />
      <div #trace class="sol-panel sol-cs-panel ds-hold">
        <div class="sol-cs-bar sol-mono">
          <span class="sol-cs-bar-title">
            <span class="sol-cs-dots" aria-hidden="true"><i></i><i></i><i></i></span>
            Payment flow / trace
          </span>
          <span class="sol-mono--teal">Illustrative preview</span>
        </div>
        <div class="sol-cs-body">
          <div class="sol-cs-trace" role="img"
               aria-label="Illustrative flow from POS through acquirer and host to issuer, with an HSM connection">
            <svg class="sol-cs-trace-svg" viewBox="0 0 600 330" preserveAspectRatio="none" aria-hidden="true">
              <path class="sol-cs-track" d="M76 195H222H380H526M380 185V92" />
              <path class="sol-cs-track" d="M526 220H76" />
              <path class="sol-cs-flow" d="M76 195H222H380H526" />
              @if (scenario().ret) { <path class="sol-cs-flow sol-cs-flow--return" d="M526 220H76" /> }
              <path class="sol-cs-flow sol-cs-flow--up" d="M380 185V92" />
            </svg>
            <div class="sol-cs-tnode sol-cs-t-pos"><img solArt="terminal" sizes="92px"><span>POS</span></div>
            <div class="sol-cs-tnode sol-cs-t-acquirer"><img solArt="acquirer" sizes="92px"><span>ACQUIRER</span></div>
            <div class="sol-cs-tnode sol-cs-t-host"><img solArt="server" sizes="92px"><span>HOST</span></div>
            <div class="sol-cs-tnode sol-cs-t-issuer"><img solArt="issuer" sizes="92px"><span>ISSUER</span></div>
            <div class="sol-cs-tnode sol-cs-t-hsm"><img solArt="hsm" sizes="92px"><span>HSM</span></div>
            <div class="sol-cs-chip sol-cs-chip--out">{{ scenario().requestChip }}</div>
            <div class="sol-cs-chip sol-cs-chip--in">{{ scenario().responseChip }}</div>
            <span class="sol-mono sol-cs-trace-note">Synthetic data · visual demonstration</span>
          </div>
          <div class="sol-cs-scenario">
            @for (s of scenarios; track s.name; let n = $index) {
              <div [hidden]="n !== chosen()">
                <div class="sol-mono sol-mono--teal">Scenario preview</div>
                <div class="sol-cs-cmd"><span class="sol-cs-cmd-mark">›</span>{{ s.command }}<span class="sol-cs-caret" aria-hidden="true"></span></div>
                <div role="group" aria-label="Illustrative transaction log">
                  <div class="sol-cs-log" [class.is-dim]="dim(0)" [class.is-active]="at(0)"><span>00:00</span><span class="sol-cs-dir">INIT</span><span>Load synthetic transaction</span></div>
                  <div class="sol-cs-log" [class.is-dim]="dim(1)" [class.is-active]="at(1)"><span>00:01</span><span class="sol-cs-dir sol-cs-dir--out">OUT</span><span>{{ s.request }}</span></div>
                  <div class="sol-cs-field" [class.is-dim]="dim(2)">DE 03 <strong>000000</strong> · DE 11 <strong>000001</strong></div>
                  <div class="sol-cs-log" [class.is-dim]="dim(3)" [class.is-active]="at(3)"><span>00:02</span><span class="sol-cs-dir" [class.sol-cs-dir--in]="s.ret" [class.sol-cs-dir--wait]="!s.ret">{{ s.direction }}</span><span>{{ s.response }}</span></div>
                  <div class="sol-cs-field" [class.is-dim]="dim(4)">{{ s.result[0] }}@if (s.result[1]) {<strong>{{ s.result[1] }}</strong>}{{ s.result[2] }}</div>
                </div>
                <div class="sol-cs-outcome">
                  @if (s.ret) {
                    <ui-badge tone="teal" icon="check-circle">{{ s.outcome }}</ui-badge>
                  } @else {
                    <ui-badge tone="warn" icon="clock-countdown">{{ s.outcome }}</ui-badge>
                  }
                </div>
              </div>
            }
          </div>
        </div>
        <div class="sol-cs-controls">
          <div class="sol-seg" role="tablist" aria-label="Scenario">
            @for (s of scenarios; track s.name; let n = $index) {
              <button type="button" role="tab" [attr.aria-selected]="n === chosen()" (click)="choose(n)">{{ s.name }}</button>
            }
          </div>
          <button type="button" class="btn btn--secondary btn--sm sol-cycle-toggle" (click)="cycle.restart()">
            <ui-icon name="arrow-counter-clockwise" [size]="14" />Replay trace
          </button>
        </div>
      </div>
      <p class="sol-mono sol-mono--plain sol-note">Illustrative flow and message fields only. This preview makes no network requests.</p>
      <div class="sol-grid3">
        @for (a of automation; track a.title; let i = $index) {
          <div class="sol-tile sol-glass ds-item" [class.sol-glass--lit]="!!a.at" [style.--at]="a.at"
               [style.--d]="500 + (i >= 3 ? 340 : 0) + (i % 3) * 120">
            <ui-icon [name]="a.icon" [size]="22" />
            <h3>{{ a.title }}</h3>
            <p>{{ a.desc }}</p>
          </div>
        }
      </div>
    </section>

    <section class="sol-sec" id="benefits" data-sect="Why Cloud Simulator?" uiReveal [uiRevealDelay]="250">
      <ui-section-heading align="left" heading="No terminal on your desk, no slot in the lab queue"
                          sub="Run the full transaction chain against hosted simulators instead of waiting for hardware or a shared test lab." />
      <sol-benefits [items]="benefits" />
    </section>

    <section class="sol-sec sol-sec--faq" id="faq" data-sect="faq" uiReveal [uiRevealDelay]="250">
      <ui-section-heading heading="Hosted simulation, answered" sub="Endpoints, CI, data and what runs where." />
      <div class="sol-faq"><ui-accordion [items]="faq" [stagger]="110" [base]="500" /></div>
    </section>

    <sol-close sect="Ready to simulate?" kicker="Connect your next test environment" cta="Request a hosted endpoint"
               text="Get hosted endpoints for your team, or run simulators locally with the desktop Studio.">
      <p solCloseFoot class="pro-nudge sol-pro-line">
        <a routerLink="/pro">ISO8583Studio Pro</a>
        <span>Advanced testing tools, hosted endpoints and support for your team.</span>
      </p>
      <div class="sol-cs-close">
        <img class="sol-i-base" solArt="base" sizes="(max-width: 900px) 72vw, 410px">
        <img class="sol-i-cloud" solArt="cloud" sizes="(max-width: 900px) 42vw, 235px">
      </div>
    </sol-close>
  `,
})
export class CloudSimulatorsPage {
  protected readonly releases = EXTERNAL.releases;
  protected readonly nodes = NODES;
  protected readonly wires = WIRES;
  protected readonly sims = SIMS;
  protected readonly scenarios = SCENARIOS;
  protected readonly automation = AUTOMATION;
  protected readonly benefits = BENEFITS;
  protected readonly faq = FAQ;
  protected readonly marks = MARKS;
  protected readonly traceHeading = 'Scripted from a pipeline,\ninspectable to the bit';

  /** Index of the scenario on show. */
  protected readonly chosen = signal(0);

  private readonly trace = viewChild<ElementRef<HTMLElement>>('trace');

  /** Walks the log one line every 720 ms while the panel is on screen. */
  protected readonly cycle = solCycle(
    { count: TRACE_FRAMES, interval: 720 },
    () => this.trace()?.nativeElement,
  );

  protected scenario(): Scenario {
    return SCENARIOS[this.chosen()];
  }

  protected choose(n: number): void {
    this.chosen.set(n);
    this.cycle.restart();
  }

  /**
   * A log line the trace has not reached yet. Nothing is dimmed until the
   * cycle is running, so the prerendered page — and the page under reduced
   * motion — shows the whole log.
   */
  protected dim(line: number): boolean {
    return this.cycle.live() && line > this.cycle.index();
  }

  protected at(line: number): boolean {
    return this.cycle.live() && line === this.cycle.index();
  }
}
