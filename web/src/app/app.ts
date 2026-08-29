import { ChangeDetectionStrategy, Component, PLATFORM_ID, inject } from '@angular/core';
import { DOCUMENT, isPlatformBrowser, ViewportScroller } from '@angular/common';
import { RouterLink, RouterOutlet } from '@angular/router';
import { Header } from './shell/header';
import { Footer } from './shell/footer';
import { CookieBanner } from './shell/cookie-banner';
import { ProDownloadModal } from './shell/pro-download-modal';
import { SeoService } from './core/seo';
import { AnalyticsService } from './core/analytics';

/** Height of the fixed header plus a little breathing room — keep in step with
 *  --nav-h and --sp-5 in _tokens.css. */
const SCROLL_OFFSET = 84;

@Component({
  selector: 'app-root',
  imports: [RouterLink, RouterOutlet, Header, Footer, CookieBanner, ProDownloadModal],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './app.html',
})
export class App {
  private readonly doc = inject(DOCUMENT);

  /** Moves the keyboard to the content, not just the viewport. */
  protected focusMain(): void {
    this.doc.getElementById('main')?.focus({ preventScroll: true });
  }

  constructor() {
    // SEO runs on the server too, so metadata lands in the prerendered file.
    inject(SeoService).init();
    // Analytics is a no-op outside the browser.
    inject(AnalyticsService).init();

    if (isPlatformBrowser(inject(PLATFORM_ID))) {
      /* The router scrolls to anchors by computing an absolute position, so it
         ignores scroll-margin-top and would drop the target behind the fixed
         header. This is the only way to tell it about that header. */
      inject(ViewportScroller).setOffset([0, SCROLL_OFFSET]);
    }
  }
}
