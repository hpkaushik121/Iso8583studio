/**
 * Builds public/icons.svg — one SVG sprite holding every Phosphor icon the
 * site uses, so no icon font or CDN is loaded.
 *
 * Which icons are "used" is read out of the source rather than kept in a list:
 * every quoted lowercase token under src/app is tested against the Phosphor
 * set. A common word that happens to be an icon name ('key', 'list') pulls in
 * an icon nobody draws, which costs a few hundred bytes; a hand-kept list that
 * drifts costs a missing icon on a live page.
 *
 * Regular weight is the default. A name ending in -fill ('star-fill') selects
 * the filled drawing.
 */
import { readdirSync, readFileSync, writeFileSync, statSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const SRC = join(ROOT, 'src/app');
const ASSETS = join(ROOT, 'node_modules/@phosphor-icons/core/assets');
const OUT = join(ROOT, 'public/icons.svg');

const available = new Map();
for (const weight of ['regular', 'fill']) {
  for (const file of readdirSync(join(ASSETS, weight))) {
    if (file.endsWith('.svg')) available.set(file.slice(0, -4), join(ASSETS, weight, file));
  }
}

function* sourceFiles(dir) {
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry);
    if (statSync(full).isDirectory()) yield* sourceFiles(full);
    else if (/\.(ts|html)$/.test(entry)) yield full;
  }
}

const used = new Set();
const TOKEN = /['"`]([a-z][a-z0-9]*(?:-[a-z0-9]+)*)['"`]/g;
for (const file of sourceFiles(SRC)) {
  for (const [, token] of readFileSync(file, 'utf8').matchAll(TOKEN)) {
    if (available.has(token)) used.add(token);
  }
}

const symbols = [...used].sort().map((name) => {
  const svg = readFileSync(available.get(name), 'utf8');
  const body = svg.replace(/^<svg[^>]*>/, '').replace(/<\/svg>\s*$/, '');
  return `<symbol id="${name}" viewBox="0 0 256 256">${body}</symbol>`;
});

writeFileSync(
  OUT,
  `<svg xmlns="http://www.w3.org/2000/svg" fill="currentColor">${symbols.join('')}</svg>\n`,
);
console.log(`icons: ${symbols.length} symbols, ${(statSync(OUT).size / 1024).toFixed(1)} kB`);
