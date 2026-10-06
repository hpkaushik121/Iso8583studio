import { NgTemplateOutlet, isPlatformBrowser } from '@angular/common';
import {
  ChangeDetectionStrategy, Component, ElementRef, OnDestroy, PLATFORM_ID, TemplateRef, afterNextRender,
  computed, contentChild, inject, input, signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { EXTERNAL } from '../../../core/site-nav';
import { Crumb, UiBadge, UiIcon } from '../../../ui';
import {
  GuideBlock, GuideBlocks, GuideCard, GuideCards, GuideCta, GuideHero, GuideLayout, GuideNote, GuideNoteTone,
  GuideRailItem, GuideRich, GuideSection, GuideTips,
} from '../guide';
import { HeroVideo } from '../hero-video';
import { GlassTicker } from '../glass/glass-ticker';

/** One screen of a screen picker: its chip and the caption under it. */
export interface SimShotItem {
  /** Key of the screen (what the projected screen template receives). */
  id: string;
  /** Chip icon. */
  icon: string;
  label: string;
  /** Chip label when the column is narrow. */
  short?: string;
  caption: string;
}

/** The page-specific blocks of a simulator guide (GuideBlock `custom` payloads). */
export type SimCustom =
  /** One glass screen with a caption: "<slug> · <sub> — <caption>". */
  | { k: 'shot'; id: string; sub: string; caption?: string }
  /** A chip row over several glass screens, one shown at a time. */
  | { k: 'shots'; label: string; items: SimShotItem[] }
  /** Definition rows: [icon, term, rich description]. */
  | { k: 'def'; items: [string, string, string][] }
  /** A table whose "Live" / "Pending" cells are drawn as badges; other cells are rich. */
  | { k: 'states'; head: string[]; rows: string[][]; cols: string }
  /** A note that carries a list: rich lead, [term, rich] bullets, rich closing paragraph. */
  | { k: 'listnote'; tone: GuideNoteTone; icon: string; title: string; lead: string; items: [string, string][]; after: string }
  /** Numbered tips (rich). */
  | { k: 'tips'; items: string[] };

export type SimBlock = GuideBlock<SimCustom>;

/** Shorthands for the custom blocks, as the data files write them. */
export const shot = (id: string, sub: string, caption?: string): SimBlock => ({ t: 'custom', data: { k: 'shot', id, sub, caption } });
export const shots = (label: string, items: SimShotItem[]): SimBlock => ({ t: 'custom', data: { k: 'shots', label, items } });
export const def = (items: [string, string, string][]): SimBlock => ({ t: 'custom', data: { k: 'def', items } });

export interface SimSection {
  /** The section's id — what the rail links to and section_view reports. */
  id: string;
  title: string;
  /** Shorter label for the rail. */
  rail?: string;
  /** Rail icon. */
  icon: string;
  /** Mono line above the heading ("01 · Configure"). */
  eyebrow?: string;
  /** Rich lede. */
  intro?: string;
  blocks: SimBlock[];
}

/** Everything one simulator guide page is rendered from. */
export interface SimGuideData {
  /** The app's name for the simulator, used in captions: 'pos-simulator'. */
  slug: string;
  /** The h1 and the last breadcrumb item. */
  title: string;
  /** Mono line above the h1. */
  meta: string;
  lede: string;
  /** Hero background: clip and poster names under /media, and how the clip is laid out. */
  video: { clip: string; poster: string; layout?: 'cover' | 'right' };
  /** The Overview section (id `overview`). */
  intro: {
    eyebrow: string;
    /** Rich paragraphs. */
    paras: string[];
    shot: { id: string; sub: string; caption: string };
    /** [icon, name, rich description] */
    features: [string, string, string][];
  };
  /** The Configuration tabs section (id `tabs`): cards that jump to the tab sections. */
  tabs: { lede: string; cards: GuideCard[] };
  sections: SimSection[];
  cta: { heading: string; text: string };
}

/**
 * SgShots — a chip row that picks one of several glass screens. Every screen
 * is in the page; the ones not picked are `hidden`. While the block is on
 * screen it moves to the next chip every seven seconds, until the reader picks
 * one (one shared GlassTicker; nothing moves under prefers-reduced-motion).
 *
 *   <sg-shots label="Hardware groups" slug="pos-simulator" [items]="items" [screen]="screenTemplate" />
 *
 * `screen` is a template that draws a screen: it receives the item id as its
 * implicit value and `active` (whether that screen is the one shown).
 */
@Component({
  selector: 'sg-shots',
  imports: [NgTemplateOutlet, UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'sg-shots', '[class.sg-shots--live]': 'moved()' },
  template: `
    <div class="sg-chips" role="tablist" [attr.aria-label]="label()">
      @for (item of items(); track item.id; let k = $index) {
        <button type="button" class="sg-tabchip" role="tab" [class.on]="k === cur()" [id]="uid() + '-tab-' + item.id"
                [attr.aria-selected]="k === cur()" [attr.aria-controls]="uid() + '-panel-' + item.id" (click)="pick(k)">
          <ui-icon [name]="item.icon" [size]="14" /><span class="sg-tabchip-full">{{ item.label }}</span><span class="sg-tabchip-short">{{ item.short || item.label }}</span>
        </button>
      }
    </div>
    @for (item of items(); track item.id; let k = $index) {
      <div class="sg-shot" role="tabpanel" [id]="uid() + '-panel-' + item.id" [attr.aria-labelledby]="uid() + '-tab-' + item.id"
           [hidden]="k !== cur()">
        <ng-container [ngTemplateOutlet]="screen()" [ngTemplateOutletContext]="{ $implicit: item.id, active: k === cur() }" />
        <p class="gd-caption">{{ slug() }} · {{ item.label }} — {{ item.caption }}</p>
      </div>
    }
  `,
})
export class SgShots implements OnDestroy {
  readonly items = input.required<readonly SimShotItem[]>();
  readonly label = input.required<string>();
  readonly slug = input.required<string>();
  readonly screen = input.required<TemplateRef<unknown>>();

  protected readonly cur = signal(0);
  /** True once the picked screen has changed, so captions only fade in on a switch. */
  protected readonly moved = signal(false);
  protected readonly uid = computed(() => 'sg-' + this.slug());

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly ticker = inject(GlassTicker);
  private unregister: (() => void) | null = null;

  constructor() {
    if (!isPlatformBrowser(inject(PLATFORM_ID))) return;
    afterNextRender(() => {
      this.unregister = this.ticker.register({
        el: this.host.nativeElement,
        period: 7000,
        tick: () => this.show((this.cur() + 1) % this.items().length),
      });
    });
  }

  /** The reader takes over: show the screen and stop advancing. */
  protected pick(index: number): void {
    this.unregister?.();
    this.unregister = null;
    this.show(index);
  }

  private show(index: number): void {
    if (index === this.cur()) return;
    this.moved.set(true);
    this.cur.set(index);
  }

  ngOnDestroy(): void {
    this.unregister?.();
  }
}

/**
 * SimGuide — a whole simulator guide page drawn from one SimGuideData: the
 * hero over its looping product video, the rail, Overview (`overview`),
 * Configuration tabs (`tabs`), one section per entry of `sections`, and the
 * closing download panel (`download`).
 *
 * The screens themselves are the page's: project one template that draws a
 * screen by id.
 *
 *   <sg-sim-guide [guide]="guide">
 *     <ng-template let-id let-active="active"><pg-screen [screen]="id" [active]="active" /></ng-template>
 *   </sg-sim-guide>
 *
 * Built on the shared guide shell (../guide); styles for the screens and the
 * blocks added here are in styles/bundles/_sims-pos.css.
 */
@Component({
  selector: 'sg-sim-guide',
  imports: [
    NgTemplateOutlet, RouterLink, UiBadge, UiIcon, GuideHero, GuideLayout, GuideSection, GuideBlocks, GuideCards,
    GuideCta, GuideNote, GuideRich, GuideTips, HeroVideo, SgShots,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'sg-guide' },
  template: `
    @let g = guide();
    <app-guide-hero align="left" [heading]="g.title" [crumbs]="crumbs()" [meta]="g.meta" [lede]="g.lede" status="Beta">
      <app-hero-video gdHeroBg [clips]="[g.video.clip]" [poster]="g.video.poster" [layout]="g.video.layout ?? 'cover'" />
      <a class="btn btn--primary btn--glow" [href]="releases">Download Studio<ui-icon name="arrow-up-right" [size]="16" /></a>
      <a class="btn btn--secondary" [routerLink]="[]" fragment="tabs">Browse the tabs<ui-icon name="arrow-down" [size]="16" /></a>
    </app-guide-hero>

    <app-guide-layout [rail]="rail()" badge="Simulator guide" status="Beta">
      <app-guide-section anchor="overview" heading="Overview" [eyebrow]="g.intro.eyebrow" eyebrowTone="faint" [threshold]="0.15">
        @for (para of g.intro.paras; track $index) {
          <p class="gd-lede ds-hold" [style.--d]="220 + $index * 80" [gdRich]="para"></p>
        }
        <div class="sg-figure">
          <ng-container [ngTemplateOutlet]="screen()" [ngTemplateOutletContext]="{ $implicit: g.intro.shot.id, active: true }" />
          <p class="gd-caption">{{ g.slug }} · {{ g.intro.shot.sub }} — {{ g.intro.shot.caption }}</p>
        </div>
        <div class="gd-features">
          @for (f of g.intro.features; track f[1]; let i = $index) {
            <div class="gd-feature ds-item" [style.--d]="320 + floor(i / 2) * 200 + (i % 2) * 90">
              <span class="gd-feature-icon"><ui-icon [name]="f[0]" [size]="17" /></span>
              <span class="gd-feature-text"><strong>{{ f[1] }}</strong><span [gdRich]="f[2]"></span></span>
            </div>
          }
        </div>
      </app-guide-section>

      <app-guide-section anchor="tabs" heading="Configuration tabs" [intro]="g.tabs.lede" [threshold]="0.1">
        <app-guide-cards [items]="g.tabs.cards" more="Read the section" />
      </app-guide-section>

      @for (section of g.sections; track section.id) {
        <app-guide-section [anchor]="section.id" [heading]="section.title" [eyebrow]="section.eyebrow ?? null"
                           [intro]="section.intro ?? null">
          <app-guide-blocks [blocks]="section.blocks">
            <ng-template let-b>
              @switch (b.k) {
                @case ('shot') {
                  <div class="sg-figure">
                    <ng-container [ngTemplateOutlet]="screen()" [ngTemplateOutletContext]="{ $implicit: b.id, active: true }" />
                    <p class="gd-caption">{{ g.slug }} · {{ b.sub }}{{ b.caption ? ' — ' + b.caption : '' }}</p>
                  </div>
                }
                @case ('shots') {
                  <sg-shots [label]="b.label" [slug]="g.slug" [items]="b.items" [screen]="screen()" />
                }
                @case ('def') {
                  <div class="sg-def">
                    @for (row of b.items; track row[1]) {
                      <div class="sg-def-row">
                        <span class="gd-feature-icon"><ui-icon [name]="row[0]" [size]="17" /></span>
                        <strong class="sg-def-term">{{ row[1] }}</strong>
                        <span class="sg-def-desc" [gdRich]="row[2]"></span>
                      </div>
                    }
                  </div>
                }
                @case ('states') {
                  <div class="gd-table-wrap">
                    <table class="gd-table">
                      <colgroup>@for (w of widths(b.cols); track $index) { <col [class.gd-col-fixed]="w !== null" [style.--gd-w]="w"> }</colgroup>
                      <thead><tr>@for (h of b.head; track $index) { <th scope="col">{{ h }}</th> }</tr></thead>
                      <tbody>
                        @for (row of b.rows; track $index) {
                          <tr>
                            @for (cell of row; track $index) {
                              @if (cell === 'Live') {
                                <td [attr.data-label]="b.head[$index]"><ui-badge tone="teal" [dot]="true">Live</ui-badge></td>
                              } @else if (cell === 'Pending') {
                                <td [attr.data-label]="b.head[$index]"><ui-badge tone="neutral">Pending</ui-badge></td>
                              } @else {
                                <td [attr.data-label]="b.head[$index]" [gdRich]="cell"></td>
                              }
                            }
                          </tr>
                        }
                      </tbody>
                    </table>
                  </div>
                }
                @case ('listnote') {
                  <app-guide-note [tone]="b.tone" [icon]="b.icon" [heading]="b.title">
                    <span [gdRich]="b.lead"></span>
                    <ul class="gd-bullets sg-note-list">
                      @for (item of b.items; track item[0]) { <li><strong>{{ item[0] }}</strong> — <span [gdRich]="item[1]"></span></li> }
                    </ul>
                    <span class="sg-note-after" [gdRich]="b.after"></span>
                  </app-guide-note>
                }
                @case ('tips') { <app-guide-tips [items]="b.items" /> }
              }
            </ng-template>
          </app-guide-blocks>
        </app-guide-section>
      }

      <app-guide-cta [heading]="g.cta.heading" [text]="g.cta.text" />
    </app-guide-layout>
  `,
})
export class SimGuide {
  readonly guide = input.required<SimGuideData>();

  /** Draws a screen: implicit value = screen id, `active` = whether it is the one on show. */
  protected readonly screen = contentChild.required(TemplateRef);

  protected readonly releases = EXTERNAL.releases;
  protected readonly floor = Math.floor;

  protected readonly crumbs = computed<Crumb[]>(() => [
    { label: 'Home', link: '/' },
    { label: 'Documentation', link: '/docs' },
    { label: this.guide().title },
  ]);

  protected readonly rail = computed<GuideRailItem[]>(() => [
    { id: 'overview', label: 'Overview', icon: 'book-open' },
    { id: 'tabs', label: 'Configuration tabs', icon: 'squares-four' },
    ...this.guide().sections.map((s) => ({ id: s.id, label: s.rail || s.title, icon: s.icon })),
    { id: 'download', label: 'Try it', icon: 'download-simple' },
  ]);

  /** '130px 96px minmax(0,1fr)' → ['130px', '96px', null]: only px widths are applied. */
  protected widths(cols: string): (string | null)[] {
    return cols.split(' ').filter(Boolean).map((w) => (/^[\d.]+px$/.test(w) ? w : null));
  }
}
