import { ChangeDetectionStrategy, Component, input, signal } from '@angular/core';
import { UiIcon } from './icon';

export interface AccordionItem { q: string; a: string; }

let nextId = 0;

/**
 * FAQ accordion. Every answer stays in the DOM whether or not its row is
 * open — the row animates its height — so the prerendered page carries the
 * full text for readers and crawlers alike.
 *
 * `stagger` (ms) drops the rows in one after another from an ancestor
 * uiReveal.
 */
@Component({
  selector: 'ui-accordion',
  imports: [UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'accordion' },
  template: `
    @for (item of items(); track item.q; let i = $index) {
      <div class="accordion-row" [class.is-open]="open() === i" [class.ds-item]="stagger() > 0"
           [style.--d]="stagger() > 0 ? base() + i * stagger() : null">
        <button class="accordion-q" type="button" [attr.aria-expanded]="open() === i"
                [attr.aria-controls]="uid + '-' + i" (click)="toggle(i)">
          {{ item.q }}
          <span class="accordion-caret" aria-hidden="true"><ui-icon name="arrow-down" [size]="14" /></span>
        </button>
        <div class="accordion-a" role="region" [id]="uid + '-' + i">
          <div><p>{{ item.a }}</p></div>
        </div>
      </div>
    }
  `,
})
export class UiAccordion {
  readonly items = input.required<AccordionItem[]>();
  /** Index of the row open at first; -1 for none. */
  readonly defaultOpen = input(0);
  readonly stagger = input(0);
  readonly base = input(0);

  protected readonly uid = `acc-${nextId++}`;
  /** undefined until the reader first toggles a row, so defaultOpen applies. */
  private readonly chosen = signal<number | undefined>(undefined);

  protected open(): number {
    return this.chosen() ?? this.defaultOpen();
  }

  protected toggle(i: number): void {
    this.chosen.set(this.open() === i ? -1 : i);
  }
}
