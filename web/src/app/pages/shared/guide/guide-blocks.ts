import { NgTemplateOutlet } from '@angular/common';
import {
  ChangeDetectionStrategy, Component, OnDestroy, TemplateRef, computed, contentChild, input,
  signal,
} from '@angular/core';
import { UiIcon } from '../../../ui';
import { GuideRich } from './guide-rich';

/**
 * Guide doc blocks — the pieces a guide section is written with.
 *
 * Two ways to use them:
 *
 *  1. Data-driven. Describe a section body as GuideBlock[] and hand it to
 *     <app-guide-blocks [blocks]="…">. This is how the ported guides work: the
 *     prototype's `blocks: [{ t: 'p', x: '…' }, { t: 'table', … }]` arrays map
 *     onto GuideBlock one to one.
 *  2. By hand. <app-guide-note>, <app-guide-table> and <app-guide-code> are
 *     usable on their own, and the rest are plain classes (see the list at the
 *     bottom of this comment) for markup you write yourself.
 *
 * All strings marked "rich" accept GuideRich markdown: `code`, **strong**,
 * *emphasis*, [label](/path#id).
 *
 * Classes for hand-written markup (styles/bundles/guide.css):
 *   p.gd-p                  body paragraph            h3.gd-h3 / h4.gd-h4   sub-headings
 *   p.gd-lede               larger intro paragraph    div.gd-label          mono uppercase label
 *   ol.gd-steps > li        numbered steps            ul.gd-bullets > li    bullets
 *   ul.gd-bullets.gd-bullets--2   two-column bullets (one column in a narrow main column)
 *   dl.gd-specs > div.gd-spec > dt + dd     spec / definition rows, two columns
 *   div.gd-table-wrap > table.gd-table      a real table in a scroll wrapper
 *   p.gd-caption            mono caption under a figure
 */

export type GuideNoteTone = 'neutral' | 'blue' | 'teal' | 'warn';

/**
 * One block of a section body. `C` is the payload type of page-specific
 * blocks (`t: 'custom'`), rendered by the template projected into
 * <app-guide-blocks>.
 */
export type GuideBlock<C = never> =
  /** Paragraph (rich). */
  | { t: 'p'; x: string }
  | { t: 'h3'; x: string }
  | { t: 'h4'; x: string }
  /** Mono uppercase label, e.g. "Inputs" above a spec list. */
  | { t: 'label'; x: string }
  /** `cols` is a space-separated list of column widths; only px values are applied ('160px 56px auto'). Cells are rich. */
  | { t: 'table'; head: string[]; rows: string[][]; cols?: string }
  /** Numbered steps (rich). */
  | { t: 'steps'; items: string[] }
  /** Bullets: a rich string, or [term, rich description]. `cols: 2` lays them out in two columns. */
  | { t: 'bullets'; items: (string | [string, string])[]; cols?: number }
  /** Spec rows: [name, rich description]. */
  | { t: 'specs'; items: [string, string][] }
  | { t: 'note'; tone: GuideNoteTone; icon?: string; title: string; x: string }
  /** Code block. Lines starting "Output" or "Response" are highlighted, plus the indexes in `bold`. */
  | { t: 'code'; lines: string[]; bold?: number[] }
  | { t: 'custom'; data: C };

/**
 * GuideNote — a tinted note card: icon, title, body.
 *
 *   <app-guide-note tone="warn" icon="warning" heading="Note">MD5 and SHA-1 are here because…</app-guide-note>
 *
 * Inputs: tone ('neutral' | 'blue' | 'teal' | 'warn', default 'blue'), icon (Phosphor name,
 * default 'info'), heading (string | null). Slot: the body (inline content).
 */
@Component({
  selector: 'app-guide-note',
  imports: [UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'gd-note', '[class]': '"gd-note--" + tone()' },
  template: `
    <ui-icon [name]="icon()" [size]="18" />
    <div class="gd-note-body">
      @if (heading()) { <b class="gd-note-title">{{ heading() }}</b> }
      <div class="gd-note-text"><ng-content /></div>
    </div>
  `,
})
export class GuideNote {
  readonly tone = input<GuideNoteTone>('blue');
  readonly icon = input('info');
  readonly heading = input<string | null>(null);
}

/**
 * GuideTable — a reference table in a scroll wrapper.
 *
 *   <app-guide-table [head]="['Mode', 'IV?', 'Properties']" [rows]="rows" cols="160px 56px auto" />
 *
 * Inputs: head (string[]), rows (string[][], rich cells), cols (optional column widths, see
 * GuideBlock 'table'). The host is the scroll wrapper (.gd-table-wrap). When the guide's main
 * column is under 640px the rows stack, each cell labelled with its column heading.
 *
 * For a table you would rather write out, use the classes directly:
 *   <div class="gd-table-wrap"><table class="gd-table"><thead>…</thead><tbody>…</tbody></table></div>
 */
@Component({
  selector: 'app-guide-table',
  imports: [GuideRich],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'gd-table-wrap' },
  template: `
    <table class="gd-table">
      @if (widths().length) {
        <colgroup>@for (w of widths(); track $index) { <col [class.gd-col-fixed]="w !== null" [style.--gd-w]="w"> }</colgroup>
      }
      <thead><tr>@for (h of head(); track $index) { <th scope="col">{{ h }}</th> }</tr></thead>
      <tbody>
        @for (row of rows(); track $index) {
          <tr>@for (cell of row; track $index) { <td [attr.data-label]="head()[$index]" [gdRich]="cell"></td> }</tr>
        }
      </tbody>
    </table>
  `,
})
export class GuideTable {
  readonly head = input.required<string[]>();
  readonly rows = input.required<string[][]>();
  readonly cols = input<string | null | undefined>(null);

  protected readonly widths = computed<(string | null)[]>(() => {
    const cols = this.cols();
    if (!cols) return this.head().length === 2 ? ['220px', null] : [];
    return cols.split(' ').filter(Boolean).map((w) => (/^[\d.]+px$/.test(w) ? w : null));
  });
}

/**
 * GuideCode — a code block with a copy button.
 *
 *   <app-guide-code [lines]="['Key:    0123…', '', 'Output: B11FFC78A4FB1B5A']" />
 *
 * Inputs
 *   lines   string[], required   one entry per line
 *   bold    number[]             indexes of lines to highlight
 *   auto    boolean              also highlight lines that start "Output" or "Response" (default true)
 *
 * The button is a real <button> (in-page UI, not navigation). It copies the
 * lines joined with newlines and reads "Copied" for a moment.
 */
@Component({
  selector: 'app-guide-code',
  imports: [UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'gd-code' },
  template: `
    <button type="button" class="gd-code-copy" (click)="copy()" [attr.aria-label]="copied() ? 'Copied' : 'Copy code'">
      @if (copied()) { <ui-icon name="check" [size]="13" /> } @else { <ui-icon name="copy" [size]="13" /> }
      <span>{{ copied() ? 'Copied' : 'Copy' }}</span>
    </button>
    <pre class="gd-code-pre" tabindex="0">@for (line of view(); track $index) {<span [class.gd-code-hi]="line.hi">{{ line.text }}</span>{{ newline }}}</pre>
  `,
})
export class GuideCode implements OnDestroy {
  readonly lines = input.required<string[]>();
  readonly bold = input<number[] | null | undefined>(null);
  readonly auto = input(true);

  protected readonly newline = '\n';
  protected readonly copied = signal(false);
  protected readonly view = computed(() => {
    const bold = this.bold() ?? [];
    return this.lines().map((text, i) => ({
      text,
      hi: bold.includes(i) || (this.auto() && /^(Output|Response)/.test(text)),
    }));
  });

  private timer: ReturnType<typeof setTimeout> | null = null;

  protected copy(): void {
    const text = this.lines().join('\n');
    const done = () => {
      this.copied.set(true);
      if (this.timer) clearTimeout(this.timer);
      this.timer = setTimeout(() => this.copied.set(false), 1600);
    };
    // Only ever called from a click, so the clipboard is there to ask for.
    navigator.clipboard?.writeText(text).then(done, () => { /* denied: leave the label alone */ });
  }

  ngOnDestroy(): void {
    if (this.timer) clearTimeout(this.timer);
  }
}

/**
 * GuideBlocks — renders a GuideBlock[] as a section body.
 *
 *   <app-guide-blocks [blocks]="section.blocks" />
 *
 * Page-specific blocks: give them `t: 'custom'` and project one <ng-template>;
 * it is stamped for each custom block with the block's `data` as its implicit
 * context.
 *
 *   <app-guide-blocks [blocks]="section.blocks">
 *     <ng-template let-panel><app-glass-panel [spec]="panels[panel.id]" /></ng-template>
 *   </app-guide-blocks>
 *
 * Input: blocks (GuideBlock<C>[], required). The host is display:block and the
 * blocks are its direct children, so their vertical margins collapse exactly
 * as written in guide.css.
 */
@Component({
  selector: 'app-guide-blocks',
  imports: [NgTemplateOutlet, GuideRich, GuideNote, GuideTable, GuideCode],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'gd-blocks' },
  template: `
    @for (b of blocks(); track $index) {
      @switch (b.t) {
        @case ('p') { <p class="gd-p" [gdRich]="b.x"></p> }
        @case ('h3') { <h3 class="gd-h3">{{ b.x }}</h3> }
        @case ('h4') { <h4 class="gd-h4">{{ b.x }}</h4> }
        @case ('label') { <div class="gd-label">{{ b.x }}</div> }
        @case ('table') { <app-guide-table [head]="b.head" [rows]="b.rows" [cols]="b.cols" /> }
        @case ('steps') {
          <ol class="gd-steps">@for (s of b.items; track $index) { <li><span [gdRich]="s"></span></li> }</ol>
        }
        @case ('bullets') {
          <ul class="gd-bullets" [class.gd-bullets--2]="b.cols === 2">
            @for (s of b.items; track $index) {
              <li>@if (term(s); as t) {<strong>{{ t }}</strong> — }<span [gdRich]="body(s)"></span></li>
            }
          </ul>
        }
        @case ('specs') {
          <dl class="gd-specs">
            @for (s of b.items; track $index) {
              <div class="gd-spec"><dt>{{ s[0] }}</dt>@if (s[1]) { <dd [gdRich]="s[1]"></dd> }</div>
            }
          </dl>
        }
        @case ('note') {
          <app-guide-note [tone]="b.tone" [icon]="b.icon || 'info'" [heading]="b.title"><span [gdRich]="b.x"></span></app-guide-note>
        }
        @case ('code') { <app-guide-code [lines]="b.lines" [bold]="b.bold" /> }
        @case ('custom') {
          @if (custom(); as tpl) {
            <ng-container [ngTemplateOutlet]="tpl" [ngTemplateOutletContext]="{ $implicit: b.data }" />
          }
        }
      }
    }
  `,
})
export class GuideBlocks<C = unknown> {
  readonly blocks = input.required<readonly GuideBlock<C>[]>();
  protected readonly custom = contentChild(TemplateRef);

  protected term(item: string | [string, string]): string | null {
    return Array.isArray(item) ? item[0] : null;
  }

  protected body(item: string | [string, string]): string {
    return Array.isArray(item) ? item[1] : item;
  }
}
