import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EXTERNAL } from '../../../core/site-nav';
import { Crumb, UiIcon } from '../../../ui';
import {
  GuideBlock, GuideBlocks, GuideCard, GuideCards, GuideCta, GuideFeature, GuideFeatures, GuideFrame,
  GuideHero, GuideLayout, GuideRailItem, GuideRich, GuideSection, GuideTips,
} from '../guide';
import { GlassPanel } from './glass-panel';
import { GlassSpec, HubStage, HubTool } from './glass-model';
import { ToolHub } from './tool-hub';

/** Payload of a tool guide's custom block: which glass panel to draw, and its caption. */
export interface ToolPanelRef {
  /** Key into ToolGuideData.panels. */
  id: string;
  /** Text after the dash in the caption under the panel. */
  caption?: string;
}

export type ToolBlock = GuideBlock<ToolPanelRef>;

/** Shorthand for a glass-panel block in a section body. */
export const panel = (id: string, caption?: string): ToolBlock => ({ t: 'custom', data: { id, caption } });

/** "01 · Offline authentication" — the line above a section or on a card. */
export const stageTag = (stages: readonly HubStage[], stage: number): string =>
  `${stages[stage][0]} · ${stages[stage][1]}`;

/** [icon, name, desc] rows, as the data files write them, to GuideFeature objects. */
export const toFeatures = (rows: readonly [string, string, string][]): GuideFeature[] =>
  rows.map(([icon, name, desc]) => ({ icon, name, desc }));

export interface ToolSection {
  /** The section's id — what the rail links to and section_view reports. */
  id: string;
  title: string;
  /** Shorter label for the rail. */
  rail?: string;
  /** Rail icon. */
  icon: string;
  /** Mono line above the heading. */
  eyebrow?: string;
  /** Rich lede. */
  intro?: string;
  blocks: ToolBlock[];
}

/** Everything one tool guide page is rendered from. */
export interface ToolGuideData {
  /** The app's name for the group, used in panel paths and captions: 'emv-tools'. */
  slug: string;
  /** Last breadcrumb item. */
  crumb: string;
  /** Mono line above the h1. */
  meta: string;
  title: string;
  lede: string;
  hub: {
    title: string;
    sub: string;
    tools: HubTool[];
    stages: HubStage[];
    columns: number;
    /** Index of this group in the hub's sidebar. */
    category: number;
    dense?: boolean;
    search: string;
    badge: string;
    label: string;
    note: string;
    aria: string;
  };
  /** Every glass panel the sections refer to. */
  panels: Record<string, GlassSpec>;
  /** Pill for panels whose spec does not set one. */
  panelBadge?: string;
  /** Which spec field a panel's caption starts with. */
  captionFrom: 'sub' | 'title';
  intro: {
    eyebrow: string;
    /** Rich paragraphs. */
    paras: string[];
    features?: GuideFeature[];
    /** Rich paragraphs after the fact cards. */
    after?: string[];
  };
  allTools: { lede: string; cards: GuideCard[] };
  sections: ToolSection[];
  /** Rich. */
  tips: string[];
  cta: { heading: string; text: string };
}

/**
 * ToolGuide — a whole tool guide page, drawn from one ToolGuideData: hero with
 * the tool hub in its frame, the rail, Introduction (id `overview`), All tools
 * (`all-tools`), one section per tool with its glass panels, Tips (`tips`) and
 * the closing download panel (`download`).
 *
 *   @Component({ …, imports: [ToolGuide], template: `<app-tool-guide [guide]="guide" />` })
 *   export class DocsEmvToolsPage { protected readonly guide = EMV_TOOLS_GUIDE; }
 *
 * The five tool guides differ only in their data (src/app/pages/data/*.data.ts).
 * It is built entirely from the shared guide shell (../guide), so it doubles
 * as the worked example of how a guide page is assembled.
 *
 * Input: guide (ToolGuideData, required).
 */
@Component({
  selector: 'app-tool-guide',
  imports: [
    RouterLink, UiIcon, GuideHero, GuideFrame, GuideLayout, GuideSection, GuideBlocks, GuideCards,
    GuideFeatures, GuideTips, GuideCta, GuideRich, GlassPanel, ToolHub,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'tool-guide' },
  template: `
    @let g = guide();
    <app-guide-hero [heading]="g.title" [crumbs]="crumbs()" [meta]="g.meta" [lede]="g.lede" [dots]="true">
      <a class="btn btn--primary btn--glow" [href]="releases">Download Studio<ui-icon name="arrow-up-right" [size]="16" /></a>
      <a class="btn btn--secondary" [routerLink]="[]" fragment="all-tools">Browse the tools<ui-icon name="arrow-down" [size]="16" /></a>
      <app-guide-frame gdHeroVisual>
        <app-tool-hub [heading]="g.hub.title" [sub]="g.hub.sub" [tools]="g.hub.tools" [stages]="g.hub.stages"
                      [columns]="g.hub.columns" [category]="g.hub.category" [dense]="!!g.hub.dense"
                      [search]="g.hub.search" [badge]="g.hub.badge" [label]="g.hub.label" [note]="g.hub.note"
                      [aria]="g.hub.aria" />
      </app-guide-frame>
    </app-guide-hero>

    <app-guide-layout [rail]="rail()" badge="Tool reference">
      <app-guide-section anchor="overview" heading="Introduction" [eyebrow]="g.intro.eyebrow" eyebrowTone="faint" [threshold]="0.15">
        @for (para of g.intro.paras; track $index) {
          <p class="gd-lede ds-hold" [style.--d]="220 + $index * 120" [gdRich]="para"></p>
        }
        @if (g.intro.features; as features) { <app-guide-features [items]="features" /> }
        @for (para of g.intro.after ?? []; track $index) { <p class="gd-p" [gdRich]="para"></p> }
      </app-guide-section>

      <app-guide-section anchor="all-tools" heading="All tools" [intro]="g.allTools.lede" [threshold]="0.1">
        <app-guide-cards [items]="g.allTools.cards" />
      </app-guide-section>

      @for (section of g.sections; track section.id) {
        <app-guide-section [anchor]="section.id" [heading]="section.title" [eyebrow]="section.eyebrow ?? null"
                           [intro]="section.intro ?? null">
          <app-guide-blocks [blocks]="section.blocks">
            <ng-template let-ref>
              <div class="gp-figure">
                <app-glass-panel [spec]="g.panels[ref.id]" [app]="g.slug" [badge]="g.panelBadge ?? 'EMV'" />
                <p class="gd-caption">{{ caption(ref) }}</p>
              </div>
            </ng-template>
          </app-guide-blocks>
        </app-guide-section>
      }

      <app-guide-section anchor="tips" heading="Tips" [threshold]="0.2">
        <app-guide-tips [items]="g.tips" />
      </app-guide-section>

      <app-guide-cta [heading]="g.cta.heading" [text]="g.cta.text" />
    </app-guide-layout>
  `,
})
export class ToolGuide {
  readonly guide = input.required<ToolGuideData>();

  protected readonly releases = EXTERNAL.releases;

  protected readonly crumbs = computed<Crumb[]>(() => [
    { label: 'Home', link: '/' },
    { label: 'Documentation', link: '/docs' },
    { label: this.guide().crumb },
  ]);

  protected readonly rail = computed<GuideRailItem[]>(() => [
    { id: 'overview', label: 'Introduction', icon: 'book-open' },
    { id: 'all-tools', label: 'All tools', icon: 'squares-four' },
    ...this.guide().sections.map((s) => ({ id: s.id, label: s.rail || s.title, icon: s.icon })),
    { id: 'tips', label: 'Tips', icon: 'lightbulb' },
    { id: 'download', label: 'Try it', icon: 'download-simple' },
  ]);

  protected caption(ref: ToolPanelRef): string {
    const guide = this.guide();
    const spec = guide.panels[ref.id];
    const lead = guide.captionFrom === 'sub' ? spec.sub : spec.title;
    return `${guide.slug} · ${lead}${ref.caption ? ' — ' + ref.caption : ''}`;
  }
}
