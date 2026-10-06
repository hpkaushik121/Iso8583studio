import {
  ChangeDetectionStrategy, Component, DestroyRef, ElementRef, PLATFORM_ID, afterNextRender,
  computed, inject, input, viewChild,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Crumb, UiBreadcrumb, UiIcon, UiWords } from '../../ui';

/**
 * The centred hero every solution page opens with: dot field, breadcrumb,
 * mono fact line, headline, copy, CTAs, and the page's own art scene
 * projected underneath.
 *
 * It renders `section.page-hero` with the CTAs in `.ph-ctas` — the pair
 * analytics reads for hero_cta_click — and names the section with `sect`, the
 * string the page reported for its hero before the redesign.
 *
 * The primary CTA always goes to /contact. Project the secondary one as
 * `<a solHeroCta …>`, an optional fact row as `[solHeroFacts]`, and the scene
 * as the default content.
 *
 * Two pointer/scroll effects start after the first browser render and never
 * under prefers-reduced-motion: the dots brighten around the pointer, and
 * `explode` (px of scroll over which the scene closes up) drives the
 * --explode custom property the scene's layers read, only while the hero is
 * on screen.
 */
@Component({
  selector: 'sol-hero',
  imports: [RouterLink, UiBreadcrumb, UiIcon, UiWords],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'sol-block' },
  template: `
    <section #root class="page-hero sol-hero" [attr.data-sect]="sect()"
             [style.--sol-h1-max.px]="headingMax()" [style.--sol-sub-max.px]="subMax()"
             [style.--sol-hero-pb.px]="padBottom()">
      <div class="sol-dots" aria-hidden="true">
        <div class="sol-dots-grid"></div>
        <div #hot class="sol-dots-grid sol-dots-hot"></div>
      </div>
      <div class="sol-hero-copy">
        <ui-breadcrumb class="sol-crumb ds-fade" [style.--d]="150" [items]="crumbs()" />
        <div class="sol-hero-eyebrow ds-fade" [style.--d]="250">{{ eyebrow() }}</div>
        <h1 class="ds-in"><ui-words [text]="heading()" [base]="350" /></h1>
        @if (lead()) {
          <p class="sol-hero-lead ds-fade" [style.--d]="900">{{ lead() }}</p>
        }
        <p class="sol-hero-sub ds-fade" [class.sol-hero-sub--small]="!!lead()"
           [style.--d]="lead() ? 1100 : 900">{{ sub() }}</p>
        <div class="ph-ctas sol-hero-ctas ds-fade" [style.--d]="lead() ? 1300 : 1100">
          <a class="btn btn--primary btn--glow" routerLink="/contact">{{ cta() }}<ui-icon name="arrow-right" [size]="16" /></a>
          <ng-content select="[solHeroCta]" />
        </div>
        <ng-content select="[solHeroFacts]" />
      </div>
      <ng-content />
    </section>
  `,
})
export class SolHero {
  /** section_view name of the hero. */
  readonly sect = input.required<string>();
  /** Label of this page in the breadcrumb. */
  readonly crumb = input.required<string>();
  readonly eyebrow = input.required<string>();
  readonly heading = input.required<string>();
  /** Optional bold line between the headline and the copy. */
  readonly lead = input<string | null>(null);
  readonly sub = input.required<string>();
  /** Label of the primary CTA (links to /contact). */
  readonly cta = input.required<string>();
  readonly headingMax = input(760);
  readonly subMax = input(640);
  readonly padBottom = input(40);
  /** Scroll distance in px over which --explode goes 1 → 0; 0 leaves it alone. */
  readonly explode = input(0);

  protected readonly crumbs = computed<Crumb[]>(() => [{ label: 'Home', link: '/' }, { label: this.crumb() }]);

  private readonly root = viewChild.required<ElementRef<HTMLElement>>('root');
  private readonly hot = viewChild.required<ElementRef<HTMLElement>>('hot');

  constructor() {
    if (!isPlatformBrowser(inject(PLATFORM_ID))) return;
    const teardown: (() => void)[] = [];
    afterNextRender(() => this.start(teardown));
    inject(DestroyRef).onDestroy(() => teardown.forEach((fn) => fn()));
  }

  private start(teardown: (() => void)[]): void {
    const root = this.root().nativeElement;
    const hot = this.hot().nativeElement;
    const view = root.ownerDocument.defaultView;
    if (!view || !('IntersectionObserver' in view)) return;
    if (view.matchMedia('(prefers-reduced-motion: reduce)').matches) return;

    // Dots light up around the pointer.
    let raf = 0;
    let x = 0;
    let y = 0;
    let lit = false;
    const paint = () => {
      raf = 0;
      hot.style.setProperty('--sol-px', `${x}px`);
      hot.style.setProperty('--sol-py', `${y}px`);
      hot.classList.toggle('is-lit', lit);
    };
    const queue = () => { if (!raf) raf = view.requestAnimationFrame(paint); };
    const move = (e: PointerEvent) => {
      const r = root.getBoundingClientRect();
      x = e.clientX - r.left;
      y = e.clientY - r.top;
      lit = true;
      queue();
    };
    const leave = () => { lit = false; queue(); };
    root.addEventListener('pointermove', move, { passive: true });
    root.addEventListener('pointerleave', leave);
    teardown.push(() => {
      root.removeEventListener('pointermove', move);
      root.removeEventListener('pointerleave', leave);
      if (raf) view.cancelAnimationFrame(raf);
    });

    // The scene closes up as the hero scrolls away.
    const distance = this.explode();
    if (!distance) return;
    let queued = 0;
    let listening = false;
    const run = () => {
      queued = 0;
      const p = Math.min(1, Math.max(0, view.scrollY / distance));
      root.style.setProperty('--explode', (1 - p).toFixed(3));
    };
    const onScroll = () => { if (!queued) queued = view.requestAnimationFrame(run); };
    const io = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting && !listening) {
        listening = true;
        view.addEventListener('scroll', onScroll, { passive: true });
        run();
      } else if (!entry.isIntersecting && listening) {
        listening = false;
        view.removeEventListener('scroll', onScroll);
      }
    });
    io.observe(root);
    teardown.push(() => {
      io.disconnect();
      view.removeEventListener('scroll', onScroll);
      if (queued) view.cancelAnimationFrame(queued);
    });
  }
}
