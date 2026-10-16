import {
  ChangeDetectionStrategy, Component, ElementRef, OnDestroy, PLATFORM_ID, afterNextRender,
  computed, inject, input, signal,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { UiIcon } from '../../../ui';
import { HubStage, HubTool } from './glass-model';
import { GlassTicker } from './glass-ticker';

/** The studio's tool categories, as its sidebar lists them. */
const CATEGORIES: { icon: string; name: string; count: string }[] = [
  { icon: 'wifi-high', name: 'Payment Simulators', count: '9 tools' },
  { icon: 'credit-card', name: 'EMV & Card Tools', count: '12 tools' },
  { icon: 'lock-key', name: 'Cryptographic Tools', count: '7 tools' },
  { icon: 'key', name: 'Key Management', count: '10 tools' },
  { icon: 'keyboard', name: 'Payment Utilities', count: '21 tools' },
  { icon: 'swap', name: 'Data Converters', count: '6 tools' },
  { icon: 'seal-check', name: 'Card Validation', count: '4 tools' },
  { icon: 'gear', name: 'DUKPT Tools', count: '6 tools' },
  { icon: 'shield-check', name: 'MAC Tools', count: '5 tools' },
];

/**
 * Last row of the tool grid: leftover tiles share the row when they divide it
 * evenly, otherwise the final tile takes the remainder.
 */
function lastRowSpan(i: number, total: number, columns: number): number {
  const rest = total % columns;
  if (!rest || i < total - rest) return 1;
  if (columns % rest === 0) return columns / rest;
  return i === total - 1 ? columns - rest + 1 : 1;
}

/**
 * ToolHub — the studio's tool hub for one category, drawn to fill the hero
 * frame: the category sidebar, a header, the group's stages and its tools,
 * with a highlight that walks the tools in turn.
 *
 *   <app-guide-frame gdHeroVisual>
 *     <app-tool-hub heading="EMV & Card Tools" sub="Smart card, EMV and contactless payment tools"
 *                   [tools]="tools" [stages]="stages" [category]="1" search="Search EMV tools…"
 *                   badge="12 tools" label="In the order an EMV transaction reaches them"
 *                   note="Every intermediate value stays visible." aria="The EMV & Card Tools hub: …" />
 *   </app-guide-frame>
 *
 * Inputs
 *   heading, sub, search, badge, label, note   the hub's copy
 *   aria       accessible name of the drawing (it is role="img")
 *   tools      HubTool[]      the tiles, in order
 *   stages     HubStage[]     [code, name, sub] columns above the tiles; a tool's `stage` indexes them
 *   columns    number         tile columns (default 3; at most 3 in the narrow layout)
 *   category   number         index of the highlighted sidebar row (default 1)
 *   dense      boolean        name-only tiles, for big groups
 *   delay      number         ms before the entrance starts (default 2700 — after the frame has risen)
 *
 * The host fills its positioned parent (position:absolute; inset:0) and is a
 * size container: the drawing is 1160 design units wide (600, without the
 * sidebar, when the frame is under 560px) and scales in CSS alone.
 *
 * Prerendered with the third tool highlighted — the state reduced motion
 * keeps. In the browser the highlight advances every 1.7s while the hub is on
 * screen, driven by the shared GlassTicker.
 *
 * Styles: styles/bundles/tools.css (.th-*).
 */
@Component({
  selector: 'app-tool-hub',
  imports: [UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'th', role: 'img', '[attr.aria-label]': 'aria()', '[class.th--live]': 'live()' },
  template: `
    <div class="th-in">
      <div class="th-side ds-layer" [style.--d]="delay()">
        <div class="th-brand">
          <span class="th-logo"><ui-icon name="wifi-high" /></span>
          <span><span class="th-brand-name">ISO8583Studio</span><span class="th-brand-sub">Professional Payment Testing</span></span>
        </div>
        <div class="th-label th-cats-label">Tool Categories</div>
        <div class="th-cats">
          @for (c of categories; track c.name; let i = $index) {
            <div class="th-cat" [class.on]="i === category()">
              <ui-icon [name]="c.icon" />
              <span class="th-cat-text"><span class="th-cat-name">{{ c.name }}</span><span class="th-cat-n">{{ c.count }}</span></span>
            </div>
          }
        </div>
      </div>
      <div class="th-main">
        <div class="th-head ds-layer" [style.--d]="delay() + 80">
          <div class="th-head-l">
            <span class="th-logo th-logo--narrow"><ui-icon [name]="categories[category()].icon" /></span>
            <div class="th-titles"><div class="th-title">{{ heading() }}</div><div class="th-sub">{{ sub() }}</div></div>
          </div>
          <div class="th-head-r">
            <div class="th-search"><ui-icon name="magnifying-glass" /><span>{{ search() }}</span></div>
            <span class="th-badge">{{ badge() }}</span>
          </div>
        </div>
        <div class="th-stages" [style.--n]="stages().length">
          @for (s of stageView(); track s.code; let i = $index) {
            <div class="th-stage ds-tile" [class.on]="s.on" [style.--d]="delay() + 300 + i * 130">
              <div class="th-stage-top"><span class="th-stage-n">{{ s.code }}</span><span class="th-stage-count">{{ s.count }} {{ s.count === 1 ? 'tool' : 'tools' }}</span></div>
              <div class="th-stage-name">{{ s.name }}</div>
              <div class="th-stage-sub">{{ s.sub }}</div>
              <div class="th-track"><div class="th-bar" [style.--p]="s.count ? s.done / s.count : 0"></div></div>
            </div>
          }
        </div>
        <div class="th-panel ds-rise" [style.--d]="delay() + 640">
          <div class="th-panel-h ds-tile" [style.--d]="delay() + 700">
            <div><div class="th-panel-title">All tools</div><div class="th-panel-sub">{{ label() }}</div></div>
            <span class="th-label">hex-driven · audit log</span>
          </div>
          <div class="th-grid" [style.--gc]="columns()" [style.--gcn]="narrowColumns()">
            @for (tool of tools(); track tool.id; let i = $index) {
              <div class="th-tile ds-tile" [class.on]="at() === i" [style.--d]="delay() + 800 + i * 60"
                   [style.--span]="span(i, columns())" [style.--spann]="span(i, narrowColumns())">
                <div class="th-tile-top">
                  <ui-icon [name]="tool.icon" />
                  <span class="th-tile-name">{{ tool.name }}</span>
                  <span class="th-tile-n">{{ stages()[tool.stage][0] }}</span>
                </div>
                @if (!dense()) { <div class="th-tile-desc">{{ tool.desc }}</div> }
              </div>
            }
          </div>
          <div class="th-foot">
            <div class="th-note ds-tile" [style.--d]="delay() + 1450">
              <span class="th-note-mark">›</span>
              @for (tool of current(); track tool.id) {
                <span class="th-note-text"><span class="th-note-name">Opening {{ tool.name }}</span><span class="th-note-desc"> · {{ tool.desc }}</span></span>
              }
            </div>
            <div class="th-log ds-tile" [style.--d]="delay() + 1550">
              <div class="th-label th-label--teal">Activity log</div>
              <div class="th-log-text">{{ note() }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class ToolHub implements OnDestroy {
  readonly heading = input.required<string>();
  readonly sub = input.required<string>();
  readonly tools = input.required<readonly HubTool[]>();
  readonly stages = input.required<readonly HubStage[]>();
  readonly search = input('');
  readonly badge = input('');
  readonly label = input('');
  readonly note = input('');
  readonly aria = input('');
  readonly columns = input(3);
  readonly category = input(1);
  readonly dense = input(false);
  readonly delay = input(2700);

  protected readonly categories = CATEGORIES;

  /** Index of the highlighted tool. Starts on the third: the settled state. */
  protected readonly at = signal(2);
  /** Public only because the host binding reads it. */
  readonly live = signal(false);

  protected readonly narrowColumns = computed(() => Math.min(this.columns(), 3));

  /** A one-item list so the note is re-created — and re-animated — when the tool changes. */
  protected readonly current = computed(() => {
    const tool = this.tools()[this.at()];
    return tool ? [tool] : [];
  });

  protected readonly stageView = computed(() => {
    const tools = this.tools(), at = this.at(), active = tools[at]?.stage;
    return this.stages().map(([code, name, sub], i) => ({
      code, name, sub,
      on: active === i,
      count: tools.filter((tool) => tool.stage === i).length,
      done: tools.filter((tool, j) => tool.stage === i && j <= at).length,
    }));
  });

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly ticker = inject(GlassTicker);
  private unregister: (() => void) | null = null;

  constructor() {
    if (!isPlatformBrowser(inject(PLATFORM_ID))) return;
    afterNextRender(() => {
      this.unregister = this.ticker.register({
        el: this.host.nativeElement,
        period: 1700,
        // Hold the settled state until the entrance has played out.
        delay: this.delay() + 2300,
        tick: () => {
          this.live.set(true);
          this.at.set((this.at() + 1) % Math.max(this.tools().length, 1));
        },
      });
    });
  }

  protected span(i: number, columns: number): number {
    return lastRowSpan(i, this.tools().length, columns);
  }

  ngOnDestroy(): void {
    this.unregister?.();
  }
}
