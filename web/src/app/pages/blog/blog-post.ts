import {
  ChangeDetectionStrategy, Component, DestroyRef, ElementRef, afterNextRender, computed, inject,
  signal, viewChild,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DomSanitizer } from '@angular/platform-browser';
import { BLOG_POSTS, BLOG_TOPICS } from '../../content/blog-index';
import { EXTERNAL } from '../../core/site-nav';
import { Crumb, UiBadge, UiBreadcrumb, UiCtaPanel, UiIcon, UiReveal, UiWords } from '../../ui';
import { SitePage } from '../site/site-page';
import { RelatedPosts } from './related-posts';
import { BlogMeta, PostContent, longDate } from './blog-meta';

const TOPIC_BY_ID = new Map(BLOG_TOPICS.map((t) => [t.id, t]));
const two = (n: number) => String(n).padStart(2, '0');

/**
 * One article: header, cover, the rendered Markdown body beside a sticky
 * table of contents, then tags, the previous and next parts of the topic's
 * series, related posts and the closing call to action.
 *
 * The body is HTML produced at build time by tools/build-blog-routes.mjs and
 * delivered by the route's resolver, so the heading and the whole article are
 * in the prerendered file. The read-progress bar, the contents scroll-spy and
 * the pointer glow in the header are started after hydration.
 */
@Component({
  selector: 'app-blog-post',
  imports: [RouterLink, UiBadge, UiBreadcrumb, UiCtaPanel, UiIcon, UiReveal, UiWords, RelatedPosts],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'bp-page' },
  // The section funnel and the router hand-off, same as every site page. The
  // directive snapshots its section list in ngAfterViewInit, which is safe
  // only because each post has its own Route object and so its own component
  // instance — collapsing these to a parameterised blogs/:slug route would
  // leave that snapshot stale and silently kill blog section_view.
  hostDirectives: [SitePage],
  template: `
    @if (post(); as p) {
      <span #progress class="bp-progress" aria-hidden="true"></span>

      <header #hero class="bp-hero" data-sect="post_hero">
        <div class="bp-dots" aria-hidden="true">
          <span class="bp-dots-base"></span>
          <span class="bp-dots-hot"></span>
        </div>
        <span class="bp-hero-glow" aria-hidden="true"></span>
        <div class="bp-hero-in">
          <div class="bp-crumbs ds-fade"><ui-breadcrumb [items]="crumbs()" /></div>
          <div class="bp-head">
            <div class="bp-kicker ds-fade" [style.--d]="80">
              <ui-badge tone="blue" [icon]="topic().icon">{{ topic().name }}</ui-badge>
              <span class="bp-part">Part {{ part().index }} of {{ part().total }}</span>
            </div>
            <h1 class="bp-title ds-in"><ui-words [text]="p.title" [base]="160" /></h1>
            <p class="bp-excerpt ds-fade" [style.--d]="380">{{ p.description }}</p>
            <div class="bp-byline ds-fade" [style.--d]="480">
              <span>{{ date() }}</span><span aria-hidden="true">·</span>
              <span>{{ p.minutes }} min read</span><span aria-hidden="true">·</span>
              <span>{{ p.author }}</span>
            </div>
          </div>
        </div>
      </header>

      <div class="bp-body">
        <div class="bp-cover ds-fade" [style.--d]="300">
          <span class="bp-cover-bloom" aria-hidden="true"></span>
          <span class="bp-cover-frame">
            @if (p.image && !coverFailed()) {
              <img #cover [src]="large(p.thumb) ?? p.image" alt="" width="1376" height="768" fetchpriority="high"
                   [attr.srcset]="p.thumb ? p.thumb + ' 640w, ' + large(p.thumb) + ' 1376w' : null"
                   [attr.sizes]="p.thumb ? '(max-width: 700px) 100vw, 1240px' : null"
                   decoding="async" (error)="coverFailed.set(true)">
            } @else {
              <span class="bp-cover-fallback" aria-hidden="true"><ui-icon [name]="topic().icon" [size]="30" /></span>
            }
          </span>
        </div>

        <div class="bp-layout" [class.bp-layout--solo]="!hasToc()">
          <article #article class="bp-article">
            <div class="prose bp-prose" data-sect="post_body" [innerHTML]="body()"></div>

            <div class="bp-tags">
              @for (tag of p.tags; track tag) { <span class="bp-tag">{{ tag }}</span> }
            </div>

            @if (prev() || next()) {
              <div class="bp-series" data-sect="post_series">
                @if (prev(); as before) {
                  <a class="bp-step bp-step--prev" [routerLink]="before.path">
                    <span class="bp-step-label"><ui-icon name="arrow-left" [size]="13" />Previous</span>
                    <span class="bp-step-title">{{ before.title }}</span>
                  </a>
                } @else {
                  <span class="bp-step-gap"></span>
                }
                @if (next(); as after) {
                  <a class="bp-step bp-step--next" [routerLink]="after.path">
                    <span class="bp-step-label">Next<ui-icon name="arrow-right" [size]="13" /></span>
                    <span class="bp-step-title">{{ after.title }}</span>
                  </a>
                }
              </div>
            }
          </article>

          @if (hasToc()) {
            <aside class="bp-aside">
              <nav class="bp-toc" aria-label="On this page">
                <span class="bp-toc-label">On this page</span>
                @for (entry of toc(); track entry.id) {
                  <a class="bp-toc-link" [class.is-on]="active() === entry.id"
                     [routerLink]="[]" [fragment]="entry.id">{{ entry.text }}</a>
                }
              </nav>
              <a class="bp-all" routerLink="/blogs" [fragment]="topic().id">
                <ui-icon name="arrow-left" [size]="13" />All {{ topic().name }} guides
              </a>
            </aside>
          }
        </div>

        <app-related-posts [category]="p.category" [excludeSlug]="p.slug" [avoid]="neighbours()" />

        <ui-cta-panel class="bp-cta">
          <div class="bp-cta-in" uiReveal>
            <div class="ds-hold"><ui-badge tone="teal" icon="download-simple">Free and open source</ui-badge></div>
            <h2><ui-words text="Try ISO8583Studio Today" /></h2>
            <p class="ds-hold" [style.--d]="320">Download the free desktop application for Windows, macOS, and Linux.</p>
            <div class="cta-actions ds-hold" [style.--d]="440">
              <a class="btn btn--primary btn--lg btn--glow" [href]="releases">Download Free<ui-icon name="arrow-up-right" [size]="16" /></a>
            </div>
          </div>
        </ui-cta-panel>
      </div>
    }
  `,
})
export class BlogPost {
  /** The full-width WebP of a cover thumbnail. The JPG stays as the og:image. */
  protected large(thumb: string | null): string | null {
    return thumb ? thumb.replace(/\.webp$/, '-1376.webp') : null;
  }

  private readonly route = inject(ActivatedRoute);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly releases = EXTERNAL.releases;

  private readonly data = toSignal(this.route.data, { requireSync: true });

  protected readonly post = computed<BlogMeta | undefined>(() =>
    BLOG_POSTS.find((p) => p.slug === this.data()['slug']),
  );

  private readonly content = computed<PostContent>(() =>
    (this.data()['post'] as PostContent | undefined) ?? { html: '', toc: [] },
  );

  /**
   * The HTML is produced at build time by our own Markdown renderer, never by
   * a user, so bypassing the sanitizer is safe here — and necessary, because
   * the sanitizer strips the heading ids that in-page anchors rely on.
   */
  protected readonly body = computed(() => this.sanitizer.bypassSecurityTrustHtml(this.content().html));

  protected readonly toc = computed(() => this.content().toc);
  /** A single heading is not worth a rail. */
  protected readonly hasToc = computed(() => this.toc().length > 1);

  protected readonly topic = computed(() => TOPIC_BY_ID.get(this.post()?.topicId ?? '') ?? BLOG_TOPICS[0]);
  protected readonly date = computed(() => longDate(this.post()?.date ?? ''));

  /** The topic's posts in the order they were written: BLOG_POSTS is newest first. */
  private readonly series = computed(() =>
    BLOG_POSTS.filter((p) => p.topicId === this.post()?.topicId).reverse());
  private readonly position = computed(() => this.series().findIndex((p) => p.slug === this.post()?.slug));

  protected readonly part = computed(() => ({
    index: two(this.position() + 1),
    total: two(this.series().length),
  }));
  protected readonly prev = computed<BlogMeta | undefined>(() => this.series()[this.position() - 1]);
  protected readonly next = computed<BlogMeta | undefined>(() => this.series()[this.position() + 1]);
  protected readonly neighbours = computed(() =>
    [this.prev()?.slug, this.next()?.slug].filter((s): s is string => !!s));

  protected readonly crumbs = computed<Crumb[]>(() => [
    { label: 'Home', link: '/' },
    { label: 'Blog', link: '/blogs' },
    { label: this.topic().name },
  ]);

  /** Id of the heading the reader is under, for the contents rail. */
  protected readonly active = signal<string | null>(null);
  protected readonly coverFailed = signal(false);

  private readonly progress = viewChild<ElementRef<HTMLElement>>('progress');
  private readonly hero = viewChild<ElementRef<HTMLElement>>('hero');
  private readonly article = viewChild<ElementRef<HTMLElement>>('article');
  private readonly cover = viewChild<ElementRef<HTMLImageElement>>('cover');

  constructor() {
    afterNextRender(() => {
      // A cover that failed before hydration never delivers its error event.
      const cover = this.cover()?.nativeElement;
      if (cover && cover.complete && cover.naturalWidth === 0) this.coverFailed.set(true);

      this.trackReading();
      if (!matchMedia('(prefers-reduced-motion: reduce)').matches) this.lightDots();
    });
  }

  /** Drives the progress bar and marks the current heading in the contents. */
  private trackReading(): void {
    const article = this.article()?.nativeElement;
    const bar = this.progress()?.nativeElement;
    if (!article || !bar) return;

    const headings = this.toc()
      .map((entry) => article.querySelector<HTMLElement>(`[id="${entry.id}"]`))
      .filter((el): el is HTMLElement => !!el);

    let frame = 0;
    const measure = () => {
      frame = 0;
      // The bar hangs under the fixed header, so its offset is the header's height.
      const top = parseFloat(getComputedStyle(bar).top) || 72;
      const rect = article.getBoundingClientRect();
      const span = rect.height - (innerHeight - top);
      const done = span > 0 ? Math.min(1, Math.max(0, (top - rect.top) / span)) : 0;
      bar.style.transform = `scaleX(${done.toFixed(4)})`;

      let current: string | null = null;
      for (const heading of headings) {
        if (heading.getBoundingClientRect().top < top + 140) current = heading.id;
      }
      if (current !== this.active()) this.active.set(current);
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

  /** Brightens the header's dot texture around the pointer. */
  private lightDots(): void {
    const hero = this.hero()?.nativeElement;
    if (!hero) return;

    let frame = 0, x = 0, y = 0;
    const paint = () => {
      frame = 0;
      hero.style.setProperty('--mx', `${x}px`);
      hero.style.setProperty('--my', `${y}px`);
      hero.classList.add('is-lit');
    };
    const move = (event: PointerEvent) => {
      const rect = hero.getBoundingClientRect();
      x = event.clientX - rect.left;
      y = event.clientY - rect.top;
      if (!frame) frame = requestAnimationFrame(paint);
    };
    const leave = () => hero.classList.remove('is-lit');

    hero.addEventListener('pointermove', move, { passive: true });
    hero.addEventListener('pointerleave', leave);
    this.destroyRef.onDestroy(() => {
      hero.removeEventListener('pointermove', move);
      hero.removeEventListener('pointerleave', leave);
      if (frame) cancelAnimationFrame(frame);
    });
  }
}
