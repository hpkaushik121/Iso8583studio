import {
  DestroyRef, Directive, ElementRef, PLATFORM_ID, afterNextRender, inject, signal,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

export interface CycleOptions {
  /** Number of steps. */
  count: number;
  /** Milliseconds each step stays up. */
  interval: number;
  /** After the reader picks a step, resume on its own this many ms later. Omit to stay paused until toggled. */
  resumeAfter?: number;
}

/**
 * The auto-advancing index behind a step picker.
 *
 * It starts on step 0 and does nothing on the server, so the prerendered page
 * shows the first step. In the browser it advances only while its scene is on
 * screen, never under prefers-reduced-motion, and stops with the page.
 */
export class SolCycle {
  readonly index = signal(0);
  /** True once the reader has taken over (or paused it). */
  readonly paused = signal(false);
  /** True once the cycle is allowed to run in this browser: false on the server and under reduced motion. */
  readonly live = signal(false);

  private timer: ReturnType<typeof setInterval> | null = null;
  private resume: ReturnType<typeof setTimeout> | null = null;
  private io: IntersectionObserver | null = null;
  private visible = false;
  private motion = false;

  constructor(private readonly opts: CycleOptions) {}

  /** Browser only: begin watching `el` and run while it is in view. */
  start(el: HTMLElement | undefined): void {
    const view = el?.ownerDocument.defaultView;
    if (!el || !view || !('IntersectionObserver' in view)) return;
    if (view.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    this.motion = true;
    this.live.set(true);
    this.io = new IntersectionObserver(([entry]) => {
      this.visible = entry.isIntersecting;
      this.sync();
    }, { threshold: 0.15 });
    this.io.observe(el);
  }

  /** The reader chose a step. */
  pick(i: number): void {
    this.index.set(i);
    this.paused.set(true);
    if (this.resume) clearTimeout(this.resume);
    this.resume = null;
    if (this.opts.resumeAfter) {
      this.resume = setTimeout(() => { this.paused.set(false); this.sync(); }, this.opts.resumeAfter);
    }
    this.sync();
  }

  toggle(): void {
    if (this.resume) clearTimeout(this.resume);
    this.resume = null;
    this.paused.update((p) => !p);
    this.sync();
  }

  /** Back to the first step, keeping the clock running. */
  restart(): void {
    this.index.set(0);
    this.stop();
    this.sync();
  }

  destroy(): void {
    this.stop();
    if (this.resume) clearTimeout(this.resume);
    this.io?.disconnect();
    this.io = null;
  }

  private stop(): void {
    if (this.timer) clearInterval(this.timer);
    this.timer = null;
  }

  private sync(): void {
    const run = this.motion && this.visible && !this.paused();
    if (run && !this.timer) {
      this.timer = setInterval(
        () => this.index.update((i) => (i + 1) % this.opts.count), this.opts.interval);
    } else if (!run) {
      this.stop();
    }
  }
}

/**
 * Creates a SolCycle tied to the calling component: started after the first
 * browser render against the element `scene` returns, destroyed with it.
 * Call from a field initialiser or constructor.
 */
export function solCycle(opts: CycleOptions, scene: () => HTMLElement | undefined): SolCycle {
  const cycle = new SolCycle(opts);
  if (isPlatformBrowser(inject(PLATFORM_ID))) {
    afterNextRender(() => cycle.start(scene()));
    inject(DestroyRef).onDestroy(() => cycle.destroy());
  }
  return cycle;
}

/**
 * Pauses the CSS animations inside a block while it is off screen (see
 * .sol-paused in solutions.css). The prerendered page carries no such class,
 * so without script the art simply keeps moving.
 */
@Directive({ selector: '[solLive]' })
export class SolLive {
  constructor() {
    if (!isPlatformBrowser(inject(PLATFORM_ID))) return;
    const el = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
    let io: IntersectionObserver | null = null;
    afterNextRender(() => {
      const view = el.ownerDocument.defaultView;
      if (!view || !('IntersectionObserver' in view)) return;
      io = new IntersectionObserver(([entry]) => {
        el.classList.toggle('sol-paused', !entry.isIntersecting);
      }, { rootMargin: '80px 0px' });
      io.observe(el);
    });
    inject(DestroyRef).onDestroy(() => io?.disconnect());
  }
}
