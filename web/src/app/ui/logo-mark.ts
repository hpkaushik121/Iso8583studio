import { Component, ChangeDetectionStrategy, input } from '@angular/core';

/** The two arcs of the S, drawn separately so each can carry its own colour
 *  and its own timing. */
const ARC_TOP = 'M35 14 H20 A6 6 0 0 0 20 26 H28';
const ARC_BOTTOM = 'M28 26 A6 6 0 0 1 28 38 H13';

/** The same letter as one unbroken pen stroke, for the travelling trace. */
const ARC_WHOLE = `${ARC_TOP} A6 6 0 0 1 28 38 H13`;

/** Which of the studies to render. */
export type LogoMarkMode = 'brand' | 'spin' | 'trace' | 'still';

/**
 * The ISO8583Studio S mark, in the states the "Logo Animation 4e" study
 * settled on. One component owns the geometry so the header brand, the route
 * spinner and the processing disc cannot drift apart.
 *
 *  - `brand` — the gradient tile. The S draws itself in on load, then the teal
 *    arc keeps a slow idle beat. Hovering turns the tile a half turn, which
 *    lands back on the same letter because the S is 180° rotationally
 *    symmetric; only the two colours swap ends.
 *  - `spin` — loading. The mark holds still and a ring carries the wait.
 *  - `trace` — processing. A light runs the length of the letter over a
 *    ghosted S, so the motion follows the work rather than circling it.
 *  - `still` — the mark with no motion, for anywhere the animation would be
 *    noise.
 *
 * `size` is the tile in `brand` and the glyph everywhere else, because the
 * tile is what occupies layout when there is one.
 */
@Component({
  selector: 'ui-logo-mark',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @switch (mode()) {
      @case ('brand') {
        <span class="logo-tile" [style.--logo-size.px]="size()" aria-hidden="true">
          <svg class="logo-s logo-s--draw" viewBox="0 0 48 48" fill="none" stroke-width="5.5"
               stroke-linecap="round" stroke-linejoin="round">
            <path class="logo-arc-a" [attr.d]="arcTop" stroke="#fff" />
            <path class="logo-arc-b" [attr.d]="arcBottom" stroke="var(--teal-hi)" />
          </svg>
        </span>
      }
      @case ('spin') {
        <svg class="logo-glyph" [attr.width]="size()" [attr.height]="size()" viewBox="0 0 48 48"
             fill="none" aria-hidden="true">
          <circle class="logo-ring" cx="24" cy="26" r="19" stroke="var(--blue)" stroke-width="3"
                  stroke-linecap="round" />
          <g stroke-width="5.5" stroke-linecap="round" stroke-linejoin="round">
            <path [attr.d]="arcTop" stroke="var(--blue)" />
            <path [attr.d]="arcBottom" stroke="var(--teal-hi)" />
          </g>
        </svg>
      }
      @case ('trace') {
        <svg class="logo-glyph" [attr.width]="size()" [attr.height]="size()" viewBox="0 0 48 48"
             fill="none" stroke-width="5.5" stroke-linecap="round" stroke-linejoin="round"
             aria-hidden="true">
          <path class="logo-ghost" [attr.d]="arcTop" stroke="var(--blue)" />
          <path class="logo-ghost" [attr.d]="arcBottom" stroke="var(--blue)" />
          <path class="logo-trace" [attr.d]="arcWhole" stroke="var(--teal-hi)" />
        </svg>
      }
      @default {
        <svg class="logo-glyph" [attr.width]="size()" [attr.height]="size()" viewBox="0 0 48 48"
             fill="none" stroke-width="5.5" stroke-linecap="round" stroke-linejoin="round"
             aria-hidden="true">
          <path [attr.d]="arcTop" stroke="var(--blue)" />
          <path [attr.d]="arcBottom" stroke="var(--teal-hi)" />
        </svg>
      }
    }
  `,
})
export class UiLogoMark {
  readonly mode = input<LogoMarkMode>('brand');
  readonly size = input(27);

  protected readonly arcTop = ARC_TOP;
  protected readonly arcBottom = ARC_BOTTOM;
  protected readonly arcWhole = ARC_WHOLE;
}
