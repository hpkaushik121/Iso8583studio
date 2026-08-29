import { Component, ChangeDetectionStrategy, input } from '@angular/core';

export type CalloutTone = 'note' | 'tip' | 'warn' | 'danger';

@Component({
  selector: 'ui-callout',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <aside class="callout callout--{{ tone() }}">
      @if (heading()) { <p class="callout-title">{{ heading() }}</p> }
      <ng-content />
    </aside>
  `,
})
export class UiCallout {
  readonly tone = input<CalloutTone>('note');
  readonly heading = input<string | null>(null);
}
