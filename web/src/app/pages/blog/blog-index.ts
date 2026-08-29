import { ChangeDetectionStrategy, Component, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BLOG_CATEGORIES, BLOG_POSTS } from '../../content/blog-index';
import { UiTag } from '../../ui/tag';

@Component({
  selector: 'app-blog-index',
  imports: [RouterLink, UiTag],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="blog-hero">
      <div class="blog-hero-in">
        <h1 class="blog-hero-title">ISO8583Studio Blog</h1>
        <p class="page-description" style="margin:0 auto">
          Expert guides on payment testing, ISO 8583, HSM simulation, EMV tools,
          cryptography, and fintech development.
        </p>
      </div>
    </header>

    <div class="blog-filters" role="group" aria-label="Filter posts by category">
      <button class="filter-btn" type="button" [attr.aria-pressed]="active() === null"
              (click)="active.set(null)">All</button>
      @for (cat of categories; track cat) {
        <button class="filter-btn" type="button" [attr.aria-pressed]="active() === cat"
                (click)="active.set(cat)">{{ cat }}</button>
      }
    </div>

    <div class="blog-index-grid">
      @for (post of visible(); track post.slug) {
        <a class="card card--post card--interactive" [routerLink]="post.path">
          @if (post.image) {
            <img class="post-thumb" [src]="post.image" alt="" width="1376" height="768"
                 loading="lazy" decoding="async">
          }
          <span class="ui-card-eyebrow">{{ post.category }}</span>
          <span class="ui-card-title">{{ post.title }}</span>
          <span class="ui-card-desc">{{ post.description }}</span>
          <span class="tag-row">
            @for (tag of post.tags.slice(0, 4); track tag) { <ui-tag>{{ tag }}</ui-tag> }
          </span>
          <span class="ui-card-meta"><span>{{ post.date }}</span><span>{{ post.readTime }}</span></span>
        </a>
      } @empty {
        <p>No posts in this category yet.</p>
      }
    </div>
  `,
})
export class BlogIndex {
  protected readonly categories = BLOG_CATEGORIES;
  protected readonly active = signal<string | null>(null);

  protected readonly visible = computed(() => {
    const cat = this.active();
    return cat ? BLOG_POSTS.filter((p) => p.category === cat) : BLOG_POSTS;
  });
}
