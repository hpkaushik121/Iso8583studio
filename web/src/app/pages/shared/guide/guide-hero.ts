import {
  ChangeDetectionStrategy, Component, ElementRef, OnDestroy, PLATFORM_ID, afterNextRender,
  inject, input, viewChild,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Crumb, UiBadge, UiBreadcrumb, UiWords } from '../../../ui';

/**
 * GuideHero — the hero of a guide page (tool guide, simulator guide, docs).
 *
 * Renders <section class="page-hero gd-hero …">: breadcrumb, a mono meta line,
 * the h1 (word-by-word), a lede and the CTA row. `.page-hero` and `.ph-ctas`
 * are the classes analytics reads for hero_cta_click, so CTAs placed in the
 * default slot are tracked without anything further — they must be anchors
 * (<ui-button href|routerLink> or <a class="btn …">), never click-handler
 * buttons.
 *
 *   <app-guide-hero align="left" heading="Host Simulator" meta="Server · Client · Proxy"
 *                   lede="Simulate acquirer and issuer host responses…"
 *                   [crumbs]="[{label:'Home', link:'/'}, {label:'Documentation', link:'/docs'}, {label:'Host Simulator'}]">
 *     <app-hero-video gdHeroBg [clips]="['host']" poster="host" />
 *     <ui-button [href]="releases" iconRight="arrow-up-right" [glow]="true">Download Studio</ui-button>
 *     <ui-button variant="secondary" [routerLink]="[]" fragment="commands" iconRight="arrow-down">Browse the tabs</ui-button>
 *   </app-guide-hero>
 *
 * Inputs
 *   heading  string, required      the h1
 *   crumbs   Crumb[], required     breadcrumb trail; the last item is the current page (no link)
 *   meta     string | null         mono line above the h1 ("9 tools · hex-driven")
 *   lede     string | null         paragraph under the h1
 *   align    'center' | 'left'     'center' (default): centred copy, room for a visual below —
 *                                  the tool guides. 'left': copy in a 480px column on the left,
 *                                  vertically centred in a min(760px, 90vh) hero — made for a
 *                                  projected background video.
 *   status      string | null   release pill beside the title ('Beta')
 *   dots     boolean               draws the dot-grid texture behind a centred hero; the dots
 *                                  near the pointer brighten (off under reduced motion)
 *
 * Slots
 *   (default)        the CTA anchors; they land inside <div class="ph-ctas">
 *   [gdHeroBg]       a background that fills the section, e.g. <app-hero-video gdHeroBg …>.
 *                    It is the section's first child and the section is position:relative.
 *   [gdHeroVisual]   anything that follows the copy inside the section — the tool guides put
 *                    <app-guide-frame gdHeroVisual> here.
 *
 * The section deliberately has no id and no data-sect.
 */
@Component({
  selector: 'app-guide-hero',
  imports: [UiBadge, UiBreadcrumb, UiWords],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'gd-hero-host' },
  template: `
    <section #hero class="page-hero gd-hero" data-sect="hero" [class.gd-hero--center]="align() === 'center'"
             [class.gd-hero--left]="align() === 'left'">
      <ng-content select="[gdHeroBg]" />
      @if (dots()) {
        <div class="gd-hero-dots" aria-hidden="true"><span class="gd-hero-dots-base"></span><span class="gd-hero-dots-hot"></span></div>
      }
      <div class="gd-hero-in">
        <div class="gd-hero-copy">
          <div class="gd-hero-crumb ds-fade" [style.--d]="150"><ui-breadcrumb [items]="crumbs()" /></div>
          @if (meta()) { <div class="gd-hero-meta ds-fade" [style.--d]="250">{{ meta() }}</div> }
          @if (status()) {
            <div class="gd-hero-titlerow">
              <h1 class="gd-hero-title ds-in"><ui-words [text]="heading()" [base]="350" /></h1>
              <span class="gd-hero-status ds-fade" [style.--d]="700"><ui-badge tone="blue">{{ status() }}</ui-badge></span>
            </div>
          } @else {
            <h1 class="gd-hero-title ds-in"><ui-words [text]="heading()" [base]="350" /></h1>
          }
          @if (lede()) { <p class="gd-hero-lede ds-fade" [style.--d]="900">{{ lede() }}</p> }
          <div class="ph-ctas gd-hero-ctas ds-fade" [style.--d]="1200"><ng-content /></div>
        </div>
      </div>
      <ng-content select="[gdHeroVisual]" />
    </section>
  `,
})
export class GuideHero implements OnDestroy {
  readonly heading = input.required<string>();
  readonly crumbs = input.required<Crumb[]>();
  readonly meta = input<string | null>(null);
  readonly lede = input<string | null>(null);
  readonly align = input<'center' | 'left'>('center');
  readonly dots = input(false);
  /** Release status shown as a pill beside the title, e.g. 'Beta'. */
  readonly status = input<string | null>(null);

  private readonly hero = viewChild.required<ElementRef<HTMLElement>>('hero');
  private teardown: (() => void) | null = null;

  constructor() {
    if (!isPlatformBrowser(inject(PLATFORM_ID))) return;
    afterNextRender(() => this.illuminate());
  }

  /** Brightens the dots under the pointer by moving a mask over a second, hotter dot layer. */
  private illuminate(): void {
    if (!this.dots()) return;
    const host = this.hero().nativeElement;
    const view = host.ownerDocument.defaultView;
    if (!view || view.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    if (!view.matchMedia('(hover: hover)').matches) return;

    let frame = 0, x = 0, y = 0, lit = false;
    const paint = () => {
      frame = 0;
      host.style.setProperty('--gd-px', `${x}px`);
      host.style.setProperty('--gd-py', `${y}px`);
      host.classList.toggle('is-lit', lit);
    };
    const move = (e: PointerEvent) => {
      const r = host.getBoundingClientRect();
      x = e.clientX - r.left; y = e.clientY - r.top; lit = true;
      if (!frame) frame = view.requestAnimationFrame(paint);
    };
    const leave = () => { lit = false; if (!frame) frame = view.requestAnimationFrame(paint); };
    host.addEventListener('pointermove', move, { passive: true });
    host.addEventListener('pointerleave', leave);
    this.teardown = () => {
      host.removeEventListener('pointermove', move);
      host.removeEventListener('pointerleave', leave);
      if (frame) view.cancelAnimationFrame(frame);
    };
  }

  ngOnDestroy(): void {
    this.teardown?.();
  }
}

/**
 * GuideFrame — the framed stage that rises into a centred guide hero: a lit
 * top edge, hairline border and a fade to the page at the bottom. Whatever you
 * project fills the frame (give it position:absolute; inset:0).
 *
 *   <app-guide-hero …>
 *     …CTAs…
 *     <app-guide-frame gdHeroVisual><app-tool-hub … /></app-guide-frame>
 *   </app-guide-hero>
 *
 * The frame is 980px wide (never wider than the hero's content box) and
 * 700 / 510 / 400px tall at >1200 / ≤1200 / ≤900px viewports — all CSS, so the
 * prerendered page already has its final geometry.
 *
 * Input: delay (ms before the frame rises; default 1700).
 */
@Component({
  selector: 'app-guide-frame',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'gd-frame-block' },
  template: `
    <div class="gd-frame ds-frame" [style.--d]="delay()">
      <span class="gd-frame-edge" aria-hidden="true"></span>
      <ng-content />
      <span class="gd-frame-fade" aria-hidden="true"></span>
    </div>
  `,
})
export class GuideFrame {
  readonly delay = input(1700);
}
