import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/**
 * A Phosphor icon, drawn from the sprite tools/build-icons.mjs writes to
 * /icons.svg. `name` is the Phosphor slug ('arrow-up-right'); add -fill for
 * the filled drawing ('star-fill').
 *
 * The name has to appear in the source as a quoted literal — in a template
 * attribute or a data file — because that is how the sprite builder finds it.
 * A name assembled at runtime from pieces will not be in the sprite.
 *
 * Inherits colour from the surrounding text and is hidden from assistive
 * tech; give the control it sits in a label instead.
 */
@Component({
  selector: 'ui-icon',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'ui-icon', '[style.--icon-size.px]': 'size()' },
  template: `
    <svg aria-hidden="true" focusable="false"><use [attr.href]="href()" /></svg>
  `,
})
export class UiIcon {
  readonly name = input.required<string>();
  /** Edge length in px. Omit to follow the font size of the surrounding text. */
  readonly size = input<number | null>(null);

  protected readonly href = computed(() => `/icons.svg#${this.name()}`);
}
