import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

interface Word { text: string; delay: number; last: boolean; }

/**
 * Headline text that un-blurs word by word. The spans are rendered on the
 * server, so the headline is ordinary readable text in the prerendered page;
 * the animation fires from an ancestor carrying .ds-in (see UiReveal).
 *
 * A newline in `text` becomes a line break.
 */
@Component({
  selector: 'ui-words',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { style: 'display: contents' },
  template: `@for (line of lines(); track $index; let first = $first) {@if (!first) {<br>}@for (word of line; track $index) {<span class="ds-word" [style.--d]="word.delay">{{ word.text }}</span>{{ word.last ? '' : ' ' }}}}`,
})
export class UiWords {
  readonly text = input.required<string>();
  /** Delay before the first word, in ms. */
  readonly base = input(0);
  /** Gap between words, in ms. */
  readonly step = input(90);

  protected readonly lines = computed<Word[][]>(() => {
    let n = 0;
    return this.text().split('\n').map((line) => {
      const words = line.split(' ').filter(Boolean);
      return words.map((text, i) => ({
        text,
        delay: this.base() + n++ * this.step(),
        last: i === words.length - 1,
      }));
    });
  });
}
