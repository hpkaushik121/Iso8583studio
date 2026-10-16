import {
  ChangeDetectionStrategy, Component, ElementRef, OnDestroy, PLATFORM_ID, afterNextRender,
  computed, inject, input, signal,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { UiIcon } from '../../../ui';
import { GlassTicker } from '../glass/glass-ticker';

/**
 * Simulator glass — the window frame and the app widgets the HSM Simulator,
 * Host Simulator and HSM Command Console screens are drawn with. Each screen
 * stands in for a product screenshot: the real tab, rebuilt as DOM, running a
 * short scripted loop.
 *
 *   <app-sim-glass #g heading="HSM Simulator - Thales PayShield 10k" sub="Logs" [tabs]="tabs" [tab]="4"
 *                  aria="…" [steps]="9" [period]="1000" [settled]="6">
 *     … the tab's body, reading g.t() to decide what is on screen …
 *   </app-sim-glass>
 *
 * Sizing. The host is a size container and the screen is drawn 1000 design
 * units wide (460, in a single column, when the host is under 600px). Every
 * length in the stylesheet is a multiple of that unit (--u), so the screen
 * scales with its column in CSS alone and the prerendered page is already the
 * right size. Parts that only exist in one layout carry .hg-w (wide only) or
 * .hg-n (narrow only).
 *
 * Motion. `t` is the step of the loop, 0 … steps-1. The prerendered screen
 * sits on `settled`, the step where the screen is complete. In the browser
 * the loop advances while the screen is in view (one shared GlassTicker) and
 * stops when it leaves; under prefers-reduced-motion it never starts. Things
 * that come and go during the loop keep their room (.hg-off hides without
 * collapsing), so the screen's height does not change while it runs.
 *
 * Styles: styles/bundles/_sims-hsm.css (every class is hg-).
 */

/** A tab in a screen's tab bar: [Phosphor icon or null, label]. */
export type SimTab = readonly [icon: string | null, label: string];

/** Seeded hex, the same on the server and in the browser. */
export function hgHx(seed: string, n: number): string {
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

@Component({
  selector: 'app-sim-glass',
  imports: [UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'hg-stage', '[class.off]': '!onScreen()' },
  template: `
    <div class="hg-wrap">
      <div class="hg-bg" aria-hidden="true"><span></span><span></span><span></span></div>
      <div class="hg" role="img" [attr.aria-label]="aria()" [class.hg--armed]="armed()" [class.in]="entered()"
           [class.hg--live]="live()">
        <div class="hg-dots" aria-hidden="true"></div>
        <div class="hg-head hg-layer" [style.--hd]="0">
          <span class="hg-head-l">
            <span class="hg-lights" aria-hidden="true"><span></span><span></span><span></span></span>
            <ui-icon name="arrow-left" class="hg-cf" />
            <span class="hg-title">{{ heading() }}</span>
          </span>
          <span class="hg-head-r">
            <span class="hg-path hg-w">{{ app() }} › {{ sub() }}</span>
            <span class="hg-badge">{{ badge() }}</span>
          </span>
        </div>
        @if (tabs(); as list) {
          <div class="hg-tabs hg-layer" [style.--hd]="90">
            @for (item of list; track item[1]; let i = $index) {
              <span class="hg-tab" [class.on]="i === tab()">@if (item[0]; as icon) {<ui-icon class="hg-w" [name]="icon" />}<span class="hg-w">{{ item[1] }}</span><span class="hg-n">{{ first(item[1]) }}</span></span>
            }
          </div>
        }
        <div class="hg-body"><ng-content /></div>
        <div class="hg-sheen" aria-hidden="true"></div>
      </div>
    </div>
  `,
})
export class SimGlass implements OnDestroy {
  /** Window title. */
  readonly heading = input.required<string>();
  /** The tab or panel on show; the second half of the path label. */
  readonly sub = input.required<string>();
  readonly tabs = input<readonly SimTab[] | null>(null);
  /** Index of the active tab. */
  readonly tab = input(0);
  /** Description of the screen for assistive tech (the frame is role="img"). */
  readonly aria = input.required<string>();
  /** First half of the path label. */
  readonly app = input('hsm-simulator');
  readonly badge = input('HSM');
  /** Number of steps in the loop. */
  readonly steps = input.required<number>();
  /** Milliseconds per step. */
  readonly period = input(1100);
  /** The step the prerendered (and reduced-motion) screen rests on. */
  readonly settled = input.required<number>();

  private readonly step = signal<number | null>(null);
  /** Where the loop is: 0 … steps-1. */
  readonly t = computed(() => this.step() ?? this.settled());

  /** Public only because the host binding reads it. */
  readonly onScreen = signal(false);
  protected readonly armed = signal(false);
  protected readonly entered = signal(false);
  /** Lines blur in as they land — only from the loop's first restart, so nothing replays on load. */
  protected readonly live = signal(false);

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly ticker = inject(GlassTicker);
  private unregister: (() => void) | null = null;

  constructor() {
    if (!isPlatformBrowser(inject(PLATFORM_ID))) return;
    afterNextRender(() => this.start());
  }

  protected first(label: string): string {
    return label.split(' ')[0];
  }

  private start(): void {
    if (this.ticker.still) return;
    const el = this.host.nativeElement;
    const view = el.ownerDocument.defaultView;
    // Only a screen still below the fold gets the layered entrance; one already
    // on screen is left exactly as the server rendered it.
    if (view && el.getBoundingClientRect().top >= view.innerHeight) this.armed.set(true);

    this.unregister = this.ticker.register({
      el,
      period: this.period(),
      tick: () => {
        const steps = this.steps();
        const next = (this.t() + 1) % steps;
        // The last step of every script is the cleared screen.
        if (next === steps - 1) this.live.set(true);
        this.step.set(next);
      },
      visible: (on, first) => {
        this.onScreen.set(on);
        if (first) this.entered.set(true);
      },
    });
  }

  ngOnDestroy(): void {
    this.unregister?.();
  }
}

export type HgBtnKind = 'pri' | 'ghost' | 'danger' | 'teal' | 'warn' | 'rev';

/** A button of the app, drawn (not a control): `<span hgBtn kind="ghost" icon="x" [press]="t === 5">Clear</span>`. */
@Component({
  selector: '[hgBtn]',
  imports: [UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'hg-btn', '[class]': '"hg-btn--" + kind()', '[class.press]': 'press()', '[class.dim]': 'dim()' },
  template: `@if (icon(); as name) {<ui-icon [name]="name" />}<ng-content />`,
})
export class HgBtn {
  readonly kind = input<HgBtnKind>('pri');
  readonly icon = input<string | null>(null);
  readonly press = input(false);
  readonly dim = input(false);
}

/** A labelled text field: the value blurs in when it lands, `on` lights the box while it is being typed. */
@Component({
  selector: 'app-hg-fld',
  imports: [UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'hg-fldw' },
  template: `
    <div class="hg-lab">{{ label() }}</div>
    <div class="hg-fld" [class.on]="on()">
      @if (icon(); as name) { <ui-icon [name]="name" class="hg-cf" /> }
      @if (value()) { <span class="hg-line">{{ value() }}</span> } @else { <span class="hg-ph">{{ ph() }}</span> }
      @if (caret()) { <ui-icon name="caret-down" class="hg-cf hg-fld-caret" /> }
    </div>
  `,
})
export class HgFld {
  readonly label = input.required<string>();
  readonly value = input<string | null>(null);
  readonly icon = input<string | null>(null);
  /** Placeholder shown while there is no value. */
  readonly ph = input('');
  readonly on = input(false);
  readonly caret = input(false);
}

/** A choice tile: `<div hgTile icon="buildings" label="Thales" [on]="picked"></div>`. Selected, its icon becomes a tick. */
@Component({
  selector: '[hgTile]',
  imports: [UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'hg-tile', '[class.on]': 'on()', '[class.hov]': 'hov() && !on()' },
  template: `@if (icon(); as name) {<ui-icon [name]="on() ? 'check-circle' : name" />}<span>{{ label() }}</span>`,
})
export class HgTile {
  readonly icon = input<string | null>(null);
  readonly label = input.required<string>();
  readonly on = input(false);
  readonly hov = input(false);
}

/**
 * Mono lines that land one after another. `off` hides them without giving up
 * their room; lines from index `wide` on exist only in the wide layout.
 */
@Component({
  selector: 'app-hg-lines',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'hg-lines' },
  template: `@for (line of lines(); track $index) {<div class="hg-line" [class.hg-off]="off()" [class.hg-w]="$index >= wide()" [style.--ld]="$index * step()">{{ line || ' ' }}</div>}`,
})
export class HgLines {
  readonly lines = input.required<readonly string[]>();
  /** Milliseconds between lines. */
  readonly step = input(60);
  readonly off = input(false);
  readonly wide = input(Infinity);
}

/** A titled mono pane of the request / response split view, at a fixed height (`h` design units; `hn` when narrow). */
@Component({
  selector: 'app-hg-pane',
  imports: [UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'hg-pane hg-rise', '[style.--hd]': 'd()', '[style.--ph]': 'h()', '[style.--pn]': 'hn()' },
  template: `
    <div class="hg-pane-h" [class.req]="tone() === 'req'" [class.rsp]="tone() === 'rsp'">
      <span class="hg-pane-t"><ui-icon [name]="icon()" />{{ heading() }}</span>
      <span class="hg-pane-c">{{ count() }}<ui-icon name="caret-up" /><ui-icon name="caret-left" /></span>
    </div>
    <div class="hg-mono hg-pane-b"><ng-content /></div>
  `,
})
export class HgPane {
  readonly heading = input.required<string>();
  readonly tone = input<'req' | 'rsp'>('req');
  readonly icon = input.required<string>();
  readonly count = input<number | string>(0);
  readonly h = input.required<number>();
  readonly hn = input.required<number>();
  /** Entrance delay, ms. */
  readonly d = input(0);
}

export type HgStatTone = 'blue' | 'green' | 'grey' | 'teal' | 'red' | 'amber' | 'text' | 'ok';

/** A counter tile: icon, big number, label. */
@Component({
  selector: '[hgStat]',
  imports: [UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'hg-card hg-stat hg-rise', '[class]': '"hg-stat--" + tone()', '[style.--hd]': 'd()' },
  template: `<ui-icon [name]="icon()" /><b>{{ n() }}</b><span>{{ label() }}</span>`,
})
export class HgStat {
  readonly icon = input.required<string>();
  readonly tone = input<HgStatTone>('blue');
  readonly n = input.required<number | string>();
  readonly label = input.required<string>();
  readonly d = input(0);
}

/** A settings row: icon, title, optional sub-line, and a toggle at the right. */
@Component({
  selector: '[hgTogRow]',
  imports: [UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'hg-togrow hg-rise', '[style.--hd]': 'd()' },
  template: `
    <span class="hg-togrow-l"><ui-icon [name]="icon()" class="hg-cb" /><span><span class="hg-togrow-t">{{ heading() }}</span>@if (sub()) {<span class="hg-sub">{{ sub() }}</span>}</span></span>
    <i class="hg-tog" [class.on]="on()"></i>
  `,
})
export class HgTogRow {
  readonly icon = input.required<string>();
  readonly heading = input.required<string>();
  readonly sub = input<string | null>(null);
  readonly on = input(false);
  readonly d = input(0);
}

/**
 * A row of the command list: [code, name, category, tone letter, icon?].
 * Tone letters: n muted · b blue · g green · o amber · p purple · t teal.
 */
export type HgCmd = readonly [code: string, name: string, cat: string, tone: string, icon?: string];

/**
 * The command list down the left of the Host Commands, Secure Commands and
 * Console tabs: a blue header, optional search and category chips, then the
 * rows with `sel` highlighted. Rows with an icon use the Secure Commands
 * layout (icon, code and category on one line, the name under it).
 */
@Component({
  selector: 'app-hg-cmdlist',
  imports: [UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'hg-list hg-card hg-layer', '[class.dim]': 'dim()', '[style.--hd]': '160' },
  template: `
    <div class="hg-list-h"><div class="hg-list-t">{{ heading() }}</div><div class="hg-list-s">{{ sub() }}</div></div>
    @if (search()) {
      <div class="hg-list-search"><div class="hg-fld hg-fld--30"><ui-icon name="magnifying-glass" class="hg-cf" /><span class="hg-ph">Search commands…</span></div></div>
    }
    @if (chips(); as list) {
      <div class="hg-list-chips">@for (chip of list; track chip; let i = $index) {<span class="hg-chip hg-chip--sm" [class.on]="i === 0">{{ chip }}</span>}</div>
    }
    @for (row of items(); track row[0]; let i = $index) {
      <div class="hg-item hg-rise" [class.on]="row[0] === sel()" [style.--hd]="260 + i * 40">
        @if (row[4]; as icon) {
          <ui-icon [name]="icon" class="hg-item-ic" />
          <span class="hg-item-x"><b><span class="hg-item-code" [class]="'hg-c' + row[3]">{{ row[0] }}</span><span class="hg-tag" [class]="'hg-c' + row[3]">{{ row[2] }}</span></b><small class="hg-item-name">{{ row[1] }}</small></span>
        } @else {
          <span class="hg-code" [class]="'hg-c' + row[3]">{{ row[0] }}</span>
          <span class="hg-item-x"><b>{{ row[1] }}</b><small [class]="'hg-c' + row[3]">{{ row[2] }}</small></span>
        }
      </div>
    }
  `,
})
export class HgCmdList {
  readonly items = input.required<readonly HgCmd[]>();
  readonly sel = input<string | null>(null);
  readonly heading = input.required<string>();
  readonly sub = input.required<string>();
  readonly search = input(false);
  readonly chips = input<readonly string[] | null>(null);
  readonly dim = input(false);
}

/** Everything a screen component needs to import. */
export const SIM_GLASS = [SimGlass, HgBtn, HgFld, HgTile, HgLines, HgPane, HgStat, HgTogRow, HgCmdList, UiIcon] as const;
