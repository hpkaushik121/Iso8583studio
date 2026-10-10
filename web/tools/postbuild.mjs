/**
 * Post-build: sitemap, legacy redirect stubs and 404.html.
 *
 * The sitemap is derived from what was actually emitted rather than from a
 * separate list. build_blogs.py kept a hardcoded STATIC_SITEMAP_ENTRIES of 11
 * pages and overwrote sitemap.xml on every run, which is why 21 live pages
 * were never submitted. Reading the build output makes that class of drift
 * impossible.
 */
import { readdirSync, readFileSync, writeFileSync, statSync, copyFileSync, existsSync, mkdirSync } from 'node:fs';
import { join, dirname, relative } from 'node:path';
import { fileURLToPath } from 'node:url';
import { MOVED_ROUTES } from './moved-routes.mjs';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const DIST = join(ROOT, 'dist/iso8583-studio/browser');
const SITE = 'https://iso8583.studio';

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
const modifiedOf = (html) =>
  html.match(/"dateModified"\s*:\s*"([^"]+)"/)?.[1];

const indexable = pages.filter((p) => !noindex(p.html));
const missingCanonical = indexable.filter((p) => !canonicalOf(p.html));
if (missingCanonical.length) {
  throw new Error(`pages without a canonical: ${missingCanonical.map((p) => p.route).join(', ')}`);
}

const urls = indexable.map((p) => {
  const loc = canonicalOf(p.html);
  // Only a real content date is emitted. Blog posts carry datePublished, and
  // dateModified once their front matter declares `updated:`; static pages
  // carry neither and so get no <lastmod> at all. The previous build stamped
  // the deploy date on every static page, which re-dated 33 URLs on each push
  // and taught search engines to discount the field site-wide. priority and
  // changefreq are not emitted: Google ignores both.
  const lastmod = modifiedOf(p.html) ?? publishedOf(p.html);
  return `  <url>\n    <loc>${loc}</loc>\n` +
    (lastmod ? `    <lastmod>${lastmod}</lastmod>\n` : '') +
    `  </url>`;
});

// Angular's client-side fallback shell has no content; keep it out of the index.
const csr = join(DIST, 'index.csr.html');
if (existsSync(csr) && !noindex(readFileSync(csr, 'utf8'))) {
  writeFileSync(csr, readFileSync(csr, 'utf8').replace('</head>', '<meta name="robots" content="noindex"></head>'));
}

writeFileSync(join(DIST, 'sitemap.xml'),
  `<?xml version="1.0" encoding="UTF-8"?>\n` +
  `<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${urls.join('\n')}\n</urlset>\n`);

// ---- extensionless URLs served without a redirect ------------------------

/**
 * Prerender emits every route as <route>/index.html. GitHub Pages serves that
 * at /<route>/ and answers the extensionless /<route> with a 301 to the slash
 * form. The canonical tag, og:url, the sitemap and every internal link all use
 * the extensionless form, so without this step 30 of the site's URLs were
 * published as redirects whose target then declared the redirecting URL as
 * canonical — a circular signal for search engines.
 *
 * Mirroring each page to <route>.html makes /<route> resolve to a real 200,
 * which is how the blog posts and legal pages already behaved. A request for
 * /<route> can resolve to either <route>.html or <route>/index.html, and
 * GitHub Pages' precedence between them is not something to depend on; serving
 * identical content at both paths is deterministic whichever way it resolves,
 * and the canonical tag consolidates them. A redirect stub here would be
 * ambiguous for exactly that reason and could loop.
 */
const aliases = indexable
  .filter((p) => p.route !== '/')
  .map((p) => [`${p.route.slice(1)}.html`, p.route]);

for (const [file, route] of aliases) {
  const source = join(DIST, route.slice(1), 'index.html');
  if (!existsSync(source)) throw new Error(`alias has no page to mirror: ${route}`);
  copyFileSync(source, join(DIST, file));
}

// ---- moved /docs/* routes --------------------------------------------------

/**
 * Simulator and tool pages moved out of /docs into /simulator/* and /tools/*.
 * The old URLs are indexed and linked from outside, and GitHub Pages cannot
 * issue a 301, so each old route gets a meta-refresh stub. Unlike the .html
 * aliases above there is no resolution ambiguity here — /docs/<slug>/ only
 * ever maps to <slug>/index.html — so a redirect cannot loop. The stub is
 * noindex (it must not enter the sitemap); the canonical and the refresh both
 * point at the new home, which is what transfers the indexing.
 *
 * The inline script carries the query string and hash across, and the meta
 * refresh is only the no-JS fallback. A meta refresh can only name a fixed
 * URL, so on its own it dropped `?gclid=...` and `?src=ads` — which meant an
 * ad click on an old URL arrived with no click id (unattributable, and filed
 * as Direct) and never set the paid-session flag that suppresses AdSense and
 * the Pro interstitial. The script runs during parse while a `content="0"`
 * refresh is merely queued, so the script always wins; `replace` rather than
 * `assign` keeps the stub out of history so Back does not bounce through it.
 * check-links.mjs matches the refresh URL exactly, so that tag stays a bare
 * path — do not append anything to it.
 */
for (const [oldRoute, newRoute] of Object.entries(MOVED_ROUTES)) {
  if (!existsSync(join(DIST, newRoute.slice(1), 'index.html'))) {
    throw new Error(`moved route points at a page that was not emitted: ${oldRoute} -> ${newRoute}`);
  }
  const dir = join(DIST, oldRoute.slice(1));
  mkdirSync(dir, { recursive: true });
  const stub =
    `<!doctype html>\n<html lang="en">\n<head>\n` +
    `<meta charset="utf-8">\n` +
    `<title>Moved to ${SITE}${newRoute} - ISO8583Studio</title>\n` +
    `<meta name="robots" content="noindex">\n` +
    `<link rel="canonical" href="${SITE}${newRoute}">\n` +
    `<script>location.replace(${JSON.stringify(newRoute)}+location.search+location.hash)</script>\n` +
    `<meta http-equiv="refresh" content="0; url=${newRoute}">\n` +
    `</head>\n<body>\n` +
    `<p>This page has moved to <a href="${newRoute}">${SITE}${newRoute}</a>.</p>\n` +
    `</body>\n</html>\n`;
  writeFileSync(join(dir, 'index.html'), stub);
  // Without the .html sibling the old extensionless URL 301s to its slash form
  // before the stub can forward it — a three-hop chain for every inbound link.
  writeFileSync(join(DIST, `${oldRoute.slice(1)}.html`), stub);
}

// ---- 404 -------------------------------------------------------------------

const notFound = join(DIST, '404/index.html');
if (!existsSync(notFound)) throw new Error('the /404 route was not prerendered');
copyFileSync(notFound, join(DIST, '404.html'));

console.log(`sitemap: ${urls.length} urls | .html aliases: ${aliases.length} | ` +
  `moved-route stubs: ${Object.keys(MOVED_ROUTES).length} | 404.html written`);
