import { Component, ChangeDetectionStrategy } from '@angular/core';

/** Wraps wide tables so they scroll inside their own box rather than forcing
 *  the page to scroll horizontally. */
@Component({
  selector: 'ui-table',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div class="table-wrapper"><ng-content /></div>`,
})
export class UiTable {}
