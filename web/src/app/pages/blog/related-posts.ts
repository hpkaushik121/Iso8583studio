import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BLOG_POSTS } from '../../content/blog-index';
import { UiTag } from '../../ui/tag';

/** Up to three posts from the same category, matching the previous rule. */
@Component({
  selector: 'app-related-posts',
  imports: [RouterLink, UiTag],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (related().length) {
      <section class="related-posts">
        <h3>Related Articles</h3>
        <div class="related-grid">
          @for (post of related(); track post.slug) {
            <a class="card card--interactive related-card" [routerLink]="post.path">
              <span class="ui-card-eyebrow">{{ post.category }}</span>
              <span class="ui-card-title">{{ post.title }}</span>
              <span class="tag-row">
                @for (tag of post.tags.slice(0, 3); track tag) { <ui-tag>{{ tag }}</ui-tag> }
              </span>
            </a>
          }
        </div>
      </section>
    }
  `,
})
export class RelatedPosts {
  readonly category = input.required<string>();
  readonly excludeSlug = input.required<string>();

  protected readonly related = computed(() =>
    BLOG_POSTS
      .filter((p) => p.category === this.category() && p.slug !== this.excludeSlug())
      .slice(0, 3),
  );
}
