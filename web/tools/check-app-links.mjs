// Every `app:` slug on a tool tile must name a web-ready tool in the Kotlin catalog, so a docs
// page never links into the browser app for a tool the app hides. Web-ready tools without a
// tile are listed as a reminder, not an error.
import { readFileSync, readdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = dirname(dirname(fileURLToPath(import.meta.url)));
const catalog = readFileSync(join(root, '..', 'tools-ui/src/commonMain/kotlin/in/aicortex/iso8583studio/tools/ToolCatalog.kt'), 'utf8');
const all = new Set();
const ready = new Set();
for (const line of catalog.split('\n')) {
  const m = line.match(/WebTool\("([a-z0-9-]+)"/);
  if (!m) continue;
  all.add(m[1]);
  if (/webReady\s*=\s*true/.test(line)) ready.add(m[1]);
}

const dataDir = join(root, 'src/app/pages/data');
const slugs = new Map();
for (const file of readdirSync(dataDir).filter((f) => f.endsWith('.data.ts'))) {
  const text = readFileSync(join(dataDir, file), 'utf8');
  for (const line of text.split('\n')) {
    if (!/\bstage:\s*\d/.test(line)) continue; // hub tiles only
    const m = line.match(/\bapp:\s*'([a-z0-9-]+)'/);
    if (m) slugs.set(m[1], file);
  }
}

const bad = [...slugs].filter(([slug]) => !ready.has(slug));
if (bad.length) {
  for (const [slug, file] of bad) console.error(`✖ ${file}: app slug '${slug}' is ${all.has(slug) ? 'not web-ready' : 'not in the tool catalog'}`);
  process.exit(1);
}
const missing = [...ready].filter((id) => !slugs.has(id));
console.log(`✔ ${slugs.size} app links resolve to web-ready tools (${ready.size} web-ready, ${all.size} in catalog)` + (missing.length ? `; no docs tile yet for: ${missing.join(', ')}` : ''));
