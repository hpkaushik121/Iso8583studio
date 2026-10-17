import {
  ChangeDetectionStrategy, Component, DestroyRef, ElementRef, PLATFORM_ID,
  afterNextRender, inject, signal, viewChild,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { SitePage } from './site-page';
import { UiIcon } from '../../ui';
import { AnalyticsService } from '../../core/analytics';

/** Messages the embedded Kotlin/Wasm app posts to this page (same origin only). */
interface ToolMessage {
  source?: string;
  type?: string;
  tool?: string;
  reason?: string;
  [key: string]: unknown;
}

/** A GA4 event name the app may ask this page to report; anything else is dropped. */
const EVENT_TYPES = new Set(['calculation_run', 'tool_open', 'tool_close', 'app_ready', 'app_error']);

/**
 * /tools/app — the calculators, running in the browser.
 *
 * The Kotlin/Wasm build of the desktop tool screens lives at /app/ (copied there by
 * `npm run build:app`) and is embedded in an iframe: the canvas-driven app owns the whole
 * document, so the site's stylesheet, fonts and scripts stay out of its way. The hash of this
 * page deep-links into a tool (`/tools/app#des-calculator`); the frame reports back every
 * route change so the address bar stays shareable, and forwards its usage events so they
 * land in the site's GA4 stream under the site's consent rules.
 */
@Component({
  selector: 'page-tools-app',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, UiIcon],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-tools-app' },
  template: `
    <section class="ta-intro">
      <div class="ta-intro-text">
        <p class="ta-eyebrow">Tools · in the browser</p>
        <h1 class="ta-title">Payment cryptography calculators, no install</h1>
        <p class="ta-lede">
          The same DES/3DES, DUKPT, TR-31, key-component, PIN-block and parser tools as the desktop
          app, compiled to WebAssembly from the same Kotlin code. Keys and card data stay on this page:
          nothing you type is sent anywhere.
        </p>
        <p class="ta-meta">
          Needs a browser with WebAssembly GC: Chrome/Edge 119+, Firefox 120+, Safari 18.2+.
          Best on a desktop-sized window.
          Prefer an app? <a routerLink="/download">Download ISO8583Studio</a>.
        </p>
      </div>
    </section>

    <section class="ta-frame-wrap" [class.ta-frame-wrap--ready]="ready()">
      @if (unsupported()) {
        <div class="ta-notice">
          <ui-icon name="warning-circle" />
          <div>
            <strong>This browser cannot run the in-browser tools yet.</strong>
            Update to Chrome/Edge 119+, Firefox 120+ or Safari 18.2+, or
            <a routerLink="/download">download the desktop app</a>.
          </div>
        </div>
      }
      <iframe #frame class="ta-frame" [src]="frameSrc" title="ISO8583Studio tools"
              loading="eager" allow="clipboard-write"></iframe>
    </section>
  `,
  styles: `
    :host { display: block; }
    .ta-intro { padding: calc(var(--nav-h, 72px) + var(--sp-8, 2rem)) var(--gutter, 1rem) var(--sp-4, 1rem); max-width: var(--nav-max, 1300px); margin: 0 auto; }
    .ta-eyebrow { font-family: var(--font-mono, monospace); font-size: 0.8rem; letter-spacing: 0.08em; text-transform: uppercase; color: var(--c-accent, inherit); margin: 0 0 0.5rem; }
    .ta-title { font-size: clamp(1.6rem, 3vw, 2.4rem); line-height: 1.15; margin: 0 0 0.75rem; }
    .ta-lede { font-size: 1.05rem; line-height: 1.6; margin: 0 0 0.5rem; max-width: 60ch; }
    .ta-meta { font-size: 0.9rem; opacity: 0.8; margin: 0; }
    .ta-frame-wrap { position: relative; width: 100%; height: calc(100dvh - var(--header-h, 64px)); min-height: 560px; }
    .ta-frame { display: block; width: 100%; height: 100%; border: 0; background: transparent; }
    .ta-notice { display: flex; gap: 0.75rem; align-items: flex-start; margin: 0 var(--gutter, 1rem) 1rem; padding: 1rem; border-radius: 12px; border: 1px solid var(--c-border, currentColor); }
    .ta-notice ui-icon { flex: none; width: 22px; height: 22px; }
  `,
})
export class ToolsAppPage {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly destroyRef = inject(DestroyRef);
  private readonly analytics = inject(AnalyticsService);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly frame = viewChild.required<ElementRef<HTMLIFrameElement>>('frame');

  protected readonly ready = signal(false);
  protected readonly unsupported = signal(false);
  /** Deep link: /tools/app#des-calculator -> /app/#/des-calculator, decided before the frame loads. */
  protected readonly frameSrc: SafeResourceUrl;
  /** Set once the app is up and has been told the page's hash; route reports before that are stale. */
  private synced = false;

  constructor() {
    const initial = isPlatformBrowser(this.platformId) ? location.hash.replace(/^#\/?/, '') : '';
    // Same-origin, fixed prefix and a slug that only ever reaches the hash: safe to trust.
    this.frameSrc = this.sanitizer.bypassSecurityTrustResourceUrl(initial ? `/app/#/${initial}` : '/app/');
    afterNextRender(() => this.wire());
  }

  private wire(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    const frame = this.frame().nativeElement;
    const pushHash = () => {
      const tool = location.hash.replace(/^#\/?/, '');
      frame.contentWindow?.postMessage({ source: 'iso8583-site', type: 'navigate', tool }, location.origin);
    };

    const onMessage = (ev: MessageEvent<ToolMessage>) => {
      if (ev.origin !== location.origin || ev.source !== frame.contentWindow) return;
      const msg = ev.data;
      if (!msg || msg.source !== 'iso8583-tools' || typeof msg.type !== 'string') return;

      switch (msg.type) {
        case 'route': {
          if (!this.synced) return;
          const tool = typeof msg.tool === 'string' ? msg.tool : '';
          const hash = tool ? `#${tool}` : '';
          if (location.hash !== hash) {
            history.replaceState(history.state, '', `${location.pathname}${location.search}${hash}`);
          }
          this.analytics.toolRouteChanged(tool || 'home');
          return;
        }
        case 'unsupported':
          this.unsupported.set(true);
          this.analytics.reportToolEvent('app_error', { reason: 'wasm_gc' });
          return;
        case 'app_ready':
          this.ready.set(true);
          // The app started from whatever hash its frame had; make sure it matches the page.
          pushHash();
          this.synced = true;
          break;
      }
      if (EVENT_TYPES.has(msg.type)) {
        const { source: _s, type: _t, ...params } = msg;
        this.analytics.reportToolEvent(msg.type, params);
      }
    };
    window.addEventListener('message', onMessage);

    // Browser back/forward on this page: push the new hash down into the frame.
    const onPop = () => pushHash();
    window.addEventListener('popstate', onPop);

    this.destroyRef.onDestroy(() => {
      window.removeEventListener('message', onMessage);
      window.removeEventListener('popstate', onPop);
    });
  }
}
