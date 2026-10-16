import {
  ChangeDetectionStrategy, Component, ElementRef, OnDestroy, PLATFORM_ID, afterNextRender,
  computed, inject, input, signal,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { UiIcon } from '../../../ui';
import { GlassField, GlassSpec } from './glass-model';
import { GlassTicker } from './glass-ticker';

interface LogLine {
  key: number;
  /** in · run · out */
  k: 'in' | 'run' | 'out';
  tag: string;
  stamp: string;
  label?: string;
  text: string;
}

interface BitGrid {
  start: number;
  name: string;
  count: number;
  bits: { n: number; order: number }[];
}

const stamp = (i: number): string => '00:' + String(Math.min(59, Math.round(i * 1.3))).padStart(2, '0');

/**
 * GlassPanel — a tool screen rebuilt as a glass panel: the form, its tabs and
 * the activity log beside it. It stands in for a product screenshot.
 *
 *   <app-glass-panel [spec]="panels['sda-retrieve']" app="emv-tools" badge="EMV" />
 *
 * Inputs
 *   spec    GlassSpec, required   the screen to draw (see glass-model.ts)
 *   app     string                path label for specs that do not set their own (default 'emv-tools')
 *   badge   string                pill for specs that do not set their own (default 'EMV')
 *
 * Sizing. The host is a size container; the panel is drawn 860 design units
 * wide (440, in a single column, when the host is under 600px) and every
 * length is a multiple of that unit, so it scales with its column in CSS
 * alone. The log reserves the height of its fullest state, so the panel's
 * height never changes while it loops.
 *
 * Motion. The prerendered panel is settled: every field filled, the results
 * in the log. In the browser the loop starts when the panel scrolls into
 * view — clear, fill field by field, press the button, log the results, hold —
 * and pauses when it leaves. One shared GlassTicker drives every panel on the
 * page. Under prefers-reduced-motion the panel stays settled.
 *
 * Styles: styles/bundles/tools.css (.gp-*).
 */
@Component({
  selector: 'app-glass-panel',
  imports: [UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'gp-stage', '[class.off]': '!onScreen()' },
  template: `
    <div class="gp-wrap">
      <div class="gp-bg" aria-hidden="true"><span></span><span></span><span></span></div>
      <div class="gp" role="img" [attr.aria-label]="label()" [class.gp--armed]="armed()" [class.in]="entered()"
           [class.gp--live]="live()" [style.--gp-n]="reserve().wide" [style.--gp-nn]="reserve().narrow">
        <div class="gp-dots" aria-hidden="true"></div>
        <div class="gp-head gp-layer" [style.--d]="0">
          <span class="gp-head-l">
            <span class="gp-lights" aria-hidden="true"><span></span><span></span><span></span></span>
            <span class="gp-title">{{ spec().title }}</span>
          </span>
          <span class="gp-head-r">
            <span class="gp-path">{{ spec().app || app() }} › {{ spec().sub }}</span>
            <span class="gp-badge">{{ spec().badge || badge() }}</span>
          </span>
        </div>
        @if (spec().tabs; as tabs) {
          <div class="gp-tabs gp-layer" [style.--d]="110">
            @for (tab of tabs; track tab; let i = $index) {
              <span class="gp-tab" [class.on]="i === activeTab()" [class.done]="i < activeTab()">@if (i < activeTab()) {<ui-icon name="check" />}{{ tab }}</span>
            }
          </div>
        }
        <div class="gp-body">
          <div class="gp-main">
            <div class="gp-form">
              @for (f of spec().fields; track $index; let i = $index) {
                <div class="gp-field gp-tile" [class.on]="filling() && i === t()" [class.ok]="shown(i) && !clearing() && !(filling() && i === t())"
                     [class.gp-field--half]="f.w <= 3" [style.--w]="f.w" [style.--d]="240 + i * 70">
                  <div class="gp-lab"><b>{{ f.l }}</b>@if (f.tag) {<span class="gp-tag">{{ f.tag }}</span>}@if (f.opt) {<span class="gp-opt">optional</span>}</div>
                  <div class="gp-box" [class.area]="f.kind === 'area'">
                    @if (f.kind === 'check') {
                      <span class="gp-check" [class.on]="shown(i)" aria-hidden="true">@if (shown(i)) {<ui-icon name="check" />}</span>
                    }
                    @if (shown(i)) {
                      <span class="gp-val" [class.out]="clearing()">{{ f.v }}</span>
                    } @else {
                      <span class="gp-ph">{{ placeholder(f) }}</span>
                    }
                    @if (f.kind === 'sel') {
                      <ui-icon name="caret-down" />
                    } @else if (f.kind !== 'check') {
                      <span class="gp-count">{{ shown(i) && !clearing() ? f.n : 0 }}</span>
                    }
                  </div>
                </div>
              }
            </div>
            @if (grids(); as boxes) {
              <div class="gp-grids gp-tile" [style.--d]="240 + count() * 70 + 20">
                @for (g of boxes; track g.start) {
                  <div class="gp-grid-box">
                    <div class="gp-grid-h"><span>{{ g.name }}</span><span>{{ gridOn() ? g.count + ' set' : '—' }}</span></div>
                    <div class="gp-grid">
                      @for (bit of g.bits; track bit.n) {
                        <span class="gp-bit" [class.on]="gridOn() && bit.order >= 0" [style.--o]="bit.order >= 0 ? bit.order : null">{{ bit.n }}</span>
                      }
                    </div>
                  </div>
                }
              </div>
            }
            @if (spec().banner; as banner) {
              <div class="gp-banner" [class.warn]="banner.tone === 'warn'" [class.show]="t() >= count() && !clearing()"><ui-icon name="warning" />{{ banner.text }}</div>
            }
            <div class="gp-foot gp-tile" [style.--d]="240 + count() * 70 + 60">
              <span class="gp-btns">
                <span class="gp-btn" [class.press]="t() === count()"><ui-icon [name]="spec().icon || 'play'" />{{ spec().button }}</span>
                @if (spec().alt) { <span class="gp-btn ghost">{{ spec().alt }}</span> }
              </span>
              <span class="gp-hint">{{ spec().hint || 'live character count · odd-length hex flagged' }}</span>
            </div>
          </div>
          <div class="gp-log gp-layer" [style.--d]="420">
            <div class="gp-log-h"><span>Activity log</span><span>{{ lines().length ? lines().length + ' entries' : 'idle' }}</span></div>
            @if (lines().length) {
              <div class="gp-lines" [class.clear]="clearing()">
                @for (line of view(); track line.key) {
                  <div class="gp-line" [class.in]="line.k === 'in'" [class.run]="line.k === 'run'" [class.out]="line.k === 'out'"><span class="gp-line-s">{{ line.stamp }}</span><span class="gp-line-k">{{ line.tag }}</span><span class="gp-line-t">@if (line.label) {<b>{{ line.label }} </b>}{{ line.text }}</span></div>
                }
              </div>
            } @else {
              <div class="gp-idle">› session opened · {{ spec().sub.toLowerCase() }}<br>› waiting for input<span class="gp-caret" aria-hidden="true"></span></div>
            }
          </div>
        </div>
        <div class="gp-sheen" aria-hidden="true"></div>
      </div>
    </div>
  `,
})
export class GlassPanel implements OnDestroy {
  readonly spec = input.required<GlassSpec>();
  readonly app = input('emv-tools');
  readonly badge = input('EMV');

  /** Number of fields. The loop has count + 4 steps: fill each, run, results, hold, clear. */
  protected readonly count = computed(() => this.spec().fields.length);
  protected readonly activeTab = computed(() => this.spec().tab ?? 0);
  protected readonly label = computed(() =>
    `${this.spec().title} — ${this.spec().sub} form beside its activity log`);

  /** Where the loop is. Null until it starts, which reads as the settled state (count + 2). */
  private readonly step = signal<number | null>(null);
  protected readonly t = computed(() => this.step() ?? this.count() + 2);
  protected readonly filling = computed(() => this.t() >= 0 && this.t() < this.count());
  protected readonly clearing = computed(() => this.t() === this.count() + 3);
  protected readonly gridOn = computed(() => this.t() >= 0 && !this.clearing());

  /** Public only because the host binding reads it. */
  readonly onScreen = signal(false);
  protected readonly armed = signal(false);
  protected readonly entered = signal(false);
  protected readonly live = signal(false);

  /** Every line the log holds at step t. */
  protected readonly lines = computed<LogLine[]>(() => {
    const spec = this.spec(), t = this.t(), n = this.count();
    const lines: LogLine[] = [];
    if (t < 0) return lines;
    for (let i = 0; i <= Math.min(t, n - 1); i++) {
      const f = spec.fields[i];
      const value = f.kind === 'sel' || f.kind === 'check' ? f.v : `${f.n} ${f.kind === 'hex' ? 'hex' : 'chars'}`;
      lines.push({ key: lines.length, k: 'in', tag: 'IN', stamp: stamp(i), text: `${f.l} · ${value}` });
    }
    if (t >= n) lines.push({ key: lines.length, k: 'run', tag: 'RUN', stamp: stamp(n), text: spec.button });
    if (t >= n + 1) {
      spec.result.forEach(([label, text], j) =>
        lines.push({ key: lines.length, k: 'out', tag: 'OUT', stamp: stamp(n + 1 + j), label, text }));
    }
    return lines;
  });

  /** The tail of the log that fits: seven lines (CSS shows the last four in the narrow layout). */
  protected readonly view = computed(() => this.lines().slice(-7));

  /** Lines to reserve room for, so the panel is its final height from the first paint. */
  protected readonly reserve = computed(() => {
    const total = this.count() + 1 + this.spec().result.length;
    return { wide: Math.min(7, total), narrow: Math.min(4, total) };
  });

  protected readonly grids = computed<BitGrid[] | null>(() => {
    const grid = this.spec().grid;
    if (!grid) return null;
    return [[1, 'Primary bitmap · 1–64'], [65, 'Secondary bitmap · 65–128']].map(([start, name]) => {
      const from = start as number;
      const bits = Array.from({ length: 64 }, (_, j) => ({ n: from + j, order: grid.indexOf(from + j) }));
      return { start: from, name: name as string, count: bits.filter((b) => b.order >= 0).length, bits };
    });
  });

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly ticker = inject(GlassTicker);
  private unregister: (() => void) | null = null;

  constructor() {
    if (!isPlatformBrowser(inject(PLATFORM_ID))) return;
    afterNextRender(() => this.start());
  }

  protected shown(i: number): boolean {
    return this.t() >= 0 && i <= this.t();
  }

  protected placeholder(f: GlassField): string {
    return f.kind === 'sel' ? 'Select…' : f.kind === 'hex' ? 'hex' : '';
  }

  private start(): void {
    if (this.ticker.still) return;
    const el = this.host.nativeElement;
    const view = el.ownerDocument.defaultView;
    // Only a panel still below the fold gets the layered entrance; one already
    // on screen is left exactly as the server rendered it.
    if (view && el.getBoundingClientRect().top >= view.innerHeight) this.armed.set(true);

    const steps = this.count() + 4;
    this.unregister = this.ticker.register({
      el,
      period: Math.max(760, Math.min(1300, 11000 / steps)),
      tick: () => {
        this.live.set(true);
        this.step.set((this.t() + 1) % steps);
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
