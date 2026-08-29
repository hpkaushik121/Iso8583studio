import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'ui-kicker',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<p class="kicker"><ng-content /></p>`,
})
export class UiKicker {}
