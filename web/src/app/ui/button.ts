import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { RouterLink } from '@angular/router';

export type ButtonVariant = 'primary' | 'ghost';
export type ButtonSize = 'md' | 'lg';

/**
 * Renders as an internal link, an external link or a real button depending on
 * which input is set, so callers never hand-roll an anchor that only looks
 * like a button.
 *
 * The label goes through a single <ng-content> captured in a template and
 * stamped into whichever branch wins. Repeating <ng-content> once per branch
 * does not work: a component has one default projection slot, so only the
 * last copy receives the content and the link variants render empty.
 */
@Component({
  selector: 'ui-button',
  imports: [NgTemplateOutlet, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ng-template #label><ng-content /></ng-template>
    @if (routerLink()) {
      <a [routerLink]="routerLink()" [class]="classes()">
        <ng-container [ngTemplateOutlet]="label" />
      </a>
    } @else if (href()) {
      <a [href]="href()" [attr.target]="external() ? '_blank' : null"
         [attr.rel]="external() ? 'noopener' : null" [class]="classes()">
        <ng-container [ngTemplateOutlet]="label" />
      </a>
    } @else {
      <button [type]="type()" [class]="classes()" [disabled]="disabled()">
        <ng-container [ngTemplateOutlet]="label" />
      </button>
    }
  `,
})
export class UiButton {
  readonly variant = input<ButtonVariant>('primary');
  readonly size = input<ButtonSize>('md');
  readonly routerLink = input<string | unknown[] | null>(null);
  readonly href = input<string | null>(null);
  readonly external = input(false);
  readonly disabled = input(false);
  readonly type = input<'button' | 'submit'>('button');

  protected classes(): string {
    return `btn btn--${this.variant()}${this.size() === 'lg' ? ' btn--lg' : ''}`;
  }
}
