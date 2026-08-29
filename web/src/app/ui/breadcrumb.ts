import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { RouterLink } from '@angular/router';

export interface Crumb { label: string; link?: string; }

@Component({
  selector: 'ui-breadcrumb',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <nav class="breadcrumb" aria-label="Breadcrumb">
      @for (crumb of items(); track crumb.label; let last = $last) {
        @if (crumb.link && !last) {
          <a [routerLink]="crumb.link">{{ crumb.label }}</a>
          <span class="breadcrumb-sep" aria-hidden="true">/</span>
        } @else {
          <span aria-current="page">{{ crumb.label }}</span>
        }
      }
    </nav>
  `,
})
export class UiBreadcrumb {
  readonly items = input.required<Crumb[]>();
}
