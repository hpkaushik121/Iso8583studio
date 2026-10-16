import {
  ChangeDetectionStrategy, Component, DestroyRef, ElementRef, PLATFORM_ID,
  afterNextRender, computed, inject, input, signal,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { BadgeTone, Crumb, UiBadge, UiBreadcrumb, UiIcon, UiReveal, UiWords } from '../../../ui';

/**
 * The long-form shell: the layout shared by the documentation hub, the
 * installation / contributing / versions guides, the two legal pages and the
 * contact page. Ported from the prototype's Legal.jsx.
 *
 * Every rule these components rely on lives in styles/bundles/longform.css
 * under an `lf-` prefix, so a route using them must load the `longform`
 * bundle.
 *
 * A page is assembled as
 *
 *   <lf-header [crumbs]="…" [badge]="…" title="…" lede="…" [meta]="…" />
 *   <lf-body [toc]="toc">
 *     <section lfSection id="overview" n="01" heading="Overview">
 *       <p class="lf-p">…</p>
 *       <ul class="lf-ul lf-ul--def"><li><strong>Term:</strong> text</li></ul>
 *       <lf-note tone="blue" icon="info" title="Note">…</lf-note>
 *       <lf-code label="clone and build" [code]="snippet" />
 *     </section>
 *   </lf-body>
 *
 * Body copy is written as ordinary elements carrying block classes, so links
 * and inline code stay real markup:
 *
 *   p.lf-p            paragraph
 *   h3.lf-h3          sub-heading inside a section
 *   ul.lf-ul          plain list; add .lf-ul--def when every item opens with
 *                     a <strong>Term:</strong> (the prototype's DefList)
 *   ol.lf-steps       numbered steps; each <li> may hold a code.lf-step-code
 *   div.lf-table-wrap > table.lf-table
 *   div.lf-cards      grid of <lf-link-card>
 *   div.lf-foot       closing line: span.lf-foot-line + a.lf-foot-link
 *   code              inline code anywhere inside .lf-main
 */

export type LfTone = 'blue' | 'teal' | 'warn' | 'neutral';

export interface LfBadge { tone: BadgeTone; label: string; }

/** One card of the optional row under the hero. `icon` is a Phosphor name. */
export interface LfHighlight { icon: string; tone: LfTone; title: string; body: string; }

/**
 * One entry of the contents rail. `n` is the section number shown on legal
 * pages; `icon` and `count` are the docs-hub variant. `id` must be the id of
 * a section inside the same <lf-body>.
 */
export interface LfTocEntry {
  id: string;
  label: string;
  n?: string;
  icon?: string;
  count?: number | string;
}

/**
 * Page hero: breadcrumb, status badge, word-revealed h1, lede, meta pills and
 * an optional row of highlight cards.
 *
 * Inputs
 *   crumbs      breadcrumb trail (rendered by <ui-breadcrumb>, so clicks report
 *               breadcrumb_click)
 *   badge       optional status pill above the title
 *   title       the h1
 *   lede        one-paragraph introduction
 *   meta        [label, value] pills under the lede
 *   highlights  cards under the meta row
 *   layout      'stack' (default) · 'split' puts the lede beside the title ·
 *               'art' reserves a right-hand column for decoration
 *
 * Slots
 *   default          placed under the lede/meta in the text column (the docs
 *                    hub puts its filter field here)
 *   [lfHeaderArt]    the right-hand column of the 'art' layout; hidden at
 *                    ≤1040px and from assistive tech
 *
 * Renders <header>, not <section>, so it adds no section_view.
 */
@Component({
  selector: 'lf-header',
  imports: [UiBadge, UiBreadcrumb, UiIcon, UiWords],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { style: 'display: block' },
  template: `
    <header class="lf-hero page-hero" [class.lf-hero--split]="layout() === 'split'"
            [class.lf-hero--art]="layout() === 'art'">
      <span class="lf-hero-dots" aria-hidden="true"></span>
      <span class="lf-hero-glow" aria-hidden="true"></span>
      <div class="lf-hero-in">
        <div class="lf-hero-main">
          <div class="lf-hero-crumb ds-fade"><ui-breadcrumb [items]="crumbs()" /></div>
          @if (badge(); as b) {
            <div class="lf-hero-badge ds-fade"><ui-badge [tone]="b.tone" [dot]="true">{{ b.label }}</ui-badge></div>
          }
          <div class="lf-hero-head">
            <h1 class="lf-hero-title ds-in"><ui-words [text]="title()" [base]="200" /></h1>
            @if (lede()) { <p class="lf-hero-lede ds-fade" [style.--d]="140">{{ lede() }}</p> }
          </div>
          @if (meta().length) {
            <div class="lf-hero-meta ds-fade" [style.--d]="220">
              @for (m of meta(); track m[0]) {
                <span class="lf-meta"><span class="lf-meta-k">{{ m[0] }}</span><span class="lf-meta-v">{{ m[1] }}</span></span>
              }
            </div>
          }
          <ng-content />
          @if (highlights().length) {
            <div class="lf-highlights ds-in">
              @for (h of highlights(); track h.title; let i = $index) {
                <div class="lf-highlight ds-item" [style.--d]="700 + i * 120">
                  <span class="lf-ico lf-ico--{{ h.tone }}"><ui-icon [name]="h.icon" [size]="17" /></span>
                  <span class="lf-highlight-title">{{ h.title }}</span>
                  <span class="lf-highlight-body">{{ h.body }}</span>
                </div>
              }
            </div>
          }
        </div>
        @if (layout() === 'art') {
          <div class="lf-hero-art" aria-hidden="true"><ng-content select="[lfHeaderArt]" /></div>
        }
      </div>
    </header>
  `,
})
export class LfHeader {
  readonly crumbs = input.required<Crumb[]>();
  readonly badge = input<LfBadge | null>(null);
  readonly title = input.required<string>();
  readonly lede = input<string | null>(null);
  readonly meta = input<readonly (readonly [string, string])[]>([]);
  readonly highlights = input<readonly LfHighlight[]>([]);
  readonly layout = input<'stack' | 'split' | 'art'>('stack');
}

/**
 * The page column under the hero. With `toc` entries it is a two-column grid:
 * a sticky contents rail (a <details> disclosure at ≤1040px) beside the
 * content; without entries it is a single full-width column.
 *
 * Inputs
 *   toc        entries of the rail, in page order. May change at runtime (the
 *              docs hub drops groups its filter has emptied).
 *   tocLabel   heading of the rail and summary of the disclosure
 *
 * Slots
 *   default    the page's <section lfSection> blocks and anything after them
 *   [lfRail]   extra content under the rail's links (desktop only)
 *
 * Scroll-spy is a browser-only enhancement: the prerender marks the first
 * entry active. Links are router links to `<this page>#<id>`.
 */
@Component({
  selector: 'lf-body',
  imports: [RouterLink, UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { style: 'display: block' },
  template: `
    <div class="lf-body" [class.lf-body--bare]="!toc().length">
      @if (toc().length) {
        <details class="lf-toc-m" #disclosure>
          <summary>{{ tocLabel() }}</summary>
          <div class="lf-toc-m-list">
            @for (e of toc(); track e.id) {
              <a [routerLink]="[]" [fragment]="e.id" (click)="disclosure.open = false">
                @if (e.icon) { <ui-icon [name]="e.icon" [size]="15" /> }
                @if (e.n) { <span class="lf-toc-n">{{ e.n }}</span> }
                <span>{{ e.label }}</span>
                @if (e.count !== undefined) { <span class="lf-toc-count">{{ e.count }}</span> }
              </a>
            }
          </div>
        </details>
        <div class="lf-rail">
          <nav class="lf-toc" [attr.aria-label]="tocLabel()">
            <div class="lf-toc-label">{{ tocLabel() }}</div>
            @for (e of toc(); track e.id) {
              <a class="lf-toc-link" [class.lf-toc-link--icon]="!!e.icon" [class.is-active]="current() === e.id"
                 [attr.aria-current]="current() === e.id ? 'location' : null"
                 [routerLink]="[]" [fragment]="e.id">
                @if (e.icon) { <ui-icon [name]="e.icon" [size]="15" /> }
                @if (e.n) { <span class="lf-toc-n">{{ e.n }}</span> }
                <span>{{ e.label }}</span>
                @if (e.count !== undefined) { <span class="lf-toc-count">{{ e.count }}</span> }
              </a>
            }
          </nav>
          <ng-content select="[lfRail]" />
        </div>
      }
      <div class="lf-main"><ng-content /></div>
    </div>
  `,
})
export class LfBody {
  readonly toc = input<readonly LfTocEntry[]>([]);
  readonly tocLabel = input('Table of contents');

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly spied = signal<string | null>(null);

  /** The spied section, or the first entry until the reader has scrolled. */
  protected readonly current = computed(() => {
    const ids = this.toc().map((e) => e.id);
    const spied = this.spied();
    return spied && ids.includes(spied) ? spied : ids[0] ?? null;
  });

  constructor() {
    if (!isPlatformBrowser(inject(PLATFORM_ID))) return;
    const destroyRef = inject(DestroyRef);
    afterNextRender(() => {
      const view = this.host.nativeElement.ownerDocument.defaultView;
      if (!view) return;
      let frame = 0;
      const measure = () => {
        frame = 0;
        let found: string | null = null;
        for (const entry of this.toc()) {
          const el = this.host.nativeElement.querySelector<HTMLElement>(`#${CSS.escape(entry.id)}`);
          // A section the hub filter has hidden has no box and must not win.
          if (!el || el.offsetParent === null) continue;
          if (found === null || el.getBoundingClientRect().top <= 140) found = entry.id;
        }
        this.spied.set(found);
      };
      const onScroll = () => { if (!frame) frame = view.requestAnimationFrame(measure); };
      measure();
      view.addEventListener('scroll', onScroll, { passive: true });
      view.addEventListener('resize', onScroll, { passive: true });
      destroyRef.onDestroy(() => {
        view.removeEventListener('scroll', onScroll);
        view.removeEventListener('resize', onScroll);
        if (frame) view.cancelAnimationFrame(frame);
      });
    });
  }
}

/**
 * One numbered section of a long-form page. Put it on a real <section> and
 * give that element its id — the id is what analytics reports as the
 * section_view name and what the contents rail links to:
 *
 *   <section lfSection id="security" n="05" heading="Security measures">…</section>
 *
 * Inputs
 *   heading   the h2 (revealed word by word)
 *   n         section number; draws the numbered hairline above the heading.
 *             Omit it for the docs-hub variant, which puts the heading,
 *             `count` and the hairline on one line.
 *   count     small mono figure beside the heading (hub variant)
 *   sub       one-paragraph introduction under the heading
 *   hold      fade the projected body in as one block (default). Set false
 *             when the body staggers its own .ds-item children.
 *
 * The section reveals itself on scroll (UiReveal); the prerender shows it in
 * full.
 */
@Component({
  selector: 'section[lfSection]',
  imports: [UiWords],
  hostDirectives: [UiReveal],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'lf-section' },
  template: `
    @if (n()) {
      <div class="lf-rule ds-hold"><span class="lf-rule-n">{{ n() }}</span><span class="lf-rule-line"></span></div>
      <h2 class="lf-h2"><ui-words [text]="heading()" /></h2>
    } @else {
      <div class="lf-head-row">
        <h2 class="lf-h2"><ui-words [text]="heading()" /></h2>
        @if (count() !== null) { <span class="lf-count ds-hold">{{ count() }}</span> }
        <span class="lf-rule-line ds-hold"></span>
      </div>
    }
    @if (sub()) { <p class="lf-sub ds-hold" [style.--d]="220">{{ sub() }}</p> }
    <div class="lf-section-body" [class.ds-hold]="hold()" [style.--d]="260"><ng-content /></div>
  `,
})
export class LfSection {
  readonly heading = input.required<string>();
  readonly n = input<string | null>(null);
  readonly count = input<string | number | null>(null);
  readonly sub = input<string | null>(null);
  readonly hold = input(true);
}

/**
 * Callout: an icon tile, a title and projected body copy.
 *
 * Inputs
 *   tone    blue · teal · warn · neutral
 *   icon    Phosphor name (must appear as a quoted literal in src/app)
 *   title   the bold first line
 *   legal   render the body as a legal clause: full-strength text, smaller
 *           and more widely leaded (used for the licence's capitalised
 *           disclaimers)
 */
@Component({
  selector: 'lf-note',
  imports: [UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[class]': "'lf-note lf-note--' + tone()",
    '[class.lf-note--legal]': 'legal()',
  },
  template: `
    <span class="lf-ico lf-ico--{{ tone() }}"><ui-icon [name]="icon()" [size]="17" /></span>
    <div>
      <div class="lf-note-title">{{ title() }}</div>
      <div class="lf-note-text"><ng-content /></div>
    </div>
  `,
})
export class LfNote {
  readonly tone = input<LfTone>('blue');
  readonly icon = input('info');
  readonly title = input.required<string>();
  readonly legal = input(false);
}

interface CodeLine { code: string; comment: string; }

/**
 * Code block with a caption and a Copy button.
 *
 * Inputs
 *   label   caption in the block's header
 *   code    the snippet; lines are separated by \n. A trailing `# …` or
 *           `// …` comment on a line is dimmed.
 *
 * The button is a browser-only enhancement; it copies the snippet verbatim.
 */
@Component({
  selector: 'lf-code',
  imports: [UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'lf-code' },
  template: `
    <div class="lf-code-head">
      <span class="lf-code-label">{{ label() }}</span>
      <button type="button" class="lf-code-copy" [class.is-copied]="copied()" (click)="copy()">
        @if (copied()) { <ui-icon name="check" [size]="13" /> } @else { <ui-icon name="copy" [size]="13" /> }
        <span aria-live="polite">{{ copied() ? 'Copied' : 'Copy' }}</span>
      </button>
    </div>
    <pre class="lf-code-pre"><code>@for (line of lines(); track $index) {<span class="lf-code-line">{{ line.code }}@if (line.comment) {<span class="lf-code-comment">{{ line.comment }}</span>}</span>}</code></pre>
  `,
})
export class LfCode {
  readonly label = input('');
  readonly code = input.required<string>();

  protected readonly copied = signal(false);
  protected readonly lines = computed<CodeLine[]>(() => this.code().split('\n').map((line) => {
    // A comment starts the line or follows whitespace, so `https://` is safe.
    const at = line.search(/(^|\s)(#|\/\/)/);
    if (at < 0) return { code: line, comment: '' };
    const start = line[at] === '#' || line[at] === '/' ? at : at + 1;
    return { code: line.slice(0, start), comment: line.slice(start) };
  }));

  private timer: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    inject(DestroyRef).onDestroy(() => { if (this.timer) clearTimeout(this.timer); });
  }

  /** Only ever runs from a click, so it never executes during prerender. */
  protected copy(): void {
    void navigator.clipboard?.writeText(this.code()).catch(() => undefined);
    this.copied.set(true);
    if (this.timer) clearTimeout(this.timer);
    this.timer = setTimeout(() => this.copied.set(false), 1600);
  }
}

/**
 * Link card for the contact blocks at the end of a long-form page and for the
 * contact page's channels.
 *
 * It is a hub card as far as analytics is concerned: an <a class="hub-card">
 * whose .hub-title holds the eyebrow and, inside a .badge, the title — so a
 * click reports card_title "<eyebrow> <title>" and card_badge "<title>",
 * which is what the old cards reported ("Email Privacy requests" /
 * "Privacy requests"). The .badge is restyled as the card's headline, not
 * drawn as a pill.
 *
 * Inputs
 *   icon      Phosphor name
 *   eyebrow   small caps line (the old card's title)
 *   title     headline (the old card's badge)
 *   body      one or two sentences
 *   cta       link text; an arrow is appended
 *   href      destination. Internal paths (/contact) are routed by the page
 *             directive; mailto: and https: links are left to the browser.
 *   wide      the contact page's lead card: icon, text and a pill CTA in a row
 */
@Component({
  selector: 'lf-link-card',
  imports: [UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'lf-card-host', '[class.lf-card-host--wide]': 'wide()' },
  template: `
    <a class="hub-card lf-card" [class.lf-card--wide]="wide()" [attr.href]="href()">
      <span class="lf-card-ico"><ui-icon [name]="icon()" [size]="17" /></span>
      <span class="lf-card-text">
        <span class="hub-title"><span class="lf-card-eyebrow">{{ eyebrow() }}</span>{{ ' ' }}<span class="badge">{{ title() }}</span></span>
        <span class="hub-desc">{{ body() }}</span>
      </span>
      <span class="hub-link">{{ cta() }}<ui-icon name="arrow-right" [size]="14" /></span>
    </a>
  `,
})
export class LfLinkCard {
  readonly icon = input.required<string>();
  readonly eyebrow = input.required<string>();
  readonly title = input.required<string>();
  readonly body = input.required<string>();
  readonly cta = input.required<string>();
  readonly href = input.required<string>();
  readonly wide = input(false);
}

/** Everything a long-form page template needs, for a component's `imports`. */
export const LONGFORM = [LfHeader, LfBody, LfSection, LfNote, LfCode, LfLinkCard] as const;
