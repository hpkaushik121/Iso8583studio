import { Component, ChangeDetectionStrategy, input } from '@angular/core';

export type TagTone = 'blue' | 'teal' | 'muted';

@Component({
  selector: 'ui-tag',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="ui-tag" [class.ui-tag--teal]="tone() === 'teal'"
                   [class.ui-tag--muted]="tone() === 'muted'"><ng-content /></span>`,
})
export class UiTag {
  readonly tone = input<TagTone>('blue');
}
