/**
 * Reports what a first visit actually downloads: each page is loaded from the
 * dist server in a clean browser, left alone without scrolling, and the bytes
 * that came over the wire are totalled by type.
 *
 *   npm run build && node tools/measure-weight.mjs [out.json]
 *
 * It also fails if a first load pulls anything that is meant to wait for the
 * reader: a video on a page whose hero has none, or the three.js scene.
 */
import { spawn } from 'node:child_process';
import { writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import puppeteer from 'puppeteer';

const HERE = dirname(fileURLToPath(import.meta.url));
const PORT = 4398;
const ORIGIN = `http://localhost:${PORT}`;

const PAGES = [
  '/', '/simulator/pos', '/simulator/hsm', '/simulator/atm', '/emv-certification',
  '/tools/pin-tools', '/docs', '/blogs', '/blogs/what-is-iso8583-studio', '/pro',
];

const kind = (type, url) =>
  /\.mp4(\?|$)/.test(url) || type === 'media' ? 'video'
  : type === 'script' ? 'js'
  : type === 'stylesheet' ? 'css'
  : type === 'image' ? 'image'
  : type === 'font' ? 'font'
  : type === 'document' ? 'html'
  : 'other';

const server = spawn(process.execPath, [join(HERE, 'serve-dist.mjs')], {
  env: { ...process.env, PORT: String(PORT) }, stdio: 'ignore',
});
await new Promise((r) => setTimeout(r, 600));

const browser = await puppeteer.launch({ headless: true, args: ['--no-sandbox'] });
const report = {};
const problems = [];

try {
  for (const path of PAGES) {
    const page = await browser.newPage();
    await page.setViewport({ width: 1360, height: 850 });
    await page.setCacheEnabled(false);
    const client = await page.createCDPSession();
    await client.send('Network.enable');

    const requests = new Map();
    client.on('Network.responseReceived', (e) =>
      requests.set(e.requestId, { url: e.response.url, type: e.type.toLowerCase() }));
    const totals = { html: 0, css: 0, js: 0, image: 0, font: 0, video: 0, other: 0 };
    const local = [];
    const sizes = [];
    client.on('Network.loadingFinished', (e) => {
      const req = requests.get(e.requestId);
      if (!req || !req.url.startsWith(ORIGIN)) return; // third parties are not ours to budget
      totals[kind(req.type, req.url)] += e.encodedDataLength;
      local.push(req.url.slice(ORIGIN.length));
      sizes.push({ url: req.url.slice(ORIGIN.length), kind: kind(req.type, req.url), bytes: e.encodedDataLength });
    });

    await page.goto(ORIGIN + path, { waitUntil: 'domcontentloaded' });
    // Long enough for deferred work that does not wait for the reader to show itself.
    await new Promise((r) => setTimeout(r, 4000));

    const total = Object.values(totals).reduce((a, b) => a + b, 0);
    report[path] = { total, ...totals, requests: local.length };

    // Video is meant to load only when its hero is on screen. On a guide page
    // the hero IS on screen at load, so one clip is expected there; elsewhere
    // none is.
    const clips = local.filter((u) => u.endsWith('.mp4'));
    const heroHasVideo = path.startsWith('/simulator/');
    if (clips.length > (heroHasVideo ? 2 : 0)) problems.push(`${path}: loaded ${clips.join(', ')} before any scrolling`);
    // The three.js scene is by far the largest lazy chunk (hundreds of kB). It
    // has a hashed name, so it is recognised by size: nothing else that is
    // not the main bundle comes close.
    const heavy = sizes.filter((r) => r.kind === 'js' && r.bytes > 300_000 && !/\/main-/.test(r.url));
    if (heavy.length) problems.push(`${path}: loaded a ${Math.round(heavy[0].bytes / 1024)} kB script before any scrolling (${heavy[0].url})`);

    await page.close();
  }
} finally {
  await browser.close();
  server.kill();
}

const kb = (n) => `${(n / 1024).toFixed(0).padStart(6)} kB`;
console.log('page'.padEnd(36) + ['total', 'html', 'css', 'js', 'image', 'font', 'video'].map((h) => h.padStart(9)).join(''));
for (const [path, r] of Object.entries(report)) {
  console.log(path.padEnd(36) + [r.total, r.html, r.css, r.js, r.image, r.font, r.video].map(kb).join(''));
}
if (process.argv[2]) writeFileSync(process.argv[2], JSON.stringify(report, null, 2));

if (problems.length) {
  console.error('\n' + problems.map((p) => `✖ ${p}`).join('\n'));
  process.exit(1);
}
