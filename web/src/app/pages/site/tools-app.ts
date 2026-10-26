import {
  ChangeDetectionStrategy, Component, DestroyRef, ElementRef, PLATFORM_ID,
  afterNextRender, computed, inject, signal, viewChild,
} from '@angular/core';
import { NgTemplateOutlet, isPlatformBrowser } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { SitePage } from './site-page';
import { UiIcon, UiIconButton } from '../../ui';
import { AnalyticsService } from '../../core/analytics';

/** Messages the embedded Kotlin/Wasm app posts to this page (same origin only). */
interface ToolMessage {
  source?: string;
  type?: string;
  tool?: string;
  tools?: number;
  reason?: string;
  [key: string]: unknown;
}

/** A GA4 event name the app may ask this page to report; anything else is dropped. */
const EVENT_TYPES = new Set(['calculation_run', 'tool_open', 'tool_close', 'app_ready', 'app_error']);
const HEADER_KEY = 'ta-header';

/**
 * /tools/app — the calculators, running in the browser.
 *
 * The Kotlin/Wasm build of the desktop tool screens lives at /app/ (copied there by
 * `npm run build:app`) and is embedded in an iframe. By default it takes the whole viewport as
 * a fixed layer over the page, with its own slim bar (status, deep link, exit); leaving that
 * layer shows the page underneath: the nav, a header band (full, or compact at the user's
 * choice) with the engine status, a copyable deep link and a control back into the full
 * layer, the frame filling the rest of the first viewport, and the site footer. This is the
 * page's own layer, never the browser's full-screen mode. The hash of this page deep-links
 * into a tool (`/tools/app#des-calculator`); the frame reports back every route change so the
 * address bar stays shareable, and forwards its usage events so they land in the site's GA4
 * stream under the site's consent rules.
 */
@Component({
  selector: 'page-tools-app',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, NgTemplateOutlet, UiIcon, UiIconButton],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-tools-app' },
  template: `
    <div #band class="ta-band">
      @if (compact()) {
        <div class="ta-compact">
          <div class="ta-compact-text">
            <span class="ta-eyebrow">Tools · in the browser</span>
            <span class="ta-compact-sub">Payment cryptography calculators, no install</span>
          </div>
          <ng-container *ngTemplateOutlet="controls" />
        </div>
      } @else {
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
          <div class="ta-head-side">
            <ng-container *ngTemplateOutlet="controls" />
            <span class="ta-req">Needs WebAssembly GC: Chrome/Edge 119+, Firefox 120+, Safari 18.2+</span>
            <a class="ta-prefer" routerLink="/download">Prefer an app? Download ISO8583Studio</a>
          </div>
        </header>
      }
    </div>

    <ng-template #controls>
      <div class="ta-controls">
        @if (tool()) {
          <button type="button" class="ta-route" title="Copy link to this tool" (click)="copyLink()">
            <span class="ta-route-hash">#</span><span class="ta-route-id">{{ tool() }}</span>
            <ui-icon name="link-simple" [size]="13" />
          </button>
        }
        <span class="ta-status lit-capsule" [class.ta-status--off]="status() === 'unsupported'" [class.ta-status--wait]="status() === 'loading'">
          <span class="ta-dot" aria-hidden="true"></span>
          <span class="ta-status-text" role="status">{{ statusText() }}</span>
          <span class="ta-status-tag">WASM</span>
        </span>
        <ui-icon-button icon="arrows-out" label="Fill the window" [size]="30" (click)="enterFullscreen()" />
        @if (compact()) {
          <ui-icon-button icon="arrows-out-simple" label="Show header" [size]="30" (click)="setCompact(false)" />
        } @else {
          <ui-icon-button icon="arrows-in-simple" label="Hide header" [size]="30" (click)="setCompact(true)" />
        }
      </div>
    </ng-template>

    <section class="ta-frame-wrap" aria-label="ISO8583Studio tools"
             [class.ta-frame-wrap--overlay]="overlay()" [class.ta-frame-wrap--ready]="ready()"
             [style.--ta-band.px]="bandHeight()">
      @if (unsupported()) {
        <div class="ta-notice" role="alert">
          <ui-icon name="warning-circle" [size]="22" />
          <div>
            <strong>This browser cannot run the in-browser tools yet.</strong>
            Update to Chrome/Edge 119+, Firefox 120+ or Safari 18.2+, or
            <a routerLink="/download">download the desktop app</a>.
          </div>
        </div>
      }
      @if (full()) {
        <div class="ta-fsbar">
          <span class="ta-fsdots" aria-hidden="true"><i></i><i></i><i></i></span>
          <span class="ta-fstitle">ISO8583Studio tools</span>
          <span class="ta-fsgrow"></span>
          @if (tool()) {
            <button type="button" class="ta-route" title="Copy link to this tool" (click)="copyLink()">
              <span class="ta-route-hash">#</span><span class="ta-route-id">{{ tool() }}</span>
              <ui-icon name="link-simple" [size]="13" />
            </button>
          }
          <span class="ta-status lit-capsule" [class.ta-status--off]="status() === 'unsupported'" [class.ta-status--wait]="status() === 'loading'">
            <span class="ta-dot" aria-hidden="true"></span>
            <span class="ta-status-text">{{ statusText() }}</span>
            <span class="ta-status-tag">WASM</span>
          </span>
          <button type="button" class="btn btn--secondary btn--sm" (click)="exitFullscreen()">
            <ui-icon name="arrows-in" [size]="14" /> Show the page
          </button>
        </div>
      }
      <iframe #frame class="ta-frame" [src]="frameSrc" title="ISO8583Studio tools"
              loading="eager" allow="clipboard-write"></iframe>
    </section>

    @if (toast()) {
      <div class="ta-toast" role="status">
        <ui-icon name="check-circle" [size]="18" />
        <div><b>Link copied</b><span>{{ toast() }}</span></div>
      </div>
    }
  `,
  styles: `
    :host { display: block; --bar-h: var(--nav-h, 72px); }
    .ta-band { padding-top: var(--bar-h); }
    .ta-eyebrow { font-family: var(--mono); font-size: var(--fs-xs); letter-spacing: 0.12em; text-transform: uppercase; color: var(--blue-hi); white-space: nowrap; }
    p.ta-eyebrow { margin: 0 0 5px; }

    /* Full band. */
    .ta-head {
      display: flex; align-items: flex-end; justify-content: space-between; gap: 24px;
      width: 100%; max-width: var(--nav-max, 1300px); margin: 0 auto; box-sizing: border-box;
      padding: 18px var(--gutter, 1rem) 14px;
    }
    .ta-head-text { min-width: 0; }
    .ta-title { margin: 0; font-size: clamp(19px, 1.6vw, 24px); font-weight: 500; letter-spacing: var(--ls-snug); line-height: 1.2; text-wrap: balance; }
    .ta-lede {
      margin: 6px 0 0; max-width: 78ch; font-size: var(--fs-sm); line-height: 1.5; color: var(--muted);
      display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
    }
    .ta-head-side { flex: none; display: flex; flex-direction: column; align-items: flex-end; gap: 7px; text-align: right; max-width: 100%; }
    .ta-req { font-family: var(--mono); font-size: var(--fs-xs); letter-spacing: 0.04em; color: var(--faint); }
    .ta-prefer { font-family: var(--mono); font-size: var(--fs-xs); letter-spacing: 0.04em; color: var(--blue-hi); text-decoration: none; }
    .ta-prefer:hover { color: var(--teal-hi); text-decoration: underline; text-underline-offset: 3px; }

    /* Compact band. */
    .ta-compact {
      display: flex; align-items: center; justify-content: space-between; gap: 16px;
      width: 100%; max-width: var(--nav-max, 1300px); margin: 0 auto; box-sizing: border-box;
      padding: 6px var(--gutter, 1rem) 8px;
    }
    .ta-compact-text { display: flex; align-items: center; gap: 12px; min-width: 0; overflow: hidden; }
    .ta-compact-text .ta-eyebrow { overflow: hidden; text-overflow: ellipsis; }
    .ta-compact-sub { font-size: var(--fs-sm); color: var(--muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

    /* Controls: link chip, status capsule, full screen, header toggle. */
    .ta-controls { display: flex; align-items: center; gap: 8px; min-width: 0; flex: 0 1 auto; }
    .ta-controls > * { flex: none; }
    .ta-controls > .ta-status, .ta-controls > .ta-route { flex: 0 1 auto; min-width: 0; }
    .ta-route {
      display: inline-flex; align-items: center; gap: 7px; height: 30px; min-width: 0; padding: 0 10px 0 11px; margin: 0;
      border: 0; border-radius: var(--r-pill); background: transparent; box-shadow: inset 0 0 0 1px var(--line2); cursor: pointer;
      font: inherit; font-family: var(--mono); font-size: var(--fs-xs); letter-spacing: 0.02em; color: var(--muted);
      transition: color var(--dur) var(--ease), background var(--dur) var(--ease);
    }
    .ta-route:hover { background: var(--neutral-wash); color: var(--text); }
    .ta-route-hash { color: var(--faint); }
    .ta-route-id { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 22ch; }
    .ta-route ui-icon { color: var(--blue-hi); }
    .ta-status { gap: 8px; padding: 6px 12px 6px 10px; font-size: var(--fs-caption); color: var(--text); white-space: nowrap; min-width: 0; --dot: var(--teal-hi); }
    .ta-status--wait { --dot: var(--amber-hi); }
    .ta-status--off { --dot: var(--red-hi); }
    .ta-dot { width: 7px; height: 7px; border-radius: 50%; background: var(--dot); box-shadow: 0 0 10px var(--dot); flex: none; }
    .ta-status--wait .ta-dot { animation: ta-pulse 1.2s ease-in-out infinite; }
    .ta-status-text { overflow: hidden; text-overflow: ellipsis; }
    .ta-status-tag { font-family: var(--mono); font-size: var(--fs-2xs); letter-spacing: 0.1em; color: var(--faint); }

    /* The frame takes the rest of the first viewport, but never less than the hub needs. */
    .ta-frame-wrap {
      position: relative; display: flex; flex-direction: column; background: var(--canvas); border-top: 1px solid var(--line2);
      height: max(660px, calc(100dvh - var(--bar-h) - var(--ta-band, 120px)));
    }
    .ta-frame { display: block; flex: 1 1 auto; min-height: 0; width: 100%; border: 0; background: var(--canvas); }
    /* The full layer: the frame and its bar over everything, the whole viewport. */
    .ta-frame-wrap--overlay { position: fixed; inset: 0; z-index: calc(var(--z-nav) + 1); height: 100dvh; width: 100%; border-top: 0; }
    .ta-fsbar { flex: none; height: 46px; display: flex; align-items: center; gap: 10px; padding: 0 10px 0 14px; background: var(--bg-deep); border-bottom: 1px solid var(--line2); }
    .ta-fsdots { display: inline-flex; gap: 5px; flex: none; }
    .ta-fsdots i { width: 8px; height: 8px; border-radius: 50%; background: #3c4256; }
    .ta-fstitle { font-size: var(--fs-sm); font-weight: 500; letter-spacing: var(--ls-snug); white-space: nowrap; }
    .ta-fsgrow { flex: 1; }
    .ta-notice { flex: none; display: flex; gap: 12px; align-items: flex-start; margin: 12px var(--gutter, 1rem) 0; padding: 14px 16px; border-radius: var(--r-lg); border: 1px solid var(--amber-line); background: var(--amber-wash); color: var(--amber-hi); font-size: var(--fs-sm); line-height: 1.55; }
    .ta-notice ui-icon { flex: none; }
    .ta-notice strong { font-weight: 500; }
    .ta-notice a { color: var(--amber-hi); text-decoration: underline; text-underline-offset: 3px; }

    .ta-toast {
      position: fixed; right: 20px; bottom: 20px; z-index: var(--z-toast); display: flex; gap: 10px; align-items: flex-start; max-width: min(420px, calc(100vw - 40px));
      padding: 12px 14px; border-radius: var(--r-lg); border: 1px solid var(--line); background: var(--surface); color: var(--text); box-shadow: 0 16px 40px rgba(0, 0, 0, .4);
      animation: ds-fade-in .22s var(--ease-out) both;
    }
    .ta-toast ui-icon { color: var(--teal-hi); flex: none; margin-top: 1px; }
    .ta-toast b { display: block; font-weight: 500; font-size: var(--fs-sm); }
    .ta-toast span { display: block; font-family: var(--mono); font-size: var(--fs-xs); color: var(--muted); overflow-wrap: anywhere; }

    @keyframes ta-pulse { 0%, 100% { transform: scale(1); opacity: 1; } 50% { transform: scale(.92); opacity: .7; } }
    @keyframes ds-fade-in { from { opacity: 0; transform: translateY(-4px); } to { opacity: 1; transform: none; } }

    @media (max-width: 860px) { :host { --bar-h: var(--nav-h, 64px); } }
    @media (max-width: 760px) {
      .ta-head { flex-direction: column; align-items: flex-start; gap: 10px; padding: 12px var(--gutter, 1rem) 12px; }
      .ta-lede, .ta-req, .ta-compact-sub { display: none; }
      .ta-head-side { flex-direction: row; align-items: center; flex-wrap: wrap; gap: 12px; text-align: left; }
      .ta-frame-wrap { height: max(560px, calc(100dvh - var(--bar-h) - var(--ta-band, 80px))); }
    }
    @media (max-width: 560px) {
      .ta-route, .ta-status-tag { display: none; }
    }
    @media (max-width: 480px) {
      .ta-prefer, .ta-fstitle { display: none; }
    }
    @media (prefers-reduced-motion: reduce) { .ta-status--wait .ta-dot, .ta-toast { animation: none; } }
  `,
})
export class ToolsAppPage {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly destroyRef = inject(DestroyRef);
  private readonly analytics = inject(AnalyticsService);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly frame = viewChild.required<ElementRef<HTMLIFrameElement>>('frame');
  private readonly band = viewChild.required<ElementRef<HTMLElement>>('band');

  protected readonly ready = signal(false);
  protected readonly unsupported = signal(false);
  protected readonly toolCount = signal(0);
  protected readonly compact = signal(false);
  protected readonly route = signal('');
  protected readonly bandHeight = signal(120);
  protected readonly toast = signal<string | null>(null);
  /** The full layer: on by default, off while the visitor looks at the page around the frame. */
  protected readonly overlay = signal(true);
  protected readonly full = computed(() => this.overlay());
  /** The open tool's id, for the deep-link chip; empty on the hub and on a category view. */
  protected readonly tool = computed(() => { const r = this.route(); return r && !r.startsWith('c/') ? r : ''; });
  protected readonly status = computed<'ready' | 'loading' | 'unsupported'>(() => this.unsupported() ? 'unsupported' : this.ready() ? 'ready' : 'loading');
  protected readonly statusText = computed(() => {
    switch (this.status()) {
      case 'ready': return `Engine ready · ${this.toolCount()} tools`;
      case 'loading': return 'Downloading the calculator engine';
      default: return 'WebAssembly GC unavailable';
    }
  });
  /** Deep link: /tools/app#des-calculator -> /app/#/des-calculator, decided before the frame loads. */
  protected readonly frameSrc: SafeResourceUrl;
  /** Set once the app is up and has been told the page's hash; route reports before that are stale. */
  private synced = false;
  private toastTimer: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    const browser = isPlatformBrowser(this.platformId);
    const initial = browser ? location.hash.replace(/^#\/?/, '') : '';
    this.route.set(initial);
    // Same-origin, fixed prefix and a slug that only ever reaches the hash: safe to trust.
    this.frameSrc = this.sanitizer.bypassSecurityTrustResourceUrl(initial ? `/app/#/${initial}` : '/app/');
    if (browser) {
      try { this.compact.set(localStorage.getItem(HEADER_KEY) === 'compact'); } catch { /* storage blocked */ }
    }
    afterNextRender(() => this.wire());
  }

  protected setCompact(on: boolean): void {
    this.compact.set(on);
    try { localStorage.setItem(HEADER_KEY, on ? 'compact' : 'full'); } catch { /* storage blocked */ }
  }

  protected copyLink(): void {
    const url = `${location.origin}${location.pathname}#${this.tool()}`;
    const done = () => {
      this.toast.set(url);
      if (this.toastTimer) clearTimeout(this.toastTimer);
      this.toastTimer = setTimeout(() => this.toast.set(null), 3200);
    };
    if (navigator.clipboard) navigator.clipboard.writeText(url).then(done, done); else done();
  }

  /** The frame as a layer over the whole viewport: the page's own layer, never the browser's full-screen mode. */
  protected enterFullscreen(): void { this.setOverlay(true); }

  protected exitFullscreen(): void { this.setOverlay(false); }

  private setOverlay(on: boolean): void {
    this.overlay.set(on);
    if (isPlatformBrowser(this.platformId)) {
      document.body.style.overflow = on ? 'hidden' : '';
      this.frame().nativeElement.contentWindow?.postMessage({ source: 'iso8583-site', type: 'fullscreen', on }, location.origin);
    }
  }

  private wire(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    const frame = this.frame().nativeElement;
    const tell = (msg: Record<string, unknown>) =>
      frame.contentWindow?.postMessage({ source: 'iso8583-site', ...msg }, location.origin);
    const pushHash = () => {
      const tool = location.hash.replace(/^#\/?/, '');
      this.route.set(tool);
      tell({ type: 'navigate', tool });
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
          this.route.set(tool);
          this.analytics.toolRouteChanged(tool || 'home');
          return;
        }
        case 'unsupported':
          this.unsupported.set(true);
          this.analytics.reportToolEvent('app_error', { reason: 'wasm_gc' });
          return;
        case 'app_ready':
          this.ready.set(true);
          if (typeof msg.tools === 'number') this.toolCount.set(msg.tools);
          // The app started from whatever hash its frame had; make sure it matches the page.
          pushHash();
          tell({ type: 'fullscreen', on: this.full() });
          this.synced = true;
          break;
      }
      if (EVENT_TYPES.has(msg.type)) {
        const { source: _s, type: _t, ...params } = msg;
        this.analytics.reportToolEvent(msg.type, params);
      }
    };
    window.addEventListener('message', onMessage);

    // Browser back/forward on this page, and any in-page hash change (a site link to a tool
    // while this page is already open): push the new hash down into the frame.
    const onPop = () => pushHash();
    window.addEventListener('popstate', onPop);
    window.addEventListener('hashchange', onPop);

    // Escape leaves the full layer; the page under it does not scroll while it is up.
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape' && this.overlay()) this.setOverlay(false); };
    window.addEventListener('keydown', onKey);
    document.body.style.overflow = this.overlay() ? 'hidden' : '';

    // The band's height sets the frame's: measure it, and again whenever it changes.
    const band = this.band().nativeElement;
    const fit = () => this.bandHeight.set(band.offsetHeight);
    fit();
    const ro = typeof ResizeObserver !== 'undefined' ? new ResizeObserver(fit) : null;
    if (ro) ro.observe(band); else window.addEventListener('resize', fit);

    this.destroyRef.onDestroy(() => {
      window.removeEventListener('message', onMessage);
      window.removeEventListener('popstate', onPop);
      window.removeEventListener('hashchange', onPop);
      window.removeEventListener('keydown', onKey);
      document.body.style.overflow = '';
      if (ro) ro.disconnect(); else window.removeEventListener('resize', fit);
      if (this.toastTimer) clearTimeout(this.toastTimer);
    });
  }
}
