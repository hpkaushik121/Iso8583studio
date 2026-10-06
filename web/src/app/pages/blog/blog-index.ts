import {
  ChangeDetectionStrategy, Component, DestroyRef, ElementRef, afterNextRender, computed, inject,
  signal, viewChild,
} from '@angular/core';
import { Location } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { BLOG_POSTS, BLOG_TOPICS } from '../../content/blog-index';
import { EXTERNAL } from '../../core/site-nav';
import { UiBadge, UiCtaPanel, UiIcon, UiReveal, UiWords } from '../../ui';
import { SitePage } from '../site/site-page';
import { BlogCover } from './blog-cover';
import { BlogMeta, BlogTopic, longDate, shortDate } from './blog-meta';

type Sort = 'new' | 'old' | 'short' | 'long';

/** A post with everything the card needs looked up once. */
interface Entry {
  post: BlogMeta;
  topic: BlogTopic;
  /** Lower-cased text the search box matches against. */
  hay: string;
  /** Position in the newest-first list; the tie-break for every sort. */
  rank: number;
  day: string;
  fullDay: string;
  /** The newest post: the one the unfiltered index leads with. */
  latest: boolean;
}

/** One card as the grid draws it right now. */
interface Card extends Entry {
  hidden: boolean;
  featured: boolean;
  /** Stagger for the fade-in, in ms. */
  delay: number;
}

const PAGE = 12;
const SAVE_KEY = 'iso8583studio-blog-saved';

const TOPIC_BY_ID = new Map(BLOG_TOPICS.map((t) => [t.id, t]));

const ENTRIES: Entry[] = BLOG_POSTS.map((post, rank) => ({
  post,
  topic: TOPIC_BY_ID.get(post.topicId)!,
  hay: `${post.title} ${post.description} ${post.tags.join(' ')} ${post.category}`.toLowerCase(),
  rank,
  day: shortDate(post.date),
  fullDay: longDate(post.date),
  // BLOG_POSTS is generated newest first.
  latest: rank === 0,
}));

const ORDER: Record<Sort, Entry[]> = {
  new: ENTRIES,
  old: [...ENTRIES].reverse(),
  short: [...ENTRIES].sort((a, b) => a.post.minutes - b.post.minutes || a.rank - b.rank),
  long: [...ENTRIES].sort((a, b) => b.post.minutes - a.post.minutes || a.rank - b.rank),
};

const SORTS: { value: Sort; label: string }[] = [
  { value: 'new', label: 'Newest' },
  { value: 'old', label: 'Oldest' },
  { value: 'short', label: 'Shortest read' },
  { value: 'long', label: 'Longest read' },
];

const TAG_COUNT = new Map<string, number>();
for (const { post } of ENTRIES) {
  for (const tag of post.tags) TAG_COUNT.set(tag, (TAG_COUNT.get(tag) ?? 0) + 1);
}
/* Most used first. The tie-break is a plain code-point comparison rather than
   localeCompare, so the prerender and the browser always agree on the order. */
const TOP_TAGS = [...TAG_COUNT.keys()].sort((a, b) =>
  TAG_COUNT.get(b)! - TAG_COUNT.get(a)! || (a < b ? -1 : a > b ? 1 : 0));
const TRENDING = TOP_TAGS.slice(0, 6);
const FILTER_TAGS = TOP_TAGS.slice(0, 18).map((name) => ({ name, count: TAG_COUNT.get(name)! }));

/** The newest post of each topic lends its cover to the topic's tile. */
const TOPIC_TILES = BLOG_TOPICS.map((topic) => ({
  topic,
  cover: ENTRIES.find((e) => e.post.topicId === topic.id)?.post.thumb ?? null,
}));

/**
 * The blog index: a search hero with a strip of topics, a sticky toolbar
 * (sort, topic tabs, tag filters), the latest post as a feature, and an
 * image-first grid that loads twelve more at a time.
 *
 * Every post's card is always in the document. Search, the filters and "load
 * more" only set `hidden` on the cards they exclude, so the prerendered page
 * links to all of the posts and the browser never has to build a card.
 *
 * Tracking: a card's link is `a.card--post` holding `.ui-card-title` and
 * `.ui-card-eyebrow` (the category), which is what blog_card_click reports;
 * the topic tabs are `button.filter-btn`, reported as blog_filter. The
 * bookmark and the topic chip are buttons beside the card's link, never
 * inside it.
 */
@Component({
  selector: 'app-blog-index',
  imports: [RouterLink, UiBadge, UiCtaPanel, UiIcon, UiReveal, UiWords, BlogCover],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'bl-page' },
  // Same section funnel and router hand-off as every site page; see the note
  // on blog-post.ts about the one-Route-per-page requirement.
  hostDirectives: [SitePage],
  template: `
    <header class="bl-hero" data-sect="blog_hero">
      <span class="bl-hero-glow" aria-hidden="true"></span>
      <span class="bl-scan" aria-hidden="true"></span>
      <div class="bl-hero-in">
        <div class="ds-fade" [style.--d]="60">
          <ui-badge tone="blue" icon="article">{{ postCount }} guides · {{ tiles.length }} topics</ui-badge>
        </div>
        <h1 class="bl-h1 ds-in"><ui-words text="ISO8583Studio Blog" [base]="160" /></h1>
        <p class="bl-lede ds-fade" [style.--d]="260">
          Expert guides on payment testing, ISO 8583, HSM simulation, EMV tools,
          cryptography, and fintech development.
        </p>

        <div class="bl-search ds-fade" role="search" [style.--d]="360">
          <ui-icon name="magnifying-glass" [size]="19" />
          <input #box type="text" inputmode="search" enterkeyhint="search" autocomplete="off"
                 aria-label="Search guides" [placeholder]="'Search ' + postCount + ' guides…'"
                 [value]="query()" (input)="setQuery(box.value)" (keydown.enter)="toGrid()">
          @if (query()) {
            <button type="button" class="bl-search-x" aria-label="Clear search"
                    (click)="setQuery(''); box.focus()"><ui-icon name="x" [size]="15" /></button>
          }
          <button type="button" class="bl-search-go" aria-label="Search" (click)="toGrid()">
            <ui-icon name="magnifying-glass" [size]="18" />
          </button>
        </div>

        <div class="bl-trending ds-fade" [style.--d]="440">
          <span class="bl-trending-label">Trending searches</span>
          @for (tag of trending; track tag) {
            <button type="button" class="bl-pill bl-pill--sm"
                    [attr.aria-pressed]="needle() === tag.toLowerCase()"
                    (click)="setQuery(tag); toGrid()">{{ tag }}</button>
          }
        </div>
      </div>

      <div class="bl-strip-wrap">
        <div class="bl-strip-in">
          <div #strip class="bl-strip" role="group" aria-label="Browse by topic">
            @for (tile of tiles; track tile.topic.id; let i = $index) {
              <button type="button" class="bl-tile ds-fade" [style.--d]="520 + i * 60"
                      [attr.aria-pressed]="topic() === tile.topic.id" [title]="tile.topic.summary"
                      (click)="pick(topic() === tile.topic.id ? null : tile.topic.id)">
                <span class="bl-tile-art">
                  <app-blog-cover [src]="tile.cover" [icon]="tile.topic.icon" />
                  <span class="bl-tile-shade" aria-hidden="true"></span>
                  <span class="bl-tile-meta">
                    <span class="bl-tile-icon"><ui-icon [name]="tile.topic.icon" [size]="16" /></span>
                    <span class="bl-tile-count">{{ tile.topic.count }} guides</span>
                  </span>
                </span>
                <span class="bl-tile-name">{{ tile.topic.name }}</span>
              </button>
            }
          </div>
          <button type="button" class="bl-strip-arrow bl-strip-arrow--prev"
                  aria-label="Scroll topics left" (click)="scrollStrip(-1)">
            <ui-icon name="caret-left" [size]="16" />
          </button>
          <button type="button" class="bl-strip-arrow bl-strip-arrow--next"
                  aria-label="Scroll topics right" (click)="scrollStrip(1)">
            <ui-icon name="caret-right" [size]="16" />
          </button>
        </div>
      </div>
    </header>

    <div #sentinel class="bl-sentinel" aria-hidden="true"></div>
    <div #toolbar class="bl-toolbar" [class.is-stuck]="stuck()">
      <div class="bl-toolbar-in">
        <span class="bl-select">
          <select #order aria-label="Sort guides" (change)="setSort(order.value)">
            @for (s of sorts; track s.value) {
              <option [value]="s.value" [selected]="sort() === s.value">{{ s.label }}</option>
            }
          </select>
          <ui-icon name="caret-down" [size]="13" />
        </span>

        <div class="bl-tabs" role="group" aria-label="Topics">
          <button class="filter-btn" type="button" [attr.aria-pressed]="topic() === null"
                  (click)="pick(null)">All</button>
          @for (tile of tiles; track tile.topic.id) {
            <button class="filter-btn" type="button" [attr.aria-pressed]="topic() === tile.topic.id"
                    (click)="pick(tile.topic.id)">{{ tile.topic.name }}</button>
          }
        </div>

        <button type="button" class="bl-pill bl-filter-toggle"
                [class.is-on]="filtersOpen() || tags().size > 0"
                [attr.aria-expanded]="filtersOpen()" aria-controls="blog-tag-filters"
                (click)="filtersOpen.set(!filtersOpen())">
          <ui-icon name="sliders-horizontal" [size]="15" />Filters
          @if (tags().size > 0) { <span class="bl-count">{{ tags().size }}</span> }
        </button>
      </div>

      <div id="blog-tag-filters" class="bl-tags" [hidden]="!filtersOpen()">
        <div class="bl-tags-in">
          <span class="bl-tags-label">TAGS</span>
          @for (tag of filterTags; track tag.name) {
            <button type="button" class="bl-pill bl-pill--sm" [attr.aria-pressed]="tags().has(tag.name)"
                    (click)="toggleTag(tag.name)">{{ tag.name }}<span class="bl-tag-n">{{ tag.count }}</span></button>
          }
          @if (tags().size > 0) {
            <button type="button" class="bl-text-btn bl-text-btn--end" (click)="clearTags()">Clear tags</button>
          }
        </div>
      </div>
    </div>

    <div #grid class="bl-main">
      <!-- Landing spots for /blogs#<topic>: the link selects the topic (see
           the constructor) and the page settles with the grid under the toolbar. -->
      <div class="bl-anchors" aria-hidden="true">
        @for (tile of tiles; track tile.topic.id) { <span class="bl-anchor" [id]="tile.topic.id"></span> }
      </div>

      @if (filtering()) {
        <div class="bl-status">
          <span class="bl-status-n">{{ view().total }} {{ view().total === 1 ? 'guide' : 'guides' }}</span>
          @if (activeTopic(); as t) { <span>in {{ t.name }}</span> }
          @if (needle()) { <span>matching “{{ query().trim() }}”</span> }
          @if (tags().size > 0) { <span>tagged {{ tagList() }}</span> }
          <button type="button" class="bl-text-btn bl-text-btn--end" (click)="clearAll()">Clear all</button>
        </div>
      }

      @if (view().total === 0) {
        <div class="bl-empty">
          <span class="bl-empty-icon"><ui-icon name="magnifying-glass" [size]="22" /></span>
          <p class="bl-empty-title">No guides match {{ needle() ? '“' + query().trim() + '”' : 'these filters' }}</p>
          <p class="bl-empty-sub">Try another spelling, or browse a topic instead.</p>
          <button type="button" class="btn btn--secondary" (click)="clearAll()">Show all guides</button>
        </div>
      }

      <div class="bl-grid" data-sect="blog_grid">
        @for (card of view().cards; track card.post.slug) {
          <article class="bl-card ds-fade" [class.is-featured]="card.featured"
                   [hidden]="card.hidden" [style.--d]="card.delay">
            <div class="bl-shot">
              <a class="bl-shot-link card--post" [routerLink]="card.post.path">
                <app-blog-cover [src]="card.post.thumb" [icon]="card.topic.icon"
                                [full]="card.latest && card.post.thumb ? card.post.thumb.replace('.webp', '-1376.webp') : null"
                                sizes="(max-width: 1040px) 100vw, 640px" [eager]="card.featured" />
                <span class="bl-shot-shade" aria-hidden="true"></span>
                <span class="ui-card-eyebrow">{{ card.post.category }}</span>
                <span class="ui-card-title">{{ card.post.title }}</span>
                <span class="bl-round bl-round--open" aria-hidden="true"><ui-icon name="arrow-up-right" [size]="16" /></span>
              </a>
              <button type="button" class="bl-round bl-round--save"
                      [attr.aria-pressed]="saved().has(card.post.slug)"
                      [attr.aria-label]="saved().has(card.post.slug) ? 'Remove from saved' : 'Save guide'"
                      [title]="saved().has(card.post.slug) ? 'Remove from saved' : 'Save guide'"
                      (click)="toggleSave(card.post.slug)">
                <ui-icon [name]="saved().has(card.post.slug) ? 'bookmark-simple-fill' : 'bookmark-simple'" [size]="16" />
              </button>
            </div>

            <div class="bl-card-meta">
              <button type="button" class="bl-topic" (click)="pick(card.topic.id)"
                      [attr.aria-label]="'Show ' + card.topic.name + ' guides'">
                <span class="bl-avatar" aria-hidden="true"><ui-icon [name]="card.topic.icon" [size]="12" /></span>
                <span class="bl-topic-name">{{ card.topic.name }}</span>
              </button>
              @if (card.latest) { <span class="bl-new">NEW</span> }
              <span class="bl-stats">
                <span><ui-icon name="clock" [size]="13" />{{ card.post.minutes }} min</span>
                <span><ui-icon name="calendar-blank" [size]="13" />{{ card.day }}</span>
              </span>
            </div>

            <!-- The latest post opens the unfiltered index as a feature; these
                 are the parts only that layout shows. It is a second
                 .card--post so its two links report blog_card_click as well. -->
            @if (card.latest) {
              <div class="bl-feat card--post">
                <div class="bl-feat-top">
                  <span class="bl-avatar bl-avatar--lg" aria-hidden="true"><ui-icon [name]="card.topic.icon" [size]="14" /></span>
                  <span class="ui-card-eyebrow">{{ card.post.category }}</span>
                  <ui-badge tone="blue" [dot]="true">Latest</ui-badge>
                </div>
                <h2 class="ui-card-title"><a [routerLink]="card.post.path">{{ card.post.title }}</a></h2>
                <p class="bl-feat-desc">{{ card.post.description }}</p>
                <div class="bl-feat-foot">
                  <span class="bl-feat-date">{{ card.fullDay }} · {{ card.post.minutes }} min read</span>
                  <a class="bl-feat-read" [routerLink]="card.post.path">Read the guide<ui-icon name="arrow-up-right" [size]="13" /></a>
                </div>
              </div>
            }
          </article>
        }
      </div>

      @if (view().shown < view().pool) {
        <div class="bl-more">
          <button type="button" class="btn btn--secondary btn--lg" (click)="loadMore()">Load more guides</button>
          <span class="bl-more-n">{{ view().shown }} of {{ view().pool }}</span>
        </div>
      }

      <ui-cta-panel class="bl-cta">
        <div class="bl-cta-in" uiReveal>
          <div class="ds-hold"><ui-badge tone="teal" icon="download-simple">Free and open source</ui-badge></div>
          <h2><ui-words text="Every guide runs on the free desktop app." /></h2>
          <p class="ds-hold" [style.--d]="320">
            ISO8583Studio ships the simulators, calculators and parsers these guides use,
            for macOS, Windows and Linux. AGPL v3, no license fees.
          </p>
          <div class="cta-actions ds-hold" [style.--d]="440">
            <a class="btn btn--primary btn--lg btn--glow" [href]="releases">Download Studio<ui-icon name="arrow-up-right" [size]="16" /></a>
          </div>
        </div>
      </ui-cta-panel>
    </div>
  `,
})
export class BlogIndex {
  private readonly location = inject(Location);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly releases = EXTERNAL.releases;
  protected readonly postCount = ENTRIES.length;
  protected readonly tiles = TOPIC_TILES;
  protected readonly sorts = SORTS;
  protected readonly trending = TRENDING;
  protected readonly filterTags = FILTER_TAGS;

  /** Selected topic id; null is "All". */
  protected readonly topic = signal<string | null>(null);
  protected readonly query = signal('');
  protected readonly sort = signal<Sort>('new');
  protected readonly tags = signal<ReadonlySet<string>>(new Set());
  protected readonly filtersOpen = signal(false);
  protected readonly saved = signal<ReadonlySet<string>>(new Set());
  protected readonly stuck = signal(false);
  /** How many grid cards "load more" has revealed so far. */
  private readonly limit = signal(PAGE);

  protected readonly needle = computed(() => this.query().trim().toLowerCase());
  protected readonly activeTopic = computed(() => TOPIC_BY_ID.get(this.topic() ?? '') ?? null);
  protected readonly tagList = computed(() => [...this.tags()].join(', '));
  protected readonly filtering = computed(() =>
    this.topic() !== null || this.needle() !== '' || this.tags().size > 0);

  protected readonly view = computed(() => {
    const topic = this.topic(), needle = this.needle(), tags = this.tags(), sort = this.sort();
    const limit = this.limit();

    const matched = ORDER[sort].filter((e) =>
      (topic === null || e.post.topicId === topic) &&
      (needle === '' || e.hay.includes(needle)) &&
      (tags.size === 0 || e.post.tags.some((t) => tags.has(t))));

    // With nothing narrowed down, the latest post leads as the feature and the
    // grid counts from the one after it.
    const featured = !this.filtering() && sort === 'new';
    const lead = featured ? 1 : 0;
    const pool = matched.length - lead;
    const kept = new Set(matched);

    const cards: Card[] = [
      ...matched.map((e, i) => ({
        ...e,
        featured: featured && i === 0,
        hidden: i - lead >= limit,
        delay: Math.min(Math.max(i - lead, 0), 11) * 45,
      })),
      // Excluded cards stay in the document, after the ones that match.
      ...ENTRIES.filter((e) => !kept.has(e))
        .map((e) => ({ ...e, featured: false, hidden: true, delay: 0 })),
    ];
    return { cards, total: matched.length, pool, shown: Math.min(limit, pool) };
  });

  private readonly strip = viewChild.required<ElementRef<HTMLElement>>('strip');
  private readonly sentinel = viewChild.required<ElementRef<HTMLElement>>('sentinel');
  private readonly toolbar = viewChild.required<ElementRef<HTMLElement>>('toolbar');
  private readonly grid = viewChild.required<ElementRef<HTMLElement>>('grid');

  private browser = false;

  constructor() {
    afterNextRender(() => {
      this.browser = true;

      try {
        const stored: unknown = JSON.parse(localStorage.getItem(SAVE_KEY) || '[]');
        if (Array.isArray(stored)) this.saved.set(new Set(stored.map(String)));
      } catch { /* private mode or a corrupt value: start with nothing saved */ }

      // /blogs#emv-tools opens on that topic. Read here rather than during
      // render, so the prerendered page and the first client render both show
      // "All" and hydration has nothing to reconcile.
      this.route.fragment.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((fragment) => {
        this.applyTopic(TOPIC_BY_ID.has(fragment ?? '') ? fragment : null);
      });

      this.watchStuck();
    });
  }

  // ---- filters -------------------------------------------------------------

  /** A topic chosen on the page: filter, put it in the URL, show the grid. */
  protected pick(id: string | null): void {
    this.applyTopic(id);
    // Written straight to the address bar rather than through the router: a
    // router navigation is what analytics counts as a page view, and picking
    // a topic is a filter on this page, already reported as blog_filter.
    const path = this.location.path(false);
    this.location.replaceState(id ? `${path}#${id}` : path);
    this.toGrid();
  }

  private applyTopic(id: string | null): void {
    if (this.topic() === id) return;
    this.topic.set(id);
    this.limit.set(PAGE);
    this.revealTile(id);
  }

  protected setQuery(value: string): void {
    this.query.set(value);
    this.limit.set(PAGE);
  }

  protected setSort(value: string): void {
    this.sort.set(SORTS.some((s) => s.value === value) ? (value as Sort) : 'new');
    this.limit.set(PAGE);
  }

  protected toggleTag(tag: string): void {
    const next = new Set(this.tags());
    if (!next.delete(tag)) next.add(tag);
    this.tags.set(next);
    this.limit.set(PAGE);
  }

  protected clearTags(): void {
    this.tags.set(new Set());
    this.limit.set(PAGE);
  }

  protected clearAll(): void {
    this.query.set('');
    this.tags.set(new Set());
    this.sort.set('new');
    this.limit.set(PAGE);
    this.pick(null);
  }

  protected loadMore(): void {
    this.limit.update((n) => n + PAGE);
  }

  // ---- bookmarks -----------------------------------------------------------

  protected toggleSave(slug: string): void {
    const next = new Set(this.saved());
    if (!next.delete(slug)) next.add(slug);
    this.saved.set(next);
    try { localStorage.setItem(SAVE_KEY, JSON.stringify([...next])); } catch { /* storage unavailable */ }
  }

  // ---- scrolling -----------------------------------------------------------

  private scrollBehavior(): ScrollBehavior {
    return matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth';
  }

  /** Height the fixed header occupies — the toolbar's own sticky offset. */
  private barHeight(): number {
    return parseFloat(getComputedStyle(this.toolbar().nativeElement).top) || 72;
  }

  /** Brings the top of the grid up under the toolbar, unless it is there already. */
  protected toGrid(): void {
    if (!this.browser) return;
    requestAnimationFrame(() => {
      const y = this.grid().nativeElement.getBoundingClientRect().top + scrollY - this.barHeight() - 76;
      if (Math.abs(scrollY - y) > 40) scrollTo({ top: y, behavior: this.scrollBehavior() });
    });
  }

  protected scrollStrip(direction: -1 | 1): void {
    const row = this.strip().nativeElement;
    row.scrollBy({ left: direction * Math.round(row.clientWidth * .7), behavior: this.scrollBehavior() });
  }

  /** Keeps the selected topic's tile in view within the strip. */
  private revealTile(id: string | null): void {
    if (!this.browser || id === null) return;
    const row = this.strip().nativeElement;
    const tile = row.children[TOPIC_TILES.findIndex((t) => t.topic.id === id)] as HTMLElement | undefined;
    if (!tile) return;
    const left = tile.offsetLeft - row.scrollLeft;
    if (left < 24 || left + tile.offsetWidth > row.clientWidth - 24) {
      row.scrollTo({
        left: tile.offsetLeft - row.clientWidth / 2 + tile.offsetWidth / 2,
        behavior: this.scrollBehavior(),
      });
    }
  }

  /** The toolbar takes a glass backing once it is pinned under the header. */
  private watchStuck(): void {
    let frame = 0;
    const measure = () => {
      frame = 0;
      const stuck = this.sentinel().nativeElement.getBoundingClientRect().top <= this.barHeight() + 1;
      if (stuck !== this.stuck()) this.stuck.set(stuck);
    };
    const schedule = () => { if (!frame) frame = requestAnimationFrame(measure); };
    addEventListener('scroll', schedule, { passive: true });
    addEventListener('resize', schedule);
    measure();
    this.destroyRef.onDestroy(() => {
      removeEventListener('scroll', schedule);
      removeEventListener('resize', schedule);
      if (frame) cancelAnimationFrame(frame);
    });
  }
}
