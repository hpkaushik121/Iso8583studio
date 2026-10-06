import { ChangeDetectionStrategy, Component, ElementRef, viewChild, viewChildren } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EXTERNAL } from '../../core/site-nav';
import { AccordionItem, UiAccordion, UiIcon, UiReveal, UiSectionHeading } from '../../ui';
import { SolArt, SolBento, SolClose, SolHero, SolLive, SolMark, SolStrip, solCycle } from '../solutions';
import { SitePage } from './site-page';
import { LeadForm } from '../shared/lead-form';

interface Level { tag: string; title: string; desc: string; tokens: string[]; }
interface Step { label: string; kicker: string; title: string; text: string; tokens: string[]; }
interface Expertise { title: string; desc: string; at?: string; }

const LEVELS: Level[] = [
  { tag: 'L1', title: 'EMV Level 1', desc: 'Electrical, mechanical and protocol conformance — ATR handling, contact interfaces and contactless RF behaviour.', tokens: ['Contact', 'Contactless', 'ATR', 'Protocol'] },
  { tag: 'L2', title: 'EMV Level 2', desc: 'Kernel behaviour, application selection, data authentication, cardholder verification and terminal risk management.', tokens: ['SDA', 'DDA', 'CDA', 'CVM'] },
  { tag: 'L3', title: 'EMV Level 3', desc: 'Acquirer and scheme integration testing — transaction flows against host systems, from sale to settlement.', tokens: ['Terminal', 'Host', 'Scheme', 'Settlement'] },
];

const STEPS: Step[] = [
  { label: 'Assess', kicker: 'Define the scope', title: 'Assessment & planning', text: 'Map your terminal or host stack against target requirements. Identify gaps and build a certification plan with clear milestones.', tokens: ['Gap analysis', 'Test scope', 'Milestones'] },
  { label: 'Design', kicker: 'Shape the solution', title: 'Design & architecture', text: 'Align kernel configuration, parameter profiles and EMV data flows with the acceptance environment you need to support.', tokens: ['Kernel profiles', 'Parameters', 'Data flows'] },
  { label: 'Build', kicker: 'Connect the stack', title: 'Development & integration', text: 'Bring terminals, kernels and hosts together. Validate tags, cryptograms and cardholder verification throughout development.', tokens: ['EMV tags', 'Cryptograms', 'Host integration'] },
  { label: 'Validate', kicker: 'Find issues early', title: 'Testing & validation', text: 'Run pre-certification passes with simulated hosts, HSMs and card profiles. Reproduce failures and verify every fix.', tokens: ['Pre-certification', 'Regression', 'Evidence'] },
  { label: 'Submit', kicker: 'Prepare for the lab', title: 'Certification submission', text: 'Coordinate lab submission, organize log packages and work through findings with a clear defect-resolution process.', tokens: ['Log packages', 'Lab coordination', 'Issue resolution'] },
  { label: 'Support', kicker: 'Support the rollout', title: 'Deployment & support', text: 'Move from certification into production with rollout support, parameter management and planning for future kernel updates.', tokens: ['Rollout', 'Parameters', 'Re-certification'] },
];

const EXPERTISE: Expertise[] = [
  { title: 'Security & cryptography', desc: 'Cryptogram validation, data authentication, key management and HSM integration.', at: '88% 6%' },
  { title: 'Mobile & contactless', desc: 'Contactless kernels, mobile wallets and tokenized acceptance flows.', at: '6% 96%' },
  { title: 'Global standards', desc: 'EMVCo, ISO 8583, ISO 9564 and scheme requirements applied to your stack.' },
  { title: 'Performance engineering', desc: 'Transaction timing, EMV tag optimization and terminal responsiveness under load.', at: '104% 52%' },
  { title: 'Integration & APIs', desc: 'Terminals, hosts, switches and gateways over TCP/IP, RS232 and REST.', at: '50% 108%' },
  { title: 'Testing & QA', desc: 'Repeatable regression suites with simulated hosts, HSMs and card profiles.', at: '-4% 12%' },
];

const FAQ: AccordionItem[] = [
  { q: 'Which EMV levels do you cover?', a: 'All three. Level 1 for the electrical, mechanical and protocol layer, Level 2 for kernel behaviour and card processing, Level 3 for acquirer and scheme end-to-end acceptance — plus the scheme-specific test plans that sit on top.' },
  { q: 'Can you work with our existing kernel?', a: 'Yes. Most engagements start from a terminal and kernel you already have. We map the current configuration against the target requirements, then work through the gaps rather than replacing what already passes.' },
  { q: 'How does Studio tooling shorten certification?', a: 'Pre-certification runs happen on your machine against simulated hosts, HSMs and card profiles, so defects surface before the lab slot instead of during it. Failing cases replay deterministically with the same keys, clock and responses.' },
  { q: 'Do you handle the lab submission itself?', a: 'We prepare and coordinate it: log packages, evidence bundles and submission paperwork, then work through lab findings with a defect-resolution loop until sign-off.' },
  { q: 'What happens after sign-off?', a: 'Rollout support, parameter management across the estate, and a plan for the next kernel update or re-certification window so the approval does not expire unnoticed.' },
  { q: 'Which standards do you test against?', a: 'EMVCo Level 1, 2 and 3 specifications, ISO 8583 and ISO 9564, plus scheme requirements from the networks your acceptance environment needs to support.' },
];

const MARKS: SolMark[] = [
  ['cpu', 'Kernel'], ['identification-card', 'Chip'], ['device-mobile-camera', 'Terminal'],
  ['broadcast', 'Contactless'], ['shield-check', 'Crypto'], ['hard-drives', 'Host'],
  ['globe-hemisphere-west', 'Scheme'], ['seal-check', 'Lab'], ['files', 'Evidence'],
];

/** Left offset and rise of each spark drifting up off the hero base. */
const SPARKS: [x: string, rise: string][] = [
  ['8%', '150px'], ['26%', '190px'], ['44%', '130px'], ['61%', '205px'], ['78%', '160px'], ['92%', '185px'],
];

@Component({
  selector: 'page-emv-certification',
  imports: [
    LeadForm,
    RouterLink, UiAccordion, UiIcon, UiReveal, UiSectionHeading,
    SolArt, SolBento, SolClose, SolHero, SolLive, SolStrip,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  hostDirectives: [SitePage],
  host: { class: 'static-page page-emv-certification' },
  template: `
    <sol-hero sect="Certification Services" crumb="EMV certification"
              eyebrow="L1 · L2 · L3 · certification &amp; development"
              heading="Certify every layer of the terminal"
              lead="L1 hardware, L2 kernel and L3 host, run by one team."
              sub="Kernel-level expertise and Studio tooling for every stage of EMV certification — from gap analysis to lab sign-off."
              cta="Scope your certification" [headingMax]="780" [subMax]="560" [explode]="620">
      <a solHeroCta class="btn btn--secondary" [href]="releases">Download Studio<ui-icon name="arrow-up-right" [size]="16" /></a>
      <div class="sol-emv-scene" role="img"
           aria-label="Exploded certification assembly: glass shield, EMV chip and kernel board above a blue-lit base">
        <div class="sol-emv-halo"></div>
        <div class="sol-emv-floor"></div>
        <div class="sol-emv-pulse" aria-hidden="true">
          @for (n of [0, 1, 2]; track n) { <i [style.--i]="n"></i> }
        </div>
        <div class="sol-emv-piece sol-emv-p-base"><img solArt="base" priority sizes="(max-width: 600px) 100vw, 640px"></div>
        <div class="sol-emv-piece sol-emv-p-kernel"><img solArt="kernel-board" eager sizes="(max-width: 600px) 75vw, 435px"></div>
        <div class="sol-emv-piece sol-emv-p-chip"><img solArt="chip" eager sizes="(max-width: 600px) 72vw, 420px"></div>
        <div class="sol-emv-piece sol-emv-p-glass"><img solArt="glass-plate" eager sizes="(max-width: 600px) 76vw, 440px"></div>
        <div class="sol-emv-piece sol-emv-p-shield"><img solArt="shield" eager sizes="(max-width: 600px) 27vw, 160px"></div>
        <div class="sol-emv-sparks" aria-hidden="true">
          @for (s of sparks; track s[0]; let i = $index) { <i [style.--x]="s[0]" [style.--rise]="s[1]" [style.--i]="i"></i> }
        </div>
        <div class="sol-emv-sheen" aria-hidden="true"></div>
        <div class="sol-emv-scan"></div>
        <div class="sol-emv-badges"><span>L1</span><span>L2</span><span>L3</span></div>
      </div>
    </sol-hero>

    <sol-strip caption="EMVCo L1, L2 and L3 · scheme certification · kernel engineering" [marks]="marks" />

    <section class="sol-sec" id="coverage" data-sect="Complete EMV Certification Suite" uiReveal [uiRevealDelay]="250" solLive>
      <ui-section-heading heading="L1 contact plate to L3 host response"
                          sub="One continuous path from the contact plate on the terminal to the authorization response coming back off the scheme." />
      <div class="sol-grid3">
        @for (l of levels; track l.tag; let i = $index) {
          <sol-bento class="ds-item" [style.--d]="500 + i * 120" [heading]="l.title" [badge]="l.tag"
                     [badgeTone]="i === 2 ? 'teal' : 'blue'" [desc]="l.desc" [radial]="i === 1" [glow]="i !== 1">
            <div class="sol-tags sol-emv-tags">
              @for (t of l.tokens; track t) { <span class="ui-tag">{{ t }}</span> }
            </div>
            @switch (i) {
              @case (0) {
                <div class="sol-emv-lvl" aria-hidden="true">
                  <div class="sol-emv-rings">@for (n of [0, 1, 2, 3]; track n) { <i [style.--i]="n"></i> }</div>
                  <img class="sol-i-chip" solArt="chip" sizes="310px">
                </div>
              }
              @case (1) {
                <div class="sol-emv-lvl sol-emv-lvl--stack" aria-hidden="true">
                  <img solArt="kernel-board" sizes="285px"><img solArt="kernel-board" sizes="285px"><img solArt="kernel-board" sizes="285px">
                </div>
              }
              @default {
                <div class="sol-emv-lvl sol-emv-lvl--network" aria-hidden="true">
                  <img class="sol-i-terminal" solArt="terminal" sizes="170px">
                  <svg viewBox="0 0 140 100" preserveAspectRatio="none">
                    <path class="sol-emv-route" d="M0 50 C40 10 100 90 140 50" />
                    <path class="sol-emv-packet" d="M0 50 C40 10 100 90 140 50" />
                  </svg>
                  <img class="sol-i-server" solArt="server" sizes="155px">
                </div>
              }
            }
          </sol-bento>
        }
      </div>
    </section>

    <section class="sol-sec" id="process" data-sect="Our Proven Methodology" uiReveal [uiRevealDelay]="250" solLive>
      <ui-section-heading align="left" heading="Six steps to sign-off"
                          sub="The same sequence on every engagement, whether the scope is one contactless kernel or a whole acceptance estate." />
      <div class="sol-emv-stepper ds-hold">
        <div class="sol-emv-rail" aria-hidden="true"></div>
        <div class="sol-emv-rail sol-emv-rail--done" aria-hidden="true" [style.--p]="cycle.index() / 5"></div>
        <ol>
          @for (st of steps; track st.label; let i = $index) {
            <li>
              <button #stepBtn type="button" class="sol-emv-step" aria-controls="process-detail"
                      [class.is-active]="i === cycle.index()" [class.is-done]="i < cycle.index()"
                      [attr.aria-pressed]="i === cycle.index()"
                      (click)="cycle.pick(i)" (keydown)="onKey($event, i)">
                <span class="sol-emv-step-n">{{ pad(i) }}</span>{{ st.label }}
              </button>
            </li>
          }
        </ol>
      </div>
      <div #scene class="sol-panel sol-emv-detail" id="process-detail">
        <span class="sol-panel-radial" aria-hidden="true"></span>
        <div class="sol-emv-detail-grid">
          <div class="sol-emv-copy">
            @for (st of steps; track st.label; let i = $index) {
              <div class="sol-step-copy" [hidden]="i !== cycle.index()">
                <div class="sol-mono sol-mono--teal">{{ pad(i) }} / {{ st.kicker }}</div>
                <h3>{{ st.title }}</h3>
                <p>{{ st.text }}</p>
                <div class="sol-tags">
                  @for (t of st.tokens; track t) { <span class="ui-tag">{{ t }}</span> }
                </div>
              </div>
            }
          </div>
          <div class="sol-emv-process" aria-hidden="true">
            <div class="sol-emv-vis sol-emv-v-assess" [class.is-active]="cycle.index() === 0">
              <img class="sol-i-sheet" solArt="test-document" sizes="270px">
              <img class="sol-i-lens" solArt="assessment-magnifier" sizes="225px">
            </div>
            <div class="sol-emv-vis sol-emv-v-design" [class.is-active]="cycle.index() === 1">
              <img solArt="glass-plate" sizes="270px"><img solArt="glass-plate" sizes="270px"><img solArt="glass-plate" sizes="270px">
            </div>
            <div class="sol-emv-vis sol-emv-v-build" [class.is-active]="cycle.index() === 2">
              <img class="sol-i-kernel" solArt="kernel-board" sizes="305px">
              <img class="sol-i-chip" solArt="chip" sizes="270px">
            </div>
            <div class="sol-emv-vis sol-emv-v-validate" [class.is-active]="cycle.index() === 3">
              <img class="sol-i-sheet" solArt="test-document" sizes="260px">
              <img class="sol-i-shield" solArt="shield" sizes="175px">
              <span class="sol-emv-docscan"></span>
            </div>
            <div class="sol-emv-vis sol-emv-v-submit" [class.is-active]="cycle.index() === 4">
              <img class="sol-i-folder" solArt="submission-folder" sizes="270px">
              <svg class="sol-emv-upload" viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="2.5">
                <path d="M32 50V12M17 28l15-16 15 16M13 45v9h38v-9" />
              </svg>
            </div>
            <div class="sol-emv-vis sol-emv-v-support" [class.is-active]="cycle.index() === 5">
              <img class="sol-i-terminal" solArt="terminal" sizes="225px">
              <span class="sol-emv-link"></span>
              <img class="sol-i-server" solArt="server" sizes="200px">
            </div>
          </div>
        </div>
      </div>
      <div class="sol-foot-row sol-emv-status sol-mono">
        <span>{{ cycle.paused() ? 'Select a step' : 'Running through the process' }}</span>
        <span>{{ pad(cycle.index()) }} / 06</span>
      </div>
    </section>

    <section class="sol-sec" id="expertise" data-sect="Deep Technical Expertise" uiReveal [uiRevealDelay]="250" solLive>
      <ui-section-heading heading="Cryptography, contactless, schemes and host APIs in one team"
                          sub="The disciplines a certification programme actually touches, in one team." />
      <div class="sol-grid3">
        @for (c of expertise; track c.title; let i = $index) {
          <sol-bento class="sol-glass ds-item" [class.sol-glass--lit]="!!c.at" [style.--at]="c.at"
                     [style.--d]="500 + (i >= 3 ? 370 : 0) + (i % 3) * 120" [heading]="c.title" [desc]="c.desc">
            @switch (i) {
              @case (0) {
                <div class="sol-emv-ex" aria-hidden="true"><img solArt="shield" sizes="138px"></div>
              }
              @case (1) {
                <div class="sol-emv-ex sol-emv-ex--nfc" aria-hidden="true">
                  <img solArt="chip" sizes="126px">
                  <div class="sol-emv-rings">@for (n of [0, 1, 2]; track n) { <i [style.--i]="n"></i> }</div>
                </div>
              }
              @case (2) {
                <div class="sol-emv-ex" aria-hidden="true"><img solArt="test-document" sizes="138px"></div>
              }
              @case (3) {
                <div class="sol-emv-ex sol-emv-ex--perf" aria-hidden="true">
                  <svg viewBox="0 0 220 90">
                    <path class="sol-emv-wave-grid" d="M0 25h220M0 45h220M0 65h220M30 0v90M70 0v90M110 0v90M150 0v90M190 0v90" />
                    <path class="sol-emv-wave-base" d="M0 50h20l12-22 13 42 20-58 20 67 16-44 14 15h20l12-12 18 30 20-49 15 31h20" />
                    <path class="sol-emv-wave-live" d="M0 50h20l12-22 13 42 20-58 20 67 16-44 14 15h20l12-12 18 30 20-49 15 31h20" />
                  </svg>
                </div>
              }
              @case (4) {
                <div class="sol-emv-ex sol-emv-ex--int" aria-hidden="true">
                  <img solArt="terminal" sizes="124px"><img solArt="server" sizes="130px">
                </div>
              }
              @default {
                <div class="sol-emv-ex" aria-hidden="true">
                  <img solArt="test-document" sizes="138px"><span class="sol-emv-qa">✓</span>
                </div>
              }
            }
          </sol-bento>
        }
      </div>
    </section>

    <section class="sol-sec sol-sec--faq" id="faq" data-sect="faq" uiReveal [uiRevealDelay]="250">
      <ui-section-heading heading="Certification questions"
                          sub="Scope, kernels, lab submission and what happens after sign-off." />
      <div class="sol-faq"><ui-accordion [items]="faq" [stagger]="110" [base]="500" /></div>
    </section>

    <aside class="pro-nudge sol-pro">
      <span class="badge badge--teal badge--mono">Pro</span>
      <p>Testing with a team, or certifying with a scheme? Pro raises the CPS ceiling, unlocks the full algorithm set and deep simulator tweaks, plus hosted endpoints and priority support.</p>
      <a routerLink="/pro">Register for Pro<ui-icon name="arrow-right" [size]="14" /></a>
    </aside>

    <app-lead-form surface="emv-certification"
                   heading="Scope your certification"
                   lede="Tell us about the terminal, kernel or host project and we will map the route to sign-off." />

    <sol-close sect="Ready to certify?" kicker="Your next milestone" cta="Scope your certification"
               text="Tell us about your terminal, kernel or host project. We map the fastest route to certification and where Studio tooling removes lab cycles.">
      <div solCloseFoot class="sol-facts">
        <span class="sol-fact"><ui-icon name="cpu" [size]="17" />Kernel-level</span>
        <span class="sol-fact"><ui-icon name="seal-check" [size]="17" />Lab-ready evidence</span>
        <span class="sol-fact"><ui-icon name="clock-counter-clockwise" [size]="17" />Re-certification</span>
      </div>
      <div class="sol-emv-close">
        <img class="sol-i-base" solArt="base" sizes="(max-width: 900px) 90vw, 540px">
        <img class="sol-i-glass" solArt="glass-plate" sizes="(max-width: 900px) 60vw, 330px">
        <img class="sol-i-shield" solArt="shield" sizes="130px">
      </div>
    </sol-close>
  `,
})
export class EmvCertificationPage {
  protected readonly releases = EXTERNAL.releases;
  protected readonly levels = LEVELS;
  protected readonly steps = STEPS;
  protected readonly expertise = EXPERTISE;
  protected readonly faq = FAQ;
  protected readonly marks = MARKS;
  protected readonly sparks = SPARKS;

  private readonly scene = viewChild<ElementRef<HTMLElement>>('scene');
  private readonly stepButtons = viewChildren<ElementRef<HTMLButtonElement>>('stepBtn');

  /** Steps advance every 4.2 s; choosing one holds it for 14 s, then the run resumes. */
  protected readonly cycle = solCycle(
    { count: STEPS.length, interval: 4200, resumeAfter: 14000 },
    () => this.scene()?.nativeElement,
  );

  protected pad(i: number): string {
    return String(i + 1).padStart(2, '0');
  }

  /** Arrow keys, Home and End move between the steps, as in a tab list. */
  protected onKey(event: KeyboardEvent, i: number): void {
    const last = STEPS.length - 1;
    let next: number;
    switch (event.key) {
      case 'ArrowRight': next = i === last ? 0 : i + 1; break;
      case 'ArrowLeft': next = i === 0 ? last : i - 1; break;
      case 'Home': next = 0; break;
      case 'End': next = last; break;
      default: return;
    }
    event.preventDefault();
    this.cycle.pick(next);
    this.stepButtons()[next]?.nativeElement.focus();
  }
}
