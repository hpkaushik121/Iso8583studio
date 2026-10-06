import {
  ChangeDetectionStrategy, Component, DestroyRef, ElementRef, PLATFORM_ID, afterNextRender,
  inject, viewChild,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { EXTERNAL } from '../../core/site-nav';
import {
  AccordionItem, UiAccordion, UiCtaPanel, UiIcon, UiReveal, UiSectionHeading, UiWords,
} from '../../ui';
import { HomeHeroDashboard } from '../home/hero-dashboard';
import { HomeJourney } from '../home/journey';
import { HomePrismBeam } from '../home/prism-beam';
import { HomeScrollDots } from '../home/scroll-dots';
import { SitePage } from './site-page';
import { ProReserve } from '../pro/pro-reserve';

/**
 * The nine simulators in the strip under the hero. `short` is the label the
 * design shows; `pre`/`post` complete it to the simulator's full name for
 * screen readers — and for analytics, which reports the .st-name text content
 * as sim_board_engage's simulator_type and has always sent the full name.
 */
interface Sim { icon: string; short: string; pre?: string; post?: string; }

const SIMULATORS: Sim[] = [
  { icon: 'device-mobile-camera', short: 'POS', post: ' Simulator' },
  { icon: 'identification-card', short: 'APDU', post: ' Simulator' },
  { icon: 'receipt', short: 'ECR', post: ' Simulator' },
  { icon: 'arrows-left-right', short: 'Switch', post: ' Simulator' },
  { icon: 'hard-drives', short: 'Host', post: ' Simulator' },
  { icon: 'globe-hemisphere-west', short: 'Scheme', post: ' Simulator' },
  { icon: 'lock-key', short: 'HSM', post: ' Simulator' },
  { icon: 'terminal-window', short: 'Console', pre: 'HSM Command ' },
  { icon: 'squares-four', short: 'ATM', post: ' Simulator' },
];

const PERSONAS = [
  { n: '01', phase: 'Develop', title: 'Develop against simulators', desc: 'No test host? Simulate one. Build against local host, HSM and scheme endpoints with realistic responses.', who: 'For payment developers' },
  { n: '02', phase: 'Test', title: 'Test to the bit', desc: 'Craft edge cases field by field, validate cryptograms and MACs, replay reversals. Deterministic and logged.', who: 'For QA & test engineers' },
  { n: '03', phase: 'Certify', title: 'Fewer lab re-submissions', desc: 'EMV L2/L3 and scheme certification prep with kernel-level tooling. Cut lab time and re-submission cycles.', who: 'For certification teams' },
];

const CRYPTO_TILES: [icon: string, label: string][] = [
  ['key', 'DUKPT'], ['lock-key', 'TR-31'], ['fingerprint', 'MAC'], ['shuffle', '3DES'],
  ['cube', 'AES'], ['hash', 'KCV'], ['lock', 'RSA'], ['binary', 'SHA'],
];

const TRACE: [field: string, value: string][] = [
  ['MTI', '0200'], ['P-02', '4761 7300 0000 0018'], ['P-03', '00 00 00'],
  ['P-04', '0000 0000 1250'], ['P-55', '9F26 08 A1 B2 C3 D4 …'],
];

const KEYS: [tag: string, label: string, hex: string][] = [
  ['BDK', 'Base derivation key', '0123 4567 89AB CDEF'],
  ['IPEK', 'Initial PIN encryption key', '6AC2 92FA A131 5B4D'],
  ['TXN', 'Transaction key · KSN 00031', 'C3F8 1E77 9D02 44A1'],
  ['PIN', 'PIN block · ISO 9564 format 4', '4C2E 8A19 77B3 0F5D'],
];

const KEY_TAGS = ['TR-31', 'Thales', 'Futurex', 'KCV', 'Shares'];

/** The six disciplines of the studio sidebar; the longest bar is the largest count. */
const CATEGORIES = [
  { name: 'Payment Simulators', n: 9, link: '/simulator' },
  { name: 'EMV & Card Tools', n: 12, link: '/tools/emv-tools' },
  { name: 'Cryptographic Tools', n: 7, link: '/tools/cipher-tools' },
  { name: 'Key Management', n: 10, link: '/tools/key-tools' },
  { name: 'Payment Utilities', n: 21, link: '/tools/pin-tools' },
  { name: 'Data Converters', n: 6, link: '/tools/utility-tools' },
].map((c) => ({ ...c, width: `${Math.round((c.n / 21) * 100)}%` }));

const SOLUTIONS = [
  { icon: 'certificate', title: 'EMV Certification', desc: 'End-to-end L1/L2/L3 and scheme certification, from test plans to sign-off.', link: '/emv-certification' },
  { icon: 'cloud', title: 'Cloud Simulators', desc: 'Hosted host & HSM endpoints for CI pipelines and distributed teams.', link: '/cloud-simulators' },
  { icon: 'arrows-left-right', title: 'Payment Middleware', desc: 'Switching, routing and protocol translation built on the Studio engine.', link: '/middleware' },
  { icon: 'cpu', title: 'Kernel Development', desc: 'EMV L2 kernel engineering for terminals, from contact to contactless.', link: '/kernel' },
];

const FAQ: AccordionItem[] = [
  { q: 'What is ISO8583Studio?', a: 'A desktop workbench for payment engineers: nine simulators (host, HSM, POS, APDU, switch, issuer, scheme, ATM, ECR) and 64 tools for ISO 8583, EMV, PIN, MAC and key work — on Windows, macOS and Linux, free and open source under the GNU AGPL v3.' },
  { q: 'Can I test ISO 8583 transactions without a production host?', a: 'Yes. Stand up an authorization host on your laptop, script any MTI and response code, and run a full 0200 → 0210 flow with no bank connection, no lab booking and no scheme contact.' },
  { q: 'Do I need a real payShield HSM to test PIN blocks and MACs?', a: 'No. The HSM Simulator implements payShield 10K host commands for key management, PIN block translation and ISO 9797 MAC verification, and its output matches a real appliance byte for byte.' },
  { q: 'Does it help with EMV L2/L3 certification?', a: 'That is what it is built for. Drive APDU dialogues command by command, work through certification test cases, validate cryptograms and capture logs — so you cut lab time and re-submission cycles before you book a slot.' },
  { q: 'Which protocols and message formats are supported?', a: 'Binary ISO 8583, hexadecimal, JSON, XML, key-value and YAML mapping, carried over TCP/IP, RS232, REST or dial-up — mapped on the way in and out.' },
  { q: 'Can I run the simulators in CI?', a: 'Yes. Cloud Simulators expose hosted host and HSM endpoints so pipelines and distributed teams point at a stable URL instead of standing up an environment per branch.' },
  { q: 'Is ISO8583Studio really free?', a: 'The desktop studio is free and open source under the GNU AGPL v3 — no trial, no seat limit. Pro adds hosted endpoints, a higher CPS ceiling, the full algorithm set and priority support.' },
];

/** Radius of the pointer's pool of light on the world map, in px. */
const LIGHT_RADIUS = 210;

@Component({
  selector: 'page-home',
  imports: [
    RouterLink, UiAccordion, UiCtaPanel, UiIcon, UiReveal, UiSectionHeading, UiWords,
    HomeHeroDashboard, HomeJourney, HomePrismBeam, HomeScrollDots, ProReserve,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  hostDirectives: [SitePage],
  host: { class: 'static-page page-home' },
  template: `
    <section #hero class="hero" data-sect="hero">
      <div #dots class="hp-dots" aria-hidden="true">
        <div class="hp-dots-glow"></div>
        <div class="hp-dots-map"></div>
        <div class="hp-dots-hot"></div>
      </div>

      <div class="hp-hero-copy">
        <span class="lit-capsule hp-hero-stat ds-fade" [style.--d]="250">9 simulators<span class="hp-hero-stat-rule" aria-hidden="true"></span>64 tools</span>
        <h1 class="ds-in"><ui-words text="The payment engineer's workbench" [base]="350" /></h1>
        <p class="hp-hero-line ds-fade" [style.--d]="900">Every field. Every bit. Nothing hidden.</p>
        <p class="hp-hero-sub ds-fade" [style.--d]="1100">Develop, test and certify ISO 8583 integrations without a production host, HSM or card in sight.</p>
        <div class="hero-ctas ds-fade" [style.--d]="1300">
          <a class="btn btn--primary btn--glow" [href]="releases">Download Studio<ui-icon name="arrow-up-right" [size]="16" /></a>
          <a class="btn btn--ghost" routerLink="/" fragment="toolbox">Browse the tools</a>
        </div>
      </div>

      <div class="hp-stage">
        <home-prism-beam [frame]="frame" />
        <div #frame class="hp-frame ds-frame" [style.--d]="1700">
          <div class="hp-frame-edge" aria-hidden="true"></div>
          <home-hero-dashboard [delay]="2700" />
          <div class="hp-frame-fade" aria-hidden="true"></div>
        </div>
      </div>
    </section>

    <div class="hp-strip" uiReveal>
      <div class="ds-hold">
        <p class="hp-strip-caption">Nine simulators ship in the box · open source under AGPL v3</p>
        <div class="hp-marquee" id="simGrid">
          <div class="hp-marquee-track ds-marquee">
            @for (s of simulators; track s.short) {
              <span class="hp-mark simtile">
                <ui-icon [name]="s.icon" />
                <span class="hp-mark-name st-name">@if (s.pre) {<span class="visually-hidden">{{ s.pre }}</span>}{{ s.short }}@if (s.post) {<span class="visually-hidden">{{ s.post }}</span>}</span>
              </span>
              <span class="hp-mark-rule" aria-hidden="true"></span>
            }
            <!-- The second copy only exists so the loop has no seam. -->
            @for (s of simulators; track s.short) {
              <span class="hp-mark" aria-hidden="true">
                <ui-icon [name]="s.icon" />
                <span class="hp-mark-name">{{ s.short }}</span>
              </span>
              <span class="hp-mark-rule" aria-hidden="true"></span>
            }
          </div>
        </div>
      </div>
    </div>

    <home-journey />

    <section class="hp-sect" data-sect="lifecycle" uiReveal [uiRevealDelay]="250">
      <ui-section-heading align="left" heading="Develop → Test → Certify"
        sub="For the engineer writing the integration, the QA team proving it, and the manager signing it off." />
      <div class="hp-personas">
        <div class="hp-personas-line" aria-hidden="true"></div>
        <div class="hp-personas-grid">
          @for (p of personas; track p.n; let i = $index) {
            <div class="hp-persona ds-item" [style.--d]="500 + i * 110">
              <span class="hp-persona-dot" aria-hidden="true"></span>
              <p class="hp-persona-phase">{{ p.n }} / {{ p.phase }}</p>
              <h3>{{ p.title }}</h3>
              <p class="hp-persona-desc">{{ p.desc }}</p>
              <p class="hp-persona-who">{{ p.who }}</p>
            </div>
          }
        </div>
      </div>
    </section>

    <section id="toolbox" class="hp-sect" data-sect="toolbox" uiReveal [uiRevealDelay]="250">
      <ui-section-heading heading="What used to need a lab"
        sub="The parts of the job that usually need a lab, a scheme contact and three weeks of waiting." />
      <div class="hp-bento-grid">
        <div class="hp-bento hp-bento--3 ds-item" [style.--d]="500" [style.--at]="'88% 6%'">
          <div class="hp-bento-head">
            <h3>Cryptographic workbench</h3>
            <p>DUKPT, TR-31, ISO 9797 MAC, 3DES and AES in one panel, checked against payShield output.</p>
          </div>
          <div class="hp-bento-body">
            <div class="hp-tiles">
              @for (t of cryptoTiles; track t[1]) {
                <div class="hp-tile"><ui-icon [name]="t[0]" [size]="18" /><span>{{ t[1] }}</span></div>
              }
            </div>
          </div>
        </div>

        <div class="hp-bento hp-bento--3 ds-item" [style.--d]="620" [style.--at]="'6% 96%'">
          <div class="hp-bento-head">
            <h3>Byte-level trace</h3>
            <p>Every field, subfield and bitmap beside the raw hex it came from.</p>
          </div>
          <div class="hp-bento-body">
            <div class="hp-trace">
              @for (line of trace; track line[0]) { <div><span>{{ line[0] }}</span> {{ line[1] }}</div> }
            </div>
          </div>
        </div>

        <div class="hp-bento hp-bento--2 hp-bento--plain ds-item" [style.--d]="870">
          <div class="hp-bento-head">
            <h3>EMV kernel tooling</h3>
            <p>APDU scripting, CVM lists and TLV editing against a live card or a simulated one.</p>
          </div>
        </div>
        <div class="hp-bento hp-bento--2 ds-item" [style.--d]="990" [style.--at]="'50% 108%'">
          <div class="hp-bento-head">
            <h3>Hosted endpoints</h3>
            <p>Point your CI at a cloud host or HSM instead of standing one up per branch.</p>
          </div>
        </div>
        <div class="hp-bento hp-bento--2 ds-item" [style.--d]="1110" [style.--at]="'104% 52%'">
          <div class="hp-bento-head">
            <h3>Deterministic replay</h3>
            <p>Re-run yesterday's failing reversal with the same keys, clock and responses.</p>
          </div>
        </div>

        <div class="hp-bento hp-bento--3 ds-item" [style.--d]="1150" [style.--at]="'-4% 12%'">
          <div class="hp-bento-head">
            <h3>Key management</h3>
            <p>DUKPT derivation, TR-31 key blocks, key shares, KCVs, Thales and Futurex formats.</p>
          </div>
          <div class="hp-bento-body">
            <div class="hp-keys">
              @for (k of keys; track k[0]; let first = $first) {
                <div class="hp-key" [class.is-on]="first">
                  <span class="hp-key-tag">{{ k[0] }}</span>
                  <span><span class="hp-key-label">{{ k[1] }}</span><span class="hp-key-hex">{{ k[2] }}</span></span>
                </div>
              }
              <div class="hp-key-tags">
                @for (t of keyTags; track t) { <span class="ui-tag">{{ t }}</span> }
              </div>
            </div>
          </div>
        </div>

        <div class="hp-bento hp-bento--3 ds-item" [style.--d]="1270" [style.--at]="'74% 104%'">
          <div class="hp-bento-head">
            <h3>64 tools, six disciplines</h3>
            <p>The same categories as the studio sidebar — every tool documented, every workflow covered.</p>
          </div>
          <div class="hp-bento-body">
            <div class="hp-count">
              <div class="hp-count-top">
                <strong>64</strong>
                <span>tools shipped<br>in the studio</span>
              </div>
              <div class="hp-cats">
                @for (c of categories; track c.name) {
                  <a class="cat" [routerLink]="c.link">
                    <h3>{{ c.name }}</h3>
                    <span class="hp-cat-bar" aria-hidden="true"><span [style.--w]="c.width"></span></span>
                    <span class="hp-cat-n">{{ c.n }}</span>
                  </a>
                }
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="hp-sect" data-sect="solutions" uiReveal [uiRevealDelay]="250">
      <ui-section-heading heading="Solutions & services" />
      <div class="hp-sol-grid">
        @for (s of solutions; track s.title; let i = $index) {
          <a class="sol ds-item" [routerLink]="s.link" [style.--d]="500 + i * 110">
            <ui-icon [name]="s.icon" [size]="22" />
            <h3>{{ s.title }}</h3>
            <span class="hp-sol-desc">{{ s.desc }}</span>
            <span class="hp-sol-go">Learn more →</span>
          </a>
        }
      </div>
    </section>

    <section class="hp-sect hp-pricing" data-sect="pricing" uiReveal [uiRevealDelay]="250">
      <ui-section-heading eyebrow="Early access" heading="Pre-register for Pro"
        sub="Testing with a team, or certifying with a scheme? Pro raises the ceiling and adds hosted endpoints. Reserve a seat today to lock the founder rate." />
      <app-pro-reserve class="hp-reserve" [stagger]="true" fine="Secure checkout · refundable until launch" />
    </section>

    <section class="hp-sect hp-faq" data-sect="faq" uiReveal [uiRevealDelay]="250">
      <ui-section-heading heading="Frequently asked questions"
        sub="ISO 8583 testing, EMV certification and HSM simulation — answered." />
      <ui-accordion [items]="faq" [stagger]="110" [base]="500" />
    </section>

    <div class="hp-cta" uiReveal [uiRevealDelay]="250">
      <ui-cta-panel>
        <h2><ui-words [text]="ctaTitle" /></h2>
        <p class="ds-hold" [style.--d]="250">Free and open source under the GNU AGPL v3. Download the studio, or star the repo and follow the roadmap.</p>
        <div class="cta-actions ds-hold" [style.--d]="500">
          <a class="btn btn--primary" [href]="releases">Download Studio<ui-icon name="arrow-up-right" [size]="16" /></a>
          <a class="btn btn--secondary" [href]="repo" target="_blank" rel="noopener"><ui-icon name="star" [size]="16" />Star on GitHub</a>
        </div>
        <div class="hp-os ds-hold" [style.--d]="600">
          <span><ui-icon name="apple-logo" [size]="17" />macOS</span>
          <span><ui-icon name="windows-logo" [size]="17" />Windows</span>
          <span><ui-icon name="linux-logo" [size]="17" />Linux</span>
        </div>
      </ui-cta-panel>
    </div>

    <home-scroll-dots />
  `,
})
export class HomePage {
  protected readonly releases = EXTERNAL.releases;
  protected readonly repo = EXTERNAL.repo;
  protected readonly simulators = SIMULATORS;
  protected readonly personas = PERSONAS;
  protected readonly cryptoTiles = CRYPTO_TILES;
  protected readonly trace = TRACE;
  protected readonly keys = KEYS;
  protected readonly keyTags = KEY_TAGS;
  protected readonly categories = CATEGORIES;
  protected readonly solutions = SOLUTIONS;
  protected readonly faq = FAQ;
  protected readonly ctaTitle = 'Download the studio.\nSend your first 0200.';

  private readonly hero = viewChild.required<ElementRef<HTMLElement>>('hero');
  private readonly dots = viewChild.required<ElementRef<HTMLElement>>('dots');

  constructor() {
    if (!isPlatformBrowser(inject(PLATFORM_ID))) return;
    const destroyRef = inject(DestroyRef);
    afterNextRender(() => destroyRef.onDestroy(this.lightTheMap()));
  }

  /**
   * The dotted world map behind the hero brightens in a pool around the
   * pointer. A small enhancement: the bright copy of the map is not even
   * requested until the pointer first moves over the hero, and none of it runs
   * under reduced motion or on a device without a fine pointer.
   */
  private lightTheMap(): () => void {
    const hero = this.hero().nativeElement;
    const layer = this.dots().nativeElement;
    if (matchMedia('(prefers-reduced-motion: reduce)').matches ||
        !matchMedia('(hover: hover) and (pointer: fine)').matches) return () => {};

    let raf = 0;
    let x = 0;
    let y = 0;
    let lit = false;

    const paint = () => {
      raf = 0;
      layer.style.setProperty('--mx', `${x}px`);
      layer.style.setProperty('--my', `${y}px`);
      layer.style.setProperty('--mr', `${LIGHT_RADIUS}px`);
      layer.classList.add('is-armed');
      layer.classList.toggle('is-lit', lit);
    };
    const move = (e: PointerEvent) => {
      const r = hero.getBoundingClientRect();
      x = e.clientX - r.left;
      y = e.clientY - r.top;
      lit = true;
      if (!raf) raf = requestAnimationFrame(paint);
    };
    const leave = () => {
      lit = false;
      if (!raf) raf = requestAnimationFrame(paint);
    };

    hero.addEventListener('pointermove', move, { passive: true });
    hero.addEventListener('pointerleave', leave);
    return () => {
      hero.removeEventListener('pointermove', move);
      hero.removeEventListener('pointerleave', leave);
      if (raf) cancelAnimationFrame(raf);
    };
  }
}
