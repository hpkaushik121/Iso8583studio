import { ChangeDetectionStrategy, Component, PLATFORM_ID, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  NavigationCancel, NavigationEnd, NavigationError, NavigationStart, Router,
} from '@angular/router';

/** A navigation shorter than this never shows the bar, so instant
 *  (already-cached) navigations don't flash it. */
const SHOW_AFTER_MS = 120;

/**
 * Indeterminate progress bar pinned to the top of the viewport while the
 * router is navigating — the lazy page chunks load over the network, so a
 * click in the nav can otherwise sit on the old page with no feedback.
 */
@Component({
  selector: 'app-route-progress',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (loading()) {
      <div class="route-progress" role="progressbar" aria-label="Loading page"></div>
    }
  `,
})
export class RouteProgress {
  protected readonly loading = signal(false);
  private showTimer: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    // The prerender renders each page in a settled state; only a live browser
    // ever navigates.
    if (!isPlatformBrowser(inject(PLATFORM_ID))) return;
    inject(Router).events.pipe(takeUntilDestroyed()).subscribe((event) => {
      if (event instanceof NavigationStart) {
        this.showTimer ??= setTimeout(() => this.loading.set(true), SHOW_AFTER_MS);
      } else if (event instanceof NavigationEnd
        || event instanceof NavigationCancel
        || event instanceof NavigationError) {
        if (this.showTimer !== null) { clearTimeout(this.showTimer); this.showTimer = null; }
        this.loading.set(false);
      }
    });
  }
}
