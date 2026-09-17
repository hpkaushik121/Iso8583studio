import { Component, ChangeDetectionStrategy, input } from '@angular/core';

@Component({
  selector: 'ui-spec-row',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div class="spec-row"><span class="k">{{ key() }}</span><span class="v">{{ value() }}</span></div>`,
})
export class UiSpecRow {
  readonly key = input.required<string>();
  readonly value = input.required<string>();
}
