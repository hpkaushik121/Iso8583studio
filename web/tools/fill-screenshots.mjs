/**
 * Fills the empty <image-slot> placeholders once the screenshot files exist.
 *
 * Operates on the page components under src/app/pages/site. It used to edit
 * docs/docs/<page>/index.html, which was the content source until those pages
 * became components; the figure markup it matches is unchanged by that move.
 *
 * Each figure declares the file it expects via data-shot, and the alt text via
 * data-alt. This reads the PNG header for the real pixel dimensions, writes the
 * <img> with width/height (so the space is reserved before it loads) and sets
 * --shot-w to the 1x width, which is what the layout sizes the figure by.
 *
 *   node tools/fill-screenshots.mjs           # report what is missing
 *   node tools/fill-screenshots.mjs --write   # fill in what is present
 */
import { readFileSync, writeFileSync, existsSync, readdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const DOCS = join(ROOT, 'docs');
const SITE_PAGES = join(ROOT, 'web', 'src', 'app', 'pages', 'site');

/** `docs-pos-simulator.ts` -> `pos-simulator`, which is also the image folder. */
const docPages = () => readdirSync(SITE_PAGES)
  .filter((f) => f.startsWith('docs-') && f.endsWith('.ts'))
  .map((f) => [f.slice('docs-'.length, -'.ts'.length), join(SITE_PAGES, f)]);
/* Images are served from the Angular public directory; docs/images only still
   exists for the legacy site. Prefer public, fall back to docs. */
const IMAGE_ROOTS = [join(ROOT, 'web', 'public', 'images', 'docs'), join(DOCS, 'images', 'docs')];
const findImage = (page, shot) => IMAGE_ROOTS
  .map((root) => join(root, page, `${shot}.png`))
  .find((p) => existsSync(p));
const WRITE = process.argv.includes('--write');

/** PNG dimensions straight from the IHDR chunk — no image library needed. */
function pngSize(file) {
  const b = readFileSync(file);
  if (b.length < 24 || b.readUInt32BE(0) !== 0x89504e47) return null;
  return { w: b.readUInt32BE(16), h: b.readUInt32BE(20) };
}

/** Captures are taken at 2x, so a 1486px file is drawn 743px wide. */
const DPR = 2;

let filled = 0, missing = [], already = 0;

for (const [page, file] of docPages()) {
  let html = readFileSync(file, 'utf8');
  let changed = false;

  html = html.replace(
    /<figure class="([^"]*)" data-shot="([^"]+)" data-alt="([^"]*)"([^>]*)>\s*<image-slot>(\s*)<\/image-slot>/g,
    (whole, cls, shot, alt, rest, gap) => {
      const img = findImage(page, shot);
      if (!img) { missing.push(`web/public/images/docs/${page}/${shot}.png`); return whole; }
      const size = pngSize(img);
      if (!size) { missing.push(`${shot}.png (not a readable PNG)`); return whole; }

      // Keep an explicit --shot-w if the page already set one; otherwise derive it.
      const styled = /--shot-w:/.test(rest);
      const shotW = Math.round(size.w / DPR);
      const style = styled ? rest : `${rest} style="--shot-w:${shotW}px"`;

      filled++;
      changed = true;
      return `<figure class="${cls}" data-shot="${shot}" data-alt="${alt}"${style}>\n` +
        `                    <image-slot><img src="/images/docs/${page}/${shot}.png"\n` +
        `                                     alt="${alt}"\n` +
        `                                     width="${size.w}" height="${size.h}" loading="lazy"></image-slot>`;
    },
  );

  if (changed && WRITE) writeFileSync(file, html);
}

// Count slots that already carry an image.
for (const [, file] of docPages()) {
  already += (readFileSync(file, 'utf8').match(/<image-slot><img/g) ?? []).length;
}

console.log(`${WRITE ? 'filled' : 'would fill'}: ${filled}   already filled: ${already}   awaiting a file: ${missing.length}`);
if (missing.length) {
  console.log('\nsave these, then re-run with --write:');
  for (const m of [...new Set(missing)]) console.log(`  ${m}`);
}
