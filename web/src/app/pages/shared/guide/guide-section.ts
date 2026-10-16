import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { UiReveal, UiWords } from '../../../ui';
import { GuideRich } from './guide-rich';

/**
 * GuideSection — one top-level section of a guide: an optional mono eyebrow,
 * the h2 (word-by-word), an optional lede, then whatever you project.
 *
 * Renders a real <section class="doc-section gd-section" id="…">, which is
 * what the page's section_view tracking reports (by id) and what the rail's
 * scroll-spy watches. Use it only for top-level sections and never nest one
 * inside another; cards and panels inside it are <div>s.
 *
 *   <app-guide-section anchor="sda" heading="SDA Verification"
 *                      eyebrow="01 · Offline authentication"
 *                      intro="The terminal verifies **issuer-signed** static data…">
 *     <app-guide-blocks [blocks]="blocks" />
 *   </app-guide-section>
 *
 * Inputs
 *   anchor       string, required    the section's id (the input is not called `id` so the host
 *                                    element does not end up carrying the same id)
 *   heading      string, required    the h2
 *   eyebrow      string | null       mono line above the h2
 *   eyebrowTone  'teal' | 'faint'    default 'teal'
 *   intro        string | null       lede under the h2; GuideRich markdown
 *   threshold    number              share of the section that must be visible before its
 *                                    reveal plays (default .04 — sections are tall)
 *
 * Slot: (default) — the section body. Use the gd-* blocks (guide-blocks.ts) or the gd-* classes
 * in styles/bundles/guide.css; bare <p>/<ul>/<h3> inside a .doc-section pick up older global
 * rules, so give them the gd- class (gd-p, gd-h3, gd-h4, gd-bullets, gd-steps).
 *
 * Spacing: 72px above every section; the first child of .gd-main gets none.
 */
@Component({
  selector: 'app-guide-section',
  imports: [UiReveal, UiWords, GuideRich],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'gd-section-host' },
  template: `
    <section class="doc-section gd-section" [id]="anchor()" uiReveal [uiRevealThreshold]="threshold()">
      @if (eyebrow()) {
        <div class="gd-eyebrow ds-hold" [class.gd-eyebrow--faint]="eyebrowTone() === 'faint'">{{ eyebrow() }}</div>
      }
      <h2 class="gd-h2"><ui-words [text]="heading()" /></h2>
      @if (intro()) { <p class="gd-lede ds-hold" [style.--d]="220" [gdRich]="intro()!"></p> }
      <ng-content />
    </section>
  `,
})
export class GuideSection {
  readonly anchor = input.required<string>();
  readonly heading = input.required<string>();
  readonly eyebrow = input<string | null>(null);
  readonly eyebrowTone = input<'teal' | 'faint'>('teal');
  readonly intro = input<string | null>(null);
  readonly threshold = input(0.04);
}
