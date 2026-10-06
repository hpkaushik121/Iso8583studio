import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { UiIcon } from './icon';

/**
 * Icon-only control. Renders an anchor when given `href` and a button
 * otherwise; `label` is required either way because the icon carries no text.
 *
 * On an anchor the label is also the `title`, which is what analytics falls
 * back to for link_text when a link has no visible text.
 */
@Component({
  selector: 'ui-icon-button',
  imports: [UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { style: 'display: contents' },
  template: `
    @if (href()) {
      <a [href]="href()" [attr.title]="label()" [attr.aria-label]="label()" rel="noopener"
         [class]="classes()" [style.--ib-size.px]="size()">
        <ui-icon [name]="icon()" [size]="iconSize()" />
      </a>
    } @else {
      <button type="button" [attr.title]="label()" [attr.aria-label]="label()"
              [class]="classes()" [style.--ib-size.px]="size()">
        <ui-icon [name]="icon()" [size]="iconSize()" />
      </button>
    }
  `,
})
export class UiIconButton {
  readonly icon = input.required<string>();
  readonly label = input.required<string>();
  readonly href = input<string | null>(null);
  readonly size = input(36);
  readonly variant = input<'ghost' | 'filled'>('ghost');
  readonly round = input(false);
  /** Extra classes for the rendered control, e.g. a tracked hook. */
  readonly controlClass = input('');

  protected iconSize(): number { return Math.round(this.size() * 0.5); }

  protected classes(): string {
    return `icon-btn icon-btn--${this.variant()}${this.round() ? ' icon-btn--round' : ''} ${this.controlClass()}`.trim();
  }
}
