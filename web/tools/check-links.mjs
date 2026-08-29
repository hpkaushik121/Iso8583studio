/**
 * CI gate over the build output. This is what keeps "no broken links" true
 * after the migration rather than just at the moment of it.
 *
 * Fails the build when:
 *  - a route that should exist was not emitted;
 *  - an internal href points at a file that is not there;
 *  - a canonical, og:url or in-page link still carries .html;
 *  - a sitemap entry does not resolve;
 *  - a legacy .html URL lost its redirect stub;
 *  - any URL that is live today is missing from the output.
 */
import { readdirSync, readFileSync, existsSync, statSync } from 'node:fs';
import { join, dirname, relative } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const DIST = process.argv[2] ? join(process.cwd(), process.argv[2]) : join(ROOT, 'dist/iso8583-studio/browser');
const SITE = 'https://iso8583.studio';

const errors = [];
const fail = (msg) => errors.push(msg);

if (!existsSync(DIST)) {
  console.error(`build output not found at ${DIST}`);
  process.exit(1);
}

// ---- collect emitted files -------------------------------------------------

function walk(dir, out = []) {
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry);
    if (statSync(full).isDirectory()) walk(full, out);
    else out.push(relative(DIST, full));
  }
  return out;
}

const files = new Set(walk(DIST));
const pages = [...files].filter((f) => f.endsWith('index.html') && f !== 'index.csr.html');
const routes = new Set(pages.map((f) => `/${f.replace(/index\.html$/, '').replace(/\/$/, '')}`.replace(/^\/$/, '/')));

/** Does a site-absolute URL resolve to something in the output? */
function resolves(url) {
  const path = url.split('#')[0].split('?')[0].replace(/^\//, '').replace(/\/$/, '');
  if (path === '') return files.has('index.html');
  if (files.has(path)) return true;
  if (files.has(`${path}/index.html`)) return true;
  if (files.has(`${path}.html`)) return true;
  return false;
}

// ---- 1. every expected route emitted ---------------------------------------

const EXPECTED_BLOG = readdirSync(join(ROOT, 'content/blog'))
  .filter((f) => f.endsWith('.md')).map((f) => `/blogs/${f.replace(/\.md$/, '')}`);

const EXPECTED_PAGES = [
  '/', '/privacy-policy', '/terms-and-conditions',
  '/cloud-simulators', '/contact', '/emv-certification', '/kernel', '/middleware', '/pro',
  '/docs', '/blogs',
  ...['apdu-simulator', 'atm-simulator', 'card-validation', 'cipher-tools', 'contributing',
    'dukpt-tools', 'ecr-simulator', 'emv-tools', 'host-simulator', 'hsm-command-console',
    'hsm-simulator', 'installation', 'issuer-simulator', 'key-tools', 'mac-tools',
    'payment-simulators', 'payment-switch', 'pin-tools', 'pos-simulator', 'utility-tools',
    'versions'].map((s) => `/docs/${s}`),
];

const expected = [...EXPECTED_PAGES, ...EXPECTED_BLOG];
for (const route of expected) {
  if (!routes.has(route)) fail(`route not emitted: ${route}`);
}
if (expected.length !== 84) fail(`expected 84 routes, the checker knows about ${expected.length}`);

// ---- 2/3. internal links resolve, and nothing keeps .html ------------------

const STUBS = new Set([
  'privacy-policy.html', 'terms-and-conditions.html',
  ...EXPECTED_BLOG.map((r) => `${r.slice(1)}.html`),
]);

for (const file of pages) {
  const html = readFileSync(join(DIST, file), 'utf8');
  const from = `/${file.replace(/index\.html$/, '')}`;

  for (const m of html.matchAll(/\b(?:href|src)="([^"]+)"/g)) {
    const url = m[1];
    if (/^(https?:|mailto:|tel:|data:|javascript:|#)/i.test(url)) continue;
    // Every page carries <base href="/">, so a relative URL — which is how the
    // bundler emits its own script and stylesheet tags — resolves from the root.
    const target = url.startsWith('/') ? url : `/${url}`;
    if (/\.html(\?|#|$)/.test(target)) fail(`${from}: link still carries .html -> ${url}`);
    if (!resolves(target)) fail(`${from}: broken link -> ${url}`);
  }

  const canonical = html.match(/<link[^>]+rel="canonical"[^>]+href="([^"]+)"/i)?.[1];
  if (!canonical) fail(`${from}: no canonical`);
  else if (/\.html/.test(canonical)) fail(`${from}: canonical still carries .html -> ${canonical}`);

  const ogUrl = html.match(/<meta[^>]+property="og:url"[^>]+content="([^"]+)"/i)?.[1];
  if (ogUrl && /\.html/.test(ogUrl)) fail(`${from}: og:url still carries .html -> ${ogUrl}`);

  if (!/<title>[^<]+<\/title>/.test(html)) fail(`${from}: no title`);
}

// ---- 3b. in-page anchors point at an element that exists -----------------

/* <base href="/"> makes a bare "#id" resolve to the site root, so every
   in-page anchor is written as "/page#id". That only works if the id is
   actually on that page. */
const idsOf = (html) => new Set(
  [...html.matchAll(/\sid="([^"]+)"/g)].map((m) => m[1]),
);

let anchorsChecked = 0;
for (const file of pages) {
  const html = readFileSync(join(DIST, file), 'utf8');
  const from = `/${file.replace(/index\.html$/, '')}`.replace(/\/$/, '') || '/';
  const ids = idsOf(html);

  for (const m of html.matchAll(/\bhref="([^"]*#[^"]*)"/g)) {
    const [path, frag] = m[1].split('#');
    if (!frag) continue;
    // Only same-page anchors can be verified from this document.
    const target = path.replace(/\/$/, '') || '/';
    if (path && target !== from) continue;
    anchorsChecked++;
    if (!ids.has(frag)) fail(`${from}: anchor #${frag} has no matching element`);
    if (!path) fail(`${from}: bare "#${frag}" resolves to the site root under <base href="/">`);
  }
}

// ---- 4. sitemap ------------------------------------------------------------

const sitemapPath = join(DIST, 'sitemap.xml');
if (!existsSync(sitemapPath)) fail('sitemap.xml missing');
else {
  const sitemap = readFileSync(sitemapPath, 'utf8');
  const locs = [...sitemap.matchAll(/<loc>([^<]+)<\/loc>/g)].map((m) => m[1]);
  if (!locs.length) fail('sitemap.xml has no entries');
  for (const loc of locs) {
    if (!loc.startsWith(SITE)) { fail(`sitemap: foreign origin ${loc}`); continue; }
    if (/\.html/.test(loc)) fail(`sitemap: entry still carries .html -> ${loc}`);
    if (!resolves(loc.slice(SITE.length))) fail(`sitemap: entry does not resolve -> ${loc}`);
  }
  // Every indexable page must be listed.
  const listed = new Set(locs.map((l) => l.slice(SITE.length) || '/'));
  for (const route of expected) {
    if (!listed.has(route)) fail(`sitemap: missing ${route}`);
  }
}

// ---- 5. legacy .html URLs still serve the real page ------------------------

for (const alias of STUBS) {
  if (!files.has(alias)) { fail(`legacy .html alias missing: /${alias}`); continue; }
  const html = readFileSync(join(DIST, alias), 'utf8');
  const canonical = html.match(/<link[^>]+rel="canonical"[^>]+href="([^"]+)"/i)?.[1];
  const expected = `${SITE}/${alias.replace(/\.html$/, '')}`;
  if (!canonical) fail(`legacy alias has no canonical: /${alias}`);
  else if (canonical !== expected) {
    fail(`legacy alias canonical should be ${expected}, got ${canonical}`);
  }
  // A redirect here would be ambiguous and could loop; these must be real pages.
  if (/http-equiv="refresh"/i.test(html)) fail(`legacy alias is a redirect, not the page: /${alias}`);
  if (!/<title>[^<]+<\/title>/.test(html)) fail(`legacy alias has no title: /${alias}`);
}

// ---- 6. everything indexed today still resolves ----------------------------

const LIVE_SITEMAP = join(ROOT, '..', 'docs', 'sitemap.xml');
if (existsSync(LIVE_SITEMAP)) {
  const live = readFileSync(LIVE_SITEMAP, 'utf8');
  for (const m of live.matchAll(/<loc>([^<]+)<\/loc>/g)) {
    const path = m[1].replace(SITE, '');
    if (!resolves(path)) fail(`currently-indexed URL would 404: ${m[1]}`);
  }
}

// ---- report ----------------------------------------------------------------

if (!existsSync(join(DIST, '404.html'))) fail('404.html missing');
if (!existsSync(join(DIST, 'CNAME'))) fail('CNAME missing');
if (!existsSync(join(DIST, 'robots.txt'))) fail('robots.txt missing');

if (errors.length) {
  console.error(`\n✘ ${errors.length} problem(s):\n`);
  for (const e of errors.slice(0, 60)) console.error(`  - ${e}`);
  if (errors.length > 60) console.error(`  … and ${errors.length - 60} more`);
  process.exit(1);
}

console.log(`✔ ${pages.length} pages, ${STUBS.size} legacy .html aliases, ${anchorsChecked} in-page anchors, sitemap and assets all check out`);
