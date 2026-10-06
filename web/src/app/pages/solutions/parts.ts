import { RouterLink } from '@angular/router';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { EXTERNAL } from '../../core/site-nav';
import { BadgeTone, UiBadge, UiIcon, UiReveal } from '../../ui';
import { SolLive } from './cycle';

/** A Phosphor icon name and the word beside it. */
export type SolMark = readonly [icon: string, label: string];
/** Number, title, description. */
export type SolBenefit = readonly [n: string, title: string, desc: string];

/**
 * The strip under the hero: one line of copy above a looping row of
 * icon-and-word marks. The row is rendered twice so the loop has no seam; the
 * second copy is hidden from assistive tech.
 */
@Component({
  selector: 'sol-strip',
  imports: [UiIcon, UiReveal, SolLive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'sol-block' },
  template: `
    <section class="sol-strip" [class.sol-strip--tight]="tight()" [attr.data-sect]="sect()" uiReveal solLive>
      <div class="ds-hold">
        <p class="sol-strip-caption">{{ caption() }}</p>
        <div class="sol-strip-rail">
          <div class="sol-strip-track ds-marquee">
            @for (copy of copies; track copy) {
              <div class="sol-strip-set" [attr.aria-hidden]="copy ? 'true' : null">
                @for (mark of marks(); track mark[1]) {
                  <span class="sol-strip-mark"><ui-icon [name]="mark[0]" [size]="22" /><span>{{ mark[1] }}</span></span>
                  <span class="sol-strip-rule" aria-hidden="true"></span>
                }
              </div>
            }
          </div>
        </div>
      </div>
    </section>
  `,
})
export class SolStrip {
  readonly caption = input.required<string>();
  readonly marks = input.required<readonly SolMark[]>();
  readonly sect = input('capability_strip');
  /** 24px above instead of 32px. */
  readonly tight = input(false);
  protected readonly copies = [0, 1];
}

/**
 * Feature card: heading (with an optional mono badge) and one line of copy at
 * the top, the projected visual running to the bottom edge. A div, never a
 * section — cards are not page sections.
 */
@Component({
  selector: 'sol-bento',
  imports: [UiBadge],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'sol-bento' },
  template: `
    @if (glow()) { <span class="sol-bento-glow" aria-hidden="true"></span> }
    @if (radial()) { <span class="sol-bento-radial" aria-hidden="true"></span> }
    <div class="sol-bento-head">
      <h3 class="sol-bento-title">{{ heading() }}@if (badge()) {<ui-badge [tone]="badgeTone()" [mono]="true">{{ badge() }}</ui-badge>}</h3>
      <p class="sol-bento-desc">{{ desc() }}</p>
    </div>
    <div class="sol-bento-body"><ng-content /></div>
  `,
})
export class SolBento {
  readonly heading = input.required<string>();
  readonly desc = input.required<string>();
  readonly badge = input<string | null>(null);
  readonly badgeTone = input<BadgeTone>('blue');
  readonly glow = input(false);
  readonly radial = input(false);
}

/** Six numbered benefits in two ruled rows of three. */
@Component({
  selector: 'sol-benefits',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'sol-benefits' },
  template: `
    @for (row of rows(); track $index; let r = $index) {
      <div class="sol-ben-row" [class.sol-ben-row--dim]="r > 0">
        @for (item of row; track item[0]; let k = $index) {
          <div class="sol-ben ds-item" [style.--d]="500 + r * 340 + k * 110">
            <p class="sol-ben-n">{{ item[0] }}</p>
            <h3>{{ item[1] }}</h3>
            <p class="sol-ben-desc">{{ item[2] }}</p>
          </div>
        }
      </div>
    }
  `,
})
export class SolBenefits {
  readonly items = input.required<readonly SolBenefit[]>();
  protected readonly rows = computed(() => {
    const items = this.items();
    const rows: SolBenefit[][] = [];
    for (let i = 0; i < items.length; i += 3) rows.push(items.slice(i, i + 3));
    return rows;
  });
}

/**
 * The closing CTA. Same surface as ui-cta-panel (section.cta.cta-panel with
 * the two planets) but laid out in two columns with the page's art on the
 * right, and named by `sect` — these pages report their closing section under
 * the headline they have always used, not 'final_cta'.
 *
 * Both actions are anchors: the primary to the page's own enquiry form, the
 * secondary to the release page. It is an in-page anchor rather than a link to
 * /contact because each solution page now carries the form itself — sending
 * someone who is ready to talk off to another page was the long way round.
 * The click still reports as final_cta_click: the listener matches on the
 * enclosing section.cta before it ever looks at the href.
 *
 * routerLink with a fragment rather than a bare href="#enquiry": the document
 * has <base href="/">, against which a bare fragment resolves to the site
 * root, not to this page. check-links.mjs fails the build on that.
 * Project a footer line as `[solCloseFoot]` and the art as the default content.
 */
@Component({
  selector: 'sol-close',
  imports: [RouterLink, UiIcon, UiReveal, SolLive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'sol-block' },
  template: `
    <section class="cta cta-panel sol-close" id="contact" [attr.data-sect]="sect()"
             uiReveal [uiRevealDelay]="250" solLive>
      <div class="cta-planet cta-planet--left" aria-hidden="true"></div>
      <div class="cta-planet cta-planet--right" aria-hidden="true"></div>
      <div class="cta-panel-in sol-close-in">
        <div class="sol-close-copy ds-hold">
          <div class="sol-mono sol-mono--teal">{{ kicker() }}</div>
          <h2>{{ sect() }}</h2>
          <p class="sol-close-text">{{ text() }}</p>
          <div class="sol-close-actions">
            <a class="btn btn--primary btn--glow" [routerLink]="[]" fragment="enquiry">{{ cta() }}<ui-icon name="arrow-right" [size]="16" /></a>
            <a class="btn btn--secondary" [href]="releases">Download Studio<ui-icon name="arrow-up-right" [size]="16" /></a>
          </div>
          <ng-content select="[solCloseFoot]" />
        </div>
        <div class="sol-close-art ds-hold" aria-hidden="true" [style.--d]="260"><ng-content /></div>
      </div>
    </section>
  `,
})
export class SolClose {
  /** The headline, which is also the section_view name. */
  readonly sect = input.required<string>();
  readonly kicker = input.required<string>();
  readonly text = input.required<string>();
  /** Label of the primary CTA (links to /contact). */
  readonly cta = input.required<string>();
  protected readonly releases = EXTERNAL.releases;
}
