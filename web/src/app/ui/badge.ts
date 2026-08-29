import { Component, ChangeDetectionStrategy, input } from '@angular/core';

export type BadgeTone = 'blue' | 'teal' | 'green' | 'yellow' | 'purple' | 'red';

@Component({
  selector: 'ui-badge',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="badge badge--{{ tone() }}"><ng-content /></span>`,
})
export class UiBadge {
  readonly tone = input<BadgeTone>('blue');
}
