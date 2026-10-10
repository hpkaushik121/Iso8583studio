import {
  ChangeDetectionStrategy, Component, ElementRef, afterNextRender, computed, input, signal,
  viewChild,
} from '@angular/core';
import { UiIcon } from '../../ui';

/**
 * A post's 16:9 cover. When the image is missing or fails to load, the topic's
 * icon on a soft glow stands in for it, so a card never shows a broken image.
 */
@Component({
  selector: 'app-blog-cover',
  imports: [UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'blog-cover' },
  template: `
    @if (broken()) {
      <span class="blog-cover-fallback" aria-hidden="true"><ui-icon [name]="icon()" [size]="30" /></span>
    } @else {
      <img #img [src]="src()" [attr.srcset]="srcset()" [attr.sizes]="srcset() ? sizes() : null"
           [alt]="alt()" width="640" height="360" decoding="async"
           [attr.loading]="eager() ? 'eager' : 'lazy'" (error)="failed.set(true)">
    }
  `,
})
export class BlogCover {
  /** The 640px thumbnail. */
  readonly src = input.required<string | null>();
  /** The full-size image, offered as a second candidate where the cover is drawn large. */
  readonly full = input<string | null>(null);
  readonly sizes = input('640px');
  /** Phosphor icon of the post's topic, for the fallback. */
  readonly icon = input('article');
  readonly eager = input(false);
  /** Describes the cover; pass the post title. Empty keeps it decorative. */
  readonly alt = input('');

  protected readonly failed = signal(false);
  protected readonly broken = computed(() => this.failed() || !this.src());
  protected readonly srcset = computed(() => {
    const src = this.src(), full = this.full();
    return src && full ? `${src} 640w, ${full} 1376w` : null;
  });

  private readonly img = viewChild<ElementRef<HTMLImageElement>>('img');

  constructor() {
    // An image that failed before hydration never delivers its error event.
    afterNextRender(() => {
      const img = this.img()?.nativeElement;
      if (img && img.complete && img.naturalWidth === 0) this.failed.set(true);
    });
  }
}
