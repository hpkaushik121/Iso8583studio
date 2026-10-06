import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EXTERNAL } from '../../core/site-nav';
import {
  AccordionItem, BadgeTone, ButtonVariant, UiAccordion, UiBadge, UiBreadcrumb, UiButton, UiCallout,
  UiCtaPanel, UiDialog, UiFigure, UiIcon, UiIconButton, UiLogoMark, UiReveal, UiSectionHeading,
  UiSpecRow, UiTable, UiTag, UiWords,
} from '../../ui';

interface Swatch { name: string; token: string; }
interface TypeStep { token: string; label: string; }

/**
 * Living styleguide: every primitive in every variant, on one page. Not
 * indexed and not in the sitemap — it exists so the system stays visible as
 * pages get added.
 *
 * Its route loads no stylesheet bundle, so the page leans on the global
 * primitives and keeps only its own layout in the component styles.
 */
@Component({
  selector: 'app-design-system',
  imports: [
    RouterLink, UiAccordion, UiBadge, UiBreadcrumb, UiButton, UiCallout, UiCtaPanel, UiDialog, UiFigure,
    UiIcon, UiIconButton, UiLogoMark, UiReveal, UiSectionHeading, UiSpecRow, UiTable, UiTag, UiWords,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`
    .dsp { max-width: var(--wrap-max); margin: 0 auto; padding: calc(var(--nav-h) + var(--sp-12)) var(--gutter) var(--sp-16); }
    .dsp-meta { margin: var(--sp-6) 0 0; font-family: var(--mono); font-size: var(--fs-xs); letter-spacing: .02em; color: var(--faint); }
    .dsp-title { margin: var(--sp-4) 0 0; font-size: var(--fs-hero); font-weight: 500; letter-spacing: var(--ls-tight); line-height: 1.1; color: var(--text); }
    .dsp-lede, .dsp-note { max-width: 68ch; color: var(--muted); line-height: 1.65; text-wrap: pretty; }
    .dsp-lede { margin: var(--sp-4) 0 0; font-size: var(--fs-md); }
    .dsp-note { margin: var(--sp-4) 0 0; font-size: var(--fs-base); }
    .dsp-sec { padding-top: var(--sp-16); }
    .dsp-row { display: flex; flex-wrap: wrap; align-items: center; gap: var(--sp-3); margin-top: var(--sp-5); }
    .dsp-row--end { align-items: flex-end; gap: var(--sp-8); }
    .dsp-cap { display: flex; flex-direction: column; align-items: center; gap: var(--sp-3); font-family: var(--mono); font-size: var(--fs-2xs); color: var(--faint); }
    .dsp-cap .ui-icon { color: var(--text); }
    .dsp-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: var(--sp-3); margin-top: var(--sp-5); }
    .dsp-grid--cards { grid-template-columns: repeat(auto-fit, minmax(230px, 1fr)); gap: 14px; }
    .dsp-swatch { overflow: hidden; border: 1px solid var(--line); border-radius: var(--r-md); font-family: var(--mono); font-size: var(--fs-xs); color: var(--muted); }
    .dsp-chip { display: block; height: 56px; background: var(--c); }
    .dsp-token { display: block; padding: var(--sp-2) var(--sp-3); }
    .dsp-type { margin: var(--sp-3) 0 0; font-size: var(--fs); line-height: 1.2; color: var(--text); }
    .dsp-type span { margin-left: var(--sp-3); font-family: var(--mono); font-size: var(--fs-xs); color: var(--faint); }
    .dsp-card-title { display: block; font-size: var(--fs-lg); font-weight: 500; letter-spacing: var(--ls-snug); color: var(--text); }
    .dsp-card-desc { display: block; margin-top: var(--sp-2); color: var(--muted); font-size: var(--fs-sm); line-height: 1.6; }
    .dsp-block { margin-top: var(--sp-5); max-width: 760px; }
    .dsp-frame { margin-top: var(--sp-5); padding: var(--sp-8); border: 1px dashed var(--line); border-radius: var(--r-lg); }
    .dsp-cta { padding-top: var(--sp-8); }
  `],
  template: `
    <div class="dsp">
      <ui-breadcrumb [items]="[{ label: 'Home', link: '/' }, { label: 'Design system' }]" />
      <p class="dsp-meta">Internal reference · not indexed</p>
      <h1 class="dsp-title"><ui-words text="Design system" /></h1>
      <p class="dsp-lede">Every primitive the site is built from, in every variant it supports. If a page
         needs something that is not here, it belongs here first.</p>

      <section class="dsp-sec" id="mark">
        <ui-section-heading heading="Brand mark" align="left" size="h2" />
        <p class="dsp-note">The S is two arcs and the letter is 180&deg; rotationally symmetric, so a
           half turn lands back on itself. Every state is those same two paths: hover the tile for
           the turn, and note that the loading ring circles the mark while the processing trace
           runs along it &mdash; motion beside the work versus motion following it.</p>
        <div class="dsp-row dsp-row--end">
          <div class="dsp-cap"><ui-logo-mark [size]="40" />brand</div>
          <div class="dsp-cap"><ui-logo-mark mode="spin" [size]="40" />spin &middot; loading</div>
          <div class="dsp-cap"><ui-logo-mark mode="trace" [size]="40" />trace &middot; processing</div>
          <div class="dsp-cap"><ui-logo-mark mode="still" [size]="40" />still</div>
          <div class="dsp-cap"><ui-logo-mark [size]="16" />16px</div>
        </div>
      </section>

      <section class="dsp-sec" id="colour">
        <ui-section-heading heading="Colour" align="left" size="h2" sub="Tokens from styles/_tokens.css. Pages never write a literal colour." />
        <div class="dsp-grid">
          @for (s of swatches; track s.token) {
            <div class="dsp-swatch">
              <span class="dsp-chip" [style.--c]="'var(' + s.token + ')'"></span>
              <span class="dsp-token">{{ s.token }}</span>
            </div>
          }
        </div>
      </section>

      <section class="dsp-sec" id="type">
        <ui-section-heading heading="Type scale" align="left" size="h2" />
        @for (t of typeScale; track t.token) {
          <p class="dsp-type" [style.--fs]="'var(' + t.token + ')'">{{ t.label }}<span>{{ t.token }}</span></p>
        }
      </section>

      <section class="dsp-sec" id="buttons">
        <ui-section-heading heading="Buttons" align="left" size="h2"
                            sub="ui-button. Anything that navigates is given routerLink or href so it renders an anchor." />
        <div class="dsp-row">
          @for (v of buttonVariants; track v) { <ui-button [variant]="v">{{ v }}</ui-button> }
          <ui-button [disabled]="true">disabled</ui-button>
        </div>
        <div class="dsp-row">
          <ui-button size="sm">Small</ui-button>
          <ui-button>Medium</ui-button>
          <ui-button size="lg">Large</ui-button>
          <ui-button variant="secondary" size="sm">Small</ui-button>
          <ui-button variant="secondary" size="lg">Large</ui-button>
        </div>
        <div class="dsp-row">
          <ui-button icon="download-simple">Icon left</ui-button>
          <ui-button iconRight="arrow-up-right">Icon right</ui-button>
          <ui-button iconRight="arrow-up-right" [glow]="true">Glow</ui-button>
          <ui-button variant="secondary" iconRight="arrow-right">Secondary</ui-button>
          <ui-button variant="outline" icon="github-logo">Outline</ui-button>
        </div>
        <div class="dsp-row">
          <ui-button routerLink="/docs" iconRight="arrow-right">Internal link</ui-button>
          <ui-button variant="secondary" href="https://github.com/hpkaushik121/Iso8583studio" [external]="true"
                     iconRight="arrow-up-right">External link</ui-button>
        </div>
        <div class="dsp-block"><ui-button variant="secondary" [block]="true">Block</ui-button></div>
      </section>

      <section class="dsp-sec" id="badges">
        <ui-section-heading heading="Badges and tags" align="left" size="h2"
                            sub="ui-badge for states, counts and deltas; ui-tag for flat keyword chips." />
        <div class="dsp-row">
          @for (tone of badgeTones; track tone) { <ui-badge [tone]="tone">{{ tone }}</ui-badge> }
        </div>
        <div class="dsp-row">
          <ui-badge tone="teal" [dot]="true">Available</ui-badge>
          <ui-badge tone="blue" [dot]="true">Beta</ui-badge>
          <ui-badge tone="neutral">In development</ui-badge>
          <ui-badge tone="blue" icon="sparkle">Pro</ui-badge>
          <ui-badge tone="up" icon="arrow-up-right" [mono]="true">+12.4%</ui-badge>
          <ui-badge tone="neutral" [mono]="true">v1.0.14</ui-badge>
        </div>
        <div class="dsp-row">
          <ui-tag>plain</ui-tag><ui-tag tone="blue">blue</ui-tag><ui-tag tone="teal">teal</ui-tag><ui-tag tone="muted">muted</ui-tag>
        </div>
      </section>

      <section class="dsp-sec" id="icons">
        <ui-section-heading heading="Icons" align="left" size="h2"
                            sub="ui-icon draws Phosphor glyphs from the /icons.svg sprite; add -fill for the filled drawing. ui-icon-button is the icon-only control." />
        <div class="dsp-row dsp-row--end">
          <div class="dsp-cap"><ui-icon name="arrows-left-right" [size]="22" />arrows-left-right</div>
          <div class="dsp-cap"><ui-icon name="key" [size]="22" />key</div>
          <div class="dsp-cap"><ui-icon name="credit-card" [size]="22" />credit-card</div>
          <div class="dsp-cap"><ui-icon name="terminal-window" [size]="22" />terminal-window</div>
          <div class="dsp-cap"><ui-icon name="shield-check" [size]="22" />shield-check</div>
          <div class="dsp-cap"><ui-icon name="star" [size]="22" />star</div>
          <div class="dsp-cap"><ui-icon name="star-fill" [size]="22" />star-fill</div>
          <div class="dsp-cap"><ui-icon name="arrow-up-right" [size]="14" />14px</div>
          <div class="dsp-cap"><ui-icon name="arrow-up-right" [size]="32" />32px</div>
        </div>
        <div class="dsp-row">
          <ui-icon-button icon="gear" label="Settings" />
          <ui-icon-button icon="gear" label="Settings, filled" variant="filled" />
          <ui-icon-button icon="x" label="Close, round" variant="filled" [round]="true" />
          <ui-icon-button icon="copy" label="Copy, small" [size]="28" />
          <ui-icon-button icon="github-logo" label="GitHub repository" variant="filled" [round]="true"
                          href="https://github.com/hpkaushik121/Iso8583studio" />
        </div>
      </section>

      <section class="dsp-sec" id="cards">
        <ui-section-heading heading="Cards" align="left" size="h2"
                            sub=".card is a div or an anchor, never a section. Modifiers: --raised, --glow, --radial, --interactive." />
        <div class="dsp-grid dsp-grid--cards">
          <div class="card">
            <span class="dsp-card-title">.card</span>
            <span class="dsp-card-desc">Glass surface: hairline border, 12px radius, lit top edge.</span>
          </div>
          <div class="card card--raised">
            <span class="dsp-card-title">.card--raised</span>
            <span class="dsp-card-desc">One step up, on the surface fill.</span>
          </div>
          <div class="card card--glow">
            <span class="dsp-card-title">.card--glow</span>
            <span class="dsp-card-desc">A blue wash rising from the lower left.</span>
          </div>
          <div class="card card--radial">
            <span class="dsp-card-title">.card--radial</span>
            <span class="dsp-card-desc">Concentric rings behind the content.</span>
          </div>
          <a class="card card--interactive" routerLink="/docs">
            <span class="dsp-card-title">.card--interactive</span>
            <span class="dsp-card-desc">An anchor. Lifts on hover because it links somewhere.</span>
          </a>
        </div>
      </section>

      <section class="dsp-sec" id="headings">
        <ui-section-heading heading="Section heading and breadcrumb" align="left" size="h2" />
        <div class="dsp-frame">
          <ui-section-heading eyebrow="9 simulators" heading="Every party in the payment network"
                              sub="ui-section-heading, centred, display size, with an eyebrow and a subtitle. It reveals itself on scroll." />
        </div>
        <div class="dsp-frame">
          <ui-breadcrumb [items]="[{ label: 'Home', link: '/' }, { label: 'Documentation', link: '/docs' }, { label: 'This page' }]" />
        </div>
      </section>

      <section class="dsp-sec" id="accordion">
        <ui-section-heading heading="Accordion" align="left" size="h2"
                            sub="ui-accordion. Every answer stays in the DOM, open or not." />
        <div class="dsp-block" uiReveal><ui-accordion [items]="faq" [stagger]="120" /></div>
      </section>

      <section class="dsp-sec" id="callouts">
        <ui-section-heading heading="Callouts" align="left" size="h2" />
        <div class="dsp-block">
          @for (tone of calloutTones; track tone) {
            <ui-callout [tone]="tone" [heading]="tone + ' callout'">
              <p>Body copy inside a {{ tone }} callout.</p>
            </ui-callout>
          }
        </div>
      </section>

      <section class="dsp-sec" id="table">
        <ui-section-heading heading="Table and specs" align="left" size="h2" />
        <div class="dsp-block">
          <ui-table>
            <table>
              <thead><tr><th>Field</th><th>Type</th><th>Meaning</th></tr></thead>
              <tbody>
                <tr><td>MTI</td><td>n4</td><td>Message type indicator</td></tr>
                <tr><td>DE 3</td><td>n6</td><td>Processing code</td></tr>
                <tr><td>DE 39</td><td>an2</td><td>Response code</td></tr>
              </tbody>
            </table>
          </ui-table>
        </div>
        <div class="dsp-block">
          <ui-spec-row key="Protocol" value="TCP/IP, TLS 1.2+" />
          <ui-spec-row key="Framing" value="2-byte length header" />
          <ui-spec-row key="Character set" value="ASCII / EBCDIC" />
        </div>
      </section>

      <section class="dsp-sec" id="figure">
        <ui-section-heading heading="Figure" align="left" size="h2" />
        <div class="dsp-block">
          <ui-figure src="/images/app.png" alt="ISO8583Studio" caption="A figure with its caption" />
        </div>
      </section>

      <section class="dsp-sec" id="dialog">
        <ui-section-heading heading="Dialog" align="left" size="h2"
                            sub="ui-dialog. Focus is trapped inside, Escape closes it, and focus returns to the trigger." />
        <div class="dsp-row"><ui-button variant="secondary" (click)="dialogOpen.set(true)">Open dialog</ui-button></div>
        @if (dialogOpen()) {
          <ui-dialog label="Example dialog" (close)="dialogOpen.set(false)">
            <h3>Example dialog</h3>
            <p>Focus is trapped inside, Escape closes it, and focus returns to the trigger.</p>
            <div class="pm-actions">
              <ui-button variant="ghost" (click)="dialogOpen.set(false)">Cancel</ui-button>
              <ui-button (click)="dialogOpen.set(false)">Confirm</ui-button>
            </div>
          </ui-dialog>
        }
      </section>

      <!-- ui-cta-panel renders the page's closing <section class="cta">, so it
           sits outside the sections above rather than inside one. -->
      <div class="dsp-sec" id="cta-panel">
        <ui-section-heading heading="CTA panel" align="left" size="h2"
                            sub="ui-cta-panel: the closing band of a page. One per page; its links are anchors." />
        <div class="dsp-cta" uiReveal>
          <ui-cta-panel>
            <h2><ui-words text="Try it on your own transactions" /></h2>
            <p class="ds-hold" [style.--d]="300">Free and open source. Download the studio and run it on your desk in minutes.</p>
            <div class="cta-actions ds-hold" [style.--d]="420">
              <a class="btn btn--primary btn--glow" [href]="releases">Download Studio<ui-icon name="arrow-up-right" [size]="16" /></a>
              <a class="btn btn--secondary" routerLink="/docs/installation">Installation guide<ui-icon name="arrow-right" [size]="16" /></a>
            </div>
            <p class="cta-meta">cta-meta · mono footnote</p>
          </ui-cta-panel>
        </div>
      </div>
    </div>
  `,
})
export class DesignSystem {
  protected readonly dialogOpen = signal(false);
  protected readonly releases = EXTERNAL.releases;
  protected readonly buttonVariants: ButtonVariant[] = ['primary', 'secondary', 'ghost', 'outline'];
  protected readonly badgeTones: BadgeTone[] = ['neutral', 'blue', 'teal', 'green', 'yellow', 'purple', 'red', 'up', 'down', 'warn'];
  protected readonly calloutTones = ['note', 'tip', 'warn', 'danger'] as const;

  protected readonly faq: AccordionItem[] = [
    { q: 'Is the first row open?', a: 'Yes. defaultOpen is 0; pass -1 to start with every row closed.' },
    { q: 'Do closed answers reach crawlers?', a: 'They do. A row animates its height, it is never removed from the document.' },
    { q: 'Can the rows stagger in?', a: 'Set stagger (ms) and put the accordion inside a uiReveal block.' },
  ];

  protected readonly typeScale: TypeStep[] = [
    { token: '--fs-hero', label: 'Hero' },
    { token: '--fs-display', label: 'Display' },
    { token: '--fs-3xl', label: 'Section heading' },
    { token: '--fs-xl', label: 'Sub-heading' },
    { token: '--fs-md', label: 'Lede and large body' },
    { token: '--fs-base', label: 'Body text' },
    { token: '--fs-sm', label: 'Small: captions and meta' },
    { token: '--fs-xs', label: 'Extra small: mono labels' },
  ];

  protected readonly swatches: Swatch[] = [
    'bg-deep', 'bg', 'surface', 'card', 'card-hi', 'card-deep',
    'text', 'muted', 'faint',
    'blue', 'blue-hi', 'blue-lo', 'blue-deep',
    'teal', 'teal-hi', 'teal-lo',
    'green', 'green-hi', 'amber', 'amber-hi', 'yellow', 'red', 'red-hi', 'purple',
  ].map((name) => ({ name, token: `--${name}` }));
}
