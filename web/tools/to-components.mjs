/**
 * One-shot: turns the imported pages into real Angular components.
 *
 * Until now every non-blog page was a string of HTML bound through
 * [innerHTML] by StaticPage. That was the bridge the bulk import needed, and
 * it cost the pages everything the framework gives a template: no compilation,
 * no type checking, no primitives, a sanitizer bypass, and a hand-rolled click
 * handler to put internal links back on the router.
 *
 * This reads the last generated content modules — the exact markup the site
 * ships today — and writes one standalone component per page with that markup
 * as an inline template. The output is source: it is committed, hand-edited
 * from then on, and this script together with tools/import-pages.mjs and
 * docs/*.html is deleted once it has run.
 *
 * It deliberately does not substitute ui/ primitives. Structure is preserved
 * exactly so the conversion can be verified as a no-op against the previous
 * build; adopting primitives is a separate, reviewable pass on the components.
 */
import { readFileSync, writeFileSync, readdirSync, mkdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const PAGES_IN = join(ROOT, 'src/app/content/pages');
const OUT = join(ROOT, 'src/app/pages/site');

/** `docs-hsm-simulator` -> `DocsHsmSimulatorPage` */
const className = (id) =>
  id.split(/[-_]/).map((s) => s[0].toUpperCase() + s.slice(1)).join('') + 'Page';

/* The markup becomes a template literal, so the three sequences that would end
   it or start an interpolation are escaped. None occur in the current corpus;
   escaping anyway is what keeps that from being a silent break later. */
const forTemplateLiteral = (s) =>
  s.replace(/\\/g, '\\\\').replace(/`/g, '\\`').replace(/\$\{/g, '\\${');

/* A bare & is valid in a document but not in a template: Angular's parser
   reads it as the start of a character reference. Only alt/data-alt text in
   docs/emv-tools has one. */
const fixBareAmp = (s) =>
  s.replace(/&(?![a-zA-Z][a-zA-Z0-9]*;|#\d+;|#x[0-9a-fA-F]+;)/g, '&amp;');

/* page-routes.ts is generated with JSON.stringify, so each route's seo object
   parses as JSON once it has been isolated by brace matching. */
function routeMeta() {
  const src = readFileSync(join(ROOT, 'src/app/content/page-routes.ts'), 'utf8');
  const out = [];
  const re = /path:\s*("(?:[^"\\]|\\.)*"),/g;
  for (let m; (m = re.exec(src));) {
    const path = JSON.parse(m[1]);
    const seoAt = src.indexOf('seo: ', m.index);
    const idAt = src.indexOf("import('../content/pages/", m.index);
    const id = src.slice(idAt).match(/import\('\.\.\/content\/pages\/([^']+)'\)/)[1];

    let depth = 0, start = src.indexOf('{', seoAt), end = start;
    for (let i = start; i < src.length; i++) {
      if (src[i] === '{') depth++;
      else if (src[i] === '}' && --depth === 0) { end = i + 1; break; }
    }
    out.push({ id, path, seo: JSON.parse(src.slice(start, end)) });
  }
  return out;
}

mkdirSync(OUT, { recursive: true });

const metas = routeMeta();
const byId = new Map(metas.map((m) => [m.id, m]));
const ids = readdirSync(PAGES_IN).filter((f) => f.endsWith('.ts')).map((f) => f.replace(/\.ts$/, ''));

let ampFixes = 0;
for (const id of ids) {
  if (!byId.has(id)) { console.warn(`  NO ROUTE for ${id}`); continue; }
  const mod = readFileSync(join(PAGES_IN, `${id}.ts`), 'utf8');
  let html = JSON.parse(mod.slice(mod.indexOf('"')).replace(/;\s*$/, ''));

  const fixed = fixBareAmp(html);
  if (fixed !== html) ampFixes++;
  html = fixed;

  writeFileSync(join(OUT, `${id}.ts`),
    `import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';\n` +
    `import { SitePage } from './site-page';\n\n` +
    `@Component({\n` +
    `  selector: 'page-${id}',\n` +
    `  changeDetection: ChangeDetectionStrategy.OnPush,\n` +
    `  hostDirectives: [SitePage],\n` +
    `  host: { class: 'static-page page-${id}' },\n` +
    `  // <image-slot> is a styling-only element the design system owns; see\n` +
    `  // _components.css. Drop this schema once it becomes part of ui-figure.\n` +
    `  schemas: [CUSTOM_ELEMENTS_SCHEMA],\n` +
    `  template: \`${forTemplateLiteral(html)}\`,\n` +
    `})\n` +
    `export class ${className(id)} {}\n`);
}

const entries = metas.map((m) => `  {
    path: ${JSON.stringify(m.path.replace(/^\//, ''))},${m.path === '/' ? "\n    pathMatch: 'full'," : ''}
    loadComponent: () => import('./${m.id}').then((c) => c.${className(m.id)}),
    data: { seo: ${JSON.stringify(m.seo, null, 2).replace(/\n/g, '\n    ')} },
  },`);

writeFileSync(join(OUT, 'site-routes.ts'),
  `import { Routes } from '@angular/router';\n\n` +
  `/** One lazy component per page. */\n` +
  `export const siteRoutes: Routes = [\n${entries.join('\n')}\n];\n`);

console.log(`components: ${ids.length} written, ${metas.length} routes, ${ampFixes} pages had a bare & escaped`);
