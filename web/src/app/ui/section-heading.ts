import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { UiWords } from './words';
import { UiReveal } from './reveal';

/**
 * Section headline with the word-by-word reveal, an optional eyebrow and a
 * one-line subtitle. Reveals itself on scroll.
 *
 * Eyebrows are for facts ("9 simulators"), not labels ("Features").
 */
@Component({
  selector: 'ui-section-heading',
  imports: [UiWords],
  hostDirectives: [UiReveal],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'section-heading', '[class.section-heading--left]': "align() === 'left'" },
  template: `
    @if (eyebrow()) { <div class="section-eyebrow">{{ eyebrow() }}</div> }
    <h2 [class.section-title--h2]="size() === 'h2'"><ui-words [text]="heading()" [base]="base()" /></h2>
    @if (sub()) { <p class="section-sub ds-hold" [style.--d]="base() + 250">{{ sub() }}</p> }
  `,
})
export class UiSectionHeading {
  readonly heading = input.required<string>();
  readonly sub = input<string | null>(null);
  readonly eyebrow = input<string | null>(null);
  readonly align = input<'center' | 'left'>('center');
  /** 'display' is the marketing size; 'h2' the smaller in-page size. */
  readonly size = input<'display' | 'h2'>('display');
  readonly base = input(0);
}
