import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { RouterLink } from '@angular/router';
import { UiIcon } from './icon';

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'outline';
export type ButtonSize = 'sm' | 'md' | 'lg';

/**
 * Pill button. Renders as an internal link, an external link or a real button
 * depending on which input is set, so callers never hand-roll an anchor that
 * only looks like a button.
 *
 * Anything that navigates must be given `routerLink` or `href`: analytics
 * reports clicks on anchors only, so a <button> that navigates from a click
 * handler is invisible to it.
 *
 * The label goes through a single <ng-content> captured in a template and
 * stamped into whichever branch wins. Repeating <ng-content> once per branch
 * does not work: a component has one default projection slot, so only the
 * last copy receives the content and the link variants render empty.
 */
@Component({
  selector: 'ui-button',
  imports: [NgTemplateOutlet, RouterLink, UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class.ui-button--block]': 'block()' },
  template: `
    <ng-template #label>
      @if (icon()) { <ui-icon [name]="icon()!" [size]="size() === 'sm' ? 14 : 16" /> }
      <ng-content />
      @if (iconRight()) { <ui-icon [name]="iconRight()!" [size]="size() === 'sm' ? 14 : 16" /> }
    </ng-template>
    @if (routerLink()) {
      <a [routerLink]="routerLink()" [fragment]="fragment() ?? undefined" [class]="classes()">
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
  readonly fragment = input<string | null>(null);
  readonly href = input<string | null>(null);
  readonly external = input(false);
  readonly disabled = input(false);
  readonly type = input<'button' | 'submit'>('button');
  /** Phosphor icon before the label. */
  readonly icon = input<string | null>(null);
  /** Phosphor icon after the label; 'arrow-up-right' is the CTA signature. */
  readonly iconRight = input<string | null>(null);
  /** Keeps the hover halo on at rest. One per surface. */
  readonly glow = input(false);
  /** Full width of its container. */
  readonly block = input(false);

  protected classes(): string {
    const size = this.size() === 'md' ? '' : ` btn--${this.size()}`;
    return `btn btn--${this.variant()}${size}${this.glow() ? ' btn--glow' : ''}${this.block() ? ' btn--block' : ''}`;
  }
}
