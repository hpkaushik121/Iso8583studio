/*!
 * ISO8583Studio - Google Analytics 4 + Google Ads tagging.
 *
 * Loaded by every page in the site via:  <script defer src="/assets/analytics.js"></script>
 * This file is the single source of truth for the measurement IDs - do not inline
 * gtag snippets into individual pages.
 *
 * Event taxonomy tracks the redesigned site: sections scrolled into view
 * ("screens"), every navigable click, and the actions the new components
 * expose (mega menus, the Pro download intercept, cookie consent).
 * Selectors that only exist on the redesigned home page degrade to no-ops
 * on the older pages, which fall back to the generic link_click event.
 */
(function () {
    'use strict';

    // ---------------------------------------------------------------- config
    var MEASUREMENT_ID = 'G-445XQ0W2Q4';   // GA4 web data stream
    var ADS_ID         = '';               // Google Ads, e.g. 'AW-123456789'. Empty = disabled.
    var ADS_CONVERSION = '';               // Download conversion label, e.g. 'AbC-D_efG'

    // Never report from local previews.
    var host = location.hostname;
    var LOCAL_HOSTS = ['localhost', '127.0.0.1', '::1', '[::1]', '0.0.0.0', ''];
    if (location.protocol === 'file:' ||
        LOCAL_HOSTS.indexOf(host) !== -1 ||
        /\.local$|\.test$|\.localhost$/.test(host)) {
        return;
    }
    if (!MEASUREMENT_ID || MEASUREMENT_ID.indexOf('XXXX') !== -1) {
        return; // not configured yet
    }

    // ------------------------------------------------------------- utilities
    // GA4 caps user-property keys at 24 chars and values at 36.
    function clampValue(v) {
        if (v === null || v === undefined) return undefined;
        v = String(v);
        if (v === '') return undefined;
        return v.length > 36 ? v.slice(0, 36) : v;
    }

    // GA4 stores empty user properties as real (blank) values, which pollutes reporting.
    function compact(obj) {
        var out = {};
        for (var k in obj) {
            if (obj[k] !== undefined) out[k] = obj[k];
        }
        return out;
    }

    function lsGet(key) {
        try { return window.localStorage.getItem(key); } catch (e) { return null; }
    }

    function lsSet(key, value) {
        try { window.localStorage.setItem(key, value); } catch (e) { /* private mode */ }
    }

    function uuid() {
        try {
            if (window.crypto && window.crypto.randomUUID) return window.crypto.randomUUID();
        } catch (e) { /* fall through */ }
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
            var r = (Math.random() * 16) | 0;
            return (c === 'x' ? r : ((r & 0x3) | 0x8)).toString(16);
        });
    }

    function param(name) {
        try {
            return new URLSearchParams(location.search).get(name) || '';
        } catch (e) { return ''; }
    }

    // Event parameter values are capped at 100 chars by GA4.
    function text(el) {
        if (!el) return '';
        return (el.textContent || '').trim().replace(/\s+/g, ' ').slice(0, 100);
    }

    function trim100(v) {
        return String(v == null ? '' : v).slice(0, 100);
    }

    // ------------------------------------------------------- page dimensions
    // Derived from the path so every page reports distinctly with no per-page markup.
    var SIMULATOR_DOCS = /^(host|hsm|apdu|pos|atm|ecr|issuer|payment-switch|hsm-command-console)/;
    var GUIDE_DOCS = ['installation', 'versions', 'contributing'];

    function pageInfo() {
        var path = location.pathname.replace(/index\.html$/, '');
        var seg = path.split('/').filter(Boolean);

        if (seg.length === 0) return { group: 'home', id: 'home' };

        var last = seg[seg.length - 1];
        var slug = last.replace(/\.html$/, '');

        if (seg[0] === 'blogs') {
            return seg.length === 1
                ? { group: 'blog_index', id: 'index' }
                : { group: 'blog', id: slug };
        }

        if (seg[0] === 'docs') {
            if (seg.length === 1) return { group: 'docs_index', id: 'index' };
            if (GUIDE_DOCS.indexOf(seg[1]) !== -1) return { group: 'docs_guide', id: seg[1] };
            if (seg[1] === 'payment-simulators' || SIMULATOR_DOCS.test(seg[1])) {
                return { group: 'docs_simulator', id: seg[1] };
            }
            return { group: 'docs_tool', id: seg[1] };
        }

        if (/^(privacy-policy|terms-and-conditions)$/.test(slug)) {
            return { group: 'legal', id: slug };
        }

        if (['emv-certification', 'cloud-simulators', 'kernel', 'middleware'].indexOf(seg[0]) !== -1) {
            return { group: 'solution', id: seg[0] };
        }

        if (seg[0] === 'pro') return { group: 'pro', id: 'pro' };
        if (seg[0] === 'contact') return { group: 'contact', id: 'contact' };

        return { group: 'other', id: slug || seg[0] };
    }

    // ------------------------------------------------------------- identity
    var VISITOR_KEY = 'iso8583_uid';
    var visitorId = lsGet(VISITOR_KEY);
    if (!visitorId) {
        visitorId = uuid();
        lsSet(VISITOR_KEY, visitorId);
    }

    // First-touch attribution: captured once, on the very first page ever seen.
    function firstTouch() {
        var stored = lsGet('iso8583_first');
        if (stored) {
            try { return JSON.parse(stored); } catch (e) { /* rewrite below */ }
        }
        var ft = {
            page: location.pathname,
            ref: document.referrer || '(direct)',
            date: new Date().toISOString().slice(0, 10),
            gclid: param('gclid'),
            gbraid: param('gbraid'),
            wbraid: param('wbraid'),
            src: param('utm_source'),
            med: param('utm_medium'),
            cmp: param('utm_campaign')
        };
        lsSet('iso8583_first', JSON.stringify(ft));
        return ft;
    }

    var ft = firstTouch();

    var visits = parseInt(lsGet('iso8583_visits') || '0', 10) + 1;
    lsSet('iso8583_visits', String(visits));

    // ----------------------------------------------------------- environment
    function environment() {
        var nav = window.navigator || {};
        var conn = nav.connection || {};
        var scheme = 'unknown';
        try {
            if (window.matchMedia) {
                scheme = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
            }
        } catch (e) { /* ignore */ }

        var tz = '';
        try { tz = Intl.DateTimeFormat().resolvedOptions().timeZone || ''; } catch (e) { /* ignore */ }

        return {
            // The desktop app reports into the same GA4 property; this keeps the two
            // separable in every report.
            stream_source: 'website',
            browser_language: clampValue(nav.language),
            timezone: clampValue(tz),
            screen_resolution: clampValue(screen.width + 'x' + screen.height),
            viewport_size: clampValue(window.innerWidth + 'x' + window.innerHeight),
            device_pixel_ratio: clampValue(window.devicePixelRatio),
            color_scheme: clampValue(scheme),
            device_memory: clampValue(nav.deviceMemory),
            hardware_concurrency: clampValue(nav.hardwareConcurrency),
            connection_type: clampValue(conn.effectiveType),
            platform: clampValue(nav.platform),
            visit_count: clampValue(visits),
            first_landing_page: clampValue(ft.page),
            first_referrer: clampValue(ft.ref),
            first_seen_date: clampValue(ft.date),
            first_gclid: clampValue(ft.gclid),
            first_gbraid: clampValue(ft.gbraid),
            first_wbraid: clampValue(ft.wbraid),
            first_utm_source: clampValue(ft.src),
            first_utm_medium: clampValue(ft.med),
            first_utm_campaign: clampValue(ft.cmp)
        };
    }

    // --------------------------------------------------------------- bootstrap
    window.dataLayer = window.dataLayer || [];
    function gtag() { window.dataLayer.push(arguments); }
    window.gtag = gtag;

    var s = document.createElement('script');
    s.async = true;
    s.src = 'https://www.googletagmanager.com/gtag/js?id=' + encodeURIComponent(MEASUREMENT_ID);
    (document.head || document.documentElement).appendChild(s);

    var page = pageInfo();

    gtag('js', new Date());
    gtag('set', 'user_properties', compact(environment()));
    gtag('config', MEASUREMENT_ID, {
        user_id: visitorId,
        page_group: page.group,
        content_id: page.id
    });

    // Google Ads: remarketing + conversion attribution.
    // Enhanced conversions stays off - this site collects no email/phone to hash.
    var adsEnabled = ADS_ID && ADS_ID.indexOf('XXXX') === -1;
    if (adsEnabled) {
        gtag('config', ADS_ID, { allow_enhanced_conversions: false });
    }

    // Every event carries the page dimensions so reports can slice by page
    // without joining against page_view.
    function track(name, params) {
        var p = { page_group: page.group, content_id: page.id };
        for (var k in params) {
            if (params[k] !== undefined && params[k] !== '') p[k] = params[k];
        }
        gtag('event', name, p);
    }

    // Fires an event at most once per page load, keyed by name+discriminator.
    var fired = {};
    function trackOnce(key, name, params) {
        if (fired[key]) return;
        fired[key] = 1;
        track(name, params);
    }

    // ============================================================== SCREENS
    // Sections of the redesigned page are tagged data-sect; each reports the
    // first time it enters the viewport, giving a funnel down the page.
    // Every page type names its sections differently: the home page tags them
    // data-sect, doc pages use <section class="doc-section" id="...">, and the
    // solution pages use an unnamed <section> introduced by a .kicker label.
    // Fall through those in order so one observer covers the whole site.
    function sectionName(el, index) {
        var n = el.getAttribute('data-sect') || el.id;
        if (n) return n.slice(0, 100);
        n = text(el.querySelector('.kicker')) || text(el.querySelector('h2'));
        return (n || 'section_' + (index + 1)).slice(0, 100);
    }

    function observeSections() {
        if (!window.IntersectionObserver) return;

        var seen = [];
        [].forEach.call(document.querySelectorAll('[data-sect], section, .doc-section'), function (el) {
            if (seen.indexOf(el) === -1) seen.push(el);
        });
        var shots = [].slice.call(document.querySelectorAll('.shot'));
        if (!seen.length && !shots.length) return;

        // rootMargin rather than a ratio threshold: doc sections are routinely
        // taller than the viewport, so they can never reach a 0.25 ratio.
        var io = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (!entry.isIntersecting) return;
                var el = entry.target;
                io.unobserve(el);

                var si = seen.indexOf(el);
                if (si !== -1) {
                    var name = sectionName(el, si);
                    trackOnce('sect:' + name, 'section_view', {
                        section_name: name,
                        section_index: si + 1
                    });
                    return;
                }

                var bar = el.querySelector('.shot-bar span');
                trackOnce('shot:' + shots.indexOf(el), 'screenshot_view', {
                    screenshot_title: text(bar) || 'untitled',
                    screenshot_index: shots.indexOf(el) + 1
                });
            });
        }, { threshold: 0, rootMargin: '0px 0px -20% 0px' });

        seen.forEach(function (el) { io.observe(el); });
        shots.forEach(function (el) { io.observe(el); });
    }

    // Scroll milestones - the coarse read on how far down anyone gets.
    function observeScroll() {
        var marks = [25, 50, 75, 90];
        var ticking = false;

        function check() {
            ticking = false;
            var doc = document.documentElement;
            var scrollable = doc.scrollHeight - window.innerHeight;
            if (scrollable <= 0) return;
            var pct = ((window.pageYOffset || doc.scrollTop) / scrollable) * 100;
            for (var i = 0; i < marks.length; i++) {
                if (pct >= marks[i]) {
                    trackOnce('scroll:' + marks[i], 'scroll_depth', { percent_scrolled: marks[i] });
                }
            }
        }

        window.addEventListener('scroll', function () {
            if (ticking) return;
            ticking = true;
            window.requestAnimationFrame(check);
        }, { passive: true });
        check();
    }

    // ============================================================== ACTIONS
    // Components injected by assets/site.js (cookie bar, Pro download modal)
    // are watched rather than instrumented in place, so site.js stays the
    // untouched design source of truth.
    function observeInjected() {
        function noteCookieBar(el) {
            if (!el) return;
            trackOnce('cookie_view', 'cookie_banner_view', {});
        }
        function noteProModal(el) {
            if (!el) return;
            var skip = el.querySelector('.pm-skip');
            track('pro_modal_view', {
                trigger_url: trim100(skip ? skip.getAttribute('href') : '')
            });
        }

        noteCookieBar(document.querySelector('.cookie-bar'));
        noteProModal(document.querySelector('.pro-modal-ov'));

        if (!window.MutationObserver || !document.body) return;
        new MutationObserver(function (records) {
            records.forEach(function (rec) {
                [].forEach.call(rec.addedNodes, function (node) {
                    if (!node || node.nodeType !== 1 || !node.classList) return;
                    if (node.classList.contains('cookie-bar')) noteCookieBar(node);
                    if (node.classList.contains('pro-modal-ov')) noteProModal(node);
                });
            });
        }).observe(document.body, { childList: true });
    }

    // Autoplaying demos pause on hover/touch - that pause is a real signal of
    // interest, so it is reported once per page.
    function observeDemos() {
        var rail = document.getElementById('flowRail');
        if (rail) {
            ['mouseenter', 'touchstart'].forEach(function (evt) {
                rail.addEventListener(evt, function () {
                    trackOnce('rail_engage', 'flow_rail_engage', {});
                }, { passive: true });
            });
        }

        var grid = document.getElementById('simGrid');
        if (!grid) return;
        [].forEach.call(grid.querySelectorAll('.simtile'), function (tile) {
            ['mouseenter', 'touchstart'].forEach(function (evt) {
                tile.addEventListener(evt, function () {
                    var name = text(tile.querySelector('.st-name'));
                    trackOnce('tile:' + name, 'sim_board_engage', { simulator: name });
                }, { passive: true });
            });
        });
    }

    // ============================================================== CLICKS
    // One delegated listener classifies every click on the site. The listener
    // runs on capture so it still sees clicks that site.js intercepts (the Pro
    // download modal calls preventDefault but never stopPropagation).
    var DOWNLOAD_FILE = /\.(dmg|msi|exe|deb|rpm|jar|zip)(\?|#|$)/i;
    var RELEASE_LINK = /releases\/(latest|download)/i;

    function navArea(el) {
        if (el.closest('.m-menu')) return 'mobile';
        if (el.closest('header.nav-bar')) return 'header';
        if (el.closest('footer')) return 'footer';
        return '';
    }

    function navGroup(el, area) {
        if (area === 'header') {
            var holder = el.closest('.nav-links > div');
            var btn = holder && holder.querySelector('button.nav-a');
            return btn ? text(btn) : '';
        }
        if (area === 'mobile') {
            var grp = '';
            var node = el.closest('a');
            while (node && (node = node.previousElementSibling)) {
                if (node.classList && node.classList.contains('grp')) { grp = text(node); break; }
            }
            return grp;
        }
        if (area === 'footer') {
            var col = el.closest('.f-col');
            var head = col && col.querySelector('b');
            return head ? text(head) : (el.closest('.f-social') ? 'Social' : '');
        }
        return '';
    }

    function isExternal(href) {
        return /^https?:\/\//i.test(href) && href.indexOf('//' + host) === -1 &&
               href.indexOf('//www.' + host) === -1;
    }

    function domainOf(href) {
        try { return new URL(href, location.href).hostname; } catch (e) { return ''; }
    }

    document.addEventListener('click', function (e) {
        var el = e.target && e.target.closest ? e.target.closest('a, button') : null;
        if (!el) return;

        var href = (el.getAttribute && el.getAttribute('href')) || '';
        var label = text(el);
        // GA4 rejects PII in event parameters, so a mailto: never reports its
        // local part - only the domain - no matter which branch logs it.
        var url = trim100(/^mailto:/i.test(href) ? 'mailto:@' + (href.split('@')[1] || '') : href);

        // -- mega-menu triggers and the mobile hamburger ---------------------
        if (el.tagName === 'BUTTON') {
            if (el.classList.contains('nav-a')) {
                track('nav_open', { nav_group: label, nav_area: 'header' });
                return;
            }
            if (el.id === 'hamBtn') {
                var menu = document.getElementById('mMenu');
                track('mobile_menu_toggle', {
                    // classList is read before site.js toggles it.
                    menu_state: menu && menu.classList.contains('open') ? 'close' : 'open'
                });
                return;
            }
            if (el.classList.contains('cb-accept') || el.classList.contains('cb-decline')) {
                track('cookie_consent', {
                    consent_choice: el.classList.contains('cb-accept') ? 'all' : 'essential'
                });
                return;
            }
            if (el.classList.contains('pm-x')) {
                track('pro_modal_action', { action: 'close' });
                return;
            }
            return;
        }

        // -- the Pro download-intercept modal --------------------------------
        if (el.closest('.pro-modal')) {
            track('pro_modal_action', {
                action: el.classList.contains('pm-skip') ? 'skip' : 'register',
                link_url: url
            });
            if (!el.classList.contains('pm-skip')) return;
            // fall through: skipping the modal is a real download
        }

        // -- downloads --------------------------------------------------------
        var file = href.match(DOWNLOAD_FILE);
        if (file) {
            track('app_download', {
                file_extension: file[1].toLowerCase(),
                link_text: label,
                link_url: url,
                location: navArea(el) || 'body'
            });
            if (adsEnabled && ADS_CONVERSION) {
                gtag('event', 'conversion', { send_to: ADS_ID + '/' + ADS_CONVERSION });
            }
            return;
        }
        if (RELEASE_LINK.test(href)) {
            track('download_intent', {
                link_text: label,
                link_url: url,
                location: navArea(el) || (el.closest('.pro-modal') ? 'pro_modal' :
                          el.closest('.hero') ? 'hero' :
                          el.closest('section.cta') ? 'final_cta' : 'body')
            });
            return;
        }

        // -- Pro surfaces -----------------------------------------------------
        if (el.classList.contains('pro-pill')) {
            track('pro_click', { pro_surface: 'nav_pill', link_url: url });
            return;
        }
        if (el.closest('.pro-nudge')) {
            track('pro_click', { pro_surface: 'nudge', link_url: url });
            return;
        }

        // -- doc hubs and the contact page (.hub-card grids) -----------------
        var hub = el.closest('.hub-card');
        if (hub) {
            track('hub_card_click', {
                card_title: text(hub.querySelector('.hub-title')),
                card_badge: text(hub.querySelector('.badge')),
                link_url: url
            });
            return;
        }

        // -- breadcrumb trail (every doc page) -------------------------------
        if (el.closest('.breadcrumb') || el.closest('.crumb')) {
            track('breadcrumb_click', { link_text: label, link_url: url });
            return;
        }

        // -- home-page content cards -----------------------------------------
        var node = el.closest('.node');
        if (node) {
            var nodes = [].slice.call(node.parentNode.querySelectorAll('.node'));
            track('flow_node_click', {
                step_index: nodes.indexOf(node) + 1,
                step_name: text(node.querySelector('h3')),
                link_url: url
            });
            return;
        }
        if (el.classList.contains('cat')) {
            track('tool_category_click', {
                category: text(el.querySelector('h3')),
                tool_count: text(el.querySelector('.cnt')),
                link_url: url
            });
            return;
        }
        if (el.classList.contains('sol')) {
            track('solution_click', {
                solution: text(el.querySelector('h3')),
                link_url: url
            });
            return;
        }

        // -- hero and closing calls to action --------------------------------
        // .hero-ctas is the home page, .ph-ctas the solution pages.
        if (el.closest('.hero-ctas') || el.closest('.ph-ctas')) {
            track('hero_cta_click', { link_text: label, link_url: url });
            return;
        }
        if (el.closest('section.cta')) {
            track('final_cta_click', { link_text: label, link_url: url });
            return;
        }

        // -- header / mobile drawer / footer navigation ----------------------
        var area = navArea(el);
        if (area) {
            track('nav_click', {
                nav_area: area,
                nav_group: navGroup(el, area),
                link_text: label || (el.getAttribute('title') || ''),
                link_url: url
            });
            return;
        }

        // -- everything else --------------------------------------------------
        if (href.charAt(0) === '#') {
            track('anchor_click', { anchor: url, link_text: label });
            return;
        }
        if (/^mailto:/i.test(href)) {
            // Report the address only as a domain - never the local part.
            track('email_click', { link_domain: href.split('@')[1] || '', link_text: label });
            return;
        }
        if (isExternal(href)) {
            track('outbound_click', {
                link_domain: domainOf(href),
                link_url: url,
                link_text: label
            });
            return;
        }
        if (href) {
            track('link_click', { link_url: url, link_text: label });
        }
    }, true);

    // Clicking the modal backdrop dismisses it - site.js handles that on the
    // overlay itself, so it never reaches the delegated anchor/button listener.
    document.addEventListener('click', function (e) {
        if (e.target && e.target.classList && e.target.classList.contains('pro-modal-ov')) {
            track('pro_modal_action', { action: 'dismiss_backdrop' });
        }
    }, true);

    // --------------------------------------------------------------- start up
    function start() {
        observeSections();
        observeScroll();
        observeInjected();
        observeDemos();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', start);
    } else {
        start();
    }
})();
