import {
  ChangeDetectionStrategy, Component, DestroyRef, ElementRef, PLATFORM_ID, afterNextRender,
  computed, inject, signal,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { JOURNEY_STAGES } from './journey.data';

interface Dot {
  /** The data-sect of the page section this dot stands for. */
  sect: string;
  label: string;
  /** The journey section splits into one small dot per transaction stage. */
  stages?: string[];
}

/* Labels are what the reader sees on hover. None may begin with "Download":
   tools/check-tracking.mjs treats a <button> that does as an untracked CTA. */
const DOTS: Dot[] = [
  { sect: 'hero', label: 'Home' },
  { sect: 'transaction_path', label: 'The payment journey', stages: JOURNEY_STAGES.map((s) => `${s.n} / ${s.label}`) },
  { sect: 'lifecycle', label: 'Develop → Test → Certify' },
  { sect: 'toolbox', label: 'What used to need a lab' },
  { sect: 'solutions', label: 'Solutions & services' },
  { sect: 'pricing', label: 'Register for Pro' },
  { sect: 'faq', label: 'Frequently asked questions' },
  { sect: 'final_cta', label: 'Get the studio' },
];

/**
 * Fixed right-edge rail: one dot per page section, the active one elongated;
 * a click scrolls there. Inside the payment journey the rail belongs to the
 * transaction and shows its eight stage dots instead.
 *
 * The dots are buttons, not links — they move the viewport, they do not
 * navigate — and the whole rail is rendered on the server in its resting
 * state (first dot active). The scroll-spy starts in the browser. Hidden at
 * 760px and below, in CSS.
 */
@Component({
  selector: 'home-scroll-dots',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <nav class="scroll-dots" aria-label="Page sections" [class.in-journey]="inJourney()">
      @for (dot of dots; track dot.sect; let i = $index) {
        <button type="button" class="scroll-dot" [class.is-on]="active() === i"
                [attr.aria-label]="dot.label" [attr.aria-current]="active() === i ? 'true' : null"
                (click)="goSection(i)">
          <span class="scroll-dot-label" aria-hidden="true">{{ dot.label }}</span>
          <span class="scroll-dot-mark"></span>
        </button>
        @if (dot.stages; as stages) {
          <span class="scroll-dot-stages">
            @for (label of stages; track label; let j = $index) {
              <button type="button" class="scroll-dot scroll-dot--stage" [class.is-on]="stage() === j"
                      [style.--d]="j * 30" [attr.aria-label]="label"
                      [attr.aria-current]="stage() === j ? 'true' : null" (click)="goStage(j)">
                <span class="scroll-dot-label" aria-hidden="true">{{ label }}</span>
                <span class="scroll-dot-mark"></span>
              </button>
            }
          </span>
        }
      }
    </nav>
  `,
})
export class HomeScrollDots {
  protected readonly dots = DOTS;
  protected readonly active = signal(0);
  protected readonly stage = signal(0);
  protected readonly inJourney = computed(() => (DOTS[this.active()]?.stages?.length ?? 0) > 1);

  private readonly el = inject<ElementRef<HTMLElement>>(ElementRef);
  private sections: (HTMLElement | null)[] = [];
  private stages: HTMLElement[] = [];

  constructor() {
    if (!isPlatformBrowser(inject(PLATFORM_ID))) return;
    const destroyRef = inject(DestroyRef);

    afterNextRender(() => {
      const page = this.el.nativeElement.closest<HTMLElement>('.static-page') ?? document.body;
      this.sections = DOTS.map((d) => page.querySelector<HTMLElement>(`[data-sect="${d.sect}"]`));
      this.stages = [...page.querySelectorAll<HTMLElement>('.story-stage')];

      let frame = 0;
      const spy = () => {
        frame = 0;
        const line = innerHeight * 0.38;
        let best = 0;
        this.sections.forEach((s, i) => { if (s && s.getBoundingClientRect().top <= line) best = i; });
        this.active.set(best);
        if (DOTS[best].stages) {
          let current = 0;
          this.stages.forEach((s, i) => { if (s.getBoundingClientRect().top <= innerHeight * 0.5) current = i; });
          this.stage.set(current);
        }
      };
      const onScroll = () => { if (!frame) frame = requestAnimationFrame(spy); };

      spy();
      addEventListener('scroll', onScroll, { passive: true });
      addEventListener('resize', onScroll, { passive: true });
      destroyRef.onDestroy(() => {
        removeEventListener('scroll', onScroll);
        removeEventListener('resize', onScroll);
        if (frame) cancelAnimationFrame(frame);
      });
    });
  }

  protected goSection(i: number): void { this.scrollTo(this.sections[i]); }
  protected goStage(j: number): void { this.scrollTo(this.stages[j]); }

  private scrollTo(target: HTMLElement | null | undefined): void {
    target?.scrollIntoView({
      block: 'start',
      behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth',
    });
  }
}
