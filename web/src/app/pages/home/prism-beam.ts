import {
  ChangeDetectionStrategy, Component, DestroyRef, ElementRef, PLATFORM_ID, afterNextRender,
  inject, input, signal, viewChild,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import type { BeamHandle } from './prism-beam.gl';

/** The beam only runs where the hero is wide enough to show it. */
const WIDE = '(min-width: 901px)';
/** When the strike starts, in ms after the hero's load sequence begins. */
const STRIKE_AT = 2100;

/**
 * The light that lands on the hero's product frame.
 *
 * Three layers, all decorative. The wash is a CSS gradient that lights the
 * backdrop at every width. The canvas is the WebGL beam. The static glow is
 * what stands in for the beam in the prerendered page, on a reader who asked
 * for reduced motion, and wherever WebGL is unavailable.
 *
 * The WebGL code is a separate chunk, fetched after first render and only on
 * a viewport wider than 900px without reduced motion. Once it is drawing, the
 * host gets .is-live and the static glow steps aside.
 */
@Component({
  selector: 'home-prism-beam',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class.is-live]': 'live()' },
  template: `
    <div class="pb-wash ds-fade" aria-hidden="true"></div>
    <div #host class="pb-host" aria-hidden="true"><canvas #canvas></canvas></div>
    <div class="pb-static ds-glow" aria-hidden="true" [style.--d]="strikeAt"></div>
  `,
})
export class HomePrismBeam {
  /** The product frame the beam strikes. */
  readonly frame = input.required<HTMLElement>();

  protected readonly live = signal(false);
  protected readonly strikeAt = STRIKE_AT;

  private readonly host = viewChild.required<ElementRef<HTMLElement>>('host');
  private readonly canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');

  constructor() {
    if (!isPlatformBrowser(inject(PLATFORM_ID))) return;
    const destroyRef = inject(DestroyRef);

    afterNextRender(() => {
      if (matchMedia('(prefers-reduced-motion: reduce)').matches) return;

      const wide = matchMedia(WIDE);
      let handle: BeamHandle | null = null;
      let loading = false;
      let dead = false;

      const start = () => {
        if (handle || loading || dead || !wide.matches) return;
        loading = true;
        import('./prism-beam.gl')
          .then((m) => {
            if (dead) return;
            handle = m.startBeam(
              this.host().nativeElement, this.canvas().nativeElement, this.frame(), this.remainingDelay(),
            );
            this.live.set(handle !== null);
          })
          .catch((err) => console.warn('Prism beam failed to load', err))
          .finally(() => { loading = false; });
      };

      start();
      wide.addEventListener('change', start);

      destroyRef.onDestroy(() => {
        dead = true;
        wide.removeEventListener('change', start);
        handle?.destroy();
        handle = null;
      });
    });
  }

  /**
   * How long until the strike is due. The frame's rise is a CSS animation that
   * started with the page, before this code loaded, so the beam reads the
   * animation's own clock rather than counting from now.
   */
  private remainingDelay(): number {
    const running = this.frame().getAnimations?.() ?? [];
    const elapsed = running.reduce((max, a) => Math.max(max, Number(a.currentTime) || 0), 0);
    return running.length ? Math.max(0, STRIKE_AT - elapsed) : 0;
  }
}
