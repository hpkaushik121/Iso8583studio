/**
 * Side-by-side screenshots of a prototype page and the built page, for
 * checking a port against the design by eye.
 *
 *   node tools/compare-design.mjs <prototype url> <site url> <out dir> [width] [max shots]
 *
 * Writes <out dir>/NN.jpg: prototype on the left, site on the right, one image
 * per viewport-height step down the page.
 */
import { mkdirSync } from 'node:fs';
import { join } from 'node:path';
import puppeteer from 'puppeteer';
import sharp from 'sharp';

const [protoUrl, siteUrl, outDir, widthArg, maxArg] = process.argv.slice(2);
const WIDTH = Number(widthArg ?? 1360);
const HEIGHT = WIDTH < 600 ? 800 : 850;
const MAX = Number(maxArg ?? 12);
mkdirSync(outDir, { recursive: true });

const browser = await puppeteer.launch({ headless: true, args: ['--no-sandbox', '--enable-unsafe-swiftshader'] });

async function open(url) {
  const page = await browser.newPage();
  await page.setViewport({ width: WIDTH, height: HEIGHT });
  await page.evaluateOnNewDocument(() => {
    try {
      localStorage.setItem('iso8583-cookie-consent', 'essential');
      localStorage.setItem('iso8583_no_analytics', '1');
      sessionStorage.setItem('pl-seen', '1');
      sessionStorage.setItem('iso8583_paid_session', '1');
    } catch { /* ignore */ }
  });
  await page.goto(url, { waitUntil: 'domcontentloaded' });
  await new Promise((r) => setTimeout(r, 5000));
  return page;
}

const pages = [await open(protoUrl), await open(siteUrl)];
const heights = await Promise.all(pages.map((p) => p.evaluate(() => document.documentElement.scrollHeight)));
console.log(`page heights: prototype ${heights[0]}px, site ${heights[1]}px`);

// Both pages reveal content as it scrolls into view, so walk each one top to
// bottom once before capturing, or the captures show blocks mid-animation.
for (const page of pages) {
  await page.evaluate(async (limit) => {
    for (let y = 0; y < Math.min(document.documentElement.scrollHeight, limit); y += 280) {
      window.scrollTo({ top: y, behavior: 'instant' });
      await new Promise((r) => setTimeout(r, 220));
    }
  }, MAX * HEIGHT + HEIGHT);
}

const steps = Math.min(MAX, Math.ceil(Math.max(...heights) / HEIGHT));
for (let i = 0; i < steps; i++) {
  const shots = [];
  for (const page of pages) {
    await page.evaluate((y) => window.scrollTo({ top: y, behavior: 'instant' }), i * HEIGHT);
    await new Promise((r) => setTimeout(r, 1600));
    shots.push(await page.screenshot({ type: 'png' }));
  }
  const gap = 12;
  const wide = await sharp({ create: { width: WIDTH * 2 + gap, height: HEIGHT, channels: 3, background: '#ff00aa' } })
    .composite([{ input: shots[0], left: 0, top: 0 }, { input: shots[1], left: WIDTH + gap, top: 0 }])
    .png().toBuffer();
  await sharp(wide)
    .resize({ width: Math.min(1900, WIDTH * 2 + gap) })
    .jpeg({ quality: 72 })
    .toFile(join(outDir, `${String(i).padStart(2, '0')}.jpg`));
}
await browser.close();
console.log(`${steps} comparison image(s) in ${outDir}`);
