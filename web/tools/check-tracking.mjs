/**
 * Static guard for the analytics hooks. Runs over the built site and fails if
 * a page has lost an element that core/analytics.ts (or the section funnel in
 * pages/site/site-page.ts) reports by selector.
 *
 * It exists because those hooks are ordinary class names and ids: a restyle
 * can remove one without anything else breaking, and the only symptom is an
 * event that quietly stops arriving in GA4.
 *
 * What it checks:
 *   - the shell hooks on every page;
 *   - per page family, the content hooks and the section names that page has
 *     always reported;
 *   - that nothing that navigates is a <button> (the click listener reports
 *     anchors only), and that page sections are not nested (each <section> is
 *     one entry in the section funnel).
 *
 *   npm run build && node tools/check-tracking.mjs
 */
import { existsSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { parseDocument } from 'htmlparser2';
import { selectAll, selectOne } from 'css-select';
import { textContent } from 'domutils';

const DIST = join(dirname(fileURLToPath(import.meta.url)), '../dist/iso8583-studio/browser');

const failures = [];
const fail = (msg) => failures.push(msg);

function load(route) {
  const file = join(DIST, route === '/' ? 'index.html' : `${route.slice(1)}/index.html`);
  if (!existsSync(file)) { fail(`${route}: page not built`); return null; }
  return parseDocument(readFileSync(file, 'utf8'));
}

const count = (doc, selector) => selectAll(selector, doc).length;
const text = (node) => textContent(node).replace(/\s+/g, ' ').trim();

/** `need` is a minimum unless `exact` is set. */
function expect(route, doc, selector, need = 1, exact = false) {
  const n = count(doc, selector);
  if (exact ? n !== need : n < need) {
    fail(`${route}: expected ${exact ? 'exactly' : 'at least'} ${need} × ${selector}, found ${n}`);
  }
}

/** The names the section funnel would report, in document order. */
function sectionNames(doc) {
  const page = selectOne('.static-page', doc);
  if (!page) return [];
  return selectAll('[data-sect], section, .doc-section', page).map((el, i) =>
    el.attribs['data-sect'] || el.attribs.id ||
    (selectOne('.kicker', el) && text(selectOne('.kicker', el))) ||
    (selectOne('h2', el) && text(selectOne('h2', el))) || `section_${i + 1}`);
}

function expectSections(route, doc, names) {
  const have = new Set(sectionNames(doc));
  const missing = names.filter((n) => !have.has(n));
  if (missing.length) fail(`${route}: section_view would no longer report: ${missing.join(', ')}`);
}

// ---- Checks every page gets ------------------------------------------------

function shell(route, doc) {
  expect(route, doc, 'header.nav-bar .nav-item button.nav-a', 3);
  expect(route, doc, 'header.nav-bar a.nav-a', 2);
  expect(route, doc, 'header.nav-bar a.pro-pill');
  expect(route, doc, 'header.nav-bar button.ham[aria-expanded]');
  expect(route, doc, 'header.nav-bar a[href*="releases/latest"]');
  expect(route, doc, '.m-menu p.grp', 5);
  expect(route, doc, '.m-menu > a', 30);
  expect(route, doc, 'footer .f-col > b', 5);
  expect(route, doc, 'footer .f-social a[title]', 4);

  // A link's nav_group in the drawer is the nearest preceding .grp sibling, so
  // the drawer's links have to be direct children alongside the group labels.
  const drawer = selectOne('.m-menu', doc);
  if (drawer && selectAll('p.grp', drawer).some((p) => p.parent !== drawer)) {
    fail(`${route}: .m-menu group labels are no longer direct children of the drawer`);
  }

  for (const button of selectAll('button', doc)) {
    if (/^(⬇\s*)?download\b/i.test(text(button))) {
      fail(`${route}: "${text(button)}" is a <button>; download CTAs must be anchors to be tracked`);
    }
  }

  const page = selectOne('.static-page', doc);
  if (page) {
    for (const section of selectAll('section section', page)) {
      fail(`${route}: nested <section>${section.attribs.id ? ` #${section.attribs.id}` : ''} — each one is counted in the section funnel`);
    }
    for (const link of selectAll('a', page)) {
      const href = link.attribs.href;
      if (href === undefined && !('name' in link.attribs)) {
        fail(`${route}: <a> without href ("${text(link).slice(0, 40)}") — not reported as a link`);
      }
    }
  }
}

// ---- Per-page expectations --------------------------------------------------

const SOLUTION_SECTIONS = {
  '/emv-certification': ['Certification Services', 'Complete EMV Certification Suite', 'Our Proven Methodology', 'Deep Technical Expertise', 'Ready to certify?'],
  '/cloud-simulators': ['Hosted Test Infrastructure', 'Complete Simulator Suite', 'Testing-First Architecture', 'Why Cloud Simulator?', 'Ready to simulate?'],
  '/middleware': ['Transaction Orchestration', 'Middleware Services Suite', 'Intelligent Transaction Flow', 'Middleware Advantages', 'Ready to orchestrate?'],
  '/kernel': ['Engineering Services', 'Kernel Development Services', 'Technical Expertise Areas', 'Kernel Development Benefits', 'Ready to develop?'],
};

const GUIDE_SECTIONS = {
  '/docs': ['simulators', 'tools', 'solutions', 'resources'],
  '/docs/installation': ['prerequisites', 'windows', 'macos', 'linux', 'source', 'troubleshooting'],
  '/docs/versions': ['current', 'history', 'channel'],
  '/docs/contributing': ['prereqs', 'setup', 'style', 'pr', 'issues'],
  '/simulator': ['simulators'],
  '/simulator/host': ['overview', 'quick-start', 'gateway-types', 'gateway-config', 'transmission', 'connection-types', 'message-formats', 'log-settings', 'host-handler', 'transaction-rules', 'placeholders', 'rest-matching', 'iso8583-template', 'unsolicited'],
  '/simulator/hsm': ['overview', 'profile', 'quick-start', 'protocol', 'network', 'lmk', 'cmd-diagnostics', 'cmd-key-mgmt', 'cmd-pin-block', 'cmd-pin-verify', 'cmd-pin-gen', 'cmd-encrypt', 'cmd-mac', 'cmd-hash', 'cmd-rsa', 'cmd-cvv', 'cmd-storage', 'key-types', 'pin-formats'],
  '/simulator/pos': ['overview', 'quick-start', 'device', 'hardware', 'peripherals', 'system-boot', 'card-host', 'runtime'],
  '/simulator/apdu': ['overview', 'quick-start', 'modes', 'card-profile', 'config-tabs', 'runtime', 'status-words', 'tips'],
  '/simulator/hsm-command-console': ['overview', 'quick-start', 'vendors', 'connection', 'framing', 'ssl', 'console', 'scenario', 'load-test', 'logs'],
  '/simulator/payment-switch': ['overview', 'planned', 'fit'],
  '/simulator/issuer': ['overview'],
  '/simulator/atm': ['overview'],
  '/simulator/ecr': ['overview'],
  '/tools/emv-tools': ['overview', 'all-tools', 'sda', 'dda', 'app-crypto', 'emv42', 'mchip', 'vsdc', 'secure-msg', 'cap', 'hce'],
  '/tools/pin-tools': ['overview', 'all-tools', 'formats', 'pin-block', 'format-walkthroughs', 'aes-pin', 'translate', 'dukpt-pin', 'tips'],
  '/tools/cipher-tools': ['overview'],
  '/tools/key-tools': ['overview'],
  '/tools/utility-tools': ['overview'],
  '/tools/dukpt-tools': ['overview'],
  '/tools/mac-tools': ['overview'],
  '/tools/card-validation': ['overview', 'concepts', 'cvc-mc', 'amex', 'service-codes', 'tips'],
  '/download': ['installers', 'whats-inside'],
  '/contact': ['channels'],
  '/privacy-policy': ['overview', 'data-collection', 'data-usage', 'data-sharing', 'security', 'retention', 'rights', 'cookies', 'international', 'minors', 'updates', 'compliance'],
  '/terms-and-conditions': [],
  '/pro': ['what', 'register', 'faq'],
};

/** Pages that close on a call to action, reported as final_cta_click. */
const CLOSING_CTA = [
  '/', '/emv-certification', '/cloud-simulators', '/middleware', '/kernel',
  '/simulator/host', '/simulator/hsm', '/simulator/pos',
  '/tools/emv-tools', '/tools/pin-tools', '/tools/dukpt-tools',
];

/** Pages that carry a Pro upsell link, reported as pro_click. */
const PRO_NUDGE = [
  '/', '/emv-certification', '/docs', '/docs/installation', '/contact',
  '/simulator/host', '/simulator/hsm', '/tools/emv-tools',
];

function home(doc) {
  const route = '/';
  expect(route, doc, 'section.hero[data-sect="hero"]');
  expect(route, doc, '.hero .hero-ctas a[href*="releases/latest"]');
  expect(route, doc, '#flowRail .node h3', 6);
  expect(route, doc, '#simGrid .simtile .st-name', 9, true);
  expect(route, doc, 'a.cat h3', 6, true);
  expect(route, doc, 'a.sol h3', 4, true);
  expectSections(route, doc, ['hero', 'transaction_path', 'toolbox', 'lifecycle', 'solutions', 'final_cta']);
}

const pages = new Set(['/', ...Object.keys(SOLUTION_SECTIONS), ...Object.keys(GUIDE_SECTIONS),
  '/blogs', '/blogs/what-is-iso8583-studio']);

for (const route of pages) {
  const doc = load(route);
  if (!doc) continue;
  shell(route, doc);

  if (route === '/') home(doc);

  if (SOLUTION_SECTIONS[route]) {
    expectSections(route, doc, SOLUTION_SECTIONS[route]);
    expect(route, doc, '.ph-ctas a', 1);
    expect(route, doc, '.breadcrumb a, .crumb a');
  }

  if (GUIDE_SECTIONS[route]) {
    expectSections(route, doc, GUIDE_SECTIONS[route]);
    expect(route, doc, '.breadcrumb a, .crumb a');
  }

  if (CLOSING_CTA.includes(route)) expect(route, doc, 'section.cta a');
  if (PRO_NUDGE.includes(route)) expect(route, doc, '.pro-nudge a[href^="/pro"]');

  if (route === '/docs') expect(route, doc, 'a.hub-card .hub-title', 28);
  if (route === '/contact') expect(route, doc, 'a.hub-card .hub-title .badge', 4);
  if (route === '/simulator') expect(route, doc, 'a.hub-card .hub-title', 9);
  if (route === '/privacy-policy') expect(route, doc, 'a.hub-card .hub-title', 2);
  if (route === '/download') {
    expect(route, doc, 'a[href$=".dmg"]');
    expect(route, doc, 'a[href$=".exe"]');
  }

  if (route === '/blogs') {
    expect(route, doc, '.card--post .ui-card-title', 52);
    expect(route, doc, '.card--post .ui-card-eyebrow', 52);
    expect(route, doc, 'button.filter-btn', 11, true);
  }
  if (route.startsWith('/blogs/')) {
    expect(route, doc, '.related-card .ui-card-title', 1);
    expect(route, doc, '.related-card .ui-card-eyebrow', 1);
  }
}

if (failures.length) {
  console.error(failures.map((f) => `✖ ${f}`).join('\n'));
  console.error(`\n${failures.length} tracking hook problem(s)`);
  process.exit(1);
}
console.log(`✔ tracking hooks present on ${pages.size} pages`);
