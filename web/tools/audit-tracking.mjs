/**
 * Records what the analytics layer reports for each tracked element, without
 * sending anything to Google.
 *
 * Analytics switches itself off on localhost, so the dist server is opened
 * under a hostname Chrome is told to resolve to 127.0.0.1. Every request to a
 * Google endpoint is aborted; the events are read back out of window.dataLayer.
 *
 *   npm run build && node tools/audit-tracking.mjs out.json
 *   node tools/audit-tracking.mjs --diff before.json after.json
 *
 * The probes are the selectors core/analytics.ts keys its events on. A redesign
 * that keeps tracking intact produces the same event names and parameters for
 * the same probes, so a diff of two runs is the acceptance test.
 */
import { spawn } from 'node:child_process';
import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import puppeteer from 'puppeteer';

const HERE = dirname(fileURLToPath(import.meta.url));
const PORT = 4399;
const HOST = 'tracking-audit.example';
const ORIGIN = `http://${HOST}:${PORT}`;

const BLOCKED = /fonts\.googleapis\.com|fonts\.gstatic\.com|googletagmanager\.com|google-analytics\.com|googlesyndication\.com|doubleclick\.net|googleadservices\.com|google\.com\/(ads|pagead|ccm)/;

/** Parameters that differ run to run and say nothing about the markup. */
const VOLATILE = new Set(['transaction_id', 'checkout_id', 'event_callback', 'event_timeout']);

const SHELL = [
  'header.nav-bar button.nav-a',
  'header.nav-bar .nav-item .menu a',
  'header.nav-bar a.nav-a',
  'a.pro-pill',
  'button.ham',
  // Direct children only: the drawer's own brand link sits in its top bar.
  '.m-menu > a',
  'footer .f-col a',
  'footer .f-social a',
  'button.cb-decline',
];

const PAGES = {
  '/': [...SHELL, '.hero-ctas a', '.node a', '.cat', '.sol', 'section.cta a', '.pro-nudge a'],
  '/emv-certification': ['.crumb a, .breadcrumb a', '.ph-ctas a', 'section.cta a', '.pro-nudge a'],
  '/cloud-simulators': ['.crumb a, .breadcrumb a', '.ph-ctas a', 'section.cta a'],
  '/middleware': ['.crumb a, .breadcrumb a', '.ph-ctas a', 'section.cta a'],
  '/kernel': ['.crumb a, .breadcrumb a', '.ph-ctas a', 'section.cta a'],
  '/docs': ['.breadcrumb a', '.hub-card', 'section.cta a', '.pro-nudge a'],
  '/docs/installation': ['.breadcrumb a', '.pro-nudge a'],
  '/docs/versions': ['.breadcrumb a'],
  '/docs/contributing': ['.breadcrumb a'],
  '/simulator': ['.breadcrumb a', '.hub-card'],
  '/simulator/host': ['.breadcrumb a', 'section.cta a', '.pro-nudge a'],
  '/simulator/hsm': ['.breadcrumb a', 'section.cta a', '.pro-nudge a'],
  '/simulator/pos': ['.breadcrumb a', 'section.cta a'],
  '/simulator/apdu': ['.breadcrumb a'],
  '/simulator/hsm-command-console': ['.breadcrumb a'],
  '/simulator/payment-switch': ['.breadcrumb a'],
  '/tools/emv-tools': ['.breadcrumb a', 'section.cta a', '.pro-nudge a'],
  '/tools/pin-tools': ['.breadcrumb a', 'section.cta a'],
  '/tools/card-validation': ['.breadcrumb a'],
  '/download': ['.breadcrumb a', 'a[href$=".dmg"]', 'a[href$=".exe"]'],
  '/contact': ['.breadcrumb a', '.hub-card', '.pro-nudge a'],
  '/privacy-policy': ['.breadcrumb a', '.hub-card'],
  '/pro': ['.crumb a', '.breadcrumb a', '.ph-ctas a', '.hero-ctas a'],
  '/blogs': ['button.filter-btn', '.card--post', 'section.cta a'],
  '/blogs/what-is-iso8583-studio': ['.breadcrumb a', '.related-card', 'section.cta a'],
};

function normalise(entry) {
  const [kind, name, params] = entry;
  if (kind !== 'event') return null;
  const clean = {};
  for (const key of Object.keys(params || {}).sort()) {
    if (!VOLATILE.has(key)) clean[key] = params[key];
  }
  return { name, params: clean };
}

async function record(outFile) {
  const server = spawn(process.execPath, [join(HERE, 'serve-dist.mjs')], {
    env: { ...process.env, PORT: String(PORT) }, stdio: 'ignore',
  });
  await new Promise((r) => setTimeout(r, 600));

  const browser = await puppeteer.launch({
    headless: true,
    args: [`--host-resolver-rules=MAP ${HOST} 127.0.0.1`, '--no-sandbox'],
  });
  const result = {};

  try {
    for (const [path, probes] of Object.entries(PAGES)) {
      result[path] = { sections: [], probes: {} };

      const open = async () => {
        const page = await browser.newPage();
        await page.setViewport({ width: 1360, height: 900 });
        await page.setRequestInterception(true);
        page.on('request', (req) => (BLOCKED.test(req.url()) ? req.abort() : req.continue()));
        // Not networkidle: the blocked third-party requests never settle. The
        // page is ready for a probe once analytics has booted and reported it.
        await page.goto(ORIGIN + path, { waitUntil: 'domcontentloaded' });
        await page.waitForFunction(
          () => (window.dataLayer || []).some((a) => a[0] === 'event' && a[1] === 'page_view'),
          { timeout: 15000 },
        );
        await new Promise((r) => setTimeout(r, 150));
        await page.evaluate(() => {
          // Leaving the site would lose the page; analytics has already seen
          // the click by the time this runs, and preventDefault does not stop
          // it propagating.
          window.addEventListener('click', (e) => {
            const a = e.target instanceof Element ? e.target.closest('a') : null;
            if (a && /^(https?:|mailto:)/.test(a.getAttribute('href') || '')) e.preventDefault();
          }, true);
        });
        return page;
      };
      const events = (page, from) => page.evaluate((start) =>
        (window.dataLayer || []).slice(start).map((a) => Array.from(a)), from);
      const mark = (page) => page.evaluate(() => (window.dataLayer || []).length);

      // Section funnel: scroll the whole page and keep the names in order.
      {
        const page = await open();
        const start = 0;
        await page.evaluate(async () => {
          // Instant, because the site sets scroll-behavior: smooth and a smooth
          // scroll never catches up with the loop on a long page. The height is
          // re-read each step: lazy content makes the page grow as it scrolls.
          const step = Math.round(window.innerHeight * 0.6);
          for (let y = 0; y < document.documentElement.scrollHeight; y += step) {
            window.scrollTo({ top: y, behavior: 'instant' });
            await new Promise((r) => setTimeout(r, 150));
          }
          window.scrollTo({ top: document.documentElement.scrollHeight, behavior: 'instant' });
          await new Promise((r) => setTimeout(r, 600));
        });
        const all = (await events(page, start)).map(normalise).filter(Boolean);
        result[path].sections = all
          .filter((e) => e.name === 'section_view')
          .map((e) => e.params.section_name);
        result[path].pageView = all.find((e) => e.name === 'page_view')?.params ?? null;
        await page.close();
      }

      for (const selector of probes) {
        const page = await open();
        const count = await page.$$eval(selector, (els) => els.length);
        if (!count) {
          result[path].probes[selector] = { count: 0, events: [] };
          await page.close();
          continue;
        }
        const from = await mark(page);
        await page.$eval(selector, (el) =>
          el.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true })));
        await new Promise((r) => setTimeout(r, 250));
        const fired = (await events(page, from)).map(normalise).filter(Boolean)
          // The click may navigate; the destination's own page_view and
          // section events are not what this probe is measuring.
          .filter((e) => !['page_view', 'section_view', 'scroll_depth', 'view_item',
            'cookie_banner_view'].includes(e.name));
        result[path].probes[selector] = { count, events: fired };
        await page.close();
      }
      console.log(`audited ${path}`);
    }
  } finally {
    await browser.close();
    server.kill();
  }

  writeFileSync(outFile, JSON.stringify(result, null, 2));
  console.log(`wrote ${outFile}`);
}

/** Probes whose selector was widened after the first baseline was recorded. */
const RENAMED = { '.m-menu a': '.m-menu > a', '.crumb a': '.crumb a, .breadcrumb a' };

function diff(beforeFile, afterFile) {
  const before = JSON.parse(readFileSync(beforeFile, 'utf8'));
  const after = JSON.parse(readFileSync(afterFile, 'utf8'));
  let changes = 0;
  const say = (msg) => { changes++; console.log(msg); };

  for (const path of Object.keys(before)) {
    const b = before[path];
    const a = after[path];
    if (!a) { say(`${path}: missing from the second run`); continue; }

    const gone = b.sections.filter((s) => !a.sections.includes(s));
    const added = a.sections.filter((s) => !b.sections.includes(s));
    if (gone.length) say(`${path}: section_view no longer fires for ${gone.join(', ')}`);
    if (added.length) say(`${path}: section_view newly fires for ${added.join(', ')}`);

    if (JSON.stringify(b.pageView) !== JSON.stringify(a.pageView)) {
      say(`${path}: page_view changed\n  was ${JSON.stringify(b.pageView)}\n  now ${JSON.stringify(a.pageView)}`);
    }

    for (const selector of Object.keys(b.probes)) {
      const was = b.probes[selector];
      const now = a.probes[selector] ?? a.probes[RENAMED[selector]] ?? { count: 0, events: [] };
      if (!was.count) continue;
      if (!now.count) { say(`${path}: ${selector} matched ${was.count} before and nothing now`); continue; }
      const names = (p) => p.events.map((e) => e.name).join(',');
      if (names(was) !== names(now)) {
        say(`${path}: ${selector} fired [${names(was)}] before and [${names(now)}] now`);
        continue;
      }
      was.events.forEach((event, i) => {
        const other = now.events[i].params;
        for (const key of new Set([...Object.keys(event.params), ...Object.keys(other)])) {
          if (event.params[key] !== other[key]) {
            say(`${path}: ${selector} ${event.name}.${key}: ${JSON.stringify(event.params[key])} -> ${JSON.stringify(other[key])}`);
          }
        }
      });
    }
  }
  console.log(changes ? `\n${changes} difference(s)` : 'no differences');
}

const args = process.argv.slice(2);
if (args[0] === '--diff') diff(args[1], args[2]);
else await record(args[0] ?? 'tracking-audit.json');
