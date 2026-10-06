import { DOCUMENT, Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';
import { minorToMajor } from './payments';

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
// Google Ads. Empty disables the Ads tag entirely.
const ADS_ID: string = 'AW-18221862602';
// Download conversion label, e.g. 'AbC-D_efG'.
const ADS_CONVERSION: string = '';
// Purchase conversion label, fired on payment success.
const ADS_PURCHASE: string = 'P2S9CLKR6bocEMqd7vBD';

const LOCAL_HOSTS = ['localhost', '127.0.0.1', '::1', '[::1]', '0.0.0.0', ''];
const SOLUTION_PAGES = ['emv-certification', 'cloud-simulators', 'kernel', 'middleware'];
const DOWNLOAD_FILE = /\.(dmg|msi|exe|deb|rpm|jar|zip)(\?|#|$)/i;
const RELEASE_LINK = /releases\/(latest|download)/i;
const SCROLL_MARKS = [25, 50, 75, 90];
/**
 * Where the visitor's cookie choice is kept.
 *
 * The cookie banner writes it and the inline script in index.html replays it
 * into Consent Mode before the AdSense tag loads. Exported so the banner reads
 * the same constant rather than its own copy of the string; index.html cannot
 * import it, so that one copy is duplicated there on purpose.
 */
export const CONSENT_KEY = 'iso8583-cookie-consent';
const PAID_SESSION_KEY = 'iso8583_paid_session';

/** Buckets keep amount_bucket's cardinality at five. */
function amountBucket(rupees: number): string {
  if (rupees >= 10000) return '10000_plus';
  if (rupees >= 2000) return '2000_9999';
  if (rupees >= 500) return '500_1999';
  if (rupees >= 100) return '100_499';
  return '2_99';
}

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
      MEASUREMENT_ID.includes('XXXX') ||
      // Owner kill-switch: survives a changing residential IP, unlike the
      // GA4 internal-traffic filter it backs up.
      this.lsGet('iso8583_no_analytics') === '1'
    ) {
      return;
    }

    // The paid-session flag (?src=ads, or any Google click id) is written by
    // the inline script in index.html, which runs before AdSense and therefore
    // before this. paidSession() below only reads it.

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
      return seg.length === 1
        ? { group: 'docs_index', id: 'index' }
        : { group: 'docs_guide', id: seg[1] };
    }

    if (seg[0] === 'simulator') {
      return { group: 'docs_simulator', id: seg.length === 1 ? 'index' : seg[1] };
    }

    if (seg[0] === 'tools') {
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
    // index.html installs the standard stub. Only stand in for it if that
    // script did not run (a shell this file is used from without it).
    this.win.gtag ||= (...args: unknown[]) => this.gtag(...args);

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

    // Consent Mode v2 defaults, and the replay of a stored choice, are set by
    // the inline script in index.html — they have to be in place before the
    // AdSense tag loads, which is long before this bundle runs. The region
    // list and CONSENT_KEY are mirrored there; keep the two in step.
    // applyConsent() below is what the banner calls at runtime.

    this.gtag('js', new Date());
    this.gtag('set', 'user_properties', this.compact({
      ...this.environment(ft, visits),
      // Durable audience seeds, replayed each boot so they survive sessions.
      pro_customer: this.lsGet('iso8583_pro') === '1' ? 'yes' : undefined,
      downloader: this.lsGet('iso8583_dl') === '1' ? 'yes' : undefined,
    }));
    // send_page_view is off because the router, not the document load, decides
    // when a page has been viewed. content_group mirrors page_group into GA4's
    // built-in Content group dimension, which costs no custom-dimension slot.
    this.gtag('config', MEASUREMENT_ID, {
      user_id: this.visitorId,
      send_page_view: false,
      page_group: this.page.group,
      content_group: this.page.group,
      content_id: this.page.id,
    });

    if (this.adsEnabled) {
      this.gtag('config', ADS_ID, { allow_enhanced_conversions: true });
    }
  }

  // ---------------------------------------------------------------- reporting

  private track(name: string, params: Record<string, unknown> = {}): void {
    if (!this.enabled) return;
    const p: Record<string, unknown> = {
      page_group: this.page.group,
      content_group: this.page.group,
      content_id: this.page.id,
    };
    for (const k of Object.keys(params)) {
      if (params[k] !== undefined && params[k] !== '') p[k] = params[k];
    }
    this.gtag('event', name, p);
  }

  /** The deepest activated route's data, read synchronously at NavigationEnd. */
  private routeData(): Record<string, unknown> {
    let r = this.router.routerState.snapshot.root;
    while (r.firstChild) r = r.firstChild;
    return r.data as Record<string, unknown>;
  }

  /**
   * page_location carrying only the parameters that mean something here.
   *
   * Everything else is dropped, so no query parameter can become a GA4
   * dimension by accident — the payment ref did exactly that, putting customer
   * email addresses into page_location. It also keeps /pro's cardinality
   * sane: a unique location per checkout made its landing-page report useless.
   *
   * The click ids stay because GA4 reads them from page_location for its own
   * attribution (gtag's linker reads the real URL, not this).
   */
  private cleanLocation(): string {
    const KEEP = new Set(['gclid', 'gbraid', 'wbraid', 'src']);
    try {
      const url = new URL(this.win.location.href);
      for (const key of [...url.searchParams.keys()]) {
        if (!KEEP.has(key) && !key.startsWith('utm_')) url.searchParams.delete(key);
      }
      return url.href;
    } catch {
      return this.win.location.href;
    }
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
        // A route may declare its own group when the URL cannot imply one —
        // the 404 wildcard keeps the address it was asked for.
        const declared = this.routeData()['pageGroup'];
        this.page = typeof declared === 'string'
          ? { group: declared, id: '404' }
          : this.pageInfo(e.urlAfterRedirects);
        // A new view starts a new once-per-page budget.
        this.fired.clear();
        this.gtag('event', 'page_view', {
          page_location: this.cleanLocation(),
          page_title: this.doc.title,
          page_group: this.page.group,
          content_group: this.page.group,
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
          this.track('pro_modal_action', { modal_action: 'close' });
          return;
        }
        if (el.classList.contains('filter-btn')) {
          this.track('blog_filter', { filter_category: label });
          return;
        }
        return;
      }

      if (el.closest('.dialog')) {
        const skipped = el.classList.contains('pm-skip');
        this.track('pro_modal_action', {
          modal_action: skipped ? 'skip' : 'register',
          link_url: url,
        });
        if (skipped) {
          // The skip link is the click that actually leaves for the release
          // page — the terminal download event. Returning here is also what
          // stops the RELEASE_LINK branch below double-counting the journey.
          this.reportAppDownload('release_page', label, url, 'pro_modal');
        }
        return;
      }

      const file = href.match(DOWNLOAD_FILE);
      if (file) {
        this.reportAppDownload('direct_binary', label, url, this.navArea(el) || 'body',
          file[1].toLowerCase());
        return;
      }

      if (RELEASE_LINK.test(href)) {
        this.track('download_intent', {
          link_text: label,
          link_url: url,
          cta_location: this.navArea(el) ||
            (el.closest('.hero') ? 'hero'
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

      // The three home-page surfaces the legacy site.js tracked; restored with
      // params that reuse already-registered dimensions.
      const node = el.closest('.node');
      if (node) {
        const siblings = Array.from(node.parentElement?.children ?? []).filter(
          (c) => c.classList.contains('node'));
        this.track('flow_node_click', {
          section_index: siblings.indexOf(node) + 1,
          section_name: this.text(node.querySelector('h3')),
          link_url: url,
        });
        return;
      }
      if (el.classList.contains('cat') || el.closest('.cat')) {
        const cat = el.classList.contains('cat') ? el : el.closest('.cat')!;
        this.track('tool_category_click', {
          card_category: this.text(cat.querySelector('h3')),
          link_url: url,
        });
        return;
      }
      if (el.classList.contains('sol') || el.closest('.sol')) {
        const sol = el.classList.contains('sol') ? el : el.closest('.sol')!;
        this.track('solution_click', {
          card_title: this.text(sol.querySelector('h3')),
          link_url: url,
        });
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
        this.track('anchor_click', { link_url: url, link_text: label });
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

  /** Whether this session arrived from a paid click (?src=ads or a gclid). */
  paidSession(): boolean {
    try { return this.win?.sessionStorage.getItem(PAID_SESSION_KEY) === '1'; } catch { return false; }
  }

  /**
   * Fires an event and calls `done` once the beacon has left (or after 400ms,
   * whichever is first) — for the one event that immediately precedes a
   * cross-origin redirect and must not be lost to it.
   */
  private trackThen(name: string, params: Record<string, unknown>, done: () => void): void {
    if (!this.enabled) { done(); return; }
    let called = false;
    const fire = () => { if (!called) { called = true; done(); } };
    this.track(name, { ...params, event_callback: fire, event_timeout: 400 });
    setTimeout(fire, 400);
  }

  /** Called by the components that site.js used to inject and watch. */
  reportCookieBannerShown(): void { this.trackOnce('cookie_view', 'cookie_banner_view'); }
  reportProModalShown(triggerUrl: string): void {
    this.track('pro_modal_view', { link_url: this.trim100(triggerUrl) });
  }
  reportProModalDismissed(): void { this.track('pro_modal_action', { modal_action: 'dismiss_backdrop' }); }

  /** Consent Mode v2 update; called by the cookie banner and replayed on boot. */
  applyConsent(all: boolean): void {
    this.gtag('consent', 'update', {
      analytics_storage: 'granted',
      ad_storage: all ? 'granted' : 'denied',
      ad_user_data: all ? 'granted' : 'denied',
      ad_personalization: all ? 'granted' : 'denied',
    });
  }

  // ------------------------------------------------------------- pro funnel

  /** view_item for the Pro pitch — once per navigation, skipped on returns. */
  reportProView(currency = 'INR'): void {
    this.trackOnce('pro_view', 'view_item', {
      currency,
      items: [{ item_id: 'pp_pro_initial', item_name: 'ISO8583Studio Pro — early access',
                item_category: 'pro', item_brand: 'ISO8583Studio', quantity: 1 }],
    });
  }

  reportFormStart(): void { this.trackOnce('pro_form_start', 'form_start'); }

  reportFormSubmit(valid: boolean, value?: number, currency = 'INR'): void {
    this.track('form_submit', {
      form_valid: valid ? 'yes' : 'no',
      ...(valid && value ? { value, currency } : {}),
    });
  }

  /** Field *names* only — the form holds PII that must never reach GA4. */
  reportFormError(fields: string[]): void {
    this.track('form_error', { error_field: this.trim100(fields.join('|')) });
  }

  /**
   * begin_checkout + generate_lead, then `done` — which performs the redirect.
   * Fired with the amount the service froze, the only authoritative figure.
   */
  reportBeginCheckout(amountMinor: number, checkoutId: string, done: () => void, currency = 'INR'): void {
    const value = minorToMajor(amountMinor, currency);
    this.track('generate_lead', { value, currency });

    // Click-time Ads purchase conversion, so a payer who completes in the UPI
    // app but never returns to the result page is still counted. Keyed to the
    // checkout id: the success-page fire uses the same id, and Google Ads
    // drops the duplicate. Dispatched before begin_checkout so it rides the
    // same pre-redirect flush that trackThen's callback/timeout guards.
    if (this.adsEnabled && ADS_PURCHASE) {
      this.gtag('event', 'conversion', {
        send_to: `${ADS_ID}/${ADS_PURCHASE}`,
        value,
        currency,
        transaction_id: checkoutId,
      });
    }
    this.trackThen('begin_checkout', {
      value, currency,
      amount_bucket: amountBucket(value),
      checkout_id: checkoutId,
      items: [{ item_id: 'pp_pro_initial', item_name: 'ISO8583Studio Pro — early access',
                item_category: 'pro', price: value, quantity: 1 }],
    }, done);
  }

  reportCheckoutError(code: string): void {
    this.track('checkout_error', { error_code: this.trim100(code) });
  }

  /**
   * The purchase, deduped by a localStorage ledger of reported refs — written
   * before the event fires, so a mid-flight failure fails closed. The ledger
   * is localStorage because a UPI app returns the customer in a new tab.
   */
  reportPurchase(orderRef: string, amountMinor: number | null, checkoutId: string | null, currency = 'INR'): void {
    const LEDGER = 'iso8583studio.purchases_reported';
    // Keyed on the checkout, not the customer. Keyed on the customer it
    // suppressed their *second* genuine purchase forever; keyed on the
    // checkout it still drops a reload, which is all it was ever for.
    const key = checkoutId || orderRef;
    try {
      const seen: string[] = JSON.parse(this.lsGet(LEDGER) || '[]');
      if (seen.includes(key)) return;
      this.lsSet(LEDGER, JSON.stringify([...seen, key].slice(-20)));
    } catch { /* private mode: transaction_id still dedupes server-side */ }

    this.lsSet('iso8583_pro', '1');
    this.gtag('set', 'user_properties', { pro_customer: 'yes' });

    const rupees = amountMinor === null ? undefined : minorToMajor(amountMinor, currency);

    // Google Ads purchase conversion. Behind the same ledger as the GA4 event,
    // and keyed to the checkout id so Google drops it as a duplicate of the
    // click-time fire in reportBeginCheckout when both arrive. The order ref
    // is the fallback key for flows that never had a checkout id. Falls back
    // to the conversion action's default value (1.0) when the amount is
    // unknown.
    if (this.adsEnabled && ADS_PURCHASE) {
      this.gtag('event', 'conversion', this.compact({
        send_to: `${ADS_ID}/${ADS_PURCHASE}`,
        value: rupees ?? 1.0,
        currency,
        transaction_id: checkoutId || orderRef,
      }));
    }

    this.track('purchase', {
      // The same id as the Ads fire above, or the GA4-to-Ads import cannot
      // dedupe the two against each other.
      transaction_id: checkoutId || orderRef,
      currency,
      ...(rupees !== undefined ? { value: rupees, amount_bucket: amountBucket(rupees) } : {}),
      value_known: rupees !== undefined ? 'yes' : 'no',
      ...(checkoutId ? { checkout_id: checkoutId } : {}),
      items: [{ item_id: 'pp_pro_initial', item_name: 'ISO8583Studio Pro — early access',
                item_category: 'pro', ...(rupees !== undefined ? { price: rupees } : {}),
                quantity: 1 }],
    });
  }

  /**
   * Google Ads Enhanced Conversions user-provided data.
   *
   * gtag normalises and SHA-256-hashes this locally before transmission, and
   * Consent Mode gates it on ad_user_data — so a visitor in a deny region who
   * has not accepted sends nothing. Deliberately not routed through track():
   * this value must never become a GA4 event parameter. It is the sanctioned
   * home for the email that transaction_id was previously misused for.
   *
   * A no-op until Enhanced Conversions is enabled on the conversion action in
   * the Ads UI, so it is safe to ship ahead of that.
   */
  setAdsUserData(email: string): void {
    if (!this.enabled || !this.adsEnabled || !email) return;
    this.gtag('set', 'user_data', { email: email.trim().toLowerCase() });
  }

  reportPaymentFailed(ref: string): void {
    this.track('payment_failed', { payment_state: 'failed', transaction_id: ref });
  }

  reportPaymentResult(state: string, ref: string): void {
    this.track('payment_result_view', { payment_state: state, transaction_id: ref });
  }

  /** The terminal download event — the click that actually leaves for a build. */
  reportAppDownload(source: string, label: string, url: string, ctaLocation: string,
                    fileExtension?: string): void {
    this.lsSet('iso8583_dl', '1');
    this.gtag('set', 'user_properties', { downloader: 'yes' });
    this.track('app_download', {
      download_source: source,
      link_text: label,
      link_url: url,
      cta_location: ctaLocation,
      ...(fileExtension ? { file_extension: fileExtension } : {}),
    });

    // The primary campaign goal, fired natively rather than left to the GA4
    // import: the import lands hours later, which is exactly the latency
    // Smart Bidding cannot absorb on the goal it optimises against.
    //
    // No transaction_id and no value — the conversion action is set to
    // Count = One, so three builds grabbed by one visitor count once, and the
    // value is owned in the Ads UI rather than hard-coded here.
    //
    // The GA4 app_download key event must NOT also be imported into the same
    // Ads account; that would count this click twice. An empty
    // ADS_CONVERSION makes this a no-op, so it ships before the label exists.
    if (this.adsEnabled && ADS_CONVERSION) {
      this.gtag('event', 'conversion', { send_to: `${ADS_ID}/${ADS_CONVERSION}` });
    }
  }
  /**
   * A page nobody asked for.
   *
   * Reuses link_url and link_text, which are already registered custom
   * dimensions, rather than spending two more slots: link_url is the address
   * that missed, link_text the referrer's host. Worth marking as a key event
   * in GA4 — it is what catches a broken ad final URL in hours rather than at
   * the end of a billing cycle.
   */
  reportNotFound(path: string, referrer: string): void {
    this.trackOnce('not_found', 'page_not_found', {
      link_url: this.trim100(path),
      link_text: this.trim100(referrer ? this.domainOf(referrer) : '(direct)'),
    });
  }

  reportSectionView(name: string, index: number): void {
    this.trackOnce(`sect:${name}`, 'section_view', { section_name: name.slice(0, 100), section_index: index });
  }

  /** The autoplaying demos pause on hover or touch, and that pause is a real
   *  signal of interest — reported once per view, as before. */
  reportRailEngage(): void { this.trackOnce('rail_engage', 'flow_rail_engage'); }
  reportBoardEngage(simulator: string): void {
    this.trackOnce(`tile:${simulator}`, 'sim_board_engage', { simulator_type: this.trim100(simulator) });
  }
}
