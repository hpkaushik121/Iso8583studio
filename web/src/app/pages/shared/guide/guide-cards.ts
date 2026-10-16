import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BadgeTone, UiBadge, UiIcon } from '../../../ui';
import { GuideRich } from './guide-rich';

/** A card in the "all tools" / "runtime tabs" grid: jumps to a section of the same page. */
export interface GuideCard {
  /** Id of the section the card jumps to. */
  id: string;
  /** Phosphor icon name (quoted literal somewhere under src/app). */
  icon: string;
  name: string;
  desc: string;
  /** Mono teal line at the top right ("01 · Offline authentication"). */
  tag?: string;
  /** Optional pill next to the tag ("Popular", "New"). */
  badge?: string;
  badgeTone?: BadgeTone;
}

/**
 * GuideCards — a grid of glass link cards, each jumping to a section of this
 * page. Three columns when the guide's main column is 900px or wider, two from
 * 640px, one below.
 *
 *   <app-guide-cards [items]="cards" more="View details" />
 *
 * Inputs: items (GuideCard[], required), more (the link line at the foot of each card; default
 * 'View details').
 *
 * Each card is an <a class="gd-card hub-card"> with the name in .hub-title, so a click reports
 * hub_card_click (card_title = name, card_badge = the pill) exactly as the old in-page hub cards
 * did. Each card sits in a .ds-item cell, so inside an app-guide-section they rise in with its
 * reveal.
 */
@Component({
  selector: 'app-guide-cards',
  imports: [RouterLink, UiBadge, UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'gd-cards' },
  template: `
    @for (card of items(); track card.id; let i = $index) {
      <div class="gd-card-cell ds-item" [style.--d]="delay(i)">
        <a class="gd-card hub-card" [routerLink]="[]" [fragment]="card.id">
          <span class="gd-card-top">
            <ui-icon [name]="card.icon" [size]="22" />
            <span class="gd-card-tags">
              @if (card.badge) { <ui-badge [tone]="card.badgeTone || 'teal'">{{ card.badge }}</ui-badge> }
              @if (card.tag) { <span class="gd-card-tag">{{ card.tag }}</span> }
            </span>
          </span>
          <strong class="gd-card-name hub-title">{{ card.name }}</strong>
          <span class="gd-card-desc">{{ card.desc }}</span>
          <span class="gd-card-go">{{ more() }}<ui-icon name="arrow-right" [size]="13" /></span>
        </a>
      </div>
    }
  `,
})
export class GuideCards {
  readonly items = input.required<readonly GuideCard[]>();
  readonly more = input('View details');

  /** Staggered as a two-column grid, which is what the main column shows on a desktop. */
  protected delay(i: number): number {
    return 400 + Math.floor(i / 2) * 260 + (i % 2) * 120;
  }
}

/** An icon, a name and a line of copy — a non-interactive fact card. */
export interface GuideFeature {
  icon: string;
  name: string;
  desc: string;
}

/**
 * GuideFeatures — a two-column grid of small fact cards (icon tile, name,
 * one line), used under a guide's introduction. One column when the main
 * column is under 640px.
 *
 *   <app-guide-features [items]="[{ icon: 'lock', name: 'AES', desc: '128 / 192 / 256-bit…' }]" />
 *
 * Input: items (GuideFeature[], required). Not links — plain <div>s.
 */
@Component({
  selector: 'app-guide-features',
  imports: [UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'gd-features' },
  template: `
    @for (item of items(); track item.name; let i = $index) {
      <div class="gd-feature ds-item" [style.--d]="320 + floor(i / 2) * 200 + (i % 2) * 90">
        <span class="gd-feature-icon"><ui-icon [name]="item.icon" [size]="17" /></span>
        <span class="gd-feature-text"><strong>{{ item.name }}</strong><span>{{ item.desc }}</span></span>
      </div>
    }
  `,
})
export class GuideFeatures {
  readonly items = input.required<readonly GuideFeature[]>();
  protected readonly floor = Math.floor;
}

/**
 * GuideTips — a numbered list of tips in one radial-lit card.
 *
 *   <app-guide-tips [items]="['Always verify the **KCV** after…', '…']" />
 *
 * Input: items (string[], rich, required). Renders <ol class="gd-tips card card--radial">.
 */
@Component({
  selector: 'app-guide-tips',
  imports: [GuideRich],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'gd-tips-host ds-hold', '[style.--d]': '200' },
  template: `
    <ol class="gd-tips card card--radial">
      @for (tip of items(); track $index) {
        <li class="gd-tip"><span class="gd-tip-n" aria-hidden="true">{{ number($index) }}</span><span class="gd-tip-text" [gdRich]="tip"></span></li>
      }
    </ol>
  `,
})
export class GuideTips {
  readonly items = input.required<readonly string[]>();

  protected number(i: number): string {
    return String(i + 1).padStart(2, '0');
  }
}
