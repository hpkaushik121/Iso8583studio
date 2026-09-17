import { Component, ChangeDetectionStrategy, input } from '@angular/core';

/** A documentation section with a linkable heading. */
@Component({
  selector: 'ui-section',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="doc-section" [id]="anchor()">
      @if (heading()) { <h2>{{ heading() }}</h2> }
      <ng-content />
    </section>
  `,
})
export class UiSection {
  readonly heading = input<string | null>(null);
  readonly anchor = input<string | null>(null);
}
