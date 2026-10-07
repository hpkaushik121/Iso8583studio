import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BLOG_POSTS, BLOG_TOPICS } from '../../content/blog-index';
import { UiReveal, UiWords } from '../../ui';
import { BlogCover } from './blog-cover';
import { longDate } from './blog-meta';

const ICON = new Map(BLOG_TOPICS.map((t) => [t.id, t.icon]));

/**
 * Up to three more posts from the same category, newest first.
 *
 * Posts named in `avoid` — the previous and next parts, which already have
 * cards of their own above — are used only when the category has nothing else
 * to offer.
 *
 * Tracking: each card is `.related-card` holding `.ui-card-title` and
 * `.ui-card-eyebrow` (the category, present for screen readers and analytics
 * but not drawn), which is what blog_card_click reports.
 */
@Component({
  selector: 'app-related-posts',
  imports: [RouterLink, UiReveal, UiWords, BlogCover],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (related().length) {
      <section class="bp-related" data-sect="post_related" uiReveal [uiRevealThreshold]="0.1">
        <div class="bp-related-head ds-hold">
          <h2><ui-words text="Related Articles" /></h2>
          <span class="bp-related-rule" aria-hidden="true"></span>
        </div>
        <div class="bp-related-grid">
          @for (item of related(); track item.post.slug; let i = $index) {
            <div class="bp-mini-slot ds-item" [style.--d]="160 + i * 70">
              <a class="related-card bp-mini" [routerLink]="item.post.path">
                <app-blog-cover [src]="item.post.thumb" [icon]="item.icon" />
                <span class="bp-mini-body">
                  <span class="ui-card-eyebrow">{{ item.post.category }}</span>
                  <span class="bp-mini-meta">{{ item.day }} · {{ item.post.minutes }} min read</span>
                  <span class="ui-card-title">{{ item.post.title }}</span>
                </span>
              </a>
            </div>
          }
        </div>
      </section>
    }
  `,
})
export class RelatedPosts {
  readonly category = input.required<string>();
  readonly excludeSlug = input.required<string>();
  /** Slugs to pass over while the category still has other posts. */
  readonly avoid = input<readonly string[]>([]);

  protected readonly related = computed(() => {
    const avoid = this.avoid();
    const pool = BLOG_POSTS
      .filter((p) => p.category === this.category() && p.slug !== this.excludeSlug());
    return [
      ...pool.filter((p) => !avoid.includes(p.slug)),
      ...pool.filter((p) => avoid.includes(p.slug)),
    ].slice(0, 3).map((post) => ({
      post,
      icon: ICON.get(post.topicId) ?? 'article',
      day: longDate(post.date),
    }));
  });
}
