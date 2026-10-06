import {
  ChangeDetectionStrategy, Component, DOCUMENT, OnDestroy, PLATFORM_ID, afterNextRender,
  computed, inject, input, signal,
} from '@angular/core';
import { NgTemplateOutlet, isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { EXTERNAL } from '../../../core/site-nav';
import { Crumb, UiBadge, UiBreadcrumb, UiIcon, UiWords } from '../../../ui';
import { GuideNote, GuideRailItem, GuideRich, GuideSection } from '../guide';
import { HeroVideo } from '../hero-video';
import { SIMULATOR_PREVIEWS, SimulatorPreviewData } from '../../data/simulator-preview.data';
import { PreviewWorkflow } from './preview-workflow';

/** The sections of a concept guide, in page order. The ids are what section_view reports. */
const SECTIONS: GuideRailItem[] = [
  { id: 'overview', label: 'Overview', icon: 'book-open' },
  { id: 'workflow', label: 'Workflow preview', icon: 'arrows-left-right' },
  { id: 'planned', label: 'Planned capabilities', icon: 'squares-four' },
  { id: 'fit', label: 'Where it fits', icon: 'tree-structure' },
  { id: 'development', label: 'Development status', icon: 'wrench' },
];

/**
 * SimulatorPreview — the whole concept guide for a simulator that is still in
 * development (Switch, Issuer System, ATM, ECR). The four page components
 * render nothing but this, each with its own data.
 *
 *   <app-simulator-preview [page]="page" />
 *
 * Hero: the looping product clip behind a left-hand copy column. Its section
 * carries .page-hero and the CTAs sit in .ph-ctas, which is what analytics
 * reads for hero_cta_click. The guide shell's hero is not used because this
 * one has a status badge beside the title and a note under the CTAs.
 *
 * Body: the guide shell's grid, rail and sections (gd- classes from
 * guide.css), with a rail of its own — it carries a second status badge and
 * the list of the other upcoming simulators, which <app-guide-layout> has no
 * place for.
 *
 * Styles: styles/bundles/preview.css (sp-), on top of guide.css.
 */
@Component({
  selector: 'app-simulator-preview',
  imports: [
    NgTemplateOutlet, RouterLink, UiBadge, UiBreadcrumb, UiIcon, UiWords,
    GuideNote, GuideRich, GuideSection, HeroVideo, PreviewWorkflow,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'sp-page' },
  template: `
    <section class="page-hero sp-hero" data-sect="hero">
      <app-hero-video [clips]="clips()" [poster]="page().media" focus="68% 50%" [style.--d]="500" />
      <div class="sp-wrap sp-hero-inner">
        <div class="sp-hero-copy">
          <div class="sp-hero-crumb ds-fade" [style.--d]="150"><ui-breadcrumb [items]="crumbs()" /></div>
          <p class="sp-hero-meta ds-fade" [style.--d]="250">{{ meta() }}</p>
          <div class="sp-hero-title">
            <h1 class="ds-in"><ui-words [text]="page().name" [base]="350" /></h1>
            <span class="sp-hero-badge ds-fade" [style.--d]="650"><ui-badge tone="warn">In development</ui-badge></span>
          </div>
          <p class="sp-lede ds-fade" [style.--d]="900">{{ page().description }}</p>
          <div class="ph-ctas sp-actions ds-fade" [style.--d]="1200">
            <a class="btn btn--primary btn--glow" [href]="releases">Download Studio<ui-icon name="arrow-up-right" [size]="16" /></a>
            <a class="btn btn--secondary" [routerLink]="[]" fragment="workflow">Explore the workflow<ui-icon name="arrow-down" [size]="16" /></a>
          </div>
          <p class="sp-hero-note ds-fade" [style.--d]="1350">Concept preview · Not yet available in Studio.</p>
        </div>
      </div>
    </section>

    <ng-template #links>
      @for (item of sections; track item.id) {
        <a [routerLink]="[]" [fragment]="item.id" [class.on]="active() === item.id"
           [attr.aria-current]="active() === item.id ? 'location' : null"><ui-icon [name]="item.icon" [size]="15" /><span>{{ item.label }}</span></a>
      }
      <div class="gd-rail-label sp-rail-related">Upcoming simulators</div>
      @for (other of pages; track other.id) {
        <a [routerLink]="other.link" [class.on]="other.id === page().id"
           [attr.aria-current]="other.id === page().id ? 'page' : null"><ui-icon [name]="other.icon" [size]="15" /><span>{{ other.name }}</span></a>
      }
    </ng-template>

    <div class="gd-wrap sp-guide-grid">
      <details class="gd-toc sp-mobile-rail">
        <summary>On this page</summary>
        <nav class="gd-rail sp-rail" aria-label="On this page"><ng-container [ngTemplateOutlet]="links" /></nav>
      </details>

      <div class="gd-rail-col">
        <nav class="gd-rail sp-rail" aria-label="On this page">
          <div class="sp-rail-status"><ui-badge tone="teal" [dot]="true">Simulator guide</ui-badge><ui-badge tone="warn">In development</ui-badge></div>
          <div class="gd-rail-label">On this page</div>
          <ng-container [ngTemplateOutlet]="links" />
        </nav>
      </div>

      <div class="gd-main sp-guide-main">
        <app-guide-section anchor="overview" heading="Overview" eyebrowTone="faint"
                           [eyebrow]="page().slug + ' · Concept preview'" [intro]="page().overviewText" [threshold]="0.15">
          <p class="gd-p ds-hold" [style.--d]="300" [gdRich]="page().overviewMore"></p>
          <app-guide-note class="sp-development-note" tone="warn" icon="wrench" heading="This simulator is under active development">
            <span [gdRich]="status()"></span>
          </app-guide-note>
        </app-guide-section>

        <app-guide-section anchor="workflow" heading="Workflow preview" [threshold]="0.1"
                           [intro]="page().overviewTitle + ' Choose a scenario, then follow each step through the illustrative exchange.'">
          <app-preview-workflow [page]="page()" />
        </app-guide-section>

        <app-guide-section anchor="planned" heading="Planned capabilities" [threshold]="0.15"
                           [intro]="'The intended scope for ' + page().name + '. These capabilities are being developed and may evolve before release.'">
          <div class="sp-capabilities">
            @for (feature of page().features; track feature.title; let i = $index) {
              <div class="ds-item" [style.--d]="350 + i * 120">
                <div class="sp-capability">
                  <div class="sp-capability-top"><span class="sp-capability-icon"><ui-icon [name]="feature.icon" [size]="18" /></span><span class="sp-mono">0{{ i + 1 }}</span></div>
                  <h3>{{ feature.title }}</h3>
                  <p>{{ feature.body }}</p>
                  <span class="sp-capability-status">Planned</span>
                </div>
              </div>
            }
          </div>
          <div class="gd-label ds-hold" [style.--d]="500">Planned scope</div>
          <ul class="gd-bullets gd-bullets--2 ds-hold" [style.--d]="560">
            @for (item of page().planned; track item) { <li>{{ item }}</li> }
          </ul>
          <app-guide-note tone="neutral" icon="info" heading="Note">
            Scope is indicative and may change as the feature is built. Follow the
            <a [href]="roadmap" target="_blank" rel="noopener">roadmap</a> for status.
          </app-guide-note>
        </app-guide-section>

        <app-guide-section anchor="fit" heading="Where it fits" [intro]="page().fit" [threshold]="0.15" />

        <app-guide-section anchor="development" heading="Development status" [threshold]="0.15">
          <div class="ds-item sp-development-item" [style.--d]="300">
            <div class="sp-development">
              <div class="sp-development-copy">
                <ui-badge tone="warn" [dot]="true" [mono]="true">In development</ui-badge>
                <h3>On its way to Studio.</h3>
                <p>{{ page().name }} is in active development. The illustrations and workflows on this page show the intended direction, and the final interface may change.</p>
                <div class="sp-release-note"><ui-icon name="calendar-blank" [size]="18" /><span>A release date has not been announced.</span></div>
                <a class="sp-text-link" routerLink="/docs/versions">Follow the release notes<ui-icon name="arrow-up-right" [size]="15" /></a>
              </div>
              <div class="sp-available">
                <span class="sp-available-label"><span class="sp-available-dot" aria-hidden="true"></span>Explore Studio today</span>
                <h3>{{ page().related.label }}</h3>
                <p>{{ page().related.body }}</p>
                <div class="sp-actions"><a class="btn btn--secondary" [routerLink]="page().related.link">Explore {{ page().related.label }}<ui-icon name="arrow-right" [size]="16" /></a></div>
                <a class="sp-text-link" routerLink="/docs/installation">Install ISO8583Studio<ui-icon name="arrow-up-right" [size]="15" /></a>
              </div>
            </div>
          </div>
        </app-guide-section>

        <div class="sp-next">
          <span>Also in development</span>
          <a [routerLink]="next().link"><ui-icon [name]="next().icon" [size]="20" />{{ next().name }}<ui-icon name="arrow-right" [size]="20" /></a>
        </div>
      </div>
    </div>
  `,
})
export class SimulatorPreview implements OnDestroy {
  readonly page = input.required<SimulatorPreviewData>();

  protected readonly sections = SECTIONS;
  protected readonly pages = SIMULATOR_PREVIEWS;
  protected readonly releases = EXTERNAL.releases;
  protected readonly roadmap = EXTERNAL.roadmap;

  protected readonly clips = computed(() => [this.page().media]);
  protected readonly meta = computed(() => this.page().protocols.join(' · '));
  protected readonly crumbs = computed<Crumb[]>(() => [
    { label: 'Home', link: '/' },
    { label: 'Documentation', link: '/docs' },
    { label: this.page().name },
  ]);
  /** The status banner. GuideRich markdown; the link is written in full because the site has a base href. */
  protected readonly status = computed(() =>
    `The \`${this.page().typeName}\` type is scaffolded in ISO8583Studio, and its configuration & runtime screens are being built. ` +
    'This page describes the intended capabilities: the workflows below use sample data and do not represent a released simulator interface. ' +
    `In the meantime, the simulators available now cover the same message flows — see [Where it fits](${this.page().link}#fit).`);
  /** The guide after this one; the last wraps round to the first. */
  protected readonly next = computed(() => {
    const at = SIMULATOR_PREVIEWS.findIndex((p) => p.id === this.page().id);
    return SIMULATOR_PREVIEWS[(at + 1) % SIMULATOR_PREVIEWS.length];
  });

  /** Id of the section being read; the first until the browser says otherwise. */
  protected readonly active = signal(SECTIONS[0].id);

  private readonly doc = inject(DOCUMENT);
  private io: IntersectionObserver | null = null;
  private end: IntersectionObserver | null = null;

  constructor() {
    if (!isPlatformBrowser(inject(PLATFORM_ID))) return;
    afterNextRender(() => this.spy());
  }

  /**
   * Scroll-spy for the rail, the same way GuideLayout does it: the first
   * section touching a band from just under the fixed header to 40% down the
   * viewport is the one being read, and the last section takes over once all
   * of it is on screen.
   */
  private spy(): void {
    const view = this.doc.defaultView;
    if (!view || !('IntersectionObserver' in view)) return;
    const targets = SECTIONS
      .map((item) => this.doc.getElementById(item.id))
      .filter((el): el is HTMLElement => !!el);
    if (!targets.length) return;

    const inBand = new Set<string>();
    const last = targets[targets.length - 1];
    let atEnd = false;
    const pick = () => {
      const current = atEnd ? last : targets.find((el) => inBand.has(el.id));
      if (current) this.active.set(current.id);
    };
    this.io = new IntersectionObserver((entries) => {
      for (const entry of entries) {
        if (entry.isIntersecting) inBand.add(entry.target.id);
        else inBand.delete(entry.target.id);
      }
      pick();
    }, { rootMargin: '-100px 0px -60% 0px', threshold: 0 });
    targets.forEach((el) => this.io!.observe(el));

    this.end = new IntersectionObserver(([entry]) => {
      atEnd = entry.isIntersecting;
      pick();
    }, { threshold: 1 });
    this.end.observe(last);
  }

  ngOnDestroy(): void {
    this.io?.disconnect();
    this.end?.disconnect();
  }
}
