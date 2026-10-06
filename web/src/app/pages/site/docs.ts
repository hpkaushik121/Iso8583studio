import {
  ChangeDetectionStrategy, Component, DestroyRef, ElementRef, PLATFORM_ID,
  afterNextRender, computed, inject, signal, viewChild,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { SitePage } from './site-page';
import { HomeHeroDashboard } from '../home/hero-dashboard';
import { UiBadge, UiCtaPanel, UiIcon, UiReveal, UiWords } from '../../ui';
import { LONGFORM, LfBadge, LfTocEntry } from '../shared/longform';
import {
  HUB_RESOURCES, HUB_SIMULATORS, HUB_SOLUTIONS, HUB_START, HUB_TOOLS, HubStatus,
} from '../data/docs-hub.data';

const pad = (n: number) => String(n).padStart(2, '0');

/**
 * The documentation hub: a filterable index of every guide, reference and
 * resource, in five groups, with a contents rail.
 *
 * Every entry is a hub card (a.hub-card > .hub-title [> .badge]) so clicks
 * report hub_card_click with the same titles and badges as before. The filter
 * and the rail's scroll-spy are browser-only; the prerender lists everything.
 */
@Component({
  selector: 'page-docs',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [...LONGFORM, HomeHeroDashboard, RouterLink, UiBadge, UiCtaPanel, UiIcon, UiReveal, UiWords],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs' },
  template: `
    <lf-header layout="art" [crumbs]="crumbs" [badge]="badge"
               title="Documentation"
               lede="Guides and references for every simulator, tool and service in ISO8583Studio.">
      <div class="lf-filter ds-fade" [style.--d]="220">
        <label class="lf-filter-box">
          <ui-icon name="magnifying-glass" [size]="16" />
          <span class="visually-hidden">Filter the documentation index</span>
          <input #box type="search" autocomplete="off" spellcheck="false"
                 placeholder="Filter the index, press / to jump here"
                 [value]="q()" (input)="q.set(box.value)">
        </label>
        <span class="lf-filter-count" aria-live="polite">{{ needle() ? total() + ' of ' + all + ' entries' : all + ' entries' }}</span>
      </div>
      <div lfHeaderArt class="lf-frame ds-frame" [style.--d]="500" aria-hidden="true">
        <home-hero-dashboard [delay]="1500" />
      </div>
    </lf-header>

    <lf-body [toc]="groups()" tocLabel="On this page">
      <div lfRail class="lf-kbd-card">
        <div class="lf-kbd-title">Keyboard</div>
        <div class="lf-kbd-rows">
          <span><kbd>/</kbd> filter the index</span>
          <span><kbd>esc</kbd> clear the filter</span>
        </div>
      </div>

      @if (total() === 0) {
        <div class="lf-empty">
          <span class="lf-card-ico"><ui-icon name="magnifying-glass" [size]="17" /></span>
          <span class="lf-empty-title">Nothing matches “{{ q().trim() }}”</span>
          <span class="lf-empty-body">Try a message type (0200), a tool family (DUKPT, TR-31, payShield) or a platform name. Full-text search across guide bodies lives on the guide pages themselves.</span>
          <button type="button" class="btn btn--ghost" (click)="q.set('')">Clear filter</button>
        </div>
      }

      <section lfSection id="start" heading="Start here" [count]="count(start().length)" [hold]="false"
               [hidden]="!start().length"
               sub="Three pages cover the first hour: install the studio, bring up a host session, and know which release you are on.">
        <div class="lf-start-grid">
          @for (s of start(); track s.title; let i = $index) {
            <div class="lf-start-cell ds-item" [style.--d]="260 + i * 140">
              <a class="hub-card lf-start" [class.lf-start--wide]="i === 0" [attr.href]="s.href">
                <span class="lf-start-ico"><ui-icon [name]="s.icon" [size]="18" /></span>
                <span class="hub-title">{{ s.title }}</span>
                <span class="hub-desc">{{ s.body }}</span>
                @if (s.steps; as steps) {
                  <span class="lf-start-steps">
                    @for (step of steps; track step; let n = $index) {
                      <span class="lf-start-step"><span class="lf-start-step-n">{{ n + 1 }}</span>{{ step }}</span>
                    }
                  </span>
                }
                <span class="hub-link">{{ s.cta }}<ui-icon name="arrow-right" [size]="14" /></span>
              </a>
            </div>
          }
        </div>
      </section>

      <section lfSection id="simulators" heading="Simulator guides" [count]="count(sims().length)" [hold]="false"
               [hidden]="!sims().length">
        <p class="lf-sub ds-hold" [style.--d]="220">One guide per simulator: configuration tabs, runtime behaviour and protocol detail. Status reflects what ships in the current build. Or start from the <a href="/simulator">all-simulators overview</a>.</p>
        <div class="lf-guides">
          @for (g of sims(); track g.title; let i = $index) {
            <div class="lf-guide-cell ds-item" [style.--d]="240 + i * 60">
              <a class="hub-card lf-guide" [class]="tone[g.status].row" [attr.href]="g.href">
                <span class="lf-guide-ico"><ui-icon [name]="g.icon" [size]="17" /></span>
                <span class="lf-guide-text">
                  <span class="hub-title">{{ g.title }} <span class="badge" [class]="tone[g.status].badge">{{ g.status }}</span></span>
                  <span class="hub-desc">{{ g.desc }}</span>
                </span>
                <ui-icon class="lf-guide-arrow" name="arrow-right" [size]="15" />
              </a>
            </div>
          }
        </div>
      </section>

      <section lfSection id="tools" heading="Tool references" [count]="count(tools().length)" [hold]="false"
               [hidden]="!tools().length"
               sub="Category references for the 64 tools in the studio: calculators and codecs, grouped by family. The number is how many tools that reference covers.">
        <div class="lf-tiles">
          @for (t of tools(); track t.title; let i = $index) {
            <div class="lf-tile-cell ds-item" [style.--d]="240 + i * 60">
              <a class="hub-card lf-tile" [attr.href]="t.href">
                <ui-icon class="lf-tile-ico" [name]="t.icon" [size]="18" />
                <span class="hub-title">
                  <span class="lf-tile-name">{{ t.title }}</span>{{ ' ' }}
                  @if (t.count !== null) {
                    <span class="badge">{{ t.count }}<span class="lf-tile-unit">&ngsp;tools</span></span>
                  }
                </span>
                <span class="hub-desc">{{ t.desc }}</span>
              </a>
            </div>
          }
        </div>
      </section>

      <section lfSection id="solutions" heading="Solutions and services" [count]="count(sols().length)" [hold]="false"
               [hidden]="!sols().length"
               sub="Engagements built on the same engine, for teams that need certification, hosted endpoints or kernel work.">
        <div class="lf-sols">
          @for (s of sols(); track s.title; let i = $index) {
            <div class="lf-sol-cell ds-item" [style.--d]="240 + i * 60">
              <a class="hub-card lf-sol" [attr.href]="s.href">
                <span class="lf-sol-ico"><ui-icon [name]="s.icon" [size]="17" /></span>
                <span class="hub-title">{{ s.title }}</span>
                <span class="hub-desc">{{ s.desc }}</span>
                <span class="hub-link">Learn more<ui-icon name="arrow-right" [size]="13" /></span>
              </a>
            </div>
          }
        </div>
      </section>

      <section lfSection id="resources" heading="Resources" [count]="count(res().length)" [hidden]="!res().length">
        <div class="lf-chips">
          @for (r of res(); track r.title) {
            <a class="hub-card lf-chip" [attr.href]="r.href" [attr.title]="r.desc">
              <ui-icon class="lf-chip-ico" [name]="r.icon" [size]="15" />
              <span class="hub-title">{{ r.title }}</span>
              @if (external(r.href)) { <ui-icon name="arrow-up-right" [size]="12" /> } @else { <ui-icon name="arrow-right" [size]="12" /> }
            </a>
          }
        </div>
      </section>

      <div class="lf-cta">
        <ui-cta-panel>
          <div class="lf-cta-in" uiReveal>
            <div class="ds-hold"><ui-badge tone="blue" icon="sparkle">Pro</ui-badge></div>
            <h2><ui-words text="Testing with a team, or certifying with a scheme?" /></h2>
            <p class="ds-hold" [style.--d]="320">Pro raises the CPS ceiling, unlocks the full algorithm set and deep simulator tweaks, plus hosted endpoints and priority support.</p>
            <div class="pro-nudge ds-hold" [style.--d]="440">
              <a class="btn btn--primary btn--lg btn--glow" routerLink="/pro">Register for Pro <ui-icon name="arrow-up-right" [size]="16" /></a>
            </div>
          </div>
        </ui-cta-panel>
      </div>
    </lf-body>
  `,
})
export class DocsPage {
  protected readonly crumbs = [{ label: 'Home', link: '/' }, { label: 'Documentation' }];
  protected readonly badge: LfBadge = { tone: 'teal', label: 'Docs for v1.0.14' };
  protected readonly all =
    HUB_START.length + HUB_SIMULATORS.length + HUB_TOOLS.length + HUB_SOLUTIONS.length + HUB_RESOURCES.length;

  /** Row and badge classes per status. */
  protected readonly tone: Record<HubStatus, { row: string; badge: string }> = {
    'Available': { row: 'lf-guide--available', badge: 'badge--teal' },
    'Beta': { row: 'lf-guide--beta', badge: 'badge--blue' },
    'In development': { row: 'lf-guide--dev', badge: 'badge--neutral' },
  };

  protected readonly q = signal('');
  protected readonly needle = computed(() => this.q().trim().toLowerCase());

  protected readonly start = computed(() => HUB_START.filter((s) => this.hit(s.title, s.body, s.cta)));
  protected readonly sims = computed(() => HUB_SIMULATORS.filter((g) => this.hit(g.title, g.desc, g.status)));
  protected readonly tools = computed(() => HUB_TOOLS.filter((t) => this.hit(t.title, t.desc)));
  protected readonly sols = computed(() => HUB_SOLUTIONS.filter((s) => this.hit(s.title, s.desc)));
  protected readonly res = computed(() => HUB_RESOURCES.filter((r) => this.hit(r.title, r.desc)));

  protected readonly total = computed(() =>
    this.start().length + this.sims().length + this.tools().length + this.sols().length + this.res().length);

  /** Rail entries; a group the filter has emptied drops out. */
  protected readonly groups = computed<LfTocEntry[]>(() => [
    { id: 'start', label: 'Start here', icon: 'flag', count: this.start().length },
    { id: 'simulators', label: 'Simulator guides', icon: 'arrows-left-right', count: this.sims().length },
    { id: 'tools', label: 'Tool references', icon: 'wrench', count: this.tools().length },
    { id: 'solutions', label: 'Solutions', icon: 'buildings', count: this.sols().length },
    { id: 'resources', label: 'Resources', icon: 'bookmark-simple', count: this.res().length },
  ].filter((g) => g.count > 0));

  private readonly box = viewChild<ElementRef<HTMLInputElement>>('box');

  constructor() {
    if (!isPlatformBrowser(inject(PLATFORM_ID))) return;
    const host = inject<ElementRef<HTMLElement>>(ElementRef);
    const destroyRef = inject(DestroyRef);
    afterNextRender(() => {
      const doc = host.nativeElement.ownerDocument;
      const onKey = (event: KeyboardEvent) => {
        const tag = (event.target as HTMLElement | null)?.tagName ?? '';
        if (event.key === '/' && tag !== 'INPUT' && tag !== 'TEXTAREA' && tag !== 'SELECT' &&
            !event.metaKey && !event.ctrlKey && !event.altKey) {
          event.preventDefault();
          this.box()?.nativeElement.focus();
        }
        if (event.key === 'Escape' && this.q()) this.q.set('');
      };
      doc.addEventListener('keydown', onKey);
      destroyRef.onDestroy(() => doc.removeEventListener('keydown', onKey));
    });
  }

  protected count(n: number): string { return pad(n); }

  protected external(href: string): boolean { return href.startsWith('http'); }

  private hit(...parts: string[]): boolean {
    const needle = this.needle();
    return !needle || parts.join(' ').toLowerCase().includes(needle);
  }
}
