import { DOCUMENT, Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';

/**
 * GA4 + Google Ads tagging, ported from docs/assets/analytics.js.
 *
 * The event taxonomy, identity keys and GA4 length caps are carried over
 * unchanged so historical reports stay comparable. Three things had to change
 * for a routed app:
 *
 *  1. Everything is browser-only. Prerendering has no window/localStorage.
 *  2. gtag is configured with send_page_view:false and page_view is fired per
 *     NavigationEnd — otherwise a session reports exactly one pageview.
 *  3. Page dimensions are re-derived per navigation from the router URL, which
 *     is now extensionless.
 */

const MEASUREMENT_ID: string = 'G-445XQ0W2Q4';
// Google Ads, e.g. 'AW-123456789'. Empty disables the Ads tag entirely.
const ADS_ID: string = '';
// Download conversion label, e.g. 'AbC-D_efG'.
const ADS_CONVERSION: string = '';

const LOCAL_HOSTS = ['localhost', '127.0.0.1', '::1', '[::1]', '0.0.0.0', ''];
const SIMULATOR_DOCS = /^(host|hsm|apdu|pos|atm|ecr|issuer|payment-switch|hsm-command-console)/;
const GUIDE_DOCS = ['installation', 'versions', 'contributing'];
const SOLUTION_PAGES = ['emv-certification', 'cloud-simulators', 'kernel', 'middleware'];
const DOWNLOAD_FILE = /\.(dmg|msi|exe|deb|rpm|jar|zip)(\?|#|$)/i;
const RELEASE_LINK = /releases\/(latest|download)/i;
const SCROLL_MARKS = [25, 50, 75, 90];

interface PageInfo { group: string; id: string; }

declare global {
  interface Window { dataLayer?: unknown[]; gtag?: (...args: unknown[]) => void; }
}

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private readonly doc = inject(DOCUMENT);
  private readonly router = inject(Router);
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  private enabled = false;
  private adsEnabled = false;
  private page: PageInfo = { group: 'other', id: '' };
  /** Reset on every navigation so once-per-page events fire once per view. */
  private fired = new Set<string>();
  private visitorId = '';
  private win!: Window & typeof globalThis;

  init(): void {
    if (!this.isBrowser) return;
    this.win = this.doc.defaultView as Window & typeof globalThis;
    if (!this.win) return;

    const host = this.win.location.hostname;
    if (
      this.win.location.protocol === 'file:' ||
      LOCAL_HOSTS.includes(host) ||
      /\.local$|\.test$|\.localhost$/.test(host) ||
      !MEASUREMENT_ID ||
      MEASUREMENT_ID.includes('XXXX')
    ) {
      return;
    }

    this.enabled = true;
    this.adsEnabled = !!ADS_ID && !ADS_ID.includes('XXXX');
    this.bootstrap();
    this.observeNavigation();
    this.observeClicks();
    this.observeScroll();
  }

  // ---------------------------------------------------------------- utilities

  private clamp(v: unknown): string | undefined {
    if (v === null || v === undefined) return undefined;
    const s = String(v);
    if (s === '') return undefined;
    return s.length > 36 ? s.slice(0, 36) : s;
  }

  private compact(obj: Record<string, unknown>): Record<string, unknown> {
    const out: Record<string, unknown> = {};
    for (const k of Object.keys(obj)) if (obj[k] !== undefined) out[k] = obj[k];
    return out;
  }

  private lsGet(key: string): string | null {
    try { return this.win.localStorage.getItem(key); } catch { return null; }
  }

  private lsSet(key: string, value: string): void {
    try { this.win.localStorage.setItem(key, value); } catch { /* private mode */ }
  }

  private uuid(): string {
    try {
      if (this.win.crypto?.randomUUID) return this.win.crypto.randomUUID();
    } catch { /* fall through */ }
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
      const r = (Math.random() * 16) | 0;
      return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16);
    });
  }

  private param(name: string): string {
    try { return new URLSearchParams(this.win.location.search).get(name) || ''; } catch { return ''; }
  }

  private text(el: Element | null): string {
    if (!el) return '';
    return (el.textContent || '').trim().replace(/\s+/g, ' ').slice(0, 100);
  }

  private trim100(v: unknown): string {
    return String(v ?? '').slice(0, 100);
  }

  // ----------------------------------------------------------- page dimensions

  /** Derived from the path, which no longer carries .html. */
  private pageInfo(url: string): PageInfo {
    const path = url.split('#')[0].split('?')[0];
    const seg = path.split('/').filter(Boolean).map((s) => s.replace(/\.html$/, ''));

    if (seg.length === 0) return { group: 'home', id: 'home' };
    const slug = seg[seg.length - 1];

    if (seg[0] === 'blogs') {
      return seg.length === 1 ? { group: 'blog_index', id: 'index' } : { group: 'blog', id: slug };
    }

    if (seg[0] === 'docs') {
      if (seg.length === 1) return { group: 'docs_index', id: 'index' };
      if (GUIDE_DOCS.includes(seg[1])) return { group: 'docs_guide', id: seg[1] };
      if (seg[1] === 'payment-simulators' || SIMULATOR_DOCS.test(seg[1])) {
        return { group: 'docs_simulator', id: seg[1] };
      }
      return { group: 'docs_tool', id: seg[1] };
    }

    if (/^(privacy-policy|terms-and-conditions)$/.test(slug)) return { group: 'legal', id: slug };
    if (SOLUTION_PAGES.includes(seg[0])) return { group: 'solution', id: seg[0] };
    if (seg[0] === 'pro') return { group: 'pro', id: 'pro' };
    if (seg[0] === 'contact') return { group: 'contact', id: 'contact' };
    return { group: 'other', id: slug || seg[0] };
  }

  // ------------------------------------------------------------------ identity

  private firstTouch(): Record<string, string> {
    const stored = this.lsGet('iso8583_first');
    if (stored) {
      try { return JSON.parse(stored); } catch { /* rewrite below */ }
    }
    const ft = {
      page: this.win.location.pathname,
      ref: this.doc.referrer || '(direct)',
      date: new Date().toISOString().slice(0, 10),
      gclid: this.param('gclid'),
      gbraid: this.param('gbraid'),
      wbraid: this.param('wbraid'),
      src: this.param('utm_source'),
      med: this.param('utm_medium'),
      cmp: this.param('utm_campaign'),
    };
    this.lsSet('iso8583_first', JSON.stringify(ft));
    return ft;
  }

  private environment(ft: Record<string, string>, visits: number): Record<string, unknown> {
    const nav = this.win.navigator as Navigator & {
      connection?: { effectiveType?: string };
      deviceMemory?: number;
    };
    const conn = nav.connection || {};

    let scheme = 'unknown';
    try {
      if (this.win.matchMedia) {
        scheme = this.win.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
      }
    } catch { /* ignore */ }

    let tz = '';
    try { tz = Intl.DateTimeFormat().resolvedOptions().timeZone || ''; } catch { /* ignore */ }

    return {
      // The desktop app reports into the same GA4 property; this keeps the two
      // separable in every report.
      stream_source: 'website',
      browser_language: this.clamp(nav.language),
      timezone: this.clamp(tz),
      screen_resolution: this.clamp(`${this.win.screen.width}x${this.win.screen.height}`),
      viewport_size: this.clamp(`${this.win.innerWidth}x${this.win.innerHeight}`),
      device_pixel_ratio: this.clamp(this.win.devicePixelRatio),
      color_scheme: this.clamp(scheme),
      device_memory: this.clamp(nav.deviceMemory),
      hardware_concurrency: this.clamp(nav.hardwareConcurrency),
      connection_type: this.clamp(conn.effectiveType),
      platform: this.clamp(nav.platform),
      visit_count: this.clamp(visits),
      first_landing_page: this.clamp(ft['page']),
      first_referrer: this.clamp(ft['ref']),
      first_seen_date: this.clamp(ft['date']),
      first_gclid: this.clamp(ft['gclid']),
      first_gbraid: this.clamp(ft['gbraid']),
      first_wbraid: this.clamp(ft['wbraid']),
      first_utm_source: this.clamp(ft['src']),
      first_utm_medium: this.clamp(ft['med']),
      first_utm_campaign: this.clamp(ft['cmp']),
    };
  }

  // ----------------------------------------------------------------- bootstrap

  private gtag(...args: unknown[]): void {
    // gtag.js only interprets a dataLayer entry as a command when it is a real
    // `arguments` object; a plain array is pushed and then ignored, so every
    // js/set/config/event silently never reaches GA. Hence the apply().
    const dl = (this.win.dataLayer ||= []);
    (function () { dl.push(arguments); }).apply(null, args as []);
  }

  private bootstrap(): void {
    this.win.dataLayer ||= [];
    this.win.gtag = (...args: unknown[]) => this.gtag(...args);

    const s = this.doc.createElement('script');
    s.async = true;
    s.src = `https://www.googletagmanager.com/gtag/js?id=${encodeURIComponent(MEASUREMENT_ID)}`;
    (this.doc.head || this.doc.documentElement).appendChild(s);

    this.visitorId = this.lsGet('iso8583_uid') || '';
    if (!this.visitorId) {
      this.visitorId = this.uuid();
      this.lsSet('iso8583_uid', this.visitorId);
    }

    const ft = this.firstTouch();
    const visits = parseInt(this.lsGet('iso8583_visits') || '0', 10) + 1;
    this.lsSet('iso8583_visits', String(visits));

    this.page = this.pageInfo(this.router.url);

    this.gtag('js', new Date());
    this.gtag('set', 'user_properties', this.compact(this.environment(ft, visits)));
    // send_page_view is off because the router, not the document load, decides
    // when a page has been viewed.
    this.gtag('config', MEASUREMENT_ID, {
      user_id: this.visitorId,
      send_page_view: false,
      page_group: this.page.group,
      content_id: this.page.id,
    });

    if (this.adsEnabled) {
      this.gtag('config', ADS_ID, { allow_enhanced_conversions: false });
    }
  }

  // ---------------------------------------------------------------- reporting

  private track(name: string, params: Record<string, unknown> = {}): void {
    if (!this.enabled) return;
    const p: Record<string, unknown> = { page_group: this.page.group, content_id: this.page.id };
    for (const k of Object.keys(params)) {
      if (params[k] !== undefined && params[k] !== '') p[k] = params[k];
    }
    this.gtag('event', name, p);
  }

  private trackOnce(key: string, name: string, params: Record<string, unknown> = {}): void {
    if (this.fired.has(key)) return;
    this.fired.add(key);
    this.track(name, params);
  }

  private observeNavigation(): void {
    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe((e) => {
        this.page = this.pageInfo(e.urlAfterRedirects);
        // A new view starts a new once-per-page budget.
        this.fired.clear();
        this.gtag('event', 'page_view', {
          page_location: this.win.location.href,
          page_path: e.urlAfterRedirects,
          page_title: this.doc.title,
          page_group: this.page.group,
          content_id: this.page.id,
        });
      });
  }

  // -------------------------------------------------------------------- scroll

  private observeScroll(): void {
    let ticking = false;
    const check = () => {
      ticking = false;
      const el = this.doc.documentElement;
      const scrollable = el.scrollHeight - this.win.innerHeight;
      if (scrollable <= 0) return;
      const pct = ((this.win.pageYOffset || el.scrollTop) / scrollable) * 100;
      for (const mark of SCROLL_MARKS) {
        if (pct >= mark) this.trackOnce(`scroll:${mark}`, 'scroll_depth', { percent_scrolled: mark });
      }
    };
    this.win.addEventListener('scroll', () => {
      if (ticking) return;
      ticking = true;
      this.win.requestAnimationFrame(check);
    }, { passive: true });
  }

  // -------------------------------------------------------------------- clicks

  private navArea(el: Element): string {
    if (el.closest('.m-menu')) return 'mobile';
    if (el.closest('header.nav-bar')) return 'header';
    if (el.closest('footer')) return 'footer';
    return '';
  }

  private navGroup(el: Element, area: string): string {
    if (area === 'header') {
      const holder = el.closest('.nav-item');
      const btn = holder?.querySelector('button.nav-a');
      return btn ? this.text(btn) : '';
    }
    if (area === 'mobile') {
      let node: Element | null = el.closest('a');
      while (node && (node = node.previousElementSibling)) {
        if (node.classList?.contains('grp')) return this.text(node);
      }
      return '';
    }
    if (area === 'footer') {
      const col = el.closest('.f-col');
      const head = col?.querySelector('b');
      return head ? this.text(head) : (el.closest('.f-social') ? 'Social' : '');
    }
    return '';
  }

  private isExternal(href: string): boolean {
    const host = this.win.location.hostname;
    return /^https?:\/\//i.test(href) &&
      !href.includes(`//${host}`) &&
      !href.includes(`//www.${host}`);
  }

  private domainOf(href: string): string {
    try { return new URL(href, this.win.location.href).hostname; } catch { return ''; }
  }

  private observeClicks(): void {
    this.doc.addEventListener('click', (ev) => {
      const target = ev.target as Element | null;
      const el = target?.closest?.('a, button');
      if (!el) return;

      const href = el.getAttribute('href') || '';
      const label = this.text(el);
      // GA4 rejects PII, so a mailto never reports its local part.
      const url = this.trim100(
        /^mailto:/i.test(href) ? `mailto:@${href.split('@')[1] || ''}` : href,
      );

      if (el.tagName === 'BUTTON') {
        if (el.classList.contains('nav-a')) {
          this.track('nav_open', { nav_group: label, nav_area: 'header' });
          return;
        }
        if (el.classList.contains('ham')) {
          this.track('mobile_menu_toggle', {
            menu_state: el.getAttribute('aria-expanded') === 'true' ? 'close' : 'open',
          });
          return;
        }
        if (el.classList.contains('cb-accept') || el.classList.contains('cb-decline')) {
          this.track('cookie_consent', {
            consent_choice: el.classList.contains('cb-accept') ? 'all' : 'essential',
          });
          return;
        }
        if (el.classList.contains('dialog-x')) {
          this.track('pro_modal_action', { action: 'close' });
          return;
        }
        if (el.classList.contains('filter-btn')) {
          this.track('blog_filter', { category: label });
          return;
        }
        return;
      }

      if (el.closest('.dialog')) {
        this.track('pro_modal_action', {
          action: el.classList.contains('pm-skip') ? 'skip' : 'register',
          link_url: url,
        });
        if (!el.classList.contains('pm-skip')) return;
        // Skipping the modal is a real download: fall through.
      }

      const file = href.match(DOWNLOAD_FILE);
      if (file) {
        this.track('app_download', {
          file_extension: file[1].toLowerCase(),
          link_text: label,
          link_url: url,
          location: this.navArea(el) || 'body',
        });
        if (this.adsEnabled && ADS_CONVERSION) {
          this.gtag('event', 'conversion', { send_to: `${ADS_ID}/${ADS_CONVERSION}` });
        }
        return;
      }

      if (RELEASE_LINK.test(href)) {
        this.track('download_intent', {
          link_text: label,
          link_url: url,
          location: this.navArea(el) ||
            (el.closest('.dialog') ? 'pro_modal'
              : el.closest('.hero') ? 'hero'
              : el.closest('section.cta') ? 'final_cta' : 'body'),
        });
        return;
      }

      if (el.classList.contains('pro-pill')) {
        this.track('pro_click', { pro_surface: 'nav_pill', link_url: url });
        return;
      }
      if (el.closest('.pro-nudge')) {
        this.track('pro_click', { pro_surface: 'nudge', link_url: url });
        return;
      }

      const hub = el.closest('.hub-card');
      if (hub) {
        this.track('hub_card_click', {
          card_title: this.text(hub.querySelector('.hub-title')),
          card_badge: this.text(hub.querySelector('.badge')),
          link_url: url,
        });
        return;
      }

      const post = el.closest('.card--post, .related-card');
      if (post) {
        this.track('blog_card_click', {
          card_title: this.text(post.querySelector('.ui-card-title')),
          card_category: this.text(post.querySelector('.ui-card-eyebrow')),
          link_url: url,
        });
        return;
      }

      if (el.closest('.breadcrumb') || el.closest('.crumb')) {
        this.track('breadcrumb_click', { link_text: label, link_url: url });
        return;
      }

      if (el.closest('.hero-ctas') || el.closest('.ph-ctas')) {
        this.track('hero_cta_click', { link_text: label, link_url: url });
        return;
      }
      if (el.closest('section.cta')) {
        this.track('final_cta_click', { link_text: label, link_url: url });
        return;
      }

      const area = this.navArea(el);
      if (area) {
        this.track('nav_click', {
          nav_area: area,
          nav_group: this.navGroup(el, area),
          link_text: label || el.getAttribute('title') || '',
          link_url: url,
        });
        return;
      }

      if (href.charAt(0) === '#') {
        this.track('anchor_click', { anchor: url, link_text: label });
        return;
      }
      if (/^mailto:/i.test(href)) {
        this.track('email_click', { link_domain: href.split('@')[1] || '', link_text: label });
        return;
      }
      if (this.isExternal(href)) {
        this.track('outbound_click', { link_domain: this.domainOf(href), link_url: url, link_text: label });
        return;
      }
      if (href) {
        this.track('link_click', { link_url: url, link_text: label });
      }
    }, true);
  }

  /** Called by the components that site.js used to inject and watch. */
  reportCookieBannerShown(): void { this.trackOnce('cookie_view', 'cookie_banner_view'); }
  reportProModalShown(triggerUrl: string): void {
    this.track('pro_modal_view', { trigger_url: this.trim100(triggerUrl) });
  }
  reportProModalDismissed(): void { this.track('pro_modal_action', { action: 'dismiss_backdrop' }); }
  reportSectionView(name: string, index: number): void {
    this.trackOnce(`sect:${name}`, 'section_view', { section_name: name.slice(0, 100), section_index: index });
  }

  /** The autoplaying demos pause on hover or touch, and that pause is a real
   *  signal of interest — reported once per view, as before. */
  reportRailEngage(): void { this.trackOnce('rail_engage', 'flow_rail_engage'); }
  reportBoardEngage(simulator: string): void {
    this.trackOnce(`tile:${simulator}`, 'sim_board_engage', { simulator: this.trim100(simulator) });
  }
}
