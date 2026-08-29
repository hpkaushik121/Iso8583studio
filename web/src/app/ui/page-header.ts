import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { UiKicker } from './kicker';

@Component({
  selector: 'ui-page-header',
  imports: [UiKicker],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (kicker()) { <ui-kicker>{{ kicker() }}</ui-kicker> }
    <div class="page-title-row">
      <h1 class="page-title">{{ heading() }}</h1>
      <ng-content select="[slot=badge]" />
    </div>
    @if (description()) { <p class="page-description">{{ description() }}</p> }
  `,
})
export class UiPageHeader {
  readonly kicker = input<string | null>(null);
  readonly heading = input.required<string>();
  readonly description = input<string | null>(null);
}
