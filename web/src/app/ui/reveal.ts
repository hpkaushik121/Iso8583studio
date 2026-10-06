import {
  Directive, ElementRef, OnDestroy, PLATFORM_ID, afterNextRender, inject, input,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

/**
 * Plays the reveal animations of its descendants (.ds-word, .ds-item,
 * .ds-hold — see styles/_motion.css) the first time the block scrolls into
 * view.
 *
 * The prerendered page is fully visible. After hydration a block that is
 * still below the viewport is armed, which hides its reveal children, and is
 * revealed on intersection. A block already on screen is left exactly as the
 * server rendered it, so nothing the reader is looking at blinks out.
 */
@Directive({ selector: '[uiReveal]' })
export class UiReveal implements OnDestroy {
  /** Fraction of the block that must be visible before it reveals. */
  readonly threshold = input(0.25, { alias: 'uiRevealThreshold' });
  /** Milliseconds to hold the reveal back once the block is in view. */
  readonly delay = input(0, { alias: 'uiRevealDelay' });

  private readonly el = inject<ElementRef<HTMLElement>>(ElementRef);
  private io: IntersectionObserver | null = null;
  private timer: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    if (!isPlatformBrowser(inject(PLATFORM_ID))) return;
    afterNextRender(() => this.arm());
  }

  private arm(): void {
    const el = this.el.nativeElement;
    const view = el.ownerDocument.defaultView;
    if (!view || !('IntersectionObserver' in view)) return;
    if (view.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    if (el.classList.contains('ds-in')) return;
    if (el.getBoundingClientRect().top < view.innerHeight) return;

    el.classList.add('ds-armed');
    // A block taller than the viewport can never show a quarter of itself, so
    // the threshold is capped to what can actually be reached.
    const reachable = Math.min(this.threshold(), (view.innerHeight * 0.5) / Math.max(el.offsetHeight, 1));
    this.io = new IntersectionObserver(([entry]) => {
      if (!entry.isIntersecting) return;
      this.io?.disconnect();
      this.timer = setTimeout(() => el.classList.add('ds-in'), this.delay());
    }, { threshold: Math.max(reachable, 0.01) });
    this.io.observe(el);
  }

  ngOnDestroy(): void {
    this.io?.disconnect();
    if (this.timer) clearTimeout(this.timer);
  }
}
