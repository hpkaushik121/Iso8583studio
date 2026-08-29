/**
 * Post-build: sitemap, legacy redirect stubs and 404.html.
 *
 * The sitemap is derived from what was actually emitted rather than from a
 * separate list. build_blogs.py kept a hardcoded STATIC_SITEMAP_ENTRIES of 11
 * pages and overwrote sitemap.xml on every run, which is why 21 live pages
 * were never submitted. Reading the build output makes that class of drift
 * impossible.
 */
import { readdirSync, readFileSync, writeFileSync, statSync, copyFileSync, existsSync } from 'node:fs';
import { join, dirname, relative } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const DIST = join(ROOT, 'dist/iso8583-studio/browser');
const SITE = 'https://iso8583.studio';
const TODAY = new Date().toISOString().slice(0, 10);

if (!existsSync(DIST)) throw new Error(`build output not found at ${DIST} — run ng build first`);

/** Every emitted page, as { route, file, html }. */
function emittedPages(dir = DIST) {
  const out = [];
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry);
    if (statSync(full).isDirectory()) { out.push(...emittedPages(full)); continue; }
    if (entry !== 'index.html') continue;
    const rel = relative(DIST, full).replace(/index\.html$/, '').replace(/\/$/, '');
    out.push({ route: `/${rel}`.replace(/^\/$/, '/'), file: full, html: readFileSync(full, 'utf8') });
  }
  return out;
}

const pages = emittedPages().sort((a, b) => a.route.localeCompare(b.route));

// ---- sitemap ---------------------------------------------------------------

const noindex = (html) => /<meta[^>]+name="robots"[^>]+content="[^"]*noindex/i.test(html);
const canonicalOf = (html) =>
  html.match(/<link[^>]+rel="canonical"[^>]+href="([^"]+)"/i)?.[1];
const publishedOf = (html) =>
  html.match(/"datePublished"\s*:\s*"([^"]+)"/)?.[1];

const priorityFor = (route) => {
  if (route === '/') return '1.0';
  if (route === '/docs' || route === '/blogs') return '0.9';
  if (/^\/docs\//.test(route)) return '0.8';
  if (/^\/blogs\//.test(route)) return '0.7';
  if (/^\/(privacy-policy|terms-and-conditions)$/.test(route)) return '0.3';
  return '0.8';
};

const changefreqFor = (route) =>
  route === '/' || route === '/blogs' || route === '/docs' ? 'weekly'
    : /^\/(privacy-policy|terms-and-conditions)$/.test(route) ? 'yearly' : 'monthly';

const indexable = pages.filter((p) => !noindex(p.html));
const missingCanonical = indexable.filter((p) => !canonicalOf(p.html));
if (missingCanonical.length) {
  throw new Error(`pages without a canonical: ${missingCanonical.map((p) => p.route).join(', ')}`);
}

const urls = indexable.map((p) => {
  const loc = canonicalOf(p.html);
  const lastmod = publishedOf(p.html) ?? TODAY;
  return `  <url>\n    <loc>${loc}</loc>\n    <lastmod>${lastmod}</lastmod>\n` +
    `    <changefreq>${changefreqFor(p.route)}</changefreq>\n` +
    `    <priority>${priorityFor(p.route)}</priority>\n  </url>`;
});

writeFileSync(join(DIST, 'sitemap.xml'),
  `<?xml version="1.0" encoding="UTF-8"?>\n` +
  `<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${urls.join('\n')}\n</urlset>\n`);

// ---- legacy .html URLs -----------------------------------------------------

/**
 * The 52 blog posts and the two legal pages were indexed at .html URLs, and
 * GitHub Pages cannot issue a 301.
 *
 * These serve the real page rather than a redirect. A redirect stub at
 * blogs/<slug>.html would be ambiguous: a request for /blogs/<slug> can resolve
 * to either <slug>.html or <slug>/index.html, and GitHub Pages' precedence
 * between the two is not something to depend on. If it preferred the .html
 * file, the stub would redirect to a URL that served the stub again — an
 * infinite loop. Serving identical content at both paths is deterministic
 * whichever way it resolves, and the canonical tag (already pointing at the
 * extensionless URL) is what consolidates them for search engines. This also
 * matches how the site behaves today, where both forms return 200.
 */
const legacy = [
  ...pages.filter((p) => /^\/blogs\/.+/.test(p.route))
    .map((p) => [`${p.route.slice(1)}.html`, p.route]),
  ['privacy-policy.html', '/privacy-policy'],
  ['terms-and-conditions.html', '/terms-and-conditions'],
];

for (const [file, route] of legacy) {
  const source = join(DIST, route.slice(1), 'index.html');
  if (!existsSync(source)) throw new Error(`legacy alias has no page to mirror: ${route}`);
  copyFileSync(source, join(DIST, file));
}

// ---- 404 -------------------------------------------------------------------

const notFound = join(DIST, '404/index.html');
if (!existsSync(notFound)) throw new Error('the /404 route was not prerendered');
copyFileSync(notFound, join(DIST, '404.html'));

console.log(`sitemap: ${urls.length} urls | legacy .html aliases: ${legacy.length} | 404.html written`);
