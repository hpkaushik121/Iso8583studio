import { Component, ChangeDetectionStrategy, input } from '@angular/core';

@Component({
  selector: 'ui-icon-tile',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="icon-tile" [class.icon-tile--lg]="large()" aria-hidden="true">{{ glyph() }}</span>`,
})
export class UiIconTile {
  readonly glyph = input.required<string>();
  readonly large = input(false);
}
