import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { UiIcon } from './icon';

export type BadgeTone =
  | 'neutral' | 'blue' | 'teal' | 'green' | 'yellow' | 'purple' | 'red'
  | 'up' | 'down' | 'warn';

/** Status pill: states (Available, Beta, Dev), counts and deltas. */
@Component({
  selector: 'ui-badge',
  imports: [UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="badge badge--{{ tone() }}" [class.badge--mono]="mono()">@if (dot()) {<span class="badge-dot"></span>}@if (icon()) {<ui-icon [name]="icon()!" [size]="11" />}<ng-content /></span>`,
})
export class UiBadge {
  readonly tone = input<BadgeTone>('blue');
  readonly icon = input<string | null>(null);
  readonly dot = input(false);
  readonly mono = input(false);
}
