import {
  ChangeDetectionStrategy, Component, DOCUMENT, OnDestroy, PLATFORM_ID, afterNextRender,
  inject, input, signal,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { BadgeTone, UiBadge, UiIcon } from '../../../ui';

/** One entry of the section rail. `id` is the id of a section on the page. */
export interface GuideRailItem {
  id: string;
  label: string;
  /** Phosphor icon name. Must appear as a quoted literal somewhere under src/app. */
  icon: string;
}

/**
 * GuideLayout — the body of a guide page: a sticky "On this page" rail on the
 * left with scroll-spy, and the content column beside it.
 *
 *   <app-guide-layout [rail]="rail" badge="Simulator guide">
 *     <app-guide-section anchor="overview" heading="Overview"> … </app-guide-section>
 *     <app-guide-section anchor="commands" heading="Runtime tabs"> … </app-guide-section>
 *     <app-guide-cta anchor="download" heading="Try it on your own transactions" …/>
 *   </app-guide-layout>
 *
 * Inputs
 *   rail        GuideRailItem[], required   the sections to list, in page order. Every id must
 *                                           exist in the projected content (a <section>, or the
 *                                           wrapper app-guide-cta renders).
 *   badge       string | null               pill above the rail ("Tool reference", "Simulator guide")
 *   badgeTone   BadgeTone                   default 'teal'
 *   status      string | null               release pill beside the badge ('Beta')
 *
 * Slot: (default) — the page content; it lands in <div class="gd-main">.
 *
 * Behaviour
 *   · Rail links are real anchors ([routerLink]="[]" + fragment), so they render as
 *     /this-page#id and the router does the scrolling.
 *   · Above 1040px the rail is a sticky column. At ≤1040px it is replaced — in CSS only, both
 *     are in the prerendered markup — by a closed <details class="gd-toc"> "On this page" block
 *     above the content.
 *   · The first item is marked active in the prerender; an IntersectionObserver started in the
 *     browser then follows the section nearest the top of the viewport.
 *   · .gd-main is a size container named `gd-main`. Shell blocks switch their column counts on
 *     its width (<640 one column, <900 two), so write
 *     `@container gd-main (max-width: 639px) { … }` in your own bundle for the same breakpoints.
 */
@Component({
  selector: 'app-guide-layout',
  imports: [RouterLink, UiBadge, UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'gd-layout' },
  template: `
    <div class="gd-wrap">
      <details class="gd-toc">
        <summary>On this page</summary>
        <nav class="gd-rail" aria-label="On this page">
          @for (item of rail(); track item.id) {
            <a [routerLink]="[]" [fragment]="item.id"><ui-icon [name]="item.icon" [size]="15" /><span>{{ item.label }}</span></a>
          }
        </nav>
      </details>
      <div class="gd-rail-col">
        <nav class="gd-rail" aria-label="On this page">
          @if (badge()) {
            <div class="gd-rail-badge"><ui-badge [tone]="badgeTone()" [dot]="true">{{ badge() }}</ui-badge>@if (status()) {<ui-badge tone="blue">{{ status() }}</ui-badge>}</div>
          }
          <div class="gd-rail-label">On this page</div>
          @for (item of rail(); track item.id) {
            <a [routerLink]="[]" [fragment]="item.id" [class.on]="active() === item.id"
               [attr.aria-current]="active() === item.id ? 'location' : null"><ui-icon [name]="item.icon" [size]="15" /><span>{{ item.label }}</span></a>
          }
        </nav>
      </div>
      <div class="gd-main"><ng-content /></div>
    </div>
  `,
})
export class GuideLayout implements OnDestroy {
  readonly rail = input.required<GuideRailItem[]>();
  readonly badge = input<string | null>(null);
  readonly badgeTone = input<BadgeTone>('teal');
  /** Release status shown beside the rail badge, e.g. 'Beta'. */
  readonly status = input<string | null>(null);

  /** Id of the section the reader is in. Null until the rail input is read. */
  private readonly spied = signal<string | null>(null);
  private readonly doc = inject(DOCUMENT);
  private io: IntersectionObserver | null = null;
  private end: IntersectionObserver | null = null;

  constructor() {
    if (!isPlatformBrowser(inject(PLATFORM_ID))) return;
    afterNextRender(() => this.spy());
  }

  protected active(): string | null {
    return this.spied() ?? this.rail()[0]?.id ?? null;
  }

  private spy(): void {
    const view = this.doc.defaultView;
    if (!view || !('IntersectionObserver' in view)) return;
    const targets = this.rail()
      .map((item) => this.doc.getElementById(item.id))
      .filter((el): el is HTMLElement => !!el);
    if (!targets.length) return;

    // A band from just under the fixed header to 40% down the viewport; the
    // first section touching it is the one being read. The band starts a few
    // pixels below where an anchor jump lands a section (92–96px), so the
    // section above — whose last pixels would otherwise still be in the band —
    // does not win straight after a jump.
    const inBand = new Set<string>();
    const last = targets[targets.length - 1];
    let atEnd = false;
    const pick = () => {
      const current = atEnd ? last : targets.find((el) => inBand.has(el.id));
      if (current) this.spied.set(current.id);
    };
    this.io = new IntersectionObserver((entries) => {
      for (const entry of entries) {
        if (entry.isIntersecting) inBand.add(entry.target.id);
        else inBand.delete(entry.target.id);
      }
      pick();
    }, { rootMargin: '-100px 0px -60% 0px', threshold: 0 });
    targets.forEach((el) => this.io!.observe(el));

    // The last section is usually too short to reach the band before the page
    // runs out of scroll, so it takes over once all of it is on screen.
    this.end = new IntersectionObserver(([entry]) => {
      atEnd = entry.isIntersecting;
      pick();
    }, { threshold: 1 });
    this.end.observe(last);
  }

  ngOnDestroy(): void {
    this.io?.disconnect();
    this.end?.disconnect();
  }
}
