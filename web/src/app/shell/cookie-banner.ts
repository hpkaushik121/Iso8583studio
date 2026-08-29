import {
  ChangeDetectionStrategy, Component, DOCUMENT, PLATFORM_ID, inject, signal,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AnalyticsService } from '../core/analytics';

/** Same storage key as the previous implementation, so visitors who already
 *  chose are not asked again. */
const KEY = 'iso8583-cookie-consent';

@Component({
  selector: 'app-cookie-banner',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (visible()) {
      <div class="cookie-bar" [class.out]="leaving()" role="region" aria-label="Cookie consent">
        <p>We use cookies to remember your preferences and to measure how the site is used.
           See our <a routerLink="/privacy-policy">Privacy Policy</a>.</p>
        <div class="cb-actions">
          <button class="cb-decline" type="button" (click)="choose('essential')">Essential only</button>
          <button class="btn btn--primary cb-accept" type="button" (click)="choose('all')">Accept all</button>
        </div>
      </div>
    }
  `,
})
export class CookieBanner {
  protected readonly visible = signal(false);
  protected readonly leaving = signal(false);

  private readonly doc = inject(DOCUMENT);
  private readonly analytics = inject(AnalyticsService);
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  constructor() {
    if (!this.isBrowser) return;
    try {
      if (this.doc.defaultView?.localStorage.getItem(KEY)) return;
    } catch { /* private mode: show the banner */ }
    this.visible.set(true);
    this.analytics.reportCookieBannerShown();
  }

  protected choose(value: 'all' | 'essential'): void {
    try { this.doc.defaultView?.localStorage.setItem(KEY, value); } catch { /* ignore */ }
    this.leaving.set(true);
    setTimeout(() => this.visible.set(false), 220);
  }
}
