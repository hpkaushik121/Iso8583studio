import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { RouterLink } from '@angular/router';

export type CardVariant = 'plain' | 'hub' | 'tile' | 'post';

/**
 * A link card when given a target, a plain container otherwise.
 *
 * One <ng-content>, stamped into the winning branch — see the note on
 * UiButton for why a copy per branch silently drops the content.
 */
@Component({
  selector: 'ui-card',
  imports: [NgTemplateOutlet, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ng-template #body><ng-content /></ng-template>
    @if (routerLink()) {
      <a [routerLink]="routerLink()" [class]="classes()">
        <ng-container [ngTemplateOutlet]="body" />
      </a>
    } @else if (href()) {
      <a [href]="href()" [class]="classes()">
        <ng-container [ngTemplateOutlet]="body" />
      </a>
    } @else {
      <div [class]="classes()">
        <ng-container [ngTemplateOutlet]="body" />
      </div>
    }
  `,
})
export class UiCard {
  readonly variant = input<CardVariant>('plain');
  readonly routerLink = input<string | unknown[] | null>(null);
  readonly href = input<string | null>(null);

  protected classes(): string {
    const interactive = this.routerLink() || this.href() ? ' card--interactive' : '';
    const variant = this.variant() === 'plain' ? '' : ` card--${this.variant()}`;
    return `card${variant}${interactive}`;
  }
}
