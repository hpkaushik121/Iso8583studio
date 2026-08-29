import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { DomSanitizer } from '@angular/platform-browser';
import { map } from 'rxjs/operators';
import { BLOG_POSTS } from '../../content/blog-index';
import { UiTag } from '../../ui/tag';
import { UiButton } from '../../ui/button';
import { RelatedPosts } from './related-posts';
import { EXTERNAL } from '../../core/site-nav';
import type { BlogMeta } from './blog-meta';

@Component({
  selector: 'app-blog-post',
  imports: [UiTag, UiButton, RelatedPosts],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (post(); as p) {
      <article>
        <header class="blog-hero">
          <div class="blog-hero-in">
            <span class="ui-card-eyebrow">{{ p.category }}</span>
            <h1 class="blog-hero-title">{{ p.title }}</h1>
            <div class="blog-meta">
              <span class="blog-meta-item">📅 {{ p.date }}</span>
              <span class="blog-meta-item">⏱ {{ p.readTime }}</span>
              <span class="blog-meta-item">✎ {{ p.author }}</span>
            </div>
            <div class="tag-row">
              @for (tag of p.tags; track tag) { <ui-tag>{{ tag }}</ui-tag> }
            </div>
          </div>
        </header>

        <div class="blog-body">
          <div class="prose" [innerHTML]="body()"></div>

          <div class="blog-cta prose">
            <h3>Try ISO8583Studio Today</h3>
            <p>Download the free desktop application for Windows, macOS, and Linux.</p>
            <ui-button [href]="releases">Download Free →</ui-button>
          </div>

          <app-related-posts [category]="p.category" [excludeSlug]="p.slug" />
        </div>
      </article>
    }
  `,
})
export class BlogPost {
  private readonly route = inject(ActivatedRoute);
  private readonly sanitizer = inject(DomSanitizer);

  protected readonly releases = EXTERNAL.releases;

  private readonly data = toSignal(this.route.data, { requireSync: true });

  protected readonly post = computed<BlogMeta | undefined>(() =>
    BLOG_POSTS.find((p) => p.slug === this.data()['slug']),
  );

  /**
   * The HTML is produced at build time by our own Markdown renderer, never by
   * a user, so bypassing the sanitizer is safe here — and necessary, because
   * the sanitizer strips the heading ids that in-page anchors rely on.
   */
  protected readonly body = computed(() =>
    this.sanitizer.bypassSecurityTrustHtml((this.data()['html'] as string) ?? ''),
  );
}
