import { Component, ChangeDetectionStrategy, input } from '@angular/core';

@Component({
  selector: 'ui-figure',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <figure class="shot-fig">
      <img [src]="src()" [alt]="alt()" loading="lazy" decoding="async">
      @if (caption()) { <figcaption class="shot-cap">{{ caption() }}</figcaption> }
    </figure>
  `,
})
export class UiFigure {
  readonly src = input.required<string>();
  readonly alt = input('');
  readonly caption = input<string | null>(null);
}
