/**
 * The blog shell is deliberately new markup, so a lockstep DOM comparison would
 * be meaningless. What must still match is the reading experience: the
 * typography of the article body, which the old pages styled as .blog-content
 * and the new ones style as .prose.
 */
import puppeteer from 'puppeteer';

const OLD = 'http://localhost:4322', NEW = 'http://localhost:4321';
const SLUGS = ['what-is-iso8583-studio', 'tr31-key-block-guide', 'pin-block-formats-iso9564',
  'hmac-cmac-calculation', 'iso8583-bitmap-explained', 'emv-cryptogram-validation'];
const PROPS = ['fontSize', 'lineHeight', 'color', 'fontWeight', 'marginTop', 'marginBottom',
  'paddingLeft', 'backgroundColor', 'borderRadius', 'fontFamily'];
const PARTS = ['p', 'h2', 'h3', 'h4', 'ul', 'li', 'strong', 'a', 'code', 'pre', 'blockquote',
  'table', 'th', 'td'];

const browser = await puppeteer.launch({ headless: true, args: ['--no-sandbox'] });

async function typography(url, rootSel) {
  const page = await browser.newPage();
  await page.setViewport({ width: 1280, height: 1000 });
  await page.goto(url, { waitUntil: 'networkidle0', timeout: 45000 });
  await new Promise((r) => setTimeout(r, 300));
  const out = await page.evaluate(({ rootSel, PARTS, PROPS }) => {
    const root = document.querySelector(rootSel);
    if (!root) return null;
    const res = {};
    for (const part of PARTS) {
      const el = root.querySelector(part);
      if (!el) { res[part] = 'ABSENT'; continue; }
      const cs = getComputedStyle(el);
      const o = {};
      for (const p of PROPS) o[p] = cs[p];
      res[part] = o;
    }
    return res;
  }, { rootSel, PARTS, PROPS });
  await page.close();
  return out;
}

let total = 0;
for (const slug of SLUGS) {
  const [a, b] = await Promise.all([
    typography(`${OLD}/blogs/${slug}.html`, 'article.blog-content'),
    typography(`${NEW}/blogs/${slug}`, '.prose'),
  ]);
  if (!a || !b) { console.log(`  ! ${slug}: root not found (old=${!!a} new=${!!b})`); continue; }
  const diffs = [];
  for (const part of PARTS) {
    if (a[part] === 'ABSENT' || b[part] === 'ABSENT') continue;
    for (const p of PROPS) {
      if (a[part][p] !== b[part][p]) diffs.push(`${part}.${p}: old=${a[part][p]} new=${b[part][p]}`);
    }
  }
  total += diffs.length;
  console.log(`  ${slug}: ${diffs.length} diff(s)`);
  for (const d of diffs) console.log(`      ${d}`);
}
await browser.close();
console.log(`\ntotal article-typography differences: ${total}`);
