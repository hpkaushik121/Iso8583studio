/**
 * Compares the visible text of every old page against its rebuilt counterpart.
 * Codegen over 84 hand-authored documents will drop content silently; this is
 * what surfaces it.
 */
import { readFileSync, readdirSync, existsSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const DOCS = join(ROOT, '..', 'docs');
const DIST = join(ROOT, 'dist/iso8583-studio/browser');

/** Strip chrome that legitimately differs (nav, footer, scripts, styles). */
function visibleText(html, { dropShell = true } = {}) {
  let s = html;
  s = s.replace(/<script[\s\S]*?<\/script>/gi, ' ');
  s = s.replace(/<style[\s\S]*?<\/style>/gi, ' ');
  s = s.replace(/<!--[\s\S]*?-->/g, ' ');
  if (dropShell) {
    s = s.replace(/<header[\s\S]*?<\/header>/gi, ' ');
    s = s.replace(/<footer[\s\S]*?<\/footer>/gi, ' ');
    s = s.replace(/<nav[\s\S]*?<\/nav>/gi, ' ');
    s = s.replace(/<site-header[\s\S]*?<\/site-header>/gi, ' ');
    s = s.replace(/<site-footer[\s\S]*?<\/site-footer>/gi, ' ');
    s = s.replace(/<div class="m-menu[\s\S]*?<\/div>\s*(?=<)/gi, ' ');
  }
  s = s.replace(/<[^>]+>/g, ' ');
  s = s.replace(/&nbsp;/g, ' ').replace(/&amp;/g, '&').replace(/&lt;/g, '<')
       .replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&#39;/g, "'")
       .replace(/&rarr;/g, '').replace(/&copy;/g, '').replace(/&hellip;/g, '');
  return s.replace(/\s+/g, ' ').trim();
}

/** Word multiset difference: what the old page had that the new one lacks. */
function missingWords(oldText, newText) {
  const count = (t) => {
    const m = new Map();
    for (const w of t.toLowerCase().match(/[a-z0-9][a-z0-9'-]{2,}/g) ?? []) {
      m.set(w, (m.get(w) ?? 0) + 1);
    }
    return m;
  };
  const a = count(oldText), b = count(newText);
  const missing = [];
  for (const [w, n] of a) {
    const have = b.get(w) ?? 0;
    if (have < n) missing.push([w, n - have]);
  }
  return missing.sort((x, y) => y[1] - x[1]);
}

const PAIRS = [
  ['index.html', ''],
  ['privacy-policy.html', 'privacy-policy'],
  ['terms-and-conditions.html', 'terms-and-conditions'],
  ...['cloud-simulators', 'contact', 'emv-certification', 'kernel', 'middleware', 'pro']
    .map((s) => [`${s}/index.html`, s]),
  ['docs/index.html', 'docs'],
  ...['apdu-simulator', 'atm-simulator', 'card-validation', 'cipher-tools', 'contributing',
    'dukpt-tools', 'ecr-simulator', 'emv-tools', 'host-simulator', 'hsm-command-console',
    'hsm-simulator', 'installation', 'issuer-simulator', 'key-tools', 'mac-tools',
    'payment-simulators', 'payment-switch', 'pin-tools', 'pos-simulator', 'utility-tools',
    'versions'].map((s) => [`docs/${s}/index.html`, `docs/${s}`]),
  ...readdirSync(join(ROOT, 'content/blog')).filter((f) => f.endsWith('.md'))
    .map((f) => f.replace(/\.md$/, ''))
    .map((slug) => [`blogs/${slug}.html`, `blogs/${slug}`]),
];

let worst = [];
let checked = 0;

for (const [src, route] of PAIRS) {
  const oldPath = join(DOCS, src);
  const newPath = join(DIST, route, 'index.html');
  if (!existsSync(oldPath) || !existsSync(newPath)) {
    console.warn(`  skip (missing): ${src}`);
    continue;
  }
  checked++;
  const oldText = visibleText(readFileSync(oldPath, 'utf8'));
  const newText = visibleText(readFileSync(newPath, 'utf8'));
  const missing = missingWords(oldText, newText);
  const lost = missing.reduce((n, [, c]) => n + c, 0);
  const ratio = newText.length / Math.max(oldText.length, 1);
  worst.push({ route: route || '/', oldLen: oldText.length, newLen: newText.length, ratio, lost, missing });
}

worst.sort((a, b) => a.ratio - b.ratio);

console.log(`compared ${checked} pages\n`);
console.log('lowest text retention:');
for (const w of worst.slice(0, 12)) {
  console.log(`  ${(w.ratio * 100).toFixed(0).padStart(4)}%  ${w.route.padEnd(38)} ` +
    `${w.oldLen} -> ${w.newLen} chars, ${w.lost} words missing`);
  if (w.missing.length) {
    console.log(`         missing: ${w.missing.slice(0, 12).map(([t, n]) => n > 1 ? `${t}×${n}` : t).join(', ')}`);
  }
}
const bad = worst.filter((w) => w.ratio < 0.9);
console.log(`\n${bad.length} page(s) below 90% text retention`);
