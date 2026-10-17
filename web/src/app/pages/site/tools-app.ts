import {
  ChangeDetectionStrategy, Component, DestroyRef, ElementRef, PLATFORM_ID, Renderer2,
  afterNextRender, inject, signal, viewChild,
} from '@angular/core';
import { DOCUMENT, isPlatformBrowser } from '@angular/common';
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
    <header class="ta-head">
      <div class="ta-head-text">
        <p class="ta-eyebrow">Tools · in the browser</p>
        <h1 class="ta-title">Payment cryptography calculators, no install</h1>
        <p class="ta-lede">
          The same DES/3DES, DUKPT, TR-31, key-component, PIN-block and EMV tools as the desktop
          app, compiled to WebAssembly from the same Kotlin code. Keys and card data stay on this page:
          nothing you type is sent anywhere.
        </p>
      </div>
      <p class="ta-meta">
        <span class="ta-meta-req">Needs WebAssembly GC: Chrome/Edge 119+, Firefox 120+, Safari 18.2+</span>
        <a routerLink="/download">Prefer an app? Download ISO8583Studio</a>
      </p>
    </header>

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
    /* An app shell, not a document: the header is one compact band and the frame takes every
       remaining pixel under the fixed nav, so the page itself never scrolls (html.app-shell in
       _base.css locks the document and drops the footer). */
    :host {
      display: grid; grid-template-rows: auto minmax(0, 1fr);
      height: 100dvh; box-sizing: border-box; padding-top: var(--nav-h, 72px);
    }
    .ta-head {
      display: flex; align-items: flex-end; justify-content: space-between; gap: var(--sp-6, 1.5rem);
      width: 100%; max-width: var(--nav-max, 1300px); margin: 0 auto; box-sizing: border-box;
      padding: var(--sp-5, 1.25rem) var(--gutter, 1rem) var(--sp-3, 0.75rem);
    }
    .ta-head-text { min-width: 0; }
    .ta-eyebrow { font-family: var(--mono); font-size: 0.72rem; letter-spacing: 0.12em; text-transform: uppercase; color: var(--blue-hi); margin: 0 0 0.3rem; }
    .ta-title { font-size: clamp(1.15rem, 1.6vw, 1.5rem); line-height: 1.2; letter-spacing: -0.01em; margin: 0; text-wrap: balance; }
    .ta-lede {
      font-size: 0.92rem; line-height: 1.5; color: var(--muted); margin: 0.35rem 0 0; max-width: 78ch;
      display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
    }
    .ta-meta {
      flex: none; display: flex; flex-direction: column; align-items: flex-end; gap: 0.25rem; margin: 0 0 0.15rem;
      font-family: var(--mono); font-size: 0.72rem; letter-spacing: 0.04em; color: var(--faint); text-align: right;
    }
    .ta-meta a { color: var(--blue-hi); text-decoration: none; }
    .ta-meta a:hover { text-decoration: underline; }
    .ta-frame-wrap { position: relative; display: flex; flex-direction: column; min-height: 0; background: var(--canvas); }
    .ta-frame { display: block; flex: 1 1 auto; min-height: 0; width: 100%; border: 0; background: var(--canvas); }
    .ta-notice { flex: none; display: flex; gap: 0.75rem; align-items: flex-start; margin: 0 var(--gutter, 1rem) 1rem; padding: 1rem; border-radius: 12px; border: 1px solid var(--amber-line); background: var(--amber-wash); color: var(--amber-hi); }
    .ta-notice ui-icon { flex: none; width: 22px; height: 22px; }
    @media (max-width: 760px) {
      .ta-head { flex-direction: column; align-items: flex-start; gap: 0.35rem; padding-top: var(--sp-4, 1rem); }
      .ta-lede, .ta-meta-req { display: none; }
      .ta-meta { flex-direction: row; align-items: center; text-align: left; margin: 0; }
    }
    @media (max-width: 480px) {
      .ta-eyebrow { margin-bottom: 0.15rem; }
      .ta-meta { display: none; }
    }
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
    // The document becomes an app shell while this page is up: no page scroll, no footer.
    const doc = inject(DOCUMENT);
    const renderer = inject(Renderer2);
    renderer.addClass(doc.documentElement, 'app-shell');
    this.destroyRef.onDestroy(() => renderer.removeClass(doc.documentElement, 'app-shell'));

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
