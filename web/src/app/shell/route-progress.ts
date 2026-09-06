import { ChangeDetectionStrategy, Component, PLATFORM_ID, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  NavigationCancel, NavigationEnd, NavigationError, NavigationStart, Router,
} from '@angular/router';
import { UiLogoMark } from '../ui/logo-mark';

/** A navigation shorter than this never shows the indicator, so instant
 *  (already-cached) navigations don't flash it. */
const SHOW_AFTER_MS = 120;

/**
 * Indeterminate loading indicator shown while the router is navigating — the
 * lazy page chunks load over the network, so a click in the nav can otherwise
 * sit on the old page with no feedback.
 *
 * It is the spin study from the logo animation: the mark holds still and the
 * ring carries the wait.
 */
@Component({
  selector: 'app-route-progress',
  imports: [UiLogoMark],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (loading()) {
      <div class="route-busy" role="progressbar" aria-label="Loading page">
        <ui-logo-mark mode="spin" [size]="30" />
      </div>
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
