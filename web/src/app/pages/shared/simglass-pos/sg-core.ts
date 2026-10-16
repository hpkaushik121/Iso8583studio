import {
  ChangeDetectionStrategy, Component, ElementRef, OnDestroy, PLATFORM_ID, afterNextRender,
  computed, effect, inject, input, signal, untracked,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { UiIcon } from '../../../ui';
import { GlassTicker } from '../glass/glass-ticker';

/**
 * The glass primitives the POS and APDU simulator guides draw their screens
 * with: the window frame that runs a screen's scripted loop, and the small
 * app widgets (button, field, toggle row, pane) the screens are made of.
 *
 * Styles: styles/bundles/_sims-pos.css — every class is prefixed sg-. A screen
 * is authored 1000 design units wide (460 when its column is under 600px) and
 * every length is a multiple of --u, one design unit in container units, so a
 * screen scales with its column in CSS alone.
 */

/** A tab of the window's tab bar: [Phosphor icon or null, label]. */
export type SgTab = readonly [icon: string | null, label: string];

/** Repeatable hex filler: the same seed always gives the same digits. */
export function sgHex(seed: string, n: number): string {
  let h = 2166136261;
  for (const c of seed) {
    h ^= c.charCodeAt(0);
    h = Math.imul(h, 16777619) >>> 0;
  }
  let s = '';
  while (s.length < n) {
    h = (Math.imul(h, 1103515245) + 12345) >>> 0;
    s += (h >>> 8).toString(16).toUpperCase().padStart(6, '0');
  }
  return s.slice(0, n);
}

/** 'A0B1C2' → 'A0 B1 C2'. */
export const sgPairs = (hex: string): string => hex.replace(/(..)/g, '$1 ').trim();

/**
 * SgGlass — the window a screen sits in (title bar, tab bar, body) and the
 * clock its animation runs on.
 *
 *   <sg-glass #g heading="POS Terminal - POS - 1" sub="Device" [tabs]="tabs" [tab]="0"
 *             app="pos-simulator" badge="POS" aria="…" [steps]="11" [period]="1000" [settled]="6">
 *     <pg-terminal [t]="g.t()" />
 *   </sg-glass>
 *
 * Inputs
 *   heading   window title                     sub      path label after the app name
 *   tabs      SgTab[]                          tab      index of the selected tab
 *   app       'pos-simulator'                  badge    pill at the right of the title bar
 *   aria      description of the drawing (the window is role="img")
 *   steps     number of steps in the loop      period   ms between steps
 *   settled   the step the screen rests on: what is prerendered, and what stays under
 *             prefers-reduced-motion
 *   active    false while the screen is a hidden tab panel; turning true replays the
 *             entrance and restarts the loop from its empty state
 *
 * `t` is the current step (-1 = the empty state before a replayed loop starts).
 * The loop starts when the window scrolls into view and pauses when it leaves;
 * one shared GlassTicker drives every window on the page.
 */
@Component({
  selector: 'sg-glass',
  imports: [UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'sg-stage', '[class.off]': '!onScreen()' },
  template: `
    <div class="sg-wrap">
      <div class="sg-bg" aria-hidden="true"><span></span><span></span><span></span></div>
      <div class="sg" role="img" [attr.aria-label]="aria()" [class.sg--armed]="armed()" [class.in]="entered()">
        <div class="sg-dots" aria-hidden="true"></div>
        <div class="sg-head sg-layer" [style.--d]="0">
          <span class="sg-head-l">
            <span class="sg-lights" aria-hidden="true"><span></span><span></span><span></span></span>
            <ui-icon name="arrow-left" class="sg-cf" />
            <span class="sg-title">{{ heading() }}</span>
          </span>
          <span class="sg-head-r">
            <span class="sg-path sg-wide">{{ app() }} › {{ sub() }}</span>
            <span class="sg-badge">{{ badge() }}</span>
          </span>
        </div>
        <div class="sg-tabs sg-layer" [style.--d]="90">
          @for (item of tabView(); track item.label; let i = $index) {
            <span class="sg-tab" [class.on]="i === tab()">@if (item.icon) {<ui-icon [name]="item.icon" class="sg-wide" />}<span>{{ item.first }}<span class="sg-wide">{{ item.rest }}</span></span></span>
          }
        </div>
        <div class="sg-body"><ng-content /></div>
        <div class="sg-sheen" aria-hidden="true"></div>
      </div>
    </div>
  `,
})
export class SgGlass implements OnDestroy {
  readonly heading = input.required<string>();
  readonly sub = input.required<string>();
  readonly tabs = input.required<readonly SgTab[]>();
  readonly tab = input(0);
  readonly app = input.required<string>();
  readonly badge = input.required<string>();
  readonly aria = input.required<string>();
  readonly steps = input.required<number>();
  readonly period = input(1100);
  readonly settled = input.required<number>();
  readonly active = input(true);

  /** Where the loop is. Null until it starts, which reads as the settled step. */
  private readonly step = signal<number | null>(null);
  readonly t = computed(() => this.step() ?? this.settled());

  /** Public only because the host binding reads it. */
  readonly onScreen = signal(false);
  protected readonly armed = signal(false);
  protected readonly entered = signal(false);

  /** In a narrow column a tab shows only the first word of its label. */
  protected readonly tabView = computed(() => this.tabs().map(([icon, label]) => {
    const cut = label.indexOf(' ');
    return { icon, label, first: cut < 0 ? label : label.slice(0, cut), rest: cut < 0 ? '' : label.slice(cut) };
  }));

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly ticker = inject(GlassTicker);
  private unregister: (() => void) | null = null;
  private wasActive: boolean | null = null;
  private frame = 0;

  constructor() {
    effect(() => {
      const active = this.active();
      untracked(() => {
        if (this.wasActive === false && active) this.replay();
        this.wasActive = active;
      });
    });
    if (!isPlatformBrowser(inject(PLATFORM_ID))) return;
    afterNextRender(() => this.start());
  }

  private start(): void {
    if (this.ticker.still) return;
    const el = this.host.nativeElement;
    const view = el.ownerDocument.defaultView;
    // Only a window still below the fold gets the layered entrance; one already
    // on screen is left exactly as the server rendered it.
    if (view && el.getBoundingClientRect().top >= view.innerHeight) this.armed.set(true);

    this.unregister = this.ticker.register({
      el,
      period: this.period(),
      tick: () => this.step.set((this.t() + 1) % this.steps()),
      visible: (on, first) => {
        this.onScreen.set(on);
        if (first) this.entered.set(true);
      },
    });
  }

  /** A tab panel coming back on screen: empty the screen, replay the entrance, run the loop again. */
  private replay(): void {
    if (!this.unregister) return;
    const view = this.host.nativeElement.ownerDocument.defaultView;
    if (!view) return;
    this.step.set(-1);
    this.armed.set(true);
    this.entered.set(false);
    if (this.frame) view.cancelAnimationFrame(this.frame);
    this.frame = view.requestAnimationFrame(() => {
      this.frame = 0;
      this.entered.set(true);
    });
  }

  ngOnDestroy(): void {
    this.unregister?.();
    if (this.frame) this.host.nativeElement.ownerDocument.defaultView?.cancelAnimationFrame(this.frame);
  }
}

/**
 * SgBtn — an app button drawn in a screen (not interactive).
 *
 *   <sg-btn kind="ghost" icon="wrench" [press]="t() === 4" [dim]="busy()">Prepare AVD</sg-btn>
 *
 * kind: 'pri' (default) | 'ghost' | 'danger' | 'teal'. Size variants are classes on the
 * element: sg-btn--xs, sg-btn--tall, sg-btn--block, sg-btn--launch, sg-btn--flex.
 */
@Component({
  selector: 'sg-btn',
  imports: [UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'sg-btn', '[class]': '"sg-btn--" + kind()', '[class.press]': 'press()', '[class.dim]': 'dim()' },
  template: `@if (icon(); as name) {<ui-icon [name]="name" [style.--s]="11" />}<ng-content />`,
})
export class SgBtn {
  readonly kind = input<'pri' | 'ghost' | 'danger' | 'teal'>('pri');
  readonly icon = input<string | null>(null);
  readonly press = input(false);
  readonly dim = input(false);
}

/**
 * SgFld — a labelled form field. An empty `value` shows the placeholder; a
 * value that changes is redrawn so it fades in.
 *
 *   <sg-fld label="Port" [value]="filled() ? port : ''" ph="No port selected" [caret]="true" [on]="t() === 7" />
 */
@Component({
  selector: 'sg-fld',
  imports: [UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'sg-fldw', '[class.sg-fldw--mono]': 'mono()' },
  template: `
    <div class="sg-lab">{{ label() }}</div>
    <div class="sg-fld" [class.on]="on()">
      @if (icon(); as name) {<ui-icon [name]="name" class="sg-cf" />}
      @if (value()) {
        @for (v of [value()]; track v) {<span class="sg-line">{{ v }}</span>}
      } @else {
        <span class="sg-ph">{{ ph() }}</span>
      }
      @if (caret()) {<ui-icon name="caret-down" class="sg-cf sg-fld-caret" [style.--s]="11" />}
    </div>
  `,
})
export class SgFld {
  readonly label = input.required<string>();
  readonly value = input('');
  readonly icon = input<string | null>(null);
  readonly ph = input('');
  readonly on = input(false);
  readonly caret = input(false);
  readonly mono = input(false);
}

/**
 * SgTogRow — an icon, a title with an optional line under it, and a toggle.
 *
 *   <sg-togrow icon="lock" heading="Offline PIN (VERIFY)" sub="Plaintext and enciphered PIN by ICC" [on]="s(0)" [style.--d]="300" />
 */
@Component({
  selector: 'sg-togrow',
  imports: [UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'sg-togrow sg-tile' },
  template: `
    <span class="sg-togrow-l">
      <ui-icon [name]="icon()" class="sg-cb" [style.--s]="15" />
      <span><span class="sg-togrow-t">{{ heading() }}</span>@if (sub()) {<span class="sg-sub">{{ sub() }}</span>}</span>
    </span>
    <i class="sg-tog" [class.on]="on()"></i>
  `,
})
export class SgTogRow {
  readonly icon = input.required<string>();
  readonly heading = input.required<string>();
  readonly sub = input<string | null>(null);
  readonly on = input(false);
}

/**
 * SgPane — a titled output pane (formatted / raw command and response).
 *
 *   <sg-pane class="sg-pane--fmt" heading="Formatted Command" tone="req" icon="arrow-up-right" [count]="12" [style.--d]="340">…lines…</sg-pane>
 */
@Component({
  selector: 'sg-pane',
  imports: [UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'sg-pane sg-tile' },
  template: `
    <div class="sg-pane-h" [class.req]="tone() === 'req'" [class.rsp]="tone() === 'rsp'">
      <span class="sg-pane-t"><ui-icon [name]="icon()" />{{ heading() }}</span>
      <span class="sg-pane-n">{{ count() }}<ui-icon name="caret-up" [style.--s]="10" /><ui-icon name="caret-left" [style.--s]="10" /></span>
    </div>
    <div class="sg-pane-b"><ng-content /></div>
  `,
})
export class SgPane {
  readonly heading = input.required<string>();
  readonly tone = input.required<'req' | 'rsp'>();
  readonly icon = input.required<string>();
  readonly count = input(0);
}
